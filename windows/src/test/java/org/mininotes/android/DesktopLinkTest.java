package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Linked through what you share, in the real notebook and through {@code Post.arrived}, with synthetic devices and
 * no network: each message is sealed and handed to the device it is for. A shares a note with B and C; B links with C
 * through the list, C takes B's hello because its own list names B, a stranger's hello is not taken, and a device
 * taken off everything stays known and is sent nothing. See docs/SHARING.md, Linked through what you share.
 */
public class DesktopLinkTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxLinkOwnerFixture@127.0.0.1:9301",B_AT="MxLinkTabletFixture@127.0.0.1:9302",
        C_AT="MxLinkPcFixture@127.0.0.1:9303",D_AT="MxLinkStrangerFixture@127.0.0.1:9304",E_AT="MxLinkMadeUpFixture@127.0.0.1:9305";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=key();store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        String key() throws Exception {return Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());}
        void pair(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Parcel.Member member(int level,long changed,boolean seal) throws Exception {
            return new Parcel.Member(key(),at,store.myName,level,changed,seal?Point.shorten(keys.agreement().getPublic()):null);
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        /** A note from this device, sealed for another; answer is not asked, so nothing here reaches for a node. */
        byte[] note(Device to,String id,long revision,String body,List<Parcel.Member> members,boolean files) throws Exception {
            List<Parcel.Member> all=new ArrayList<>(members);all.add(member(Sharing.Level.ADMIN.said(),1L,true));
            byte[] parcel=Parcel.wrap(new Parcel.Sent("","","","","Plans",body,true,all,"PAGE",id,false,-1L,true,files?List.of():null,1L));
            return Envelope.seal(page(id),revision,System.currentTimeMillis(),parcel,keys.signing(),to.keys.agreement().getPublic());
        }
        byte[] hello(Device to,boolean asking) throws Exception {
            byte[] said=Hello.wrap(Linking.hello(store.myName,at,Point.shorten(keys.agreement().getPublic()),Point.shorten(keys.signing().getPublic()),asking));
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),said,keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    @Test public void acceptingAgainBringsAnEarlierCopyOutOfTheBin() throws Exception {
        // The owner, 2026-10-02: a collection shared again arrived into the bin, where an earlier copy had been put.
        Device a=new Device("Test phone",A_AT),b=new Device("Test tablet",B_AT);
        b.pair(a,"Test phone");
        String id=UUID.randomUUID().toString();
        b.hear(a.note(b,id,1,"first line",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true)),false));
        assertEquals("first line",b.store.get(id).body);
        b.store.putAway(NoteStore.Branch.Kind.PAGE,id,true,true);
        assertTrue("in the bin",b.store.get(id).deleted);
        assertTrue("accepted again: back out",b.store.acceptedBack(A_AT,id));
        assertFalse(b.store.get(id).deleted);
        assertFalse("nothing to bring back now",b.store.acceptedBack(A_AT,id));
        b.store.putAway(NoteStore.Branch.Kind.PAGE,id,false,true);
        assertTrue("out of the archive too",b.store.acceptedBack(A_AT,id));
        assertFalse(b.store.get(id).archived);
        assertFalse("something never here: nothing",b.store.acceptedBack(A_AT,UUID.randomUUID().toString()));
    }

    @Test public void theOwnersOtherDevicesAreOnTheListSoTheOthersTakeTheirHello() throws Exception {
        // The owner, 2026-10-03: the grocery list went between Parisa and the PC that shared it, never between Parisa and
        // his phones, which have it through Connect my other device and so were on no list she had.
        Device pc=new Device("Test PC",C_AT),phone=new Device("Test phone",A_AT),friend=new Device("Somebody else",D_AT);
        pc.store.pairedWith(phone.at,"Test phone",true,phone.keys.agreement().getPublic().getEncoded(),phone.keys.signing().getPublic().getEncoded());
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,A_AT,true));
        pc.pair(friend,"Somebody else");
        String id=UUID.randomUUID().toString();
        NoteStore.Note note=new NoteStore.Note();note.id=id;note.book=pc.store.someBook();note.title="Plans";note.body="milk";note.revision=1;pc.store.save(note);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,D_AT,Sharing.Level.WRITE,5_000L,friend.key()));

        List<Parcel.Member> list=pc.store.travelling(Sharing.Scope.PAGE,id);
        byte[] phoneSeal=keyFor(list,phone.key());
        assertNotNull("the owner's phone is on the list, with its key to seal for",phoneSeal);
        assertArrayEquals(Point.shorten(phone.keys.agreement().getPublic()),phoneSeal);
        String phoneKey=phone.key();
        assertEquals("once",1,list.stream().filter(m->m.key.equals(phoneKey)).count());
        assertNull("nobody else's device",keyFor(list,new Device("Made up",E_AT).key()));

        friend.pair(pc,"Test PC");
        List<Parcel.Member> others=new ArrayList<>();for(Parcel.Member one:list)if(!one.key.equals(pc.key()))others.add(one);
        friend.hear(pc.note(friend,id,1,"milk",others,true));
        assertEquals("milk",friend.store.get(id).body);
        // The friend links the phone through the list, and takes its hello.
        assertTrue(friend.store.address(A_AT).listed());
        friend.hear(phone.hello(friend,false));
        assertFalse(friend.store.linkingNow().contains(A_AT));
    }

    /**
     * The colour each person chose comes with their notes (the owner, 2026-10-03): drawn in it on the other side, under a
     * colour given there, and between the owner's own devices the later choice wins.
     */
    @Test public void aPersonsOwnColourComesWithTheirNotes() throws Exception {
        Device a=new Device("Test phone",A_AT),b=new Device("Test tablet",B_AT);
        b.pair(a,"Test phone");
        String id=UUID.randomUUID().toString();
        Parcel.Member bOn=b.member(Sharing.Level.WRITE.said(),5_000L,true);
        List<Parcel.Member> list=new ArrayList<>(List.of(bOn));list.add(a.member(Sharing.Level.ADMIN.said(),1L,true));
        Parcel.Sent going=new Parcel.Sent("","","","","Plans","milk",true,list,"PAGE",id,false,-1L,true,List.of(),1L,List.of(),List.of(),"",null);
        going.ink=3;going.inkAt=10_000L;
        byte[] sealed=Envelope.seal(page(id),1,System.currentTimeMillis(),Parcel.wrap(going),a.keys.signing(),b.keys.agreement().getPublic());
        b.hear(sealed);
        String writer=b.store.writerOf(A_AT);
        assertEquals("drawn in the colour they chose",3,b.store.palette().colourOf(writer));
        // A colour given to them on this device wins, and taking it back shows theirs again.
        b.store.chooseInk(b.store.palette().person(writer),5);
        assertEquals(5,b.store.palette().colourOf(writer));
        b.store.chooseInk(b.store.palette().person(writer),Tint.NONE);
        assertEquals(3,b.store.palette().colourOf(writer));
        // An older word from them changes nothing.
        assertFalse(b.store.inkSaid(b.store.address(A_AT),1,9_000L));
        assertEquals(3,b.store.palette().colourOf(writer));

        // One of the owner's own devices: its choice is the owner's own here, where it is the later one.
        NoteStore.Contact own=new NoteStore.Contact("MxOwnFixture@127.0.0.1:9309","Own",true);
        b.store.chooseInk(Writers.ME,2);
        long chosen=b.store.myInk()[1];
        assertFalse("older than this device's own choice",b.store.inkSaid(own,6,chosen-1));
        assertEquals(2,b.store.palette().mine);
        assertTrue(b.store.inkSaid(own,6,chosen+1));
        assertEquals(6,b.store.palette().mine);
        assertEquals(6,b.store.myInk()[0]);
    }

    /** A note made temporary carries its time to whoever has it, and goes there when it comes (decision 71). */
    @Test public void aTemporaryNotesTimeGoesWithIt() throws Exception {
        Device a=new Device("Test phone",A_AT),b=new Device("Test tablet",B_AT);
        b.pair(a,"Test phone");
        String id=UUID.randomUUID().toString();
        List<Parcel.Member> list=new ArrayList<>(List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true)));list.add(a.member(Sharing.Level.ADMIN.said(),1L,true));
        long until=System.currentTimeMillis()+3_600_000L;
        for(long revision=1;revision<=2;revision++) {
            Parcel.Sent going=new Parcel.Sent("","","","","Plans","milk",true,list,"PAGE",id,false,-1L,true,List.of(),1L,List.of(),List.of(),"",null);
            going.until=revision==1?0L:until;
            b.hear(Envelope.seal(page(id),revision,System.currentTimeMillis(),Parcel.wrap(going),a.keys.signing(),b.keys.agreement().getPublic()));
        }
        assertEquals(until,b.store.untilOf(NoteStore.Branch.Kind.PAGE,id));
        assertEquals(1,b.store.expire(until+1));
        assertNull("gone there too",b.store.get(id));
    }

    @Test public void devicesOnOneListLinkThroughItAndNobodyElseDoes() throws Exception {
        Device a=new Device("Test phone",A_AT),b=new Device("Test tablet",B_AT),c=new Device("Test PC",C_AT),d=new Device("Somebody else",D_AT);
        b.pair(a,"Test phone");c.pair(a,"Test phone");
        String id=UUID.randomUUID().toString();
        List<Parcel.Member> list=List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true),c.member(Sharing.Level.WRITE.said(),5_000L,true));

        // B gets A's note with the keys on its list: C is linked through it, written down as such, and waited for.
        b.hear(a.note(b,id,1,"first line",list,true));
        assertEquals("first line",b.store.get(id).body);
        NoteStore.Contact cAtB=b.store.address(C_AT);
        assertNotNull(cAtB);assertTrue(cAtB.paired());assertTrue(cAtB.listed());assertFalse(cAtB.mine);
        assertEquals("Test PC",cAtB.name);
        assertTrue(b.store.linkingNow().contains(C_AT));
        // Its round says so, and the thing is not sent to it until it answers.
        SyncStatus.Person round=null;for(SyncStatus.Person one:SyncStatus.who(b.store,id))if(one.address().equals(C_AT))round=one;
        assertNotNull(round);assertFalse(round.linked());assertTrue(round.linking());
        assertEquals("Linking with Test PC… it goes once they answer.",round.notLinked("this PC"));
        assertEquals("Linked through “Plans”",b.store.linkedLines().get(C_AT));
        // A was paired by a code, and says nothing of the sort.
        assertFalse(b.store.address(A_AT).listed());assertNull(b.store.linkedLines().get(A_AT));

        // C's copy came without the keys - too long for a list of files - so C cannot reach B by itself.
        c.hear(a.note(c,id,1,"first line",list,false));
        assertNull(c.store.address(B_AT));
        // B's hello, asking: C's own list names B, so it is taken - linked, not waited for, since B asked.
        Post.Landed took=c.hear(b.hello(c,true));
        assertNull(took.accepted);
        NoteStore.Contact bAtC=c.store.address(B_AT);
        assertNotNull(bAtC);assertTrue(bAtC.paired());assertTrue(bAtC.listed());
        assertFalse(c.store.linkingNow().contains(B_AT));

        // A stranger that knows C's address and its key to seal for asks too: named by no list at C, not taken.
        Post.Landed refused=c.hear(d.hello(c,true));
        assertNull(refused.accepted);assertNull(refused.said);
        assertNull(c.store.address(D_AT));
        // And what it sends as a note is written down nowhere.
        c.hear(d.note(c,id,9,"written over",List.of(c.member(Sharing.Level.WRITE.said(),9_000L,true)),true));
        assertEquals("first line",c.store.get(id).body);

        // C answers; B stops waiting, and C wears a normal round there.
        b.hear(c.hello(b,false));
        assertFalse(b.store.linkingNow().contains(C_AT));
        for(SyncStatus.Person one:SyncStatus.who(b.store,id))if(one.address().equals(C_AT))assertTrue(one.linked());
        // And B's list now hands on the key to seal for C.
        boolean handsOn=false;
        for(Parcel.Member one:b.store.travelling(Sharing.Scope.PAGE,id))if(one.key.equals(c.key()))handsOn=Arrays.equals(Point.shorten(c.keys.agreement().getPublic()),one.agreement);
        assertTrue(handsOn);

        // B writes; it reaches C straight from B, and is taken as B's - a writer on A's list.
        c.hear(b.note(c,id,2,"first line\nfrom the tablet",List.of(c.member(Sharing.Level.WRITE.said(),5_000L,true)),true));
        assertEquals("first line\nfrom the tablet",c.store.get(id).body);

        // A writer's list links nobody: B names a stranger with keys, and C writes down no contact for it.
        Device e=new Device("Made up",E_AT);
        c.hear(b.note(c,id,3,"first line\nfrom the tablet\nagain",List.of(e.member(Sharing.Level.ADMIN.said(),9_000L,true)),true));
        assertNull(c.store.address(E_AT));
        assertFalse(c.store.linkingNow().contains(E_AT));

        // A takes C off the note. At B, C stays known - linked through something it no longer has - and is sent nothing.
        b.hear(a.note(b,id,4,"first line\nfrom the tablet\nagain",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true),
            c.member(Sharing.Level.GONE.said(),8_000L,true)),true));
        assertNotNull(b.store.address(C_AT));
        assertEquals("Linked through something it no longer has. Nothing is sent to it.",b.store.linkedLines().get(C_AT));
        assertFalse(b.store.everybodyIn(NoteStore.Branch.Kind.PAGE,id).contains(C_AT));
        assertNull("nobody taken off is handed on",keyFor(b.store.travelling(Sharing.Scope.PAGE,id),c.key()));
    }

    /**
     * Seen on 0.1.026 between two phones and a PC: the list named a device at the address its owner last saw it at, the
     * row stayed there, and the device was linked - and met - at another. Its round stayed dashed, People said it was
     * linked through nothing, and the device waited for one hello for ever while notes went both ways.
     */
    @Test public void aDeviceLinkedAtAnotherAddressIsFiledWhereItIsAndAnythingFromItEndsTheWaiting() throws Exception {
        String cBefore="MxLinkPcEarlierFixture@127.0.0.1:9313";
        Device a=new Device("Test phone",A_AT),b=new Device("Test tablet",B_AT),c=new Device("Test PC",C_AT);
        b.pair(a,"Test phone");
        String id=UUID.randomUUID().toString();
        // The first copy came without keys, naming C where A had last seen it.
        Parcel.Member earlier=new Parcel.Member(c.key(),cBefore,"Test PC",Sharing.Level.WRITE.said(),5_000L,null);
        b.hear(a.note(b,id,1,"first line",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true),earlier),false));
        assertNull(b.store.address(cBefore));
        // The next came with keys and C where it is now, the same decision: the row is not moved by the list, and C is
        // linked - at the address it is at now.
        b.hear(a.note(b,id,2,"first line\nsecond",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true),
            c.member(Sharing.Level.WRITE.said(),5_000L,true)),true));
        assertNotNull(b.store.address(C_AT));
        assertTrue("its row is where its contact is",b.store.everybodyIn(NoteStore.Branch.Kind.PAGE,id).contains(C_AT));
        assertFalse(b.store.everybodyIn(NoteStore.Branch.Kind.PAGE,id).contains(cBefore));
        assertEquals("Linked through “Plans”",b.store.linkedLines().get(C_AT));
        assertTrue(b.store.linkingNow().contains(C_AT));

        // C's hello back went astray; a note from C comes instead. Signed by C, it is C's answer: the waiting ends.
        c.store.linkThrough(B_AT,"Test tablet",Point.shorten(b.keys.agreement().getPublic()),b.keys.signing().getPublic().getEncoded(),false);
        b.hear(c.note(b,id,3,"first line\nsecond\nfrom the PC",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true)),true));
        assertFalse(b.store.linkingNow().contains(C_AT));
        assertEquals("first line\nsecond\nfrom the PC",b.store.get(id).body);
        SyncStatus.Person round=null;for(SyncStatus.Person one:SyncStatus.who(b.store,id))if(one.address().equals(C_AT))round=one;
        assertNotNull(round);assertTrue("a normal round",round.linked());
        // A later list naming C at its old address files it where its contact is, too.
        b.hear(a.note(b,id,4,"first line\nsecond\nfrom the PC",List.of(b.member(Sharing.Level.WRITE.said(),5_000L,true),
            new Parcel.Member(c.key(),cBefore,"Test PC",Sharing.Level.WRITE.said(),7_000L,null)),true));
        assertTrue(b.store.everybodyIn(NoteStore.Branch.Kind.PAGE,id).contains(C_AT));
        assertFalse(b.store.everybodyIn(NoteStore.Branch.Kind.PAGE,id).contains(cBefore));
    }

    /** A notebook the first linking build left behind - a row at the old address, a waiting row for nobody - put right. */
    @Test public void whatTheFirstBuildLeftIsPutRight() throws Exception {
        Device b=new Device("Test tablet",B_AT),c=new Device("Test PC",C_AT);
        String id=UUID.randomUUID().toString(),old="MxLinkPcEarlierFixture@127.0.0.1:9313";
        b.store.linkThrough(C_AT,"Test PC",Point.shorten(c.keys.agreement().getPublic()),c.keys.signing().getPublic().getEncoded(),true);
        b.store.mergeMembership(Sharing.Scope.PAGE,id,List.of(new Sharing.Rule(Sharing.Scope.PAGE,id,old,Sharing.Level.WRITE,5_000L,"")),null);
        b.store.getWritableDatabase().execSQL("UPDATE shares SET who=? WHERE address=?",new String[]{c.key(),old});
        b.store.accepting("MxGoneFixture@127.0.0.1:9399","Gone",Linking.SCOPE,Linking.TARGET,true);
        b.store.tidyLinks();
        List<Sharing.Rule> rows=b.store.membership(Sharing.Scope.PAGE,id);
        assertEquals(1,rows.size());assertEquals(C_AT,rows.get(0).address);
        assertEquals(Set.of(C_AT),b.store.linkingNow());
    }

    private static byte[] keyFor(List<Parcel.Member> members,String key) {
        for(Parcel.Member one:members)if(one.key.equals(key))return one.agreement.length==0?null:one.agreement;
        return null;
    }
}
