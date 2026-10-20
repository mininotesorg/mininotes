package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** A note's tabs on the PC (decision 142): the page holds one tab, and the note is always written down whole. */
public class DesktopTabsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;private Path folder;
    @Before public void start() throws Exception {
        folder=temp.newFolder().toPath();SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();pad.frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    @Test public void theOpenTabIsOnThePageAndTheNoteIsWrittenWhole() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Weekly groceries";
        note.body="=== List ===\nBread\nTomatoes\n=== Ideas ===\nSoup on Sunday\n=== Receipts ===\nNone yet";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals("Bread\nTomatoes"));settle();
        assertEquals(0,pad.tab);
        shoot("tabs-header");
        // Looking at other tabs writes nothing.
        long revision=pad.store.get(note.id).revision;
        SwingUtilities.invokeAndWait(()->pad.switchTab(1));settle();
        assertEquals("Soup on Sunday",pad.page.getText());
        SwingUtilities.invokeAndWait(()->pad.switchTab(2));SwingUtilities.invokeAndWait(()->pad.save(null));settle();
        assertEquals(revision,pad.store.get(note.id).revision);
        // Typing in one tab changes that tab and no other letter.
        SwingUtilities.invokeAndWait(()->{pad.switchTab(1);pad.page.setCaretPosition(pad.page.getDocument().getLength());pad.page.replaceSelection("\nand bread");});
        SwingUtilities.invokeAndWait(()->pad.save(null));
        await(()->pad.store.get(note.id).body.contains("and bread"));settle();
        assertEquals("=== List ===\nBread\nTomatoes\n=== Ideas ===\nSoup on Sunday\nand bread\n=== Receipts ===\nNone yet",pad.store.get(note.id).body);
        // The note opens again on the tab it was left on.
        SwingUtilities.invokeAndWait(pad::goHome);settle();SwingUtilities.invokeAndWait(()->pad.open(note.id));settle();
        assertEquals("Soup on Sunday\nand bread",pad.page.getText());
    }

    @Test public void tabsAreAddedRenamedAndRemovedAsWriting() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Plain";note.body="Just words";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals("Just words"));settle();
        assertFalse(Sections.tabbed(pad.noteNow()));
        shoot("tabs-plain-note");
        // Added: the words become Page 1, the new tab is open and empty. (The name box is not shown in this test: see below.)
        String added=Sections.add("Just words",null);
        SwingUtilities.invokeAndWait(()->{pad.page.setText(pad.page.getText());});
        SwingUtilities.invokeAndWait(()->pad.changeTabsForTest("Just words",added,1));
        await(()->pad.store.get(note.id).body.equals("=== Page 1 ===\nJust words\n=== Page 2 ==="));settle();
        assertEquals("",pad.page.getText());assertEquals(1,pad.tab);
        // Words typed into the empty last tab go under its line.
        SwingUtilities.invokeAndWait(()->{pad.page.replaceSelection("Second");pad.save(null);});
        await(()->pad.store.get(note.id).body.equals("=== Page 1 ===\nJust words\n=== Page 2 ===\nSecond"));settle();
        // Renamed, then the first removed: the second is all there is, open.
        String renamed=Sections.rename(pad.noteNow(),1,"Ideas");
        SwingUtilities.invokeAndWait(()->pad.changeTabsForTest(pad.noteNow(),renamed,1));
        await(()->pad.store.get(note.id).body.equals("=== Page 1 ===\nJust words\n=== Ideas ===\nSecond"));settle();
        String removed=Sections.remove(pad.noteNow(),0);
        SwingUtilities.invokeAndWait(()->pad.changeTabsForTest(pad.noteNow(),removed,0));
        await(()->pad.store.get(note.id).body.equals("=== Ideas ===\nSecond"));settle();
        assertEquals("Second",pad.page.getText());assertEquals(0,pad.tab);
        shoot("tabs-one-named");
    }

    @Test public void writingThatArrivesKeepsTheSameTabOpenAndTheWordsTypedThere() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Shared";
        note.body="=== A ===\nfirst\n=== B ===\nsecond";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals("first"));settle();
        SwingUtilities.invokeAndWait(()->pad.switchTab(1));settle();
        // Somebody else adds a tab before this one and writes in A.
        NoteStore.Note other=pad.store.get(note.id);other.body="=== New ===\nnew\n=== A ===\nfirst, edited\n=== B ===\nsecond";other.revision++;pad.store.save(other);
        SwingUtilities.invokeAndWait(()->pad.followForTest(note.id));
        await(()->pad.noteNow().contains("first, edited"));settle();
        assertEquals("second",pad.page.getText());assertEquals(2,pad.tab);
    }

    private void shoot(String name) throws Exception {
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(()->{try{
            JRootPane root=pad.frame.getRootPane();BufferedImage shot=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=shot.createGraphics();root.paint(g);g.dispose();
            Path out=Paths.get("build","verification");Files.createDirectories(out);javax.imageio.ImageIO.write(shot,"png",out.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(120);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
