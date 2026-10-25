package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Ctrl+V on Home or in Temp keeps a picture there (the owner, 2026-10-21: "after a screen grab with greenshot, a ctrl+v would
 * let us put the image directly in a folder"; 2026-10-22: "doesnt work"; decision 233), wherever the keys are, the search
 * included: a made-up picture on the clipboard, pasted as a key would, then found kept.
 */
public class DesktopPasteHomeTest {
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

    /** A picture alone on the clipboard, as a screen grab puts it. */
    private static void grab() {
        BufferedImage shot=new BufferedImage(20,12,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=shot.createGraphics();g.setColor(Color.ORANGE);g.fillRect(0,0,20,12);g.dispose();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable(){
            public DataFlavor[] getTransferDataFlavors(){return new DataFlavor[]{DataFlavor.imageFlavor};}
            public boolean isDataFlavorSupported(DataFlavor f){return DataFlavor.imageFlavor.equals(f);}
            public Object getTransferData(DataFlavor f)throws UnsupportedFlavorException{if(!isDataFlavorSupported(f))throw new UnsupportedFlavorException(f);return shot;}
        },null);
    }
    private int pictures(){return (int)pad.store.contents(Things.HOME).stream().filter(b->b.kind==NoteStore.Branch.Kind.FILE).count()
        +(int)pad.store.temporary(System.currentTimeMillis()).stream().filter(b->b.kind==NoteStore.Branch.Kind.FILE).count();}

    @Test public void aPicturePastedInTempWithTheSearchHoldingTheKeysIsKeptInTemp() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});Thread.sleep(400);
        grab();
        // Ctrl+V with the search's keys: the search's own paste, which the window's handler wraps.
        SwingUtilities.invokeAndWait(()->{pad.home.search.requestFocusInWindow();
            TransferHandler.getPasteAction().actionPerformed(new java.awt.event.ActionEvent(pad.home.search,java.awt.event.ActionEvent.ACTION_PERFORMED,"paste"));});
        for(int i=0;i<200&&pad.store.temporary(System.currentTimeMillis()).stream().noneMatch(b->b.kind==NoteStore.Branch.Kind.FILE);i++)Thread.sleep(100);
        assertTrue("kept in Temp; the bar said: "+onEdt(()->pad.status.getText()),
            pad.store.temporary(System.currentTimeMillis()).stream().anyMatch(b->b.kind==NoteStore.Branch.Kind.FILE));
        assertEquals("nothing typed in the search","",onEdt(()->pad.home.search.getText()).trim());
    }

    @Test public void aPicturePastedOnHomeWithNoFieldIsKeptOnHome() throws Exception {
        SwingUtilities.invokeAndWait(pad::goHome);Thread.sleep(400);
        int before=pictures();grab();
        SwingUtilities.invokeAndWait(()->{pad.frame.getRootPane().requestFocusInWindow();pad.pasteHere();});
        for(int i=0;i<200&&pictures()==before;i++)Thread.sleep(100);
        assertEquals("kept on Home; the bar said: "+onEdt(()->pad.status.getText()),before+1,pictures());
    }

    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
