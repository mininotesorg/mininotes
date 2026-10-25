// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * The one path after a code is scanned or pasted (the owner, 2026-10-25: "once scanned, we should be invited to define what
 * it is, own device or not, put this in a group, what to share and so on there must be a clear path as unified as possible
 * always"; decision 258). One box, its steps drawn in turn, each with one filled button: what it is, a group (someone else
 * only: a device of mine is in My devices), what to share, Nothing yet first, and done. ‹ goes back a step, as in People and
 * devices. What is kept and sent is Joining's, the same on the phone.
 */
final class DesktopJoining {
    private static final int WHAT=0,GROUP=1,SHARE=2,DONE=3;

    private final Desktop app;private final Pairing.Said code;private final String digits;private final NoteStore.Branch shareTarget;
    /** The box the scan was opened from, opened again when this one closes, done or not; null for none. */
    private final Runnable back;
    private final Joining.Choice choice;
    private final JPanel holder=new JPanel(new BorderLayout()),foot=new JPanel(new BorderLayout());
    private JDialog box;private int step=WHAT;
    /** Read off the event thread when the box opens: the groups there are, and what may be shared from Home. */
    private List<Groups.Group> groups=List.of();private List<NoteStore.Branch> things=List.of();
    /** Choose notes or folders, picked on the share step for someone else. */
    private boolean choosing;
    /** At the last step: still working, and how it ended. */
    private boolean working;private String ended,also;
    /** Closed for the helpers' box, which goes on with it: nothing is opened again behind it. */
    private boolean gone;

    private DesktopJoining(Desktop app,Pairing.Said code,String digits,NoteStore.Branch shareTarget,Runnable back) {
        this.app=app;this.code=code;this.digits=digits==null?"":digits;this.shareTarget=shareTarget;this.back=back;
        choice=Joining.Choice.from(code);
        holder.setOpaque(false);foot.setOpaque(false);
        // From a Share box: that thing is chosen for them already, the box it came from shown again at the end.
        if(shareTarget!=null&&Joining.scopeOf(shareTarget)!=null){choice.mine=false;choice.things.add(shareTarget);choosing=true;}
    }

    /**
     * The path for a code read, up at once on its first step; the groups and Home's things are read meanwhile.
     *
     * @param digits      the six digits to check, for a code pasted rather than scanned; empty for a camera's
     * @param shareTarget the thing whose Share box the code was scanned from, or null
     * @param back        the box the scan was opened from, opened again when this closes; null for none
     */
    static void open(Desktop app,Pairing.Said code,String digits,NoteStore.Branch shareTarget,Runnable back) {
        DesktopJoining path=new DesktopJoining(app,code,digits,shareTarget,back);
        path.draw();
        app.disk.submit(()->new Object[]{app.store.groups(),Joining.shareable(app.store)},read->{
            @SuppressWarnings("unchecked") List<Groups.Group> g=(List<Groups.Group>)read[0];
            @SuppressWarnings("unchecked") List<NoteStore.Branch> t=(List<NoteStore.Branch>)read[1];
            path.groups=g;path.things=Joining.withChosen(t,path.choice.things);
            if(path.box!=null&&path.box.isDisplayable()&&path.step!=WHAT&&path.step!=DONE)path.draw();
        },app::failed);
        DesktopUi.show(path.box,520,560,760);
    }

    /** One title for the steps, the question the box asks; each step's own question is its heading. Done, their name. */
    private String title(int at) {
        if(at==DONE)return code.name;
        return Joining.offers(code)?Say.t("Accept from {0}?",code.name):Say.t("Add {0}?",code.name);
    }
    private int previous(){return step==SHARE&&choice.mine?WHAT:step-1;}
    private void back(){if(step==WHAT||step==DONE){box.dispose();return;}step=previous();draw();}
    private void next(){step=step==WHAT&&choice.mine?SHARE:step+1;draw();}

