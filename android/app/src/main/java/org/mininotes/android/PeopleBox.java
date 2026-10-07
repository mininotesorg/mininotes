// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * People and devices in two views with a page each, and Share with by name (the owner, 2026-10-05: "the whole people and
 * devices page should be reworked to better present all these functionalities", then, the same day: "it should be clearly
 * 2 sections, people and their assignation to a group, and then the group management"; docs/HOME.md, decision 103).
 *
 * <p>One box, and levels inside it drawn in turn, not a box opened on a box: the list in its two views, a group's page,
 * a person's page, this device. Every level under the first has ‹ and where it goes back to at its top, and the phone's
 * Back goes back one level too, never closing them all at once, to the view and the place in the list it was left at
 * (the owner: "we can always come back to the previous level when digging an element"). What a level shows is read again
 * each time it is drawn, so none of them says what was true when the box opened. A thing's Share box and a person's
 * Parlons! box open over the page they were asked from, and the page is drawn again when they close.
 *
 * <p>Share with is a box of the same making on one thing: groups first, then people, with the rights beside each name
 * and changed there (his choice, "Rights beside each name"). It stays open and is drawn again after each change.
 */
final class PeopleBox {
    private final MainActivity a;
    PeopleBox(MainActivity a){this.a=a;}

    private static final int LIST=0,GROUP=1,PERSON=2,SELF=3;
    static final String TITLE="People and devices";

    /** One level: what it is, whose it is (a group's id, a device's address), what the level under it calls it, and where it was left. */
    private static final class Level {
        final int kind;final String id;String name;int scroll;boolean adding;
        Level(int kind,String id,String name){this.kind=kind;this.id=id;this.name=name;}
    }

    /** Everything a level draws from, read off the main thread. */
    private static final class Read {
        List<NoteStore.Contact> contacts=new ArrayList<>();Map<String,String> through=new HashMap<>(),sided=new HashMap<>(),checks=new HashMap<>();
        Map<String,String[]> roads=new HashMap<>();List<String> here=new ArrayList<>();
        List<Groups.Group> groups=new ArrayList<>();Map<String,List<NoteStore.Contact>> in=new HashMap<>();
        /** What a group or a person has, on their page. */
        List<NoteStore.Shared> shared=new ArrayList<>();
        /** Share with: what this phone may give, the groups the thing is given to, and where everybody stands on it. */
        List<Sharing.Level> give=new ArrayList<>();Map<String,Groups.Given> given=new HashMap<>();Map<String,NoteStore.Stands> stands=new HashMap<>();
        NoteStore.Contact contact(String address){for(NoteStore.Contact one:contacts)if(one.address.equals(address))return one;return null;}
        Groups.Group group(String id){for(Groups.Group one:groups)if(one.id.equals(id))return one;return null;}
        String groupName(String id){if(Groups.MINE.equals(id))return Groups.MY_DEVICES;Groups.Group one=group(id);return one==null?"a group":one.name;}
        List<NoteStore.Contact> in(String id){List<NoteStore.Contact> all=in.get(id);return all==null?new ArrayList<NoteStore.Contact>():all;}
        /** The groups somebody is in, My devices among them for a device of mine. */
        Set<String> groupsOf(String address) {
            Set<String> all=new HashSet<>();
            for(Map.Entry<String,List<NoteStore.Contact>> one:in.entrySet())for(NoteStore.Contact who:one.getValue())if(who.address.equals(address))all.add(one.getKey());
            return all;
        }
    }

    private final ArrayList<Level> levels=new ArrayList<>();
    private AlertDialog box;private ScrollView scroll;private Level laid;
    /** The view the list is in, People or Groups, kept while the box is open. */
    private boolean groupsView;
    /** Whether the box is wanted: one closed while it was being read is not put up again. */
    private boolean open;
    /** Which reading the box waits for: an earlier one that comes back late draws nothing. */
    private int asked;

    // ---- levels ---------------------------------------------------------------------------------------------

    /** People and devices, opened at its list; where it is open already, drawn again where it is. */
    void open() {
        if(showing()){draw();return;}
        levels.clear();levels.add(new Level(LIST,"",TITLE));groupsView=false;box=null;scroll=null;laid=null;open=true;
        draw();
    }
    boolean showing(){return box!=null&&box.isShowing();}
    /** Drawn again where it is, if it is open: something it shows changed somewhere else. */
    void again(){if(showing())draw();}
    /** What a box opened over this one says its ‹ goes back to: the level on top; null while this is not open. */
    String onTop(){return showing()&&!levels.isEmpty()?top().name:null;}

    private Level top(){return levels.get(levels.size()-1);}

    /** The level on top read again and drawn: after every change, and whenever something arrives that it shows. */
    private void draw() {
        if(levels.isEmpty())return;
        final int mine=++asked;final Level at=top();
        if(laid==at&&scroll!=null)at.scroll=scroll.getScrollY();
        a.background.submit(()->read(at),got->{if(mine==asked&&open)lay(at,got);},e->a.alert(MainActivity.READ_FAILED));
    }
    /** Down one level, the place in this one kept for coming back. */
    private void go(Level to){if(scroll!=null)top().scroll=scroll.getScrollY();laid=null;levels.add(to);draw();}
    /** Back one level; from the first, the box closes. */
    private void back() {
        if(levels.size()>1){levels.remove(levels.size()-1);laid=null;draw();}
        else if(box!=null)box.dismiss();
    }

