package org.mininotes.android;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.function.IntConsumer;
import java.util.zip.*;

/**
 * A newer Mininotes for Windows: found, fetched, checked and put in place of this one, from inside the app.
 *
 * <p>The repository keeps one line, {@code dist/latest-windows.txt}, with the newest Windows version; every
 * release carries that build as {@code Mininotes-Windows-<version>.zip} with its SHA-256 beside it, so the
 * zip is found from the version alone under {@code releases/latest/download}. Nothing is sent with any
 * request. The zip is kept only if it matches its checksum; then it is opened beside this app, and once
 * Mininotes has closed a small script puts it where this app was - same folder, so shortcuts still work -
 * and starts it. The notebook is not in that folder and is not touched. If anything goes wrong the old
 * folder is put back and started instead.
 *
 * <p>A Mininotes the MSI installed (decision 235) updates by the MSI of the newer version instead, checked the same way:
 * once Mininotes has closed, Windows' own installer puts it in place of this one, per user, with no administrator asked
 * for, so Settings, Apps keeps one Mininotes at the newer version; then it is started. An unzipped copy updates by the zip.
 */
final class DesktopUpdate {
    static final String SOURCE="https://github.com/mininotesorg/mininotes";
    static final String LATEST=SOURCE.replace("github.com","raw.githubusercontent.com")+"/main/dist/latest-windows.txt";
    static final String RELEASES=SOURCE+"/releases/latest";
    private static final long MOST=400L*1024*1024;

    private DesktopUpdate(){}

    private static final HttpClient HTTP=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();

    /** The newest published version, or "" when it could not be read. Blocking. */
    static String newest(){try{return latest();}catch(IOException no){return "";}}

