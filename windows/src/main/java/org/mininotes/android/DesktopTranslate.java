// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import javax.swing.*;

/**
 * Translate…, on the PC (docs/TRANSLATION.md; the owner, 2026-10-13: Bergamot, the packs from our GitHub, the result in a
 * box). What is chosen in a note, or the tab in front, put into another language on this PC and nowhere else: by the engine
 * Firefox translates with (bergamot.exe, beside the app, started for each translation and closed after, so its memory is
 * given back), with the packs of the two languages, downloaded the first time they are wanted and checked against our list.
 * The box says what it is translating from and into, both changed there; then Copy, and Replace for what was chosen or Add
 * as a tab for the whole tab.
 */
final class DesktopTranslate {
    private DesktopTranslate(){}

    /** Where our packs are published: a release of their own, with the list of their files beside them. */
    static final String RELEASE=DesktopUpdate.SOURCE+"/releases/download/translation-packs-1/";
    static final String LIST="translation-packs.txt";
    private static final HttpClient HTTP=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();

    /** Where the packs come from: our release, or for a test a folder or address given at the start. */
    static String source(){String given=System.getProperty("mininotes.packs","");return given.isBlank()?RELEASE:given.endsWith("/")||given.endsWith("\\")?given:given+"/";}

    /** The packs on this PC, beside the notebook. */
    static Path packs(Desktop app){return app.context.getFilesDir().toPath().resolve("translation");}

