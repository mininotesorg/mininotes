package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The tab of its note each file is under, in a notebook of its own, no network (decision 299): put under the open tab when
 * added, said on the note's list, taken from another device's list only when decided later, carried by a rename, cleared
 * when the file goes to another note.
 */
public class DesktopTabFilesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;private String note;
    @After public void close(){if(store!=null)store.close();}

    @Before public void one() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pc")));store.getWritableDatabase();
        NoteStore.Note n=new NoteStore.Note();n.id=UUID.randomUUID().toString();n.book=store.someBook();
        n.body="Intro\n=== Ideas ===\none\n=== Receipts ===\ntwo";n.revision=1;store.save(n);note=n.id;
    }

    private String attach(String name) throws Exception {
        String id=UUID.randomUUID().toString();byte[] bytes=name.getBytes();
        Files.write(store.fileFor(id).toPath(),bytes);
        store.keep(new NoteStore.Held(id,note,name,"text/plain",bytes.length,System.currentTimeMillis()));
        return id;
    }

    private Enclosure.Listed listed(String id) {
        for(Enclosure.Listed one:store.enclosed(note))if(one.id.equals(id))return one;
        return null;
    }

    @Test public void aFileAddedGoesUnderTheTabOpenOnItsNote() throws Exception {
        String before=attach("before.txt");
        store.tabOpen(note,"Ideas");
        String ideas=attach("ideas.txt");
        store.tabOpen(note,"");
        String first=attach("first.txt");
        // Another note open: this one's files are not put under its tab.
        store.tabOpen("another","Receipts");
        String other=attach("other.txt");
        Map<String,String> tabs=store.tabsOf(note);
        assertFalse(tabs.containsKey(before));assertEquals("Ideas",tabs.get(ideas));
        assertFalse(tabs.containsKey(first));assertFalse(tabs.containsKey(other));
        // Said on the list, with when; a file with none says nothing, as before.
        assertEquals("Ideas",listed(ideas).tab);assertTrue(listed(ideas).tabAt>0);
        assertNull(listed(before).tab);
    }

    @Test public void anotherDevicesListIsTakenOnlyWhereItDecidedLater() throws Exception {
        store.tabOpen(note,"Ideas");String id=attach("a.txt");long at=listed(id).tabAt;
        Enclosure.Listed mine=listed(id);
        store.listed(note,"MxOther@1",1L,List.of(mine.under("Receipts",at-1)));
        assertEquals("Ideas",store.tabOf(id));
        store.listed(note,"MxOther@1",2L,List.of(mine.under("Receipts",at+1)));
        assertEquals("Receipts",store.tabOf(id));
        // A build from before says no tab: nothing changes.
        store.listed(note,"MxOther@1",3L,List.of(new Enclosure.Listed(mine.id,mine.name,mine.kind,mine.bytes,mine.manifest)));
        assertEquals("Receipts",store.tabOf(id));
    }

    @Test public void moveToTabAndARenameCarryFiles() throws Exception {
        String old=attach("old.txt");
        store.tabOpen(note,"Ideas");String ideas=attach("ideas.txt");
        store.tabFile(old,"Receipts");
        assertEquals("Receipts",store.tabOf(old));
        String was="Intro\n=== Ideas ===\none\n=== Receipts ===\ntwo";
        assertEquals(1,store.retab(note,TabFiles.carried(was,Sections.rename(was,1,"Thoughts"))));
        assertEquals("Thoughts",store.tabOf(ideas));assertEquals("Receipts",store.tabOf(old));
        // Nothing to carry: nothing moves.
        assertEquals(0,store.retab(note,Map.of()));
    }

    @Test public void aFileMovedToAnotherNoteLeavesItsTabBehind() throws Exception {
        store.tabOpen(note,"Ideas");String id=attach("a.txt");
        NoteStore.Note other=new NoteStore.Note();other.id=UUID.randomUUID().toString();other.book=store.someBook();other.body="x";store.save(other);
        store.moveInto(NoteStore.Branch.Kind.FILE,id,other.id);
        assertEquals("",store.tabOf(id));
    }
}
