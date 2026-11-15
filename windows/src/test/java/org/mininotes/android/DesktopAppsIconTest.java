package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.awt.image.BufferedImage;

/**
 * A file's tile shows the icon Explorer would (the owner, 2026-11-04: a .jar in Temp "doesnt have the java logo"): where
 * Windows has none for the kind itself, the program it opens in; never Windows' blank page over the kind's letters.
 * Asks this PC's own Windows, so only on Windows, and only for the kinds this PC has a program for.
 */
public class DesktopAppsIconTest {
    @Test public void noKindIsDrawnAsABlankPage() throws Exception {
        Assume.assumeTrue(System.getProperty("os.name","").toLowerCase().contains("windows"));
        java.nio.file.Path empty=java.nio.file.Files.createTempFile("kind",".mininotes-no-such-kind-either");
        javax.swing.Icon got=javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(empty.toFile(),64,64);
        BufferedImage blank=new BufferedImage(got.getIconWidth(),got.getIconHeight(),BufferedImage.TYPE_INT_ARGB);
        var g=blank.createGraphics();got.paintIcon(null,g,0,0);g.dispose();
        java.nio.file.Files.deleteIfExists(empty);
        StringBuilder wrong=new StringBuilder();
        for(String kind:new String[]{".jar",".pdf",".txt",".zip",".docx",".xlsx",".mp4",".mp3",".png",".py",".msi",".apk",".csv",".html"}) {
            BufferedImage icon=DesktopApps.icon(kind);
            if(icon!=null&&DesktopApps.same(icon,blank))wrong.append(kind).append(" drawn as a blank page\n");
            // A kind this PC opens in a program has a picture: its own, or the program's.
            if(icon==null&&DesktopApps.windowsChoice(kind)!=null&&java.nio.file.Files.isRegularFile(DesktopApps.windowsChoice(kind).exe()))
                wrong.append(kind).append(" opens in ").append(DesktopApps.windowsChoice(kind).exe()).append(" and has no icon\n");
        }
        assertEquals("",wrong.toString());
    }
}
