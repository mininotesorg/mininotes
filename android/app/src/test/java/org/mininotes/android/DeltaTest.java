package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Only what changed in a note (decision 320): made, sealed in a parcel, read and rebuilt to the very text; refused where the
 * base is not the one it was made against; never for a build that has not said it reads it. Synthetic text only.
 */
public class DeltaTest {

    /** A long note: numbered lines of ordinary writing, about {@code characters} long. */
    private static String longNote(int characters) {
        StringBuilder out=new StringBuilder();
        for(int line=1;out.length()<characters;line++)
            out.append(line).append(". The shopping, the garden and the letters to answer before Friday.\n");
        return out.toString();
    }

    private static Parcel.Sent whole(String body) {
        List<Parcel.Step> path=Collections.singletonList(new Parcel.Step("c1","Home things","",Tint.NONE,0L));
        return new Parcel.Sent("c1","Home things","","","A note",body,true,Collections.<Parcel.Member>emptyList(),"","",
            true,5L,true,new ArrayList<Enclosure.Listed>(),1L,Collections.<String>emptyList(),path,"",null);
    }

    @Test public void aChangeMadeAndAppliedGivesBackTheExactText() {
        String[][] pairs={
            {longNote(5000),longNote(5000).replace("17. The shopping","17. The shopping, and milk")},
            {"one\ntwo\nthree","one\nthree"},
            {"","written into an empty note"},
            {"all of it","",},
            {"a\nb\n","a\nb\n\n"},
            {"line\r\nwith returns\r\n","line\r\nwith returns, kept\r\n"},
            {"accents é, 東京 and 🌱\nsecond","accents é, 東京 and 🌱🌱\nsecond\nthird"},
            {"same","same"},
            {"x\ny\nz","z\ny\nx"},
        };
        for(String[] one:pairs) {
            Delta change=Delta.between(one[0],4L,one[1]);
            assertEquals(one[1],change.onto(one[0]));
        }
    }

    @Test public void itTravelsInAParcelAndIsRebuiltToTheSameText() throws Exception {
        String base=longNote(20000), now=base.replace("40. The shopping","40. The shopping and the bread");
        Delta change=Delta.going(true,false,7L,base,now);
        assertNotNull(change);
        byte[] bytes=Parcel.wrap(Parcel.asChange(whole(now),change),Envelope.MAX_TEXT);
        Parcel.Sent read=Parcel.open(bytes);
        assertNotNull(read);
        assertTrue(read.changeSaid);
        assertNotNull(read.change);
        assertEquals(7L,read.change.base);
        assertEquals(now,read.change.onto(base));
        assertEquals("the rest of the parcel as the whole note's","Home things",read.path.get(0).name);
        assertEquals(5L,read.basedOn);
        // And what is taken from it is a whole note, nothing of the change left in it.
        Parcel.Sent rebuilt=Parcel.withText(read,read.change.onto(base));
        assertEquals(now,rebuilt.body);
        assertNull(rebuilt.change);
        assertFalse(rebuilt.changeSaid);
    }

    @Test public void aWrongOrMissingBaseIsRefusedSoTheWholeIsAskedFor() {
        String base=longNote(8000), now=base.replace("3. The shopping","3. The shopping, done");
        Delta change=Delta.between(base,9L,now);
        assertNull("no copy here",change.onto(null));
        assertNull("a copy that differs by one letter",change.onto(base.replace("12. The","12. the")));
        assertNull("what is written here since",change.onto(base+"more\n"));
        assertNull("an empty note",change.onto(""));
        assertEquals(now,change.onto(base));
    }

    @Test public void aChangeThatDoesNotReadIsRefusedAndNeverTakenAsTheNote() throws Exception {
        String base=longNote(8000), now=base.replace("3. The shopping","3. The shopping, done");
        byte[] bytes=Parcel.wrap(Parcel.asChange(whole(now),Delta.between(base,9L,now)),Envelope.MAX_TEXT);
        // Cut inside the change: the parcel's fixed part still reads, the change does not.
        Parcel.Sent cut=Parcel.open(Arrays.copyOf(bytes,bytes.length-3));
        assertNotNull(cut);
        assertTrue("said, so refused",cut.changeSaid);
        assertNull(cut.change);
        assertEquals("the body is no note's words",Delta.IN_TAIL,cut.body);
    }

    @Test public void aChangeAgainstAnOldRevisionMergesWithWritingDoneSince() {
        String agreed="Groceries\neggs\nbread\nmilk\n\nGarden\nwater the beans\n"+longNote(3000);
        // They wrote at the top since; here, at the bottom, before their change came.
        String theirs=agreed.replace("bread\n","bread\nbutter\n");
        String mine=agreed+"call the plumber";
        Delta change=Delta.going(true,false,3L,agreed,theirs);
        assertNotNull(change);
        String rebuilt=change.onto(agreed);
        assertEquals(theirs,rebuilt);
        Merge.Result both=Merge.merge(agreed,mine,rebuilt);
        assertTrue(both.clean);
        assertTrue(both.text.contains("butter"));
        assertTrue(both.text.endsWith("call the plumber"));
    }

    @Test public void anOlderBuildOnlyEverGetsWholeNotes() {
        String base=longNote(50000), now=base.replace("100. The","100. A");
        assertNull("not said it reads changes",Delta.going(false,false,8L,base,now));
        assertNull("Sync, or the whole asked for",Delta.going(true,true,8L,base,now));
        assertNull("the first time: nothing confirmed",Delta.going(true,false,0L,base,now));
        assertNull("the base not kept here exactly",Delta.going(true,false,8L,null,now));
        assertNull("a short note",Delta.going(true,false,8L,"short","shorter"));
        assertNull("a change not clearly smaller",Delta.going(true,false,8L,base,longNote(50000).replace("The","the")));
        assertNotNull(Delta.going(true,false,8L,base,now));
    }

    @Test public void aLargeNotesOneLineChangeIsAFewHundredBytes() throws Exception {
        String base=longNote(95000), now=base.replace("700. The shopping","700. The shopping and a new kettle");
        assertTrue(now.length()<NoteLimit.NOTE);
        Delta change=Delta.going(true,false,12L,base,now);
        assertNotNull(change);
        assertTrue("a change of "+change.size()+" bytes",change.size()<300);
        int asWhole=Parcel.wrap(whole(now),Envelope.MAX_TEXT).length;
        int asChange=Parcel.wrap(Parcel.asChange(whole(now),change),Envelope.MAX_TEXT).length;
        assertTrue("whole "+asWhole+", change "+asChange,asChange<500&&asWhole>95000);
        assertEquals(now,Parcel.open(Parcel.wrap(Parcel.asChange(whole(now),change),Envelope.MAX_TEXT)).change.onto(base));
    }

    @Test public void aHugeRewriteIsStillExactWithoutComparingEveryLine() {
        // More lines changed than the comparison is let fill: said as one stretch, still exact (and then not worth sending).
        StringBuilder a=new StringBuilder(), b=new StringBuilder();
        for(int line=0;line<5000;line++){a.append("a").append(line).append('\n');b.append("b").append(line).append('\n');}
        Delta change=Delta.between(a.toString(),2L,b.toString());
        assertEquals(1,change.stretches.size());
        assertEquals(b.toString(),change.onto(a.toString()));
        assertNull(Delta.worth(a.toString(),2L,b.toString()));
    }
}
