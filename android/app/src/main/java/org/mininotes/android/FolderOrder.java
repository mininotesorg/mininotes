// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The order of what is in a folder or in Temp (the owner, 2026-11-09: "the latest added element is positioned last" and "the
 * option in the right click menu to order the elements by date added, name, size and so on at the folder level"; docs/HOME.md,
 * decision 336). Chosen for each folder and kept on the device, as where an icon was put is. Dates go oldest first, so the
 * latest is last whichever date it is; sizes biggest first, which is what is looked for. Holds no Android or Swing types.
 */
final class FolderOrder {
    private FolderOrder(){}

    static final String ADDED="added",NAME="name",SIZE="size",CHANGED="changed",TYPE="type";
    /** The orders offered, in the order the menus list them. */
    static final List<String> CHOICES=List.of(ADDED,NAME,SIZE,CHANGED,TYPE);

    /** What a type starts with: folders, then notes, then files by their ending. */
    static final String FOLDER="0",NOTE="1",FILE="2";

    /** What an order goes by: when it came to where it is, when it last changed, how much it weighs, and what it is. */
    record Facts(long added,long changed,long size,String type){}
    private static final Facts NOTHING=new Facts(0,0,0,"");

    /** An order's words on the menus. */
    static String said(String how) {
        return switch(how) {
            case NAME->"Name";
            case SIZE->"Size";
            case CHANGED->"Date changed";
            case TYPE->"Type";
            default->"Date added";
        };
    }

    /** What a thing is, for the order by type: a file by its ending, so its kinds stand together. */
    static String type(String kind,String name) {
        if(!FILE.equals(kind)||name==null)return kind;
        int dot=name.lastIndexOf('.');
        return FILE+(dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT));
    }

    /**
     * The lines in an order; with none chosen, by when each came. Lines alike stay as they were given, and a name ties by
     * when it came, so the same folder is always drawn the same way.
     */
    static <T> List<T> sorted(List<T> lines,String how,Function<T,String> name,Function<T,Facts> facts) {
        Function<T,Facts> known=one->{Facts got=facts.apply(one);return got==null?NOTHING:got;};
        Collator words=Collator.getInstance();words.setStrength(Collator.SECONDARY);
        Comparator<T> byName=(a,b)->words.compare(String.valueOf(name.apply(a)),String.valueOf(name.apply(b)));
        Comparator<T> byAdded=Comparator.comparingLong(one->known.apply(one).added());
        Comparator<T> order;
        switch(how==null?"":how) {
            case NAME->order=byName.thenComparing(byAdded);
            case SIZE->order=Comparator.<T>comparingLong(one->known.apply(one).size()).reversed().thenComparing(byAdded);
            case CHANGED->order=Comparator.<T>comparingLong(one->known.apply(one).changed()).thenComparing(byAdded);
            case TYPE->order=Comparator.comparing((T one)->known.apply(one).type()).thenComparing(byName).thenComparing(byAdded);
            default->order=byAdded;
        }
        List<T> out=new ArrayList<>(lines);
        out.sort(order);
        return out;
    }
}
