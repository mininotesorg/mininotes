package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Several icons chosen on Home at once (decision 292), in the window: Ctrl+click chooses them and draws them chosen, Shift+click
 * the run, the menu on one of them acts on all of them (files: Save, Copy, Send, Move to bin; a note among them: Move to bin
 * only), Copy puts them all on the clipboard as files, and Esc lets go of them. Made-up files and notes.
 */
public class DesktopChoosingWindowTest {
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
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isDisplayable())w.dispose();});
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void severalChosenAreActedOnTogether() throws Exception {
        for(String name:new String[]{"beach.jpg","boat.jpg","sunset.jpg"}) {
            NoteStore.Held f=pad.store.opening(NoteStore.Branch.Kind.COLLECTION,Things.HOME,name,"application/octet-stream",name.length());
            Files.write(pad.store.fileFor(f.id).toPath(),name.getBytes());pad.store.keep(f);
        }
        NoteStore.Note n=new NoteStore.Note();n.title="Holiday";n.body="Synthetic holiday";n.book=Things.HOME;pad.store.save(n);
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle();
        DesktopHome home=pad.home;DesktopHome.Icons grid=home.grid;

        // Ctrl+click two: both chosen, drawn so, and neither opened.
        SwingUtilities.invokeAndWait(()->{click(tile(grid,"beach.jpg"),InputEvent.CTRL_DOWN_MASK);click(tile(grid,"sunset.jpg"),InputEvent.CTRL_DOWN_MASK);});
        SwingUtilities.invokeAndWait(()->{
            assertTrue(home.picked(tile(grid,"beach.jpg")));assertTrue(home.picked(tile(grid,"sunset.jpg")));
            assertFalse(home.picked(tile(grid,"boat.jpg")));
            assertFalse("nothing opened",pad.onPage());
        });
        shoot(pad.frame,"292-two-chosen");
        // The menu on one of them: for both, each line saying how many.
        JPopupMenu menu=onEdt(()->home.menuAt(tile(grid,"beach.jpg")));
        for(String line:new String[]{"Save on my computer…  (2)","Copy  (2)","Send to a device…  (2)","Move to bin  (2)"})
            assertNotNull(line,onEdt(()->item(menu,line)));
        // Copy: both on the clipboard, as files to paste in a folder.
        SwingUtilities.invokeAndWait(()->item(menu,"Copy  (2)").doClick());
        for(int i=0;i<150&&!onEdt(()->pad.status.getText().startsWith("2 files copied"));i++)Thread.sleep(100);
        assertEquals("2 files copied: paste them in a folder",onEdt(()->pad.status.getText()));
        @SuppressWarnings("unchecked") List<java.io.File> copied=(List<java.io.File>)Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
        // In the grid's order, each under its own name, with what it holds.
        List<String> names=new ArrayList<>();for(java.io.File f:copied){names.add(f.getName());assertEquals(f.getName(),Files.readString(f.toPath()));}
        java.util.Collections.sort(names);
        assertEquals(List.of("beach.jpg","sunset.jpg"),names);

        // The menu on an icon not chosen is its own, and lets go of the choice.
        JPopupMenu own=onEdt(()->home.menuAt(tile(grid,"boat.jpg")));
        assertNotNull(onEdt(()->item(own,Say.t("Open with…"))));
        assertTrue(onEdt(()->home.choosing.isEmpty()));

        // Shift+click: the run from the icon last clicked, along the grid's order. Then Esc: nothing chosen.
        SwingUtilities.invokeAndWait(()->{
            home.plainClick(tile(grid,"beach.jpg"));
            click(tile(grid,"sunset.jpg"),InputEvent.SHIFT_DOWN_MASK);
            List<String> order=new ArrayList<>();
            for(String id:DesktopChoosing.order(grid.drawn()))for(DesktopHome.Tile one:grid.tiles)if(one.thing.id.equals(id)&&DesktopChoosing.choosable(one.thing.kind))order.add(id);
            int a=order.indexOf(tile(grid,"beach.jpg").thing.id),b=order.indexOf(tile(grid,"sunset.jpg").thing.id);
            assertEquals(order.subList(Math.min(a,b),Math.max(a,b)+1),home.choosing.inOrder(order));
            assertTrue(home.choosing.size()>=2);
            for(String id:home.choosing.chosen())for(DesktopHome.Tile one:grid.tiles)if(one.thing.id.equals(id))assertTrue(home.picked(one));
            assertTrue(home.escape());
            assertTrue(home.choosing.isEmpty());
        });

        // A note among them: Move to bin only, and binned together, with the others on Home staying put.
        SwingUtilities.invokeAndWait(()->{click(tile(grid,"Holiday"),InputEvent.CTRL_DOWN_MASK);click(tile(grid,"boat.jpg"),InputEvent.CTRL_DOWN_MASK);});
        JPopupMenu mixed=onEdt(()->home.menuAt(tile(grid,"boat.jpg")));
        // Move to bin, and Select all under it (2026-11-07).
        assertEquals(2,onEdt(()->{int items=0;for(Component c:mixed.getComponents())if(c instanceof JMenuItem)items++;return items;}).intValue());
        assertNotNull(onEdt(()->item(mixed,Say.t("Select all"))));
        Rectangle sunsetWas=onEdt(()->new Rectangle(tile(grid,"sunset.jpg").getBounds()));
        SwingUtilities.invokeAndWait(()->item(mixed,"Move to bin  (2)").doClick());
        await(()->onEdt(()->"2 moved to the bin".equals(pad.status.getText())));settle();
        assertEquals(2,onEdt(()->pad.store.awayCount(true)).intValue());
        SwingUtilities.invokeAndWait(()->{
            for(DesktopHome.Tile one:grid.tiles){assertNotEquals("Holiday",one.thing.name);assertNotEquals("boat.jpg",one.thing.name);}
            assertEquals("the others stay where they were",sunsetWas,tile(grid,"sunset.jpg").getBounds());
            assertTrue("nothing chosen after",home.choosing.isEmpty());
        });
    }

    /** A left click on an icon, with these keys held, as the mouse gives it. */
    private static void click(DesktopHome.Tile on,int keys) {
        on.dispatchEvent(new MouseEvent(on,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),keys,20,20,1,false,MouseEvent.BUTTON1));
    }
    private static DesktopHome.Tile tile(DesktopHome.Icons grid,String name) {
        for(DesktopHome.Tile one:grid.tiles)if(name.equals(one.thing.name))return one;
        throw new AssertionError("no icon "+name);
    }
    private static JMenuItem item(JPopupMenu menu,String text){for(Component c:menu.getComponents())if(c instanceof JMenuItem i&&text.equals(i.getText()))return i;return null;}
    private void settle() throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var image=new java.awt.image.BufferedImage(Math.max(1,window.getWidth()),Math.max(1,window.getHeight()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();window.paint(g);g.dispose();
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
