// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Stamp on Minima and Verify on Minima on the phone (decision 199; see Integritas), as the PC has them (DesktopStamp): linked
 * once with the person's own Integritas key, and what goes is the fingerprint alone.
 *
 * <p>The owner, 2026-10-19 (decision 218): "we need a full communication box, showing everything, the transaction of encryption
 * on the blockchain, the hashes, the date, the block and so on". Stamping is a box that shows each step as it happens, the
 * fingerprint of exactly what, sent to Integritas, waiting for Minima, then the block, the date and the transaction; closing it
 * stops nothing, the watch goes on looking and says so in the middle of the screen when it lands.
 */
final class PhoneStamp {
    private PhoneStamp(){}

    static final String KEY=Integritas.KEY;

    // ---- the watch ----------------------------------------------------------------------------------------------------

    private static Integritas.Watch watch;
    /** The screen the watch speaks to: the one showing now. */
    private static java.lang.ref.WeakReference<MainActivity> shown=new java.lang.ref.WeakReference<>(null);

    static synchronized Integritas.Watch watch(MainActivity a) {
        shown=new java.lang.ref.WeakReference<>(a);
        if(watch!=null&&watch.store()==a.store)return watch;
        if(watch!=null)watch.stop();
        watch=new Integritas.Watch(a.store,1000);
        watch.listen((row,landed,trouble)->{MainActivity now=shown.get();if(now==null)return;now.runOnUiThread(()->{
            if(now.isFinishing())return;
            if(landed)Post.stampsChanged(now,now.store,Keys.of(now));
            if(landed)notice(now,Say.t("{0} is on Minima: block {1}",named(row),String.valueOf(row.block())));
            else if(NoteStore.STAMP_UNSEEN.equals(row.state()))notice(now,Say.t("{0} was not found on Minima in two hours. Stamped on Minima… can look again.",named(row)));
            now.stampLineAgain();now.filesAgain();
            // A file found on Minima wears it on its icon (decision 249).
            if(landed&&"file".equals(row.kind()))now.refresh();
        });});
        return watch;
    }
    /** At a start: those still waiting for Minima looked for again (decision 218). */
    static void resume(MainActivity a){watch(a).resume();}

    private static String named(NoteStore.StampRow row){return "“"+(row.name().isEmpty()?Say.t("Untitled"):row.name())+"”";}

