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
 * A note in a folder shared with another person, moved out of it on one of the owner's devices only (2026-10-19, the
 * owner's "Martin" note in Friends, five files with it): the PC sent the same revision again and again, each answered
 * as "put it with their own", and Graphene weighed each as older. Three synthetic devices, no node and no network: each
 * note goes as Post makes it, is handed to the device it is for, and is answered as Post answers it; files are left on
 * one shelf all can reach.
 */
public class MovedOutOnOneDeviceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC="MxMovedPcFixture@127.0.0.1:9601",GRAPHENE="MxMovedGrapheneFixture@127.0.0.1:9602",
        ANA="MxMovedAnaFixture@127.0.0.1:9603";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;final String name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name,boolean mine) throws Exception {
            store.pairedWith(other.at,name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        String attach(String note,byte[] bytes,String name) throws Exception {
            String id=UUID.randomUUID().toString();
            Files.write(store.fileFor(id).toPath(),bytes);
            store.keep(new NoteStore.Held(id,note,name,"application/octet-stream",bytes.length,System.currentTimeMillis()));
            MediaManifest made=new MediaService(null,new BlobStore(shelf)).publish(bytes,"application/octet-stream");
            store.published(id,made.encode());
            return id;
        }
        void write(String id,String body){NoteStore.Note n=store.get(id);n.body=body;n.revision++;store.save(n);}
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device pc,graphene,ana;private final Map<String,Device> by=new HashMap<>();private File shelf;
    private final List<String> went=new ArrayList<>();
    /** Every revision that went to a device because it was owed: none goes twice to a device that has answered for it. */
    private final Set<String> owedWent=new HashSet<>();

    /** One note from one device to another, as Post sends it; heard there, and answered as Post answers it. */
    private void send(Device from,Device to,String id) throws Exception {
        NoteStore.Note n=from.store.get(id);
        Parcel.Sent going=Post.parcelFor(from.store,from.store.address(to.at),n,from.store.above(id),true,true);
        byte[] text=Parcel.wrap(going,Envelope.MAX_TEXT);
        byte[] sealed=Envelope.seal(page(id),n.revision,System.currentTimeMillis(),text,from.keys.signing(),to.keys.agreement().getPublic());
        from.store.handedOver(to.at,id,n.revision);
        Parcel.Sent sent=Parcel.open(text);
        if(sent.files!=null){List<String> named=new ArrayList<>();for(Enclosure.Listed one:sent.files)if(one.fetchable())named.add(one.id);if(!named.isEmpty())from.store.told(named,to.at);}
        from.store.keepVersion(id,n.revision,"",n.title,n.body);
        went.add(from.name+">"+to.name+"@"+n.revision);
        // No runtime context: what Post says back on its own threads is said here.
        Post.arrived(null,to.store,to.keys,sealed);
        NoteStore.Note there=to.store.get(id);
        boolean took=there!=null&&there.revision==n.revision&&there.body.equals(n.body);
        from.store.acknowledged(to.at,id,n.revision,took);
        if(there!=null&&sent.files!=null)for(String file:to.store.haveHere(id,sent.files))from.store.fileReached(file,to.at);
    }

    private void fetches(Device to) throws Exception {
        for(NoteStore.Incoming one:to.store.toFetch(System.currentTimeMillis())) {
            byte[] plain=new MediaService(null,new BlobStore(shelf)).fetch(MediaManifest.decode(one.manifest));
            to.store.fileArrived(one,plain);
        }
    }

    /** Every device sends what it owes and tells its files' list again where due, until nothing is or it never ends. */
    private int rounds() throws Exception {
        for(int round=0;round<12;round++) {
            int before=went.size();
            for(Device one:List.of(pc,graphene,ana)) {
                for(Outbox.Wait wait:one.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING)) {
                    Device to=by.get(wait.address);
                    if(to==null)continue;
                    assertTrue("sent again after it was answered: "+one.name+">"+to.name+"@"+wait.revision+" "+went,
                        owedWent.add(one.name+">"+to.name+"@"+wait.revision));
                    send(one,to,wait.page);
                }
                for(Map.Entry<String,Set<String>> note:one.store.toTell(Long.MAX_VALUE/2).entrySet())
                    for(String address:note.getValue())if(by.containsKey(address))send(one,by.get(address),note.getKey());
                fetches(one);
            }
            if(went.size()==before)return round;
        }
        return -1;
    }

    private String martin;
    /** Whether the owner's two devices have each other only through the shared folder, with no bond with everything. */
    private boolean folderOnly;

    @Before public void three() throws Exception {
        Hashes.setSha3(Sha3::of);
        shelf=temp.newFolder("shelf");
        pc=new Device("PC",PC);graphene=new Device("Graphene",GRAPHENE);ana=new Device("Ana",ANA);
        by.put(PC,pc);by.put(GRAPHENE,graphene);by.put(ANA,ana);
        pc.pair(graphene,"Graphene",true);graphene.pair(pc,"PC",true);
        pc.pair(ana,"Ana",false);ana.pair(pc,"PC",false);
        graphene.pair(ana,"Ana",false);ana.pair(graphene,"Graphene",false);
    }

    /** Friends, shared with Ana and, without a bond, with the owner's other device; Martin in it with five files. */
    private void madeOn(Device maker) throws Exception {
        if(!folderOnly) {
            pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,GRAPHENE,true));
            graphene.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PC,true));
        }
        String friends=maker.store.addCollection("Friends").id;
        maker.store.setLevel(Sharing.Scope.THING,friends,ANA,Sharing.Level.WRITE,"Ana");
        if(folderOnly)maker.store.setLevel(Sharing.Scope.THING,friends,maker==pc?GRAPHENE:PC,Sharing.Level.WRITE,"Mine");
        NoteStore.Note n=new NoteStore.Note();n.book=friends;n.title="Martin";
        n.body="Martin\none\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten";n.revision=1;
        maker.store.save(n);martin=n.id;
        Random bytes=new Random(5);
        for(int i=0;i<5;i++){byte[] one=new byte[1500+i];bytes.nextBytes(one);maker.attach(martin,one,"scan"+i+".pdf");}
        settles("made",5);
    }

    /** Nothing more goes, and the two of the owner's devices say the same and keep the same files. */
    private void settles(String what,int files) throws Exception {
        assertTrue(what+" settles: "+went,rounds()>=0);
        assertEquals(what,pc.store.get(martin).body,graphene.store.get(martin).body);
        assertEquals(what,pc.store.get(martin).revision,graphene.store.get(martin).revision);
        assertEquals(what,files,pc.store.filesOf(NoteStore.Branch.Kind.PAGE,martin).size());
        assertEquals(what,files,graphene.store.filesOf(NoteStore.Branch.Kind.PAGE,martin).size());
        went.clear();
    }

    /** Moved out of Friends onto Home on one device (or both), then written in on each, and a file added: always the same. */
    private void movedOutOn(Device... movers) throws Exception {
        for(Device one:movers)one.store.moveInto(NoteStore.Branch.Kind.PAGE,martin,Things.HOME);
        pc.store.intoSharedFolders();graphene.store.intoSharedFolders();
        settles("the move",5);
        graphene.write(martin,"Martin\none\ntwo\nthree");
        settles("Graphene's writing",5);
        pc.write(martin,"Martin\none\ntwo\nthree\nfrom the PC");
        settles("the PC's writing",5);
        // Both at once, before either hears from the other.
        pc.write(martin,"Martin\none\ntwo\nfrom the PC");
        graphene.write(martin,"Martin\none\ntwo\nthree\nfrom the PC\nfrom Graphene");
        settles("both writing",5);
        movers[0].attach(martin,new byte[]{1,2,3,4,5,6},"added.pdf");
        settles("a file added",6);
    }

    @Test public void movedOutOnThePcOnlyWithNoBond() throws Exception {folderOnly=true;madeOn(pc);movedOutOn(pc);}
    @Test public void movedOutOnGrapheneOnlyWithNoBond() throws Exception {folderOnly=true;madeOn(pc);movedOutOn(graphene);}
    @Test public void madeOnGrapheneMovedOutOnThePcOnlyWithNoBond() throws Exception {folderOnly=true;madeOn(graphene);movedOutOn(pc);}
    @Test public void movedOutOnBothWithNoBond() throws Exception {folderOnly=true;madeOn(pc);movedOutOn(pc,graphene);}
    @Test public void movedOutOnThePcOnlyWithTheBond() throws Exception {madeOn(pc);movedOutOn(pc);}
    @Test public void madeOnGrapheneMovedOutOnThePcOnlyWithTheBond() throws Exception {madeOn(graphene);movedOutOn(pc);}

    /** Shown elsewhere on one device only, as Only put it here does: nothing about its sharing changes. */
    @Test public void shownOnHomeOnThePcOnly() throws Exception {
        folderOnly=true;madeOn(pc);
        pc.store.showIn(NoteStore.Branch.Kind.PAGE,martin,Things.HOME);
        settles("shown elsewhere",5);
        pc.write(martin,"Martin\nfrom the PC");
        settles("the PC's writing",5);
    }

    /** Taken off the folder on purpose: a device of mine that has had the note does not get it back by decision 211. */
    @Test public void aDeviceTakenOffIsNotSentItAgain() throws Exception {
        folderOnly=true;madeOn(pc);
        String friends=pc.store.bookOf(martin);
        pc.store.setLevel(Sharing.Scope.THING,friends,GRAPHENE,Sharing.Level.GONE,"Mine");
        pc.write(martin,"Martin\nonly here");
        rounds();
        assertNotEquals("Martin\nonly here",graphene.store.get(martin).body);
    }
}
