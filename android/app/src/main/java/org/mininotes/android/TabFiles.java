// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Which of a note's tabs each of its files goes under (the owner, 2026-10-30: "when we attached a file it is specified to the
 * open tab and when browsing the attachments they are ordered by the sequence of tab at the top and centered on the tab
 * open"; decision 299).
 *
 * <p>A file keeps the name of the tab it was added under, as the tab's {@code === Name ===} line says it (see {@link Sections}),
 * and empty for a first tab with no name of its own. A name, not a place: tabs are carried about (decision 239) and a file goes
 * with its tab wherever it is put. A file kept before tabs had files, or named under a tab since removed, goes under the first
 * tab, where a note with one tab shows every file anyway: no heading is ever said for a tab that is not there, and nothing is
 * shown apart as "other", which would need a word of its own for what is only a file that lost its tab.
 *
 * <p>Holds no Android types and no Swing: both apps group a note's files the same way, and every rule is unit tested.
 */
final class TabFiles {
    private TabFiles(){}

    /** How a tab is kept with a file: its name, or empty for a first tab with no name of its own. */
    static String key(String name){return name==null?"":name;}

    /** The same for one of a note's tabs, by its place; empty where there is no such tab. */
    static String keyOf(List<Sections.Section> tabs,int index){return index>=0&&index<tabs.size()?key(tabs.get(index).name):"";}

    /**
     * The tab a file kept under {@code stored} goes under now, by its place among the note's tabs: the first tab of that name;
     * the first tab for nothing kept, and for a name no tab has any more.
     */
    static int under(String stored,List<Sections.Section> tabs) {
        if(stored==null||stored.isEmpty())return 0;
        for(int i=0;i<tabs.size();i++)if(stored.equals(tabs.get(i).name))return i;
        return 0;
    }

    /** Whether the files are said under their tabs' names: only where the note has more than one tab. */
    static boolean headed(String body){return Sections.of(body).size()>1;}

    /** The files under one tab, in the order they were given. */
    static final class Group<T> {
        /** The tab's place among the note's tabs, and what the strip calls it. */
        final int tab; final String called; final List<T> files=new ArrayList<>();
        Group(int tab,String called){this.tab=tab;this.called=called;}
    }

    /**
     * A note's files grouped by tab, in the order the tabs stand at the top; within a tab as they were given (newest first, as
     * the notebook gives them). Only tabs with files have a group.
     *
     * @param stored the tab each file was kept under, as {@link #key} keeps it; null for none
     */
    static <T> List<Group<T>> groups(String body,List<T> files,Function<T,String> stored) {
        List<Sections.Section> tabs=Sections.of(body);
        Map<Integer,Group<T>> by=new HashMap<>();
        for(T one:files) {
            int at=under(stored.apply(one),tabs);
            Group<T> group=by.get(at);
            if(group==null){group=new Group<>(at,tabs.get(at).called(at));by.put(at,group);}
            group.files.add(one);
        }
        List<Group<T>> out=new ArrayList<>();
        for(int i=0;i<tabs.size();i++)if(by.containsKey(i))out.add(by.get(i));
        return out;
    }

    /** The files of every group, one after another. */
    static <T> List<T> ordered(List<Group<T>> groups) {
        List<T> out=new ArrayList<>();
        for(Group<T> one:groups)out.addAll(one.files);
        return out;
    }

    /**
     * Which group a list opens on: the open tab's; where it has no files, the next tab's that has, so what is in view is the
     * nearest after it; where none after has, the last. -1 where there are no groups.
     */
    static <T> int opensAt(List<Group<T>> groups,int open) {
        if(groups.isEmpty())return -1;
        for(int i=0;i<groups.size();i++)if(groups.get(i).tab>=open)return i;
        return groups.size()-1;
    }

    /**
     * Where the files of a note go when its tabs change from {@code was} to {@code now} here: from the name they were kept under
     * to the name they are to be kept under, so a renamed tab takes its files with it (the owner's point 5, decision 299).
     *
     * <ul>
     * <li>One tab's name gone and one new one come, the rest the same: a rename. Its files go with it.
     * <li>A first tab with no name of its own given one (renamed, carried, or given Page 1 when a tab is added to a plain note):
     * what was kept as empty goes under that name, so it stays with that tab wherever it is put.
     * <li>A first tab with a name carried away from the front: what was kept as empty, files from before tabs had files, goes
     * under that tab's name, so they stay with the tab they were shown under.
     * </ul>
     * Nothing else moves a file: a tab added or removed leaves every name as it was, and a removed tab's files are shown under
     * the first tab. Empty where nothing is to change.
     */
    static Map<String,String> carried(String was,String now) {
        List<Sections.Section> before=Sections.of(was),after=Sections.of(now);
        Map<String,String> out=new LinkedHashMap<>();
        List<String> gone=new ArrayList<>(),come=new ArrayList<>();
        List<String> left=new ArrayList<>();for(Sections.Section one:after)left.add(key(one.name));
        for(Sections.Section one:before)if(!left.remove(key(one.name)))gone.add(key(one.name));
        List<String> had=new ArrayList<>();for(Sections.Section one:before)had.add(key(one.name));
        for(Sections.Section one:after)if(!had.remove(key(one.name)))come.add(key(one.name));
        if(before.size()==after.size()&&gone.size()==1&&come.size()==1)out.put(gone.get(0),come.get(0));
        String first=key(before.get(0).name),firstNow=key(after.get(0).name);
        // Where the first tab's files go: the name it was renamed to, or its own where it kept it.
        String firstGoes=out.getOrDefault(first,first);
        if(first.isEmpty()) {
            // A plain note given tabs: its words are Page 1, first, and its files with them.
            if(!out.containsKey("")&&after.size()==before.size()+1&&!firstNow.isEmpty()&&!had.contains(firstNow)&&before.size()==1)out.put("",firstNow);
        } else if(!firstGoes.equals(firstNow)&&!out.containsKey("")) {
            for(Sections.Section one:after)if(key(one.name).equals(firstGoes)){out.put("",firstGoes);break;}
        }
        out.entrySet().removeIf(e->e.getKey().equals(e.getValue()));
        return out;
    }
}
