// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A device of mine linked with all of mine (the owner, 2026-10-28: "in temp I should be able to link my other laptop ... when I
 * link one additional of my devices, it should be linked to all of my devices or at least it should be clearly proposed"; decision
 * 268), in the real notebook and through {@code Post.arrived}, with three or four synthetic devices and no network. Added on
 * the PC, the new laptop is to be introduced to the Pro both ways, but for a device unticked; the Pro, told by the PC, links it
 * as mine with no code, and the laptop takes the Pro's hello because the PC's card lists the Pro; a device of mine another
 * lists is offered with Link, and one paired as a person (the "Windows PC" of the owner's Temp box) with This is my device.
 * And nobody else's word makes a device mine.
 */
public class LinkMyDevicesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC_AT="MxLinkMinePcFixture@127.0.0.1:9831",PRO_AT="MxLinkMineProFixture@127.0.0.1:9832",
        LAPTOP_AT="MxLinkMineLaptopFixture@127.0.0.1:9833",ANA_AT="MxLinkMineAnaFixture@127.0.0.1:9834",
        STRANGER_AT="MxLinkMineStrangerFixture@127.0.0.1:9835";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at,name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name.replace(' ','-')));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
            Node.chooseDevice(context,name);
        }
        void pair(Device other) throws Exception {
            store.pairedWith(other.at,other.name,false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        void bond(Device other) throws Exception {
            store.pairedWith(other.at,other.name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
            store.setMine(other.at,true);
        }
        String who() throws Exception {return Introducing.who(keys.signing().getPublic().getEncoded());}
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
        /** Its card of persons for another device, made as Post.sendCards makes it: itself, and every device it counts as mine. */
        byte[] card(Device to) throws Exception {
            Persons.Me me=Post.me(context);
            List<Persons.Device> others=new ArrayList<>();
            for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())
                others.add(new Persons.Device(Point.shorten(Point.read(one.signing)),Point.shorten(Point.read(one.agreement)),one.address,one.name,Persons.ACTIVE,one.named));
            Persons.Device self=new Persons.Device(Point.shorten(keys.signing().getPublic()),Point.shorten(keys.agreement().getPublic()),at,name,Persons.ACTIVE,me.deviceNamed);
            Persons.Card card=Persons.card(me,self,others);
            return Envelope.seal(Persons.idBytes(me.id),card.newest(),System.currentTimeMillis(),Persons.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
        /** The hello a device of mine says to link with another, as Post.askToLink says it. */
        byte[] linking(Device to,boolean asking) throws Exception {return linking(to,asking,Point.shorten(keys.agreement().getPublic()));}
        byte[] linking(Device to,boolean asking,byte[] agreement) throws Exception {
            byte[] hello=Hello.wrap(Introducing.hello(name,at,agreement,Point.shorten(keys.signing().getPublic()),asking));
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),hello,keys.signing(),to.keys.agreement().getPublic());
        }
        /** "Link with that device of mine", as Post.introduce says it. */
        byte[] introducing(Device to,Device about) throws Exception {
            return Envelope.seal(Introducing.which(about.keys.signing().getPublic().getEncoded()),0,System.currentTimeMillis(),Receipt.wrap(Receipt.LINK_MINE),
                keys.signing(),to.keys.agreement().getPublic());
        }
        boolean mine(Device other){NoteStore.Contact them=store.address(other.at);return them!=null&&them.paired()&&them.mine;}
    }

    private static Set<String> keysOf(List<Introducing.Offered> offered){Set<String> all=new HashSet<>();for(Introducing.Offered one:offered)all.add(one.key);return all;}
    private static String key(Device of) throws Exception {return Persons.key(of.keys.signing().getPublic().getEncoded());}

    private Device pc,pro,laptop;
    @Before public void three() throws Exception {
        pc=new Device("Test PC",PC_AT);pro=new Device("Test Pro",PRO_AT);laptop=new Device("Test laptop",LAPTOP_AT);
        pc.bond(pro);pro.bond(pc);
    }

    /** The laptop scans the PC's Add my device code, offering nothing yet, and the PC adds it as Desktop.addMine does. */
    private void added(Device on,Device scans) throws Exception {
        scans.bond(on);
        Hello.Said back=new Hello.Said(scans.name,scans.at,Point.shorten(scans.keys.agreement().getPublic()),Point.shorten(scans.keys.signing().getPublic()),
            Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY,false).claiming(Sharing.Level.READ);
        Post.addedAsMine(on.context,on.store,Hello.open(Hello.wrap(back)),null);
    }

    @Test public void addedOnThePcTheLaptopIsToBeIntroducedToEachOtherDeviceOfMineBothWays() throws Exception {
        Post.alsoLinkSkip(pc.context,Set.of());
        added(pc,laptop);
        assertTrue("one of mine",pc.mine(laptop));
        Set<String> lines=new HashSet<>();
        for(String one:Post.introductions(pc.context)){Introducing.Pending p=Introducing.pending(one);assertNotNull(p);lines.add(p.told+">"+p.about);}
        assertEquals("the laptop told about the Pro, the Pro about the laptop",Set.of(laptop.who()+">"+pro.who(),pro.who()+">"+laptop.who()),lines);
    }

    @Test public void aDeviceUntickedIsNotIntroduced() throws Exception {
        Post.alsoLinkSkip(pc.context,Set.of(PRO_AT));
        added(pc,laptop);
        assertTrue(pc.mine(laptop));
        assertTrue("the Pro was unticked",Post.introductions(pc.context).isEmpty());
    }

    @Test public void thePathsChoiceOfAPlainCodeAsMineIsKeptToo() throws Exception {
        // The + path, a code scanned on the PC, One of my devices chosen, the Pro left ticked (Joining.chosen and Joining.linked).
        Pairing.Said code=Pairing.read(Pairing.line(Pairing.link(laptop.keys.line(laptop.name,laptop.at,"",Sharing.Level.WRITE,"",""))));
        Joining.Choice choice=Joining.Choice.from(code);choice.mine=true;
        assertEquals("my other devices to choose from",List.of(PRO_AT),addresses(Joining.otherMine(pc.store,code.signing)));
        Joining.paired(pc.context,pc.store,code,true);
        Joining.Kept kept=Joining.chosen(pc.store,code,choice);
        assertEquals(LAPTOP_AT,kept.mine);assertTrue("a plain code: told it is mine",kept.tellMine);
        Joining.linked(pc.context,pc.store,pc.keys,kept);
        assertEquals(2,Post.introductions(pc.context).size());
        // Unticked on the way, nothing.
        Device other=new Device("Test tablet","MxLinkMineTabletFixture@127.0.0.1:9836");
        Pairing.Said second=Pairing.read(Pairing.line(Pairing.link(other.keys.line(other.name,other.at,"",Sharing.Level.WRITE,"",""))));
        Joining.Choice none=Joining.Choice.from(second);none.mine=true;none.skip.add(PRO_AT);none.skip.add(LAPTOP_AT);
        Joining.paired(pc.context,pc.store,second,true);
        Joining.linked(pc.context,pc.store,pc.keys,Joining.chosen(pc.store,second,none));
        assertEquals("nothing more to introduce",2,Post.introductions(pc.context).size());
    }

    private static List<String> addresses(List<NoteStore.Contact> in){List<String> all=new ArrayList<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}

    @Test public void toldByThePcTheProLinksTheLaptopAndTheLaptopTakesTheProAsMine() throws Exception {
        added(pc,laptop);
        // The cards go round: the PC's lists the Pro and the laptop, to each.
        pro.hear(pc.card(pro));
        laptop.hear(pc.card(laptop));
        // Listed on the Pro, not linked there: offered with Link, as a device not paired there.
        List<Introducing.Offered> onPro=Post.notLinked(pro.context,pro.store,pro.keys);
        assertEquals(Set.of(key(laptop)),keysOf(onPro));
        assertFalse(onPro.get(0).pairedHere);assertEquals("Test laptop",onPro.get(0).device.name);
        // The PC tells the Pro: the Pro takes the laptop as mine, waiting for it to answer.
        Post.Landed told=pro.hear(pc.introducing(pro,laptop));
        assertTrue("People and devices drawn again",told.people);
        assertTrue(pro.mine(laptop));
        assertTrue("nothing goes to it until it answers",pro.store.linkingNow().contains(LAPTOP_AT));
        assertTrue("no longer offered",Post.notLinked(pro.context,pro.store,pro.keys).isEmpty());
        // The Pro's hello reaches the laptop, whose card from the PC lists the Pro: the laptop takes it as mine.
        assertFalse(laptop.mine(pro));
        laptop.hear(pro.linking(laptop,true));
        assertTrue(laptop.mine(pro));
        assertEquals("called what my devices call it","Test Pro",laptop.store.address(PRO_AT).name);
        // The laptop's answer is the Pro's answer: it stops waiting.
        pro.hear(laptop.linking(pro,false));
        assertFalse(pro.store.linkingNow().contains(LAPTOP_AT));
    }

    @Test public void theIntroductionIsDoneOnceTheCardOfTheOneToldListsTheOther() throws Exception {
        added(pc,laptop);
        pro.hear(pc.card(pro));
        pro.hear(pc.introducing(pro,laptop));
        // The Pro's card, back on the PC, lists the laptop now.
        pc.hear(pro.card(pc));
        Persons.Card kept=Post.cardKept(pc.context,pro.who());
        assertNotNull("kept as it came",kept);
        assertTrue(Introducing.done(kept,laptop.keys.signing().getPublic().getEncoded()));
        assertFalse(Introducing.done(kept,new Device("Test other","MxLinkMineOtherFixture@127.0.0.1:9837").keys.signing().getPublic().getEncoded()));
    }

    @Test public void linkPressedOnAnOfferedDeviceTakesItAsMine() throws Exception {
        added(pc,laptop);
        pro.hear(pc.card(pro));
        Introducing.Offered offered=Post.notLinked(pro.context,pro.store,pro.keys).get(0);
        NoteStore.Contact them=Post.linkMine(pro.context,pro.store,pro.keys,offered.device);
        assertNotNull(them);assertEquals(LAPTOP_AT,them.address);
        assertTrue(pro.mine(laptop));
        assertTrue(pro.store.linkingNow().contains(LAPTOP_AT));
        assertEquals("Test laptop",pro.store.address(LAPTOP_AT).name);
    }

    /** Linked, then forgotten on the Pro: not offered again while the PC's card still lists it; paired again, it is (decision 277). */
    @Test public void aDeviceForgottenIsNotOfferedAgainUntilItIsPairedAgain() throws Exception {
        added(pc,laptop);
        pro.hear(pc.card(pro));
        Introducing.Offered offered=Post.notLinked(pro.context,pro.store,pro.keys).get(0);
        NoteStore.Contact them=Post.linkMine(pro.context,pro.store,pro.keys,offered.device);
        pro.store.removeAddress(them.address);
        assertTrue("forgotten: not offered though the PC's card lists it",Post.notLinked(pro.context,pro.store,pro.keys).isEmpty());
        Post.linkMine(pro.context,pro.store,pro.keys,offered.device);
        Post.notLinked(pro.context,pro.store,pro.keys);
        assertTrue("paired again: no longer kept as forgotten",pro.store.forgotten().isEmpty());
    }

    @Test public void aDevicePairedAsAPersonIsOfferedAsMineAndThisIsMyDeviceMovesIt() throws Exception {
        // The owner's case: the second laptop is mine on the Pro, and a person on the PC.
        Device second=laptop;
        pro.bond(second);second.bond(pro);
        pc.pair(second);second.pair(pc);
        pc.hear(pro.card(pc));
        List<Introducing.Offered> offered=Post.notLinked(pc.context,pc.store,pc.keys);
        assertEquals(Set.of(key(second)),keysOf(offered));
        assertTrue("paired here already, as a person",offered.get(0).pairedHere);
        assertEquals(Set.of(LAPTOP_AT),Post.maybeMine(pc.context,pc.store,offered));
        assertFalse(pc.mine(second));
        Post.linkMine(pc.context,pc.store,pc.keys,pc.store.address(LAPTOP_AT));
        assertTrue("This is my device: in My devices",pc.mine(second));
        assertTrue(Post.maybeMine(pc.context,pc.store,Post.notLinked(pc.context,pc.store,pc.keys)).isEmpty());
        // And on the second laptop, where the PC was a person too: the PC's hello, vouched for by the Pro's card, moves it.
        second.hear(pro.card(second));
        assertFalse(second.mine(pc));
        second.hear(pc.linking(second,true));
        assertTrue(second.mine(pc));
    }

    @Test public void aDeviceThatCountsThisOneAsItsOwnersIsOfferedTooThoughNoCardListsIt() throws Exception {
        // A plain code scanned on the laptop, One of my devices chosen there (decision 258): a person on the PC, saying it is mine.
        pc.pair(laptop);laptop.bond(pc);
        assertTrue(Post.maybeMine(pc.context,pc.store,Post.notLinked(pc.context,pc.store,pc.keys)).isEmpty());
        pc.hear(laptop.said(pc,Receipt.PERSONS_MINE));
        assertEquals(Set.of(LAPTOP_AT),Post.maybeMine(pc.context,pc.store,Post.notLinked(pc.context,pc.store,pc.keys)));
        assertFalse("not mine until it is pressed",pc.mine(laptop));
    }

    @Test public void nobodyElsesWordMakesADeviceMine() throws Exception {
        added(pc,laptop);
        laptop.hear(pc.card(laptop));
        Device ana=new Device("Ana",ANA_AT),stranger=new Device("Stranger",STRANGER_AT);
        laptop.pair(ana);ana.bond(stranger);ana.pair(laptop);
        // Ana's card lists the stranger as hers: Ana is not mine on the laptop, so it is not kept, and vouches for nobody.
        ana.hear(laptop.said(ana,Receipt.PERSONS_MINE));
        laptop.hear(ana.card(laptop));
        assertNull(Post.cardKept(laptop.context,ana.who()));
        laptop.hear(stranger.linking(laptop,true));
        assertNull("a hello no card of mine vouches for makes no contact",laptop.store.address(STRANGER_AT));
        // Ana telling the laptop to link with the Pro changes nothing: she is not mine.
        laptop.hear(ana.introducing(laptop,pro));
        assertFalse(laptop.mine(pro));assertNull(laptop.store.address(PRO_AT));
        // The Pro's own key, with another key to seal for: not the Pro the card lists.
        laptop.hear(pro.linking(laptop,true,Point.shorten(stranger.keys.agreement().getPublic())));
        assertFalse(laptop.mine(pro));
        // And once the PC is no longer mine on the laptop, its card vouches for nothing either.
        laptop.store.setMine(PC_AT,false);
        laptop.hear(pro.linking(laptop,true));
        assertFalse(laptop.mine(pro));
        assertTrue(Post.notLinked(laptop.context,laptop.store,laptop.keys).isEmpty());
    }

    @Test public void anIntroductionAboutADeviceItsCardDoesNotListWaits() throws Exception {
        // The PC tells the Pro about the laptop before its card listing it came: nothing yet, said again later.
        pro.hear(pc.card(pro));
        added(pc,laptop);
        pro.hear(pc.introducing(pro,laptop));
        assertNull(pro.store.address(LAPTOP_AT));
        pro.hear(pc.card(pro));
        pro.hear(pc.introducing(pro,laptop));
        assertTrue(pro.mine(laptop));
    }
}
