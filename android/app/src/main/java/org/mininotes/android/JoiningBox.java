// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The one path after a code is scanned or pasted, on the phone (the owner, 2026-10-25: "once scanned, we should be invited to
 * define what it is, own device or not, put this in a group, what to share and so on there must be a clear path as unified as
 * possible always"; decision 258). One box, its steps drawn in turn, each with one filled button: what it is, a group (someone
 * else only: a device of mine is in My devices), what to share, Nothing yet first, and done. ‹ Back and the phone's Back go
 * back a step. What is kept and sent is Joining's, the same on the PC (DesktopJoining).
 */
final class JoiningBox {
    private static final int WHAT=0,GROUP=1,SHARE=2,DONE=3;

    private final MainActivity a;private final Pairing.Said code;private final String digits;
    /** Where closing it leads: the Share box the code was scanned from; null for nowhere. */
    private final Runnable then;
    private final Joining.Choice choice;
    private AlertDialog box;private ScrollView scroll;private int step=WHAT;
    private List<Groups.Group> groups=new ArrayList<>();private List<NoteStore.Branch> things=new ArrayList<>();
    private boolean choosing,working;private String ended,also;

    /** @param chosen the thing whose Share box the code was scanned from: chosen for them already; null for none */
    JoiningBox(MainActivity a,Pairing.Said code,String digits,Runnable then,NoteStore.Branch chosen) {
        this.a=a;this.code=code;this.digits=digits==null?"":digits;this.then=then;
        choice=Joining.Choice.from(code);
        if(chosen!=null&&Joining.scopeOf(chosen)!=null){choice.mine=false;choice.things.add(chosen);choosing=true;}
    }

    /** Up at once on its first step; the groups and Home's things are read meanwhile. */
    void open() {
        draw();
        a.background.submit(()->new Object[]{a.store.groups(),Joining.shareable(a.store)},read->{
            @SuppressWarnings("unchecked") List<Groups.Group> g=(List<Groups.Group>)((Object[])read)[0];
            @SuppressWarnings("unchecked") List<NoteStore.Branch> t=(List<NoteStore.Branch>)((Object[])read)[1];
            groups=g;things=Joining.withChosen(t,choice.things);
            if(box!=null&&box.isShowing()&&(step==GROUP||step==SHARE))draw();
        },e->{});
    }

    private String title() {
        if(step==DONE)return code.name;
        return Joining.offers(code)?Say.t("Accept from {0}?",code.name):Say.t("Add {0}?",code.name);
    }
    private void back() {
        if(step==WHAT||step==DONE){box.dismiss();return;}
        step=step==SHARE&&choice.mine?WHAT:step-1;draw();
    }
    private void next(){step=step==WHAT&&choice.mine?SHARE:step+1;draw();}

