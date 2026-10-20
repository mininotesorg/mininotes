// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.OverScroller;

/** The page: an editor ruled like paper, so a blank note reads as something to write on. */
// Built only in code, like every view in this app; there is no layout XML for a tool to inflate it from.
@android.annotation.SuppressLint("ViewConstructor")
final class Pad extends EditText {
    private final Paint rule=new Paint();
    private final Rect line=new Rect();
    private final Rect clip=new Rect();
    private final int drop;
    // A swipe keeps the page moving after the finger lifts, as every other list on the phone does. An editor
    // scrolls only while it is dragged, so a long note went by one push at a time.
    private final OverScroller coasting;
    private final GestureDetector swipes;
    private android.os.Bundle resizing;
    private android.os.Bundle pendingPosition;
    private boolean writing;

    void caretVisible(boolean visible){writing=visible;}

    /** Store offsets, never the note's words. The top character survives a different line width. */
    android.os.Bundle position() {
        android.os.Bundle state=new android.os.Bundle();
        state.putInt("start",Math.max(0,getSelectionStart()));state.putInt("end",Math.max(0,getSelectionEnd()));
        android.text.Layout lines=getLayout();
        if(lines!=null) {
            int line=lines.getLineForVertical(getScrollY());
            state.putInt("top",lines.getLineStart(line));
            state.putFloat("within",(getScrollY()-lines.getLineTop(line))/(float)Math.max(1,lines.getLineBottom(line)-lines.getLineTop(line)));
        }
        return state;
    }

    void restorePosition(android.os.Bundle state) {
        android.text.Layout lines=getLayout();if(lines==null)return;
        int length=length();
        setSelection(Math.min(length,state.getInt("start")),Math.min(length,state.getInt("end")));
        int line=lines.getLineForOffset(Math.min(length,state.getInt("top")));
        int top=lines.getLineTop(line)+Math.round(state.getFloat("within")*(lines.getLineBottom(line)-lines.getLineTop(line)));
        scrollTo(0,Math.max(0,Math.min(top,furthest())));
        if(writing)bringPointIntoView(Math.max(0,getSelectionEnd()));
    }

