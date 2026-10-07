package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Something another person shared with me for the first time, on the PC's screens (the owner, 2026-10-06; docs/HOME.md,
 * decision 109): the pop-up for one thing and for several, Shared with me with Waiting for you first, the Refuse box, and on
 * the sender's side the notice and the Refused line in the Share box. Each pictured, made-up people and notes only.
 */
public class DesktopSharedWithMeTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private static final String ANA="MxSharedAnaFixture@127.0.0.1:9821",BEN="MxSharedBenFixture@127.0.0.1:9822";

    @Test public void thePopUpWaitingForYouTheRefuseBoxAndTheSendersSide() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("shared").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            NoteStore store=pad.store;
            store.pairedWith(ANA,"Ana",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            store.pairedWith(BEN,"Ben",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            SwingUtilities.invokeAndWait(pad::goHome);settle(pad);
            // Ana's folder arrives, as an arrival from somebody else leaves it: kept on Home, hidden, waiting.
            String market=store.addCollection("Saturday market").id;
            theirs(store,"things",market,ANA);
            store.stands(Sharing.Scope.THING,market,Sharing.Level.WRITE,1);
            SwingUtilities.invokeAndWait(()->{pad.refresh();pad.askWaiting();});settle(pad);
            JDialog box=pad.waitingBox;
            assertNotNull("the pop-up",box);assertTrue(box.isShowing());
            assertTrue(text(box).contains("Ana shared “Saturday market” with you"));
            assertTrue(text(box).contains("A folder · Can write"));
            shoot(box,"109a-popup-one");
            // Two more, from Ben and from Ana, while it is up: the same box, counted.
            NoteStore.Note list=new NoteStore.Note();list.book=Things.HOME;list.title="Packing list";list.body="Synthetic socks";store.save(list);
            theirs(store,"notes",list.id,BEN);
            NoteStore.Note recipe=new NoteStore.Note();recipe.book=Things.HOME;recipe.title="Soup";recipe.body="Synthetic soup";store.save(recipe);
            theirs(store,"notes",recipe.id,ANA);
            SwingUtilities.invokeAndWait(()->{pad.refresh();pad.askWaiting();});settle(pad);
            assertSame("never a box over a box",box,pad.waitingBox);
            assertTrue(text(box).contains("Ana and Ben shared 3 things with you"));
            shoot(box,"109b-popup-several");
            // Said once: closed, and asked again, nothing comes.
            SwingUtilities.invokeAndWait(box::dispose);
            SwingUtilities.invokeAndWait(pad::askWaiting);settle(pad);
            assertFalse(pad.waitingBox.isShowing());
            // Home: Shared with me wears how many wait; nothing that waits is drawn.
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isShowing()&&w instanceof JDialog)w.dispose();});
            settle(pad);
            shoot(pad.frame,"109c-home-count");
            for(DesktopHome.Tile one:pad.home.grid.tiles){assertNotEquals(market,one.thing.id);assertNotEquals(list.id,one.thing.id);}
            // Shared with me: Waiting for you first, a line each, Refuse and Accept on it.
            SwingUtilities.invokeAndWait(()->pad.home.folder.openPlace(DesktopHome.sharedPlace()));settle(pad);
            assertEquals(3,pad.home.folder.asking.size());assertTrue(pad.home.folder.waits.isShowing());
            shoot(pad.frame,"109d-waiting-for-you");
            // Accepted: on Home, and the line is gone.
            NoteStore.Branch first=null;for(NoteStore.Branch one:pad.home.folder.asking)if(one.id.equals(market))first=one;
            NoteStore.Branch accepting=first;
            SwingUtilities.invokeAndWait(()->pad.acceptWaiting(accepting));settle(pad);settle(pad);
            assertFalse(store.waits(NoteStore.Branch.Kind.COLLECTION,market));
            assertEquals(2,pad.home.folder.asking.size());
            assertTrue(pad.status.getText(),pad.status.getText().contains("“Saturday market” is on Home now"));
            shoot(pad.frame,"109e-accepted");
            // Refuse: the box, quietly first, then telling them with a few words.
            NoteStore.Branch soup=null;for(NoteStore.Branch one:pad.home.folder.asking)if(one.id.equals(recipe.id))soup=one;
            NoteStore.Branch refusing=soup;
            JDialog refuse=opened(()->pad.refuseWaiting(refusing),"refuse");
            shoot(refuse,"109f-refuse-box");
            SwingUtilities.invokeAndWait(()->{button(refuse,FirstShare.TELL).doClick();JTextField words=field(refuse);words.setText("Thanks, I have one already");});
            Thread.sleep(200);shoot(refuse,"109g-refuse-and-tell");
            SwingUtilities.invokeAndWait(()->button(refuse,FirstShare.REFUSE).doClick());
            pad.network.flush(10000);settle(pad);
            assertNull("deleted here",store.get(recipe.id));
            assertEquals(1,pad.home.folder.asking.size());
            shoot(pad.frame,"109h-refused");
            SwingUtilities.invokeAndWait(()->pad.home.folder.close());settle(pad);
            // The sender's side: a note of mine shared with Ana, who refused it with a few words.
            NoteStore.Note mine=new NoteStore.Note();mine.book=Things.HOME;mine.title="Garden plan";mine.body="Synthetic tomatoes";store.save(mine);
            store.setLevel(Sharing.Scope.PAGE,mine.id,ANA,Sharing.Level.WRITE,null);
            store.setLevel(Sharing.Scope.PAGE,mine.id,BEN,Sharing.Level.READ,null);
            long when=System.currentTimeMillis()+5;
            assertTrue(store.left(ANA,mine.id,Sharing.Scope.PAGE,when));
            assertEquals("Garden plan",store.refusedBy(ANA,mine.id,Sharing.Scope.PAGE,"Thanks, I have one already",when));
            SwingUtilities.invokeAndWait(()->pad.arrived(Post.Landed.left(FirstShare.notice("Ana","Garden plan","Thanks, I have one already"),mine.id)));
            settle(pad);
            assertEquals("Ana refused “Garden plan”\n“Thanks, I have one already”",pad.status.getText());
            shoot(pad.frame,"109i-sender-notice");
            NoteStore.Branch target=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,mine.id,Sharing.EVERYTHING,"Garden plan","",0,0,false);
            JDialog share=opened(()->pad.share(target,null,null),"Share “Garden plan”");
            assertNotNull("the Refused line",named(share,"refused Ana"));
            shoot(share,"109j-share-box-refused");
            SwingUtilities.invokeAndWait(share::dispose);
        } finally {
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog&&w.isShowing())w.dispose();});
            pad.shutdown(false,null);
        }
    }

    /** A thing as an arrival from another person leaves it: theirs, from them, and waiting to be accepted. */
    private static void theirs(NoteStore store,String table,String id,String from) {
        store.getWritableDatabase().execSQL("UPDATE "+table+" SET theirs=1,origin=?,accepted=0"+("notes".equals(table)?",writes=1":"")+" WHERE id=?",new Object[]{from,id});
    }

    /** A box opened by something that waits for it to close, found once it is up, by its title or its name. */
    private static JDialog opened(Runnable open,String called) throws Exception {
        java.util.Set<Window> before=new java.util.HashSet<>(java.util.Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(open);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until) {
            JDialog[] found={null};
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&!before.contains(w)&&(called.equals(d.getName())||called.equals(d.getTitle())))found[0]=d;});
            if(found[0]!=null){Thread.sleep(300);return found[0];}
            Thread.sleep(50);
        }
        throw new AssertionError("no box "+called);
    }
    private static String text(Container in) {
        StringBuilder all=new StringBuilder();
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t)all.append(t.getText()).append('\n');if(c instanceof Container k)all.append(text(k));}
        return all.toString();
    }
    private static AbstractButton button(Container in,String words) {
        AbstractButton found=buttonIn(in,words);if(found==null)throw new AssertionError("no button "+words);return found;
    }
    private static AbstractButton buttonIn(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof AbstractButton b&&words.equals(b.getText()))return b;if(c instanceof Container k){AbstractButton f=buttonIn(k,words);if(f!=null)return f;}}
        return null;
    }
    private static JTextField field(Container in) {
        for(Component c:in.getComponents()){if(c instanceof JTextField f)return f;if(c instanceof Container k){JTextField f=field(k);if(f!=null)return f;}}
        return null;
    }
    private static Component named(Container in,String name) {
        for(Component c:in.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name);if(f!=null)return f;}}
        return null;
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
