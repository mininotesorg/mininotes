// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The quiet part of a help request on the phone: a personal-safety alarm for this phone's OWN owner, like a panic button
 * (the owner, 2026-10-06: "it sends a help request to a contact with the phone location"; the alarm "should be set in
 * profile and all authorisation should be granted beforehand"; decision 115, reshaping decision 113). The owner sets it up
 * in their own Profile, where they can find it, change it and switch it off; it is not hidden from the owner, and the app
 * declares its location permission honestly. It is discreet only at the moment it fires, so that someone coercing the owner
 * in that moment does not notice: no popup, sound or vibration, and nothing logged.
 *
 * <p>When a note or folder the owner chose opens, this gathers the phone's place and hands each update to {@link
 * Post#sendHelp} to go out sealed, with nothing on screen. The place is the phone's last known position only ({@link
 * LocationManager#getLastKnownLocation}, the freshest across providers): no active GPS request, which is lighter and happens
 * not to raise the system's location indicator (decision 115). The first cry goes at once even with no position known.
 *
 * <p>The first update goes the moment the thing opens; then one every five minutes for an hour. The beat runs through {@link
 * AlarmManager} waking {@link HelpWaking}, set to fire even while the phone is idle, so the updates continue with the screen
 * off. <b>No foreground service</b> is used, because it would show a permanent notification; that is a UX choice, not
 * concealment. <b>What Android may still delay or stop:</b> an idle alarm can be delivered late, by up to about half again
 * the five minutes; Doze and app-standby may batch it; and if the system reclaims the process between updates, the live
 * incident is gone and the beat stops until the owner opens a chosen thing again (nothing of the incident is written to disk,
 * so a private trigger's recipients never leave the vault). <b>What stops it for good:</b> an hour from when the thing opened.
 *
 * <p>Holds the incidents in memory only. Nothing is logged: not the place, not who it went to, not the words.
 */
final class HelpAlarm {
    /** How long the updates go for, and how often, once a thing opens. */
    private static final long FOR=60L*60*1000, EVERY=5L*60*1000;

    private final MainActivity app;
    private final List<Incident> incidents=new ArrayList<>();
    /** The one live alarm, so {@link HelpWaking}, woken by AlarmManager while the process lives, can reach it. */
    private static volatile HelpAlarm live;

    HelpAlarm(MainActivity app){this.app=app;live=this;}

    /** One opening: the recipients and words it was set with, named by sixteen random bytes, going for an hour. */
    private static final class Incident {
        final byte[] id=new byte[Help.INCIDENT];
        final List<String> to; final String message; final long until;
        final AtomicInteger number=new AtomicInteger();
        Incident(List<String> to,String message) {
            new java.security.SecureRandom().nextBytes(id);
            this.to=new ArrayList<>(to); this.message=message==null?"":message; this.until=System.currentTimeMillis()+FOR;
        }
    }

    /**
     * A chosen note or folder has opened: a new incident, its first update sent at once with the last known place, and the
     * five minute beat set for an hour. Called on the main thread, once per opening (MainActivity and PrivateScreen guard
     * against a double fire on the same opening). The first send rides a background thread so the opening is not held up.
     */
    void raise(List<String> to,String message) {
        if(to==null||to.isEmpty())return;
        final Incident inc=new Incident(to,message);
        synchronized(this){incidents.add(inc);}
        // The last known place at once, whatever it is, even none: a cry for help is not held back for a fix (decision 115).
        app.background.submit(()->{send(inc);return null;},done->{},e->{});
        schedule();
    }

    /**
     * Woken by AlarmManager, even with the screen off and the phone idle (see {@link HelpWaking}): an update for each
     * incident still inside its hour, the expired ones let go. Runs on the receiver's own worker thread, which keeps the
     * phone awake until the sends are done, so each goes blocking.
     */
    static void woken() {
        HelpAlarm it=live;
        if(it!=null)it.beat();
    }

    private void beat() {
        long now=System.currentTimeMillis();
        List<Incident> going;
        synchronized(this) {
            incidents.removeIf(inc->now>=inc.until);
            going=new ArrayList<>(incidents);
        }
        for(Incident inc:going)send(inc);
        schedule();
    }

    /** The next wake-up in five minutes while any incident is still inside its hour; with none left, the alarm is cancelled. */
    private void schedule() {
        AlarmManager alarms=app.getSystemService(AlarmManager.class);
        if(alarms==null)return;
        boolean any; synchronized(this){any=!incidents.isEmpty();}
        if(any)alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+EVERY,pending(app));
        else alarms.cancel(pending(app));
    }

    private static PendingIntent pending(Context any) {
        Context app=any.getApplicationContext();
        return PendingIntent.getBroadcast(app,1,new Intent(app,HelpWaking.class),PendingIntent.FLAG_IMMUTABLE);
    }

    /** One update of one incident, with the last known place or none, sealed and sent (blocking). Nothing is logged. */
    private void send(final Incident inc) {
        final Location loc=lastKnown();
        final boolean located=loc!=null;
        final Help.Request request=new Help.Request(inc.id,inc.number.getAndIncrement(),inc.message,located,
            located?loc.getLatitude():0,located?loc.getLongitude():0,located&&loc.hasAccuracy()?loc.getAccuracy():0,
            located?loc.getTime():0,System.currentTimeMillis());
        final NoteStore store=app.store; final Keys keys=app.keys();
        if(store==null||keys==null)return;
        Post.sendHelp(app,store,keys,request,new ArrayList<>(inc.to));
    }

    /** Whether the owner has let the app have the place at all: asked in Profile when the request is set up (decision 115). */
    private boolean mayLocate() {
        return app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            ||app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }

    private LocationManager manager() {
        return (LocationManager)app.getSystemService(Context.LOCATION_SERVICE);
    }

    /** The freshest place the phone already has, or null where there is none, or none is allowed: no active fix (decision 115). */
    private Location lastKnown() {
        if(!mayLocate())return null;
        LocationManager lm=manager();if(lm==null)return null;
        Location best=null;
        try {
            for(String provider:lm.getProviders(true)) {
                Location one=lm.getLastKnownLocation(provider);
                if(one!=null&&(best==null||one.getTime()>best.getTime()))best=one;
            }
        } catch(SecurityException none){return null;}
        return best;
    }
}
