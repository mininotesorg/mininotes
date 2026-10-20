package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

public class DesktopRecoveryTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Context context;private NoteStore store;
    @Before public void start() throws Exception{context=new Context(temp.newFolder().toPath().toFile());store=new NoteStore(context);store.getWritableDatabase();}
    @After public void close(){store.close();}
    private NoteStore.Note note(String body){NoteStore.Note n=new NoteStore.Note();n.body=body;n.book=Things.HOME;store.save(n);return n;}
    @Test public void boundariesAndCountsAreSeparateAndNothingInvalidBecomesANote(){
        NoteStore.Note n=note("");n=DesktopEdits.save(store,n,"t".repeat(160),"x".repeat(24000));assertEquals(24000,store.get(n.id).body.length());
        var title=DesktopRecovery.save(store,n,"t".repeat(161),n.body,null,null);assertTrue(title.draft().reason().contains("Title has 161"));
        var body=DesktopRecovery.save(store,n,n.title,"x".repeat(24001),null,null);assertTrue(body.draft().reason().contains("Note text has 24001"));
        assertEquals(160,store.get(n.id).title.length());assertEquals(24000,store.get(n.id).body.length());assertEquals(2,DesktopRecovery.all(store).size());
        assertTrue(store.backup().contains("mininotes.v1"));assertFalse(store.backup().contains("desktop-writing"));
    }
    @Test public void fullUnicodeWritingBaseAndAuthorsSurviveRestartAndExport() throws Exception {
        NoteStore.Note n=note("base\n茶");String body="😀茶".repeat(9000);Writers authors=Writers.of(Writers.ME,body.length());
        var draft=DesktopRecovery.save(store,n,"Long writing",body,authors,null).draft();store.close();store=new NoteStore(context);
        var read=DesktopRecovery.get(store,n.id);assertEquals(body,read.body());assertEquals(n.body,read.base().body);assertEquals(n.revision,read.base().revision);assertEquals(authors.write(),read.writers().write());
        Path out=temp.getRoot().toPath().resolve("writing.txt");DesktopRecovery.export(read,out);assertEquals("Long writing\n\n"+body,Files.readString(out));
        assertTrue(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>body.length());assertEquals(draft.token(),read.token());
    }
    @Test public void mergedOverflowKeepsBothInputsWithoutSavingAnInvalidMergedRow(){
        NoteStore.Note base=note("first\nlast");
        String remote="r".repeat(12500)+"\nfirst\nlast";DesktopEdits.save(store,base,"",remote);
        String local="first\nlast\n"+"l".repeat(12500);
        var result=DesktopRecovery.save(store,base,"",local,null,null);assertNotNull(result.draft());assertEquals(local,result.draft().body());assertEquals(remote,store.get(base.id).body);
        assertNotNull(DesktopEdits.limits("",Merge.merge(result.draft().base().body,result.draft().body(),remote).text));
    }
    @Test public void shortenedDraftMergesConcurrentArrivalsAndClearsOnlyItsToken(){
        NoteStore.Note base=note("one\ntwo\nthree");var draft=DesktopRecovery.save(store,base,"","x".repeat(24001),null,null).draft();
        DesktopEdits.save(store,base,"","ONE\ntwo\nthree");
        var newer=DesktopRecovery.keep(store,base,"","newer full writing",null,"local");
        var saved=DesktopRecovery.save(store,draft.base(),"","one\ntwo\nTHREE",null,draft.token());
        assertEquals("ONE\ntwo\nTHREE",saved.note().body);assertEquals(1,DesktopRecovery.all(store).size());assertEquals(newer.token(),DesktopRecovery.get(store,base.id).token());
    }
    @Test public void failedDraftWriteRollsBackAndLeavesTheNoteAndEarlierDraft(){
        NoteStore.Note n=note("saved");var old=DesktopRecovery.keep(store,n,"","old full draft",null,"local");
        store.getWritableDatabase().execSQL("CREATE TRIGGER refuse_recovery BEFORE INSERT ON desktop_recovery BEGIN SELECT RAISE(ABORT,'test full disk'); END");
        assertThrows(IllegalStateException.class,()->DesktopRecovery.save(store,n,"","x".repeat(24001),null,old.token()));
        assertEquals("saved",store.get(n.id).body);assertEquals(old.token(),DesktopRecovery.get(store,n.id).token());
    }
    @Test public void notebookRekeyProtectsRecoveryAndLockedWritingBackupRestoresIt() throws Exception {
        NoteStore.Note n=note("saved");String full="secret synthetic writing"+"x".repeat(24001);DesktopRecovery.keep(store,n,"Draft",full,null,"long");
        byte[] key=new byte[32];Arrays.fill(key,(byte)7);store.getWritableDatabase().rekey(key);context.unlock(key);
        Path db=context.getFilesDir().toPath().resolve("mininotes.db");store.close();assertFalse(new String(Files.readAllBytes(db),java.nio.charset.StandardCharsets.ISO_8859_1).contains("secret synthetic writing"));
        store=new NoteStore(context);assertEquals(full,DesktopRecovery.get(store,n.id).body());
        Vault.Made lock=Vault.make("synthetic key".toCharArray(),20000);Path backup=temp.getRoot().toPath().resolve("writing.mnwriting");DesktopWritingBackup.write(store,backup,lock.key,lock.kept);
        assertFalse(new String(Files.readAllBytes(backup),java.nio.charset.StandardCharsets.ISO_8859_1).contains("secret synthetic writing"));
        var d=DesktopRecovery.get(store,n.id);DesktopRecovery.discard(store,n.id,d.token());assertEquals(1,DesktopWritingBackup.restore(store,backup,lock.key));assertEquals(full,DesktopRecovery.get(store,n.id).body());
        assertThrows(Vault.Refused.class,()->DesktopWritingBackup.restore(store,backup,new byte[32]));assertEquals(1,DesktopRecovery.all(store).size());
    }
    @Test public void oversizedStoredTextIsPreservedAndCanEnterRecovery(){
        NoteStore.Note n=note("received".repeat(4000));assertEquals(32000,store.get(n.id).body.length());
        var d=DesktopRecovery.save(store,n,"",n.body+"\nlocal",null,null).draft();assertEquals(n.body+"\nlocal",d.body());assertEquals(n.body,store.get(n.id).body);
    }
    @Test public void readOnlyArrivalKeepsAlreadyTypedWritingWithoutChangingTheSharedNote(){
        NoteStore.Note n=note("before");NoteStore.Note local=n.copy();n.theirs=true;n.writes=false;n.body="from sender";store.save(n);
        assertTrue((Boolean)store.readOnlyHere(n.id)[0]);
        var result=DesktopRecovery.save(store,local,"My local title","already typed here",null,null);
        assertNotNull(result.draft());assertEquals("already typed here",result.draft().body());assertEquals("from sender",store.get(n.id).body);assertTrue(store.owed(NoteStore.Branch.Kind.PAGE,n.id).isEmpty());
    }
    @Test public void ordinaryBackupCannotSilentlyOmitRecoveryWriting() throws Exception {
        NoteStore.Note n=note("saved");DesktopRecovery.keep(store,n,"Draft","complete local writing",null,"local");
        Path target=temp.getRoot().toPath().resolve("backup.zip");
        assertThrows(IllegalStateException.class,()->DesktopBackup.write(store,target));assertFalse(Files.exists(target));
        DesktopBackup.writeSaved(store,target,null,null,null);assertTrue(Files.isRegularFile(target));assertEquals(1,DesktopRecovery.all(store).size());
    }
    @Test public void restoringWritingNeverReplacesTheSavedNotebookOrOtherDrafts(){
        NoteStore.Note n=note("saved");var a=DesktopRecovery.keep(store,n,"A","first",null,"local");String backup=DesktopRecovery.backup(store);
        var b=DesktopRecovery.keep(store,n,"B","second",null,"local");assertEquals(1,DesktopRecovery.restore(store,backup));assertEquals(2,DesktopRecovery.all(store).size());
        assertEquals("saved",store.get(n.id).body);
        String changed=backup.replace("first","tampered");DesktopRecovery.restore(store,changed);assertEquals("first",DesktopRecovery.token(store,a.token()).body());
        DesktopRecovery.discard(store,n.id,a.token());assertEquals(b.token(),DesktopRecovery.get(store,n.id).token());
    }
}
