package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** A contact in trouble said under the bar, with what to do (decision 188). Made-up contact, offline; drawn from the window. */
public class DesktopTroublesTest {
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
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isDisplayable())w.dispose();});
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void somethingOwedForDaysIsSaidWithWhatToDo() throws Exception {
        Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder("ana").toPath().toFile()));
        String ana="MxAnaTrouble@127.0.0.1:9951";
        pad.store.pairedWith(ana,"Ana",false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
        NoteStore.Note plan=new NoteStore.Note();plan.title="Plan";plan.body="made up";plan.book=Things.HOME;pad.store.save(plan);
        pad.store.give(Sharing.Scope.PAGE,plan.id,ana,Sharing.Level.WRITE,"Ana");
        // Changed four days ago, and nothing heard from Ana since: stuck, as the red mark says.
        pad.store.getWritableDatabase().execSQL("UPDATE notes SET updated=? WHERE id=?",new Object[]{System.currentTimeMillis()-4L*24*60*60*1000,plan.id});
        assertFalse("found",Troubles.of(pad.store).isEmpty());
        assertEquals(Troubles.Kind.STUCK,Troubles.of(pad.store).get(0).kind());
        SwingUtilities.invokeAndWait(()->pad.troubles.soon());
        await(()->pad.troubles.said()!=null);
        assertTrue(pad.troubles.said(),pad.troubles.said().startsWith("Ana not reached for days"));
        SwingUtilities.invokeAndWait(()->{try{shoot(pad.frame,"98-trouble-strip",0,0,pad.frame.getWidth(),160);}catch(Exception e){throw new RuntimeException(e);}});
        SwingUtilities.invokeLater(()->pad.troubles.open());Thread.sleep(1500);
        JDialog[] box={null};SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"98b-what-to-do",0,0,box[0].getWidth(),box[0].getHeight());}catch(Exception e){throw new RuntimeException(e);}});
    }

    private static void shoot(Window window,String name,int x,int y,int w,int h) throws Exception {
        var scale=window.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(w*scale.getScaleX()),(int)Math.ceil(h*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);g.translate(-x,-y);window.paint(g);g.dispose();
        javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
