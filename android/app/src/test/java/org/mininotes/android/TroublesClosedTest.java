package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/** The strip at the top closed with its cross (decision 296): what it showed is said no more, a new trouble is. Made-up names. */
public class TroublesClosedTest {
    private static Troubles.Trouble t(String at,Troubles.Kind kind){return new Troubles.Trouble(at,"Synthetic "+at,kind,1,false);}

    @Test public void whatWasClosedIsNotSaidAgainButSomethingNewIs() {
        Troubles.Trouble ana=t("MxAna@1",Troubles.Kind.STUCK),ben=t("MxBen@2",Troubles.Kind.LINKING);
        Set<String> closed=new HashSet<>(Set.of(Troubles.key(ana)));
        assertEquals(List.of(ben),Troubles.open(List.of(ana,ben),closed));
        assertTrue(Troubles.open(List.of(ana),closed).isEmpty());
        // The same person in another trouble is new.
        assertEquals(1,Troubles.open(List.of(t("MxAna@1",Troubles.Kind.UPDATE)),closed).size());
        assertEquals(2,Troubles.open(List.of(ana,ben),null).size());
    }

    @Test public void oneFixedAndBackLaterIsSaidAgain() {
        Troubles.Trouble ana=t("MxAna@1",Troubles.Kind.STUCK);
        Set<String> closed=Set.of(Troubles.key(ana));
        assertEquals(closed,Troubles.stillClosed(List.of(ana),closed));
        Set<String> after=Troubles.stillClosed(List.of(),closed);
        assertTrue("put right: forgotten as closed",after.isEmpty());
        assertEquals(List.of(ana),Troubles.open(List.of(ana),after));
    }
}
