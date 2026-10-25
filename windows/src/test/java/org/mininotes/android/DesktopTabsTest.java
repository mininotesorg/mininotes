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

    // Tabs reordered (the owner, 2026-10-24: "We have to be able to drag and reorder the tabs in a note", decision 239).
    @Test public void aTabMovesByItsMenuAndByBeingCarriedAndTheNoteIsWrittenInTheNewOrder() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Ordered";
        note.body="=== A ===\na\n=== B ===\nb\n=== C ===\nc";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals("a"));settle();
        // Move right, from the menu, on the open tab: it goes one place on and stays open.
        SwingUtilities.invokeAndWait(()->pad.moveTabForTest(0,1));
        await(()->pad.store.get(note.id).body.equals("=== B ===\nb\n=== A ===\na\n=== C ===\nc"));settle();
        assertEquals(1,pad.tab);assertEquals("a",pad.page.getText());
        // Move left on a tab that is not open: the open one stays open where its words went.
        SwingUtilities.invokeAndWait(()->pad.moveTabForTest(2,1));
        await(()->pad.store.get(note.id).body.equals("=== B ===\nb\n=== C ===\nc\n=== A ===\na"));settle();
        assertEquals(2,pad.tab);assertEquals("a",pad.page.getText());
        // Carried: the first card pressed, pulled past the last, let go. It is last, and open.
        SwingUtilities.invokeAndWait(()->{
            java.util.List<JLabel> cards=cards();JLabel first=cards.get(0),last=cards.get(cards.size()-1);
            java.awt.Point over=SwingUtilities.convertPoint(last,last.getWidth()-2,last.getHeight()/2,first);
            int y=first.getHeight()/2;long now=System.currentTimeMillis();
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_PRESSED,now,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,5,y,1,false,java.awt.event.MouseEvent.BUTTON1));
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_DRAGGED,now+20,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,30,y,0,false,java.awt.event.MouseEvent.NOBUTTON));
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_DRAGGED,now+40,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,over.x,y,0,false,java.awt.event.MouseEvent.NOBUTTON));
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_RELEASED,now+60,0,over.x,y,1,false,java.awt.event.MouseEvent.BUTTON1));
        });
        await(()->pad.store.get(note.id).body.equals("=== C ===\nc\n=== A ===\na\n=== B ===\nb"));settle();
        assertEquals(2,pad.tab);assertEquals("b",pad.page.getText());
        // Carried and put back with Esc: nothing changes, and letting go after is not a click.
        long revision=pad.store.get(note.id).revision;
        SwingUtilities.invokeAndWait(()->{
            JLabel first=cards().get(0);int y=first.getHeight()/2;long now=System.currentTimeMillis();
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_PRESSED,now,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,5,y,1,false,java.awt.event.MouseEvent.BUTTON1));
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_DRAGGED,now+20,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,first.getWidth()+80,y,0,false,java.awt.event.MouseEvent.NOBUTTON));
            // Through the focus manager, where Esc is caught while a tab is carried.
            pad.page.dispatchEvent(new java.awt.event.KeyEvent(pad.page,java.awt.event.KeyEvent.KEY_PRESSED,now+40,0,java.awt.event.KeyEvent.VK_ESCAPE,java.awt.event.KeyEvent.CHAR_UNDEFINED));
            first.dispatchEvent(new java.awt.event.MouseEvent(first,java.awt.event.MouseEvent.MOUSE_RELEASED,now+60,0,first.getWidth()+80,y,1,false,java.awt.event.MouseEvent.BUTTON1));
        });
        settle();
        assertEquals(revision,pad.store.get(note.id).revision);assertEquals(2,pad.tab);
        assertEquals("=== C ===\nc\n=== A ===\na\n=== B ===\nb",pad.noteNow());
        shoot("tabs-moved");
    }

    private java.util.List<JLabel> cards() {
        java.util.List<JLabel> out=new java.util.ArrayList<>();
        for(Component c:pad.tabStrip.getComponents())if(c instanceof JLabel l)out.add(l);
        return out;
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
