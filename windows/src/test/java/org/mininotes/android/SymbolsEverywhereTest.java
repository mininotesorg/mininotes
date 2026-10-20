package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Every menu line in both apps wears a symbol (the owner, 2026-10-12: "make sure we have icons everywhere in the menus";
 * decision 157): each line's words, as the sources write them, read from the sources, and each must have one in Symbols.
 * A new line without one fails here, before anybody sees it.
 */
public class SymbolsEverywhereTest {
    private static final String[] PATTERNS={
        "sheet\\.row\\(\"([^\"]+)\"","sheet\\.toggle\\(\"([^\"]+)\"","\\.dimRow\\(\"([^\"]+)\"",
        "item\\((?:menu|m|more|plus|bin|note|file|one|it|lines|popup)\\s*,\\s*\"([^\"]+)\"",
        "DesktopMenus\\.item\\(\"([^\"]+)\"","ticked\\(\"([^\"]+)\"","new JMenu\\(\"([^\"]+)\""};
    private static final Pattern HELD=Pattern.compile("heldMenu\\([^;]*");
    private static final Pattern HELD_ROW=Pattern.compile("\"([^\"]{2,40})\"\\s*,\\s*\\(Runnable\\)");

    @Test public void everyMenuLineHasItsSymbol() throws Exception {
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.isDirectory(root.resolve("android")))root=root.getParent();
        assertNotNull("the project",root);
        List<Path> sources=new ArrayList<>();
        try(DirectoryStream<Path> in=Files.newDirectoryStream(root.resolve("android/app/src/main/java/org/mininotes/android"),"*.java")){for(Path p:in)sources.add(p);}
        try(DirectoryStream<Path> in=Files.newDirectoryStream(root.resolve("windows/src/main/java/org/mininotes/android"),"Desktop*.java")){for(Path p:in)sources.add(p);}
        Map<String,String> without=new TreeMap<>();int seen=0;
        for(Path p:sources) {
            String s=Files.readString(p);
            List<String> words=new ArrayList<>();
            for(String pattern:PATTERNS){Matcher m=Pattern.compile(pattern).matcher(s);while(m.find())words.add(m.group(1));}
            Matcher held=HELD.matcher(s);
            while(held.find()){Matcher row=HELD_ROW.matcher(held.group());while(row.find())words.add(row.group(1));}
            for(String one:words) {
                seen++;
                String said=one.replace("\\u2026","…");
                if(Symbols.of(said)==null)without.put(said,p.getFileName().toString());
            }
        }
        assertTrue("menu lines read from the sources: "+seen,seen>50);
        assertEquals("menu lines with no symbol",Map.of(),without);
    }

    /** One symbol for one doing (the owner, 2026-10-12): these three each have their own. */
    @Test public void differentDoingsHaveDifferentSymbols() {
        java.util.Set<String> seen=new java.util.HashSet<>();
        for(String words:List.of("Send files…","Sent files…","Send what is in Temp","Refuse","Block","Delete","Forget","Move to bin","Remove"))
            assertTrue(words+" shares its symbol",seen.add(Symbols.of(words)));
    }

    @Test public void everySymbolIsOneThisBuildCanDraw() {
        for(String words:List.of("Rename…","Delete","Tree","Export backup…","Add from backup…","Feedback…","Sent files…","Recovery writing…","Move to bin","Colour"))
            assertTrue(words,Icons.known(Symbols.of(words)));
    }
}
