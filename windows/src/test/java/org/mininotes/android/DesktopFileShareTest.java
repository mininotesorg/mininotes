package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import com.eurobuddha.maxima.core.media.MediaManifest;
import com.eurobuddha.maxima.core.media.MediaService;
import com.eurobuddha.maxima.core.store.BlobStore;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A file shared on its own, like a note (the owner, 2026-10-04; docs/HOME.md, decision 92), in the real notebook and
 * through {@code Post.arrived}, with synthetic devices and no network: each message is sealed and handed to the device it is
 * for, and the pieces are left on one shelf both can reach, as a relay holds them. A file on A's Home is given to B to read;
 * B keeps it on Home, shown in Shared with me, as theirs; A deleting it for good deletes it at B, and a sleeve that set off
 * before does not bring it back; taking B off leaves B its own copy; B leaving is heard at A. And the gate: nothing is ever
 * sealed for a device that has not said it knows files on their own. And since 0.2.027 (decision 93): a writer renames and
 * replaces it for everybody, a reader cannot; it reaches the owner's and the receiver's own other devices; and a device that
 * has it first from an admin keeps its owner as its owner.
 */
public class DesktopFileShareTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();NoteStore.fileKey=NoteStore::key;}

    private static final String A_AT="MxFileOwnerFixture@127.0.0.1:9701",B_AT="MxFileReaderFixture@127.0.0.1:9702",
        C_AT="MxFileOldBuildFixture@127.0.0.1:9703",D_AT="MxFileFourthFixture@127.0.0.1:9704",
        A2_AT="MxFileOwnersOtherFixture@127.0.0.1:9705",B2_AT="MxFileReadersOtherFixture@127.0.0.1:9706";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        /** Another device of the same owner, bonded as Connect my other device bonds them: My device, and everything shared. */
        void bond(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
            store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,other.at,true));
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        /** A file kept loose on Home. */
        String onHome(byte[] bytes,String name) throws Exception {
            String id=UUID.randomUUID().toString();
            Files.write(store.fileFor(id).toPath(),bytes);
            store.keep(new NoteStore.Held(id,Things.HOME,name,"image/png",bytes.length,System.currentTimeMillis(),NoteStore.Branch.Kind.COLLECTION));
            return id;
        }
        /** Its sleeve as it goes to another device now, not asking to be answered: that would start a node. */
        byte[] sleeve(Device to,String file) throws Exception {
            return sealed(to,file,store.sleeve(file,to.at).quiet(),store.fileRevision(file));
        }
        /** Any sleeve, as this device would seal it: what a changed build could send. */
        byte[] sealed(Device to,String file,Sleeve.Sent s,long revision) throws Exception {
            return Envelope.seal(page(file),revision,System.currentTimeMillis(),Sleeve.wrap(s,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        /** What this device holds of a file, as its bytes are. */
        byte[] bytes(String file) throws Exception {return store.bytesOf(store.file(file));}
        byte[] said(Device to,String file,long revision,int what) throws Exception {
            return Envelope.seal(page(file),revision,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device a,b;private File shelf;
    @Before public void two() throws Exception {
        Hashes.setSha3(Sha3::of);
        a=new Device("Test PC",A_AT);b=new Device("Test phone",B_AT);
        a.pair(b,"Test phone");b.pair(a,"Test PC");
        shelf=temp.newFolder("relay");
    }

    private void goesUp(Device from,String file) throws Exception {
        MediaManifest made=new MediaService(null,new BlobStore(shelf)).publish(from.store.bytesOf(from.store.file(file)),"image/png");
        from.store.published(file,made.encode());
    }

    private int fetches(Device to) throws Exception {
        int kept=0;
        for(NoteStore.Incoming one:to.store.toFetch(System.currentTimeMillis())) {
            byte[] plain=new MediaService(null,new BlobStore(shelf)).fetch(MediaManifest.decode(one.manifest));
            if(to.store.fileArrived(one,plain))kept++;
        }
        return kept;
    }

    private static boolean drawnIn(NoteStore store,String place,String file) {
        for(NoteStore.Branch one:store.contents(place))if(one.kind==NoteStore.Branch.Kind.FILE&&one.id.equals(file))return true;
        return false;
    }

    /** A file on A's Home shared with B to read: there, theirs, kept on Home and shown in Shared with me. */
    private String shared(byte[] bytes) throws Exception {return shared(bytes,Sharing.Level.READ);}
    private String shared(byte[] bytes,Sharing.Level level) throws Exception {
        String file=a.onHome(bytes,"plan.png");
        a.store.give(Sharing.Scope.FILE,file,B_AT,level,"Test phone");
        // Shared, so it is to go up, as nothing on Home did before.
        List<NoteStore.Going> up=a.store.toPublish(System.currentTimeMillis());
        assertEquals(1,up.size());assertEquals(file,up.get(0).file.id);
        goesUp(a,file);
        assertTrue(a.store.toPublish(System.currentTimeMillis()).isEmpty());
        assertEquals("its sleeve is owed to B",1,a.store.sleevesOwed(file,false).stream().filter(w->w.address.equals(B_AT)).count());
        b.hear(a.sleeve(b,file));
        assertEquals("waited for, then fetched",1,fetches(b));
        return file;
    }

    @Test public void aFileSharedOnItsOwnIsKeptOnHomeShownInSharedWithMeAndReadOnly() throws Exception {
        byte[] bytes=new byte[30_000];new Random(3).nextBytes(bytes);
        String file=shared(bytes);
        NoteStore.Held there=b.store.file(file);
        assertNotNull(there);
        assertTrue("kept on Home, so it never travels on with a collection",NoteStore.home(there.note));
        assertEquals(A_AT,b.store.originOf(file));
        assertEquals("theirs",A_AT,b.store.cameFrom(NoteStore.Branch.Kind.FILE,file));
        assertEquals(Sharing.Level.READ,b.store.myLevel(Sharing.Scope.FILE,file));
        assertArrayEquals(bytes,b.store.bytesOf(there));
        // Shown in the place Shared with me (decision 94), which no collection stands for.
        String place=NoteStore.SHARED;
        for(NoteStore.Shelf one:b.store.collections())assertNotEquals("no collection made for it",NoteStore.SHARED_WITH_ME,one.name);
        assertTrue(drawnIn(b.store,place,file));
        assertFalse("not on Home as well",drawnIn(b.store,Things.HOME,file));
        // B may only read it, so B owes nobody anything, and may give nobody anything.
        assertTrue(b.store.sleevesOwed(file,true).isEmpty());
        assertTrue(b.store.mayGive(Sharing.Scope.FILE,file).isEmpty());
        // A owns it: an admin, a writer or a reader, as for a note (decision 93). B may change nothing of it.
        assertEquals(List.of(Sharing.Level.ADMIN,Sharing.Level.WRITE,Sharing.Level.READ),a.store.mayGive(Sharing.Scope.FILE,file));
        assertFalse(b.store.mayChangeFile(file));
        try{b.store.renameFile(file,"mine now.png");fail("a reader renamed it");}catch(IllegalStateException refused){assertEquals(NoteStore.FILE_READ_ONLY,refused.getMessage());}
        try{b.store.replaceFile(file,"image/png",new java.io.ByteArrayInputStream(new byte[]{1}));fail("a reader replaced it");}
        catch(IllegalStateException refused){assertEquals(NoteStore.FILE_READ_ONLY,refused.getMessage());}
        assertEquals("plan.png",b.store.file(file).name);
        assertArrayEquals(bytes,b.bytes(file));
        // B's answer clears it at A, and it wears the mark of a shared thing there.
        a.hear(b.said(a,file,a.store.fileRevision(file),Receipt.TOOK));
        assertTrue(a.store.sleevesOwed(file,false).isEmpty());
        assertTrue(a.store.sharedAtAll(NoteStore.Branch.Kind.FILE,file));
        for(NoteStore.Branch one:a.store.contents(Things.HOME))if(one.id.equals(file))assertEquals(Sharing.State.OTHERS,one.state);
        // A place is never put away (decision 94): it stays shown there.
        assertTrue(drawnIn(b.store,NoteStore.SHARED,file));
    }

    @Test public void itsOwnerDeletingItDeletesItForEverybodyAndALateSleeveDoesNotBringItBack() throws Exception {
        String file=shared("synthetic picture".getBytes());
        byte[] early=a.sleeve(b,file);
        long before=a.store.fileRevision(file);
        // Deleted for good at A: hidden there at once, and gone for everybody at a later revision.
        a.store.erase(NoteStore.Branch.Kind.FILE,file);
        assertFalse(drawnIn(a.store,Things.HOME,file));
        assertTrue(a.store.fileRevision(file)>before);
        assertTrue(a.store.sleeve(file,B_AT).gone);
        assertEquals(1,a.store.sleevesOwed(file,false).size());
        b.hear(a.sleeve(b,file));
        assertNull("gone at B",b.store.file(file));
        assertFalse(b.store.fileFor(file).isFile());
        // A sleeve that set off before arrives late: nothing comes back.
        b.hear(early);
        assertNull(b.store.file(file));
        assertTrue(b.store.toFetch(System.currentTimeMillis()).isEmpty());
        // B's answer: everybody has heard, so A lets it go too.
        a.hear(b.said(a,file,a.store.fileRevision(file),Receipt.TOOK));
        assertNull(a.store.file(file));
        assertTrue(a.store.sleevesOwed(null,true).isEmpty());
    }

    @Test public void takenOffLeavesTheirCopyTheirOwn() throws Exception {
        String file=shared("synthetic picture".getBytes());
        long now=System.currentTimeMillis();
        Sharing.Rule them=null;for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.FILE,file))if(one.address.equals(B_AT))them=one;
        a.store.decide(them,Sharing.Level.GONE,now);
        assertTrue("nobody is owed it now",a.store.sleevesOwed(file,true).isEmpty());
        Post.Landed heard=b.hear(a.said(b,file,now,Receipt.REMOVED_FILE));
        assertTrue(heard.said,heard.said.contains("removed you from a file"));
        NoteStore.Held kept=b.store.file(file);
        assertNotNull("their copy stays",kept);
        assertEquals("and is theirs now","",b.store.cameFrom(NoteStore.Branch.Kind.FILE,file));
        assertNull(b.store.myLevel(Sharing.Scope.FILE,file));
        // A late sleeve from A is refused: B is off it.
        b.hear(a.sleeve(b,file));
        assertEquals("",b.store.cameFrom(NoteStore.Branch.Kind.FILE,file));
    }

    @Test public void leavingIsHeardByItsOwner() throws Exception {
        String file=shared("synthetic picture".getBytes());
        long when=System.currentTimeMillis();
        Post.Landed heard=a.hear(b.said(a,file,when,Receipt.LEFT_FILE));
        assertTrue(heard.said,heard.said.contains("unfollowed a file"));
        for(Sharing.Rule one:a.store.membership(Sharing.Scope.FILE,file))
            if(one.address.equals(B_AT))assertEquals(Sharing.Level.GONE,one.level);
        assertTrue(a.store.sleevesOwed(file,true).isEmpty());
        // B deleting its copy is leaving too: everybody who had it is told, again, for a week.
        b.store.erase(NoteStore.Branch.Kind.FILE,file);
        assertNull(b.store.file(file));
        boolean said=false;
        for(NoteStore.Leaving one:b.store.leavings())if(one.note.equals(file)&&one.scope==Sharing.Scope.FILE&&one.address.equals(A_AT))said=true;
        assertTrue(said);
        b.hear(a.sleeve(b,file));
        assertNull("and it does not come back",b.store.file(file));
    }

    /**
     * Decision 93: B, given it to write in, renames it and replaces it with a new version; both reach its owner A, and
     * through A a reader D, the new bytes only once fetched and under the same id. An older name or version arriving late
     * changes nothing, and a reader's sleeve saying another name is taken by nobody.
     */
    @Test public void aWriterRenamesAndReplacesItForEverybody() throws Exception {
        Device d=new Device("Fourth",D_AT);
        a.pair(d,"Fourth");d.pair(a,"Test PC");
        byte[] first=new byte[20_000];new Random(5).nextBytes(first);
        String file=shared(first,Sharing.Level.WRITE);
        a.store.give(Sharing.Scope.FILE,file,D_AT,Sharing.Level.READ,"Fourth");
        d.hear(a.sleeve(d,file));
        assertEquals(1,fetches(d));
        assertTrue("a writer may change it",b.store.mayChangeFile(file));
        assertFalse(d.store.mayChangeFile(file));
        assertTrue("and nothing is owed from it until it does",b.store.sleevesOwed(file,false).isEmpty());

        // Renamed at B: owed to A, who takes it and is moved on, so it goes on to D.
        b.store.renameFile(file,"plan final.png");
        assertEquals("plan final.png",b.store.file(file).name);
        assertEquals(1,b.store.sleevesOwed(file,false).size());
        byte[] renamedOnce=b.sleeve(a,file);
        b.store.renameFile(file,"plan final 2.png");
        a.hear(b.sleeve(a,file));
        assertEquals("plan final 2.png",a.store.file(file).name);
        a.hear(renamedOnce);
        assertEquals("an older name arriving late changes nothing","plan final 2.png",a.store.file(file).name);
        d.hear(a.sleeve(d,file));
        assertEquals("plan final 2.png",d.store.file(file).name);

        // A reader's word is nobody's: D sends A another name, later than any, and A keeps its own.
        Sleeve.Sent was=d.store.sleeve(file,A_AT);
        Sleeve.Sent forged=new Sleeve.Sent(file,"mine now.png",was.kind,was.bytes,was.manifest,false,was.scope,was.target,was.members,false,false,
            A_AT,"",System.currentTimeMillis()+60_000,0);
        a.hear(d.sealed(a,file,forged,d.store.fileRevision(file)+5));
        assertEquals("plan final 2.png",a.store.file(file).name);

        // Replaced at B: goes up again, and its sleeve then says where and when.
        byte[] second=new byte[25_000];new Random(6).nextBytes(second);
        byte[] before=b.sleeve(a,file);
        assertEquals(second.length,b.store.replaceFile(file,"image/png",new java.io.ByteArrayInputStream(second)));
        assertArrayEquals(second,b.bytes(file));
        assertEquals("not up yet, so its sleeve says no new version",0,b.store.sleeve(file,A_AT).replaced);
        List<NoteStore.Going> up=b.store.toPublish(System.currentTimeMillis());
        assertEquals("a writer sends its version up",1,up.size());
        goesUp(b,file);
        assertTrue(b.store.sleeve(file,A_AT).replaced>0);
        a.hear(b.sleeve(a,file));
        assertArrayEquals("the old bytes stay until the new are fetched",first,a.bytes(file));
        assertEquals(1,fetches(a));
        assertArrayEquals(second,a.bytes(file));
        assertEquals("the same file, under the same name","plan final 2.png",a.store.file(file).name);
        a.hear(before);
        assertArrayEquals("an older version arriving late changes nothing",second,a.bytes(file));
        assertTrue(a.store.toFetch(System.currentTimeMillis()).isEmpty());
        // And on to the reader, from A, who has it now.
        d.hear(a.sleeve(d,file));
        assertEquals(1,fetches(d));
        assertArrayEquals(second,d.bytes(file));
        assertEquals(1,d.store.contents(NoteStore.SHARED).size());
    }

    /**
     * Decision 93: a file goes to its sender's own other devices as every note does (A to A2), and a file shared with B
     * goes on to B's other device B2 as a note shared with B does, B only reading it; B2 keeps A as its owner, and A's
     * deleting it for everybody reaches B2 through B.
     */
    /**
     * Decision 95, as the owner met it: two screenshots let go on Temp on his phone never reached his laptop. His devices are
     * paired as his own, not bonded. A file made temporary there is given to them and goes, with its time, and lands on
     * Temp at the other end, not in Shared with me; somebody else it is shared with is never told the time. And decision
     * 98 ("On laptop, the Temp files are also shown on the desktop, where is the setting for that"): only in Temp, not on
     * Home, unless Also show on Home is on, which moves those already there; not temporary any more, it is on Home.
     */
    @Test public void aFileOnTempGoesToMyOtherDevicesWithItsTime() throws Exception {
        Device mine=new Device("Owners other",A2_AT);
        a.store.pairedWith(A2_AT,"Owners other",true,mine.keys.agreement().getPublic().getEncoded(),mine.keys.signing().getPublic().getEncoded());
        mine.store.pairedWith(A_AT,"Test PC",true,a.keys.agreement().getPublic().getEncoded(),a.keys.signing().getPublic().getEncoded());
        String file=a.onHome("synthetic screenshot".getBytes(),"Screenshot.png");
        long until=System.currentTimeMillis()+3_600_000L;
        a.store.makeTemporary(NoteStore.Branch.Kind.FILE,file,until);
        assertEquals("given to the other device of mine",1,a.store.toMyDevices(Sharing.Scope.FILE,file));
        a.store.give(Sharing.Scope.FILE,file,B_AT,Sharing.Level.READ,"Test phone");
        goesUp(a,file);
        Set<String> owed=new HashSet<>();for(Outbox.Wait one:a.store.sleevesOwed(file,true))owed.add(one.address);
        assertTrue(owed.contains(A2_AT));
        assertEquals("the time to my own device",until,a.store.sleeve(file,A2_AT).until);
        assertEquals("never to somebody else",0,a.store.sleeve(file,B_AT).until);
        mine.hear(a.sleeve(mine,file));
        assertEquals(1,fetches(mine));
        assertEquals("temporary there, for the same time",until,mine.store.untilOf(NoteStore.Branch.Kind.FILE,file));
        boolean onTemp=false;for(NoteStore.Branch one:mine.store.temporary(System.currentTimeMillis()))if(one.id.equals(file))onTemp=true;
        assertTrue("listed on Temp",onTemp);
        assertFalse("not on Home",drawnIn(mine.store,Things.HOME,file));
        assertFalse("not in Shared with me",drawnIn(mine.store,NoteStore.SHARED,file));
        assertFalse("off unless switched on",mine.store.tempOnHome());
        mine.store.setTempOnHome(true);
        assertTrue("on Home with the setting on",drawnIn(mine.store,Things.HOME,file));
        onTemp=false;for(NoteStore.Branch one:mine.store.temporary(System.currentTimeMillis()))if(one.id.equals(file))onTemp=true;
        assertTrue("and still listed on Temp",onTemp);
        mine.store.setTempOnHome(false);
        assertFalse("off again, in Temp only",drawnIn(mine.store,Things.HOME,file));
        mine.store.makeTemporary(NoteStore.Branch.Kind.FILE,file,0);
        assertTrue("not temporary any more: on Home",drawnIn(mine.store,Things.HOME,file));
        // A file of this device's own let go on Temp stays where it is, setting or not.
        assertTrue(drawnIn(a.store,Things.HOME,file));
    }

    /** The owner's report of 2026-10-05: a screenshot from the phone on Temp, opened on the PC, wore "waiting to go" there. */
    @Test public void aFileFromMyOtherDeviceIsNotOwedBackToIt() throws Exception {
        Device phone=new Device("Owners phone",A2_AT);
        a.bond(phone,"Owners phone");phone.bond(a,"Test PC");
        String file=phone.onHome("synthetic screenshot".getBytes(),"Screenshot.png");
        phone.store.makeTemporary(NoteStore.Branch.Kind.FILE,file,System.currentTimeMillis()+3_600_000L);
        phone.store.toMyDevices(Sharing.Scope.FILE,file);
        goesUp(phone,file);
        a.hear(phone.sleeve(a,file));
        assertEquals(1,fetches(a));
        Set<String> owed=new HashSet<>();for(Outbox.Wait one:a.store.sleevesOwed(file,false))owed.add(one.address);
        assertFalse("never owed back to the device it came from: "+owed,owed.contains(A2_AT));
        // A third device of the owner's has it from the phone, as the PC did: the PC owes it nothing either.
        Device third=new Device("Owners third",D_AT);
        a.bond(third,"Owners third");third.bond(a,"Test PC");phone.bond(third,"Owners third");third.bond(phone,"Owners phone");
        owed.clear();for(Outbox.Wait one:a.store.sleevesOwed(file,false))owed.add(one.address);
        assertTrue("the phone sends it to the third itself: "+owed,owed.isEmpty());
        // Right-clicked on the PC: who sent it, and where each of the others stands, as far as the PC knows.
        NoteStore.Standing there=a.store.standing(file);
        assertEquals(A2_AT,there.from);
        assertEquals("Sent it",there.said.get(A2_AT));
        assertEquals("Gets it from Owners phone",there.said.get(D_AT));
        assertFalse("not itself",there.said.containsKey(A_AT));
        // On the phone it came from: the PC has it once it says so; the third is still to have it from there.
        phone.hear(a.said(phone,file,phone.store.fileRevision(file),Receipt.TOOK));
        NoteStore.Standing sender=phone.store.standing(file);
        assertEquals("added there","",sender.from);
        assertEquals("Has it",sender.said.get(A_AT));
        assertEquals("Waiting",sender.said.get(D_AT));
    }

    @Test public void itGoesToTheOwnersOwnDevicesAndToTheReceiversOwn() throws Exception {
        Device a2=new Device("Owners other",A2_AT),b2=new Device("Readers other",B2_AT);
        a.bond(a2,"Owners other");a2.bond(a,"Test PC");
        b.bond(b2,"Readers other");b2.bond(b,"Test phone");
        String file=shared("synthetic picture".getBytes());
        Set<String> owed=new HashSet<>();for(Outbox.Wait one:a.store.sleevesOwed(file,true))owed.add(one.address);
        assertEquals("the owner's other device as well as B",Set.of(B_AT,A2_AT),owed);
        boolean listed=false;for(Parcel.Member one:a.store.sleeve(file,B_AT).members)if(A2_AT.equals(one.address))listed=true;
        assertTrue("and named on its list",listed);
        // B may only read it, and still owes it to its own other device, and to nobody else.
        List<Outbox.Wait> fromB=b.store.sleevesOwed(file,false);
        assertEquals(1,fromB.size());assertEquals(B2_AT,fromB.get(0).address);
        b2.hear(b.sleeve(b2,file));
        assertEquals(1,fetches(b2));
        assertEquals("its owner, though it came from B",A_AT,b2.store.cameFrom(NoteStore.Branch.Kind.FILE,file));
        assertEquals(Sharing.Level.READ,b2.store.myLevel(Sharing.Scope.FILE,file));
        assertTrue(drawnIn(b2.store,NoteStore.SHARED,file));
        // A deletes it for everybody: B lets it go, and tells B2, which has heard nothing from A.
        a.store.erase(NoteStore.Branch.Kind.FILE,file);
        b.hear(a.sleeve(b,file));
        assertFalse(drawnIn(b.store,NoteStore.SHARED,file));
        assertFalse(b.store.fileFor(file).isFile());
        assertEquals(1,b.store.sleevesOwed(file,false).size());
        b2.hear(b.sleeve(b2,file));
        assertFalse("gone at B2 too",drawnIn(b2.store,NoteStore.SHARED,file));
        assertFalse(b2.store.fileFor(file).isFile());
        b.hear(b2.said(b,file,b.store.fileRevision(file),Receipt.TOOK));
        assertNull("and let go at B once B2 has heard",b.store.file(file));
    }

    /**
     * Decision 93: C, an admin of A's file, hands it to D before A's own sleeve reaches D. D keeps A as its owner, as the
     * sleeve says, so A's deleting it for everybody is obeyed at D.
     */
    @Test public void anAdminHandsItOnAndItsOwnerIsStillItsOwner() throws Exception {
        Device c=new Device("Admin",C_AT),d=new Device("Fourth",D_AT);
        for(Device one:new Device[]{a,c,d})for(Device other:new Device[]{a,c,d})if(one!=other)one.pair(other,other.store.myName);
        String file=a.onHome("synthetic picture".getBytes(),"plan.png");
        a.store.give(Sharing.Scope.FILE,file,C_AT,Sharing.Level.ADMIN,"Admin");
        goesUp(a,file);
        c.hear(a.sleeve(c,file));
        assertEquals(1,fetches(c));
        assertEquals(Sharing.Level.ADMIN,c.store.myLevel(Sharing.Scope.FILE,file));
        c.store.give(Sharing.Scope.FILE,file,D_AT,Sharing.Level.READ,"Fourth");
        d.hear(c.sleeve(d,file));
        assertEquals(1,fetches(d));
        assertEquals("its owner, not the admin it came from",A_AT,d.store.originOf(file));
        assertEquals(A_AT,d.store.cameFrom(NoteStore.Branch.Kind.FILE,file));
        // A deletes it for everybody, and A knows D from C's list: gone at D.
        a.hear(c.sleeve(a,file));
        a.store.erase(NoteStore.Branch.Kind.FILE,file);
        Post.Landed heard=d.hear(a.sleeve(d,file));
        assertNull("gone at D",d.store.file(file));
        assertTrue(heard.said,heard.said.contains("deleted a file they shared with you"));
        // C's own sleeve, set off before, does not bring it back.
        d.hear(c.sleeve(d,file));
        assertNull(d.store.file(file));
    }

    /** Decision 93: Favourites, Temp and the archive's lines of a file shared on its own wear its mark, and a temporary thing its timer. */
    @Test public void itsMarkAndItsTimerShowWhereverItIsListed() throws Exception {
        String file=shared("synthetic picture".getBytes());
        a.store.keepToHand(NoteStore.Branch.Kind.FILE,file,true);
        a.store.makeTemporary(NoteStore.Branch.Kind.FILE,file,System.currentTimeMillis()+60_000);
        boolean seen=false;
        for(NoteStore.Branch one:a.store.favouritesAll())if(one.id.equals(file)){seen=true;assertEquals(Sharing.State.OTHERS,one.state);assertTrue(one.temporary);}
        assertTrue(seen);seen=false;
        for(NoteStore.Branch one:a.store.temporary(System.currentTimeMillis()))if(one.id.equals(file)){seen=true;assertEquals(Sharing.State.OTHERS,one.state);}
        assertTrue(seen);
        for(NoteStore.Branch one:a.store.contents(Things.HOME))if(one.id.equals(file))assertTrue("on Home, its timer",one.temporary);
        // Theirs at B, wherever it is listed.
        b.store.keepToHand(NoteStore.Branch.Kind.FILE,file,true);
        for(NoteStore.Branch one:b.store.favouritesAll())if(one.id.equals(file))assertEquals(Sharing.State.THEIRS,one.state);
        // And a note made temporary wears it too.
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Soon gone";a.store.save(note);
        a.store.makeTemporary(NoteStore.Branch.Kind.PAGE,note.id,System.currentTimeMillis()+60_000);
        for(NoteStore.Branch one:a.store.contents(Things.HOME))if(one.id.equals(note.id))assertTrue(one.temporary);
    }

    /** Moving a file is asked about along where it is kept, not where it is shown (decision 93). */
    @Test public void aFileShownElsewhereIsReachedAlongWhereItIsKept() throws Exception {
        String file=shared("synthetic picture".getBytes());
        String shown=NoteStore.SHARED;
        assertEquals("kept on Home, though it shows in Shared with me",List.of(file),b.store.filePath(file));
        assertNotEquals(shown,b.store.filePath(file).get(0));
        assertTrue(Sharing.fileMoveSaid(true).contains("on its own keep it"));
        assertFalse(Sharing.fileMoveSaid(false).contains("on its own"));
    }

    @Test public void nothingIsSealedForADeviceThatHasNotSaidItKnowsFilesOnTheirOwn() throws Exception {

        Device old=new Device("Old build",C_AT);
        a.pair(old,"Old build");old.pair(a,"Test PC");
        String file=a.onHome("synthetic picture".getBytes(),"plan.png");
        a.store.give(Sharing.Scope.FILE,file,C_AT,Sharing.Level.READ,"Old build");
        goesUp(a,file);
        assertEquals(Set.of(B_AT,C_AT),a.store.beforeLoose());
        // Owed, and not sent: said to wait for the device to be updated, and no node asked for, since nothing can go.
        Post.Done done=Post.sleeves(a.context,a.store,a.keys,file,null,false);
        assertEquals(0,done.sent);assertEquals(1,done.failed);
        assertEquals(Unsent.Why.NEEDS_UPDATE,done.problems.get(0).why);
        assertTrue(a.store.handed().isEmpty());
        assertEquals(SyncMark.WAITING,a.store.markOf(NoteStore.Branch.Kind.FILE,file));
        // It says it knows them: from then on the gate is open for it.
        byte[] loose=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.LOOSE),old.keys.signing(),a.keys.agreement().getPublic());
        assertTrue("the marks are asked again at once",a.hear(loose).answered);
        assertTrue(Post.knowsLoose(a.context,a.store.address(C_AT)));
        assertEquals(Set.of(B_AT),a.store.beforeLoose());
        assertFalse("said again, it is nothing new",a.hear(Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.LOOSE),
            old.keys.signing(),a.keys.agreement().getPublic())).answered);
    }

    /**
     * Decision 94: a sleeve waits for its file's bytes to be up, its icon saying it is uploading meanwhile, and nothing is
     * counted as failed; at the receiver, what is announced shows as coming in Shared with me at once, until it is kept.
     */
    @Test public void aSleeveWaitsForItsBytesAndWhatIsComingShowsInSharedWithMe() throws Exception {
        String file=a.onHome("synthetic picture".getBytes(),"plan.png");
        a.store.give(Sharing.Scope.FILE,file,B_AT,Sharing.Level.READ,"Test phone");
        // B has said it knows files on their own, so only the upload holds it.
        a.hear(Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.LOOSE),b.keys.signing(),a.keys.agreement().getPublic()));
        assertTrue(Post.knowsLoose(a.context,a.store.address(B_AT)));
        assertFalse(a.store.sleeveReady(file));
        assertTrue("its icon says it is uploading",uploading(a,file));
        Post.Done done=Post.sleeves(a.context,a.store,a.keys,file,null,false);
        assertEquals("not sent",0,done.sent);assertEquals("and not a failure",0,done.failed);
        assertTrue(a.store.handed().isEmpty());
        assertEquals("still owed",1,a.store.sleevesOwed(file,false).size());
        goesUp(a,file);
        assertTrue(a.store.sleeveReady(file));
        assertFalse(uploading(a,file));
        // At B: announced, and coming, in Shared with me, until its bytes are kept.
        b.hear(a.sleeve(b,file));
        List<NoteStore.Branch> coming=b.store.coming(NoteStore.SHARED);
        assertEquals(1,coming.size());assertEquals("plan.png",coming.get(0).name);
        assertEquals(NoteStore.Branch.Kind.WAITING,coming.get(0).kind);assertTrue(coming.get(0).id.startsWith(NoteStore.COMING));
        assertTrue("not on Home",b.store.coming(Things.HOME).isEmpty());
        assertEquals(1,fetches(b));
        assertTrue("come",b.store.coming(NoteStore.SHARED).isEmpty());
        assertTrue(drawnIn(b.store,NoteStore.SHARED,file));
    }
    private static boolean uploading(Device on,String file) {
        for(NoteStore.Branch one:on.store.contents(Things.HOME))if(one.id.equals(file))return one.uploading;
        return false;
    }

    /** Decision 94: the collection 0.2.026 made for Shared with me becomes the place, never left beside it as a second one. */
    @Test public void theCollectionSharedWithMeBecomesThePlace() throws Exception {
        String made=b.store.addCollection(NoteStore.SHARED_WITH_ME).id;
        b.context.getSharedPreferences("settings",0).edit().putString(NoteStore.SHARED_MADE,made).putString(NoteStore.SHARED_TO,made).apply();
        String file=b.onHome("synthetic picture".getBytes(),"plan.png");
        b.store.getWritableDatabase().execSQL("UPDATE files SET shown=? WHERE id=?",new Object[]{made,file});
        assertTrue(drawnIn(b.store,made,file));
        b.store.sharedWithMeBecomesAPlace();
        assertTrue("shown in the place",drawnIn(b.store,NoteStore.SHARED,file));
        assertFalse("not on Home as well",drawnIn(b.store,Things.HOME,file));
        assertFalse("the collection is gone",b.store.stillThere(NoteStore.Branch.Kind.COLLECTION,made));
        assertEquals(NoteStore.SHARED,b.store.sharedWithMe());
        // One somebody put a note in stays, a folder of theirs; what showed in it shows in the place all the same.
        String kept=b.store.addCollection(NoteStore.SHARED_WITH_ME).id;
        NoteStore.Note note=new NoteStore.Note();note.book=kept;note.title="Mine";note.body="synthetic";b.store.save(note);
        b.context.getSharedPreferences("settings",0).edit().putString(NoteStore.SHARED_MADE,kept).apply();
        b.store.sharedWithMeBecomesAPlace();
        assertTrue(b.store.stillThere(NoteStore.Branch.Kind.COLLECTION,kept));
        assertEquals(NoteStore.SHARED,b.store.sharedWithMe());
    }

    /** Decision 94: My first note and Read me on Home, once a device, and never again once deleted; none made twice. */
    @Test public void everybodyHasTwoNotesOnHomeOnce() throws Exception {
        assertTrue(a.store.welcome("0.2.028"));
        List<String> titles=new ArrayList<>();
        for(NoteStore.Branch one:a.store.contents(Things.HOME))if(one.kind==NoteStore.Branch.Kind.PAGE)titles.add(one.name);
        assertEquals(List.of(NoteStore.FIRST_NOTE,NoteStore.READ_ME),titles);
        NoteStore.Note first=a.store.latest();
        assertEquals(NoteStore.FIRST_NOTE,first.title);assertEquals("Write anything here. It is saved as you type.",first.body);
        a.store.remove(first.id);
        assertFalse("once",a.store.welcome("0.2.028"));
        assertNotEquals(NoteStore.FIRST_NOTE,a.store.latest().title);
        // A Read me already here, come from the owner's other device: only My first note is made.
        NoteStore.Note came=new NoteStore.Note();came.book=Things.HOME;came.title=NoteStore.READ_ME;came.body=NoteStore.READ_ME_SAYS;b.store.save(came);
        assertTrue(b.store.welcome("0.2.028"));
        int readMe=0;for(NoteStore.Branch one:b.store.contents(Things.HOME))if(NoteStore.READ_ME.equals(one.name))readMe++;
        assertEquals(1,readMe);
        assertTrue(NoteStore.READ_ME_SAYS.contains("**Keeping it safe**"));
    }
}
