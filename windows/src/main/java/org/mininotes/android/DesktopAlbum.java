// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;

/**
 * A folder shown as an album on the PC (the owner, 2026-10-19: "would be nice to have a type of folder specific for photos,
 * that will display photos in a nice album setup and make browsing convenient"; decision 217): its pictures as squares,
 * cropped to fill, newest first, over what else the folder holds; and a picture clicked shown large over the window, the one
 * before and the one after with the arrow keys or the buttons at its sides, Esc to close, its name, where it is ("3 of 24")
 * and its own menu. Squares and large pictures are made on a thread of their own, never on the one the note's saves wait on,
 * read through the lock as every file is (DesktopFiles.copyOut), and kept while the app runs.
 */
final class DesktopAlbum {
    private DesktopAlbum(){}

    /** One picture of an album: its line, as the folder lists it, for its menu; and its row, for its bytes. */
    record Picture(NoteStore.Branch line,NoteStore.Held held){}

    /** The least a square is wide, and the room between two: about 160 to 200 pixels a square at the card's usual width. */
    static final int LEAST=156,GAP=8;
    /** Bigger than this, a picture is shown by its kind: read whole into memory, it would be too much to hold. */
    static final long MOST=40L*1024*1024;

    private static final java.util.concurrent.ExecutorService MAKING=java.util.concurrent.Executors.newSingleThreadExecutor(work->{
        Thread one=new Thread(work,"mininotes-album");one.setDaemon(true);one.setPriority(Thread.MIN_PRIORITY);return one;});
    /** Squares made, by file and size; NONE where the picture could not be read. Kept while the app runs, the latest few hundred. */
    private static final Map<String,BufferedImage> SQUARES=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String,BufferedImage>(128,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> eldest){return size()>400;}});
    static final BufferedImage NONE=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    /** Whether a square of this file is made, and could be read: null while it is not made yet. For the tests. */
    static Boolean squareMade(NoteStore.Held file){BufferedImage made=SQUARES.get(file.id+":"+file.bytes);return made==null?null:made!=NONE;}

    // ---- reading pictures ----------------------------------------------------------------------------------------------

    /** A kept file's own bytes, through the lock where there is one. */
    static byte[] bytes(Desktop pad,NoteStore.Held file) throws IOException {
        if(file.bytes>MOST)return null;
        ByteArrayOutputStream out=new ByteArrayOutputStream();DesktopFiles.copyOut(pad.context,pad.store.fileFor(file.id).toPath(),out);return out.toByteArray();
    }

    /** A picture read with every n-th pixel, in memory and never through ImageIO's cache files; null where it cannot be read. */
    static BufferedImage read(byte[] raw,int step) throws IOException {
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(raw))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())return null;
            ImageReader reader=readers.next();
            try {
                reader.setInput(in,true,true);
                ImageReadParam param=reader.getDefaultReadParam();
                if(step>1)param.setSourceSubsampling(step,step,0,0);
                return reader.read(0,param);
            } finally{reader.dispose();}
        } catch(RuntimeException unreadable){return null;}
    }

    /** The square in the middle of a picture, {@code side} wide, turned as its camera said; null where it cannot be read. */
    static BufferedImage square(byte[] raw,int side) throws IOException {
        int[] size=raw==null?null:DesktopPictures.PAINTER.size(raw);if(size==null)return null;
        BufferedImage whole=read(raw,Album.step(size[0],size[1],side));if(whole==null)return null;
        whole=DesktopPictures.upright(whole,DesktopPictures.PAINTER.turn(raw));
        int[] s=Album.square(whole.getWidth(),whole.getHeight());
        BufferedImage out=new BufferedImage(side,side,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,side,side);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(whole,0,0,side,side,s[0],s[1],s[0]+s[2],s[1]+s[2],null);g.dispose();whole.flush();
        return out;
    }

    /** A picture decoded at about the size of the room it is shown in, never at its own where that is far bigger. */
    static BufferedImage large(byte[] raw,int w,int h) throws IOException {
        int[] size=raw==null?null:DesktopPictures.PAINTER.size(raw);if(size==null)return null;
        int step=1;while(Math.max(size[0],size[1])/(step*2)>=Math.max(1,Math.max(w,h)))step*=2;
        BufferedImage whole=read(raw,step);
        return whole==null?null:DesktopPictures.upright(whole,DesktopPictures.PAINTER.turn(raw));
    }

    /** What an album of this PC reads its squares with: the file's bytes, then its square. */
    static Function<NoteStore.Held,BufferedImage> squares(Desktop pad,int side){return file->{try{return square(bytes(pad,file),side);}catch(Exception unreadable){return null;}};}
    /** And its large pictures, fitted to the window. */
    static Function<NoteStore.Held,BufferedImage> larges(Desktop pad,Component room){return file->{try{return large(bytes(pad,file),Math.max(800,room.getWidth()),Math.max(600,room.getHeight()));}catch(Exception unreadable){return null;}};}

    // ---- the grid of squares -------------------------------------------------------------------------------------------

    /**
     * The album's squares, as many across as the width holds, each a picture cropped to fill; a click opens it large, a
     * right-click (or the Menu key) its menu. A square not made yet is the paper's line colour, with the picture's kind on it.
     */
    static final class Grid extends JPanel {
        private final Function<NoteStore.Held,BufferedImage> making;
        private final java.util.function.IntConsumer open;
        private final java.util.function.BiConsumer<Picture,Component> menu;
        final List<Picture> pictures=new ArrayList<>();
        private final JLabel empty;
        /** Each square's side as last laid out. */
        private int side=LEAST;
        /** Bumped on each fill, so a square made for what was shown before is not put into what is shown now. */
        private int round;

        Grid(Function<NoteStore.Held,BufferedImage> making,java.util.function.IntConsumer open,java.util.function.BiConsumer<Picture,Component> menu) {
            super(null);this.making=making;this.open=open;this.menu=menu;setOpaque(false);
            empty=new JLabel(Say.t("No photos yet. Click + to add photos, or drop pictures here."),SwingConstants.CENTER);
            empty.setFont(DesktopUi.BODY.deriveFont(13f));empty.setForeground(DesktopUi.QUIET);
            getAccessibleContext().setAccessibleName(Say.t("Photos"));
        }

        /** These pictures, in this order; their squares made where they are not made yet. */
        void fill(List<Picture> now) {
            round++;pictures.clear();pictures.addAll(now);removeAll();
            if(now.isEmpty())add(empty);
            for(int at=0;at<now.size();at++)add(new Square(this,at,now.get(at)));
            revalidate();repaint();
        }
        int across(){return Album.across(room(),LEAST,GAP);}
        private int room(){Container up=getParent();int w=up!=null&&up.getWidth()>0?up.getWidth():getWidth();return Math.max(LEAST,w-2*PAD);}
        static final int PAD=10;
        @Override public Dimension getPreferredSize() {
            int w=room()+2*PAD;
            if(pictures.isEmpty())return new Dimension(w,96);
            int n=across(),s=Album.side(room(),n,GAP),rows=(pictures.size()+n-1)/n;
            return new Dimension(w,PAD+rows*(s+GAP));
        }
        @Override public void doLayout() {
            if(pictures.isEmpty()){empty.setBounds(PAD,0,Math.max(1,getWidth()-2*PAD),96);return;}
            int n=across();side=Album.side(room(),n,GAP);
            for(Component c:getComponents())if(c instanceof Square q)q.setBounds(PAD+(q.at%n)*(side+GAP),PAD+(q.at/n)*(side+GAP),side,side);
        }
        /** The square for this picture: made now on the album's thread where it is not kept already. */
        BufferedImage squareOf(Square q) {
            String key=q.picture.held().id+":"+q.picture.held().bytes;
            BufferedImage known=SQUARES.get(key);
            if(known!=null)return known==NONE?null:known;
            if(q.asked)return null;
            q.asked=true;int asked=round;
            MAKING.execute(()->{
                BufferedImage made=null;try{made=making.apply(q.picture.held());}catch(Throwable unreadable){/* its kind, then */}
                SQUARES.put(key,made==null?NONE:made);
                SwingUtilities.invokeLater(()->{if(asked==round)q.repaint();});
            });
            return null;
        }
        /** The square with the keyboard, for a test or the Menu key; -1 where none has it. */
        int focused(){for(Component c:getComponents())if(c instanceof Square q&&q.isFocusOwner())return q.at;return -1;}
        void openAt(int at){if(at>=0&&at<pictures.size())open.accept(at);}
        void menuAt(int at,Component on){if(at>=0&&at<pictures.size())menu.accept(pictures.get(at),on);}
    }

    /** One picture's square: its picture cropped to fill, rounded, ringed while it has the keyboard. */
    static final class Square extends JComponent {
        final Grid grid;final int at;final Picture picture;boolean asked;
        /** Named for anybody who cannot see it, as a button that opens its picture. */
        @Override public javax.accessibility.AccessibleContext getAccessibleContext(){if(accessibleContext==null)accessibleContext=new AccessibleJComponent(){@Override public javax.accessibility.AccessibleRole getAccessibleRole(){return javax.accessibility.AccessibleRole.PUSH_BUTTON;}};return accessibleContext;}
        Square(Grid grid,int at,Picture picture) {
            this.grid=grid;this.at=at;this.picture=picture;
            setFocusable(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(picture.held().name);getAccessibleContext().setAccessibleName(picture.held().name);
            addMouseListener(new MouseAdapter(){
                public void mousePressed(MouseEvent e){requestFocusInWindow();if(e.isPopupTrigger())grid.menuAt(at,Square.this);}
                public void mouseReleased(MouseEvent e){if(e.isPopupTrigger())grid.menuAt(at,Square.this);}
                public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))grid.openAt(at);}
            });
            addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){repaint();scrollRectToVisible(new Rectangle(0,0,getWidth(),getHeight()));}public void focusLost(FocusEvent e){repaint();}});
            for(String key:new String[]{"ENTER","SPACE"})getInputMap().put(KeyStroke.getKeyStroke(key),"open");
            getActionMap().put("open",new AbstractAction(){public void actionPerformed(ActionEvent e){grid.openAt(at);}});
            // The arrow keys go from square to square, as they go from icon to icon on Home.
            for(String key:new String[]{"LEFT","RIGHT","UP","DOWN"})getInputMap().put(KeyStroke.getKeyStroke(key),key);
            getActionMap().put("LEFT",go(-1,0));getActionMap().put("RIGHT",go(1,0));getActionMap().put("UP",go(0,-1));getActionMap().put("DOWN",go(0,1));
        }
        private Action go(int dx,int dy){return new AbstractAction(){public void actionPerformed(ActionEvent e){
            int to=at+dx+dy*grid.across();if(to<0||to>=grid.pictures.size())return;
            for(Component c:grid.getComponents())if(c instanceof Square q&&q.at==to){q.requestFocusInWindow();return;}}};}
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();Shape round=new RoundRectangle2D.Float(0,0,w,h,12,12);
            BufferedImage made=grid.squareOf(this);
            if(made!=null){g.setClip(round);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(made,0,0,w,h,null);g.setClip(null);}
            else {
                g.setColor(DesktopUi.LINE);g.fill(round);
                String kind=DesktopFileCards.extension(picture.held().name);
                g.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,14f));g.setColor(DesktopUi.QUIET);FontMetrics m=g.getFontMetrics();
                g.drawString(kind,(w-m.stringWidth(kind))/2,(h+m.getAscent()-m.getDescent())/2);
            }
            if(isFocusOwner()){g.setColor(DesktopUi.ACCENT);g.setStroke(new BasicStroke(3f));g.draw(new RoundRectangle2D.Float(1.5f,1.5f,w-3,h-3,12,12));}
            g.dispose();
        }
    }

    /** How big a square is made: as big as one is drawn at the usual sizes, drawn smaller or a little larger from it. */
    static final int MADE=256;

    /**
     * What a folder shown as an album scrolls: the squares, then, under "Notes and other files", the folder's ordinary grid
     * with whatever else it holds, so nothing in it is hidden. As wide as the card, as tall as both.
     */
    static final class View extends JPanel implements Scrollable {
        final Grid grid;final JLabel heading;
        View(Grid grid) {
            super(null);this.grid=grid;setOpaque(false);
            heading=new JLabel(Say.t("Notes and other files"));heading.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,13f));heading.setForeground(DesktopUi.QUIET);
            heading.setBorder(BorderFactory.createEmptyBorder(14,Grid.PAD+2,2,0));
            add(grid);add(heading);
        }
        /** The folder's own grid under the squares, and its heading, shown only where there is anything in it. */
        void others(JComponent others,boolean any){if(others.getParent()!=this)add(others);heading.setVisible(any);others.setVisible(any);revalidate();repaint();}
        @Override public void doLayout(){int w=getWidth(),y=0;for(Component c:getComponents()){if(!c.isVisible())continue;int h=c.getPreferredSize().height;c.setBounds(0,y,w,h);y+=h;}}
        @Override public Dimension getPreferredSize(){int h=0;for(Component c:getComponents())if(c.isVisible())h+=c.getPreferredSize().height;return new Dimension(Math.max(1,getWidth()),h);}
        @Override public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}
        @Override public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-48);}
        @Override public boolean getScrollableTracksViewportWidth(){return true;}
        @Override public boolean getScrollableTracksViewportHeight(){return false;}
    }

    // ---- one picture, large ------------------------------------------------------------------------------------------

    /**
     * A picture large over the window, fitted to it: the one before and the one after with ← and →, or the buttons at its
     * sides; Esc or × to close; at the top its name, where it is ("3 of 24") and ⋯, the picture's own menu, the one its
     * square's right-click opens. Large pictures are decoded on the album's thread at about the window's size, the one in
     * view and those either side of it.
     */
    static final class Viewer extends JComponent {
        final List<Picture> pictures=new ArrayList<>();
        int at;
        private final Function<NoteStore.Held,BufferedImage> making;
        private final java.util.function.BiConsumer<Picture,Component> menu;
        private final Runnable closed;
        private final Map<String,BufferedImage> large=java.util.Collections.synchronizedMap(new java.util.HashMap<>());
        private final java.util.Set<String> asked=java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        final JLabel name=new JLabel(),where=new JLabel();
        final JButton before,after,more,close;
        private static final Color DARK=new Color(18,20,19),LIGHT=new Color(236,236,230),DIM=new Color(170,172,166);
        @Override public javax.accessibility.AccessibleContext getAccessibleContext(){if(accessibleContext==null)accessibleContext=new AccessibleJComponent(){@Override public javax.accessibility.AccessibleRole getAccessibleRole(){return javax.accessibility.AccessibleRole.PANEL;}};return accessibleContext;}

        Viewer(List<Picture> pictures,int at,Function<NoteStore.Held,BufferedImage> making,java.util.function.BiConsumer<Picture,Component> menu,Runnable closed) {
            this.pictures.addAll(pictures);this.at=Math.max(0,Math.min(at,pictures.size()-1));this.making=making;this.menu=menu;this.closed=closed;
            setLayout(null);setOpaque(true);setFocusable(true);setFocusCycleRoot(true);
            name.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,15f));name.setForeground(LIGHT);
            where.setFont(DesktopUi.BODY.deriveFont(13f));where.setForeground(DIM);
            before=button("‹",Say.t("Previous picture (←)"),this::previous);after=button("›",Say.t("Next picture (→)"),this::next);
            more=button("⋯",Say.t("Menu"),()->menu.accept(shown(),moreButton()));close=button("×",Say.t("Close (Esc)"),this::close);
            before.setFont(DesktopUi.BODY.deriveFont(34f));after.setFont(DesktopUi.BODY.deriveFont(34f));close.setFont(DesktopUi.BODY.deriveFont(22f));more.setFont(DesktopUi.BODY.deriveFont(20f));
            for(JComponent c:new JComponent[]{name,where,before,after,more,close})add(c);
            bind("LEFT",this::previous);bind("RIGHT",this::next);bind("ESCAPE",this::close);bind("HOME",()->show(0));bind("END",()->show(this.pictures.size()-1));
            bind("shift F10",()->menu.accept(shown(),moreButton()));bind("CONTEXT_MENU",()->menu.accept(shown(),moreButton()));
            // The window under it is not clicked or scrolled through it; a right-click on the picture is its menu.
            addMouseListener(new MouseAdapter(){
                public void mousePressed(MouseEvent e){requestFocusInWindow();if(e.isPopupTrigger())menu.accept(Viewer.this.pictures.get(Viewer.this.at),Viewer.this);}
                public void mouseReleased(MouseEvent e){if(e.isPopupTrigger())menu.accept(Viewer.this.pictures.get(Viewer.this.at),Viewer.this);}
            });
            addMouseWheelListener(e->{if(e.getWheelRotation()>0)next();else if(e.getWheelRotation()<0)previous();});
            said();
        }
        private JButton button(String face,String said,Runnable does) {
            JButton b=new JButton(face);b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            b.setForeground(LIGHT);b.setFocusable(false);b.setToolTipText(said);b.getAccessibleContext().setAccessibleName(said);b.addActionListener(e->does.run());return b;
        }
        private void bind(String key,Runnable does){getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key),key);getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){does.run();}});}

        Picture shown(){return pictures.isEmpty()?null:pictures.get(at);}
        /** Whether the one shown is decoded, and could be read: null while it is not yet. For the tests. */
        Boolean ready(){Picture now=shown();BufferedImage made=now==null?null:large.get(now.held().id);return made==null?null:made!=NONE;}
        private JButton moreButton(){return more;}
        void next(){show(Album.next(at,pictures.size()));}
        void previous(){show(Album.previous(at,pictures.size()));}
        void show(int to){if(to<0||to>=pictures.size())return;at=to;said();repaint();}
        /** The name and the place said, the buttons there only where there is somewhere to go. */
        private void said() {
            if(pictures.isEmpty())return;
            name.setText(DesktopMenus.shortly(pictures.get(at).held().name));where.setText(Album.position(at,pictures.size()));
            before.setVisible(at>0);after.setVisible(at<pictures.size()-1);
            getAccessibleContext().setAccessibleName(pictures.get(at).held().name+", "+where.getText());
            doLayout();
        }
        void close(){Container up=getParent();if(up!=null){up.remove(this);up.repaint();}closed.run();}
        /**
         * The one shown, gone from the folder since (put in the bin, archived, moved): out of the album, the one after it shown,
         * or the viewer closed where none is left.
         */
        void without(java.util.Set<String> still) {
            List<Picture> left=new ArrayList<>();int keep=0;
            for(int i=0;i<pictures.size();i++){Picture one=pictures.get(i);if(still.contains(one.held().id)){if(i<at)keep++;left.add(one);}}
            if(left.size()==pictures.size())return;
            pictures.clear();pictures.addAll(left);
            if(pictures.isEmpty()){close();return;}
            at=Math.max(0,Math.min(keep,pictures.size()-1));said();repaint();
        }
        static final int BAR=52,SIDE=72;
        @Override public void doLayout() {
            int w=getWidth(),h=getHeight();
            Dimension c=close.getPreferredSize(),m=more.getPreferredSize();
            close.setBounds(w-c.width-12,(BAR-c.height)/2,c.width,c.height);more.setBounds(w-c.width-m.width-16,(BAR-m.height)/2,m.width,m.height);
            Dimension n=name.getPreferredSize(),p=where.getPreferredSize();
            int room=Math.max(40,w-c.width-m.width-48-p.width-16);
            name.setBounds(20,(BAR-n.height)/2,Math.min(n.width,room),n.height);where.setBounds(20+Math.min(n.width,room)+14,(BAR-p.height)/2,p.width+8,p.height);
            Dimension b=before.getPreferredSize();
            before.setBounds(10,(h-b.height)/2,Math.max(b.width,48),b.height);after.setBounds(w-Math.max(b.width,48)-10,(h-b.height)/2,Math.max(b.width,48),b.height);
        }
        /** The room the picture is fitted into: under the bar, between the buttons. */
        Rectangle room(){return new Rectangle(SIDE,BAR,Math.max(1,getWidth()-2*SIDE),Math.max(1,getHeight()-BAR-16));}
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();
            g.setColor(DARK);g.fillRect(0,0,getWidth(),getHeight());
            Picture now=shown();
            if(now!=null) {
                BufferedImage picture=largeOf(now);
                Rectangle r=room();
                if(picture!=null) {
                    // Fitted to the room, as large as the room lets it be: a picture decoded near the window's size fills it.
                    double scale=Math.min(r.width/(double)picture.getWidth(),r.height/(double)picture.getHeight());
                    int fw=Math.max(1,(int)Math.round(picture.getWidth()*scale)),fh=Math.max(1,(int)Math.round(picture.getHeight()*scale));
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
                    g.drawImage(picture,r.x+(r.width-fw)/2,r.y+(r.height-fh)/2,fw,fh,null);
                } else {
                    String said=large.containsKey(now.held().id)?Say.t("This picture cannot be shown here. Its menu can open it in another program."):Say.t("Opening…");
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setFont(DesktopUi.BODY.deriveFont(14f));g.setColor(DIM);FontMetrics m=g.getFontMetrics();
                    g.drawString(said,r.x+(r.width-m.stringWidth(said))/2,r.y+r.height/2);
                }
            }
            g.dispose();
        }
        /** The large picture of this one, made where it is not yet, and those either side of it made next. */
        private BufferedImage largeOf(Picture now) {
            BufferedImage known=large.get(now.held().id);
            if(!asked.contains(now.held().id))make(now);
            int i=pictures.indexOf(now);
            for(int side:new int[]{i+1,i-1})if(side>=0&&side<pictures.size()&&!asked.contains(pictures.get(side).held().id))make(pictures.get(side));
            return known==NONE?null:known;
        }
        private void make(Picture one) {
            String id=one.held().id;asked.add(id);
            MAKING.execute(()->{
                BufferedImage made=null;try{made=making.apply(one.held());}catch(Throwable unreadable){/* said, then */}
                BufferedImage kept=made==null?NONE:made;
                SwingUtilities.invokeLater(()->{
                    // Only those near the one in view are held: a long album browsed through does not fill the memory.
                    large.put(id,kept);
                    synchronized(large){large.keySet().removeIf(k->{int i=indexOf(k);boolean far=i<0||Math.abs(i-at)>2;if(far)asked.remove(k);return far;});}
                    repaint();
                });
            });
        }
        private int indexOf(String id){for(int i=0;i<pictures.size();i++)if(pictures.get(i).held().id.equals(id))return i;return -1;}
    }

    /** The viewer up now, if any, so Esc and the tests reach it. */
    static Viewer up;

    /** A picture of an album, opened large over the window. */
    static void open(Desktop pad,String folder,List<Picture> pictures,int at) {
        if(pictures.isEmpty())return;
        if(up!=null)up.close();
        JLayeredPane layer=pad.frame.getLayeredPane();
        Viewer[] made={null};
        java.util.function.BiConsumer<Picture,Component> menu=(one,on)->{
            JPopupMenu popup=pad.home.fileMenu(one.line());
            // Whatever was chosen in it done, the album asked again: one put in the bin, archived or moved is not browsed to.
            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener(){
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e){}
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e){}
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e){SwingUtilities.invokeLater(()->stillThere(pad,folder,made[0]));}
            });
            Component anchor=on==null?made[0]:on;
            popup.show(anchor,anchor==made[0].more?0:anchor.getWidth()/2,anchor==made[0].more?anchor.getHeight():anchor.getHeight()/2);
        };
        Viewer viewer=new Viewer(pictures,at,larges(pad,layer),menu,()->{if(up!=null&&up==made[0])up=null;pad.home.folder.grid.repaint();
            SwingUtilities.invokeLater(()->{if(pad.home.folder.isOpen())pad.home.folder.focusPicture(made[0].at);});});
        made[0]=viewer;up=viewer;
        viewer.setBounds(0,0,layer.getWidth(),layer.getHeight());
        layer.add(viewer,JLayeredPane.MODAL_LAYER);
        layer.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){if(viewer.getParent()==layer){viewer.setBounds(0,0,layer.getWidth(),layer.getHeight());viewer.revalidate();viewer.repaint();}else layer.removeComponentListener(this);}});
        layer.revalidate();layer.repaint();
        SwingUtilities.invokeLater(viewer::requestFocusInWindow);
    }
    /** After a menu: the pictures still in the folder, read on the disk, and the viewer told. */
    private static void stillThere(Desktop pad,String folder,Viewer viewer) {
        if(viewer==null||viewer.getParent()==null)return;
        pad.disk.submit(()->{java.util.Set<String> ids=new java.util.HashSet<>();for(NoteStore.Held one:pad.store.notAway(pad.store.filesOf(NoteStore.Branch.Kind.COLLECTION,folder)))ids.add(one.id);return ids;},
            ids->{if(viewer.getParent()!=null)viewer.without(ids);},pad::failed);
    }
    /** Esc, or a note opened: the viewer closed where one is up. Whether it was. */
    static boolean closeViewer(){if(up==null||up.getParent()==null){up=null;return false;}up.close();return true;}
}
