// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/** A file travelling on its own (decision 92), there and back, inside its bounds, and never mistaken for anything else. */
public class SleeveTest {
    private static final String ID="3f2b8c4e-1d2a-4b5c-9e8f-7a6b5c4d3e2f";

    private static Sleeve.Sent sleeve(String manifest,boolean gone) {
        return new Sleeve.Sent(ID,"plan.png","image/png",4321L,manifest,false,"FILE",ID,
            List.of(new Parcel.Member("key-a","Mx-a","Ana",Sharing.Level.ADMIN.said(),77L,new byte[]{5,5}),
                    new Parcel.Member("key-b","Mx-b","Bea",Sharing.Level.READ.said(),78L)),
            true,gone);
    }

    @Test public void thereAndBack() throws IOException {
        Sleeve.Sent in=Sleeve.open(Sleeve.wrap(sleeve("where-the-pieces-are",false)));
        assertNotNull(in);
        assertEquals(ID,in.id);
        assertEquals("plan.png",in.name);
        assertEquals("image/png",in.kind);
        assertEquals(4321L,in.bytes);
        assertEquals("where-the-pieces-are",in.manifest);
        assertFalse(in.writes);
        assertEquals("FILE",in.scope);
        assertEquals(ID,in.target);
        assertEquals(2,in.members.size());
        assertEquals("Ana",in.members.get(0).name);
        assertEquals(Sharing.Level.ADMIN.said(),in.members.get(0).level);
        assertEquals(77L,in.members.get(0).changed);
        assertArrayEquals(new byte[]{5,5},in.members.get(0).agreement);
        assertEquals(0,in.members.get(1).agreement.length);
        assertTrue(in.answer);
        assertFalse(in.gone);
        assertTrue("gone for everybody says so",Sleeve.open(Sleeve.wrap(sleeve("",true))).gone);
    }

    /** What a later build adds goes after the end, where this one stops reading. */
    @Test public void whatALaterBuildAddsIsLeftAlone() throws IOException {
        byte[] whole=Sleeve.wrap(sleeve("m",false));
        byte[] longer=Arrays.copyOf(whole,whole.length+9);
        Sleeve.Sent in=Sleeve.open(longer);
        assertNotNull(in);assertEquals(ID,in.id);assertEquals("m",in.manifest);
    }

    /** Since 0.2.027 (decision 93): whose it is, and when its name and its bytes were last changed, there and back. */
    @Test public void itsTimeOnTempTravelsAndAnOlderOneHasNone() throws IOException {
        // Decision 95: a file let go on Temp says when it is to be gone, to the sender's own devices; a 0.2.027 or 0.2.028
        // sleeve stops before it, and is read as not temporary.
        Sleeve.Sent s=sleeve("m",false);
        Sleeve.Sent timed=new Sleeve.Sent(s.id,s.name,s.kind,s.bytes,s.manifest,true,s.scope,s.target,s.members,true,false,
            "Mx-owner","key-owner",0,0,1_800_000_000_000L);
        byte[] whole=Sleeve.wrap(timed);
        assertEquals(1_800_000_000_000L,Sleeve.open(whole).until);
        assertEquals("kept when it stops saying where the pieces are",1_800_000_000_000L,Sleeve.open(whole).unsaid().until);
        Sleeve.Sent older=Sleeve.open(Arrays.copyOf(whole,whole.length-SINCE_0_2_029));
        assertNotNull(older);assertEquals(0,older.until);assertEquals("Mx-owner",older.owner);
    }

    @Test public void itsOwnerAndItsChangesTravel() throws IOException {
        Sleeve.Sent s=sleeve("m",false);
        Sleeve.Sent named=new Sleeve.Sent(s.id,s.name,s.kind,s.bytes,s.manifest,true,s.scope,s.target,s.members,true,false,
            "Mx-owner","key-owner",1_700_000_000_123L,1_700_000_000_456L);
        Sleeve.Sent in=Sleeve.open(Sleeve.wrap(named));
        assertNotNull(in);
        assertTrue(in.writes);
        assertEquals("Mx-owner",in.owner);assertEquals("key-owner",in.ownerKey);
        assertEquals(1_700_000_000_123L,in.named);assertEquals(1_700_000_000_456L,in.replaced);
        assertEquals("kept when it stops saying where the pieces are","Mx-owner",in.unsaid().owner);
        assertFalse(in.quiet().answer);assertEquals(1_700_000_000_456L,in.quiet().replaced);
    }

