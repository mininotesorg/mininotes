// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * The overview: what was opened lately - Recent - as cards side by side, the newest first, flicked through as a phone's
 * open apps are (docs/HOME.md, step 2 and decisions 22 and 86). A note's card is its paper, with its title and its first
 * lines; a collection's, the mini-grid of what is in it, with its name. Tap a card to go to it. Nothing is closed: what
 * was opened falls out of Recent by itself (the owner, 2026-10-04: "we would get rid of open and keep only recent"). Back,
 * or a tap anywhere but a card, puts it away.
 *
 * <p>The pure {@link Overview} is still kept, as the order things were opened in, for going back; the cards are Recent's.
 */
final class OverviewScreen {
    /** Where the list is kept among the settings. */
    private static final String KEPT="overview";
    private final MainActivity a;
    private final HomeScreen home;
    /** What is open, read from the settings the first time it is wanted. */
    private Overview open;
    /** The screen over everything while it is up, and in it the row of cards, what it says with none, and Close all. */
    private FrameLayout layer;
    private LinearLayout cards;
    private View none,along;

    OverviewScreen(MainActivity a,HomeScreen home){this.a=a;this.home=home;}

    private Overview list() {
        if(open==null)open=Overview.read(a.getSharedPreferences("settings",MainActivity.MODE_PRIVATE).getString(KEPT,""));
        return open;
    }

    private void keep() {
        a.getSharedPreferences("settings",MainActivity.MODE_PRIVATE).edit().putString(KEPT,list().said()).apply();
    }

    /** Opened: to the front. Already at the front, nothing is written. */
    void remember(Overview.Kind kind,String id) {
        Overview now=list();
        if(!now.isEmpty()&&now.all().get(0).equals(new Overview.Open(kind,id)))return;
        if(now.open(kind,id))keep();
    }

    /** Put away or deleted: no card opens what is not there. */
    void forget(String id){if(list().forget(id))keep();}

    /** What is open, the newest first: what the Open icon's card lists (decision 78). */
    List<Overview.Open> all(){return list().all();}

    boolean isShowing(){return layer!=null&&layer.getParent()!=null;}

    /** Up, over Home, with the cards read from the notebook as they are now. */
    void open() {
        if(!home.showing()||isShowing())return;
        if(a.showing!=null)a.showing.close();
        layer=new FrameLayout(a);
        layer.setBackgroundColor((a.SHEET&0x00FFFFFF)|0xF0000000);
        layer.setElevation(a.dp(16));
        layer.setContentDescription("Put the overview away");
        // A tap anywhere that is not a card puts it away, as a tap outside every box here does.
        layer.setOnClickListener(v->close());
        LinearLayout column=a.column();
        column.setPadding(0,a.root.getPaddingTop()+a.dp(20),0,a.root.getPaddingBottom()+a.dp(12));
        column.setOnClickListener(v->close());
        HorizontalScrollView row=new HorizontalScrollView(a);
        row.setHorizontalScrollBarEnabled(false);row.setClipToPadding(false);row.setFillViewport(true);
        // In the middle while they fit across, and from the left, scrolling, once they do not.
        cards=new LinearLayout(a);cards.setGravity(Gravity.CENTER);
        cards.setPadding(a.dp(18),0,a.dp(18),0);
        cards.setOnClickListener(v->close());
        row.addView(cards,new FrameLayout.LayoutParams(-2,-1));
        along=row;
        column.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=a.label("Nothing opened lately. The notes and folders you open are here, the newest first.",MainActivity.QUIET,a.MUTED);
        empty.setGravity(Gravity.CENTER);empty.setPadding(a.dp(32),0,a.dp(32),0);
        empty.setVisibility(View.GONE);
        none=empty;
        column.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
        layer.addView(column,new FrameLayout.LayoutParams(-1,-1));
        a.stage.addView(layer,new FrameLayout.LayoutParams(-1,-1));
        load();
    }

    /** Put away: Home, as it was under it. */
    void close() {
        if(layer!=null&&layer.getParent()!=null)((ViewGroup)layer.getParent()).removeView(layer);
        layer=null;cards=null;none=null;along=null;
    }

    /** How many cards at most: a glance at what was lately in hand, not a second list of everything. */
    private static final int CARDS=20;

