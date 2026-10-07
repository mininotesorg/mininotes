package org.mininotes.android;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Private notes and folders on the PC (the owner, 2026-10-06; decision 111): the codes caught as they are typed, the
 * screen of their own, and the rhythm that writes the private file the same for everybody (see {@link Slots}).
 *
 * <p>The screen takes the window's place while it is up, so nothing of it is ever among Home, the tree, the open list, the
 * overview, search, Recent or the tray; it goes, and the window is as it was, on ‹ or Esc at its top, when the window is
 * minimised or hidden, when Mininotes closes, and after five minutes with no key or click. While it is up Windows is asked to leave the
 * window out of screenshots and the taskbar's pictures (SetWindowDisplayAffinity), the PC's FLAG_SECURE. Nothing about
 * it is ever written to the log: whatever fails in here is let go without a word. The PC has no keyboard that learns, so
 * there is nothing to tell one.
 */
final class DesktopPrivate {
    // Pictures read and made in memory, never through ImageIO's cache files in the temporary folder: for everybody, so that
    // nobody's temporary folder says anything (decision 111).
    static {ImageIO.setUseCache(false);}
    /** Closed after five minutes with no key or click on it. */
    static final long IDLE=5*60_000L;
    /** A picture is made about two megapixels, its longest side this, JPEG at this quality, and nothing of its camera kept. */
    static final int LONGEST=Shrink.LONGEST;static final float QUALITY=Shrink.QUALITY/100f;

    private final Desktop d;
    private volatile Slots slots;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mininotes-pages");t.setDaemon(true);return t;});
    private final javax.swing.Timer rhythm,watch;
    private final AWTEventListener use=e->{if(DesktopPrivate.this.screen!=null)lastUse=System.currentTimeMillis();};
    private final AWTEventListener windows=e->{if(e.getID()==WindowEvent.WINDOW_OPENED&&DesktopPrivate.this.screen!=null&&e.getSource() instanceof Window w)secure(w,true);};

    /** What is open, each as it was handed over once; and the one on the screen. */
    private final Map<Slots.Space,PrivateSpace> open=new IdentityHashMap<>();
    private PrivateSpace shown;
    private JPanel screen;private Container before;
    private String folder="",note;private boolean inBin,full;
    private long lastUse;
    /** What was copied from in here, cleared from the clipboard when it closes if it is still there. */
    private String copied;

