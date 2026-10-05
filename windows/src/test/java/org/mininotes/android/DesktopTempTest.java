package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Temp, Recent and what is on its way (docs/HOME.md, decisions 71 to 73; the owner, 2026-10-03). A synthetic notebook, no
 * window and no network.
 */
public class DesktopTempTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    private String kitchen,inside,soup,loose;

    @Before public void open() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();
        kitchen=store.addCollection("Kitchen").id;inside=store.addBook(kitchen,"Recipes").id;
        soup=note(inside,"Soup");loose=note(Things.HOME,"Loose");
    }
    @After public void close(){store.close();}

    private String note(String in,String title){NoteStore.Note one=new NoteStore.Note();one.book=in;one.title=title;one.body="Synthetic "+title;one.updated=System.currentTimeMillis();store.save(one);return one.id;}
    private static Set<String> ids(List<NoteStore.Branch> lines){Set<String> out=new HashSet<>();for(NoteStore.Branch one:lines)out.add(one.id);return out;}

    @Test public void aTemporaryNoteIsListedAndGoneForGoodWhenItsTimeComes() {
        long now=System.currentTimeMillis(),revision=store.get(loose).revision;
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,loose,now+3_600_000L);
        assertEquals(now+3_600_000L,store.untilOf(NoteStore.Branch.Kind.PAGE,loose));
        assertTrue("its time goes with it",store.get(loose).revision>revision);
        assertEquals(1,store.temporaryCount());
        assertTrue(ids(store.temporary(now)).contains(loose));
        assertEquals("nothing before its time",0,store.expire(now));
        assertNotNull(store.get(loose));
        assertEquals(1,store.expire(now+3_600_001L));
        assertNull("gone for good",store.get(loose));
        assertEquals(0,store.temporaryCount());
    }

    @Test public void aFileGoesWhereANoteGoes() throws Exception {
        // Decision 87: a file on Home archived, binned, starred, temporary - each listed there and not drawn on Home.
        NoteStore.Held file=store.opening(NoteStore.Branch.Kind.COLLECTION,Things.HOME,"Synthetic receipt.pdf","application/pdf",12);
        java.nio.file.Files.writeString(store.fileFor(file.id).toPath(),"synthetic");store.keep(file);
        assertTrue(ids(store.contents(Things.HOME)).contains(file.id));
        store.putAway(NoteStore.Branch.Kind.FILE,file.id,false,true);
        assertFalse("archived, not on Home",ids(store.contents(Things.HOME)).contains(file.id));
        assertTrue(ids(store.heldIn(false)).contains(file.id));assertEquals(1,store.awayCount(false));
        store.restore(NoteStore.Branch.Kind.FILE,file.id);
        assertTrue("put back",ids(store.contents(Things.HOME)).contains(file.id));
        store.keepToHand(NoteStore.Branch.Kind.FILE,file.id,true);
        assertTrue(store.favourite(NoteStore.Branch.Kind.FILE,file.id));assertTrue(ids(store.favouritesAll()).contains(file.id));
        long now=System.currentTimeMillis();
        store.makeTemporary(NoteStore.Branch.Kind.FILE,file.id,now+60_000L);
        assertTrue(ids(store.temporary(now)).contains(file.id));assertEquals(1,store.temporaryCount());
        store.putAway(NoteStore.Branch.Kind.FILE,file.id,true,true);
        assertTrue(ids(store.heldIn(true)).contains(file.id));
        assertEquals(1,store.emptyBin());
        assertNull("gone for good",store.file(file.id));
        // And one whose time has come goes as Delete takes it.
        NoteStore.Held other=store.opening(NoteStore.Branch.Kind.COLLECTION,Things.HOME,"Synthetic ticket.txt","text/plain",9);
        java.nio.file.Files.writeString(store.fileFor(other.id).toPath(),"synthetic");store.keep(other);
        store.makeTemporary(NoteStore.Branch.Kind.FILE,other.id,now+1000L);
        assertEquals(1,store.expire(now+2000L));assertNull(store.file(other.id));
    }

    @Test public void recentPutsWhatWasOpenedLastFirstEvenAWrittenNote() throws Exception {
        // Opened, not only written (decision 86): a note opened after a collection is above it, though written before it.
        long since=System.currentTimeMillis()-60_000L;
        store.touch(NoteStore.Branch.Kind.PAGE,soup);Thread.sleep(5);
        store.touch(NoteStore.Branch.Kind.COLLECTION,kitchen);Thread.sleep(5);
        store.touch(NoteStore.Branch.Kind.PAGE,soup);
        List<NoteStore.Branch> lately=store.recent(since);
        assertEquals("the note opened last is first",soup,lately.get(0).id);
        assertEquals(kitchen,lately.get(1).id);
    }

    @Test public void tempGoesToMyOtherDevicesAndNobodyElse() throws Exception {
        // Temp as a note to self (decision 84): his own paired devices are given it, somebody else is not, and once is enough.
        Keys mine=new Keys(new Context(temp.newFolder("laptop"))),theirs=new Keys(new Context(temp.newFolder("friend")));
        store.pairedWith("MxTempLaptopFixture@127.0.0.1:9401","Test laptop",true,mine.agreement().getPublic().getEncoded(),mine.signing().getPublic().getEncoded());
        store.pairedWith("MxTempFriendFixture@127.0.0.1:9402","Somebody",false,theirs.agreement().getPublic().getEncoded(),theirs.signing().getPublic().getEncoded());
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,loose,System.currentTimeMillis()+3_600_000L);
        assertEquals(1,store.toMyDevices(Sharing.Scope.PAGE,loose));
        Map<String,Sharing.Level> on=new HashMap<>();for(Sharing.Rule rule:store.sharesOn(Sharing.Scope.PAGE,loose))on.put(rule.address,rule.level);
        assertEquals("my laptop writes in it",Sharing.Level.WRITE,on.get("MxTempLaptopFixture@127.0.0.1:9401"));
        assertNull("nobody else is given it",on.get("MxTempFriendFixture@127.0.0.1:9402"));
        assertEquals("already there",0,store.toMyDevices(Sharing.Scope.PAGE,loose));
    }

    @Test public void aTemporaryCollectionGivesItsTimeToEveryNoteInIt() {
        long at=System.currentTimeMillis()+86_400_000L;
        store.makeTemporary(NoteStore.Branch.Kind.COLLECTION,kitchen,at);
        assertEquals(at,store.untilOf(NoteStore.Branch.Kind.COLLECTION,kitchen));
        assertEquals("a note however deep",at,store.untilOf(NoteStore.Branch.Kind.PAGE,soup));
        store.makeTemporary(NoteStore.Branch.Kind.COLLECTION,kitchen,0L);
        assertEquals(0L,store.untilOf(NoteStore.Branch.Kind.PAGE,soup));
        assertEquals(0,store.temporaryCount());
    }

    @Test public void goneInIsSaidAsAPersonWould() {
        long now=1_000_000L;
        assertEquals("Gone in 3 days",NoteStore.goneIn(now+3*86_400_000L+5,now));
        assertEquals("Gone in 5 hours",NoteStore.goneIn(now+5*3_600_000L+5,now));
        assertEquals("Gone in 20 minutes",NoteStore.goneIn(now+20*60_000L+5,now));
        assertEquals("Gone in a moment",NoteStore.goneIn(now+30_000L,now));
    }

    @Test public void recentListsWhatWasOpenedWithinItsTime() {
        long before=System.currentTimeMillis();
        store.touch(NoteStore.Branch.Kind.COLLECTION,kitchen);
        assertTrue(ids(store.recent(before)).contains(kitchen));
        assertTrue("written lately counts too",ids(store.recent(before-60_000L)).contains(soup));
        assertFalse("nothing from after now",ids(store.recent(System.currentTimeMillis()+60_000L)).contains(kitchen));
    }

    @Test public void whatAnAcceptedCodeBringsStandsOnHomeUntilItComes() {
        String at="MxWaitingFixture@127.0.0.1:9310";
        store.accepting(at,"Parisa test","PAGE","n-1",Sharing.Level.WRITE);
        assertTrue("nothing named, nothing shown",store.waitingOnHome().isEmpty());
        store.acceptingOffer(at,"the note Groceries");
        List<NoteStore.Branch> waiting=store.waitingOnHome();
        assertEquals(1,waiting.size());
        assertEquals("Groceries",waiting.get(0).name);
        assertEquals(NoteStore.Branch.Kind.WAITING,waiting.get(0).kind);
        assertTrue(waiting.get(0).detail.contains("Parisa test"));
        store.answered(at);
        assertTrue("it came: the line goes",store.waitingOnHome().isEmpty());
    }
}
