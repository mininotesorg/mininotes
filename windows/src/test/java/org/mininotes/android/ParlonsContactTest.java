package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A person's Parlons! address (the owner, 2026-10-05; docs/HOME.md, decision 101), in the real notebook and through
 * {@code Post.arrived}, with synthetic devices and no network: what is taken for an address and what is refused with words
 * that say why; set and read back, through a re-scan too; a card goes only to the devices of the owner's that were chosen,
 * are theirs at both ends and said they read one, never to a build from before or to anybody else's device; the later
 * decision stands; a card about somebody not known is dropped; and Remove travels as a decision like any other.
 */
public class ParlonsContactTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxParlonsOwnerFixture@127.0.0.1:9811",A2_AT="MxParlonsOwnersPhoneFixture@127.0.0.1:9812",
        OLD_AT="MxParlonsOldBuildFixture@127.0.0.1:9813",ANA_AT="MxParlonsAnaFixture@127.0.0.1:9814",BEN_AT="MxParlonsBenFixture@127.0.0.1:9815";
    /** Made up, in the shape Parlons! gives them. */
    private static final String ANA_PARLONS="MxG18HGGSYNTHETICANA0000000000000000000@78.141.237.9:9501",
        ANA_NEWER="MxG18HGGSYNTHETICANANEWER00000000000000@78.141.237.9:9501",BEN_PARLONS="MxG18HGGSYNTHETICBEN0000000000000000000@78.141.237.9:9501";

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
        /** Five bytes about what this build knows, as a device says them. */
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
        /** A person's Parlons! address as it is here, as its card goes to another device of the owner's. */
        byte[] card(Device to,String about) throws Exception {
            Parlons.Card card=store.parlonsCard(about);
            return Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),Parlons.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private Device a,a2,ana,ben;
    @Before public void four() throws Exception {
        a=new Device("Test PC",A_AT);a2=new Device("Test phone",A2_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.bond(a2,"Test phone");a2.bond(a,"Test PC");
        a.pair(ana,"Ana");a.pair(ben,"Ben");a2.pair(ana,"Ana");
    }

    private static Set<String> addresses(List<NoteStore.Contact> in){Set<String> all=new HashSet<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}

    @Test public void anAddressIsCheckedLooselyAndFoundInAPastedLine() {
        assertEquals(ANA_PARLONS,Parlons.address("  "+ANA_PARLONS+"\n"));
        assertEquals("a line from Parlons! with the address in it",ANA_PARLONS,Parlons.address("Add me on Parlons!: "+ANA_PARLONS+" see you there"));
        assertEquals("a full stop is not part of it",ANA_PARLONS,Parlons.address("My address is "+ANA_PARLONS+"."));
        assertEquals("nor the brackets round it",ANA_PARLONS,Parlons.address("("+ANA_PARLONS+")"));
        assertEquals("nothing is no address, which removes one","",Parlons.address("   "));
        for(String wrong:new String[]{"hello","ana@example.org","Mx@78.141.237.9:9501","MxG18HGG","MxG18HGG@"}) {
            try{Parlons.address(wrong);fail("taken for an address: "+wrong);}
            catch(IllegalArgumentException refused){assertTrue(wrong+": "+refused.getMessage(),refused.getMessage().contains("Parlons! address"));}
        }
        try{Parlons.address("Mx"+"A".repeat(500)+"@78.141.237.9:9501");fail("too long");}catch(IllegalArgumentException refused){/* said */}
        // The words a card's line shows it by, and the words nobody is helped by: no long dashes.
        assertEquals("MxG18HGG…",Parlons.shortened(ANA_PARLONS));
        assertEquals("Not set",Parlons.shortened(""));
        for(String words:new String[]{Parlons.ONLY_HERE,Parlons.ALL_MINE,Parlons.CHOOSE,Parlons.CONTACT,Parlons.ADD})assertFalse(words,words.contains("\u2014")||words.contains(" - "));
    }

    @Test public void anAddressIsSetAndReadBackAndOutlivesAReScan() throws Exception {
        assertEquals("nobody has one yet","",a.store.parlons(ANA_AT));
        long decided=System.currentTimeMillis();
        assertEquals(ANA_PARLONS,a.store.setParlons(ANA_AT,"Find me: "+ANA_PARLONS,decided));
        assertEquals(ANA_PARLONS,a.store.parlons(ANA_AT));
        NoteStore.Contact her=a.store.address(ANA_AT);
        assertEquals(ANA_PARLONS,her.parlons);assertEquals(decided,her.parlonsDecided);
        assertEquals(Map.of(ANA_AT,ANA_PARLONS),a.store.parlonsBook());
        assertEquals("",a.store.parlons(BEN_AT));
        // Scanned again: still hers.
        a.pair(ana,"Ana");
        assertEquals(ANA_PARLONS,a.store.parlons(ANA_AT));
        // Something that is not an address changes nothing.
        try{a.store.setParlons(ANA_AT,"not an address",decided+1);fail("kept");}catch(IllegalArgumentException refused){/* said */}
        assertEquals(ANA_PARLONS,a.store.parlons(ANA_AT));assertEquals(decided,a.store.address(ANA_AT).parlonsDecided);
        // The card the other devices are sent: her key, the address, the time.
        Parlons.Card card=a.store.parlonsCard(ANA_AT);
        Parlons.Card back=Parlons.open(Parlons.wrap(card));
        assertNotNull(back);assertEquals(card.key,back.key);assertEquals(ANA_PARLONS,back.address);assertEquals(decided,back.decided);
        byte[] whole=Parlons.wrap(card);
        assertNull("half a card is not a card",Parlons.open(Arrays.copyOf(whole,whole.length-3)));
        assertNull("nor one with more after it",Parlons.open(Arrays.copyOf(whole,whole.length+1)));
        assertFalse(Parlons.isCard(Groups.wrap(a.store.groupsCard())));
        assertFalse(Groups.isCard(whole));
        assertNull("a device never paired has no key to carry it",new Device("Unpaired",OLD_AT).store.parlonsCard(ANA_AT));
    }

    @Test public void aCardGoesOnlyToTheChosenOwnDevicesThatReadIt() throws Exception {
        Device old=new Device("Old tablet",OLD_AT);
        a.bond(old,"Old tablet");old.bond(a,"Test PC");
        Set<String> every=Set.of(A2_AT,OLD_AT,ANA_AT);
        // Nothing said yet: nobody is sent one.
        assertTrue(Post.parlonsGoTo(a.context,a.store,every).isEmpty());
        // All count this PC as their owner's; only the phone's build says it reads Parlons! addresses.
        a.hear(a2.said(a,Receipt.PERSONS_MINE));a.hear(old.said(a,Receipt.PERSONS_MINE));a.hear(ana.said(a,Receipt.PERSONS_MINE));
        assertTrue("counting this PC as theirs is not reading them",Post.parlonsGoTo(a.context,a.store,every).isEmpty());
        a.hear(a2.said(a,Receipt.PARLONS));
        assertTrue(Post.readsParlons(a.context,a.store.address(A2_AT)));
        assertFalse(Post.readsParlons(a.context,a.store.address(OLD_AT)));
        assertEquals(Set.of(A2_AT),addresses(Post.parlonsGoTo(a.context,a.store,every)));
        // Somebody else's device that reads them is never sent one.
        a.hear(ana.said(a,Receipt.PARLONS));
        assertTrue(Post.readsParlons(a.context,a.store.address(ANA_AT)));
        assertEquals(Set.of(A2_AT),addresses(Post.parlonsGoTo(a.context,a.store,every)));
        // Only what was chosen: the old tablet alone, or nothing, sends nothing.
        assertTrue(Post.parlonsGoTo(a.context,a.store,Set.of(OLD_AT)).isEmpty());
        assertTrue(Post.parlonsGoTo(a.context,a.store,Set.of()).isEmpty());
        assertTrue(Post.takesParlons(a.context,a.store,a.store.address(A2_AT)));
        assertFalse(Post.takesParlons(a.context,a.store,a.store.address(OLD_AT)));
        // Nor one of yours turned off here.
        a.store.setMine(A2_AT,false);
        assertTrue(Post.parlonsGoTo(a.context,a.store,every).isEmpty());
        // The rule itself.
        assertTrue(Parlons.goesTo(true,true,true,true));
        assertFalse(Parlons.goesTo(false,true,true,true));
        assertFalse(Parlons.goesTo(true,false,true,true));
        assertFalse(Parlons.goesTo(true,true,false,true));
        assertFalse(Parlons.goesTo(true,true,true,false));
        // And what the owner is told, by the names of their devices.
        assertEquals("Saved on this PC",Parlons.said(false,"this PC",List.of(),List.of(),List.of()));
        assertEquals("Saved, and sent to Test phone",Parlons.said(false,"this PC",List.of("Test phone"),List.of(),List.of()));
        assertEquals("Saved, and sent to Test phone. Old tablet needs an update of Mininotes first",
            Parlons.said(false,"this PC",List.of("Test phone"),List.of(),List.of("Old tablet")));
        assertEquals("Saved on this PC. Pro and Laptop could not be reached now: save again later",
            Parlons.said(false,"this PC",List.of(),List.of("Pro","Laptop"),List.of()));
        assertEquals("Removed here, and on Test phone",Parlons.said(true,"this PC",List.of("Test phone"),List.of(),List.of()));
    }

    @Test public void theLaterDecisionStands() throws Exception {
        long first=System.currentTimeMillis();
        a.store.setParlons(ANA_AT,ANA_PARLONS,first);
        Post.Landed took=a2.hear(a.card(a2,ANA_AT));
        assertTrue("an open People and devices is drawn again",took.contacts);
        assertEquals(ANA_PARLONS,a2.store.parlons(ANA_AT));
        assertEquals(first,a2.store.address(ANA_AT).parlonsDecided);
        assertFalse("the same card again is no news",a2.hear(a.card(a2,ANA_AT)).contacts);
        // Changed on the phone later; the PC's card from before changes nothing there.
        byte[] early=a.card(a2,ANA_AT);
        a2.store.setParlons(ANA_AT,ANA_NEWER,first+10);
        assertFalse(a2.hear(early).contacts);
        assertEquals(ANA_NEWER,a2.store.parlons(ANA_AT));
        // The phone's card back to the PC: its later address stands there.
        assertTrue(a.hear(a2.card(a,ANA_AT)).contacts);
        assertEquals(ANA_NEWER,a.store.parlons(ANA_AT));
        // A card from somebody else's device changes nothing, whatever it says.
        Device stranger=new Device("Stranger",OLD_AT);
        a2.pair(stranger,"Stranger");stranger.pair(ana,"Ana");
        stranger.store.setParlons(ANA_AT,BEN_PARLONS,first+1000);
        assertFalse(a2.hear(stranger.card(a2,ANA_AT)).contacts);
        assertEquals(ANA_NEWER,a2.store.parlons(ANA_AT));
    }

    @Test public void aCardAboutSomebodyNotKnownHereIsDropped() throws Exception {
        a.store.setParlons(BEN_AT,BEN_PARLONS,System.currentTimeMillis());
        int before=a2.store.addresses().size();
        assertFalse(a2.hear(a.card(a2,BEN_AT)).contacts);
        assertNull("nobody is made up from it",a2.store.address(BEN_AT));
        assertEquals(before,a2.store.addresses().size());
        assertTrue(a2.store.parlonsBook().isEmpty());
        // Paired later, he has no address here until one is sent again.
        a2.pair(ben,"Ben");
        assertEquals("",a2.store.parlons(BEN_AT));
        assertTrue(a2.hear(a.card(a2,BEN_AT)).contacts);
        assertEquals(BEN_PARLONS,a2.store.parlons(BEN_AT));
    }

    @Test public void removeTravelsAsALaterDecision() throws Exception {
        long first=System.currentTimeMillis();
        a.store.setParlons(ANA_AT,ANA_PARLONS,first);
        a2.hear(a.card(a2,ANA_AT));
        assertEquals(ANA_PARLONS,a2.store.parlons(ANA_AT));
        // Removed on the PC: an empty address, decided later.
        assertEquals("",a.store.setParlons(ANA_AT,"",first+5));
        Parlons.Card removed=a.store.parlonsCard(ANA_AT);
        assertEquals("",removed.address);assertEquals(first+5,removed.decided);
        assertTrue(a2.hear(a.card(a2,ANA_AT)).contacts);
        assertEquals("",a2.store.parlons(ANA_AT));
        assertTrue(a2.store.parlonsBook().isEmpty());
        assertEquals(first+5,a2.store.address(ANA_AT).parlonsDecided);
    }
}
