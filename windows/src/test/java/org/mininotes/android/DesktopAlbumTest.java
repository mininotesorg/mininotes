// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * Photos on Home and folders shown as albums (the owner, 2026-10-19: "would be nice to have a type of folder specific for
 * photos ... It should come by default on the Home page for new installs and updates"; decision 217). Synthetic devices and
 * made-up pictures only.
 */
public class DesktopAlbumTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}
    private static final String PERSON="0123456789abcdef0123456789abcdef",OTHER="fedcba9876543210fedcba9876543210";
    private static final String PC="MxAlbumPc@127.0.0.1:9311",PHONE="MxAlbumPhone@127.0.0.1:9312";
    private static final Path SHOTS=Path.of("build","verification","gallery");

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;
        Device(String name,String person) throws Exception {
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            if(person!=null)context.getSharedPreferences("settings",0).edit().putString("person",person).apply();
        }
        void pair(String address,Device other) throws Exception {
            store.pairedWith(address,"Synthetic",true,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        int photos(){int n=0;for(String id:store.collectionIds())if(Album.PHOTOS.equals(store.nameOf(id,true)))n++;return n;}
    }

    @Test public void twoDevicesOfOneOwnerHaveOnePhotos() throws Exception {
        Device pc=new Device("pc",PERSON),phone=new Device("phone",PERSON);
        String made=pc.store.photosOnce();
        assertEquals("the same id on every device of the owner",Album.photosId(PERSON),made);
        assertEquals(Album.photosId(PERSON),phone.store.photosOnce());
        assertNotEquals("another person, another Photos",Album.photosId(PERSON),Album.photosId(OTHER));
        assertEquals(Album.ICON,pc.store.wornIcon(NoteStore.Branch.Kind.COLLECTION,made));
        assertTrue("on Home",NoteStore.home(pc.store.collectionOf(made)));
        assertTrue(pc.store.album(made));assertTrue(phone.store.album(made));
        // The PC's sent to the phone, which made its own: one Photos there still, under the one id.
        pc.pair(PHONE,phone);phone.pair(PC,pc);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,made,PHONE,true));
        Carton.Sent carton=pc.store.carton(made,PHONE,true);
        assertEquals(Album.PHOTOS,carton.name);
        byte[] sealed=Envelope.seal(Things.envelopeId(made),pc.store.revisionOf(made),1,Carton.wrap(carton),pc.keys.signing(),phone.keys.agreement().getPublic());
        Post.arrived(phone.context,phone.store,phone.keys,sealed);
        assertEquals(1,phone.photos());assertEquals(1,pc.photos());
    }

    @Test public void oneThatCameFirstIsNotMadeAgain() throws Exception {
        Device pc=new Device("pc",PERSON),phone=new Device("phone",PERSON);
        String made=pc.store.photosOnce();
        pc.pair(PHONE,phone);phone.pair(PC,pc);
        pc.store.addShare(new Sharing.Rule(Sharing.Scope.COLLECTION,made,PHONE,true));
        byte[] sealed=Envelope.seal(Things.envelopeId(made),pc.store.revisionOf(made),1,Carton.wrap(pc.store.carton(made,PHONE,true)),pc.keys.signing(),phone.keys.agreement().getPublic());
        Post.arrived(phone.context,phone.store,phone.keys,sealed);
        assertEquals(1,phone.photos());
        // The phone starts after it came: nothing made, the one that came is its Photos, an album there too.
        assertNull(phone.store.photosOnce());
        assertEquals(1,phone.photos());assertTrue(phone.store.album(made));
    }

    @Test public void madeOnceAndNeverAgain() throws Exception {
        Device pc=new Device("pc",null);
        assertNull("no person known yet: it waits",pc.store.photosOnce());
        assertEquals(0,pc.photos());
        pc.context.getSharedPreferences("settings",0).edit().putString("person",PERSON).apply();
        String made=pc.store.photosOnce();assertNotNull("made at a later start",made);
        assertNull("once",pc.store.photosOnce());
        // Renamed, then put in the bin and the bin emptied: never made again on this device.
        pc.store.renameCollection(made,"Holidays");assertNull(pc.store.photosOnce());
        pc.store.putAway(NoteStore.Branch.Kind.COLLECTION,made,true,true);
        assertNull(pc.store.photosOnce());assertEquals(0,pc.photos());
        // A new start on the same notebook remembers it.
        NoteStore again=new NoteStore(pc.context);again.getWritableDatabase();open.add(again);
        assertNull(again.photosOnce());
    }

    @Test public void albumsAreThisDevicesOwnView() throws Exception {
        Device pc=new Device("pc",PERSON);
        String trip=pc.store.addCollection("Trip").id;
        assertFalse(pc.store.album(trip));
        pc.store.showAsAlbum(trip,true);assertTrue(pc.store.album(trip));
        NoteStore again=new NoteStore(pc.context);again.getWritableDatabase();open.add(again);
        assertTrue("kept in the settings",again.album(trip));
        again.showAsAlbum(trip,false);assertFalse(again.album(trip));assertFalse(pc.store.album(trip));
    }

    @Test public void oneMadeAloneGivesWayOnceOnePerson() throws Exception {
        Device phone=new Device("phone",OTHER);
        String alone=phone.store.photosOnce();assertNotNull(alone);
        // Bonded with the owner's other device, whose person it now is: its empty Photos lets go, the shared one is the album.
        phone.store.photosFollow(OTHER,PERSON);
        assertEquals(0,phone.photos());assertTrue(phone.store.album(Album.photosId(PERSON)));
        assertNull("not made again",phone.store.photosOnce());
        // One somebody put something in stays.
        Device pc=new Device("pc",OTHER);String kept=pc.store.photosOnce();
        NoteStore.Note note=new NoteStore.Note();note.book=kept;note.body="Synthetic caption";pc.store.save(note);
        pc.store.photosFollow(OTHER,PERSON);
        assertEquals(1,pc.photos());
    }

    @Test public void aNewDeviceTakesTheOwnersPhotosAndNeverMakesOne() throws Exception {
        // A new device that made none before it was one person with the owner's other devices, where Photos was deleted: it
        // takes theirs as it is, gone, and makes none at a later start (it had come back on every device).
        Device fresh=new Device("fresh",null);
        assertNull(fresh.store.photosOnce());
        fresh.context.getSharedPreferences("settings",0).edit().putString("person",PERSON).apply();
        fresh.store.photosFollow(OTHER,PERSON);
        assertNull("not made here",fresh.store.photosOnce());
        assertEquals(0,fresh.photos());
        assertTrue("theirs an album here when it comes",fresh.store.album(Album.photosId(PERSON)));
        NoteStore again=new NoteStore(fresh.context);again.getWritableDatabase();open.add(again);
        assertNull("nor at the next start",again.photosOnce());
    }

    @Test public void picturesNewestFirstAndTheRestUnderThem() {
        NoteStore.Held old=new NoteStore.Held("a","f","beach.jpg","image/jpeg",10,100,NoteStore.Branch.Kind.COLLECTION);
        NoteStore.Held fresh=new NoteStore.Held("b","f","dune.png",null,10,200,NoteStore.Branch.Kind.COLLECTION);
        NoteStore.Held text=new NoteStore.Held("c","f","list.txt","text/plain",10,300,NoteStore.Branch.Kind.COLLECTION);
        List<NoteStore.Held> pictures=Album.pictures(List.of(old,fresh,text));
        assertEquals(List.of("b","a"),pictures.stream().map(h->h.id).toList());
        List<NoteStore.Branch> lines=List.of(new NoteStore.Branch(NoteStore.Branch.Kind.FILE,"a","f","beach.jpg","",0,0,false),
            new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,"n","f","Caption","",0,0,false),new NoteStore.Branch(NoteStore.Branch.Kind.FILE,"c","f","list.txt","",0,0,false));
        assertEquals("one the folder does not show is not in the album",List.of("a"),Album.shown(pictures,lines).stream().map(h->h.id).toList());
        assertEquals(List.of("n","c"),Album.others(lines,Album.shown(pictures,lines)).stream().map(b->b.id).toList());
        assertEquals(4,Album.across(700,DesktopAlbum.LEAST,DesktopAlbum.GAP));
        int side=Album.side(700,4,DesktopAlbum.GAP);assertTrue(side>=160&&side<=200);
    }

    private static BufferedImage plain(Color c,int w,int h){BufferedImage b=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();g.setColor(c);g.fillRect(0,0,w,h);g.dispose();return b;}
    private static List<DesktopAlbum.Picture> three() {
        List<DesktopAlbum.Picture> out=new ArrayList<>();
        String[] names={"red.png","green.png","blue.png"};
        for(int i=0;i<3;i++){NoteStore.Held h=new NoteStore.Held("p"+i+UUID.randomUUID(),"f",names[i],"image/png",100+i,1000-i,NoteStore.Branch.Kind.COLLECTION);
            out.add(new DesktopAlbum.Picture(new NoteStore.Branch(NoteStore.Branch.Kind.FILE,h.id,"f",h.name,"",0,0,false),h));}
        return out;
    }
    private static Color colour(NoteStore.Held h){return h.name.startsWith("red")?Color.RED:h.name.startsWith("green")?Color.GREEN:Color.BLUE;}
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("not drawn in time");
    }

    @Test public void theGridDrawsItsSquares() throws Exception {
        List<DesktopAlbum.Picture> pictures=three();
        int[] opened={-1};
        DesktopAlbum.Grid grid=new DesktopAlbum.Grid(h->plain(colour(h),256,256),at->opened[0]=at,(p,c)->{});
        JPanel room=new JPanel(null);
        SwingUtilities.invokeAndWait(()->{room.setSize(700,400);room.add(grid);grid.fill(pictures);grid.setBounds(0,0,700,grid.getPreferredSize().height);grid.doLayout();
            BufferedImage first=new BufferedImage(700,400,BufferedImage.TYPE_INT_RGB);Graphics2D g=first.createGraphics();grid.paint(g);g.dispose();});
        // The squares are made on the album's own thread, then drawn.
        BufferedImage[] shot={null};
        await(()->{SwingUtilities.invokeAndWait(()->{BufferedImage b=new BufferedImage(700,400,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();grid.paint(g);g.dispose();shot[0]=b;});
            return new Color(shot[0].getRGB(DesktopAlbum.Grid.PAD+40,DesktopAlbum.Grid.PAD+40)).equals(Color.RED);});
        int side=Album.side(700-2*DesktopAlbum.Grid.PAD,4,DesktopAlbum.GAP);
        assertEquals("the second square, beside the first",Color.GREEN,new Color(shot[0].getRGB(DesktopAlbum.Grid.PAD+side+DesktopAlbum.GAP+40,DesktopAlbum.Grid.PAD+40)));
        assertEquals(Color.BLUE,new Color(shot[0].getRGB(DesktopAlbum.Grid.PAD+2*(side+DesktopAlbum.GAP)+40,DesktopAlbum.Grid.PAD+40)));
        assertTrue("one row of four across",grid.getPreferredSize().height<side+3*DesktopAlbum.GAP+DesktopAlbum.Grid.PAD);
        SwingUtilities.invokeAndWait(()->grid.openAt(1));assertEquals(1,opened[0]);
        Files.createDirectories(SHOTS);javax.imageio.ImageIO.write(shot[0],"png",SHOTS.resolve("217-album-grid.png").toFile());
    }

    @Test public void theViewerGoesToTheNextAndThePrevious() throws Exception {
        List<DesktopAlbum.Picture> pictures=three();boolean[] closed={false};
        DesktopAlbum.Viewer[] v={null};
        SwingUtilities.invokeAndWait(()->{v[0]=new DesktopAlbum.Viewer(pictures,0,h->plain(colour(h),400,300),(p,c)->{},()->closed[0]=true);v[0].setSize(900,600);v[0].doLayout();});
        DesktopAlbum.Viewer viewer=v[0];
        SwingUtilities.invokeAndWait(()->{assertEquals("1 of 3",viewer.where.getText());assertEquals("red.png",viewer.name.getText());
            assertFalse("nothing before the first",viewer.before.isVisible());assertTrue(viewer.after.isVisible());
            viewer.previous();assertEquals(0,viewer.at);
            viewer.next();assertEquals(1,viewer.at);assertEquals("2 of 3",viewer.where.getText());
            viewer.next();viewer.next();assertEquals("stops at the last",2,viewer.at);assertFalse(viewer.after.isVisible());
            viewer.previous();assertEquals(1,viewer.at);});
        // Drawn fitted in the middle, once made.
        BufferedImage[] shot={null};
        await(()->{SwingUtilities.invokeAndWait(()->{BufferedImage b=new BufferedImage(900,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();viewer.paint(g);g.dispose();shot[0]=b;});
            return new Color(shot[0].getRGB(450,330)).equals(Color.GREEN);});
        assertEquals("dark round it",new Color(18,20,19),new Color(shot[0].getRGB(20,580)));
        Files.createDirectories(SHOTS);javax.imageio.ImageIO.write(shot[0],"png",SHOTS.resolve("217-album-viewer.png").toFile());
        // One gone from the folder since: the one after it shown; none left, closed.
        SwingUtilities.invokeAndWait(()->{viewer.without(Set.of(pictures.get(0).held().id,pictures.get(2).held().id));assertEquals(1,viewer.at);assertEquals("2 of 2",viewer.where.getText());
            viewer.without(Set.of());assertTrue(closed[0]);});
    }

    @Test public void photosOpensAsAnAlbumOnThePc() throws Exception {
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);pad.disk.flush(10000);
            NoteStore store=pad.store;
            pad.context.getSharedPreferences("settings",0).edit().putString("person",PERSON).apply();
            String photos=store.photosOnce();assertNotNull(photos);
            // Three made-up pictures and a note in it.
            for(int i=0;i<3;i++){String id=UUID.randomUUID().toString();BufferedImage p=plain(new Color[]{new Color(214,168,96),new Color(84,130,110),new Color(96,120,190)}[i],640,420);
                javax.imageio.ImageIO.write(p,"png",store.fileFor(id));store.keep(new NoteStore.Held(id,photos,"photo-"+i+".png","image/png",Files.size(store.fileFor(id).toPath()),1000+i,NoteStore.Branch.Kind.COLLECTION));}
            NoteStore.Note caption=new NoteStore.Note();caption.book=photos;caption.title="Captions";caption.body="Synthetic captions";store.save(caption);
            NoteStore.Branch line=new NoteStore.Branch(NoteStore.Branch.Kind.COLLECTION,photos,Sharing.EVERYTHING,Album.PHOTOS,"",0,0,true);
            SwingUtilities.invokeAndWait(()->{pad.goHome();pad.home.folder.open(line);});
            for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{assertTrue(pad.home.folder.albumShown);assertEquals(3,pad.home.folder.album.pictures.size());
                assertEquals("newest first","photo-2.png",pad.home.folder.album.pictures.get(0).held().name);
                assertTrue("the note under them",pad.home.folder.grid.isVisible());assertEquals(String.valueOf(store.contents(photos).stream().map(b->b.kind+" "+b.name).toList()),1,pad.home.folder.grid.tiles.size());
                assertTrue(pad.home.folder.albumView.heading.isVisible());});
            // The squares made off the disk thread, from the pictures as they are kept.
            SwingUtilities.invokeAndWait(()->pad.home.folder.album.repaint());
            await(()->{for(DesktopAlbum.Picture one:pad.home.folder.album.pictures)if(DesktopAlbum.squareMade(one.held())==null)return false;return true;});
            for(DesktopAlbum.Picture one:pad.home.folder.album.pictures)assertTrue(one.held().name,DesktopAlbum.squareMade(one.held()));
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{try{Files.createDirectories(SHOTS);BufferedImage b=new BufferedImage(pad.frame.getWidth(),pad.frame.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=b.createGraphics();pad.frame.paint(g);g.dispose();javax.imageio.ImageIO.write(b,"png",SHOTS.resolve("217-photos-card.png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            // A square opened: the viewer over the window; Esc closes it and the card stays.
            SwingUtilities.invokeAndWait(()->pad.home.folder.album.openAt(0));
            SwingUtilities.invokeAndWait(()->{assertNotNull(DesktopAlbum.up);assertEquals("1 of 3",DesktopAlbum.up.where.getText());});
            await(()->DesktopAlbum.up.ready()!=null);assertTrue("decoded large",DesktopAlbum.up.ready());
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(()->{try{BufferedImage b=new BufferedImage(pad.frame.getWidth(),pad.frame.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=b.createGraphics();pad.frame.paint(g);g.dispose();javax.imageio.ImageIO.write(b,"png",SHOTS.resolve("217-photos-viewer.png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{assertTrue(DesktopAlbum.closeViewer());assertNull(DesktopAlbum.up);assertTrue(pad.home.folder.isOpen());});
            // The switch off: its icons as any folder's.
            SwingUtilities.invokeAndWait(()->{store.showAsAlbum(photos,false);pad.refresh();});
            for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
            SwingUtilities.invokeAndWait(()->{assertFalse(pad.home.folder.albumShown);assertEquals(4,pad.home.folder.grid.tiles.size());});
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }
}