    private Read read(Level at) {
        Read r=new Read();NoteStore store=a.store;
        r.contacts=store.addresses();r.groups=store.groups();r.in=store.inGroups();r.through=store.linkedLines();
        // Whether each device is reached directly, and which road the last thing to it went by (decision 96).
        if(at.kind==LIST||at.kind==PERSON)
            for(NoteStore.Contact one:r.contacts)if(one.paired()&&(at.kind==LIST||one.address.equals(at.id)))r.roads.put(one.address,Node.seen(one,Post.heardLately(one)));
        if(at.kind==LIST||at.kind==SELF)r.here=Node.hereLines(a);
        if(at.kind==PERSON) {
            NoteStore.Contact who=r.contact(at.id);
            if(who!=null) {
                // Why only one end counts it as the owner's, and the six digits from this phone's key and theirs: the same
                // on their screen, if nothing came between the two.
                String line=Post.oneSided(a,who);if(line!=null)r.sided.put(who.address,line);
                if(who.signing.length>0)try{r.checks.put(who.address,Envelope.code(a.keys().signing().getPublic(),Keys.publicKey(who.signing)));}catch(Exception unreadable){/* no digits for it */}
                r.shared=store.sharedWith(who.address);
            }
            a.parlonsBook=store.parlonsBook();
        }
        if(at.kind==GROUP)r.shared=store.sharedWithGroup(at.id);
        return r;
    }

