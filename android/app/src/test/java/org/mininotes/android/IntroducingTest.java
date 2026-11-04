package org.mininotes.android;

import static org.junit.Assert.*;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * A device of mine linked with all of mine (decision 268): what the cards between my devices offer to link, what vouches for a
 * hello as one of mine, which device an introduction names, what waits to be said and when it is said, dropped or done. Keys
 * are made for the test; addresses are documentation addresses; the names are nobody's.
 */
public class IntroducingTest {
    private static final String OWNER="0000000000000000000000000000000a";

    private static Persons.Device device(KeyPair sign,KeyPair agree,String name,int state,long decided) throws Exception {
        return new Persons.Device(Point.shorten(sign.getPublic()),Point.shorten(agree.getPublic()),"MxTEST@203.0.113.9:9001",name,state,decided);
    }
    private static Persons.Device device(KeyPair sign,String name) throws Exception {return device(sign,Envelope.keys(),name,Persons.ACTIVE,1);}
    private static Persons.Card card(Persons.Device self,Persons.Device... others) {
        return Persons.card(new Persons.Me(OWNER,1,null,"Synthetic owner",0,self.name,0),self,Arrays.asList(others));
    }
    private static String key(KeyPair one){return Persons.key(one.getPublic().getEncoded());}

    @Test public void theCardsOfferWhatIsNotLinkedHereAsMine() throws Exception {
        KeyPair here=Envelope.keys(),pc=Envelope.keys(),laptop=Envelope.keys(),pro=Envelope.keys(),tablet=Envelope.keys();
        Persons.Card fromPc=card(device(pc,"Desk PC"),device(here,"This phone"),device(laptop,"Laptop"),device(pro,"Pro"));
        Persons.Card fromPro=card(device(pro,"Pro"),device(here,"This phone"),device(tablet,"Tablet"));
        Map<String,Boolean> paired=new HashMap<>();
        paired.put(key(pc),true);paired.put(key(pro),true);paired.put(key(laptop),false);
        List<Introducing.Offered> offered=Introducing.notLinked(List.of(fromPc,fromPro),here.getPublic().getEncoded(),paired);
        assertEquals("in the order of their names, never this one, never one mine here already",2,offered.size());
        assertEquals("Laptop",offered.get(0).device.name);assertTrue("paired here, as a person",offered.get(0).pairedHere);
        assertEquals("Tablet",offered.get(1).device.name);assertFalse(offered.get(1).pairedHere);
        assertTrue("no card, nothing offered",Introducing.notLinked(List.of(),here.getPublic().getEncoded(),paired).isEmpty());
    }

    @Test public void oneMineNoLongerByTheLaterWordIsNotOffered() throws Exception {
        KeyPair here=Envelope.keys(),pc=Envelope.keys(),pro=Envelope.keys(),old=Envelope.keys();KeyPair agree=Envelope.keys();
        Persons.Card early=card(device(pc,"Desk PC"),device(old,agree,"Old tablet",Persons.ACTIVE,5));
        Persons.Card later=card(device(pro,"Pro"),device(old,agree,"Old tablet",Persons.GONE,9));
        assertTrue(Introducing.notLinked(List.of(early,later),here.getPublic().getEncoded(),new HashMap<>()).stream().noneMatch(o->o.device.name.equals("Old tablet")));
        assertNull(Introducing.vouched(List.of(early,later),old.getPublic().getEncoded()));
        // The other way round, the later word is that it is mine again.
        Persons.Card again=card(device(pro,"Pro"),device(old,agree,"Old tablet",Persons.ACTIVE,12));
        assertNotNull(Introducing.vouched(List.of(early,later,again),old.getPublic().getEncoded()));
    }