    /** The engine: beside the app's own files, or where a test or a build of it says. Null where there is none. */
    static Path engine() {
        String given=System.getProperty("mininotes.engine","");
        if(!given.isBlank()&&Files.isRegularFile(Path.of(given)))return Path.of(given);
        try {
            Path code=Path.of(DesktopTranslate.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path beside=(Files.isDirectory(code)?code:code.getParent()).resolve("translate").resolve("bergamot.exe");
            if(Files.isRegularFile(beside))return beside;
            // From the sources: native/build-pc, where tools/build-translation-engine.ps1 puts it.
            for(Path up=code;up!=null;up=up.getParent()) {
                Path built=up.resolve("native").resolve("build-pc").resolve("app").resolve("bergamot.exe");
                if(Files.isRegularFile(built))return built;
            }
        } catch(Exception none){/* no engine */}
        return null;
    }

    // ---- packs ----------------------------------------------------------------------------------------------------

    /** Whether a pack is here whole: the engine's settings can be written for it. */
    static boolean have(Desktop app,Translation.Pair pair){return config(packs(app).resolve(pair.folder()))!=null;}

    private static String config(Path folder) {
        try(var names=Files.list(folder)) {
            return Translation.config(folder.toString(),names.map(p->p.getFileName().toString()).filter(n->!n.endsWith(".part")&&!n.equals("config.yml")).sorted().toList());
        } catch(IOException none){return null;}
    }

    /** Our list of packs, read from the release (or the test's folder). Blocking. */
    static List<Translation.File> list() throws IOException{return Translation.manifest(new String(read(source()+LIST),StandardCharsets.UTF_8));}

    private static byte[] read(String where) throws IOException {
        if(!where.startsWith("http"))return Files.readAllBytes(Path.of(where));
        try {
            HttpResponse<byte[]> got=HTTP.send(HttpRequest.newBuilder(URI.create(where)).timeout(Duration.ofSeconds(30)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            if(got.statusCode()!=200)throw new IOException("The translation packs are not on GitHub yet (HTTP "+got.statusCode()+").");
            return got.body();
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Stopped.");}
        catch(IOException offline){throw offline.getMessage()!=null&&offline.getMessage().startsWith("The translation")?offline:new IOException("GitHub could not be reached. Check the connection and try again.");}
    }

    /** How much the packs not here yet weigh, from the list; -1 where a pack is not on it. */
    static long missing(Desktop app,List<Translation.File> all,List<Translation.Pair> pairs) {
        long size=0;
        for(Translation.Pair pair:pairs) {
            if(have(app,pair))continue;
            List<Translation.File> files=Translation.filesOf(all,pair);
            if(files.isEmpty())return -1;
            for(Translation.File f:files)size+=f.size();
        }
        return size;
    }

    /** The packs not here yet, downloaded and each file checked against the list, then put in place. Blocking. */
    static void fetch(Desktop app,List<Translation.File> all,List<Translation.Pair> pairs,IntConsumer percent,AtomicReference<Boolean> stop) throws Exception {
        long total=Math.max(1,missing(app,all,pairs)),done=0;int shown=-1;
        for(Translation.Pair pair:pairs) {
            if(have(app,pair))continue;
            Path folder=packs(app).resolve(pair.folder());Files.createDirectories(folder);
            for(Translation.File file:Translation.filesOf(all,pair)) {
                Path into=folder.resolve(file.name()),part=folder.resolve(file.name()+".part");
                MessageDigest sha=MessageDigest.getInstance("SHA-256");
                try(InputStream in=open(source()+file.name());OutputStream out=Files.newOutputStream(part)) {
                    byte[] buffer=new byte[1<<16];int got;long mine=0;
                    while((got=in.read(buffer))>0) {
                        if(Boolean.TRUE.equals(stop.get()))throw new InterruptedIOException("Stopped.");
                        mine+=got;if(mine>file.size())throw new IOException(file.name()+" is larger than the list says, so it was not kept.");
                        sha.update(buffer,0,got);out.write(buffer,0,got);
                        int now=(int)((done+mine)*100/total);if(now!=shown){shown=now;percent.accept(now);}
                    }
                    done+=mine;
                } catch(IOException e){Files.deleteIfExists(part);throw e;}
                if(!Update.hex(sha.digest()).equals(file.sha256())){Files.deleteIfExists(part);throw new IOException(file.name()+" does not match its checksum, so it was not kept.");}
                Files.move(part,into,StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static InputStream open(String where) throws IOException {
        if(!where.startsWith("http"))return Files.newInputStream(Path.of(where));
        try {
            HttpResponse<InputStream> got=HTTP.send(HttpRequest.newBuilder(URI.create(where)).timeout(Duration.ofMinutes(10)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
            if(got.statusCode()!=200){got.body().close();throw new IOException("A pack could not be downloaded (HTTP "+got.statusCode()+").");}
            return got.body();
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Stopped.");}
    }

    /** Every pack here, by language, with what they weigh together. */
    static Map<String,Long> installed(Desktop app) {
        Map<String,Long> out=new TreeMap<>();
        for(Map.Entry<String,String> one:Translation.PACKS.entrySet()) {
            if(one.getKey().equals("en"))continue;
            long size=0;boolean any=false;
            for(Translation.Pair pair:Translation.both(one.getKey())) {
                Path folder=packs(app).resolve(pair.folder());
                if(!Files.isDirectory(folder))continue;
                try(var files=Files.list(folder)){for(Path f:files.toList()){size+=Files.size(f);any=true;}}catch(IOException skipped){/* counted as it is */}
            }
            if(any)out.put(one.getKey(),size);
        }
        return out;
    }

    /** A language's packs taken off this PC. */
    static void remove(Desktop app,String language) throws IOException {
        for(Translation.Pair pair:Translation.both(language)) {
            Path folder=packs(app).resolve(pair.folder());
            if(!Files.isDirectory(folder))continue;
            try(var files=Files.list(folder)){for(Path f:files.toList())Files.deleteIfExists(f);}
            Files.deleteIfExists(folder);
        }
    }

    // ---- the engine -------------------------------------------------------------------------------------------------

    /**
     * Words put into another language, through English where neither is, line for line (Translation.prepare). Blocking;
     * {@code running} holds the engine while it works, so Stop can end it.
     */
    static String translate(Desktop app,String text,String from,String to,AtomicReference<Process> running) throws Exception {
        Path engine=engine();
        if(engine==null)throw new IOException("This Mininotes has no translation engine. Install a newer build.");
        Translation.Prepared prepared=Translation.prepare(text);
        if(prepared.input().isBlank())return text;
        String words=prepared.input();
        for(Translation.Pair pair:Translation.steps(from,to)) {
            Path folder=packs(app).resolve(pair.folder());
            String settings=config(folder);
            if(settings==null)throw new IOException("A pack is missing. Download it again.");
            Path file=folder.resolve("config.yml");Files.writeString(file,settings,StandardCharsets.UTF_8);
            words=run(engine,file,words,running);
        }
        return Translation.restore(prepared,words);
    }

    private static String run(Path engine,Path config,String input,AtomicReference<Process> running) throws Exception {
        int threads=Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-1));
        Process engineRun=new ProcessBuilder(engine.toString(),"--model-config-paths",config.toString(),"--cpu-threads",Integer.toString(threads))
            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        running.set(engineRun);
        try {
            Thread feed=new Thread(()->{try(OutputStream in=engineRun.getOutputStream()){in.write(input.getBytes(StandardCharsets.UTF_8));}catch(IOException closed){/* it stopped */}},"mininotes-translate-in");
            feed.setDaemon(true);feed.start();
            byte[] out=engineRun.getInputStream().readAllBytes();
            if(!engineRun.waitFor(2,java.util.concurrent.TimeUnit.MINUTES)){engineRun.destroyForcibly();throw new IOException("The translation took too long and was stopped.");}
            if(engineRun.exitValue()!=0)throw new IOException(running.get()==null?"Stopped.":"The translation engine stopped with an error ("+engineRun.exitValue()+").");
            return new String(out,StandardCharsets.UTF_8);
        } finally {running.compareAndSet(engineRun,null);}
    }

    // ---- the box ----------------------------------------------------------------------------------------------------

    /** Translate…, on what is chosen on the page; the whole tab where nothing is. */
    static void selection(Desktop app) {
        int from=Math.min(app.page.getSelectionStart(),app.page.getSelectionEnd()),to=Math.max(app.page.getSelectionStart(),app.page.getSelectionEnd());
        if(to<=from){note(app);return;}
        String chosen=app.page.getText().substring(from,to);
        open(app,chosen,new int[]{from,to});
    }

    /** Translate…, on the tab in front. */
    static void note(Desktop app){open(app,app.page.getText(),null);}

    private static void open(Desktop app,String text,int[] chosen) {
        if(text==null||text.isBlank()){app.status.setText(Say.t("Nothing to translate here."));return;}
        List<String> codes=new ArrayList<>(Translation.PACKS.keySet());
        String[] names=codes.stream().map(c->Translation.name(c)).toArray(String[]::new);
        JComboBox<String> fromBox=new JComboBox<>(names),toBox=new JComboBox<>(names);
        // Which languages can be used, said on each line (the owner, 2026-10-13: "make clear what is available and what
        // languages are not"): ✓ here; ↓ and the size where its packs can be downloaded; dimmed, "not available yet", where
        // there is nothing to download. The list of packs is read once the box is open; until then, here or not.
        AtomicReference<List<Translation.File>> listed=new AtomicReference<>();
        java.util.function.Function<String,String> state=code->{
            if(code.equals("en")||Translation.both(code).stream().allMatch(p->have(app,p)))return "✓";
            List<Translation.File> all=listed.get();
            long size=all==null?-1:missing(app,all,Translation.both(code));
            return size>0?"↓ "+megabytes(size):Say.t("not available yet");
        };
        for(JComboBox<String> box:List.of(fromBox,toBox))box.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused) {
                int at=index>=0?index:Arrays.asList(names).indexOf(String.valueOf(value));
                String code=at>=0?codes.get(at):"en",mark=state.apply(code);
                JPanel line=new JPanel(new BorderLayout(16,0));line.setOpaque(true);
                JLabel name=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);
                name.setFont(DesktopUi.fontFor(String.valueOf(value),list.getFont()));
                JLabel side=new JLabel(mark);side.setFont(list.getFont().deriveFont(12f));
                boolean ready=mark.equals("✓");
                side.setForeground(ready?DesktopUi.ACCENT:DesktopUi.QUIET);
                if(!ready&&!mark.startsWith("↓")&&!selected)name.setForeground(DesktopUi.QUIET);
                line.setBackground(name.getBackground());side.setBorder(BorderFactory.createEmptyBorder(0,0,0,6));
                line.add(name);line.add(side,BorderLayout.EAST);
                line.getAccessibleContext().setAccessibleName(value+", "+(ready?Say.t("on this PC"):mark));
                return line;
            }
        });
        app.network.submit(DesktopTranslate::list,all->{listed.set(all);fromBox.repaint();toBox.repaint();},none->{});
        String guessed=Translation.guess(text),mine=Translation.PACKS.containsKey(Say.language())?Say.language():"en";
        String into=mine.equals(guessed)?(guessed.equals("en")?"fr":"en"):mine;
        fromBox.setSelectedIndex(codes.indexOf(guessed));toBox.setSelectedIndex(codes.indexOf(into));
        JPanel languages=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));languages.setOpaque(false);
        JLabel arrow=new JLabel("→");arrow.setFont(DesktopUi.BODY.deriveFont(16f));
        languages.add(fromBox);languages.add(arrow);languages.add(toBox);

        JTextArea result=new JTextArea(10,44);result.setEditable(false);result.setLineWrap(true);result.setWrapStyleWord(true);
        result.setFont(DesktopUi.BODY.deriveFont(15f));result.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));result.setBackground(DesktopUi.CARD);
        result.getAccessibleContext().setAccessibleName("Translation");
        JScrollPane scroll=new JScrollPane(result);scroll.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));
        DesktopUi.Text said=DesktopUi.quiet(" ");
        JButton fetchPacks=DesktopUi.primary("Download",()->{});fetchPacks.setVisible(false);
        JButton stop=DesktopUi.button("Stop",()->{});stop.setVisible(false);

