// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Backup page's backups on the phone (decision 329): into a folder the owner picked (a tree the phone's storage lent,
 * kept with a lasting permission), sealed, made when their time comes by a job Android runs every half hour or so, and when
 * Mininotes is left where that is chosen. The rules are {@link Backups}; the file is the one Export writes, so Add from a
 * backup opens it on any phone or PC.
 */
final class PhoneBackups {
    private PhoneBackups(){}

    static final String KEPT="backups";
    /** The request the folder picker answers. */
    static final int PICK=44;
    private static final int JOB=4401,SOON=4402;
    private static final String BACKUP_TEXT="mininotes-backup.json",PAGES="pages";

    private static SharedPreferences settings(Context c){return c.getSharedPreferences("settings",Context.MODE_PRIVATE);}
    static List<Backups.Place> places(Context c){return Backups.read(settings(c).getString(KEPT,""));}
    static synchronized void keep(Context c,List<Backups.Place> places){settings(c).edit().putString(KEPT,Backups.said(places)).apply();plan(c);}
    static synchronized void update(Context c,Backups.Place one) {
        List<Backups.Place> all=places(c);
        for(Backups.Place now:all)if(now.id.equals(one.id)){now.last=one.last;now.tried=one.tried;now.said=one.said;now.fingerprint=one.fingerprint;}
        settings(c).edit().putString(KEPT,Backups.said(all)).apply();
    }

    // ---- when -------------------------------------------------------------------------------------------------

