package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Linking my devices with each other, drawn (decision 268): the path's step for a device of mine has Also link it with, each
 * of my other devices switched on and switched off to leave it out; People and devices lists a device of mine another of mine
 * lists, with Link, and somebody paired as a person whom it lists, with This is my device; Temp's Who it goes to has both.
 * Made-up devices only; the window is offline and nothing is sent; every box is closed without its button being pressed.
 */
public class DesktopAlsoLinkTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private static final String PRO_AT="MxAlsoLinkPro@127.0.0.1:9841",WINDOWS_AT="MxAlsoLinkWindows@127.0.0.1:9842",
        SECOND_AT="MxAlsoLinkSecond@127.0.0.1:9843";

    @Test public void theStepsOfLinkingMyDevicesDraw() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("also").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            NoteStore store=pad.store;
            Node.chooseName(pad.context,"Sam");Node.chooseDevice(pad.context,"Study PC");
            Keys pro=new Keys(new Context(temp.newFolder("pro"))),windows=new Keys(new Context(temp.newFolder("windows"))),second=new Keys(new Context(temp.newFolder("second")));
            store.pairedWith(PRO_AT,"Test Pro",true,pro.agreement().getPublic().getEncoded(),pro.signing().getPublic().getEncoded());store.setMine(PRO_AT,true);
            // The other laptop, paired here as a person, never named: "Windows PC", as the owner's Temp box showed it.
            store.pairedWith(WINDOWS_AT,"Windows PC",false,windows.agreement().getPublic().getEncoded(),windows.signing().getPublic().getEncoded());

            // 1. The path for a device of mine: what it gets, and under it Also link it with, the Pro switched on.
            String laptop=pad.keys.line("Sam's laptop","MxAlsoLinkLaptop@127.0.0.1:9844",Sharing.MINE_OFFER,Sharing.Level.READ,Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY);
            Pairing.Said mine=Pairing.read(Pairing.line(Pairing.link(laptop)));
            JDialog path=open(()->DesktopJoining.open(pad,mine,"",null,null));
            try {
                press(path,"Next");
                SwingUtilities.invokeAndWait(()->{
                    assertNotNull("Also link it with",showing(path.getContentPane(),Say.t(Introducing.ALSO_LINK)));
                    JCheckBox on=(JCheckBox)named(path.getContentPane(),"alsoLink "+PRO_AT,JCheckBox.class);
                    assertNotNull("the Pro, a switch",on);assertTrue("switched on at first",on.isSelected());
                    assertNull("never somebody else",named(path.getContentPane(),"alsoLink "+WINDOWS_AT,JCheckBox.class));
                });
                shoot(path,"268-also-link-on-the-path");
                SwingUtilities.invokeAndWait(()->((JCheckBox)named(path.getContentPane(),"alsoLink "+PRO_AT,JCheckBox.class)).doClick(0));
                SwingUtilities.invokeAndWait(()->{
                    try {
                        java.lang.reflect.Field f=DesktopJoining.class.getDeclaredField("choice");f.setAccessible(true);
                        assertEquals("switched off, left out",java.util.Set.of(PRO_AT),((Joining.Choice)f.get(path.getRootPane().getClientProperty("joining"))).skip);
                    } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                });
            } finally {SwingUtilities.invokeAndWait(path::dispose);}
            assertTrue("nothing was kept from the code drawn",store.addresses().stream().noneMatch(c->c.address.contains("MxAlsoLinkLaptop")));

            // 2. A card from the Pro: it counts this PC, the other laptop and a second laptop as mine.
            Persons.Me owner=new Persons.Me(Persons.newId(new java.security.SecureRandom()),1_700_000_000_000L,null,"Sam",0,"Test Pro",0);
            Persons.Card card=Persons.card(owner,device(pro,PRO_AT,"Test Pro"),java.util.List.of(device(pad.keys,"MxAlsoLinkPc@127.0.0.1:9845","Study PC"),
                device(windows,WINDOWS_AT,"Travel laptop"),device(second,SECOND_AT,"Second laptop")));
            Post.arrived(pad.context,store,pad.keys,Envelope.seal(Persons.idBytes(owner.id),card.newest(),System.currentTimeMillis(),Persons.wrap(card),pro.signing(),pad.keys.agreement().getPublic()));
            String secondKey=Persons.key(second.signing().getPublic().getEncoded());
            JDialog people=open(()->pad.people(null));
            try {
                SwingUtilities.invokeAndWait(()->{
                    Container in=people.getContentPane();
                    assertNotNull("a device of mine not linked here, under My devices",named(in,"offered "+secondKey,JPanel.class));
                    assertNotNull("with Link",named(in,"link "+secondKey,JButton.class));
                    assertNotNull("not linked here yet",showing(in,Say.t(Introducing.NOT_LINKED)));
                    assertNotNull("the person who is mine, This is my device",named(in,"thisIsMine "+WINDOWS_AT,JButton.class));
                });
                shoot(people,"268-people-link-and-this-is-mine");
            } finally {SwingUtilities.invokeAndWait(people::dispose);}

            // 3. Temp's Who it goes to: the same two, from there.
            JDialog tempBox=open(pad::chooseTempDevices);
            try {
                SwingUtilities.invokeAndWait(()->{
                    Container in=tempBox.getContentPane();
                    assertNotNull(named(in,"tempLink "+secondKey,JButton.class));
                    assertNotNull(named(in,"tempThisIsMine "+WINDOWS_AT,JButton.class));
                });
                shoot(tempBox,"268-temp-link-and-this-is-mine");
            } finally {SwingUtilities.invokeAndWait(tempBox::dispose);}
            assertFalse("nothing was made mine by drawing it",store.address(WINDOWS_AT).mine);
            assertNull(store.address(SECOND_AT));
        } finally {
            SwingUtilities.invokeAndWait(()->{pad.frame.dispose();});
        }
    }

    private static Persons.Device device(Keys keys,String at,String name) throws Exception {
        return new Persons.Device(Point.shorten(keys.signing().getPublic()),Point.shorten(keys.agreement().getPublic()),at,name,Persons.ACTIVE,1);
    }

    private static void shoot(JDialog box,String name) throws Exception {
        Thread.sleep(400);
        SwingUtilities.invokeAndWait(()->{
            box.validate();
            try {
                var scale=box.getGraphicsConfiguration().getDefaultTransform();
                var image=new java.awt.image.BufferedImage((int)Math.ceil(box.getWidth()*scale.getScaleX()),(int)Math.ceil(box.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();g.transform(scale);box.paint(g);g.dispose();
                javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
            } catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void press(JDialog box,String words) throws Exception {
        SwingUtilities.invokeAndWait(()->{AbstractButton b=calledIn(box.getContentPane(),words);assertNotNull("nothing called "+words,b);b.doClick(0);});
        Thread.sleep(400);SwingUtilities.invokeAndWait(box::validate);
    }
    private static AbstractButton calledIn(Container in,String words) {
        for(Component c:in.getComponents()) {
            if(c instanceof AbstractButton b&&(words.equals(b.getText())||words.equals(b.getAccessibleContext().getAccessibleName())))return b;
            if(c instanceof Container k){AbstractButton f=calledIn(k,words);if(f!=null)return f;}
        }
        return null;
    }
    private static Component named(Container in,String name,Class<?> kind) {
        for(Component c:in.getComponents()){if(kind.isInstance(c)&&name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name,kind);if(f!=null)return f;}}
        return null;
    }
    private static Component showing(Container in,String words) {
        for(Component one:in.getComponents()) {
            if(one instanceof javax.swing.text.JTextComponent l&&words.equals(l.getText()))return one;
            if(one instanceof JLabel l&&words.equals(l.getText()))return one;
            if(one instanceof Container c){Component deeper=showing(c,words);if(deeper!=null)return deeper;}
        }
        return null;
    }
    private static JDialog open(Runnable open) throws Exception {
        java.util.Set<Window> before=new java.util.HashSet<>(java.util.Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(open);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until) {
            JDialog[] found={null};
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&!before.contains(w))found[0]=d;});
            if(found[0]!=null){Thread.sleep(800);SwingUtilities.invokeAndWait(found[0]::validate);Thread.sleep(200);return found[0];}
            Thread.sleep(50);
        }
        throw new AssertionError("The box did not open");
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
