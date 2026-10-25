package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * A note made with Temp's +, words typed and the title left empty, then back (the owner, 2026-10-24: "Again, adding a note to
 * temp, without title saved it on the desktop not in temp"; decision 238): in Temp and not on Home. And the same with nothing
 * typed at all: not kept, or kept in Temp, never on Home. A made-up notebook, drawn from the window.
 */
public class DesktopTempUntitledTest {
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

    @Test public void wordsWithNoTitleStayInTemp() throws Exception {
        String id=fromTempsPlus();
        SwingUtilities.invokeAndWait(()->{pad.page.requestFocusInWindow();pad.page.replaceSelection("Bread\nMilk");});
        assertEquals("no title typed","",onEdt(()->pad.title.getText()));
        SwingUtilities.invokeAndWait(pad::goHome);
        await(()->pad.store.get(id)!=null&&"Bread\nMilk".equals(pad.store.get(id).body));Thread.sleep(800);SwingUtilities.invokeAndWait(()->{});
        assertEquals("still no title","",pad.store.get(id).title);
        inTempNotOnHome(id);
    }

    @Test public void nothingTypedIsNotOnHome() throws Exception {
        String id=fromTempsPlus();
        SwingUtilities.invokeAndWait(pad::goHome);
        Thread.sleep(1500);SwingUtilities.invokeAndWait(()->{});
        if(pad.store.get(id)!=null)inTempNotOnHome(id);
    }

    /** The owner's way: Temp's card open, its +, Note. */
    private String fromTempsPlus() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});Thread.sleep(400);
        String before=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->((JMenuItem)pad.home.plusMenu(NoteStore.TEMP).getComponent(0)).doClick());
        await(()->pad.store.latest()!=null&&!pad.store.latest().id.equals(before)&&pad.page.isEditable());Thread.sleep(400);
        return pad.store.latest().id;
    }
    private void inTempNotOnHome(String id) {
        assertTrue("still in Temp",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        assertTrue("listed in Temp",pad.store.temporary(System.currentTimeMillis()).stream().anyMatch(b->b.id.equals(id)));
        assertFalse("not on Home while Temp's things are not shown there",pad.store.contents(Things.HOME).stream().anyMatch(b->b.id.equals(id)));
    }

    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
