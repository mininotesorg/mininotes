package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.Point;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** The owner, 2026-10-10: typing on the PC moved the whole rest of the text. The words below the caret stay where they are. */
public class DesktopTypingStaysTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;private Path folder;
    @Before public void start() throws Exception {
        folder=temp.newFolder().toPath();SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();pad.frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    @Test public void savingWordsComingAndGoingNeverMoveThePage() throws Exception {
        StringBuilder body=new StringBuilder();for(int i=1;i<=80;i++)body.append("Synthetic line ").append(i).append(": words to read.\n");
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Typing stays";note.body=body.toString();pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals(note.body));settle();
        int at=note.body.indexOf("Synthetic line 40:"),below=note.body.indexOf("Synthetic line 44:");
        SwingUtilities.invokeAndWait(()->{pad.page.requestFocusInWindow();pad.page.setCaretPosition(at);
            try{pad.paperScroll.getViewport().setViewPosition(new Point(0,(int)pad.page.modelToView2D(at).getY()-200));}catch(Exception e){throw new RuntimeException(e);}});
        settle();
        int before=onScreen(below+1);
        assertFalse("nothing said before typing",pad.noteWords.isVisible());
        SwingUtilities.invokeAndWait(()->pad.page.replaceSelection("x"));
        await(()->pad.noteWords.isVisible());SwingUtilities.invokeAndWait(()->{});
        assertEquals("the words below stay put while it says Saving",before,onScreen(below+1));
        await(()->!pad.noteWords.isVisible()&&pad.store.get(note.id).body.contains("xSynthetic line 40:"));settle();
        assertEquals("and stay put once it has stopped saying it",before,onScreen(below+1));
    }

    /**
     * Typing, newlines and deletions all over a note full of marks: the lines after the paragraph written in are drawn
     * exactly where they were, and every letter is still drawn and found where it is (FlushMarks, decision 118).
     */
    @Test public void typingAmongMarksNeverMovesOrLosesTheLinesBelow() throws Exception {
        StringBuilder body=new StringBuilder();
        for(int i=1;i<=60;i++)body.append(i%3==0?"A __long underlined stretch__ that wraps across the width of the page, then **bold words** and *slanted ones* and more plain words to wrap "+i+"\n"
            :i%3==1?"Plain line "+i+" with __under__ and **bold**\n":"Short "+i+" mininotes.org\n");
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Typing among marks";note.body=body.toString();pad.store.save(note);
        SwingUtilities.invokeAndWait(()->pad.open(note.id));await(()->pad.page.getText().equals(note.body));settle();
        java.util.Random random=new java.util.Random(7);
        String[] typed={"x","__","*","**","\n","a b ","_"};
        for(int round=0;round<200;round++) {
            final int r=round;
            final Object[] before=new Object[6];
            SwingUtilities.invokeAndWait(()->{try{
                String text=pad.page.getText();
                int at=random.nextInt(text.length()/2)+text.length()/4;
                int lineEnd=text.indexOf('\n',at);if(lineEnd<0)lineEnd=text.length();
                // Somewhere after the paragraph about to change, and where it and that paragraph's end are drawn now.
                int probe=text.indexOf('\n',Math.min(text.length()-1,lineEnd+1));probe=probe<0||probe+1>=text.length()?-1:probe+1;
                before[0]=text;before[1]=at;before[2]=lineEnd;before[3]=probe;
                before[4]=probe<0?null:pad.page.modelToView2D(probe).getBounds();before[5]=pad.page.modelToView2D(lineEnd).getBounds();
                if(r%4==3&&at<text.length()-2&&text.charAt(at)!='\n')pad.page.getDocument().remove(at,1);
                else pad.page.getDocument().insertString(at,typed[r%typed.length],null);
            }catch(javax.swing.text.BadLocationException e){throw new RuntimeException(e);}});
            // As the screen would be drawn: once whatever the change set going has run.
            for(int n=0;n<3;n++)SwingUtilities.invokeAndWait(()->{});
            SwingUtilities.invokeAndWait(()->{try{
                String text=(String)before[0],now=pad.page.getText();int at=(Integer)before[1],lineEnd=(Integer)before[2],probe=(Integer)before[3];
                int shift=now.length()-text.length();
                if(probe>=0) {
                    Rectangle was=(Rectangle)before[4],endWas=(Rectangle)before[5];
                    Rectangle is=pad.page.modelToView2D(probe+shift).getBounds(),endIs=pad.page.modelToView2D(lineEnd+shift).getBounds();
                    if(endIs.y-endWas.y!=is.y-was.y) {
                        int from=text.lastIndexOf('\n',Math.max(0,at-1))+1;
                        String paragraph=now.substring(from,Math.min(now.length(),lineEnd+shift)).replace('\n','|');
                        String wasParagraph=text.substring(from,lineEnd).replace('\n','|');
                        Rectangle start=pad.page.modelToView2D(from).getBounds();
                        pad.page.setText(now);pad.page.getParent().validate();
                        Rectangle fresh=pad.page.modelToView2D(probe+shift).getBounds(),freshEnd=pad.page.modelToView2D(lineEnd+shift).getBounds(),freshStart=pad.page.modelToView2D(from).getBounds();
                        fail("round "+r+": next line was y="+was.y+" now "+is.y+"; paragraph end was y="+endWas.y+" now "+endIs.y+"; start y="+start.y
                            +"; laid out afresh: start "+freshStart.y+" end "+freshEnd.y+" next "+fresh.y+"; edit at "+at+" typed ["+(shift<0?"(deleted)":now.substring(at,at+shift).replace('\n','|'))
                            +"]; paragraph was ["+wasParagraph+"] now ["+paragraph+"]");
                    }
                    assertEquals("round "+r+": and stays at its left edge",was.x,is.x);
                }
                // Every letter on the screen is found again at itself.
                for(int i=0;i<now.length();i+=37) {
                    Rectangle letter=pad.page.modelToView2D(i).getBounds();
                    int found=pad.page.viewToModel2D(new Point(letter.x+1,letter.y+letter.height/2));
                    if(Math.abs(found-i)>3) {
                        Rectangle second=pad.page.modelToView2D(found).getBounds();
                        String around=now.substring(Math.max(0,i-30),Math.min(now.length(),i+30)).replace('\n','|');
                        String there=now.substring(Math.max(0,found-30),Math.min(now.length(),found+30)).replace('\n','|');
                        // The same words laid out afresh: is it the change that left the page wrong, or the layout itself?
                        pad.page.setText(now);pad.page.getParent().validate();
                        Rectangle fresh=pad.page.modelToView2D(i).getBounds();int freshFound=pad.page.viewToModel2D(new Point(fresh.x+1,fresh.y+fresh.height/2));
                        fail("round "+r+": letter "+i+" drawn at "+letter+" is found at "+found+" (drawn at "+second+"); around ["+around+"] there ["+there+"]; laid out afresh: drawn at "+fresh+" found "+freshFound+"; last edit at "+at);
                    }
                }
            }catch(javax.swing.text.BadLocationException e){throw new RuntimeException(e);}});
        }
    }
    /** How many rows the page has drawn down to a place. */
    private int lines(int offset) throws javax.swing.text.BadLocationException {
        Rectangle at=pad.page.modelToView2D(Math.min(offset,pad.page.getDocument().getLength())).getBounds();
        return at.y/pad.page.getFontMetrics(pad.page.getFont()).getHeight();
    }

    private String diag() throws Exception {String[] o={""};SwingUtilities.invokeAndWait(()->{Container row=pad.noteWords.getParent();
        o[0]="words="+pad.noteWords.isVisible()+"/"+pad.noteWords.getText()+" rowH="+(row==null?-1:row.getHeight())+" rowParent="+(row==null?"":row.getParent().getClass().getName()+":"+row.getParent().getHeight())
            +" scrollY="+SwingUtilities.convertPoint(pad.paperScroll,0,0,pad.frame.getRootPane()).y+" view="+pad.paperScroll.getViewport().getViewPosition().y+" frameH="+pad.frame.getHeight();});return o[0];}
    /** Where a letter is drawn on the screen, from the top of the window. */
    private int onScreen(int offset) throws Exception {
        int[] y={0};
        SwingUtilities.invokeAndWait(()->{try{
            Rectangle letter=pad.page.modelToView2D(offset).getBounds();
            y[0]=SwingUtilities.convertPoint(pad.page,letter.getLocation(),pad.frame.getRootPane()).y;
        }catch(Exception e){throw new RuntimeException(e);}});
        return y[0];
    }
    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(120);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