    /** The job that looks, every half hour or so while there is a place with a time; none while there is none. */
    static void plan(Context c) {
        JobScheduler jobs=c.getSystemService(JobScheduler.class);
        if(jobs==null)return;
        // A device's place too, whatever its time: one that did not reach it waits and is pushed again (decision 330).
        boolean timed=false;for(Backups.Place p:places(c))if(Backups.timed(p))timed=true;
        if(!timed){jobs.cancel(JOB);return;}
        if(jobs.getPendingJob(JOB)!=null)return;
        try{jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,PhoneBackupJob.class)).setPeriodic(30L*60*1000).setPersisted(true).build());}
        catch(RuntimeException refused){/* looked at again when the app opens */}
    }
    /** Mininotes left: a look soon, for those made then, in a job of its own so it is done even as the app goes. */
    static void soon(Context c) {
        boolean any=false;long now=System.currentTimeMillis();for(Backups.Place p:places(c))if(Backups.dueOnClose(p,now)||p.on&&p.toDevice()&&waitingFile(c,p)!=null)any=true;
        if(!any)return;
        JobScheduler jobs=c.getSystemService(JobScheduler.class);
        if(jobs==null)return;
        try{jobs.schedule(new JobInfo.Builder(SOON,new ComponentName(c,PhoneBackupJob.class)).setMinimumLatency(2000).setOverrideDeadline(60_000).build());}
        catch(RuntimeException refused){/* the next time */}
    }

    /** Every place due now made, one after another, here and now. Blocking: the job's thread. */
    static void check(Context any) {
        Context c=any.getApplicationContext();
        long now=System.currentTimeMillis();ZoneId zone=ZoneId.systemDefault();
        for(Backups.Place p:places(c)) {
            if(Backups.due(p,now,zone)||Backups.dueOnClose(p,now)){make(c,p,false);update(c,p);}
            // One waiting for its device, from a try that did not reach it: pushed again each half hour, on from where it stopped.
            else if(Backups.pushAgain(p,now)&&waitingFile(c,p)!=null){p.tried=now;update(c,p);push(c,p);update(c,p);}
        }
    }

    // ---- to another of my devices (decision 330) -------------------------------------------------------------------

    /** Where a backup for a device waits until it is pushed: in the app's own storage, one per place, the newest only. */
    static java.io.File outgoing(Context c,Backups.Place p){return new java.io.File(new java.io.File(c.getFilesDir(),"outgoing-backups"),p.id.replaceAll("[^A-Za-z0-9-]",""));}
    static java.io.File waitingFile(Context c,Backups.Place p) {
        java.io.File[] all=outgoing(c,p).listFiles((d,n)->n.startsWith(Backups.START)&&n.endsWith(Backups.ENDING));
        return all==null||all.length==0?null:all[0];
    }

    /** A backup for a device: sealed into the app's storage, an older one waiting replaced, then pushed. Blocking. */
    private static Backups.Said toDevice(Context c,NoteStore store,Backups.Place p,boolean asked,byte[] key,byte[] lock,long now) throws Exception {
        final String text=store.backup();
        final List<NoteStore.Held> files=p.what==Backups.What.EVERYTHING?store.everyFile():new ArrayList<>();
        List<String> ids=new ArrayList<>();for(NoteStore.Held one:files)ids.add(one.id);
        String fingerprint=Backups.fingerprint(text,ids,p.what);
        boolean waiting=waitingFile(c,p)!=null;
        if(!asked&&!waiting&&!p.fingerprint.isEmpty()&&p.fingerprint.equals(fingerprint)&&(p.said==Backups.Said.MADE||p.said==Backups.Said.SAME)){p.last=now;return p.said=Backups.Said.SAME;}
        if(asked||!waiting||!p.fingerprint.equals(fingerprint)) {
            java.io.File folder=outgoing(c,p);
            if(!folder.isDirectory()&&!folder.mkdirs())return p.said=Backups.Said.FAILED;
            java.io.File target=new java.io.File(folder,Backups.fileName(Node.nameHere(c),now,ZoneId.systemDefault())),part=new java.io.File(folder,target.getName()+".part");
            try(OutputStream out=new java.io.FileOutputStream(part)){write(store,text,files,out,key,lock);}
            catch(Exception failed){part.delete();throw failed;}
            java.io.File[] old=folder.listFiles();if(old!=null)for(java.io.File one:old)if(!one.equals(part))one.delete();
            if(!part.renameTo(target)){part.delete();return p.said=Backups.Said.FAILED;}
            p.fingerprint=fingerprint;
        }
        return push(c,p);
    }

    /** The backup waiting for this place pushed to its device; gone from here once that device has it whole. Blocking. */
    static Backups.Said push(Context c,Backups.Place p) {
        java.io.File file=waitingFile(c,p);
        if(file==null)return p.said;
        NoteStore store;
        try{store=NoteStore.of(c);}catch(IllegalStateException locked){return p.said=Backups.Said.NO_KEY;}
        Backups.Said went=Post.pushBig(c,store,Keys.of(c),p.device,file.getName(),Backups.kindKeeping(p.keep),file.length(),at->{
            java.io.FileInputStream in=new java.io.FileInputStream(file);
            long skipped=0;while(skipped<at){long n=in.skip(at-skipped);if(n<=0)break;skipped+=n;}
            return in;
        });
        p.tried=System.currentTimeMillis();
        if(went==Backups.Said.MADE){file.delete();p.last=p.tried;}
        return p.said=went;
    }

    // ---- making one -------------------------------------------------------------------------------------------

    /** One backup into its folder. Not asked for and nothing changed: none made, and that is said. Then the oldest go. */
    static Backups.Said make(Context c,Backups.Place p,boolean asked) {
        long now=System.currentTimeMillis();p.tried=now;
        ContentResolver resolver=c.getContentResolver();
        Uri made=null;
        try {
            NoteStore store;
            try{store=NoteStore.of(c);}catch(IllegalStateException locked){return p.said=Backups.Said.NO_KEY;}
            if(p.toDevice()) {
                byte[] key,lock;
                if(p.opens==Backups.Opens.NOTEBOOK){key=NoteStore.key();lock=PhoneLock.locked(c)?PhoneLock.kept(c):null;}
                else{String kept=store.secret(Backups.secretOf(p.id));key=kept.isEmpty()?null:Base64.getDecoder().decode(kept);lock=p.lock.isEmpty()?null:Base64.getDecoder().decode(p.lock);}
                if(key==null||lock==null)return p.said=Backups.Said.NO_KEY;
                return toDevice(c,store,p,asked,key,lock,now);
            }
            Uri tree=Uri.parse(p.where);
            Uri folder=folderOf(tree);
            if(p.where.isEmpty()||folder==null||!exists(resolver,folder))return p.said=Backups.Said.NO_FOLDER;
            byte[] key,lock;
            if(p.opens==Backups.Opens.NOTEBOOK){key=NoteStore.key();lock=PhoneLock.locked(c)?PhoneLock.kept(c):null;}
            else{String kept=store.secret(Backups.secretOf(p.id));key=kept.isEmpty()?null:Base64.getDecoder().decode(kept);lock=p.lock.isEmpty()?null:Base64.getDecoder().decode(p.lock);}
            if(key==null||lock==null)return p.said=Backups.Said.NO_KEY;
            final String text=store.backup();
            final List<NoteStore.Held> files=p.what==Backups.What.EVERYTHING?store.everyFile():new ArrayList<>();
            List<String> ids=new ArrayList<>();for(NoteStore.Held one:files)ids.add(one.id);
            String fingerprint=Backups.fingerprint(text,ids,p.what);
            if(!asked&&!p.fingerprint.isEmpty()&&p.fingerprint.equals(fingerprint)&&(p.said==Backups.Said.MADE||p.said==Backups.Said.SAME)){p.last=now;return p.said=Backups.Said.SAME;}
            String device=Node.nameHere(c);
            String name=Backups.fileName(device,now,ZoneId.systemDefault());
            List<String> before=names(resolver,tree,folder);
            if(before.contains(name))return p.said=Backups.Said.SAME;
            made=DocumentsContract.createDocument(resolver,folder,"application/octet-stream",name);
            if(made==null)return p.said=Backups.Said.NO_FOLDER;
            try(OutputStream out=resolver.openOutputStream(made,"wt")) {
                if(out==null)throw Big.broken("no-output");
                write(store,text,files,out,key,lock);
            }
            p.last=now;p.fingerprint=fingerprint;p.said=Backups.Said.MADE;made=null;
            List<String> all=names(resolver,tree,folder);
            for(String old:Backups.beyond(all,device,p.keep))try{Uri gone=child(resolver,tree,folder,old);if(gone!=null)DocumentsContract.deleteDocument(resolver,gone);}catch(Exception kept){/* the next time */}
            return p.said;
        } catch(Exception failed) {
            android.util.Log.w("Mininotes/Backup","a backup not made: "+failed.getClass().getSimpleName());
            String why=String.valueOf(failed.getMessage());
            return p.said=why.contains("ENOSPC")||why.contains("No space")?Backups.Said.NO_ROOM:Backups.Said.FAILED;
        } finally {
            // Cut short, what was begun is not left to pass for a backup.
            if(made!=null)try{DocumentsContract.deleteDocument(resolver,made);}catch(Exception gone){/* not there */}
        }
    }

    /** Sealed whole with the key, the lock first so it opens anywhere; the zip is sealed as it is made, never written plain. */
    static void write(NoteStore store,String text,List<NoteStore.Held> files,OutputStream out,byte[] key,byte[] lock) throws Exception {
        PhoneLock.writeBackupHead(out,lock);
        java.io.PipedInputStream plain=new java.io.PipedInputStream(1<<16);java.io.PipedOutputStream into=new java.io.PipedOutputStream(plain);
        final Exception[] failed={null};
        Thread making=new Thread(()->{try(into){zip(store,text,files,into);}catch(Exception e){failed[0]=e;}},"mininotes-backup");
        making.start();
        try{Sealed.seal(key,plain,out);}finally{making.join();}
        if(failed[0]!=null)throw failed[0];
    }

    /** The backup's zip: the notes as text, every file given plain inside it, and the private file (decision 111). */
    static void zip(NoteStore store,String text,List<NoteStore.Held> files,OutputStream out) throws Exception {
        ZipOutputStream zip=new ZipOutputStream(out);
        zip.putNextEntry(new ZipEntry(BACKUP_TEXT));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        for(NoteStore.Held held:files) {
            File kept=store.fileFor(held.id);
            if(!kept.isFile())continue;
            zip.putNextEntry(new ZipEntry(Attachment.entry(held.id)));
            PhoneLock.copyOut(kept,zip);
            zip.closeEntry();
        }
        Slots pages=PrivateScreen.slots();
        if(pages!=null){zip.putNextEntry(new ZipEntry(PAGES));pages.portable(zip);zip.closeEntry();}
        zip.finish();zip.flush();
    }

    // ---- the folder -------------------------------------------------------------------------------------------

    static Uri folderOf(Uri tree){try{return DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));}catch(RuntimeException not){return null;}}
    private static boolean exists(ContentResolver r,Uri folder) {
        try(Cursor c=r.query(folder,new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){return c!=null&&c.moveToFirst()&&DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(0));}
        catch(RuntimeException refused){return false;}
    }
    private static List<String> names(ContentResolver r,Uri tree,Uri folder) {
        List<String> out=new ArrayList<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(folder));
        try(Cursor c=r.query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){while(c!=null&&c.moveToNext())out.add(c.getString(0));}
        catch(RuntimeException refused){/* none read */}
        return out;
    }
    private static Uri child(ContentResolver r,Uri tree,Uri folder,String name) {
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(folder));
        try(Cursor c=r.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)) {
            while(c!=null&&c.moveToNext())if(name.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));
        }
        return null;
    }
    /** A folder's name as the phone says it: the last part of where it is, "Downloads", "Backups". */
    static String shown(Context c,Uri tree) {
        // On the phone's own storage, the path from its top, "Documents/Mininotes backup", so it can be found again in Files
        // (the owner, 2026-11-07: "the folder I picked should be shown here"); elsewhere (a cloud, a card) the folder's name.
        try {
            String id=DocumentsContract.getTreeDocumentId(tree);int colon=id.indexOf(':');
            if("com.android.externalstorage.documents".equals(tree.getAuthority())&&colon>=0&&colon<id.length()-1)return id.substring(colon+1);
        } catch(RuntimeException unknown){/* below */}
        Uri folder=folderOf(tree);
        if(folder!=null)try(Cursor q=c.getContentResolver().query(folder,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(q!=null&&q.moveToFirst()&&q.getString(0)!=null)return q.getString(0);}catch(RuntimeException unknown){/* below */}
        String id=DocumentsContract.getTreeDocumentId(tree);int colon=id.lastIndexOf(':');
        return colon>=0&&colon<id.length()-1?id.substring(colon+1):id;
    }
}