    /** The newest published version, or why it could not be known, in words for the person. Blocking. */
    static String latest() throws IOException {
        HttpResponse<String> said;
        try{said=HTTP.send(HttpRequest.newBuilder(URI.create(LATEST)).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("The check was stopped.");}
        catch(IOException offline){throw new IOException("GitHub could not be reached. Check the connection and try again.");}
        if(said.statusCode()==404)throw new IOException("No version for Windows has been published on GitHub yet.");
        if(said.statusCode()!=200)throw new IOException(Say.t("GitHub did not answer the check (HTTP {0}).",said.statusCode()));
        String version=Update.read(said.body().lines().findFirst().orElse(""));
        if(version.isEmpty())throw new IOException("GitHub's answer was not a version.");
        return version;
    }

    /** Where the zip of a version is, and its checksum beside it. */
    static String zip(String version){String v=Update.read(version);return v.isEmpty()?"":RELEASES+"/download/Mininotes-Windows-"+v+".zip";}
    /** Where the installer of a version is, and its checksum beside it (decision 235). */
    static String msi(String version){String v=Update.read(version);return v.isEmpty()?"":RELEASES+"/download/Mininotes-Windows-"+v+".msi";}

    /**
     * The file the MSI's copy of the app has and an unzipped copy never has: build.ps1 writes it into the copy of the app
     * image the MSI is made from, alone. Asked of the app's own files, not of where they are: a folder under
     * %LOCALAPPDATA%\Programs can be an unzipped copy somebody put there, and the MSI's copy is the MSI's wherever Windows
     * put it. Asking Windows' list of apps would need the registry at every update; this is one file, and it comes and
     * goes with the MSI's own files.
     */
    static final String MSI_MARKER="msi-installed";
    /** Whether the MSI installed this app: it then updates by the MSI and is uninstalled through Windows. */
    static boolean byMsi(Path app){return app!=null&&Files.isRegularFile(app.resolve("app").resolve(MSI_MARKER));}

    /** This app's own folder - where Mininotes.exe is - or null when it is not running as the packaged app. */
    static Path appFolder() {
        String exe=System.getProperty("jpackage.app-path");
        if(exe==null||exe.isBlank())return null;
        Path folder=Path.of(exe).toAbsolutePath().getParent();
        return folder!=null&&Files.isRegularFile(folder.resolve("Mininotes.exe"))?folder:null;
    }

    /**
     * The zip of that version, downloaded and checked against its SHA-256, then opened beside this app.
     * Blocking; {@code percent} hears how far the download is.
     *
     * @return the opened app, ready to be put in place
     */
    static Path fetch(String version,Path app,IntConsumer percent) throws Exception {
        if(byMsi(app))return fetchMsi(version,percent);
        String url=zip(version);
        if(url.isEmpty())throw new IOException("That is not a version.");
        Path zip=Files.createTempFile("mininotes-update-",".zip");
        try {
            download(url,version,zip,percent);
            Path staged=staged(app);
            delete(staged);Files.createDirectories(staged);
            unzip(zip,staged);
            Path opened=staged.resolve("Mininotes");
            if(!Files.isRegularFile(opened.resolve("Mininotes.exe")))throw new IOException("The download is not a Mininotes build.");
            // Which version waits there, so a start that finds it still there can say which update did not finish.
            Files.writeString(staged.resolve(STAGED_VERSION),Update.read(version),StandardCharsets.UTF_8);
            return opened;
        } finally {Files.deleteIfExists(zip);}
    }

    // ---- an update that did not finish (decision 264) -----------------------------------------------------------------

    /** Where a newer version is opened beside this app until it is put in place: ".Mininotes.next" beside "Mininotes". */
    static Path staged(Path app){return app.resolveSibling("."+app.getFileName()+".next");}
    /** The file beside the staged copy saying which version it is. */
    static final String STAGED_VERSION="version.txt";
    /**
     * What the update's script leaves beside this app when it could not put the new version in place, or Windows' installer
     * did not install it: the version it was. Removed by the next start, which says so.
     */
    static Path failedMark(Path app){return app.resolveSibling("."+app.getFileName()+".update-failed");}

    /**
     * The update that did not finish, found at a start: the version it was to, empty where that cannot be told (a copy
     * left by a build before 0.4.031, which wrote no version), or null for none. The update that could not move its own
     * folder (decision 260) started the old copy again without a word, and the next start downloaded it again, for ever:
     * it is said once now. Found by the script's mark, or by the new copy still waiting beside this app: a swap that
     * worked moved it out, leaving its folder empty for the few seconds before the script removes it. Not an update to
     * this version or an older one, which is done or overtaken and only left over.
     */
    static String unfinished(Path app,String running) {
        if(app==null)return null;
        String version=null;
        Path mark=failedMark(app);
        if(Files.isRegularFile(mark))version=readVersion(mark);
        else if(Files.isRegularFile(staged(app).resolve("Mininotes").resolve("Mininotes.exe")))version=readVersion(staged(app).resolve(STAGED_VERSION));
        if(version==null)return null;
        return version.isEmpty()||Update.newer(version,running)?version:null;
    }
    private static String readVersion(Path file) {
        // Windows PowerShell writes UTF-8 with its byte-order mark first, which is no part of a version.
        try{return Files.isRegularFile(file)?Update.read(Files.readString(file,StandardCharsets.UTF_8).replace(String.valueOf((char)0xFEFF),"").trim()):"";}
        catch(IOException|RuntimeException unreadable){return "";}
    }

    /** What an update left beside this app, gone, so it is said once: the mark, and a copy the next update downloads again. */
    static void forgetUnfinished(Path app) throws IOException {
        if(app==null)return;
        Files.deleteIfExists(failedMark(app));
        delete(staged(app));
    }

    /** What the start says about it: which update, that this version is unchanged, and the two ways on. */
    static String unfinishedSaid(String version,String running) {
        return version==null||version.isEmpty()
            ?Say.t("An update did not finish, so this is still v{0}. Click to update again, or install the MSI from the releases page.",running)
            :Say.t("The update to v{0} did not finish, so this is still v{1}. Click to update again, or install the MSI from the releases page.",version,running);
    }

    /** The version a fetched update is of: the installer's in its name, an unzipped copy's in the file beside it. */
    static String versionOf(Path opened) {
        String name=opened.getFileName().toString();
        if(name.toLowerCase(java.util.Locale.ROOT).endsWith(".msi"))return Update.read(name.replaceFirst("(?i)^Mininotes-Windows-","").replaceFirst("(?i)\\.msi$",""));
        Path parent=opened.toAbsolutePath().getParent();
        return parent==null?"":readVersion(parent.resolve(STAGED_VERSION));
    }

    /** The line of a script that leaves the mark: the version it failed to put in place, for the next start to say. */
    private static String markFailed(Path app,String version) {
        return "Set-Content -LiteralPath '"+ps(failedMark(app))+"' -Value '"+version.replace("'","''")+"' -Encoding UTF8 -ErrorAction SilentlyContinue";
    }

    /**
     * The installer of that version, downloaded and checked against its SHA-256, kept in a folder of its own among the
     * temporary files until Windows has installed it, once Mininotes has closed (see {@link #replaceAfterExit}).
     *
     * @return the checked .msi
     */
    static Path fetchMsi(String version,IntConsumer percent) throws Exception {
        String url=msi(version);
        if(url.isEmpty())throw new IOException("That is not a version.");
        Path folder=Files.createTempDirectory("mininotes-update-");
        Path msi=folder.resolve("Mininotes-Windows-"+Update.read(version)+".msi");
        try{download(url,version,msi,percent);return msi;}
        catch(Exception failed){delete(folder);throw failed;}
    }

    /** A release file saved, kept only if its checksum, published beside it, is there and matches. */
    private static void download(String url,String version,Path to,IntConsumer percent) throws Exception {
        HttpResponse<String> sum=HTTP.send(HttpRequest.newBuilder(URI.create(url+".sha256")).timeout(Duration.ofSeconds(30)).GET().build(),HttpResponse.BodyHandlers.ofString());
        String expected=sum.statusCode()==200?Update.digest(sum.body()):"";
        if(expected.isEmpty())throw new IOException("The release has no checksum for this version, so nothing was installed.");
        HttpResponse<InputStream> got=HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        if(got.statusCode()!=200)throw new IOException(Say.t("The release has no Windows build for {0} (HTTP {1}).",version,got.statusCode()));
        long size=got.headers().firstValueAsLong("content-length").orElse(-1);
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        try(InputStream in=got.body();OutputStream out=Files.newOutputStream(to)) {
            byte[] buffer=new byte[1<<16];long total=0;int read,shown=-1;
            while((read=in.read(buffer))>0) {
                total+=read;if(total>MOST)throw new IOException("The download is larger than any Mininotes build.");
                sha.update(buffer,0,read);out.write(buffer,0,read);
                int now=size>0?(int)(total*100/size):-1;if(now!=shown){shown=now;percent.accept(now);}
            }
        }
        if(!Update.hex(sha.digest()).equals(expected)){Files.deleteIfExists(to);throw new IOException("The download does not match its checksum, so nothing was installed.");}
    }

    /** Every entry, kept inside the folder: a name that climbs out of it refuses the whole zip. */
    static void unzip(Path zip,Path into) throws IOException {
        Path root=into.toAbsolutePath().normalize();
        try(ZipInputStream in=new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            for(ZipEntry entry;(entry=in.getNextEntry())!=null;) {
                Path target=root.resolve(entry.getName().replace('\\','/')).normalize();
                if(!target.startsWith(root)||target.equals(root))throw new IOException("The download has a file that would land outside its folder.");
                if(entry.isDirectory()){Files.createDirectories(target);continue;}
                Files.createDirectories(target.getParent());
                Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * The script that waits for this Mininotes to close, puts the new one where it was and starts it -
     * or, if any step fails, puts the old one back and starts that. Started now, hidden; the caller then
     * closes Mininotes.
     */
    static void replaceAfterExit(Path app,Path opened) throws IOException{replaceAfterExit(app,opened,ProcessHandle.current().pid(),true);}

    /** @param start whether to open Mininotes afterwards: yes for "Restart to update", no when it was simply closed */
    static Process replaceAfterExit(Path app,Path opened,long waitFor,boolean start) throws IOException {
        // What the MSI's copy fetched is an installer, not a folder: Windows puts it in place.
        if(opened.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".msi"))return run(installScript(app,opened,waitFor,start),"mininotes-update-");
        String stamp=Long.toString(System.currentTimeMillis());
        Path old=app.resolveSibling("."+app.getFileName()+".old-"+stamp);
        // Either way it fails, the mark is left first, so the old copy, started again, says the update did not finish (decision 264).
        String failed=markFailed(app,versionOf(opened));
        String script=String.join("\r\n",
            "$ErrorActionPreference='Stop'",
            "$app='"+ps(app)+"';$new='"+ps(opened)+"';$old='"+ps(old)+"';$staged='"+ps(opened.getParent())+"';$start=$"+start,
            "try{Wait-Process -Id "+waitFor+" -Timeout 120 -ErrorAction SilentlyContinue}catch{}",
            // Files of a program that has just closed can stay locked for a moment; a few tries.
            "$moved=$false;for($i=0;$i -lt 20 -and -not $moved;$i++){try{Move-Item -LiteralPath $app -Destination $old;$moved=$true}catch{Start-Sleep -Milliseconds 500}}",
            "if(-not $moved){"+failed+";if($start){Start-Process -FilePath (Join-Path $app 'Mininotes.exe')};exit 1}",
            "try{Move-Item -LiteralPath $new -Destination $app;if($start){Start-Process -FilePath (Join-Path $app 'Mininotes.exe')}}",
            "catch{if(Test-Path -LiteralPath $app){Remove-Item -LiteralPath $app -Recurse -Force};Move-Item -LiteralPath $old -Destination $app;"+failed+";if($start){Start-Process -FilePath (Join-Path $app 'Mininotes.exe')};exit 1}",
            "Start-Sleep -Seconds 5;Remove-Item -LiteralPath $old -Recurse -Force -ErrorAction SilentlyContinue;Remove-Item -LiteralPath $staged -Recurse -Force -ErrorAction SilentlyContinue",
            "");
        return run(script,"mininotes-update-");
    }

    /**
     * The newer MSI installed once this Mininotes has closed: Windows' installer, per user, showing its progress and asking
     * nothing (/qb), replaces the older install, which the notebook is not part of; then Mininotes is started from where it
     * was, the newer one if Windows installed it, this one if it did not. Still running after two minutes, nothing is done.
     */
    static String installScript(Path app,Path msi,long waitFor,boolean start) {
        return String.join("\r\n",
            "$ErrorActionPreference='Stop'",
            "$app='"+ps(app)+"';$msi='"+ps(msi)+"';$start=$"+start,
            "try{Wait-Process -Id "+waitFor+" -Timeout 120 -ErrorAction SilentlyContinue}catch{}",
            "if(Get-Process -Id "+waitFor+" -ErrorAction SilentlyContinue){exit 1}",
            "Start-Sleep -Milliseconds 800",
            // Its answer kept: 0 installed, 3010 installed and Windows wants a restart some time, anything else not installed.
            "$code=1;try{$done=Start-Process -FilePath '"+ps(msiexec())+"' -ArgumentList '"+install(msi).replace("'","''")+"' -Wait -PassThru;$code=$done.ExitCode}catch{}",
            // Not installed (1602 is the person cancelling it): the mark, so the next start says so (decision 264).
            "if($code -ne 0 -and $code -ne 3010){"+markFailed(app,versionOf(msi))+"}",
            "if($start){Start-Process -FilePath (Join-Path $app 'Mininotes.exe')}",
            "Remove-Item -LiteralPath (Split-Path -Parent $msi) -Recurse -Force -ErrorAction SilentlyContinue",
            "exit $code","");
    }
    /** What Windows' installer is told to install a Mininotes MSI with: its progress shown, nothing asked, no restart. */
    static String install(Path msi){return "/i \""+msi.toAbsolutePath()+"\" /qb /norestart";}
    /** And to uninstall the Mininotes Windows lists under that product code, the same way. */
    static String uninstall(String productCode){return "/x "+productCode+" /qb /norestart";}
    /** Windows' own installer. */
    static Path msiexec(){return Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","msiexec.exe");}

    /** A script written to a file and started now, hidden: what the update, the restart and the uninstall each run. */
    static Process run(String script,String prefix) throws IOException{return shell(script,prefix).start();}

    /**
     * How {@link #run} starts a script: PowerShell, hidden, in the folder the script was written to.
     *
     * <p>That folder, not this app's own: a program started without one works in the folder of the program that started
     * it, and Mininotes works in its own folder whenever Explorer or a shortcut opened it (a shortcut's Start in is that
     * folder, and so is a double-click). Windows does not let a folder some program works in be moved or deleted, so the
     * script held its own app folder, and every move of it failed: an unzipped copy was never replaced, the old one was
     * opened again, and nothing said so (a user's report, 2026-10-26; decision 260). Only a Mininotes started from
     * somewhere else, as the owner's was, updated.
     */
    static ProcessBuilder shell(String script,String prefix) throws IOException {
        Path file=Files.createTempFile(prefix,".ps1");
        // With the byte-order mark: Windows PowerShell reads a file without one in the old code page, and a
        // folder with an accent in its name would be a different folder.
        Files.writeString(file,"\ufeff"+script,StandardCharsets.UTF_8);
        String shell=Path.of(System.getenv().getOrDefault("SystemRoot","C:\\Windows"),"System32","WindowsPowerShell","v1.0","powershell.exe").toString();
        return new ProcessBuilder(shell,"-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-WindowStyle","Hidden","-File",file.toString())
            .directory(file.toAbsolutePath().getParent().toFile())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    /** This Mininotes started again once it has closed: the same hidden way an update starts it (decision 160). */
    static Process startAfterExit(Path exe) throws IOException {
        String script=String.join("\r\n",
            "try{Wait-Process -Id "+ProcessHandle.current().pid()+" -Timeout 120 -ErrorAction SilentlyContinue}catch{}",
            "Start-Sleep -Milliseconds 800",
            "Start-Process -FilePath '"+ps(exe)+"'","");
        return run(script,"mininotes-restart-");
    }

    /** A path inside single quotes in PowerShell: a quote is written twice. */
    static String ps(Path path){return path.toAbsolutePath().toString().replace("'","''");}

    static void delete(Path folder) throws IOException {
        if(!Files.exists(folder))return;
        try(var all=Files.walk(folder)){for(Path one:all.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(one);}
    }
}
