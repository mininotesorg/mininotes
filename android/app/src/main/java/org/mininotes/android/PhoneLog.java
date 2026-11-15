// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The phone's log kept in a file (see {@link LogKeep}): every two minutes while the app's process lives, and once more when
 * the person shares it, what this process wrote to the system's log since the last time is copied out. An app may read its
 * own process's log and nothing else's, which is all that is asked here.
 */
final class PhoneLog {
    private PhoneLog(){}

    private static ScheduledExecutorService keeping;

    static File folder(Context any){return new File(any.getApplicationContext().getFilesDir(),"logs");}

    /** Started once per process; a second call does nothing. */
    static synchronized void start(Context any) {
        if(keeping!=null)return;
        final Context app=any.getApplicationContext();
        keeping=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"mininotes-log");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
        keeping.scheduleWithFixedDelay(()->keep(app),20,120,TimeUnit.SECONDS);
    }

    /** What the system's log holds of this process since the last line kept, added to the file. Never throws. */
    static synchronized void keep(Context any) {
        File folder=folder(any),last=new File(folder,"last");
        try {
            String since=last.isFile()?new String(Files.readAllBytes(last.toPath()),StandardCharsets.UTF_8).trim():"";
            List<String> command=new ArrayList<>(List.of("logcat","-d","-v","threadtime","--pid="+android.os.Process.myPid()));
            List<String> lines=new ArrayList<>();
            Process logcat=new ProcessBuilder(command).redirectErrorStream(true).start();
            try(BufferedReader in=new BufferedReader(new InputStreamReader(logcat.getInputStream(),StandardCharsets.UTF_8))) {
                for(String line=in.readLine();line!=null;line=in.readLine())lines.add(line);
            }
            logcat.waitFor(10,TimeUnit.SECONDS);
            List<String> adding=LogKeep.after(lines,since);
            if(adding.isEmpty())return;
            LogKeep.add(folder,adding);
            Files.write(last.toPath(),LogKeep.stamp(adding.get(adding.size()-1)).getBytes(StandardCharsets.UTF_8));
        } catch(Exception notNow){/* the next time */}
    }
}
