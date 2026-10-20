package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Notes and folders kept as files (decision 162), on made-up notes in a temporary folder: written out, kept up to date both
 * ways, nothing lost when both sides changed, lost and found again, and never a private thing or a locked notebook written.
 */
public class DesktopOnDiskTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;private DesktopOnDisk disk;private Path out;

    @Before public void start() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pad").toPath().toFile()));store.getWritableDatabase();
        disk=new DesktopOnDisk(store,id->false,()->false);
        out=temp.newFolder("outside").toPath();
    }
    @After public void stop(){store.close();}

    private NoteStore.Note note(String title,String body){return note(title,body,Things.HOME);}
    private NoteStore.Note note(String title,String body,String folder) {
        NoteStore.Note n=new NoteStore.Note();n.title=title;n.body=body;n.book=folder;store.save(n);return store.get(n.id);
    }
    private static String read(Path p) throws Exception{return Files.readString(p,StandardCharsets.UTF_8);}
    /** As an editor saves: the file's time moves on, as it does when somebody writes in it. */
    private static void edit(Path p,String text) throws Exception {
        long before=Files.getLastModifiedTime(p).toMillis();
        Files.writeString(p,text,StandardCharsets.UTF_8);
        Files.setLastModifiedTime(p,java.nio.file.attribute.FileTime.fromMillis(before+2000));
    }
    private static List<DesktopOnDisk.Did> did(List<DesktopOnDisk.Change> changes){List<DesktopOnDisk.Did> all=new ArrayList<>();for(var c:changes)all.add(c.did());return all;}

    @Test public void aNoteKeptAsAFileIsWrittenAsItsTitleWithItsId() throws Exception {
        NoteStore.Note n=note("Shopping","Bread\n__Milk__\n=== Later ===\nEggs");
        DesktopOnDisk.Link l=disk.keepNote(n.id,out);
        assertEquals(out.resolve("Shopping.md"),l.path());
        assertEquals("---\nmininotes: "+n.id+"\n---\nBread\n<u>Milk</u>\n<!-- tab: Later -->\nEggs",read(l.path()));
        assertEquals("",disk.state(n.id));assertNull(disk.state("somebody else"));
        try{disk.keepNote(n.id,out);fail("kept twice");}catch(IllegalStateException once){/* said */}
        // A second note of the same name is given a name of its own; one with no title is called by its first line.
        assertEquals(out.resolve("Shopping (2).md"),disk.keepNote(note("Shopping","x").id,out).path());
        assertEquals(out.resolve("First line.md"),disk.keepNote(note("","First line\nmore").id,out).path());
        // Nothing to do while nothing changed.
        assertEquals(List.of(),disk.pass());
    }

    @Test public void keptUnderTheNameChosenInTheSaveBox() throws Exception {
        NoteStore.Note n=note("Shopping","Bread");
        // The name the box offered, the note's title: nothing about the note changes.
        DesktopOnDisk.Link l=disk.keepNoteAs(n.id,out.resolve("Shopping.md"));
        assertEquals(out.resolve("Shopping.md"),l.path());assertEquals("Shopping",store.get(n.id).title);assertEquals(n.revision,store.get(n.id).revision);
        // Another name typed there: the note takes it; and a file already there is never written over.
        NoteStore.Note m=note("Plan","x");Files.writeString(out.resolve("Week.md"),"somebody else's");
        l=disk.keepNoteAs(m.id,out.resolve("Week.md"));
        assertEquals("Week",store.get(m.id).title);assertEquals(out.resolve("Week (2).md"),l.path());
        assertEquals("somebody else's",read(out.resolve("Week.md")));
        assertEquals(List.of(),disk.pass());
        // Written in again, it keeps its numbered name rather than looking for another.
        DesktopEdits.save(store,store.get(m.id),"Week","y");disk.pass();
        assertEquals(out.resolve("Week (2).md"),disk.link(m.id).path());assertEquals("y",NoteFile.read(read(out.resolve("Week (2).md"))).body());
    }

    @Test public void writingInMininotesWritesTheFileAndATitleChangedRenamesIt() throws Exception {
        NoteStore.Note n=note("Shopping","Bread");
        Path file=disk.keepNote(n.id,out).path();
        NoteStore.Note now=DesktopEdits.save(store,n,"Shopping","Bread\nButter");
        assertEquals(List.of(DesktopOnDisk.Did.WROTE),did(disk.pass()));
        assertEquals("Bread\nButter",NoteFile.read(read(file)).body());
        DesktopEdits.save(store,now,"Groceries","Bread\nButter");
        assertEquals(List.of(DesktopOnDisk.Did.WROTE),did(disk.pass()));
        assertFalse(Files.exists(file));assertTrue(Files.exists(out.resolve("Groceries.md")));
        assertEquals(out.resolve("Groceries.md"),disk.link(n.id).path());
        assertEquals(List.of(),disk.pass());
    }

    @Test public void theFilesKeptWithANoteGoInAFolderBesideIt() throws Exception {
        NoteStore.Note n=note("Trip","Pack");
        NoteStore.Held receipt=store.opening(NoteStore.Branch.Kind.PAGE,n.id,"receipt.txt","text/plain",5);
        Files.write(store.fileFor(receipt.id).toPath(),"12.50".getBytes(StandardCharsets.UTF_8));store.keep(receipt);
        disk.keepNote(n.id,out);
        assertEquals("12.50",read(out.resolve("Trip.files/receipt.txt")));
        // One added later, and two of the same name: each written once, beside the others.
        for(String said:new String[]{"map","map again"}) {
            NoteStore.Held map=store.opening(NoteStore.Branch.Kind.PAGE,n.id,"map.txt","text/plain",said.length());
            Files.write(store.fileFor(map.id).toPath(),said.getBytes(StandardCharsets.UTF_8));store.keep(map);
        }
        DesktopEdits.save(store,n,"Trip","Pack light");disk.pass();
        assertTrue(Files.exists(out.resolve("Trip.files/map.txt")));assertTrue(Files.exists(out.resolve("Trip.files/map (2).txt")));
        // Renamed in Mininotes: its files' folder goes with it.
        DesktopEdits.save(store,store.get(n.id),"Holiday","Pack light");disk.pass();
        assertEquals("12.50",read(out.resolve("Holiday.files/receipt.txt")));assertFalse(Files.exists(out.resolve("Trip.files")));
    }

    @Test public void aChangeFromSomebodyItIsSharedWithIsWrittenToTheFile() throws Exception {
        NoteStore.Note n=note("Shared list","Bread");
        Path file=disk.keepNote(n.id,out).path();
        // As a change arrives from another device: a newer revision, their words.
        store.landed(n.id,"MxSOMEBODY",n.revision+1,"Shared list","Bread\nApples from Ana",Things.HOME);
        assertEquals(List.of(DesktopOnDisk.Did.WROTE),did(disk.pass()));
        assertEquals("Bread\nApples from Ana",NoteFile.read(read(file)).body());
    }

    @Test public void anEditOutsideIsTakenInAsTheOwnersAndWhatWasThereKeptInVersions() throws Exception {
        NoteStore.Note n=note("Shopping","Bread");
        Path file=disk.keepNote(n.id,out).path();
        edit(file,"---\nmininotes: "+n.id+"\n---\nBread\n<u>Cheese</u>\n");
        assertEquals(List.of(DesktopOnDisk.Did.TOOK_IN),did(disk.pass()));
        NoteStore.Note now=store.get(n.id);
        assertEquals("Bread\n__Cheese__\n",now.body);assertEquals(n.revision+1,now.revision);
        assertEquals("Bread",store.versions(n.id).get(0).body);
        // Taken in as the owner's own writing, so it goes as any edit goes.
        assertEquals(Writers.ME,store.writersOf(n.id,now.body).at(now.body.indexOf("Cheese")));
        // Its own writing seen again: nothing more to do.
        assertEquals(List.of(),disk.pass());
        // An editor that took the header away: the words are taken in, and the header is written back.
        edit(file,"Only words now");
        assertEquals(List.of(DesktopOnDisk.Did.TOOK_IN),did(disk.pass()));
        assertEquals("Only words now",store.get(n.id).body);assertEquals(n.id,NoteFile.read(read(file)).id());
        // A private code typed into the file never stays in the note, nor in the file.
        edit(file,"---\nmininotes: "+n.id+"\n---\nlist\nprivatespace//:hunter2\n");
        disk.pass();
        assertFalse(store.get(n.id).body.contains("hunter2"));assertFalse(read(file).contains("hunter2"));
    }

    /**
     * Changed on both sides: asked first (the owner, 2026-10-14: "let's ask first"), nothing written over until then, and
     * whichever words are kept, the others are in Versions.
     */
    @Test public void changedOnBothSidesIsAskedAndTheFilesWordsKept() throws Exception {
        NoteStore.Note n=note("Plan","one");
        Path file=disk.keepNote(n.id,out).path();
        DesktopEdits.save(store,n,"Plan","one, written here");
        edit(file,"---\nmininotes: "+n.id+"\n---\none, written in Notepad");
        assertEquals(List.of(DesktopOnDisk.Did.BOTH),did(disk.pass()));
        assertEquals(DesktopOnDisk.ASK,disk.state(n.id));
        assertEquals("nothing taken in yet","one, written here",store.get(n.id).body);
        assertEquals("nothing written over yet","one, written in Notepad",NoteFile.read(read(file)).body());
        assertEquals("asked once, not again on every look",List.of(),did(disk.pass()));
        disk.settled(n.id,true);
        assertEquals(List.of(DesktopOnDisk.Did.TOOK_IN),did(disk.pass()));
        assertEquals("one, written in Notepad",store.get(n.id).body);
        boolean kept=false;for(var v:store.versions(n.id))kept|=v.body.equals("one, written here");
        assertTrue("Mininotes' own writing is in Versions",kept);
        assertEquals("",disk.state(n.id));
    }

    @Test public void changedOnBothSidesMininotesWordsKept() throws Exception {
        NoteStore.Note n=note("Plan","one");
        Path file=disk.keepNote(n.id,out).path();
        DesktopEdits.save(store,n,"Plan","one, written here");
        edit(file,"---\nmininotes: "+n.id+"\n---\none, written in Notepad");
        assertEquals(List.of(DesktopOnDisk.Did.BOTH),did(disk.pass()));
        disk.settled(n.id,false);
        disk.pass();
        assertEquals("one, written here",store.get(n.id).body);
        assertEquals("the file says Mininotes' words","one, written here",NoteFile.read(read(file)).body());
        boolean kept=false;for(var v:store.versions(n.id))kept|=v.body.equals("one, written in Notepad");
        assertTrue("the file's writing is in Versions",kept);
    }

    @Test public void notepadsWindowsLineEndsAreReadAsTheNotesOwn() throws Exception {
        NoteStore.Note n=note("Crlf","a\nb");
        Path file=disk.keepNote(n.id,out).path();
        edit(file,"---\r\nmininotes: "+n.id+"\r\n---\r\na\r\nb\r\nc");
        assertEquals(List.of(DesktopOnDisk.Did.TOOK_IN),did(disk.pass()));
        assertEquals("a\nb\nc",store.get(n.id).body);
    }

    @Test public void renamedInExplorerIsFollowedAndDeletedIsLostUntilFound() throws Exception {
        NoteStore.Note n=note("Shopping","Bread");
        Path file=disk.keepNote(n.id,out).path();
        Files.move(file,out.resolve("Groceries.md"));
        assertEquals(List.of(DesktopOnDisk.Did.TOOK_IN),did(disk.pass()));
        assertEquals("Groceries",store.get(n.id).title);assertEquals("Bread",store.get(n.id).body);
        assertEquals(out.resolve("Groceries.md"),disk.link(n.id).path());
        // Moved where it cannot be seen: lost, and said once.
        Path away=temp.newFolder("elsewhere").toPath().resolve("Somewhere.md");
        Files.move(out.resolve("Groceries.md"),away);
        assertEquals(List.of(DesktopOnDisk.Did.LOST),did(disk.pass()));
        assertEquals(DesktopOnDisk.GONE,disk.state(n.id));
        assertEquals(List.of(),disk.pass());
        // Find it…: only the note's own file is taken.
        Path stranger=out.resolve("Stranger.md");Files.writeString(stranger,"# not a note of ours");
        try{disk.findIt(n.id,stranger);fail("took a file that is not this note's");}catch(IllegalStateException refused){/* said */}
        NoteStore.Note other=note("Other","x");Path otherFile=disk.keepNote(other.id,out).path();
        try{disk.findIt(n.id,otherFile);fail("took another note's file");}catch(IllegalStateException refused){/* said */}
        assertEquals(DesktopOnDisk.Did.FOUND,disk.findIt(n.id,away).did());
        assertEquals("",disk.state(n.id));assertEquals(away,disk.link(n.id).path());
        // Keep it inside only: no longer kept, and neither the note nor the file is deleted.
        disk.keepInsideOnly(n.id);
        assertNull(disk.link(n.id));assertNull(disk.state(n.id));assertTrue(Files.exists(away));assertNotNull(store.get(n.id));
    }

    @Test public void aFileOnADriveThatIsNotConnectedIsSaidSoAndComesBackByItself() throws Exception {
        NoteStore.Note n=note("Trip","Pack");
        DesktopOnDisk.Link l=disk.keepNote(n.id,out);
        String letter=null;
        for(char c='Z';c>='D';c--)if(!Files.exists(Path.of(c+":\\"))){letter=c+":";break;}
        Assume.assumeNotNull(letter);
        Path unplugged=Path.of(letter+"\\Notes\\Trip.md");
        store.getWritableDatabase().execSQL("UPDATE ondisk SET path=? WHERE id=?",new Object[]{unplugged.toString(),n.id});
        assertEquals(List.of(DesktopOnDisk.Did.LOST),did(disk.pass()));
        assertEquals(DesktopOnDisk.DRIVE,disk.state(n.id));assertEquals(letter,DesktopOnDisk.drive(unplugged));
        // Plugged in again (here: the link put back where the file is), it is found without anybody doing anything.
        store.getWritableDatabase().execSQL("UPDATE ondisk SET path=? WHERE id=?",new Object[]{l.path().toString(),n.id});
        assertEquals(List.of(DesktopOnDisk.Did.FOUND),did(disk.pass()));
        assertEquals("",disk.state(n.id));
    }

    @Test public void anotherNotesFileWhereThisOnesWasIsNeverWrittenOver() throws Exception {
        NoteStore.Note n=note("A","a"),m=note("B","b");
        Path a=disk.keepNote(n.id,out).path();
        String strange="---\nmininotes: "+m.id+"\n---\nb";
        edit(a,strange);
        assertEquals(List.of(DesktopOnDisk.Did.LOST),did(disk.pass()));
        assertEquals(DesktopOnDisk.OTHER,disk.state(n.id));
        DesktopEdits.save(store,n,"A","a changed");disk.pass();
        assertEquals(strange,read(a));
    }

    @Test public void aMarkdownFileOpenedBecomesANoteKeptAsThatFileWhereItIs() throws Exception {
        Path md=out.resolve("Recipes.md");Files.writeString(md,"---\ntags: food\n---\n# Soup\n- <u>leeks</u>\n");
        NoteStore.Note n=disk.open(md,Things.HOME);
        assertEquals("Recipes",n.title);assertEquals("# Soup\n- __leeks__\n",store.get(n.id).body);
        assertEquals("---\nmininotes: "+n.id+"\ntags: food\n---\n# Soup\n- <u>leeks</u>\n",read(md));
        assertEquals(md,disk.link(n.id).path());
        assertEquals("opened again, the same note",n.id,disk.open(md,Things.HOME).id);
        assertEquals(List.of(),disk.pass());
        // Too long for a note: refused, and the file left as it was.
        Path big=out.resolve("Big.md");String huge="x".repeat(30000);Files.writeString(big,huge);
        try{disk.open(big,Things.HOME);fail("took a file too long for a note");}catch(IllegalStateException refused){assertTrue(refused.getMessage().contains("24,000"));}
        assertEquals(huge,read(big));
    }

    @Test public void aPrivateThingOrALockedNotebookIsNeverWrittenOut() throws Exception {
        NoteStore.Note n=note("Diary","words");
        DesktopOnDisk privately=new DesktopOnDisk(store,id->id.equals(n.id),()->false);
        try{privately.keepNote(n.id,out);fail("wrote a private thing");}catch(IllegalStateException refused){/* said */}
        DesktopOnDisk locked=new DesktopOnDisk(store,id->false,()->true);
        try{locked.keepNote(n.id,out);fail("wrote while locked");}catch(IllegalStateException refused){/* said */}
        NoteStore.Shelf folder=store.addCollectionIn(Things.HOME,"Box");
        try{locked.keepFolder(folder.id,out);fail("wrote a folder while locked");}catch(IllegalStateException refused){/* said */}
        try(var listed=Files.list(out)){assertEquals(0,listed.count());}
        // Kept, then the notebook locks: nothing more is written until it is open again.
        Path file=disk.keepNote(n.id,out).path();String was=read(file);
        DesktopEdits.save(store,n,"Diary","more words");
        assertEquals(List.of(),locked.pass());assertEquals(was,read(file));
        // A private code an older build left in a note is never written into its file.
        store.getWritableDatabase().execSQL("UPDATE notes SET body=?,revision=revision+1 WHERE id=?",new Object[]{"a\nprivate//:secret\nb",n.id});
        disk.pass();
        assertFalse(read(file).contains("secret"));
    }

    @Test public void aFolderKeptOnDiskAgreesBothWays() throws Exception {
        NoteStore.Shelf trips=store.addCollectionIn(Things.HOME,"Trips"),italy=store.addCollectionIn(trips.id,"Italy");
        NoteStore.Note pack=note("Packing","socks",trips.id),rome=note("Rome","Colosseum",italy.id);
        DesktopOnDisk.Link root=disk.keepFolder(trips.id,out);
        assertEquals(out.resolve("Trips"),root.path());
        assertTrue(Files.isRegularFile(out.resolve("Trips/Packing.md")));assertTrue(Files.isRegularFile(out.resolve("Trips/Italy/Rome.md")));
        assertEquals("Colosseum",NoteFile.read(read(out.resolve("Trips/Italy/Rome.md"))).body());
        assertEquals(List.of(),disk.pass());
        // A note made in Mininotes in it: written there.
        NoteStore.Note venice=note("Venice","canals",italy.id);
        assertEquals(List.of(DesktopOnDisk.Did.MADE),did(disk.pass()));
        assertTrue(Files.isRegularFile(out.resolve("Trips/Italy/Venice.md")));
        // A .md put there in Explorer, and a folder made there: a note and a folder in Mininotes.
        Files.writeString(out.resolve("Trips/Italy/Milan.md"),"Duomo");
        Files.createDirectories(out.resolve("Trips/Spain"));Files.writeString(out.resolve("Trips/Spain/Seville.md"),"Alcazar");
        disk.pass();
        String milan=NoteFile.read(read(out.resolve("Trips/Italy/Milan.md"))).id();
        assertNotNull(milan);assertEquals("Duomo",store.get(milan).body);assertEquals(italy.id,store.get(milan).book);
        String seville=NoteFile.read(read(out.resolve("Trips/Spain/Seville.md"))).id();
        NoteStore.Note sev=store.get(seville);assertEquals("Spain",store.nameOf(sev.book,true));assertEquals(trips.id,store.collectionOfBook(sev.book));
        assertEquals(List.of(),disk.pass());
        // A note's file moved to another of its folders in Explorer: the note moves there.
        Files.move(out.resolve("Trips/Packing.md"),out.resolve("Trips/Spain/Packing.md"));
        disk.pass();
        assertEquals(sev.book,store.get(pack.id).book);assertEquals("socks",store.get(pack.id).body);
        // And a note moved in Mininotes: its file follows.
        store.moveInto(NoteStore.Branch.Kind.PAGE,rome.id,trips.id);
        disk.pass();
        assertTrue(Files.isRegularFile(out.resolve("Trips/Rome.md")));assertFalse(Files.exists(out.resolve("Trips/Italy/Rome.md")));
        // A folder renamed in Explorer: the folder in Mininotes takes the name, and keeps its notes.
        Files.move(out.resolve("Trips/Italy"),out.resolve("Trips/Italia"));
        disk.pass();
        assertEquals("Italia",store.nameOf(italy.id,true));assertEquals(italy.id,store.get(venice.id).book);
        assertEquals(out.resolve("Trips/Italia/Venice.md"),disk.link(venice.id).path());
        assertEquals("",disk.state(venice.id));
        // Keep it inside only, on the folder: nothing kept any longer, and nothing deleted on either side.
        disk.keepInsideOnly(trips.id);
        assertTrue(disk.links().isEmpty());assertTrue(Files.isRegularFile(out.resolve("Trips/Italia/Venice.md")));assertNotNull(store.get(venice.id));
    }
}
