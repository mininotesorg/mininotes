package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * README as the PC draws it, in English and in French (the owner, 2026-10-22, a picture of the French README with lines
 * drawn over each other; decision 234): drawn from the window into build/verification/gallery, in a made-up notebook.
 */
public class ReadMeLookTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private Desktop pad;

    @Before public void start() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void readMeDrawnInEnglishAndFrench() throws Exception {
        for(String code:new String[]{"en","fr"}) {
            NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="README "+code;n.body=ReadMe.text(code);n.revision=1;pad.store.save(n);
            SwingUtilities.invokeAndWait(()->{pad.frame.setSize(1400,900);pad.open(n.id);});
            await(()->n.id.equals(pad.noteShown())&&pad.page.getText().length()>1000);Thread.sleep(800);
            String find="en".equals(code)?"Stamp on Minima with Integritas":"Integritas";
            int at=pad.page.getText().indexOf("**",pad.page.getText().indexOf(find));
            SwingUtilities.invokeAndWait(()->{pad.page.setCaretPosition(Math.max(0,at));try{Rectangle r=pad.page.modelToView2D(Math.max(0,at)).getBounds();r.height=pad.page.getVisibleRect().height;pad.page.scrollRectToVisible(r);}catch(Exception none){}});
            Thread.sleep(800);
            shoot(pad.frame,"readme-"+code);
        }
    }

    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
