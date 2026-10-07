package org.mininotes.android;

import java.awt.*;
import javax.swing.*;

/**
 * Every switch in one window. Whatever can be turned on or off - the lock and its ways in, updating by
 * itself, listening while the window is closed, the tree - is here, so nobody has to hunt for it.
 */
final class DesktopSettings {
    private DesktopSettings(){}

    /**
     * The six marks, drawn as they are drawn everywhere else, each with one short line: the same lines as the
     * phone's (SyncMark.meaning), and the round a person wears under a title, with its dot.
     */
    static JPanel legend() {
        JPanel all=DesktopUi.column();
        for(SyncMark one:new SyncMark[]{SyncMark.HERE,SyncMark.WAITING,SyncMark.SENT,SyncMark.GONE,SyncMark.PAUSED,SyncMark.STUCK}) {
            JPanel line=new JPanel(new BorderLayout(12,0));line.setOpaque(false);line.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));
            JLabel mark=new JLabel(new DesktopMark(one,20,false));mark.setVerticalAlignment(SwingConstants.TOP);
            mark.getAccessibleContext().setAccessibleName(DesktopMark.said(one));
            line.add(mark,BorderLayout.WEST);line.add(DesktopUi.note(one.meaning(),340,DesktopUi.INK,DesktopUi.BODY));
            DesktopUi.add(all,line);
        }
        DesktopUi.gap(all,DesktopUi.S);
        DesktopUi.add(all,DesktopUi.note("After the mark, a round for each person the note reaches: its dot is where they stand. Click a round to see who, and where.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        return all;
    }

    static void open(Desktop app) {
        JDialog[] box={null};
        JPanel body=DesktopUi.column();

        // Security first: it is the one that protects everything else.
        DesktopUi.add(body,DesktopUi.heading("Security"));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopLock.panel(app,box));
        DesktopUi.gap(body,DesktopUi.L);

        // Sync: whether notes go by themselves, and how soon after the writing stops.
        DesktopUi.add(body,DesktopUi.heading("Sync"));DesktopUi.gap(body,DesktopUi.S);
        int[] waits={NoteStore.RIGHT_AWAY,3,10,30,120};String[] names={"Right away","After 3 seconds","After 10 seconds","After 30 seconds","After 2 minutes"};
        int now=app.syncAfter();
        JCheckBox syncing=DesktopUi.toggle("Sync automatically",now>=0);
        JComboBox<String> after=new JComboBox<>(names);int at=2;for(int i=0;i<waits.length;i++)if(waits[i]==now)at=i;after.setSelectedIndex(at);after.setEnabled(now>=0);
        syncing.addActionListener(e->{after.setEnabled(syncing.isSelected());app.setSyncAfter(syncing.isSelected()?waits[after.getSelectedIndex()]:NoteStore.WHEN_ASKED);});
        after.addActionListener(e->{if(syncing.isSelected())app.setSyncAfter(waits[after.getSelectedIndex()]);});
        JPanel afterHolder=new JPanel(new GridBagLayout());afterHolder.setOpaque(false);afterHolder.add(after);
        JPanel sync=DesktopUi.column();sync.add(DesktopUi.switchRow("Sync automatically",syncing));
        sync.add(DesktopUi.row(DesktopUi.body("Send changes"),afterHolder));
        DesktopUi.add(sync,DesktopUi.note(NoteStore.RIGHT_AWAY_COSTS,380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));DesktopUi.gap(sync,DesktopUi.S);
        DesktopUi.add(sync,DesktopUi.note("Off, a note goes when you click its mark under the title, or Sync now. A note or a folder can also keep a timing of its own, in its Share box.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.add(body,DesktopUi.card(null,sync));
        DesktopUi.gap(body,DesktopUi.L);

        // The marks say where a thing stands without words, so the words are kept here, once, for whoever asks.
        DesktopUi.add(body,DesktopUi.heading("What the marks mean"));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.card(null,legend()));
        DesktopUi.gap(body,DesktopUi.L);

        // How notes travel: only between the owner's devices, or also through helpers - the relays below it.
        DesktopUi.add(body,DesktopUi.heading("How notes travel"));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.card(null,travel(app)));
        DesktopUi.gap(body,DesktopUi.L);

        // Direct: the devices reach this PC without a relay, once Windows lets them in.
        DesktopUi.add(body,DesktopUi.heading("Direct connections"));DesktopUi.gap(body,DesktopUi.S);
        JCheckBox direct=DesktopUi.toggle("Let devices reach this PC directly",false);direct.setEnabled(false);
        DesktopUi.Text directSaid=DesktopUi.note("Asking Windows…",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f));
        JPanel directCard=DesktopUi.column();directCard.add(DesktopUi.switchRow("Let devices reach this PC directly",direct));
        DesktopUi.add(directCard,directSaid);
        // What this PC holds for the owner's phones until they collect it (see Home): one line, only while sharing runs.
        DesktopUi.Text homeSaid=DesktopUi.note(" ",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f));
        DesktopUi.gap(directCard,DesktopUi.S);DesktopUi.add(directCard,homeSaid);
        if(!app.offline)app.connectivity.submit(Node::home,line->homeSaid.setText(line.isEmpty()?" ":line),e->{});
        DesktopUi.add(body,DesktopUi.card(null,directCard));
        DesktopUi.gap(body,DesktopUi.L);
        boolean[] quiet={false};
        java.util.function.Consumer<DesktopFirewall.State> shown=state->{
            quiet[0]=true;direct.setSelected(state.allowed());quiet[0]=false;direct.setEnabled(true);
            directSaid.setText(!state.allowed()
                ?"Off: Windows keeps other devices out, so yours cannot reach this PC directly. On, your devices at home (and, when your router allows it, from anywhere) send to this PC straight. Windows asks your permission."
                :state.publicHere()
                    ?"On, but Windows calls this network public, so it stays closed here. It opens on networks set as private, like your home."
                    :"On: your devices reach this PC without a relay at home, and from outside when your router opens a port. Only messages sealed to this PC are accepted.");
        };
        app.connectivity.submit(DesktopFirewall::read,shown::accept,e->directSaid.setText("Windows did not say whether it lets devices in."));
        direct.addActionListener(e->{
            if(quiet[0])return;boolean want=direct.isSelected();
            direct.setEnabled(false);directSaid.setText(want?"Waiting for your answer in Windows' permission box…":"Closing it again. Windows asks your permission…");
            app.connectivity.submit(()->DesktopFirewall.set(want),shown::accept,failure->{
                directSaid.setText(failure.getMessage()==null?"Nothing was changed.":failure.getMessage());
                app.connectivity.submit(DesktopFirewall::read,shown::accept,none->direct.setEnabled(true));
            });
        });

        // Updates.
        DesktopUi.add(body,DesktopUi.heading("Updates"));DesktopUi.gap(body,DesktopUi.S);
        JCheckBox auto=DesktopUi.toggle("Update automatically",app.autoUpdate());
        auto.addActionListener(e->app.setAutoUpdate(auto.isSelected()));
        JPanel updates=DesktopUi.column();updates.add(DesktopUi.switchRow("Update automatically",auto));
        DesktopUi.add(updates,DesktopUi.note("New versions are downloaded from GitHub, checked, and put in place when you restart or close Mininotes. This is v"+Desktop.VERSION+".",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.gap(updates,DesktopUi.S);DesktopUi.add(updates,DesktopUi.actions(DesktopUi.button("Check now",()->{box[0].dispose();app.lookForUpdate(true);})));
        DesktopUi.add(body,DesktopUi.card(null,updates));
        DesktopUi.gap(body,DesktopUi.L);

        // In the background.
        DesktopUi.add(body,DesktopUi.heading("While the window is closed"));DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.Text said=DesktopUi.note(SystemTray.isSupported()?"Mininotes stays in the system tray, so notes can arrive.":"The system tray is not available here. Keep the window open or minimised.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f));
        JCheckBox listen=DesktopUi.toggle("Keep listening",app.listenInTray);listen.setEnabled(SystemTray.isSupported()&&!app.offline);
        listen.addActionListener(e->app.setTrayListening(listen.isSelected(),said));
        JPanel background=DesktopUi.column();background.add(DesktopUi.switchRow("Keep listening",listen));DesktopUi.add(background,said);
        DesktopUi.add(body,DesktopUi.card(null,background));
        DesktopUi.gap(body,DesktopUi.L);

        // The window.
        DesktopUi.add(body,DesktopUi.heading("Window"));DesktopUi.gap(body,DesktopUi.S);
        JCheckBox tree=DesktopUi.toggle("Show the tree",app.treeShown());
        tree.addActionListener(e->app.showTree(tree.isSelected()));
        JPanel window=DesktopUi.column();window.add(DesktopUi.switchRow("Show the tree",tree));
        // What Home shows, under one heading, each by its name only (the owner, 2026-10-04: "instead of repeating Show at each
        // element, change the title of the section to Show on Home and use only the element name"; decision 95).
        DesktopUi.gap(window,DesktopUi.M);DesktopUi.add(window,DesktopUi.quiet("Show on Home"));
        // Recent, listed down the right of the window (decisions 77 and 86).
        JCheckBox opened=DesktopUi.toggle("Recent",app.openListWanted());
        opened.addActionListener(e->app.setOpenListWanted(opened.isSelected()));
        DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.switchRow("Recent",opened));
        DesktopUi.add(window,DesktopUi.note("Every folder and note as a list, beside Home and the page. ⋯ → Tree, or Ctrl+B, does the same.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // The archive and the bin as icons on Home, as a phone keeps its bin on its desktop (decision 41); off, in ⋯.
        // Home's places, each on Home or not, as Home's right-click switches them (decision 78).
        for(String[] place:Desktop.PLACES_ON_HOME) {
            String id=place[0];JCheckBox placeOn=DesktopUi.toggle(place[1],app.onHome(id));
            placeOn.addActionListener(e->app.setOnHome(id,placeOn.isSelected()));
            DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.switchRow(place[1],placeOn));
        }
        DesktopUi.add(window,DesktopUi.note("Off, each is in ⋯.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // How long what is carried onto Temp stays (decision 79).
        JComboBox<String> stay=new JComboBox<>(Desktop.TEMP_SPANS);stay.setSelectedIndex(app.tempSpan());
        stay.addActionListener(e->app.setTempSpan(stay.getSelectedIndex()));
        JPanel stayRow=new JPanel(new BorderLayout(12,0));stayRow.setOpaque(false);stayRow.add(DesktopUi.body("Temp: things stay"),BorderLayout.WEST);
        JPanel holdStay=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));holdStay.setOpaque(false);holdStay.add(stay);stayRow.add(holdStay,BorderLayout.EAST);
        DesktopUi.gap(window,DesktopUi.S);DesktopUi.add(window,stayRow);
        // Temp as a note to self (decision 84).
        JCheckBox toMine=DesktopUi.toggle("Temp: send to my devices",app.tempToMine());
        toMine.addActionListener(e->app.setTempToMine(toMine.isSelected()));
        DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.switchRow("Temp: send to my devices",toMine));
        DesktopUi.add(window,DesktopUi.note("What is let go on Temp goes to your other devices too, as a note to yourself.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // Where what comes on Temp from my other devices shows: in Temp only, unless this is on (decision 98).
        JCheckBox tempHome=DesktopUi.toggle("Temp: also show on Home",app.store.tempOnHome());
        tempHome.addActionListener(e->app.setTempOnHome(tempHome.isSelected()));
        DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.switchRow("Temp: also show on Home",tempHome));
        DesktopUi.add(window,DesktopUi.note("What comes on Temp from your other devices is in Temp. On, it is on Home as well.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // How long Recent keeps what was opened (the owner: "an option letting the user decide for how long").
        int[] keepDays={1,3,7,30};int kept=java.util.Arrays.binarySearch(keepDays,app.recentDays());
        JComboBox<String> keeps=new JComboBox<>(new String[]{"A day","Three days","A week","A month"});keeps.setSelectedIndex(Math.max(0,kept));
        keeps.addActionListener(e->app.setRecentDays(keepDays[keeps.getSelectedIndex()]));
        JPanel keepRow=new JPanel(new BorderLayout(12,0));keepRow.setOpaque(false);keepRow.add(DesktopUi.body("Recent keeps"),BorderLayout.WEST);
        JPanel holdKeeps=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));holdKeeps.setOpaque(false);holdKeeps.add(keeps);keepRow.add(holdKeeps,BorderLayout.EAST);
        DesktopUi.gap(window,DesktopUi.S);DesktopUi.add(window,keepRow);
        // Where a file somebody shares with this PC on its own is shown (the owner, 2026-10-04: "by default let's have a
        // folder, Shared with me, but this setting could be changed by the user"; decision 92): Home, the place Shared with
        // me (decision 94), or any folder on Home. Kept on Home all the same, so it never goes on with a folder.
        JComboBox<String> sharedTo=new JComboBox<>(new String[]{"Home",NoteStore.SHARED_WITH_ME});sharedTo.setEnabled(false);
        java.util.List<String> sharedIds=new java.util.ArrayList<>(java.util.List.of(Things.HOME,""));
        app.disk.submit(()->app.store.collections(),shelves->{
            for(NoteStore.Shelf one:shelves){sharedTo.addItem(one.name);sharedIds.add(one.id);}
            sharedTo.setSelectedIndex(Math.max(0,sharedIds.indexOf(app.context.getSharedPreferences("settings",0).getString(NoteStore.SHARED_TO,""))));
            sharedTo.addActionListener(e->{int picked=sharedTo.getSelectedIndex();if(picked>=0)app.context.getSharedPreferences("settings",0).edit().putString(NoteStore.SHARED_TO,sharedIds.get(picked)).apply();});
            sharedTo.setEnabled(true);
        },app::failed);
        JPanel sharedRow=new JPanel(new BorderLayout(12,0));sharedRow.setOpaque(false);sharedRow.add(DesktopUi.body("Shared with me goes to"),BorderLayout.WEST);
        JPanel holdShared=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));holdShared.setOpaque(false);holdShared.add(sharedTo);sharedRow.add(holdShared,BorderLayout.EAST);
        DesktopUi.gap(window,DesktopUi.S);DesktopUi.add(window,sharedRow);
        DesktopUi.add(window,DesktopUi.note("Where a file somebody shares with you on its own shows. It stays on Home, so it never goes on with a folder.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // Favourites and the search on Home (decision 47), as Home's right-click switches them.
        JCheckBox dockOn=DesktopUi.toggle("Dock",app.showing(Desktop.SHOW_DOCK));
        dockOn.addActionListener(e->app.setShowing(Desktop.SHOW_DOCK,dockOn.isSelected()));
        JCheckBox searchOn=DesktopUi.toggle("Search",app.showing(Desktop.SHOW_SEARCH));
        searchOn.addActionListener(e->app.setShowing(Desktop.SHOW_SEARCH,searchOn.isSelected()));
        DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.switchRow("Dock",dockOn));window.add(DesktopUi.switchRow("Search",searchOn));
        DesktopUi.add(window,DesktopUi.note("On Home's first page. Off, favourites are still in the Favourites icon, and search is Ctrl+F.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        // The same ladder as in a note's menu and on the phone: ten rungs. Here it is this PC's, for every note that
        // has no size of its own; one that has keeps it (see Reading).
        JPanel sizeHolder=new JPanel(new GridBagLayout());sizeHolder.setOpaque(false);sizeHolder.add(DesktopLook.ladder(app::textSize,app::setTextSize));
        DesktopUi.gap(window,DesktopUi.S);window.add(DesktopUi.row(DesktopUi.body("Text size of notes"),sizeHolder));
        DesktopUi.add(window,DesktopUi.note("Notes open at this size unless one has its own, set from its menu.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.add(body,DesktopUi.card(null,window));
        DesktopUi.gap(body,DesktopUi.L);

        // Pictures made smaller on their way in, and those already here (the owner, 2026-10-06; decision 112).
        DesktopUi.add(body,DesktopUi.heading("Pictures and files"));DesktopUi.gap(body,DesktopUi.S);
        JCheckBox shrinkOn=DesktopUi.toggle(Shrink.SWITCH,app.store.shrinkPictures());
        shrinkOn.addActionListener(e->app.store.setShrinkPictures(shrinkOn.isSelected()));
        JPanel pictures=DesktopUi.column();pictures.add(DesktopUi.switchRow(Shrink.SWITCH,shrinkOn));
        DesktopUi.add(pictures,DesktopUi.note(Shrink.SWITCH_UNDER,380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.gap(pictures,DesktopUi.S);DesktopUi.add(pictures,DesktopUi.actions(DesktopUi.button(Shrink.ALREADY,()->{box[0].dispose();app.shrinkHere();})));
        DesktopUi.add(body,DesktopUi.card(null,pictures));
        DesktopUi.gap(body,DesktopUi.L);

        // Who wrote what: whether it shows, and the colour your own writing takes. Other people's are theirs.
        DesktopUi.add(body,DesktopUi.heading("Writing colours"));DesktopUi.gap(body,DesktopUi.S);
        JCheckBox whoWrote=DesktopUi.toggle("Show who wrote what",app.whoWrote);
        whoWrote.addActionListener(e->app.setWhoWrote(whoWrote.isSelected()));
        JPanel writing=DesktopUi.column();writing.add(DesktopUi.switchRow("Show who wrote what",whoWrote));
        JPanel myInk=new JPanel(new GridBagLayout());myInk.setOpaque(false);
        writing.add(DesktopUi.row(DesktopUi.body("My writing colour"),myInk));
        app.inkRow(null,(you,row)->{myInk.add(row);myInk.revalidate();myInk.repaint();});
        DesktopUi.add(writing,DesktopUi.note("In notes two or more people write in. To change someone's colour, click their round under a note's title.",380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.add(body,DesktopUi.card(null,writing));

        box[0]=DesktopUi.sheet(app.frame,"Settings",DesktopUi.scrolling(body),null,true);
        DesktopUi.show(box[0],540,760);
    }

    /**
     * The two ways notes travel (see Relays.ONLY_MINE): a real choice between two, so a round mark beside each,
     * with what each means under it. The relays are part of the second way and shown only while it is chosen.
     * The change is applied while Mininotes runs, and the line under the choice says so until it has.
     */
    private static JPanel travel(Desktop app) {
        JPanel card=DesktopUi.column();
        java.awt.Font small=DesktopUi.BODY.deriveFont(12.5f);
        boolean mine=Node.onlyMine(app.context);
        JRadioButton only=way(Relays.ONLY_MINE,mine),helpers=way(Relays.HELPERS,!mine);
        ButtonGroup ways=new ButtonGroup();ways.add(only);ways.add(helpers);
        card.add(wayRow(Relays.ONLY_MINE,only));
        DesktopUi.add(card,DesktopUi.note(Relays.travelLine(true),380,DesktopUi.QUIET,small));
        DesktopUi.gap(card,DesktopUi.S);
        card.add(wayRow(Relays.HELPERS,helpers));
        DesktopUi.add(card,DesktopUi.note(Relays.travelLine(false),380,DesktopUi.QUIET,small));
        // What is going on once a way is chosen; nothing, and no room, until then.
        DesktopUi.Text said=DesktopUi.note(" ",380,DesktopUi.QUIET,small);said.setVisible(false);
        DesktopUi.add(card,said);
        // The helpers themselves, under the way that uses them.
        JPanel through=DesktopUi.column();
        DesktopUi.gap(through,DesktopUi.M);DesktopUi.add(through,DesktopUi.heading("Relays"));DesktopUi.gap(through,DesktopUi.S);
        DesktopUi.add(through,relays(app));
        through.setVisible(!mine);
        DesktopUi.add(card,through);
        boolean[] was={mine};
        java.util.function.Consumer<Boolean> choose=on->{
            if(on==was[0])return;
            was[0]=on;through.setVisible(!on);card.revalidate();card.repaint();
            only.setEnabled(false);helpers.setEnabled(false);said.setVisible(true);
            said.setText(on?"Letting go of every relay…":"Connecting to your relays…");
            app.connectivity.submit(()->{Node.onlyMine(app.context,on);return null;},
                done->{only.setEnabled(true);helpers.setEnabled(true);said.setText(on?"Notes go only between your devices now.":"Helpers carry notes when needed now.");},
                failure->{was[0]=!on;(on?helpers:only).setSelected(true);through.setVisible(on);only.setEnabled(true);helpers.setEnabled(true);said.setText("That could not be changed.");});
        };
        only.addActionListener(e->choose.accept(true));
        helpers.addActionListener(e->choose.accept(false));
        return card;
    }
    private static JRadioButton way(String name,boolean chosen) {
        JRadioButton mark=new JRadioButton("",chosen);mark.setOpaque(false);mark.setFocusPainted(false);
        mark.getAccessibleContext().setAccessibleName(name);mark.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return mark;
    }
    /** A way's name with its mark at the right, where a switch would be; the name chooses it too. */
    private static JPanel wayRow(String name,JRadioButton mark) {
        DesktopUi.Text words=DesktopUi.body(name);
        words.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(mark.isEnabled()&&!mark.isSelected())mark.doClick();}});
        return DesktopUi.row(words,mark);
    }

    /** Where a relay's state is read: the running node. The gallery, which runs none, draws one of each. */
    static java.util.function.Function<String,String> stateOf=Node::relayState;

    /**
     * The switch for the public relays, the owner's own relays each with its state and a Remove, and a field to
     * add one. Adding asks the relay first and keeps it only if it answered; every change is applied while
     * Mininotes runs, and what is going on is said in the line under the field until it has ended.
     */
    private static JPanel relays(Desktop app) {
        JPanel card=DesktopUi.column();
        java.awt.Font small=DesktopUi.BODY.deriveFont(12.5f);
        JCheckBox publicOn=DesktopUi.toggle(Relays.SWITCH,Node.publicRelays(app.context));
        card.add(DesktopUi.switchRow(Relays.SWITCH,publicOn));
        DesktopUi.Text means=DesktopUi.note(Node.relaysLine(app.context),380,DesktopUi.QUIET,small);
        DesktopUi.add(card,means);
        JPanel mine=DesktopUi.column();
        DesktopUi.gap(card,DesktopUi.S);DesktopUi.add(card,mine);
        JTextField typed=new JTextField(18);typed.putClientProperty("JTextField.placeholderText","host:port");
        typed.getAccessibleContext().setAccessibleName("Add a relay, as host:port");
        DesktopUi.Text said=DesktopUi.note(" ",380,DesktopUi.QUIET,small);
        Runnable[] draw={null};
        draw[0]=()->{
            mine.removeAll();
            for(String one:Node.ownRelays(app.context)) {
                String state=stateOf.apply(one);
                JButton[] remove={null};
                remove[0]=DesktopUi.button("Remove",()->{
                    remove[0].setEnabled(false);said.setText("Letting go of "+one+"…");
                    app.connectivity.submit(()->{Node.removeRelay(app.context,one);return null;},
                        done->{said.setText("Removed "+one+".");draw[0].run();},
                        failure->{said.setText(one+" could not be removed.");remove[0].setEnabled(true);});
                });
                remove[0].getAccessibleContext().setAccessibleName("Remove "+one);
                // The address, its state quietly under it, and its button on the right, the edge under Add's.
                JPanel named=DesktopUi.column();DesktopUi.add(named,DesktopUi.body(one));
                if(!state.isEmpty())DesktopUi.add(named,DesktopUi.quiet(state));
                mine.add(DesktopUi.row(named,remove[0]));
            }
            means.setText(Node.relaysLine(app.context));
            mine.revalidate();mine.repaint();
        };
        draw[0].run();
        boolean[] quiet={false};
        publicOn.addActionListener(e->{
            if(quiet[0])return;boolean on=publicOn.isSelected();
            publicOn.setEnabled(false);said.setText(on?"Connecting to the public relays…":"Letting go of the public relays…");
            app.connectivity.submit(()->{Node.publicRelays(app.context,on);return null;},
                done->{publicOn.setEnabled(true);said.setText(on?"The public relays are on.":"Only your relays are used now.");draw[0].run();},
                failure->{quiet[0]=true;publicOn.setSelected(!on);quiet[0]=false;publicOn.setEnabled(true);said.setText("That could not be changed.");});
        });
        JButton[] adds={null};
        Runnable adding=()->{
            JButton add=adds[0];
            String problem=Relays.problem(typed.getText());
            if(problem!=null){said.setText(problem);return;}
            String one=Relays.parse(typed.getText());
            java.util.List<String> kept=Node.ownRelays(app.context);
            if(kept.contains(one)){said.setText(one+" is already one of your relays.");return;}
            if(kept.size()>=Relays.MOST){said.setText("Eight relays is the most. Remove one first.");return;}
            add.setEnabled(false);typed.setEnabled(false);said.setText("Checking "+one+"…");
            app.connectivity.submit(()->{
                if(!Node.relayAnswers(one))return one+" did not answer, so it was not added. Check the address, and that the relay is running.";
                SwingUtilities.invokeLater(()->said.setText("Connecting to "+one+"…"));
                boolean connected=Node.addRelay(app.context,one);
                return connected?"Added. Connected to "+one+"."
                    :Node.running()?"Added. "+one+" answered but did not take this PC yet; it is tried again every few minutes."
                    :"Added. It is used once sharing starts.";
            },outcome->{
                add.setEnabled(true);typed.setEnabled(true);said.setText(outcome);
                if(outcome.startsWith("Added"))typed.setText("");
                draw[0].run();
            },failure->{add.setEnabled(true);typed.setEnabled(true);said.setText("It could not be checked.");});
        };
        adds[0]=DesktopUi.button("Add",adding);typed.addActionListener(e->adding.run());
        card.add(DesktopUi.row(typed,adds[0]));
        DesktopUi.add(card,said);
        return card;
    }
}
