// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
            if(landed)notice(d,Say.t("{0} is on Minima: block {1}",name,String.valueOf(row.block())));
            else if(NoteStore.STAMP_UNSEEN.equals(row.state()))notice(d,Say.t("{0} was not found on Minima in two hours. Stamped on Minima… can look again.",name));
            d.stampLine.again();if(d.fileCards!=null)d.fileCards.again();
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

    /** The account linked, or linked again, or forgotten: the key from the person's profile at integritas.technology. */
    static void account(Desktop d,Runnable then) {
        d.disk.submit(()->d.store.secret(KEY),had->{
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.note(Say.t("Stamping records a file's fingerprint on the Minima blockchain with the date, through your own Integritas account, so you can prove later that it existed then, unchanged. The file itself never leaves this PC."),420,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f)));
            DesktopUi.gap(body,DesktopUi.M);
            DesktopUi.add(body,DesktopUi.note(Say.t("Paste the API key from your profile at integritas.technology. It is kept in your notebook on this PC, locked with it, and sent only to Integritas."),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
            DesktopUi.gap(body,DesktopUi.S);
            JPasswordField field=new JPasswordField(had,28);field.getAccessibleContext().setAccessibleName(Say.t("Integritas API key"));
            DesktopUi.add(body,field);DesktopUi.gap(body,DesktopUi.S);
            JButton site=DesktopUi.quietButton("integritas.technology  ›",()->{try{Desktop.browse(Integritas.SITE);}catch(Exception noBrowser){d.status.setText(Integritas.SITE);}});
            DesktopUi.add(body,DesktopUi.actions(site));
            JDialog[] box={null};
            JButton forget=DesktopUi.button("Unlink",()->{box[0].dispose();d.disk.submit(()->{d.store.setSecret(KEY,"");d.store.setSecret(KEY+".at",String.valueOf(System.currentTimeMillis()));return null;},v->linked(d,false),d::failed);});
            forget.setVisible(!had.isEmpty());
            JButton save=DesktopUi.primary("Link",()->{
                String key=new String(field.getPassword()).trim();
                if(!Integritas.looksLikeAKey(key)){field.requestFocusInWindow();return;}
                box[0].dispose();
                d.disk.submit(()->{d.store.setSecret(KEY,key);d.store.setSecret(KEY+".at",String.valueOf(System.currentTimeMillis()));return null;},v->{linked(d,true);
                    // And to the devices chosen for it in My profile (decision 223).
                    d.network.submit(()->Post.sendKeyToChosen(d.context,d.store,d.keys,KEY),said->{if(!said.isEmpty())d.status.setText(said);},e->{});if(then!=null)then.run();},d::failed);
            });
            box[0]=DesktopUi.sheet(d.frame,"Your Integritas account",body,DesktopUi.footer(forget,save),true);
            box[0].getRootPane().setDefaultButton(save);box[0].getRootPane().putClientProperty("focus",field);
            DesktopUi.show(box[0],480,520);
        },d::failed);
    }
    /** Integritas linked or unlinked, said in the middle of the window (decision 218); My profile's Save says it the same way. */
    static void linked(Desktop d,boolean on){notice(d,Say.t(on?"Integritas is linked":"Integritas is unlinked"));}

    // ---- stamping -----------------------------------------------------------------------------------------------------

    /** What is about to be stamped: a note's saved text, kept as it is, or a file's bytes. */
    record Subject(String id,String kind,String name,String hash,long size,String title,String words) {
        boolean note(){return "note".equals(kind);}
    }

    /** These files stamped: one in its box; several asked once, each then waiting for Minima, all in Stamped on Minima. */
    static void files(Desktop d,List<NoteStore.Held> files) {
        if(files.isEmpty())return;
        if(files.size()==1){NoteStore.Held one=files.get(0);open(d,()->{byte[] bytes=bytesOf(d,one);return new Subject(one.id,"file",one.name,Integritas.hash(bytes),bytes.length,null,null);});return;}
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){account(d,()->files(d,files));return;}
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

    /** A note's text, as it is saved now, stamped in its box: exactly that text is fingerprinted and kept. */
    static void note(Desktop d,String id) {
        open(d,()->{NoteStore.Note note=d.store.get(id);if(note==null)return null;
            return new Subject(id,"note",note.heading(),Integritas.hashOfText(note.body),note.body.length(),note.title,note.body);});
    }

    private interface Making{Subject make() throws Exception;}

    /** The box for something about to be stamped, once its fingerprint is made; linked first where there is no key yet. */
    private static void open(Desktop d,Making making) {
        d.disk.submit(()->{String key=d.store.secret(KEY);Subject s=key.isEmpty()?null:making.make();return new Object[]{key,s};},got->{
            if(((String)got[0]).isEmpty()){account(d,()->open(d,making));return;}
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
        return n;
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
        boolean isNote(){return row!=null?row.note():subject.note();}
        String name(){return row!=null?row.name():subject.name();}

        /** Everything drawn again from what is known now. */
        void draw() {
            body.removeAll();foot.removeAll();
            DesktopUi.Text head=DesktopUi.heading("“"+(name().isEmpty()?Say.t("Untitled"):name())+"”   ·   "+Say.t(isNote()?"Note":"File"));head.setName("stampName");
            DesktopUi.add(body,head);DesktopUi.gap(body,DesktopUi.M);
            // 1. The fingerprint, of exactly what.
            step(Mark.DONE,"1. The fingerprint, made on this PC");
            if(subject!=null)indent(said(Integritas.what(subject.note(),subject.size()),DesktopUi.QUIET));
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
                if(!row.said().isEmpty())indent(said(Say.t("Integritas said: {0}",row.said()),DesktopUi.QUIET));
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
            DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.actions(more.toArray(new JComponent[0])));
            JButton copy=DesktopUi.button("Copy the details",()->{copy(Integritas.details(row));notice(d,Say.t("Copied"));});copy.setName("stampCopy");foot.add(copy);
            JButton done=DesktopUi.primary("Done",dialog::dispose);foot.add(done);dialog.getRootPane().setDefaultButton(done);
        }

        /** The one step that costs a stamp, pressed: sent, kept, and watched; the box shows each part as it comes. */
        void stamp() {
            sending=true;problem=null;draw();
            d.disk.submit(()->d.store.secret(KEY),key->d.network.submit(()->send(d,key,subject),made->{
                sending=false;n=made;
                d.disk.submit(()->d.store.stampRow(made),r->{row=r;draw();d.stampLine.again();if(d.fileCards!=null)d.fileCards.again();
                    notice(d,r!=null&&r.confirmed()?Say.t("{0} is on Minima: block {1}",named(r),String.valueOf(r.block())):Say.t("Sent to Integritas, waiting for Minima"));},d::failed);
            },e->{sending=false;problem=Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage());draw();}),d::failed);
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
            if(key.isEmpty()){account(d,()->report(d,row,then));return;}
            notice(d,Say.t("Asking Integritas for the report…"));
            // The NFT it mints for the report kept with the stamp, to be seen on the Minima Explorer (decision 224).
            d.network.submit(()->{Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);d.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(d).told(row.hash());return f;},found->{
                if(then!=null)then.run();
                if(found.report()==null||found.report().isEmpty()){notice(d,Say.t("Integritas gave no report this time. Try again in a moment."));return;}
                try{Desktop.browse(found.report());}catch(Exception noBrowser){copy(found.report());notice(d,Say.t("The report's link is copied"));}
            },e->notice(d,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage())));
        },d::failed);
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
            boolean same;try{same=Integritas.hashOfText(words[1]).equals(row.hash());}catch(Exception never){same=false;}
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
    static void verifyFile(Desktop d,NoteStore.Held file) {
        verify(d,file.name,()->{byte[] bytes=bytesOf(d,file);return new Object[]{Integritas.hash(bytes),(long)bytes.length};});
    }
    /** A note's words, as they are now, looked for on Minima. */
    static void verifyNote(Desktop d,String id) {
        d.disk.submit(()->d.store.get(id),note->{if(note!=null)verify(d,note.heading(),()->new Object[]{Integritas.hashOfText(note.body),-1L});},d::failed);
    }

    private interface Hashing{Object[] hash() throws Exception;}

    private static void verify(Desktop d,String name,Hashing hashing) {
        d.disk.submit(()->d.store.secret(KEY),key->{
            if(key.isEmpty()){account(d,()->verify(d,name,hashing));return;}
            notice(d,Say.t("Looking for it on Minima…"));
            d.network.submit(()->{Object[] h=hashing.hash();Integritas.Found f=Integritas.verify(key,(String)h[0],name,(Long)h[1],true);
                // Where it was found, kept with the stamps of the same fingerprint (decisions 203 and 218).
                if(f.match()&&!f.blocks().isEmpty()){Integritas.Block b=Integritas.chosen(f.blocks(),0);if(b!=null)d.store.stampFound((String)h[0],b.number(),b.txpow(),b.date());}
                d.store.stampNft((String)h[0],f.nft(),f.nftTx());watch(d).told((String)h[0]);
                return new Object[]{f,h[0]};},got->{
                Integritas.Found found=(Integritas.Found)got[0];String hash=(String)got[1];
                d.stampLine.again();
                JPanel body=DesktopUi.column();
                if(found.match()) {
                    DesktopUi.add(body,DesktopUi.body(Say.t("{0} is on Minima, unchanged.","“"+name+"”")));DesktopUi.gap(body,DesktopUi.S);
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
     * we list everything stamped, we keep the on-chain reference and all needed information"; decisions 203 and 218): what and
     * when, how far it is, the block, the fingerprint; Details… shows its box, and a note's stamped text is one click further.
     *
     * <p>Always as the notebook has it now (decision 224): it is read again whenever the watch hears something, so a stamp that
     * lands while it is open says so here too; and a stamp of a fingerprint another stamp found is found as well (see
     * NoteStore.stampsAgree), where it said "not found yet" beside the box saying block 2374505.
     */
    static void list(Desktop d,java.awt.Window over) {
        JPanel body=DesktopUi.column();JDialog[] box={null};
        JButton key=DesktopUi.button("Your Integritas account…",()->account(d,null));
        box[0]=DesktopUi.sheet(over!=null?over:d.frame,"Stamped on Minima",DesktopUi.scrolling(body),DesktopUi.footer(key,DesktopUi.primary("Done",()->box[0].dispose())),false);
        box[0].setName("stampList");
        Runnable fill=()->d.disk.submit(()->d.store.stamps(),rows->{if(box[0].isDisplayable())fill(d,body,rows);},d::failed);
        Integritas.Watch.Listener ear=(heard,landed,trouble)->SwingUtilities.invokeLater(fill);
        watch(d).listen(ear);
        box[0].addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){watch(d).unlisten(ear);}});
        d.disk.submit(()->d.store.stamps(),rows->{fill(d,body,rows);DesktopUi.show(box[0],640,700);},d::failed);
    }
    /** The list's cards, drawn again from the rows read now. */
    private static void fill(Desktop d,JPanel body,List<NoteStore.StampRow> rows) {
        body.removeAll();
        DesktopUi.add(body,said(Say.t("Stamp on Minima records that a file or a note exists as it is now. Verify on Minima checks later whether it is still the same, and when it was recorded."),DesktopUi.QUIET));
        DesktopUi.gap(body,DesktopUi.M);
        if(rows.isEmpty())DesktopUi.add(body,DesktopUi.spread(Say.t("Nothing stamped from this PC yet.")));
        for(NoteStore.StampRow one:rows) {
            JPanel card=DesktopUi.column();card.setName("stampCard"+one.n());
            DesktopUi.Text name=DesktopUi.body((one.name().isEmpty()?Say.t("Untitled"):one.name())+"   ·   "+Say.t(one.note()?"Note":"File"));name.setFont(words().deriveFont(Font.BOLD));
            DesktopUi.add(card,name);DesktopUi.gap(card,4);
            DesktopUi.Text how=DesktopUi.note(Say.t("Stamped {0}",Integritas.when(one.at()))+"   ·   "+state(one),WIDE-20,one.confirmed()?DesktopUi.INK:DesktopUi.QUIET,words());how.setName("stampState");
            DesktopUi.add(card,how);
            if(one.confirmed()&&!one.recorded().isEmpty())DesktopUi.add(card,DesktopUi.note(Say.t("Recorded {0}",Integritas.when(one.recorded())),WIDE-20,DesktopUi.QUIET,words()));
            DesktopUi.gap(card,4);
            if(one.confirmed())DesktopUi.add(card,copyable(d,Say.t("Block"),String.valueOf(one.block()),"listBlock",Integritas.blockLink(one.block()),WIDE-20));
            if(!one.txpow().isEmpty())DesktopUi.add(card,copyable(d,Say.t("Transaction (txpow)"),one.txpow(),"listTxpow",Integritas.txpowLink(one.txpow()),WIDE-20));
            DesktopUi.add(card,copyable(d,Say.t("Fingerprint (SHA-256)"),one.hash(),"listHash",null,WIDE-20));
            DesktopUi.gap(card,6);
            List<JComponent> actions=new ArrayList<>();
            // Details…: the stamp's own box, with everything known of it (it said Open…, which was not clear; decision 224).
            JButton details=DesktopUi.button("Details…",()->new Box(d,null,one.n()).show());details.setName("stampDetails");actions.add(details);
            if(one.note()&&one.kept())actions.add(DesktopUi.button("Open this version",()->version(d,one.n())));
            JButton verify=verifyButton(d,one);verify.setName("stampVerify");actions.add(verify);
            actions.add(DesktopUi.button("Copy the details",()->{copy(Integritas.details(one));notice(d,Say.t("Copied"));}));
            DesktopUi.add(card,DesktopUi.actions(actions.toArray(new JComponent[0])));
            DesktopUi.add(body,DesktopUi.card(null,card));DesktopUi.gap(body,DesktopUi.S);
        }
        body.revalidate();body.repaint();
    }

    /** A block's date, from Integritas' ISO time, as this PC says dates; as it came where it cannot be read. */
    static String when(String iso){return Integritas.when(iso);}
    /**
     * A stamped file's menu starts with its stamp (the owner, 2026-10-19: "the file should be marked as stamped when done, and
     * all the details should be accessible again from the right click"; decision 224): "Stamped on Minima, <date>, block <n>",
     * waiting, or not found yet, as a note's line says it, opening the stamp's box. Read in the background; nothing on a file
     * never stamped.
     */
    static void fileLine(Desktop d,JPopupMenu menu,String id) {
        JMenuItem line=DesktopMenus.item();line.setIcon(DesktopIcons.glyph("receipt-text",16));line.setName("fileStamp");line.setVisible(false);
        JPopupMenu.Separator under=new JPopupMenu.Separator();under.setVisible(false);
        menu.insert(line,0);menu.insert(under,1);
        d.disk.submit(()->d.store.stampsOf(id),stamps->{if(stamps.isEmpty())return;NoteStore.StampRow last=stamps.get(0);
            line.setText(DesktopMenus.shortly(Integritas.noteLine(last,false)+"…"));line.putClientProperty("words",Integritas.noteLine(last,false));
            line.setToolTipText(Say.t("Open the stamp"));line.addActionListener(e->show(d,last.n()));
            line.setVisible(true);under.setVisible(true);if(menu.isVisible())menu.pack();},e->{});
    }

    // ---- the note's line ----------------------------------------------------------------------------------------------

    /**
     * The line at the top of a stamped note, under its title (decision 218): "Stamped on Minima, <date>, block <n>", or waiting,
     * and once its text is not the text stamped, "Changed since it was stamped on <date>", toned down, with Stamp this version….
     * A click on it opens the stamp's box. Not there on a note never stamped. Its text compared a moment after the typing stops.
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
            settle=new javax.swing.Timer(500,e->compare(d.noteNow()));settle.setRepeats(false);
        }
        /** A note shown: its stamps read, and its text compared with them. */
        void follow(String noteId,String body) {
            id=noteId;settle.stop();
            if(noteId==null){stamps=List.of();setVisible(false);return;}
            d.disk.submit(()->d.store.stampsOf(noteId),all->{if(!noteId.equals(id))return;stamps=all;compare(body);},e->{});
        }
        /** Read again: after a stamp, a look that landed, a verification. */
        void again(){if(id!=null)follow(id,d.noteNow());}
        /** Typed in: compared once the typing settles. */
        void edited(){if(!stamps.isEmpty())settle.restart();}
        void compare(String body) {
            if(stamps.isEmpty()){shown=null;setVisible(false);return;}
            String now;try{now=Integritas.hashOfText(body);}catch(Exception never){return;}
            shown=Integritas.standing(stamps,now);changed=!shown.hash().equals(now);
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
