// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The node kept up while the pad is closed, so a note can arrive when nobody is looking.
 *
 * <p>Maxima is not a shelf: nothing waits anywhere for this phone to come and fetch it. A note sent while
 * nobody is attached is a note a relay took in on behalf of nobody. So for as long as the node lived only
 * while the pad was on screen, sharing worked when two people happened to have the app open together and
 * did nothing at all when they did not — demonstrable rather than usable.
 *
 * <p>Android will let a process stay up for this on one condition, which is that it says so: a notification
 * for as long as it lasts. That is the right condition. It is up only while there is somebody to hear from —
 * a pad that has never been paired with anything has nothing to listen for and keeps nothing running — and
 * it can be switched off from Profile, or from the notification itself, which is where a person looking at
 * it and wondering is already standing.
 *
 * <p>The node itself is {@link Node}'s, and belongs to the process rather than to this. What this holds is
 * the process.
 */
public final class Listening extends Service {
    private static final String STAYING="listening", ARRIVED="arrived", ALARM="help";
    private static final String STOP="org.mininotes.android.STOP_LISTENING";
    /** Carried by a notification that is about one note, so tapping it opens that note. */
    static final String NOTE="note";
    /** Said on the way into the pad from the notification that files came: the list of them is opened. */
    static final String RECEIVED="received";
    private static final int ONGOING=1, LANDED=2, ACCEPTED=3, FILES=4, WAITING=5, SHARED=6, NEWS=7;
    /**
     * Everything that arrived while nobody was looking, in one line with its count (the owner, 2026-10-22: "On the phones we
     * should have a notification at the phone level and on the app icon on the desktop when something new arrives"; decision
     * 236). A channel of its own, which may sound, because the old quiet one is the person's to have set as they like, and
     * an update must not take that from them: it stays, said nothing more.
     */
    private static final String NEWS_CHANNEL="news";
    /** What came since the app was last opened. Kept by this process; one that was ended starts counting again. */
    private static final ArrivalNews news=new ArrivalNews();
    /** Something shared with me, said so it is seen: louder than an arrival, which is every few seconds while somebody writes. */
    private static final String SHARED_CHANNEL="shared";
    /** What a line was said for already, by the thing it waits under (decision 109): once a run, not once a revision. */
    private static final java.util.Set<String> SAID_WAITING=java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Whoever is on screen, told as things land. Null while nobody is, and then the phone says it instead. */
    private static volatile Consumer<Post.Landed> watcher;
    /** Offers taken up while nobody was there to be asked. Put to them the moment somebody is. */
    private static final List<Hello.Said> unanswered=new ArrayList<>();
    private static boolean hearing;

    /** Whether the person wants this at all. They do until they say otherwise. */
    static boolean switchedOn(Context any) {
        return any.getSharedPreferences("settings",Context.MODE_PRIVATE).getBoolean("listening",true);
    }

    static void switchOn(Context any,boolean on) {
        any.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("listening",on).apply();
    }

    /**
     * Started where it should be running and stopped where it should not. Blocking: it asks the notebook
     * whether there is anybody to hear from.
     *
     * <p>Called while the pad is on screen, which is also the honest moment, since it is when the person can see it
     * begin; and after the phone starts and after an update, two moments Android allows it too (see Starting).
     */
    static void settle(Context any) {
        Context app=any.getApplicationContext();
        Intent service=new Intent(app,Listening.class);
        try {
            // A locked notebook cannot say whether anybody is paired; it listens, and what comes waits sealed.
            if(switchedOn(app)&&(!PhoneLock.open(app)||NoteStore.of(app).anybodyPaired()))app.startForegroundService(service);
            else{app.stopService(service);Waking.stop(app);}
        } catch(RuntimeException notAllowed){/* not on screen after all; the next opening settles it */}
    }

    /** Who is on screen, or null when nobody is any more. */
    static void watch(Consumer<Post.Landed> there){synchronized(WATCHING){watcher=there;}Node.near(there!=null);}

    /**
     * Taken down - but only by whoever put it up. A screen that is going away after another has already
     * come up must not leave the one people are looking at with nobody telling it anything.
     */
    static void unwatch(Consumer<Post.Landed> there) {
        synchronized(WATCHING){if(watcher!=there)return;watcher=null;}
        Node.near(false);
    }
    /**
     * Watching's own lock, not the class's: {@link #hear} holds that while the node starts, which takes seconds, and the
     * screen calls these as it comes and goes, so it would wait there and Android would say Mininotes is not responding.
     */
    private static final Object WATCHING=new Object();

