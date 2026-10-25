package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;

/**
 * "I have this file", once a run for each file and device (decision 243): a file is taken as it is queued, so a list that
 * arrives while the last answers are still on their way gets none of them again, and one that did not go is given back.
 */
public class SaidHaveTest {
    @Test public void aListArrivingWhileTheAnswersAreOnTheirWaySaysNoneAgain() {
        Set<String> said=ConcurrentHashMap.newKeySet();
        assertEquals(List.of("a","b","c"),Post.reserve(said,"pro",List.of("a","b","c")));
        // The next list, two seconds later, before any of the three has gone: nothing is due.
        assertEquals(List.of(),Post.reserve(said,"pro",List.of("a","b","c")));
        // A new file in it is.
        assertEquals(List.of("d"),Post.reserve(said,"pro",List.of("a","b","c","d")));
    }

    @Test public void eachDeviceIsToldOnceAndAFileThatDidNotGoIsSaidAgain() {
        Set<String> said=ConcurrentHashMap.newKeySet();
        Post.reserve(said,"pro",List.of("a","b"));
        assertEquals("another device is told too",List.of("a","b"),Post.reserve(said,"pc",List.of("a","b")));
        Post.release(said,"pro",List.of("b"));
        assertEquals(List.of("b"),Post.reserve(said,"pro",List.of("a","b")));
        assertEquals("the same file twice in one list, once",List.of("x"),Post.reserve(said,"pro",java.util.Arrays.asList("x","x",null)));
    }
}
