package org.mininotes.android;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.List;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/**
 * The owner's Martin note, 2026-10-18 (decisions 193 and 195): one from the owner's own device is the owner's to move; what
 * is only shown in a shared folder is put in it for real, another person's as a copy with its files; and a note's file
 * taken out goes to the bin and back. Made-up notes and devices.
 */
public class SharedFolderHoldsAllTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String MY_PHONE="MxMyPhoneFixture@127.0.0.1:9971",CAROL="MxCarolFixture@127.0.0.1:9972";

    private static String note(NoteStore store,String book,String title){
        NoteStore.Note n=new NoteStore.Note();n.book=book;n.title=title;n.body="Synthetic "+title;n.revision=1;store.save(n);return n.id;
    }
    private static void cameFrom(NoteStore store,String id,String origin){
        store.getWritableDatabase().execSQL("UPDATE notes SET theirs=1, origin=? WHERE id=?",new Object[]{origin,id});
    }

    @Test public void ownDevicesMoveSharedFoldersHoldEverythingAndFilesGoToTheBin() throws Exception {
        try(NoteStore store=new NoteStore(new Context(temp.newFolder("pc")))) {
            store.pairedWith(MY_PHONE,"My phone",true,new byte[]{1},new byte[]{2});
            store.pairedWith(CAROL,"Carol",false,new byte[]{3},new byte[]{4});
            String working=store.addCollectionIn(Things.HOME,"Working").id,friends=store.addCollectionIn(Things.HOME,"Friends").id;
            String martin=note(store,working,"Martin"),theirs=note(store,working,"Carol's list");
            cameFrom(store,martin,MY_PHONE);cameFrom(store,theirs,CAROL);
            assertFalse("from my own phone: mine to move",store.someoneElses(NoteStore.Branch.Kind.PAGE,martin));
            assertTrue("from Carol: only shown elsewhere",store.someoneElses(NoteStore.Branch.Kind.PAGE,theirs));
            // Both shown in Friends, as a move did before; Carol's carries a file.
            NoteStore.Held file=store.opening(NoteStore.Branch.Kind.PAGE,theirs,"plan.txt","text/plain",3);
            Files.write(store.fileFor(file.id).toPath(),new byte[]{1,2,3});store.keep(file);
            store.showIn(NoteStore.Branch.Kind.PAGE,martin,friends);store.showIn(NoteStore.Branch.Kind.PAGE,theirs,friends);
            assertTrue("Friends shared with nobody: nothing changes",store.intoSharedFolders().isEmpty());
            // Friends shared: everything in it is in it for real.
            store.give(Sharing.Scope.THING,friends,CAROL,Sharing.Level.WRITE,"Carol");
            List<String> made=store.intoSharedFolders();
            assertEquals(2,made.size());
            assertEquals("Martin moved for real",friends,store.get(martin).book);
            assertEquals("",store.shownIn(NoteStore.Branch.Kind.PAGE,martin));
            String copy=made.get(0).equals(martin)?made.get(1):made.get(0);
            assertEquals("Carol's copied into Friends",friends,store.get(copy).book);
            assertEquals("Carol's list",store.get(copy).title);
            assertEquals("with its file",1,store.filesOf(NoteStore.Branch.Kind.PAGE,copy).size());
            assertEquals("hers back where it is",working,store.get(theirs).book);assertEquals("",store.shownIn(NoteStore.Branch.Kind.PAGE,theirs));
            assertTrue("and done once",store.intoSharedFolders().isEmpty());
            // A note's file taken out: in the bin, out of its strip, back from the bin.
            store.putAway(NoteStore.Branch.Kind.FILE,file.id,true,true);
            assertTrue(store.notAway(store.filesOf(NoteStore.Branch.Kind.PAGE,theirs)).isEmpty());
            boolean inBin=false;for(NoteStore.Branch one:store.heldIn(true))inBin|=one.id.equals(file.id);
            assertTrue("listed in the bin",inBin);
            store.restore(NoteStore.Branch.Kind.FILE,file.id);
            assertEquals(1,store.notAway(store.filesOf(NoteStore.Branch.Kind.PAGE,theirs)).size());
        }
    }
}
