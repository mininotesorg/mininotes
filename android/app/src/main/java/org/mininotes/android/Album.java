// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A folder shown as an album (the owner, 2026-10-19: "would be nice to have a type of folder specific for photos, that will
 * display photos in a nice album setup and make browsing convenient. It should come by default on the Home page for new
 * installs and updates"; decision 217): its pictures in a grid of squares, newest first, then whatever else it holds as the
 * folder draws it, and each picture opened large with the one before and the one after. Which folders are albums is this
 * device's view, kept in its settings (see NoteStore#albums); Photos on Home is one from the start. Plain Java: both apps read it.
 */
final class Album {
    private Album(){}

    /** The folder Home has from the start, its name as stored (a folder's name is its owner's, never translated), and its icon. */
    static final String PHOTOS="Photos", ICON="images";

    /**
     * Photos' id on every device of one person, from the person's id: the owner's devices sync everything with each other, and
     * one made on each with an id of its own would be one Photos a device.
     */
    static String photosId(String person){return UUID.nameUUIDFromBytes(("mininotes photos\0"+person).getBytes(StandardCharsets.UTF_8)).toString();}

    /** Whether a kept file is a picture an album shows: its kind says so, or its name. */
    static boolean picture(String name,String kind) {
        if(kind!=null&&kind.toLowerCase(Locale.ROOT).startsWith("image/"))return true;
        String low=name==null?"":name.toLowerCase(Locale.ROOT);
        for(String end:new String[]{".png",".jpg",".jpeg",".gif",".webp",".bmp",".heic"})if(low.endsWith(end))return true;
        return false;
    }

    /** The pictures among a folder's files, newest first: the order an album is browsed in. */
    static List<NoteStore.Held> pictures(List<NoteStore.Held> files) {
        List<NoteStore.Held> out=new ArrayList<>();
        for(NoteStore.Held one:files)if(picture(one.name,one.kind))out.add(one);
        out.sort((a,b)->Long.compare(b.added,a.added));
        return out;
    }

    /** Of these, those the folder shows (its lines name them): one put away or shown elsewhere is not in the album either. */
    static List<NoteStore.Held> shown(List<NoteStore.Held> pictures,List<NoteStore.Branch> lines) {
        Set<String> drawn=new java.util.HashSet<>();
        for(NoteStore.Branch one:lines)if(one.kind==NoteStore.Branch.Kind.FILE)drawn.add(one.id);
        List<NoteStore.Held> out=new ArrayList<>();
        for(NoteStore.Held one:pictures)if(drawn.contains(one.id))out.add(one);
        return out;
    }

    /** What the folder holds besides its pictures, in its own order, drawn under them as it always draws it: nothing is hidden. */
    static List<NoteStore.Branch> others(List<NoteStore.Branch> lines,List<NoteStore.Held> pictures) {
        Set<String> in=new java.util.HashSet<>();for(NoteStore.Held one:pictures)in.add(one.id);
        List<NoteStore.Branch> out=new ArrayList<>();
        for(NoteStore.Branch one:lines)if(one.kind!=NoteStore.Branch.Kind.FILE||!in.contains(one.id))out.add(one);
        return out;
    }

    /** The picture after this one, or this one at the end: browsing stops at the last, as it starts at the first. */
    static int next(int at,int count){return count<=0?-1:Math.max(0,Math.min(count-1,at+1));}
    /** The picture before this one, or the first. */
    static int previous(int at,int count){return count<=0?-1:Math.max(0,Math.min(count-1,at-1));}
    /** Where a picture is in its album, as the viewer says it: "3 of 24". */
    static String position(int at,int count){return Say.t("{0} of {1}",String.valueOf(at+1),String.valueOf(count));}

    /** How many squares a row holds in this width, each at least {@code least} wide, {@code gap} between them. */
    static int across(int width,int least,int gap){return Math.max(1,(width+gap)/Math.max(1,least+gap));}
    /** How wide each square is once {@code across} of them fill the width. */
    static int side(int width,int across,int gap){return Math.max(1,(width-gap*(Math.max(1,across)-1))/Math.max(1,across));}

    /** The square in the middle of a picture this size, {x, y, side}: what a thumbnail shows, cropped to fill. */
    static int[] square(int w,int h){int s=Math.min(w,h);return new int[]{(w-s)/2,(h-s)/2,s};}
    /** A picture this size fitted inside a room this size, kept whole and never made bigger: {x, y, w, h}. */
    static int[] fit(int w,int h,int roomW,int roomH) {
        if(w<=0||h<=0||roomW<=0||roomH<=0)return new int[]{0,0,0,0};
        double scale=Math.min(1.0,Math.min(roomW/(double)w,roomH/(double)h));
        int fw=Math.max(1,(int)Math.round(w*scale)),fh=Math.max(1,(int)Math.round(h*scale));
        return new int[]{(roomW-fw)/2,(roomH-fh)/2,fw,fh};
    }
    /** Every n-th pixel read of a picture this size, to be at least {@code want} on its shorter side: large ones decoded small. */
    static int step(int w,int h,int want){int step=1;while(Math.min(w,h)/(step*2)>=Math.max(1,want))step*=2;return step;}
}
