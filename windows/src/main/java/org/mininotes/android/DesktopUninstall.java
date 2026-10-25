package org.mininotes.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

/**
 * Uninstall Mininotes…, from the menu's Mininotes part (the owner, 2026-10-22: "at uninstall, there should be a warning
 * that everything in the app will be deleted if not backed up. Generally speaking we should propose a backup system. It
 * should be clear that any private element can't be backed up"; decision 235).
 *
 * <p>One box: what uninstalling can delete, said plainly; that private spaces are in no backup; Back up first…; a switch,
 * on unless turned off, keeping the notebook on this PC, where a later install finds it again; and Uninstall. Turned off,
 * a second question asks before anything of the notes goes. Then Mininotes saves and closes, and a small script, started
 * just before, waits for it to be gone and does the rest: the MSI's copy is uninstalled by Windows' own installer, as
 * Settings, Apps would (which keeps the notes: the notebook folder, %LOCALAPPDATA%\Mininotes, is not part of the install);
 * an unzipped copy's folder is deleted, with the shortcuts to it; the notebook only when the switch was turned off and the
 * second question answered.
 *
 * <p>What is decided, and the script that does it, are worked out here from what they are given and nothing else, so the
 * tests can ask them (DesktopUninstallTest) without anything being deleted.
 */
final class DesktopUninstall {
    private DesktopUninstall(){}

    /**
     * What an uninstall does, decided before Mininotes closes.
     *
     * @param app         the app's folder, where Mininotes.exe is
     * @param msi         whether the MSI installed it: Windows uninstalls it, under {@code productCode}
     * @param productCode the MSI's product code, {xxxxxxxx-…}; empty for an unzipped copy
     * @param remove      what is deleted once the app is gone, in order: an unzipped copy's folder and the update waiting
     *                    beside it, then, only when asked, the notebook and the copies of files opened to edit
     */
    record Plan(Path app,boolean msi,String productCode,List<Path> remove,boolean notes) {}

    private static final Pattern GUID=Pattern.compile("\\{[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}\\}");

    /**
     * The plan, or why there is none, in words for the person (an IllegalStateException): nothing is deleted that does not
     * look like what it is said to be.
     *
     * @param notebook     the notebook this window has open
     * @param isNotebook   whether that folder holds a notebook (its notebook.lock): asked by the caller, on the disk
     * @param keepNotes    the switch: on keeps the notebook
     * @param localAppData %LOCALAPPDATA%, where the copies of files opened to edit are; may be null
     * @param home         the person's own folder, which is never deleted, nor any folder holding it
     */
    static Plan plan(Path app,boolean msi,String productCode,Path notebook,boolean isNotebook,boolean keepNotes,Path localAppData,Path home) {
        if(app==null)throw new IllegalStateException("This copy of Mininotes is not the installed app, so it cannot uninstall itself.");
        app=app.toAbsolutePath().normalize();
        List<Path> remove=new ArrayList<>();
        if(msi) {
            if(productCode==null||!GUID.matcher(productCode).matches())throw new IllegalStateException("Windows does not list Mininotes among its apps here. Uninstall it from Settings, Apps.");
        } else {
            // An unzipped copy is a folder: deleted only if it is the app's (its launcher, its jars and its Java beside it).
            if(!looksLikeApp(app)||!mayGo(app,localAppData,home))throw new IllegalStateException("This folder does not look like Mininotes alone, so nothing was deleted.");
            if(keepNotes&&notebook!=null&&notebook.toAbsolutePath().normalize().startsWith(app))
                throw new IllegalStateException("Your notes are inside the app's folder, so it cannot be deleted without them. Move the app first.");
            remove.add(app);
            // A newer version downloaded and waiting beside it (DesktopUpdate.fetch) goes with it.
            remove.add(app.resolveSibling("."+app.getFileName()+".next"));
        }
        if(!keepNotes) {
            if(notebook==null||!isNotebook||!mayGo(notebook.toAbsolutePath().normalize(),localAppData,home))
                throw new IllegalStateException("Mininotes could not be sure which folder holds your notes, so nothing was deleted.");
            remove.add(notebook.toAbsolutePath().normalize());
            // The copies of attachments opened to edit (DesktopOpening), which are copies of notes' files.
            if(localAppData!=null)remove.add(localAppData.toAbsolutePath().normalize().resolve("Mininotes-editing"));
        }
        return new Plan(app,msi,msi?productCode:"",List.copyOf(remove),!keepNotes);
    }

