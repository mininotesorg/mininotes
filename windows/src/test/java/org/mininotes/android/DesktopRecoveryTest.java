package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Writing a note cannot take is made ordinary notes, never kept aside (decision 328): too long, the rest beside it; the note
 * gone or read only, a copy on Home; drafts an older PC kept, notes at the start. Nothing typed is lost. Made-up words only.
 */
public class DesktopRecoveryTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Context context;private NoteStore store;
    @Before public void start() throws Exception{context=new Context(temp.newFolder().toPath().toFile());store=new NoteStore(context);store.getWritableDatabase();}
    @After public void close(){store.close();}
    private NoteStore.Note note(String body){NoteStore.Note n=new NoteStore.Note();n.body=body;n.book=Things.HOME;store.save(n);return n;}

    @Test public void whatFitsIsWrittenInAndTheRestGoesOnBesideIt(){
        NoteStore.Note n=note("");String body="a".repeat(20000)+"\n"+"b".repeat(10000);
        var result=DesktopRecovery.save(store,n,"Plan",body,null);
        assertNotNull(result.note());assertEquals("a".repeat(20000)+"\n",store.get(n.id).body);
        assertEquals(1,result.made().size());NoteStore.Note rest=store.get(result.made().get(0).id);
        assertEquals("Plan (continued)",rest.title);assertEquals("b".repeat(10000),rest.body);assertEquals(n.book,rest.book);
        assertTrue(DesktopRecovery.all(store).isEmpty());
    }
    @Test public void aTitleTooLongGoesWholeAtTheTopOfTheWords(){
        NoteStore.Note n=note("");String name="t".repeat(161);
        var result=DesktopRecovery.save(store,n,name,"words",null);
        assertEquals(160,store.get(n.id).title.length());assertEquals(name+"\nwords",store.get(n.id).body);assertTrue(result.made().isEmpty());
    }
    @Test public void aNoteGoneMeansACopyOnHome(){
        NoteStore.Note n=note("before");NoteStore.Note seen=n.copy();store.getWritableDatabase().delete("notes","id=?",new String[]{n.id});
        var result=DesktopRecovery.save(store,seen,"Gone note","typed here",null);
        assertNull(result.note());assertEquals(1,result.made().size());
        NoteStore.Note copy=store.get(result.made().get(0).id);assertEquals("Gone note (copy)",copy.title);assertEquals("typed here",copy.body);assertEquals(Things.HOME,copy.book);
    }
    @Test public void readOnlyArrivalLeavesTheSharedNoteAndTheWritingIsACopy(){
        NoteStore.Note n=note("before");NoteStore.Note local=n.copy();n.theirs=true;n.writes=false;n.body="from sender";store.save(n);
        assertTrue((Boolean)store.readOnlyHere(n.id)[0]);
        var result=DesktopRecovery.save(store,local,"My local title","already typed here",null);
        assertNull(result.note());assertEquals("from sender",store.get(n.id).body);assertTrue(store.owed(NoteStore.Branch.Kind.PAGE,n.id).isEmpty());
        assertEquals("already typed here",store.get(result.made().get(0).id).body);assertEquals("My local title (copy)",result.made().get(0).title);
    }
    @Test public void whatArrivedMakingItTooLongLeavesTheNoteAndTheWritingIsACopy(){
        NoteStore.Note base=note("first\nlast");
        String remote="r".repeat(12500)+"\nfirst\nlast";DesktopEdits.save(store,base,"",remote);
        String local="first\nlast\n"+"l".repeat(12500);
        var result=DesktopRecovery.save(store,base,"",local,null);
        assertNull(result.note());assertEquals(remote,store.get(base.id).body);
        assertEquals(local,store.get(result.made().get(0).id).body);
    }
    @Test public void whoWroteWhatGoesWithEachPiece(){
        NoteStore.Note n=note("");String body="a".repeat(24000)+"b".repeat(10);Writers who=Writers.of(Writers.ME,body.length());
        var result=DesktopRecovery.save(store,n,"",body,who);
        assertEquals(24000,store.get(n.id).body.length());
        assertEquals(10,store.writersOf(result.made().get(0).id,"b".repeat(10)).length());
    }
    @Test public void draftsAnOlderPcKeptAreNotesAndNothingIsKeptAside(){
        NoteStore.Note n=note("saved");String full="synthetic writing "+"x".repeat(30000);
        DesktopRecovery.keep(store,n,"Draft",full,null,"long");DesktopRecovery.keep(store,n,"","short one",null,"local");
        List<NoteStore.Note> made=DesktopRecovery.adopt(store);
        assertTrue(DesktopRecovery.all(store).isEmpty());assertEquals("saved",store.get(n.id).body);
        StringBuilder first=new StringBuilder();for(NoteStore.Note one:made)if(one.title.startsWith("Draft"))first.append(store.get(one.id).body);
        assertEquals(full,first.toString());
        assertTrue(made.stream().anyMatch(one->one.title.equals("Untitled (copy)")&&"short one".equals(store.get(one.id).body)));
        assertTrue("once only",DesktopRecovery.adopt(store).isEmpty());
    }
}
