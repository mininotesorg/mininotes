package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Files let go from Explorer on Temp's open card (the owner, 2026-10-28: "for now they fall on the home page when the temps
 * folder is open"; decision 266): wherever on the card, they are aimed at Temp, as Windows hands them to the component under
 * the pointer that takes drops. A made-up notebook, drawn from the window.
 */
public class DesktopTempDropTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;

    @Before public void start() throws Exception {
        java.nio.file.Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void everywhereOnTempsOpenCardIsTemp() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});cardUp();
        List<String> wrong=new ArrayList<>();
        SwingUtilities.invokeAndWait(()->{
            JComponent card=pad.home.folder;
            Rectangle r=card.getVisibleRect();
            java.awt.Point[] spots={new java.awt.Point(r.x+r.width/2,r.y+r.height/2),new java.awt.Point(r.x+r.width/2,r.y+r.height-30),new java.awt.Point(r.x+30,r.y+r.height-30),
                new java.awt.Point(r.x+r.width/2,r.y+25),new java.awt.Point(r.x+r.width-40,r.y+r.height/3)};
            for(java.awt.Point spot:spots) {
                java.awt.Point inRoot=SwingUtilities.convertPoint(card,spot,pad.frame.getRootPane());
                Component under=SwingUtilities.getDeepestComponentAt(pad.frame.getRootPane(),inRoot.x,inRoot.y);
                // Windows hands the drop to the nearest component, from the one under the pointer up, that takes drops.
                Component takes=under;while(takes!=null&&takes.getDropTarget()==null)takes=takes.getParent();
                String said=spot+" over "+(under==null?"nothing":under.getClass().getSimpleName())+", taken by "+(takes==null?"nothing":takes.getClass().getSimpleName());
                if(takes==null){wrong.add(said+": nothing takes it");continue;}
                TransferHandler handler=takes instanceof JComponent j?j.getTransferHandler():pad.frame.getTransferHandler();
                if(!(handler instanceof DesktopDrops.Aimed)){wrong.add(said+": its handler is "+(handler==null?"none":handler.getClass().getName()));continue;}
                DesktopDrops.Aim aim=DesktopDrops.aim(pad,takes,SwingUtilities.convertPoint(pad.frame.getRootPane(),inRoot,takes));
                if(!(aim.does()==DesktopDrops.KEEP&&NoteStore.TEMP.equals(aim.note())))wrong.add(said+": aimed at "+aim.does()+" "+aim.note()+" ("+aim.name()+")");
            }
        });
        assertTrue(String.join("\n",wrong),wrong.isEmpty());
    }

    /** The card's own round +, pressed as the owner does, then Note: the note is in Temp and, unless Temp shows on Home, not on Home. */
    @Test public void tempsOwnPlusMakesANoteInTemp() throws Exception {
        for(boolean shown:new boolean[]{false,true}) {
            SwingUtilities.invokeAndWait(()->pad.store.setTempOnHome(shown));
            SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});cardUp();
            String before=pad.store.latest().id;
            JButton[] plus={null};
            SwingUtilities.invokeAndWait(()->{Deque<Component> left=new ArrayDeque<>(List.of(pad.home.folder));
                while(!left.isEmpty()){Component c=left.pop();if(c instanceof JButton b&&b.isShowing()&&"New".equals(b.getAccessibleContext().getAccessibleName()))plus[0]=b;if(c instanceof Container k)for(Component in:k.getComponents())left.push(in);}});
            assertNotNull("the card's + is showing",plus[0]);
            SwingUtilities.invokeAndWait(()->plus[0].doClick());Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w.isShowing())for(MenuElement m:MenuSelectionManager.defaultManager().getSelectedPath())if(m instanceof JPopupMenu p){((JMenuItem)p.getComponent(0)).doClick();return;}});
            await(()->pad.store.latest()!=null&&!pad.store.latest().id.equals(before));Thread.sleep(800);
            String id=pad.store.latest().id;
            SwingUtilities.invokeAndWait(()->pad.page.setText("groceries"));Thread.sleep(300);
            SwingUtilities.invokeAndWait(pad::goHome);Thread.sleep(1500);SwingUtilities.invokeAndWait(()->{});
            assertTrue("in Temp (shown on Home: "+shown+")",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0);
            assertEquals("on Home only where Temp shows there",shown,pad.store.contents(Things.HOME).stream().anyMatch(b->b.id.equals(id)));
        }
    }

    /**
     * A folder made in Temp and a note made in it (the owner, 2026-10-28, on the Graphene: "the note was created in the root temp
     * folder, not in the folder"; decision 272): the note is in the folder, temporary with it, and Temp's card shows the folder
     * only. Temp's whole list, by which things are sent and go when their time is up, still has the note.
     */
    @Test public void aNoteInAFolderInTempShowsInTheFolderNotAtTempsTop() throws Exception {
        String folder=pad.store.addCollectionIn(Things.HOME,"Trip").id;
        pad.intoPlaceNow(NoteStore.Branch.Kind.COLLECTION,folder,NoteStore.TEMP);
        NoteStore.Note note=new NoteStore.Note();note.title="Tickets";note.body="Synthetic";note.book=folder;pad.store.save(note);
        long now=System.currentTimeMillis();
        assertTrue("temporary with its folder",pad.store.untilOf(NoteStore.Branch.Kind.PAGE,note.id)>0);
        assertTrue("in Temp's whole list",pad.store.temporary(now).stream().anyMatch(b->b.id.equals(note.id)));
        assertFalse("not at Temp's top",pad.store.temporaryShown(now).stream().anyMatch(b->b.id.equals(note.id)));
        assertTrue("the folder is",pad.store.temporaryShown(now).stream().anyMatch(b->b.id.equals(folder)));
        assertEquals(1,pad.store.temporaryCount());
    }

    /** Temp's card as it is drawn, where each round + is and what it makes (synthetic notebook). */
    @Test public void tempsCardDrawn() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.openPlace(DesktopHome.tempPlace(0));});cardUp();
        String out=System.getenv("MININOTES_PICTURES");
        SwingUtilities.invokeAndWait(()->{
            JRootPane root=pad.frame.getRootPane();
            if(out!=null) {
                java.awt.image.BufferedImage picture=new java.awt.image.BufferedImage(root.getWidth(),root.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D g=picture.createGraphics();root.paint(g);g.dispose();
                try{javax.imageio.ImageIO.write(picture,"png",new java.io.File(out,"266-temp-card.png"));}catch(java.io.IOException e){throw new RuntimeException(e);}
            }
            JComponent card=pad.home.folder;
            System.out.println("card "+SwingUtilities.convertRectangle(card.getParent(),card.getBounds(),root)+" root "+root.getSize());
            List<JButton> pluses=new ArrayList<>();Deque<Component> left=new ArrayDeque<>(List.of(root));
            while(!left.isEmpty()){Component c=left.pop();if(c instanceof JButton b&&"New".equals(b.getAccessibleContext().getAccessibleName()))pluses.add(b);if(c instanceof Container k)for(Component in:k.getComponents())left.push(in);}
            for(JButton plus:pluses)
                System.out.println("+ "+SwingUtilities.convertRectangle(plus.getParent(),plus.getBounds(),root)+" showing "+plus.isShowing()+" in card "+SwingUtilities.isDescendingFrom(plus,card));
        });
    }

    /** Until Temp's card is up and laid out, its + showing: under load a fixed wait was not enough. */
    private void cardUp() throws Exception {
        await(()->{boolean[] up={false};SwingUtilities.invokeAndWait(()->{JComponent card=pad.home.folder;
            if(!card.isShowing()||card.getHeight()<200)return;
            Deque<Component> left=new ArrayDeque<>(List.of(card));
            while(!left.isEmpty()){Component c=left.pop();if(c instanceof JButton b&&b.isShowing()&&b.getWidth()>0&&"New".equals(b.getAccessibleContext().getAccessibleName()))up[0]=true;if(c instanceof Container k)for(Component in:k.getComponents())left.push(in);}});
            return up[0];});
        Thread.sleep(200);SwingUtilities.invokeAndWait(()->{});
    }

    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
