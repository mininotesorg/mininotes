package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/**
 * The steps of connecting with somebody and sharing a first note, drawn one by one for the review in docs/CONNECT-REVIEW.md.
 * Made-up people and notes only. Nothing is sent: the window is made offline, and is let draw a code by turning that off
 * for the drawing alone, with no node started; every box is closed without its button being pressed.
 */
public class DesktopConnectGalleryTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void theStepsOfConnectingDraw() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("connect").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            NoteStore store=pad.store;
            Node.chooseName(pad.context,"Sam");Node.chooseDevice(pad.context,"Study PC");
            store.pairedWith("MxConnectGallery@127.0.0.1:9101","Ana's phone",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            SwingUtilities.invokeAndWait(()->{pad.title.setText("Groceries");pad.page.setText("Bread\nTea\nApples");});
            await(()->pad.store.latest().body.contains("Apples"));
            NoteStore.Note note=store.latest();
            NoteStore.Branch groceries=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,note.id,"","Groceries","",0,0,true);

            store.pairedWith("MxConnectGalleryLaptop@127.0.0.1:9102","Work laptop",true,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            store.setMine("MxConnectGalleryLaptop@127.0.0.1:9102",true);
            store.makeGroup("Friends");store.makeGroup("Family");

            // 1. People and devices (decision 258): one round + at the foot on the right, where Home has it, my code quietly at
            //    the left, and no + by People or by My devices.
            JDialog people=open(pad,()->pad.people(null));
            try {
                SwingUtilities.invokeAndWait(()->{
                    Container in=people.getContentPane();
                    assertNull("no + by People",named(in,"addPerson"));assertNull("no + by My devices",named(in,"addDevice"));
                    JButton plus=named(in,"peoplePlus");assertNotNull("the one round +",plus);
                    assertEquals("Add someone or a device",plus.getAccessibleContext().getAccessibleName());
                    assertNotNull("my code, quietly",named(in,"showMyCode"));
                    // At the foot, on the right, as Home's.
                    java.awt.Point at=SwingUtilities.convertPoint(plus,0,0,people.getRootPane());
                    assertTrue("at the right",at.x+plus.getWidth()>people.getRootPane().getWidth()-80);
                    assertTrue("at the foot",at.y+plus.getHeight()>people.getRootPane().getHeight()-90);
                });
                shoot(people,"connect-01-people-one-plus");
            } finally {SwingUtilities.invokeAndWait(people::dispose);}

            // 2. The scan screen the + opens: the camera (started at once outside the tests), a picture or a link, and my code
            //    instead, quietly.
            dialog(pad,"connect-05-scan",()->DesktopScanner.open(pad.frame,code->{},()->{},null));

            // Codes are drawn only by a window that is not offline; turned off for the drawing, with no node started.
            java.lang.reflect.Field offline=Desktop.class.getDeclaredField("offline");offline.setAccessible(true);
            SwingUtilities.invokeAndWait(()->{try{offline.setBoolean(pad,false);}catch(Exception e){throw new RuntimeException(e);}});
            try {
                // 2. Sharing a note by code: the role asked first, then the code.
                dialog(pad,"connect-02-role-before-code",()->pad.showCode(groceries));
                dialogAfter(pad,"connect-03-code-for-a-note",()->pad.showCode(groceries),"Show my code");
                // 3. My other device: one box, what it gets first, Nothing yet unless everything is chosen, and the code under
                //    it (decision 255).
                dialog(pad,"connect-04-my-device-what-it-gets",()->pad.showCode(Desktop.library()));
            } finally {SwingUtilities.invokeAndWait(()->{try{offline.setBoolean(pad,true);}catch(Exception e){throw new RuntimeException(e);}});}

            // 4. The path after a code is read (decision 258), a step at a time. Drawn from this PC's own keys under made-up
            //    names; closed before Add or Accept, so nothing is kept or sent. A device of mine first: its Add my device code
            //    says what it is, so One of my devices is chosen; then what it gets, Nothing yet first; then done.
            String laptop=pad.keys.line("Sam's laptop","MxConnectLaptop@127.0.0.1:9202",Sharing.MINE_OFFER,Sharing.Level.READ,Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY);
            Pairing.Said mine=Pairing.read(Pairing.line(Pairing.link(laptop)));
            JDialog path=open(pad,()->DesktopJoining.open(pad,mine,"",null,null));
            try {
                SwingUtilities.invokeAndWait(()->assertTrue("the code says it is mine",((JRadioButton)named(path.getContentPane(),"joiningMine",JRadioButton.class)).isSelected()));
                shoot(path,"connect-10-mine-what-is-it");
                press(path,"Next");
                SwingUtilities.invokeAndWait(()->{
                    assertNotNull("no group step for a device of mine: what it gets",showing(path.getContentPane(),"What it gets"));
                    assertTrue("Nothing yet first",calledIn(path.getContentPane(),"Nothing yet").isSelected());
                    assertEquals("Add",named(path.getContentPane(),"joiningNext").getText());
                });
                shoot(path,"connect-11-mine-what-it-gets");
                SwingUtilities.invokeAndWait(()->((DesktopJoining)path.getRootPane().getClientProperty("joining")).endedForPicture(true,Say.t("Waiting for {0} to accept","Sam's laptop"),null));
                shoot(path,"connect-12-mine-done");
            } finally {SwingUtilities.invokeAndWait(path::dispose);}
            // Someone else: a plain code, someone else first; a group; what she gets, notes chosen; done.
            String plainAna=pad.keys.line("Ana","MxConnectAna@127.0.0.1:9201","",Sharing.Level.WRITE,"","");
            Pairing.Said her=Pairing.read(Pairing.line(Pairing.link(plainAna)));
            JDialog other=open(pad,()->DesktopJoining.open(pad,her,"",null,null));
            try {
                SwingUtilities.invokeAndWait(()->assertTrue("someone else first",((JRadioButton)named(other.getContentPane(),"joiningSomeone",JRadioButton.class)).isSelected()));
                shoot(other,"connect-13-someone-what-is-it");
                press(other,"Next");
                SwingUtilities.invokeAndWait(()->{assertNotNull(calledIn(other.getContentPane(),"No group"));assertNotNull(calledIn(other.getContentPane(),"Friends"));});
                shoot(other,"connect-14-someone-group");
                press(other,"Friends");press(other,"Next");
                press(other,"Choose notes or folders");
                SwingUtilities.invokeAndWait(()->assertTrue("Home's things, a switch each",startsNamed(other.getContentPane(),"joiningThing ")));
                shoot(other,"connect-15-someone-what-she-gets");
                SwingUtilities.invokeAndWait(()->((DesktopJoining)other.getRootPane().getClientProperty("joining")).endedForPicture(false,Say.t("Waiting for {0} to accept","Ana"),null));
                shoot(other,"connect-16-someone-done");
            } finally {SwingUtilities.invokeAndWait(other::dispose);}
            // A plain code chosen as one of mine: done says the other device turns My device on too.
            JDialog oneSided=open(pad,()->DesktopJoining.open(pad,her,"",null,null));
            try {
                SwingUtilities.invokeAndWait(()->((DesktopJoining)oneSided.getRootPane().getClientProperty("joining")).endedForPicture(true,Say.t("Waiting for {0} to accept","Ana"),Say.t("On {0}, turn My device on too.","Ana")));
                shoot(oneSided,"connect-17-plain-as-mine-done");
            } finally {SwingUtilities.invokeAndWait(oneSided::dispose);}

            // 5. The same path for what the scanner reads: a plain code pasted, then a code that shares a note, each read as
            //    the scanner reads it, closed at its first step.
            String plain=pad.keys.line("Ana","MxConnectAna@127.0.0.1:9201",
                "",Sharing.Level.WRITE,"","");
            String offer=pad.keys.line("Ana","MxConnectAna@127.0.0.1:9201",
                Sharing.travelling(Sharing.Scope.PAGE,"Groceries"),Sharing.Level.WRITE,Sharing.Scope.PAGE.name(),"made-up-note");
            java.lang.reflect.Method receive=Desktop.class.getDeclaredMethod("receiveCode",DesktopScanner.Code.class,NoteStore.Branch.class);
            receive.setAccessible(true);
            dialog(pad,"connect-06-pair-pasted",()->call(receive,pad,new DesktopScanner.Code(Pairing.link(plain),false)));
            dialog(pad,"connect-07-pair-scanned",()->call(receive,pad,new DesktopScanner.Code(Pairing.link(plain),true)));
            dialog(pad,"connect-08-accept-scanned",()->call(receive,pad,new DesktopScanner.Code(Pairing.link(offer),true)));
            assertTrue("nothing was kept from the codes drawn",store.addresses().stream().noneMatch(c->c.address.contains("MxConnectAna")));

            // 5. Profile, turned to its code: where Show my code… leads.
            turnTo("Profile","Your code");
            dialog(pad,"connect-09-profile-code",()->pad.profile(pad.frame,null));
        } finally {
            SwingUtilities.invokeAndWait(()->{pad.frame.dispose();});
        }
    }

    /** A box already open, pictured as it is now. */
    private static void shoot(JDialog box,String name) throws Exception {
        Thread.sleep(400);SwingUtilities.invokeAndWait(()->{box.validate();try{shootNow(box,name);}catch(Exception e){throw new RuntimeException(e);}});
    }
    /** The button or choice with these words pressed in an open box, and the box let draw again. */
    private static void press(JDialog box,String words) throws Exception {
        SwingUtilities.invokeAndWait(()->{AbstractButton b=calledIn(box.getContentPane(),words);assertNotNull("nothing called "+words,b);b.doClick(0);});
        Thread.sleep(400);SwingUtilities.invokeAndWait(box::validate);
    }
    private static Component named(Container in,String name,Class<?> kind) {
        for(Component c:in.getComponents()){if(kind.isInstance(c)&&name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name,kind);if(f!=null)return f;}}
        return null;
    }
    private static boolean startsNamed(Container in,String start) {
        for(Component c:in.getComponents()){if(c.getName()!=null&&c.getName().startsWith(start))return true;if(c instanceof Container k&&startsNamed(k,start))return true;}
        return false;
    }
    private static void call(java.lang.reflect.Method m,Desktop pad,DesktopScanner.Code code) {
        try{m.invoke(pad,code,null);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static JDialog open(Desktop pad,Runnable open) throws Exception {
        java.util.Set<Window> before=new java.util.HashSet<>(java.util.Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(open);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until) {
            JDialog[] found={null};
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&!before.contains(w))found[0]=d;});
            if(found[0]!=null){Thread.sleep(600);SwingUtilities.invokeAndWait(found[0]::validate);Thread.sleep(200);return found[0];}
            Thread.sleep(50);
        }
        throw new AssertionError("The box did not open");
    }
    /** Turns the next box with this title to the words, once it is up, for a picture of a part further down. */
    private static void turnTo(String title,String words) {
        new javax.swing.Timer(300,e->{((javax.swing.Timer)e.getSource()).stop();
            for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&title.equals(d.getTitle())){
                Component heading=showing(d.getContentPane(),words);
                JScrollPane pane=heading==null?null:(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,heading);
                if(pane!=null)pane.getVerticalScrollBar().setValue(SwingUtilities.convertPoint(heading,0,0,pane.getViewport().getView()).y-8);}
        }).start();
    }
    private static Component showing(Container in,String words) {
        for(Component one:in.getComponents()) {
            if(one instanceof javax.swing.text.JTextComponent l&&words.equals(l.getText()))return one;
            if(one instanceof JLabel l&&words.equals(l.getText()))return one;
            if(one instanceof Container c){Component deeper=showing(c,words);if(deeper!=null)return deeper;}
        }
        return null;
    }
    /** Opens a window, waits for it to be drawn, keeps a picture of it and closes it. */
    private static void dialog(Desktop pad,String name,Runnable open) throws Exception {dialogAfter(pad,name,open);}
    /** A box opened, the buttons with these names pressed in turn, and what is then shown pictured and closed. */
    private static void dialogAfter(Desktop pad,String name,Runnable open,String... presses) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        java.util.Set<Window> before=new java.util.HashSet<>(java.util.Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(()->{
            long[] seen={0};int[] pressed={0};
            javax.swing.Timer look=new javax.swing.Timer(150,event->{
                for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&!before.contains(window)) {
                    if(seen[0]==0){seen[0]=System.nanoTime();return;}
                    if(System.nanoTime()-seen[0]<TimeUnit.MILLISECONDS.toNanos(1200))return;
                    if(pressed[0]<presses.length) {
                        AbstractButton press=calledIn(dialog.getContentPane(),presses[pressed[0]]);
                        if(press==null){((javax.swing.Timer)event.getSource()).stop();failure.set(new AssertionError(name+": nothing called "+presses[pressed[0]]));dialog.dispose();done.countDown();return;}
                        pressed[0]++;seen[0]=System.nanoTime();before.add(dialog);press.doClick(0);return;
                    }
                    ((javax.swing.Timer)event.getSource()).stop();
                    try{shootNow(dialog,name);}catch(Throwable error){failure.set(error);}finally{dialog.dispose();done.countDown();}
                    return;
                }
            });look.start();open.run();
        });
        assertTrue(name+" did not open",done.await(30,TimeUnit.SECONDS));
        if(failure.get()!=null)throw new AssertionError(failure.get());
        pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
    }
    private static AbstractButton calledIn(Container in,String words) {
        for(Component c:in.getComponents()) {
            if(c instanceof AbstractButton b&&(words.equals(b.getText())||words.equals(b.getAccessibleContext().getAccessibleName())))return b;
            if(c instanceof Container k){AbstractButton f=calledIn(k,words);if(f!=null)return f;}
        }
        return null;
    }
    private static JButton named(Container in,String name) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&name.equals(b.getName()))return b;if(c instanceof Container k){JButton f=named(k,name);if(f!=null)return f;}}
        return null;
    }
    /** A menu, drawn on its own at the screen's scale. */
    private static void picture(JComponent menu,String name) throws Exception {
        var scale=menu.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(Math.max(1,menu.getWidth())*scale.getScaleX()),(int)Math.ceil(Math.max(1,menu.getHeight())*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);menu.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    /** At the screen's own scale, from the window itself, never from the screen. */
    private static void shootNow(Window window,String name) throws Exception {
        var scale=window.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);
        Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
        window.paint(g);g.dispose();
        javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
