package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * "Not here" between real notebooks (decision 293; the owner, 2026-10-30, of Remove on a device of his in a folder's Share
 * box: "It seems Retirez also doesnt work"; "this button retirer here, it should also be there for every contact including
 * own devices"). Five synthetic devices, no node and no network: each note goes as Post makes it, is handed to the device it
 * is for, and is answered as Post answers it. Trip is shared with Ana and Ben to write and with Carl to read; the owner's
 * phone has everything. Taken away from one note in Trip, Ana is sent nothing more of it, by the owner or by Ben, and what she
 * writes in it is not taken; Ben given less there writes nothing into it; a device of mine taken away from a note keeps its
 * bond; Carl, whose build has not said it reads "not here", is never sent such an entry; and choosing a level again lifts it.
 */
public class NotHereStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC="MxNotHerePcFixture@127.0.0.1:9701",PHONE="MxNotHerePhoneFixture@127.0.0.1:9702",
        ANA="MxNotHereAnaFixture@127.0.0.1:9703",BEN="MxNotHereBenFixture@127.0.0.1:9704",CARL="MxNotHereCarlFixture@127.0.0.1:9705";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at,name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other,boolean mine) throws Exception {
            store.pairedWith(other.at,other.name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        /** That the other device's build has said it reads "not here", as Post keeps it when it hears Receipt.NOT_HERE. */
        void readsNotHere(Device other){Post.notHereHeard(context,Post.fingerprint(store.address(other.at)));}
        void write(String id,String body){NoteStore.Note n=store.get(id);n.body=body;n.revision++;store.save(n);}
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device pc,phone,ana,ben,carl;private final Map<String,Device> by=new LinkedHashMap<>();
    /** What went, as "from>to:note title". */
    private final List<String> went=new ArrayList<>();

    /** One note from one device to another, as Post sends it; heard there, and answered as Post answers it. */
    private void send(Device from,Device to,String id) throws Exception {
        NoteStore.Note n=from.store.get(id);
        Parcel.Sent going=Post.parcelFor(from.store,from.store.address(to.at),n,from.store.above(id),true,true);
        byte[] text=Parcel.wrap(going,Envelope.MAX_TEXT);
        byte[] sealed=Envelope.seal(page(id),n.revision,System.currentTimeMillis(),text,from.keys.signing(),to.keys.agreement().getPublic());
        from.store.handedOver(to.at,id,n.revision);
        went.add(from.name+">"+to.name+":"+n.title);
        Post.arrived(null,to.store,to.keys,sealed);
        NoteStore.Note there=to.store.get(id);
        from.store.acknowledged(to.at,id,n.revision,there!=null&&there.revision==n.revision&&there.body.equals(n.body));
    }

    /** Every device sends what it owes to every device it is paired with, until nothing more goes. */
    private void rounds() throws Exception {
        for(int round=0;round<12;round++) {
            int before=went.size();
            for(Device one:by.values())
                for(Outbox.Wait wait:one.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING)) {
                    Device to=by.get(wait.address);NoteStore.Contact them=one.store.address(wait.address);
                    if(to!=null&&them!=null&&them.paired())send(one,to,wait.page);
                }
            if(went.size()==before)return;
        }
        fail("never settles: "+went);
    }

    private String trip,plan,packing;

    @Before public void five() throws Exception {
        Hashes.setSha3(Sha3::of);
        pc=new Device("PC",PC);phone=new Device("Phone",PHONE);ana=new Device("Ana",ANA);ben=new Device("Ben",BEN);carl=new Device("Carl",CARL);
        for(Device one:List.of(pc,phone,ana,ben,carl))by.put(one.at,one);
        pc.pair(phone,true);phone.pair(pc,true);
        for(Device one:List.of(ana,ben,carl)){pc.pair(one,false);one.pair(pc,false);}
        ana.pair(ben,false);ben.pair(ana,false);ben.pair(carl,false);carl.pair(ben,false);ana.pair(carl,false);carl.pair(ana,false);
        // Every build here reads "not here" but Carl's, which is a build from before.
        for(Device one:List.of(pc,phone,ana,ben))for(Device other:List.of(pc,phone,ana,ben))if(one!=other)one.readsNotHere(other);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PHONE,true));
        phone.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PC,true));
        trip=pc.store.addCollection("Trip").id;
        pc.store.setLevel(Sharing.Scope.THING,trip,ANA,Sharing.Level.WRITE,"Ana");
        pc.store.setLevel(Sharing.Scope.THING,trip,BEN,Sharing.Level.WRITE,"Ben");
        pc.store.setLevel(Sharing.Scope.THING,trip,CARL,Sharing.Level.READ,"Carl");
        plan=note("Plan");packing=note("Packing");
        rounds();
        for(Device one:List.of(phone,ana,ben,carl)){assertNotNull(one.name+" has the plan",one.store.get(plan));assertNotNull(one.store.get(packing));}
        // Each of them accepts Trip, as a person does (decision 109): until then nothing of it is theirs to pass on.
        for(Device one:List.of(ana,ben,carl))one.store.acceptShared(NoteStore.Branch.Kind.COLLECTION,one.store.bookOf(plan));
        went.clear();
    }

    private String note(String title) {
        NoteStore.Note n=new NoteStore.Note();n.book=trip;n.title=title;n.body=title+"\none";n.revision=1;pc.store.save(n);return n.id;
    }
    private boolean wentTo(String who,String title){for(String one:went)if(one.endsWith(">"+who+":"+title))return true;return false;}

    @Test public void takenAwayFromOneNoteInAFolderSheIsSentNothingMoreOfItByAnybody() throws Exception {
        pc.store.decideHere(Sharing.Scope.PAGE,plan,ANA,Sharing.Level.GONE,System.currentTimeMillis());
        assertFalse(pc.store.reaches(Sharing.Scope.PAGE,plan).containsKey(ANA));
        assertTrue("the rest of Trip still goes to her",pc.store.reaches(Sharing.Scope.PAGE,packing).containsKey(ANA));
        assertEquals(NoteStore.Stands.HERE,pc.store.standsOn(Sharing.Scope.PAGE,plan).get(ANA).how);
        assertTrue("and Removed here may be undone here",pc.store.standsOn(Sharing.Scope.PAGE,plan).get(ANA).may);
        // She is told by the road Remove always took: a row at GONE within the week, said to her at every opening.
        boolean telling=false;for(NoteStore.Leaving one:pc.store.removals())telling|=one.address.equals(ANA)&&one.note.equals(plan);
        assertTrue(telling);
        // The list that goes with the note says so to a build that reads it, and leaves her entry off for one from before.
        boolean toBen=false;for(Parcel.Member one:pc.store.travelling(Sharing.Scope.PAGE,plan,BEN))toBen|=one.address.equals(ANA)&&one.here;
        assertTrue(toBen);
        for(Parcel.Member one:pc.store.travelling(Sharing.Scope.PAGE,plan,CARL))assertNotEquals("never to Carl's build",ANA,one.address);
        pc.write(plan,"Plan\none\nfrom the PC");
        rounds();
        assertFalse("the owner sends her nothing more of it: "+went,wentTo("Ana","Plan"));
        assertEquals("Plan\none\nfrom the PC",ben.store.get(plan).body);
        assertEquals("Plan\none\nfrom the PC",carl.store.get(plan).body);
        assertEquals("the owner's phone has it",ben.store.get(plan).body,phone.store.get(plan).body);
        // Ben has heard: what he writes in it goes to everybody but her.
        assertFalse(ben.store.reaches(Sharing.Scope.PAGE,plan).containsKey(ANA));
        went.clear();
        ben.write(plan,"Plan\none\nfrom the PC\nfrom Ben");
        rounds();
        assertFalse("nor does Ben: "+went,wentTo("Ana","Plan"));
        assertEquals(went.toString(),"Plan\none\nfrom the PC\nfrom Ben",pc.store.get(plan).body);
        // Whatever she writes in it still, on a copy she had, is not taken by the owner.
        ana.write(plan,"Plan\nAna's words");
        send(ana,pc,plan);
        assertEquals("Plan\none\nfrom the PC\nfrom Ben",pc.store.get(plan).body);
        assertEquals(Sharing.Level.GONE,pc.store.standingOf(ANA,plan,null).level);
        // Told, her copy becomes her own, as a removed share's does.
        assertTrue(ana.store.takenOff(PC,plan,Sharing.Scope.PAGE,System.currentTimeMillis()));
        assertFalse(ana.store.get(plan).theirs);
        // Choosing a level again gives it back: the row is a rule like any other again.
        pc.store.decideHere(Sharing.Scope.PAGE,plan,ANA,Sharing.Level.WRITE,System.currentTimeMillis());
        assertTrue(pc.store.reaches(Sharing.Scope.PAGE,plan).containsKey(ANA));
        for(Sharing.Rule one:pc.store.membership(Sharing.Scope.PAGE,plan))if(one.address.equals(ANA))assertFalse(one.here);
    }

    @Test public void givenLessHereWhatTheyWriteThereIsNotTaken() throws Exception {
        pc.store.decideHere(Sharing.Scope.PAGE,plan,BEN,Sharing.Level.READ,System.currentTimeMillis());
        NoteStore.Stands stands=pc.store.standsOn(Sharing.Scope.PAGE,plan).get(BEN);
        assertEquals(NoteStore.Stands.OWN,stands.how);assertEquals(Sharing.Level.READ,stands.level);
        assertTrue(stands.rule.here);
        pc.write(plan,"Plan\none\ntwo");
        rounds();
        assertEquals("he still gets it, to read","Plan\none\ntwo",ben.store.get(plan).body);
        ben.write(plan,"Plan\nBen's words");
        send(ben,pc,plan);
        assertEquals("Plan\none\ntwo",pc.store.get(plan).body);
        // Packing, beside it, he still writes in.
        ben.write(packing,"Packing\nBen's words");
        send(ben,pc,packing);
        assertEquals("Packing\nBen's words",pc.store.get(packing).body);
        // Can write again, as the folder gives: lifted, and his words are taken.
        pc.store.decideHere(Sharing.Scope.PAGE,plan,BEN,Sharing.Level.WRITE,System.currentTimeMillis());
        assertFalse(pc.store.standsOn(Sharing.Scope.PAGE,plan).get(BEN).rule.here);
        ben.write(plan,"Plan\nBen again");
        send(ben,pc,plan);
        assertEquals("Plan\nBen again",pc.store.get(plan).body);
    }

    @Test public void aDeviceOfMineTakenAwayFromOneNoteKeepsItsBondWithEverything() throws Exception {
        NoteStore.Stands before=pc.store.standsOn(Sharing.Scope.PAGE,plan).get(PHONE);
        assertEquals("not Owner with nothing to change: through everything, changed here",NoteStore.Stands.ABOVE,before.how);
        assertTrue(before.may);
        pc.store.decideHere(Sharing.Scope.PAGE,plan,PHONE,Sharing.Level.GONE,System.currentTimeMillis());
        assertFalse(pc.store.reaches(Sharing.Scope.PAGE,plan).containsKey(PHONE));
        assertTrue(pc.store.reaches(Sharing.Scope.PAGE,packing).containsKey(PHONE));
        Sharing.Rule bond=null;for(Sharing.Rule one:pc.store.shares())if(one.scope==Sharing.Scope.LIBRARY&&one.address.equals(PHONE))bond=one;
        assertNotNull("its bond with everything is untouched",bond);assertEquals(Sharing.Level.WRITE,bond.level);
        pc.write(plan,"Plan\nonly the PC");
        rounds();
        assertNotEquals("Plan\nonly the PC",phone.store.get(plan).body);
        // Nor is it put back on the list the note carries, as the owner's other devices are.
        for(Parcel.Member one:pc.store.travelling(Sharing.Scope.PAGE,plan,BEN))if(one.address.equals(PHONE))assertTrue(one.here);
        // What it writes in it is not taken, and told it is off it, its copy is its own, as a removed share's is.
        phone.write(plan,"Plan\nfrom the phone");
        send(phone,pc,plan);
        assertEquals("Plan\nonly the PC",pc.store.get(plan).body);
        assertTrue(phone.store.takenOff(PC,plan,Sharing.Scope.PAGE,System.currentTimeMillis()));
        assertFalse(phone.store.get(plan).theirs);
    }

    @Test public void anAdminTakesAWriterAwayHereAndNeverAnotherAdmin() throws Exception {
        pc.store.setLevel(Sharing.Scope.THING,trip,BEN,Sharing.Level.ADMIN,"Ben");
        pc.store.setLevel(Sharing.Scope.THING,trip,CARL,Sharing.Level.ADMIN,"Carl");
        pc.write(packing,"Packing\nlists go");
        rounds();
        assertEquals(Sharing.Level.ADMIN,ben.store.myLevel(Sharing.Scope.PAGE,packing));
        ben.store.decideHere(Sharing.Scope.PAGE,packing,ANA,Sharing.Level.GONE,System.currentTimeMillis());
        assertFalse(ben.store.reaches(Sharing.Scope.PAGE,packing).containsKey(ANA));
        try{ben.store.decideHere(Sharing.Scope.PAGE,packing,CARL,Sharing.Level.GONE,System.currentTimeMillis());fail("an admin took another admin away");}
        catch(IllegalStateException expected){/* only the owner does */}
        try{ben.store.decideHere(Sharing.Scope.PAGE,packing,PC,Sharing.Level.GONE,System.currentTimeMillis());fail("an admin took the owner away");}
        catch(IllegalStateException expected){/* nobody does */}
    }

    @Test public void aFolderTakenAwayTakesWhatIsInside() throws Exception {
        String days=pc.store.addCollectionIn(trip,"Days").id;
        NoteStore.Note n=new NoteStore.Note();n.book=days;n.title="Monday";n.body="Monday";n.revision=1;pc.store.save(n);
        rounds();went.clear();
        pc.store.decideHere(Sharing.Scope.THING,days,ANA,Sharing.Level.GONE,System.currentTimeMillis());
        assertFalse(pc.store.reaches(Sharing.Scope.PAGE,n.id).containsKey(ANA));
        assertFalse(pc.store.reaches(Sharing.Scope.THING,days).containsKey(ANA));
        assertTrue(pc.store.reaches(Sharing.Scope.PAGE,plan).containsKey(ANA));
        pc.write(n.id,"Monday\nlater");
        rounds();
        assertFalse(went.toString(),wentTo("Ana","Monday"));
        assertEquals("Monday\nlater",ben.store.get(n.id).body);
    }
}
