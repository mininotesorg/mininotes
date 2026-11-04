package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Only what changed in a note, between real notebooks (decision 320; the owner, 2026-11-03). Two synthetic devices of the
 * owner's, no node and no network: each note goes as Post makes it (the whole parcel, then the change where Post would send
 * one), is handed to the other notebook, answered, and written down as went, as Post does. The first time goes whole; a line
 * typed into a long note then goes as a few hundred bytes and arrives as the very text; a build that has not said it reads
 * changes gets every note whole; a copy of the base that is not here exactly takes nothing; and a change against what was
 * agreed is put together with what was written here since.
 */
public class DeltaStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC="MxDeltaPcFixture@127.0.0.1:9801",PHONE="MxDeltaPhoneFixture@127.0.0.1:9802";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at,name;
        Device(String name,String at) throws Exception {
            this.at=at;this.name=name;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other) throws Exception {
            store.pairedWith(other.at,other.name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        void write(String id,String body){NoteStore.Note n=store.get(id);n.body=body;n.revision++;store.save(n);}
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device pc,phone;

    /** What one sending was: whether it went as a change, and how many bytes. */
    private static final class Went {final boolean change;final int bytes;Went(boolean change,int bytes){this.change=change;this.bytes=bytes;}}

    /** One note from one device to another, as Post sends it; heard there, answered, and written down as went. */
    private Went send(Device from,Device to,String id,boolean reads) throws Exception {
        NoteStore.Note n=from.store.get(id);
        Parcel.Sent going=Post.parcelFor(from.store,from.store.address(to.at),n,from.store.above(id),true,true);
        Parcel.Sent change=Post.changeFor(from.store,to.at,n,going,reads,false);
        byte[] text=Parcel.wrap(change!=null?change:going,Envelope.MAX_TEXT);
        byte[] sealed=Envelope.seal(page(id),n.revision,System.currentTimeMillis(),text,from.keys.signing(),to.keys.agreement().getPublic());
        from.store.handedOver(to.at,id,n.revision);
        Post.arrived(null,to.store,to.keys,sealed);
        NoteStore.Note there=to.store.get(id);
        boolean took=there!=null&&there.revision==n.revision&&there.body.equals(n.body);
        // Answered only where it was written down: a change that could not be rebuilt is not answered at all.
        if(there!=null&&to.store.versions(id).stream().anyMatch(v->v.revision==n.revision&&v.body.equals(n.body)))
            from.store.acknowledged(to.at,id,n.revision,took);
        from.store.keepVersion(id,n.revision,"",n.title,n.body);
        return new Went(change!=null,text.length);
    }

    /** A long note: numbered lines of ordinary writing, about {@code characters} long. */
    private static String longNote(int characters) {
        StringBuilder out=new StringBuilder();
        for(int line=1;out.length()<characters;line++)
            out.append(line).append(". The shopping, the garden and the letters to answer before Friday.\n");
        return out.toString();
    }

    private String id;

    @Before public void two() throws Exception {
        Hashes.setSha3(Sha3::of);
        pc=new Device("PC",PC);phone=new Device("Phone",PHONE);
        pc.pair(phone);phone.pair(pc);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PHONE,true));
        phone.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PC,true));
        NoteStore.Note n=new NoteStore.Note();n.book=phone.store.someBook();n.title="Long";n.body=longNote(90000);n.revision=1;
        phone.store.save(n);id=n.id;
    }

    @Test public void theFirstTimeWholeThenALineTypedGoesAsAFewHundredBytesAndArrivesExact() throws Exception {
        Went first=send(phone,pc,id,true);
        assertFalse("nothing agreed yet: whole",first.change);
        assertTrue(first.bytes>90000);
        assertEquals(phone.store.get(id).body,pc.store.get(id).body);
        assertEquals(1,phone.store.agreedAt(id,PC));

        String now=phone.store.get(id).body.replace("500. The shopping","500. The shopping and a new kettle");
        phone.write(id,now);
        Went second=send(phone,pc,id,true);
        assertTrue("a change",second.change);
        assertTrue("a change of "+second.bytes+" bytes",second.bytes<1000);
        assertEquals("the very text",now,pc.store.get(id).body);
        assertEquals(2,pc.store.get(id).revision);
        assertEquals("agreed, so the next goes as a change too",2,phone.store.agreedAt(id,PC));
        assertTrue("kept under versions as it arrived",pc.store.versions(id).stream().anyMatch(v->v.revision==2&&v.body.equals(now)));

        phone.write(id,now+"one more line");
        Went third=send(phone,pc,id,true);
        assertTrue(third.change);
        assertEquals(now+"one more line",pc.store.get(id).body);
    }

    @Test public void aBuildThatHasNotSaidItReadsChangesGetsEveryNoteWhole() throws Exception {
        send(phone,pc,id,false);
        for(int round=0;round<3;round++) {
            phone.write(id,phone.store.get(id).body+"line "+round+"\n");
            Went went=send(phone,pc,id,false);
            assertFalse(went.change);
            assertTrue(went.bytes>90000);
            assertEquals(phone.store.get(id).body,pc.store.get(id).body);
        }
    }

    @Test public void aBaseNotHereExactlyTakesNothingAndTheWholeNoteThenLands() throws Exception {
        send(phone,pc,id,true);
        // Here, the agreed text is gone from Versions and the page says something else since: no copy of the base.
        pc.store.getWritableDatabase().execSQL("DELETE FROM versions WHERE note=?",new Object[]{id});
        pc.write(id,pc.store.get(id).body.replace("7. The","7. A"));
        String mine=pc.store.get(id).body;
        phone.write(id,phone.store.get(id).body.replace("300. The","300. A"));
        Went went=send(phone,pc,id,true);
        assertTrue(went.change);
        assertEquals("nothing taken",mine,pc.store.get(id).body);
        assertEquals("nothing kept from it",0,pc.store.versions(id).stream().filter(v->v.revision==2).count());
        assertEquals("not answered, so still owed",1,phone.store.agreedAt(id,PC));
        // What the ask brings: the note whole (Post sends whole to an ASK naming a note), put together with what is here.
        NoteStore.Note n=phone.store.get(id);
        Parcel.Sent whole=Post.parcelFor(phone.store,phone.store.address(PC),n,phone.store.above(id),true,true);
        assertNull("asked for whole: no change",Post.changeFor(phone.store,PC,n,whole,true,true));
        Post.arrived(null,pc.store,pc.keys,Envelope.seal(page(id),n.revision,System.currentTimeMillis(),Parcel.wrap(whole,Envelope.MAX_TEXT),
            phone.keys.signing(),pc.keys.agreement().getPublic()));
        String landed=pc.store.get(id).body;
        assertTrue("their line",landed.contains("300. A shopping"));
        assertTrue("and mine",landed.contains("7. A shopping"));
    }

    @Test public void aChangeAgainstTheAgreedTextIsPutTogetherWithWritingHereSince() throws Exception {
        send(phone,pc,id,true);
        // Written here meanwhile, at the bottom; there, at the top.
        pc.write(id,pc.store.get(id).body+"call the plumber");
        phone.write(id,phone.store.get(id).body.replace("2. The shopping","2. The shopping and butter"));
        Went went=send(phone,pc,id,true);
        assertTrue(went.change);
        String both=pc.store.get(id).body;
        assertTrue(both.substring(0,300),both.contains("2. The shopping and butter"));
        assertTrue(both.endsWith("call the plumber"));
    }
}
