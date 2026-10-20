package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.Files;
import org.mininotes.desktop.platform.content.Context;

/**
 * Bytes an earlier try left in place with no row never stop the file from coming (the owner, 2026-10-11: a screenshot let
 * go on Temp on the PC was "synced" but never reached the phone, which had its bytes and no row, and failed every try).
 */
public class FileLeftoverTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @Before public void open() throws Exception {store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();}
    @After public void close(){store.close();}

    @Test public void bytesLeftByAnEarlierTryAreClearedAndTheFileComes() throws Exception {
        String id=java.util.UUID.randomUUID().toString();
        byte[] plain="a screenshot, as bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        store.getWritableDatabase().execSQL("INSERT INTO incoming(id,note,origin,name,kind,bytes,manifest,tried,tries,declined,alone,owner,replaces,until,accepted)"
            +" VALUES(?,?,?,?,?,?,'',0,0,0,1,'',0,?,1)",new Object[]{id,Things.HOME,"MxLeftoverPc@127.0.0.1:9890","shot.png","image/png",plain.length,System.currentTimeMillis()+3_600_000L});
        // What the earlier try left: bytes where the file goes, and no row.
        java.io.File left=store.fileFor(id);left.getParentFile().mkdirs();Files.write(left.toPath(),new byte[]{1,2,3});
        assertNull(store.file(id));
        NoteStore.Incoming one=new NoteStore.Incoming(id,Things.HOME,"MxLeftoverPc@127.0.0.1:9890","shot.png","image/png",plain.length,"",0L,0);
        assertTrue("kept this time",store.fileArrived(one,plain));
        assertNotNull("with its row",store.file(id));
        assertTrue("temporary, as it was sent",store.untilOf(NoteStore.Branch.Kind.FILE,id)>0);
    }
}
