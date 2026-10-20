package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;
import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * Any file in a folder kept on disk (the owner, 2026-10-14: a .docx changed in Word or LibreOffice and kept in step by
 * Mininotes; docs/FILES.md), on made-up files in a temporary folder: taken in exactly, written out, both kept when both
 * changed, followed when renamed, and never deleted on either side.
 */
public class DesktopFolderFilesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;private DesktopOnDisk disk;private Path out;

    @Before public void start() throws Exception {
        store=new NoteStore(new Context(temp.newFolder("pad").toPath().toFile()));store.getWritableDatabase();
        disk=new DesktopOnDisk(store,id->false,()->false);
        out=temp.newFolder("outside").toPath();
    }
    @After public void stop(){store.close();}

    /** Left alone long enough to be taken in, as a file saved a while ago is. */
    private static void aged(Path p,int seconds) throws Exception{Files.setLastModifiedTime(p,FileTime.fromMillis(System.currentTimeMillis()-seconds*1000L));}
    private static List<DesktopOnDisk.Did> did(List<DesktopOnDisk.Change> changes){List<DesktopOnDisk.Did> all=new ArrayList<>();for(var c:changes)all.add(c.did());return all;}
    private NoteStore.Held named(String folder,String name){for(NoteStore.Held f:store.filesOf(NoteStore.Branch.Kind.COLLECTION,folder))if(f.name.equals(name))return f;return null;}
    private static byte[] bytes(String s){return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);}

    @Test public void anyFileIsKeptTheSameBothWays() throws Exception {
        NoteStore.Shelf work=store.addCollectionIn(Things.HOME,"Work");
        NoteStore.Held plan=store.addFileExact(work.id,"Plan.docx",null,new ByteArrayInputStream(bytes("PK plan one")));
        DesktopOnDisk.Link root=disk.keepFolder(work.id,out);
        // A file of the folder in Mininotes: written there, exactly.
        Path planOnDisk=root.path().resolve("Plan.docx");
        assertArrayEquals(bytes("PK plan one"),Files.readAllBytes(planOnDisk));
        aged(planOnDisk,60);
        // Put there in Explorer: taken in, the same bytes; Word's lock file and one still being written are not.
        Path budget=root.path().resolve("Budget.xlsx");Files.write(budget,bytes("PK budget"));aged(budget,60);
        Files.write(root.path().resolve("~$Plan.docx"),bytes("lock"));aged(root.path().resolve("~$Plan.docx"),60);
        Files.write(root.path().resolve("Copying.pdf"),bytes("%PDF half"));
        assertEquals(List.of(DesktopOnDisk.Did.FILE_ADDED),did(disk.pass()));
        NoteStore.Held b=named(work.id,"Budget.xlsx");
        assertNotNull(b);assertArrayEquals(bytes("PK budget"),store.bytesOf(b));
        assertNull(named(work.id,"~$Plan.docx"));assertNull(named(work.id,"Copying.pdf"));
        aged(root.path().resolve("Copying.pdf"),60);
        assertEquals(List.of(DesktopOnDisk.Did.FILE_ADDED),did(disk.pass()));
        assertEquals(List.of(),disk.pass());
        // Changed in Word: taken in exactly.
        Files.write(budget,bytes("PK budget two"));aged(budget,30);
        assertEquals(List.of(DesktopOnDisk.Did.FILE_TAKEN),did(disk.pass()));
        assertArrayEquals(bytes("PK budget two"),store.bytesOf(store.file(b.id)));
        // Changed in Mininotes, as another device's version arriving: written out.
        store.replaceFile(plan.id,plan.kind,new ByteArrayInputStream(bytes("PK plan two")),true);
        assertEquals(List.of(DesktopOnDisk.Did.WROTE),did(disk.pass()));
        assertArrayEquals(bytes("PK plan two"),Files.readAllBytes(planOnDisk));
        aged(planOnDisk,40);disk.pass();
        // Changed on both sides: asked, and nothing written over until the owner says which (the owner, 2026-10-15).
        store.replaceFile(plan.id,plan.kind,new ByteArrayInputStream(bytes("PK plan from the phone")),true);
        Files.write(planOnDisk,bytes("PK plan from Word"));aged(planOnDisk,20);
        assertEquals(List.of(DesktopOnDisk.Did.BOTH),did(disk.pass()));
        assertEquals(DesktopOnDisk.ASK,disk.state(plan.id));assertEquals(Map.of(plan.id,"Plan.docx"),disk.filesAsked());
        assertEquals(List.of(),disk.pass());
        assertArrayEquals(bytes("PK plan from Word"),Files.readAllBytes(planOnDisk));
        assertArrayEquals(bytes("PK plan from the phone"),store.bytesOf(store.file(plan.id)));
        // The file on this PC kept: taken in.
        disk.settleFile(plan.id,true);
        assertArrayEquals(bytes("PK plan from Word"),store.bytesOf(store.file(plan.id)));assertEquals("",disk.state(plan.id));
        aged(planOnDisk,15);assertEquals(List.of(),disk.pass());
        // Again, and Mininotes' version kept: written over the one on disk.
        store.replaceFile(plan.id,plan.kind,new ByteArrayInputStream(bytes("PK plan, phone again")),true);
        Files.write(planOnDisk,bytes("PK plan, Word again"));aged(planOnDisk,10);
        assertEquals(List.of(DesktopOnDisk.Did.BOTH),did(disk.pass()));
        disk.settleFile(plan.id,false);
        assertArrayEquals(bytes("PK plan, phone again"),Files.readAllBytes(planOnDisk));assertEquals("",disk.state(plan.id));
    }

    @Test public void renamedIsFollowedAndNothingIsEverDeleted() throws Exception {
        NoteStore.Shelf work=store.addCollectionIn(Things.HOME,"Work");
        NoteStore.Held plan=store.addFileExact(work.id,"Plan.docx",null,new ByteArrayInputStream(bytes("PK plan")));
        NoteStore.Held old=store.addFileExact(work.id,"Old.pdf",null,new ByteArrayInputStream(bytes("%PDF old")));
        DesktopOnDisk.Link root=disk.keepFolder(work.id,out);
        Path planOnDisk=root.path().resolve("Plan.docx");aged(planOnDisk,60);aged(root.path().resolve("Old.pdf"),60);
        // Renamed in Explorer: the same file, under its new name, not a second one.
        Path renamed=root.path().resolve("Plan v2.docx");Files.move(planOnDisk,renamed);
        assertEquals(List.of(DesktopOnDisk.Did.FOUND),did(disk.pass()));
        assertEquals("Plan v2.docx",store.file(plan.id).name);assertEquals(2,store.filesOf(NoteStore.Branch.Kind.COLLECTION,work.id).size());
        // Renamed in Mininotes: the file on disk takes the name.
        store.renameFile(plan.id,"Plan final.docx");
        disk.pass();
        assertTrue(Files.isRegularFile(root.path().resolve("Plan final.docx")));assertFalse(Files.exists(renamed));
        // Deleted on disk: lost, and still in Mininotes.
        Files.delete(root.path().resolve("Plan final.docx"));
        assertEquals(List.of(DesktopOnDisk.Did.LOST),did(disk.pass()));
        assertNotNull(store.file(plan.id));assertEquals(DesktopOnDisk.GONE,disk.state(plan.id));
        // Deleted in Mininotes: the file on disk stays, and is not taken in again.
        store.drop(old.id);
        disk.pass();
        assertTrue(Files.isRegularFile(root.path().resolve("Old.pdf")));
        assertEquals(List.of(),disk.pass());
        assertNull(named(work.id,"Old.pdf"));
    }

    @Test public void aWindowsFolderAlreadyThereIsTakenInWhole() throws Exception {
        Path mine=Files.createDirectories(out.resolve("Taxes"));
        Files.write(mine.resolve("Return.pdf"),bytes("%PDF return"));aged(mine.resolve("Return.pdf"),60);
        Files.createDirectories(mine.resolve("Receipts"));Files.write(mine.resolve("Receipts/Shop.jpg"),bytes("JPEG shop"));aged(mine.resolve("Receipts/Shop.jpg"),60);
        DesktopOnDisk.Link root=disk.syncFolder(mine);
        assertEquals(mine,root.path());assertEquals("Taxes",store.nameOf(root.id(),true));
        assertNotNull(named(root.id(),"Return.pdf"));
        String receipts=disk.links().stream().filter(l->l.folder()&&l.path().equals(mine.resolve("Receipts"))).findFirst().orElseThrow().id();
        assertArrayEquals(bytes("JPEG shop"),store.bytesOf(named(receipts,"Shop.jpg")));
        // Nothing on disk was renamed, moved or rewritten.
        assertArrayEquals(bytes("%PDF return"),Files.readAllBytes(mine.resolve("Return.pdf")));
        try{disk.syncFolder(mine.resolve("Receipts"));fail("kept twice");}catch(IllegalStateException once){/* said */}
        try{disk.syncFolder(mine.getRoot());fail("a whole drive");}catch(IllegalStateException refused){/* said */}
    }

    @Test public void oneFileSharedThroughMininotesStaysWhereItIs() throws Exception {
        Path sheet=out.resolve("Budget.ods");Files.write(sheet,bytes("PK ods one"));aged(sheet,60);
        DesktopOnDisk.Link l=disk.syncFile(sheet);
        assertEquals(sheet,l.path());assertEquals("",l.under());
        NoteStore.Held kept=named(Things.HOME,"Budget.ods");
        assertNotNull("on Home",kept);assertArrayEquals(bytes("PK ods one"),store.bytesOf(kept));
        assertEquals(List.of(),disk.pass());
        // Saved in LibreOffice: the new version taken in; the file never moved.
        Files.write(sheet,bytes("PK ods two"));aged(sheet,30);
        assertEquals(List.of(DesktopOnDisk.Did.FILE_TAKEN),did(disk.pass()));
        assertArrayEquals(bytes("PK ods two"),store.bytesOf(store.file(kept.id)));
        // Another device's version arriving: written into it where it is.
        aged(sheet,25);disk.pass();
        store.replaceFile(kept.id,kept.kind,new ByteArrayInputStream(bytes("PK ods from the phone")),true);
        assertEquals(List.of(DesktopOnDisk.Did.WROTE),did(disk.pass()));
        assertArrayEquals(bytes("PK ods from the phone"),Files.readAllBytes(sheet));
        assertTrue(disk.watched().contains(out));
        // Refused: twice, a Markdown file, a file not there.
        try{disk.syncFile(sheet);fail("twice");}catch(IllegalStateException once){/* said */}
        Files.writeString(out.resolve("Note.md"),"x");
        try{disk.syncFile(out.resolve("Note.md"));fail("a note");}catch(IllegalStateException note){/* said */}
        try{disk.syncFile(out.resolve("Gone.ods"));fail("not there");}catch(IllegalStateException gone){/* said */}
    }

    @Test public void syncSwitchedOffAndOnAndNothingFollowedInTheBin() throws Exception {
        Path sheet=out.resolve("Plan.ods");Files.write(sheet,bytes("PK one"));aged(sheet,60);
        DesktopOnDisk.Link l=disk.syncFile(sheet);
        assertTrue(disk.linkedFile(l.id()));
        // Off: a save on this PC is not taken in, and the link stays, to be switched on again.
        disk.syncing(l.id(),false);
        assertTrue(disk.syncOff(l.id()));assertNull("no lost mark",disk.state(l.id()));assertEquals(sheet,disk.pathOf(l.id()));
        Files.write(sheet,bytes("PK two"));aged(sheet,40);
        assertEquals(List.of(),disk.pass());
        assertArrayEquals(bytes("PK one"),store.bytesOf(store.file(l.id())));
        // On again: what was saved meanwhile is taken in.
        disk.syncing(l.id(),true);
        assertEquals(List.of(DesktopOnDisk.Did.FILE_TAKEN),did(disk.pass()));
        assertArrayEquals(bytes("PK two"),store.bytesOf(store.file(l.id())));
        // In the bin (Remove from Mininotes): nothing followed, the file on this PC untouched; put back, followed again.
        store.putAway(NoteStore.Branch.Kind.FILE,l.id(),true,true);
        Files.write(sheet,bytes("PK three"));aged(sheet,30);
        assertEquals(List.of(),disk.pass());
        assertArrayEquals(bytes("PK three"),Files.readAllBytes(sheet));assertArrayEquals(bytes("PK two"),store.bytesOf(store.file(l.id())));
        store.putAway(NoteStore.Branch.Kind.FILE,l.id(),true,false);
        assertEquals(List.of(DesktopOnDisk.Did.FILE_TAKEN),did(disk.pass()));
        assertArrayEquals(bytes("PK three"),store.bytesOf(store.file(l.id())));
    }

    @Test public void whatIsAFileOfTheFolder() {
        assertTrue(DesktopOnDisk.plainFile("Report.docx"));assertTrue(DesktopOnDisk.plainFile("photo.JPG"));assertTrue(DesktopOnDisk.plainFile("README"));
        for(String no:new String[]{"Note.md","~$Report.docx",".~lock.Report.docx#","~WRL0001.tmp","film.mp4.part","setup.exe.crdownload","desktop.ini","Thumbs.db",".hidden"})
            assertFalse(no,DesktopOnDisk.plainFile(no));
    }
}
