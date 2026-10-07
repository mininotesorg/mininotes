package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import com.eurobuddha.maxima.core.media.MediaManifest;
import com.eurobuddha.maxima.core.media.MediaService;
import com.eurobuddha.maxima.core.store.BlobStore;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;
import org.mininotes.desktop.platform.database.Cursor;

/**
 * Something another person shares with me for the first time arrives and stays hidden until I answer (the owner, 2026-10-06:
 * "When someone shares something with me, I need an alert, a pop-up at first telling me, and it should go in Shared with me,
 * and there I should have an accept or refuse, and refuse could be silent or inform the sender with the option to send a
 * message back"; docs/HOME.md, decision 109). In the real notebook and through {@code Post.arrived}, with synthetic devices
 * and no network: each message is built as Post builds it, sealed, and handed to the device it is for. Ana is another person;
 * the tablet is one of mine.
 */
public class DesktopFirstShareTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String ANA="MxFirstShareAnaFixture@127.0.0.1:9811",PC="MxFirstSharePcFixture@127.0.0.1:9812",
        TABLET="MxFirstShareTabletFixture@127.0.0.1:9813";
    private static final NoteStore.Branch.Kind PAGE=NoteStore.Branch.Kind.PAGE,COLLECTION=NoteStore.Branch.Kind.COLLECTION,FILE=NoteStore.Branch.Kind.FILE;

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name,boolean mine) throws Exception {
            store.pairedWith(other.at,name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        /** A note as this device sends it now, with its path, not asking to be answered. */
        byte[] note(Device to,String id) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
                List.of(),1L,List.of(),store.steps(store.above(id)),"",null);
            return Envelope.seal(page(id),n.revision,1,Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        /** A note written here, on Home or in a folder, at its first revision. */
        String write(String in,String title,String body){
            NoteStore.Note n=new NoteStore.Note();n.book=in;n.title=title;n.body=body;n.revision=1;store.save(n);return n.id;
        }
        /** Written in again: its next revision. */
        void again(String id,String body){NoteStore.Note n=store.get(id);n.body=body;n.revision=n.revision+1;store.save(n);}
        /** Bytes this build seals for another device: a refusal, an answer. */
        byte[] said(Device to,String id,long when,byte[] inner) throws Exception {
            return Envelope.seal(page(id),when,System.currentTimeMillis(),inner,keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device ana,pc,tablet;
    @Before public void three() throws Exception {
        Hashes.setSha3(Sha3::of);
        ana=new Device("Ana's phone",ANA);pc=new Device("Test PC",PC);tablet=new Device("My tablet",TABLET);
        ana.pair(pc,"Test PC",false);pc.pair(ana,"Ana's phone",false);
        tablet.pair(pc,"Test PC",true);pc.pair(tablet,"My tablet",true);
    }

    /** A note on Ana's Home, shared with the PC to write in: Ana gives it, and the PC has her as one who may send it. */
    private String anasNote(String title) throws Exception {
        String id=ana.write(Things.HOME,title,"Synthetic "+title);
        ana.store.setLevel(Sharing.Scope.PAGE,id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,ANA,true));
        return id;
    }

    private static Set<String> ids(List<NoteStore.Branch> lines){Set<String> out=new HashSet<>();for(NoteStore.Branch one:lines)out.add(one.id);return out;}
    private static List<String> lines(List<NoteStore.Branch> lines){List<String> out=new ArrayList<>();for(NoteStore.Branch one:lines)out.add(one.id);return out;}

    /**
     * Every list that draws things, and every list of what goes to anybody, asked whether any of these shows in it. A hidden
     * thing must not leak through any of them (decision 109).
     */
    private void showsNowhere(NoteStore store,Set<String> hidden,String word) {
        Map<String,Collection<String>> lists=new LinkedHashMap<>();
        lists.put("Home",ids(store.contents(Things.HOME)));
        lists.put("Home's level",ids(store.inside(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING).holds));
        lists.put("Shared with me",ids(store.contents(NoteStore.SHARED)));
        lists.put("on its way to Shared with me",ids(store.coming(NoteStore.SHARED)));
        lists.put("on its way to Home",ids(store.coming(Things.HOME)));
        lists.put("the search at the foot of Home",ids(store.lookingEverywhere(word)));
        lists.put("the search",ids(store.looking(word)));
        lists.put("written lately",ids(store.lately(500)));
        lists.put("Recent",ids(store.recent(0)));
        lists.put("the tree",ids(store.wholeTree()));
        lists.put("the overview's Recent, as drawn",ids(store.asDrawn(store.recent(0))));
        lists.put("Favourites",ids(store.favourites()));
        lists.put("every favourite",ids(store.favouritesAll()));
        lists.put("the dock",ids(store.dock()));
        lists.put("past the dock",ids(store.favouritesBeyondDock()));
        lists.put("Temp",ids(store.temporary(System.currentTimeMillis())));
        lists.put("where a note can be moved",ids(store.places(true)));
        lists.put("where a folder can be moved",ids(store.places(false)));
        List<String> owed=new ArrayList<>();for(Outbox.Page one:store.pagesUnder(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING))owed.add(one.id);
        lists.put("what is owed anybody",owed);
        List<String> cartons=new ArrayList<>();for(Outbox.Wait one:store.cartonsOwed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,true))cartons.add(one.page);
        lists.put("folders owed anybody",cartons);
        List<String> sleeves=new ArrayList<>();for(Outbox.Wait one:store.sleevesOwed(null,true))sleeves.add(one.page);
        lists.put("files owed anybody",sleeves);
        for(Map.Entry<String,Collection<String>> list:lists.entrySet())
            for(String one:hidden)assertFalse(one+" leaks into "+list.getKey(),list.getValue().contains(one));
        // And the counts said on Home's places, and what a new page is written in, and what opens first.
        assertEquals("Temp's count",0,store.temporaryCount());
        assertEquals("the favourites' count",0,store.keptCount());
        assertEquals("new files on Home",0,store.freshOnHome());
        assertFalse("a new page is never written in it",hidden.contains(store.someBook()));
        NoteStore.Note first=store.latest();
        assertTrue("never what opens first",first==null||!hidden.contains(first.id));
        for(String one:hidden)if(store.get(one)!=null)assertFalse(one+" is not still there to open",store.stillThere(PAGE,one));
    }

    @Test public void aShareFromAnotherPersonArrivesKeptHiddenAndWaitingAndMoreOfItAsksNothingNew() throws Exception {
        String id=anasNote("Saturday market");
        Post.Landed landed=pc.hear(ana.note(pc,id));
        assertTrue("a question, not news",landed.waiting);
        assertEquals("Ana's phone shared “Saturday market” with you",landed.said);
        assertEquals("nothing opens from it","",landed.note);
        // Kept, so accepting is instant: the words are here.
        assertEquals("Synthetic Saturday market",pc.store.get(id).body);
        assertTrue(pc.store.waits(PAGE,id));
        List<NoteStore.Branch> waiting=pc.store.waitingForYou();
        assertEquals(List.of(id),lines(waiting));
        assertEquals("Saturday market",waiting.get(0).name);assertEquals("from Ana's phone · Can write",waiting.get(0).detail);
        assertEquals(ANA,waiting.get(0).origin);
        assertEquals(1,pc.store.waitingCount());
        showsNowhere(pc.store,Set.of(id),"Saturday");
        // The pop-up says it once: said, it is not said again, and it stays waiting.
        assertEquals(List.of(id),lines(pc.store.waitingUnsaid()));
        pc.store.waitingSaid(List.of(id));
        assertTrue(pc.store.waitingUnsaid().isEmpty());assertEquals(1,pc.store.waitingCount());
        // Written in again by Ana: taken in, still hidden, and not "updated a note".
        ana.again(id,"Synthetic Saturday market, with eggs");
        landed=pc.hear(ana.note(pc,id));
        assertTrue("said "+landed.said+" note "+landed.note+" waits "+pc.store.waits(PAGE,id),landed.waiting);assertNotEquals("Ana's phone updated a note.",landed.said);
        assertEquals("Synthetic Saturday market, with eggs",pc.store.get(id).body);
        assertTrue(pc.store.waitingUnsaid().isEmpty());
        showsNowhere(pc.store,Set.of(id),"eggs");
        // Accepted: on Home, found, and waiting no more.
        pc.store.acceptShared(PAGE,id);
        assertTrue(ids(pc.store.contents(Things.HOME)).contains(id));
        assertTrue(ids(pc.store.lookingEverywhere("eggs")).contains(id));
        assertTrue(ids(pc.store.recent(0)).contains(id));
        assertTrue(pc.store.waitingForYou().isEmpty());assertEquals(0,pc.store.waitingCount());
        // And from now on it comes as anything of hers does.
        ana.again(id,"Synthetic Saturday market, with eggs and bread");
        landed=pc.hear(ana.note(pc,id));
        assertFalse(landed.waiting);assertEquals("Ana's phone updated a note.",landed.said);
    }

    @Test public void fromOneOfMyOwnDevicesItArrivesShownAndNothingAsks() throws Exception {
        String id=tablet.write(Things.HOME,"Packing","Synthetic packing");
        tablet.store.setLevel(Sharing.Scope.PAGE,id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,TABLET,true));
        Post.Landed landed=pc.hear(tablet.note(pc,id));
        assertFalse(landed.waiting);
        assertFalse(pc.store.waits(PAGE,id));
        assertTrue(ids(pc.store.contents(Things.HOME)).contains(id));
        assertTrue(pc.store.waitingForYou().isEmpty());
        // And what waits here from Ana, once one of mine sends it (it was accepted there), is accepted here too.
        String hers=anasNote("Lunch");
        pc.hear(ana.note(pc,hers));
        assertTrue(pc.store.waits(PAGE,hers));
        NoteStore.Note n=pc.store.get(hers);
        Parcel.Sent passed=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",hers,false,-1L,false,
            List.of(),1L,List.of(),List.of(),"",null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,hers,TABLET,true));
        pc.hear(Envelope.seal(page(hers),n.revision+1,1,Parcel.wrap(passed,Envelope.MAX_TEXT),tablet.keys.signing(),pc.keys.agreement().getPublic()));
        assertFalse("one of mine said yes to it",pc.store.waits(PAGE,hers));
        assertTrue(ids(pc.store.contents(Things.HOME)).contains(hers));
    }

    @Test public void everyListLeavesOutWhatWaitsAndAcceptingShowsItWhole() throws Exception {
        // Ana's folder Market, a note in it, a folder in that with a note of its own; and a note on her Home. All four reach the PC.
        String market=ana.store.addCollection("Market").id,stalls=ana.store.addBook(market,"Stalls").id;
        String bread=ana.write(market,"Bread","Synthetic sourdough"),cheese=ana.write(stalls,"Cheese","Synthetic sourdough cheese"),
            saturday=ana.write(Things.HOME,"Saturday","Synthetic sourdough list");
        ana.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,market,PC,true));
        ana.store.setLevel(Sharing.Scope.PAGE,saturday,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,ANA,true));
        for(String one:new String[]{bread,cheese,saturday})assertTrue(pc.hear(ana.note(pc,one)).waiting);
        String marketHere=pc.store.above(bread).get(0),stallsHere=pc.store.above(cheese).get(1);
        assertEquals("Market",pc.store.nameOf(marketHere,true));
        // Two things wait: the folder, which holds the rest, and the note. Nothing inside the folder waits by itself.
        assertEquals(Set.of(marketHere,saturday),ids(pc.store.waitingForYou()));
        assertTrue(pc.store.waits(COLLECTION,marketHere));assertFalse(pc.store.waits(PAGE,bread));
        assertArrayEquals(new Object[]{COLLECTION,marketHere},pc.store.waitsUnder(PAGE,cheese));
        // Made a favourite, put in the dock, temporary and opened lately, as no arrival makes them: still nowhere.
        long later=System.currentTimeMillis()+3_600_000;
        for(String table:new String[]{"notes","things"})
            pc.store.getWritableDatabase().execSQL("UPDATE "+table+" SET "+("notes".equals(table)?"pinned":"favourite")+"=1,dock=1,until=?,touched=? WHERE id IN (?,?,?,?,?)",
                new Object[]{later,System.currentTimeMillis(),marketHere,stallsHere,bread,cheese,saturday});
        // And a file shared with the PC on its own, here and waiting, beside another still on its way.
        String here=UUID.randomUUID().toString(),coming=UUID.randomUUID().toString();
        Files.write(pc.store.fileFor(here).toPath(),new byte[]{1,2,3});
        pc.store.keep(new NoteStore.Held(here,Things.HOME,"sourdough.png","image/png",3,System.currentTimeMillis(),COLLECTION));
        pc.store.getWritableDatabase().execSQL("UPDATE files SET theirs=1,origin=?,fresh=1,shown=?,accepted=0,starred=1,until=? WHERE id=?",new Object[]{ANA,NoteStore.SHARED,later,here});
        pc.store.getWritableDatabase().execSQL("INSERT INTO incoming(id,note,origin,name,kind,bytes,manifest,alone,accepted) VALUES(?,?,?,?,?,?,?,1,0)",
            new Object[]{coming,Things.HOME,ANA,"sourdough-2.png","image/png",3,"m"});
        Set<String> hidden=Set.of(marketHere,stallsHere,bread,cheese,saturday,here,NoteStore.COMING+coming,coming);
        showsNowhere(pc.store,hidden,"sourdough");
        assertEquals(4,pc.store.waitingCount());
        assertEquals(Set.of(marketHere,saturday,here,NoteStore.COMING+coming),ids(pc.store.waitingForYou()));
        // Accepted, one by one: the folder shows with all it holds; the note on Home; the files in Shared with me.
        pc.store.acceptShared(COLLECTION,marketHere);
        assertTrue(ids(pc.store.contents(Things.HOME)).contains(marketHere));
        assertEquals(Set.of(bread,stallsHere),ids(pc.store.contents(marketHere)));
        assertTrue(ids(pc.store.lookingEverywhere("sourdough")).containsAll(Set.of(bread,cheese)));
        assertFalse(ids(pc.store.lookingEverywhere("sourdough")).contains(saturday));
        pc.store.acceptShared(PAGE,saturday);
        assertTrue(ids(pc.store.contents(Things.HOME)).contains(saturday));
        pc.store.acceptShared(FILE,here);pc.store.acceptShared(NoteStore.Branch.Kind.WAITING,NoteStore.COMING+coming);
        assertTrue(ids(pc.store.contents(NoteStore.SHARED)).contains(here));
        assertTrue(ids(pc.store.coming(NoteStore.SHARED)).contains(NoteStore.COMING+coming));
        assertTrue(pc.store.waitingForYou().isEmpty());
        // Accepted, what arrived is this device's to pass on, as anything of theirs here is.
        List<String> owed=new ArrayList<>();for(Outbox.Page one:pc.store.pagesUnder(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING))owed.add(one.id);
        assertTrue(owed.containsAll(List.of(bread,cheese,saturday)));
    }

    @Test public void aNewNoteInsideAnAcceptedFolderArrivesShownAndAsksNothing() throws Exception {
        String market=ana.store.addCollection("Market").id;
        String bread=ana.write(market,"Bread","Synthetic bread");
        ana.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,market,PC,true));
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,ANA,true));
        assertTrue(pc.hear(ana.note(pc,bread)).waiting);
        String here=pc.store.above(bread).get(0);
        pc.store.acceptShared(COLLECTION,here);
        String eggs=ana.write(market,"Eggs","Synthetic eggs");
        Post.Landed landed=pc.hear(ana.note(pc,eggs));
        assertFalse(landed.waiting);assertEquals("Ana's phone shared a note with you.",landed.said);
        assertTrue(ids(pc.store.contents(here)).contains(eggs));
        assertTrue(pc.store.waitingForYou().isEmpty());
    }

    @Test public void aFileSharedOnItsOwnWaitsWhileItComesAndOnceItIsHere() throws Exception {
        File shelf=temp.newFolder("relay");
        String file=UUID.randomUUID().toString();byte[] bytes=new byte[2_000];new Random(5).nextBytes(bytes);
        Files.write(ana.store.fileFor(file).toPath(),bytes);
        ana.store.keep(new NoteStore.Held(file,Things.HOME,"stall-map.png","image/png",bytes.length,System.currentTimeMillis(),COLLECTION));
        ana.store.give(Sharing.Scope.FILE,file,PC,Sharing.Level.READ,"Test PC");
        ana.store.toPublish(System.currentTimeMillis());
        MediaManifest made=new MediaService(null,new BlobStore(shelf)).publish(bytes,"image/png");
        ana.store.published(file,made.encode());
        Post.Landed landed=pc.hear(Envelope.seal(page(file),ana.store.fileRevision(file),System.currentTimeMillis(),
            Sleeve.wrap(ana.store.sleeve(file,PC).quiet(),Envelope.MAX_TEXT),ana.keys.signing(),pc.keys.agreement().getPublic()));
        assertTrue(landed.waiting);assertEquals("Ana's phone shared “stall-map.png” with you",landed.said);
        // On its way: waiting, not drawn as coming.
        assertEquals(List.of(NoteStore.COMING+file),lines(pc.store.waitingForYou()));
        assertTrue(pc.store.coming(NoteStore.SHARED).isEmpty());
        // Fetched: here, and still waiting.
        for(NoteStore.Incoming one:pc.store.toFetch(System.currentTimeMillis()))
            assertTrue(pc.store.fileArrived(one,new MediaService(null,new BlobStore(shelf)).fetch(MediaManifest.decode(one.manifest))));
        assertNotNull(pc.store.file(file));
        assertEquals(List.of(file),lines(pc.store.waitingForYou()));
        assertEquals("from Ana's phone · Can read",pc.store.waitingForYou().get(0).detail);
        showsNowhere(pc.store,Set.of(file),"stall");
        // Accepted: where files shared with me go, Shared with me.
        pc.store.acceptShared(FILE,file);
        assertTrue(ids(pc.store.contents(NoteStore.SHARED)).contains(file));
    }

    @Test public void refusedQuietlyItIsDeletedNothingMoreOfItComesAndTheSenderHearsNothing() throws Exception {
        String id=anasNote("Saturday market");
        pc.hear(ana.note(pc,id));
        assertEquals("nobody is told, quietly",0,Post.refuse(pc.context,pc.store,pc.keys,PAGE,id,false,""));
        assertNull("deleted here",pc.store.get(id));
        assertTrue("its versions too",pc.store.versions(id).isEmpty());
        assertTrue(pc.store.waitingForYou().isEmpty());
        NoteStore.Refusal refused=pc.store.refusal(ANA,id,null);
        assertNotNull(refused);assertTrue(refused.quiet);assertFalse("not a leaving, which is said for a week",refused.gone);
        assertTrue("nor something stopped for now, which Resume undoes",pc.store.paused().isEmpty());
        assertTrue("and nothing is said again",pc.store.leavings().isEmpty());
        // Her next words do not bring it back.
        ana.again(id,"Synthetic Saturday market, again");
        Post.Landed landed=pc.hear(ana.note(pc,id));
        assertFalse(landed.waiting);assertNull(landed.said);
        assertNull(pc.store.get(id));assertTrue(pc.store.waitingForYou().isEmpty());
        // And Ana heard nothing: the PC is still on it as she gave it, and nobody refused anything of hers.
        boolean still=false;for(Sharing.Rule one:ana.store.sharesOn(Sharing.Scope.PAGE,id))if(one.address.equals(PC)&&one.level==Sharing.Level.WRITE)still=true;
        assertTrue(still);
        assertTrue(ana.store.refusalsOn(PAGE,id).isEmpty());
    }

    @Test public void refusedWithAFewWordsTheSenderIsTakenOffAndKeepsTheWords() throws Exception {
        String id=anasNote("Saturday market");
        pc.hear(ana.note(pc,id));
        // Told: deleted here at once, whether or not the message could go (there is no network here), and left as Unfollow leaves.
        Post.refuse(pc.context,pc.store,pc.keys,PAGE,id,true,"Thanks, I have one");
        assertNull(pc.store.get(id));
        NoteStore.Refusal refused=pc.store.refusal(ANA,id,null);
        assertTrue(refused.gone);assertFalse(refused.quiet);
        // What the PC says to Ana, as Post.refuse seals it for a build that reads a refusal.
        long when=System.currentTimeMillis();
        Post.Landed heard=ana.hear(pc.said(ana,id,when,Receipt.refusal(Receipt.left(Sharing.Scope.PAGE),"Thanks, I have one")));
        assertEquals("Test PC refused “Saturday market”\n“Thanks, I have one”",heard.said);
        List<NoteStore.RefusedBy> kept=ana.store.refusalsOn(PAGE,id);
        assertEquals(1,kept.size());assertEquals(PC,kept.get(0).address);assertEquals("Test PC",kept.get(0).who);
        assertEquals("Thanks, I have one",kept.get(0).words);assertEquals("Saturday market",kept.get(0).thing);
        // Taken off it, as a leaving takes somebody off: nothing more of it is owed to the PC.
        for(Sharing.Rule one:ana.store.sharesOn(Sharing.Scope.PAGE,id))assertNotEquals(PC,one.address);
        assertTrue(ana.store.owed(PAGE,id).isEmpty());
        // No words: the notice alone.
        String other=anasNote("Sunday");
        pc.hear(ana.note(pc,other));
        heard=ana.hear(pc.said(ana,other,System.currentTimeMillis()+50,Receipt.refusal(Receipt.LEFT_PAGE,"")));
        assertEquals("Test PC refused “Sunday”",heard.said);
        // A build from before is never sealed a refusal (see Post.knowsRefusals): it hears what Unfollow says, and is told so.
        String third=anasNote("Monday");
        heard=ana.hear(pc.said(ana,third,System.currentTimeMillis()+50,Receipt.wrap(Receipt.LEFT_PAGE)));
        assertEquals("Test PC unfollowed a note.",heard.said);
        assertTrue(ana.store.refusalsOn(PAGE,third).isEmpty());
    }

    @Test public void refusedAFolderItIsDeletedWithEverythingInIt() throws Exception {
        String market=ana.store.addCollection("Market").id;
        String bread=ana.write(market,"Bread","Synthetic bread");
        ana.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,market,PC,true));
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,ANA,true));
        pc.hear(ana.note(pc,bread));
        String here=pc.store.above(bread).get(0);
        assertEquals(bread,pc.store.namingNote(COLLECTION,here));
        assertTrue(pc.store.whoHasWaiting(COLLECTION,here).contains(ANA));
        Post.refuse(pc.context,pc.store,pc.keys,COLLECTION,here,false,"");
        assertNull(pc.store.get(bread));assertFalse(pc.store.collectionIds().contains(here));
        // More of it does not bring it back: a note out of it, or one new in it.
        ana.again(bread,"Synthetic bread, again");
        String eggs=ana.write(market,"Eggs","Synthetic eggs");
        assertNull(pc.hear(ana.note(pc,bread)).said);assertNull(pc.hear(ana.note(pc,eggs)).said);
        assertNull(pc.store.get(bread));assertNull(pc.store.get(eggs));assertTrue(pc.store.waitingForYou().isEmpty());
    }

    @Test public void theUpgradeCountsEverythingAlreadyHereAsAccepted() throws Exception {
        Context old=new Context(temp.newFolder("before-accepting"));
        try(java.sql.Connection db=java.sql.DriverManager.getConnection("jdbc:sqlite:"+old.getFilesDir().toPath().resolve("mininotes.db"));
            java.sql.Statement run=db.createStatement()) {
            for(String sql:SchemaMigrations.create().subList(0,2))run.execute(sql);
            for(String sql:SchemaMigrations.upgrade(1,45))run.execute(sql);
            // Somebody else's note and folder on Home, and a file they shared on their own, as an older build kept them.
            run.execute("INSERT INTO notes(id,title,body,notebook,pinned,deleted,updated,book,theirs,origin) VALUES('n-1','Groceries','Bread','',0,0,1,'"+Things.HOME+"',1,'"+ANA+"')");
            run.execute("INSERT INTO things(id,parent,kind,name,made,updated,theirs,origin) VALUES('t-1','"+Things.HOME+"','collection','Kitchen',1,1,1,'"+ANA+"')");
            run.execute("INSERT INTO files(id,note,name,kind,bytes,added,held,theirs,origin,shown) VALUES('f-1','"+Things.HOME+"','plan.png','image/png',3,1,'collection',1,'"+ANA+"','"+NoteStore.SHARED+"')");
            run.execute("PRAGMA user_version=45");
        }
        try(NoteStore upgraded=new NoteStore(old)) {
            try(Cursor row=upgraded.getReadableDatabase().rawQuery("PRAGMA user_version",null)){assertTrue(row.moveToFirst());assertEquals(SchemaMigrations.VERSION,row.getInt(0));}
            assertTrue("nothing asks",upgraded.waitingForYou().isEmpty());
            assertTrue(ids(upgraded.contents(Things.HOME)).containsAll(Set.of("n-1","t-1")));
            assertTrue(ids(upgraded.contents(NoteStore.SHARED)).contains("f-1"));
            for(String table:new String[]{"notes","things","files"})
                try(Cursor row=upgraded.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+" WHERE accepted<>1",null)){assertTrue(row.moveToFirst());assertEquals(table,0,row.getInt(0));}
        }
    }
}
