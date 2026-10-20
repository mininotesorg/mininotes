package org.mininotes.android;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

/**
 * A person's picture on this PC (decision 158): read from the notebook once and kept, so every round - under a note's title,
 * on a line of People, in Profile - draws it where the initial was, and their round's menu shows it big. Yours under "".
 */
final class DesktopFaces {
    private DesktopFaces(){}
    private static final BufferedImage NONE=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
    private static final Map<String,BufferedImage> KEPT=new ConcurrentHashMap<>();
    private static final Map<String,Boolean> ASKED=new ConcurrentHashMap<>();
    /** Moved on each time what is kept is forgotten: a reading begun before is not kept after. */
    private static final java.util.concurrent.atomic.AtomicLong AGE=new java.util.concurrent.atomic.AtomicLong();
    private static volatile Runnable redraw=()->{};

    private static volatile Desktop app;
    /** The window whose notebook the pictures are read from, drawn again when one is read. */
    static void use(Desktop window){app=window;redraw=()->{for(Window w:Window.getWindows())if(w.isShowing())w.repaint();};}
    /** The same, from the app in use. */
    static BufferedImage of(String address){return of(app,address);}

    /** A person's picture, or null: read in the background the first time it is asked for, then drawn. Null address: yours. */
    static BufferedImage of(Desktop app,String address) {
        String key=address==null?"":address;
        BufferedImage kept=KEPT.get(key);
        if(kept!=null)return kept==NONE?null:kept;
        if(app!=null&&ASKED.putIfAbsent(key,true)==null) {
            long age=AGE.get();
            app.disk.submit(()->picture(app.store.faceOf(address)),picture->{
                ASKED.remove(key);
                if(age==AGE.get())KEPT.put(key,picture==null?NONE:picture);
                redraw.run();
            },e->ASKED.remove(key));
        }
        return null;
    }
    /** Read again the next time it is asked for: a picture set or taken off. Everything, where address is null and all is said. */
    static void forget(String address){AGE.incrementAndGet();if(address==null)KEPT.remove("");else KEPT.remove(address);}
    static void forgetAll(){AGE.incrementAndGet();KEPT.clear();}

    static BufferedImage picture(byte[] bytes) {
        if(bytes==null||bytes.length==0)return null;
        try{return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));}catch(Exception unreadable){return null;}
    }

    /** The picture drawn into a round, filling it. */
    static void paintRound(Graphics2D g,BufferedImage picture,int x,int y,int side) {
        Graphics2D in=(Graphics2D)g.create();
        in.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        in.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        in.clip(new Ellipse2D.Float(x,y,side,side));
        float scale=Math.max(side/(float)picture.getWidth(),side/(float)picture.getHeight());
        int w=Math.round(picture.getWidth()*scale),h=Math.round(picture.getHeight()*scale);
        in.drawImage(picture,x+(side-w)/2,y+(side-h)/2,w,h,null);
        in.dispose();
    }

    /** The picture big, with rounded corners: what a tap on a round shows first. */
    static JComponent big(BufferedImage picture,int side) {
        // A label, not a bare component: a bare one has no accessible context for a screen reader to name.
        JLabel shown=new JLabel(){
            {setPreferredSize(new Dimension(side,side));setOpaque(false);}
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int s=Math.min(getWidth(),getHeight()),x=(getWidth()-s)/2;
                g.clip(new RoundRectangle2D.Float(x,0,s,s,28,28));
                float scale=Math.max(s/(float)picture.getWidth(),s/(float)picture.getHeight());
                int w=Math.round(picture.getWidth()*scale),h=Math.round(picture.getHeight()*scale);
                g.drawImage(picture,x+(s-w)/2,(s-h)/2,w,h,null);g.dispose();
            }
        };
        shown.getAccessibleContext().setAccessibleName("Picture");
        return shown;
    }
}
