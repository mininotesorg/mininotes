// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
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

/**
 * A stamp on Minima travels with what is shared with another person (the owner, 2026-10-25: "yes carry its stamp
 * automatically when shared"; decision 251). The format: a sleeve's and a parcel's stamps after their last field, behind
 * "MNW1" and a length, bounded, read back whole; and a build from before reads the same sleeve or parcel as it always did,
 * since it reads the bytes in front and stops at a mark it does not know. And in two synthetic notebooks, with no node: a
 * note with a file, and a file on its own, stamped and shared with somebody else, reach them with their stamps; a stamp of
 * anything else is refused; a stamp made or found on Minima after sharing makes the thing owed again, and reaches them.
 */
public class StampsWithSharesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    // ---- the format ------------------------------------------------------------------------------------------------------

    private static StampCard.Row row(String id,String kind,String hash,long at,String request,String words) {
        return new StampCard.Row(id,kind,"Synthetic "+id,hash,at,request,"Stamp request received","0xFAKEADDRESS",NoteStore.STAMP_WAITING,
            0,"","","",words==null?null:"Synthetic title",words,"","");
    }

    private static Sleeve.Sent sleeve(String id) {
        List<Parcel.Member> members=List.of(new Parcel.Member("key-a","MxA@127.0.0.1:1","Ana",Sharing.Level.READ.ordinal(),5L));
        return new Sleeve.Sent(id,"plan.png","image/png",3,"manifest-synthetic",false,Sharing.Scope.FILE.name(),id,members,true,false,
            "MxOwner@127.0.0.1:2","owner-key",7L,8L,0L,"");
    }

    private static Parcel.Sent parcel(String body,List<Enclosure.Listed> files) {
        Parcel.Sent p=new Parcel.Sent("","","","","Synthetic title",body,false,new ArrayList<>(),"","",true,-1L,true,files,9L,
            new ArrayList<>(),new ArrayList<>(),"",null);
        p.colour=2;p.colourDecided=11L;p.tone=Tint.USUAL;p.toneDecided=12L;
        return p;
    }

    private static boolean startsWith(byte[] all,byte[] front) {
        if(all.length<front.length)return false;
        for(int at=0;at<front.length;at++)if(all[at]!=front[at])return false;
        return true;
    }

    /** The first time the bytes of "MNW1" stand at or after {@code from}. */
    private static int markAt(byte[] all,int from) {
        for(int at=from;at+4<=all.length;at++)if(all[at]=='M'&&all[at+1]=='N'&&all[at+2]=='W'&&all[at+3]=='1')return at;
        return -1;
    }

    @Test public void theMarkCollidesWithNothing() {
        assertEquals(0x4d4e5731,StampCard.WITH_MARK);
        for(byte[] magic:List.of(StampCard.MAGIC,Sleeve.MAGIC,Parcel.MAGIC,Carton.MAGIC,Envelope.MAGIC,Drop.MAGIC,Receipt.MAGIC,
                Sealed.MAGIC,Groups.MAGIC,KeyCard.MAGIC,Persons.MAGIC,AboutMe.MAGIC,Face.MAGIC,Called.MAGIC,Help.MAGIC,Message.MAGIC,
                Parlons.MAGIC,Vault.MAGIC,Shrink.MAGIC,Home.MAGIC,Courier.MAGIC,Direct.MAGIC))
            assertNotEquals(StampCard.WITH_MARK,java.nio.ByteBuffer.wrap(magic).getInt());
        for(int mark:new int[]{0x4d4e4831,0x4d4e5031,0x4d4e4931,0x4d4e5431,Parcel.LOOK_MARK})assertNotEquals(StampCard.WITH_MARK,mark);
    }

    @Test public void aSleeveCarriesItsStampsAndAnOldReaderReadsWhatItAlwaysRead() throws Exception {
        Sleeve.Sent bare=sleeve("file-1");
        byte[] before=Sleeve.wrap(bare,Envelope.MAX_TEXT);
        Sleeve.Sent stamped=sleeve("file-1");
        stamped.stamps=List.of(row("file-1","file","aa11",2000L,"req-2",null),row("file-1","file","bb22",1000L,"req-1",null));
        byte[] after=Sleeve.wrap(stamped,Envelope.MAX_TEXT);
        // The bytes a build from before reads are exactly those it read: the stamps only follow them.
        assertTrue("the same sleeve in front",startsWith(after,before));
        assertEquals("then the mark",before.length,markAt(after,before.length));
        Sleeve.Sent read=Sleeve.open(after);
        assertNotNull(read);
        assertEquals(2,read.stamps.size());
        assertEquals("req-2",read.stamps.get(0).request);assertEquals("aa11",read.stamps.get(0).hash);
        assertEquals("file-1",read.id);assertEquals("manifest-synthetic",read.manifest);assertEquals(8L,read.replaced);assertEquals("",read.linked);
        assertNull("none said, none read",Sleeve.open(before).stamps);
        // Every shipped reader stops after the last field it knows: what follows does not change what it reads.
        Sleeve.Sent old=Sleeve.open(Arrays.copyOf(after,before.length));
        assertEquals(read.id,old.id);assertEquals(read.members.size(),old.members.size());assertEquals(read.owner,old.owner);
        // A later mark not known here, or stamps cut short: the sleeve stands, without them.
        byte[] later=after.clone();later[before.length+3]='9';
        Sleeve.Sent unknown=Sleeve.open(later);
        assertNotNull(unknown);assertNull(unknown.stamps);assertEquals("file-1",unknown.id);
        Sleeve.Sent cut=Sleeve.open(Arrays.copyOf(after,after.length-3));
        assertNotNull(cut);assertNull(cut.stamps);
        // Copies keep them.
        assertEquals(2,stamped.quiet().stamps.size());assertEquals(2,stamped.unsaid().stamps.size());
    }

    @Test public void aParcelCarriesThemLastAndItsTailStopsCleanlyForABuildFromBefore() throws Exception {
        List<Enclosure.Listed> files=List.of(new Enclosure.Listed("file-1","scan.pdf","application/pdf",3,""));
        Parcel.Sent bare=parcel("Synthetic body",files);
        byte[] before=Parcel.wrap(bare,Envelope.MAX_TEXT);
        Parcel.Sent stamped=parcel("Synthetic body",files);
        stamped.stamps=List.of(row("note-1","titled","cc33",3000L,"req-n","Synthetic body"),row("file-1","file","aa11",2000L,"req-f",null));
        byte[] after=Parcel.wrap(stamped,Envelope.MAX_TEXT);
        assertTrue("the same parcel in front",startsWith(after,before));
        assertEquals("then the mark, after the look",before.length,markAt(after,before.length));
        Parcel.Sent read=Parcel.open(after);
        assertNotNull(read);
        assertEquals(2,read.stamps.size());
        assertEquals("Synthetic body",read.stamps.get(0).words);assertEquals("Synthetic title",read.stamps.get(0).title);
        assertEquals("the look still read",2,read.colour);assertEquals(11L,read.colourDecided);assertEquals(12L,read.toneDecided);
        assertEquals("Synthetic body",read.body);assertEquals(1,read.files.size());
        assertNull(Parcel.open(before).stamps);
        // A build from before meets "MNW1" as a mark it does not know: its tail stops there, everything above read. The
        // same as this build meeting a later mark.
        byte[] later=after.clone();later[before.length+3]='9';
        Parcel.Sent old=Parcel.open(later);
        assertNotNull(old);assertNull(old.stamps);assertEquals(2,old.colour);assertEquals("Synthetic body",old.body);
        Parcel.Sent cut=Parcel.open(Arrays.copyOf(after,after.length-5));
        assertNotNull("stamps cut short leave the note",cut);assertNull(cut.stamps);assertEquals(2,cut.colour);
        // No path, no stamps: a build from before would read them as something else.
        Parcel.Sent pathless=new Parcel.Sent("","","","","T","B",false,new ArrayList<>(),"","",true,-1L,true,files,9L,null,null,"",null);
        pathless.stamps=stamped.stamps;
        assertEquals(-1,markAt(Parcel.wrap(pathless,Envelope.MAX_TEXT),0));
    }

    @Test public void theyAreBoundedTheNewestGoWordsLeftOffFirst() throws Exception {
        List<StampCard.Row> many=new ArrayList<>();
        for(int at=0;at<40;at++)many.add(row("file-1","file",String.format("%04x",at),10_000L-at,"req-"+at,null));
        Sleeve.Sent s=sleeve("file-1");s.stamps=many;
        Sleeve.Sent read=Sleeve.open(Sleeve.wrap(s,Envelope.MAX_TEXT));
        assertEquals("at most "+StampCard.WITH_MOST,StampCard.WITH_MOST,read.stamps.size());
        assertEquals("the newest first","req-0",read.stamps.get(0).request);
        assertEquals("req-31",read.stamps.get(31).request);
        // A note nearly as big as an envelope: its stamps go without their words, and it still fits.
        char[] big=new char[120_000];Arrays.fill(big,'w');String body=new String(big);
        Parcel.Sent p=parcel(body,new ArrayList<>());
        p.stamps=List.of(row("note-1","titled","cc33",3000L,"req-new",body),row("note-1","titled","dd44",2000L,"req-old","short words"));
        byte[] wrapped=Parcel.wrap(p,Envelope.MAX_TEXT);
        assertTrue(wrapped.length<=Envelope.MAX_TEXT);
        Parcel.Sent bigRead=Parcel.open(wrapped);
        assertEquals(2,bigRead.stamps.size());
        assertNull("words that do not fit left off first",bigRead.stamps.get(0).words);
        assertEquals("words that fit kept","short words",bigRead.stamps.get(1).words);
        // Where not every stamp fits, none keeps its words before any is left off.
        Parcel.Sent pinched=parcel("Synthetic",new ArrayList<>());
        pinched.stamps=List.of(row("note-1","titled","cc33",3000L,"req-new","w".repeat(3000)),row("note-1","titled","dd44",2000L,"req-old","x".repeat(3000)));
        byte[] pinchedBare=Parcel.wrap(parcel("Synthetic",new ArrayList<>()),Envelope.MAX_TEXT);
        // Room for the newest with its words and no more.
        int newestWithWords=StampCard.wrap(pinched.stamps.subList(0,1)).length;
        Parcel.Sent squeezed=Parcel.open(Parcel.wrap(pinched,pinchedBare.length+8+newestWithWords));
        assertEquals(2,squeezed.stamps.size());
        assertNull(squeezed.stamps.get(0).words);assertNull(squeezed.stamps.get(1).words);
        // Little room: the oldest left off, never more than the room.
        byte[] alone=Sleeve.wrap(sleeve("file-1"),Envelope.MAX_TEXT);
        Sleeve.Sent two=sleeve("file-1");two.stamps=many.subList(0,2);
        byte[] both=Sleeve.wrap(two,Envelope.MAX_TEXT);
        int one=alone.length+(both.length-alone.length)-1;
        byte[] tight=Sleeve.wrap(two,one);
        assertTrue(tight.length<=one);
        assertEquals(1,Sleeve.open(tight).stamps.size());assertEquals("req-0",Sleeve.open(tight).stamps.get(0).request);
        assertArrayEquals("no room: the sleeve alone",alone,Sleeve.wrap(two,alone.length+8));
        // A card saying more than go with one thing is not taken.
        java.io.ByteArrayOutputStream raw=new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out=new java.io.DataOutputStream(raw);
        out.write(alone);byte[] card=StampCard.wrap(many);out.writeInt(StampCard.WITH_MARK);out.writeInt(card.length);out.write(card);
        Sleeve.Sent tooMany=Sleeve.open(raw.toByteArray());
        assertNotNull(tooMany);assertNull(tooMany.stamps);
    }

    // ---- two people's notebooks ------------------------------------------------------------------------------------------

    private static final String A_AT="MxStampsSharerFixture@127.0.0.1:9821",B_AT="MxStampsReceiverFixture@127.0.0.1:9822";

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
        String file(String note,byte[] bytes,String name,NoteStore.Branch.Kind held) throws Exception {
            String id=UUID.randomUUID().toString();
            Files.write(store.fileFor(id).toPath(),bytes);
            store.keep(new NoteStore.Held(id,note,name,"application/pdf",bytes.length,System.currentTimeMillis(),held));
            MediaManifest made=new MediaService(null,new BlobStore(shelf)).publish(bytes,"application/pdf");
            store.published(id,made.encode());
            return id;
        }
        /** A note to another device as Post makes it, heard there; what went written down as Post writes it. */
        Post.Landed note(Device to,String id,java.util.function.Consumer<Parcel.Sent> change) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=Post.parcelFor(store,store.address(to.at),n,store.above(id),true,false);
            if(change!=null)change.accept(going);
            byte[] sealed=Envelope.seal(page(id),n.revision,System.currentTimeMillis(),Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
            store.handedOver(to.at,id,n.revision);Post.stampsWent(context,to.at,id,going.stamps);
            return Post.arrived(null,to.store,to.keys,sealed);
        }
        /** A file on its own to another device, as Post makes its sleeve, not asking to be answered. */
        Post.Landed sleeve(Device to,String file,java.util.function.Consumer<Sleeve.Sent> change) throws Exception {
            Sleeve.Sent going=store.sleeve(file,to.at).quiet();
            if(change!=null)change.accept(going);
            byte[] sealed=Envelope.seal(page(file),store.fileRevision(file),System.currentTimeMillis(),Sleeve.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
            store.handedOver(to.at,file,store.fileRevision(file));Post.stampsWent(context,to.at,file,going.stamps);
            return Post.arrived(to.context,to.store,to.keys,sealed);
        }
        Set<String> requests(){Set<String> all=new HashSet<>();for(NoteStore.StampRow one:store.stamps())all.add(one.request());return all;}
        NoteStore.StampRow only(String request){for(NoteStore.StampRow one:store.stamps())if(one.request().equals(request))return one;return null;}
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device a,b;private File shelf;
    @Before public void two() throws Exception {
        Hashes.setSha3(Sha3::of);
        shelf=temp.newFolder("relay");
        a=new Device("Sharer",A_AT);b=new Device("Receiver",B_AT);
        a.pair(b,"Receiver");b.pair(a,"Sharer");
    }

    private Set<String> owed(Device from){Set<String> all=new HashSet<>();for(Post.StampsOwed one:Post.stampsOwed(from.context,from.store))all.add(one.thing()+(one.sleeve()?" sleeve ":" note ")+one.address());return all;}

    @Test public void theReportGoesWithTheFileOnlyWhereChosen() throws Exception {
        // Decision 251: the report kept beside a file (decision 253) follows it to the same people, once chosen.
        String loose=a.file(Things.HOME,new byte[]{4,5,6},"plan.pdf",NoteStore.Branch.Kind.COLLECTION);
        String report=a.file(Things.HOME,"%PDF-1.4 synthetic".getBytes(java.nio.charset.StandardCharsets.UTF_8),Integritas.reportName("plan.pdf"),NoteStore.Branch.Kind.COLLECTION);
        assertEquals(report,a.store.reportBeside(loose));
        assertNull("a report has no report",a.store.reportBeside(report));
        a.store.give(Sharing.Scope.FILE,loose,B_AT,Sharing.Level.READ,"Receiver");
        assertTrue("off until chosen",a.store.sharesOn(Sharing.Scope.FILE,report).isEmpty());
        assertEquals(1,a.store.reportGoesWith(loose,true));
        assertEquals("the one who has the file has its report",1,a.store.sharesOn(Sharing.Scope.FILE,report).size());
        a.store.give(Sharing.Scope.FILE,loose,"MxSYNTHETICSECOND@relay.example:9001",Sharing.Level.WRITE,"Second");
        assertEquals("and one given it later",2,a.store.sharesOn(Sharing.Scope.FILE,report).size());
        a.store.reportGoesWith(loose,false);
        a.store.give(Sharing.Scope.FILE,loose,"MxSYNTHETICTHIRD@relay.example:9001",Sharing.Level.READ,"Third");
        assertEquals("not once turned off",2,a.store.sharesOn(Sharing.Scope.FILE,report).size());
    }

    @Test public void aStampedNoteAndFileReachSomebodyElseWithTheirStampsAndNothingElseIsTaken() throws Exception {
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Synthetic contract";n.body="Synthetic terms";n.revision=1;a.store.save(n);
        String attached=a.file(n.id,new byte[]{1,2,3},"scan.pdf",NoteStore.Branch.Kind.PAGE);
        String loose=a.file(Things.HOME,new byte[]{4,5,6},"plan.pdf",NoteStore.Branch.Kind.COLLECTION);
        NoteStore.Note other=new NoteStore.Note();other.book=Things.HOME;other.title="Not shared";other.body="Private synthetic";other.revision=1;a.store.save(other);
        a.store.stamped(n.id,Integritas.TITLED,n.title,"aa01",1_760_000_000_000L,"req-note","","0xFAKE",n.title,n.body);
        a.store.stamped(attached,Integritas.FILE,"scan.pdf","aa02",1_760_000_001_000L,"req-attached","","0xFAKE",null,null);
        long looseRow=a.store.stamped(loose,Integritas.FILE,"plan.pdf","aa03",1_760_000_002_000L,"req-loose","","0xFAKE",null,null);
        a.store.stamped(other.id,Integritas.TITLED,other.title,"aa04",1_760_000_003_000L,"req-other","","0xFAKE",other.title,other.body);
        a.store.give(Sharing.Scope.PAGE,n.id,B_AT,Sharing.Level.READ,"Receiver");
        a.store.give(Sharing.Scope.FILE,loose,B_AT,Sharing.Level.READ,"Receiver");
        assertEquals("before anything went, both owed to B for their stamps",Set.of(n.id+" note "+B_AT,loose+" sleeve "+B_AT),owed(a));

        // The note goes, carrying its own stamp and its file's, and nothing else of A's.
        Parcel.Sent made=Post.parcelFor(a.store,a.store.address(B_AT),n,a.store.above(n.id),true,false);
        Set<String> carried=new HashSet<>();for(StampCard.Row one:made.stamps)carried.add(one.request);
        assertEquals(Set.of("req-note","req-attached"),carried);
        Post.Landed landed=a.note(b,n.id,null);
        assertTrue("the screens are told",landed.stamps);
        assertEquals(Set.of("aa01","aa02"),landed.stamped);
        assertEquals(Set.of("req-note","req-attached"),b.requests());
        NoteStore.StampRow kept=b.only("req-note");
        assertEquals(n.id,kept.id());assertArrayEquals(new String[]{n.title,n.body},b.store.stampedWords(kept.n()));
        assertEquals(NoteStore.STAMP_WAITING,kept.state());

        // The file on its own, with its own.
        a.sleeve(b,loose,null);
        assertEquals(Set.of("req-note","req-attached","req-loose"),b.requests());
        assertTrue("nothing owed now",owed(a).isEmpty());

        // A sender claiming stamps of something else: of another note, of a note of B's own, of the loose file through the
        // note, of the note through the sleeve, or of the note's file said as a note. None is taken.
        NoteStore.Note mine=new NoteStore.Note();mine.book=Things.HOME;mine.title="B's own";mine.body="Synthetic";mine.revision=1;b.store.save(mine);
        a.note(b,n.id,p->{List<StampCard.Row> more=new ArrayList<>(p.stamps);
            more.add(row(other.id,Integritas.TITLED,"bb01",1L,"forged-other",null));
            more.add(row(mine.id,Integritas.TITLED,"bb02",2L,"forged-mine",null));
            more.add(row(loose,Integritas.FILE,"bb03",3L,"forged-loose",null));
            more.add(row(attached,Integritas.TITLED,"bb04",4L,"forged-kind",null));
            p.stamps=more;});
        a.sleeve(b,loose,s->s.stamps=List.of(row(n.id,Integritas.TITLED,"bb05",5L,"forged-sleeve",null),row(loose,Integritas.FILE,"aa03",1_760_000_002_000L,"req-loose",null)));
        assertEquals(Set.of("req-note","req-attached","req-loose"),b.requests());
        assertTrue(b.store.stampsCameWith(mine.id,new ArrayList<>(),List.of(row(other.id,Integritas.TITLED,"bb06",6L,"forged-direct",null))).added()==0);

        // A stamp made after sharing, of the note and of the file: owed again without a new revision, and it reaches B.
        long revision=a.store.get(n.id).revision;
        a.store.stamped(n.id,Integritas.TITLED,n.title,"aa05",1_760_000_100_000L,"req-note-2","","0xFAKE",n.title,n.body);
        a.store.stamped(loose,Integritas.FILE,"plan.pdf","aa03",1_760_000_101_000L,"req-loose-2","","0xFAKE",null,null);
        assertEquals(Set.of(n.id+" note "+B_AT,loose+" sleeve "+B_AT),owed(a));
        assertEquals("the words did not change",revision,a.store.get(n.id).revision);
        for(Post.StampsOwed one:Post.stampsOwed(a.context,a.store))if(one.sleeve())a.sleeve(b,one.thing(),null);else a.note(b,one.thing(),null);
        assertTrue(b.requests().containsAll(Set.of("req-note-2","req-loose-2")));
        assertTrue(owed(a).isEmpty());

        // Found on Minima at A: owed again, and B learns the block.
        a.store.stampLanded(looseRow,2374505,"0xTXPOW","0xTX","2026-10-25 09:00:00",3,System.currentTimeMillis());
        assertEquals(Set.of(loose+" sleeve "+B_AT),owed(a));
        Post.Landed found=a.sleeve(b,loose,null);
        assertTrue(found.stamps);
        NoteStore.StampRow there=b.only("req-loose");
        assertTrue(there.confirmed());assertEquals(2374505,there.block());assertEquals("0xTXPOW",there.txpow());
        assertTrue(owed(a).isEmpty());
        // And never back to waiting from an older copy.
        a.sleeve(b,loose,s->s.stamps=List.of(row(loose,Integritas.FILE,"aa03",1_760_000_002_000L,"req-loose",null)));
        assertTrue(b.only("req-loose").confirmed());
    }

    @Test public void stampsGoNowhereFromAReaderNorToTheOwnersOwnDevices() throws Exception {
        // A note B was given to read: B stamping it sends nothing back for it, and nothing from a reader would be taken.
        NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Shared to read";n.body="Synthetic";n.revision=1;a.store.save(n);
        a.store.give(Sharing.Scope.PAGE,n.id,B_AT,Sharing.Level.READ,"Receiver");
        a.note(b,n.id,null);
        b.store.stamped(n.id,Integritas.TITLED,n.title,"cc01",1_760_000_000_000L,"req-b","","0xFAKE",n.title,n.body);
        assertTrue("a reader's copy goes nowhere",owed(b).isEmpty());
        // Nor to a device of A's own, which has the stamps card.
        Device mine=new Device("Sharer's other",  "MxStampsSharersOtherFixture@127.0.0.1:9823");
        a.store.pairedWith(mine.at,"Sharer's other",true,mine.keys.agreement().getPublic().getEncoded(),mine.keys.signing().getPublic().getEncoded());
        a.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,mine.at,true));
        a.store.stamped(n.id,Integritas.TITLED,n.title,"cc02",1_760_000_001_000L,"req-a","","0xFAKE",n.title,n.body);
        assertEquals(Set.of(n.id+" note "+B_AT),owed(a));
    }
}
