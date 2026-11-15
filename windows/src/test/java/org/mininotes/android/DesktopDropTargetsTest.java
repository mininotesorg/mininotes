package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Wherever files from Explorer can be let go inside an open Temp, something takes them (the owner, 2026-11-04: "inside Temp
 * once it was open ... nothing happen"): no part of the window shown has a drop of its own that refuses files. A made-up notebook.
 */
public class DesktopDropTargetsTest {
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

    @Test public void everythingInAnOpenTempTakesFiles() throws Exception {
        // One small file in Temp first, so its tile is there too.
        Path small=temp.newFile("small.txt").toPath();Files.writeString(small,"hello");
        SwingUtilities.invokeAndWait(()->pad.keep(Things.HOME,"Temp",List.of(small),NoteStore.TEMP));Thread.sleep(1500);
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(1));});
        // At once, before its things are read: a drop then is Temp's too.
        Thread.sleep(150);
        List<String> refusing=new ArrayList<>();List<String> aims=new ArrayList<>();
        SwingUtilities.invokeAndWait(()->{
            Container root=pad.frame.getContentPane();
            walk(root,refusing);
            // Points across the open Temp: each is Temp's, to keep in Temp.
            JComponent folder=pad.home.folder;
            for(int y=20;y<folder.getHeight();y+=60)for(int x=20;x<folder.getWidth();x+=80) {
                java.awt.Point at=SwingUtilities.convertPoint(folder,x,y,pad.frame);
                if(!folder.isShowing())continue;
                Component deep=SwingUtilities.getDeepestComponentAt(folder,x,y);
                DesktopDrops.Aim aim=DesktopDrops.aim(pad,pad.frame,at);
                // On a note's icon in Temp, the file is attached to that note, as on Home.
                if(SwingUtilities.getAncestorOfClass(DesktopHome.Tile.class,deep)!=null||deep instanceof DesktopHome.Tile)continue;
                if(aim.does()!=DesktopDrops.KEEP||!NoteStore.TEMP.equals(aim.note()))aims.add(x+","+y+" "+(deep==null?"-":deep.getClass().getName())+" -> "+aim.does()+" "+aim.note());
            }
        });
        assertTrue("parts that would refuse files: "+refusing,refusing.isEmpty());
        assertTrue("points not aimed at Temp: "+aims,aims.isEmpty());
    }

    private static void walk(Container in,List<String> refusing) {
        for(Component one:in.getComponents()) {
            if(one.isShowing()&&one.getDropTarget()!=null&&one.getDropTarget().isActive()) {
                TransferHandler h=one instanceof JComponent j?j.getTransferHandler():null;
                if(!(h instanceof DesktopDrops.Aimed)&&!(h instanceof DesktopDrops.Dropping))refusing.add(one.getClass().getName()+" "+one.getBounds()+" "+(h==null?"no handler":h.getClass().getName()));
            }
            if(one instanceof Container k)walk(k,refusing);
        }
    }

    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Not seen in 20 s");}
}
