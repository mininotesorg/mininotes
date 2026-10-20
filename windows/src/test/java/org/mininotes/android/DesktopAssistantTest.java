package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * The Agent on the PC (decisions 191, 192): a question answered from the help, the todo list read and added to, a note shared
 * with a contact once confirmed, what is new. Made-up notes and a made-up contact, offline; drawn from the window.
 */
public class DesktopAssistantTest {
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

    @Test public void asksAnswersAndDoesWhatItSays() throws Exception {
        NoteStore.Note todo=new NoteStore.Note();todo.title="Todo";todo.body="- Synthetic bread\n- Synthetic milk";todo.book=Things.HOME;pad.store.save(todo);
        NoteStore.Note grocery=new NoteStore.Note();grocery.title="Grocery";grocery.body="Synthetic apples";grocery.book=Things.HOME;pad.store.save(grocery);
        Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder("carol").toPath().toFile()));
        String carol="MxCarolAssistant@127.0.0.1:9961";
        pad.store.pairedWith(carol,"Carol",false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());

        SwingUtilities.invokeAndWait(()->DesktopAssistant.open(pad));
        await(()->DesktopAssistant.shown()!=null&&DesktopAssistant.shown().box().isShowing());
        DesktopAssistant a=DesktopAssistant.shown();

        // A question: the help's answer about the dock.
        SwingUtilities.invokeAndWait(()->a.ask("What is remove from the dock?"));
        await(()->said(a).contains("The dock is the row of icons"));

        // The todo list, read.
        SwingUtilities.invokeAndWait(()->a.ask("What is on my todo list?"));
        await(()->said(a).contains("Synthetic milk"));

        // A line added, once confirmed.
        SwingUtilities.invokeAndWait(()->a.ask("add Synthetic eggs to my todo list"));
        await(()->button(a.talk(),"Add")!=null);
        SwingUtilities.invokeAndWait(()->button(a.talk(),"Add").doClick());
        await(()->pad.store.get(todo.id).body.endsWith("- Synthetic eggs"));
        await(()->said(a).contains("Added to Todo."));

        // Shared with Carol, to read, once confirmed.
        SwingUtilities.invokeAndWait(()->a.ask("share Grocery with Carol read only"));
        await(()->button(a.talk(),"Share")!=null);
        {String now=said(a);assertTrue(now,now.contains("Share “Grocery” with Carol? They can read it."));}
        shoot(a.box(),"99c-ask-mininotes");
        SwingUtilities.invokeAndWait(()->button(a.talk(),"Share").doClick());
        await(()->{for(Sharing.Rule r:pad.store.shares())if(r.target.equals(grocery.id)&&r.address.equals(carol)&&r.level==Sharing.Level.READ)return true;return false;});
        await(()->said(a).contains("Shared with Carol."));

        // Somebody nobody knows: said, with who there is.
        SwingUtilities.invokeAndWait(()->a.ask("share Grocery with Zed"));
        await(()->said(a).contains("I do not know anybody called “Zed”. Your people: Carol."));

        // What is new, and something not understood.
        SwingUtilities.invokeAndWait(()->a.ask("what's new?"));
        await(()->said(a).contains("new")||said(a).contains("Nothing is new"));
        SwingUtilities.invokeAndWait(()->a.ask("qqq zzz"));
        await(()->said(a).contains("I do not have an answer for that yet."));
        shoot(a.box(),"99d-ask-mininotes-more");
        // No button closes it: a press outside it, on the window behind, does.
        assertTrue(onEdt(()->a.box().isDisplayable()));
        SwingUtilities.invokeAndWait(()->{Component behind=pad.frame.getContentPane();
            java.awt.event.MouseEvent press=new java.awt.event.MouseEvent(behind,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,10,10,1,false,java.awt.event.MouseEvent.BUTTON1);
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(press);});
        await(()->!onEdt(()->a.box().isDisplayable()));
    }

    /** Every word the conversation shows, in order. */
    private static String said(DesktopAssistant a) throws Exception {
        StringBuilder all=new StringBuilder();
        SwingUtilities.invokeAndWait(()->collect(a.talk(),all));
        return all.toString();
    }
    private static void collect(Container in,StringBuilder all) {
        for(Component c:in.getComponents()) {
            if(c instanceof javax.swing.text.JTextComponent t)all.append(t.getText()).append('\n');
            else if(c instanceof JLabel l)all.append(l.getText()).append('\n');
            else if(c instanceof AbstractButton b)all.append(b.getText()).append('\n');
            if(c instanceof Container k)collect(k,all);
        }
    }
    /** The last enabled button with these words. */
    private static JButton button(Container in,String text) {
        JButton[] found={null};
        find(in,text,found);
        return found[0];
    }
    private static void find(Container in,String text,JButton[] found) {
        for(Component c:in.getComponents()) {
            if(c instanceof JButton b&&text.equals(b.getText())&&b.isEnabled())found[0]=b;
            if(c instanceof Container k)find(k,text,found);
        }
    }
    private static void shoot(Window window,String name) throws Exception {
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(()->{try{
            window.validate();
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
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
