package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Every menu line that does something wears its symbol, the same as on the phone (decision 157): the note's menu and
 * Temp's, drawn to pictures in build/verification/gallery to be looked at. Synthetic, offline.
 */
public class DesktopMenuSymbolsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void menuLinesWearTheirSymbols() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Groceries";pad.store.save(note);
            int[] withSymbol={0},lines={0};
            SwingUtilities.invokeAndWait(()->{
                NoteStore.Branch it=null;for(NoteStore.Branch one:pad.store.contents(Things.HOME))if(one.id.equals(note.id))it=one;
                JPopupMenu thing=pad.thingMenu(it);
                for(Component c:thing.getComponents())if(c instanceof JMenuItem m&&!(m instanceof JMenu)){lines[0]++;if(m.getIcon()!=null)withSymbol[0]++;}
                shoot(thing,"157a-note-menu-symbols");
                JPopupMenu tempMenu=pad.home.menuFor(DesktopHome.tempPlace(0));
                shoot(tempMenu,"157b-temp-menu-symbols");
            });
            assertTrue("most lines wear a symbol: "+withSymbol[0]+" of "+lines[0],withSymbol[0]*2>lines[0]);
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    /** The same menu, and Settings, in French (decision 160): drawn to look at. */
    @Test public void inFrench() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad-fr").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();app[0].frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            Say.use("fr");
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Courses";pad.store.save(note);
            String[] first={null};
            SwingUtilities.invokeAndWait(()->{
                NoteStore.Branch it=null;for(NoteStore.Branch one:pad.store.contents(Things.HOME))if(one.id.equals(note.id))it=one;
                JPopupMenu thing=pad.thingMenu(it);
                for(java.awt.Component c:thing.getComponents())if(c instanceof JMenuItem m&&!(m instanceof JMenu)&&first[0]==null)first[0]=m.getText();
                shoot(thing,"160a-note-menu-french");
            });
            // Settings is a box that waits for an answer: opened later, not waited for, or the test waits with it.
            SwingUtilities.invokeLater(()->DesktopSettings.open(pad));
            Window[] open={null};
            await(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing())open[0]=d;return open[0]!=null;});
            Thread.sleep(800);
            Window settings=open[0];
            Window shown=settings;
            SwingUtilities.invokeAndWait(()->{BufferedImage img=new BufferedImage(shown.getWidth(),shown.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=img.createGraphics();shown.paint(g);g.dispose();
                try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve("160b-settings-french.png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            assertEquals("Couleur",Say.t("Colour"));
        } finally {Say.use("en");SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d)d.dispose();pad.shutdown(false);});await(()->!pad.frame.isDisplayable());}
    }

    private static void shoot(JPopupMenu menu,String name) {
        Dimension size=menu.getPreferredSize();menu.setSize(size);
        layout(menu);
        BufferedImage img=new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,size.width,size.height);menu.printAll(g);g.dispose();
        try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void layout(Container c){c.doLayout();for(Component one:c.getComponents())if(one instanceof Container k)layout(k);}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
