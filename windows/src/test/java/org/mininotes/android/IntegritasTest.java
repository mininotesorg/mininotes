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

    @Test public void noMatchIsSaid() throws Exception {
        JSONObject none=new JSONObject("{\"status\":\"success\",\"data\":{\"data\":{\"result\":\"no match\",\"blockchainData\":[]}}}");
        assertFalse(Integritas.found(none).match());
    }
}
