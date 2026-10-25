// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Add my device, with nothing yet (the owner, 2026-10-25: "from people and device, I wanted to only have the new device in my
 * connected device, not necessary share everything. Just adding it and then choosing"; decision 255), in the real notebook,
 * with synthetic devices and no network. The new laptop scans a code that offers nothing yet: it is added as one of mine,
 * listed under My devices, and has the cards a device of mine has, the persons card and the stamps card, and what is given to
 * it on its own, Temp's included; no note of everything. Given Can write on its line, it has everything, as Add my device
 * always gave. And a code offering everything still adds a device of mine with everything, as before.
 */
public class AddMyDeviceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC_AT="MxAddMinePcFixture@127.0.0.1:9821",LAPTOP_AT="MxAddMineLaptopFixture@127.0.0.1:9822",
        PRO_AT="MxAddMineProFixture@127.0.0.1:9823";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at,name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name.replace(' ','-')));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        /** Its code, as Add my device makes it: offering nothing yet, or everything at a level. */
        String code(Sharing.Level everything) throws Exception {
            return everything==null
                ?keys.line(name,at,Sharing.MINE_OFFER,Sharing.Level.READ,Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY)
                :keys.line(name,at,Sharing.travelling(Sharing.Scope.LIBRARY,"Everything"),everything,Sharing.Scope.LIBRARY.name(),Sharing.EVERYTHING);
        }
        /** What it says back once it has scanned another's code, as Post.accept says it: the code's scope and target, and whose it is. */
        Hello.Said accepting(Pairing.Said code) throws Exception {
            Hello.Said said=new Hello.Said(name,at,Point.shorten(keys.agreement().getPublic()),Point.shorten(keys.signing().getPublic()),
                code.scope,code.target,code.level.writes()).claiming(code.level);
            return said.owner(new Persons.Me(Persons.newId(new java.security.SecureRandom()),1_700_000_000_000L,null,"Synthetic owner",0,name,0));
        }
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        String note(String title) {
            NoteStore.Note n=new NoteStore.Note();n.id=UUID.randomUUID().toString();n.book=store.someBook();n.title=title;n.body=title+"\nSynthetic words";n.revision=3;
            store.save(n);return n.id;
        }
    }

    private Device pc,laptop;
    private String groceries,recipes;

    @Before public void two() throws Exception {
        pc=new Device("Test PC",PC_AT);laptop=new Device("Test laptop",LAPTOP_AT);
        groceries=pc.note("Synthetic groceries");recipes=pc.note("Synthetic recipes");
    }

    /** The laptop scans the PC's code, and the PC adds it as the code said: what Desktop.accepted and the phone's do. */
    private Pairing.Said scanned(Device shows,Device scans,Sharing.Level everything) throws Exception {
        Pairing.Said code=Pairing.read(Pairing.line(Pairing.link(shows.code(everything))));
        // The scanning end, as keepPairing and receiveCode: a code of the library's is a device of the owner's.
        boolean own=Sharing.Scope.LIBRARY.name().equals(code.scope);
        scans.store.pairedWith(code.address,code.name,own,code.agreement,code.signing);
        if(own)scans.store.setMine(code.address,true);
        // Over the wire and read back, as it arrives.
        Hello.Said back=Hello.open(Hello.wrap(scans.accepting(code)));
        assertNotNull(back);assertTrue("it says whose it is",back.persons);
        assertEquals("what is quoted back still names it",Sharing.mineOnly(back.scope,back.target),everything==null);
        Post.addedAsMine(shows.context,shows.store,back,Sharing.mineOnly(back.scope,back.target)?null:back.level);
        return code;
    }

    private static Set<String> to(List<Outbox.Wait> owed,String address) {
        Set<String> pages=new HashSet<>();for(Outbox.Wait one:owed)if(one.address.equals(address))pages.add(one.page);return pages;
    }
    private static Set<String> addresses(List<NoteStore.Contact> in){Set<String> all=new HashSet<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}
    private boolean everythingRule(Device on,String address) {
        for(Sharing.Rule rule:on.store.shares())if(rule.scope==Sharing.Scope.LIBRARY&&rule.address.equals(address)&&rule.level!=Sharing.Level.GONE)return true;
        return false;
    }

    @Test public void theCodeOfNothingYetIsTheLibrarysSoAnyBuildCountsItAsMine() throws Exception {
        Pairing.Said code=Pairing.read(Pairing.line(Pairing.link(pc.code(null))));
        assertEquals(Sharing.Scope.LIBRARY.name(),code.scope);
        assertEquals(Sharing.MINE_ONLY,code.target);
        assertEquals("the lesser level, which a build from before quotes back",Sharing.Level.READ,code.level);
        assertTrue(Sharing.mineOnly(code.scope,code.target));
        assertFalse("everything is not nothing yet",Sharing.mineOnly(Sharing.Scope.LIBRARY.name(),Sharing.EVERYTHING));
        assertFalse(Sharing.mineOnly(Sharing.Scope.PAGE.name(),Sharing.MINE_ONLY));
    }

    @Test public void addedWithNothingYetItIsMineAndGetsNoNote() throws Exception {
        scanned(pc,laptop,null);
        NoteStore.Contact it=pc.store.address(LAPTOP_AT);
        assertNotNull(it);
        assertTrue("one of mine",it.mine);assertTrue(it.paired());
        assertTrue("listed under My devices",addresses(pc.store.inGroups().get(Groups.MINE)).contains(LAPTOP_AT));
        assertFalse("no bond with everything",everythingRule(pc,LAPTOP_AT));
        assertFalse(pc.store.myOtherDevices().contains(LAPTOP_AT));
        assertTrue("no note of everything goes to it",to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT).isEmpty());
        // And the laptop, which scanned, counts the PC as one of its owner's, as with every Add my device.
        assertTrue(laptop.store.address(PC_AT).mine);
    }

    @Test public void addedWithNothingYetItGetsTheCardsOfADeviceOfMine() throws Exception {
        scanned(pc,laptop,null);
        assertEquals("the persons card",Set.of(LAPTOP_AT),addresses(Post.cardsGoTo(pc.context,pc.store)));
        assertTrue("the stamps card waits for its build to say it reads one",Post.stampsGoTo(pc.context,pc.store).isEmpty());
        pc.hear(laptop.said(pc,Receipt.STAMPS));
        assertEquals("the stamps card",Set.of(LAPTOP_AT),addresses(Post.stampsGoTo(pc.context,pc.store)));
        assertTrue("and the Integritas key, where it is chosen",Post.takesKey(pc.context,pc.store.address(LAPTOP_AT)));
    }

    @Test public void addedWithNothingYetItGetsWhatIsSharedWithItAndTemp() throws Exception {
        scanned(pc,laptop,null);
        pc.store.give(Sharing.Scope.PAGE,recipes,LAPTOP_AT,Sharing.Level.READ,"Test laptop");
        assertEquals("the note shared with it, and only that",Set.of(recipes),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT));
        // Temp to my devices reaches it as it reaches every device of mine.
        String screenshot=pc.note("Synthetic screenshot note");
        assertTrue(pc.store.toMyDevices(Sharing.Scope.PAGE,screenshot)>0);
        assertEquals(Set.of(recipes,screenshot),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT));
        assertFalse(to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT).contains(groceries));
    }

    @Test public void addedWithNothingYetItGetsOnlyTheStampsOfWhatIsSharedWithIt() throws Exception {
        // Decision 255: the stamps card carries a stamped note's text, so a device of mine with nothing yet gets only the
        // stamps of what reaches it; one given everything gets them all.
        scanned(pc,laptop,null);
        pc.store.stamped(recipes,Integritas.TITLED,"Recipes","aa01",1_760_000_000_000L,"req-shared","","0xFAKE","Recipes","Synthetic recipe");
        pc.store.stamped(groceries,Integritas.TITLED,"Groceries","aa02",1_760_000_001_000L,"req-private","","0xFAKE","Groceries","Synthetic list");
        assertTrue("nothing shared yet, no stamp",Post.stampsReaching(pc.store,pc.store.stampsCard(),LAPTOP_AT).isEmpty());
        pc.store.give(Sharing.Scope.PAGE,recipes,LAPTOP_AT,Sharing.Level.READ,"Test laptop");
        List<StampCard.Row> its=Post.stampsReaching(pc.store,pc.store.stampsCard(),LAPTOP_AT);
        assertEquals(1,its.size());assertEquals("req-shared",its.get(0).request);
        assertFalse(Post.givenEverything(pc.store).contains(LAPTOP_AT));
        pc.store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,LAPTOP_AT,Sharing.Level.WRITE,"Test laptop");
        assertTrue("given everything, every stamp",Post.givenEverything(pc.store).contains(LAPTOP_AT));
    }

    @Test public void givenCanWriteOnItsLineItHasEverythingAsBefore() throws Exception {
        scanned(pc,laptop,null);
        // What ownDeviceRole and the phone's ownDeviceRow do with Can write chosen where it said Nothing yet.
        pc.store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,LAPTOP_AT,Sharing.Level.WRITE,"Test laptop");
        assertTrue(everythingRule(pc,LAPTOP_AT));
        assertTrue(pc.store.myOtherDevices().contains(LAPTOP_AT));
        assertEquals(Set.of(groceries,recipes),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT));
        assertEquals(Set.of(LAPTOP_AT),addresses(Post.cardsGoTo(pc.context,pc.store)));
    }

    @Test public void theCodeOfEverythingStillAddsADeviceOfMineWithEverything() throws Exception {
        scanned(pc,laptop,Sharing.Level.WRITE);
        assertTrue(pc.store.address(LAPTOP_AT).mine);
        Sharing.Rule bond=null;
        for(Sharing.Rule rule:pc.store.shares())if(rule.scope==Sharing.Scope.LIBRARY&&rule.address.equals(LAPTOP_AT))bond=rule;
        assertNotNull(bond);assertEquals(Sharing.Level.WRITE,bond.level);
        assertEquals(Set.of(groceries,recipes),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT));
        assertEquals(Set.of(LAPTOP_AT),addresses(Post.cardsGoTo(pc.context,pc.store)));
        // Can read of everything, as before.
        Device pro=new Device("Test Pro",PRO_AT);
        scanned(pc,pro,Sharing.Level.READ);
        for(Sharing.Rule rule:pc.store.shares())if(rule.scope==Sharing.Scope.LIBRARY&&rule.address.equals(PRO_AT))assertEquals(Sharing.Level.READ,rule.level);
        assertEquals(Set.of(groceries,recipes),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),PRO_AT));
    }
}
