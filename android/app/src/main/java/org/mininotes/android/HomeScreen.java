// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Home: the app's first screen, laid out as a phone's own desktop is (docs/HOME.md, step 2). The bar as it always was;
 * under it the grid of what is on Home - its collections and notes in the owner's order, then the files that came from
 * other devices - four across; a round <b>+</b> at the foot on the right; the search bar; and the dock, with the handle
 * above it that is swiped up for the overview. A collection opens as a pop-up over the dimmed grid (see {@link Folder});
 * a note opens as the page it always was.
 *
 * <p>Everything is picked up the way the tiles were: held, the thing's menu comes up; moved while still held, it is
 * carried. Let go on a note it makes a collection of the two; on a collection it goes in; in an empty cell it stays there,
 * with the empty cells round it left empty, as a phone's home screen keeps its icons (decision 39, see {@link Layout});
 * out of a pop-up onto the dimmed grid it comes up a level; on the dock it is a favourite there. What each drop means is
 * decided in {@link Grid}; this class only measures where the finger is.
 *
 * <p>Drawn from MainActivity's own helpers, opened to the package for it, so a tile, a mark, a menu and a box look and
 * behave here exactly as they do everywhere else in the app.
 */
final class HomeScreen {
    private final MainActivity a;
    final Folder folder;
    final Dock dock;
    final OverviewScreen overview;

    /** The screen this last drew on: a read that comes back after the reader has gone somewhere else is dropped. */
    private LinearLayout drawnOn;
    /** Everything between the bar and the search bar: the grid, and over it the + and any pop-up. */
    FrameLayout desk;
    private ScrollView gridScroll;
    private GridLayout grid;
    /** The search bar, shown on Home or not (decision 47). */
    private View searchView;
    private View dockView;
    /** Home's page in view, across and down from the main one (decision 45); kept while the app is open. */
    int pageX,pageY;
    /** The dots saying which page is in view. */
    private View dots;
    /**
     * Home's pages, all of them, side by side as they lie (decision 45): {@code world} holds them and is moved under the
     * finger as a page is swiped and scaled as it is pinched, inside {@code pager}, which hears those gestures. The page
     * in view is {@code here}, which never moves inside {@code world} and holds the real grid; the others are drawn beside
     * it the same way, as many holders, one for each page there is.
     */
    private FrameLayout pager,world,here;
    private final Map<List<Integer>,Laid> pageLayouts=new HashMap<>();
    private float baseX,baseY;
    private int fittedWidth,fittedHeight;
    private float restoreAcross,restoreDown;
    private boolean restoreViewport;
    private android.widget.OverScroller momentum;
    private boolean scrolling;
    private boolean panning;
    private String landedId;
    private final Runnable scrollFrame=()->advanceScroll();
    private final List<View> others=new ArrayList<>();
    /** Each page's holder by {x, y}, the page in view's among them; and, while a page is carried, the empty places it can go. */
    private final Map<List<Integer>,View> holders=new HashMap<>();
    private final Map<List<Integer>,View> places=new HashMap<>();
    /** A whole page picked up, zoomed out: which, and its holder. */
    private int[] carriedPage;private View carriedHolder;private List<Integer> litPlace;
    /** Zoomed out, every page small in its frame, and which page is in the middle of it then. */
    private boolean zoomed;private int focusX,focusY;
    /** Zoomed out as far as every page at once (decision 51), not only the one in the middle with the next ones beside it. */
    private boolean allSeen;
    /** How small the pages are, zoomed out: a whole page, in a frame, with the ones beside it showing. */
    private static final float SMALL=0.6f;
    /** The world's scale and where it is moved to, now; and what is moving it, if anything. */
    private float scaleNow=1f,shiftX,shiftY;private android.animation.ValueAnimator moving;
    /** Things made with + on another page than the main one, by id: they stand on that page once they are read (decision 46). */
    private final Map<String,int[]> placeNext=new HashMap<>();
    /** The grid a carried icon came out of, kept from the start of the carry: a page turned under it draws its icons anew. */
    private GridLayout carriedFrom;
    /** The mark in the bar, asked again whenever Home is, since a pop-up's own mark takes over the one the app keeps. */
    private View homeMark;
    /** "new" at the top of Home, with how many: shown while anything is new. */
    private TextView newMark;
    /**
     * Home's own + : hidden while a pop-up is open, whose own + is the one - two at once, one of them bright over the
     * dimmed grid behind the card, made the next step a choice between two things that look alike.
     */
    View homePlus;
    /** Something changed while Home could not be drawn again - a thing being carried, a name being typed. */
    private boolean stale;
    /** Which read is the newest: an older one that comes back late draws nothing. */
    private int reads;
    /** Where a carried thing is, as far as letting it go goes, and where the finger last was. */
    private Grid.Zone zone=Grid.Zone.NONE;
    /** Whether the dock is lit, as a place to let go. */
    private boolean dockLit;
    /** Home's grid as it was last drawn: where each icon stands, and the empty cells between (see {@link Laid}). */
    private Laid homeLaid;
    /** The empty cell shown as where a carried thing, or files from another app, will land; and which grid it is in. */
    private View landing;
    private Laid aimIn;
    private int[] aimAt;

    HomeScreen(MainActivity a) {
        this.a=a;
        folder=new Folder(a,this);dock=new Dock(a,this);overview=new OverviewScreen(a,this);
    }

    /**
     * Whether a level of the old shelves is Home now: the top, any collection (a pop-up over Home), the favourites (the
     * Favourites collection's pop-up) and the drop box, whose files are on Home; and the archive and the bin while they are
     * on Home (decision 41), as cards over it. Switched off, they are the lists they were, reached from ⋮, and so is the
     * screen of places a thing carried with Move to… can go.
     *
     * @param away whether the archive and the bin are on Home (Settings)
     */
    static boolean takes(NoteStore.Branch.Kind kind,boolean away) {
        return kind==NoteStore.Branch.Kind.LIBRARY||kind==NoteStore.Branch.Kind.COLLECTION||kind==NoteStore.Branch.Kind.BOOK
            ||kind==NoteStore.Branch.Kind.FAVOURITES||kind==NoteStore.Branch.Kind.DROPS
            // Temp, Recent and Shared with me are cards on Home too: back from a note opened in one is that card again, not
            // an empty page named after it (the owner, 2026-10-11).
            ||kind==NoteStore.Branch.Kind.TEMP||kind==NoteStore.Branch.Kind.RECENT||kind==NoteStore.Branch.Kind.SHARED
            ||away&&(kind==NoteStore.Branch.Kind.ARCHIVE||kind==NoteStore.Branch.Kind.BIN);
    }

    /** Whether Home is what is on the screen now. */
    boolean showing(){return drawnOn!=null&&drawnOn==a.root&&a.shelves&&a.carrying==null;}

    /** Home as a thing a menu and a mark are about: everything on this device, which is what the library has always been. */
    static NoteStore.Branch home() {
        return new NoteStore.Branch(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,"","Home","",0,0,true);
    }

    // ---- drawing -----------------------------------------------------------------------------------------------

