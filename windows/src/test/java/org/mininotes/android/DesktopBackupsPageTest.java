package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** The Backup page and its box drawn, as the owner will see them (decision 329), into build/verification/backups. Made-up only. */
public class DesktopBackupsPageTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;
    @Before public void start() throws Exception {
        Path folder=temp.newFolder().toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        long until=System.currentTimeMillis()+30_000;while(pad.store.latest()==null&&System.currentTimeMillis()<until)Thread.sleep(100);
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable())SwingUtilities.invokeAndWait(()->pad.shutdown(false));}

    private static void shoot(JDialog dialog,String name) throws Exception {
        var scale=dialog.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(dialog.getWidth()*scale.getScaleX()),(int)Math.ceil(dialog.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);dialog.paint(g);g.dispose();
        Path path=Path.of("build","verification","backups",name+".png");Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"png",path.toFile());
    }
    private void capture(String title,String name,Runnable opening) throws Exception {
        CountDownLatch seen=new CountDownLatch(1);
        SwingUtilities.invokeLater(()->{
            javax.swing.Timer look=new javax.swing.Timer(150,e->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals(Say.t(title))){
                ((javax.swing.Timer)e.getSource()).stop();
                try{Thread.sleep(200);shoot(d,name);}catch(Exception x){throw new RuntimeException(x);}finally{d.dispose();seen.countDown();}
            }});look.start();opening.run();
        });
        assertTrue(name,seen.await(20,TimeUnit.SECONDS));
    }

    @Test public void thePageAndItsBoxAreDrawn() throws Exception {
        Backups.Place p=new Backups.Place();p.where="D:\\Synthetic backups";p.shown=p.where;p.often=Backups.Often.WEEKLY;p.weekday=1;p.hour=20;
        p.last=System.currentTimeMillis()-86_400_000L;p.said=Backups.Said.MADE;
        Backups.Place q=new Backups.Place();q.where="E:\\Gone";q.shown=q.where;q.often=Backups.Often.ON_CLOSE;q.what=Backups.What.NO_FILES;q.opens=Backups.Opens.WORDS;q.said=Backups.Said.NO_FOLDER;q.keep=10;
        // A made-up server of the owner's, paired (decision 330).
        java.security.KeyPair a=Envelope.keys(),s=Envelope.keys();
        pad.store.pairedWith("Mx-synthetic-server","Backup server",false,Point.shorten(a.getPublic()),Point.shorten(s.getPublic()));
        pad.store.setMine("Mx-synthetic-server",true);
        Backups.Place r=new Backups.Place();r.device="Mx-synthetic-server";r.shown="Backup server";r.often=Backups.Often.DAILY;r.hour=3;r.keep=10;
        r.last=System.currentTimeMillis()-3_600_000L;r.said=Backups.Said.MADE;
        DesktopBackups.keep(pad,java.util.List.of(p,q,r));
        capture("Backup","page",()->DesktopBackups.open(pad));
        capture("Change this backup","change",()->DesktopBackups.edit(pad,pad.frame,p,null));
        capture("Change this backup","change-device",()->DesktopBackups.edit(pad,pad.frame,r,null));
        capture("Add a place for backups","add",()->DesktopBackups.edit(pad,pad.frame,null,null));
        capture("Your own server","server",()->DesktopBackups.ownServer(pad,pad.frame));
    }
}
