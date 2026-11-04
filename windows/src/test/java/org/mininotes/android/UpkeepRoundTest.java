// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * What one upkeep round costs where nothing changed (decision 261). On the Graphene on 0.4.030 the upkeep's thread used about
 * 600 s of a core in 2 h 40 min with few sends, ten times what decision 225 measured. In a synthetic notebook of a usual size,
 * with no node: 500 notes in 10 folders, 100 files, 50 stamps, 10 things shared with somebody else, 5 paired devices, 2 of
 * them mine (one given everything, one nothing yet). Each part a round works out is timed; the parts about stamps, worked
 * out again every round from the whole notebook, are kept from the last round while nothing they read changed.
 */
public class UpkeepRoundTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;private Context context;private Keys keys;
    @After public void close(){if(store!=null)store.close();}

    private static final String MINE_ALL="MxUpkeepMineAllFixture@127.0.0.1:9831",MINE_SOME="MxUpkeepMineSomeFixture@127.0.0.1:9832";
    private static final String[] OTHERS={"MxUpkeepAnaFixture@127.0.0.1:9833","MxUpkeepBenFixture@127.0.0.1:9834","MxUpkeepCleoFixture@127.0.0.1:9835"};
    private final List<String> notes=new ArrayList<>(),files=new ArrayList<>(),loose=new ArrayList<>();

    @Before public void notebook() throws Exception {
        Hashes.setSha3(Sha3::of);
        context=new Context(temp.newFolder("upkeep"));store=new NoteStore(context);store.getWritableDatabase();
        keys=new Keys(context);
        store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
        store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName="Upkeep PC";store.myAddress="MxUpkeepPcFixture@127.0.0.1:9830";
        List<String> folders=new ArrayList<>();
        for(int f=0;f<10;f++)folders.add(store.addCollection("Synthetic folder "+f).id);
        StringBuilder words=new StringBuilder();for(int w=0;w<60;w++)words.append("synthetic words of a usual note ");
        for(int at=0;at<500;at++) {
            NoteStore.Note n=new NoteStore.Note();n.book=at%5==0?Things.HOME:folders.get(at%10);n.title="Synthetic note "+at;n.body=words+" "+at;n.revision=1+at%7;
            store.save(n);notes.add(n.id);
        }
        for(int at=0;at<100;at++) {
            String id=UUID.randomUUID().toString();
            boolean attached=at<70;
            store.keep(new NoteStore.Held(id,attached?notes.get(at*3):folders.get(at%10),"synthetic-"+at+".pdf","application/pdf",1000+at,
                1_760_000_000_000L+at,attached?NoteStore.Branch.Kind.PAGE:NoteStore.Branch.Kind.COLLECTION));
            store.published(id,"manifest-synthetic-"+at);
            files.add(id);if(!attached)loose.add(id);
        }
        // Five devices: two of mine, three of other people.
        List<Keys> theirs=new ArrayList<>();
        String[] all={MINE_ALL,MINE_SOME,OTHERS[0],OTHERS[1],OTHERS[2]};
        for(int at=0;at<all.length;at++) {
            Keys k=new Keys(new Context(temp.newFolder("device"+at)));theirs.add(k);
            store.pairedWith(all[at],"Synthetic device "+at,at<2,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
            if(at<2)store.setMine(all[at],true);
        }
        store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,MINE_ALL,true));
        // Each has said what its build reads, and the two of mine count this one as theirs.
        org.mininotes.desktop.platform.content.SharedPreferences post=context.getSharedPreferences("post",Context.MODE_PRIVATE);
        Set<String> heard=new HashSet<>(),answers=new HashSet<>(),mine=new HashSet<>();
        for(int at=0;at<all.length;at++) {
            String who=Courier.hex(Envelope.fingerprint(theirs.get(at).signing().getPublic()));
            heard.add(who);answers.add(Base64.getEncoder().encodeToString(theirs.get(at).signing().getPublic().getEncoded()));
            if(at<2)mine.add(who);
        }
        post.edit().putStringSet("stamps",heard).putStringSet("trees",heard).putStringSet("loose",heard).putStringSet("files",heard)
            .putStringSet("answers",answers).putStringSet("mineThere",mine).putStringSet("persons",heard).apply();
        // Ten things shared with somebody else: six notes, four files on their own; and one note with the device of mine given nothing yet.
        for(int at=0;at<6;at++)store.give(Sharing.Scope.PAGE,notes.get(at*3),OTHERS[at%3],Sharing.Level.READ,"Synthetic device");
        for(int at=0;at<4;at++)store.give(Sharing.Scope.FILE,loose.get(at),OTHERS[at%3],Sharing.Level.READ,"Synthetic device");
        store.give(Sharing.Scope.PAGE,notes.get(0),MINE_SOME,Sharing.Level.WRITE,"Synthetic device 1");
        // Fifty stamps: of shared notes and files, of other notes and files.
        for(int at=0;at<50;at++) {
            if(at%2==0){String id=notes.get(at%20==0?0:at*3%500);NoteStore.Note n=store.get(id);
                store.stamped(id,Integritas.TITLED,n.title,String.format("%064x",at),1_760_000_000_000L+at,"req-"+at,"","0xFAKE",n.title,n.body);}
            else{String id=at%5==1?loose.get(at%4):files.get(at%100);
                store.stamped(id,Integritas.FILE,"synthetic.pdf",String.format("%064x",at),1_760_000_000_000L+at,"req-"+at,"","0xFAKE",null,null);}
        }
        // Everything owed for its stamps once went, so a round finds nothing to send.
        for(Post.StampsOwed one:Post.stampsOwed(context,store))
            Post.stampsWent(context,one.address(),one.thing(),one.sleeve()?store.sleeve(one.thing(),"").stamps
                :store.stampsTravelling(Post.stampedWith(one.thing(),store.enclosed(one.thing()))));
        // And every folder and file on its own went and was answered, as in a notebook that has run a while.
        for(Outbox.Wait w:store.cartonsOwed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,false))
            store.collectionAcknowledged(w.address,Things.envelopeId(w.page),w.revision,true);
        for(Outbox.Wait w:store.sleevesOwed(null,false))store.fileAcknowledged(w.address,w.page,w.revision,true);
    }

    /** The average time of one call, in microseconds, over enough calls to be read. */
    private static long micros(int times,ThrowingRunnable work) throws Exception {
        for(int at=0;at<times;at++)work.run();
        long start=System.nanoTime();
        for(int at=0;at<times;at++)work.run();
        return (System.nanoTime()-start)/1000/times;
    }
    private interface ThrowingRunnable{void run() throws Exception;}

    @Test public void aRoundWithNothingNewWorksOutNothingAgain() throws Exception {
        assertTrue("everything owed went",Post.stampsOwed(context,store).isEmpty());
        assertEquals(2,Post.stampsGoTo(context,store).size());
        Map<String,Long> took=new LinkedHashMap<>();
        took.put("addresses",micros(50,()->store.addresses()));
        took.put("changes",micros(50,()->store.changes()));
        took.put("fingerprints",micros(50,()->{for(NoteStore.Contact one:store.addresses())Post.fingerprint(one);}));
        took.put("stampsOwed worked out",micros(20,()->{Post.forgetKept();Post.stampsOwed(context,store);}));
        took.put("stampCards worked out",micros(20,()->{Post.forgetKept();Post.stampCards(context,store,Post.stampsGoTo(context,store));}));
        took.put("stampsOwed",micros(20,()->Post.stampsOwed(context,store)));
        took.put("stampCarriers",micros(20,()->store.stampCarriers()));
        took.put("stampsCard",micros(20,()->store.stampsCard()));
        took.put("stampCards",micros(20,()->Post.stampCards(context,store,Post.stampsGoTo(context,store))));
        took.put("outboxDue",micros(20,()->Outbox.due(store.handed(),store.sent(),store.revisions(),System.currentTimeMillis())));
        took.put("applyGroups",micros(20,()->store.applyGroups()));
        took.put("cartonsOwed",micros(20,()->store.cartonsOwed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,false)));
        took.put("sleevesOwed",micros(20,()->store.sleevesOwed(null,false)));
        System.out.println("upkeep round, microseconds each: "+took);
        // Robust, not tight: on any machine a round of the stamps' work with nothing changed is far below the 0.17 s a whole
        // round took on the Graphene (decision 225), and the second time it is kept, not worked out.
        assertTrue("stamps owed, kept while nothing changed: "+took.get("stampsOwed"),took.get("stampsOwed")<20_000);
        assertTrue("stamps cards, kept while nothing changed: "+took.get("stampCards"),took.get("stampCards")<20_000);
        assertTrue("kept is far cheaper than worked out: "+took,took.get("stampsOwed")*10<took.get("stampsOwed worked out"));
        // And the round's three readings of the whole notebook (decision 278): most of a round's 105 ms here before.
        assertTrue("revisions, kept while nothing changed: "+took,took.get("outboxDue")<20_000);
        assertTrue("cartons owed, kept while nothing changed: "+took,took.get("cartonsOwed")<5_000);
        assertTrue("files on their own owed, kept while nothing changed: "+took,took.get("sleevesOwed")<5_000);
    }

    /** What the round reads from the whole notebook is kept only while nothing is written, and kept apart from what it hands out. */
    @Test public void theRoundsReadingsFollowEveryWrite() throws Exception {
        String L=Sharing.EVERYTHING;NoteStore.Branch.Kind LIB=NoteStore.Branch.Kind.LIBRARY;
        assertTrue(store.cartonsOwed(LIB,L,false).isEmpty());
        assertTrue(store.sleevesOwed(null,false).isEmpty());
        // A new folder, with nothing in it yet: owed to the device of mine given everything.
        String folder=store.addCollection("Synthetic new folder").id;
        List<Outbox.Wait> owed=store.cartonsOwed(LIB,L,false);
        assertEquals(1,owed.size());assertEquals(folder,owed.get(0).page);assertEquals(MINE_ALL,owed.get(0).address);
        // Changing what was handed out changes nothing kept.
        owed.clear();
        assertEquals(1,store.cartonsOwed(LIB,L,false).size());
        // Answered: owed no more.
        store.collectionAcknowledged(MINE_ALL,Things.envelopeId(folder),store.revisionOf(folder),true);
        assertTrue(store.cartonsOwed(LIB,L,false).isEmpty());
        // A file on its own shared with somebody: owed to them; answered, owed no more.
        String file=UUID.randomUUID().toString();
        store.keep(new NoteStore.Held(file,folder,"synthetic-new.pdf","application/pdf",2000,1_760_000_300_000L,NoteStore.Branch.Kind.COLLECTION));
        store.published(file,"manifest-synthetic-new");
        store.give(Sharing.Scope.FILE,file,OTHERS[1],Sharing.Level.READ,"Synthetic device");
        List<Outbox.Wait> files=store.sleevesOwed(null,false);
        Set<String> to=new HashSet<>();for(Outbox.Wait one:files)if(one.page.equals(file))to.add(one.address);
        assertTrue("owed to whoever it is shared with: "+files,to.contains(OTHERS[1]));
        assertEquals("the same asked for that file alone",store.sleevesOwed(file,false).size(),files.size());
        for(Outbox.Wait one:files)store.fileAcknowledged(one.address,one.page,one.revision,true);
        assertTrue(store.sleevesOwed(null,false).isEmpty());
        // A note written: its revision is there at once.
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Synthetic newer note";n.body="words";n.revision=4;store.save(n);
        long written=store.get(n.id).revision;
        assertEquals(Long.valueOf(written),store.revisions().get(n.id));
        store.revisions().clear();
        assertEquals(Long.valueOf(written),store.revisions().get(n.id));
    }

    @Test public void whatIsKeptIsWorkedOutAgainOnceAnythingItReadsChanged() throws Exception {
        assertTrue(Post.stampsOwed(context,store).isEmpty());
        Map<String,Post.StampCardFor> before=Post.stampCards(context,store,Post.stampsGoTo(context,store));
        // A stamp made: owed again to whoever has the note, and the card to the owner's devices changes.
        NoteStore.Note n=store.get(notes.get(3));
        store.stamped(n.id,Integritas.TITLED,n.title,String.format("%064x",999),1_760_000_100_000L,"req-new","","0xFAKE",n.title,n.body);
        assertEquals(1,Post.stampsOwed(context,store).size());
        Map<String,Post.StampCardFor> after=Post.stampCards(context,store,Post.stampsGoTo(context,store));
        assertFalse(Arrays.equals(before.get(MINE_ALL).plain(),after.get(MINE_ALL).plain()));
        // The device given nothing yet has only what reaches it: given that note now, its card has the new stamp.
        int was=StampCard.open(after.get(MINE_SOME).plain()).size();
        store.give(Sharing.Scope.PAGE,n.id,MINE_SOME,Sharing.Level.READ,"Synthetic device 1");
        assertEquals(was+1,StampCard.open(Post.stampCards(context,store,Post.stampsGoTo(context,store)).get(MINE_SOME).plain()).size());
        // Shared with somebody new: owed to them too.
        store.give(Sharing.Scope.PAGE,notes.get(6*3+3),OTHERS[2],Sharing.Level.READ,"Synthetic device");
        NoteStore.Note m=store.get(notes.get(6*3+3));
        store.stamped(m.id,Integritas.TITLED,m.title,String.format("%064x",998),1_760_000_200_000L,"req-newer","","0xFAKE",m.title,m.body);
        assertEquals(2,Post.stampsOwed(context,store).size());
        // What is kept is what would be worked out.
        List<Post.StampsOwed> kept=Post.stampsOwed(context,store);
        Map<String,Post.StampCardFor> keptCards=Post.stampCards(context,store,Post.stampsGoTo(context,store));
        Post.forgetKept();
        assertEquals(kept,Post.stampsOwed(context,store));
        Map<String,Post.StampCardFor> workedOut=Post.stampCards(context,store,Post.stampsGoTo(context,store));
        assertEquals(keptCards.keySet(),workedOut.keySet());
        for(String at:workedOut.keySet())assertArrayEquals(keptCards.get(at).plain(),workedOut.get(at).plain());
        // Found on Minima: owed again.
        for(Post.StampsOwed one:Post.stampsOwed(context,store))
            Post.stampsWent(context,one.address(),one.thing(),store.stampsTravelling(Post.stampedWith(one.thing(),store.enclosed(one.thing()))));
        assertTrue(Post.stampsOwed(context,store).isEmpty());
        long row=-1;for(NoteStore.StampRow one:store.stamps())if(one.request().equals("req-new"))row=one.n();
        store.stampLanded(row,2374505,"0xTXPOW","0xTX","2026-10-25 09:00:00",3,System.currentTimeMillis());
        assertFalse(Post.stampsOwed(context,store).isEmpty());
    }
}
