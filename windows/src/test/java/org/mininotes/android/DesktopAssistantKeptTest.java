package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * The Agent on the PC, decision 213: one tall conversation with Full screen; Take me there opening My profile at its
 * Stamping part (named Integritas before decision 231) with the key's field ready; and the conversation kept in the notebook through closing the window, for
 * three hours after the last message, and forgotten by New conversation. A made-up notebook, offline.
 */
public class DesktopAssistantKeptTest {
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

    private DesktopAssistant opened() throws Exception {
        SwingUtilities.invokeAndWait(()->DesktopAssistant.open(pad));
        await(()->DesktopAssistant.shown()!=null&&DesktopAssistant.shown().box().isShowing());
        return DesktopAssistant.shown();
    }

    @Test public void takeMeThereOpensTheStampingPartOfMyProfile() throws Exception {
        DesktopAssistant a=opened();
        // Tall: most of the screen it is on.
        Rectangle screen=onEdt(()->a.box().getGraphicsConfiguration().getBounds());
        assertTrue("tall",onEdt(()->a.box().getHeight())>=Math.min(420,screen.height*0.6));
        SwingUtilities.invokeAndWait(()->a.ask("How do I set my Integritas key?"));
        await(()->button(a.talk(),"Take me there")!=null);
        shoot(a.box(),"99e-agent-take-me-there");
        JButton there=button(a.talk(),"Take me there");
        SwingUtilities.invokeAndWait(there::doClick);
        await(()->!onEdt(()->a.box().isDisplayable()));
        await(()->profile()!=null);
        JDialog profile=profile();
        // The key's field is the one given the keyboard, and it is in sight: the page is scrolled down to it.
        await(()->onEdt(()->{
            if(!(profile.getRootPane().getClientProperty("focus") instanceof JComponent field)||!"integritasKey".equals(field.getName()))return false;
            JScrollPane scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,field);
            Rectangle seen=scroll.getViewport().getViewRect();
            Rectangle at=SwingUtilities.convertRectangle(field.getParent(),field.getBounds(),scroll.getViewport().getView());
            return seen.y>0&&seen.contains(at.x,at.y)&&seen.contains(at.x,at.y+at.height-1);
        }));
        // The part is called Stamping now, and its card is the one in sight (decision 231).
        assertNotNull(onEdt(()->named(profile,"profileCard "+DesktopProfile.STAMPING)));
        assertEquals("Stamping",DesktopProfile.STAMPING);
        shoot(profile,"99f-profile-at-integritas");
        // The conversation, with its Take me there, is still there when the Agent opens again.
        DesktopAssistant again=opened();
        await(()->said(again).contains("How do I set my Integritas key?")&&button(again.talk(),"Take me there")!=null);
    }

    @Test public void theConversationIsKeptThreeHoursThenStartsAfresh() throws Exception {
        DesktopAssistant a=opened();
        SwingUtilities.invokeAndWait(()->a.ask("What is the dock?"));
        await(()->said(a).contains("The dock is the row of icons"));
        await(()->Assistant.Conversation.read(pad.store.secret(Assistant.KEPT_AS),System.currentTimeMillis()).turns.size()==1);
        // Full screen, and back.
        Rectangle was=onEdt(()->a.box().getBounds());
        SwingUtilities.invokeAndWait(a::fullScreen);
        assertTrue(onEdt(a::isFullScreen));
        assertTrue(onEdt(()->a.box().getWidth())>=was.width);
        SwingUtilities.invokeAndWait(a::fullScreen);
        assertEquals(was,onEdt(()->a.box().getBounds()));
        // Closed and opened again: still there.
        SwingUtilities.invokeAndWait(()->a.box().dispose());
        DesktopAssistant again=opened();
        await(()->said(again).contains("What is the dock?")&&said(again).contains("The dock is the row of icons"));

        // Kept from a start of Mininotes two hours ago: shown. From four hours ago: forgotten.
        SwingUtilities.invokeAndWait(()->again.box().dispose());
        pad.store.setSecret(Assistant.KEPT_AS,kept("Synthetic question two hours ago",System.currentTimeMillis()-2*60*60_000L));
        DesktopAssistant two=opened();
        await(()->said(two).contains("Synthetic question two hours ago"));
        SwingUtilities.invokeAndWait(()->two.box().dispose());
        pad.store.setSecret(Assistant.KEPT_AS,kept("Synthetic question four hours ago",System.currentTimeMillis()-4*60*60_000L));
        DesktopAssistant four=opened();
        Thread.sleep(600);
        assertFalse(said(four).contains("Synthetic question four hours ago"));
        assertTrue(onEdt(()->four.kept().isEmpty()));

        // New conversation forgets it, here and in the notebook.
        SwingUtilities.invokeAndWait(()->four.ask("What is the dock?"));
        await(()->!pad.store.secret(Assistant.KEPT_AS).isEmpty());
        SwingUtilities.invokeAndWait(four::afresh);
        assertEquals("",said(four).trim());
        await(()->pad.store.secret(Assistant.KEPT_AS).isEmpty());
    }

    private static String kept(String asked,long at) {
        Assistant.Conversation c=new Assistant.Conversation();
        c.add(c.start(asked,at),Assistant.Kind.SAY,"Synthetic answer",at);
        return c.write();
    }
    private static JDialog profile() throws Exception {
        return onEdt(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&Say.t("My profile").equals(d.getTitle()))return d;return null;});
    }
    private static Component named(Container in,String name) {
        for(Component c:in.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container k){Component f=named(k,name);if(f!=null)return f;}}
        return null;
    }
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
    private static JButton button(Container in,String text) throws Exception {
        return onEdt(()->{JButton[] found={null};find(in,text,found);return found[0];});
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
