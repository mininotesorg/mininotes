// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Big files on the phone (see {@link Big}, decision 327): one offered here is a notification with Save and Refuse, which work
 * with the app closed; saved straight into Downloads, the notification saying how far it is, then that it is there. From
 * somebody whose files are taken without asking, saved at once. And one sent from here is read from what the other app handed
 * over, never copied in.
 */
final class PhoneBig {
    private PhoneBig(){}

    static final String SAVE="org.mininotes.android.BIG_SAVE", REFUSE="org.mininotes.android.BIG_REFUSE", TOKEN="token";
    /** The channel things shared with you are said on (Listening's). */
    private static final String CHANNEL="shared";

    /** Heard from now on: offers that come are asked about in a notification, or saved. Said when the phone starts listening. */
    static void listen(Context any) {
        Context app=any.getApplicationContext();
        Big.asking=one->ask(app,one);
        Big.saving=one->new Thread(()->save(app,one),"mininotes-big").start();
    }

    private static int id(String token){return 1000+(token.hashCode()&0x3fffffff)%100000;}

    /** What it is, from whom, with Save and Refuse; a tap opens the app. */
    static void ask(Context app,Big.Waiting one) {
        NotificationManager all=app.getSystemService(NotificationManager.class);
        if(all==null)return;
        String said=Say.t("{0} wants to send you “{1}” ({2}).",one.name,one.offer.name,Attachment.size(one.offer.bytes));
        Notification.Builder shown=new Notification.Builder(app,CHANNEL).setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(Say.t("A file from {0}",one.name)).setContentText(said)
            .setStyle(new Notification.BigTextStyle().bigText(said+"\n"+Say.t("Saved straight into Downloads, not kept in Mininotes.")))
            .setAutoCancel(false)
            .setContentIntent(PendingIntent.getActivity(app,0,new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_IMMUTABLE))
            .addAction(new Notification.Action.Builder(null,Say.t("Refuse"),action(app,REFUSE,one.offer.token)).build())
            .addAction(new Notification.Action.Builder(null,Say.t("Save"),action(app,SAVE,one.offer.token)).build());
        try{all.notify(id(one.offer.token),shown.build());}catch(RuntimeException refused){/* notifications off */}
    }

