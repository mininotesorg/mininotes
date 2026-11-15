package org.mininotes.android;

import static org.junit.Assert.*;

import com.eurobuddha.maxima.core.net.Frame;
import com.eurobuddha.maxima.core.net.PrivateStreams;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 * A big file straight between two devices (decision 327): the offer said and read back, as a message of its own kind; the file
 * fetched whole over a door on this machine, read a piece at a time from where it is; a piece changed on the way found; Refuse
 * heard by the sender. Made-up bytes only.
 */
public class BigTest {
    private static byte[] made(int n){byte[] b=new byte[n];new Random(7).nextBytes(b);return b;}

    @Test public void theOfferIsSaidAndReadBackAndAMessageCarriesItWhole() {
        Big.Offer offer=Big.Offer.of("synthetic film.mp4","video/mp4",1_500_000_000L);
        Big.Offer back=Big.Offer.read(offer.said());
        assertNotNull(back);
        assertEquals(offer.token,back.token);assertArrayEquals(offer.key,back.key);
        assertEquals("synthetic film.mp4",back.name);assertEquals("video/mp4",back.kind);assertEquals(1_500_000_000L,back.bytes);
        assertNull("bigger than 2 GB is no offer",Big.Offer.read(offer.said().replace("1500000000","3000000000")));
        assertNull(Big.Offer.read("not an offer"));
        byte[] id=Message.newId();
        Message.Card card=Message.open(Message.wrap(new Message.Card(id,Message.BIG,offer.said(),"",System.currentTimeMillis())));
        assertNotNull(card);assertEquals(Message.BIG,card.kind);
        assertEquals("the offer whole, never cut as words are",offer.said(),card.words);
        assertTrue(Big.goes(Enclosure.MOST+1));assertFalse(Big.goes(Enclosure.MOST));assertFalse(Big.goes(Big.MOST+1));
    }

