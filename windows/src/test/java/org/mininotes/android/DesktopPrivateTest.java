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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Private notes and folders on the PC (the owner, 2026-10-06; docs/HOME.md, decision 111), proved where a leak would be
 * the failure: two installs, one never used and one filled with words and fifty photographs, after the same starts and
 * the same time, leave the same files at the same sizes, written by the same batches, with none of the words, the title,
 * the password, the photographs' names and camera, or the codes anywhere in any byte of them; a code typed in a shared
 * note reaches nobody; a password nothing answers to changes nothing; a backup carries it to another PC.
 */
public class DesktopPrivateTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String PASSWORD="correct horse battery staple 1729",TITLE="Diary of private things",
        WORDS="the treasure is under the old oak",PHOTO="IMG_SECRET_",CAMERA="CAMERA-SECRET-MODEL";

    // ---- the proof: a user and a non-user, side by side ------------------------------------------------------------------

    @Test public void aUserAndANonUserLeaveTheSameFilesAfterTheSameStartsAndTheSameTime() throws Exception {
        String was=System.getProperty("mininotes.pages");System.setProperty("mininotes.pages",String.valueOf(Slots.COUNT));
        int cachesBefore=imageioCaches();
        try {
            Path never=temp.newFolder("never").toPath(),used=temp.newFolder("used").toPath();
            String[] groceries=new String[2];
            // The first start: the same note written in both; in one, three private spaces and fifty photographs (decision 116).
            Desktop a=open(never),b=open(used);
            try {
                groceries[0]=everyday(a);groceries[1]=everyday(b);
                // The code, typed into the shared-looking everyday note, and the password into its field. A new password: it
                // is asked twice and a new, empty space is made.
                type(b,b.page,"privatespace//:");
                await(()->onEdt(()->b.privately.asking!=null&&b.privately.asking.isEditable()));
                assertFalse("taken out at once",onEdt(()->b.page.getText()).contains("privatespace"));
                SwingUtilities.invokeAndWait(()->{b.privately.asking.setText(PASSWORD);b.privately.asking.postActionEvent();});
                answer(b,PASSWORD);
                await(()->onEdt(()->b.privately.showing()));
                List<byte[]> photos=new ArrayList<>();for(int i=0;i<50;i++)photos.add(photo(i));
                List<byte[]> small=new ArrayList<>();for(byte[] one:photos)small.add(DesktopPrivate.shrink(one));
                for(byte[] one:small){
                    assertFalse("nothing of the camera kept",contains(one,CAMERA));
                    BufferedImage back=DesktopPrivate.read(one,0);
                    assertTrue("about two megapixels",Math.max(back.getWidth(),back.getHeight())<=DesktopPrivate.LONGEST);
                    assertTrue("turned the way up its camera said",back.getHeight()>back.getWidth());
                }
                // The first space opens as an empty folder (decision 116): a note is made in it, with the title, words and fifty photographs.
                SwingUtilities.invokeAndWait(()->{
                    PrivateSpace p=b.privately.shown();assertTrue("a brand-new space is empty",p.in("").isEmpty());
                    PrivateSpace.Thing note=p.newNote("",System.currentTimeMillis());
                    p.text(note.id,TITLE,WORDS,System.currentTimeMillis());
                    for(int i=0;i<small.size();i++)p.addFile(note.id,PHOTO+i+".jpg","image/jpeg",small.get(i),true,System.currentTimeMillis());
                    try{p.write();}catch(Exception e){throw new RuntimeException(e);}
                });
                SwingUtilities.invokeAndWait(()->b.privately.close());
                // Two more spaces, each by its own password, each with a little in it.
                space(b,"second phrase","Second space note");
                space(b,"third phrase","Third space note");
                // Written only by batches, as many in both: the user's go in theirs, the other's are written again as they were.
                Slots sa=a.privately.slots(),sb=b.privately.slots();
                a.privately.holdRhythm();b.privately.holdRhythm();
                int turns=0;
                while(sb.anyWaiting()&&turns<400){a.privately.batchNow();b.privately.batchNow();turns++;}
                assertFalse("all written",sb.anyWaiting());
                long bytes=0;for(byte[] one:small)bytes+=one.length;
                System.out.println("three spaces and 50 photographs made small: "+bytes+" bytes, written in "+turns+" batches");
                assertTrue("photographs took more than one batch",turns>1);
                for(int i=0;i<3;i++){a.privately.batchNow();b.privately.batchNow();}
                assertEquals(sa.batches,sb.batches);assertEquals(sa.written,sb.written);
            } finally{close(a);close(b);}
            // The second start: each space opens with its own password, whole.
            a=open(never);Desktop b2=open(used);
            try {
                a.privately.holdRhythm();b2.privately.holdRhythm();
                open(b2,"private//:");
                PrivateSpace back=onEdt(()->b2.privately.shown());
                PrivateSpace.Thing note=back.in("").get(0);
                assertEquals(TITLE,note.title);assertEquals(WORDS,note.body);assertEquals(50,back.filesOf(note.id).size());
                SwingUtilities.invokeAndWait(()->b2.privately.close());
                open(b2,"private//:","second phrase");
                assertEquals("Second space note",onEdt(()->b2.privately.shown().in("").get(0).body));
                SwingUtilities.invokeAndWait(()->b2.privately.close());
                open(b2,"private//:","third phrase");
                assertEquals("Third space note",onEdt(()->b2.privately.shown().in("").get(0).body));
                SwingUtilities.invokeAndWait(()->b2.privately.close());
                for(int i=0;i<2;i++){a.privately.batchNow();b2.privately.batchNow();}
                assertEquals(a.privately.slots().batches,b2.privately.slots().batches);
                assertEquals(a.privately.slots().written,b2.privately.slots().written);
            } finally{close(a);close(b2);}
            // Side by side: the same files, the same sizes, the same note; none of it anywhere in any byte.
            Map<String,Long> one=files(never),two=files(used);
            assertEquals(one.keySet(),two.keySet());
            for(String name:one.keySet())assertEquals(name,one.get(name),two.get(name));
            assertEquals((long)Slots.COUNT*Slots.SLOT,(long)two.get(Slots.NAME));
            String[] words={PASSWORD,"second phrase","third phrase",TITLE,WORDS,"Second space note","treasure",PHOTO,CAMERA,"privatespace","privatenote","privatefolder","private//:","Diary"};
            for(Path root:new Path[]{never,used})for(Path file:all(root)){String found=find(file,words);assertNull(found+" in "+root.relativize(file),found);}
            // The private file is noise from end to end in both: it does not compress.
            for(Path root:new Path[]{never,used})assertTrue(root+" compresses",packed(root.resolve(Slots.NAME))>Files.size(root.resolve(Slots.NAME))*0.999);
            assertEquals("no picture went through a cache file",cachesBefore,imageioCaches());
        } finally{if(was==null)System.clearProperty("mininotes.pages");else System.setProperty("mininotes.pages",was);}
    }

    /** A new space made from outside (privatespace//:, the password twice), a note written in it, and closed. */
    private void space(Desktop pad,String password,String words) throws Exception {
        fromOutside(pad,PrivateCode.Kind.SPACE,password);
        SwingUtilities.invokeAndWait(()->{
            PrivateSpace p=pad.privately.shown();
            PrivateSpace.Thing note=p.newNote("",System.currentTimeMillis());
            p.text(note.id,"",words,System.currentTimeMillis());
            try{p.write();}catch(Exception e){throw new RuntimeException(e);}
        });
        SwingUtilities.invokeAndWait(()->pad.privately.close());
    }

    // ---- a code typed in a shared note --------------------------------------------------------------------------------

    private static final String PHONE="MxPrivatePhoneFixture@127.0.0.1:9431";

    @Test public void aCodeTypedInASharedNoteReachesNobody() throws Exception {
        Hashes();
        Path folder=temp.newFolder("pc").toPath();
        Desktop pad=open(folder);
        Context phoneContext=new Context(temp.newFolder("phone"));NoteStore phone=new NoteStore(phoneContext);phone.getWritableDatabase();Keys phoneKeys=new Keys(phoneContext);
        try {
            phone.mySigningKey=Base64.getEncoder().encodeToString(phoneKeys.signing().getPublic().getEncoded());
            pad.store.mySigningKey=Base64.getEncoder().encodeToString(pad.keys.signing().getPublic().getEncoded());
            pad.store.pairedWith(PHONE,"Test phone",false,phoneKeys.agreement().getPublic().getEncoded(),phoneKeys.signing().getPublic().getEncoded());
            String pcAt="MxPrivatePcFixture@127.0.0.1:9432";
            phone.pairedWith(pcAt,"Test PC",false,pad.keys.agreement().getPublic().getEncoded(),pad.keys.signing().getPublic().getEncoded());
            NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Plan";n.body="Meet at noon";n.revision=1;pad.store.save(n);
            pad.store.setLevel(Sharing.Scope.PAGE,n.id,PHONE,Sharing.Level.WRITE,null);
            phone.addShare(new Sharing.Rule(Sharing.Scope.PAGE,n.id,pcAt,true));
            SwingUtilities.invokeAndWait(()->pad.open(n.id));settle(pad);
            SwingUtilities.invokeAndWait(()->pad.page.setCaretPosition(pad.page.getDocument().getLength()));
            // Typed slowly, a pause at every key: the writing down waits while the word at the caret could become a code.
            for(char c:" privatespace//".toCharArray()){type(pad,pad.page,String.valueOf(c));Thread.sleep(420);}
            settle(pad);
            assertFalse("half a code is never written down",pad.store.get(n.id).body.contains("privatespace"));
            type(pad,pad.page,":");
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            assertEquals("Meet at noon ",onEdt(()->pad.page.getText()));
            assertFalse("nothing to undo back into it",onEdt(()->((javax.swing.undo.UndoManager)pad.page.getClientProperty("undo")).canUndo()));
            SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText(PASSWORD);pad.privately.asking.postActionEvent();});
            answer(pad,PASSWORD);
            await(()->onEdt(()->pad.privately.showing()));
            SwingUtilities.invokeAndWait(()->pad.privately.close());
            // And in the title, and in the search: the same.
            type(pad,pad.title,"private//:");
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            assertEquals("Plan",onEdt(()->pad.title.getText()));
            SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText("nothing answers to this");pad.privately.asking.postActionEvent();});
            SwingUtilities.invokeAndWait(()->pad.home.search.requestFocusInWindow());
            type(pad,pad.home.search,"privatespace//:");
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            assertEquals("",onEdt(()->pad.home.search.getText()));
            SwingUtilities.invokeAndWait(()->pad.privately.asking.postActionEvent());
            SwingUtilities.invokeAndWait(()->pad.save(null));settle(pad);pad.privately.settle();
            // The note, its versions, what is owed, and the note as it goes to the phone: none of it.
            NoteStore.Note kept=pad.store.get(n.id);
            assertEquals("Meet at noon ",kept.body);assertEquals("Plan",kept.title);
            for(NoteStore.Version v:pad.store.versions(n.id))assertFalse(v.body.contains("privatespace")||v.body.contains("privatenote")||v.body.contains(PASSWORD));
            Parcel.Sent going=new Parcel.Sent("","","","",kept.title,kept.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",n.id,false,-1L,false,
                List.of(),1L,List.of(),pad.store.steps(pad.store.above(n.id)),"",null);
            byte[] plain=Parcel.wrap(going,Envelope.MAX_TEXT);
            for(String word:new String[]{"privatespace","privatenote","privatefolder","private//:",PASSWORD})assertFalse(word,contains(plain,word));
            byte[] sealed=Envelope.seal(page(n.id),kept.revision,1,plain,pad.keys.signing(),phoneKeys.agreement().getPublic());
            Post.arrived(phoneContext,phone,phoneKeys,sealed);
            NoteStore.Note there=phone.get(n.id);
            assertNotNull("it reached the phone",there);assertEquals("Meet at noon ",there.body);
            for(NoteStore.Version v:phone.versions(n.id))assertFalse(v.body.contains("privatespace")||v.body.contains("privatenote"));
            for(Outbox.Wait w:pad.store.owed(NoteStore.Branch.Kind.PAGE,n.id))assertEquals(PHONE,w.address);
        } finally{close(pad);phone.close();}
        for(Path file:all(folder)){String found=find(file,new String[]{"privatespace","privatenote","privatefolder","private//:",PASSWORD,"nothing answers"});assertNull(found+" in "+folder.relativize(file),found);}
    }

    // ---- nothing answers --------------------------------------------------------------------------------------------

    @Test public void aPasswordNothingAnswersToShowsNothingAndChangesNothing() throws Exception {
        Path folder=temp.newFolder("pad").toPath();
        Desktop pad=open(folder);
        try {
            pad.privately.holdRhythm();
            Path file=folder.resolve(Slots.NAME);
            Map<String,Long> before=files(folder);byte[] was=Files.readAllBytes(file);
            pad.privately.go(PrivateCode.Kind.OPEN,"no space has this".toCharArray());pad.privately.settle();
            SwingUtilities.invokeAndWait(()->{});
            assertFalse(onEdt(()->pad.privately.showing()));
            assertArrayEquals("not a byte changed",was,Files.readAllBytes(file));
            assertEquals(before,files(folder));
        } finally{close(pad);}
    }

    // ---- made from outside: the password twice ----------------------------------------------------------------------

    /** A space made from outside asks for its password twice: two that differ make nothing and write nothing. */
    @Test public void madeFromOutsideItAsksTwiceAndTwoThatDifferWriteNothing() throws Exception {
        Path folder=temp.newFolder("twice").toPath();
        Desktop pad=open(folder);
        try {
            pad.privately.holdRhythm();
            byte[] was=Files.readAllBytes(folder.resolve(Slots.NAME));Map<String,Long> before=files(folder);
            SwingUtilities.invokeAndWait(()->pad.home.search.requestFocusInWindow());
            type(pad,pad.home.search,"privatespace//:");
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText("a long phrase");pad.privately.asking.postActionEvent();});
            answer(pad,"a long phrase with a typo");
            pad.privately.settle();SwingUtilities.invokeAndWait(()->{});pad.privately.settle();
            assertFalse(onEdt(()->pad.privately.showing()));
            assertFalse("nothing made, nothing open",pad.privately.slots().anyOpen());
            assertArrayEquals("not a byte written",was,Files.readAllBytes(folder.resolve(Slots.NAME)));
            assertEquals(before,files(folder));
            // And the same twice makes it.
            fromOutside(pad,PrivateCode.Kind.SPACE,"a long phrase");
            assertTrue(pad.privately.slots().anyOpen());
        } finally{close(pad);}
    }

    // ---- a backup carries it -------------------------------------------------------------------------------------------

    @Test public void aBackupCarriesItToAnotherPcWhereItOpensWithTheSamePassword() throws Exception {
        Path here=temp.newFolder("here").toPath(),there=temp.newFolder("there").toPath();
        Path zip=temp.getRoot().toPath().resolve("backup.zip");
        Desktop a=open(here);
        try {
            a.privately.holdRhythm();
            fromOutside(a,PrivateCode.Kind.SPACE,PASSWORD);
            SwingUtilities.invokeAndWait(()->{PrivateSpace p=a.privately.shown();PrivateSpace.Thing note=p.newNote("",1);p.text(note.id,"",WORDS,1);try{p.write();}catch(Exception e){throw new RuntimeException(e);}a.privately.close();});
            while(a.privately.slots().anyWaiting())a.privately.batchNow();
            DesktopBackup.write(a.store,zip,null,null,a.privately.slots());
            assertFalse(contains(Files.readAllBytes(zip),WORDS));
        } finally{close(a);}
        Desktop b=open(there);
        try {
            b.privately.holdRhythm();
            DesktopBackup.add(b.store,zip,null,true,b.privately.slots());
            assertEquals((long)Integer.getInteger("mininotes.pages",Slots.COUNT)*Slots.SLOT,Files.size(there.resolve(Slots.NAME)));
            open(b,"private//:");
            assertEquals(WORDS,onEdt(()->b.privately.shown().in("").get(0).body));
        } finally{close(b);}
    }

    // ---- pictures of it, to be looked at ------------------------------------------------------------------------------

    @Test public void thePrivateScreenAsItIsSeen() throws Exception {
        Path folder=temp.newFolder("shots").toPath();
        Desktop pad=open(folder);
        Path out=Paths.get("build","private-shots");Files.createDirectories(out);
        try {
            pad.privately.holdRhythm();
            fromOutside(pad,PrivateCode.Kind.SPACE,"shots");
            SwingUtilities.invokeAndWait(()->{
                PrivateSpace p=pad.privately.shown();
                // A brand-new space is empty (decision 116): the folder, its note and the rest are made in it.
                PrivateSpace.Thing box=p.newFolder("","Taxes",1);p.colour(box.id,6);
                PrivateSpace.Thing inside=p.newNote(box.id,1);p.text(inside.id,"","Account numbers\nSynthetic 12 34",1);
                PrivateSpace.Thing one=p.newNote("",2);p.text(one.id,"","Gift ideas\nA synthetic list",2);p.colour(one.id,2);
                PrivateSpace.Thing two=p.newNote("",3);p.text(two.id,"","Doctor",3);
                p.newFolder("","Letters",4);
                try{p.write();}catch(Exception e){throw new RuntimeException(e);}
            });
            SwingUtilities.invokeAndWait(()->pad.privately.close());
            open(pad,"private//:","shots");
            settle(pad);shot(pad,out.resolve("private-home.png"));
            SwingUtilities.invokeAndWait(()->{
                PrivateSpace p=pad.privately.shown();
                for(PrivateSpace.Thing t:p.in(""))if(!t.folder&&t.name().startsWith("Gift")){
                    try{p.addFile(t.id,"receipt.jpg","image/jpeg",DesktopPrivate.shrink(photo(1)),true,5);p.write();}catch(Exception e){throw new RuntimeException(e);}
                    java.lang.reflect.Method m;try{m=DesktopPrivate.class.getDeclaredMethod("openThing",PrivateSpace.Thing.class);m.setAccessible(true);m.invoke(pad.privately,t);}catch(Exception e){throw new RuntimeException(e);}
                }
            });
            settle(pad);shot(pad,out.resolve("private-note.png"));
            // The ⋮ menu, grouped and drawn like the app's own menu, with Delete this private space (decision 117): pictured.
            BufferedImage[] menu={null};
            SwingUtilities.invokeAndWait(()->{try{
                java.lang.reflect.Method mm=DesktopPrivate.class.getDeclaredMethod("moreMenu");mm.setAccessible(true);
                JPopupMenu m=(JPopupMenu)mm.invoke(pad.privately);
                m.show(pad.frame.getContentPane(),pad.frame.getContentPane().getWidth()-280,8);
                Dimension s=m.getSize();if(s.width<=0||s.height<=0){s=m.getPreferredSize();m.setSize(s);m.doLayout();}
                menu[0]=new BufferedImage(Math.max(1,s.width),Math.max(1,s.height),BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=menu[0].createGraphics();m.paint(g);g.dispose();m.setVisible(false);
            }catch(Exception e){throw new RuntimeException(e);}});
            settle(pad);javax.imageio.ImageIO.write(menu[0],"png",out.resolve("private-menu.png").toFile());
            // The bin, on the same ground, with no + (decision 114).
            SwingUtilities.invokeAndWait(()->{
                PrivateSpace p=pad.privately.shown();
                for(PrivateSpace.Thing t:p.in(""))if(!t.folder&&t.name().startsWith("Doctor"))p.bin(t.id,6);
                try{java.lang.reflect.Field bin=DesktopPrivate.class.getDeclaredField("inBin");bin.setAccessible(true);bin.setBoolean(pad.privately,true);
                    java.lang.reflect.Method draw=DesktopPrivate.class.getDeclaredMethod("draw");draw.setAccessible(true);draw.invoke(pad.privately);}catch(Exception e){throw new RuntimeException(e);}
            });
            settle(pad);shot(pad,out.resolve("private-bin.png"));
            SwingUtilities.invokeAndWait(()->pad.privately.close());
            settle(pad);
            assertFalse(onEdt(()->pad.privately.showing()));
        } finally{close(pad);}
    }

    // ---- helpers --------------------------------------------------------------------------------------------------------

    private static void Hashes(){com.eurobuddha.maxima.core.crypto.Hashes.setSha3(Sha3::of);}
    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}

    /** The everyday note both installs write the same: made, opened, a line typed. */
    private String everyday(Desktop pad) throws Exception {
        NoteStore.Note n=new NoteStore.Note();
        n.book=Things.HOME;n.title="Groceries";n.body="milk, eggs";n.revision=1;n.updated=1000;n.place=-1000;pad.store.save(n);
        SwingUtilities.invokeAndWait(()->pad.open(n.id));settle(pad);
        SwingUtilities.invokeAndWait(()->pad.page.setCaretPosition(pad.page.getDocument().getLength()));
        type(pad,pad.page,"\nbread");
        Thread.sleep(500);settle(pad);
        return n.id;
    }

    /** The second password field, when it comes: the same password typed again, or another. */
    private static void answer(Desktop pad,String password) throws Exception {
        await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
        SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText(password);pad.privately.asking.postActionEvent();});
    }
    /** A space made from outside, as privatenote//: or privatefolder//: and the password twice make one. */
    private static void fromOutside(Desktop pad,PrivateCode.Kind kind,String password) throws Exception {
        pad.privately.go(kind,password.toCharArray());
        answer(pad,password);
        await(()->onEdt(()->pad.privately.showing()));
    }

    /** A password typed after a code: in the search, which every screen has, and Enter. */
    private void open(Desktop pad,String code) throws Exception {open(pad,code,PASSWORD);}
    private void open(Desktop pad,String code,String password) throws Exception {
        SwingUtilities.invokeAndWait(()->pad.home.search.requestFocusInWindow());
        type(pad,pad.home.search,code);
        await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
        SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText(password);pad.privately.asking.postActionEvent();});
        await(()->onEdt(()->pad.privately.showing()));
    }

    private static void type(Desktop pad,javax.swing.text.JTextComponent field,String text) throws Exception {
        for(char c:text.toCharArray())SwingUtilities.invokeAndWait(()->field.replaceSelection(String.valueOf(c)));
    }

    /** A synthetic photograph from a camera: 3000 by 2000, smooth, with EXIF saying it was held upright and naming the camera. */
    static byte[] photo(int seed) throws Exception {
        BufferedImage img=new BufferedImage(3000,2000,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();
        g.setPaint(new GradientPaint(0,0,new Color(40+seed*3%200,90,160),3000,2000,new Color(230,200-seed*2%150,90)));g.fillRect(0,0,3000,2000);
        g.setColor(new Color(250,250,240,120));for(int i=0;i<12;i++)g.fillOval((seed*137+i*251)%2800,(seed*71+i*173)%1800,220,220);
        g.dispose();
        ByteArrayOutputStream jpeg=new ByteArrayOutputStream();
        try(javax.imageio.stream.MemoryCacheImageOutputStream to=new javax.imageio.stream.MemoryCacheImageOutputStream(jpeg)){javax.imageio.ImageIO.write(img,"jpeg",to);}
        byte[] plain=jpeg.toByteArray();
        // An APP1 Exif segment: big-endian TIFF, one entry, Orientation 6 (turned a quarter), and the camera's name after it.
        ByteArrayOutputStream exif=new ByteArrayOutputStream();
        byte[] tiff={'M','M',0,42,0,0,0,8,0,1,1,0x12,0,3,0,0,0,1,0,6,0,0,0,0,0,0,0,0};
        byte[] camera=CAMERA.getBytes(StandardCharsets.US_ASCII);
        int length=2+6+tiff.length+camera.length;
        exif.write(0xFF);exif.write(0xE1);exif.write(length>>8);exif.write(length&255);exif.write(new byte[]{'E','x','i','f',0,0});exif.write(tiff);exif.write(camera);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(plain,0,2);out.write(exif.toByteArray());out.write(plain,2,plain.length-2);
        return out.toByteArray();
    }

    private static Desktop open(Path folder) throws Exception {
        Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        await(()->pad.page.isEditable()&&pad.store.latest()!=null&&pad.privately.slots()!=null);
        settle(pad);
        return pad;
    }
    private static void close(Desktop pad) throws Exception {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);pad.privately.settle();SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(150);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shot(Desktop pad,Path to) throws Exception {
        BufferedImage[] img={null};
        SwingUtilities.invokeAndWait(()->{Container c=pad.frame.getContentPane();c.validate();img[0]=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img[0].createGraphics();c.paint(g);g.dispose();});
        javax.imageio.ImageIO.write(img[0],"png",to.toFile());
    }
    private static Map<String,Long> files(Path root) throws Exception {
        Map<String,Long> out=new TreeMap<>();
        for(Path p:all(root))out.put(root.relativize(p).toString().replace('\\','/'),Files.size(p));
        return out;
    }
    private static List<Path> all(Path root) throws Exception {try(var walk=Files.walk(root)){return walk.filter(Files::isRegularFile).toList();}}
    private static int imageioCaches() throws Exception {
        try(var list=Files.list(Paths.get(System.getProperty("java.io.tmpdir")))){return (int)list.filter(p->p.getFileName().toString().startsWith("imageio")).count();}
    }
    /** The first of these words anywhere in the file, as UTF-8 or UTF-16, or null: read a piece at a time, the pieces overlapping. */
    static String find(Path file,String[] words) throws Exception {
        List<String[]> wanted=new ArrayList<>();
        for(String w:words){wanted.add(new String[]{w,new String(w.getBytes(StandardCharsets.UTF_8),StandardCharsets.ISO_8859_1)});
            wanted.add(new String[]{w,new String(w.getBytes(StandardCharsets.UTF_16LE),StandardCharsets.ISO_8859_1)});}
        try(var in=Files.newInputStream(file)) {
            byte[] piece=new byte[8<<20];int keep=0,n;
            while((n=in.readNBytes(piece,keep,piece.length-keep))>0) {
                String seen=new String(piece,0,keep+n,StandardCharsets.ISO_8859_1);
                for(String[] w:wanted)if(seen.contains(w[1]))return w[0];
                int tail=Math.min(200,seen.length());
                System.arraycopy(piece,seen.length()-tail,piece,0,tail);keep=tail;
            }
        }
        return null;
    }
    /** How small the file deflates to, a piece at a time. */
    static long packed(Path file) throws Exception {
        long total=0;
        try(var in=Files.newInputStream(file)){byte[] piece;while((piece=in.readNBytes(4<<20)).length>0)total+=Slots.deflate(piece).length;}
        return total;
    }
    static boolean contains(byte[] all,String word) {
        for(byte[] w:new byte[][]{word.getBytes(StandardCharsets.UTF_8),word.getBytes(StandardCharsets.UTF_16LE)}) {
            outer:for(int i=0;i+w.length<=all.length;i++){for(int j=0;j<w.length;j++)if(all[i+j]!=w[j])continue outer;return true;}
        }
        return false;
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
