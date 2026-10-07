package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import javax.swing.*;

public class DesktopUiTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    /**
     * Painting a text with an emoji changes nothing about it. It used to set its background for the emoji
     * and back again, and each setting asked for another paint: the window redrew itself without end,
     * using the processor and too busy to hear its close button.
     */
    @Test public void paintingEmojiDoesNotAskForAnotherPaint() throws Exception {
        if(java.awt.GraphicsEnvironment.isHeadless())return;
        int[] changes={0};
        SwingUtilities.invokeAndWait(()->{
            DesktopUi.install();
            DesktopUi.Text text=DesktopUi.note("Bread 🥖 and tea ☕",300,DesktopUi.INK,DesktopUi.BODY);
            JPanel card=DesktopUi.card(null,text);card.setSize(360,80);card.doLayout();text.setSize(text.getPreferredSize());
            text.addPropertyChangeListener(e->{if(!"ancestor".equals(e.getPropertyName()))changes[0]++;});
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(360,80,java.awt.image.BufferedImage.TYPE_INT_RGB);
            for(int i=0;i<3;i++){java.awt.Graphics2D g=image.createGraphics();card.paint(g);g.dispose();}
        });
        assertEquals("nothing about the text changes while it is painted",0,changes[0]);
    }

    @Test public void typeAutosaveReopenAndRender() throws Exception {
        Path folder=temp.newFolder("ui-pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            // Drain the startup callbacks before typing through the real documents.
            pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            SwingUtilities.invokeAndWait(()->{pad.title.setText("Saturday");pad.page.setText("Pick up fresh bread\nTea for the ferry\n\nLeave room for a little wandering.");});
            await(()->pad.store.latest().body.contains("wandering"));
            // One line under the title once the save settles - the phone's mark, then a round for each person it
            // reaches - said by being seen: no words, and no box under the pointer. A note nobody has: the empty ring.
            await(()->pad.noteMark!=null);
            assertEquals(SyncMark.HERE,pad.noteMark);assertTrue(pad.syncMark.isVisible());assertNull(pad.syncMark.getToolTipText());
            // No rounds, only the + that adds somebody (the owner, 2026-10-05).
            assertEquals(1,pad.rounds.getComponentCount());assertTrue(pad.peopleShown.isEmpty());
            assertEquals(SyncMark.HERE.said("this PC"),pad.syncMark.getAccessibleContext().getAccessibleName());
            SwingUtilities.invokeAndWait(()->{
                try {
                    var image=new java.awt.image.BufferedImage(pad.frame.getWidth(),pad.frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var g=image.createGraphics();pad.frame.paint(g);g.dispose();
                    Path shot=Path.of("build","verification","windows-pad.png");Files.createDirectories(shot.getParent());javax.imageio.ImageIO.write(image,"png",shot.toFile());
                }catch(Exception e){throw new RuntimeException(e);}
            });
            assertTrue(Files.isRegularFile(Path.of("build","verification","windows-pad.png")));
            java.util.concurrent.CountDownLatch shared=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> shareFailure=new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeLater(()->{
                javax.swing.Timer capture=new javax.swing.Timer(100,event->{
                    for(java.awt.Window window:pad.frame.getOwnedWindows())if(window instanceof JDialog dialog&&dialog.isVisible()) {
                        ((javax.swing.Timer)event.getSource()).stop();
                        try {
                            var image=new java.awt.image.BufferedImage(dialog.getWidth(),dialog.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                            var g=image.createGraphics();dialog.paint(g);g.dispose();
                            javax.imageio.ImageIO.write(image,"png",Path.of("build","verification","windows-sharing.png").toFile());
                        }catch(Throwable error){shareFailure.set(error);}finally{dialog.dispose();shared.countDown();}
                    }
                });capture.start();findButton(pad.frame,"Share").doClick();
            });
            assertTrue("Sharing dialog did not open",shared.await(30,java.util.concurrent.TimeUnit.SECONDS));
            if(shareFailure.get()!=null)throw new AssertionError(shareFailure.get());
            String id=pad.store.latest().id;pad.disk.flush(10000);
            SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());
            try(NoteStore reopened=new NoteStore(new org.mininotes.desktop.platform.content.Context(folder.toFile()))){assertEquals("Saturday",reopened.get(id).title);assertTrue(reopened.get(id).body.contains("wandering"));}
        } finally {if(pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}
    }
    /** The owner, 2026-10-05: Home's colour is the whole app's on the laptop, and This note is first on a page's right-click. */
    @Test public void theWholeAppTakesHomesColourAndThisNoteIsFirst() throws Exception {
        Path folder=temp.newFolder("ui-colour").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            java.awt.Color plain=pad.tree.getBackground(),paper=pad.page.getBackground();
            SwingUtilities.invokeAndWait(()->pad.paintHome(6));
            await(()->!pad.tree.getBackground().equals(plain));
            assertEquals("the side list as the tree",pad.tree.getBackground(),pad.openList.getBackground());
            assertNotEquals("a note with no colour of its own is in the app's",paper,pad.page.getBackground());
            SwingUtilities.invokeAndWait(()->{pad.title.setText("Colours");pad.page.setText("Plain, __underlined__ and plain again.");pad.page.setCaretPosition(0);});
            pad.disk.flush(10000);Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{
                try {
                    var image=new java.awt.image.BufferedImage(pad.frame.getWidth(),pad.frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var g=image.createGraphics();pad.frame.paint(g);g.dispose();
                    javax.imageio.ImageIO.write(image,"png",Path.of("build","verification","windows-app-colour.png").toFile());
                }catch(Exception e){throw new RuntimeException(e);}
                javax.swing.JPopupMenu menu=pad.paperMenu(pad.page,null);
                assertTrue(menu.getComponent(0) instanceof javax.swing.JMenu first&&"This note".equals(first.getText()));
            });
            SwingUtilities.invokeAndWait(()->pad.paintHome(Tint.NONE));
            await(()->pad.tree.getBackground().equals(plain));
        } finally {if(pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}
    }
    /**
     * The owner, 2026-10-06: "the color intensity applies to the whole app, it should be specific to the elements selected.
     * Also, when I pick the color, the menu should stay open so that I can set other elements" (decision 107).
     */
    @Test public void aColourPickedLeavesTheMenuUpAndTheStrengthMovesOneThingOnly() throws Exception {
        Path folder=temp.newFolder("ui-tone").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            SwingUtilities.invokeAndWait(()->{pad.title.setText("One");pad.page.setText("The note whose colour and strength are set.");});
            await(()->pad.store.latest().body.contains("strength"));pad.disk.flush(10000);
            NoteStore.Note one=pad.store.latest();
            // Another note, red already, at the usual strength: it is not to move.
            NoteStore.Note other=new NoteStore.Note();other.book=one.book;other.title="Other";other.body="Red before, and red the same after.";
            pad.store.save(other);pad.store.paint(NoteStore.Branch.Kind.PAGE,other.id,1);
            SwingUtilities.invokeAndWait(pad::refresh);pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            java.awt.Color paper=pad.page.getBackground();
            NoteStore.Branch thing=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,one.id,one.book,"One","",0,0,false);
            JPopupMenu[] menu=new JPopupMenu[1];JMenu[] colour=new JMenu[1];
            SwingUtilities.invokeAndWait(()->{
                menu[0]=pad.thingMenu(thing);menu[0].show(pad.page,40,40);
                for(java.awt.Component c:menu[0].getComponents())if(c instanceof JMenu m&&"Colour".equals(m.getText()))colour[0]=m;
                MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu[0],colour[0],colour[0].getPopupMenu()});
            });
            Thread.sleep(400);
            // Red, let go on as the mouse does: the menu stays, the ring moves, and the page behind it is red.
            SwingUtilities.invokeAndWait(()->{assertTrue("the colours are showing",colour[0].getPopupMenu().isShowing());release(line(colour[0].getPopupMenu(),"Red"));});
            await(()->!pad.page.getBackground().equals(paper));pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            SwingUtilities.invokeAndWait(()->{
                assertTrue("the menu is still up after a colour",menu[0].isShowing()&&colour[0].getPopupMenu().isShowing());
                assertEquals(3,MenuSelectionManager.defaultManager().getSelectedPath().length);
                assertEquals("Red, chosen",line(colour[0].getPopupMenu(),"Red").getAccessibleContext().getAccessibleName());
                assertEquals("No colour",line(colour[0].getPopupMenu(),"No colour").getAccessibleContext().getAccessibleName());
                assertNotNull("the line it dropped from wears the colour",colour[0].getIcon());
                assertEquals("at the usual strength, never given one",DesktopLook.wash(1,Desktop.PAPER,0.12f,0.72f,pad.usual),pad.page.getBackground());
                shot(pad,"windows-colour-menu-stays.png");
            });
            assertEquals(1,pad.store.colourOf(NoteStore.Branch.Kind.PAGE,one.id));
            // Its strength, to the loudest: this note's page, and nothing else's.
            java.awt.Color shelf=pad.tree.getBackground();
            SwingUtilities.invokeAndWait(()->{
                JComponent band=(JComponent)((java.awt.Container)colour[0].getPopupMenu().getComponent(colour[0].getPopupMenu().getComponentCount()-1)).getComponent(1);
                band.dispatchEvent(new java.awt.event.MouseEvent(band,java.awt.event.MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,band.getWidth()-3,band.getHeight()/2,1,false,java.awt.event.MouseEvent.BUTTON1));
                assertEquals("at once, as the hand moves",DesktopLook.wash(1,Desktop.PAPER,0.12f,0.72f,Tint.TONES.length-1),pad.page.getBackground());
                assertEquals("Colour strength "+Tint.TONES.length+" of "+Tint.TONES.length,band.getToolTipText());
            });
            pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
            SwingUtilities.invokeAndWait(()->{
                assertTrue("the menu is still up after the strength",menu[0].isShowing()&&colour[0].getPopupMenu().isShowing());
                assertEquals("the app keeps its own",shelf,pad.tree.getBackground());assertEquals(pad.usual,pad.homeTone());
                shot(pad,"windows-colour-strength-its-own.png");
            });
            assertEquals(Tint.TONES.length-1,pad.store.toneOf(NoteStore.Branch.Kind.PAGE,one.id));
            assertEquals("the other red note keeps the usual",Tint.USUAL,pad.store.toneOf(NoteStore.Branch.Kind.PAGE,other.id));
            assertEquals("the usual is not moved",Tint.FIRST_TONE,pad.usual);
            // Writing lines is set by looking too: the menu stays for it.
            SwingUtilities.invokeAndWait(()->{
                MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu[0]});
                JMenuItem lines=line(menu[0],"Writing lines");boolean was=lines.isSelected();release(lines);
                assertNotEquals(was,lines.isSelected());assertTrue("the menu is still up after Writing lines",menu[0].isShowing());
                MenuSelectionManager.defaultManager().clearSelectedPath();
            });
            // The other note, opened: red as it was, at the usual strength.
            SwingUtilities.invokeAndWait(()->pad.open(other.id));
            await(()->pad.page.getBackground().equals(DesktopLook.wash(1,Desktop.PAPER,0.12f,0.72f,pad.usual)));
            // And back: the first is as loud as it was made, read from the notebook.
            SwingUtilities.invokeAndWait(()->pad.open(one.id));
            await(()->pad.page.getBackground().equals(DesktopLook.wash(1,Desktop.PAPER,0.12f,0.72f,Tint.TONES.length-1)));
        } finally {if(pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->{MenuSelectionManager.defaultManager().clearSelectedPath();pad.shutdown(false);});await(()->!pad.frame.isDisplayable());}}
    }
    /** A click let go on a line of a menu, as the mouse does it: through the menu's own handling, which is what closes a menu or leaves it up. */
    private static void release(JComponent line){line.dispatchEvent(new java.awt.event.MouseEvent(line,java.awt.event.MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,4,4,1,false,java.awt.event.MouseEvent.BUTTON1));}
    private static JMenuItem line(JPopupMenu menu,String words){for(java.awt.Component c:menu.getComponents())if(c instanceof JMenuItem m&&words.equals(m.getText()))return m;throw new AssertionError("no line "+words);}
    /** The window with whatever menu is up on it, kept to be looked at. */
    private static void shot(Desktop pad,String name) {
        try {
            var image=new java.awt.image.BufferedImage(pad.frame.getWidth(),pad.frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();pad.frame.paint(g);g.dispose();
            Path to=Path.of("build","verification",name);Files.createDirectories(to.getParent());javax.imageio.ImageIO.write(image,"png",to.toFile());
        }catch(Exception e){throw new RuntimeException(e);}
    }
    /** The owner, 2026-10-05: "a new downloaded windows app should open on the desktop not inside the My first note." */
    @Test public void aNewNotebookOpensOnHomeWithItsFirstNotesOnIt() throws Exception {
        Path folder=temp.newFolder("ui-new").toPath();Desktop[] app=new Desktop[1];
        boolean was=Desktop.welcomes;Desktop.welcomes=true;
        try {
            SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
            Desktop pad=app[0];
            try {
                await(()->pad.frame.isVisible());pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
                assertFalse("on Home, not in a note",pad.onPage);
                java.util.List<String> names=new java.util.ArrayList<>();
                for(NoteStore.Branch one:pad.store.contents(Things.HOME))names.add(one.name);
                assertTrue(names.toString(),names.contains("My first note")&&names.contains("Read me"));
                // And from then on, whatever was open: closed inside a note, it opens there.
                String first=null;for(NoteStore.Branch one:pad.store.contents(Things.HOME))if("My first note".equals(one.name))first=one.id;
                String id=first;SwingUtilities.invokeAndWait(()->pad.open(id));
                await(()->pad.onPage);pad.disk.flush(10000);
            } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
            SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
            Desktop again=app[0];
            try {
                await(()->again.frame.isVisible());again.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});
                assertTrue("the note it was closed in",again.onPage);
            } finally {SwingUtilities.invokeAndWait(()->again.shutdown(false));await(()->!again.frame.isDisplayable());}
        } finally {Desktop.welcomes=was;}
    }
    private static JButton findButton(java.awt.Container parent,String text) {
        for(java.awt.Component child:parent.getComponents()) {
            if(child instanceof JButton button&&text.equals(button.getText()))return button;
            if(child instanceof java.awt.Container container){JButton found=findButton(container,text);if(found!=null)return found;}
        }return null;
    }
    private static void await(java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
