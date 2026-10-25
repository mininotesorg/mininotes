package org.mininotes.android;

import org.mininotes.desktop.platform.content.Context;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;

/** Windows UI. Shared Java classes retain their package to avoid widening their APIs. */
public final class Desktop {
    static final Color PAPER=new Color(250,249,244),INK=new Color(43,48,43),QUIET=new Color(111,117,108),ACCENT=new Color(48,99,72);
    // The look is set before a single component is made, fields included: a tree made first keeps the old one.
    static{DesktopScale.apply();DesktopUi.install();}
    // Pictures added on this PC are drawn smaller with ImageIO (decision 112): said before any is kept.
    static{Shrink.painter=DesktopPictures.PAINTER;}
    final JFrame frame=new JFrame("Mininotes");
    final JTextField title=new JTextField();
    final Paper page=new Paper();
    final JLabel connection=new Dot("Connecting…");
    /** Whether this PC's notebook is encrypted, always in sight; a click opens Security. */
    final JLabel security=new JLabel();
    /** This build, beside the name in the bar - and, when a newer one is out, the way to it. */
    static final String VERSION="0.4.030";
    final JLabel version=new JLabel();
    /** Beside the version, only while a newer one is out: an outlined button, so it reads as one to press. */
    final JButton updateButton=new JButton();
    /** "new" at the top, with how many, while anything is new: a click lists them (the owner, 2026-10-14). */
    final JButton newButton=new JButton();
    String newestKnown="";
    /** The paperclip at the foot of the note, with how many files the note holds. */
    final JButton clip=new JButton(paperclip());
    /** A paperclip drawn in the quiet grey, one stroke: the emoji one came out in Windows' own colours, looking struck through. */
    static Icon paperclip() {
        return new Icon(){
            public int getIconWidth(){return 16;}public int getIconHeight(){return 20;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
                g.setColor(QUIET);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                // Inner leg up, round the top, the long outer leg down, round the bottom, and up again.
                p.moveTo(x+6,y+6);p.lineTo(x+6,y+14);
                p.curveTo(x+6,y+16.5,x+10,y+16.5,x+10,y+14);p.lineTo(x+10,y+4.5);
                p.curveTo(x+10,y+0.5,x+3.5,y+0.5,x+3.5,y+4.5);p.lineTo(x+3.5,y+15);
                p.curveTo(x+3.5,y+20.5,x+12.5,y+20.5,x+12.5,y+15);p.lineTo(x+12.5,y+7);
                g.draw(p);g.dispose();
            }
        };
    }
    /** Beside the paperclip: press it and a recording starts, kept with the note when it stops. */
    final JButton mic=new JButton(microphone());
    /** Beside it, the camera: Windows' own, whose photo comes into the note (decision 194; see DesktopCamera). */
    final JButton camera=new JButton(glyph("camera",19));
    DesktopRecorder recorder;
    /** A microphone drawn as the paperclip is, in the same grey: the emoji would come in Windows' own colours. */
    static Icon microphone() {
        return new Icon(){
            public int getIconWidth(){return 16;}public int getIconHeight(){return 20;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
                g.setColor(QUIET);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                // The head, the cradle round it, and the stand.
                g.draw(new java.awt.geom.RoundRectangle2D.Float(x+5,y+1.5f,6,10.5f,6,6));
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(x+2.5,y+9);p.curveTo(x+2.5,y+17,x+13.5,y+17,x+13.5,y+9);
                p.moveTo(x+8,y+15.5);p.lineTo(x+8,y+18.5);p.moveTo(x+5,y+18.5);p.lineTo(x+11,y+18.5);
                g.draw(p);g.dispose();
            }
        };
    }
    /** The menu's button, and the bar's row it goes back to while Recent is not shown (see placeMenu). */
    private JButton menuButton;private JPanel barActions;
    /**
     * The menu over Recent while Recent is shown and open, at the top of its column; else at the end of the bar, so it is
     * always there (decision 192).
     */
    void placeMenu() {
        if(menuButton==null||barActions==null)return;
        boolean over=openList.isShowing()&&!openFolded;
        if(menuButton.getParent()==null)barActions.add(menuButton);
        // Over Recent, its own line there (see DesktopOpenList.menuRow); else the bar's button.
        if(menuButton.isVisible()==!over&&openList.menuRow.isVisible()==over)return;
        menuButton.setVisible(!over);openList.menuRow.setVisible(over);
        rebar();
    }
    /** One of the icon set's glyphs, as an icon in the ink. */
    static Icon glyph(String name,int size) {
        return new Icon(){public int getIconWidth(){return size;}public int getIconHeight(){return size;}
            public void paintIcon(Component c,Graphics g,int x,int y){DesktopIcons.draw((Graphics2D)g,name,x,y,size,INK);}};
    }
    /** Three lines, one over the other: the menu's mark, beside the word Menu. */
    static Icon lines() {
        return new Icon(){
            public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g0,int x,int y){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(INK);for(int i=0;i<3;i++)g.fillRoundRect(x+1,y+3+i*5,14,2,2,2);g.dispose();}
        };
    }
    /** Three dots in a row: the ⋯ of the bar and of a card, drawn so both are the same. */
    static Icon dots() {
        return new Icon(){
            public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g0,int x,int y){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(INK);for(int i=0;i<3;i++)g.fillOval(x+1+i*5+(i>0?i:0),y+7,3,3);g.dispose();}
        };
    }
    final DesktopUi.Text status=DesktopUi.quiet("Opening your pad…"),standing=DesktopUi.quiet(" ");
    /** The strip the status lies on, over the foot of the window (decision 97). */
    JPanel statusStrip;
    /** Before the title, the icon the open note wears; clicked, the box it is chosen in (see DesktopIconPicker). */
    final JButton noteFace=new JButton();
    /** What the open note wears, as last read: its icon's name, empty for its own, and its picture, null for none. */
    private String noteIcon="";private byte[] noteImage;
    /** The open note's sync mark, first on the line under its title, as on the phone (see DesktopMark). */
    final JLabel syncMark=new JLabel();
    SyncMark noteMark;
    /** After the mark, the people the open note reaches: a round each, tinted by where they stand. */
    final JPanel rounds=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
    /** Who they are, as the rounds were last drawn: what the gallery and the tests look at. */
    java.util.List<SyncStatus.Person> peopleShown=List.of();
    /** After the people, what the open note's syncing is doing, in a few words in the mark's colour (see NoteLine). */
    final JLabel noteWords=new JLabel();
    /**
     * Those words in English, as NoteLine said them: the line shows them in the language chosen, and what the line is
     * saying now (still saving, still sending) is asked of these, never of the translated words on the screen.
     */
    private String noteWordsSaid="";
    /** What a click on those words does: null for what the mark does, or the reasons behind "see why". */
    private Runnable wordsClicked;
    private final javax.swing.Timer wordsAway=new javax.swing.Timer(6000,e->noteSays(null,null,null));
    /** Every collection's and note's mark, however deep, read with the tree: drawn on its line and on its card. */
    volatile Map<String,SyncMark> marks=Map.of();
    private static final int TREE_MARK=16;
    /** A line's face in the tree: small enough for a line, big enough that its icon still reads. */
    private static final int TREE_FACE=20;
    /** Taking hold of things and putting them elsewhere, in the tree and on the cards (see DesktopMoving). */
    final DesktopMoving moving=new DesktopMoving(this);
    final JTree tree=new JTree(new DefaultMutableTreeNode("Home")){
        // Where a dragged thing would land is drawn over the lines, after them.
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);moving.paint(this,g);}
    };
    /** The main area: Home, or the note that is open, filling the window (docs/HOME.md, decision 2). */
    private final JPanel mainArea=new JPanel(new CardLayout());
    /** Whether the note is what is on the screen, rather than Home. */
    boolean onPage;
    /** What is open, the newest first, as cards over the window: the tab bar's place (see DesktopOverview). */
    final DesktopOverview overview=new DesktopOverview(this);
    /** What is open, down the left of the page area (decision 77). */
    final DesktopOpenList openList=new DesktopOpenList(this);
    /** The ⋯ menu pinned open, on the right of Recent, while Show on Home says so. */
    final DesktopPinnedMenu pinnedMenu=new DesktopPinnedMenu(this);
    /** The open note's attachments, as cards under the page. */
    DesktopFileCards fileCards;
    /** Where the cursor was in each note, so going back to it goes back to the same place. */
    private final Map<String,Integer> carets=new HashMap<>();
    /** Home: the grid, the +, the search, the dock, and a collection's card over them (see DesktopHome). */
    DesktopHome home;
    /** Everything, as the tree last drew it. */
    private List<NoteStore.Branch> everything=new ArrayList<>();
    /** What is starred, in the order it was starred, for the tree's Favourites. */
    private volatile List<NoteStore.Branch> favouritesNow=List.of();
    private JPanel side;private JSplitPane split;
    /** How wide the side panel is, as last pulled; the grip that pulls it; the narrowest it is kept at. */
    private int sideWidth=280;private static final int GRIP=9,MIN_SIDE=180;
    /**
     * The line between the side panel and the page. What is seen is one hair-thin rule, and at its middle a small
     * rounded handle with the arrow in it - the handle says the line can be taken hold of, the arrow that it
     * folds. Under the pointer the rule turns green and the pointer becomes the two-way arrow. A click on the
     * handle folds the panel; anywhere else on the line, pressing and pulling changes its width.
     */
    private final class Grip extends javax.swing.plaf.basic.BasicSplitPaneDivider {
        private boolean over;
        /** Whether this is the line before the list of what is open, on the right, rather than the tree's on the left. */
        private final boolean right;
        Grip(javax.swing.plaf.basic.BasicSplitPaneUI ui){this(ui,false);}
        Grip(javax.swing.plaf.basic.BasicSplitPaneUI ui,boolean right) {
            super(ui);this.right=right;setBorder(null);setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){over=true;repaint();}
                public void mouseExited(MouseEvent e){over=false;repaint();}
                public void mouseClicked(MouseEvent e){if(Math.abs(e.getY()-getHeight()/2)<=HANDLE/2){if(right)foldOpenList(!openFolded);else showTree(!treeWanted);}}
                // Pulled narrower than a list can be read at, it folds, as a click on the handle folds it; the tree, pulled
                // out from the edge, opens.
                public void mouseReleased(MouseEvent e){SwingUtilities.invokeLater(right?Desktop.this::openListPulled:Desktop.this::treePulled);}
            });
        }
        private static final int HANDLE=34;
        @Override public void paint(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),mid=w/2;
            // In the app's colour, as the bar and the side list are (see paintApp): a cream band down a coloured window was
            // the one thing left that was not (the owner's picture, 2026-10-05).
            Color shelf=DesktopLook.wash(homeColour(),DesktopUi.SHELF,0.12f,0.72f,homeTone());
            g.setColor(shelf);g.fillRect(0,0,w,h);
            g.setColor(over?ACCENT:DesktopUi.mix(shelf,INK,0.12f));g.fillRect(over?mid-1:mid,0,over?2:1,h);
            int top=h/2-HANDLE/2;
            g.setColor(over?DesktopUi.mix(ACCENT,Color.WHITE,0.85f):shelf);g.fillRoundRect(0,top,w,HANDLE,w,w);
            g.setColor(over?ACCENT:DesktopUi.LINE);g.drawRoundRect(0,top,w-1,HANDLE-1,w,w);
            g.setColor(over?ACCENT:QUIET);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            // The arrow says which way a click moves the panel: the tree folds left; the open list folds right, and opens left.
            int cy=h/2;boolean leftward=right?openFolded:treeWanted;
            if(leftward)g.drawPolyline(new int[]{mid+2,mid-2,mid+2},new int[]{cy-5,cy,cy+5},3);
            else g.drawPolyline(new int[]{mid-2,mid+2,mid-2},new int[]{cy-5,cy,cy+5},3);
            g.dispose();
        }
    }
    private final javax.swing.Timer keepWidth=new javax.swing.Timer(600,e->keepSideWidth());

    /** The list of what is open: the line before it, how wide it was last pulled, and whether it is folded to the edge. */
    private JSplitPane openSplit;private int openWidth=DesktopOpenList.WIDE;private boolean openFolded,placingOpen;
    private static final int MIN_OPEN=120;
    private final javax.swing.Timer keepOpenWidth=new javax.swing.Timer(600,e->keepOpenList());
    {keepOpenWidth.setRepeats(false);}
    private void keepOpenList() {
        int w=openWidth;boolean folded=openFolded;
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString("openWidth",Integer.toString(w)).putString("openFolded",Boolean.toString(folded)).apply();return null;},done->{},x->{});
    }
    private boolean waitingForWidth;
    /** Once, when the split first has a width: then the list is placed. */
    private void placeWhenSized() {
        if(waitingForWidth)return;
        waitingForWidth=true;
        openSplit.addComponentListener(new java.awt.event.ComponentAdapter(){@Override public void componentResized(java.awt.event.ComponentEvent e){
            if(openSplit.getWidth()<=0)return;
            openSplit.removeComponentListener(this);waitingForWidth=false;placeOpenList();}});
    }
    /** The open list shown or not as {@link DesktopOpenList#refresh} decided, at its width, or folded to the edge with its line still there. */
    void placeOpenList() {
        if(openSplit==null)return;
        placingOpen=true;
        try {
            if(!openList.isVisible()){openSplit.setDividerSize(0);return;}
            openSplit.setDividerSize(GRIP);
            // Not laid out yet: placed once it has a width, and not asked again and again meanwhile, which kept the screen's
            // thread busy for as long as the window had none (a test window closing spun it for an hour, 2026-10-19).
            int w=openSplit.getWidth();if(w<=0){placeWhenSized();return;}
            openSplit.setDividerLocation(Math.max(0,w-GRIP-(openFolded?0:openWidth)));
        } finally{placingOpen=false;placeMenu();}
        openSplit.getComponent(0).repaint();
        for(Component one:openSplit.getComponents())if(one instanceof Grip)one.repaint();
    }
    /** Folded to the edge, or opened again at the width it had: the handle's click. */
    void foldOpenList(boolean folded){openFolded=folded;placeOpenList();keepOpenList();}
    /** Let go of the line: narrower than a list can be read at, it folds. */
    private void openListPulled() {
        if(!openList.isVisible())return;
        int wide=openSplit.getWidth()-openSplit.getDividerLocation()-GRIP;
        if(wide<MIN_OPEN&&!openFolded)foldOpenList(true);
    }
    private void keepSideWidth(){int w=sideWidth;disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString("sideWidth",Integer.toString(w)).apply();return null;},done->{},x->{});}
    /** Whether the tree is wanted beside Home and the page: off unless it was switched on (decision 26). */
    private volatile boolean treeWanted;
    /** Where the tree's switch is kept. A new key, so the tree 0.1 showed by default starts off, as decision 26 has it. */
    static final String TREE="treePanel";
    final Context context;
    final NoteStore store;
    final Keys keys;
    /** Private notes and folders, and the file that keeps them the same for everybody (decision 111). */
    final DesktopPrivate privately;
    /** Notes and folders kept as files too, and what keeps them up to date (docs/FILES.md, decision 162). */
    final DesktopOnDisk onDisk;
    /** Over the open note while its file is lost: what happened, Find it… and Keep it inside only. */
    final DesktopOnDisk.Strip lostStrip=new DesktopOnDisk.Strip();
    /** The line under a stamped note's title: stamped on Minima, waiting, or changed since (decision 218). */
    final DesktopStamp.Line stampLine=new DesktopStamp.Line(this);
    /** A contact in trouble, under the bar (see DesktopTroubles). */
    DesktopTroubles troubles;
    /** The hook this window set for where a file is linked (see NoteStore.linkedHere), let go when it closes. */
    private java.util.function.Function<String,String> linkedHook;
    final Background disk=new Background(SwingUtilities::invokeLater),network=new Background(SwingUtilities::invokeLater);
    final Background connectivity=new Background(SwingUtilities::invokeLater);
    final javax.swing.Timer autosave,syncLater;
    private NoteStore.Note base;
    private boolean drawing,dirty,saving;
    /** Something arrived for the open note while it was being written down: the page follows once the writing is back. */
    private boolean followAfterSave;
    /** How many writings of the page have come back, so a read of the notebook overtaken by one is not trusted. */
    private int pageWritings;
    volatile boolean closing;
    private Runnable afterSave;
    private String recoveryToken;private boolean recovering;private long editorGeneration,editorSession;
    private int treeRequest,noteRequest;
    final boolean offline;
    volatile boolean listenInTray;
    /**
     * Who wrote what, drawn in each writer's colour in a note two or more people wrote in (see Writers): whether it is,
     * and the colours, read from the notebook when a note opens and whenever one is chosen.
     */
    boolean whoWrote=true;
    Writers.Palette palette=Writers.Palette.plain();
    private TrayIcon tray;
    private boolean nodeStarting;
    private String currentAddress="";
    private NoteStore.Branch selected;
    private final Set<String> pendingOffers=new HashSet<>();
    private final Map<String,Sharing.Level> offers=new HashMap<>();
    private final Map<String,Long> offeredAt=new HashMap<>();
    private final Map<String,Long> due=new HashMap<>();
    /**
     * The reading rung and the usual strength of a colour, as the phone keeps them (see DesktopLook). The rung is this
     * PC's: what every note is read at unless it has one of its own (see Reading). The usual strength is the same for
     * colours: what a thing is washed at unless it has one of its own (see toneOf; decision 107).
     */
    int rung=DesktopLook.FIRST,usual=Tint.FIRST_TONE;
    /** The notes with a rung of their own, by id, read with the tree, so a note's menu knows it before it is open. */
    private volatile Map<String,Integer> rungsNow=Map.of();
    /** The notes shown without their writing lines on this PC, as last read, for their menus. */
    private volatile Set<String> plainNow=Set.of();
    /** Where the window was when it closed - Home, or a note - so it opens there again, as the phone does. */
    static final String WHERE="where",HOME_SHOWN="home",NOTE_SHOWN="note:";
    /** The note the window opens on, or null when it opens on Home: the first frame waits for it. */
    private String startNote;
    JScrollPane paperScroll;private JPanel editorPanel;private Runnable fitNote=()->{};
    boolean compactNote;private int readingTop,readingInto,readingWidth,readingHeight;private String readingId;private boolean restoringReading;
    private void rememberReading(){
        if(restoringReading||base==null||paperScroll==null)return;JViewport port=paperScroll.getViewport();
        if(readingId!=null&&readingId.equals(base.id)&&(port.getWidth()!=readingWidth||port.getHeight()!=readingHeight))return;
        try{Rectangle seen=port.getViewRect();int at=page.viewToModel2D(new java.awt.Point(seen.x,seen.y));var line=page.modelToView2D(at);
            if(line!=null){readingTop=at;readingInto=seen.y-(int)Math.round(line.getY());readingWidth=port.getWidth();readingHeight=port.getHeight();readingId=base.id;}}
        catch(Exception unavailable){/* next layout */}
    }
    private void restoreReading(){
        if(base==null||paperScroll==null||!Objects.equals(readingId,base.id))return;
        String id=base.id;long generation=editorGeneration;int top=readingTop,into=readingInto;restoringReading=true;page.holding(true);
        SwingUtilities.invokeLater(()->{try{
            if(base==null||!id.equals(base.id)||generation!=editorGeneration)return;
            JViewport port=paperScroll.getViewport();var line=page.modelToView2D(Math.min(top,page.getDocument().getLength()));
            if(page.isFocusOwner()&&System.currentTimeMillis()-lastWriting<2000){var caret=page.modelToView2D(page.getCaretPosition());if(caret!=null)page.scrollRectToVisible(caret.getBounds());}
            else if(line!=null)port.setViewPosition(new java.awt.Point(0,Math.max(0,(int)Math.round(line.getY())+into)));
            readingWidth=port.getWidth();readingHeight=port.getHeight();
        }catch(Exception unavailable){/* next layout */}finally{page.holding(false);restoringReading=false;rememberReading();}});
    }
    private long lastWriting;
    private FileChannel instanceChannel;private FileLock instanceLock;

    public static void main(String[] args) {
        try {
            Path data=Path.of(System.getenv().getOrDefault("LOCALAPPDATA",System.getProperty("user.home")),"Mininotes");
            boolean offline=false;
            for(int i=0;i<args.length;i++) {
                if(args[i].equals("--data-dir")&&i+1<args.length)data=Path.of(args[++i]);
                else if(args[i].equals("--offline"))offline=true;
                else throw new IllegalArgumentException("Unknown launch argument");
            }
            // The language chosen, read before anything is drawn, from beside the notebook, which is not open yet: the face
            // the app is drawn in follows it (see DesktopUi.family), and so do the words of the unlock box.
            String language=Say.resolve(languageChosen(data),java.util.Locale.getDefault());
            System.setProperty("mininotes.language",language);Say.use(language);
            DesktopUi.install();
            // A new install: its language asked first, so README and everything else come in it (decision 232). And any start
            // until a language is confirmed (decision 240), but not one only asking a window already open forward.
            if(!alreadyOpen(data)&&firstLanguage(data)){language=Say.resolve(languageChosen(data),java.util.Locale.getDefault());
                System.setProperty("mininotes.language",language);Say.use(language);DesktopUi.install();}
            Path home=data;boolean local=offline;
            // What the shared code says, kept beside the notebook where whoever helps can ask for it (see Log).
            org.mininotes.desktop.platform.util.Log.to(home.resolve("logs"));
            org.mininotes.desktop.platform.util.Log.i("Mininotes/Desktop","started, version "+VERSION);
            // Already open: that window is asked to come forward, and this one goes - before any password is asked.
            if(alreadyOpen(home)){
                // The request says which version is asking. An older Mininotes running hands over to this newer
                // one - it saves and closes, and this one opens once the notebook is free. The same or an older
                // version only asks the running window forward - and hands it the front this start was given, which
                // Windows otherwise refuses it, flashing the taskbar instead.
                if(!Update.newer(VERSION,runningVersion(home)))DesktopLock.letForward();
                try{Files.createDirectories(home);Files.writeString(home.resolve(SHOW_ME),VERSION);}catch(IOException ignored){/* nothing to ask; it is open */}
                long until=System.currentTimeMillis()+20_000;
                while(Update.newer(VERSION,runningVersion(home))&&alreadyOpen(home)&&System.currentTimeMillis()<until)
                    try{Thread.sleep(300);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
                if(alreadyOpen(home)){System.exit(0);return;}
            }
            holdWhileRunning(home);
            answerUnanswered(home);
            SwingUtilities.invokeLater(()->{
                try {
                    // A locked notebook is asked for before anything of it is opened. Closing the question closes Mininotes.
                    // The question, or with no lock a box saying it is opening, stays up until the window is whole (see opener).
                    boolean open=DesktopLock.locked(home)?DesktopLock.askAtStart(home,true,opener(home,local))!=null:DesktopLock.opening(opener(home,local));
                    if(!open){System.exit(0);return;}
                }
                // Said, and then really gone: a Mininotes that could not open must not linger with no window.
                catch(Exception e){DesktopLock.tell("Mininotes could not open",e.getMessage());System.exit(1);}
            });
        } catch(Exception e){DesktopLock.tell("Mininotes",e.getMessage());System.exit(1);}
    }

    /** Which version holds the notebook: written by it when it opens, read by a second start. */
    static final String RUNNING="running.version";
    static String runningVersion(Path home){try{return Files.readString(home.resolve(RUNNING)).trim();}catch(IOException none){return "";}}

    /** Left by a second start, for the Mininotes already running to come forward - or, if older, to make way. */
    static final String SHOW_ME="show.request";

    /** The window open in this process, if one is: what a second start is handed to. None while the question is asked. */
    private static volatile Desktop current;

    /** Mininotes started again while this window is open: a newer one is made way for, after saving; any other comes forward. */
    private void asked(String asking) {
        if(Update.newer(asking,VERSION)){status.setText(Say.t("A newer Mininotes is opening…"));save(()->shutdown(true));return;}
        // Still opening: the box saying so is what comes forward, never the window before it is whole.
        if(!revealed){if(DesktopLock.front.mayTake())for(Window w:Window.getWindows())if(w.isShowing())w.toFront();return;}
        // Forward only with the front the second start handed on (see main); without it, asking flashed the taskbar.
        boolean let=DesktopLock.front.mayTake();if(!let)DesktopLock.quietly(frame);
        frame.setVisible(true);Node.near(true);frame.setState(Frame.NORMAL);if(let){frame.toFront();frame.requestFocus();}
    }

    /** Mininotes started again while this one runs: its request is answered here, for as long as the process lives. */
    private static void answerUnanswered(Path home) {
        // One watch for the whole process, answering within half a second. Each window used to keep its own, and
        // the one a window made after locking itself again was seen to answer nothing - a newer version then
        // waited, gave up, and could not open. Now the watch hands the request to whichever window is open, which
        // saves before making way; only with no window open at all - the question for the password on the screen,
        // nothing written to lose - does a newer version take over at once.
        Thread watching=new Thread(()->{
            Path asked=home.resolve(SHOW_ME);
            while(true) {
                try {
                    Thread.sleep(500);
                    if(!Files.exists(asked))continue;
                    String asking=Files.readString(asked).trim();
                    Files.deleteIfExists(asked);
                    SwingUtilities.invokeLater(()->{
                        Desktop open=current;
                        if(open!=null&&!open.closing){open.asked(asking);return;}
                        if(Update.newer(asking,VERSION))System.exit(0);
                        // Locked while put away and opened again now: this is the moment to ask.
                        Runnable waiting=askLater;if(waiting!=null){askLater=null;waiting.run();return;}
                        // The box for the password, forward - if Windows lets it; if not, it stays where it is rather than flash.
                        if(DesktopLock.front.mayTake())for(Window w:Window.getWindows())if(w.isShowing()){w.setAutoRequestFocus(true);w.toFront();w.requestFocus();}
                    });
                } catch(InterruptedException stop){return;}
                catch(Exception notNow){/* looked at again in half a second */}
            }
        },"mininotes-second-start");
        watching.setDaemon(true);watching.start();
    }

    /**
     * Held from start to finish of this process, the question for the password included. The notebook's own
     * lock is let go whenever it locks itself again, and a Mininotes waiting at its question held nothing: a
     * second start saw nothing open and asked for the password beside it, and a newer version never had anybody
     * to hand over to. This one is never let go while the process lives.
     */
    private static FileChannel runningChannel;
    private static FileLock runningLock;
    private static void holdWhileRunning(Path home) {
        try{Files.createDirectories(home);runningChannel=FileChannel.open(home.resolve(RUNNING_LOCK),StandardOpenOption.CREATE,StandardOpenOption.WRITE);runningLock=runningChannel.tryLock();if(runningLock!=null)Files.writeString(home.resolve(RUNNING),VERSION);}
        catch(Exception notHeld){/* then only the notebook's lock says it is open, as before */}
    }
    static final String RUNNING_LOCK="running.lock";

    /** Whether another Mininotes has this notebook open, or is waiting at its question: it holds either lock. */
    static boolean alreadyOpen(Path home){return held(home.resolve(RUNNING_LOCK))||held(home.resolve("notebook.lock"));}
    private static boolean held(Path lock) {
        if(!Files.exists(lock))return false;
        try(FileChannel channel=FileChannel.open(lock,StandardOpenOption.WRITE)) {
            FileLock held=channel.tryLock();
            if(held==null)return true;
            held.release();return false;
        } catch(OverlappingFileLockException same){return true;}
        catch(IOException unreadable){return false;}
    }
    /**
     * Whether a notebook opened here is given the two notes everybody has (decision 94): always, but in the tests that open
     * a window on a notebook of their own and count what is in it, which say so (the build's test task, mininotes.welcome).
     */
    static boolean welcomes=!"false".equals(System.getProperty("mininotes.welcome"));
    Desktop(Path folder,boolean offline) throws Exception{this(folder,offline,null);}
    Desktop(Path folder,boolean offline,byte[] key) throws Exception {
        this.offline=offline;context=new Context(folder.toFile());if(offline)connection.setText("Offline");
        context.unlock(key);
        if(key==null&&DesktopLock.locked(folder))throw new IllegalStateException("This notebook is locked. Open Mininotes again to type its password.");
        instanceChannel=FileChannel.open(folder.resolve("notebook.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        try{instanceLock=instanceChannel.tryLock();}catch(OverlappingFileLockException e){instanceLock=null;}
        if(instanceLock==null){instanceChannel.close();throw new IllegalStateException("This pad is already open in another window.");}
        store=new NoteStore(context);keys=new Keys(context);store.filesOut=this::filesChanged;
        DesktopFaces.use(this);
        // The app's words in the language chosen, or this PC's own where there is a list for it (decision 160).
        // A language chosen at the first start, before there were settings to keep it in (decision 232): taken in now.
        String setLanguage=context.getSharedPreferences("settings",0).getString("language",null);
        if(setLanguage==null){setLanguage=languageChosen(folder);context.getSharedPreferences("settings",0).edit().putString("language",setLanguage).apply();}
        // Another language picked in the box at this start, for a notebook in use (decision 240): it, not the one kept here.
        String pickedAtStart=languageAtStart;
        if(pickedAtStart!=null){setLanguage=pickedAtStart;context.getSharedPreferences("settings",0).edit().putString("language",setLanguage).apply();}
        Say.use(Say.resolve(setLanguage,java.util.Locale.getDefault()));
        // Descriptions under the pointer, as My profile has them (decision 205).
        DesktopMenus.describing(context);
        // Kept beside the notebook too, for the next start to draw in the right face before it is unlocked.
        try{Files.writeString(context.getFilesDir().toPath().resolve(LANGUAGE_FILE),context.getSharedPreferences("settings",0).getString("language",Say.SYSTEM),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException|RuntimeException kept){/* only the face waits */}
        // Files that arrive with a shared note are sealed with the key this window holds, as any file kept here is.
        NoteStore.fileKey=context::databaseKey;
        // A file fetched, or somebody saying they have one: the open note's cards and its line drawn again.
        Post.filesMoved=note->SwingUtilities.invokeLater(()->{if(base!=null&&base.id.equals(note)){fileCards.show(note);updateStanding();}});
        // Files sent or received on their own moved on - fetched, answered: said, and the list drawn again (see DesktopDrops).
        Post.news=landed->SwingUtilities.invokeLater(()->{if(!closing)DesktopDrops.heard(this,landed);});
        // Not while a private code is half typed at the caret (decision 111): the writing down waits for the next key.
        autosave=new javax.swing.Timer(350,e->{if(codeBeingTyped()){((javax.swing.Timer)e.getSource()).restart();return;}save(null);});autosave.setRepeats(false);
        syncLater=new javax.swing.Timer(1000,e->sendDue());syncLater.start();
        Toolkit.getDefaultToolkit().addAWTEventListener(using,AWTEvent.KEY_EVENT_MASK|AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        idle=new javax.swing.Timer(20_000,e->relockIfIdle());idle.start();
        // Mininotes started again while this one runs: it asked this window to come forward.
        try{Files.writeString(folder.resolve(RUNNING),VERSION);}catch(IOException ignored){/* an older one simply is not asked to make way */}
        current=this;
        privately=new DesktopPrivate(this);
        // Never written out: what is in a private space open now, and anything while the notebook locks (decision 162).
        onDisk=new DesktopOnDisk(store,id->privately.holds(id),()->closing||relocking);
        // The programs picked here for a kind of file, where Windows' own choice is not wanted (see DesktopApps).
        DesktopApps.load(context);
        // A file linked to one on this PC says so to the owner's other devices (see NoteStore.linkedHere).
        DesktopOnDisk linking=onDisk;
        NoteStore.linkedHere=id->{try{return linking.linkedFile(id)?Node.nameHere(context):"";}catch(RuntimeException closed){return "";}};
        linkedHook=NoteStore.linkedHere;
        build();
        // A code typed in a note's page or title, or in the search, is caught there before anything can keep it (decision 111).
        privately.watch(page,()->!drawing&&base!=null,()->((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits());
        privately.watch(title,()->!drawing&&base!=null,()->{});
        privately.watch(home.search,()->true,()->{});
        privately.start(folder);
        // Files dropped from Explorer: on a note, attached to it; on a collection, kept with it; on Home, kept there (see DesktopDrops.aim).
        DesktopDrops.takeDrops(this);
        disk.submit(()->{
            var settings=context.getSharedPreferences("settings",0);
            listenInTray="true".equals(settings.getString("listenInTray","false"));
            treeWanted="true".equals(settings.getString(TREE,"false"));
            try{int w=Integer.parseInt(settings.getString("sideWidth","280"));if(w>=MIN_SIDE&&w<=900)sideWidth=w;}catch(NumberFormatException none){/* the usual width */}
            try{int w=Integer.parseInt(settings.getString("openWidth",Integer.toString(DesktopOpenList.WIDE)));if(w>=MIN_OPEN&&w<=900)openWidth=w;}catch(NumberFormatException none){/* the usual width */}
            openFolded="true".equals(settings.getString("openFolded","false"));
            step="reading your notebook";store.getWritableDatabase();
            store.usually=syncAfter();
            // Shared with me a place, out of the collection 0.2.026 made for it, and the two notes everybody has, once a
            // device (decision 94): before what to open is chosen, so a fresh notebook opens on Home with the two on it.
            store.sharedWithMeBecomesAPlace();
            boolean welcomed=welcomes&&store.welcome(VERSION);
            // Read me said again where it still says what an earlier build wrote, as the phone does: since 0.3.011 its codes
            // are taught with the word and //: apart, so the guard at the door leaves them whole (decision 114).
            // And in the language chosen since the last start, where it was changed (decision 232): Settings restarts the PC
            // for it, so the change is told here, by the language README was last said in.
            // A language picked in the box at this start (decision 240) is a change too, even where README's language was
            // never noted (a notebook from before 232).
            boolean boxChanged=languageAtStart!=null;languageAtStart=null;
            if(welcomes){String readIn=context.getSharedPreferences("settings",0).getString("readMeLanguage","");
                welcomed|=store.readMeAgain(boxChanged||!readIn.isEmpty()&&!readIn.equals(Say.language()));
                context.getSharedPreferences("settings",0).edit().putString("readMeLanguage",Say.language()).apply();}
            // Photos on Home, an album, once on this PC, under the id all the owner's devices give it (decision 217).
            if(welcomes)welcomed|=store.photosOnce()!=null;
            // A locked notebook seals any attachment still plain: one kept before the lock covered attachments.
            byte[] opened=context.databaseKey();if(opened!=null){step="sealing attachments";DesktopFiles.every(store,opened,true);}
            step="reading your last note";
            // What was open: the overview, or - the first time a 0.2 build opens - the tabs 0.1 kept, read once into it.
            String kept=settings.getString(DesktopOverview.KEPT,null),where=settings.getString(WHERE,"");
            Overview open=kept!=null?Overview.read(kept):DesktopOverview.fromTabs(settings.getString("tabs",""));
            if(kept==null)settings.edit().putString(DesktopOverview.KEPT,open.said()).putString("tabs","").apply();
            // Where the window was: Home, or a note - the one in front of the old tabs, the first time - else the last written.
            String noteId=where.startsWith(NOTE_SHOWN)?where.substring(NOTE_SHOWN.length())
                :where.isEmpty()&&!open.isEmpty()&&open.all().get(0).kind==Overview.Kind.NOTE?open.all().get(0).id:null;
            NoteStore.Note note=noteId!=null&&store.stillThere(NoteStore.Branch.Kind.PAGE,noteId)?store.get(noteId):null;
            if(note==null&&!HOME_SHOWN.equals(where)) {
                note=store.latest();
                if(note==null){note=new NoteStore.Note();note.book=store.someBook();store.save(note);}
                // Opened for the first time, the notes everybody starts with made just now: on Home, where they are to be
                // seen, not inside one of them (the owner, 2026-10-05: "a new downloaded windows app should open on the
                // desktop not inside the My first note"). Any other notebook opens on its last note, as it did.
                else if(where.isEmpty()&&welcomed)note=null;
            }
            return new Object[]{note,open};
        },opened->{
            NoteStore.Note note=(NoteStore.Note)opened[0];overview.use((Overview)opened[1]);
            step="drawing your notes";
            // Stamps still waiting for Minima when the PC last closed, looked for again (decision 218).
            DesktopStamp.resume(this);
            usual=DesktopLook.usual(context);showSize(DesktopLook.rung(context));whoWrote=DesktopLook.whoWrote(context);
            startNote=note==null?null:note.id;
            if(note!=null)display(note);else showHome();
            refresh();status.setText(" ");if(!treeWanted)showTree(false);else split.setDividerLocation(sideWidth);if(!offline){startNode();lookForUpdate(false);}
            if(!closing)remindBackup();
            settle(0);
            // The notes kept as files looked at, after what opening asked of the notebook, and their folders watched.
            if(!closing)onDisk.start(this);
            // Who asked to be added while this PC was closed, asked now (decision 163).
            if(!offline&&!closing)askInvites();
            pinnedMenu.place();
            // Whether this PC has Windows Hello, asked now so Security can show it the moment it opens.
            connectivity.submit(DesktopHello::supported,yes->{},e->{});},this::openFailed);
    }
    /**
     * The window comes up with the notes already in it, in one step. Shown at once, it was an empty page and an
     * empty tree for the moment the notebook took to read; shown after two seconds whatever it held, a slow unlock
     * still gave that empty window. Now it waits for as long as it takes, behind the box that opened it, which says
     * so (see opener): never an empty window.
     */
    void show(){wanted=true;if(ready)reveal();}
    private boolean wanted,ready,revealed;
    /** What opening is doing, said by the box if it takes long. */
    private volatile String step="opening";
    /** Told once the window is up whole, and why it could not open: the box that waits for it (see opener). */
    private Runnable whenShown;private java.util.function.Consumer<Exception> whenFailed;
    // Never once it is closing: a window let go of, or locked again, is not brought back by a late answer.
    private void reveal(){if(revealed||!wanted||closing)return;revealed=true;frame.setVisible(true);Node.near(true);if(onPage)page.requestFocusInWindow();else home.focusFirst();
        Runnable told=whenShown;whenShown=null;whenFailed=null;if(told!=null)told.run();
        // Back from a restart for the size: My profile at Text size, where the size was chosen (decision 219).
        if(DesktopScale.reopening())SwingUtilities.invokeLater(()->DesktopProfile.open(this,"Text size"));}
    /** The first page and the tree are drawn: the window may come up. */
    private void drawn(){ready=true;paintApp();if(wanted)reveal();}
    /**
     * Waits for the disk to have answered all the first drawing asked: the tree, and the note in front with its
     * words, files, writers' colours and people. Each is asked when the one before it answers, so the disk is asked
     * again - a few rounds - until all are in. The disk does one thing at a time, so each round answers after them.
     */
    private void settle(int round){disk.submit(()->null,none->SwingUtilities.invokeLater(()->{if(whole()||round>=8)drawn();else settle(round+1);}),e->drawn());}
    private boolean treeDrawn;private String stoodFor;
    private boolean whole(){return treeDrawn&&home.drawn()&&(startNote==null||base!=null&&base.id.equals(startNote)&&base.id.equals(stoodFor));}
    /** Opening failed before the window came up: said by the box that waits for it, and nothing of it left open. */
    private void openFailed(Exception e){java.util.function.Consumer<Exception> told=whenFailed;if(revealed||told==null){failed(e);return;}whenShown=null;whenFailed=null;told.accept(e);}
    /**
     * How a box opens the notebook with its key (see DesktopLock.Opener): the window is made and fills itself unseen,
     * and comes up whole as the box goes. Slower than ten seconds, the box says what it is still doing; failed, why.
     */
    static DesktopLock.Opener opener(Path folder,boolean offline) {
        return (key,shown,said,failed)->{
            Desktop pad;
            try{pad=new Desktop(folder,offline,key);}catch(Exception e){failed.accept(couldNot(e));return;}
            // Opened again by an update, after the old one closed, the window has no claim to the front: it comes up quietly.
            DesktopLock.quietUnlessLet(pad.frame);
            pad.whenShown=shown;pad.whenFailed=e->pad.shutdown(false,()->failed.accept(couldNot(e)));
            javax.swing.Timer slow=new javax.swing.Timer(10_000,t->{if(!pad.revealed&&!pad.closing)said.accept("Still "+pad.step+"… This is taking longer than usual.");});
            slow.setRepeats(false);slow.start();
            pad.show();
        };
    }
    private static String couldNot(Exception e){return "Your notebook could not be opened. "+(e.getMessage()!=null?e.getMessage():e.getClass().getSimpleName());}
    private void build() {
        DesktopUi.install();
        Font body=DesktopUi.BODY;
        frame.setIconImages(List.of(DesktopIcon.image(16),DesktopIcon.image(32),DesktopIcon.image(48),DesktopIcon.image(256)));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(820,520));frame.setSize(1120,760);frame.setLocationRelativeTo(null);
        // In front again: what waits for an answer and was not said yet is asked now (decision 109).
        frame.addWindowListener(new WindowAdapter(){public void windowActivated(WindowEvent e){askWaiting();catchUp();}});
        // Put away, or another window in front: the open note's words go now (see sendOnLeaving).
        frame.addWindowListener(new WindowAdapter(){public void windowDeactivated(WindowEvent e){if(onPage&&base!=null)sendOnLeaving(base.id);}});
        frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){privately.left();save(()->{if(listenInTray&&ensureTray()){frame.setVisible(false);Node.near(false);}else shutdown(true);});}});
        // Minimised, or hidden: whatever is private closes, as it does when the phone is put away (decision 111).
        frame.addWindowStateListener(e->{if((e.getNewState()&Frame.ICONIFIED)!=0)privately.left();});
        frame.addComponentListener(new ComponentAdapter(){@Override public void componentHidden(ComponentEvent e){if(privately.showing())privately.left();}});
        JPanel shell=new JPanel(new BorderLayout());shell.setBackground(PAPER);

        // The bar: what this is on the left; on the right, whether it is connected, then what can be done, the menu
        // last. Nothing in it is filled: making a note is Home's +, where the thing made appears.
        // In the window's own title bar, not a row of its own under it (the owner, 2026-10-17: "could we avoid the empty
        // header, bring up the top of the note and home screen"): a menu bar, which the look draws inside the title bar,
        // the title where its glue is and what can be done at the right, before the window's own buttons. Where the system
        // draws the title bar itself, it is one thin row under it.
        JMenuBar bar=new JMenuBar();bar.setBackground(DesktopUi.SHELF);barPanel=bar;
        bar.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
        JLabel brand=new JLabel("Mininotes",new ImageIcon(DesktopIcon.image(28)),SwingConstants.LEFT);brand.setIconTextGap(10);brand.setFont(body.deriveFont(Font.BOLD,18f));brand.setForeground(INK);
        // The logo and the name are the way Home, as a website's are (the owner, 2026-10-06).
        brand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));brand.setToolTipText(Say.t("Home, then your Profile"));brand.getAccessibleContext().setAccessibleName(Say.t("Mininotes: Home, then your Profile"));
        // On Home already, it is the way to your Profile, where everything about you is set (the owner, 2026-10-17: "clicking the
        // Mininotes icon should bring back to the main page and when clicked from the main page it should bring to the My
        // profile page").
        brand.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e))return;
            if(!onPage&&home!=null&&!home.folder.isOpen())profile();else goHome();}});
        // The version is in the menu, as on the phone, and not here (the owner, 2026-10-14: "let's put the version in the menu,
        // instead of on top of the app, and we will have an update notification at the top like in VS Code"): what is at the
        // top is Update, at the right beside Share, and only while there is one.
        // Not shown: the window's own title says Mininotes, and the bar is for what can be done (the owner, 2026-10-17: "let's
        // remove the Mininotes and icon at the very top"). Profile is in ⋯'s Sync and sharing.
        updateButton.setFocusPainted(false);updateButton.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,12.5f));updateButton.setMargin(new Insets(3,12,3,12));
        updateButton.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"arc:8;foreground:#FFFFFF;background:#306348;hoverBackground:#2A5840;pressedBackground:#224A35;borderWidth:0;focusWidth:0");
        updateButton.addActionListener(e->{if(staged!=null)restartToUpdate();else updatesBox(null);});
        version.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));version.setIconTextGap(6);
        version.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){lookForUpdate(true);}});
        updateShown();

        connection.setForeground(QUIET);connection.setFont(body.deriveFont(13f));connection.setIconTextGap(6);
        connection.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));connection.setToolTipText(Say.t("Connection details are in Profile"));
        connection.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){profile();}});
        // Said, not three dots (the owner, 2026-10-17: "the menu would also be better presented than 3 dots").
        JButton menu=new JButton(Say.t("Menu"),lines());menuButton=menu;menu.setIconTextGap(8);menu.setFont(body.deriveFont(Font.BOLD,13f));menu.setForeground(INK);
        menu.setFocusPainted(false);menu.setMargin(new Insets(4,10,4,12));menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menu.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        menu.setToolTipText(Say.t("Menu"));menu.getAccessibleContext().setAccessibleName(Say.t("Menu"));menu.addActionListener(e->menu(menu));
        security.setFont(body.deriveFont(13f));security.setIconTextGap(6);security.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        security.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){DesktopLock.settings(Desktop.this);}});
        securityShown();
        // The menu last, at the far right, where a window keeps its menu. What Share ▾ dropped down to is the top section
        // of ⋯ now, Sync and sharing (the owner, 2026-10-17: "let's remove this dropdown Share menu and integrate it as the
        // top section of the main menu"; decision 185).
        newButton.setFocusPainted(false);newButton.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,12.5f));newButton.setMargin(new Insets(3,12,3,12));
        newButton.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"arc:999;foreground:#FFFFFF;background:#306348;hoverBackground:#2A5840;pressedBackground:#224A35;borderWidth:0;focusWidth:0");
        newButton.setVisible(false);newButton.addActionListener(e->whatIsNew());
        // One row that never wraps: a box's footer wraps by the width it had, and when "new" or Update came the bar
        // broke onto two rows and stayed so.
        // As wide as what is in it: in the bar's row, a panel left to grow would take the title's room.
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,DesktopUi.S,0)){@Override public Dimension getMaximumSize(){return getPreferredSize();}};
        actions.setOpaque(false);actions.setAlignmentY(Component.CENTER_ALIGNMENT);
        // The Agent where the menu was, the menu over Recent (the owner, 2026-10-18: "I dont like the Menu in the top bar,
        // please bring it in the app over Recents. Put the agent visible at the top bar where the menu is currently"). The
        // menu comes back up here only while Recent is not shown, so it is never out of reach (see placeMenu).
        JButton agent=new JButton(Say.t("Agent"),glyph("bot-message-square",17));agent.setIconTextGap(7);agent.setFont(body.deriveFont(Font.BOLD,13f));agent.setForeground(INK);
        agent.setFocusPainted(false);agent.setMargin(new Insets(4,10,4,12));agent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agent.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        agent.setToolTipText(Say.t("Agent")+" (Ctrl+K)");agent.getAccessibleContext().setAccessibleName(Say.t("Agent"));agent.addActionListener(e->DesktopAssistant.open(this));
        for(JComponent one:new JComponent[]{newButton,updateButton,security,connection,agent})actions.add(one);
        barActions=actions;
        // The window's name on the left, beside the logo, and not in the middle of the bar (the owner, 2026-10-19: "align the
        // Mininotes on the left beside the logo"; decision 210): the frame's own title is hidden, and this one follows it.
        JLabel named=new JLabel(frame.getTitle());named.setFont(body.deriveFont(13f));named.setForeground(QUIET);named.setBorder(BorderFactory.createEmptyBorder(0,4,0,12));
        named.setAlignmentY(Component.CENTER_ALIGNMENT);
        frame.addPropertyChangeListener("title",e->named.setText(frame.getTitle()));
        frame.getRootPane().putClientProperty(com.formdev.flatlaf.FlatClientProperties.TITLE_BAR_SHOW_TITLE,false);
        bar.add(named);
        bar.add(Box.createHorizontalGlue());bar.add(actions);
        frame.setJMenuBar(bar);
        placeMenu();
        // Under the bar, a contact in trouble, said while there is one (decision 188).
        troubles=new DesktopTroubles(this);
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(troubles,BorderLayout.SOUTH);
        shell.add(top,BorderLayout.NORTH);

        // The tree, an optional panel beside Home and the page (decision 26): everything, on a shade of its own.
        JPanel side=new JPanel(new BorderLayout(0,10));side.setBackground(DesktopUi.SHELF);sidePanel=side;side.setBorder(BorderFactory.createEmptyBorder(12,12,12,8));
        // The top of the tree is not drawn: its first line is Home, holding everything, and Favourites inside it.
        tree.setRootVisible(false);tree.setShowsRootHandles(true);tree.setRowHeight(30);tree.setBackground(DesktopUi.SHELF);tree.setBorder(BorderFactory.createEmptyBorder(2,0,4,0));
        tree.setCellRenderer(new DefaultTreeCellRenderer(){
            // The wash is drawn here as a rounded box with room above and below, so two coloured rows one under the
            // other read as two things; filled edge to edge by the renderer, they ran into one block.
            private Color wash;
            /** Its sync mark, at the end of the line, as the phone's tree wears it; null for the top and Favourites. */
            private SyncMark mark;
            @Override public void paint(Graphics g0) {
                if(wash!=null&&!selected){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(wash);g.fillRoundRect(0,3,getWidth(),getHeight()-6,8,8);g.dispose();}
                super.paint(g0);
                if(mark!=null)new DesktopMark(mark,TREE_MARK,true).paintIcon(this,g0,getWidth()-TREE_MARK-4,(getHeight()-TREE_MARK)/2);
            }
            @Override public Dimension getPreferredSize() {
                Dimension d=super.getPreferredSize();
                return mark==null||d==null?d:new Dimension(d.width+TREE_MARK+12,Math.max(d.height,TREE_MARK));
            }
            public Component getTreeCellRendererComponent(JTree t,Object v,boolean s,boolean ex,boolean leaf,int row,boolean focus) {
                Object said=v instanceof DefaultMutableTreeNode node?node.getUserObject():v;
                setTextNonSelectionColor(INK);
                super.getTreeCellRendererComponent(t,v,s,ex,leaf,row,focus);setIcon(null);setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                boolean page=said instanceof Item item&&item.branch.kind==NoteStore.Branch.Kind.PAGE;
                setFont(page?DesktopUi.BODY:DesktopUi.BODY.deriveFont(Font.BOLD));
                // Before its name, its face, small: its icon or picture in its colour, as its tile on Home wears it.
                int colour=said instanceof Item item?item.branch.colour:Tint.NONE;
                if(said instanceof Item item){NoteStore.Branch line=item.branch;setIcon(DesktopHome.faceIcon(()->line,TREE_FACE,()->usual));setIconTextGap(8);}
                // And its row washed in it, at its own strength (decision 107), as the phone's tree view does.
                wash=said instanceof Item item&&Tint.known(colour)?DesktopLook.wash(colour,DesktopUi.SHELF,0.16f,0.82f,toneOf(item.branch)):null;
                mark=said instanceof Item item&&DesktopMoving.movable(item.branch)?marks.get(item.branch.id):null;
                setBackgroundNonSelectionColor(new Color(0,0,0,0));return this;
            }
        });
        // A line chosen opens what it is: a note fills the window, a collection opens as its card over Home, Home is Home.
        tree.addTreeSelectionListener(e->{if(drawing)return;Object value=((DefaultMutableTreeNode)tree.getLastSelectedPathComponent());
            if(value instanceof DefaultMutableTreeNode node&&node.getUserObject() instanceof Item item){
                if(item.branch.kind==NoteStore.Branch.Kind.FAVOURITES)return;
                NoteStore.Branch real=item.branch.kind==NoteStore.Branch.Kind.PAGE?item.branch:find(item.branch.id)!=null?find(item.branch.id):item.branch;
                if(real.kind==NoteStore.Branch.Kind.PAGE)save(()->open(real.id));else openCollection(real.id);
                selected=real;
            }else if(value instanceof DefaultMutableTreeNode node&&allCollections(node)){selected=null;save(()->{showHome();home.folder.close();});}});
        JScrollPane shelves=DesktopUi.scrolling(tree);side.add(shelves);
        // Every row opens something, so the hand over a row; the arrow over the empty paper below the last one.
        tree.addMouseMotionListener(new MouseMotionAdapter(){public void mouseMoved(MouseEvent e){
            int row=tree.getClosestRowForLocation(e.getX(),e.getY());Rectangle at=row<0?null:tree.getRowBounds(row);
            boolean onRow=at!=null&&e.getY()>=at.y&&e.getY()<at.y+at.height;
            tree.setCursor(Cursor.getPredefinedCursor(onRow?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR));}});
        // Everything in the tree can be renamed and coloured where it stands: right-click it, or F2 to rename.
        tree.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){if(e.isPopupTrigger())shelfMenu(e);}
            public void mouseReleased(MouseEvent e){if(e.isPopupTrigger()&&!moving.carrying())shelfMenu(e);}
            // The mark at the end of a line is a button, as on the phone: waiting sends, anything else shows who has it.
            public void mouseClicked(MouseEvent e) {
                if(!SwingUtilities.isLeftMouseButton(e)||moving.moved())return;
                int row=tree.getRowForLocation(e.getX(),e.getY());if(row<0)return;
                Rectangle at=tree.getRowBounds(row);if(e.getX()<at.x+at.width-TREE_MARK-8)return;
                if(((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).getUserObject() instanceof Item item&&DesktopMoving.movable(item.branch)) {
                    SyncMark what=marks.get(item.branch.id);if(what!=null)markClicked(find(item.branch.id)!=null?find(item.branch.id):item.branch,what);
                }
            }
        });
        // And taken hold of and dragged: among its own kind to a new place, onto a collection to go inside.
        moving.watch(tree);
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("F2"),"rename");
        tree.getActionMap().put("rename",new AbstractAction(){public void actionPerformed(ActionEvent e){if(selected!=null)rename(selected);}});

        // The page: ← Home and where it lives on one quiet line, its title, the line for whether the others have it, then paper.
        JPanel editor=new JPanel(new BorderLayout());editor.setBackground(PAPER);editorPanel=editor;
        // The note's head, as the owner chose it (2026-10-10, design C): on a band a shade darker than the paper, the way back
        // and who the note reaches; under them its tabs, as index cards, the open one the paper itself; then on the paper
        // its title, with where it lives and what its writing is doing on one quiet line.
        JPanel heading=new JPanel();heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS));heading.setOpaque(false);
        JPanel bandTop=new JPanel(new BorderLayout(10,0)){@Override protected void paintComponent(Graphics g){g.setColor(band());g.fillRect(0,0,getWidth(),getHeight());}};
        bandTop.setOpaque(false);bandTop.setBorder(BorderFactory.createEmptyBorder(6,22,2,30));
        JPanel bandTabs=new JPanel(new BorderLayout()){@Override protected void paintComponent(Graphics g){g.setColor(band());g.fillRect(0,0,getWidth(),getHeight());}};
        bandTabs.setOpaque(false);bandTabs.setBorder(BorderFactory.createEmptyBorder(2,30,0,30));bandTabs.add(tabStrip,BorderLayout.WEST);tabStrip.setOpaque(false);
        tabsBand=bandTabs;
        // The title above the tabs, on the same band (the owner, 2026-10-10: "the title of the note should be above the tabs").
        JPanel onPaper=new JPanel(new BorderLayout(0,2)){@Override protected void paintComponent(Graphics g){g.setColor(band());g.fillRect(0,0,getWidth(),getHeight());}};
        onPaper.setOpaque(false);onPaper.setBorder(BorderFactory.createEmptyBorder(0,30,4,38));
        // On the band now, so it wears the band, not a box of paper of its own.
        title.setFont(body.deriveFont(Font.BOLD,20f));title.setBorder(BorderFactory.createEmptyBorder());title.setOpaque(false);title.setBackground(new Color(0,0,0,0));title.setForeground(INK);title.setToolTipText(Say.t("Note title (optional)"));
        title.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,Say.t("Untitled"));
        title.setEditable(false);title.getAccessibleContext().setAccessibleName("Note title");heading.add(title);standing.setForeground(QUIET);standing.setFont(body.deriveFont(12.5f));
        // Where the note lives, small, above its title; under it, where it stands, in marks and no words: the sync
        // mark, then a round for each person it reaches, tinted by where they stand. The words were "Shared with
        // Ana · not sent yet – send now" beside a green mark, and the colour said the opposite of the words. The
        // mark is a click - waiting sends now, anything else opens who has it - and so is each round, which says
        // who and where in a small box. Nothing shows under the pointer; Settings says what each mark means.
        // At the left of that line, ← Home, as the phone's note page has its arrow back (decision 24); Esc does the same.
        JButton back=new JButton(Say.t("Home"),backIcon());back.setIconTextGap(6);
        back.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        back.setFont(body.deriveFont(13f));back.setForeground(INK);back.setFocusPainted(false);back.setMargin(new Insets(4,6,4,8));
        back.setToolTipText(Say.t("Back to Home (Esc)"));back.getAccessibleContext().setAccessibleName(Say.t("Back to Home"));back.addActionListener(e->goHome());
        backButton=back;back.setFont(body.deriveFont(Font.BOLD,14f));
        // One bar (the owner, 2026-10-11: "the header on our notes now takes too much space"): the way back, the note's
        // face and title, who it reaches; under it one quiet line; the tabs only on a note that has some.
        JPanel where=new JPanel(new BorderLayout(10,0));where.setOpaque(false);where.add(back,BorderLayout.WEST);
        bandTop.add(where);
        JPanel titled=new JPanel(new BorderLayout(8,0));titled.setOpaque(false);
        heading.remove(title);titled.add(title);where.add(titled,BorderLayout.CENTER);
        // Before the title, the note's icon, as its tile on Home wears it (docs/HOME.md, step 4): a click chooses another.
        noteFace.setIcon(DesktopHome.faceIcon(()->base==null?null:noteLook(),30,()->usual));
        noteFace.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        noteFace.setMargin(new Insets(3,3,3,3));noteFace.setFocusPainted(false);noteFace.setToolTipText(Say.t("Change its icon"));
        noteFace.getAccessibleContext().setAccessibleName("The note's icon: choose another");
        noteFace.addActionListener(e->{if(base!=null)DesktopIconPicker.open(this,openNote());});
        JPanel faceHolder=new JPanel(new GridBagLayout());faceHolder.setOpaque(false);faceHolder.add(noteFace);titled.add(faceHolder,BorderLayout.WEST);
        // As tall when it says nothing as when it says "Saving…": the row coming and going at every pause in the typing
        // moved the whole page up and down under the writer, and took the line being written out of view (the owner,
        // 2026-10-10: "the whole rest of the text moves"). Only while the words are on it; compact puts them elsewhere.
        JPanel details=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        details.setOpaque(false);
        syncMark.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));syncMark.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));syncMark.setVisible(false);
        syncMark.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(base!=null&&noteMark!=null)markClicked(openNote(),noteMark);}});
        rounds.setOpaque(false);
        // Then what its syncing is doing, in words: beside the mark it is about, not on the bar at the foot, where
        // the eye that went up to the mark did not look. A click does what the mark does, or shows why.
        noteWords.setFont(body.deriveFont(13f));noteWords.setBorder(BorderFactory.createEmptyBorder(0,6,0,0));
        noteWords.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));noteWords.setVisible(false);wordsAway.setRepeats(false);
        noteWords.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            if(wordsClicked!=null)wordsClicked.run();else if(base!=null&&noteMark!=null)markClicked(openNote(),noteMark);}});
        details.add(syncMark);details.add(rounds);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));right.setOpaque(false);right.add(details);
        // Where it lives, then what its syncing is doing: one quiet line under the title, as tall saying nothing as saying
        // "Saving…", so the page never moves under the writer.
        JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)){
            @Override public Dimension getPreferredSize(){Dimension size=super.getPreferredSize();size.height=Math.max(size.height,getFontMetrics(noteWords.getFont()).getHeight()+2);return size;}
        };
        line.setOpaque(false);line.setBorder(BorderFactory.createEmptyBorder(0,2,0,0));line.add(standing);line.add(noteWords);pathLine=line;
        // A note with no tabs shows no strip: they begin here, quietly, at the end of its line.
        addTabLink.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        addTabLink.setFont(body.deriveFont(12.5f));addTabLink.setForeground(QUIET);addTabLink.setFocusPainted(false);addTabLink.setMargin(new Insets(0,10,0,4));
        addTabLink.setToolTipText(Say.t("Add a tab to this note"));addTabLink.addActionListener(e->addTab());line.add(addTabLink);
        onPaper.add(line,BorderLayout.SOUTH);
        // Under the title, a stamped note's line (decision 218): there only on a note stamped on Minima.
        onPaper.add(stampLine,BorderLayout.NORTH);
        bandTop.add(right,BorderLayout.EAST);
        // First, over all of it, the line that says the note's file is lost, while it is (decision 162).
        for(JComponent part:new JComponent[]{lostStrip,bandTop,onPaper,bandTabs}){part.setAlignmentX(Component.LEFT_ALIGNMENT);heading.add(part);}
        editor.add(heading,BorderLayout.NORTH);
        JButton tools=button("Note tools",this::clipMenu);tools.setName("compactNoteTools");tools.setVisible(false);right.add(tools);
        page.setFont(body.deriveFont(18f));page.setForeground(INK);page.setBackground(PAPER);page.setLineWrap(true);page.setWrapStyleWord(true);page.setBorder(BorderFactory.createEmptyBorder(12,42,40,38));
        // Ctrl+B, Ctrl+I and Ctrl+U on the page: bold, italic, underline, as marks in the words (decision 76). On the page they
        // come before the window's Ctrl+B, which shows the tree everywhere else.
        for(String[] key:new String[][]{{"control B","BOLD"},{"control I","ITALIC"},{"control U","UNDERLINE"}}) {
            page.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key[0]),"style-"+key[1]);
            Marks.Style style=Marks.Style.valueOf(key[1]);
            page.getActionMap().put("style-"+key[1],new AbstractAction(){public void actionPerformed(ActionEvent e){page.style(style);}});
        }
        page.setEditable(false);page.getAccessibleContext().setAccessibleName("Note text");page.setTabSize(4);page.cannotOpen=this::failed;
        JScrollPane paper=DesktopUi.scrolling(page);paperScroll=paper;
        paper.getViewport().addChangeListener(e->rememberReading());
        paper.getViewport().addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){restoreReading();}});
        // The paperclip sits on the page itself, in its lower right corner, with how many files the note holds:
        // a band of its own at the foot took a strip of the window from the writing for one small button.
        clip.setToolTipText(Say.t("Attachments"));clip.getAccessibleContext().setAccessibleName("Attachments");
        clip.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        clip.setFont(body.deriveFont(13f));clip.setForeground(QUIET);clip.setIconTextGap(4);clip.addActionListener(e->clipMenu());
        // The microphone beside it: recording is attaching something made here and now. While it records, the
        // bar takes the microphone's place, so there is one Stop and nothing to start a second time.
        mic.setToolTipText(Say.t("Record audio"));mic.getAccessibleContext().setAccessibleName("Record audio");
        mic.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        mic.addActionListener(e->record());
        camera.setToolTipText(Say.t("Take a photo"));camera.getAccessibleContext().setAccessibleName(Say.t("Take a photo"));
        camera.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        camera.addActionListener(e->{if(base!=null&&page.isEditable())save(()->DesktopCamera.take(this,base.id));else status.setText(Say.t("This note is read only here"));});
        recorder=new DesktopRecorder(this);
        JPanel foot=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));foot.setOpaque(false);foot.setBorder(BorderFactory.createEmptyBorder(0,16,6,22));foot.add(recorder);foot.add(mic);foot.add(camera);foot.add(clip);
        // The files themselves, folded into the paperclip: the pointer on it slides them up over the foot of the
        // page, and they fold again a moment after it leaves both.
        fileCards=new DesktopFileCards(this);
        JPanel drawer=new JPanel(new BorderLayout());drawer.setOpaque(false);
        drawer.add(fileCards,BorderLayout.CENTER);drawer.add(foot,BorderLayout.SOUTH);
        JPanel sheet=new JPanel(null){
            @Override public boolean isOptimizedDrawingEnabled(){return false;}
            @Override public void doLayout() {
                int w=getWidth(),h=getHeight();paper.setBounds(0,0,w,h);
                int bar=paper.getVerticalScrollBar().isVisible()?paper.getVerticalScrollBar().getWidth():0;
                if(findBar.isVisible()){Dimension f=findBar.getPreferredSize();findBar.setBounds(Math.max(0,w-bar-f.width-16),8,Math.min(f.width,w),f.height);}
                Dimension d=drawer.getPreferredSize();drawer.setBounds(0,h-d.height,w-bar,d.height);
            }
            @Override public Dimension getPreferredSize(){return paper.getPreferredSize();}
        };
        findBar=new DesktopFindBar(page);
        sheet.setOpaque(false);sheet.add(findBar);sheet.add(drawer);sheet.add(paper);editor.add(sheet);
        fitNote=()->{
            boolean compact=editor.getHeight()>0&&editor.getHeight()<480;
            if(compactNote!=compact){rememberReading();compactNote=compact;
                // Short: the tabs, the title's line and the people make way; what the writing is doing goes on the bar.
                if(compact){line.remove(noteWords);right.add(noteWords,0);details.setVisible(false);}
                else{right.remove(noteWords);line.add(noteWords,1);details.setVisible(true);}
                line.setVisible(!compact);drawTabs();
                tools.setVisible(compact);
                heading.revalidate();restoreReading();
            }
            drawer.setVisible(!compact||recorder.recording());mic.setVisible(!compact&&!recorder.recording());camera.setVisible(!compact&&!recorder.recording());clip.setVisible(!compact);
            if(compact&&!recorder.recording())fileCards.open(false);sheet.revalidate();sheet.repaint();
        };
        editor.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){fitNote.run();}});
        javax.swing.Timer fold=new javax.swing.Timer(250,null);
        long[] away={0};
        fold.addActionListener(e->{
            java.awt.Point at=MouseInfo.getPointerInfo()==null?null:MouseInfo.getPointerInfo().getLocation();
            boolean over=false;
            if(at!=null&&drawer.isShowing()){for(Component c:new Component[]{clip,fileCards})if(c.isShowing()){java.awt.Point o=c.getLocationOnScreen();if(new Rectangle(o,c.getSize()).contains(at))over=true;}}
            if(over){away[0]=0;return;}
            if(away[0]==0){away[0]=System.currentTimeMillis();return;}
            if(System.currentTimeMillis()-away[0]>500){fileCards.open(false);sheet.revalidate();sheet.repaint();fold.stop();away[0]=0;}
        });
        clip.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){if(fileCards.isOpen())return;fileCards.open(true);sheet.revalidate();sheet.repaint();away[0]=0;fold.start();}});
        // The main area is Home, or the open note filling the window (decision 2).
        home=new DesktopHome(this);
        mainArea.add(home,"home");mainArea.add(editor,"page");
        // Where the note lives, clicked: that collection's card over Home, or Home.
        standing.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));standing.setToolTipText(Say.t("Open the folder it is in"));
        standing.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(base!=null){
            if(NoteStore.home(base.book))goHome();else openCollection(base.book);}}});
        this.side=side;
        JPanel pageSide=new JPanel(new BorderLayout());pageSide.setBackground(PAPER);
        // And beside it, what is open (decision 77): hidden with nothing open, or switched off. The line before it can be
        // pulled, and its handle folds it to the edge and opens it again, as the tree's does (decision 83).
        openSplit=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,mainArea,openList);
        openSplit.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI(){
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider(){return new Grip(this,true);}
        });
        openSplit.setBorder(BorderFactory.createEmptyBorder());openSplit.setContinuousLayout(true);openSplit.setResizeWeight(1);
        mainArea.setMinimumSize(new Dimension(320,0));openList.setMinimumSize(new Dimension(0,0));
        openList.setVisible(false);openSplit.setDividerSize(0);
        // Shown or not - its switch, the page in front, the window - the menu goes over it or back to the bar (decision 192).
        openList.addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0)SwingUtilities.invokeLater(this::placeMenu);});
        openSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,e->{
            if(placingOpen||waitingForWidth||!openList.isVisible())return;
            int wide=openSplit.getWidth()-openSplit.getDividerLocation()-GRIP;
            if(wide>=MIN_OPEN){openWidth=wide;openFolded=false;keepOpenWidth.restart();}
        });
        openSplit.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){if(openList.isVisible()&&openFolded)placeOpenList();}});
        pageSide.add(openSplit);
        // And on the right of Recent, the menu pinned open, where Show on Home says so (the owner, 2026-10-13).
        pageSide.add(pinnedMenu,BorderLayout.EAST);
        // The line between the panel and the page can be taken and pulled. It was one pixel wide, which no hand
        // can catch; it is six now, but painted as the page with one thin rule at the panel's edge, so what is
        // seen is still a single line. The width chosen is kept for next time.
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,side,pageSide);this.split=split;split.setDividerLocation(sideWidth);split.setDividerSize(GRIP);split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);keepWidth.setRepeats(false);
        // Folded to the edge, the tree takes no room and its line stays, as the list on the right does (see showTree).
        side.setMinimumSize(new Dimension(0,0));pageSide.setMinimumSize(new Dimension(420,0));
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI(){
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider(){return new Grip(this);}
        });
        split.setDividerSize(GRIP);split.setBorder(BorderFactory.createEmptyBorder());
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,e->{
            if(!treeWanted||split.getDividerLocation()<MIN_SIDE)return;
            sideWidth=split.getDividerLocation();keepWidth.restart();});
        shell.add(split);

        frame.setContentPane(shell);
        // What just happened, on a strip lying over the foot of the window (decision 97). It was a bar of the window's own,
        // and coming and going it pushed Home and the page up and let them down again; now nothing under it moves.
        status.setForeground(QUIET);status.setFont(body.deriveFont(13f));
        JPanel said=statusStrip=DesktopUi.floating(frame,status);said.setVisible(!status.getText().trim().isEmpty());
        // It is there only while it has something to say: it comes with a message and goes a few seconds later, except
        // while something is going on (a message ending in "…"), which stays until the work that said it says how it ended.
        javax.swing.Timer quiet=new javax.swing.Timer(6000,e->said.setVisible(false));quiet.setRepeats(false);
        status.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            void said(){SwingUtilities.invokeLater(()->{String now=status.getText().trim();quiet.stop();
                if(!now.isEmpty())DesktopUi.placeFloating(said);said.setVisible(!now.isEmpty());if(!now.isEmpty()&&!now.endsWith("…"))quiet.restart();});}
            public void insertUpdate(javax.swing.event.DocumentEvent e){said();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){said();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){said();}
        });frame.setFocusTraversalPolicy(DesktopUi.skippingText());
        title.getDocument().addDocumentListener(watch(this::edited));page.getDocument().addDocumentListener(watch(this::edited));
        // The overview lies over the window, above Home and the page, and follows the window's size.
        frame.getLayeredPane().add(overview,JLayeredPane.PALETTE_LAYER);
        frame.getLayeredPane().addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){if(overview.showing())SwingUtilities.invokeLater(overview::place);}});
        bind("control B",()->showTree(!treeWanted));
        // Ctrl+W closes what is open, as it closed a tab: the note goes from the overview, and Home is what is left.
        bind("control W",()->{if(onPage&&base!=null){String id=base.id;save(()->{overview.close(Overview.Kind.NOTE,id);goHome();});}
            else if(home.folder.isOpen()){NoteStore.Branch card=home.folder.shown();if(card!=null)overview.close(Overview.Kind.COLLECTION,card.id);home.folder.close();}});
        // Ctrl+Tab puts up the overview and moves along its cards, as Alt+Tab does with windows; letting go of Ctrl
        // opens the one chosen. Taken before the text areas can use it to move focus.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e->{
            if(!frame.isActive()||privately!=null&&privately.showing())return false;
            if(e.getID()==KeyEvent.KEY_RELEASED&&e.getKeyCode()==KeyEvent.VK_CONTROL&&overview.cycling()){overview.goChosen();return false;}
            if(e.getID()!=KeyEvent.KEY_PRESSED||e.getKeyCode()!=KeyEvent.VK_TAB||!e.isControlDown())return false;
            boolean backward=e.isShiftDown();save(()->overview.cycle(backward));return true;});
        // The Menu key and Shift+F10: the menu of whatever has the keyboard, as a right-click on it would open.
        bind("shift F10",this::contextKey);bind("CONTEXT_MENU",this::contextKey);
        // And a right-click on the page or its title opens the same menu there.
        DesktopMenus.onRightClick(page,this::paperClicked);DesktopMenus.onRightClick(title,this::paperClicked);
        bind("control N",()->save(this::newNote));bind("control S",()->save(null));
        // Ctrl+V on Home, in a folder or in Temp, where no field has the keys: the picture or the files on the clipboard
        // kept there (decision 233). On a note, or in a field, the field's own paste goes first and this never runs.
        bind("control V",this::pasteHere);
        bind("control F",()->save(()->{showHome();home.focusSearch();}));
        bind("control K",()->DesktopAssistant.open(this));
        // Esc: a menu that is up goes first, then the overview; on a note, back to Home (decision 24); on Home, out of
        // the card a level at a time, then the search emptied.
        bind("ESCAPE",this::escape);
        javax.swing.undo.UndoManager undo=new javax.swing.undo.UndoManager();page.getDocument().addUndoableEditListener(e->{if(!drawing)undo.addEdit(page.remembered(e.getEdit()));});
        page.putClientProperty("undo",undo);bind("control Z",()->{if(page.isEditable()&&undo.canUndo())undo.undo();});bind("control Y",()->{if(page.isEditable()&&undo.canRedo())undo.redo();});
    }    JButton button(String text,Runnable action){return DesktopUi.button(text,action);}
    private void bind(String key,Runnable action){frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),key);frame.getRootPane().getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){if(privately!=null&&privately.showing())return;action.run();}});}
    /** Whether the word at the caret of the page or the title could still become a private code (see PrivateCode). */
    private boolean codeBeingTyped() {
        for(javax.swing.text.JTextComponent field:new javax.swing.text.JTextComponent[]{page,title})
            if(field.isFocusOwner()&&PrivateCode.typing(field.getText(),field.getCaretPosition()))return true;
        return false;
    }
    /** A save that cannot wait: an unfinished private code at the caret taken out first, so it is never written down. */
    private void dropUnfinishedCode() {
        for(javax.swing.text.JTextComponent field:new javax.swing.text.JTextComponent[]{page,title}) {
            int[] code=PrivateCode.unfinished(field.getText(),field.getCaretPosition());
            if(code==null)continue;
            try{field.getDocument().remove(code[0],code[1]-code[0]);}catch(javax.swing.text.BadLocationException moved){/* not there now */}
            if(field==page)((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
        }
    }
    /**
     * The guard at the door, in the page and the title (the owner, 2026-10-06; decision 114): a whole code that got past the
     * catching, a paste or one an older build sent, taken out with the rest of its line before anything is written, so the
     * page shows at once what is kept; the caret moved back by what went before it, and nothing for Ctrl+Z to bring back.
     */
    private void scrubCodes() {
        if(base==null)return;
        for(javax.swing.text.JTextComponent field:new javax.swing.text.JTextComponent[]{page,title}) {
            List<int[]> codes=PrivateCode.runs(field.getText());
            if(codes.isEmpty())continue;
            int caret=PrivateCode.caretAfter(codes,field.getCaretPosition());
            try{for(int i=codes.size()-1;i>=0;i--)field.getDocument().remove(codes.get(i)[0],codes.get(i)[1]-codes.get(i)[0]);}
            catch(javax.swing.text.BadLocationException moved){/* not there now */}
            field.setCaretPosition(Math.min(caret,field.getDocument().getLength()));
            if(field==page)((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
        }
    }
    /** Whether a change to the page is being drawn rather than typed. */
    boolean drawingPage(){return drawing;}
    private static DocumentListener watch(Runnable action){return new DocumentListener(){public void insertUpdate(DocumentEvent e){action.run();}public void removeUpdate(DocumentEvent e){action.run();}public void changedUpdate(DocumentEvent e){action.run();}};}
    private void edited(){if(drawing||base==null)return;dirty=true;stampLine.edited();editorGeneration++;lastWriting=System.currentTimeMillis();due.remove(base.id);String limit=DesktopEdits.warning(title.getText(),noteNow());noteSays(limit==null?NoteLine.SAVING:limit,limit==null?NoteLine.Tone.GOING:NoteLine.Tone.FAILED,null);
        // Written in, and shared: waiting to go at once, as the phone's mark turns to the arrow.
        if(noteMark!=null&&waitingOver(noteMark)!=noteMark&&typedHere())showNoteMark(waitingOver(noteMark));
        autosave.restart();}
    /**
     * Whether the page holds writing the notebook does not: touched since it was last written down, and saying
     * something other than the stored note it came from. A key typed and taken back is not writing; nor is a page
     * showing text older than the notebook's, which only ever says what the notebook said when it was drawn.
     */
    boolean typedHere() {
        return dirty&&base!=null&&(!noteNow().equals(Objects.toString(base.body,""))||!title.getText().equals(Objects.toString(base.title,"")));
    }
    void save(Runnable next) {
        if(next!=null){afterSave=next;dropUnfinishedCode();}
        autosave.stop();if(saving)return;
        scrubCodes();
        // Nothing typed that the notebook does not say already: nothing is written, so nothing goes - above all not the
        // page's older text, written over what arrived one revision higher and sent back to the devices that wrote it.
        if(dirty&&!typedHere()&&!recovering){dirty=false;if(NoteLine.SAVING.equals(noteWordsSaid))noteSays(null,null,null);updateStanding();}
        if(!dirty||base==null){Runnable go=afterSave;afterSave=null;if(go!=null)go.run();return;}
        saving=true;String body=noteNow(),name=title.getText();NoteStore.Note seen=base.copy();Writers runs=noteWriters();
        String expected=recoveryToken;long generation=editorGeneration,session=editorSession;
        disk.submit(()->DesktopRecovery.save(store,seen,name,body,runs,expected),result->{
            if(base==null||!base.id.equals(seen.id)||session!=editorSession)return;
            saving=false;
            if(result.draft()!=null) {
                recoveryToken=result.draft().token();recovering=true;
                dirty=editorGeneration!=generation||!noteNow().equals(body)||!title.getText().equals(name);
                if(dirty){save(null);return;}
                noteSays(DesktopRecovery.KEPT,NoteLine.Tone.FAILED,this::recoveryWriting);
                status.setText(Say.t(result.draft().reason())+" "+Say.t("Writing kept on this PC"));
                if(followAfterSave){followAfterSave=false;changedUnderneath(seen.id);}
                Runnable go=afterSave;afterSave=null;if(go!=null)go.run();return;
            }
            NoteStore.Note saved=result.note();recoveryToken=null;recovering=false;String now=noteNow(),nowTitle=title.getText();
            boolean changed=!now.equals(body)||!nowTitle.equals(name);
            drawing=true;
            String merged=changed?Merge.merge(body,now,saved.body).text:saved.body;
            String mergedTitle=changed?Merge.merge(name,nowTitle,saved.title).text:saved.title;
            if(!noteNow().equals(merged)) {
                // What was typed while it was written down stays yours; what came with the writing keeps its writers.
                redrawWhole(merged,changed?Writers.follow(merged,Writers.UNKNOWN,now,noteWriters(),saved.body,saved.writers):saved.writers);
                ((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
            }
            if(!title.getText().equals(mergedTitle))title.setText(mergedTitle);
            drawing=false;
            // A size chosen while it was being written down is the page's, not the older one the notebook gave back.
            if(base!=null&&base.id.equals(saved.id))saved.rung=base.rung;
            base=saved;pageWritings++;dirty=!noteNow().equals(saved.body)||!title.getText().equals(saved.title);
            // Written in, it is one of the things open: a blank page nothing was written on never was.
            if(onPage&&written(saved))overview.remember(Overview.Kind.NOTE,saved.id);
            if(NoteLine.SAVING.equals(noteWordsSaid))noteSays(null,null,null);refresh();
            if(dirty){save(null);return;}scheduleSync();
            // Something arrived for it while it was being written down: the page now follows it (see changedUnderneath).
            if(followAfterSave){followAfterSave=false;changedUnderneath(saved.id);}
            Runnable go=afterSave;afterSave=null;if(go!=null)go.run();
        },error->{if(base==null||!base.id.equals(seen.id)||session!=editorSession)return;saving=false;afterSave=null;
            org.mininotes.desktop.platform.util.Log.i("Mininotes/Page","the open note was not saved: "+error.getClass().getSimpleName());
            // Why, on the bar as every error is; that it did not save, and the click that tries again, where it was said to be saving.
            noteSays("Not saved. Click to try again",NoteLine.Tone.FAILED,()->save(null));failed(error);
            // A writing that cannot be kept must not keep the page on older text either: what arrived is put with it.
            if(followAfterSave&&base!=null){followAfterSave=false;changedUnderneath(base.id);}});
    }
    /**
     * The note that is open changed in the notebook from somewhere else - a device's writing arrived, or waited in the
     * inbox while the notebook was locked. The page is a copy taken when it was drawn, and the next word typed on it
     * would otherwise be put against that older text and send the old lines back. As on the phone (see
     * {@link Arriving#onThePage}): a page nobody typed on simply becomes what the notebook says, with no writing of its
     * own and nothing Ctrl+Z could bring back; what was typed is put with what arrived, and written down.
     *
     * <p>Asked of the notebook on the disk worker, behind anything already on its way there. A save on its way is let
     * finish first - it merges against the notebook as it is by then - and the page follows once it is back.
     */
    private void changedUnderneath(String id) {
        if(base==null||id==null||!base.id.equals(id))return;
        // Open as it changed: seen, not new (the owner, 2026-10-14).
        disk.submit(()->{store.seen(id);return null;},done->{},e->{});
        if(saving){followAfterSave=true;return;}
        int asOf=pageWritings;
        disk.submit(()->{NoteStore.Note read=store.get(id);if(read!=null)read.writers=store.writersOf(id,read.body);
            return new Object[]{read,Boolean.TRUE.equals(store.readOnlyHere(id)[0])};},got->{
            NoteStore.Note stored=(NoteStore.Note)got[0];
            if(stored==null||base==null||!base.id.equals(id))return;
            if(saving){followAfterSave=true;return;}
            // Written down again while this was read: what was read may be older than the page now is, so it is read again.
            if(pageWritings!=asOf){changedUnderneath(id);return;}
            title.setEditable(recovering||!(Boolean)got[1]);page.setEditable(recovering||!(Boolean)got[1]);drawTabs();
            String keptTitle=Objects.toString(base.title,""),storedTitle=Objects.toString(stored.title,"");
            if(stored.revision==base.revision&&Objects.equals(stored.body,base.body)&&storedTitle.equals(keptTitle)){updateStanding();return;}
            boolean typed=typedHere()||recovering;
            Arriving.Page said=Arriving.onThePage(base.body,noteNow(),stored.body,typed);
            Arriving.Page named=Arriving.onThePage(keptTitle,title.getText(),storedTitle,typed);
            org.mininotes.desktop.platform.util.Log.i("Mininotes/Page",(said.unsaved||named.unsaved?"the open note changed elsewhere, and what was typed here is put with it"
                :typed?"the open note changed elsewhere, and what was typed here was already in it"
                :"the open note changed elsewhere, and the page, not typed in, now says what arrived")+" (revision "+base.revision+" to "+stored.revision+")");
            autosave.stop();base=stored;showSize(rung);
            // Drawn, not typed: no undo entry, and none of the old ones, which would put the older text back.
            drawing=true;
            if(!noteNow().equals(said.text)) {
                // What was typed here keeps being yours, and what arrived is whoever the notebook says wrote it.
                redrawWhole(said.text,typed?Writers.follow(said.text,Writers.UNKNOWN,noteNow(),noteWriters(),stored.body,stored.writers):stored.writers);
                ((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
            }
            if(!title.getText().equals(named.text))title.setText(named.text);
            drawing=false;
            // Words typed since the last writing are on the page and nowhere else, so it is written again.
            dirty=said.unsaved||named.unsaved;
            if(dirty)autosave.restart();else if(NoteLine.SAVING.equals(noteWordsSaid))noteSays(null,null,null);
            updateStanding();
        },this::failed);
    }
    /**
     * The page says something else now - the note changed underneath it - and the reader stays where they were: the
     * first line they could see, the caret and the selection are each taken to the same words (see Writers.moved), and
     * the view is put back on that line, never scrolled to the caret. Lines arriving above push the page down with the
     * words that were being read. Drawn, not typed: whoever calls this holds {@code drawing}.
     */
    // ---- tabs, kept as sections in the note's words (Sections; the owner, 2026-10-10; decision 142) ----------------------

    /** The band under the note's head: a shade darker than the paper the note is on, whatever colour that is. */
    private Color band(){Color paper=page.getBackground()==null?PAPER:page.getBackground();return shade(paper,INK,isDark(paper)?0.10f:0.07f);}
    private static boolean isDark(Color c){return (c.getRed()*299+c.getGreen()*587+c.getBlue()*114)/1000<128;}
    private static Color shade(Color from,Color to,float much) {
        return new Color(Math.round(from.getRed()+(to.getRed()-from.getRed())*much),Math.round(from.getGreen()+(to.getGreen()-from.getGreen())*much),
            Math.round(from.getBlue()+(to.getBlue()-from.getBlue())*much));
    }

    /** The tabs as cards, on the band under the bar. */
    final JPanel tabStrip=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)){@Override protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        // Where a carried tab would go, a bar between the cards (decision 239).
        if(tabMark>=0){g.setColor(INK);g.fillRect(tabMark-1,2,3,getHeight()-4);}}};
    /** A tab being carried along the strip (the owner, 2026-10-24: "We have to be able to drag and reorder the tabs in a note", decision 239): the one pressed, where it would go, the marker's x, or -1. */
    private int tabFrom=-1,tabTo=-1,tabMark=-1;private boolean tabCarrying;private java.awt.Point tabPressed;private KeyEventDispatcher tabEscape;private String statusBeforeTab;private boolean tabCancelled;
    private JPanel tabsBand,pathLine;JButton backButton;
    /** "+ Add a tab", at the end of the quiet line of a note with no tabs. */
    private final JButton addTabLink=new JButton("+ "+Say.t("Add a tab"));
    /** The tab open on the page, by note, for this session: a note opens again on the tab it was left on. */
    private final Map<String,Integer> tabsOpen=new HashMap<>();
    /** The open tab; the note it was cut from and that tab's words as they were; the note around them, and its writers. */
    int tab;
    private String tabBody="",tabWords="",tabBefore="",tabAfter="";
    private Writers tabPageWriters=Writers.none(),writersBefore=Writers.none(),writersAfter=Writers.none(),rawWritersBefore=Writers.none(),rawWritersAfter=Writers.none();

    /**
     * The note cut around one tab: its words go on the page, and what is before and after them is kept to put them back
     * between. Where the tab has no line end to keep typed words off the next tab line, or its tab line is the last thing
     * in the note, one is kept ready; it is only written if something is typed.
     */
    private void split(String body,Writers writers,int index) {
        String text=body==null?"":body;
        List<Sections.Section> all=Sections.of(text);
        tab=Math.max(0,Math.min(index,all.size()-1));
        Sections.Section s=all.get(tab);
        Writers w=writers!=null&&writers.length()==text.length()?writers:Writers.unknown(text.length());
        tabBody=text;tabWords=text.substring(s.start,s.end);
        tabBefore=text.substring(0,s.start);tabAfter=text.substring(s.end);
        rawWritersBefore=writersBefore=w.slice(0,s.start);rawWritersAfter=writersAfter=w.slice(s.end,text.length());
        tabPageWriters=w.slice(s.start,s.end);
        if(tab<all.size()-1&&s.start==s.end&&s.end<text.length()&&text.charAt(s.end)!='\n') {
            tabAfter="\n"+tabAfter;writersAfter=Writers.join(Writers.of(Writers.ME,1),writersAfter);
        }
        if(s.name!=null&&s.start==text.length()&&!text.isEmpty()&&text.charAt(text.length()-1)!='\n') {
            tabBefore=tabBefore+"\n";writersBefore=Writers.join(writersBefore,Writers.of(Writers.ME,1));
        }
        if(base!=null&&base.id!=null)tabsOpen.put(base.id,tab);
    }

    /** The whole note as it stands: the words around the open tab, and the tab's words as they are on the page. */
    String noteNow() {
        String now=page.getText();
        return now.equals(tabWords)?tabBody:tabBefore+now+tabAfter;
    }

    /** Who wrote each letter of the whole note, the page's share as the page has it. */
    private Writers noteWriters() {
        return page.getText().equals(tabWords)?Writers.join(rawWritersBefore,page.writers(),rawWritersAfter):Writers.join(writersBefore,page.writers(),writersAfter);
    }

    /** The open tab's name, or null for a first tab with none of its own. */
    private String tabName(){List<Sections.Section> all=Sections.of(tabBody);return tab<all.size()?all.get(tab).name:null;}

    /** The tab of that name in a note, or the same place where it is gone. */
    private static int tabNamed(String body,String name,int index) {
        List<Sections.Section> all=Sections.of(body);
        if(name!=null)for(int i=0;i<all.size();i++)if(name.equals(all.get(i).name))return i;
        return Math.max(0,Math.min(index,all.size()-1));
    }

    /** The note says something else now, whole: the same tab stays open, found by its name, and the page follows it. */
    private void redrawWhole(String text,Writers who) {
        split(text,who,tabNamed(text,tabName(),tab));
        redraw(tabWords,tabPageWriters);
        drawTabs();
    }

    /** The open tab on the page, from the top; drawn, not typed, and with nothing Ctrl+Z could take back across tabs. */
    private void showTab() {
        drawing=true;page.put(tabWords,tabPageWriters);page.setCaretPosition(0);drawing=false;
        ((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
        if(paperScroll!=null)paperScroll.getViewport().setViewPosition(new java.awt.Point(0,0));
        drawTabs();page.requestFocusInWindow();
    }

    /** The cards: each tab, the open one the paper itself; then + for another. Only + on a note with no tabs yet. */
    void drawTabs() {
        // The strip drawn again under a carried tab, writing that arrived say: the tab goes back where it was.
        if(tabCarrying)letGoOfTab(false);
        tabStrip.removeAll();
        if(base!=null) {
            String body=noteNow();
            List<Sections.Section> all=Sections.of(body);
            boolean writes=page.isEditable();
            if(Sections.tabbed(body))for(int i=0;i<all.size();i++) {
                final int index=i;final boolean open=i==tab;final String called=all.get(i).called(i);final int count=all.size();
                JLabel card=new JLabel(called){@Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(open?page.getBackground():shade(band(),INK,0.06f));
                    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0,0,getWidth(),getHeight()+12,12,12));g2.dispose();
                    super.paintComponent(g);}};
                card.setFont(DesktopUi.BODY.deriveFont(open?Font.BOLD:Font.PLAIN,14f));card.setForeground(open?INK:QUIET);
                card.setBorder(BorderFactory.createEmptyBorder(open?9:7,open?18:16,open?9:7,open?18:16));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.getAccessibleContext().setAccessibleName("Tab "+called+(open?", open":""));
                card.setToolTipText(writes?Say.t(count>1?"Drag to move it, right-click to rename or remove":"Right-click to rename or remove"):null);
                // Pressed and pulled sideways, a tab is carried along the strip and put where it is let go (decision 239);
                // only a press that never went far is a click that opens it.
                MouseAdapter mouse=new MouseAdapter(){
                    @Override public void mousePressed(MouseEvent e){
                        tabCancelled=false;
                        if(e.isPopupTrigger()){tabMenu(card,index,called,count,e);return;}
                        if(SwingUtilities.isLeftMouseButton(e)&&writes&&count>1){tabFrom=index;tabPressed=e.getPoint();}}
                    @Override public void mouseDragged(MouseEvent e){
                        if(tabFrom!=index||tabPressed==null)return;
                        if(!tabCarrying&&Math.abs(e.getX()-tabPressed.x)<6)return;
                        if(!tabCarrying)startCarryingTab();
                        carryTab(SwingUtilities.convertPoint(card,e.getPoint(),tabStrip).x);}
                    @Override public void mouseReleased(MouseEvent e){
                        if(e.isPopupTrigger()){tabMenu(card,index,called,count,e);return;}
                        if(tabCarrying&&tabFrom==index){letGoOfTab(true);return;}
                        // Let go after Esc put the tab back: nothing more, not a click.
                        if(tabCancelled){tabCancelled=false;return;}
                        tabFrom=-1;tabPressed=null;
                        if(SwingUtilities.isLeftMouseButton(e))switchTab(index);}};
                card.addMouseListener(mouse);card.addMouseMotionListener(mouse);
                tabStrip.add(card);
            }
            boolean some=Sections.tabbed(body);
            if(tabsBand!=null)tabsBand.setVisible(some&&!compactNote);
            addTabLink.setVisible(!some&&writes);
            if(writes&&some) {
                JButton plus=new JButton("+");plus.setToolTipText(Say.t("Add a tab"));plus.getAccessibleContext().setAccessibleName("Add a tab");
                plus.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
                plus.setFont(DesktopUi.BODY.deriveFont(18f));plus.setForeground(QUIET);plus.setFocusPainted(false);plus.addActionListener(e->addTab());
                tabStrip.add(plus);
            }
        }
        tabStrip.revalidate();tabStrip.repaint();
    }

    void switchTab(int index) {
        if(base==null||index==tab)return;
        split(noteNow(),noteWriters(),index);
        showTab();
    }

    /** Another tab, last, opened, and its name asked; Cancel keeps the one it was given. */
    /** A translation of the tab in front, as a tab of its own after the others, named for its language (docs/TRANSLATION.md). */
    void addTranslatedTab(String language,String words) {
        if(base==null||!page.isEditable())return;
        String was=noteNow(),now=Sections.add(was,language);int index=Sections.of(now).size()-1;
        now=Sections.put(now,index,words);
        changeTabs(was,now,index);
    }

    /** The tab in front, its words put in place of what it said, who wrote what followed as an edit is (docs/TRANSLATION.md). */
    void replaceTab(String words) {
        if(base==null||!page.isEditable())return;
        String was=noteNow(),now=Sections.put(was,tab,words);
        if(!now.equals(was))changeTabs(was,now,tab);
    }

    void addTab() {
        if(base==null||!page.isEditable())return;
        String body=noteNow(),now=Sections.add(body,null);
        changeTabs(body,now,Sections.of(now).size()-1);
        String called=Sections.of(now).get(tab).called(tab);
        String name=DesktopUi.ask(frame,"Name this tab","Name",called);
        if(name!=null&&!name.isBlank()&&!name.strip().equals(called)){String was=noteNow(),renamed=Sections.rename(was,tab,name);if(!renamed.equals(was))changeTabs(was,renamed,tab);}
    }

    private void tabMenu(Component card,int index,String called,int count,MouseEvent e) {
        if(!page.isEditable())return;
        JPopupMenu menu=new JPopupMenu();
        JMenuItem named=DesktopMenus.plain(called);named.setEnabled(false);menu.add(named);menu.addSeparator();
        item(menu,"Rename…",()->{String name=DesktopUi.ask(frame,"Rename tab","Name",called);
            if(name==null)return;String was=noteNow(),now=Sections.rename(was,index,name);if(!now.equals(was))changeTabs(was,now,index);});
        // The same as carrying it, one place at a time, for the keyboard and for precision (decision 239); the open tab stays open.
        if(index>0)item(menu,"Move left",()->moveTab(index,index-1,Sections.follow(tab,index,index-1)));
        if(index<count-1)item(menu,"Move right",()->moveTab(index,index+1,Sections.follow(tab,index,index+1)));
        if(count>1)item(menu,"Remove this tab…",()->{
            int answer=JOptionPane.showConfirmDialog(frame,Say.t("Its words go with it. Versions still has the note as it was."),Say.t("Remove {0}?",called),JOptionPane.OK_CANCEL_OPTION);
            if(answer!=JOptionPane.OK_OPTION)return;
            String was=noteNow(),now=Sections.remove(was,index);changeTabs(was,now,tab>index?tab-1:tab==index?Math.max(0,index-1):tab);});
        menu.show(card,e.getX(),e.getY());
    }

    private static final String TAB_CARRIED="Let go to put the tab there  ·  Esc cancels";
    /** The carrying of a tab begun: the strip says how to finish it, and Esc puts it back (as carrying on Home does). */
    private void startCarryingTab() {
        tabCarrying=true;tabCancelled=false;tabTo=tabFrom;
        statusBeforeTab=status.getText();status.setToolTipText(null);status.setText(Say.t(TAB_CARRIED));
        tabEscape=k->{
            if(!tabCarrying||k.getKeyCode()!=KeyEvent.VK_ESCAPE)return false;
            if(k.getID()==KeyEvent.KEY_PRESSED)letGoOfTab(false);
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(tabEscape);
    }

    /** The carried tab over the strip at x: where it would go, the gap before the first card whose middle is past x. */
    private void carryTab(int x) {
        List<Component> cards=new ArrayList<>();for(Component c:tabStrip.getComponents())if(c instanceof JLabel)cards.add(c);
        if(cards.isEmpty())return;
        int slot=0;for(Component c:cards)if(c.getX()+c.getWidth()/2<x)slot++;
        tabTo=slot>tabFrom?slot-1:slot;
        Component last=cards.get(cards.size()-1);
        tabMark=tabTo==tabFrom?-1:slot<cards.size()?cards.get(slot).getX()-2:last.getX()+last.getWidth()+2;
        tabStrip.repaint();
    }

    /** Let go: put where the marker was, or, after Esc or when the strip is drawn again under it, back where it was. */
    private void letGoOfTab(boolean put) {
        int from=tabFrom,to=tabTo;
        tabCarrying=false;tabFrom=-1;tabTo=-1;tabMark=-1;tabPressed=null;tabCancelled=!put;
        if(tabEscape!=null){KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(tabEscape);tabEscape=null;}
        if(statusBeforeTab!=null&&status.getText().equals(Say.t(TAB_CARRIED)))status.setText(statusBeforeTab);
        statusBeforeTab=null;tabStrip.repaint();
        // The moved tab is the one open after, as it was the one in hand.
        if(put&&from>=0&&to>=0&&to!=from)moveTab(from,to,to);
    }

    /** One tab put in another place, as an edit of the note like any other; said only where it could not be. */
    private void moveTab(int from,int to,int open) {
        if(base==null||!page.isEditable())return;
        String was=noteNow(),now=Sections.move(was,from,to);
        if(now.equals(was)){status.setText(Say.t("That tab could not be moved"));return;}
        changeTabs(was,now,open);
    }

    /** For tests: Move left or Move right as the tab's menu does it. */
    void moveTabForTest(int from,int to){moveTab(from,to,Sections.follow(tab,from,to));}
    /** For tests: the same as Add, Rename or Remove without their boxes, and an arrival followed. */
    void changeTabsForTest(String was,String now,int index){changeTabs(was,now,index);}
    void followForTest(String id){changedUnderneath(id);}
    /** The note's tabs changed here: written as any writing is, the open tab shown, who wrote what kept. */
    private void changeTabs(String was,String now,int index) {
        Writers who=Writers.follow(now,Writers.ME,was,noteWriters());
        split(now,who,index);
        showTab();
        dirty=true;editorGeneration++;lastWriting=System.currentTimeMillis();save(null);
    }

    private void redraw(String text,Writers who) {
        String was=page.getText();
        JViewport port=paperScroll==null?null:paperScroll.getViewport();
        Rectangle view=port==null?null:port.getViewRect();
        int top=0,into=0;
        if(view!=null)try {
            top=page.viewToModel2D(new java.awt.Point(view.x,view.y));
            java.awt.geom.Rectangle2D line=page.modelToView2D(top);
            if(line!=null)into=view.y-(int)Math.round(line.getY());
        } catch(Exception notLaidOut){top=0;into=0;}
        int dot=page.getCaret().getDot(),mark=page.getCaret().getMark();
        int[] at=Writers.moved(was,text,top,mark,dot);
        page.holding(true);
        page.put(text,who);
        int length=page.getDocument().getLength();
        page.getCaret().setDot(Math.min(at[1],length));
        if(at[2]!=at[1])page.getCaret().moveDot(Math.min(at[2],length));
        int anchor=Math.min(at[0],length),below=into;
        String anchored=base==null?null:base.id;long anchoredGeneration=editorGeneration;
        Runnable back=()->{
            if(base==null||!Objects.equals(anchored,base.id)||anchoredGeneration!=editorGeneration)return;
            if(port!=null)try {
                java.awt.geom.Rectangle2D line=page.modelToView2D(anchor);
                if(line!=null)port.setViewPosition(new java.awt.Point(view.x,Math.max(0,(int)Math.round(line.getY())+below)));
            } catch(Exception notLaidOut){/* where it is */}
        };
        // Now; and once more after the page has been laid out again and the caret has had its say, which it has a moment
        // after it moves - only then does it scroll the page again.
        back.run();SwingUtilities.invokeLater(()->{back.run();page.holding(false);});
    }
    private void display(NoteStore.Note note) {
        // What the pinned menu offers follows what is in front: this note's lines now.
        pinnedMenu.soon();
        overview.close();
        ((CardLayout)mainArea.getLayout()).show(mainArea,"page");onPage=true;keepWhere(NOTE_SHOWN+note.id);
        SwingUtilities.invokeLater(openList::refresh);
        peopleShown=List.of();DesktopMark.people(rounds,peopleShown,24);
        selected=null;
        // What was said about the last note is not about this one; the same note drawn again keeps it.
        if(base==null||!base.id.equals(note.id)) {
            noteSays(null,null,null);
            // A help request, where this note is set to send one when it opens (decision 113): once per opening, not each redraw.
            helpOnOpened(NoteStore.Branch.Kind.PAGE,note.id);
        }
        recorder.leaving(note.id);
        page.setEditable(false);title.setEditable(false);
        if(base!=null)carets.put(base.id,page.getCaretPosition());
        // Opened at the top, the caret at its start (the owner, 2026-10-02: a note opened where it was left, in the middle,
        // and the beginning had to be scrolled back to); the same note drawn again keeps its place.
        int at=base!=null&&base.id.equals(note.id)?page.getCaretPosition():0;
        // The note's own size before its words, so they are never drawn at another note's first.
        drawing=true;editorGeneration++;editorSession++;readingId=null;recoveryToken=null;recovering=false;base=note;dirty=false;showSize(rung);page.lined=note.lines;title.setText(note.title);
        // The tab it was last open on in this session, or the first (decision 142).
        Integer wasTab=tabsOpen.get(note.id);split(note.body,note.writers,wasTab==null?0:wasTab);page.put(tabWords,tabPageWriters);
        page.setCaretPosition(Math.min(at,page.getDocument().getLength()));drawing=false;drawTabs();
        // One of the things open, for the overview; not a blank page nothing was written on, as on the phone.
        if(written(note))overview.remember(Overview.Kind.NOTE,note.id);
        fileCards.show(note.id);stampLine.follow(note.id,note.title,note.body);washPage();syncMark.setVisible(false);noteMark=null;lostStrip.update(this);
        // Its icon as the tree last read it, at once, so the face before the title is never another note's; read again below.
        NoteStore.Branch known=find(note.id);noteIcon=known==null?"":known.icon;noteImage=known==null?null:known.image;noteFace.repaint();
        ((javax.swing.undo.UndoManager)page.getClientProperty("undo")).discardAllEdits();
        disk.submit(()->Boolean.TRUE.equals(store.readOnlyHere(note.id)[0]),readOnly->{if(base==null||!base.id.equals(note.id))return;title.setEditable(recovering||!readOnly);page.setEditable(recovering||!readOnly);drawTabs();updateStanding();},this::failed);
        askWriters(note.id,note.body);
        page.requestFocusInWindow();SwingUtilities.invokeLater(()->{rememberReading();fitNote.run();});
    }
    /**
     * Who wrote what on a note just opened, and the colours they are drawn in, asked of the notebook once its words are
     * on the page: the letters take their writers as they come, and anything typed meanwhile stays yours.
     */
    private void askWriters(String id,String body) {
        disk.submit(()->new Object[]{store.palette(),store.writersOf(id,body)},got->{
            palette=(Writers.Palette)got[0];page.inks(palette,whoWrote);
            // The note's writers, cut to the tab on the page; the rest kept for when it is written down whole.
            if(base!=null&&base.id.equals(id)&&body.equals(tabBody)){split(body,(Writers)got[1],tab);page.follow(tabWords,tabPageWriters);rounds.repaint();}
        },this::failed);
    }
    /** "Show who wrote what", turned on or off: the page at once, and kept for the next time. */
    void setWhoWrote(boolean on) {
        whoWrote=on;page.inks(palette,on);updateStanding();
        disk.submit(()->{DesktopLook.keepWhoWrote(context,on);return null;},done->{},this::failed);
    }
    /** A writer's colour chosen here: kept on this PC, and the page drawn in it at once. */
    void chooseInk(String writer,int colour) {
        // Drawn again at once everywhere a round or the page shows it, the box it was chosen in too (the owner, 2026-10-17:
        // "the colour doesn't change, please make sure it is reflected right away").
        disk.submit(()->{store.chooseInk(writer,colour);return store.palette();},now->{palette=now;roundInks=new HashMap<>(roundInks);page.inks(palette,whoWrote);updateStanding();
            recolourRounds();},this::failed);
    }
    /** Every person's round showing, in any window, worn again in the colour its person writes in now. */
    private void recolourRounds() {
        List<JComponent> rounds=new ArrayList<>();
        for(Window w:Window.getWindows())if(w.isShowing())collectRounds(w,rounds);
        disk.submit(()->{
            Writers.Palette now=store.palette();Map<JComponent,Integer> colours=new HashMap<>();
            for(JComponent c:rounds){String a=(String)c.getClientProperty("faceOf");colours.put(c,now.colourOf(a==null||a.isEmpty()?Writers.ME:store.writerOf(a)));}
            return colours;
        },colours->{
            for(Map.Entry<JComponent,Integer> one:colours.entrySet()) {
                int colour=one.getValue();JComponent c=one.getKey();
                if(!Tint.known(colour)){c.putClientProperty("fill",null);c.putClientProperty("letter",null);}
                else{int fill=Tint.over(colour,DesktopUi.CARD.getRGB(),0.22f,false);c.putClientProperty("fill",new Color(fill));c.putClientProperty("letter",new Color(Writers.ink(colour,fill)));}
                c.repaint();
            }
            for(Window w:Window.getWindows())if(w.isShowing())w.repaint();
        },e->{});
    }
    private static void collectRounds(Container in,List<JComponent> out) {
        for(Component c:in.getComponents()) {
            if(c instanceof JComponent j&&j.getClientProperty("faceOf")!=null)out.add(j);
            if(c instanceof Container k)collectRounds(k,out);
        }
        if(in instanceof RootPaneContainer r&&r.getContentPane()!=in)collectRounds(r.getContentPane(),out);
    }
    /** The rounds a writer's colour is chosen from, for the writer a device's writing is, or for you where there is none. */
    void inksOf(String address,java.util.function.Consumer<JComponent> then) {
        inkRow(address,(you,row)->{
            JPanel box=DesktopUi.column();
            DesktopUi.add(box,DesktopUi.quiet(you?"Your colour, which everybody sees":"Their colour, on this PC"));DesktopUi.gap(box,4);
            DesktopUi.add(box,row);
            then.accept(box);
        });
    }
    /** The rounds alone, and whether they are yours. */
    void inkRow(String address,java.util.function.BiConsumer<Boolean,JPanel> then) {
        disk.submit(()->new Object[]{address==null?Writers.ME:store.writerOf(address),store.palette()},got->{
            palette=(Writers.Palette)got[1];String writer=(String)got[0];
            String who=Writers.ME.equals(palette.person(writer))?Writers.ME:palette.person(writer);
            then.accept(Writers.ME.equals(who),DesktopLook.inks(palette,writer,colour->chooseInk(who,colour)));
        },this::failed);
    }
    /** The colour each person on the open note's line writes in, by their device, as last read. */
    private Map<String,Integer> roundInks=Map.of();
    /**
     * The colour a person's round wears: their colour, always, so two people with the same initial are told apart (the
     * owner, 2026-10-03); on a page drawn in its writers' colours it also says whose the coloured words are.
     */
    private int roundInk(SyncStatus.Person who){return roundInks.getOrDefault(who.address(),Tint.NONE);}
    /**
     * A linked person's round clicked: who, where they stand, the colour their writing is drawn in here, and Contact on
     * Parlons!, the one button (the owner, 2026-10-05: "in the people details here, let's have a contact button, and if the
     * Parlons! details are not specified, let's link this to the People and devices"; decision 101).
     */
    void personClicked(SyncStatus.Person who,Component round) {
        // Who and where they stand, and their colour; no Parlons! here (the owner, 2026-10-17: "we don't need discuss on
        // Parlons! here"). One of your own devices writes as you: its colour is yours, set in Profile, not chosen on its round,
        // where it changed every one of your devices' rounds at once (the owner: "it changes colors of both").
        inkRow(who.address(),(you,row)->{
            if(!round.isShowing())return;
            JPanel more=DesktopUi.column();
            if(you)DesktopUi.add(more,DesktopUi.note(Say.t("One of your devices. It writes in your colour, which you set in Profile."),260,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
            else{DesktopUi.add(more,DesktopUi.quiet("Their colour, on this PC"));DesktopUi.gap(more,4);DesktopUi.add(more,row);}
            DesktopMark.told(who,round,more).show(round,0,round.getHeight()+4);
        });
    }

    // ---- a person's Parlons! address (decision 101) ------------------------------------------------------------

    /** Every Parlons! address set here, by device, as last read: what a menu's words are chosen by before it opens. */
    volatile Map<String,String> parlonsBook=Map.of();
    /** Chat on Parlons! where their address is known; where it is not, said missing, greyed, with the item that adds it. */
    void parlonsItems(JPopupMenu menu,String address) {
        if(address==null)return;
        if(parlonsBook.containsKey(address)){item(menu,Parlons.CONTACT,()->contactOnParlons(address));return;}
        // Missing, said as such, and the item itself is the way to add it (the owner, 2026-10-10).
        item(menu,Parlons.MISSING,()->contactOnParlons(address));
    }
    /** What the line that contacts somebody says: Chat on Parlons! where their address is known, or asks for it. */
    String parlonsWords(String address){return address!=null&&parlonsBook.containsKey(address)?Parlons.CONTACT:Parlons.ADD;}
    /** A Parlons! address on the clipboard, as copied from Parlons! or a message with it in, or empty. */
    static String copiedParlons() {
        try {
            Object said=Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            return said==null?"":Parlons.address(said.toString());
        } catch(Exception notOne){return "";}
    }

    /** Contact on Parlons!, from anywhere a person is: their address copied, or, where there is none yet, the box that takes it. */
    void contactOnParlons(String address) {
        if(address==null)return;
        disk.submit(()->Optional.ofNullable(store.address(address)),who->{
            if(who.isEmpty()){status.setText(Say.t("They are no longer in People and devices."));return;}
            if(who.get().parlons.isEmpty())parlonsBox(who.get(),null);else copyParlons(who.get().name,who.get().parlons);
        },this::failed);
    }
    /**
     * Their address on the clipboard, and said so. Parlons! is a phone's app: there is none on Windows to open, and on the
     * phone it opens on nothing but its own first screen, so an address is always pasted there.
     */
    void copyParlons(String name,String parlons) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(parlons),null);
        status.setToolTipText(null);status.setText(Say.t("Copied {0}'s Parlons! address. Paste it in Parlons! on your phone to add or find them.",name));
    }
    /**
     * A person's Parlons! address, set, changed or removed, and where it goes (the owner: "when we set a contact, we should
     * have the option to reflect that across all my devices, pick the devices to copy the contact details, or keep on the
     * current device"): only this PC, all my devices (chosen first when there are any), or the ones switched on. Save is the
     * one primary button. {@code after} runs once it is saved, to draw again what showed it; null for nothing.
     */
    void parlonsBox(NoteStore.Contact contact,Runnable after){parlonsBox(contact,after,null,null);}
    /** @param over the box it is opened from, a person's page, and {@code backTo} what its ‹ says it goes back to; null from the window */
    void parlonsBox(NoteStore.Contact contact,Runnable after,Window over,String backTo) {
        disk.submit(()->{
            NoteStore.Contact now=store.address(contact.address);
            List<NoteStore.Contact> own=new ArrayList<>();Set<String> takes=new HashSet<>();
            if(now!=null&&now.paired())for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired()){own.add(one);if(Post.takesParlons(context,store,one))takes.add(one.address);}
            return new Object[]{Optional.ofNullable(now),own,takes};
        },got->{
            @SuppressWarnings("unchecked") Optional<NoteStore.Contact> found=(Optional<NoteStore.Contact>)got[0];
            if(found.isEmpty()){status.setText(Say.t("They are no longer in People and devices."));return;}
            NoteStore.Contact who=found.get();
            @SuppressWarnings("unchecked") List<NoteStore.Contact> own=(List<NoteStore.Contact>)got[1];
            @SuppressWarnings("unchecked") Set<String> takes=(Set<String>)got[2];
            JDialog[] box={null};
            JPanel body=DesktopUi.column();
            // What happens by itself first, then the one thing to do (the owner, 2026-10-11; decision 147).
            DesktopUi.add(body,DesktopUi.note(who.parlons.isEmpty()&&who.paired()&&!who.mine
                ?"When "+who.name+" sets their Parlons! address in Mininotes, it arrives here by itself. Or paste it now:"
                :"Their address, as Parlons! shows it. A whole message with it in will do: the address is found in it.",440,QUIET,DesktopUi.BODY.deriveFont(13f)));
            DesktopUi.gap(body,DesktopUi.S);
            JTextField field=new JTextField(who.parlons,32);field.putClientProperty("JTextField.placeholderText",Parlons.HINT);
            // Copied from Parlons! a moment ago: already in the field.
            if(who.parlons.isEmpty()){String copied=copiedParlons();if(!copied.isEmpty())field.setText(copied);}
            field.getAccessibleContext().setAccessibleName("Parlons! address of "+who.name);
            DesktopUi.add(body,field);
            DesktopUi.Text wrong=DesktopUi.note(" ",440,INK,DesktopUi.BODY.deriveFont(13f));wrong.setVisible(false);
            DesktopUi.gap(body,4);DesktopUi.add(body,wrong);
            // Where it goes: a choice of three, dropping down; Choose devices… shows a switch for each of mine.
            String[] where={own.isEmpty()?Parlons.ONLY_HERE:Parlons.ALL_MINE};
            Set<String> picked=new HashSet<>(takes);
            JPanel picks=DesktopUi.column();picks.setVisible(false);
            Runnable fit=()->{if(box[0]==null)return;box[0].validate();Dimension wants=box[0].getPreferredSize();box[0].setSize(Math.max(box[0].getWidth(),wants.width),Math.min(640,wants.height));};
            if(!own.isEmpty()) {
                JButton goes=new JButton(Say.t(where[0])+"  ▾");goes.setFocusPainted(false);goes.getAccessibleContext().setAccessibleName("Where it goes: "+where[0]);
                goes.addActionListener(e->{
                    JPopupMenu menu=new JPopupMenu();ButtonGroup one=new ButtonGroup();
                    for(String choice:new String[]{Parlons.ONLY_HERE,Parlons.ALL_MINE,Parlons.CHOOSE}) {
                        JRadioButtonMenuItem item=new JRadioButtonMenuItem(Say.t(choice),choice.equals(where[0]));one.add(item);
                        item.addActionListener(a->{where[0]=choice;goes.setText(Say.t(choice)+"  ▾");goes.getAccessibleContext().setAccessibleName("Where it goes: "+choice);
                            picks.setVisible(Parlons.CHOOSE.equals(choice));fit.run();});
                        menu.add(item);
                    }
                    menu.show(goes,0,goes.getHeight());
                });
                DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.row(DesktopUi.body("Where it goes"),goes));
                for(NoteStore.Contact one:own) {
                    boolean can=takes.contains(one.address);
                    JCheckBox on=DesktopUi.toggle(one.name,can);on.setEnabled(can);
                    on.addActionListener(e->{if(on.isSelected())picked.add(one.address);else picked.remove(one.address);});
                    DesktopUi.add(picks,DesktopUi.switchRow(can?one.name:one.name+" · needs an update",on));
                }
                DesktopUi.add(body,picks);
            }
            java.util.function.Supplier<Set<String>> going=()->{
                Set<String> to=new HashSet<>();
                if(Parlons.ALL_MINE.equals(where[0]))for(NoteStore.Contact one:own)to.add(one.address);
                else if(Parlons.CHOOSE.equals(where[0]))to.addAll(picked);
                return to;};
            if(!who.parlons.isEmpty()) {
                JButton remove=button("Remove",()->{box[0].dispose();saveParlons(who,"",going.get(),after);});
                remove.getAccessibleContext().setAccessibleName("Remove "+who.name+"'s Parlons! address");
                DesktopUi.gap(body,DesktopUi.M);
                DesktopUi.add(body,DesktopUi.actions(button(Parlons.CONTACT,()->{box[0].dispose();copyParlons(who.name,who.parlons);}),remove));
            }
            // Save checks the address first, and keeps the box open over words that say what is wrong with it.
            JButton save=DesktopUi.primary("Save",()->{
                try {
                    String kept=Parlons.address(field.getText());
                    if(kept.isEmpty()&&who.parlons.isEmpty())throw new IllegalArgumentException("Paste their Parlons! address first.");
                    box[0].dispose();saveParlons(who,kept,going.get(),after);
                } catch(IllegalArgumentException wrongly){wrong.setText(Say.t(wrongly.getMessage()));wrong.setVisible(true);fit.run();}
            });
            box[0]=DesktopUi.sheet(over==null?frame:over,who.name+" on Parlons!",body,DesktopUi.footer(save),true);
            if(backTo!=null)DesktopUi.head(box[0],backTo,box[0]::dispose,who.name+" on Parlons!",null);
            box[0].getRootPane().setDefaultButton(save);box[0].getRootPane().putClientProperty("focus",field);
            DesktopUi.show(box[0],480,640);
        },this::failed);
    }
    /**
     * An address kept here, then sent to the devices chosen: said in the status line from when it starts to how it ended,
     * by the names of the devices it reached. Nothing goes on in silence.
     */
    void saveParlons(NoteStore.Contact who,String value,Set<String> to,Runnable after) {
        boolean removed=value.isEmpty();
        status.setToolTipText(null);status.setText(Say.t(removed?"Removing {0}'s Parlons! address…":"Saving {0}'s Parlons! address…",who.name));
        disk.submit(()->{store.setParlons(who.address,value,System.currentTimeMillis());parlonsBook=store.parlonsBook();return null;},done->{
            if(after!=null)after.run();
            if(to.isEmpty()){status.setText(Say.t(Parlons.said(removed,Post.here(),List.of(),List.of(),List.of())));return;}
            status.setText(Say.t("Sending to my devices…"));
            network.submit(()->Post.sendParlons(context,store,keys,who.address,to,Post.here()),said->status.setText(Say.t(said)),this::failed);
        },this::failed);
    }

    /** This PC as People and devices shows it (decision 124). */
    Called.Choice deviceName() {
        var kept=context.getSharedPreferences("settings",0);
        return new Called.Choice(Node.deviceHere(context),kept.getString(Called.PREF_PICKED,""),kept.getInt(Called.PREF_SHOW,0)==1,kept.getInt(Called.PREF_HERE,0)==1);
    }

    /** The name this PC shows for itself. */
    String deviceShown(){Called.Choice name=deviceName();return Called.display(name.shared,name.picked,name.showOurs);}

    /** The name we pick for somebody, kept here and sent to every device of ours unless it stays on this PC (decision 124). */
    void saveCalled(NoteStore.Contact who,String picked,boolean showOurs,boolean onlyHere,Runnable after) {
        long now=System.currentTimeMillis();
        status.setToolTipText(null);status.setText(Say.t("Saving the name…"));
        disk.submit(()->{
            store.setCalled(who.address,picked,showOurs,onlyHere,now);
            java.util.List<String> to=new java.util.ArrayList<>();
            if(!onlyHere)for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())to.add(one.address);
            return to;
        },to->{
            if(after!=null)after.run();refresh();
            if(to.isEmpty()){status.setText(Called.said(Post.here(),java.util.List.of(),java.util.List.of(),java.util.List.of()));return;}
            status.setText(Say.t("Sending to my devices…"));
            network.submit(()->Post.sendCalled(context,store,keys,who.address,to,Post.here()),said->status.setText(Say.t(said)),this::failed);
        },this::failed);
    }

    /** The same for this PC. */
    void saveThisCalled(String picked,boolean showOurs,boolean onlyHere,Runnable after) {
        long now=System.currentTimeMillis();
        String ours=Called.clip(picked);boolean show=showOurs&&!ours.isEmpty();
        status.setToolTipText(null);status.setText(Say.t("Saving the name…"));
        disk.submit(()->{
            context.getSharedPreferences("settings",0).edit().putString(Called.PREF_PICKED,ours).putInt(Called.PREF_SHOW,show?1:0)
                .putInt(Called.PREF_HERE,onlyHere?1:0).putLong(Called.PREF_AT,now).apply();
            java.util.List<String> to=new java.util.ArrayList<>();
            if(!onlyHere)for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())to.add(one.address);
            return to;
        },to->{
            if(after!=null)after.run();
            if(to.isEmpty()){status.setText(Called.said(Post.here(),java.util.List.of(),java.util.List.of(),java.util.List.of()));return;}
            status.setText(Say.t("Sending to my devices…"));
            network.submit(()->Post.sendCalled(context,store,keys,null,to,Post.here()),said->status.setText(Say.t(said)),this::failed);
        },this::failed);
    }

    /** What this PC shares as its own name among your devices. */
    void renameSharedDevice(String said,Runnable after) {
        String name=said==null?"":said.trim();
        disk.submit(()->{Node.chooseDevice(context,name);return null;},
            done->{if(after!=null)after.run();status.setText(name.isEmpty()?Say.t("Back to the PC's own name"):Say.t("Your devices call this one {0}",Node.deviceHere(context)));},
            this::failed);
    }
    /**
     * A person on a list of people, whose round opens the colour their writing is drawn in here, as a round under a
     * note's title does. {@code address} is their device; null for you.
     */
    JPanel inkOnRound(JPanel person,String name,String address) {
        Component round=roundOf(person);
        round.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if(round instanceof JComponent c) {
            c.setToolTipText(Say.t("Writing colour of {0}",name));
            // A bare round has no accessible context of its own; the words beside it name the person.
            if(c.getAccessibleContext()!=null)c.getAccessibleContext().setAccessibleName("Writing colour of "+name);
        }
        round.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){
            if(SwingUtilities.isLeftMouseButton(e))inks(round,name,address);
        }});
        return inRound(person,address);
    }
    /** A person's round in their colour, as their round under a note's title is: two people with the same initial are told apart by it. */
    JPanel inRound(JPanel person,String address) {
        Component round=roundOf(person);
        if(round instanceof JComponent face)face.putClientProperty("faceOf",address==null?"":address);
        if(round instanceof JComponent c)disk.submit(()->store.palette().colourOf(address==null?Writers.ME:store.writerOf(address)),colour->{
            if(!Tint.known(colour))return;
            int fill=Tint.over(colour,DesktopUi.CARD.getRGB(),0.22f,false);
            c.putClientProperty("fill",new Color(fill));c.putClientProperty("letter",new Color(Writers.ink(colour,fill)));c.repaint();
        },e->{});
        return person;
    }
    // ---- a person's picture (decision 158) ----------------------------------------------------------------------------

    /** Picture…: choose one, or take off the one picked here. For you (null): yours, which goes to everybody paired. */
    void faceMenu(Component near,String address,String name) {
        disk.submit(()->address==null?store.myFace()!=null:store.faceIsPickedHere(address),has->{
            if(!has){chooseFace(address);return;}
            JPopupMenu menu=new JPopupMenu();
            JMenuItem pick=DesktopMenus.item("Choose a picture…");pick.addActionListener(e->chooseFace(address));menu.add(pick);
            JMenuItem off=DesktopMenus.item("Remove the picture");off.addActionListener(e->setFace(address,null));menu.add(off);
            if(near!=null&&near.isShowing())menu.show(near,0,near.getHeight());else chooseFace(address);
        },this::failed);
    }
    /** The system's box for a picture; the one chosen made small, as a thing's is, and kept as yours or as theirs here. */
    void chooseFace(String address) {
        Path picked=DesktopPick.openOne(frame,"Choose a picture");if(picked==null)return;
        java.io.File file=picked.toFile();
        status.setText(Say.t("Making the picture…"));
        disk.submit(()->{DesktopIcons.Chosen chosen=DesktopIcons.read(file.toPath());byte[] thumb=DesktopIcons.thumb(chosen.picture(),chosen.turn());
            if(thumb==null)throw new IllegalArgumentException("That picture could not be made small enough to keep.");return thumb;},
            thumb->setFace(address,thumb),this::failed);
    }
    /** Kept, or taken off with null; yours sent to everybody paired; every round drawn again. */
    void setFace(String address,byte[] thumb) {
        disk.submit(()->{if(address==null)store.setMyFace(thumb);else store.setFaceHere(address,thumb);return null;},done->{
            status.setText(Say.t(thumb==null?"Picture removed":"Picture set"));
            DesktopFaces.forget(address);frame.repaint();
            if(address==null)network.submit(()->Post.sendMyFace(context,store,keys,true),sent->{},e->{});
        },this::failed);
    }

    /** Who, and the colour their writing is drawn in here, in a small box under their round. */
    void inks(Component round,String name,String address) {
        // One of your own devices writes as you: its colour is yours, set in Profile, said here rather than chosen (decision 185).
        inkRow(address,(you,row)->{
            JPanel inks=DesktopUi.column();
            if(you&&address!=null)DesktopUi.add(inks,DesktopUi.note(Say.t("One of your devices. It writes in your colour, which you set in Profile."),260,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
            else{DesktopUi.add(inks,DesktopUi.quiet(you?"Your colour, which everybody sees":"Their colour, on this PC"));DesktopUi.gap(inks,4);DesktopUi.add(inks,row);}
            JPanel words=DesktopUi.column();words.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
            DesktopUi.add(words,DesktopUi.body(name));DesktopUi.gap(words,8);DesktopUi.add(words,inks);
            JPopupMenu box=new JPopupMenu();box.setBackground(DesktopUi.CARD);box.add(words);
            if(round.isShowing())box.show(round,0,round.getHeight()+4);
        });
    }
    /** The round a person line from inkOnRound begins with. */
    static Component roundOf(JPanel person){return ((JPanel)person.getComponent(0)).getComponent(0);}
    /**
     * A round under the title, right-clicked, as the phone's is held: linking with somebody not linked yet, first;
     * their writing colour; who has the note; and, for somebody not linked, taking them off its list, last.
     */
    JPopupMenu roundMenu(SyncStatus.Person who,Component round) {
        JPopupMenu menu=new JPopupMenu();
        JMenuItem named=DesktopMenus.plain(who.called());named.setEnabled(false);menu.add(named);
        // Their picture in big, first (the owner, 2026-10-12: "when we click a contact we should see their picture in big"; decision 158).
        java.awt.image.BufferedImage face=who.linked()?DesktopFaces.of(this,who.address()):null;
        if(face!=null){JPanel big=new JPanel(new FlowLayout(FlowLayout.CENTER,12,8));big.setOpaque(false);big.add(DesktopFaces.big(face,200));menu.add(big);}
        menu.addSeparator();
        // Their page in People and devices: renaming them, their colour, Parlons!, groups, what they have (the owner, 2026-10-11).
        if(who.linked())item(menu,who.called()+"'s page…",()->DesktopPeople.person(this,who.address(),who.called()));
        if(!who.linked())item(menu,Unsent.link(who.called()),()->link(who.listing()));
        // Parlons! is on their page, not on a note's round (the owner, 2026-10-17).
        if(who.linked())item(menu,"Picture…",()->faceMenu(round,who.address(),who.called()));
        // Their colour, for somebody else; one of your own devices writes in yours, set in Profile, and has no line here.
        JMenuItem ink=DesktopMenus.item("Writing colour…");ink.addActionListener(a->personClicked(who,round));ink.setVisible(false);menu.add(ink);
        disk.submit(()->{NoteStore.Contact c=store.address(who.address());return c==null||!c.mine;},theirs->{ink.setVisible(theirs);if(menu.isVisible())menu.pack();},e->{});
        item(menu,"Sharing…",()->{if(base!=null){selected=openNote();save(this::share);}});
        if(!who.linked()&&who.listing()!=null){menu.addSeparator();item(menu,Unsent.takeOff(who.listedIn()),()->takeOff(who.called(),who.listedIn(),who.listing()));}
        return menu;
    }
    /**
     * After the rounds, a + that adds somebody to the open note, there whoever may: greyed, and saying so under the
     * pointer, for one who may not (the owner, 2026-10-05).
     */
    private JComponent addRound(boolean may) {
        JLabel plus=new JLabel(){@Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Color ink=may?DesktopUi.INK:DesktopUi.LINE.darker();int d=24,x=(getWidth()-d)/2,y=(getHeight()-d)/2;
            g2.setColor(ink);g2.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1f,new float[]{3f,2.5f},0f));g2.drawOval(x,y,d-1,d-1);
            g2.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.drawLine(x+d/2,y+7,x+d/2,y+d-8);g2.drawLine(x+7,y+d/2,x+d-8,y+d/2);g2.dispose();}};
        plus.setPreferredSize(new Dimension(30,26));
        plus.setToolTipText(Say.t(may?"Add someone":"Only the owner or an admin can add people"));
        plus.getAccessibleContext().setAccessibleName(may?"Add someone":"Add someone. Only the owner or an admin can add people.");
        if(may)plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plus.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){
            if(!SwingUtilities.isLeftMouseButton(e))return;
            if(!may){status.setText(Say.t("Only the owner or an admin can add people"));return;}
            if(base!=null){NoteStore.Branch note=openNote();save(()->people(note));}
        }});
        return plus;
    }
    void refreshStanding(){updateStanding();}
    private void updateStanding() {
        if(base==null)return;String id=base.id;
        disk.submit(()->{
            String shelf=shelfLine(store,id);
            int files=store.notAway(store.filesOf(NoteStore.Branch.Kind.PAGE,id)).size();SwingUtilities.invokeLater(()->{if(base!=null&&base.id.equals(id)){clip.setText(files==0?"":Integer.toString(files));if(!fileCards.shows(id,files))fileCards.show(id);}});
            // Who it reaches and where each stands, and the mark worked out from them as the phone does (SyncStatus.mark).
            List<SyncStatus.Person> who=SyncStatus.who(store,id);
            // And the colour each one's writing is drawn in, so a round says whose the coloured words are.
            Writers.Palette inks=store.palette();Map<String,Integer> colours=new HashMap<>();
            for(SyncStatus.Person one:who)colours.put(one.address(),inks.colourOf(store.writerOf(one.address())));
            // And what the pointer resting on each round says (decision 212): see roundDetails.
            Map<String,String> details=roundDetails(store,id,who);
            // And whose Parlons! address is known, for what a round offers (decision 101).
            parlonsBook=store.parlonsBook();
            return new Object[]{shelf+(Boolean.TRUE.equals(store.readOnlyHere(id)[0])?"  ·  "+Say.t("Read only"):""),who,SyncStatus.mark(store,id,who,false),colours,
                store.wornIcon(NoteStore.Branch.Kind.PAGE,id),store.wornImage(NoteStore.Branch.Kind.PAGE,id),
                // Whether this PC may add somebody, for the + after the rounds (the owner, 2026-10-05).
                !store.mayGive(Sharing.Scope.PAGE,id).isEmpty(),
                store.untilOf(NoteStore.Branch.Kind.PAGE,id)>0,details};
        },text->{if(base!=null&&base.id.equals(id)){stoodFor=id;
            // The way back says where it is: Temp for a note in Temp, and goes there.
            inTemp=(Boolean)text[7];
            backButton.setText(Say.t(inTemp?"Temp":"Home"));backButton.setToolTipText(Say.t(inTemp?"Back to Temp":"Back to Home")+" (Esc)");
            backButton.getAccessibleContext().setAccessibleName(Say.t(inTemp?"Back to Temp":"Back to Home"));
            standing.setText((String)text[0]);
            noteIcon=(String)text[4];noteImage=(byte[])text[5];noteFace.repaint();
            @SuppressWarnings("unchecked") List<SyncStatus.Person> who=(List<SyncStatus.Person>)text[1];
            @SuppressWarnings("unchecked") Map<String,Integer> colours=(Map<String,Integer>)text[3];
            peopleShown=who;roundInks=colours;DesktopMark.people(rounds,who,24,this::notLinked,(one,round)->roundMenu(one,round).show(round,0,round.getHeight()+4),this::roundInk);
            // Right-click on a round: the phone's hold on it (see roundMenu).
            @SuppressWarnings("unchecked") Map<String,String> details=(Map<String,String>)text[8];
            for(int i=0;i<who.size()&&i<rounds.getComponentCount();i++){SyncStatus.Person one=who.get(i);Component round=rounds.getComponent(i);
                DesktopMenus.onRightClick(round,e->roundMenu(one,round).show(e.getComponent(),e.getX(),e.getY()));
                if(round instanceof JComponent face&&details.containsKey(one.address()))face.setToolTipText(details.get(one.address()));}
            rounds.add(addRound((Boolean)text[6]));rounds.revalidate();
            // Unsaved words are waiting whatever the notebook says (a red mark stays red; see SyncMark.of). Only words:
            // a page that merely shows older text has nothing of its own to send.
            SyncMark mark=(SyncMark)text[2];
            showNoteMark(typedHere()||saving?waitingOver(mark):mark);}},this::failed);
    }
    /**
     * What the pointer resting on a round says (the owner, 2026-10-19: "when we mouse over these here, we should have the
     * basic details including their status on the note, sync and all"; decision 212): who, your device or a person, what
     * they may do with the note, where they stand with its writing, and how many of its files have not reached them. Shown
     * while the descriptions are on (My profile), as every line under the pointer is. On the disk thread.
     */
    static Map<String,String> roundDetails(NoteStore store,String note,List<SyncStatus.Person> who) {
        Map<String,String> out=new HashMap<>();
        List<String> path=new ArrayList<>(store.pathOf(note));path.add(0,Sharing.EVERYTHING);
        Map<String,Waits.Where> where=store.whereEach(store.pagesUnder(NoteStore.Branch.Kind.PAGE,note)).getOrDefault(note,Map.of());
        Map<String,Boolean> mine=Sharing.audience(store.shares(),store.pathOf(note));
        List<Sharing.Rule> rules=store.shares();
        for(SyncStatus.Person one:who) {
            Sharing.Level may=null;
            for(Sharing.Rule rule:rules)if(rule.address.equals(one.address())&&path.contains(rule.target)&&(may==null||rule.level.ordinal()>may.ordinal()))may=rule.level;
            StringBuilder said=new StringBuilder("<html><b>").append(esc(one.called())).append("</b><br>");
            said.append(esc(Say.t(Boolean.TRUE.equals(mine.get(one.address()))?"Your device":"A person")));
            if(may!=null&&may!=Sharing.Level.GONE)said.append("  ·  ").append(esc(Say.t(may==Sharing.Level.ADMIN?"Admin":may.writes()?"Can write":"Can read")));
            said.append("<br>").append(esc(Say.t(one.linked()?one.standing():one.notLinked(Post.here()))));
            Waits.Where at=where.get(one.address());
            if(at!=null&&at.files()>0)said.append("<br>").append(esc(at.lacking()==0?Say.t("All {0} files are with them",String.valueOf(at.files()))
                :Say.t("{0} of {1} files still going to them",String.valueOf(at.lacking()),String.valueOf(at.files()))));
            out.put(one.address(),said.append("</html>").toString());
        }
        return out;
    }
    private static String esc(String words){return words==null?"":words.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}

    /**
     * Where a note is, on the line over its title: every collection above it from the top down, however deep, as the
     * trail over the cards says it; Home for one on Home itself. On the disk thread.
     */
    static String shelfLine(NoteStore store,String note) {
        List<String> names=new ArrayList<>();
        // A note in Temp says so, as its tile is in Temp (the owner, 2026-10-14: "when I push + note in Temp, it saves it on
        // home"): it is kept on Home, its time running, and shown in Temp.
        if(store.untilOf(NoteStore.Branch.Kind.PAGE,note)>0)names.add(Say.t("Temp"));
        for(String one:store.above(note))names.add(store.collectionName(one));
        return names.isEmpty()?"Home":String.join("  /  ",names);
    }
    /** What the mark turns to when words are typed: waiting to go, unless it reaches nobody, is paused, or has gone wrong. */
    private static SyncMark waitingOver(SyncMark was) {
        return was==null||was==SyncMark.HERE||was==SyncMark.PAUSED||was==SyncMark.STUCK?was:SyncMark.WAITING;
    }
    /** The open note's mark drawn in a state, on a round of paper so a washed page does not swallow its colour. */
    private void showNoteMark(SyncMark what) {
        noteMark=what;syncMark.setIcon(new DesktopMark(what,20,true));
        syncMark.getAccessibleContext().setAccessibleName(DesktopMark.said(what));syncMark.setVisible(true);
    }
    /**
     * The open note's syncing said on its own line, after the mark and the people, in the mark's colour; gone a few
     * seconds later unless it is still going on (NoteLine.stays). Null words clear it.
     *
     * @param click what a click on the words does; null for what a click on the mark does
     */
    void noteSays(String words,NoteLine.Tone tone,Runnable click) {
        wordsAway.stop();
        boolean said=words!=null&&!words.isBlank()&&tone!=null;
        noteWordsSaid=said?words:"";noteWords.setText(said?Say.t(words):"");noteWords.setVisible(said);wordsClicked=said?click:null;
        if(!said)return;
        noteWords.setForeground(DesktopMark.ink(tone.mark));noteWords.getAccessibleContext().setAccessibleName(Say.t(words));
        // Something that could not go stays until clicked or the next send goes, as on the phone.
        if(!NoteLine.stays(words)&&tone!=NoteLine.Tone.FAILED)wordsAway.restart();
    }
    /** Whether a thing is the note open on the page now, whose line its syncing is said on; anything else uses the bar. */
    private boolean isOpen(String id){return base!=null&&base.id.equals(id)&&editorPanel!=null&&editorPanel.isVisible();}
    /** Who the open note reaches by name, as its rounds were last drawn: those it can be sent to. */
    private List<String> namesShown() {
        List<String> names=new ArrayList<>();
        for(SyncStatus.Person one:peopleShown)if(one.linked()&&!names.contains(one.called()))names.add(one.called());
        return names;
    }
    /**
     * How a send of the open note ended, on its line: sent, or what could not go with a click for why.
     *
     * @param asked whether the others were asked for theirs too, so that nothing to send is still an answer worth saying
     */
    private void noteSent(Post.Done done,boolean asked) {
        if(done.failed>0)noteSays(NoteLine.couldNot(done.problems,done.sent),NoteLine.couldNotTone(done.problems),()->tellUnsent(done));
        else if(done.sent>0)noteSays(NoteLine.sent(namesShown()),NoteLine.Tone.DONE,null);
        else if(asked)noteSays(NoteLine.NOTHING,NoteLine.Tone.DONE,null);
        // Nothing went by itself: the "Sending…" that went ahead of it is taken down, and new typing is left saying so.
        else if(NoteLine.isSending(noteWordsSaid))noteSays(null,null,null);
    }
    /** A send of the open note that threw: on its line, with the words it threw with behind the click. */
    private void noteFailed(Exception e) {
        String why=e.getMessage()==null?"That did not finish. Please try again.":e.getMessage();
        noteSays("Could not go. See why",NoteLine.Tone.FAILED,()->tellUnsent(new Post.Done(0,1,why)));
    }
    /** The open note, as the thing its mark and its menus are about. */
    private NoteStore.Branch openNote() {
        NoteStore.Branch known=find(base.id);if(known!=null)return known;
        return new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,base.id,base.book,base.title==null||base.title.isBlank()?"this note":base.title,"",0,0,false,base.colour);
    }
    /**
     * A mark clicked, as the phone's is tapped: waiting says what waits and for whom, with Send now as the one thing
     * to press (see Waits); anything else shows who has it.
     */
    /** How many things are new, said at the top: "new", or "new · 3"; nothing while none is. */
    void newHere(int many) {
        newButton.setText(many>1?Say.t("new")+" · "+many:Say.t("new"));newButton.setVisible(many>0);
        newButton.setToolTipText(many==1?Say.t("One thing is new. Click to see it."):Say.t("{0} things are new. Click to see them.",many));
        newButton.getAccessibleContext().setAccessibleName(newButton.getToolTipText());
        rebar();
    }
    /** The bar measured again once something in it shows or goes, or the row wraps under the brand. */
    private void rebar() {
        if(barPanel==null)return;
        for(Component c=newButton.getParent();c!=null&&c!=barPanel;c=c.getParent())c.invalidate();
        barPanel.revalidate();barPanel.repaint();
    }
    /** What is new here, each a click from being opened, which is what makes it not new (the owner, 2026-10-14). */
    /**
     * New: what arrived or changed and was not opened yet, each with what it is, who sent or changed it, when, and where it
     * is, and what can be done with it, open it, show where it is, or mark it seen (the owner, 2026-10-17: "the New window
     * must be better designed and more informative, now the user doesn't really have any input"; decision 187).
     */
    void whatIsNew() {
        disk.submit(()->{
            List<Object[]> out=new ArrayList<>();
            for(NoteStore.Branch one:store.whatIsNew()) {
                String who="",kind;long when=0;
                if(one.kind==NoteStore.Branch.Kind.FILE) {
                    NoteStore.Held held=store.file(one.id);
                    kind=held==null?Say.t("File"):Say.t("File")+"  ·  "+Attachment.size(held.bytes);
                    NoteStore.Standing standing=store.standing(one.id);
                    if(standing!=null&&!standing.from.isEmpty()){who=standing.fromName;when=standing.at;}
                    else if(held!=null)when=held.added;
                } else {
                    kind=Say.t("Note");
                    NoteStore.Note n=store.get(one.id);
                    if(n!=null){when=n.updated;
                        if(!n.origin.isEmpty())who=store.nameFor(n.origin);}
                }
                out.add(new Object[]{one,kind,who,when,whereItIs(one)});
            }
            return out;
        },items->{
            JPanel body=DesktopUi.column();JDialog[] box={null};
            if(items.isEmpty())DesktopUi.add(body,DesktopUi.note(Say.t("Nothing is new.")));
            else DesktopUi.add(body,DesktopUi.spread(Say.t("What arrived or changed since you last looked. Open it, see where it is, or mark it seen.")));
            List<NoteStore.Branch> all=new ArrayList<>();
            for(Object[] item:items) {
                NoteStore.Branch one=(NoteStore.Branch)item[0];all.add(one);
                @SuppressWarnings("unchecked") List<String[]> where=(List<String[]>)item[4];
                StringBuilder place=new StringBuilder(Say.t("Home"));for(String[] step:where)place.append(" › ").append(step[1]);
                StringBuilder said=new StringBuilder((String)item[1]);
                if(!((String)item[2]).isEmpty())said.append("  ·  ").append(Say.t("from {0}",(String)item[2]));
                if((Long)item[3]>0)said.append("  ·  ").append(shortWhen((Long)item[3]));
                JPanel card=DesktopUi.column();
                // Its face with room around it, so its edge is never cut by the space it is drawn in (the owner, 2026-10-19: "make
                // sure the left edge of the icon is visible here").
                JLabel face=new JLabel(new Icon(){public int getIconWidth(){return 38;}public int getIconHeight(){return 38;}
                    public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);DesktopHome.face(g2,one,x+2,y+2,34,60);g2.dispose();}});
                JPanel words=DesktopUi.column();
                DesktopUi.add(words,DesktopUi.body(one.name==null||one.name.isBlank()?Say.t("Untitled"):DesktopMenus.shortly(one.name)));
                DesktopUi.add(words,DesktopUi.note(said.toString(),330,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
                DesktopUi.add(words,DesktopUi.note(place.toString(),330,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
                JPanel top=new JPanel(new BorderLayout(12,0));top.setOpaque(false);top.add(face,BorderLayout.WEST);top.add(words);
                DesktopUi.add(card,top);DesktopUi.gap(card,6);
                JButton seen=DesktopUi.button(Say.t("Mark as seen"),null);
                seen.addActionListener(e->disk.submit(()->{markSeen(one);return null;},done->{seen.setEnabled(false);seen.setText(Say.t("Seen"));refresh();},this::failed));
                DesktopUi.add(card,DesktopUi.actions(DesktopUi.primary(Say.t("Open"),()->{box[0].dispose();openThing(one);}),
                    DesktopUi.button(Say.t("Show where it is"),()->{box[0].dispose();goWhereItIs(where);}),seen));
                DesktopUi.add(body,DesktopUi.card(null,card));DesktopUi.gap(body,8);
            }
            JButton allSeen=DesktopUi.button(Say.t("Mark all as seen"),()->disk.submit(()->{for(NoteStore.Branch one:all)markSeen(one);return null;},done->{box[0].dispose();refresh();status.setText(Say.t("Nothing is new."));},this::failed));
            allSeen.setEnabled(!items.isEmpty());
            box[0]=DesktopUi.sheet(frame,"New",DesktopUi.scrolling(body),DesktopUi.footer(allSeen),true);DesktopUi.show(box[0],600,620);
        },this::failed);
    }
    /** No longer new: a file as opened, a note as seen. On the disk thread. */
    private void markSeen(NoteStore.Branch one){if(one.kind==NoteStore.Branch.Kind.FILE)store.opened(one.id);else store.seen(one.id);}
    /** One thing in a list, a click from being opened where it lives. */
    private JComponent opening(NoteStore.Branch thing,String what,Runnable close) {
        JButton line=new JButton((thing.name==null||thing.name.isBlank()?Say.t("Untitled"):DesktopMenus.shortly(thing.name))+"   ·   "+Say.t(what));
        line.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        line.setHorizontalAlignment(SwingConstants.LEFT);line.setFont(DesktopUi.BODY);line.setFocusPainted(false);
        line.addActionListener(e->{close.run();openThing(thing);});
        return line;
    }
    void markClicked(NoteStore.Branch thing,SyncMark what) {
        selected=thing;
        // Temp has no Share box: its mark is Sync now (the owner, 2026-10-14: "an arrow there to sync now").
        if(what!=SyncMark.WAITING&&thing.kind==NoteStore.Branch.Kind.TEMP){save(()->{selected=thing;sync(true);});return;}
        if(what!=SyncMark.WAITING){save(this::share);return;}
        save(()->disk.submit(()->new Object[]{SyncStatus.waits(store,thing.kind,thing.id),SyncStatus.waitingThings(store,thing.kind,thing.id)},got->{
            String said=(String)got[0];
            @SuppressWarnings("unchecked") List<NoteStore.Branch> things=(List<NoteStore.Branch>)got[1];
            // Who it waits for, then what, each a click from being opened (the owner, 2026-10-14: "clicking the yellow arrow
            // showing that something is not fully synced should give us the list of what they are").
            JPanel body=DesktopUi.column();JDialog[] box={null};
            DesktopUi.add(body,DesktopUi.note(said,380,INK,DesktopUi.BODY));
            if(!things.isEmpty()){DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.quiet(Say.t("What waits")));}
            for(NoteStore.Branch one:things)DesktopUi.add(body,opening(one,one.kind==NoteStore.Branch.Kind.FILE?"File":"Note",()->box[0].dispose()));
            // What waits only for a device to be updated cannot go any sooner for a press: said, with nothing to press.
            JComponent foot=Looks.onlyUpdates(said)?DesktopUi.footer():DesktopUi.footer(DesktopUi.primary(Waits.SEND,()->{box[0].dispose();selected=thing;sync(true);}));
            box[0]=DesktopUi.sheet(frame,Waits.TITLE,body,foot,true);DesktopUi.show(box[0],440,560);
        },this::failed));
    }
    void open(String id){
        int request=++noteRequest;
        if(base!=null&&!base.id.equals(id))sendOnLeaving(base.id);
        // Opened: what Recent lists (decision 72).
        disk.submit(()->{store.touch(NoteStore.Branch.Kind.PAGE,id);return null;},done->openList.refresh(),e->{});
        disk.submit(()->store.get(id),note->{
            if(request!=noteRequest||note==null)return;
            if(dirty||saving){if(afterSave==null)save(()->open(id));return;}
            display(note);
        },this::failed);
    }
    /**
     * A new note: in the open card's collection; else beside the open note, in the collection it is in; else on Home -
     * where the reader is. Ctrl+N and the menu's New note make it here; the + makes it where the + is.
     */
    private void newNote() {
        NoteStore.Branch card=home.folder.shown();
        newNoteIn(card!=null&&!onPage?card.id:onPage&&base!=null?(NoteStore.home(base.book)?Things.HOME:base.book):Things.HOME);
    }
    /** A new note in this collection, or on Home, opened at once to be written on. */
    void newNoteIn(String asked) {
        int request=++noteRequest;
        // From a place's +: made on Home and put in the place (decision 87).
        String place=takesNew(asked)?asked:null,where=place!=null?Things.HOME:asked;
        save(()->disk.submit(()->{
            NoteStore.Note note=new NoteStore.Note();
            note.book=NoteStore.home(where)?Things.HOME:where;
            if(!NoteStore.home(where)&&Boolean.FALSE.equals(store.mayWriteIn(NoteStore.Branch.Kind.COLLECTION,where)))throw new IllegalStateException("This folder is read only here. Ask whoever shared it to let you write in it.");
            store.save(note);if(place!=null)intoPlaceNow(NoteStore.Branch.Kind.PAGE,note.id,place);return note;
        },note->{if(home!=null&&NoteStore.home(where)&&shownOnHome(place,store.tempOnHome()))home.placeNew(note.id);if(request!=noteRequest)return;if(dirty||saving){if(afterSave==null)save(()->open(note.id));return;}display(note);refresh();},this::failed));
    }
    /**
     * Drawn again half a second after the first of what arrived together (Graphene, 2026-10-17: twenty answers in two
     * seconds each drew Home whole), with all of it.
     */
    void refreshSoon(){if(!refreshing.isRunning())refreshing.start();}
    private final javax.swing.Timer refreshing=later(500,()->{if(!closing)refresh();});
    private static javax.swing.Timer later(int ms,Runnable then){javax.swing.Timer t=new javax.swing.Timer(ms,e->then.run());t.setRepeats(false);return t;}
    /**
     * Everything on the screen read again. While a note is in front, Home under it is read again only once it is shown, and
     * the tree is asked for once however many come before it is read (the owner, 2026-10-18, writing in a shared note whose
     * files were still going: "there is a lag, the note is not really responsive"; each read of the whole tree and of Home
     * went on the one disk thread the note's saves wait on).
     */
    void refresh() {
        if(!treeAsked){treeAsked=true;search("");}
        updateStanding();
        if(home!=null){if(onPage)home.later();else home.refresh();}
        if(onDisk!=null)onDisk.soon();pinnedMenu.soon();if(troubles!=null)troubles.soon();
    }
    /** A whole-tree read asked for and not begun yet: another refresh before it begins needs no read of its own. */
    private volatile boolean treeAsked;
    /** A note written by something other than the page (its file, changed outside: decision 162) goes as a typed one does. */
    void sendSoon(String id) {
        if(offline||id==null)return;
        disk.submit(()->store.pauseFor(NoteStore.Branch.Kind.PAGE,id),seconds->{if(seconds>=0)due.put(id,System.currentTimeMillis()+Math.max(1,seconds)*1000L);},this::failed);
    }
    /** The open note, where it is this one, follows what the notebook now says of it (see changedUnderneath). */
    void follow(String id){changedUnderneath(id);}

    /** Whether a note has anything in it: a blank page nothing was written on is not one of the things open. */
    private static boolean written(NoteStore.Note note){return note!=null&&!(Objects.toString(note.title,"").isBlank()&&Objects.toString(note.body,"").isBlank());}

    /** Where the window is, kept for the next time it opens: Home, or a note. */
    private void keepWhere(String where){disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString(WHERE,where).apply();return null;},done->{},e->{});}

    /**
     * Home on the screen, as it was left: a card that was open under the note is open again, and the grid is where it was
     * scrolled to. The note is not closed by it - it stays one of the things open, a Ctrl+Tab away.
     */
    void showHome() {
        overview.close();
        if(onPage&&base!=null)carets.put(base.id,page.getCaretPosition());
        if(onPage&&base!=null)sendOnLeaving(base.id);
        boolean was=onPage;onPage=false;selected=null;pinnedMenu.soon();
        ((CardLayout)mainArea.getLayout()).show(mainArea,"home");keepWhere(HOME_SHOWN);
        if(was)home.focusThing(base==null?null:base.id);
        home.refreshIfStale();
        openList.refresh();
    }

    /** The open thing in front: the note on the page, or the collection whose card is up on Home; null for Home alone. */
    Overview.Open inFront() {
        if(onPage&&base!=null)return new Overview.Open(Overview.Kind.NOTE,base.id);
        NoteStore.Branch card=home==null||!home.folder.isOpen()?null:home.folder.shown();
        return card==null?null:new Overview.Open(Overview.Kind.COLLECTION,card.id);
    }
    /** Whether the list of what is open is shown (Settings, ⋯): on unless switched off. */
    static final String OPEN_LIST="openList";
    boolean openListWanted(){return !"false".equals(context.getSharedPreferences("settings",0).getString(OPEN_LIST,"true"));}
    void setOpenListWanted(boolean on){context.getSharedPreferences("settings",0).edit().putString(OPEN_LIST,String.valueOf(on)).apply();openList.refresh();}
    /** ← Home, or Esc on a note: what was typed written down first, then Home. */
    void goHome(){save(()->{showHome();
        // A note in Temp goes back to Temp's card, where it is shown.
        if(inTemp&&!home.folder.isOpen()){home.folder.openPlace(DesktopHome.tempPlace(0));return;}
        if(!home.folder.isOpen()&&base!=null)home.focusThing(base.id);});}
    /** Whether the open note is in Temp: what its way back says and where it leads. */
    private boolean inTemp;

    /** A collection, from anywhere: its card over Home, however deep it is. */
    void openCollection(String id){save(()->{showHome();home.folder.openAt(id);});}

    /** A thing chosen from a list - the search, the dock, the tree: a note opens, a collection its card, a file itself. */
    /** Words looked for in the open note: see DesktopFindBar (decision 216). */
    DesktopFindBar findBar;

    /**
     * A thing found by a search, opened: a note at the first place the words are, with the bar to go through every place
     * they are (decision 216); anything else as it opens.
     */
    void openFound(NoteStore.Branch thing,String term) {
        if(thing.kind!=NoteStore.Branch.Kind.PAGE||term==null||term.isBlank()){openThing(thing);return;}
        save(()->{
            open(thing.id);
            // Once the note is on the page, which it is a moment after open: then the bar, on the words.
            javax.swing.Timer wait=new javax.swing.Timer(80,null);long until=System.currentTimeMillis()+4000;
            wait.addActionListener(e->{
                if(base!=null&&base.id.equals(thing.id)&&onPage&&page.getDocument().getLength()>0){wait.stop();findBar.show(term);}
                else if(System.currentTimeMillis()>until)wait.stop();
            });
            wait.start();
        });
    }

    void openThing(NoteStore.Branch thing) {
        if(thing.kind==NoteStore.Branch.Kind.PAGE)save(()->open(thing.id));
        else if(thing.kind==NoteStore.Branch.Kind.FILE)home.openFile(thing);
        else if(thing.kind==NoteStore.Branch.Kind.FAVOURITES)save(()->{showHome();home.folder.openFavourites();});
        else if(isCollection(thing))openCollection(thing.id);
    }

    /** A card in the overview chosen: gone to, as it was opened. */
    void goTo(Overview.Open one){if(one.kind==Overview.Kind.NOTE)save(()->open(one.id));else openCollection(one.id);}

    /** Whether this open thing is what is on the screen now: the note on the page, or the collection whose card is up. */
    boolean showingOpen(Overview.Open one) {
        if(one.kind==Overview.Kind.NOTE)return onPage&&base!=null&&base.id.equals(one.id);
        NoteStore.Branch card=home.folder.shown();return !onPage&&card!=null&&card.id.equals(one.id);
    }

    /** What the overview lies over: the tree, and Home or the page - under the toolbar, over the status bar. */
    JComponent overviewArea(){return split;}

    /** One card closed in the overview: if it is what is on the screen, Home is what is left. */
    void closed(Overview.Open one) {
        if(!showingOpen(one))return;
        if(one.kind==Overview.Kind.NOTE)goHome();else home.folder.close();
    }
    /** Close all: nothing is open, so Home is what is on the screen, with no card over it. */

    /** A note moved from Home or a card: the page's own copy says where it is now, as the line over its title does. */
    void movedNote(NoteStore.Branch moved,String into){if(moved.kind==NoteStore.Branch.Kind.PAGE&&base!=null&&base.id.equals(moved.id))base.book=NoteStore.home(into)?Things.HOME:into;}

    /** Moved into something somebody else reads, or put together with it: it goes now, as sharing does. */
    void sendChanged(NoteStore.Branch.Kind kind,String id){if(!offline)network.submit(()->{Post.changed(context,store,keys,kind,id);return null;},v->refresh(),e->{});}

    /**
     * Esc, where nothing nearer took it: a menu that is up closes first, then the overview; on a note, back to Home
     * (decision 24); on Home, out of the card a level at a time, then the search emptied.
     */
    private void escape() {
        if(home!=null&&home.carry.carrying()){home.carry.cancel();return;}
        if(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath().length>0){javax.swing.MenuSelectionManager.defaultManager().clearSelectedPath();return;}
        // A picture shown large closes before the album under it (decision 217).
        if(DesktopAlbum.closeViewer())return;
        if(overview.showing()){overview.close();return;}
        if(onPage){goHome();return;}
        home.escape();
    }
    private void search(String term) {
        int request=++treeRequest;
        disk.submit(()->{if(term.isBlank())treeAsked=false;List<NoteStore.Branch> found=term.isBlank()?store.wholeTree():store.looking(term);
            // The favourites wear what they wear on Home, as the lines under them do.
            List<NoteStore.Branch> starred=term.isBlank()?store.favourites():List.of();store.dress(starred);
            STARRED=Set.copyOf(store.keptIds());favouritesNow=starred;rungsNow=store.rungs();plainNow=store.plain();
            if(term.isBlank())try{marks=DesktopMark.read(store);}catch(Exception unread){/* the lines, without their marks this time */}
            return found;},rows->{
            if(request!=treeRequest)return;
            if(term.isBlank()){everything=rows;treeDrawn=true;}
            // One line at the top: Home, holding everything on the shelves, Favourites first inside it, as on the phone.
            drawing=true;DefaultMutableTreeNode top=new DefaultMutableTreeNode(""),root=new DefaultMutableTreeNode("Home");top.add(root);
            Map<String,DefaultMutableTreeNode> nodes=new HashMap<>();
            nodes.put(Sharing.EVERYTHING,root);
            for(NoteStore.Branch branch:rows) {
                if(branch.kind==NoteStore.Branch.Kind.LIBRARY)continue;
                DefaultMutableTreeNode node=new DefaultMutableTreeNode(new Item(branch));nodes.put(branch.id,node);
                nodes.getOrDefault(branch.parent,root).add(node);
            }
            // Favourites first, as on the phone: what is starred, where it really lives one click away.
            if(!favouritesNow.isEmpty()) {
                DefaultMutableTreeNode starred=new DefaultMutableTreeNode(new Item(new NoteStore.Branch(NoteStore.Branch.Kind.FAVOURITES,NoteStore.FAVOURITES,"","Favourites","",0,0,true)));
                for(NoteStore.Branch one:favouritesNow)starred.add(new DefaultMutableTreeNode(new Item(one)));
                root.insert(starred,0);
            }
            tree.setModel(new DefaultTreeModel(top));for(int i=0;i<tree.getRowCount();i++)tree.expandRow(i);drawing=false;
            // Colours are read with the tree, so the page is washed again with whatever it now says.
            if(term.isBlank())washPage();
        },this::failed);
    }
    /**
     * A place's colour - Favourites, Recent, Temp, the archive, the bin - chosen as a collection's is (the owner, 2026-10-03:
     * "all groups including these technical ones ... should have a choice of colour too"; decision 81). Kept on this PC, as
     * Home's own colour is: a place is nobody else's. And how strongly it lands, beside it ("tone_" and its id), the
     * place's own or the usual (decision 107).
     */
    int placeColour(String id){try{return Integer.parseInt(context.getSharedPreferences("settings",0).getString("colour_"+id,String.valueOf(Tint.NONE)));}catch(RuntimeException unread){return Tint.NONE;}}
    void setPlaceColour(String id,int colour) {
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString("colour_"+id,String.valueOf(colour)).apply();return null;},
            done->{home.refresh();home.folder.painted();},this::failed);
    }
    int placeTone(String id){return DesktopLook.tone(context,"tone_"+id);}
    /** A place's strength, kept at once so the band that set it reads it back, and the place drawn again wherever it shows. */
    void setPlaceTone(String id,int step) {
        DesktopLook.keepTone(context,"tone_"+id,step);
        home.refresh();home.folder.painted();
    }
    JMenu placeColourMenu(NoteStore.Branch place){return DesktopLook.colours(placeColour(place.id),colour->setPlaceColour(place.id,colour),()->Tint.tone(placeTone(place.id),usual),step->setPlaceTone(place.id,step));}
    /** Colour ▸ for one thing, its colour and its strength as the tree last read them from the notebook. */
    private JMenu colourMenu(NoteStore.Branch thing) {
        NoteStore.Branch known=find(thing.id);boolean open=thing.kind==NoteStore.Branch.Kind.PAGE&&base!=null&&base.id.equals(thing.id);
        int now=known!=null?known.colour:open?base.colour:thing.colour;
        int[] own={known!=null?known.tone:open?base.tone:thing.tone};
        return DesktopLook.colours(now,colour->paint(thing,colour),()->Tint.tone(own[0],usual),step->{own[0]=step;tone(thing,step);});
    }
    /** The strength a thing's colour lands at: its own where it was given one, the usual where it was not (decision 107). */
    int toneOf(NoteStore.Branch thing){return Tint.tone(thing==null?Tint.USUAL:thing.tone,usual);}
    /** A colour given to one thing: kept, and the tree, the cards and the page drawn in it at once. */
    void paint(NoteStore.Branch thing,int colour) {
        if(thing.kind==NoteStore.Branch.Kind.PAGE&&base!=null&&base.id.equals(thing.id))base.colour=colour;
        disk.submit(()->{store.paint(thing.kind,thing.id,colour);return null;},done->{
            // Home, the card and the tree are drawn from the notebook, so they are drawn again once it has been read.
            refresh();overview.again();if(openList!=null)openList.refresh();disk.submit(()->null,read->washPage(),e->{});
            lookSent(thing);
        },this::failed);
    }
    /**
     * A thing's colour or strength decided here goes to everybody who has it, as its icon does (the owner, 2026-10-06: "when
     * sharing everything should travel, then on the other device it can be set individually"; decision 108). Quietly: the
     * marks say how it went.
     */
    private void lookSent(NoteStore.Branch thing) {
        if(offline)return;
        network.submit(()->Post.send(context,store,keys,thing.kind,thing.id),done->refresh(),e->{});
    }
    /**
     * How strongly one thing's colour lands, and that thing's only (the owner, 2026-10-06: "the color intensity applies to
     * the whole app, it should be specific to the elements selected"; decision 107). Its line of the tree, its page and
     * its face are drawn at it at once, as the hand moves; Home and the cards once the notebook has it. Sent to whoever has
     * the thing, as its colour is (decision 108).
     */
    void tone(NoteStore.Branch thing,int step) {
        if(base!=null&&base.id.equals(thing.id))base.tone=step;
        NoteStore.Branch known=find(thing.id);if(known!=null)known.tone=step;
        thing.tone=step;
        tree.repaint();washPage();noteFace.repaint();if(home!=null){home.repaint();home.folder.toned(thing.id,step);}
        disk.submit(()->{store.tone(thing.kind,thing.id,step);return null;},done->{refresh();overview.again();if(openList!=null)openList.refresh();lookSent(thing);},this::failed);
    }
    /**
     * The page washed in the colour of the note that is open, as on the phone. A collection's card is washed in its own
     * colour where it is drawn (see DesktopFolder); Home itself is plain paper.
     */
    void washPage() {
        // The tree is read after every change, a backup's included, so it is asked first; the note itself if it is not there.
        NoteStore.Branch open=base!=null?find(base.id):null;
        int own=open!=null?open.colour:base!=null?base.colour:Tint.NONE;
        // At its own strength (decision 107), which the tree read with its colour.
        int tone=Tint.tone(open!=null?open.tone:base!=null?base.tone:Tint.USUAL,usual);
        // A note with no colour of its own is in the app's, at the app's strength (see paintApp).
        if(!Tint.known(own)){own=homeColour();tone=homeTone();}
        Color paper=DesktopLook.wash(own,PAPER,0.12f,0.72f,tone);
        if(editorPanel!=null)editorPanel.setBackground(paper);
        page.setBackground(paper);if(paperScroll!=null)paperScroll.getViewport().setBackground(paper);
        // The file drawer slides up over the page, so it is the same paper.
        if(fileCards!=null)fileCards.setBackground(paper);
        page.rules=DesktopLook.wash(own,Paper.RULES,0.35f,1f,tone);
        page.repaint();
    }
    /** The Home line: the first under the tree's top, which is not drawn. */
    private static boolean allCollections(DefaultMutableTreeNode node) {
        return node.getParent()!=null&&node.getParent().getParent()==null&&!(node.getUserObject() instanceof Item);
    }
    /** What can be done to one thing in the tree, where it is. */
    private void shelfMenu(MouseEvent e) {
        TreePath path=tree.getPathForLocation(e.getX(),e.getY());if(path==null)return;
        rowMenu(path,e.getX(),e.getY());
    }
    /** A line of the tree, chosen first: the top offers what can be made there; Favourites is a place, with none. */
    private void rowMenu(TreePath path,int x,int y) {
        tree.setSelectionPath(path);
        DefaultMutableTreeNode node=(DefaultMutableTreeNode)path.getLastPathComponent();
        if(allCollections(node)){roomMenu(library(),tree,x,y);return;}
        if(!(node.getUserObject() instanceof Item item)||item.branch.kind==NoteStore.Branch.Kind.FAVOURITES)return;
        shelfMenu(item.branch,tree,x,y);
    }
    void shelfMenu(NoteStore.Branch branch,Component where,int x,int y){thingMenu(branch).show(where,x,y);}
    /**
     * Everything that can be done to one thing, made in one place so it is the same menu wherever it is opened: its
     * icon, its card, its dock icon, its line in the tree. In the phone's order (MainActivity.menuFor): how it looks,
     * then Undo, then what is done to it - its versions or a file for it, moving it, sharing it and syncing it, keeping
     * it to hand, putting it away - and last, its words taken elsewhere. Rename and a note's files are the PC's own,
     * where the phone renames where the name is written and keeps the clip on the page.
     */
    JPopupMenu thingMenu(NoteStore.Branch branch) {
        selected=branch;
        JPopupMenu menu=new JPopupMenu();
        boolean note=branch.kind==NoteStore.Branch.Kind.PAGE;
        // The top every menu has, in the same order (decision 185): its colour and icon, then syncing and sharing.
        menu.add(colourMenu(branch));
        if(note||isCollection(branch))lookRows(menu,branch);
        menu.addSeparator();
        heading(menu,"Sync and sharing");
        menu.add(syncRow(branch));
        item(menu,"Sync parameters…",()->{selected=branch;save(this::share);});
        JMenuItem share=DesktopMenus.item("Share…");share.addActionListener(a->{selected=branch;save(this::share);});menu.add(share);
        menu.addSeparator();
        undoRow(menu);
        JMenuItem rename=DesktopMenus.item("Rename…");rename.setAccelerator(KeyStroke.getKeyStroke("F2"));rename.addActionListener(a->rename(branch));menu.add(rename);
        // A note's own: what the page's menus offer for the open one, here for any note - opened first if it is not.
        if(note) {
            boolean readOnly=isOpen(branch.id)&&!page.isEditable();
            // Its text size here, the one place it is set for one note (the ⋯ menu's top sets it for the open one).
            menu.add(sizeMenu(branch));menu.add(linesItem(branch));
            JMenuItem attach=DesktopMenus.item("Attach a file…");attach.setEnabled(!readOnly);attach.addActionListener(a->save(()->onNote(branch,true,this::attach)));menu.add(attach);
            JMenuItem record=DesktopMenus.item("Record audio");record.setEnabled(!readOnly&&!recorder.recording());record.addActionListener(a->save(()->onNote(branch,true,this::record)));menu.add(record);
        }
        // A collection keeps files as a note does: added from here, or dropped on its card (decision 23).
        if(isCollection(branch))item(menu,"Add a file…",()->addFiles(branch));
        // Its pictures in a grid, to browse them large, or its icons as any folder's: this PC's view of it, which does not
        // travel (the owner, 2026-10-19: "a type of folder specific for photos"; decision 217).
        if(isCollection(branch)) {
            if(store.album(branch.id))item(menu,"Add photos…",()->addPhotos(branch.id,named(branch)));
            menu.add(ticked("Show as an album",store.album(branch.id),()->store.album(branch.id),()->{store.showAsAlbum(branch.id,!store.album(branch.id));refresh();}));
        }
        if(note||isCollection(branch)) {
            // Any collection moves too now, onto Home or into another: collections nest (docs/HOME.md, decision 14).
            JMenuItem move=DesktopMenus.item("Move to…");move.addActionListener(a->save(()->moveTo(branch)));menu.add(move);
            boolean starred=STARRED.contains(branch.id);
            JMenuItem star=DesktopMenus.item(starred?"Remove from favourites":"Add to favourites");star.addActionListener(a->favourite(branch,!starred));menu.add(star);
            // Under it the dock, on its own (the owner, 2026-10-18: "we need a button, add to the dock and this should be
            // independent of add the favourites"; decision 190).
            if(home!=null&&home.inDock(branch.id))item(menu,"Remove from the dock",()->undock(branch));
            else item(menu,"Add to the dock",()->dock(branch));
        }
        // Its words, somewhere else: the phone's Send to another app, here the clipboard; then its past.
        if(note){item(menu,"Copy the text",()->save(()->onNote(branch,false,this::copyNote)));
            // Into another language on this PC (docs/TRANSLATION.md).
            item(menu,"Translate…",()->save(()->onNote(branch,false,()->DesktopTranslate.note(this))));
            item(menu,"Versions…",()->save(()->onNote(branch,false,this::versions)));
            // Its words as they are now, on Minima: one line, one window (decisions 199 and 253).
            DesktopStamp.menuLine(this,menu,branch.id,()->save(()->DesktopStamp.thing(this,branch.id,true)));}
        // Kept as a file too, or as a folder on disk (docs/FILES.md, decision 162).
        DesktopOnDisk.rows(this,menu,branch);
        // Put away, last, as in every menu: for a while, in the archive, or in the bin.
        if(note||isCollection(branch)) {
            menu.addSeparator();
            JMenuItem temporary=DesktopMenus.item("Temporary…");temporary.addActionListener(a->save(()->temporaryBox(branch)));menu.add(temporary);
            JMenuItem archive=DesktopMenus.item("Archive");archive.addActionListener(a->save(()->putAway(branch,false)));menu.add(archive);
            JMenuItem bin=DesktopMenus.item("Move to bin");bin.addActionListener(a->save(()->putAway(branch,true)));menu.add(bin);
        }
        return menu;
    }
    /**
     * Icon…, right after Colour on both apps (decision 33): the box its icon is chosen in. And Remove the picture under it,
     * only while it wears one: the icon under the picture is worn again.
     */
    private void lookRows(JPopupMenu menu,NoteStore.Branch thing) {
        item(menu,"Icon…",()->DesktopIconPicker.open(this,thing));
        if(pictured(thing))item(menu,"Remove the picture",()->DesktopIconPicker.removePicture(this,thing));
    }
    /** Whether a thing wears a picture, as it was listed or as the tree last read it: a hand-made line knows nothing of it. */
    private boolean pictured(NoteStore.Branch thing) {
        if(thing.image!=null)return true;
        if(base!=null&&base.id.equals(thing.id)&&noteImage!=null)return true;
        NoteStore.Branch known=find(thing.id);return known!=null&&known.image!=null;
    }
    /**
     * A note's or a collection's icon or picture, changed here: drawn again everywhere, and sent to whoever has it now - a
     * note does not go on its own, and a collection's carton would otherwise wait for the next round. Quietly, as every
     * automatic send is: the mark says how it goes.
     */
    void lookChanged(NoteStore.Branch.Kind kind,String id) {
        refresh();
        if(!offline)network.submit(()->Post.send(context,store,keys,kind,id),done->refresh(),e->{});
    }
    /**
     * Taken out of the dock: still a favourite, in the Favourites collection, and the rest of the dock closes up. Any icon
     * the dock draws: one of the notebook's own five leaves them, and none taken out comes back into the room this PC's
     * dock has past them (see DesktopHome.docked).
     */
    void undock(NoteStore.Branch thing) {
        disk.submit(()->{store.outOfDock(thing.kind,thing.id);return null;},
            done->{status.setText(Say.t("Out of the dock"));refresh();},this::failed);
    }
    /** At the end of the dock, a favourite or not as it was (decision 190). */
    void dock(NoteStore.Branch thing) {
        disk.submit(()->{store.toDockEnd(thing.kind,thing.id);return null;},
            done->{status.setText(Say.t("In the dock"));refresh();},this::failed);
    }
    /**
     * The places a + makes things in (the owner, 2026-10-04: "all groups including Temp, Archive, Favourites and Bin should
     * have the + button"; decision 87): what is made there is made on Home and put in the place.
     */
    static boolean takesNew(String id){return NoteStore.TEMP.equals(id)||NoteStore.ARCHIVE.equals(id)||NoteStore.BIN.equals(id)||NoteStore.FAVOURITES.equals(id);}
    /** Whether a thing made from that place's + (or none) stands on Home: not one put in the archive or the bin. */
    static boolean shownOnHome(String place,boolean tempOnHome) {
        // Not one made in Temp while Temp's things are not shown on Home: it was given a place on Home's grid all the same, and
        // to the owner it was "saved on Home" (2026-10-14).
        if(NoteStore.TEMP.equals(place)&&!tempOnHome)return false;
        return !NoteStore.ARCHIVE.equals(place)&&!NoteStore.BIN.equals(place);
    }
    /** A thing put in a place a + made it from, on the disk: Temp for as long as Temp keeps things, the archive, the bin, Favourites. */
    void intoPlaceNow(NoteStore.Branch.Kind kind,String id,String place) {
        if(NoteStore.TEMP.equals(place)) {
            store.makeTemporary(kind,id,System.currentTimeMillis()+TEMP_LENGTHS[tempSpan()]);
            if(tempToMine()) {
                store.toMyDevices(kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:kind==NoteStore.Branch.Kind.FILE?Sharing.Scope.FILE:Sharing.Scope.COLLECTION,id);
                // A file goes as a file on its own does, once its bytes are up (decision 95).
                if(kind==NoteStore.Branch.Kind.FILE)SwingUtilities.invokeLater(()->fileToMine(id));
            }
        }
        else if(NoteStore.ARCHIVE.equals(place))store.putAway(kind,id,false,true);
        else if(NoteStore.BIN.equals(place))store.putAway(kind,id,true,true);
        else if(NoteStore.FAVOURITES.equals(place))store.keepToHand(kind,id,true);
    }
    /** Files from this PC - pictures, documents, anything - kept where the + was pressed: Home, a collection, or Home and then the place. */
    void fromThisDevice(String where) {
        List<Path> sources=DesktopPick.open(frame,"From this device",true);
        if(sources.isEmpty())return;
        String place=takesNew(where)?where:null;
        keep(place!=null?Things.HOME:where,place!=null?null:NoteStore.home(where)?null:store_name(where),sources,place);
    }
    private String store_name(String collection){NoteStore.Branch known=find(collection);return known==null?null:named(known);}
    /** Add photos…, on an album (decision 217): several pictures of this PC at once, in Windows' own box, kept in the folder. */
    void addPhotos(String folder,String called) {
        List<Path> sources=DesktopPick.open(frame,"Add photos",true);
        if(!sources.isEmpty())keep(folder,called!=null?called:store_name(folder),sources);
    }
    /** Take a photo…, on an album: Windows' Camera, and what it saves kept in the folder (decision 217). */
    void takePhotoInto(String folder){DesktopCamera.into(this,folder,store_name(folder));}
    /** Add a file… on a collection: files chosen here, kept with it. */
    private void addFiles(NoteStore.Branch collection) {
        List<Path> sources=DesktopPick.open(frame,"Add a file",true);
        if(!sources.isEmpty())keep(collection.id,named(collection),sources);
    }
    /** Sync now for one thing, or for everything: what waits for it goes, and the others are asked for theirs. */
    private JMenuItem syncRow(NoteStore.Branch thing) {
        JMenuItem sync=DesktopMenus.item("Sync now");sync.addActionListener(a->{selected=thing;save(()->{selected=thing;sync(true);});});return sync;
    }
    /** Whether a thing reaches anybody, as its mark last said: only those have anything to sync. */
    private boolean shared(NoteStore.Branch thing) {
        SyncMark mark=marks.get(thing.id);if(mark==null&&base!=null&&base.id.equals(thing.id))mark=noteMark;
        return mark!=null&&mark!=SyncMark.HERE;
    }
    /**
     * Something done to the open note, for a note that may not be open: opened first, and done once it is on the page
     * and the notebook has said whether it may be written in here. {@code writes}: it needs a note it may write in.
     */
    private void onNote(NoteStore.Branch note,boolean writes,Runnable then) {
        Runnable go=()->{if(writes&&!page.isEditable()){status.setText(Say.t("This note is read only here"));return;}then.run();};
        if(isOpen(note.id)){go.run();return;}
        if(dirty||saving){save(()->onNote(note,writes,then));return;}
        int request=++noteRequest;
        disk.submit(()->store.get(note.id),got->{
            if(request!=noteRequest||got==null)return;
            display(got);
            // Behind the question display asks the notebook, whether it may be written in here.
            disk.submit(()->null,asked->{if(isOpen(note.id))go.run();},this::failed);
        },this::failed);
    }
    /**
     * Home itself, or a card, right-clicked where there is no icon - or the tree's Home line: what + makes there first,
     * then, on Home, Undo and syncing everything, as the phone's menu offers there; in a card, the collection's own menu.
     */
    void roomMenu(NoteStore.Branch here,Component where,int x,int y){roomMenuOf(here).show(where,x,y);}
    /** The same menu, built and not shown: what the tests draw. */
    JPopupMenu roomMenuOf(NoteStore.Branch here) {
        boolean top=here.kind==NoteStore.Branch.Kind.LIBRARY;String in=top?Things.HOME:here.id;
        JPopupMenu menu=new JPopupMenu();
        if(top) {
            // Everything about Home, as a long press on its empty room gives it on the phone (docs/HOME.md, decisions 43
            // and 44): the size notes are read at, Home's colour and its strength; Undo; what is made and done here;
            // favourites and search shown or not.
            // The same top as ⋯ (decision 185): the size, Home's colour, then syncing and sharing.
            menu.add(DesktopLook.ladder(()->rung,this::setTextSize));
            menu.add(homeColourMenu());
            menu.addSeparator();
            undoRow(menu);
            heading(menu,"Home");
            item(menu,"New note",()->newNoteIn(in));
            item(menu,"New folder",()->home.newCollection(in));
            // Every page at once, where there is more than one (decision 45); and back to the centre from another.
            if(Layout.active(home.grid.spots()).size()>1)item(menu,"All pages",()->home.pages.up());
            if(!home.grid.onCentre())item(menu,"Back to the main page",()->home.grid.showPage(0,0));
            // Sync and sharing just under Home, as in ⋯.
            menu.addSeparator();selected=library();syncAndSharing(menu,library());
            // Create and Find, as ⋯ has them (the owner, 2026-10-16: "the right click on the desktop should also display the
            // Create and Search section of the menu").
            menu.addSeparator();heading(menu,"Create");createRows(menu);
            menu.addSeparator();findRows(menu);
            // What Home shows, last: set once and seldom again (decision 80).
            onHomeRows(menu);
        } else {
            // The folder's own menu, its top as every menu's, and what is made in it under Create (decision 185).
            for(Component one:thingMenu(here).getComponents())menu.add(one);
            menu.addSeparator();heading(menu,"Create");
            item(menu,"New note",()->newNoteIn(in));
            item(menu,"New folder",()->home.newCollection(in));
        }
        return menu;
    }
    /**
     * The Menu key, or Shift+F10: the menu of what has the keyboard - the chosen line of the tree, a card, or,
     * in the note itself, the note - opened where it is, as a right-click on it would.
     */
    private void contextKey() {
        Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if(focus==tree) {
            TreePath path=tree.getSelectionPath();int row=path==null?-1:tree.getRowForPath(path);if(row<0)return;
            Rectangle at=tree.getRowBounds(row);tree.scrollRectToVisible(at);rowMenu(path,at.x+12,at.y+at.height);return;
        }
        // An icon, a dock icon, a found line or an open card: each says what its menu is.
        if(focus instanceof JComponent c&&c.getClientProperty(DesktopHome.MENU) instanceof java.util.function.Supplier<?> menu&&menu.get() instanceof JPopupMenu m){m.show(c,12,c.getHeight()-8);return;}
        if((focus==page||focus==title)&&base!=null) {
            java.awt.Point at=new java.awt.Point(12,focus.getHeight()/2);
            if(focus==page)try{Rectangle caret=page.modelToView2D(page.getCaretPosition()).getBounds();at=new java.awt.Point(caret.x,caret.y+caret.height);}catch(Exception none){/* the middle of the page */}
            paperMenu((javax.swing.text.JTextComponent)focus,focus==page?Links.at(page.links(),page.getCaretPosition()):null).show(focus,at.x,at.y);
        }
    }
    /**
     * A right-click on the page or the title: the caret goes where it was, unless it was inside the words chosen,
     * which stay chosen for Cut or Copy. Then the menu of what is there (see paperMenu).
     */
    private void paperClicked(MouseEvent e) {
        if(base==null)return;
        javax.swing.text.JTextComponent words=(javax.swing.text.JTextComponent)e.getComponent();
        int at=words.viewToModel2D(e.getPoint()),from=words.getSelectionStart(),to=words.getSelectionEnd();
        if(at>=0&&!(from<to&&at>=from&&at<=to))words.setCaretPosition(at);
        words.requestFocusInWindow();
        paperMenu(words,words==page?page.linkAt(e.getPoint()):null).show(words,e.getX(),e.getY());
    }
    /**
     * The page's menu, or the title's: an address first, where there is one under the pointer; then the words -
     * Cut and Copy while some are chosen, Paste, Select all; then the note's own menu, the same as on its tab.
     */
    JPopupMenu paperMenu(javax.swing.text.JTextComponent words,Links.Link link) {
        List<Component> first=new ArrayList<>();
        if(link!=null) {
            first.add(line("Open link",null,true,()->page.open(link.target())));
            String address=link.target().startsWith("mailto:")?link.target().substring(7):link.target();
            first.add(line("Copy link",null,true,()->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address),null);status.setText(Say.t("Link copied"));}));
            first.add(new JPopupMenu.Separator());
        }
        boolean chosen=words.getSelectionStart()<words.getSelectionEnd(),writes=words.isEditable(),pasting=false;
        // Words, or on the page a picture or files, which are kept with the note (DesktopDrops.Aimed).
        try{var clip=Toolkit.getDefaultToolkit().getSystemClipboard();pasting=clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)
            ||words==page&&(clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor)||clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.javaFileListFlavor));}
        catch(Exception busy){/* greyed, then */}
        if(chosen) {
            first.add(line("Cut","control X",writes,words::cut));
            first.add(line("Copy","control C",true,words::copy));
            // What is chosen, into another language (docs/TRANSLATION.md); the whole tab stays under This note.
            if(words==page)first.add(line("Translate…",null,true,()->DesktopTranslate.selection(this)));
        }
        first.add(line("Paste","control V",writes&&pasting,words::paste));
        first.add(line("Select all","control A",words.getDocument().getLength()>0,words::selectAll));
        first.add(new JPopupMenu.Separator());
        // Then what is done most from the page itself; the rest of the note's menu, the same as on its tab, one step in.
        JPopupMenu menu=new JPopupMenu(),whole=thingMenu(openNote());
        // This note first (the owner, 2026-10-05: "we should have This note at the top of the menu"), then the words' own lines.
        JMenu note=new JMenu(Say.t("This note"));menu.add(note);menu.addSeparator();
        for(Component one:first)menu.add(one);
        Map<String,Component> pulled=new HashMap<>();List<Component> rest=new ArrayList<>();
        for(Component one:whole.getComponents()){if(one instanceof JMenuItem item&&!(one instanceof JMenu)&&FROM_THE_PAGE.contains(DesktopMenus.words(item))&&!(chosen&&"Translate…".equals(DesktopMenus.words(item))))pulled.put(DesktopMenus.words(item),one);else rest.add(one);}
        for(String said:FROM_THE_PAGE)if(pulled.containsKey(said))menu.add(pulled.get(said));
        boolean divided=true;
        for(Component one:rest) {
            boolean line=one instanceof JPopupMenu.Separator;
            if(line&&divided)continue;
            note.add(one);divided=line;
        }
        if(divided&&note.getMenuComponentCount()>0)note.getPopupMenu().remove(note.getMenuComponentCount()-1);
        // And what someone else shows the code of, from a note as from every + and every menu (the owner: "this is key").
        menu.addSeparator();menu.add(line("From another device…",null,true,()->scanCode(null)));
        return menu;
    }
    /** The note's own lines a right-click on its page offers straight away, in this order; the rest are under This note. */
    private static final List<String> FROM_THE_PAGE=List.of("Attach a file…","Record audio","Versions…","Copy the text","Translate…","Share…","Sync now");
    private static JMenuItem line(String said,String key,boolean can,Runnable does) {
        JMenuItem line=DesktopMenus.item(said);if(key!=null)line.setAccelerator(KeyStroke.getKeyStroke(key));
        line.setEnabled(can);line.addActionListener(a->does.run());return line;
    }
    /** The open note, title to files, while it is shown: what files dropped on it light. */
    JComponent noteArea(){return editorPanel!=null&&editorPanel.isShowing()?editorPanel:null;}
    /** Whether the note is what is on the screen, rather than Home. */
    boolean onPage(){return onPage;}

    /**
     * The last thing put away, moved or renamed, and how to put it back: one level, as on the phone. Offered near
     * the top of the menus until something else takes its place.
     */
    private String undoWhat="";private Background.Work<Object> undoHow;
    void canUndo(String what,Background.Work<Object> how) {
        // Named by its first words only: the whole first line of a note made the menu as wide as the screen (2026-10-14).
        String name=what==null?"":what.replaceAll("\\s*[\\r\\n]+\\s*"," ").trim();
        undoWhat=name.isEmpty()?"Untitled":name.length()<=32?name:name.substring(0,31).trim()+"…";undoHow=how;
    }
    private void undoRow(JPopupMenu menu) {
        if(undoHow==null)return;
        JMenuItem row=DesktopMenus.item("Undo");row.setText(DesktopMenus.shortly(Say.t("Undo “{0}”",undoWhat)));row.addActionListener(a->undo());menu.add(row);menu.addSeparator();
    }
    void undo() {
        Background.Work<Object> how=undoHow;String what=undoWhat;undoHow=null;undoWhat="";
        if(how==null)return;
        status.setText(Say.t("Putting back…"));
        disk.submit(how,done->{status.setText(Say.t("“{0}” put back",what));refresh();},e->status.setText(Say.t("Could not put that back. Nothing was changed.")));
    }
    /** Starred or not: the tree and the menus say so at once. Starred, it goes into the dock too; unstarred, it stays there. */
    void favourite(NoteStore.Branch thing,boolean kept) {
        disk.submit(()->{store.keepToHand(thing.kind,thing.id,kept);return null;},done->{status.setText(Say.t(kept?"Added to favourites":"Removed from favourites"));refresh();},this::failed);
    }

    /** A note or a collection put in the archive or the bin; its card in the overview goes with it. */
    void putAway(NoteStore.Branch thing,boolean bin) {
        if(thing.kind==NoteStore.Branch.Kind.PAGE&&base!=null&&base.id.equals(thing.id)){putAway(bin);return;}
        // Pin the others first, so the gap this one leaves stays a gap (decision 119).
        Runnable pin=home==null?null:home.stayPut(thing.id);
        disk.submit(()->{if(pin!=null)pin.run();store.putAway(thing.kind,thing.id,bin,true);return null;},done->{
            canUndo(thing.name,()->{store.restore(thing.kind,thing.id);return null;});
            overview.forget(thing.id);status.setText(Say.t(bin?"Moved to the bin":"Archived"));refresh();},this::failed);
    }

    /**
     * A note into another collection, or a collection onto the top or into another one - never into itself or anything
     * it holds (docs/HOME.md, decision 14). Where that changes who can read it, it is said by name first, as on the
     * phone: moving into something shared is a disclosure, out of it a withdrawal.
     */
    void moveTo(NoteStore.Branch moved) {
        boolean page=moved.kind==NoteStore.Branch.Kind.PAGE;
        String now=page?store_bookOf(moved):moved.parent;
        disk.submit(()->places(store,moved,now),places->{
            if(places.isEmpty()){status.setText(Say.t(page?"There is no other folder to move it into.":"There is nowhere else to move it."));return;}
            NoteStore.Branch into=DesktopUi.pick(frame,"Move “"+moved.name+"”","Into which folder?",places,Desktop::placeName,"Move here");
            if(into==null)return;
            moveInto(moved,into.kind==NoteStore.Branch.Kind.LIBRARY?DesktopMoving.top():into,null);
        },this::failed);
    }
    /**
     * Where Move to… offers to put a thing, {@code now} being where it is: for a note every collection, for a collection
     * the top and every collection but itself and what is inside it - the notebook leaves those out - and in either
     * case not where it already is. On the disk thread.
     */
    static List<NoteStore.Branch> places(NoteStore store,NoteStore.Branch moved,String now) {
        List<NoteStore.Branch> places=new ArrayList<>();
        for(NoteStore.Branch one:store.places(moved))if(!(NoteStore.home(now)?NoteStore.home(one.id):one.id.equals(now)))places.add(one);
        return places;
    }
    /** A place in Move to…'s list: Home, or a collection's name and, for one inside another, where it is. */
    static String placeName(NoteStore.Branch place) {
        if(place.kind==NoteStore.Branch.Kind.LIBRARY)return Say.t("Home");
        String name=DesktopHome.named(place),where=place.detail==null?"":place.detail;
        return where.startsWith("in ")?name+"   ·   "+where.substring(3):name;
    }

    /**
     * Moved into this collection, or onto the top - from Move to…, or a drag let go on it. {@code order} is the level
     * it lands in, in the order it is to keep there; null leaves it at the top, where a move puts things.
     */
    void moveInto(NoteStore.Branch moved,NoteStore.Branch into,List<String> order) {
        boolean page=moved.kind==NoteStore.Branch.Kind.PAGE,onTop=into.kind==NoteStore.Branch.Kind.LIBRARY;
        disk.submit(()->{
            // Refused before anybody is asked about who it would reach: a collection never goes inside itself.
            if(!page&&!Things.mayGoInto(store.parents(),moved.id,onTop?Things.HOME:into.id,true))
                throw new IllegalArgumentException("A folder cannot go inside itself, or inside anything it holds.");
            List<Sharing.Rule> rules=store.shares();
            // Its whole path where it is and where it would be, each ending with itself: every collection above counts, however deep.
            List<String> from=store.pathOf(moved.id),to=onTop?new ArrayList<>():store.pathOf(into.id);to.add(moved.id);
            Sharing.Change change=Sharing.moving(rules,from,to);
            Map<String,String> names=new HashMap<>();for(NoteStore.Contact c:store.addresses())names.put(c.address,c.name);
            return new Object[]{change,names};
        },found->{
            Sharing.Change change=(Sharing.Change)found[0];@SuppressWarnings("unchecked") Map<String,String> names=(Map<String,String>)found[1];
            if(change.any()) {
                StringBuilder said=new StringBuilder("Moving “"+moved.name+"” into "+into.name+" changes who receives it.");
                if(!change.gained.isEmpty()){said.append("\n\nStarts reaching: ");said.append(String.join(", ",change.gained.keySet().stream().map(a->names.getOrDefault(a,"a paired device")).toList()));}
                if(!change.lost.isEmpty()){said.append("\n\nStops reaching: ");said.append(String.join(", ",change.lost.keySet().stream().map(a->names.getOrDefault(a,"a paired device")).toList()));}
                said.append("\n\nWhoever starts receiving it gets it now. What has already reached somebody stays with them.");
                if(!DesktopUi.confirm(frame,"This changes who can read it",said.toString(),"Move anyway",false)){status.setText(Say.t("Not moved"));return;}
            }
            disk.submit(()->{
                // Where it was, before it is anywhere else: an undo has to know the place to put it back into.
                String from=page?store.bookOf(moved.id):store.collectionOfBook(moved.id);
                if(page)store.movePage(moved.id,into.id);else store.moveBook(moved.id,into.id);
                if(order!=null)store.order(moved.kind,order);return from;},from->{
                canUndo(moved.name,()->{if(page)store.movePage(moved.id,from);else store.moveBook(moved.id,from);
                    network.submit(()->{Post.changed(context,store,keys,moved.kind,moved.id);return null;},v->{},e->{});return null;});
                // The open note's line says which collection it is in: it is in another one now.
                if(page&&base!=null&&base.id.equals(moved.id))base.book=into.id;
                status.setText(Say.t("Moved into {0}",into.name));refresh();
                network.submit(()->{Post.changed(context,store,keys,moved.kind,moved.id);return null;},v->{},e->{});
            },this::failed);
        },this::failed);
    }
    private String store_bookOf(NoteStore.Branch page){return base!=null&&base.id.equals(page.id)?base.book:page.parent;}

    /** A level put in a new order by a drag: kept as it now looks, and one Undo from how it was. */
    void reorder(NoteStore.Branch moved,List<String> order,List<String> before) {
        disk.submit(()->{store.order(moved.kind,order);return null;},done->{
            canUndo(moved.name,()->{store.order(moved.kind,before);return null;});refresh();
        },e->{status.setText(Say.t("Could not keep that order. Nothing was changed."));refresh();});
    }

    /** Everything, as the tree last read it. */
    List<NoteStore.Branch> everything(){return everything;}

    /** The open note's words, on the clipboard: to paste into a message, a mail, anything. */
    void copyNote() {
        if(base==null)return;
        String words=(title.getText().isBlank()?"":title.getText()+"\n\n")+noteNow();
        // Never a private code, nor the rest of its line, on the clipboard for another program (decision 114).
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(PrivateCode.scrub(words)),null);
        status.setText(Say.t("The note is copied. Paste it anywhere."));
    }

    /** What to say, and about what: the form on GitHub, filled in, opened in the browser. Nothing is sent from here. */
    void feedback() {
        JDialog[] box={null};
        JComboBox<String> kind=new JComboBox<>();DesktopUi.translate(kind);for(Feedback.Kind k:Feedback.Kind.values())kind.addItem(k.said);
        JComboBox<String> area=new JComboBox<>();DesktopUi.translate(area);for(Feedback.Area a:Feedback.Area.values())area.addItem(a.said);
        JTextArea words=new JTextArea(6,36);words.setLineWrap(true);words.setWrapStyleWord(true);
        words.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopUi.LINE),BorderFactory.createEmptyBorder(8,10,8,10)));
        JPanel body=DesktopUi.column();
        JPanel kindHolder=new JPanel(new GridBagLayout());kindHolder.setOpaque(false);kindHolder.add(kind);
        JPanel areaHolder=new JPanel(new GridBagLayout());areaHolder.setOpaque(false);areaHolder.add(area);
        body.add(DesktopUi.row(DesktopUi.body("What is it?"),kindHolder));body.add(DesktopUi.row(DesktopUi.body("About"),areaHolder));
        DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,words);DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.note("It opens a filled-in form on GitHub under your own name; nothing is sent from here. Reports are public, so say what the app did rather than who you are.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        JButton go=DesktopUi.primary("Open the form",()->{
            String url=Feedback.url(DesktopUpdate.SOURCE,Feedback.Kind.values()[kind.getSelectedIndex()],Feedback.Area.values()[area.getSelectedIndex()],words.getText(),"",
                "Mininotes for Windows v"+VERSION+", "+System.getProperty("os.name"));
            try{java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));box[0].dispose();}catch(Exception e){failed(e);}
        });
        box[0]=DesktopUi.sheet(frame,"Feedback",body,DesktopUi.footer(go),true);box[0].getRootPane().putClientProperty("focus",words);DesktopUi.show(box[0],480,560);
    }

    /** The rung of the reading ladder notes are read at here, unless one has its own: ten, as on the phone (see DesktopLook). */
    int textSize(){return rung;}
    /** Every note without a size of its own at one rung; kept, as the phone keeps it. */
    void setTextSize(int which) {
        showSize(which);
        int kept=rung;disk.submit(()->{DesktopLook.keepRung(context,kept);return null;},done->{},e->{});
    }
    /** The rung the open note is read at: its own, or this PC's. */
    int pageSize(){return Reading.of(base==null?Reading.NONE:base.rung,rung);}
    /** The ladder at the top of the menu: the open note's size while one is open, as its own Text size is; this PC's when none is. */
    private void sizeHere(int which) {
        if(base!=null&&onPage)resize(noteAsThing(),DesktopLook.clamp(which));else setTextSize(which);
    }
    private NoteStore.Branch noteAsThing(){NoteStore.Branch thing=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,base.id,base.book,base.title,"",0,0,false,base.colour);thing.tone=base.tone;return thing;}
    /** The open note as its face is drawn: its colour as the page has it, and what it wears as last read. */
    private NoteStore.Branch noteLook(){NoteStore.Branch look=noteAsThing();look.icon=noteIcon;look.image=noteImage;return look;}
    /** Text size ▸ for one note, at the rung it has of its own as the notebook last said. */
    /**
     * A note's writing lines, shown or not (the owner, 2026-10-03: "with a right click on a note we should be able to display
     * the writing lines or not"), beside its size: kept on this PC only, and the open page drawn so at once.
     */
    private JMenuItem linesItem(NoteStore.Branch thing) {
        boolean open=base!=null&&base.id.equals(thing.id);
        // Set by looking, as a colour is: the menu stays up (decision 107).
        JCheckBoxMenuItem item=DesktopLook.stays(new JCheckBoxMenuItem(Say.t("Writing lines"),open?base.lines:!plainNow.contains(thing.id)));
        DesktopMenus.symbol(item,"Writing lines");
        item.addActionListener(e->{
            boolean on=item.isSelected();
            if(base!=null&&base.id.equals(thing.id)){base.lines=on;page.lined=on;page.repaint();}
            Set<String> now=new java.util.HashSet<>(plainNow);if(on)now.remove(thing.id);else now.add(thing.id);plainNow=now;
            disk.submit(()->{store.lined(thing.id,on);return null;},done->{},this::failed);
        });
        return item;
    }
    private JMenu sizeMenu(NoteStore.Branch thing) {
        int own=base!=null&&base.id.equals(thing.id)?base.rung:rungsNow.getOrDefault(thing.id,Reading.NONE);
        return DesktopLook.sizes(own,rung,which->resize(thing,which));
    }
    /**
     * A size given to one note, or taken back with Reading.NONE: kept on this PC only, and the page drawn at it at
     * once when it is the one open. A note that is not open shows nothing, so the bar says what it will open at.
     */
    void resize(NoteStore.Branch thing,int which) {
        int kept=Reading.stored(which);boolean open=base!=null&&base.id.equals(thing.id);
        if(open){base.rung=kept;showSize(rung);}
        Map<String,Integer> now=new HashMap<>(rungsNow);if(Reading.known(kept))now.put(thing.id,kept);else now.remove(thing.id);rungsNow=now;
        disk.submit(()->{store.size(thing.id,kept);return null;},done->{
            if(!open){String name=thing.name==null||thing.name.isBlank()?"This note":"“"+thing.name+"”";
                status.setText(Reading.known(kept)?Say.t("{0} opens at text size {1} of {2}",name,kept+1,Reading.RUNGS):Say.t("{0} opens at the same size as other notes",name));}
        },this::failed);
    }
    /** This PC's rung, and the page at the rung the open note is read at: its own if it has one. */
    private void showSize(int which) {
        rememberReading();
        rung=DesktopLook.clamp(which);float size=DesktopLook.size(pageSize());
        page.setFont(page.getFont().deriveFont(size));title.setFont(title.getFont().deriveFont((float)Math.round(size*24f/18f)));
        page.revalidate();page.repaint();title.revalidate();restoreReading();
    }

    private NoteStore.Branch find(String id) {
        if(id==null)return null;for(NoteStore.Branch one:everything)if(one.id.equals(id))return one;return null;
    }
    /**
     * A new name for a collection, at any depth - typed where the name is written while its card is up, else in a box;
     * a note's name is its title, typed where it is.
     */
    void rename(NoteStore.Branch branch) {
        if(branch.kind==NoteStore.Branch.Kind.PAGE){if(onPage&&base!=null&&base.id.equals(branch.id)){title.requestFocusInWindow();title.selectAll();}else{save(()->open(branch.id));}return;}
        if(!isCollection(branch))return;
        NoteStore.Branch card=home.folder.shown();
        if(!onPage&&card!=null&&card.id.equals(branch.id)){home.folder.startNaming();return;}
        String name=DesktopUi.ask(frame,"Rename folder","Name",branch.name);
        if(name==null||name.isBlank()||name.trim().equals(branch.name))return;
        String was=branch.name;
        disk.submit(()->{store.renameCollection(branch.id,name.trim());return null;},done->{
            status.setText(Say.t("Renamed to {0}",name.trim()));refresh();
            // Only where there was a name before, as on the phone: back to a made-up one is no undo.
            if(!was.isBlank()&&!was.equals("New folder")&&!was.equals("New collection")&&!was.equals("New book")&&!was.equals(NoteStore.UNTITLED))
                canUndo(was,()->{store.renameCollection(branch.id,was);return null;});
        },this::failed);
    }
    /** What is starred, by id: the tree puts a star after it, so the names still start in one column. */
    static volatile Set<String> STARRED=Set.of();
    record Item(NoteStore.Branch branch){public String toString(){
        if(branch.kind==NoteStore.Branch.Kind.FAVOURITES)return Say.t("Favourites")+"  ★";
        if(branch.kind==NoteStore.Branch.Kind.LIBRARY)return Say.t("Home");
        return DesktopHome.named(branch)+(STARRED.contains(branch.id)?"  ★":"");}}
    void menu(JButton anchor){mainMenu().show(anchor,0,anchor.getHeight());}
    /** The ⋯ menu: shown from its button, or pinned down the right of the window (see DesktopPinnedMenu). */
    JPopupMenu mainMenu() {
        // One section per kind of thing, each under its own small heading: the open note first, then making
        // new things, then the places things are put away, then people, backups, and the app itself.
        JPopupMenu menu=new JPopupMenu();
        boolean noteOpen=base!=null&&onPage;
        // The same top in every menu, in the same order (the owner, 2026-10-17: "colour should be at the top, the top should
        // always stay the same and generally speaking the order of elements in each menu should stay the same"; decision
        // 185): the size it is read at, once; the colour and icon of what is in view; then syncing and sharing, which is what
        // Mininotes is for ("these functions should be at the top of all menus in a dedicated section").
        // A note's size, while a note is open; the app's own size is in My profile (decision 200), and notes' usual size in Settings.
        if(noteOpen)menu.add(DesktopLook.ladder(this::pageSize,this::sizeHere));
        boolean atHome=!onPage&&!home.folder.isOpen();
        NoteStore.Branch card=noteOpen?null:home.folder.shown(),placeCard=noteOpen?null:home.folder.placeShown();
        boolean placeShown=placeCard!=null&&placeCard.kind!=NoteStore.Branch.Kind.TOOLS;
        if(noteOpen){menu.add(colourMenu(noteAsThing()));lookRows(menu,noteAsThing());}
        else if(card!=null){menu.add(colourMenu(card));lookRows(menu,card);}
        else if(placeShown)menu.add(placeColourMenu(placeCard));
        else if(atHome)menu.add(homeColourMenu());
        menu.addSeparator();
        undoRow(menu);
        if(noteOpen) {
            heading(menu,"This note");
            // In the order the note's own menu has it (see thingMenu): its look, what it holds, where it goes, its words
            // elsewhere, its past; put away last.
            menu.add(linesItem(noteAsThing()));
            item(menu,"Attach a file…",()->save(this::attach));item(menu,"Attachments…",this::attachments);
            item(menu,"Move to…",()->save(()->moveTo(new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,base.id,base.book,base.title==null||base.title.isBlank()?"this note":base.title,"",0,0,false))));
            boolean starred=STARRED.contains(base.id);
            item(menu,starred?"Remove from favourites":"Add to favourites",()->favourite(new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,base.id,base.book,base.title,"",0,0,false),!starred));
            item(menu,"Copy the text",this::copyNote);
            item(menu,"Translate…",()->DesktopTranslate.note(this));
            item(menu,"Versions…",()->save(this::versions));
            // On Minima, where the owner looked for it (decision 224), in one line opening one window (decision 253).
            String stampId=base.id;
            DesktopStamp.menuLine(this,menu,stampId,()->save(()->DesktopStamp.thing(this,stampId,true)));
            DesktopOnDisk.rows(this,menu,noteAsThing());
            item(menu,"Archive",()->save(()->putAway(false)));item(menu,"Move to bin",()->save(()->putAway(true)));
            menu.addSeparator();
        } else if(placeShown&&placeCard.kind==NoteStore.Branch.Kind.TEMP) {
            heading(menu,placeCard.name);
            tempRows(menu);
            menu.addSeparator();
        } else if(atHome) {
            heading(menu,"Home");
            homeRows(menu);
            menu.addSeparator();
        }
        // Sync and sharing just under what is in view (the owner, 2026-10-17: "Share should be just below Home and Sync now
        // should be in the Share section").
        syncAndSharing(menu,noteOpen?null:card);
        menu.addSeparator();
        // In the owner's order (the owner, 2026-10-04: "Home, New, People, Find, On Home, Backup"; decision 89).
        heading(menu,"Create");
        // On Home, what is made is in Home's own rows, as on the phone; anywhere else, here.
        if(!atHome){menu.add(line("New note","control N",true,()->save(this::newNote)));item(menu,"New folder",()->save(()->addShelf(null)));}
        // And one inside the collection shown or chosen, or beside the open note's, where that is not the top: named, so it is plain where.
        NoteStore.Branch inside=shelfParent();if(inside!=null)item(menu,"New folder in “"+named(inside)+"”",()->save(()->addShelf(inside)));
        createRows(menu);
        menu.addSeparator();
        findRows(menu);
        menu.addSeparator();
        // Home's places switched off Home are here instead (decision 78): one way to each, not two.
        if(!onHome(NoteStore.ARCHIVE)||!onHome(NoteStore.BIN)||!onHome(NoteStore.TEMP)||!onHome(NoteStore.SHARED)||!openListWanted()) {
            heading(menu,"Places");
            if(!openListWanted())item(menu,"Recent",()->{showHome();home.folder.openPlace(DesktopHome.recentPlace());});
            if(!onHome(NoteStore.TEMP))item(menu,"Temp",()->{showHome();home.folder.openPlace(DesktopHome.tempPlace(0));});
            if(!onHome(NoteStore.SHARED))item(menu,"Shared with me",()->{showHome();home.folder.openPlace(DesktopHome.sharedPlace());});
            // Their cards, as on the phone and as their icons open them (decision 89).
            if(!onHome(NoteStore.ARCHIVE))item(menu,"Archive",()->{showHome();home.folder.openPlace(DesktopHome.archivePlace(0));});
            if(!onHome(NoteStore.BIN))item(menu,"Bin",()->{showHome();home.folder.openPlace(DesktopHome.binPlace(0));});
            menu.addSeparator();
        }
        // What Home shows, in a section of its own after Find, while Home is what is in view (decisions 80 and 89).
        if(card==null&&!onPage&&!home.folder.isOpen()){onHomeRows(menu);menu.addSeparator();}
        heading(menu,"Backup");
        item(menu,"Recovery writing…",()->save(this::recoveryWriting));
        item(menu,"Export backup…",()->save(this::backup));item(menu,"Add from backup…",()->save(this::importBackup));
        menu.addSeparator();
        heading(menu,"Mininotes");
        // Share Mininotes first, Version just above About (the owner, 2026-10-14).
        item(menu,"Share Mininotes…",this::shareApp);
        // The language, where nobody has to look for it (decision 160): the one in use ticked, each in its own words.
        menu.add(languageMenu());
        // In the owner's order (decision 89); a newer version first while one is known, as on the phone.
        if(Update.newer(newestKnown,VERSION))item(menu,"Update to v"+newestKnown,()->lookForUpdate(true));
        item(menu,"Feedback…",this::feedback);
        item(menu,"Settings…",()->DesktopSettings.open(this));
        // Which build this is, as on the phone; clicked, it looks for a newer one.
        item(menu,Say.t("Version")+"  ·  v"+VERSION,()->lookForUpdate(true));
        // The other app, just above About, as on the phone (decision 241).
        item(menu,"Mininotes on your phone…",()->DesktopOtherApp.open(frame));
        item(menu,"About…",this::about);
        // Beside About and the updates, as on any app (decision 235): what it deletes said before anything is.
        item(menu,"Uninstall Mininotes…",()->DesktopUninstall.open(this));
        item(menu,"Exit Mininotes",()->save(()->shutdown(true)));
        return menu;
    }
    /**
     * Sync and sharing, under its heading, near the top of every menu in the same order (decision 185): Sync now, its
     * parameters and Share… for what is in view (the open note, or {@code thing}), then the people it can go to and the way
     * to add them. Each line its own symbol.
     */
    void syncAndSharing(JPopupMenu menu,NoteStore.Branch thing) {
        heading(menu,"Sync and sharing");
        if(thing!=null)menu.add(syncRow(thing));
        else item(menu,"Sync now",()->{selected=null;save(()->sync(true));});
        boolean something=thing!=null||base!=null&&onPage;
        if(something) {
            item(menu,"Sync parameters…",()->{selected=thing;save(this::share);});
            item(menu,"Share…",()->{selected=thing;save(this::share);});
            item(menu,"Show my code…",()->{selected=thing;save(()->{NoteStore.Branch what=target();if(what!=null)showCode(what);});});
        }
        // One file anywhere on this PC, kept in step with your devices (docs/FILES.md): sharing, so here (the owner, 2026-10-17).
        item(menu,"Share a file through Mininotes…",()->DesktopOnDisk.syncFile(this));
        // A whole folder of this PC, any files, kept in step through Mininotes and on disk (decision 197; built in 174).
        item(menu,"Sync a folder of this PC…",()->DesktopOnDisk.syncFolder(this));
        // Then the people, just above you (the owner, 2026-10-18: "Personnes et appareils should be just on top Profil, and
        // rename this one Mon profil to make it clear"; decision 200).
        item(menu,"From another device…",()->scanCode(null));
        item(menu,"People and devices…",()->people(null));
        item(menu,"My profile…",this::profile);
    }

    /** The sharing box for one thing, as its Share… does: what the strip of a carry opens (decision 90). */
    void shareThing(NoteStore.Branch thing){selected=thing;save(this::share);}
    /** Share, right-clicked: sharing what is open, syncing it, and the people it could go to. */
    JPopupMenu shareMenu() {
        selected=onPage?null:home.chosen();
        JPopupMenu menu=new JPopupMenu();boolean open=target()!=null;
        menu.add(line("Share…",null,open,()->save(this::share)));
        menu.add(line("Show my code…",null,open,()->save(()->{NoteStore.Branch what=target();if(what!=null)showCode(what);})));
        item(menu,"Sync now",()->save(()->sync(true)));
        menu.addSeparator();
        item(menu,"From another device…",()->scanCode(null));
        item(menu,"People and devices…",()->people(null));
        item(menu,"My profile…",this::profile);
        return menu;
    }
    /** A section's name inside a menu: small, quiet, not something to press. */
    private static void heading(JPopupMenu menu,String name) {
        // In the language chosen, as every other word in the menu (the owner, 2026-10-13: "translate the menu sections' titles too").
        JLabel label=new JLabel(Say.t(name).toUpperCase(java.util.Locale.forLanguageTag(Say.language())));label.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,11f));label.setForeground(QUIET);
        label.setBorder(BorderFactory.createEmptyBorder(8,16,2,16));menu.add(label);
    }
    /** An arrow pointing left: ← Home, at the left of a note's bar. */
    static Icon backIcon() {
        return new Icon(){
            public int getIconWidth(){return 14;}public int getIconHeight(){return 14;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(INK);g.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.drawLine(x+2,y+7,x+13,y+7);g.drawPolyline(new int[]{x+7,x+2,x+7},new int[]{y+2,y+7,y+12},3);
                g.dispose();
            }
        };
    }
    /**
     * The tree beside Home and the page, or not (decision 26): ⋯ → Tree, Ctrl+B, the grip on its edge and the switch in
     * Settings all turn it, and which it is is kept for the next time.
     */
    void showTree(boolean shown) {
        // Folded, the tree is still there at the window's edge as its line with the handle (the owner, 2026-10-12: "the
        // tree view should be displayable directly from the side bar ... the side bar should always be visible"): a click
        // on the handle, or pulling the line out, opens it.
        treeWanted=shown;side.setVisible(true);split.setDividerSize(GRIP);split.setDividerLocation(shown?sideWidth:0);
        tree.setFocusable(shown);
        split.revalidate();for(Component one:split.getComponents())if(one instanceof Grip)one.repaint();
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString(TREE,Boolean.toString(shown)).apply();return null;},done->{},this::failed);
    }
    static final String DOWNLOAD="https://github.com/mininotesorg/mininotes/releases/latest";
    /** Three lines to send with the link: what it is, what it is for, where to get it. The same words as the phone's. */
    static final String INVITE="I use Mininotes to keep notes and lists with the people close to me: a private paper pad, sealed from phone to phone, nothing to sign up for.\nAndroid: open the link and install the .apk file.\n";

    /** Mininotes, handed on: a code for a phone's camera, and the three lines with the link to paste anywhere. */
    void shareApp() {
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note("Somebody with their phone here can scan the code with its camera. For anybody else, copy the message and send it.",400,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,DesktopUi.M);
        try {
            JLabel code=new JLabel(new ImageIcon(DesktopQr.draw(DOWNLOAD,220)));JPanel centred=new JPanel(new GridBagLayout());centred.setOpaque(false);centred.add(DesktopUi.card(null,code));
            DesktopUi.add(body,centred);DesktopUi.gap(body,DesktopUi.M);
        } catch(Exception noCode){/* the message is enough */}
        DesktopUi.Text message=DesktopUi.note(INVITE+DOWNLOAD,400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));
        DesktopUi.add(body,message);DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.Text copied=DesktopUi.quiet(" ");
        DesktopUi.add(body,DesktopUi.actions(DesktopUi.primary("Copy the message",()->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(INVITE+DOWNLOAD),null);copied.setText(Say.t("Copied. Paste it into a message or an email."));})));
        DesktopUi.gap(body,4);DesktopUi.add(body,copied);
        DesktopUi.tell(frame,"Share Mininotes",body);
    }

    // ---- a newer version --------------------------------------------------------------------------------------

    /** A newer version already downloaded and checked, waiting beside this app to take its place; or null. */
    private Path staged;private String stagedVersion="";
    private boolean fetching;

    /** Whether new versions are fetched by themselves. On unless it was switched off. */
    /** How long after the writing stops a note goes, in seconds, or NoteStore.WHEN_ASKED: the app's own setting. */
    int syncAfter() {
        try{return Integer.parseInt(context.getSharedPreferences("settings",0).getString("syncAfter",Integer.toString(NoteStore.USUALLY)));}
        catch(NumberFormatException e){return NoteStore.USUALLY;}
    }
    /** The setting changed: kept, used at once for everything that has no timing of its own, and what is waiting goes. */
    void setSyncAfter(int seconds) {
        context.getSharedPreferences("settings",0).edit().putString("syncAfter",Integer.toString(seconds)).apply();
        store.usually=seconds;if(seconds<0)due.clear();else queuePending();
    }

    /** Whether the tree is showing beside the page. */
    boolean treeShown(){return treeWanted;}
    /** The tree's line let go of: pulled out far enough to read, the tree opens at that width; pushed in too far, it folds. */
    private void treePulled() {
        int at=split.getDividerLocation();
        if(treeWanted&&at<MIN_SIDE)showTree(false);
        else if(!treeWanted&&at>=MIN_SIDE){sideWidth=Math.min(at,900);keepWidth.restart();showTree(true);}
        else if(!treeWanted)split.setDividerLocation(0);
    }

    /**
     * A row that is on or off, ticked at the right while it is on, where the tick moves no word: a check box's own tick
     * pushed the words out of the column the others start in.
     */
    static JMenuItem ticked(String said,boolean on,Runnable flip){return ticked(said,on,null,flip);}
    /**
     * @param state where the switch stands now, read each time the tick is drawn, or null to draw what it was when the line was
     *              made and was clicked to since. Read, the tick is the setting itself and never a copy that could say the
     *              opposite (the owner, 2026-10-14: Favourites in Show on Home "does the opposite").
     */
    static JMenuItem ticked(String said,boolean on,java.util.function.BooleanSupplier state,Runnable flip) {
        JMenuItem row=new JMenuItem(Say.t(said)){
            /** Ticked now: switched in place, the menu staying open for the next one. */
            boolean now=on;
            // Clicked, it switches where it is and the menu stays open (the owner, 2026-10-13: "when we click one of the
            // elements of Home, make sure the menu doesn't close, so that we can click another one"). Enter still closes it.
            @Override protected void processMouseEvent(MouseEvent e) {
                if(e.getID()==MouseEvent.MOUSE_RELEASED&&SwingUtilities.isLeftMouseButton(e)&&contains(e.getPoint())&&isEnabled()) {
                    flip.run();now=state!=null?state.getAsBoolean():!now;getAccessibleContext().setAccessibleName(said+(now?", on":", off"));repaint();e.consume();return;
                }
                super.processMouseEvent(e);
            }
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);if(state!=null)now=state.getAsBoolean();if(!now)return;
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(ACCENT);g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int x=getWidth()-28,y=getHeight()/2;g.drawPolyline(new int[]{x,x+4,x+12},new int[]{y,y+4,y-5},3);g.dispose();
            }
        };DesktopMenus.symbol(row,said);row.putClientProperty("words",said);
        row.getAccessibleContext().setAccessibleName(said+(on?", on":", off"));
        row.addActionListener(a->flip.run());
        return row;
    }

    /** Favourites and the search on Home, shown or not (docs/HOME.md, decision 47): Settings and Home's menu switch them. */
    static final String SHOW_DOCK="showDock",SHOW_SEARCH="showSearch";
    boolean showing(String which){return !"false".equals(context.getSharedPreferences("settings",0).getString(which,"true"));}
    void setShowing(String which,boolean on) {
        context.getSharedPreferences("settings",0).edit().putString(which,Boolean.toString(on)).apply();
        if(home!=null)home.showFoot();
    }

    /** Home's own colour, kept as the phone keeps it (its "colour" setting): the room Home is drawn in (decision 44). */
    static final String HOME_COLOUR="colour";
    int homeColour(){return (int)context.getSharedPreferences("settings",0).getLong(HOME_COLOUR,Tint.NONE);}
    void paintHome(int colour) {
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putLong(HOME_COLOUR,colour).apply();return null;},
            done->{if(home!=null)home.paintRoom();paintApp();washPage();},this::failed);
    }
    /**
     * Home's own strength, kept beside its colour ("homeTone", as the phone keeps it): the app's, since Home's colour is
     * the whole app's (decision 102). Never given one, it is the usual, which is what the whole pad had (decision 107).
     */
    static final String HOME_TONE="homeTone";
    int homeTone(){return Tint.tone(DesktopLook.tone(context,HOME_TONE),usual);}
    /** Kept at once, so the band that set it reads it back; Home, the bar, the side list and an uncoloured page drawn again. */
    void toneHome(int step) {
        DesktopLook.keepTone(context,HOME_TONE,step);
        if(home!=null)home.paintRoom();paintApp();washPage();
    }
    /** The bar and the side list, kept to be washed in the app's colour (see paintApp). */
    private JComponent barPanel;private JPanel sidePanel;
    /**
     * The whole app in Home's colour, not Home alone (the owner, 2026-10-05: "when we set it up on laptop, the whole app
     * color should change not only the desktop of it"), as the phone's is: the bar, the side list and its tree washed in
     * it, and the page of a note that has no colour of its own (see washPage).
     */
    void paintApp() {
        Color shelf=DesktopLook.wash(homeColour(),DesktopUi.SHELF,0.12f,0.72f,homeTone());
        if(barPanel!=null)barPanel.setBackground(shelf);
        // And the title bar the bar sits in.
        frame.getRootPane().putClientProperty(com.formdev.flatlaf.FlatClientProperties.TITLE_BAR_BACKGROUND,shelf);
        if(sidePanel!=null)sidePanel.setBackground(shelf);
        tree.setBackground(shelf);if(openList!=null)openList.setBackground(shelf);
        frame.repaint();
    }
    /** Create's rows that are the same wherever the menu is opened: files sent, Markdown, a file edited or shared. */
    private void createRows(JPopupMenu menu) {
        // Files straight to another device, belonging to no note: the ones that came that way are on Home, new until they
        // are opened; what went from here is listed under Sent files (decision 20). With what is made (decision 89).
        item(menu,"Send files…",()->DesktopDrops.send(this));
        item(menu,"Sent files…",()->DesktopDrops.sentFiles(this));
        // A Markdown file from anywhere, made a note kept as that file, where it is (docs/FILES.md).
        item(menu,"Open a Markdown file…",()->DesktopOnDisk.openFile(this));
        // A file only opened, worked in and closed, as Notepad does, and never part of Mininotes (docs/FILES.md).
        item(menu,"Edit a file…",()->DesktopEditFile.open(this));
    }

    /** Find, under its heading: Search, the Agent, the tree ticked while it shows, and everything stamped on Minima. */
    private void findRows(JPopupMenu menu) {
        heading(menu,"Find");
        menu.add(line("Search…","control F",true,()->save(()->{showHome();home.focusSearch();})));
        // A question, or something to do, said in words, answered on this PC (decision 191).
        menu.add(line("Agent…","control K",true,()->DesktopAssistant.open(this)));
        // The tree, ticked while it shows beside Home and the page: the list of everything (decision 26). The tick is drawn
        // at the right, where it moves no word: a check box's own tick pushed "Tree" out of the column the others start in.
        JMenuItem tree=ticked("Tree",treeWanted,()->showTree(!treeWanted));tree.setToolTipText("Ctrl+B");menu.add(tree);
        // Everything stamped on Minima from here, in its own window, last in Find (the owner, 2026-10-21: "Move the Stamped on
        // Minima in find section of the menu"; decision 230); + in it stamps something new, and the key is set in My profile.
        item(menu,"Stamped on Minima…",()->DesktopStamp.list(this,frame));
    }

    /** Colour ▸ for Home, with its own strength under it, as for any thing. */
    JMenu homeColourMenu(){return DesktopLook.colours(homeColour(),this::paintHome,this::homeTone,this::toneHome);}

    /**
     * Home's own rows in ⋯ while Home is in view, as the phone's ⋮ has them (the owner, 2026-10-04: "the Home should be the
     * one we have on mobile"; decision 89): what is made, what comes from another device, Sync now, every page at once, and
     * back to the main page from another. Its colour is at the top; what it shows is lower down.
     */
    void homeRows(JPopupMenu menu) {
        menu.add(line("New note","control N",true,()->save(this::newNote)));
        item(menu,"New folder",()->save(()->addShelf(null)));
        if(home!=null&&Layout.active(home.grid.spots()).size()>1)item(menu,"All pages",()->home.pages.up());
        if(home!=null&&!home.grid.onCentre())item(menu,"Back to the main page",()->home.grid.showPage(0,0));
    }

    /**
     * What Home shows, switched, under a heading of its own low in the menu (the owner, 2026-10-03: "a dedicated section in
     * the menu ... lower in the menu since these will be set once, just before the section Mininotes"; decision 80).
     */
    void onHomeRows(JPopupMenu menu) {
        // One line before it, not two where the section before ended with one.
        int count=menu.getComponentCount();
        if(count>0&&!(menu.getComponent(count-1) instanceof JPopupMenu.Separator))menu.addSeparator();
        heading(menu,"Show on Home");
        menu.add(ticked("Dock",showing(SHOW_DOCK),()->setShowing(SHOW_DOCK,!showing(SHOW_DOCK))));
        menu.add(ticked("Search",showing(SHOW_SEARCH),()->setShowing(SHOW_SEARCH,!showing(SHOW_SEARCH))));
        placesRows(menu);
    }

    /**
     * Home's places, each shown or not (the owner, 2026-10-03: "in the Home section of the menu, a toggle for showing favourites,
     * bin, archive, open notes, temp ... on the desktop directly, not in a group"; decision 78). Open is the list of what is
     * open, on the right of the window.
     */
    void placesRows(JPopupMenu menu) {
        // Recent is the list down the right on the PC, not an icon on Home (decision 86).
        boolean listed=openListWanted();menu.add(ticked("Recent",listed,()->setOpenListWanted(!openListWanted())));
        // The menu itself, pinned open on the right of Recent (the owner, 2026-10-13).
        menu.add(ticked("Menu",DesktopPinnedMenu.wanted(this),()->{
            context.getSharedPreferences("settings",0).edit().putString(DesktopPinnedMenu.SETTING,String.valueOf(!DesktopPinnedMenu.wanted(this))).apply();
            pinnedMenu.place();}));
        for(String[] place:PLACES_ON_HOME) {
            String id=place[0];boolean on=onHome(id);
            menu.add(ticked(place[1],on,()->onHome(id),()->setOnHome(id,!onHome(id))));
        }
        // Read me, which a newcomer needs and the owner of a full Home may not (decision 214).
        menu.add(ticked(NoteStore.READ_ME,store.readMeOnHome(),store::readMeOnHome,()->{store.setReadMeOnHome(!store.readMeOnHome());home.refresh();}));
    }
    /** Home's places on the PC and the words their switches say: Recent is the list on the right instead (decision 86). */
    // Shared with me a place among them, with its own switch (decision 94).
    static final String[][] PLACES_ON_HOME={{NoteStore.FAVOURITES,"Favourites"},{NoteStore.TEMP,"Temp"},{NoteStore.SHARED,"Shared with me"},{NoteStore.ARCHIVE,"Archive"},{NoteStore.BIN,"Bin"}};
    /** Whether one of Home's places stands on Home: yes unless switched off. */
    boolean onHome(String id){return !"false".equals(context.getSharedPreferences("settings",0).getString("show_"+id,"true"));}
    void setOnHome(String id,boolean on){context.getSharedPreferences("settings",0).edit().putString("show_"+id,String.valueOf(on)).apply();home.refresh();}

    /** How long what is carried onto Temp stays before it is deleted for good (decision 79): Temp's menu, and Settings. */
    // Short, as a note to self is (decision 85). Kept as minutes under a new name, so a place in the old list is never read as one in this.
    static final String[] TEMP_SPANS={"15 minutes","30 minutes","An hour","2 hours","5 hours","10 hours","24 hours"};
    static final long[] TEMP_LENGTHS={15*60_000L,30*60_000L,3_600_000L,2*3_600_000L,5*3_600_000L,10*3_600_000L,24*3_600_000L};
    static final int TEMP_USUAL=6;
    int tempSpan() {
        try {
            long minutes=Long.parseLong(context.getSharedPreferences("settings",0).getString("tempMinutes",String.valueOf(24*60)));
            for(int at=0;at<TEMP_LENGTHS.length;at++)if(TEMP_LENGTHS[at]==minutes*60_000L)return at;
        } catch(RuntimeException unread){/* the usual */}
        return TEMP_USUAL;
    }
    void setTempSpan(int at){context.getSharedPreferences("settings",0).edit().putString("tempMinutes",String.valueOf(TEMP_LENGTHS[at]/60_000L)).apply();}
    JMenu tempSpanMenu() {
        JMenu menu=new JMenu(Say.t("Things stay"));DesktopMenus.symbol(menu,"Things stay");ButtonGroup group=new ButtonGroup();int now=tempSpan();
        for(int at=0;at<TEMP_SPANS.length;at++){int which=at;JRadioButtonMenuItem one=new JRadioButtonMenuItem(Say.t(TEMP_SPANS[at]),at==now);one.addActionListener(e->setTempSpan(which));group.add(one);menu.add(one);}
        return menu;
    }
    /** Carried onto Temp: temporary for as long as Temp keeps things, with no question, said, and one Undo away. */
    void intoTemp(NoteStore.Branch thing) {
        if(thing==null||(thing.kind!=NoteStore.Branch.Kind.PAGE&&thing.kind!=NoteStore.Branch.Kind.FILE&&!isCollection(thing)))return;
        NoteStore.Branch.Kind kind=thing.kind==NoteStore.Branch.Kind.PAGE||thing.kind==NoteStore.Branch.Kind.FILE?thing.kind:NoteStore.Branch.Kind.COLLECTION;
        disk.submit(()->kind!=NoteStore.Branch.Kind.FILE&&DesktopIconPicker.readsOnly(store,kind,thing.id),reads->{
            if(reads){status.setText(Say.t("Only somebody who can write in it can make it temporary"));return;}
            setTemporary(thing,kind,System.currentTimeMillis()+TEMP_LENGTHS[tempSpan()]);
            canUndo(named(thing),()->{store.makeTemporary(kind,thing.id,0L);return null;});
        },this::failed);
    }
    /** Updating by itself switched on or off; switched on with a newer version known, it is fetched now. */
    void setAutoUpdate(boolean on) {
        context.getSharedPreferences("settings",0).edit().putString("autoUpdate",Boolean.toString(on)).apply();
        if(on&&Update.newer(newestKnown,VERSION))fetchQuietly(newestKnown);
    }
    boolean autoUpdate(){return !"false".equals(context.getSharedPreferences("settings",0).getString("autoUpdate","true"));}

    /**
     * The version in the bar, and beside it - only when there is something to do - one filled button, as an
     * editor shows it: "Restart to update" once a newer version is downloaded, "Update" while it is only known.
     */
    /** A round dot of a colour, the size of the version's letters. */
    private static Icon dot(Color colour) {
        return new Icon(){
            public void paintIcon(Component c,Graphics g0,int x,int y){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(colour);g.fillOval(x,y+1,8,8);g.dispose();}
            public int getIconWidth(){return 8;}
            public int getIconHeight(){return 10;}
        };
    }

    void updateShown() {
        boolean behind=Update.newer(newestKnown,VERSION);
        version.setFont(DesktopUi.BODY.deriveFont(13f));version.setForeground(QUIET);version.setText("v"+VERSION);
        // The dot before it: green on the newest there is, yellow while a newer one is out, none until GitHub has answered.
        Update.Standing standing=Update.standing(newestKnown,VERSION);
        version.setIcon(standing==Update.Standing.UNKNOWN?null:dot(new Color(Tint.of(standing==Update.Standing.BEHIND?3:4,false),true)));
        version.setToolTipText(standing==Update.Standing.LATEST?Say.t("Mininotes for Windows v{0}, the newest there is. Click for updates.",VERSION):standing==Update.Standing.BEHIND?Say.t("Mininotes for Windows v{0}, v{1} is out. Click for updates.",VERSION,newestKnown):Say.t("Mininotes for Windows v{0}. Click for updates.",VERSION));
        version.getAccessibleContext().setAccessibleName(version.getToolTipText());
        updateButton.setText(Say.t(staged!=null?"Restart to update":"Update"));updateButton.setVisible(staged!=null||behind);
        updateButton.setToolTipText(staged!=null?Say.t("Mininotes v{0} is downloaded and checked. Restart to use it.",stagedVersion)
            :Say.t("Mininotes v{0} is out. Click to get it.",newestKnown));
        updateButton.getAccessibleContext().setAccessibleName(updateButton.getToolTipText());
        rebar();
    }

    /**
     * Whether a newer version is out: once a day by itself, or now when asked. What was last found is kept,
     * so the bar says so from the moment Mininotes opens. Found by itself, and updating by itself is on, it
     * is downloaded straight away and waits for a restart.
     */
    void lookForUpdate(boolean asked) {
        var kept=context.getSharedPreferences("settings",0);
        newestKnown=Update.read(kept.getString("updateNewest",""));updateShown();
        long looked;try{looked=Long.parseLong(kept.getString("updateLooked","0"));}catch(NumberFormatException e){looked=0;}
        if(!asked&&!Update.due(System.currentTimeMillis(),looked)){if(Update.newer(newestKnown,VERSION)&&autoUpdate())fetchQuietly(newestKnown);return;}
        if(asked){version.setText(Say.t("Checking…"));version.setForeground(ACCENT);status.setToolTipText(null);status.setText(Say.t("Looking for a newer version…"));}
        connectivity.submit(DesktopUpdate::latest,newest->{
            kept.edit().putString("updateNewest",newest).putString("updateLooked",Long.toString(System.currentTimeMillis())).apply();
            newestKnown=newest;updateShown();
            if(Update.newer(newest,VERSION)&&autoUpdate()&&!asked)fetchQuietly(newest);
            if(asked){status.setText(" ");updatesBox(null);}
        },e->{updateShown();if(asked){status.setText(" ");updatesBox(e.getMessage());}});
    }

    /** Downloaded and checked in the background; the bar then offers the restart. Failing is quiet: it is tried again. */
    private void fetchQuietly(String newest) {
        Path app=DesktopUpdate.appFolder();
        if(app==null||fetching||(staged!=null&&stagedVersion.equals(newest)))return;
        fetching=true;
        connectivity.submit(()->DesktopUpdate.fetch(newest,app,p->{}),opened->{fetching=false;staged=opened;stagedVersion=newest;updateShown();
            status.setText(Say.t("Mininotes v{0} is ready. Restart to update, or it is put in place when you close Mininotes.",newest));},
            e->fetching=false);
    }

    /** The newer version put in place of this one, and Mininotes opened again on it. */
    private void restartToUpdate() {
        Path app=DesktopUpdate.appFolder();
        if(app==null||staged==null)return;
        try{DesktopUpdate.replaceAfterExit(app,staged);staged=null;save(()->shutdown(true));}
        catch(Exception e){failed(e);}
    }

    /**
     * Everything about updates in one box, opened from the version: which version this is, whether a newer
     * one is out - or why that could not be known - the one thing to do about it, and the switch for doing it
     * by itself.
     */
    void updatesBox(String problem) {
        Path app=DesktopUpdate.appFolder();
        boolean behind=Update.newer(newestKnown,VERSION);
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        String said=staged!=null?"Mininotes v"+stagedVersion+" is downloaded and checked. Restart to use it; your notes stay as they are."
            :problem!=null?problem
            :behind?"Mininotes v"+newestKnown+" is available. You have v"+VERSION+"."
            :"You have the newest version, v"+VERSION+".";
        DesktopUi.add(body,DesktopUi.note(said,400,problem!=null?DesktopUi.WARN:DesktopUi.INK,DesktopUi.BODY));
        if(behind&&staged==null&&app==null){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.note("This copy is not the packaged app, so it cannot replace itself: get the new version from the releases page.",400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));}
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.Text progress=DesktopUi.quiet(" ");DesktopUi.add(body,progress);
        JCheckBox auto=DesktopUi.toggle("Update automatically",autoUpdate());
        auto.addActionListener(e->{context.getSharedPreferences("settings",0).edit().putString("autoUpdate",Boolean.toString(auto.isSelected())).apply();
            if(auto.isSelected()&&Update.newer(newestKnown,VERSION))fetchQuietly(newestKnown);});
        JPanel card=DesktopUi.column();card.add(DesktopUi.switchRow("Update automatically",auto));
        DesktopUi.add(card,DesktopUi.note("New versions are downloaded from the Mininotes releases on GitHub, checked, and put in place when you restart or close Mininotes.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.add(body,DesktopUi.card(null,card));
        JButton go=null;
        if(staged!=null)go=DesktopUi.primary("Restart to update",()->{box[0].dispose();restartToUpdate();});
        else if(behind&&app==null)go=DesktopUi.primary("Open the releases page",()->{try{java.awt.Desktop.getDesktop().browse(java.net.URI.create(DesktopUpdate.RELEASES));}catch(Exception e){failed(e);}box[0].dispose();});
        else if(behind) {
            JButton[] self={null};
            go=DesktopUi.primary("Update now",()->{
                self[0].setEnabled(false);progress.setForeground(DesktopUi.QUIET);progress.setText(Say.t("Downloading…"));
                String newest=newestKnown;
                new SwingWorker<Path,Integer>(){
                    protected Path doInBackground() throws Exception{return DesktopUpdate.fetch(newest,app,p->publish(p));}
                    protected void process(java.util.List<Integer> got){int p=got.get(got.size()-1);progress.setText(p<0?Say.t("Downloading…"):Say.t("Downloading…")+" "+p+"%");}
                    protected void done(){
                        try{staged=get();stagedVersion=newest;updateShown();progress.setText(Say.t("Checked. Mininotes closes and opens again with v{0}…",newest));box[0].dispose();restartToUpdate();}
                        catch(Exception e){Throwable why=e.getCause()!=null?e.getCause():e;progress.setForeground(DesktopUi.WARN);
                            progress.setText(Say.t(why.getMessage()!=null?why.getMessage():"The update could not be installed. This version is unchanged."));self[0].setEnabled(true);}
                    }
                }.execute();
            });
            self[0]=go;
        } else if(problem!=null)go=DesktopUi.button("Try again",()->{box[0].dispose();lookForUpdate(true);});
        box[0]=DesktopUi.sheet(frame,"Updates",body,go==null?null:DesktopUi.footer(go),true);
        if(go!=null)box[0].getRootPane().setDefaultButton(go);
        DesktopUi.show(box[0],460,460);
    }

    /** Kept for the gallery and anything that already knows a newer version: the same box. */
    void updateBox(String newest){newestKnown=newest;updateShown();updatesBox(null);}

    /** Closing with a newer version waiting: it is put in place as Mininotes goes, and not opened. */
    void updateOnExit() {
        if(staged==null)return;
        Path app=DesktopUpdate.appFolder();
        if(app==null||staged==null)return;
        // Throwable, not Exception: nothing about an update may stop Mininotes from closing. The packaged app once
        // shipped without the part of Java the updater needs, and this line then threw an Error that ended every
        // close half-way - the window stayed, and a newer version could never take over.
        try{DesktopUpdate.replaceAfterExit(app,staged,ProcessHandle.current().pid(),false);staged=null;}catch(Throwable e){/* the next start downloads it again */}
    }

    /**
     * Uninstall Mininotes… agreed to (see DesktopUninstall): everything saved, the script that uninstalls started, and
     * Mininotes closed for it to begin. A newer version waiting is not put in place of what is going.
     */
    void uninstallNow(DesktopUninstall.Plan plan) {
        save(()->{
            try{DesktopUninstall.start(plan);}catch(Exception e){failed(e);return;}
            staged=null;shutdown(true);
        });
    }

    /**
     * A backup proposed, quietly (decision 235): at most once a start, a line on the status strip, never a box, when this
     * notebook has notes and no backup was saved here in 30 days (counted from when this version first opened it, for a
     * notebook never backed up). Clicked, it is the backup. Said only while the strip has nothing else to say.
     */
    private void remindBackup() {
        var kept=context.getSharedPreferences("settings",0);long now=System.currentTimeMillis();
        if(kept.getString(DesktopBackup.FIRST_SEEN,null)==null)kept.edit().putString(DesktopBackup.FIRST_SEEN,Long.toString(now)).apply();
        long last=DesktopBackup.number(kept.getString(DesktopBackup.BACKED_UP,"0")),since=DesktopBackup.number(kept.getString(DesktopBackup.FIRST_SEEN,"0"));
        disk.submit(()->store.latest()!=null,any->{
            if(!DesktopBackup.due(now,last,since,any))return;
            javax.swing.Timer later=new javax.swing.Timer(10_000,e->{
                if(closing||statusStrip==null||statusStrip.isVisible())return;
                String said=Say.t(DesktopBackup.REMIND);
                status.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent click){
                    if(!said.equals(status.getText()))return;
                    status.removeMouseListener(this);status.setCursor(Cursor.getDefaultCursor());status.setText(" ");save(Desktop.this::backup);}});
                status.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));status.setToolTipText(null);status.setText(said);
            });
            later.setRepeats(false);later.start();
        },e->{});
    }

    void securityShown() {
        boolean on=DesktopLock.locked(context.getFilesDir().toPath());
        security.setIcon(DesktopLock.padlock(on,15));security.setText(Say.t(on?"Encrypted":"Not encrypted"));
        security.setForeground(on?ACCENT:QUIET);security.setToolTipText(Say.t(on?"Your notes on this PC are encrypted with a password. Click for Security.":"Your notes on this PC are not encrypted. Click to lock them with a password."));
        security.getAccessibleContext().setAccessibleName(security.getText());
    }
    /** The Minima address donations go to - the same one the phone's About shows. */
    static final String DONATE="MxG087BNANGRNYJAAKU73AHE0YSUE9NYQU1PJCFK4E5J6010W5U68ABKKGHS7AQ";
    void about() {
        JPanel body=DesktopUi.column();
        JLabel brand=new JLabel("Mininotes",new ImageIcon(DesktopIcon.image(48)),SwingConstants.LEFT);brand.setIconTextGap(14);brand.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,20f));brand.setForeground(INK);
        DesktopUi.add(body,brand);DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.quiet("Windows v"+VERSION+" · preview"));DesktopUi.gap(body,DesktopUi.M);
        // What it is and where it is from, said as what it is; every switch is in Settings.
        DesktopUi.Text words=DesktopUi.note("A paper pad for the people close to you, shared over Maxima, the communication layer of Minima.\n\n"+(DesktopLock.locked(context.getFilesDir().toPath())?"Your notes on this PC are locked and encrypted.":"Lock Mininotes in Settings to encrypt your notes on this PC.")+" Free software under the GNU GPL. Its icons are Lucide's, under the ISC licence.");
        DesktopUi.add(body,words);DesktopUi.gap(body,DesktopUi.M);
        // The other app, named with the menu entry that tells of it (decision 241).
        DesktopUi.add(body,DesktopUi.note("Mininotes is also on Android phones: Mininotes on your phone…, in the menu."));DesktopUi.gap(body,DesktopUi.M);
        // Donate, as on the phone: the Minima address in full, and a button that copies it.
        DesktopUi.add(body,DesktopUi.quiet("Donate"));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.note(DONATE,380,INK,DesktopUi.BODY.deriveFont(12.5f)));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Copy the address",()->{
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(DONATE),null);
            status.setText(Say.t("Donation address copied"));})));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Source code",()->{try{java.awt.Desktop.getDesktop().browse(java.net.URI.create(DesktopUpdate.SOURCE));}catch(Exception e){failed(e);}}),
            DesktopUi.button("Check for updates",()->lookForUpdate(true))));
        DesktopUi.tell(frame,"About",body);
    }
    /** The language chosen, as kept beside the notebook (not a secret, and needed before it is unlocked); system where none. */
    /**
     * At the very first start of a new install, before any notebook is made (the owner, 2026-10-21: "When we install the app
     * for the first time, there should be a step where we set the language. So the user gets everything in the language of
     * its choice, including the README right away"; decision 232): the languages, each in its own words, this PC's own first
     * and chosen; kept beside the notebook as a language chosen in Settings is. Asked once: a folder with a notebook or a
     * language in it never is. Whether a language was chosen.
     *
     * <p>And at every start after, until a language is confirmed once (the owner, 2026-10-24: "When launching for the first
     * time, or until the language hasn't been confirmed, a pop up window should ask the user to pick his language"; decision
     * 240): a folder in use is asked too, with the language it uses now first and chosen (not this PC's). Continue confirms it
     * (the file {@link LanguageAsk#CONFIRMED_FILE} beside the notebook, as a language chosen in Settings does); closed
     * without, it asks again next time. Asked before the window is built, so another language picked here needs no restart:
     * the notebook takes it in when it opens ({@link #languageAtStart}), README said again in it. Whether the language changed
     * (or, for a new folder, was chosen), so main reads it again.
     */
    static boolean firstLanguage(Path folder) {
        if(Files.exists(folder.resolve(LanguageAsk.CONFIRMED_FILE)))return false;
        boolean inUse=Files.exists(folder.resolve("mininotes.db"));
        if(LanguageAsk.when(false,inUse,false)==LanguageAsk.When.NEVER)return false;
        String setting=languageChosen(folder),now=Say.resolve(setting,java.util.Locale.getDefault());
        java.util.List<String> codes=LanguageAsk.order(now,Say.LANGUAGES.keySet());
        String[] chosen={now};boolean[] confirmed={false};
        // In English, whatever the PC is set to, while it is open (decision 254).
        try{SwingUtilities.invokeAndWait(()->LanguageAsk.inEnglish(()->{
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.body(Say.t("Choose the language Mininotes speaks. It can be changed later in Settings.")));DesktopUi.gap(body,DesktopUi.S);
            ButtonGroup group=new ButtonGroup();
            for(String code:codes){JRadioButton one=new JRadioButton(Say.LANGUAGES.get(code),code.equals(now));one.setName("language "+code);one.setFont(DesktopUi.fontFor(Say.LANGUAGES.get(code),DesktopUi.BODY));
                one.setOpaque(false);one.addActionListener(e->chosen[0]=code);group.add(one);DesktopUi.add(body,one);}
            JDialog[] box={null};
            box[0]=DesktopUi.sheet(null,Say.t("Language"),DesktopUi.scrolling(body),DesktopUi.footer(DesktopUi.primary("Continue",()->{confirmed[0]=true;box[0].dispose();})),true);
            box[0].setName("firstLanguage");DesktopUi.show(box[0],440,620);
            return null;
        }));}catch(Exception noScreen){return false;}
        // Closed without Continue: a folder in use stays as it was, and is asked again at the next start.
        if(!confirmed[0]&&inUse)return false;
        // The language in use, picked again, keeps its setting ("this PC's language" goes on following Windows); another, as itself.
        String keep=LanguageAsk.kept(chosen[0],setting,now);
        try{Files.createDirectories(folder);
            Files.writeString(folder.resolve(LANGUAGE_FILE),keep,java.nio.charset.StandardCharsets.UTF_8);
            if(confirmed[0])Files.writeString(folder.resolve(LanguageAsk.CONFIRMED_FILE),keep,java.nio.charset.StandardCharsets.UTF_8);}
        catch(IOException|RuntimeException notKept){return false;}
        if(!inUse)return true;
        if(keep.equals(setting))return false;
        languageAtStart=keep;return true;
    }
    /**
     * A language picked in the box at this start for a notebook in use (decision 240), or null: the notebook's own setting
     * would otherwise win when it opens, and README, maybe never said in a language before, is to be said in it again.
     */
    static volatile String languageAtStart;
    /** A language confirmed, beside the notebook: the box at the start no longer asks (decision 240). */
    static void languageConfirmed(Path folder,String code) {
        try{Files.writeString(folder.resolve(LanguageAsk.CONFIRMED_FILE),code,java.nio.charset.StandardCharsets.UTF_8);}catch(IOException|RuntimeException notKept){/* asked again at the next start */}
    }

    static String languageChosen(Path folder) {
        try{String said=Files.readString(folder.resolve(LANGUAGE_FILE),java.nio.charset.StandardCharsets.UTF_8).trim();return said.isEmpty()?Say.SYSTEM:said;}
        catch(IOException|RuntimeException none){return Say.SYSTEM;}
    }
    static final String LANGUAGE_FILE="language";
    /**
     * A language chosen: kept, and Mininotes started again in it, once asked (the owner, 2026-10-12: "switching to French on
     * desktop doesn't work" - it said so only on the status line, and the window stayed as it was).
     */
    void chooseLanguage(String code) {
        context.getSharedPreferences("settings",0).edit().putString("language",code).apply();
        try{Files.writeString(context.getFilesDir().toPath().resolve(LANGUAGE_FILE),code,java.nio.charset.StandardCharsets.UTF_8);}catch(IOException|RuntimeException kept){/* the setting is kept */}
        // Chosen in Settings or the menu, a language is confirmed: the box at the start no longer asks (decision 240).
        languageConfirmed(context.getFilesDir().toPath(),code);
        String name=Say.SYSTEM.equals(code)?Say.t("This PC's language"):Say.LANGUAGES.getOrDefault(code,code);
        if(!DesktopUi.confirm(frame,Say.t("Restart Mininotes?"),Say.t("Mininotes opens again in {0}. What you write is saved first.",name),Say.t("Restart"),false)) {
            status.setText(Say.t("Open Mininotes again to see it everywhere."));return;
        }
        restartNow();
    }
    /** Saved, closed, and opened again by itself, as an update restarts it. Where this is not the packaged app, it only says so. */
    private void restartNow() {
        String exe=ProcessHandle.current().info().command().orElse("");
        if(!exe.toLowerCase(java.util.Locale.ROOT).endsWith("mininotes.exe")){status.setText(Say.t("Open Mininotes again to see it everywhere."));return;}
        try{DesktopUpdate.startAfterExit(Path.of(exe));save(()->shutdown(true));}catch(Exception e){failed(e);}
    }

    /** Language ▸: this PC's own, then each there is a list for; taken in when Mininotes opens again, as it says. */
    JMenu languageMenu() {
        String now=context.getSharedPreferences("settings",0).getString("language",Say.SYSTEM);
        JMenu languages=new JMenu(Say.t("Language")+"  ·  "+Say.LANGUAGES.getOrDefault(Say.language(),"English"));
        DesktopMenus.symbol(languages,"Language");
        java.util.Map<String,String> all=new java.util.LinkedHashMap<>();all.put(Say.SYSTEM,Say.t("This PC's language"));all.putAll(Say.LANGUAGES);
        ButtonGroup one=new ButtonGroup();
        for(java.util.Map.Entry<String,String> language:all.entrySet()) {
            JRadioButtonMenuItem line=new JRadioButtonMenuItem(language.getValue(),language.getKey().equals(now));one.add(line);
            // Each name in its own script, in a face that can draw it.
            line.setFont(DesktopUi.fontFor(language.getValue(),line.getFont()));
            line.addActionListener(e->chooseLanguage(language.getKey()));
            languages.add(line);
        }
        return languages;
    }

    private void item(JPopupMenu menu,String label,Runnable go){JMenuItem item=DesktopMenus.item(label);item.addActionListener(e->go.run());menu.add(item);}
    /**
     * A new collection inside {@code parent}, or on Home where that is null: made as Untitled, and its card opened with
     * the name ready to be typed over, as the + makes one.
     */
    private void addShelf(NoteStore.Branch parent) {
        boolean top=parent==null||parent.kind==NoteStore.Branch.Kind.LIBRARY;
        home.newCollection(top?Things.HOME:parent.id);
    }
    /**
     * Where New collection in… puts one: the collection whose card is up, or the one the open note is in. Null on Home,
     * which New collection makes already.
     */
    private NoteStore.Branch shelfParent() {
        NoteStore.Branch card=onPage?null:home.folder.shown();
        if(card!=null)return card;
        NoteStore.Branch holder=onPage&&base!=null?find(base.book):null;
        return isCollection(holder)?holder:null;
    }
    /** A collection, at any depth: every one is COLLECTION now, and a line made from an older one may say BOOK. */
    private static boolean isCollection(NoteStore.Branch thing){return thing!=null&&DesktopMoving.shelf(thing.kind);}
    private static String named(NoteStore.Branch thing){return DesktopHome.named(thing);}
    /** The open note put in the archive or the bin: out of the overview, and Home is what is left. */
    private void putAway(boolean bin) {
        if(base==null)return;String id=base.id,name=title.getText();
        // The open note is still an icon on its grid: pin that grid too, so binning it does not close the gap (decision 119).
        Runnable pin=home==null?null:home.stayPut(id);
        disk.submit(()->{if(pin!=null)pin.run();store.putAway(NoteStore.Branch.Kind.PAGE,id,bin,true);return null;},done->{
            canUndo(name,()->{store.restore(NoteStore.Branch.Kind.PAGE,id);return null;});
            status.setText(Say.t(bin?"Moved to the bin":"Archived"));
            overview.forget(id);carets.remove(id);showHome();if(base!=null&&base.id.equals(id))base=null;refresh();},this::failed);
    }
    /**
     * The bin or the archive, whole: each thing with Put back beside it - and, in the bin, Delete for good -
     * and Empty the bin at the foot. The same choices as on the phone.
     */
    void restore(boolean bin) {
        disk.submit(()->store.heldIn(bin),rows->{
            if(rows.isEmpty()){status.setText(Say.t(bin?"The bin is empty":"The archive is empty"));return;}
            JDialog[] box={null};JPanel list=DesktopUi.column();
            for(NoteStore.Branch row:rows) {
                String kind=row.kind==NoteStore.Branch.Kind.PAGE?"note":"folder";
                JButton back=button("Put back",()->disk.submit(()->{store.restore(row.kind,row.id);return null;},done->{box[0].dispose();status.setText(Say.t("Put back"));refresh();restore(bin);},this::failed));
                JButton gone=bin?button("Delete for good",()->{
                    if(!DesktopUi.confirm(box[0],"Delete for good?","“"+row.name+"” and everything in it is deleted from this PC. It cannot be put back.","Delete for good",true))return;
                    disk.submit(()->{store.erase(row.kind,row.id);return null;},done->{box[0].dispose();status.setText(Say.t("Deleted for good"));refresh();restore(true);},this::failed);
                }):null;
                JPanel right=new JPanel(new GridBagLayout());right.setOpaque(false);right.add(DesktopUi.actions(back,gone));
                JPanel line=DesktopUi.row(DesktopUi.person(row.name.isBlank()?"Untitled":row.name,kind),right);
                DesktopMenus.echoOnRightClick(line,back,null,gone);list.add(line);
            }
            JButton empty=bin?DesktopUi.danger("Empty the bin",()->{
                if(!DesktopUi.confirm(box[0],"Empty the bin?","Everything in the bin is deleted from this PC. It cannot be put back.","Empty the bin",true))return;
                disk.submit(store::emptyBin,n->{box[0].dispose();status.setText(n==1?Say.t("1 thing deleted for good"):Say.t("{0} things deleted for good",n));refresh();},this::failed);
            }):null;
            box[0]=DesktopUi.sheet(frame,bin?"Bin":"Archive",DesktopUi.scrolling(DesktopUi.card(null,list)),empty==null?null:DesktopUi.footer(empty),true);
            DesktopUi.show(box[0],560,640);
        },this::failed);
    }

    /** Empty the bin, from the bin's icon or its card: how many things go is said first, since it cannot be put back. */
    void askEmptyBin() {
        disk.submit(()->store.awayCount(true),n->{
            if(n==0){status.setToolTipText(null);status.setText(Say.t("The bin is already empty"));return;}
            if(!DesktopUi.confirm(frame,"Empty the bin?",(n==1?"1 thing":n+" things")+" in it, and whatever they hold, deleted from this PC. It cannot be put back.","Empty the bin",true))return;
            disk.submit(store::emptyBin,gone->{status.setToolTipText(null);status.setText(gone==1?Say.t("1 thing deleted for good"):Say.t("{0} things deleted for good",gone));refresh();},this::failed);
        },this::failed);
    }

    /** One thing in the bin gone for good, after one question, from its icon in the bin's card. */
    void eraseForGood(NoteStore.Branch thing) {
        String name=thing.name==null||thing.name.isBlank()?"Untitled":thing.name;
        if(!DesktopUi.confirm(frame,"Delete for good?","“"+name+"” and everything in it is deleted from this PC. It cannot be put back.","Delete for good",true))return;
        disk.submit(()->{store.erase(thing.kind,thing.id);return null;},done->{overview.forget(thing.id);status.setToolTipText(null);status.setText(Say.t("Deleted for good"));refresh();},this::failed);
    }

    /** Where the archive and the bin are: icons on Home (decision 41), unless switched off - then ⋯ → Put away, as before. */
    static final String AWAY_ON_HOME="awayOnHome";
    boolean awayOnHome(){return !"false".equals(context.getSharedPreferences("settings",0).getString(AWAY_ON_HOME,"true"));}
    /** How many days Recent lists what was opened (Settings, Recent keeps). */
    int recentDays(){try{return Integer.parseInt(context.getSharedPreferences("settings",0).getString("recentDays","7"));}catch(RuntimeException unread){return 7;}}
    void setRecentDays(int days){context.getSharedPreferences("settings",0).edit().putString("recentDays",String.valueOf(days)).apply();home.refresh();openList.refresh();}

    /**
     * Temporary…, from a thing's menu or carried onto Temp (decision 71): for how long, and then it is deleted for good on
     * every device that has it. Already temporary: how long it has, another time, or not temporary any more.
     */
    void temporaryBox(NoteStore.Branch thing) {
        if(thing==null||(thing.kind!=NoteStore.Branch.Kind.PAGE&&thing.kind!=NoteStore.Branch.Kind.FILE&&!isCollection(thing)))return;
        NoteStore.Branch.Kind kind=thing.kind==NoteStore.Branch.Kind.PAGE||thing.kind==NoteStore.Branch.Kind.FILE?thing.kind:NoteStore.Branch.Kind.COLLECTION;
        disk.submit(()->new Object[]{store.untilOf(kind,thing.id),kind!=NoteStore.Branch.Kind.FILE&&DesktopIconPicker.readsOnly(store,kind,thing.id)},got->{
            long until=(Long)got[0];
            if((Boolean)got[1]){status.setText(Say.t("Only somebody who can write in it can make it temporary"));return;}
            String name=named(thing);
            if(until>0) {
                int answer=DesktopUi.confirmOr(frame,"Temporary: "+name,NoteStore.goneIn(until,System.currentTimeMillis())+", for everybody who has it.","Change","Not temporary");
                if(answer==2){setTemporary(thing,kind,0L);return;}
                if(answer!=1)return;
            }
            String[] spans=TEMP_SPANS;long[] lengths=TEMP_LENGTHS;
            int at=DesktopUi.choose(frame,(until>0?"Temporary: ":"Make temporary: ")+name,
                "Gone in, and then deleted for good, for everybody who has it on Mininotes 0.2.016 or later.",spans,tempSpan(),until>0?"Change":"Make temporary");
            if(at>=0)setTemporary(thing,kind,System.currentTimeMillis()+lengths[at]);
        },this::failed);
    }
    /**
     * Temp as a note to self (decision 84): what is let go on it goes to this owner's other devices too, unless switched off
     * in Temp's menu or Settings.
     */
    boolean tempToMine(){return !"false".equals(context.getSharedPreferences("settings",0).getString("tempToMine","true"));}
    void setTempToMine(boolean on){context.getSharedPreferences("settings",0).edit().putString("tempToMine",String.valueOf(on)).apply();if(on)tempSyncNow();}
    /** Every temporary thing given to this owner's other devices where it was not, then everything sent now: Temp's Sync now. */
    void tempSyncNow() {
        status.setToolTipText(null);status.setText(Say.t("Sending Temp to your devices…"));
        disk.submit(()->{
            List<String> files=new ArrayList<>();
            for(NoteStore.Branch one:store.temporary(System.currentTimeMillis())) {
                boolean file=one.kind==NoteStore.Branch.Kind.FILE;
                store.toMyDevices(one.kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:file?Sharing.Scope.FILE:Sharing.Scope.COLLECTION,one.id);
                if(file)files.add(one.id);
            }
            return files;
        },files->{refresh();for(String one:files)fileToMine(one);sync(true);},this::failed);
    }
    /** Whether what comes on Temp from my other devices is on Home too (decision 98): those already here move with it. */
    /**
     * Shrink the pictures already here… (decision 112): looked at first, the status line saying how far it has got; then
     * asked, with how much it frees, since an original cannot be had back; then done, and said. Closing the box replaces
     * nothing.
     */
    void shrinkHere() {
        status.setToolTipText(null);status.setText(Say.t(Shrink.LOOKING));
        disk.submit(()->store.shrinkLook((done,of)->SwingUtilities.invokeLater(()->status.setText(Say.t(Shrink.looking(done,of))))),look->{
            int n=look.count();
            if(n==0){status.setText(Say.t(Shrink.UNCHANGED));DesktopUi.tell(frame,Shrink.SWITCH,DesktopUi.note(Shrink.nothing(look.left),380,DesktopUi.INK,DesktopUi.BODY));return;}
            if(!DesktopUi.confirm(frame,Shrink.asking(n),Shrink.asked(n,look.before,look.after,look.left),Shrink.yes(n),true)) {
                disk.submit(()->{store.shrinkForget();return null;},done->status.setText(Say.t(Shrink.UNCHANGED)),this::failed);return;
            }
            status.setText(Say.t(Shrink.shrinking(n)));
            disk.submit(()->store.shrinkDone(look),freed->{status.setText(Say.t(Shrink.shrunk(n,freed)));refresh();},this::failed);
        },this::failed);
    }
    void setTempOnHome(boolean on){disk.submit(()->{store.setTempOnHome(on);return null;},done->refresh(),this::failed);}
    /** A file given to this owner's other devices, sent now: its bytes go up, and its sleeve once they are (decision 95). */
    void fileToMine(String file){network.submit(()->Post.fileChanged(context,store,keys,file),done->{},e->{});}
    /**
     * Which of this owner's devices Temp goes to (decision 150): every one, as it always went, or those ticked. Kept on this
     * PC; what is already in Temp is sent to any newly ticked one at once.
     */
    void chooseTempDevices() {
        disk.submit(()->{
            java.util.List<NoteStore.Contact> mine=new ArrayList<>(),people=new ArrayList<>();
            for(NoteStore.Contact one:store.addresses())if(one.paired())(one.mine?mine:people).add(one);
            return new Object[]{mine,store.tempDevices(),people,store.tempPeople()};
        },got->{
            @SuppressWarnings("unchecked") java.util.List<NoteStore.Contact> mine=(java.util.List<NoteStore.Contact>)got[0];
            @SuppressWarnings("unchecked") java.util.Set<String> chosen=(java.util.Set<String>)got[1];
            @SuppressWarnings("unchecked") java.util.List<NoteStore.Contact> people=(java.util.List<NoteStore.Contact>)got[2];
            @SuppressWarnings("unchecked") java.util.Set<String> chosenPeople=(java.util.Set<String>)got[3];
            if(mine.isEmpty()&&people.isEmpty()){status.setText(Say.t("Nobody added yet. Add someone or your other device in People and devices."));return;}
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.note("What you let go on Temp, or drop on it, goes to everyone ticked here, with Temp's time.",380,QUIET,DesktopUi.BODY.deriveFont(13f)));
            java.util.List<JCheckBox> boxes=new ArrayList<>(),peopleBoxes=new ArrayList<>();
            if(!mine.isEmpty()){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.body("My devices"));}
            for(NoteStore.Contact one:mine){JCheckBox tick=new JCheckBox(one.name,chosen==null||chosen.contains(one.address));tick.setOpaque(false);tick.putClientProperty("address",one.address);boxes.add(tick);DesktopUi.add(body,tick);}
            if(!people.isEmpty()){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.body("People"));
                DesktopUi.add(body,DesktopUi.note("They can read it, not change it.",380,QUIET,DesktopUi.BODY.deriveFont(12.5f)));}
            for(NoteStore.Contact one:people){JCheckBox tick=new JCheckBox(one.name,chosenPeople.contains(one.address));tick.setOpaque(false);tick.putClientProperty("address",one.address);peopleBoxes.add(tick);DesktopUi.add(body,tick);}
            JDialog[] box={null};
            JButton save=DesktopUi.primary("Save",()->{
                java.util.Set<String> now=new java.util.LinkedHashSet<>(),nowPeople=new java.util.LinkedHashSet<>();
                for(JCheckBox tick:boxes)if(tick.isSelected())now.add((String)tick.getClientProperty("address"));
                for(JCheckBox tick:peopleBoxes)if(tick.isSelected())nowPeople.add((String)tick.getClientProperty("address"));
                boolean all=now.size()==mine.size();int count=now.size()+nowPeople.size();
                box[0].dispose();
                disk.submit(()->{store.setTempDevices(all?null:now);store.setTempPeople(nowPeople);return null;},done->{status.setText(Say.t(count==1?"Temp goes to {0} device or person":"Temp goes to {0} devices and people",count));tempSyncNow();},this::failed);
            });
            box[0]=DesktopUi.sheet(frame,"Temp goes to",body,DesktopUi.footer(save),true);
            DesktopUi.show(box[0],420,480);
        },this::failed);
    }
    /** Temp's own rows in its right-click: how long things stay, whether they go to this owner's other devices, and sending now. */
    void tempRows(JPopupMenu menu) {
        menu.add(tempSpanMenu());
        boolean on=tempToMine();menu.add(ticked("Send what is in Temp",on,()->setTempToMine(!tempToMine())));
        // To whom: devices of mine and people (the owner, 2026-10-11; decisions 150 and 151).
        if(on)item(menu,"Who it goes to\u2026",this::chooseTempDevices);
        boolean home=store.tempOnHome();menu.add(ticked("Also show on Home",home,()->setTempOnHome(!store.tempOnHome())));
        item(menu,"Sync Temp now",this::tempSyncNow);
    }

    private void setTemporary(NoteStore.Branch thing,NoteStore.Branch.Kind kind,long until) {
        // A file on Temp goes to this owner's other devices too, where Temp sends there, with its time (decision 95).
        if(kind==NoteStore.Branch.Kind.FILE) {
            boolean mine=until>0&&tempToMine();
            disk.submit(()->{store.makeTemporary(kind,thing.id,until);return mine&&store.toMyDevices(Sharing.Scope.FILE,thing.id)>=0;},sends->{
                status.setToolTipText(null);status.setText(Say.t(until>0?NoteStore.goneIn(until,System.currentTimeMillis()):"Not temporary any more"));refresh();
                if(sends)fileToMine(thing.id);
            },this::failed);
            return;
        }
        Sharing.Scope scope=kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:Sharing.Scope.COLLECTION;boolean toMine=until>0&&tempToMine();
        disk.submit(()->{store.makeTemporary(kind,thing.id,until);if(toMine)store.toMyDevices(scope,thing.id);return null;},done->{
            status.setToolTipText(null);status.setText(until>0?Say.t("{0}, for everybody",Say.t(NoteStore.goneIn(until,System.currentTimeMillis()))):Say.t("Not temporary any more"));
            refresh();
            // A note's time goes with it, as its words do: now, to whoever has it.
            sendChanged(kind,thing.id);
        },this::failed);
    }
    void setAwayOnHome(boolean on) {
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString(AWAY_ON_HOME,Boolean.toString(on)).apply();return null;},done->refresh(),this::failed);
    }
    void versions() {
        if(base==null)return;String id=base.id;
        disk.submit(()->store.versions(id),all->{
            if(all.isEmpty()){status.setText(Say.t("No earlier versions yet"));return;}
            // Each version by when it was kept and who it came from; what it said, in full, beside it.
            java.time.format.DateTimeFormatter when=java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss");
            String[] labels=all.stream().map(v->when.format(java.time.Instant.ofEpochMilli(v.at).atZone(java.time.ZoneId.systemDefault()))).toArray(String[]::new);
            JList<String> list=new JList<>(labels);list.setSelectedIndex(0);list.setFixedCellHeight(36);list.setBackground(DesktopUi.CARD);list.setCellRenderer(DesktopUi.roomy());
            JTextArea preview=new JTextArea(16,40);preview.setLineWrap(true);preview.setWrapStyleWord(true);preview.setEditable(false);preview.setText(all.get(0).body);preview.setCaretPosition(0);
            preview.setFont(DesktopUi.BODY.deriveFont(15f));preview.setBackground(DesktopUi.CARD);preview.setMargin(new Insets(12,14,12,14));
            list.addListSelectionListener(e->{if(list.getSelectedIndex()>=0){preview.setText(all.get(list.getSelectedIndex()).body);preview.setCaretPosition(0);}});
            JScrollPane left=new JScrollPane(list);left.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));left.setPreferredSize(new Dimension(210,360));
            JScrollPane right=new JScrollPane(preview);right.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));
            JPanel content=new JPanel(new BorderLayout(12,0));content.setOpaque(false);content.add(left,BorderLayout.WEST);content.add(right);
            JDialog[] box={null};boolean[] restore={false};
            JButton back=DesktopUi.primary("Put this version back",()->{restore[0]=true;box[0].dispose();});back.setEnabled(page.isEditable());
            box[0]=DesktopUi.sheet(frame,"Versions",content,DesktopUi.footer(back),true);
            DesktopUi.show(box[0],760,640);
            if(restore[0]&&page.isEditable()){var old=all.get(list.getSelectedIndex());putBackOnPage(old.title,old.body);status.setText(Say.t("Version put back. The text it replaced is kept as a version."));}
        },this::failed);
    }
    /** A version's title and text put back on the open page, as Versions does. */
    private void putBackOnPage(String oldTitle,String oldBody) {
        title.setText(oldTitle);
        // Put back, not written: what is still on the page keeps its writer, and what comes back is nobody's.
        // The version put back whole, on the same tab where it has it; who wrote what follows the letters it kept.
        String wasWhole=noteNow();Writers hadWhole=noteWriters();String was=page.getText();Writers had=page.writers();
        drawing=true;split(oldBody,Writers.follow(oldBody,Writers.UNKNOWN,wasWhole,hadWhole),tabNamed(oldBody,tabName(),tab));drawing=false;
        page.setText(tabWords);page.putBack(was,had);drawTabs();save(null);
    }
    /**
     * A note's stamped version put back as its text (decision 218): what it says now kept as a version first, as Versions
     * does. On the page where the note is open, through it, so who wrote what follows; otherwise in the notebook, and sent.
     */
    void putStampedBack(long n,String id,String oldTitle,String oldBody) {
        if(base!=null&&base.id.equals(id)) {
            if(!page.isEditable()){DesktopStamp.notice(this,Say.t("Read only. This note is somebody else's to change."));return;}
            save(()->disk.submit(()->{store.keepVersion(id,"");return null;},kept->{
                if(base==null||!base.id.equals(id))return;
                putBackOnPage(oldTitle,oldBody);DesktopStamp.notice(this,Say.t("The stamped version is put back. The text it replaced is kept as a version."));
            },this::failed));
            return;
        }
        disk.submit(()->store.putStampedBack(n),done->{
            if(!done){DesktopStamp.notice(this,Say.t("Read only. This note is somebody else's to change."));return;}
            DesktopStamp.notice(this,Say.t("The stamped version is put back. The text it replaced is kept as a version."));
            refresh();sendChanged(NoteStore.Branch.Kind.PAGE,id);
        },this::failed);
    }
    void recoveryWriting() {save(this::showRecoveryWriting);}
    private void showRecoveryWriting() {
        disk.submit(()->DesktopRecovery.all(store),drafts->{
            if(dirty||saving){save(this::showRecoveryWriting);return;}
            JPanel body=DesktopUi.column();JDialog[] box={null};
            DesktopUi.add(body,DesktopUi.body(drafts.isEmpty()?"No recovery writing on this PC":"Writing kept on this PC"));
            DesktopUi.add(body,DesktopUi.note("Recovery writing stays separate from saved notes and sharing. Shorten it to save a note, or export the complete writing.",440,DesktopUi.QUIET,DesktopUi.BODY));
            for(DesktopRecovery.Draft d:drafts) {
                String label=d.title().isBlank()?"Untitled":d.title().substring(0,Math.min(60,d.title().length()));
                DesktopUi.add(body,DesktopUi.note(label+" · "+d.reason(),440,DesktopUi.QUIET,DesktopUi.BODY));
                DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Reopen",()->{box[0].dispose();save(()->reopenWriting(d));}),
                    DesktopUi.button("Export text…",()->exportWriting(d)),DesktopUi.button("Save as new note",()->{
                        disk.submit(()->{String reason=DesktopEdits.limits(d.title(),d.body());if(reason!=null)throw new IllegalStateException(reason+" Reopen and shorten the writing first.");
                            var db=store.getWritableDatabase();db.beginTransaction();try{NoteStore.Note copy=new NoteStore.Note();copy.book=Things.HOME;copy.title=d.title();copy.body=d.body();copy.writers=d.writers();store.save(copy);DesktopRecovery.discard(store,d.id(),d.token());db.setTransactionSuccessful();return copy;}finally{db.endTransaction();}
                        },copy->{box[0].dispose();display(copy);refresh();},this::failed);
                    }),DesktopUi.button("Discard…",()->{
                        if(!DesktopUi.confirm(box[0],"Discard recovery writing?","The saved note stays. This complete recovery draft is deleted from this PC.","Discard",true))return;
                        disk.submit(()->{DesktopRecovery.discard(store,d.id(),d.token());return null;},done->{box[0].dispose();if(Objects.equals(recoveryToken,d.token())){recoveryToken=null;recovering=false;dirty=false;disk.submit(()->store.get(d.id()),saved->{if(saved!=null)display(saved);else showHome();},this::failed);}recoveryWriting();},this::failed);
                    })));
            }
            DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Back up recovery writing…",this::backupWriting),DesktopUi.button("Restore recovery writing…",this::restoreWriting)));
            box[0]=DesktopUi.sheet(frame,"Recovery writing",body,DesktopUi.footer(DesktopUi.primary("Done",()->box[0].dispose())),true);DesktopUi.show(box[0],520,540);
        },this::failed);
    }
    void reopenWriting(DesktopRecovery.Draft d) {
        int request=++noteRequest;
        disk.submit(()->new Object[]{store.get(d.id()),DesktopRecovery.token(store,d.token())},got->{
            DesktopRecovery.Draft current=(DesktopRecovery.Draft)got[1];if(current==null||request!=noteRequest)return;
            NoteStore.Note metadata=(NoteStore.Note)got[0];NoteStore.Note original=metadata==null?current.base().copy():metadata.copy();
            original.title=current.base().title;original.body=current.base().body;original.revision=current.base().revision;
            display(original);drawing=true;title.setText(current.title());split(current.body(),current.writers(),tab);page.put(tabWords,tabPageWriters);drawing=false;drawTabs();
            recoveryToken=current.token();recovering=true;dirty=true;editorGeneration++;page.setEditable(true);title.setEditable(true);drawTabs();
            noteSays(DesktopRecovery.KEPT,NoteLine.Tone.FAILED,this::recoveryWriting);
        },this::failed);
    }
    private void exportWriting(DesktopRecovery.Draft draft) {
        Path target=DesktopPick.save(frame,"Export recovery writing","recovery-writing.txt");if(target==null)return;
        if(context.databaseKey()!=null&&!DesktopUi.confirm(frame,"Export unlocked text?","This text file can be read without your notebook password. Keep it somewhere safe.","Export text",false))return;
        disk.submit(()->{DesktopRecovery.export(draft,target);return null;},done->status.setText(Say.t("Complete recovery writing exported")),this::failed);
    }
    private void backupWriting() {
        char[] chosen=DesktopLock.backupPassword(frame);if(chosen==null)return;
        if(chosen.length==0){status.setText(Say.t("Recovery writing backups need a password"));return;}
        Path target=DesktopPick.save(frame,"Back up recovery writing","mininotes-writing.mnwriting");if(target==null){Arrays.fill(chosen,' ');return;}
        status.setText(Say.t("Backing up recovery writing…"));disk.submit(()->{try{Vault.Made made=Vault.make(chosen);DesktopWritingBackup.write(store,target,made.key,made.kept);return null;}finally{Arrays.fill(chosen,' ');}},done->status.setText(Say.t("Recovery writing backup saved, locked")),this::failed);
    }
    private void restoreWriting() {
        Path source=DesktopPick.openOne(frame,"Restore recovery writing");if(source==null)return;
        try {byte[] key=DesktopLock.openBackup(frame,DesktopWritingBackup.lock(source));if(key==null)return;
            disk.submit(()->DesktopWritingBackup.restore(store,source,key),count->{status.setText(Say.t("{0} recovery drafts restored",count));recoveryWriting();},this::failed);
        }catch(Exception e){failed(e);}
    }
    void backup() {
        disk.submit(()->DesktopRecovery.all(store).size(),count->{
            if(count>0&&!DesktopUi.confirm(frame,"Saved notes only","This portable backup covers saved notes only. "+count+" recovery drafts stay on this PC. Use Recovery writing to export their full text or make a locked writing backup first.","Export saved notes",false))return;
            backupSavedNotes();
        },this::failed);
    }
    private void backupSavedNotes() {
        // Not locked, a password for this backup, or none, said for what it is (decision 82).
        char[] chosen=context.databaseKey()!=null?null:DesktopLock.backupPassword(frame);
        if(context.databaseKey()==null&&chosen==null)return;
        boolean sealed=context.databaseKey()!=null||chosen.length>0;
        Path target=DesktopPick.save(frame,"Export backup",sealed?"mininotes-backup-locked.mnbackup":"mininotes-backup.zip");if(target==null)return;
        status.setText(Say.t("Writing backup…"));disk.submit(()->{
            byte[] key=context.databaseKey();Path lock=context.getFilesDir().toPath().resolve(DesktopLock.KEPT);
            if(key!=null){DesktopBackup.writeSaved(store,target,key,Files.readAllBytes(lock),privately.slots());return true;}
            // A lock made for this backup alone: its password opens it anywhere.
            if(chosen.length>0){Vault.Made made=Vault.make(chosen);java.util.Arrays.fill(chosen,' ');DesktopBackup.writeSaved(store,target,made.key,made.kept,privately.slots());return true;}
            DesktopBackup.writeSaved(store,target,null,null,privately.slots());return false;
        },locked->{
            // When, for the reminder that proposes one (decision 235).
            context.getSharedPreferences("settings",0).edit().putString(DesktopBackup.BACKED_UP,Long.toString(System.currentTimeMillis())).apply();
            status.setText(Say.t(locked?"Backup saved, locked":"Backup saved, not locked"));},this::failed);
    }
    void importBackup() {
        Path source=DesktopPick.openOne(frame,"Add from backup");if(source==null)return;byte[][] opener={null};
        // A locked backup is opened here, on this thread, before any work starts: its password or its words.
        if(DesktopBackup.locked(source)) {
            try {
                byte[] lock;try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))){in.readFully(new byte[4]);lock=new byte[in.readUnsignedShort()];in.readFully(lock);}
                opener[0]=DesktopLock.openBackup(frame,lock);if(opener[0]==null)return;
            } catch(Exception e){failed(e);return;}
        }
        int how=askHowToImport();
        if(how<0)return;boolean replacing=how==1;
        if(replacing&&!confirmReplace())return;
        status.setText(Say.t(replacing?"Restoring from backup…":"Adding notes from backup…"));disk.submit(()->{
            int count=DesktopBackup.add(store,source,lock->opener[0],replacing,privately.slots());
            byte[] opened=context.databaseKey();if(opened!=null)DesktopFiles.every(store,opened,true);
            return new Object[]{count,replacing?store.latest():null};
        },done->{int count=(Integer)done[0];
            status.setText(replacing?(count==1?Say.t("1 note restored"):Say.t("{0} notes restored",count)):(count==1?Say.t("1 note added"):Say.t("{0} notes added",count)));
            if(replacing) {
                // What was open may be gone: the overview is emptied, and the newest note of the restored pad opens.
                undoHow=null;overview.clear();carets.clear();base=null;home.folder.close();
                NoteStore.Note latest=(NoteStore.Note)done[1];if(latest!=null)display(latest);else showHome();
            }
            refresh();},this::failed);
    }
    /** Added beside what is here, or put in its place - the phone's two ways, in the phone's words. 1 is replace; -1, neither. */
    int askHowToImport() {
        return DesktopUi.choose(frame,"Import this backup","Adding keeps your notes and brings the backup's in beside them, as copies. Replacing is a restore: this pad becomes what the backup was.",
            new String[]{"Add to this pad","Replace everything"},0,"Import");
    }
    /** Replacing cannot be undone, so it is said what goes, and asked once more. */
    boolean confirmReplace() {
        int here=0;for(NoteStore.Branch one:everything)if(one.kind==NoteStore.Branch.Kind.COLLECTION)here++;
        return DesktopUi.confirm(frame,"Replace everything?",(here==0?"This pad":here==1?"The one folder on this pad":"All "+here+" folders on this pad")
            +" and every note and file in them are deleted, and the backup is put in their place.\n\nThis cannot be undone. Export what is here first if you are not sure.","Replace everything",true);
    }
    /** Several files can be picked at once; each is kept or refused on its own, with one line for all of them. See {@link Given}. */
    private void attach() {
        if(base==null||!page.isEditable())return;
        List<Path> sources=DesktopPick.open(frame,"Attach a file",true);
        if(sources.isEmpty())return;
        attach(base.id,null,sources);
    }
    /**
     * These files kept with this note: chosen with Attach, or dropped on it (see DesktopDrops.aim). {@code called}
     * is the note's, said with the outcome when it is not the one open.
     */
    void attach(String id,String called,List<Path> sources) {
        status.setToolTipText(null);
        if(isOpen(id)&&!page.isEditable()){status.setText(Say.t("This note is read only here"));return;}
        status.setText(Say.t(Given.adding(sources.size())));
        disk.submit(()->{
            if(Boolean.TRUE.equals(store.readOnlyHere(id)[0]))throw new IllegalStateException("This note is read only here");
            List<String> refused=new ArrayList<>();int kept=0;String only=null;
            for(Path source:sources) {
                String name=Attachment.named(source.getFileName().toString());
                try {
                    long size=Files.size(source);
                    if(size>Attachment.LIMIT){refused.add(Given.refusal(name,Given.TOO_BIG));continue;}
                    if(store.weight()+size>Attachment.PLENTY){refused.add(Given.refusal(name,Given.FULL));continue;}
                    String key=UUID.randomUUID().toString();Path dest=store.fileFor(key).toPath();DesktopFiles.keep(context,source,dest);
                    // A picture made smaller, anything else packed, before its row is written (decision 112).
                    try{NoteStore.Held held=store.settle(new NoteStore.Held(key,id,name,Attachment.kind(Files.probeContentType(source)),size,System.currentTimeMillis()));store.keep(held);only=held.name;}
                    catch(Exception failure){Files.deleteIfExists(dest);throw failure;}
                    kept++;
                } catch(Exception unreadable){refused.add(Given.refusal(name,Given.UNREADABLE));}
            }
            return new Object[]{kept,only,refused};
        },done->{
            int kept=(Integer)done[0];@SuppressWarnings("unchecked") List<String> refused=(List<String>)done[2];
            // The count, and which were not kept and why, on the same line; all of it again on hover if it is long.
            String said=Given.added(kept,(String)done[1],refused.size());
            // Another note than the one open: which, after "added", so the person knows where they went.
            if(kept>0&&called!=null&&!isOpen(id)){int at=said.lastIndexOf(" added")+6;said=said.substring(0,at)+" to “"+called+"”"+said.substring(at);}
            status.setText(said+(refused.isEmpty()?"":". "+String.join("; ",refused)));
            status.setToolTipText(refused.isEmpty()?null:Given.refusals(refused));
            if(kept>0)attachmentsChanged(id);
        },this::failed);
    }
    /**
     * A picture pasted on a note - a screenshot from Greenshot or the Snipping Tool, a picture copied in a browser - kept
     * with it as a PNG, as a messaging app takes one (the owner's ask, 2026-10-01; see Attachment.pasted). It is named for
     * when it came, as a recording is, and goes by the road every attachment takes: the same limits, sealed on the way in
     * when the notebook is locked, said in the bar.
     */
    void attachPicture(String id,String called,BufferedImage picture) {
        status.setToolTipText(null);
        if(isOpen(id)&&!page.isEditable()){status.setText(Say.t("This note is read only here"));return;}
        String name=Attachment.picture(System.currentTimeMillis());String[] keptAs={name};
        status.setText(Say.t("Adding the picture…"));
        disk.submit(()->{
            if(Boolean.TRUE.equals(store.readOnlyHere(id)[0]))throw new IllegalStateException("This note is read only here");
            ByteArrayOutputStream png=new ByteArrayOutputStream();
            if(!javax.imageio.ImageIO.write(picture,"png",png))throw new IllegalStateException("That picture could not be kept");
            byte[] whole=png.toByteArray();
            if(whole.length>Attachment.LIMIT)return Given.refusal(name,Given.TOO_BIG);
            if(store.weight()+whole.length>Attachment.PLENTY)return Given.refusal(name,Given.FULL);
            String key=UUID.randomUUID().toString();Path dest=store.fileFor(key).toPath();DesktopFiles.keep(context,whole,dest);
            // A PNG or a JPEG, whichever is the smaller, as every picture added is (decision 112).
            try{NoteStore.Held held=store.settle(new NoteStore.Held(key,id,name,"image/png",whole.length,System.currentTimeMillis()));store.keep(held);keptAs[0]=held.name;}
            catch(Exception failure){Files.deleteIfExists(dest);throw failure;}
            return "";
        },refused->{
            if(!refused.isEmpty()){status.setText(Given.added(0,null,1)+". "+refused);return;}
            String said=Given.added(1,keptAs[0],0);
            // Another note than the one open: which, after "added", so the person knows where it went.
            if(called!=null&&!isOpen(id))said=said+" to “"+called+"”";
            status.setText(said);
            attachmentsChanged(id);
        },this::failed);
    }
    /**
     * These files kept with a collection, or on Home: chosen with its Add a file…, or dropped from Explorer on its icon,
     * its card or Home's grid (see DesktopDrops.aim). Each is kept or refused on its own, with one line for all of them,
     * and Home and the card show them at once. {@code called} names where they went.
     */
    /**
     * What is on the clipboard, kept where the reader is (the owner, 2026-10-21: "Would be good if after a screen grab with
     * greenshot, a ctrl+v would let us put the image directly in a folder, it works in a note already"; decision 233): on
     * Home, the folder shown or Temp, a picture as a PNG named for when it came, as one pasted on a note is, and files copied
     * in Explorer as if they were let go there. Nothing on a note's page, which pastes for itself.
     */
    void pasteHere() {
        if(onPage)return;
        // A place's card (Temp, or one that only lists: the archive, the bin, Recent, Favourites) is placeShown, a folder shown.
        NoteStore.Branch atPlace=home==null?null:home.folder.placeShown(),card=home==null?null:home.folder.shown();
        boolean temp=atPlace!=null&&atPlace.kind==NoteStore.Branch.Kind.TEMP;
        if(atPlace!=null&&!temp||card!=null&&!DesktopMoving.shelf(card.kind)){status.setText(Say.t("Open Home or a folder to paste a picture or files there"));return;}
        if(temp)card=null;
        String into=card==null||temp?Things.HOME:card.id,called=card==null?"Home":temp?"Temp":DesktopHome.named(card),place=temp?NoteStore.TEMP:null;
        java.awt.datatransfer.Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            if(clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked") List<File> files=(List<File>)clip.getData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                List<Path> paths=new ArrayList<>();for(File f:files)if(f.isFile())paths.add(f.toPath());
                if(!paths.isEmpty()){keep(into,called,paths,place);return;}
            }
            if(clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor)) {
                BufferedImage picture=DesktopIconPicker.buffered((Image)clip.getData(java.awt.datatransfer.DataFlavor.imageFlavor));
                if(picture==null){status.setText(Say.t("That picture could not be read from the clipboard"));return;}
                // Written once as a PNG under the name a pasted picture is given, then kept as any file let go here is.
                // Its name as Windows allows a file to be called: "Picture 22 Oct, 01:23.png" has a colon (DesktopFiles.onDisk).
                Path dir=Files.createTempDirectory("mininotes-paste");Path png=dir.resolve(DesktopFiles.onDisk(Attachment.picture(System.currentTimeMillis())));
                if(!javax.imageio.ImageIO.write(picture,"png",png.toFile()))throw new IllegalStateException("That picture could not be kept");
                png.toFile().deleteOnExit();dir.toFile().deleteOnExit();
                keep(into,called,List.of(png),place);return;
            }
            status.setText(Say.t("Nothing to paste: copy a picture or files first"));
        } catch(Exception unreadable){status.setText(Say.t("That picture could not be read from the clipboard"));}
    }

    void keep(String collection,String called,List<Path> sources){keep(collection,called,sources,null);}
    /** @param place a place each file kept is then put in (decision 87), or null */
    void keep(String collection,String called,List<Path> sources,String place) {
        boolean onHome=NoteStore.home(collection);String into=onHome?Things.HOME:collection;
        status.setToolTipText(null);status.setText(Say.t(Given.adding(sources.size())));
        disk.submit(()->{
            if(!onHome&&Boolean.FALSE.equals(store.mayWriteIn(NoteStore.Branch.Kind.COLLECTION,into)))throw new IllegalStateException("This folder is read only here");
            List<String> refused=new ArrayList<>();int kept=0;String only=null;
            for(Path source:sources) {
                String name=Attachment.named(source.getFileName().toString());
                try {
                    long size=Files.size(source);
                    if(size>Attachment.LIMIT){refused.add(Given.refusal(name,Given.TOO_BIG));continue;}
                    if(store.weight()+size>Attachment.PLENTY){refused.add(Given.refusal(name,Given.FULL));continue;}
                    NoteStore.Held held=store.opening(NoteStore.Branch.Kind.COLLECTION,into,name,Files.probeContentType(source),size);
                    Path dest=store.fileFor(held.id).toPath();DesktopFiles.keep(context,source,dest);
                    // The row is written last: until it exists the bytes are nobody's, and get swept up. A picture made smaller,
                    // anything else packed, before it is (decision 112).
                    try{held=store.settle(held);store.keep(held);}catch(Exception failure){Files.deleteIfExists(dest);throw failure;}
                    if(place!=null)intoPlaceNow(NoteStore.Branch.Kind.FILE,held.id,place);
                    kept++;only=held.name;
                } catch(Exception unreadable){refused.add(Given.refusal(name,Given.UNREADABLE));}
            }
            return new Object[]{kept,only,refused};
        },done->{
            int kept=(Integer)done[0];@SuppressWarnings("unchecked") List<String> refused=(List<String>)done[2];
            String said=Given.added(kept,(String)done[1],refused.size());
            if(kept>0){int at=said.lastIndexOf(" added")+6;if(at>=6)said=said.substring(0,at)+(onHome?" to Home":" to “"+(called==null?"the folder":called)+"”")+said.substring(at);}
            status.setText(said+(refused.isEmpty()?"":". "+String.join("; ",refused)));
            status.setToolTipText(refused.isEmpty()?null:Given.refusals(refused));
            if(kept>0)refresh();
        },this::failed);
    }
    /**
     * A file added to the open note or taken out of it: whoever has the note is sent its list now, and what is
     * to go up goes up. See {@link Enclosure}.
     */
    void filesChanged(String note){network.submit(()->{Post.filesChanged(context,store,keys,note);return null;},done->updateStanding(),e->{});}
    /** What the paperclip offers: to attach something, and each file already attached. */
    private void clipMenu() {
        if(base==null)return;String id=base.id;
        disk.submit(()->{
            List<NoteStore.Held> files=store.notAway(store.filesOf(NoteStore.Branch.Kind.PAGE,id));
            // Which are stamped on Minima, and how far, said on their lines as on their cards (decision 224).
            Map<String,NoteStore.StampRow> stamped=new HashMap<>();
            for(NoteStore.Held file:files){List<NoteStore.StampRow> all=store.stampsOf(file.id);if(!all.isEmpty())stamped.put(file.id,all.get(0));}
            return new Object[]{files,stamped};
        },got->{
            @SuppressWarnings("unchecked") List<NoteStore.Held> files=(List<NoteStore.Held>)got[0];
            @SuppressWarnings("unchecked") Map<String,NoteStore.StampRow> stamped=(Map<String,NoteStore.StampRow>)got[1];
            JPopupMenu menu=new JPopupMenu();
            JMenuItem add=DesktopMenus.item("Attach a file…");add.setEnabled(page.isEditable());add.addActionListener(e->save(this::attach));menu.add(add);
            // The camera here too, as beside the microphone (the owner, 2026-10-18; decision 194).
            JMenuItem photo=DesktopMenus.item("Take a photo…");photo.setEnabled(page.isEditable());photo.addActionListener(e->save(()->DesktopCamera.take(this,id)));menu.add(photo);
            JMenuItem record=DesktopMenus.item("Record audio");record.setEnabled(page.isEditable()&&!recorder.recording());record.addActionListener(e->record());menu.add(record);
            if(!files.isEmpty())selecting(menu,id,files,stamped);
            menu.show(clip,clip.getWidth()-menu.getPreferredSize().width,-menu.getPreferredSize().height-4);
        },this::failed);
    }
    /**
     * The note's files in the paperclip's menu, each ticked or not, the menu staying open while they are; then what can be done
     * with those ticked, all at once (the owner, 2026-10-18: "we need to be able to perform bulk action on all attachments ...
     * see them, select them and perform action on the selection we pick"; decision 196).
     */
    private void selecting(JPopupMenu menu,String note,List<NoteStore.Held> files,Map<String,NoteStore.StampRow> stamped) {
        menu.addSeparator();
        List<JCheckBoxMenuItem> ticks=new ArrayList<>();
        JCheckBoxMenuItem all=new JCheckBoxMenuItem(Say.t("Select all"));
        JMenuItem save=DesktopMenus.item("Save on my computer…"),bin=DesktopMenus.item("Move to bin"),stamp=DesktopMenus.item("Stamp on Minima…");
        Runnable said=()->{
            int n=0;for(JCheckBoxMenuItem one:ticks)if(one.isSelected())n++;
            boolean any=n>0,every=n==ticks.size();
            String count=any?"  ("+n+")":"";
            save.setText(Say.t("Save on my computer…")+count);bin.setText(Say.t("Move to bin")+count);
            stamp.setText(Say.t("Stamp on Minima…")+count);
            save.setEnabled(any);bin.setEnabled(any&&page.isEditable());stamp.setEnabled(any);
            all.setSelected(every);
        };
        for(NoteStore.Held file:files) {
            NoteStore.StampRow itsStamp=stamped.get(file.id);
            JCheckBoxMenuItem one=new JCheckBoxMenuItem(file.name+"   ·   "+Attachment.size(file.bytes)+(itsStamp==null?"":"   ·   "+Integritas.noteLine(itsStamp,false)));
            if(itsStamp!=null)one.setName("clipStamp");
            one.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick",true);
            // A box always there, empty or ticked, so it reads as something to tick; the look's own tick is not drawn.
            one.setIcon(tickBox(one));one.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"checkIcon:null");
            one.setToolTipText(Say.t("Tick to choose it, then choose below what to do with those ticked. A right click copies it."));
            one.addActionListener(e->said.run());
            // A right click on its line: copied as the file itself, to paste anywhere (the owner, 2026-10-18; decision 207).
            one.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mousePressed(java.awt.event.MouseEvent e){
                if(!SwingUtilities.isRightMouseButton(e))return;e.consume();menu.setVisible(false);copyFile(file);}});
            ticks.add(one);menu.add(one);
        }
        menu.addSeparator();
        all.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick",true);
        all.setIcon(tickBox(all));all.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"checkIcon:null");
        all.addActionListener(e->{boolean every=all.isSelected();for(JCheckBoxMenuItem one:ticks)one.setSelected(every);said.run();});
        java.util.function.Supplier<List<NoteStore.Held>> chosen=()->{List<NoteStore.Held> out=new ArrayList<>();for(int i=0;i<ticks.size();i++)if(ticks.get(i).isSelected())out.add(files.get(i));return out;};
        // One file: Windows' box with its name; several: one folder for them all.
        save.addActionListener(e->{List<NoteStore.Held> them=chosen.get();if(them.size()==1)saveCopy(them.get(0));else saveAll(them,null);});
        bin.addActionListener(e->{List<NoteStore.Held> them=chosen.get();
            disk.submit(()->{for(NoteStore.Held one:them)store.putAway(NoteStore.Branch.Kind.FILE,one.id,true,true);return null;},
                done->{status.setText(them.size()==1?Say.t("“{0}” is in the bin",them.get(0).name):Say.t("{0} files are in the bin",String.valueOf(them.size())));
                    attachmentsChanged(note);refresh();},this::failed);});
        stamp.addActionListener(e->DesktopStamp.files(this,chosen.get()));
        menu.add(all);menu.add(save);menu.add(stamp);menu.add(bin);
        said.run();
    }

    /** An empty box, or a ticked one, as the item it is on is chosen or not. */
    private static Icon tickBox(AbstractButton on) {
        return new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g0,int x,int y){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(on.isSelected()){g.setColor(ACCENT);g.fillRoundRect(x+1,y+1,14,14,4,4);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawPolyline(new int[]{x+4,x+7,x+12},new int[]{y+8,y+11,y+5},3);}
                else{g.setColor(QUIET);g.setStroke(new BasicStroke(1.4f));g.drawRoundRect(x+1,y+1,14,14,4,4);}
                g.dispose();}};
    }

    /** These files copied into a folder: the one asked for, or {@code into} as it is. A name already there gets a number. */
    void saveAll(List<NoteStore.Held> files,Path into) {
        if(files.isEmpty())return;
        Path folder=into;
        if(folder==null) {
            folder=DesktopPick.folder(frame,"Save on my computer","Save here");
            if(folder==null)return;
        }
        Path there=folder;
        status.setText(Say.t("Saving {0} files…",String.valueOf(files.size())));
        disk.submit(()->{
            Files.createDirectories(there);
            for(NoteStore.Held file:files) {
                String name=DesktopFiles.onDisk(file.name);int dot=name.lastIndexOf('.');
                String stem=dot>0?name.substring(0,dot):name,ext=dot>0?name.substring(dot):"";
                Path target=there.resolve(name);for(int n=2;Files.exists(target);n++)target=there.resolve(stem+" ("+n+")"+ext);
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();DesktopFiles.copyOut(context,store.fileFor(file.id).toPath(),bytes);
                org.mininotes.desktop.platform.AtomicFile.write(target,bytes.toByteArray());
            }
            return null;
        },done->status.setText(files.size()==1?Say.t("Saved in {0}",there.toString()):Say.t("{0} files saved in {1}",String.valueOf(files.size()),there.toString())),this::failed);
    }

    /** The microphone, from its button or the paperclip's menu. See {@link DesktopRecorder}. */
    void record(){if(base!=null&&page.isEditable())recorder.start();}
    /** The open note's id, or null on the cards. */
    String noteShown(){return base==null?null:base.id;}
    /** The bar stands where the microphone was while it records. */
    void recordingShown(boolean going){fitNote.run();mic.setVisible(!compactNote&&!going);if(mic.getParent()!=null){mic.getParent().revalidate();mic.getParent().repaint();}}
    /** A file kept with this note by something other than Attach: its cards, its count and whoever has the note. */
    void attachmentsChanged(String note){if(base!=null&&base.id.equals(note)){updateStanding();fileCards.show(note);}filesChanged(note);}
    /**
     * Download: a copy in this computer's Downloads folder, with nothing asked, as the phone's Download (the owner,
     * 2026-10-10; decision 146). A name already there gets a number, never written over.
     */
    void download(NoteStore.Held file) {
        Path folder=Path.of(System.getProperty("user.home"),"Downloads");
        disk.submit(()->{
            Files.createDirectories(folder);
            String name=DesktopFiles.onDisk(file.name);int dot=name.lastIndexOf('.');
            String stem=dot>0?name.substring(0,dot):name,ext=dot>0?name.substring(dot):"";
            Path target=folder.resolve(name);for(int n=2;Files.exists(target);n++)target=folder.resolve(stem+" ("+n+")"+ext);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DesktopFiles.copyOut(context,store.fileFor(file.id).toPath(),bytes);
            org.mininotes.desktop.platform.AtomicFile.write(target,bytes.toByteArray());return target;
        },target->status.setText(Say.t("{0} is in Downloads",target.getFileName())),this::failed);
    }
    /**
     * A file on the clipboard, without opening it (the owner, 2026-10-18: "the option to copy a file, here a picture, directly
     * from a right click, rather than having to open it"; decision 198): a picture as a picture, which pastes into a document
     * or a message, and every file as a file too, which pastes into a folder in Explorer. The copy that goes is a plain one in
     * this PC's temporary folder, under its own name.
     */
    void copyFile(NoteStore.Held file) {
        disk.submit(()->{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DesktopFiles.copyOut(context,store.fileFor(file.id).toPath(),bytes);
            Path dir=Files.createTempDirectory("mininotes-copy");Path plain=dir.resolve(DesktopFiles.onDisk(file.name));
            Files.write(plain,bytes.toByteArray());plain.toFile().deleteOnExit();dir.toFile().deleteOnExit();
            java.awt.image.BufferedImage picture=null;
            try{picture=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes.toByteArray()));}catch(Exception notAPicture){/* a file only */}
            return new Object[]{plain,picture};
        },got->{
            Path plain=(Path)got[0];java.awt.Image picture=(java.awt.Image)got[1];
            java.awt.datatransfer.Transferable both=new java.awt.datatransfer.Transferable(){
                public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors(){
                    return picture==null?new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.javaFileListFlavor}
                        :new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.imageFlavor,java.awt.datatransfer.DataFlavor.javaFileListFlavor};}
                public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f){for(java.awt.datatransfer.DataFlavor one:getTransferDataFlavors())if(one.equals(f))return true;return false;}
                public Object getTransferData(java.awt.datatransfer.DataFlavor f)throws java.awt.datatransfer.UnsupportedFlavorException{
                    if(java.awt.datatransfer.DataFlavor.imageFlavor.equals(f)&&picture!=null)return picture;
                    if(java.awt.datatransfer.DataFlavor.javaFileListFlavor.equals(f))return List.of(plain.toFile());
                    throw new java.awt.datatransfer.UnsupportedFlavorException(f);}
            };
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(both,null);
            status.setText(Say.t(picture!=null?"“{0}” copied: paste it as a picture, or as a file in a folder":"“{0}” copied: paste it in a folder",file.name));
        },this::failed);
    }
    /** A page in the browser: Integritas' site, or a report's link. */
    static void browse(String url) throws Exception {java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));}
    /**
     * Save on my computer: one file, where Windows' own box says, under its name (the owner, 2026-10-18: "Enregistrer dans un
     * dossier is not clear to me ... and how it differs from Télécharger, let's combine these in Save on my computer";
     * decision 200). Several at once go to one folder (saveAll).
     */
    void saveCopy(NoteStore.Held file) {
        Path target=DesktopPick.save(frame,"Save on my computer",DesktopFiles.onDisk(file.name));if(target==null)return;
        disk.submit(()->{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DesktopFiles.copyOut(context,store.fileFor(file.id).toPath(),bytes);org.mininotes.desktop.platform.AtomicFile.write(target,bytes.toByteArray());return null;},
            done->status.setText(Say.t("Saved in {0}",target.getParent().toString())),this::failed);
    }
    void attachments() {
        if(base==null)return;String id=base.id;
        disk.submit(()->store.notAway(store.filesOf(NoteStore.Branch.Kind.PAGE,id)),files->{
            if(files.isEmpty()){status.setText(Say.t("No attachments on this note"));return;}
            NoteStore.Held file=DesktopUi.pick(frame,"Attachments","Kept on this PC with this note.",files,
                f->f.name+"   ·   "+Attachment.size(f.bytes),"Save on my computer…");
            if(file!=null)saveCopy(file);
        },this::failed);
    }
    private void profile() {
        DesktopProfile.open(this,null,null);
    }
    /** Profile over a page of People and devices, with ‹ back to it (decision 105). */
    void profile(Window over,String backTo){DesktopProfile.open(this,over,backTo);}
    /** Something to say that nobody asked for: in the bar, and over the tray while the window is away. */
    void notice(String said){status.setToolTipText(null);status.setText(Say.t(said));if(!frame.isVisible()&&tray!=null)tray.displayMessage("Mininotes",Say.t(said),TrayIcon.MessageType.INFO);}
    void failed(Exception error){status.setText(Say.t(error.getMessage()==null?"That did not finish. Please try again.":error.getMessage()));status.setToolTipText(status.getText());}

    // Network work never queues in front of a keystroke being committed to disk.
    void startNode() {
        if(offline||nodeStarting||closing)return;nodeStarting=true;
        connection.setText("Connecting…");
        // Taken now: locked again before sharing has started, this window may no longer have what arrives (see DesktopFiles).
        long ticket=DesktopFiles.ticket();
        network.submit(()->{
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());store.myName=Node.nameHere(context);
            store.myAgreement=Point.shorten(keys.agreement().getPublic());
            DesktopFiles.toNotebook(ticket,bytes->disk.submit(()->Post.arrived(context,store,keys,bytes),this::arrived,this::failed));
            if(!Node.listen(context,DesktopFiles::arrived))throw new IllegalStateException("Could not start sharing");
            List<String> addresses=Node.addresses(context);String address=addresses.isEmpty()?"":addresses.get(0);store.myAddress=address;
            Node.everyBeat(()->{if(!closing){Post.again(context,store,keys);Post.leftAgain(context,store,keys);Post.removedAgain(context,store,keys);SwingUtilities.invokeLater(this::queuePending);}});
            return address;
        },address->{nodeStarting=false;currentAddress=address;
            // Started, with no relay yet: still connecting. Whether it is offline is the health check's to say, fifteen seconds on.
            connection.setText(address.isEmpty()&&!Node.onlyMine(context)?"Connecting…":connected(address.isEmpty()?0:1));queuePending();
            network.submit(()->{Node.tellEverybody(context);retryAccepting();return null;},done->{},e->{});
            // Whatever arrived while the notebook was locked again, taken in now it is open.
            disk.submit(()->{int landed=DesktopFiles.takeIn(context,store,keys);Post.sayLocked(context,store,keys,false);return landed;},
                landed->{if(landed>0){refresh();if(base!=null)changedUnderneath(base.id);}},this::failed);
            // Whether Windows lets the phones in at this PC's door, said once a start: shut, the phones' notes and
            // answers go only by relay. States only - the network's kind, never its name.
            connectivity.submit(DesktopFirewall::read,state->org.mininotes.desktop.platform.util.Log.i("Mininotes/Desktop",
                "Windows firewall: the rules for devices to reach this PC are "+(state.allowed()?"on":"not on")
                +(state.network().isEmpty()?"":"; this network is "+state.network())),e->{});
        },e->{nodeStarting=false;connection.setText("Offline");failed(e);});
        Object previous=frame.getRootPane().getClientProperty("health");if(previous instanceof javax.swing.Timer old)old.stop();
        javax.swing.Timer health=new javax.swing.Timer(15000,e->{if(!closing)network.submit(Node::attached,count->{if(!closing)connection.setText(connected(count));},error->{});});health.start();frame.getRootPane().putClientProperty("health",health);
    }
    /** The word in the bar: with no relay by choice it is not "offline", it is the way the owner chose. */
    private String connected(int relays){return Node.onlyMine(context)?"Only between your devices":relays>0?"Connected":"Offline, retrying";}
    private String address() throws Exception {
        List<String> all=Node.addresses(context);if(all.isEmpty())throw new IllegalStateException(Node.onlyMine(context)?"This PC is on no network your devices could reach it on.":"No relay connection yet. Try sharing again in a moment.");
        currentAddress=all.get(0);store.myAddress=currentAddress;return currentAddress;
    }
    private void retryAccepting() throws Exception {
        String mine=address();for(NoteStore.Accepting pending:store.waitingToAccept())try{Post.sayAgain(context,store,keys,pending,Node.nameHere(context),mine);store.triedAgain(pending.address);}catch(Exception unavailable){/* Kept for next start. */}
    }
    private void scheduleSync() {
        if(offline||base==null)return;String id=base.id;
        disk.submit(()->store.pauseFor(NoteStore.Branch.Kind.PAGE,id),seconds->{if(seconds>=0){
            // Left while it was being written down: it goes now, not after the rest of its wait (the owner, 2026-10-14).
            due.put(id,System.currentTimeMillis()+(hurried.remove(id)?0:Math.max(1,seconds)*1000L));written.add(id);}},this::failed);
    }
    /** Notes left while their last words were still being written down: they go as soon as they are. */
    private final Set<String> hurried=new HashSet<>();
    /**
     * A note left - Home, another note, the window put away - sends what it owes now rather than after the rest of its wait
     * (the owner, 2026-10-14: "everything should be sync as soon as possible, at least when we are out of a note"). Not a
     * note set to go only when asked: its wait is never started.
     */
    private void sendOnLeaving(String id) {
        if(id==null)return;
        if(dirty||saving)hurried.add(id);
        if(due.containsKey(id))due.put(id,System.currentTimeMillis());
    }
    /** Notes due because they were just written in, whose going is said on the open note's line (see sendDue). */
    private final Set<String> written=new HashSet<>();
    private void sendDue() {
        if(offline||closing)return;
        List<String> ready=new ArrayList<>();long now=System.currentTimeMillis();
        due.forEach((id,at)->{if(at<=now)ready.add(id);});
        // The open note going by itself after it was written in is said on its line too, quietly: the mark alone
        // changed, and nothing said it had gone. Not the tries again a minute apart, which would say the same every minute.
        for(String id:ready){due.remove(id);boolean say=written.remove(id)&&isOpen(id);
            if(say&&!dirty)noteSays(NoteLine.sending(namesShown()),NoteLine.Tone.GOING,null);network.submit(()->{
            if(store.pauseFor(NoteStore.Branch.Kind.PAGE,id)<0)return new Post.Done(0,0,"");
            return Post.send(context,store,keys,NoteStore.Branch.Kind.PAGE,id);
        },done->{if(done.failed>0)due.putIfAbsent(id,System.currentTimeMillis()+60000);if(say&&isOpen(id)&&!dirty)noteSent(done,false);refresh();},
            e->{due.putIfAbsent(id,System.currentTimeMillis()+60000);if(say&&isOpen(id))noteFailed(e);else failed(e);});}
    }
    /** When the others were last asked for what they have, on coming to the front: not more than every two minutes. */
    private long askedOthers;
    /**
     * In front again, it catches up both ways, as the phone does on coming back: whatever is owed is queued to go, and
     * the others are asked for what they have, which a machine asleep or a phone in its pocket missed. Sync now did the
     * asking, and only Sync now (the owner, 2026-10-10: "I don't see why I have to push it manually").
     */
    private void catchUp() {
        if(closing||offline||!frame.isVisible())return;
        queuePending();
        long now=System.currentTimeMillis();
        if(askedOthers!=0&&now-askedOthers<120_000)return;
        askedOthers=now;
        network.submit(()->Post.ask(context,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING),asked->{},e->{});
    }
    private void queuePending() {
        if(closing||offline)return;
        disk.submit(()->{
            Map<String,Integer> waiting=new HashMap<>();
            for(Outbox.Wait one:store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING)) {
                int seconds=store.pauseFor(NoteStore.Branch.Kind.PAGE,one.page);if(seconds>=0)waiting.put(one.page,seconds);
            }return waiting;
        },waiting->{if(closing)return;waiting.forEach((id,seconds)->{if(!(dirty&&base!=null&&base.id.equals(id)))due.putIfAbsent(id,System.currentTimeMillis()+Math.max(1,seconds)*1000L);});},this::failed);
    }
    private void sync(boolean explicit) {
        NoteStore.Branch picked=explicit?target():null;
        // The open note's sync is said on its own line, beside the mark that was clicked; anything wider on the bar.
        boolean onLine=picked!=null&&picked.kind==NoteStore.Branch.Kind.PAGE&&isOpen(picked.id);
        if(offline){if(onLine)noteSays("Not sent: this session was started offline",NoteLine.Tone.FAILED,null);else if(explicit)status.setText(Say.t("This session was started offline"));return;}
        if(onLine)noteSays(NoteLine.sending(namesShown()),NoteLine.Tone.GOING,null);else if(explicit)status.setText(Say.t("Syncing…"));
        NoteStore.Branch.Kind kind=picked!=null?picked.kind:NoteStore.Branch.Kind.LIBRARY;
        String id=picked!=null?picked.id:Sharing.EVERYTHING;
        if(kind!=NoteStore.Branch.Kind.COLLECTION&&kind!=NoteStore.Branch.Kind.BOOK&&kind!=NoteStore.Branch.Kind.PAGE&&kind!=NoteStore.Branch.Kind.FILE
            &&kind!=NoteStore.Branch.Kind.TEMP){kind=NoteStore.Branch.Kind.LIBRARY;id=Sharing.EVERYTHING;}
        final NoteStore.Branch.Kind targetKind=kind;final String target=id;
        network.submit(()->{if(explicit)Post.ask(context,store,keys,targetKind,target);return Post.send(context,store,keys,targetKind,target,null,explicit);},done->{
            refresh();if(!explicit)return;
            // No box over the page for the open note: its line says what could not go, and a click on it says why.
            if(onLine){noteSent(done,true);return;}
            status.setText(Say.t(done.failed>0?Unsent.title(done.sent,done.failed):done.sent>0?"Sent, waiting for delivery confirmation":"No outgoing changes. Asked for updates."));
            if(done.failed>0)tellUnsent(done);
        },e->{if(onLine)noteFailed(e);else failed(e);});
    }
    /**
     * What could not go, said by device and thing, and the one thing to do about it as the box's button - pairing, or
     * How notes travel - where trying again will not put it right. The same words as the phone's (see Unsent).
     */
    void tellUnsent(Post.Done done) {
        Unsent.Problem first=Unsent.first(done.problems);
        Unsent.Fix fix=first==null?Unsent.Fix.NONE:Unsent.fix(first);
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(done.problems.isEmpty()&&!done.why.isEmpty()?done.why:Unsent.words(done.problems,Post.here()),420,DesktopUi.INK,DesktopUi.BODY));
        String button=Unsent.button(first),off=Unsent.takeOff(first);
        // Somebody only listed: linked with, or taken off the list - both from here, rather than hunted for.
        boolean listed=Unsent.listed(first);
        // Only what does something: the cross, Escape or a click beside the box is how it is left alone.
        JPanel foot=button==null?null
            :DesktopUi.footer(DesktopUi.primary(button,()->{box[0].dispose();
                    if(listed)disk.submit(()->store.listing(first.address),rule->link(rule),this::failed);
                    else if(fix==Unsent.Fix.PAIR)people(null);else DesktopSettings.open(this);}));
        if(off!=null) {
            JPanel both=new JPanel(new BorderLayout());both.setOpaque(false);
            both.add(DesktopUi.actions(DesktopUi.button(off,()->{box[0].dispose();
                disk.submit(()->store.listing(first.address),rule->takeOff(first.who,first.listedIn,rule),this::failed);})),BorderLayout.WEST);
            if(foot!=null)both.add(foot,BorderLayout.EAST);
            foot=both;
        }
        box[0]=DesktopUi.sheet(frame,Unsent.title(done.problems,done.sent,done.failed),body,foot,true);
        DesktopUi.show(box[0],520,560);
    }

    /**
     * A round on the line under a title, for somebody the note's list names and this PC is not linked with: who, on
     * what, and the two things to do about it - link with them, or take them off - where the round is.
     */
    void notLinked(SyncStatus.Person who) {
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(who.notLinked(Post.here()),380,DesktopUi.INK,DesktopUi.BODY));
        JButton link=DesktopUi.primary(Unsent.link(who.called()),()->{box[0].dispose();link(who.listing());});
        JPanel foot=new JPanel(new BorderLayout());foot.setOpaque(false);
        if(who.listing()!=null)foot.add(DesktopUi.actions(DesktopUi.button(Unsent.takeOff(who.listedIn()),
            ()->{box[0].dispose();takeOff(who.called(),who.listedIn(),who.listing());})),BorderLayout.WEST);
        foot.add(DesktopUi.footer(link),BorderLayout.EAST);
        box[0]=DesktopUi.sheet(frame,who.called(),body,foot,true);
        box[0].getRootPane().setDefaultButton(link);
        DesktopUi.show(box[0],480,360);
    }

    /** Linking with somebody a thing lists: People and devices for that thing, with Show my code ready to press. */
    void link(Sharing.Rule listing) {
        if(listing==null||listing.scope==Sharing.Scope.LIBRARY){people(null);return;}
        NoteStore.Branch.Kind kind=NoteStore.kindFor(listing.scope);
        disk.submit(()->store.thingName(kind,listing.target),name->people(new NoteStore.Branch(kind,listing.target,"",name,"",0,0,false),true),this::failed);
    }

    /** Somebody taken off a thing's list, after one question; told, as Remove tells, once there is a way to them. */
    void takeOff(String who,String listedIn,Sharing.Rule rule) {
        if(rule==null){status.setText(Say.t("{0} is no longer on any list here",who));return;}
        String what=listedIn==null||listedIn.isBlank()?"this":"“"+listedIn+"”";
        if(!DesktopUi.confirm(frame,"Take "+who+" off "+what+"?",who+" stops being listed for "+what+". Nothing is taken from anybody else.","Take them off",true))return;
        status.setText(Say.t("Updating access…"));
        disk.submit(()->{
            if(!store.saysWhoHas(rule.scope,rule.target))throw new IllegalStateException("Only its owner or an admin can take somebody off "+what+".");
            store.decide(rule,Sharing.Level.GONE,System.currentTimeMillis());return null;
        },done->network.submit(()->{Post.changed(context,store,keys,NoteStore.kindFor(rule.scope),rule.target);Post.removedAgain(context,store,keys);return null;},
            v->{status.setText(Say.t("{0} is off {1}",who,what));refresh();},this::failed),this::failed);
    }
    void arrived(Post.Landed landed) {
        // A help request arrived (decision 113): the tray says it, and a box shows it loud, filled in as the updates come.
        if(landed.helpRequest!=null){helpAlert(landed);return;}
        // A message from a 0.3.036 build (decision 145, withdrawn in 147): kept and answered by Post, shown nowhere.
        if(landed.message)return;
        if(landed.files){DesktopDrops.heard(this,landed);return;}
        // Something another person shared with me for the first time (decision 109): the tray says it once for each thing while
        // the window is away, and the pop-up asks, once for each thing, whenever the window is in front.
        if(landed.waiting) {
            if(!frame.isVisible()&&tray!=null&&landed.said!=null&&trayWaiting.add(landed.waits))tray.displayMessage("Mininotes",Say.t(landed.said),TrayIcon.MessageType.INFO);
            refresh();askWaiting();return;
        }
        if(!frame.isVisible()&&tray!=null&&landed.said!=null)tray.displayMessage("Mininotes",Say.t(landed.said),TrayIcon.MessageType.INFO);
        if(landed.accepted!=null){accepted(landed.accepted);return;}
        // A card renamed one of your devices: an open People and devices is drawn again where it is, or it shows the old names.
        // And a card that changed your groups, as one from another device of yours does (decision 100).
        if(landed.devices||landed.groups)peopleAgain();
        // Somebody's Parlons! address, from another device of yours (decision 101): known to the menus, and drawn where it shows.
        if(landed.contacts){DesktopFaces.forgetAll();frame.repaint();disk.submit(()->{parlonsBook=store.parlonsBook();return null;},v->{},e->{});
            peopleAgain();}
        // Stamps from another device of yours (decision 252): every line and box showing them told, and those still waiting
        // for Minima looked for here too where this PC has an Integritas key. The icons follow with refreshSoon.
        if(landed.stamps){Integritas.Watch w=DesktopStamp.watch(this);
            disk.submit(()->{if(!store.secret(Integritas.KEY).isEmpty())w.resume();for(String h:landed.stamped)w.told(h);return null;},v->{},e->{});}
        if(landed.said!=null)status.setText(Say.t(landed.said));refreshSoon();
        // The open note, written in elsewhere: its page follows, typed words kept (see changedUnderneath).
        changedUnderneath(landed.note);
    }

    // ---- a help request sent silently when a note or folder opens: see Help (decision 113) --------------------------

    /**
     * The box that sets a help request on (the owner, 2026-10-06, decision 113): what it does, who it goes to, and the few
     * words. The same words as the phone, but this computer has no location, so it says the request is sent without one. For
     * an ordinary thing it warns the setting is in the notebook for a forensic look; for a private one, that nothing outside
     * the space shows it. {@code save} takes the chosen recipients and words; {@code off} turns it off where it was on.
     */
    void helpSetup(java.awt.Window owner,boolean privateThing,boolean on,java.util.List<String> to,String words,
                   java.util.function.BiConsumer<java.util.List<String>,String> save,Runnable off) {
        disk.submit(()->store.addresses(),all->{
            java.util.List<NoteStore.Contact> paired=new ArrayList<>();
            for(NoteStore.Contact one:all)if(one.paired())paired.add(one);
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.note(Help.WHAT));
            DesktopUi.add(body,DesktopUi.note(Help.PC_PLACE));
            DesktopUi.add(body,DesktopUi.note(privateThing?Help.PRIVATE_TRACE:Help.ORDINARY_TRACE,360,DesktopUi.WARN,DesktopUi.BODY.deriveFont(13f)));
            if(privateThing){DesktopUi.add(body,DesktopUi.note(Help.THIS_NOTE,360,DesktopUi.INK,DesktopUi.BODY.deriveFont(13f)));DesktopUi.add(body,DesktopUi.note(Help.NEVER_AGAIN,360,DesktopUi.INK,DesktopUi.BODY.deriveFont(Font.BOLD,13f)));DesktopUi.add(body,DesktopUi.note(Help.ERASE,360,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));}
            DesktopUi.add(body,DesktopUi.heading(Help.TO));
            final java.util.Map<String,javax.swing.JCheckBox> picks=new java.util.LinkedHashMap<>();
            if(paired.isEmpty())DesktopUi.add(body,DesktopUi.note("Nobody is paired yet. Pair someone in People and devices first."));
            for(NoteStore.Contact one:paired){javax.swing.JCheckBox box=DesktopUi.toggle(one.name,to!=null&&to.contains(one.address));picks.put(one.address,box);DesktopUi.add(body,DesktopUi.switchRow(one.name,box));}
            DesktopUi.add(body,DesktopUi.heading("Message"));
            final javax.swing.JTextField message=new javax.swing.JTextField(words==null?"":words,28);
            DesktopUi.add(body,message);
            final javax.swing.JDialog[] boxD={null};
            javax.swing.JButton turnOn=DesktopUi.primary(Help.TURN_ON,()->{
                java.util.List<String> picked=new ArrayList<>();
                for(java.util.Map.Entry<String,javax.swing.JCheckBox> e:picks.entrySet())if(e.getValue().isSelected())picked.add(e.getKey());
                if(picked.isEmpty()){status.setText(Say.t(Help.NEED_SOMEONE));return;}
                save.accept(picked,message.getText().trim());boxD[0].dispose();
            });
            java.util.List<javax.swing.JComponent> f=new ArrayList<>();
            if(on&&off!=null)f.add(DesktopUi.button("Turn it off",()->{off.run();boxD[0].dispose();}));
            f.add(DesktopUi.button("Cancel",()->boxD[0].dispose()));
            f.add(turnOn);
            boxD[0]=DesktopUi.sheet(owner,Help.ASK_TITLE,body,DesktopUi.footer(f.toArray(new javax.swing.JComponent[0])),true);
            boxD[0].getRootPane().setDefaultButton(turnOn);
            DesktopUi.show(boxD[0],480,680);
        },this::failed);
    }

    /** A note or folder set to send a help request has opened: read the setting, and where it is on, send one at once. */
    void helpOnOpened(final NoteStore.Branch.Kind kind,final String id) {
        // Only a private space's own things send one now (decision 201): an ordinary note or folder never does.
        if(true)return;
        if(id==null||id.isEmpty())return;
        disk.submit(()->store.helpWhenOpenedOf(kind,id),s->{if(s.on&&!s.to.isEmpty())helpSendNow(s.to,s.words);},e->{});
    }

    /** One help request sent now, from this computer, which has no GPS, so without a place (decision 113). Off the UI thread. */
    void helpSendNow(final java.util.List<String> to,final String words) {
        byte[] incident=new byte[Help.INCIDENT];new java.security.SecureRandom().nextBytes(incident);
        final Help.Request request=new Help.Request(incident,0,words==null?"":words,false,0,0,0,0,System.currentTimeMillis());
        network.submit(()->Post.sendHelp(context,store,keys,request,to),done->{},e->{});
    }

    /** Where a help request shows on the PC (decision 113): the tray says it, and a box, one per opening, filled in as updates come. */
    private final java.util.Map<String,javax.swing.JDialog> helpBoxes=new java.util.HashMap<>();
    void helpAlert(Post.Landed landed) {
        final Help.Request r=landed.helpRequest; if(r==null)return;
        java.text.DateFormat when=java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
        final String title=Help.title(landed.helpFrom);
        final String text=Help.body(r.message,r.located,r.lat,r.lon,r.metres,r.located?when.format(new java.util.Date(r.placeAt)):"");
        status.setText(Say.t(title));
        if(tray!=null)tray.displayMessage(Say.t(title),text,TrayIcon.MessageType.WARNING);
        String key=java.util.Base64.getEncoder().encodeToString(r.incident);
        javax.swing.JDialog had=helpBoxes.get(key);
        if(had!=null&&had.isShowing()) {
            javax.swing.JTextArea area=(javax.swing.JTextArea)had.getRootPane().getClientProperty("helpText");
            had.setTitle(Say.t(title));if(area!=null)area.setText(text);had.toFront();return;
        }
        javax.swing.JTextArea area=new javax.swing.JTextArea(text);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);
        area.setFont(DesktopUi.BODY);area.setOpaque(false);area.setBorder(null);
        JPanel body=DesktopUi.column();DesktopUi.add(body,area);
        final javax.swing.JDialog[] boxD={null};
        java.util.List<javax.swing.JComponent> f=new ArrayList<>();
        if(r.located)f.add(DesktopUi.button("Open the map",()->{try{java.awt.Desktop.getDesktop().browse(java.net.URI.create(Help.web(r.lat,r.lon)));}catch(Exception ignored){}}));
        f.add(DesktopUi.button("Close",()->boxD[0].dispose()));
        boxD[0]=DesktopUi.sheet(frame,title,body,DesktopUi.footer(f.toArray(new javax.swing.JComponent[0])),false);
        boxD[0].getRootPane().putClientProperty("helpText",area);
        helpBoxes.put(key,boxD[0]);
        DesktopUi.show(boxD[0],460,560);
    }
    // ---- shared with me for the first time: the pop-up, Accept and Refuse (decision 109) -----------------------------

    /** What the tray has said already this run, by the thing each waits under: once, not once a revision. */
    private final Set<String> trayWaiting=new HashSet<>();
    /** The pop-up while it is up, and what it says so far: redrawn with the new count, never a box over a box. */
    JDialog waitingBox;private final List<NoteStore.Branch> waitingShown=new ArrayList<>();
    private DesktopUi.Text waitingLine,waitingWhat;

    /**
     * Something another person shared with me for the first time, said once (the owner, 2026-10-06: "I need an alert, a
     * pop-up at first telling me"; decision 109): who shared what, what it is and the rights they gave them, and See it, which
     * opens Shared with me, or Later. Several at once are one box, counted. What comes while the box is up redraws it. Only
     * while the window is in front: away, the tray has said it, and the box comes when the window does.
     */
    void askWaiting() {
        if(closing||!frame.isVisible())return;
        disk.submit(()->{List<NoteStore.Branch> unsaid=store.waitingUnsaid();List<String> ids=new ArrayList<>();for(NoteStore.Branch one:unsaid)ids.add(one.id);
            if(!ids.isEmpty())store.waitingSaid(ids);return unsaid;},unsaid->{
            if(unsaid.isEmpty()||closing)return;
            if(waitingBox==null||!waitingBox.isDisplayable())waitingShown.clear();
            for(NoteStore.Branch one:unsaid){boolean known=false;for(NoteStore.Branch shown:waitingShown)if(shown.id.equals(one.id))known=true;if(!known)waitingShown.add(one);}
            List<String> who=new ArrayList<>();
            for(NoteStore.Branch one:waitingShown){String from=one.origin.isEmpty()?"Somebody":store.nameFor(one.origin);if(!who.contains(from))who.add(from);}
            NoteStore.Branch first=waitingShown.get(0);
            String line=FirstShare.title(who,waitingShown.size(),first.name),what=waitingShown.size()==1
                ?Say.t(FirstShare.what(first.kind,levelIn(first.detail)))+". "+Say.t("It waits for you in Shared with me.")
                :"They wait for you in Shared with me, to accept or refuse.";
            if(waitingBox!=null&&waitingBox.isDisplayable()){waitingLine.setText(Say.t(line));waitingWhat.setText(Say.t(what));waitingBox.pack();return;}
            JPanel body=DesktopUi.column();
            waitingLine=DesktopUi.note(line,400,DesktopUi.INK,DesktopUi.BODY.deriveFont(Font.BOLD,16f));waitingWhat=DesktopUi.note(what,400,DesktopUi.QUIET,DesktopUi.BODY);
            DesktopUi.add(body,waitingLine);DesktopUi.gap(body,6);DesktopUi.add(body,waitingWhat);
            JButton see=DesktopUi.primary(FirstShare.SEE_IT,()->{waitingBox.dispose();showHome();home.folder.openPlace(DesktopHome.sharedPlace());}),
                later=DesktopUi.button(FirstShare.LATER,()->waitingBox.dispose());
            waitingBox=DesktopUi.sheet(frame,"Shared with you",body,DesktopUi.footer(later,see),false);
            waitingBox.getRootPane().setDefaultButton(see);
            DesktopUi.show(waitingBox,460,360);
        },this::failed);
    }
    /** The rights a line of Waiting for you names, read back from its words, for the pop-up's line under the name. */
    private static Sharing.Level levelIn(String detail) {
        for(Sharing.Level one:Sharing.Level.values())if(one!=Sharing.Level.GONE&&detail!=null&&detail.endsWith(one.words()))return one;
        return null;
    }

    /**
     * Accepted (decision 109): shown from now on, in Shared with me where it was put as it arrived (decision 122), or on Home
     * where that is where it was kept. Said while it works and once it is done; then it goes to my other devices as anything
     * of mine does.
     */
    void acceptWaiting(NoteStore.Branch thing) {
        status.setToolTipText(null);status.setText(Say.t("Accepting “{0}”…",thing.name));
        disk.submit(()->{store.acceptShared(thing.kind,thing.id);return store.acceptedWhere(thing.kind,thing.id);
        },where->{status.setText(Say.t(FirstShare.accepted(thing.name,where)));refresh();queuePending();
            // And to whoever was told it waits here, that it is accepted: their tick can say "everybody has it" now.
            if(!offline)Post.acceptedNow(context,store,keys);},this::failed);
    }

    /**
     * Refuse, asked in a small box (the owner, 2026-10-06: "refuse could be silent or inform the sender with the option to send
     * a message back"; decision 109): quietly, or telling them, with a few words if they like. Either way it is deleted here
     * and nothing more of it comes. Refuse is the box's one button, in the colour of what cannot be taken back.
     */
    void refuseWaiting(NoteStore.Branch thing) {
        JDialog[] box={null};
        JRadioButton quietly=new JRadioButton(Say.t(FirstShare.QUIETLY),true),tell=new JRadioButton(Say.t(FirstShare.TELL));
        ButtonGroup one=new ButtonGroup();one.add(quietly);one.add(tell);
        JTextField words=new JTextField(28);words.setEnabled(false);words.setName("words");
        words.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,Say.t(FirstShare.WORDS_HINT));
        words.getAccessibleContext().setAccessibleName(FirstShare.WORDS_HINT);
        words.setDocument(new javax.swing.text.PlainDocument(){@Override public void insertString(int at,String s,javax.swing.text.AttributeSet a)throws javax.swing.text.BadLocationException{
            if(s!=null&&getLength()+s.length()>Receipt.MOST_WORDS)s=s.substring(0,Math.max(0,Receipt.MOST_WORDS-getLength()));super.insertString(at,s,a);}});
        JPanel body=DesktopUi.column();
        for(Object[] choice:new Object[][]{{quietly,FirstShare.QUIETLY_DOES},{tell,FirstShare.TELL_DOES}}) {
            JRadioButton pick=(JRadioButton)choice[0];pick.setOpaque(false);pick.setFont(DesktopUi.BODY.deriveFont(Font.BOLD));pick.setForeground(DesktopUi.INK);
            pick.getAccessibleContext().setAccessibleName(pick.getText()+". "+choice[1]);
            DesktopUi.add(body,pick);
            DesktopUi.Text does=DesktopUi.note((String)choice[1],360,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));does.setBorder(BorderFactory.createEmptyBorder(0,26,0,0));
            does.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){pick.doClick();}});
            DesktopUi.add(body,does);DesktopUi.gap(body,8);
        }
        JPanel field=new JPanel(new BorderLayout());field.setOpaque(false);field.setBorder(BorderFactory.createEmptyBorder(0,26,0,0));field.add(words);
        DesktopUi.add(body,field);
        java.awt.event.ActionListener flip=e->{words.setEnabled(tell.isSelected());if(tell.isSelected())words.requestFocusInWindow();};
        quietly.addActionListener(flip);tell.addActionListener(flip);
        JButton go=DesktopUi.danger(FirstShare.REFUSE,()->{
            boolean told=tell.isSelected();String said=told?words.getText().trim():"";
            box[0].dispose();
            status.setToolTipText(null);status.setText(Say.t(told?"Refusing, and telling them…":"Refusing…"));
            network.submit(()->Post.refuse(context,store,keys,thing.kind,thing.id,told,said),
                reached->{status.setText(Say.t(FirstShare.refusedSaid(told,reached)));refresh();},this::failed);
        });
        box[0]=DesktopUi.sheet(frame,FirstShare.refuseTitle(thing.name),body,DesktopUi.footer(go),true);
        box[0].setName("refuse");
        DesktopUi.show(box[0],460,460);
    }

    /**
     * See sender (decision 127): their name, as a person. Block sender is there only when they are not already in
     * People and devices. Blocking keeps what is already here and drops what they send after this.
     */
    void seeSender(NoteStore.Branch thing) {
        final String origin=thing.origin==null?"":thing.origin;
        disk.submit(()->{
            String name=origin.isEmpty()?"Somebody":store.nameFor(origin);
            if(name==null||name.isBlank())name="Somebody";
            boolean block=!origin.isEmpty()&&store.address(origin)==null;
            return new Object[]{name,Boolean.valueOf(block)};
        },got->{
            String name=(String)got[0];boolean block=Boolean.TRUE.equals(got[1]);
            JDialog[] box={null};
            JButton close=DesktopUi.button("Close",()->box[0].dispose());
            JButton stop=block?DesktopUi.danger(FirstShare.BLOCK,()->{
                box[0].dispose();
                status.setToolTipText(null);
                disk.submit(()->{store.blockSender(origin,name);return null;},done->{
                    status.setText(Say.t(FirstShare.BLOCKED_SAID));peopleAgain();
                },this::failed);
            }):null;
            box[0]=DesktopUi.sheet(frame,name,DesktopUi.person(name,null),stop==null?DesktopUi.footer(close):DesktopUi.footer(close,stop),true);
            box[0].setName("sender");
            DesktopUi.show(box[0],420,320);
        },this::failed);
    }

    /**
     * What Share, Sync now and a code are about: what a menu was opened on; else the note on the screen; else the
     * collection whose card is up. Null on Home with nothing chosen.
     */
    private NoteStore.Branch target() {
        // A file too, shared on its own like a note (decision 92).
        if(selected!=null&&(selected.kind==NoteStore.Branch.Kind.LIBRARY||selected.kind==NoteStore.Branch.Kind.COLLECTION||selected.kind==NoteStore.Branch.Kind.BOOK||selected.kind==NoteStore.Branch.Kind.PAGE
            ||selected.kind==NoteStore.Branch.Kind.FILE))return selected;
        if(onPage&&base!=null)return new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,base.id,base.book,Objects.toString(base.title,"").isBlank()?"this note":base.title,"",0,0,false);
        return home==null?null:home.folder.shown();
    }
    /** A time today as the time alone, and any other day with its date. */
    static String shortWhen(long at) {
        java.time.ZonedDateTime then=java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault());
        boolean today=then.toLocalDate().equals(java.time.LocalDate.now());
        return (today?"today ":then.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))+" ")
            +then.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
    static NoteStore.Branch library(){return new NoteStore.Branch(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,"","all notes on this PC","",0,0,true);}
    void share(){share(target(),null,null);}
    /**
     * @param over   the box it is opened from, a group's or a person's page in People and devices; null from the window
     * @param backTo what its ‹ says it goes back to (the owner, 2026-10-05: "we can always come back to the previous level";
     *               decision 103); null where closing it is all there is
     */
    void share(NoteStore.Branch target,Window over,String backTo) {
        // Home is not shared from here (decision 27): everything goes to another device of yours from Profile.
        if(target==null||target.kind==NoteStore.Branch.Kind.LIBRARY){status.setToolTipText(null);
            status.setText(Say.t("Choose a note, a folder or a file to share: open it, or right-click it and choose Share…"));return;}
        Sharing.Scope scope=Sharing.Scope.valueOf(target.kind.name());
        disk.submit(()->{
            List<Sharing.Rule> rules=reaching(store,target);
            // Names looked up now, so each person is drawn once, as themselves.
            Map<String,String> names=new HashMap<>();for(Sharing.Rule rule:rules)names.put(rule.address,store.nameFor(rule.address));
            // Whose it is and whom this PC may change, with what it may give each: see Sharing.mayChange and grantable.
            Map<Sharing.Rule,List<Sharing.Level>> changes=new HashMap<>();Set<Sharing.Rule> owners=new HashSet<>();
            for(Sharing.Rule rule:rules){if(store.ownerOf(rule))owners.add(rule);else if(store.mayChange(rule))changes.put(rule,store.mayGive(rule.scope,rule.target));}
            return new Object[]{rules,store.myLevel(scope,target.id),store.cameFrom(target.kind,target.id),store.pauseFor(target.kind,target.id),names,changes,owners,store.owns(scope,target.id),
                // A file shared on its own still going up, whose sleeve waits for it (decision 94).
                scope==Sharing.Scope.FILE&&!store.looseAudience(target.id).isEmpty()&&!store.sleeveReady(target.id),
                // Who sent a file, and where each device stands with it (the owner, 2026-10-05).
                scope==Sharing.Scope.FILE?store.standing(target.id):null,
                // The groups it is given to, who has it from each, and what this PC may give (decision 100).
                store.groupsOn(scope,target.id),store.fromGroups(scope,target.id),store.groups(),store.mayGive(scope,target.id),
                // And whose Parlons! address is known, for what a person's right-click offers (decision 101).
                parlonsBook=store.parlonsBook(),
                // And who refused it, with their words (decision 109).
                store.refusalsOn(target.kind,target.id),
                // And which are the owner's own devices, with the bond that says what each may do with everything (decision 185).
                ownBonds(),
                // And where it is: the folders above it, or the note a file is kept in (the owner, 2026-10-17: "we should be
                // able to know where the element is located and go directly there").
                whereItIs(target)};
        },data->{
            @SuppressWarnings("unchecked") List<Sharing.Rule> rules=(List<Sharing.Rule>)data[0];
            boolean owner=(Boolean)data[7],admin=owner||data[1]==Sharing.Level.ADMIN;
            JDialog[] box={null};
            // Who has access: you first, then everybody else with their role beside them, as Drive does.
            JPanel people=DesktopUi.column();
            JPanel you=inkOnRound(DesktopUi.person(Node.nameHere(context)+" (you)",null),Node.nameHere(context)+" (you)",null);
            @SuppressWarnings("unchecked") Map<Sharing.Rule,List<Sharing.Level>> changes=(Map<Sharing.Rule,List<Sharing.Level>>)data[5];
            @SuppressWarnings("unchecked") Set<Sharing.Rule> owners=(Set<Sharing.Rule>)data[6];
            Sharing.Level mine=(Sharing.Level)data[1];
            JPanel yourLine=DesktopUi.row(you,roleShown(owner?Sharing.OWNER:mine==null?"":mine.words(),owner?Sharing.OWNER_DOES:mine==null?"":mine.does()));people.add(yourLine);
            DesktopMenus.onRightClick(yourLine,e->{JPopupMenu menu=new JPopupMenu();item(menu,"Writing colour",()->inks(roundOf(you),Node.nameHere(context)+" (you)",null));menu.show(e.getComponent(),e.getX(),e.getY());});
            // The groups it is given to, one line each before the people given it on their own (decision 100): the group and
            // what it may do, who has it from the group quietly under it, dropping down to the rights this PC may give and Remove.
            @SuppressWarnings("unchecked") List<Groups.Given> toGroups=(List<Groups.Given>)data[10];
            @SuppressWarnings("unchecked") Map<String,String> folded=(Map<String,String>)data[11];
            @SuppressWarnings("unchecked") List<Sharing.Level> giveGroups=(List<Sharing.Level>)data[13];
            Map<String,String> groupNames=new HashMap<>();groupNames.put(Groups.MINE,Groups.MY_DEVICES);
            for(Object one:(List<?>)data[12])groupNames.put(((Groups.Group)one).id,((Groups.Group)one).name);
            @SuppressWarnings("unchecked") Map<String,String> namedHere=(Map<String,String>)data[4];
            for(Groups.Given given:toGroups) {
                String group=groupNames.getOrDefault(given.group,"A group");
                List<String> who=new ArrayList<>();
                for(Map.Entry<String,String> one:folded.entrySet())if(one.getValue().equals(given.group))who.add(namedHere.getOrDefault(one.getKey(),"Paired device"));
                who.sort(String.CASE_INSENSITIVE_ORDER);
                java.util.function.Consumer<Sharing.Level> choose=to->groupWork(Say.t(to==Sharing.Level.GONE?"Removing {0}…":"Sharing with {0}…",group),
                    ()->store.shareWithGroup(scope,target.id,given.group,to),to==Sharing.Level.GONE?Say.t("Removed {0}",group):Say.t("Shared with {0}: {1}",group,Say.t(to.words())),()->box[0].dispose());
                JComponent role=giveGroups.isEmpty()?roleShown(given.level.words(),given.level.does()):roleButton(given.level,giveGroups,choose,group);
                JPanel right=new JPanel(new GridBagLayout());right.setOpaque(false);right.add(role);
                JPanel line=DesktopUi.row(DesktopUi.person(group,who.isEmpty()?"Nobody in it has it yet":String.join(", ",who)),right);people.add(line);
                DesktopMenus.onRightClick(line,e->roleMenu(giveGroups,given.level,giveGroups.isEmpty()?null:choose,null,group,null).show(e.getComponent(),e.getX(),e.getY()));
            }
            // One line each (the owner, 2026-10-17: "avoid duplicates"): a person or device's own rule on the thing first, then
            // one from a folder above only where there is none of their own; the owner's own devices a line each, with what
            // each may do with everything, changed there ("we need to be able to change the roles").
            @SuppressWarnings("unchecked") Map<String,Sharing.Rule> bonds=(Map<String,Sharing.Rule>)data[16];
            List<Sharing.Rule> ordered=new ArrayList<>();
            for(Sharing.Rule rule:rules)if(rule.target.equals(target.id))ordered.add(rule);
            for(Sharing.Rule rule:rules)if(!rule.target.equals(target.id))ordered.add(rule);
            Set<String> lined=new HashSet<>();
            for(Sharing.Rule rule:ordered) {
                if(!lined.add(rule.address))continue;
                if(bonds.containsKey(rule.address)){people.add(ownDeviceLine(rule.address,((Map<String,String>)data[4]).getOrDefault(rule.address,"My device"),bonds.get(rule.address),rule.level,owner,box));continue;}
                boolean inherited=!rule.target.equals(target.id);
                // Given by a group: said on the group's line, not again on their own.
                if(!inherited&&folded.containsKey(rule.address))continue;
                // Addresses are never shown in place of a person's name.
                @SuppressWarnings("unchecked") Map<String,String> named=(Map<String,String>)data[4];
                JPanel who=inkOnRound(DesktopUi.person(named.getOrDefault(rule.address,"Paired device"),inherited?(rule.scope==Sharing.Scope.LIBRARY?"Through the library it is in":"Through the folder it is in"):null),
                    named.getOrDefault(rule.address,"Paired device"),rule.address);
                String called=named.getOrDefault(rule.address,"Paired device");
                // Whose it is, said as Owner; a role this PC may change, dropping down to what it may give, each with
                // what it lets them do; any other role said, with what it lets them do under the pointer.
                List<Sharing.Level> give=inherited?null:changes.get(rule);
                java.util.function.Consumer<Sharing.Level> choose=to->{status.setText(Say.t("Updating access…"));disk.submit(()->{
                    store.decide(rule,to,System.currentTimeMillis());return null;
                },done->{box[0].dispose();network.submit(()->{Post.changed(context,store,keys,target.kind,target.id);Post.removedAgain(context,store,keys);return null;},v->{status.setText(Say.t("Access updated"));refresh();},this::failed);},this::failed);};
                JComponent role=owners.contains(rule)?roleShown(Sharing.OWNER,Sharing.OWNER_DOES)
                    :give==null?roleShown(rule.level.words(),inherited?rule.level.does()+" Change this where it was shared.":rule.level.does())
                    :roleButton(rule.level,give,choose,called);
                JPanel right=new JPanel(new GridBagLayout());right.setOpaque(false);right.add(role);
                JPanel line=DesktopUi.row(who,right);people.add(line);
                // Right-click: the same roles, each with what it lets them do, then their writing colour; Remove last.
                DesktopMenus.onRightClick(line,e->roleMenu(give==null?List.of():give,rule.level,give==null?null:choose,roundOf(who),called,rule.address).show(e.getComponent(),e.getX(),e.getY()));
            }
            // Who refused it when it first came to them, and their words where they wrote any (decision 109): off it, and said so.
            @SuppressWarnings("unchecked") List<NoteStore.RefusedBy> refusedIt=(List<NoteStore.RefusedBy>)data[15];
            for(NoteStore.RefusedBy one:refusedIt) {
                JPanel line=DesktopUi.row(DesktopUi.person(one.who,one.words.isEmpty()?null:"“"+one.words+"”"),
                    roleShown(FirstShare.REFUSED,"They refused it when it was shared with them, "+shortWhen(one.at)+"."));
                line.setName("refused "+one.who);people.add(line);
            }
            // Share with opens in this box's place, and going back from it opens this box again, as it stands then.
            JButton add=button("Add someone…",()->{box[0].dispose();DesktopPeople.with(this,target,false,over,target.name,()->share(target,over,backTo));});add.setEnabled(admin);
            JButton code=button("Show my code…",()->{box[0].dispose();showCode(target);});code.setEnabled(admin);
            if(!admin)people.add(DesktopUi.quiet("Only the owner or an admin can add people."));
            // Not in silence: a file still going up says so, and that it goes to them once it is up (decision 94).
            if((Boolean)data[8])people.add(DesktopUi.quiet("Uploading. It goes to them once it is up."));
            DesktopUi.gap(people,DesktopUi.S);DesktopUi.add(people,DesktopUi.actions(add,code));

            // Syncing: when changes go, and whether this PC takes what arrives.
            JComboBox<String> delay=DesktopUi.combo(new String[]{"Right away","After 3 seconds","After 10 seconds","After 30 seconds","After 2 minutes","When I ask"});
            int[] waits={NoteStore.RIGHT_AWAY,3,10,30,120,NoteStore.WHEN_ASKED};int current=(int)data[3];for(int i=0;i<waits.length;i++)if(waits[i]==current)delay.setSelectedIndex(i);
            delay.setEnabled(scope!=Sharing.Scope.LIBRARY);
            delay.addActionListener(e->{int seconds=waits[delay.getSelectedIndex()];disk.submit(()->{store.setPause(target.kind,target.id,seconds);return null;},done->{if(seconds<0)due.remove(target.id);},this::failed);});
            JCheckBox receiving=DesktopUi.toggle("Receive changes",true);receiving.setEnabled(scope!=Sharing.Scope.LIBRARY);
            disk.submit(()->store.pausedHere(target.kind,target.id),paused->receiving.setSelected(!paused),this::failed);
            receiving.addActionListener(e->{boolean pause=!receiving.isSelected();disk.submit(()->{
                if(pause)for(String peer:store.everybodyIn(target.kind,target.id))store.refuse(peer,target.id,target.kind);
                else store.resume(target.kind,target.id);return null;
            },done->refresh(),this::failed);});
            JPanel wait=new JPanel(new GridBagLayout());wait.setOpaque(false);wait.add(delay);
            JPanel syncing=DesktopUi.column();
            syncing.add(DesktopUi.row(DesktopUi.body("Send my changes"),wait));
            DesktopUi.add(syncing,DesktopUi.note(NoteStore.RIGHT_AWAY_COSTS,380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
            syncing.add(DesktopUi.switchRow("Receive their changes",receiving));
            // A stamped file with its Integritas report kept beside it: the report goes with it, where chosen (decision 251).
            if(scope==Sharing.Scope.FILE) {
                JCheckBox report=DesktopUi.toggle("Send its Integritas report too",false);report.setName("reportWith");report.setEnabled(admin);
                JPanel reportRow=DesktopUi.switchRow("Send its Integritas report too",report);reportRow.setVisible(false);syncing.add(reportRow);
                disk.submit(()->new Object[]{store.reportBeside(target.id),store.reportGoesWith(target.id)},got->{
                    if(got[0]==null)return;report.setSelected((Boolean)got[1]);reportRow.setVisible(true);reportRow.revalidate();},this::failed);
                report.addActionListener(e->{boolean on=report.isSelected();disk.submit(()->store.reportGoesWith(target.id,on),given->{
                    status.setText(on?Say.t("Its Integritas report goes with it"):Say.t("Its Integritas report no longer goes with it"));refresh();},this::failed);});
            }

            JPanel body=DesktopUi.column();
            // Where it is, first, and the way there: Home or its folder opened on it, or the note a file is kept in.
            @SuppressWarnings("unchecked") List<String[]> where=(List<String[]>)data[17];
            {
                StringBuilder said=new StringBuilder(Say.t("Home"));for(String[] step:where)said.append("  ›  ").append(step[1]);
                JButton go=DesktopUi.button(Say.t("Show it"),()->{box[0].dispose();goWhereItIs(where);});
                JPanel at=DesktopUi.column();
                at.add(DesktopUi.row(DesktopUi.note(said.toString(),330,DesktopUi.INK,DesktopUi.BODY),go));
                DesktopUi.add(body,DesktopUi.card("Where it is",at));DesktopUi.gap(body,12);
            }
            // A file: who sent it, and where every device that is to have it stands, each in a word or three.
            NoteStore.Standing standing=(NoteStore.Standing)data[9];
            if(standing!=null&&(!standing.from.isEmpty()||!standing.said.isEmpty())) {
                JPanel devices=DesktopUi.column();
                if(!standing.from.isEmpty())devices.add(DesktopUi.row(DesktopUi.body("From"),DesktopUi.quiet(standing.fromName+" · "+shortWhen(standing.at))));
                for(Map.Entry<String,String> one:standing.said.entrySet())
                    devices.add(DesktopUi.row(DesktopUi.body(standing.names.getOrDefault(one.getKey(),"Another device")),DesktopUi.quiet(one.getValue())));
                DesktopUi.add(body,DesktopUi.card("Devices",devices));DesktopUi.gap(body,12);
            }
            DesktopUi.add(body,DesktopUi.card("Who has access",people));
            // A file on its own goes whole, when it is renamed or replaced, never as it is typed in (decisions 92, 93): nothing to time, or to pause.
            if(scope!=Sharing.Scope.FILE){DesktopUi.gap(body,12);DesktopUi.add(body,DesktopUi.card("Syncing",syncing));}
            JButton leave=((String)data[2]).isEmpty()?null:DesktopUi.danger("Unfollow…",()->{
                if(!DesktopUi.confirm(box[0],"Unfollow?","Stop receiving "+target.name+"? Your copy stays on this PC.","Unfollow",true))return;
                box[0].dispose();status.setText(Say.t("Unfollowing…"));network.submit(()->{Post.leave(context,store,keys,target.kind,target.id);return null;},done->{status.setText(Say.t("Unfollowed. Your copy stays here"));refresh();},this::failed);
            });
            JPanel foot=new JPanel(new BorderLayout());foot.setOpaque(false);
            if(leave!=null)foot.add(DesktopUi.actions(leave),BorderLayout.WEST);
            foot.add(DesktopUi.footer(button("Sync now",()->{box[0].dispose();sync(true);})),BorderLayout.EAST);
            box[0]=DesktopUi.sheet(over==null?frame:over,Say.t("Share “{0}”",target.name),DesktopUi.scrolling(body),foot,true);
            if(backTo!=null)DesktopUi.head(box[0],backTo,box[0]::dispose,Say.t("Share “{0}”",target.name),null);
            // Closed over a page of People and devices, that page is drawn again: what it says may have changed here.
            if(over!=null)box[0].addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){peopleAgain();}});
            DesktopUi.show(box[0],520,720);
        },this::failed);
    }
    /**
     * Where a thing is, as the steps down to it from Home: each folder above it, {id, name, "folder"}, and for a file kept in
     * a note, that note last, {id, title, "note"}. Empty for one on Home. On the disk thread.
     */
    private List<String[]> whereItIs(NoteStore.Branch thing) {
        List<String[]> out=new ArrayList<>();
        List<String> path=thing.kind==NoteStore.Branch.Kind.FILE?store.filePath(thing.id):store.pathOf(thing.id);
        for(String id:path) {
            if(id.equals(thing.id))continue;
            NoteStore.Note note=store.get(id);
            if(note!=null){out.add(new String[]{id,DesktopOnDisk.name(note),"note"});continue;}
            String name=store.collectionName(id);
            if(name!=null&&!name.isEmpty())out.add(new String[]{id,name,"folder"});
        }
        return out;
    }
    /** Shown where it is: the note a file is kept in, opened; else the folder it is in, opened on Home; else Home. */
    void goWhereItIs(List<String[]> where) {
        if(where.isEmpty()){save(this::goHome);return;}
        String[] last=where.get(where.size()-1);
        if("note".equals(last[2]))save(()->open(last[0]));else openCollection(last[0]);
    }

    /** The owner's own devices, by address, with the bond (everything, on its own) that says what each may do. On the disk thread. */
    Map<String,Sharing.Rule> ownBonds() {
        // Every device of mine, with its bond where it has one; one with none is in the map too, with no rule (null), so it
        // is never drawn as the owner with nothing to change (the owner, 2026-10-17: "Owner should be in a dropdown").
        Map<String,Sharing.Rule> out=new HashMap<>();
        for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())out.put(one.address,null);
        for(Sharing.Rule rule:store.shares())if(rule.scope==Sharing.Scope.LIBRARY&&out.containsKey(rule.address)&&rule.level!=Sharing.Level.GONE)out.put(rule.address,rule);
        return out;
    }

    /**
     * One of the owner's own devices on a thing's Share box: it has everything, as the owner; what it may do with it, Can
     * write or Can read (decision 180), dropping down to change it, for every thing at once, as Add my device set it.
     */
    private JPanel ownDeviceLine(String address,String name,Sharing.Rule bond,Sharing.Level onThis,boolean owner,JDialog[] box) {
        // One with no bond with everything has this thing as it was given it, said before the words (decision 255).
        String under=bond!=null?Say.t("One of your devices, with everything")
            :(onThis==null||onThis==Sharing.Level.GONE?"":Say.t(onThis.words())+"  ·  ")+Say.t("One of your devices, not given everything");
        JPanel who=inkOnRound(DesktopUi.person(name,under),name,address);
        JComponent role=ownDeviceRole(address,name,bond,owner,()->{if(box[0]!=null)box[0].dispose();});
        JPanel right=new JPanel(new GridBagLayout());right.setOpaque(false);right.add(role);
        return DesktopUi.row(who,right);
    }
    /**
     * What one of my devices may do with everything, dropping down to Can write, Can read and Remove (decision 189); one with
     * no bond, added with nothing yet, says Nothing yet and drops down to Can write and Can read of everything (decision 255).
     */
    JComponent ownDeviceRole(String address,String name,Sharing.Rule bond,boolean owner,Runnable after) {
        java.util.function.Consumer<Sharing.Level> choose=to->{status.setText(Say.t("Updating access…"));disk.submit(()->{
            // No bond yet: one made, at what was chosen; else the bond changed, or ended with Remove.
            if(bond==null){if(to!=Sharing.Level.GONE)store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,address,to,name);}
            else store.decide(bond,to,System.currentTimeMillis());return null;
        },done->{after.run();network.submit(()->{Post.send(context,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING);return null;},v->{status.setText(Say.t("Access updated"));refresh();},this::failed);},this::failed);};
        List<Sharing.Level> give=List.of(Sharing.Level.WRITE,Sharing.Level.READ);
        if(bond==null) {
            if(!owner)return roleShown(Sharing.NOTHING_YET,Sharing.NOTHING_YET_DOES);
            JButton role=new JButton(Say.t(Sharing.NOTHING_YET)+"  ▾");role.setFocusPainted(false);role.setToolTipText(Say.t(Sharing.NOTHING_YET_DOES));
            role.getAccessibleContext().setAccessibleName("What "+name+" may do: "+Sharing.NOTHING_YET+". "+Sharing.NOTHING_YET_DOES);
            role.addActionListener(e->{
                JPopupMenu menu=new JPopupMenu();
                menu.add(new JRadioButtonMenuItem(DesktopUi.roleLine(Sharing.NOTHING_YET,Sharing.NOTHING_YET_DOES),true));
                for(Sharing.Level level:give) {
                    JRadioButtonMenuItem one=new JRadioButtonMenuItem(DesktopUi.roleLine(level.words(),Sharing.everythingDoes(level)),false);
                    one.addActionListener(a->choose.accept(level));menu.add(one);
                }
                menu.show(role,0,role.getHeight());
            });
            return role;
        }
        Sharing.Level now=bond.level;
        return owner?roleButton(now,give,choose,name):roleShown(now.words(),now.does());
    }

    /**
     * Every rule that reaches a thing, as its Share box lists them: on everything, on itself, or on any collection above
     * it however deep, at whatever level the rule was written - a 0.1 collection's or book's, or a THING (see
     * Sharing.covers). On the disk thread.
     */
    static List<Sharing.Rule> reaching(NoteStore store,NoteStore.Branch target) {
        List<String> path=target.kind==NoteStore.Branch.Kind.LIBRARY?List.of():target.kind==NoteStore.Branch.Kind.FILE?store.filePath(target.id):store.pathOf(target.id);
        List<Sharing.Rule> rules=new ArrayList<>();
        for(Sharing.Rule rule:store.shares())if(Sharing.covers(rule,path))rules.add(rule);
        return rules;
    }
    /**
     * A person's line in Share, right-clicked: the roles this PC may give them, each over what it lets them do and the
     * one they have ticked; their writing colour where there is a round to colour; and Remove, last. Where this PC may
     * change nothing about them, their role alone, said, and nothing to choose.
     *
     * @param choose what a role picked does, GONE for Remove; null where nothing may be changed here
     */
    private JPopupMenu roleMenu(List<Sharing.Level> give,Sharing.Level now,java.util.function.Consumer<Sharing.Level> choose,Component round,String name,String address) {
        JPopupMenu menu=new JPopupMenu();
        List<Sharing.Level> shown=new ArrayList<>(give);
        if(!shown.contains(now)){shown.add(now);shown.sort((a,b)->b.ordinal()-a.ordinal());}
        for(Sharing.Level level:shown) {
            JRadioButtonMenuItem one=new JRadioButtonMenuItem(DesktopUi.roleLine(level.words(),level.does()),level==now);
            one.setEnabled(choose!=null&&(give.contains(level)||level==now));one.getAccessibleContext().setAccessibleName(level.words()+". "+level.does());
            one.addActionListener(a->{if(level!=now)choose.accept(level);});menu.add(one);
        }
        if(round!=null){menu.addSeparator();item(menu,"Writing colour",()->inks(round,name,address));}
        // Contacting them on Parlons!, or giving them an address to be contacted at (decision 101).
        if(round!=null&&address!=null)item(menu,parlonsWords(address),()->contactOnParlons(address));
        if(choose!=null){menu.addSeparator();JMenuItem remove=DesktopMenus.item("Remove");remove.addActionListener(a->{
            if(DesktopUi.confirm(frame,"Remove "+name+"?","They stop getting changes. Their copy stays on their device, and they are told.","Remove",true))choose.accept(Sharing.Level.GONE);});menu.add(remove);}
        return menu;
    }
    /** A role that may be changed here: its name and a ▾, dropping down to the roles this PC may give. */
    JButton roleButton(Sharing.Level now,List<Sharing.Level> give,java.util.function.Consumer<Sharing.Level> choose,String name) {
        JButton role=new JButton(Say.t(now.words())+"  ▾");role.setFocusPainted(false);role.setToolTipText(Say.t(now.does()));
        role.getAccessibleContext().setAccessibleName("What "+name+" may do: "+now.words()+". "+now.does());
        role.addActionListener(e->roleMenu(give,now,choose,null,name,null).show(role,0,role.getHeight()));
        return role;
    }
    /** A role that is not changed here, said quietly, with what it lets them do under the pointer. */
    static JComponent roleShown(String role,String does) {
        DesktopUi.Text said=DesktopUi.quiet(role);said.setToolTipText(does.isEmpty()?null:Say.t(does));
        said.getAccessibleContext().setAccessibleName(does.isEmpty()?role:role+". "+does);
        return said;
    }
    /**
     * My link and the few words that go with it, on the clipboard, for a message, a mail or a social network (the owner,
     * 2026-10-06; decision 110). The phone hands them to its share sheet; this PC has the clipboard.
     */
    void copyMyLink() {
        if(offline){status.setText(Say.t("Offline: there is no address to share."));return;}
        status.setText(Say.t("Getting your link…"));
        connectivity.submit(()->{
            String called=Node.nameHere(context);var addresses=Node.addresses(context);String live=addresses.isEmpty()?"":addresses.get(0);
            return live.isEmpty()?"":Pairing.invite(called,Pairing.link(keys.line(called,live,"",false,"","")),DOWNLOAD);
        },words->{
            if(words.isEmpty()){status.setText(Say.t("No address yet. Wait until this PC is connected."));return;}
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(words),null);
            status.setText(Say.t("Your link is copied, with a few words. Paste it in a message, a mail or a post."));
        },this::failed);
    }
    void showCode(NoteStore.Branch target) {
        if(offline){status.setText(Say.t("This session was started offline"));return;}
        Sharing.Scope scope=Sharing.Scope.valueOf(target.kind.name());
        // Everything is offered only to a device of mine, in its own box (decision 255).
        if(scope==Sharing.Scope.LIBRARY){addMyDevice();return;}
        disk.submit(()->store.mayGive(scope,target.id),may->{
            if(may.isEmpty()){status.setText(Say.t("Only its owner or an admin can share {0}",target.name));return;}
            Sharing.Level role=offerRole(target.name,may);if(role!=null)showCode(target,scope,role);
        },this::failed);
    }
    /**
     * Add my device (decision 255; the owner, 2026-10-25: "from people and device, I wanted to only have the new device in
     * my connected device, not necessary share everything. Just adding it and then choosing"): one box, up at once, with
     * what it gets first, Nothing yet unless Can write or Can read of everything is chosen, and the code under it. The code is
     * made on a thread of its own and drawn when it is ready: it waited behind whatever the network worker was sending, after
     * a box asking for a role, and the + seemed to do nothing (the owner on the Pro: "took a bit long... but finally worked").
     */
    void addMyDevice() {
        if(offline){status.setText(Say.t("This session was started offline"));return;}
        List<Sharing.Level> picks=new ArrayList<>(Arrays.asList(null,Sharing.Level.WRITE,Sharing.Level.READ));
        Sharing.Level[] chosen={null};String[] line={""},where={null};int[] asked={0};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.heading("What it gets"));DesktopUi.gap(body,8);
        JLabel picture=new JLabel(Say.t("Preparing sharing code…"),SwingConstants.CENTER);picture.setForeground(DesktopUi.QUIET);
        picture.setPreferredSize(new Dimension(340,340));picture.setAlignmentX(Component.CENTER_ALIGNMENT);
        DesktopUi.Text copied=DesktopUi.quiet(" ");
        JButton link=button("Copy link",()->{if(line[0].isEmpty())return;Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(Pairing.link(line[0])),null);copied.setText(Say.t("Link copied. Send it to them another way if they cannot scan."));});
        link.setEnabled(false);
        Runnable make=()->{
            int mine=++asked[0];Sharing.Level level=chosen[0];line[0]="";link.setEnabled(false);
            picture.setIcon(null);picture.setText(Say.t("Preparing sharing code…"));
            String target=level==null?Sharing.MINE_ONLY:Sharing.EVERYTHING,offer=level==null?Sharing.MINE_OFFER:Sharing.travelling(Sharing.Scope.LIBRARY,"Everything");
            Sharing.Level offered=level==null?Sharing.Level.READ:level;
            new Thread(()->{
                try {
                    if(where[0]==null)where[0]=address();
                    String made=keys.line(Node.nameHere(context),where[0],offer,offered,Sharing.Scope.LIBRARY.name(),target);
                    BufferedImage image=DesktopQr.draw(Pairing.link(made),340);
                    SwingUtilities.invokeLater(()->{
                        if(mine!=asked[0])return;
                        // Showing the code is the decision: whoever scans it within a quarter of an hour is added, as it says.
                        String key=Sharing.Scope.LIBRARY.name()+":"+target;offers.put(key,offered);offeredAt.put(key,System.currentTimeMillis());
                        line[0]=made;picture.setText(null);picture.setIcon(new ImageIcon(image));link.setEnabled(true);
                        picture.getAccessibleContext().setAccessibleName(Say.t("Scan this code with Mininotes on your phone"));
                    });
                } catch(Exception failed) {
                    String said=failed.getMessage()==null?Say.t("That could not be changed."):Say.t(failed.getMessage());
                    SwingUtilities.invokeLater(()->{if(mine==asked[0])picture.setText("<html><div style='width:300px;text-align:center'>"+said+"</div></html>");});
                }
            },"mininotes-code").start();
        };
        ButtonGroup group=new ButtonGroup();
        for(Sharing.Level level:picks) {
            JRadioButton one=new JRadioButton(Say.t(level==null?Sharing.NOTHING_YET:level.words()),level==null);one.setOpaque(false);
            one.setFont(DesktopUi.BODY.deriveFont(Font.BOLD));one.setForeground(DesktopUi.INK);
            one.getAccessibleContext().setAccessibleName(Say.t(level==null?Sharing.NOTHING_YET:level.words())+". "+Say.t(Sharing.everythingDoes(level)));
            one.addActionListener(e->{if(chosen[0]!=level){chosen[0]=level;make.run();}});
            group.add(one);DesktopUi.add(body,one);
            DesktopUi.Text does=DesktopUi.note(Sharing.everythingDoes(level),360,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));does.setBorder(BorderFactory.createEmptyBorder(0,26,0,0));
            does.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){one.doClick();}});
            DesktopUi.add(body,does);DesktopUi.gap(body,8);
        }
        DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.note("On your other device, open Mininotes and scan this code."));DesktopUi.gap(body,DesktopUi.M);
        JPanel framed=new JPanel(new GridBagLayout());framed.setOpaque(false);framed.add(DesktopUi.card(null,picture));DesktopUi.add(body,framed);
        DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.actions(link));DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,copied);
        // Notes only between the owner's devices: the code carries no relay, so only a device on this network can use it.
        if(Node.onlyMine(context)){DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.quiet("This code works only for devices on this network: this PC sends notes only between your devices."));}
        status.setText(Say.t("Preparing sharing code…"));
        make.run();
        JDialog box=DesktopUi.sheet(frame,"Add my device",DesktopUi.scrolling(body),null,true);
        DesktopUi.show(box,460,760);
        // Closed: a code still being made is not drawn into a box nobody sees.
        asked[0]++;status.setText(" ");
    }
    /** What the code will let them do: the roles this PC may give, each over what it lets them do. Null if none was chosen. */
    Sharing.Level offerRole(String name,List<Sharing.Level> may) {
        return DesktopUi.chooseRole(frame,"Share “"+name+"”","Rights",may,firstRole(may),"Show my code");
    }
    /** The role a choice starts from: Can write where it may be given, else Can read, else the first. */
    static Sharing.Level firstRole(List<Sharing.Level> may) {
        return may.contains(Sharing.Level.WRITE)?Sharing.Level.WRITE:may.contains(Sharing.Level.READ)?Sharing.Level.READ:may.get(0);
    }
    /**
     * Somebody on another network while notes go only between the owner's devices, which reach nothing beyond this Wi-Fi:
     * said so, with helpers offered there and then, and what was being done done again with them (the owner, 2026-10-02:
     * "make sure we can share with everybody"). Not now, and it is kept.
     */
    void helpersFor(String name,Runnable then) {
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note("This PC sends notes only between your devices, on the same network, so it cannot reach "+name
            +". Helpers can: relays that pass sealed notes on, which they cannot read. Their device needs them too, if it has them off."));
        DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.quiet("This can be changed back in Settings, How notes travel."));
        boolean[] yes={false};JDialog[] box={null};
        JButton use=DesktopUi.primary("Use helpers",()->{yes[0]=true;box[0].dispose();});
        box[0]=DesktopUi.sheet(frame,name+" is on another network",body,DesktopUi.footer(use),true);
        DesktopUi.show(box[0],440,320);
        if(!yes[0]){status.setText(Say.t("Saved {0}. This PC tells them again at each start.",name));return;}
        status.setText(Say.t("Connecting to your relays…"));
        connectivity.submit(()->{Node.onlyMine(context,false);return null;},
            done->{status.setText(Say.t("Helpers carry notes when needed now"));then.run();},
            failure->status.setText(Say.t("That could not be changed.")));
    }
    private void showCode(NoteStore.Branch target,Sharing.Scope scope,Sharing.Level role) {
        status.setText(Say.t("Preparing sharing code…"));
        network.submit(()->keys.line(Node.nameHere(context),address(),Sharing.travelling(scope,target.name),role,scope.name(),target.id),line->{
            offers.put(scope.name()+":"+target.id,role);offeredAt.put(scope.name()+":"+target.id,System.currentTimeMillis());status.setText(Say.t("Scan this code with Mininotes on your phone"));
            try {
                BitMatrix matrix=new MultiFormatWriter().encode(Pairing.link(line),BarcodeFormat.QR_CODE,340,340,Map.of(EncodeHintType.MARGIN,2));
                BufferedImage image=new BufferedImage(matrix.getWidth(),matrix.getHeight(),BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<matrix.getHeight();y++)for(int x=0;x<matrix.getWidth();x++)image.setRGB(x,y,matrix.get(x,y)?0xff17261b:0xffffffff);
                JLabel picture=new JLabel(new ImageIcon(image));picture.setAlignmentX(Component.CENTER_ALIGNMENT);
                JPanel body=DesktopUi.column();
                DesktopUi.Text how=DesktopUi.note("On the phone, open Mininotes and scan this code. They get "+target.name+" as "+role.words()+": "+role.does());
                DesktopUi.add(body,how);DesktopUi.gap(body,DesktopUi.M);
                JPanel framed=new JPanel(new GridBagLayout());framed.setOpaque(false);framed.add(DesktopUi.card(null,picture));DesktopUi.add(body,framed);
                DesktopUi.Text copied=DesktopUi.quiet(" ");
                JButton link=button("Copy link",()->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(Pairing.link(line)),null);copied.setText(Say.t("Link copied. Send it to them another way if they cannot scan."));});
                DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.actions(link));DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,copied);
                // A file on its own goes only to a build that knows them (decision 93): said before anybody scans it with an older one.
                if(scope==Sharing.Scope.FILE){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.quiet(Sharing.FILE_NEEDS));}

                // Notes only between the owner's devices: the code carries no relay, so only a device on this network can use it.
                if(Node.onlyMine(context)) {
                    DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.quiet("This code works only for devices on this network: this PC sends notes only between your devices."));
                    JButton anywhere=button("Use helpers, so anybody anywhere can use it",()->{
                        Window up=SwingUtilities.getWindowAncestor(body);if(up!=null)up.dispose();
                        status.setText(Say.t("Connecting to your relays…"));
                        connectivity.submit(()->{Node.onlyMine(context,false);return null;},
                            done->{status.setText(Say.t("Helpers carry notes when needed now"));showCode(target,scope,role);},
                            failure->status.setText(Say.t("That could not be changed.")));
                    });
                    DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.actions(anywhere));
                }
                DesktopUi.tell(frame,Say.t("Share “{0}”",target.name),body);
            } catch(Exception e){failed(e);}
        },this::failed);
    }
    /**
     * A code from another device: the camera at once, a picture or a link as the quiet other ways, and, from People and
     * devices, my own code instead (decision 258). What is read goes into the one path (DesktopJoining).
     */
    void scanCode(NoteStore.Branch shareTarget){scanCode(shareTarget,null,null);}
    /**
     * @param mine where Show my code instead leads, or null where the scan screen does not offer it
     * @param back the box it was opened from, opened again when the scan or the path is left, and once the path is done;
     *             null for none
     */
    void scanCode(NoteStore.Branch shareTarget,Runnable mine,Runnable back) {
        if(offline){status.setText(Say.t("This session was started offline"));return;}
        DesktopScanner.open(frame,text->receiveCode(text,shareTarget,back),mine,back);
    }
    /**
     * A code read, scanned or pasted, into the one path (decision 258): what it is, a group, what to share, done. Read for
     * what it is first: anything that is not a Mininotes code is refused, and nothing is kept. A pasted one has six digits
     * to check, worked out off this thread.
     */
    private void receiveCode(DesktopScanner.Code code,NoteStore.Branch shareTarget){receiveCode(code,shareTarget,null);}
    private void receiveCode(DesktopScanner.Code code,NoteStore.Branch shareTarget,Runnable back) {
        try {
            Pairing.Said said=Pairing.read(Pairing.line(code.text().trim()));
            disk.submit(()->code.camera()?"":Envelope.code(keys.signing().getPublic(),Keys.publicKey(said.signing)),
                digits->DesktopJoining.open(this,said,digits,shareTarget,back),this::failed);
        } catch(Exception e){failed(e);if(back!=null)back.run();}
    }
    /**
     * The path's end (decision 258): paired as it was chosen, one of mine or not, told so, then the group and the shares
     * given and sent, the status line saying each step. {@code ended} has what the box says at its last step: how it went,
     * and a second line or null; {@code gone} closes it where the helpers' box takes over.
     */
    void joined(Pairing.Said said,Joining.Choice choice,java.util.function.BiConsumer<String,String> ended,Runnable gone) {
        boolean[] elsewhere={false},told={false};int[] failed={0};
        status.setText(Say.t("Pairing with {0}…",said.name));network.submit(()->{
            Joining.paired(context,store,said,choice.mine);
            // Only between the owner's devices, a code from another network is saved and said plainly.
            try{String key=Node.introduce(context,said.address);if(!key.isEmpty())store.knownAs(said.address,key);}catch(Exception unreachable){elsewhere[0]=Node.ON_THE_SAME_WIFI.equals(unreachable.getMessage());/* The code's address remains usable. */}
            // A plain code too: the device that showed it has to hear about this PC, or it drops as a
            // stranger's everything this PC shares with it. Kept, and said again at each start until answered.
            Joining.accepting(store,said);
            try{Post.accept(context,keys,said,Node.nameHere(context),address());told[0]=true;}catch(Exception notNow){/* said again until they hear */}
            Joining.Kept kept=Joining.chosen(store,said,choice);
            if(!kept.everything&&!kept.grouped&&kept.given.isEmpty())return kept;
            SwingUtilities.invokeLater(()->status.setText(Say.t("Sharing with {0}…",said.name)));
            try{failed[0]=Joining.send(context,store,keys,kept);}catch(Exception notNow){failed[0]=1;}
            return kept;
        },kept->{
            refresh();peopleAgain();
            if(elsewhere[0]){gone.run();helpersFor(said.name,()->joined(said,choice,(a,b)->{},()->{}));return;}
            String how=failed[0]>0?Say.t("Shared, waiting to send")
                :Joining.offers(said)?Say.t("Paired, waiting for {0} to approve and send",said.name)
                :told[0]?Say.t("Waiting for {0} to accept",said.name):Say.t("Added. {0} is asked when it is next reachable",said.name);
            status.setText(Joining.done(said.name,choice.mine)+"  ·  "+how);
            // One of mine chosen for a plain code: the other device counts this one as a person until its switch is turned.
            String more=choice.mine&&!Joining.saysMine(said)?Say.t("On {0}, turn My device on too.",said.name):null;
            ended.accept(how,more);
        },e->{status.setText(Say.t("Pairing has not finished. Reopen the app to retry, or try the link again."));ended.accept(Say.t("Pairing has not finished. Reopen the app to retry, or try the link again."),null);});
    }
    private void accepted(Hello.Said said) {
        // A plain code, offering nothing: an invitation, Accept or Ignore (decision 163), but for a device added already.
        if(said.target.isEmpty()){invited(said);return;}
        // A build from before quotes back the scope the code gave it, with the level after it: see Pairing.scopeIn.
        String bare=Pairing.scopeIn(said.scope);
        String key=bare+":"+said.target;Sharing.Level offered=offers.get(key);
        // A reply cannot upgrade its offer, refer to an unoffered item, or bypass consent. What it claims is given:
        // Can write from a build that read an Admin offer as that, never more than was offered.
        if(offered==null||said.level.ordinal()>offered.ordinal()||!pendingOffers.add(key+said.address))return;
        Sharing.Level level=said.level;
        if(!frame.isVisible()){DesktopLock.quietUnlessLet(frame);frame.setVisible(true);Node.near(true);if(DesktopLock.front.mayTake())frame.toFront();}
        // Showing the code was the decision: scanned within a quarter of an hour, it is handed over without asking again.
        Long shown=offeredAt.get(key);long age=shown==null?Long.MAX_VALUE:System.currentTimeMillis()-shown;
        // Add my device with nothing yet (decision 255): added as one of mine, given nothing; asked first where the code is old.
        if(Sharing.mineOnly(bare,said.target)) {
            boolean add=age>=0&&age<15*60_000L||DesktopUi.confirm(frame,Say.t("Add {0} as one of your devices?",said.name),Say.t("{0} scanned your code to be one of your devices.",said.name),Say.t("Add it"),false);
            pendingOffers.remove(key+said.address);if(add)addMine(said);
            return;
        }
        boolean give=age>=0&&age<15*60_000L||DesktopUi.confirm(frame,said.name+" scanned your code",said.name+" accepted what you offered. Give them "+level.words()+"? "+level.does(),"Give access",false);
        pendingOffers.remove(key+said.address);if(!give)return;
        Sharing.Scope scope=Sharing.Scope.valueOf(bare);NoteStore.Branch.Kind kind=NoteStore.kindFor(scope);
        status.setText(Say.t("Sharing with {0}…",said.name));network.submit(()->{
            if(!store.mayGive(scope,said.target).contains(level))throw new IllegalStateException("Only its owner or an admin can share this");
            // Everything, accepted: another device of the owner's, marked as theirs here (see Persons) and given everything.
            if(scope==Sharing.Scope.LIBRARY)Post.addedAsMine(context,store,said,level);
            else store.pairedWith(said.address,said.name,false,said.agreement,said.signing);
            try{String contact=Node.introduce(context,said.address);if(!contact.isEmpty())store.knownAs(said.address,contact);}catch(Exception unreachable){/* Fallback to the current address. */}
            if(scope!=Sharing.Scope.LIBRARY)store.give(scope,said.target,said.address,level,null);
            return Post.send(context,store,keys,kind,said.target);
        },done->{
            // The open note given to them: how its sending went is said on its line, as any other send of it is.
            if(isOpen(said.target)){status.setText(Say.t("Shared with {0}",said.name));noteSent(done,false);refresh();return;}
            status.setText(Say.t(done.failed>0?"Shared, waiting to send":"Sent, waiting for delivery confirmation"));refresh();if(done.failed>0)tellUnsent(done);},this::failed);
    }
    /**
     * A device that scanned Add my device offering nothing yet (decision 255): one of mine, with the cards a device of mine
     * has, and nothing of everything; answered, so it stops saying that it accepted. What it gets is chosen on its line.
     */
    private void addMine(Hello.Said said) {
        status.setText(Say.t("Pairing with {0}…",said.name));network.submit(()->{
            Post.addedAsMine(context,store,said,null);
            try{String contact=Node.introduce(context,said.address);if(!contact.isEmpty())store.knownAs(said.address,contact);}catch(Exception unreachable){/* the address they sent still works */}
            NoteStore.Contact saved=store.address(said.address);
            try{Post.helloBack(context,keys,said,Node.nameHere(context),address(),saved==null?null:saved.contact);}catch(Exception notNow){/* they say it again until they hear */}
            return null;
        },done->{status.setText(Say.t("{0} is one of your devices now. Nothing is shared with it yet.",said.name));refresh();peopleAgain();},this::failed);
    }
    /**
     * Somebody scanned this PC's plain code (decision 163: "let's make that as an invitation, for contact and for elements,
     * all this needs to be accepted or ignored"): one already added is answered as before; one ignored, nothing; anybody
     * else waits to be accepted, asked now.
     */
    private void invited(Hello.Said said) {
        disk.submit(()->Invites.known(store,said)?1:Invites.ignored(context,said)?2:Invites.add(context,said)?3:4,how->{
            if(how==1){pairBack(said);return;}
            if(how==2)return;
            askInvite(said);
        },this::failed);
    }
    private final Set<String> invitesAsked=new HashSet<>();
    /** One invitation: Accept adds them and answers; Ignore tells nobody; Later leaves it to be asked again. */
    private void askInvite(Hello.Said said) {
        if(!invitesAsked.add(said.address))return;
        if(!frame.isVisible()&&tray!=null)tray.displayMessage("Mininotes",Say.t("{0} wants to add you",said.name),TrayIcon.MessageType.INFO);
        Object[] ways={Say.t("Accept"),Say.t("Ignore"),Say.t("Later")};
        SwingUtilities.invokeLater(()->{
            int picked=JOptionPane.showOptionDialog(frame,Say.t("Accept, and you can share notes and files with {0}. Ignore, and {0} is told nothing.",said.name),
                Say.t("{0} wants to add you",said.name),JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,ways,ways[0]);
            if(picked==0)disk.submit(()->{Invites.done(context,said,false);return null;},done->pairBack(said),this::failed);
            else if(picked==1)disk.submit(()->{Invites.done(context,said,true);return null;},done->status.setText(Say.t("Ignored")),this::failed);
            else invitesAsked.remove(said.address);
        });
    }
    /** What waits to be accepted or ignored, asked once the window is up. */
    void askInvites(){disk.submit(()->Invites.waiting(context),waiting->{for(Hello.Said one:waiting)askInvite(one);},e->{});}

    /** Somebody scanned this PC's code with nothing offered: asked, then saved and answered. */
    private void pairBack(Hello.Said said) {
        if(!pendingOffers.add("pair:"+said.address))return;
        // Somebody scanned the code this PC showed: showing it was the decision, so they are paired back without a question.
        pendingOffers.remove("pair:"+said.address);
        status.setText(Say.t("Pairing with {0}…",said.name));network.submit(()->{
            store.pairedWith(said.address,said.name,false,said.agreement,said.signing);
            try{String contact=Node.introduce(context,said.address);if(!contact.isEmpty())store.knownAs(said.address,contact);}catch(Exception unreachable){/* the address they sent still works */}
            NoteStore.Contact saved=store.address(said.address);
            try{Post.helloBack(context,keys,said,Node.nameHere(context),address(),saved==null?null:saved.contact);}catch(Exception notNow){/* they say hello again until they hear */}
            return null;
        },done->{status.setText(Say.t("{0} paired with this PC",said.name));if(tray!=null&&!frame.isVisible())tray.displayMessage("Mininotes",Say.t("{0} paired with this PC",said.name),TrayIcon.MessageType.INFO);refresh();},this::failed);
    }
    /** When somebody last typed or clicked in Mininotes; the notebook locks again after the time chosen in Security. */
    private volatile long lastUse=System.currentTimeMillis();
    private final AWTEventListener using=e->lastUse=System.currentTimeMillis();
    private javax.swing.Timer idle;
    private boolean relocking;
    /** What closing the unlock window after a re-lock does: leaves Mininotes. Tests put something quieter in. */
    Runnable leave=()->System.exit(0);

    private void relockIfIdle() {
        // Recording is using it, with no key pressed: a lecture should not lock the notebook halfway through.
        if(closing||relocking||recorder!=null&&recorder.recording())return;
        java.nio.file.Path folder=context.getFilesDir().toPath();
        int minutes=DesktopLock.minutes(context);
        if(minutes<=0||!DesktopLock.locked(folder))return;
        if(System.currentTimeMillis()-lastUse<minutes*60_000L)return;
        relock();
    }

    /**
     * Locked again: writing saved, the notebook closed and its key let go, and the password asked for as at
     * start. What arrives meanwhile waits sealed in the inbox, and is taken in when it is opened.
     */
    void relock() {
        relocking=true;
        java.nio.file.Path folder=context.getFilesDir().toPath();boolean wasOffline=offline;
        // Locked again while the window was closed to the tray: the question waits until Mininotes is opened
        // again. Asked at once, Windows Hello came up over whatever the owner was doing, with the taskbar
        // flashing, for a window they had put away.
        boolean away=!frame.isVisible()&&tray!=null;
        save(()->{
            if(!wasOffline)DesktopFiles.toInbox(folder);
            // Said to the other devices while the notebook can still say who they are: what they send now waits sealed,
            // unanswered, and their marks say this PC is locked rather than waiting amber for an answer that cannot come.
            if(!wasOffline)Post.sayLocked(context,store,keys,true);
            Runnable ask=()->{
                try {
                    // Hello is asked without a click only when this waited for Mininotes to be opened again: locked
                    // for want of use, nobody is there to answer it, and a question nobody sees is only in the way. The
                    // box then comes up without asking for the front too: the window it replaces has just closed.
                    if(DesktopLock.askAtStart(folder,away,opener(folder,wasOffline))==null)leave.run();
                } catch(Exception e){DesktopLock.tell("Mininotes could not open",e.getMessage());System.exit(1);}
            };
            shutdown(false,away?()->askLater=ask:ask);
        });
    }
    /** The question for the password, kept for when Mininotes is opened again after locking while put away. */
    private static volatile Runnable askLater;

    void shutdown(boolean exit){shutdown(exit,null);}
    void shutdown(boolean exit,Runnable after) {
        // This window's notebook no longer answers where a file is linked once it is closing.
        if(NoteStore.linkedHere==linkedHook)NoteStore.linkedHere=id->"";
        if(troubles!=null)troubles.stop();DesktopCamera.stop();
        // A recording going on is kept first: queued on the disk ahead of the notebook closing, and sealed with its key.
        if(recorder!=null)recorder.finish();
        // Whatever is private closes, and the last batch is written, as for anybody (decision 111).
        privately.stop();if(home!=null)home.stop();onDisk.stop();
        closing=true;if(current==this)current=null;
        // A newer version downloaded and waiting takes this one's place as Mininotes goes.
        if(exit)updateOnExit();if(idle!=null)idle.stop();Toolkit.getDefaultToolkit().removeAWTEventListener(using);autosave.stop();syncLater.stop();Object timer=frame.getRootPane().getClientProperty("health");if(timer instanceof javax.swing.Timer health)health.stop();
        frame.setEnabled(false);status.setText(Say.t("Closing…"));
        Node.everyBeat(null);
        disk.submit(()->{if(!exit){store.close();instanceLock.release();instanceChannel.close();}return null;},done->{
            if(tray!=null)SystemTray.getSystemTray().remove(tray);
            for(Window owned:frame.getOwnedWindows())owned.dispose();frame.dispose();disk.abandon();network.abandon();connectivity.abandon();
            context.unlock(null);
            if(exit)System.exit(0);
            if(after!=null)SwingUtilities.invokeLater(after);
        },e->{closing=false;frame.setEnabled(true);failed(e);});
    }
    void people(NoteStore.Branch target){people(target,false);}

    // ---- people in groups (decision 100) ----------------------------------------------------------------------------

    /**
     * A change to your groups, made, then what it gives or takes sent and the card to your other devices: said in the
     * status line from when it starts to how it ended, as sharing is.
     */
    void groupWork(String going,Background.Work<List<Groups.Changed>> change,String done,Runnable after) {
        status.setToolTipText(null);status.setText(Say.t(going));
        disk.submit(change,changed->{
            if(after!=null)after.run();
            network.submit(()->Post.groupsChanged(context,store,keys,changed),sent->{
                status.setText(sent.failed>0?Say.t("{0}. {1} could not be sent yet: they go when they can",Say.t(done),sent.failed):Say.t(done));refresh();
            },this::failed);
        },this::failed);
    }
    /** People and devices as they are open now, each drawn again when something arrives that it shows: see DesktopPeople. */
    final List<DesktopPeople> peopleBoxes=new ArrayList<>();
    private void peopleAgain(){for(DesktopPeople one:new ArrayList<>(peopleBoxes))if(one.showing())one.draw();}
    /**
     * People and devices, or, on a thing, Share with: one box whose levels are drawn in turn, with a way back from each
     * (the owner, 2026-10-05; decision 103; see DesktopPeople).
     *
     * @param codeFirst Show my code… ready to press when the box opens, for linking with somebody already listed
     */
    void people(NoteStore.Branch target,boolean codeFirst) {
        if(target==null)DesktopPeople.open(this);else DesktopPeople.with(this,target,codeFirst,null,null,null);
    }
    void setTrayListening(boolean enabled,javax.swing.text.JTextComponent outcome) {
        if(enabled&&!ensureTray()){outcome.setText(Say.t("Could not add Mininotes to the system tray."));return;}
        disk.submit(()->{context.getSharedPreferences("settings",0).edit().putString("listenInTray",Boolean.toString(enabled)).apply();return null;},done->{listenInTray=enabled;outcome.setText(Say.t(enabled?"Closing the window keeps Mininotes listening in the tray.":"Closing the window exits Mininotes."));if(!enabled&&tray!=null){SystemTray.getSystemTray().remove(tray);tray=null;}},this::failed);
    }
    private boolean ensureTray() {
        if(offline||!SystemTray.isSupported())return false;if(tray!=null)return true;
        BufferedImage icon=DesktopIcon.image(32);
        PopupMenu menu=new PopupMenu();MenuItem open=new MenuItem(Say.t("Open Mininotes")),quit=new MenuItem(Say.t("Exit Mininotes"));menu.add(open);menu.add(quit);
        Runnable reveal=()->{frame.setVisible(true);Node.near(true);frame.setState(Frame.NORMAL);frame.toFront();};open.addActionListener(e->SwingUtilities.invokeLater(reveal));quit.addActionListener(e->SwingUtilities.invokeLater(()->save(()->shutdown(true))));
        tray=new TrayIcon(icon,Say.t("Mininotes, listening"),menu);tray.setImageAutoSize(true);tray.addActionListener(e->SwingUtilities.invokeLater(reveal));
        try{SystemTray.getSystemTray().add(tray);return true;}catch(AWTException e){tray=null;return false;}
    }
    /** Connected, connecting or not: a coloured dot says which before the words are read. */
    static final class Dot extends JLabel {
        Dot(String text){super();setText(text);}
        @Override public void setText(String text) {
            // The colour from the English, the words in the language chosen.
            super.setText(text==null?null:Say.t(text));
            Color colour=text==null?QUIET:text.startsWith("Connected")?new Color(64,145,94):text.startsWith("Connecting")||text.contains("retrying")?new Color(207,150,48):new Color(168,165,156);
            setIcon(new Icon(){
                public int getIconWidth(){return 8;}public int getIconHeight(){return 8;}
                public void paintIcon(Component c,Graphics g0,int x,int y){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(colour);g.fillOval(x,y,8,8);g.dispose();}
            });
        }
    }
    static final class Paper extends JTextArea {
        static final Color RULES=new Color(222,226,215);
        /** The ruled lines: washed in the note's colour, as the phone washes them. */
        Color rules=RULES;
        /** Whether the page is ruled: a note can be plain paper on this PC (its menu, Writing lines). */
        boolean lined=true;
        /** Told when an address could not be opened: nothing on this PC took it. */
        java.util.function.Consumer<Exception> cannotOpen=e->{};
        /** The web and mail addresses in the words (see Links), found again when the words change. */
        private volatile List<Links.Link> links;
        List<Links.Link> links(){List<Links.Link> now=links;if(now==null)links=now=Links.find(getText());return now;}

        /**
         * A click on an address opens it, as a tap does on the phone; a click anywhere else only puts the caret there,
         * and a drag selects, over an address or not - so it opens only on a press and a release in the same place.
         */
        Paper() {
            DocumentListener changed=new DocumentListener(){
                public void insertUpdate(DocumentEvent e){links=null;edited(e.getOffset(),0,e.getLength());}
                public void removeUpdate(DocumentEvent e){links=null;edited(e.getOffset(),e.getLength(),0);}
                public void changedUpdate(DocumentEvent e){}
            };
            getDocument().addDocumentListener(changed);
            addPropertyChangeListener("document",e->{
                if(e.getOldValue() instanceof javax.swing.text.Document was)was.removeDocumentListener(changed);
                if(e.getNewValue() instanceof javax.swing.text.Document now)now.addDocumentListener(changed);
                links=null;
            });
            MouseAdapter pointer=new MouseAdapter(){
                java.awt.Point pressed;Links.Link on;
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(linkAt(e.getPoint())!=null?Cursor.HAND_CURSOR:Cursor.TEXT_CURSOR));
                }
                @Override public void mousePressed(MouseEvent e){pressed=e.getPoint();on=SwingUtilities.isLeftMouseButton(e)&&!e.isShiftDown()?linkAt(pressed):null;}
                @Override public void mouseReleased(MouseEvent e) {
                    Links.Link was=on;on=null;
                    if(was==null||pressed==null||pressed.distance(e.getPoint())>4||!was.equals(linkAt(e.getPoint())))return;
                    open(was.target());
                }
            };
            addMouseListener(pointer);addMouseMotionListener(pointer);
            // The selection drawn by the highlighter itself, under the words: the page's own line view (FlushMarks) does
            // not draw Swing's layered highlights, so a selection made with the mouse or Ctrl+A was there but never seen
            // (the owner, 2026-10-11: "I can't select text in the note").
            if(getHighlighter() instanceof javax.swing.text.DefaultHighlighter marks)marks.setDrawsLayeredHighlights(false);
            // An underline's marks show only while the cursor is in its stretch (see paintMarks): drawn again as it moves.
            addCaretListener(e->{if(getDocument().getLength()>0&&getText().contains("__"))repaint();});
        }

        // ---- who wrote what (see Writers) ------------------------------------------------------------------------

        /** Who wrote each letter on the page: always as long as the page's text. */
        private Writers runs=Writers.none();
        /** Who wrote what just before the last edit, for Ctrl+Z to put back with the letters (see remembered). */
        private Writers before;
        private Writers.Palette palette=Writers.Palette.plain();
        /** "Show who wrote what"; and whether the page is drawn in its writers' colours now: wanted, and two or more wrote. */
        private boolean wanted=true, inked;
        /** Text being put on the page with its writers, rather than typed. */
        private boolean putting;

        /** An edit, typed or pasted or cut: the new letters are yours, and the rest keep their writers. */
        private void edited(int at,int removed,int inserted) {
            if(putting||runs==null)return;
            before=runs.copy();
            runs.edit(at,removed,inserted,Writers.ME);
            // Never drawn from runs that do not fit the words: nobody's, rather than a colour on the wrong letters.
            if(runs.length()!=getDocument().getLength())runs=Writers.unknown(getDocument().getLength());
            boolean now=wanted&&palette.shows(runs);
            if(now!=inked){inked=now;repaint();}
        }

        /** Words put on the page as they are, with who wrote them: a note opened, or one that changed underneath it. */
        void put(String text,Writers who) {
            putting=true;
            try{setText(text);}finally{putting=false;}
            String on=getText(),given=text==null?"":text;
            runs=who!=null&&who.length()==on.length()&&given.equals(on)?who.copy():Writers.follow(on,Writers.UNKNOWN,given,who);
            before=null;ink();
        }

        /** Who wrote the words a note said, read once the page was showing them: anything typed since stays yours. */
        void follow(String said,Writers who) {
            if(who==null)return;
            String on=getText();
            runs=on.equals(said)&&who.length()==on.length()?who.copy():Writers.follow(on,Writers.ME,said,who,on,runs);
            ink();
        }

        /** Words that came back from an old version: what is still on the page keeps its writer, the rest is nobody's. */
        void putBack(String was,Writers had) {
            runs=Writers.follow(getText(),Writers.UNKNOWN,was,had);
            ink();
        }

        /** Who wrote what on the page now, to be written down with its words. */
        Writers writers(){return runs.copy();}

        /** The colours writers are drawn in, and whether they are. */
        void inks(Writers.Palette palette,boolean wanted){this.palette=palette==null?Writers.Palette.plain():palette;this.wanted=wanted;ink();}

        /** Whether the page is drawn in its writers' colours now. */
        boolean inked(){return inked;}

        private void ink(){inked=wanted&&palette.shows(runs);repaint();}

        /**
         * An edit Ctrl+Z can take back, with who wrote the letters: taken back, they are whoever wrote them before,
         * not letters of yours typed again; made again, they are what they were made as.
         */
        javax.swing.undo.UndoableEdit remembered(javax.swing.undo.UndoableEdit edit) {
            Writers was=before, after=runs.copy();
            return new javax.swing.undo.AbstractUndoableEdit() {
                @Override public void undo(){super.undo();edit.undo();back(was);}
                @Override public void redo(){super.redo();edit.redo();back(after);}
                @Override public boolean isSignificant(){return edit.isSignificant();}
                @Override public String getPresentationName(){return edit.getPresentationName();}
                @Override public void die(){super.die();edit.die();}
            };
        }
        private void back(Writers kept) {
            if(kept!=null&&kept.length()==getDocument().getLength())runs=kept.copy();
            ink();
        }

        /** The page's own look, with a caret that can be told to leave the view where it is (see holding). */
        @Override public void updateUI(){setUI(new Inked());}

        /**
         * While the page is being put back where the reader was, the caret does not scroll it: moving the caret brings it
         * into view a moment later, and a caret left at the end of a note took the page to the bottom at every arrival.
         */
        private boolean holding;
        void holding(boolean now){holding=now;}

        private final class Inked extends com.formdev.flatlaf.ui.FlatTextAreaUI {
            @Override protected javax.swing.text.Caret createCaret() {
                return new com.formdev.flatlaf.ui.FlatCaret(null,false) {
                    @Override protected void adjustVisibility(Rectangle place){if(!holding)super.adjustVisibility(place);}
                };
            }
            /** The box Java lays the page's lines out in and draws them in, for the styles and addresses drawn again over them. */
            Rectangle laidOut(){return getVisibleEditorRect();}
            /** Underline marks take no room (decision 118): this view lays the lines out, the plain one gives every letter a width. */
            @Override public javax.swing.text.View create(javax.swing.text.Element elem) {
                if(getComponent() instanceof JTextArea area&&area.getLineWrap()&&elem.getParentElement()==null)
                    return new FlushMarks(elem,area.getWrapStyleWord());
                return super.create(elem);
            }
        }

        /**
         * The box the page's lines are laid out in, exactly as Java draws them: its own size less the border and less the
         * room it keeps for the caret at the right. Bold, italic, addresses and writers are drawn again from the same line
         * view, and given a box one caret wider, the view laid every line out again at that width: where a line then fitted
         * one word more, every line under it moved up a row, and another line's words came out in a bold label's place (the
         * owner, 2026-10-22, the French README: "Vérifier" again over its own line, "L'horodatage" over "La différence",
         * bold labels cut; decision 234).
         */
        private Rectangle laidOut() {
            Rectangle box=getUI() instanceof Inked own?own.laidOut():null;
            if(box!=null)return box;
            Insets in=getInsets();
            return new Rectangle(in.left,in.top,Math.max(0,getWidth()-in.left-in.right),Math.max(0,getHeight()-in.top-in.bottom));
        }

        /** The address under a point on the page, or null: over its letters, not merely the nearest place to them. */
        Links.Link linkAt(java.awt.Point p) {
            List<Links.Link> all=links();
            if(all.isEmpty())return null;
            try {
                int near=viewToModel2D(p);
                for(int at=Math.max(0,near-1);at<=near&&at<getDocument().getLength();at++) {
                    Links.Link one=Links.at(all,at);if(one==null)continue;
                    java.awt.geom.Rectangle2D from=modelToView2D(at),to=modelToView2D(at+1);
                    if(from==null||to==null)continue;
                    double right=to.getY()==from.getY()?to.getX():getWidth();
                    if(p.x>=from.getX()&&p.x<right&&p.y>=from.getY()&&p.y<from.getY()+from.getHeight())return one;
                }
            } catch(Exception notLaidOut){/* none, then */}
            return null;
        }

        private void open(String target) {
            try {
                java.net.URI where=new java.net.URI(target);
                if("mailto".equalsIgnoreCase(where.getScheme()))java.awt.Desktop.getDesktop().mail(where);
                else java.awt.Desktop.getDesktop().browse(where);
            } catch(Exception e) {
                cannotOpen.accept(new IllegalStateException(target.startsWith("mailto:")
                    ?"No mail app on this PC offered to write to that address.":"Nothing on this PC could open that address."));
            }
        }

        protected void paintComponent(Graphics original) {
            Graphics2D g=(Graphics2D)original.create();g.setColor(getBackground());g.fillRect(0,0,getWidth(),getHeight());
            int height=getFontMetrics(getFont()).getHeight();g.setColor(rules);
            if(lined)for(int y=getInsets().top+height;y<getHeight();y+=height)g.drawLine(28,y,getWidth()-22,y);
            g.setColor(new Color(221,192,179));g.drawLine(30,0,30,getHeight());g.dispose();
            setOpaque(false);super.paintComponent(original);
            // Who wrote what is drawn by the page's own line view, each letter once in its writer's colour (see inkAt):
            // drawn again over the plain letters inside a tight box, a letter's edges that reached past it were cut off
            // and the rest looked soft (the owner, 2026-10-12: "with a colour like blue it is not sharp, parts of the
            // letters are missing").
            paintLinks(original);
            paintMarks(original);
            ColourEmoji colour=ColourEmoji.get();if(colour!=null)colour.paint(this,original);
        }

        /** While an address is drawn again: the letters come out in the accent colour. See paintLinks. */
        private boolean inking;
        /** A stretch of the page in one writer's colour: the colour, null for the page's own ink, and where it stops. */
        record Ink(Color colour,int end){}
        /**
         * The colour the letter at this place is drawn in, and how far that goes: its writer's, while the page is drawn in
         * its writers' colours; the page's own ink inside a selection, as a selection is always drawn, and while the page
         * is drawn again in one colour (an address, a writer).
         */
        Ink inkAt(int at) {
            Writers who=runs;int length=getDocument().getLength();
            if(!inked||inking||inkNow!=null||who==null||who.length()!=length||at>=length)return new Ink(null,Integer.MAX_VALUE);
            int chosenFrom=Math.min(getSelectionStart(),getSelectionEnd()),chosenTo=Math.max(getSelectionStart(),getSelectionEnd());
            if(chosenTo>chosenFrom&&at>=chosenFrom&&at<chosenTo)return new Ink(null,chosenTo);
            int start=0;
            for(int run=0;run<who.count();run++) {
                int end=start+who.lengthOf(run);
                if(at<end) {
                    int stop=chosenTo>chosenFrom&&chosenFrom>at?Math.min(end,chosenFrom):end;
                    int colour=palette.colourOf(who.writerOf(run));
                    return new Ink(Tint.known(colour)?new Color(Writers.ink(colour,getBackground().getRGB())):null,stop);
                }
                start=end;
            }
            return new Ink(null,Integer.MAX_VALUE);
        }
        /** While a writer's letters are drawn again: their colour. See paintWriters. */
        private Color inkNow;
        @Override public Color getForeground(){return inking?ACCENT:inkNow!=null?inkNow:super.getForeground();}

        /**
         * Each writer's letters in view drawn again over the plain ones Java drew, in the writer's colour, the way
         * addresses are (see paintLinks): the place filled with the paper, the page's own text drawn into that place alone
         * with the colour as its colour - so the letters land exactly where Java put them, whatever view it chose for the
         * words - then the ruled line put back. Only what the paint was asked for, so typing redraws a line, not the page.
         * Selected words are left as any selection is drawn, and the caret is drawn again on top.
         */
        private void paintWriters(Graphics original) {
            Writers who=runs;
            if(!inked||who==null||who.length()!=getDocument().getLength()||who.length()==0)return;
            Rectangle view=getVisibleRect(),asked=original.getClipBounds(),area=asked==null?view:view.intersection(asked);
            if(area.isEmpty())return;
            String text=getText();int from,to;
            try {
                from=Math.max(0,viewToModel2D(new java.awt.Point(0,area.y))-1);
                to=Math.min(text.length(),viewToModel2D(new java.awt.Point(getWidth(),area.y+area.height))+1);
            } catch(RuntimeException notLaidOut){return;}
            Graphics2D g=(Graphics2D)original.create();
            try {
                Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
                if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
                FontMetrics metrics=getFontMetrics(getFont());
                int chosenFrom=getSelectionStart(),chosenTo=getSelectionEnd(),paper=getBackground().getRGB(),start=0;
                Insets in=getInsets();
                Rectangle editor=laidOut();
                for(int run=0;run<who.count()&&start<to;run++) {
                    int end=start+who.lengthOf(run),at=Math.max(start,from),stop=Math.min(end,to);
                    int colour=palette.colourOf(who.writerOf(run));
                    start=end;
                    if(stop<=at||!Tint.known(colour))continue;
                    Color ink=new Color(Writers.ink(colour,paper));
                    while(at<stop) {
                        // One row's worth: words wrapped onto the next row are drawn there as a piece of their own.
                        java.awt.geom.Rectangle2D first=modelToView2D(at);int past=at+1;
                        java.awt.geom.Rectangle2D next=past<text.length()?modelToView2D(past):null;
                        while(past<stop&&next!=null&&next.getY()==first.getY()){past++;next=past<text.length()?modelToView2D(past):null;}
                        if(first==null||chosenTo>chosenFrom&&at<chosenTo&&past>chosenFrom){at=past;continue;}
                        float x=(float)first.getX(),top=(float)first.getY(),high=(float)first.getHeight();
                        float right=next!=null&&next.getY()==first.getY()?(float)next.getX():x+metrics.stringWidth(text.substring(at,past).replace("\n",""));
                        if(right>x) {
                            java.awt.geom.Rectangle2D.Float place=new java.awt.geom.Rectangle2D.Float(x,top,right-x,high);
                            g.setColor(getBackground());g.fill(place);
                            Graphics2D inside=(Graphics2D)g.create();inside.clip(place);inkNow=ink;
                            try{getUI().getRootView(this).paint(inside,editor);}finally{inkNow=null;inside.dispose();}
                            // The ruled line the paper covered, put back.
                            g.setColor(rules);
                            if(lined)for(int y=in.top+metrics.getHeight();y<getHeight();y+=metrics.getHeight())
                                if(y>=top&&y<top+high)g.drawLine(Math.round(x),y,Math.round(right),y);
                        }
                        at=past;
                    }
                }
                if(getCaret()!=null&&getCaret().isVisible())getCaret().paint(g);
            } catch(Exception notNow){/* plain letters, then */}
            finally{g.dispose();}
        }

        /**
         * Each address in view drawn again over the plain letters Java drew, in the accent colour and underlined, as
         * the phone draws them: its place filled with the paper, then the page's own text drawn into that place alone
         * with the accent as its colour - so the letters land exactly where Java put them, whatever it measured -
         * then the ruled line put back. Nothing is set on the page while it paints, which would ask for another paint.
         * Selected words are left as any selection is drawn, and the caret is drawn again on top.
         */
        /**
         * A style switched on what is chosen (see Marks.toggle), as typing would: only the marks put in or taken out, through
         * the page's own document, so who wrote the words is kept. Nothing on a page only read.
         */
        void style(Marks.Style style) {
            if(!isEditable())return;
            int from=Math.min(getSelectionStart(),getSelectionEnd()),to=Math.max(getSelectionStart(),getSelectionEnd());
            Marks.Changed now=Marks.toggle(getText(),from,to,style);
            String mark=style.mark;int width=mark.length();
            try {
                javax.swing.text.Document document=getDocument();
                if(now.text().length()<getDocument().getLength()){document.remove(to,width);document.remove(from-width,width);}
                else{int start=now.start()-width,end=now.end()-width;document.insertString(end,mark,null);document.insertString(start,mark,null);}
                select(now.start(),now.end());
            } catch(javax.swing.text.BadLocationException outside){/* the words moved under it: nothing done */}
            repaint();
        }

        /**
         * Bold, italic and underline drawn over the page (decision 76): a text area draws one font, so each styled stretch is
         * drawn again where it is - bold struck twice a hair apart, italic slanted in its own place, underline a line under
         * it - and its marks, which take no room, not drawn at all. Line by line, as the links are.
         */
        private void paintMarks(Graphics original) {
            String text=getText();
            List<Marks.Run> runs=Marks.of(text);
            if(runs.isEmpty())return;
            Rectangle view=getVisibleRect();int from,to;
            try {
                from=Math.max(0,viewToModel2D(new java.awt.Point(view.x,view.y))-1);
                to=Math.min(text.length(),viewToModel2D(new java.awt.Point(view.x+view.width,view.y+view.height))+1);
            } catch(RuntimeException notLaidOut){return;}
            Graphics2D g=(Graphics2D)original.create();
            try {
                Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
                if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
                g.setFont(getFont());FontMetrics metrics=getFontMetrics(getFont());
                Rectangle editor=laidOut();
                for(Marks.Run run:runs) {
                    if(run.close()<=from||run.open()>=to)continue;
                    for(int[] part:new int[][]{{run.open(),run.start(),0},{run.start(),run.end(),1},{run.end(),run.close(),0}}) {
                        int at=part[0],end=part[1];
                        while(at<end) {
                            java.awt.geom.Rectangle2D first=modelToView2D(at);int past=at+1;
                            java.awt.geom.Rectangle2D next=past<text.length()?modelToView2D(past):null;
                            while(past<end&&next!=null&&next.getY()==first.getY()){past++;next=past<text.length()?modelToView2D(past):null;}
                            String piece=text.substring(at,past);
                            float x=(float)first.getX(),top=(float)first.getY(),high=(float)first.getHeight();
                            float right=next!=null&&next.getY()==first.getY()?(float)next.getX():x+metrics.stringWidth(piece);
                            float baseline=top+high-metrics.getDescent()-metrics.getLeading();
                            java.awt.geom.Rectangle2D.Float place=new java.awt.geom.Rectangle2D.Float(x,top,right-x,high);
                            if(part[2]==0) {
                                // The marks take no room and are not drawn, whatever the style (decisions 118 and 144, see
                                // FlushMarks): the owner, 2026-10-10, saw ** before and after every bold word.
                                at=past;continue;
                            } else if(run.style()==Marks.Style.UNDERLINE) {
                                g.setColor(getForeground());g.fill(new java.awt.geom.Rectangle2D.Float(x,baseline+2f,right-x,1.2f));
                            } else if(run.style()==Marks.Style.BOLD) {
                                Graphics2D inside=(Graphics2D)g.create();inside.clip(place);inside.translate(0.7,0);
                                try{getUI().getRootView(this).paint(inside,editor);}finally{inside.dispose();}
                            } else {
                                Graphics2D inside=(Graphics2D)g.create();
                                java.awt.geom.Rectangle2D.Float wider=new java.awt.geom.Rectangle2D.Float(x,top,right-x+3f,high);
                                inside.setColor(getBackground());inside.fill(place);inside.clip(wider);
                                inside.translate(x,baseline);inside.shear(-0.18,0);inside.translate(-x,-baseline);
                                try{getUI().getRootView(this).paint(inside,editor);}finally{inside.dispose();}
                            }
                            at=past;
                        }
                    }
                }
            } catch(javax.swing.text.BadLocationException outside){/* drawn as plain */}
            finally{g.dispose();}
        }

        private void paintLinks(Graphics original) {
            List<Links.Link> all=links();
            if(all.isEmpty())return;
            String text=getText();Rectangle view=getVisibleRect();int from,to;
            try {
                from=Math.max(0,viewToModel2D(new java.awt.Point(view.x,view.y))-1);
                to=Math.min(text.length(),viewToModel2D(new java.awt.Point(view.x+view.width,view.y+view.height))+1);
            } catch(RuntimeException notLaidOut){return;}
            Graphics2D g=(Graphics2D)original.create();
            try {
                Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
                if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
                // The page's own measure of the font, which is where Java put the letters it drew.
                g.setFont(getFont());FontMetrics metrics=getFontMetrics(getFont());
                int chosenFrom=getSelectionStart(),chosenTo=getSelectionEnd();
                Rectangle editor=laidOut();
                for(Links.Link one:all) {
                    if(one.end()<=from||one.start()>=to)continue;
                    int at=Math.max(one.start(),from),end=Math.min(one.end(),to);
                    while(at<end) {
                        // One line's worth: an address wrapped onto the next line is drawn there as a second run.
                        java.awt.geom.Rectangle2D first=modelToView2D(at);int past=at+1;
                        java.awt.geom.Rectangle2D next=past<text.length()?modelToView2D(past):null;
                        while(past<end&&next!=null&&next.getY()==first.getY()){past++;next=past<text.length()?modelToView2D(past):null;}
                        String run=text.substring(at,past);
                        if(chosenTo>chosenFrom&&at<chosenTo&&past>chosenFrom){at=past;continue;}
                        float x=(float)first.getX(),top=(float)first.getY(),high=(float)first.getHeight();
                        float right=next!=null&&next.getY()==first.getY()?(float)next.getX():x+metrics.stringWidth(run);
                        java.awt.geom.Rectangle2D.Float place=new java.awt.geom.Rectangle2D.Float(x,top,right-x,high);
                        g.setColor(getBackground());g.fill(place);
                        Graphics2D inside=(Graphics2D)g.create();inside.clip(place);inking=true;
                        try{getUI().getRootView(this).paint(inside,editor);}finally{inking=false;inside.dispose();}
                        // Just under the letters, inside the line's own descent.
                        float baseline=top+high-metrics.getDescent()-metrics.getLeading();
                        g.setColor(ACCENT);g.fill(new java.awt.geom.Rectangle2D.Float(x,baseline+2f,right-x,1f));
                        // The ruled line the paper covered, put back.
                        g.setColor(rules);
                        if(lined)for(int y=getInsets().top+metrics.getHeight();y<getHeight();y+=metrics.getHeight())
                            if(y>=top&&y<top+high)g.drawLine(Math.round(x),y,Math.round(right),y);
                        at=past;
                    }
                }
                if(getCaret()!=null&&getCaret().isVisible())getCaret().paint(g);
            } catch(Exception notNow){/* plain letters, then */}
            finally{g.dispose();}
        }
    }
}

