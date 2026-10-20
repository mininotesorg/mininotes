package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * The unlock box shows every button whole, in every language (the owner, 2026-10-13: "please make sure the login window
 * always displays all buttons in full"): in French, Windows Hello cancelled, the two buttons under it wrapped onto two
 * lines and the second was cut off. Drawn from the box itself to a picture in build/verification/gallery, never from the screen.
 */
public class DesktopLockFitTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void everyButtonIsWholeWhenHelloIsCancelled() throws Exception {
        for(String language:Say.LANGUAGES.keySet())check(language);
    }

    private void check(String language) throws Exception {
        Files.createDirectories(SHOTS);
        Path locked=temp.newFolder("locked-"+language).toPath();Vault.Made made=Vault.make("password1".toCharArray(),20_000);
        Files.write(locked.resolve(DesktopLock.KEPT),made.kept);Files.write(locked.resolve(DesktopHello.FILE),new byte[]{1});
        DesktopLock.HelloAsk helloWas=DesktopLock.helloAsk;DesktopLock.Front frontWas=DesktopLock.front;
        DesktopLock.helloAsk=f->{throw new java.io.IOException("Windows Hello was cancelled.");};
        DesktopLock.front=new DesktopLock.Front(){public boolean mayTake(){return true;}public boolean holds(){return true;}};
        Say.use(language);
        List<String> cut=new ArrayList<>();
        try {
            SwingUtilities.invokeLater(()->{try{DesktopLock.askAtStart(locked,true);}catch(Exception e){throw new RuntimeException(e);}});
            JDialog[] box={null};
            for(int i=0;i<200&&box[0]==null;i++){Thread.sleep(50);SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getContentPane().getComponentCount()>0&&named(d,"unlockHello")!=null)box[0]=d;});}
            assertNotNull("the unlock box came up in "+language,box[0]);
            // Hello asked and cancelled: the longer line under the buttons is there.
            for(int i=0;i<100;i++){Thread.sleep(50);String[] said={""};SwingUtilities.invokeAndWait(()->{Component c=named(box[0],"unlockSaid");said[0]=c instanceof JTextArea t?t.getText():"";});if(said[0].length()>30)break;}
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{
                Container content=box[0].getContentPane();
                for(JButton b:buttons(content))if(b.isShowing()) {
                    Rectangle at=SwingUtilities.convertRectangle(b.getParent(),b.getBounds(),content);
                    if(!new Rectangle(0,0,content.getWidth(),content.getHeight()).contains(at))cut.add(b.getText()+" at "+at+" in "+content.getSize());
                }
                // And the line under them, whole: not wider than the box, so no word of it is cut off at the edge.
                Component said=named(content,"unlockSaid");
                Rectangle line=SwingUtilities.convertRectangle(said.getParent(),said.getBounds(),content);
                if(line.x+line.width>content.getWidth()-8)cut.add("the line under the buttons, "+line+" in "+content.getSize());
                JRootPane root=box[0].getRootPane();
                // At the screen's own scale: the words were laid out at it, and drawn at another they run past their lines.
                var scale=box[0].getGraphicsConfiguration().getDefaultTransform();
                BufferedImage img=new BufferedImage((int)Math.ceil(root.getWidth()*scale.getScaleX()),(int)Math.ceil(root.getHeight()*scale.getScaleY()),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=img.createGraphics();g.transform(scale);root.paint(g);g.dispose();
                try{javax.imageio.ImageIO.write(img,"png",SHOTS.resolve("64-locked-hello-cancelled-"+language+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}
                box[0].dispose();
            });
            assertEquals("buttons cut off in "+language,List.of(),cut);
        } finally {DesktopLock.helloAsk=helloWas;DesktopLock.front=frontWas;Say.use("en");}
    }

    private static List<JButton> buttons(Container in) {
        List<JButton> out=new ArrayList<>();
        for(Component c:in.getComponents()){if(c instanceof JButton b)out.add(b);if(c instanceof Container k)out.addAll(buttons(k));}
        return out;
    }
    private static Component named(Container in,String name) {
        for(Component c:in.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name);if(f!=null)return f;}}
        return null;
    }
}