    void restoreAfterLayout(android.os.Bundle state) {
        boolean waiting=pendingPosition!=null;
        pendingPosition=state;
        if(waiting)return;
        getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                android.os.Bundle held=pendingPosition;pendingPosition=null;
                restorePosition(held);return true;
            }
        });
        invalidate();
    }

    @Override protected void onMeasure(int width,int height) {
        if(getLayout()!=null&&getWidth()>0&&getHeight()>0
            &&(MeasureSpec.getSize(width)!=getWidth()||MeasureSpec.getSize(height)!=getHeight())&&resizing==null)resizing=position();
        super.onMeasure(width,height);
        if(getMeasuredWidth()==getWidth()&&getMeasuredHeight()==getHeight())resizing=null;
    }

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        if(resizing!=null) {
            final android.os.Bundle held=resizing;resizing=null;
            coasting.forceFinished(true);
            if(pendingPosition==null)restoreAfterLayout(held);
        }
    }

    /**
     * The page's words. A letter typed moves every style after it along by one, and Android tells the page's layout of
     * each style moved as though it had changed, which lays its paragraph out again twice: in a long note with marks that
     * was seconds a keystroke near the top (Graphene, 2026-10-10). A style moved with its words lays out the same, and the
     * paragraph written in is laid out again for the letters anyway, so while styles are only being moved along after a
     * change the layout alone is not told. Everybody else still is, and every other change reaches the layout as before.
     */
    static final class Words extends android.text.SpannableStringBuilder {
        private static final String LAYOUT="android.text.DynamicLayout$ChangeWatcher";
        /** Told last that the letters have changed: what follows is the styles being moved along. */
        private final class Moving implements TextWatcher {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable e){moving=true;watchers=null;}
        }
        private boolean moving;
        /**
         * Who watches the styles, less the layout, found once while they are moved along: looking them up again for each
         * style moved was most of a keystroke. Nothing is added or taken off meanwhile without it being looked up again.
         */
        private Object[] watchers;
        Words(CharSequence text) {
            super(text);
            for(Moving copied:getSpans(0,length(),Moving.class))super.removeSpan(copied);
            super.setSpan(new Moving(),0,length(),SPAN_INCLUSIVE_INCLUSIVE);
        }
        @Override public android.text.SpannableStringBuilder replace(int start,int end,CharSequence words,int from,int to) {
            boolean was=moving;moving=false;
            try{return super.replace(start,end,words,from,to);}finally{moving=was;}
        }
        @Override public void setSpan(Object what,int start,int end,int flags) {
            boolean was=moving;moving=false;watchers=null;
            try{super.setSpan(what,start,end,flags);}finally{moving=was;watchers=null;}
        }
        @Override public void removeSpan(Object what) {
            boolean was=moving;moving=false;watchers=null;
            try{super.removeSpan(what);}finally{moving=was;watchers=null;}
        }
        @SuppressWarnings("unchecked")
        @Override public <T> T[] getSpans(int start,int end,Class<T> kind) {
            if(!moving||kind!=android.text.SpanWatcher.class)return super.getSpans(start,end,kind);
            if(watchers==null) {
                java.util.List<Object> found=new java.util.ArrayList<>();
                for(Object one:super.getSpans(0,length(),kind))if(!one.getClass().getName().equals(LAYOUT))found.add(one);
                watchers=found.toArray();
            }
            // The ones the stretch would have found, as getSpans finds them: touching it, but not only at an edge.
            java.util.List<T> some=new java.util.ArrayList<>(watchers.length);
            for(Object one:watchers) {
                int from=getSpanStart(one),to=getSpanEnd(one);
                if(from<0||from>end||to<start)continue;
                if(from!=to&&start!=end&&(from==end||to==start))continue;
                some.add((T)one);
            }
            return some.toArray((T[])java.lang.reflect.Array.newInstance(kind,some.size()));
        }
    }

    /** Whether the keyboard may learn from what is written in a note here, and so dictate (decision 222): yes unless switched off. */
    static final String KEYBOARD_LEARNS="keyboardLearns";
    static boolean learnsHere(Context c){return !"false".equals(c.getSharedPreferences("settings",Context.MODE_PRIVATE).getString(KEYBOARD_LEARNS,"true"));}
    /** The keyboard told whether it may learn, at once if it is up. */
    void learns(boolean on) {
        int flag=android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        int was=getImeOptions(),now=on?was&~flag:was|flag;
        if(now==was)return;
        setImeOptions(now);
        android.view.inputmethod.InputMethodManager keys=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(keys!=null&&hasFocus())keys.restartInput(this);
    }

    Pad(Context c,int colour) {
        super(c);
        setEditableFactory(new Editable.Factory(){@Override public Editable newEditable(CharSequence source){return new Words(source);}});
        rule.setColor(colour);rule.setStrokeWidth(1f);
        drop=(int)(getResources().getDisplayMetrics().density*4);
        setBackground(null);
        // The keyboard is told not to learn from what is written in a note, any note (the owner, 2026-10-06; decision 111):
        // what is typed is not offered back as a suggestion, nor kept by the keyboard.
        // Also: the keyboard never takes the note over. In landscape most keyboards go fullscreen, putting their own
        // one-field editor over the whole note (the owner, 2026-10-07: "the keyboard takes over the text"); NO_EXTRACT_UI
        // and NO_FULLSCREEN keep the note itself in view, the keyboard below it, and the line being written scrolled to.
        // Not learning is the keyboard's incognito, and keyboards turn their voice typing off there (the owner, 2026-10-19:
        // "the micro to dictate text in the keyboard now says it is unavailable in the note"; decision 222): so a switch in
        // Settings, on by default, lets the keyboard learn, and dictate. A private space's page stays incognito whatever it says.
        setImeOptions(getImeOptions()|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        learns(learnsHere(c));
        coasting=new OverScroller(c);
        // Before anybody else's watcher: whoever reads the page after a keystroke reads runs that already fit it.
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){replaced=count>0&&after>0&&!putting?s.subSequence(start,start+count).toString():null;}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){typed(s,start,before,count);}
            @Override public void afterTextChanged(Editable e){}
        });
        swipes=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent from,MotionEvent to,float across,float down) {
                int most=furthest();
                if(most<=0)return false;
                coasting.fling(0,getScrollY(),0,Math.round(-down),0,0,0,most);
                postInvalidateOnAnimation();
                return true;
            }
        });
    }

    /** How far down the page can go: the writing's height, less what the screen already shows. */
    private int furthest() {
        if(getLayout()==null)return 0;
        return Math.max(0,getLayout().getHeight()+getTotalPaddingTop()+getTotalPaddingBottom()-getHeight());
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        // A finger on the page stops it where it is, the way a hand stops a sliding sheet.
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&!coasting.isFinished())coasting.forceFinished(true);
        swipes.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override public void computeScroll() {
        if(coasting.computeScrollOffset()) {
            scrollTo(getScrollX(),Math.min(coasting.getCurrY(),furthest()));
            postInvalidateOnAnimation();
            return;
        }
        super.computeScroll();
    }

    // A tap that opened an address is a click, and is reported as one so a screen reader sees it happen.
    @Override public boolean performClick(){return super.performClick();}

    // ---- who wrote what (see Writers) -----------------------------------------------------------------------------

    /** A writer's colour on their letters. Its own kind, so it is never taken for any other span on the page. */
    private static final class Ink extends ForegroundColorSpan {Ink(int colour){super(colour);}}
    /** Writers' colours are drawn before any other style, whichever was put on the words first. */
    private static final int INK_FIRST=1<<Spanned.SPAN_PRIORITY_SHIFT;

    /** Who wrote each letter on the page: always as long as the page's text. */
    private Writers runs=Writers.none();
    private Writers.Palette palette=Writers.Palette.plain();
    /** Whether writers are drawn in their colours at all: "Show who wrote what". */
    private boolean wanted=true;
    /** Whether they are drawn now: wanted, and two or more people wrote on this page. */
    private boolean inked;
    /** The paper under the letters, which the colours are made readable against. */
    private int paper=0xFFFFFFFF;
    /** Text being put on the page with its writers, rather than typed. */
    private boolean putting;
    /** The letters an edit is about to replace, to tell what in it is really new. */
    private String replaced;

    /** A keystroke, a paste, a cut, anything typed: the new letters are yours, and the rest keep their writers. */
    private void typed(CharSequence now,int start,int before,int count) {
        if(putting)return;
        restyleSoon(start,before,count);
        // A keyboard writes a word again whole as each letter of it is typed, and takes up a word already there when the
        // cursor lands in it: only what is really new in it is yours, and the letters it kept keep their writer.
        String was=replaced;replaced=null;
        if(was!=null&&was.length()==before) {
            int same=0,end=0;
            while(same<before&&same<count&&was.charAt(same)==now.charAt(start+same))same++;
            while(end<before-same&&end<count-same&&was.charAt(before-1-end)==now.charAt(start+count-1-end))end++;
            start+=same;before-=same+end;count-=same+end;
        }
        runs.edit(start,before,count,Writers.ME);
        // Never drawn from runs that do not fit the words: nobody's, rather than a colour on the wrong letters.
        if(runs.length()!=now.length()){runs=Writers.unknown(now.length());ink();return;}
        if(inked!=(wanted&&palette.shows(runs))){ink();return;}
        if(inked)ink(start,start+count);
    }

    /** Words put on the page as they are, with who wrote them - opening a note, or one that changed underneath it. */
    void put(CharSequence text,Writers who) {
        // Styled before it goes on the page: a style added to words already laid out lays their paragraph out again, and
        // one at a time that took seconds to open a long note with marks in it (Graphene, 2026-10-10). Laid out once here.
        android.text.SpannableString styled=new android.text.SpannableString(text==null?"":text);
        mark(styled,0,styled.length());
        removeCallbacks(restyling);changedLines.clear();
        putting=true;
        try{setText(styled);}finally{putting=false;}
        String on=getText().toString();
        String given=text==null?"":text.toString();
        runs=who!=null&&who.length()==on.length()&&given.equals(on)?who.copy():Writers.follow(on,Writers.UNKNOWN,given,who);
        ink();
        // Whatever a filter on the page cut away changed where the marks fall: those are drawn again from the page.
        if(!given.equals(on))restyle();
    }

    // ---- bold, italic and underline, as marks in the words (see Marks; docs/HOME.md, decision 76) ----------------------

    /** The spans this page puts on its marks and their words: its own kinds, so only they are taken off again. */
    private static final class Bold extends StyleSpan{Bold(){super(android.graphics.Typeface.BOLD);}}
    private static final class Italic extends StyleSpan{Italic(){super(android.graphics.Typeface.ITALIC);}}
    private static final class Under extends UnderlineSpan{}
    /**
     * A style's marks, present in the words and taking no room (decisions 118 and 144). Drawn as nothing, measured as
     * nothing, so the line under the word sits flush with the words around it.
     */
    private static final class Gone extends ReplacementSpan {
        @Override public int getSize(Paint paint,CharSequence text,int start,int end,Paint.FontMetricsInt fm){return 0;}
        @Override public void draw(Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint paint){}
    }

    private final Marks.ChangedLines changedLines=new Marks.ChangedLines();
    private final Runnable restyling=()->{int[] changed=changedLines.take(getText());if(changed!=null)restyle(changed[0],changed[1]);};
    /** Drawn again a moment after the typing, not at every letter. */
    private void restyleSoon(int start,int before,int count){changedLines.changed(start,before,count);removeCallbacks(restyling);postDelayed(restyling,120);}

    /** Every styled stretch drawn: its words bold, italic or underlined, its marks hidden. */
    void restyle() {
        removeCallbacks(restyling);changedLines.clear();restyle(0,length());
    }

    private void restyle(int from,int to){mark(getText(),from,to);}

    private void mark(android.text.Spannable text,int from,int to) {
        // Only what differs is taken off and put on: each style put on or taken off is slow in a long note, and a letter
        // typed mostly leaves the styles on its line as they were (Graphene, 2026-10-10).
        java.util.List<Object> had=new java.util.ArrayList<>();
        for(Class<?> kind:new Class<?>[]{Bold.class,Italic.class,Under.class,Gone.class})
            had.addAll(java.util.Arrays.asList(text.getSpans(from,to,kind)));
        java.util.List<Object[]> wanted=new java.util.ArrayList<>();
        for(Marks.Run local:Marks.of(text.subSequence(from,to))) {
            Marks.Run run=new Marks.Run(local.style(),local.open()+from,local.start()+from,local.end()+from,local.close()+from);
            Class<?> style=run.style()==Marks.Style.BOLD?Bold.class:run.style()==Marks.Style.ITALIC?Italic.class:Under.class;
            wanted.add(new Object[]{style,run.start(),run.end()});
            for(int[] mark:new int[][]{{run.open(),run.start()},{run.end(),run.close()}}) {
                // Every style's marks hidden, taking no room: the word is bold, slanted or underlined, and the stars and
                // underscores that say so are not seen (the owner, 2026-10-10: bold showed ** before and after the word).
                wanted.add(new Object[]{Gone.class,mark[0],mark[1]});
            }
        }
        for(java.util.Iterator<Object[]> want=wanted.iterator();want.hasNext();) {
            Object[] one=want.next();
            for(java.util.Iterator<Object> was=had.iterator();was.hasNext();) {
                Object span=was.next();
                if(span.getClass()==one[0]&&text.getSpanStart(span)==(int)one[1]&&text.getSpanEnd(span)==(int)one[2]){was.remove();want.remove();break;}
            }
        }
        for(Object stale:had)text.removeSpan(stale);
        for(Object[] one:wanted) {
            Class<?> kind=(Class<?>)one[0];
            Object span=kind==Bold.class?new Bold():kind==Italic.class?new Italic():kind==Under.class?new Under():new Gone();
            text.setSpan(span,(int)one[1],(int)one[2],Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * A style switched on what is chosen (see {@link Marks#toggle}), as typing would: the marks put in, or taken out, where
     * they belong, so who wrote the words is kept and only the marks are this phone's. Nothing on a page only read.
     */
    void style(Marks.Style style) {
        if(getKeyListener()==null)return;
        Editable text=getText();
        int from=Math.max(0,Math.min(getSelectionStart(),getSelectionEnd())),to=Math.max(getSelectionStart(),getSelectionEnd());
        Marks.Changed now=Marks.toggle(text.toString(),from,to,style);
        String mark=style.mark;int width=mark.length();
        if(now.text().length()<text.length()) {
            text.delete(to,to+width);text.delete(from-width,from);
        } else {
            int start=now.start()-width,end=now.end()-width;
            text.insert(end,mark);text.insert(start,mark);
        }
        setSelection(Math.min(now.start(),text.length()),Math.min(now.end(),text.length()));
        // Only the lines the marks went into or came out of, which typing them noted: the whole page took seconds.
        removeCallbacks(restyling);restyling.run();
    }

    /** Ctrl+B, Ctrl+I and Ctrl+U from a keyboard on the phone, as on the PC. */
    @Override public boolean onKeyShortcut(int keyCode,KeyEvent event) {
        if(event.isCtrlPressed()&&getKeyListener()!=null) {
            if(keyCode==KeyEvent.KEYCODE_B){style(Marks.Style.BOLD);return true;}
            if(keyCode==KeyEvent.KEYCODE_I){style(Marks.Style.ITALIC);return true;}
            if(keyCode==KeyEvent.KEYCODE_U){style(Marks.Style.UNDERLINE);return true;}
        }
        return super.onKeyShortcut(keyCode,event);
    }

    {
        // And among Copy and Paste when words are chosen: Bold, Italic, Underline, for a phone with no keyboard.
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode,Menu menu) {
                if(getKeyListener()==null)return true;
                menu.add(Menu.NONE,0x4d42,200,"Bold");menu.add(Menu.NONE,0x4d49,201,"Italic");menu.add(Menu.NONE,0x4d55,202,"Underline");
                return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode mode,Menu menu){return false;}
            @Override public boolean onActionItemClicked(ActionMode mode,MenuItem item) {
                switch(item.getItemId()) {
                    case 0x4d42: style(Marks.Style.BOLD);return true;
                    case 0x4d49: style(Marks.Style.ITALIC);return true;
                    case 0x4d55: style(Marks.Style.UNDERLINE);return true;
                    default: return false;
                }
            }
            @Override public void onDestroyActionMode(ActionMode mode){}
        });
    }

    /**
     * The words on the page become others - the note changed underneath it - and the reader stays where they were.
     *
     * <p>The first line they could see, the cursor and the selection are each taken to the same words in what the page
     * says now (see {@link Writers#moved}), so lines arriving above push the page down with the words they were
     * reading. Putting words on a text field brings its cursor into view, and a cursor left at the end of a note took
     * the page to the bottom every time anything arrived: so once the words are laid out, the page is put back at the
     * line it was on, a swipe still carrying it is stopped, and nothing is scrolled to the cursor.
     */
    void replace(String said,Writers who) {
        String was=getText().toString();
        android.text.Layout out=getLayout();
        int top=0,into=0;
        if(out!=null) {
            int y=getScrollY(),line=out.getLineForVertical(Math.max(0,y-getTotalPaddingTop()));
            top=out.getLineStart(line);into=y-(out.getLineTop(line)+getTotalPaddingTop());
        }
        int start=Math.max(0,getSelectionStart()),end=Math.max(0,getSelectionEnd());
        int[] at=Writers.moved(was,said,top,start,end);
        if(!coasting.isFinished())coasting.forceFinished(true);
        put(said,who);
        int length=getText().length();
        setSelection(Math.min(at[1],length),Math.min(at[2],length));
        hold(at[0],into);
    }

    /** The page put back at a place once it has been laid out, after whatever the text field did on its own. */
    private void hold(final int offset,final int into) {
        final android.view.ViewTreeObserver seen=getViewTreeObserver();
        seen.addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                android.view.ViewTreeObserver now=getViewTreeObserver();
                if(now.isAlive())now.removeOnPreDrawListener(this);
                android.text.Layout out=getLayout();
                if(out==null)return true;
                int line=out.getLineForOffset(Math.max(0,Math.min(offset,getText().length())));
                scrollTo(getScrollX(),Math.max(0,Math.min(out.getLineTop(line)+getTotalPaddingTop()+into,furthest())));
                return true;
            }
        });
    }

    /**
     * Who wrote the words a note said, read once the page was already showing them: each letter still on the page
     * takes its writer, and anything typed since keeps being yours.
     */
    void follow(String said,Writers who) {
        if(who==null)return;
        String on=getText().toString();
        runs=on.equals(said)&&who.length()==on.length()?who.copy():Writers.follow(on,Writers.ME,said,who,on,runs);
        ink();
    }

    /** Who wrote what on the page now, to be written down with its words. */
    Writers writers(){return runs.copy();}

    /** The colours writers are drawn in, whether they are, and the paper they must read on. */
    void inks(Writers.Palette palette,boolean wanted,int paper) {
        this.palette=palette==null?Writers.Palette.plain():palette;this.wanted=wanted;this.paper=paper;
        ink();
    }

    /** Whether this page is drawn in its writers' colours now. */
    boolean inked(){return inked;}

    /** Every letter drawn again in its writer's colour, or all of them in the ordinary ink. */
    private void ink() {
        Editable text=getText();
        for(Ink was:text.getSpans(0,text.length(),Ink.class))text.removeSpan(was);
        inked=wanted&&palette.shows(runs);
        if(inked)paint(text,0,text.length());
    }

    /** The letters around an edit drawn again: an edit inside a colour would otherwise take the colour it landed in. */
    private void ink(int from,int to) {
        Editable text=getText();
        int start=Math.max(0,from-1), end=Math.min(text.length(),to+1);
        for(Ink was:text.getSpans(start,end,Ink.class)) {
            start=Math.min(start,text.getSpanStart(was));end=Math.max(end,text.getSpanEnd(was));
            text.removeSpan(was);
        }
        paint(text,start,end);
    }

    private void paint(Editable text,int from,int to) {
        int at=0;
        for(int run=0;run<runs.count()&&at<to;run++) {
            int end=at+runs.lengthOf(run);
            if(end>from) {
                int colour=palette.colourOf(runs.writerOf(run));
                if(Tint.known(colour))
                    text.setSpan(new Ink(Writers.ink(colour,paper)),Math.max(at,from),Math.min(end,to),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE|INK_FIRST);
            }
            at=end;
        }
    }

    /**
     * Copying and cutting take the words and nothing else. The page's colours are its own: pasted into a message, they
     * would be somebody's writing colour in a place that knows nothing of writers. Pasting in is the same: the words.
     */
    @Override public boolean onTextContextMenuItem(int id) {
        if(id==android.R.id.paste)return super.onTextContextMenuItem(android.R.id.pasteAsPlainText);
        if(id==android.R.id.copy||id==android.R.id.cut) {
            int from=Math.max(0,Math.min(getSelectionStart(),getSelectionEnd())), to=Math.max(getSelectionStart(),getSelectionEnd());
            ClipboardManager clip=(ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if(to>from&&clip!=null) {
                // From an ordinary page, never a private code with it, nor the rest of its line (decision 114).
                String words=getText().subSequence(from,to).toString();if(copiedPrivately==null)words=PrivateCode.scrub(words);
                // From a private page: marked sensitive, and said, so it can be taken off the clipboard when it closes.
                clip.setPrimaryClip(copiedPrivately!=null?PrivateScreen.sensitive(words):ClipData.newPlainText(null,words));
                if(copiedPrivately!=null)copiedPrivately.accept(words);
                if(id==android.R.id.cut)getText().delete(from,to);
                setSelection(id==android.R.id.cut?from:to);
                return true;
            }
        }
        return super.onTextContextMenuItem(id);
    }

    /** Set on a private page (decision 111): told what was copied from it. */
    java.util.function.Consumer<String> copiedPrivately;

    /** The rules follow the paper: a darker page needs a rule the writing can still be read against. */
    void rules(int colour){rule.setColor(colour);}

    /** Whether the page is ruled: a note can be plain paper on this device (its menu, Writing lines). */
    private boolean lined=true;
    void lined(boolean on){if(lined!=on){lined=on;invalidate();}}

    @Override protected void onDraw(Canvas canvas) {
        int height=getLineHeight();
        if(height>0&&lined) {
            // Ruled to the bottom of the page like a paper pad, not only under the lines already written.
            int baseline=getLineBounds(0,line)+drop;
            canvas.getClipBounds(clip);
            int top=Math.max(getScrollY(),clip.top),bottom=Math.min(getScrollY()+getHeight(),clip.bottom);
            // Keep the original rule alignment, but draw only what this frame can show, even at the end of a long note.
            int first=baseline+Math.max(0,(int)Math.ceil((top-baseline)/(double)height))*height;
            for(int y=first;y<bottom;y+=height)canvas.drawLine(0,y,getWidth(),y,rule);
        }
        super.onDraw(canvas);
    }
}
