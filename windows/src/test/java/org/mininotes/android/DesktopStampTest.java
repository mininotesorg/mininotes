// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;
import com.sun.net.httpserver.HttpServer;
import java.awt.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/**
 * Stamping on Minima seen through (the owner, 2026-10-19: "we need a full communication box, showing everything ... there
 * should be a mention at the top of the note for all this, and as soon as there is a change, this should be notified; all
 * stamped versions of the notes should be easily retrievable"; decision 218). Against a stand-in Integritas on this PC that
 * answers as its v2 documentation says: timestamp/post sends, verify/file finds nothing until the fingerprint is "in a block".
 * Synthetic notes and keys only; nothing reaches Integritas.
 */
public class DesktopStampTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private HttpServer server;
    /** How many looks find nothing before the fingerprint is "in a block". */
    private final AtomicInteger pending=new AtomicInteger(2),looks=new AtomicInteger(),stamps=new AtomicInteger();
    private final Set<String> anchored=ConcurrentHashMap.newKeySet();
    private Desktop pad;

    @Before public void stand() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/core/v2",x->{
            String path=x.getRequestURI().getPath(),body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            String hash=new JSONObject(body).optString("hash","");String answer;
            if(path.endsWith("timestamp/post")){stamps.incrementAndGet();anchored.add(hash);
                answer="{\"service\":\"CORE_API\",\"apiVersion\":2,\"status\":\"success\",\"statusCode\":200,\"message\":\"Hash anchored successfully.\","
                    +"\"data\":{\"service\":\"MINIMA_NODE\",\"command\":\"send\",\"params\":{\"amount\":\"1e-22\",\"address\":\"0xADDRESS\",\"state\":{\"99\":\"0x"+hash+"\"}},\"status\":true}}";}
            else {
                looks.incrementAndGet();
                boolean there=anchored.contains(hash)&&pending.getAndDecrement()<=0;
                answer=there?"{\"status\":\"success\",\"statusCode\":200,\"message\":\"Verification complete\",\"data\":{\"data\":{\"result\":\"full match\",\"blockchainData\":[{\"blockNumber\":777001,"
                    +"\"blockDate\":\""+java.time.Instant.now()+"\",\"txpowId\":\"0xTXPOW\",\"transactionId\":\"0xTX\",\"matchedHash\":\""+hash+"\"}]}}"
                    // A report where one is asked for: its link on this stand-in, a synthetic PDF (decision 253).
                    +("true".equals(x.getRequestHeaders().getFirst("x-report-required"))?",\"file\":{\"fileName\":\"report.pdf\",\"downloadUrl\":\"http://127.0.0.1:"+server.getAddress().getPort()+"/report.pdf\",\"expiresIn\":\"1 hour\"}":"")+"}"
                    :"{\"status\":\"success\",\"statusCode\":200,\"data\":{\"data\":{\"result\":\"no match\",\"blockchainData\":[]}}}";
            }
            byte[] out=answer.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(200,out.length);try(OutputStream o=x.getResponseBody()){o.write(out);}
        });
        server.createContext("/report.pdf",x->{byte[] pdf="%PDF-1.4 synthetic Integritas report".getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(200,pdf.length);try(OutputStream o=x.getResponseBody()){o.write(pdf);}});
        server.start();
        Integritas.bases=new String[]{"http://127.0.0.1:"+server.getAddress().getPort()+"/core/v2"};
        DesktopStamp.unit=2;
    }
    @After public void down() throws Exception {
        if(pad!=null) {
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isDisplayable())w.dispose();});
            if(pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
        }
        server.stop(0);Integritas.bases=Integritas.BASES;DesktopStamp.unit=1000;
    }

    @Test public void theWatchLooksUntilMinimaHasItAndKeepsWhereItLanded() throws Exception {
        NoteStore store=new NoteStore(new org.mininotes.desktop.platform.content.Context(temp.newFolder("alone")));store.getWritableDatabase();
        try {
            store.setSecret(Integritas.KEY,"synthetic-key-123");
            NoteStore.Note n=new NoteStore.Note();n.title="Contract";n.body="Synthetic terms, first draft";n.book=Things.HOME;store.save(n);
            String hash=Integritas.hashOfText(n.body);
            Integritas.Stamped sent=Integritas.stamp("synthetic-key-123",hash);
            assertEquals("0xADDRESS",sent.address());assertTrue("nothing on Minima yet",sent.already().isEmpty());
            long row=store.stamped(n.id,"note",n.heading(),hash,sent.at(),sent.request(),sent.message(),sent.address(),n.title,n.body);
            NoteStore.StampRow waiting=store.stampRow(row);
            assertTrue(waiting.waiting());assertFalse(waiting.confirmed());assertTrue(waiting.kept());
            assertEquals("Stamping, waiting for Minima",Integritas.noteLine(waiting,false));
            assertEquals(1,store.stampsWaiting().size());
            Integritas.Watch watch=new Integritas.Watch(store,1);
            CountDownLatch landed=new CountDownLatch(1);List<NoteStore.StampRow> heard=new CopyOnWriteArrayList<>();
            watch.listen((r,l,trouble)->{heard.add(r);if(l)landed.countDown();});
            watch.watch(row,0);
            assertTrue("landed",landed.await(20,TimeUnit.SECONDS));
            NoteStore.StampRow done=store.stampRow(row);
            assertTrue(done.confirmed());assertEquals(777001,done.block());assertEquals("0xTXPOW",done.txpow());assertEquals("0xTX",done.tx());
            assertEquals("two looks found nothing, the third found it",3,done.tries());assertEquals(3,looks.get());
            assertEquals("each look told",3,heard.size());assertTrue(store.stampsWaiting().isEmpty());
            assertTrue(Integritas.noteLine(done,false),Integritas.noteLine(done,false).startsWith("Stamped on Minima, ")&&Integritas.noteLine(done,false).endsWith("block 777001"));
            String details=Integritas.details(done);
            for(String part:new String[]{hash,"Block: 777001","0xTXPOW","0xTX","0xADDRESS",sent.request()})assertTrue(part,details.contains(part));
            watch.stop();
            // The text changed: the line says so; changed back, it stands by the stamp again.
            String later=Integritas.hashOfText(n.body+" and a second line");
            NoteStore.StampRow standing=Integritas.standing(store.stampsOf(n.id),later);
            assertTrue(Integritas.noteLine(standing,!standing.hash().equals(later)).startsWith("Changed since it was stamped on "));
            assertEquals(row,Integritas.standing(store.stampsOf(n.id),hash).n());
            // Stamped again, changed: each version its own row, the older kept.
            NoteStore.Note now=store.get(n.id);now.body="Synthetic terms, second draft";now.revision++;store.save(now);
            long second=store.stamped(n.id,"note",n.heading(),Integritas.hashOfText(now.body),System.currentTimeMillis(),"r2","","",now.title,now.body);
            assertEquals(2,store.stampsOf(n.id).size());assertEquals(second,store.stampsOf(n.id).get(0).n());
            assertArrayEquals(new String[]{"Contract","Synthetic terms, first draft"},store.stampedWords(row));
            // The first version put back: what the note said is kept as a version first.
            assertTrue(store.putStampedBack(row));
            assertEquals("Synthetic terms, first draft",store.get(n.id).body);
            boolean keptSecond=false;for(NoteStore.Version v:store.versions(n.id))keptSecond|=v.body.equals("Synthetic terms, second draft");
            assertTrue("the text it replaced is a version",keptSecond);
            assertEquals("the oldest record stands where the fingerprint was there before",777001,
                Integritas.chosen(List.of(new Integritas.Block(777001,"2026-10-19T10:00:00Z","a",""),new Integritas.Block(700000,"2026-01-01T10:00:00Z","b","")),java.time.Instant.parse("2026-10-19T09:59:00Z").toEpochMilli()).number());
            assertEquals(700000,Integritas.chosen(List.of(new Integritas.Block(777001,"2026-10-19T10:00:00Z","a",""),new Integritas.Block(700000,"2026-01-01T10:00:00Z","b","")),java.time.Instant.parse("2026-12-01T00:00:00Z").toEpochMilli()).number());
        } finally {store.close();}
    }

    @Test public void aLookThatNeverFindsItStopsAndCanBeStartedAgain() throws Exception {
        NoteStore store=new NoteStore(new org.mininotes.desktop.platform.content.Context(temp.newFolder("never")));store.getWritableDatabase();
        try {
            store.setSecret(Integritas.KEY,"synthetic-key-123");pending.set(Integritas.MOST+5);
            String hash=Integritas.hashOfText("never in a block");anchored.add(hash);
            long row=store.stamped("file-1","file","scan.pdf",hash,System.currentTimeMillis(),"r","","",null,null);
            assertFalse("a file keeps no text",store.stampRow(row).kept());assertNull(store.stampedWords(row));
            Integritas.Watch watch=new Integritas.Watch(store,0);CountDownLatch stopped=new CountDownLatch(1);
            watch.listen((r,l,t)->{if(NoteStore.STAMP_UNSEEN.equals(r.state()))stopped.countDown();});
            watch.watch(row,0);
            assertTrue(stopped.await(30,TimeUnit.SECONDS));
            NoteStore.StampRow gave=store.stampRow(row);assertEquals(Integritas.MOST,gave.tries());assertFalse(gave.waiting());
            assertEquals(0,watch.nextLook(row));
            assertTrue(Integritas.noteLine(gave,false).contains("not found on Minima yet"));
            // Verify again: looked for once more, and on from there.
            pending.set(0);CountDownLatch landed=new CountDownLatch(1);watch.listen((r,l,t)->{if(l)landed.countDown();});
            watch.now(row);
            assertTrue(landed.await(20,TimeUnit.SECONDS));assertTrue(store.stampRow(row).confirmed());
            watch.stop();
        } finally {store.close();}
    }

    @Test public void stampingANoteOnThePcShowsEveryStepAndTheNoteSaysIt() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
        pad.store.setSecret(Integritas.KEY,"synthetic-key-123");pending.set(3);
        NoteStore.Note n=new NoteStore.Note();n.title="Lease";n.body="Synthetic lease, signed copy";n.book=Things.HOME;pad.store.save(n);
        SwingUtilities.invokeAndWait(()->pad.open(n.id));
        await(()->onEdt(()->n.id.equals(pad.noteShown())));
        assertFalse("not stamped: no line",onEdt(pad.stampLine::isVisible));
        // Stamp on Minima… opens the box: the fingerprint of exactly the saved text, and nothing sent before Stamp is pressed.
        SwingUtilities.invokeAndWait(()->DesktopStamp.note(pad,n.id));
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampGo")!=null));
        JDialog box=onEdt(()->dialog("stampBox"));
        // Its title and text (decision 230).
        assertEquals(Integritas.hashOfNote(n.title,n.body),onEdt(()->((javax.swing.text.JTextComponent)find(box,"stampHash")).getText()));
        assertEquals(Integritas.hashOfText("Lease\n\nSynthetic lease, signed copy"),Integritas.hashOfNote(n.title,n.body));
        assertEquals("nothing sent yet",0,stamps.get());
        java.awt.Point at=onEdt(box::getLocationOnScreen);Dimension size=onEdt(box::getSize);Rectangle frame=onEdt(()->pad.frame.getBounds());
        assertTrue("centred over the window",Math.abs(at.x+size.width/2-(frame.x+frame.width/2))<=2);
        SwingUtilities.invokeAndWait(()->((JButton)find(box,"stampGo")).doClick());
        // Sent: the request and the address shown; waiting for Minima, while the watch looks; then the block.
        await(()->onEdt(()->find(box,"stampRequest")!=null));
        assertEquals(1,stamps.get());
        assertTrue("stamped with its title",pad.store.stampsOf(n.id).get(0).titled());
        assertEquals(Integritas.hashOfNote(n.title,n.body),pad.store.stampsOf(n.id).get(0).hash());
        assertEquals("0xADDRESS",onEdt(()->((javax.swing.text.JTextComponent)find(box,"stampAddress")).getText()));
        await(()->onEdt(()->find(box,"stampBlock")!=null));
        assertEquals("777001",onEdt(()->((javax.swing.text.JTextComponent)find(box,"stampBlock")).getText()));
        assertEquals("0xTXPOW",onEdt(()->((javax.swing.text.JTextComponent)find(box,"stampTxpow")).getText()));
        assertEquals("0xTX",onEdt(()->((javax.swing.text.JTextComponent)find(box,"stampTx")).getText()));
        assertNotNull(onEdt(()->find(box,"stampVersion")));assertNotNull(onEdt(()->find(box,"stampReport")));
        shoot(box,"218-stamp-box");
        // Said in the middle of the window when it landed.
        await(()->DesktopStamp.said.stream().anyMatch(one->one.endsWith("block 777001")));
        SwingUtilities.invokeAndWait(box::dispose);
        // The note's line, under its title.
        await(()->onEdt(()->pad.stampLine.isVisible()&&pad.stampLine.words().endsWith("block 777001")));
        assertFalse(onEdt(pad.stampLine::changed));
        // Typed in: changed since, with Stamp this version….
        SwingUtilities.invokeAndWait(()->{pad.page.setCaretPosition(pad.page.getDocument().getLength());pad.page.replaceSelection(" and an unsigned line");});
        await(()->onEdt(()->pad.stampLine.changed()));
        assertTrue(onEdt(pad.stampLine::words).startsWith("Changed since it was stamped on "));
        assertTrue(onEdt(()->pad.stampLine.again.isVisible()));
        shoot(pad.frame,"218-note-changed");
        // The stamped version opened, read only, and put back: the note says it again, its line stands by the stamp again.
        long row=pad.store.stampsOf(n.id).get(0).n();
        SwingUtilities.invokeAndWait(()->DesktopStamp.version(pad,row));
        await(()->onEdt(()->dialog("stampedVersion")!=null));
        JDialog version=onEdt(()->dialog("stampedVersion"));
        JTextArea text=onEdt(()->(JTextArea)find(version,"stampedText"));
        assertEquals(n.body,onEdt(text::getText));assertFalse(onEdt(text::isEditable));
        SwingUtilities.invokeAndWait(()->((JButton)find(version,"stampPutBack")).doClick());
        await(()->n.body.equals(pad.store.get(n.id).body));
        await(()->onEdt(()->!pad.stampLine.changed()&&pad.stampLine.words().endsWith("block 777001")));
        boolean kept=false;for(NoteStore.Version v:pad.store.versions(n.id))kept|=v.body.endsWith("and an unsigned line");
        assertTrue("the text it replaced is a version",kept);
        // Renamed: the stamp covers the title too, so the note is changed (the owner, 2026-10-21: "Yes, absolutely"); named
        // back, it stands by its stamp again.
        SwingUtilities.invokeAndWait(()->pad.title.setText("Lease, renamed"));
        await(()->onEdt(()->pad.stampLine.changed()));
        assertTrue(onEdt(pad.stampLine::words).startsWith("Changed since it was stamped on "));
        SwingUtilities.invokeAndWait(()->pad.title.setText("Lease"));
        await(()->onEdt(()->!pad.stampLine.changed()&&pad.stampLine.words().endsWith("block 777001")));
        // Stamped on Minima… is a plain list: one line for each stamp, and its details a click further (decision 230).
        SwingUtilities.invokeAndWait(()->DesktopStamp.list(pad,pad.frame));
        long only=pad.store.stampsOf(n.id).get(0).n();
        await(()->onEdt(()->dialog("stampList")!=null&&find(dialog("stampList"),"stampCard"+only)!=null));
        JDialog list=onEdt(()->dialog("stampList"));
        JButton line=onEdt(()->(JButton)find(list,"stampCard"+only));
        assertTrue(onEdt(line::getText),onEdt(line::getText).startsWith("“Lease”   ·   Note   ·   ")&&onEdt(line::getText).contains("On Minima, block 777001"));
        assertNull("no fingerprint in the list",onEdt(()->find(list,"listHash")));assertNull("no buttons in a line",onEdt(()->find(list,"stampDetails")));
        assertNotNull(onEdt(()->find(list,"stampPlus")));
        assertEquals("Stamp something…",onEdt(()->((JButton)find(list,"stampPlus")).getToolTipText()));
        // Laid out as Home is (decision 231): no Done, Home's round + at the foot where Done was, below the list; the website a
        // link in its own words; and the stamps under their heading.
        assertNull("no Done",onEdt(()->buttonSaying(list,"Done")));
        assertNull("Know more is the website's own name now",onEdt(()->buttonSaying(list,"Know more  ›")));
        JButton plusAtFoot=onEdt(()->(JButton)find(list,"stampPlus"));
        assertTrue("the + is below the list",onEdt(()->plusAtFoot.getLocationOnScreen().y>line.getLocationOnScreen().y));
        assertEquals(new Dimension(52,52),onEdt(plusAtFoot::getPreferredSize));
        assertEquals("",onEdt(plusAtFoot::getText));
        JButton site=onEdt(()->(JButton)find(list,"stampSite"));
        assertEquals("integritas.technology",onEdt(site::getText));assertEquals(Integritas.SITE,onEdt(site::getToolTipText));
        assertEquals(Boolean.TRUE,onEdt(()->site.getClientProperty("link")));
        assertEquals(DesktopUi.ACCENT,onEdt(site::getForeground));
        assertTrue("the hand over it",onEdt(()->site.getCursor().getType()==Cursor.HAND_CURSOR));
        assertEquals("Stamped notes and files",onEdt(()->((JTextArea)find(list,"stampListHeading")).getText()));
        assertTrue("the heading is above the lines",onEdt(()->find(list,"stampListHeading").getLocationOnScreen().y<line.getLocationOnScreen().y));
        // Your Integritas account… opens My profile at Stamping, and the list stays open (decision 231).
        SwingUtilities.invokeAndWait(()->((JButton)find(list,"stampAccount")).doClick());
        await(()->onEdt(()->{for(Window w:Window.getWindows())if(w instanceof JDialog p&&p.isShowing()&&"My profile".equals(p.getTitle())
            &&p.getRootPane().getClientProperty("focus") instanceof JComponent field&&"integritasKey".equals(field.getName()))return true;return false;}));
        assertTrue("the list is still open",onEdt(list::isDisplayable));
        assertNull("no box of its own for the account",onEdt(()->{for(Window w:Window.getWindows())if(w instanceof JDialog b&&b.isShowing()&&"Your Integritas account".equals(b.getTitle()))return b;return null;}));
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog p&&"My profile".equals(p.getTitle()))p.dispose();});
        Set<String> plus=onEdt(()->{Set<String> all=new HashSet<>();for(Component c:DesktopStamp.plusMenu(pad,list).getComponents())if(c instanceof JMenuItem i)all.add(DesktopMenus.words(i));return all;});
        assertEquals(Set.of("A note or file in Mininotes…","A file on this PC…"),plus);
        shoot(list,"218-stamped-list");
        SwingUtilities.invokeAndWait(line::doClick);
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampBlock")!=null));
        assertEquals("777001",onEdt(()->((javax.swing.text.JTextComponent)find(dialog("stampBox"),"stampBlock")).getText()));
    }

    @Test public void theListShowsWhatMinimaSaysTheExplorerIsLinkedAndAFileWearsItsStamp() throws Exception {
        // Decision 224. The owner's list said "not found yet" beside a box saying block 2374505: an older stamp of the same text,
        // given up on (or copied in from before 0.4.010 as not seen), stood apart from the one the watch found.
        Path folder=temp.newFolder("listed").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
        NoteStore store=pad.store;store.setSecret(Integritas.KEY,"synthetic-key-123");
        NoteStore.Note n=new NoteStore.Note();n.title="Deed";n.body="Synthetic deed, page one";n.book=Things.HOME;store.save(n);
        String hash=Integritas.hashOfText(n.body);long now=System.currentTimeMillis();
        long old=store.stamped(n.id,"note","Deed",hash,now-3_600_000L,"r1","","",n.title,n.body);store.stampChecked(old,Integritas.MOST,now,NoteStore.STAMP_UNSEEN);
        assertFalse(store.stampRow(old).confirmed());
        String txpow="0x0000004C07F34532B6D48A4514A22CAAE5709B35CBB88EC280A195DCC3284F1D";
        long found=store.stamped(n.id,"note","Deed",hash,now,"r2","","0xADDRESS",n.title,n.body);store.stampLanded(found,2374505,txpow,"0xDECA2444",java.time.Instant.now().toString(),3,now);
        assertTrue("the older stamp of the same fingerprint is found too",store.stampRow(old).confirmed());assertEquals(2374505,store.stampRow(old).block());
        // A stamp of other words, not found yet: its first action is Verify on Minima.
        String other=Integritas.hashOfText("Synthetic deed, page two");
        long lost=store.stamped("note-2","note","Deed, page two",other,now,"r3","","",null,null);store.stampChecked(lost,Integritas.MOST,now,NoteStore.STAMP_UNSEEN);
        // The Minima Explorer's own forms (explorer.minima.global, read 2026-10-19).
        assertEquals("https://explorer.minima.global/search?q=2374505",Integritas.blockLink(2374505));
        assertEquals("https://explorer.minima.global/transactions/"+txpow,Integritas.txpowLink(txpow));
        assertNull("not an id the explorer shows",Integritas.txpowLink("0xTX"));assertNull(Integritas.blockLink(0));
        assertTrue(Integritas.details(store.stampRow(found)).contains("https://explorer.minima.global/transactions/"+txpow));
        // A file of this PC's drive, never put in Mininotes (decision 230): its fingerprint and name alone.
        Path drive=temp.newFile("receipt.pdf").toPath();Files.writeString(drive,"%PDF-1.4 synthetic receipt");
        long away=store.stamped("outside:"+UUID.randomUUID(),Integritas.OUTSIDE,"receipt.pdf",Integritas.hash(Files.readAllBytes(drive)),now,"r5","","",null,null);
        assertTrue(store.stampRow(away).outside());assertFalse(store.stampRow(away).note());
        SwingUtilities.invokeAndWait(()->DesktopStamp.list(pad,pad.frame));
        await(()->onEdt(()->dialog("stampList")!=null&&find(dialog("stampList"),"stampCard"+lost)!=null));
        JDialog list=onEdt(()->dialog("stampList"));
        // One line each, newest first: what, when, how far; no fingerprint, transaction or button in it.
        JButton oldLine=onEdt(()->(JButton)find(list,"stampCard"+old)),lostLine=onEdt(()->(JButton)find(list,"stampCard"+lost)),awayLine=onEdt(()->(JButton)find(list,"stampCard"+away));
        assertTrue(onEdt(oldLine::getText),onEdt(oldLine::getText).startsWith("“Deed”   ·   Note, text only   ·   ")&&onEdt(oldLine::getText).contains("On Minima, block 2374505"));
        assertTrue(onEdt(awayLine::getText),onEdt(awayLine::getText).startsWith("“receipt.pdf”   ·   File on this PC   ·   "));
        assertNull(onEdt(()->find(list,"listTxpow")));assertNull(onEdt(()->find(list,"stampVerify")));
        assertEquals(DesktopStamp.words().getSize2D(),onEdt(()->oldLine.getFont().getSize2D()),0.01f);
        // A click opens its box: there its verification, its block and its link to the Minima Explorer.
        SwingUtilities.invokeAndWait(oldLine::doClick);
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampAgain")!=null));
        assertEquals("Verify again",onEdt(()->((JButton)find(dialog("stampBox"),"stampAgain")).getText()));
        assertEquals("“Deed”   ·   Note, text only",onEdt(()->((javax.swing.text.JTextComponent)find(dialog("stampBox"),"stampName")).getText()));
        SwingUtilities.invokeAndWait(()->dialog("stampBox").dispose());
        SwingUtilities.invokeAndWait(()->((JButton)find(list,"stampCard"+found)).doClick());
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampTxpow")!=null));
        assertEquals("https://explorer.minima.global/transactions/"+txpow,onEdt(()->((JComponent)find(dialog("stampBox"),"stampTxpow")).getClientProperty("link")));
        SwingUtilities.invokeAndWait(()->dialog("stampBox").dispose());
        SwingUtilities.invokeAndWait(lostLine::doClick);
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampAgain")!=null));
        assertEquals("Verify on Minima",onEdt(()->((JButton)find(dialog("stampBox"),"stampAgain")).getText()));
        SwingUtilities.invokeAndWait(()->dialog("stampBox").dispose());
        // The file of the drive: Verify on Minima… asks for it again, and another file is said to be another before Minima is asked.
        Path edited=temp.newFile("receipt-edited.pdf").toPath();Files.writeString(edited,"%PDF-1.4 synthetic receipt, edited");
        java.util.function.BiFunction<Component,String,Path> was=DesktopStamp.pick;
        try {
            DesktopStamp.pick=(owner,title)->edited;
            SwingUtilities.invokeAndWait(awayLine::doClick);
            await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampAgain")!=null));
            assertEquals("Verify on Minima…",onEdt(()->((JButton)find(dialog("stampBox"),"stampAgain")).getText()));
            SwingUtilities.invokeAndWait(()->((JButton)find(dialog("stampBox"),"stampAgain")).doClick());
            await(()->DesktopStamp.said.stream().anyMatch(one->one.contains("is not the file stamped")));
            SwingUtilities.invokeAndWait(()->dialog("stampBox").dispose());
            // + then A file on this PC…: Windows' box, here a stand-in; its fingerprint in the stamping box, nothing sent yet.
            DesktopStamp.pick=(owner,title)->drive;int sent=stamps.get();
            SwingUtilities.invokeAndWait(()->DesktopStamp.outside(pad,list));
            await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampGo")!=null));
            assertEquals(Integritas.hash(Files.readAllBytes(drive)),onEdt(()->((javax.swing.text.JTextComponent)find(dialog("stampBox"),"stampHash")).getText()));
            assertEquals("“receipt.pdf”   ·   File on this PC",onEdt(()->((javax.swing.text.JTextComponent)find(dialog("stampBox"),"stampName")).getText()));
            assertEquals(sent,stamps.get());
            SwingUtilities.invokeAndWait(()->dialog("stampBox").dispose());
        } finally {DesktopStamp.pick=was;}
        // Found while the list is open: it says so at once.
        store.stampLanded(lost,2374600,"0x00000019299334C6E083683292728F7F1459FCF14A16623F3CE2A77BA343E57F","",java.time.Instant.now().toString(),1,now);
        DesktopStamp.watch(pad).told(other);
        await(()->onEdt(()->{JButton c=(JButton)find(dialog("stampList"),"stampCard"+lost);return c!=null&&c.getText().contains("block 2374600");}));
        shoot(onEdt(()->dialog("stampList")),"224-stamped-list");
        SwingUtilities.invokeAndWait(()->dialog("stampList").dispose());
        // A file stamped: a stamp on its card, and its menu starts with its stamp, opening its box.
        String file=java.util.UUID.randomUUID().toString();Files.writeString(store.fileFor(file).toPath(),"%PDF-1.4 synthetic scan");
        store.keep(new NoteStore.Held(file,n.id,"scan.pdf","application/pdf",Files.size(store.fileFor(file).toPath()),now));
        long stampedFile=store.stamped(file,"file","scan.pdf",Integritas.hash(Files.readAllBytes(store.fileFor(file).toPath())),now,"r4","","",null,null);
        store.stampLanded(stampedFile,2374700,txpow,"",java.time.Instant.now().toString(),1,now);
        SwingUtilities.invokeAndWait(()->pad.open(n.id));
        await(()->onEdt(()->n.id.equals(pad.noteShown())));
        SwingUtilities.invokeAndWait(()->{pad.fileCards.show(n.id);pad.fileCards.open(true);});
        await(()->onEdt(()->find(pad.fileCards,"stampMark")!=null));
        assertTrue(onEdt(()->((JComponent)find(pad.fileCards,"stampMark")).getToolTipText()).endsWith("block 2374700"));
        NoteStore.Held held=store.filesOf(NoteStore.Branch.Kind.PAGE,n.id).get(0);
        JPopupMenu menu=onEdt(()->pad.fileCards.menu(held));
        // One line about Minima, saying how far its stamp is (decision 253).
        await(()->onEdt(()->find(menu,"stampOnMinima")!=null&&((JMenuItem)find(menu,"stampOnMinima")).getText().startsWith("Stamped on Minima, ")));
        Set<String> lines=onEdt(()->{Set<String> all=new HashSet<>();for(Component c:menu.getComponents())if(c instanceof JMenuItem i)all.add(DesktopMenus.words(i));return all;});
        for(String one:List.of("Open with…","Copy","Save on my computer…","Send to a device…","Move to another note…","Rename…","Replace with another file…","Move to bin"))
            assertTrue(one,lines.contains(one));
        for(String gone:List.of("Verify on Minima…","Compare with a fingerprint…"))assertFalse(gone,lines.contains(gone));
        assertEquals(1,onEdt(()->{int many=0;for(Component c:menu.getComponents())if("stampOnMinima".equals(c.getName()))many++;return many;}).intValue());
        // This note's part of the ⋯ menu: its stamp, then Stamp and Verify on Minima.
        await(()->onEdt(()->pad.stampLine.standing()>0));
        // Its stamps are of its text alone, made before titles were included: compared by its text alone, it is unchanged.
        assertFalse("an old stamp still matches by its text",onEdt(pad.stampLine::changed));
        assertEquals(found,Integritas.matching(store.stampsOf(n.id),"Deed, renamed",n.body).n());
        assertNull(Integritas.matching(store.stampsOf(n.id),"Deed",n.body+" and more"));
        // Stamped on Minima… is the last line of Find, no longer under Create (decision 230).
        List<String> rows=onEdt(()->{List<String> all=new ArrayList<>();for(Component c:pad.mainMenu().getComponents())if(c instanceof JMenuItem i)all.add(DesktopMenus.words(i));return all;});
        int stamped=rows.indexOf("Stamped on Minima…");assertTrue(String.valueOf(rows),stamped>0);
        assertEquals(String.valueOf(rows),"Tree",rows.get(stamped-1));
        Set<String> mine=onEdt(()->{Set<String> all=new HashSet<>();for(Component c:pad.mainMenu().getComponents())if(c instanceof JMenuItem i)all.add(DesktopMenus.words(i));return all;});
        assertFalse(mine.contains("Verify on Minima…"));
        assertEquals("one line about Minima in This note",1,onEdt(()->{int many=0;for(Component c:pad.mainMenu().getComponents())if("stampOnMinima".equals(c.getName()))many++;return many;}).intValue());
    }

    // ---- helpers ----

    private JDialog dialog(String name){for(Window w:Window.getWindows())if(w.isShowing()&&w instanceof JDialog d&&name.equals(d.getName()))return d;return null;}
    private Component centred(){for(Component c:pad.frame.getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER))if(c instanceof JComponent j&&j.getClientProperty("centred")!=null)return c;
        for(Window w:Window.getWindows())if(w instanceof RootPaneContainer r)for(Component c:r.getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER))if(c instanceof JComponent j&&j.getClientProperty("centred")!=null)return c;return null;}
    private static Component find(Container root,String name) {
        if(root==null)return null;
        for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container k){Component f=find(k,name);if(f!=null)return f;}}
        return null;
    }
    @Test public void aFileIsComparedWithAFingerprintAndWearsItsStamp() throws Exception {
        // Decisions 249 and 250: the box answers as the fingerprint is typed; a stamped file wears it on Home.
        Path folder=temp.newFolder("compared").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
        byte[] bytes="Synthetic contract, signed copy".getBytes(StandardCharsets.UTF_8);String hash=Integritas.hash(bytes);
        NoteStore.Held file=pad.store.addFileExact(Things.HOME,"contract.txt","text/plain",new java.io.ByteArrayInputStream(bytes));
        long n=pad.store.stamped(file.id,"file","contract.txt",hash,System.currentTimeMillis(),"r","Hash anchored successfully.","","","");
        pad.store.stampLanded(n,777001,"0xT","0xX",java.time.Instant.now().toString(),1,System.currentTimeMillis());
        SwingUtilities.invokeAndWait(pad::goHome);await(()->!pad.onPage());SwingUtilities.invokeAndWait(pad::refresh);Thread.sleep(1200);shoot(pad.frame.getContentPane(),"250-home-stamped-file");
        SwingUtilities.invokeAndWait(()->DesktopStamp.compareFile(pad,file));
        await(()->onEdt(()->compareBox()!=null));
        JDialog box=onEdt(this::compareBox);
        JTextField theirs=(JTextField)named(box,"compareTheirs");javax.swing.text.JTextComponent answer=(javax.swing.text.JTextComponent)named(box,"compareAnswer");
        SwingUtilities.invokeAndWait(()->theirs.setText("0x"+hash.toUpperCase(java.util.Locale.ROOT)));
        assertTrue(answer.getText(),answer.getText().startsWith("The same"));shoot(box.getContentPane(),"250-compare-same");
        String other=Integritas.hashOfText("another");SwingUtilities.invokeAndWait(()->theirs.setText(other));
        assertTrue(answer.getText(),answer.getText().startsWith("Not the same"));shoot(box.getContentPane(),"250-compare-not-same");
        SwingUtilities.invokeAndWait(()->theirs.setText("abc"));
        assertTrue(answer.getText(),answer.getText().startsWith("Not a SHA-256"));
    }
    @Test public void oneWindowStampsLooksAndKeepsTheReport() throws Exception {
        // Decision 253: Stamp on Minima… opens one window; unstamped, it offers Stamp and Look for it on Minima; once on Minima,
        // the PDF report is fetched and attached to the note, as a file sealed like any other.
        Path folder=temp.newFolder("one-window").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
        NoteStore store=pad.store;store.setSecret(Integritas.KEY,"synthetic-key-123");
        NoteStore.Note n=new NoteStore.Note();n.title="Lease";n.body="Synthetic lease, signed";n.book=Things.HOME;store.save(n);
        SwingUtilities.invokeAndWait(()->DesktopStamp.thing(pad,n.id,true));
        await(()->onEdt(()->dialog("stampBox")!=null&&find(dialog("stampBox"),"stampGo")!=null&&find(dialog("stampBox"),"stampLook")!=null));
        shoot(onEdt(()->dialog("stampBox").getContentPane()),"253-one-window-unstamped");
        pending.set(0);
        SwingUtilities.invokeAndWait(()->((JButton)find(dialog("stampBox"),"stampGo")).doClick());
        await(()->{List<NoteStore.StampRow> r=store.stampsOf(n.id);return !r.isEmpty()&&r.get(0).confirmed();});
        await(()->onEdt(()->find(dialog("stampBox"),"stampAttach")!=null));
        shoot(onEdt(()->dialog("stampBox").getContentPane()),"253-one-window-stamped");
        int before=store.filesOf(NoteStore.Branch.Kind.PAGE,n.id).size();
        SwingUtilities.invokeAndWait(()->((JButton)find(dialog("stampBox"),"stampAttach")).doClick());
        await(()->store.filesOf(NoteStore.Branch.Kind.PAGE,n.id).size()==before+1);
        NoteStore.Held pdf=store.filesOf(NoteStore.Branch.Kind.PAGE,n.id).get(0);
        for(NoteStore.Held one:store.filesOf(NoteStore.Branch.Kind.PAGE,n.id))if(one.name.endsWith("Integritas report.pdf"))pdf=one;
        assertEquals("Lease, Integritas report.pdf",pdf.name);
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();DesktopFiles.copyOut(pad.context,store.fileFor(pdf.id).toPath(),out);
        assertTrue(out.toString(StandardCharsets.ISO_8859_1).startsWith("%PDF"));
        await(()->DesktopStamp.said.stream().anyMatch(one->one.contains("The report is attached to the note")));
    }

    private JDialog compareBox(){for(Window w:Window.getWindows())if(w.isShowing()&&w instanceof JDialog d&&"Compare with a fingerprint…".equals(d.getTitle()))return d;return null;}
    private static Component named(Container in,String name) {
        for(Component c:in.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name);if(f!=null)return f;}}
        return null;
    }

    /** A button by the words it shows, in the language the test runs in. */
    private static JButton buttonSaying(Container root,String words) {
        if(root==null)return null;
        for(Component c:root.getComponents()){if(c instanceof JButton b&&words.equals(b.getText()))return b;if(c instanceof Container k){JButton f=buttonSaying(k,words);if(f!=null)return f;}}
        return null;
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {Object[] out={null};Exception[] failed={null};SwingUtilities.invokeAndWait(()->{try{out[0]=read.call();}catch(Exception e){failed[0]=e;}});if(failed[0]!=null)throw failed[0];@SuppressWarnings("unchecked") T t=(T)out[0];return t;}
    private static void await(Callable<Boolean> ok) throws Exception {for(int i=0;i<300;i++){if(Boolean.TRUE.equals(ok.call()))return;Thread.sleep(50);}fail("did not happen in time");}
    private static void shoot(Component c,String name) {
        try{SwingUtilities.invokeAndWait(()->{try{Path dir=Path.of("build","verification","gallery");Files.createDirectories(dir);
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(Math.max(1,c.getWidth()),Math.max(1,c.getHeight()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            c.validate();Graphics2D g=image.createGraphics();c.paint(g);g.dispose();javax.imageio.ImageIO.write(image,"png",dir.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});}
        catch(Exception e){throw new RuntimeException(e);}
    }
}
