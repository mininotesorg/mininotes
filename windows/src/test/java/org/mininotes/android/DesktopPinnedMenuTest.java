package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * The owner's asks of 2026-10-13 on the menu: its sections' titles in the language chosen; a line of Show on Home switched
 * without the menu closing; and the menu pinned open on the right of Recent, from Show on Home. Synthetic, offline; pictures
 * drawn from the window itself at the screen's scale, in build/verification/gallery.
 */
public class DesktopPinnedMenuTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void theMenuInFrenchStaysOpenAndPinsBesideRecent() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();app[0].frame.setSize(1300,800);}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            Say.use("fr");
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Courses";note.body="pain";pad.store.save(note);
            SwingUtilities.invokeAndWait(()->{pad.open(note.id);});
            await(()->note.id.equals(pad.noteShown()));
            SwingUtilities.invokeAndWait(pad::showHome);Thread.sleep(500);
            // The sections' titles, in French.
            List<String> titles=new ArrayList<>();
            JPopupMenu[] menu={null};
            SwingUtilities.invokeAndWait(()->{menu[0]=pad.mainMenu();for(Component c:menu[0].getComponents())if(c instanceof JLabel l)titles.add(l.getText());});
            assertTrue(titles.toString(),titles.contains("CRÉER"));assertTrue(titles.toString(),titles.contains("AFFICHER SUR L'ACCUEIL"));
            assertFalse(titles.toString(),titles.contains("CREATE"));
            // A line of Show on Home clicked: switched, and the menu still open for the next one.
            boolean before=pad.onHome(NoteStore.FAVOURITES);
            SwingUtilities.invokeAndWait(()->menu[0].show(pad.frame.getContentPane(),300,60));
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{
                JMenuItem row=row(menu[0],"Favoris");assertNotNull(row);
                row.dispatchEvent(new MouseEvent(row,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),MouseEvent.BUTTON1_DOWN_MASK,10,5,1,false,MouseEvent.BUTTON1));
                row.dispatchEvent(new MouseEvent(row,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,10,5,1,false,MouseEvent.BUTTON1));
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->assertTrue("the menu stays open",menu[0].isVisible()));
            assertEquals(!before,pad.onHome(NoteStore.FAVOURITES));
            SwingUtilities.invokeAndWait(()->menu[0].setVisible(false));
            // Pinned, from its line in Show on Home: the menu open down the right, beside Recent.
            JPopupMenu[] again={null};
            SwingUtilities.invokeAndWait(()->{again[0]=pad.mainMenu();again[0].show(pad.frame.getContentPane(),300,60);});Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{JMenuItem pin=row(again[0],"Menu");assertNotNull("Menu is in Show on Home",pin);
                pin.dispatchEvent(new MouseEvent(pin,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,10,5,1,false,MouseEvent.BUTTON1));});
            SwingUtilities.invokeAndWait(()->again[0].setVisible(false));
            await(()->pad.pinnedMenu.isShowing());
            Thread.sleep(600);
            SwingUtilities.invokeAndWait(()->{
                assertTrue(DesktopPinnedMenu.wanted(pad));
                Rectangle recent=SwingUtilities.convertRectangle(pad.openList.getParent(),pad.openList.getBounds(),pad.frame.getContentPane());
                Rectangle pinned=SwingUtilities.convertRectangle(pad.pinnedMenu.getParent(),pad.pinnedMenu.getBounds(),pad.frame.getContentPane());
                assertTrue("on the right of Recent: "+pinned+" / "+recent,pinned.x>=recent.x+recent.width-2);
                assertNotNull("it holds the menu's lines",row(pad.pinnedMenu,"Rechercher…"));
            });
            shoot(pad,"166a-menu-pinned-beside-recent-french");
            // Opening a note: the pinned menu now offers that note's lines.
            SwingUtilities.invokeAndWait(()->pad.open(note.id));
            await(()->note.id.equals(pad.noteShown()));Thread.sleep(600);
            SwingUtilities.invokeAndWait(()->assertNotNull("the open note's lines",row(pad.pinnedMenu,"Versions…")));
            shoot(pad,"166b-menu-pinned-note-open");
        } finally {Say.use("en");SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    private static JMenuItem row(Container in,String text) {
        for(Component c:in.getComponents()){if(c instanceof JMenuItem m&&!(c instanceof JMenu)&&text.equals(m.getText()))return m;if(c instanceof Container k){JMenuItem f=row(k,text);if(f!=null)return f;}}
        return null;
    }
    private static void shoot(Desktop pad,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            JRootPane root=pad.frame.getRootPane();root.validate();
            var scale=root.getGraphicsConfiguration().getDefaultTransform();
            BufferedImage img=new BufferedImage((int)Math.ceil(root.getWidth()*scale.getScaleX()),(int)Math.ceil(root.getHeight()*scale.getScaleY()),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();g.transform(scale);root.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
