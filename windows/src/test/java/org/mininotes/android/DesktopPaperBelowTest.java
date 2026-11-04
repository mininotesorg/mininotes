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

/**
 * The paper under the writing (decision 267; the owner, 2026-10-28: "Make sure we can click and type anywhere in a note"):
 * the page scrolls on past its last line, a click on an empty rule puts the cursor there and what is typed lands on that
 * line, and a click that is not followed by typing leaves the note exactly as it was. Events go to the window under test
 * only, never through the real pointer or keyboard.
 */
public class DesktopPaperBelowTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();pad.frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    private NoteStore.Note opened(String body) throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Paper";note.body=body;pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals(body)&&pad.page.isEditable());settle();
        return note;
    }

    /** A press and release on the page, a few rules under the last line written. */
    private void clickBelow(int rules) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            pad.page.requestFocusInWindow();
            Rectangle last=pad.page.modelToView2D(pad.page.getDocument().getLength()).getBounds();
            int line=pad.page.getFontMetrics(pad.page.getFont()).getHeight();
            int x=last.x+40,y=last.y+last.height+(rules-1)*line+line/2;
            assertTrue("the place clicked is on the page ("+y+" of "+pad.page.getHeight()+")",y<pad.page.getHeight());
            long t=System.currentTimeMillis();
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_PRESSED,t,InputEvent.BUTTON1_DOWN_MASK,x,y,1,false,MouseEvent.BUTTON1));
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_RELEASED,t+60,0,x,y,1,false,MouseEvent.BUTTON1));
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_CLICKED,t+60,0,x,y,1,false,MouseEvent.BUTTON1));
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private void type(String words) throws Exception {
        SwingUtilities.invokeAndWait(()->{for(char c:words.toCharArray())
            pad.page.dispatchEvent(new KeyEvent(pad.page,KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,KeyEvent.VK_UNDEFINED,c));});
    }

    @Test public void aClickBelowTheWritingAndTypingWritesOnThatLine() throws Exception {
        NoteStore.Note note=opened("First line\nSecond line");
        clickBelow(3);
        int[] below={0};SwingUtilities.invokeAndWait(()->below[0]=pad.page.below());
        assertEquals("the cursor is on the third rule under the writing",3,below[0]);
        assertEquals("nothing is made before anything is typed","First line\nSecond line",pad.page.getText());
        type("Lower");
        String wanted="First line\nSecond line\n\n\nLower";
        await(()->pad.page.getText().equals(wanted));
        await(()->wanted.equals(pad.store.get(note.id).body));
        SwingUtilities.invokeAndWait(()->below[0]=pad.page.below());
        assertEquals("and the cursor is in the words again",0,below[0]);
    }

    @Test public void aClickBelowWithNothingTypedLeavesTheNoteAsItWas() throws Exception {
        NoteStore.Note note=opened("Only these words");
        NoteStore.Note was=pad.store.get(note.id);
        clickBelow(4);
        int[] below={0};SwingUtilities.invokeAndWait(()->below[0]=pad.page.below());
        assertEquals(4,below[0]);
        settle();
        // Back up into the words: the lines clicked to are gone, never having been there.
        SwingUtilities.invokeAndWait(()->pad.page.setCaretPosition(3));
        SwingUtilities.invokeAndWait(()->below[0]=pad.page.below());
        assertEquals("the cursor left the paper",0,below[0]);
        SwingUtilities.invokeAndWait(pad::goHome);settle();
        NoteStore.Note now=pad.store.get(note.id);
        assertEquals("the words",was.body,now.body);
        assertEquals("no new revision",was.revision,now.revision);
        assertEquals("not written again",was.updated,now.updated);
    }

    @Test public void backspaceOnAClickedLineMovesUpAndWritesNothing() throws Exception {
        NoteStore.Note note=opened("Words");
        clickBelow(2);
        SwingUtilities.invokeAndWait(()->pad.page.dispatchEvent(new KeyEvent(pad.page,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,KeyEvent.VK_BACK_SPACE,KeyEvent.CHAR_UNDEFINED)));
        int[] below={0};SwingUtilities.invokeAndWait(()->below[0]=pad.page.below());
        assertEquals("a line up",1,below[0]);
        assertEquals("Words",pad.page.getText());
        type("x");
        await(()->pad.page.getText().equals("Words\nx"));
        await(()->"Words\nx".equals(pad.store.get(note.id).body));
    }

    @Test public void theNoteScrollsOnPastItsLastLine() throws Exception {
        StringBuilder many=new StringBuilder();for(int i=1;i<=80;i++)many.append("Line ").append(i).append('\n');
        String body=many.toString().trim();
        opened(body);
        int[] seen=new int[3];
        SwingUtilities.invokeAndWait(()->{try{
            JViewport port=pad.paperScroll.getViewport();
            int line=pad.page.getFontMetrics(pad.page.getFont()).getHeight();
            Rectangle last=pad.page.modelToView2D(pad.page.getDocument().getLength()).getBounds();
            seen[0]=pad.page.getHeight()-(last.y+last.height);
            seen[1]=port.getHeight()-3*line;
            // Scrolled to the very end: the last line written is near the top, paper under it.
            port.setViewPosition(new Point(0,pad.page.getHeight()-port.getHeight()));
            seen[2]=last.y-port.getViewPosition().y;
        }catch(Exception e){throw new RuntimeException(e);}});
        assertTrue("about a screen of paper under the last line ("+seen[0]+" for a view of "+seen[1]+")",seen[0]>=seen[1]);
        assertTrue("at the end, the last line is still in view, near the top ("+seen[2]+")",seen[2]>=0&&seen[2]<pad.paperScroll.getViewport().getHeight()/3);
        // And a click down there writes there.
        clickBelow(5);
        type("Under");
        await(()->pad.page.getText().equals(body+"\n\n\n\n\nUnder"));
    }

    @Test public void aPageOnlyReadHasNoPaperToWriteOn() throws Exception {
        opened("Read only words");
        SwingUtilities.invokeAndWait(()->pad.page.setEditable(false));
        int[] seen=new int[2];
        SwingUtilities.invokeAndWait(()->{try{
            pad.page.reach(new Point(60,pad.page.getHeight()-5));
            seen[0]=pad.page.below();
            seen[1]=pad.page.getPreferredSize().height-pad.page.getUI().getPreferredSize(pad.page).height;
        }catch(Exception e){throw new RuntimeException(e);}});
        assertEquals("no cursor on paper that cannot be written on",0,seen[0]);
        assertEquals("and no paper added below",0,seen[1]);
        assertEquals("Read only words",pad.page.getText());
    }

    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(150);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
