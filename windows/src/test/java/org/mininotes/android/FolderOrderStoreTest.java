package org.mininotes.android;

import static org.junit.Assert.*;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/** On a real notebook (decisions 336, 337): what came last stands last, a save does not move it, and each order holds. */
public class FolderOrderStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception {store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();}
    @After public void close(){store.close();}

    private NoteStore.Note note(String book,String title,String body) throws Exception {
        NoteStore.Note n=new NoteStore.Note();n.id=title;n.book=book;n.title=title;n.body=body;store.save(n);
        Thread.sleep(5);
        return n;
    }
    private List<String> shown(String folder) {
        List<String> names=new ArrayList<>();
        for(NoteStore.Branch one:store.inOrder(folder,store.contents(folder)))names.add(one.id);
        return names;
    }
    private long arrived(String id) {
        try(org.mininotes.desktop.platform.database.Cursor c=store.getReadableDatabase().rawQuery("SELECT arrived FROM notes WHERE id=?",new String[]{id})){c.moveToFirst();return c.getLong(0);}
    }

    @Test public void theLatestAddedStandsLastAndASaveDoesNotMoveIt() throws Exception {
        String folder=store.addCollection("F").id;
        NoteStore.Note first=note(folder,"first","a");note(folder,"second","b");note(folder,"third","c");
        assertEquals(List.of("first","second","third"),shown(folder));
        long was=arrived("first");assertTrue("a new note has its date",was>0);
        Thread.sleep(5);first.body="written again";store.save(first);
        assertEquals("a save is not an arrival",was,arrived("first"));
        assertEquals(List.of("first","second","third"),shown(folder));
    }

    @Test public void aThingMovedInStandsLastAndSoDoesOnePutInTemp() throws Exception {
        String from=store.addCollection("From").id,to=store.addCollection("To").id;
        note(from,"old","x");Thread.sleep(5);note(to,"there","y");Thread.sleep(5);
        store.moveInto(NoteStore.Branch.Kind.PAGE,"old",to);
        assertEquals(List.of("there","old"),shown(to));
        long moved=arrived("old");Thread.sleep(5);
        store.makeTemporary(NoteStore.Branch.Kind.PAGE,"there",System.currentTimeMillis()+3_600_000L);
        assertTrue("put in Temp is an arrival",arrived("there")>moved);
    }

    @Test public void eachOrderIsKeptForItsFolderAndPuttingAnIconByHandEndsIt() throws Exception {
        String folder=store.addCollection("F").id,other=store.addCollection("G").id;
        note(folder,"b","short");note(folder,"c","the longest of the three");note(folder,"a","mid dle");
        store.folderOrder(folder,FolderOrder.NAME);
        assertEquals(List.of("a","b","c"),shown(folder));
        assertEquals("the other folder is its own",List.of(),shown(other));
        store.folderOrder(folder,FolderOrder.SIZE);
        assertEquals(List.of("c","a","b"),shown(folder));
        for(NoteStore.Branch one:store.inOrder(folder,store.contents(folder)))assertEquals("the order decides, not a cell",Layout.NONE,one.cell);
        List<NoteStore.Branch> lines=store.contents(folder);
        store.place(lines,Map.of(lines.get(0).id,Layout.cell(2,1)));
        assertEquals("by hand: as it was put","",store.folderOrder(folder));
        assertEquals(Layout.cell(2,1),store.contents(folder).stream().filter(x->x.id.equals(lines.get(0).id)).findFirst().orElseThrow().cell);
    }

    @Test public void aClickInTheMarginChoosesTheWholeLine() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            Desktop.Paper page=new Desktop.Paper();
            page.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF,java.awt.Font.PLAIN,18));page.setLineWrap(true);page.setWrapStyleWord(true);
            page.setBorder(BorderFactory.createEmptyBorder(12,42,40,38));
            page.setText("First line\nSecond line of words\nThird");page.setSize(560,300);
            int rowHeight=page.getFontMetrics(page.getFont()).getHeight();
            assertEquals("the arrow of the margin is drawn for it",java.awt.Cursor.CUSTOM_CURSOR,Desktop.Paper.marginArrow().getType());
            assertTrue("the margin, beside a line",page.onMargin(new Point(20,12+rowHeight/2)));
            assertFalse("on the words",page.onMargin(new Point(60,12+rowHeight/2)));
            assertFalse("beside no line, under the words",page.onMargin(new Point(20,12+rowHeight*5)));
            page.chooseLine(new Point(20,12+rowHeight+rowHeight/2));
            assertEquals("the words, not the return","Second line of words",page.getSelectedText());
            page.chooseLine(new Point(20,12+2*rowHeight+rowHeight/2));
            assertEquals("the last line has no line end","Third",page.getSelectedText());
            // A long line as it is drawn: one row of it, whole, and the next row starts where this one stopped.
            page.setSize(200,400);page.setText("alpha beta gamma delta epsilon zeta eta theta iota kappa");
            page.chooseLine(new Point(20,12+rowHeight/2));
            String row=page.getSelectedText();
            assertTrue(row,row.startsWith("alpha")&&row.length()<page.getText().length());
            page.chooseLine(new Point(20,12+rowHeight+rowHeight/2));
            assertEquals("nothing dropped between two rows",row.length(),page.getSelectionStart());
        });
    }
}
