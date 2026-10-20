// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
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
            if(landed)notice(now,Say.t("{0} is on Minima: block {1}",named(row),String.valueOf(row.block())));
            else if(NoteStore.STAMP_UNSEEN.equals(row.state()))notice(now,Say.t("{0} was not found on Minima in two hours. Stamped on Minima… can look again.",named(row)));
            now.stampLineAgain();now.filesAgain();
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

    /** The account linked, linked again, or unlinked: the key from the person's profile at integritas.technology. */
    static void account(MainActivity a,Runnable then) {
        a.background.submit(()->a.store.secret(KEY),had->{
            LinearLayout body=a.inside();
            body.addView(a.label("Stamping records a file's fingerprint on the Minima blockchain with the date, through your own Integritas account, so you can prove later that it existed then, unchanged. The file itself never leaves this phone.",MainActivity.QUIET,a.INK));
            body.addView(a.label("Paste the API key from your profile at integritas.technology. It is kept in your notebook on this phone, locked with it, and sent only to Integritas.",MainActivity.QUIET,a.MUTED));
            EditText field=a.field("Integritas API key",400);field.setText(had);
            field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            body.addView(field);
            body.addView(a.tapRow("integritas.technology",()->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(Integritas.SITE)));}catch(Exception none){a.toast(Integritas.SITE);}}));
            MainActivity.Box box=a.new Box();box.setTitle("Your Integritas account").setView(a.scrolling(body));
            box.setPositiveButton("Link",(d,w)->{
                String key=field.getText().toString().trim();
                if(!Integritas.looksLikeAKey(key)){a.toast("That does not look like an API key.");return;}
                a.background.submit(()->{a.store.setSecret(KEY,key);a.store.setSecret(KEY+".at",String.valueOf(System.currentTimeMillis()));return null;},v->{
                    // And to the devices chosen for it in My profile (decision 223).
                    a.network.submit(()->Post.sendKeyToChosen(a,a.store,a.keys(),KEY),said->{},e->{});notice(a,Say.t("Integritas is linked"));if(then!=null)then.run();},e->a.alert("Could not keep that key. Nothing was changed."));
            });
            if(!had.isEmpty())box.setNeutralButton("Unlink",(d,w)->a.background.submit(()->{a.store.setSecret(KEY,"");a.store.setSecret(KEY+".at",String.valueOf(System.currentTimeMillis()));return null;},v->notice(a,Say.t("Integritas is unlinked")),e->{}));
            box.show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    // ---- stamping -----------------------------------------------------------------------------------------------------

    /** What is about to be stamped: a note's saved text, kept as it is, or a file's bytes. */
    record Subject(String id,String kind,String name,String hash,long size,String title,String words) {
        boolean note(){return "note".equals(kind);}
    }
    private interface Making{Subject make() throws Exception;}

    /** A file stamped, in its box. */
    static void file(MainActivity a,NoteStore.Held file) {
        open(a,()->{byte[] bytes=bytesOf(a,file);return new Subject(file.id,"file",file.name,Integritas.hash(bytes),bytes.length,null,null);});
    }
    /** A note's text, as it is saved now, stamped in its box: exactly that text is fingerprinted and kept. */
    static void note(MainActivity a,String id,String name) {
        open(a,()->{NoteStore.Note n=a.store.get(id);if(n==null)return null;return new Subject(id,"note",n.heading(),Integritas.hashOfText(n.body),n.body.length(),n.title,n.body);});
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
        return n;
    }

    /** One stamp's box, of a row already kept. */
    static void show(MainActivity a,long n){new Box(a,null,n).show();}

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
            boolean note=row!=null?row.note():subject.note();String name=row!=null?row.name():subject.name();String hash=row!=null?row.hash():subject.hash();
            TextView head=a.label("",MainActivity.READING,a.INK);head.setText("“"+(name.isEmpty()?Say.t("Untitled"):name)+"”   ·   "+Say.t(note?"Note":"File"));
            head.setTypeface(head.getTypeface(),android.graphics.Typeface.BOLD);body.addView(head);
            step("✓","1. The fingerprint, made on this phone",a.INK);
            if(subject!=null)quiet(Integritas.what(subject.note(),subject.size()));
            else quiet(Say.t(row.note()?(row.kept()?"The note's text as it was saved, without its title, in UTF-8. That exact text is kept with the stamp: Open this version shows it.":"The note's text as it was saved, without its title, in UTF-8."):"The file exactly as it was kept here."));
            value("SHA-256",hash);
            if(row==null) {
                step(sending?"…":problem!=null?"!":"○","2. Sent to Integritas",sending||problem!=null?a.INK:a.MUTED);
                TextView said=a.label(sending?Say.t("Sending to Integritas…"):problem!=null?problem:Say.t("Only this fingerprint goes, through your own Integritas account, which records it on Minima with the date. Stamping uses one stamp of your account."),MainActivity.READING,problem!=null&&!sending?a.WARN:a.MUTED);
                body.addView(said);
                step("○","3. Recorded on Minima",a.MUTED);
            } else {
                step("✓","2. Sent to Integritas",a.INK);
                quiet(Say.t("Sent {0}",Integritas.when(row.at())));
                if(!row.request().isEmpty())value(Say.t("Request"),row.request());
                if(!row.said().isEmpty())quiet(Say.t("Integritas said: {0}",row.said()));
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
                body.addView(verifyLine(a,r));
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
            a.background.submit(()->a.store.secret(KEY),key->a.network.submit(()->{long made=send(a,key,subject);return a.store.stampRow(made);},r->{
                sending=false;row=r;n=r.n();draw();a.stampLineAgain();a.filesAgain();
                notice(a,r.confirmed()?Say.t("{0} is on Minima: block {1}",named(r),String.valueOf(r.block())):Say.t("Sent to Integritas, waiting for Minima"));
            },e->{sending=false;problem=Say.t(e.getMessage()==null?"Integritas could not be reached. Try again in a moment.":e.getMessage());draw();}),e->a.alert(MainActivity.READ_FAILED));
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
        return a.tapRow(r.confirmed()?"Verify again":"Verify on Minima",()->{if(r.confirmed())verifyRow(a,r);else{watch(a).now(r.n());notice(a,Say.t("Looking for it on Minima…"));}});
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
            a.network.submit(()->{Integritas.Found f=Integritas.verify(key,row.hash(),row.name(),-1,true);a.store.stampNft(row.hash(),f.nft(),f.nftTx());watch(a).told(row.hash());return f;},found->{
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
            boolean same;try{same=Integritas.hashOfText(words[1]).equals(row.hash());}catch(Exception never){same=false;}
            LinearLayout body=a.inside();
            TextView when=a.label("",MainActivity.QUIET,a.INK);when.setText(Say.t("Stamped {0}",Integritas.when(row.at()))+"   ·   "+state(row));body.addView(when);
            TextView check=a.label("",MainActivity.QUIET,same?a.MUTED:a.WARN);check.setText(same?Say.t("Its fingerprint is the one stamped: SHA-256 {0}",row.hash()):Say.t("This text does not give the fingerprint stamped."));body.addView(check);
            TextView text=a.label("",MainActivity.READING,a.INK);text.setText(words[1].isEmpty()?Say.t("(empty)"):words[1]);text.setTextIsSelectable(true);text.setPadding(0,a.dp(10),0,0);body.addView(text);
            body.addView(a.tapRow("Copy",()->{copy(a,words[1]);notice(a,Say.t("Copied"));}));
            String heading=words[0].isEmpty()?row.name():words[0];
            a.new Box().setTitle(Say.t("Stamped version of {0}","“"+(heading.isEmpty()?Say.t("Untitled"):heading)+"”")).setView(a.scrolling(body))
                .setPositiveButton("Put this version back",(d,w)->a.putStampedBack(row)).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    // ---- verifying ----------------------------------------------------------------------------------------------------

    private interface Hashing{Object[] hash() throws Exception;}

    /** A file looked for on Minima. */
    static void verifyFile(MainActivity a,NoteStore.Held file) {
        verify(a,file.name,()->{byte[] bytes=bytesOf(a,file);return new Object[]{Integritas.hash(bytes),(long)bytes.length};});
    }
    /** A note's words, as they are now, looked for on Minima. */
    static void verifyNote(MainActivity a,String id,String name) {
        verify(a,name,()->{NoteStore.Note n=a.store.get(id);return new Object[]{Integritas.hashOfText(n==null?"":n.body),-1L};});
    }

    private static void verify(MainActivity a,String name,Hashing hashing) {
        a.background.submit(()->a.store.secret(KEY),key->{
            if(key.isEmpty()){account(a,()->verify(a,name,hashing));return;}
            final int job=a.busy(Say.t("Looking for it on Minima…"));
            a.network.submit(()->{Object[] h=hashing.hash();Integritas.Found f=Integritas.verify(key,(String)h[0],name,(Long)h[1],true);
                // Where it was found, kept with the stamps of the same fingerprint (decisions 203 and 218).
                if(f.match()&&!f.blocks().isEmpty()){Integritas.Block b=Integritas.chosen(f.blocks(),0);if(b!=null)a.store.stampFound((String)h[0],b.number(),b.txpow(),b.date());}
                a.store.stampNft((String)h[0],f.nft(),f.nftTx());watch(a).told((String)h[0]);
                return new Object[]{f,h[0]};},got->{
                a.busyDone(job,null);a.stampLineAgain();
                Integritas.Found found=(Integritas.Found)got[0];
                StringBuilder said=new StringBuilder();
                if(found.match()) {
                    said.append(Say.t("{0} is on Minima, unchanged.","“"+name+"”"));
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
     * Everything stamped from this phone, each version its own card (decisions 203 and 218): Details… opens its box. Always as
     * the notebook has it now (decision 224): read again whenever the watch hears something, and a stamp of a fingerprint
     * another stamp found is found as well (NoteStore.stampsAgree).
     */
    static void list(MainActivity a) {
        a.background.submit(()->a.store.stamps(),rows->{
            LinearLayout body=a.inside();
            fill(a,body,rows);
            AlertDialog box=a.new Box().setTitle("Stamped on Minima").setView(a.scrolling(body)).setPositiveButton("Done",null).show();
            Integritas.Watch.Listener ear=(heard,landed,trouble)->a.background.submit(()->a.store.stamps(),now->{if(box.isShowing())fill(a,body,now);},e->{});
            watch(a).listen(ear);box.setOnDismissListener(d->watch(a).unlisten(ear));
        },e->a.alert(MainActivity.READ_FAILED));
    }
    private static void fill(MainActivity a,LinearLayout body,List<NoteStore.StampRow> rows) {
        body.removeAllViews();
        body.addView(a.label("Stamp on Minima records that a file or a note exists as it is now. Verify on Minima checks later whether it is still the same, and when it was recorded.",MainActivity.READING,a.MUTED));
        if(rows.isEmpty())body.addView(a.spaced(a.label("Nothing stamped from this phone yet.",MainActivity.READING,a.INK)));
        for(NoteStore.StampRow one:rows) {
            LinearLayout card=a.framed(body);
            TextView name=a.label("",MainActivity.READING,a.INK);name.setText((one.name().isEmpty()?Say.t("Untitled"):one.name())+"   ·   "+Say.t(one.note()?"Note":"File"));
            name.setTypeface(name.getTypeface(),android.graphics.Typeface.BOLD);card.addView(name);
            StringBuilder all=new StringBuilder();
            all.append(Say.t("Stamped {0}",Integritas.when(one.at()))).append("   ·   ").append(state(one));
            if(one.confirmed()&&!one.recorded().isEmpty())all.append('\n').append(Say.t("Recorded {0}",Integritas.when(one.recorded())));
            TextView said=a.label("",MainActivity.READING,one.confirmed()?a.INK:a.MUTED);said.setText(all.toString());card.addView(said);
            if(one.confirmed())value(a,card,Say.t("Block"),String.valueOf(one.block()),Integritas.blockLink(one.block()));
            if(!one.txpow().isEmpty())value(a,card,Say.t("Transaction (txpow)"),one.txpow(),Integritas.txpowLink(one.txpow()));
            value(a,card,Say.t("Fingerprint (SHA-256)"),one.hash(),null);
            // Details…: the stamp's own box, with everything known of it (decision 224).
            card.addView(a.tapRow("Details…",()->show(a,one.n())));
            if(one.note()&&one.kept())card.addView(a.tapRow("Open this version",()->version(a,one.n())));
            card.addView(verifyLine(a,one));
            card.addView(a.tapRow("Copy the details",()->{copy(a,Integritas.details(one));notice(a,Say.t("Copied"));}));
        }
    }

    static String when(String iso){return Integritas.when(iso);}
}