    /** Every offer taken up while nobody was looking, handed over once. */
    static List<Hello.Said> unanswered() {
        synchronized(unanswered) {
            List<Hello.Said> all=new ArrayList<>(unanswered);
            unanswered.clear();
            return all;
        }
    }

    /**
     * The node told where to bring what arrives. Blocking — it starts the node if nothing has — and safe to
     * ask for twice.
     *
     * <p>It belongs to the process and not to a screen. A screen used to set it, holding its own notebook,
     * and a screen goes away: what arrived afterwards was handed to a notebook that had been closed.
     */
    static synchronized void hear(Context any) {
        final Context app=any.getApplicationContext();
        // Locked, and nobody has opened it: what arrives is kept as it came, sealed for this phone, until it is.
        if(!PhoneLock.open(app)) {
            if(!hearing)hearing=Node.listen(app,message->PhoneLock.keepArriving(app,message));
            return;
        }
        final NoteStore store=NoteStore.of(app);
        final Keys keys=Keys.of(app);
        // The notebook names this phone in every membership it sends, and leaves this phone out of every
        // one that arrives. Without being told who that is, it would take its own entry in somebody's list
        // for a stranger and start sharing with itself. Said again every time, because the name and the
        // address are both things that change.
        try {
            store.mySigningKey=android.util.Base64.encodeToString(
                keys.signing().getPublic().getEncoded(),android.util.Base64.NO_WRAP);
            store.myAgreement=Point.shorten(keys.agreement().getPublic());
            store.myName=Node.nameHere(app);
            store.myAddress=app.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("address","");
        } catch(Exception notNow){return;}
        if(hearing&&heardForReal)return;
        // What was handed over and never answered is sent again from the node's own upkeep, so it goes on
        // happening for as long as the process lives, screen or no screen.
        Node.everyBeat(()->Post.again(app,store,keys));
        heardForReal=true;
        // What happens to files sent on their own away from an arrival - fetched, answered - goes where an arrival does.
        Post.news=landed->told(app,landed);
        hearing=Node.listen(app,message->{
            // While the notebook is being swapped for its locked or unlocked copy, nothing is written into it.
            if(PhoneLock.busy){PhoneLock.keepArriving(app,message);return;}
            told(app,Post.arrived(app,store,keys,message));
        });
    }

    /** Something landed: to the screen, if one is watching, and otherwise said in one line on the phone. */
    private static void told(Context app,Post.Landed landed) {
        if(landed==null)return;
        Consumer<Post.Landed> there=watcher;
        if(there!=null){there.accept(landed);return;}
        // A help request arrived while nobody was in the pad (decision 113): a loud, high-priority line, HELP from whoever
        // sent it, with the words, the place, the time and a map link, filled in as the updates come. In front it is the
        // in-app box instead (MainActivity.heard). Nothing is logged about it.
        if(landed.helpRequest!=null){sayHelp(app,landed);return;}
        // A message from a 0.3.036 build (decision 145, withdrawn in 147): kept and answered, said nowhere.
        if(landed.message)return;
        // Somebody saying they have something is for the marks, not for the person.
        if(landed.answered)return;
        // Something another person shared with me for the first time (the owner, 2026-10-06; decision 109): counted once for
        // each thing, never for each of its revisions, and the pad asks Accept or Refuse when it is next in front.
        if(landed.waiting) {
            if(landed.said!=null&&SAID_WAITING.add(landed.waits))gathered(app,ArrivalNews.Kind.WAITING,landed,landed.waits);
            return;
        }
        // Shared with me, and here without waiting for an answer (a file from another device of mine): counted once, to decide.
        if(landed.shared) {
            if(landed.said!=null&&SAID_WAITING.add(landed.waits))gathered(app,ArrivalNews.Kind.WAITING,landed,landed.waits);
            return;
        }
        // Files sent on their own: that they came, or that somebody wants to send some. Never what they are called. What came
        // of a sending from here (reached, refused) is news too, of another kind.
        if(landed.files) {
            if(landed.said!=null)gathered(app,landed.many>0?ArrivalNews.Kind.FILES:ArrivalNews.Kind.OTHER,landed,null);
            return;
        }
        if(landed.accepted!=null) {
            // Not something that arrived but a question about a person: its own line, where it was always asked.
            synchronized(unanswered){unanswered.add(landed.accepted);}
            if(landed.accepted.target.isEmpty())say(app,ACCEPTED,Say.t("{0} wants to add you",landed.accepted.name),
                Say.t("Open the pad to accept or ignore."),"",false,SHARED_CHANNEL);
            else say(app,ACCEPTED,Say.t("{0} accepted what you offered",landed.accepted.name),
                Say.t("Open the pad to send it to them."),"",false,SHARED_CHANNEL);
        } else if(landed.said!=null) {
            // A note shared with me or written in, counted by the note; anything else (somebody left, refused, removed me)
            // each on its own, since each is a thing that happened once. Landed.left is the one with "people" said.
            boolean note=!landed.from.isEmpty()&&!landed.people&&!landed.note.isEmpty();
            gathered(app,!note?ArrivalNews.Kind.OTHER:landed.newly?ArrivalNews.Kind.SHARED:ArrivalNews.Kind.UPDATED,landed,note?landed.note:null);
        }
    }

