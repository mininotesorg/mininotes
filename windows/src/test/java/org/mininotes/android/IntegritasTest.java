package org.mininotes.android;

import static org.junit.Assert.*;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.*;

/**
 * Stamping through Integritas (decision 199): the fingerprint as Integritas wants it, only it and the key going out, and the
 * answers read in the shape its v2 documentation gives. Against a stand-in on this PC: nothing reaches Integritas.
 */
public class IntegritasTest {
    private HttpServer server;
    private final AtomicReference<String> key=new AtomicReference<>(),body=new AtomicReference<>(),path=new AtomicReference<>();

    @Before public void stand() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/core/v2",x->{
            key.set(x.getRequestHeaders().getFirst("x-api-key"));path.set(x.getRequestURI().getPath());
            body.set(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            String answer;int code=200;
            if("wrong-key-123".equals(key.get())){code=401;answer="{\"status\":\"error\",\"message\":\"Unauthorized\"}";}
            else if(path.get().endsWith("timestamp/post"))answer="{\"service\":\"CORE_API\",\"apiVersion\":2,\"status\":\"success\",\"statusCode\":200,"
                +"\"message\":\"Hash anchored successfully.\",\"data\":{\"service\":\"MINIMA_NODE\",\"command\":\"send\",\"status\":true}}";
            else answer="{\"status\":\"success\",\"statusCode\":200,\"message\":\"Verification complete - PDF uploaded\",\"data\":{\"service\":\"SEARCH_API\","
                +"\"data\":{\"result\":\"full match\",\"blockchainData\":[{\"blockNumber\":123456,\"blockDate\":\"2026-10-18T13:10:00.000Z\",\"txpowId\":\"0xAB\"}]}},"
                +"\"file\":{\"fileName\":\"report.pdf\",\"downloadUrl\":\"https://reports.example/report.pdf\",\"expiresIn\":\"1 hour\"}}";
            byte[] out=answer.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(code,out.length);try(OutputStream o=x.getResponseBody()){o.write(out);}
        });
        server.start();
        Integritas.bases=new String[]{"http://127.0.0.1:"+server.getAddress().getPort()+"/core/v2"};
    }
    @After public void down(){server.stop(0);Integritas.bases=Integritas.BASES;}

    @Test public void theFingerprintAloneGoesAndTheAnswersAreRead() throws Exception {
        // SHA-256 of "abc", the standard's own example.
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",Integritas.hashOfText("abc"));
        String hash=Integritas.hashOfText("Synthetic contract");
        Integritas.Stamped s=Integritas.stamp("synthetic-key-123",hash);
        assertEquals("synthetic-key-123",key.get());
        assertEquals("/core/v2/timestamp/post",path.get());
        assertEquals("the hash, and nothing else",hash,new JSONObject(body.get()).getString("hash"));
        assertEquals(1,new JSONObject(body.get()).length());
        assertEquals("Hash anchored successfully.",s.message());
        Integritas.Found f=Integritas.verify("synthetic-key-123",hash,"contract.pdf",18,true);
        assertEquals("/core/v2/verify/file",path.get());
        assertTrue(f.match());assertEquals(1,f.blocks().size());assertEquals(123456,f.blocks().get(0).number());
        assertEquals("https://reports.example/report.pdf",f.report());
        // Refused: said in words, nothing kept.
        try{Integritas.stamp("wrong-key-123",hash);fail("accepted a key Integritas refused");}
        catch(java.io.IOException refused){assertTrue(refused.getMessage(),refused.getMessage().contains("did not accept this API key"));}
        try{Integritas.stamp("",hash);fail("went with no key");}
        catch(java.io.IOException none){assertTrue(none.getMessage().startsWith("Link your Integritas account first"));}
    }

    @Test public void theNftOfAReportIsRead() throws Exception {
        // As verify/file answers with x-report-required (docs.integritas.technology, v2 response schema).
        org.json.JSONObject said=new org.json.JSONObject("{\"status\":\"success\",\"data\":{\"data\":{\"result\":\"full match\",\"nftCoinId\":\"0xB2E1\",\"nftTxnId\":\"0x00000370\","
            +"\"blockchainData\":[{\"blockNumber\":1906787,\"blockDate\":\"2026-01-21T09:55:48.726Z\",\"txpowId\":\"0x00000019\",\"transactionId\":\"0xA1\"}]}}}");
        Integritas.Found f=Integritas.found(said);
        assertTrue(f.match());assertEquals("0xB2E1",f.nft());assertEquals("0x00000370",f.nftTx());
        assertEquals("",Integritas.found(new org.json.JSONObject("{\"data\":{\"data\":{\"result\":\"no match\"}}}")).nft());
    }

    /** A server that says 503 to its first {@code busy} requests, then answers "no match". */
    private static HttpServer busyFor(int busy,java.util.concurrent.atomic.AtomicInteger asked) throws Exception {
        HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        s.createContext("/core/v2",x->{x.getRequestBody().readAllBytes();
            boolean down=asked.incrementAndGet()<=busy;
            byte[] out=(down?"":"{\"status\":\"success\",\"data\":{\"data\":{\"result\":\"no match\",\"blockchainData\":[]}}}").getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(down?503:200,out.length==0?-1:out.length);if(out.length>0)try(OutputStream o=x.getResponseBody()){o.write(out);}});
        s.start();return s;
    }

    @Test public void aQuickFailureIsTriedOnceMoreAfterAPause() throws Exception {
        java.util.concurrent.atomic.AtomicInteger asked=new java.util.concurrent.atomic.AtomicInteger();
        HttpServer busy=busyFor(2,asked);long was=Integritas.againAfter;Integritas.againAfter=50;
        String at="http://127.0.0.1:"+busy.getAddress().getPort()+"/core/v2";
        try {
            // Two names, one server, as today: both busy in the first round, the second round answered.
            Integritas.bases=new String[]{at,at};
            assertFalse(Integritas.verify("synthetic-key-123",Integritas.hashOfText("x"),"x.txt",1,false).match());
            assertEquals(3,asked.get());
            // Busy throughout: said unreachable after the two rounds, and no more.
            asked.set(-100);
            try{Integritas.verify("synthetic-key-123",Integritas.hashOfText("x"),"x.txt",1,false);fail("answered");}
            catch(Integritas.Unreachable away){assertEquals("Integritas could not be reached. Try again in a moment.",away.getMessage());}
            assertEquals(-96,asked.get());
        } finally{busy.stop(0);Integritas.againAfter=was;}
    }

    @Rule public org.junit.rules.TemporaryFolder temp=new org.junit.rules.TemporaryFolder();

    @Test public void theWatchSaysUnreachableOnlyAfterSeveralLooksOrWhenAsked() throws Exception {
        java.util.concurrent.atomic.AtomicInteger asked=new java.util.concurrent.atomic.AtomicInteger();
        HttpServer busy=busyFor(Integer.MAX_VALUE,asked);long was=Integritas.againAfter;Integritas.againAfter=1;
        Integritas.bases=new String[]{"http://127.0.0.1:"+busy.getAddress().getPort()+"/core/v2"};
        NoteStore store=new NoteStore(new org.mininotes.desktop.platform.content.Context(temp.newFolder("alone")));store.getWritableDatabase();
        Integritas.Watch watch=new Integritas.Watch(store,0);
        try {
            store.setSecret(Integritas.KEY,"synthetic-key-123");
            NoteStore.Note n=new NoteStore.Note();n.title="Contract";n.body="Synthetic terms";n.book=Things.HOME;store.save(n);
            long row=store.stamped(n.id,"note",n.heading(),Integritas.hashOfText(n.body),System.currentTimeMillis(),"r1","","",n.title,n.body);
            java.util.concurrent.LinkedBlockingQueue<String> said=new java.util.concurrent.LinkedBlockingQueue<>();
            watch.listen((r,l,trouble)->said.add(trouble==null?"-":trouble));
            for(int look=1;look<=Integritas.Watch.QUIET_LOOKS;look++) {
                if(look==1)watch.watch(row,0);
                String one=said.poll(20,java.util.concurrent.TimeUnit.SECONDS);
                assertNotNull("look "+look+" told",one);
                if(look<Integritas.Watch.QUIET_LOOKS)assertEquals("quiet at look "+look,"-",one);
                else assertTrue(one,one.startsWith("Integritas could not be reached"));
            }
            // Asked for (Verify again): said at once, even on a new count.
            Integritas.Watch fresh=new Integritas.Watch(store,0);
            try {
                java.util.concurrent.LinkedBlockingQueue<String> now=new java.util.concurrent.LinkedBlockingQueue<>();
                fresh.listen((r,l,trouble)->now.add(trouble==null?"-":trouble));
                fresh.now(row);
                String one=now.poll(20,java.util.concurrent.TimeUnit.SECONDS);
                assertNotNull(one);assertTrue(one,one.startsWith("Integritas could not be reached"));
            } finally{fresh.stop();}
        } finally{watch.stop();busy.stop(0);Integritas.againAfter=was;store.close();}
    }

    @Test public void itsAnswerIsSaidPlainlyAndTheFileWearsHowFarItIs() throws Exception {
        Say.use("fr");
        try {
            assertEquals("Integritas l'a pris en charge et transmis à Minima.",Integritas.answered("Hash anchored successfully."));
            assertTrue(Integritas.answered("Quota low").contains("Quota low"));
        } finally{Say.use("en");}
        assertEquals("",Integritas.answered(""));
        NoteStore store=new NoteStore(new org.mininotes.desktop.platform.content.Context(temp.newFolder("marks")));store.getWritableDatabase();
        try {
            assertEquals(NoteStore.Branch.STAMP_NONE,store.stampMark("f1"));
            long n=store.stamped("f1","file","a.pdf",Integritas.hashOfText("a"),System.currentTimeMillis(),"r","Hash anchored successfully.","","","");
            assertEquals(NoteStore.Branch.STAMP_SENT,store.stampMark("f1"));
            store.stampLanded(n,777001,"0xT","0xX",java.time.Instant.now().toString(),1,System.currentTimeMillis());
            assertEquals(NoteStore.Branch.STAMP_ON_MINIMA,store.stampMark("f1"));
        } finally{store.close();}
    }

    @Test public void aFingerprintPastedIsFoundAndCompared() throws Exception {
        String mine=Integritas.hashOfText("abc");
        assertEquals(mine,Integritas.fingerprintIn(mine));
        assertEquals(mine,Integritas.fingerprintIn("0x"+mine.toUpperCase(java.util.Locale.ROOT)));
        StringBuilder pairs=new StringBuilder();for(int i=0;i<mine.length();i+=2)pairs.append(i==0?"":":").append(mine,i,i+2);
        assertEquals("colons between pairs",mine,Integritas.fingerprintIn(pairs.toString()));
        assertEquals("words around it",mine,Integritas.fingerprintIn("SHA-256 of report.pdf: "+mine+" (sent Monday)"));
        assertNull("too short",Integritas.fingerprintIn(mine.substring(1)));
        assertNull("too long",Integritas.fingerprintIn(mine+"a"));
        assertTrue(Integritas.sameFingerprint(mine,"  "+mine.toUpperCase(java.util.Locale.ROOT)+"\n"));
        assertFalse(Integritas.sameFingerprint(mine,Integritas.hashOfText("abd")));
        assertNull(Integritas.sameFingerprint(mine,"not yet"));
    }

    @Test public void noMatchIsSaid() throws Exception {
        JSONObject none=new JSONObject("{\"status\":\"success\",\"data\":{\"data\":{\"result\":\"no match\",\"blockchainData\":[]}}}");
        assertFalse(Integritas.found(none).match());
    }
}
