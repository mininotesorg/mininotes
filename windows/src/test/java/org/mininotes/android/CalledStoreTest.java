package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The name we pick (the owner, 2026-10-07; docs/HOME.md, decision 124), in the real notebook and through
 * {@code Post.arrived}, with synthetic devices and no network: set and read back, through a re-scan too; the later
 * decision stands; a name kept on this device only is left alone; the name they publish still moves, and the one we
 * pick stays the one shown; somebody not known is not made up; a card about this device itself is kept in its settings.
 */
public class CalledStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxCalledOwnerFixture@127.0.0.1:9821",A2_AT="MxCalledOwnersPhoneFixture@127.0.0.1:9822",
        ANA_AT="MxCalledAnaFixture@127.0.0.1:9824",BEN_AT="MxCalledBenFixture@127.0.0.1:9825";

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
            store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,other.at,true));
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
        byte[] card(Device to,String about) throws Exception {
            Called.Card card=store.calledCard(about);
            return Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),Called.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private Device a,a2,ana,ben;
    @Before public void four() throws Exception {
        a=new Device("Test PC",A_AT);a2=new Device("Test phone",A2_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.bond(a2,"Test phone");a2.bond(a,"Test PC");
        a.pair(ana,"Ana");a.pair(ben,"Ben");a2.pair(ana,"Ana");
    }

    @Test public void aNameWePickIsShownAndTheNameTheyShareStays() throws Exception {
        NoteStore.Contact before=a.store.address(ANA_AT);
        assertEquals("Ana",before.name);assertEquals("Ana",before.shared());assertEquals("",before.picked);assertFalse(before.showPicked);assertFalse(before.pickedHere);
        long now=System.currentTimeMillis();
        a.store.setCalled(ANA_AT,"Annie",false,false,now);
        NoteStore.Contact kept=a.store.address(ANA_AT);
        assertEquals("theirs stays shown until we say so","Ana",kept.name);
        assertEquals("Annie",kept.picked);assertEquals("Ana",kept.shared());assertFalse(kept.showPicked);assertFalse(kept.pickedHere);
        a.store.setCalled(ANA_AT,"Annie",true,false,now+1);
        NoteStore.Contact shown=a.store.address(ANA_AT);
        assertEquals("Annie",shown.name);assertEquals("Ana",shown.shared());assertTrue(shown.showPicked);
        a.store.setCalled(ANA_AT,"  ",true,true,now+2);
        NoteStore.Contact cleared=a.store.address(ANA_AT);
        assertEquals("clearing it shows theirs","Ana",cleared.name);assertEquals("",cleared.picked);assertFalse(cleared.showPicked);assertTrue(cleared.pickedHere);
        // Scanned again: the name we pick is still hers.
        a.store.setCalled(ANA_AT,"Annie",true,false,now+3);
        a.pair(ana,"Ana again");
        NoteStore.Contact again=a.store.address(ANA_AT);
        assertEquals("Annie",again.name);assertEquals("Ana",again.shared());assertEquals("Annie",again.picked);assertTrue(again.showPicked);
        // What they publish moves. The name we pick keeps being the one shown.
        assertTrue(a.store.ownDeviceNamed(ana.keys.signing().getPublic().getEncoded(),"Ana phone",now+4));
        NoteStore.Contact published=a.store.address(ANA_AT);
        assertEquals("Ana phone",published.shared());assertEquals("Annie",published.name);assertEquals("Annie",published.picked);
        assertFalse("the same name again is no news",a.store.ownDeviceNamed(ana.keys.signing().getPublic().getEncoded(),"Ana phone",now+4));
    }

    @Test public void theLaterDecisionStandsAndOnlyThisDeviceIsLeftAlone() throws Exception {
        long first=System.currentTimeMillis();
        a.store.setCalled(ANA_AT,"Annie",true,false,first);
        Post.Landed took=a2.hear(a.card(a2,ANA_AT));
        assertTrue("an open People and devices is drawn again",took.contacts);
        assertEquals("Annie",a2.store.address(ANA_AT).name);
        assertEquals("Ana",a2.store.address(ANA_AT).shared());
        assertEquals(first,a2.store.address(ANA_AT).pickedAt);
        assertFalse("the same card again is no news",a2.hear(a.card(a2,ANA_AT)).contacts);
        byte[] early=a.card(a2,ANA_AT);
        a2.store.setCalled(ANA_AT,"Local",true,true,first+10);
        assertFalse(a2.hear(early).contacts);
        assertEquals("Only this device keeps its own","Local",a2.store.address(ANA_AT).name);
        assertTrue(a2.store.address(ANA_AT).pickedHere);
        a.store.setCalled(ANA_AT,"Ann",true,false,first+20);
        assertFalse("a later card does not overwrite a name kept here",a2.hear(a.card(a2,ANA_AT)).contacts);
        assertEquals("Local",a2.store.address(ANA_AT).name);
        // On the PC the later name stands, and it is still the one they share underneath.
        assertEquals("Ann",a.store.address(ANA_AT).name);assertEquals("Ana",a.store.address(ANA_AT).shared());
        a.hear(a2.said(a,Receipt.CALLED));
        assertTrue(Post.readsCalled(a.context,a.store.address(A2_AT)));
        assertFalse(Post.readsCalled(a.context,a.store.address(ANA_AT)));
    }

    @Test public void somebodyNotKnownIsNotMadeUp() throws Exception {
        a.store.setCalled(BEN_AT,"Benny",true,false,System.currentTimeMillis());
        int before=a2.store.addresses().size();
        assertFalse(a2.hear(a.card(a2,BEN_AT)).contacts);
        assertNull(a2.store.address(BEN_AT));
        assertEquals(before,a2.store.addresses().size());
        a2.pair(ben,"Ben");
        assertEquals("Ben",a2.store.address(BEN_AT).name);
        assertTrue(a2.hear(a.card(a2,BEN_AT)).contacts);
        assertEquals("Benny",a2.store.address(BEN_AT).name);assertEquals("Ben",a2.store.address(BEN_AT).shared());
    }

    @Test public void aCardAboutThisDeviceIsKeptInItsSettingsUnlessItStaysHere() throws Exception {
        long now=System.currentTimeMillis();
        String key=a.store.keyOf(a.keys.signing().getPublic().getEncoded());
        Called.Card card=new Called.Card(key,"Pocket",true,now);
        byte[] sealed=Envelope.seal(new byte[16],now,System.currentTimeMillis(),Called.wrap(card),a2.keys.signing(),a.keys.agreement().getPublic());
        assertTrue(a.hear(sealed).contacts);
        var kept=a.context.getSharedPreferences("settings",0);
        assertEquals("Pocket",kept.getString(Called.PREF_PICKED,""));
        assertEquals(1,kept.getInt(Called.PREF_SHOW,0));
        assertEquals(now,kept.getLong(Called.PREF_AT,0));
        assertEquals("Pocket",Called.display("Test PC",kept.getString(Called.PREF_PICKED,""),kept.getInt(Called.PREF_SHOW,0)==1));
        // Kept on this device only: a later card does not replace it.
        kept.edit().putString(Called.PREF_PICKED,"Here").putInt(Called.PREF_SHOW,1).putInt(Called.PREF_HERE,1).putLong(Called.PREF_AT,now+5).apply();
        Called.Card later=new Called.Card(key,"Other",true,now+20);
        byte[] again=Envelope.seal(new byte[16],now+20,System.currentTimeMillis(),Called.wrap(later),a2.keys.signing(),a.keys.agreement().getPublic());
        assertFalse(a.hear(again).contacts);
        assertEquals("Here",a.context.getSharedPreferences("settings",0).getString(Called.PREF_PICKED,""));
    }
}
