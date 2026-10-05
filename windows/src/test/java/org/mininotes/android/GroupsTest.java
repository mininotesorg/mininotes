package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * People in groups (the owner, 2026-10-05; docs/HOME.md, decision 100), in the real notebook and through {@code Post.arrived},
 * with synthetic devices and no network. A group is made, renamed and deleted; one person is in several; a thing shared with
 * a group follows it, given to whoever is put in and taken from whoever is taken out, at the highest level any of their
 * groups gives; a rule given to somebody on their own outlives every group; the groups card goes round and the later
 * decision stands, row by row; and the gate: a card is never sealed for a device that has not said it reads them. And
 * decision 103: a member's own rights on a thing a group has win, As the group puts the group's back, also after a removal
 * by hand, and what a group has and what a person has are listed, with the group each came through.
 */
public class GroupsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxGroupsOwnerFixture@127.0.0.1:9801",A2_AT="MxGroupsOwnersOtherFixture@127.0.0.1:9802",
        ANA_AT="MxGroupsAnaFixture@127.0.0.1:9803",BEN_AT="MxGroupsBenFixture@127.0.0.1:9804",OLD_AT="MxGroupsOldBuildFixture@127.0.0.1:9805";

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
        /** This device's groups, as its card goes to another device of the owner's. */
        byte[] card(Device to) throws Exception {
            Groups.Card card=store.groupsCard();
            return Envelope.seal(new byte[16],card.newest(),System.currentTimeMillis(),Groups.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
        String note(String title) {
            NoteStore.Note one=new NoteStore.Note();one.book=Things.HOME;one.title=title;one.body="Synthetic "+title.toLowerCase(Locale.ROOT);store.save(one);
            return one.id;
        }
        /** What somebody may do with a note here: null where they have no rule on it, GONE where they were taken off. */
        Sharing.Level level(String note,String address) {
            for(Sharing.Rule one:store.membership(Sharing.Scope.PAGE,note))if(one.address.equals(address))return one.level;
            return null;
        }
    }

    private Device a,ana,ben;
    @Before public void three() throws Exception {
        a=new Device("Test PC",A_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.pair(ana,"Ana");a.pair(ben,"Ben");
    }

    private static List<String> names(List<Groups.Group> groups){List<String> all=new ArrayList<>();for(Groups.Group one:groups)all.add(one.name);return all;}
    private static Set<String> addresses(List<NoteStore.Contact> in){Set<String> all=new HashSet<>();for(NoteStore.Contact one:in)all.add(one.address);return all;}

    @Test public void aGroupIsMadeRenamedAndDeletedAndItsPeopleStay() throws Exception {
        String friends=a.store.makeGroup("  Friends ");
        String work=a.store.makeGroup("colleagues");
        assertEquals("by name, whatever the case",List.of("colleagues","Friends"),names(a.store.groups()));
        try{a.store.makeGroup("   ");fail("a group with no name");}catch(IllegalArgumentException refused){assertEquals("A group needs a name.",refused.getMessage());}
        a.store.renameGroup(work,"Colleagues");
        assertEquals(List.of("Colleagues","Friends"),names(a.store.groups()));
        try{a.store.renameGroup(Groups.MINE,"Gadgets");fail("My devices renamed");}catch(IllegalStateException refused){/* built in */}
        try{a.store.deleteGroup(Groups.MINE);fail("My devices deleted");}catch(IllegalStateException refused){/* built in */}
        a.store.putInGroup(friends,ANA_AT,true);
        a.store.deleteGroup(friends);
        assertEquals(List.of("Colleagues"),names(a.store.groups()));
        assertFalse("a deleted group is not listed",a.store.inGroups().containsKey(friends));
        assertNotNull("its people stay",a.store.address(ANA_AT));
        // Deleted is a decision, and travels as one.
        boolean gone=false;for(Groups.Group one:a.store.groupsCard().groups)if(one.id.equals(friends))gone=one.gone;
        assertTrue(gone);
        // My devices is always there, first, and never kept.
        assertEquals(Groups.MINE,a.store.inGroups().keySet().iterator().next());
        for(Groups.Group one:a.store.groupsCard().groups)assertNotEquals(Groups.MINE,one.id);
    }

    @Test public void onePersonIsInSeveralGroupsAndHasTheHighestLevelAnyOfThemGives() throws Exception {
        String friends=a.store.makeGroup("Friends"),work=a.store.makeGroup("Colleagues");
        a.store.putInGroup(friends,ANA_AT,true);a.store.putInGroup(work,ANA_AT,true);a.store.putInGroup(friends,BEN_AT,true);
        Map<String,List<NoteStore.Contact>> in=a.store.inGroups();
        assertEquals(Set.of(ANA_AT,BEN_AT),addresses(in.get(friends)));
        assertEquals(Set.of(ANA_AT),addresses(in.get(work)));
        String note=a.note("Plans");
        List<Groups.Changed> changed=a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.READ);
        assertEquals("the one thing whose people changed, for the caller to send",1,changed.size());
        assertEquals(note,changed.get(0).target);
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,work,Sharing.Level.WRITE);
        assertEquals(Sharing.Level.WRITE,a.level(note,ANA_AT));
        assertEquals(Sharing.Level.READ,a.level(note,BEN_AT));
        assertEquals("from a group, folded under its line",work,a.store.fromGroups(Sharing.Scope.PAGE,note).get(ANA_AT));
        // Out of Colleagues, she still has it from Friends, at what Friends gives.
        a.store.putInGroup(work,ANA_AT,false);
        assertEquals(Sharing.Level.READ,a.level(note,ANA_AT));
        assertEquals(friends,a.store.fromGroups(Sharing.Scope.PAGE,note).get(ANA_AT));
        // Who has access lists both groups, by name.
        List<String> on=new ArrayList<>();for(Groups.Given one:a.store.groupsOn(Sharing.Scope.PAGE,note))on.add(one.group);
        assertEquals(List.of(work,friends),on);
        // Asked again, nothing changes.
        assertTrue(a.store.applyGroups().isEmpty());
    }

    @Test public void aShareFollowsTheGroupAsPeopleArePutInAndTakenOut() throws Exception {
        String friends=a.store.makeGroup("Friends");
        String note=a.note("Holiday");
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.WRITE);
        assertNull("nobody in it yet, nobody has it",a.level(note,ANA_AT));
        // Put in later, she has everything the group has.
        List<Groups.Changed> changed=a.store.putInGroup(friends,ANA_AT,true);
        assertEquals(1,changed.size());
        assertEquals(Sharing.Level.WRITE,a.level(note,ANA_AT));
        int rows=0;for(Sharing.Rule one:a.store.shares())if(one.address.equals(ANA_AT))rows++;
        assertEquals("one rule, once: "+a.store.shares(),1,rows);
        // A collection given to the group too.
        NoteStore.Shelf trip=a.store.addCollection("Trip");
        a.store.shareWithGroup(Sharing.Scope.COLLECTION,trip.id,friends,Sharing.Level.READ);
        boolean has=false;for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.COLLECTION,trip.id))has|=one.address.equals(ANA_AT)&&one.level==Sharing.Level.READ;
        assertTrue(has);
        // Taken out, it is taken from her: a decision written down, so an older list cannot put her back.
        changed=a.store.putInGroup(friends,ANA_AT,false);
        assertEquals(2,changed.size());
        assertEquals(Sharing.Level.GONE,a.level(note,ANA_AT));
        // Put back in, the group gives it again.
        a.store.putInGroup(friends,ANA_AT,true);
        assertEquals(Sharing.Level.WRITE,a.level(note,ANA_AT));
        // The thing taken from the group: taken from everybody who had it from the group.
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.GONE);
        assertEquals(Sharing.Level.GONE,a.level(note,ANA_AT));
        assertTrue(a.store.groupsOn(Sharing.Scope.PAGE,note).isEmpty());
        // And a deleted group takes what it gave.
        a.store.deleteGroup(friends);
        for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.COLLECTION,trip.id))if(one.address.equals(ANA_AT))assertEquals(Sharing.Level.GONE,one.level);
    }

    @Test public void aRuleGivenDirectlySurvivesTheGroupAndSomebodyTakenOffByHandStaysOff() throws Exception {
        String friends=a.store.makeGroup("Friends");
        String note=a.note("Recipes");
        a.store.give(Sharing.Scope.PAGE,note,ANA_AT,Sharing.Level.READ,"Ana");
        a.store.putInGroup(friends,ANA_AT,true);a.store.putInGroup(friends,BEN_AT,true);
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.WRITE);
        assertEquals("a group neither raises a rule given on its own",Sharing.Level.READ,a.level(note,ANA_AT));
        assertNull("nor folds it under the group",a.store.fromGroups(Sharing.Scope.PAGE,note).get(ANA_AT));
        assertEquals(Sharing.Level.WRITE,a.level(note,BEN_AT));
        // Ben's role changed by hand: his own from now on.
        Sharing.Rule his=null;for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.PAGE,note))if(one.address.equals(BEN_AT))his=one;
        a.store.decide(his,Sharing.Level.READ,System.currentTimeMillis());
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.GONE);
        assertEquals("nor takes it away",Sharing.Level.READ,a.level(note,ANA_AT));
        assertEquals(Sharing.Level.READ,a.level(note,BEN_AT));
        // Somebody the group gave it to, taken off by hand, stays off while nothing about the group is decided later.
        String other=a.note("Shopping");
        a.store.shareWithGroup(Sharing.Scope.PAGE,other,friends,Sharing.Level.READ);
        Sharing.Rule bens=null;for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.PAGE,other))if(one.address.equals(BEN_AT))bens=one;
        Thread.sleep(5);
        a.store.decide(bens,Sharing.Level.GONE,System.currentTimeMillis());
        assertTrue(a.store.applyGroups().isEmpty());
        assertEquals(Sharing.Level.GONE,a.level(other,BEN_AT));
        // Given to the group again, later: the later decision stands.
        Thread.sleep(5);
        a.store.shareWithGroup(Sharing.Scope.PAGE,other,friends,Sharing.Level.READ);
        assertEquals(Sharing.Level.READ,a.level(other,BEN_AT));
    }

    @Test public void onlyWhoMayGiveAThingSharesItWithAGroupAndMyDevicesIsAGroupLikeTheOthers() throws Exception {
        Device a2=new Device("Test phone",A2_AT);
        a.bond(a2,"Test phone");
        String note=a.note("Ours");
        assertEquals(Set.of(A2_AT),addresses(a.store.inGroups().get(Groups.MINE)));
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,Groups.MINE,Sharing.Level.WRITE);
        assertEquals(Sharing.Level.WRITE,a.level(note,A2_AT));
        assertEquals(Groups.MINE,a.store.groupsOn(Sharing.Scope.PAGE,note).get(0).group);
        // Ana's note, given to this PC to read: it may give nobody anything, so no group either.
        String friends=a.store.makeGroup("Friends");
        String theirs=ana.note("Ana's");
        try{ana.store.shareWithGroup(Sharing.Scope.PAGE,theirs,friends,Sharing.Level.ADMIN);}catch(IllegalStateException refused){fail("Ana owns hers");}
        Device reader=new Device("Reader",OLD_AT);
        String notHers=reader.note("Somebody else's");
        reader.store.getWritableDatabase().execSQL("UPDATE notes SET theirs=1,origin=? WHERE id=?",new String[]{ANA_AT,notHers});
        try{reader.store.shareWithGroup(Sharing.Scope.PAGE,notHers,friends,Sharing.Level.READ);fail("a reader gave it to a group");}
        catch(IllegalStateException refused){assertEquals("Only its owner or an admin can share this.",refused.getMessage());}
        // And the words Who? lists them by.
        assertEquals("My devices · 1 device",Groups.counted(Groups.MY_DEVICES,1,true));
        assertEquals("Friends · 3 people",Groups.counted("Friends",3,false));
        assertEquals("Friends · 1 person",Groups.counted("Friends",1,false));
        assertEquals("Friends · Can write",Groups.given("Friends",Sharing.Level.WRITE));
    }

    @Test public void theCardGoesRoundAndTheLaterDecisionStandsRowByRow() throws Exception {
        Device a2=new Device("Test phone",A2_AT);
        a.bond(a2,"Test phone");a2.bond(a,"Test PC");a2.pair(ana,"Ana");
        String friends=a.store.makeGroup("Friends");
        a.store.putInGroup(friends,ANA_AT,true);
        // A note the phone has: given to Friends on the PC, it is given to Ana on the phone when the card arrives.
        String note=a2.note("Groceries");
        a.store.getWritableDatabase().execSQL("INSERT INTO group_shares(grp,scope,target,level,decided) VALUES(?,?,?,?,?)",
            new String[]{friends,"PAGE",note,"WRITE",Long.toString(System.currentTimeMillis())});
        // The format, both ways round: what goes is what comes back.
        Groups.Card card=a.store.groupsCard();
        Groups.Card back=Groups.open(Groups.wrap(card));
        assertNotNull(back);
        assertEquals(1,back.groups.size());assertEquals("Friends",back.groups.get(0).name);
        assertEquals(1,back.members.size());assertEquals(card.members.get(0).key,back.members.get(0).key);
        assertEquals(1,back.given.size());assertEquals(Sharing.Level.WRITE,back.given.get(0).level);assertEquals(note,back.given.get(0).target);
        // Half a card, or one with more after it, is not a card.
        byte[] whole=Groups.wrap(card);
        assertNull(Groups.open(Arrays.copyOf(whole,whole.length-3)));
        assertNull(Groups.open(Arrays.copyOf(whole,whole.length+1)));
        assertFalse(Groups.isCard(Receipt.wrap(Receipt.GROUPS)));

        Post.Landed took=a2.hear(a.card(a2));
        assertTrue("an open People and devices is drawn again",took.groups);
        assertEquals(List.of("Friends"),names(a2.store.groups()));
        assertEquals(Set.of(ANA_AT),addresses(a2.store.inGroups().get(friends)));
        assertEquals("what the group gives, given there too",Sharing.Level.WRITE,a2.level(note,ANA_AT));
        assertFalse("the same card again is no news",a2.hear(a.card(a2)).groups);

        // Renamed on the phone later; a card from before that says the old name changes nothing.
        byte[] early=a.card(a2);
        Thread.sleep(5);
        a2.store.renameGroup(friends,"Close friends");
        a2.hear(early);
        assertEquals(List.of("Close friends"),names(a2.store.groups()));
        // The phone's card back to the PC: its later name stands there, and the rest is as the PC had it.
        a.hear(a2.card(a));
        assertEquals(List.of("Close friends"),names(a.store.groups()));
        // Ana taken out on the PC, later still: the phone takes her out and what the group gave her goes.
        Thread.sleep(5);
        a.store.putInGroup(friends,ANA_AT,false);
        a2.hear(a.card(a2));
        assertTrue(a2.store.inGroups().get(friends).isEmpty());
        assertEquals(Sharing.Level.GONE,a2.level(note,ANA_AT));

        // A member whose device is not paired here is kept all the same, and counts once it is.
        String colleagues=a.store.makeGroup("Colleagues");
        a.store.putInGroup(colleagues,BEN_AT,true);
        a2.hear(a.card(a2));
        assertTrue(a2.store.inGroups().get(colleagues).isEmpty());
        a2.pair(ben,"Ben");
        assertEquals(Set.of(BEN_AT),addresses(a2.store.inGroups().get(colleagues)));

        // A card from somebody else's device changes nothing.
        Device stranger=new Device("Stranger",OLD_AT);
        a2.pair(stranger,"Stranger");
        stranger.store.makeGroup("Their group");
        assertFalse(a2.hear(stranger.card(a2)).groups);
        assertFalse(names(a2.store.groups()).contains("Their group"));
    }

    @Test public void aBuildThatHasNotSaidItReadsGroupsIsNeverSentACard() throws Exception {
        Device a2=new Device("Test phone",A2_AT),old=new Device("Old tablet",OLD_AT);
        a.bond(a2,"Test phone");a.bond(old,"Old tablet");
        a.store.makeGroup("Friends");
        // Both are the owner's at this end, neither has said anything: nobody is sent a card.
        assertTrue(Post.groupsGoTo(a.context,a.store).isEmpty());
        // Both count this PC as their owner's; only the phone's build says it reads groups.
        a.hear(a2.said(a,Receipt.PERSONS_MINE));a.hear(old.said(a,Receipt.PERSONS_MINE));
        assertTrue("counting this PC as theirs is not reading groups",Post.groupsGoTo(a.context,a.store).isEmpty());
        a.hear(a2.said(a,Receipt.GROUPS));
        assertTrue(Post.readsGroups(a.context,a.store.address(A2_AT)));
        assertFalse(Post.readsGroups(a.context,a.store.address(OLD_AT)));
        List<NoteStore.Contact> to=Post.groupsGoTo(a.context,a.store);
        assertEquals(1,to.size());assertEquals(A2_AT,to.get(0).address);
        // Somebody else's device that reads groups is never sent one either.
        a.hear(ana.said(a,Receipt.GROUPS));
        assertTrue(Post.readsGroups(a.context,a.store.address(ANA_AT)));
        assertEquals(1,Post.groupsGoTo(a.context,a.store).size());
        // Nor one of yours turned off here.
        a.store.setMine(A2_AT,false);
        assertTrue(Post.groupsGoTo(a.context,a.store).isEmpty());
        // The rule itself.
        assertTrue(Groups.goesTo(true,true,true));
        assertFalse(Groups.goesTo(true,false,true));
        assertFalse(Groups.goesTo(false,true,true));
        assertFalse(Groups.goesTo(true,true,false));
    }

    // ---- a member's own rights, and what a group and a person have (decision 103) ---------------------------------

    private Sharing.Rule rule(String note,String address) {
        for(Sharing.Rule one:a.store.sharesOn(Sharing.Scope.PAGE,note))if(one.address.equals(address))return one;
        return null;
    }
    private NoteStore.Stands stands(String note,String address){return a.store.standsOn(Sharing.Scope.PAGE,note).get(address);}
    private static List<String> said(List<NoteStore.Shared> all){List<String> names=new ArrayList<>();for(NoteStore.Shared one:all)names.add(one.said());return names;}

    @Test public void aMembersOwnRightsWinAndAsTheGroupPutsTheGroupsBack() throws Exception {
        String friends=a.store.makeGroup("Friends");
        a.store.putInGroup(friends,ANA_AT,true);a.store.putInGroup(friends,BEN_AT,true);
        String note=a.note("Saturday market");
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.WRITE);
        assertEquals("as the group",NoteStore.Stands.GROUP,stands(note,BEN_AT).how);
        assertEquals(friends,stands(note,BEN_AT).group);
        assertTrue(stands(note,BEN_AT).may);
        // Ben given rights of his own on it: his from now on, whatever the group is given.
        a.store.decide(rule(note,BEN_AT),Sharing.Level.READ,System.currentTimeMillis());
        assertEquals(NoteStore.Stands.OWN,stands(note,BEN_AT).how);
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.ADMIN);
        assertEquals("a member's own rights win over the group's",Sharing.Level.READ,a.level(note,BEN_AT));
        assertEquals(Sharing.Level.ADMIN,a.level(note,ANA_AT));
        // As the group: the group's rights again, and he follows it from now on.
        List<Groups.Changed> changed=a.store.asTheGroup(Sharing.Scope.PAGE,note,BEN_AT);
        assertEquals("the one thing whose people changed, for the caller to send",1,changed.size());
        assertEquals(Sharing.Level.ADMIN,a.level(note,BEN_AT));
        assertEquals(NoteStore.Stands.GROUP,stands(note,BEN_AT).how);
        assertTrue("asked again, nothing changes",a.store.asTheGroup(Sharing.Scope.PAGE,note,BEN_AT).isEmpty());
        a.store.shareWithGroup(Sharing.Scope.PAGE,note,friends,Sharing.Level.READ);
        assertEquals(Sharing.Level.READ,a.level(note,BEN_AT));
        // Taken off by hand: off, and said as removed, not as never given, while the group still has it.
        Thread.sleep(5);
        a.store.decide(rule(note,BEN_AT),Sharing.Level.GONE,System.currentTimeMillis());
        assertTrue(a.store.applyGroups().isEmpty());
        assertEquals(NoteStore.Stands.REMOVED,stands(note,BEN_AT).how);
        assertFalse(stands(note,BEN_AT).has());
        // As the group puts him back, at what the group gives.
        assertEquals(1,a.store.asTheGroup(Sharing.Scope.PAGE,note,BEN_AT).size());
        assertEquals(Sharing.Level.READ,a.level(note,BEN_AT));
        assertEquals(NoteStore.Stands.GROUP,stands(note,BEN_AT).how);
        assertEquals("both have it from the group again",Set.of(ANA_AT,BEN_AT),a.store.fromGroups(Sharing.Scope.PAGE,note).keySet());
        // A thing no group of his has: nothing to follow, so refused, and his own rights stay.
        String alone=a.note("Alone");
        a.store.give(Sharing.Scope.PAGE,alone,BEN_AT,Sharing.Level.WRITE,"Ben");
        try{a.store.asTheGroup(Sharing.Scope.PAGE,alone,BEN_AT);fail("followed a group that has not got it");}
        catch(IllegalStateException refused){assertEquals("No group they are in has this.",refused.getMessage());}
        assertEquals(Sharing.Level.WRITE,a.level(alone,BEN_AT));
        assertEquals(NoteStore.Stands.OWN,stands(alone,BEN_AT).how);
        assertEquals("never given, and in no group that has it",NoteStore.Stands.NONE,stands(alone,ANA_AT).how);
        // Taken off a thing no group has is simply not on it.
        a.store.decide(rule(alone,BEN_AT),Sharing.Level.GONE,System.currentTimeMillis());
        assertEquals(NoteStore.Stands.NONE,stands(alone,BEN_AT).how);
    }

    @Test public void whatAGroupHasAndWhatAPersonHasAreListedWithTheGroupItCameThrough() throws Exception {
        Device a2=new Device("Test phone",A2_AT);
        a.bond(a2,"Test phone");
        String friends=a.store.makeGroup("Friends"),work=a.store.makeGroup("Colleagues");
        a.store.putInGroup(friends,ANA_AT,true);
        String market=a.note("Saturday market"),groceries=a.note("Groceries"),ours=a.note("Ours");
        NoteStore.Shelf trip=a.store.addCollection("Trip to Lisbon");
        a.store.shareWithGroup(Sharing.Scope.PAGE,market,friends,Sharing.Level.WRITE);
        a.store.shareWithGroup(Sharing.Scope.COLLECTION,trip.id,friends,Sharing.Level.READ);
        a.store.give(Sharing.Scope.PAGE,groceries,ANA_AT,Sharing.Level.READ,"Ana");
        a.store.shareWithGroup(Sharing.Scope.PAGE,ours,Groups.MINE,Sharing.Level.WRITE);
        // A group's page: what it was given, by name, a folder said as one, with the group's rights.
        List<NoteStore.Shared> has=a.store.sharedWithGroup(friends);
        assertEquals(List.of("Saturday market","Trip to Lisbon folder"),said(has));
        assertEquals(Sharing.Level.WRITE,has.get(0).level);assertEquals(Sharing.Level.READ,has.get(1).level);
        assertTrue(has.get(0).mayChange);assertNull(has.get(0).rule);
        assertTrue("a group with nothing",a.store.sharedWithGroup(work).isEmpty());
        // My devices lists what was given to it as a group; the bond is not a group's.
        assertEquals(List.of("Ours"),said(a.store.sharedWithGroup(Groups.MINE)));
        // A person's page: what they were given on their own, and what came through a group, with which.
        List<NoteStore.Shared> anas=a.store.sharedWith(ANA_AT);
        assertEquals(List.of("Groceries","Saturday market","Trip to Lisbon folder"),said(anas));
        assertEquals("",anas.get(0).group);assertEquals(friends,anas.get(1).group);assertEquals(friends,anas.get(2).group);
        assertEquals(Sharing.Level.READ,anas.get(0).level);assertNotNull(anas.get(1).rule);assertTrue(anas.get(1).mayChange);
        // A device of mine has everything through the bond, and the note through My devices.
        List<NoteStore.Shared> phones=a.store.sharedWith(A2_AT);
        assertEquals(List.of("Everything","Ours"),said(phones));
        assertEquals(Groups.MINE,phones.get(1).group);
        // Changed on her page, it is hers: no longer through the group, and the group's line is as it was.
        a.store.decide(anas.get(1).rule,Sharing.Level.READ,System.currentTimeMillis());
        assertEquals("",a.store.sharedWith(ANA_AT).get(1).group);
        assertEquals(Sharing.Level.WRITE,a.store.sharedWithGroup(friends).get(0).level);
        // Taken from the group, it is on neither list; what she was given on her own stays.
        a.store.shareWithGroup(Sharing.Scope.COLLECTION,trip.id,friends,Sharing.Level.GONE);
        assertEquals(List.of("Saturday market"),said(a.store.sharedWithGroup(friends)));
        assertEquals(List.of("Groceries","Saturday market"),said(a.store.sharedWith(ANA_AT)));
    }
}
