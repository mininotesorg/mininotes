// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

public class LaterTest {
    @After public void english(){Say.use("en");}

    @Test public void whatMissedADeviceIsKeptOnceUntilItGoesOrAMonthPasses() {
        Set<String> lines=Later.add(Set.of(),new Later.Owed(Later.Kind.PARLONS,"MxAna","MxPro",1000));
        // The same thing to the same device again: kept once, with when it first missed.
        lines=Later.add(lines,new Later.Owed(Later.Kind.PARLONS,"MxAna","MxPro",5000));
        lines=Later.add(lines,new Later.Owed(Later.Kind.CALLED,"","MxPro",2000));
        assertEquals(2,lines.size());
        List<Later.Owed> owed=Later.owed(lines,10_000);
        assertEquals(1000,owed.get(0).since);
        assertEquals("",owed.get(1).about);
        lines=Later.remove(lines,Later.Kind.PARLONS,"MxAna","MxPro");
        assertEquals(1,lines.size());
        assertTrue(Later.owed(lines,2000+Later.KEPT).isEmpty());
        assertTrue(Later.kept(Set.of("nonsense",new Later.Owed(Later.Kind.KEY,"integritas","MxPro",0).line()),1).size()==1);
    }

    @Test public void theOwnerIsToldItGoesByItself() {
        assertEquals("Saved on this PC. It goes to Pro and Laptop by itself once Mininotes is open there",
            Parlons.said(false,"this PC",List.of(),List.of("Pro","Laptop"),List.of()));
        Say.use("fr");
        assertEquals("Enregistré sur ce PC. Il part tout seul vers Pro dès que Mininotes y est ouvert",
            Parlons.said(false,"this PC",List.of(),List.of("Pro"),List.of()));
    }
}
