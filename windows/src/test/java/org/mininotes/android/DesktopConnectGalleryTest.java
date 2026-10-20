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

            // 1. People and devices, its + by People: the ways to connect.
            JDialog people=open(pad,()->pad.people(null));
            try {
                JButton plus=named(people.getContentPane(),"addPerson");assertNotNull("the + by People",plus);
                SwingUtilities.invokeAndWait(plus::doClick);
                JPopupMenu[] menu={null};
                long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                while(menu[0]==null&&System.nanoTime()<until) {
                    SwingUtilities.invokeAndWait(()->{MenuElement[] path=MenuSelectionManager.defaultManager().getSelectedPath();
                        if(path.length>0&&path[0] instanceof JPopupMenu p&&p.isShowing())menu[0]=p;});
                    Thread.sleep(50);
                }
                assertNotNull("the ways to connect",menu[0]);Thread.sleep(300);
                SwingUtilities.invokeAndWait(()->{try{picture(menu[0],"connect-01-people-plus");}catch(Exception e){throw new RuntimeException(e);}
                    menu[0].setVisible(false);MenuSelectionManager.defaultManager().clearSelectedPath();});
            } finally {SwingUtilities.invokeAndWait(people::dispose);}

            // Codes are drawn only by a window that is not offline; turned off for the drawing, with no node started.
            java.lang.reflect.Field offline=Desktop.class.getDeclaredField("offline");offline.setAccessible(true);
            SwingUtilities.invokeAndWait(()->{try{offline.setBoolean(pad,false);}catch(Exception e){throw new RuntimeException(e);}});
            try {
                // 2. Sharing a note by code: the role asked first, then the code.
                dialog(pad,"connect-02-role-before-code",()->pad.showCode(groceries));
                dialogAfter(pad,"connect-03-code-for-a-note",()->pad.showCode(groceries),"Show my code");
                // 3. My other device: the same code box, offering everything.
                dialog(pad,"connect-04-my-device-role",()->pad.showCode(Desktop.library()));
                dialogAfter(pad,"connect-05-my-device-code",()->pad.showCode(Desktop.library()),"Show my code");
            } finally {SwingUtilities.invokeAndWait(()->{try{offline.setBoolean(pad,true);}catch(Exception e){throw new RuntimeException(e);}});}

            // 4. The other side: a plain code pasted, then a code that shares a note, each read as the scanner reads it.
            //    Drawn from this PC's own keys under a made-up name; closed without Pair or Accept, so nothing is kept or sent.
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
