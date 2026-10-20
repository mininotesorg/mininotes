// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;

/**
 * The page laid out so an underline's marks take no room (the owner, 2026-10-07: "Underline must not add space around
 * the underlined run"; decision 118). The marks stay in the words, so the note still says {@code __bread__} and Ctrl+U
 * still toggles them. They are not drawn, and a following word starts where they would have. Since 2026-10-10 bold and
 * italic marks too (decision 144). A plain text area measures every letter, so this view is the one that lays the lines out.
 */
final class FlushMarks extends BoxView {
    private final boolean wordWrap;

    FlushMarks(Element elem, boolean wordWrap) {
        super(elem, Y_AXIS);
        this.wordWrap = wordWrap;
    }

    @Override protected void loadChildren(ViewFactory factory) { sync(); }

    /**
     * One line view per line of the note. A keystroke dirties the line it lands in. A new line rebuilds the views from
     * the note as it is now, and only removes views that are really there: the change's own count can point past them,
     * and that is a null (decision 118).
     */
    private boolean sync() {
        Element root = getElement();
        int n = root.getElementCount();
        int have = getViewCount();
        boolean same = have == n;
        for (int i = 0; same && i < n; i++) {
            View child = getView(i);
            same = child instanceof Line && child.getElement() == root.getElement(i);
        }
        if (same) return false;
        View[] kids = new View[n];
        for (int i = 0; i < n; i++) kids[i] = new Line(root.getElement(i));
        replace(0, have, kids);
        return true;
    }

