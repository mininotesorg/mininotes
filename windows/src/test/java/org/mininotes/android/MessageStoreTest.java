package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Brief messages for the Inbox (docs/HOME.md, decision 145), in the real notebook and through {@code Post.arrived}, with
 * synthetic devices and no network: kept once by their id, shown in threads with what is unseen counted, answered that
 * they are kept so their sender stops, never taken from a device not paired or blocked, and the build that reads them heard.
 */
public class MessageStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxMessageOwnerFixture@127.0.0.1:9831",ANA_AT="MxMessageAnaFixture@127.0.0.1:9832",
        BEN_AT="MxMessageBenFixture@127.0.0.1:9833";

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
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] message(Device to,Message.Card card) throws Exception {
            return Envelope.seal(card.id,0,System.currentTimeMillis(),Message.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
        byte[] said(Device to,byte[] page,int what) throws Exception {
            return Envelope.seal(page,0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private Device a,ana,ben;
    @Before public void three() throws Exception {
        a=new Device("Test PC",A_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.pair(ana,"Ana");ana.pair(a,"Test PC");
    }

    @Test public void theNotebookKeepsEachMessageOnceInThreads() throws Exception {
        assertTrue(a.store.keepMessage("01","x",true,Message.TEXT,"first","",100));
        assertFalse("the same id is kept once",a.store.keepMessage("01","x",true,Message.TEXT,"again","",101));
        assertTrue(a.store.keepMessage("02","x",false,Message.TEXT,"reply","",200));
        assertTrue(a.store.keepMessage("03","y",false,Message.ASK_PARLONS,"","",150));
        assertEquals(List.of("first","reply"),a.store.messagesWith("x").stream().map(NoteStore.Said::line).toList());
        List<NoteStore.Said> threads=a.store.threads();
        assertEquals(2,threads.size());assertEquals("02",threads.get(0).id);assertEquals("03",threads.get(1).id);
        assertEquals(Message.ASKS,threads.get(1).line());
        assertEquals(2,a.store.unseenMessages());assertEquals(1,a.store.unseenFrom("x"));
        a.store.seenFrom("x");assertEquals(1,a.store.unseenMessages());assertEquals(0,a.store.unseenFrom("x"));
        assertEquals(List.of("01"),a.store.undeliveredMessages().stream().map(m->m.id).toList());
        a.store.messageHanded("01",500);assertEquals(1,a.store.message("01").tries);assertEquals(500,a.store.message("01").handedAt);
        a.store.messageDelivered("01");assertTrue(a.store.undeliveredMessages().isEmpty());
        a.store.messageDelivered("02");assertFalse("only what went from here is delivered",a.store.message("02").delivered);
    }

    @Test public void myParlonsAddressIsCheckedKeptAndForgotten() {
        assertEquals("",a.store.myParlons());
        String kept=a.store.setMyParlons("MxG18HGGSYNTHETICOWNER000000000000000000@127.0.0.1:9001");
        assertEquals(kept,a.store.myParlons());
        assertThrows(IllegalArgumentException.class,()->a.store.setMyParlons("not an address"));
        assertEquals(kept,a.store.myParlons());
        a.store.setMyParlons("  ");assertEquals("",a.store.myParlons());
    }

    @Test public void aMessageArrivesIsKeptOnceAndItsAnswerSaysItGotThere() throws Exception {
        byte[] id=Message.newId();
        Message.Card card=new Message.Card(id,Message.ASK_PARLONS,"","",System.currentTimeMillis());
        ana.store.keepMessage(Message.hex(id),A_AT,true,Message.ASK_PARLONS,"","",card.at);
        byte[] sealed=ana.message(a,card);
        Post.Landed landed=a.hear(sealed);
        assertTrue(landed.message);assertEquals("Ana",landed.messageFrom);assertEquals(Message.ASKS,landed.messageSaid);assertEquals(ANA_AT,landed.messageAddress);
        assertEquals(1,a.store.messagesWith(ANA_AT).size());assertEquals(1,a.store.unseenMessages());
        Post.Landed twice=a.hear(sealed);
        assertTrue(twice.message);assertEquals("a second arrival is not news","",twice.messageFrom);
        assertEquals(1,a.store.messagesWith(ANA_AT).size());
        // Its answer, at Ana's: delivered, no longer handed on.
        assertEquals(1,ana.store.undeliveredMessages().size());
        assertTrue(ana.hear(a.said(ana,id,Receipt.MESSAGE_KEPT)).message);
        assertTrue(ana.store.undeliveredMessages().isEmpty());assertTrue(ana.store.message(Message.hex(id)).delivered);
    }

    @Test public void aBuildThatReadsMessagesIsHeard() throws Exception {
        NoteStore.Contact ana_=a.store.address(ANA_AT);
        assertFalse(Post.readsMessages(a.context,ana_));
        a.hear(ana.said(a,new byte[16],Receipt.MESSAGES));
        assertTrue(Post.readsMessages(a.context,a.store.address(ANA_AT)));
        // Sending to somebody whose build has not said so: kept, never sealed for them, to go once they say it.
        a.pair(ben,"Ben");
        assertEquals(Post.Sent.WAITS_FOR_THEM,Post.sendMessage(a.context,a.store,a.keys,BEN_AT,Message.TEXT,"hi",""));
        assertEquals(1,a.store.messagesWith(BEN_AT).size());assertEquals(0,a.store.messagesWith(BEN_AT).get(0).tries);
        // To one whose build has: kept here first, to go when it can, offline.
        Post.Sent sent=Post.sendMessage(a.context,a.store,a.keys,ANA_AT,Message.TEXT,"Hello Ana","");
        assertNotEquals(Post.Sent.WAITS_FOR_THEM,sent);
        assertEquals(1,a.store.messagesWith(ANA_AT).size());assertTrue(a.store.messagesWith(ANA_AT).get(0).outgoing);
    }

    @Test public void nothingIsTakenFromADeviceNotPairedOrBlocked() throws Exception {
        Message.Card card=new Message.Card(Message.newId(),Message.TEXT,"from a stranger","",System.currentTimeMillis());
        assertFalse(a.hear(ben.message(a,card)).message);
        assertTrue(a.store.threads().isEmpty());
        a.pair(ben,"Ben");a.store.blockSender(BEN_AT,"Ben");
        assertFalse(a.hear(ben.message(a,card)).message);
        assertTrue(a.store.threads().isEmpty());
    }

    @Test public void aCardWhoseIdIsNotItsEnvelopesIsDropped() throws Exception {
        Message.Card card=new Message.Card(Message.newId(),Message.TEXT,"mismatched","",System.currentTimeMillis());
        byte[] sealed=Envelope.seal(Message.newId(),0,System.currentTimeMillis(),Message.wrap(card),ana.keys.signing(),a.keys.agreement().getPublic());
        assertFalse(a.hear(sealed).message);assertTrue(a.store.threads().isEmpty());
    }
}
