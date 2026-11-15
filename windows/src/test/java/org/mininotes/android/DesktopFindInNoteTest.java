package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Words looked for in the open note (the owner, 2026-11-04: "We need a search in note function ... in the menu and from a right
 * click but also from ctrl-f on desktop"): Ctrl+F opens it over a note, and it goes on into the note's other tabs. Synthetic.
 */
public class DesktopFindInNoteTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;
    @Before public void start() throws Exception {
        Path folder=temp.newFolder().toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();pad.frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    @Test public void findGoesThroughEveryTab() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Trip";
        note.body="=== Plan ===\nTrain to Lyon\nhotel\n=== Packing ===\nsocks\n=== Budget ===\ntrain 40\nhotel 90";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().startsWith("Train"));settle();
        SwingUtilities.invokeAndWait(pad::findInNote);settle();
        assertTrue("the bar is up",pad.findBar.isVisible());
        SwingUtilities.invokeAndWait(()->pad.findBar.typeForTest("train"));settle();
        assertEquals(1,pad.findBar.found());assertEquals(0,pad.tab);
        // On past this tab's only one: Packing has none, so Budget.
        SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(1));settle();
        assertEquals(2,pad.tab);assertEquals(1,pad.findBar.found());assertEquals(0,pad.findBar.inHand());
        assertTrue("the caret on it",pad.page.getText().toLowerCase().startsWith("train",pad.page.getCaretPosition()));
        // And round to the first tab again.
        SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(1));settle();
        assertEquals(0,pad.tab);
        // Back the other way: the last match of the tab before.
        SwingUtilities.invokeAndWait(()->pad.findBar.typeForTest("hotel"));settle();
        SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(-1));settle();
        assertEquals(2,pad.tab);assertEquals(pad.findBar.found()-1,pad.findBar.inHand());
        // Nothing was written by looking.
        assertEquals(note.body,pad.store.get(note.id).body);
    }

    @Test public void aNoteWithoutTabsWrapsInItself() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Plain";note.body="apple pie\napple tart\npear";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().startsWith("apple"));settle();
        SwingUtilities.invokeAndWait(pad::findInNote);SwingUtilities.invokeAndWait(()->pad.findBar.typeForTest("apple"));settle();
        assertEquals(2,pad.findBar.found());
        SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(1));SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(1));settle();
        assertEquals(0,pad.findBar.inHand());
    }

    /** The owner, 2026-11-07: typing beside a match made the caret jump to the next one. Writing with the bar up stays where it is. */
    @Test public void typingWithTheBarUpLeavesTheCaretWhereItIs() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Plain";note.body="apple pie\napple tart\npear";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().startsWith("apple"));settle();
        SwingUtilities.invokeAndWait(pad::findInNote);SwingUtilities.invokeAndWait(()->pad.findBar.typeForTest("apple"));settle();
        // Writing right after the first match, then taking a letter away there, as a person does.
        int after="apple".length();
        SwingUtilities.invokeAndWait(()->{pad.page.setCaretPosition(after);pad.page.replaceSelection("s");});settle();
        assertEquals(after+1,pad.page.getCaretPosition());
        SwingUtilities.invokeAndWait(()->{try{pad.page.getDocument().remove(after,1);}catch(Exception e){throw new RuntimeException(e);}pad.page.setCaretPosition(after);});settle();
        assertEquals(after,pad.page.getCaretPosition());
        assertEquals("still found in both places",2,pad.findBar.found());
        // The arrows still go there.
        SwingUtilities.invokeAndWait(()->pad.findBar.goForTest(1));settle();
        assertEquals(pad.page.getText().indexOf("apple tart"),pad.page.getCaretPosition());
    }

    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(120);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
