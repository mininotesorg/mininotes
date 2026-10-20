package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;

/**
 * The attachments of the open note as small cards under the page: a picture shows itself, anything else its
 * kind; the name and size under it; a click opens it; the cross deletes it, after one question.
 */
final class DesktopFileCards extends JPanel {
    private final Desktop app;
    private String showing="";

    /**
     * The cards in one row that slides sideways, as on the phone. It was a row that simply stopped at the
     * window's edge: four cards of eight showed, and nothing said there were more or let them be reached. Now
     * the last card that fits is cut by the edge, which says there are more; a thin bar under them shows how
     * far along the row is; and the wheel moves it sideways, since a row has no up and down.
     */
    private final JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
    private final JScrollPane slide=new JScrollPane(row,ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    DesktopFileCards(Desktop app) {
        super(new BorderLayout());this.app=app;
        // A drawer over the foot of the page: the paper's own colour and a rule on top, so the writing it covers
        // does not show through between the cards.
        setOpaque(true);setBackground(DesktopUi.PAPER);setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,DesktopUi.LINE),BorderFactory.createEmptyBorder(10,38,0,38)));setVisible(false);
        row.setOpaque(false);row.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));
        slide.setOpaque(false);slide.getViewport().setOpaque(false);slide.setBorder(BorderFactory.createEmptyBorder());
        slide.getHorizontalScrollBar().setUnitIncrement(40);
        slide.getHorizontalScrollBar().putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,"width:8;thumbInsets:2,0,0,0");
        slide.setWheelScrollingEnabled(false);
        slide.addMouseWheelListener(e->{JScrollBar bar=slide.getHorizontalScrollBar();bar.setValue(bar.getValue()+(int)Math.round(e.getPreciseWheelRotation()*60));});
        add(slide);
    }

    private int drawn=-1;
    /**
     * Folded away unless asked for. A row of cards under every note took a fifth of the window from the writing,
     * whether or not anybody wanted the files just then; now the paperclip holds them, and they slide up over
     * the foot of the page while the pointer is on it or on them.
     */
    private boolean open;
    void open(boolean wanted){open=wanted;setVisible(open&&drawn>0);revalidate();repaint();if(getParent()!=null)getParent().revalidate();}
    boolean isOpen(){return open&&isVisible();}
    /** Whether these cards already show this many files of this note: nothing to draw again. */
    boolean shows(String note,int count){return showing.equals(note==null?"":note)&&drawn==count;}

    /** The cards for this note, drawn again: thumbnails are made off the event thread. */
    /** The pages drawn for documents' cards, by file and size; NONE where nothing could be drawn. */
    private final java.util.Map<String,BufferedImage> pages=new java.util.concurrent.ConcurrentHashMap<>();
    private static final BufferedImage NONE=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    private static final java.util.concurrent.ExecutorService PREVIEWS=java.util.concurrent.Executors.newSingleThreadExecutor(work->{
        Thread one=new Thread(work,"mininotes-previews");one.setDaemon(true);one.setPriority(Thread.MIN_PRIORITY);return one;});

    void show(String note) {
        if(!showing.equals(note==null?"":note))stopPlaying();
        showing=note==null?"":note;
        if(note==null){row.removeAll();setVisible(false);revalidate();return;}
        app.disk.submit(()->{
            List<NoteStore.Held> files=app.store.notAway(app.store.filesOf(NoteStore.Branch.Kind.PAGE,note));
            java.util.Map<String,BufferedImage> thumbs=new java.util.HashMap<>();
            java.util.Map<String,Long> lengths=new java.util.HashMap<>();
            for(NoteStore.Held file:files)if(picture(file))try{BufferedImage made=thumbnail(file);if(made!=null)thumbs.put(file.id,made);}catch(Exception unreadable){/* shown by its kind */}
            // A document's page is drawn on a thread of its own, never on the one the note's saves wait on (decision 208):
            // what is drawn already is used, what is not is drawn next and the cards shown again.
            List<NoteStore.Held> toDraw=new java.util.ArrayList<>();
            for(NoteStore.Held file:files)if(!picture(file)&&Peek.previewed(file.name,file.kind,file.bytes)) {
                String key=file.id+":"+file.bytes;
                if(pages.containsKey(key)){BufferedImage made=pages.get(key);if(made!=NONE)thumbs.put(file.id,made);}else toDraw.add(file);
            }
            if(!toDraw.isEmpty())PREVIEWS.execute(()->{
                for(NoteStore.Held file:toDraw){BufferedImage made=null;try{made=thumbnail(file);}catch(Throwable unreadable){/* its kind, then */}
                    pages.put(file.id+":"+file.bytes,made==null?NONE:made);}
                SwingUtilities.invokeLater(()->{if(showing.equals(note))show(note);});
            });
            // How long each recording is, read from its own bytes, so the card says it before ▶ is pressed.
            for(NoteStore.Held file:files)if(Recording.audio(file.name,file.kind)&&file.bytes<=Attachment.LIMIT)try{lengths.put(file.id,Recording.millis(bytes(file)));}catch(Exception unreadable){/* no length said */}
            // Which are stamped on Minima, and how far, for the mark on their cards (decision 224).
            java.util.Map<String,NoteStore.StampRow> stamped=new java.util.HashMap<>();
            for(NoteStore.Held file:files){List<NoteStore.StampRow> all=app.store.stampsOf(file.id);if(!all.isEmpty())stamped.put(file.id,all.get(0));}
            return new Object[]{files,thumbs,app.store.fileStates(note,Node.onlyMine(app.context)),lengths,stamped};
        },loaded->{
            if(!showing.equals(note))return;
            @SuppressWarnings("unchecked") List<NoteStore.Held> files=(List<NoteStore.Held>)loaded[0];
            @SuppressWarnings("unchecked") java.util.Map<String,BufferedImage> thumbs=(java.util.Map<String,BufferedImage>)loaded[1];
            @SuppressWarnings("unchecked") java.util.Map<String,String> states=(java.util.Map<String,String>)loaded[2];
            @SuppressWarnings("unchecked") java.util.Map<String,Long> lengths=(java.util.Map<String,Long>)loaded[3];
            @SuppressWarnings("unchecked") java.util.Map<String,NoteStore.StampRow> stamped=(java.util.Map<String,NoteStore.StampRow>)loaded[4];
            row.removeAll();faces.clear();
            for(NoteStore.Held file:files)row.add(card(file,thumbs.get(file.id),states.getOrDefault(file.id,""),lengths.getOrDefault(file.id,-1L),stamped.get(file.id)));
            // Still playing if it is still here: the player finds its new card by the file's id.
            if(playing!=null&&!faces.containsKey(playing))stopPlaying();
            drawn=files.size();
            setVisible(open&&drawn>0);revalidate();repaint();if(getParent()!=null)getParent().revalidate();
        },app::failed);
    }

    /** The cards drawn again as they stand: a stamp sent, landed or found shows on its file's card (decision 224). */
    void again(){if(!showing.isEmpty())show(showing);}

    static boolean picture(NoteStore.Held file) {
        return (file.kind!=null&&file.kind.startsWith("image/"))||pictureNamed(file.name);
    }
    /** Whether a file's name says it is a picture this PC can show. */
    static boolean pictureNamed(String name) {
        String low=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
        return low.endsWith(".png")||low.endsWith(".jpg")||low.endsWith(".jpeg")||low.endsWith(".gif")||low.endsWith(".bmp");
    }

    /** A picture, read through the lock if there is one, made small. */
    private BufferedImage thumbnail(NoteStore.Held file) throws Exception{return thumbnail(file,132,72);}
    /** The same at another size. */
    BufferedImage thumbnail(NoteStore.Held file,int w,int h) throws Exception {
        if(file.bytes>20L*1024*1024)return null;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DesktopFiles.copyOut(app.context,app.store.fileFor(file.id).toPath(),bytes);
        // A document: its first page, or its first words on a small page (decision 208).
        if(!picture(file))return page(bytes.toByteArray(),file.name,file.kind,w,h);
        BufferedImage whole=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        if(whole==null)return null;
        double scale=Math.max((double)w/whole.getWidth(),(double)h/whole.getHeight());
        BufferedImage small=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=small.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int sw=(int)Math.round(whole.getWidth()*scale),sh=(int)Math.round(whole.getHeight()*scale);
        g.drawImage(whole,(w-sw)/2,(h-sh)/2,sw,sh,null);g.dispose();
        return small;
    }

    /**
     * A document's card face: a PDF's first page drawn by PDFBox, the top of it across the card; anything else whose words
     * can be read, those words on a small white page. Null where there is nothing to draw, so its kind is shown.
     */
    static BufferedImage page(byte[] bytes,String name,String kind,int w,int h) {
        if(Peek.pdf(name,kind)) {
            try(org.apache.pdfbox.pdmodel.PDDocument pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
                if(pdf.getNumberOfPages()==0)return null;
                float wide=pdf.getPage(0).getMediaBox().getWidth();
                BufferedImage drawn=new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImage(0,Math.max(0.05f,w*2f/Math.max(1f,wide)));
                return top(drawn,w,h);
            } catch(Throwable unreadable){return null;}
        }
        List<String> lines=Peek.lines(bytes,name,kind,8);
        if(lines.isEmpty())return null;
        BufferedImage paper=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=paper.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRect(0,0,paper.getWidth(),paper.getHeight());
        g.setColor(new Color(60,64,67));Font small=DesktopUi.BODY.deriveFont(15f);g.setFont(small);
        FontMetrics m=g.getFontMetrics();int y=14+m.getAscent(),x=16,room=paper.getWidth()-2*x;
        for(String line:lines) {
            // Each line to the width, cut with an ellipsis: a glance at it, not the document.
            String said=line;while(said.length()>1&&m.stringWidth(said+"…")>room)said=said.substring(0,said.length()-1);
            g.drawString(said.length()<line.length()?said+"…":said,x,y);y+=m.getHeight();
            if(y>paper.getHeight())break;
        }
        g.dispose();
        return top(paper,w,h);
    }
    /** The top of a page across the card's width, made small. */
    private static BufferedImage top(BufferedImage whole,int w,int h) {
        double scale=(double)w/whole.getWidth();
        BufferedImage small=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=small.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);g.fillRect(0,0,w,h);
        g.drawImage(whole,0,0,w,(int)Math.round(whole.getHeight()*scale),null);g.dispose();
        return small;
    }

    private JComponent card(NoteStore.Held file,BufferedImage thumb,String going,long millis,NoteStore.StampRow stamp) {
        JPanel card=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DesktopUi.CARD);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g.setColor(DesktopUi.LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);g.dispose();
            }
        };
        card.setOpaque(false);card.setPreferredSize(new Dimension(148,122));card.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        // The picture, or the file's kind on a tile.
        JComponent face;
        if(thumb!=null){JLabel image=new JLabel(new ImageIcon(thumb));face=image;}
        else if(Recording.audio(file.name,file.kind))face=listen(file,millis);
        else {
            String kind=extension(file.name);
            JLabel tile=new JLabel(kind,SwingConstants.CENTER){
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(232,239,234));g.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g.dispose();super.paintComponent(g0);
                }
            };
            tile.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,15f));tile.setForeground(Desktop.ACCENT);tile.setPreferredSize(new Dimension(132,72));face=tile;
        }
        // The name and size, and the cross beside them.
        JLabel name=new JLabel(file.name);name.setFont(DesktopUi.BODY.deriveFont(12.5f));name.setForeground(Desktop.INK);
        // Its size, and where it is while it has not reached everybody the note is shared with - in the accent,
        // and gone once it has.
        JLabel size=new JLabel(going.isEmpty()?Attachment.size(file.bytes):Attachment.size(file.bytes)+" · "+going);
        size.setFont(DesktopUi.BODY.deriveFont(11.5f));size.setForeground(going.isEmpty()?Desktop.QUIET:Desktop.ACCENT);
        if(!going.isEmpty())size.setToolTipText(going);
        JPanel words=new JPanel(new BorderLayout());words.setOpaque(false);words.add(name,BorderLayout.NORTH);words.add(size,BorderLayout.SOUTH);
        // Stamped on Minima: a small stamp before its name, saying how far, and a click on it opens the stamp's box (decision 224).
        if(stamp!=null) {
            JLabel mark=new JLabel(DesktopIcons.glyph("stamp",14));mark.setName("stampMark");mark.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
            mark.setToolTipText(Integritas.noteLine(stamp,false));mark.getAccessibleContext().setAccessibleName(Integritas.noteLine(stamp,false));
            mark.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            mark.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)){e.consume();DesktopStamp.show(app,stamp.n());}}});
            words.add(mark,BorderLayout.WEST);
        }
        JButton delete=new JButton("×");delete.setFocusPainted(false);delete.setMargin(new Insets(0,4,0,4));delete.setFont(DesktopUi.BODY.deriveFont(15f));
        delete.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        delete.setToolTipText("Move to bin");delete.getAccessibleContext().setAccessibleName("Delete "+file.name);
        delete.setEnabled(app.page.isEditable());
        delete.addActionListener(e->delete(file));
        JPanel foot=new JPanel(new BorderLayout(4,0));foot.setOpaque(false);foot.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));
        foot.add(words);foot.add(delete,BorderLayout.EAST);
        card.add(face);card.add(foot,BorderLayout.SOUTH);
        card.setToolTipText("Open "+file.name);card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter open=new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))open(file);}};
        card.addMouseListener(open);face.addMouseListener(open);words.addMouseListener(open);
        DesktopMenus.onRightClick(card,e->menu(file).show(e.getComponent(),e.getX(),e.getY()));
        return card;
    }

    /** What can be done to one attached file: the card's click and cross, and what the paperclip and the Send box do. */
    JPopupMenu menu(NoteStore.Held file) {
        JPopupMenu menu=new JPopupMenu();
        JMenuItem open=DesktopMenus.item(DesktopApps.openLine(file.name));open.addActionListener(e->open(file));menu.add(open);
        JMenuItem with=DesktopMenus.item("Open with…");with.addActionListener(e->DesktopOpening.openWith(app,file));menu.add(with);
        JMenuItem clipboard=DesktopMenus.item("Copy");clipboard.addActionListener(e->app.copyFile(file));menu.add(clipboard);
        // Everything a file has elsewhere that an attachment can use (the owner, 2026-10-19: "A right click on an attachment should
        // also bring as many functionalities as possible, including stamping on Minima"; decision 224), each Home's file menu's own.
        NoteStore.Branch line=new NoteStore.Branch(NoteStore.Branch.Kind.FILE,file.id,file.note,file.name,"",0,0,false);
        if(app.onDisk!=null&&app.onDisk.linkedFile(file.id)){JMenuItem explorer=DesktopMenus.item("Show in Explorer");explorer.addActionListener(e->DesktopOnDisk.showInExplorer(app,file.id));menu.add(explorer);}
        // Written in by whoever may (decision 93): offered once the notebook says this PC may.
        JMenuItem rename=DesktopMenus.item("Rename…");rename.addActionListener(e->app.home.renameFile(line));rename.setVisible(false);menu.add(rename);
        JMenuItem replace=DesktopMenus.item("Replace with another file…");replace.addActionListener(e->app.home.replaceFile(line));replace.setVisible(false);menu.add(replace);
        app.disk.submit(()->app.store.mayChangeFile(file.id),may->{rename.setVisible(may);replace.setVisible(may);if(menu.isVisible())menu.pack();},e->{});
        JMenuItem stamp=DesktopMenus.item("Stamp on Minima…");stamp.addActionListener(e->DesktopStamp.files(app,java.util.List.of(file)));menu.add(stamp);
        JMenuItem verify=DesktopMenus.item("Verify on Minima…");verify.addActionListener(e->DesktopStamp.verifyFile(app,file));menu.add(verify);
        // A file stamped from here: its menu starts with its stamp, which opens the stamp's box (decisions 218 and 224).
        DesktopStamp.fileLine(app,menu,file.id);
        JMenuItem save=DesktopMenus.item("Save on my computer…");save.addActionListener(e->app.saveCopy(file));menu.add(save);
        JMenuItem send=DesktopMenus.item("Send to a device…");send.addActionListener(e->send(file));menu.add(send);
        JMenuItem move=DesktopMenus.item("Move to another note…");move.setEnabled(app.page.isEditable());move.addActionListener(e->app.home.intoNote(line));menu.add(move);
        menu.addSeparator();
        JMenuItem delete=DesktopMenus.item("Move to bin");delete.setEnabled(app.page.isEditable());delete.addActionListener(e->delete(file));menu.add(delete);
        return menu;
    }

    /**
     * Straight to one of the devices, in no note, as files dropped on the window go: a copy is taken out of the
     * notebook - opened on the way if it is locked - for the Send box, and deleted when Mininotes closes.
     */
    void send(NoteStore.Held file) {
        app.disk.submit(()->{
            Path room=Files.createTempDirectory("mininotes-");room.toFile().deleteOnExit();
            Path copy=room.resolve(DesktopFiles.onDisk(file.name));
            try(java.io.OutputStream out=Files.newOutputStream(copy)){DesktopFiles.copyOut(app.context,app.store.fileFor(file.id).toPath(),out);}
            copy.toFile().deleteOnExit();
            return copy;
        },copy->DesktopDrops.send(app,List.of(copy)),app::failed);
    }

    // ---- listening, without leaving the note ------------------------------------------------------------------

    /** What one sound's card shows while it plays: ▶ or ⏸, where it has got to, and a thin line for how far. */
    private static final class Face {
        final long millis;final JButton button=new JButton();final JLabel time=new JLabel();
        float done;
        final JComponent line=new JComponent(){
            @Override protected void paintComponent(Graphics g) {
                g.setColor(DesktopUi.LINE);g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(Desktop.ACCENT);g.fillRect(0,0,Math.round(getWidth()*done),getHeight());
            }
        };
        Face(long millis){this.millis=millis;line.setPreferredSize(new Dimension(10,3));}
    }
    private final java.util.Map<String,Face> faces=new java.util.HashMap<>();
    /** One sound at a time, as a player does: the file playing, and Java Sound's clip of it. */
    private String playing;
    private javax.sound.sampled.Clip clip;
    private final javax.swing.Timer tick=new javax.swing.Timer(100,e->ticked());

    /**
     * A sound's face: ▶ and how long it is. A WAV - what the PC records - plays here, on the card. Java Sound
     * cannot decode what the phone records (AAC) or an MP3, and no small decoder the GPL can take in exists, so
     * those go to the PC's own player, as a click on any other file does; the card says so before it is pressed.
     */
    private JComponent listen(NoteStore.Held file,long millis) {
        boolean here=Recording.wav(file.name,file.kind);
        JPanel tile=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(232,239,234));g.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g.dispose();
            }
        };
        tile.setOpaque(false);tile.setPreferredSize(new Dimension(132,72));tile.setBorder(BorderFactory.createEmptyBorder(8,6,10,10));
        Face face=new Face(millis);
        JButton play=face.button;play.setFocusPainted(false);play.setMargin(new Insets(2,4,2,4));
        play.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        play.setToolTipText(here?null:"Opens in your player");
        play.addActionListener(e->{if(here)toggle(file);else open(file);});
        face.time.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,15f));face.time.setForeground(Desktop.ACCENT);
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,2,0));top.setOpaque(false);top.add(play);top.add(face.time);
        tile.add(top,BorderLayout.NORTH);
        if(here)tile.add(face.line,BorderLayout.SOUTH);
        else{JLabel away=new JLabel("Opens in your player");away.setFont(DesktopUi.BODY.deriveFont(11.5f));away.setForeground(Desktop.QUIET);away.setBorder(BorderFactory.createEmptyBorder(4,8,0,0));tile.add(away,BorderLayout.SOUTH);}
        faces.put(file.id,face);draw(file.id,face,file.name);
        return tile;
    }

    /** ▶ on a WAV: plays it from its start, or pauses it, or goes on from where it was paused. */
    private void toggle(NoteStore.Held file) {
        if(file.id.equals(playing)&&clip!=null) {
            if(clip.isRunning())clip.stop();
            else{if(clip.getFramePosition()>=clip.getFrameLength())clip.setFramePosition(0);clip.start();tick.start();}
            ticked();return;
        }
        stopPlaying();String id=file.id;playing=id;
        app.disk.submit(()->{
            javax.sound.sampled.AudioInputStream in=javax.sound.sampled.AudioSystem.getAudioInputStream(new java.io.ByteArrayInputStream(bytes(file)));
            javax.sound.sampled.AudioFormat was=in.getFormat();
            // μ-law, what the PC records, is widened to plain samples: a sound card's line takes nothing else.
            if(was.getEncoding()!=javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED)
                in=javax.sound.sampled.AudioSystem.getAudioInputStream(new javax.sound.sampled.AudioFormat(was.getSampleRate(),16,was.getChannels(),true,false),in);
            javax.sound.sampled.Clip made=javax.sound.sampled.AudioSystem.getClip();made.open(in);
            return made;
        },made->{
            if(!id.equals(playing)){made.close();return;}
            clip=made;clip.start();tick.start();ticked();
        },e->{playing=null;app.status.setText("This PC could not play "+file.name+". Open it from its menu to use another player.");});
    }

    private void ticked() {
        Face face=playing==null?null:faces.get(playing);
        if(clip!=null&&!clip.isRunning()&&clip.getFramePosition()>=clip.getFrameLength()){clip.setFramePosition(0);tick.stop();}
        if(clip!=null&&!clip.isRunning())tick.stop();
        if(face!=null)draw(playing,face,null);
    }

    /** One card as it stands: ⏸ while it plays, where it is while it is part way, its length otherwise. */
    private void draw(String id,Face face,String name) {
        boolean mine=id.equals(playing)&&clip!=null,running=mine&&clip.isRunning();
        long at=mine?clip.getMicrosecondPosition()/1000:0,length=mine?clip.getMicrosecondLength()/1000:face.millis;
        face.button.setIcon(running?PAUSE:PLAY);
        if(name!=null)face.button.getAccessibleContext().setAccessibleName("Play "+name);
        face.button.getAccessibleContext().setAccessibleDescription(running?"Pause":"Play");
        face.time.setText(at>0?Recording.clock(at):length>=0?Recording.clock(length):"");
        face.done=length>0?Math.min(1f,(float)at/length):0;face.line.repaint();
    }

    /** Stopped and let go: another note shown, the card gone, or another sound pressed. */
    void stopPlaying() {
        String was=playing;playing=null;tick.stop();
        if(clip!=null){clip.stop();clip.close();clip=null;}
        Face face=was==null?null:faces.get(was);
        if(face!=null)draw(was,face,null);
    }

    /** A kept file's own bytes, opened through the lock if there is one. */
    private byte[] bytes(NoteStore.Held file) throws java.io.IOException {
        ByteArrayOutputStream all=new ByteArrayOutputStream();DesktopFiles.copyOut(app.context,app.store.fileFor(file.id).toPath(),all);return all.toByteArray();
    }

    private static final Icon PLAY=shape(false),PAUSE=shape(true);
    /** ▶ and ⏸ drawn, in the accent: as characters Windows gives them its emoji colours. */
    private static Icon shape(boolean pause) {
        return new Icon(){
            public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g0,int x,int y) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Desktop.ACCENT);
                if(pause){g.fillRoundRect(x+3,y+2,4,12,2,2);g.fillRoundRect(x+9,y+2,4,12,2,2);}
                else g.fill(new java.awt.geom.Path2D.Float(){{moveTo(x+3,y+1.5);lineTo(x+14.5,y+8);lineTo(x+3,y+14.5);closePath();}});
                g.dispose();
            }
        };
    }

    static String extension(String name){return DropList.type(name);}

    /** Opened in the program it opens in: to work in where this PC may change it, its saves coming back. */
    private void open(NoteStore.Held file){DesktopOpening.open(app,file);}

    /**
     * Into the bin, with no question (the owner, 2026-10-18: "no need for this confirmation, it goes in the bin"; decision
     * 193): out of the note's strip at once, and back into the note from the bin. Gone for good only when the bin is emptied.
     */
    private void delete(NoteStore.Held file) {
        String note=showing;
        app.disk.submit(()->{app.store.putAway(NoteStore.Branch.Kind.FILE,file.id,true,true);return null;},
            done->{app.status.setText(Say.t("“{0}” is in the bin",file.name));show(note);app.refreshStanding();app.refresh();},app::failed);
    }
}
