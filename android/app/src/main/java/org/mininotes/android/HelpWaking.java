// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The phone's own alarm that keeps a help request's updates going while the phone sleeps (the owner, 2026-10-06, decision
 * 115). Set by {@link HelpAlarm} through AlarmManager to fire even while the phone is idle, so an update still goes with the
 * screen off; only this app sets it. It wakes the live {@link HelpAlarm}, which sends an update for each opening still inside
 * its hour. Nothing is logged here, as nothing is anywhere about a help request. If the process was reclaimed between
 * updates the live alarm is gone and this does nothing, which is a limitation, not concealment (see {@link HelpAlarm}).
 */
public final class HelpWaking extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        // The phone stays awake on this thread until the sends are done, and not a moment longer.
        final PendingResult finished=goAsync();
        new Thread(()->{
            try{HelpAlarm.woken();}
            catch(Throwable quietly){/* nothing said: a help request is kept fully quiet */}
            finally{finished.finish();}
        },"mininotes-help").start();
    }
}
