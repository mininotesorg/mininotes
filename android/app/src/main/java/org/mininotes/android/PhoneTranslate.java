// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Translate…, on the phone (docs/TRANSLATION.md, phase 3): what the PC does (DesktopTranslate), with the same engine built for
 * the phone. Android lets an app run a program only from its own libraries, so the engine travels as
 * lib/arm64-v8a/libbergamot.so and is started from there for each translation, then closed. The packs are the PC's, downloaded
 * the first time two languages are wanted and each file checked against our list; the box says which languages are ready.
 */
final class PhoneTranslate {
    private PhoneTranslate(){}

    static final String RELEASE="https://github.com/mininotesorg/mininotes/releases/download/translation-packs-1/";
    static final String LIST="translation-packs.txt";

    static File packs(Context c){return new File(c.getFilesDir(),"translation");}

    /** The engine the app carries; null on a phone it was not built for. */
    static File engine(Context c) {
        File made=new File(c.getApplicationInfo().nativeLibraryDir,"libbergamot.so");
        return made.isFile()?made:null;
    }

    static boolean have(Context c,Translation.Pair pair){return config(new File(packs(c),pair.folder()))!=null;}
    static boolean ready(Context c,String language){
        if(language.equals("en"))return true;
        for(Translation.Pair p:Translation.both(language))if(!have(c,p))return false;
        return true;
    }

    private static String config(File folder) {
        String[] names=folder.list();
        if(names==null)return null;
        List<String> files=new ArrayList<>();
        for(String n:names)if(!n.endsWith(".part")&&!n.equals("config.yml"))files.add(n);
        java.util.Collections.sort(files);
        return Translation.config(folder.getAbsolutePath(),files);
    }

    /** Our list of packs. Blocking. */
    static List<Translation.File> list() throws IOException {
        HttpURLConnection got=(HttpURLConnection)new URL(RELEASE+LIST).openConnection();
        got.setConnectTimeout(15000);got.setReadTimeout(30000);
        try {
            if(got.getResponseCode()!=200)throw new IOException("The translation packs are not on GitHub yet (HTTP "+got.getResponseCode()+").");
            try(InputStream in=got.getInputStream()){return Translation.manifest(new String(readAll(in),StandardCharsets.UTF_8));}
        } catch(java.net.UnknownHostException|java.net.SocketTimeoutException|java.net.ConnectException offline) {
            throw new IOException("GitHub could not be reached. Check the connection and try again.");
        } finally{got.disconnect();}
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream all=new java.io.ByteArrayOutputStream();byte[] b=new byte[1<<14];
        for(int n;(n=in.read(b))>0;)all.write(b,0,n);
        return all.toByteArray();
    }

    static long missing(Context c,List<Translation.File> all,List<Translation.Pair> pairs) {
        long size=0;
        for(Translation.Pair pair:pairs) {
            if(have(c,pair))continue;
            List<Translation.File> files=Translation.filesOf(all,pair);
            if(files.isEmpty())return -1;
            for(Translation.File f:files)size+=f.size();
        }
        return size;
    }

