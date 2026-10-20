package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/** Formatting invalidation uses current offsets and includes both sides of every changed line break. */
public class ChangedLinesTest {
    @Test public void aKeystrokeInALongNoteOnlyReparsesItsLine() {
        String prefix="Unchanged **bold** and __underline__\n".repeat(600);
        String text=prefix+"Last *line*!\nUntouched tail";
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(prefix.length()+11,0,1);
        int[] window=edits.take(text);assertArrayEquals(new int[]{prefix.length(),prefix.length()+12},window);
        assertEquals(1,Marks.of(text.subSequence(window[0],window[1])).size());assertNull(edits.take(text));
    }
    @Test public void insertingANewlineIncludesBothResultingLines() {
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(5,0,1);
        assertArrayEquals(new int[]{4,13},edits.take("top\na\nbottom!\ntail"));
    }
    @Test public void deletingANewlineIncludesBothJoinedHalves() {
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(5,1,0);
        assertArrayEquals(new int[]{4,12},edits.take("top\nabottom!\ntail"));
    }
    @Test public void laterEditsBeforeAnEarlierEditMoveItsPendingEnd() {
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(10,0,1);edits.changed(0,0,5);
        assertArrayEquals(new int[]{0,18},edits.take("12345abc\ndefghi!jk\ntail"));
    }
    @Test public void deletionAcrossPendingEditsClampsToTheReplacement() {
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(12,0,1);edits.changed(4,12,1);
        assertArrayEquals(new int[]{4,9},edits.take("top\nXlast\ntail"));
    }
    @Test public void emptyNoteAndClearHaveNoStaleRange() {
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(0,10,0);assertArrayEquals(new int[]{0,0},edits.take(""));
        edits.changed(0,0,3);edits.clear();assertNull(edits.take("new"));
    }
    @Test public void pasteIncludingSeveralLinesIncludesTheirWholeFormatting() {
        String text="top\nold **bold**\nnew __underline__\nlast *italic* end\ntail";
        Marks.ChangedLines edits=new Marks.ChangedLines();edits.changed(7,0,35);
        int[] window=edits.take(text);assertEquals(4,window[0]);assertEquals(text.lastIndexOf('\n'),window[1]);
        assertEquals(3,Marks.of(text.substring(window[0],window[1])).size());
    }
    @Test public void repeatedTypingDoesNotScanTheUnchangedPrefix() {
        String text="A quiet line\n".repeat(1800)+"Last word!";
        final int[] reads={0};CharSequence counted=new CharSequence(){
            public int length(){return text.length();}public char charAt(int i){reads[0]++;return text.charAt(i);}
            public CharSequence subSequence(int a,int b){return text.substring(a,b);}
        };
        Marks.ChangedLines edits=new Marks.ChangedLines();
        for(int i=0;i<1000;i++){edits.changed(text.length()-1,1,1);int[] window=edits.take(counted);assertEquals(text.lastIndexOf('\n')+1,window[0]);}
        assertTrue("work depends on line length, not document length: "+reads[0],reads[0]<15000);
    }
}