    @Test public void aDevicePairedHereAsSomebodyElsesIsTakenAsMineWhereACardOfMineListsIt() throws Exception {
        // Decision 290: the Pro's card listed the spare laptop as the owner's; the Graphene had it as somebody else's.
        KeyPair here=Envelope.keys(),pro=Envelope.keys(),spare=Envelope.keys(),tablet=Envelope.keys(),parisa=Envelope.keys();KeyPair agree=Envelope.keys();
        Persons.Card fromPro=card(device(pro,"Pro"),device(here,"This phone"),device(spare,"Spare laptop"),device(tablet,"Tablet"),
            device(Envelope.keys(),agree,"Old phone",Persons.GONE,9));
        Map<String,Boolean> paired=new HashMap<>();
        paired.put(key(pro),true);paired.put(key(spare),false);paired.put(key(parisa),false);
        List<Introducing.Offered> adopted=Introducing.adopted(Introducing.notLinked(List.of(fromPro),here.getPublic().getEncoded(),paired));
        assertEquals("only the one paired here and listed active: not the tablet, never paired here, nor somebody else",1,adopted.size());
        assertEquals("Spare laptop",adopted.get(0).device.name);
        // Whether the device a file came from counts another as its owner's, by its own card.
        assertTrue(Introducing.lists(fromPro,spare.getPublic().getEncoded()));
        assertTrue(Introducing.lists(fromPro,Point.shorten(here.getPublic())));
        assertFalse(Introducing.lists(fromPro,parisa.getPublic().getEncoded()));
        assertFalse("no card, no word",Introducing.lists(null,spare.getPublic().getEncoded()));
    }

    @Test public void aHelloIsVouchedForOnlyByACardListingItsKey() throws Exception {
        KeyPair pc=Envelope.keys(),pro=Envelope.keys(),stranger=Envelope.keys();
        Persons.Card fromPc=card(device(pc,"Desk PC"),device(pro,"Pro"));
        Persons.Device vouch=Introducing.vouched(List.of(fromPc),pro.getPublic().getEncoded());
        assertNotNull("in either form of the key",vouch);assertEquals("Pro",vouch.name);
        assertNotNull(Introducing.vouched(List.of(fromPc),Point.shorten(pro.getPublic())));
        assertNull(Introducing.vouched(List.of(fromPc),stranger.getPublic().getEncoded()));
        assertNull("no cards, no word",Introducing.vouched(List.of(),pro.getPublic().getEncoded()));
        assertNull(Introducing.vouched(List.of(fromPc),new byte[0]));
    }

    @Test public void anIntroductionNamesADeviceOnTheCardByHalfItsFingerprint() throws Exception {
        KeyPair here=Envelope.keys(),pc=Envelope.keys(),laptop=Envelope.keys(),stranger=Envelope.keys();
        Persons.Card fromPc=card(device(pc,"Desk PC"),device(here,"This phone"),device(laptop,"Laptop"));
        byte[] which=Introducing.which(laptop.getPublic().getEncoded());
        assertEquals("an envelope's page",16,which.length);
        assertArrayEquals("whichever form the key is in",which,Introducing.which(Point.shorten(laptop.getPublic())));
        assertArrayEquals("the first half of the fingerprint an envelope names its sender by",
            Arrays.copyOf(Envelope.fingerprint(laptop.getPublic()),16),which);
        assertEquals(Courier.hex(Envelope.fingerprint(laptop.getPublic())),Introducing.who(Point.shorten(laptop.getPublic())));
        assertEquals("Laptop",Introducing.introduced(fromPc,which,here.getPublic().getEncoded()).name);
        assertNull("not on the card",Introducing.introduced(fromPc,Introducing.which(stranger.getPublic().getEncoded()),here.getPublic().getEncoded()));
        assertNull("never this device",Introducing.introduced(fromPc,Introducing.which(here.getPublic().getEncoded()),here.getPublic().getEncoded()));
        assertNull("no card yet",Introducing.introduced(null,which,here.getPublic().getEncoded()));
        assertNull(Introducing.introduced(fromPc,new byte[16],here.getPublic().getEncoded()));
        assertEquals(0,Introducing.which(new byte[]{1,2,3}).length);
    }

    @Test public void doneOnceTheCardOfTheOneToldListsTheOther() throws Exception {
        KeyPair pro=Envelope.keys(),laptop=Envelope.keys();
        assertFalse(Introducing.done(card(device(pro,"Pro")),laptop.getPublic().getEncoded()));
        assertTrue(Introducing.done(card(device(pro,"Pro"),device(laptop,"Laptop")),laptop.getPublic().getEncoded()));
        assertFalse("no card kept yet",Introducing.done(null,laptop.getPublic().getEncoded()));
    }

