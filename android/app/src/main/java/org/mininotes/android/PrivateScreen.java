// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Private notes and folders on the phone (the owner, 2026-10-06; decision 111): the codes caught as they are typed, the
 * screen of their own, and the rhythm that writes the private file the same for everybody (see {@link Slots}). The same
 * words and the same behaviour as the PC's (decision 105).
 *
 * <p>The screen lies over the whole window, above whatever was open, so nothing of it is ever among Home, search, Recent,
 * Favourites, the dock, Temp, the overview or a notification; it goes, and the app is as it was, on ‹ or the phone's
 * Back from its top, when the app is left (onStop), and after five minutes with no touch. While it is up the window is
 * secure (FLAG_SECURE): no screenshot, and nothing in the recent apps. Nothing about it is ever logged: whatever fails in
 * here is let go without a word.
 */
final class PrivateScreen {
    static final long IDLE=5*60_000L;
    static final int LONGEST=Shrink.LONGEST,QUALITY=Shrink.QUALITY;
    static final int ADD=40,SAVE=41;

    /** The file and the worker are the process's: the screen comes and goes with the activity, the rhythm does not. */
    private static volatile Slots slots;private static boolean started;
    private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mininotes-pages");t.setDaemon(true);return t;});
    static Slots slots(){try{worker.submit(()->null).get();}catch(Exception waiting){/* as it is */}return slots;}

    private final MainActivity a;
    private final Handler later=new Handler(Looper.getMainLooper());
    private final Map<Slots.Space,PrivateSpace> open=new IdentityHashMap<>();
    private PrivateSpace shown;
    private FrameLayout screen;
    private String folder="",note;private boolean inBin,full,picking,inFront;
    private long lastUse;
    private boolean copiedHere;private ClipboardManager.OnPrimaryClipChangedListener clipMoved;

    PrivateScreen(MainActivity a){this.a=a;}

    // ---- the rhythm ---------------------------------------------------------------------------------------------------

    private final Runnable tick=new Runnable(){public void run(){batch();later.postDelayed(this,Slots.EVERY);}};

    /** In front: the first time in this process the file is made or kept and a batch written; every time after, a batch; then one a minute. */
    void started() {
        inFront=true;picking=false;
        // The window is secure only while something private is on it (see close).
        if(screen==null)a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        final File file=new File(a.getFilesDir(),Slots.NAME);
        final Context app=a.getApplicationContext();
        worker.submit(()->{
            try {
                if(!started) {
                    byte[] secret=Keys.of(app).agreement().getPrivate().getEncoded();
                    Slots made=new Slots(file,Slots.maskFrom(secret));Arrays.fill(secret,(byte)0);
                    made.start();slots=made;started=true;
                } else if(slots!=null)slots.batch();
            } catch(Throwable quietly){/* nothing about it is said or kept anywhere */}
        });
        later.removeCallbacks(tick);later.postDelayed(tick,Slots.EVERY);
        later.removeCallbacks(idle);later.postDelayed(idle,30_000);
    }
    /** Left: everything private closes (unless it was this screen that asked for a picture), and one batch, as for anybody. */
    void stopped() {
        inFront=false;
        later.removeCallbacks(tick);
        if(!picking)close();
        batch();
    }
    private static void batch(){worker.submit(()->{try{Slots s=slots;if(s!=null)s.batch();}catch(Throwable quietly){/* the next one */}});}

    boolean showing(){return screen!=null;}
    void used(){lastUse=System.currentTimeMillis();}

    private final Runnable idle=new Runnable(){public void run(){
        if(screen!=null&&System.currentTimeMillis()-lastUse>IDLE)close();
        if(screen!=null||inFront)later.postDelayed(this,30_000);
    }};

    // ---- the codes ----------------------------------------------------------------------------------------------------

    /** What the field holds while the password field comes up: typed in the meantime, it is taken, not written. */
    private StringBuilder catching;

