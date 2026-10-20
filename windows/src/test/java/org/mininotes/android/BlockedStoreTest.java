package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A blocked sender (the owner, 2026-10-07; docs/HOME.md, decision 127), in the real notebook and through
 * {@code Post.arrived}, with synthetic devices and no network: the record is kept on this device, a later message
 * from them is dropped, a note already kept stays, and deleting the record lets a later message in.
 */
public class BlockedStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxBlockedOwnerFixture@127.0.0.1:9831",ANA_AT="MxBlockedAnaFixture@127.0.0.1:9832";

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
    }

    @Test public void blockingDropsWhatTheySendNextAndDeletingTheRecordLetsThemShareAgain() throws Exception {
        Device a=new Device("Test PC",A_AT),ana=new Device("Ana",ANA_AT);
        a.pair(ana,"Ana");
        assertFalse(a.store.senderBlocked(ANA_AT));
        assertTrue(a.store.blockedSenders().isEmpty());
        a.store.blockSender(null,"no");a.store.blockSender("","no");
        a.store.blockSender(ANA_AT,"Ana\nExtra\tName");
        assertTrue(a.store.senderBlocked(ANA_AT));
        List<String[]> listed=a.store.blockedSenders();
        assertEquals(1,listed.size());
        assertEquals(ANA_AT,listed.get(0)[0]);
        assertEquals("Ana Extra Name",listed.get(0)[1]);
        a.store.blockSender(ANA_AT,"Again");
        assertEquals("a second block changes nothing",1,a.store.blockedSenders().size());
        assertEquals("Ana Extra Name",a.store.blockedSenders().get(0)[1]);
        NoteStore.Note kept=new NoteStore.Note();kept.title="Kept";kept.body="Synthetic kept";a.store.save(kept);
        long moment=System.currentTimeMillis();
        byte[] locked=Envelope.seal(new byte[16],moment,moment,Receipt.wrap(Receipt.LOCKED),ana.keys.signing(),a.keys.agreement().getPublic());
        Post.arrived(a.context,a.store,a.keys,locked);
        assertFalse("a blocked sender's message is dropped",a.store.lockedThere().contains(ANA_AT));
        assertNotNull("a note already kept stays",a.store.get(kept.id));
        a.store.forgetBlocked(ANA_AT);
        assertFalse(a.store.senderBlocked(ANA_AT));
        assertTrue(a.store.blockedSenders().isEmpty());
        long later=moment+5;
        byte[] again=Envelope.seal(new byte[16],later,later,Receipt.wrap(Receipt.LOCKED),ana.keys.signing(),a.keys.agreement().getPublic());
        Post.arrived(a.context,a.store,a.keys,again);
        assertTrue("deleting the record lets them share again",a.store.lockedThere().contains(ANA_AT));
        assertNotNull(a.store.get(kept.id));
    }
}
