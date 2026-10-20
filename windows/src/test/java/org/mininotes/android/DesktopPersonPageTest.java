package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** A person's page opened from their round over a note (the owner, 2026-10-11): People and devices, on them. Synthetic, offline. */
public class DesktopPersonPageTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String ANA="MxPersonPageAnaFixture@127.0.0.1:9861";
    private Desktop pad;

    @Before public void start() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog)w.dispose();pad.shutdown(false);});await(()->!pad.frame.isDisplayable());}}

    @Test public void theirPageOpensOnThem() throws Exception {
        org.mininotes.desktop.platform.content.Context anaHome=new org.mininotes.desktop.platform.content.Context(temp.newFolder("ana"));
        Keys ana=new Keys(anaHome);
        pad.store.pairedWith(ANA,"Ana",false,ana.agreement().getPublic().getEncoded(),ana.signing().getPublic().getEncoded());
        SwingUtilities.invokeAndWait(()->DesktopPeople.person(pad,ANA,"Ana"));
        JDialog[] box={null};
        await(()->{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing())box[0]=d;});return box[0]!=null;});
        Thread.sleep(500);
        java.util.List<String> said=new java.util.ArrayList<>();
        SwingUtilities.invokeAndWait(()->texts(box[0].getContentPane(),said));
        assertTrue("her page: "+said,said.stream().anyMatch(s->s.contains("Ana")));
        assertTrue("with the way to her Parlons! address: "+said,said.stream().anyMatch(s->s.contains("Parlons!")));
        SwingUtilities.invokeAndWait(()->{try{
            JRootPane root=box[0].getRootPane();BufferedImage shot=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=shot.createGraphics();root.paint(g);g.dispose();
            Path out=Paths.get("build","verification");Files.createDirectories(out);javax.imageio.ImageIO.write(shot,"png",out.resolve("person-page-from-round.png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }

    private static void texts(Container in,java.util.List<String> out) {
        for(Component c:in.getComponents()) {
            if(c instanceof JLabel l&&l.getText()!=null)out.add(l.getText());
            else if(c instanceof AbstractButton b&&b.getText()!=null)out.add(b.getText());
            else if(c instanceof javax.swing.text.JTextComponent t)out.add(t.getText());
            if(c instanceof Container k)texts(k,out);
        }
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
