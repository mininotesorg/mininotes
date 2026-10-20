package org.mininotes.android;

import javax.swing.Timer;
import java.util.function.*;

/** One EDT clock for Home movement. Fractional input survives rounding; ticks only move the viewport. */
final class DesktopMotion {
    interface Move {boolean by(int x,int y);}
    private final Move move;
    private final Timer timer=new Timer(16,e->tick());
    private long last,edgeAfter;private double x,y,fractionX,fractionY;
    private DoubleConsumer edge;
    DesktopMotion(Move move){this.move=move;timer.setCoalesce(true);}
    void stop(){timer.stop();x=y=fractionX=fractionY=0;edge=null;last=0;}
    void wheel(double dx,double dy,boolean precise){
        edge=null;
        if(precise){timer.stop();x=y=0;pixels(dx,dy);return;}
        x+=dx;y+=dy;start();
    }
    void to(double dx,double dy){stop();x=dx;y=dy;start();}
    void edge(int delay,DoubleConsumer next){stop();edge=next;edgeAfter=System.nanoTime()+delay*1_000_000L;start();}
    boolean edging(){return edge!=null;}
    private void start(){if(!timer.isRunning()){last=System.nanoTime();timer.start();}}
    private void tick(){long now=System.nanoTime();double seconds=Math.min(.05,(now-last)/1e9);last=now;
        if(edge!=null){if(now>=edgeAfter)edge.accept(seconds);return;}
        double portion=1-Math.exp(-seconds*16),dx=x*portion,dy=y*portion;x-=dx;y-=dy;pixels(dx,dy);
        if(Math.abs(x)+Math.abs(y)<.4){pixels(x,y);x=y=0;timer.stop();}
    }
    void pixels(double dx,double dy){fractionX+=dx;fractionY+=dy;int ix=(int)Math.round(fractionX),iy=(int)Math.round(fractionY);fractionX-=ix;fractionY-=iy;if(ix!=0||iy!=0)move.by(ix,iy);}
}
