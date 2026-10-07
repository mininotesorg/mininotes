package org.mininotes.android;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * How the PC draws a picture smaller (decision 112): ImageIO, in memory and never through its cache files, every n-th
 * pixel read where the picture is far bigger than it needs to be, then scaled smoothly and turned as its camera said. The
 * rules are {@link Shrink}'s, the same on the phone; the private notes (decision 111) draw with this too. What the PC
 * cannot read (a HEIC from a phone) is kept as it is.
 */
final class DesktopPictures implements Shrink.Painter<BufferedImage> {
    static final DesktopPictures PAINTER=new DesktopPictures();
    private DesktopPictures(){}

    @Override public int[] size(byte[] raw) {
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(raw))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())return null;
            ImageReader reader=readers.next();
            try{reader.setInput(in,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);return w>0&&h>0&&w<=30000&&h<=30000?new int[]{w,h}:null;}
            finally{reader.dispose();}
        } catch(IOException|RuntimeException unreadable){return null;}
    }

    /** A JPEG's EXIF says it; ImageIO turns nothing by itself, and the PC reads no other picture's. */
    @Override public int turn(byte[] raw){return "jpeg".equals(Thumb.kind(raw))?Shrink.orientation(raw):1;}

    @Override public BufferedImage open(byte[] raw,int turn) throws IOException {
        BufferedImage picture;
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(raw))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())return null;
            ImageReader reader=readers.next();
            try {
                reader.setInput(in,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>30000||h>30000)return null;
                ImageReadParam param=reader.getDefaultReadParam();
                // Never under the longest side it is made at: every second pixel of a 4000 is still 2000.
                int step=Math.max(1,Math.max(w,h)/Shrink.LONGEST);
                if(step>1)param.setSourceSubsampling(step,step,0,0);
                picture=reader.read(0,param);
            } finally{reader.dispose();}
        }
        if(picture==null)return null;
        int w=picture.getWidth(),h=picture.getHeight();
        double scale=Math.min(1.0,Shrink.LONGEST/(double)Math.max(w,h));
        int sw=Math.max(1,(int)Math.round(w*scale)),sh=Math.max(1,(int)Math.round(h*scale));
        boolean alpha=picture.getColorModel().hasAlpha();
        BufferedImage made=new BufferedImage(sw,sh,alpha?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        Graphics2D g=made.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(picture,0,0,sw,sh,null);g.dispose();picture.flush();
        return upright(made,turn);
    }

    /** Turned as EXIF says (1 to 8), the whole picture, not only a square of it, keeping whether it can be seen through. */
    static BufferedImage upright(BufferedImage in,int turn) {
        if(turn<2||turn>8)return in;
        int w=in.getWidth(),h=in.getHeight();boolean swap=turn>=5;
        BufferedImage out=new BufferedImage(swap?h:w,swap?w:h,in.getColorModel().hasAlpha()?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        AffineTransform t=new AffineTransform();
        switch(turn) {
            case 2 -> {t.translate(w,0);t.scale(-1,1);}
            case 3 -> {t.translate(w,h);t.rotate(Math.PI);}
            case 4 -> {t.translate(0,h);t.scale(1,-1);}
            case 5 -> {t.rotate(Math.PI/2);t.scale(1,-1);}
            case 6 -> {t.translate(h,0);t.rotate(Math.PI/2);}
            case 7 -> {t.scale(-1,1);t.translate(-h,0);t.translate(0,w);t.rotate(3*Math.PI/2);}
            case 8 -> {t.translate(0,w);t.rotate(3*Math.PI/2);}
            default -> {}
        }
        Graphics2D g=out.createGraphics();g.drawImage(in,t,null);g.dispose();in.flush();
        return out;
    }

    @Override public boolean seeThrough(BufferedImage picture) {
        if(!picture.getColorModel().hasAlpha())return false;
        int w=picture.getWidth(),h=picture.getHeight();int[] row=new int[w];
        for(int y=0;y<h;y++){picture.getRGB(0,y,w,1,row,0,w);for(int c:row)if((c>>>24)!=0xFF)return true;}
        return false;
    }

    /** On white where it could be seen through, as the private notes always made it: a JPEG has no see-through. */
    @Override public byte[] jpeg(BufferedImage picture) throws IOException {
        BufferedImage flat=picture;
        if(picture.getColorModel().hasAlpha()) {
            flat=new BufferedImage(picture.getWidth(),picture.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=flat.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,flat.getWidth(),flat.getHeight());g.drawImage(picture,0,0,null);g.dispose();
        }
        ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(MemoryCacheImageOutputStream to=new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(to);ImageWriteParam param=writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);param.setCompressionQuality(Shrink.QUALITY/100f);
            writer.write(null,new IIOImage(flat,null,null),param);
        } finally{writer.dispose();if(flat!=picture)flat.flush();}
        return out.toByteArray();
    }

    @Override public byte[] png(BufferedImage picture) throws IOException {
        ImageWriter writer=ImageIO.getImageWritersByFormatName("png").next();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(MemoryCacheImageOutputStream to=new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(to);writer.write(null,new IIOImage(picture,null,null),null);
        } finally{writer.dispose();}
        return out.toByteArray();
    }

    @Override public void close(BufferedImage picture){picture.flush();}
}