    /**
     * The cards, read on the worker: Recent as its card lists it, each note as it now reads, and each collection with its
     * name, colour and how much it holds.
     */
    private void load() {
        final long since=System.currentTimeMillis()-a.recentDays()*86_400_000L;
        a.background.submit(()->{
            List<Overview.Open> all=new ArrayList<>();
            for(NoteStore.Branch one:a.store.recent(since)) {
                if(all.size()>=CARDS)break;
                all.add(new Overview.Open(one.kind==NoteStore.Branch.Kind.PAGE?Overview.Kind.NOTE:Overview.Kind.COLLECTION,one.id));
            }
            List<Object[]> shown=new ArrayList<>();List<String> gone=new ArrayList<>();
            // Each card wears its thing's look: the lines are made here, so they are dressed here (NoteStore.dress).
            List<NoteStore.Branch> faces=new ArrayList<>();
            Icons.all();
            for(Overview.Open one:all) {
                NoteStore.Branch.Kind kind=one.kind==Overview.Kind.NOTE?NoteStore.Branch.Kind.PAGE:NoteStore.Branch.Kind.COLLECTION;
                if(!a.store.stillThere(kind,one.id)){gone.add(one.id);continue;}
                if(one.kind==Overview.Kind.NOTE) {
                    NoteStore.Note note=a.store.get(one.id);
                    if(note==null){gone.add(one.id);continue;}
                    NoteStore.Branch face=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,one.id,note.book,"","",0,0,false,note.colour);
                    faces.add(face);
                    shown.add(new Object[]{one,note,face});
                } else {
                    NoteStore.Branch collection=new NoteStore.Branch(NoteStore.Branch.Kind.COLLECTION,one.id,"",a.store.collectionName(one.id),
                        String.valueOf(a.store.contents(one.id).size()),0,0,true,a.store.colourOf(NoteStore.Branch.Kind.COLLECTION,one.id));
                    faces.add(collection);
                    shown.add(new Object[]{one,collection,collection});
                }
            }
            a.store.dress(faces);
            return new Object[]{shown,gone};
        },got->{
            @SuppressWarnings("unchecked") List<String> gone=(List<String>)got[1];
            for(String id:gone)forget(id);
            if(!isShowing())return;
            @SuppressWarnings("unchecked") List<Object[]> shown=(List<Object[]>)got[0];
            cards.removeAllViews();
            for(Object[] one:shown)cards.addView(card((Overview.Open)one[0],one[1],(NoteStore.Branch)one[2]));
            said();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** With no card left: the words that say why, and no button with nothing to do. */
    private void said() {
        if(cards==null)return;
        boolean empty=cards.getChildCount()==0;
        along.setVisibility(empty?View.GONE:View.VISIBLE);
        none.setVisibility(empty?View.VISIBLE:View.GONE);
    }

    /**
     * One open thing, as a card: a note as its paper, with its face before its title; a collection as its face - its
     * picture, its icon, or its mini-grid; tapped, gone to; thrown up, closed.
     *
     * @param face the thing as a line dressed in its look, for its face
     */
    private View card(final Overview.Open one,Object what,NoteStore.Branch face) {
        int wide=Math.round(a.getResources().getDisplayMetrics().widthPixels*0.7f);
        int high=Math.round(a.getResources().getDisplayMetrics().heightPixels*0.56f);
        LinearLayout card=a.column();
        card.setPadding(a.dp(18),a.dp(16),a.dp(18),a.dp(16));
        card.setElevation(a.dp(8));
        GradientDrawable paper=new GradientDrawable();paper.setCornerRadius(a.dp(20));paper.setStroke(Math.max(1,a.dp(1)),a.LINE);
        String name;
        if(what instanceof NoteStore.Note) {
            NoteStore.Note note=(NoteStore.Note)what;
            paper.setColor(Tint.over(note.colour,a.PAPER,a.wash(note.tone,0.12f,0.72f),a.darkPaper()));
            // Its title, or where it has none its first line, which is what it has always been known by; then what follows.
            String body=note.body==null?"":note.body;
            name=note.title==null?"":note.title.trim();
            if(name.isEmpty()){int line=body.indexOf('\n');name=(line<0?body:body.substring(0,line)).trim();body=line<0?"":body.substring(line+1);}
            if(name.isEmpty())name="Untitled";
            body=body.trim();
            TextView title=a.label(name,MainActivity.READING,a.INK);
            title.setTypeface(title.getTypeface(),android.graphics.Typeface.BOLD);
            title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
            // Its face before its title, as the bar over the note has it.
            LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams faceAt=new LinearLayout.LayoutParams(a.dp(28),a.dp(28));faceAt.setMargins(0,0,a.dp(10),0);
            head.addView(IconFace.view(a,face,a.dp(28)),faceAt);
            head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            card.addView(head,new LinearLayout.LayoutParams(-1,-2));
            View rule=new View(a);rule.setBackgroundColor(a.LINE);
            LinearLayout.LayoutParams under=new LinearLayout.LayoutParams(-1,Math.max(1,a.dp(1)));under.setMargins(0,a.dp(8),0,a.dp(8));
            card.addView(rule,under);
            // The first lines, as many as the card has room for; a card is a glance, not the note.
            TextView lines=a.label(body.length()>900?body.substring(0,900):body,MainActivity.READING,a.INK);
            lines.setEllipsize(TextUtils.TruncateAt.END);lines.setMaxLines(12);
            card.addView(lines,new LinearLayout.LayoutParams(-1,0,1));
        } else {
            NoteStore.Branch collection=(NoteStore.Branch)what;
            paper.setColor(a.CARD);
            name=collection.name;
            card.setGravity(Gravity.CENTER);
            int side=Math.min(wide,high)/2;
            card.addView(IconFace.view(a,collection,side),new LinearLayout.LayoutParams(side,side));
            TextView called=a.label(name,MainActivity.READING,a.INK);
            called.setGravity(Gravity.CENTER);called.setMaxLines(2);called.setEllipsize(TextUtils.TruncateAt.END);
            called.setPadding(0,a.dp(12),0,0);
            card.addView(called,new LinearLayout.LayoutParams(-1,-2));
        }
        card.setBackground(paper);
        card.setForeground(a.getDrawable(a.touchFeedback()));
        card.setContentDescription((one.kind==Overview.Kind.NOTE?"The note ":"The folder ")+name+". Tap to go to it.");
        card.setOnClickListener(v->{close();home.goTo(one);});
        LinearLayout.LayoutParams place=new LinearLayout.LayoutParams(wide,high);
        place.setMargins(a.dp(10),0,a.dp(10),0);
        card.setLayoutParams(place);
        return card;
    }
}
