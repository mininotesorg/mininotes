package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decision 304 (Graphene, 2026-10-31): a screenshot shared into a stopped app was added while the node was starting, found it
 * not running, and waited for the next round. What is asked for when the node is up runs at once only if it already is; before
 * that, it waits for the start, which runs it (seen on the phones, not here: no test starts a real node). A hook that throws
 * never gets out.
 */
public class NodeWhenUpTest {
    @After public void forget(){Node.whenUp(null);}

    @Test public void runsNowOnlyIfTheNodeIsAlreadyUp() {
        AtomicInteger ran=new AtomicInteger();
        Node.whenUp(ran::incrementAndGet);
        assertEquals(Node.running()?1:0,ran.get());
    }

    @Test public void aHookThatThrowsNeverGetsOut() {
        Node.whenUp(()->{throw new IllegalStateException("not now");});
    }
}
