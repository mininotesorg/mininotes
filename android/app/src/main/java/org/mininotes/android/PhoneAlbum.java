// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A folder shown as an album on the phone (the owner, 2026-10-19: "would be nice to have a type of folder specific for
 * photos, that will display photos in a nice album setup and make browsing convenient"; decision 217): its pictures as
 * squares, three or four across, cropped to fill, newest first, over what else the folder holds; a square tapped shows its
 * picture over the whole screen, the one before and the one after a swipe away, Back to close, its name, where it is ("3 of
 * 24") and ⋮, the picture's own menu. Squares and large pictures are made on a thread of their own, never on the worker the
 * note's saves wait on, read through the lock as every file is (PhoneLock.copyOut), and kept while the app runs.
 */
final class PhoneAlbum {
    private PhoneAlbum(){}

    /** One picture of an album: its line, as the folder lists it, for its menu; and its row, for its bytes. */
    static final class Picture {
        final NoteStore.Branch line;final NoteStore.Held held;
        Picture(NoteStore.Branch line,NoteStore.Held held){this.line=line;this.held=held;}
    }

    /** The least a square is wide, in dp, and the room between two: three or four across a phone's card. */
    static final int LEAST=88,GAP=4;
    /** How big a square is made, in pixels: drawn from it at the size the screen gives. */
    static final int MADE=360;
    /** Bigger than this, a picture is shown by its kind: read whole into memory, it would be too much to hold. */
    static final long MOST=40L*1024*1024;

    private static final java.util.concurrent.ExecutorService MAKING=java.util.concurrent.Executors.newSingleThreadExecutor(work->{
        Thread one=new Thread(work,"mininotes-album");one.setDaemon(true);one.setPriority(Thread.MIN_PRIORITY);return one;});
    /** Squares made, by file and size; NONE where the picture could not be read. The latest two hundred. */
    private static final Map<String,Bitmap> SQUARES=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String,Bitmap>(64,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> eldest){return size()>200;}});
    private static final Bitmap NONE=Bitmap.createBitmap(1,1,Bitmap.Config.RGB_565);

    // ---- reading pictures ----------------------------------------------------------------------------------------------

    private static byte[] bytes(NoteStore store,NoteStore.Held file) throws java.io.IOException {
        if(file.bytes>MOST)return null;
        ByteArrayOutputStream out=new ByteArrayOutputStream();PhoneLock.copyOut(store.fileFor(file.id),out);return out.toByteArray();
    }
    /** Decoded with every n-th pixel, and turned as its camera said. */
    private static Bitmap decode(byte[] raw,int step) {
        BitmapFactory.Options how=new BitmapFactory.Options();how.inSampleSize=Math.max(1,step);
        Bitmap read;try{read=BitmapFactory.decodeByteArray(raw,0,raw.length,how);}catch(RuntimeException|OutOfMemoryError unreadable){return null;}
        if(read==null)return null;
        int turn="jpeg".equals(Thumb.kind(raw))?Shrink.orientation(raw):1;
        if(turn<2||turn>8)return read;
        Matrix m=new Matrix();
        switch(turn) {
            case 2: m.setScale(-1,1);break;
            case 3: m.setRotate(180);break;
            case 4: m.setScale(1,-1);break;
            case 5: m.setRotate(90);m.postScale(-1,1);break;
            case 6: m.setRotate(90);break;
            case 7: m.setRotate(-90);m.postScale(-1,1);break;
            default: m.setRotate(-90);break;
        }
        Bitmap turned=Bitmap.createBitmap(read,0,0,read.getWidth(),read.getHeight(),m,true);
        if(turned!=read)read.recycle();
        return turned;
    }
    private static int[] bounds(byte[] raw) {
        BitmapFactory.Options size=new BitmapFactory.Options();size.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(raw,0,raw.length,size);
        return size.outWidth>0&&size.outHeight>0?new int[]{size.outWidth,size.outHeight}:null;
    }
    /** The square in the middle of a picture, {@code side} wide; null where it cannot be read. */
    static Bitmap square(byte[] raw,int side) {
        int[] size=raw==null?null:bounds(raw);if(size==null)return null;
        Bitmap whole=decode(raw,Album.step(size[0],size[1],side));if(whole==null)return null;
        int[] s=Album.square(whole.getWidth(),whole.getHeight());
        Bitmap out=Bitmap.createBitmap(side,side,Bitmap.Config.ARGB_8888);
        new Canvas(out).drawBitmap(whole,new Rect(s[0],s[1],s[0]+s[2],s[1]+s[2]),new Rect(0,0,side,side),new Paint(Paint.FILTER_BITMAP_FLAG));
        whole.recycle();
        return out;
    }
    /** A picture decoded at about the size of the screen, never at its own where that is far bigger. */
    static Bitmap large(byte[] raw,int w,int h) {
        int[] size=raw==null?null:bounds(raw);if(size==null)return null;
        int step=1;while(Math.max(size[0],size[1])/(step*2)>=Math.max(1,Math.max(w,h)))step*=2;
        return decode(raw,step);
    }

    // ---- the grid of squares -------------------------------------------------------------------------------------------

    /**
     * The album's squares, laid in a grid of {@code across} columns, each as wide as {@code width} lets it be: tapped, its
     * picture large; held, its menu.
     */
    static void fill(MainActivity a,GridLayout into,List<Picture> pictures,int width,boolean last) {
        into.removeAllViews();
        int gap=a.dp(GAP),across=Album.across(width,a.dp(LEAST),gap),side=Album.side(width,across,gap);
        into.setColumnCount(across);
        into.setPadding(0,a.dp(4),0,last?a.dp(88):0);
        if(pictures.isEmpty()) {
            TextView none=a.label("No photos yet. Tap + to add photos or take one.",MainActivity.QUIET,a.MUTED);
            none.setGravity(Gravity.CENTER);none.setPadding(a.dp(12),a.dp(24),a.dp(12),a.dp(24));
            GridLayout.LayoutParams at=new GridLayout.LayoutParams(GridLayout.spec(0),GridLayout.spec(0,across));at.width=width;
            into.addView(none,at);return;
        }
        for(int i=0;i<pictures.size();i++) {
            final int at=i;final Picture one=pictures.get(i);
            ImageView square=new ImageView(a);square.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable ground=new GradientDrawable();ground.setColor(a.LINE);ground.setCornerRadius(a.dp(8));
            square.setBackground(ground);square.setClipToOutline(true);
            square.setContentDescription(one.held.name);
            square.setOnClickListener(v->open(a,pictures,at));
            square.setOnLongClickListener(v->{a.homeScreen().fileMenu(v,one.line);return true;});
            GridLayout.LayoutParams place=new GridLayout.LayoutParams(GridLayout.spec(i/across),GridLayout.spec(i%across));
            place.width=side;place.height=side;place.setMargins(i%across==0?0:gap,i<across?0:gap,0,0);
            into.addView(square,place);
            show(a,square,one.held);
        }
    }
    /** Its square, kept or made now on the album's thread. */
    private static void show(MainActivity a,ImageView square,NoteStore.Held file) {
        final String key=file.id+":"+file.bytes;
        Bitmap known=SQUARES.get(key);
        if(known!=null){if(known!=NONE)square.setImageBitmap(known);return;}
        square.setTag(key);
        final NoteStore store=a.store;
        MAKING.execute(()->{
            Bitmap made=null;try{made=square(bytes(store,file),MADE);}catch(Throwable unreadable){/* its colour, then */}
            final Bitmap kept=made==null?NONE:made;SQUARES.put(key,kept);
            square.post(()->{if(key.equals(square.getTag())&&kept!=NONE)square.setImageBitmap(kept);});
        });
    }

    // ---- one picture, large ------------------------------------------------------------------------------------------

    /** A square tapped: its picture over the whole screen, the album either side of it. */
    static void open(MainActivity a,List<Picture> pictures,int at) {
        if(pictures.isEmpty())return;
        if(a.viewer!=null)a.viewer.close();
        a.viewer=new Viewer(a,new ArrayList<>(pictures),at);
    }

    /**
     * The picture large over the screen, fitted to it, on black: a swipe to the left, the next; to the right, the one before;
     * Back or × to close. Its name and where it is at the top, and ⋮, its menu. Large pictures are decoded at about the
     * screen's size, the one in view and those either side of it.
     */
    static final class Viewer {
        final MainActivity a;final List<Picture> pictures;int at;
        final FrameLayout room;final TextView name,where;final Large large;
        private final Map<String,Bitmap> made=java.util.Collections.synchronizedMap(new java.util.HashMap<>());
        private final Set<String> asked=java.util.Collections.synchronizedSet(new java.util.HashSet<>());

        Viewer(MainActivity a,List<Picture> pictures,int at) {
            this.a=a;this.pictures=pictures;this.at=Math.max(0,Math.min(at,pictures.size()-1));
            room=new FrameLayout(a);room.setBackgroundColor(0xFF111312);room.setClickable(true);room.setElevation(a.dp(16));
            large=new Large();
            room.addView(large,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(a.dp(14),a.dp(6),a.dp(4),a.dp(6));bar.setBackgroundColor(0x99111312);
            LinearLayout words=a.column();
            name=a.label("",MainActivity.READING,0xFFECECE6);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            where=a.label("",MainActivity.QUIET,0xFFAAACA6);
            words.addView(name);words.addView(where);
            bar.addView(words,new LinearLayout.LayoutParams(0,-2,1));
            bar.addView(a.tap("⋮",Say.t("Menu"),22,0xFFECECE6,v->{Picture now=shown();if(now!=null)a.homeScreen().fileMenu(v,now.line);}));
            bar.addView(a.tap("×",Say.t("Close"),24,0xFFECECE6,v->close()));
            room.addView(bar,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
            a.stage.addView(room,new FrameLayout.LayoutParams(-1,-1));
            said();
        }
        Picture shown(){return pictures.isEmpty()?null:pictures.get(at);}
        void next(){go(Album.next(at,pictures.size()));}
        void previous(){go(Album.previous(at,pictures.size()));}
        void go(int to){if(to<0||to>=pictures.size()||to==at)return;at=to;said();}
        private void said() {
            Picture now=shown();if(now==null)return;
            name.setText(now.held.name);where.setText(Album.position(at,pictures.size()));
            room.setContentDescription(now.held.name+", "+where.getText());
            large.invalidate();
        }
        /** Back, or ×: the album under it again. Whether it was up. */
        boolean close() {
            if(room.getParent()==null)return false;
            ((ViewGroup)room.getParent()).removeView(room);
            if(a.viewer==this)a.viewer=null;
            return true;
        }
        /**
         * The album read again (a picture put in the bin, archived or moved, from its menu here or elsewhere): those gone are
         * not browsed to; the one after the one shown is shown where it went, and the viewer closes where none is left.
         */
        void still(Set<String> ids) {
            List<Picture> left=new ArrayList<>();int keep=0;
            for(int i=0;i<pictures.size();i++){Picture one=pictures.get(i);if(ids.contains(one.held.id)){if(i<at)keep++;left.add(one);}}
            if(left.size()==pictures.size())return;
            pictures.clear();pictures.addAll(left);
            if(pictures.isEmpty()){close();return;}
            at=Math.max(0,Math.min(keep,pictures.size()-1));said();
        }
        /** The large picture of this one, made where it is not yet, and those either side of it made next. */
        private Bitmap largeOf(int i) {
            if(i<0||i>=pictures.size())return null;
            Picture one=pictures.get(i);
            Bitmap known=made.get(one.held.id);
            if(known==null&&!asked.contains(one.held.id))make(one);
            return known==NONE?null:known;
        }
        private void make(Picture one) {
            final String id=one.held.id;asked.add(id);
            final NoteStore store=a.store;
            final int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;
            MAKING.execute(()->{
                Bitmap got=null;try{got=large(bytes(store,one.held),w,h);}catch(Throwable unreadable){/* said, then */}
                final Bitmap kept=got==null?NONE:got;
                room.post(()->{
                    made.put(id,kept);
                    // Only those near the one in view are held: a long album swiped through does not fill the memory.
                    synchronized(made){made.keySet().removeIf(k->{int i=indexOf(k);boolean far=i<0||Math.abs(i-at)>1;if(far)asked.remove(k);return far;});}
                    large.invalidate();
                });
            });
        }
        private int indexOf(String id){for(int i=0;i<pictures.size();i++)if(pictures.get(i).held.id.equals(id))return i;return -1;}

        /** The picture fitted to the screen, the one beside it coming in as a finger carries it. */
        final class Large extends View {
            private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG),words=new Paint(Paint.ANTI_ALIAS_FLAG);
            private float downX,downY,moved;private boolean sliding;
            Large(){super(a);words.setColor(0xFFAAACA6);words.setTextSize(a.dp(15));words.setTextAlign(Paint.Align.CENTER);}
            @Override protected void onDraw(Canvas c) {
                int w=getWidth(),h=getHeight();
                for(int side=-1;side<=1;side++) {
                    int i=at+side;if(i<0||i>=pictures.size())continue;
                    if(side!=0&&moved==0)continue;
                    float dx=moved+side*w;
                    Bitmap one=largeOf(i);
                    if(one==null) {
                        if(side==0)c.drawText(Say.t(made.get(pictures.get(i).held.id)==NONE?"This picture cannot be shown here. Its menu can open it in another app.":"Opening…"),w/2f+dx,h/2f,words);
                        continue;
                    }
                    double scale=Math.min(w/(double)one.getWidth(),h/(double)one.getHeight());
                    int fw=(int)Math.round(one.getWidth()*scale),fh=(int)Math.round(one.getHeight()*scale);
                    int x=(w-fw)/2,y=(h-fh)/2;
                    c.drawBitmap(one,null,new Rect(x+(int)dx,y,x+(int)dx+fw,y+fh),paint);
                }
                // Those either side made before they are swiped to.
                largeOf(at+1);largeOf(at-1);
            }
            @Override public boolean onTouchEvent(MotionEvent e) {
                int slop=ViewConfiguration.get(a).getScaledTouchSlop();
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: downX=e.getX();downY=e.getY();moved=0;sliding=false;return true;
                    case MotionEvent.ACTION_MOVE:
                        if(!sliding&&Math.abs(e.getX()-downX)>slop&&Math.abs(e.getX()-downX)>Math.abs(e.getY()-downY))sliding=true;
                        if(sliding){moved=e.getX()-downX;
                            // At either end it gives only a little, saying there is nothing more that way.
                            if((moved>0&&at==0)||(moved<0&&at==pictures.size()-1))moved=moved/4;
                            invalidate();}
                        return true;
                    case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                        float far=moved;moved=0;
                        if(sliding&&Math.abs(far)>getWidth()/5f){if(far<0)next();else previous();}
                        else if(!sliding&&e.getActionMasked()==MotionEvent.ACTION_UP)performClick();
                        sliding=false;invalidate();return true;
                    default: return true;
                }
            }
            @Override public boolean performClick(){return super.performClick();}
        }
    }
}
