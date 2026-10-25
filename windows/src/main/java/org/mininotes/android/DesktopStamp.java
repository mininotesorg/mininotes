// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * Stamp on Minima and Verify on Minima on the PC (decision 199; see Integritas): a file, several ticked, or a note. Linked
 * once with the person's own Integritas key; what goes is the fingerprint alone.
 *
 * <p>The owner, 2026-10-19 (decision 218): "we need a full communication box, showing everything, the transaction of encryption
 * on the blockchain, the hashes, the date, the block and so on, and there should be a mention at the top of the note for all
 * this, and as soon as there is a change, this should be notified; all stamped versions of the notes should be easily
 * retrievable". So stamping is a box, centred, that shows each step as it happens: what is fingerprinted and its SHA-256,
 * sent to Integritas and its request, waiting for Minima and how often it is looked for, then the block, the date recorded
 * and the transaction. Closing it stops nothing: {@link Integritas.Watch} goes on looking, and says so, centred, when it lands.
 */
final class DesktopStamp {
    private DesktopStamp(){}

    static final String KEY=Integritas.KEY;

    // ---- the watch, one for each notebook -----------------------------------------------------------------------------

    private static final Map<NoteStore,Integritas.Watch> WATCHES=new java.util.WeakHashMap<>();
    /** How long a second of waiting is, in milliseconds: shorter in a test. */
    static long unit=1000;

    /** The notebook's watch: started once, telling this window what lands, and the note's line kept up with it. */
    static synchronized Integritas.Watch watch(Desktop d) {
        Integritas.Watch w=WATCHES.get(d.store);
        if(w!=null)return w;
        Integritas.Watch made=new Integritas.Watch(d.store,unit);WATCHES.put(d.store,made);
        made.listen((row,landed,trouble)->SwingUtilities.invokeLater(()->{
            if(!d.frame.isDisplayable())return;
            String name=named(row);
            if(landed)Post.stampsChanged(d.context,d.store,d.keys);
            if(landed)notice(d,Say.t("{0} is on Minima: block {1}",name,String.valueOf(row.block())));
            else if(NoteStore.STAMP_UNSEEN.equals(row.state()))notice(d,Say.t("{0} was not found on Minima in two hours. Stamped on Minima… can look again.",name));
            d.stampLine.again();if(d.fileCards!=null)d.fileCards.again();
            // A file found on Minima wears it on its icon (decision 249).
            if(landed&&"file".equals(row.kind()))d.refreshSoon();
        }));
        return made;
    }
    /** At a start: those still waiting for Minima looked for again. */
    static void resume(Desktop d){watch(d).resume();}

    /** The notices given, the last 20, for a test to read. */
    static final java.util.Deque<String> said=new java.util.concurrent.ConcurrentLinkedDeque<>();
    /** A notice in the middle of what is in front: the window, or the box open over it (decision 218). */
    static void notice(Desktop d,String words) {
        said.add(words);if(said.size()>20)said.poll();Window front=d.frame;
        for(Window w:d.frame.getOwnedWindows())if(w.isShowing()&&w.isFocused())front=w;
        if(front==d.frame)for(Window w:d.frame.getOwnedWindows())if(w.isShowing()&&w instanceof Dialog box&&box.isModal())front=w;
        DesktopUi.centred(front,words);
    }

    private static String named(NoteStore.StampRow row){return "“"+(row.name().isEmpty()?Say.t("Untitled"):row.name())+"”";}

