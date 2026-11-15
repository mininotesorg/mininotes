package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A backup the Backup page makes (decision 329): sealed whole with the lock made for it, opened again with its password or its
 * 12 words; nothing made when nothing changed; the newest kept and older ones of this PC removed, nothing else; a folder gone
 * said so. Made-up notes only.
 */
public class DesktopBackupsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Context context;private NoteStore store;private Path folder;
    @Before public void start() throws Exception {
        context=new Context(temp.newFolder().toPath().toFile());store=new NoteStore(context);store.getWritableDatabase();
        folder=temp.newFolder("backups").toPath();
    }
    @After public void close(){store.close();}
    private Backups.Place place(Vault.Made made) {
        Backups.Place p=new Backups.Place();p.where=folder.toString();p.shown=p.where;p.opens=Backups.Opens.PASSWORD;
        p.lock=Base64.getEncoder().encodeToString(made.kept);store.setSecret(Backups.secretOf(p.id),Base64.getEncoder().encodeToString(made.key));
        return p;
    }
    private List<Path> made() throws Exception{List<Path> out=new ArrayList<>();try(var in=Files.newDirectoryStream(folder,"mininotes-backup-*.mnbackup")){for(Path one:in)out.add(one);}Collections.sort(out);return out;}
    private void note(String body){NoteStore.Note n=new NoteStore.Note();n.title="Synthetic";n.body=body;n.book=Things.HOME;store.save(n);}

    @Test public void sealedWholeAndOpenedWithItsPasswordOrItsWords() throws Exception {
        note("a synthetic line nobody wrote");
        Vault.Made made=Vault.make("synthetic password".toCharArray(),20000);Backups.Place p=place(made);
        assertEquals(Backups.Said.MADE,DesktopBackups.make(context,store,p,true,null));
        Path one=made().get(0);
        assertTrue(DesktopBackup.locked(one));
        assertFalse("never plain inside",new String(Files.readAllBytes(one),StandardCharsets.ISO_8859_1).contains("a synthetic line nobody wrote"));
        byte[] lock=Base64.getDecoder().decode(p.lock);
        assertArrayEquals(made.key,Vault.open(lock,"synthetic password".toCharArray()));
        assertArrayEquals(made.key,Vault.recover(lock,String.join(" ",made.words)));
        Context other=new Context(temp.newFolder().toPath().toFile());
        try(NoteStore elsewhere=new NoteStore(other)) {
            elsewhere.getWritableDatabase();
            DesktopBackup.add(elsewhere,one,l->Vault.open(l,"synthetic password".toCharArray()));
            boolean found=false;for(NoteStore.Note n:elsewhere.pages(Things.HOME))if(elsewhere.get(n.id).body.equals("a synthetic line nobody wrote"))found=true;
            assertTrue("put back on another PC",found);
        }
    }
    @Test public void nothingChangedNothingMadeUnlessAsked() throws Exception {
        note("one");Backups.Place p=place(Vault.make("synthetic password".toCharArray(),20000));
        assertEquals(Backups.Said.MADE,DesktopBackups.make(context,store,p,false,null));
        Thread.sleep(1100);
        assertEquals(Backups.Said.SAME,DesktopBackups.make(context,store,p,false,null));
        assertEquals(1,made().size());
        note("two");Thread.sleep(1100);
        assertEquals(Backups.Said.MADE,DesktopBackups.make(context,store,p,false,null));
        assertEquals(2,made().size());
    }
    @Test public void theNewestAreKeptAndNothingElseInTheFolderIsTouched() throws Exception {
        Files.writeString(folder.resolve("holiday.txt"),"keep me");
        Files.writeString(folder.resolve("mininotes-backup-Other-PC-20200101-000000.mnbackup"),"another device's");
        Backups.Place p=place(Vault.make("synthetic password".toCharArray(),20000));p.keep=2;
        for(int i=0;i<4;i++){note("n"+i);assertEquals(Backups.Said.MADE,DesktopBackups.make(context,store,p,true,null));Thread.sleep(1100);}
        String mine=Backups.START+Backups.safe(Node.nameHere(context))+"-";
        long own=made().stream().filter(x->x.getFileName().toString().startsWith(mine)).count();
        assertEquals(2,own);
        assertTrue(Files.exists(folder.resolve("holiday.txt")));
        assertTrue(Files.exists(folder.resolve("mininotes-backup-Other-PC-20200101-000000.mnbackup")));
    }
    @Test public void withoutFilesAndAFolderGoneAndNoKey() throws Exception {
        note("words");Backups.Place p=place(Vault.make("synthetic password".toCharArray(),20000));p.what=Backups.What.NO_FILES;
        assertEquals(Backups.Said.MADE,DesktopBackups.make(context,store,p,true,null));
        p.where=folder.resolve("not there").toString();
        assertEquals(Backups.Said.NO_FOLDER,DesktopBackups.make(context,store,p,true,null));
        p.where=folder.toString();store.setSecret(Backups.secretOf(p.id),"");
        assertEquals(Backups.Said.NO_KEY,DesktopBackups.make(context,store,p,true,null));
        p.opens=Backups.Opens.NOTEBOOK;
        assertEquals("the notebook's lock while it is not locked",Backups.Said.NO_KEY,DesktopBackups.make(context,store,p,true,null));
    }
    /** Kept as words and read back the same (org.json is the PC's real one here; the phone's unit tests have none). */
    @Test public void placesAreKeptAndReadBackAndNothingReadableIsNone() {
        Backups.Place p=new Backups.Place();p.where="D:\\Backups";p.shown="D:\\Backups";p.what=Backups.What.NO_FILES;p.often=Backups.Often.WEEKLY;
        p.weekday=3;p.hour=7;p.keep=10;p.opens=Backups.Opens.WORDS;p.lock="AAEC";p.last=5;p.said=Backups.Said.MADE;p.fingerprint="ab";
        Backups.Place back=Backups.read(Backups.said(List.of(p))).get(0);
        assertEquals(p.json().toString(),back.json().toString());
        assertTrue(Backups.read("not json").isEmpty());assertTrue(Backups.read("").isEmpty());
    }
    /** The switch on each place (the owner, 2026-11-07): kept as it was set; a place kept before the switch existed is on. */
    @Test public void aPlaceSwitchedOffStaysOffAndOneFromBeforeTheSwitchIsOn() {
        Backups.Place p=new Backups.Place();p.on=false;
        assertFalse(Backups.read(Backups.said(List.of(p))).get(0).on);
        org.json.JSONObject before=p.json();before.remove("on");
        assertTrue(Backups.Place.read(before).on);
    }
}
