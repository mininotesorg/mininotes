// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * A help request sent silently when a note or folder opens, on the PC (docs/HOME.md, decision 113): the box that sets it on,
 * which says the computer has no location so the request goes without one, and warns an ordinary thing's setting is in the
 * notebook; and the loud alert when one arrives, "HELP from <name>" with the message, the place, the time and a map link.
 * Synthetic devices, offline, no network; pictures saved beside the gallery's.
 */
public class DesktopHelpTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private static final String ANA="MxHelpAna@127.0.0.1:9201";

    @Test public void theBoxThatSetsItOnAndTheAlertWhenItArrives() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            Node.chooseName(pad.context,"Sam");Node.chooseDevice(pad.context,"Study PC");
            pad.store.pairedWith(ANA,"Ana",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Walk home";note.body="Synthetic";pad.store.save(note);
            settle(pad);

            // The box that sets it on, pictured: what it does, that the PC has no location, the forensic warning, and Ana to choose.
            boolean[] saved={false};
            JDialog box=open(pad,()->pad.helpSetup(pad.frame,false,false,List.of(),"",
                (to,words)->{try{pad.store.helpWhenOpened(NoteStore.Branch.Kind.PAGE,note.id,true,to,words);}catch(Exception e){throw new RuntimeException(e);}saved[0]=true;},
                ()->{}));
            try {
                Container in=box.getContentPane();
                SwingUtilities.invokeAndWait(()->{
                    assertEquals(Help.ASK_TITLE,box.getTitle());
                    List<String> said=new java.util.ArrayList<>();texts(in,said);
                    assertTrue(said.toString(),said.stream().anyMatch(s->s.contains("no location")));
                    assertTrue(said.toString(),said.stream().anyMatch(s->s.contains("examines this device")));
                    assertNotNull("Ana should be a choice",checkbox(in));
                    // Choose Ana and write a message.
                    checkbox(in).setSelected(true);
                    JTextField message=field(in);assertNotNull(message);message.setText("Being followed");
                });
                shoot(box,"help-request-set");
                SwingUtilities.invokeAndWait(()->button(in,Help.TURN_ON).doClick());
            } finally { SwingUtilities.invokeAndWait(box::dispose); }
            await(()->{try{return pad.store.helpWhenOpenedOf(NoteStore.Branch.Kind.PAGE,note.id).on;}catch(Exception e){return false;}});
            NoteStore.HelpWhenOpened kept=onEdt(()->pad.store.helpWhenOpenedOf(NoteStore.Branch.Kind.PAGE,note.id));
            assertTrue(saved[0]);
            assertTrue(kept.on);
            assertEquals(List.of(ANA),kept.to);
            assertEquals("Being followed",kept.words);

            // The alert when one arrives, pictured: HELP from Ana, the message, the place, the time and a map link.
            byte[] incident=new byte[Help.INCIDENT];for(int i=0;i<incident.length;i++)incident[i]=(byte)i;
            Help.Request r=new Help.Request(incident,0,"Being followed",true,48.8566,2.3522,15f,
                System.currentTimeMillis(),System.currentTimeMillis());
            JDialog alert=open(pad,()->pad.helpAlert(Post.Landed.help(r,"Ana")));
            try {
                Container in=alert.getContentPane();
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("HELP from Ana",alert.getTitle());
                    List<String> said=new java.util.ArrayList<>();texts(in,said);
                    String all=String.join(" / ",said);
                    assertTrue(all,all.contains("Being followed"));
                    assertTrue(all,all.contains("48.856600, 2.352200"));
                    assertTrue(all,all.contains("openstreetmap.org"));
                    assertNotNull("a way to open the map",button(in,"Open the map"));
                });
                shoot(alert,"help-request-alert");
            } finally { SwingUtilities.invokeAndWait(alert::dispose); }
        } finally {
            SwingUtilities.invokeAndWait(()->{try{pad.shutdown(false);}catch(Exception ignored){}pad.frame.dispose();});
        }
    }

    /**
     * The help request set up in Profile (decision 115): the store lists, adds and removes the ordinary notes and folders
     * that send it, the switch that moved out of each thing's menu and into one place in Profile. A private thing's setting
     * stays inside the vault and is never in this list (decision 113).
     */
    @Test public void profileListsAddsAndRemovesOrdinaryTriggers() throws Exception {
        Path folder=temp.newFolder("triggers").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title="Walk home";note.body="Synthetic";pad.store.save(note);
            NoteStore.Shelf errands=onEdt(()->pad.store.addCollection("Errands"));
            settle(pad);
            // None set yet.
            assertTrue(onEdt(()->pad.store.helpTriggers()).isEmpty());
            // Chosen, as Profile's Choose does: both listed, by their names.
            onEdt(()->{pad.store.helpWhenOpened(NoteStore.Branch.Kind.PAGE,note.id,true,List.of(ANA),"Help");return null;});
            onEdt(()->{pad.store.helpWhenOpened(NoteStore.Branch.Kind.COLLECTION,errands.id,true,List.of(ANA),"Help");return null;});
            List<NoteStore.HelpTrigger> listed=onEdt(()->pad.store.helpTriggers());
            assertEquals(2,listed.size());
            java.util.Set<String> names=new java.util.HashSet<>();for(NoteStore.HelpTrigger t:listed)names.add(t.name);
            assertTrue(names.toString(),names.contains("Walk home"));assertTrue(names.toString(),names.contains("Errands"));
            // Removed, as Profile's Remove does: off the list, and the thing's setting cleared.
            onEdt(()->{pad.store.helpWhenOpened(NoteStore.Branch.Kind.PAGE,note.id,false,null,null);return null;});
            List<NoteStore.HelpTrigger> after=onEdt(()->pad.store.helpTriggers());
            assertEquals(1,after.size());assertEquals("Errands",after.get(0).name);
            assertFalse(onEdt(()->pad.store.helpWhenOpenedOf(NoteStore.Branch.Kind.PAGE,note.id)).on);
        } finally {
            SwingUtilities.invokeAndWait(()->{try{pad.shutdown(false);}catch(Exception ignored){}pad.frame.dispose();});
        }
    }

    // ---- the small helpers, as the other desktop tests have them ------------------------------------------------------

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
    private static void texts(Container in,List<String> out) {
        for(Component c:in.getComponents()) {
            if(c instanceof JLabel l&&l.getText()!=null&&!l.getText().isBlank())out.add(l.getText());
            // DesktopUi.Text and the alert's body are both JTextArea, so this catches the notes, warnings and the alert body.
            if(c instanceof JTextArea a&&a.getText()!=null&&!a.getText().isBlank())out.add(a.getText());
            if(c instanceof Container k)texts(k,out);
        }
    }
    private static JCheckBox checkbox(Container in) {
        for(Component c:in.getComponents()){if(c instanceof JCheckBox b)return b;if(c instanceof Container k){JCheckBox f=checkbox(k);if(f!=null)return f;}}
        return null;
    }
    private static JTextField field(Container in) {
        for(Component c:in.getComponents()){if(c instanceof JTextField b)return b;if(c instanceof Container k){JTextField f=field(k);if(f!=null)return f;}}
        return null;
    }
    private static JButton button(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&words.equals(b.getText()))return b;if(c instanceof Container k){JButton f=button(k,words);if(f!=null)return f;}}
        return null;
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
    private static <T> T onEdt(java.util.concurrent.Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T t=(T)got[0];return t;
    }
    private static void await(java.util.concurrent.Callable<Boolean> until) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<end){if(Boolean.TRUE.equals(onEdt(until)))return;Thread.sleep(50);}
        throw new AssertionError("waited too long");
    }
}