    private void lay(final Level at,final Read r) {
        LinearLayout body=a.inside();String title=at.name;
        switch(at.kind) {
            case LIST: list(body,r);break;
            case SELF: body.addView(head(null));self(body,r);break;
            case GROUP: {
                final Groups.Group group=r.group(at.id);
                if(group==null&&!Groups.MINE.equals(at.id)){back();return;}
                title=at.name=r.groupName(at.id);
                body.addView(head(group==null?null:(Runnable)()->groupMenu(moreOf(body),group,true)));
                group(body,r,at,group);break;
            }
            default: {
                final NoteStore.Contact who=r.contact(at.id);
                if(who==null){back();return;}
                title=at.name=who.name;
                body.addView(head(()->a.heldMenu(moreOf(body),null,"Writing colour",(Runnable)()->a.inkBox(who.name,null,who.address),null,"Forget",(Runnable)()->a.forgetAddress(who))));
                person(body,r,who);
            }
        }
        boolean first=box==null;
        if(first) {
            scroll=a.scrolling(body);
            final AlertDialog made=a.new Box().setTitle(title).setView(scroll).create();
            box=made;
            // The phone's Back goes back one level, as ‹ does; from the first level it closes the box, as it always did.
            box.setOnKeyListener((d,code,event)->{
                if(code!=KeyEvent.KEYCODE_BACK||levels.size()<2)return false;
                if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())back();
                return true;});
            // A box says it has gone a moment after it went: one opened again meanwhile is not this one's to close.
            box.setOnDismissListener(d->{if(box==made){open=false;levels.clear();}});
            box.show();
        } else {box.setTitle(title);scroll.removeAllViews();scroll.addView(body);}
        laid=at;
        final int place=at.scroll;final ScrollView pane=scroll;
        pane.post(()->pane.scrollTo(0,place));
    }

    // ---- small parts ----------------------------------------------------------------------------------------

    /**
     * The top of a page reached from another: ‹ and the level it goes back to, and ⋮ at its right for what can be done to
     * what the page is about, where there is anything.
     */
    private View head(final Runnable more) {
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);
        String to=levels.size()>1?levels.get(levels.size()-2).name:TITLE;
        line.addView(backTo(to,this::back),new LinearLayout.LayoutParams(0,-2,1));
        if(more!=null) {
            TextView dots=a.tap("⋮","More",MainActivity.READING,a.MUTED,v->more.run());
            dots.setTag("more");line.addView(dots);
        }
        return line;
    }
    /** ‹ and where it goes back to, as a line to tap: the same on every box that was opened from another. */
    View backTo(String to,final Runnable back) {
        TextView word=a.label("‹  "+to,MainActivity.QUIET,a.MUTED);
        word.setGravity(Gravity.CENTER_VERTICAL);word.setMinHeight(a.dp(48));word.setPadding(0,0,a.dp(12),0);
        word.setSingleLine(true);word.setEllipsize(android.text.TextUtils.TruncateAt.END);
        word.setContentDescription("Back to "+to);word.setBackgroundResource(a.touchFeedback());
        word.setOnClickListener(v->back.run());
        return word;
    }
    /** The ⋮ at the top of the page being drawn, which its menu drops from. */
    private static View moreOf(LinearLayout body){View found=body.findViewWithTag("more");return found==null?body:found;}

    /**
     * A line that opens a page: a name, a quiet line under it, anything else to do with it ({@code before}, or null), and ›
     * at its end. A tap anywhere on it goes in.
     */
    private LinearLayout entry(String name,String under,final Runnable go,View before) {
        LinearLayout entry=new LinearLayout(a);entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setMinimumHeight(a.dp(56));entry.setPadding(0,a.dp(6),0,a.dp(6));
        LinearLayout words=a.column();
        words.addView(a.line(name,MainActivity.READING,a.INK));
        if(under!=null&&!under.isEmpty())words.addView(a.label(under,MainActivity.QUIET,a.MUTED));
        entry.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        if(before!=null)entry.addView(before);
        TextView on=a.label("›",MainActivity.READING,a.MUTED);on.setPadding(a.dp(10),0,0,a.dp(2));entry.addView(on);
        entry.setBackgroundResource(a.touchFeedback());entry.setOnClickListener(v->go.run());
        entry.setContentDescription(under==null||under.isEmpty()?name:name+", "+under);
        return entry;
    }

    /** A line with a name and one thing at its right, which is what is tapped. */
    private LinearLayout line(String name,View right) {
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);
        line.setMinimumHeight(a.dp(52));line.setPadding(0,a.dp(4),0,a.dp(4));
        line.addView(a.line(name,MainActivity.READING,a.INK),new LinearLayout.LayoutParams(0,-2,1));
        if(right!=null)line.addView(right);
        return line;
    }

    /** Small things in a row that goes on to the next line when it runs out of width, as words do. */
    private static final class Flow extends ViewGroup {
        private final int gap;
        Flow(android.content.Context where,int gap){super(where);this.gap=gap;}
        @Override protected void onMeasure(int wide,int high) {
            boolean any=MeasureSpec.getMode(wide)==MeasureSpec.UNSPECIFIED;
            int most=any?Integer.MAX_VALUE:Math.max(0,MeasureSpec.getSize(wide)-getPaddingLeft()-getPaddingRight());
            int x=0,y=0,row=0,widest=0;
            for(int at=0;at<getChildCount();at++) {
                View one=getChildAt(at);if(one.getVisibility()==GONE)continue;
                one.measure(any?MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED):MeasureSpec.makeMeasureSpec(most,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
                if(x>0&&x+one.getMeasuredWidth()>most){x=0;y+=row+gap;row=0;}
                x+=one.getMeasuredWidth()+gap;row=Math.max(row,one.getMeasuredHeight());widest=Math.max(widest,x-gap);
            }
            setMeasuredDimension(resolveSize(widest+getPaddingLeft()+getPaddingRight(),wide),resolveSize(y+row+getPaddingTop()+getPaddingBottom(),high));
        }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
            int most=right-left-getPaddingLeft()-getPaddingRight(),x=0,y=0,row=0;
            for(int at=0;at<getChildCount();at++) {
                View one=getChildAt(at);if(one.getVisibility()==GONE)continue;
                int w=one.getMeasuredWidth(),h=one.getMeasuredHeight();
                if(x>0&&x+w>most){x=0;y+=row+gap;row=0;}
                one.layout(getPaddingLeft()+x,getPaddingTop()+y,getPaddingLeft()+x+w,getPaddingTop()+y+h);
                x+=w+gap;row=Math.max(row,h);
            }
        }
    }

    /**
     * A group somebody is in or is not, small, under their name: in is filled, with a tick; not in is outlined, with a
     * plus. A tap changes it, as a switch would.
     */
    private TextView chip(String name,boolean in,String says,final Runnable flip) {
        TextView chip=a.label(name+(in?"  ✓":"  +"),MainActivity.QUIET,in?a.ACCENT:a.MUTED);
        chip.setGravity(Gravity.CENTER);chip.setMinHeight(a.dp(36));chip.setMinWidth(a.dp(48));chip.setPadding(a.dp(14),a.dp(4),a.dp(14),a.dp(4));
        GradientDrawable edge=new GradientDrawable();edge.setCornerRadius(a.dp(18));
        edge.setColor(in?MainActivity.mix(a.CARD,a.ACCENT,0.16f):0);
        edge.setStroke(Math.max(1,a.dp(1)),in?MainActivity.mix(a.CARD,a.ACCENT,0.4f):a.MUTED);
        chip.setBackground(edge);chip.setContentDescription(says);chip.setOnClickListener(v->flip.run());
        return chip;
    }

    // ---- the list, in its two views ---------------------------------------------------------------------------

    private void list(LinearLayout body,Read r) {
        LinearLayout views=new LinearLayout(a);
        views.addView(view("People",!groupsView,false),new LinearLayout.LayoutParams(0,-2,1));
        views.addView(view("Groups",groupsView,true),new LinearLayout.LayoutParams(0,-2,1));
        body.addView(views);
        if(groupsView)groupsView(body,r);else peopleView(body,r);
    }

    /** One of the two views, at the top of the list: the one in view is in ink, with a line under it. */
    private View view(String name,boolean shown,final boolean groups) {
        LinearLayout one=a.column();
        TextView word=a.label(name,MainActivity.READING,shown?a.INK:a.MUTED);
        word.setGravity(Gravity.CENTER);word.setMinHeight(a.dp(46));
        if(shown)word.setTypeface(null,android.graphics.Typeface.BOLD);
        one.addView(word,new LinearLayout.LayoutParams(-1,-2));
        View under=new View(a);under.setBackgroundColor(shown?a.ACCENT:a.LINE);
        one.addView(under,new LinearLayout.LayoutParams(-1,a.dp(shown?3:1)));
        one.setContentDescription(name+" view"+(shown?", shown":""));
        one.setBackgroundResource(a.touchFeedback());
        one.setOnClickListener(v->{if(groupsView==groups)return;groupsView=groups;top().scroll=0;laid=null;draw();});
        return one;
    }

    /**
     * People, then my devices, then this one: in the order the page is called by, each part built the same way, its lines
     * and then the one thing to do in it, a + and its words, at the same place in both (the owner, 2026-10-05: "My devices
     * and People sections should be structured the same way, with the buttons located at the same place, the text too ...
     * since we say People and devices, people section should be first"; decision 105). Everybody else has the groups each
     * is in under their name, changed there.
     */
    private void peopleView(LinearLayout body,final Read r) {
        final String here=a.thisDevice();
        int mine=0,others=0;
        body.addView(adds("People","Connect with someone",null));
        for(final NoteStore.Contact one:r.contacts) {
            if(one.mine)continue;
            others++;body.addView(device(one,r));
            // The groups they are in, right under the name: a chip each, changed at once (the owner: "people and their
            // assignation to a group"). None where there is no group yet, or nothing to put in one.
            if(!one.paired()||r.groups.isEmpty())continue;
            Flow chips=new Flow(a,a.dp(8));chips.setPadding(0,0,0,a.dp(8));
            Set<String> theirs=r.groupsOf(one.address);
            for(final Groups.Group group:r.groups) {
                final boolean in=theirs.contains(group.id);
                chips.addView(chip(group.name,in,one.name+(in?" is in ":" is not in ")+group.name+". Tap to "+(in?"take them out.":"put them in."),()->put(one,group,!in)));
            }
            body.addView(chips,new LinearLayout.LayoutParams(-1,-2));
        }
        // Each part says what to do when it is empty.
        if(others==0)body.addView(a.under("Nobody else yet. Tap + to connect with someone, or share a note or a folder with them."));
        body.addView(adds(Groups.MY_DEVICES,"Add a device",()->a.shareSheet(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,"Everything")));
        for(NoteStore.Contact one:r.contacts)if(one.mine){mine++;body.addView(device(one,r));}
        if(mine==0)body.addView(a.under("No other device yet. Tap + to connect another device of yours and keep the same notes on both."));
        // This device is the one Profile is about: its line goes there, where its name, its code and how it connects are,
        // over this box, which is still here when Profile is left (the owner: "it should bring to the profile page ... and
        // from the profile page, we should be able to go back to people and devices").
        body.addView(a.part("This device"));
        // With its round, as every line above has one: the same line, about this phone.
        body.addView(a.person(entry(here,"Your name, your code and how this phone connects",a::profile,null),here,null));
    }

    /**
     * A part's name with a + at its right that adds to it: the one sign for adding, at the one place, in both parts (the
     * owner, 2026-10-06: "just put a + on the right side of both My devices and People so that people will understand that
     * this is to add elements"; decision 106). It was a row under each part's lines and two buttons at the foot of the box.
     */
    private View adds(String part,String does,final Runnable go) {
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.BOTTOM);
        line.addView(a.part(part),new LinearLayout.LayoutParams(0,-2,1));
        // By People, the ways to connect, theirs and mine, from the + itself (decision 110); by My devices, the code.
        TextView plus=a.tap("+",does,Math.round(MainActivity.READING*1.25f),a.INK,v->{if(go!=null)go.run();else a.connectWithSomeone(v);});
        plus.setMinWidth(a.dp(48));plus.setGravity(Gravity.CENTER);
        line.addView(plus);
        return line;
    }

    /** One device or person in the list: their round in their colour, their name, how they are reached, and › to their page. */
    private View device(final NoteStore.Contact one,Read r) {
        String[] road=r.roads.get(one.address);
        String under=!one.paired()?"Not paired yet":r.through.containsKey(one.address)?r.through.get(one.address):road!=null&&road.length>0&&road[0]!=null?road[0]:"";
        LinearLayout entry=entry(one.name,under,()->go(new Level(PERSON,one.address,one.name)),null);
        // Held, as a right-click on the PC: whether it is a device of mine, then Forget, last.
        entry.setOnLongClickListener(v->{a.heldMenu(v,one.mine?"My device":null,"My device",(Runnable)()->mine(one,!one.mine),
            null,"Forget",(Runnable)()->a.forgetAddress(one));return true;});
        return a.person(entry,one.name,one.address);
    }

    /** Groups: making them, and who is in each, My devices first as the one every owner has. */
    private void groupsView(LinearLayout body,final Read r) {
        body.addView(a.toDo("New group","+",this::newGroup));
        body.addView(groupEntry(r,Groups.MINE,Groups.MY_DEVICES,null));
        for(Groups.Group group:r.groups)body.addView(groupEntry(r,group.id,group.name,group));
        body.addView(a.under(r.groups.isEmpty()?"No group of your own yet. Make one, like Friends or Colleagues, then put people in it."
            :"A group is the same on all your devices. What is shared with it is shared with everybody in it."));
    }

    private View groupEntry(Read r,final String id,final String name,final Groups.Group group) {
        List<NoteStore.Contact> in=r.in(id);List<String> who=new ArrayList<>();
        for(NoteStore.Contact one:in)who.add(one.name);
        java.util.Collections.sort(who,String.CASE_INSENSITIVE_ORDER);
        final TextView more=group==null?null:a.tap("⋮","More for "+name,MainActivity.READING,a.MUTED,null);
        if(more!=null)more.setOnClickListener(v->groupMenu(more,group,false));
        LinearLayout entry=entry(Groups.counted(name,in.size(),group==null),who.isEmpty()?(group==null?"No other device yet":"Nobody in it yet"):String.join(", ",who),
            ()->go(new Level(GROUP,id,name)),more);
        if(more!=null)entry.setOnLongClickListener(v->{groupMenu(more,group,false);return true;});
        return entry;
    }

    // ---- what is done to a group ------------------------------------------------------------------------------

    /** Rename… and Delete group, on a group's line and on its page; My devices has neither. */
    private void groupMenu(View anchor,final Groups.Group group,final boolean onPage) {
        a.heldMenu(anchor,null,"Rename…",(Runnable)()->renameGroup(group),null,"Delete group",(Runnable)()->deleteGroup(group,onPage));
    }

    /** New group…: a name, and an empty group in the Groups view, on every device of yours. */
    private void newGroup() {
        final EditText input=a.field("Name, like Friends or Colleagues",40);
        a.new Box().setTitle("New group").setView(a.naming(input,""))
            .setPositiveButton("Create",(d,w)->{
                final String name=input.getText().toString().trim();
                if(name.isEmpty())return;
                a.groupWork("Making "+name+"…",()->{a.store.makeGroup(name);return new ArrayList<Groups.Changed>();},"Made "+name,this::draw);
            }).show();
    }

    /** Rename…, on a group: the same group, everywhere, under a new name. */
    private void renameGroup(final Groups.Group group) {
        final EditText input=a.field("Name",40);
        input.setText(group.name);input.setSelection(0,group.name.length());
        a.new Box().setTitle("Rename group").setView(a.naming(input,""))
            .setPositiveButton("Rename",(d,w)->{
                final String name=input.getText().toString().trim();
                if(name.isEmpty()||name.equals(group.name))return;
                a.groupWork("Renaming "+group.name+"…",()->{a.store.renameGroup(group.id,name);return new ArrayList<Groups.Changed>();},"Renamed to "+name,this::draw);
            }).show();
    }

    /** Delete group, asked once: its people stay, and lose only what the group gave them. From its own page, back to the list. */
    private void deleteGroup(final Groups.Group group,final boolean onPage) {
        a.new Box().setTitle("Delete "+group.name+"?")
            .setMessage("The people in it stay. What was shared with the group is taken from them, unless it was shared with them on their own.")
            .setPositiveButton("Delete",(d,w)->a.groupWork("Deleting "+group.name+"…",()->a.store.deleteGroup(group.id),"Deleted "+group.name,onPage?(Runnable)this::back:(Runnable)this::draw))
            .show();
    }

    /** Somebody put in a group or taken out of it, at once, said on the strip from start to end. */
    private void put(final NoteStore.Contact who,final Groups.Group group,final boolean in) {
        a.groupWork((in?"Putting "+who.name+" in ":"Taking "+who.name+" out of ")+group.name+"…",
            ()->a.store.putInGroup(group.id,who.address,in),in?who.name+" is in "+group.name:who.name+" is out of "+group.name,this::draw);
    }

    // ---- a group's page ---------------------------------------------------------------------------------------

    /** A group's page: who is in it, added and removed here, and what is shared with it, with the group's rights on each. */
    private void group(LinearLayout body,final Read r,final Level at,final Groups.Group group) {
        final boolean mine=group==null;final String name=r.groupName(at.id);
        List<NoteStore.Contact> in=r.in(at.id);
        body.addView(a.part(mine?"Devices":"Members"));
        for(final NoteStore.Contact one:in) {
            // Removed from the group, asked first, with what they lose said by name.
            View remove=mine?null:a.tap("Remove","Remove "+one.name+" from "+name,MainActivity.QUIET,a.INK,v->{
                List<String> loses=new ArrayList<>();for(NoteStore.Shared has:r.shared)loses.add(has.said());
                a.new Box().setTitle("Take "+one.name+" out of "+name+"?")
                    .setMessage((loses.isEmpty()?"Nothing is shared with "+name+" yet, so they lose nothing.":"They lose what is shared with "+name+": "+String.join(", ",loses)+".")
                        +" What was shared with them on their own stays.")
                    .setPositiveButton("Remove",(d,w)->put(one,group,false)).setNegativeButton("Cancel",null).show();});
            body.addView(a.person(entry(one.name,"",()->go(new Level(PERSON,one.address,one.name)),remove),one.name,one.address));
        }
        if(in.isEmpty())body.addView(a.under(mine?"No other device yet. Connect my other device to keep the same notes on both.":"Nobody in this group yet. Add people to put somebody in it."));
        if(mine)body.addView(a.tapRow("Connect my other device…",()->a.shareSheet(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,"Everything")));
        else {
            // Whoever is not in it yet, dropped down under the members: a tap adds, and the list stays for the next one.
            TextView turn=a.label(at.adding?"▴":"▾",MainActivity.QUIET,a.MUTED);turn.setPadding(a.dp(12),0,0,0);
            LinearLayout add=line("Add people",turn);add.setBackgroundResource(a.touchFeedback());
            add.setContentDescription(at.adding?"Add people, shown":"Add people");
            add.setOnClickListener(v->{at.adding=!at.adding;draw();});
            body.addView(add);
            if(at.adding) {
                int out=0;
                for(final NoteStore.Contact one:r.contacts) {
                    if(one.mine||!one.paired()||r.groupsOf(one.address).contains(at.id))continue;
                    out++;
                    LinearLayout line=line(one.name,a.tap("Add","Add "+one.name+" to "+name,MainActivity.QUIET,a.INK,v->put(one,group,true)));
                    line.setPadding(a.dp(12),a.dp(4),0,a.dp(4));
                    body.addView(a.person(line,one.name,one.address));
                }
                if(out==0)body.addView(a.under(in.isEmpty()?"Nobody to add yet. Share a note or a folder with somebody first, or scan their code.":"Everybody is in it already."));
            }
        }
        body.addView(a.part("Shared with this group"));
        if(mine)body.addView(a.under("Your devices keep the same notes already. This is what was also shared with them as a group."));
        for(final NoteStore.Shared one:r.shared) {
            final java.util.function.Consumer<Sharing.Level> share=to->{
                final boolean off=to==Sharing.Level.GONE;
                a.groupWork((off?"Removing ":"Sharing with ")+name+"…",()->a.store.shareWithGroup(one.scope,one.target,at.id,to),off?"Removed "+name:"Shared with "+name,this::draw);};
            // Remove asks first: everybody in the group who has it only through it stops having it.
            body.addView(thing(one,null,one.mayChange,one.level,share,()->a.new Box().setTitle("Remove "+name+" from "+one.name+"?")
                .setMessage("They stop getting changes. Their copy stays on their device, and they are told. Anybody given it directly keeps it.")
                .setPositiveButton("Remove",(d,w)->share.accept(Sharing.Level.GONE)).setNegativeButton("Cancel",null).show()));
        }
        if(r.shared.isEmpty())body.addView(a.under(mine?"Nothing is shared with them as a group yet.":"Nothing is shared with this group yet. Share a note or a folder and pick the group."));
    }

    /**
     * A thing on a group's or a person's page: its name, which opens its Share box, what it came through quietly under it,
     * and the rights on it at the right, dropping down to the ones this phone may give and Remove where it may change them.
     */
    private View thing(final NoteStore.Shared one,String under,boolean may,final Sharing.Level now,
                       final java.util.function.Consumer<Sharing.Level> picked,final Runnable remove) {
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);line.setMinimumHeight(a.dp(52));
        LinearLayout words=a.column();words.setPadding(0,a.dp(6),0,a.dp(6));
        words.addView(a.line(one.said(),MainActivity.READING,a.INK));
        if(under!=null)words.addView(a.label(under,MainActivity.QUIET,a.MUTED));
        words.setBackgroundResource(a.touchFeedback());
        words.setContentDescription("Who has "+one.said());
        words.setOnClickListener(v->a.sharedWith(one.scope,one.target,one.name));
        line.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        final String role=one.owner?Sharing.OWNER:now.words(),does=one.owner?Sharing.OWNER_DOES:now.does();
        final TextView set=a.label(may&&!one.owner?role+"  ▾":role,MainActivity.QUIET,a.MUTED);
        set.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);set.setMinHeight(a.dp(48));set.setPadding(a.dp(12),0,0,0);
        set.setBackgroundResource(a.touchFeedback());
        if(may&&!one.owner) {
            // A role this phone may not give is still where the list starts from: shown, ticked, above the ones that may be chosen.
            final List<Sharing.Level> shown=new ArrayList<>(one.give);
            if(!shown.contains(now)){shown.add(now);java.util.Collections.sort(shown,(x,y)->y.ordinal()-x.ordinal());}
            set.setContentDescription(one.said()+", "+role+". "+does+" Tap to change.");
            set.setOnClickListener(v->a.rolesDown(set,shown,now,picked,null,"Remove",remove));
        } else {
            set.setContentDescription(one.said()+", "+role+". "+does);
            set.setOnClickListener(v->a.roleSays(set,role,does));
        }
        line.addView(set);
        return line;
    }

    // ---- a person's page --------------------------------------------------------------------------------------

    /** A person's page: how they are reached, their Parlons! address, their groups, what is shared with them, and what can be done to them. */
    private void person(LinearLayout body,final Read r,final NoteStore.Contact who) {
        LinearLayout card=a.framed(body);
        LinearLayout entry=new LinearLayout(a);entry.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words=a.column();
        TextView name=a.line(who.name,MainActivity.READING,a.INK);name.setTypeface(null,android.graphics.Typeface.BOLD);words.addView(name);
        if(!who.paired())words.addView(a.label("Not paired yet",MainActivity.QUIET,a.MUTED));
        // Never scanned here: linked through something both have, and it says what (see Linking).
        else if(r.through.containsKey(who.address))words.addView(a.label(r.through.get(who.address),MainActivity.QUIET,a.MUTED));
        entry.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        // Their round, in their colour, before the name: a tap gives them another here (decision 69).
        card.addView(a.person(entry,who.name,who.address));
        // How it is reached, one line across the block (decision 97); why only one end counts it as the owner's; the six
        // digits to check with them.
        String[] road=r.roads.get(who.address);
        if(road!=null)card.addView(a.spaced(a.label(Routes.oneLine(road),MainActivity.QUIET,a.MUTED)));
        if(r.sided.containsKey(who.address))card.addView(a.spaced(a.label(r.sided.get(who.address),MainActivity.QUIET,a.MUTED)));
        if(r.checks.containsKey(who.address))card.addView(a.spaced(a.label("Check with them: "+r.checks.get(who.address),MainActivity.QUIET,a.MUTED)));
        // Their Parlons! address, shortened, or Not set; › opens the box that sets it, and with one set they are contacted from here (decision 101).
        if(!who.mine) {
            body.addView(a.row("Parlons!",Parlons.shortened(who.parlons),()->a.parlonsBox(who,this::draw,who.name)));
            if(!who.parlons.isEmpty())body.addView(a.tapRow(Parlons.CONTACT,()->a.openParlons(who.name,who.parlons)));
        }
        // The groups they are in, a switch each (decision 100): on puts them in, and whatever the group has is theirs.
        if(!who.mine&&who.paired()&&!r.groups.isEmpty()) {
            body.addView(a.part("Groups"));
            Set<String> theirs=r.groupsOf(who.address);
            for(final Groups.Group group:r.groups)body.addView(a.switchRow(group.name,theirs.contains(group.id),now->put(who,group,now)));
        }
        // What they have, thing by thing, with the rights on each changed here or taken away; what came through a group says
        // which, and changed here it becomes their own.
        body.addView(a.part("Shared with "+who.name));
        for(final NoteStore.Shared one:r.shared)
            body.addView(thing(one,one.group.isEmpty()?null:"through "+r.groupName(one.group),one.mayChange,one.level,
                to->rights(one.scope,one.target,who,one.rule,to,this::draw),()->rights(one.scope,one.target,who,one.rule,Sharing.Level.GONE,this::draw)));
        if(r.shared.isEmpty())body.addView(a.under("Nothing shared with "+who.name+" yet."));
        // What can be done to the device itself, last: whether it is mine (on or off, so a switch), and forgetting it.
        body.addView(a.part("Device"));
        body.addView(a.switchRow("My device",who.mine,on->mine(who,on)));
        body.addView(a.row("Forget","",()->a.forgetAddress(who)));
    }

    private void mine(final NoteStore.Contact who,final boolean yes) {
        final int job=a.busy(yes?"Counting "+who.name+" as my device…":"No longer counting "+who.name+" as my device…");
        a.background.submit(()->{a.store.setMine(who.address,yes);return null;},
            done->{a.busyDone(job,yes?who.name+" is one of my devices":who.name+" is not one of my devices");draw();a.refresh();},
            e->{a.busyDone(job,null);a.alert("Could not change that. Nothing was changed.");});
    }

    /** This device: what it is called, its door, how notes travel. */
    private void self(LinearLayout body,Read r) {
        LinearLayout self=a.framed(body);
        TextView name=a.line(a.thisDevice(),MainActivity.READING,a.INK);name.setTypeface(null,android.graphics.Typeface.BOLD);self.addView(name);
        for(String line:r.here)self.addView(a.spaced(a.label(line,MainActivity.QUIET,a.MUTED)));
    }

    /**
     * Rights of their own for one person on one thing, or taken off it (GONE): saved, what showed it drawn again, then sent,
     * said on the strip from start to end. Somebody not on it yet is given it; anybody else has their rule decided again.
     * Nothing goes on in silence.
     */
    private void rights(final Sharing.Scope scope,final String target,final NoteStore.Contact who,final Sharing.Rule rule,final Sharing.Level to,final Runnable drawn) {
        final long now=System.currentTimeMillis();
        final boolean first=rule==null||rule.level==Sharing.Level.GONE,off=to==Sharing.Level.GONE;
        final int job=a.busy(first?"Sharing with "+who.name+"…":off?"Removing "+who.name+"…":"Changing what "+who.name+" may do…");
        a.background.submit(()->{
            if(first)a.store.give(scope,target,who.address,to,who.name);else a.store.decide(rule,to,now);
            return null;
        },done->{
            drawn.run();a.refresh();
            // Given, or changed: what the new rule owes goes now, said as any other sharing is, on the open note's own line
            // where it is the open note.
            if(!off){a.sendAfterSharing(scope,target,job,who.name);return;}
            // Taken off: they are told by the same road as leaving, so their copy becomes their own; then everybody else.
            a.busySay(job,"Telling "+who.name+"…");
            a.network.submit(()->{
                boolean told=Post.removed(a,a.store,a.keys(),scope,target,who.address,now);
                a.busySay(job,"Telling the others…");
                Post.changed(a,a.store,a.keys(),MainActivity.kindOf(scope),target);
                return told;
            },told->{
                a.saidState();a.refresh();a.refreshOwed();
                a.busyDone(job,told?"Removed "+who.name:"Removed. "+who.name+" is told when their phone is next open.");
            },e->{a.busyDone(job,null);a.alert("Removed here. "+who.name+" could not be told yet; this phone tells them again by itself.");});
        },e->{a.busyDone(job,null);a.alert(e instanceof IllegalStateException?e.getMessage()+" Nothing was changed.":"Could not save that. Nothing was changed.");});
    }

    // ---- Share with -------------------------------------------------------------------------------------------

    private AlertDialog withBox;private ScrollView withScroll;
    private Sharing.Scope withScope;private String withTarget="",withName="";
    /** Whether it was opened from the thing's Share box, which its ‹ goes back to; and whether it closed to go on somewhere, not back. */
    private boolean withBack,withOpen;
    /** The box that is up now closed to go on somewhere: a box says it has gone a moment later, and answers for itself alone. */
    private boolean[] withLeft={false};
    private int withAsked;
    /** The groups folded open in it. */
    private final Set<String> unfolded=new HashSet<>();

    /**
     * Share with, on one thing: groups first, then people, the rights beside each name (the owner, 2026-10-05: "we should
     * see first the people and devices and then for each of them the rights we give them; for the group the rights could
     * be defined at the group level or at the individual group's members level"). A group's name folds open to who is in
     * it, each with As the group or rights of their own.
     *
     * @param fromItsBox opened from the thing's Share box: that box closes for this one, and going back opens it again, as
     *                   things stand then; false from the note itself, where closing this is all there is
     */
    void with(Sharing.Scope scope,String target,String name,boolean fromItsBox) {
        if(withBox!=null&&withBox.isShowing()){withLeft[0]=true;withBox.dismiss();}
        withScope=scope;withTarget=target;withName=name;withBack=fromItsBox;withLeft=new boolean[]{false};withOpen=true;withBox=null;withScroll=null;unfolded.clear();
        drawWith();
    }
    boolean withShowing(){return withBox!=null&&withBox.isShowing();}
    /** Drawn again where it is, if it is open. */
    void withAgain(){if(withShowing())drawWith();}

    private void drawWith() {
        final int mine=++withAsked;final Sharing.Scope scope=withScope;final String target=withTarget;
        final int place=withScroll==null?0:withScroll.getScrollY();
        a.background.submit(()->{
            Read r=new Read();NoteStore store=a.store;
            r.contacts=store.addresses();r.groups=store.groups();r.in=store.inGroups();r.through=store.linkedLines();
            r.give=store.mayGive(scope,target);r.stands=store.standsOn(scope,target);
            for(Groups.Given one:store.groupsOn(scope,target))r.given.put(one.group,one);
            return r;
        },r->{if(mine==withAsked&&withOpen)layWith(r,place);},e->a.alert(MainActivity.READ_FAILED));
    }

    private void layWith(final Read r,final int place) {
        final Sharing.Scope scope=withScope;final String target=withTarget,name=withName;
        if(r.give.isEmpty()&&withBox==null){withOpen=false;a.alert("Only its owner or an admin can share this.");return;}
        LinearLayout body=a.inside();
        if(withBack)body.addView(backTo(name,()->{if(withBox!=null)withBox.dismiss();}));
        body.addView(a.part("Groups"));
        withGroup(body,r,Groups.MINE,Groups.MY_DEVICES,true);
        for(Groups.Group group:r.groups)withGroup(body,r,group.id,group.name,false);
        body.addView(a.part("People"));
        int alone=0,others=0;
        for(NoteStore.Contact one:r.contacts) {
            if(one.mine)continue;
            others++;
            // In a group: listed under it, where it folds open, and not again here.
            if(!r.groupsOf(one.address).isEmpty())continue;
            alone++;body.addView(a.person(line(one.name,standing(r,one,null)),one.name,one.address));
        }
        if(alone==0)body.addView(a.under(r.contacts.isEmpty()?"No devices paired yet. Scan another device's code, or show them yours."
            :others>0?"Everybody else is in a group.":"Nobody else yet. Scan their code, or show them yours."));
        body.addView(a.row("Show them my code","",()->{withLeft[0]=true;if(withBox!=null)withBox.dismiss();a.shareSheet(scope,target,name);}));
        body.addView(a.row("Scan their code","",()->{withLeft[0]=true;if(withBox!=null)withBox.dismiss();a.typeAddress(scope,target,name);}));
        if(withBox==null) {
            withScroll=a.scrolling(body);
            final AlertDialog made=a.new Box().setTitle("Share “"+name+"” with").setView(withScroll).create();
            final boolean[] left=withLeft;final boolean back=withBack;
            withBox=made;
            // Back, or a tap beside it, goes back to the thing's Share box where it came from there, drawn as things stand now.
            made.setOnDismissListener(d->{if(withBox==made)withOpen=false;if(back&&!left[0])a.sharedWith(scope,target,name);});
            a.closeShareBox();
            withBox.show();
        } else {
            withScroll.removeAllViews();withScroll.addView(body);
            final ScrollView pane=withScroll;pane.post(()->pane.scrollTo(0,place));
        }
    }

    /** One group in Share with: its name, folding open to its people, and the group's rights on the thing, or Add. */
    private void withGroup(LinearLayout body,final Read r,final String id,final String name,boolean devices) {
        List<NoteStore.Contact> in=r.in(id);final Groups.Given given=r.given.get(id);final boolean open=unfolded.contains(id);
        final java.util.function.Consumer<Sharing.Level> share=to->{
            final boolean off=to==Sharing.Level.GONE;
            a.groupWork((off?"Removing ":"Sharing with ")+name+"…",()->a.store.shareWithGroup(withScope,withTarget,id,to),off?"Removed "+name:"Shared with "+name+" · "+to.words(),this::drawWith);};
        TextView right=null;
        if(given!=null||!r.give.isEmpty()) {
            final TextView set=a.label(given==null?Groups.ADD+"  ▾":r.give.isEmpty()?given.level.words():given.level.words()+"  ▾",MainActivity.QUIET,a.MUTED);
            set.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);set.setMinHeight(a.dp(48));set.setPadding(a.dp(12),0,0,0);set.setBackgroundResource(a.touchFeedback());
            if(given==null) {
                set.setContentDescription("Share with "+name);
                set.setOnClickListener(v->a.rolesDown(set,r.give,null,share));
            } else if(r.give.isEmpty()) {
                set.setContentDescription(name+", "+given.level.words()+". "+given.level.does());
                set.setOnClickListener(v->a.roleSays(set,given.level.words(),given.level.does()));
            } else {
                final List<Sharing.Level> shown=new ArrayList<>(r.give);
                if(!shown.contains(given.level)){shown.add(given.level);java.util.Collections.sort(shown,(x,y)->y.ordinal()-x.ordinal());}
                set.setContentDescription(name+", "+given.level.words()+". "+given.level.does()+" Tap to change.");
                // Remove asks first, as in Who has access: everybody in the group who has it only through it stops having it.
                set.setOnClickListener(v->a.rolesDown(set,shown,given.level,share,null,"Remove",(Runnable)()->a.new Box().setTitle("Remove "+name+"?")
                    .setMessage("They stop getting changes. Their copy stays on their device, and they are told. Anybody given it directly keeps it.")
                    .setPositiveButton("Remove",(d,w)->share.accept(Sharing.Level.GONE)).setNegativeButton("Cancel",null).show()));
            }
            right=set;
        }
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);line.setMinimumHeight(a.dp(52));
        TextView fold=a.line((open?"▾  ":"▸  ")+Groups.counted(name,in.size(),devices),MainActivity.READING,a.INK);
        fold.setGravity(Gravity.CENTER_VERTICAL);fold.setMinHeight(a.dp(48));fold.setBackgroundResource(a.touchFeedback());
        fold.setContentDescription((open?"Hide who is in ":"Show who is in ")+name);
        fold.setOnClickListener(v->{if(!unfolded.remove(id))unfolded.add(id);drawWith();});
        line.addView(fold,new LinearLayout.LayoutParams(0,-2,1));
        if(right!=null)line.addView(right);
        body.addView(line);
        if(!open)return;
        for(NoteStore.Contact one:in) {
            LinearLayout member=line(one.name,standing(r,one,id));member.setPadding(a.dp(20),a.dp(4),0,a.dp(4));
            body.addView(a.person(member,one.name,one.address));
        }
        if(in.isEmpty()){TextView none=a.under(devices?"No other device yet.":"Nobody in it yet.");none.setPadding(a.dp(20),a.dp(2),0,a.dp(6));body.addView(none);}
    }

    /**
     * Where one person stands on the thing, beside their name: the rights they have, As the group under a group that gives
     * it to them, Removed where they were taken off by hand, or Add. Where this phone may change it, it drops down to the
     * rights it may give, As the group where a group of theirs has the thing, and Remove; where it may not, the words alone.
     *
     * @param under the group whose people this line is among; null under People
     */
    private View standing(final Read r,final NoteStore.Contact who,String under) {
        final NoteStore.Stands stands=r.stands.get(who.address);
        if(stands==null)return quietly("Not paired yet",null);
        if(stands.how==NoteStore.Stands.OWNER)return quietly(Sharing.OWNER,Sharing.OWNER_DOES);
        if(stands.how==NoteStore.Stands.ABOVE)return quietly(stands.level.words(),stands.level.does()+" Through the folder it is in. Change this where it was shared.");
        final String said=stands.how==NoteStore.Stands.OWN?stands.level.words():stands.how==NoteStore.Stands.REMOVED?Groups.REMOVED:stands.how==NoteStore.Stands.NONE?Groups.ADD
            :stands.group.equals(under)?Groups.AS_GROUP:"Through "+r.groupName(stands.group);
        if(!stands.may)return stands.level==null?quietly(stands.how==NoteStore.Stands.REMOVED?said:"",null):quietly(said,stands.level.does()+" Only its owner or an admin can change this.");
        // Whether a group of theirs has the thing: only then is there a group to follow.
        boolean follows=false;for(String group:r.groupsOf(who.address))follows|=r.given.containsKey(group);
        final boolean follow=follows,grouped=stands.how==NoteStore.Stands.GROUP;
        final TextView set=a.label(said+"  ▾",MainActivity.QUIET,a.MUTED);
        set.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);set.setMinHeight(a.dp(48));set.setPadding(a.dp(12),0,0,0);set.setBackgroundResource(a.touchFeedback());
        set.setContentDescription(stands.has()?who.name+", "+said+". Tap to change.":stands.how==NoteStore.Stands.REMOVED?who.name+" was removed. Tap to change.":"Share with "+who.name);
        final Sharing.Level now=stands.how==NoteStore.Stands.OWN?stands.level:null;
        final List<Sharing.Level> shown=new ArrayList<>(r.give);
        if(now!=null&&!shown.contains(now)){shown.add(now);java.util.Collections.sort(shown,(x,y)->y.ordinal()-x.ordinal());}
        final java.util.function.Consumer<Sharing.Level> own=to->rights(withScope,withTarget,who,stands.rule,to,this::drawWith);
        final Runnable asGroup=()->{if(!grouped)a.groupWork("Giving "+who.name+" the group's rights…",()->a.store.asTheGroup(withScope,withTarget,who.address),who.name+" has the group's rights",this::drawWith);};
        final Runnable remove=()->own.accept(Sharing.Level.GONE);
        set.setOnClickListener(v->{
            List<Object> more=new ArrayList<>();
            if(follow){more.add(null);more.add(grouped?"✓  "+Groups.AS_GROUP:Groups.AS_GROUP);more.add(asGroup);}
            if(stands.has()){more.add(null);more.add("Remove");more.add(remove);}
            a.rolesDown(set,shown,now,own,more.toArray());});
        return set;
    }

    /** Where somebody stands, said and not changed here: a tap says what it lets them do, where there is anything to say. */
    private View quietly(final String said,final String does) {
        final TextView words=a.label(said,MainActivity.QUIET,a.MUTED);
        words.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);words.setPadding(a.dp(12),0,0,0);
        if(does!=null){words.setMinHeight(a.dp(48));words.setBackgroundResource(a.touchFeedback());words.setOnClickListener(v->a.roleSays(words,said,does));}
        return words;
    }
}
