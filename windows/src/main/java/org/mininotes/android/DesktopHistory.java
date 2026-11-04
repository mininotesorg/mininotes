package org.mininotes.android;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * History, from Sync and sharing (decision 305): what Mininotes did, newest first, a card for each day, the same lines as the
 * phone's. A note's or a file's line opens it; a device back opens nothing. Nothing private is ever in it (see History).
 */
final class DesktopHistory {
    private DesktopHistory(){}

    static void open(Desktop pad) {
        pad.disk.submit(()->pad.store.history(),got->{
            JDialog[] box={null};
            JPanel body=DesktopUi.column();
            if(got.isEmpty())DesktopUi.add(body,DesktopUi.note("Nothing here yet.",400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
            long now=System.currentTimeMillis();java.time.ZoneId zone=java.time.ZoneId.systemDefault();
            String day=null;JPanel lines=null;
            // Which relays are the owner's, so a line says "your relay" or "a public relay" (decision 322).
            java.util.List<String> own=Node.ownRelays(pad.context);
            for(NoteStore.HistoryShown one:got) {
                History.Line line=one.line();
                String under=History.day(line.at(),now,zone);
                if(!under.equals(day)) {
                    if(lines!=null){DesktopUi.add(body,DesktopUi.card(day,lines));DesktopUi.gap(body,12);}
                    day=under;lines=DesktopUi.column();
                }
                Runnable go=line.kind()==History.Kind.BACK?null:()->{box[0].dispose();
                    pad.openThing(new NoteStore.Branch(line.kind().file()?NoteStore.Branch.Kind.FILE:NoteStore.Branch.Kind.PAGE,line.thing(),"",one.title(),"",0,0,false));};
                DesktopUi.add(lines,row(History.said(line.kind(),one.title(),one.who(),line.road(),own),History.time(line.at(),zone),go));
            }
            if(lines!=null)DesktopUi.add(body,DesktopUi.card(day,lines));
            box[0]=DesktopUi.sheet(pad.frame,"History",DesktopUi.scrolling(body),null,true);
            DesktopUi.show(box[0],560,640);
        },pad::failed);
    }

    /** One line: what happened, and its time at the right; a click opens its thing where it has one. */
    private static JPanel row(String said,String time,Runnable go) {
        JPanel row=DesktopUi.row(DesktopUi.note(said,400,DesktopUi.INK,DesktopUi.BODY),DesktopUi.quiet(time));
        if(go==null)return row;
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));row.setToolTipText(Say.t("Open"));
        MouseAdapter click=new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))go.run();}};
        every(row,click);
        return row;
    }
    /** The same click on a row and on every part of it, so its words are the row too. */
    private static void every(Component part,MouseAdapter click) {
        part.addMouseListener(click);
        if(part instanceof Container holds)for(Component one:holds.getComponents())every(one,click);
    }
}
