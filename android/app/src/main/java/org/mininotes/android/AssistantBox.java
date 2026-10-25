// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Agent on the phone (decisions 191, 192 and 213; see Assistant), as the PC has it: one conversation that scrolls, what you
 * asked on the right and each answer a card, newest at the bottom, suggestions to tap and the field under it all. A question is
 * answered from the help kept in the app; a task is shown as what it would do, with a button to do it, and done the way its
 * menu does it. Its symbol beside ⋮, or Menu → Agent…. No button closes it: a tap outside it does, as for every box.
 *
 * <p>The owner, 2026-10-19 (decision 213): the conversation takes as much of the screen as it can, Full screen gives it all of
 * it, an answer about a place in the app ends with Take me there, and what was said is kept in the notebook on this phone for
 * three hours after the last message, through closing the box and the app starting again.
 */
final class AssistantBox {
    private final MainActivity a;
    private LinearLayout talk;
    private ScrollView scroll;
    private EditText field;
    private AlertDialog box;
    private TextView full;
    /** The height the conversation is given, without the keyboard; and whether the box is the whole screen. */
    private int tall;
    private boolean wholeScreen;
    /** What has been said, as it is kept; and the exchange each answer's card belongs to. */
    private final Assistant.Conversation kept=new Assistant.Conversation();
    private final Map<LinearLayout,List<Assistant.Line>> turns=new IdentityHashMap<>();

    AssistantBox(MainActivity a){this.a=a;}

