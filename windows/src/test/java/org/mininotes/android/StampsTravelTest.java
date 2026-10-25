// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Stamps on Minima, the same on every device of the owner's (the owner, 2026-10-25: "I horodated [a spreadsheet]
 * from the pro, this is not visible on the laptop"; decision 252), in the real notebook and through {@code Post.arrived}, with
 * synthetic devices and no network: a stamp made on one device of the owner's is listed on the other; a known one learns where
 * on Minima it landed and is never taken back to waiting; a stamp from before requests is known by what, fingerprint and when;
 * the card goes only to a device of the owner's whose build said it reads one, and nobody else's is taken in. And the two
 * fixes found on the way: a key card goes only to a build that reads one, and a card about whose a device is reaches AboutMe.
 */
public class StampsTravelTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC_AT="MxStampsPcFixture@127.0.0.1:9811",PRO_AT="MxStampsProFixture@127.0.0.1:9812",
        OLD_AT="MxStampsOldBuildFixture@127.0.0.1:9813",ANA_AT="MxStampsAnaFixture@127.0.0.1:9814";

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
        void bond(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
        /** This device's stamps card, sealed for another device as Post.sendStamps seals it. */
        byte[] card(Device to) throws Exception {
            return Envelope.seal(new byte[16],StampCard.newest(store.stampsCard()),System.currentTimeMillis(),Post.stampsCard(store),keys.signing(),to.keys.agreement().getPublic());
        }
        NoteStore.StampRow only(String request) {
            NoteStore.StampRow found=null;
            for(NoteStore.StampRow one:store.stamps())if(one.request().equals(request)){assertNull("one row for "+request,found);found=one;}
            return found;
        }
    }

    private static Set<String> addresses(List<NoteStore.Contact> in){Set<String> all=new HashSet<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}

    private Device pc,pro;
    @Before public void two() throws Exception {
        pc=new Device("Test PC",PC_AT);pro=new Device("Test Pro",PRO_AT);
        pc.bond(pro,"Test Pro");pro.bond(pc,"Test PC");
    }

    @Test public void aStampMadeOnTheProIsListedOnThePcAndLearnsWhereItLanded() throws Exception {
        long n=pro.store.stamped("file-ods","file","Synthetic holding.ods","aa11bb22",1_760_000_000_000L,"req-ods","Stamp request received","0xFAKE",null,null);
        pro.store.stamped("note-1",Integritas.TITLED,"Synthetic note","cc33dd44",1_760_000_100_000L,"req-note","","0xFAKE","Synthetic title","Synthetic words");
        Post.Landed took=pc.hear(pro.card(pc));
        assertTrue("the screens are told",took.stamps);
        assertEquals(Set.of("aa11bb22","cc33dd44"),took.stamped);
        assertEquals(2,pc.store.stamps().size());
        NoteStore.StampRow ods=pc.only("req-ods");
        assertEquals("file-ods",ods.id());assertEquals("Synthetic holding.ods",ods.name());assertEquals(NoteStore.STAMP_WAITING,ods.state());
        assertEquals("Stamp request received",ods.said());assertEquals(0,ods.tries());
        NoteStore.StampRow note=pc.only("req-note");
        assertTrue("the stamped text came with it",note.kept());
        assertArrayEquals(new String[]{"Synthetic title","Synthetic words"},pc.store.stampedWords(note.n()));
        assertEquals("on its icon too",NoteStore.Branch.STAMP_SENT,pc.store.stampMark("file-ods"));
        assertFalse("the same card again is no news",pc.hear(pro.card(pc)).stamps);
        assertEquals(2,pc.store.stamps().size());

        // Found on Minima on the Pro: the PC learns where, and a card from before saying waiting takes nothing back.
        byte[] early=pro.card(pc);
        pro.store.stampLanded(n,2374505,"0xTXPOW","0xTX","2026-10-25 09:00:00",3,System.currentTimeMillis());
        pro.store.stampNft("aa11bb22","0xNFTCOIN","0xNFTTX");
        Post.Landed landed=pc.hear(pro.card(pc));
        assertTrue(landed.stamps);assertEquals(Set.of("aa11bb22"),landed.stamped);
        ods=pc.only("req-ods");
        assertTrue(ods.confirmed());assertEquals(NoteStore.STAMP_CONFIRMED,ods.state());assertEquals(2374505,ods.block());
        assertEquals("0xTXPOW",ods.txpow());assertEquals("0xTX",ods.tx());assertEquals("2026-10-25 09:00:00",ods.recorded());
        assertEquals("0xNFTCOIN",ods.nft());assertEquals("0xNFTTX",ods.nftTx());
        assertEquals(NoteStore.Branch.STAMP_ON_MINIMA,pc.store.stampMark("file-ods"));
        assertFalse(pc.hear(early).stamps);
        assertTrue("never back to waiting",pc.only("req-ods").confirmed());

        // And back the other way: a stamp made on the PC reaches the Pro, which keeps its own as they are.
        pc.store.stamped("file-pc","file","Synthetic pc.pdf","ee55ff66",1_760_000_200_000L,"req-pc","","0xFAKE",null,null);
        assertTrue(pro.hear(pc.card(pro)).stamps);
        assertEquals(3,pro.store.stamps().size());
        assertNotNull(pro.only("req-pc"));assertEquals(2374505,pro.only("req-ods").block());
    }

    @Test public void aStampFromBeforeRequestsIsKnownByWhatFingerprintAndWhen() throws Exception {
        // Both devices hold the same stamp copied in from before 0.4.010, without a request; the Pro also one of its own.
        for(Device one:List.of(pc,pro))one.store.getWritableDatabase().execSQL(
            "INSERT INTO stamped(id,kind,name,hash,at,request,state) VALUES('note-old','note','Old synthetic','0011',1700000000000,'','unseen')");
        pro.store.getWritableDatabase().execSQL(
            "INSERT INTO stamped(id,kind,name,hash,at,request,state,block,txpow,recorded) VALUES('note-old','note','Old synthetic','0011',1700000000000,'','confirmed',99,'0xOLD','2023-11-14')");
        pro.store.getWritableDatabase().execSQL(
            "INSERT INTO stamped(id,kind,name,hash,at,request,state) VALUES('note-old2','note','Older synthetic','0022',1690000000000,'','unseen')");
        Post.Landed took=pc.hear(pro.card(pc));
        assertTrue(took.stamps);
        List<NoteStore.StampRow> all=pc.store.stamps();
        assertEquals("the one known by what, fingerprint and when is not kept twice",2,all.size());
        for(NoteStore.StampRow one:all)if(one.id().equals("note-old"))assertEquals("found on Minima there, so here",99,one.block());
    }

    @Test public void theCardGoesOnlyToADeviceOfTheOwnersWhoseBuildReadsOne() throws Exception {
        Device old=new Device("Old tablet",OLD_AT),ana=new Device("Ana",ANA_AT);
        pc.bond(old,"Old tablet");pc.pair(ana,"Ana");ana.pair(pc,"Test PC");
        pc.store.stamped("file-pc","file","Synthetic.pdf","ab",1_760_000_000_000L,"req-pc","","",null,null);
        assertTrue("nobody has said anything",Post.stampsGoTo(pc.context,pc.store).isEmpty());
        // Both count this PC as their owner's; only the Pro's build says it reads stamps.
        pc.hear(pro.said(pc,Receipt.PERSONS_MINE));pc.hear(old.said(pc,Receipt.PERSONS_MINE));
        assertTrue("counting this PC as theirs is not reading stamps",Post.stampsGoTo(pc.context,pc.store).isEmpty());
        pc.hear(pro.said(pc,Receipt.STAMPS));
        assertTrue(Post.readsStamps(pc.context,pc.store.address(PRO_AT)));
        assertFalse(Post.readsStamps(pc.context,pc.store.address(OLD_AT)));
        assertEquals(Set.of(PRO_AT),addresses(Post.stampsGoTo(pc.context,pc.store)));
        // Somebody else's device that reads stamps is never sent one.
        pc.hear(ana.said(pc,Receipt.PERSONS_MINE));pc.hear(ana.said(pc,Receipt.STAMPS));
        assertTrue(Post.readsStamps(pc.context,pc.store.address(ANA_AT)));
        assertEquals(Set.of(PRO_AT),addresses(Post.stampsGoTo(pc.context,pc.store)));
        // Nor one of yours turned off here.
        pc.store.setMine(PRO_AT,false);
        assertTrue(Post.stampsGoTo(pc.context,pc.store).isEmpty());

        // And a card from somebody else's device changes nothing here.
        ana.store.stamped("file-ana","file","Their.pdf","cd",1_760_000_000_000L,"req-ana","","",null,null);
        assertFalse(pc.hear(ana.card(pc)).stamps);
        assertNull(pc.only("req-ana"));
        assertEquals(1,pc.store.stamps().size());
    }

    @Test public void aKeyCardGoesOnlyToABuildThatHasSaidItReadsOne() throws Exception {
        Device ana=new Device("Ana",ANA_AT);
        pc.pair(ana,"Ana");
        NoteStore.Contact theirs=pc.store.address(PRO_AT);
        assertFalse("a build that has said nothing could keep the key as a note",Post.takesKey(pc.context,theirs));
        pc.hear(pro.said(pc,Receipt.GROUPS));pc.hear(pro.said(pc,Receipt.ABOUT_ME));
        assertFalse("no number before stamps says it reads a key card",Post.takesKey(pc.context,pc.store.address(PRO_AT)));
        pc.hear(pro.said(pc,Receipt.STAMPS));
        assertTrue(Post.takesKey(pc.context,pc.store.address(PRO_AT)));
        pc.hear(ana.said(pc,Receipt.STAMPS));
        assertFalse("never somebody else's device",Post.takesKey(pc.context,pc.store.address(ANA_AT)));
        // Taken in on the Pro from the PC, as before.
        pc.store.setSecret(Integritas.KEY,"made-up-key-0001");
        byte[] key=Envelope.seal(new byte[16],5,System.currentTimeMillis(),KeyCard.wrap(new KeyCard.Card(Integritas.KEY,"made-up-key-0001",5)),
            pc.keys.signing(),pro.keys.agreement().getPublic());
        pro.hear(key);
        assertEquals("made-up-key-0001",pro.store.secret(Integritas.KEY));
    }

    @Test public void aCardAboutWhoseADeviceIsReachesAboutMeAndACardOfPersonsStillReachesPersons() throws Exception {
        Device ana=new Device("Ana",ANA_AT);
        pc.pair(ana,"Ana");ana.pair(pc,"Test PC");
        String person=Persons.newId(new java.security.SecureRandom());
        String key=Groups.kept(Point.shorten(ana.keys.signing().getPublic()));
        byte[] about=AboutMe.wrap(new AboutMe.Card(key,person,1_700_000_000_000L));
        assertTrue("the same four bytes",Persons.isCard(about));
        assertNull("which a card of persons cannot read",Persons.open(about));
        Post.Landed took=pc.hear(Envelope.seal(new byte[16],0,System.currentTimeMillis(),about,ana.keys.signing(),pc.keys.agreement().getPublic()));
        assertTrue("taken as AboutMe",took.contacts);
        assertEquals(person,pc.store.personOf(ANA_AT));
        // A card of persons is never read as one about whose a device is.
        Persons.Device self=new Persons.Device(pro.keys.signing().getPublic().getEncoded(),Point.shorten(pro.keys.agreement().getPublic()),PRO_AT,"Test Pro",Persons.ACTIVE,1);
        byte[] persons=Persons.wrap(new Persons.Card(person,1,List.of(),"Synthetic owner",1,List.of(self)));
        assertTrue(AboutMe.isCard(persons));
        assertNull(AboutMe.open(persons));
    }
}
