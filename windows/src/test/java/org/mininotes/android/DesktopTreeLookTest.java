package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.List;

/**
 * The tree down the side, four levels deep with long names (the owner, 2026-10-28: "the tree should be aligned on the left so
 * that it is displayed in full", decision 270), and the faces on it drawn at 125% (decision 271). A made-up notebook.
 */
public class DesktopTreeLookTest {
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

    @Test public void deepLinesStartNearTheLeftAndHomeStaysOpen() throws Exception {
        String working=pad.store.addCollectionIn(Things.HOME,"Working").id,projects=pad.store.addCollectionIn(working,"Projects").id,
            stables=pad.store.addCollectionIn(projects,"Stables").id;
        pad.store.paint(NoteStore.Branch.Kind.COLLECTION,working,3);
        NoteStore.Note deep=new NoteStore.Note();deep.title="Stables Todo 2026 synthetic";deep.body="Synthetic";deep.book=stables;pad.store.save(deep);
        pad.store.paint(NoteStore.Branch.Kind.PAGE,deep.id,6);
        SwingUtilities.invokeAndWait(()->{pad.showTree(true);pad.refresh();});Thread.sleep(1500);
        SwingUtilities.invokeAndWait(()->{for(int i=0;i<pad.tree.getRowCount();i++)pad.tree.expandRow(i);});Thread.sleep(300);
        int[] at=new int[2];
        SwingUtilities.invokeAndWait(()->{
            at[0]=pad.tree.getRowBounds(0).x;
            for(int i=0;i<pad.tree.getRowCount();i++)if(String.valueOf(((DefaultMutableTreeNode)pad.tree.getPathForRow(i).getLastPathComponent()).getUserObject()).contains("Stables Todo"))at[1]=pad.tree.getRowBounds(i).x;
        });
        assertTrue("Home's line starts at the left edge: "+at[0],at[0]<=4);
        assertTrue("four levels down, a short way in: "+at[1],at[1]>0&&at[1]<=4*14+4);
        // A double click on Home cannot leave it closed with no arrow to open it again.
        SwingUtilities.invokeAndWait(()->{Rectangle r=pad.tree.getRowBounds(0);
            for(int c=1;c<=2;c++){pad.tree.dispatchEvent(new MouseEvent(pad.tree,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,r.x+20,r.y+r.height/2,c,false,MouseEvent.BUTTON1));
                pad.tree.dispatchEvent(new MouseEvent(pad.tree,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,r.x+20,r.y+r.height/2,c,false,MouseEvent.BUTTON1));}});
        Thread.sleep(400);
        boolean[] open={false};SwingUtilities.invokeAndWait(()->open[0]=pad.tree.isExpanded(0));
        assertTrue("Home still open after a double click",open[0]);
        String out=System.getenv("MININOTES_PICTURES");
        if(out!=null)SwingUtilities.invokeAndWait(()->{
            JComponent side=(JComponent)pad.tree.getParent().getParent().getParent();
            float scale=1.25f;
            java.awt.image.BufferedImage picture=new java.awt.image.BufferedImage(Math.round(side.getWidth()*scale),Math.round(side.getHeight()*scale),java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g=picture.createGraphics();g.scale(scale,scale);side.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(picture,"png",new java.io.File(out,"270-271-tree-125.png"));}catch(java.io.IOException e){throw new RuntimeException(e);}
        });
        // The whole notebook's mark in the top bar, beside the window's name (decision 275): nothing shared here, so the quiet ring.
        JLabel[] whole={null};
        SwingUtilities.invokeAndWait(()->{Deque<Component> left=new ArrayDeque<>(List.of(pad.frame.getJMenuBar()));
            while(!left.isEmpty()){Component c=left.pop();if(c instanceof JLabel l&&"wholeMark".equals(l.getName()))whole[0]=l;if(c instanceof Container k)for(Component in:k.getComponents())left.push(in);}});
        assertNotNull("the top bar's mark",whole[0]);
        assertTrue("showing",whole[0].isShowing()&&whole[0].getIcon()!=null);
        assertEquals("Not shared",whole[0].getToolTipText());
        // On the right, just before the Agent, as on the phone (decision 276).
        int[] x={0,0};SwingUtilities.invokeAndWait(()->{JMenuBar bar=pad.frame.getJMenuBar();x[0]=SwingUtilities.convertPoint(whole[0],0,0,bar).x;x[1]=bar.getWidth();});
        assertTrue("in the right half of the bar: "+x[0]+" of "+x[1],x[0]>x[1]/2);
        if(out!=null)SwingUtilities.invokeAndWait(()->{
            JComponent bar=pad.frame.getJMenuBar();float scale=1.25f;
            java.awt.image.BufferedImage picture=new java.awt.image.BufferedImage(Math.round(bar.getWidth()*scale),Math.round(bar.getHeight()*scale),java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g=picture.createGraphics();g.scale(scale,scale);bar.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(picture,"png",new java.io.File(out,"275-top-bar.png"));}catch(java.io.IOException e){throw new RuntimeException(e);}
        });
    }

    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
