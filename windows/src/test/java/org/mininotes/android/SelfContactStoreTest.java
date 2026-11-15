package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A device never kept as its own contact (the Pro, 2026-11-04; decision 324): a list of who has a note, taken in before the
 * Pro knew its own key, made it a contact of itself, and it sent to itself every 30 s. Synthetic devices, no network.
 */
public class SelfContactStoreTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String ME_AT="MxSelfFixture@127.0.0.1:9831",PC_AT="MxSelfPcFixture@127.0.0.1:9832";

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at,boolean knowsItself) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            if(knowsItself)store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myName=name;store.myAddress=at;
        }
        byte[] signing() throws Exception{return keys.signing().getPublic().getEncoded();}
        byte[] agreement() throws Exception{return keys.agreement().getPublic().getEncoded();}
    }

    @Test public void aSelfRowKeptBeforeTheKeyWasKnownGoesOnceItIs() throws Exception {
        Device me=new Device("Test phone",ME_AT,false);
        // As it went on the Pro: its own entry on a list, with its own key not known yet.
        me.store.pairedWith(ME_AT,"Test phone",false,me.agreement(),me.signing());
        me.store.addShare(new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,ME_AT,true));
        assertNotNull(me.store.address(ME_AT));
        me.store.knowMyself(me.signing());
        assertNull("itself is no contact",me.store.address(ME_AT));
        assertTrue("nothing shared with itself",me.store.shares().stream().noneMatch(r->ME_AT.equals(r.address)));
    }

    @Test public void knowingItselfItIsNeverKeptAgain() throws Exception {
        Device me=new Device("Test phone",ME_AT,true);
        assertFalse("not linked from a list",me.store.linkThrough(ME_AT,"Test phone",me.agreement(),me.signing(),false));
        try{me.store.pairedWith(ME_AT,"Test phone",false,me.agreement(),me.signing());fail("its own code taken");}
        catch(IllegalArgumentException said){assertEquals("That is this device's own code.",said.getMessage());}
        assertNull(me.store.address(ME_AT));
        // Somebody else still is.
        Device pc=new Device("Test PC",PC_AT,true);
        assertTrue(me.store.linkThrough(PC_AT,"Test PC",pc.agreement(),pc.signing(),false));
    }

    @Test public void anArrivalTellsTheNotebookWhoItIsFirst() throws Exception {
        Device me=new Device("Test phone",ME_AT,false),pc=new Device("Test PC",PC_AT,true);
        me.store.pairedWith(PC_AT,"Test PC",true,pc.agreement(),pc.signing());
        byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.TAKES_FILES),pc.keys.signing(),me.keys.agreement().getPublic());
        Post.arrived(me.context,me.store,me.keys,sealed);
        assertEquals(Base64.getEncoder().encodeToString(me.signing()),me.store.mySigningKey);
        assertTrue(me.store.isMine(Base64.getEncoder().encodeToString(me.signing())));
    }

    @Test public void aListIsNotLinkedFromWhileThisDeviceDoesNotKnowItsOwnKey() throws Exception {
        Device me=new Device("Test phone",ME_AT,false);
        List<Parcel.Member> list=List.of(new Parcel.Member(Base64.getEncoder().encodeToString(me.signing()),ME_AT,"Test phone",Sharing.Level.WRITE.said(),1L,me.agreement()));
        assertTrue(Linking.toLink(list,true,"",Set.of()).isEmpty());
    }
}
