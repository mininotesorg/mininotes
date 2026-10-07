package org.mininotes.android;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/**
 * The rules for pictures made smaller and files packed (docs/HOME.md, decision 112), with no picture drawn: a painter that
 * only says what it was asked stands in for each app's. The drawing itself is pictured and measured in DesktopShrinkTest.
 */
public class ShrinkTest {
    /** Says what it is told to, and keeps what it was asked. */
    static final class Told implements Shrink.Painter<String> {
        int w,h,turn=1;boolean clear;int jpeg,png;final List<String> asked=new ArrayList<>();
        Told(int w,int h){this.w=w;this.h=h;}
        @Override public int[] size(byte[] raw){return w>0?new int[]{w,h}:null;}
        @Override public int turn(byte[] raw){return turn;}
        @Override public String open(byte[] raw,int turn){asked.add("open "+turn);return "picture";}
        @Override public boolean seeThrough(String p){return clear;}
        @Override public byte[] jpeg(String p){asked.add("jpeg");return new byte[jpeg];}
        @Override public byte[] png(String p){asked.add("png");return png(this.png);}
        @Override public void close(String p){asked.add("close");}
        static byte[] png(int n){byte[] b=new byte[Math.max(12,n)];b[0]=(byte)0x89;b[1]='P';b[2]='N';b[3]='G';return b;}
    }

