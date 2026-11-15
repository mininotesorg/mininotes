package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * A file too big for the pad let go on Temp (the owner, 2026-11-04: "I tried to drop a large file 169000 Kb in temp and nothing
 * happen"; the + said "Nothing was added. … more than 25 MB"): a drop says what happened, as the + does. A made-up notebook.
 */
public class DesktopDropSaysTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    private Path big() throws Exception {
        Path big=temp.newFile("synthetic-setup.exe").toPath();
        try(java.io.RandomAccessFile f=new java.io.RandomAccessFile(big.toFile(),"rw")){f.setLength(Attachment.LIMIT+1024*1024);}
        return big;
    }

    @Test public void aTooBigFileDroppedOnTempsOpenCardIsSaid() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});Thread.sleep(800);
        letGo(new DesktopDrops.Aim(DesktopDrops.KEEP,NoteStore.TEMP,"Temp",pad.home.folder,null));
    }
    @Test public void aTooBigFileDroppedOnHomesTempIsSaid() throws Exception {
        SwingUtilities.invokeAndWait(pad::goHome);Thread.sleep(800);
        letGo(new DesktopDrops.Aim(DesktopDrops.KEEP,NoteStore.TEMP,"Temp",pad.home,null));
    }

    /** As a drop goes: held over (the bar says what letting go does), let go (the bar put back), then the files taken. */
    private void letGo(DesktopDrops.Aim aim) throws Exception {
        Path big=big();
        SwingUtilities.invokeAndWait(()->DesktopDrops.over(pad,aim));
        SwingUtilities.invokeAndWait(()->{DesktopDrops.away(pad);SwingUtilities.invokeLater(()->DesktopDrops.dropped(pad,aim,List.of(big)));});
        // The drop's own listener hears the drop after the handler took it.
        SwingUtilities.invokeAndWait(()->DesktopDrops.away(pad));
        String[] said={""};boolean[] seen={false};
        await(()->{SwingUtilities.invokeAndWait(()->{said[0]=pad.status.getText();seen[0]=pad.status.isShowing();});return said[0].startsWith("Nothing was added");});
        assertTrue("said: "+said[0],said[0].contains("synthetic-setup.exe"));
        assertTrue("the bar saying it is on screen: "+said[0],seen[0]);
        // And a box saying which and why, not to be missed with the eyes where the file was let go.
        JDialog[] box={null};
        await(()->{SwingUtilities.invokeAndWait(()->{for(java.awt.Window w:java.awt.Window.getWindows())
            if(w instanceof JDialog d&&d.isShowing()&&d.getTitle()!=null&&d.getTitle().startsWith("Nothing was added"))box[0]=d;});return box[0]!=null;});
        SwingUtilities.invokeAndWait(box[0]::dispose);
    }

    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Not seen in 20 s");}
}
