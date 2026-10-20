// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Listening again by itself after the phone starts and after the app is updated (the owner, 2026-10-20: "Start by itself";
 * decision 226).
 *
 * <p>An update stops the app, and so does a restart of the phone; until then the pad listened again only once it was opened.
 * Seen on Graphene: a 0.4.012 installed at night had not run by the morning, so the Integritas key sent to it from the PC
 * waited for somebody to open it. These two moments are ones Android lets a listening service start from, with its line in
 * the notifications as always; whether it should listen at all is still {@link Listening#settle}'s to say, as at an opening.
 * Only the system sends these two, and only for this app.
 */
public final class Starting extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        String what=intent==null?null:intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(what)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(what))return;
        final Context app=context.getApplicationContext();
        if(!Listening.switchedOn(app))return;
        // Settling asks the notebook whether anybody is paired: off this thread, the phone kept awake until it is said.
        final PendingResult finished=goAsync();
        new Thread(()->{
            try{Listening.settle(app);}
            catch(Throwable notNow){/* at the next opening, then */}
            finally{finished.finish();}
        },"mininotes-starting").start();
    }
}