    private void draw() {
        LinearLayout body=a.inside();
        if(step==GROUP||step==SHARE) {
            TextView back=a.label("‹  "+Say.t("Back"),MainActivity.QUIET,a.MUTED);
            back.setGravity(Gravity.CENTER_VERTICAL);back.setMinHeight(a.dp(48));back.setBackgroundResource(a.touchFeedback());
            back.setContentDescription(Say.t("Back"));back.setOnClickListener(v->back());
            body.addView(back);
        }
        // Whoever it is, at the top of every step, as a person is shown: their round, their name, and a quiet line.
        String under=Joining.offers(code)?Sharing.shown(code.offer)+"  ·  "+Say.t(code.level.words()):null;
        if(step==DONE)under=working?Say.t("Adding {0}…",code.name):"✓  "+Joining.done(code.name,choice.mine);
        LinearLayout who=new LinearLayout(a);who.setGravity(Gravity.CENTER_VERTICAL);who.setMinimumHeight(a.dp(56));
        LinearLayout words=a.column();words.addView(a.line(code.name,MainActivity.READING,a.INK));
        if(under!=null)words.addView(a.label(under,MainActivity.QUIET,a.MUTED));
        who.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        body.addView(a.personLook(who,code.name));
        switch(step) {
            case WHAT: what(body);body.addView(a.primary(Say.t("Next"),this::next));break;
            case GROUP: group(body);body.addView(a.primary(Say.t("Next"),this::next));break;
            case SHARE: share(body);body.addView(a.primary(Say.t(Joining.offers(code)?"Accept":"Add"),this::finish));break;
            default: {
                if(ended!=null)body.addView(a.under(ended));
                if(also!=null)body.addView(a.under(also));
                View done=a.primary(Say.t("Done"),()->box.dismiss());
                done.setEnabled(!working);done.setAlpha(working?0.5f:1f);
                body.addView(done);
            }
        }
        if(box==null) {
            scroll=a.scrolling(body);
            box=a.new Box().setTitle(title()).setView(scroll).create();
            // The phone's Back goes back a step, as ‹ does; from the first step, and the last, it closes the box.
            box.setOnKeyListener((d,key,event)->{
                if(key!=KeyEvent.KEYCODE_BACK||step==WHAT||step==DONE)return false;
                if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())back();
                return true;});
            // Closed, done or not: back to the Share box it came from, and People and devices drawn again where it is open.
            box.setOnDismissListener(d->{a.people().again();if(then!=null)then.run();});
            box.show();
        } else {box.setTitle(title());scroll.removeAllViews();scroll.addView(body);scroll.post(()->scroll.scrollTo(0,0));}
    }

    /** What it is: one of my devices or someone else, as the code said where it said. */
    private void what(LinearLayout body) {
        body.addView(a.part("What is it?"));
        body.addView(a.choice("One of my devices","",choice.mine,()->{choice.mine=true;draw();}));
        body.addView(a.choice("Someone else","",!choice.mine,()->{choice.mine=false;draw();}));
        // What they share arrives on Home, wherever the + was that took it: said before it is accepted.
        if(Joining.offers(code))body.addView(a.under("It will appear on Home when it arrives."));
        // Pasted, not scanned: what a paste cannot prove, and the six digits to check.
        if(!digits.isEmpty())body.addView(a.under(Say.t("Pasted, not scanned. To be sure nobody changed it on the way, open People and devices"
            +" on both phones afterwards: each shows six digits for the other, and they must be the same. Here: {0}.",digits)));
    }

    /** Someone else's group: none, one there is, or one made here. */
    private void group(LinearLayout body) {
        body.addView(a.part(Say.t("Put {0} in a group?",code.name)));
        boolean none=choice.group.isEmpty()&&choice.newGroup.isEmpty();
        body.addView(a.choice("No group","",none,()->{choice.group="";choice.newGroup="";draw();}));
        for(final Groups.Group one:groups)
            body.addView(a.choice(one.name,"",one.id.equals(choice.group),()->{choice.group=one.id;choice.newGroup="";draw();}));
        if(!choice.newGroup.isEmpty())body.addView(a.choice(choice.newGroup,"",true,()->{}));
        // As People and devices' New group asks it; made only at Add, with them in it.
        body.addView(a.toDo("New group","+",()->{
            final android.widget.EditText input=a.field("Name, like Friends or Colleagues",40);
            a.new Box().setTitle("New group").setView(a.naming(input,""))
                .setPositiveButton("Create",(d,w)->{
                    String named=input.getText().toString().trim();
                    if(named.isEmpty())return;
                    choice.newGroup=named;choice.group="";draw();
                }).show();
        }));
    }

    /** What it gets: for a device of mine, everything or nothing yet; for someone else, nothing yet or notes and folders chosen. */
    private void share(LinearLayout body) {
        body.addView(a.part(choice.mine?Say.t("What it gets"):Say.t("What {0} gets",code.name)));
        if(choice.mine) {
            for(final Sharing.Level level:Arrays.asList(null,Sharing.Level.WRITE,Sharing.Level.READ))
                body.addView(a.choice(level==null?Sharing.NOTHING_YET:level.words(),Sharing.everythingDoes(level),choice.everything==level,()->{choice.everything=level;draw();}));
            return;
        }
        body.addView(a.choice(Sharing.NOTHING_YET,"",!choosing,()->{choosing=false;choice.things.clear();draw();}));
        body.addView(a.choice("Choose notes or folders","",choosing,()->{choosing=true;draw();}));
        if(!choosing)return;
        if(things.isEmpty()){body.addView(a.under("Nothing on Home to share yet."));return;}
        for(final NoteStore.Branch one:things) {
            boolean on=false;for(NoteStore.Branch t:choice.things)on|=t.id.equals(one.id);
            String name=one.name==null||one.name.trim().isEmpty()?"Untitled":one.name;
            body.addView(a.switchRow(name,on,now->{
                for(int i=choice.things.size()-1;i>=0;i--)if(choice.things.get(i).id.equals(one.id))choice.things.remove(i);
                if(now)choice.things.add(one);
            }));
        }
        // What they may do with what is chosen: one drop-down for all of it, Can write first, as a Share box offers.
        final List<Sharing.Level> levels=Arrays.asList(Sharing.Level.WRITE,Sharing.Level.READ);
        body.addView(a.dropRow("Rights",Arrays.asList(Say.t(Sharing.Level.WRITE.words()),Say.t(Sharing.Level.READ.words())),null,
            levels.indexOf(choice.level),Say.t(choice.level.words()),at->{choice.level=levels.get(at);draw();}));
    }

    /** Add, or Accept: the last step drawn at once, saying it is adding, and the work done off this thread (see keepPairing). */
    private void finish() {
        if(!choice.mine&&!choosing)choice.things.clear();
        working=true;ended=null;also=null;step=DONE;draw();
        a.keepPairing(code,choice,(said,more)->{
            working=false;ended=said;also=more;
            if(box!=null&&box.isShowing())draw();
        });
    }
}
