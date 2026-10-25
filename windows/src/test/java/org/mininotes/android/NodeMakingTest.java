// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * The node is made under its own lock, never the class's (the Pro, 2026-10-25: the screen drawn again while the node
 * waited on relays, and Android said Mininotes was not responding). What the screen asks as it comes and goes answers at
 * once while a node is being made.
 */
public class NodeMakingTest {
    @Test public void theScreenIsAnsweredWhileTheNodeIsBeingMade() throws Exception {
        Field making=Node.class.getDeclaredField("MAKING");making.setAccessible(true);
        Object lock=making.get(null);
        CountDownLatch held=new CountDownLatch(1),done=new CountDownLatch(1);
        Thread maker=new Thread(()->{synchronized(lock){held.countDown();try{done.await(10,TimeUnit.SECONDS);}catch(InterruptedException ignored){/* let go */}}});
        maker.setDaemon(true);maker.start();
        assertTrue(held.await(5,TimeUnit.SECONDS));
        try {
            long from=System.nanoTime();
            Node.near(true);Node.near(false);Node.running();
            assertTrue("answered while making",System.nanoTime()-from<TimeUnit.SECONDS.toNanos(2));
        } finally{done.countDown();}
    }

    @Test public void theSlowWaysInDoNotHoldTheClassLock() throws Exception {
        for(String name:new String[]{"started","media","blobs"})
            for(var m:Node.class.getDeclaredMethods())
                if(m.getName().equals(name))assertFalse(name+" holds Node's lock while it may start the node",Modifier.isSynchronized(m.getModifiers()));
    }
}