    @Test public void theFileIsFetchedWholeFromTheDoorAndTheSenderHearsItSaved() throws Exception {
        byte[] file=made(3*Big.PIECE+12345);
        Big.Offer offer=Big.Offer.of("synthetic.bin","",file.length);
        AtomicBoolean saved=new AtomicBoolean();
        Big.serve(offer,at->{ByteArrayInputStream in=new ByteArrayInputStream(file);in.skip(at);return in;},new Big.Heard(){
            @Override public void saved(){saved.set(true);}
            @Override public void refused(){fail("not refused");}
        });
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            Thread answering=answer(door);
            ByteArrayOutputStream got=new ByteArrayOutputStream();long[] seen={0};
            Big.fetch(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer,got,here->seen[0]=here);
            answering.join(5000);
            assertArrayEquals(file,got.toByteArray());
            assertEquals(file.length,seen[0]);
            assertTrue("the sender heard it saved",saved.get());
            assertFalse("no longer offered once saved",Big.serving(offer.token));
        } finally{Big.withdraw(offer.token);}
    }

    @Test public void aPieceChangedOnTheWayIsFoundAndNothingPassesForTheFile() throws Exception {
        byte[] file=made(2*Big.PIECE+10);
        Big.Offer offer=Big.Offer.of("synthetic.bin","",file.length);
        ByteArrayOutputStream wire=new ByteArrayOutputStream();
        Big.send(offer,at->new ByteArrayInputStream(file),0,new DataOutputStream(wire));
        byte[] said=wire.toByteArray();
        said[said.length/2]^=1;
        try{Big.receive(offer,new DataInputStream(new ByteArrayInputStream(said)),new ByteArrayOutputStream(),null);fail("a changed piece passed");}
        catch(IOException found){/* as it should */}
        // Cut short: one piece missing at the end is found too.
        byte[] whole=wire.toByteArray();
        // The last piece is 4 + 10 + 16 bytes; taken away, the end said after the second.
        byte[] cut=java.util.Arrays.copyOf(whole,whole.length-30);
        java.nio.ByteBuffer.wrap(cut,cut.length-4,4).putInt(0);
        try{Big.receive(offer,new DataInputStream(new ByteArrayInputStream(cut)),new ByteArrayOutputStream(),null);fail("a short file passed");}
        catch(IOException found){/* as it should */}
    }

    @Test public void refusedTheSenderHearsItAndStopsOffering() throws Exception {
        Big.Offer offer=Big.Offer.of("synthetic.bin","",100);
        AtomicBoolean refused=new AtomicBoolean();
        Big.serve(offer,at->new ByteArrayInputStream(new byte[100]),new Big.Heard(){
            @Override public void saved(){fail("not saved");}
            @Override public void refused(){refused.set(true);}
        });
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            Thread answering=answer(door);
            Big.refuse(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer);
            answering.join(5000);
            assertTrue(refused.get());assertFalse(Big.serving(offer.token));
        } finally{Big.withdraw(offer.token);}
    }

    // ---- pushed (decision 330) ---------------------------------------------------------------------------------

    /** A file kept in memory, as a server's folder would keep it on disk: what is there, and written on from a place. */
    private static final class Kept implements Big.Sink {
        final ByteArrayOutputStream bytes=new ByteArrayOutputStream();boolean done;
        @Override public long have(){return bytes.size();}
        @Override public OutputStream from(long at) {
            byte[] before=java.util.Arrays.copyOf(bytes.toByteArray(),(int)at);bytes.reset();bytes.write(before,0,before.length);
            return new FilterOutputStream(bytes){@Override public void close(){/* kept */}};
        }
        @Override public void done(){done=true;}
    }
    private static Big.Source from(byte[] file){return at->{ByteArrayInputStream in=new ByteArrayInputStream(file);in.skip(at);return in;};}

    @Test public void aPushedOfferSaysSoAndABuildFromBeforeSeesNoOfferInIt() {
        Big.Offer offer=Big.Offer.pushing("mininotes-backup-Pro-20261105-204000.mnbackup","application/x-mininotes-backup",400_000_000L);
        assertTrue(offer.said().startsWith("2\n"));
        Big.Offer back=Big.Offer.read(offer.said());
        assertNotNull(back);assertTrue(back.pushed);assertEquals(offer.token,back.token);
        assertFalse(Big.Offer.read(Big.Offer.of("a.bin","",5).said()).pushed);
        // What 0.4.056 and before read: only a 1 first.
        assertFalse("read as nothing by a build from before",offer.said().split("\n")[0].equals("1"));
    }

    @Test public void aPushedFileIsTakenWholeAtTheReceiversDoor() throws Exception {
        byte[] file=made(3*Big.PIECE+777);
        Big.Offer offer=Big.Offer.pushing("synthetic.mnbackup","",file.length);
        Kept kept=new Kept();long[] seen={0};
        Big.take(Big.Offer.read(offer.said()),kept,here->seen[0]=here);
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            Thread answering=answer(door);
            Big.push(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer,from(file),5000);
            answering.join(5000);
            assertArrayEquals(file,kept.bytes.toByteArray());assertTrue(kept.done);assertEquals(file.length,seen[0]);
            assertFalse("no longer taken once whole",Big.taking(offer.token));
        } finally{Big.stopTaking(offer.token);}
    }

    @Test public void aPushThatBrokeOffGoesOnFromWhereItWasAndOneThatKnocksTooSoonTriesAgain() throws Exception {
        byte[] file=made(4*Big.PIECE+5);
        Big.Offer offer=Big.Offer.pushing("synthetic.mnbackup","",file.length);
        // Two pieces and a bit came before: it goes on from the second piece's end, the bit after it dropped.
        Kept kept=new Kept();kept.bytes.write(file,0,2*Big.PIECE+100);
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            Thread answering=answerMany(door,5);
            // Knocks before the offer is heard there: nobody answers its token, and it tries again.
            Thread later=new Thread(()->{try{Thread.sleep(2500);}catch(InterruptedException e){return;}Big.take(Big.Offer.read(offer.said()),kept,null);});
            later.start();
            Big.push(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer,at->{
                assertEquals("on from a whole piece",2L*Big.PIECE,at);return from(file).from(at);},15000);
            later.join(5000);answering.join(5000);
            assertArrayEquals(file,kept.bytes.toByteArray());assertTrue(kept.done);
        } finally{Big.stopTaking(offer.token);}
    }

    @Test public void nobodyTakingItThePushGivesUpInTime() throws Exception {
        Big.Offer offer=Big.Offer.pushing("synthetic.mnbackup","",10);
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            answerMany(door,3);
            long started=System.currentTimeMillis();
            try{Big.push(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer,from(new byte[10]),3000);fail("pushed to nobody");}
            catch(IOException none){/* as it should */}
            assertTrue(System.currentTimeMillis()-started<10_000);
        }
    }

    /** As {@link #answer}, for several knocks one after the other. */
    private static Thread answerMany(ServerSocket door,int knocks) {
        Thread t=new Thread(()->{
            for(int i=0;i<knocks;i++)try(Socket one=door.accept()) {
                DataInputStream in=new DataInputStream(new BufferedInputStream(one.getInputStream()));
                DataOutputStream out=new DataOutputStream(one.getOutputStream());
                byte[] frame=Frame.readOrSkip(in,1024);
                if(frame!=null&&Frame.typeOf(frame)==PrivateStreams.TYPE)PrivateStreams.serve(frame,one,in,out);
            } catch(Exception broken){/* the next */}
        });
        t.setDaemon(true);t.start();return t;
    }

    /** A door as the device's own hands a stream over: its first frame read, then the one it names served. */
    private static Thread answer(ServerSocket door) {
        Thread t=new Thread(()->{
            try(Socket one=door.accept()) {
                DataInputStream in=new DataInputStream(new BufferedInputStream(one.getInputStream()));
                DataOutputStream out=new DataOutputStream(one.getOutputStream());
                byte[] frame=Frame.readOrSkip(in,1024);
                if(frame!=null&&Frame.typeOf(frame)==PrivateStreams.TYPE)PrivateStreams.serve(frame,one,in,out);
            } catch(Exception broken){throw new RuntimeException(broken);}
        });
        t.start();return t;
    }

    /** Not reached: the way to send it first, tried again for a day (the owner, 2026-11-07: "present the solution"). */
    @Test public void notReachedSaysTheWayAndTriesAgainForADay() {
        assertTrue(Big.notReached(Big.NOT_NEAR));assertFalse(Big.notReached("Something else"));assertFalse(Big.notReached(null));
        String steps=Big.steps("Laptop");
        assertTrue(steps.contains("Open Mininotes on Laptop"));assertTrue(steps.contains("same Wi-Fi"));assertTrue(steps.contains("Wake Laptop"));
        assertFalse("no long dash",steps.contains("—"));
        assertTrue(Big.tryAgain(0,Big.AGAIN_FOR-1));assertFalse(Big.tryAgain(0,Big.AGAIN_FOR));
    }
}
