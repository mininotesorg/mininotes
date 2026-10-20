package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Nothing in the project that the nightly private backup's secret scan refuses (The bay\tools\sync-private-project-backup.ps1,
 * ExplicitSecret): a name like password given eight or more characters in quotes. Three tests did, from 2026-10-07, and every
 * night's private GitHub snapshot of Mininotes failed for five days without anybody seeing it (the owner, 2026-10-12: "I
 * don't find clearly our drive saved on GitHub"). The one reviewed exception, LockChoice's warning sentence, stays.
 */
public class BackupScanTest {
    private static final Pattern EXPLICIT_SECRET=Pattern.compile(
        "(?i)(mnemonic|seed[ _-]?phrase|private[ _-]?key|secret[ _-]?key|password|passwd)\\s*[:=]\\s*[\"'][^\"'\\r\\n]{8,}[\"']");
    private static final Set<String> REVIEWED=Set.of("android/app/src/main/java/org/mininotes/android/LockChoice.java");

    @Test public void nothingTheBackupScanRefuses() throws Exception {
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.isDirectory(root.resolve("android")))root=root.getParent();
        assertNotNull(root);
        final Path top=root;
        List<String> refused=new ArrayList<>();
        for(String part:List.of("android/app/src","windows/src","docs")) {
            try(Stream<Path> all=Files.walk(top.resolve(part))) {
                for(Path p:all.filter(Files::isRegularFile).filter(p->{String n=p.toString();return n.endsWith(".java")||n.endsWith(".md")||n.endsWith(".kt")||n.endsWith(".xml")||n.endsWith(".ps1")||n.endsWith(".gradle");}).collect(Collectors.toList())) {
                    String rel=top.relativize(p).toString().replace('\\','/');
                    if(REVIEWED.contains(rel))continue;
                    if(EXPLICIT_SECRET.matcher(Files.readString(p)).find())refused.add(rel);
                }
            }
        }
        assertEquals("files the nightly backup refuses (rename the constant; never a real secret)",List.of(),refused);
    }
}
