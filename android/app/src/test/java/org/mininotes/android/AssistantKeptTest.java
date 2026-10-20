package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;

/**
 * The Agent's conversation kept three hours after its last message, then started afresh, and the place an answer takes you
 * to (decision 213). Made-up words only.
 */
public class AssistantKeptTest {
    private static final long HOUR=60*60_000L;

    @Test public void theConversationIsKeptThreeHoursAfterItsLastMessage() {
        long then=1_800_000_000_000L;
        Assistant.Conversation c=new Assistant.Conversation();
        List<Assistant.Line> turn=c.start("Where is the bin?",then);
        c.add(turn,Assistant.Kind.TITLE,"Archive and Bin",then);
        c.add(turn,Assistant.Kind.SAY,"Two lines\nwith a\ttab and a \\ back slash",then);
        c.add(turn,Assistant.Kind.THERE,Assistant.Place.BIN.name(),then);
        // An answer chosen from another, without a question of its own.
        c.add(c.start(null,then+1000),Assistant.Kind.QUIET,"Synthetic quiet line",then+1000);
        String kept=c.write();

        Assistant.Conversation back=Assistant.Conversation.read(kept,then+1000+Assistant.KEEP-60_000L);
        assertEquals(2,back.turns.size());
        assertEquals(then+1000,back.last);
        assertEquals(new Assistant.Line(Assistant.Kind.YOU,"Where is the bin?"),back.turns.get(0).get(0));
        assertEquals("Two lines\nwith a\ttab and a \\ back slash",back.turns.get(0).get(2).words());
        assertEquals(Assistant.Kind.THERE,back.turns.get(0).get(3).kind());
        assertEquals("Synthetic quiet line",back.turns.get(1).get(0).words());
        // Two hours and more is "a couple of hours at least": still there.
        assertFalse(Assistant.Conversation.read(kept,then+2*HOUR).isEmpty());
        // Longer than three hours after the last message, it is gone.
        assertTrue(Assistant.Conversation.read(kept,then+1000+Assistant.KEEP+60_000L).isEmpty());
        assertTrue(Assistant.KEEP>=3*HOUR);
        // Nothing, or something that is not a conversation, is an empty one.
        assertTrue(Assistant.Conversation.read("",then).isEmpty());
        assertTrue(Assistant.Conversation.read(null,then).isEmpty());
        assertTrue(Assistant.Conversation.read("not kept by the agent",then).isEmpty());
        // Kept a while, then over, even with the window open.
        assertFalse(back.over(then+HOUR));assertTrue(back.over(then+1000+Assistant.KEEP+1));
    }

    @Test public void theOldestGoFirstAndEarlierComesBefore() {
        Assistant.Conversation c=new Assistant.Conversation();
        for(int i=0;i<Assistant.MOST+10;i++)c.start("Question "+i,1000+i);
        assertEquals(Assistant.MOST,c.turns.size());
        assertEquals("Question 10",c.turns.get(0).get(0).words());
        Assistant.Conversation now=new Assistant.Conversation(),earlier=new Assistant.Conversation();
        earlier.start("Before",100);now.start("After",200);
        now.before(earlier);
        assertEquals("Before",now.turns.get(0).get(0).words());assertEquals("After",now.turns.get(1).get(0).words());assertEquals(200,now.last);
        now.clear();assertTrue(now.isEmpty());assertEquals(0,now.last);
    }

    @Test public void anAnswerAboutAPlaceTakesYouThere() {
        List<Assistant.Help> en=Assistant.help("en"),fr=Assistant.help("fr");
        Assistant.Help stamp=Assistant.lookUp("How do I set my Integritas key?",en,3).get(0);
        assertEquals("stamp",stamp.id());
        assertEquals(Assistant.Place.INTEGRITAS,Assistant.place(stamp,"How do I set my Integritas key?"));
        assertEquals(Assistant.Place.STAMPED,Assistant.place(stamp,"what did I stamp, can I verify it"));
        Assistant.Help archive=Assistant.lookUp("where is the bin",en,3).get(0);
        assertEquals("archive",archive.id());
        assertEquals(Assistant.Place.BIN,Assistant.place(archive,"where is the bin"));
        assertEquals(Assistant.Place.ARCHIVE,Assistant.place(archive,"how do I archive a note"));
        assertEquals("parlons",Assistant.lookUp("where do I put my parlons address",en,3).get(0).id());
        assertEquals(Assistant.Place.INTEGRITAS,Assistant.place(Assistant.lookUp("où mettre ma clé API Integritas",fr,3).get(0),"où mettre ma clé API Integritas"));
        // An answer about no place has none.
        Assistant.Help dock=Assistant.lookUp("What is remove from the dock?",en,3).get(0);
        assertNull(Assistant.place(dock,"What is remove from the dock?"));
        // The French help is about the same places as the English.
        for(int i=0;i<en.size();i++)assertEquals(en.get(i).id(),en.get(i).places(),fr.get(i).places());
        // A place asked for by its name.
        assertEquals(Assistant.Place.BIN,Assistant.placeNamed("the bin"));
        assertEquals(Assistant.Place.BIN,Assistant.placeNamed("Corbeille"));
        assertEquals(Assistant.Place.PROFILE,Assistant.placeNamed("mon profil"));
        assertEquals(Assistant.Place.SHARED_WITH_ME,Assistant.placeNamed("Shared with me"));
        assertNull(Assistant.placeNamed("Synthetic grocery"));
        assertNull("open Temp opens Temp itself, not its settings",Assistant.placeNamed("temp"));
    }
}
