package org.mininotes.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Choose several… on a file where it is held, on Home, in Temp or in a folder (the owner, 2026-11-07: "we should be able to
 * select multiple elements like images in Temp to be able to copy them all at once", on the phone too): which files the box
 * lists, and which are ticked. Android-free, so it is tested without a phone; HomeScreen draws the box.
 */
final class Several {
    private Several(){}

    /** Where the menu was opened, which says what the box lists: Home's loose files, Temp's, a folder's; elsewhere, no box. */
    enum Place{HOME,TEMP,FOLDER,NONE}

    /**
     * The place shown: Home when no card is open over it, else the card's own. Other places (Favourites, Recent, Shared with
     * me, the archive, the bin, Tools) list things kept elsewhere, so they have no Choose several… of their own.
     *
     * @param home whether Home is what is on the screen (no note open)
     * @param card the kind of what the open card shows, or null where no card is open
     */
    static Place place(boolean home,NoteStore.Branch.Kind card) {
        if(!home)return Place.NONE;
        if(card==null)return Place.HOME;
        switch(card) {
            case TEMP: return Place.TEMP;
            case COLLECTION: return Place.FOLDER;
            default: return Place.NONE;
        }
    }

    /**
     * The files among what the place shows, in its order, each once; notes and folders left out. The one the menu was on is
     * always there, first where the place does not show it (a favourite in the dock, say), so it can be ticked.
     */
    static <T> List<T> listed(List<T> shown,Predicate<T> isFile,Function<T,String> id,T held) {
        List<T> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        if(shown!=null)for(T one:shown)if(one!=null&&isFile.test(one)&&seen.add(id.apply(one)))out.add(one);
        if(held!=null&&!seen.contains(id.apply(held)))out.add(0,held);
        return out;
    }

    /** The ticks the box opens with: Select all first, then one per file, the one held ticked (and Select all where it is alone). */
    static <T> boolean[] ticks(List<T> files,Function<T,String> id,String held) {
        boolean[] ticked=new boolean[files.size()+1];
        for(int i=0;i<files.size();i++)ticked[i+1]=id.apply(files.get(i)).equals(held);
        ticked[0]=every(ticked);
        return ticked;
    }

    /** A line ticked or unticked: Select all ticks or unticks every file; any file keeps Select all saying whether all are. */
    static void tick(boolean[] ticked,int which,boolean on) {
        ticked[which]=on;
        if(which==0)for(int i=1;i<ticked.length;i++)ticked[i]=on;
        else ticked[0]=every(ticked);
    }

    /** The files ticked, in the list's order: what Copy puts on the clipboard, first item first. */
    static <T> List<T> chosen(List<T> files,boolean[] ticked) {
        List<T> out=new ArrayList<>();
        for(int i=1;i<ticked.length&&i<=files.size();i++)if(ticked[i])out.add(files.get(i-1));
        return out;
    }

    private static boolean every(boolean[] ticked) {
        if(ticked.length<2)return false;
        for(int i=1;i<ticked.length;i++)if(!ticked[i])return false;
        return true;
    }
}