    private void draw() {
        JPanel body=DesktopUi.column();JComponent primary;
        // Whoever it is, at the top of every step, as a person is shown: their round and their name.
        String under=Joining.offers(code)?Sharing.shown(code.offer)+"  ·  "+Say.t(code.level.words()):null;
        if(step==DONE)under=working?Say.t("Adding {0}…",code.name):"✓  "+Joining.done(code.name,choice.mine);
        JPanel who=DesktopUi.person(code.name,under);who.setName("joiningWho");
        DesktopUi.add(body,DesktopUi.card(null,who));DesktopUi.gap(body,DesktopUi.L);
        switch(step) {
            case WHAT: what(body);primary=DesktopUi.primary("Next",this::next);break;
            case GROUP: group(body);primary=DesktopUi.primary("Next",this::next);break;
            case SHARE: share(body);primary=DesktopUi.primary(Joining.offers(code)?"Accept":"Add",this::finish);break;
            default: {
                if(ended!=null){DesktopUi.add(body,DesktopUi.spread(ended));}
                if(also!=null){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.spread(also));}
                primary=DesktopUi.primary("Done",box::dispose);
                primary.setEnabled(!working);
            }
        }
        primary.setName("joiningNext");
        boolean first=box==null;
        if(first) {
            box=DesktopUi.sheet(app.frame,title(step),holder,foot,true);box.setName("joining");box.getRootPane().putClientProperty("joining",this);
            DesktopUi.leaves(box,this::back);
            // Closed, done or not: back to the Share box it came from, or the box the scan was opened from.
            box.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){
                if(gone)return;
                if(shareTarget!=null)app.people(shareTarget);else if(back!=null)back.run();
            }});
        }
        DesktopUi.head(box,step==WHAT||step==DONE?null:Say.t("Back"),this::back,title(step),null);
        holder.removeAll();holder.add(DesktopUi.scrolling(body));
        foot.removeAll();foot.add(DesktopUi.footer(primary),BorderLayout.EAST);
        box.getRootPane().setDefaultButton(primary instanceof JButton b&&b.isEnabled()?b:null);
        holder.revalidate();holder.repaint();foot.revalidate();foot.repaint();
        if(!first)SwingUtilities.invokeLater(primary::requestFocusInWindow);
    }

    /** A choice among a few, its name over what it means, as Add my device's box has them. */
    private JRadioButton pick(JPanel body,ButtonGroup group,String said,String does,boolean on,Runnable chosen) {
        JRadioButton one=new JRadioButton(Say.t(said),on);one.setOpaque(false);
        one.setFont(DesktopUi.BODY.deriveFont(Font.BOLD));one.setForeground(DesktopUi.INK);
        one.getAccessibleContext().setAccessibleName(does==null?Say.t(said):Say.t(said)+". "+Say.t(does));
        one.addActionListener(e->chosen.run());group.add(one);DesktopUi.add(body,one);
        if(does!=null) {
            DesktopUi.Text under=DesktopUi.note(does,380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));under.setBorder(BorderFactory.createEmptyBorder(0,26,0,0));
            under.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){one.doClick();}});
            DesktopUi.add(body,under);
        }
        DesktopUi.gap(body,8);
        return one;
    }

    /** What it is: one of my devices or someone else, as the code said where it said. */
    private void what(JPanel body) {
        DesktopUi.add(body,DesktopUi.heading("What is it?"));DesktopUi.gap(body,8);
        ButtonGroup group=new ButtonGroup();
        pick(body,group,"One of my devices",null,choice.mine,()->choice.mine=true).setName("joiningMine");
        pick(body,group,"Someone else",null,!choice.mine,()->choice.mine=false).setName("joiningSomeone");
        // What they share arrives on Home, wherever the + was that took it: said before it is accepted.
        if(Joining.offers(code)){DesktopUi.gap(body,DesktopUi.S);DesktopUi.add(body,DesktopUi.quiet("It will appear on Home when it arrives."));}
        // A code read from a picture or a link, not a camera: what a paste cannot prove said, and the digits to check.
        if(!digits.isEmpty()) {
            DesktopUi.gap(body,DesktopUi.M);
            DesktopUi.add(body,DesktopUi.note("Pasted, not scanned. To be sure nobody changed it on the way, open People on both devices afterwards: each shows six digits for the other, and they must be the same. Here:"));
            DesktopUi.gap(body,DesktopUi.S);JLabel shown=new JLabel(digits);shown.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,28f));shown.setForeground(DesktopUi.INK);DesktopUi.add(body,shown);
        }
    }

    /** Someone else's group: none, one there is, or one made here. */
    private void group(JPanel body) {
        DesktopUi.add(body,DesktopUi.heading(Say.t("Put {0} in a group?",code.name)));DesktopUi.gap(body,8);
        ButtonGroup group=new ButtonGroup();
        boolean none=choice.group.isEmpty()&&choice.newGroup.isEmpty();
        pick(body,group,"No group",null,none,()->{choice.group="";choice.newGroup="";}).setName("joiningNoGroup");
        for(Groups.Group one:groups)pick(body,group,one.name,null,one.id.equals(choice.group),()->{choice.group=one.id;choice.newGroup="";});
        if(!choice.newGroup.isEmpty())pick(body,group,choice.newGroup,null,true,()->choice.group="");
        DesktopUi.gap(body,4);
        JButton make=DesktopUi.quietButton("New group…",()->{
            String name=DesktopUi.ask(box,"New group","Name, like Friends or Colleagues",null);
            if(name==null||name.trim().isEmpty())return;
            choice.newGroup=name.trim();choice.group="";draw();
        });
        make.setName("joiningNewGroup");
        DesktopUi.add(body,DesktopUi.actions(make));
    }

    /** What it gets: for a device of mine, everything or nothing yet; for someone else, nothing yet or notes and folders chosen. */
    private void share(JPanel body) {
        DesktopUi.add(body,DesktopUi.heading(choice.mine?"What it gets":Say.t("What {0} gets",code.name)));DesktopUi.gap(body,8);
        ButtonGroup group=new ButtonGroup();
        if(choice.mine) {
            for(Sharing.Level level:Arrays.asList(null,Sharing.Level.WRITE,Sharing.Level.READ))
                pick(body,group,level==null?Sharing.NOTHING_YET:level.words(),Sharing.everythingDoes(level),choice.everything==level,()->choice.everything=level);
            return;
        }
        pick(body,group,Sharing.NOTHING_YET,null,!choosing,()->{choosing=false;draw();}).setName("joiningNothing");
        pick(body,group,"Choose notes or folders",null,choosing,()->{choosing=true;draw();}).setName("joiningChoose");
        // Taken at Add only while it is chosen; turned back to Nothing yet, nothing is given.
        if(!choosing){choice.things.clear();return;}
        if(things.isEmpty()){DesktopUi.add(body,DesktopUi.spread("Nothing on Home to share yet."));return;}
        JPanel lines=DesktopUi.column();
        for(NoteStore.Branch one:things) {
            boolean on=choice.things.stream().anyMatch(t->t.id.equals(one.id));
            JCheckBox flick=DesktopUi.toggle(one.name,on);
            flick.addActionListener(e->{choice.things.removeIf(t->t.id.equals(one.id));if(flick.isSelected())choice.things.add(one);});
            String name=one.name==null||one.name.isBlank()?Say.t("Untitled"):DesktopMenus.shortly(one.name);
            JPanel row=DesktopUi.switchRow(name,flick);row.setName("joiningThing "+one.id);
            DesktopUi.add(lines,row);
        }
        DesktopUi.add(body,DesktopUi.card(null,lines));
        DesktopUi.gap(body,DesktopUi.S);
        // What they may do with what is chosen: one drop-down for all of it, Can write first, as a Share box offers.
        JButton role=new JButton(Say.t(choice.level.words())+"  ▾");role.setFocusPainted(false);role.setName("joiningRole");
        role.setToolTipText(Say.t(choice.level.does()));
        role.addActionListener(e->{
            JPopupMenu menu=new JPopupMenu();
            for(Sharing.Level level:List.of(Sharing.Level.WRITE,Sharing.Level.READ)) {
                JRadioButtonMenuItem one=new JRadioButtonMenuItem(DesktopUi.roleLine(level.words(),level.does()),level==choice.level);
                one.addActionListener(a->{choice.level=level;draw();});menu.add(one);
            }
            menu.show(role,0,role.getHeight());
        });
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("Rights"),role));
    }

    /** Add, or Accept: the last step drawn at once, saying it is adding, and the work done off this thread (see Desktop.joined). */
    private void finish() {
        if(!choice.mine&&!choosing)choice.things.clear();
        working=true;ended=null;also=null;step=DONE;draw();
        app.joined(code,choice,(said,more)->{
            working=false;ended=said;also=more;
            if(box.isDisplayable())draw();
        },()->{gone=true;if(box.isDisplayable())box.dispose();});
    }

    /** For the pictures (DesktopConnectGalleryTest): the last step as it ends, drawn without pairing or sending anything. */
    void endedForPicture(boolean mine,String said,String more){choice.mine=mine;working=false;ended=said;also=more;step=DONE;draw();}
}