    /** Listening again, with the notebook as it now is: after the lock went on or came off. */
    static synchronized void rehear(Context any){heardForReal=false;hear(any);}
    /** Locked again: the upkeep stops writing, and whatever arrives waits sealed in the inbox. */
    static synchronized void relock(Context any){Node.everyBeat(null);heardForReal=false;hearing=false;hear(any);}
    private static boolean heardForReal;

    // ---- the service itself ----------------------------------------------------------------------------

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null&&STOP.equals(intent.getAction())) {
            // From the notification: the way out is where the thing is. Remembered, so it stays off.
            switchOn(this,false);Waking.stop(this);
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if(Build.VERSION.SDK_INT>=34)
                startForeground(ONGOING,ongoing(),ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
            else startForeground(ONGOING,ongoing());
        } catch(RuntimeException refused) {
            // Brought back by the system with nobody on screen, which newer Android does not always allow.
            // Nothing is lost by stopping: the next time the pad is opened it is started again.
            stopSelf();
            return START_NOT_STICKY;
        }
        // Off this thread: starting a node means reaching relays, which takes as long as it takes.
        new Thread(()->{
            try {
                if(!switchedOn(this)||PhoneLock.open(this)&&!NoteStore.of(this).anybodyPaired()){stopSelf();return;}
                hear(this);
                Waking.again(this);
            } catch(Exception notNow){/* the process is up, and the next opening tries again */}
        },"mininotes-listening").start();
        return START_STICKY;
    }

    private Notification ongoing() {
        channels(this);
        PendingIntent stop=PendingIntent.getService(this,0,
            new Intent(this,Listening.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,STAYING)
            .setSmallIcon(R.drawable.ic_listening)
            .setContentTitle("Listening for notes")
            .setContentText("So a note can arrive while the pad is closed.")
            .setContentIntent(opening(this,""))
            .setOngoing(true).setShowWhen(false)
            .addAction(new Notification.Action.Builder(null,"Stop listening",stop).build())
            .build();
    }

    // ---- saying that something arrived -----------------------------------------------------------------

    /**
     * One line, saying who and what kind of thing — never what the note says. A notification is read by
     * whoever is holding the phone, locked or not, and by anything else allowed to read notifications. What
     * arrived is said in one line together now ({@link #gathered}); this is left for a question about a person.
     *
     * @param received whether tapping it opens Received files rather than a note
     */
    private static void say(Context app,int id,String title,String text,String note,boolean received,String channel) {
        channels(app);
        Notification.Builder said=new Notification.Builder(app,channel)
            .setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(title)
            .setContentIntent(received?openingReceived(app):opening(app,note))
            .setAutoCancel(true);
        if(!text.isEmpty())said.setContentText(text);
        NotificationManager all=app.getSystemService(NotificationManager.class);
        // Refused permission is an answer: the note still landed, and the shelves will show it.
        if(all!=null)try{all.notify(id,said.build());}catch(RuntimeException refused){/* as above */}
    }

    /**
     * A help request, said loud (decision 113): "HELP from <name>", the words, the place, the time and a map link. One line
     * for each opening, named by its incident, so the updates of it fill in the same line rather than piling up. A
     * high-priority channel, so it is a heads-up the owner of the phone that gets it cannot miss. Nothing is logged.
     */
    private static void sayHelp(Context app,Post.Landed landed) {
        channels(app);
        Help.Request r=landed.helpRequest;
        java.text.DateFormat when=java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
        String body=Help.body(r.message,r.located,r.lat,r.lon,r.metres,r.located?when.format(new java.util.Date(r.placeAt)):"");
        Notification.Builder said=new Notification.Builder(app,ALARM)
            .setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(Help.title(landed.helpFrom))
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(opening(app,""))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true);
        NotificationManager all=app.getSystemService(NotificationManager.class);
        // One id per incident, so an update of the same opening replaces its line rather than making a new one.
        int id=2_000_000+java.util.Arrays.hashCode(r.incident);
        if(all!=null)try{all.notify(id,said.build());}catch(RuntimeException refused){/* refused is an answer; it still arrived */}
    }

    /**
     * One more thing in the one line of what arrived (decision 236), said again with the new count: one notification, its
     * number set so a launcher that shows counts on the icon shows it. It sounds only for the first arrival since the app
     * was opened or after a quiet spell ({@link ArrivalNews#QUIET}); the rest update it in silence.
     */
    private static void gathered(Context app,ArrivalNews.Kind kind,Post.Landed landed,String key) {
        String title,text,note;boolean sound,files;int total;
        synchronized(news) {
            sound=news.arrived(kind,landed.from,key,Math.max(1,landed.many),landed.said,System.currentTimeMillis());
            title=news.title();text=news.text();note=news.onlyNote();files=news.onlyFiles();total=news.total();
        }
        channels(app);
        Notification.Builder said=new Notification.Builder(app,NEWS_CHANNEL)
            .setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(title)
            .setContentText(text)
            .setNumber(total)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
            .setContentIntent(files?openingReceived(app):opening(app,note))
            // Said again with one more in it: not heard again, unless it is the first after a quiet spell.
            .setOnlyAlertOnce(!sound)
            .setAutoCancel(true);
        NotificationManager all=app.getSystemService(NotificationManager.class);
        // Refused permission is an answer: it still landed, and the shelves will show it.
        if(all!=null)try{all.notify(NEWS,said.build());}catch(RuntimeException refused){/* as above */}
    }

    /**
     * The app is in front: everything that arrived is seen, so its line goes and the count starts again (decision 236). The
     * lines said one by one before it, by a build before this one, go too: they were about the same things.
     */
    static void seen(Context any) {
        synchronized(news){news.clear();}
        NotificationManager all=any.getApplicationContext().getSystemService(NotificationManager.class);
        if(all==null)return;
        try{for(int id:new int[]{NEWS,LANDED,FILES,WAITING,SHARED})all.cancel(id);}catch(RuntimeException refused){/* nothing to take away */}
    }

    private static PendingIntent opening(Context app,String note) {
        Intent open=new Intent(app,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(note!=null&&!note.isEmpty())open.putExtra(NOTE,note);
        // One per note, or the second notification's note would quietly replace the first one's.
        return PendingIntent.getActivity(app,note==null?0:note.hashCode(),open,
            PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent openingReceived(Context app) {
        Intent open=new Intent(app,MainActivity.class).putExtra(RECEIVED,true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(app,RECEIVED.hashCode(),open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Two channels, because they are two different things to want less of: the line that says the pad is
     * listening, and the line that says something came. Both quiet. A shared list being written in sends
     * every few seconds, and a pad that chimed each time would be switched off within the hour; anybody
     * who wants a sound can turn that one channel up, which is what channels are for.
     *
     * <p>What came is now said on "news" instead (decision 236), which may sound, and does so once for each time somebody
     * starts after a quiet spell rather than for each of those few seconds ({@link ArrivalNews#QUIET}). "arrived" is still
     * made, never deleted: deleting a channel loses what the person set on it, and the system keeps count of deleted ones.
     */
    private static void channels(Context app) {
        NotificationManager all=app.getSystemService(NotificationManager.class);
        if(all==null)return;
        NotificationChannel staying=new NotificationChannel(STAYING,"Listening for notes",
            NotificationManager.IMPORTANCE_LOW);
        staying.setDescription("Shown for as long as the pad is listening while closed.");
        staying.setShowBadge(false);
        NotificationChannel arrived=new NotificationChannel(ARRIVED,"A note arrived",
            NotificationManager.IMPORTANCE_LOW);
        arrived.setDescription("Somebody shared a note with you, or wrote in one you share.");
        NotificationChannel alarm=new NotificationChannel(ALARM,"A help request",
            NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription("Somebody you share with sent a help request from Mininotes.");
        NotificationChannel shared=new NotificationChannel(SHARED_CHANNEL,Say.t("Shared with you"),NotificationManager.IMPORTANCE_DEFAULT);
        shared.setDescription(Say.t("Somebody, or another device of yours, shared something with you."));
        all.createNotificationChannel(shared);
        all.createNotificationChannel(staying);
        all.createNotificationChannel(arrived);
        all.createNotificationChannel(alarm);
        NotificationChannel news=new NotificationChannel(NEWS_CHANNEL,Say.t("What arrived"),NotificationManager.IMPORTANCE_DEFAULT);
        news.setDescription(Say.t("Notes, files and things shared with you, gathered in one line"));
        news.setShowBadge(true);
        all.createNotificationChannel(news);
    }
}
