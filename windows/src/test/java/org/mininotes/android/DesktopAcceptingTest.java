package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The sender's tick says "everybody has it" only once the other person has accepted (the connect review, 2026-10-14: their
 * device answered "I have it" while the note still waited, hidden, in their Shared with me, so the tick was green before
 * they had even seen it). In the real notebook and through {@code Post.arrived}, with synthetic devices and no network: Ana
 * shares a note with the PC, the PC keeps it waiting and says so, and says it was accepted once it is. See {@link Asking}.
 */
public class DesktopAcceptingTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String ANA="MxAcceptingAnaFixture@127.0.0.1:9821",PC="MxAcceptingPcFixture@127.0.0.1:9822";
    private static final NoteStore.Branch.Kind PAGE=NoteStore.Branch.Kind.PAGE;

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name) throws Exception{pair(other,name,false);}
        void pair(Device other,String name,boolean mine) throws Exception {
            store.pairedWith(other.at,name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        /**
         * A note as this device sends it, not asking to be answered: an answer, or "it waits", would start this device's node
         * and go out over the network, which a test never does. So the PC never hears Ana answering, and says nothing to her.
         */
        byte[] note(Device to,String id) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
                List.of(),1L,List.of(),store.steps(store.above(id)),"",null);
            return Envelope.seal(page(id),n.revision,1,Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        /** An answer this build says about one note: "I have it", "it waits", "accepted". */
        byte[] answer(Device to,String id,long revision,int what) throws Exception {
            return Envelope.seal(page(id),revision,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device ana,pc;
    private String id;
    @Before public void two() throws Exception {
        Hashes.setSha3(Sha3::of);
        ana=new Device("Ana's phone",ANA);pc=new Device("Test PC",PC);
        ana.pair(pc,"Test PC");pc.pair(ana,"Ana's phone");
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Groceries";n.body="Synthetic bread and tea";n.revision=1;ana.store.save(n);
        id=n.id;
        ana.store.setLevel(Sharing.Scope.PAGE,id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,ANA,true));
    }

    private boolean toldWaits(Device who,String address){
        for(String[] one:who.store.toldWaits())if(one[0].equals(address)&&one[1].equals("PAGE")&&one[2].equals(id))return true;
        return false;
    }

    @Test public void theTickWaitsForAcceptAndAcceptedMakesItGreen() throws Exception {
        // The PC keeps it waiting. Nothing asked for an answer, so nothing is said or kept to say.
        Post.Landed landed=pc.hear(ana.note(pc,id));
        assertTrue(landed.waiting);
        assertFalse("nothing said where nothing asked",toldWaits(pc,ANA));
        assertFalse("not accepted yet",pc.store.acceptedHere("PAGE",id));
        // As Post.sayWaits keeps it, where the note asked for an answer.
        pc.store.toldWaits(ANA,PAGE,id);
        assertTrue(toldWaits(pc,ANA));

        // Ana heard "I have it": until now, the green tick.
        ana.store.acknowledged(PC,id,1,true);
        assertEquals(SyncMark.GONE,ana.store.markOf(PAGE,id));
        // And "it waits for me to accept it": sent, not everybody has it.
        ana.hear(pc.answer(ana,id,1,Receipt.WAITS));
        assertEquals(SyncMark.SENT,ana.store.markOf(PAGE,id));
        assertTrue(ana.store.waitsAcceptance(PAGE,id));
        assertEquals("Waiting for matching versions",SyncStatus.read(ana.store,id).status());
        assertEquals("Waiting for Test PC to accept it.",SyncStatus.who(ana.store,id).get(0).standing());
        assertEquals("Waiting for Test PC to accept it.",SyncStatus.waits(ana.store,PAGE,id));

        // Accepted on the PC: no longer waiting there. Ana has not been heard answering here, so it is kept, not sent.
        pc.store.acceptShared(PAGE,id);
        assertTrue(pc.store.acceptedHere("PAGE",id));
        Post.sayAccepted(pc.context,pc.store,pc.keys);
        assertTrue("kept until it goes",toldWaits(pc,ANA));

        // Ana hears it was accepted: everybody has it.
        ana.hear(pc.answer(ana,id,System.currentTimeMillis(),Receipt.ACCEPTED));
        assertEquals(SyncMark.GONE,ana.store.markOf(PAGE,id));
        assertFalse(ana.store.waitsAcceptance(PAGE,id));
        assertEquals("Sync confirmed",SyncStatus.read(ana.store,id).status());
        // A "waits" that was slower than the "accepted" changes nothing.
        ana.hear(pc.answer(ana,id,1,Receipt.WAITS));
        assertEquals(SyncMark.GONE,ana.store.markOf(PAGE,id));
    }

    @Test public void aRefusalForgetsTheWaitAndAGoneThingIsNotSaidAgain() throws Exception {
        pc.hear(ana.note(pc,id));
        pc.store.toldWaits(ANA,PAGE,id);
        ana.store.acknowledged(PC,id,1,true);
        ana.hear(pc.answer(ana,id,1,Receipt.WAITS));
        assertFalse(ana.store.askedWaiting().isEmpty());
        // Refused, and Ana told: no longer waiting there.
        ana.hear(Envelope.seal(page(id),2,System.currentTimeMillis(),Receipt.refusal(Receipt.left(Sharing.Scope.PAGE),"No thanks"),
            pc.keys.signing(),ana.keys.agreement().getPublic()));
        assertTrue(ana.store.askedWaiting().isEmpty());
        // On the PC, refused and deleted: nothing more to say about it, and the line is let go of.
        pc.store.refuseShared(PAGE,id,pc.store.whoHasWaiting(PAGE,id),true,System.currentTimeMillis());
        assertFalse(pc.store.stillHere("PAGE",id));
        Post.sayAccepted(pc.context,pc.store,pc.keys);
        assertFalse(toldWaits(pc,ANA));
    }

    /**
     * New where a change has not been seen (the owner, 2026-10-14: "let's have the same everywhere a change hasn't been viewed,
     * on notes when an update is shared"): a note somebody else changed wears it, and the folder it is in, until it is opened;
     * what my own devices change does not.
     */
    @Test public void aNoteSomebodyElseChangedIsNewUntilOpened() throws Exception {
        pc.hear(ana.note(pc,id));
        assertTrue(pc.store.unseenNotes().contains(id));
        pc.store.acceptShared(PAGE,id);
        // Accepted, it is shown in Shared with me or on Home (decision 122): wherever it is, new on its tile.
        NoteStore.Branch line=null;
        for(NoteStore.Branch one:shown(pc))if(one.id.equals(id))line=one;
        assertNotNull("shown once accepted",line);
        assertTrue("new on its tile",line.fresh);
        // Opened: seen.
        pc.store.touch(PAGE,id);
        for(NoteStore.Branch one:shown(pc))if(one.id.equals(id))assertFalse("seen once opened",one.fresh);
        assertFalse(pc.store.unseenNotes().contains(id));
    }

    /** What Home and Shared with me show, dressed as they are drawn. */
    private static List<NoteStore.Branch> shown(Device on) {
        List<NoteStore.Branch> all=new ArrayList<>(on.store.contents(Things.HOME));all.addAll(on.store.contents(NoteStore.SHARED));
        on.store.dress(all);return all;
    }

    @Test public void aFolderWithANewNoteInItIsNewAndMyOwnDevicesChangesAreNot() throws Exception {
        // Ana's note, in a folder of the PC's: new on the note and on the folder.
        NoteStore.Shelf kitchen=pc.store.addCollection("Kitchen");
        pc.hear(ana.note(pc,id));pc.store.acceptShared(PAGE,id);
        NoteStore.Note n=pc.store.get(id);n.book=kitchen.id;pc.store.save(n);
        List<NoteStore.Branch> home=pc.store.contents(Things.HOME);pc.store.dress(home);
        boolean folderNew=false;for(NoteStore.Branch one:home)if(one.id.equals(kitchen.id))folderNew=one.fresh;
        assertTrue("the folder it is in is new too",folderNew);
        // A note from one of my own devices: not new.
        Device tablet=new Device("My tablet","MxAcceptingTabletFixture@127.0.0.1:9823");
        tablet.pair(pc,"Test PC",true);pc.pair(tablet,"My tablet",true);
        NoteStore.Note t=new NoteStore.Note();t.book=Things.HOME;t.title="From my tablet";t.body="Synthetic";t.revision=1;tablet.store.save(t);
        tablet.store.setLevel(Sharing.Scope.PAGE,t.id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,t.id,tablet.at,true));
        pc.hear(tablet.note(pc,t.id));
        assertNotNull(pc.store.get(t.id));
        assertFalse("my own device's writing is not new",pc.store.unseenNotes().contains(t.id));
    }

    @Test public void aNoteFromSomebodyWhoIsNotWaitingSaysNothingOfIt() throws Exception {
        // One of the PC's own notes, shared to Ana: nothing waits, so nothing is said or kept.
        NoteStore.Note mine=new NoteStore.Note();mine.book=Things.HOME;mine.title="Mine";mine.body="Synthetic";mine.revision=1;pc.store.save(mine);
        pc.store.setLevel(Sharing.Scope.PAGE,mine.id,ANA,Sharing.Level.WRITE,null);
        pc.store.acknowledged(ANA,mine.id,1,true);
        assertEquals(SyncMark.GONE,pc.store.markOf(PAGE,mine.id));
        assertTrue(pc.store.askedWaiting().isEmpty());
        assertTrue(pc.store.acceptedHere("PAGE",mine.id));
    }
}
