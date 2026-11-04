// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The owner's phones' backup before an install, in the fast build only (the owner, 2026-11-01: "we should try to make the app as
 * fast as possible"; decision 311). GrapheneOS compiles no app as it runs and never compiles a debuggable one, so the debug build
 * ran interpreted there; the fast build is not debuggable, so run-as, which the backups used, cannot reach its folder. This copies
 * databases, files and shared_prefs, as run-as did, into the app's own folder on shared storage, which only the app and adb's
 * shell can read, and says "done" there with how many files; the PC pulls it, checks every SHA-256 and takes it away again.
 *
 * <p>Sent only by the shell (the manifest asks for DUMP, which no ordinary app can hold), and only after the app was stopped, so
 * nothing writes while it copies: the process this starts opens no notebook. Never in a published build.
 */
public final class ShellBackup extends BroadcastReceiver {
    static final String[] FOLDERS={"databases","files","shared_prefs"};

    @Override public void onReceive(Context context,Intent intent) {
        final PendingResult later=goAsync();
        final Context app=context.getApplicationContext();
        new Thread(()->{
            try{copy(app);}catch(IOException failed){android.util.Log.w("Mininotes/Backup","could not copy: "+failed.getClass().getSimpleName());}
            finally{later.finish();}
        },"mininotes-shell-backup").start();
    }

    private static void copy(Context app) throws IOException {
        File shared=app.getExternalFilesDir(null);
        if(shared==null)throw new IOException("no shared storage");
        File to=new File(shared,"shell-backup");
        wipe(to);
        if(!to.mkdirs()&&!to.isDirectory())throw new IOException("cannot make the folder");
        File data=app.getDataDir();
        int[] count={0};long[] bytes={0};
        for(String one:FOLDERS)copyTree(new File(data,one),new File(to,one),count,bytes);
        try(OutputStream done=new FileOutputStream(new File(to,"done"))) {
            done.write((count[0]+" "+bytes[0]+"\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        android.util.Log.i("Mininotes/Backup","copied for the shell: "+count[0]+" files, "+bytes[0]+" bytes");
    }

    private static void copyTree(File from,File to,int[] count,long[] bytes) throws IOException {
        if(!from.exists())return;
        if(from.isDirectory()) {
            if(!to.mkdirs()&&!to.isDirectory())throw new IOException("cannot make a folder");
            File[] inside=from.listFiles();
            if(inside!=null)for(File one:inside)copyTree(one,new File(to,one.getName()),count,bytes);
            return;
        }
        try(InputStream in=new FileInputStream(from);OutputStream out=new FileOutputStream(to)) {
            byte[] buffer=new byte[1<<16];int n;
            while((n=in.read(buffer))>0){out.write(buffer,0,n);bytes[0]+=n;}
        }
        count[0]++;
    }

    private static void wipe(File what) {
        if(!what.exists())return;
        File[] inside=what.listFiles();
        if(inside!=null)for(File one:inside)wipe(one);
        if(!what.delete())android.util.Log.w("Mininotes/Backup","could not take away an older copy");
    }
}