    /** One from 0.2.026, which stops after saying whether it is gone: read as it was, nobody named and nothing changed. */
    @Test public void oneFromBeforeIsReadAsItWas() throws IOException {
        byte[] whole=Sleeve.wrap(sleeve("m",true));
        byte[] before=Arrays.copyOf(whole,whole.length-SINCE_0_2_027);
        Sleeve.Sent in=Sleeve.open(before);
        assertNotNull(in);assertTrue(in.gone);
        assertEquals("",in.owner);assertEquals("",in.ownerKey);assertEquals(0,in.named);assertEquals(0,in.replaced);
    }

    /** How many bytes the field since 0.2.029 takes: when it is to be gone (decision 95). */
    private static final int SINCE_0_2_029=8;
    /** How many bytes the fields since 0.2.027 take with nobody named as owner: two empty fields, two times, then the 0.2.029 one. */
    private static final int SINCE_0_2_027=4+4+8+8+SINCE_0_2_029;

    @Test public void halfOfOneIsNotOne() throws IOException {
        byte[] whole=Sleeve.wrap(sleeve("m",true));
        // Cut anywhere but where a 0.2.026 sleeve ends, which is a whole one of those (see oneFromBeforeIsReadAsItWas).
        int before=whole.length-SINCE_0_2_027,between=whole.length-SINCE_0_2_029;
        for(int cut=Sleeve.MAGIC.length;cut<whole.length;cut++)if(cut!=before&&cut!=between)assertNull("cut at "+cut,Sleeve.open(Arrays.copyOf(whole,cut)));
        assertNull(Sleeve.open(null));assertNull(Sleeve.open(new byte[0]));assertNull(Sleeve.open(Sleeve.MAGIC));
    }

    @Test public void itsBoundsAreKept() throws IOException {
        // A field that says it is longer than it may be, or a list of more people than any list holds, is not read.
        byte[] whole=Sleeve.wrap(sleeve("m",false));
        byte[] tooLong=whole.clone();
        // The id's length, just after the magic and the flag.
        tooLong[Sleeve.MAGIC.length+1]=0x7f;
        assertNull(Sleeve.open(tooLong));
        Sleeve.Sent none=new Sleeve.Sent("","Nameless","",0L,"",false,"","",List.of(),true,false);
        assertNull("a file with no id is not one",Sleeve.open(Sleeve.wrap(none)));
        Sleeve.Sent negative=new Sleeve.Sent(ID,"x","",-1L,"",false,"FILE",ID,List.of(),true,false);
        assertNull("nor one of less than nothing",Sleeve.open(Sleeve.wrap(negative)));
        // A manifest too long for an envelope stops saying where the pieces are, and is still the file.
        Sleeve.Sent heavy=sleeve("m".repeat(5000),false);
        byte[] fits=Sleeve.wrap(heavy,Sleeve.wrap(heavy).length-1);
        assertEquals("",Sleeve.open(fits).manifest);assertEquals(ID,Sleeve.open(fits).id);
        assertArrayEquals(Sleeve.wrap(heavy),Sleeve.wrap(heavy,Sleeve.wrap(heavy).length));
    }

    @Test public void neitherACartonNorAParcelNorANote() throws IOException {
        byte[] whole=Sleeve.wrap(sleeve("m",false));
        assertNull("a carton is never one of these",Sleeve.open(Carton.wrap(new Carton.Sent("x-9","Mondays","",null,0,0L,List.of(),false,"","",List.of(),true,null,0L))));
        assertNull(Sleeve.open(Parcel.wrap(new Parcel.Sent("c","C","b","B","t","text",true))));
        assertNull(Sleeve.open("plain text written the oldest way".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertNull("nor is it one of them",Carton.open(whole));
        assertNull(Parcel.open(whole));
        assertNull(Drop.open(whole));
        assertEquals("never taken for an answer",0,Receipt.open(whole));
        assertFalse("not Sealed's magic either",Arrays.equals(Sleeve.MAGIC,Sealed.MAGIC));
    }
}
