package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.file.*;
import java.util.List;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/**
 * Uninstall Mininotes… and the backup it proposes (decision 235), on throwaway folders: what is chosen to go, and the
 * script that would do it, read as text. Nothing here runs the script or touches a real notebook.
 */
public class DesktopUninstallTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String CODE="{0A1B2C3D-4E5F-6071-8293-A4B5C6D7E8F9}";

    /** A folder made the way jpackage makes the app image. */
    private Path app(Path where) throws Exception {
        Files.createDirectories(where.resolve("app"));Files.createDirectories(where.resolve("runtime"));
        Files.write(where.resolve("Mininotes.exe"),new byte[]{1});return where;
    }
    private Path notebook(Path where) throws Exception {
        Files.createDirectories(where);Files.write(where.resolve("notebook.lock"),new byte[0]);Files.write(where.resolve("mininotes.db"),new byte[]{1});return where;
    }

    @Test public void theProductCodeIsTheKeyWhoseDisplayNameIsMininotes() {
        String printed=String.join("\r\n",
            "",
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\SomethingElse",
            "    Publisher    REG_SZ    Mininotes",
            "",
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{11111111-2222-3333-4444-555555555555}",
            "    Publisher    REG_SZ    Mininotes",
            "",
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\"+CODE,
            "    DisplayName    REG_SZ    Mininotes",
            "",
            "End of search: 3 match(es) found.");
        assertEquals("the vendor's name elsewhere is not taken for the app",CODE,DesktopUninstall.productCode(printed));
        assertEquals("",DesktopUninstall.productCode("End of search: 0 match(es) found."));
        assertEquals("a key not named by a code is not an MSI's","",DesktopUninstall.productCode(
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Mininotes\r\n    DisplayName    REG_SZ    Mininotes\r\n"));
        assertTrue(DesktopUninstall.asking().contains(DesktopUninstall.UNINSTALL_LIST));
    }

    @Test public void theMsisCopyIsUninstalledByWindowsAndTheNotesStayUnlessAsked() throws Exception {
        Path root=temp.getRoot().toPath(),home=root.resolve("home"),local=home.resolve("AppData/Local");
        Path installed=app(local.resolve("Programs/Mininotes")),notes=notebook(local.resolve("Mininotes"));
        DesktopUninstall.Plan kept=DesktopUninstall.plan(installed,true,CODE,notes,true,true,local,home);
        assertTrue(kept.msi());assertEquals(CODE,kept.productCode());
        assertEquals("Windows removes the app; nothing else goes",List.of(),kept.remove());assertFalse(kept.notes());

        DesktopUninstall.Plan all=DesktopUninstall.plan(installed,true,CODE,notes,true,false,local,home);
        assertEquals(List.of(notes.toAbsolutePath().normalize(),local.toAbsolutePath().normalize().resolve("Mininotes-editing")),all.remove());

        assertThrows("not listed by Windows: Settings, Apps is said",IllegalStateException.class,()->DesktopUninstall.plan(installed,true,"",notes,true,true,local,home));
        assertThrows(IllegalStateException.class,()->DesktopUninstall.plan(installed,true,"Mininotes",notes,true,true,local,home));
        assertThrows("not the packaged app",IllegalStateException.class,()->DesktopUninstall.plan(null,false,"",notes,true,true,local,home));
    }

    @Test public void anUnzippedCopyGoesWithItsFolderAndOnlyIfItIsTheAppAlone() throws Exception {
        Path root=temp.getRoot().toPath(),home=root.resolve("home"),local=home.resolve("AppData/Local");
        Path unzipped=app(home.resolve("Downloads/Mininotes")),notes=notebook(local.resolve("Mininotes"));
        DesktopUninstall.Plan plan=DesktopUninstall.plan(unzipped,false,"",notes,true,true,local,home);
        Path a=unzipped.toAbsolutePath().normalize();
        assertEquals(List.of(a,a.resolveSibling(".Mininotes.next")),plan.remove());assertEquals("",plan.productCode());

        // Its files unzipped straight into Downloads, beside somebody's own: Downloads is never deleted.
        Path loose=app(home.resolve("Downloads"));Files.writeString(loose.resolve("tax return.pdf"),"mine");
        assertFalse(DesktopUninstall.looksLikeApp(loose));
        assertThrows(IllegalStateException.class,()->DesktopUninstall.plan(loose,false,"",notes,true,true,local,home));
        // The notebook inside the app's folder, and kept: the folder cannot go without it.
        Path inside=app(root.resolve("Portable/Mininotes"));
        assertThrows(IllegalStateException.class,()->DesktopUninstall.plan(inside,false,"",notebook(inside.resolve("app/notes")),true,true,local,home));
    }

    @Test public void theNotebookGoesOnlyWhereItIsSurelyANotebook() throws Exception {
        Path root=temp.getRoot().toPath(),home=root.resolve("home"),local=home.resolve("AppData/Local");
        Path installed=app(local.resolve("Programs/Mininotes"));
        assertThrows("no notebook.lock",IllegalStateException.class,()->DesktopUninstall.plan(installed,true,CODE,local.resolve("Mininotes"),false,false,local,home));
        assertThrows("the person's own folder",IllegalStateException.class,()->DesktopUninstall.plan(installed,true,CODE,home,true,false,local,home));
        assertThrows("%LOCALAPPDATA% itself",IllegalStateException.class,()->DesktopUninstall.plan(installed,true,CODE,local,true,false,local,home));
        assertFalse("a folder holding the person's own",DesktopUninstall.mayGo(root,local,home));
        assertFalse("a drive",DesktopUninstall.mayGo(root.getRoot(),local,home));
        assertTrue(DesktopUninstall.mayGo(local.resolve("Mininotes"),local,home));
    }

    @Test public void theScriptWaitsThenUninstallsAndStopsBeforeTheNotesIfWindowsDidNot() throws Exception {
        Path root=temp.getRoot().toPath(),home=root.resolve("home"),local=home.resolve("AppData/Local");
        Path installed=app(local.resolve("Programs/Mininotes")),notes=notebook(local.resolve("Mininotes"));
        String msi=DesktopUninstall.script(DesktopUninstall.plan(installed,true,CODE,notes,true,false,local,home),4242);
        assertTrue(msi.contains("Wait-Process -Id 4242"));assertTrue("still running: nothing",msi.contains("if(Get-Process -Id 4242 -ErrorAction SilentlyContinue){exit 1}"));
        assertTrue(msi.contains("msiexec.exe' -ArgumentList '/x "+CODE+" /qb /norestart' -Wait -PassThru"));
        int stops=msi.indexOf("if($code -ne 0 -and $code -ne 3010){exit $code}"),goes=msi.indexOf("Gone '"+DesktopUpdate.ps(notes)+"'");
        assertTrue("the notes go only after Windows has uninstalled",stops>0&&goes>stops);
        assertFalse("Windows removes its own shortcuts",msi.contains("WScript.Shell"));

        String keeping=DesktopUninstall.script(DesktopUninstall.plan(installed,true,CODE,notes,true,true,local,home),4242);
        assertFalse("kept, the notebook is not named",keeping.contains(DesktopUpdate.ps(notes)));

        Path unzipped=app(home.resolve("Downloads/Mininotes"));
        String zip=DesktopUninstall.script(DesktopUninstall.plan(unzipped,false,"",notes,true,true,local,home),4242);
        assertFalse(zip.contains("msiexec"));assertTrue("its shortcuts",zip.contains("GetFolderPath('Programs')")&&zip.contains("GetFolderPath('Desktop')"));
        assertTrue("the app's folder first, and nothing after it if it stays",zip.contains("if(-not (Gone '"+DesktopUpdate.ps(unzipped)+"')){exit 1}"));
        assertFalse(zip.contains(DesktopUpdate.ps(notes)));
    }

    @Test public void aBackupIsProposedOnceThirtyDaysHaveGoneWithNotesToLose() {
        long day=24L*60*60*1000,now=100*day;
        assertFalse("never with nothing to lose",DesktopBackup.due(now,0,now-60*day,false));
        assertFalse("counting has only begun",DesktopBackup.due(now,0,now,true));
        assertTrue(DesktopBackup.due(now,0,now-30*day,true));
        assertFalse("backed up last week",DesktopBackup.due(now,now-7*day,now-90*day,true));
        assertTrue(DesktopBackup.due(now,now-31*day,now-90*day,true));
        assertEquals(0,DesktopBackup.number("not a time"));assertEquals(0,DesktopBackup.number(null));assertEquals(5,DesktopBackup.number(" 5 "));
        assertFalse("no long dash in what it says",DesktopBackup.REMIND.contains("—"));
    }
}
