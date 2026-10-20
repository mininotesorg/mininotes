// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * The Agent on the PC (decisions 191, 192 and 213; see Assistant): one conversation that scrolls, what you asked on the right
 * and each answer a card on the left, newest at the bottom, the field to ask in under it all. A question is answered from the
 * help kept in the app; a task is shown as what it would do, with a button to do it, and done the way its menu does it. Nothing
 * leaves this PC but what a task sends, as its menu would send it. The Agent in the title bar, Menu → Agent…, or Ctrl+K. No
 * button closes it: a click outside it does, or Esc (the owner, 2026-10-18: "we dont need a close button on the agent window,
 * only clicking out closes it").
 *
 * <p>The owner, 2026-10-19: "make the agent conversation window fully scrolling, not a little answer box in it, so that we use
 * as much as possible the whole screen to display the conversation. We even should have a way to expand full screen the dialog
 * box", "the agent should propose: do you want me to bring you there", and "we have to make sure the agent keeps the
 * conversation for a couple of hours at least" (decision 213). So it is most of the screen's height, Full screen at its top
 * lays it over the whole screen, an answer about a place in the app ends with Take me there, and what was said is kept in the
 * notebook on this PC for three hours after the last message, through closing it and starting Mininotes again.
 */
final class DesktopAssistant {
    private final Desktop d;
    private final JPanel talk=DesktopUi.column();
    private final JTextField field=new JTextField();
    private JScrollPane scroll;
    private JDialog box;
    private JButton full;
    /** Where the box was before Full screen; null while it is not full screen. */
    private Rectangle before;
    /** What has been said, as it is kept. */
    private final Assistant.Conversation kept=new Assistant.Conversation();
    /** The one box, brought forward when asked for again. */
    private static DesktopAssistant open;

    private DesktopAssistant(Desktop d){this.d=d;}

    static void open(Desktop d) {
        if(open!=null&&open.box!=null&&open.box.isDisplayable()){open.box.toFront();open.field.requestFocusInWindow();return;}
        open=new DesktopAssistant(d);open.show();
    }
    /** The box as it is, for the tests. */
    static DesktopAssistant shown(){return open!=null&&open.box!=null&&open.box.isDisplayable()?open:null;}

    private void show() {
        JPanel body=new JPanel(new BorderLayout(0,DesktopUi.M));body.setOpaque(false);
        // What it is, first and staying there (the owner, 2026-10-18: "make it clear at the top of the agent dialog box that it
        // answers questions and does stuff, that everything is private in the app").
        JPanel about=DesktopUi.column();
        DesktopUi.add(about,DesktopUi.note(Say.t(Assistant.WHAT),DesktopUi.SPREAD,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f)));DesktopUi.gap(about,4);
        DesktopUi.add(about,DesktopUi.note(Say.t(Assistant.PRIVATE_PC),DesktopUi.SPREAD,DesktopUi.ACCENT,DesktopUi.BODY.deriveFont(Font.BOLD,12.5f)));
        body.add(about,BorderLayout.NORTH);
        // The conversation takes all the room there is.
        scroll=DesktopUi.scrolling(talk);body.add(scroll,BorderLayout.CENTER);
        JPanel bottom=DesktopUi.column();
        DesktopUi.add(bottom,suggestions());DesktopUi.gap(bottom,DesktopUi.S);
        field.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,Say.t("Ask, or say what to do: share Grocery with Carol"));
        field.setFont(DesktopUi.BODY.deriveFont(14f));field.getAccessibleContext().setAccessibleName(Say.t("Agent"));
        field.addActionListener(e->ask());
        JButton go=DesktopUi.primary("Ask",this::ask);
        JPanel line=new JPanel(new BorderLayout(DesktopUi.S,0));line.setOpaque(false);line.add(field,BorderLayout.CENTER);line.add(go,BorderLayout.EAST);
        DesktopUi.add(bottom,line);
        body.add(bottom,BorderLayout.SOUTH);
        box=DesktopUi.sheet(d.frame,"Agent",body,null,false);
        // At the right of its title: a new conversation, and the whole screen.
        JButton afresh=DesktopUi.quietButton(Assistant.AFRESH,this::afresh);afresh.setToolTipText(Say.t(Assistant.KEPT_SAID));
        full=DesktopUi.quietButton(Assistant.FULL,this::fullScreen);
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.RIGHT,DesktopUi.S,0));tools.setOpaque(false);tools.add(afresh);tools.add(full);
        if(box.getRootPane().getClientProperty("top") instanceof JPanel top)top.add(tools,BorderLayout.EAST);
        // No title bar and no close button: a click anywhere outside it closes it, as Esc does. A window it opened itself (a
        // code to scan, a menu) is not outside it.
        box.setUndecorated(true);box.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        box.getRootPane().setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));
        // A press of the mouse, not the keyboard's focus going elsewhere: a box that closed whenever focus moved closed by itself.
        java.awt.event.AWTEventListener outside=event->{
            if(!(event instanceof java.awt.event.MouseEvent m)||m.getID()!=java.awt.event.MouseEvent.MOUSE_PRESSED||!(m.getSource() instanceof Component c))return;
            Window at=SwingUtilities.getWindowAncestor(c);if(c instanceof Window w)at=w;
            for(Window w=at;w!=null;w=w.getOwner())if(w==box)return;
            box.dispose();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outside,java.awt.AWTEvent.MOUSE_EVENT_MASK);
        box.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){Toolkit.getDefaultToolkit().removeAWTEventListener(outside);}});
        box.getRootPane().putClientProperty("focus",field);
        box.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){if(open==DesktopAssistant.this)open=null;}});
        box.setMinimumSize(new Dimension(480,420));
        // Tall, most of the screen it is on, and a comfortable width for reading.
        Rectangle screen=usable(d.frame);
        int wide=Math.min(780,Math.max(480,screen.width-160)),tall=Math.max(420,Math.min(screen.height-24,Math.round(screen.height*0.88f)));
        box.setSize(wide,tall);box.setLocationRelativeTo(d.frame);
        java.awt.Point at=box.getLocation();
        box.setLocation(Math.max(screen.x,Math.min(at.x,screen.x+screen.width-wide)),screen.y+(screen.height-tall)/2);
        box.setVisible(true);
        remember();
    }

    /** The part of the screen a window may use, the taskbar left out: of the screen the main window is on. */
    private static Rectangle usable(Window w) {
        GraphicsConfiguration gc=w!=null&&w.getGraphicsConfiguration()!=null?w.getGraphicsConfiguration()
            :GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle all=gc.getBounds();Insets bars=Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(all.x+bars.left,all.y+bars.top,all.width-bars.left-bars.right,all.height-bars.top-bars.bottom);
    }

    /** Over the whole screen, and back to where it was. */
    void fullScreen() {
        if(before==null){before=box.getBounds();box.setBounds(usable(d.frame));full.setText(Say.t(Assistant.SMALLER));}
        else{box.setBounds(before);before=null;full.setText(Say.t(Assistant.FULL));}
        box.validate();down();
    }
    boolean isFullScreen(){return before!=null;}

    // ---- the conversation kept (decision 213) ------------------------------------------------------------------------

    /** Nothing is kept while a private space is up: whatever was said there is not written anywhere. */
    private boolean keeping(){return d.privately==null||!d.privately.showing();}

    /** What was said in the last three hours, read from the notebook and shown first, as it was. */
    private void remember() {
        if(!keeping())return;
        d.disk.submit(()->d.store.secret(Assistant.KEPT_AS),text->{
            Assistant.Conversation earlier=Assistant.Conversation.read(text,System.currentTimeMillis());
            if(earlier.isEmpty()||!box.isDisplayable())return;
            kept.before(earlier);
            JPanel shown=DesktopUi.column();
            for(List<Assistant.Line> turn:earlier.turns)replay(shown,turn);
            talk.add(shown,0);shown.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Asked before it was read, the new and the old are kept together.
            if(kept.turns.size()>earlier.turns.size())keep();
            down();
        },e->{});
    }

    /** Written to the notebook as it is now, off the screen's thread. */
    private void keep() {
        if(!keeping())return;
        String text=kept.isEmpty()?"":kept.write();
        d.disk.submit(()->{d.store.setSecret(Assistant.KEPT_AS,text);return null;},done->{},e->{});
    }

    /** A new conversation: what was said is gone, here and in the notebook. */
    void afresh() {
        kept.clear();talk.removeAll();talk.revalidate();talk.repaint();
        d.disk.submit(()->{d.store.setSecret(Assistant.KEPT_AS,"");return null;},done->{},e->{});
        field.requestFocusInWindow();
    }

    /** An exchange kept, shown again: what was asked, and what was said back. A task's buttons are not kept, only its words. */
    private void replay(JPanel into,List<Assistant.Line> turn) {
        String asked=null;
        for(Assistant.Line one:turn)if(one.kind()==Assistant.Kind.YOU){asked=one.words();break;}
        if(asked!=null){DesktopUi.gap(into,DesktopUi.S);DesktopUi.add(into,bubble(asked));}
        boolean answered=false;for(Assistant.Line one:turn)answered|=one.kind()!=Assistant.Kind.YOU;
        if(!answered)return;
        JPanel answer=DesktopUi.column();
        DesktopUi.gap(into,6);DesktopUi.add(into,DesktopUi.card(null,answer));
        for(Assistant.Line one:turn)switch(one.kind()) {
            case YOU -> {}
            case TITLE -> title(answer,one.words());
            case SAY -> say(answer,one.words());
            case QUIET -> quietly(answer,one.words());
            case THERE -> {try{there(answer,Assistant.Place.valueOf(one.words()));}catch(IllegalArgumentException unknown){/* a later version's place */}}
        }
    }

    // ---- asking ------------------------------------------------------------------------------------------------------

    /** The suggestions, a click each: one that needs words of its own puts its start in the field instead of asking it. */
    private JComponent suggestions() {
        List<JComponent> chips=new ArrayList<>();
        for(String one:Assistant.SUGGESTED) {
            JButton chip=DesktopUi.quietButton(Say.t(one),()->{
                if(one.endsWith("…")){field.setText(starting(one));field.requestFocusInWindow();}
                else ask(Say.t(one),one);
            });
            chip.setFont(DesktopUi.BODY.deriveFont(12.5f));
            chips.add(chip);
        }
        return DesktopUi.wrapping(0,chips.toArray(new JComponent[0]));
    }
    /** What a suggestion that needs words puts in the field: "Add  to my todo list" with the caret where the words go. */
    private String starting(String suggestion) {
        if(suggestion.startsWith("Add to my todo list")){String s=Say.t("Add {0} to my todo list","");SwingUtilities.invokeLater(()->field.setCaretPosition(Math.max(0,s.indexOf("  ")+1)));return s;}
        if(suggestion.startsWith("Share a note")){String s=Say.t("Share {0} with {1}","","");SwingUtilities.invokeLater(()->field.setCaretPosition(Math.max(0,s.indexOf("  ")+1)));return s;}
        return Say.t(suggestion).replace("…","");
    }

    /** What is in the field, asked: said back as the user's line, then answered. */
    void ask() {
        String said=field.getText().trim();
        if(said.isEmpty())return;
        field.setText("");
        ask(said);
    }
    void ask(String said){ask(said,said);}
    /** Shown as one thing and understood as another: a suggestion is shown in the language in use and read in English. */
    void ask(String said,String meant) {
        long now=System.currentTimeMillis();
        // Three hours after the last message, it starts afresh, even with the window open all along.
        if(kept.over(now))afresh();
        DesktopUi.gap(talk,DesktopUi.S);DesktopUi.add(talk,bubble(said));
        JPanel answer=exchange(kept.start(said,now));
        keep();
        answer(Assistant.understand(meant),answer);
        down();
    }

    /** What you asked, on the right, as a chat has it. */
    private JComponent bubble(String said) {
        Font font=DesktopUi.BODY.deriveFont(Font.BOLD,13.5f);
        int wide=Math.max(40,Math.min(420,talk.getFontMetrics(font).stringWidth(said)+8));
        DesktopUi.Text you=DesktopUi.note("",wide,DesktopUi.INK,font);you.setText(said);
        JPanel round=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DesktopUi.SHELF);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g.setColor(DesktopUi.LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);g.dispose();
            }
        };
        round.setOpaque(false);round.putClientProperty("ground",DesktopUi.SHELF);round.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));round.add(you);
        JPanel line=new JPanel(new BorderLayout()){
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };
        line.setOpaque(false);line.add(round,BorderLayout.EAST);
        return line;
    }
    /** The card an answer is said in, on the left: what is said in it is kept with its exchange. */
    private JPanel exchange(List<Assistant.Line> turn) {
        JPanel answer=DesktopUi.column();answer.putClientProperty("turn",turn);
        DesktopUi.gap(talk,6);DesktopUi.add(talk,DesktopUi.card(null,answer));
        return answer;
    }

    private void down() {
        talk.revalidate();talk.repaint();
        SwingUtilities.invokeLater(()->{JScrollBar bar=scroll.getVerticalScrollBar();bar.setValue(bar.getMaximum());});
    }
    /** What is said in an answer, kept with its exchange. */
    @SuppressWarnings("unchecked")
    private void record(JPanel in,Assistant.Kind kind,String words) {
        if(!(in.getClientProperty("turn") instanceof List<?> turn))return;
        kept.add((List<Assistant.Line>)turn,kind,words,System.currentTimeMillis());keep();
    }
    private void title(JPanel in,String words) {
        DesktopUi.Text title=DesktopUi.note("",DesktopUi.SPREAD,DesktopUi.INK,DesktopUi.BODY.deriveFont(Font.BOLD,13.5f));title.setText(words);
        DesktopUi.add(in,title);DesktopUi.gap(in,4);record(in,Assistant.Kind.TITLE,words);
    }
    private void say(JPanel in,String words){DesktopUi.add(in,DesktopUi.note(words,DesktopUi.SPREAD,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f)));DesktopUi.gap(in,6);in.revalidate();record(in,Assistant.Kind.SAY,words);down();}
    private void quietly(JPanel in,String words){DesktopUi.add(in,DesktopUi.note(words,DesktopUi.SPREAD,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));DesktopUi.gap(in,6);in.revalidate();record(in,Assistant.Kind.QUIET,words);down();}
    private void buttons(JPanel in,JComponent... them){DesktopUi.add(in,DesktopUi.wrapping(0,them));in.revalidate();down();}
    private void failed(JPanel in,Throwable e){String why=e==null||e.getMessage()==null?Say.t("Nothing was changed."):Say.t(e.getMessage());say(in,why);}

    /** The answer to what was understood, as cards and buttons in the answer's place. */
    private void answer(Assistant.Asked asked,JPanel in) {
        switch(asked.act()) {
            case NOTHING -> {}
            case HELP -> help(asked.what(),in);
            case WHAT_NEW -> whatIsNew(in);
            case TODO_SHOW -> todoShow(in);
            case TODO_ADD -> lineAdd(asked.what(),asked.who(),in);
            case SHARE -> share(asked,in);
            case ADD_CONTACT -> addSomeone(in);
            case OPEN -> openThing(asked.what(),in);
            case FIND -> find(asked.what(),in);
            case NEW_NOTE -> newNote(asked.what(),in);
            case DOCK -> dock(asked.what(),in);
            case FAVOURITE -> favourite(asked.what(),in);
        }
    }

    // ---- questions -------------------------------------------------------------------------------------------------

    private void help(String question,JPanel in) {
        List<Assistant.Help> all=Assistant.help(Say.language());
        List<Assistant.Help> found=Assistant.lookUp(question,all,3);
        if(found.isEmpty()){say(in,Say.t("I do not have an answer for that yet. Try other words, or one of the suggestions below."));return;}
        helpCard(found.get(0),question,in);
        if(found.size()>1) {
            List<JComponent> more=new ArrayList<>();
            for(Assistant.Help one:found.subList(1,found.size()))more.add(DesktopUi.quietButton(one.title()+"  ›",()->{helpCard(one,question,exchange(kept.start(null,System.currentTimeMillis())));down();}));
            quietly(in,Say.t("Also:"));buttons(in,more.toArray(new JComponent[0]));
        }
    }
    private void helpCard(Assistant.Help one,String question,JPanel in) {
        title(in,one.title());
        for(String paragraph:one.answer().split("\n\n"))say(in,paragraph);
        JButton doIt=doer(one.act());
        if(doIt!=null)buttons(in,doIt);
        Assistant.Place place=Assistant.place(one,question);
        if(place!=null)there(in,place);
    }
    /** The button an answer offers, to do at once what it explains. */
    private JButton doer(Assistant.Act act) {
        if(act==null)return null;
        return switch(act) {
            case WHAT_NEW -> DesktopUi.button("Show what is new",()->ask(Say.t("What is new?"),"What is new?"));
            case TODO_SHOW -> DesktopUi.button("Show my todo list",()->ask(Say.t("What is on my todo list?"),"What is on my todo list?"));
            case ADD_CONTACT -> DesktopUi.button("Add someone…",()->ask(Say.t("Add someone"),"Add someone"));
            case SHARE -> DesktopUi.button("Share a note…",()->{field.setText(starting("Share a note…"));field.requestFocusInWindow();});
            case FIND -> DesktopUi.button("Search…",()->{d.goHome();d.home.focusSearch();});
            default -> null;
        };
    }

    // ---- Take me there (decision 213) ------------------------------------------------------------------------------

    /** Take me there, under an answer about a place this PC has: the box goes, and the place opens, as its menu opens it. */
    private void there(JPanel in,Assistant.Place place) {
        Runnable go=way(place);
        if(go==null)return;
        record(in,Assistant.Kind.THERE,place.name());
        JButton b=DesktopUi.button(Assistant.THERE,()->{box.dispose();go.run();});
        b.setToolTipText(place.said());
        buttons(in,b);
    }
    /** How each place is opened on the PC: My profile at its part, Settings at its heading, the others as their menus open them. */
    Runnable way(Assistant.Place place) {
        return switch(place) {
            case PROFILE -> ()->DesktopProfile.open(d);
            case INTEGRITAS -> ()->DesktopProfile.open(d,"Integritas");
            case PARLONS -> ()->DesktopProfile.open(d,"Your Parlons! address");
            case TEXT_SIZE -> ()->DesktopProfile.open(d,"Text size");
            case DESCRIBE -> ()->DesktopProfile.open(d,"Describe what each thing does");
            case BACKUP -> ()->DesktopProfile.open(d,"Backup");
            case PEOPLE -> ()->d.people(null);
            case SECURITY -> ()->settingsAt("Security");
            case LANGUAGE -> ()->settingsAt("Language");
            case TEMP -> ()->settingsAt("Temp: things stay");
            case SHOW_ON_HOME -> ()->settingsAt("Show on Home");
            case SETTINGS -> ()->settingsAt(null);
            case STAMPED -> ()->DesktopStamp.list(d,d.frame);
            case SENT_FILES -> ()->DesktopDrops.sentFiles(d);
            case BIN -> ()->onHome(DesktopHome.binPlace(0));
            case ARCHIVE -> ()->onHome(DesktopHome.archivePlace(0));
            case SHARED_WITH_ME -> ()->onHome(DesktopHome.sharedPlace());
        };
    }
    /** One of Home's places, its card opened over Home. */
    private void onHome(NoteStore.Branch place){d.save(()->{d.showHome();d.home.folder.openPlace(place);});}
    /**
     * Settings, scrolled to a heading or a line of it. Settings waits until it is closed, so what finds the heading is set
     * going first, and looks for it once the window is up.
     */
    private void settingsAt(String heading) {
        if(heading!=null) {
            String want=Say.t(heading),title=Say.t("Settings");int[] tries={0};
            javax.swing.Timer look=new javax.swing.Timer(60,null);
            look.addActionListener(e->{
                if(++tries[0]>80){look.stop();return;}
                for(Window w:Window.getWindows()) {
                    if(!(w instanceof JDialog dialog)||!dialog.isShowing()||!title.equals(dialog.getTitle()))continue;
                    JComponent found=named(dialog.getContentPane(),want);
                    JScrollPane s=found==null?null:(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,found);
                    if(s==null||!(s.getViewport().getView() instanceof JComponent page))continue;
                    look.stop();DesktopProfile.showAt(s,page,found,null);return;
                }
            });
            look.start();
        }
        DesktopSettings.open(d);
    }
    /** The first text in a window that says these words. */
    private static JComponent named(Container in,String words) {
        for(Component c:in.getComponents()) {
            if(c instanceof javax.swing.text.JTextComponent t&&words.equals(t.getText()))return t;
            if(c instanceof JLabel l&&words.equals(l.getText()))return l;
            if(c instanceof Container k){JComponent deeper=named(k,words);if(deeper!=null)return deeper;}
        }
        return null;
    }

    // ---- tasks -----------------------------------------------------------------------------------------------------

    private void whatIsNew(JPanel in) {
        d.disk.submit(()->d.store.whatIsNew(),news->{
            if(news.isEmpty()){say(in,Say.t("Nothing is new. Everything has been seen."));return;}
            say(in,news.size()==1?Say.t("One thing is new:"):Say.t("{0} things are new:",String.valueOf(news.size())));
            List<JComponent> each=new ArrayList<>();
            for(NoteStore.Branch one:news.subList(0,Math.min(8,news.size())))each.add(DesktopUi.quietButton(DesktopHome.named(one)+"  ›",()->d.openThing(one)));
            buttons(in,each.toArray(new JComponent[0]));
            buttons(in,DesktopUi.button("Open New",d::whatIsNew));
        },e->failed(in,e));
    }

    private void todoShow(JPanel in) {
        d.disk.submit(()->{NoteStore.Note todo=Assistant.todo(d.store);return todo;},todo->{
            if(todo==null){say(in,Assistant.noTodo());buttons(in,DesktopUi.primary("Make it",()->make(Say.t("Todo"),"",in)));return;}
            List<String> lines=Assistant.lines(todo.body);
            if(lines.isEmpty())say(in,Say.t("“{0}” is empty.",todo.heading()));
            else {
                say(in,Say.t("On “{0}”:",todo.heading()));
                StringBuilder all=new StringBuilder();for(String one:lines)all.append(all.length()==0?"":"\n").append("•  ").append(one);
                say(in,all.toString());
            }
            buttons(in,DesktopUi.button("Open it",()->d.open(todo.id)),DesktopUi.button("Add a line…",()->{field.setText(starting("Add to my todo list…"));field.requestFocusInWindow();}));
        },e->failed(in,e));
    }

    /** A line for the to-do list, or for the note named: asked first, then written as a page writes it, then sent. */
    private void lineAdd(String line,String noteWords,JPanel in) {
        if(line==null||line.isBlank()){say(in,Say.t("What should I add? For example: add buy bread to my todo list."));return;}
        d.disk.submit(()->{
            if(noteWords==null||noteWords.isBlank())return Assistant.todo(d.store);
            NoteStore.Branch found=Assistant.thing(d.store,noteWords);
            return found==null||found.kind!=NoteStore.Branch.Kind.PAGE?null:d.store.get(found.id);
        },note->{
            if(note==null&&noteWords!=null&&!noteWords.isBlank()){say(in,Assistant.notFound(noteWords));return;}
            if(note==null){say(in,Assistant.noTodo());buttons(in,DesktopUi.primary("Make it",()->make(Say.t("Todo"),line,in)));return;}
            if(Assistant.readOnly(note)){say(in,Say.t("You can only read “{0}”.",note.heading()));return;}
            say(in,Assistant.addAsk(line,note.heading()));
            JButton[] add={null};
            add[0]=DesktopUi.primary("Add",()->{add[0].setEnabled(false);
                d.disk.submit(()->Assistant.addLine(d.store,note.id,line),written->{
                    say(in,Assistant.added(written.heading()));sent(written.id);},e->failed(in,e));});
            buttons(in,add[0]);
        },e->failed(in,e));
    }

    /** Written here: the open page follows, and it goes as a page's writing goes. */
    private void sent(String id) {
        d.arrived(new Post.Landed(null,null,id));
        if(!d.offline)d.network.submit(()->Post.send(d.context,d.store,d.keys,NoteStore.Branch.Kind.PAGE,id),done->d.refresh(),e->{});
    }

    /** A new note, the to-do list or one asked for, on Home, with what it should say. */
    private void make(String title,String body,JPanel in) {
        d.disk.submit(()->Assistant.make(d.store,title,body),made->{
            say(in,Say.t("Made “{0}” on Home.",made.heading()));d.refresh();
            buttons(in,DesktopUi.button("Open it",()->d.open(made.id)));
        },e->failed(in,e));
    }

    private void share(Assistant.Asked asked,JPanel in) {
        d.disk.submit(()->new Object[]{Assistant.thing(d.store,asked.what()),Assistant.person(d.store,asked.who()),Assistant.people(d.store)},got->{
            NoteStore.Branch thing=(NoteStore.Branch)got[0];NoteStore.Contact who=(NoteStore.Contact)got[1];
            @SuppressWarnings("unchecked") List<String> people=(List<String>)got[2];
            if(thing==null){say(in,Assistant.notFound(asked.what()));return;}
            if(who==null){say(in,Assistant.nobody(asked.who(),people));if(people.isEmpty())buttons(in,DesktopUi.button("Add someone…",()->ask(Say.t("Add someone"),"Add someone")));return;}
            String name=d.store.nameFor(who.address);
            boolean[] read={asked.readOnly()};
            DesktopUi.Text question=DesktopUi.note(Assistant.shareAsk(thing.name,name,read[0]),DesktopUi.SPREAD,DesktopUi.INK,DesktopUi.BODY.deriveFont(13.5f));
            DesktopUi.add(in,question);DesktopUi.gap(in,6);
            JButton[] go={null,null};
            go[1]=DesktopUi.button(read[0]?"Can write instead":"Can read instead",()->{read[0]=!read[0];question.setText(Assistant.shareAsk(thing.name,name,read[0]));
                go[1].setText(Say.t(read[0]?"Can write instead":"Can read instead"));});
            go[0]=DesktopUi.primary("Share",()->{go[0].setEnabled(false);go[1].setEnabled(false);
                d.disk.submit(()->{Assistant.share(d.store,thing,who,read[0]);return null;},done->{
                    say(in,Assistant.shared(name));
                    d.network.submit(()->{Post.changed(d.context,d.store,d.keys,thing.kind,thing.id);return Post.send(d.context,d.store,d.keys,thing.kind,thing.id);},v->d.refresh(),e->{});
                },e->failed(in,e));});
            buttons(in,go[0],go[1]);
        },e->failed(in,e));
    }

    private void addSomeone(JPanel in) {
        say(in,Say.t("To add someone, one of you shows a code and the other scans it. Scan their code, or paste the link they sent you; or show them your code."));
        buttons(in,DesktopUi.primary("Scan their code or paste their link…",()->d.scanCode(null)),
            DesktopUi.button("People and devices",()->DesktopPeople.open(d)));
    }

    private void openThing(String words,JPanel in) {
        // A place of the app's, "open the bin", "open my profile", is opened as its menu opens it.
        Assistant.Place place=Assistant.placeNamed(words);
        Runnable go=place==null?null:way(place);
        if(go!=null){say(in,Say.t("Opening “{0}”.",place.said()));box.dispose();go.run();return;}
        d.disk.submit(()->Assistant.thing(d.store,words),thing->{
            if(thing==null){say(in,Assistant.notFound(words));find(words,in);return;}
            say(in,Say.t("Opening “{0}”.",thing.name));d.openThing(thing);
        },e->failed(in,e));
    }

    private void find(String words,JPanel in) {
        d.disk.submit(()->d.store.lookingEverywhere(words),found->{
            if(found.isEmpty()){say(in,Say.t("Nothing found for “{0}”.",words));return;}
            say(in,found.size()==1?Say.t("Found one:"):Say.t("Found {0}:",String.valueOf(found.size())));
            List<JComponent> each=new ArrayList<>();
            for(NoteStore.Branch one:found.subList(0,Math.min(8,found.size())))each.add(DesktopUi.quietButton(DesktopHome.named(one)+"  ›",()->d.openThing(one)));
            buttons(in,each.toArray(new JComponent[0]));
        },e->failed(in,e));
    }

    private void newNote(String title,JPanel in){make(title,"",in);}

    private void dock(String words,JPanel in) {
        d.disk.submit(()->{NoteStore.Branch thing=Assistant.thing(d.store,words);if(thing!=null)d.store.toDockEnd(thing.kind,thing.id);return thing;},thing->{
            if(thing==null){say(in,Assistant.notFound(words));return;}
            say(in,Say.t("“{0}” is in the dock.",thing.name));d.refresh();
        },e->failed(in,e));
    }

    private void favourite(String words,JPanel in) {
        d.disk.submit(()->{NoteStore.Branch thing=Assistant.thing(d.store,words);if(thing!=null)d.store.keepToHand(thing.kind,thing.id,true);return thing;},thing->{
            if(thing==null){say(in,Assistant.notFound(words));return;}
            say(in,Say.t("“{0}” is a favourite, and in the dock.",thing.name));d.refresh();
        },e->failed(in,e));
    }

    /** The field, for the tests. */
    JTextField field(){return field;}
    JPanel talk(){return talk;}
    JDialog box(){return box;}
    Assistant.Conversation kept(){return kept;}
}
