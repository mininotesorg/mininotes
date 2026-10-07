package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Pictures shrunk and files packed on their way in, on the PC (the owner, 2026-10-06; docs/HOME.md, decision 112): sizes
 * before and after, nothing of the camera or the place left, a screenshot kept a PNG where that is the smaller and where
 * it is see-through, files packed and read back byte for byte (locked too, and in a backup), the switch off keeping
 * originals, Shrink the pictures already here (and what it leaves alone), a new version, and the screens pictured.
 * Made-up pictures and words only.
 */
public class DesktopShrinkTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private NoteStore store;private Context context;
    private java.util.function.Supplier<byte[]> wasKey;private Shrink.Painter<?> wasPainter;

    @Before public void open() throws Exception {
        context=new Context(temp.newFolder("pad"));store=new NoteStore(context);store.getWritableDatabase();
        wasKey=NoteStore.fileKey;wasPainter=Shrink.painter;
        NoteStore.fileKey=context::databaseKey;Shrink.painter=DesktopPictures.PAINTER;
    }
    @After public void close(){store.close();NoteStore.fileKey=wasKey;Shrink.painter=wasPainter;}

    @Test public void aBigPhotographIsMadeAboutTwoMegapixelsUprightWithNothingOfTheCameraOrThePlace() throws Exception {
        byte[] original=photo(3200,2400,6);
        assertEquals(6,Shrink.orientation(original));
        assertTrue(contains(original,"SyntheticCam"));assertTrue(contains(original,"48.8566N"));
        NoteStore.Held kept=add("IMG_2041.JPG","image/jpeg",original);
        byte[] now=read(kept);
        System.out.println("decision 112: photo 3200x2400 "+original.length+" bytes -> "+now.length+" bytes");
        assertTrue("smaller",now.length<original.length/2);assertEquals(now.length,kept.bytes);
        assertEquals("IMG_2041.JPG",kept.name);assertEquals("image/jpeg",kept.kind);
        BufferedImage back=ImageIO.read(new java.io.ByteArrayInputStream(now));
        // Turned as its camera said (6: a quarter turn), so it stands: 1440 across, 1920 down.
        assertEquals(1920,back.getHeight());assertEquals(1440,back.getWidth());
        for(String said:new String[]{"Exif","SyntheticCam","48.8566N"})assertFalse(said+" is gone",contains(now,said));
        assertEquals(1,Shrink.orientation(now));
    }

    @Test public void aSmallUprightPhotographLosesOnlyWhatItSaysAboutTheCamera() throws Exception {
        byte[] original=photo(1200,900,1);
        NoteStore.Held kept=add("small.jpg","image/jpeg",original);
        byte[] now=read(kept);
        System.out.println("decision 112: small photo "+original.length+" bytes -> "+now.length+" bytes (lossless)");
        assertFalse(contains(now,"SyntheticCam"));assertFalse(contains(now,"Exif"));
        // Byte for byte the same picture: only the EXIF part is out.
        assertArrayEquals(Shrink.bareJpeg(original),now);
        assertEquals(1200,ImageIO.read(new java.io.ByteArrayInputStream(now)).getWidth());
    }

    @Test public void aScreenshotStaysAPngWhereThatIsTheSmallerAndWhereItIsSeeThrough() throws Exception {
        // Flat colours and words: a PNG, its words chunk taken out.
        byte[] screen=withText(png(screenshot(1600,900,false)),"Author","Synthetic Person");
        NoteStore.Held kept=add("Screenshot.png","image/png",screen);
        byte[] now=read(kept);
        System.out.println("decision 112: screenshot PNG "+screen.length+" bytes -> "+now.length+" bytes");
        assertEquals("image/png",kept.kind);assertEquals("Screenshot.png",kept.name);
        assertFalse(contains(now,"Synthetic Person"));assertTrue(now.length<screen.length);
        // See-through and big, a picture in it: a PNG still, made 1920 wide, its see-through kept.
        byte[] clear=png(sticker(2600,1300));
        NoteStore.Held sticker=add("sticker.png","image/png",clear);
        BufferedImage back=ImageIO.read(new java.io.ByteArrayInputStream(read(sticker)));
        assertEquals("image/png",sticker.kind);assertEquals(1920,back.getWidth());assertTrue(back.getColorModel().hasAlpha());
        assertEquals(0,back.getRGB(5,5)>>>24);
        System.out.println("decision 112: see-through PNG 2600x1300 "+clear.length+" bytes -> "+sticker.bytes+" bytes");
        // A photograph saved as a PNG: a JPEG, the smaller, named for what it is.
        byte[] photoPng=png(noisy(2400,1600));
        NoteStore.Held made=add("holiday.png","image/png",photoPng);
        assertEquals("image/jpeg",made.kind);assertEquals("holiday.jpg",made.name);
        System.out.println("decision 112: photograph PNG 2400x1600 "+photoPng.length+" bytes -> "+made.bytes+" bytes, as JPEG");
    }

    @Test public void theSwitchOffKeepsOriginalsAndFilesAreStillPacked() throws Exception {
        store.setShrinkPictures(false);assertFalse(store.shrinkPictures());
        byte[] original=photo(3200,2400,6);
        NoteStore.Held kept=add("IMG_2042.jpg","image/jpeg",original);
        assertArrayEquals(original,read(kept));assertEquals(original.length,kept.bytes);
        byte[] words=lines(3000);
        NoteStore.Held list=add("list.txt","text/plain",words);
        assertTrue(Shrink.isPacked(Files.readAllBytes(store.fileFor(list.id).toPath())));
        assertArrayEquals(words,read(list));
        store.setShrinkPictures(true);assertTrue(store.shrinkPictures());
    }

    @Test public void otherFilesArePackedWhereThatSavesRoomAndComeBackByteForByte() throws Exception {
        byte[] words=lines(5000);
        NoteStore.Held list=add("minutes.txt","text/plain",words);
        Path at=store.fileFor(list.id).toPath();long lies=Files.size(at);
        System.out.println("decision 112: text "+words.length+" bytes kept in "+lies+" bytes");
        assertTrue(Shrink.isPacked(Files.readAllBytes(at)));assertTrue(lies<words.length*0.9);
        // Its row, every list and what travels: its own size and its own bytes, as before.
        assertEquals(words.length,list.bytes);assertEquals(words.length,store.file(list.id).bytes);
        assertArrayEquals(words,store.bytesOf(store.file(list.id)));assertArrayEquals(words,read(list));
        // Noise does not pack: kept as it came.
        byte[] noise=new byte[200_000];new Random(7).nextBytes(noise);
        NoteStore.Held random=add("noise.bin","application/octet-stream",noise);
        assertArrayEquals(noise,Files.readAllBytes(store.fileFor(random.id).toPath()));
        // A file that begins as a packed one does is packed all the same, so it is never taken for one.
        byte[] lookalike=Arrays.copyOf(Shrink.MAGIC,5000);new Random(8).nextBytes(lookalike);System.arraycopy(Shrink.MAGIC,0,lookalike,0,4);
        NoteStore.Held odd=add("odd.bin","application/octet-stream",lookalike);
        assertArrayEquals(lookalike,store.bytesOf(store.file(odd.id)));
        // A backup holds the files' own bytes, as an older build reads them.
        Path zip=temp.getRoot().toPath().resolve("backup.zip");DesktopBackup.write(store,zip);
        try(var z=new java.util.zip.ZipFile(zip.toFile())) {
            assertArrayEquals(words,z.getInputStream(z.getEntry(Attachment.entry(list.id))).readAllBytes());
            assertArrayEquals(lookalike,z.getInputStream(z.getEntry(Attachment.entry(odd.id))).readAllBytes());
        }
    }

    @Test public void lockedAPackedFileIsSealedAndStillReadsBack() throws Exception {
        Vault.Made made=Vault.make("password1".toCharArray(),20_000);
        store.getWritableDatabase().rekey(made.key);context.unlock(made.key);
        byte[] words=lines(4000);
        NoteStore.Held list=add("locked.txt","text/plain",words);
        byte[] lies=Files.readAllBytes(store.fileFor(list.id).toPath());
        assertTrue(Sealed.is(lies));assertFalse(new String(lies,StandardCharsets.ISO_8859_1).contains("Synthetic line"));
        assertArrayEquals(words,read(list));assertArrayEquals(words,store.bytesOf(store.file(list.id)));
        byte[] original=photo(3200,2400,1);
        NoteStore.Held photo=add("locked.jpg","image/jpeg",original);
        assertTrue(Sealed.is(Files.readAllBytes(store.fileFor(photo.id).toPath())));
        assertTrue(read(photo).length<original.length/2);
        // The lock taken off: plain on the disk, packed still, read the same.
        DesktopFiles.every(store,made.key,false);store.getWritableDatabase().rekey(null);context.unlock(null);
        assertTrue(Shrink.isPacked(Files.readAllBytes(store.fileFor(list.id).toPath())));assertArrayEquals(words,read(list));
    }

    @Test public void shrinkThePicturesAlreadyHereLeavesWhatWentAnywhere() throws Exception {
        store.setShrinkPictures(false);
        byte[] one=photo(3200,2400,1),two=photo(2800,2100,1),gone=photo(3000,2000,1);
        NoteStore.Held a=add("one.jpg","image/jpeg",one),b=add("two.jpg","image/jpeg",two),c=add("gone.jpg","image/jpeg",gone);
        add("notes.txt","text/plain",lines(100));
        store.published(c.id,"{\"synthetic\":true}");
        store.setShrinkPictures(true);
        List<int[]> seen=new ArrayList<>();
        NoteStore.Shrinking look=store.shrinkLook((done,of)->seen.add(new int[]{done,of}));
        assertEquals(2,look.count());assertEquals(1,look.left);assertEquals(one.length+two.length,look.before);assertTrue(look.after<look.before/2);
        assertArrayEquals(new int[]{3,3},seen.get(seen.size()-1));
        System.out.println("decision 112: already here "+look.before+" bytes -> "+look.after+" bytes; "+Shrink.asked(look.count(),look.before,look.after,look.left).replace('\n',' '));
        // Nothing is replaced until it is said yes to.
        assertArrayEquals(one,read(a));
        store.shrinkForget();assertArrayEquals(one,read(a));
        look=store.shrinkLook(null);
        long freed=store.shrinkDone(look);
        assertEquals(look.before-look.after,freed);
        assertTrue(read(store.file(a.id)).length<one.length*3/4);assertTrue(read(store.file(b.id)).length<two.length*3/4);
        assertEquals(1920,ImageIO.read(new java.io.ByteArrayInputStream(read(store.file(b.id)))).getWidth());
        assertEquals(read(store.file(a.id)).length,store.file(a.id).bytes);
        // The one that went up somewhere is as it was, byte for byte.
        assertArrayEquals(gone,read(store.file(c.id)));
        assertEquals(0,store.shrinkLook(null).count());store.shrinkForget();
        assertTrue(Shrink.nothing(1).contains("left as it is"));
    }

    @Test public void aNewVersionIsAddedAsAnyFileIs() throws Exception {
        NoteStore.Held kept=add("plan.png","image/png",png(screenshot(800,600,false)));
        byte[] newer=photo(3200,2400,1);
        long size;
        try(var in=new java.io.ByteArrayInputStream(newer)){size=store.replaceFile(kept.id,"image/jpeg",in);}
        catch(IllegalStateException readOnly){return;/* not this device's to change: nothing to show */}
        assertTrue(size<newer.length/2);assertEquals(size,store.file(kept.id).bytes);
        assertEquals("plan.jpg",store.file(kept.id).name);assertEquals("image/jpeg",store.file(kept.id).kind);
        assertEquals(size,read(store.file(kept.id)).length);
    }

    @Test public void theScreensSaySoAndAskFirst() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("window").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            // Two photographs kept before the switch was on, as a pad from an older build has them.
            pad.store.setShrinkPictures(false);
            String note=pad.store.latest().id;
            for(String name:new String[]{"beach.jpg","market.jpg"}) {
                byte[] bytes=photo(3200,2400,1);String id=UUID.randomUUID().toString();Files.write(pad.store.fileFor(id).toPath(),bytes);
                pad.store.keep(pad.store.settle(new NoteStore.Held(id,note,name,"image/jpeg",bytes.length,System.currentTimeMillis())));
            }
            pad.store.setShrinkPictures(true);
            JDialog settings=opened(()->DesktopSettings.open(pad),"Settings");
            SwingUtilities.invokeAndWait(()->{
                Component heading=showing(settings.getContentPane(),"Pictures and files");
                JScrollPane pane=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,heading);
                pane.getVerticalScrollBar().setValue(SwingUtilities.convertPoint(heading,0,0,pane.getViewport().getView()).y-8);
            });
            Thread.sleep(300);shoot(settings,"112a-settings-pictures");
            assertTrue(text(settings).contains(Shrink.SWITCH_UNDER));
            JDialog ask=opened(()->button(settings,Shrink.ALREADY).doClick(),Shrink.asking(2));
            assertTrue(text(ask).contains("now and would take"));
            shoot(ask,"112b-shrink-ask");
            SwingUtilities.invokeAndWait(()->button(ask,Shrink.yes(2)).doClick());
            await(()->pad.status.getText().startsWith("2 pictures shrunk"));
            settle(pad);shoot(pad.frame,"112c-shrunk");
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    // ---- made-up pictures and words ----------------------------------------------------------------------------------

    /** Kept as every way in keeps a file: copied in (sealed while locked), settled, then its row. */
    private NoteStore.Held add(String name,String kind,byte[] bytes) throws Exception {
        NoteStore.Note note=new NoteStore.Note();note.book=store.someBook();note.body="Synthetic holder";store.save(note);
        String id=UUID.randomUUID().toString();
        DesktopFiles.keep(context,bytes,store.fileFor(id).toPath());
        NoteStore.Held held=store.settle(new NoteStore.Held(id,note.id,name,kind,bytes.length,System.currentTimeMillis()));
        store.keep(held);return held;
    }
    private byte[] read(NoteStore.Held held) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();DesktopFiles.copyOut(context,store.fileFor(held.id).toPath(),out);return out.toByteArray();
    }
    private static byte[] lines(int n) {
        StringBuilder all=new StringBuilder();for(int i=0;i<n;i++)all.append("Synthetic line ").append(i).append(": bread, tea, postcards\n");
        return all.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static BufferedImage noisy(int w,int h) {
        BufferedImage picture=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Random r=new Random(w*31L+h);
        int[] all=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int g=(x*255/w+r.nextInt(40))&0xff,b=(y*255/h+r.nextInt(40))&0xff;all[y*w+x]=(r.nextInt(60)+120)<<16|g<<8|b;}
        picture.setRGB(0,0,w,h,all,0,w);return picture;
    }
    /** A photograph cut out: noise in the middle, nothing around it. */
    private static BufferedImage sticker(int w,int h) {
        BufferedImage picture=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);BufferedImage inside=noisy(w*3/4,h*3/4);
        Graphics2D g=picture.createGraphics();g.drawImage(inside,w/8,h/8,null);g.dispose();return picture;
    }
    private static BufferedImage screenshot(int w,int h,boolean clear) {
        BufferedImage picture=new BufferedImage(w,h,clear?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        Graphics2D g=picture.createGraphics();
        if(!clear){g.setColor(new Color(250,249,244));g.fillRect(0,0,w,h);}
        g.setColor(new Color(48,99,72));g.fillRoundRect(w/8,h/8,w*3/4,h*3/4,40,40);
        g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,28));
        for(int i=0;i<12;i++)g.drawString("Synthetic screen line "+i,w/8+30,h/8+50+i*36);
        g.dispose();return picture;
    }
    private static byte[] png(BufferedImage picture) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(picture,"png",out);return out.toByteArray();}

    /** A made-up photograph: noise, so it is as big as a camera's, with an EXIF part naming a camera, a place and a turn. */
    private static byte[] photo(int w,int h,int turn) throws Exception {
        ByteArrayOutputStream jpeg=new ByteArrayOutputStream();ImageIO.write(noisy(w,h),"jpeg",jpeg);
        byte[] plain=jpeg.toByteArray();
        ByteArrayOutputStream tiff=new ByteArrayOutputStream();
        byte[] make="SyntheticCam\0".getBytes(StandardCharsets.ISO_8859_1),place="GPS 48.8566N 2.3522E\0".getBytes(StandardCharsets.ISO_8859_1);
        tiff.write(new byte[]{'I','I',42,0,8,0,0,0});
        tiff.write(new byte[]{3,0});
        int data=8+2+3*12+4;
        entry(tiff,0x0112,3,1,turn);
        entry(tiff,0x010F,2,make.length,data);
        entry(tiff,0x010E,2,place.length,data+make.length);
        tiff.write(new byte[]{0,0,0,0});tiff.write(make);tiff.write(place);
        byte[] body=tiff.toByteArray();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(plain,0,2);
        int length=2+6+body.length;
        out.write(0xFF);out.write(0xE1);out.write(length>>8);out.write(length&0xff);out.write("Exif\0\0".getBytes(StandardCharsets.ISO_8859_1));out.write(body);
        out.write(plain,2,plain.length-2);
        return out.toByteArray();
    }
    private static void entry(ByteArrayOutputStream out,int tag,int type,int count,int value) {
        out.write(tag&0xff);out.write(tag>>8);out.write(type);out.write(0);
        out.write(count&0xff);out.write((count>>8)&0xff);out.write(0);out.write(0);
        out.write(value&0xff);out.write((value>>8)&0xff);out.write((value>>16)&0xff);out.write(0);
    }
    /** A PNG with a tEXt chunk added before its end. */
    private static byte[] withText(byte[] png,String key,String value) throws Exception {
        byte[] text=(key+"\0"+value).getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream chunk=new ByteArrayOutputStream();
        chunk.write(new byte[]{(byte)(text.length>>24),(byte)(text.length>>16),(byte)(text.length>>8),(byte)text.length});
        byte[] typed=new byte[4+text.length];System.arraycopy("tEXt".getBytes(StandardCharsets.ISO_8859_1),0,typed,0,4);System.arraycopy(text,0,typed,4,text.length);
        chunk.write(typed);java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(typed);long c=crc.getValue();
        chunk.write(new byte[]{(byte)(c>>24),(byte)(c>>16),(byte)(c>>8),(byte)c});
        int end=png.length-12;
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(png,0,end);out.write(chunk.toByteArray());out.write(png,end,12);
        return out.toByteArray();
    }
    private static boolean contains(byte[] in,String what){return new String(in,StandardCharsets.ISO_8859_1).contains(what);}

    // ---- the window --------------------------------------------------------------------------------------------------

    private static JDialog opened(Runnable open,String called) throws Exception {
        Set<Window> before=new HashSet<>(Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(open);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while(System.nanoTime()<until) {
            JDialog[] found={null};
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&!before.contains(w)&&called.equals(d.getTitle()))found[0]=d;});
            if(found[0]!=null){Thread.sleep(400);return found[0];}
            Thread.sleep(50);
        }
        throw new AssertionError(called+" did not open");
    }
    private static Component showing(Container in,String words) {
        for(Component c:in.getComponents()) {
            if(c instanceof javax.swing.text.JTextComponent t&&words.equals(t.getText()))return c;
            if(c instanceof JLabel l&&words.equals(l.getText()))return c;
            if(c instanceof Container k){Component found=showing(k,words);if(found!=null)return found;}
        }
        return null;
    }
    private static String text(Container in) {
        StringBuilder all=new StringBuilder();
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t)all.append(t.getText()).append('\n');if(c instanceof JLabel l)all.append(l.getText()).append('\n');if(c instanceof Container k)all.append(text(k));}
        return all.toString();
    }
    private static AbstractButton button(Container in,String words) {
        for(Component c:in.getComponents()) {
            if(c instanceof AbstractButton b&&words.equals(b.getText()))return b;
            if(c instanceof Container k){try{return button(k,words);}catch(AssertionError none){/* elsewhere */}}
        }
        throw new AssertionError("no button "+words);
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
            ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
