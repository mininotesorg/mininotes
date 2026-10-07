// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Recent, down the right of the window (the owner, 2026-10-03: "a set of open elements we can go from one to the other
 * directly ... on the side right"; then 2026-10-04: "we would get rid of open and keep only recent", the reordering
 * accepted): one line for each note and collection opened lately, newest first, its face and its name, the one in front
 * marked; a click goes to it. Nothing is closed: what was opened falls out by itself, after as long as Recent keeps
 * things (docs/HOME.md, decisions 77 and 86).
 */
final class DesktopOpenList extends JPanel {
    private final Desktop pad;
    private final JPanel lines=DesktopUi.column();
    /** How many lines at most: what was lately in hand, not a second tree. */
    private static final int MOST=30;
    static final int WIDE=210;

    DesktopOpenList(Desktop pad) {
        super(new BorderLayout());this.pad=pad;
        // The line before it is the grip's (decision 83), so it has no edge of its own.
        setBackground(DesktopUi.SHELF);
        setPreferredSize(new Dimension(WIDE,0));
        JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.setBorder(BorderFactory.createEmptyBorder(10,12,6,8));
        JLabel title=new JLabel("Recent");title.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,13f));title.setForeground(DesktopUi.QUIET);
        head.add(title,BorderLayout.WEST);
        add(head,BorderLayout.NORTH);
        lines.setOpaque(false);lines.setBorder(BorderFactory.createEmptyBorder(0,6,8,6));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(lines,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(top);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll);
    }

    /** Drawn again from Recent: names and faces read on the disk, newest first, the one in front marked. Hidden with nothing in it. */
    void refresh() {
        Overview.Open front=pad.inFront();
        long since=System.currentTimeMillis()-pad.recentDays()*86_400_000L;
        pad.disk.submit(()->{
            List<NoteStore.Branch> looks=new ArrayList<>(pad.store.recent(since));
            if(looks.size()>MOST)looks=new ArrayList<>(looks.subList(0,MOST));
            pad.store.dress(looks);
            return looks;
        },got->{
            @SuppressWarnings("unchecked") List<NoteStore.Branch> looks=(List<NoteStore.Branch>)got;
            lines.removeAll();
            for(NoteStore.Branch look:looks) {
                Overview.Open one=new Overview.Open(look.kind==NoteStore.Branch.Kind.PAGE?Overview.Kind.NOTE:Overview.Kind.COLLECTION,look.id);
                lines.add(line(one,look,one.equals(front)));
            }
            boolean was=isVisible(),wanted=pad.openListWanted()&&lines.getComponentCount()>0;
            setVisible(wanted);
            lines.revalidate();lines.repaint();revalidate();repaint();
            if(was!=wanted)pad.placeOpenList();
        },e->{});
    }

    /** One thing opened lately: its face, its name, marked while it is the one in front; a click goes to it. */
    private JComponent line(Overview.Open one,NoteStore.Branch look,boolean front) {
        JPanel row=new JPanel(new BorderLayout(8,0)){
            @Override protected void paintComponent(Graphics g0) {
                if(!front&&!Boolean.TRUE.equals(getClientProperty("over")))return;
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(front?DesktopUi.CARD:new Color(0,0,0,14));g.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                if(front){g.setColor(DesktopUi.ACCENT);g.fillRoundRect(0,6,3,getHeight()-12,3,3);}
                g.dispose();
            }
        };
        row.setOpaque(false);row.setBorder(BorderFactory.createEmptyBorder(5,8,5,4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));row.setAlignmentX(0f);
        JLabel face=new JLabel(DesktopHome.faceIcon(()->look,22,()->pad.usual));
        JLabel name=new JLabel(DesktopHome.named(look));name.setFont(DesktopUi.BODY.deriveFont(front?Font.BOLD:Font.PLAIN,13f));name.setForeground(DesktopUi.INK);
        row.add(face,BorderLayout.WEST);row.add(name);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText((one.kind==Overview.Kind.NOTE?"The note ":"The folder ")+DesktopHome.named(look));
        MouseAdapter hand=new MouseAdapter(){
            public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))pad.save(()->pad.goTo(one));}
            public void mouseEntered(MouseEvent e){row.putClientProperty("over",true);row.repaint();}
            public void mouseExited(MouseEvent e){row.putClientProperty("over",false);row.repaint();}
        };
        row.addMouseListener(hand);face.addMouseListener(hand);name.addMouseListener(hand);
        return row;
    }
}
