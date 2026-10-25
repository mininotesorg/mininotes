package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.*;

/**
 * Home: the PC's first screen, laid out as the phone's is and as a phone's own desktop is (docs/HOME.md, step 3). It
 * fills the window under the toolbar: the grid of what is on Home - its collections and notes in the owner's order, then
 * the files that came from other devices - in as many columns as the window holds; a round <b>+</b> at the foot on the
 * right; the search field with the overview button beside it; and the dock, with as many favourites as fit. The search
 * and the dock float over the icons, so the lines of icons run on under them (decision 128). The status
 * bar stays under all of it, where a window keeps it (decision 25). A collection opens as a card over the dimmed Home
 * (see {@link DesktopFolder}); a note opens as the page it always was, filling the window.
 *
 * <p>Every icon stands in the cell it was put in, with the empty cells between left empty, as a phone's own home screen
 * keeps them (decision 39, {@link Layout}). Every icon is picked up the way the phone's are: pressed and moved, it is
 * carried. Let go in an empty cell it stays there; on a note it makes a collection of the two; on a collection it goes
 * in; out of a card onto the dimmed Home it comes up a level, into the cell it is let go in if that is empty; on the
 * dock it is a favourite there. What each drop means is {@link Grid}'s, shared with the phone; this class only measures
 * where the pointer is. Right-click, the Menu key or Shift+F10 gives the thing's menu - the same one, in the same order,
 * as everywhere else - and the keyboard reaches every icon: the arrows move between them, Ctrl+arrow moves the icon
 * itself a cell, Enter opens, F2 renames, Delete puts it in the bin.
 *
 * <p>Home's places stand among the icons (decision 41): the Favourites collection while the dock has no room for every
 * favourite, and the archive and the bin unless they are switched off in Settings, each with how many things wait in it.
 * Each opens as a card of what is in it; a thing let go on the Archive is archived, on the Bin it goes in the bin. A
 * place is carried to another empty cell as any icon is (decision 42), and into nothing.
 */
final class DesktopHome extends JPanel {
    /** Each icon and each line says what it stands for under this key: files from Explorer and the drag ask it. */
    static final String THING="mininotes.thing";
    /** And its menu under this one, made when it is asked for: the Menu key and Shift+F10 open it (see Desktop.contextKey). */
    static final String MENU="mininotes.menu";
    /** What files let go here are kept with: Home, or a collection (see DesktopDrops.aim). */
    static final String KEEPS="mininotes.keeps";
    /** One icon's column and row, its face, the room at either side of a grid, and the room under it for the +. */
    static final int CELL=112,HIGH=126,FACE=64,SIDE=24,TOP=14,UNDER=92;
    /** One place in the dock, the face in it, and the room at its ends. */
    static final int DOCKED=60,DOCK_FACE=44,DOCK_ENDS=14;

    /**
     * Where an icon is: on Home, in a collection's card, in the Favourites card, in the dock, or in the archive's or the
     * bin's card (decision 41), where a thing waits to be put back.
     */
    enum Where { HOME, CARD, FAVOURITES, DOCK, AWAY }

    /**
     * Whether the keyboard, rather than the pointer, was used last. The ring round the icon that has the keyboard shows
     * only then, as a browser's focus-visible does: drawn after every click, it read as the icon being chosen.
     */
    static volatile boolean keyboardLast;
    static {
        if(!GraphicsEnvironment.isHeadless())Toolkit.getDefaultToolkit().addAWTEventListener(e->{
            if(e.getID()==KeyEvent.KEY_PRESSED)keyboardLast=true;else if(e.getID()==MouseEvent.MOUSE_PRESSED)keyboardLast=false;
        },AWTEvent.KEY_EVENT_MASK|AWTEvent.MOUSE_EVENT_MASK);
    }
    /** Whether a component has the keyboard and it shows: the keyboard was used last. */
    static boolean ringed(JComponent c){return c.isFocusOwner()&&keyboardLast;}

    // ---- the rules, without a window, so they are unit tested --------------------------------------------------------

    /** How many icons go across a grid this wide: as many whole columns as fit between its margins, one at the least. */
    static int columns(int width){return Math.max(1,(width-2*SIDE)/CELL);}

    /** How many favourites the dock shows at this width: as many as fit (decision 3), one at the least. */
    static int dockRoom(int width){return Math.max(1,(width-2*DOCK_ENDS)/DOCKED);}

    /** What a key pressed on an icon does. The MOVE_ ones move the icon itself, a cell that way. */
    enum Does { OPEN, RENAME, BIN, MENU, LEFT, RIGHT, UP, DOWN, FIRST, LAST, MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN, NONE }

    /**
     * A key on an icon, as the rest of Windows reads it: Enter or Space opens, F2 renames, Delete bins, the Menu key or
     * Shift+F10 gives the menu, the arrows move, Home and End go to the ends. With Ctrl or Alt held an arrow moves the
     * icon itself one cell - the keyboard's way to put a thing where it is wanted; any other key with them is somebody
     * else's - Ctrl+N, Ctrl+Tab and the rest belong to the window.
     */
    static Does does(int key,boolean shift,boolean ctrlOrAlt) {
        if(ctrlOrAlt) {
            if(shift)return Does.NONE;
            switch(key) {
                case KeyEvent.VK_LEFT: return Does.MOVE_LEFT;
                case KeyEvent.VK_RIGHT: return Does.MOVE_RIGHT;
                case KeyEvent.VK_UP: return Does.MOVE_UP;
                case KeyEvent.VK_DOWN: return Does.MOVE_DOWN;
                default: return Does.NONE;
            }
        }
        switch(key) {
            case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: return shift?Does.NONE:Does.OPEN;
            case KeyEvent.VK_F2: return Does.RENAME;
            case KeyEvent.VK_DELETE: return Does.BIN;
            case KeyEvent.VK_CONTEXT_MENU: return Does.MENU;
            case KeyEvent.VK_F10: return shift?Does.MENU:Does.NONE;
            case KeyEvent.VK_LEFT: return Does.LEFT;
            case KeyEvent.VK_RIGHT: return Does.RIGHT;
            case KeyEvent.VK_UP: return Does.UP;
            case KeyEvent.VK_DOWN: return Does.DOWN;
            case KeyEvent.VK_HOME: return Does.FIRST;
            case KeyEvent.VK_END: return Does.LAST;
            default: return Does.NONE;
        }
    }

    /**
     * Where an arrow takes the keyboard from icon {@code at} of {@code count}, {@code columns} across, drawn one after the
     * other - the dock's row: along the row, wrapping to the next; up and down a column; and down from a row with nothing
     * under it, to the last icon, as a file window does. At an edge it stays. A grid of cells goes by {@link #stepTo}.
     */
    static int step(int at,int count,int columns,Does move) {
        if(count<=0)return -1;
        int cols=Math.max(1,columns),here=Math.max(0,Math.min(count-1,at));
        switch(move) {
            case LEFT: return Math.max(0,here-1);
            case RIGHT: return Math.min(count-1,here+1);
            case UP: return here-cols>=0?here-cols:here;
            case DOWN: {
                if(here+cols<count)return here+cols;
                return here/cols<(count-1)/cols?count-1:here;
            }
            case FIRST: return 0;
            case LAST: return count-1;
            default: return here;
        }
    }

    /**
     * Where an arrow takes the keyboard on a grid whose icons stand where they were put ({@link Layout#arrange}), by id:
     * along, the next icon in reading order - on along the row, then the rows below - or the one before it, stepping over
     * the empty cells; up and down, the nearest icon that way, a column across counting twice a row, so the one straight
     * above or below wins over one nearer but off to the side, and the leftmost where two are as near. Home and End go to
     * the first and the last in reading order. With nothing that way, it stays.
     */
    static String stepTo(Map<String,int[]> drawn,String from,Does move) {
        int[] here=drawn.get(from);
        if(here==null)return from;
        List<Map.Entry<String,int[]>> reading=new ArrayList<>();
        for(Map.Entry<String,int[]> one:drawn.entrySet())if(one.getValue()!=null)reading.add(one);
        reading.sort((a,b)->a.getValue()[0]!=b.getValue()[0]?Integer.compare(a.getValue()[0],b.getValue()[0]):Integer.compare(a.getValue()[1],b.getValue()[1]));
        int at=-1;for(int i=0;i<reading.size();i++)if(reading.get(i).getKey().equals(from))at=i;
        switch(move) {
            case LEFT: return at>0?reading.get(at-1).getKey():from;
            case RIGHT: return at>=0&&at+1<reading.size()?reading.get(at+1).getKey():from;
            case FIRST: return reading.get(0).getKey();
            case LAST: return reading.get(reading.size()-1).getKey();
            case UP: case DOWN: {
                String best=from;int bestScore=Integer.MAX_VALUE,bestAcross=Integer.MAX_VALUE;
                for(Map.Entry<String,int[]> one:reading) {
                    int rows=(move==Does.DOWN?1:-1)*(one.getValue()[0]-here[0]);
                    if(rows<=0)continue;
                    int across=Math.abs(one.getValue()[1]-here[1]),score=rows+2*across;
                    if(score<bestScore||score==bestScore&&across<bestAcross){best=one.getKey();bestScore=score;bestAcross=across;}
                }
                return best;
            }
            default: return from;
        }
    }

    /**
     * Where Ctrl+arrow moves an icon: the cell next to it that way, {row, column}, where that is on a grid {@code columns}
     * wide and empty; null where it is not - the edge, or another icon in the way. Down always has somewhere to go: the
     * grid keeps an empty row under its last icon.
     */
    static int[] nudged(Map<String,int[]> drawn,String id,Does move,int columns) {
        int[] here=drawn.get(id);
        if(here==null)return null;
        int row=here[0],column=here[1];
        switch(move) {
            case MOVE_LEFT: column--;break;
            case MOVE_RIGHT: column++;break;
            case MOVE_UP: row--;break;
            case MOVE_DOWN: row++;break;
            default: return null;
        }
        if(row<0||column<0||column>=Math.max(1,Math.min(Layout.WIDEST,columns)))return null;
        return Layout.at(drawn,row,column)==null?new int[]{row,column}:null;
    }

    /**
     * The cell under a point of a grid's view, {row, column}, whether or not anything is in it: {@code seen} is where the
     * point is in the part of the grid that shows, {@code scrolled} how far the grid is scrolled, and {@code left} and
     * {@code top} the room before its first column and its first row. A point in that room, or in the room past the last
     * column, takes the nearest cell, so an icon carried to the edge of the grid still has somewhere to go.
     */
    static int[] cellUnder(java.awt.Point seen,java.awt.Point scrolled,int left,int top,int columns) {
        int cols=Math.max(1,Math.min(Layout.WIDEST,columns));
        float x=Math.max(0,Math.min(cols*CELL-1,seen.x+scrolled.x-left)),y=Math.max(0,seen.y+scrolled.y-top);
        return Layout.under(x,y,CELL,HIGH,cols);
    }

    /** What the dock shows, and the favourites that are not in it. */
    record Docked(List<NoteStore.Branch> shown,List<NoteStore.Branch> rest) {
        /** Whether the dock shows this thing: what "Remove from the dock" is offered on. */
        boolean shows(String id){for(NoteStore.Branch one:shown)if(one.id.equals(id))return true;return false;}
    }

    /**
     * The PC's dock taken out of the dock it kept before 0.3.077 (decision 190): what was taken out of it here, kept in this
     * PC's settings, is taken out of the notebook's own dock now, once, and the setting forgotten.
     */
    static final String KEPT_OUT="keptOutOfDock";
    static void keptOutOnce(org.mininotes.desktop.platform.content.Context context,NoteStore store) {
        var settings=context.getSharedPreferences("settings",0);
        Set<String> out=settings.getStringSet(KEPT_OUT,Set.of());
        if(out.isEmpty())return;
        for(NoteStore.Branch one:store.dock())if(out.contains(one.id))store.outOfDock(one.kind,one.id);
        settings.edit().putStringSet(KEPT_OUT,new java.util.HashSet<>()).apply();
    }