    /**
     * A notice in the middle of the screen, over what is there, for a few seconds (the owner, 2026-10-19: "The Integritas
     * message is too low, it should be centred on the screen"; decision 218); a tap puts it away, a newer one takes its place.
     */
    static void notice(MainActivity a,String words) {
        ViewGroup over=(ViewGroup)a.getWindow().getDecorView();
        View was=over.findViewWithTag("centredNotice");if(was!=null)over.removeView(was);
        TextView said=a.label(words,MainActivity.READING,a.INK);said.setGravity(Gravity.CENTER);
        said.setTypeface(said.getTypeface(),android.graphics.Typeface.BOLD);
        GradientDrawable card=new GradientDrawable();card.setColor(a.CARD);card.setCornerRadius(a.dp(18));card.setStroke(Math.max(1,a.dp(1)),a.LINE);
        said.setBackground(card);said.setPadding(a.dp(22),a.dp(18),a.dp(22),a.dp(18));said.setElevation(a.dp(8));said.setTag("centredNotice");
        said.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        FrameLayout.LayoutParams at=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER);at.setMargins(a.dp(28),0,a.dp(28),0);
        over.addView(said,at);
        Runnable gone=()->{if(said.getParent()!=null)over.removeView(said);};
        said.setOnClickListener(v->gone.run());said.postDelayed(gone,4500);
    }

    // ---- the account --------------------------------------------------------------------------------------------------

    /**
     * The account, wanted before something can be stamped or looked for: My profile, opened at Stamping with the key's field in
     * hand, the one place its settings are (the owner, 2026-10-21: "Your Integritas account needs a window with the full
     * settings ... we need to be brought on the dedicated section on the profile page"; decision 231). What was asked goes on
     * once a key is saved there.
     */
    static void account(MainActivity a,Runnable then){a.profile(MainActivity.STAMPING,then);}

    // ---- stamping -----------------------------------------------------------------------------------------------------

    /** What is about to be stamped: a note's saved title and text, kept as they are, or a file's bytes. */
    record Subject(String id,String kind,String name,String hash,long size,String title,String words) {
        boolean note(){return Integritas.NOTE.equals(kind)||Integritas.TITLED.equals(kind);}
        boolean outside(){return Integritas.OUTSIDE.equals(kind);}
    }
    private interface Making{Subject make() throws Exception;}

    /** A file stamped, in its box. */
    static void file(MainActivity a,NoteStore.Held file) {
        open(a,()->{byte[] bytes=bytesOf(a,file);return new Subject(file.id,Integritas.FILE,file.name,Integritas.hash(bytes),bytes.length,null,null);});
    }
    /**
     * A note, as it is saved now, stamped in its box: its title and text, fingerprinted, and
     * both kept (the owner, 2026-10-21: "Why integritas/stamping doesnt include the title of the note? I think it should";
     * decision 230).
     */
    static void note(MainActivity a,String id,String name) {
        open(a,()->{NoteStore.Note n=a.store.get(id);if(n==null)return null;
            return new Subject(id,Integritas.TITLED,n.heading(),Integritas.hashOfNote(n.title,n.body),Integritas.noteWords(n.title,n.body).length(),n.title,n.body);});
    }
    /**
     * A file from the phone's own storage stamped, never put in Mininotes (the owner, 2026-10-21: the + "should give the
     * option, from the app or external from the drive"; decision 230): the system's picker, the bytes read off the interface
     * thread and fingerprinted, the stamp kept under an id of its own and the file's name. Its Verify on Minima asks for it again.
     */
    static void outside(MainActivity a) {
        pick(a,(now,uri)->open(now,()->{Object[] h=hashOf(now,uri);
            return new Subject("outside:"+java.util.UUID.randomUUID(),Integritas.OUTSIDE,nameOf(now,uri),(String)h[0],(Long)h[1],null,null);}));
    }

    // ---- a file picked from the phone ---------------------------------------------------------------------------------

    /** The request the phone's picker answers, in MainActivity.onActivityResult (the private screen has 40 and 41). */
    static final int PICK=42;
    interface Picked{void got(MainActivity a,Uri uri);}
    /** What to do with the file picked, while the picker is open; the screen it comes back to is the one given then. */
    private static Picked picking;
    static void pick(MainActivity a,Picked then) {
        picking=then;
        try{a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),PICK);}
        catch(RuntimeException none){picking=null;a.toast("No app on this phone can pick a file.");}
    }
    /** The picker's answer: nothing where nothing was picked. */
    static void picked(MainActivity a,Uri uri){Picked then=picking;picking=null;if(then!=null&&uri!=null)then.got(a,uri);}
    /** A picked file's SHA-256 and its length in bytes, read through once, never kept. */
    private static Object[] hashOf(MainActivity a,Uri uri) throws Exception {
        final long[] count={0};
        try(java.io.InputStream in=a.getContentResolver().openInputStream(uri)) {
            if(in==null)throw new java.io.FileNotFoundException("That file could not be read.");
            java.io.InputStream counted=new java.io.FilterInputStream(in){
                @Override public int read() throws java.io.IOException{int b=super.read();if(b>=0)count[0]++;return b;}
                @Override public int read(byte[] into,int off,int len) throws java.io.IOException{int n=super.read(into,off,len);if(n>0)count[0]+=n;return n;}};
            return new Object[]{Integritas.hash(counted),count[0]};
        }
    }
    private static String nameOf(MainActivity a,Uri uri) {
        try(android.database.Cursor c=a.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if(c!=null&&c.moveToFirst()&&c.getString(0)!=null)return c.getString(0);
        } catch(RuntimeException none){/* a name of its own, then */}
        return Say.t("File");
    }

    /** What a stamp is, in a list and at the top of its box: a note with its title, an older one of its text alone, a file. */
    static String kindWords(String kind) {
        if(Integritas.TITLED.equals(kind))return Say.t("Note");
        if(Integritas.NOTE.equals(kind))return Say.t("Note, text only");
        return Say.t(Integritas.OUTSIDE.equals(kind)?"File on this phone":"File");
    }
    /** What a stamp kept covers, said plainly in its box: each in its own form (decision 230). */
    static String covers(NoteStore.StampRow r) {
        if(r.titled())return Say.t(r.kept()?"The note's title and text as they were saved, in UTF-8. That exact text is kept with the stamp: Open this version shows it.":"The note's title and text as they were saved, in UTF-8.");
        if(r.note())return Say.t(r.kept()?"The note's text as it was saved, without its title, in UTF-8. That exact text is kept with the stamp: Open this version shows it.":"The note's text as it was saved, without its title, in UTF-8.");
        if(r.outside())return Say.t("The file exactly as it was on this phone. It was not put in Mininotes: Verify on Minima asks for it again.");
        return Say.t("The file exactly as it was kept here.");
    }
    private static byte[] bytesOf(MainActivity a,NoteStore.Held file) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();PhoneLock.copyOut(a.store.fileFor(file.id),bytes);return bytes.toByteArray();
    }
    private static void open(MainActivity a,Making making) {
        a.background.submit(()->{String key=a.store.secret(KEY);return new Object[]{key,key.isEmpty()?null:making.make()};},got->{
            if(((String)got[0]).isEmpty()){account(a,()->open(a,making));return;}
            if(got[1]!=null)new Box(a,(Subject)got[1],0).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** Sent to Integritas and kept, waiting for Minima, or found there at once where it already was. Its row. */
    static long send(MainActivity a,String key,Subject s) throws Exception {
        Integritas.Stamped sent=Integritas.stamp(key,s.hash());
        long n=a.store.stamped(s.id(),s.kind(),s.name(),s.hash(),sent.at(),sent.request(),sent.message(),sent.address(),s.title(),s.words());
        Integritas.Block there=Integritas.chosen(sent.already(),sent.at());
        if(there!=null)a.store.stampLanded(n,there.number(),there.txpow(),there.tx(),there.date(),0,System.currentTimeMillis());
        else watch(a).watch(n,0);
        // To my other devices at once (decision 252).
        Post.stampsChanged(a,a.store,Keys.of(a));
        // Its icon wears the stamp from now (decision 249).
        if("file".equals(s.kind()))a.runOnUiThread(a::refresh);
        return n;
    }

    /** One stamp's box, of a row already kept. */
    static void show(MainActivity a,long n){new Box(a,null,n).show();}

    /**
     * Everything about stamping one note or file, from the one row of its menu, Stamp on Minima… (the owner, 2026-10-25: "a
     * combined windows with everything relating to horodating and only one element in the contextual menu of the note";
     * decision 253): the box of what it is now, its fingerprint made here; its stamp where that version has one; where it has
     * none, Stamp, and looking for it on Minima; a file compared with a fingerprint; the versions stamped before. No key is
     * asked for to see it.
     */
    static void thing(MainActivity a,String id,boolean note) {
        a.background.submit(()->{
            Subject s;
            if(note){NoteStore.Note n=a.store.get(id);if(n==null)return null;
                s=new Subject(id,Integritas.TITLED,n.heading(),Integritas.hashOfNote(n.title,n.body),Integritas.noteWords(n.title,n.body).length(),n.title,n.body);}
            else{NoteStore.Held f=a.store.file(id);if(f==null)return null;byte[] bytes=bytesOf(a,f);s=new Subject(id,Integritas.FILE,f.name,Integritas.hash(bytes),bytes.length,null,null);}
            long n=0;for(NoteStore.StampRow r:a.store.stampsOf(id))if(r.hash().equals(s.hash())){n=r.n();break;}
            return new Object[]{s,n};
        },got->{if(got!=null)new Box(a,(Subject)got[0],(Long)got[1]).show();},e->a.alert(MainActivity.READ_FAILED));
    }

    /**
     * The PDF report of a stamp, asked of Integritas and kept (decision 253): attached to the note stamped, or kept beside the
     * file, where it is. Sealed as every file is when the notebook is locked, and going where the note or the folder goes.
     */
    static void attachReport(MainActivity a,NoteStore.StampRow row) {
        a.background.submit(()->a.store.secret(KEY),key->{
            if(key.isEmpty()){account(a,null);return;}
            final int job=a.busy(Say.t("Asking Integritas for the report…"));
            a.network.submit(()->{
                Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);a.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(a).told(row.hash());Post.stampsChanged(a,a.store,Keys.of(a));
                if(f.report()==null||f.report().isEmpty())throw new java.io.IOException("Integritas gave no report this time. Try again in a moment.");
                byte[] pdf=Integritas.download(f.report());
                keepReport(a,row,pdf);return pdf.length;
            },done->{a.busyDone(job,row.note()?"The report is attached to the note":"The report is kept beside the file");a.filesAgain();a.refresh();},
            e->{a.busyDone(job,null);notice(a,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage()));});
        },e->a.alert(MainActivity.READ_FAILED));
    }
    /** The report written in, beside what it is about: the note's attachment, or a file in the same folder. Off the screen's thread. */
    static void keepReport(MainActivity a,NoteStore.StampRow row,byte[] pdf) throws Exception {
        String name=Integritas.reportName(row.name()),note;
        if(row.note())note=row.id();
        else {
            NoteStore.Held f=a.store.file(row.id());if(f==null)throw new java.io.IOException("The file is not here any more.");
            if(f.held!=NoteStore.Branch.Kind.PAGE){a.store.addFileExact(f.note,name,"application/pdf",new java.io.ByteArrayInputStream(pdf));return;}
            note=f.note;
        }
        NoteStore.Held held=a.store.opening(NoteStore.Branch.Kind.PAGE,note,name,"application/pdf",pdf.length);
        java.io.File landing=a.store.fileFor(held.id);
        try(java.io.OutputStream out=new java.io.FileOutputStream(landing)) {
            // Sealed on the way in when the notebook is locked, as every file that comes in is.
            byte[] key=NoteStore.key();
            if(key!=null)Sealed.seal(key,new java.io.ByteArrayInputStream(pdf),out);else out.write(pdf);
        } catch(Exception e){//noinspection ResultOfMethodCallIgnored
            landing.delete();throw e;}
        a.store.keep(a.store.settle(new NoteStore.Held(held.id,held.note,held.name,held.kind,pdf.length,held.added,held.held)));
    }

    /** The stamp's box: the steps one under the other, drawn again as the watch hears something and every second while waiting. */
    static final class Box {
        final MainActivity a;final Subject subject;long n;
        NoteStore.StampRow row;String problem;boolean sending;List<NoteStore.StampRow> before=List.of();
        final LinearLayout body;AlertDialog dialog;
        final Integritas.Watch.Listener ear;
        final Runnable tick=new Runnable(){public void run(){if(dialog!=null&&dialog.isShowing()){if(row!=null&&row.waiting())draw();body.postDelayed(this,1000);}}};

        Box(MainActivity a,Subject subject,long n) {
            this.a=a;this.subject=subject;this.n=n;body=a.inside();
            ear=(heard,landed,trouble)->a.runOnUiThread(()->{if(heard.n()==this.n){row=heard;draw();}});
        }
        void show() {
            MainActivity.Box box=a.new Box();box.setTitle("Stamp on Minima").setView(a.scrolling(body));
            box.setPositiveButton(subject!=null?"Stamp":"Done",null);
            if(subject!=null)box.setNegativeButton("Cancel",null);
            dialog=box.show();
            dialog.setOnDismissListener(d->watch(a).unlisten(ear));
            watch(a).listen(ear);
            if(n>0)read();else{a.background.submit(()->a.store.stampsOf(subject.id()),all->{before=others(all);draw();},e->{});draw();}
            body.postDelayed(tick,1000);
        }
        private void read(){a.background.submit(()->{NoteStore.StampRow r=a.store.stampRow(n);return new Object[]{r,r==null?List.of():a.store.stampsOf(r.id())};},got->{
            row=(NoteStore.StampRow)got[0];@SuppressWarnings("unchecked") List<NoteStore.StampRow> all=(List<NoteStore.StampRow>)got[1];before=others(all);draw();},e->a.alert(MainActivity.READ_FAILED));}
        private List<NoteStore.StampRow> others(List<NoteStore.StampRow> all){List<NoteStore.StampRow> out=new ArrayList<>();for(NoteStore.StampRow one:all)if(one.n()!=n)out.add(one);return out;}

        void draw() {
            body.removeAllViews();
            if(row==null&&subject==null)return;
            String kind=row!=null?row.kind():subject.kind();String name=row!=null?row.name():subject.name();String hash=row!=null?row.hash():subject.hash();
            TextView head=a.label("",MainActivity.READING,a.INK);head.setText("“"+(name.isEmpty()?Say.t("Untitled"):name)+"”   ·   "+kindWords(kind));
            head.setTypeface(head.getTypeface(),android.graphics.Typeface.BOLD);body.addView(head);
            step("✓","1. The fingerprint, made on this phone",a.INK);
            if(subject!=null)quiet(subject.outside()?Say.t("The file exactly as it is on this phone: {0}. It is not put in Mininotes.",Attachment.size(subject.size())):Integritas.what(subject.note(),subject.size()));
            else quiet(covers(row));
            value("SHA-256",hash);
            // Changed since its last stamp: said, so Stamp is read as stamping this version (decision 253).
            if(row==null&&subject!=null&&!before.isEmpty())body.addView(a.label(Say.t("This version is not stamped yet: it changed since it was last stamped. The stamped versions are under Stamped before."),MainActivity.READING,a.INK));
            if(row==null) {
                step(sending?"…":problem!=null?"!":"○","2. Sent to Integritas",sending||problem!=null?a.INK:a.MUTED);
                TextView said=a.label(sending?Say.t("Sending to Integritas…"):problem!=null?problem:Say.t("Only this fingerprint goes, through your own Integritas account, which records it on Minima with the date. Stamping uses one stamp of your account."),MainActivity.READING,problem!=null&&!sending?a.WARN:a.MUTED);
                body.addView(said);
                step("○","3. Recorded on Minima",a.MUTED);
                // What can be done without stamping (decision 253).
                if(subject!=null&&!subject.outside())body.addView(a.tapRow("Look for it on Minima",()->{if(subject.note())verifyNote(a,subject.id(),subject.name());else a.background.submit(()->a.store.file(subject.id()),f->{if(f!=null)verifyFile(a,f);},e->{});}));
                if(subject!=null&&!subject.note()&&!subject.outside())body.addView(a.tapRow("Compare with a fingerprint…",()->a.background.submit(()->a.store.file(subject.id()),f->{if(f!=null)compareFile(a,f);},e->{})));
            } else {
                step("✓","2. Sent to Integritas",a.INK);
                quiet(Say.t("Sent {0}",Integritas.when(row.at())));
                if(!row.request().isEmpty())value(Say.t("Request"),row.request());
                if(!row.said().isEmpty())quiet(Integritas.answered(row.said()));
                if(!row.address().isEmpty())value(Say.t("Minima address"),row.address(),Integritas.addressLink(row.address()));
                Integritas.Watch w=watch(a);
                if(row.confirmed()) {
                    step("✓","3. Recorded on Minima",a.INK);
                    body.addView(a.label(Say.t("Confirmed: the fingerprint is in block {0} of the Minima blockchain.",String.valueOf(row.block())),MainActivity.READING,a.INK));
                    // Each on the Minima Explorer where it can be shown there, as the PDF report links them (decision 224).
                    value(Say.t("Block"),String.valueOf(row.block()),Integritas.blockLink(row.block()));
                    if(!row.recorded().isEmpty())value(Say.t("Recorded"),Integritas.when(row.recorded()));
                    if(!row.txpow().isEmpty())value(Say.t("Transaction (txpow)"),row.txpow(),Integritas.txpowLink(row.txpow()));
                    if(!row.tx().isEmpty())value(Say.t("Transaction id"),row.tx());
                    if(!row.nft().isEmpty())value(Say.t("NFT coin id"),row.nft());
                    if(!row.nftTx().isEmpty())value(Say.t("NFT transaction"),row.nftTx(),Integritas.txpowLink(row.nftTx()));
                } else if(row.waiting()) {
                    step("…","3. Waiting for Minima",a.INK);
                    quiet(Say.t("The fingerprint is in a transaction on its way into a block, usually within a few minutes. This phone looks for it, less and less often, even with this box closed, and says when it is there."));
                    long next=w.nextLook(row.n());long in=next<=0?0:Math.max(0,(next-System.currentTimeMillis()+999)/1000);
                    String looked=row.tries()==0?Say.t("Not looked for yet."):Say.t("Looked for {0} times, last at {1}.",String.valueOf(row.tries()),Integritas.when(row.checked()));
                    body.addView(a.label(looked+"  "+(next<=0?Say.t("Looking now…"):Say.t("Next look in {0} s.",String.valueOf(in))),MainActivity.READING,a.INK));
                    String trouble=w.trouble(row.n());if(trouble!=null)body.addView(a.label(Say.t(trouble),MainActivity.READING,a.WARN));
                } else {
                    step("!","3. Not found on Minima yet",a.INK);
                    quiet(Say.t("It was looked for {0} times, for about two hours, and is not on Minima yet. Verify on Minima looks once more and goes on looking.",String.valueOf(row.tries())));
                }
                final NoteStore.StampRow r=row;
                body.addView(a.tapRow("Copy the details",()->{copy(a,Integritas.details(r));notice(a,Say.t("Copied"));}));
                if(r.note()&&r.kept())body.addView(a.tapRow("Open this version",()->version(a,r.n())));
                if(r.confirmed())body.addView(a.tapRow("Open the PDF report",()->report(a,r)));
                // The report kept with it, sealed as its files are, going where they go (decision 253).
                if(r.confirmed()&&!r.outside())body.addView(a.tapRow(r.note()?"Attach the PDF report":"Keep the PDF report beside it",()->attachReport(a,r)));
                body.addView(verifyLine(a,r));
                if(!r.note()&&!r.outside())body.addView(a.tapRow("Compare with a fingerprint…",()->a.background.submit(()->a.store.file(r.id()),f->{if(f!=null)compareFile(a,f);},e->{})));
            }
            if(!before.isEmpty()) {
                body.addView(a.spaced(a.label("Stamped before",MainActivity.QUIET,a.MUTED)));
                for(NoteStore.StampRow one:before)body.addView(a.tapRow(Integritas.when(one.at())+"   ·   "+state(one),()->{dialog.dismiss();PhoneStamp.show(a,one.n());}));
            }
            // The one button: Stamp, until it is sent; Done after.
            android.widget.Button main=dialog==null?null:dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if(main!=null) {
                if(row==null){main.setText(Say.t(problem!=null?"Try again":"Stamp"));main.setEnabled(!sending);main.setOnClickListener(v->stamp());}
                else{main.setText(Say.t("Done"));main.setEnabled(true);main.setOnClickListener(v->dialog.dismiss());
                    android.widget.Button cancel=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);if(cancel!=null)cancel.setVisibility(View.GONE);}
            }
        }
        void stamp() {
            sending=true;problem=null;draw();
            // No account linked yet: My profile at Stamping; this box stays (decision 253).
            a.background.submit(()->a.store.secret(KEY),key->{if(key.isEmpty()){sending=false;draw();account(a,null);return;}a.network.submit(()->{long made=send(a,key,subject);return a.store.stampRow(made);},r->{
                sending=false;row=r;n=r.n();draw();a.stampLineAgain();a.filesAgain();
                notice(a,r.confirmed()?Say.t("{0} is on Minima: block {1}",named(r),String.valueOf(r.block())):Say.t("Sent to Integritas, waiting for Minima"));
            },e->{sending=false;problem=Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage());draw();});},e->a.alert(MainActivity.READ_FAILED));
        }
        private void step(String mark,String words,int colour) {
            TextView t=a.label("",MainActivity.READING,colour);t.setText(mark+"  "+Say.t(words));t.setTypeface(t.getTypeface(),android.graphics.Typeface.BOLD);
            t.setPadding(0,a.dp(14),0,a.dp(2));body.addView(t);
        }
        /** Words of the box, at the size of its buttons' words (the owner, 2026-10-19: "make it the same size as the buttons' text"; decision 224). */
        private void quiet(String words){TextView t=a.label("",MainActivity.READING,a.MUTED);t.setText(words);t.setTextIsSelectable(true);body.addView(t);}
        private void value(String label,String value){value(label,value,null);}
        private void value(String label,String value,String link){PhoneStamp.value(a,body,label,value,link);}
    }

    /**
     * A label, the value whole in letters of one width at the reading size, wrapped and held to select, and under it Copy and,
     * where the Minima Explorer shows it, Minima Explorer, which opens it there (decision 224).
     */
    static void value(MainActivity a,LinearLayout body,String label,String value,String link) {
        TextView what=a.label("",MainActivity.READING,a.MUTED);what.setText(label);what.setPadding(0,a.dp(8),0,0);body.addView(what);
        TextView shown=a.label("",MainActivity.READING,link!=null?a.ACCENT:a.INK);shown.setText(value);shown.setTypeface(android.graphics.Typeface.MONOSPACE);shown.setTextIsSelectable(true);
        if(link!=null)shown.setOnClickListener(v->explore(a,link));
        body.addView(shown);
        LinearLayout under=new LinearLayout(a);under.setGravity(Gravity.CENTER_VERTICAL);
        TextView copy=a.label("Copy",MainActivity.READING,a.ACCENT);copy.setPadding(0,a.dp(6),a.dp(22),a.dp(6));copy.setOnClickListener(v->{copy(a,value);notice(a,Say.t("Copied"));});under.addView(copy);
        if(link!=null){TextView there=a.label("Minima Explorer  ›",MainActivity.READING,a.ACCENT);there.setPadding(0,a.dp(6),0,a.dp(6));there.setOnClickListener(v->explore(a,link));under.addView(there);}
        body.addView(under);
    }
    /** A page of the Minima Explorer opened in the browser; its address copied where none opens it. */
    static void explore(MainActivity a,String link){try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)));}catch(Exception none){copy(a,link);notice(a,Say.t("The link is copied"));}}
    /**
     * Verify on Minima, for a stamp not found there yet, its first look; Verify again once it has been found, which checks it
     * there anew with the PDF report (the owner, 2026-10-19; decision 224).
     */
    static View verifyLine(MainActivity a,NoteStore.StampRow r) {
        return a.tapRow(r.confirmed()?"Verify again":"Verify on Minima",()->{if(r.outside())again(a,r);else if(r.confirmed())verifyRow(a,r);else{watch(a).now(r.n());notice(a,Say.t("Looking for it on Minima…"));}});
    }
    /**
     * A file stamped from the phone's storage, verified: Mininotes never had it, so it is picked again, its SHA-256 compared
     * with the stamp's, and only the same file is looked for on Minima (decision 230).
     */
    static void again(MainActivity a,NoteStore.StampRow r) {
        a.toast("Pick the file stamped again, to compare it.");
        pick(a,(now,uri)->now.background.submit(()->hashOf(now,uri),h->{
            String hash=(String)h[0];
            if(!hash.equals(r.hash())) {
                now.new Box().setTitle("Verify on Minima…").setMessage(Say.t("This is not the file stamped as {0}: even one byte makes another fingerprint.","“"+r.name()+"”")
                    +"\n\n"+Say.t("Fingerprint (SHA-256): {0}",hash)).setPositiveButton("Done",null).show();
                return;
            }
            if(r.confirmed())verify(now,r.name(),()->new Object[]{hash,h[1]});
            else{watch(now).now(r.n());notice(now,Say.t("It is the file stamped. Looking for it on Minima…"));}
        },e->now.alert("That file could not be read.")));
    }

    /** How far a stamp is, in a few words. */
    static String state(NoteStore.StampRow s) {
        if(s.confirmed())return Say.t("On Minima, block {0}",String.valueOf(s.block()));
        return Say.t(s.waiting()?"Waiting for Minima":"Not found on Minima yet");
    }
    static void copy(MainActivity a,String text) {
        android.content.ClipboardManager board=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if(board!=null)board.setPrimaryClip(android.content.ClipData.newPlainText("Minima",text));
    }

    /** The PDF report Integritas makes of a verification, its link good for about an hour. */
    static void report(MainActivity a,NoteStore.StampRow row) {
        a.background.submit(()->a.store.secret(KEY),key->{
            if(key.isEmpty()){account(a,()->report(a,row));return;}
            final int job=a.busy(Say.t("Asking Integritas for the report…"));
            // The NFT it mints for the report kept with the stamp, to be seen on the Minima Explorer (decision 224).
            a.network.submit(()->{Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);a.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(a).told(row.hash());Post.stampsChanged(a,a.store,Keys.of(a));return f;},found->{
                a.busyDone(job,null);
                if(found.report()==null||found.report().isEmpty()){notice(a,Say.t("Integritas gave no report this time. Try again in a moment."));return;}
                try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(found.report())));}catch(Exception none){copy(a,found.report());notice(a,Say.t("The report's link is copied"));}
            },e->{a.busyDone(job,null);notice(a,Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage()));});
        },e->a.alert(MainActivity.READ_FAILED));
    }
    static void verifyRow(MainActivity a,NoteStore.StampRow row){verify(a,row.name(),()->new Object[]{row.hash(),-1L});}

    /** A note's stamped version, read only, with Copy and Put this version back (decision 218). */
    static void version(MainActivity a,long n) {
        a.background.submit(()->new Object[]{a.store.stampRow(n),a.store.stampedWords(n)},got->{
            NoteStore.StampRow row=(NoteStore.StampRow)got[0];String[] words=(String[])got[1];
            if(row==null||words==null){notice(a,Say.t("This stamp was made before Mininotes kept the stamped text."));return;}
            // Each in the form it was stamped in: a titled one its title and text, an older one its text alone (decision 230).
            boolean same;try{same=(row.titled()?Integritas.hashOfNote(words[0],words[1]):Integritas.hashOfText(words[1])).equals(row.hash());}catch(Exception never){same=false;}
            final String copied=row.titled()?Integritas.noteWords(words[0],words[1]):words[1];
            LinearLayout body=a.inside();
            TextView when=a.label("",MainActivity.QUIET,a.INK);when.setText(Say.t("Stamped {0}",Integritas.when(row.at()))+"   ·   "+state(row));body.addView(when);
            TextView check=a.label("",MainActivity.QUIET,same?a.MUTED:a.WARN);check.setText(same?Say.t("Its fingerprint is the one stamped: SHA-256 {0}",row.hash()):Say.t("This text does not give the fingerprint stamped."));body.addView(check);
            TextView text=a.label("",MainActivity.READING,a.INK);text.setText(words[1].isEmpty()?Say.t("(empty)"):words[1]);text.setTextIsSelectable(true);text.setPadding(0,a.dp(10),0,0);body.addView(text);
            body.addView(a.tapRow("Copy",()->{copy(a,copied);notice(a,Say.t("Copied"));}));
            String heading=words[0].isEmpty()?row.name():words[0];
            a.new Box().setTitle(Say.t("Stamped version of {0}","“"+(heading.isEmpty()?Say.t("Untitled"):heading)+"”")).setView(a.scrolling(body))
                .setPositiveButton("Put this version back",(d,w)->a.putStampedBack(row)).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    // ---- verifying ----------------------------------------------------------------------------------------------------

    private interface Hashing{Object[] hash() throws Exception;}

    /** A file looked for on Minima. */
    /**
     * A file compared with a fingerprint the person has (decision 250): its own SHA-256, made here from its bytes, and a field
     * to paste the other, filled from the clipboard where one is there; the answer as it is typed. Nothing leaves the phone.
     */
    static void compareFile(MainActivity a,NoteStore.Held file) {
        a.background.submit(()->Integritas.hash(bytesOf(a,file)),mine->{
            LinearLayout body=a.inside();
            body.addView(a.label(Say.t("Paste the fingerprint (SHA-256) you were given for {0}.","“"+file.name+"”"),MainActivity.READING,a.INK));
            body.addView(a.label(Say.t("This file's fingerprint (SHA-256)"),MainActivity.QUIET,a.MUTED));
            TextView own=a.label(mine,MainActivity.QUIET,a.INK);own.setTypeface(android.graphics.Typeface.MONOSPACE);own.setTextIsSelectable(true);
            body.addView(own);
            final EditText theirs=a.field("The fingerprint you have",200);body.addView(theirs);
            try{android.content.ClipboardManager clip=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                CharSequence text=clip!=null&&clip.hasPrimaryClip()&&clip.getPrimaryClip().getItemCount()>0?clip.getPrimaryClip().getItemAt(0).getText():null;
                if(text!=null&&Integritas.fingerprintIn(text.toString())!=null)theirs.setText(text.toString().trim());}catch(RuntimeException noText){/* typed instead */}
            final TextView answer=a.label(" ",MainActivity.READING,a.INK);body.addView(answer);
            final Runnable judge=()->{String typed=theirs.getText().toString();Boolean same=Integritas.sameFingerprint(mine,typed);
                answer.setText(same==null?(typed.isBlank()?" ":Say.t("Not a SHA-256 fingerprint: 64 characters, 0 to 9 and a to f.")):same?Say.t("The same: this is exactly that file."):Say.t("Not the same: this is not that file, or it changed since."));
                answer.setTextColor(same==null?a.MUTED:same?a.ACCENT:a.WARN);};
            theirs.addTextChangedListener(new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int b,int c){}
                public void afterTextChanged(android.text.Editable e){judge.run();}});
            judge.run();
            a.new Box().setTitle("Compare with a fingerprint…").setView(a.scrolling(body)).setPositiveButton("Done",null).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    static void verifyFile(MainActivity a,NoteStore.Held file) {
        verify(a,file.name,()->{byte[] bytes=bytesOf(a,file);return new Object[]{Integritas.hash(bytes),(long)bytes.length};});
    }
    /**
     * A note, as it is now, looked for on Minima: its title and text first, as notes are stamped from 0.4.018, then, where
     * Minima does not have that, its text alone, as they were stamped before, and the answer says which (decision 230).
     */
    static void verifyNote(MainActivity a,String id,String name) {
        verify(a,name,()->{NoteStore.Note n=a.store.get(id);String title=n==null?"":n.title,body=n==null?"":n.body;
            String titled=Integritas.hashOfNote(title,body),alone=Integritas.hashOfText(body);
            return new Object[]{titled,-1L,alone.equals(titled)?null:alone};});
    }

    private static void verify(MainActivity a,String name,Hashing hashing) {
        a.background.submit(()->a.store.secret(KEY),key->{
            if(key.isEmpty()){account(a,()->verify(a,name,hashing));return;}
            final int job=a.busy(Say.t("Looking for it on Minima…"));
            a.network.submit(()->{Object[] h=hashing.hash();String hash=(String)h[0];Integritas.Found f=Integritas.verify(key,hash,name,(Long)h[1],true);boolean alone=false;
                // Not there with its title: perhaps as its text alone, as a note was stamped before titles were (decision 230).
                if(!f.match()&&h.length>2&&h[2]!=null){Integritas.Found older=Integritas.verify(key,(String)h[2],name,(Long)h[1],true);if(older.match()){f=older;hash=(String)h[2];alone=true;}}
                // Where it was found, kept with the stamps of the same fingerprint (decisions 203 and 218).
                if(f.match()&&!f.blocks().isEmpty()){Integritas.Block b=Integritas.chosen(f.blocks(),0);if(b!=null)a.store.stampFound(hash,b.number(),b.txpow(),b.date());}
                a.store.stampNft(hash,f.nft(),f.nftTx());watch(a).told(hash);Post.stampsChanged(a,a.store,Keys.of(a));
                return new Object[]{f,hash,alone};},got->{
                a.busyDone(job,null);a.stampLineAgain();
                Integritas.Found found=(Integritas.Found)got[0];
                StringBuilder said=new StringBuilder();
                if(found.match()) {
                    said.append(Say.t("{0} is on Minima, unchanged.","“"+name+"”"));
                    if((Boolean)got[2])said.append("\n\n").append(Say.t("Found as its text alone, stamped before titles were included."));
                    for(Integritas.Block b:found.blocks()){said.append("\n\n").append(Say.t("Recorded {0}, block {1}",Integritas.when(b.date()),String.valueOf(b.number())));
                        if(!b.txpow().isEmpty())said.append('\n').append(Say.t("Transaction: {0}",b.txpow()));}
                } else said.append(Say.t("{0} is not on Minima as it is now.","“"+name+"”")).append("\n\n").append(Say.t("Either it was never stamped, or it changed since: even one letter makes another fingerprint."));
                said.append("\n\n").append(Say.t("Fingerprint (SHA-256): {0}",(String)got[1]));
                MainActivity.Box box=a.new Box();box.setTitle("Verify on Minima…").setMessage(said.toString()).setPositiveButton("Done",null);
                if(found.report()!=null&&!found.report().isEmpty())
                    box.setNeutralButton("Open the PDF report",(d,w)->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(found.report())));}catch(Exception none){copy(a,found.report());a.toast("The report's link is copied");}});
                box.show();
            },e->{a.busyDone(job,null);notice(a,e.getMessage()==null?Say.t("Integritas could not be reached. Try again in a moment."):Say.t(e.getMessage()));});
        },e->a.alert(MainActivity.READ_FAILED));
    }

    // ---- everything stamped -------------------------------------------------------------------------------------------

    /**
     * Everything stamped from this phone, a plain list, the newest first, one line each: its name, what it is, when, and how
     * far, and a tap opens its own box with everything known of it (the owner, 2026-10-21: "When we click in the menu,
     * stamped on Minima, we should just have the list of everything we stamped, not directly the details, this should come
     * afterward when clicking one element"; decision 230). Above it, that stamping is Integritas, with its site as a link, and
     * under a heading of their own the stamps; at the foot, where Done was, the round + of Home that stamps something more
     * (the owner, 2026-10-21: "Remove the done and put the plus there similar to the + on the home page. Make know more a
     * clickable link and use the integritas.technology label"; decision 231). With no button the box goes as every box goes,
     * with Back or a tap outside it (MainActivity.Box). Always as the notebook has it now (decision 224): read again whenever
     * the watch hears something, and a stamp of a fingerprint another stamp found is found as well (NoteStore.stampsAgree).
     */
    static void list(MainActivity a) {
        a.background.submit(()->a.store.stamps(),rows->{
            LinearLayout body=a.inside();
            AlertDialog box=a.new Box().setTitle("Stamped on Minima").setView(a.scrolling(body)).show();
            fill(a,body,rows,box);
            Integritas.Watch.Listener ear=(heard,landed,trouble)->a.background.submit(()->a.store.stamps(),now->{if(box.isShowing())fill(a,body,now,box);},e->{});
            watch(a).listen(ear);box.setOnDismissListener(d->watch(a).unlisten(ear));
        },e->a.alert(MainActivity.READ_FAILED));
    }
    private static void fill(MainActivity a,LinearLayout body,List<NoteStore.StampRow> rows,AlertDialog box) {
        body.removeAllViews();
        // What it uses, said once and quietly, and its site, a link that says where it goes (the owner, 2026-10-21: "we should
        // mention that it uses integritas with a know more with the link to their website"; decision 231: its words are the site's).
        body.addView(a.label("Stamping uses Integritas: through your own Integritas account, each fingerprint is recorded on the Minima blockchain with the date.",MainActivity.QUIET,a.MUTED));
        body.addView(a.link("integritas.technology",()->explore(a,Integritas.SITE)),new LinearLayout.LayoutParams(-2,-2));
        // Said to be the stamped things, and they under it (the owner, 2026-10-21: "Make it clear that this is a section of
        // stamped elements, label it and list the elements underneath").
        body.addView(a.part("Stamped notes and files"));
        if(rows.isEmpty())body.addView(a.spaced(a.label("Nothing is stamped yet.",MainActivity.READING,a.INK)));
        for(NoteStore.StampRow one:rows)
            body.addView(a.row(one.name().isEmpty()?Say.t("Untitled"):one.name(),kindWords(one.kind())+"   ·   "+Integritas.when(one.at())+"   ·   "+state(one),()->show(a,one.n())));
        // The account is set in one place, My profile's Stamping (the owner, 2026-10-21: "we need to be brought on the
        // dedicated section on the profile page"; decision 231): this box goes, and it opens there.
        body.addView(a.tapRow("Your Integritas account…",()->{box.dismiss();a.profile(MainActivity.STAMPING,null);}));
        // The + at the foot, on the right where the thumb is, as Home's (decision 231).
        LinearLayout foot=new LinearLayout(a);foot.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);foot.setPadding(0,a.dp(12),a.dp(4),a.dp(8));
        foot.addView(a.roundPlus("Stamp something…",v->{box.dismiss();stampSomething(a);}),new LinearLayout.LayoutParams(a.dp(56),a.dp(56)));
        body.addView(foot,new LinearLayout.LayoutParams(-1,-2));
    }

    /**
     * The + of Stamped on Minima: a note or a file in Mininotes, or a file from the phone's own storage (the owner,
     * 2026-10-21: it "should give the option, from the app or external from the drive"). The list's box is closed first, so
     * the menu stands over the screen, not under the box.
     */
    static void stampSomething(MainActivity a) {
        MainActivity.Sheet sheet=a.new Sheet();
        sheet.heading("Stamp on Minima");
        sheet.row("A note or file in Mininotes…",()->choose(a));
        sheet.row("A file on this phone…",()->outside(a));
        sheet.show(null);
    }

    /**
     * A note or a file of the notebook, chosen to stamp: the notes written in lately, newest first, and, as a word is typed,
     * the notes and files it is found in, as Search finds them (NoteStore.lookingEverywhere). The one tapped goes to its box.
     */
    static void choose(MainActivity a) {
        LinearLayout body=a.inside();
        final EditText word=a.field("A word to look for",80);body.addView(word);
        final LinearLayout found=a.column();body.addView(found);
        final AlertDialog box=a.new Box().setTitle("A note or file in Mininotes").setView(a.scrolling(body)).setNegativeButton("Cancel",null).create();
        final Runnable look=()->{
            final String term=word.getText().toString().trim();
            a.background.submit(()->term.length()<2?a.store.lately(30):a.store.lookingEverywhere(term),hits->{
                if(!box.isShowing()||!word.getText().toString().trim().equals(term))return;
                found.removeAllViews();
                int shown=0;
                for(final NoteStore.Branch one:hits) {
                    if(one.kind!=NoteStore.Branch.Kind.PAGE&&one.kind!=NoteStore.Branch.Kind.FILE)continue;
                    found.addView(a.row(one.name.isEmpty()?Say.t("Untitled"):one.name,one.detail,()->{box.dismiss();chosen(a,one);}));shown++;
                }
                if(shown==0)found.addView(a.spaced(a.label(term.length()<2?"Nothing here yet.":"Nothing found.",MainActivity.READING,a.MUTED)));
                else if(term.length()<2)found.addView(a.spaced(a.label("Type a word to find a file as well.",MainActivity.QUIET,a.MUTED)));
            },e->{found.removeAllViews();found.addView(a.spaced(a.label(MainActivity.READ_FAILED,MainActivity.READING,a.WARN)));});
        };
        final Runnable[] waiting={null};
        word.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(android.text.Editable e){if(waiting[0]!=null)word.removeCallbacks(waiting[0]);waiting[0]=look;word.postDelayed(look,200);}});
        box.show();
        look.run();
    }
    /** The note or file chosen, in its stamp's box: a file read as the notebook keeps it. */
    private static void chosen(MainActivity a,NoteStore.Branch one) {
        if(one.kind==NoteStore.Branch.Kind.PAGE){note(a,one.id,one.name);return;}
        a.background.submit(()->a.store.file(one.id),held->{if(held==null)a.alert(MainActivity.READ_FAILED);else file(a,held);},e->a.alert(MainActivity.READ_FAILED));
    }

    static String when(String iso){return Integritas.when(iso);}
}
