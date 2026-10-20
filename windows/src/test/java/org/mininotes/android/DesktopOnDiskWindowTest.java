package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * A note kept as a file, in the window (decision 162), on a made-up note in a temporary folder: its menu lines, the file
 * changed outside and the page following, a word typed and the file following, the file taken away and the line that says
 * so, and the marks on Home. Drawn to pictures in build/verification/gallery to be looked at. Offline.
 */
public class DesktopOnDiskWindowTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void aNoteKeptAsAFileInTheWindow() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath(),outside=temp.newFolder("Notes").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();app[0].frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Shopping";note.body="Bread\n__Milk__";pad.store.save(note);
            NoteStore.Shelf trips=pad.store.addCollectionIn(Things.HOME,"Trips");
            SwingUtilities.invokeAndWait(pad::refresh);
            // Not kept yet: its menu offers to keep it, and a folder's to keep it on disk.
            List<String> before=lines(pad,note.id,"162a-note-menu-keep-as-a-file");
            assertTrue(before.toString(),before.contains("Save on my device…"));
            assertFalse(before.contains("Show in Explorer"));
            assertTrue(lines(pad,trips.id,"162b-folder-menu-keep-on-disk").contains("Save on my device…"));
            // Kept, as Save on my device… does once the folder is chosen.
            CountDownLatch kept=new CountDownLatch(1);
            SwingUtilities.invokeAndWait(()->pad.disk.submit(()->pad.onDisk.keepNote(note.id,outside),l->{pad.refresh();kept.countDown();},e->{throw new RuntimeException(e);}));
            assertTrue(kept.await(30,TimeUnit.SECONDS));
            Path file=outside.resolve("Shopping.md");
            assertEquals("---\nmininotes: "+note.id+"\n---\nBread\n<u>Milk</u>",Files.readString(file,StandardCharsets.UTF_8));
            List<String> after=lines(pad,note.id,"162c-note-menu-kept");
            assertTrue(after.toString(),after.containsAll(List.of("Show in Explorer","Keep it inside only")));
            assertFalse(after.contains("Save on my device…"));assertFalse("not lost, so nothing to find",after.contains("Find it…"));
            // Open, then changed in Notepad: the page follows by itself, and the bar says where from.
            SwingUtilities.invokeAndWait(()->pad.open(note.id));
            await(()->note.id.equals(pad.noteShown())&&pad.page.isEditable());
            long was=Files.getLastModifiedTime(file).toMillis();
            Files.writeString(file,"---\nmininotes: "+note.id+"\n---\nBread\n<u>Milk</u>\nCheese from Notepad",StandardCharsets.UTF_8);
            Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.fromMillis(was+2000));
            await(()->pad.noteNow().equals("Bread\n__Milk__\nCheese from Notepad"));
            await(()->pad.status.getText().equals("Taken in from Shopping.md"));
            shootWindow(pad,"162d-taken-in-from-the-file");
            // Typed here: written to the file once it is saved.
            SwingUtilities.invokeAndWait(()->{pad.page.setCaretPosition(pad.page.getDocument().getLength());pad.page.replaceSelection("\nTyped in Mininotes");});
            await(()->Files.readString(file,StandardCharsets.UTF_8).endsWith("Cheese from Notepad\nTyped in Mininotes"));
            // Taken away: the note says so on a line of its own, with the way to find it and the way out.
            Files.delete(file);
            await(()->pad.lostStrip.said()!=null);
            assertEquals("Shopping.md is no longer in Notes",pad.lostStrip.said());
            assertEquals(DesktopOnDisk.GONE,pad.onDisk.state(note.id));
            assertTrue(lines(pad,note.id,"162e-note-menu-lost").contains("Find it…"));
            shootWindow(pad,"162f-lost-line-on-the-note");
            // On Home, its icon wears the warning; a note kept and well wears the file.
            NoteStore.Note other=new NoteStore.Note();other.book=Things.HOME;other.title="Recipes";other.body="Soup";pad.store.save(other);
            CountDownLatch second=new CountDownLatch(1);
            SwingUtilities.invokeAndWait(()->pad.disk.submit(()->pad.onDisk.keepNote(other.id,outside),l->{pad.refresh();second.countDown();},e->{throw new RuntimeException(e);}));
            assertTrue(second.await(30,TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(pad::showHome);
            Thread.sleep(800);
            shootWindow(pad,"162g-home-marks");
            // Found again where it was put back: the line goes by itself.
            Files.writeString(file,"---\nmininotes: "+note.id+"\n---\nBread\n<u>Milk</u>\nCheese from Notepad\nTyped in Mininotes",StandardCharsets.UTF_8);
            SwingUtilities.invokeAndWait(()->pad.open(note.id));
            await(()->"".equals(pad.onDisk.state(note.id))&&pad.lostStrip.said()==null);
            // And in French, the line and the menu (decision 160).
            Files.delete(file);
            await(()->pad.lostStrip.said()!=null);
            Say.use("fr");
            assertEquals("Shopping.md n'est plus dans Notes",DesktopOnDisk.lostWords(DesktopOnDisk.GONE,"Shopping.md",file));
            assertTrue(lines(pad,note.id,"162h-note-menu-lost-french").contains("Le retrouver…"));
        } finally {Say.use("en");SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    /** The words of a thing's menu lines, as its icon's right-click shows them; the menu drawn to a picture. */
    private static List<String> lines(Desktop pad,String id,String shot) throws Exception {
        List<String> out=new ArrayList<>();
        SwingUtilities.invokeAndWait(()->{
            NoteStore.Branch it=null;for(NoteStore.Branch one:pad.store.contents(Things.HOME))if(one.id.equals(id))it=one;
            assertNotNull(it);
            JPopupMenu menu=pad.thingMenu(it);
            for(Component c:menu.getComponents())if(c instanceof JMenuItem m&&!(m instanceof JMenu))out.add(m.getText());
            Dimension size=menu.getPreferredSize();menu.setSize(size);layout(menu);
            BufferedImage img=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,size.width,size.height);menu.printAll(g);g.dispose();
            try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve(shot+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
        });
        return out;
    }
    private static void shootWindow(Desktop pad,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JRootPane root=pad.frame.getRootPane();root.validate();
            BufferedImage img=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();root.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component one:c.getComponents())if(one instanceof Container k)layout(k);}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
