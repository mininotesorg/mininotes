// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * The strip under the PC's bar that says a contact is in trouble (decision 188; see Troubles): one line, the first trouble
 * and how many more, and What to do, which opens what is wrong with each, what Mininotes does about it by itself, and what
 * the owner can do, with the buttons for it. Not there while all is well. Looked at again a while after each refresh and
 * every minute; when it finds trouble it had not seen, Mininotes tries again at once, by itself.
 */
final class DesktopTroubles extends JPanel {
    private final Desktop d;
    private final JLabel words=new JLabel();
    private List<Troubles.Trouble> shown=List.of();
    private final javax.swing.Timer soon,round;
    private boolean looking;

    DesktopTroubles(Desktop d) {
        super(new BorderLayout(12,0));this.d=d;
        setOpaque(true);setVisible(false);
        setBorder(BorderFactory.createEmptyBorder(6,16,6,16));
        words.setFont(DesktopUi.BODY.deriveFont(13f));words.setForeground(DesktopUi.INK);
        words.setIcon(new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g,int x,int y){DesktopIcons.draw((Graphics2D)g,"triangle-alert",x,y,16,DesktopMark.ink(NoteLine.Tone.FAILED.mark));}});
        words.setIconTextGap(8);
        add(words,BorderLayout.CENTER);
        JButton what=DesktopUi.button(Say.t("What to do"),this::open);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));right.setOpaque(false);right.add(what);add(right,BorderLayout.EAST);
        soon=new javax.swing.Timer(2000,e->look());soon.setRepeats(false);
        round=new javax.swing.Timer(60_000,e->look());round.start();
    }

    @Override protected void paintComponent(Graphics g) {
        Color warning=DesktopMark.ink(NoteLine.Tone.FAILED.mark);
        g.setColor(DesktopUi.mix(warning,DesktopUi.PAPER,0.9f));g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(DesktopUi.mix(warning,DesktopUi.PAPER,0.65f));g.fillRect(0,getHeight()-1,getWidth(),1);
    }

    /** A look a moment from now: after a refresh, many come together. */
    void soon(){soon.restart();}
    void stop(){soon.stop();round.stop();}

    private void look() {
        if(looking||d.closing)return;
        looking=true;
        d.disk.submit(()->Troubles.of(d.store),found->{
            looking=false;
            boolean fresh=false;
            for(Troubles.Trouble t:found){boolean seen=false;for(Troubles.Trouble s:shown)if(s.address().equals(t.address())&&s.kind()==t.kind())seen=true;fresh|=!seen;}
            shown=found;
            if(found.isEmpty()){if(isVisible()){setVisible(false);revalidate();}return;}
            String line=Troubles.line(found);
            words.setText(line+"  ·  "+Say.t("Mininotes keeps trying"));words.getAccessibleContext().setAccessibleName(line);
            if(!isVisible()){setVisible(true);revalidate();}
            repaint();
            // Put right by the app where it can (the owner: "it should be rectified by the app directly"): tried again now.
            if(fresh&&!d.offline)tryAgain();
        },e->looking=false);
    }

    private void tryAgain(){d.network.submit(()->{Post.again(d.context,d.store,d.keys);return null;},done->{},e->{});}

    /** What is wrong with each, what Mininotes does, what the owner can do, and the buttons for it. */
    void open() {
        JPanel body=DesktopUi.column();JDialog[] box={null};
        for(Troubles.Trouble t:shown) {
            JPanel card=DesktopUi.column();
            DesktopUi.add(card,DesktopUi.body(t.name()));DesktopUi.gap(card,4);
            DesktopUi.add(card,DesktopUi.note(t.wrong(),400,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f)));DesktopUi.gap(card,6);
            DesktopUi.add(card,DesktopUi.note(t.doing(),400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));DesktopUi.gap(card,6);
            DesktopUi.add(card,DesktopUi.note(Say.t("What you can do: {0}",t.yours()),400,DesktopUi.INK,DesktopUi.BODY.deriveFont(12.5f)));DesktopUi.gap(card,8);
            JButton again=DesktopUi.button(Say.t("Try now"),()->{tryAgain();d.status.setText(Say.t("Trying again…"));});
            JButton page=DesktopUi.button(Say.t("Their page"),()->{box[0].dispose();DesktopPeople.person(d,t.address(),t.name());});
            JButton scan=DesktopUi.button(Say.t("Scan their code…"),()->{box[0].dispose();d.scanCode(null);});
            DesktopUi.add(card,t.kind()==Troubles.Kind.STUCK||t.kind()==Troubles.Kind.LINKING?DesktopUi.actions(again,page,scan):DesktopUi.actions(again,page));
            DesktopUi.add(body,DesktopUi.card(null,card));DesktopUi.gap(body,8);
        }
        box[0]=DesktopUi.sheet(d.frame,"What to do",DesktopUi.scrolling(body),DesktopUi.footer(),true);DesktopUi.show(box[0],520,560);
    }

    /** What the strip says now, or null while it is not shown: what the tests read. */
    String said(){return isVisible()?words.getText():null;}
    List<Troubles.Trouble> troubles(){return shown;}
}
