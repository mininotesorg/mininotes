// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

/**
 * What both apps say of a stamp (decision 218): the line at the top of a stamped note, which stamp it stands by, how often the
 * watch looks, which record on Minima stands for a stamp, and the details copied. Pure logic, shared by the phone and the PC.
 */
public class StampLineTest {
    private static NoteStore.StampRow row(long n,String hash,long at,String state,long block) {
        return new NoteStore.StampRow(n,"note-1","note","Lease",hash,at,"req-"+n,"Hash anchored successfully.","0xADDRESS",state,block>0?3:0,0,block,
            block>0?"0xTXPOW":"",block>0?"0xTX":"",block>0?"2026-10-19T10:00:00Z":"",true,"","");
    }

    @Test public void theNoteLineSaysHowFarAndWhenItChanged() throws Exception {
        long at=java.time.Instant.parse("2026-10-19T09:58:00Z").toEpochMilli();
        NoteStore.StampRow waiting=row(1,"aa",at,NoteStore.STAMP_WAITING,0),landed=row(2,"bb",at,NoteStore.STAMP_CONFIRMED,777001),lost=row(3,"cc",at,NoteStore.STAMP_UNSEEN,0);
        assertEquals("Stamping, waiting for Minima",Integritas.noteLine(waiting,false));
        assertTrue(Integritas.noteLine(landed,false).startsWith("Stamped on Minima, "));
        assertTrue(Integritas.noteLine(landed,false).endsWith(", block 777001"));
        assertTrue(Integritas.noteLine(lost,false).endsWith("not found on Minima yet"));
        assertTrue(Integritas.noteLine(landed,true).startsWith("Changed since it was stamped on "));
        assertTrue(waiting.waiting());assertTrue(landed.confirmed());assertFalse(lost.waiting());assertFalse(lost.confirmed());
    }

    @Test public void aNoteStandsByTheStampOfWhatItSaysNow() {
        NoteStore.StampRow newer=row(5,"new",2000,NoteStore.STAMP_WAITING,0),older=row(4,"old",1000,NoteStore.STAMP_CONFIRMED,10);
        List<NoteStore.StampRow> all=List.of(newer,older);
        assertSame("its text is the older version's",older,Integritas.standing(all,"old"));
        assertSame("changed from both: the newest",newer,Integritas.standing(all,"other"));
        assertNull(Integritas.standing(List.of(),"x"));
    }

    @Test public void theExplorerIsLinkedWhereItShowsTheThing() {
        // Decision 224: the explorer's own forms, a transaction by its txpow id and a block or an address by its search.
        String txpow="0x00000019299334C6E083683292728F7F1459FCF14A16623F3CE2A77BA343E57F";
        assertEquals("https://explorer.minima.global/transactions/"+txpow,Integritas.txpowLink(txpow));
        assertEquals("https://explorer.minima.global/search?q=1906787",Integritas.blockLink(1906787));
        assertEquals("https://explorer.minima.global/search?q=MxG0SYNTHETIC0ADDRESS",Integritas.addressLink("MxG0SYNTHETIC0ADDRESS"));
        assertNull(Integritas.txpowLink(""));assertNull(Integritas.txpowLink("0xTX"));assertNull(Integritas.blockLink(0));assertNull(Integritas.addressLink("a b"));
        NoteStore.StampRow landed=new NoteStore.StampRow(9,"f","file","scan.pdf","aa",1000,"r","","0xADDRESS",NoteStore.STAMP_CONFIRMED,1,0,1906787,txpow,"0xDECA",
            "2026-01-21T09:55:48.726Z",false,"0xB2E182C1AA0635BDD1154EEB9C6512CA73568D09B59DE3E1831517241ABC5A96","0x00000370B400925F2B5F196D212ACCCF6A8E993A28F90AACAC2520A5C3CCFCF4");
        String details=Integritas.details(landed);
        for(String part:new String[]{"Minima Explorer: https://explorer.minima.global/transactions/"+txpow,"NFT coin id: 0xB2E182C1","NFT on the Minima Explorer: https://explorer.minima.global/transactions/0x00000370B4"})
            assertTrue(part,details.contains(part));
    }

    @Test public void lookingBacksOffAndStops() {
        assertEquals(15,Integritas.waitSeconds(0));
        long total=0;for(int i=0;i<Integritas.MOST;i++){assertTrue(Integritas.waitSeconds(i+1)>=Integritas.waitSeconds(i));total+=Integritas.waitSeconds(i);}
        assertTrue("about two hours, not forever: "+total,total>=90*60&&total<=3*3600);
    }

    @Test public void theRecordThatStandsAndTheDetails() {
        long at=java.time.Instant.parse("2026-10-19T10:00:00Z").toEpochMilli();
        Integritas.Block before=new Integritas.Block(100,"2026-01-01T00:00:00Z","t1","x1"),now=new Integritas.Block(200,"2026-10-19T10:02:00Z","t2","x2");
        assertEquals(200,Integritas.chosen(List.of(before,now),at).number());
        assertEquals("there before: the oldest proof",100,Integritas.chosen(List.of(before),at).number());
        assertNull(Integritas.chosen(List.of(),at));
        String said=Integritas.details(row(2,"bb",at,NoteStore.STAMP_CONFIRMED,777001));
        for(String part:new String[]{"SHA-256: bb","Integritas request: req-2","Minima address: 0xADDRESS","Block: 777001","Transaction (txpow): 0xTXPOW","Transaction id: 0xTX"})assertTrue(part,said.contains(part));
        assertTrue(Integritas.details(row(1,"aa",at,NoteStore.STAMP_WAITING,0)).contains("State: waiting for Minima"));
    }
}