    /** The strip the dock scrolls in: no wider than the window less its margins, the wheel turned into across. */
    private JScrollPane dockScroll(Dock inside) {
        JScrollPane strip=new JScrollPane(inside,ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED){
            @Override public Dimension getPreferredSize() {
                Dimension want=inside.getPreferredSize();
                int room=Math.max(DOCKED+2*DOCK_ENDS,(getParent()!=null&&getParent().getParent()!=null?getParent().getParent().getWidth():want.width)-48);
                boolean across=want.width>room;
                return new Dimension(Math.min(want.width,room),want.height+(across?getHorizontalScrollBar().getPreferredSize().height:0));
            }
            @Override protected void paintComponent(Graphics g){inside.shell(g,getWidth(),inside.getHeight());}
        };
        strip.setBorder(BorderFactory.createEmptyBorder());strip.setOpaque(false);strip.getViewport().setOpaque(false);
        strip.getHorizontalScrollBar().setUnitIncrement(DOCKED/2);
        strip.getHorizontalScrollBar().putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"width:6");
        strip.setWheelScrollingEnabled(false);
        strip.addMouseWheelListener(e->{JScrollBar bar=strip.getHorizontalScrollBar();if(!bar.isVisible())return;
            bar.setValue(bar.getValue()+(int)Math.round(e.getPreciseWheelRotation()*DOCKED));e.consume();});
        return strip;
    }

    /** How many things a collection's line says it holds - "2 collections · 3 notes" - so its face can show them. */
    static int countIn(String detail) {
        int count=0,number=0;String said=detail==null?"":detail;
        for(int at=0;at<=said.length();at++) {
            char digit=at<said.length()?said.charAt(at):' ';
            if(digit>='0'&&digit<='9')number=number*10+(digit-'0');
            else{count+=number;number=0;}
        }
        return count;
    }

    /**
     * A name in lines of a width, at most {@code most} of them: broken between words where it can be, inside a word too
     * long for a line where it must, and the last line ended with … when there is more than fits.
     */
    static List<String> lines(String name,FontMetrics m,int width,int most) {
        List<String> out=new ArrayList<>();
        String rest=name==null||name.isBlank()?"Untitled":name.trim().replaceAll("\\s+"," ");
        while(!rest.isEmpty()&&out.size()<most) {
            if(m.stringWidth(rest)<=width){out.add(rest);rest="";break;}
            int cut=rest.length();
            while(cut>1&&m.stringWidth(rest.substring(0,cut))>width)cut--;
            int space=rest.lastIndexOf(' ',cut);
            int end=space>0?space:cut;
            out.add(rest.substring(0,end));rest=rest.substring(end).trim();
        }
        if(!rest.isEmpty()&&!out.isEmpty()) {
            String last=out.get(out.size()-1)+" "+rest;
            while(last.length()>1&&m.stringWidth(last+"…")>width)last=last.substring(0,last.length()-1);
            out.set(out.size()-1,last.trim()+"…");
        }
        return out;
    }

    // ---- the screen ---------------------------------------------------------------------------------------------------

    final Desktop pad;
    /** Everything under the toolbar: the grid, and over it the +, any card, and the search and favourites. */
    final JLayeredPane desk=new JLayeredPane(){
        @Override public void doLayout() {
            int w=getWidth(),h=getHeight();
            // The search and the favourites float over the icons (decision 128). The card and every page at once stay
            // above that band, as they did when the band had a strip of the window of its own: the dock is never under them.
            int band=foot==null||!foot.isVisible()?0:foot.getPreferredSize().height;
            scroll.setBounds(0,0,w,h);
            if(foot!=null)foot.setBounds(0,Math.max(0,h-band),w,band);
            if(pages!=null)pages.setBounds(0,0,w,Math.max(0,h-band));
            int bottom=Math.max(18,pad.statusStrip==null?18:pad.statusStrip.getPreferredSize().height+18);
            Dimension p=plus.getPreferredSize();plus.setBounds(w-p.width-30,Math.max(0,h-band-p.height-bottom),p.width,p.height);
            folder.setBounds(0,0,w,Math.max(0,h-band));folder.doLayout();
        }
    };
    final Icons grid;
    final JScrollPane scroll;
    final JButton plus;
    final JTextField search=new JTextField();
    final JButton overviewButton;
    final Dock dock=new Dock();
    /**
     * The dock in a strip that scrolls across where it holds more than the window shows (the owner, 2026-10-18: "make sure we
     * can add multiple elements to the dock and that we can browse them horizontally"; decision 190): the wheel, a drag of its
     * thin bar, or the keys from icon to icon.
     */
    final JScrollPane dockScroll=dockScroll(dock);
    /** The row the dock stands in, shown or not (decision 47); and the foot it and the search float in, over the icons. */
    private JPanel dockRow;
    /** The search and the favourites, floating over the icons (decision 128). A point on the paper beside them is the icon under it. */
    JPanel foot;
    /** Every page at once, zoomed out (decision 45). */
    final DesktopPages pages;
    /** Things made with + on a page other than the centre one, by id: they stand on that page, once they are read. */
    private final Map<String,int[]> placeNext=new HashMap<>();
    final DesktopFolder folder;
    final Carry carry=new Carry();
    final DesktopMotion motion;
    /** What was found, while something is typed in the search: in the grid's place, where the eye already is. */
    private final JPanel results=new ResultList();
    /** Home's lines, what the dock holds, and the favourites not in it (decision 190). */
    private List<NoteStore.Branch> homeLines=List.of(),docked=List.of(),beyond=List.of();
    /** Whether there is any favourite, for the Favourites icon on Home. */
    private boolean anyFavourite;
    /** Where each place was pinned on Home at the last move there - its cell and its page - by id, as last read (see placeCells). */
    private Map<String,Integer> placeCells=Map.of(),placePages=Map.of();
    /** Whether the archive and the bin are on Home, and how many things wait in each, as last read (decision 41). */
    private boolean away=true;private int archived,binned;
    /** Which read is the newest, whether Home has been drawn once, and whether something changed while it could not be. */
    private int reads;private boolean drawn,stale;
    private final javax.swing.Timer looking;

    DesktopHome(Desktop pad) {
        super(new BorderLayout());this.pad=pad;pad.frame.addWindowListener(lost);
        setBackground(DesktopUi.PAPER);putClientProperty(KEEPS,Desktop.library());
        grid=new Icons(this,Where.HOME);motion=new DesktopMotion((x,y)->grid.scrollBy(x,y));Toolkit.getDefaultToolkit().addAWTEventListener(stopOnPress,AWTEvent.MOUSE_EVENT_MASK);grid.putClientProperty(KEEPS,Desktop.library());
        scroll=new JScrollPane(grid);scroll.setBorder(null);scroll.getViewport().setBackground(DesktopUi.PAPER);
        // Home is one surface of pages, scrolled by pixels in both directions (decision 126). The bars stay hidden:
        // the wheel, a drag and a carry at the edge move it.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        plus=plusButton(()->Things.HOME);
        folder=new DesktopFolder(this);folder.setVisible(false);
        pages=new DesktopPages(this);pages.setVisible(false);
        desk.add(scroll,JLayeredPane.DEFAULT_LAYER);desk.add(plus,JLayeredPane.PALETTE_LAYER);desk.add(folder,JLayeredPane.MODAL_LAYER);desk.add(pages,JLayeredPane.POPUP_LAYER);
        add(desk);
        // A right-click on Home itself, not on an icon: what can be made here, and syncing everything.
        MouseAdapter room=new MouseAdapter(){
            public void mousePressed(MouseEvent e){if(e.isPopupTrigger())pad.roomMenu(Desktop.library(),e.getComponent(),e.getX(),e.getY());}
            public void mouseReleased(MouseEvent e){if(e.isPopupTrigger())pad.roomMenu(Desktop.library(),e.getComponent(),e.getX(),e.getY());}
        };
        grid.addMouseListener(room);scroll.getViewport().addMouseListener(room);
        turning();

        // The foot: the search with the overview button beside it, then the dock.
        search.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,Say.t("Search notes, folders and files"));
        search.putClientProperty(com.formdev.flatlaf.FlatClientProperties.TEXT_FIELD_LEADING_ICON,new com.formdev.flatlaf.icons.FlatSearchIcon());
        search.putClientProperty(com.formdev.flatlaf.FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON,true);
        search.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"arc:999;margin:7,14,7,14");
        search.setToolTipText(Say.t("Search everything (Ctrl+F)"));search.getAccessibleContext().setAccessibleName("Search notes, folders and files");
        search.setColumns(34);
        looking=new javax.swing.Timer(200,e->look());looking.setRepeats(false);
        search.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){if(!e.isTemporary()&&search.getText().isBlank())showFoot();}});
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){looking.restart();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){looking.restart();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){looking.restart();}
        });
        // Down from the field goes into what was found; Enter opens the first of it.
        search.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"),"into");
        search.getActionMap().put("into",new AbstractAction(){public void actionPerformed(ActionEvent e){Component first=firstResult();if(first!=null)first.requestFocusInWindow();}});
        search.addActionListener(e->{if(firstResult() instanceof Row row)pad.openFound(row.thing,search.getText());});
        overviewButton=new JButton(overviewIcon());
        overviewButton.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        overviewButton.setFocusPainted(false);overviewButton.setMargin(new Insets(7,9,7,9));
        overviewButton.setToolTipText(Say.t("What is open (Ctrl+Tab)"));overviewButton.getAccessibleContext().setAccessibleName("Show the notes and folders that are open");
        overviewButton.addActionListener(e->pad.overview.up(false));
        JPanel searchRow=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0));searchRow.setOpaque(false);searchRow.add(search);searchRow.add(overviewButton);
        dockRow=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));dockRow.setOpaque(false);dockRow.add(dockScroll);
        // Only as wide as the search and the favourites, centred, and only those take a click. The paper beside them, and
        // the icons under them, are the grid's (decision 128).
        foot=new JPanel(null){
            @Override public void doLayout() {
                int w=getWidth(),y=8;
                if(searchRow.isVisible()){Dimension s=searchRow.getPreferredSize();searchRow.setBounds(Math.max(0,(w-s.width)/2),y,Math.min(w,s.width),s.height);y+=s.height+10;}
                if(dockRow.isVisible()){Dimension d=dockRow.getPreferredSize();dockRow.setBounds(Math.max(0,(w-d.width)/2),y,Math.min(w,d.width),d.height);}
            }
            @Override public Dimension getPreferredSize() {
                int w=0,h=0;boolean any=false;
                if(searchRow.isVisible()){Dimension s=searchRow.getPreferredSize();w=Math.max(w,s.width);h+=s.height;any=true;}
                if(dockRow.isVisible()){Dimension d=dockRow.getPreferredSize();w=Math.max(w,d.width);if(h>0)h+=10;h+=d.height;any=true;}
                return any?new Dimension(w,h+20):new Dimension(0,0);
            }
            @Override public boolean contains(int x,int y){return over(searchRow,x,y)||over(dockRow,x,y);}
            private boolean over(Component c,int x,int y){return c.isVisible()&&c.getBounds().contains(x,y);}
        };
        foot.setOpaque(false);foot.add(searchRow);foot.add(dockRow);
        desk.add(foot,JLayeredPane.PALETTE_LAYER);
        showFoot();paintRoom();
        // Wider or narrower, the dock may hold more or fewer, and the Favourites icon come or go with them: the grid is drawn
        // again only when it does, and the Favourites card when it is the one up.
        dockRow.addComponentListener(new ComponentAdapter(){int last=-1;public void componentResized(ComponentEvent e){
            int now=dockRoom(dockRow.getWidth()-32);if(now==last||!drawn)return;last=now;
            fillDock();
            boolean shows=!grid.tiles.isEmpty()&&grid.tiles.get(0).thing.kind==NoteStore.Branch.Kind.FAVOURITES;
            if(shows==overflow().isEmpty())fillGrid();
            if(folder.amongFavourites())refresh();
        }});
    }

    private final AWTEventListener stopOnPress=event->{if(event instanceof MouseEvent e&&e.getID()==MouseEvent.MOUSE_PRESSED&&e.getSource() instanceof Component c&&SwingUtilities.isDescendingFrom(c,this))stopMotion();};
    private final WindowAdapter lost=new WindowAdapter(){public void windowDeactivated(WindowEvent e){carry.cancel();motion.stop();}public void windowClosed(WindowEvent e){stop();}};
    private void stopMotion(){motion.stop();}
    void stop(){carry.cancel();motion.stop();looking.stop();pad.frame.removeWindowListener(lost);Toolkit.getDefaultToolkit().removeAWTEventListener(stopOnPress);}
    NoteStore store(){return pad.store;}

    /**
     * Favourites and the search shown or not, as Settings and Home's menu say (decision 47). The overview's button stays
     * beside where the search was: it is the way to what is open. They float, so hiding one gives its room back to the icons.
     */
    void showFoot() {
        // On the centre page only (decision 47).
        boolean centre=(grid==null||grid.onCentre())&&(pages==null||!pages.isVisible());
        foot.setVisible(centre);
        search.setVisible(centre&&pad.showing(Desktop.SHOW_SEARCH));
        dockRow.setVisible(centre&&pad.showing(Desktop.SHOW_DOCK));
        revalidate();repaint();
    }

    /**
     * How Home is scrolled (decision 126): the wheel up and down, Shift with it sideways, by pixels, and it rests wherever
     * it is let go, between pages as well as on one; Ctrl with the wheel out to every page; the empty room dragged, as a
     * map is; Page Up and Page Down, Ctrl with them sideways, a page at a time; and a double-click on the empty room, or
     * Ctrl+Home, back to the centre page.
     */
    private void turning() {
        MouseWheelListener wheel=new MouseWheelListener(){
            public void mouseWheelMoved(MouseWheelEvent e) {
                if(folder.isOpen()||carry.carrying())return;
                if(e.isControlDown()){if(e.getWheelRotation()>0)pages.up();e.consume();return;}
                double d=e.getPreciseWheelRotation()*48;
                boolean precise=e.getPreciseWheelRotation()!=e.getWheelRotation();
                motion.wheel(e.isShiftDown()?d:0,e.isShiftDown()?0:d,precise);
                e.consume();
            }
        };
        grid.addMouseWheelListener(wheel);scroll.addMouseWheelListener(wheel);
        MouseAdapter drag=new MouseAdapter(){
            java.awt.Point from,origin;int axis;
            public void mousePressed(MouseEvent e){motion.stop();axis=HomeMotion.NONE;origin=from=SwingUtilities.isLeftMouseButton(e)?e.getLocationOnScreen():null;}
            public void mouseDragged(MouseEvent e) {
                if(from==null||carry.carrying())return;
                if(axis==HomeMotion.NONE){if(origin.distance(e.getLocationOnScreen())<6)return;axis=HomeMotion.axis(origin.x-e.getXOnScreen(),origin.y-e.getYOnScreen());}
                grid.scrollBy(axis==HomeMotion.HORIZONTAL?from.x-e.getXOnScreen():0,axis==HomeMotion.VERTICAL?from.y-e.getYOnScreen():0);
                from=e.getLocationOnScreen();
            }
            public void mouseReleased(MouseEvent e){from=null;}
            public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==2)grid.returnCentre();}
        };
        grid.addMouseListener(drag);grid.addMouseMotionListener(drag);
        InputMap keys=getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);ActionMap does=getActionMap();
        String[][] turns={{"PAGE_UP","0","-1"},{"PAGE_DOWN","0","1"},{"control PAGE_UP","-1","0"},{"control PAGE_DOWN","1","0"}};
        for(String[] one:turns) {
            keys.put(KeyStroke.getKeyStroke(one[0]),"turn "+one[0]);
            int dx=Integer.parseInt(one[1]),dy=Integer.parseInt(one[2]);
            does.put("turn "+one[0],new AbstractAction(){public void actionPerformed(ActionEvent e){if(!folder.isOpen())grid.turn(dx,dy);}});
        }
        keys.put(KeyStroke.getKeyStroke("control HOME"),"centre");
        does.put("centre",new AbstractAction(){public void actionPerformed(ActionEvent e){if(!folder.isOpen())grid.returnCentre();}});
    }

    /** A page come into view: the foot as it is on that page, and the bar saying which page it is when it is not the centre. */
    void pageShown() {
        showFoot();
        pad.status.setToolTipText(null);
        pad.status.setText(grid.onCentre()?" ":Say.t(DesktopPages.said(grid.pageX,grid.pageY))+"  ·  "+Say.t("double-click to go back"));
    }

    /** Something just made with + on the page in view: it stands on that page once Home has read it (decision 46). */
    void placeNew(String id){if(!grid.onCentre())placeNext.put(id,new int[]{grid.pageX,grid.pageY});}

    /** Home's room in Home's own colour, at Home's own strength (decision 107), as the phone's Home is (decision 44). */
    void paintRoom() {
        Color paper=DesktopLook.wash(pad.homeColour(),DesktopUi.PAPER,0.12f,0.72f,pad.homeTone());
        setBackground(paper);scroll.getViewport().setBackground(paper);
        repaint();
    }

    /** Whether Home has been drawn from the notebook at least once: the first frame waits for it. */
    boolean drawn(){return drawn;}
    /** Whether the dock shows a thing, as it is drawn now: the menu offers to take it out. */
    boolean inDock(String id){return docked().shows(id);}
    /** Whether something is typed in the search. */
    boolean searching(){return !search.getText().isBlank();}

    // ---- reading it again ---------------------------------------------------------------------------------------------

    /**
     * Home read again, and the card and the dock with it: after anything changes, here or on another device. What the
     * reader is looking at does not move - each grid keeps where it was scrolled to and which icon has the keyboard, and
     * an open card stays open on the collection it shows - unless that collection has gone, which is then said. Not
     * while a thing is carried or a name is typed: it is read again once they are done.
     */
    /** Only the card read and drawn again, under a carry, when Home itself waits for the carry to end. */
    void refreshCard() {
        if(!folder.isOpen())return;
        List<NoteStore.Branch> path=folder.path();
        pad.disk.submit(()->{Map<String,Object> got=new HashMap<>();folder.read(path,got);return got;},got->{if(folder.isOpen())folder.fill(path,got);},pad::failed);
    }

    void refresh() {
        if(carry.carrying()||folder.naming()){stale=true;return;}
        stale=false;int read=++reads;
        List<NoteStore.Branch> path=folder.path();
        pad.disk.submit(()->{
            Map<String,Object> got=new HashMap<>();
            got.put("home",store().contents(Things.HOME));
            keptOutOnce(pad.context,store());
            // Everything in a shared folder shared (decision 195): what that put in one for real goes now.
            List<String> made=store().intoSharedFolders();
            if(!made.isEmpty()&&!pad.offline)pad.network.submit(()->{for(String id:made)Post.send(pad.context,store(),pad.keys,NoteStore.Branch.Kind.PAGE,id);return null;},v->{},e->{});
            got.put("docked",store().dock());got.put("beyond",store().favouritesBeyondDock());
            got.put("favourites",store().keptCount()>0);
            got.put("cells",placeCells(pad.context));got.put("pages",placePages(pad.context));
            // The archive and the bin, and how many things wait in each, for the count on each (decision 41).
            boolean onHome=pad.awayOnHome();got.put("away",onHome);
            // What is temporary and whose time has come goes first, so it is never drawn once more (decision 71).
            store().expire(System.currentTimeMillis());
            got.put("archived",store().awayCount(false));got.put("binned",store().awayCount(true));got.put("temp",store().temporaryCount());
            // Whether anything in Temp is new, for its tile (the owner, 2026-10-14), and what is new anywhere, for the bar.
            got.put("tempNew",store().tempHasNew());got.put("newHere",store().whatIsNew().size());
            // And how many things wait for an answer in Shared with me, for the count it wears (decision 109).
            got.put("asking",store().waitingCount());
            // What a code accepted here is bringing, standing on Home until it comes (decision 73).
            // And what is coming to Home on its own, where Settings shows files shared with you there (decision 94).
            List<NoteStore.Branch> waits=new ArrayList<>(store().waitingOnHome());waits.addAll(store().coming(Things.HOME));
            got.put("waiting",waits);
            folder.read(path,got);
            return got;
        },got->{
            if(read!=reads)return;
            if(carry.carrying()||folder.naming()){stale=true;return;}
            @SuppressWarnings("unchecked") List<NoteStore.Branch> lines=(List<NoteStore.Branch>)got.get("home");
            @SuppressWarnings("unchecked") List<NoteStore.Branch> inDock=(List<NoteStore.Branch>)got.get("docked");
            @SuppressWarnings("unchecked") List<NoteStore.Branch> past=(List<NoteStore.Branch>)got.get("beyond");
            homeLines=lines;docked=inDock;beyond=past;anyFavourite=Boolean.TRUE.equals(got.get("favourites"));
            @SuppressWarnings("unchecked") Map<String,Integer> cells=(Map<String,Integer>)got.get("cells");placeCells=cells;
            @SuppressWarnings("unchecked") Map<String,Integer> onPages=(Map<String,Integer>)got.get("pages");placePages=onPages;
            away=(Boolean)got.get("away");archived=(Integer)got.getOrDefault("archived",0);binned=(Integer)got.getOrDefault("binned",0);
            temporary=(Integer)got.getOrDefault("temp",0);tempNew=Boolean.TRUE.equals(got.get("tempNew"));
            pad.newHere((Integer)got.getOrDefault("newHere",0));asking=(Integer)got.getOrDefault("asking",0);unread=(Integer)got.getOrDefault("unread",0);
            @SuppressWarnings("unchecked") List<NoteStore.Branch> coming=(List<NoteStore.Branch>)got.getOrDefault("waiting",List.of());waiting=coming;
            fillDock();fillGrid();folder.fill(path,got);
            drawn=true;
        },e->{drawn=true;pad.failed(e);});
    }

    /** The dock as it is drawn now: everything in it, however many, scrolled across where they do not fit (decision 190). */
    private Docked docked(){return new Docked(docked,beyond);}
    /** The favourites not in the dock. */
    List<NoteStore.Branch> overflow(){return docked().rest();}

    /**
     * Home's grid drawn again where it stands: the things, and the places among them - the Favourites icon while the dock
     * cannot show every favourite, the archive and the bin unless they are switched off - each where it was put (see
     * Layout.home). With nothing of the owner's yet, the words saying what to do are over the places.
     */
    private void fillGrid() {
        List<NoteStore.Branch> lines=new ArrayList<>();
        // The Favourites icon whenever there is a favourite: it lists every one, in their order (decision 74).
        // Home's places, each on Home unless switched off in Home's menu (decision 78); Favourites while there is one.
        if(anyFavourite&&pad.onHome(NoteStore.FAVOURITES))lines.add(placed(coloured(favouritesPlace())));
        lines.addAll(homeLines);
        // Recent is the list down the right of the window on the PC (decision 86).
        NoteStore.Branch inTemp=tempPlace(temporary);inTemp.fresh=tempNew;
        for(NoteStore.Branch one:new NoteStore.Branch[]{inTemp,sharedPlace(asking),archivePlace(archived),binPlace(binned)})
            if(pad.onHome(one.id))lines.add(placed(coloured(one)));
        lines.addAll(waiting);
        grid.fill(lines,homeLines.isEmpty()?"Nothing here yet. Click + to make a note or a folder.":null);
        wantPreviews(lines);
        placeMade();
        // The page in view gone empty - its last icon moved or put away - is gone: the centre page instead.
        if(!carry.carrying()&&!Layout.active(grid.spots()).contains(List.of(grid.pageX,grid.pageY)))grid.showPage(0,0);
        if(pages.isVisible())pages.repaint();
    }
    /** Those made with + on another page than the centre, put there now they are read: the first free cell of that page. */
    private void placeMade() {
        if(placeNext.isEmpty())return;
        for(Tile one:new ArrayList<>(grid.tiles)) {
            int[] on=placeNext.remove(one.thing.id);
            if(on==null||one.thing.cell>=0)continue;
            Map<String,Layout.Spot> others=new java.util.LinkedHashMap<>(grid.spots());others.remove(one.thing.id);
            Layout.Spot at=Layout.freeOn(others,on[0],on[1],grid.columns(),grid.rows());
            one.thing.cell=at.cell();one.thing.page=at.page();grid.rearrange();
            NoteStore.Branch line=one.thing;Map<String,Layout.Spot> spot=Map.of(line.id,at);
            pad.disk.submit(()->{store().placeOnPages(List.of(line),spot);return null;},done->{},e->{});
        }
    }
    /** A place in the colour chosen for it on this PC (decision 81), at the strength chosen for it (decision 107). */
    private NoteStore.Branch coloured(NoteStore.Branch place) {
        NoteStore.Branch worn=new NoteStore.Branch(place.kind,place.id,place.parent,place.name,place.detail,0,0,true,pad.placeColour(place.id));
        worn.tone=pad.placeTone(place.id);return worn;
    }
    /** A place with the page and the cell this PC keeps for it. */
    private NoteStore.Branch placed(NoteStore.Branch place) {
        Integer at=placeCells.get(place.id);place.cell=at==null?Layout.NONE:at;
        Integer on=placePages.get(place.id);place.page=on==null?Layout.NO_PAGE:on;
        return place;
    }
    private void fillDock(){dock.fill(docked().shown());}

    /** The Favourites collection, as the thing its icon stands for: a place, like the archive, not a collection. */
    static NoteStore.Branch favouritesPlace() {
        return new NoteStore.Branch(NoteStore.Branch.Kind.FAVOURITES,NoteStore.FAVOURITES,Sharing.EVERYTHING,"Favourites","",0,0,true);
    }
    /** The archive, as Home shows it, with how many things are in it - which is all its line says (decision 41). */
    static NoteStore.Branch archivePlace(int count){return place(NoteStore.Branch.Kind.ARCHIVE,NoteStore.ARCHIVE,"Archive",count);}
    /** Tools, Temp and Recent, as Home shows them (decisions 71 and 72). */
    static NoteStore.Branch toolsPlace(){return place(NoteStore.Branch.Kind.TOOLS,NoteStore.TOOLS,"Tools",0);}
    static NoteStore.Branch tempPlace(int count){return place(NoteStore.Branch.Kind.TEMP,NoteStore.TEMP,"Temp",count);}
    static NoteStore.Branch recentPlace(){return place(NoteStore.Branch.Kind.RECENT,NoteStore.RECENT,"Recent",0);}
    /** Shared with me: where files shared with you on their own show (decision 94). */
    static NoteStore.Branch sharedPlace(){return sharedPlace(0);}
    /** The same, with how many things wait in it for an answer, which it wears as Temp wears its count (decision 109). */
    static NoteStore.Branch sharedPlace(int waiting){return place(NoteStore.Branch.Kind.SHARED,NoteStore.SHARED,NoteStore.SHARED_WITH_ME,waiting);}
    /** How many things are temporary, as last read, how many wait for an answer, and what accepted codes are bringing. */
    private int temporary,asking,unread;private boolean tempNew;private List<NoteStore.Branch> waiting=List.of();
    /** Whether one of the four is kept in Tools rather than on Home (decision 72): yes until it is shown on Home. */
    boolean inTools(String id){return !"false".equals(pad.context.getSharedPreferences("settings",0).getString(id+"InTools","true"));}
    void keepInTools(String id,boolean in){pad.context.getSharedPreferences("settings",0).edit().putString(id+"InTools",String.valueOf(in)).apply();refresh();}
    /** What Tools holds, as its card lists it. Read on the disk. */
    List<NoteStore.Branch> inTools(int archived,int binned,int temporary) {
        List<NoteStore.Branch> held=new ArrayList<>();
        for(NoteStore.Branch one:new NoteStore.Branch[]{archivePlace(archived),binPlace(binned),tempPlace(temporary),recentPlace()})if(inTools(one.id))held.add(one);
        return held;
    }
    /** Each place's glyph from the Lucide set, as the phone draws it. */
    static String placeGlyph(NoteStore.Branch.Kind kind) {
        return switch(kind) {
            case BIN -> BIN_ICON;
            case TOOLS -> "toolbox";
            case TEMP -> "timer";
            case RECENT -> "clock-3";
            case WAITING -> "hourglass";
            case SHARED -> "inbox";
            case MESSAGES -> "mail";
            default -> ARCHIVE_ICON;
        };
    }
    /** The bin, as Home shows it, with how many things are in it. */
    static NoteStore.Branch binPlace(int count){return place(NoteStore.Branch.Kind.BIN,NoteStore.BIN,"Bin",count);}
    private static NoteStore.Branch place(NoteStore.Branch.Kind kind,String id,String name,int count) {
        return new NoteStore.Branch(kind,id,Sharing.EVERYTHING,name,count<=0?"":count==1?"1 thing":count+" things",0,0,true);
    }
    /** The archive's and the bin's icons in the Lucide set, as the phone draws them (the set's "trash" has lines in it). */
    static final String ARCHIVE_ICON="archive",BIN_ICON="trash";

    // ---- opening ------------------------------------------------------------------------------------------------------

    /** An icon clicked or Entered: a note opens, a collection opens its card - in the same card from inside one - a file opens. */
    void opened(NoteStore.Branch thing,Where where) {
        switch(thing.kind) {
            case PAGE: pad.save(()->pad.open(thing.id));return;
            case FILE: openFile(thing);return;
            case FAVOURITES: folder.openFavourites();return;
            case ARCHIVE: case BIN: case TOOLS: case TEMP: case RECENT: case SHARED: folder.openPlace(thing);return;
            case WAITING: waiting(thing);return;
            case COLLECTION: case BOOK:
                if(where==Where.CARD)folder.into(thing);
                // A favourite opens where it really lives, with ‹ going up from there.
                else if(where==Where.FAVOURITES&&folder.isOpen()&&folder.lists())folder.into(thing);
                else if(where==Where.FAVOURITES||where==Where.DOCK)folder.openAt(thing.id);
                else folder.open(thing);
                return;
            default:
        }
    }

    /**
     * A file opened in the program it opens in (see DesktopApps), and no longer new: to work in where this PC may change it,
     * its saves coming back; else read only (see DesktopOpening).
     */
    void openFile(NoteStore.Branch file) {
        withHeld(file,held->DesktopOpening.open(pad,held));
    }

    /**
     * What the Share button means while Home is showing: the open card's collection, or else the icon that has the
     * keyboard. Null when neither is a note or a collection.
     */
    NoteStore.Branch chosen() {
        // The icon that had the keyboard last: pressing Share has just taken the keyboard to the button.
        Tile last=lastTile;
        if(last!=null&&last.isShowing()&&last.thing.scope()!=null&&(!folder.isOpen()||SwingUtilities.isDescendingFrom(last,folder)))return last.thing;
        return folder.shown();
    }
    /** The icon that had the keyboard last, for chosen(). */
    private Tile lastTile;

    /** The icon of this thing, on Home or in the card, given the keyboard: where the reader was when they went to it. */
    void focusThing(String id) {
        if(id==null)return;
        Icons in=folder.isOpen()?folder.grid:grid;
        for(Tile one:in.tiles)if(one.thing.id.equals(id)){one.requestFocusInWindow();return;}
    }

    /** The keyboard to the first icon on Home, or to the + where there is none: where a window opening on Home starts. */
    void focusFirst(){Tile first=grid.first();if(first!=null)first.requestFocusInWindow();else plus.requestFocusInWindow();}

    /** The keyboard to the search, with what is there chosen so typing replaces it. */
    void focusSearch() {
        // Hidden in Settings, it comes for as long as it is used, and goes again once it is emptied and left (decision 47).
        if(!search.isVisible()){search.setVisible(true);revalidate();}
        search.requestFocusInWindow();search.selectAll();
    }

    /**
     * Esc on Home: out of the card one level, then the search emptied. Whether it did something; a carried thing and a
     * name being typed take Esc for themselves before it gets here.
     */
    boolean escape() {
        if(folder.isOpen()){folder.up();return true;}
        if(searching()){search.setText("");search.requestFocusInWindow();return true;}
        return false;
    }

    // ---- making -------------------------------------------------------------------------------------------------------

    /**
     * The round +, with no word on it, as the phone's is, named for anybody who cannot see it. It offers the two things
     * there are, for where it is: Home's makes them on Home, a card's in that collection.
     */
    JButton plusButton(Supplier<String> where) {
        JButton plus=round();
        plus.setToolTipText(Say.t("New note or folder, or one from another device"));plus.getAccessibleContext().setAccessibleName("New");
        plus.addActionListener(e->{
            JPopupMenu menu=plusMenu(where.get());
            Dimension m=menu.getPreferredSize();menu.show(plus,plus.getWidth()-m.width-6,-m.height-2);
        });
        return plus;
    }
    /** The + as it is drawn, round and with no word, its menu its own: Home's, a card's, and the private screen's (decision 114). */
    static JButton round() {
        JButton plus=new JButton(){
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int d=Math.min(getWidth(),getHeight())-8,x=(getWidth()-d)/2,y=(getHeight()-d)/2-1;
                // No shadow: drawn inside the button's own square it was cut off at the foot (the owner, 2026-10-01).
                g.setColor(getModel().isPressed()?new Color(34,74,53):getModel().isRollover()?new Color(42,88,64):DesktopUi.ACCENT);g.fillOval(x,y,d,d);
                if(ringed(this)){g.setColor(DesktopUi.mix(DesktopUi.ACCENT,Color.WHITE,0.55f));g.setStroke(new BasicStroke(2f));g.drawOval(x-3,y-3,d+5,d+5);}
                g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int cx=x+d/2,cy=y+d/2,arm=d/5;g.drawLine(cx-arm,cy,cx+arm,cy);g.drawLine(cx,cy-arm,cx,cy+arm);
                g.dispose();
            }
        };
        plus.setContentAreaFilled(false);plus.setBorderPainted(false);plus.setFocusPainted(false);plus.setOpaque(false);
        plus.setPreferredSize(new Dimension(64,64));plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return plus;
    }

    /**
     * What a + offers where it is: a note and a collection made there; and on every +, something from another device - a
     * note or a collection someone shows the code of, scanned or pasted - which arrives on Home (the owner: "this is key").
     */
    JPopupMenu plusMenu(String where) {
        JPopupMenu menu=new JPopupMenu();
        JMenuItem note=DesktopMenus.item("Note");note.addActionListener(a->pad.newNoteIn(where));menu.add(note);
        JMenuItem collection=DesktopMenus.item("Folder");collection.addActionListener(a->newCollection(where));menu.add(collection);
        // An album's + adds pictures (decision 217): several of this PC's at once, or one from the camera, into the folder.
        if(!NoteStore.home(where)&&!Desktop.takesNew(where)&&store().album(where)) {
            JMenuItem photos=DesktopMenus.item("Add photos…");photos.addActionListener(a->pad.addPhotos(where,null));menu.add(photos);
            JMenuItem camera=DesktopMenus.item("Take a photo…");camera.addActionListener(a->pad.takePhotoInto(where));menu.add(camera);
        }
        // And files from this PC, pictures and anything else, wherever there is a + (decision 87).
        JMenuItem files=DesktopMenus.item("From this device…");files.addActionListener(a->pad.fromThisDevice(where));menu.add(files);
        menu.addSeparator();JMenuItem other=DesktopMenus.item("From another device…");other.addActionListener(a->pad.scanCode(null));menu.add(other);
        return menu;
    }

    /**
     * A new collection where the + was pressed, made as Untitled, and its card opened with the name ready to be typed
     * over, since nothing else can say what it is called.
     */
    void newCollection(String asked) {
        // From a place's +: made on Home and put in the place at once (decision 87).
        String place=Desktop.takesNew(asked)?asked:null,where=place!=null?Things.HOME:asked;
        pad.save(()->pad.disk.submit(()->{NoteStore.Shelf made=store().addCollectionIn(where,"");if(place!=null)pad.intoPlaceNow(NoteStore.Branch.Kind.COLLECTION,made.id,place);return made;},made->{
            if(NoteStore.home(where)&&Desktop.shownOnHome(place,store().tempOnHome()))placeNew(made.id);
            NoteStore.Branch line=new NoteStore.Branch(NoteStore.Branch.Kind.COLLECTION,made.id,NoteStore.home(where)?Sharing.EVERYTHING:where,made.name,"Empty",0,0,true);
            pad.showHome();
            if(NoteStore.home(where))folder.open(line,true);
            else if(folder.isOpen()&&where.equals(folder.id()))folder.into(line,true);
            else folder.openAt(line.id,true);
            pad.refresh();
        },e->pad.status.setText(Say.t("Could not make that folder. Nothing was changed."))));
    }

    // ---- files on Home and in collections ------------------------------------------------------------------------------

    /**
     * A file's menu, where it is held, in the phone's order: what a click does first, then what else can be done with it,
     * and deleting it last, under the line, since that is the one thing here that cannot be taken back.
     */
    JPopupMenu fileMenu(NoteStore.Branch file) {
        JPopupMenu menu=new JPopupMenu();
        // Who sent it, first, opening the box that says where every device stands with it (the owner, 2026-10-05).
        JMenuItem from=DesktopMenus.item();from.addActionListener(e->pad.shareThing(file));from.setVisible(false);menu.add(from);
        JPopupMenu.Separator under=new JPopupMenu.Separator();under.setVisible(false);menu.add(under);
        pad.disk.submit(()->store().standing(file.id),standing->{
            if(standing.from.isEmpty())return;
            from.setText(Say.t("From {0}",standing.fromName)+" · "+Desktop.shortWhen(standing.at));from.setVisible(true);under.setVisible(true);
            if(menu.isVisible())menu.pack();
        },pad::failed);
        // Linked to a file on this PC (Share a file through Mininotes): a menu of its own (the owner, 2026-10-16: "we should
        // have distinct functions for linked files like these"), first where it lives, said and not done.
        boolean linked=pad.onDisk!=null&&pad.onDisk.linkedFile(file.id);
        java.nio.file.Path there=linked?pad.onDisk.pathOf(file.id):null;
        if(there!=null) {
            java.nio.file.Path in=there.getParent();
            String where=in==null||in.getFileName()==null?there.toString():in.getFileName()+"\\"+there.getFileName();
            JMenuItem lives=DesktopMenus.plain(Say.t("Linked to {0}",where));lives.setEnabled(false);lives.setToolTipText(there.toString());menu.add(lives);
            menu.addSeparator();
        }
        // Changed on this PC and in Mininotes, and waiting to be told which version it keeps: that first.
        if(pad.onDisk!=null&&DesktopOnDisk.ASK.equals(pad.onDisk.state(file.id)))item(menu,"Which version to keep…",()->DesktopOnDisk.askWhichFile(pad,file.id,file.name));
        // One Open, named by the program it opens in, and Open with… to change that program (the owner, 2026-10-15: "we
        // should have only one open, once open we can decide to modify or not"). Open's own symbol, its words the program's.
        JMenuItem open=DesktopMenus.item("Open");open.setText(DesktopMenus.shortly(DesktopApps.openLine(file.name)));open.addActionListener(e->openFile(file));menu.add(open);
        item(menu,"Open with…",()->withHeld(file,held->DesktopOpening.openWith(pad,held)));
        if(linked) {
            item(menu,"Show in Explorer",()->DesktopOnDisk.showInExplorer(pad,file.id));
            // One switch, one meaning: kept in step with the file on this PC, or not, and on again as easily.
            boolean on=!pad.onDisk.syncOff(file.id);
            menu.add(Desktop.ticked("Synced with the file on this PC",on,()->pad.disk.submit(()->{pad.onDisk.syncing(file.id,!on);return null;},
                done->{pad.status.setText(on?Say.t("Not synced any more. {0} on this PC stays as it is",file.name):Say.t("Synced again with {0} on this PC",file.name));
                    pad.refresh();if(!on)pad.onDisk.soon();},pad::failed)));
        }
        // Written in like a note, by whoever may (decision 93): offered once the notebook says this PC may.
        JMenuItem rename=DesktopMenus.item("Rename…");rename.addActionListener(e->renameFile(file));rename.setVisible(false);menu.add(rename);
        JMenuItem replace=DesktopMenus.item("Replace with another file…");replace.addActionListener(e->replaceFile(file));replace.setVisible(false);menu.add(replace);
        pad.disk.submit(()->store().mayChangeFile(file.id),may->{rename.setVisible(may);replace.setVisible(may);if(menu.isVisible())menu.pack();},pad::failed);
        // On the clipboard without opening it (the owner, 2026-10-18; decision 198): a picture as a picture, any file as a file.
        item(menu,"Copy",()->withHeld(file,pad::copyFile));
        // On Minima: one line, saying how far its stamp is, opening the one window for it (decisions 199, 224 and 253).
        DesktopStamp.menuLine(pad,menu,file.id,()->withHeld(file,held->DesktopStamp.thing(pad,held.id,false)));
        item(menu,"Save on my computer…",()->withHeld(file,pad::saveCopy));
        item(menu,"Send to a device…",()->withHeld(file,held->pad.fileCards.send(held)));
        item(menu,"Put in a note…",()->intoNote(file));
        item(menu,"Move to…",()->moveFile(file));
        // Shared like a note, with its own people and roles (decision 92): the same box a note's Share… opens.
        item(menu,"Share…",()->pad.shareThing(file));
        // A file in the places a note can be in (decision 87): Favourites, Temp, the archive and the bin.
        JMenuItem star=DesktopMenus.item("Add to favourites");menu.add(star);
        JMenuItem temporary=DesktopMenus.item("Temporary…");temporary.addActionListener(e->pad.temporaryBox(file));menu.add(temporary);
        pad.disk.submit(()->store().favourite(NoteStore.Branch.Kind.FILE,file.id),starred->{
            star.setText(Say.t(starred?"Remove from favourites":"Add to favourites"));
            star.addActionListener(e->pad.disk.submit(()->{store().keepToHand(NoteStore.Branch.Kind.FILE,file.id,!starred);return null;},
                done->{pad.status.setText(Say.t(starred?"Not a favourite":"A favourite"));pad.refresh();},pad::failed));
        },pad::failed);
        menu.addSeparator();
        if(linked) {
            // Out of Mininotes on this PC, never deleting: the copy to the bin, where putting it back links it again; the file
            // on this PC as it is; the other devices keep theirs (the owner, 2026-10-16: "only this PC"; "in bin so the user
            // can relink it easily, but it has to be clear that it is not deleting").
            item(menu,"Remove from Mininotes…",()->{
                if(!DesktopUi.confirm(pad.frame,Say.t("Remove “{0}” from Mininotes on this PC?",file.name),
                    Say.t("Nothing is deleted. {0} on this PC stays where it is, as it is. Mininotes' copy goes to the bin: put it back from there to link it again. Your other devices keep theirs.",file.name),
                    Say.t("Remove from Mininotes"),false))return;
                pad.putAway(file,true);
            });
            return menu;
        }
        item(menu,"Archive",()->pad.putAway(file,false));
        item(menu,"Move to bin",()->pad.putAway(file,true));
        return menu;
    }
    private static void item(JPopupMenu menu,String said,Runnable does){JMenuItem one=DesktopMenus.item(said);one.addActionListener(e->does.run());menu.add(one);}

    /** Rename…, on a file this PC may write in (decision 93): the same file under a new name, for everybody who has it. */
    void renameFile(NoteStore.Branch file) {
        String name=DesktopUi.ask(pad.frame,"Rename file","Name",file.name);
        if(name==null||name.isBlank()||name.trim().equals(file.name))return;
        pad.disk.submit(()->{store().renameFile(file.id,name.trim());return null;},
            done->{pad.status.setText(Say.t("Renamed to {0}",name.trim()));pad.refresh();fileChanged(file);},pad::failed);
    }

    /** Replace with another file…, on a file this PC may write in (decision 93): the file picked becomes its new version. */
    void replaceFile(NoteStore.Branch file) {
        java.nio.file.Path source=DesktopPick.openOne(pad.frame,Say.t("Replace {0} with","“"+file.name+"”"));if(source==null)return;
        pad.status.setText(Say.t("Replacing {0}…",file.name));
        pad.disk.submit(()->{
            String kind;try{kind=java.nio.file.Files.probeContentType(source);}catch(java.io.IOException unsaid){kind=null;}
            try(java.io.InputStream in=java.nio.file.Files.newInputStream(source)){return store().replaceFile(file.id,kind,in);}
        },size->{
            PREVIEWS.remove(file.id);previewAsked.remove(file.id);
            pad.status.setText(Say.t("Replaced  ·  {0}",Attachment.size(size)));pad.refresh();fileChanged(file);
        },pad::failed);
    }

    /** A file renamed or replaced here, to whoever has it now; a new version goes up first (see Post.fileChanged). */
    private void fileChanged(NoteStore.Branch file) {
        if(pad.offline)return;
        boolean shared=file.state!=null&&file.state!=Sharing.State.HERE;
        if(shared)pad.status.setText(Say.t("Sending…"));
        pad.network.submit(()->Post.fileChanged(pad.context,store(),pad.keys,file.id),done->{
            pad.refresh();
            if(shared)pad.status.setText(Say.t(done.failed>0&&!done.why.isEmpty()?done.why:done.sent>0?"Sent":"Nothing to send"));
        },e->{if(shared)pad.status.setText(Say.t("Could not send it. It goes when it can."));});
    }

    /** One kept file, read from the notebook, for what needs more of it than its line says. */
    private void withHeld(NoteStore.Branch file,java.util.function.Consumer<NoteStore.Held> then) {

        pad.disk.submit(()->store().file(file.id),held->{
            if(held==null){pad.status.setText(Say.t("That file is not here any more."));refresh();return;}
            then.accept(held);
        },pad::failed);
    }

    /** Put in a note: one of the notes written in lately, or a new one. It is that note's attachment from then on. */
    void intoNote(NoteStore.Branch file) {
        pad.disk.submit(()->store().lately(Given.RECENT*3),lately->{
            List<NoteStore.Branch> choices=new ArrayList<>();
            choices.add(new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,"","","New note","",0,0,false));
            for(NoteStore.Branch one:lately)if(choices.size()<=12)choices.add(one);
            NoteStore.Branch into=DesktopUi.pick(pad.frame,"Put in a note",file.name,choices,
                b->b.id.isEmpty()?"New note":b.name+(b.detail.isEmpty()?"":"   ·   "+b.detail),"Put it there");
            if(into==null)return;
            pad.disk.submit(()->{
                String note=into.id;
                if(note.isEmpty()) {
                    NoteStore.Note fresh=new NoteStore.Note();fresh.book=Things.HOME;
                    int dot=file.name.lastIndexOf('.');fresh.title=dot>0?file.name.substring(0,dot):file.name;store().save(fresh);note=fresh.id;
                } else if(store().onlyReads(note))throw new IllegalStateException("That note is read only. Nothing was changed.");
                store().intoNote(file.id,note);
                return note;
            },note->{pad.status.setText(Say.t("{0} is in the note now",file.name));pad.filesChanged(note);pad.refresh();pad.open(note);},pad::failed);
        },pad::failed);
    }

    /** Move to…: Home, or any collection, with where each is. */
    private void moveFile(NoteStore.Branch file) {
        pad.disk.submit(()->store().places(false),places->{
            List<NoteStore.Branch> offered=new ArrayList<>();
            for(NoteStore.Branch one:places)if(!(NoteStore.home(file.parent)?NoteStore.home(one.id):one.id.equals(file.parent)))offered.add(one);
            if(offered.isEmpty()){pad.status.setText(Say.t("There is nowhere else to move it."));return;}
            NoteStore.Branch into=DesktopUi.pick(pad.frame,"Move “"+file.name+"”","Where to?",offered,Desktop::placeName,"Move here");
            if(into==null)return;
            boolean top=into.kind==NoteStore.Branch.Kind.LIBRARY;
            into(file,top?Things.HOME:into.id,top?"Home":into.name,NoteStore.Branch.Kind.COLLECTION,null);
        },pad::failed);
    }


    // ---- moving, merging, the order, the dock --------------------------------------------------------------------------

    /**
     * The collections above a thing and then the thing itself: what a sharing rule is looked for along. A file's is what
     * keeps it - Home, a collection or a note - and then the file, since a file has no place in the tree of its own.
     */
    private List<String> pathOf(NoteStore.Branch line) {
        if(line.kind!=NoteStore.Branch.Kind.FILE)return store().pathOf(line.id);
        // Where it is kept, not where it is shown: one shared with this PC is kept on Home wherever it shows (decision 93).
        return store().filePath(line.id);
    }
    /** Where a thing would be, once it is inside {@code into}: the path down to that, and then it. */
    private List<String> pathInto(String into,String id) {
        List<String> path=NoteStore.home(into)?new ArrayList<>():store().pathOf(into);
        path.add(id);
        return path;
    }
    /** Names of the devices by address, for the words that say who a move reaches. */
    private Map<String,String> names() {
        Map<String,String> names=new HashMap<>();
        for(NoteStore.Contact contact:store().addresses())names.put(contact.address,contact.name);
        return names;
    }
    /** Who a move starts and stops reaching, by name, between a first line and a last - as the Move box has always said it. */
    static String changeSaid(String first,Sharing.Change change,Map<String,String> names,String last) {
        StringBuilder said=new StringBuilder(first);
        if(!change.gained.isEmpty()){said.append("\n\nStarts reaching: ");said.append(String.join(", ",change.gained.keySet().stream().map(a->names.getOrDefault(a,"a paired device")).toList()));}
        if(!change.lost.isEmpty()){said.append("\n\nStops reaching: ");said.append(String.join(", ",change.lost.keySet().stream().map(a->names.getOrDefault(a,"a paired device")).toList()));}
        return said.append("\n\n").append(last).toString();
    }

    /**
     * Into a collection or onto Home, or a file into a note - or up a level, out of a card. What changes who it reaches
     * is asked first, as every move is; what the notebook refuses, such as a collection into its own inside, is said as
     * it says it.
     *
     * @param after what to do once it has gone, or null for nothing more than drawing Home again
     */
    void into(NoteStore.Branch moved,String dest,String destName,NoteStore.Branch.Kind destKind,Runnable after){into(moved,dest,destName,destKind,after,null);}
    /** @param there run on the disk once it is moved, to put it in the cell it was let go in; failing, it takes the first free one */
    void into(NoteStore.Branch moved,String dest,String destName,NoteStore.Branch.Kind destKind,Runnable after,Runnable there) {
        pad.disk.submit(()->{
            List<Sharing.Rule> rules=store().shares();
            List<String> from=pathOf(moved),to=pathInto(dest,moved.id);
            Sharing.Change change=Sharing.moving(rules,from,to);
            boolean reaches=!Sharing.audience(rules,from).isEmpty()||!Sharing.audience(rules,to).isEmpty();
            boolean theirs=moved.kind!=NoteStore.Branch.Kind.FILE&&store().someoneElses(moved.kind,moved.id);
            boolean alone=moved.kind==NoteStore.Branch.Kind.FILE&&!store().looseAudience(moved.id).isEmpty();
            return new Object[]{change,names(),reaches,theirs,alone};
        },found->{
            Sharing.Change change=(Sharing.Change)found[0];boolean reaches=(Boolean)found[2],theirs=(Boolean)found[3],alone=(Boolean)found[4];
            @SuppressWarnings("unchecked") Map<String,String> names=(Map<String,String>)found[1];
            String where=NoteStore.home(dest)?"Home":destName;
            if(!change.any()){doInto(moved,dest,where,destKind,reaches,after,there);return;}
            if(moved.kind==NoteStore.Branch.Kind.FILE) {
                if(!DesktopUi.confirm(pad.frame,"This changes who can read it",changeSaid("Moving “"+named(moved)+"” into "+where+" changes who receives it.",change,names,
                    Sharing.fileMoveSaid(alone)),"Move anyway",false)){pad.status.setText(Say.t("Not moved"));refresh();return;}

                doInto(moved,dest,where,destKind,reaches,after,there);return;
            }
            String said=NoteStore.home(dest)?Say.t("Home"):"“"+destName+"”";
            // What somebody shares stays where their sharing has it, linked as before, and is shown where it was put.
            if(theirs){placeOnly(moved,dest,said,after,there);return;}
            // One of this person's own: asked, never shared or stopped by a move alone (the owner, 2026-10-03: "we should be
            // asked if we want to share").
            boolean starts=!change.gained.isEmpty();
            int answer=DesktopUi.confirmOr(pad.frame,starts?"Share “"+named(moved)+"” with them?":"Stop sharing “"+named(moved)+"”?",
                changeSaid(starts?"In "+said+" it would go to more people.":"Out of where it is, it would stop going to some people.",change,names,
                    starts?"Or only put it there on this PC: nobody new gets it.":"Or only put it there on this PC: it stays shared as it is."),starts?"Share it":"Stop sharing","Only put it here");
            if(answer==1)doInto(moved,dest,where,destKind,reaches,after,there);
            else if(answer==2)placeOnly(moved,dest,said,after,there);
            else{pad.status.setText(Say.t("Not moved"));refresh();}
        },pad::failed);
    }

    /**
     * Shown in a collection, or on Home, on this PC only: where it really is and who has it are as they were (see
     * NoteStore.showIn). In the cell it was let go in, as a move puts it; one Undo from where it was shown.
     */
    private void placeOnly(NoteStore.Branch moved,String dest,String where,Runnable after,Runnable there) {
        pad.disk.submit(()->{
            String was=store().showIn(moved.kind,moved.id,dest);
            if(there!=null)try{there.run();}catch(RuntimeException unplaced){/* shown there, in the first free cell */}
            return was;
        },was->{
            pad.canUndo(named(moved),()->{store().showIn(moved.kind,moved.id,was);return null;});
            pad.status.setToolTipText(null);pad.status.setText(Say.t("Put in {0}, shared as before",where));
            if(after!=null)after.run();
            pad.refresh();
        },e->{pad.status.setText(Say.t(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not put that there. Nothing was changed."));pad.refresh();});
    }

    private void doInto(NoteStore.Branch moved,String dest,String where,NoteStore.Branch.Kind destKind,boolean reaches,Runnable after,Runnable there) {
        pad.disk.submit(()->{
            // Where it was, before it is anywhere else: an undo has to know the place to put it back into.
            String was;
            if(moved.kind==NoteStore.Branch.Kind.FILE){NoteStore.Held held=store().file(moved.id);was=held==null?Things.HOME:held.note;}
            else was=moved.kind==NoteStore.Branch.Kind.PAGE?store().bookOf(moved.id):store().collectionOfBook(moved.id);
            store().moveInto(moved.kind,moved.id,dest);
            if(there!=null)try{there.run();}catch(RuntimeException unplaced){/* moved, and in the first free cell */}
            return was==null||was.isEmpty()?Things.HOME:was;
        },was->{
            pad.canUndo(named(moved),()->{store().moveInto(moved.kind,moved.id,was);return null;});
            pad.movedNote(moved,dest);
            pad.status.setToolTipText(null);pad.status.setText(NoteStore.home(dest)?Say.t("Moved to Home"):Say.t("Moved into {0}",where));
            if(after!=null)after.run();
            pad.refresh();
            // Into something somebody else reads is a disclosure: it goes now, as sharing does.
            if(moved.kind==NoteStore.Branch.Kind.FILE){if(destKind==NoteStore.Branch.Kind.PAGE)pad.filesChanged(dest);}
            else if(reaches)pad.sendChanged(moved.kind,moved.id);
        },e->{pad.status.setText(Say.t(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not move that. Nothing was changed."));pad.refresh();});
    }

    /**
     * A note let go on a note: a new collection, Untitled, where the one let go on was, holding both - and its card
     * opened with the name ready to be typed over. Asked first if it changes who either of them reaches.
     */
    void merge(NoteStore.Branch dropped,NoteStore.Branch onto,String container,boolean inCard) {
        pad.disk.submit(()->{
            List<Sharing.Rule> rules=store().shares();
            Map<String,Boolean> gained=new java.util.LinkedHashMap<>(),lost=new java.util.LinkedHashMap<>();
            boolean reaches=false;
            for(NoteStore.Branch thing:new NoteStore.Branch[]{dropped,onto}) {
                List<String> from=store().pathOf(thing.id),to=pathInto(container,thing.id);
                Sharing.Change change=Sharing.moving(rules,from,to);
                gained.putAll(change.gained);lost.putAll(change.lost);
                if(!Sharing.audience(rules,from).isEmpty())reaches=true;
            }
            return new Object[]{new Sharing.Change(gained,lost),names(),reaches};
        },found->{
            Sharing.Change change=(Sharing.Change)found[0];boolean reaches=(Boolean)found[2];
            @SuppressWarnings("unchecked") Map<String,String> names=(Map<String,String>)found[1];
            if(change.any()&&!DesktopUi.confirm(pad.frame,"This changes who can read them",changeSaid("Putting “"+named(dropped)+"” and “"+named(onto)+"” together changes who receives them.",change,names,
                    "Whoever starts receiving them gets them now. What has already reached somebody stays with them."),"Put them together",false)){pad.status.setText(Say.t("Not moved"));refresh();return;}
            pad.disk.submit(()->store().merge(dropped.id,onto.id),made->{
                NoteStore.Branch line=new NoteStore.Branch(NoteStore.Branch.Kind.COLLECTION,made.id,NoteStore.home(container)?Sharing.EVERYTHING:container,made.name,"2 notes",0,0,true);
                pad.status.setToolTipText(null);pad.status.setText(Say.t("Put together in a new folder. Type its name."));
                if(inCard)folder.into(line,true);else folder.open(line,true);
                pad.refresh();
                if(reaches)pad.sendChanged(NoteStore.Branch.Kind.COLLECTION,made.id);
            },e->{pad.status.setText(Say.t(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Could not put those together. Nothing was changed."));pad.refresh();});
        },pad::failed);
    }

    /** Whether a grid keeps where its icons are put: Home's and a collection's card; a place's card only lists them. */
    boolean places(Icons in){return in!=null&&(in==grid||folder!=null&&in==folder.grid&&!folder.listing());}

    /**
     * The icons stay where they are drawn when one of them is put in the bin or the archive (the owner, 2026-10-07:
     * "On the desktop, binning an element must not move the others"; decision 119). A carry already pins the grid the
     * first time something on it is moved (decision 39). Putting one away did not, so the ones with no cell closed up
     * into the gap. What is drawn now is written down first, the one leaving included, so the gap it leaves stays a gap
     * and putting it back puts it back there. A place's card only lists, and is left as it is.
     *
     * @return what writes that, for the disk's thread, or null where this grid does not keep cells
     */
    Runnable stayPut(String id) {
        Icons in=gridOf(id);
        if(!places(in))return null;
        List<NoteStore.Branch> lines=new ArrayList<>();
        for(Tile one:in.tiles)lines.add(one.thing);
        if(in.paged()) {
            Map<String,Layout.Spot> spots=new java.util.LinkedHashMap<>(in.spots());
            return ()->writeSpots(lines,spots);
        }
        Map<String,int[]> drawn=in.drawn();
        Map<String,Integer> cells=new java.util.LinkedHashMap<>();
        for(Tile one:in.tiles){int[] at=drawn.get(one.thing.id);cells.put(one.thing.id,at==null?one.thing.cell:Layout.cell(at[0],at[1]));}
        return ()->writeCells(lines,cells);
    }

    /** The open folder's grid when the thing is in it, otherwise Home's, or null where it is on neither. */
    private Icons gridOf(String id) {
        if(id==null)return null;
        if(folder!=null&&folder.grid!=null&&!folder.listing())for(Tile one:folder.grid.tiles)if(one.thing.id.equals(id))return folder.grid;
        if(grid!=null)for(Tile one:grid.tiles)if(one.thing.id.equals(id))return grid;
        return null;
    }

    /**
     * Home's places, by id: each keeps where it stands on Home in this PC's settings, as the phone keeps them, having no row
     * to keep it in - "favouritesCell", "archiveCell", "binCell".
     */
    static final String[] PLACES={NoteStore.FAVOURITES,NoteStore.ARCHIVE,NoteStore.BIN,NoteStore.TOOLS,NoteStore.TEMP,NoteStore.RECENT,NoteStore.SHARED};

    /** Every favourite, in their one order: the Favourites card's lines (decision 74). */
    List<NoteStore.Branch> allFavourites(){List<NoteStore.Branch> all=new ArrayList<>(docked);all.addAll(beyond);return all;}

    /** One favourite put before another in their one order, or last; the dock's first places are the first of it. */
    private void reorderFavourites(NoteStore.Branch carried,NoteStore.Branch before) {
        List<NoteStore.Branch> order=new ArrayList<>(allFavourites());
        order.removeIf(one->one.id.equals(carried.id));
        int at=order.size();
        if(before!=null)for(int i=0;i<order.size();i++)if(order.get(i).id.equals(before.id)){at=i;break;}
        order.add(at,carried);int place=at+1;
        pad.disk.submit(()->{store().orderFavourites(order);return null;},done->{pad.status.setText(Say.t("Favourite {0}",place));refresh();},pad::failed);
    }

    /** Something an accepted code is bringing, clicked: who it waits for, and the one thing to do, to stop waiting. */
    private void waiting(NoteStore.Branch line) {
        // A file shared with you, still being fetched (decision 94): nothing to do but know it is coming.
        if(line.id.startsWith(NoteStore.COMING)){DesktopUi.tell(pad.frame,line.name,DesktopUi.note(line.detail+". It opens here once all of it has come.",380,DesktopUi.INK,DesktopUi.BODY));return;}
        String address=line.id.startsWith("waiting:")?line.id.substring(8):line.id;
        if(DesktopUi.confirmOr(pad.frame,line.name,line.detail+". It comes when their device is next on, and takes this place on Home.","Keep waiting","Stop waiting")==2)
            pad.disk.submit(()->{store().stopWaiting(address);return null;},done->refresh(),pad::failed);
    }
    static Map<String,Integer> placeCells(org.mininotes.desktop.platform.content.Context context) {
        var settings=context.getSharedPreferences("settings",0);
        Map<String,Integer> cells=new HashMap<>();
        for(String id:PLACES)cells.put(id,(int)settings.getLong(id+"Cell",Layout.NONE));
        return cells;
    }
    /** A grid's cells written, the places' among them kept too, so none jumps into a gap. On the disk's thread. */
    private void writeCells(List<NoteStore.Branch> lines,Map<String,Integer> cells) {
        store().place(lines,cells);
        var kept=pad.context.getSharedPreferences("settings",0).edit();boolean any=false;
        for(String id:PLACES){Integer at=cells.get(id);if(at!=null){kept.putLong(id+"Cell",at);any=true;}}
        if(any)kept.apply();
    }
    /** Home's icons written where they stand, each on its page, the places' in this PC's settings. On the disk's thread. */
    /**
     * Everything on a grid but what is leaving it written down where it is drawn now, so the spot left behind stays empty: an
     * icon never placed by hand has no cell of its own and took the first free one, which the leaving one had just freed.
     */
    private void holdOthers(Icons in,NoteStore.Branch leaving) {
        if(in==null||leaving==null)return;
        List<NoteStore.Branch> lines=new ArrayList<>();
        for(Tile one:in.tiles)if(!one.thing.id.equals(leaving.id))lines.add(one.thing);
        if(in.paged()) {
            Map<String,Layout.Spot> spots=new HashMap<>(in.spots());spots.remove(leaving.id);
            pad.disk.submit(()->{writeSpots(lines,spots);return null;},done->{},pad::failed);
        } else {
            Map<String,Integer> cells=new HashMap<>();
            for(Map.Entry<String,int[]> one:in.drawn().entrySet())
                if(!one.getKey().equals(leaving.id)&&one.getValue()!=null)cells.put(one.getKey(),Layout.cell(one.getValue()[0],one.getValue()[1]));
            pad.disk.submit(()->{store().place(lines,cells);return null;},done->{},pad::failed);
        }
    }

    private void writeSpots(List<NoteStore.Branch> lines,Map<String,Layout.Spot> spots) {
        store().placeOnPages(lines,spots);
        var kept=pad.context.getSharedPreferences("settings",0).edit();boolean any=false;
        for(String id:PLACES){Layout.Spot at=spots.get(id);if(at!=null){kept.putLong(id+"Cell",at.cell());kept.putLong(id+"Page",at.page());any=true;}}
        if(any)kept.apply();
    }
    /** Home's icons put back as they were, a place each or none, the places' in this PC's settings. On the disk's thread. */
    private void writeWere(List<NoteStore.Branch> lines,Map<String,Integer> cells,Map<String,Integer> pages) {
        store().placeOnPages(lines,cells,pages);
        var kept=pad.context.getSharedPreferences("settings",0).edit();boolean any=false;
        for(String id:PLACES){Integer at=cells.get(id),on=pages.get(id);if(at!=null&&on!=null){kept.putLong(id+"Cell",at);kept.putLong(id+"Page",on);any=true;}}
        if(any)kept.apply();
    }
    /** The page each place was pinned on, by id, from this PC's settings: none for one kept before pages. */
    static Map<String,Integer> placePages(org.mininotes.desktop.platform.content.Context context) {
        var settings=context.getSharedPreferences("settings",0);
        Map<String,Integer> on=new HashMap<>();
        for(String id:PLACES)on.put(id,(int)settings.getLong(id+"Page",Layout.NO_PAGE));
        return on;
    }

    /**
     * A grid's icons where they were left (decision 39): every one pinned where it is drawn and the one moved in its new
     * cell, as {@link Layout#moveTo} gives them - shown at once, so it does not jump back while the notebook is written,
     * then written in one go, one Undo from where they all were.
     */
    private void keepCells(Icons in,NoteStore.Branch moved,Map<String,Integer> cells,String said) {
        if(in.paged()){keepSpots(in,moved,spotsFrom(in,cells),said);return;}
        List<NoteStore.Branch> lines=new ArrayList<>();Map<String,Integer> before=new HashMap<>();
        for(Tile one:in.tiles){lines.add(one.thing);before.put(one.thing.id,one.thing.cell);}
        for(Tile one:in.tiles){Integer now=cells.get(one.thing.id);if(now!=null)one.thing.cell=now;}
        in.rearrange();
        pad.disk.submit(()->{writeCells(lines,cells);return null;},done->{
            pad.canUndo(named(moved),()->{writeCells(lines,before);return null;});
            pad.status.setToolTipText(null);pad.status.setText(Say.t(said));pad.refresh();
        },e->{pad.status.setText(Say.t("Could not put it there. Nothing was changed."));pad.refresh();});
    }

    /** Cells on the page in view, as Home keeps them: every icon pinned where it stands on its page, those given here on this one. */
    private static Map<String,Layout.Spot> spotsFrom(Icons in,Map<String,Integer> cells) {
        Map<String,Layout.Spot> all=new java.util.LinkedHashMap<>(in.spots());
        for(Map.Entry<String,Integer> one:cells.entrySet())
            all.put(one.getKey(),new Layout.Spot(in.pageX,in.pageY,Layout.row(one.getValue()),Layout.column(one.getValue())));
        return all;
    }
    /**
     * Home's icons where they were left, on every page (decision 50): shown at once, then written in one go, one Undo from
     * where they all were.
     */
    private void keepSpots(Icons in,NoteStore.Branch moved,Map<String,Layout.Spot> spots,String said){keepSpots(in,named(moved),spots,said);}
    /**
     * A whole page carried in the view of every page to another place (Layout.movePage): written as any move on Home is,
     * one Undo from where they all were, and said.
     */
    void movePage(int fx,int fy,int tx,int ty) {
        Map<String,Layout.Spot> before=grid.spots();
        boolean swapped=before.values().stream().anyMatch(at->at!=null&&at.on(tx,ty));
        keepSpots(grid,"the page",Layout.movePage(before,fx,fy,tx,ty),Say.t(swapped?"The two pages changed places":"Page moved"));
        if(grid.pageX==fx&&grid.pageY==fy)grid.showPage(tx,ty);
        pages.repaint();
    }
    private void keepSpots(Icons in,String undone,Map<String,Layout.Spot> spots,String said) {
        // Undo puts back what each had, a place of its own or none - not where it happened to be drawn.
        List<NoteStore.Branch> lines=new ArrayList<>();Map<String,Integer> cellsWere=new HashMap<>(),pagesWere=new HashMap<>();
        for(Tile one:in.tiles){lines.add(one.thing);cellsWere.put(one.thing.id,one.thing.cell);pagesWere.put(one.thing.id,one.thing.page);}
        for(Tile one:in.tiles){Layout.Spot now=spots.get(one.thing.id);if(now!=null){one.thing.cell=now.cell();one.thing.page=now.page();}}
        in.rearrange();
        pad.disk.submit(()->{writeSpots(lines,spots);return null;},done->{
            pad.canUndo(undone,()->{writeWere(lines,cellsWere,pagesWere);return null;});
            pad.status.setToolTipText(null);pad.status.setText(Say.t(said));pad.refresh();
        },e->{pad.status.setText(Say.t("Could not put it there. Nothing was changed."));pad.refresh();});
    }

    /**
     * Ctrl+arrow on an icon: one cell that way, where that cell is empty - the keyboard's way to put a thing where it is
     * wanted. The keyboard stays on it; what stops it is said.
     */
    private void nudge(Icons in,Tile tile,Does way) {
        if(!places(in)||!Grid.carried(tile.thing.kind)||Grid.place(tile.thing.kind)&&in!=grid){pad.status.setToolTipText(null);pad.status.setText(Say.t("This one stays where it is"));return;}
        Map<String,int[]> drawn=in.drawn();
        int[] to=nudged(drawn,tile.thing.id,way,in.columns());
        // On a page of Home, not past its last row.
        if(to!=null&&in.paged()&&to[0]>=in.rows())to=null;
        if(to==null) {
            int[] here=drawn.get(tile.thing.id);
            int row=here==null?-1:here[0]+(way==Does.MOVE_DOWN?1:way==Does.MOVE_UP?-1:0),column=here==null?-1:here[1]+(way==Does.MOVE_RIGHT?1:way==Does.MOVE_LEFT?-1:0);
            String there=row<0||column<0?null:Layout.at(drawn,row,column);
            Tile other=null;if(there!=null)for(Tile one:in.tiles)if(one.thing.id.equals(there))other=one;
            pad.status.setToolTipText(null);pad.status.setText(other!=null?Say.t("“{0}” is in the way",named(other.thing)):Say.t("It is at the edge of the grid"));
            return;
        }
        keepCells(in,tile.thing,Layout.moveTo(drawn,tile.thing.id,to[0],to[1]),"“"+named(tile.thing)+"” is in row "+(to[0]+1)+", column "+(to[1]+1)+" now");
        tile.requestFocusInWindow();tile.scrollRectToVisible(new Rectangle(tile.getSize()));
    }

    /** A note or a folder at a place in the dock, the rest moved along; a favourite or not as it was (decision 190). */
    private void toDock(NoteStore.Branch thing,int slot) {
        pad.disk.submit(()->{store().toDock(thing.kind,thing.id,slot);return null;},
            done->{pad.status.setToolTipText(null);pad.status.setText(Say.t("“{0}” is in the dock",named(thing)));pad.refresh();},
            e->{pad.status.setText(Say.t("Could not put that in the dock. Nothing was changed."));pad.refresh();});
    }

    static String named(NoteStore.Branch b){return b.name==null||b.name.isBlank()?Say.t("Untitled"):place(b.kind)?Say.t(b.name):b.name;}
    /**
     * The app's own places (Temp, Favourites, Bin, Archive, Shared with me, Recent...), named in the language chosen; a
     * folder, note or file keeps the name its owner gave it (the owner, 2026-10-12: "why are the system folders not
     * translated in Chinese?").
     */
    static boolean place(NoteStore.Branch.Kind kind) {
        return switch(kind){case COLLECTION,BOOK,PAGE,FILE,WAITING->false;default->true;};
    }

    // ---- searching ----------------------------------------------------------------------------------------------------

    /**
     * What is typed looked for as it is typed - a moment after the last key, so a fast typist asks the notebook once -
     * and shown in the grid's place: notes, collections and files alike, each saying where it is. Emptied, the grid is back
     * where it was scrolled to.
     */
    private void look() {
        String term=search.getText().trim();
        if(term.length()<2){showGrid();return;}
        if(folder.isOpen())folder.close();
        pad.disk.submit(()->store().lookingEverywhere(term),hits->{
            if(!search.getText().trim().equals(term))return;
            results.removeAll();
            if(hits.isEmpty())DesktopUi.add(results,DesktopUi.note("Nothing found for “"+term+"”.",420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
            for(NoteStore.Branch hit:hits)DesktopUi.add(results,new Row(hit));
            if(scroll.getViewport().getView()!=results){gridAt=scroll.getViewport().getViewPosition();scroll.setViewportView(results);}
            results.revalidate();results.repaint();scroll.getViewport().setViewPosition(new java.awt.Point(0,0));
        },pad::failed);
    }
    private java.awt.Point gridAt=new java.awt.Point(0,0);
    private void showGrid() {
        if(scroll.getViewport().getView()==grid)return;
        scroll.setViewportView(grid);java.awt.Point back=gridAt;SwingUtilities.invokeLater(()->scroll.getViewport().setViewPosition(back));
    }
    private Component firstResult(){for(Component c:results.getComponents())if(c instanceof Row)return c;return null;}

    /** The results: a column as wide as the window allows, in the middle of it. */
    private static final class ResultList extends JPanel implements Scrollable {
        ResultList(){setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setOpaque(false);}
        /** Room at the sides for a column no wider than a line is read at, however wide the window. */
        @Override public Insets getInsets(){int side=Math.max(SIDE,(getWidth()-720)/2);return new Insets(TOP,side,UNDER,side);}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-48);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }

    /** One thing a word was found in: its small face, what it is called, and where it is with the words around it. */
    final class Row extends JPanel {
        final NoteStore.Branch thing;boolean over;
        Row(NoteStore.Branch thing) {
            super(new BorderLayout(12,0));this.thing=thing;setOpaque(false);setFocusable(true);
            setBorder(BorderFactory.createEmptyBorder(8,10,8,10));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            putClientProperty(THING,thing);putClientProperty(MENU,(Supplier<JPopupMenu>)()->menuFor(thing));
            JComponent face=new JComponent(){
                {setPreferredSize(new Dimension(36,36));}
                @Override protected void paintComponent(Graphics g0){Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);face(g,thing,0,0,36,pad.usual);g.dispose();}
            };
            JPanel faceHolder=new JPanel(new GridBagLayout());faceHolder.setOpaque(false);faceHolder.add(face);add(faceHolder,BorderLayout.WEST);
            JPanel words=DesktopUi.column();
            JLabel name=new JLabel(named(thing));name.setFont(DesktopUi.BODY);name.setForeground(DesktopUi.INK);DesktopUi.add(words,name);
            if(thing.detail!=null&&!thing.detail.isBlank()){JLabel where=new JLabel(thing.detail);where.setFont(DesktopUi.BODY.deriveFont(13f));where.setForeground(DesktopUi.QUIET);DesktopUi.add(words,where);}
            add(words);
            getAccessibleContext().setAccessibleName(said(thing)+(thing.detail==null||thing.detail.isBlank()?"":", "+thing.detail));
            MouseAdapter hand=new MouseAdapter(){
                public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))pad.openFound(thing,search.getText());}
                public void mousePressed(MouseEvent e){requestFocusInWindow();if(e.isPopupTrigger())menuFor(thing).show(e.getComponent(),e.getX(),e.getY());}
                public void mouseReleased(MouseEvent e){if(e.isPopupTrigger())menuFor(thing).show(e.getComponent(),e.getX(),e.getY());}
                public void mouseEntered(MouseEvent e){over=true;repaint();}
                public void mouseExited(MouseEvent e){over=false;repaint();}
            };
            // The words and the face take the same clicks as the line, so a line is one thing to point at.
            for(Component part:new Component[]{this,faceHolder,face,words})part.addMouseListener(hand);
            for(Component part:words.getComponents())part.addMouseListener(hand);
            addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){repaint();if(e.getCause()!=FocusEvent.Cause.MOUSE_EVENT)scrollRectToVisible(new Rectangle(getSize()));}public void focusLost(FocusEvent e){repaint();}});
            addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){
                Does does=does(e.getKeyCode(),e.isShiftDown(),e.isControlDown()||e.isAltDown());
                Component[] all=results.getComponents();int at=java.util.Arrays.asList(all).indexOf(Row.this);
                switch(does) {
                    case OPEN: pad.openFound(thing,search.getText());break;
                    case MENU: menuFor(thing).show(Row.this,12,getHeight()-6);break;
                    case UP: if(at>0&&all[at-1] instanceof Row up)up.requestFocusInWindow();else search.requestFocusInWindow();break;
                    case DOWN: if(at+1<all.length)all[at+1].requestFocusInWindow();break;
                    default: return;
                }
                e.consume();
            }});
        }
        @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        @Override protected void paintComponent(Graphics g0) {
            boolean ring=ringed(this);
            if(!over&&!ring)return;
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.PAPER,0.92f));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            if(ring){g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(1.5f));g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,12,12);}
            g.dispose();
        }
    }

    /**
     * A thing's menu wherever it is: a file's is a file's; a place's opens it - and the bin's empties it; anything else, the
     * thing's. In the archive's or the bin's card, where it goes from there.
     */
    JPopupMenu menuFor(NoteStore.Branch thing){return menuFor(thing,null);}
    JPopupMenu menuFor(NoteStore.Branch thing,Where where) {
        // A share that waits: Accept, Refuse, See sender. Not the file's menu, and not the thing's (decision 127).
        if(thing!=null&&thing.asking)return arrivalMenu(thing);
        if(where==Where.AWAY)return awayMenu(thing,folder.inBin());
        if(thing.kind==NoteStore.Branch.Kind.FILE)return fileMenu(thing);
        if(thing.kind==NoteStore.Branch.Kind.FAVOURITES){JPopupMenu menu=new JPopupMenu();item(menu,"Open",folder::openFavourites);
            menu.addSeparator();menu.add(pad.placeColourMenu(thing));return menu;}
        if(Grid.place(thing.kind)&&thing.kind!=NoteStore.Branch.Kind.WAITING) {
            JPopupMenu menu=new JPopupMenu();item(menu,"Open",()->folder.openPlace(thing));
            // Its colour, as any collection's (decision 81).
            if(thing.kind!=NoteStore.Branch.Kind.TOOLS){menu.addSeparator();menu.add(pad.placeColourMenu(thing));}
            if(thing.kind==NoteStore.Branch.Kind.BIN){menu.addSeparator();item(menu,"Empty the bin…",pad::askEmptyBin);}
            // Temp's own setting: how long what is carried onto it stays (decision 79).
            if(thing.kind==NoteStore.Branch.Kind.TEMP){menu.addSeparator();pad.tempRows(menu);}
            // Off Home, from its own menu: then it is in ⋯, and Home's menu brings it back (decision 78).
            if(Grid.tool(thing.kind)||thing.kind==NoteStore.Branch.Kind.SHARED||thing.kind==NoteStore.Branch.Kind.MESSAGES){menu.addSeparator();item(menu,"Hide from Home",()->pad.setOnHome(thing.id,false));}
            return menu;
        }
        if(thing.kind==NoteStore.Branch.Kind.WAITING){JPopupMenu menu=new JPopupMenu();item(menu,"Waiting…",()->waiting(thing));return menu;}
        JPopupMenu menu=pad.thingMenu(thing);
        // In Recent, the thing's whole menu and, at its end, taking it out of Recent (decision 154).
        if(menu!=null&&where==Where.FAVOURITES&&folder.inRecent()&&(thing.kind==NoteStore.Branch.Kind.PAGE||DesktopMoving.shelf(thing.kind))) {
            menu.addSeparator();
            item(menu,"Remove from Recent",()->pad.disk.submit(()->{pad.store.outOfRecent(thing.id);return null;},done->{refresh();pad.status.setText(Say.t("Removed from Recent"));},pad::failed));
        }
        return menu;
    }

    /** A share that waits, clicked: Accept, Refuse, and See sender. Block lives on See sender, and only for someone not in People. */
    private JPopupMenu arrivalMenu(NoteStore.Branch thing) {
        JPopupMenu menu=new JPopupMenu();
        item(menu,FirstShare.ACCEPT,()->pad.acceptWaiting(thing));
        item(menu,FirstShare.REFUSE,()->pad.refuseWaiting(thing));
        item(menu,FirstShare.SEE_SENDER,()->pad.seeSender(thing));
        return menu;
    }

    /**
     * A thing waiting in the archive or the bin: where it can go from there, as the lists always offered - Put back; and
     * from the archive to the bin, or from the bin gone for good, after one question.
     */
    JPopupMenu awayMenu(NoteStore.Branch thing,boolean bin) {
        JPopupMenu menu=new JPopupMenu();
        item(menu,"Put back",()->pad.disk.submit(()->{store().restore(thing.kind,thing.id);return null;},
            done->{pad.status.setToolTipText(null);pad.status.setText(Say.t("“{0}” is back where it was",named(thing)));pad.refresh();},pad::failed));
        if(bin)item(menu,"Delete for good…",()->pad.eraseForGood(thing));
        else item(menu,"Move to the bin",()->pad.disk.submit(()->{store().putAway(thing.kind,thing.id,false,false);store().putAway(thing.kind,thing.id,true,true);return null;},
            done->{pad.status.setToolTipText(null);pad.status.setText(Say.t("Moved to the bin"));pad.refresh();},pad::failed));
        return menu;
    }

    // ---- drawing a thing ----------------------------------------------------------------------------------------------

    /**
     * A thing's face in a square, as the phone draws it (docs/HOME.md, step 4): a picture of its own filling the rounded
     * square; else its icon - a note's the note icon until it is given another - in its colour's ink on paper washed in
     * that colour; a collection with no icon, the mini-grid of what is inside it; a file's kind in its own letters; the
     * Favourites' star. An icon this build's set does not have is drawn as the thing's default.
     */
    /**
     * Picture files' faces, their pictures made small (the owner, 2026-10-04: "when we add elements like pictures, the icon
     * should be a preview"; decision 88): read through the lock on the disk, kept while the app runs.
     */
    static final Map<String,java.awt.image.BufferedImage> PREVIEWS=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String,java.awt.image.BufferedImage>(64,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,java.awt.image.BufferedImage> eldest){return size()>80;}
    });
    private final Set<String> previewAsked=java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** The pictures among lines just drawn, read for their faces if they are not yet; the window drawn again with them. */
    void wantPreviews(List<NoteStore.Branch> lines) {
        List<String> missing=new ArrayList<>();
        for(NoteStore.Branch one:lines)
            if(one.kind==NoteStore.Branch.Kind.FILE&&DesktopFileCards.pictureNamed(one.name)&&!PREVIEWS.containsKey(one.id)&&previewAsked.add(one.id))missing.add(one.id);
        if(missing.isEmpty())return;
        pad.disk.submit(()->{
            for(String id:missing) {
                NoteStore.Held held=store().file(id);
                if(held==null||!DesktopFileCards.picture(held))continue;
                try{java.awt.image.BufferedImage made=pad.fileCards.thumbnail(held,128,128);if(made!=null)PREVIEWS.put(id,made);}
                catch(Exception unreadable){/* shown by its kind */}
            }
            return null;
        },done->pad.frame.repaint(),e->{});
    }

    /**
     * A thing's face. Its colour lands at the thing's own strength where it has one (the owner, 2026-10-06; decision 107);
     * {@code usual} is the strength for one that has none.
     */
    static void face(Graphics2D g,NoteStore.Branch b,int x,int y,int side,int usual) {
        int tone=Tint.tone(b.tone,usual);
        int arc=Math.max(10,side*18/64);
        // A picture file shows itself, once read (decision 88): the picture filling the round square, a hairline round it.
        java.awt.image.BufferedImage preview=b.kind==NoteStore.Branch.Kind.FILE?PREVIEWS.get(b.id):null;
        if(preview!=null) {
            Shape was=g.getClip();g.clip(new java.awt.geom.RoundRectangle2D.Float(x,y,side,side,arc,arc));
            Object hint=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(preview,x,y,side,side,null);
            if(hint!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,hint);
            g.setClip(was);g.setColor(new Color(0,0,0,36));g.setStroke(new BasicStroke(1f));g.drawRoundRect(x,y,side-1,side-1,arc,arc);
            return;
        }
        boolean coloured=Tint.known(b.colour);
        Color edge=coloured?DesktopLook.of(b.colour):DesktopUi.LINE.darker();
        g.setStroke(new BasicStroke(side>=48?1.4f:1.1f));
        switch(b.kind) {
            case FILE -> {
                g.setColor(DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.CARD,0.88f));g.fillRoundRect(x,y,side,side,arc,arc);
                g.setColor(DesktopUi.LINE.darker());g.drawRoundRect(x,y,side-1,side-1,arc,arc);
                String kind=DesktopFileCards.extension(b.name);kind=kind.length()>4?kind.substring(0,4):kind;
                // The icon of the program it opens in, as Explorer shows the kind, over its kind in letters (the owner,
                // 2026-10-15: "the app used to open it should be presented ... in the icon type").
                java.awt.image.BufferedImage app=side>=36?DesktopApps.icon(DesktopApps.kind(b.name)):null;
                if(app!=null) {
                    int icon=Math.round(side*0.46f);
                    Object hint=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(app,x+(side-icon)/2,y+Math.round(side*0.14f),icon,icon,null);
                    if(hint!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,hint);
                    g.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,Math.max(8f,side*0.16f)));FontMetrics m=g.getFontMetrics();
                    g.setColor(DesktopUi.ACCENT);g.drawString(kind,x+(side-m.stringWidth(kind))/2f,y+side*0.86f);
                } else {
                    g.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,Math.max(9f,side*0.22f)));FontMetrics m=g.getFontMetrics();
                    g.setColor(DesktopUi.ACCENT);g.drawString(kind,x+(side-m.stringWidth(kind))/2f,y+(side-m.getHeight())/2f+m.getAscent());
                }
            }
            case FAVOURITES -> {
                // In the colour chosen for it, washed and edged as a collection's square is (decision 81).
                g.setColor(coloured?DesktopLook.wash(b.colour,DesktopUi.CARD,0.22f,0.92f,tone):DesktopUi.CARD);g.fillRoundRect(x,y,side,side,arc,arc);
                g.setColor(edge);g.drawRoundRect(x,y,side-1,side-1,arc,arc);
                frame(g,edge,x,y,side,arc);
                star(g,x+side/2f,y+side/2f,side*0.26f,coloured?DesktopIcons.ink(b.colour):DesktopUi.INK);
            }
            case ARCHIVE, BIN, TOOLS, TEMP, RECENT, SHARED, MESSAGES, WAITING -> {
                // The same outlined square as the Favourites', with the place's own glyph, half its size, as the phone has it.
                g.setColor(coloured?DesktopLook.wash(b.colour,DesktopUi.CARD,0.22f,0.92f,tone):DesktopUi.CARD);g.fillRoundRect(x,y,side,side,arc,arc);
                g.setColor(edge);g.drawRoundRect(x,y,side-1,side-1,arc,arc);
                // What is on its way is not a place, and keeps the one edge a thing has.
                if(b.kind!=NoteStore.Branch.Kind.WAITING)frame(g,edge,x,y,side,arc);
                float glyph=side*0.5f;
                DesktopIcons.draw(g,placeGlyph(b.kind),x+(side-glyph)/2f,y+(side-glyph)/2f,glyph,coloured?DesktopIcons.ink(b.colour):DesktopUi.INK);
            }
            default -> {
                // A picture of its own fills the round square, a hairline round it so a pale one keeps its edge. One this PC
                // cannot read is not drawn: the icon under it is.
                if(DesktopIcons.picture(g,b.image,x,y,side,arc)) {
                    g.setColor(new Color(0,0,0,36));g.setStroke(new BasicStroke(1f));g.drawRoundRect(x,y,side-1,side-1,arc,arc);
                    return;
                }
                boolean collection=b.holds||DesktopMoving.shelf(b.kind);
                String icon=DesktopIcons.known(b.icon)?b.icon:collection?null:DesktopIcons.NOTE;
                Color ground=collection?DesktopLook.wash(b.colour,DesktopUi.CARD,0.22f,0.92f,tone):DesktopLook.wash(b.colour,DesktopUi.PAPER,0.14f,0.82f,tone);
                g.setColor(ground);g.fillRoundRect(x,y,side,side,arc,arc);
                g.setColor(edge);g.drawRoundRect(x,y,side-1,side-1,arc,arc);
                if(icon!=null) {
                    // Its icon, a little over half the square - more of a small one - in its colour: as the phone draws it.
                    float glyph=Looks.glyphSide(side,DesktopIcons.SMALL);
                    DesktopIcons.draw(g,icon,x+(side-glyph)/2f,y+(side-glyph)/2f,glyph,DesktopIcons.ink(b.colour));
                    return;
                }
                // A collection with no icon of its own: a rounded square with the first few things inside it showing through. A
                // small one - a line of the tree, a bar - shows all four: too small to count on, and a lone speck read as nothing.
                boolean small=side<DesktopIcons.SMALL;
                int pad=Math.max(small?3:5,side/7),gap=Math.max(2,side/22),cell=(side-2*pad-gap)/2,held=small?4:Math.min(4,countIn(b.detail));
                for(int at=0;at<held;at++) {
                    int px=x+pad+(at%2)*(cell+gap),py=y+pad+(at/2)*(cell+gap);
                    g.setColor(DesktopLook.wash(b.colour,DesktopUi.PAPER,0.10f,0.7f,tone));g.fillRoundRect(px,py,cell,cell,side/10,side/10);
                    g.setColor(DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawRoundRect(px,py,cell-1,cell-1,side/10,side/10);
                }
            }
        }
    }

    /**
     * A place in its own frame (decision 94, the owner: "let them all have something that differentiates them"): a thin
     * ring inside the edge, in the edge's colour, so a place is told from a folder of the owner's at a glance, as the phone
     * draws it.
     */
    private static void frame(Graphics2D g,Color edge,int x,int y,int side,int arc) {
        int in=Math.max(3,side/14),round=Math.max(4,arc-2*in);
        java.awt.Stroke was=g.getStroke();
        g.setColor(edge);g.setStroke(new BasicStroke(1f));g.drawRoundRect(x+in,y+in,side-1-2*in,side-1-2*in,round,round);
        g.setStroke(was);
    }

    /**
     * A thing's face as a Swing icon, for a line of the tree and the bars over a note and a card: drawn as it is read, at
     * its own strength, or the usual one as it is then, so what it wears changes with the thing without a new icon.
     */
    static Icon faceIcon(java.util.function.Supplier<NoteStore.Branch> thing,int side,java.util.function.IntSupplier usual) {
        return new Icon(){
            // A pixel of room all round: the outline is drawn on the face's edge, and at 125% half of it fell outside what the
            // line next to it let it draw, so the left edge was missing on some lines (the owner, 2026-10-13).
            public int getIconWidth(){return side+2;}public int getIconHeight(){return side+2;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                NoteStore.Branch now=thing.get();if(now==null)return;
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                face(g,now,x+1,y+1,side,usual.getAsInt());g.dispose();
            }
        };
    }

    /** A five-pointed star, filled: the Favourites' face and the corner of a favourite. */
    static void star(Graphics2D g,float cx,float cy,float r,Color colour) {
        // Its outline in the middle, not its points' centre: a star reaches r above it and only 0.81 r below, so it sat high
        // in its round (the owner, 2026-10-14: "make sure the star for favorites is centered in its circle").
        cy+=r*0.0955f;
        java.awt.geom.Path2D.Float p=new java.awt.geom.Path2D.Float();
        for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=i%2==0?r:r*0.45f;float px=cx+(float)(Math.cos(a)*rr),py=cy+(float)(Math.sin(a)*rr);if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}
        p.closePath();g.setColor(colour);g.fill(p);
    }

    /** What a screen reader says for an icon: what a click does, and what else is true of it. */
    static String said(NoteStore.Branch b) {
        if(b.kind==NoteStore.Branch.Kind.ARCHIVE||b.kind==NoteStore.Branch.Kind.BIN)
            return "Open the "+(b.kind==NoteStore.Branch.Kind.BIN?"bin":"archive")+", "+(b.detail==null||b.detail.isBlank()?"empty":b.detail);
        if(b.kind==NoteStore.Branch.Kind.TOOLS)return "Open Tools: the archive, the bin, Temp and Recent";
        if(b.kind==NoteStore.Branch.Kind.TEMP)return "Open Temp, "+(b.detail==null||b.detail.isBlank()?"empty":b.detail+" to be gone");
        if(b.kind==NoteStore.Branch.Kind.RECENT)return "Open Recent, what was opened lately";
        if(b.kind==NoteStore.Branch.Kind.SHARED)return "Open Shared with me, the files people share with you"+(countIn(b.detail)>0?", "+b.detail+" waiting for you":"");
        if(b.kind==NoteStore.Branch.Kind.MESSAGES)return "Open the Inbox, brief messages with the people you are paired with"+(countIn(b.detail)>0?", "+b.detail+" not read":"");
        if(b.kind==NoteStore.Branch.Kind.WAITING)return b.name+": "+b.detail;
        String open=b.kind==NoteStore.Branch.Kind.FILE?"Open the file "+b.name+(b.detail==null||b.detail.isBlank()?"":", "+b.detail)
            :b.kind==NoteStore.Branch.Kind.FAVOURITES?"Open Favourites, the favourites the dock has no room for"
            :b.holds||DesktopMoving.shelf(b.kind)?"Open the folder "+named(b):"Open the note "+named(b);
        return open+(b.kept?", a favourite":"")+(b.temporary?", temporary":"")+(b.fresh?", new":"")+(b.uploading?", uploading":"");
    }

    /** The overview button's picture: two cards, one behind the other, as open things are shown. */
    private static Icon overviewIcon() {
        return new Icon(){
            public int getIconWidth(){return 18;}public int getIconHeight(){return 18;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DesktopUi.INK);g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(x+5,y+1,12,12,3,3);
                g.setColor(DesktopUi.PAPER);g.fillRoundRect(x+1,y+5,12,12,3,3);g.setColor(DesktopUi.INK);g.drawRoundRect(x+1,y+5,12,12,3,3);
                g.dispose();
            }
        };
    }

    // ---- the icons ----------------------------------------------------------------------------------------------------

    /**
     * Icons in cells, as many to a row as the width holds, centred: Home's, or a card's. Each stands in the cell it was put
     * in, with the empty cells between left empty ({@link Layout#arrange}); as tall as the rows make it and one empty row
     * more, so there is always somewhere to put a thing, and never shorter than what shows of it.
     */
    static final class Icons extends JPanel implements Scrollable {
        final DesktopHome home;final Where where;
        final List<Tile> tiles=new ArrayList<>();
        /** Words above the icons where there are none: what to do. */
        private JComponent words;
        /** Where a carried icon would land: the empty cell under the pointer, drawn as the quiet outline of an icon. */
        Rectangle landing;
        /** Where each icon stands, {row, column} by id, for the columns it was worked out for: again when they change. */
        private Map<String,int[]> drawn=Map.of();private int drawnFor=-1;
        /** Home's page in view, across and down from the centre one (decision 45); a card has one grid and no pages. */
        int pageX,pageY;
        /** The page of the cell last found under the pointer. A drop uses this, which can be the page beside the one in the middle. */
        int landX,landY;
        /** One empty page opened while something is carried past the last page there is, so it can be let go there. */
        private boolean past;private int pastX,pastY;
        /** Set while the view is being placed, so that does not count as the reader scrolling. */
        private boolean placing;private double logicalX,logicalY;private boolean anchored;private int oldW,oldH;
        /** Where every icon of Home stands on every page, for the columns and rows it was worked out for. */
        private Map<String,Layout.Spot> spots=Map.of();private int spotsFor=-1;
        /** Whether this grid is Home's, in pages; a card's is one grid that scrolls down (decision 49). */
        boolean paged(){return where==Where.HOME;}
        boolean onCentre(){return !paged()||pageX==0&&pageY==0;}
        /** How many rows a page holds: as many as the room shows (decision 48). */
        int rows() {
            Container up=getParent();int h=up!=null&&up.getHeight()>0?up.getHeight():getHeight();
            return HomeMotion.rows(h-TOP*2,HIGH);
        }
        /**
         * Where every icon of Home stands, on every page (Layout.pages): Favourites first while it has no cell, the archive
         * and the bin after the last icon of the centre page.
         */
        Map<String,Layout.Spot> spots() {
            int now=columns()*1000+rows();
            if(now!=spotsFor) {
                List<String> ids=new ArrayList<>();List<Integer> onPages=new ArrayList<>(),cells=new ArrayList<>();
                Tile first=null;
                for(Tile one:tiles)if(one.thing.kind==NoteStore.Branch.Kind.FAVOURITES&&one.thing.cell<0)first=one;
                if(first!=null){ids.add(first.thing.id);onPages.add(Layout.NO_PAGE);cells.add(Layout.NONE);}
                for(Tile one:tiles)if(one!=first){ids.add(one.thing.id);onPages.add(one.thing.page);cells.add(one.thing.cell);}
                spots=Layout.pages(ids,onPages,cells,java.util.Set.of(NoteStore.ARCHIVE,NoteStore.BIN,NoteStore.TOOLS,NoteStore.TEMP,NoteStore.RECENT),columns(),rows());spotsFor=now;
            }
            return spots;
        }
        /** The page that way, where there is one; else nothing changes and nothing is said. The keys' way, a whole page. */
        boolean turn(int dx,int dy) {
            int[] to=Layout.next(Layout.active(spots()),pageX,pageY,dx,dy);
            if(to==null)return false;
            showPage(to[0],to[1]);return true;
        }
        /** A page in view: scrolled so its top left is the room's, any of them, an empty one beyond the edge while something is carried there. */
        void showPage(int x,int y) {
            if(!paged())return;
            int nx=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,x)),ny=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,y));
            boolean changed=nx!=pageX||ny!=pageY;
            pageX=nx;pageY=ny;landX=nx;landY=ny;
            if(changed){landing=null;drawnFor=-1;}
            scrollTo(nx,ny);
            if(changed)home.pageShown();
        }
        /** How wide and tall one page is: the room it is seen in, and as many rows as that room shows. */
        int pageW(){int w=viewWidth();return w>0?w:CELL+2*SIDE;}
        int pageH(){return Math.max(HIGH,rows()*HIGH);}
        private int viewWidth() {
            Container up=getParent();
            if(up instanceof JViewport v&&v.getWidth()>0)return v.getWidth();
            if(up!=null&&up.getWidth()>0)return up.getWidth();
            return Math.max(getWidth(),CELL+2*SIDE);
        }
        private int viewHeight() {
            Container up=getParent();
            if(up instanceof JViewport v&&v.getHeight()>0)return v.getHeight();
            if(up!=null&&up.getHeight()>0)return up.getHeight();
            return Math.max(getHeight(),4*HIGH);
        }
        /** The pages drawn, and the empty one opened past them while something is carried there. */
        private int[] spanNow() {
            int[] s=Layout.span(spots());
            if(past){s[0]=Math.min(s[0],pastX);s[1]=Math.max(s[1],pastX);s[2]=Math.min(s[2],pastY);s[3]=Math.max(s[3],pastY);}
            return s;
        }
        /** Whether the page in view is the empty one opened past the last, while something is carried there. */
        boolean onPast(){return past&&pageX==pastX&&pageY==pastY;}
        /** One page that way, past the last there is, so a thing carried to the edge can be let go on it. Once only. */
        boolean openBeyond(int dx,int dy) {
            int[] s=Layout.span(spots());
            int nx=dx<0?s[0]-1:dx>0?s[1]+1:pageX,ny=dy<0?s[2]-1:dy>0?s[3]+1:pageY;
            if(past&&pastX==nx&&pastY==ny)return false;
            double x=logicalX,y=logicalY;past=true;pastX=nx;pastY=ny;placing=true;revalidate();placeNow();restoreLogical(x,y);placing=false;return true;
        }
        void clearPast(){if(!past)return;double x=logicalX,y=logicalY;past=false;placing=true;revalidate();placeNow();restoreLogical(x,y);placing=false;followScroll();}
        /** Scrolled so that page's top left is the room's top left. */
        void returnCentre() {
            JViewport port=getParent() instanceof JViewport v?v:null;if(port==null)return;
            int[] s=spanNow();java.awt.Point p=port.getViewPosition();
            home.motion.to(-s[0]*pageW()-p.x,-s[2]*pageH()-p.y);
        }
        private void scrollTo(int x,int y) {
            JViewport port=getParent() instanceof JViewport v?v:null;
            if(port==null)return;
            int[] s=spanNow();
            int sx=Math.max(0,(x-s[0])*pageW()),sy=Math.max(0,(y-s[2])*pageH());
            Dimension size=getPreferredSize(),seen=port.getExtentSize();
            sx=Math.min(sx,Math.max(0,size.width-seen.width));sy=Math.min(sy,Math.max(0,size.height-seen.height));
            placing=true;port.setViewPosition(new java.awt.Point(sx,sy));placing=false;rememberPosition();
        }
        /** The view moved by so many pixels. False where it was already at the end that way. */
        boolean scrollBy(int dx,int dy) {
            JViewport port=getParent() instanceof JViewport v?v:null;
            if(port==null||dx==0&&dy==0)return false;
            java.awt.Point p=port.getViewPosition();Dimension size=getPreferredSize(),seen=port.getExtentSize();
            int x=Math.max(0,Math.min(p.x+dx,Math.max(0,size.width-seen.width)));
            int y=Math.max(0,Math.min(p.y+dy,Math.max(0,size.height-seen.height)));
            if(x==p.x&&y==p.y)return false;
            placing=true;port.setViewPosition(new java.awt.Point(x,y));placing=false;
            followScroll();return true;
        }
        /** The page under the middle of the room, once the reader has scrolled. Favourites and the search follow it. */
        private void followScroll() {
            if(placing||!paged())return;
            rememberPosition();
            JViewport port=getParent() instanceof JViewport v?v:null;
            if(port==null||pageW()<=0||pageH()<=0)return;
            java.awt.Point p=port.getViewPosition();int[] s=spanNow();
            int nx=s[0]+(p.x+Math.max(1,port.getWidth())/2)/pageW();
            int ny=s[2]+(p.y+Math.max(1,port.getHeight())/2)/pageH();
            nx=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,nx));ny=Math.max(-Layout.MIDDLE,Math.min(Layout.MIDDLE-1,ny));
            if(nx==pageX&&ny==pageY)return;
            pageX=nx;pageY=ny;drawnFor=-1;
            // While a thing is carried the bar says where to let go. The search and the dock still follow the page.
            if(home.carry.carrying())home.showFoot();else home.pageShown();
        }
        private void restoreLogical(double x,double y){
            if(!paged()||!(getParent() instanceof JViewport port))return;
            int[] span=spanNow();Dimension size=getPreferredSize(),seen=port.getExtentSize();
            port.setViewPosition(new java.awt.Point(Math.max(0,Math.min((int)Math.round((x-span[0])*pageW()),Math.max(0,size.width-seen.width))),Math.max(0,Math.min((int)Math.round((y-span[2])*pageH()),Math.max(0,size.height-seen.height)))));
            oldW=pageW();oldH=pageH();logicalX=span[0]+port.getViewPosition().x/(double)oldW;logicalY=span[2]+port.getViewPosition().y/(double)oldH;anchored=true;
        }
        private void rememberPosition(){
            if(!paged()||!(getParent() instanceof JViewport port))return;
            // Resize events come after the extent has changed. Do not measure old pixels with new geometry.
            int w=pageW(),h=pageH();if(anchored&&(w!=oldW||h!=oldH))return;
            int[] span=spanNow();java.awt.Point p=port.getViewPosition();logicalX=span[0]+p.x/(double)w;logicalY=span[2]+p.y/(double)h;oldW=w;oldH=h;anchored=true;
        }
        Icons(DesktopHome home,Where where) {
            super(null);this.home=home;this.where=where;setOpaque(false);
            // Wider or narrower can mean a different number to a row, and so a different height: laid out again at once. On
            // Home a taller or shorter room is a bigger or smaller page too (decision 48). The room is the viewport, not
            // the canvas, which is wider than the room once there is a page beside this one.
            addHierarchyListener(ev->{
                if((ev.getChangeFlags()&java.awt.event.HierarchyEvent.PARENT_CHANGED)==0||!(getParent() instanceof JViewport port))return;
                if(port.getClientProperty("mininotes.follow")!=null)return;
                port.putClientProperty("mininotes.follow",Boolean.TRUE);
                port.addChangeListener(e->followScroll());
                port.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){
                    if(oldW==pageW()&&oldH==pageH())return;
                    double x=anchored?logicalX:pageX,y=anchored?logicalY:pageY;spotsFor=-1;
                    placing=true;revalidate();placeNow();
                    if(paged()){int[] span=spanNow();Dimension size=getPreferredSize(),seen=port.getExtentSize();
                        port.setViewPosition(new java.awt.Point(Math.max(0,Math.min((int)Math.round((x-span[0])*pageW()),Math.max(0,size.width-seen.width))),Math.max(0,Math.min((int)Math.round((y-span[2])*pageH()),Math.max(0,size.height-seen.height)))));}
                    placing=false;oldW=pageW();oldH=pageH();followScroll();
                }});
            });
        }
        /**
         * The icons put in their places now, for the width the grid has, and the scroll pane round it sized to them. Left
         * to Swing's own pass, a grid filled again as the tree came or the window changed size could be pictured - and
         * seen - with every icon still at no size at all: that pass was not always run before the next paint.
         */
        private void placeNow() {
            Container scroll=SwingUtilities.getAncestorOfClass(JScrollPane.class,this);
            if(scroll!=null&&scroll.isShowing())scroll.validate();
            doLayout();repaint();
        }
        int columns(){return DesktopHome.columns(width());}
        private int width(){if(paged())return viewWidth();Container up=getParent();return getWidth()>0?getWidth():up!=null&&up.getWidth()>0?up.getWidth():CELL+2*SIDE;}
        /** The columns stand in the middle of the width, the icons from the left of them, as a phone's grid does. */
        private int left(){return Math.max(SIDE,(width()-columns()*CELL)/2);}
        /** Room over the first row: on Home's pages none for the words, which stand under the icons, so every page is one size. */
        private int top(){return TOP+(paged()||words==null?0:words.getPreferredSize().height+12);}

        /**
         * Where each icon stands for the columns the grid has now. A narrower grid than the one a cell was chosen on moves
         * that icon to the first free cell after its row, without writing anything: its own cell is kept for when there is
         * room again, until something on the grid is moved.
         */
        Map<String,int[]> drawn() {
            if(paged()) {
                // Those on the page in view, as {row, column}: what the keyboard, the cells and the carrying go by.
                Map<String,int[]> here=new java.util.LinkedHashMap<>();
                for(Map.Entry<String,Layout.Spot> one:spots().entrySet())
                    if(one.getValue()!=null&&one.getValue().on(pageX,pageY))here.put(one.getKey(),new int[]{one.getValue().row(),one.getValue().column()});
                return here;
            }
            int cols=columns();
            if(cols!=drawnFor) {
                List<String> ids=new ArrayList<>();List<Integer> cells=new ArrayList<>();
                // Among the favourites, the archive and the bin a thing is only listed, not kept, so it has no cell there: they
                // stand one after another.
                boolean kept=home.places(this);
                // The places among Home's icons where each was put (Layout.home): Favourites first while it has no cell of its
                // own, the archive and the bin after everything - as the phone has it.
                Map<String,Integer> places=new java.util.LinkedHashMap<>();
                for(Tile one:tiles) {
                    if(Grid.place(one.thing.kind)){places.put(one.thing.id,one.thing.cell);continue;}
                    ids.add(one.thing.id);cells.add(kept?one.thing.cell:Layout.NONE);
                }
                drawn=Layout.home(ids,cells,places,NoteStore.FAVOURITES,cols);drawnFor=cols;
            }
            return drawn;
        }
        /** Laid out again from the cells the icons have now: after one is put somewhere. */
        void rearrange(){drawnFor=-1;spotsFor=-1;revalidate();placeNow();}
        /** A cell's place on the page in view. */
        Rectangle cellBounds(int row,int column){return boundsOn(pageX,pageY,row,column);}
        /** A cell's place on a page, in the canvas. */
        Rectangle boundsOn(int px,int py,int row,int column) {
            if(!paged())return new Rectangle(left()+column*CELL,top()+row*HIGH,CELL,HIGH);
            int[] s=spanNow();
            return new Rectangle((px-s[0])*pageW()+left()+column*CELL,TOP+(py-s[2])*pageH()+row*HIGH,CELL,HIGH);
        }
        /** The cell under a point of the screen, whether anything is in it or not (see cellUnder). On Home, the page too. */
        int[] cellAt(java.awt.Point screen) {
            if(!paged()) {
                JViewport port=getParent() instanceof JViewport v?v:null;
                java.awt.Point seen=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(seen,port!=null?port:this);
                return cellUnder(seen,port!=null?port.getViewPosition():new java.awt.Point(0,0),left(),top(),columns());
            }
            java.awt.Point on=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(on,this);
            int[] s=spanNow();
            int[] hit=Layout.underCanvas(on.x,on.y,pageW(),pageH(),left(),TOP,CELL,HIGH,columns(),rows(),s[0],s[2],s[1],s[3]);
            if(hit==null)return null;
            landX=hit[0];landY=hit[1];
            return new int[]{hit[2],hit[3]};
        }
        /** The icon in a cell, or null for an empty one. */
        Tile in(int row,int column) {
            String id=paged()?Layout.at(spots(),landX,landY,row,column):Layout.at(drawn(),row,column);
            if(id!=null)for(Tile one:tiles)if(one.thing.id.equals(id))return one;
            return null;
        }
        /** The first icon in reading order: where the keyboard starts. */
        Tile first() {
            if(tiles.isEmpty())return null;
            String id=stepTo(drawn(),tiles.get(0).thing.id,Does.FIRST);
            for(Tile one:tiles)if(one.thing.id.equals(id))return one;
            return tiles.get(0);
        }
        /** The keyboard to the icon of this thing. */
        void focus(String id){for(Tile one:tiles)if(one.thing.id.equals(id)){one.requestFocusInWindow();return;}}

        /**
         * Drawn again from these lines, keeping what the reader is looking at: where the grid is scrolled to and the icon
         * that has the keyboard stay where they were - an arrival is not a reason to lose your place.
         */
        void fill(List<NoteStore.Branch> lines,String nothing) {
            Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            String focused=focus instanceof Tile t&&t.getParent()==this?t.thing.id:null;
            JViewport port=getParent() instanceof JViewport v?v:null;
            java.awt.Point kept=port==null?null:port.getViewPosition();
            double keepX=anchored?logicalX:pageX,keepY=anchored?logicalY:pageY;
            removeAll();tiles.clear();words=null;landing=null;drawnFor=-1;spotsFor=-1;
            if(nothing!=null){words=DesktopUi.note(nothing,Math.max(200,Math.min(460,width()-2*SIDE)),DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));add(words);}
            // In the archive's or the bin's card a thing waits to be put back: its icon says so, and offers where it can go.
            // In Temp's or Recent's, it is listed as a favourite is, opening where it really is.
            Where here=where==Where.CARD&&home.folder!=null&&home.folder.away()?Where.AWAY
                :where==Where.CARD&&home.folder!=null&&home.folder.lists()?Where.FAVOURITES:where;
            for(NoteStore.Branch one:lines){Tile tile=home.tile(one,here);tiles.add(tile);add(tile);}
            revalidate();placeNow();
            if(focused!=null)for(Tile t:tiles)if(t.thing.id.equals(focused)){t.requestFocusInWindow();break;}
            if(port!=null&&kept!=null)SwingUtilities.invokeLater(()->{if(getParent()==port){Dimension view=port.getViewSize(),seen=port.getExtentSize();
                if(paged()){placing=true;restoreLogical(keepX,keepY);placing=false;followScroll();}
                else port.setViewPosition(new java.awt.Point(0,Math.max(0,Math.min(kept.y,Math.max(0,view.height-seen.height)))));}});
        }
        @Override public void doLayout() {
            int x0=left(),y0=top();
            if(paged()) {
                int[] s=spanNow();Map<String,Layout.Spot> at=spots();
                if(words!=null) {
                    Dimension d=words.getPreferredSize();
                    int y=TOP+(0-s[2])*pageH()+HIGH+8;
                    words.setBounds((0-s[0])*pageW()+Math.max(SIDE,(pageW()-d.width)/2),y,d.width,d.height);words.setVisible(true);
                }
                for(Tile one:tiles) {
                    Layout.Spot spot=at.get(one.thing.id);
                    if(spot!=null){one.setBounds((spot.x()-s[0])*pageW()+left()+spot.column()*CELL,TOP+(spot.y()-s[2])*pageH()+spot.row()*HIGH,CELL,HIGH);one.setVisible(true);}
                    else one.setVisible(false);
                }
                return;
            }
            Map<String,int[]> at=drawn();
            if(words!=null) {
                Dimension d=words.getPreferredSize();
                // In a card, over the icons.
                words.setBounds(Math.max(SIDE,(width()-d.width)/2),TOP,d.width,d.height);words.setVisible(true);
            }
            for(Tile one:tiles) {
                int[] cell=at.get(one.thing.id);
                if(cell!=null){one.setBounds(x0+cell[1]*CELL,y0+cell[0]*HIGH,CELL,HIGH);one.setVisible(true);}
            }
        }
        @Override public Dimension getPreferredSize() {
            // Home is every page there is, edge to edge. Past the last page, enough to scroll the last name clear of the
            // bars that float over the icons (decision 128).
            if(paged()) {
                int[] s=spanNow();
                int w=Math.max(pageW(),(s[1]-s[0]+1)*pageW());
                int clear=Math.max(HIGH,DOCKED+home.search.getPreferredSize().height+30);
                int h=TOP+(s[3]-s[2]+1)*pageH()+clear+TOP;
                return new Dimension(w,Math.max(h,viewHeight()+clear));
            }
            // One empty row under the last icon, so a thing can always be put below the rest. It is also the room the + floats
            // over, which kept the last icons clear of it before there was an empty row.
            int rows=Layout.rows(drawn())+1;
            return new Dimension(columns()*CELL+2*SIDE,top()+rows*HIGH+TOP);
        }
        @Override protected void paintChildren(Graphics g0) {
            super.paintChildren(g0);
            if(paged())DesktopPages.dots((Graphics2D)g0,this,Layout.dotted(spots(),pageX,pageY),pageX,pageY);
            if(landing==null)return;
            // Where a carried icon will land: the outline of an icon, quiet, so it reads as a place and not a thing.
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int x=landing.x+(landing.width-FACE)/2,y=landing.y+10,arc=Math.max(10,FACE*18/64);
            g.setColor(DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.PAPER,0.9f));g.fillRoundRect(x,y,FACE,FACE,arc,arc);
            g.setColor(DesktopUi.ACCENT);
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(x,y,FACE-1,FACE-1,arc,arc);
            g.dispose();
        }
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-48);}
        public boolean getScrollableTracksViewportWidth(){return !paged();}
        /** A card is never shorter than what shows of it. Home is as tall as its pages, so it can scroll. */
        public boolean getScrollableTracksViewportHeight(){return !paged()&&getParent() instanceof JViewport port&&port.getHeight()>getPreferredSize().height;}
    }

    /** One icon, made with everything it answers to: a click, the keyboard, a right-click, and being carried. */
    Tile tile(NoteStore.Branch thing,Where where) {
        Tile tile=new Tile(this,thing,where);
        MouseAdapter hand=new MouseAdapter(){
            public void mousePressed(MouseEvent e){tile.requestFocusInWindow();if(e.isPopupTrigger()){menuFor(thing,where).show(tile,e.getX(),e.getY());return;}carry.press(tile,e);}
            public void mouseReleased(MouseEvent e){boolean carried=carry.carrying();carry.release(e);if(!carried&&e.isPopupTrigger())menuFor(thing,where).show(tile,e.getX(),e.getY());}
            // A drag let go on an icon is not a click on it. In the archive or the bin, a click asks where it goes from there.
            public void mouseClicked(MouseEvent e) {
                if(!SwingUtilities.isLeftMouseButton(e)||carry.moved())return;
                if(thing.asking||where==Where.AWAY)menuFor(thing,where).show(tile,e.getX(),e.getY());else opened(thing,where);
            }
            public void mouseDragged(MouseEvent e){carry.drag(e);}
            public void mouseEntered(MouseEvent e){tile.over=true;tile.repaint();}
            public void mouseExited(MouseEvent e){tile.over=false;tile.repaint();}
        };
        tile.addMouseListener(hand);tile.addMouseMotionListener(hand);
        tile.addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){
            Does does=does(e.getKeyCode(),e.isShiftDown(),e.isControlDown()||e.isAltDown());
            switch(does) {
                case OPEN: if(thing.asking||where==Where.AWAY)menuFor(thing,where).show(tile,16,tile.getHeight()-12);else opened(thing,where);break;
                case MENU: menuFor(thing,where).show(tile,16,tile.getHeight()-12);break;
                case RENAME: if(where!=Where.AWAY&&(thing.kind==NoteStore.Branch.Kind.PAGE||DesktopMoving.shelf(thing.kind)))pad.rename(thing);break;
                case BIN:
                    if(where==Where.AWAY)break;
                    if(where!=Where.FAVOURITES&&where!=Where.DOCK&&(thing.kind==NoteStore.Branch.Kind.PAGE||thing.kind==NoteStore.Branch.Kind.FILE||DesktopMoving.shelf(thing.kind)))pad.save(()->pad.putAway(thing,true));
                    break;
                case NONE: return;
                case MOVE_LEFT: case MOVE_RIGHT: case MOVE_UP: case MOVE_DOWN:
                    if(tile.getParent() instanceof Icons in)nudge(in,tile,does);
                    else return;
                    break;
                default: {
                    // Between the icons, over the empty cells; along the dock, its one row.
                    if(tile.getParent() instanceof Icons in)in.focus(stepTo(in.drawn(),thing.id,does));
                    else if(tile.getParent() instanceof Dock row)row.focusAt(step(row.tiles.indexOf(tile),row.tiles.size(),row.tiles.size(),does));
                }
            }
            e.consume();
        }});
        return tile;
    }

    /**
     * One icon: its face, its name under it in two lines at most, its mark on the corner and, where it is a favourite, a
     * star on the other - the corners are the one part of a picture that is never the thing itself - and a <i>new</i> on
     * a file that came and has not been opened. In the dock, the face alone: its name is said to a screen reader and under
     * the pointer instead, as a phone's dock has no words.
     */
    static final class Tile extends JPanel {
        final DesktopHome home;final NoteStore.Branch thing;final Where where;
        boolean over,lifted,target;
        Tile(DesktopHome home,NoteStore.Branch thing,Where where) {
            super(null);this.home=home;this.thing=thing;this.where=where;
            setOpaque(false);setFocusable(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            putClientProperty(THING,thing);putClientProperty(MENU,(Supplier<JPopupMenu>)()->home.menuFor(thing,where));
            getAccessibleContext().setAccessibleName(where==Where.DOCK?named(thing):where==Where.AWAY?named(thing)+", "+thing.detail+". Put it back, or "+(home.folder.inBin()?"delete it for good":"move it to the bin"):said(thing));
            setToolTipText(where==Where.DOCK?named(thing):thing.kind==NoteStore.Branch.Kind.FILE&&thing.detail!=null?named(thing)+"  ·  "+thing.detail:null);
            addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){home.lastTile=Tile.this;repaint();if(e.getCause()!=FocusEvent.Cause.MOUSE_EVENT)scrollRectToVisible(new Rectangle(getSize()));}public void focusLost(FocusEvent e){repaint();}});
        }
        boolean docked(){return where==Where.DOCK;}
        int side(){return docked()?DOCK_FACE:FACE;}
        /** The face's square, in this icon's own place. */
        Rectangle faceAt(){int s=side();return new Rectangle((getWidth()-s)/2,docked()?(getHeight()-s)/2:10,s,s);}
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
            if(lifted){g.dispose();return;}
            // Pointed at, or reached with the keyboard: a soft ground under the whole icon, and a ring while it has the keyboard.
            boolean ring=ringed(this);
            if(over||ring){g.setColor(DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.PAPER,0.91f));g.fillRoundRect(3,2,getWidth()-6,getHeight()-4,16,16);}
            if(ring){g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(1.6f));g.drawRoundRect(3,2,getWidth()-7,getHeight()-5,16,16);}
            Rectangle f=faceAt();
            face(g,thing,f.x,f.y,f.width,home.pad.usual);
            // Where a carried thing would go into or onto: this one ringed.
            if(target){g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(3f));g.drawRoundRect(f.x-4,f.y-4,f.width+7,f.height+7,22,22);}
            int badge=docked()?16:20;
            // A file wears its mark only once it is shared on its own (decision 92): the many only here stay as they were.
            if(thing.scope()!=null&&(thing.kind!=NoteStore.Branch.Kind.FILE||thing.state!=null&&thing.state!=Sharing.State.HERE))
                new DesktopMark(thing.mark(),badge,true).paintIcon(this,g,f.x+f.width-badge+badge/3,f.y-badge/3);
            // Temp's own mark, as a folder wears one, while anything in it reaches somebody (the owner, 2026-10-14: "it seems
            // it was the temp folder missing one mark"); its count then stands just left of it.
            SyncMark tempMark=thing.kind==NoteStore.Branch.Kind.TEMP?home.pad.marks.get(NoteStore.TEMP):null;
            boolean tempMarked=tempMark!=null&&tempMark!=SyncMark.HERE;
            if(tempMarked)new DesktopMark(tempMark,badge,true).paintIcon(this,g,f.x+f.width-badge+badge/3,f.y-badge/3);
            if(thing.kept&&where!=Where.FAVOURITES&&where!=Where.DOCK&&where!=Where.AWAY) {
                int d=badge-2,sx=f.x-d/3,sy=f.y-d/3;
                g.setColor(DesktopUi.PAPER);g.fillOval(sx,sy,d,d);g.setColor(DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawOval(sx,sy,d-1,d-1);
                star(g,sx+d/2f,sy+d/2f,d*0.34f,DesktopUi.INK);
            }
            // Temporary: a timer on the corner under the star's, as the star says a favourite (decision 93); Lucide's "timer",
            // which Temp itself wears.
            if(thing.temporary&&where!=Where.FAVOURITES&&where!=Where.DOCK&&where!=Where.AWAY) {
                int d=badge-2,sx=f.x-d/3,sy=f.y+f.height-d+d/3;
                g.setColor(DesktopUi.PAPER);g.fillOval(sx,sy,d,d);g.setColor(DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawOval(sx,sy,d-1,d-1);
                DesktopIcons.draw(g,"timer",sx+d*0.17f,sy+d*0.17f,d*0.66f,DesktopUi.INK);
            }
            // Kept as a file too (decision 162): a small file on the corner none of the others takes, with a tick while all is
            // well, and in the warning colour with its mark while its file is lost.
            String onDisk=home.pad.onDisk==null?null:home.pad.onDisk.state(thing.id);
            // A file linked to one on this PC (Share a file through Mininotes) wears a link instead, broken while its syncing
            // is switched off (the owner, 2026-10-16: linked files distinct).
            boolean linked=thing.kind==NoteStore.Branch.Kind.FILE&&home.pad.onDisk!=null&&home.pad.onDisk.pathOf(thing.id)!=null&&!home.pad.onDisk.underFolder(thing.id);
            boolean off=linked&&home.pad.onDisk.syncOff(thing.id);
            if((onDisk!=null||off)&&where!=Where.DOCK&&where!=Where.AWAY) {
                int d=badge-2,sx=f.x+f.width-d+d/3,sy=f.y+f.height-d+d/3;boolean lost=onDisk!=null&&!onDisk.isEmpty();
                Color warning=DesktopMark.ink(NoteLine.Tone.FAILED.mark);
                g.setColor(lost?DesktopUi.mix(warning,DesktopUi.PAPER,0.85f):DesktopUi.PAPER);g.fillOval(sx,sy,d,d);
                g.setColor(lost?warning:DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawOval(sx,sy,d-1,d-1);
                // A folder that lives on this PC, its files kept there and in step (decision 197), wears a folder with arrows.
                boolean outside=DesktopMoving.shelf(thing.kind)&&!home.pad.onDisk.underFolder(thing.id);
                String mark=lost?"file-exclamation-point":off?"link-2-off":linked?"link":outside?"folder-sync":"file-check";
                DesktopIcons.draw(g,mark,sx+d*0.17f,sy+d*0.17f,d*0.66f,lost?warning:off?DesktopUi.QUIET:DesktopUi.INK);
            }
            // How many things wait in the archive or the bin, on the corner where a thing wears its mark: quiet, since a full
            // bin is not news, and nothing when it is empty.
            int many=thing.kind==NoteStore.Branch.Kind.ARCHIVE||thing.kind==NoteStore.Branch.Kind.BIN||thing.kind==NoteStore.Branch.Kind.TEMP
                ||thing.kind==NoteStore.Branch.Kind.SHARED||thing.kind==NoteStore.Branch.Kind.MESSAGES?countIn(thing.detail):0;
            // Ink on paper, bold, the number in the middle of its pill and the pill whole inside the tile (the owner, 2026-10-14:
            // "the 1 in Temp is not well displayed": a pale pill on the rim, the number low, lost on a coloured Home).
            if(many>0)pill(g,many>99?"99+":String.valueOf(many),DesktopUi.INK,DesktopUi.PAPER,tempMarked?new Rectangle(f.x,f.y,f.width-badge,f.height):f,getWidth(),false);
            // New: a file that came, or a note somebody else changed, or a folder with one in it, not opened since (the owner,
            // 2026-10-14: "the same everywhere a change hasn't been viewed"). Over the middle of the top edge, clear of the mark.
            if(thing.fresh)pill(g,Say.onNote("new"),DesktopUi.ACCENT,Color.WHITE,f,getWidth(),true);
            // Updated on another of my own devices and not opened here since: a check mark in the same place (decision 220).
            else if(thing.updated)pill(g,"✓",DesktopUi.ACCENT,Color.WHITE,f,getWidth(),true);
            // Shared on its own and still going up: said on its face, until its bytes are up and its sleeve has gone
            // (decision 94). Quiet, as a count is: it is busy, not wrong.
            if(thing.kind==NoteStore.Branch.Kind.FILE&&thing.uploading) {
                g.setFont(DesktopUi.BODY.deriveFont(11f));FontMetrics m=g.getFontMetrics();
                int w=m.stringWidth(Say.t("uploading"))+12,h=17,nx=f.x+(f.width-w)/2,ny=f.y+f.height-h+7;
                g.setColor(DesktopUi.PAPER);g.fillRoundRect(nx,ny,w,h,h,h);
                g.setColor(DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawRoundRect(nx,ny,w-1,h-1,h,h);
                g.setColor(DesktopUi.INK);g.drawString(Say.t("uploading"),nx+6,ny+(h-m.getHeight())/2+m.getAscent());
            }
            // Stamped on Minima (decision 249): a stamp on its lower right corner, on a paper disc, in the accent once Minima
            // has it and quiet while it is on its way; its menu's first line says which.
            if(thing.kind==NoteStore.Branch.Kind.FILE&&thing.stamped!=NoteStore.Branch.STAMP_NONE) {
                int d=22,sx=f.x+f.width-d+6,sy=f.y+f.height-d+6;
                g.setColor(DesktopUi.PAPER);g.fillOval(sx,sy,d,d);
                g.setColor(DesktopUi.LINE.darker());g.setStroke(new BasicStroke(1f));g.drawOval(sx,sy,d-1,d-1);
                Graphics2D t=(Graphics2D)g.create();
                DesktopIcons.draw(t,"stamp",sx+4,sy+4,14,thing.stamped==NoteStore.Branch.STAMP_ON_MINIMA?DesktopUi.ACCENT:DesktopUi.QUIET);
                t.dispose();
            }
            if(!docked()) {
                g.setFont(DesktopUi.BODY);g.setColor(DesktopUi.INK);FontMetrics m=g.getFontMetrics();
                int y=f.y+f.height+8+m.getAscent();
                for(String line:lines(named(thing),m,getWidth()-14,2)){g.drawString(line,(getWidth()-m.stringWidth(line))/2f,y);y+=m.getHeight();}
            }
            g.dispose();
        }
    }

    /**
     * A word or a count over a tile's top edge, at its right corner or in its middle: bold, centred in its pill by the
     * letters' own height, and kept inside the tile, so no edge cuts it off.
     */
    static void pill(Graphics2D g,String said,Color ground,Color ink,Rectangle face,int wide,boolean middle) {
        g.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,12f));FontMetrics m=g.getFontMetrics();
        int h=20,w=Math.max(h,m.stringWidth(said)+12);
        int nx=middle?face.x+(face.width-w)/2:Math.min(face.x+face.width-w+9,wide-w-1),ny=Math.max(face.y-8,1);
        g.setColor(ground);g.fillRoundRect(nx,ny,w,h,h,h);
        g.setColor(ground.darker());g.setStroke(new BasicStroke(1f));g.drawRoundRect(nx,ny,w-1,h-1,h,h);
        g.setColor(ink);
        g.drawString(said,nx+(w-m.stringWidth(said))/2f,ny+(h-m.getAscent()-m.getDescent())/2f+m.getAscent());
    }

    // ---- the dock -----------------------------------------------------------------------------------------------------

    /**
     * The dock, at the foot of Home: as many favourites as fit (decision 3), faces only, as a phone's dock is - each says
     * its name under the pointer and to a screen reader. A click opens it; its right-click is its menu, with Remove from
     * the dock. A note or a collection carried here becomes a favourite at the place it is let go. With nothing in it,
     * it says Favourites, quietly, so the empty strip is not a mystery.
     */
    final class Dock extends JPanel {
        final List<Tile> tiles=new ArrayList<>();
        boolean lit;
        private final JLabel empty=new JLabel(Say.t("Favourites"));
        Dock() {
            super(new FlowLayout(FlowLayout.CENTER,0,0));setOpaque(false);setBorder(BorderFactory.createEmptyBorder(4,DOCK_ENDS,4,DOCK_ENDS));
            empty.setFont(DesktopUi.BODY.deriveFont(13f));empty.setForeground(DesktopUi.QUIET);empty.setBorder(BorderFactory.createEmptyBorder(0,40,0,40));
            empty.setPreferredSize(new Dimension(empty.getPreferredSize().width+80,DOCKED));
            empty.setToolTipText(Say.t("Favourites go here: carry a note or a folder here, or choose Add to favourites from its menu"));
            getAccessibleContext().setAccessibleName("The dock: your favourites");
        }
        void fill(List<NoteStore.Branch> shown) {
            Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            String focused=focus instanceof Tile t&&t.getParent()==this?t.thing.id:null;
            removeAll();tiles.clear();
            if(shown.isEmpty())add(empty);
            for(NoteStore.Branch one:shown){Tile tile=tile(one,Where.DOCK);tile.setPreferredSize(new Dimension(DOCKED,DOCKED));tiles.add(tile);add(tile);}
            // Placed now, as Home's grid is, rather than left for a later pass with the icons at no size.
            revalidate();Container foot=getParent()==null?null:getParent().getParent();
            if(foot!=null&&foot.isShowing())foot.validate();
            repaint();
            if(focused!=null)for(Tile t:tiles)if(t.thing.id.equals(focused))t.requestFocusInWindow();
        }
        void focusAt(int at){if(at>=0&&at<tiles.size())tiles.get(at).requestFocusInWindow();}
        void lit(boolean on){if(lit!=on){lit=on;repaint();if(getParent()!=null&&getParent().getParent()!=null)getParent().getParent().repaint();}}
        /** Its shell, drawn by the strip it scrolls in, so the rounded ends stay where they are while the icons go across. */
        void shell(Graphics g0,int w,int h) {
            // On Home's own ground, no box of its own (the owner, 2026-10-18: "let's put the elements of the dock directly on
            // the background, not in a different colour box"); ringed only while something carried can be let go on it.
            if(!lit)return;
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(lit?DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.PAPER,0.9f):DesktopUi.SHELF);g.fillRoundRect(0,0,w-1,h-1,24,24);
            g.setColor(lit?DesktopUi.ACCENT:DesktopUi.LINE);g.setStroke(new BasicStroke(lit?2f:1f));g.drawRoundRect(0,0,w-1,h-1,24,24);
            g.dispose();
        }
        /** The place in the dock, 1 the first, that a thing let go at this point of the screen takes (see Grid.dockSlot). */
        int slotAt(java.awt.Point screen,Tile carried) {
            List<Float> middles=new ArrayList<>();int dragged=-1;
            for(Tile one:tiles){if(one==carried)dragged=middles.size();java.awt.Point at=new java.awt.Point(one.getWidth()/2,0);SwingUtilities.convertPointToScreen(at,one);middles.add((float)at.x);}
            float[] all=new float[middles.size()];for(int i=0;i<all.length;i++)all[i]=middles.get(i);
            return Grid.dockSlot(screen.x,all,dragged);
        }
    }

    // ---- carrying -----------------------------------------------------------------------------------------------------

    /**
     * An icon taken hold of and carried, wherever it is picked up: the pointer is measured against the dock, an open card,
     * the dimmed Home round it and Home's grid, and letting go does what {@link Grid} says it does there. The icon in hand
     * is drawn pale where it was, its name beside the pointer; what it would go onto is ringed, the empty cell it would
     * land in is the quiet outline of an icon, and the bar says the same in words. Esc puts it down where it was.
     */
    final class Carry {
        private Tile pressed,carried,target,inTheWay;private java.awt.Point pressedAt;private boolean moved;
        /** Held at an edge of Home's page: the page beyond comes after a moment, a new one if there is none (decision 46). */
        private int edgeX,edgeY;private java.awt.Point last;
        /** What the bar says while it is held at an edge, or null; and whether this hold has turned its page already. */
        private String edgeSaid;private boolean turnedAt;
        private JLabel ghost;private java.awt.Point pickup;private KeyEventDispatcher escape;
        /** The grid and the cell it would land in, where that is empty - or its own cell, where it would stay. */
        private Grid.Zone zone=Grid.Zone.NONE;private Icons markedIn;private int[] cell;private boolean stays;private String said="";
        /**
         * The grid it was picked up from, and the collection that grid showed (Home's id for Home): what letting go somewhere
         * else moves it out of. Kept from the start, since a card opened or left while carrying shows another collection.
         */
        private Icons fromGrid;private String outOf;
        /**
         * Held a moment on a collection, it opens - its card over Home, or a level in - and held on the card's name, the card
         * goes up a level (the owner, 2026-10-03: "put it at any level of grouping we have in both directions").
         */
        private final javax.swing.Timer spring=new javax.swing.Timer(650,e->sprung());private String springKey,springAt;private Runnable springDo;
        private void sprung(){Runnable then=springDo;springAt=null;if(carried!=null&&then!=null){then.run();if(last!=null)at(last);}}
        private void springOn() {
            if(springKey==null){spring.stop();springAt=null;return;}
            if(springKey.equals(springAt))return;
            springAt=springKey;spring.setRepeats(false);spring.restart();
        }
        /**
         * The mouse, wherever it is, while a thing is carried whose icon has been drawn away - a card opened or gone up under it -
         * since then nothing tells the icon the mouse moved.
         */
        private java.awt.event.AWTEventListener follow;

        boolean carrying(){return carried!=null;}
        boolean moved(){return moved;}

        void press(Tile t,MouseEvent e) {
            motion.stop();moved=false;pressed=null;
            // A thing anywhere; a place on Home, to another cell of it (decision 42); nothing out of the archive or the bin.
            if(!SwingUtilities.isLeftMouseButton(e)||!Grid.carried(t.thing.kind)||t.where==Where.AWAY||Grid.place(t.thing.kind)&&t.where!=Where.HOME)return;
            pressed=t;pressedAt=e.getLocationOnScreen();
        }
        void drag(MouseEvent e) {
            if(pressed==null)return;
            java.awt.Point now=e.getLocationOnScreen();
            if(carried==null){if(now.distance(pressedAt)<Math.max(4,java.awt.dnd.DragSource.getDragThreshold()))return;begin(pressed);}
            at(now);
        }
        void release(MouseEvent e){if(carried!=null){at(e.getLocationOnScreen());letGo(e.getLocationOnScreen());}pressed=null;}

        private void begin(Tile t) {
            motion.stop();
            pickup=new java.awt.Point(pressedAt);SwingUtilities.convertPointFromScreen(pickup,t);
            double scale=pad.frame.getGraphicsConfiguration().getDefaultTransform().getScaleX();
            java.awt.image.BufferedImage preview=new java.awt.image.BufferedImage(Math.max(1,(int)Math.ceil(t.getWidth()*scale)),Math.max(1,(int)Math.ceil(t.getHeight()*scale)),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D image=preview.createGraphics();image.scale(scale,scale);t.paint(image);image.dispose();
            carried=t;moved=true;t.lifted=true;t.repaint();
            fromGrid=t.getParent() instanceof Icons i?i:null;
            outOf=fromGrid==grid?Things.HOME:fromGrid!=null&&fromGrid==folder.grid&&folder.isOpen()?folder.id():null;
            follow=ev->{
                if(!(ev instanceof MouseEvent me)||carried==null||carried.isShowing())return;
                if(me.getID()==MouseEvent.MOUSE_DRAGGED)at(me.getLocationOnScreen());
                else if(me.getID()==MouseEvent.MOUSE_RELEASED){at(me.getLocationOnScreen());letGo(me.getLocationOnScreen());pressed=null;}
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(follow,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);
            int previewW=t.getWidth(),previewH=t.getHeight();
            ghost=new JLabel(new javax.swing.Icon(){public int getIconWidth(){return previewW;}public int getIconHeight(){return previewH;}
                public void paintIcon(Component c,Graphics g,int x,int y){g.drawImage(preview,x,y,previewW,previewH,null);}});
            ghost.setSize(t.getSize());ghost.setVisible(false);
            pad.frame.getLayeredPane().add(ghost,JLayeredPane.DRAG_LAYER);
            escape=k->{
                if(carried==null||k.getKeyCode()!=KeyEvent.VK_ESCAPE)return false;
                if(k.getID()==KeyEvent.KEY_PRESSED)cancel();
                return true;
            };
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(escape);
            // What can be done to it by letting go, across the top while it is carried (decision 90).
            NoteStore.Branch.Kind k=t.thing.kind;
            if(k==NoteStore.Branch.Kind.PAGE||k==NoteStore.Branch.Kind.FILE||DesktopMoving.shelf(k)) {
                strip=new DropStrip();
                JLayeredPane layer=pad.frame.getLayeredPane();
                // In the bar just above Home's rows, centred, leaving the rows free to drop on: carried past it - above it or
                // beside it on the bar - the page above comes as it always did (the owner: "if we overpass them, we go to the
                // page on top").
                java.awt.Point room=SwingUtilities.convertPoint(scroll,0,0,layer);
                Rectangle area=SwingUtilities.convertRectangle(scroll.getParent(),scroll.getBounds(),layer);
                Dimension d=strip.getPreferredSize();
                // In the title bar now there is no bar of its own (0.3.076): in the middle of its height where it fits.
                int y=room.y>=d.height?(room.y-d.height)/2:Math.max(0,room.y-d.height-2);
                strip.setBounds(Math.max(8,area.x+(area.width-d.width)/2),y,d.width,d.height);
                layer.add(strip,JLayeredPane.POPUP_LAYER);layer.repaint();
            }
        }

        private boolean over(Component c,java.awt.Point screen) {
            if(c==null||!c.isShowing())return false;
            java.awt.Point p=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(p,c);
            return c instanceof JComponent j?j.getVisibleRect().contains(p):c.contains(p);
        }

        /** The pointer at this point of the screen: where it would land there, marked, and said in the bar. */
        void at(java.awt.Point screen) {
            if(carried==null)return;
            last=screen;
            java.awt.Point onLayer=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(onLayer,pad.frame.getLayeredPane());
            ghost.setLocation(onLayer.x-pickup.x,onLayer.y-pickup.y);ghost.setVisible(true);
            // Over the strip: that, and nothing under it.
            if(strip!=null&&over(strip,screen)) {
                motion.stop();
                int which=strip.light(screen);
                // And no folder opens under it: a pass over one on the way to the strip set its spring going.
                spring.stop();springAt=null;springKey=null;springDo=null;
                if(target!=null){target.target=false;target.repaint();target=null;}
                if(markedIn!=null){markedIn.landing=null;markedIn.repaint();markedIn=null;}
                cell=null;zone=Grid.Zone.NONE;dock.lit(false);
                pad.status.setToolTipText(null);pad.status.setText((which<0?Say.t("Let go on one of these"):Say.t("Let go to {0}",Say.t(strip.doing(which))))+"  ·  "+Say.t("Esc cancels"));
                return;
            }
            if(strip!=null)strip.light(null);
            Icons from=fromGrid;
            outOfCard(from,screen);
            zone=Grid.zone(over(dock,screen),folder.isOpen(),folder.isOpen()&&over(folder.card,screen),over(desk,screen));
            Tile wasTarget=target;Icons wasIn=markedIn;
            target=null;cell=null;stays=false;markedIn=null;inTheWay=null;
            boolean dockNow=zone==Grid.Zone.DOCK&&Grid.docks(carried.thing.kind);
            boolean up=zone==Grid.Zone.UP&&from!=null&&!folder.listing();
            springKey=null;springDo=null;
            dock.lit(dockNow);
            folder.lit(up);
            edgeSaid=null;
            // Carried out of Home's rows - up into the bar, down short of the dock - is a turn of the page too.
            boolean homeSeen=!folder.isOpen()||folder.aside();
            boolean outOfRows=zone==Grid.Zone.NONE&&from!=null&&homeSeen;
            // Round the card is Home, which is what is seen there: let go there, it goes onto Home, from any depth.
            if((zone==Grid.Zone.HOME||up)&&from!=null){across(grid,screen);if(homeSeen)atTheEdge(screen);}
            else if(outOfRows)atTheEdge(screen);
            else if(zone==Grid.Zone.CARD&&from==folder.grid&&folder.amongFavourites()) {
                // Among the favourites: the one under the pointer is ringed, the place this one goes before (decision 74).
                int[] under=folder.grid.cellAt(screen);Tile there=under==null?null:folder.grid.in(under[0],under[1]);
                if(there!=null&&there!=carried)target=there;
            }
            else if(zone==Grid.Zone.CARD&&from!=null&&!folder.listing()) {
                // Held on ‹ and the collection above, in a card inside another: up a level. Out of the card is Home.
                if(folder.back!=null&&over(folder.back,screen)){springKey="up:"+folder.id();springDo=folder::up;}
                else across(folder.grid,screen);
            }
            springOn();
            if(wasTarget!=target){if(wasTarget!=null){wasTarget.target=false;wasTarget.repaint();}if(target!=null){target.target=true;target.repaint();}}
            if(wasIn!=null&&wasIn!=markedIn){wasIn.landing=null;wasIn.repaint();}
            if(!((zone==Grid.Zone.HOME||up)&&homeSeen)&&!outOfRows){motion.stop();turnedAt=false;}
            String name=named(carried.thing);
            if(edgeSaid!=null){say(edgeSaid);return;}
            say(dockNow?Say.t("Let go to put “{0}” in the dock",name)
                :zone==Grid.Zone.DOCK?"Only a note or a folder goes in the dock"
                :target!=null?onto(carried.thing,target.thing)
                :springKey!=null&&springKey.startsWith("up:")?Say.t("Hold here to go up to “{0}”",folder.parentName())
                :cell!=null&&!java.util.Objects.equals(markedIn==grid?Things.HOME:folder.id(),outOf)?(markedIn==grid?Say.t("Let go to move it into Home, here"):Say.t("Let go to move it into “{0}”, here",folder.name()))
                :up?"Let go to move it onto Home"
                :stays?"Let go to leave it where it was"
                :cell!=null?"Let go to put it here"
                :inTheWay!=null?Say.t("“{0}” is already there",named(inTheWay.thing))
                :"It cannot go here");
        }
        /**
         * Carried out of a card, over its edge or past it: the card steps aside, at once past it and after a moment held on
         * its edge, and the whole of Home is where it can be let go (see {@link DesktopFolder#stepAside}).
         */
        /**
         * Carried out of a card: the card steps aside as soon as the pointer leaves it (see {@link DesktopFolder#stepAside}).
         * Not on a hold at its edge, as when the card filled the window: its top edge is its name, where a hold goes up a
         * level, and a hold there stepped it aside first.
         */
        private boolean cardEntered=true;
        private void outOfCard(Icons from,java.awt.Point screen) {
            if(!folder.isOpen()||folder.aside()||folder.listing()||from==null)return;
            java.awt.Point p=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(p,folder.card);
            boolean outside=p.x<0||p.y<0||p.x>=folder.card.getWidth()||p.y>=folder.card.getHeight();
            // A card opened under the pointer waits for it to have been over the card before stepping aside.
            if(!outside)cardEntered=true;
            if(cardEntered&&outside)folder.stepAside(true);
        }
        /** What letting go on an icon does, in words: together, into it, or put away by the place it is. */
        private String onto(NoteStore.Branch carried,NoteStore.Branch there) {
            switch(Grid.onto(carried.kind,there.kind)) {
                case MERGE: return "Let go to put them together in a new folder";
                case AWAY:
                    if(there.kind==NoteStore.Branch.Kind.ARCHIVE)return "Let go to archive it";
                    return carried.kind==NoteStore.Branch.Kind.FILE?"Let go to delete it":"Let go to put it in the bin";
                default: return Say.t("Let go to move it into “{0}”",named(there));
            }
        }
        /**
         * Whether the pointer is at an edge of the room, and which way. Held there, Home scrolls that way, and a page
         * beyond the last one comes only once the scroll has gone as far as the pages there are (decision 126). Over a
         * cell, including the last row, it is let go there: the edge is the band round the room, not the row itself.
         */
        private void atTheEdge(java.awt.Point screen) {
            java.awt.Point p=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(p,scroll);
            int reach=28,wait=650;
            // Above the room, under the app's name, at once. Below, after a moment while the dock is there, so a thing on
            // its way to the dock does not scroll the page as it passes.
            int dy=p.y<0?-1:p.y>scroll.getHeight()-reach?1:0;
            if(dy<0)wait=0;else if(dy>0)wait=dock.isShowing()?400:250;
            int dx=dy!=0?0:p.x<reach?-1:p.x>scroll.getWidth()-reach?1:0;
            if(dx==0&&dy==0){motion.stop();turnedAt=false;return;}
            // One page for each time it goes out: back into the room, or off the edge and back, for the next.
            if(turnedAt&&dx==edgeX&&dy==edgeY){edgeSaid=Say.t("{0} now. Bring it onto the page to put it there",Say.t(DesktopPages.said(grid.pageX,grid.pageY)));return;}
            edgeSaid=Say.t(dx<0?"Hold here for the page to the left":dx>0?"Hold here for the page to the right":dy<0?"Hold here for the page above":"Hold here for the page below");
            if(motion.edging()&&dx==edgeX&&dy==edgeY)return;
            edgeX=dx;edgeY=dy;edgeFractionX=edgeFractionY=0;
            motion.edge(wait,this::overTheEdge);
        }
        private double edgeFractionX,edgeFractionY;
        private void overTheEdge(double seconds) {
            if(carried==null)return;
            java.awt.Point p=new java.awt.Point(last);SwingUtilities.convertPointFromScreen(p,scroll);
            double vx=edgeX==0?0:HomeMotion.edgeSpeed(p.x,0,scroll.getWidth(),28,600);
            double vy=edgeY==0?0:HomeMotion.edgeSpeed(p.y,0,scroll.getHeight(),28,600);
            edgeFractionX+=vx*seconds;edgeFractionY+=vy*seconds;
            int stepX=(int)Math.round(edgeFractionX),stepY=(int)Math.round(edgeFractionY);edgeFractionX-=stepX;edgeFractionY-=stepY;
            if(stepX==0&&stepY==0)return;
            if(!grid.scrollBy(stepX,stepY)) {
                if(!grid.openBeyond(edgeX,edgeY)||!grid.scrollBy(stepX,stepY)) {
                    motion.stop();turnedAt=true;
                    edgeSaid=Say.t("{0} now. Bring it onto the page to put it there",Say.t(DesktopPages.said(grid.pageX,grid.pageY)));
                    say(edgeSaid);
                    return;
                }
            }
            // The empty page is the one in view: say so, and do not open another until the pointer leaves the edge.
            // Continue to the end of the one extra page; page detection changes halfway through it.
            if(last!=null)at(last);
        }
        private void say(String words){String all=Say.t(words)+"  ·  "+Say.t("Esc cancels");if(!all.equals(said)){said=all;pad.status.setToolTipText(null);pad.status.setText(all);}}

        /**
         * Over a grid, by the cell under the pointer (decision 39): one that holds a thing it can go onto or into, that thing;
         * an empty one, the place it would land, outlined; its own, where it stays; one holding something else, nothing.
         * Near the top or foot of the grid, it scrolls.
         */
        private void across(Icons in,java.awt.Point screen) {
            java.awt.Point p=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(p,in);
            Rectangle seen=in.getVisibleRect();int reach=40,step=16;
            if(!in.paged()&&p.y<seen.y+reach)in.scrollRectToVisible(new Rectangle(seen.x,Math.max(0,seen.y-step),1,1));
            else if(!in.paged()&&p.y>seen.y+seen.height-reach)in.scrollRectToVisible(new Rectangle(seen.x,seen.y+seen.height+step,1,1));
            int[] under=in.cellAt(screen);
            Rectangle land=null;
            if(under!=null) {
                // What letting go there does is Grid's, as on the phone.
                Tile there=in.in(under[0],under[1]);
                switch(Grid.letGo(carried.thing.kind,there==null?null:there.thing.kind,there==carried)) {
                    case PLACE: cell=under;markedIn=in;land=in.boundsOn(in.paged()?in.landX:0,in.paged()?in.landY:0,under[0],under[1]);break;
                    case MERGE: case INTO: case AWAY:
                        target=there;
                        if(there.thing.kind==NoteStore.Branch.Kind.COLLECTION&&Grid.onto(carried.thing.kind,there.thing.kind)==Grid.Onto.INTO) {
                            NoteStore.Branch opening=there.thing;boolean onHome=in==grid;
                            springKey="in:"+opening.id;springDo=()->{if(folder.isOpen()&&opening.id.equals(folder.id()))return;if(onHome){cardEntered=false;folder.open(opening);}else folder.into(opening);};
                        }
                        break;
                    default: if(there==carried){cell=under;markedIn=in;stays=true;}else inTheWay=there;
                }
            }
            if(!java.util.Objects.equals(in.landing,land)){in.landing=land;in.repaint();}
        }

        /** Let go: onto a thing, into the dock, into whatever collection is seen there or onto Home, or in an empty cell of its grid. */
        private void letGo(java.awt.Point screen) {
            Tile t=carried,onto=target;Grid.Zone z=zone;Icons in=markedIn;int[] at=cell;
            Icons from=fromGrid;String came=outOf;
            int dockSlot=dock.slotAt(screen,t);
            int onStrip=strip!=null&&over(strip,screen)?strip.light(screen):-1;
            NoteStore.Branch thing=t.thing;
            if(onStrip<0&&onto==null&&at!=null&&in==from&&places(from)&&outOf!=null&&t.where!=Where.DOCK&&t.where!=Where.FAVOURITES) {
                if(from.paged()){
                    Layout.Spot to=new Layout.Spot(from.landX,from.landY,at[0],at[1]);Map<String,Layout.Spot> spots=Layout.moveTo(from.spots(),thing.id,to);
                    if(spots!=null&&!to.equals(from.spots().get(thing.id))){keepSpots(from,thing,spots,to.x()!=0||to.y()!=0?Say.t("Moved to {0}",Say.t(DesktopPages.said(to.x(),to.y()).toLowerCase(java.util.Locale.ROOT))):Say.t("Moved"));end();return;}
                }else{
                    Map<String,Integer> cells=Layout.moveTo(from.drawn(),thing.id,at[0],at[1]);
                    if(cells!=null){keepCells(from,thing,cells,"Moved");end();return;}
                }
            }
            end();
            if(onStrip>=0){fromStrip(thing,onStrip);return;}
            if(z==Grid.Zone.DOCK) {
                if(!Grid.docks(thing.kind)){pad.status.setText(Say.t("Only a note or a folder goes in the dock"));refreshIfStale();return;}
                toDock(thing,dockSlot);return;
            }
            // Among the favourites, let go on another: before it in their one order; in the room after them, last (decision 74).
            if(t.where==Where.FAVOURITES&&z==Grid.Zone.CARD&&folder.amongFavourites()){reorderFavourites(thing,onto==null?null:onto.thing);return;}
            // From the dock or among the favourites a thing is only listed: it goes to the dock from there, and nowhere else.
            if(t.where==Where.DOCK||t.where==Where.FAVOURITES||t.where==Where.AWAY||from==null||came==null){pad.status.setText(Say.t("Not moved"));refreshIfStale();return;}
            boolean inCard=z==Grid.Zone.CARD,onHome=z==Grid.Zone.HOME||z==Grid.Zone.UP;
            // Let go on Home round the card, stepped aside or not: the card goes once the thing is there.
            Runnable leftCard=onHome&&folder.isOpen()?folder::close:null;
            if(onto!=null) {
                Grid.Onto does=Grid.onto(thing.kind,onto.thing.kind);
                // On the archive or the bin: put away there, as its menu puts it away, said and one Undo from where it was
                // (decision 41); a file on the bin asked about first, as its own Delete asks.
                // Whatever leaves this grid for a place leaves its spot empty: the others are kept where they are drawn now
                // (the owner, 2026-10-14: "the elements on the home page don't move when we drag an element in the bin or
                // archive, the spot left behind should stay empty").
                if(does==Grid.Onto.AWAY)holdOthers(from,thing);
                if(does==Grid.Onto.AWAY&&onto.thing.kind==NoteStore.Branch.Kind.TEMP){refreshIfStale();pad.intoTemp(thing);return;}
                if(does==Grid.Onto.AWAY) {
                    boolean bin=onto.thing.kind==NoteStore.Branch.Kind.BIN;
                    pad.save(()->pad.putAway(thing,bin));
                    return;
                }
                if(does==Grid.Onto.MERGE)merge(thing,onto.thing,inCard?folder.id():Things.HOME,inCard);
                else into(thing,onto.thing.id,named(onto.thing),onto.thing.kind,leftCard);
                return;
            }
            if(!inCard&&!onHome){pad.status.setText(Say.t("Not moved"));refreshIfStale();return;}
            String dest=inCard?folder.id():Things.HOME;
            if(!dest.equals(came)) {
                // Into another collection than it came out of, or onto Home, at any depth: in the empty cell it was let go in,
                // or else the first free one.
                Runnable there=null;
                if(at!=null&&in!=null&&in==(inCard?folder.grid:grid)) {
                    List<NoteStore.Branch> lines=new ArrayList<>();for(Tile one:in.tiles)lines.add(one.thing);lines.add(thing);
                    if(in.paged()) {
                        Map<String,Layout.Spot> spots=Layout.moveTo(in.spots(),thing.id,new Layout.Spot(in.landX,in.landY,at[0],at[1]));
                        if(spots!=null)there=()->writeSpots(lines,spots);
                    } else {
                        Map<String,Integer> cells=Layout.moveTo(in.drawn(),thing.id,at[0],at[1]);
                        if(cells!=null)there=()->store().place(lines,cells);
                    }
                }
                into(thing,dest,inCard?folder.name():"Home",NoteStore.Branch.Kind.COLLECTION,leftCard,there);
                return;
            }
            if(at!=null&&in==from&&places(from)&&from.paged()) {
                // On a page of Home, maybe another than it came from - which is how a page comes to be (decision 46).
                Layout.Spot to=new Layout.Spot(from.landX,from.landY,at[0],at[1]);
                Map<String,Layout.Spot> spots=Layout.moveTo(from.spots(),thing.id,to);
                boolean there=to.x()!=0||to.y()!=0;
                if(spots!=null&&!to.equals(from.spots().get(thing.id))){keepSpots(from,thing,spots,there?Say.t("Moved to {0}",Say.t(DesktopPages.said(to.x(),to.y()).toLowerCase(java.util.Locale.ROOT))):Say.t("Moved"));return;}
            }
            else if(at!=null&&in==from&&places(from)) {
                Map<String,Integer> cells=Layout.moveTo(from.drawn(),thing.id,at[0],at[1]);
                int[] was=from.drawn().get(thing.id);
                if(cells!=null&&(was==null||was[0]!=at[0]||was[1]!=at[1])){keepCells(from,thing,cells,"Moved");return;}
            }
            pad.status.setText(Say.t("Not moved"));refreshIfStale();
        }
        /** Esc: put back where it was, and said. */
        void cancel(){if(carried==null)return;end();pressed=null;pad.status.setText(Say.t("Not moved"));refreshIfStale();}

        private void end() {
            motion.stop();last=null;turnedAt=false;grid.clearPast();
            folder.stepAside(false);
            spring.stop();springAt=null;springKey=null;springDo=null;fromGrid=null;outOf=null;cardEntered=true;
            if(follow!=null){Toolkit.getDefaultToolkit().removeAWTEventListener(follow);follow=null;}
            Tile t=carried;carried=null;said="";
            if(t!=null){t.lifted=false;t.repaint();}
            if(target!=null){target.target=false;target.repaint();target=null;}
            if(markedIn!=null){markedIn.landing=null;markedIn.repaint();markedIn=null;}
            for(Icons one:new Icons[]{grid,folder.grid})if(one.landing!=null){one.landing=null;one.repaint();}
            cell=null;stays=false;inTheWay=null;zone=Grid.Zone.NONE;dock.lit(false);folder.lit(false);
            if(escape!=null){KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(escape);escape=null;}
            if(ghost!=null){Container layer=ghost.getParent();if(layer!=null){layer.remove(ghost);layer.repaint();}ghost=null;}
            if(strip!=null){Container layer=strip.getParent();if(layer!=null){layer.remove(strip);layer.repaint();}strip=null;}
        }
        private DropStrip strip;

        /** Let go on one of the strip's targets: done as its menu does it, said, with Undo where the menu has one. */
        private void fromStrip(NoteStore.Branch thing,int which) {
            boolean file=thing.kind==NoteStore.Branch.Kind.FILE;
            switch(which) {
                case 0 -> {
                    if(file)pad.disk.submit(()->{store().keepToHand(NoteStore.Branch.Kind.FILE,thing.id,true);return null;},done->{pad.status.setText(Say.t("A favourite"));pad.refresh();},pad::failed);
                    else pad.favourite(thing,true);
                }
                case 1 -> pad.intoTemp(thing);
                case 2 -> pad.save(()->pad.putAway(thing,false));
                case 3 -> pad.save(()->pad.putAway(thing,true));
                default -> pad.shareThing(thing);
            }
            refreshIfStale();
        }
    }

    /**
     * What can be done to a thing by letting go of it, across the top of the window while it is carried, as a phone offers
     * Remove and Uninstall over its home screen (the owner, 2026-10-04: "when we drag and drop elements we should be
     * presented, like on our phone with an app, Archive or Bin ... a UI/UX that lets the user do pretty much everything with
     * drag and drop"; decision 90): a favourite, Temp, the archive, the bin, and sharing it: a file's too, since it is shared
     * like a note (decision 92); Send to a device… stays in its menu.
     */
    private static final class DropStrip extends JComponent {
        private static final String[][] TARGETS={{"star","Favourite","make it a favourite"},{"timer","Temp","put it on Temp"},
            {ARCHIVE_ICON,"Archive","archive it"},{BIN_ICON,"Bin","move it to the bin"},{"share-2","Share","share it"}};
        // One row, each icon beside its word, low enough to lie in the title bar above Home (0.3.076), not over its first row.
        private static final int WIDE=108,HIGH=34;
        private int lit=-1;
        DropStrip(){setOpaque(false);setPreferredSize(new Dimension(WIDE*TARGETS.length+16,HIGH+10));}
        private String said(int at){return Say.t(TARGETS[at][1]);}
        String doing(int at){return TARGETS[at][2];}
        /** The target under the pointer, lit; -1 for none. */
        int light(java.awt.Point screen) {
            int at=-1;
            if(screen!=null&&isShowing()){java.awt.Point p=new java.awt.Point(screen);SwingUtilities.convertPointFromScreen(p,this);
                if(p.y>=0&&p.y<getHeight()&&p.x>=8&&p.x<8+WIDE*TARGETS.length)at=(p.x-8)/WIDE;}
            if(at!=lit){lit=at;repaint();}
            return at;
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // Its words as Windows draws words, ClearType and all, as the icons' names under them (the owner, 2026-10-14: "the
            // top menu elements writing is not sharp").
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
            g.setColor(new Color(0,0,0,30));g.fillRoundRect(2,4,getWidth()-4,getHeight()-4,22,22);
            g.setColor(DesktopUi.CARD);g.fillRoundRect(0,0,getWidth()-4,getHeight()-6,22,22);
            g.setColor(DesktopUi.LINE);g.drawRoundRect(0,0,getWidth()-5,getHeight()-7,22,22);
            g.setFont(DesktopUi.BODY.deriveFont(12.5f));FontMetrics m=g.getFontMetrics();
            for(int at=0;at<TARGETS.length;at++) {
                int x=8+at*WIDE;
                if(at==lit){g.setColor(DesktopUi.mix(DesktopUi.ACCENT,DesktopUi.CARD,0.8f));g.fillRoundRect(x+2,4,WIDE-4,HIGH-6,14,14);
                    g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(2f));g.drawRoundRect(x+2,4,WIDE-4,HIGH-6,14,14);}
                String glyph=TARGETS[at][0];
                String word=said(at);int across=18+6+m.stringWidth(word),from=x+(WIDE-across)/2;
                DesktopIcons.draw(g,glyph,from,4+(HIGH-6-18)/2f,18,DesktopUi.INK);
                g.setColor(DesktopUi.INK);g.drawString(word,from+24,4+(HIGH-6+m.getAscent()-m.getDescent())/2);
            }
            g.dispose();
        }
    }

    /** Something changed while a thing was carried or a name typed: Home read again now they are done. */
    void refreshIfStale(){if(stale)refresh();}
    /** Read again when it is next shown: a note is in front of it now (see Desktop#refresh). */
    void later(){stale=true;}
}