    void open() {
        LinearLayout whole=a.column();whole.setPadding(a.dp(16),a.dp(4),a.dp(16),a.dp(4));
        // What it is, first and staying there (the owner, 2026-10-18: "make it clear at the top of the agent dialog box that it
        // answers questions and does stuff, that everything is private in the app").
        whole.addView(words(Say.t(Assistant.WHAT),MainActivity.QUIET,a.INK));
        TextView privately=words(Say.t(Assistant.PRIVATE_PHONE),MainActivity.QUIET,a.ACCENT);privately.setTypeface(privately.getTypeface(),Typeface.BOLD);
        whole.addView(privately);
        // A new conversation, and the whole screen.
        LinearLayout tools=new LinearLayout(a);tools.setGravity(Gravity.END);tools.setPadding(0,a.dp(2),0,a.dp(2));
        TextView afresh=chip(Say.t(Assistant.AFRESH),this::afresh);afresh.setContentDescription(Say.t(Assistant.KEPT_SAID));
        full=chip(Say.t(Assistant.FULL),this::fullScreen);
        tools.addView(afresh);tools.addView(full);
        whole.addView(tools,new LinearLayout.LayoutParams(-1,-2));
        talk=a.column();
        scroll=new ScrollView(a);scroll.addView(talk);
        // Most of the screen for the conversation (decision 213).
        tall=Math.round(a.getResources().getDisplayMetrics().heightPixels*0.58f);
        whole.addView(scroll,new LinearLayout.LayoutParams(-1,tall));
        // The suggestions, swiped across: one that needs words of its own puts its start in the field.
        LinearLayout chips=new LinearLayout(a);chips.setPadding(0,a.dp(8),0,a.dp(4));
        for(String one:Assistant.SUGGESTED)chips.addView(chip(Say.t(one),()->{
            if(one.endsWith("…")){field.setText(starting(one));field.setSelection(Math.max(0,field.getText().toString().indexOf("  ")+1));}
            else ask(Say.t(one),one);
        }));
        HorizontalScrollView across=new HorizontalScrollView(a);across.setHorizontalScrollBarEnabled(false);across.addView(chips);
        whole.addView(across);
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER_VERTICAL);
        field=a.field("Ask, or say what to do: share Grocery with Carol",400);
        field.setImeOptions(EditorInfo.IME_ACTION_SEND|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        field.setOnEditorActionListener((v,action,key)->{if(action==EditorInfo.IME_ACTION_SEND){askField();return true;}return false;});
        // The field and Ask side by side, the same height and level (the owner, 2026-10-18: "the ask button is not well
        // displayed").
        LinearLayout.LayoutParams wide=new LinearLayout.LayoutParams(0,a.dp(52),1);
        line.addView(field,wide);
        TextView go=button(Say.t("Ask"),this::askField,true);
        LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-2,a.dp(52));at.setMargins(a.dp(8),0,0,0);
        line.addView(go,at);
        line.setPadding(0,a.dp(6),0,a.dp(8));
        whole.addView(line);
        box=a.new Box().setTitle("Agent").setView(whole).show();
        fitAboveKeyboard(whole);
        remember();
    }

    /**
     * The whole box above the keyboard, its top included (the owner, 2026-10-18: "with the keyboard activated, the top of the
     * window is not visible"): the window is resized rather than pushed up, and while the keyboard is there what was said
     * takes only the room left, never less than a few lines.
     */
    private void fitAboveKeyboard(View whole) {
        if(box.getWindow()==null)return;
        box.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            |android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        View root=box.getWindow().getDecorView();
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            android.graphics.Rect seen=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(seen);
            int screen=a.getResources().getDisplayMetrics().heightPixels;
            boolean keys=screen-seen.height()>screen/4;
            // The rest of the box - its title, what it is, the suggestions, the field - and the room round it.
            int rest=whole.getHeight()-scroll.getHeight()+a.dp(this.wholeScreen?72:120);
            int room=this.wholeScreen?screen-rest:tall;
            int want=keys?Math.max(a.dp(64),Math.min(room,seen.height()-rest)):Math.max(a.dp(64),room);
            android.view.ViewGroup.LayoutParams now=scroll.getLayoutParams();
            if(Math.abs(now.height-want)>a.dp(8)){now.height=want;scroll.setLayoutParams(now);}
        });
    }

    /** The box over the whole screen, and back (decision 213). */
    private void fullScreen() {
        if(box==null||box.getWindow()==null)return;
        wholeScreen=!wholeScreen;
        if(wholeScreen)box.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        else box.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        full.setText(Say.t(wholeScreen?Assistant.SMALLER:Assistant.FULL));
        scroll.requestLayout();down();
    }

    private String starting(String suggestion) {
        if(suggestion.startsWith("Add to my todo list"))return Say.t("Add {0} to my todo list","");
        if(suggestion.startsWith("Share a note"))return Say.t("Share {0} with {1}","","");
        return Say.t(suggestion).replace("…","");
    }

    private void askField() {
        String said=field.getText().toString().trim();
        if(said.isEmpty())return;
        field.setText("");
        ask(said);
    }

    void ask(String said){ask(said,said);}
    /** Shown as one thing and understood as another: a suggestion is shown in the language in use and read in English. */
    void ask(String said,String meant) {
        long now=System.currentTimeMillis();
        // Three hours after the last message, it starts afresh, even with the box open all along.
        if(kept.over(now))afresh();
        talk.addView(bubble(said));
        LinearLayout card=a.framed(talk);turns.put(card,kept.start(said,now));
        keep();
        answer(Assistant.understand(meant),card);
        down();
    }

    // ---- the conversation kept (decision 213) ------------------------------------------------------------------------

    /** Nothing is kept while a private space is up: whatever was said there is not written anywhere. */
    private boolean keeping(){return a.privately==null||!a.privately.showing();}

    /** What was said in the last three hours, read from the notebook and shown first, as it was. */
    private void remember() {
        if(!keeping())return;
        a.background.submit(()->a.store.secret(Assistant.KEPT_AS),text->{
            Assistant.Conversation earlier=Assistant.Conversation.read(text,System.currentTimeMillis());
            if(earlier.isEmpty()||box==null||!box.isShowing())return;
            kept.before(earlier);
            LinearLayout shown=a.column();
            for(List<Assistant.Line> turn:earlier.turns)replay(shown,turn);
            talk.addView(shown,0,new LinearLayout.LayoutParams(-1,-2));
            // Asked before it was read, the new and the old are kept together.
            if(kept.turns.size()>earlier.turns.size())keep();
            down();
        },e->{});
    }

    /** Written to the notebook as it is now, off the screen's thread. */
    private void keep() {
        if(!keeping())return;
        String text=kept.isEmpty()?"":kept.write();
        a.background.submit(()->{a.store.setSecret(Assistant.KEPT_AS,text);return null;},done->{},e->{});
    }

    /** A new conversation: what was said is gone, here and in the notebook. */
    private void afresh() {
        kept.clear();turns.clear();talk.removeAllViews();
        a.background.submit(()->{a.store.setSecret(Assistant.KEPT_AS,"");return null;},done->{},e->{});
    }

    /** An exchange kept, shown again: what was asked, and what was said back. A task's buttons are not kept, only its words. */
    private void replay(LinearLayout into,List<Assistant.Line> turn) {
        LinearLayout card=null;
        for(Assistant.Line one:turn) {
            if(one.kind()==Assistant.Kind.YOU){into.addView(bubble(one.words()));continue;}
            if(card==null)card=a.framed(into);
            switch(one.kind()) {
                case TITLE -> title(card,one.words());
                case SAY -> say(card,one.words());
                case QUIET -> quietly(card,one.words());
                case THERE -> {try{there(card,Assistant.Place.valueOf(one.words()));}catch(IllegalArgumentException unknown){/* a later version's place */}}
                default -> {}
            }
        }
    }

    /** What is said in an answer, kept with its exchange. */
    private void record(LinearLayout in,Assistant.Kind kind,String words) {
        List<Assistant.Line> turn=turns.get(in);
        if(turn==null)return;
        kept.add(turn,kind,words,System.currentTimeMillis());keep();
    }

    // ---- what it shows -----------------------------------------------------------------------------------------------

    /** Words as they are: what was typed, or what Assistant already put in the language in use. */
    private TextView words(String s,int size,int colour) {
        // The app's own words, at the size chosen in the menu; set after, so what was typed is not looked up as a phrase.
        TextView t=a.label("",size,colour);t.setText(s);
        t.setPadding(0,a.dp(4),0,a.dp(4));t.setTextIsSelectable(true);return t;
    }
    /** What you asked, on the right, as a chat has it. */
    private View bubble(String said) {
        TextView you=words(said,MainActivity.QUIET,a.INK);you.setTypeface(you.getTypeface(),Typeface.BOLD);
        you.setPadding(a.dp(12),a.dp(8),a.dp(12),a.dp(8));
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(a.dp(12));shape.setColor(a.PAPER);shape.setStroke(Math.max(1,a.dp(1)),a.LINE);
        you.setBackground(shape);
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.END);line.setPadding(a.dp(48),a.dp(10),0,0);
        line.addView(you,new LinearLayout.LayoutParams(-2,-2));
        return line;
    }
    private void title(LinearLayout in,String s) {
        TextView title=words(s,MainActivity.READING,a.INK);title.setTypeface(title.getTypeface(),Typeface.BOLD);in.addView(title);
        record(in,Assistant.Kind.TITLE,s);
    }
    private void say(LinearLayout in,String s){in.addView(words(s,MainActivity.READING,a.INK));record(in,Assistant.Kind.SAY,s);down();}
    private void quietly(LinearLayout in,String s){in.addView(words(s,MainActivity.QUIET,a.MUTED));record(in,Assistant.Kind.QUIET,s);down();}
    private void failed(LinearLayout in,Throwable e){say(in,e==null||e.getMessage()==null?Say.t("Nothing was changed."):Say.t(e.getMessage()));}

    private TextView button(String s,Runnable go,boolean main) {
        TextView b=a.label("",MainActivity.QUIET,a.INK);b.setText(s);b.setGravity(Gravity.CENTER);b.setMinHeight(a.dp(44));
        b.setPadding(a.dp(14),a.dp(8),a.dp(14),a.dp(8));
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(a.dp(12));
        if(main){shape.setColor(a.ACCENT);b.setTextColor(a.PAPER);b.setTypeface(b.getTypeface(),Typeface.BOLD);}
        else{shape.setColor(a.CARD);shape.setStroke(Math.max(1,a.dp(1)),a.LINE);b.setTextColor(a.INK);}
        b.setBackground(shape);
        b.setOnClickListener(v->go.run());
        return b;
    }
    private TextView chip(String s,Runnable go) {
        TextView b=button(s,go,false);
        LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-2,-2);at.setMargins(0,0,a.dp(8),0);b.setLayoutParams(at);
        return b;
    }
    /** Buttons one under the other, the width of the card: a thumb finds each. */
    private void buttons(LinearLayout in,TextView... them) {
        for(TextView one:them){LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-1,-2);at.setMargins(0,a.dp(4),0,a.dp(4));in.addView(one,at);}
        down();
    }
    private void down(){if(scroll!=null)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    private void close(){if(box!=null)box.dismiss();}

    // ---- what it does: as on the PC (DesktopAssistant) ------------------------------------------------------------

    private void answer(Assistant.Asked asked,LinearLayout in) {
        switch(asked.act()) {
            case NOTHING -> {}
            case HELP -> help(asked.what(),in);
            case WHAT_NEW -> whatIsNew(in);
            case TODO_SHOW -> todoShow(in);
            case TODO_ADD -> lineAdd(asked.what(),asked.who(),in);
            case SHARE -> share(asked,in);
            case ADD_CONTACT -> addSomeone(in);
            case OPEN -> openThing(asked.what(),in);
            case FIND -> find(asked.what(),in);
            case NEW_NOTE -> make(asked.what(),"",in);
            case DOCK -> dock(asked.what(),in);
            case FAVOURITE -> favourite(asked.what(),in);
        }
    }

    private void help(String question,LinearLayout in) {
        List<Assistant.Help> found=Assistant.lookUp(question,Assistant.help(Say.language()),3);
        if(found.isEmpty()){say(in,Say.t("I do not have an answer for that yet. Try other words, or one of the suggestions below."));return;}
        helpCard(found.get(0),question,in);
        if(found.size()>1){quietly(in,Say.t("Also:"));for(Assistant.Help one:found.subList(1,found.size()))buttons(in,button(one.title()+"  ›",()->{
            LinearLayout card=a.framed(talk);turns.put(card,kept.start(null,System.currentTimeMillis()));helpCard(one,question,card);},false));}
    }
    private void helpCard(Assistant.Help one,String question,LinearLayout in) {
        title(in,one.title());
        for(String paragraph:one.answer().split("\n\n"))say(in,paragraph);
        TextView doIt=doer(one.act());
        if(doIt!=null)buttons(in,doIt);
        Assistant.Place place=Assistant.place(one,question);
        if(place!=null)there(in,place);
    }
    private TextView doer(Assistant.Act act) {
        if(act==null)return null;
        return switch(act) {
            case WHAT_NEW -> button(Say.t("Show what is new"),()->ask(Say.t("What is new?"),"What is new?"),false);
            case TODO_SHOW -> button(Say.t("Show my todo list"),()->ask(Say.t("What is on my todo list?"),"What is on my todo list?"),false);
            case ADD_CONTACT -> button(Say.t("Add someone…"),()->ask(Say.t("Add someone"),"Add someone"),false);
            case SHARE -> button(Say.t("Share a note…"),()->{field.setText(starting("Share a note…"));field.requestFocus();},false);
            default -> null;
        };
    }

    // ---- Take me there (decision 213) ------------------------------------------------------------------------------

    /** Take me there, under an answer about a place this phone has: the box goes, and the place opens, as its menu opens it. */
    private void there(LinearLayout in,Assistant.Place place) {
        Runnable go=way(place);
        if(go==null)return;
        record(in,Assistant.Kind.THERE,place.name());
        buttons(in,button(Say.t(Assistant.THERE),()->{close();go.run();},false));
    }
    /**
     * How each place is opened on the phone, as its menu opens it; null for one the phone has no way to yet: Settings is
     * the menu's own, and the PC's text size for the whole app and its descriptions under the pointer are the PC's alone.
     */
    private Runnable way(Assistant.Place place) {
        return switch(place) {
            case PROFILE -> a::profile;
            // At its own part of My profile (the owner, 2026-10-19: "in setting Integritas it will open directly the section in
            // the profile page"; decision 231: that part is Stamping).
            case INTEGRITAS -> ()->a.profile(MainActivity.STAMPING,null);
            case PARLONS -> ()->a.profile("You",null);
            case BACKUP -> ()->a.profile("Backup",null);
            case PEOPLE -> ()->a.people().open();
            case LANGUAGE -> a::chooseLanguage;
            case STAMPED -> ()->PhoneStamp.list(a);
            case SENT_FILES -> ()->a.homeScreen().sentFiles();
            case BIN -> ()->a.homeScreen().openPlace(HomeScreen.bin(0));
            case ARCHIVE -> ()->a.homeScreen().openPlace(HomeScreen.archive(0));
            case SHARED_WITH_ME -> ()->a.homeScreen().openPlace(HomeScreen.shared());
            case SECURITY,SHOW_ON_HOME,TEMP,SETTINGS -> a::settings;
            default -> null;
        };
    }

    private void whatIsNew(LinearLayout in) {
        a.background.submit(()->a.store.whatIsNew(),news->{
            if(news.isEmpty()){say(in,Say.t("Nothing is new. Everything has been seen."));return;}
            say(in,news.size()==1?Say.t("One thing is new:"):Say.t("{0} things are new:",String.valueOf(news.size())));
            for(NoteStore.Branch one:news.subList(0,Math.min(8,news.size())))buttons(in,button(one.name+"  ›",()->{close();open(one);},false));
            buttons(in,button(Say.t("Open New"),()->{close();a.whatIsNew();},true));
        },e->failed(in,e));
    }

    /** A note or a folder opened from here: the box closes, so it is what is in front. */
    private void open(NoteStore.Branch one) {
        if(one.kind==NoteStore.Branch.Kind.PAGE)a.homeScreen().openNote(one.id);
        else if(one.kind==NoteStore.Branch.Kind.COLLECTION)a.homeScreen().openCollection(one.id);
    }

    private void todoShow(LinearLayout in) {
        a.background.submit(()->Assistant.todo(a.store),todo->{
            if(todo==null){say(in,Assistant.noTodo());buttons(in,button(Say.t("Make it"),()->make(Say.t("Todo"),"",in),true));return;}
            List<String> lines=Assistant.lines(todo.body);
            if(lines.isEmpty())say(in,Say.t("“{0}” is empty.",todo.heading()));
            else{say(in,Say.t("On “{0}”:",todo.heading()));StringBuilder all=new StringBuilder();for(String one:lines)all.append(all.length()==0?"":"\n").append("•  ").append(one);say(in,all.toString());}
            buttons(in,button(Say.t("Open it"),()->{close();a.homeScreen().openNote(todo.id);},false),
                button(Say.t("Add a line…"),()->{field.setText(starting("Add to my todo list…"));field.setSelection(Math.max(0,field.getText().toString().indexOf("  ")+1));},false));
        },e->failed(in,e));
    }

    private void lineAdd(String line,String noteWords,LinearLayout in) {
        if(line==null||line.isBlank()){say(in,Say.t("What should I add? For example: add buy bread to my todo list."));return;}
        a.background.submit(()->{
            if(noteWords==null||noteWords.isBlank())return Assistant.todo(a.store);
            NoteStore.Branch found=Assistant.thing(a.store,noteWords);
            return found==null||found.kind!=NoteStore.Branch.Kind.PAGE?null:a.store.get(found.id);
        },note->{
            if(note==null&&noteWords!=null&&!noteWords.isBlank()){say(in,Assistant.notFound(noteWords));return;}
            if(note==null){say(in,Assistant.noTodo());buttons(in,button(Say.t("Make it"),()->make(Say.t("Todo"),line,in),true));return;}
            if(Assistant.readOnly(note)){say(in,Say.t("You can only read “{0}”.",note.heading()));return;}
            say(in,Assistant.addAsk(line,note.heading()));
            TextView[] add={null};
            add[0]=button(Say.t("Add"),()->{add[0].setEnabled(false);add[0].setAlpha(0.5f);
                a.background.submit(()->Assistant.addLine(a.store,note.id,line),written->{say(in,Assistant.added(written.heading()));sent(written.id);},e->failed(in,e));},true);
            buttons(in,add[0]);
        },e->failed(in,e));
    }

    /** Written here: the open page follows, and it goes as a page's writing goes. */
    private void sent(String id) {
        a.changedUnderneath(id);a.refresh();
        a.network.submit(()->Post.send(a,a.store,a.keys(),NoteStore.Branch.Kind.PAGE,id),done->a.refresh(),e->{});
    }

    private void make(String title,String body,LinearLayout in) {
        a.background.submit(()->Assistant.make(a.store,title,body),made->{
            say(in,Say.t("Made “{0}” on Home.",made.heading()));a.refresh();
            buttons(in,button(Say.t("Open it"),()->{close();a.homeScreen().openNote(made.id);},false));
        },e->failed(in,e));
    }

    private void share(Assistant.Asked asked,LinearLayout in) {
        a.background.submit(()->new Object[]{Assistant.thing(a.store,asked.what()),Assistant.person(a.store,asked.who()),Assistant.people(a.store)},got->{
            NoteStore.Branch thing=(NoteStore.Branch)got[0];NoteStore.Contact who=(NoteStore.Contact)got[1];
            @SuppressWarnings("unchecked") List<String> people=(List<String>)got[2];
            if(thing==null){say(in,Assistant.notFound(asked.what()));return;}
            if(who==null){say(in,Assistant.nobody(asked.who(),people));if(people.isEmpty())buttons(in,button(Say.t("Add someone…"),()->ask(Say.t("Add someone"),"Add someone"),false));return;}
            String name=a.store.nameFor(who.address);
            boolean[] read={asked.readOnly()};
            TextView question=words(Assistant.shareAsk(thing.name,name,read[0]),MainActivity.READING,a.INK);in.addView(question);
            TextView[] go={null,null};
            go[1]=button(Say.t(read[0]?"Can write instead":"Can read instead"),()->{read[0]=!read[0];question.setText(Assistant.shareAsk(thing.name,name,read[0]));
                go[1].setText(Say.t(read[0]?"Can write instead":"Can read instead"));},false);
            go[0]=button(Say.t("Share"),()->{go[0].setEnabled(false);go[1].setEnabled(false);go[0].setAlpha(0.5f);
                a.background.submit(()->{Assistant.share(a.store,thing,who,read[0]);return null;},done->{
                    say(in,Assistant.shared(name));
                    a.network.submit(()->{Post.changed(a,a.store,a.keys(),thing.kind,thing.id);return Post.send(a,a.store,a.keys(),thing.kind,thing.id);},v->a.refresh(),e->{});
                },e->failed(in,e));},true);
            buttons(in,go[0],go[1]);
        },e->failed(in,e));
    }

    private void addSomeone(LinearLayout in) {
        say(in,Say.t("To add someone, one of you shows a code and the other scans it. Scan their code, or paste the link they sent you; or show them your code."));
        buttons(in,button(Say.t("Scan their code or paste their link…"),()->{close();a.addFromSomeone();},true),
            button(Say.t("People and devices"),()->{close();a.people().open();},false));
    }

    private void openThing(String words,LinearLayout in) {
        // A place of the app's, "open the bin", "open my profile", is opened as its menu opens it.
        Assistant.Place place=Assistant.placeNamed(words);
        Runnable go=place==null?null:way(place);
        if(go!=null){close();go.run();return;}
        a.background.submit(()->Assistant.thing(a.store,words),thing->{
            if(thing==null){say(in,Assistant.notFound(words));find(words,in);return;}
            close();open(thing);
        },e->failed(in,e));
    }

    private void find(String words,LinearLayout in) {
        a.background.submit(()->a.store.lookingEverywhere(words),found->{
            if(found.isEmpty()){say(in,Say.t("Nothing found for “{0}”.",words));return;}
            say(in,found.size()==1?Say.t("Found one:"):Say.t("Found {0}:",String.valueOf(found.size())));
            for(NoteStore.Branch one:found.subList(0,Math.min(8,found.size())))buttons(in,button(one.name+"  ›",()->{close();open(one);},false));
        },e->failed(in,e));
    }

    private void dock(String words,LinearLayout in) {
        a.background.submit(()->{NoteStore.Branch thing=Assistant.thing(a.store,words);if(thing!=null)a.store.toDockEnd(thing.kind,thing.id);return thing;},thing->{
            if(thing==null){say(in,Assistant.notFound(words));return;}
            say(in,Say.t("“{0}” is in the dock.",thing.name));a.refresh();
        },e->failed(in,e));
    }

    private void favourite(String words,LinearLayout in) {
        a.background.submit(()->{NoteStore.Branch thing=Assistant.thing(a.store,words);if(thing!=null)a.store.keepToHand(thing.kind,thing.id,true);return thing;},thing->{
            if(thing==null){say(in,Assistant.notFound(words));return;}
            say(in,Say.t("“{0}” is a favourite, and in the dock.",thing.name));a.refresh();
        },e->failed(in,e));
    }
}