    DesktopPrivate(Desktop d) {
        this.d=d;
        rhythm=new javax.swing.Timer((int)Slots.EVERY,e->batch());
        watch=new javax.swing.Timer(2000,e->tick());
        Toolkit.getDefaultToolkit().addAWTEventListener(use,AWTEvent.KEY_EVENT_MASK|AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(windows,AWTEvent.WINDOW_EVENT_MASK);
    }

    // ---- the rhythm, the same for everybody -------------------------------------------------------------------------

    /** At every start: the file made or kept, a batch, and one every minute after (see {@link Slots}). */
    void start(Path folder) {
        File file=folder.resolve(Slots.NAME).toFile();
        worker.submit(()->{
            try {
                byte[] secret=d.keys.agreement().getPrivate().getEncoded();
                Slots made=new Slots(file,Slots.maskFrom(secret),Integer.getInteger("mininotes.pages",Slots.COUNT),Slots.ROUNDS);
                Arrays.fill(secret,(byte)0);
                made.start();slots=made;
            } catch(Throwable quietly){/* nothing about it is said or kept anywhere */}
        });
        rhythm.start();
    }
    private void batch(){worker.submit(()->{try{Slots s=slots;if(s!=null)s.batch();}catch(Throwable quietly){/* the next one */}});}
    /** A batch now, waited for: when the window is left, and for the tests that hold the rhythm the same. */
    void batchNow(){try{worker.submit(()->{Slots s=slots;if(s!=null)s.batch();return null;}).get(30,TimeUnit.SECONDS);}catch(Exception quietly){/* as above */}}
    /** The file, once it is ready: for the backup, and for the tests. */
    Slots slots(){try{worker.submit(()->null).get(30,TimeUnit.SECONDS);}catch(Exception waiting){/* as it is */}return slots;}

    /** The window minimised or hidden: everything closes, and one batch, as for anybody leaving it. */
    void left(){close();batch();}

    /** Mininotes closing: everything closed, the last batch written. */
    void stop() {
        close();rhythm.stop();watch.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(use);Toolkit.getDefaultToolkit().removeAWTEventListener(windows);
        batchNow();worker.shutdown();
    }

    boolean showing(){return screen!=null;}
    /** For the tests: the password field while it is up, what is on the screen, and the rhythm held so batches can be counted. */
    JPasswordField asking;
    PrivateSpace shown(){return shown;}
    void holdRhythm(){rhythm.stop();}
    /** For the tests: everything the worker was given, done. */
    void settle(){try{worker.submit(()->null).get(60,TimeUnit.SECONDS);}catch(Exception waiting){/* as it is */}}

    // ---- the codes ----------------------------------------------------------------------------------------------------

    /**
     * A field where a code can be typed: a note's page and title, the search. The code is caught by the field's own filter,
     * before the words change anywhere else: taken out at once, and what is typed next goes into a password field of its
     * own. {@code typing} says whether a change is typed (not a note being drawn); {@code forget} empties its undo.
     */
    void watch(JTextComponent field,BooleanSupplier typing,Runnable forget) {
        ((AbstractDocument)field.getDocument()).setDocumentFilter(new Catch(field,typing,forget));
    }

    /** The field a code is being typed into, between its colon and the password field taking the keyboard. */
    private Catch catching;private final StringBuilder caught=new StringBuilder();private PrivateCode.Kind kind;

    private final class Catch extends DocumentFilter {
        final JTextComponent field;final BooleanSupplier typing;final Runnable forget;
        Catch(JTextComponent field,BooleanSupplier typing,Runnable forget){this.field=field;this.typing=typing;this.forget=forget;}
        // While the password field comes up, what is typed is kept for it; a word handed over again whole, code and all, gives
        // only what follows the code, as on the phone (decision 114).
        @Override public void insertString(FilterBypass fb,int at,String s,AttributeSet a) throws BadLocationException {
            if(catching==this){PrivateCode.more(caught,s);return;}
            fb.insertString(at,s,a);caught(fb,at,s);
        }
        @Override public void replace(FilterBypass fb,int at,int length,String s,AttributeSet a) throws BadLocationException {
            if(catching==this){PrivateCode.more(caught,s);return;}
            fb.replace(at,length,s,a);caught(fb,at,s);
        }
        @Override public void remove(FilterBypass fb,int at,int length) throws BadLocationException {
            if(catching==this){if(caught.length()>0)caught.setLength(caught.length()-1);return;}
            fb.remove(at,length);
        }
        /**
         * A key, a word handed over whole, or a short paste that finished a code: the code taken out, and with it what came
         * after its colon in the same change, which is the password, kept for its field (decision 114: a pasted code left
         * its password in the note).
         */
        private void caught(FilterBypass fb,int at,String s) throws BadLocationException {
            if(s==null||s.isEmpty()||s.length()>48||s.indexOf(':')<0||!typing.getAsBoolean())return;
            Document doc=fb.getDocument();
            PrivateCode.Caught found=PrivateCode.finished(doc.getText(0,doc.getLength()),at,at+s.length());
            if(found==null)return;
            int from=found.start;
            fb.remove(from,found.stop-from);
            catching=this;caught.setLength(0);caught.append(found.rest);if(found.entered)caught.append('\n');kind=found.kind;
            SwingUtilities.invokeLater(()->{forget.run();ask(field,from);});
        }
    }

    /** The password, in a field of its own under where the code was: Enter opens, Esc or a click elsewhere lets it go. */
    private void ask(JTextComponent from,int at) {
        PrivateCode.Kind asked=kind;
        String already=caught.toString();caught.setLength(0);catching=null;
        // Typed faster than the field could come: the whole of it, Enter too, is here already.
        int enter=already.indexOf('\n');
        if(enter>=0){go(asked,already.substring(0,enter).toCharArray());return;}
        java.awt.Point p=null;
        try {
            java.awt.geom.Rectangle2D place=from.modelToView2D(Math.min(at,from.getDocument().getLength()));
            p=new java.awt.Point((int)place.getX(),(int)(place.getY()+place.getHeight()+4));SwingUtilities.convertPointToScreen(p,from);
        } catch(Exception notLaidOut){/* in the middle, then */}
        password("Password, then Enter",already,p,(said,box)->go(asked,said,box),()->{});
    }

    /**
     * The box the password is typed in, kept up from the first key until it is decided: Opening… while the password is
     * tried, and The same again in the same place when a new space is to be made. It used to close at Enter and come back
     * a moment later, and keys typed in that moment went to the note (found on the phone, 2026-10-06; the same here).
     */
    interface Box {
        void waiting(String words);
        void again(String hint,java.util.function.BiConsumer<char[],Box> enter);
        void close();
    }

    /** A password field of its own, for a moment: Enter hands over what was typed; Esc or a click elsewhere lets it go. */
    private void password(String hint,String already,java.awt.Point at,java.util.function.BiConsumer<char[],Box> enter,Runnable away) {
        JDialog box=new JDialog(d.frame);box.setUndecorated(true);box.setType(Window.Type.POPUP);
        JPasswordField field=new JPasswordField(already,22);asking=field;
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,hint);
        field.putClientProperty(FlatClientProperties.STYLE,"margin:8,10,8,10");
        field.getAccessibleContext().setAccessibleName(hint);
        JPanel round=new JPanel(new BorderLayout());round.setBackground(DesktopUi.CARD);
        round.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopUi.LINE.darker()),BorderFactory.createEmptyBorder(6,6,6,6)));
        // An eye at its right that shows the characters while it is held (decision 116).
        round.add(field);round.add(eye(field),BorderLayout.EAST);box.setContentPane(round);
        boolean[] done={false};
        Runnable gone=()->{if(done[0])return;done[0]=true;if(asking==field)asking=null;char[] wipe=field.getPassword();Arrays.fill(wipe,'\0');field.setText("");box.dispose();away.run();};
        @SuppressWarnings("unchecked") java.util.function.BiConsumer<char[],Box>[] next=new java.util.function.BiConsumer[]{enter};
        Box[] self={null};
        self[0]=new Box(){
            // No blinking caret while it says Opening, so it does not look as if it wants typing (decision 116): not editable, and the caret hidden.
            public void waiting(String words){field.setText("");field.setEditable(false);field.getCaret().setVisible(false);field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,words);field.repaint();}
            public void again(String asked,java.util.function.BiConsumer<char[],Box> then){next[0]=then;field.setText("");field.setEditable(true);field.getCaret().setVisible(true);
                field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,asked);field.getAccessibleContext().setAccessibleName(asked);field.requestFocusInWindow();field.repaint();}
            public void close(){if(done[0])return;done[0]=true;if(asking==field)asking=null;field.setText("");box.dispose();}
        };
        field.addActionListener(e->{if(done[0]||!field.isEditable())return;char[] said=field.getPassword();field.setText("");next[0].accept(said,self[0]);});
        field.registerKeyboardAction(e->gone.run(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_FOCUSED);
        box.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){gone.run();}});
        box.pack();
        if(at!=null)box.setLocation(at);else box.setLocationRelativeTo(d.frame);
        box.setVisible(true);field.requestFocusInWindow();
        secure(box,true);
    }

    /**
     * What a password opens, shown. Nothing answering: for privatespace//:, the password is asked once more, and only the
     * same twice makes a new, independent space (the owner, 2026-10-06; decision 116), so a typo makes nothing and writes
     * nothing; for private//:, nothing happens and nothing is said, as for any wrong password.
     */
    void go(PrivateCode.Kind kind,char[] password){go(kind,password,null);}
    void go(PrivateCode.Kind kind,char[] password,Box box) {
        if(password.length==0){if(box!=null)box.close();return;}
        if(box!=null)box.waiting("Opening…");
        worker.submit(()->{
            boolean keep=false;
            try {
                Slots s=slots;if(s==null)return;
                Slots.Space space=s.open(password);
                if(space==null) {
                    if(kind==PrivateCode.Kind.OPEN){if(box!=null)SwingUtilities.invokeLater(box::close);return;}
                    keep=true;
                    java.util.function.BiConsumer<char[],Box> then=(again,same)->{boolean match=Arrays.equals(again,password);Arrays.fill(again,'\0');same.close();
                        if(match)make(password);else Arrays.fill(password,'\0');};
                    SwingUtilities.invokeLater(()->{if(box!=null)box.again("The same again, then Enter",then);
                        else password("The same again, then Enter","",null,then,()->Arrays.fill(password,'\0'));});
                    return;
                }
                if(box!=null)SwingUtilities.invokeLater(box::close);
                showLater(space);
            } catch(Throwable quietly){/* nothing said: as a password nothing answers to */}
            finally{if(!keep)Arrays.fill(password,'\0');}
        });
    }
    /** A new, independent space from outside (privatespace//:, the password twice; decision 116): an empty folder with the + ready. */
    private void make(char[] password) {
        worker.submit(()->{
            try {
                Slots s=slots;if(s==null)return;
                Slots.Space space=s.make(password,PrivateSpace.empty());
                if(space!=null)showLater(space);
            } catch(Throwable quietly){/* nothing said */}
            finally{Arrays.fill(password,'\0');}
        });
    }
    private void showLater(Slots.Space got) {
        SwingUtilities.invokeLater(()->{
            try {
                PrivateSpace model=open.get(got);
                if(model==null){model=PrivateSpace.of(got);open.put(got,model);}
                show(model);
            } catch(Throwable quietly){/* nothing said */}
        });
    }

    // ---- the screen ---------------------------------------------------------------------------------------------------

    private DesktopUi.Text heading;private JButton back,plus,more;private JPanel middle;private CardLayout cards;
    private Tiles tiles,binTiles;private JLabel says;private JPanel binTop;
    private JPanel noteView;private JScrollPane noteScroll;
    private JTextField noteTitle;private Desktop.Paper notePage;private JPanel noteFiles;private boolean loading;
    private javax.swing.undo.UndoManager undo;
    private final javax.swing.Timer keep=new javax.swing.Timer(700,e->write());{keep.setRepeats(false);}

    /** A space always opens on its grid (folder mode), never a note, a brand-new one as an empty folder (decision 116). */
    private void show(PrivateSpace p) {
        if(screen==null)build();
        leaveNote();
        shown=p;inBin=false;folder="";note=null;full=false;
        lastUse=System.currentTimeMillis();
        draw();
    }

    private void build() {
        // Its own ground from edge to edge, and a lock beside Private, so it is never taken for Home (decision 114).
        screen=new JPanel(new BorderLayout());screen.setBackground(GROUND);
        JPanel top=new JPanel(new BorderLayout(12,0));top.setOpaque(false);top.setBorder(BorderFactory.createEmptyBorder(14,20,10,16));
        // Always there: ‹ goes up a level, and at the top it closes, as Esc does. No Close (the owner, 2026-10-06: "we
        // don't need the close, the back button should be enough"; decision 114).
        back=DesktopUi.quietButton("‹ Mininotes",this::escape);
        heading=DesktopUi.title("Private");heading.setOpaque(false);
        JLabel lock=new JLabel(new Lock(16,20));lock.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        JPanel named=new JPanel(new BorderLayout());named.setOpaque(false);named.add(lock,BorderLayout.WEST);named.add(heading);
        JPanel left=new JPanel();left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));left.setOpaque(false);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);named.setAlignmentX(Component.LEFT_ALIGNMENT);left.add(back);left.add(named);
        top.add(left,BorderLayout.CENTER);
        more=DesktopUi.button("⋯",()->moreMenu().show(more,0,more.getHeight()));more.setToolTipText("More");more.getAccessibleContext().setAccessibleName("More");
        JPanel middleRight=new JPanel(new GridBagLayout());middleRight.setOpaque(false);middleRight.add(DesktopUi.actions(more));
        top.add(middleRight,BorderLayout.EAST);
        screen.add(top,BorderLayout.NORTH);
        cards=new CardLayout();middle=new JPanel(cards);middle.setOpaque(false);
        tiles=new Tiles();
        JScrollPane grid=new JScrollPane(tiles);grid.setBorder(null);grid.setOpaque(false);grid.getViewport().setBackground(GROUND);grid.getVerticalScrollBar().setUnitIncrement(24);
        middle.add(grid,"grid");
        middle.add(noteView(),"note");
        // The bin: the same tiles Home and folders use, Empty the bin the one primary action at the top (decision 117).
        binTiles=new Tiles(true);
        JScrollPane binGrid=new JScrollPane(binTiles);binGrid.setBorder(null);binGrid.setOpaque(false);binGrid.getViewport().setBackground(GROUND);binGrid.getVerticalScrollBar().setUnitIncrement(24);
        binTop=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));binTop.setOpaque(false);binTop.setBorder(BorderFactory.createEmptyBorder(10,DesktopHome.SIDE,0,0));
        JPanel binPanel=new JPanel(new BorderLayout());binPanel.setOpaque(false);
        binPanel.add(binTop,BorderLayout.NORTH);binPanel.add(binGrid,BorderLayout.CENTER);
        middle.add(binPanel,"bin");
        // The same + as Home's, in the same place, the foot on the right (the owner, 2026-10-06: "let's keep the same + at
        // the bottom"; decision 114), over whatever is shown, the bin aside.
        plus=DesktopHome.round();plus.setToolTipText("New note or folder");plus.getAccessibleContext().setAccessibleName("New");
        plus.addActionListener(e->{JPopupMenu menu=plusMenu();Dimension m=menu.getPreferredSize();menu.show(plus,plus.getWidth()-m.width-6,-m.height-2);});
        JLayeredPane room=new JLayeredPane(){
            @Override public void doLayout() {
                int w=getWidth(),h=getHeight();middle.setBounds(0,0,w,h);
                Dimension p=plus.getPreferredSize();plus.setBounds(w-p.width-30,h-p.height-18,p.width,p.height);
            }
        };
        room.add(middle,JLayeredPane.DEFAULT_LAYER);room.add(plus,JLayeredPane.PALETTE_LAYER);
        screen.add(room,BorderLayout.CENTER);
        says=new JLabel(" ");says.setFont(DesktopUi.BODY.deriveFont(13f));says.setForeground(DesktopUi.QUIET);says.setBorder(BorderFactory.createEmptyBorder(6,24,10,24));
        screen.add(says,BorderLayout.SOUTH);
        // Esc goes back a level, as everywhere: a note to its folder, a folder up, and at the top it closes.
        screen.registerKeyboardAction(e->escape(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        before=d.frame.getContentPane();
        d.frame.setContentPane(screen);d.frame.revalidate();d.frame.repaint();
        secure(d.frame,true);
        watch.start();
    }

    private JComponent noteView() {
        // Laid out as a note's page is (see Desktop.build): its title, then ruled paper, washed in its colour.
        noteView=new JPanel(new BorderLayout());noteView.setBackground(GROUND);
        noteTitle=new JTextField();noteTitle.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,24f));noteTitle.setForeground(DesktopUi.INK);
        noteTitle.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,"Untitled");noteTitle.setBorder(BorderFactory.createEmptyBorder());noteTitle.setBackground(GROUND);
        noteTitle.getAccessibleContext().setAccessibleName("Note title");
        JPanel titled=new JPanel(new BorderLayout());titled.setOpaque(false);titled.setBorder(BorderFactory.createEmptyBorder(2,42,10,38));titled.add(noteTitle);
        notePage=new Desktop.Paper();notePage.setLineWrap(true);notePage.setWrapStyleWord(true);notePage.setFont(DesktopUi.BODY.deriveFont(18f));
        notePage.setForeground(DesktopUi.INK);notePage.setBackground(GROUND);notePage.setBorder(BorderFactory.createEmptyBorder(12,42,40,38));notePage.setTabSize(4);
        notePage.getAccessibleContext().setAccessibleName("Private note");
        undo=new javax.swing.undo.UndoManager();notePage.getDocument().addUndoableEditListener(e->{if(!loading)undo.addEdit(e.getEdit());});
        notePage.registerKeyboardAction(e->{if(undo.canUndo())undo.undo();},KeyStroke.getKeyStroke("control Z"),JComponent.WHEN_FOCUSED);
        notePage.registerKeyboardAction(e->{if(undo.canRedo())undo.redo();},KeyStroke.getKeyStroke("control Y"),JComponent.WHEN_FOCUSED);
        javax.swing.event.DocumentListener typed=new javax.swing.event.DocumentListener(){
            void changed(){if(loading||shown==null||note==null)return;shown.text(note,noteTitle.getText(),notePage.getText(),System.currentTimeMillis());keep.restart();}
            public void insertUpdate(javax.swing.event.DocumentEvent e){changed();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){changed();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        };
        noteTitle.getDocument().addDocumentListener(typed);notePage.getDocument().addDocumentListener(typed);
        // A code typed in here too: another space opened beside this one, or made remembering it.
        watch(noteTitle,()->!loading,()->{});watch(notePage,()->!loading,undo::discardAllEdits);
        guardCopies(noteTitle);guardCopies(notePage);
        DesktopMenus.onRightClick(notePage,e->noteMenu().show(e.getComponent(),e.getX(),e.getY()));
        noteScroll=DesktopUi.scrolling(notePage);
        noteFiles=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));noteFiles.setOpaque(false);noteFiles.setBorder(BorderFactory.createEmptyBorder(0,34,4,110));
        noteView.add(titled,BorderLayout.NORTH);noteView.add(noteScroll,BorderLayout.CENTER);noteView.add(noteFiles,BorderLayout.SOUTH);
        return noteView;
    }

    /** Everything drawn again from what is open: the heading, the grid, the note, the bin, the line at the foot. */
    private void draw() {
        if(screen==null||shown==null)return;
        PrivateSpace.Thing in=folder.isEmpty()?null:shown.thing(folder);
        if(in==null)folder="";
        boolean onNote=note!=null&&shown.thing(note)!=null;
        if(!onNote)note=null;
        // Where ‹ goes, named: up a level, and at the top out of here, back to Mininotes as it was.
        back.setText("‹ "+(onNote?(in!=null?in.name():"Private"):inBin?"Private":in!=null?parentName(in):"Mininotes"));
        heading.setText(inBin?"Bin":onNote?"Private note":in!=null?in.name():"Private");
        plus.setVisible(!inBin);
        if(inBin){drawBin();cards.show(middle,"bin");}
        else if(onNote){drawNote();cards.show(middle,"note");}
        else{tiles.fill(shown.in(folder));cards.show(middle,"grid");}
        tick();
        screen.revalidate();screen.repaint();
    }
    private String parentName(PrivateSpace.Thing in){PrivateSpace.Thing up=shown.thing(in.parent);return up==null?"Private":up.name();}

    private void drawNote() {
        PrivateSpace.Thing t=shown.thing(note);
        if(!note.equals(notePage.getClientProperty("note"))) {
            loading=true;
            try{noteTitle.setText(t.title);notePage.setText(t.body);notePage.setCaretPosition(0);}finally{loading=false;}
            notePage.putClientProperty("note",note);undo.discardAllEdits();
            SwingUtilities.invokeLater(notePage::requestFocusInWindow);
        }
        // Washed as a note's page is (see Desktop.washPage): the whole of it, at its own strength.
        int tone=Tint.tone(t.tone,d.usual);
        // Over the private ground, so a note with no colour is still on the private screen's own (decision 114).
        Color paper=DesktopLook.wash(t.colour,GROUND,0.12f,0.72f,tone);
        noteView.setBackground(paper);noteTitle.setBackground(paper);notePage.setBackground(paper);noteScroll.getViewport().setBackground(paper);
        notePage.rules=DesktopLook.wash(t.colour,Desktop.Paper.RULES,0.35f,1f,tone);notePage.repaint();
        noteFiles.removeAll();
        for(PrivateSpace.Kept k:shown.filesOf(note))noteFiles.add(chip(k));
        noteFiles.revalidate();noteFiles.repaint();
    }

    private JComponent chip(PrivateSpace.Kept k) {
        JButton chip=DesktopUi.button(k.name+"  ·  "+size(k.bytes),()->{});
        if(k.picture)try{BufferedImage small=read(shown.bytesOf(k.id),64);if(small!=null)chip.setIcon(new ImageIcon(fit(small,28)));}catch(Exception none){/* its name, then */}
        chip.getAccessibleContext().setAccessibleName(k.name);
        for(ActionListener a:chip.getActionListeners())chip.removeActionListener(a);
        chip.addActionListener(e->{if(k.picture)view(k);else fileMenu(k).show(chip,0,chip.getHeight());});
        DesktopMenus.onRightClick(chip,e->fileMenu(k).show(e.getComponent(),e.getX(),e.getY()));
        return chip;
    }

    /**
     * The bin, binned things drawn as the app's own tiles rather than text rows (the owner, 2026-10-07: "these are elements,
     * they look like simple text ... the bin must look better"; decision 117): the same face, colour and name a tile has
     * everywhere, each with Put back / Delete for good, and Empty the bin as the one primary action at the top.
     */
    private void drawBin() {
        List<PrivateSpace.Thing> away=shown.binned();
        binTop.removeAll();
        if(!away.isEmpty()) {
            JButton empty=DesktopUi.primary("Empty the bin…",()->{
                if(!DesktopUi.confirm(d.frame,"Empty the bin?","Everything in the bin is deleted for good. This cannot be undone.","Empty the bin",true))return;
                shown.emptyBin();write();draw();});
            binTop.add(empty);
        }
        binTiles.fill(away);
        binTop.revalidate();binTop.repaint();
    }
    /** A binned thing's menu: Put back, or Delete for good after one question (decision 117). */
    private JPopupMenu binMenu(PrivateSpace.Thing t) {
        JPopupMenu menu=new JPopupMenu();
        item(menu,"Put back",()->{shown.putBack(t.id);write();draw();});
        menu.addSeparator();
        item(menu,"Delete for good…",()->{
            if(!DesktopUi.confirm(d.frame,"Delete for good?","“"+t.name()+"” is deleted for good. This cannot be undone.","Delete for good",true))return;
            shown.deleteForGood(t.id);write();draw();});
        return menu;
    }

    /** The line at the foot: full, or how much still waits to be written, or how much is used. Said here only. */
    private void tick() {
        if(screen==null||shown==null)return;
        if(System.currentTimeMillis()-lastUse>IDLE){close();return;}
        // In minutes, never bytes, the same words as the phone's (decision 114).
        says.setForeground(full?DesktopUi.WARN:DesktopUi.QUIET);
        says.setText(shown.saying(full));
    }

    private static String size(long bytes){return bytes<1024?bytes+" bytes":bytes<1024*1024?(bytes/1024)+" KB":String.format(java.util.Locale.ROOT,"%.1f MB",bytes/1048576.0);}

    /** What changed, handed to be written by the next batches; refused whole where it does not fit. */
    private void write() {
        if(shown==null)return;
        try{shown.write();full=false;}
        catch(Slots.Full f){full=true;if(note!=null){notePage.putClientProperty("note",null);}}
        catch(Throwable quietly){/* nothing said */}
        tick();
    }

    private void leaveNote() {
        if(shown!=null&&note!=null){keep.stop();shown.keepVersion(note,System.currentTimeMillis());write();}
        if(notePage!=null)notePage.putClientProperty("note",null);
    }

    private void up() {
        if(shown==null)return;
        if(inBin){inBin=false;draw();return;}
        if(note!=null){leaveNote();note=null;draw();return;}
        PrivateSpace.Thing in=shown.thing(folder);folder=in==null?"":in.parent;draw();
    }
    private void escape(){if(note!=null||!folder.isEmpty()||inBin)up();else close();}
    private void openThing(PrivateSpace.Thing t) {
        leaveNote();
        if(t.folder){folder=t.id;note=null;}else{note=t.id;folder=t.parent;}
        draw();
        // A help request, where this private thing is set to send one when it opens (decision 113): once per opening. The
        // setting lives in the vault, so nothing outside it ever showed it; this computer has no GPS, so it goes with no place.
        if(t.helpOnOpen&&!t.helpTo.isEmpty())d.helpSendNow(t.helpTo,t.helpMessage);
    }

    // ---- menus --------------------------------------------------------------------------------------------------------

    private JPopupMenu plusMenu() {
        JPopupMenu menu=new JPopupMenu();
        // Home's menu, in Home's words: a note, a folder, and, in a note, a file from this PC into it (decision 114).
        item(menu,"Note",()->{leaveNote();PrivateSpace.Thing t=shown.newNote(folder,System.currentTimeMillis());write();openThing(t);});
        item(menu,"Folder",()->{String name=DesktopUi.ask(d.frame,"New private folder","Name",null);if(name==null)return;shown.newFolder(folder,name,System.currentTimeMillis());write();draw();});
        // At every level now, as Home's + is (decision 115): from the top or a folder a file becomes its own private note named after it.
        item(menu,"From this device…",this::addFiles);
        return menu;
    }
    private JPopupMenu moreMenu() {
        JPopupMenu menu=new JPopupMenu();
        if(note!=null){for(Component c:noteMenu().getComponents())menu.add(c);menu.addSeparator();}
        // The space's own rows, grouped and last, as the app's menu groups the app rows (decision 117); Delete set apart.
        item(menu,"Bin",()->{leaveNote();note=null;inBin=true;draw();});
        item(menu,"Change password…",this::changePassword);
        menu.addSeparator();
        item(menu,"Delete this private space…",this::deleteSpace);
        return menu;
    }

    /**
     * Delete this private space (the owner, 2026-10-07; decision 117): it asks first, plainly, that everything is deleted for
     * good, the password opens nothing after, and it cannot be undone; on yes the space is erased by the next batches (its
     * index first, so it can never be opened again) and the screen closes to the ordinary window.
     */
    private void deleteSpace() {
        if(shown==null)return;
        if(!DesktopUi.confirm(d.frame,"Delete this private space?",
            "Everything in this space is deleted for good. Its password will open nothing after. This cannot be undone: the 12 recovery words do not cover it.",
            "Delete for good",true))return;
        PrivateSpace now=shown;Slots.Space space=now.space;
        // Let the model go here and erase through the slots; close takes the screen away, as leaving does.
        open.remove(space);now.wipe();
        worker.submit(()->{try{space.erase();}catch(Throwable quietly){/* nothing said */}});
        shown=null;note=null;folder="";inBin=false;
        close();
    }
    private JPopupMenu noteMenu() {
        JPopupMenu menu=new JPopupMenu();
        PrivateSpace.Thing t=note==null?null:shown.thing(note);
        if(t==null)return menu;
        menu.add(colours(t));
        item(menu,"Versions…",()->versions(t));
        item(menu,"Add a file…",this::addFiles);
        menu.add(moveTo(t));
        item(menu,Help.SWITCH,()->privateHelp(t));
        menu.addSeparator();
        item(menu,"Delete",()->{leaveNote();shown.bin(t.id,System.currentTimeMillis());note=null;write();draw();});
        return menu;
    }
    private JPopupMenu thingMenu(PrivateSpace.Thing t) {
        JPopupMenu menu=new JPopupMenu();
        item(menu,"Open",()->openThing(t));
        if(t.folder)item(menu,"Rename…",()->{String name=DesktopUi.ask(d.frame,"Rename folder","Name",t.title);if(name==null)return;shown.rename(t.id,name);write();draw();});
        menu.add(colours(t));
        menu.add(moveTo(t));
        item(menu,Help.SWITCH,()->privateHelp(t));
        menu.addSeparator();
        item(menu,"Delete",()->{shown.bin(t.id,System.currentTimeMillis());write();draw();});
        return menu;
    }

    /** The box that sets a private thing's help request on or off (decision 113): the Desktop's own, saving into the vault. */
    private void privateHelp(final PrivateSpace.Thing t) {
        if(t==null||shown==null)return;
        d.helpSetup(d.frame,true,t.helpOnOpen,t.helpTo,t.helpMessage,
            (to,words)->{shown.help(t.id,true,to,words);write();},
            ()->{shown.help(t.id,false,null,null);write();});
    }

    private JMenu colours(PrivateSpace.Thing t) {
        JMenu colour=new JMenu("Colour");
        for(int c=0;c<Tint.count();c++) {
            int pick=c;
            JMenuItem one=new JMenuItem(Tint.NAMES[c],DesktopLook.swatch(c,16,t.colour==c||!Tint.known(t.colour)&&c==Tint.NONE));
            one.addActionListener(e->{shown.colour(t.id,pick);write();draw();});
            colour.add(one);
        }
        return colour;
    }
    private JMenu moveTo(PrivateSpace.Thing t) {
        JMenu move=new JMenu("Move to");
        JMenuItem top=new JMenuItem("Private");top.setEnabled(!t.parent.isEmpty());top.addActionListener(e->{shown.move(t.id,"");write();draw();});move.add(top);
        for(PrivateSpace.Thing f:shown.placesFor(t.id)) {
            JMenuItem one=new JMenuItem(f.name());one.setEnabled(!f.id.equals(t.parent));
            one.addActionListener(e->{shown.move(t.id,f.id);write();draw();});move.add(one);
        }
        return move;
    }
    private JPopupMenu fileMenu(PrivateSpace.Kept k) {
        JPopupMenu menu=new JPopupMenu();
        if(k.picture)item(menu,"Open",()->view(k));
        item(menu,"Save a copy…",()->saveCopy(k));
        menu.addSeparator();
        item(menu,"Remove",()->{shown.removeFile(k.id);write();draw();});
        return menu;
    }
    private static void item(JPopupMenu menu,String words,Runnable does){JMenuItem one=new JMenuItem(words);one.addActionListener(e->does.run());menu.add(one);}

    private void versions(PrivateSpace.Thing t) {
        keep.stop();shown.keepVersion(t.id,System.currentTimeMillis());write();
        List<PrivateSpace.Version> all=shown.versionsOf(t.id);
        if(all.size()<2){DesktopUi.tell(d.frame,"Versions",DesktopUi.note("Versions are kept each time you leave the note after writing in it. There is no older one yet.",360,DesktopUi.INK,DesktopUi.BODY));return;}
        java.text.DateFormat when=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT);
        PrivateSpace.Version chosen=DesktopUi.pick(d.frame,"Versions","Put an older version back. What the note says now is kept as a version.",all.subList(1,all.size()),
            v->when.format(new java.util.Date(v.at))+"  ·  "+(Given.firstLine(v.body).isEmpty()?"Empty":Given.firstLine(v.body)),"Put back");
        if(chosen==null)return;
        shown.putBackVersion(t.id,chosen,System.currentTimeMillis());notePage.putClientProperty("note",null);write();draw();
    }

    private void changePassword() {
        JPasswordField first=new JPasswordField(24),again=new JPasswordField(24);
        first.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,"New password");again.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,"The same again");
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note("A long phrase of several words is best. Nobody can recover it if it is forgotten. Until everything is written again under it, the old password still opens this.",360,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
        // An eye at each field's right that shows the characters while it is held (decision 116).
        DesktopUi.gap(body,12);DesktopUi.add(body,withEye(first));DesktopUi.gap(body,8);DesktopUi.add(body,withEye(again));
        JDialog[] box={null};JLabel wrong=new JLabel(" ");wrong.setForeground(DesktopUi.WARN);DesktopUi.gap(body,6);DesktopUi.add(body,wrong);
        JButton go=DesktopUi.primary("Change password",()->{
            char[] a=first.getPassword(),b=again.getPassword();
            if(a.length==0){wrong.setText("Type the new password.");return;}
            if(!Arrays.equals(a,b)){wrong.setText("The two are not the same.");Arrays.fill(a,'\0');Arrays.fill(b,'\0');return;}
            Arrays.fill(b,'\0');box[0].dispose();
            PrivateSpace now=shown;says.setText("Changing the password…");
            worker.submit(()->{
                boolean[] fit={true};
                try{now.space.rekey(a);}catch(Slots.Full f){fit[0]=false;}catch(Throwable quietly){fit[0]=false;}
                finally{Arrays.fill(a,'\0');}
                SwingUtilities.invokeLater(()->{if(shown==now){full=!fit[0];tick();if(fit[0])says.setText("Password changed. Keep Mininotes open until it is all written.");}});
            });
        });
        box[0]=DesktopUi.sheet(d.frame,"Change password",body,DesktopUi.footer(go),true);
        box[0].getRootPane().setDefaultButton(go);DesktopUi.show(box[0],440,420);
    }

    // ---- files and pictures -------------------------------------------------------------------------------------------

    private void addFiles() {
        if(shown==null)return;
        JFileChooser pick=new JFileChooser();pick.setMultiSelectionEnabled(true);
        if(pick.showOpenDialog(d.frame)!=JFileChooser.APPROVE_OPTION)return;
        File[] chosen=pick.getSelectedFiles();if(chosen.length==0&&pick.getSelectedFile()!=null)chosen=new File[]{pick.getSelectedFile()};
        final String into=note;final String parent=note==null?folder:null;final PrivateSpace now=shown;says.setText("Adding…");
        final List<File> files=Arrays.asList(chosen);
        worker.submit(()->{
            List<Object[]> ready=new ArrayList<>();
            for(File f:files) {
                try {
                    byte[] raw=Files.readAllBytes(f.toPath());
                    byte[] small=shrink(raw);
                    if(small!=null){Arrays.fill(raw,(byte)0);ready.add(new Object[]{jpegName(f.getName()),"image/jpeg",small,true,f});}
                    else ready.add(new Object[]{f.getName(),"",raw,false,f});
                } catch(Throwable unreadable){/* that one is not added */}
            }
            SwingUtilities.invokeLater(()->{
                if(shown!=now)return;
                List<File> added=new ArrayList<>();
                for(Object[] one:ready) {
                    String target=into;
                    // From the top or a folder: a private note named after the file, the file kept with it (decision 115).
                    if(target==null){PrivateSpace.Thing t=now.newNote(parent==null?"":parent,System.currentTimeMillis());now.rename(t.id,(String)one[0]);target=t.id;}
                    now.addFile(target,(String)one[0],(String)one[1],(byte[])one[2],(Boolean)one[3],System.currentTimeMillis());write();
                    if(full)break;
                    added.add((File)one[4]);
                }
                draw();
                offerTrashOriginals(added);
            });
        });
    }

    /**
     * After a file is added from the PC's picker, offer once to move the original to the Recycle Bin (decision 115). The copy
     * in the vault is kept; the original stays where it was unless the owner says so. Where this desktop cannot move it to the
     * Recycle Bin, it is said plainly.
     */
    private void offerTrashOriginals(List<File> originals) {
        if(originals==null||originals.isEmpty())return;
        boolean one=originals.size()==1;
        String named=one?"“"+originals.get(0).getName()+"”":originals.size()+" files";
        if(!DesktopUi.confirm(d.frame,"Move the original to the Recycle Bin?",
            (one?named+" stays where it was unless you move it.":named+" stay where they were unless you move them.")+" The copy kept here is private.",
            one?"Move to Recycle Bin":"Move them",false))return;
        if(!(java.awt.Desktop.isDesktopSupported()&&java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MOVE_TO_TRASH))) {
            says.setText("Mininotes cannot move it to the Recycle Bin here. Delete it in File Explorer.");return;
        }
        int gone=0;boolean refused=false;
        for(File f:originals){try{if(java.awt.Desktop.getDesktop().moveToTrash(f))gone++;else refused=true;}catch(Throwable cannot){refused=true;}}
        says.setText(refused?"Some originals could not be moved. Delete them in File Explorer."
            :(gone==1?"The original is in the Recycle Bin":"The originals are in the Recycle Bin"));
    }
    static String jpegName(String name){int dot=name.lastIndexOf('.');return (dot>0?name.substring(0,dot):name)+".jpg";}

    /**
     * A picture made small on its way in (the owner, 2026-10-06: "maybe like 50 photos"): about two megapixels, its longest
     * side {@link #LONGEST}, turned the way up its camera said, JPEG at {@link #QUALITY}, and nothing of the camera or of
     * where it was taken kept, since a picture made again carries none of it. In memory from end to end. Null where it is
     * not a picture this PC reads, which is then kept as it is. Drawn as every picture added anywhere is (decision 112),
     * always made again here, as it was.
     */
    static byte[] shrink(byte[] original) throws IOException {return Shrink.remade(original,DesktopPictures.PAINTER);}
    /** Bytes read as a picture in memory, every n-th pixel where it is larger than it needs to be; null where it is not one. */
    static BufferedImage read(byte[] bytes,int least) throws IOException {
        if(bytes==null)return null;
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())return null;
            ImageReader reader=readers.next();
            try {
                reader.setInput(in,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>30000||h>30000)return null;
                javax.imageio.ImageReadParam param=reader.getDefaultReadParam();
                if(least>0){int step=DesktopIcons.subsampling(w,h,least);if(step>1)param.setSourceSubsampling(step,step,0,0);}
                return reader.read(0,param);
            } finally{reader.dispose();}
        }
    }
    private static Image fit(BufferedImage in,int side) {
        double s=Math.min(side/(double)in.getWidth(),side/(double)in.getHeight());
        return in.getScaledInstance(Math.max(1,(int)(in.getWidth()*s)),Math.max(1,(int)(in.getHeight()*s)),Image.SCALE_SMOOTH);
    }

    /** A picture, shown in a box of its own, from memory: never through a file another program could open. */
    private void view(PrivateSpace.Kept k) {
        try {
            BufferedImage picture=read(shown.bytesOf(k.id),0);if(picture==null){fileMenu(k).show(noteFiles,0,0);return;}
            Rectangle room=d.frame.getGraphicsConfiguration().getBounds();
            int most=Math.min(room.width,room.height)*3/4;
            JLabel shownPicture=new JLabel(new ImageIcon(fit(picture,most)));
            JPanel body=new JPanel(new BorderLayout());body.setOpaque(false);body.add(shownPicture);
            JDialog box=DesktopUi.sheet(d.frame,k.name,body,DesktopUi.footer(DesktopUi.button("Save a copy…",()->saveCopy(k))),true);
            DesktopUi.show(box,Math.min(room.width-80,most+80),most+160);
        } catch(Throwable quietly){/* nothing said */}
    }

    /** A copy saved where the user says: their own act, the one way anything private leaves Mininotes. */
    private void saveCopy(PrivateSpace.Kept k) {
        JFileChooser pick=new JFileChooser();pick.setSelectedFile(new File(k.name));
        if(pick.showSaveDialog(d.frame)!=JFileChooser.APPROVE_OPTION)return;
        Path target=pick.getSelectedFile().toPath();
        if(Files.exists(target)&&!DesktopUi.confirm(d.frame,"Replace it?","A file called "+target.getFileName()+" is already there. Replace it?","Replace",true))return;
        try{Files.write(target,shown.bytesOf(k.id));says.setText("Saved a copy");}catch(Throwable failed){says.setText("Could not save a copy there");}
    }

    // ---- closing --------------------------------------------------------------------------------------------------------

    /**
     * Everything closes: what was typed handed to be written, the screen gone and the window as it was, every open space
     * let go (what still waits to be written goes in the next batches, sealed), and what was copied from in here taken
     * off the clipboard if it is still there.
     */
    void close() {
        catching=null;caught.setLength(0);
        if(screen==null&&open.isEmpty()){worker.submit(()->{Slots s=slots;if(s!=null)s.close();});return;}
        try {
            // What was typed is in it already, a key at a time; kept as a version as leaving a note keeps one.
            if(shown!=null&&note!=null){keep.stop();shown.keepVersion(note,System.currentTimeMillis());}
            for(PrivateSpace one:open.values())try{one.write();}catch(Throwable notKept){/* refused whole: as it was */}
        } finally {
            for(PrivateSpace one:open.values())one.wipe();
            open.clear();shown=null;note=null;folder="";inBin=false;full=false;
            if(notePage!=null){loading=true;try{notePage.setText("");noteTitle.setText("");}finally{loading=false;}notePage.putClientProperty("note",null);undo.discardAllEdits();}
            keep.stop();watch.stop();
            unclip();
            if(screen!=null) {
                d.frame.setContentPane(before);before=null;screen=null;tiles=null;binTiles=null;
                d.frame.revalidate();d.frame.repaint();
                secure(d.frame,false);
            }
            worker.submit(()->{Slots s=slots;if(s!=null)s.close();});
        }
    }

    /** What was copied from in here, off the clipboard, if nothing else was copied since. */
    private void unclip() {
        String was=copied;copied=null;
        if(was==null)return;
        try {
            Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
            Object now=clip.isDataFlavorAvailable(DataFlavor.stringFlavor)?clip.getData(DataFlavor.stringFlavor):null;
            if(was.equals(now))clip.setContents(new StringSelection(""),null);
        } catch(Throwable quietly){/* the clipboard was busy: nothing more can be done */}
    }
    private void guardCopies(JTextComponent c) {
        TransferHandler was=c.getTransferHandler();c.setDragEnabled(false);
        c.setTransferHandler(new TransferHandler() {
            @Override public void exportToClipboard(JComponent comp,Clipboard clip,int action) {
                was.exportToClipboard(comp,clip,action);
                try{Object got=clip.getData(DataFlavor.stringFlavor);copied=got==null?null:got.toString();}catch(Exception none){/* nothing copied */}
            }
            @Override public boolean importData(TransferSupport s){return was.importData(s);}
            @Override public boolean canImport(TransferSupport s){return was.canImport(s);}
            @Override public int getSourceActions(JComponent comp){return was.getSourceActions(comp);}
        });
    }

    // ---- out of pictures ----------------------------------------------------------------------------------------------

    /** Left out of screenshots, screen recordings and the taskbar's pictures while it is up: Windows 10 2004 and later. */
    static void secure(Window w,boolean on) {
        try {
            if(!w.isDisplayable())return;
            com.sun.jna.platform.win32.WinDef.HWND hwnd=new com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Native.getWindowPointer(w));
            if(!Affinity.USER.SetWindowDisplayAffinity(hwnd,on?0x11:0)&&on)Affinity.USER.SetWindowDisplayAffinity(hwnd,1);
        } catch(Throwable older){/* nothing more this Windows can do */}
    }
    interface Affinity extends com.sun.jna.win32.StdCallLibrary {
        Affinity USER=com.sun.jna.Native.load("user32",Affinity.class,com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS);
        boolean SetWindowDisplayAffinity(com.sun.jna.platform.win32.WinDef.HWND window,int affinity);
    }

    // ---- the look -----------------------------------------------------------------------------------------------------

    /** The private screen's own ground and its lock's colour (see PrivateSpace.GROUND): the PC has the light paper only. */
    static final Color GROUND=new Color(PrivateSpace.GROUND),LOCKED=new Color(PrivateSpace.LOCK);

    /** The lock beside Private, drawn as the phone draws it (PrivateScreen.Lock): a shackle over a filled body. */
    static final class Lock implements Icon {
        final int w,h;Lock(int w,int h){this.w=w;this.h=h;}
        public int getIconWidth(){return w;}public int getIconHeight(){return h;}
        public void paintIcon(Component c,Graphics g0,int x0,int y0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x0,y0);g.setColor(LOCKED);
            float side=Math.min(w,h*0.82f),x=(w-side)/2,body=h*0.52f,top=h-body,arm=side*0.30f,cx=w/2f;
            g.setStroke(new BasicStroke(side*0.16f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Arc2D.Float(cx-arm,h*0.08f,2*arm,2*arm,0,180,java.awt.geom.Arc2D.OPEN));
            g.draw(new java.awt.geom.Line2D.Float(cx-arm,h*0.08f+arm,cx-arm,top+1));g.draw(new java.awt.geom.Line2D.Float(cx+arm,h*0.08f+arm,cx+arm,top+1));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(x,top,side,body,side*0.36f,side*0.36f));
            g.dispose();
        }
    }

    /** The eye on a password field, drawn as the phone draws it (PrivateScreen.Eye): an almond and a pupil, a slash while masked. */
    static final class Eye implements Icon {
        final int w,h;final boolean open;
        Eye(int w,int h,boolean open){this.w=w;this.h=h;this.open=open;}
        public int getIconWidth(){return w;}public int getIconHeight(){return h;}
        public void paintIcon(Component c,Graphics g0,int x0,int y0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x0,y0);g.setColor(LOCKED);
            float cx=w/2f,cy=h/2f,rx=Math.min(w,h)*0.44f,ry=rx*0.62f;
            g.setStroke(new BasicStroke(Math.max(1f,rx*0.22f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D.Float almond=new java.awt.geom.Path2D.Float();
            almond.moveTo(cx-rx,cy);almond.quadTo(cx,cy-ry*1.9f,cx+rx,cy);almond.quadTo(cx,cy+ry*1.9f,cx-rx,cy);almond.closePath();
            g.draw(almond);
            g.fill(new java.awt.geom.Ellipse2D.Float(cx-rx*0.34f,cy-rx*0.34f,rx*0.68f,rx*0.68f));
            if(!open)g.draw(new java.awt.geom.Line2D.Float(cx-rx*0.9f,cy-ry*1.1f,cx+rx*0.9f,cy+ry*1.1f));
            g.dispose();
        }
    }
    /** A password field with an eye at its right that shows the characters while it is held, masking them when let go (decision 116). */
    private static JPanel withEye(JPasswordField field) {
        JPanel row=new JPanel(new BorderLayout());row.setOpaque(false);
        row.add(field);row.add(eye(field),BorderLayout.EAST);
        return row;
    }
    /** The eye button: while pressed it shows the field's characters, and masks them again when let go. */
    private static JComponent eye(JPasswordField field) {
        char echo=field.getEchoChar();
        JButton button=new JButton(new Eye(20,14,false));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.setFocusable(false);button.setToolTipText("Hold to show the password");button.getAccessibleContext().setAccessibleName("Show the password");
        button.getModel().addChangeListener(e->{boolean down=button.getModel().isPressed();field.setEchoChar(down?(char)0:echo);button.setIcon(new Eye(20,14,down));});
        return button;
    }

    // ---- the grid -----------------------------------------------------------------------------------------------------

    /** The private things in a folder, as Home lays its icons out, each drawn as Home draws it. */
    private final class Tiles extends JPanel {
        final boolean bin;
        Tiles(){this(false);}
        Tiles(boolean bin){super(null);setOpaque(false);this.bin=bin;}
        void fill(List<PrivateSpace.Thing> things) {
            removeAll();
            if(things.isEmpty()){JLabel none=new JLabel(bin?"Nothing is in the bin.":folder.isEmpty()?"Nothing private yet. Make a note or a folder with the +.":"Nothing in this folder yet.");
                none.setFont(DesktopUi.BODY.deriveFont(13f));none.setForeground(DesktopUi.QUIET);none.setName("none");add(none);}
            for(PrivateSpace.Thing t:things)add(new Tile(t,bin));
            revalidate();repaint();
        }
        private int columns(){Container up=getParent();int w=up!=null&&up.getWidth()>0?up.getWidth():getWidth();return DesktopHome.columns(w);}
        @Override public void doLayout() {
            int cols=columns(),left=DesktopHome.SIDE,i=0;
            for(Component c:getComponents()) {
                if("none".equals(c.getName())){Dimension p=c.getPreferredSize();c.setBounds(left,DesktopHome.TOP+8,p.width,p.height);continue;}
                c.setBounds(left+(i%cols)*DesktopHome.CELL,DesktopHome.TOP+(i/cols)*DesktopHome.HIGH,DesktopHome.CELL,DesktopHome.HIGH);i++;
            }
        }
        @Override public Dimension getPreferredSize() {
            int cols=columns(),n=getComponentCount();
            return new Dimension(cols*DesktopHome.CELL+2*DesktopHome.SIDE,DesktopHome.TOP*2+Math.max(1,(n+cols-1)/cols)*DesktopHome.HIGH);
        }
    }
    private final class Tile extends JPanel {
        final PrivateSpace.Thing thing;final NoteStore.Branch face;boolean over;final boolean bin;
        Tile(PrivateSpace.Thing t,boolean bin) {
            super(null);setOpaque(false);thing=t;this.bin=bin;
            int inside=t.folder?shown.in(t.id).size():0;
            face=new NoteStore.Branch(t.folder?NoteStore.Branch.Kind.COLLECTION:NoteStore.Branch.Kind.PAGE,t.id,t.parent,t.name(),
                t.folder?(inside==1?"1 thing":inside+" things"):"",0,0,t.folder,t.colour);
            face.tone=t.tone;
            setFocusable(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName((t.folder?"Folder ":"Note ")+t.name()+(bin?". In the bin.":""));
            addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){over=true;repaint();}
                @Override public void mouseExited(MouseEvent e){over=false;repaint();}
                // In the bin a tile offers Put back / Delete for good; anywhere else it opens (decision 117).
                @Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)){if(Tile.this.bin)binMenu(thing).show(Tile.this,e.getX(),e.getY());else openThing(thing);}}
            });
            DesktopMenus.onRightClick(this,e->(Tile.this.bin?binMenu(thing):thingMenu(thing)).show(e.getComponent(),e.getX(),e.getY()));
            registerKeyboardAction(e->{if(Tile.this.bin)binMenu(thing).show(Tile.this,getWidth()/2,0);else openThing(thing);},KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),WHEN_FOCUSED);
            addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){repaint();}public void focusLost(FocusEvent e){repaint();}});
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
            if(over||isFocusOwner()){g.setColor(DesktopUi.mix(DesktopUi.ACCENT,GROUND,0.91f));g.fillRoundRect(3,2,getWidth()-6,getHeight()-4,16,16);}
            if(isFocusOwner()){g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(1.6f));g.drawRoundRect(3,2,getWidth()-7,getHeight()-5,16,16);}
            int side=DesktopHome.FACE,x=(getWidth()-side)/2,y=10;
            DesktopHome.face(g,face,x,y,side,d.usual);
            g.setFont(DesktopUi.BODY);g.setColor(DesktopUi.INK);FontMetrics m=g.getFontMetrics();
            int ty=y+side+8+m.getAscent();
            for(String line:DesktopHome.lines(thing.name(),m,getWidth()-14,2)){g.drawString(line,(getWidth()-m.stringWidth(line))/2f,ty);ty+=m.getHeight();}
            g.dispose();
        }
    }
}
