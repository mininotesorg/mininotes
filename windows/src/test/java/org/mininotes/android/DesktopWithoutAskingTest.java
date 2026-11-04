package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import com.eurobuddha.maxima.core.crypto.Hashes;
import com.eurobuddha.maxima.core.media.MediaManifest;
import com.eurobuddha.maxima.core.media.MediaService;
import com.eurobuddha.maxima.core.store.BlobStore;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Accept everything without asking, per contact (the owner, 2026-10-28: "we have to make it that we dont need to accept files
 * shared with my contact or at least it should be optional, direct when I accept one we should have a message auto accept all
 * incoming files from this contact and this should be a parameter at the contact level"; decision 269). Two notebooks, through
 * the real arrival path ({@code Post.arrived}), synthetic devices and no network; then the switch on the person's page and the
 * offer after Accept, drawn on a synthetic PC.
 */
public class DesktopWithoutAskingTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    private Desktop pad;
    @After public void close() throws Exception {
        for(NoteStore one:open)one.close();
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog)w.dispose();pad.shutdown(false);});await(()->!pad.frame.isDisplayable());}
    }

    private static final String ANA="MxWithoutAskingAnaFixture@127.0.0.1:9891",PC="MxWithoutAskingPcFixture@127.0.0.1:9892",
        TABLET="MxWithoutAskingTabletFixture@127.0.0.1:9893";
    private static final NoteStore.Branch.Kind PAGE=NoteStore.Branch.Kind.PAGE;

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
        byte[] note(Device to,String id) throws Exception {
            NoteStore.Note n=store.get(id);
            Parcel.Sent going=new Parcel.Sent("","","","",n.title,n.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
                List.of(),1L,List.of(),store.steps(store.above(id)),"",null);
            return Envelope.seal(page(id),n.revision,1,Parcel.wrap(going,Envelope.MAX_TEXT),keys.signing(),to.keys.agreement().getPublic());
        }
        String write(String in,String title,String body){
            NoteStore.Note n=new NoteStore.Note();n.book=in;n.title=title;n.body=body;n.revision=1;store.save(n);return n.id;
        }
    }

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}
    private static byte[] bytes(int size,int seed){byte[] b=new byte[size];new Random(seed).nextBytes(b);return b;}

    private Device ana,pc,tablet;private File shelf;
    @Before public void three() throws Exception {
        Hashes.setSha3(Sha3::of);
        ana=new Device("Ana's phone",ANA);pc=new Device("Test PC",PC);tablet=new Device("My tablet",TABLET);
        ana.pair(pc,"Test PC",false);pc.pair(ana,"Ana's phone",false);
        tablet.pair(pc,"Test PC",true);pc.pair(tablet,"My tablet",true);
        shelf=temp.newFolder("door");
    }

    private String anasNote(String title) throws Exception {
        String id=ana.write(Things.HOME,title,"Synthetic "+title);
        ana.store.setLevel(Sharing.Scope.PAGE,id,PC,Sharing.Level.WRITE,null);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.PAGE,id,ANA,true));
        return id;
    }

    /** Files offered from {@code from} to {@code to}, their pieces where both can reach them. */
    private byte[] offer(Device from,Device to,String wire,byte[]... files) throws Exception {
        List<Enclosure.Listed> listed=new ArrayList<>();
        for(byte[] plain:files) {
            MediaManifest made=new MediaService(null,new BlobStore(shelf)).publish(plain,"application/octet-stream");
            listed.add(new Enclosure.Listed(UUID.randomUUID().toString(),"photo-"+listed.size()+".jpg","image/jpeg",plain.length,made.encode()));
        }
        return Envelope.seal(page(wire),1,System.currentTimeMillis(),Drop.wrap(listed),from.keys.signing(),to.keys.agreement().getPublic());
    }

    private static Set<String> ids(List<NoteStore.Branch> lines){Set<String> out=new HashSet<>();for(NoteStore.Branch one:lines)out.add(one.id);return out;}

    @Test public void offByDefaultHerNoteWaitsAndHerFilesAsk() throws Exception {
        assertFalse(pc.store.acceptsWithoutAsking(ANA));
        String id=anasNote("Saturday market");
        assertTrue(pc.hear(ana.note(pc,id)).waiting);
        assertTrue(pc.store.waits(PAGE,id));
        Post.Landed files=pc.hear(offer(ana,pc,UUID.randomUUID().toString(),bytes(2048,1)));
        assertNotNull("asked",files.asking);
        assertEquals(Drop.ASKING,pc.store.transfer(files.asking).state);
    }

    @Test public void onHerNoteComesInAcceptedWhereAcceptPutsItAndNothingIsToldItWaits() throws Exception {
        pc.store.acceptWithoutAsking(ANA,true);
        assertTrue(pc.store.acceptsWithoutAsking(ANA));
        String id=anasNote("Saturday market");
        Post.Landed landed=pc.hear(ana.note(pc,id));
        assertFalse("not a question",landed.waiting);
        assertEquals("Ana's phone shared a note with you.",landed.said);
        assertFalse(pc.store.waits(PAGE,id));
        assertTrue(pc.store.waitingForYou().isEmpty());
        // Where Accept would have put it (decision 122): Shared with me, not loose on Home.
        assertTrue(ids(pc.store.contents(NoteStore.SHARED)).contains(id));
        assertFalse(ids(pc.store.contents(Things.HOME)).contains(id));
        assertTrue(pc.store.acceptedHere(PAGE.name(),id));
        // Nobody is told it waits, so her tick says the PC has it.
        assertTrue(pc.store.toldWaits().isEmpty());
        // And the rights are as she gave them: Can write, not read only.
        assertFalse(pc.store.onlyReads(id));
    }

    @Test public void onHerFilesAreFetchedAtOnceAndLandInSharedWithMe() throws Exception {
        pc.store.acceptWithoutAsking(ANA,true);
        String wire=UUID.randomUUID().toString();
        Post.Landed landed=pc.hear(offer(ana,pc,wire,bytes(2048,2)));
        assertNull("nothing asked",landed.asking);
        NoteStore.Transfer one=pc.store.transferOnTheWire(wire,ANA,false);
        assertEquals(Drop.FETCHING,one.state);
        assertEquals(1,pc.store.transfersDue(System.currentTimeMillis()).size());
        for(NoteStore.Loose file:one.files)assertTrue(pc.store.looseArrived(file,new MediaService(null,new BlobStore(shelf)).fetch(MediaManifest.decode(file.manifest))));
        assertTrue(pc.store.transfer(one.id).allHere());
        assertTrue(ids(pc.store.contents(NoteStore.SHARED)).contains(one.files.get(0).id));
        assertTrue(pc.store.waitingForYou().isEmpty());
    }

    @Test public void aBlockedSenderStaysBlockedAndForgottenSomebodyIsAskedAgain() throws Exception {
        pc.store.acceptWithoutAsking(ANA,true);
        pc.store.blockSender(ANA,"Ana's phone");
        assertFalse("blocked stays blocked",pc.store.acceptsWithoutAsking(ANA));
        assertFalse(pc.store.offerWithoutAsking(ANA));
        pc.store.forgetBlocked(ANA);
        assertTrue(pc.store.acceptsWithoutAsking(ANA));
        pc.store.removeAddress(ANA);
        assertFalse("forgotten, off the list",pc.store.acceptsWithoutAsking(ANA));
    }

    @Test public void myOwnDevicesStillTakeEverythingAndAreNeverOffered() throws Exception {
        assertFalse(pc.store.offerWithoutAsking(TABLET));
        String wire=UUID.randomUUID().toString();
        Post.Landed landed=pc.hear(offer(tablet,pc,wire,bytes(1024,3)));
        assertNull(landed.asking);
        assertEquals(Drop.FETCHING,pc.store.transferOnTheWire(wire,TABLET,false).state);
    }

    @Test public void theOfferIsMadeOnceAndKeepAskingIsRemembered() throws Exception {
        assertTrue(pc.store.offerWithoutAsking(ANA));
        pc.store.keepAsking(ANA);
        assertFalse(pc.store.offerWithoutAsking(ANA));
        assertFalse("Keep asking leaves it off",pc.store.acceptsWithoutAsking(ANA));
        assertFalse("not paired here, not offered",pc.store.offerWithoutAsking("MxNobody@127.0.0.1:1"));
    }

    // ---- drawn ------------------------------------------------------------------------------------------------

    @Test public void theSwitchOnHerPageAndTheOfferAfterAccept() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
        Keys hers=new Keys(new Context(temp.newFolder("ana-keys")));
        pad.store.pairedWith(ANA,"Ana",false,hers.agreement().getPublic().getEncoded(),hers.signing().getPublic().getEncoded());

        // Her page: the switch, off, with its line under it.
        SwingUtilities.invokeAndWait(()->DesktopPeople.person(pad,ANA,"Ana"));
        JDialog page=showing(null);
        JCheckBox[] unasked={null};
        await(()->{SwingUtilities.invokeAndWait(()->unasked[0]=find(page.getContentPane(),"withoutAsking"));return unasked[0]!=null;});
        List<String> said=new ArrayList<>();SwingUtilities.invokeAndWait(()->texts(page.getContentPane(),said));
        assertTrue(said.toString(),said.contains(WithoutAsking.SWITCH));
        assertTrue(said.toString(),said.contains("Files, notes and folders from Ana come in at once."));
        assertFalse("off by default",unasked[0].isSelected());
        shot(page,"without-asking-switch-off.png");
        SwingUtilities.invokeAndWait(()->unasked[0].doClick());
        await(()->pad.store.acceptsWithoutAsking(ANA));
        await(()->"Everything from Ana is accepted without asking.".equals(pad.status.getText()));
        await(()->{JCheckBox[] again={null};SwingUtilities.invokeAndWait(()->{again[0]=find(page.getContentPane(),"withoutAsking");});return again[0]!=null&&again[0].isSelected();});
        shot(page,"without-asking-switch-on.png");
        SwingUtilities.invokeAndWait(()->{find(page.getContentPane(),"withoutAsking").doClick();});
        await(()->!pad.store.acceptsWithoutAsking(ANA));
        await(()->"Everything from Ana waits for you to accept it.".equals(pad.status.getText()));
        SwingUtilities.invokeAndWait(page::dispose);

        // Accepted by hand: the offer, one primary button, and Keep asking beside it.
        SwingUtilities.invokeLater(()->pad.offerWithoutAsking(ANA));
        JDialog offer=showing("offerWithoutAsking");
        assertEquals("Accept everything from Ana without asking?",offer.getTitle());
        List<String> words=new ArrayList<>();SwingUtilities.invokeAndWait(()->texts(offer.getContentPane(),words));
        assertTrue(words.toString(),words.contains(WithoutAsking.WHERE));
        assertTrue(words.toString(),words.contains(WithoutAsking.YES)&&words.contains(WithoutAsking.KEEP));
        assertEquals("one default, the primary",WithoutAsking.YES,offer.getRootPane().getDefaultButton().getText());
        shot(offer,"without-asking-offer.png");
        SwingUtilities.invokeAndWait(()->offer.getRootPane().getDefaultButton().doClick());
        await(()->pad.store.acceptsWithoutAsking(ANA));
        await(()->!offer.isShowing());
        // On now: not offered again.
        assertFalse(pad.store.offerWithoutAsking(ANA));
    }

    private static JDialog showing(String name) throws Exception {
        JDialog[] box={null};
        await(()->{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&(name==null||name.equals(d.getName())))box[0]=d;});return box[0]!=null;});
        Thread.sleep(400);
        return box[0];
    }
    private static JCheckBox find(Container in,String name) {
        for(Component c:in.getComponents()) {
            if(c instanceof JCheckBox b&&name.equals(b.getName()))return b;
            if(c instanceof Container k){JCheckBox found=find(k,name);if(found!=null)return found;}
        }
        return null;
    }
    private static void texts(Container in,List<String> out) {
        for(Component c:in.getComponents()) {
            if(c instanceof JLabel l&&l.getText()!=null)out.add(l.getText());
            else if(c instanceof AbstractButton b&&b.getText()!=null)out.add(b.getText());
            else if(c instanceof javax.swing.text.JTextComponent t)out.add(t.getText());
            if(c instanceof Container k)texts(k,out);
        }
    }
    private static void shot(JDialog box,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            JRootPane root=box.getRootPane();BufferedImage shot=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=shot.createGraphics();root.paint(g);g.dispose();
            Path out=Paths.get("build","verification");Files.createDirectories(out);javax.imageio.ImageIO.write(shot,"png",out.resolve(name).toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
