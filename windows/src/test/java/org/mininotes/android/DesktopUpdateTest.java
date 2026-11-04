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

    /**
     * The swap again, asked for by a program that works in the very folder being replaced, as Mininotes does whenever
     * Explorer or a shortcut opened it (decision 260): a Java started in the old folder asks for the swap and ends, as
     * Mininotes does. The script it starts must not work there too, or Windows refuses to move the folder and the old one
     * is opened again. The test above passed all along because the tests' own Java works in windows\, not in the app.
     */
    @Test public void aMininotesOpenedFromItsOwnFolderIsReplacedToo() throws Exception {
        if(!System.getProperty("os.name","").startsWith("Windows"))return;
        Path stand=Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","hostname.exe");
        Path root=temp.newFolder("Some User").toPath();
        Path app=root.resolve("Mininotes");Files.createDirectories(app);
        Files.copy(stand,app.resolve("Mininotes.exe"));Files.writeString(app.resolve("which.txt"),"old");
        Path opened=root.resolve(".Mininotes.next").resolve("Mininotes");Files.createDirectories(opened);
        Files.copy(stand,opened.resolve("Mininotes.exe"));Files.writeString(opened.resolve("which.txt"),"new");
        Path javaExe=Path.of(System.getProperty("java.home"),"bin","java.exe");
        Process asking=new ProcessBuilder(javaExe.toString(),"-cp",System.getProperty("java.class.path"),FromItsOwnFolder.class.getName(),app.toString(),opened.toString())
            .directory(app.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        assertTrue(asking.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,asking.exitValue());
        // The script it left behind waits for it to end, then moves the folders; it can take ten seconds to give up.
        long until=System.currentTimeMillis()+40_000;
        while(System.currentTimeMillis()<until&&!(Files.isRegularFile(app.resolve("which.txt"))&&"new".equals(Files.readString(app.resolve("which.txt")))))Thread.sleep(250);
        assertEquals("the new folder is in the old one's place","new",Files.readString(app.resolve("which.txt")));
    }

    /**
     * An update that did not finish is said at the next start (decision 264). Here the swap really fails: something else works
     * in the old folder for longer than the script tries, as Mininotes itself did before decision 260. The script leaves its
     * mark beside the app, the old copy is where it was, and the start finds which update did not finish, once.
     */
    @Test public void aSwapThatFailsLeavesAMarkAndTheNextStartSaysSo() throws Exception {
        if(!System.getProperty("os.name","").startsWith("Windows"))return;
        Path stand=Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","hostname.exe");
        Path root=temp.newFolder("Held").toPath();
        Path app=root.resolve("Mininotes");Files.createDirectories(app);
        Files.copy(stand,app.resolve("Mininotes.exe"));Files.writeString(app.resolve("which.txt"),"old");
        Path opened=DesktopUpdate.staged(app).resolve("Mininotes");Files.createDirectories(opened);
        Files.copy(stand,opened.resolve("Mininotes.exe"));Files.writeString(opened.resolve("which.txt"),"new");
        Files.writeString(DesktopUpdate.staged(app).resolve(DesktopUpdate.STAGED_VERSION),"0.4.031");
        // Something working in the old folder for longer than the script's ten seconds of tries.
        Process holding=new ProcessBuilder("ping","-n","30","127.0.0.1").directory(app.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            // Mininotes, already closed.
            Process closed=new ProcessBuilder("cmd","/c","exit").start();closed.waitFor();
            Process swap=DesktopUpdate.replaceAfterExit(app,opened,closed.pid(),false);
            assertTrue(swap.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals("it could not move the folder",1,swap.exitValue());
        } finally {holding.destroy();holding.waitFor(10,java.util.concurrent.TimeUnit.SECONDS);}
        assertEquals("the old copy is where it was","old",Files.readString(app.resolve("which.txt")));
        assertTrue("the mark is left",Files.isRegularFile(DesktopUpdate.failedMark(app)));
        assertEquals("0.4.031",DesktopUpdate.unfinished(app,"0.4.030"));
        assertTrue(DesktopUpdate.unfinishedSaid("0.4.031","0.4.030").contains("v0.4.031"));
        assertTrue(DesktopUpdate.unfinishedSaid("0.4.031","0.4.030").contains("MSI"));
        // Said once: what it left is cleared away, and the next start says nothing.
        DesktopUpdate.forgetUnfinished(app);
        assertNull(DesktopUpdate.unfinished(app,"0.4.030"));
        assertFalse(Files.exists(DesktopUpdate.staged(app)));
        assertTrue("the app itself untouched",Files.isRegularFile(app.resolve("Mininotes.exe")));
    }

    /** What a start finds beside the app, each way an update can have left it (decision 264). */
    @Test public void whatAnUpdateLeftIsReadAsItIs() throws Exception {
        Path root=temp.newFolder("Left").toPath();
        Path app=root.resolve("Mininotes");Files.createDirectories(app);
        assertNull("nothing beside it",DesktopUpdate.unfinished(app,"0.4.031"));
        assertNull(DesktopUpdate.unfinished(null,"0.4.031"));
        // A swap that worked: the staged folder empty for the seconds before the script removes it. Nothing to say.
        Files.createDirectories(DesktopUpdate.staged(app));
        assertNull(DesktopUpdate.unfinished(app,"0.4.031"));
        // A copy a build before 0.4.031 left when its swap failed: no version beside it, said without one.
        Path opened=DesktopUpdate.staged(app).resolve("Mininotes");Files.createDirectories(opened);Files.writeString(opened.resolve("Mininotes.exe"),"stand-in");
        assertEquals("",DesktopUpdate.unfinished(app,"0.4.031"));
        assertTrue(DesktopUpdate.unfinishedSaid("","0.4.031").contains("v0.4.031"));
        // With its version: said where it is newer than this one; overtaken (this one installed by hand since), only left over.
        Files.writeString(DesktopUpdate.staged(app).resolve(DesktopUpdate.STAGED_VERSION),"0.4.032");
        assertEquals("0.4.032",DesktopUpdate.unfinished(app,"0.4.031"));
        assertNull(DesktopUpdate.unfinished(app,"0.4.032"));
        assertEquals("0.4.032",DesktopUpdate.versionOf(opened));
        assertEquals("0.4.033",DesktopUpdate.versionOf(Path.of("C:\\Temp\\mininotes-update-1\\Mininotes-Windows-0.4.033.msi")));
        // The mark wins: the installer's update that Windows did not install.
        DesktopUpdate.forgetUnfinished(app);
        Files.writeString(DesktopUpdate.failedMark(app),"0.4.033\r\n");
        assertEquals("0.4.033",DesktopUpdate.unfinished(app,"0.4.031"));
        DesktopUpdate.forgetUnfinished(app);
        assertNull(DesktopUpdate.unfinished(app,"0.4.031"));
        // Both scripts leave the mark where they fail, and only there.
        Path msi=Path.of("C:\\Temp\\mininotes-update-1\\Mininotes-Windows-0.4.033.msi");
        String install=DesktopUpdate.installScript(app,msi,4242,true);
        assertTrue(install.contains("if($code -ne 0 -and $code -ne 3010){Set-Content -LiteralPath '"+DesktopUpdate.failedMark(app)+"' -Value '0.4.033'"));
    }

    /** What the test above starts in the old folder: the swap asked for, without starting anything after, then the end. */
    public static final class FromItsOwnFolder {
        public static void main(String[] args) throws Exception {
            DesktopUpdate.replaceAfterExit(Path.of(args[0]),Path.of(args[1]),ProcessHandle.current().pid(),false);
            System.exit(0);
        }
    }

    /** Every script starts in its own folder among the temporary files, never in the folder of the program that started it. */
    @Test public void aScriptWorksInItsOwnFolder() throws Exception {
        ProcessBuilder shell=DesktopUpdate.shell("exit 0","mininotes-test-");
        assertNotNull(shell.directory());
        Path file=Path.of(shell.command().get(shell.command().size()-1));
        try{assertEquals(file.toAbsolutePath().getParent(),shell.directory().toPath().toAbsolutePath());}
        finally{Files.deleteIfExists(file);}
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