    @Test public void eachTickIsKeptBothWaysOnce() {
        Set<String> kept=Introducing.introduce(Set.of(),"laptop",List.of("pro","pc"),100);
        assertEquals(Set.of("laptop:pro:100","pro:laptop:100","laptop:pc:100","pc:laptop:100"),kept);
        // The same two again: once, with the later time; a broken line dropped; the device itself never.
        Set<String> again=Introducing.introduce(new java.util.HashSet<>(List.of("laptop:pro:100","broken","pro:laptop:100")),"laptop",List.of("pro","laptop"),200);
        assertEquals(Set.of("laptop:pro:200","pro:laptop:200"),again);
        assertEquals(Set.of("a:b:1"),Introducing.introduce(Set.of("a:b:1"),"c",List.of(),5));
        assertNull(Introducing.pending("a:a:1"));assertNull(Introducing.pending("a:b:x"));assertNull(Introducing.pending(null));
        Introducing.Pending one=Introducing.pending("a:b:7");assertEquals("a",one.told);assertEquals("b",one.about);assertEquals(7,one.since);
    }

    @Test public void saidOnlyOnceBothReadItAndTheOneToldCountsThisDeviceAsMine() {
        Introducing.Pending one=Introducing.pending("laptop:pro:1000");
        long now=2000;
        assertEquals(Introducing.Step.SAY,Introducing.weigh(one,now,true,true,false,true,true,true));
        assertEquals("a build from before never says it reads one: it waits, never told",Introducing.Step.WAIT,Introducing.weigh(one,now,true,true,false,false,true,true));
        assertEquals("nor about a device whose build would not take the hello",Introducing.Step.WAIT,Introducing.weigh(one,now,true,true,false,true,false,true));
        assertEquals("nor to one that does not count this device as mine",Introducing.Step.WAIT,Introducing.weigh(one,now,true,true,false,true,true,false));
        assertEquals(Introducing.Step.DONE,Introducing.weigh(one,now,true,true,true,true,true,true));
        assertEquals("forgotten, or no longer mine: nothing said about it",Introducing.Step.DROP,Introducing.weigh(one,now,false,true,false,true,true,true));
        assertEquals(Introducing.Step.DROP,Introducing.weigh(one,now,true,false,true,true,true,true));
        assertEquals("two months on",Introducing.Step.DROP,Introducing.weigh(one,1000+Introducing.KEEP+1,true,true,false,false,false,false));
        assertEquals(Introducing.Step.DROP,Introducing.weigh(null,now,true,true,false,true,true,true));
        assertTrue(Introducing.due(0,now));
        assertFalse(Introducing.due(now-1000,now));
        assertTrue(Introducing.due(now-Introducing.AGAIN,now));
        assertTrue("a clock put back",Introducing.due(now+5,now));
    }

    @Test public void theHelloNamesMineAndAnOlderBuildReadsItAsAnyLinkHello() throws Exception {
        KeyPair sign=Envelope.keys(),agree=Envelope.keys();
        Hello.Said said=Hello.open(Hello.wrap(Introducing.hello("Pro","MxTEST@203.0.113.9:9001",Point.shorten(agree.getPublic()),Point.shorten(sign.getPublic()),true)));
        assertNotNull(said);
        assertTrue(Introducing.isMine(said));
        assertTrue("a LINK hello, as a build from before reads it",Linking.isLink(said));
        assertTrue(Linking.asks(said));
        assertFalse("never a target a share has, never empty",said.target.isEmpty());
        assertFalse(Introducing.isMine(Linking.hello("Pro","MxTEST@203.0.113.9:9001",Point.shorten(agree.getPublic()),Point.shorten(sign.getPublic()),true)));
        assertNotEquals(Receipt.LINKS_MINE,Receipt.STAMPS);assertEquals(41,Receipt.LINK_MINE);
        assertEquals(Receipt.LINK_MINE,Receipt.open(Receipt.wrap(Receipt.LINK_MINE)));
    }
}
