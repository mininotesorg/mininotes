package org.mininotes.android;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;

/**
 * A scroll bar that is not seen (the owner, 2026-10-05: "let's use an invisible scrolling bar on laptop"): its strip is
 * the colour of what scrolls beside it, so a page washed in a colour runs to the edge with no cream band down its side,
 * and its handle shows only while the pointer is on the strip or it is being dragged. The wheel, the keys and a drag
 * scroll as they did.
 */
public final class DesktopScroll extends com.formdev.flatlaf.ui.FlatScrollBarUI {
    public static ComponentUI createUI(JComponent c){return new DesktopScroll();}

    /** The colour of what this bar scrolls: the thing in view if it paints itself, else the view port, else the pane. */
    @SuppressWarnings("unchecked")
    private static Color beside(JComponent bar) {
        if(bar.getParent() instanceof JScrollPane pane) {
            // A pane that paints no ground of its own, inside something that does (a folder's card): that says its colour.
            Object ground=pane.getClientProperty("ground");
            if(ground instanceof java.util.function.Supplier<?> says&&says.get() instanceof Color colour)return colour;
            if(ground instanceof Color colour)return colour;
            Component view=pane.getViewport()==null?null:pane.getViewport().getView();
            if(view!=null&&view.isOpaque())return view.getBackground();
            if(pane.getViewport()!=null&&pane.getViewport().isOpaque())return pane.getViewport().getBackground();
            return pane.getBackground();
        }
        return bar.getBackground();
    }

    @Override protected Color getTrackColor(JComponent c,boolean hover,boolean pressed){return beside(c);}

    @Override protected Color getThumbColor(JComponent c,boolean hover,boolean pressed) {
        Color ground=beside(c);
        if(!hoverTrack&&!hoverThumb&&!hover&&!pressed&&!isDragging)return ground;
        return DesktopUi.mix(ground,DesktopUi.INK,pressed||isDragging?0.45f:0.3f);
    }
}
