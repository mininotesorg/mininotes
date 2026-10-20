package org.mininotes.android;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/**
 * Windows' own file box (the owner, 2026-10-13: "the modern Windows Explorer when saving and opening a MD file"): shown over
 * a window of the test's, and answered as a person would, Save with the name it offers.
 */
public class DesktopFileDialogTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void theSaveBoxOffersTheNotesTitleInTheFolderAsked() throws Exception {
        Assume.assumeTrue(System.getProperty("os.name","").startsWith("Windows"));
        Path folder=temp.newFolder("Notes").toPath();
        JFrame[] owner={null};
        SwingUtilities.invokeAndWait(()->{owner[0]=new JFrame("file box owner");owner[0].setBounds(80,80,700,500);owner[0].setVisible(true);});
        AtomicReference<Object> got=new AtomicReference<>();
        String title="Mininotes test: keep as a file";
        SwingUtilities.invokeLater(()->{try{got.set(DesktopFileDialog.save(owner[0],title,folder,"Shopping list.md","Keep here"));}catch(Exception e){got.set(e);}});
        try {
            WinDef.HWND box=null;
            for(int i=0;i<200&&box==null;i++){Thread.sleep(50);box=User32.INSTANCE.FindWindow("#32770",title);}
            assertNotNull("Windows' box came up",box);
            // Not pictured: a capture of the screen there could catch whatever else is on it.
            // The window behind went on being drawn while the box was up: its events were not held.
            boolean[] alive={false};SwingUtilities.invokeAndWait(()->alive[0]=true);assertTrue(alive[0]);
            // Save, as a person presses it once the box has settled; pressed again while it has not answered.
            for(int tries=0;tries<10&&got.get()==null;tries++) {
                Thread.sleep(800);
                User32.INSTANCE.PostMessage(box,0x0111,new WinDef.WPARAM(1),new WinDef.LPARAM(0));
                for(int i=0;i<40&&got.get()==null;i++)Thread.sleep(50);
            }
            assertEquals(folder.resolve("Shopping list.md").toAbsolutePath(),got.get());
        } finally {
            // Never left open, whatever happened: Cancel, so the window behind is let go.
            WinDef.HWND left=User32.INSTANCE.FindWindow("#32770",title);
            if(left!=null)User32.INSTANCE.PostMessage(left,0x0111,new WinDef.WPARAM(2),new WinDef.LPARAM(0));
            for(int i=0;i<100&&got.get()==null;i++)Thread.sleep(50);
            SwingUtilities.invokeAndWait(()->owner[0].dispose());
        }
    }
}
