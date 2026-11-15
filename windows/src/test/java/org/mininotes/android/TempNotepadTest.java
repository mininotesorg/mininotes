package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The notepad that stays in Temp (the owner, 2026-10-31: "In temps, we should have the possibility to put there a permanent note
 * thats stays but it content is delete, put it by default and make it in its menu that it can be turned off"; decision 307). In
 * the real notebook, no network: made once, the same note for one person on two devices, its words gone in Temp's time with a
 * version kept rather than the note taken, an ordinary Temp note still taken, and off it is an ordinary Temp note.
 */
public class TempNotepadTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}
    @Before public void english(){Say.use("en");}

    private NoteStore notebook(String name) throws Exception {
        NoteStore store=new NoteStore(new Context(temp.newFolder(name)));store.getWritableDatabase();open.add(store);return store;
    }
    private static final String PERSON="0123456789abcdef0123456789abcdef";

    @Test public void madeOnceInTempStayingAndTheSameNoteOnEveryDeviceOfOnePerson() throws Exception {
        NoteStore phone=notebook("phone"),pc=notebook("pc");
        phone.notepadOnce(PERSON);pc.notepadOnce(PERSON);
        String id=NoteStore.notepadId(PERSON);
        assertEquals("one person's devices make one note",id,NoteStore.notepadId(PERSON));
        assertNotEquals(id,NoteStore.notepadId("fedcba9876543210fedcba9876543210"));
        for(NoteStore store:List.of(phone,pc)) {
            assertTrue(store.notepadOn(PERSON));
            assertEquals("Notepad",store.get(id).title);
            assertTrue(NoteStore.stays(store.untilOf(NoteStore.Branch.Kind.PAGE,id)));
            boolean inTemp=false;for(NoteStore.Branch one:store.temporary(System.currentTimeMillis()))if(one.id.equals(id)){inTemp=true;assertEquals("Stays in Temp",one.detail);}
            assertTrue("listed in Temp",inTemp);
        }
        // Made, it is not the note last written, which a start opens.
        NoteStore.Note mine=new NoteStore.Note();mine.book=Things.HOME;mine.title="Synthetic last written";mine.body="Synthetic";mine.revision=1;mine.updated=System.currentTimeMillis()-86_400_000L;pc.save(mine);
        assertEquals(mine.id,pc.latest().id);
        // Turned off, then let go of: never made again by itself.
        phone.notepad(PERSON,false);
        assertFalse(phone.notepadOn(PERSON));
        long until=phone.untilOf(NoteStore.Branch.Kind.PAGE,id);
        assertTrue("an ordinary Temp note now, gone in Temp's time",until>System.currentTimeMillis()&&until<=System.currentTimeMillis()+phone.tempSpan());
        phone.expire(until+1);
        assertNull(phone.get(id));
        phone.notepadOnce(PERSON);
        assertNull("made once only",phone.get(id));
        // On again from Settings: made again.
        phone.notepad(PERSON,true);
        assertTrue(phone.notepadOn(PERSON));
    }

    /** Not counted on Temp's icon: always there, it would be a 1 over a Temp with nothing else in it (decision 326). */
    @Test public void theNotepadIsNotCountedOnTempsIcon() throws Exception {
        NoteStore store=notebook("count");
        store.notepadOnce(PERSON);
        assertEquals("the notepad alone: no count",0,store.temporaryCount());
        NoteStore.Note n=store.get(NoteStore.notepadId(PERSON));n.body="Synthetic words";n.updated=System.currentTimeMillis();store.save(n);
        assertEquals("written in, still not counted",0,store.temporaryCount());
        NoteStore.Note other=new NoteStore.Note();other.book=Things.HOME;other.title="Synthetic for a while";other.body="Synthetic";other.updated=System.currentTimeMillis();store.save(other);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,other.id,System.currentTimeMillis()+store.tempSpan());
        assertEquals("what comes and goes is counted",1,store.temporaryCount());
    }

    @Test public void itsWordsGoInTempsTimeWithAVersionKeptAndTheNoteStays() throws Exception {
        NoteStore store=notebook("pad");
        store.notepadOnce(PERSON);
        String id=NoteStore.notepadId(PERSON);
        long written=System.currentTimeMillis();
        NoteStore.Note pad=store.get(id);pad.body="Synthetic shopping";pad.updated=written;pad.revision=pad.revision+1;store.save(pad);
        // An ordinary Temp note beside it, due at the same time.
        NoteStore.Note other=new NoteStore.Note();other.book=Things.HOME;other.title="Synthetic other";other.body="Synthetic";other.revision=1;store.save(other);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,other.id,written+store.tempSpan());
        store.expire(written+store.tempSpan()-60_000L);
        assertEquals("not yet",  "Synthetic shopping",store.get(id).body);
        store.expire(written+store.tempSpan()+1);
        assertNotNull("the notepad stays",store.get(id));
        assertEquals("",store.get(id).body);
        assertEquals("Notepad",store.get(id).title);
        assertTrue(NoteStore.stays(store.untilOf(NoteStore.Branch.Kind.PAGE,id)));
        boolean kept=false;for(NoteStore.Version one:store.versions(id))if(one.body.equals("Synthetic shopping"))kept=true;
        assertTrue("what it said is in its versions",kept);
        assertNull("an ordinary Temp note is taken as before",store.get(other.id));
    }
}
