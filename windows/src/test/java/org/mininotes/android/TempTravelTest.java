package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A note let go on Temp on one of the owner's devices arrives on another as temporary, with its time, so it is in Temp there
 * too and not only on Home (the owner, 2026-10-11: "I shared it with Temp from the laptop, it went on the desktop on mobile").
 * Real notebooks, {@code Post.arrived}, no network.
 */
public class TempTravelTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC_AT="MxTempTravelPc@127.0.0.1:9871",PHONE_AT="MxTempTravelPhone@127.0.0.1:9872";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        void bond(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
            store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,other.at,true));
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    /** Made from Temp's +, then written in: still temporary, and its wait and its own settings kept (the owner, 2026-10-11). */
    @Test public void writingInATemporaryNoteKeepsItTemporary() throws Exception {
        Device pc=new Device("Test PC",PC_AT);
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;pc.store.save(note);
        long until=System.currentTimeMillis()+3_600_000L;
        pc.store.makeTemporary(NoteStore.Branch.Kind.PAGE,note.id,until);
        pc.store.setPause(NoteStore.Branch.Kind.PAGE,note.id,30);
        // Written in, as the page does it: the note it holds is the one from before it was made temporary.
        note.body="Bread\nMilk";note.revision=pc.store.get(note.id).revision+1;pc.store.save(note);
        note.body="Bread\nMilk\nEggs";note.revision++;pc.store.save(note);
        assertEquals("still temporary",until,pc.store.untilOf(NoteStore.Branch.Kind.PAGE,note.id));
        assertEquals("its wait kept",30,pc.store.pauseOn(NoteStore.Branch.Kind.PAGE,note.id));
        assertEquals("Bread\nMilk\nEggs",pc.store.get(note.id).body);
    }

    /** Temp goes to the devices chosen for it, or to all of them where none were chosen (decision 150). */
    @Test public void tempGoesToTheDevicesChosenForIt() throws Exception {
        Device pc=new Device("Test PC",PC_AT),phone=new Device("Test phone",PHONE_AT),pro=new Device("Test Pro","MxTempTravelPro@127.0.0.1:9873");
        pc.store.pairedWith(PHONE_AT,"Test phone",true,phone.keys.agreement().getPublic().getEncoded(),phone.keys.signing().getPublic().getEncoded());
        pc.store.pairedWith(pro.at,"Test Pro",true,pro.keys.agreement().getPublic().getEncoded(),pro.keys.signing().getPublic().getEncoded());
        assertNull("all of them, as it always was",pc.store.tempDevices());
        NoteStore.Note one=new NoteStore.Note();one.book=Things.HOME;one.body="Screen";pc.store.save(one);
        pc.store.setTempDevices(java.util.Set.of(PHONE_AT));
        assertEquals(java.util.Set.of(PHONE_AT),pc.store.tempDevices());
        assertEquals("only the phone",1,pc.store.toMyDevices(Sharing.Scope.PAGE,one.id));
        Set<String> has=new HashSet<>();for(Sharing.Rule rule:pc.store.sharesOn(Sharing.Scope.PAGE,one.id))has.add(rule.address);
        assertTrue(has.contains(PHONE_AT));assertFalse(has.contains(pro.at));
        pc.store.setTempDevices(null);
        assertNull(pc.store.tempDevices());
        assertEquals("and the Pro too, once all are chosen",1,pc.store.toMyDevices(Sharing.Scope.PAGE,one.id));
        pc.store.setTempDevices(java.util.Set.of());
        assertEquals("none chosen: nobody",java.util.Set.of(),pc.store.tempDevices());
    }

    /** Made inside a temporary folder: temporary with it (decision 152). */
    @Test public void whatIsMadeInATemporaryFolderIsTemporary() throws Exception {
        Device pc=new Device("Test PC",PC_AT);
        NoteStore.Shelf folder=pc.store.addCollectionIn(Things.HOME,"Shots");
        long until=System.currentTimeMillis()+3_600_000L;
        pc.store.makeTemporary(NoteStore.Branch.Kind.COLLECTION,folder.id,until);
        NoteStore.Note note=new NoteStore.Note();note.book=folder.id;note.body="Later";pc.store.save(note);
        assertEquals("the note made in it",until,pc.store.untilOf(NoteStore.Branch.Kind.PAGE,note.id));
        NoteStore.Shelf inner=pc.store.addCollectionIn(folder.id,"Inner");
        assertEquals("and a folder made in it",until,pc.store.untilOf(NoteStore.Branch.Kind.COLLECTION,inner.id));
        NoteStore.Note plain=new NoteStore.Note();plain.book=Things.HOME;pc.store.save(plain);
        assertEquals("on Home: not",0L,pc.store.untilOf(NoteStore.Branch.Kind.PAGE,plain.id));
    }

    /** Temp goes to the people chosen for it too, to read (decision 151); to nobody else. */
    @Test public void tempGoesToThePeopleChosenForIt() throws Exception {
        Device pc=new Device("Test PC",PC_AT),friend=new Device("Friend","MxTempTravelFriend@127.0.0.1:9874"),other=new Device("Other","MxTempTravelOther@127.0.0.1:9875");
        pc.store.pairedWith(friend.at,"Friend",false,friend.keys.agreement().getPublic().getEncoded(),friend.keys.signing().getPublic().getEncoded());
        pc.store.pairedWith(other.at,"Other",false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        NoteStore.Note one=new NoteStore.Note();one.book=Things.HOME;one.body="Screen";pc.store.save(one);
        assertEquals("nobody unless chosen",0,pc.store.toMyDevices(Sharing.Scope.PAGE,one.id));
        pc.store.setTempPeople(Set.of(friend.at));
        assertEquals(Set.of(friend.at),pc.store.tempPeople());
        assertEquals("the friend",1,pc.store.toMyDevices(Sharing.Scope.PAGE,one.id));
        Map<String,Sharing.Level> has=new HashMap<>();for(Sharing.Rule rule:pc.store.sharesOn(Sharing.Scope.PAGE,one.id))has.put(rule.address,rule.level);
        assertEquals("to read",Sharing.Level.READ,has.get(friend.at));
        assertFalse(has.containsKey(other.at));
    }

    /** Temp's "Also show on Home", off: a temporary note or folder is in Temp only; on, on Home as well (the owner, 2026-10-11). */
    @Test public void whatIsInTempIsOnHomeOnlyWhereSwitchedOn() throws Exception {
        Device pc=new Device("Test PC",PC_AT);
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.body="Temp words";pc.store.save(note);
        NoteStore.Shelf folder=pc.store.addCollectionIn(Things.HOME,"Temp folder");
        long until=System.currentTimeMillis()+3_600_000L;
        pc.store.makeTemporary(NoteStore.Branch.Kind.PAGE,note.id,until);
        pc.store.makeTemporary(NoteStore.Branch.Kind.COLLECTION,folder.id,until);
        pc.store.setTempOnHome(false);
        Set<String> home=new HashSet<>();for(NoteStore.Branch one:pc.store.contents(Things.HOME))home.add(one.id);
        assertFalse("the note is not on Home",home.contains(note.id));
        assertFalse("nor the folder",home.contains(folder.id));
        Set<String> temp=new HashSet<>();for(NoteStore.Branch one:pc.store.temporary(System.currentTimeMillis()))temp.add(one.id);
        assertTrue("both in Temp",temp.contains(note.id)&&temp.contains(folder.id));
        pc.store.setTempOnHome(true);
        home.clear();for(NoteStore.Branch one:pc.store.contents(Things.HOME))home.add(one.id);
        assertTrue("on Home as well, once switched on",home.contains(note.id)&&home.contains(folder.id));
    }

    @Test public void aTemporaryNoteArrivesTemporary() throws Exception {
        Device pc=new Device("Test PC",PC_AT),phone=new Device("Test phone",PHONE_AT);
        pc.bond(phone,"Test phone");phone.bond(pc,"Test PC");
        long until=System.currentTimeMillis()+3_600_000L;
        String id=UUID.randomUUID().toString();
        Parcel.Sent going=new Parcel.Sent("","","","","Shopping","Bread",true,Collections.emptyList(),"","",false,-1,true,
            Collections.emptyList(),0L,Collections.emptyList(),new ArrayList<>(),"",null);
        going.until=until;
        byte[] sealed=Envelope.seal(page(id),2,System.currentTimeMillis(),Parcel.wrap(going,Envelope.MAX_TEXT),pc.keys.signing(),phone.keys.agreement().getPublic());
        Post.arrived(phone.context,phone.store,phone.keys,sealed);
        assertNotNull("the note arrived",phone.store.get(id));
        assertEquals("with its time",until,phone.store.untilOf(NoteStore.Branch.Kind.PAGE,id));
    }
}
