// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The stamps card between the owner's own devices (decision 252): there and back whole, its bounds, and junk refused. Made up. */
public class StampCardTest {
    private static StampCard.Row row(String request,long at,String words) {
        return new StampCard.Row("note-"+at,Integritas.TITLED,"Synthetic note "+at,"ab12cd34ef"+at,at,request,"Stamp request received",
            "0xFAKEADDRESS",NoteStore.STAMP_CONFIRMED,2374505,"0xTXPOW"+at,"0xTX"+at,"2026-10-25 10:00:00","Synthetic title",words,"0xNFT","0xNFTTX");
    }

    @Test public void everyFieldGoesThereAndBackWhole() {
        StampCard.Row one=row("req-1",1_700_000_000_000L,"Made-up words\nover two lines, \u00e9");
        StampCard.Row file=new StampCard.Row("file-1","file","budget.ods","ffee",1_700_000_000_500L,"","","",NoteStore.STAMP_WAITING,0,"","","",null,null,"","");
        byte[] wire=StampCard.wrap(List.of(one,file));
        assertTrue(StampCard.isCard(wire));
        assertEquals(2,StampCard.count(wire));
        List<StampCard.Row> back=StampCard.open(wire);
        assertNotNull(back);assertEquals(2,back.size());
        StampCard.Row b=back.get(0);
        assertEquals(one.id,b.id);assertEquals(one.kind,b.kind);assertEquals(one.name,b.name);assertEquals(one.hash,b.hash);assertEquals(one.at,b.at);
        assertEquals(one.request,b.request);assertEquals(one.said,b.said);assertEquals(one.address,b.address);assertEquals(one.state,b.state);
        assertEquals(one.block,b.block);assertEquals(one.txpow,b.txpow);assertEquals(one.tx,b.tx);assertEquals(one.recorded,b.recorded);
        assertEquals(one.title,b.title);assertEquals(one.words,b.words);assertEquals(one.nft,b.nft);assertEquals(one.nftTx,b.nftTx);
        assertTrue(b.confirmed());
        StampCard.Row f=back.get(1);
        assertNull("no stamped text kept",f.words);assertNull(f.title);assertEquals("",f.request);assertFalse(f.confirmed());
        assertEquals(1_700_000_000_500L,StampCard.newest(back));
        // None at all is a card too.
        assertEquals(List.of(),StampCard.open(StampCard.wrap(List.of())));
    }

    @Test public void theNewestThatFitGoAndATextTooLongGoesWithoutIt() {
        String big=new String(new char[90_000]).replace('\0','x');
        List<StampCard.Row> rows=new ArrayList<>();
        for(int at=10;at>0;at--)rows.add(row("req-"+at,at,big));
        byte[] wire=StampCard.wrap(rows);
        assertTrue(wire.length<=Envelope.MAX_TEXT);
        List<StampCard.Row> back=StampCard.open(wire);
        assertEquals("every stamp goes, most without their text",10,back.size());
        assertEquals("the newest first",10,back.get(0).at);
        assertNotNull(back.get(0).words);assertNotNull(back.get(1).words);assertNull(back.get(2).words);
        // A text larger than any envelope: the stamp goes all the same, without it.
        String huge=new String(new char[Envelope.MAX_TEXT+10]).replace('\0','y');
        List<StampCard.Row> alone=StampCard.open(StampCard.wrap(List.of(row("req-huge",5,huge))));
        assertEquals(1,alone.size());assertNull(alone.get(0).words);assertEquals("req-huge",alone.get(0).request);
        // A row this format cannot say is left off, not the card.
        StampCard.Row noHash=new StampCard.Row("x","file","n","",1,"r","","",NoteStore.STAMP_WAITING,0,"","","",null,null,"","");
        StampCard.Row longName=new StampCard.Row("x","file",new String(new char[StampCard.NAME_MOST+1]).replace('\0','n'),"h",1,"r","","",NoteStore.STAMP_WAITING,0,"","","",null,null,"","");
        assertEquals(1,StampCard.open(StampCard.wrap(List.of(noHash,longName,row("req-ok",3,null)))).size());
    }

    @Test public void junkHalfACardAndAnythingAfterItAreRefused() {
        byte[] wire=StampCard.wrap(List.of(row("req-1",1,"words"),row("req-2",2,null)));
        for(int cut=0;cut<wire.length;cut++)assertNull("cut at "+cut,StampCard.open(Arrays.copyOf(wire,cut)));
        assertNull("nothing after it",StampCard.open(Arrays.copyOf(wire,wire.length+1)));
        byte[] later=wire.clone();later[4]=2;
        assertNull("a later format",StampCard.open(later));
        byte[] many=wire.clone();many[5]=(byte)0x7f;
        assertNull("a count it does not hold",StampCard.open(many));
        StampCard.Row odd=new StampCard.Row("x","file","n","h",1,"r","","","lost",0,"","","",null,null,"","");
        assertNull("a state this build does not know",StampCard.open(StampCard.wrap(List.of(odd))));
        assertFalse(StampCard.isCard(Receipt.wrap(Receipt.STAMPS)));
        assertFalse(StampCard.isCard(KeyCard.MAGIC));assertFalse(StampCard.isCard(Groups.MAGIC));
        assertFalse(Groups.isCard(wire));assertFalse(KeyCard.isCard(wire));assertFalse(Persons.isCard(wire));
        assertNull(StampCard.open(null));assertNull(StampCard.open(new byte[]{'M','N','Q'}));
        // The rule of who is sent one.
        assertTrue(StampCard.goesTo(true,true,true));
        assertFalse(StampCard.goesTo(false,true,true));assertFalse(StampCard.goesTo(true,false,true));assertFalse(StampCard.goesTo(true,true,false));
    }
}