    private static PendingIntent action(Context app,String what,String token) {
        Intent asked=new Intent(app,Listening.class).setAction(what).putExtra(TOKEN,token);
        return PendingIntent.getService(app,(what+token).hashCode(),asked,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Save or Refuse pressed: done off the service's thread. Whether this was one of them. */
    static boolean heard(Context app,Intent intent) {
        if(intent==null||!(SAVE.equals(intent.getAction())||REFUSE.equals(intent.getAction())))return false;
        String token=intent.getStringExtra(TOKEN);
        Big.Waiting one=Big.waitingFor(token);
        if(one==null){if(token!=null)tell(app,id(token),Say.t("That file is no longer offered."),null);return true;}
        if(REFUSE.equals(intent.getAction()))new Thread(()->{Post.refuseBig(app,NoteStore.of(app),one);cancel(app,id(token));},"mininotes-big").start();
        else new Thread(()->save(app,one),"mininotes-big").start();
        return true;
    }

    /** Fetched from the sender's door into Downloads; what came thrown away where it failed. Blocking. */
    static void save(Context app,Big.Waiting one) {
        int id=id(one.offer.token);
        if(android.os.Build.VERSION.SDK_INT<29){tell(app,id,Big.needsAndroid10(),null);return;}
        NotificationManager all=app.getSystemService(NotificationManager.class);
        Notification.Builder going=new Notification.Builder(app,CHANNEL).setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(Say.t("Receiving “{0}”…",one.offer.name)).setOngoing(true).setOnlyAlertOnce(true).setProgress(100,0,false);
        try{if(all!=null)all.notify(id,going.build());}catch(RuntimeException refused){/* notifications off */}
        ContentResolver resolver=app.getContentResolver();
        ContentValues about=new ContentValues();
        about.put(MediaStore.MediaColumns.DISPLAY_NAME,one.offer.name);
        about.put(MediaStore.MediaColumns.MIME_TYPE,one.offer.kind.isEmpty()?"application/octet-stream":one.offer.kind);
        about.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri made=null;
        try {
            made=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,about);
            if(made==null)throw new IOException("Downloads refused the file");
            long[] said={0};
            try(OutputStream out=new BufferedOutputStream(resolver.openOutputStream(made,"w"),1<<16)) {
                Post.saveBig(app,NoteStore.of(app),one,out,here->{
                    long now=System.currentTimeMillis();if(now-said[0]<700||all==null)return;said[0]=now;
                    going.setProgress(100,(int)(here*100/Math.max(1,one.offer.bytes)),false);
                    try{all.notify(id,going.build());}catch(RuntimeException refused){/* notifications off */}
                });
            }
            about.clear();about.put(MediaStore.MediaColumns.IS_PENDING,0);resolver.update(made,about,null,null);
            tell(app,id,Say.t("{0} is in Downloads",one.offer.name),new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        } catch(Exception failed) {
            if(made!=null)try{resolver.delete(made,null,null);}catch(RuntimeException gone){/* not there */}
            android.util.Log.w("Mininotes/Big","a big file not saved: "+failed.getClass().getSimpleName());
            // The offer still stands (Big.answered runs only once it is whole): the way to get it, and Save again (2026-11-07).
            again(app,id,one);
        }
    }

    /** Did not come whole: what to do, and Save again, which fetches it anew while the sender still offers it. */
    private static void again(Context app,int id,Big.Waiting one) {
        NotificationManager all=app.getSystemService(NotificationManager.class);
        if(all==null)return;
        String said=Big.notWhole(one.name);
        Notification.Builder shown=new Notification.Builder(app,CHANNEL).setSmallIcon(R.drawable.ic_listening)
            .setContentTitle(Big.notWholeTitle(one.offer.name)).setContentText(said).setStyle(new Notification.BigTextStyle().bigText(said))
            .setAutoCancel(true)
            .addAction(new Notification.Action.Builder(null,Say.t("Save again"),action(app,SAVE,one.offer.token)).build());
        try{all.notify(id,shown.build());}catch(RuntimeException refused){/* notifications off */}
    }

    private static void tell(Context app,int id,String said,Intent open) {
        NotificationManager all=app.getSystemService(NotificationManager.class);
        if(all==null)return;
        Notification.Builder shown=new Notification.Builder(app,CHANNEL).setSmallIcon(R.drawable.ic_listening).setContentTitle(said).setAutoCancel(true);
        if(open!=null)shown.setContentIntent(PendingIntent.getActivity(app,id,open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_IMMUTABLE));
        try{all.notify(id,shown.build());}catch(RuntimeException refused){/* notifications off */}
    }
    private static void cancel(Context app,int id){NotificationManager all=app.getSystemService(NotificationManager.class);if(all!=null)try{all.cancel(id);}catch(RuntimeException none){/* gone */}}

    // ---- sending ---------------------------------------------------------------------------------------------

    /** Read from what the other app handed over, from a place in it. */
    static Big.Source from(Context app,Uri file) {
        // Kept readable while this runs, where the picker allows it: the other device may save it hours later.
        try{app.getContentResolver().takePersistableUriPermission(file,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(RuntimeException notOffered){/* for as long as the app runs */}
        return at->{
            InputStream in=app.getContentResolver().openInputStream(file);
            if(in==null)throw Big.broken("no-stream");
            long left=at;
            while(left>0){long n=in.skip(left);if(n<=0){if(in.read()<0){in.close();throw Big.broken("the-file-is-shorter-than-it-was");}n=1;}left-=n;}
            return in;
        };
    }
}
