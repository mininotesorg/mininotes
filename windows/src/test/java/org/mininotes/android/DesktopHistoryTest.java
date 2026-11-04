package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * History (the owner, 2026-10-31: "do we have a full log of all actions? This should be made available to the user but of course
 * not including anything related to the private space"; decision 305). In the real notebook and through {@code Post.arrived},
 * with synthetic devices and no network: a note from my tablet is a line, my note answered by it is a line, the same again soon
 * is the same line, old lines go, and a line whose note is in the bin, gone for good or never kept here says nothing.
 */
public class DesktopHistoryTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String PC="MxHistoryPcFixture@127.0.0.1:9821",TABLET="MxHistoryTabletFixture@127.0.0.1:9822";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name) throws Exception {
            store.pairedWith(other.at,name,true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        String write(String title){NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title=title;n.body="Synthetic "+title;n.revision=1;store.save(n);return n.id;}
        /** A note as this device sends it, not asking to be answered. */
        byte[] note(Device to,String id) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
                List.of(),1L,List.of(),store.steps(store.above(id)),"",null);
            return Envelope.seal(page(id),n.revision,1,Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        /** This device saying it has another's note at a revision. */
        byte[] took(Device to,String id,long revision) throws Exception {
            return Envelope.seal(page(id),revision,System.currentTimeMillis(),Receipt.wrap(Receipt.TOOK),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    private Device pc,tablet;
    @Before public void two() throws Exception {
        Hashes.setSha3(Sha3::of);Say.use("en");
        pc=new Device("Test PC",PC);tablet=new Device("My tablet",TABLET);
        tablet.pair(pc,"Test PC");pc.pair(tablet,"My tablet");
    }

    private static List<String> said(NoteStore store) {
        List<String> out=new ArrayList<>();
        for(NoteStore.HistoryShown one:store.history())out.add(History.said(one.line().kind(),one.title(),one.who()));
        return out;
    }

    @Test public void aNoteThatCameAndAnAnswerToMineAreLinesWithTheirNamesNow() throws Exception {
        String theirs=tablet.write("Plan");
        pc.hear(tablet.note(pc,theirs));
        String mine=pc.write("Shopping");
        pc.hear(tablet.took(pc,mine,1));
        List<String> lines=said(pc.store);
        assertTrue(lines.toString(),lines.contains("Plan came from My tablet."));
        assertTrue(lines.toString(),lines.contains("Shopping reached My tablet."));
        // Renamed since: the line says its name now, for nothing of a title is kept in it.
        NoteStore.Note renamed=pc.store.get(mine);renamed.title="Groceries";renamed.revision=2;pc.store.save(renamed);
        assertTrue(said(pc.store).contains("Groceries reached My tablet."));
    }

    @Test public void theSameAgainSoonIsOneLineAndLaterANewOne() {
        String id=pc.write("Plan");long now=System.currentTimeMillis();
        pc.store.happened(History.Kind.NOTE_REACHED,id,TABLET,now-30*60_000L);
        pc.store.happened(History.Kind.NOTE_REACHED,id,TABLET,now-25*60_000L);
        assertEquals(1,pc.store.history().size());
        assertEquals(2,pc.store.history().get(0).line().times());
        pc.store.happened(History.Kind.NOTE_REACHED,id,TABLET,now);
        List<NoteStore.HistoryShown> lines=pc.store.history();
        assertEquals(2,lines.size());
        assertEquals("newest first",now,lines.get(0).line().at());
    }

    @Test public void linesOlderThanThirtyDaysAreLetGo() {
        String id=pc.write("Plan");long now=System.currentTimeMillis();
        pc.store.happened(History.Kind.NOTE_CAME,id,TABLET,now-History.KEPT-60_000L);
        pc.store.happened(History.Kind.BACK,"",TABLET,now);
        List<String> lines=said(pc.store);
        assertEquals(List.of("My tablet can be reached again."),lines);
    }

    @Test public void aLineSaysNothingWhileItsNoteIsInTheBinAndGoesWithItForGood() {
        String id=pc.write("Plan");long now=System.currentTimeMillis();
        pc.store.happened(History.Kind.NOTE_CAME,id,TABLET,now);
        pc.store.putAway(NoteStore.Branch.Kind.PAGE,id,true,true);
        assertTrue(pc.store.history().isEmpty());
        pc.store.restore(NoteStore.Branch.Kind.PAGE,id);
        assertEquals("back from the bin, its line too",1,pc.store.history().size());
        pc.store.putAway(NoteStore.Branch.Kind.PAGE,id,true,true);
        pc.store.erase(NoteStore.Branch.Kind.PAGE,id);
        assertTrue(pc.store.history().isEmpty());
    }

    @Test public void somethingNeverInTheNotebookOrSomebodyUnknownSaysNothing() {
        // What the private space holds is never in the notebook, so a line about it, were one ever written, would say nothing.
        long now=System.currentTimeMillis();
        pc.store.happened(History.Kind.NOTE_REACHED,UUID.randomUUID().toString(),TABLET,now);
        pc.store.happened(History.Kind.FILE_CAME,UUID.randomUUID().toString(),TABLET,now);
        pc.store.happened(History.Kind.NOTE_CAME,pc.write("Plan"),"MxNobodyFixture@127.0.0.1:9829",now);
        assertTrue(pc.store.history().isEmpty());
    }
}
