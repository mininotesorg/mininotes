package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.file.*;
import java.util.zip.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class DesktopUpdateTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void theZipIsFoundFromTheVersionAlone() {
        assertEquals("https://github.com/mininotesorg/mininotes/releases/latest/download/Mininotes-Windows-0.0.020.zip",DesktopUpdate.zip("v0.0.020"));
        assertEquals("",DesktopUpdate.zip("<html>"));
        assertTrue(DesktopUpdate.LATEST.endsWith("/main/dist/latest-windows.txt"));
    }

    @Test public void theVersionInTheBarIsTheOneInLatestWindows() throws Exception {
        // The file every installed PC reads, and this build, say the same when the tree is released.
        Path latest=Path.of("..","dist","latest-windows.txt");
        if(Files.exists(latest))assertEquals(Desktop.VERSION,Update.read(Files.readString(latest)));
        assertTrue(Update.newer("9.9.999",Desktop.VERSION));assertFalse(Update.newer(Desktop.VERSION,Desktop.VERSION));
    }

    @Test public void aZipOpensInsideItsFolderAndOneThatClimbsOutIsRefused() throws Exception {
        Path good=temp.getRoot().toPath().resolve("good.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(good))) {
            out.putNextEntry(new ZipEntry("Mininotes/"));out.putNextEntry(new ZipEntry("Mininotes/Mininotes.exe"));out.write(new byte[]{1,2,3});
            out.putNextEntry(new ZipEntry("Mininotes/app/Mininotes.cfg"));out.write("x".getBytes());
        }
        Path into=temp.newFolder("staged").toPath();DesktopUpdate.unzip(good,into);
        assertEquals(3,Files.size(into.resolve("Mininotes/Mininotes.exe")));
        assertTrue(Files.isRegularFile(into.resolve("Mininotes/app/Mininotes.cfg")));

        Path bad=temp.getRoot().toPath().resolve("bad.zip");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(bad))){out.putNextEntry(new ZipEntry("../outside.txt"));out.write(1);}
        Path other=temp.newFolder("other").toPath();
        assertThrows(java.io.IOException.class,()->DesktopUpdate.unzip(bad,other));
        assertFalse(Files.exists(temp.getRoot().toPath().resolve("outside.txt")));
    }

    /**
     * The swap itself, on throwaway folders: it waits for the program to close, puts the new folder where
     * the old one was, starts what is there and clears the old one away. A copy of hostname.exe stands in
     * for Mininotes.exe, so what gets started prints a name and ends.
     */
    @Test public void theNewFolderTakesTheOldOnesPlaceOnceTheProgramHasClosed() throws Exception {
        if(!System.getProperty("os.name","").startsWith("Windows"))return;
        Path stand=Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","hostname.exe");
        Path root=temp.newFolder("Programs ünïcode").toPath();
        Path app=root.resolve("Mininotes-Windows-0.0.001");Files.createDirectories(app);
        Files.copy(stand,app.resolve("Mininotes.exe"));Files.writeString(app.resolve("which.txt"),"old");
        Path opened=root.resolve(".Mininotes-Windows-0.0.001.next").resolve("Mininotes");Files.createDirectories(opened);
        Files.copy(stand,opened.resolve("Mininotes.exe"));Files.writeString(opened.resolve("which.txt"),"new");
        // The program being replaced: something that runs for two seconds.
        Process running=new ProcessBuilder("ping","-n","3","127.0.0.1").redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        Process swap=DesktopUpdate.replaceAfterExit(app,opened,running.pid(),true);
        Thread.sleep(500);
        assertEquals("nothing moves while it runs","old",Files.readString(app.resolve("which.txt")));
        assertTrue(swap.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,swap.exitValue());
        assertEquals("new",Files.readString(app.resolve("which.txt")));
        try(var left=Files.list(root)){assertEquals("the old and the staged folders are cleared away",java.util.List.of(app),left.toList());}
    }

    @Test public void notThePackagedAppMeansNoFolderToReplace() {
        String was=System.getProperty("jpackage.app-path");
        try{System.clearProperty("jpackage.app-path");assertNull(DesktopUpdate.appFolder());}
        finally{if(was!=null)System.setProperty("jpackage.app-path",was);}
    }

    /** The MSI's copy (decision 235): known by the file build.ps1 puts in its app folder alone, and updated by the MSI. */
    @Test public void theMsisCopyIsKnownByItsMarkerAndUpdatesByTheMsi() throws Exception {
        assertEquals("https://github.com/mininotesorg/mininotes/releases/latest/download/Mininotes-Windows-0.4.020.msi",DesktopUpdate.msi("v0.4.020"));
        assertEquals("",DesktopUpdate.msi("<html>"));
        Path app=temp.newFolder("Mininotes").toPath();Files.createDirectories(app.resolve("app"));
        assertFalse("an unzipped copy",DesktopUpdate.byMsi(app));assertFalse(DesktopUpdate.byMsi(null));
        Files.writeString(app.resolve("app").resolve(DesktopUpdate.MSI_MARKER),"Installed by Mininotes-Windows-0.4.020.msi.");
        assertTrue(DesktopUpdate.byMsi(app));
        // build.ps1 writes the same name the app looks for.
        Path script=Path.of("build.ps1");
        if(Files.exists(script))assertTrue(Files.readString(script).contains("app\\"+DesktopUpdate.MSI_MARKER));
    }

    @Test public void theInstallerIsRunOnceMininotesHasClosedThenMininotesIsStarted() {
        Path app=Path.of("C:\\Users\\Ann O'Neil\\AppData\\Local\\Programs\\Mininotes");
        Path msi=Path.of("C:\\Users\\Ann O'Neil\\AppData\\Local\\Temp\\mininotes-update-1\\Mininotes-Windows-0.4.020.msi");
        assertEquals("/i \""+msi+"\" /qb /norestart",DesktopUpdate.install(msi));
        assertEquals("/x {0A1B2C3D-4E5F-6071-8293-A4B5C6D7E8F9} /qb /norestart",DesktopUpdate.uninstall("{0A1B2C3D-4E5F-6071-8293-A4B5C6D7E8F9}"));
        String script=DesktopUpdate.installScript(app,msi,4242,true);
        assertTrue(script.contains("Wait-Process -Id 4242"));
        assertTrue("still running: nothing is installed over it",script.contains("if(Get-Process -Id 4242 -ErrorAction SilentlyContinue){exit 1}"));
        // A quote in a folder's name is written twice inside PowerShell's single quotes, the path itself kept in double ones.
        assertTrue(script.contains("-ArgumentList '/i \"C:\\Users\\Ann O''Neil\\AppData\\Local\\Temp\\mininotes-update-1\\Mininotes-Windows-0.4.020.msi\" /qb /norestart' -Wait -PassThru"));
        assertTrue(script.indexOf("-Wait -PassThru")<script.indexOf("Join-Path $app 'Mininotes.exe'"));
        assertTrue(script.contains("$start=$true"));assertTrue(DesktopUpdate.installScript(app,msi,4242,false).contains("$start=$false"));
    }
}
