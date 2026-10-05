package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * Every card's ⋯ on the PC (docs/HOME.md, decision 98; the owner, 2026-10-04: "the Temp folder and all other folders should
 * have the 3 dots menu for contextual settings, just as the right click"): a folder's card at any depth and a place's card
 * each have one at the right of their bar, and it opens the very menu a right-click on that folder or place opens. A
 * synthetic notebook, offline; a picture saved beside the gallery's.
 */
public class DesktopCardMenuTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void everyCardHasTheMenuItsRightClickOpens() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            String kitchen=pad.store.addCollection("Kitchen").id,recipes=pad.store.addBook(kitchen,"Recipes").id;
            settle(pad);
            // A folder on Home, and one inside it: each its own menu, as a right-click on it.
            for(String id:new String[]{kitchen,recipes}) {
                SwingUtilities.invokeAndWait(()->pad.openCollection(id));settle(pad);
                String in=id.equals(kitchen)?Things.HOME:kitchen;
                NoteStore.Branch it=onEdt(()->{for(NoteStore.Branch one:pad.store.contents(in))if(one.id.equals(id))return one;return null;});
                assertNotNull(it);
                assertEquals(id,items(()->pad.thingMenu(it)),opened(pad));
            }
            shoot(pad.frame,"98b-folder-card-menu");
            // Temp's card: its menu on Home, Also show on Home in it; and the archive's and the bin's, theirs.
            for(NoteStore.Branch place:new NoteStore.Branch[]{DesktopHome.tempPlace(0),DesktopHome.archivePlace(0),DesktopHome.binPlace(0),DesktopHome.sharedPlace()}) {
                SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(place);});settle(pad);
                List<String> got=opened(pad);
                assertEquals(place.name,items(()->pad.home.menuFor(place)),got);
                if(place.kind==NoteStore.Branch.Kind.TEMP){assertTrue(got.toString(),got.contains("Also show on Home"));shoot(pad.frame,"98-temp-card-menu");}
                if(place.kind==NoteStore.Branch.Kind.BIN)assertTrue(got.toString(),got.contains("Empty the bin…"));
            }
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    /** The card's ⋯ found in its bar and clicked: the words of the menu it opened, which is then closed. */
    private static List<String> opened(Desktop pad) throws Exception {
        return onEdt(()->{
            JButton more=menuButton(pad.home.folder.head);
            assertNotNull("the card has its ⋯",more);assertTrue(more.isShowing());assertEquals("Menu",more.getToolTipText());
            more.doClick(0);
            MenuElement[] path=MenuSelectionManager.defaultManager().getSelectedPath();
            assertTrue("a menu opened",path.length>0&&path[0] instanceof JPopupMenu);
            JPopupMenu menu=(JPopupMenu)path[0];
            List<String> words=words(menu);menu.setVisible(false);MenuSelectionManager.defaultManager().clearSelectedPath();
            return words;
        });
    }
    private static List<String> items(Callable<JPopupMenu> menu) throws Exception {return onEdt(()->words(menu.call()));}
    private static List<String> words(JPopupMenu menu) {
        List<String> out=new ArrayList<>();
        for(Component c:menu.getComponents())if(c instanceof JMenuItem item)out.add(item.getText());
        return out;
    }
    private static JButton menuButton(Container in) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&"Menu".equals(b.getAccessibleContext().getAccessibleName()))return b;
            if(c instanceof Container k){JButton f=menuButton(k);if(f!=null)return f;}}
        return null;
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
