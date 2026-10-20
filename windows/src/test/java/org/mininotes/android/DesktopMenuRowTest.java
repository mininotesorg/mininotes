package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * The menu over Recent (decision 192): a line lined up with Recent's own - its mark where their faces are, its word where
 * their names start - with room and a line before them; back in the bar while Recent is not shown. Made-up notes.
 */
public class DesktopMenuRowTest {
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
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void theMenuLinesUpWithRecent() throws Exception {
        for(String title:new String[]{"Saturday","Transfer","Friday"}) {
            NoteStore.Note n=new NoteStore.Note();n.title=title;n.body="Synthetic "+title;n.book=Things.HOME;pad.store.save(n);
            pad.store.touch(NoteStore.Branch.Kind.PAGE,n.id);
        }
        SwingUtilities.invokeAndWait(()->pad.openList.refresh());
        await(()->onEdt(()->pad.openList.isShowing()&&pad.openList.menuRow.isShowing()));
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(()->{
            JButton row=pad.openList.menuRow;
            // The first line of Recent: its face's left edge, and its name's.
            JComponent line=first(pad.openList);
            assertNotNull("a line in Recent",line);
            Component face=line.getComponent(0),name=line.getComponent(1);
            int faceX=SwingUtilities.convertPoint(face,0,0,pad.openList).x,nameX=SwingUtilities.convertPoint(name,0,0,pad.openList).x;
            Insets in=row.getInsets();int markX=SwingUtilities.convertPoint(row,in.left,0,pad.openList).x;
            assertEquals("the mark where the faces are",faceX,markX);
            assertEquals("the word where the names start",nameX,markX+row.getIcon().getIconWidth()+row.getIconTextGap());
            int below=SwingUtilities.convertPoint(line,0,0,pad.openList).y-SwingUtilities.convertPoint(row,0,row.getHeight(),pad.openList).y;
            assertTrue("room before Recent: "+below,below>=30);
            assertFalse("not in the bar as well",onBar());
        });
        shoot(pad.frame,"92b-menu-over-recent");
        // Recent switched off: the menu is back in the bar.
        SwingUtilities.invokeAndWait(()->pad.setOpenListWanted(false));
        await(()->onEdt(()->!pad.openList.isShowing()&&onBar()));
    }

    private boolean onBar() {
        for(Component c:pad.frame.getJMenuBar().getComponents())if(c instanceof Container k)for(Component b:k.getComponents())
            if(b instanceof JButton j&&"Menu".equals(j.getToolTipText())&&j.isShowing())return true;
        return false;
    }
    /** The first line Recent shows: a panel holding a face and a name. */
    private static JComponent first(Container in) {
        for(Component c:in.getComponents()) {
            if(c instanceof JPanel p&&p.getComponentCount()==2&&p.getComponent(0) instanceof JLabel f&&f.getIcon()!=null&&p.getComponent(1) instanceof JLabel)return p;
            if(c instanceof Container k){JComponent got=first(k);if(got!=null)return got;}
        }
        return null;
    }
    private static void shoot(JFrame frame,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            int w=300,h=260,x=frame.getWidth()-w;
            var scale=frame.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(w*scale.getScaleX()),(int)Math.ceil(h*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);g.translate(-x,0);frame.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
