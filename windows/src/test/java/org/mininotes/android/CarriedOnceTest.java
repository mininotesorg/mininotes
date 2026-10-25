package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Decision 257 (Graphene, 2026-10-25): the same two revisions brought to Graphene again and again, each copy costing it a
 * receipt sealed and mined for the carrier. A carrier holds one copy of a note for whoever it is for, the newest; the same
 * revision left again is already held and keeps its turn; a carrier is told once in a while that a thing was collected; and
 * a device that never says back what it was told is told less and less often. Synthetic devices, no network.
 */
public class CarriedOnceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String WRITER="MxWriterOnce@127.0.0.1:9201",OTHER="MxOtherOnce@127.0.0.1:9202",
        CARRIER="MxCarrierOnce@127.0.0.1:9203",AWAY="MxAwayOnce@127.0.0.1:9204";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;
        Device(String name) throws Exception {
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
        }
        void pair(String address,String name,Device other) throws Exception {
            store.pairedWith(address,name,false,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        /**
         * That {@code of}'s build carries, as its notes' flag would have said; written straight in, so the device is not also
         * taken to be there just now and brought what is held at once, which would start a node.
         */
        void heardCarries(Device of) throws Exception {
            Set<String> all=new HashSet<>(context.getSharedPreferences("post",0).getStringSet("carries",new HashSet<String>()));
            all.add(Base64.getEncoder().encodeToString(of.keys.signing().getPublic().getEncoded()));
            context.getSharedPreferences("post",0).edit().putStringSet("carries",all).apply();
        }
        byte[] fingerprint() throws Exception {return Envelope.fingerprint(keys.signing().getPublic());}
        void hear(byte[] sealed){Post.arrived(context,store,keys,sealed);}
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    /** A copy of a note for the away phone, left with the carrier by {@code from}. */
    private static byte[] left(Device from,Device carrier,Device away,String id,long revision,int sort,byte[] inner) throws Exception {
        return Envelope.seal(page(id),revision,revision,Courier.leave(sort,away.fingerprint(),inner),from.keys.signing(),carrier.keys.agreement().getPublic());
    }

    @Test public void theCarrierKeepsOneCopyOfANoteForEachDeviceTheNewest() throws Exception {
        Device writer=new Device("writer"),other=new Device("other"),carrier=new Device("carrier"),away=new Device("away");
        carrier.pair(WRITER,"Writer",writer);carrier.pair(OTHER,"Other",other);carrier.pair(AWAY,"Away",away);
        carrier.heardCarries(away);
        String id=UUID.randomUUID().toString(),awayKey=Courier.hex(away.fingerprint()),pageKey=Courier.hex(page(id));
        byte[] first=Envelope.seal(page(id),7,1,"first".getBytes(),writer.keys.signing(),away.keys.agreement().getPublic());

        // Left at every try at sending it: one row, and its turn to be brought kept.
        carrier.hear(left(writer,carrier,away,id,7,Courier.NOTE,first));
        carrier.store.broughtAgain(carrier.store.carried(awayKey).get(0));
        for(int again=0;again<10;again++)carrier.hear(left(writer,carrier,away,id,7,Courier.NOTE,first));
        List<NoteStore.Carried> held=carrier.store.carried(awayKey);
        assertEquals(1,held.size());
        assertEquals("the same revision left again does not make it due at once",1,held.get(0).tries);
        assertArrayEquals(first,held.get(0).bytes);
        assertEquals(Courier.ALREADY,carrier.store.carry(Courier.hex(writer.fingerprint()),awayKey,pageKey,Courier.NOTE,7,first));
        assertEquals(Courier.ALREADY,carrier.store.carry(Courier.hex(writer.fingerprint()),awayKey,pageKey,Courier.NOTE,6,first));

        // The same revision from another device that has the note: already held.
        byte[] others=Envelope.seal(page(id),7,2,"others".getBytes(),other.keys.signing(),away.keys.agreement().getPublic());
        carrier.hear(left(other,carrier,away,id,7,Courier.NOTE,others));
        held=carrier.store.carried(awayKey);
        assertEquals(1,held.size());assertEquals(Courier.hex(writer.fingerprint()),held.get(0).sender);

        // A newer one, from either, replaces it: still one, fresh, and an older one after it changes nothing.
        byte[] newer=Envelope.seal(page(id),9,3,"newer".getBytes(),other.keys.signing(),away.keys.agreement().getPublic());
        carrier.hear(left(other,carrier,away,id,9,Courier.NOTE,newer));
        carrier.hear(left(writer,carrier,away,id,8,Courier.NOTE,first));
        held=carrier.store.carried(awayKey);
        assertEquals(1,held.size());assertEquals(9,held.get(0).revision);assertEquals(0,held.get(0).tries);
        assertArrayEquals(newer,held.get(0).bytes);

        // Another note is another copy.
        String second=UUID.randomUUID().toString();
        carrier.hear(left(writer,carrier,away,second,1,Courier.NOTE,first));
        assertEquals(2,carrier.store.carried(awayKey).size());

        // One receipt from the phone lets go of everything held for it about that note, up to its revision.
        carrier.hear(Envelope.seal(page(id),9,4,Receipt.wrap(Receipt.COLLECTED),away.keys.signing(),carrier.keys.agreement().getPublic()));
        held=carrier.store.carried(awayKey);
        assertEquals(1,held.size());assertEquals(Courier.hex(page(second)),held.get(0).page);
    }

    @Test public void answersAreHeldOncePerDeviceThatLeftThem() throws Exception {
        Device writer=new Device("writer"),other=new Device("other"),carrier=new Device("carrier"),away=new Device("away");
        carrier.pair(WRITER,"Writer",writer);carrier.pair(OTHER,"Other",other);carrier.pair(AWAY,"Away",away);
        carrier.heardCarries(away);
        String id=UUID.randomUUID().toString(),awayKey=Courier.hex(away.fingerprint());
        byte[] answer=Envelope.seal(page(id),3,1,Receipt.wrap(Receipt.HAVE),writer.keys.signing(),away.keys.agreement().getPublic());
        // Each says a different device has the note: both kept, and each once however often it is left.
        for(int again=0;again<5;again++) {
            carrier.hear(left(writer,carrier,away,id,3,Courier.ANSWER,answer));
            carrier.hear(left(other,carrier,away,id,3,Courier.ANSWER,answer));
        }
        assertEquals(2,carrier.store.carried(awayKey).size());
        carrier.hear(Envelope.seal(page(id),3,2,Receipt.wrap(Receipt.COLLECTED_ANSWER),away.keys.signing(),carrier.keys.agreement().getPublic()));
        assertTrue("one receipt lets go of both",carrier.store.carried(awayKey).isEmpty());
    }

    @Test public void aRevisionBroughtAgainAndAgainIsSaidCollectedOnce() {
        // As Post says COLLECTED to a carrier, and leaves a copy with the carriers: by carrier, thing and kind.
        Courier.Lately said=new Courier.Lately(Courier.COLLECTED_AGAIN);
        long at=1_700_000_000_000L;String key="carrier page 1";
        int receipts=0;
        for(int copy=0;copy<120;copy++)if(said.take(key,2259,at+copy*1000))receipts++;
        assertEquals("120 copies of revision 2259 within two minutes: one receipt",1,receipts);
        assertFalse("an older revision is covered by it",said.take(key,2258,at+5000));
        assertTrue("a newer one is said",said.take(key,2260,at+6000));
        assertFalse(said.take(key,2260,at+7000));
        assertTrue("another carrier is told too",said.take("another page 1",2260,at+7000));
        // One that did not go is said again by the next copy.
        said.giveBack(key,2260);
        assertTrue(said.take(key,2260,at+8000));
        // A receipt lost on the way is said again once the while is out, when the carrier brings it again.
        assertFalse(said.take(key,2260,at+8000+Courier.COLLECTED_AGAIN-1));
        assertTrue(said.take(key,2260,at+8000+Courier.COLLECTED_AGAIN));
        assertTrue("a clock put back says it",said.take(key,2260,at));
        // Giving back an older revision than the one kept now leaves the newer one said.
        said.giveBack(key,2259);
        assertFalse(said.take(key,2260,at+1));
    }

    @Test public void oneReceiptWaitsForEachCarrierAndThingTheNewest() {
        // As Post queues COLLECTED for a carrier: copies arriving faster than receipts go queue one, not one each.
        Courier.Queued queued=new Courier.Queued();
        int tasks=0;
        for(int copy=0;copy<100;copy++)if(queued.queue("carrier page 1",598+copy%3))tasks++;
        assertTrue(queued.queue("another page 1",598));tasks++;
        assertEquals("one waiting for each carrier and thing",2,tasks);
        assertEquals(2,queued.size());
        assertEquals("it goes for the newest revision brought meanwhile",600,queued.run("carrier page 1",598));
        assertEquals("nothing queued: what it was asked for",598,queued.run("carrier page 1",598));
        assertTrue("once it has run, the next copy queues again",queued.queue("carrier page 1",601));
        assertEquals(601,queued.run("carrier page 1",0));
        assertEquals(598,queued.run("another page 1",0));
        assertEquals(0,queued.size());
    }

    @Test public void aCopyIsLeftOnceForTheSameRevision() {
        Courier.Lately left=new Courier.Lately(Courier.LEAVE_AGAIN);
        long at=1_700_000_000_000L;String key="away page 1";
        int copies=0;
        // Sent again every half minute for an hour to a device that is away.
        for(int round=0;round<120;round++)if(left.take(key,598,at+round*30_000L))copies++;
        assertEquals(1,copies);
        assertTrue("a new revision is left",left.take(key,599,at+60_000L));
    }

    @Test public void aDeviceThatNeverSaysItBackIsToldLessAndLess() {
        assertEquals(Said.TELL_AGAIN,Said.unanswered(0));
        assertEquals(2*Said.TELL_AGAIN,Said.unanswered(1));
        assertEquals(4*Said.TELL_AGAIN,Said.unanswered(2));
        assertEquals(8*Said.TELL_AGAIN,Said.unanswered(3));
        assertEquals("two hours at most",2L*60*60*1000,Said.unanswered(4));
        assertEquals(2L*60*60*1000,Said.unanswered(400));
        assertEquals(Said.TELL_AGAIN,Said.unanswered(-1));
        // A PC left off for ten hours: told each of these about 9 times, not 60.
        long spent=0;int told=0;
        while(spent<10L*60*60*1000){spent+=Said.unanswered(told);told++;}
        assertTrue("told "+told+" times",told<=10);
    }
}
