// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Android's every half hour or so, and once soon after Mininotes is left: the backups due made (see {@link PhoneBackups}). */
public final class PhoneBackupJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(()->{
            try{PhoneBackups.check(this);}catch(RuntimeException failed){android.util.Log.w("Mininotes/Backup","the backups' look failed: "+failed.getClass().getSimpleName());}
            jobFinished(params,false);
        },"mininotes-backups").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