    /**
     * Home, on the screen MainActivity has just made (see MainActivity.browse), and over it the pop-up the trail ends in,
     * if it ends in one: that is how the way back from a note, a restart, and a thing moved somewhere all reopen it.
     */
    void draw() {
        if(fittedWidth>0&&fittedHeight>0){restoreAcross=shiftX/fittedWidth;restoreDown=shiftY/fittedHeight;restoreViewport=true;}
        fittedWidth=0;fittedHeight=0;
        stopScroll();panning=false;
        a.tiles=null;a.rows=null;a.desktop=false;a.scroller=null;
        // The drop box is on Home now: a trail remembered by a build from before, that stood in it, stands on Home.
        for(int at=1;at<a.trail.size();at++)if(a.trail.get(at).kind==NoteStore.Branch.Kind.DROPS){while(a.trail.size()>at)a.trail.remove(a.trail.size()-1);break;}
        drawnOn=a.root;stale=false;zone=Grid.Zone.NONE;dockLit=false;homeLaid=null;landing=null;aimIn=null;aimAt=null;
        // A fresh screen has nothing carried on it: a carry that never heard its end must not hold Home still for ever.
        a.dragging=null;a.lifted=null;
        a.root.addView(bar());
        // Under the bar, a contact in trouble, said while there is one (decision 188).
        a.root.addView(a.troubleStrip(),new LinearLayout.LayoutParams(-1,-2));
        desk=new FrameLayout(a);
        // One surface owns scrolling and momentum; pages have no independent scroll inside them.
        final Turner turner=new Turner();
        pager=new FrameLayout(a){
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){return turner.intercept(e);}
            @android.annotation.SuppressLint("ClickableViewAccessibility")
            @Override public boolean onTouchEvent(android.view.MotionEvent e){turner.touch(e);return true;}
            // The pages round the one in view are drawn once the pager has a size: before, there is nowhere to put them.
            @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(w>0&&h>0)post(()->{refitNow();buildPages();});}
        };
        // The world has real bounds spanning all pages. Only the viewport clips it.
        pager.setClipChildren(true);
        world=new FrameLayout(a);world.setClipChildren(false);world.setClipToPadding(false);
        // The page in view outlined in the accent, zoomed out, so it is plain where you are among them.
        GradientDrawable seen=frame();seen.setStroke(Math.max(1,a.dp(3)),a.ACCENT);
        here=new FrameLayout(a);here.setBackground(seen);holders.clear();places.clear();carriedPage=null;carriedHolder=null;
        world.addView(here,new FrameLayout.LayoutParams(-1,-1));
        pager.addView(world,new FrameLayout.LayoutParams(-1,-1));
        zoomed=false;scaleNow=1f;shiftX=0;shiftY=0;others.clear();
        // The surface owns Home's scrolling. A second scroll inside a page would compete with its gestures.
        gridScroll=new ScrollView(a){
            @Override public boolean performClick(){return super.performClick();}
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){return false;}
            @Override public boolean onTouchEvent(android.view.MotionEvent event){return false;}
        };
        gridScroll.setClipToPadding(false);gridScroll.setVerticalScrollBarEnabled(false);
        grid=new GridLayout(a);grid.setColumnCount(columns(a.getResources().getDisplayMetrics().widthPixels));
        // A few pixels at the edge only. A tall band under the last line was not a cell, so nothing could be let go
        // between one page and the next (decision 129). The + floats over the corner.
        grid.setPadding(a.dp(8),0,a.dp(8),0);
        gridScroll.addView(grid,new FrameLayout.LayoutParams(-1,-2));
        here.addView(gridScroll,new FrameLayout.LayoutParams(-1,-1));
        desk.addView(pager,new FrameLayout.LayoutParams(-1,-1));
        // Files dragged in from another app and let go on Home's grid go with what is under the finger, as the PC aims what
        // Explorer drops: a note's icon, the note; a collection's, the collection; anywhere else Home, in the empty cell.
        gridScroll.setOnDragListener((v,event)->filesOn(v,event,grid,()->homeLaid,()->Things.HOME));
        // Laid out again once the grid is measured, or its names change size, so its rows fill what is seen of it.
        // Not while a finger is down on an icon: laying the grid out again takes that icon away, and the hold never becomes
        // a drag (decision 130). The finger lifting asks again, once, in case the room did change.
        gridScroll.addOnLayoutChangeListener((v,l,t,r,b,wl,wt,wr,wb)->{if(a.pressing||a.heldTile!=null)return;refitNow();});
        homePlus=plus(()->Things.HOME);
        desk.addView(homePlus,plusPlace());
        dots=new PageDots();
        FrameLayout.LayoutParams under=new FrameLayout.LayoutParams(-1,a.dp(18),Gravity.BOTTOM);under.setMargins(0,0,0,a.dp(6));
        desk.addView(dots,under);
        a.root.addView(desk,new LinearLayout.LayoutParams(-1,0,1));
        searchView=searchBar();
        a.root.addView(searchView,searchPlace());
        dockView=dock.build();
        a.root.addView(dockView,new LinearLayout.LayoutParams(-1,-2));
        showFoot();
        // One listener for everything carried on Home, so a thing can go from the grid or a pop-up to the dock and back.
        a.root.setOnDragListener(this::dragged);
        // The pop-up the trail ends in, if it ends in one; either way Home is read, once.
        if(a.trail.size()>1)folder.show();else refresh();
    }

    /**
     * Favourites and the search shown or not, as Settings and Home's menu say (decision 47). The handle above the dock stays
     * either way: it is the way to what is open.
     */
    void showFoot() {
        // Hidden controls leave their room to the grid, including touch and drop targets.
        boolean centre=!zoomed&&onCentre();
        if(searchView!=null)searchView.setVisibility(a.showSearch&&centre?View.VISIBLE:View.GONE);
        if(dockView!=null)dockView.setVisibility(centre?View.VISIBLE:View.GONE);
        dock.showRow(a.showDock?View.VISIBLE:View.GONE);
    }

    /** Stable page geometry uses all the room below the bar, including room a visible foot occupies. */
    private int homeRoom() {
        if(a.root==null||a.root.getHeight()<=0)return pager==null?1:Math.max(1,pager.getHeight());
        int bar=a.root.getChildCount()==0?0:a.root.getChildAt(0).getMeasuredHeight();
        return Math.max(1,a.root.getHeight()-a.root.getPaddingTop()-a.root.getPaddingBottom()-bar);
    }

    // ---- pages (docs/HOME.md, decisions 45-50) ------------------------------------------------------------------------

    boolean onCentre(){return seenPageX()==0&&seenPageY()==0;}
    /** The page under the middle of the screen. Favourites and the search follow it, including while a scroll rests between pages. */
    private int seenPageX(){return zoomed||pager==null||pager.getWidth()<=0?pageX:(int)Math.round(pageX-shiftX/strideX());}
    private int seenPageY(){return zoomed||pager==null||pager.getHeight()<=0?pageY:(int)Math.round(pageY-shiftY/strideY());}
    private float strideX(){return pager==null?1f:Math.max(1,pager.getWidth());}
    private float strideY(){return homeLaid==null?pager==null?1f:Math.max(1,pager.getHeight()):Math.max(1,homeLaid.rows*homeLaid.height);}

    /** How many pages there are: the main one, and each that something stands on. */
    int pageCount(){return homeLaid==null||homeLaid.spots==null?1:Layout.active(homeLaid.spots).size();}

    /**
     * Whether two fingers put down at that point of the screen are Home's: on its pages, with nothing open over them - a
     * pinch there shrinks the pages (decision 51), not the whole pad.
     */
    boolean pinchesAt(float rawX,float rawY) {
        if(!showing()||pager==null||!pager.isShown()||folder.isOpen()||a.showing!=null)return false;
        int[] at=new int[2];pager.getLocationOnScreen(at);
        return rawX>=at[0]&&rawX<at[0]+pager.getWidth()&&rawY>=at[1]&&rawY<at[1]+pager.getHeight();
    }

    /** The page that way, where there is one, sliding in under the finger's way; else a short nudge says there is none. */
    void turn(int dx,int dy) {
        int[] to=homeLaid==null||homeLaid.spots==null?null:Layout.next(Layout.active(homeLaid.spots),seenPageX(),seenPageY(),dx,dy);
        if(to==null){nudge(dx,dy);return;}
        go(to[0],to[1]);
    }

    /** Nothing that way: the pages give a little and come back. */
    private void nudge(int dx,int dy) {
        float back=a.dp(24);
        move(1f,-dx*back,-dy*back,90,()->move(1f,0,0,140,null));
    }

    /** Where a page lies in the world, from the page whose grid is drawn: edge to edge, with no empty band between (decision 126). */
    private float[] offset(int x,int y) {
        return new float[]{(x-pageX)*strideX(),(y-pageY)*strideY()};
    }

    /** The pages something stands on, the one in view always inside, and one past them while a thing is carried there. */
    private boolean beyond;private int beyondX,beyondY;
    private int[] pageBounds() {
        int minX=pageX,maxX=pageX,minY=pageY,maxY=pageY;
        if(homeLaid!=null&&homeLaid.spots!=null) {
            int[] s=Layout.span(homeLaid.spots);
            minX=Math.min(minX,s[0]);maxX=Math.max(maxX,s[1]);minY=Math.min(minY,s[2]);maxY=Math.max(maxY,s[3]);
        }
        if(beyond){minX=Math.min(minX,beyondX);maxX=Math.max(maxX,beyondX);minY=Math.min(minY,beyondY);maxY=Math.max(maxY,beyondY);}
        return new int[]{minX,maxX,minY,maxY};
    }

    /**
     * The scroll kept where the finger let go, between pages as well as on one, and never past the last page. When it has
     * crossed a whole page, that page's grid is the one drawn and the fraction left over stays, so the move does not jump.
     */
    private void rest() {
        if(pager==null)return;
        float[] b=scrollLimits();
        show(1f,Math.max(b[0],Math.min(b[1],shiftX)),Math.max(b[2],Math.min(b[3],shiftY)));
        showFoot();
        if(stale&&!a.pressing&&a.dragging==null&&!panning)refresh();
    }

    /** Translation bounds include the last page's final partial row, so every icon clears the foot. */
    private float[] scrollLimits() {
        int[] b=pageBounds();
        float extra=Math.max(0,strideY()-(pager==null?strideY():pager.getHeight()));
        return new float[]{-(b[1]-pageX)*strideX(),-(b[0]-pageX)*strideX(),
            -(b[3]-pageY)*strideY()-extra,-(b[2]-pageY)*strideY()};
    }

    private void stopScroll() {
        scrolling=false;
        if(momentum!=null)momentum.abortAnimation();
        if(pager!=null)pager.removeCallbacks(scrollFrame);
    }

    private void fling(float vx,float vy) {
        stopScroll();rest();
        if(pager==null)return;
        if(momentum==null)momentum=new android.widget.OverScroller(a);
        float[] b=scrollLimits();
        momentum.fling(Math.round(shiftX),Math.round(shiftY),Math.round(vx),Math.round(vy),
            Math.round(b[0]),Math.round(b[1]),Math.round(b[2]),Math.round(b[3]));
        scrolling=true;pager.postOnAnimation(scrollFrame);
    }

    private void advanceScroll() {
        if(!scrolling||pager==null||!pager.isAttachedToWindow())return;
        if(momentum.computeScrollOffset()) {
            show(1f,momentum.getCurrX(),momentum.getCurrY());showFoot();
            pager.postOnAnimation(scrollFrame);
        } else {scrolling=false;rest();}
    }

    /** That page slid into view, the others moving with it, and then it is the page in view. */
    void go(int x,int y) {
        if(pager==null||x==pageX&&y==pageY){if(pager!=null)move(1f,0,0,160,null);return;}
        float[] at=offset(x,y);
        move(1f,-at[0],-at[1],260,()->settle(x,y));
    }

    /**
     * A page in view - any of them, an empty one beyond the edge while something is carried there - drawn from what Home
     * last read, coming in from the side it lies on ({@code dx}, {@code dy}), or at once with neither.
     */
    void showPage(int x,int y,int dx,int dy) {
        if(x==pageX&&y==pageY)return;
        settle(x,y);
        if(pager!=null&&(dx!=0||dy!=0)) {
            place(1f,dx*strideX(),dy*strideY());
            move(1f,0,0,220,null);
        }
    }

    /** The page in view is that one now: its grid drawn where the page in view always stands, the others round it. */
    private void settle(int x,int y) {
        pageX=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,x));pageY=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,y));
        focusX=pageX;focusY=pageY;zoomed=false;allSeen=false;
        aim(null,null);a.markOnto(null);
        if(homeLaid!=null&&showing())fill(homeLaid.lines,homeLaid.places);
        place(1f,0,0);
        showFoot();
        if(dots!=null)dots.invalidate();
        if(gridScroll!=null)gridScroll.announceForAccessibility(said(pageX,pageY));
    }

    /** Every page at once, each whole in its frame: a tap goes into one (decisions 45, 51). Home's menu, All pages. */
    void zoomOut() {
        if(pager==null||homeLaid==null)return;
        if(folder.isOpen())folder.close();
        focusX=pageX;focusY=pageY;
        float[] all=allAt();
        zoomed=true;allSeen=true;
        move(all[0],all[1],all[2],240,null);
    }

    /**
     * How small, and moved how, every page is seen at once: the pages there are, with half a page's room round them for the
     * places a carried page can go, as big as fits the screen - never bigger than the one-page-and-its-neighbours view.
     */
    private float[] allAt() {
        int minX=pageX,maxX=pageX,minY=pageY,maxY=pageY;
        if(homeLaid!=null&&homeLaid.spots!=null)for(List<Integer> one:Layout.active(homeLaid.spots)){
            minX=Math.min(minX,one.get(0));maxX=Math.max(maxX,one.get(0));minY=Math.min(minY,one.get(1));maxY=Math.max(maxY,one.get(1));}
        float w=strideX(),h=strideY();
        float wide=(maxX-minX+2)*w,high=(maxY-minY+2)*h;
        float scale=Math.min(SMALL,Math.min(pager.getWidth()*0.96f/wide,pager.getHeight()*0.96f/high));
        // The middle of them all where the middle of the screen is.
        float midX=((minX+maxX)/2f-pageX)*w,midY=((minY+maxY)/2f-pageY)*h;
        return new float[]{scale,-scale*midX,-scale*midY};
    }

    /** One page in the middle, zoomed out, the next ones beside it, as a phone's own home screen does. */
    private void zoomTo(int x,int y,long millis) {
        focusX=x;focusY=y;zoomed=true;allSeen=false;
        float[] at=offset(x,y);
        move(SMALL,-SMALL*at[0],-SMALL*at[1],millis,null);
    }

    /** Back in, onto the page in the middle - or the one tapped. */
    void zoomInto(int x,int y) {
        float[] at=offset(x,y);
        zoomed=false;allSeen=false;
        move(1f,-at[0],-at[1],240,()->{if(x!=pageX||y!=pageY)settle(x,y);else place(1f,0,0);});
    }
    boolean isZoomed(){return zoomed;}

    /** The world at a scale and moved so, at once: the frames, the dots and the + as fit that scale. */
    private void place(float scale,float x,float y) {
        stopScroll();
        if(moving!=null){moving.cancel();moving=null;}
        show(scale,x,y);
    }
    /** The same, leaving a move under way to go on: what a step of a move does, and Home drawn again mid-move. */
    private void show(float scale,float x,float y) {
        scaleNow=scale;shiftX=x;shiftY=y;
        if(world==null)return;
        if(scale>=0.999f&&!zoomed)ensureVisiblePages();
        world.setPivotX(baseX+pager.getWidth()/2f);world.setPivotY(baseY+pager.getHeight()/2f);
        world.setScaleX(scale);world.setScaleY(scale);world.setTranslationX(x-baseX);world.setTranslationY(y-baseY);
        // The frames come as the pages shrink, and the room round them darkens a little, as a phone's own home screen does.
        float framed=Math.max(0f,Math.min(1f,(1f-scale)/(1f-SMALL)));
        int alpha=Math.round(framed*255);
        if(here.getBackground()!=null)here.getBackground().setAlpha(alpha);
        for(View one:others)if(one.getBackground()!=null)one.getBackground().setAlpha(alpha);
        // Home's own colour still seen round the pages, only dimmed, as a phone's wallpaper is behind its pages.
        pager.setBackgroundColor(Math.round(framed*0.22f*255)<<24);
        if(dots!=null){dots.setVisibility(scale<0.999f?View.INVISIBLE:View.VISIBLE);dots.invalidate();}
        if(homePlus!=null&&!folder.isOpen())homePlus.setVisibility(scale<0.999f?View.GONE:View.VISIBLE);
        showFoot();
    }

    /** The world moved there and scaled so, over a moment, and then what follows. */
    private void move(float scale,float x,float y,long millis,Runnable then) {
        stopScroll();
        if(moving!=null){moving.cancel();moving=null;}
        final float fromS=scaleNow,fromX=shiftX,fromY=shiftY;
        android.animation.ValueAnimator go=android.animation.ValueAnimator.ofFloat(0f,1f);
        go.setDuration(millis);go.setInterpolator(new android.view.animation.DecelerateInterpolator());
        go.addUpdateListener(v->{
            float f=(Float)v.getAnimatedValue();
            float s1=fromS+(scale-fromS)*f,x1=fromX+(x-fromX)*f,y1=fromY+(y-fromY)*f;
            show(s1,x1,y1);
        });
        go.addListener(new android.animation.AnimatorListenerAdapter(){
            boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator anim){cancelled=true;}
            @Override public void onAnimationEnd(android.animation.Animator anim){if(moving==go)moving=null;if(!cancelled&&then!=null)then.run();}
        });
        moving=go;go.start();
    }

    /** A page's frame, seen only as the pages shrink. */
    private GradientDrawable frame() {
        // A faint fill, so each page reads as a page over the dimmed room round it.
        GradientDrawable edge=new GradientDrawable();edge.setColor(0x1FFFFFFF);edge.setCornerRadius(a.dp(22));
        edge.setStroke(Math.max(1,a.dp(2)),MainActivity.mix(a.INK,a.PAPER,0.55f));edge.setAlpha(0);
        return edge;
    }

    /**
     * The pages beside the one in view, each drawn as the grid draws it - the same icons, in the same cells - where it
     * lies from it, so a swipe or a pinch shows it coming. Laid out again whenever Home is.
     */
    private void buildPages() {
        if(world==null||homeLaid==null||homeLaid.spots==null)return;
        for(View one:others)world.removeView(one);
        others.clear();holders.clear();pageLayouts.clear();
        holders.put(java.util.Arrays.asList(pageX,pageY),here);
        if(pager.getWidth()<=0)return;
        int[] bounds=pageBounds();int width=Math.round(strideX()),height=Math.round(strideY());
        if(restoreViewport) {
            shiftX=restoreAcross*width;shiftY=restoreDown*height;restoreViewport=false;
        } else if(fittedWidth>0&&fittedHeight>0&&(width!=fittedWidth||height!=fittedHeight)) {
            stopScroll();
            if(moving!=null){moving.cancel();moving=null;}
            shiftX=shiftX*width/fittedWidth;shiftY=shiftY*height/fittedHeight;
        }
        fittedWidth=width;fittedHeight=height;
        baseX=(pageX-bounds[0])*width;baseY=(pageY-bounds[2])*height;
        world.setLayoutParams(new FrameLayout.LayoutParams((bounds[1]-bounds[0]+1)*width,(bounds[3]-bounds[2]+1)*height));
        FrameLayout.LayoutParams main=new FrameLayout.LayoutParams(width,height);
        main.leftMargin=(int)baseX;main.topMargin=(int)baseY;here.setLayoutParams(main);
        homeLaid.canvas=grid;homeLaid.pageX=pageX;homeLaid.pageY=pageY;
        pageLayouts.put(List.of(pageX,pageY),homeLaid);
        Set<List<Integer>> pages=Layout.active(homeLaid.spots);
        if(beyond)pages.add(List.of(beyondX,beyondY));
        for(List<Integer> page:pages) {
            if(page.get(0)==pageX&&page.get(1)==pageY)continue;
            FrameLayout holder=new FrameLayout(a);holder.setBackground(frame());
            GridLayout cells=new GridLayout(a);cells.setColumnCount(homeLaid.columns);cells.setPadding(grid.getPaddingLeft(),0,grid.getPaddingRight(),0);
            Laid laid=neighbor(cells,page.get(0),page.get(1));
            holder.addView(cells,new FrameLayout.LayoutParams(width,height));
            pageLayouts.put(page,laid);
            cells.setOnDragListener((v,event)->filesOn(v,event,cells,()->laid,()->Things.HOME));
            FrameLayout.LayoutParams spot=new FrameLayout.LayoutParams(width,height);
            spot.leftMargin=(page.get(0)-bounds[0])*width;spot.topMargin=(page.get(1)-bounds[2])*height;
            world.addView(holder,spot);
            others.add(holder);holders.put(page,holder);
        }
        show(scaleNow,shiftX,shiftY);
        showFoot();
    }

    /** Keep page coordinates and the fraction scrolled, independent of the next screen's pixels. */
    void keepViewport(android.os.Bundle state) {
        state.putInt("homeX",pageX);state.putInt("homeY",pageY);
        state.putFloat("homeAcross",fittedWidth>0?shiftX/fittedWidth:restoreAcross);
        state.putFloat("homeDown",fittedHeight>0?shiftY/fittedHeight:restoreDown);
    }

    void restoreViewport(android.os.Bundle state) {
        pageX=state.getInt("homeX");pageY=state.getInt("homeY");
        restoreAcross=state.getFloat("homeAcross");restoreDown=state.getFloat("homeDown");restoreViewport=true;
    }

    /** Every visible page is a real grid. No page has to be rebuilt just to tap or pick up its icons. */
    private Laid neighbor(GridLayout cells,int px,int py) {
        Map<String,int[]> drawn=new java.util.LinkedHashMap<>();
        for(Map.Entry<String,Layout.Spot> one:homeLaid.spots.entrySet())if(one.getValue().on(px,py))
            drawn.put(one.getKey(),new int[]{one.getValue().row(),one.getValue().column()});
        Laid laid=new Laid(drawn,homeLaid.lines,homeLaid.places,Where.HOME,homeLaid.columns,homeLaid.rows,
            homeLaid.width,homeLaid.height,homeLaid.seen,homeLaid.natural);
        laid.spots=homeLaid.spots;laid.canvas=cells;laid.pageX=px;laid.pageY=py;
        Map<String,NoteStore.Branch> byId=new HashMap<>();
        for(NoteStore.Branch one:laid.lines)byId.put(one.id,one);
        for(NoteStore.Branch one:laid.places)byId.put(one.id,one);
        for(int row=0;row<laid.rows;row++)for(int col=0;col<laid.columns;col++) {
            String id=Layout.at(drawn,row,col);View view;
            if(id==null) {
                view=new View(a);view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                view.setOnLongClickListener(v->{roomHeld(v,Where.HOME);return true;});
                laid.spaces.put(Layout.cell(row,col),view);
            } else {
                NoteStore.Branch one=byId.get(id);
                view=Grid.place(one.kind)?placeIcon(one,laid.width):icon(one,laid.width,Where.HOME);
                if(a.dragging!=null&&id.equals(a.dragging.id))view.setVisibility(View.INVISIBLE);
                laid.things.put(id,one);laid.icons.put(id,view);
            }
            GridLayout.LayoutParams spot=new GridLayout.LayoutParams(GridLayout.spec(row),GridLayout.spec(col));
            spot.width=laid.width;spot.height=laid.height;view.setMinimumHeight(laid.height);cells.addView(view,spot);
        }
        return laid;
    }

    /** Blank room between occupied pages is a usable grid too. Create only the pages entering the viewport. */
    private void ensureVisiblePages() {
        if(homeLaid==null||homeLaid.spots==null||pager==null||pager.getWidth()<=0)return;
        int[] bounds=pageBounds();
        int left=pageX+(int)Math.floor(-shiftX/strideX()),top=pageY+(int)Math.floor(-shiftY/strideY());
        for(int px=left;px<=left+1;px++)for(int py=top;py<=top+1;py++) {
            List<Integer> key=List.of(px,py);
            if(px<bounds[0]||px>bounds[1]||py<bounds[2]||py>bounds[3]||pageLayouts.containsKey(key))continue;
            FrameLayout holder=new FrameLayout(a);holder.setBackground(frame());
            GridLayout cells=new GridLayout(a);cells.setColumnCount(homeLaid.columns);cells.setPadding(grid.getPaddingLeft(),0,grid.getPaddingRight(),0);
            Laid laid=neighbor(cells,px,py);pageLayouts.put(key,laid);
            holder.addView(cells,new FrameLayout.LayoutParams(Math.round(strideX()),Math.round(strideY())));
            cells.setOnDragListener((v,event)->filesOn(v,event,cells,()->laid,()->Things.HOME));
            FrameLayout.LayoutParams spot=new FrameLayout.LayoutParams(Math.round(strideX()),Math.round(strideY()));
            spot.leftMargin=Math.round(baseX+(px-pageX)*strideX());spot.topMargin=Math.round(baseY+(py-pageY)*strideY());
            world.addView(holder,spot);others.add(holder);holders.put(key,holder);
        }
    }

    private Laid homeAt(float x,float y) {
        for(Laid laid:pageLayouts.values())if(cellAt(laid.canvas,laid,x,y)!=null)return laid;
        return null;
    }

    private boolean homeGrid(GridLayout cells) {
        if(cells==grid)return true;
        if(cells!=null&&cells==carriedFrom&&Things.HOME.equals(outOf))return true;
        for(Laid laid:pageLayouts.values())if(laid.canvas==cells)return true;
        return false;
    }

    // ---- a whole page carried, zoomed out (the owner's ask: "grab and move the pages") ---------------------------------

    /** The places a page can be let go in: every page there is, and the empty places round them, one further each way. */
    private java.util.Set<List<Integer>> placesFor() {
        java.util.Set<List<Integer>> active=Layout.active(homeLaid.spots),all=new java.util.LinkedHashSet<>(active);
        for(List<Integer> one:active)for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)
            if(Math.abs(dx)+Math.abs(dy)==1)all.add(java.util.Arrays.asList(one.get(0)+dx,one.get(1)+dy));
        return all;
    }

    /** Which place - a page, or an empty place a page can go - is under a point of the pager, zoomed out as it is; or null. */
    private List<Integer> placeAt(float x,float y) {
        float cx=pager.getWidth()/2f,cy=pager.getHeight()/2f;
        float wx=cx+(x-cx-shiftX)/scaleNow,wy=cy+(y-cy-shiftY)/scaleNow;
        int px=pageX+(int)Math.floor(wx/strideX()),py=pageY+(int)Math.floor(wy/strideY());
        List<Integer> at=java.util.Arrays.asList(px,py);
        return placesFor().contains(at)?at:null;
    }

    /** Held on a page, zoomed out: it is picked up - a little bigger, over the others - and the empty places show. */
    private void liftPage(float x,float y) {
        if(homeLaid==null||homeLaid.spots==null)return;
        int[] q=pageAt(x,y);
        if(q==null)return;
        View holder=holders.get(java.util.Arrays.asList(q[0],q[1]));
        if(holder==null)return;
        carriedPage=q;carriedHolder=holder;
        pager.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        holder.setElevation(a.dp(16));holder.bringToFront();holder.animate().scaleX(1.06f).scaleY(1.06f).alpha(0.9f).setDuration(120);
        for(List<Integer> one:placesFor()) {
            if(holders.containsKey(one))continue;
            View empty=new View(a);
            GradientDrawable dashed=new GradientDrawable();dashed.setCornerRadius(a.dp(22));
            dashed.setStroke(Math.max(1,a.dp(2)),MainActivity.mix(a.INK,a.PAPER,0.5f),a.dp(10),a.dp(8));
            empty.setBackground(dashed);
            float[] at=offset(one.get(0),one.get(1));
            FrameLayout.LayoutParams spot=new FrameLayout.LayoutParams(Math.round(strideX()),Math.round(strideY()));
            spot.leftMargin=Math.round(baseX+at[0]);spot.topMargin=Math.round(baseY+at[1]);
            world.addView(empty,0,spot);
            places.put(one,empty);
        }
        if(gridScroll!=null)gridScroll.announceForAccessibility("Picked up "+said(q[0],q[1]).toLowerCase(java.util.Locale.ROOT)+". Let go on another place.");
    }

    /** The carried page under the finger, and the place it would go lit. */
    private void dragPage(float dx,float dy,float x,float y) {
        if(carriedHolder==null)return;
        carriedHolder.setTranslationX(dx/scaleNow);carriedHolder.setTranslationY(dy/scaleNow);
        List<Integer> under=placeAt(x,y);
        if(java.util.Objects.equals(under,litPlace))return;
        light(litPlace,false);litPlace=under;light(under,true);
    }
    private void light(List<Integer> place,boolean on) {
        if(place==null)return;
        View view=holders.containsKey(place)?holders.get(place):places.get(place);
        if(view==null||view==carriedHolder||!(view.getBackground() instanceof GradientDrawable))return;
        GradientDrawable edge=(GradientDrawable)view.getBackground();
        if(places.containsKey(place))edge.setStroke(Math.max(1,a.dp(on?3:2)),on?a.ACCENT:MainActivity.mix(a.INK,a.PAPER,0.5f),a.dp(10),a.dp(8));
        else if(view==here)edge.setStroke(Math.max(1,a.dp(on?4:3)),a.ACCENT);
        else edge.setStroke(Math.max(1,a.dp(on?3:2)),on?a.ACCENT:MainActivity.mix(a.INK,a.PAPER,0.55f));
    }

    /**
     * The carried page let go: on another page, the two change places; on an empty place, it moves there; anywhere else,
     * back where it was (Layout.movePage). Every icon of Home is written where it stands then, and the page it went to is
     * the one in the middle, still zoomed out.
     */
    private void dropPage(float x,float y) {
        if(carriedHolder==null)return;
        List<Integer> to=placeAt(x,y);
        int[] from=carriedPage;View holder=carriedHolder;
        carriedPage=null;carriedHolder=null;light(litPlace,false);litPlace=null;
        for(View one:places.values())world.removeView(one);
        places.clear();
        holder.setElevation(0);holder.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120);
        if(to==null||to.get(0)==from[0]&&to.get(1)==from[1]){holder.animate().translationX(0).translationY(0).setDuration(160);return;}
        final Map<String,Layout.Spot> spots=Layout.movePage(homeLaid.spots,from[0],from[1],to.get(0),to.get(1));
        // The lines take their new places at once, so Home is drawn with them before the notebook has been written.
        for(NoteStore.Branch line:homeLaid.lines){Layout.Spot at=spots.get(line.id);if(at!=null){line.cell=at.cell();line.page=at.page();}}
        placesOn(spots);
        final List<NoteStore.Branch> lines=new ArrayList<>(homeLaid.lines);
        a.background.submit(()->{a.store.placeOnPages(lines,spots);return null;},done->{},e->{a.alert("Could not move that page. Nothing was changed.");refresh();});
        boolean swapped=homeLaid.spots.values().stream().anyMatch(at->at!=null&&at.on(to.get(0),to.get(1)));
        // The page in view's holder always stands at the middle of the world, whichever page it shows.
        here.animate().cancel();here.setTranslationX(0);here.setTranslationY(0);here.setScaleX(1f);here.setScaleY(1f);here.setAlpha(1f);
        pageX=to.get(0);pageY=to.get(1);focusX=pageX;focusY=pageY;
        fill(homeLaid.lines,homeLaid.places);
        zoomed=true;
        if(allSeen){float[] all=allAt();place(all[0],all[1],all[2]);}else place(SMALL,0,0);
        showFoot();
        a.toast(swapped?"The two pages changed places":"Page moved");
    }

    /** Which page is under a point of the pager, zoomed out as it is now, or null. */
    private int[] pageAt(float x,float y) {
        float cx=pager.getWidth()/2f,cy=pager.getHeight()/2f;
        float wx=cx+(x-cx-shiftX)/scaleNow,wy=cy+(y-cy-shiftY)/scaleNow;
        if(homeLaid==null||homeLaid.spots==null)return null;
        for(List<Integer> page:Layout.active(homeLaid.spots)) {
            float[] at=offset(page.get(0),page.get(1));
            if(wx>=at[0]&&wx<at[0]+strideX()&&wy>=at[1]&&wy<at[1]+strideY())return new int[]{page.get(0),page.get(1)};
        }
        return null;
    }

    /** The page nearest the middle, zoomed out as it is now: where a let-go swipe settles. */
    private int[] nearest() {
        int[] best={pageX,pageY};float least=Float.MAX_VALUE;
        if(homeLaid==null||homeLaid.spots==null)return best;
        for(List<Integer> page:Layout.active(homeLaid.spots)) {
            float[] at=offset(page.get(0),page.get(1));
            float dx=shiftX+scaleNow*at[0],dy=shiftY+scaleNow*at[1],d=dx*dx+dy*dy;
            if(d<least){least=d;best=new int[]{page.get(0),page.get(1)};}
        }
        return best;
    }

    /** Where a page lies, in words: "The main page", "Page one to the right", "Page two up, one to the left". */
    static String said(int x,int y) {
        if(x==0&&y==0)return "The main page";
        List<String> parts=new ArrayList<>();
        if(y!=0)parts.add(count(y)+(y<0?" up":" down"));
        if(x!=0)parts.add(count(x)+(x<0?" to the left":" to the right"));
        return "Page "+TextUtils.join(", ",parts);
    }
    private static String count(int n){int m=Math.abs(n);return m==1?"one":m==2?"two":m==3?"three":String.valueOf(m);}

    /** Something just made with + on the page in view: it stands on that page once Home has read it (decision 46). */
    void placeNew(String id){if(!onCentre())placeNext.put(id,new int[]{seenPageX(),seenPageY()});}

    /** Those made with + on another page than the main one, put there now they are read: the first free cell of that page. */
    private void placeMade() {
        if(placeNext.isEmpty()||homeLaid==null||homeLaid.spots==null)return;
        boolean moved=false;
        for(NoteStore.Branch line:homeLaid.lines) {
            int[] on=placeNext.remove(line.id);
            if(on==null||line.cell>=0)continue;
            Map<String,Layout.Spot> others=new java.util.LinkedHashMap<>(homeLaid.spots);others.remove(line.id);
            final Layout.Spot at=Layout.freeOn(others,on[0],on[1],homeLaid.columns,homeLaid.rows);
            line.cell=at.cell();line.page=at.page();moved=true;
            final NoteStore.Branch kept=line;
            a.background.submit(()->{a.store.placeOnPages(java.util.Collections.singletonList(kept),java.util.Collections.singletonMap(kept.id,at));return null;},done->{},e->{});
        }
        if(moved)fill(homeLaid.lines,homeLaid.places);
    }

    /** Home's icons written where they stand, each on its page, the places' in this phone's settings. On the worker. */
    private void writeHome(List<NoteStore.Branch> lines,Map<String,Layout.Spot> spots){a.store.placeOnPages(lines,spots);placesOn(spots);}

    /** For an empty cell of the page in view let go in: every icon of Home pinned where it stands, that one in its new cell. */
    private Map<String,Layout.Spot> homeMove(Laid in,String id,int row,int column) {
        return in.spots==null?null:Layout.moveTo(in.spots,id,new Layout.Spot(in.pageX,in.pageY,row,column));
    }

    /** Whether a point of the pager is over an icon of the page in view: a double tap there opens it, not the main page. */
    private boolean overIcon(float x,float y) {
        Rect room=rect(pager);Laid laid=homeAt(x+room.left,y+room.top);
        if(laid==null)return false;
        int[] at=cellAt(laid.canvas,laid,x+room.left,y+room.top);
        return at!=null&&Layout.at(laid.drawn,at[0],at[1])!=null;
    }

    /**
     * Swipes, pinches and taps on Home's pages (decision 45), as a phone's own home screen takes them. A swipe moves the
     * pages under the finger, the next one coming in beside it, and let go it settles on whichever page it was taken to
     * - back where it was if it was not taken far. A finger held first is the icon's, picked up as it always was. Two
     * fingers pinched shrink the pages as they move, each into its frame, and let go small they stay so: then a swipe
     * moves between them, a tap goes into one, and two fingers spread go into the one in the middle. A double tap on the
     * empty room goes back to the main page.
     */
    private final class Turner {
        private float downX,downY,fromX,fromY,startScale;private boolean swiping,scaling,downOnIcon;
        private int axis;
        private boolean centring;
        private long downAt;
        private long heardTime=-1;
        private int heardAction=-1,heardPointers;
        private float heardX,heardY;
        private final int slop=ViewConfiguration.get(a).getScaledTouchSlop();
        private android.view.VelocityTracker speed;
        /** Zoomed out, a finger held still on a page picks the page up. */
        private final Runnable holdPage=()->{if(zoomed&&!swiping&&!scaling)liftPage(downX,downY);};
        private final android.view.GestureDetector taps=new android.view.GestureDetector(a,new android.view.GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDoubleTap(android.view.MotionEvent e) {
                if(!overIcon(e.getX(),e.getY())&&a.heldTile==null&&a.dragging==null) {
                    centring=true;zoomed=false;allSeen=false;panning=false;
                    pager.removeCallbacks(holdPage);go(0,0);return true;
                }
                return false;
            }
        });
        /**
         * The pinch: the pages grow and shrink about the point between the fingers, which stays under them, as a map does;
         * let go, they settle on whichever is nearest - the page itself, it zoomed out with the next ones beside it, or
         * every page at once.
         */
        private final android.view.ScaleGestureDetector pinch=new android.view.ScaleGestureDetector(a,new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener(){
            float total=1f,heldX,heldY;
            @Override public boolean onScaleBegin(android.view.ScaleGestureDetector d) {
                stopScroll();
                if(moving!=null){moving.cancel();moving=null;}
                pager.removeCallbacks(holdPage);
                total=1f;scaling=true;swiping=false;startScale=scaleNow;
                // The point of the world under the fingers.
                float cx=pager.getWidth()/2f,cy=pager.getHeight()/2f;
                heldX=cx+(d.getFocusX()-cx-shiftX)/scaleNow;heldY=cy+(d.getFocusY()-cy-shiftY)/scaleNow;
                return true;
            }
            @Override public boolean onScale(android.view.ScaleGestureDetector d) {
                total*=d.getScaleFactor();
                float least=Math.min(SMALL,allAt()[0])*0.85f;
                float now=Math.max(least,Math.min(1.05f,startScale*total));
                float cx=pager.getWidth()/2f,cy=pager.getHeight()/2f;
                show(now,d.getFocusX()-cx-(heldX-cx)*now,d.getFocusY()-cy-(heldY-cy)*now);
                return true;
            }
            @Override public void onScaleEnd(android.view.ScaleGestureDetector d) {
                float all=allAt()[0];
                int[] middle=nearest();
                if(scaleNow>0.85f)zoomInto(middle[0],middle[1]);
                else if(all>=SMALL-0.01f||scaleNow>(SMALL+all)/2f)zoomTo(middle[0],middle[1],180);
                else{zoomed=true;allSeen=true;float[] to=allAt();move(to[0],to[1],to[2],180,null);}
            }
        });
        boolean intercept(android.view.MotionEvent e) {
            if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)centring=false;
            else hear(e);
            if(centring)return true;
            switch(e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    stopScroll();
                    if(moving!=null){moving.cancel();moving=null;}
                    downX=e.getX();downY=e.getY();fromX=shiftX;fromY=shiftY;swiping=false;scaling=false;
                    axis=HomeMotion.NONE;
                    downOnIcon=!zoomed&&overIcon(e.getX(),e.getY());downAt=e.getEventTime();
                    // Let a double tap start its return after cancelling the previous motion.
                    hear(e);
                    // Zoomed out, the pages are pictures: every touch is the pager's.
                    // The same press that stops momentum can still hold an icon for its menu or drag.
                    return zoomed||centring;
                // Two fingers are a pinch, not anything for the icons under them.
                case android.view.MotionEvent.ACTION_POINTER_DOWN: return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    if(zoomed||scaling||e.getPointerCount()>1)return true;
                    float dx=e.getX()-downX,dy=e.getY()-downY;
                    float moved=Math.max(Math.abs(dx),Math.abs(dy));
                    // A press on an icon stays with the icon. A clear slide before the hold still moves the pages (decision 130).
                    if(Layout.pageTakes(downOnIcon,a.heldTile!=null,a.dragging!=null,moved,slop,e.getEventTime()-downAt,ViewConfiguration.getLongPressTimeout())){
                        if(!swiping)axis=HomeMotion.axis(dx,dy);
                        swiping=true;panning=true;return true;
                    }
                    return false;
                default: return false;
            }
        }
        void touch(android.view.MotionEvent e) {
            hear(e);
            if(centring)return;
            switch(e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX=e.getX();downY=e.getY();fromX=shiftX;fromY=shiftY;swiping=false;scaling=false;
                    axis=HomeMotion.NONE;
                    if(zoomed)pager.postDelayed(holdPage,ViewConfiguration.getLongPressTimeout());
                    break;
                case android.view.MotionEvent.ACTION_POINTER_DOWN: pager.removeCallbacks(holdPage);break;
                case android.view.MotionEvent.ACTION_MOVE: {
                    if(carriedHolder!=null){dragPage(e.getX()-downX,e.getY()-downY,e.getX(),e.getY());break;}
                    if(scaling||e.getPointerCount()>1)break;
                    float dx=e.getX()-downX,dy=e.getY()-downY;
                    if(Math.max(Math.abs(dx),Math.abs(dy))>slop*2)pager.removeCallbacks(holdPage);
                    if(!swiping&&Math.max(Math.abs(dx),Math.abs(dy))>slop*2){swiping=true;panning=true;axis=HomeMotion.axis(dx,dy);}
                    if(!swiping)break;
                    if(zoomed){place(scaleNow,fromX+dx,fromY+dy);break;}
                    float[] limits=scrollLimits();
                    // One direction per swipe; two fingers in the overview still move freely.
                    place(1f,Math.max(limits[0],Math.min(limits[1],fromX+(axis==HomeMotion.HORIZONTAL?dx:0))),
                        Math.max(limits[2],Math.min(limits[3],fromY+(axis==HomeMotion.VERTICAL?dy:0))));
                    showFoot();
                    break;
                }
                case android.view.MotionEvent.ACTION_UP: {
                    panning=false;
                    pager.removeCallbacks(holdPage);
                    if(carriedHolder!=null){dropPage(e.getX(),e.getY());break;}
                    if(scaling){scaling=false;break;}
                    float dx=e.getX()-downX,dy=e.getY()-downY;
                    float vx=0,vy=0;
                    if(speed!=null){speed.computeCurrentVelocity(1000);vx=speed.getXVelocity();vy=speed.getYVelocity();}
                    if(zoomed) {
                        if(!swiping){int[] tapped=pageAt(e.getX(),e.getY());if(tapped!=null)zoomInto(tapped[0],tapped[1]);break;}
                        // Every page at once stays so; one in the middle settles on whichever is nearest the middle now.
                        if(allSeen){float[] all=allAt();move(all[0],all[1],all[2],200,null);break;}
                        int[] to=nearest();zoomTo(to[0],to[1],200);
                        break;
                    }
                    if(!swiping)break;
                    swiping=false;
                    // A flick carries on a little, the way the finger was going, and then it rests there. It does not snap
                    // onto a page.
                    int minimum=ViewConfiguration.get(a).getScaledMinimumFlingVelocity();
                    int maximum=ViewConfiguration.get(a).getScaledMaximumFlingVelocity();
                    vx=axis==HomeMotion.HORIZONTAL?Math.max(-maximum,Math.min(maximum,vx)):0;
                    vy=axis==HomeMotion.VERTICAL?Math.max(-maximum,Math.min(maximum,vy)):0;
                    if(Math.abs(vx)>minimum||Math.abs(vy)>minimum)fling(vx,vy);else rest();
                    a.pressing=false;
                    break;
                }
                case android.view.MotionEvent.ACTION_CANCEL:
                    panning=false;
                    pager.removeCallbacks(holdPage);
                    if(carriedHolder!=null){dropPage(-1,-1);break;}
                    if(!zoomed&&swiping){swiping=false;rest();}
                    a.pressing=false;
                    break;
                default:
            }
        }
        private void follow(android.view.MotionEvent e) {
            if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){if(speed!=null)speed.recycle();speed=android.view.VelocityTracker.obtain();}
            if(speed!=null)speed.addMovement(e);
        }
        private void hear(android.view.MotionEvent e) {
            // Interception and handling can see the same event. Feed the gesture detectors once.
            if(heardTime==e.getEventTime()&&heardAction==e.getAction()&&heardPointers==e.getPointerCount()
                    &&heardX==e.getX()&&heardY==e.getY())return;
            heardTime=e.getEventTime();heardAction=e.getAction();heardPointers=e.getPointerCount();heardX=e.getX();heardY=e.getY();
            follow(e);taps.onTouchEvent(e);pinch.onTouchEvent(e);
        }
    }

    /**
     * The dots at the foot of Home's grid, one for each page where it lies (the one in view among them, even empty), nothing
     * while there is one; the filled one goes with the finger as the pages slide, as a phone's own home screen's does.
     */
    private final class PageDots extends View {
        PageDots(){super(a);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        @Override protected void onDraw(android.graphics.Canvas canvas) {
            if(homeLaid==null||homeLaid.spots==null)return;
            java.util.Set<List<Integer>> pages=Layout.dotted(homeLaid.spots,pageX,pageY);
            if(pages.size()<=1){fitRows(1);return;}
            int minX=0,maxX=0,minY=0,maxY=0;
            for(List<Integer> one:pages){minX=Math.min(minX,one.get(0));maxX=Math.max(maxX,one.get(0));minY=Math.min(minY,one.get(1));maxY=Math.max(maxY,one.get(1));}
            // As tall as its rows of dots. It was one row tall, so with a page above and one below only the middle row
            // was inside it and the rest were cut off - the owner saw one dot left while carrying a thing up.
            if(fitRows(maxY-minY+1))return;
            float step=a.dp(12);float x0=(getWidth()-(maxX-minX)*step)/2f,y0=getHeight()/2f-(maxY-minY)*step/2f;
            paint.setColor(MainActivity.mix(a.INK,a.PAPER,0.6f));
            for(List<Integer> one:pages)canvas.drawCircle(x0+(one.get(0)-minX)*step,y0+(one.get(1)-minY)*step,a.dp(3),paint);
            // Where the pages stand now, part way from one to the next while they slide.
            float seenX=pageX,seenY=pageY;
            if(pager!=null&&pager.getWidth()>0){seenX-=shiftX/strideX();seenY-=shiftY/strideY();}
            seenX=Math.max(minX,Math.min(maxX,seenX));seenY=Math.max(minY,Math.min(maxY,seenY));
            paint.setColor(a.ACCENT);
            canvas.drawCircle(x0+(seenX-minX)*step,y0+(seenY-minY)*step,a.dp(4),paint);
        }
        /** Made tall enough for so many rows of dots, growing up from the foot. Whether it changed, and is to be drawn again. */
        private boolean fitRows(int rows) {
            android.view.ViewGroup.LayoutParams now=getLayoutParams();
            int tall=a.dp(18)+Math.max(0,rows-1)*a.dp(12);
            if(now==null||now.height==tall)return false;
            now.height=tall;
            post(this::requestLayout);
            return true;
        }
    }

    /** How many icons go across a width in pixels: see {@link Grid#columns}. */
    int columns(int widthPx){return Grid.columns(widthPx/a.getResources().getDisplayMetrics().density);}

    /**
     * The bar as it always was at the top: the app's name and which build this is, the mark for everything on this
     * device, and ⋮ with the app's rows. Nothing at the left, where a level further in has its arrow back, so the name
     * stands in the middle as it does on every other screen.
     */
    private LinearLayout bar() {
        LinearLayout top=a.bar();
        // The app's icon and its name at the left, not its name alone in the middle; a tap is your Profile, where everything
        // about you is set (the owner, 2026-10-17: "put it on the left with the icon we have for it ... clicked from the main
        // page it should bring to the My profile page").
        LinearLayout brand=new LinearLayout(a);brand.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView logo=new android.widget.ImageView(a);logo.setImageResource(R.mipmap.ic_launcher);
        brand.addView(logo,new LinearLayout.LayoutParams(a.dp(30),a.dp(30)));
        TextView called=a.label(a.getString(R.string.app_name),MainActivity.READING,a.INK);
        called.setTypeface(called.getTypeface(),android.graphics.Typeface.BOLD);called.setSingleLine(true);called.setEllipsize(TextUtils.TruncateAt.END);
        called.setPadding(a.dp(8),0,0,0);brand.addView(called);
        brand.setMinimumHeight(a.dp(48));brand.setPadding(a.dp(4),0,a.dp(8),0);
        brand.setBackgroundResource(a.borderlessFeedback());
        brand.setContentDescription(Say.t("Mininotes: your Profile"));
        brand.setOnClickListener(v->a.profile());
        // Which build this is lives in the menu, under Mininotes (the owner, 2026-10-12: "remove the version in the header on
        // mobile and put it only in the menu").
        a.versionLine=null;
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        // What is new here, said at the top and listed on a tap (the owner, 2026-10-14: "it should be at the folder level too
        // and even at the top of the app, and clicking it ... should give us the list of what they are"). Shown only while
        // anything is new.
        newMark=a.label("new",MainActivity.QUIET,a.PAPER);
        newMark.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);newMark.setIncludeFontPadding(false);
        newMark.setPadding(a.dp(8),a.dp(3),a.dp(8),a.dp(4));
        GradientDrawable newPill=new GradientDrawable();newPill.setColor(a.ACCENT);newPill.setCornerRadius(a.dp(11));newMark.setBackground(newPill);
        newMark.setVisibility(View.GONE);newMark.setOnClickListener(v->a.whatIsNew());
        LinearLayout.LayoutParams beside=new LinearLayout.LayoutParams(-2,-2);beside.gravity=Gravity.CENTER_VERTICAL;beside.setMargins(0,0,a.dp(6),0);
        top.addView(newMark,beside);
        homeMark=a.syncMark(home());
        top.addView(homeMark);
        top.addView(a.agentButton());
        top.addView(a.tap("⋮","Home",22,a.INK,v->a.menuFor(v,home())));
        return top;
    }

    /**
     * The + : a round button, with no word on it, as a phone's own is - named for anybody who cannot see it. It offers
     * the two things there are, for where it is: Home's makes them on Home, a pop-up's in that collection. Every + also
     * takes something from another device - a note or a collection someone shows the code of, scanned or pasted - which
     * arrives on Home, as the box that takes it says (the owner: "this is key").
     *
     * @param where the collection it makes things in, asked when it is pressed, so a pop-up that has moved on is followed
     */
    View plus(final java.util.function.Supplier<String> where) {
        // Drawn by MainActivity.roundPlus, as Stamped on Minima's is (decision 231).
        TextView plus=a.roundPlus("New",null);
        // And files from this device, photos and anything else, wherever there is a + (decision 87).
        // Take a photo keeps the picture in this folder, or on Home, without a picker (decision 123).
        plus.setOnClickListener(v->{
            // An album's + adds pictures first (decision 217): several from the phone's picker at once, or one from the camera.
            String in=where.get();
            if(in!=null&&!NoteStore.home(in)&&!MainActivity.takesNew(in)&&a.store.album(in)){a.heldMenu(v,null,"Add photos…",(Runnable)()->a.addPhotos(in),
                "Take a photo",(Runnable)()->a.takePhoto(in),null,"Note",(Runnable)()->newNote(in),"Folder",(Runnable)()->newCollection(in),
                "Record a note",(Runnable)()->recordNote(in),"From this device…",(Runnable)()->a.fromThisDevice(in),null,"From another device…",(Runnable)a::addFromSomeone);return;}
            a.heldMenu(v,null,"Note",(Runnable)()->newNote(where.get()),"Folder",(Runnable)()->newCollection(where.get()),
            "Take a photo",(Runnable)()->a.takePhoto(where.get()),"Record a note",(Runnable)()->recordNote(where.get()),
            "From this device…",(Runnable)()->a.fromThisDevice(where.get()),null,"From another device…",(Runnable)a::addFromSomeone);});
        return plus;
    }

    /** Where a + sits: the foot of what it adds to, on the right, where the thumb is. */
    FrameLayout.LayoutParams plusPlace() {
        FrameLayout.LayoutParams place=new FrameLayout.LayoutParams(a.dp(56),a.dp(56),Gravity.BOTTOM|Gravity.END);
        place.setMargins(0,0,a.dp(18),a.dp(16));
        return place;
    }

    /**
     * The search bar above the dock: a rounded, quiet field that says Search, and opens the search that finds notes,
     * collections and files alike, by name and by what they say.
     */
    private View searchBar() {
        TextView search=a.label("Search",MainActivity.READING,a.MUTED);
        search.setGravity(Gravity.CENTER_VERTICAL);search.setSingleLine(true);
        search.setMinimumHeight(a.dp(48));search.setPadding(a.dp(20),0,a.dp(20),0);
        GradientDrawable field=new GradientDrawable();
        field.setColor(a.CARD);field.setCornerRadius(a.dp(24));field.setStroke(Math.max(1,a.dp(1)),a.LINE);
        search.setBackground(field);
        search.setForeground(a.getDrawable(a.touchFeedback()));
        search.setContentDescription("Search notes, folders and files");
        search.setOnClickListener(v->a.searching());
        return search;
    }

    private LinearLayout.LayoutParams searchPlace() {
        LinearLayout.LayoutParams place=new LinearLayout.LayoutParams(-1,-2);
        place.setMargins(a.dp(16),a.dp(6),a.dp(16),a.dp(2));
        return place;
    }

    // ---- reading it again ----------------------------------------------------------------------------------------

    /**
     * Home read again, and the pop-up and the dock with it: after anything changes, here or on another device. What the
     * reader is looking at does not move - each grid keeps where it was scrolled to, and an open pop-up stays open on the
     * collection it shows - unless that collection has gone, which is then said.
     */
    void refresh() {
        if(!showing())return;
        // Not under a thing being carried, whose icon would be taken out from under the finger, nor under a name being
        // typed, whose field would go with the row it is in. Each ends by reading Home again.
        if(a.dragging!=null||a.pressing||a.heldTile!=null||panning||scrolling||naming()){stale=true;return;}
        stale=false;
        final int read=++reads;
        final List<MainActivity.Step> path=new ArrayList<>(a.trail);
        final boolean away=true;
        a.background.submit(()->{
            // The icons' set is read here the first time, off the screen's thread, before a face is drawn with it.
            Icons.all();
            Map<String,Object> got=new HashMap<>();
            // Everything in a shared folder shared (decision 195): what that put in one for real goes now.
            List<String> made=a.store.intoSharedFolders();
            if(!made.isEmpty())a.network.submit(()->{for(String id:made)Post.send(a,a.store,a.keys(),NoteStore.Branch.Kind.PAGE,id);return null;},v->{},e->{});
            got.put("home",a.store.contents(Things.HOME));
            got.put("beyond",a.store.favouritesAll());
            got.put("dock",a.store.dock());
            got.put("colour",a.libraryColour());
            // And how strongly it lands, Home's own (decision 107).
            got.put("tone",a.homeTone());
            // How many things wait in the archive and the bin, for the count on each (decision 41).
            // What is temporary and whose time has come goes first, so it is never drawn once more (decision 71).
            a.store.expire(System.currentTimeMillis());
            got.put("archived",a.store.awayCount(false));got.put("binned",a.store.awayCount(true));got.put("temp",a.store.temporaryCount());
            // Temp's own mark and whether anything in it is new, for its tile (the owner, 2026-10-14).
            got.put("tempMark",a.store.markOf(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP));got.put("tempNew",a.store.tempHasNew());
            got.put("newHere",a.store.whatIsNew().size());
            // And how many things wait for an answer in Shared with me, for the count it wears (decision 109).
            got.put("asking",a.store.waitingCount());
            // What a code accepted here is bringing, standing on Home until it comes (decision 73).
            got.put("waiting",a.store.waitingOnHome());
            // And what is coming to Home on its own, where Settings shows files shared with you there (decision 94).
            got.put("coming",a.store.coming(Things.HOME));
            if(path.size()>1)folder.read(path,got);
            return got;
        },got->{
            if(read!=reads||!showing())return;
            if(a.dragging!=null||a.pressing||a.heldTile!=null||panning||scrolling||naming()){stale=true;return;}
            a.levelColour=(Integer)got.get("colour");a.levelTone=(Integer)got.get("tone");a.repaint();
            @SuppressWarnings("unchecked") List<NoteStore.Branch> lines=(List<NoteStore.Branch>)got.get("home");
            @SuppressWarnings("unchecked") List<NoteStore.Branch> beyond=(List<NoteStore.Branch>)got.get("beyond");
            @SuppressWarnings("unchecked") List<NoteStore.Branch> docked=(List<NoteStore.Branch>)got.get("dock");
            List<NoteStore.Branch> places=new ArrayList<>();
            // Home's places, each on Home unless switched off in Home's menu (decision 78); Favourites while there is one.
            if(!beyond.isEmpty()&&a.onHome(NoteStore.FAVOURITES))places.add(favourites());
            NoteStore.Branch inTemp=temp((Integer)got.get("temp"));
            inTemp.standing=(SyncMark)got.get("tempMark");inTemp.fresh=Boolean.TRUE.equals(got.get("tempNew"));
            for(NoteStore.Branch one:new NoteStore.Branch[]{recent(),inTemp,shared((Integer)got.get("asking")),
                    archive((Integer)got.get("archived")),bin((Integer)got.get("binned"))})if(a.onHome(one.id))places.add(one);
            @SuppressWarnings("unchecked") List<NoteStore.Branch> waiting=(List<NoteStore.Branch>)got.get("waiting");
            if(waiting!=null)places.addAll(waiting);
            @SuppressWarnings("unchecked") List<NoteStore.Branch> coming=(List<NoteStore.Branch>)got.get("coming");
            if(coming!=null)places.addAll(coming);
            fill(lines,places);
            placeMade();
            // The page in view gone empty - its last icon moved or put away - is gone: the main page instead.
            if(homeLaid!=null&&homeLaid.spots!=null&&!Layout.active(homeLaid.spots).contains(java.util.Arrays.asList(pageX,pageY)))showPage(0,0,0,0);
            if(dots!=null)dots.invalidate();
            dock.fill(docked);
            if(path.size()>1&&folder.isOpen())folder.fill(path,got);
            if(homeMark!=null)a.askOwed(homeMark,home());
            if(newMark!=null){Integer many=(Integer)got.get("newHere");int n=many==null?0:many;
                newMark.setText(n>1?Say.t("new")+" · "+n:Say.t("new"));newMark.setVisibility(n>0?View.VISIBLE:View.GONE);
                newMark.setContentDescription(n==1?"One thing is new. Tap to see it.":n+" things are new. Tap to see them.");}
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /**
     * Something a code accepted here is bringing, tapped: who it is waited for from, and the one thing to do, to stop
     * waiting. It turns into the thing itself when it comes.
     */
    private void waiting(final NoteStore.Branch line) {
        // A file shared with you, still being fetched (decision 94): nothing to do but know it is coming.
        if(line.id.startsWith(NoteStore.COMING)) {
            a.new Box().setTitle(line.name).setMessage(line.detail+". It opens here once all of it has come.").setPositiveButton("OK",(d,w)->{}).show();
            return;
        }
        final String address=line.id.startsWith("waiting:")?line.id.substring(8):line.id;
        a.new Box().setTitle(line.name)
            .setMessage(line.detail+". It comes when their phone is next open, and takes this place on Home.")
            .setPositiveButton("Keep waiting",(d,w)->{})
            .setNeutralButton("Stop waiting",(d,w)->a.background.submit(()->{a.store.stopWaiting(address);return null;},done->refresh(),e->a.alert(MainActivity.READ_FAILED)))
            .show();
    }

    /** Over the Favourites card while carrying one of them: the favourite under the finger is ringed, the place it goes before. */
    private void reorderOver(float x,float y) {
        aim(null,null);
        int[] at=cellAt(folder.grid,folder.laid,x,y);
        String there=at==null?null:Layout.at(folder.laid.drawn,at[0],at[1]);
        a.markOnto(there==null||there.equals(a.dragging.id)?null:folder.laid.icons.get(there));
    }

    /** One favourite put before another in their one order, or last; the dock keeps its own (decision 190). */
    private void reorder(final NoteStore.Branch carried,final NoteStore.Branch before) {
        final List<NoteStore.Branch> order=new ArrayList<>(folder.laid.lines);
        order.removeIf(one->one.id.equals(carried.id));
        int at=order.size();
        if(before!=null)for(int i=0;i<order.size();i++)if(order.get(i).id.equals(before.id)){at=i;break;}
        order.add(at,carried);
        final int place=at+1;
        a.background.submit(()->{a.store.orderFavourites(order);return null;},done->{a.toast("Favourite "+place);refresh();},
            e->{a.alert("Could not put that there. Nothing was changed.");refresh();});
    }

    /** Whether a name is being typed somewhere on Home: redrawn now, the field would be taken away mid-word. */
    private boolean naming(){return a.naming!=null&&a.naming.isAttachedToWindow();}

    /**
     * Home's grid, drawn again where it stands: each icon in its cell, and Home's places among them - the Favourites
     * collection when it holds anything, the archive and the bin unless they are switched off (see {@link #lay}).
     */
    private void fill(List<NoteStore.Branch> lines,List<NoteStore.Branch> places) {
        final int kept=gridScroll.getScrollY();
        int width=pager.getWidth()>0?pager.getWidth():a.getResources().getDisplayMetrics().widthPixels;
        grid.setColumnCount(columns(width));
        homeLaid=lay(grid,lines,places,width,Where.HOME,
            homeRoom(),"Nothing here yet. Tap + to make a note or a folder.");
        if(a.dragging==null)beyond=false;
        buildPages();
        landedId=null;
        // Scrolled back to where it was once the rows are laid out again: an arrival is not a reason to lose your place.
        gridScroll.post(()->gridScroll.scrollTo(0,kept));
        if(!zoomed&&!panning&&!scrolling)rest();
    }

    // ---- icons where they were put (docs/HOME.md, decision 39) ------------------------------------------------------

    /**
     * One grid as it was last drawn: where each icon stands and what it is, the empty cells between, and how big a cell
     * is - what a finger over the grid is measured against, and what a thing let go in an empty cell is written from.
     */
    static final class Laid {
        /** Each icon's {row, column}, by id; a place's under its own id ({@link NoteStore#FAVOURITES} and the rest). */
        final Map<String,int[]> drawn;
        /** The lines drawn, as the notebook gave them: whose cells {@link NoteStore#place} writes. */
        final List<NoteStore.Branch> lines;
        /** Home's places drawn among them - the Favourites collection, the archive, the bin - whose cells this phone keeps. */
        final List<NoteStore.Branch> places;
        /** On Home, where every icon stands on every page (Layout.pages); null for a card's one grid. */
        Map<String,Layout.Spot> spots;
        GridLayout canvas;
        int pageX,pageY;
        final Where where;
        /** Each icon's line and view by id, and each empty cell's view by {@link Layout#cell}. */
        final Map<String,NoteStore.Branch> things=new HashMap<>();
        final Map<String,View> icons=new HashMap<>();
        final Map<Integer,View> spaces=new HashMap<>();
        /**
         * Columns and rows drawn, a cell's width, and the height a row is drawn at. {@code natural} is that height at the
         * reading size, before a Home page stretches its rows to fill the screen. {@code seen} is how tall the grid was
         * when it was laid out.
         */
        final int columns,rows,width,height,seen,natural;

        Laid(Map<String,int[]> drawn,List<NoteStore.Branch> lines,List<NoteStore.Branch> places,Where where,int columns,int rows,int width,int height,int seen,int natural) {
            this.drawn=drawn;this.lines=lines;this.places=places;this.where=where;
            this.columns=columns;this.rows=rows;this.width=width;this.height=height;this.seen=seen;this.natural=natural;
        }
    }

    /**
     * A grid's icons, each in its cell (decision 39): where it was put, or - put nowhere yet - the first free cell from the
     * top, in the owner's order. Every cell between is an empty place the size of an icon, so the grid does not close them
     * up and a thing can be let go in any of them; there is always a free row under the last icon, and rows enough to fill
     * the height the grid is seen in, so there is always somewhere to put a thing. Added row by row, so a screen reader
     * goes through them in the order they are seen.
     *
     * @param places     Home's places, to draw among them where each was put (see {@link Layout#home}), or none
     * @param widthPx    how wide the grid is drawn, for its columns
     * @param seen       how tall the grid is seen, in pixels, or 0 before it has been measured
     * @param nothingYet what an empty grid says to do
     */
    Laid lay(GridLayout in,List<NoteStore.Branch> lines,List<NoteStore.Branch> places,int widthPx,Where where,int seen,String nothingYet) {
        in.removeAllViews();
        int columns=Math.max(1,in.getColumnCount()),cell=cell(in,widthPx),high=rowHeight(cell),natural=high;
        List<String> ids=new ArrayList<>();List<Integer> cells=new ArrayList<>();
        // Among the favourites, the archive and the bin a thing is only listed, not kept, so it has no cell there: they
        // stand one after the other.
        boolean listed=where==Where.FAVOURITES||where==Where.AWAY;
        for(NoteStore.Branch one:lines){ids.add(one.id);cells.add(listed?Layout.NONE:one.cell);}
        int least=seen>0?seen:a.getResources().getDisplayMetrics().heightPixels;
        Map<String,int[]> drawn;Map<String,Layout.Spot> spots=null;int rows;
        if(where==Where.HOME) {
            // A page ends at a whole row. The last row may be partly below the viewport; no row is stretched to fill it.
            int inner=Math.max(1,least-in.getPaddingTop()-in.getPaddingBottom());
            rows=HomeMotion.rows(inner,high);
            List<String> all=new ArrayList<>();List<Integer> onPages=new ArrayList<>(),cellsOn=new ArrayList<>();
            NoteStore.Branch lead=null;
            for(NoteStore.Branch place:places)if(place.kind==NoteStore.Branch.Kind.FAVOURITES&&placeCell(place.id)<0)lead=place;
            if(lead!=null){all.add(lead.id);onPages.add(Layout.NO_PAGE);cellsOn.add(Layout.NONE);}
            for(NoteStore.Branch one:lines){all.add(one.id);onPages.add(one.page);cellsOn.add(one.cell);}
            for(NoteStore.Branch place:places)if(place!=lead){all.add(place.id);onPages.add(placePage(place.id));cellsOn.add(placeCell(place.id));}
            spots=Layout.pages(all,onPages,cellsOn,new java.util.HashSet<>(java.util.Arrays.asList(NoteStore.ARCHIVE,NoteStore.BIN)),columns,rows);
            drawn=new java.util.LinkedHashMap<>();
            for(Map.Entry<String,Layout.Spot> one:spots.entrySet())
                if(one.getValue()!=null&&one.getValue().on(pageX,pageY))drawn.put(one.getKey(),new int[]{one.getValue().row(),one.getValue().column()});
        } else {
            Map<String,Integer> at=new java.util.LinkedHashMap<>();
            for(NoteStore.Branch place:places)at.put(place.id,placeCell(place.id));
            drawn=Layout.home(ids,cells,at,NoteStore.FAVOURITES,columns);
            rows=Math.max(Layout.rows(drawn)+1,(int)Math.ceil((least-in.getPaddingTop()-in.getPaddingBottom())/(double)high));
        }
        Laid laid=new Laid(drawn,lines,places,where,columns,rows,cell,high,seen,natural);
        laid.spots=spots;
        if(where==Where.HOME){laid.canvas=in;laid.pageX=pageX;laid.pageY=pageY;}
        if(lines.isEmpty()&&places.isEmpty()) {
            // Nothing to carry and nowhere to carry it: the words saying what to do, and no empty cells under them.
            View said=nothing(in,nothingYet,widthPx);
            ((GridLayout.LayoutParams)said.getLayoutParams()).rowSpec=GridLayout.spec(0);
            in.addView(said);
            return laid;
        }
        // Nothing of the owner's yet, only the places: the words saying what to do in the row under them, on the main page.
        int sayingRow=lines.isEmpty()&&(where!=Where.HOME||onCentre())?Layout.rows(drawn):-1;
        if(sayingRow>=rows)sayingRow=-1;
        Map<Integer,String> byCell=new HashMap<>();
        for(Map.Entry<String,int[]> one:drawn.entrySet())byCell.put(Layout.cell(one.getValue()[0],one.getValue()[1]),one.getKey());
        Map<String,NoteStore.Branch> byId=new HashMap<>();
        for(NoteStore.Branch one:lines)byId.put(one.id,one);
        for(NoteStore.Branch one:places)byId.put(one.id,one);
        for(int row=0;row<rows;row++)for(int column=0;column<columns;column++) {
            if(row==sayingRow) {
                if(column==0){View said=nothing(in,nothingYet,widthPx);((GridLayout.LayoutParams)said.getLayoutParams()).rowSpec=GridLayout.spec(row);
                    said.setOnLongClickListener(v->{roomHeld(v,where);return true;});in.addView(said);}
                continue;
            }
            String id=byCell.get(Layout.cell(row,column));
            View view;
            if(id==null) {
                view=new View(a);
                view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                // Held down, the room's menu - Home's, or the card's - as a right-click on it on the PC (decision 44).
                view.setOnLongClickListener(v->{roomHeld(v,where);return true;});
                GridLayout.LayoutParams place=new GridLayout.LayoutParams();
                place.width=cell;place.height=high;
                view.setLayoutParams(place);
                laid.spaces.put(Layout.cell(row,column),view);
            } else {
                NoteStore.Branch one=byId.get(id);
                view=Grid.place(one.kind)?placeIcon(one,cell):icon(one,cell,where);
                // The one being carried, met again on its own page while it is carried over others: still in the hand.
                if(where==Where.HOME&&a.dragging!=null&&id.equals(a.dragging.id))view.setVisibility(View.INVISIBLE);
                // As tall as a name of two lines at the least, so every row is one height and a cell is found by sum.
                view.setMinimumHeight(high);
                laid.things.put(id,one);laid.icons.put(id,view);
            }
            GridLayout.LayoutParams place=(GridLayout.LayoutParams)view.getLayoutParams();
            place.rowSpec=GridLayout.spec(row);place.columnSpec=GridLayout.spec(column);
            in.addView(view,place);
        }
        return laid;
    }

    /** The empty room of a grid held down: the menu of what the grid shows - Home, the card's collection, or the place. */
    private void roomHeld(View on,Where where) {
        if(where==Where.HOME){a.roomFor(on,home());return;}
        NoteStore.Branch shown=folder.room();
        if(shown==null)return;
        if(where==Where.CARD)a.roomFor(on,shown);else a.heldFor(on,shown);
    }

    /** How tall a row of icons is: an icon whose name takes its two lines, at the reading size the pad is at now. */
    int rowHeight(int cell) {
        TextView name=a.label("Ag\nAg",MainActivity.READING,a.INK);
        name.setMaxLines(2);name.setPadding(0,a.dp(6),0,0);
        name.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,cell),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        return a.dp(8)+face(cell)+name.getMeasuredHeight()+a.dp(6);
    }

    /**
     * A grid laid out again where what it was laid out for has changed: the height it is seen in, it is first drawn before
     * the screen has measured it, or the size of its names, after the reading ladder. A row stretched to fill the page is
     * still that row, so the taller drawing is not a change (decision 130). Never under a thing being carried or a name
     * being typed, each of which ends by reading Home again.
     */
    void refit(ScrollView scroll,Laid laid,Runnable again) {
        if(laid==null||scroll==null||a.dragging!=null||a.pressing||a.heldTile!=null||panning||scrolling||naming())return;
        int height=laid.where==Where.HOME?homeRoom():scroll.getHeight();
        boolean wider=laid.where==Where.HOME&&pager!=null&&pager.getWidth()>0
            &&(columns(pager.getWidth())!=laid.columns||cell(grid,pager.getWidth())!=laid.width);
        if(!wider&&!Layout.gridChanged(height,laid.seen,rowHeight(laid.width),laid.natural))return;
        scroll.post(again);
    }

    /** Home's grid, laid out again where the room or the reading size has changed. The finger lifting asks this once. */
    void refitNow(){refit(gridScroll,homeLaid,()->{if(homeLaid!=null&&showing())fill(homeLaid.lines,homeLaid.places);});}

    /** Home's places, by id: each keeps its cell in this phone's settings - "favouritesCell", "archiveCell", "binCell". */
    private static final String[] PLACES={NoteStore.FAVOURITES,NoteStore.ARCHIVE,NoteStore.BIN,NoteStore.TOOLS,NoteStore.TEMP,NoteStore.RECENT,NoteStore.OPEN,NoteStore.SHARED};

    /**
     * Whether a place is kept in Tools rather than on Home itself (decision 72): yes until it is carried out onto Home, and
     * again once it is let go on Tools. Kept on this phone, as every cell is.
     */
    boolean inTools(String id) {
        try{return a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).getBoolean(id+"InTools",true);}
        catch(ClassCastException unlike){return true;}
    }
    private void keepInTools(String id,boolean in){a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).edit().putBoolean(id+"InTools",in).apply();}

    /** What Tools holds, as its card lists it: those of the four kept in it, with the counts each says. */
    List<NoteStore.Branch> inTools(int archived,int binned,int temporary) {
        List<NoteStore.Branch> held=new ArrayList<>();
        for(NoteStore.Branch one:new NoteStore.Branch[]{archive(archived),bin(binned),temp(temporary),recent()})if(inTools(one.id))held.add(one);
        return held;
    }

    /** One of the four kept in Tools, or shown on Home itself, from its menu. */
    void toolsOrHome(String id,boolean in){keepInTools(id,in);if(!in&&folder.isOpen())folder.close();refresh();}

    /** One of Home's places opened as a card, from a menu as from its icon. */
    void openPlace(NoteStore.Branch place){folder.openPlace(place);}

    /** Where a place was pinned on Home at the last move there, or none. Kept on this phone, as every cell is. */
    private int placeCell(String id) {
        try{return a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).getInt(id+"Cell",Layout.NONE);}
        catch(ClassCastException unlike){return Layout.NONE;}
    }

    /** The page a place was pinned on, or none for one pinned before pages. */
    private int placePage(String id) {
        try{return a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).getInt(id+"Page",Layout.NO_PAGE);}
        catch(ClassCastException unlike){return Layout.NO_PAGE;}
    }

    /** The places' pages and cells, among those a move on Home pins, kept. */
    private void placesOn(Map<String,Layout.Spot> spots) {
        if(spots==null)return;
        android.content.SharedPreferences.Editor kept=null;
        for(String id:PLACES) {
            Layout.Spot at=spots.get(id);
            if(at==null)continue;
            if(kept==null)kept=a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).edit();
            kept.putInt(id+"Cell",at.cell()).putInt(id+"Page",at.page());
        }
        if(kept!=null)kept.apply();
    }

    /** The places' cells, among the cells a move on Home pins, kept - they have no row of their own to keep them in. */
    private void placesAt(Map<String,Integer> cells) {
        if(cells==null)return;
        android.content.SharedPreferences.Editor kept=null;
        for(String id:PLACES) {
            Integer at=cells.get(id);
            if(at==null)continue;
            if(kept==null)kept=a.getSharedPreferences("settings",android.content.Context.MODE_PRIVATE).edit();
            kept.putInt(id+"Cell",at);
        }
        if(kept!=null)kept.apply();
    }

    /** The cell of a grid under a point of the screen's column, or null outside its columns and rows. */
    private int[] cellAt(GridLayout in,Laid laid,float x,float y) {
        if(in==null||laid==null||in.getParent()==null)return null;
        Rect at=rect(in);
        if(at.isEmpty())return null;
        int[] cell=Layout.under(x-at.left-in.getPaddingLeft(),y-at.top-in.getPaddingTop(),laid.width,laid.height,laid.columns);
        return cell==null||cell[0]>=laid.rows?null:cell;
    }

    /**
     * Where a carried thing, or files, will land if let go now: an empty cell, outlined quietly where an icon's face would
     * be - the place it will stand - or nowhere, which takes the outline away.
     */
    private void aim(Laid in,int[] at) {
        View space=in==null||at==null?null:in.spaces.get(Layout.cell(at[0],at[1]));
        aimIn=space==null?null:in;aimAt=space==null?null:at;
        if(space==landing)return;
        if(landing!=null)landing.setBackground(null);
        landing=space;
        if(space==null)return;
        int face=face(in.width),side=(in.width-face)/2;
        GradientDrawable outline=new GradientDrawable();
        outline.setCornerRadius(Looks.corner(face,a.dp(14)));outline.setStroke(Math.max(1,a.dp(3)),a.ACCENT);
        outline.setColor(MainActivity.mix(a.PAPER,a.ACCENT,0.12f));
        space.setBackground(new android.graphics.drawable.InsetDrawable(outline,side,a.dp(8),in.width-face-side,Math.max(0,in.height-a.dp(8)-face)));
    }

    /** How wide one icon's column is in a grid drawn across {@code widthPx}, less the grid's own edges. */
    int cell(GridLayout in,int widthPx) {
        return Math.max(a.dp(56),(widthPx-in.getPaddingLeft()-in.getPaddingRight())/Math.max(1,in.getColumnCount()));
    }

    /** A quiet line across a grid with nothing in it, saying what to do. */
    View nothing(GridLayout in,String words,int widthPx) {
        TextView said=a.label(words,MainActivity.QUIET,a.MUTED);
        said.setGravity(Gravity.CENTER);said.setPadding(a.dp(20),a.dp(48),a.dp(20),a.dp(20));
        GridLayout.LayoutParams across=new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),GridLayout.spec(0,in.getColumnCount()));
        across.width=in.getColumnCount()*cell(in,widthPx);
        said.setLayoutParams(across);
        return said;
    }

    // ---- icons -------------------------------------------------------------------------------------------------

    /**
     * Where an icon is: on Home's grid, in a collection's pop-up, in the Favourites collection's, or in the archive's or the
     * bin's (decision 41), where a thing waits to be put back.
     */
    enum Where { HOME, CARD, FAVOURITES, AWAY }

    /**
     * One thing as an icon, as a phone draws an app: its face, its name under it in two lines at most, and its mark on
     * the corner. A note's or a collection's face is its picture or its icon, and with neither, a note's is the note
     * glyph and a collection's the mini-grid of what is inside it (see {@link IconFace}); a file's, what kind of file it
     * is, with <i>new</i> on it until it is opened. Tapping opens it; holding gives its menu; moving while holding carries it.
     * In the archive or the bin, a tap or a hold asks where it goes from there, as their lists always did.
     */
    View icon(final NoteStore.Branch branch,int cell,final Where where) {
        LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(6));
        GridLayout.LayoutParams place=new GridLayout.LayoutParams();
        place.width=cell;place.height=GridLayout.LayoutParams.WRAP_CONTENT;
        tile.setLayoutParams(place);
        int face=face(cell);
        tile.addView(faced(branch,face,where==Where.HOME||where==Where.CARD),new LinearLayout.LayoutParams(face,face));
        tile.setClipChildren(false);tile.setClipToPadding(false);
        TextView name=a.label(branch.name,MainActivity.READING,a.INK);
        name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0,a.dp(6),0,0);
        tile.addView(name,new LinearLayout.LayoutParams(-1,-2));
        tile.setBackgroundResource(a.touchFeedback());
        if(where==Where.AWAY) {
            final boolean bin=folder.inBin();
            tile.setContentDescription(branch.name+", "+branch.detail+". Tap to put it back"+(bin?" or delete it for good":""));
            tile.setTag(branch);
            tile.setOnClickListener(v->a.awaySheet(v,branch,bin));
            tile.setOnLongClickListener(v->{a.awaySheet(v,branch,bin);return true;});
            return tile;
        }
        tile.setContentDescription(said(branch));
        // A share that waits is an icon like any other. The tap decides: Accept, Refuse, See sender (decision 127).
        tile.setOnClickListener(v->{if(branch.asking)a.arrival(v,branch);else opened(branch,where);});
        a.grabbable(tile,branch);
        if(branch.id.equals(landedId)) {
            tile.setScaleX(0.94f);tile.setScaleY(0.94f);tile.setAlpha(0.5f);
            tile.post(()->tile.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160));
        }
        return tile;
    }

    /** How big an icon's face is in a column of a width: a margin round it, and never so small it cannot be hit. */
    int face(int cell){return Math.max(a.dp(40),Math.min(a.dp(72),cell-a.dp(28)));}

    /**
     * An icon's face, with its mark on the corner and, where it is a favourite, a star on the other: the picture is what
     * the eye lands on, and its corners are the one part of it that is never the thing itself.
     *
     * @param starred whether a favourite says so: not among the favourites, where every one would
     */
    View faced(NoteStore.Branch branch,int face,boolean starred) {
        // A note or a collection wears its look (docs/HOME.md, step 4): its picture, its icon, or its default.
        View picture=branch.kind==NoteStore.Branch.Kind.FILE?fileFace(branch,face):IconFace.view(a,branch,face);
        FrameLayout over=new FrameLayout(a);
        over.addView(picture,new FrameLayout.LayoutParams(face,face));
        int badge=Math.max(a.dp(16),face/4);
        View mark=a.shareBadge(branch,badge);
        if(mark!=null) {
            FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(badge,badge,Gravity.TOP|Gravity.END);
            corner.setMargins(0,-badge/4,-badge/4,0);
            over.addView(mark,corner);
        }
        if(starred&&branch.kept) {
            FrameLayout.LayoutParams other=new FrameLayout.LayoutParams(badge,badge,Gravity.TOP|Gravity.START);
            other.setMargins(-badge/4,-badge/4,0,0);
            over.addView(a.star(badge),other);
        }
        // Temporary: a timer on the corner under the star's, as the star says a favourite (decision 93); where things are
        // only listed, as among the favourites, it is said by the line under it instead.
        if(starred&&branch.temporary) {
            FrameLayout.LayoutParams low=new FrameLayout.LayoutParams(badge,badge,Gravity.BOTTOM|Gravity.START);
            low.setMargins(-badge/4,0,0,-badge/4);
            over.addView(a.timer(badge),low);
        }
        // New: a file that came, or a note somebody else changed, or a folder with one in it, not opened since (the owner,
        // 2026-10-14: "the same everywhere a change hasn't been viewed"). Over the middle of the top edge, clear of the mark.
        if(branch.fresh||branch.updated) {
            // New from somebody else; ✓ when one of my own devices updated it (decision 220).
            TextView fresh=a.label(branch.fresh?Say.onNote("new"):"✓",MainActivity.QUIET,a.PAPER);
            fresh.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            fresh.setIncludeFontPadding(false);fresh.setPadding(a.dp(6),a.dp(2),a.dp(6),a.dp(3));
            GradientDrawable pill=new GradientDrawable();pill.setColor(a.ACCENT);pill.setCornerRadius(a.dp(10));
            fresh.setBackground(pill);
            fresh.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            top.setMargins(0,-a.dp(8),0,0);
            over.addView(fresh,top);
        }
        // Shared on its own and still going up: said on its face, under where its mark is, until its bytes are up and its
        // sleeve has gone (decision 94). Quiet, as a count is: it is busy, not wrong.
        if(branch.kind==NoteStore.Branch.Kind.FILE&&branch.uploading) {
            TextView going=a.label("uploading",MainActivity.QUIET,a.INK);
            going.setIncludeFontPadding(false);going.setPadding(a.dp(6),a.dp(2),a.dp(6),a.dp(3));
            GradientDrawable pill=new GradientDrawable();pill.setColor(a.PAPER);pill.setCornerRadius(a.dp(10));
            pill.setStroke(Math.max(1,a.dp(1)),a.LINE);
            going.setBackground(pill);
            going.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams low=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
            low.setMargins(0,0,0,-a.dp(8));
            over.addView(going,low);
        }
        // Stamped on Minima (decision 249): a stamp on its lower right corner, on a paper disc, in the accent once Minima has
        // it and quiet while it is on its way; its menu's first line says which.
        if(branch.kind==NoteStore.Branch.Kind.FILE&&branch.stamped!=NoteStore.Branch.STAMP_NONE) {
            boolean there=branch.stamped==NoteStore.Branch.STAMP_ON_MINIMA;
            FrameLayout disc=new FrameLayout(a);
            GradientDrawable round=new GradientDrawable();round.setShape(GradientDrawable.OVAL);round.setColor(a.PAPER);
            round.setStroke(Math.max(1,a.dp(1)),a.LINE);disc.setBackground(round);
            View stamp=new View(a);stamp.setBackground(IconFace.symbol(a,"stamp",there?a.ACCENT:a.MUTED));
            disc.addView(stamp,new FrameLayout.LayoutParams(a.dp(14),a.dp(14),Gravity.CENTER));
            disc.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(a.dp(22),a.dp(22),Gravity.BOTTOM|Gravity.END);
            corner.setMargins(0,0,-a.dp(6),-a.dp(6));
            over.addView(disc,corner);
        }
        over.setClipChildren(false);over.setClipToPadding(false);
        return over;
    }

    /**
     * A file's face: a picture shows itself (decision 88), read on the worker and put in when it is ready; any other file,
     * or a picture not read yet, what kind of file it is, in its own letters, on a pale square, as its card under a note has it.
     */
    private View fileFace(NoteStore.Branch file,int face) {
        if(!MainActivity.pictureNamed(file.name))return kindFace(file,face);
        final FrameLayout holder=new FrameLayout(a);
        android.graphics.Bitmap known=a.previewKnown(file.id);
        if(known!=null){holder.addView(pictureFace(known,face),new FrameLayout.LayoutParams(face,face));return holder;}
        holder.addView(kindFace(file,face),new FrameLayout.LayoutParams(face,face));
        a.preview(file.id,made->{holder.removeAllViews();holder.addView(pictureFace(made,face),new FrameLayout.LayoutParams(face,face));});
        return holder;
    }
    private View pictureFace(android.graphics.Bitmap picture,int face) {
        View shown=new View(a);shown.setBackground(IconFace.picture(a,picture));
        shown.setMinimumWidth(face);shown.setMinimumHeight(face);shown.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return shown;
    }
    private View kindFace(NoteStore.Branch file,int face) {
        int dot=file.name.lastIndexOf('.');
        String kind=dot<0||dot==file.name.length()-1?"FILE":file.name.substring(dot+1).toUpperCase(java.util.Locale.ROOT);
        TextView tile=a.label(kind.length()>4?kind.substring(0,4):kind,MainActivity.QUIET,a.ACCENT);
        tile.setTypeface(null,android.graphics.Typeface.BOLD);tile.setGravity(Gravity.CENTER);tile.setSingleLine(true);
        GradientDrawable pale=new GradientDrawable();pale.setColor(MainActivity.mix(a.ACCENT,a.CARD,0.88f));pale.setCornerRadius(a.dp(14));
        pale.setStroke(Math.max(1,a.dp(1)),a.LINE);
        tile.setBackground(pale);
        tile.setMinimumWidth(face);tile.setMinimumHeight(face);
        return tile;
    }

    /**
     * One of Home's places as an icon (decision 41): the Favourites collection's star, the archive's box, the bin's bin,
     * each on an outlined square where a collection shows what is in it, and - on the archive and the bin - how many things
     * wait there. Tapped, it opens as a card; held, its menu; moved while held, it goes to another empty cell of Home
     * (decision 42). It is never renamed, shared, given another icon or put away.
     */
    private View placeIcon(final NoteStore.Branch place,int cell) {
        LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(6));
        GridLayout.LayoutParams at=new GridLayout.LayoutParams();
        at.width=cell;at.height=GridLayout.LayoutParams.WRAP_CONTENT;
        tile.setLayoutParams(at);
        tile.setClipChildren(false);tile.setClipToPadding(false);
        int face=face(cell);
        tile.addView(placeFace(place,face),new LinearLayout.LayoutParams(face,face));
        TextView name=a.label(place.name,MainActivity.READING,a.INK);
        name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0,a.dp(6),0,0);
        tile.addView(name,new LinearLayout.LayoutParams(-1,-2));
        tile.setBackgroundResource(a.touchFeedback());
        tile.setContentDescription(said(place));
        tile.setOnClickListener(v->opened(place,Where.HOME));
        a.grabbable(tile,place);
        return tile;
    }

    /**
     * A place's face: the star for the Favourites collection, and for the archive and the bin their Lucide glyphs, on the
     * same outlined square; on the corner where a thing wears its mark, how many things are in the archive or the bin -
     * quiet, since a full bin is not news - and nothing when it is empty.
     */
    View placeFace(NoteStore.Branch place,int face) {
        // In the colour chosen for it, washed and edged as a collection's square is (decision 81).
        int colour=a.placeColour(place.id),tone=a.placeTone(place.id);
        if(place.kind==NoteStore.Branch.Kind.FAVOURITES)return starFace(face,colour,tone);
        FrameLayout over=new FrameLayout(a);
        View square=new View(a);
        // What is on its way is not a place, and keeps the one edge a thing has.
        square.setBackground(placeSquare(colour,tone,place.kind!=NoteStore.Branch.Kind.WAITING));
        over.addView(square,new FrameLayout.LayoutParams(face,face));
        View glyph=new View(a);
        glyph.setBackground(IconFace.bare(a,placeIcon(place.kind),Looks.ink(colour,a.darkPaper(),a.INK),0));
        over.addView(glyph,new FrameLayout.LayoutParams(face,face));
        // How many wait in the archive, the bin and Temp, and for an answer in Shared with me (decision 109); nothing counted on
        // the others, whose words are not a count.
        boolean counted=place.kind==NoteStore.Branch.Kind.ARCHIVE||place.kind==NoteStore.Branch.Kind.BIN||place.kind==NoteStore.Branch.Kind.TEMP
            ||place.kind==NoteStore.Branch.Kind.SHARED;
        int count=counted?a.countIn(place):0;
        // Temp's sync mark on its corner, as a folder wears its own, while anything in it reaches somebody (the owner,
        // 2026-10-14: "it seems it was the temp folder missing one mark"); its count then stands just left of it.
        boolean marked=place.kind==NoteStore.Branch.Kind.TEMP&&place.standing!=null&&place.standing!=SyncMark.HERE;
        int badge=Math.round(face*0.26f);
        if(marked) {
            FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(badge,badge,Gravity.TOP|Gravity.END);
            corner.setMargins(0,-badge/3,-badge/3,0);
            over.addView(a.placeMark(place,badge),corner);
        }
        if(place.fresh) {
            TextView fresh=a.label(Say.onNote("new"),MainActivity.QUIET,a.PAPER);
            fresh.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            fresh.setIncludeFontPadding(false);fresh.setPadding(a.dp(6),a.dp(2),a.dp(6),a.dp(3));
            GradientDrawable pill=new GradientDrawable();pill.setColor(a.ACCENT);pill.setCornerRadius(a.dp(10));
            fresh.setBackground(pill);fresh.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            top.setMargins(0,-a.dp(8),0,0);
            over.addView(fresh,top);
        }
        if(count>0) {
            // Ink on paper, bold, as on the PC (the owner, 2026-10-14: "the 1 in Temp is not well displayed").
            TextView many=a.label(count>99?"99+":String.valueOf(count),MainActivity.QUIET,a.PAPER);
            many.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            many.setIncludeFontPadding(false);many.setGravity(Gravity.CENTER);
            many.setMinWidth(a.dp(22));many.setMinHeight(a.dp(22));many.setPadding(a.dp(6),a.dp(2),a.dp(6),a.dp(2));
            GradientDrawable pill=new GradientDrawable();pill.setColor(a.INK);pill.setCornerRadius(a.dp(11));
            many.setBackground(pill);
            many.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);
            corner.setMargins(0,-a.dp(6),marked?badge-badge/3+a.dp(2):-a.dp(8),0);
            over.addView(many,corner);
        }
        over.setClipChildren(false);over.setClipToPadding(false);
        return over;
    }

    /**
     * A place's square: the card's grey with none chosen, else washed in its colour and edged in it, as a folder's; and in
     * its own frame (decision 94, the owner: "let them all have something that differentiates them"), a thin ring inside
     * the edge, in the edge's colour, so a place is told from a folder of the owner's at a glance. Its colour lands at
     * the place's own strength (decision 107).
     */
    private android.graphics.drawable.Drawable placeSquare(int colour,int tone,boolean place) {
        android.graphics.drawable.Drawable edge=a.edged(Tint.known(colour)?Tint.over(colour,a.CARD,a.wash(tone,0.22f,0.92f),a.darkPaper()):a.CARD,colour,false);
        if(!place)return edge;
        GradientDrawable ring=new GradientDrawable();ring.setColor(0);ring.setCornerRadius(a.dp(10));
        ring.setStroke(Math.max(1,a.dp(1)),Tint.known(colour)?Tint.of(colour,a.darkPaper()):a.LINE);
        android.graphics.drawable.LayerDrawable framed=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{edge,ring});
        int in=a.dp(Tint.known(colour)?6:4);
        framed.setLayerInset(1,in,in,in,in);
        return framed;
    }

    /** The archive's and the bin's icons in the Lucide set (decision 41; the set's "trash" is the bin with lines in it). */
    static final String ARCHIVE_ICON="archive",BIN_ICON="trash";

    /** A star on an outlined square: the Favourites collection's face, here and in the dock. */
    View starFace(int face){return starFace(face,Tint.NONE,Tint.USUAL);}
    View starFace(int face,int colour,int tone) {
        LinearLayout box=a.column();box.setGravity(Gravity.CENTER);
        box.setBackground(placeSquare(colour,tone,true));
        TextView star=a.label("★",MainActivity.QUIET,Looks.ink(colour,a.darkPaper(),a.INK));
        star.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,face*0.42f);
        star.setIncludeFontPadding(false);star.setGravity(Gravity.CENTER);
        box.addView(star,new LinearLayout.LayoutParams(-1,-1));
        box.setMinimumWidth(face);box.setMinimumHeight(face);
        return box;
    }

    /** The Favourites collection, as a thing a menu and a pop-up are about: a place, like the archive, not a collection. */
    static NoteStore.Branch favourites() {
        return new NoteStore.Branch(NoteStore.Branch.Kind.FAVOURITES,NoteStore.FAVOURITES,Sharing.EVERYTHING,"Favourites","",0,0,true);
    }

    /** The archive, as Home shows it, with how many things are in it - which is all its line says. */
    /** Each place's glyph from the Lucide set: Tools a toolbox, Temp a timer, Recent a clock, something on its way an hourglass. */
    static String placeIcon(NoteStore.Branch.Kind kind) {
        switch(kind) {
            case BIN: return BIN_ICON;
            case TOOLS: return "toolbox";
            case TEMP: return "timer";
            case RECENT: return "clock-3";
            case OPEN: return "layers";
            case WAITING: return "hourglass";
            case SHARED: return "inbox";
            case MESSAGES: return "mail";
            default: return ARCHIVE_ICON;
        }
    }
    /** Tools, holding the archive, the bin, Temp and Recent until they are carried out (decision 72). */
    static NoteStore.Branch tools(){return place(NoteStore.Branch.Kind.TOOLS,NoteStore.TOOLS,"Tools",0);}
    /** Temp, with how many things in it are to be gone (decision 71). */
    static NoteStore.Branch temp(int count){return place(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP,"Temp",count);}
    /** Recent: what was opened lately (decision 72). */
    static NoteStore.Branch recent(){return place(NoteStore.Branch.Kind.RECENT,NoteStore.RECENT,"Recent",0);}
    /** Shared with me: where files shared with you on their own show (decision 94). */
    static NoteStore.Branch shared(){return shared(0);}
    /** The same, with how many things wait in it for an answer, which it wears as Temp wears its count (decision 109). */
    static NoteStore.Branch shared(int waiting){return place(NoteStore.Branch.Kind.SHARED,NoteStore.SHARED,NoteStore.SHARED_WITH_ME,waiting);}
    static NoteStore.Branch archive(int count){return place(NoteStore.Branch.Kind.ARCHIVE,NoteStore.ARCHIVE,"Archive",count);}
    /** The bin, as Home shows it, with how many things are in it. */
    static NoteStore.Branch bin(int count){return place(NoteStore.Branch.Kind.BIN,NoteStore.BIN,"Bin",count);}
    private static NoteStore.Branch place(NoteStore.Branch.Kind kind,String id,String name,int count) {
        return new NoteStore.Branch(kind,id,Sharing.EVERYTHING,name,count<=0?"":count==1?"1 thing":count+" things",0,0,true);
    }

    /** What a screen reader says for an icon: what tapping it does, and what else is true of it. */
    static String said(NoteStore.Branch branch) {
        switch(branch.kind) {
            case FAVOURITES: return "Open Favourites, the favourites that are not in the dock";
            case ARCHIVE: return "Open the archive, "+(branch.detail.isEmpty()?"empty":branch.detail);
            case BIN: return "Open the bin, "+(branch.detail.isEmpty()?"empty":branch.detail);
            case TOOLS: return "Open Tools: the archive, the bin, Temp and Recent";
            case TEMP: return "Open Temp, "+(branch.detail.isEmpty()?"empty":branch.detail+" to be gone");
            case RECENT: return "Open Recent, what was opened lately";
            case SHARED: return "Open Shared with me, the files people share with you"+(branch.detail.isEmpty()?"":", "+branch.detail+" waiting for you");
            case MESSAGES: return "Open the Inbox, brief messages with the people you are paired with"+(branch.detail.isEmpty()?"":", "+branch.detail+" not read");
            case OPEN: return "What is open, "+(branch.detail.isEmpty()?"nothing":branch.detail);
            case WAITING: return branch.name+": "+branch.detail;
            default:
        }
        String open=branch.kind==NoteStore.Branch.Kind.FILE?"Open the file "+branch.name+", "+branch.detail
            :branch.holds?"Open the folder "+branch.name:"Open the note "+branch.name;
        return open+(branch.kept?", a favourite":"")+(branch.temporary?", temporary":"")+(branch.fresh?", new":"")+(branch.uploading?", uploading":"");
    }

    /**
     * An icon tapped: a note opens, a collection opens its pop-up - in the same card, from inside one - a file opens, and
     * a place opens as a card of what is in it.
     */
    void opened(NoteStore.Branch branch,Where where) {
        switch(branch.kind) {
            case PAGE: openNote(branch.id);return;
            // New and not opened yet: what it is and what to do with it, before anything opens (the owner, 2026-10-16: "to
            // prevent opening it first, clicking it should provide all the needed information for the person to decide").
            case FILE: if(branch.fresh)fileMenu(a.getWindow().getDecorView(),branch);else openFile(branch);return;
            case FAVOURITES: folder.openFavourites();return;
            case ARCHIVE: case BIN: case TOOLS: case TEMP: case RECENT: case OPEN: case SHARED: folder.openPlace(branch);return;
            case WAITING: waiting(branch);return;
            case COLLECTION: case BOOK:
                if(where==Where.CARD)folder.into(branch);
                // A favourite opens where it really lives, with the way back being the way back from there.
                else if(where==Where.FAVOURITES&&folder.isOpen()&&folder.lists())folder.into(branch);
                else if(where==Where.FAVOURITES)openCollection(branch.id);
                else folder.open(branch);
                return;
            default:
        }
    }

    // ---- opening, from Home and from anything over it ----------------------------------------------------------------

    /**
     * A note, opened from Home: its page, with the way back being Home as it was - the pop-up it was opened from, if one
     * was open, or the grid - rather than the collection the note happens to be in.
     */
    void openNote(String id) {
        a.cameFrom=new ArrayList<>(a.trail);a.cameFromNote=id;
        a.open(id);
    }

    /** A collection, from anywhere: its pop-up over Home, with the trail above it being where it really is, however deep. */
    void openCollection(final String id) {
        a.background.submit(()->a.trailTo(id),steps->{
            a.trail.clear();a.trail.addAll(steps);
            if(showing())folder.show();else a.browse();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** A file opened where it is kept: handed to whatever opens it, and no longer new. */
    void openFile(final NoteStore.Branch file) {
        a.background.submit(()->{
            NoteStore.Held held=a.store.file(file.id);
            if(held!=null&&held.fresh)a.store.opened(file.id);
            return held;
        },held->{
            if(held==null){a.alert("That file is not here any more.");refresh();return;}
            a.openFile(held);
            if(file.fresh)a.refresh();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** Something asked for in the overview: gone to, from Home as it is. */
    void goTo(Overview.Open one) {
        if(one.kind==Overview.Kind.NOTE)openNote(one.id);else openCollection(one.id);
    }

    // ---- making ------------------------------------------------------------------------------------------------

    /** A new note where the + was pressed - on Home, or in the pop-up's collection - opened at once to be written on. */
    /**
     * Record a note, from a +: a new note in that place, named for when it was recorded, written down at once so the sound
     * has a note to be kept with, then opened and recording (the owner, 2026-10-12: "since we have take a picture, we should
     * also have record a note").
     */
    void recordNote(final String asked) {
        final String place=asked!=null&&MainActivity.takesNew(asked)?asked:null,where=asked==null||asked.isEmpty()||place!=null?Things.HOME:asked;
        a.background.submit(()->Things.HOME.equals(where)?Boolean.TRUE:a.store.mayWriteIn(NoteStore.Branch.Kind.COLLECTION,where),may->{
            if(Boolean.FALSE.equals(may)){a.alert("This folder is read only here. Ask whoever shared it to let you write in it.");return;}
            final NoteStore.Note note=new NoteStore.Note();note.book=where;
            note.title=Say.t("Recording")+" "+new java.text.SimpleDateFormat("d MMM, HH:mm",java.util.Locale.getDefault()).format(new java.util.Date());note.body="";
            // Written at once, with its title: from a place's +, put in the place at once too, as its first word would put a
            // note typed in. It was left on Home, never in Temp (decision 238).
            a.background.submit(()->{a.store.save(note);if(place!=null)a.intoPlaceNow(NoteStore.Branch.Kind.PAGE,note.id,place);return a.store.get(note.id);},kept->{
                if(Things.HOME.equals(where)&&!NoteStore.ARCHIVE.equals(place)&&!NoteStore.BIN.equals(place))placeNew(note.id);
                a.cameFrom=new ArrayList<>(a.trail);a.cameFromNote=note.id;
                a.write((NoteStore.Note)kept);
                a.getWindow().getDecorView().postDelayed(a::record,250);
            },e->a.alert("Could not make the note. Nothing was changed."));
        },e->a.alert(MainActivity.READ_FAILED));
    }

    void newNote(final String asked) {
        // From a place's +: made on Home, and put in the place when it is first written down (decision 87).
        final String place=MainActivity.takesNew(asked)?asked:null,where=place!=null?Things.HOME:asked;
        a.background.submit(()->Things.HOME.equals(where)?Boolean.TRUE:a.store.mayWriteIn(NoteStore.Branch.Kind.COLLECTION,where),may->{
            if(Boolean.FALSE.equals(may)){a.alert("This folder is read only here. Ask whoever shared it to let you write in it.");return;}
            NoteStore.Note note=new NoteStore.Note();note.book=where;
            if(place!=null)a.placeOnSave.put(note.id,place);
            if(Things.HOME.equals(where)&&!NoteStore.ARCHIVE.equals(place)&&!NoteStore.BIN.equals(place))placeNew(note.id);
            a.cameFrom=new ArrayList<>(a.trail);a.cameFromNote=note.id;
            a.write(note);
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /**
     * A new collection where the + was pressed, made as Untitled, and its pop-up opened with the name ready to be typed
     * over - selected, with the keyboard up - since a gesture cannot say what a thing is called.
     */
    void newCollection(final String asked) {
        // From a place's +: made on Home and put in the place at once (decision 87).
        final String place=MainActivity.takesNew(asked)?asked:null,where=place!=null?Things.HOME:asked;
        a.background.submit(()->{NoteStore.Shelf made=a.store.addCollectionIn(where,"");if(place!=null)a.intoPlaceNow(NoteStore.Branch.Kind.COLLECTION,made.id,place);return made;},made->{
            if(Things.HOME.equals(where)&&!NoteStore.ARCHIVE.equals(place)&&!NoteStore.BIN.equals(place))placeNew(made.id);
            a.nameNext=made.id;
            if(Things.HOME.equals(where)){a.trail.clear();a.trail.add(new MainActivity.Step(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,"Home"));}
            a.trail.add(new MainActivity.Step(NoteStore.Branch.Kind.COLLECTION,made.id,made.name));
            if(showing())folder.show();else a.browse();
        },e->a.alert("Could not make that folder. Nothing was changed."));
    }

    // ---- the files on Home and in collections ------------------------------------------------------------------

    /**
     * A file's menu, where it is held: what its tap does first, then what else can be done with it, and deleting it last,
     * under the line, since that is the one thing here that cannot be taken back.
     */
    void fileMenu(View anchor,final NoteStore.Branch file) {
        // Starred and temporary first, read from the notebook, so each row says what it will do (decision 87); and whether
        // this phone may change it, which only then offers to (decision 93).
        a.background.submit(()->new Object[]{a.store.favourite(NoteStore.Branch.Kind.FILE,file.id),a.store.untilOf(NoteStore.Branch.Kind.FILE,file.id),
                a.store.mayChangeFile(file.id),a.store.standing(file.id),a.store.file(file.id),a.store.linkedOn(file.id)},
            got->fileMenu(anchor,file,(Boolean)got[0],(Long)got[1],(Boolean)got[2],(NoteStore.Standing)got[3],(NoteStore.Held)got[4],(String)got[5]),e->a.alert(MainActivity.READ_FAILED));
    }
    private void fileMenu(View anchor,final NoteStore.Branch file,boolean starred,long until,boolean changes,NoteStore.Standing standing,NoteStore.Held kept,String linked) {
        MainActivity.Sheet sheet=a.new Sheet();
        // Its original a file on one of the owner's PCs, kept in step from there (the owner, 2026-10-16: yes, say so).
        if(!linked.isEmpty())sheet.heading(Say.t("Linked to a file on {0}",linked));
        // New here, not opened yet: what it is, then what to do with it, first (the owner, 2026-10-16).
        if(kept!=null&&kept.fresh) {
            sheet.heading(Say.t("New · {0}",Attachment.size(kept.bytes)));
            if(!standing.from.isEmpty())sheet.row("From "+standing.fromName+" · "+a.shortWhen(standing.at),()->a.aboutSharing(file));
            sheet.row("Keep it here",()->a.background.submit(()->{a.store.opened(file.id);return null;},done->{a.toast("Kept here");a.refresh();},e->a.alert(MainActivity.READ_FAILED)));
            sheet.row("Move to…",()->{a.carrying=file;a.browse();});
            sheet.row("Move to bin",()->a.putAway(file,true));
            sheet.line();
        }
        // Who sent it, first, and the box that says where every device stands with it (the owner, 2026-10-05).
        if(!standing.from.isEmpty()&&(kept==null||!kept.fresh)) {
            sheet.row("From "+standing.fromName+" · "+a.shortWhen(standing.at),()->a.aboutSharing(file));
            sheet.line();
        }
        // One Open, named by the app it opens in, and Open with… to pick another (the owner, 2026-10-15: "we should have only
        // one open, once open we can decide to modify or not").
        sheet.row("Open",kept==null?null:a.openLine(kept),()->openFile(file));
        sheet.row("Open with…",()->withHeld(file,a::openFileWith));
        // Written in like a note, by whoever may (decision 93): the same file, by the same id, for everybody who has it.
        if(changes) {
            sheet.row("Rename…",()->a.renameFile(file));
            sheet.row("Replace with another file…",()->a.replaceFile(file));
        }

        // Out to anybody or anywhere, as on a note's attachment (the owner, 2026-10-10): another app, Downloads, a place picked.
        // Copied first, as a note's attachment is (decision 207), so the photo viewer's ⋮ has it too.
        sheet.row("Copy",()->withHeld(file,held->a.copyFile(held.id,held.name)));
        sheet.row("Send with another app…",()->withHeld(file,held->a.sendWithApp(held.id,held.name,held.kind)));
        sheet.row("Save on my phone…",()->withHeld(file,held->a.saveCopyOf(held.id,held.name,held.kind)));
        // On Minima: one row, saying how far its stamp is, opening the one window for it (decision 253).
        if(file.stamped!=NoteStore.Branch.STAMP_NONE)sheet.row("Stamp on Minima…",Say.t(file.stamped==NoteStore.Branch.STAMP_ON_MINIMA?"On Minima":"Waiting for Minima"),()->withHeld(file,held->PhoneStamp.thing(a,held.id,false)));
        else sheet.row("Stamp on Minima…",()->withHeld(file,held->PhoneStamp.thing(a,held.id,false)));
        sheet.row("Send to a device…",()->a.chooseDevice(Collections.singletonList(Lending.of(file.id)),new ArrayList<>()));
        sheet.row("Put in a note…",()->withHeld(file,held->a.putInNote(loose(held))));
        sheet.row("Move to…",()->{a.carrying=file;a.browse();});
        // Shared like a note, with its own people and roles (decision 92): the same box a note's Share… opens.
        sheet.row("Share…",()->a.aboutSharing(file));
        // A file in the places a note can be in (decision 87): Favourites, Temp, the archive and the bin.
        sheet.row(starred?"Remove from favourites":"Add to favourites",()->a.background.submit(()->{a.store.keepToHand(NoteStore.Branch.Kind.FILE,file.id,!starred);return null;},
            done->{a.toast(starred?"Not a favourite":"A favourite");a.refresh();},e->a.alert("Could not change that. Nothing was changed.")));
        sheet.row(until>0?"Temporary · "+NoteStore.goneIn(until,System.currentTimeMillis()).toLowerCase(java.util.Locale.ROOT):"Temporary…",()->a.temporaryBox(file));
        sheet.line();
        sheet.row("Archive",()->a.putAway(file,false));
        sheet.row("Move to bin",()->a.putAway(file,true));
        sheet.show(anchor);
    }

    /** One kept file, read from the notebook, for what needs more of it than its line says. */
    private void withHeld(final NoteStore.Branch file,final java.util.function.Consumer<NoteStore.Held> then) {
        a.background.submit(()->a.store.file(file.id),held->{
            if(held==null){a.alert("That file is not here any more.");refresh();return;}
            then.accept(held);
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** A kept file as the received file the older boxes - Save a copy, Put in a note - were written for. */
    private static NoteStore.Loose loose(NoteStore.Held held) {
        return new NoteStore.Loose(held.id,"","",held.name,held.kind,held.bytes,"",true,false,0,0);
    }

    // ---- files dragged in from another app ---------------------------------------------------------------------------

    /** Where files from another app would go if let go now: the thing kept with, the empty cell they land in, and what is lit. */
    private static final class Aim {
        final NoteStore.Branch.Kind kind;final String id,name;final Laid in;final int[] cell;final View lit;
        Aim(NoteStore.Branch.Kind kind,String id,String name,Laid in,int[] cell,View lit){this.kind=kind;this.id=id;this.name=name;this.in=in;this.cell=cell;this.lit=lit;}
    }

    /**
     * Files from another app over a grid - Home's, or a card's - aimed at what is under the finger, as the PC aims what
     * Explorer drops: over a note's icon they are kept with the note, over a collection's with that collection, each ringed
     * as a carried thing's target is; over an empty cell, with what the grid shows, in that cell, outlined as the place they
     * land; anywhere else on it, with what the grid shows, in the first free cells.
     *
     * @param holder what the grid shows, asked when they are let go: Home, or the card's collection
     */
    boolean filesOn(View on,DragEvent event,GridLayout in,java.util.function.Supplier<Laid> laid,java.util.function.Supplier<String> holder) {
        int action=event.getAction();
        // The finger, in the screen's column, as a carried icon's is measured: the view's own window, wherever it is scrolled.
        Rect window=seen(on);
        float x=window.left+event.getX(),y=window.top+event.getY();
        final Aim dropped=action==DragEvent.ACTION_DROP?filesAim(in,laid.get(),holder.get(),x,y):null;
        boolean said=a.filesOver(on,event,files->keepAt(files,dropped));
        if(action==DragEvent.ACTION_DRAG_LOCATION&&said) {
            Aim now=filesAim(in,laid.get(),holder.get(),x,y);
            if(now.cell!=null){a.markOnto(null);aim(now.in,now.cell);}
            else{aim(null,null);a.markOnto(now.lit);}
        }
        if(action==DragEvent.ACTION_DRAG_EXITED||action==DragEvent.ACTION_DROP||action==DragEvent.ACTION_DRAG_ENDED){aim(null,null);a.markOnto(null);}
        return said;
    }

    private Aim filesAim(GridLayout in,Laid laid,String holder,float x,float y) {
        int[] at=cellAt(in,laid,x,y);
        String there=at==null?null:Layout.at(laid.drawn,at[0],at[1]);
        NoteStore.Branch thing=there==null?null:laid.things.get(there);
        switch(Grid.filesOnto(thing==null?null:thing.kind)) {
            case NOTE: return new Aim(NoteStore.Branch.Kind.PAGE,thing.id,thing.name,null,null,laid.icons.get(there));
            case COLLECTION: return new Aim(NoteStore.Branch.Kind.COLLECTION,thing.id,thing.name,null,null,laid.icons.get(there));
            default:
                boolean empty=at!=null&&there==null&&laid.spaces.containsKey(Layout.cell(at[0],at[1]));
                return new Aim(NoteStore.Branch.Kind.COLLECTION,holder,"",empty?laid:null,empty?at:null,null);
        }
    }

    /** Files let go where they were aimed: kept there - a note this phone only reads takes none - and put in their cell. */
    private void keepAt(final List<Uri> files,final Aim aim) {
        if(aim==null||aim.id==null||aim.id.isEmpty())return;
        if(aim.kind==NoteStore.Branch.Kind.PAGE) {
            a.background.submit(()->a.store.onlyReads(aim.id),reads->{
                if(Boolean.TRUE.equals(reads))a.alert("“"+aim.name+"” is read only here. Ask whoever shared it to let you write in it.");
                else a.keepFiles(NoteStore.Branch.Kind.PAGE,aim.id,files,new ArrayList<>());
            },e->a.alert(MainActivity.READ_FAILED));
            return;
        }
        if(aim.cell==null){a.keepFiles(aim.kind,aim.id,files,new ArrayList<>());return;}
        final Laid in=aim.in;final int[] at=aim.cell;final boolean onHome=in.where==Where.HOME;
        a.keepFiles(aim.kind,aim.id,files,new ArrayList<>(),kept->{
            // The first in the cell they were let go in, every icon there pinned where it was drawn, and any others after
            // it in the first free cells, as anything new takes.
            List<NoteStore.Branch> lines=new ArrayList<>(in.lines);
            lines.add(new NoteStore.Branch(NoteStore.Branch.Kind.FILE,kept.get(0),"","","",0,0,false));
            if(onHome&&in.spots!=null){Map<String,Layout.Spot> spots=homeMove(in,kept.get(0),at[0],at[1]);if(spots!=null)writeHome(lines,spots);return;}
            Map<String,Integer> cells=Layout.moveTo(in.drawn,kept.get(0),at[0],at[1]);
            if(cells==null)return;
            if(onHome)placesAt(cells);
            a.store.place(lines,cells);
        });
    }

    // ---- what this device sent, now there is no drop box -----------------------------------------------------------

    /**
     * ⋮ → Sent files: what this device sent to others, the newest first, each with where it stands, and a tap to stop
     * it or take it off the list (decision 20). Above it, while there is any, what another device is sending here -
     * somebody asking, or files still on their way - since with the drop box gone this is the one list of sendings left.
     */
    void sentFiles() {
        a.background.submit(()->{
            List<NoteStore.Transfer> sent=a.store.sentFiles(),coming=new ArrayList<>();
            for(NoteStore.Transfer one:a.store.transfers())if(!one.out)coming.add(one);
            Set<String> takes=new HashSet<>();
            for(NoteStore.Transfer one:sent)if(Post.takesFiles(a,a.store.address(one.address)))takes.add(one.address);
            return new Object[]{sent,coming,takes};
        },got->{
            @SuppressWarnings("unchecked") List<NoteStore.Transfer> sent=(List<NoteStore.Transfer>)got[0];
            @SuppressWarnings("unchecked") List<NoteStore.Transfer> coming=(List<NoteStore.Transfer>)got[1];
            @SuppressWarnings("unchecked") Set<String> takes=(Set<String>)got[2];
            LinearLayout body=a.inside();
            final AlertDialog[] box={null};
            if(!coming.isEmpty()) {
                body.addView(a.part("Coming to this phone"));
                for(final NoteStore.Transfer one:coming) {
                    if(one.state==Drop.ASKING){body.addView(line(one.name,Drop.wants(one.files.size(),one.bytes()),true,()->{box[0].dismiss();a.askAboutSending(one.id);}));continue;}
                    int here=0;for(NoteStore.Loose file:one.files)if(file.here)here++;
                    body.addView(line(one.name,Drop.files(one.files.size())+" coming, "+here+" here",false,()->{box[0].dismiss();a.askStopComing(one);}));
                }
                if(!sent.isEmpty())body.addView(a.part("Sent"));
            }
            for(final NoteStore.Transfer one:sent)
                body.addView(line(one.name,Drop.files(one.files.size())+"  ·  "+a.when(one.at)+"  ·  "+Drop.state(one.state,one.name,takes.contains(one.address)),
                    false,()->{box[0].dismiss();a.askStopSending(one);}));
            if(sent.isEmpty()&&coming.isEmpty())body.addView(a.under("Nothing sent from this phone yet. Files go to another device from ⋮ → Send files, or from a file's own menu."));
            box[0]=a.new Box().setTitle("Sent files").setView(a.scrolling(body)).create();
            box[0].show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /** One sending, as the drop box listed it, fitted to a box's own margins. */
    private View line(String name,String detail,boolean asking,Runnable tapped) {
        View row=a.dropRow(name,detail,asking,tapped);
        row.setPadding(0,a.dp(12),0,a.dp(12));
        return row;
    }

    /** Somebody wants to send files and has not been answered: asked, where the notification that says so leads. */
    void askWaiting() {
        a.background.submit(()->{
            for(NoteStore.Transfer one:a.store.transfers())if(!one.out&&one.state==Drop.ASKING)return one.id;
            return "";
        },id->{if(!id.isEmpty())a.askAboutSending(id);},e->{});
    }

    // ---- the dock, from a menu --------------------------------------------------------------------------------------

    /** Taken out of the dock: a favourite or not as it was, and the rest of the dock closes up (decision 190). */
    void undock(final NoteStore.Branch thing) {
        a.background.submit(()->{a.store.outOfDock(thing.kind,thing.id);return null;},
            done->{a.toast("Out of the dock");a.refresh();},e->a.alert("Could not change that. Nothing was changed."));
    }

    /** Put at the end of the dock from its menu: a favourite or not as it was (decision 190). */
    void dockAtEnd(final NoteStore.Branch thing) {
        a.background.submit(()->{a.store.toDockEnd(thing.kind,thing.id);return null;},
            done->{a.toast("In the dock");a.refresh();},e->a.alert("Could not put that in the dock. Nothing was changed."));
    }

    // ---- open things, for the overview ---------------------------------------------------------------------------

    /** A note on the page: to the front of the overview, where it is not already. Not a blank page nothing was written on. */
    void noteOpened(NoteStore.Note note) {
        if(note==null||note.id==null)return;
        if((note.title==null||note.title.trim().isEmpty())&&(note.body==null||note.body.trim().isEmpty()))return;
        overview.remember(Overview.Kind.NOTE,note.id);
        // A help request, where this note is set to send one when it opens (decision 113): once per opening, here where a
        // real note opens, not a blank page.
        a.helpOnOpened(NoteStore.Branch.Kind.PAGE,note.id);
    }

    /** Something put away or deleted: no card opens what is not there. */
    void forget(String id){overview.forget(id);}

    // ---- back ------------------------------------------------------------------------------------------------------

    /**
     * The phone's back, on Home: a menu that is up goes first, then the overview, then a pop-up one level at a time until
     * it closes. With none of them there is nothing on Home to go back from, and the answer is no.
     */
    boolean back() {
        if(!showing())return false;
        if(a.showing!=null){a.showing.close();return true;}
        if(overview.isShowing()){overview.close();return true;}
        if(folder.isOpen()){folder.up();return true;}
        if(zoomed){zoomInto(focusX,focusY);return true;}
        // On another page, back goes to the main one, as a phone's own home screen does.
        if(!onCentre()){go(0,0);return true;}
        return false;
    }

    /** A colour chosen for a collection while its menu is over its pop-up: the pop-up takes it at once, as the page does. */
    void painted(String id,int colour){folder.painted(id,colour);}
    /** And a strength chosen for it there: the pop-up takes that at once too (decision 107). */
    void toned(String id,int tone){folder.toned(id,tone);}

    // ---- carrying ---------------------------------------------------------------------------------------------------

    /**
     * Everything carried on Home comes here, wherever it is picked up: the finger is measured against the dock, an open
     * pop-up, the dimmed grid round it and Home's grid, and letting go does what {@link Grid} says it does there.
     */
    private boolean dragged(View on,DragEvent event) {
        switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: stopScroll();carriedFrom=null;carriedFrom=from();outOf=homeGrid(carriedFrom)?Things.HOME:carriedFrom!=null&&folder.isOpen()?folder.id():null;
                if(a.dragging!=null)showStrip(a.dragging);
                return a.dragging!=null;
            case DragEvent.ACTION_DRAG_LOCATION:
                // Over the strip of what can be done to it: that, and nothing under it (decision 90).
                // And no folder opens under it: a pass over one on the way to the strip set its spring going (decision 90).
                if(overStrip(event.getX(),event.getY())){cancelEdge();light(Grid.Zone.NONE);a.markOnto(null);aim(null,null);desk.removeCallbacks(springing);springAt=null;springKey=null;springDo=null;return true;}
                over(event.getX(),event.getY());return true;
            case DragEvent.ACTION_DRAG_EXITED: cancelEdge();light(Grid.Zone.NONE);a.markOnto(null);aim(null,null);litTarget(-1);return true;
            case DragEvent.ACTION_DROP:
                if(overStrip(event.getX(),event.getY()))return dropOnStrip();
                over(event.getX(),event.getY());return drop(event.getX());
            case DragEvent.ACTION_DRAG_ENDED: ended(event.getResult());return true;
            default: return true;
        }
    }

    // ---- the strip of what can be done, while a thing is carried ------------------------------------------------------

    /**
     * What can be done to a thing by letting go of it, across the top while it is carried, as a phone offers Remove and
     * Uninstall over its home screen (the owner, 2026-10-04: "when we drag and drop elements we should be presented, like
     * on our phone with an app, Archive or Bin ... a UI/UX that lets the user do pretty much everything with drag and
     * drop"; decision 90): a favourite, Temp, the archive, the bin, and sharing it: a file's too, since it is shared like a
     * note (decision 92); Send to a device stays in its menu.
     */
    private LinearLayout strip;
    private final List<View> targets=new ArrayList<>();
    private int litAt=-1;
    private static final String[][] STRIP={{"star","Favourite"},{"timer","Temp"},{HomeScreen.ARCHIVE_ICON,"Archive"},{HomeScreen.BIN_ICON,"Bin"},{"share-2","Share"}};

    private void showStrip(NoteStore.Branch carried) {
        hideStrip();
        if(carried.kind!=NoteStore.Branch.Kind.PAGE&&carried.kind!=NoteStore.Branch.Kind.COLLECTION&&carried.kind!=NoteStore.Branch.Kind.BOOK
            &&carried.kind!=NoteStore.Branch.Kind.FILE)return;
        strip=new LinearLayout(a);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setGravity(Gravity.CENTER);
        strip.setPadding(a.dp(6),a.dp(2),a.dp(6),a.dp(2));
        GradientDrawable ground=new GradientDrawable();ground.setColor(a.CARD);ground.setCornerRadius(a.dp(20));ground.setStroke(Math.max(1,a.dp(1)),a.LINE);
        strip.setBackground(ground);strip.setElevation(a.dp(12));
        targets.clear();litAt=-1;
        for(String[] one:STRIP) {
            String glyph=one[0],said=one[1];
            LinearLayout target=a.column();target.setGravity(Gravity.CENTER_HORIZONTAL);target.setPadding(a.dp(2),a.dp(4),a.dp(2),a.dp(4));
            View face=new View(a);face.setBackground(IconFace.bare(a,glyph,a.INK,0));
            target.addView(face,new LinearLayout.LayoutParams(a.dp(28),a.dp(28)));
            TextView name=a.label(said,MainActivity.QUIET,a.INK);name.setGravity(Gravity.CENTER);name.setSingleLine(true);
            target.addView(name,new LinearLayout.LayoutParams(-2,-2));
            target.setContentDescription(said);
            strip.addView(target,new LinearLayout.LayoutParams(0,-2,1));targets.add(target);
        }
        // In the bar just above Home's rows, centred, leaving the rows free to drop on: carried past it - above it or beside it
        // on the bar - the page above comes as it always did (the owner: "if we overpass them, we go to the page on top").
        strip.measure(View.MeasureSpec.makeMeasureSpec(a.dp(320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int high=strip.getMeasuredHeight();
        FrameLayout.LayoutParams at=new FrameLayout.LayoutParams(a.dp(320),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        at.setMargins(0,Math.max(a.root.getPaddingTop(),rect(desk).top-high-a.dp(2)),0,0);
        a.stage.addView(strip,at);
    }

    private void hideStrip() {
        if(strip!=null&&strip.getParent()!=null)((ViewGroup)strip.getParent()).removeView(strip);
        strip=null;targets.clear();litAt=-1;
    }

    /** Whether the finger is over the strip, lighting the target it is over. Measured on the screen, as the strip is not in the column. */
    private boolean overStrip(float x,float y) {
        if(strip==null||strip.getParent()==null||strip.getHeight()==0)return false;
        int[] column=new int[2],bar=new int[2];a.root.getLocationOnScreen(column);strip.getLocationOnScreen(bar);
        float sx=x+column[0]-bar[0],sy=y+column[1]-bar[1];
        if(sx<0||sy<0||sx>strip.getWidth()||sy>strip.getHeight()){litTarget(-1);return false;}
        int which=-1;
        for(int i=0;i<targets.size();i++){View one=targets.get(i);if(sx>=one.getLeft()&&sx<one.getRight())which=i;}
        litTarget(which);
        return true;
    }

    private void litTarget(int which) {
        if(which==litAt)return;
        for(int i=0;i<targets.size();i++) {
            View one=targets.get(i);
            if(i==which){GradientDrawable lit=new GradientDrawable();lit.setColor(MainActivity.mix(a.ACCENT,a.CARD,0.8f));lit.setCornerRadius(a.dp(14));lit.setStroke(a.dp(2),a.ACCENT);one.setBackground(lit);}
            else one.setBackground(null);
        }
        litAt=which;
    }

    /** Let go on one of the strip's targets: done as its menu does it, said, with Undo where the menu has one. */
    private boolean dropOnStrip() {
        final NoteStore.Branch carried=a.dragging;int which=litAt;
        if(carried==null||which<0)return false;
        stale=true;
        boolean file=carried.kind==NoteStore.Branch.Kind.FILE;
        switch(which) {
            case 0:
                if(file)a.background.submit(()->{a.store.keepToHand(NoteStore.Branch.Kind.FILE,carried.id,true);return null;},done->{a.toast("A favourite");a.refresh();},e->a.alert("Could not change that. Nothing was changed."));
                else a.keepToHand(carried,true);
                break;
            case 1: a.intoTemp(carried);break;
            case 2: a.putAway(carried,false);break;
            case 3: a.putAway(carried,true);break;
            default: a.aboutSharing(carried);
        }
        return true;
    }

    /** Where a view is, in the coordinates of the screen's own column, which is what a drag is measured in. */
    private Rect rect(View view) {
        if(view==null||!view.isAttachedToWindow())return new Rect();
        int[] at=new int[2],root=new int[2];view.getLocationOnScreen(at);a.root.getLocationOnScreen(root);
        // Include the surface's translation. Layout offsets alone still point at the cell before the swipe.
        return new Rect(at[0]-root[0],at[1]-root[1],at[0]-root[0]+view.getWidth(),at[1]-root[1]+view.getHeight());
    }

    /**
     * Where a view is seen, in the same coordinates: {@link #rect} of a scrolled view is where its inside is, moved up by
     * however far it is scrolled, so its window is asked for from where it is scrolled to.
     */
    private Rect seen(View view) {
        return rect(view);
    }

    private boolean inside(View view,float x,float y) {
        if(view==null||view.getParent()==null||!view.isShown())return false;
        return rect(view).contains((int)x,(int)y);
    }

    /** Which grid the carried icon came out of: Home's, the pop-up's, or neither (the dock). */
    private GridLayout from() {
        if(a.lifted==null)return null;
        // Kept from the start of the carry: a page turned under it has drawn its icons anew.
        if(carriedFrom!=null)return carriedFrom;
        if(a.lifted.getParent() instanceof GridLayout&&homeGrid((GridLayout)a.lifted.getParent()))return (GridLayout)a.lifted.getParent();
        if(folder.grid!=null&&a.lifted.getParent()==folder.grid)return folder.grid;
        return null;
    }

    private void over(float x,float y) {
        if(a.dragging==null)return;
        outOfCard(x,y);
        zone=Grid.zone(inside(dock.row,x,y),folder.isOpen(),inside(folder.card,x,y),inside(desk,x,y));
        light(zone);
        if(android.util.Log.isLoggable("MininotesCarry",android.util.Log.DEBUG))android.util.Log.d("MininotesCarry",(int)x+","+(int)y+" "+zone+" card "+(folder.card==null?"-":rect(folder.card).toShortString()+" "+folder.card.getVisibility())
            +" shows "+folder.name()+" aside "+folder.aside()+" entered "+cardEntered);
        springKey=null;springDo=null;
        boolean homeSeen=!folder.isOpen()||folder.aside();
        // Carried out of Home's rows - up under the app's name, down short of the dock - is a turn of the page too.
        if((zone==Grid.Zone.HOME||zone==Grid.Zone.NONE||zone==Grid.Zone.UP)&&from()!=null&&homeSeen)edgeWatch(x,y);else cancelEdge();
        // Round the card is Home, which is what is seen there (the owner, 2026-10-03: in and out of a group at any level).
        if(zone==Grid.Zone.HOME||zone==Grid.Zone.UP&&!folder.amongFavourites()) {
            Laid under=homeAt(x,y);
            across(under==null?null:under.canvas,gridScroll,under,x,y);
        }
        else if(zone==Grid.Zone.CARD&&folder.amongFavourites()&&from()==folder.grid)reorderOver(x,y);
        else if(zone==Grid.Zone.CARD&&!folder.amongFavourites()) {
            // Held on ‹ and the collection above, in a card inside another: up a level. Out of the card is Home.
            if(folder.back!=null&&inside(folder.back,x,y)&&!folder.listing()){a.markOnto(null);aim(null,null);
                springKey="up:"+folder.id();springDo=folder::up;}
            else across(folder.grid,folder.scroll,folder.laid,x,y);
        }
        else{a.markOnto(null);aim(null,null);}
        spring();
    }

    /**
     * Held a moment on a collection while carrying, it opens - its card over Home, or one level in, in the card - and held on
     * the card's name, the card goes up a level: so a thing goes in and out of collections at any depth in one carry, as
     * on a phone's own home screen. What it came out of, for whether letting go moves it into something else.
     */
    private String outOf,springKey,springAt;private Runnable springDo;
    private final Runnable springing=this::sprung;
    private void sprung(){if(android.util.Log.isLoggable("MininotesCarry",android.util.Log.DEBUG))android.util.Log.d("MininotesCarry","held: "+springAt);
        Runnable then=springDo;springAt=null;if(a.dragging!=null&&then!=null)then.run();}
    private void spring() {
        if(springKey==null){if(springAt!=null){desk.removeCallbacks(springing);springAt=null;}return;}
        if(springKey.equals(springAt))return;
        desk.removeCallbacks(springing);springAt=springKey;desk.postDelayed(springing,650);
    }

    /** Only the card read and drawn again, as under a carry, when Home itself is not drawn again: see {@link #refresh}. */
    void refreshCard() {
        if(!folder.isOpen())return;
        final List<MainActivity.Step> path=new ArrayList<>(a.trail);
        a.background.submit(()->{Map<String,Object> got=new HashMap<>();folder.read(path,got);return got;},
            got->{if(folder.isOpen())folder.fill(path,got);},e->{});
    }

    /**
     * A thing carried out of a card: the card steps aside as soon as the finger leaves it, and the whole of Home is where it
     * can be let go (see {@link Folder#stepAside}). Not on a hold at its edge, as when the card filled the screen: its top
     * edge is its name, where a hold goes up a level, and a hold there stepped it aside first (seen on the Graphene,
     * 2026-10-03, when a carry into a card opened what lay behind it instead).
     */
    private boolean cardEntered=true;
    private void outOfCard(float x,float y) {
        if(!folder.isOpen()||folder.aside()||folder.listing()||from()==null||folder.card==null)return;
        boolean outside=!rect(folder.card).contains((int)x,(int)y);
        // A card opened under the finger waits for it to have been over the card before stepping aside.
        if(!outside)cardEntered=true;
        if(cardEntered&&outside)folder.stepAside(true);
    }

    /**
     * Over the dimmed Home round a card whose collection is on Home: an empty cell of Home's grid under the finger is where
     * a thing brought up out of the card will stand; anywhere else it takes the first free cell, as anything new does.
     */
    private void behind(float x,float y) {
        a.markOnto(null);
        int[] at=cellAt(grid,homeLaid,x,y);
        aim(at!=null&&Layout.at(homeLaid.drawn,at[0],at[1])==null?homeLaid:null,at);
    }

    /** The dock lit while a thing that can go in it is over it, and the dimmed grid darker while one can go up a level. */
    private void light(Grid.Zone now) {
        boolean dockNow=now==Grid.Zone.DOCK&&a.dragging!=null&&Grid.docks(a.dragging.kind);
        if(dockNow!=dockLit){dockLit=dockNow;dock.lit(dockNow);}
        folder.lit(now==Grid.Zone.UP&&from()!=null&&!folder.amongFavourites());
    }

    /**
     * Over a grid, by the cell under the finger (decision 39): a thing it can go onto or into there is ringed; an empty
     * cell is outlined as the place it will stand; anything else - its own cell, a thing that takes nothing from it -
     * shows nothing, and letting go there puts it back. Near the top or foot of the grid, the grid scrolls.
     */
    private void across(GridLayout in,ScrollView scroll,Laid laid,float x,float y) {
        // Only over the grid it came out of: a favourite carried from the dock goes back to the dock or nowhere.
        // Into any grid seen, from wherever it came: a favourite carried from the dock goes back to the dock or nowhere.
        if(in==null||laid==null||from()==null){a.markOnto(null);aim(null,null);return;}
        Rect window=seen(scroll);
        int reach=a.dp(56),step=a.dp(14);
        if(laid.where!=Where.HOME) {
            if(y<window.top+reach)scroll.scrollBy(0,-step);
            else if(y>window.bottom-reach)scroll.scrollBy(0,step);
        }
        int[] at=cellAt(in,laid,x,y);
        String there=at==null?null:Layout.at(laid.drawn,at[0],at[1]);
        NoteStore.Branch held=there==null?null:laid.things.get(there);
        // Tools, held on, opens, as a collection does; and one of the four is let go on it to be kept there.
        if(held!=null&&held.kind==NoteStore.Branch.Kind.TOOLS) {
            final NoteStore.Branch tools=held;
            if(Grid.tool(a.dragging.kind)&&homeGrid(in)){a.markOnto(laid.icons.get(there));aim(null,null);return;}
            if(!Grid.place(a.dragging.kind)){a.markOnto(null);aim(null,null);springKey="in:"+NoteStore.TOOLS;springDo=()->{cardEntered=false;folder.openPlace(tools);};return;}
        }
        Grid.LetGo does=at==null||(there!=null&&held==null)?Grid.LetGo.BACK:Grid.letGo(a.dragging.kind,held==null?null:held.kind,a.dragging.id.equals(there));
        if(does==Grid.LetGo.MERGE||does==Grid.LetGo.INTO||does==Grid.LetGo.AWAY) {
            a.markOnto(laid.icons.get(there));aim(null,null);
            if(does==Grid.LetGo.INTO&&held.kind==NoteStore.Branch.Kind.COLLECTION) {
                final NoteStore.Branch opening=held;final boolean onHome=homeGrid(in);
                springKey="in:"+held.id;springDo=()->{if(folder.isOpen()&&opening.id.equals(folder.id()))return;if(onHome){cardEntered=false;folder.open(opening);}else folder.into(opening);};
            }
            return;
        }
        a.markOnto(null);
        aim(does==Grid.LetGo.PLACE?laid:null,at);
    }

    /** Let go: onto a thing, into the dock, up a level, or in an empty cell of its grid. */
    private boolean drop(float x) {
        final NoteStore.Branch carried=a.dragging;
        View target=a.onto;a.markOnto(null);
        final Laid placeIn=aimIn;final int[] placeAt=aimAt;aim(null,null);
        if(carried!=null&&placeIn!=null)landedId=carried.id;
        GridLayout came=from();
        boolean fromFavourites=came!=null&&came==folder.grid&&folder.amongFavourites();
        light(Grid.Zone.NONE);
        if(carried==null)return false;
        // Wherever a drop does nothing, Home is drawn again as it is: icons parted on the way there close up again.
        if(zone==Grid.Zone.DOCK) {
            if(!Grid.docks(carried.kind)){stale=true;a.toast("Only a note or a folder goes in the dock");return true;}
            toDock(carried,dock.slotAt(x,a.lifted));
            return true;
        }
        // Among the favourites, let go on another: put before it in their one order (decision 74); let go in the room after them,
        // last. Anywhere else it is only listed, not kept: it goes to the dock from there, and nowhere else.
        if(fromFavourites&&zone==Grid.Zone.CARD&&folder.laid!=null) {
            NoteStore.Branch before=target!=null&&target.getTag() instanceof NoteStore.Branch?(NoteStore.Branch)target.getTag():null;
            reorder(carried,before);return true;
        }
        if(fromFavourites||came==null){stale=true;return true;}
        // One of the four carried out of Tools onto Home, or onto Tools from Home (decision 72): kept where it is let go.
        if(Grid.tool(carried.kind)) {
            NoteStore.Branch onto=target!=null&&target.getTag() instanceof NoteStore.Branch?(NoteStore.Branch)target.getTag():null;
            if(homeGrid(came)&&onto!=null&&onto.kind==NoteStore.Branch.Kind.TOOLS){keepInTools(carried.id,true);a.toast("Kept in Tools");refresh();return true;}
            if(came==folder.grid&&folder.isOpen()&&(zone==Grid.Zone.HOME||zone==Grid.Zone.UP)) {
                keepInTools(carried.id,false);
                final Map<String,Layout.Spot> spots=placeIn!=null&&placeIn.where==Where.HOME?homeMove(placeIn,carried.id,placeAt[0],placeAt[1]):null;
                if(spots!=null)placesOn(spots);
                folder.close();a.toast(carried.name+" is on Home now");refresh();return true;
            }
        }
        // What a place only lists - Temp, Recent, Tools - is not moved from there: it stays where it really is.
        if(came==folder.grid&&folder.listing()){stale=true;return true;}
        boolean inCard=zone==Grid.Zone.CARD,onHome=zone==Grid.Zone.HOME||zone==Grid.Zone.UP;
        // Let go on Home round the card, which stepped aside or not: the card goes once the thing is there.
        final Runnable leftCard=onHome&&folder.isOpen()?folder::close:null;
        if(target!=null&&target.getTag() instanceof NoteStore.Branch) {
            NoteStore.Branch onto=(NoteStore.Branch)target.getTag();
            String container=inCard?folder.id():Things.HOME;
            Grid.Onto does=Grid.onto(carried.kind,onto.kind);
            // On the archive or the bin: put away there, as its menu puts it away (decision 41).
            // On Temp: asked for how long (decision 71).
            // Whatever leaves Home for a place leaves its spot empty: the others are kept where they are drawn now (the owner,
            // 2026-10-14: "the spot left behind should stay empty"). An icon never placed by hand took the first free cell.
            if(does==Grid.Onto.AWAY&&homeGrid(came)&&homeLaid!=null&&homeLaid.spots!=null) {
                final List<NoteStore.Branch> others=new ArrayList<>();
                for(NoteStore.Branch one:homeLaid.lines)if(!one.id.equals(carried.id))others.add(one);
                final Map<String,Layout.Spot> held=new HashMap<>(homeLaid.spots);held.remove(carried.id);
                a.background.submit(()->{a.store.placeOnPages(others,held);return null;},done->placesOn(held),e->{});
            }
            if(does==Grid.Onto.AWAY&&onto.kind==NoteStore.Branch.Kind.TEMP){stale=true;a.intoTemp(carried);}
            else if(does==Grid.Onto.AWAY)away(carried,onto.kind==NoteStore.Branch.Kind.BIN);
            else if(does==Grid.Onto.MERGE)merge(carried,onto,container,inCard);
            else into(carried,onto.id,onto.name,onto.kind,leftCard,null);
            return true;
        }
        if(!inCard&&!onHome){stale=true;return true;}
        final String dest=inCard?folder.id():Things.HOME;
        final Laid laid=placeIn!=null&&(inCard?placeIn==folder.laid:placeIn.where==Where.HOME)?placeIn:null;
        // Where it came from: only its cell changes.
        if(dest.equals(outOf)){if(laid!=null)put(laid,carried,placeAt);else stale=true;return true;}
        // Into another collection than the one it came out of, or onto Home, at any depth: in the empty cell it was let go
        // in, or else the first free one, as anything new takes.
        Runnable there=null;
        if(laid!=null) {
            final List<NoteStore.Branch> lines=new ArrayList<>(laid.lines);lines.add(carried);
            if(laid.spots!=null) {
                final Map<String,Layout.Spot> spots=homeMove(laid,carried.id,placeAt[0],placeAt[1]);
                if(spots!=null)there=()->writeHome(lines,spots);
            } else {
                final Map<String,Integer> cells=Layout.moveTo(laid.drawn,carried.id,placeAt[0],placeAt[1]);
                if(cells!=null)there=()->a.store.place(lines,cells);
            }
        }
        into(carried,dest,inCard?folder.name():"Home",NoteStore.Branch.Kind.COLLECTION,leftCard,there);
        return true;
    }

    /**
     * Held at an edge of the room while carrying. Over a cell, including the last row, it is let go there. In the band
     * round the room, Home scrolls that way: first the row that was under the foot, then on towards the next page, one
     * new page at a time (decision 126).
     */
    private Runnable edgeTurn;private int edgeX,edgeY;private boolean turnedHere;
    private float edgeSpeedX,edgeSpeedY,edgePointerX,edgePointerY;
    private long edgeTime;
    private void edgeWatch(float x,float y) {
        Rect room=rect(pager);
        float band=a.dp(48),maximum=a.dp(520);
        float vx=HomeMotion.edgeSpeed(x,room.left,room.right,band,maximum);
        float vy=HomeMotion.edgeSpeed(y,room.top,room.bottom,band,maximum);
        if(Math.abs(vy)>=Math.abs(vx))vx=0;else vy=0;
        edgePointerX=x;edgePointerY=y;edgeSpeedX=vx;edgeSpeedY=vy;
        int dx=(int)Math.signum(vx),dy=(int)Math.signum(vy);
        if(dx==0&&dy==0){cancelEdge();turnedHere=false;return;}
        if(turnedHere&&dx==edgeX&&dy==edgeY)return;
        if(edgeTurn!=null&&dx==edgeX&&dy==edgeY)return;
        cancelEdge();edgeX=dx;edgeY=dy;
        edgeTurn=()->{
            if(a.dragging==null){edgeTurn=null;return;}
            long now=android.os.SystemClock.uptimeMillis();
            float seconds=Math.min(0.048f,Math.max(0.001f,(now-edgeTime)/1000f));edgeTime=now;
            if(!nudgePage(edgeSpeedX*seconds,edgeSpeedY*seconds)){edgeTurn=null;turnedHere=true;return;}
            over(edgePointerX,edgePointerY);
            if(desk!=null&&edgeTurn!=null)desk.postOnAnimation(edgeTurn);
        };
        edgeTime=android.os.SystemClock.uptimeMillis()+180;
        if(desk!=null)desk.postDelayed(edgeTurn,180);
    }
    /** The world moved one step that way. False once a new page that way is already open and the scroll is at its end. */
    private boolean nudgePage(float dx,float dy) {
        float sx=shiftX-dx,sy=shiftY-dy;
        int[] b=pageBounds();
        float[] limits=scrollLimits();
        boolean outside=sx<limits[0]||sx>limits[1]||sy<limits[2]||sy>limits[3];
        if(outside) {
            if(!beyond) {
                beyond=true;beyondX=dx<0?b[0]-1:dx>0?b[1]+1:seenPageX();beyondY=dy<0?b[2]-1:dy>0?b[3]+1:seenPageY();
                buildPages();limits=scrollLimits();
            }
        }
        sx=Math.max(limits[0],Math.min(limits[1],sx));sy=Math.max(limits[2],Math.min(limits[3],sy));
        if(Math.abs(sx-shiftX)<0.01f&&Math.abs(sy-shiftY)<0.01f)return false;
        show(1f,sx,sy);showFoot();return true;
    }
    private void cancelEdge(){if(edgeTurn!=null&&desk!=null)desk.removeCallbacks(edgeTurn);edgeTurn=null;}

    private void ended(boolean result) {
        hideStrip();
        cancelEdge();turnedHere=false;carriedFrom=null;
        folder.stepAside(false);
        desk.removeCallbacks(springing);springAt=null;springKey=null;springDo=null;outOf=null;cardEntered=true;
        light(Grid.Zone.NONE);a.markOnto(null);aim(null,null);zone=Grid.Zone.NONE;
        if(a.lifted!=null)a.lifted.setVisibility(View.VISIBLE);
        if(a.lifted!=null)a.releaseHold(a.lifted);
        a.dragging=null;a.lifted=null;a.pressing=false;
        // Remove the temporary page, including after a cancelled drag, while keeping the viewport in bounds.
        refresh();
    }

    /**
     * Let go in an empty cell of the grid it came out of: it stays there, and at the first move every other icon is pinned
     * where it is drawn, so nothing closes up behind it (decision 39). The same collection, so nobody it reaches changes and
     * nothing is asked. The icon stands in its cell at once, and the notebook is written behind it, in one go and silently,
     * as the tiles' order always was.
     */
    private void put(Laid in,NoteStore.Branch carried,int[] at) {
        if(in.spots!=null) {
            // Home: on the page in view, which comes to be if it was empty (decision 46); every icon pinned on its page.
            final Map<String,Layout.Spot> spots=homeMove(in,carried.id,at[0],at[1]);
            if(spots==null){stale=true;return;}
            // Put the hidden source in its destination before drag-ended reveals it. Waiting for the
            // worker's read-back briefly showed the old cell again after the shadow disappeared.
            landTile(in,carried,at);
            in.spots.clear();in.spots.putAll(spots);
            for(Laid page:pageLayouts.values()) {
                page.drawn.remove(carried.id);page.icons.remove(carried.id);page.things.remove(carried.id);
            }
            in.drawn.put(carried.id,at.clone());in.icons.put(carried.id,a.lifted);in.things.put(carried.id,carried);
            final List<NoteStore.Branch> lines=new ArrayList<>(in.lines);
            a.background.submit(()->{writeHome(lines,spots);return null;},done->{if(!onCentre())a.toast("Moved to "+said(pageX,pageY).toLowerCase(java.util.Locale.ROOT));refresh();},
                e->{a.alert("Could not put that there. Nothing was changed.");refresh();});
            return;
        }
        final Map<String,Integer> cells=Layout.moveTo(in.drawn,carried.id,at[0],at[1]);
        if(cells==null){stale=true;return;}
        if(in==homeLaid)placesAt(cells);
        landTile(in,carried,at);
        final List<NoteStore.Branch> lines=new ArrayList<>(in.lines);
        a.background.submit(()->{a.store.place(lines,cells);return null;},done->refresh(),
            e->{a.alert("Could not put that there. Nothing was changed.");refresh();});
    }

    /** The dragged view lands immediately; storage success or failure is still reconciled by refresh. */
    private void landTile(Laid in,NoteStore.Branch carried,int[] at) {
        View tile=a.lifted;
        if(tile==null||in.canvas==null||!(tile.getLayoutParams() instanceof GridLayout.LayoutParams))return;
        GridLayout.LayoutParams place=new GridLayout.LayoutParams((GridLayout.LayoutParams)tile.getLayoutParams());
        place.rowSpec=GridLayout.spec(at[0]);place.columnSpec=GridLayout.spec(at[1]);
        place.width=in.width;place.height=in.height;
        if(tile.getParent()!=in.canvas) {
            if(tile.getParent() instanceof ViewGroup)((ViewGroup)tile.getParent()).removeView(tile);
            in.canvas.addView(tile,place);
        } else tile.setLayoutParams(place);
    }

    /**
     * Let go on the archive or the bin: put away there by the same road its menu takes - said, with Undo in the next menu -
     * and a file on the bin asked about first, as its own Delete asks, since there is no bin for files.
     */
    private void away(NoteStore.Branch carried,boolean bin) {
        a.putAway(carried,bin);
    }

    /** A note or a folder at a place in the dock, the rest moved along; whether it is a favourite does not change (decision 190). */
    private void toDock(final NoteStore.Branch thing,final int slot) {
        a.background.submit(()->{a.store.toDock(thing.kind,thing.id,slot);return null;},
            done->a.refresh(),e->{a.alert("Could not put that in the dock. Nothing was changed.");a.refresh();});
    }

    /**
     * The collections above a line and then the line itself: what a sharing rule is looked for along. A file's is what
     * keeps it - Home, a collection or a note - and then the file, since a file has no row in the tree of its own.
     */
    private List<String> pathOf(NoteStore.Branch line) {
        if(line.kind!=NoteStore.Branch.Kind.FILE)return a.store.pathOf(line.id);
        // Where it is kept, not where it is shown: one shared with this phone is kept on Home wherever it shows (decision 93).
        return a.store.filePath(line.id);
    }

    /** Where a line would be, once it is inside {@code into}: the path down to that, and then the line. */
    private List<String> pathInto(String into,String id) {
        List<String> path=NoteStore.home(into)?new ArrayList<>():a.store.pathOf(into);
        path.add(id);
        return path;
    }

    /** Names of the devices by address, for the words that say who a move reaches. */
    private Map<String,String> names() {
        Map<String,String> names=new HashMap<>();
        for(NoteStore.Contact contact:a.store.addresses())names.put(contact.address,contact.name);
        return names;
    }

    /**
     * Into a collection, or a file into a note - or up a level, out of a pop-up. What changes who it reaches is asked
     * first, as every move is; what the notebook refuses, such as a collection into its own inside, is said as it says it.
     *
     * @param after what to do once it has gone, or null for nothing more than drawing Home again
     * @param there what to write on the worker once it has gone - the cell it was let go in - or null for the first free one
     */
    private void into(final NoteStore.Branch moved,final String dest,final String destName,final NoteStore.Branch.Kind destKind,final Runnable after,final Runnable there) {
        a.background.submit(()->{
            List<Sharing.Rule> rules=a.store.shares();
            // Into a note, a file is reached along the note's own path; into a collection, or Home, along that one's.
            List<String> from=pathOf(moved),to=pathInto(dest,moved.id);
            Sharing.Change change=Sharing.moving(rules,from,to);
            boolean reaches=!Sharing.audience(rules,from).isEmpty()||!Sharing.audience(rules,to).isEmpty();
            boolean theirs=moved.kind!=NoteStore.Branch.Kind.FILE&&a.store.someoneElses(moved.kind,moved.id);
            boolean alone=moved.kind==NoteStore.Branch.Kind.FILE&&!a.store.looseAudience(moved.id).isEmpty();
            return new Object[]{change,names(),reaches,theirs,alone};
        },found->{
            Sharing.Change change=(Sharing.Change)found[0];
            final boolean reaches=(Boolean)found[2],theirs=(Boolean)found[3],alone=(Boolean)found[4];
            final Runnable go=()->doInto(moved,dest,destName,destKind,reaches,after,there);
            if(!change.any()){go.run();return;}
            String where=NoteStore.home(dest)?"Home":"“"+destName+"”";
            if(moved.kind==NoteStore.Branch.Kind.FILE) {
                a.new Box().setTitle("This changes who can read it")
                    .setMessage(a.changeSaid("Moving “"+moved.name+"” into "+where+" changes who receives it.",change,a.castNames(found[1]),
                        Sharing.fileMoveSaid(alone)))

                    .setPositiveButton("Move anyway",(d,w)->go.run())
                    .setOnCancelListener(d->a.refresh()).show();
                return;
            }
            final Runnable only=()->placeOnly(moved,dest,where,after,there);
            // What somebody shares stays where their sharing has it, linked as before, and is shown where it was put.
            if(theirs){only.run();return;}
            // One of this person's own: asked, never shared or stopped by a move alone (the owner, 2026-10-03: "we should be
            // asked if we want to share").
            boolean starts=!change.gained.isEmpty();
            a.new Box().setTitle(starts?"Share “"+moved.name+"” with them?":"Stop sharing “"+moved.name+"”?")
                .setMessage(a.changeSaid(starts?"In "+where+" it would go to more people.":"Out of where it is, it would stop going to some people.",
                    change,a.castNames(found[1]),starts?"Or only put it there on this phone: nobody new gets it.":"Or only put it there on this phone: it stays shared as it is."))
                .setPositiveButton(starts?"Share it":"Stop sharing",(d,w)->go.run())
                .setNeutralButton("Only put it here",(d,w)->only.run())
                .setOnCancelListener(d->a.refresh()).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    /**
     * Shown in a collection, or on Home, on this device only: where it really is and who has it are as they were (see
     * NoteStore.showIn). In the cell it was let go in, as a move puts it; one Undo from where it was shown.
     */
    private void placeOnly(final NoteStore.Branch moved,final String dest,final String where,final Runnable after,final Runnable there) {
        a.background.submit(()->{
            String was=a.store.showIn(moved.kind,moved.id,dest);
            if(there!=null)try{there.run();}catch(RuntimeException unplaced){/* shown there, in the first free cell */}
            return was;
        },was->{
            a.canUndo(moved.name,()->a.store.showIn(moved.kind,moved.id,(String)was));
            a.toast("Put in "+where+", shared as before");
            if(after!=null)after.run();
            a.refresh();
        },e->{a.alert(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not put that there. Nothing was changed.");a.refresh();});
    }

    private void doInto(final NoteStore.Branch moved,final String dest,final String destName,final NoteStore.Branch.Kind destKind,final boolean reaches,final Runnable after,final Runnable there) {
        a.background.submit(()->{
            // Where it was, before it is anywhere else: an undo has to know the place to put it back into.
            String was;
            if(moved.kind==NoteStore.Branch.Kind.FILE){NoteStore.Held held=a.store.file(moved.id);was=held==null?Things.HOME:held.note;}
            else was=moved.kind==NoteStore.Branch.Kind.PAGE?a.store.bookOf(moved.id):a.store.collectionOfBook(moved.id);
            a.store.moveInto(moved.kind,moved.id,dest);
            // And in the cell it was let go in. Moved it is, whatever this says: failing, it stands in the first free cell.
            if(there!=null)try{there.run();}catch(RuntimeException unplaced){/* moved, and in the first free cell */}
            return was==null||was.isEmpty()?Things.HOME:was;
        },was->{
            a.canUndo(moved.name,()->a.store.moveInto(moved.kind,moved.id,was));
            a.toast(NoteStore.home(dest)?"Moved to Home":"Moved into "+destName);
            if(after!=null)after.run();
            a.refresh();
            // Into something somebody else reads is a disclosure: it goes now, as sharing does.
            if(moved.kind==NoteStore.Branch.Kind.FILE){if(destKind==NoteStore.Branch.Kind.PAGE)a.filesChanged(NoteStore.Branch.Kind.PAGE,dest);}
            else if(reaches)a.sendAfterSharing(moved.kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:Sharing.Scope.COLLECTION,moved.id);
        },e->{a.alert(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not move that. Nothing was changed.");a.refresh();});
    }

    /**
     * A note let go on a note: a new collection, Untitled, where the one let go on was, holding both - and its pop-up
     * opened with the name ready to be typed over. Asked first if it changes who either of them reaches.
     */
    private void merge(final NoteStore.Branch dropped,final NoteStore.Branch onto,final String container,final boolean inCard) {
        a.background.submit(()->{
            List<Sharing.Rule> rules=a.store.shares();
            Map<String,Boolean> gained=new java.util.LinkedHashMap<>(),lost=new java.util.LinkedHashMap<>();
            boolean reaches=false;
            for(NoteStore.Branch thing:new NoteStore.Branch[]{dropped,onto}) {
                List<String> from=a.store.pathOf(thing.id),to=pathInto(container,thing.id);
                Sharing.Change change=Sharing.moving(rules,from,to);
                gained.putAll(change.gained);lost.putAll(change.lost);
                if(!Sharing.audience(rules,from).isEmpty())reaches=true;
            }
            return new Object[]{new Sharing.Change(gained,lost),names(),reaches};
        },found->{
            Sharing.Change change=(Sharing.Change)found[0];
            final boolean reaches=(Boolean)found[2];
            final Runnable go=()->doMerge(dropped,onto,inCard,reaches);
            if(!change.any()){go.run();return;}
            a.new Box().setTitle("This changes who can read them")
                .setMessage(a.changeSaid("Putting “"+dropped.name+"” and “"+onto.name+"” together changes who receives them.",change,a.castNames(found[1]),
                    "Whoever starts receiving them gets them now. What has already reached somebody stays with them."))
                .setPositiveButton("Put them together",(d,w)->go.run())
                .setOnCancelListener(d->a.refresh()).show();
        },e->a.alert(MainActivity.READ_FAILED));
    }

    private void doMerge(final NoteStore.Branch dropped,final NoteStore.Branch onto,final boolean inCard,final boolean reaches) {
        a.background.submit(()->a.store.merge(dropped.id,onto.id),made->{
            a.nameNext=made.id;
            if(!inCard){a.trail.clear();a.trail.add(new MainActivity.Step(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,"Home"));}
            a.trail.add(new MainActivity.Step(NoteStore.Branch.Kind.COLLECTION,made.id,made.name));
            if(showing())folder.show();else a.browse();
            if(reaches)a.sendAfterSharing(Sharing.Scope.COLLECTION,made.id);
        },e->{a.alert(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not put those together. Nothing was changed.");a.refresh();});
    }

    /** Whether the finger has moved far enough, from where it went down, to be more than a tap: the platform's own slop. */
    boolean moved(float dx,float dy) {
        int slop=ViewConfiguration.get(a).getScaledTouchSlop();
        return Math.abs(dx)>slop||Math.abs(dy)>slop;
    }
}
