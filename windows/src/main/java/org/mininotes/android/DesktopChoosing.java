package org.mininotes.android;

import java.util.*;

/**
 * Several icons chosen at once on Home's grid or in a card, as Explorer chooses files (the owner, 2026-10-30: "we should have
 * a way to select multiple file at the time"; decision 292). The rules alone, with no window, so they are unit tested: Ctrl+click
 * adds an icon or takes it out again; Shift+click chooses the run from the icon last clicked to this one, in the order the grid
 * is read; a plain click chooses nothing, since it opens what it is on as it always did, and only marks where a Shift+click's
 * run starts; Esc or a click on the empty room clears it. Which ids may be chosen at all is the caller's: a place (Temp, the
 * bin...) is never among them.
 */
final class DesktopChoosing {
    /** What a menu on the chosen ones can offer: only what applies to every one of them. */
    enum Offer { SAVE, COPY, SEND, BIN }

    private final LinkedHashSet<String> chosen=new LinkedHashSet<>();
    /** The icon last clicked, plainly or with Ctrl: where a Shift+click's run starts, as in Explorer. */
    private String anchor;

    /** A plain click: nothing chosen, this one where a run starts. */
    void plain(String id){chosen.clear();anchor=id;}
    /** Ctrl+click: in if it was out, out if it was in; and the run starts here from now on. */
    void toggle(String id){if(!chosen.remove(id))chosen.add(id);anchor=id;}
    /**
     * Shift+click: the run from where it starts to this one, in {@code order}, the grid's reading order of what may be chosen,
     * instead of what was chosen; the start stays, so another Shift+click changes the run's far end only. With no start, or
     * one no longer in the order, this one alone, and it becomes the start.
     */
    void run(List<String> order,String id) {
        int to=order.indexOf(id);if(to<0)return;
        int from=anchor==null?-1:order.indexOf(anchor);
        if(from<0){from=to;anchor=id;}
        chosen.clear();
        for(int at=Math.min(from,to);at<=Math.max(from,to);at++)chosen.add(order.get(at));
    }
    /** Ctrl+A: every one in {@code order} chosen, the run starting at the first (the owner, 2026-11-07, "Choisir plusieurs"). */
    void all(List<String> order){chosen.clear();chosen.addAll(order);anchor=order.isEmpty()?null:order.get(0);}
    /**
     * A box dragged over the empty room: those it covers, in {@code order}; with Ctrl held, added to those chosen before
     * ({@code before}), as Explorer does.
     */
    void covered(List<String> order,Collection<String> under,Collection<String> before) {
        chosen.clear();if(before!=null)chosen.addAll(before);
        for(String one:order)if(under.contains(one))chosen.add(one);
    }
    /** Nothing chosen, and no start. */
    void clear(){chosen.clear();anchor=null;}
    /** What is no longer there is no longer chosen: the grid was drawn again without it. */
    void keepOnly(Collection<String> there){chosen.retainAll(new HashSet<>(there));if(anchor!=null&&!there.contains(anchor))anchor=null;}
    boolean has(String id){return chosen.contains(id);}
    int size(){return chosen.size();}
    boolean isEmpty(){return chosen.isEmpty();}
    /** The chosen ones, in the order they were chosen. */
    List<String> chosen(){return new ArrayList<>(chosen);}
    /** The chosen ones in {@code order}'s order, the grid's: what a menu acts on, and what is saved first. */
    List<String> inOrder(List<String> order){List<String> out=new ArrayList<>();for(String one:order)if(chosen.contains(one))out.add(one);return out;}

    /**
     * The order a grid is read in, left to right and then down, from where each icon is drawn ({row, column} by id): the run a
     * Shift+click chooses goes along it, as along the lines of a page.
     */
    static List<String> order(Map<String,int[]> drawn) {
        List<Map.Entry<String,int[]>> all=new ArrayList<>();
        for(Map.Entry<String,int[]> one:drawn.entrySet())if(one.getValue()!=null)all.add(one);
        all.sort((a,b)->a.getValue()[0]!=b.getValue()[0]?Integer.compare(a.getValue()[0],b.getValue()[0]):Integer.compare(a.getValue()[1],b.getValue()[1]));
        List<String> out=new ArrayList<>();for(Map.Entry<String,int[]> one:all)out.add(one.getKey());
        return out;
    }

    /** Whether a thing of this kind can be among those chosen: a note, a file or a folder, never a place. */
    static boolean choosable(NoteStore.Branch.Kind kind) {
        return kind==NoteStore.Branch.Kind.PAGE||kind==NoteStore.Branch.Kind.FILE||DesktopMoving.shelf(kind);
    }

    /**
     * What the menu on the chosen ones offers: for files only, Save on my computer, Copy, Send to a device and Move to bin, as a
     * file's own menu; with a note or a folder among them, only Move to bin, the one thing all of them can do. Nothing for none.
     */
    static Set<Offer> offers(Collection<NoteStore.Branch.Kind> kinds) {
        if(kinds.isEmpty())return EnumSet.noneOf(Offer.class);
        boolean files=true;
        for(NoteStore.Branch.Kind one:kinds){if(!choosable(one))return EnumSet.noneOf(Offer.class);if(one!=NoteStore.Branch.Kind.FILE)files=false;}
        return files?EnumSet.of(Offer.SAVE,Offer.COPY,Offer.SEND,Offer.BIN):EnumSet.of(Offer.BIN);
    }
}
