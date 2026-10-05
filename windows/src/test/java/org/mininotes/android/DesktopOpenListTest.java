package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * Recent, down the right of the window (docs/HOME.md, decisions 77 and 86): each note and collection opened lately has a
 * line, newest first, reordered as they are opened, the one in front marked; switched off, the list goes. A synthetic
 * notebook; a picture beside the gallery's.
 */
public class DesktopOpenListTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void recentIsListedNewestFirst() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            NoteStore store=pad.store;
            NoteStore.Shelf kitchen=store.addCollection("Kitchen");
            String shopping=note(store,Things.HOME,"Shopping"),ideas=note(store,Things.HOME,"Ideas");
            SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle(pad);
            SwingUtilities.invokeAndWait(()->pad.open(shopping));settle(pad);
            SwingUtilities.invokeAndWait(()->pad.open(ideas));settle(pad);
            SwingUtilities.invokeAndWait(()->pad.openCollection(kitchen.id));settle(pad);
            SwingUtilities.invokeAndWait(()->pad.open(ideas));settle(pad);
            SwingUtilities.invokeAndWait(()->{
                assertTrue("shown",pad.openList.isVisible());
                assertEquals(new Overview.Open(Overview.Kind.NOTE,ideas),pad.inFront());
            });
            String said=onEdt(()->words(pad.openList));
            assertTrue(said,said.startsWith("Recent | Ideas")&&said.indexOf("Ideas")<said.indexOf("Kitchen")&&said.indexOf("Kitchen")<said.indexOf("Shopping"));
            shoot(pad.frame,"86-recent-list");
            // Opened again: to the top.
            SwingUtilities.invokeAndWait(()->pad.open(shopping));settle(pad);
            assertTrue(onEdt(()->words(pad.openList)).startsWith("Recent | Shopping"));
            // Switched off: the list goes, and comes back on.
            SwingUtilities.invokeAndWait(()->pad.setOpenListWanted(false));settle(pad);
            assertFalse(onEdt(()->pad.openList.isVisible()));
            SwingUtilities.invokeAndWait(()->pad.setOpenListWanted(true));settle(pad);
            assertTrue(onEdt(()->pad.openList.isVisible()));
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    private static String note(NoteStore store,String in,String title) {
        NoteStore.Note one=new NoteStore.Note();one.book=in;one.title=title;one.body="Synthetic "+title.toLowerCase();one.updated=System.currentTimeMillis();store.save(one);return one.id;
    }
    /** Every label's words in a component, in order. */
    private static String words(Container in) {
        StringBuilder out=new StringBuilder();
        for(Component one:in.getComponents()) {
            if(one instanceof JLabel l&&l.getText()!=null)out.append(l.getText()).append(" | ");
            if(one instanceof Container c)out.append(words(c));
        }
        return out.toString();
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(300);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
