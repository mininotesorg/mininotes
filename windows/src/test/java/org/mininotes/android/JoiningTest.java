// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The one path after a code is scanned (the owner, 2026-10-25: "once scanned, we should be invited to define what it is, own
 * device or not, put this in a group, what to share and so on"; decision 258), in the real notebook, with synthetic devices
 * and no network: what the code says first, then what is kept for each choice. A device of mine's Add my device code: one of
 * mine, nothing given, and the other end adding this one as its own. Someone else's plain code: paired, not mine, put in the
 * group chosen, made on the way where it was, and given the notes chosen.
 */
public class JoiningTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC_AT="MxJoiningPcFixture@127.0.0.1:9831",LAPTOP_AT="MxJoiningLaptopFixture@127.0.0.1:9832",
        ANA_AT="MxJoiningAnaFixture@127.0.0.1:9833";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at,name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name.replace(' ','-')));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        /** Its plain code, as Profile and Show my code… show it. */
        Pairing.Said plain() throws Exception {return Pairing.read(Pairing.line(Pairing.link(keys.line(name,at,"",Sharing.Level.WRITE,"",""))));}
        /** Its Add my device code, offering nothing yet (decision 255). */
        Pairing.Said addMine() throws Exception {
            return Pairing.read(Pairing.line(Pairing.link(keys.line(name,at,Sharing.MINE_OFFER,Sharing.Level.READ,Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY))));
        }
        /** A code sharing one note. */
        Pairing.Said offering(String id,String title) throws Exception {
            return Pairing.read(Pairing.line(Pairing.link(keys.line(name,at,Sharing.travelling(Sharing.Scope.PAGE,title),Sharing.Level.WRITE,Sharing.Scope.PAGE.name(),id))));
        }
        String note(String title) {
            NoteStore.Note n=new NoteStore.Note();n.id=UUID.randomUUID().toString();n.book=store.someBook();n.title=title;n.body=title+"\nSynthetic words";n.revision=3;
            store.save(n);return n.id;
        }
        /** What it says back once it has read another's code, as Post.accept says it. */
        Hello.Said accepting(Pairing.Said code) throws Exception {
            Hello.Said said=new Hello.Said(name,at,Point.shorten(keys.agreement().getPublic()),Point.shorten(keys.signing().getPublic()),
                code.scope,code.target,code.level.writes()).claiming(code.level);
            return said.owner(new Persons.Me(Persons.newId(new java.security.SecureRandom()),1_700_000_000_000L,null,"Synthetic owner",0,name,0));
        }
    }

    private Device pc,laptop,ana;
    private String groceries,recipes;

    @Before public void three() throws Exception {
        pc=new Device("Test PC",PC_AT);laptop=new Device("Test laptop",LAPTOP_AT);ana=new Device("Ana",ANA_AT);
        groceries=pc.note("Synthetic groceries");recipes=pc.note("Synthetic recipes");
    }

    /** The path's end on the device that read the code, as Desktop.joined and the phone's keepPairing do it, the network left out. */
    private Joining.Kept walk(Device reads,Pairing.Said code,Joining.Choice choice) {
        Joining.paired(reads.context,reads.store,code,choice.mine);
        Joining.accepting(reads.store,code);
        return Joining.chosen(reads.store,code,choice);
    }
    private static Set<String> to(List<Outbox.Wait> owed,String address) {
        Set<String> pages=new HashSet<>();for(Outbox.Wait one:owed)if(one.address.equals(address))pages.add(one.page);return pages;
    }
    private static Set<String> addresses(List<NoteStore.Contact> in){Set<String> all=new HashSet<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}
    private static boolean everything(Device on,String address) {
        for(Sharing.Rule rule:on.store.shares())if(rule.scope==Sharing.Scope.LIBRARY&&rule.address.equals(address)&&rule.level!=Sharing.Level.GONE)return true;
        return false;
    }
    private NoteStore.Branch page(String id,String title){return new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,id,"",title,"",0,0,true);}

    @Test public void whatTheCodeSaysComesFirst() throws Exception {
        assertTrue("Add my device's code says it is one of mine",Joining.Choice.from(laptop.addMine()).mine);
        assertFalse("a plain code says nothing, and someone else is the first choice",Joining.Choice.from(ana.plain()).mine);
        assertNull("nothing yet, first",Joining.Choice.from(laptop.addMine()).everything);
        assertTrue(Joining.Choice.from(ana.plain()).things.isEmpty());
        assertFalse("nothing yet is not something to accept",Joining.offers(laptop.addMine()));
        assertFalse(Joining.offers(ana.plain()));
        assertTrue("a note they share is accepted at the last step",Joining.offers(ana.offering("made-up-id","Synthetic list")));
        assertFalse(Joining.saysMine(ana.offering("made-up-id","Synthetic list")));
    }

    @Test public void anOwnDevicesCodeIsAddedAsMineWithNothing() throws Exception {
        Pairing.Said code=laptop.addMine();
        Joining.Kept kept=walk(pc,code,Joining.Choice.from(code));
        NoteStore.Contact it=pc.store.address(LAPTOP_AT);
        assertNotNull(it);assertTrue("one of mine",it.mine);assertTrue(it.paired());
        assertTrue("under My devices",addresses(pc.store.inGroups().get(Groups.MINE)).contains(LAPTOP_AT));
        assertFalse("nothing given",kept.everything);assertTrue(kept.given.isEmpty());assertFalse(kept.grouped);
        assertFalse(everything(pc,LAPTOP_AT));
        assertTrue("no note goes to it",to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT).isEmpty());
        // What is said back quotes the code, so the laptop adds this PC as one of its own, with nothing (decision 255).
        boolean[] said={false};
        for(NoteStore.Accepting one:pc.store.waitingToAccept())if(one.address.equals(LAPTOP_AT)){said[0]=true;assertTrue(Sharing.mineOnly(one.scope,one.target));}
        assertTrue("kept to be said again until heard",said[0]);
        Post.addedAsMine(laptop.context,laptop.store,pcSaying(code),null);
        assertTrue(laptop.store.address(PC_AT).mine);
        assertFalse(everything(laptop,PC_AT));
    }
    /** The PC's hello to the laptop, quoting the laptop's code, as it arrives there. */
    private Hello.Said pcSaying(Pairing.Said code) throws Exception {
        Hello.Said back=Hello.open(Hello.wrap(pc.accepting(code)));assertNotNull(back);return back;
    }

    @Test public void anOwnDeviceGivenEverythingOnTheWayHasIt() throws Exception {
        Pairing.Said code=laptop.addMine();
        Joining.Choice choice=Joining.Choice.from(code);choice.everything=Sharing.Level.WRITE;
        Joining.Kept kept=walk(pc,code,choice);
        assertTrue(kept.everything);assertTrue(everything(pc,LAPTOP_AT));
        assertEquals(Set.of(groceries,recipes),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),LAPTOP_AT));
    }

    @Test public void someoneElseIsPairedPutInTheGroupAndGivenWhatWasChosen() throws Exception {
        String friends=pc.store.makeGroup("Friends");
        Pairing.Said code=ana.plain();
        Joining.Choice choice=Joining.Choice.from(code);
        choice.group=friends;choice.things.add(page(groceries,"Synthetic groceries"));choice.level=Sharing.Level.READ;
        Joining.Kept kept=walk(pc,code,choice);
        NoteStore.Contact her=pc.store.address(ANA_AT);
        assertNotNull(her);assertFalse("someone else",her.mine);assertTrue(her.paired());
        assertTrue("in Friends",addresses(pc.store.inGroups().get(friends)).contains(ANA_AT));
        assertTrue(kept.grouped);assertEquals(1,kept.given.size());assertEquals(0,kept.refused);
        assertEquals("the note chosen, and only that",Set.of(groceries),to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),ANA_AT));
        Sharing.Rule rule=null;for(Sharing.Rule one:pc.store.sharesOn(Sharing.Scope.PAGE,groceries))if(one.address.equals(ANA_AT))rule=one;
        assertNotNull(rule);assertEquals("at the level chosen",Sharing.Level.READ,rule.level);
        assertFalse(everything(pc,ANA_AT));
        assertTrue("a plain code is answered with a plain hello",pc.store.waitingToAccept().stream().anyMatch(one->one.address.equals(ANA_AT)&&one.target.isEmpty()));
    }

    @Test public void aGroupNamedOnTheWayIsMadeAndTheyAreInIt() throws Exception {
        Pairing.Said code=ana.plain();
        Joining.Choice choice=Joining.Choice.from(code);choice.newGroup="Synthetic choir";
        Joining.Kept kept=walk(pc,code,choice);
        Groups.Group made=null;for(Groups.Group one:pc.store.groups())if(one.name.equals("Synthetic choir"))made=one;
        assertNotNull("made",made);assertTrue(kept.grouped);
        assertTrue("and she is in it",addresses(pc.store.inGroups().get(made.id)).contains(ANA_AT));
        assertTrue("nothing yet: nothing given",kept.given.isEmpty());
        assertTrue(to(pc.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),ANA_AT).isEmpty());
    }

    @Test public void nothingYetForSomeoneElseGivesNothingAndNoGroup() throws Exception {
        Joining.Kept kept=walk(pc,ana.plain(),Joining.Choice.from(ana.plain()));
        assertFalse(kept.grouped);assertTrue(kept.given.isEmpty());assertFalse(kept.everything);
        assertTrue(pc.store.address(ANA_AT).paired());assertFalse(pc.store.address(ANA_AT).mine);
    }

    @Test public void aPlainCodeChosenAsMineIsMineHere() throws Exception {
        Pairing.Said code=laptop.plain();
        Joining.Choice choice=Joining.Choice.from(code);choice.mine=true;
        walk(pc,code,choice);
        assertTrue("mine here; the laptop is asked to turn My device on too",pc.store.address(LAPTOP_AT).mine);
        assertFalse(everything(pc,LAPTOP_AT));
    }

    @Test public void homesNotesAreWhatMayBeChosen() throws Exception {
        // What is on Home: the folder the two notes are in, which shares them both.
        boolean folder=false;for(NoteStore.Branch one:Joining.shareable(pc.store))folder|=one.kind==NoteStore.Branch.Kind.COLLECTION;
        assertTrue(folder);
        for(NoteStore.Branch one:Joining.shareable(pc.store))assertNotNull("each shared on its own",Joining.scopeOf(one));
    }
}
