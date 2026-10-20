package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Your own Parlons! address, set once and sent by itself to everybody paired (docs/HOME.md, decision 147), through the real
 * notebook and {@code Post.arrived}, with synthetic devices and no network.
 */
public class OwnParlonsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxOwnParlonsOwner@127.0.0.1:9851",A2_AT="MxOwnParlonsOwnersPhone@127.0.0.1:9852",
        ANA_AT="MxOwnParlonsAna@127.0.0.1:9853",BEN_AT="MxOwnParlonsBen@127.0.0.1:9854";
    private static final String MINE="MxSYNTHETICOWNERPARLONS0000000000000000@127.0.0.1:9001",
        BENS="MxSYNTHETICBENPARLONS000000000000000000@127.0.0.1:9002";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name,boolean mine) throws Exception {
            store.pairedWith(other.at,name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] card(Device to,Parlons.Card card) throws Exception {
            return Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),Parlons.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
        byte[] said(Device to,int what) throws Exception {
            return Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private Device a,a2,ana,ben;
    @Before public void four() throws Exception {
        a=new Device("Test PC",A_AT);a2=new Device("Test phone",A2_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.pair(a2,"Test phone",true);a2.pair(a,"Test PC",true);
        a.pair(ana,"Ana",false);ana.pair(a,"Test PC",false);ana.pair(ben,"Ben",false);
    }

    @Test public void yourAddressIsKeptWithWhenAndGoesAsACardOfYourOwnKey() {
        assertEquals(0,a.store.myParlonsAt());
        String kept=a.store.setMyParlons(MINE,1000);
        assertEquals(kept,a.store.myParlons());assertEquals(1000,a.store.myParlonsAt());
        Parlons.Card card=a.store.myParlonsCard();
        assertEquals(kept,card.address);assertEquals(1000,card.decided);
        assertTrue("the card is about this device itself",ana.store.aboutSender(ana.store.address(A_AT),card));
        a.store.setMyParlons("",2000);assertEquals("",a.store.myParlons());assertEquals(2000,a.store.myParlonsAt());
    }

    @Test public void aContactsOwnAddressArrivesOnTheirCardAndAnOlderOneChangesNothing() throws Exception {
        a.store.setMyParlons(MINE,5000);
        assertTrue(ana.hear(a.card(ana,a.store.myParlonsCard())).contacts);
        assertEquals(a.store.myParlons(),ana.store.address(A_AT).parlons);
        // An older decision, arriving late: nothing changes.
        a.store.setMyParlons(BENS,4000);
        ana.hear(a.card(ana,a.store.myParlonsCard()));
        assertEquals(Parlons.address(MINE),ana.store.address(A_AT).parlons);
        // Removed later: removed there too.
        a.store.setMyParlons("",9000);
        ana.hear(a.card(ana,a.store.myParlonsCard()));
        assertEquals("",ana.store.address(A_AT).parlons);
    }

    @Test public void aCardAboutSomebodyElseFromAContactIsNotTaken() throws Exception {
        // Ana says what Ben's address is: only Ben, or one of the owner's own devices, says that.
        Parlons.Card aboutBen=new Parlons.Card(Base64.getEncoder().encodeToString(ben.keys.signing().getPublic().getEncoded()),BENS,7000);
        a.pair(ben,"Ben",false);
        assertFalse(a.hear(ana.card(a,aboutBen)).contacts);
        assertEquals("",a.store.address(BEN_AT).parlons);
    }

    @Test public void fromYourOwnDeviceItIsYoursToo() throws Exception {
        a2.store.setMyParlons(MINE,8000);
        a.hear(a2.card(a,a2.store.myParlonsCard()));
        assertEquals(Parlons.address(MINE),a.store.myParlons());assertEquals(8000,a.store.myParlonsAt());
    }

    @Test public void aBuildThatTakesItIsHeard() throws Exception {
        assertFalse(Post.readsOwnParlons(a.context,a.store.address(ANA_AT)));
        a.hear(ana.said(a,Receipt.PARLONS_OWN));
        assertTrue(Post.readsOwnParlons(a.context,a.store.address(ANA_AT)));
    }
}
