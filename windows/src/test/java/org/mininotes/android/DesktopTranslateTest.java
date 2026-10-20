package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/**
 * Translate… on the PC (docs/TRANSLATION.md): packs fetched from a release folder and checked against its list, kept and
 * taken off, the engine run through English, the box, and Add as a tab. Synthetic: made-up packs, and an engine that gives
 * back what it is given (a command file), so nothing is downloaded; the real engine is tried by hand (docs).
 */
public class DesktopTranslateTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    private Path release() throws Exception {
        Path folder=temp.newFolder("release").toPath();
        StringBuilder list=new StringBuilder("# made up\n");
        for(String pack:List.of("fren","enfr"))for(String name:List.of("model."+pack+".bin","lex."+pack+".bin","vocab."+pack+".spm")) {
            byte[] bytes=(pack+name).getBytes(StandardCharsets.UTF_8);Files.write(folder.resolve(name),bytes);
            list.append(pack).append('\t').append(name).append('\t').append(bytes.length).append('\t').append(Update.hex(MessageDigest.getInstance("SHA-256").digest(bytes))).append('\n');
        }
        Files.writeString(folder.resolve(DesktopTranslate.LIST),list);
        return folder;
    }
    private Path echoEngine() throws Exception {
        Path engine=temp.newFile("echo.cmd").toPath();
        Files.writeString(engine,"@more\r\n");
        return engine;
    }

    @After public void forget(){System.clearProperty("mininotes.packs");System.clearProperty("mininotes.engine");}

    @Test public void packsComeCheckedAndGoAgain() throws Exception {
        Path release=release();System.setProperty("mininotes.packs",release.toString());
        Desktop pad=open(temp.newFolder("pad").toPath());
        try {
            List<Translation.File> all=DesktopTranslate.list();
            assertEquals(6,all.size());
            List<Translation.Pair> both=Translation.both("fr");
            assertFalse(DesktopTranslate.have(pad,both.get(0)));
            assertEquals(all.stream().mapToLong(Translation.File::size).sum(),DesktopTranslate.missing(pad,all,both));
            assertEquals("Spanish is not on the list",-1,DesktopTranslate.missing(pad,all,Translation.both("es")));
            // A file that does not match its line is not kept, and the pack is not whole.
            Files.writeString(release.resolve("lex.fren.bin"),"changed");
            try{DesktopTranslate.fetch(pad,all,both,p->{},new AtomicReference<>(false));fail("a changed file was taken");}
            catch(java.io.IOException refused){assertTrue(refused.getMessage(),refused.getMessage().contains("lex.fren.bin"));}
            assertFalse(DesktopTranslate.have(pad,new Translation.Pair("fr","en")));
            Files.writeString(release.resolve("lex.fren.bin"),"fren"+"lex.fren.bin");
            int[] last={-1};
            DesktopTranslate.fetch(pad,all,both,p->last[0]=p,new AtomicReference<>(false));
            assertEquals(100,last[0]);
            assertTrue(DesktopTranslate.have(pad,both.get(0))&&DesktopTranslate.have(pad,both.get(1)));
            assertEquals(0,DesktopTranslate.missing(pad,all,both));
            assertTrue(DesktopTranslate.installed(pad).containsKey("fr"));
            DesktopTranslate.remove(pad,"fr");
            assertTrue(DesktopTranslate.installed(pad).isEmpty());
        } finally{close(pad);}
    }

    @Test public void theBoxTranslatesTheTabAndAddsItAsATab() throws Exception {
        Path release=release();System.setProperty("mininotes.packs",release.toString());System.setProperty("mininotes.engine",echoEngine().toString());
        Desktop pad=open(temp.newFolder("pad").toPath());
        try {
            DesktopTranslate.fetch(pad,DesktopTranslate.list(),Translation.both("fr"),p->{},new AtomicReference<>(false));
            // Through the engine twice where neither side is English? Here French to English: once, lines kept.
            String note="Courses\n\n- Du pain et du fromage";
            assertEquals(note,DesktopTranslate.translate(pad,note,"fr","en",new AtomicReference<>()));
            SwingUtilities.invokeAndWait(()->pad.page.setText(note));SwingUtilities.invokeLater(()->DesktopTranslate.note(pad));
            JDialog box=null;JTextArea result=null;
            for(long until=System.currentTimeMillis()+20000;System.currentTimeMillis()<until;Thread.sleep(200)) {
                for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&"Translate".equals(d.getTitle()))box=d;
                if(box!=null){result=area(box.getContentPane());if(result!=null&&!result.getText().isBlank())break;}
            }
            assertNotNull("the box",box);assertNotNull(result);
            assertEquals(note,result.getText().replace("\r",""));
            Files.createDirectories(SHOTS);
            JDialog shown=box;
            SwingUtilities.invokeAndWait(()->{
                var image=new java.awt.image.BufferedImage(shown.getWidth(),shown.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();shown.paint(g);g.dispose();
                try{javax.imageio.ImageIO.write(image,"png",SHOTS.resolve("161-translate-box.png").toFile());}catch(Exception e){throw new RuntimeException(e);}
            });
            JButton add=button(box.getContentPane(),Say.t("Add as a tab"));
            assertNotNull(add);assertTrue(add.isEnabled());
            SwingUtilities.invokeAndWait(add::doClick);
            String body=pad.noteNow();
            assertTrue(body,Sections.tabbed(body));
            assertEquals(2,Sections.of(body).size());
            assertEquals("English",Sections.of(body).get(1).called(1));
        } finally{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d)d.dispose();});close(pad);}
    }

    /**
     * The real engine with real packs, French to Chinese through English: only where MININOTES_REAL_PACKS names a folder laid
     * out as our release (tools/translation_packs.py release) and the engine has been built (tools/build-translation-engine.ps1).
     */
    @Test public void theRealEngineThroughEnglish() throws Exception {
        String packs=System.getenv("MININOTES_REAL_PACKS");
        Assume.assumeTrue(packs!=null&&Files.isDirectory(Path.of(packs))&&DesktopTranslate.engine()!=null);
        System.setProperty("mininotes.packs",packs);
        Desktop pad=open(temp.newFolder("pad").toPath());
        try {
            List<Translation.File> all=DesktopTranslate.list();
            List<Translation.Pair> steps=Translation.steps("fr","zh");
            DesktopTranslate.fetch(pad,all,steps,p->{},new AtomicReference<>(false));
            long started=System.nanoTime();
            String out=DesktopTranslate.translate(pad,"Courses\n\n- Du pain et du fromage\n- Appeler Marie demain","fr","zh",new AtomicReference<>());
            System.out.println("fr->zh "+(System.nanoTime()-started)/1_000_000+" ms:\n"+out);
            started=System.nanoTime();DesktopTranslate.translate(pad,"Appeler Marie demain","fr","zh",new AtomicReference<>());
            System.out.println("again "+(System.nanoTime()-started)/1_000_000+" ms");
            started=System.nanoTime();DesktopTranslate.translate(pad,"Appeler Marie demain","fr","en",new AtomicReference<>());
            System.out.println("fr->en "+(System.nanoTime()-started)/1_000_000+" ms");
            String[] lines=out.split("\n",-1);
            assertEquals(4,lines.length);assertEquals("",lines[1]);assertTrue(lines[2].startsWith("- ")&&lines[3].startsWith("- "));
            assertTrue(out,out.codePoints().anyMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN));
        } finally{close(pad);}
    }

    private static JTextArea area(Container in){for(Component c:in.getComponents()){if(c instanceof JTextArea a&&"Translation".equals(a.getAccessibleContext().getAccessibleName()))return a;if(c instanceof Container k){JTextArea f=area(k);if(f!=null)return f;}}return null;}
    private static JButton button(Container in,String text){for(Component c:in.getComponents()){if(c instanceof JButton b&&text.equals(b.getText()))return b;if(c instanceof Container k){JButton f=button(k,text);if(f!=null)return f;}}return null;}

    private static Desktop open(Path folder) throws Exception {
        Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        for(long until=System.currentTimeMillis()+20000;!(pad.page.isEditable()&&pad.store.latest()!=null)&&System.currentTimeMillis()<until;)Thread.sleep(100);
        pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
        return pad;
    }
    private static void close(Desktop pad) throws Exception {
        SwingUtilities.invokeAndWait(()->pad.shutdown(false));
        for(long until=System.currentTimeMillis()+20000;pad.frame.isDisplayable()&&System.currentTimeMillis()<until;)Thread.sleep(100);
    }
}
