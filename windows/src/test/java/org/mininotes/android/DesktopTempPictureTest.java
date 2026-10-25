package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * A note made with Temp's +, nothing typed in it, a picture pasted, then back (the owner, 2026-10-21: "if we type nothing in a
 * note in temp and only add a picture ... the note is saved on the home page but not in the Temp folder"; decision 233): it is
 * in Temp, with its picture, and not on Home. A made-up notebook, drawn from the window.
 */
public class DesktopTempPictureTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        java.nio.file.Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void aPictureAloneInANoteMadeInTempKeepsItInTemp() throws Exception {
        // The owner's way: Temp's card open, its +, nothing typed, a picture pasted, then Home.
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});Thread.sleep(400);
        String before=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->((JMenuItem)pad.home.plusMenu(NoteStore.TEMP).getComponent(0)).doClick());
        await(()->pad.store.latest()!=null&&!pad.store.latest().id.equals(before)&&pad.page.isEditable());Thread.sleep(400);
        String id=pad.store.latest().id;
        assertTrue("made in Temp",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        BufferedImage dot=new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);
        String state=onEdt(()->"onPage="+pad.onPage()+" editable="+pad.page.isEditable()+" shown="+id.equals(pad.noteShown()));
        SwingUtilities.invokeAndWait(()->pad.attachPicture(id,null,dot));
        for(int i=0;i<300&&pad.store.filesOf(NoteStore.Branch.Kind.PAGE,id).isEmpty();i++)Thread.sleep(100);
        assertFalse("the picture kept ("+state+"); the bar said: "+onEdt(()->pad.status.getText()),pad.store.filesOf(NoteStore.Branch.Kind.PAGE,id).isEmpty());
        SwingUtilities.invokeAndWait(pad::goHome);
        Thread.sleep(1500);SwingUtilities.invokeAndWait(()->{});
        assertNotNull("the note is kept",pad.store.get(id));
        assertTrue("still in Temp",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        assertTrue("listed in Temp",pad.store.temporary(System.currentTimeMillis()).stream().anyMatch(b->b.id.equals(id)));
        assertFalse("not on Home while Temp's things are not shown there",pad.store.contents(Things.HOME).stream().anyMatch(b->b.id.equals(id)));
        assertEquals("its picture with it",1,pad.store.filesOf(NoteStore.Branch.Kind.PAGE,id).size());
    }

    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
