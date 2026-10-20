package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** A note made with Temp's + on the PC and written in (the owner, 2026-10-11): kept, temporary, and in Temp. Synthetic, offline. */
public class DesktopTempNoteTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    @Test public void aNoteMadeInTempAndWrittenInIsKeptAndStaysInTemp() throws Exception {
        String before=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->pad.newNoteIn(NoteStore.TEMP));
        await(()->pad.store.latest()!=null&&!pad.store.latest().id.equals(before)&&pad.page.isEditable());settle();
        String id=pad.store.latest().id;
        assertTrue("made temporary",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        SwingUtilities.invokeAndWait(()->{pad.title.setText("Groceries");pad.page.requestFocusInWindow();pad.page.replaceSelection("Bread\nMilk");});
        SwingUtilities.invokeAndWait(()->pad.save(null));
        await(()->"Bread\nMilk".equals(pad.store.get(id).body));settle();
        assertEquals("Groceries",pad.store.get(id).title);
        assertTrue("still temporary once written in",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        // Home and back: what Temp lists has it, and it opens with its words.
        SwingUtilities.invokeAndWait(pad::goHome);settle();
        boolean[] listed={false};
        for(NoteStore.Branch one:pad.store.temporary(System.currentTimeMillis()))if(one.id.equals(id))listed[0]=true;
        assertTrue("listed in Temp",listed[0]);
        SwingUtilities.invokeAndWait(()->pad.open(id));await(()->pad.page.getText().equals("Bread\nMilk"));
    }

    /** The owner's way (2026-10-11): the Temp tile's card open, its +, a title and words, Home, and the card again. */
    @Test public void fromTheTempCardItIsOnTheCardAfterwards() throws Exception {
        // One of the owner's devices paired, as on the owner's PC, so what is let go on Temp goes to it too.
        org.mininotes.desktop.platform.content.Context phoneHome=new org.mininotes.desktop.platform.content.Context(temp.newFolder("phone"));
        Keys phone=new Keys(phoneHome);
        pad.store.pairedWith("MxTempCardPhone@127.0.0.1:9881","Phone",true,phone.agreement().getPublic().getEncoded(),phone.signing().getPublic().getEncoded());
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});settle();
        String before=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->((JMenuItem)pad.home.plusMenu(NoteStore.TEMP).getComponent(0)).doClick());
        await(()->pad.store.latest()!=null&&!pad.store.latest().id.equals(before)&&pad.page.isEditable());settle();
        String id=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->{pad.title.setText("Screens");pad.page.requestFocusInWindow();pad.page.replaceSelection("Shot one");});
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(pad::goHome);
        await(()->"Shot one".equals(pad.store.get(id).body));settle();
        assertTrue("still temporary",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
        SwingUtilities.invokeAndWait(()->pad.home.folder.openPlace(DesktopHome.tempPlace(0)));settle();Thread.sleep(500);
        java.util.List<String> said=new java.util.ArrayList<>();
        SwingUtilities.invokeAndWait(()->texts(pad.home.folder.card,said));
        assertTrue("on the Temp card: "+said,said.stream().anyMatch(s->s.contains("Screens")));
    }
    /** A screenshot dropped on Temp's icon (the owner, 2026-10-11; decision 150): kept, temporary, listed in Temp. */
    @Test public void aFileDroppedOnTempIsInTemp() throws Exception {
        Path shot=temp.newFile("screenshot.png").toPath();Files.write(shot,new byte[]{(byte)0x89,'P','N','G',13,10,26,10,1,2,3});
        SwingUtilities.invokeAndWait(()->{pad.goHome();
            DesktopDrops.dropped(pad,new DesktopDrops.Aim(DesktopDrops.KEEP,NoteStore.TEMP,"Temp",pad.frame.getRootPane(),new java.awt.Rectangle()),java.util.List.of(shot));});
        await(()->pad.store.temporary(System.currentTimeMillis()).stream().anyMatch(b->b.kind==NoteStore.Branch.Kind.FILE&&b.name.contains("screenshot")));
    }

    private static void texts(java.awt.Container in,java.util.List<String> out) {
        for(java.awt.Component c:in.getComponents()) {
            if(c instanceof JLabel l&&l.getText()!=null)out.add(l.getText());
            if(c instanceof javax.accessibility.Accessible a&&a.getAccessibleContext()!=null&&a.getAccessibleContext().getAccessibleName()!=null)out.add(a.getAccessibleContext().getAccessibleName());
            if(c instanceof java.awt.Container k)texts(k,out);
        }
    }

    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(150);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
