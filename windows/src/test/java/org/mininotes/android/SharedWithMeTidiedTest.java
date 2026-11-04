package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.UUID;
import org.mininotes.desktop.platform.content.Context;

/**
 * Shared with me holds what somebody else shared on its own, and what waits to be accepted, nothing else (decision 300): what came
 * from one of this owner's own devices, and what is in Temp, is taken out of it. Made-up devices and files only.
 */
public class SharedWithMeTidiedTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private NoteStore store;
    @After public void close(){if(store!=null)store.close();}
    @Before public void open(){store=new NoteStore(new Context(temp.getRoot()));store.getWritableDatabase();}

    private static final String MY_PHONE="MxTidyMyPhoneFixture@127.0.0.1:9901",ANA="MxTidyAnaFixture@127.0.0.1:9902";

    private String fileShown(String origin,long until) {
        String id=UUID.randomUUID().toString();
        store.keep(new NoteStore.Held(id,Things.HOME,"synthetic.jpg","image/jpeg",10,1_760_000_000_000L,NoteStore.Branch.Kind.COLLECTION));
        store.getWritableDatabase().execSQL("UPDATE files SET shown=?,origin=?,until=?,theirs=1 WHERE id=?",new Object[]{NoteStore.SHARED,origin,until,id});
        return id;
    }
    private String shownOf(String id) {
        try(var c=store.getReadableDatabase().rawQuery("SELECT shown FROM files WHERE id=?",new String[]{id})){return c.moveToFirst()?c.getString(0):null;}
    }

    @Test public void tempAndMyOwnDevicesLeaveSharedWithMeAndSomebodyElsesFileStays() throws Exception {
        Keys phone=new Keys(new Context(temp.newFolder("phone"))),ana=new Keys(new Context(temp.newFolder("ana")));
        store.pairedWith(MY_PHONE,"My phone",true,phone.agreement().getPublic().getEncoded(),phone.signing().getPublic().getEncoded());
        store.setMine(MY_PHONE,true);
        store.pairedWith(ANA,"Ana",false,ana.agreement().getPublic().getEncoded(),ana.signing().getPublic().getEncoded());
        long tomorrow=System.currentTimeMillis()+86_400_000L;
        String tempFromPhone=fileShown(MY_PHONE,tomorrow),fromPhone=fileShown(MY_PHONE,0),tempFromAna=fileShown(ANA,tomorrow),fromAna=fileShown(ANA,0);
        store.sharedWithMeBecomesAPlace();
        assertEquals("a Temp picture from my own phone: shown in Temp, not Shared with me","",shownOf(tempFromPhone));
        assertEquals("from my own phone, not in Temp: on Home","",shownOf(fromPhone));
        assertEquals("in Temp from somebody else: in Temp","",shownOf(tempFromAna));
        assertEquals("shared on its own by somebody else: stays",NoteStore.SHARED,shownOf(fromAna));
    }
}