    /**
     * The words of the stamping box and of Stamped on Minima…, labels and values alike, at the size of the buttons' words (the
     * owner, 2026-10-19: "The text is written a bit small in this window; make it the same size as the buttons' text"; decision
     * 224): the look's own button font, which follows the app's text size.
     */
    static Font words(){Font f=UIManager.getFont("Button.font");return f!=null?f:DesktopUi.BODY;}
    /** A fingerprint, a request, a transaction: letters of one width, at the same size. */
    static Font mono(){return new Font(Font.MONOSPACED,Font.PLAIN,Math.round(words().getSize2D()));}
    /** How wide the words of the box go before they wrap. */
    static final int WIDE=500;
    /** Words of the box: at the buttons' size, wrapped at its width. */
    static DesktopUi.Text said(String text,Color colour){return DesktopUi.note(text,WIDE,colour,words());}
    /**
     * A value whole, wrapped where the box ends rather than cut (the owner's screenshot showed a fingerprint whose start was
     * gone): letters of one width, broken anywhere, selectable; where the Minima Explorer shows it, in the accent and clicked
     * to open it there (decision 224).
     */
    static DesktopUi.Text value(String text,int width,String link,Desktop d) {
        DesktopUi.Text shown=DesktopUi.note(text,width,link!=null?Desktop.ACCENT:DesktopUi.INK,mono());shown.setWrapStyleWord(false);
        if(link!=null) {
            shown.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));shown.setToolTipText(Say.t("Open on the Minima Explorer"));shown.putClientProperty("link",link);
            shown.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(shown.getSelectedText()==null)explore(d,link);}});
        }
        return shown;
    }
    /** A page of the Minima Explorer opened in the browser; its address copied where there is none. */
    static void explore(Desktop d,String link){try{Desktop.browse(link);}catch(Exception noBrowser){copy(link);notice(d,Say.t("The link is copied"));}}

    // ---- the account --------------------------------------------------------------------------------------------------

    /**
     * The account's settings, all of them in one place: My profile, at its part Stamping, the key's field in hand, opened in
     * front and closing nothing (the owner, 2026-10-21: "Your Integritas account needs a window with the full
     * settings, where we have the option to set it across, actually we need to be brought on the dedicated section on the
     * profile page"; decision 231). Where something is to be stamped or verified with no key yet, the same place.
     */
    static void settings(Desktop d,java.awt.Window over){DesktopProfile.open(d,over,null,DesktopProfile.STAMPING);}
    /** Integritas linked or unlinked, said in the middle of the window (decision 218); My profile's Save says it the same way. */
    static void linked(Desktop d,boolean on){notice(d,Say.t(on?"Integritas is linked":"Integritas is unlinked"));}

    // ---- stamping -----------------------------------------------------------------------------------------------------

    /** What is about to be stamped: a note's saved title and text, kept as they are, or a file's bytes, in Mininotes or not. */
    record Subject(String id,String kind,String name,String hash,long size,String title,String words) {
        boolean note(){return Integritas.NOTE.equals(kind)||Integritas.TITLED.equals(kind);}
        boolean outside(){return Integritas.OUTSIDE.equals(kind);}
    }

    /**
     * What a stamp is, in a word or three, in its box and in the list (decision 230): a note, its title and text; a note stamped
     * before titles were included, its text only, said so, since renaming it does not change that stamp; a file kept in
     * Mininotes; and a file of this PC's drive, never put in Mininotes.
     */
    static String kind(String kind) {
        if(Integritas.TITLED.equals(kind))return Say.t("Note");
        if(Integritas.NOTE.equals(kind))return Say.t("Note, text only");
        if(Integritas.OUTSIDE.equals(kind))return Say.t("File on this PC");
        return Say.t("File");
    }

    /** These files stamped: one in its box; several asked once, each then waiting for Minima, all in Stamped on Minima. */
    static void files(Desktop d,List<NoteStore.Held> files) {
        if(files.isEmpty())return;
        if(files.size()==1){NoteStore.Held one=files.get(0);open(d,()->{byte[] bytes=bytesOf(d,one);return new Subject(one.id,"file",one.name,Integritas.hash(bytes),bytes.length,null,null);});return;}
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){settings(d,null);return;}
            String what=Say.t("{0} files",String.valueOf(files.size()));
            if(!DesktopUi.confirm(d.frame,Say.t("Stamp {0} on Minima?",what),Say.t("Only their fingerprints go to Integritas, which records them on Minima with today's date. The files stay on this PC."),Say.t("Stamp"),false))return;
            d.status.setText(Say.t("Stamping on Minima…"));
            d.network.submit(()->{
                int done=0;
                for(NoteStore.Held one:files){byte[] bytes=bytesOf(d,one);send(d,key,new Subject(one.id,"file",one.name,Integritas.hash(bytes),bytes.length,null,null));done++;}
                return done;
            },done->{d.status.setText(" ");notice(d,Say.t("{0} files are sent to Integritas, waiting for Minima",String.valueOf(done)));list(d,d.frame);d.refresh();if(d.fileCards!=null)d.fileCards.again();},d::failed);
        },d::failed);
    }
    private static byte[] bytesOf(Desktop d,NoteStore.Held file) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DesktopFiles.copyOut(d.context,d.store.fileFor(file.id).toPath(),bytes);return bytes.toByteArray();
    }

    /**
     * A note, as it is saved now, stamped in its box: its title, an empty line and its text, are
     * fingerprinted and kept (the owner, 2026-10-21: "Why integritas/stamping doesnt include the title of the note? I think it
     * should"; decision 230), so renaming it is a change too, as he wanted ("Yes, absolutely").
     */
    static void note(Desktop d,String id) {
        open(d,()->{NoteStore.Note note=d.store.get(id);if(note==null)return null;
            return new Subject(id,Integritas.TITLED,note.heading(),Integritas.hashOfNote(note.title,note.body),Integritas.noteWords(note.title,note.body).length(),note.title,note.body);});
    }

    /** Where a file of this PC is chosen: Windows' own box; a stand-in in a test, which cannot press its buttons. */
    static java.util.function.BiFunction<Component,String,Path> pick=DesktopPick::openOne;

    /**
     * A file of this PC's drive stamped, never put in Mininotes (the owner, 2026-10-21: the + "should give the option, from the
     * app or external from the drive"; decision 230): read here for its fingerprint, and only that kept, with its name. Its id is
     * its own, since it belongs to nothing in the notebook; Verify on Minima… asks for the file again.
     */
    static void outside(Desktop d,Component over) {
        Path file=pick.apply(over,"Choose a file to stamp");if(file==null)return;
        open(d,()->new Subject("outside:"+java.util.UUID.randomUUID(),Integritas.OUTSIDE,String.valueOf(file.getFileName()),hashOf(file),Files.size(file),null,null));
    }
    /** A file's SHA-256, read from the drive as it goes, so a large one is never held whole. */
    private static String hashOf(Path file) throws java.io.IOException {try(java.io.InputStream in=Files.newInputStream(file)){return Integritas.hash(in);}}

    private interface Making{Subject make() throws Exception;}

    /** The box for something about to be stamped, once its fingerprint is made; My profile at Stamping where there is no key yet. */
    private static void open(Desktop d,Making making) {
        d.disk.submit(()->{String key=d.store.secret(KEY);Subject s=key.isEmpty()?null:making.make();return new Object[]{key,s};},got->{
            if(((String)got[0]).isEmpty()){settings(d,null);return;}
            if(got[1]!=null)new Box(d,(Subject)got[1],0).show();
        },d::failed);
    }

    /** Sent to Integritas and kept, waiting for Minima, or found there at once where it already was. Its row. */
    static long send(Desktop d,String key,Subject s) throws Exception {
        Integritas.Stamped sent=Integritas.stamp(key,s.hash());
        long n=d.store.stamped(s.id(),s.kind(),s.name(),s.hash(),sent.at(),sent.request(),sent.message(),sent.address(),s.title(),s.words());
        Integritas.Block there=Integritas.chosen(sent.already(),sent.at());
        if(there!=null)d.store.stampLanded(n,there.number(),there.txpow(),there.tx(),there.date(),0,System.currentTimeMillis());
        else watch(d).watch(n,0);
        // To my other devices at once (decision 252).
        Post.stampsChanged(d.context,d.store,d.keys);
        // Its icon wears the stamp from now (decision 249).
        if("file".equals(s.kind()))SwingUtilities.invokeLater(d::refreshSoon);
        return n;
    }

    /**
     * Everything about stamping one note or file, from the one line of its menu, Stamp on Minima… (the owner, 2026-10-25: "a
     * combined windows with everything relating to horodating and only one element in the contextual menu of the note, so
     * combining the three menu elements we currently have and making sure I can atached the integritas pdf report";
     * decision 253): the box of what it is now, its fingerprint made here; where that version is stamped, its stamp, how far
     * and what can be done with it; where it is not, Stamp, and looking for it on Minima as somebody else may have stamped
     * the same; a file compared with a fingerprint; and the versions stamped before. No key is asked for to see it.
     */
    static void thing(Desktop d,String id,boolean note) {
        d.disk.submit(()->{
            Subject s;
            if(note){NoteStore.Note n=d.store.get(id);if(n==null)return null;
                s=new Subject(id,Integritas.TITLED,n.heading(),Integritas.hashOfNote(n.title,n.body),Integritas.noteWords(n.title,n.body).length(),n.title,n.body);}
            else{NoteStore.Held f=d.store.file(id);if(f==null)return null;byte[] bytes=bytesOf(d,f);s=new Subject(id,"file",f.name,Integritas.hash(bytes),bytes.length,null,null);}
            // The newest stamp of exactly this version, where there is one.
            long n=0;for(NoteStore.StampRow r:d.store.stampsOf(id))if(r.hash().equals(s.hash())){n=r.n();break;}
            return new Object[]{s,n};
        },got->{if(got!=null)new Box(d,(Subject)got[0],(Long)got[1]).show();},d::failed);
    }

    /** One stamp's box, of a row already kept: what it is, how far, and all that is known of it. */
    static void show(Desktop d,long n) {
        d.disk.submit(()->d.store.stampRow(n),row->{if(row!=null)new Box(d,null,n).show();},d::failed);
    }

    /**
     * The stamp's box (decision 218): the steps one under the other, each with its mark, drawn again whenever the watch hears
     * something and every second while waiting, so the time to the next look counts down. Not modal: the PC goes on.
     */
    static final class Box {
        final Desktop d;final Subject subject;long n;
        NoteStore.StampRow row;String problem;boolean sending;List<NoteStore.StampRow> before=List.of();
        final JDialog dialog;final JPanel body=DesktopUi.column(),foot=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        final javax.swing.Timer tick;final Integritas.Watch.Listener ear;

        Box(Desktop d,Subject subject,long n) {
            this.d=d;this.subject=subject;this.n=n;
            foot.setOpaque(false);
            JScrollPane scroll=DesktopUi.scrolling(body);scroll.setBorder(null);
            dialog=DesktopUi.sheet(d.frame,"Stamp on Minima",scroll,foot,false);dialog.setName("stampBox");
            tick=new javax.swing.Timer(1000,e->{if(row!=null&&row.waiting())draw();});
            ear=(heard,landed,trouble)->SwingUtilities.invokeLater(()->{if(heard.n()==this.n){row=heard;draw();}});
            dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){tick.stop();watch(d).unlisten(ear);}});
        }
        void show() {
            watch(d).listen(ear);
            if(n>0)read();else{load(subject.id());draw();}
            tick.start();
            DesktopUi.show(dialog,640,660,780);
            dialog.setLocationRelativeTo(d.frame);
        }
        private void read(){d.disk.submit(()->{NoteStore.StampRow r=d.store.stampRow(n);return new Object[]{r,r==null?List.of():d.store.stampsOf(r.id())};},got->{
            row=(NoteStore.StampRow)got[0];@SuppressWarnings("unchecked") List<NoteStore.StampRow> all=(List<NoteStore.StampRow>)got[1];before=others(all);draw();},d::failed);}
        private void load(String id){d.disk.submit(()->d.store.stampsOf(id),all->{before=others(all);draw();},d::failed);}
        private List<NoteStore.StampRow> others(List<NoteStore.StampRow> all){List<NoteStore.StampRow> out=new ArrayList<>();for(NoteStore.StampRow one:all)if(one.n()!=n)out.add(one);return out;}

        String hash(){return row!=null?row.hash():subject.hash();}
        String kind(){return row!=null?row.kind():subject.kind();}
        String name(){return row!=null?row.name():subject.name();}

        /** Everything drawn again from what is known now. */
        void draw() {
            body.removeAll();foot.removeAll();
            DesktopUi.Text head=DesktopUi.heading("“"+(name().isEmpty()?Say.t("Untitled"):name())+"”   ·   "+DesktopStamp.kind(kind()));head.setName("stampName");
            DesktopUi.add(body,head);DesktopUi.gap(body,DesktopUi.M);
            // Changed since its last stamp: said first, so Stamp is read as stamping this version (decision 253).
            if(row==null&&subject!=null&&!before.isEmpty()){DesktopUi.add(body,said(Say.t("This version is not stamped yet: it changed since it was last stamped. The stamped versions are under Stamped before."),DesktopUi.INK));DesktopUi.gap(body,DesktopUi.M);}
            // 1. The fingerprint, of exactly what.
            step(Mark.DONE,"1. The fingerprint, made on this PC");
            if(subject!=null)indent(said(subject.outside()?Say.t("The file exactly as it is on this PC: {0}. Only its fingerprint is kept, with its name: the file is not put in Mininotes.",Attachment.size(subject.size())):Integritas.what(subject.note(),subject.size()),DesktopUi.QUIET));
            // Each stamp said as it was made: its title and text from 0.4.018, its text alone before (decision 230).
            else if(row.titled())indent(said(Say.t(row.kept()?"The note's title and text as they were saved, in UTF-8. That exact text is kept with the stamp: Open this version shows it.":"The note's title and text as they were saved, in UTF-8."),DesktopUi.QUIET));
            else if(row.outside())indent(said(Say.t("The file exactly as it was on this PC. It was not put in Mininotes: Verify on Minima… asks for the file again."),DesktopUi.QUIET));
            else if(row.note())indent(said(Say.t(row.kept()?"The note's text as it was saved, without its title, in UTF-8. That exact text is kept with the stamp: Open this version shows it.":"The note's text as it was saved, without its title, in UTF-8."),DesktopUi.QUIET));
            else indent(said(Say.t("The file exactly as it was kept here."),DesktopUi.QUIET));
            indent(copyable("SHA-256",hash(),"stampHash",null));
            DesktopUi.gap(body,DesktopUi.M);
            // 2. Integritas.
            if(row==null) {
                step(sending?Mark.GOING:problem!=null?Mark.FAILED:Mark.NEXT,"2. Sent to Integritas");
                indent(said(sending?Say.t("Sending to Integritas…"):problem!=null?problem:Say.t("Only this fingerprint goes, through your own Integritas account, which records it on Minima with the date. Stamping uses one stamp of your account."),problem!=null&&!sending?DesktopUi.WARN:DesktopUi.QUIET));
                DesktopUi.gap(body,DesktopUi.M);
                step(Mark.NEXT,"3. Recorded on Minima");
            } else {
                step(Mark.DONE,"2. Sent to Integritas");
                indent(said(Say.t("Sent {0}",Integritas.when(row.at())),DesktopUi.QUIET));
                if(!row.request().isEmpty())indent(copyable(Say.t("Request"),row.request(),"stampRequest",null));
                if(!row.said().isEmpty())indent(said(Integritas.answered(row.said()),DesktopUi.QUIET));
                if(!row.address().isEmpty())indent(copyable(Say.t("Minima address"),row.address(),"stampAddress",Integritas.addressLink(row.address())));
                DesktopUi.gap(body,DesktopUi.M);
                Integritas.Watch w=watch(d);
                if(row.confirmed()) {
                    step(Mark.DONE,"3. Recorded on Minima");
                    indent(said(Say.t("Confirmed: the fingerprint is in block {0} of the Minima blockchain.",String.valueOf(row.block())),DesktopUi.INK));
                    // Each on the Minima Explorer where it can be shown there, as the PDF report links them (decision 224).
                    indent(copyable(Say.t("Block"),String.valueOf(row.block()),"stampBlock",Integritas.blockLink(row.block())));
                    if(!row.recorded().isEmpty())indent(copyable(Say.t("Recorded"),Integritas.when(row.recorded()),"stampRecorded",null));
                    if(!row.txpow().isEmpty())indent(copyable(Say.t("Transaction (txpow)"),row.txpow(),"stampTxpow",Integritas.txpowLink(row.txpow())));
                    if(!row.tx().isEmpty())indent(copyable(Say.t("Transaction id"),row.tx(),"stampTx",null));
                    if(!row.nft().isEmpty())indent(copyable(Say.t("NFT coin id"),row.nft(),"stampNft",null));
                    if(!row.nftTx().isEmpty())indent(copyable(Say.t("NFT transaction"),row.nftTx(),"stampNftTx",Integritas.txpowLink(row.nftTx())));
                } else if(row.waiting()) {
                    step(Mark.GOING,"3. Waiting for Minima");
                    long next=w.nextLook(row.n());long in=next<=0?0:Math.max(0,(next-System.currentTimeMillis()+999)/1000);
                    indent(said(Say.t("The fingerprint is in a transaction on its way into a block, usually within a few minutes. This PC looks for it, less and less often, even with this box closed, and says when it is there."),DesktopUi.QUIET));
                    String looked=row.tries()==0?Say.t("Not looked for yet."):Say.t("Looked for {0} times, last at {1}.",String.valueOf(row.tries()),Integritas.when(row.checked()));
                    indent(said(looked+"  "+(next<=0?Say.t("Looking now…"):Say.t("Next look in {0} s.",String.valueOf(in))),DesktopUi.INK));
                    String trouble=w.trouble(row.n());
                    if(trouble!=null)indent(said(Say.t(trouble),DesktopUi.WARN));
                } else {
                    step(Mark.FAILED,"3. Not found on Minima yet");
                    indent(said(Say.t("It was looked for {0} times, for about two hours, and is not on Minima yet. Verify on Minima looks once more and goes on looking.",String.valueOf(row.tries())),DesktopUi.QUIET));
                }
            }
            buttons();
            // Earlier stamps of the same note or file: each version stamped, one click away.
            if(!before.isEmpty()) {
                DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.quiet(Say.t("Stamped before")));DesktopUi.gap(body,4);
                for(NoteStore.StampRow one:before) {
                    JButton go=DesktopUi.quietButton(Integritas.when(one.at())+"   ·   "+state(one)+"  ›",()->{dialog.dispose();new Box(d,null,one.n()).show();});
                    go.setFont(words());DesktopUi.add(body,go);
                }
            }
            body.revalidate();body.repaint();foot.revalidate();foot.repaint();
        }

        private void buttons() {
            if(row==null) {
                // What can be done without stamping: look for it on Minima, as somebody may have stamped the same; and a file
                // compared with a fingerprint the person has (decision 253).
                List<JComponent> other=new ArrayList<>();
                if(subject!=null&&!subject.outside()){JButton look=DesktopUi.button("Look for it on Minima",()->{if(subject.note())verifyNote(d,subject.id());else d.disk.submit(()->d.store.file(subject.id()),f->{if(f!=null)verifyFile(d,f);},d::failed);});look.setName("stampLook");other.add(look);}
                if(subject!=null&&!subject.note()&&!subject.outside())other.add(compareButton());
                if(!other.isEmpty()){DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.actions(other.toArray(new JComponent[0])));}
                JButton stamp=DesktopUi.primary(problem!=null?"Try again":"Stamp",this::stamp);stamp.setName("stampGo");stamp.setEnabled(!sending);
                foot.add(DesktopUi.button("Cancel",dialog::dispose));foot.add(stamp);
                dialog.getRootPane().setDefaultButton(stamp);
                return;
            }
            // What can be done with it, under its steps; the box's own row keeps Copy the details and Done.
            List<JComponent> more=new ArrayList<>();
            if(row.note()&&row.kept()){JButton version=DesktopUi.button("Open this version",()->version(d,row.n()));version.setName("stampVersion");more.add(version);}
            if(row.confirmed()){JButton report=DesktopUi.button("Open the PDF report",()->report(d,row,this::read));report.setName("stampReport");more.add(report);}
            JButton again=verifyButton(d,row);again.setName("stampAgain");more.add(again);
            // The report kept with it, sealed as its files are, travelling as they do (decision 253).
            if(row.confirmed()&&!row.outside()){JButton keep=DesktopUi.button(row.note()?"Attach the PDF report":"Keep the PDF report beside it",()->attachReport(d,row));keep.setName("stampAttach");more.add(keep);}
            if(!row.note()&&!row.outside())more.add(compareButton());
            DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.actions(more.toArray(new JComponent[0])));
            JButton copy=DesktopUi.button("Copy the details",()->{copy(Integritas.details(row));notice(d,Say.t("Copied"));});copy.setName("stampCopy");foot.add(copy);
            JButton done=DesktopUi.primary("Done",dialog::dispose);foot.add(done);dialog.getRootPane().setDefaultButton(done);
        }

        /** Compare with a fingerprint…, for the file this box is about. */
        private JButton compareButton() {
            String id=row!=null?row.id():subject.id();
            JButton b=DesktopUi.button("Compare with a fingerprint…",()->d.disk.submit(()->d.store.file(id),f->{if(f!=null)compareFile(d,f);},d::failed));b.setName("stampCompare");return b;
        }

        /** The one step that costs a stamp, pressed: sent, kept, and watched; the box shows each part as it comes. */
        void stamp() {
            sending=true;problem=null;draw();
            // No account linked yet: My profile at Stamping, in front of this box, which stays (decision 253).
            d.disk.submit(()->d.store.secret(KEY),key->{if(key.isEmpty()){sending=false;draw();settings(d,dialog);return;}d.network.submit(()->send(d,key,subject),made->{
                sending=false;n=made;
                d.disk.submit(()->d.store.stampRow(made),r->{row=r;draw();d.stampLine.again();if(d.fileCards!=null)d.fileCards.again();
                    notice(d,r!=null&&r.confirmed()?Say.t("{0} is on Minima: block {1}",named(r),String.valueOf(r.block())):Say.t("Sent to Integritas, waiting for Minima"));},d::failed);
            },e->{sending=false;problem=Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage());draw();});},d::failed);
        }

        private void step(Mark mark,String words) {
            JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));line.setOpaque(false);
            JLabel sign=new JLabel(mark.sign);sign.setForeground(mark.colour);sign.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,14f));sign.setPreferredSize(new Dimension(24,20));
            DesktopUi.Text said=DesktopUi.heading(words);said.setForeground(mark==Mark.NEXT?DesktopUi.QUIET:DesktopUi.INK);
            line.add(sign);line.add(said);DesktopUi.add(body,line);DesktopUi.gap(body,4);
        }
        /** Under its step, at its own width: words wrapped where they were measured, never cut at the box's edge. */
        private void indent(JComponent c){JPanel in=new JPanel(new BorderLayout());in.setOpaque(false);in.setBorder(BorderFactory.createEmptyBorder(0,24,3,0));in.add(c,c instanceof DesktopUi.Text?BorderLayout.WEST:BorderLayout.CENTER);DesktopUi.add(body,in);}
        /**
         * A label with Copy beside it, and the Minima Explorer where it shows the value; under them the value whole, wrapped,
         * in letters of one width (decision 224).
         */
        private JComponent copyable(String label,String value,String name,String link){return DesktopStamp.copyable(d,label,value,name,link,WIDE-24);}
    }
    /** A label, Copy, and where it can be seen the Minima Explorer; under them the value whole: in the box and in the list. */
    static JComponent copyable(Desktop d,String label,String value,String name,String link,int width) {
        JPanel all=DesktopUi.column();
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));top.setOpaque(false);
        JLabel what=new JLabel(label);what.setForeground(DesktopUi.QUIET);what.setFont(words());what.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));top.add(what);
        JButton copy=DesktopUi.quietButton("Copy",()->{copy(value);notice(d,Say.t("Copied"));});copy.setFont(words());copy.setName(name+"Copy");top.add(copy);
        if(link!=null){JButton there=DesktopUi.quietButton("Minima Explorer  ›",()->explore(d,link));there.setFont(words());there.setName(name+"Link");there.setToolTipText(link);top.add(there);}
        DesktopUi.add(all,top);
        DesktopUi.Text shown=value(value,width,link,d);shown.setName(name);DesktopUi.add(all,shown);DesktopUi.gap(all,4);
        return all;
    }
    /**
     * Verify on Minima, for a stamp not found there yet, its first look (the owner, 2026-10-19; decision 224); Verify again once
     * it has been found, which checks it there anew with the PDF report.
     */
    static JButton verifyButton(Desktop d,NoteStore.StampRow row) {
        // A file of the drive is not here to be read again: it is asked for, and checked against the stamp first (decision 230).
        if(row.outside())return DesktopUi.button("Verify on Minima…",()->verifyOutside(d,row));
        return DesktopUi.button(row.confirmed()?"Verify again":"Verify on Minima",()->{if(row.confirmed())verifyRow(d,row);else{watch(d).now(row.n());notice(d,Say.t("Looking for it on Minima…"));}});
    }
    /** A step's mark: done, going on, failed, or still to come. */
    enum Mark {
        DONE("✓",new Color(46,125,80)),GOING("…",Desktop.ACCENT),FAILED("!",DesktopUi.WARN),NEXT("○",DesktopUi.LINE);
        final String sign;final Color colour;
        Mark(String sign,Color colour){this.sign=sign;this.colour=colour;}
    }
    /** How far a stamp is, in a few words. */
    static String state(NoteStore.StampRow s) {
        if(s.confirmed())return Say.t("On Minima, block {0}",String.valueOf(s.block()));
        return Say.t(s.waiting()?"Waiting for Minima":"Not found on Minima yet");
    }

    static void copy(String text){Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text),null);}

    /** The PDF report Integritas makes of a verification, its link good for about an hour, opened in the browser. */
    static void report(Desktop d,NoteStore.StampRow row,Runnable then) {
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){settings(d,null);return;}
            notice(d,Say.t("Asking Integritas for the report…"));
            // The NFT it mints for the report kept with the stamp, to be seen on the Minima Explorer (decision 224).
            d.network.submit(()->{Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);d.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(d).told(row.hash());Post.stampsChanged(d.context,d.store,d.keys);return f;},found->{
                if(then!=null)then.run();
                if(found.report()==null||found.report().isEmpty()){notice(d,Say.t("Integritas gave no report this time. Try again in a moment."));return;}
                try{Desktop.browse(found.report());}catch(Exception noBrowser){copy(found.report());notice(d,Say.t("The report's link is copied"));}
            },e->notice(d,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage())));
        },d::failed);
    }

    /**
     * The PDF report of a stamp, asked of Integritas and kept (decision 253): attached to the note stamped, or, for a file, kept
     * beside it, where it is (attached to the same note, or in the same folder). Kept as every file is, sealed when the notebook
     * is locked, and going where the note or the folder goes.
     */
    static void attachReport(Desktop d,NoteStore.StampRow row) {
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){settings(d,null);return;}
            notice(d,Say.t("Asking Integritas for the report…"));
            d.network.submit(()->{
                Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);d.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(d).told(row.hash());Post.stampsChanged(d.context,d.store,d.keys);
                if(f.report()==null||f.report().isEmpty())throw new java.io.IOException("Integritas gave no report this time. Try again in a moment.");
                return Integritas.download(f.report());
            },pdf->d.disk.submit(()->keepReport(d,row,pdf),where->{if(where.isEmpty())d.refreshSoon();else d.attachmentsChanged(where);
                notice(d,Say.t(row.note()?"The report is attached to the note":"The report is kept beside the file"));},d::failed),
            e->notice(d,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage())));
        },d::failed);
    }
    /** The report written in: the note it is attached to, or empty where it went in the file's folder. On the disk thread. */
    static String keepReport(Desktop d,NoteStore.StampRow row,byte[] pdf) throws Exception {
        String name=Integritas.reportName(row.name()),where;
        if(row.note())where=row.id();
        else {
            NoteStore.Held f=d.store.file(row.id());if(f==null)throw new java.io.IOException("The file is not here any more.");
            if(f.held!=NoteStore.Branch.Kind.PAGE){d.store.addFileExact(f.note,name,"application/pdf",new java.io.ByteArrayInputStream(pdf));return "";}
            where=f.note;
        }
        String id=java.util.UUID.randomUUID().toString();DesktopFiles.keep(d.context,pdf,d.store.fileFor(id).toPath());
        d.store.keep(d.store.settle(new NoteStore.Held(id,where,name,Attachment.kind("application/pdf"),pdf.length,System.currentTimeMillis())));
        return where;
    }

    /**
     * A file of this PC stamped, picked again and checked: its SHA-256 made here and compared with the stamp's before anything
     * is asked of Integritas, then looked for on Minima, with the PDF report.
     */
    static void verifyOutside(Desktop d,NoteStore.StampRow row) {
        Path file=pick.apply(d.frame,"Choose the file that was stamped");if(file==null)return;
        verify(d,row.name(),()->{String hash=hashOf(file);
            if(!hash.equals(row.hash()))throw new java.io.IOException(Say.t("{0} is not the file stamped: its fingerprint is another, so it changed since or it is another file.","“"+file.getFileName()+"”"));
            return new Object[]{hash,Files.size(file)};});
    }

    /** A kept stamp checked again on Minima by its fingerprint, as it was stamped. */
    static void verifyRow(Desktop d,NoteStore.StampRow row) {
        verify(d,row.name(),()->new Object[]{row.hash(),-1L});
    }

    // ---- a stamped version --------------------------------------------------------------------------------------------

    /**
     * A note's stamped version, read only (decision 218): the exact text whose fingerprint is on Minima, checked again here
     * against it, with Copy, and Put this version back, which keeps what the note says now as a version first.
     */
    static void version(Desktop d,long n) {
        d.disk.submit(()->new Object[]{d.store.stampRow(n),d.store.stampedWords(n)},got->{
            NoteStore.StampRow row=(NoteStore.StampRow)got[0];String[] words=(String[])got[1];
            if(row==null||words==null){notice(d,Say.t("This stamp was made before Mininotes kept the stamped text."));return;}
            // In the form it was stamped in: its title and text, or its text alone before 0.4.018 (decision 230).
            boolean same;try{same=(row.titled()?Integritas.hashOfNote(words[0],words[1]):Integritas.hashOfText(words[1])).equals(row.hash());}catch(Exception never){same=false;}
            JPanel body=new JPanel(new BorderLayout(0,8));body.setOpaque(false);
            JPanel top=DesktopUi.column();
            DesktopUi.add(top,DesktopUi.note(Say.t("Stamped {0}",Integritas.when(row.at()))+"   ·   "+state(row),520,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f)));
            DesktopUi.add(top,DesktopUi.note(same?Say.t("Its fingerprint is the one stamped: SHA-256 {0}",row.hash()):Say.t("This text does not give the fingerprint stamped."),520,same?DesktopUi.QUIET:DesktopUi.WARN,DesktopUi.BODY.deriveFont(12f)));
            body.add(top,BorderLayout.NORTH);
            JTextArea text=new JTextArea(words[1],18,52);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setName("stampedText");
            text.setFont(DesktopUi.BODY.deriveFont(15f));text.setBackground(DesktopUi.CARD);text.setMargin(new Insets(12,14,12,14));text.setCaretPosition(0);
            JScrollPane scroll=new JScrollPane(text);scroll.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));body.add(scroll);
            JDialog[] box={null};
            JButton copy=DesktopUi.button("Copy",()->{copy(words[1]);notice(d,Say.t("Copied"));});
            JButton back=DesktopUi.primary("Put this version back",()->{box[0].dispose();d.putStampedBack(row.n(),row.id(),words[0],words[1]);});back.setName("stampPutBack");
            String heading=words[0].isEmpty()?row.name():words[0];
            box[0]=DesktopUi.sheet(d.frame,Say.t("Stamped version of {0}","“"+(heading.isEmpty()?Say.t("Untitled"):heading)+"”"),body,DesktopUi.footer(copy,back),false);box[0].setName("stampedVersion");
            DesktopUi.show(box[0],720,600,760);
        },d::failed);
    }

    // ---- verifying ----------------------------------------------------------------------------------------------------

    /** A file looked for on Minima, with the PDF report. */
    /**
     * A file compared with a fingerprint the person has, from whoever sent it or from anywhere (decision 250): its own
     * SHA-256, made here from its bytes, and a field to paste the other, filled from the clipboard where one is there; the
     * answer as it is typed. Nothing leaves this PC, and no account is needed.
     */
    static void compareFile(Desktop d,NoteStore.Held file) {
        d.disk.submit(()->Integritas.hash(bytesOf(d,file)),mine->{
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.body(Say.t("Paste the fingerprint (SHA-256) you were given for {0}.","“"+file.name+"”")));DesktopUi.gap(body,DesktopUi.S);
            DesktopUi.add(body,copyable(d,Say.t("This file's fingerprint (SHA-256)"),mine,"compareMine",null,WIDE));DesktopUi.gap(body,DesktopUi.S);
            JTextField theirs=new JTextField();theirs.setName("compareTheirs");
            theirs.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,Say.t("The fingerprint you have"));
            theirs.getAccessibleContext().setAccessibleName(Say.t("The fingerprint you have"));
            try{Object clip=Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if(clip instanceof String text&&Integritas.fingerprintIn(text)!=null)theirs.setText(text.trim());}catch(Exception noText){/* typed instead */}
            DesktopUi.add(body,theirs);DesktopUi.gap(body,DesktopUi.S);
            DesktopUi.Text answer=said(" ",DesktopUi.QUIET);answer.setName("compareAnswer");
            Runnable judge=()->{Boolean same=Integritas.sameFingerprint(mine,theirs.getText());
                answer.setText(same==null?(theirs.getText().isBlank()?" ":Say.t("Not a SHA-256 fingerprint: 64 characters, 0 to 9 and a to f.")):same?Say.t("The same: this is exactly that file."):Say.t("Not the same: this is not that file, or it changed since."));
                answer.setForeground(same==null?DesktopUi.QUIET:same?DesktopUi.ACCENT:DesktopUi.WARN);};
            theirs.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                public void insertUpdate(javax.swing.event.DocumentEvent e){judge.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){judge.run();}
                public void changedUpdate(javax.swing.event.DocumentEvent e){judge.run();}});
            judge.run();DesktopUi.add(body,answer);
            JDialog[] box={null};
            box[0]=DesktopUi.sheet(d.frame,"Compare with a fingerprint…",body,DesktopUi.footer(DesktopUi.primary("Done",()->box[0].dispose())),true);
            DesktopUi.show(box[0],600,470);
        },d::failed);
    }

    static void verifyFile(Desktop d,NoteStore.Held file) {
        verify(d,file.name,()->{byte[] bytes=bytesOf(d,file);return new Object[]{Integritas.hash(bytes),(long)bytes.length};});
    }
    /**
     * A note, as it is now, looked for on Minima: its title and text first, as it is stamped from 0.4.018, and where Minima does
     * not have that, its text alone, as it was stamped before (decision 230); the box says which was found.
     */
    static void verifyNote(Desktop d,String id) {
        d.disk.submit(()->d.store.get(id),note->{if(note!=null)verify(d,note.heading(),()->new Object[]{Integritas.hashOfNote(note.title,note.body),-1L,Integritas.hashOfText(note.body)});},d::failed);
    }

    /** {hash, size}, and where there is another form it may have been stamped in, its hash third. */
    private interface Hashing{Object[] hash() throws Exception;}

    private static void verify(Desktop d,String name,Hashing hashing) {
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){settings(d,null);return;}
            notice(d,Say.t("Looking for it on Minima…"));
            d.network.submit(()->{Object[] h=hashing.hash();String first=(String)h[0],alone=h.length>2?(String)h[2]:null,how=null;
                // The text alone looked for only where the title and text are not on Minima, each look free; the one found then
                // verified with its report, so one NFT is minted, not two.
                if(alone!=null&&!alone.equals(first)&&!Integritas.check(key,first).match()&&Integritas.check(key,alone).match()){h[0]=alone;how=Say.t("Found as its text alone, stamped before titles were included.");}
                Integritas.Found f=Integritas.verify(key,(String)h[0],name,(Long)h[1],true);
                // Where it was found, kept with the stamps of the same fingerprint (decisions 203 and 218).
                if(f.match()&&!f.blocks().isEmpty()){Integritas.Block b=Integritas.chosen(f.blocks(),0);if(b!=null)d.store.stampFound((String)h[0],b.number(),b.txpow(),b.date());}
                d.store.stampNft((String)h[0],f.nft(),f.nftTx());watch(d).told((String)h[0]);Post.stampsChanged(d.context,d.store,d.keys);
                return new Object[]{f,h[0],how};},got->{
                Integritas.Found found=(Integritas.Found)got[0];String hash=(String)got[1],how=(String)got[2];
                d.stampLine.again();
                JPanel body=DesktopUi.column();
                if(found.match()) {
                    DesktopUi.add(body,DesktopUi.body(Say.t("{0} is on Minima, unchanged.","“"+name+"”")));DesktopUi.gap(body,DesktopUi.S);
                    if(how!=null){DesktopUi.add(body,said(how,DesktopUi.QUIET));DesktopUi.gap(body,DesktopUi.S);}
                    for(Integritas.Block b:found.blocks()) {
                        DesktopUi.add(body,said(Say.t("Recorded {0}, block {1}",Integritas.when(b.date()),String.valueOf(b.number())),DesktopUi.INK));
                        if(!b.txpow().isEmpty())DesktopUi.add(body,copyable(d,Say.t("Transaction (txpow)"),b.txpow(),"verifyTxpow",Integritas.txpowLink(b.txpow()),WIDE));
                        DesktopUi.gap(body,4);
                    }
                    if(!found.nftTx().isEmpty())DesktopUi.add(body,copyable(d,Say.t("NFT transaction"),found.nftTx(),"verifyNftTx",Integritas.txpowLink(found.nftTx()),WIDE));
                } else {
                    DesktopUi.add(body,DesktopUi.body(Say.t("{0} is not on Minima as it is now.","“"+name+"”")));DesktopUi.gap(body,DesktopUi.S);
                    DesktopUi.add(body,said(Say.t("Either it was never stamped, or it changed since: even one letter makes another fingerprint."),DesktopUi.QUIET));
                }
                DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,copyable(d,Say.t("Fingerprint (SHA-256)"),hash,"verifyHash",null,WIDE));
                JDialog[] box={null};
                List<JComponent> buttons=new ArrayList<>();
                if(found.report()!=null&&!found.report().isEmpty())buttons.add(DesktopUi.button("Open the PDF report",()->{try{Desktop.browse(found.report());}catch(Exception noBrowser){copy(found.report());notice(d,Say.t("The report's link is copied"));}}));
                buttons.add(DesktopUi.primary("Done",()->box[0].dispose()));
                box[0]=DesktopUi.sheet(d.frame,"Verify on Minima…",body,DesktopUi.footer(buttons.toArray(new JComponent[0])),true);
                DesktopUi.show(box[0],600,620);
            },e->notice(d,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage())));
        },d::failed);
    }

    // ---- everything stamped -------------------------------------------------------------------------------------------

    /**
     * Everything stamped from this PC, each version its own line (the owner, 2026-10-18: "an Integritas dedicated window where
     * we list everything stamped, we keep the on-chain reference and all needed information"; decisions 203 and 218).
     *
     * <p>A plain list, the details one click further (the owner, 2026-10-21: "When we click in the menu, stamped on Minima, we
     * should just have the list of everything we stamped, not directly the details, this should come afterward when clicking
     * one element"; decision 230): one line for each, newest first, its name, what it is, when and how far; a click opens its
     * box, where its block, transactions and fingerprint are. At the top, what does the stamping, with its website.
     *
     * <p>Laid out as Home is (the owner, 2026-10-21: "Remove the done and put the plus there similar to the + on the home page.
     * Make know more a clickable link and use the integritas.technology label ... Make it clear that this is a section of
     * stamped elements, label it and list the elements underneath"; decision 231): no Done, the window closing with its own ✕
     * and Escape as every sheet does; Home's round + where Done was; integritas.technology as a link; and the stamps under a
     * heading of their own. Your Integritas account… opens My profile at Stamping in front, closing nothing: over the main
     * window, so closing this list later leaves it open.
     *
     * <p>Always as the notebook has it now (decision 224): it is read again whenever the watch hears something, so a stamp that
     * lands while it is open says so here too; and a stamp of a fingerprint another stamp found is found as well (see
     * NoteStore.stampsAgree), where it said "not found yet" beside the box saying block 2374505.
     */
    static void list(Desktop d,java.awt.Window over) {
        JPanel body=DesktopUi.column();JDialog[] box={null};
        JButton key=DesktopUi.button("Your Integritas account…",()->settings(d,null));key.setName("stampAccount");
        // Home's +, drawn as it is (DesktopHome.round), its menu opening upwards from it as Home's does.
        JButton plus=DesktopHome.round();plus.setName("stampPlus");plus.setPreferredSize(new Dimension(52,52));
        plus.setToolTipText(Say.t("Stamp something…"));plus.getAccessibleContext().setAccessibleName(Say.t("Stamp something…"));
        plus.addActionListener(e->{JPopupMenu menu=plusMenu(d,box[0]);Dimension m=menu.getPreferredSize();menu.show(plus,plus.getWidth()-m.width-6,-m.height-2);});
        JPanel foot=new JPanel(new BorderLayout(DesktopUi.M,0));foot.setOpaque(false);
        JPanel left=new JPanel(new GridBagLayout());left.setOpaque(false);left.add(key);foot.add(left,BorderLayout.WEST);foot.add(plus,BorderLayout.EAST);
        box[0]=DesktopUi.sheet(over!=null?over:d.frame,"Stamped on Minima",DesktopUi.scrolling(body),foot,false);
        box[0].setName("stampList");
        Runnable fill=()->d.disk.submit(()->d.store.stamps(),rows->{if(box[0].isDisplayable())fill(d,box[0],body,rows);},d::failed);
        Integritas.Watch.Listener ear=(heard,landed,trouble)->SwingUtilities.invokeLater(fill);
        watch(d).listen(ear);
        box[0].addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){watch(d).unlisten(ear);}});
        d.disk.submit(()->d.store.stamps(),rows->{fill(d,box[0],body,rows);DesktopUi.show(box[0],640,700);},d::failed);
    }
    /** The list's lines, drawn again from the rows read now. */
    private static void fill(Desktop d,JDialog box,JPanel body,List<NoteStore.StampRow> rows) {
        body.removeAll();
        // What does the stamping, quietly, and its website under it, a link (decision 231).
        DesktopUi.add(body,DesktopUi.note(Say.t("Stamping uses Integritas, with your own Integritas account: it records each fingerprint on the Minima blockchain with the date, and only the fingerprint leaves this PC."),WIDE,DesktopUi.QUIET,words()));
        JButton site=DesktopUi.link("integritas.technology",()->explore(d,Integritas.SITE));site.setFont(words());site.setName("stampSite");site.setToolTipText(Integritas.SITE);
        DesktopUi.add(body,site);
        DesktopUi.gap(body,DesktopUi.L);
        // The stamps, under a heading saying they are (decision 231).
        DesktopUi.Text heading=DesktopUi.heading("Stamped notes and files");heading.setName("stampListHeading");DesktopUi.add(body,heading);
        DesktopUi.gap(body,DesktopUi.S);
        if(rows.isEmpty())DesktopUi.add(body,DesktopUi.spread(Say.t("Nothing is stamped yet.")));
        for(NoteStore.StampRow one:rows) {
            // One line each: what, when, how far. The rest is in its box.
            String name=one.name().isEmpty()?Say.t("Untitled"):DesktopMenus.shortly(one.name());
            JButton line=DesktopUi.quietButton("“"+name+"”   ·   "+kind(one.kind())+"   ·   "+Integritas.when(one.at())+"   ·   "+state(one)+"  ›",()->show(d,one.n()));
            line.setFont(words());line.setHorizontalAlignment(SwingConstants.LEFT);line.setForeground(one.confirmed()?DesktopUi.INK:DesktopUi.QUIET);
            line.setName("stampCard"+one.n());line.putClientProperty("state",state(one));line.setToolTipText(Say.t("Open the stamp"));
            DesktopUi.add(body,line);
        }
        body.revalidate();body.repaint();
    }

    /**
     * What + offers (the owner, 2026-10-21: it "should give the option, from the app or external from the drive"; decision
     * 230): a note or a file of the notebook, chosen in a list, or a file of this PC, chosen in Windows' own box.
     */
    static JPopupMenu plusMenu(Desktop d,java.awt.Window over) {
        JPopupMenu menu=new JPopupMenu();
        JMenuItem inside=DesktopMenus.item("A note or file in Mininotes…",DesktopIcons.glyph("notebook-pen",16));inside.setName("stampInside");inside.addActionListener(e->choose(d,over));menu.add(inside);
        JMenuItem drive=DesktopMenus.item("A file on this PC…",DesktopIcons.glyph("hard-drive",16));drive.setName("stampOutside");drive.addActionListener(e->outside(d,over));menu.add(drive);
        return menu;
    }

    /**
     * A note or a file of the notebook chosen to be stamped: the notes written in most recently, and as words are typed, the
     * notes and files Home's search finds for them (NoteStore.lookingEverywhere), newest first. Chosen, its own stamping box.
     */
    static void choose(Desktop d,java.awt.Window over) {
        JPanel body=new JPanel(new BorderLayout(0,DesktopUi.S));body.setOpaque(false);
        JTextField find=new JTextField(28);find.setName("stampFind");find.getAccessibleContext().setAccessibleName(Say.t("Find a note or a file"));
        find.putClientProperty("JTextField.placeholderText",Say.t("Find a note or a file"));
        JPanel top=DesktopUi.column();DesktopUi.add(top,find);DesktopUi.gap(top,4);
        DesktopUi.add(top,DesktopUi.note(Say.t("The notes written in last. Type to find any note, or a file by its name."),WIDE,DesktopUi.QUIET,words()));
        body.add(top,BorderLayout.NORTH);
        DefaultListModel<NoteStore.Branch> found=new DefaultListModel<>();JList<NoteStore.Branch> list=new JList<>(found);list.setName("stampChoices");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setFont(words());
        DefaultListCellRenderer plain=new DefaultListCellRenderer();
        list.setCellRenderer((l,one,index,selected,focused)->{JLabel said=(JLabel)plain.getListCellRendererComponent(l,(one.name==null||one.name.isBlank()?Say.t("Untitled"):DesktopMenus.shortly(one.name))
            +"   ·   "+Say.t(one.kind==NoteStore.Branch.Kind.FILE?"File":"Note"),index,selected,focused);
            said.setIcon(DesktopIcons.glyph(one.kind==NoteStore.Branch.Kind.FILE?"file":"notebook-text",16));said.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));return said;});
        JScrollPane scroll=new JScrollPane(list);scroll.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));scroll.setPreferredSize(new Dimension(WIDE,320));body.add(scroll);
        JDialog[] box={null};
        Runnable go=()->{NoteStore.Branch one=list.getSelectedValue();if(one==null)return;box[0].dispose();
            if(one.kind==NoteStore.Branch.Kind.FILE)d.disk.submit(()->d.store.keptFile(one.id),held->{if(held!=null)files(d,List.of(held));},d::failed);
            else d.save(()->note(d,one.id));};
        JButton stamp=DesktopUi.primary("Stamp…",go);stamp.setName("stampChosen");stamp.setEnabled(false);
        list.addListSelectionListener(e->stamp.setEnabled(list.getSelectedValue()!=null));
        list.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&list.getSelectedValue()!=null)go.run();}});
        // Read again a moment after the typing stops; an answer to words since changed is let go.
        int[] asked={0};
        Runnable read=()->{int mine=++asked[0];String words=find.getText().trim();
            d.disk.submit(()->{List<NoteStore.Branch> out=new ArrayList<>();
                for(NoteStore.Branch one:words.isEmpty()?d.store.lately(60):d.store.lookingEverywhere(words))if(one.kind==NoteStore.Branch.Kind.PAGE||one.kind==NoteStore.Branch.Kind.FILE)out.add(one);
                return out;},rows->{if(mine!=asked[0]||!box[0].isDisplayable())return;found.clear();for(NoteStore.Branch one:rows)found.addElement(one);if(!rows.isEmpty())list.setSelectedIndex(0);},d::failed);};
        javax.swing.Timer settle=new javax.swing.Timer(250,e->read.run());settle.setRepeats(false);
        find.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){settle.restart();}public void removeUpdate(javax.swing.event.DocumentEvent e){settle.restart();}public void changedUpdate(javax.swing.event.DocumentEvent e){}});
        find.addActionListener(e->go.run());
        box[0]=DesktopUi.sheet(over!=null?over:d.frame,"Stamp a note or a file",body,DesktopUi.footer(DesktopUi.button("Cancel",()->box[0].dispose()),stamp),false);box[0].setName("stampChoose");
        box[0].getRootPane().putClientProperty("focus",find);
        read.run();
        DesktopUi.show(box[0],600,560);
    }

    /** A block's date, from Integritas' ISO time, as this PC says dates; as it came where it cannot be read. */
    static String when(String iso){return Integritas.when(iso);}
    /**
     * The one line of a note's or a file's menu about Minima (the owner, 2026-10-25: "only one element in the contextual menu
     * of the note, so combining the three menu elements we currently have"; decision 253): Stamp on Minima…, and once it is
     * stamped, how far its newest stamp is ("Stamped on Minima, <date>, block <n>…", as decision 224 had it say), read in the
     * background. Either way it opens the one window where stamping, verifying, comparing and the report are.
     */
    static JMenuItem menuLine(Desktop d,JPopupMenu menu,String id,Runnable open) {
        JMenuItem line=DesktopMenus.item("Stamp on Minima…");line.setName("stampOnMinima");line.addActionListener(e->open.run());menu.add(line);
        d.disk.submit(()->d.store.stampsOf(id),stamps->{if(stamps.isEmpty())return;String words=Integritas.noteLine(stamps.get(0),false);
            line.setText(DesktopMenus.shortly(words+"…"));line.putClientProperty("words",words);if(menu.isVisible())menu.pack();},e->{});
        return line;
    }

    // ---- the note's line ----------------------------------------------------------------------------------------------

    /**
     * The line at the top of a stamped note, under its title (decision 218): "Stamped on Minima, <date>, block <n>", or waiting,
     * and once the note is not what was stamped, "Changed since it was stamped on <date>", toned down, with Stamp this version….
     * A click on it opens the stamp's box. Not there on a note never stamped. Compared a moment after the typing stops, in the
     * title as in the text: a stamp covers both from 0.4.018, so a renamed note is a changed one (decision 230); an older stamp,
     * of its text alone, is compared with its text alone.
     */
    static final class Line extends JPanel {
        private final Desktop d;
        final JLabel said=new JLabel(" ");final JButton again;
        private String id;private List<NoteStore.StampRow> stamps=List.of();private NoteStore.StampRow shown;private boolean changed;
        private final javax.swing.Timer settle;

        Line(Desktop d) {
            super(new FlowLayout(FlowLayout.LEFT,0,0));this.d=d;setOpaque(false);setName("stampLine");setVisible(false);
            setBorder(BorderFactory.createEmptyBorder(0,2,2,0));
            said.setFont(DesktopUi.BODY.deriveFont(12.5f));said.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));said.setName("stampLineWords");
            said.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(shown!=null)DesktopStamp.show(d,shown.n());}});
            again=DesktopUi.quietButton("Stamp this version…",()->{if(id!=null)d.save(()->note(d,id));});again.setFont(DesktopUi.BODY.deriveFont(12.5f));again.setName("stampLineAgain");
            JLabel seal=new JLabel("⛓ ");seal.setFont(DesktopUi.BODY.deriveFont(12f));seal.setForeground(DesktopUi.QUIET);
            add(seal);add(said);add(again);
            settle=new javax.swing.Timer(500,e->compare(d.title.getText(),d.noteNow()));settle.setRepeats(false);
        }
        /** A note shown: its stamps read, and its title and text compared with them. */
        void follow(String noteId,String title,String body) {
            id=noteId;settle.stop();
            if(noteId==null){stamps=List.of();setVisible(false);return;}
            d.disk.submit(()->d.store.stampsOf(noteId),all->{if(!noteId.equals(id))return;stamps=all;compare(title,body);},e->{});
        }
        /** Read again: after a stamp, a look that landed, a verification. */
        void again(){if(id!=null)follow(id,d.title.getText(),d.noteNow());}
        /** Typed in, the title or the text: compared once the typing settles. */
        void edited(){if(!stamps.isEmpty())settle.restart();}
        /** The stamp that matches the note now, each in its own form, stands; else the newest, and the note is changed. */
        void compare(String title,String body) {
            if(stamps.isEmpty()){shown=null;setVisible(false);return;}
            NoteStore.StampRow match=Integritas.matching(stamps,title,body);
            shown=match!=null?match:stamps.get(0);changed=match==null;
            said.setText(Integritas.noteLine(shown,changed));
            said.setForeground(changed?DesktopUi.QUIET:shown.confirmed()?new Color(46,125,80):shown.waiting()?Desktop.ACCENT:DesktopUi.QUIET);
            said.setToolTipText(Say.t("Open the stamp"));
            again.setVisible(changed);setVisible(true);revalidate();repaint();
        }
        boolean changed(){return changed;}
        String words(){return said.getText();}
        /** The stamp the note stands by, its row, or 0 where it has none: what This note's line opens. */
        long standing(){return isVisible()&&shown!=null?shown.n():0;}
    }
}
