// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The demo build (BuildConfig.DEMO, the "demo" build type): the real app, for recording the website's animation, with a
 * notebook of made-up things in it and pretend recovery words. Nothing here is anybody's: every note, name, picture and
 * address is invented. In every other build nothing here runs ({@link #shown} hands the real words straight back).
 */
final class Demo {
    private Demo(){}

    /** Twelve words that are plainly not anybody's recovery words, shown in their place in the demo build. */
    static final List<String> WORDS=Collections.unmodifiableList(Arrays.asList(
        "here","is","shown","my","seed","phrase","only","as","an","example","nothing","real"));

    /**
     * The words to show, and to ask back: the real ones, except in the demo build, which shows and asks for {@link #WORDS}.
     * The lock itself is still made with the real key and its real words; only what is on the screen is pretend, so a
     * demo notebook locked this way opens with its password or the phone's unlock, and its words cannot open it.
     */
    static List<String> shown(List<String> real){return BuildConfig.DEMO?WORDS:real;}

    /** The setting that says the made-up notebook was written on this device. */
    static final String SEEDED="demoSeeded";
    /** The pretend friend's address: not a Minima address, and with no keys, so nothing is ever sealed or sent to it. */
    static final String ANA="MxDEMO0ANA0PRETEND0FRIEND0NOT0A0REAL0ADDRESS";

    private static final long HOUR=3_600_000L;
    /** Which colours, as {@link Tint#NAMES} counts them. */
    private static final int RED=1,ORANGE=2,YELLOW=3,GREEN=4,BLUE=6,PURPLE=7,PINK=8;

    /**
     * The made-up notebook, once a device and only in the demo build, through the same calls the app's own screens use.
     *
     * @return whether it was written now
     */
    static boolean seed(NoteStore store,Context context) {
        if(!BuildConfig.DEMO)return false;
        SharedPreferences kept=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        if(kept.getBoolean(SEEDED,false))return false;
        // Said first, so a seed that fails half way is not written twice over on the next opening.
        kept.edit().putBoolean(SEEDED,true).apply();
        long now=System.currentTimeMillis();
        long[] clock={now};

        NoteStore.Shelf recipes=folder(store,"Recipes",ORANGE,"chef-hat");
        NoteStore.Shelf ideas=folder(store,"Ideas",PURPLE,"lightbulb");
        NoteStore.Shelf lisbon=folder(store,"Trip to Lisbon",BLUE,"plane");
        NoteStore.Shelf groceries=folder(store,"Groceries",YELLOW,"shopping-cart");

        note(store,clock,recipes.id,"Soup",String.join("\n",
            "**Tomato soup** for four",
            "- 1 kg ripe tomatoes",
            "- 1 onion, 2 cloves of garlic",
            "- a handful of basil",
            "",
            "Soften the onion, add the rest, simmer 20 minutes, blend."),Tint.NONE);
        note(store,clock,ideas.id,"Garden","Herbs on the balcony: mint, thyme, *lots* of basil.",Tint.NONE);
        note(store,clock,ideas.id,"Birthday","A picnic in the park, a kite, and **lemon cake**.",Tint.NONE);
        note(store,clock,lisbon.id,"Things to see",String.join("\n",
            "- Tram 28 up to the castle",
            "- **Belem** and its tower",
            "- Custard tarts, *still warm*",
            "- Sunset at a viewpoint"),Tint.NONE);
        note(store,clock,lisbon.id,"Flights",String.join("\n",
            "**Out:** Friday 9:40, gate opens 9:00",
            "**Back:** Monday 18:15",
            "One cabin bag each."),Tint.NONE);

        // Saturday market: written here, then a line from the pretend friend, so her colour shows in it.
        NoteStore.Note market=note(store,clock,groceries.id,"Saturday market",String.join("\n",
            "- **Strawberries**",
            "- Bread from the bakery stall",
            "- Eggs, a dozen",
            "- Cheese"),Tint.NONE);
        try {
            store.addAddress(ANA,"Ana",false);
            store.knownAs(ANA,"Ana");
            store.chooseInk(Writers.byAddress(ANA),PINK);
            store.give(Sharing.Scope.PAGE,market.id,ANA,Sharing.Level.WRITE,"Ana");
            NoteStore.Note more=store.get(market.id);
            if(more!=null) {
                more.body=more.body+"\n- Flowers for the table\n- **Honey**, the dark one";
                more.by=Writers.byAddress(ANA);more.revision++;more.updated=++clock[0];
                store.save(more);
            }
        } catch(RuntimeException noFriend){/* the rest of the notebook is still worth having */}

        note(store,clock,Things.HOME,"Books to read",String.join("\n",
            "- *The Little Prince*",
            "- __Something with dragons__",
            "- A cookbook from Lisbon",
            "- *Poems for a rainy day*"),GREEN);
        note(store,clock,Things.HOME,"Call the plumber","Kitchen tap drips. **Call before Friday.**\nAsk about the boiler too.",RED);
        NoteStore.Note parking=note(store,clock,Things.HOME,"Parking spot","Level 2, row C, near the lift.",Tint.NONE);
        try{store.makeTemporary(NoteStore.Branch.Kind.PAGE,parking.id,now+24*HOUR);}catch(RuntimeException noTime){/* still a note */}

        // Pictures drawn here, of nothing and nobody: shapes and colours, so their previews show.
        picture(store,NoteStore.Branch.Kind.COLLECTION,lisbon.id,"Lisbon sunset.png",0xFFFF7A45,0xFF7A3CE0,0xFFFFD34D,0);
        picture(store,NoteStore.Branch.Kind.COLLECTION,lisbon.id,"Tiles.png",0xFF1464E0,0xFF2FE0CE,0xFFFFFFFF,1);
        picture(store,NoteStore.Branch.Kind.COLLECTION,Things.HOME,"Beach.png",0xFF5AA6FF,0xFFFFE6A8,0xFFFFCB3D,2);

        // Last, since saving a note writes its favourite back as the note says it.
        try{store.keepToHand(NoteStore.Branch.Kind.COLLECTION,groceries.id,true);}catch(RuntimeException notKept){/* still there */}
        try{store.keepToHand(NoteStore.Branch.Kind.PAGE,market.id,true);}catch(RuntimeException notKept){/* still there */}
        return true;
    }

    private static NoteStore.Shelf folder(NoteStore store,String name,int colour,String icon) {
        NoteStore.Shelf made=store.addCollectionIn(Things.HOME,name);
        try{store.paint(NoteStore.Branch.Kind.COLLECTION,made.id,colour);}catch(RuntimeException plain){/* a folder still */}
        try{store.setIcon(NoteStore.Branch.Kind.COLLECTION,made.id,icon);}catch(RuntimeException plain){/* its own icon then */}
        return made;
    }

    private static NoteStore.Note note(NoteStore store,long[] clock,String book,String title,String body,int colour) {
        NoteStore.Note note=new NoteStore.Note();
        note.title=title;note.body=body;note.book=book;note.colour=colour;note.by=Writers.ME;
        long at=++clock[0];
        note.updated=at;note.place=-at;note.revision=1;
        store.save(note);
        return note;
    }

    /** A small picture of soft shapes, kept the way a file added from the phone is: bytes first, sealed when locked, row last. */
    private static void picture(NoteStore store,NoteStore.Branch.Kind kind,String where,String name,int top,int bottom,int sun,int style) {
        try {
            byte[] png=draw(top,bottom,sun,style);
            NoteStore.Held held=store.opening(kind,where,name,"image/png",png.length);
            File landing=store.fileFor(held.id);
            try(OutputStream out=new FileOutputStream(landing)) {
                byte[] key=NoteStore.key();
                if(key!=null)Sealed.seal(key,new ByteArrayInputStream(png),out);else out.write(png);
            }
            store.keep(held);
        } catch(Exception noPicture){/* the notebook is still worth having without it */}
    }

    private static byte[] draw(int top,int bottom,int sun,int style) {
        int w=640,h=480;
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0,0,0,h,top,bottom,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);
        if(style==1) {
            // Tiles: a grid of rounded squares with a dot in each.
            int size=80;
            for(int y=0;y<h;y+=size)for(int x=0;x<w;x+=size) {
                paint.setColor(((x+y)/size)%2==0?0x55FFFFFF:0x33000000);
                canvas.drawRoundRect(x+6,y+6,x+size-6,y+size-6,14,14,paint);
                paint.setColor(sun);
                canvas.drawCircle(x+size/2f,y+size/2f,10,paint);
            }
        } else {
            // A sun and two soft hills, or a sun over the sea.
            paint.setShader(new RadialGradient(w*0.65f,h*0.4f,110,sun,sun&0x00FFFFFF,Shader.TileMode.CLAMP));
            canvas.drawCircle(w*0.65f,h*0.4f,110,paint);
            paint.setShader(null);
            paint.setColor(sun);
            canvas.drawCircle(w*0.65f,h*0.4f,58,paint);
            if(style==0) {
                paint.setColor(0xCC3A1E6B);canvas.drawOval(-120,h*0.62f,w*0.7f,h*1.4f,paint);
                paint.setColor(0xDD24124A);canvas.drawOval(w*0.35f,h*0.7f,w+160,h*1.5f,paint);
            } else {
                paint.setColor(0xFF1E88C8);canvas.drawRect(0,h*0.6f,w,h*0.78f,paint);
                paint.setColor(0xFFFFE0A3);canvas.drawRect(0,h*0.78f,w,h,paint);
                paint.setColor(0xAAFFFFFF);
                for(int i=0;i<5;i++)canvas.drawRoundRect(40+i*120,h*0.66f,110+i*120,h*0.68f,6,6,paint);
            }
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        bitmap.recycle();
        return out.toByteArray();
    }
}
