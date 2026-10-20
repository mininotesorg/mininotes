// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Open to edit, on the phone (the owner, 2026-10-14: "having the option Open to look and Open to edit would be good";
 * docs/FILES.md): a kept file lent to another app as a working copy that app may write, in Mininotes' own folder where no
 * other app can reach it but through the address lent. What that app saves is taken in as the file's new version when
 * Mininotes is in front again, exactly as it was saved, and goes to whoever has the file. Open to look lends the file
 * itself, read only, as Open always did (see Lending).
 *
 * <p>A working copy is plain while it is being edited, inside the app's own folder; it is deleted once it has been left a
 * day with nothing more to take in.
 */
final class Editing {
    private Editing(){}

    /** How long a working copy with nothing new in it is kept after it was last written. */
    private static final long KEPT=24L*60*60*1000;

    private static SharedPreferences sums(Context c){return c.getSharedPreferences("editing",Context.MODE_PRIVATE);}
    private static File room(Context c){File d=new File(c.getFilesDir(),"editing");if(!d.isDirectory())d.mkdirs();return d;}
    /** Where a file's working copy is. Only ids the notebook knows reach here (see Lending). */
    static File copyOf(Context c,String id){return new File(room(c),id);}
    /** The address a working copy is lent at, which the app it is lent to may write. */
    static Uri of(String id){return Uri.parse("content://"+Lending.AUTHORITY+"/edit/"+id);}
    /** Whether an address is a working copy's. */
    static boolean editing(Uri uri){List<String> parts=uri==null?null:uri.getPathSegments();return parts!=null&&parts.size()==2&&"edit".equals(parts.get(0));}

    /** The working copy written from the kept file as it is now, and what it says remembered, so a change shows. */
    static void start(Context c,NoteStore store,NoteStore.Held held) throws IOException {
        File copy=copyOf(c,held.id);
        try(OutputStream out=new FileOutputStream(copy)){PhoneLock.copyOut(store.fileFor(held.id),out);}
        sums(c).edit().putString(held.id,hash(copy)).apply();
    }

    /**
     * Every working copy saved since it was lent, taken in as its file's new version, exactly. A copy whose file is gone, or
     * that was left a day with nothing new, is deleted. Blocking.
     *
     * @return the ids of the files taken in, to be sent
     */
    static List<String> takeIn(Context c,NoteStore store) {
        List<String> taken=new ArrayList<>();
        SharedPreferences sums=sums(c);
        for(Map.Entry<String,?> one:sums.getAll().entrySet()) {
            String id=one.getKey(),was=String.valueOf(one.getValue());
            File copy=copyOf(c,id);
            NoteStore.Held held=store.file(id);
            if(!copy.isFile()||held==null){copy.delete();sums.edit().remove(id).apply();continue;}
            String now;
            try{now=hash(copy);}catch(IOException unreadable){continue;}
            if(now.equals(was)) {
                if(System.currentTimeMillis()-copy.lastModified()>KEPT){copy.delete();sums.edit().remove(id).apply();}
                continue;
            }
            try(InputStream in=new FileInputStream(copy)) {
                store.replaceFile(id,held.kind,in,true);
                sums.edit().putString(id,now).apply();
                taken.add(id);
            } catch(IOException|RuntimeException refused) {
                android.util.Log.i("Mininotes/Editing","a working copy was not taken in: "+refused.getClass().getSimpleName());
            }
        }
        return taken;
    }

    static String hash(File file) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=new FileInputStream(file)){byte[] piece=new byte[65536];int n;while((n=in.read(piece))!=-1)digest.update(piece,0,n);}
            StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format("%02x",b));
            return out.toString();
        } catch(java.security.NoSuchAlgorithmException none){throw new IllegalStateException(none);}
    }
}
