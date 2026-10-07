package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A thing's own colour and strength travel with it, and each device may then set its own (the owner, 2026-10-06: "when
 * sharing everything should travel, then on the other device it can be set individually"; docs/HOME.md, decision 108): a
 * note's in its parcel, a folder's in its carton, each taken where it was decided later than what is here. In the real
 * notebook and through {@code Post.arrived}, with synthetic devices and no network: each message is built from the sender's
 * own notebook as Post builds it, sealed, and handed to the device it is for.
 */
public class DesktopLookTravelsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PHONE="MxLookPhoneFixture@127.0.0.1:9411", PC="MxLookPcFixture@127.0.0.1:9412";
    private static final NoteStore.Branch.Kind PAGE=NoteStore.Branch.Kind.PAGE, COLLECTION=NoteStore.Branch.Kind.COLLECTION;

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;
        Device(String name) throws Exception {
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
        }
        void pair(String address,String name,Device other) throws Exception {
            store.pairedWith(address,name,false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}

        /** A note as this device sends it now: its words, its path, and its own look, as Post says them. Not asking to be answered. */
        byte[] note(Device to,String id) throws Exception {return note(to,id,true);}
        /** @param look false for what a build from before sends: no look at all */
        byte[] note(Device to,String id,boolean look) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
                List.of(),1L,List.of(),store.steps(store.above(id)),"",null);
            if(look){long[] l=store.lookOf(PAGE,id);going.colour=(int)l[0];going.tone=(int)l[1];going.colourDecided=l[2];going.toneDecided=l[3];}
            return Envelope.seal(page(id),n.revision,1,Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        /** A folder's carton as this device makes it for that address, sealed at its revision, not asking to be answered. */
        byte[] carton(Device to,String at,String id,boolean look) throws Exception {
            Carton.Sent made=store.carton(id,at,true);
            Carton.Sent going=new Carton.Sent(made.id,made.name,made.icon,made.image,made.tint,made.ordinal,made.path,made.writes,
                made.scope,made.target,made.members,false,made.files,made.filesAsOf);
            if(look){going.tone=made.tone;going.colourDecided=made.colourDecided;going.toneDecided=made.toneDecided;}
            return Envelope.seal(Things.envelopeId(id),store.revisionOf(id),1,Carton.wrap(going),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}
    /** The next decision is a later one, on any clock: decisions are told apart by the millisecond. */
    private static void later() throws InterruptedException {Thread.sleep(3);}
    private static List<String> to(List<Outbox.Wait> waits){List<String> out=new ArrayList<>();for(Outbox.Wait one:waits)out.add(one.address);return out;}

    private Device phone,pc;private String id;
    /** A note on the phone, shared with the PC to write in, and had there. */
    @Before public void two() throws Exception {
        phone=new Device("phone");pc=new Device("pc");
        phone.pair(PC,"Test PC",pc);pc.pair(PHONE,"Test phone",phone);
        id=UUID.randomUUID().toString();
        NoteStore.Note n=new NoteStore.Note();n.id=id;n.book=Things.HOME;n.title="Soup";n.body="Synthetic soup";n.revision=1;phone.store.save(n);
        phone.store.setLevel(Sharing.Scope.PAGE,id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,PHONE,true));
        pc.hear(phone.note(pc,id));
        assertEquals("Synthetic soup",pc.store.get(id).body);
        // Shared with the PC by somebody else, it waited for an answer: accepted (decision 109).
        pc.store.acceptShared(PAGE,id);
        phone.store.acknowledged(PC,id,1,true);
        assertTrue(phone.store.owed(PAGE,id).isEmpty());
    }

    @Test public void aNotesColourAndStrengthGoWithItAndTheLaterDecisionWinsBothWays() throws Exception {
        // Never decided: it arrived as it was, plain, at the usual strength.
        assertEquals(Tint.NONE,pc.store.colourOf(PAGE,id));assertEquals(Tint.USUAL,pc.store.toneOf(PAGE,id));
        // A strength alone, on the phone: owed to the PC again, the words unchanged.
        long was=phone.store.get(id).revision;
        phone.store.tone(PAGE,id,7);
        assertEquals(was+1,phone.store.get(id).revision);assertEquals("Synthetic soup",phone.store.get(id).body);
        assertEquals(List.of(PC),to(phone.store.owed(PAGE,id)));
        later();phone.store.paint(PAGE,id,4);
        Post.Landed landed=pc.hear(phone.note(pc,id));
        assertEquals("the same words, so nothing is announced",null,landed.said);
        assertEquals(4,pc.store.colourOf(PAGE,id));assertEquals(7,pc.store.toneOf(PAGE,id));
        assertEquals("drawn with it",7,pc.store.get(id).tone);assertEquals(4,pc.store.get(id).colour);
        assertTrue("not owed straight back",pc.store.owed(PAGE,id).isEmpty());
        // Set on the PC afterwards: its own, and the phone's older decision, arriving again, does not put it back.
        byte[] older=phone.note(pc,id);
        later();pc.store.tone(PAGE,id,2);
        pc.hear(older);
        assertEquals(2,pc.store.toneOf(PAGE,id));assertEquals("the colour, not set here since, is still the phone's",4,pc.store.colourOf(PAGE,id));
        // And the PC's goes back to the phone, where it is the later one: the colour, decided no later, stays as it was.
        assertEquals(List.of(PHONE),to(pc.store.owed(PAGE,id)));
        phone.hear(pc.note(phone,id));
        assertEquals(2,phone.store.toneOf(PAGE,id));assertEquals(4,phone.store.colourOf(PAGE,id));
        // The phone decides again, later: the PC takes it, and a save of the page there carries it over.
        later();phone.store.paint(PAGE,id,Tint.NONE);
        pc.hear(phone.note(pc,id));
        assertEquals("no colour is a colour decided",Tint.NONE,pc.store.colourOf(PAGE,id));
        NoteStore.Note written=pc.store.get(id);written.body="Synthetic soup, more";pc.store.save(written);
        assertEquals(2,pc.store.toneOf(PAGE,id));assertArrayEquals(phone.store.lookOf(PAGE,id),pc.store.lookOf(PAGE,id));
    }

    @Test public void theUsualArrivesAsTheUsualAndALookNeverSaidChangesNothing() throws Exception {
        phone.store.tone(PAGE,id,8);
        pc.hear(phone.note(pc,id));
        assertEquals(8,pc.store.toneOf(PAGE,id));
        // Given back to the usual on the phone: the usual arrives, and the PC washes it at its own usual again.
        later();phone.store.tone(PAGE,id,Tint.USUAL);
        pc.hear(phone.note(pc,id));
        assertEquals(Tint.USUAL,pc.store.toneOf(PAGE,id));assertEquals(Tint.USUAL,pc.store.get(id).tone);
        // A build from before says no look: its words are taken, and the look the PC has is left as it is.
        later();pc.store.paint(PAGE,id,6);
        NoteStore.Note n=phone.store.get(id);n.body="Synthetic soup, from before";n.revision=n.revision+1;phone.store.save(n);
        pc.hear(phone.note(pc,id,false));
        assertEquals("Synthetic soup, from before",pc.store.get(id).body);
        assertEquals(6,pc.store.colourOf(PAGE,id));assertEquals(Tint.USUAL,pc.store.toneOf(PAGE,id));
        // A note never given a look anywhere arrives with none: plain, the usual.
        String plain=UUID.randomUUID().toString();
        NoteStore.Note p=new NoteStore.Note();p.id=plain;p.book=Things.HOME;p.title="Plain";p.body="Synthetic plain";p.revision=1;phone.store.save(p);
        phone.store.setLevel(Sharing.Scope.PAGE,plain,PC,Sharing.Level.WRITE,null);pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,plain,PHONE,true));
        pc.hear(phone.note(pc,plain));
        assertEquals(Tint.NONE,pc.store.colourOf(PAGE,plain));assertEquals(Tint.USUAL,pc.store.toneOf(PAGE,plain));
        assertArrayEquals(new long[]{Tint.NONE,Tint.USUAL,0L,0L},pc.store.lookOf(PAGE,plain));
    }

    /** A reader's own look is its own: no count moves for it, so the owner's next words are never taken for older ones. */
    @Test public void aReaderSetsItsOwnLookAndTheOwnersLaterOneStillArrives() throws Exception {
        String read=UUID.randomUUID().toString();
        NoteStore.Note n=new NoteStore.Note();n.id=read;n.book=Things.HOME;n.title="Read";n.body="Synthetic to read";n.revision=1;phone.store.save(n);
        phone.store.setLevel(Sharing.Scope.PAGE,read,PC,Sharing.Level.READ,null);pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,read,PHONE,true));
        Parcel.Sent going=new Parcel.Sent("","","","","Read","Synthetic to read",false,Collections.<Parcel.Member>emptyList(),"PAGE",read,false,-1L,false,
            List.of(),1L,List.of(),List.of(),"",null);
        pc.hear(Envelope.seal(page(read),1,1,Parcel.wrap(going),phone.keys.signing(),pc.keys.agreement().getPublic()));
        pc.store.stands(Sharing.Scope.PAGE,read,Sharing.Level.READ,1);
        assertTrue(pc.store.onlyReads(read));
        long count=pc.store.get(read).revision;
        pc.store.tone(PAGE,read,3);
        assertEquals(3,pc.store.toneOf(PAGE,read));assertEquals("no count moves for a reader's look",count,pc.store.get(read).revision);
        later();phone.store.tone(PAGE,read,9);
        going=new Parcel.Sent("","","","","Read","Synthetic to read",false,Collections.<Parcel.Member>emptyList(),"PAGE",read,false,-1L,false,
            List.of(),1L,List.of(),List.of(),"",null);
        long[] l=phone.store.lookOf(PAGE,read);going.tone=(int)l[1];going.toneDecided=l[3];
        pc.hear(Envelope.seal(page(read),phone.store.get(read).revision,1,Parcel.wrap(going),phone.keys.signing(),pc.keys.agreement().getPublic()));
        assertEquals("the owner's, decided later",9,pc.store.toneOf(PAGE,read));
    }

    @Test public void aFoldersColourAndStrengthGoInItsCartonAndTheLaterDecisionWins() throws Exception {
        String garden=phone.store.addCollection("Garden").id;
        String seeds=UUID.randomUUID().toString();
        NoteStore.Note n=new NoteStore.Note();n.id=seeds;n.book=garden;n.title="Seeds";n.body="Synthetic seeds";n.revision=1;phone.store.save(n);
        phone.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,garden,PC,true));
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PHONE,true));
        pc.hear(phone.note(pc,seeds));
        String here=pc.store.above(seeds).get(0);
        assertEquals("Garden",pc.store.nameOf(here,true));
        // Shared with the PC by somebody else, it waited for an answer: accepted, it is on Home (decision 109).
        pc.store.acceptShared(COLLECTION,here);assertEquals(Tint.USUAL,pc.store.toneOf(COLLECTION,here));
        // Its notes carry its name: nothing of its own to send, until its strength alone changes.
        assertTrue(phone.store.cartonsOwed(COLLECTION,garden).isEmpty());
        phone.store.tone(COLLECTION,garden,6);
        assertEquals(List.of(PC),to(phone.store.cartonsOwed(COLLECTION,garden)));
        later();phone.store.paint(COLLECTION,garden,3);
        pc.hear(phone.carton(pc,PC,garden,true));
        assertEquals(6,pc.store.toneOf(COLLECTION,here));assertEquals("its colour too, decided after it was made",3,pc.store.colourOf(COLLECTION,here));
        NoteStore.Branch line=null;for(NoteStore.Branch one:pc.store.contents(Things.HOME))if(one.id.equals(here))line=one;
        assertNotNull(line);assertEquals("drawn with it",6,line.tone);assertEquals(3,line.colour);
        // Set on the PC afterwards: the phone's older carton, arriving again, does not put it back.
        byte[] older=phone.carton(pc,PC,garden,true);
        later();pc.store.tone(COLLECTION,here,1);
        pc.hear(older);
        assertEquals(1,pc.store.toneOf(COLLECTION,here));assertEquals(3,pc.store.colourOf(COLLECTION,here));
        // A carton from a build from before says no look, and changes none.
        later();phone.store.tone(COLLECTION,garden,9);
        pc.hear(phone.carton(pc,PC,garden,false));
        assertEquals(1,pc.store.toneOf(COLLECTION,here));
        // The phone's later decision, said: taken.
        pc.hear(phone.carton(pc,PC,garden,true));
        assertEquals(9,pc.store.toneOf(COLLECTION,here));
    }
}