    /** The packs not here yet, each file checked against the list, then put in place. Blocking. */
    static void fetch(Context c,List<Translation.File> all,List<Translation.Pair> pairs,java.util.function.IntConsumer percent,AtomicReference<Boolean> stop) throws Exception {
        long total=Math.max(1,missing(c,all,pairs)),done=0;int shown=-1;
        for(Translation.Pair pair:pairs) {
            if(have(c,pair))continue;
            File folder=new File(packs(c),pair.folder());
            if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("There is no room for the packs on this phone.");
            for(Translation.File file:Translation.filesOf(all,pair)) {
                File into=new File(folder,file.name()),part=new File(folder,file.name()+".part");
                MessageDigest sha=MessageDigest.getInstance("SHA-256");
                HttpURLConnection got=(HttpURLConnection)new URL(RELEASE+file.name()).openConnection();
                got.setConnectTimeout(15000);got.setReadTimeout(60000);
                try {
                    if(got.getResponseCode()!=200)throw new IOException("A pack could not be downloaded (HTTP "+got.getResponseCode()+").");
                    try(InputStream in=got.getInputStream();OutputStream out=new FileOutputStream(part)) {
                        byte[] buffer=new byte[1<<16];int n;long mine=0;
                        while((n=in.read(buffer))>0) {
                            if(Boolean.TRUE.equals(stop.get()))throw new IOException("Stopped.");
                            mine+=n;if(mine>file.size())throw new IOException(file.name()+" is larger than the list says, so it was not kept.");
                            sha.update(buffer,0,n);out.write(buffer,0,n);
                            int now=(int)((done+mine)*100/total);if(now!=shown){shown=now;percent.accept(now);}
                        }
                        done+=mine;
                    }
                } catch(IOException e){part.delete();throw e;}
                finally{got.disconnect();}
                if(!Update.hex(sha.digest()).equals(file.sha256())){part.delete();throw new IOException(file.name()+" does not match its checksum, so it was not kept.");}
                if(!part.renameTo(into)){part.delete();throw new IOException("A pack could not be kept.");}
            }
        }
    }

    /** Words put into another language on the phone, through English where neither is, line for line. Blocking. */
    static String translate(Context c,String text,String from,String to,AtomicReference<Process> running) throws Exception {
        File engine=engine(c);
        if(engine==null)throw new IOException("This Mininotes has no translation engine. Install a newer build.");
        Translation.Prepared prepared=Translation.prepare(text);
        if(prepared.input().isBlank())return text;
        String words=prepared.input();
        for(Translation.Pair pair:Translation.steps(from,to)) {
            File folder=new File(packs(c),pair.folder());
            String settings=config(folder);
            if(settings==null)throw new IOException("A pack is missing. Download it again.");
            File file=new File(folder,"config.yml");
            try(OutputStream out=new FileOutputStream(file)){out.write(settings.getBytes(StandardCharsets.UTF_8));}
            words=run(engine,file,words,running);
        }
        return Translation.restore(prepared,words);
    }

