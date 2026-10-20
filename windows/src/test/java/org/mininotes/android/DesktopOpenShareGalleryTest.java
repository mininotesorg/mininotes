package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * A file's one Open, named by its program, and Share with, people first with Done to go back (the owner, 2026-10-15).
 * Made-up file and names, offline; drawn from the windows themselves at the screen's scale, never from the screen.
 */
public class DesktopOpenShareGalleryTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private Desktop pad;

    @Before public void start() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w!=pad.frame&&w.isDisplayable())w.dispose();});
        if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void oneOpenNamedByItsProgramAndShareWithPeopleFirst() throws Exception {
        NoteStore.Held sheet=pad.store.addFileExact(Things.HOME,"Budget 2026.ods",null,new ByteArrayInputStream("PK made up".getBytes()));
        for(String[] one:new String[][]{{"MxAnaGallery@127.0.0.1:9901","Ana"},{"MxBenGallery@127.0.0.1:9902","Ben"}}) {
            Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder(one[1]).toPath().toFile()));
            pad.store.pairedWith(one[0],one[1],false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
        }
        NoteStore.Branch file=new NoteStore.Branch(NoteStore.Branch.Kind.FILE,sheet.id,Things.HOME,sheet.name,"",0,0,false,Tint.NONE);
        JPopupMenu[] menu={null};
        SwingUtilities.invokeAndWait(()->{pad.refresh();menu[0]=pad.home.fileMenu(file);});
        Thread.sleep(800);
        String first=((JMenuItem)java.util.Arrays.stream(menu[0].getComponents()).filter(c->c instanceof JMenuItem m&&m.isVisible()).findFirst().orElseThrow()).getText();
        assertEquals(DesktopApps.openLine("Budget 2026.ods"),first);
        boolean anyOpenToLook=java.util.Arrays.stream(menu[0].getComponents()).anyMatch(c->c instanceof JMenuItem m&&m.getText().startsWith("Open to"));
        assertFalse("one Open only",anyOpenToLook);
        SwingUtilities.invokeAndWait(()->{try{menu[0].setSize(menu[0].getPreferredSize());layAll(menu[0]);picture(menu[0],"91-file-menu",pad.frame);}catch(Exception e){throw new RuntimeException(e);}});
        SwingUtilities.invokeAndWait(()->DesktopPeople.with(pad,file,false,null,file.name,null));
        Thread.sleep(1500);
        JDialog[] box={null};
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"92-share-with");}catch(Exception e){throw new RuntimeException(e);}});
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(box[0],said));
        int people=said.indexOf("People"),groups=said.indexOf("Groups");
        assertTrue("people before groups: "+said,people>=0&&groups>people);
        assertTrue("a way back: "+said,said.stream().anyMatch(s->s.startsWith("Done, back to")));
    }

    /** Home right-clicked where there is no icon: Home, Create and Find, as ⋯ has them (the owner, 2026-10-16). */
    @Test public void homeRightClickedHasCreateAndFind() throws Exception {
        JPopupMenu[] menu={null};
        SwingUtilities.invokeAndWait(()->{pad.goHome();menu[0]=pad.roomMenuOf(Desktop.library());menu[0].setSize(menu[0].getPreferredSize());layAll(menu[0]);});
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(menu[0],said));
        for(String one:new String[]{"Home","Create","Find","Share a file through Mininotes…","Search…","Show on Home"})
            assertTrue(one+": "+said,said.stream().anyMatch(s->s.equalsIgnoreCase(one)||s.startsWith(one)));
        SwingUtilities.invokeAndWait(()->{try{picture(menu[0],"93-home-right-click",pad.frame);}catch(Exception e){throw new RuntimeException(e);}});
    }

    /** People and devices: one person, their two devices under them (decision 183). Made-up people. */
    @Test public void onePersonsDevicesUnderThem() throws Exception {
        String ana=Persons.newId(new java.security.SecureRandom());
        for(String[] one:new String[][]{{"MxAnaPhoneGal@127.0.0.1:9931","Ana"},{"MxAnaLaptopGal@127.0.0.1:9932","Ana"},{"MxBenGal@127.0.0.1:9933","Ben"}}) {
            Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder(one[0].substring(2,10)).toPath().toFile()));
            pad.store.pairedWith(one[0],one[1],false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
        }
        pad.store.belongsTo("MxAnaPhoneGal@127.0.0.1:9931",ana);pad.store.belongsTo("MxAnaLaptopGal@127.0.0.1:9932",ana);
        SwingUtilities.invokeAndWait(()->DesktopPeople.with(pad,null,false,null,null,null));
        Thread.sleep(1500);
        JDialog[] box={null};
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(box[0],said));
        assertTrue("Ana's two devices said: "+said,said.stream().anyMatch(t->t.contains("2 devices")));
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"94-person-devices");}catch(Exception e){throw new RuntimeException(e);}});
    }

    /** New, with what each thing is, from whom, when and where, and what can be done with it (decision 187). Made up. */
    @Test public void newSaysWhatEachIsAndWhatToDo() throws Exception {
        NoteStore.Held sheet=pad.store.addFileExact(Things.HOME,"Budget 2026.ods",null,new java.io.ByteArrayInputStream("PK made up".getBytes()));
        pad.store.getWritableDatabase().execSQL("UPDATE files SET fresh=1 WHERE id=?",new Object[]{sheet.id});
        SwingUtilities.invokeAndWait(pad::whatIsNew);
        Thread.sleep(1500);
        JDialog[] box={null};
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(box[0],said));
        for(String one:new String[]{"Open","Show where it is","Mark as seen","Mark all as seen"})assertTrue(one+": "+said,said.contains(one));
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"96-new");}catch(Exception e){throw new RuntimeException(e);}});
    }

    /** A person's page: their colour under their name, no menu of two lines (decision 185). Made up. */
    @Test public void aPersonsPageHasItsColourOnTop() throws Exception {
        Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder("ana").toPath().toFile()));
        pad.store.pairedWith("MxAnaPage@127.0.0.1:9941","Ana",false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
        SwingUtilities.invokeAndWait(()->DesktopPeople.person(pad,"MxAnaPage@127.0.0.1:9941","Ana"));
        Thread.sleep(1500);
        JDialog[] box={null};
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(box[0],said));
        assertTrue("colour on the page: "+said,said.contains("Writing colour"));
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"97-person-page");}catch(Exception e){throw new RuntimeException(e);}});
    }

    /** Who has access: my own devices with a dropdown, not Owner; Share with: contacts in a group listed too, groups open (decision 189). */
    @Test public void everyoneHasADropdownAndContactsAreAtHand() throws Exception {
        String pro="MxProOwn@127.0.0.1:9961",parisa="MxParisaGal@127.0.0.1:9962";
        for(String[] one:new String[][]{{pro,"Pro"},{parisa,"Parisa"}}) {
            Keys k=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder(one[1]).toPath().toFile()));
            pad.store.pairedWith(one[0],one[1],false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
        }
        pad.store.setMine(pro,true);
        String family=pad.store.makeGroup("Family");pad.store.putInGroup(family,parisa,true);
        NoteStore.Note live=new NoteStore.Note();live.title="Live";live.body="made up";live.book=Things.HOME;pad.store.save(live);
        pad.store.give(Sharing.Scope.PAGE,live.id,pro,Sharing.Level.WRITE,"Pro");
        NoteStore.Branch note=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,live.id,Things.HOME,"Live","",0,0,false);
        SwingUtilities.invokeLater(()->pad.share(note,null,null));
        Thread.sleep(1500);
        JDialog[] box={null};SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())box[0]=j;});
        assertNotNull(box[0]);
        java.util.List<String> said=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(box[0],said));
        assertTrue("my device has a dropdown: "+said,said.stream().anyMatch(t->t.startsWith("What Pro may do")));
        SwingUtilities.invokeAndWait(()->{try{shoot(box[0],"99-who-has-access");}catch(Exception e){throw new RuntimeException(e);}box[0].dispose();});
        SwingUtilities.invokeAndWait(()->DesktopPeople.with(pad,note,false,null,"Live",null));
        Thread.sleep(1500);
        JDialog[] with={null};SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog j&&j.isShowing())with[0]=j;});
        java.util.List<String> listed=new java.util.ArrayList<>();SwingUtilities.invokeAndWait(()->texts(with[0],listed));
        assertTrue("Parisa under People though in Family: "+listed,listed.stream().filter(t->t.equals("Parisa")).count()>=2);
        SwingUtilities.invokeAndWait(()->{try{shoot(with[0],"99b-share-with");}catch(Exception e){throw new RuntimeException(e);}});
    }

    private static void texts(Container in,java.util.List<String> out) {
        for(Component c:in.getComponents()) {
            if(c instanceof JLabel l&&l.getText()!=null)out.add(l.getText());
            if(c instanceof AbstractButton b&&b.getText()!=null)out.add(b.getText());
            if(c instanceof javax.accessibility.Accessible a&&a.getAccessibleContext()!=null&&a.getAccessibleContext().getAccessibleName()!=null)out.add(a.getAccessibleContext().getAccessibleName());
            if(c instanceof Container k)texts(k,out);
        }
    }
    private static void layAll(Container c){c.doLayout();for(Component k:c.getComponents())if(k instanceof Container in)layAll(in);}
    private static void picture(JComponent menu,String name,Window on) throws Exception {
        var scale=on.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(Math.max(1,menu.getWidth())*scale.getScaleX()),(int)Math.ceil(Math.max(1,menu.getHeight())*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);menu.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    private static void shoot(Window window,String name) throws Exception {
        var scale=window.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);window.paint(g);g.dispose();
        javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
