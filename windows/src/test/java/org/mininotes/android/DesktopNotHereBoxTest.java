package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * A note's Share with on the PC, every line with a dropdown of its own for this note alone (decision 293; the owner, 2026-10-30,
 * with a picture of "Partager « Canada » avec", a device of his under Mes appareils: "this button retirer here, it should also
 * be there for every contact including own devices"; "keep them with the 3 levels, admin, write, read"). Ana has the note through
 * its folder and the owner's laptop through its bond with everything: each says what it may do, drops down to Admin, Can write,
 * Can read and Remove, and Remove asks first and takes this note alone, said Removed here, with choosing a level giving it back.
 * The bond is untouched. Synthetic devices, offline; pictures saved beside the gallery's.
 */
public class DesktopNotHereBoxTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private static final String ANA="MxHereBoxAna@127.0.0.1:9201",LAPTOP="MxHereBoxLaptop@127.0.0.1:9202";

    @Test public void everyLineChangesThisNoteAloneAndRemoveSaysRemovedHere() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            NoteStore store=pad.store;
            Node.chooseName(pad.context,"Sam");Node.chooseDevice(pad.context,"Study PC");
            store.pairedWith(ANA,"Ana",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            store.pairedWith(LAPTOP,"Work laptop",true,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,LAPTOP,Sharing.Level.WRITE,"Work laptop");
            String canada=store.addCollection("Canada").id;
            store.give(Sharing.Scope.THING,canada,ANA,Sharing.Level.WRITE,null);
            NoteStore.Note plan=new NoteStore.Note();plan.book=canada;plan.title="Plan";plan.body="Synthetic plan";store.save(plan);
            settle(pad);
            NoteStore.Branch thing=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,plan.id,"","Plan","",0,0,false);
            JDialog box=open(pad,()->DesktopPeople.with(pad,thing,false,null,null,null));
            try {
                Container in=box.getContentPane();
                SwingUtilities.invokeAndWait(()->{
                    // Ana, through Canada, and the laptop, through everything: each a dropdown, not words alone, not Owner.
                    JButton ana=named(in,"standing Ana"),laptop=named(in,"standing Work laptop");
                    assertNotNull("Ana's line drops down",ana);assertNotNull("the laptop's line drops down (it said Owner, or its bond)",laptop);
                    assertEquals("Can write  ▾",ana.getText());assertEquals("Can write  ▾",laptop.getText());
                    assertTrue(ana.getToolTipText(),ana.getToolTipText().contains("for this one only"));
                });
                shoot(box,"293-share-with-every-line");
                // The laptop's drops down to the three levels and Remove.
                SwingUtilities.invokeAndWait(()->named(in,"standing Work laptop").doClick(0));
                SwingUtilities.invokeAndWait(()->{
                    JPopupMenu menu=menu();
                    String said=lines(menu);
                    assertTrue(said,said.contains("Admin")&&said.contains("Can write")&&said.contains("Can read")&&said.contains("Remove"));
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                });
                // Remove on Ana's line asks first, saying what it does, and takes this note alone.
                SwingUtilities.invokeAndWait(()->named(in,"standing Ana").doClick(0));
                JDialog asked=open(pad,()->{JPopupMenu menu=menu();for(Component c:menu.getComponents())if(c instanceof JMenuItem item&&"Remove".equals(item.getText())){item.doClick(0);break;}
                    MenuSelectionManager.defaultManager().clearSelectedPath();});
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("Remove Ana?",asked.getTitle());
                    java.util.List<String> said=new java.util.ArrayList<>();texts(asked.getContentPane(),said);
                    assertTrue(said.toString(),said.contains(Sharing.REMOVE_HERE_SAYS));
                });
                shoot(asked,"293-remove-asks");
                SwingUtilities.invokeLater(()->button(asked.getContentPane(),"Remove").doClick(0));
                await(()->!store.reaches(Sharing.Scope.PAGE,plan.id).containsKey(ANA));
                await(()->onEdt(()->named(in,"standing Ana")!=null&&named(in,"standing Ana").getText().startsWith(Sharing.REMOVED_HERE)));
                assertTrue("the rest of Canada is still hers",store.reaches(Sharing.Scope.THING,canada).containsKey(ANA));
                shoot(box,"293-removed-here");
                // Removed here drops down to the levels alone; choosing one gives it back.
                SwingUtilities.invokeAndWait(()->pick(named(in,"standing Ana"),Sharing.Level.READ.words()));
                await(()->store.reaches(Sharing.Scope.PAGE,plan.id).containsKey(ANA));
                await(()->onEdt(()->named(in,"standing Ana")!=null&&named(in,"standing Ana").getText().startsWith("Can read")));
                // The laptop taken away from the note alone: its bond with everything is untouched.
                SwingUtilities.invokeAndWait(()->named(in,"standing Work laptop").doClick(0));
                JDialog again=open(pad,()->{JPopupMenu menu=menu();for(Component c:menu.getComponents())if(c instanceof JMenuItem item&&"Remove".equals(item.getText())){item.doClick(0);break;}
                    MenuSelectionManager.defaultManager().clearSelectedPath();});
                SwingUtilities.invokeLater(()->button(again.getContentPane(),"Remove").doClick(0));
                await(()->!store.reaches(Sharing.Scope.PAGE,plan.id).containsKey(LAPTOP));
                Sharing.Rule bond=null;for(Sharing.Rule one:store.shares())if(one.scope==Sharing.Scope.LIBRARY&&one.address.equals(LAPTOP))bond=one;
                assertNotNull(bond);assertEquals(Sharing.Level.WRITE,bond.level);
            } finally {SwingUtilities.invokeAndWait(box::dispose);}
        } finally {SwingUtilities.invokeAndWait(()->pad.frame.dispose());}
    }

    // ---- helpers, as DesktopPeopleBoxTest has them -------------------------------------------------------------------

    private static JPopupMenu menu() {
        MenuElement[] open=MenuSelectionManager.defaultManager().getSelectedPath();
        assertTrue("no menu dropped",open.length>0&&open[0].getComponent() instanceof JPopupMenu);
        return (JPopupMenu)open[0].getComponent();
    }
    private static String lines(JPopupMenu menu) {
        StringBuilder all=new StringBuilder();
        for(Component c:menu.getComponents())if(c instanceof JMenuItem item)all.append(item.getText()).append(" | ").append(item.getAccessibleContext().getAccessibleName()).append('\n');
        return all.toString();
    }
    /** A button pressed, and the line of the menu it drops that starts with these words chosen. On the event thread. */
    private static void pick(AbstractButton drops,String words) {
        assertNotNull("nothing to press for "+words,drops);
        drops.doClick(0);
        JPopupMenu menu=menu();
        try {
            for(Component c:menu.getComponents())if(c instanceof JMenuItem item&&item.isEnabled()) {
                String says=item.getAccessibleContext().getAccessibleName();
                if(words.equals(item.getText())||says!=null&&says.startsWith(words)){item.doClick(0);return;}
            }
            throw new AssertionError("no line "+words+" in the menu of "+drops.getText());
        } finally {MenuSelectionManager.defaultManager().clearSelectedPath();}
    }
    /** A box opened by {@code open}, and found once it has drawn what it fetched. */
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
    private static JButton named(Container in,String name) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&name.equals(b.getName())&&b.isShowing())return b;if(c instanceof Container k){JButton f=named(k,name);if(f!=null)return f;}}
        return null;
    }
    private static JButton button(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&words.equals(b.getText()))return b;if(c instanceof Container k){JButton f=button(k,words);if(f!=null)return f;}}
        return null;
    }
    private static void texts(Container in,java.util.List<String> out) {
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t)out.add(t.getText());if(c instanceof Container k)texts(k,out);}
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
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
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
