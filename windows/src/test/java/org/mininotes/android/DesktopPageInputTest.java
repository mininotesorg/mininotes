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
 * The page takes the mouse and the keyboard as any text does (the owner, 2026-10-11: "not able to save a note; not able to
 * select text with the mouse; Ctrl+A should select the whole text"). Events are given to the window under test only, never
 * through the real pointer or keyboard, so nothing else on this PC is touched.
 */
public class DesktopPageInputTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();pad.frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    @Test public void theMouseSelectsCtrlASelectsAllAndTypingIsSaved() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Input";note.body="First line of words\nSecond line of words";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals(note.body)&&pad.page.isEditable());settle();
        StringBuilder said=new StringBuilder();
        SwingUtilities.invokeAndWait(()->{
            said.append("editable=").append(pad.page.isEditable()).append(" enabled=").append(pad.page.isEnabled()).append(" showing=").append(pad.page.isShowing());
            // What is under a point in the middle of the first line: the page, or something over it?
            try {
                Rectangle at=pad.page.modelToView2D(5).getBounds();
                Point onFrame=SwingUtilities.convertPoint(pad.page,at.x+2,at.y+at.height/2,pad.frame.getLayeredPane());
                Component top=SwingUtilities.getDeepestComponentAt(pad.frame.getLayeredPane(),onFrame.x,onFrame.y);
                said.append(" under=").append(top==null?"null":top.getClass().getName()+(top.getName()==null?"":"#"+top.getName()));
            } catch(Exception e){said.append(" "+e);}
        });
        System.out.println("PAGE "+said);
        // A press at the start of the first line, dragged to its end: the words between are selected.
        SwingUtilities.invokeAndWait(()->{try{
            Rectangle a=pad.page.modelToView2D(0).getBounds(),b=pad.page.modelToView2D(10).getBounds();
            long t=System.currentTimeMillis();
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_PRESSED,t,InputEvent.BUTTON1_DOWN_MASK,a.x+1,a.y+a.height/2,1,false,MouseEvent.BUTTON1));
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_DRAGGED,t+50,InputEvent.BUTTON1_DOWN_MASK,b.x+1,b.y+b.height/2,1,false,MouseEvent.NOBUTTON));
            pad.page.dispatchEvent(new MouseEvent(pad.page,MouseEvent.MOUSE_RELEASED,t+100,0,b.x+1,b.y+b.height/2,1,false,MouseEvent.BUTTON1));
        }catch(Exception e){throw new RuntimeException(e);}});
        String[] selected={null};SwingUtilities.invokeAndWait(()->selected[0]=pad.page.getSelectedText());
        assertEquals("the mouse selects ("+said+")","First line",selected[0]);
        // Ctrl+A: all of it.
        SwingUtilities.invokeAndWait(()->{pad.page.requestFocusInWindow();
            pad.page.dispatchEvent(new KeyEvent(pad.page,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),InputEvent.CTRL_DOWN_MASK,KeyEvent.VK_A,'a'));});
        SwingUtilities.invokeAndWait(()->selected[0]=pad.page.getSelectedText());
        assertEquals("Ctrl+A selects the whole note",note.body,selected[0]);
        // Typed, as keys are: at the end, then written down.
        SwingUtilities.invokeAndWait(()->{pad.page.setCaretPosition(pad.page.getDocument().getLength());
            pad.page.dispatchEvent(new KeyEvent(pad.page,KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,KeyEvent.VK_UNDEFINED,'!'));});
        await(()->pad.page.getText().endsWith("!"));
        await(()->pad.store.get(note.id).body.endsWith("!"));
    }

    /** The same on a note made from the Temp card's + (the owner's way): nothing of the card left over the page. */
    @Test public void aNoteMadeFromTheTempCardTakesTheMouseAndKeys() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});settle();
        String before=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->((JMenuItem)pad.home.plusMenu(NoteStore.TEMP).getComponent(0)).doClick());
        await(()->!pad.store.latest().id.equals(before)&&pad.page.isEditable()&&pad.page.isShowing());settle();
        String id=pad.store.latest().id;
        String[] under={null};
        SwingUtilities.invokeAndWait(()->{
            Rectangle area=pad.page.getVisibleRect();
            Point onFrame=SwingUtilities.convertPoint(pad.page,area.x+60,area.y+20,pad.frame.getLayeredPane());
            Component top=SwingUtilities.getDeepestComponentAt(pad.frame.getLayeredPane(),onFrame.x,onFrame.y);
            under[0]=top==null?"null":top.getClass().getName();
        });
        assertTrue("the page is what is under the pointer, not "+under[0],under[0].contains("Paper"));
        SwingUtilities.invokeAndWait(()->{pad.page.requestFocusInWindow();
            for(char c:"Temp words".toCharArray())pad.page.dispatchEvent(new KeyEvent(pad.page,KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,KeyEvent.VK_UNDEFINED,c));});
        await(()->pad.page.getText().equals("Temp words"));
        SwingUtilities.invokeAndWait(pad::goHome);
        await(()->"Temp words".equals(pad.store.get(id).body));
        assertTrue("still temporary",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
    }

    /** A selection can be seen: drawn in a colour that stands out from the page (the owner, 2026-10-11, twice). */
    @Test public void aSelectionIsSeenOnThePage() throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Seen";note.body="Words to be selected and seen"+(char)10+"And more";pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals(note.body));settle();
        int[] counts=new int[2];String[] said={""};
        SwingUtilities.invokeAndWait(()->{
            pad.page.requestFocusInWindow();
            java.awt.image.BufferedImage plain=shot();
            pad.page.select(0,20);
            java.awt.image.BufferedImage chosen=shot();
            Color sel=pad.page.getSelectionColor(),bg=pad.page.getBackground();
            said[0]="selection="+sel+" background="+bg+" caretVisible="+pad.page.getCaret().isSelectionVisible()+" highlights="+pad.page.getHighlighter().getHighlights().length;
            for(int x=0;x<plain.getWidth();x++)for(int y=0;y<plain.getHeight();y++)if(plain.getRGB(x,y)!=chosen.getRGB(x,y))counts[0]++;
        });
        System.out.println("SEEN "+said[0]+" changed="+counts[0]);
        assertTrue("selecting changes what is drawn ("+said[0]+")",counts[0]>200);
    }
    private java.awt.image.BufferedImage shot(){
        java.awt.image.BufferedImage img=new java.awt.image.BufferedImage(pad.page.getWidth(),Math.min(pad.page.getHeight(),200),java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();pad.page.paint(g);g.dispose();return img;
    }

    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(150);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
