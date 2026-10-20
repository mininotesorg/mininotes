// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

/**
 * One photograph the phone's own camera writes, so it can be copied into a folder or a note and then thrown away
 * (decision 123). The camera cannot be handed a path in this app's folder, so it is handed this address, which opens
 * one file in the cache and nothing else. Not exported. The file is deleted once it has been kept, or if the photo
 * was not taken.
 */
public final class Shot extends ContentProvider {
    /** One per app id, so the demo build installs beside the real app without the two claiming one address. */
    static final String AUTHORITY=BuildConfig.APPLICATION_ID+".shot";

    static Uri of(){return Uri.parse("content://"+AUTHORITY+"/photo.jpg");}

    private static File jpeg(Context context){return new File(context.getCacheDir(),"photo.jpg");}
    private static File called(Context context){return new File(context.getCacheDir(),"photo.name");}

    /** Ready for the camera: an empty picture, and the name it will be kept under. */
    static void prepare(Context context,String name) throws java.io.IOException {
        File picture=jpeg(context);
        if(picture.exists()&&!picture.delete())throw new java.io.IOException("The last photo is still there");
        if(!picture.createNewFile())throw new java.io.IOException("The photo could not be started");
        java.nio.file.Files.writeString(called(context).toPath(),name==null?"Photo.jpg":name,StandardCharsets.UTF_8);
    }

    /** Whether the camera left a picture. */
    static boolean taken(Context context){File picture=jpeg(context);return picture.isFile()&&picture.length()>0;}

    /** The cache copy goes, kept or not. The notebook has its own by then, where it was kept. */
    static void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        jpeg(context).delete();
        //noinspection ResultOfMethodCallIgnored
        called(context).delete();
    }

    @Override public boolean onCreate(){return true;}

    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        File picture=jpeg(getContext());
        int bits=ParcelFileDescriptor.MODE_READ_ONLY;
        if(mode!=null&&mode.contains("w")) {
            try{if(!picture.exists())picture.createNewFile();}catch(java.io.IOException e){throw new FileNotFoundException("The photo could not be started");}
            bits=ParcelFileDescriptor.MODE_READ_WRITE|ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE;
        } else if(!picture.isFile()) throw new FileNotFoundException("No photo");
        return ParcelFileDescriptor.open(picture,bits);
    }

    @Override public String getType(Uri uri){return "image/jpeg";}

    @Override public Cursor query(Uri uri,String[] wanted,String where,String[] args,String order) {
        String name="Photo.jpg";
        try{name=java.nio.file.Files.readString(called(getContext()).toPath(),StandardCharsets.UTF_8).trim();if(name.isEmpty())name="Photo.jpg";}
        catch(java.io.IOException ignored){/* the path's own name */}
        String[] columns=wanted==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:wanted;
        MatrixCursor answer=new MatrixCursor(columns,1);
        Object[] row=new Object[columns.length];
        File picture=jpeg(getContext());
        for(int at=0;at<columns.length;at++) {
            if(OpenableColumns.DISPLAY_NAME.equals(columns[at]))row[at]=name;
            else if(OpenableColumns.SIZE.equals(columns[at]))row[at]=picture.length();
        }
        answer.addRow(row);
        return answer;
    }

    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
}
