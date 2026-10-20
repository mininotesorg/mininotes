package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Behavioral checks through the real documents, storage worker and rendered synthetic window. */
public class DesktopAdaptiveTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Desktop pad;private Path folder;
    @Before public void start() throws Exception {
        folder=temp.newFolder().toPath();SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle();
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}
    @Test public void navigationWaitsForFullDraftAndNewerTypingIsKeptBeforeLeaving() throws Exception {
        String id=pad.store.latest().id;CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        pad.disk.submit(()->{entered.countDown();assertTrue(release.await(15,TimeUnit.SECONDS));return null;},done->{},error->{throw new AssertionError(error);});assertTrue(entered.await(10,TimeUnit.SECONDS));
        String first="x".repeat(24001),latest=first+"\nnewer local writing";
        try{SwingUtilities.invokeAndWait(()->{pad.title.setText("Recovery test");pad.page.setText(first);pad.goHome();assertTrue(pad.onPage());pad.page.setText(latest);});}
        finally{release.countDown();}
        await(()->!pad.onPage());settle();assertEquals(latest,DesktopRecovery.get(pad.store,id).body());assertEquals("",pad.store.get(id).body);
        var d=DesktopRecovery.get(pad.store,id);SwingUtilities.invokeAndWait(()->pad.reopenWriting(d));await(()->pad.onPage()&&pad.page.getText().equals(latest));settle();
        SwingUtilities.invokeAndWait(()->{pad.page.setText("Shortened intentionally");pad.save(null);});await(()->pad.store.get(id).body.equals("Shortened intentionally"));settle();assertTrue(DesktopRecovery.all(pad.store).isEmpty());
        SwingUtilities.invokeAndWait(()->{pad.page.setText(first);pad.save(null);});await(()->DesktopRecovery.get(pad.store,id)!=null);settle();
        SwingUtilities.invokeAndWait(()->{pad.page.setText("Shortened intentionally");pad.save(null);});await(()->DesktopRecovery.all(pad.store).isEmpty());settle();
    }
    @Test public void failureBlocksHomeAndExitThenSuccessfulDraftSurvivesRestart() throws Exception {
        String id=pad.store.latest().id;DesktopRecovery.all(pad.store);
        pad.store.getWritableDatabase().execSQL("CREATE TRIGGER refuse_draft BEFORE INSERT ON desktop_recovery BEGIN SELECT RAISE(ABORT,'test disk full'); END");
        boolean[] left={false};SwingUtilities.invokeAndWait(()->{pad.title.setText("t".repeat(161));pad.page.setText("Complete local body");pad.save(()->left[0]=true);});
        await(()->pad.noteWords.getText().startsWith("Not saved"));assertFalse(left[0]);assertTrue(pad.onPage());assertTrue(DesktopRecovery.all(pad.store).isEmpty());
        SwingUtilities.invokeAndWait(pad::goHome);settle();assertTrue(pad.onPage());
        pad.store.getWritableDatabase().execSQL("DROP TRIGGER refuse_draft");
        SwingUtilities.invokeAndWait(()->pad.save(()->left[0]=true));await(()->left[0]);settle();assertEquals(161,DesktopRecovery.get(pad.store,id).title().length());assertEquals("",pad.store.get(id).body);
        SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});await(()->pad.store.latest()!=null);settle();
        var d=DesktopRecovery.get(pad.store,id);assertNotNull(d);assertEquals("Complete local body",d.body());
        SwingUtilities.invokeAndWait(()->pad.reopenWriting(d));await(()->pad.page.getText().equals(d.body()));settle();assertEquals(d.title(),pad.title.getText());
    }
    @Test public void oversizedStoredNoteDisplaysInFullAndEditsEnterRecovery() throws Exception {
        NoteStore.Note large=new NoteStore.Note();large.book=Things.HOME;large.title="Oversized stored synthetic note";large.body="😀 tea ".repeat(5000);pad.store.save(large);
        SwingUtilities.invokeAndWait(()->pad.open(large.id));await(()->pad.page.getText().equals(large.body));settle();assertEquals(large.body.length(),pad.page.getDocument().getLength());
        SwingUtilities.invokeAndWait(pad::goHome);await(()->!pad.onPage());settle();assertTrue(DesktopRecovery.all(pad.store).isEmpty());
        SwingUtilities.invokeAndWait(()->pad.open(large.id));await(()->pad.page.getText().equals(large.body));settle();
        SwingUtilities.invokeAndWait(()->{pad.page.append("Last new words");pad.goHome();});await(()->!pad.onPage());settle();
        assertEquals(large.body+"Last new words",DesktopRecovery.get(pad.store,large.id).body());assertEquals(large.body,pad.store.get(large.id).body);
    }
    @Test public void autosaveShortcutOpenCreateAndExitKeepEachCompleteEdit() throws Exception {
        String id=pad.store.latest().id;
        NoteStore.Note other=new NoteStore.Note();other.book=Things.HOME;other.title="Other synthetic note";other.body="Saved other writing";pad.store.save(other);
        SwingUtilities.invokeAndWait(()->pad.title.setText("a".repeat(161)));
        await(()->DesktopRecovery.get(pad.store,id)!=null);settle();
        assertEquals(161,DesktopRecovery.get(pad.store,id).title().length());
        SwingUtilities.invokeAndWait(()->{pad.page.setText("Shortcut keeps this complete body");pad.frame.getRootPane().getActionMap().get("control S").actionPerformed(new ActionEvent(pad.frame,0,"control S"));});
        await(()->DesktopRecovery.get(pad.store,id).body().equals("Shortcut keeps this complete body"));settle();
        SwingUtilities.invokeAndWait(()->{pad.page.setText("Opening another note keeps this too");pad.open(other.id);});
        await(()->pad.page.getText().equals("Saved other writing"));settle();assertEquals("Opening another note keeps this too",DesktopRecovery.get(pad.store,id).body());
        SwingUtilities.invokeAndWait(()->{pad.page.setText("b".repeat(24001));pad.newNoteIn(Things.HOME);});
        await(()->pad.onPage()&&pad.page.getText().isEmpty());settle();assertEquals(24001,DesktopRecovery.get(pad.store,other.id).body().length());
        String created=pad.store.latest().id;
        SwingUtilities.invokeAndWait(()->{pad.page.setText("exit "+"c".repeat(24001));pad.save(()->pad.shutdown(false));});
        await(()->!pad.frame.isDisplayable());
        var context=new org.mininotes.desktop.platform.content.Context(folder.toFile());
        try(NoteStore reopened=new NoteStore(context)){assertEquals("exit "+"c".repeat(24001),DesktopRecovery.get(reopened,created).body());}
    }
    @Test public void homeFractionAndHiddenFootSurviveResizeAndFractionalWheel() throws Exception {
        var left=pad.store.addCollection("Left page");var right=pad.store.addCollection("Right page");
        var rows=pad.store.contents(Things.HOME);pad.store.placeOnPages(rows,java.util.Map.of(left.id,new Layout.Spot(-1,0,0,0),right.id,new Layout.Spot(1,0,0,0)));
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle();
        SwingUtilities.invokeAndWait(()->{pad.home.grid.showPage(-1,0);pad.home.grid.scrollBy(pad.home.grid.pageW()/3,0);});
        double before=logicalX();int rowHeight=DesktopHome.HIGH;
        SwingUtilities.invokeAndWait(()->pad.frame.setSize(1280,720));settle();assertEquals(before,logicalX(),.02);
        SwingUtilities.invokeAndWait(()->pad.showTree(true));settle();assertEquals("tree folding keeps fractional Home",before,logicalX(),.02);SwingUtilities.invokeAndWait(()->pad.showTree(false));settle();assertEquals(before,logicalX(),.02);
        SwingUtilities.invokeAndWait(()->{assertFalse(pad.home.foot.isVisible());Rectangle plus=SwingUtilities.convertRectangle(pad.home.desk,pad.home.plus.getBounds(),pad.frame.getLayeredPane());assertTrue("plus stays clear of floating status",plus.y+plus.height<=pad.statusStrip.getY());shoot("home-partial-negative");assertEquals(pad.home.desk.getHeight(),pad.home.scroll.getHeight());assertEquals(rowHeight,pad.home.grid.cellBounds(1,0).y-pad.home.grid.cellBounds(0,0).y);pad.home.pages.up();});settle();
        SwingUtilities.invokeAndWait(()->{assertFalse(pad.home.foot.isVisible());assertEquals(pad.home.desk.getHeight(),pad.home.pages.getHeight());shoot("home-overview");pad.home.pages.away();pad.home.grid.showPage(0,0);});settle();
        int x=pad.home.scroll.getViewport().getViewPosition().x;
        SwingUtilities.invokeAndWait(()->{for(int n=0;n<10;n++)for(MouseWheelListener listener:pad.home.grid.getMouseWheelListeners())listener.mouseWheelMoved(new MouseWheelEvent(pad.home.grid,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),InputEvent.SHIFT_DOWN_MASK,20,20,20,20,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,3,0,.02));});
        assertEquals("subpixel wheel deltas accumulate",10,pad.home.scroll.getViewport().getViewPosition().x-x,1);
        SwingUtilities.invokeAndWait(()->pad.home.grid.returnCentre());await(()->Math.abs(logicalX())<.002);assertTrue(pad.home.foot.isVisible());
        assertEquals(Layout.page(-1,0),pad.store.contents(Things.HOME).stream().filter(t->t.id.equals(left.id)).findFirst().get().page);
    }
    @Test public void diagonalPanLocksOneAxisAndCentreLastRowClearsControls() throws Exception {
        var right=pad.store.addCollection("Pan right");
        pad.store.placeOnPages(pad.store.contents(Things.HOME),java.util.Map.of(right.id,new Layout.Spot(1,0,0,0)));
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle();
        SwingUtilities.invokeAndWait(()->{
            var grid=pad.home.grid;Point original=pad.home.scroll.getViewport().getViewPosition();Point start=grid.getLocationOnScreen();start.translate(400,300);Point end=new Point(start.x-60,start.y-12);
            MouseEvent press=event(grid,MouseEvent.MOUSE_PRESSED,start),drag=event(grid,MouseEvent.MOUSE_DRAGGED,end),release=event(grid,MouseEvent.MOUSE_RELEASED,end);
            for(MouseListener listener:grid.getMouseListeners())listener.mousePressed(press);
            for(MouseMotionListener listener:grid.getMouseMotionListeners())listener.mouseDragged(drag);
            for(MouseListener listener:grid.getMouseListeners())listener.mouseReleased(release);
            Point moved=pad.home.scroll.getViewport().getViewPosition();assertEquals(original.x+60,moved.x);assertEquals("diagonal jitter cannot pan vertically",original.y,moved.y);
            grid.showPage(0,0);Rectangle last=grid.cellBounds(grid.rows()-1,0);grid.scrollBy(0,last.y+last.height-pad.home.foot.getY()+10);Point bottom=new Point(last.x,last.y+last.height);bottom=SwingUtilities.convertPoint(grid,bottom,pad.home.desk);
            assertTrue("last row above centre controls: "+bottom.y+" < "+pad.home.foot.getY(),bottom.y<=pad.home.foot.getY());shoot("home-centre-last-row");
        });
    }
    @Test public void delayedDropRevealsOnlyTheDestinationAndEscapeCancels() throws Exception {
        var thing=pad.store.addCollection("Synthetic carry");SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle();
        DesktopHome.Tile tile=pad.home.grid.tiles.stream().filter(t->t.thing.id.equals(thing.id)).findFirst().get();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);pad.disk.submit(()->{entered.countDown();assertTrue(release.await(15,TimeUnit.SECONDS));return null;},done->{},e->{throw new AssertionError(e);});assertTrue(entered.await(10,TimeUnit.SECONDS));
        int[] to={1,pad.home.grid.columns()-1};Rectangle destination=pad.home.grid.cellBounds(to[0],to[1]);
        try{SwingUtilities.invokeAndWait(()->{
            Point pick=tile.getLocationOnScreen(),drop=new Point(destination.x+destination.width/2,destination.y+30);SwingUtilities.convertPointToScreen(drop,pad.home.grid);pick.translate(30,30);
            pad.home.carry.press(tile,event(tile,MouseEvent.MOUSE_PRESSED,pick));pad.home.carry.drag(event(tile,MouseEvent.MOUSE_DRAGGED,drop));
            assertTrue(pad.home.carry.carrying());assertTrue(tile.lifted);assertEquals(destination,pad.home.grid.landing);shoot("drag-preview");
            pad.home.carry.release(event(tile,MouseEvent.MOUSE_RELEASED,drop));assertFalse(tile.lifted);assertEquals("shown at the destination before persistence",destination,tile.getBounds());shoot("drop-before-persistence");
        });}finally{release.countDown();}settle();assertEquals(Layout.cell(to[0],to[1]),pad.store.contents(Things.HOME).stream().filter(t->t.id.equals(thing.id)).findFirst().get().cell);
        SwingUtilities.invokeAndWait(()->{
            DesktopHome.Tile again=pad.home.grid.tiles.stream().filter(t->t.thing.id.equals(thing.id)).findFirst().get();Point pick=again.getLocationOnScreen();pick.translate(30,30);Point move=new Point(pick.x+20,pick.y+10);
            pad.home.carry.press(again,event(again,MouseEvent.MOUSE_PRESSED,pick));pad.home.carry.drag(event(again,MouseEvent.MOUSE_DRAGGED,move));
            try{var f=pad.home.carry.getClass().getDeclaredField("escape");f.setAccessible(true);KeyEventDispatcher escape=(KeyEventDispatcher)f.get(pad.home.carry);assertTrue(escape.dispatchKeyEvent(new KeyEvent(again,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,KeyEvent.VK_ESCAPE,(char)27)));}catch(Exception e){throw new RuntimeException(e);}
            assertFalse(pad.home.carry.carrying());assertFalse(again.lifted);assertFalse(pad.onPage());
        });settle();assertEquals(Layout.cell(to[0],to[1]),pad.store.contents(Things.HOME).stream().filter(t->t.id.equals(thing.id)).findFirst().get().cell);
    }
    @Test public void failedPlacementReloadsPersistedCellAndHiddenFootCannotTakeADrop() throws Exception {
        var thing=pad.store.addCollection("Rollback carry");
        pad.store.placeOnPages(pad.store.contents(Things.HOME),java.util.Map.of(thing.id,new Layout.Spot(1,0,0,0)));
        SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();});settle();SwingUtilities.invokeAndWait(()->pad.home.grid.showPage(1,0));settle();
        pad.store.getWritableDatabase().execSQL("CREATE TRIGGER refuse_place BEFORE UPDATE OF cell ON things BEGIN SELECT RAISE(ABORT,'test placement failure'); END");
        DesktopHome.Tile tile=pad.home.grid.tiles.stream().filter(t->t.thing.id.equals(thing.id)).findFirst().get();
        Rectangle original=tile.getBounds();
        SwingUtilities.invokeAndWait(()->{
            assertFalse(pad.home.foot.isVisible());
            Rectangle destination=pad.home.grid.cellBounds(pad.home.grid.rows()-1,pad.home.grid.columns()-2);
            // Scroll the whole last row above the viewport's bottom before dropping.
            pad.home.grid.scrollBy(0,DesktopHome.HIGH);destination=pad.home.grid.cellBounds(pad.home.grid.rows()-1,pad.home.grid.columns()-2);
            Point pick=tile.getLocationOnScreen();pick.translate(30,30);Point drop=new Point(destination.x+30,destination.y+30);SwingUtilities.convertPointToScreen(drop,pad.home.grid);
            pad.home.carry.press(tile,event(tile,MouseEvent.MOUSE_PRESSED,pick));pad.home.carry.drag(event(tile,MouseEvent.MOUSE_DRAGGED,drop));
            assertEquals("hidden foot leaves the last cell available",destination,pad.home.grid.landing);shoot("home-last-row-landing");pad.home.carry.release(event(tile,MouseEvent.MOUSE_RELEASED,drop));
        });settle();
        assertEquals(Layout.cell(0,0),pad.store.contents(Things.HOME).stream().filter(t->t.id.equals(thing.id)).findFirst().get().cell);
        SwingUtilities.invokeAndWait(()->{DesktopHome.Tile reloaded=pad.home.grid.tiles.stream().filter(t->t.thing.id.equals(thing.id)).findFirst().get();assertEquals(original,reloaded.getBounds());});
    }
    @Test public void recoveryActionIsAvailableAfterLeavingAndRendersItsCompleteOptions() throws Exception {
        SwingUtilities.invokeAndWait(()->{pad.title.setText("Synthetic recovery writing");pad.page.setText("tea 😀 ".repeat(5000));pad.goHome();});await(()->!pad.onPage());settle();
        assertEquals(1,DesktopRecovery.all(pad.store).size());CountDownLatch captured=new CountDownLatch(1);
        SwingUtilities.invokeLater(()->{
            javax.swing.Timer capture=new javax.swing.Timer(100,e->{for(Window w:pad.frame.getOwnedWindows())if(w instanceof JDialog dialog&&dialog.isShowing()&&dialog.getTitle().equals("Recovery writing")){
                ((javax.swing.Timer)e.getSource()).stop();try{
                    var scale=dialog.getGraphicsConfiguration().getDefaultTransform();var image=new java.awt.image.BufferedImage((int)Math.ceil(dialog.getWidth()*scale.getScaleX()),(int)Math.ceil(dialog.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.transform(scale);dialog.paint(g);g.dispose();Path path=Path.of("build","verification","adaptive","recovery-actions.png");Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"png",path.toFile());
                }catch(Exception error){throw new RuntimeException(error);}finally{dialog.dispose();captured.countDown();}
            }});capture.setRepeats(true);capture.start();pad.recoveryWriting();
        });assertTrue(captured.await(15,TimeUnit.SECONDS));assertEquals(1,DesktopRecovery.all(pad.store).size());
    }
    @Test public void renderedSizesAndRewrappedReadingKeepTheirCharacterAnchor() throws Exception {
        String body=java.util.stream.IntStream.rangeClosed(1,150).mapToObj(i->"Synthetic line "+i+": Tea, café and room to write. This wraps when the window is narrower.").collect(java.util.stream.Collectors.joining("\n"));
        SwingUtilities.invokeAndWait(()->{pad.title.setText("Synthetic reading");pad.page.setText(body);pad.save(null);});await(()->pad.store.latest().body.equals(body));settle();
        SwingUtilities.invokeAndWait(()->{pad.title.requestFocusInWindow();pad.page.holding(true);pad.page.setCaretPosition(0);try{var line=pad.page.modelToView2D(body.indexOf("Synthetic line 60:"));pad.paperScroll.getViewport().setViewPosition(new Point(0,(int)line.getY()+3));}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(2100);SwingUtilities.invokeAndWait(()->shoot("reading-before-resize"));int top=top();assertTrue("fixture must be reading away from the beginning: "+top,top>1000);SwingUtilities.invokeAndWait(()->pad.page.holding(false));
        SwingUtilities.invokeAndWait(()->pad.frame.setSize(820,520));settle();assertTrue("reading anchor after rewrap: "+top+" to "+top(),Math.abs(top-top())<110);SwingUtilities.invokeAndWait(()->shoot("reading-after-resize"));
        SwingUtilities.invokeAndWait(()->pad.page.select(100,150));
        for(int[] size:new int[][]{{640,480},{800,600},{1280,720},{1440,900}}){SwingUtilities.invokeAndWait(()->pad.frame.setSize(Math.max(size[0],pad.frame.getMinimumSize().width),Math.max(size[1],pad.frame.getMinimumSize().height)));settle();
            SwingUtilities.invokeAndWait(()->{assertEquals(100,pad.page.getSelectionStart());assertEquals(150,pad.page.getSelectionEnd());shoot("note-"+size[0]+"x"+size[1]);pad.goHome();});settle();
            SwingUtilities.invokeAndWait(()->{shoot("home-"+size[0]+"x"+size[1]);pad.open(pad.store.latest().id);});settle();SwingUtilities.invokeAndWait(()->pad.page.select(100,150));
        }
        SwingUtilities.invokeAndWait(()->pad.frame.setSize(820,520));settle();assertTrue(pad.compactNote);SwingUtilities.invokeAndWait(()->{pad.setTextSize(7);});settle();SwingUtilities.invokeAndWait(()->shoot("note-large-text"));
        SwingUtilities.invokeAndWait(()->pad.frame.setSize(1280,800));settle();assertFalse(pad.compactNote);
    }
    private double logicalX(){var grid=pad.home.grid;int[] span=Layout.span(grid.spots());return span[0]+pad.home.scroll.getViewport().getViewPosition().x/(double)grid.pageW();}
    private int top(){Rectangle view=pad.paperScroll.getViewport().getViewRect();return pad.page.viewToModel2D(new Point(view.x,view.y));}
    private static MouseEvent event(Component tile,int type,Point screen){return new MouseEvent(tile,type,System.currentTimeMillis(),0,30,30,screen.x,screen.y,1,false,MouseEvent.BUTTON1);}
    private void shoot(String name){try{var scale=pad.frame.getGraphicsConfiguration().getDefaultTransform();var image=new java.awt.image.BufferedImage((int)Math.ceil(pad.frame.getWidth()*scale.getScaleX()),(int)Math.ceil(pad.frame.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.transform(scale);pad.frame.paint(g);g.dispose();Path path=Path.of("build","verification","adaptive",name+".png");Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private void settle() throws Exception{for(int n=0;n<3;n++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});Thread.sleep(120);}}
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
