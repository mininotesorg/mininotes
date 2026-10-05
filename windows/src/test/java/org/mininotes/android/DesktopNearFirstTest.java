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
 * Files near first (the owner, 2026-10-04: "It would be good if it would be faster, even more now that I'm on the same
 * Wi-Fi"; docs/HOME.md, decision 96), in the real notebook with synthetic devices and no network. A file shared with a device
 * heard on this network (B) and one that is not (D) is first kept at the sender's door shelf alone: B is given where it is at
 * once and fetches it from that shelf, before anything is on the relay's; D is told nothing about where it is until it has
 * gone up, and then fetches it from the relay. The same for a note's own file, in the list the note carries. Two shelves
 * stand in for the two places: A's own, which its door serves, and a relay's.
 */
public class DesktopNearFirstTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();Routes.nearNow=any->false;Routes.forgetAll();}

    private static final String A_AT="MxNearSenderFixture@127.0.0.1:9711",B_AT="MxNearReceiverFixture@127.0.0.1:9712",
        D_AT="MxFarReceiverFixture@127.0.0.1:9713";

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
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] sleeve(Device to,String file) throws Exception {
            return Envelope.seal(page(file),store.fileRevision(file),System.currentTimeMillis(),
                Sleeve.wrap(store.sleeve(file,to.at).quiet(),Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device a,b,d;private File door,relay;
    @Before public void three() throws Exception {
        Hashes.setSha3(Sha3::of);
        a=new Device("Test PC",A_AT);b=new Device("Phone at home",B_AT);d=new Device("Phone away",D_AT);
        a.pair(b,"Phone at home");a.pair(d,"Phone away");b.pair(a,"Test PC");d.pair(a,"Test PC");
        door=temp.newFolder("a-door");relay=temp.newFolder("relay");
        // B is heard on this network; D is not.
        Routes.nearNow=them->them!=null&&B_AT.equals(them.address);
    }

    /** What Post does at the door: the pieces kept on A's own shelf, pinned, with no source named, nothing sent up. */
    private String atTheDoor(String file) throws Exception {
        MediaManifest kept=new MediaService(null,new BlobStore(door)).publish(a.store.bytesOf(a.store.file(file)),"image/png");
        assertTrue("it names no source: the door is found as any device's is",kept.sources.isEmpty());
        a.store.offeredAtDoor(file,kept.encode());
        return kept.encode();
    }

    private void upToTheRelay(String file) throws Exception {
        a.store.published(file,new MediaService(null,new BlobStore(relay)).publish(a.store.bytesOf(a.store.file(file)),"image/png").encode());
    }

    /** Fetched from one shelf: A's door's, or the relay's. */
    private int fetches(Device to,File from) throws Exception {
        int kept=0;
        for(NoteStore.Incoming one:to.store.toFetch(System.currentTimeMillis())) {
            byte[] plain;
            try{plain=new MediaService(null,new BlobStore(from)).fetch(MediaManifest.decode(one.manifest));}catch(Exception notThere){continue;}
            if(to.store.fileArrived(one,plain))kept++;
        }
        return kept;
    }

    private static int pieces(File shelf) {
        int n=0;
        for(File one:Objects.requireNonNull(shelf.listFiles()))n+=one.isDirectory()?pieces(one):1;
        return n;
    }

    @Test public void aDeviceOnThisWifiHasAFileSharedOnItsOwnBeforeAnythingGoesUp() throws Exception {
        byte[] bytes=new byte[90_000];new Random(96).nextBytes(bytes);
        String file=UUID.randomUUID().toString();
        Files.write(a.store.fileFor(file).toPath(),bytes);
        a.store.keep(new NoteStore.Held(file,Things.HOME,"screenshot.png","image/png",bytes.length,System.currentTimeMillis(),NoteStore.Branch.Kind.COLLECTION));
        a.store.give(Sharing.Scope.FILE,file,B_AT,Sharing.Level.READ,"Phone at home");
        a.store.give(Sharing.Scope.FILE,file,D_AT,Sharing.Level.READ,"Phone away");
        assertFalse("not up: its sleeve waits, for everybody",a.store.sleeveReady(file,B_AT));
        assertEquals(1,a.store.toPublish(System.currentTimeMillis()).size());

        atTheDoor(file);
        assertEquals("nothing on the relay yet",0,pieces(relay));
        assertTrue(a.store.atDoorOnly(file));
        assertTrue("ready for the device on this Wi-Fi",a.store.sleeveReady(file,B_AT));
        assertFalse("not for the one away",a.store.sleeveReady(file,D_AT));
        assertFalse("and still uploading, as its icon says",a.store.sleeveReady(file));
        assertEquals("the device away is told nothing of where it is","",a.store.sleeve(file,D_AT).manifest);
        assertFalse(a.store.sleeve(file,B_AT).manifest.isEmpty());
        assertEquals("still to go up for everybody else",1,a.store.toPublish(System.currentTimeMillis()).size());

        // B, near: given where it is now, and has it from A's door.
        b.hear(a.sleeve(b,file));
        assertEquals("not on the relay",0,fetches(b,relay));
        assertEquals("from the door",1,fetches(b,door));
        assertArrayEquals(bytes,b.store.bytesOf(b.store.file(file)));

        // Up to the relay: everybody is told, and D has it from there.
        upToTheRelay(file);
        assertFalse(a.store.atDoorOnly(file));
        assertTrue(a.store.sleeveReady(file));assertTrue(a.store.sleeveReady(file,D_AT));
        assertTrue(a.store.toPublish(System.currentTimeMillis()).isEmpty());
        d.hear(a.sleeve(d,file));
        assertEquals("D cannot reach the door",0,fetches(d,door));
        assertEquals(1,fetches(d,relay));
        assertArrayEquals(bytes,d.store.bytesOf(d.store.file(file)));
    }

    @Test public void aNotesOwnFileIsListedAtTheDoorOnlyToTheDeviceNear() throws Exception {
        String note=UUID.randomUUID().toString();
        NoteStore.Note n=new NoteStore.Note();n.id=note;n.book=a.store.someBook();n.body="Plans";n.revision=1;a.store.save(n);
        a.store.setLevel(Sharing.Scope.PAGE,note,B_AT,Sharing.Level.READ,null);
        a.store.setLevel(Sharing.Scope.PAGE,note,D_AT,Sharing.Level.READ,null);
        byte[] bytes=new byte[20_000];new Random(7).nextBytes(bytes);
        String file=UUID.randomUUID().toString();
        Files.write(a.store.fileFor(file).toPath(),bytes);
        a.store.keep(new NoteStore.Held(file,note,"plan.png","image/png",bytes.length,System.currentTimeMillis()));
        String kept=atTheDoor(file);
        assertEquals(kept,a.store.enclosed(note,B_AT).get(0).manifest);
        assertEquals("away, as before it went anywhere","",a.store.enclosed(note,D_AT).get(0).manifest);
        assertEquals("said to nobody in particular, the same","",a.store.enclosed(note).get(0).manifest);
        upToTheRelay(file);
        assertNotEquals(kept,a.store.enclosed(note,D_AT).get(0).manifest);
        assertFalse(a.store.enclosed(note,D_AT).get(0).manifest.isEmpty());
        assertEquals(a.store.enclosed(note,D_AT).get(0).manifest,a.store.enclosed(note,B_AT).get(0).manifest);
    }

    @Test public void onlyAManifestForTheDoorAloneIsKeptAsOffered() throws Exception {
        String file=UUID.randomUUID().toString();
        byte[] bytes=new byte[1000];
        Files.write(a.store.fileFor(file).toPath(),bytes);
        a.store.keep(new NoteStore.Held(file,Things.HOME,"a.bin","application/octet-stream",bytes.length,System.currentTimeMillis(),NoteStore.Branch.Kind.COLLECTION));
        MediaManifest up=new MediaService(null,new BlobStore(relay)).publish(bytes,"application/octet-stream");
        MediaManifest named=new MediaManifest(up.mime,up.size,up.keyHex,up.nonceHex,up.sha3Hex,up.chunkIds,List.of("MxRelay@relay.example:9001"));
        try{a.store.offeredAtDoor(file,named.encode());fail("a manifest naming a relay is not the door's alone");}catch(IllegalArgumentException refused){/* as it should */}
        assertEquals("",a.store.manifestOf(file));
    }
}
