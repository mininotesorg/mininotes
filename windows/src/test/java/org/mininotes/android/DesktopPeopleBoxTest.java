package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * People and devices in two views with a page each, and Share with by name, on the PC (docs/HOME.md, decision 103): the
 * People view with each person's groups as chips under their name, the Groups view, a group's page, My devices' page and a
 * person's page, each with ‹ naming where it goes back to, and back one level at a time by ‹, Escape and the window's ✕;
 * Share with listing groups then people with the rights beside each name, a group folding open to its members, each As the
 * group or with rights of their own. And a message lying over the window (decision 97), which moves nothing on Home.
 * Synthetic devices, offline, no network; pictures saved beside the gallery's.
 */
public class DesktopPeopleBoxTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final Path SHOTS=Path.of("build","verification","gallery");

    /** A pad with a device of mine, two people, two groups, a note given to Friends and one given to Ana on her own. */
    private static final String ANA="MxBoxAna@127.0.0.1:9101",BEN="MxBoxBen@127.0.0.1:9102",LAPTOP="MxBoxLaptop@127.0.0.1:9103";
    private NoteStore.Note groceries,trip;private String friends,colleagues;
    private void fill(Desktop pad) throws Exception {
        NoteStore store=pad.store;
        Node.chooseName(pad.context,"Sam");Node.chooseDevice(pad.context,"Study PC");
        store.pairedWith(ANA,"Ana",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
        store.pairedWith(BEN,"Ben",false,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
        store.pairedWith(LAPTOP,"Work laptop",true,Envelope.keys().getPublic().getEncoded(),Envelope.keys().getPublic().getEncoded());
        groceries=note(store,"Groceries");trip=note(store,"Ferry trip");
        friends=store.makeGroup("Friends");colleagues=store.makeGroup("Colleagues");
        store.putInGroup(friends,ANA,true);
        store.shareWithGroup(Sharing.Scope.PAGE,groceries.id,friends,Sharing.Level.WRITE);
        store.give(Sharing.Scope.PAGE,trip.id,ANA,Sharing.Level.READ,null);
        settle(pad);
    }

    @Test public void twoViewsAPageEachAndAWayBackOneLevelAtATime() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            fill(pad);NoteStore store=pad.store;
            JDialog box=open(pad,()->pad.people(null));
            try {
                Container in=box.getContentPane();
                SwingUtilities.invokeAndWait(()->{
                    // The People view first: its three parts in order, each said once, and no way back from a first level.
                    assertEquals(DesktopPeople.TITLE,box.getTitle());assertNull(named(in,"back"));
                    assertTrue(toggle(in,"People").isSelected());assertFalse(toggle(in,"Groups").isSelected());
                    List<String> said=new ArrayList<>();texts(in,said);
                    // In the order the page is called by: People, My devices, then this one (decision 105). "People" is the
                    // view's own name too, so the part's is the last said.
                    int self=said.indexOf("This device"),mine=said.indexOf("My devices"),people=said.lastIndexOf("People");
                    assertTrue(said.toString(),people>=0&&mine>people&&self>mine);
                    assertTrue("Ana under People",said.indexOf("Ana")>people&&said.indexOf("Ana")<mine);
                    assertTrue("the laptop under My devices",said.indexOf("Work laptop")>mine&&said.indexOf("Work laptop")<self);
                    // Each part's one thing to do is beside its name, the same place in both.
                    // A + for each, the same sign at the same place (decision 106), and nothing at the foot.
                    JButton person=named(in,"addPerson"),device=named(in,"addDevice");
                    assertNotNull(person);assertNotNull(device);assertEquals("+",person.getText());assertEquals("+",device.getText());
                    assertEquals("Connect with someone",person.getAccessibleContext().getAccessibleName());assertEquals("Add a device",device.getAccessibleContext().getAccessibleName());
                    assertEquals("beside their headings, at the same edge",person.getLocationOnScreen().x+person.getWidth(),device.getLocationOnScreen().x+device.getWidth());
                    assertNull("no foot on the list",button(in,"Scan a code or paste a link…"));
                    // Under each person, a chip for each group: in is ticked, not in has a plus. None under a device of mine.
                    assertNotNull(called(in,"Ana is in Friends. Take Ana out of Friends"));
                    assertNotNull(called(in,"Ana is not in Colleagues. Put Ana in Colleagues"));
                    assertNotNull(called(in,"Ben is not in Friends. Put Ben in Friends"));
                    assertNull(called(in,"Work laptop is not in Friends. Put Work laptop in Friends"));
                    assertEquals("Friends  ✓",called(in,"Ana is in Friends. Take Ana out of Friends").getText());
                });
                shoot(box,"103-people-view");
                // A chip pressed puts them in, at once, and the box is drawn again where it is.
                SwingUtilities.invokeAndWait(()->called(in,"Ben is not in Friends. Put Ben in Friends").doClick(0));
                await(()->addresses(store.inGroups().get(friends)).contains(BEN));
                await(()->onEdt(()->called(in,"Ben is in Friends. Take Ben out of Friends")!=null));
                assertTrue("what the group has is his",has(store,groceries.id,BEN,Sharing.Level.WRITE));
                assertTrue("the same box",box.isShowing());
                // The Groups view: My devices first, then each group with who is in it, and New group….
                SwingUtilities.invokeAndWait(()->toggle(in,"Groups").doClick(0));
                await(()->onEdt(()->showing(in,"Friends · 2 people")!=null));
                SwingUtilities.invokeAndWait(()->{
                    List<String> said=new ArrayList<>();texts(in,said);
                    int mine=said.indexOf("My devices · 1 device"),work=said.indexOf("Colleagues · 0 people"),close=said.indexOf("Friends · 2 people");
                    assertTrue(said.toString(),mine>=0&&work>mine&&close>work);
                    assertTrue(said.toString(),said.contains("Ana, Ben")&&said.contains("Nobody in it yet")&&said.contains("Work laptop"));
                    assertNotNull(button(in,"New group…"));
                    assertNotNull("a group of my own can be renamed and deleted",called(in,"More for Friends"));
                    assertNull("My devices cannot",called(in,"More for My devices"));
                });
                shoot(box,"103b-groups-view");
                // A group's page: its people, what is shared with it, and ‹ naming where it goes back to.
                SwingUtilities.invokeAndWait(()->called(in,"Open Friends").doClick(0));
                await(()->onEdt(()->"Friends".equals(box.getTitle())&&showing(in,"Shared with this group")!=null));
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("‹  "+DesktopPeople.TITLE,named(in,"back").getText());
                    List<String> said=new ArrayList<>();texts(in,said);
                    assertTrue(said.toString(),said.indexOf("Members")<said.indexOf("Ana")&&said.indexOf("Ben")<said.indexOf("Shared with this group"));
                    assertNotNull("what the group has, by name",called(in,"Open the Share box of Groceries"));
                    assertNotNull("with the group's rights beside it",button(in,"Can write  ▾"));
                    assertNotNull(called(in,"Remove Ana from Friends"));assertNotNull(called(in,"More for Friends"));
                    // Add people drops down whoever is not in it yet: nobody is left.
                    button(in,"Add people  ▾").doClick(0);
                });
                await(()->onEdt(()->showing(in,"Everybody is in it already.")!=null));
                shoot(box,"103c-group-page");
                // A thing's name opens its Share box over the page, with ‹ naming the group; closed, the page is still there.
                JDialog shared=open(pad,()->called(in,"Open the Share box of Groceries").doClick(0));
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("Share “Groceries”",shared.getTitle());assertEquals("‹  Friends",named(shared.getContentPane(),"back").getText());
                    assertTrue("the page stays under it",box.isShowing());
                });
                shoot(shared,"103h-share-box-from-a-group");
                SwingUtilities.invokeAndWait(()->named(shared.getContentPane(),"back").doClick(0));
                await(()->!shared.isDisplayable());
                await(()->onEdt(()->"Friends".equals(box.getTitle())&&box.isShowing()&&showing(in,"Shared with this group")!=null));
                // A member's line opens their page, which goes back to the group.
                SwingUtilities.invokeAndWait(()->called(in,"Open Ana").doClick(0));
                await(()->onEdt(()->"Ana".equals(box.getTitle())&&showing(in,"Shared with Ana")!=null));
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("‹  Friends",named(in,"back").getText());
                    List<String> said=new ArrayList<>();texts(in,said);
                    assertTrue(said.toString(),said.contains("Parlons!")&&said.contains("Address")&&said.contains("Groups")&&said.contains("My device"));
                    assertTrue("what came through a group says which",said.contains("through Friends"));
                    assertNotNull(called(in,"Open the Share box of Ferry trip"));assertNotNull(called(in,"Open the Share box of Groceries"));
                    // How she is reached reads across the card. Whatever it says: a node another test left running in this JVM
                    // can make it more than "Not connected yet".
                    Component road=null;
                    for(String state:new String[]{Routes.NOT_STARTED,Routes.NEAR,Routes.DOOR_KNOWN,Routes.RELAYS,Routes.WAITS,Routes.QUIET})
                        if(road==null)road=startingWith(in,state);
                    assertNotNull("how each device is reached",road);
                    assertTrue("as wide as the card: "+road.getWidth(),road.getWidth()>in.getWidth()/2);
                });
                shoot(box,"103d-person-page");
                // Her Parlons! box opens over her page and goes back to it.
                JDialog parlons=open(pad,()->called(in,"Parlons! address of Ana: Not set").doClick(0));
                SwingUtilities.invokeAndWait(()->{
                    assertEquals("Ana on Parlons!",parlons.getTitle());assertEquals("‹  Ana",named(parlons.getContentPane(),"back").getText());
                    escape(parlons);
                });
                await(()->!parlons.isDisplayable());
                await(()->onEdt(()->"Ana".equals(box.getTitle())&&box.isShowing()&&showing(in,"Shared with Ana")!=null));
                // Back, one level at a time: to the group, then to the list, still in the Groups view. Escape does what ‹ does.
                SwingUtilities.invokeAndWait(()->named(in,"back").doClick(0));
                await(()->onEdt(()->"Friends".equals(box.getTitle())&&showing(in,"Shared with this group")!=null));
                SwingUtilities.invokeAndWait(()->escape(box));
                await(()->onEdt(()->DesktopPeople.TITLE.equals(box.getTitle())&&showing(in,"Friends · 2 people")!=null));
                SwingUtilities.invokeAndWait(()->{assertTrue("the view it was left in",toggle(in,"Groups").isSelected());assertNull(named(in,"back"));assertTrue(box.isShowing());});
                // My devices is a group like the others, with a page of its own: its devices, and no Remove, Rename or Delete.
                SwingUtilities.invokeAndWait(()->called(in,"Open My devices").doClick(0));
                await(()->onEdt(()->Groups.MY_DEVICES.equals(box.getTitle())&&showing(in,"Shared with this group")!=null));
                SwingUtilities.invokeAndWait(()->{
                    List<String> said=new ArrayList<>();texts(in,said);
                    assertTrue(said.toString(),said.contains("Devices")&&said.contains("Work laptop")&&said.contains("Nothing is shared with them as a group yet."));
                    assertNull(called(in,"More for My devices"));assertNull(called(in,"Remove Work laptop from My devices"));
                    assertNotNull(button(in,"Connect my other device…"));
                });
                shoot(box,"103e-my-devices-page");
                // The window's ✕ goes back one level too, and from the first level it closes the box.
                SwingUtilities.invokeAndWait(()->box.dispatchEvent(new java.awt.event.WindowEvent(box,java.awt.event.WindowEvent.WINDOW_CLOSING)));
                await(()->onEdt(()->DesktopPeople.TITLE.equals(box.getTitle())));
                assertTrue(onEdt(box::isShowing));
                SwingUtilities.invokeAndWait(()->box.dispatchEvent(new java.awt.event.WindowEvent(box,java.awt.event.WindowEvent.WINDOW_CLOSING)));
                await(()->!box.isDisplayable());
            } finally {SwingUtilities.invokeAndWait(box::dispose);}
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void shareWithListsGroupsThenPeopleWithTheRightsBesideEachName() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);settle(pad);
            fill(pad);NoteStore store=pad.store;
            NoteStore.Branch thing=new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,groceries.id,"","Groceries","",0,0,false);
            // From the thing's Share box: Add someone… opens Share with in its place, and ‹ goes back to the Share box.
            SwingUtilities.invokeAndWait(()->pad.open(groceries.id));settle(pad);
            JDialog share=open(pad,pad::share);
            assertEquals("Share “Groceries”",onEdt(share::getTitle));
            JDialog box=open(pad,()->button(share.getContentPane(),"Add someone…").doClick(0));
            try {
                Container in=box.getContentPane();
                SwingUtilities.invokeAndWait(()->{
                    assertFalse("in its place, not on top of it",share.isDisplayable());
                    assertEquals("Share “Groceries” with",box.getTitle());
                    assertEquals("‹  Groceries",named(in,"back").getText());
                    // Groups first, then people; the rights beside each name.
                    List<String> said=new ArrayList<>();texts(in,said);
                    assertTrue(said.toString(),said.indexOf("Groups")>=0&&said.indexOf("People")>said.indexOf("Groups"));
                    assertNotNull(button(in,"▸  My devices · 1 device"));assertNotNull(button(in,"▸  Colleagues · 0 people"));assertNotNull(button(in,"▸  Friends · 1 person"));
                    assertNotNull("Friends has it: the group's rights",called(in,"What Friends may do: Can write. "+Sharing.Level.WRITE.does()));
                    assertNotNull("Colleagues has not: Add",called(in,"Share with Colleagues"));
                    assertNotNull("nor has Ben, under People",called(in,"Share with Ben"));
                    assertEquals("Add  ▾",called(in,"Share with Ben").getText());
                    assertTrue("Ana is in a group: under it, not again under People",said.indexOf("Ben")>said.indexOf("People")&&!said.contains("Ana"));
                });
                shoot(box,"103f-share-with");
                // The group's name folds open to its people: Ana has it as the group does.
                SwingUtilities.invokeAndWait(()->button(in,"▸  Friends · 1 person").doClick(0));
                await(()->onEdt(()->button(in,"▾  Friends · 1 person")!=null&&button(in,"As the group  ▾")!=null));
                shoot(box,"103g-share-with-open");
                // Add beside Ben drops to the rights this PC may give; picking one shares at once, and the box stays.
                SwingUtilities.invokeAndWait(()->pick(pad,called(in,"Share with Ben"),Sharing.Level.READ.words()));
                await(()->has(store,groceries.id,BEN,Sharing.Level.READ));
                await(()->onEdt(()->called(in,"What Ben may do: Can read")!=null));
                assertTrue(onEdt(box::isShowing));
                // Rights of her own for one member of the group: hers, whatever the group has.
                SwingUtilities.invokeAndWait(()->pick(pad,button(in,"As the group  ▾"),Sharing.Level.READ.words()));
                await(()->has(store,groceries.id,ANA,Sharing.Level.READ));
                assertEquals(NoteStore.Stands.OWN,store.standsOn(Sharing.Scope.PAGE,groceries.id).get(ANA).how);
                await(()->onEdt(()->called(in,"What Ana may do: Can read")!=null));
                // As the group gives them up again.
                SwingUtilities.invokeAndWait(()->pick(pad,called(in,"What Ana may do: Can read"),Groups.AS_GROUP));
                await(()->has(store,groceries.id,ANA,Sharing.Level.WRITE));
                assertEquals(NoteStore.Stands.GROUP,store.standsOn(Sharing.Scope.PAGE,groceries.id).get(ANA).how);
                await(()->onEdt(()->button(in,"As the group  ▾")!=null));
                // A group not on it yet, given it from here.
                SwingUtilities.invokeAndWait(()->pick(pad,called(in,"Share with Colleagues"),Sharing.Level.READ.words()));
                await(()->store.groupsOn(Sharing.Scope.PAGE,groceries.id).size()==2);
                await(()->onEdt(()->called(in,"What Colleagues may do: Can read. "+Sharing.Level.READ.does())!=null));
                // ‹ goes back to the thing's Share box, drawn as things stand now.
                JDialog again=open(pad,()->named(in,"back").doClick(0));
                try {
                    SwingUtilities.invokeAndWait(()->{
                        assertFalse(box.isDisplayable());assertEquals("Share “Groceries”",again.getTitle());
                        List<String> said=new ArrayList<>();texts(again.getContentPane(),said);
                        assertTrue(said.toString(),said.contains("Friends")&&said.contains("Colleagues")&&said.contains("Ben"));
                    });
                } finally {SwingUtilities.invokeAndWait(again::dispose);}
            } finally {SwingUtilities.invokeAndWait(box::dispose);}
            // From the note's own + there is nothing to go back to: no ‹, and closing it closes it.
            JDialog direct=open(pad,()->pad.people(thing));
            try {
                SwingUtilities.invokeAndWait(()->{assertNull(named(direct.getContentPane(),"back"));escape(direct);});
                await(()->!direct.isDisplayable());
                Thread.sleep(300);
                SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())assertFalse("nothing opened in its place: "+w,w instanceof JDialog d&&d.isShowing());});
            } finally {SwingUtilities.invokeAndWait(direct::dispose);}
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    @Test public void aMessageLiesOverHomeAndMovesNothing() throws Exception {
        Files.createDirectories(SHOTS);
        Path folder=temp.newFolder("pad").toPath();Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        try {
            await(()->pad.page.isEditable()&&pad.store.latest()!=null);
            for(int i=1;i<=6;i++)note(pad.store,"Note "+i);
            SwingUtilities.invokeAndWait(()->{pad.goHome();pad.refresh();pad.status.setText(" ");});settle(pad);
            await(()->!pad.statusStrip.isVisible());
            Rectangle[] before=onEdt(()->new Rectangle[]{bounds(pad.home,pad.frame),bounds(pad.frame.getContentPane(),pad.frame)});
            SwingUtilities.invokeAndWait(()->pad.status.setText("Graphene deleted a file shared with you"));settle(pad);
            SwingUtilities.invokeAndWait(()->{
                assertTrue("the message is shown",pad.statusStrip.isShowing());
                // Nothing under it moved or changed size.
                assertEquals(before[0],bounds(pad.home,pad.frame));
                assertEquals(before[1],bounds(pad.frame.getContentPane(),pad.frame));
                // It lies over the foot of the window, nearly as wide as it.
                Rectangle strip=bounds(pad.statusStrip,pad.frame),content=before[1];
                assertTrue("over the content: "+strip+" in "+content,content.contains(strip));
                assertTrue("near the foot",content.y+content.height-(strip.y+strip.height)<=24);
                assertTrue("the window's width less a margin",strip.width>=content.width-40);
                assertEquals(JLayeredPane.MODAL_LAYER.intValue(),JLayeredPane.getLayer(pad.statusStrip));
            });
            shoot(pad.frame,"97c-message-over-home");
            // Going again moves nothing either.
            SwingUtilities.invokeAndWait(()->pad.status.setText(" "));settle(pad);
            SwingUtilities.invokeAndWait(()->{assertFalse(pad.statusStrip.isVisible());assertEquals(before[0],bounds(pad.home,pad.frame));});
            // Work going on stays; done, it says so.
            SwingUtilities.invokeAndWait(()->pad.status.setText("Writing backup…"));settle(pad);
            SwingUtilities.invokeAndWait(()->assertTrue(pad.statusStrip.isVisible()));
        } finally {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private static NoteStore.Note note(NoteStore store,String title) {
        NoteStore.Note one=new NoteStore.Note();one.book=Things.HOME;one.title=title;one.body="Synthetic "+title.toLowerCase();store.save(one);return one;
    }
    private static Rectangle bounds(Component c,Window in){return SwingUtilities.convertRectangle(c.getParent(),c.getBounds(),((RootPaneContainer)in).getRootPane());}
    /** A box opened by {@code open}, and found once it has drawn what it fetched. */
    private static JDialog open(Desktop pad,Runnable open) throws Exception {
        java.util.Set<Window> before=new java.util.HashSet<>(java.util.Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeLater(open);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until) {
            JDialog[] found={null};
            SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&!before.contains(w))found[0]=d;});
            if(found[0]!=null){Thread.sleep(600);SwingUtilities.invokeAndWait(found[0]::validate);Thread.sleep(200);return found[0];}
            Thread.sleep(50);
        }
        throw new AssertionError("The box did not open");
    }
    private static java.util.Set<String> addresses(List<NoteStore.Contact> in){java.util.Set<String> all=new java.util.HashSet<>();if(in!=null)for(NoteStore.Contact one:in)all.add(one.address);return all;}
    /** Whether somebody may do this with a note now. */
    private static boolean has(NoteStore store,String note,String address,Sharing.Level level) {
        for(Sharing.Rule one:store.membership(Sharing.Scope.PAGE,note))if(one.address.equals(address))return one.level==level;
        return false;
    }
    /** A button by the name a screen reader says, which says what it is about where its face is only a word or a mark. */
    private static AbstractButton called(Container in,String name) {
        for(Component c:in.getComponents()) {
            if(c instanceof AbstractButton b&&c.isShowing()&&name.equals(b.getAccessibleContext().getAccessibleName()))return b;
            if(c instanceof Container k){AbstractButton f=called(k,name);if(f!=null)return f;}
        }
        return null;
    }
    private static JToggleButton toggle(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof JToggleButton b&&!(c instanceof JCheckBox)&&words.equals(b.getText()))return b;if(c instanceof Container k){JToggleButton f=toggle(k,words);if(f!=null)return f;}}
        return null;
    }
    private static JButton named(Container in,String name) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&name.equals(b.getName())&&b.isShowing())return b;if(c instanceof Container k){JButton f=named(k,name);if(f!=null)return f;}}
        return null;
    }
    private static Component showing(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t&&c.isShowing()&&words.equals(t.getText()))return c;if(c instanceof Container k){Component f=showing(k,words);if(f!=null)return f;}}
        return null;
    }
    /** Escape, as the box hears it. */
    private static void escape(JDialog box) {
        box.getRootPane().getActionForKeyStroke(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,0)).actionPerformed(new java.awt.event.ActionEvent(box,java.awt.event.ActionEvent.ACTION_PERFORMED,"escape"));
    }
    /** A button pressed, and the line of the menu it drops that starts with these words chosen. On the event thread. */
    private static void pick(Desktop pad,AbstractButton drops,String words) {
        assertNotNull("nothing to press for "+words,drops);
        drops.doClick(0);
        JPopupMenu menu=null;
        MenuElement[] open=MenuSelectionManager.defaultManager().getSelectedPath();
        if(open.length>0&&open[0].getComponent() instanceof JPopupMenu m)menu=m;
        assertNotNull("no menu dropped from "+drops.getText(),menu);
        try {
            for(Component c:menu.getComponents())if(c instanceof JMenuItem item&&item.isEnabled()) {
                String says=item.getAccessibleContext().getAccessibleName();
                if(words.equals(item.getText())||says!=null&&says.startsWith(words)){item.doClick(0);return;}
            }
            throw new AssertionError("no line "+words+" in the menu of "+drops.getText());
        } finally {MenuSelectionManager.defaultManager().clearSelectedPath();}
    }
    private static void texts(Container in,List<String> out) {
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t)out.add(t.getText());if(c instanceof Container k)texts(k,out);}
    }
    private static Component label(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof JLabel l&&words.equals(l.getText()))return c;if(c instanceof Container k){Component f=label(k,words);if(f!=null)return f;}}
        return null;
    }
    private static Component startingWith(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof javax.swing.text.JTextComponent t&&t.getText().startsWith(words))return c;if(c instanceof Container k){Component f=startingWith(k,words);if(f!=null)return f;}}
        return null;
    }
    private static JButton button(Container in,String words) {
        for(Component c:in.getComponents()){if(c instanceof JButton b&&words.equals(b.getText()))return b;if(c instanceof Container k){JButton f=button(k,words);if(f!=null)return f;}}
        return null;
    }
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(250);SwingUtilities.invokeAndWait(pad.frame::validate);
    }
    private static void shoot(Window window,String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            var scale=window.getGraphicsConfiguration().getDefaultTransform();
            var image=new java.awt.image.BufferedImage((int)Math.ceil(window.getWidth()*scale.getScaleX()),(int)Math.ceil(window.getHeight()*scale.getScaleY()),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.transform(scale);
            Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");if(hints instanceof java.util.Map<?,?> map)g.addRenderingHints(map);
            window.paint(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",SHOTS.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