    /**
     * The app image jpackage makes, and nothing else: the launcher, its app and runtime folders, its icon. A folder holding
     * anything more is not deleted, so a copy whose files were unzipped straight into Downloads never takes Downloads with it.
     */
    static boolean looksLikeApp(Path app) {
        if(!Files.isRegularFile(app.resolve("Mininotes.exe"))||!Files.isDirectory(app.resolve("app"))||!Files.isDirectory(app.resolve("runtime")))return false;
        try(DirectoryStream<Path> in=Files.newDirectoryStream(app)) {
            for(Path one:in)if(!IMAGE.contains(one.getFileName().toString().toLowerCase(Locale.ROOT)))return false;
            return true;
        } catch(IOException unreadable){return false;}
    }
    private static final Set<String> IMAGE=Set.of("mininotes.exe","mininotes.ico","app","runtime");

    /** A folder that may be deleted whole: never a drive, the person's own folder, %LOCALAPPDATA%, or one holding them. */
    static boolean mayGo(Path folder,Path localAppData,Path home) {
        Path f=folder.toAbsolutePath().normalize();
        if(f.getParent()==null||f.getNameCount()<1)return false;
        for(Path kept:new Path[]{localAppData,home}) {
            if(kept==null)continue;
            Path k=kept.toAbsolutePath().normalize();
            if(k.startsWith(f))return false;
        }
        return true;
    }

