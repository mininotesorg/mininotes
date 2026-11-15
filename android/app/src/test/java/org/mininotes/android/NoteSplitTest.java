package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/** Writing cut into notes that each fit (decision 328): at line ends, every letter kept, never half an emoji. */
public class NoteSplitTest {
    private static String joined(List<NoteSplit.Piece> pieces){StringBuilder all=new StringBuilder();for(NoteSplit.Piece p:pieces)all.append(p.body);return all.toString();}

    @Test public void writingThatFitsIsOnePiece() {
        List<NoteSplit.Piece> pieces=NoteSplit.pieces("Plan","short");
        assertEquals(1,pieces.size());assertEquals("Plan",pieces.get(0).title);assertEquals("short",pieces.get(0).body);
    }
    @Test public void tooLongIsCutAtALineEndAndNothingIsLost() {
        String body="a".repeat(15000)+"\n"+"b".repeat(15000)+"\n"+"c".repeat(100);
        List<NoteSplit.Piece> pieces=NoteSplit.pieces("Plan",body);
        assertEquals(2,pieces.size());
        assertEquals("a".repeat(15000)+"\n",pieces.get(0).body);
        assertEquals("",pieces.get(1).title);
        assertEquals(body,joined(pieces));
        for(NoteSplit.Piece p:pieces)assertNull(NoteLimit.over(p.title.length(),p.body));
    }
    @Test public void oneLineTooLongIsCutWhereATabEndsNeverInAnEmoji() {
        String body="x".repeat(NoteLimit.TAB-1)+"😀"+"y";
        List<NoteSplit.Piece> pieces=NoteSplit.pieces("",body);
        assertEquals(body,joined(pieces));
        assertFalse(Character.isHighSurrogate(pieces.get(0).body.charAt(pieces.get(0).body.length()-1)));
        for(NoteSplit.Piece p:pieces)assertNull(NoteLimit.over(0,p.body));
    }
    @Test public void tabsWithinTheirLimitsStayInOneNote() {
        StringBuilder body=new StringBuilder();
        for(int i=1;i<=3;i++)body.append("=== Tab ").append(i).append(" ===\n").append("w".repeat(20000)).append('\n');
        List<NoteSplit.Piece> pieces=NoteSplit.pieces("Tabs",body.toString());
        assertEquals("three tabs of 20,000 fit one note of 100,000",1,pieces.size());
    }
    @Test public void aTitleTooLongIsKeptWholeAtTheTopAndItsWritersFollow() {
        String name="t".repeat(NoteLimit.TITLE+5);
        List<NoteSplit.Piece> pieces=NoteSplit.pieces(name,"words");
        assertEquals(NoteLimit.TITLE,pieces.get(0).title.length());
        assertEquals(name+"\nwords",pieces.get(0).body);
        Writers who=NoteSplit.pieces(name,"words").get(0).writers(Writers.of("k1","words".length()));
        assertEquals(pieces.get(0).body.length(),who.length());
        assertEquals(Writers.ME,who.at(0));assertEquals("k1",who.at(name.length()+1));
    }
    @Test public void copyTitlesFitTheLimit() {
        assertEquals("Plan (copy)",NoteSplit.copyTitle("Plan",1,true));
        assertEquals("Plan (continued)",NoteSplit.copyTitle("Plan",1,false));
        assertEquals("Plan (continued 3)",NoteSplit.copyTitle("Plan",3,false));
        assertTrue(NoteSplit.copyTitle("n".repeat(NoteLimit.TITLE),2,false).length()<=NoteLimit.TITLE);
        assertTrue(NoteSplit.copyTitle("n".repeat(NoteLimit.TITLE),2,false).endsWith("(continued 2)"));
    }
}
