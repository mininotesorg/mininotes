package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Every menu with the same top in the same order, and a symbol of its own for each thing it does (the owner, 2026-10-17;
 * decision 185). Made up, offline; the menus are built and read, and drawn from themselves, never from the screen.
 */
public class DesktopMenuOrderTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private Desktop pad;

    @Before public void start() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();
        SwingUtilities.invokeAndWait(()->{try{pad=new Desktop(folder,true);pad.show();}catch(Exception e){throw new RuntimeException(e);}});
        await(()->pad.page.isEditable()&&pad.store.latest()!=null);
    }
    @After public void close() throws Exception {if(pad!=null&&pad.frame.isDisplayable()){SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}}

    /** What a menu says, its headings and lines in order, and the symbol each line wears. */
    private static List<String[]> read(JPopupMenu menu) {
        List<String[]> out=new ArrayList<>();
        for(Component c:menu.getComponents()) {
            if(c instanceof JMenuItem m&&m.isVisible()){Object words=m.getClientProperty("words");String w=words==null?m.getText():words.toString();out.add(new String[]{"line",w,String.valueOf(Symbols.of(w))});}
            else if(c instanceof JLabel l&&l.getText()!=null)out.add(new String[]{"heading",l.getText(),""});
        }
        return out;
    }
    private static int at(List<String[]> said,String words){for(int i=0;i<said.size();i++)if(said.get(i)[1].equalsIgnoreCase(words)||said.get(i)[1].startsWith(words))return i;return -1;}
    private static void oneSymbolEach(String which,List<String[]> said) {
        Map<String,String> by=new HashMap<>();
        for(String[] one:said) {
            // Show on Home switches each name a place or a part of Home, and wear its symbol, which is the point of them.
            if("heading".equals(one[0])&&one[1].equalsIgnoreCase("SHOW ON HOME"))break;
            if(!"line".equals(one[0])||"null".equals(one[2]))continue;
            String was=by.putIfAbsent(one[2],one[1]);
            assertTrue(which+": "+one[1]+" and "+was+" wear the same symbol, "+one[2],was==null||was.equals(one[1]));
        }
    }

    @Test public void theSameTopEverywhereAndASymbolEach() throws Exception {
        Map<String,List<String[]>> menus=new LinkedHashMap<>();
        SwingUtilities.invokeAndWait(()->menus.put("note open",read(pad.mainMenu())));
        SwingUtilities.invokeAndWait(()->{pad.goHome();menus.put("home",read(pad.mainMenu()));menus.put("home right-clicked",read(pad.roomMenuOf(Desktop.library())));});
        NoteStore.Branch note=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,pad.store.latest().id,Things.HOME,"Plan","",0,0,false);
        SwingUtilities.invokeAndWait(()->menus.put("a note right-clicked",read(pad.thingMenu(note))));
        for(Map.Entry<String,List<String[]>> one:menus.entrySet()) {
            List<String[]> said=one.getValue();
            int colour=at(said,"Colour"),sync=at(said,"SYNC AND SHARING"),now=at(said,"Sync now"),create=at(said,"CREATE");
            int here=Math.max(at(said,"HOME"),at(said,"THIS NOTE"));
            assertTrue(one.getKey()+": colour first, Sync and sharing with Sync now in it: "+text(said),colour>=0&&sync>colour&&now==sync+1);
            // In ⋯ and on Home: under Home or This note, above Create (the owner, 2026-10-17).
            if(here>=0)assertTrue(one.getKey()+": Sync and sharing under "+said.get(here)[1]+": "+text(said),sync>here&&(create<0||create>sync));
            oneSymbolEach(one.getKey(),said);
        }
        // Text size once: the ladder at the top of ⋯, and no second one in This note.
        assertEquals("no Text size row in ⋯ while a note is open",-1,at(menus.get("note open"),"Text size"));
        // Put away last in a note's own menu.
        List<String[]> own=menus.get("a note right-clicked");
        assertEquals("Move to bin last",own.size()-1,at(own,"Move to bin"));
        SwingUtilities.invokeAndWait(()->{try{JPopupMenu m=pad.mainMenu();m.setSize(m.getPreferredSize());lay(m);picture(m,"95-menu-order",pad.frame);}catch(Exception e){throw new RuntimeException(e);}});
    }

    private static String text(List<String[]> said){StringBuilder b=new StringBuilder();for(String[] one:said)b.append(one[1]).append(" | ");return b.toString();}
    private static void lay(Container c){c.doLayout();for(Component k:c.getComponents())if(k instanceof Container in)lay(in);}
    private static void picture(JComponent menu,String name,Window on) throws Exception {
        var scale=on.getGraphicsConfiguration().getDefaultTransform();
        var image=new java.awt.image.BufferedImage((int)Math.ceil(Math.max(1,menu.getWidth())*scale.getScaleX()),(int)Math.ceil(Math.max(1,menu.getHeight())*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.transform(scale);menu.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
    }
    private static void await(Callable<Boolean> condition) throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Synthetic desktop did not finish");}
}