    @Override public void insertUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
        if (sync()) preferenceChanged(null, true, true);
        else tell(event, factory);
    }

    @Override public void removeUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
        if (sync()) preferenceChanged(null, true, true);
        else tell(event, factory);
    }

    @Override public void changedUpdate(DocumentEvent event, Shape shape, ViewFactory factory) {
        if (sync()) preferenceChanged(null, true, true);
        else tell(event, factory);
    }

    /** The line the edit landed in forgets its measured width, so the next draw measures the words again. */
    private void tell(DocumentEvent event, ViewFactory factory) {
        int start = event.getOffset();
        int end = start + Math.max(0, event.getLength());
        for (int i = 0; i < getViewCount(); i++) {
            View child = getView(i);
            if (child == null) continue;
            if (child.getEndOffset() < start || child.getStartOffset() > end) continue;
            DocumentEvent.EventType type = event.getType();
            if (type == DocumentEvent.EventType.REMOVE) child.removeUpdate(event, null, factory);
            else if (type == DocumentEvent.EventType.CHANGE) child.changedUpdate(event, null, factory);
            else child.insertUpdate(event, null, factory);
        }
    }

    /** The caret can sit at the very end, which is the end of the last line and not inside it. */
    @Override public Shape modelToView(int pos, Shape alloc, Position.Bias bias) throws BadLocationException {
        int n = getViewCount();
        if (n > 0 && pos == getEndOffset()) {
            Shape child = getChildAllocation(n - 1, alloc);
            if (child != null) return getView(n - 1).modelToView(pos, child, bias);
        }
        return super.modelToView(pos, alloc, bias);
    }

    /** One hard line, wrapped to the width it is given, with an underline's underscores taking nothing. */
    private final class Line extends View {
        private String text = "";
        private int origin;
        private boolean[] gone = new boolean[0];
        private int[] rows;
        private int laid = -1;
        private boolean fresh;
        private FontMetrics metrics;
        private Font measured;

        Line(Element elem) { super(elem); }

        private void read() throws BadLocationException {
            // Writing above moves this line along without changing its words, and nothing tells it: its rows are kept
            // as places in the whole note, so they move with it. Left behind, every line below a keystroke was laid
            // out and drawn from where it used to be, and the rest of the note jumped about (the owner, 2026-10-10).
            if (fresh) {
                int start = getStartOffset();
                if (start != origin) {
                    if (rows != null) for (int i = 0; i < rows.length; i++) rows[i] += start - origin;
                    origin = start;
                }
                return;
            }
            int start = getStartOffset(), end = getEndOffset();
            String now = end > start ? getDocument().getText(start, end - start) : "";
            origin = start;
            text = now;
            fresh = true;
            rows = null;
            gone = new boolean[now.length()];
            // Every style's marks take nothing (decision 144): bold and italic as an underline's always did.
            for (Marks.Run run : Marks.of(now)) {
                for (int i = run.open(); i < run.start() && i < gone.length; i++) gone[i] = true;
                for (int i = run.end(); i < run.close() && i < gone.length; i++) gone[i] = true;
            }
            rows = null;
        }

        private FontMetrics metrics() {
            Component host = getContainer();
            Font font = host == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 18) : host.getFont();
            if (metrics == null || measured == null || !measured.equals(font)) {
                metrics = host instanceof JComponent component ? component.getFontMetrics(font) : new Canvas().getFontMetrics(font);
                measured = font;
            }
            return metrics;
        }

        private int lineHeight() { return Math.max(1, metrics().getHeight()); }

        private int parentWidth() {
            if (getParent() instanceof BoxView box) {
                int width = (int) box.getWidth();
                if (width > 1 && width < Integer.MAX_VALUE / 2) return width;
            }
            return 0;
        }

        private void layout(int width) {
            try { read(); }
            catch (BadLocationException outside) { rows = new int[] {getEndOffset()}; laid = width; return; }
            if (rows != null && width == laid) return;
            laid = width;
            int start = getStartOffset(), end = getEndOffset();
            if (start >= end) { rows = new int[] {end}; return; }
            int[] found = new int[text.length() + 1];
            int count = 0, at = start, guard = 0;
            while (at < end && guard++ <= text.length() + 1) {
                int next = breakAt(at, end, Math.max(1, width));
                if (next <= at) next = Math.min(end, at + 1);
                found[count++] = next;
                at = next;
            }
            rows = java.util.Arrays.copyOf(found, Math.max(1, count));
            if (count == 0) rows[0] = end;
        }

        /** Where this row stops: a whole word when one fits, otherwise the letter that still fits. */
        private int breakAt(int from, int end, int width) {
            float used = 0;
            int lastSpace = -1;
            for (int i = from; i < end; i++) {
                int rel = i - origin;
                if (rel < 0 || rel >= text.length()) return end;
                char c = text.charAt(rel);
                if (c == '\n') return i + 1;
                float wide = advance(rel, used);
                if (wide > 0 && used + wide > width && i > from) return wordWrap && lastSpace > from ? lastSpace : i;
                used += wide;
                if (wordWrap && c == ' ') lastSpace = i + 1;
            }
            return end;
        }

        private float advance(int rel, float used) {
            if (rel < 0 || rel >= text.length() || gone[rel]) return 0;
            char c = text.charAt(rel);
            if (c == '\n') return 0;
            if (c == '\t') {
                int spaces = getContainer() instanceof JTextArea area ? Math.max(1, area.getTabSize()) : 8;
                int tab = Math.max(1, metrics().charWidth(' ') * spaces);
                return ((int) (used / tab) + 1) * tab - used;
            }
            return metrics().stringWidth(text.substring(rel, rel + 1));
        }

        /**
         * The x after walking [from, to), drawing when a graphics is given. Hidden marks add nothing, so the word
         * that follows starts where the marks would have.
         */
        private float walk(float rowX, float x, int from, int to, Graphics2D g, float baseline) {
            int i = from;
            while (i < to) {
                int rel = i - origin;
                if (rel < 0 || rel >= text.length()) break;
                char c = text.charAt(rel);
                if (gone[rel] || c == '\n') { i++; continue; }
                if (c == '\t') {
                    x = rowX + (x - rowX) + advance(rel, x - rowX);
                    i++;
                    continue;
                }
                int j = i + 1;
                while (j < to) {
                    int next = j - origin;
                    if (next >= text.length() || gone[next] || text.charAt(next) == '\n' || text.charAt(next) == '\t') break;
                    j++;
                }
                // Who wrote what, in their colours: the piece split where the colour changes, each part drawn once in its
                // own (see Desktop.Paper.inkAt), and measured the same way whether drawn or not, so the caret lands on it.
                Desktop.Paper paper = getContainer() instanceof Desktop.Paper p ? p : null;
                Color plain = g == null ? null : g.getColor();
                for (int k = i; k < j; ) {
                    Desktop.Paper.Ink ink = paper == null ? null : paper.inkAt(k);
                    int stop = ink == null ? j : Math.max(k + 1, Math.min(j, ink.end()));
                    String part = text.substring(k - origin, stop - origin);
                    if (g != null) { g.setColor(ink != null && ink.colour() != null ? ink.colour() : plain); g.drawString(part, x, baseline); }
                    x += metrics().stringWidth(part);
                    k = stop;
                }
                if (g != null) g.setColor(plain);
                i = j;
            }
            return x;
        }

        private int rowOf(int pos) {
            if (rows == null || rows.length == 0) return 0;
            for (int i = 0; i < rows.length; i++) if (pos < rows[i]) return i;
            return rows.length - 1;
        }

        private void dirty() { fresh = false; laid = -1; }

        @Override public void insertUpdate(DocumentEvent event, Shape shape, ViewFactory factory) { dirty(); preferenceChanged(this, true, true); }
        @Override public void removeUpdate(DocumentEvent event, Shape shape, ViewFactory factory) { dirty(); preferenceChanged(this, true, true); }
        @Override public void changedUpdate(DocumentEvent event, Shape shape, ViewFactory factory) { dirty(); preferenceChanged(this, true, true); }

        @Override public float getPreferredSpan(int axis) {
            if (axis == X_AXIS) {
                int width = parentWidth();
                return width > 1 ? width : 8;
            }
            int width = laid > 1 ? laid : parentWidth();
            if (width > 1) layout(width);
            else if (rows == null) return lineHeight();
            return Math.max(1, rows.length) * lineHeight();
        }

        @Override public float getMinimumSpan(int axis) { return axis == X_AXIS ? 8 : lineHeight(); }
        @Override public float getMaximumSpan(int axis) { return axis == X_AXIS ? Integer.MAX_VALUE : getPreferredSpan(axis); }

        @Override public void setSize(float width, float height) {
            int wide = Math.max(1, (int) width);
            int before = rows == null ? -1 : rows.length;
            layout(wide);
            super.setSize(width, height);
            int after = rows == null ? 1 : rows.length;
            if (before != -1 && before != after) preferenceChanged(this, false, true);
        }

        @Override public void paint(Graphics graphics, Shape alloc) {
            Rectangle box = alloc instanceof Rectangle rectangle ? rectangle : alloc.getBounds();
            layout(Math.max(1, box.width));
            if (rows == null || rows.length == 0) return;
            Graphics2D g = (Graphics2D) graphics.create();
            // Drawn with Windows' own text smoothing (ClearType), as every other word in the window is: without it the page's
            // words came out in grey smoothing, soft beside the rest, worst on a coloured page (the owner, 2026-10-13: "the text
            // is still not sharp on a blue background").
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints instanceof java.util.Map<?, ?> map) g.addRenderingHints(map);
            if (getContainer() instanceof JTextComponent component) {
                g.setFont(component.getFont());
                g.setColor(component.getForeground());
            }
            FontMetrics drawn = metrics();
            int height = lineHeight();
            int at = getStartOffset();
            for (int row = 0; row < rows.length; row++) {
                walk(box.x, box.x, at, rows[row], g, box.y + row * height + drawn.getAscent());
                at = rows[row];
            }
            g.dispose();
        }

        @Override public Shape modelToView(int pos, Shape alloc, Position.Bias bias) throws BadLocationException {
            Rectangle box = alloc instanceof Rectangle rectangle ? rectangle : alloc.getBounds();
            layout(Math.max(1, box.width));
            if (pos < getStartOffset() || pos > getEndOffset()) throw new BadLocationException("outside the line", pos);
            int row = rowOf(pos);
            int from = row == 0 ? getStartOffset() : rows[row - 1];
            float x = walk(box.x, box.x, from, Math.min(pos, rows[row]), null, 0);
            return new Rectangle(Math.round(x), box.y + row * lineHeight(), 1, lineHeight());
        }

        @Override public int viewToModel(float x, float y, Shape alloc, Position.Bias[] bias) {
            if (bias != null && bias.length > 0) bias[0] = Position.Bias.Forward;
            Rectangle box = alloc instanceof Rectangle rectangle ? rectangle : alloc.getBounds();
            layout(Math.max(1, box.width));
            if (rows == null || rows.length == 0) return getStartOffset();
            int height = lineHeight();
            int row = (int) Math.floor((y - box.y) / (float) height);
            if (row < 0) row = 0;
            if (row >= rows.length) row = rows.length - 1;
            int from = row == 0 ? getStartOffset() : rows[row - 1];
            int to = rows[row];
            float at = box.x;
            for (int i = from; i < to; i++) {
                int rel = i - origin;
                if (rel >= 0 && rel < text.length() && (gone[rel] || text.charAt(rel) == '\n')) continue;
                float next = walk(box.x, at, i, i + 1, null, 0);
                if (x < (at + next) / 2f) return i;
                at = next;
            }
            if (to > from) {
                int rel = to - 1 - origin;
                if (rel >= 0 && rel < text.length() && text.charAt(rel) == '\n') return to - 1;
            }
            return to;
        }
    }
}
