package org.mininotes.android;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * People and devices in two views with a page each, and Share with by name (the owner, 2026-10-05: "the whole people and
 * devices page should be reworked to better present all these functionalities", then, the same day: "it should be clearly
 * 2 sections, people and their assignation to a group, and then the group management"; docs/HOME.md, decision 103).
 *
 * <p>One box, and levels inside it drawn in turn, not a box opened on a box: the list in its two views, a group's page,
 * a person's page, this device. Every level under the first has ‹ and where it goes back to at its top left, and Escape
 * and the window's ✕ go back one level too, to the view and the place in the list it was left at (the owner: "we can
 * always come back to the previous level when digging an element"). What a level shows is read again each time it is
 * drawn, off the event thread, so none of them says what was true when the box opened. A thing's Share box and a person's
 * Parlons! box open over the page they were asked from, with the same ‹, and the page is drawn again when they close.
 *
 * <p>Share with is the same box on one thing: groups first, then people, with the rights beside each name and changed
 * there (his choice, "Rights beside each name"). It stays open and is drawn again after each change.
 */
final class DesktopPeople {
    private static final int LIST=0,GROUP=1,PERSON=2,SELF=3,WITH=4;
    static final String TITLE="People and devices";

    /** One level: what it is, whose it is (a group's id, a device's address), what the level under it calls it, and where it was left. */
    private static final class Level {
        final int kind;final String id;String name;int scroll;boolean adding;
        Level(int kind,String id,String name){this.kind=kind;this.id=id;this.name=name;}
    }

    /** Everything a level draws from, read off the event thread. */
    private static final class Read {
        List<NoteStore.Contact> contacts=List.of();Map<String,String> through=Map.of(),sided=Map.of(),checks=Map.of();
        Map<String,String[]> roads=Map.of();List<String> here=List.of();
        List<Groups.Group> groups=List.of();Map<String,List<NoteStore.Contact>> in=Map.of();
        /** What a group or a person has, on their page. */
        List<NoteStore.Shared> shared=List.of();
        /** Share with: what this PC may give, the groups the thing is given to, and where everybody stands on it. */
        List<Sharing.Level> give=List.of();Map<String,Groups.Given> given=Map.of();Map<String,NoteStore.Stands> stands=Map.of();
        NoteStore.Contact contact(String address){for(NoteStore.Contact one:contacts)if(one.address.equals(address))return one;return null;}
        Groups.Group group(String id){for(Groups.Group one:groups)if(one.id.equals(id))return one;return null;}
        String groupName(String id){if(Groups.MINE.equals(id))return Groups.MY_DEVICES;Groups.Group one=group(id);return one==null?"a group":one.name;}
        /** The groups somebody is in, My devices among them for a device of mine. */
        Set<String> groupsOf(String address) {
            Set<String> all=new HashSet<>();
            for(Map.Entry<String,List<NoteStore.Contact>> one:in.entrySet())for(NoteStore.Contact who:one.getValue())if(who.address.equals(address))all.add(one.getKey());
            return all;
        }
    }

    private final Desktop app;
    /** The thing Share with is about; null for People and devices. */
    private final NoteStore.Branch target;
    private final Window over;private final String backTo;private final Runnable closed;private final boolean codeFirst;
    private final ArrayDeque<Level> levels=new ArrayDeque<>();
    private final JPanel holder=new JPanel(new BorderLayout()),foot=new JPanel(new BorderLayout());
    private JDialog box;private JScrollPane scroll;private Level laid;
    /** The view the list is in, People or Groups, kept while the box is open; and the groups folded open in Share with. */
    private boolean groupsView;private final Set<String> unfolded=new HashSet<>();
    /** Which reading the box waits for: an earlier one that comes back late draws nothing. */
    private int asked;
    /** This box closed to go on somewhere, a code to show or to scan, not to go back. */
    private boolean leaving;
    /** The round on a person's page, which their writing colour drops down from. */
    private Component round;

    private DesktopPeople(Desktop app,NoteStore.Branch target,boolean codeFirst,Window over,String backTo,Runnable closed) {
        this.app=app;this.target=target;this.codeFirst=codeFirst;this.over=over==null?app.frame:over;this.backTo=backTo;this.closed=closed;
        holder.setOpaque(false);foot.setOpaque(false);
        levels.push(target==null?new Level(LIST,"",TITLE):new Level(WITH,target.id,target.name));
    }

    /** People and devices, from the menu. */
    static void open(Desktop app){new DesktopPeople(app,null,false,null,null,null).draw();}

    /**
     * Share with, on one thing.
     *
     * @param codeFirst Show my code… ready to press when the box opens, for linking with somebody already listed
     * @param over      the box it was opened from, null for the window itself
     * @param backTo    what its ‹ says it goes back to, and {@code closed} what going back does; null where it was opened
     *                  from the thing itself, and closing it is all there is
     */
    static void with(Desktop app,NoteStore.Branch target,boolean codeFirst,Window over,String backTo,Runnable closed) {
        new DesktopPeople(app,target,codeFirst,over,backTo,closed).draw();
    }

    // ---- levels ---------------------------------------------------------------------------------------------

    /** The level on top read again and drawn: after every change, and whenever something arrives that it shows. */
    void draw() {
        int mine=++asked;Level at=levels.peek();
        if(laid==at&&scroll!=null)at.scroll=scroll.getVerticalScrollBar().getValue();
        app.disk.submit(()->read(at),got->{if(mine==asked&&(box==null||box.isDisplayable()))lay(at,got);},app::failed);
    }
    boolean showing(){return box!=null&&box.isShowing();}

    /** Down one level, the place in this one kept for coming back. */
    private void go(Level to){Level at=levels.peek();if(scroll!=null)at.scroll=scroll.getVerticalScrollBar().getValue();laid=null;levels.push(to);draw();}
    /** Back one level; from the first, the box closes. */
    private void back(){if(levels.size()>1){levels.pop();laid=null;draw();}else box.dispose();}
    /** The box closed to go on to something else, a code to show or to scan: not a way back, so nothing is opened again. */
    private void leave(Runnable then){leaving=true;box.dispose();then.run();}

    private Read read(Level at) throws Exception {
        Read r=new Read();NoteStore store=app.store;
        r.contacts=store.addresses();r.groups=store.groups();r.in=store.inGroups();r.through=store.linkedLines();
        if(at.kind==LIST||at.kind==PERSON) {
            r.roads=new HashMap<>();
            for(NoteStore.Contact one:r.contacts)if(one.paired()&&(at.kind==LIST||one.address.equals(at.id)))r.roads.put(one.address,Node.seen(one,Post.heardLately(one)));
        }
        if(at.kind==LIST||at.kind==SELF)r.here=Node.hereLines(app.context);
        if(at.kind==PERSON) {
            NoteStore.Contact who=r.contact(at.id);
            r.sided=new HashMap<>();r.checks=new HashMap<>();
            if(who!=null) {
                // Why only one end counts it as the owner's, and the six digits from this PC's key and theirs: the same on
                // their screen, if nothing came between the two.
                String line=Post.oneSided(app.context,who);if(line!=null)r.sided.put(who.address,line);
                if(who.signing.length>0)try{r.checks.put(who.address,Envelope.code(app.keys.signing().getPublic(),Keys.publicKey(who.signing)));}catch(Exception unreadable){/* no digits for it */}
                r.shared=store.sharedWith(who.address);
            }
            app.parlonsBook=store.parlonsBook();
        }
        if(at.kind==GROUP)r.shared=store.sharedWithGroup(at.id);
        if(at.kind==WITH) {
            Sharing.Scope scope=scope();
            r.give=store.mayGive(scope,target.id);r.stands=store.standsOn(scope,target.id);
            r.given=new HashMap<>();for(Groups.Given one:store.groupsOn(scope,target.id))r.given.put(one.group,one);
        }
        return r;
    }

    private Sharing.Scope scope(){return Sharing.Scope.valueOf(target.kind.name());}

    private void lay(Level at,Read r) {
        JPanel body=DesktopUi.column();JComponent more=null;String title=at.name;JComponent[] buttons={};
        switch(at.kind) {
            case LIST: list(body,r);buttons=new JComponent[]{app.button("Scan a code or paste a link…",()->leave(()->app.scanCode(null)))};break;
            case SELF: self(body,r);break;
            case GROUP: {
                Groups.Group group=r.group(at.id);
                if(group==null&&!Groups.MINE.equals(at.id)){back();return;}
                title=at.name=r.groupName(at.id);more=group==null?null:moreFor(group,true);group(body,r,at,group);break;
            }
            case PERSON: {
                NoteStore.Contact who=r.contact(at.id);
                if(who==null){back();return;}
                title=at.name=who.name;more=moreFor(who);person(body,r,who);break;
            }
            default: {
                title="Share “"+target.name+"” with";with(body,r);
                JButton code=app.button("Show my code…",()->leave(()->app.showCode(target)));code.setEnabled(!r.give.isEmpty());
                buttons=new JComponent[]{app.button("Scan a code or paste a link…",()->leave(()->app.scanCode(target))),code};
            }
        }
        boolean first=box==null;
        if(first) {
            box=DesktopUi.sheet(over,title,holder,foot,true);
            DesktopUi.leaves(box,this::back);
            box.addWindowListener(new WindowAdapter(){
                @Override public void windowClosed(WindowEvent e){app.peopleBoxes.remove(DesktopPeople.this);if(!leaving&&closed!=null)closed.run();}
            });
            app.peopleBoxes.add(this);
        }
        // Under the first level, ‹ names the level it goes back to; a first level opened from another box, that box.
        Iterator<Level> above=levels.iterator();above.next();
        String to=above.hasNext()?above.next().name:backTo;
        DesktopUi.head(box,to,this::back,title,more);
        holder.removeAll();scroll=DesktopUi.scrolling(body);holder.add(scroll);
        foot.removeAll();foot.add(DesktopUi.actions(buttons),BorderLayout.WEST);
        if(foot.getParent()!=null)foot.getParent().setVisible(buttons.length>0);
        laid=at;holder.revalidate();holder.repaint();
        int place=at.scroll;JScrollPane pane=scroll;
        if(first) {
            if(codeFirst&&buttons.length>1)box.getRootPane().putClientProperty("focus",buttons[1]);
            if(place>0)SwingUtilities.invokeLater(()->pane.getVerticalScrollBar().setValue(place));
            DesktopUi.show(box,600,target==null?640:560,720);
        } else {box.validate();SwingUtilities.invokeLater(()->pane.getVerticalScrollBar().setValue(place));}
    }

    // ---- small parts ----------------------------------------------------------------------------------------

    /** A heading over one part of a page. */
    private static void section(JPanel body,String said){DesktopUi.add(body,DesktopUi.heading(said));DesktopUi.gap(body,DesktopUi.S);}
    /** A heading with one thing to do in that part at its right. */
    private static void section(JPanel body,String said,JComponent does) {
        JPanel line=DesktopUi.row(DesktopUi.heading(said),does);line.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        DesktopUi.add(body,line);DesktopUi.gap(body,DesktopUi.S);
    }
    private static JMenuItem item(JPopupMenu menu,String label,Runnable go){JMenuItem item=new JMenuItem(label);item.addActionListener(e->go.run());menu.add(item);return item;}
    /** ⋮, for what can be done to the thing a line or a page is about. */
    private static JButton more(String name,JPopupMenu menu) {
        JButton more=new JButton("⋮");more.setFocusPainted(false);more.getAccessibleContext().setAccessibleName("More for "+name);
        more.addActionListener(e->menu.show(more,0,more.getHeight()));return more;
    }
    /** A line that opens a page: whatever it says on the left, › at its right, and a click anywhere on it going in. */
    private static JPanel opens(JComponent left,String name,Runnable go,JComponent before) {
        JButton open=DesktopUi.quietButton("›",go);open.setFont(DesktopUi.BODY.deriveFont(22f));open.setMargin(new Insets(0,8,2,8));open.getAccessibleContext().setAccessibleName("Open "+name);
        JPanel line=DesktopUi.row(left,before==null?open:DesktopUi.footer(before,open));
        MouseAdapter click=new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))go.run();}};
        clicks(line,click);line.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return line;
    }
    private static void clicks(Component on,MouseListener click) {
        if(on instanceof AbstractButton)return;
        on.addMouseListener(click);on.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if(on instanceof Container k)for(Component one:k.getComponents())clicks(one,click);
    }
    /** A thing's name on a group's or a person's page, with what it came through quietly under it: a click opens its Share box. */
    private JComponent thing(NoteStore.Shared one,String under,String from) {
        JButton name=DesktopUi.quietButton(one.said(),()->app.share(new NoteStore.Branch(one.kind(),one.target,"",one.name,"",0,0,false),box,from));
        name.setForeground(DesktopUi.INK);name.setFont(DesktopUi.BODY);name.setBorder(BorderFactory.createEmptyBorder(2,0,2,6));name.setHorizontalAlignment(SwingConstants.LEFT);
        name.setToolTipText("Who has “"+one.name+"”");name.getAccessibleContext().setAccessibleName("Open the Share box of "+one.said());
        JPanel words=DesktopUi.column();DesktopUi.add(words,DesktopUi.actions(name));
        if(under!=null)DesktopUi.add(words,DesktopUi.quiet(under));
        return words;
    }
    private void say(String words){app.status.setToolTipText(null);app.status.setText(words);}

    // ---- the list, in its two views ---------------------------------------------------------------------------

    private void list(JPanel body,Read r) {
        DesktopUi.add(body,DesktopUi.views(new String[]{"People","Groups"},groupsView?1:0,to->{groupsView=to==1;levels.peek().scroll=0;laid=null;draw();}));
        DesktopUi.gap(body,DesktopUi.M);
        if(groupsView)groupsView(body,r);else peopleView(body,r);
    }

    /** People: this device, my devices, then everybody else with the groups each is in under their name, changed there. */
    private void peopleView(JPanel body,Read r) {
        String here=Node.deviceHere(app.context);
        section(body,"This device");
        DesktopUi.add(body,DesktopUi.card(null,opens(DesktopUi.person(here,r.here.isEmpty()?null:r.here.get(0)),here,()->go(new Level(SELF,"",here)),null)));
        DesktopUi.gap(body,DesktopUi.L);
        JPanel own=DesktopUi.column(),others=DesktopUi.column();
        for(NoteStore.Contact one:r.contacts) {
            if(one.mine){DesktopUi.add(own,line(one,r));continue;}
            if(others.getComponentCount()>0)DesktopUi.gap(others,4);
            DesktopUi.add(others,line(one,r));
            // The groups they are in, right under the name: a chip each, changed at once (the owner: "people and their
            // assignation to a group"). None where there is no group yet, or nothing to put in one.
            if(one.paired()&&!r.groups.isEmpty()) {
                Set<String> theirs=r.groupsOf(one.address);List<JComponent> chips=new ArrayList<>();
                for(Groups.Group group:r.groups) {
                    boolean in=theirs.contains(group.id);
                    JButton chip=DesktopUi.chip(group.name,in,()->put(one,group,!in));
                    chip.setToolTipText(in?"Take "+one.name+" out of "+group.name:"Put "+one.name+" in "+group.name);
                    chip.getAccessibleContext().setAccessibleName(one.name+(in?" is in ":" is not in ")+group.name+". "+chip.getToolTipText());
                    chips.add(chip);
                }
                DesktopUi.add(others,DesktopUi.wrapping(44,chips.toArray(new JComponent[0])));
            }
        }
        // Each part says what to do when it is empty, and My devices always offers to connect another.
        section(body,Groups.MY_DEVICES);
        if(own.getComponentCount()==0)DesktopUi.add(body,DesktopUi.spread("No other device yet. Connect my other device to keep the same notes on both."));
        else DesktopUi.add(body,DesktopUi.card(null,own));
        DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.actions(app.button("Connect my other device…",()->leave(()->app.showCode(Desktop.library())))));
        DesktopUi.gap(body,DesktopUi.L);
        section(body,"People",app.button("Add someone…",()->leave(()->app.scanCode(null))));
        if(others.getComponentCount()==0)DesktopUi.add(body,DesktopUi.spread("Nobody else yet. Share a note or a folder to add somebody."));
        else DesktopUi.add(body,DesktopUi.card(null,others));
    }

    /** One device or person in the list: their round in their colour, their name, how they are reached, and › to their page. */
    private JPanel line(NoteStore.Contact one,Read r) {
        String[] road=r.roads.get(one.address);
        String under=!one.paired()?"Not paired yet":r.through.containsKey(one.address)?r.through.get(one.address):road!=null&&road.length>0?road[0]:null;
        JPanel line=opens(app.inRound(DesktopUi.person(one.name,under),one.address),one.name,()->go(new Level(PERSON,one.address,one.name)),null);
        // A right-click offers what their page's ⋮ does.
        DesktopMenus.onRightClick(line,e->menuFor(one,false).show(e.getComponent(),e.getX(),e.getY()));
        return line;
    }

    /** Groups: making them, and who is in each, My devices first as the one every owner has. */
    private void groupsView(JPanel body,Read r) {
        DesktopUi.add(body,DesktopUi.row(DesktopUi.spread("A group is the same on all your devices. What is shared with it is shared with everybody in it."),app.button("New group…",this::newGroup)));
        DesktopUi.gap(body,DesktopUi.S);
        JPanel lines=DesktopUi.column();
        DesktopUi.add(lines,groupLine(r,Groups.MINE,Groups.MY_DEVICES,null));
        for(Groups.Group group:r.groups){DesktopUi.gap(lines,4);DesktopUi.add(lines,groupLine(r,group.id,group.name,group));}
        DesktopUi.add(body,DesktopUi.card(null,lines));
        if(r.groups.isEmpty()){DesktopUi.gap(body,DesktopUi.M);DesktopUi.add(body,DesktopUi.spread("No group of your own yet. Make one, like Friends or Colleagues, then put people in it."));}
    }

    private JPanel groupLine(Read r,String id,String name,Groups.Group group) {
        List<NoteStore.Contact> in=r.in.getOrDefault(id,List.of());List<String> who=new ArrayList<>();
        for(NoteStore.Contact one:in)who.add(one.name);
        who.sort(String.CASE_INSENSITIVE_ORDER);
        JPanel words=DesktopUi.column();DesktopUi.add(words,DesktopUi.body(Groups.counted(name,in.size(),group==null)));
        DesktopUi.add(words,DesktopUi.quiet(who.isEmpty()?(group==null?"No other device yet":"Nobody in it yet"):String.join(", ",who)));
        JPanel line=opens(words,name,()->go(new Level(GROUP,id,name)),group==null?null:moreFor(group,false));
        if(group!=null)DesktopMenus.onRightClick(line,e->menuFor(group,false).show(e.getComponent(),e.getX(),e.getY()));
        return line;
    }

    // ---- what is done to a group ------------------------------------------------------------------------------

    private JButton moreFor(Groups.Group group,boolean onPage){return more(group.name,menuFor(group,onPage));}

    /** Rename… and Delete group, on a group's line and on its page; My devices has neither. */
    private JPopupMenu menuFor(Groups.Group group,boolean onPage) {
        JPopupMenu menu=new JPopupMenu();
        item(menu,"Rename…",()->{String name=DesktopUi.ask(box,"Rename group","Name",group.name);if(name==null||name.equals(group.name))return;
            app.groupWork("Renaming "+group.name+"…",()->{app.store.renameGroup(group.id,name);return List.<Groups.Changed>of();},"Renamed to "+name,this::draw);});
        menu.addSeparator();
        item(menu,"Delete group",()->{
            if(!DesktopUi.confirm(box,"Delete "+group.name+"?","The people in it stay. What was shared with the group is taken from them, unless it was shared with them on their own.","Delete",true))return;
            // From its own page there is no page left to draw: back to the list, in the view it was left in.
            app.groupWork("Deleting "+group.name+"…",()->app.store.deleteGroup(group.id),"Deleted "+group.name,onPage?this::back:this::draw);});
        return menu;
    }

    private void newGroup() {
        String name=DesktopUi.ask(box,"New group","Name, like Friends or Colleagues",null);if(name==null)return;
        app.groupWork("Making "+name+"…",()->{app.store.makeGroup(name);return List.<Groups.Changed>of();},"Made "+name,this::draw);
    }

    /** Somebody put in a group or taken out of it, at once, said in the status line from start to end. */
    private void put(NoteStore.Contact who,Groups.Group group,boolean in) {
        app.groupWork((in?"Putting "+who.name+" in ":"Taking "+who.name+" out of ")+group.name+"…",
            ()->app.store.putInGroup(group.id,who.address,in),who.name+(in?" is in ":" is out of ")+group.name,this::draw);
    }

    // ---- a group's page ---------------------------------------------------------------------------------------

    /** A group's page: who is in it, added and removed here, and what is shared with it, with the group's rights on each. */
    private void group(JPanel body,Read r,Level at,Groups.Group group) {
        boolean mine=group==null;String name=r.groupName(at.id);
        List<NoteStore.Contact> in=r.in.getOrDefault(at.id,List.of());
        List<NoteStore.Contact> out=new ArrayList<>();
        if(!mine)for(NoteStore.Contact one:r.contacts)if(!one.mine&&one.paired()&&!r.groupsOf(one.address).contains(at.id))out.add(one);
        JButton add=mine?app.button("Connect my other device…",()->leave(()->app.showCode(Desktop.library())))
            :app.button(at.adding?"Add people  ▴":"Add people  ▾",()->{at.adding=!at.adding;draw();});
        section(body,mine?"Devices":"Members",add);
        // Whoever is not in it yet, dropped down under the heading: a click adds, and the list stays for the next one.
        if(at.adding&&!mine) {
            JPanel list=DesktopUi.column();
            for(NoteStore.Contact one:out) {
                JButton put=app.button("Add",()->put(one,group,true));put.getAccessibleContext().setAccessibleName("Add "+one.name+" to "+name);
                DesktopUi.add(list,DesktopUi.row(app.inRound(DesktopUi.person(one.name,null),one.address),put));
            }
            if(out.isEmpty())DesktopUi.add(list,DesktopUi.spread(in.isEmpty()?"Nobody to add yet. Share a note or a folder with somebody first, or scan their code.":"Everybody is in it already."));
            DesktopUi.add(body,DesktopUi.card("Not in "+name+" yet",list));DesktopUi.gap(body,DesktopUi.S);
        }
        if(in.isEmpty())DesktopUi.add(body,DesktopUi.spread(mine?"No other device yet. Connect my other device to keep the same notes on both.":"Nobody in this group yet. Add people to put somebody in it."));
        else {
            JPanel members=DesktopUi.column();
            for(NoteStore.Contact one:in) {
                // Removed from the group, asked first, with what they lose said by name.
                JButton remove=mine?null:app.button("Remove",()->{
                    List<String> loses=new ArrayList<>();for(NoteStore.Shared has:r.shared)loses.add(has.said());
                    if(!DesktopUi.confirm(box,"Take "+one.name+" out of "+name+"?",(loses.isEmpty()?"Nothing is shared with "+name+" yet, so they lose nothing."
                        :"They lose what is shared with "+name+": "+String.join(", ",loses)+".")+" What was shared with them on their own stays.","Remove",true))return;
                    put(one,group,false);});
                if(remove!=null)remove.getAccessibleContext().setAccessibleName("Remove "+one.name+" from "+name);
                DesktopUi.add(members,opens(app.inRound(DesktopUi.person(one.name,null),one.address),one.name,()->go(new Level(PERSON,one.address,one.name)),remove));
            }
            DesktopUi.add(body,DesktopUi.card(null,members));
        }
        DesktopUi.gap(body,DesktopUi.L);
        section(body,"Shared with this group");
        if(mine){DesktopUi.add(body,DesktopUi.spread("Your devices keep the same notes already. This is what was also shared with them as a group."));DesktopUi.gap(body,DesktopUi.S);}
        if(r.shared.isEmpty())DesktopUi.add(body,DesktopUi.spread(mine?"Nothing is shared with them as a group yet.":"Nothing is shared with this group yet. Share a note or a folder and pick the group."));
        else {
            JPanel things=DesktopUi.column();
            for(NoteStore.Shared one:r.shared) {
                java.util.function.Consumer<Sharing.Level> choose=to->app.groupWork((to==Sharing.Level.GONE?"Removing ":"Sharing with ")+name+"…",
                    ()->app.store.shareWithGroup(one.scope,one.target,at.id,to),to==Sharing.Level.GONE?"Removed "+name+" from "+one.name:"Shared with "+name+": "+to.words(),this::draw);
                DesktopUi.add(things,DesktopUi.row(thing(one,null,name),one.mayChange?app.roleButton(one.level,one.give,choose,name):Desktop.roleShown(one.level.words(),one.level.does()+" Only its owner or an admin can change this.")));
            }
            DesktopUi.add(body,DesktopUi.card(null,things));
        }
    }

    // ---- a person's page --------------------------------------------------------------------------------------

    private JButton moreFor(NoteStore.Contact who){return more(who.name,menuFor(who,true));}

    /** Writing colour and Forget…, on a person's page; on their line in the list, opening their page first and My device as well. */
    private JPopupMenu menuFor(NoteStore.Contact who,boolean onPage) {
        JPopupMenu menu=new JPopupMenu();
        if(!onPage) {
            item(menu,"Open",()->go(new Level(PERSON,who.address,who.name)));
            JCheckBoxMenuItem mine=new JCheckBoxMenuItem("My device",who.mine);mine.addActionListener(e->mine(who,!who.mine));menu.add(mine);
        } else item(menu,"Writing colour",()->{if(round!=null)app.inks(round,who.name,who.address);});
        menu.addSeparator();
        item(menu,"Forget…",()->forget(who,onPage));
        return menu;
    }

    private void mine(NoteStore.Contact who,boolean yes) {
        say(yes?"Counting "+who.name+" as my device…":"No longer counting "+who.name+" as my device…");
        app.disk.submit(()->{app.store.setMine(who.address,yes);return null;},done->{say(who.name+(yes?" is one of my devices":" is not one of my devices"));draw();app.refresh();},app::failed);
    }

    /** Forget, as the phone's does: off the list you pick from, nothing unshared. Asked first. */
    private void forget(NoteStore.Contact who,boolean onPage) {
        if(!DesktopUi.confirm(box,"Forget this device?",who.name+"\n\nAnything already shared with it stays shared; this only removes it from the list you pick from.","Forget",true))return;
        app.disk.submit(()->{app.store.removeAddress(who.address);return null;},done->{say("Forgotten");if(onPage)back();else draw();},app::failed);
    }

    /** A person's page: how they are reached, their Parlons! address, their groups, what is shared with them, and what can be done to them. */
    private void person(JPanel body,Read r,NoteStore.Contact who) {
        JPanel card=DesktopUi.column();
        JPanel named=app.inkOnRound(DesktopUi.person(who.name,who.paired()?r.through.get(who.address):"Not paired yet"),who.name,who.address);
        round=Desktop.roundOf(named);DesktopUi.add(card,named);
        // How it is reached, one line the width of the card (decision 97); why only one end counts it as the owner's; the
        // six digits to check with them.
        String[] road=r.roads.get(who.address);
        if(road!=null){DesktopUi.gap(card,6);DesktopUi.add(card,DesktopUi.spread(Routes.oneLine(road)));}
        if(r.sided.containsKey(who.address)){DesktopUi.gap(card,4);DesktopUi.add(card,DesktopUi.spread(r.sided.get(who.address)));}
        if(r.checks.containsKey(who.address)){DesktopUi.gap(card,4);DesktopUi.add(card,DesktopUi.spread("Check with them: "+r.checks.get(who.address)));}
        DesktopUi.gap(card,DesktopUi.S);
        // Their Parlons! address, shortened, or Not set; › opens the box that sets it, and with one set they are contacted from here (decision 101).
        if(!who.mine) {
            JButton parlons=app.button(Parlons.shortened(who.parlons)+"  ›",()->app.parlonsBox(who,this::draw,box,who.name));
            parlons.getAccessibleContext().setAccessibleName("Parlons! address of "+who.name+": "+Parlons.shortened(who.parlons));
            JButton contact=who.parlons.isEmpty()?null:app.button(Parlons.CONTACT,()->app.copyParlons(who.name,who.parlons));
            DesktopUi.add(card,DesktopUi.row(DesktopUi.body("Parlons!"),DesktopUi.footer(contact,parlons)));
        }
        JButton address=app.button("Show  ›",()->{
            // The whole address, wrapped and selectable, its code beside it, and one button to copy it.
            JPanel shown=DesktopUi.column();
            DesktopUi.Text text=DesktopUi.note(who.address,300,DesktopUi.INK,new Font("Consolas",Font.PLAIN,12));
            try{JLabel code=new JLabel(new ImageIcon(DesktopQr.draw(who.address,220)));code.getAccessibleContext().setAccessibleName("The address of "+who.name+" as a QR code");
                JPanel side=new JPanel(new BorderLayout(DesktopUi.M,0));side.setOpaque(false);side.add(DesktopUi.card(null,code),BorderLayout.WEST);side.add(text);DesktopUi.add(shown,side);}
            catch(Exception noCode){DesktopUi.add(shown,text);}
            DesktopUi.gap(shown,DesktopUi.M);DesktopUi.Text copied=DesktopUi.quiet(" ");
            DesktopUi.add(shown,DesktopUi.actions(DesktopUi.button("Copy the address",()->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(who.address),null);copied.setText("Copied.");})));
            DesktopUi.gap(shown,4);DesktopUi.add(shown,copied);
            DesktopUi.tell(box,who.name,shown);});
        address.getAccessibleContext().setAccessibleName("Show the address of "+who.name);
        DesktopUi.add(card,DesktopUi.row(DesktopUi.body("Address"),address));
        DesktopUi.add(body,DesktopUi.card(null,card));
        // The groups they are in, a switch each (decision 100): on puts them in, and whatever the group has is theirs.
        if(!who.mine&&who.paired()&&!r.groups.isEmpty()) {
            DesktopUi.gap(body,DesktopUi.L);section(body,"Groups");
            JPanel in=DesktopUi.column();Set<String> theirs=r.groupsOf(who.address);
            for(Groups.Group group:r.groups) {
                JCheckBox on=DesktopUi.toggle(group.name,theirs.contains(group.id));
                on.addActionListener(e->put(who,group,on.isSelected()));
                DesktopUi.add(in,DesktopUi.switchRow(group.name,on));
            }
            DesktopUi.add(body,DesktopUi.card(null,in));
        }
        // What they have, thing by thing, with the rights on each changed here or taken away; what came through a group says
        // which, and changed here it becomes their own.
        DesktopUi.gap(body,DesktopUi.L);section(body,"Shared with "+who.name);
        if(r.shared.isEmpty())DesktopUi.add(body,DesktopUi.spread("Nothing shared with "+who.name+" yet."));
        else {
            JPanel things=DesktopUi.column();
            for(NoteStore.Shared one:r.shared) {
                java.util.function.Consumer<Sharing.Level> choose=to->{
                    say("Updating access…");
                    app.disk.submit(()->{app.store.decide(one.rule,to,System.currentTimeMillis());return null;},done->{draw();
                        app.network.submit(()->{Post.changed(app.context,app.store,app.keys,one.kind(),one.target);Post.removedAgain(app.context,app.store,app.keys);return null;},
                            v->{say(to==Sharing.Level.GONE?"Removed "+who.name+" from "+one.name:"Access updated");app.refresh();},app::failed);},app::failed);};
                // Whose it is, said as Owner; a role this PC may change, dropping down to what it may give; any other said.
                JComponent role=one.owner?Desktop.roleShown(Sharing.OWNER,Sharing.OWNER_DOES)
                    :one.mayChange?app.roleButton(one.level,one.give,choose,who.name)
                    :Desktop.roleShown(one.level.words(),one.level.does()+" Only its owner or an admin can change this.");
                DesktopUi.add(things,DesktopUi.row(thing(one,one.group.isEmpty()?null:"through "+r.groupName(one.group),who.name),role));
            }
            DesktopUi.add(body,DesktopUi.card(null,things));
        }
        // What can be done to the device itself, last: whether it is mine, and forgetting it.
        DesktopUi.gap(body,DesktopUi.L);
        JCheckBox isMine=DesktopUi.toggle("My device",who.mine);isMine.addActionListener(e->mine(who,isMine.isSelected()));
        JButton forget=app.button("Forget…",()->forget(who,true));forget.getAccessibleContext().setAccessibleName("Forget "+who.name);
        JPanel last=DesktopUi.column();DesktopUi.add(last,DesktopUi.switchRow("My device",isMine));DesktopUi.add(last,DesktopUi.row(DesktopUi.quiet("Off the list you pick from. Nothing is unshared."),forget));
        DesktopUi.add(body,DesktopUi.card(null,last));
    }

    /** This device: what it is called, its door, how notes travel. */
    private void self(JPanel body,Read r) {
        JPanel self=DesktopUi.column();DesktopUi.add(self,DesktopUi.person(Node.deviceHere(app.context),null));
        for(String line:r.here){DesktopUi.gap(self,4);DesktopUi.add(self,DesktopUi.spread(line));}
        DesktopUi.add(body,DesktopUi.card(null,self));
    }

    // ---- Share with -------------------------------------------------------------------------------------------

    /**
     * Share with: groups first, then people, the rights beside each name (the owner, 2026-10-05: "we should see first the
     * people and devices and then for each of them the rights we give them; for the group the rights could be defined at
     * the group level or at the individual group's members level"). A group's name folds open to who is in it, each with
     * As the group or rights of their own.
     */
    private void with(JPanel body,Read r) {
        boolean may=!r.give.isEmpty();
        if(!may){DesktopUi.add(body,DesktopUi.spread("Only its owner or an admin can share this. Here is who has it."));DesktopUi.gap(body,DesktopUi.M);}
        section(body,"Groups");
        JPanel groups=DesktopUi.column();
        withGroup(groups,r,Groups.MINE,Groups.MY_DEVICES,true);
        for(Groups.Group group:r.groups)withGroup(groups,r,group.id,group.name,false);
        DesktopUi.add(body,DesktopUi.card(null,groups));DesktopUi.gap(body,DesktopUi.L);
        section(body,"People");
        JPanel people=DesktopUi.column();int others=0;
        for(NoteStore.Contact one:r.contacts) {
            if(one.mine)continue;
            others++;
            // In a group: listed under it, where it folds open, and not again here.
            if(r.groupsOf(one.address).isEmpty())DesktopUi.add(people,DesktopUi.row(app.inkOnRound(DesktopUi.person(one.name,one.paired()?r.through.get(one.address):"Not paired yet"),one.name,one.address),standing(r,one,null)));
        }
        if(people.getComponentCount()==0)DesktopUi.add(body,DesktopUi.spread(r.contacts.isEmpty()?"No devices paired yet. Scan another device's code, or show them yours."
            :others>0?"Everybody else is in a group.":"Nobody else yet. Scan their code, or show them yours."));
        else DesktopUi.add(body,DesktopUi.card(null,people));
    }

    /** One group in Share with: its name, folding open to its people, and the group's rights on the thing, or Add. */
    private void withGroup(JPanel rows,Read r,String id,String name,boolean devices) {
        List<NoteStore.Contact> in=r.in.getOrDefault(id,List.of());Groups.Given given=r.given.get(id);boolean open=unfolded.contains(id);
        JButton fold=DesktopUi.quietButton((open?"▾  ":"▸  ")+Groups.counted(name,in.size(),devices),()->{if(!unfolded.remove(id))unfolded.add(id);draw();});
        fold.setForeground(DesktopUi.INK);fold.setFont(DesktopUi.BODY);fold.setMargin(new Insets(2,0,2,6));
        fold.getAccessibleContext().setAccessibleName((open?"Hide who is in ":"Show who is in ")+name);
        java.util.function.Consumer<Sharing.Level> choose=to->app.groupWork((to==Sharing.Level.GONE?"Removing ":"Sharing with ")+name+"…",
            ()->app.store.shareWithGroup(scope(),target.id,id,to),to==Sharing.Level.GONE?"Removed "+name:"Shared with "+name+": "+to.words(),this::draw);
        JComponent right=given!=null?(r.give.isEmpty()?Desktop.roleShown(given.level.words(),given.level.does()):app.roleButton(given.level,r.give,choose,name))
            :r.give.isEmpty()?null:adds(name,r.give,choose);
        if(rows.getComponentCount()>0)DesktopUi.gap(rows,2);
        DesktopUi.add(rows,DesktopUi.row(DesktopUi.actions(fold),right));
        if(!open)return;
        JPanel members=DesktopUi.column();members.setBorder(BorderFactory.createEmptyBorder(0,22,6,0));
        for(NoteStore.Contact one:in)DesktopUi.add(members,DesktopUi.row(app.inkOnRound(DesktopUi.person(one.name,null),one.name,one.address),standing(r,one,id)));
        if(in.isEmpty())DesktopUi.add(members,DesktopUi.quiet(devices?"No other device yet.":"Nobody in it yet."));
        DesktopUi.add(rows,members);
    }

    /** Add ▾, beside somebody or a group not on the thing yet: it drops to the rights this PC may give, and picking one shares at once. */
    private JButton adds(String name,List<Sharing.Level> give,java.util.function.Consumer<Sharing.Level> choose) {
        JButton add=new JButton(Groups.ADD+"  ▾");add.setFocusPainted(false);
        add.getAccessibleContext().setAccessibleName("Share with "+name);
        add.addActionListener(e->{
            JPopupMenu menu=new JPopupMenu();
            for(Sharing.Level level:give){JMenuItem one=new JMenuItem(DesktopUi.roleLine(level.words(),level.does()));one.getAccessibleContext().setAccessibleName(level.words()+". "+level.does());
                one.addActionListener(a->choose.accept(level));menu.add(one);}
            menu.show(add,0,add.getHeight());});
        return add;
    }

    /**
     * Where one person stands on the thing, beside their name: the rights they have, As the group under a group that gives
     * it to them, Removed where they were taken off by hand, or Add. Where this PC may change it, it drops down to the rights
     * it may give, As the group where a group of theirs has the thing, and Remove; where it may not, the words alone.
     *
     * @param under the group whose people this line is among; null under People
     */
    private JComponent standing(Read r,NoteStore.Contact who,String under) {
        NoteStore.Stands stands=r.stands.get(who.address);
        if(stands==null)return DesktopUi.quiet("Not paired yet");
        if(stands.how==NoteStore.Stands.OWNER)return Desktop.roleShown(Sharing.OWNER,Sharing.OWNER_DOES);
        if(stands.how==NoteStore.Stands.ABOVE)return Desktop.roleShown(stands.level.words(),stands.level.does()+" Through the folder it is in. Change this where it was shared.");
        String said=stands.how==NoteStore.Stands.OWN?stands.level.words():stands.how==NoteStore.Stands.REMOVED?Groups.REMOVED:stands.how==NoteStore.Stands.NONE?Groups.ADD
            :stands.group.equals(under)?Groups.AS_GROUP:"Through "+r.groupName(stands.group);
        if(!stands.may)return stands.level==null?DesktopUi.quiet(stands.how==NoteStore.Stands.REMOVED?said:" "):Desktop.roleShown(said,stands.level.does()+" Only its owner or an admin can change this.");
        // Whether a group of theirs has the thing: only then is there a group to follow.
        boolean follows=false;for(String group:r.groupsOf(who.address))follows|=r.given.containsKey(group);
        boolean canFollow=follows;
        JButton set=new JButton(said+"  ▾");set.setFocusPainted(false);
        if(stands.level!=null)set.setToolTipText(stands.level.does());
        set.getAccessibleContext().setAccessibleName(stands.has()?"What "+who.name+" may do: "+said:stands.how==NoteStore.Stands.REMOVED?who.name+" was removed":"Share with "+who.name);
        set.addActionListener(e->{
            JPopupMenu menu=new JPopupMenu();
            List<Sharing.Level> shown=new ArrayList<>(r.give);
            if(stands.how==NoteStore.Stands.OWN&&!shown.contains(stands.level)){shown.add(stands.level);shown.sort((a,b)->b.ordinal()-a.ordinal());}
            for(Sharing.Level level:shown) {
                boolean now=stands.how==NoteStore.Stands.OWN&&level==stands.level;
                JRadioButtonMenuItem one=new JRadioButtonMenuItem(DesktopUi.roleLine(level.words(),level.does()),now);
                one.setEnabled(r.give.contains(level)||now);one.getAccessibleContext().setAccessibleName(level.words()+". "+level.does());
                one.addActionListener(a->{if(!now)own(who,stands,level);});menu.add(one);
            }
            if(canFollow) {
                menu.addSeparator();boolean now=stands.how==NoteStore.Stands.GROUP;
                JRadioButtonMenuItem as=new JRadioButtonMenuItem(DesktopUi.roleLine(Groups.AS_GROUP,Groups.AS_GROUP_DOES),now);
                as.getAccessibleContext().setAccessibleName(Groups.AS_GROUP+". "+Groups.AS_GROUP_DOES);
                as.addActionListener(a->{if(!now)app.groupWork("Giving "+who.name+" the group's rights…",()->app.store.asTheGroup(scope(),target.id,who.address),who.name+" has the group's rights",this::draw);});
                menu.add(as);
            }
            if(stands.has()){menu.addSeparator();item(menu,"Remove",()->{
                if(DesktopUi.confirm(box,"Remove "+who.name+"?","They stop getting changes. Their copy stays on their device, and they are told.","Remove",true))own(who,stands,Sharing.Level.GONE);});}
            menu.show(set,0,set.getHeight());});
        return set;
    }

    /** Rights of their own for one person on the thing, or taken off it (GONE): saved, the box drawn again, then sent. */
    private void own(NoteStore.Contact who,NoteStore.Stands stands,Sharing.Level to) {
        boolean first=stands.rule==null||stands.rule.level==Sharing.Level.GONE,off=to==Sharing.Level.GONE;
        say(first?"Sharing with "+who.name+"…":off?"Removing "+who.name+"…":"Updating access…");
        app.disk.submit(()->{
            if(first)app.store.give(scope(),target.id,who.address,to,null);else app.store.decide(stands.rule,to,System.currentTimeMillis());
            return null;
        },done->{draw();
            app.network.submit(()->{Post.changed(app.context,app.store,app.keys,target.kind,target.id);Post.removedAgain(app.context,app.store,app.keys);return null;},
                v->{say(first?"Shared with "+who.name+": "+to.words()+". Waiting for delivery confirmation":off?"Removed "+who.name:"Access updated");app.refresh();},app::failed);},app::failed);
    }
}
