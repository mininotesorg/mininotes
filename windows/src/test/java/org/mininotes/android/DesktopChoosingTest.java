package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.datatransfer.DataFlavor;
import java.nio.file.*;
import java.util.*;

/**
 * Several icons chosen at once (decision 292), the rules alone: Ctrl+click in and out, Shift+click the run along the grid's
 * reading order from the icon last clicked, a plain click choosing nothing, and what a menu on them offers.
 */
public class DesktopChoosingTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void ctrlTakesInAndOut() {
        DesktopChoosing c=new DesktopChoosing();
        c.toggle("a");c.toggle("c");
        assertEquals(List.of("a","c"),c.chosen());
        c.toggle("a");
        assertEquals(List.of("c"),c.chosen());
        assertFalse(c.has("a"));assertTrue(c.has("c"));
    }

    @Test public void shiftChoosesTheRunFromTheLastClicked() {
        List<String> order=List.of("a","b","c","d","e");
        DesktopChoosing c=new DesktopChoosing();
        // A plain click chooses nothing (it opens), but the run starts there.
        c.plain("b");assertTrue(c.isEmpty());
        c.run(order,"d");
        assertEquals(List.of("b","c","d"),c.chosen());
        // Another Shift+click moves the far end only, backwards too.
        c.run(order,"a");
        assertEquals(List.of("a","b"),c.chosen());
        // Ctrl+click moves the start: the next run goes from there.
        c.toggle("e");c.run(order,"c");
        assertEquals(List.of("c","d","e"),c.chosen());
        // With no start, the one clicked alone, and the run starts there.
        DesktopChoosing fresh=new DesktopChoosing();fresh.run(order,"c");
        assertEquals(List.of("c"),fresh.chosen());fresh.run(order,"e");
        assertEquals(List.of("c","d","e"),fresh.chosen());
        // Not in the order (a place, say): nothing changes.
        fresh.run(order,"bin");assertEquals(3,fresh.size());
    }

    @Test public void clearAndWhatIsNoLongerThere() {
        DesktopChoosing c=new DesktopChoosing();
        c.toggle("a");c.toggle("b");c.toggle("c");
        c.keepOnly(List.of("a","c","z"));
        assertEquals(List.of("a","c"),c.chosen());
        // The start went with what went: a run then starts from the one clicked.
        c.toggle("b");c.toggle("b");c.keepOnly(List.of("a","c"));
        c.run(List.of("a","c"),"c");assertEquals(List.of("c"),c.chosen());
        c.clear();assertTrue(c.isEmpty());
        c.run(List.of("a","c"),"a");assertEquals(List.of("a"),c.chosen());
    }

    @Test public void theGridIsReadAlongItsRows() {
        Map<String,int[]> drawn=new LinkedHashMap<>();
        drawn.put("low",new int[]{1,0});drawn.put("right",new int[]{0,2});drawn.put("left",new int[]{0,0});drawn.put("nowhere",null);
        assertEquals(List.of("left","right","low"),DesktopChoosing.order(drawn));
        DesktopChoosing c=new DesktopChoosing();c.toggle("low");c.toggle("left");
        assertEquals(List.of("left","low"),c.inOrder(DesktopChoosing.order(drawn)));
    }

    @Test public void theMenuOffersWhatAppliesToAll() {
        NoteStore.Branch.Kind F=NoteStore.Branch.Kind.FILE,P=NoteStore.Branch.Kind.PAGE,C=NoteStore.Branch.Kind.COLLECTION;
        assertEquals(EnumSet.allOf(DesktopChoosing.Offer.class),DesktopChoosing.offers(List.of(F,F)));
        assertEquals(EnumSet.of(DesktopChoosing.Offer.BIN),DesktopChoosing.offers(List.of(F,P)));
        assertEquals(EnumSet.of(DesktopChoosing.Offer.BIN),DesktopChoosing.offers(List.of(C,P)));
        assertTrue(DesktopChoosing.offers(List.of()).isEmpty());
        assertTrue("a place is never among them",DesktopChoosing.offers(List.of(F,NoteStore.Branch.Kind.BIN)).isEmpty());
        assertFalse(DesktopChoosing.choosable(NoteStore.Branch.Kind.TEMP));
        assertTrue(DesktopChoosing.choosable(NoteStore.Branch.Kind.BOOK));
    }

    @Test public void copiesGoingOutAreNamedAsInTheApp() throws Exception {
        Path room=temp.newFolder("room").toPath();
        Path first=DesktopDragOut.free(room,"photo.jpg");assertEquals(room.resolve("photo.jpg"),first);
        Files.writeString(first,"x");
        assertEquals("a second of the same name gets a number",room.resolve("photo (2).jpg"),DesktopDragOut.free(room,"photo.jpg"));
        var list=DesktopDragOut.fileList(List.of(first.toFile()));
        assertTrue(list.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
        assertEquals(List.of(first.toFile()),list.getTransferData(DataFlavor.javaFileListFlavor));
    }
}
