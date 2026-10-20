package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Favourites in Show on Home, in the menu pinned beside Recent (the owner, 2026-10-14: "the favorites display is reversed,
 * when selected it disappears"): its tick says whether the Favourites icon is on Home, and a click switches both. Synthetic.
 */
public class DesktopFavouritesSwitchTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void theTickAndTheIconAgree() throws Exception {
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();app[0].frame.setSize(1300,800);}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Synthetic";note.body="x";pad.store.save(note);
            pad.store.keepToHand(NoteStore.Branch.Kind.PAGE,note.id,true);
            pad.context.getSharedPreferences("settings",0).edit().putString(DesktopPinnedMenu.SETTING,"true").apply();
            SwingUtilities.invokeAndWait(()->{pad.showHome();pad.pinnedMenu.place();pad.refresh();});
            await(()->pad.pinnedMenu.isShowing());Thread.sleep(1200);
            for(int round=0;round<3;round++) {
                boolean on=pad.onHome(NoteStore.FAVOURITES);
                boolean[] ticked={false},icon={false};
                SwingUtilities.invokeAndWait(()->{ticked[0]=ticked(row(pad.pinnedMenu,"Favourites"));icon[0]=favouritesOnHome(pad);});
                assertEquals("round "+round+": the tick says the switch",on,ticked[0]);
                assertEquals("round "+round+": the icon follows the switch",on,icon[0]);
                SwingUtilities.invokeAndWait(()->{JMenuItem r=row(pad.pinnedMenu,"Favourites");
                    r.dispatchEvent(new MouseEvent(r,MouseEvent.MOUSE_ENTERED,System.currentTimeMillis(),0,10,5,0,false,MouseEvent.NOBUTTON));
                    r.dispatchEvent(new MouseEvent(r,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),MouseEvent.BUTTON1_DOWN_MASK,10,5,1,false,MouseEvent.BUTTON1));
                    r.dispatchEvent(new MouseEvent(r,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,10,5,1,false,MouseEvent.BUTTON1));
                    r.dispatchEvent(new MouseEvent(r,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,10,5,1,false,MouseEvent.BUTTON1));});
                Thread.sleep(1500);
                assertEquals("round "+round+": one click switches it once",!on,pad.onHome(NoteStore.FAVOURITES));
            }
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    private static boolean favouritesOnHome(Desktop pad) {
        for(DesktopHome.Tile one:pad.home.grid.tiles)if(one.thing.kind==NoteStore.Branch.Kind.FAVOURITES)return true;
        return false;
    }
    /** Whether a ticked row draws its tick: read from its accessible name, which says on or off. */
    private static boolean ticked(JMenuItem row) {
        assertNotNull("Favourites is in the pinned menu",row);
        String said=row.getAccessibleContext().getAccessibleName();return said!=null&&said.endsWith(", on");
    }
    private static JMenuItem row(Container in,String text) {
        for(Component c:in.getComponents()){if(c instanceof JMenuItem m&&!(c instanceof JMenu)&&text.equals(m.getText()))return m;if(c instanceof Container k){JMenuItem f=row(k,text);if(f!=null)return f;}}
        return null;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
