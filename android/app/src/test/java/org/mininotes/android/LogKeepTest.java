package org.mininotes.android;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** The phone's own log in a file (decision 324): only this app's lines, each once, in two files that roll at a megabyte. */
public class LogKeepTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    private static final String OURS="11-04 18:02:03.123  4321  4350 I Mininotes/Post: sent to a contact, revision 133, whole, 5694 bytes: OK";
    private static final String OTHER="11-04 18:02:03.200  4321  4351 D OpenGLRenderer: frame";
    private static final String CRASH="11-04 18:02:04.000  4321  4321 E AndroidRuntime: FATAL EXCEPTION: main";
    private static final String WARNED="11-04 18:02:05.000  4321  4360 W System: a resource failed to call close";

    @Test public void onlyThisAppsLinesAndWhatWentWrongAreKept() {
        assertTrue(LogKeep.ours(OURS));
        assertTrue(LogKeep.ours(CRASH));
        assertFalse(LogKeep.ours(OTHER));
        assertFalse(LogKeep.ours(WARNED));
        assertFalse(LogKeep.ours("--------- beginning of main"));
        assertFalse(LogKeep.ours(""));
        assertEquals("11-04 18:02:03.123",LogKeep.stamp(OURS));
    }

    @Test public void eachLineIsKeptOnce() {
        List<String> first=LogKeep.after(List.of(OURS,OTHER,CRASH),"");
        assertEquals(List.of(OURS,CRASH),first);
        String last=LogKeep.stamp(CRASH);
        String later="11-04 18:03:00.000  4321  4350 I Mininotes/Node: heard 342 bytes for this app";
        assertEquals(List.of(later),LogKeep.after(List.of(OURS,OTHER,CRASH,later),last));
    }

    @Test public void theFilesRollAtAMegabyteAndBothAreHandedOver() throws Exception {
        File folder=new File(temp.getRoot(),"logs");
        List<String> many=new ArrayList<>();
        for(int i=0;i<4000;i++)many.add(OURS+" #"+i);
        for(int round=0;round<12;round++)LogKeep.add(folder,many);
        assertTrue(LogKeep.now(folder).length()<=LogKeep.MOST);
        assertTrue(LogKeep.older(folder).isFile());
        assertTrue(LogKeep.older(folder).length()<=LogKeep.MOST);
        String whole=new String(LogKeep.whole(folder),StandardCharsets.UTF_8);
        assertTrue(whole.endsWith("#3999\n"));
        assertTrue(whole.length()<=2*LogKeep.MOST);
    }
}