    /**
     * A field where a code can be typed: a note's page, the search, a private note. The moment its colon is typed the code
     * is taken out of the words, before anything can write them down, and a password field takes the keyboard; Enter
     * opens. {@code typing} says whether a change is typed (not a note being drawn); {@code opened} is told when something
     * opened, so a box the field is in can go.
     */
    void watch(final EditText field,final java.util.function.BooleanSupplier typing,final Runnable opened) {
        InputFilter[] had=field.getFilters();InputFilter[] now=Arrays.copyOf(had,had.length+1);
        now[had.length]=(source,start,end,dest,dstart,dend)->{
            if(catching==null||catchingFor!=field)return null;
            // A keyboard that hands its word over again, code and all, gives only what follows the code (decision 114).
            PrivateCode.more(catching,source.subSequence(start,end));return "";
        };
        field.setFilters(now);
        field.addTextChangedListener(new TextWatcher() {
            int from=-1,end=-1;
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            // A keyboard that writes a word while it is typed hands over the whole word again at every letter, and a
            // colon may come with the word it ends (found on the Graphene's own keyboard, 2026-10-06: the code stayed in
            // the note). So: whatever this change put in, short of a paste, is looked through for a code it finished.
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){if(count>0&&count<=48){from=start;end=start+count;}else{from=-1;end=-1;}}
            @Override public void afterTextChanged(Editable e) {
                int first=from,last=end;from=-1;end=-1;
                if(last<0||!typing.getAsBoolean())return;
                // What came after the colon in the same change (a paste, a word handed over with its password) is the
                // password: taken with the code, so none of it stays in the note (decision 114), and kept for its field.
                PrivateCode.Caught found=PrivateCode.finished(e,first,last);
                if(found==null)return;
                // The keyboard's own copy of the word is dropped with it, or it writes the code back at the next letter.
                android.view.inputmethod.BaseInputConnection.removeComposingSpans(e);
                e.delete(found.start,found.stop);
                android.view.inputmethod.InputMethodManager keys=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(keys!=null)keys.restartInput(field);
                catching=new StringBuilder(found.rest);if(found.entered)catching.append('\n');catchingFor=field;
                final PrivateCode.Kind kind=found.kind;
                field.post(()->ask(field,kind,opened));
            }
        });
    }
    private EditText catchingFor;

    /** The password, in a field of its own over where the code was: the keyboard told it is one, so it neither shows nor learns it. */
    private void ask(final EditText from,final PrivateCode.Kind kind,final Runnable opened) {
        String already=catching==null?"":catching.toString();catching=null;catchingFor=null;
        int enter=already.indexOf('\n');
        if(enter>=0){go(kind,already.substring(0,enter).toCharArray(),opened);return;}
        password("Password, then Enter",already,from,(said,box)->go(kind,said,opened,box),()->{});
    }

    /**
     * The box the password is typed in, kept up from the first key until it is decided: it says Opening… while the
     * password is tried, and asks The same again in the same place when a new space is to be made. It used to close at
     * Enter and come back a second later, and in that second the keyboard was the note's again: what was typed next,
     * the password the second time, went into the note, to be kept and sent with it (found on the Graphene, 2026-10-06).
     */
    interface Box {
        /** Shown, waiting: the field emptied and greyed with these words in it. */
        void waiting(String words);
        /** Asks again in the same field; Enter hands the next words to {@code enter}. */
        void again(String hint,java.util.function.BiConsumer<char[],Box> enter);
        /** Gone, and nothing more asked. */
        void close();
    }

    /** A password field of its own, for a moment, under {@code from} or in the middle: Enter hands over what was typed. */
    private void password(String hint,String already,View from,final java.util.function.BiConsumer<char[],Box> enter,final Runnable away) {
        final EditText field=new EditText(a);
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setImeOptions(EditorInfo.IME_ACTION_GO|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        field.setHint(hint);field.setHintTextColor(a.MUTED);field.setTextColor(a.INK);field.setSingleLine(true);
        field.setText(already);field.setSelection(already.length());
        // The card holds the field and an eye at its right that shows the characters while it is held (decision 116).
        GradientDrawable box=new GradientDrawable();box.setColor(a.CARD);box.setCornerRadius(a.dp(12));box.setStroke(Math.max(1,a.dp(1)),a.LINE);
        field.setBackground(null);field.setPadding(a.dp(14),a.dp(12),a.dp(48),a.dp(12));
        FrameLayout wrap=new FrameLayout(a);wrap.setBackground(box);
        wrap.addView(field,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
        FrameLayout.LayoutParams eyeAt=new FrameLayout.LayoutParams(a.dp(40),a.dp(40),Gravity.END|Gravity.CENTER_VERTICAL);eyeAt.setMarginEnd(a.dp(4));
        wrap.addView(eye(field),eyeAt);
        final PopupWindow popup=new PopupWindow(wrap,Math.min(a.getResources().getDisplayMetrics().widthPixels-a.dp(32),a.dp(320)),ViewGroup.LayoutParams.WRAP_CONTENT,true);
        popup.setElevation(a.dp(8));popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);popup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        final boolean[] done={false};
        @SuppressWarnings("unchecked") final java.util.function.BiConsumer<char[],Box>[] next=new java.util.function.BiConsumer[]{enter};
        final Box[] self={null};final boolean[] held={false};
        field.setFilters(new InputFilter[]{(source,start,end,dest,dstart,dend)->held[0]?"":null});
        self[0]=new Box(){
            // Waiting, the field keeps the keyboard and swallows what is typed: greyed out, it let the keyboard go back to
            // the note, and keys typed while the password was tried went into the note (seen on the Graphene, 2026-10-06).
            // No blinking caret while it says Opening, so it does not look as if it wants typing (decision 116).
            public void waiting(String words){held[0]=true;field.setText("");field.setCursorVisible(false);field.setHint(words);}
            public void again(String asked,java.util.function.BiConsumer<char[],Box> then){next[0]=then;held[0]=false;field.setText("");field.setCursorVisible(true);field.setHint(asked);field.requestFocus();}
            public void close(){done[0]=true;field.setText("");if(popup.isShowing())popup.dismiss();}
        };
        popup.setOnDismissListener(()->{if(!done[0]){done[0]=true;field.setText("");away.run();}});
        field.setOnEditorActionListener((v,action,event)->{
            boolean enterKey=event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_DOWN;
            if(action!=EditorInfo.IME_ACTION_GO&&action!=EditorInfo.IME_ACTION_DONE&&!enterKey)return false;
            if(held[0])return true;
            Editable said=field.getText();char[] typed=new char[said.length()];said.getChars(0,said.length(),typed,0);
            field.setText("");next[0].accept(typed,self[0]);return true;
        });
        try {
            if(from!=null&&from.isAttachedToWindow())popup.showAsDropDown(from,a.dp(16),-from.getHeight()/2);
            else popup.showAtLocation(a.getWindow().getDecorView(),Gravity.CENTER,0,0);
        } catch(RuntimeException noWindow){done[0]=true;away.run();return;}
        field.requestFocus();
        android.view.inputmethod.InputMethodManager keys=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
        // The keyboard asked for, and asked again a moment later: the note's keyboard was just restarted to drop its copy
        // of the code, which closes it, and a box with no keyboard under it was a box nobody could type in (seen on the
        // Graphene, 2026-10-06). As a folder's name does (MainActivity's renaming).
        if(keys!=null)field.post(()->keys.showSoftInput(field,0));
        if(android.os.Build.VERSION.SDK_INT>=30)field.post(()->{android.view.WindowInsetsController asking=field.getWindowInsetsController();if(asking!=null)asking.show(android.view.WindowInsets.Type.ime());});
        field.postDelayed(()->{if(field.isAttachedToWindow()&&keys!=null){field.requestFocus();keys.showSoftInput(field,0);}},250);
    }

    /**
     * What a password opens, shown. Nothing answering: for privatespace//:, the password is asked once more, and only the
     * same twice makes a new, independent space (the owner, 2026-10-06; decision 116), so a typo makes nothing and writes
     * nothing; for private//:, nothing happens and nothing is said, as for any wrong password.
     */
    void go(final PrivateCode.Kind kind,final char[] password,final Runnable opened){go(kind,password,opened,null);}
    void go(final PrivateCode.Kind kind,final char[] password,final Runnable opened,final Box box) {
        if(password.length==0){if(box!=null)box.close();return;}
        if(box!=null)box.waiting("Opening\u2026");
        worker.submit(()->{
            boolean keep=false;
            try {
                Slots s=slots;if(s==null)return;
                Slots.Space space=s.open(password);
                if(space==null) {
                    if(kind==PrivateCode.Kind.OPEN){if(box!=null)later.post(box::close);return;}
                    keep=true;
                    later.post(()->{
                        if(a.isFinishing()||!inFront){Arrays.fill(password,'\0');if(box!=null)box.close();return;}
                        java.util.function.BiConsumer<char[],Box> then=(again,same)->{boolean match=Arrays.equals(again,password);Arrays.fill(again,'\0');same.close();
                            if(match)make(password,opened);else Arrays.fill(password,'\0');};
                        if(box!=null)box.again("The same again, then Enter",then);
                        else password("The same again, then Enter","",null,then,()->Arrays.fill(password,'\0'));
                    });
                    return;
                }
                if(box!=null)later.post(box::close);
                showLater(space,opened);
            } catch(Throwable quietly){/* nothing said: as a password nothing answers to */}
            finally{if(!keep)Arrays.fill(password,'\0');}
        });
    }
    /** A new, independent space from outside (privatespace//:, the password twice; decision 116): an empty folder with the + ready. */
    private void make(final char[] password,final Runnable opened) {
        worker.submit(()->{
            try {
                Slots s=slots;if(s==null)return;
                Slots.Space space=s.make(password,PrivateSpace.empty());
                if(space!=null)showLater(space,opened);
            } catch(Throwable quietly){/* nothing said */}
            finally{Arrays.fill(password,'\0');}
        });
    }
    private void showLater(final Slots.Space got,final Runnable opened) {
        later.post(()->{
            try {
                if(a.isFinishing()||!inFront)return;
                PrivateSpace model=open.get(got);
                if(model==null){model=PrivateSpace.of(got);open.put(got,model);}
                if(opened!=null)opened.run();
                show(model);
            } catch(Throwable quietly){/* nothing said */}
        });
    }

    // ---- the screen ---------------------------------------------------------------------------------------------------

    private TextView heading,back,says;private FrameLayout middle;private View plus;private int ground;
    private Pad page;private LinearLayout files;private boolean loading;
    /** The one menu panel open over the screen, if any: drawn in here, not on Home, so it stays in the secure overlay. */
    private Panel panel;
    private final Runnable keep=this::write;

    /** A space always opens on its grid (folder mode), never a note, a brand-new one as an empty folder (decision 116). */
    private void show(PrivateSpace p) {
        if(screen==null)build();
        leaveNote();
        shown=p;inBin=false;folder="";note=null;full=false;
        used();draw();
    }

    private void build() {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        screen=new FrameLayout(a) {
            @Override public boolean dispatchTouchEvent(MotionEvent e){used();return super.dispatchTouchEvent(e);}
        };
        // Its own ground from edge to edge, and a lock beside Private, so it is never taken for Home (decision 114).
        ground=a.darkPaper()?PrivateSpace.GROUND_DARK:PrivateSpace.GROUND;
        screen.setBackgroundColor(ground);screen.setClickable(true);screen.setFocusable(true);
        LinearLayout column=a.column();
        LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(a.dp(8),a.dp(6),a.dp(8),a.dp(6));
        // Always there: ‹ goes up a level, and at the top it closes, as the phone's Back does. No Close (the owner,
        // 2026-10-06: "we don't need the close, the back button should be enough"; decision 114).
        back=a.label("‹",MainActivity.READING+8,a.INK);back.setPadding(a.dp(12),a.dp(4),a.dp(12),a.dp(4));back.setContentDescription("Back");
        back.setBackgroundResource(a.borderlessFeedback());back.setOnClickListener(v->back());
        top.addView(back);
        LinearLayout.LayoutParams locked=new LinearLayout.LayoutParams(a.dp(18),a.dp(22));locked.setMargins(a.dp(4),0,0,0);
        top.addView(new Lock(a).in(a.darkPaper()?PrivateSpace.LOCK_DARK:PrivateSpace.LOCK),locked);
        heading=a.label("Private",MainActivity.READING+4,a.INK);heading.setTypeface(null,android.graphics.Typeface.BOLD);
        heading.setSingleLine(true);heading.setEllipsize(TextUtils.TruncateAt.END);heading.setPadding(a.dp(8),0,a.dp(8),0);
        top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=round("⋮","More",this::moreMenu);
        top.addView(more);
        column.addView(top,new LinearLayout.LayoutParams(-1,-2));
        // The same + as Home's, in the same place, the foot on the right where the thumb is (the owner, 2026-10-06: "let's
        // keep the same + at the bottom"; decision 114), over whatever is shown, the bin aside.
        FrameLayout room=new FrameLayout(a);
        middle=new FrameLayout(a);room.addView(middle,new FrameLayout.LayoutParams(-1,-1));
        plus=plus();
        FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(a.dp(56),a.dp(56),Gravity.BOTTOM|Gravity.END);corner.setMargins(0,0,a.dp(18),a.dp(16));
        room.addView(plus,corner);
        column.addView(room,new LinearLayout.LayoutParams(-1,0,1));
        says=a.label("",MainActivity.QUIET,a.MUTED);says.setPadding(a.dp(20),a.dp(6),a.dp(20),a.dp(10));
        column.addView(says,new LinearLayout.LayoutParams(-1,-2));
        screen.addView(column,new FrameLayout.LayoutParams(-1,-1));
        screen.setOnApplyWindowInsetsListener((v,insets)->{
            int topInset,bottom;
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                topInset=bars.top;bottom=Math.max(bars.bottom,insets.getInsets(WindowInsets.Type.ime()).bottom);
            } else {topInset=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}
            column.setPadding(0,topInset,0,bottom);
            return insets;
        });
        ((ViewGroup)a.getWindow().getDecorView()).addView(screen,new FrameLayout.LayoutParams(-1,-1));
        screen.requestApplyInsets();
        later.removeCallbacks(idle);later.postDelayed(idle,30_000);
    }
    /** Home's + (see HomeScreen.plus): the same round, the same colour, the same size, a menu in Home's words. */
    private View plus() {
        TextView p=a.label("+",26,a.PAPER);
        p.setGravity(Gravity.CENTER);p.setIncludeFontPadding(false);
        GradientDrawable disc=new GradientDrawable();disc.setShape(GradientDrawable.OVAL);disc.setColor(a.ACCENT);
        p.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x40FFFFFF),disc,null));
        p.setElevation(a.dp(6));p.setContentDescription("New");
        p.setOnClickListener(v->plusMenu(v));
        return p;
    }

    /** The lock beside Private: drawn, not a picture or an emoji, so it is the same shape on every phone (decision 114). */
    static final class Lock extends View {
        private final android.graphics.Paint pen=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        Lock(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        Lock in(int colour){pen.setColor(colour);invalidate();return this;}
        @Override protected void onDraw(android.graphics.Canvas g) {
            float w=getWidth(),h=getHeight(),side=Math.min(w,h*0.82f),x=(w-side)/2,body=h*0.52f,top=h-body;
            pen.setStyle(android.graphics.Paint.Style.STROKE);pen.setStrokeWidth(side*0.16f);pen.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            float arm=side*0.30f,cx=w/2;
            g.drawArc(cx-arm,h*0.08f,cx+arm,h*0.08f+2*arm,180,180,false,pen);
            g.drawLine(cx-arm,h*0.08f+arm,cx-arm,top+1,pen);g.drawLine(cx+arm,h*0.08f+arm,cx+arm,top+1,pen);
            pen.setStyle(android.graphics.Paint.Style.FILL);
            g.drawRoundRect(x,top,x+side,h,side*0.18f,side*0.18f,pen);
        }
    }

    /** The eye on a password field, drawn like the lock so it is one shape everywhere (decision 116): an almond and a pupil, a slash over it while the characters are masked. */
    static final class Eye extends View {
        private final android.graphics.Paint pen=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private boolean open;
        Eye(Context c,int colour){super(c);pen.setColor(colour);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void open(boolean showing){open=showing;invalidate();}
        @Override protected void onDraw(android.graphics.Canvas g) {
            float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,rx=Math.min(w,h)*0.34f,ry=rx*0.62f;
            pen.setStyle(android.graphics.Paint.Style.STROKE);pen.setStrokeWidth(rx*0.22f);pen.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            android.graphics.Path almond=new android.graphics.Path();
            almond.moveTo(cx-rx,cy);almond.quadTo(cx,cy-ry*1.9f,cx+rx,cy);almond.quadTo(cx,cy+ry*1.9f,cx-rx,cy);almond.close();
            g.drawPath(almond,pen);
            pen.setStyle(android.graphics.Paint.Style.FILL);g.drawCircle(cx,cy,rx*0.34f,pen);
            if(!open){pen.setStyle(android.graphics.Paint.Style.STROKE);g.drawLine(cx-rx*0.9f,cy-ry*1.1f,cx+rx*0.9f,cy+ry*1.1f,pen);}
        }
    }

    private TextView round(String sign,String says,View.OnClickListener does) {
        TextView t=a.label(sign,MainActivity.READING+6,a.INK);t.setGravity(Gravity.CENTER);t.setContentDescription(says);
        t.setMinWidth(a.dp(48));t.setMinHeight(a.dp(48));t.setBackgroundResource(a.borderlessFeedback());t.setOnClickListener(does);
        return t;
    }

    /** Everything drawn again from what is open: the heading, the grid, the note, the bin, the line at the foot. */
    private void draw() {
        if(screen==null||shown==null)return;
        if(panel!=null)panel.close();
        PrivateSpace.Thing in=folder.isEmpty()?null:shown.thing(folder);
        if(in==null)folder="";
        if(note!=null&&shown.thing(note)==null)note=null;
        heading.setText(inBin?"Bin":note!=null?"Private":in!=null?in.name():"Private");
        plus.setVisibility(inBin?View.GONE:View.VISIBLE);
        if(inBin)middle(binView());
        else if(note!=null)middle(noteView());
        else middle(gridView());
        tick();
    }
    private void middle(View shown){middle.removeAllViews();middle.addView(shown,new FrameLayout.LayoutParams(-1,-1));}

    private View gridView() {
        ScrollView scroll=new ScrollView(a);
        int width=a.getResources().getDisplayMetrics().widthPixels;
        int columns=Math.max(3,Math.min(6,width/a.dp(96)));int cell=(width-a.dp(16))/columns;
        // Room under the last row for the + (56 and its 16 under it), as Home keeps.
        GridLayout grid=new GridLayout(a);grid.setColumnCount(columns);grid.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(88));
        List<PrivateSpace.Thing> things=shown.in(folder);
        if(things.isEmpty()) {
            TextView none=a.label(folder.isEmpty()?"Nothing private yet. Make a note or a folder with the +.":"Nothing in this folder yet.",MainActivity.QUIET,a.MUTED);
            none.setPadding(a.dp(16),a.dp(16),a.dp(16),a.dp(16));scroll.addView(none);return scroll;
        }
        for(PrivateSpace.Thing t:things)grid.addView(tile(t,cell));
        scroll.addView(grid);
        return scroll;
    }
    /** One private thing as Home draws its icons: its face, and its name under it. */
    private View tile(final PrivateSpace.Thing t,int cell) {
        LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(6));
        GridLayout.LayoutParams place=new GridLayout.LayoutParams();place.width=cell;place.height=GridLayout.LayoutParams.WRAP_CONTENT;tile.setLayoutParams(place);
        int inside=t.folder?shown.in(t.id).size():0;
        NoteStore.Branch face=new NoteStore.Branch(t.folder?NoteStore.Branch.Kind.COLLECTION:NoteStore.Branch.Kind.PAGE,t.id,t.parent,t.name(),
            t.folder?(inside==1?"1 thing":inside+" things"):"",0,0,t.folder,t.colour);
        face.tone=t.tone;
        int side=Math.max(a.dp(40),Math.min(a.dp(72),cell-a.dp(28)));
        tile.addView(IconFace.view(a,face,side),new LinearLayout.LayoutParams(side,side));
        TextView name=a.label(t.name(),MainActivity.READING,a.INK);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0,a.dp(6),0,0);tile.addView(name,new LinearLayout.LayoutParams(-1,-2));
        tile.setBackgroundResource(a.touchFeedback());
        tile.setContentDescription((t.folder?"Folder ":"Note ")+t.name());
        tile.setOnClickListener(v->openThing(t));
        tile.setOnLongClickListener(v->{thingMenu(t,v);return true;});
        return tile;
    }

    private View noteView() {
        final PrivateSpace.Thing t=shown.thing(note);
        LinearLayout view=a.column();
        // Washed over the private ground, so a note with no colour is still on the private screen's own (decision 114).
        view.setBackgroundColor(Tint.over(t.colour,ground,a.wash(t.tone,0.12f,0.72f),a.darkPaper()));
        // A band at the foot for the +, under the page and its files.
        view.setPadding(0,0,0,a.dp(72));
        page=new Pad(a,Tint.known(t.colour)?Tint.over(t.colour,a.LINE,a.wash(t.tone,0.35f,1f),a.darkPaper()):a.LINE);
        page.setGravity(Gravity.TOP);page.setTextColor(a.INK);page.setTextSize(17);page.setLineSpacing(a.dp(6),1f);
        page.setPadding(0,a.dp(2),0,a.dp(24));page.setHint("Write.");page.setHintTextColor(a.MUTED);page.setContentDescription("Private note");
        page.setImeOptions(page.getImeOptions()|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        page.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        page.copiedPrivately=this::noteCopied;
        loading=true;
        try{page.setText(t.title.isEmpty()?t.body:t.title+"\n"+t.body);}finally{loading=false;}
        page.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable e) {
                if(loading||shown==null||note==null)return;
                used();
                // Known by its first line, as every note on the phone is: the words are kept whole, the title empty.
                shown.text(note,"",e.toString(),System.currentTimeMillis());
                later.removeCallbacks(keep);later.postDelayed(keep,700);
            }
        });
        watch(page,()->!loading,null);
        LinearLayout.LayoutParams sheet=new LinearLayout.LayoutParams(-1,0,1);sheet.setMargins(a.dp(20),a.dp(8),a.dp(20),a.dp(8));
        view.addView(page,sheet);
        files=new LinearLayout(a);files.setPadding(a.dp(16),a.dp(4),a.dp(16),a.dp(8));
        HorizontalScrollView strip=new HorizontalScrollView(a);strip.addView(files);
        view.addView(strip,new LinearLayout.LayoutParams(-1,-2));
        drawFiles();
        if(t.body.isEmpty()&&t.title.isEmpty())page.post(()->{page.requestFocus();
            android.view.inputmethod.InputMethodManager keys=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keys!=null)keys.showSoftInput(page,0);});
        return view;
    }
    private void drawFiles() {
        if(files==null||note==null)return;
        files.removeAllViews();
        for(final PrivateSpace.Kept k:shown.filesOf(note)) {
            LinearLayout chip=new LinearLayout(a);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setPadding(a.dp(10),a.dp(8),a.dp(14),a.dp(8));
            GradientDrawable box=new GradientDrawable();box.setColor(a.CARD);box.setCornerRadius(a.dp(12));box.setStroke(Math.max(1,a.dp(1)),a.LINE);chip.setBackground(box);
            if(k.picture) {
                Bitmap small=decode(shown.bytesOf(k.id),a.dp(32));
                if(small!=null){ImageView face=new ImageView(a);face.setImageBitmap(small);face.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(a.dp(32),a.dp(32));at.setMargins(0,0,a.dp(8),0);chip.addView(face,at);}
            }
            chip.addView(a.label(k.name+"  ·  "+size(k.bytes),MainActivity.QUIET,a.INK));
            chip.setContentDescription(k.name);
            chip.setOnClickListener(v->{if(k.picture)view(k);else fileMenu(k);});
            chip.setOnLongClickListener(v->{fileMenu(k);return true;});
            LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-2,-2);gap.setMargins(0,0,a.dp(8),0);
            files.addView(chip,gap);
        }
    }

    /**
     * The bin, binned things drawn as the app's own tiles rather than text rows (the owner, 2026-10-07: "these are elements,
     * they look like simple text ... the bin must look better"; decision 117): the same face, colour and name a tile has
     * everywhere, each with Put back / Delete for good, and Empty the bin as the one primary action at the top.
     */
    private View binView() {
        ScrollView scroll=new ScrollView(a);
        LinearLayout column=a.column();
        List<PrivateSpace.Thing> away=shown.binned();
        if(away.isEmpty()) {
            TextView none=a.label("Nothing is in the bin.",MainActivity.QUIET,a.MUTED);
            none.setPadding(a.dp(20),a.dp(16),a.dp(20),a.dp(16));column.addView(none);scroll.addView(column);return scroll;
        }
        // One primary action, styled as the app's own: emptying the whole bin, after a question in the colour of what cannot be had back.
        View empty=a.primary("Empty the bin…",()->secure(a.new Box().setTitle("Empty the bin?")
            .setMessage("Everything in the bin is deleted for good. This cannot be undone.")
            .setPositiveButton("Empty the bin",(d,w)->{shown.emptyBin();write();draw();}).setNegativeButton("Cancel",null).show()));
        LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-1,-2);at.setMargins(a.dp(20),a.dp(8),a.dp(20),a.dp(4));empty.setLayoutParams(at);
        column.addView(empty);
        int width=a.getResources().getDisplayMetrics().widthPixels;
        int columns=Math.max(3,Math.min(6,width/a.dp(96)));int cell=(width-a.dp(16))/columns;
        GridLayout grid=new GridLayout(a);grid.setColumnCount(columns);grid.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(24));
        for(PrivateSpace.Thing t:away)grid.addView(binTile(t,cell));
        column.addView(grid);
        scroll.addView(column);return scroll;
    }
    /** A binned thing as a tile, its menu offering Put back and Delete for good (decision 117). */
    private View binTile(final PrivateSpace.Thing t,int cell) {
        View tile=tile(t,cell);
        tile.setContentDescription((t.folder?"Folder ":"Note ")+t.name()+". In the bin.");
        tile.setOnClickListener(v->binItemMenu(t,v));
        tile.setOnLongClickListener(v->{binItemMenu(t,v);return true;});
        return tile;
    }
    private void binItemMenu(final PrivateSpace.Thing t,View from) {
        Panel menu=new Panel();
        menu.name(t.name());
        menu.row("Put back",()->{shown.putBack(t.id);write();draw();});
        menu.line();
        menu.danger("Delete for good…",()->secure(a.new Box().setTitle("Delete for good?")
            .setMessage("“"+t.name()+"” is deleted for good. This cannot be undone.")
            .setPositiveButton("Delete for good",(d,w)->{shown.deleteForGood(t.id);write();draw();}).setNegativeButton("Cancel",null).show()));
        menu.show(from);
    }

    /** The line at the foot: full, or how much still waits to be written, or how much is used. Said here only. */
    private void tick() {
        if(screen==null||shown==null)return;
        // In minutes, never bytes, the same words as the PC's (decision 114).
        int left=shown.space.batchesLeft();
        says.setTextColor(full?a.WARN:a.MUTED);
        says.setText(shown.saying(full));
        later.removeCallbacks(again);if(left>0)later.postDelayed(again,2000);
    }
    private final Runnable again=this::tick;
    private static String size(long bytes){return bytes<1024?bytes+" bytes":bytes<1024*1024?(bytes/1024)+" KB":String.format(java.util.Locale.ROOT,"%.1f MB",bytes/1048576.0);}

    private void write() {
        if(shown==null)return;
        try{shown.write();full=false;}catch(Slots.Full f){full=true;}catch(Throwable quietly){/* nothing said */}
        tick();
    }
    private void leaveNote() {
        if(shown!=null&&note!=null){later.removeCallbacks(keep);shown.keepVersion(note,System.currentTimeMillis());write();}
        page=null;files=null;
    }
    /** Back: a note to its folder, a folder up, the bin to the top; at the top it closes. True where it did something. */
    boolean back() {
        if(screen==null)return false;
        if(panel!=null){panel.close();return true;}
        if(note!=null||!folder.isEmpty()||inBin)up();else close();
        return true;
    }
    private void up() {
        if(shown==null)return;
        if(inBin){inBin=false;draw();return;}
        if(note!=null){leaveNote();note=null;draw();return;}
        PrivateSpace.Thing in=shown.thing(folder);folder=in==null?"":in.parent;draw();
    }
    private void openThing(PrivateSpace.Thing t) {
        leaveNote();
        if(t.folder){folder=t.id;note=null;}else{note=t.id;folder=t.parent;}
        draw();
        // A help request, where this private thing is set to send one when it opens (decision 113): once per opening, here
        // where a thing is opened, not on every redraw. The setting lives in the vault, so nothing outside it ever showed it.
        if(t.helpOnOpen&&!t.helpTo.isEmpty()&&a.helpAlarm!=null)a.helpAlarm.raise(t.helpTo,t.helpMessage);
    }

    // ---- menus --------------------------------------------------------------------------------------------------------

    /** A short list to choose from, in a box of the app's own: secure like the screen it is over. */
    private void choose(String title,String[] rows,java.util.function.IntConsumer picked) {
        secure(a.new Box().setTitle(title).setItems(rows,(d,which)->picked.accept(which)).show());
    }
    private static AlertDialog secure(AlertDialog box) {
        if(box!=null&&box.getWindow()!=null)box.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        return box;
    }

    /**
     * A menu panel in the private screen's own look, the same vocabulary as the app's own menu (the Sheet): a rounded sheet
     * of {@link MainActivity#SHEET} over a faint scrim, small capital headings, grouped rows with hairline dividers, and the
     * colours drawn as the colours themselves with the one it has ringed. Added to {@link #screen}, not to Home, so it stays
     * inside the secure overlay (decision 117); the ordinary Sheet draws itself onto Home, which is behind here.
     */
    private final class Panel {
        private final LinearLayout body=a.column();
        private final ScrollView holder=new ScrollView(a);
        private final FrameLayout scrim=new FrameLayout(a);
        Panel() {
            GradientDrawable card=new GradientDrawable();card.setColor(a.SHEET);card.setCornerRadius(a.dp(14));
            holder.setBackground(card);holder.setElevation(a.dp(12));
            holder.addView(body,new FrameLayout.LayoutParams(-1,-2));
            scrim.setBackgroundColor(0x22000000);scrim.setClickable(true);scrim.setOnClickListener(v->close());
        }
        /** A section's name: small, quiet, in capitals, not something to tap, as the app's menu has it. */
        void heading(String words) {
            TextView head=a.label(words.toUpperCase(java.util.Locale.ROOT),MainActivity.QUIET,a.MUTED);
            head.setTypeface(null,android.graphics.Typeface.BOLD);head.setLetterSpacing(0.06f);
            head.setPadding(a.dp(20),a.dp(12),a.dp(20),a.dp(2));
            body.addView(head,new LinearLayout.LayoutParams(-1,-2));
        }
        /** The thing this menu is about, named at the top without shouting it in capitals (a bin tile, a long-pressed thing). */
        void name(String words) {
            TextView t=a.label(words,MainActivity.READING,a.MUTED);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(a.dp(20),a.dp(12),a.dp(20),a.dp(2));
            body.addView(t,new LinearLayout.LayoutParams(-1,-2));
        }
        void row(String words,Runnable does){row(words,does,a.INK);}
        /** A row for something that cannot be taken back, in the warning colour, as the app marks its own. */
        void danger(String words,Runnable does){row(words,does,a.WARN);}
        private void row(String words,Runnable does,int ink) {
            TextView row=a.label(words,MainActivity.READING,ink);
            row.setPadding(a.dp(20),a.dp(14),a.dp(20),a.dp(14));row.setBackgroundResource(a.touchFeedback());
            row.setOnClickListener(v->{close();does.run();});
            body.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        void line() {
            View rule=new View(a);rule.setBackgroundColor(a.LINE);
            LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-1,Math.max(1,a.dp(1)));at.setMargins(0,a.dp(6),0,a.dp(6));
            body.addView(rule,at);
        }
        /** The colours one thing can be given, drawn as the colours themselves with the one it has ringed (as the Sheet's palette). */
        void palette(final PrivateSpace.Thing t) {
            LinearLayout colours=new LinearLayout(a);colours.setGravity(Gravity.CENTER_VERTICAL);colours.setPadding(a.dp(10),a.dp(6),a.dp(10),a.dp(6));
            for(int c=0;c<Tint.count();c++) {
                final int which=c;
                FrameLayout reach=new FrameLayout(a);reach.setPadding(a.dp(2),a.dp(9),a.dp(2),a.dp(9));
                View blob=new View(a);
                GradientDrawable ring=new GradientDrawable();ring.setShape(GradientDrawable.OVAL);
                ring.setColor(Tint.known(c)?Tint.of(c,a.darkPaper()):a.PAPER);
                ring.setStroke(a.dp(c==t.colour?3:1),c==t.colour?a.INK:a.LINE);
                blob.setBackground(ring);
                reach.addView(blob,new FrameLayout.LayoutParams(a.dp(20),a.dp(20),Gravity.CENTER));
                reach.setContentDescription(Tint.NAMES[c]+" for "+t.name());
                reach.setOnClickListener(v->{close();shown.colour(t.id,which);write();draw();});
                colours.addView(reach,new LinearLayout.LayoutParams(0,-2,1));
            }
            body.addView(colours,new LinearLayout.LayoutParams(-1,-2));
        }
        void show(View anchor) {
            if(panel!=null)panel.close();
            panel=this;
            android.view.inputmethod.InputMethodManager keys=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keys!=null&&screen!=null)keys.hideSoftInputFromWindow(screen.getWindowToken(),0);
            int width=Math.min(a.dp(300),a.getResources().getDisplayMetrics().widthPixels-a.dp(32));
            final FrameLayout.LayoutParams at=new FrameLayout.LayoutParams(width,-2,Gravity.TOP|Gravity.END);
            at.setMargins(0,topFor(anchor),a.dp(8),a.dp(8));
            scrim.addView(holder,at);
            screen.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
            // Capped to the room there is, so a long menu scrolls rather than running off the bottom.
            holder.post(()->{
                if(scrim.getParent()==null)return;
                int room=screen.getHeight()-at.topMargin-a.dp(16);
                if(room>0&&holder.getHeight()>room){at.height=room;holder.setLayoutParams(at);}
            });
        }
        private int topFor(View anchor) {
            if(anchor==null||screen==null)return a.dp(64);
            int[] here=new int[2],mine=new int[2];anchor.getLocationOnScreen(here);screen.getLocationOnScreen(mine);
            return Math.max(a.dp(8),here[1]-mine[1]+anchor.getHeight());
        }
        void close(){if(panel==this)panel=null;if(scrim.getParent()!=null)((ViewGroup)scrim.getParent()).removeView(scrim);}
    }

    /** Home's menu, in Home's words: a note, a folder, and, in a note, a file from this device into it. */
    private void plusMenu(View from) {
        if(shown==null)return;
        Runnable newNote=()->{leaveNote();PrivateSpace.Thing t=shown.newNote(folder,System.currentTimeMillis());write();openThing(t);};
        Runnable newFolder=()->named("New private folder","",name->{shown.newFolder(folder,name,System.currentTimeMillis());write();draw();});
        // The + offers the same three at every level of the private space now, as Home's does (decision 115): from the top or
        // a folder a file becomes its own private note named after it; in a note it is added to that note.
        a.heldMenu(from,null,"Note",newNote,"Folder",newFolder,"From this device…",(Runnable)this::addFiles);
    }
    /**
     * The ⋮ menu, grouped and drawn like the app's own menu rather than a plain list (the owner, 2026-10-07: "the menu
     * there we should have a delete option ... not at the level of the general menu we have in the app"; decision 117): on a
     * note its colours and the note's own rows, as an ordinary note's menu reads, then the space's own rows (Bin, Change
     * password, and Delete this private space) last, under their own heading. Built in the private screen's own panel so it
     * stays inside the secure overlay (the Sheet draws itself onto Home, which is behind here).
     */
    private void moreMenu(View from) {
        if(shown==null)return;
        final PrivateSpace.Thing t=note==null?null:shown.thing(note);
        Panel menu=new Panel();
        if(t!=null) {
            menu.palette(t);
            menu.line();
            menu.heading("This note");
            menu.row("Versions",()->versions(t));
            menu.row("Add a photo or file",this::addFiles);
            menu.row("Move to…",()->moveTo(t));
            menu.row(Help.SWITCH,()->privateHelp(t));
            menu.danger("Delete",()->{leaveNote();shown.bin(t.id,System.currentTimeMillis());note=null;write();draw();});
            menu.line();
        }
        menu.heading("Private space");
        menu.row("Bin",()->{leaveNote();note=null;inBin=true;draw();});
        menu.row("Change password…",this::changePassword);
        menu.line();
        menu.danger("Delete this private space…",this::deleteSpace);
        menu.show(from);
    }
    /** A thing's long-press menu, grouped like the app's own thing menu (decision 117): Open, its colours, Move to, help, Delete. */
    private void thingMenu(final PrivateSpace.Thing t,View from) {
        Panel menu=new Panel();
        menu.name(t.name());
        menu.row("Open",()->openThing(t));
        if(t.folder)menu.row("Rename…",()->named("Rename folder",t.title,name->{shown.rename(t.id,name);write();draw();}));
        menu.palette(t);
        menu.row("Move to…",()->moveTo(t));
        menu.row(Help.SWITCH,()->privateHelp(t));
        menu.line();
        menu.danger("Delete",()->{shown.bin(t.id,System.currentTimeMillis());write();draw();});
        menu.show(from);
    }

    /**
     * Delete this private space (the owner, 2026-10-07; decision 117): it asks first, plainly, that everything is deleted
     * for good, the password opens nothing after, and it cannot be undone; on yes the space is erased by the next batches
     * (its index first, so it can never be opened again) and the screen closes to the ordinary app.
     */
    private void deleteSpace() {
        if(shown==null)return;
        secure(a.new Box().setTitle("Delete this private space?")
            .setMessage("Everything in this space is deleted for good. Its password will open nothing after. This cannot be undone: the 12 recovery words do not cover it.")
            .setPositiveButton("Delete for good",(d,w)->eraseSpace())
            .setNegativeButton("Cancel",null).show());
    }
    private void eraseSpace() {
        final PrivateSpace now=shown;if(now==null)return;
        final Slots.Space space=now.space;
        // Let the model go here and erase through the slots; close takes the screen away, as leaving does.
        open.remove(space);now.wipe();
        worker.submit(()->{try{space.erase();}catch(Throwable quietly){/* nothing said */}});
        shown=null;note=null;folder="";inBin=false;
        close();
    }

    /**
     * The box that sets a private thing's help request on or off (decision 113). Like the ordinary one, but the setting it
     * writes lives inside the vault (decision 111), so the box says nothing outside the space shows it, and it is secure like
     * every private box. Turning it on asks for the location, if it was not already allowed.
     */
    private void privateHelp(final PrivateSpace.Thing t) {
        if(t==null||shown==null)return;
        a.background.submit(()->a.store.addresses(),got->{
            @SuppressWarnings("unchecked") List<NoteStore.Contact> all=(List<NoteStore.Contact>)got;
            List<NoteStore.Contact> paired=new ArrayList<>();
            for(NoteStore.Contact one:all)if(one.paired())paired.add(one);
            final AlertDialog[] box={null};
            LinearLayout body=a.inside();
            body.addView(a.label(Help.WHAT,MainActivity.QUIET,a.MUTED));
            body.addView(a.spaced(a.label(Help.PHONE_PLACE,MainActivity.QUIET,a.MUTED)));
            body.addView(a.spaced(a.label(Help.PRIVATE_TRACE,MainActivity.QUIET,a.MUTED)));
            final EditText message=a.field(Help.MESSAGE_HINT,280);message.setSingleLine(false);message.setText(t.helpMessage);
            message.setImeOptions(message.getImeOptions()|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
            body.addView(a.spaced(message));
            body.addView(a.spaced(a.label(Help.TO,MainActivity.QUIET,a.INK)));
            final java.util.Set<String> picked=new java.util.LinkedHashSet<>(t.helpTo);
            if(paired.isEmpty())body.addView(a.label("Nobody is paired yet. Pair someone in People and devices first.",MainActivity.QUIET,a.MUTED));
            for(final NoteStore.Contact one:paired)
                body.addView(a.switchRow(one.name,picked.contains(one.address),on->{if(on)picked.add(one.address);else picked.remove(one.address);}));
            AlertDialog.Builder builder=a.new Box().setTitle(Help.ASK_TITLE).setView(a.scrolling(body)).setPositiveButton(Help.TURN_ON,null);
            if(t.helpOnOpen)builder=builder.setNeutralButton("Turn it off",(d,w)->{shown.help(t.id,false,null,null);write();});
            builder=builder.setNegativeButton("Cancel",null);
            box[0]=builder.create();secure(box[0]);
            box[0].setOnShowListener(d->box[0].getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                if(picked.isEmpty()){a.toast(Help.NEED_SOMEONE);return;}
                shown.help(t.id,true,new ArrayList<>(picked),message.getText().toString().trim());write();
                box[0].dismiss();a.askToLocate();
            }));
            box[0].show();
        },e->{});
    }
    private void moveTo(final PrivateSpace.Thing t) {
        final List<PrivateSpace.Thing> places=shown.placesFor(t.id);
        String[] rows=new String[places.size()+1];rows[0]="Private";for(int i=0;i<places.size();i++)rows[i+1]=places.get(i).name();
        choose("Move to",rows,which->{shown.move(t.id,which==0?"":places.get(which-1).id);write();draw();});
    }
    private void named(String title,String was,java.util.function.Consumer<String> done) {
        final EditText input=a.field("Name",120);input.setText(was);input.setSelection(input.getText().length());
        input.setImeOptions(input.getImeOptions()|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        LinearLayout body=a.inside();body.addView(input);
        secure(a.new Box().setTitle(title).setView(body).setPositiveButton(was.isEmpty()?"Create":"Rename",(d,w)->{
            String name=input.getText().toString().trim();if(!name.isEmpty())done.accept(name);}).show());
    }
    private void versions(final PrivateSpace.Thing t) {
        later.removeCallbacks(keep);shown.keepVersion(t.id,System.currentTimeMillis());write();
        final List<PrivateSpace.Version> all=shown.versionsOf(t.id);
        if(all.size()<2){secure(a.new Box().setTitle("Versions").setMessage("Versions are kept each time you leave the note after writing in it. There is no older one yet.").show());return;}
        final List<PrivateSpace.Version> older=all.subList(1,all.size());
        java.text.DateFormat when=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT);
        String[] rows=new String[older.size()];
        for(int i=0;i<rows.length;i++){String first=Given.firstLine(older.get(i).body);rows[i]=when.format(new java.util.Date(older.get(i).at))+"  ·  "+(first.isEmpty()?"Empty":first);}
        choose("Put an older version back",rows,which->{shown.putBackVersion(t.id,older.get(which),System.currentTimeMillis());write();draw();});
    }
    private void changePassword() {
        LinearLayout body=a.inside();
        TextView about=a.label("A long phrase of several words is best. Nobody can recover it if it is forgotten. Until everything is written again under it, the old password still opens this.",MainActivity.QUIET,a.MUTED);
        body.addView(about);
        final EditText first=password("New password"),again=password("The same again");body.addView(withEye(first));body.addView(withEye(again));
        final AlertDialog box=secure(a.new Box().setTitle("Change password").setView(body).setPositiveButton("Change password",null).show());
        box.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            char[] x=text(first),y=text(again);
            if(x.length==0){first.setError("Type the new password.");return;}
            if(!Arrays.equals(x,y)){again.setError("The two are not the same.");Arrays.fill(x,'\0');Arrays.fill(y,'\0');return;}
            Arrays.fill(y,'\0');first.setText("");again.setText("");box.dismiss();
            final PrivateSpace now=shown;says.setText("Changing the password…");
            worker.submit(()->{
                boolean fit=true;
                try{now.space.rekey(x);}catch(Throwable notDone){fit=false;}
                finally{Arrays.fill(x,'\0');}
                final boolean fitted=fit;
                later.post(()->{if(shown==now){full=!fitted;tick();if(fitted)says.setText("Password changed. Keep Mininotes open until it is all written.");}});
            });
        });
    }
    private EditText password(String hint) {
        EditText field=a.field(hint,400);
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setImeOptions(field.getImeOptions()|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return field;
    }
    private static char[] text(EditText field){Editable e=field.getText();char[] out=new char[e.length()];e.getChars(0,e.length(),out,0);return out;}

    /**
     * A password field with an eye at its right that shows the characters while it is held, hiding them again when let go
     * (the owner, 2026-10-06: "the password should be hidden with a trigger (eye or other) to see it"; decision 116).
     */
    private View withEye(EditText field) {
        field.setPadding(field.getPaddingLeft(),field.getPaddingTop(),a.dp(44),field.getPaddingBottom());
        FrameLayout wrap=new FrameLayout(a);
        wrap.addView(field,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
        FrameLayout.LayoutParams at=new FrameLayout.LayoutParams(a.dp(40),a.dp(40),Gravity.END|Gravity.CENTER_VERTICAL);
        wrap.addView(eye(field),at);
        return wrap;
    }
    /** The eye: it shows the field's characters while it is pressed, and masks them again when let go. */
    private View eye(final EditText field) {
        final Eye icon=new Eye(a,a.darkPaper()?PrivateSpace.LOCK_DARK:PrivateSpace.LOCK);
        icon.setBackgroundResource(a.borderlessFeedback());icon.setContentDescription("Hold to show the password");
        icon.setOnTouchListener((v,e)->{
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){reveal(field,true);icon.open(true);v.setPressed(true);}
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){reveal(field,false);icon.open(false);v.setPressed(false);v.performClick();}
            return true;
        });
        return icon;
    }
    private static void reveal(EditText field,boolean show) {
        int at=field.getSelectionEnd();
        field.setTransformationMethod(show?null:new android.text.method.PasswordTransformationMethod());
        if(at>=0&&at<=field.length())field.setSelection(at);
    }

    // ---- files and pictures -------------------------------------------------------------------------------------------

    private String pickingFor;private String pickingParent;private PrivateSpace.Kept saving;

    /**
     * Android's own picker (the screen stays open while it is up, as it was this screen that asked). From a note the file is
     * added to that note; from the top or a folder each file becomes its own private note named after it (decision 115: the
     * space keeps a file only with a note, so a file loose in it is kept this way; said in Read me).
     */
    private void addFiles() {
        if(shown==null)return;
        pickingFor=note;pickingParent=note==null?folder:null;picking=true;
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        try{a.startActivityForResult(pick,ADD);}catch(RuntimeException none){picking=false;}
    }
    /** What the picker gave: each read into memory, a picture made small, and kept with a note, encrypted in the vault (decision 111). */
    void picked(Intent data) {
        picking=false;
        final String into=pickingFor;final String parent=pickingParent;pickingFor=null;pickingParent=null;
        final PrivateSpace now=shown;
        if(data==null||now==null||(into==null&&parent==null))return;
        final List<Uri> chosen=new ArrayList<>();
        if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)chosen.add(data.getClipData().getItemAt(i).getUri());
        else if(data.getData()!=null)chosen.add(data.getData());
        says.setText("Adding…");
        final android.content.ContentResolver resolver=a.getContentResolver();
        worker.submit(()->{
            final List<Object[]> ready=new ArrayList<>();
            for(Uri one:chosen) {
                try(InputStream in=resolver.openInputStream(one)) {
                    if(in==null)continue;
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] part=new byte[1<<16];int n;long total=0;
                    while((n=in.read(part))!=-1){total+=n;if(total>(long)Slots.COUNT*Slots.DATA)throw new java.io.IOException("Too big");bytes.write(part,0,n);}
                    byte[] raw=bytes.toByteArray();
                    String name=nameOf(resolver,one);
                    byte[] small=shrink(raw);
                    // Alongside each: its own uri and original name, so the original can be offered for deletion once all are in.
                    if(small!=null){Arrays.fill(raw,(byte)0);ready.add(new Object[]{jpegName(name),"image/jpeg",small,true,one,name});}
                    else ready.add(new Object[]{name,resolver.getType(one)==null?"":resolver.getType(one),raw,false,one,name});
                } catch(Throwable unreadable){/* that one is not added */}
            }
            later.post(()->{
                if(shown!=now)return;
                final List<Uri> added=new ArrayList<>();final List<String> addedNames=new ArrayList<>();
                for(Object[] one:ready) {
                    String name=(String)one[0];String type=(String)one[1];byte[] bytes=(byte[])one[2];boolean picture=(Boolean)one[3];
                    String target=into;
                    // From the top or a folder: a private note named after the file, the file kept with it (decision 115).
                    if(target==null){PrivateSpace.Thing t=now.newNote(parent==null?"":parent,System.currentTimeMillis());now.rename(t.id,name);target=t.id;}
                    now.addFile(target,name,type,bytes,picture,System.currentTimeMillis());write();
                    if(full)break;
                    added.add((Uri)one[4]);addedNames.add((String)one[5]);
                }
                draw();
                offerDeleteOriginals(resolver,added,addedNames);
            });
        });
    }

    /**
     * After a file is added from the phone's picker, offer once to delete the original from this phone (decision 115). The
     * copy in the vault is kept; the original stays unless the owner says so, and the picker keeps its own recent list (as
     * Read me says). The provider may not allow deletion, which is said plainly.
     */
    private void offerDeleteOriginals(final android.content.ContentResolver resolver,final List<Uri> uris,final List<String> names) {
        if(uris==null||uris.isEmpty())return;
        boolean one=uris.size()==1;
        String named=one?"“"+names.get(0)+"”":uris.size()+" files";
        secure(a.new Box().setTitle("Delete the original from this phone?")
            .setMessage((one?named+" stays where it was unless you delete it.":named+" stay where they were unless you delete them.")
                +" The copy kept here is private.")
            .setPositiveButton(one?"Delete the original":"Delete the originals",(d,w)->deleteOriginals(resolver,uris))
            .setNegativeButton(one?"Keep it":"Keep them",null).show());
    }
    private void deleteOriginals(final android.content.ContentResolver resolver,final List<Uri> uris) {
        says.setText("Deleting the original…");
        worker.submit(()->{
            int gone=0;boolean refused=false;
            for(Uri u:uris) {
                try{if(android.provider.DocumentsContract.deleteDocument(resolver,u))gone++;else refused=true;}
                catch(Throwable cannot){refused=true;}
            }
            final int done=gone;final boolean could=!refused;
            later.post(()->{if(says==null)return;
                says.setText(could?(done==1?"The original was deleted":"The originals were deleted")
                    :"Mininotes cannot delete it from here: delete it in Files or Gallery");});
        });
    }
    private static String nameOf(android.content.ContentResolver resolver,Uri uri) {
        try(android.database.Cursor c=resolver.query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if(c!=null&&c.moveToFirst()&&c.getString(0)!=null)return c.getString(0);
        } catch(RuntimeException none){/* a name of its own, then */}
        return "File";
    }
    static String jpegName(String name){int dot=name.lastIndexOf('.');return (dot>0?name.substring(0,dot):name)+".jpg";}

    /**
     * A picture made small on its way in (the owner, 2026-10-06: "maybe like 50 photos"): about two megapixels, its longest
     * side {@link #LONGEST}, turned the way up its camera said, JPEG at {@link #QUALITY}, and nothing of the camera or of where
     * it was taken kept, since a picture made again carries none of it. In memory from end to end. Null where it is not a
     * picture this phone reads, which is then kept as it is. Drawn as every picture added anywhere is (decision 112), always
     * made again here, as it was.
     */
    static byte[] shrink(byte[] raw) throws java.io.IOException {return Shrink.remade(raw,Pictures.PAINTER);}
    private static Bitmap decode(byte[] bytes,int least) {
        if(bytes==null)return null;
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<=0)return null;
        BitmapFactory.Options how=new BitmapFactory.Options();how.inSampleSize=1;
        while(Math.min(bounds.outWidth,bounds.outHeight)/(how.inSampleSize*2)>=least)how.inSampleSize*=2;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,how);
    }

    /** A picture over the screen, from memory: never through a file another app could open. A tap puts it away. */
    private void view(final PrivateSpace.Kept k) {
        Bitmap picture=decode(shown.bytesOf(k.id),a.getResources().getDisplayMetrics().widthPixels/2);
        if(picture==null){fileMenu(k);return;}
        final FrameLayout over=new FrameLayout(a);over.setBackgroundColor(0xF0000000);over.setClickable(true);
        ImageView shownPicture=new ImageView(a);shownPicture.setImageBitmap(picture);shownPicture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        shownPicture.setContentDescription(k.name);
        over.addView(shownPicture,new FrameLayout.LayoutParams(-1,-1));
        TextView save=a.label("Save a copy",MainActivity.READING,0xFFFFFFFF);save.setPadding(a.dp(20),a.dp(14),a.dp(20),a.dp(14));
        save.setOnClickListener(v->saveCopy(k));
        over.addView(save,new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END));
        over.setOnClickListener(v->screen.removeView(over));
        screen.addView(over,new FrameLayout.LayoutParams(-1,-1));
    }
    private void fileMenu(final PrivateSpace.Kept k) {
        List<String> rows=new ArrayList<>();List<Runnable> does=new ArrayList<>();
        if(k.picture){rows.add("Open");does.add(()->view(k));}
        rows.add("Save a copy");does.add(()->saveCopy(k));
        rows.add("Remove");does.add(()->{shown.removeFile(k.id);write();draw();});
        choose(k.name,rows.toArray(new String[0]),which->does.get(which).run());
    }
    /** A copy saved where the user says, through Android's own picker: their own act, the one way anything private leaves. */
    private void saveCopy(PrivateSpace.Kept k) {
        saving=k;picking=true;
        Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType(k.type==null||k.type.isEmpty()?"application/octet-stream":k.type);save.putExtra(Intent.EXTRA_TITLE,k.name);
        try{a.startActivityForResult(save,SAVE);}catch(RuntimeException none){picking=false;}
    }
    void saved(Intent data) {
        picking=false;
        final PrivateSpace.Kept k=saving;saving=null;
        if(data==null||data.getData()==null||k==null||shown==null)return;
        final byte[] bytes=shown.bytesOf(k.id);if(bytes==null)return;
        final Uri where=data.getData();final android.content.ContentResolver resolver=a.getContentResolver();
        worker.submit(()->{
            boolean ok;
            try(OutputStream out=resolver.openOutputStream(where,"wt")){if(out==null)throw new java.io.IOException();out.write(bytes);ok=true;}
            catch(Throwable failed){ok=false;}
            final boolean done=ok;
            later.post(()->{if(says!=null)says.setText(done?"Saved a copy":"Could not save a copy there");});
        });
    }
    /** The picker came back with nothing: the screen goes on as it was. */
    void notPicked(){picking=false;pickingFor=null;pickingParent=null;saving=null;}

    // ---- the clipboard ------------------------------------------------------------------------------------------------

    /**
     * Something copied from a private note (marked sensitive by the page, so the keyboard and the system neither show nor keep
     * it): remembered, so it can be taken off the clipboard when this closes, unless something else was copied since.
     */
    private void noteCopied(String words) {
        copiedHere=true;copiedWords=words;
        final ClipboardManager clip=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
        if(clip==null||clipMoved!=null)return;
        clipMoved=()->{
            try{ClipData now=clip.getPrimaryClip();CharSequence said=now==null||now.getItemCount()==0?null:now.getItemAt(0).getText();
                copiedHere=said!=null&&copiedWords!=null&&copiedWords.contentEquals(said);}
            catch(RuntimeException unreadable){/* as it was */}
        };
        clip.addPrimaryClipChangedListener(clipMoved);
    }
    private String copiedWords;
    /** What a private page copies: its words, marked sensitive where Android knows the mark (13 and later). */
    static ClipData sensitive(String words) {
        ClipData data=ClipData.newPlainText(null,words);
        if(android.os.Build.VERSION.SDK_INT>=33){android.os.PersistableBundle extras=new android.os.PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true);data.getDescription().setExtras(extras);}
        return data;
    }
    private void unclip() {
        ClipboardManager clip=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
        if(clip==null)return;
        if(clipMoved!=null){clip.removePrimaryClipChangedListener(clipMoved);clipMoved=null;}
        if(copiedHere)try{clip.clearPrimaryClip();}catch(RuntimeException notAllowed){/* nothing more Android allows */}
        copiedHere=false;copiedWords=null;
    }

    // ---- closing --------------------------------------------------------------------------------------------------------

    /**
     * Everything closes: what was typed handed to be written, the screen gone and the app as it was, every open space let
     * go (what still waits to be written goes in the next batches, sealed), and what was copied from in here taken off
     * the clipboard. The window stays secure until it is in front again, so the picture of it the recent apps keep is blank.
     */
    void close() {
        catching=null;picking=false;
        try {
            if(shown!=null&&note!=null){later.removeCallbacks(keep);shown.keepVersion(note,System.currentTimeMillis());}
            for(PrivateSpace one:open.values())try{one.write();}catch(Throwable notKept){/* refused whole: as it was */}
        } finally {
            for(PrivateSpace one:open.values())one.wipe();
            open.clear();shown=null;note=null;folder="";inBin=false;full=false;panel=null;
            later.removeCallbacks(keep);later.removeCallbacks(again);
            if(page!=null){loading=true;try{page.setText("");}finally{loading=false;}}
            page=null;files=null;
            unclip();
            if(screen!=null) {
                if(screen.getParent()!=null)((ViewGroup)screen.getParent()).removeView(screen);
                screen=null;
                if(inFront)a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            worker.submit(()->{Slots s=slots;if(s!=null)s.close();});
        }
    }
}
