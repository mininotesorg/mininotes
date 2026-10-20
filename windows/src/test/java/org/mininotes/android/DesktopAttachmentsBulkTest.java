package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * The paperclip's menu (decision 196): Take a photo beside Attach and Record; the note's files each ticked or not, the menu
 * open while they are; the ticked ones saved to a folder at once, or moved to the bin. Made-up files; drawn from the window.
 */
public class DesktopAttachmentsBulkTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private Desktop pad;

    @Before public void start() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isDisplayable())w.dispose();});
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void tickedFilesAreSavedTogetherAndBinnedTogether() throws Exception {
        NoteStore.Note n=new NoteStore.Note();n.title="Contracts";n.body="Synthetic contracts";n.book=Things.HOME;pad.store.save(n);
        List<NoteStore.Held> files=new ArrayList<>();
        for(String name:new String[]{"agreement.pdf","letter.docx","articles.pdf"}) {
            NoteStore.Held f=pad.store.opening(NoteStore.Branch.Kind.PAGE,n.id,name,"application/octet-stream",4);
            Files.write(pad.store.fileFor(f.id).toPath(),name.getBytes());pad.store.keep(f);files.add(f);
        }
        // One of them stamped on Minima, confirmed in a block (made-up numbers), for its line in the list (decision 224).
        long now=System.currentTimeMillis();
        long stamp=pad.store.stamped(files.get(1).id,"file","letter.docx",Integritas.hash("letter.docx".getBytes()),now,"r","","",null,null);
        pad.store.stampLanded(stamp,2374505,"0x00AB","0xCD",java.time.Instant.now().toString(),1,now);
        SwingUtilities.invokeAndWait(()->pad.open(n.id));
        await(()->onEdt(()->pad.noteShown()!=null&&pad.noteShown().equals(n.id)));Thread.sleep(400);
        SwingUtilities.invokeAndWait(()->pad.clip.doClick());
        await(()->onEdt(()->menu()!=null));
        JPopupMenu menu=onEdt(this::menu);
        // Take a photo is offered, the files are ticks, and the actions wait for a tick.
        assertNotNull(onEdt(()->item(menu,"Take a photo…")));
        // The stamped file's line says so, as its card does; the others do not.
        JMenuItem letter=onEdt(()->startsWith(menu,"letter.docx"));
        assertTrue("said: "+onEdt(letter::getText),onEdt(letter::getText).endsWith(Integritas.noteLine(pad.store.stampsOf(files.get(1).id).get(0),false)));
        assertTrue(onEdt(letter::getText).contains("2374505"));
        assertFalse(onEdt(()->startsWith(menu,"agreement.pdf").getText()).contains("Minima"));
        JMenuItem bin=onEdt(()->startsWith(menu,"Move to bin"));
        assertFalse("nothing ticked: nothing to do",onEdt(bin::isEnabled));
        SwingUtilities.invokeAndWait(()->{int ticked=0;for(Component c:menu.getComponents())if(c instanceof JCheckBoxMenuItem t&&t.getText().contains("·")&&ticked<2){t.doClick();ticked++;}});
        assertTrue("two ticked",onEdt(()->menu.isVisible()));
        assertEquals("Move to bin  (2)",onEdt(bin::getText));
        shoot(menu,"96-attachments-ticked");
        // Saved together into a folder, a number given to a name already there.
        Path out=temp.newFolder("C-drive").toPath();Files.writeString(out.resolve("agreement.pdf"),"already here");
        SwingUtilities.invokeAndWait(()->pad.saveAll(files.subList(0,2),out));
        await(()->Files.exists(out.resolve("agreement (2).pdf"))&&Files.exists(out.resolve("letter.docx")));
        assertEquals("agreement.pdf",Files.readString(out.resolve("agreement (2).pdf")));
        // Copied without opening it (decision 198): a picture as a picture and as a file.
        java.awt.image.BufferedImage dot=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(dot,"png",png);
        NoteStore.Held picture=pad.store.opening(NoteStore.Branch.Kind.PAGE,n.id,"photo.png","image/png",png.size());
        Files.write(pad.store.fileFor(picture.id).toPath(),png.toByteArray());pad.store.keep(picture);
        SwingUtilities.invokeAndWait(()->pad.copyFile(picture));
        for(int i=0;i<150&&!onEdt(()->pad.status.getText().contains("photo.png"));i++)Thread.sleep(100);
        assertTrue("said: "+onEdt(()->pad.status.getText()),onEdt(()->pad.status.getText().contains("photo.png")));
        java.awt.datatransfer.Clipboard board=Toolkit.getDefaultToolkit().getSystemClipboard();
        assertTrue("a picture",board.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor));
        assertTrue("and a file",board.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.javaFileListFlavor));
        // And the two ticked into the bin, out of the note.
        SwingUtilities.invokeAndWait(bin::doClick);
        await(()->pad.store.notAway(pad.store.filesOf(NoteStore.Branch.Kind.PAGE,n.id)).size()==2);
    }

    private JPopupMenu menu() {
        for(Window w:Window.getWindows())if(w.isShowing())for(JPopupMenu m:find(w))if(m.isShowing())return m;
        MenuElement[] open=MenuSelectionManager.defaultManager().getSelectedPath();
        return open.length>0&&open[0] instanceof JPopupMenu m?m:null;
    }
    private static List<JPopupMenu> find(Container in) {
        List<JPopupMenu> out=new ArrayList<>();
        for(Component c:in.getComponents()){if(c instanceof JPopupMenu m)out.add(m);if(c instanceof Container k)out.addAll(find(k));}
        return out;
    }
    private static JMenuItem item(JPopupMenu menu,String text){for(Component c:menu.getComponents())if(c instanceof JMenuItem i&&text.equals(i.getText()))return i;return null;}
    private static JMenuItem startsWith(JPopupMenu menu,String text){for(Component c:menu.getComponents())if(c instanceof JMenuItem i&&i.getText().startsWith(text))return i;return null;}
    private static void shoot(JComponent c,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var image=new java.awt.image.BufferedImage(Math.max(1,c.getWidth()),Math.max(1,c.getHeight()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();c.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
