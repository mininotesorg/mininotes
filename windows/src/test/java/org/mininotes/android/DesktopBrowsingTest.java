package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Synthetic windows only: visible handles, browsing origin, actual storage parent and return position. */
public class DesktopBrowsingTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;
    @Before public void start() throws Exception {
        Path dir=temp.newFolder().toPath();SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(dir,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}
    @Test public void scrollbarHandleIsVisibleWithoutHoverOnLightAndDarkPaper() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JScrollPane pane=pad.paperScroll;JScrollBar bar=pane.getVerticalScrollBar();
            assertTrue(bar.getUI() instanceof DesktopScroll);DesktopScroll ui=new DesktopScroll();
            for(Color ground:new Color[]{DesktopUi.PAPER,new Color(30,35,30)}){
                pane.putClientProperty("ground",ground);Color quiet=ui.getThumbColor(bar,false,false),hover=ui.getThumbColor(bar,true,false),pressed=ui.getThumbColor(bar,false,true);
                assertNotEquals("handle is visible at rest",ground,quiet);assertNotEquals(quiet,hover);assertNotEquals(quiet,pressed);
                assertTrue("resting thumb has visible contrast",Math.abs(quiet.getRed()-ground.getRed())+Math.abs(quiet.getGreen()-ground.getGreen())+Math.abs(quiet.getBlue()-ground.getBlue())>70);
            }
            pane.putClientProperty("ground",null);pad.title.setText("Synthetic scrollbar visibility");pad.page.setText("Long synthetic writing, with a visible scroll handle.\n".repeat(300));pad.save(null);
        });
        settle();SwingUtilities.invokeAndWait(()->shoot("scrollbar-without-hover"));
    }
    @Test public void recentFolderBackKeepsItsNameScrollAndPhysicalParent() throws Exception {
        var parent=pad.store.addCollection("Actual parent");var child=pad.store.addCollectionIn(parent.id,"Listed folder");
        for(int i=0;i<65;i++){var c=pad.store.addCollection("Recent synthetic "+i);pad.store.touch(NoteStore.Branch.Kind.COLLECTION,c.id);}
        pad.store.touch(NoteStore.Branch.Kind.COLLECTION,child.id);
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.recentPlace());});settle();
        var folder=pad.home.folder;JScrollPane scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,folder.grid);
        SwingUtilities.invokeAndWait(()->scroll.getViewport().setViewPosition(new Point(0,160)));settle();
        int kept=scroll.getViewport().getViewPosition().y;assertTrue(kept>0);
        SwingUtilities.invokeAndWait(()->{var tile=folder.grid.tiles.stream().filter(t->t.thing.id.equals(child.id)).findFirst().orElseThrow();pad.home.opened(tile.thing,tile.where);});settle();
        SwingUtilities.invokeAndWait(()->{assertEquals(child.id,folder.id());assertEquals(parent.id,folder.parentId());assertEquals("Back to Recent",folder.back.getAccessibleContext().getAccessibleName());shoot("recent-folder-back");folder.back.doClick();});settle();
        assertEquals(NoteStore.RECENT,folder.placeShown().id);assertEquals(kept,scroll.getViewport().getViewPosition().y);assertEquals(parent.id,pad.store.collectionOfBook(child.id));
        SwingUtilities.invokeAndWait(()->shoot("recent-return-position"));
    }
    @Test public void recentSidebarCollectionHasAWayBackToRecent() throws Exception {
        var collection=pad.store.addCollection("Sidebar synthetic folder");pad.store.touch(NoteStore.Branch.Kind.COLLECTION,collection.id);
        SwingUtilities.invokeAndWait(()->{pad.setOpenListWanted(true);pad.openList.refresh();});settle();
        SwingUtilities.invokeAndWait(()->{JLabel label=find(pad.openList,"Sidebar synthetic folder");assertNotNull(label);
            MouseEvent click=new MouseEvent(label,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,5,5,1,false,MouseEvent.BUTTON1);
            for(MouseListener hand:label.getMouseListeners())hand.mouseClicked(click);
        });settle();
        assertEquals(collection.id,pad.home.folder.id());assertEquals("Back to Recent",pad.home.folder.back.getAccessibleContext().getAccessibleName());
        SwingUtilities.invokeAndWait(()->pad.home.folder.up());settle();assertEquals(NoteStore.RECENT,pad.home.folder.placeShown().id);
    }
    @Test public void nestedBrowsingReturnsOneLevelThenToTempWithoutMovingAnything() throws Exception {
        var collection=pad.store.addCollection("Temporary folder");var nested=pad.store.addCollectionIn(collection.id,"Nested");
        NoteStore.Branch outer=pad.store.contents(Things.HOME).stream().filter(t->t.id.equals(collection.id)).findFirst().orElseThrow();
        NoteStore.Branch inner=pad.store.contents(collection.id).stream().filter(t->t.id.equals(nested.id)).findFirst().orElseThrow();
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));pad.home.folder.into(outer);});settle();
        SwingUtilities.invokeAndWait(()->pad.home.folder.into(inner));settle();assertEquals(collection.id,pad.home.folder.parentId());
        SwingUtilities.invokeAndWait(()->pad.home.folder.up());settle();assertEquals(collection.id,pad.home.folder.id());
        SwingUtilities.invokeAndWait(()->pad.home.folder.up());settle();assertEquals(NoteStore.TEMP,pad.home.folder.placeShown().id);
        assertEquals(collection.id,pad.store.collectionOfBook(nested.id));
    }
    private static JLabel find(Container in,String text){for(Component c:in.getComponents()){if(c instanceof JLabel l&&text.equals(l.getText()))return l;if(c instanceof Container child){JLabel got=find(child,text);if(got!=null)return got;}}return null;}
    private void shoot(String name){try{var scale=pad.frame.getGraphicsConfiguration().getDefaultTransform();var image=new java.awt.image.BufferedImage((int)Math.ceil(pad.frame.getWidth()*scale.getScaleX()),(int)Math.ceil(pad.frame.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.transform(scale);pad.frame.paint(g);g.dispose();Path path=Path.of("build","verification","browsing",name+".png");Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private void settle() throws Exception {CountDownLatch disk=new CountDownLatch(1);pad.disk.submit(()->null,v->disk.countDown(),e->disk.countDown());assertTrue(disk.await(30,TimeUnit.SECONDS));Thread.sleep(900);SwingUtilities.invokeAndWait(pad.frame::validate);}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
