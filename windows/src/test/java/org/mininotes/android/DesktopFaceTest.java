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
 * Your picture in Profile, and a contact's on their page, drawn on their rounds (decision 158): pictures drawn to
 * build/verification/gallery to be looked at. Synthetic: made-up pictures, a made-up contact, offline.
 */
public class DesktopFaceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    private static byte[] png(Color colour) throws Exception {
        BufferedImage img=new BufferedImage(96,96,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();g.setColor(colour);g.fillRect(0,0,96,96);g.setColor(Color.WHITE);g.fillOval(28,18,40,40);g.fillOval(14,60,68,60);g.dispose();
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(img,"png",out);return out.toByteArray();
    }

    @Test public void yourPictureAndTheirsAreOnTheirRounds() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();app[0].frame.setSize(1100,760);}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            Keys ana=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder("ana")));
            String at="MxFaceShotAna@127.0.0.1:9871";
            pad.store.pairedWith(at,"Ana",false,ana.agreement().getPublic().getEncoded(),ana.signing().getPublic().getEncoded());
            pad.store.setMyFace(png(new Color(60,120,90)));
            pad.store.setFaceHere(at,png(new Color(170,80,60)));
            DesktopFaces.forgetAll();
            assertNotNull(DesktopFaces.picture(pad.store.faceOf(at)));
            SwingUtilities.invokeAndWait(()->pad.profile(pad.frame,null));
            Thread.sleep(1500);
            assertNotNull("yours is kept",DesktopFaces.picture(pad.store.faceOf(null)));
            await(()->DesktopFaces.of(null)!=null);Thread.sleep(300);
            shoot(dialog("Profile"),"158a-profile-picture");
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing())d.dispose();});
            SwingUtilities.invokeAndWait(()->DesktopPeople.person(pad,at,"Ana"));
            Window page=null;
            for(long until=System.currentTimeMillis()+15000;page==null&&System.currentTimeMillis()<until;Thread.sleep(200))
                for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing())page=d;
            if(page==null)for(Window w:Window.getWindows())System.err.println("window "+w.getClass().getSimpleName()+" showing="+w.isShowing()+" "+(w instanceof Dialog d?d.getTitle():""));
            assertNotNull("their page",page);
            shoot(page,"158b-person-picture");
            JComponent big=DesktopFaces.big(DesktopFaces.picture(pad.store.faceOf(at)),200);
            big.setSize(200,200);BufferedImage img=new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,200,200);big.paint(g);g.dispose();
            javax.imageio.ImageIO.write(img,"png",SHOTS.resolve("158c-big-picture.png").toFile());
        } finally {SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d)d.dispose();pad.shutdown(false);});await(()->!pad.frame.isDisplayable());}
    }

    private static Window dialog(String title) {
        for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&title.equals(d.getTitle()))return d;
        for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing())return d;
        throw new AssertionError("no "+title);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
