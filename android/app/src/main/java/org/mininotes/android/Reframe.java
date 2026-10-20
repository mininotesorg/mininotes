package org.mininotes.android;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Consumer;

/**
 * A picture framed before it is kept (the owner, 2026-10-12: "when we choose a picture we should be able to perform basic
 * actions on it like reframing"; decision 159): the picture under a round window, moved with a finger, made bigger or
 * smaller with two, turned a quarter at a time; Use keeps what the round shows, square, and Back keeps nothing. The picture
 * always fills the round, so there is never an empty edge in it.
 */
final class Reframe {
    private Reframe(){}

    /** Over everything; {@code done} is given the square the round showed, at {@code side} pixels. */
    static void open(MainActivity a,Bitmap picture,int side,Consumer<Bitmap> done) {
        Dialog over=new Dialog(a,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Frame frame=new Frame(a,picture);
        FrameLayout stage=new FrameLayout(a);stage.setBackgroundColor(0xFF000000);
        stage.addView(frame,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(24));
        TextView back=a.label("Back",MainActivity.READING,0xFFFFFFFF);back.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));
        back.setOnClickListener(v->over.dismiss());
        TextView turn=a.label("↻",Math.round(MainActivity.READING*1.4f),0xFFFFFFFF);turn.setPadding(a.dp(16),a.dp(8),a.dp(16),a.dp(8));
        turn.setContentDescription("Turn a quarter");turn.setOnClickListener(v->frame.turn());
        View use=a.primary("Use",()->{Bitmap made=frame.square(side);over.dismiss();done.accept(made);});
        bar.addView(back);
        View gap=new View(a);bar.addView(gap,new LinearLayout.LayoutParams(0,1,1));
        bar.addView(turn);
        bar.addView(use,new LinearLayout.LayoutParams(a.dp(120),-2));
        FrameLayout.LayoutParams foot=new FrameLayout.LayoutParams(-1,-2);foot.gravity=Gravity.BOTTOM;
        stage.addView(bar,foot);
        TextView said=a.label("Move and zoom with your fingers",MainActivity.QUIET,0xCCFFFFFF);said.setGravity(Gravity.CENTER);said.setPadding(0,a.dp(48),0,0);
        FrameLayout.LayoutParams head=new FrameLayout.LayoutParams(-1,-2);head.gravity=Gravity.TOP;
        stage.addView(said,head);
        over.setContentView(stage);over.show();
    }

    /** The picture and the round over it. */
    static final class Frame extends View {
        private final Bitmap picture;private int quarter;
        private float scale=1f,least=1f,dx,dy;private float lastX,lastY;private boolean placed;
        private final Paint drawn=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG),shade=new Paint(),ring=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ScaleGestureDetector pinch;

        Frame(MainActivity a,Bitmap picture) {
            super(a);this.picture=picture;
            shade.setColor(0xB0000000);ring.setStyle(Paint.Style.STROKE);ring.setColor(0xFFFFFFFF);ring.setStrokeWidth(Math.max(2f,a.dp(2)));
            pinch=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(ScaleGestureDetector g) {
                    float before=scale;scale=Math.max(least,Math.min(least*8f,scale*g.getScaleFactor()));
                    float f=scale/before,cx=g.getFocusX(),cy=g.getFocusY();
                    dx=cx-(cx-dx)*f;dy=cy-(cy-dy)*f;keepCovered();invalidate();return true;
                }
            });
            setContentDescription("The picture, to move and zoom");
        }

        /** The round: in the middle, as wide as the screen lets it with room around. */
        private RectF round(){float s=Math.min(getWidth(),getHeight())*0.82f;return new RectF((getWidth()-s)/2f,(getHeight()-s)/2f,(getWidth()+s)/2f,(getHeight()+s)/2f);}
        /** The picture's sides as drawn, turned. */
        private float wide(){return (quarter%2==0?picture.getWidth():picture.getHeight())*scale;}
        private float high(){return (quarter%2==0?picture.getHeight():picture.getWidth())*scale;}

        private void place() {
            RectF r=round();
            least=Math.max(r.width()/(quarter%2==0?picture.getWidth():picture.getHeight()),r.height()/(quarter%2==0?picture.getHeight():picture.getWidth()));
            scale=least;dx=r.centerX()-wide()/2f;dy=r.centerY()-high()/2f;placed=true;
        }
        /** The picture never leaves part of the round empty. */
        private void keepCovered() {
            RectF r=round();
            dx=Math.min(r.left,Math.max(r.right-wide(),dx));dy=Math.min(r.top,Math.max(r.bottom-high(),dy));
        }
        void turn(){quarter=(quarter+1)%4;place();invalidate();}

        private Matrix matrix() {
            Matrix m=new Matrix();
            m.postRotate(90f*quarter);
            // Back into view after the turn: its top left where the turned picture's is.
            float w=picture.getWidth(),h=picture.getHeight();
            if(quarter==1)m.postTranslate(h,0);else if(quarter==2)m.postTranslate(w,h);else if(quarter==3)m.postTranslate(0,w);
            m.postScale(scale,scale);m.postTranslate(dx,dy);
            return m;
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);place();}

        @Override protected void onDraw(Canvas canvas) {
            if(!placed)place();
            canvas.drawBitmap(picture,matrix(),drawn);
            RectF r=round();
            Path outside=new Path();outside.setFillType(Path.FillType.EVEN_ODD);
            outside.addRect(0,0,getWidth(),getHeight(),Path.Direction.CW);outside.addOval(r,Path.Direction.CW);
            canvas.drawPath(outside,shade);canvas.drawOval(r,ring);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            pinch.onTouchEvent(e);
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:lastX=e.getX();lastY=e.getY();return true;
                case MotionEvent.ACTION_POINTER_UP:case MotionEvent.ACTION_POINTER_DOWN:lastX=e.getX(0);lastY=e.getY(0);return true;
                case MotionEvent.ACTION_MOVE:
                    if(!pinch.isInProgress()&&e.getPointerCount()==1){dx+=e.getX()-lastX;dy+=e.getY()-lastY;lastX=e.getX();lastY=e.getY();keepCovered();invalidate();}
                    return true;
                default:return true;
            }
        }

        /** What the round shows, as a square of {@code side} pixels. */
        Bitmap square(int side) {
            RectF r=round();
            Bitmap out=Bitmap.createBitmap(side,side,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(out);
            Matrix m=matrix();m.postTranslate(-r.left,-r.top);m.postScale(side/r.width(),side/r.width());
            c.drawColor(0xFFFFFFFF);c.drawBitmap(picture,m,drawn);
            return out;
        }
    }
}
