package org.mininotes.android;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;

/**
 * The same note at the same revision, sent again and again while its answer waited (Graphene, 2026-10-17): taken as
 * had, with nothing written. Synthetic notes only.
 */
public class SameCopyAgainTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String PHONE="MxPhoneFixture@127.0.0.1:9301";

    @Test public void theSameRevisionAgainChangesNothingAndCountsAsHad() throws Exception {
        try(NoteStore store=new NoteStore(new Context(temp.newFolder("pc")))) {
            String book=store.someBook();
            store.pairedWith(PHONE,"Test phone",true,new byte[]{1,2,3},new byte[]{4,5,6});
            assertEquals(Arriving.What.NEW,store.landed("n",PHONE,7,"List","eggs\nbread",book).what);
            long updated=store.get("n").updated;
            int versions=store.versions("n").size();
            Thread.sleep(5);
            for(int i=0;i<15;i++) {
                Arriving.Decision said=store.landed("n",PHONE,7,"List","eggs\nbread",book,3L);
                assertEquals("had, which is what stops them sending it",Arriving.What.NEWER,said.what);
                assertEquals("eggs\nbread",said.text);
            }
            assertEquals("the note was not written again",updated,store.get("n").updated);
            assertEquals("no version added",versions,store.versions("n").size());
            assertEquals(7,store.get("n").revision);
            // A different text at the same revision is still weighed as before.
            assertNotEquals(Arriving.What.NEW,store.landed("n",PHONE,7,"List","eggs\nbread\nmilk",book).what);
        }
    }
}