    private static String run(File engine,File config,String input,AtomicReference<Process> running) throws Exception {
        int threads=Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-2));
        Process p=new ProcessBuilder(engine.getAbsolutePath(),"--model-config-paths",config.getAbsolutePath(),"--cpu-threads",Integer.toString(threads))
            .redirectErrorStream(false).start();
        running.set(p);
        try {
            Thread feed=new Thread(()->{try(OutputStream in=p.getOutputStream()){in.write(input.getBytes(StandardCharsets.UTF_8));}catch(IOException closed){/* it stopped */}},"mininotes-translate-in");
            feed.setDaemon(true);feed.start();
            Thread drain=new Thread(()->{try(InputStream err=p.getErrorStream()){readAll(err);}catch(IOException closed){/* it stopped */}},"mininotes-translate-err");
            drain.setDaemon(true);drain.start();
            byte[] out;try(InputStream in=p.getInputStream()){out=readAll(in);}
            if(!p.waitFor(3,TimeUnit.MINUTES)){p.destroyForcibly();throw new IOException("The translation took too long and was stopped.");}
            if(p.exitValue()!=0)throw new IOException(running.get()==null?"Stopped.":"The translation engine stopped with an error ("+p.exitValue()+").");
            return new String(out,StandardCharsets.UTF_8);
        } finally{running.compareAndSet(p,null);}
    }

    // ---- the box ----------------------------------------------------------------------------------------------------

    /** Translate…, on what is chosen; null for the whole tab. */
    static void open(final MainActivity a,final String text,final int[] chosen) {
        if(text==null||text.trim().isEmpty()){a.toast("Nothing to translate here.");return;}
        final Background work=new Background(a::runOnUiThread);
        final List<String> codes=new ArrayList<>(Translation.PACKS.keySet());
        final AtomicReference<List<Translation.File>> listed=new AtomicReference<>();
        // Each language with what it is now: ✓ ready, ↓ and the size to download, or not available yet.
        final List<String> labels=new ArrayList<>();
        final Runnable relabel=()->{
            labels.clear();
            for(String code:codes) {
                String name=Translation.name(code),mark;
                if(ready(a,code))mark="✓";
                else {
                    List<Translation.File> all=listed.get();
                    long size=all==null?-1:missing(a,all,Translation.both(code));
                    mark=size>0?"↓ "+megabytes(size):Say.t("not available yet");
                }
                labels.add(name+"   "+mark);
            }
        };
        relabel.run();
        final ArrayAdapter<String> fromItems=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,labels),toItems=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,labels);
        fromItems.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);toItems.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner fromBox=new Spinner(a),toBox=new Spinner(a);
        fromBox.setAdapter(fromItems);toBox.setAdapter(toItems);
        String guessed=Translation.guess(text),mine=Translation.PACKS.containsKey(Say.language())?Say.language():"en";
        String into=mine.equals(guessed)?(guessed.equals("en")?"fr":"en"):mine;
        fromBox.setSelection(codes.indexOf(guessed));toBox.setSelection(codes.indexOf(into));

        LinearLayout body=a.column();body.setPadding(a.dp(20),a.dp(8),a.dp(20),0);
        body.addView(fromBox,new LinearLayout.LayoutParams(-1,-2));
        TextView arrow=a.label("↓",MainActivity.READING,a.MUTED);arrow.setGravity(Gravity.CENTER);body.addView(arrow,new LinearLayout.LayoutParams(-1,-2));
        body.addView(toBox,new LinearLayout.LayoutParams(-1,-2));
        final TextView result=a.label("",MainActivity.READING,a.INK);result.setTextIsSelectable(true);result.setPadding(0,a.dp(12),0,a.dp(8));
        body.addView(result,new LinearLayout.LayoutParams(-1,-2));
        final TextView said=a.label(" ",MainActivity.QUIET,a.MUTED);body.addView(said,new LinearLayout.LayoutParams(-1,-2));
        final Button fetchPacks=new Button(a);fetchPacks.setText(Say.t("Download"));fetchPacks.setVisibility(View.GONE);
        final Button stop=new Button(a);stop.setText(Say.t("Stop"));stop.setVisibility(View.GONE);
        LinearLayout buttons=new LinearLayout(a);buttons.setGravity(Gravity.END);buttons.addView(stop);buttons.addView(fetchPacks);
        body.addView(buttons,new LinearLayout.LayoutParams(-1,-2));

        final boolean writes=a.noteWritable();
        MainActivity.Box ask=a.new Box();ask.setTitle("Translate").setView(a.scrolling(body));
        // Copy, Replace (what was chosen, or the whole tab), and Add as a tab for the whole tab (the owner, 2026-10-13).
        ask.setNegativeButton("Copy",(d,w)->{
            ClipboardManager clip=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
            if(clip!=null){clip.setPrimaryClip(ClipData.newPlainText("Translation",result.getText()));a.toast("Copied.");}
        });
        if(writes)ask.setPositiveButton("Replace",(d,w)->{
            if(chosen!=null&&!a.replaceChosen(chosen[0],chosen[1],text,result.getText().toString()))a.toast("The note changed meanwhile, so nothing was replaced.");
            else if(chosen==null)a.replaceTab(result.getText().toString());
        });
        if(writes&&chosen==null)ask.setNeutralButton("Add as a tab",(d,w)->a.addTranslatedTab(Translation.name(codes.get(toBox.getSelectedItemPosition())),result.getText().toString()));
        final android.app.AlertDialog box=ask.show();
        final Runnable enable=()->{
            boolean done=result.getText().length()>0;
            for(int which:new int[]{android.app.AlertDialog.BUTTON_POSITIVE,android.app.AlertDialog.BUTTON_NEGATIVE,android.app.AlertDialog.BUTTON_NEUTRAL}) {
                Button b=box.getButton(which);if(b!=null)b.setEnabled(done);
            }
        };

        final AtomicReference<Process> running=new AtomicReference<>();
        final AtomicReference<Boolean> stopped=new AtomicReference<>(false);
        final int[] asked={0};
        final Runnable[] go={null};
        go[0]=()->{
            final int me=++asked[0];
            final String from=codes.get(fromBox.getSelectedItemPosition()),to=codes.get(toBox.getSelectedItemPosition());
            result.setText("");enable.run();fetchPacks.setVisibility(View.GONE);
            if(from.equals(to)){said.setText(Say.t("Choose two different languages."));return;}
            final List<Translation.Pair> pairs=Translation.steps(from,to);
            boolean here=true;for(Translation.Pair p:pairs)if(!have(a,p))here=false;
            if(!here) {
                said.setText(Say.t("Looking for the language packs…"));
                work.submit(PhoneTranslate::list,all->{
                    listed.set(all);relabel.run();fromItems.notifyDataSetChanged();toItems.notifyDataSetChanged();
                    if(me!=asked[0])return;
                    long size=missing(a,all,pairs);
                    if(size<0){said.setText(unavailable(a,from,to));return;}
                    said.setText(Say.t("The packs for these languages are not on this phone yet: {0} to download, once.",megabytes(size)));
                    fetchPacks.setVisibility(View.VISIBLE);
                    fetchPacks.setOnClickListener(v->{
                        fetchPacks.setVisibility(View.GONE);stop.setVisibility(View.VISIBLE);stopped.set(false);
                        said.setText(Say.t("Downloading the language packs…"));
                        work.submit(()->{fetch(a,all,pairs,p->a.runOnUiThread(()->said.setText(Say.t("Downloading the language packs… {0}%",Integer.toString(p)))),stopped);return null;},
                            done->{stop.setVisibility(View.GONE);relabel.run();fromItems.notifyDataSetChanged();toItems.notifyDataSetChanged();if(box.isShowing())go[0].run();},
                            e->{stop.setVisibility(View.GONE);said.setText(e.getMessage()==null?Say.t("The packs could not be downloaded."):Say.t(e.getMessage()));fetchPacks.setVisibility(View.VISIBLE);});
                    });
                },e->{if(me==asked[0])said.setText(unavailable(a,from,to));});
                return;
            }
            said.setText(Say.t("Translating…"));stop.setVisibility(View.VISIBLE);
            work.submit(()->translate(a,text,from,to,running),words->{
                if(me!=asked[0]||!box.isShowing())return;
                stop.setVisibility(View.GONE);result.setText(words);enable.run();
                said.setText(Say.t("Translated on this phone. Nothing was sent anywhere."));
            },e->{if(me!=asked[0])return;stop.setVisibility(View.GONE);said.setText(e.getMessage()==null?Say.t("It could not be translated."):Say.t(e.getMessage()));});
        };
        stop.setOnClickListener(v->{stopped.set(true);Process p=running.getAndSet(null);if(p!=null)p.destroy();said.setText(Say.t("Stopped."));stop.setVisibility(View.GONE);});
        AdapterView.OnItemSelectedListener changed=new AdapterView.OnItemSelectedListener(){
            int seen=0;
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(seen++>=2)go[0].run();}
            @Override public void onNothingSelected(AdapterView<?> parent){}
        };
        fromBox.setOnItemSelectedListener(changed);toBox.setOnItemSelectedListener(changed);
        box.setOnDismissListener(d->{stopped.set(true);Process p=running.getAndSet(null);if(p!=null)p.destroy();});
        // Read the list in the background so each line says what it is, then translate.
        work.submit(PhoneTranslate::list,all->{listed.set(all);relabel.run();fromItems.notifyDataSetChanged();toItems.notifyDataSetChanged();},e->{});
        go[0].run();
    }

    private static String unavailable(Context c,String from,String to) {
        List<String> missing=new ArrayList<>();
        for(String code:Arrays.asList(from,to))if(!ready(c,code))missing.add(Translation.name(code));
        return Say.t("{0} is not available yet. The languages marked ✓ are ready on this phone.",String.join(", ",missing));
    }

    static String megabytes(long bytes){return Math.max(1,Math.round(bytes/1_000_000.0))+" MB";}
}
