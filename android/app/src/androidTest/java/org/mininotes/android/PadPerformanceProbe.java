package org.mininotes.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.app.Instrumentation;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import java.lang.reflect.Field;
import java.util.List;

/** Real Android editor with synthetic words. No node, notebook, permissions or owner's activity is opened. */
public class PadPerformanceProbe extends Instrumentation {
    private Pad pad;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int passed=0;
        try{
            measureOpeningLongNotes();
            testRepeatedFullRestylingDoesNotAccumulateHiddenUnderlineSpans();passed++;
            testIncrementalFormattingMatchesFullParsingAfterKeyboardEdits();passed++;
            testLongNoteTypingAndScrolledDrawing();passed++;
            testMarksStayHiddenInWritersColours();passed++;
            result.putString("stream","Pad editor checks: 4 passed, 0 failed\n");result.putInt("passed",passed);finish(-1,result);
        }catch(Throwable error){result.putInt("passed",passed);result.putString("stream",android.util.Log.getStackTraceString(error));finish(1,result);}
    }
    private void onUi(Runnable action){
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private static void assertEquals(long expected,long actual){if(expected!=actual)throw new AssertionError("Expected "+expected+", got "+actual);}
    private static void assertTrue(String message,boolean value){if(!value)throw new AssertionError(message);}
    private void prepare(String text){onUi(()->{
        pad=new Pad(getTargetContext(),0xffdddddd);
        pad.setLayoutParams(new android.view.ViewGroup.LayoutParams(900,1000));
        pad.put(text,Writers.unknown(text.length()));
        pad.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));
        pad.layout(0,0,900,1000);
    });}
    private void settled(){try{Field field=Pad.class.getDeclaredField("restyling");field.setAccessible(true);((Runnable)field.get(pad)).run();}catch(Exception e){throw new AssertionError(e);}}
    private static int hidden(Editable text){int count=0;for(Object span:text.getSpans(0,text.length(),Object.class))if(span.getClass().getSimpleName().equals("Gone"))count++;return count;}
    private void sameAsFullFormatting(){
        Editable text=pad.getText();List<Marks.Run> expected=Marks.of(text);int count=0;
        for(Object span:text.getSpans(0,text.length(),Object.class)){
            String kind=span.getClass().getSimpleName();
            if(!kind.equals("Bold")&&!kind.equals("Italic")&&!kind.equals("Under"))continue;
            count++;int start=text.getSpanStart(span),end=text.getSpanEnd(span);
            assertTrue("style fits current words at "+start+":"+end,expected.stream().anyMatch(run->run.start()==start&&run.end()==end&&kind.equals(run.style()==Marks.Style.BOLD?"Bold":run.style()==Marks.Style.ITALIC?"Italic":"Under")));
        }
        assertEquals(expected.size(),count);
        assertEquals(expected.size()*2L,hidden(text));
    }
    /** Where the main thread spends a slow step: its stack read every few milliseconds, each frame counted once a read. */
    private static final class Sampler extends Thread {
        private final Thread watched;private volatile boolean done;private int reads;
        private final java.util.Map<String,Integer> seen=new java.util.HashMap<>();
        Sampler(Thread watched){this.watched=watched;setDaemon(true);start();}
        @Override public void run(){
            while(!done){
                java.util.Set<String> once=new java.util.HashSet<>();
                for(StackTraceElement frame:watched.getStackTrace())once.add(frame.getClassName()+"."+frame.getMethodName());
                synchronized(seen){reads++;for(String frame:once)seen.merge(frame,1,Integer::sum);}
                try{Thread.sleep(3);}catch(InterruptedException e){return;}
            }
        }
        void report(){
            done=true;try{join();}catch(InterruptedException ignored){}
            synchronized(seen){
                StringBuilder out=new StringBuilder("samples="+reads);
                seen.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).limit(45).forEach(e->out.append("\n").append(e.getValue()).append(" ").append(e.getKey()));
                for(String line:out.toString().split("\n"))android.util.Log.i("MininotesPadProbe",line);
            }
        }
    }
    /** Opening a note and restyling it whole on a page already laid out, as switching notes does: timed, not asserted. */
    public void measureOpeningLongNotes(){
        String[][] notes={
            {"plain","A plain synthetic line of words with no marks at all in it.\n"},
            {"some","A line with a **bold** word.\nPlain words.\nMore plain words here.\nAnd more.\n"},
            {"dense","__one__ and **two**\n"}};
        for(String[] note:notes){
            String text=note[1].repeat(24000/note[1].length());
            prepare("");
            onUi(()->{
                Sampler opening=null;
                long start=System.nanoTime();pad.put(text,Writers.unknown(text.length()));
                pad.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));pad.layout(0,0,900,1000);
                long opened=(System.nanoTime()-start)/1000000;
                if(opening!=null)opening.report();
                sameAsFullFormatting();
                int middle=text.indexOf(' ',text.length()/2)+1;pad.setSelection(middle,text.indexOf(' ',middle));
                Sampler sampler=null;
                start=System.nanoTime();pad.style(Marks.Style.BOLD);long bolded=(System.nanoTime()-start)/1000000;
                if(sampler!=null)sampler.report();
                sameAsFullFormatting();
                // Letters typed near the top, one at a time, as a keyboard does: each with its own layout, then formatted.
                Editable words=pad.getText();int at=words.toString().indexOf('\n',100);
                Sampler typing=null;
                start=System.nanoTime();for(int i=0;i<20;i++)words.insert(at+i,"y");long typed=(System.nanoTime()-start)/1000000;
                if(typing!=null)typing.report();
                // As a keyboard writes: the word being typed set again whole at each letter, then committed.
                android.view.inputmethod.InputConnection keyboard=pad.onCreateInputConnection(new android.view.inputmethod.EditorInfo());
                pad.setSelection(at);StringBuilder word=new StringBuilder();
                start=System.nanoTime();
                for(int i=0;i<20;i++){
                    if(i%5==4){keyboard.commitText(word+" ",1);word.setLength(0);continue;}
                    word.append('z');keyboard.setComposingText(word,1);
                }
                keyboard.finishComposingText();long composed=(System.nanoTime()-start)/1000000;
                Sampler formatting=note[0].equals("dense")?new Sampler(Thread.currentThread()):null;
                start=System.nanoTime();settled();long formatted=(System.nanoTime()-start)/1000000;
                if(formatting!=null)formatting.report();
                sameAsFullFormatting();
                android.util.Log.i("MininotesPadProbe","note="+note[0]+"; chars="+text.length()+"; runs="+Marks.of(text).size()+"; openMs="+opened+"; boldOneWordMs="+bolded+"; type20NearTopMs="+typed+"; compose20NearTopMs="+composed+"; thenFormatMs="+formatted);
            });
        }
    }
    /** A mark in a note two people wrote is hidden, as in any note, whichever was put on first: colours and marks together. */
    public void testMarksStayHiddenInWritersColours(){
        String text="**bold** words by two people\nand a second line\n";
        for(boolean coloursFirst:new boolean[]{true,false}){
            prepare("");
            onUi(()->{
                Writers two=Writers.of("someone",text.length());two.edit(0,20,20,"somebody else");
                Writers.Palette palette=new Writers.Palette(Tint.NONE,null,null);
                if(coloursFirst)pad.inks(palette,true,0xFFFFFFFF);
                pad.put(text,two);
                if(!coloursFirst)pad.inks(palette,true,0xFFFFFFFF);
                assertTrue("drawn in writers' colours",pad.inked());
                Editable words=pad.getText();words.insert(words.length()-1,"x");settled();
                assertEquals(2,hidden(words));sameAsFullFormatting();
            });
        }
    }
    public void testRepeatedFullRestylingDoesNotAccumulateHiddenUnderlineSpans(){
        // Small: each whole restyle of words already laid out lays them out again, which a long page makes very slow.
        prepare("__one__ and **two**\n".repeat(60));
        onUi(()->{int count=hidden(pad.getText());assertEquals(240,count);for(int i=0;i<50;i++)pad.restyle();assertEquals(count,hidden(pad.getText()));sameAsFullFormatting();});
    }
    public void testIncrementalFormattingMatchesFullParsingAfterKeyboardEdits(){
        prepare("**bold** __under__ *italic*\n".repeat(700));
        onUi(()->{
            Editable text=pad.getText();int at=text.length()-14;
            text.insert(at,"x");settled();sameAsFullFormatting();
            text.insert(at,"\n");settled();sameAsFullFormatting();
            text.delete(at,at+1);settled();sameAsFullFormatting();
            text.replace(0,80,"__paste__\n**new**\n");settled();sameAsFullFormatting();
            text.insert(text.length(),"\n__last__");text.insert(0,"heading\n");settled();sameAsFullFormatting();
            text.clear();settled();sameAsFullFormatting();
        });
    }
    public void testLongNoteTypingAndScrolledDrawing(){
        prepare("A long synthetic line with **bold** and __underlining__.\n".repeat(400));
        onUi(()->{
            Editable text=pad.getText();int hidden=hidden(text);Sampler sampler=null;long start=System.nanoTime();
            for(int i=0;i<100;i++){text.insert(text.length()-1,"x");settled();text.delete(text.length()-2,text.length()-1);settled();}
            long millis=(System.nanoTime()-start)/1000000;
            assertEquals(hidden,hidden(text));sameAsFullFormatting();
            pad.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));pad.layout(0,0,900,1000);
            pad.scrollTo(0,Math.max(0,pad.getLayout().getHeight()-1000));
            Bitmap frame=Bitmap.createBitmap(900,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(frame);
            start=System.nanoTime();for(int i=0;i<30;i++)pad.draw(canvas);long drawing=(System.nanoTime()-start)/1000000;frame.recycle();
            android.util.Log.i("MininotesPadProbe","chars="+text.length()+"; editAndFormat200Ms="+millis+"; bottomDraw30Ms="+drawing+"; hiddenSpans="+hidden(text));
        });
    }
}