    /**
     * The product code of the Mininotes the MSI installed, from what {@code reg query} prints of the current user's
     * uninstall list ({@link #asking}): the key, named by its code, whose DisplayName is Mininotes. Empty when there is none.
     * Another value that happens to read Mininotes - its Publisher, the vendor - is not taken for it.
     */
    static String productCode(String printed) {
        String key=null;
        for(String line:printed.split("\\R")) {
            if(line.startsWith("HKEY_")){key=line.trim();continue;}
            String[] said=line.trim().split("\\s{2,}|\\t");
            if(key!=null&&said.length==3&&said[0].equals("DisplayName")&&said[1].startsWith("REG_")&&said[2].trim().equals("Mininotes")) {
                String name=key.substring(key.lastIndexOf('\\')+1);
                if(GUID.matcher(name).matches())return name;
            }
        }
        return "";
    }
    /** Where Windows lists the apps a person installed for themselves, which a per-user MSI is. */
    static final String UNINSTALL_LIST="HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
    /** The question put to reg.exe: every value there that is exactly "Mininotes", with the key it is under. */
    static List<String> asking(){return List.of(Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","reg.exe").toString(),"query",UNINSTALL_LIST,"/s","/f","Mininotes","/d","/e");}

    /** The product code, asked of Windows. Blocking; empty when it is not listed. */
    static String askWindows() {
        try {
            Process reg=new ProcessBuilder(asking()).redirectErrorStream(true).start();
            String printed=new String(reg.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
            reg.waitFor();return productCode(printed);
        } catch(IOException none){return "";}
        catch(InterruptedException stop){Thread.currentThread().interrupt();return "";}
    }

    /**
     * The script that waits for this Mininotes ({@code waitFor}) to be gone, then does the plan. Still running after two
     * minutes, nothing is done. Windows' uninstall answering anything but done (cancelled, or failed) stops it there:
     * the notes never go while the app stays. A folder that will not go is tried again for ten seconds.
     */
    static String script(Plan plan,long waitFor) {
        List<String> lines=new ArrayList<>(List.of(
            "$ErrorActionPreference='Continue'",
            "$app='"+DesktopUpdate.ps(plan.app())+"'",
            "try{Wait-Process -Id "+waitFor+" -Timeout 120 -ErrorAction SilentlyContinue}catch{}",
            "if(Get-Process -Id "+waitFor+" -ErrorAction SilentlyContinue){exit 1}",
            "Start-Sleep -Milliseconds 800",
            "function Gone($path){for($i=0;$i -lt 20 -and (Test-Path -LiteralPath $path);$i++){try{Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction Stop}catch{Start-Sleep -Milliseconds 500}};return -not (Test-Path -LiteralPath $path)}"));
        if(plan.msi()) {
            lines.add("$code=1;try{$done=Start-Process -FilePath '"+DesktopUpdate.ps(DesktopUpdate.msiexec())+"' -ArgumentList '"+DesktopUpdate.uninstall(plan.productCode())+"' -Wait -PassThru;$code=$done.ExitCode}catch{}");
            lines.add("if($code -ne 0 -and $code -ne 3010){exit $code}");
        } else {
            // The shortcuts to this copy, in the Start menu and on the desktop: those whose target is inside its folder.
            lines.add("try{$shell=New-Object -ComObject WScript.Shell;foreach($where in @([Environment]::GetFolderPath('Programs'),[Environment]::GetFolderPath('Desktop'))){if($where -and (Test-Path -LiteralPath $where)){Get-ChildItem -LiteralPath $where -Filter *.lnk -Recurse -ErrorAction SilentlyContinue|ForEach-Object{try{$to=$shell.CreateShortcut($_.FullName).TargetPath;if($to -and $to.StartsWith($app+'\\',[StringComparison]::OrdinalIgnoreCase)){Remove-Item -LiteralPath $_.FullName -Force}}catch{}}}}}catch{}");
        }
        boolean first=true;
        for(Path one:plan.remove()) {
            // The app's own folder first: if it will not go, nothing after it does.
            if(first&&!plan.msi())lines.add("if(-not (Gone '"+DesktopUpdate.ps(one)+"')){exit 1}");
            else lines.add("Gone '"+DesktopUpdate.ps(one)+"' | Out-Null");
            first=false;
        }
        lines.add("exit 0");lines.add("");
        return String.join("\r\n",lines);
    }

    // ---- the box -------------------------------------------------------------------------------------------------------

    static final String WARNING="Uninstalling can delete everything kept in Mininotes on this PC: notes, folders, files and settings. What is not backed up is then lost.";
    static final String PRIVATE="Private spaces are never in a backup, so what is in them is lost with the rest.";
    static final String KEEP="Keep my notes on this PC";
    static final String KEPT="They are found again if Mininotes is installed later.";
    static final String GOES="Your notes, folders, files and settings in Mininotes are deleted from this PC, and your private spaces with them. Files in your own folders outside Mininotes are not touched. This cannot be undone.";

    static void open(Desktop app) {
        Path folder=DesktopUpdate.appFolder();
        if(folder==null){app.status.setText(Say.t("This copy of Mininotes is not the installed app, so it cannot uninstall itself."));return;}
        boolean msi=DesktopUpdate.byMsi(folder);
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(WARNING,400,DesktopUi.INK,DesktopUi.BODY));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.note(PRIVATE,400,DesktopUi.INK,DesktopUi.BODY));DesktopUi.gap(body,DesktopUi.M);
        JCheckBox keep=DesktopUi.toggle(KEEP,true);keep.setName("uninstallKeep");
        JPanel card=DesktopUi.column();card.add(DesktopUi.switchRow(KEEP,keep));
        DesktopUi.add(card,DesktopUi.note(KEPT,380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.add(body,DesktopUi.card(null,card));
        // The backup My profile makes (Desktop.backup), from here: the box goes, so the questions it asks are on top.
        JButton back=DesktopUi.button("Back up first…",()->{box[0].dispose();app.save(app::backup);});
        JButton go=DesktopUi.primary("Uninstall",()->{
            boolean keepNotes=keep.isSelected();
            if(!keepNotes&&!DesktopUi.confirm(box[0],Say.t("Delete everything?"),Say.t(GOES),Say.t("Delete and uninstall"),true))return;
            box[0].dispose();
            app.status.setToolTipText(null);app.status.setText(Say.t("Uninstalling Mininotes…"));
            Path notebook=app.context.getFilesDir().toPath();
            String local=System.getenv("LOCALAPPDATA");
            app.connectivity.submit(()->plan(folder,msi,msi?askWindows():"",notebook,Files.isRegularFile(notebook.resolve("notebook.lock")),keepNotes,
                    local==null?null:Path.of(local),Path.of(System.getProperty("user.home"))),
                plan->app.uninstallNow(plan),
                e->app.status.setText(Say.t(e.getMessage()==null?"Mininotes could not be uninstalled.":e.getMessage())));
        });
        // Not the default button: Enter does not uninstall.
        box[0]=DesktopUi.sheet(app.frame,"Uninstall Mininotes",body,DesktopUi.footer(back,go),true);box[0].setName("uninstall");
        DesktopUi.show(box[0],460,520);
    }

    /** The script started, hidden; Mininotes then closes (Desktop.uninstallNow). */
    static Process start(Plan plan) throws IOException{return DesktopUpdate.run(script(plan,ProcessHandle.current().pid()),"mininotes-uninstall-");}
}
