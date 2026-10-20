// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The menu, pinned (the owner, 2026-10-13: "we should be able to pin the menu so that it stays permanently visible, on the
 * right of Recent, so Menu should be an element of Show on Home"): the ⋯ menu's own lines, made by the same code, as a column
 * down the right of the window that stays open. Built again a moment after anything changes, so it always says what the ⋯
 * menu would. A line with more under it (Colour, Language) opens that beside it, as in the menu.
 */
final class DesktopPinnedMenu extends JPanel {
    static final String SETTING="menuPinned";
    static final int WIDE=250;
    private final Desktop pad;
    private final JPanel rows=new JPanel();
    private final javax.swing.Timer again;

    DesktopPinnedMenu(Desktop pad) {
        super(new BorderLayout());this.pad=pad;
        setBackground(DesktopUi.SHELF);setPreferredSize(new Dimension(WIDE,0));
        setBorder(BorderFactory.createMatteBorder(0,1,0,0,DesktopUi.LINE));
        rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));rows.setOpaque(false);rows.setBorder(BorderFactory.createEmptyBorder(6,4,8,4));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(rows,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(top);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
        again=new javax.swing.Timer(150,e->build());again.setRepeats(false);
        setVisible(false);
    }

    /** Whether the menu is pinned: off until switched on in Show on Home. */
    static boolean wanted(Desktop pad){return "true".equals(pad.context.getSharedPreferences("settings",0).getString(SETTING,"false"));}

    /** Shown or put away as the setting says, and drawn again when shown. */
    void place() {
        boolean on=wanted(pad);
        if(on!=isVisible()){setVisible(on);if(getParent()!=null){getParent().revalidate();getParent().repaint();}}
        if(on)build();
    }
    /** Drawn again a moment from now: many changes at once make one drawing. */
    void soon(){if(isVisible())again.restart();}

    /** The ⋯ menu's lines, moved here from the menu the same code makes. */
    void build() {
        if(!isVisible())return;
        JPopupMenu menu=pad.mainMenu();
        rows.removeAll();
        for(Component one:menu.getComponents()) {
            JComponent line=one instanceof JMenu more?opener(more):(JComponent)one;
            // No shortcut written at the end of a line here: the keys work wherever the window is, and written they were
            // cut off at the column's edge.
            if(line instanceof JMenuItem item){lit(item);item.setAccelerator(null);}
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            Dimension size=line.getPreferredSize();line.setMaximumSize(new Dimension(Integer.MAX_VALUE,size.height));
            rows.add(line);
        }
        // As wide as its widest line, the text size's ladder included, within reason.
        int wide=rows.getPreferredSize().width+18;
        Dimension now=new Dimension(Math.max(WIDE,Math.min(360,wide)),0);
        if(!now.equals(getPreferredSize())){setPreferredSize(now);if(getParent()!=null)getParent().revalidate();}
        rows.revalidate();rows.repaint();
    }

    /** A line with more under it: opens what it holds beside it, as the menu does. */
    private static JMenuItem opener(JMenu more) {
        JMenuItem line=new JMenuItem(more.getText(),more.getIcon()){
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DesktopUi.QUIET);g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int x=getWidth()-22,y=getHeight()/2;g.drawPolyline(new int[]{x,x+4,x},new int[]{y-4,y,y+4},3);g.dispose();
            }
        };
        line.getAccessibleContext().setAccessibleName(more.getAccessibleContext().getAccessibleName());
        line.addActionListener(e->SwingUtilities.invokeLater(()->{JPopupMenu held=more.getPopupMenu();held.show(line,line.getWidth()-8,0);}));
        return line;
    }

    /** Lit under the pointer as a line of a menu is: out of a menu, Swing does not do it by itself. */
    private static void lit(JMenuItem item) {
        item.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){if(item.isEnabled())item.setArmed(true);}
            @Override public void mouseExited(MouseEvent e){item.setArmed(false);}
        });
    }
}
