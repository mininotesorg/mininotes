// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * How the phone draws a picture smaller (decision 112): Android's own decoder, every n-th pixel read where it is far
 * bigger than it needs to be, then scaled and turned in one step. The rules are {@link Shrink}'s, the same on the PC;
 * the private notes (decision 111) draw with this too, so a picture is made one way wherever it is kept. In memory from
 * end to end.
 */
final class Pictures implements Shrink.Painter<Bitmap> {
    static final Pictures PAINTER=new Pictures();
    private Pictures(){}

    @Override public int[] size(byte[] raw) {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);
        return bounds.outWidth>0&&bounds.outHeight>0?new int[]{bounds.outWidth,bounds.outHeight}:null;
    }

    /** As ExifInterface reads it, which knows a HEIC's and a WebP's as well as a JPEG's. */
    @Override public int turn(byte[] raw) {
        try{return new android.media.ExifInterface(new ByteArrayInputStream(raw)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);}
        catch(Throwable none){return 1;}
    }

    @Override public Bitmap open(byte[] raw,int orientation) {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0)return null;
        BitmapFactory.Options how=new BitmapFactory.Options();how.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/(how.inSampleSize*2)>=Shrink.LONGEST)how.inSampleSize*=2;
        Bitmap picture=BitmapFactory.decodeByteArray(raw,0,raw.length,how);
        if(picture==null)return null;
        int w=picture.getWidth(),h=picture.getHeight();float scale=Math.min(1f,Shrink.LONGEST/(float)Math.max(w,h));
        Matrix turn=new Matrix();turn.postScale(scale,scale);
        switch(orientation) {
            case 2: turn.postScale(-1,1);break;
            case 3: turn.postRotate(180);break;
            case 4: turn.postScale(1,-1);break;
            case 5: turn.postRotate(90);turn.postScale(-1,1);break;
            case 6: turn.postRotate(90);break;
            case 7: turn.postRotate(270);turn.postScale(-1,1);break;
            case 8: turn.postRotate(270);break;
            default: break;
        }
        Bitmap made=Bitmap.createBitmap(picture,0,0,w,h,turn,true);
        if(made!=picture)picture.recycle();
        return made;
    }

    @Override public boolean seeThrough(Bitmap picture) {
        if(!picture.hasAlpha())return false;
        int w=picture.getWidth(),h=picture.getHeight();int[] row=new int[w];
        for(int y=0;y<h;y++){picture.getPixels(row,0,w,0,y,w,1);for(int c:row)if((c>>>24)!=0xFF)return true;}
        return false;
    }

    @Override public byte[] jpeg(Bitmap picture) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.JPEG,Shrink.QUALITY,out);
        return out.toByteArray();
    }

    @Override public byte[] png(Bitmap picture) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.PNG,100,out);
        return out.toByteArray();
    }

    @Override public void close(Bitmap picture){picture.recycle();}
}
