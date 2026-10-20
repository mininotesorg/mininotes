package org.mininotes.android;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.regex.*;
import org.junit.Test;

/** The version the PC says it is, the one its package is named by, and the phone's: one number (the owner, 2026-10-09). */
public class DesktopVersionTest {
    @Test public void thePcSaysTheVersionItIsBuiltAsAndThePhoneHasTheSame() throws Exception {
        String gradle=Files.readString(Path.of("build.gradle"));
        Matcher built=Pattern.compile("(?m)^version = '([^']+)'").matcher(gradle);
        assertTrue(built.find());
        assertEquals("Desktop.VERSION and windows/build.gradle",built.group(1),Desktop.VERSION);
        String phone=Files.readString(Path.of("..","android","app","build.gradle"));
        Matcher named=Pattern.compile("versionName '([^']+)'").matcher(phone);
        assertTrue(named.find());
        assertEquals("the phone's version and the PC's",built.group(1),named.group(1));
    }
}