        JPanel body=DesktopUi.column();
        DesktopUi.add(body,languages);DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,scroll);DesktopUi.gap(body,DesktopUi.S);
        JPanel under=new JPanel(new BorderLayout(8,0));under.setOpaque(false);under.add(said);
        JPanel side=DesktopUi.actions(stop,fetchPacks);under.add(side,BorderLayout.EAST);
        DesktopUi.add(body,under);

        boolean writes=app.page.isEditable();
        JButton copy=DesktopUi.button("Copy",()->{});copy.setEnabled(false);
        // Replace puts it in place of what was chosen, or of the whole tab; Add as a tab keeps the tab and adds the translation
        // beside it (the owner, 2026-10-13: "a function to replace the text in the note with the translated one").
        JButton put=DesktopUi.button("Replace",()->{}),tabbed=DesktopUi.button("Add as a tab",()->{});
        put.setEnabled(false);put.setVisible(writes);tabbed.setEnabled(false);tabbed.setVisible(writes&&chosen==null);
        // Modal, over a shade: a click beside it closes it, as every box does (the owner, 2026-10-13).
        JDialog box=DesktopUi.sheet(app.frame,"Translate",body,DesktopUi.footer(copy,tabbed,put),true);

        AtomicReference<Process> running=new AtomicReference<>();
        AtomicReference<Boolean> stopped=new AtomicReference<>(false);
        int[] asked={0};
        Runnable[] go={null};
        go[0]=()->{
            int ask=++asked[0];
            String from=codes.get(fromBox.getSelectedIndex()),to=codes.get(toBox.getSelectedIndex());
            result.setText("");copy.setEnabled(false);put.setEnabled(false);tabbed.setEnabled(false);fetchPacks.setVisible(false);
            if(from.equals(to)){said.setText(Say.t("Choose two different languages."));return;}
            List<Translation.Pair> pairs=Translation.steps(from,to);
            boolean here=pairs.stream().allMatch(p->have(app,p));
            if(!here) {
                said.setText(Say.t("Looking for the language packs…"));
                app.network.submit(DesktopTranslate::list,all->{
                    if(ask!=asked[0])return;
                    long size=missing(app,all,pairs);
                    if(size<0){said.setText(unavailable(app,from,to));return;}
                    said.setText(Say.t("The packs for these languages are not on this PC yet: {0} to download, once.",megabytes(size)));
                    fetchPacks.setVisible(true);box.getRootPane().setDefaultButton(fetchPacks);
                    for(var l:fetchPacks.getActionListeners())fetchPacks.removeActionListener(l);
                    fetchPacks.addActionListener(e->{
                        fetchPacks.setVisible(false);stop.setVisible(true);stopped.set(false);
                        said.setText(Say.t("Downloading the language packs…"));
                        app.network.submit(()->{fetch(app,all,pairs,p->SwingUtilities.invokeLater(()->said.setText(Say.t("Downloading the language packs… {0}%",Integer.toString(p)))),stopped);return null;},
                            done->{stop.setVisible(false);if(box.isDisplayable())go[0].run();},
                            error->{stop.setVisible(false);said.setText(error.getMessage()==null?Say.t("The packs could not be downloaded."):Say.t(error.getMessage()));fetchPacks.setVisible(true);});
                    });
                },error->{if(ask==asked[0])said.setText(unavailable(app,from,to));});
                return;
            }
            said.setText(Say.t("Translating…"));stop.setVisible(true);
            app.disk.submit(()->translate(app,text,from,to,running),words->{
                if(ask!=asked[0]||!box.isDisplayable())return;
                stop.setVisible(false);result.setText(words);result.setCaretPosition(0);
                said.setText(Say.t("Translated on this PC. Nothing was sent anywhere."));
                copy.setEnabled(true);put.setEnabled(writes);tabbed.setEnabled(writes);
            },error->{if(ask!=asked[0])return;stop.setVisible(false);said.setText(error.getMessage()==null?Say.t("It could not be translated."):Say.t(error.getMessage()));});
        };
        stop.addActionListener(e->{stopped.set(true);Process p=running.getAndSet(null);if(p!=null)p.destroyForcibly();said.setText(Say.t("Stopped."));stop.setVisible(false);});
        fromBox.addActionListener(e->go[0].run());toBox.addActionListener(e->go[0].run());
        copy.addActionListener(e->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(result.getText()),null);said.setText(Say.t("Copied."));});
        put.addActionListener(e->{
            String words=result.getText();
            if(chosen!=null) {
                // Only where the words chosen are still there: the note may have changed while the box was open.
                String now=app.page.getText();
                if(chosen[1]>now.length()||!now.substring(chosen[0],chosen[1]).equals(text)){said.setText(Say.t("The note changed meanwhile, so nothing was replaced."));return;}
                app.page.select(chosen[0],chosen[1]);app.page.replaceSelection(words);
            } else app.replaceTab(words);
            box.dispose();
        });
        tabbed.addActionListener(e->{
            app.addTranslatedTab(Translation.name(codes.get(toBox.getSelectedIndex())),result.getText());
            box.dispose();
        });
        box.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowClosed(java.awt.event.WindowEvent e){
            stopped.set(true);Process p=running.getAndSet(null);if(p!=null)p.destroyForcibly();
        }});
        go[0].run();
        DesktopUi.show(box,560,640);
    }

    /** Which of the two languages cannot be used yet, in words, and which can. */
    private static String unavailable(Desktop app,String from,String to) {
        List<String> missing=new ArrayList<>();
        for(String code:List.of(from,to))if(!code.equals("en")&&!Translation.both(code).stream().allMatch(p->have(app,p)))missing.add(Translation.name(code));
        return Say.t("{0} is not available yet. The languages marked ✓ are ready on this PC.",String.join(", ",missing));
    }

    static String megabytes(long bytes){return Math.max(1,Math.round(bytes/1_000_000.0))+" MB";}
}