    /** A JPEG's parts with no picture in them: its head, a JFIF part, an EXIF part naming a camera and a place, the rest. */
    static byte[] jpeg(int picture) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(new byte[]{(byte)0xFF,(byte)0xD8});
        part(out,0xE0,"JFIF\0\1\1\0\0\1\0\1\0\0".getBytes(StandardCharsets.ISO_8859_1));
        part(out,0xE1,"Exif\0\0SyntheticCam GPS 48.8566N".getBytes(StandardCharsets.ISO_8859_1));
        part(out,0xE2,"ICC_PROFILE\0colours".getBytes(StandardCharsets.ISO_8859_1));
        part(out,0xFE,"A comment".getBytes(StandardCharsets.ISO_8859_1));
        part(out,0xDB,new byte[64]);
        out.write(new byte[]{(byte)0xFF,(byte)0xDA,0,2});
        byte[] data=new byte[picture];new Random(1).nextBytes(data);for(int i=0;i<data.length;i++)if(data[i]==(byte)0xFF)data[i]=0;
        out.write(data);out.write(new byte[]{(byte)0xFF,(byte)0xD9});
        return out.toByteArray();
    }
    private static void part(ByteArrayOutputStream out,int marker,byte[] body) throws IOException {
        out.write(0xFF);out.write(marker);int n=body.length+2;out.write(n>>8);out.write(n&0xff);out.write(body);
    }
    private static boolean has(byte[] in,String what){return new String(in,StandardCharsets.ISO_8859_1).contains(what);}

    @Test public void aJpegLosesWhatItSaysAboutTheCameraAndThePlaceAndNothingElse() throws IOException {
        byte[] whole=jpeg(4000),bare=Shrink.bareJpeg(whole);
        assertNotNull(bare);
        assertTrue(has(whole,"SyntheticCam"));assertFalse(has(bare,"SyntheticCam"));assertFalse(has(bare,"48.8566N"));assertFalse(has(bare,"A comment"));
        assertTrue("its JFIF head stays",has(bare,"JFIF"));assertTrue("its colours stay",has(bare,"ICC_PROFILE"));
        assertArrayEquals("the picture itself, byte for byte",Arrays.copyOfRange(whole,whole.length-4004,whole.length),Arrays.copyOfRange(bare,bare.length-4004,bare.length));
        assertNull(Shrink.bareJpeg("not a picture".getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void aPngLosesItsWordsAndTimesAndNothingElse() throws IOException {
        ByteArrayOutputStream png=new ByteArrayOutputStream();
        png.write(new byte[]{(byte)0x89,'P','N','G',13,10,26,10});
        chunk(png,"IHDR",new byte[13]);chunk(png,"tEXt","Author\0Synthetic Person".getBytes(StandardCharsets.ISO_8859_1));
        chunk(png,"eXIf","GPS 48.8566N".getBytes(StandardCharsets.ISO_8859_1));chunk(png,"IDAT",new byte[300]);chunk(png,"IEND",new byte[0]);
        byte[] whole=png.toByteArray(),bare=Shrink.barePng(whole);
        assertFalse(has(bare,"Synthetic Person"));assertFalse(has(bare,"48.8566N"));assertTrue(has(bare,"IHDR"));assertTrue(has(bare,"IEND"));
        assertEquals(whole.length-(12+23)-(12+12),bare.length);
    }
    private static void chunk(ByteArrayOutputStream out,String type,byte[] body) throws IOException {
        int n=body.length;out.write(new byte[]{(byte)(n>>24),(byte)(n>>16),(byte)(n>>8),(byte)n});out.write(type.getBytes(StandardCharsets.ISO_8859_1));out.write(body);out.write(new byte[4]);
    }

    @Test public void theRule() throws IOException {
        byte[] small=jpeg(4000);
        // Small and upright: never drawn, only its camera's words taken out.
        Told painter=new Told(1200,900);
        Shrink.Made made=Shrink.smaller(small,painter);
        assertTrue(painter.asked.isEmpty());assertArrayEquals(Shrink.bareJpeg(small),made.bytes);assertEquals("image/jpeg",made.kind);
        // Big: drawn again, a JPEG.
        painter=new Told(4000,3000);painter.jpeg=1000;
        made=Shrink.smaller(small,painter);
        assertEquals(Arrays.asList("open 1","jpeg","close"),painter.asked);assertEquals(1000,made.bytes.length);
        // Small but turned: drawn again upright, even where that is bigger, since its turn goes with its camera's words.
        painter=new Told(1200,900);painter.turn=6;painter.jpeg=9000;
        made=Shrink.smaller(small,painter);
        assertEquals(Arrays.asList("open 6","jpeg","close"),painter.asked);assertEquals(9000,made.bytes.length);
        // Drawn again but bigger, upright: its own bytes, bare.
        painter=new Told(4000,3000);painter.jpeg=90_000;
        assertArrayEquals(Shrink.bareJpeg(small),Shrink.smaller(small,painter).bytes);
        // A screenshot: a PNG where that is the smaller, else a JPEG; a PNG where it is see-through.
        byte[] screen=Told.png(50_000);
        painter=new Told(2400,1080);painter.png=20_000;painter.jpeg=30_000;
        made=Shrink.smaller(screen,painter);assertEquals("image/png",made.kind);assertEquals(20_000,made.bytes.length);
        painter=new Told(2400,1080);painter.png=40_000;painter.jpeg=10_000;
        made=Shrink.smaller(screen,painter);assertEquals("image/jpeg",made.kind);assertEquals("Screenshot.jpg",made.name("Screenshot.png"));
        painter=new Told(2400,1080);painter.png=40_000;painter.jpeg=10_000;painter.clear=true;
        made=Shrink.smaller(screen,painter);assertEquals("image/png",made.kind);assertFalse(painter.asked.contains("jpeg"));
        // Nothing smaller to be had: kept as it is.
        painter=new Told(2400,1080);painter.png=60_000;painter.jpeg=70_000;
        assertNull(Shrink.smaller(screen,painter));
        // What this device cannot read, a GIF, and what is not a picture: kept as they are.
        assertNull(Shrink.smaller(small,new Told(0,0)));
        byte[] gif="GIF89a......".getBytes(StandardCharsets.ISO_8859_1);
        painter=new Told(4000,3000);assertNull(Shrink.smaller(gif,painter));assertTrue(painter.asked.isEmpty());
        assertNull(Shrink.smaller("plain words, not a picture".getBytes(StandardCharsets.UTF_8),new Told(4000,3000)));
        // The private notes' way, as it was: always drawn again, a JPEG.
        painter=new Told(800,600);painter.jpeg=5;
        assertEquals(5,Shrink.remade(small,painter).length);assertEquals(Arrays.asList("open 1","jpeg","close"),painter.asked);
    }

    @Test public void namesKindsAndOrientation() {
        assertEquals("IMG_2041.jpg",new Shrink.Made(new byte[0],"image/jpeg","jpg").name("IMG_2041.HEIC"));
        assertEquals("scan.jpeg",new Shrink.Made(new byte[0],"image/jpeg","jpg").name("scan.jpeg"));
        assertEquals("Picture.png",new Shrink.Made(new byte[0],"image/png","png").name("Picture"));
        assertTrue(Shrink.mayBePicture("image/heic",""));assertTrue(Shrink.mayBePicture("","photo.JPG"));assertFalse(Shrink.mayBePicture("text/plain","list.txt"));
        byte[] heic=new byte[16];System.arraycopy("ftypheic".getBytes(StandardCharsets.ISO_8859_1),0,heic,4,8);
        assertEquals("heic",Shrink.format(heic));
        assertEquals(1,Shrink.orientation("not a jpeg at all".getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void filesArePackedWhereThatSavesATenthAndComeBackByteForByte() throws IOException {
        StringBuilder words=new StringBuilder();for(int i=0;i<20_000;i++)words.append("Synthetic line ").append(i).append('\n');
        byte[] plain=words.toString().getBytes(StandardCharsets.UTF_8),packed=Shrink.packed(plain);
        assertNotNull(packed);assertTrue(Shrink.isPacked(packed));assertTrue(packed.length<plain.length*0.9);
        assertArrayEquals(plain,Shrink.plain(packed));
        // Through the stream, in pieces of every size, as a file is copied out.
        for(int piece:new int[]{1,3,4,5,4096,1<<20}) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();Shrink.Unpacking to=Shrink.unpacking(out);
            for(int at=0;at<packed.length;at+=piece)to.write(packed,at,Math.min(piece,packed.length-at));
            to.finish();assertArrayEquals(plain,out.toByteArray());
        }
        // Noise: kept as it is, and passed through as it is.
        byte[] noise=new byte[600_000];new Random(5).nextBytes(noise);
        assertNull(Shrink.packed(noise));assertSame(noise,Shrink.plain(noise));
        ByteArrayOutputStream out=new ByteArrayOutputStream();Shrink.Unpacking to=Shrink.unpacking(out);to.write(new byte[]{1,2});to.finish();
        assertArrayEquals(new byte[]{1,2},out.toByteArray());
        // A file that begins as a packed one does: packed all the same, so it is never taken for one.
        byte[] lookalike=new byte[300];new Random(6).nextBytes(lookalike);System.arraycopy(Shrink.MAGIC,0,lookalike,0,Shrink.MAGIC.length);
        assertArrayEquals(lookalike,Shrink.plain(Shrink.packed(lookalike)));
        // Cut short: said, never handed on in part.
        byte[] cut=Arrays.copyOf(packed,packed.length/2);
        assertThrows(IOException.class,()->Shrink.plain(cut));
    }

    @Test public void theWordsSayHowMuchAndWhatIsLeft() {
        assertEquals("Shrink 3 pictures?",Shrink.asking(3));
        String said=Shrink.asked(3,30L*1024*1024,3L*1024*1024,2);
        assertTrue(said,said.startsWith("They take 30 MB now and would take "+Attachment.size(3L*1024*1024)+": about 27 MB freed."));
        assertTrue(said.contains("2 pictures that went to somebody or came from somebody are left as they are"));
        for(String one:new String[]{Shrink.SWITCH_UNDER,said,Shrink.nothing(1),Shrink.shrunk(2,5_000_000)})
            assertFalse("no long dash",one.contains("—")||one.contains(" - "));
    }
}
