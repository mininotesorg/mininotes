package org.mininotes.android;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

final class DesktopProfile {
    /** @param onlyMine notes travel only between the owner's devices: the address is this PC's door at home, not a relay */
    record Connection(String name,String address,String permanent,int relays,String pairing,boolean onlyMine) {
        Connection(String name,String address,String permanent,int relays,String pairing){this(name,address,permanent,relays,pairing,false);}
    }
    static void open(Desktop app){open(app,null,null,null);}
    /**
     * My profile scrolled to one of its parts, its field ready to type in where it has one: what the Agent's Take me there
     * opens (the owner, 2026-10-19: "for example in setting Integritas it will open directly the section in the profile
     * page"; decision 213). A part is named by its heading, "Stamping", "Text size", "Backup", or by what it holds, "Your
     * Parlons! address", "Describe what each thing does"; a name it does not have opens it at its top.
     */
    static void open(Desktop app,String section){open(app,null,null,section);}
    /**
     * @param over   the box it is opened from, People and devices on its line This device; null from the window
     * @param backTo what its ‹ says it goes back to (the owner, 2026-10-05: "from the profile page, we should be able to go
     *               back to people and devices"; decision 105); null from the window
     */
    static void open(Desktop app,Window over,String backTo){open(app,over,backTo,null);}
    static void open(Desktop app,Window over,String backTo,String section) {
        JDialog[] box={null};
        JTextField name=new JTextField(24);name.setName("profileName");
        DesktopUi.Text outcome=DesktopUi.quiet(" "),counts=DesktopUi.quiet(" "),direct=DesktopUi.quiet(" "),door=DesktopUi.quiet(" "),travel=DesktopUi.quiet(" ");direct.setName("directState");door.setName("doorState");JLabel state=new Desktop.Dot(app.offline?"Offline session":"Connecting…");
        state.setFont(DesktopUi.BODY);state.setIconTextGap(8);
        JLabel qr=new JLabel("<html><div style='width:150px'>"+Say.t("Your code appears here once this PC is connected.")+"</div></html>");qr.setFont(DesktopUi.BODY.deriveFont(13f));qr.setForeground(DesktopUi.QUIET);qr.setName("profileQr");qr.setHorizontalAlignment(SwingConstants.CENTER);
        qr.setPreferredSize(new Dimension(220,220));qr.setMinimumSize(new Dimension(220,220));
        JTextArea address=readonly(3),permanent=readonly(2);address.setName("maximaAddress");permanent.setName("permanentAddress");state.setName("connectionState");
        Connection[] current={null};
        name.setEnabled(false);
        // Two names: the one other people see, the same on all your devices, and the one your own devices know
        // this PC by. Each saves as it is typed. Until the first is chosen, the PC's own name stands in, and it says so.
        JTextField device=new JTextField(24);device.setName("profileDevice");device.setEnabled(false);
        DesktopUi.Text nameHelp=DesktopUi.quiet(Node.nameChosen(app.context)?"What other people see.":"Choose the name other people see.");
        DesktopUi.Text deviceHelp=DesktopUi.quiet("What your own devices call this one.");
        Runnable saveName=()->{
            String value=name.getText().trim();if(value.isEmpty()||value.length()>80){nameHelp.setForeground(DesktopUi.WARN);nameHelp.setText(Say.t("Use a name between 1 and 80 characters."));return;}
            nameHelp.setForeground(DesktopUi.QUIET);nameHelp.setText(Say.t("Saving…"));app.disk.submit(()->{
                // When it was chosen goes with it: your other devices take whichever name was chosen last.
                Node.chooseName(app.context,value);app.store.myName=value;return null;
            },done->{if(value.equals(name.getText().trim()))nameHelp.setText(Say.t("Saved. What other people see."));app.connectivity.submit(()->{Node.called(app.context,value);return null;},v->{},app::failed);},error->{nameHelp.setForeground(DesktopUi.WARN);nameHelp.setText(Say.t("Name could not be saved. Edit it to try again."));app.failed(error);});
        };
        Runnable saveDevice=()->{
            String value=device.getText().trim();if(value.isEmpty()||value.length()>80){deviceHelp.setForeground(DesktopUi.WARN);deviceHelp.setText(Say.t("Use a name between 1 and 80 characters."));return;}
            deviceHelp.setForeground(DesktopUi.QUIET);deviceHelp.setText(Say.t("Saving…"));app.disk.submit(()->{Node.chooseDevice(app.context,value);return null;},
                done->{if(value.equals(device.getText().trim()))deviceHelp.setText(Say.t("Saved. What your own devices call this one."));},error->{deviceHelp.setForeground(DesktopUi.WARN);deviceHelp.setText(Say.t("Name could not be saved. Edit it to try again."));app.failed(error);});
        };
        JPanel you=DesktopUi.column();
        DesktopUi.add(you,DesktopUi.body("Your name"));DesktopUi.gap(you,4);DesktopUi.add(you,name);DesktopUi.gap(you,6);DesktopUi.add(you,nameHelp);
        DesktopUi.gap(you,14);
        // And the colour they see you in, beside the name (the owner, 2026-10-03: "make it easy to pick a writing colour").
        JPanel colour=DesktopUi.column();
        DesktopUi.add(you,DesktopUi.body("Your colour"));DesktopUi.gap(you,4);DesktopUi.add(you,colour);DesktopUi.gap(you,6);
        DesktopUi.add(you,DesktopUi.quiet("Everybody sees your round, and your writing, in it."));
        app.inkRow(null,(mine,dots)->{colour.add(dots);colour.revalidate();colour.repaint();});
        DesktopUi.gap(you,14);
        DesktopUi.add(you,DesktopUi.body("This device"));DesktopUi.gap(you,4);DesktopUi.add(you,device);DesktopUi.gap(you,6);DesktopUi.add(you,deviceHelp);
        // Your picture, on your round everywhere you are paired (decision 158).
        DesktopUi.gap(you,14);
        JComponent myRound=DesktopUi.avatar(app.store.myName==null||app.store.myName.isBlank()?Node.nameHere(app.context):app.store.myName);myRound.putClientProperty("faceOf","");
        JButton[] myFace={null};myFace[0]=app.button("Choose a picture…",()->app.faceMenu(myFace[0],null,null));
        // Clicked, your picture big (the owner, 2026-10-12: "we should be able to click the picture and see it bigger");
        // with none yet, the way to choose one.
        myRound.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));myRound.setToolTipText(Say.t("See it big"));
        myRound.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){
            java.awt.image.BufferedImage face=DesktopFaces.of(app,null);
            if(face==null){app.faceMenu(myFace[0],null,null);return;}
            JPopupMenu big=new JPopupMenu();JPanel shown=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER,12,12));shown.setOpaque(false);
            shown.add(DesktopFaces.big(face,320));big.add(shown);big.show(myRound,0,myRound.getHeight()+4);
        }});
        JPanel faceRow=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));faceRow.setOpaque(false);faceRow.add(myRound);faceRow.add(myFace[0]);
        DesktopUi.add(you,DesktopUi.body("Your picture"));DesktopUi.gap(you,4);DesktopUi.add(you,faceRow);
        // Your Parlons! address, set once and sent by itself to everybody you are paired with (the owner, 2026-10-11; decision 147).
        DesktopUi.gap(you,14);
        JTextField parlons=new JTextField(24);parlons.setName("profileParlons");parlons.putClientProperty("JTextField.placeholderText",Parlons.HINT);
        parlons.getAccessibleContext().setAccessibleName("Your Parlons! address");
        DesktopUi.Text parlonsHelp=DesktopUi.quiet("Your contacts get it by themselves, so they can chat with you in one tap.");
        JButton keepParlons=app.button("Save",()->{
            String typed=parlons.getText();
            String kept;try{kept=Parlons.address(typed);}catch(IllegalArgumentException wrong){parlonsHelp.setForeground(DesktopUi.WARN);parlonsHelp.setText(Say.t(wrong.getMessage()));return;}
            parlonsHelp.setForeground(DesktopUi.QUIET);parlonsHelp.setText(Say.t(kept.isEmpty()?"Removing\u2026":"Saving\u2026"));
            app.disk.submit(()->app.store.setMyParlons(kept),done->app.network.submit(()->Post.sendMyParlons(app.context,app.store,app.keys,true),
                sent->parlonsHelp.setText(sent==1?Say.t(kept.isEmpty()?"Removed, and sent to 1 device.":"Saved, and sent to 1 device."):sent>0?Say.t(kept.isEmpty()?"Removed, and sent to {0} devices.":"Saved, and sent to {0} devices.",sent):Say.t(kept.isEmpty()?"Removed. It goes to your contacts as they come online.":"Saved. It goes to your contacts as they come online.")),
                e->parlonsHelp.setText(Say.t("Saved here. It goes to your contacts as they come online."))),
                e->{parlonsHelp.setForeground(DesktopUi.WARN);parlonsHelp.setText(Say.t(e.getMessage()==null?"That address could not be kept.":e.getMessage()));});
        });
        JPanel parlonsRow=new JPanel(new BorderLayout(8,0));parlonsRow.setOpaque(false);parlonsRow.add(parlons);parlonsRow.add(keepParlons,BorderLayout.EAST);
        DesktopUi.add(you,DesktopUi.body("Your Parlons! address"));DesktopUi.gap(you,4);DesktopUi.add(you,parlonsRow);DesktopUi.gap(you,6);DesktopUi.add(you,parlonsHelp);
        app.disk.submit(()->app.store.myParlons(),mine->{
            parlons.setText(mine);
            // Copied from Parlons! a moment ago, and none set yet: already in the field, saying so.
            if(mine.isEmpty()){String copied=Desktop.copiedParlons();if(!copied.isEmpty()){parlons.setText(copied);parlonsHelp.setText(Say.t("Filled in from what you copied. Save to send it to your contacts."));}}
        },app::failed);

        // Your code: the picture another device scans, and the same thing as text for when it cannot.
        JButton link=app.button("Copy my link",()->{if(current[0]!=null&&!current[0].pairing.isEmpty()){clipboard(Pairing.invite(current[0].name,Pairing.link(current[0].pairing),Desktop.DOWNLOAD));outcome.setText(Say.t("Your link is copied, with a few words. Paste it in a message, a mail or a post."));}});link.setEnabled(false);
        JButton copy=app.button("Copy address",()->{if(current[0]!=null&&!current[0].address.isEmpty()){clipboard(current[0].address);outcome.setText(Say.t("Address copied"));}});copy.setEnabled(false);
        JButton raw=app.button("Address QR…",()->{if(current[0]!=null&&!current[0].address.isEmpty())try{JLabel big=new JLabel(new ImageIcon(DesktopQr.draw(current[0].address,320)));DesktopUi.tell(SwingUtilities.getWindowAncestor(qr),"Maxima address",big);}catch(Exception e){app.failed(e);}});raw.setEnabled(false);
        JPanel side=DesktopUi.column();
        DesktopUi.add(side,DesktopUi.note("Scan this with Mininotes on another device to pair with this PC.",200,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));DesktopUi.gap(side,12);
        for(JButton b:new JButton[]{link,copy,raw}){DesktopUi.add(side,b);DesktopUi.gap(side,DesktopUi.S);}
        JPanel code=new JPanel(new BorderLayout(DesktopUi.M,0));code.setOpaque(false);
        JPanel qrHolder=new JPanel(new BorderLayout());qrHolder.setBackground(Color.WHITE);qrHolder.setBorder(BorderFactory.createLineBorder(DesktopUi.LINE));qrHolder.add(qr);
        code.add(qrHolder,BorderLayout.WEST);code.add(side);
        JPanel codeCard=DesktopUi.column();DesktopUi.add(codeCard,code);DesktopUi.gap(codeCard,12);DesktopUi.add(codeCard,DesktopUi.quiet("Your Maxima address"));DesktopUi.gap(codeCard,4);DesktopUi.add(codeCard,address);

        // Connection: whether it is, and the one thing to do if it is not.
        JButton reconnect=app.button("Reconnect",()->{
            if(app.offline){outcome.setText(Say.t("Restart without --offline to connect."));return;}
            outcome.setText(Say.t("Finding relays and refreshing contacts…"));app.connectivity.submit(()->{
                Node.retryStart();return Node.reconnect(app.context);
            },n->{outcome.setText(Say.t(n>0?"Connected":"No relay answered. Check your internet connection and Windows firewall."));app.startNode();},app::failed);
        });reconnect.setEnabled(!app.offline&&!Node.onlyMine(app.context));
        JPanel stateRow=new JPanel(new BorderLayout(DesktopUi.M,0));stateRow.setOpaque(false);JPanel stateWords=DesktopUi.column();DesktopUi.add(stateWords,state);DesktopUi.gap(stateWords,2);DesktopUi.add(stateWords,counts);
        // Whether devices away from home can reach this PC straight, or only through a relay: docs/DIRECT.md.
        DesktopUi.gap(stateWords,2);DesktopUi.add(stateWords,direct);
        // Its door and how notes travel, which People and devices said on its line This device: here, where this device
        // is the subject (the owner: "direct connections open on port 9601 and so on should show in the profile page").
        DesktopUi.gap(stateWords,2);DesktopUi.add(stateWords,door);DesktopUi.gap(stateWords,2);DesktopUi.add(stateWords,travel);
        stateRow.add(stateWords);JPanel rc=new JPanel(new GridBagLayout());rc.setOpaque(false);rc.add(reconnect);stateRow.add(rc,BorderLayout.EAST);
        // Listening while the window is closed is a switch, and every switch is in Settings.
        JPanel connection=DesktopUi.column();DesktopUi.add(connection,stateRow);
        DesktopUi.gap(connection,12);DesktopUi.add(connection,DesktopUi.quiet("Permanent address"));DesktopUi.gap(connection,4);DesktopUi.add(connection,permanent);DesktopUi.gap(connection,DesktopUi.S);
        DesktopUi.add(connection,DesktopUi.actions(app.button("Copy permanent address",()->{if(current[0]!=null&&!current[0].permanent.isEmpty()){clipboard(current[0].permanent);outcome.setText(Say.t("Permanent address copied"));}})));

        // One way in: everything about people and devices is managed in their own window (the owner, 2026-10-18; decision 202).
        // What it holds is said under its heading (decision 231), so here only the way there.
        JPanel people=DesktopUi.column();
        DesktopUi.add(people,DesktopUi.actions(app.button("Manage…",()->{if(backTo!=null)box[0].dispose();else app.people(null);})));
        // Stamping, all of the Integritas account in one place (decisions 203 and 231): its key, and the devices it goes to.
        JPanel integritas=DesktopUi.column();
        DesktopUi.add(integritas,DesktopUi.note(Say.t("Stamp files and notes on the Minima blockchain through your own Integritas account, to prove later that they existed then, unchanged.")));
        DesktopUi.gap(integritas,12);
        // The API key typed here, as the Parlons! address is, and sent to the owner's own devices chosen, one switch each (the
        // owner, 2026-10-19: "in the profile we should just have the account API setting ... let's do that on the profile page
        // directly as we do for the Parlons address"; "we should have the option to share it on all our devices, choosing by
        // device"; decision 210). What was stamped is listed from the menu, Stamped on Minima…, last in Find (decision 230).
        JPasswordField apiKey=new JPasswordField(24);apiKey.setName("integritasKey");apiKey.getAccessibleContext().setAccessibleName(Say.t("Integritas API key"));
        apiKey.putClientProperty("JTextField.placeholderText",Say.t("Integritas API key"));
        DesktopUi.Text keyHelp=DesktopUi.note(Say.t("Paste the API key from your profile at integritas.technology. It is kept in your notebook on this PC, locked with it, and sent only to Integritas and to the devices you choose below."));
        // The devices it goes to, remembered: the switches say what was chosen, and one switched on gets the key at once (decision 223).
        JPanel devices=DesktopUi.column();java.util.Set<String> chosenDevices=Post.keyGoesTo(app.context,DesktopStamp.KEY);
        JButton keepKey=app.button("Save",()->{
            String typed=new String(apiKey.getPassword()).trim();
            if(!typed.isEmpty()&&!Integritas.looksLikeAKey(typed)){keyHelp.setForeground(DesktopUi.WARN);keyHelp.setText(Say.t("That does not look like an Integritas API key."));return;}
            java.util.Set<String> to=new java.util.HashSet<>(chosenDevices);
            DesktopStamp.linked(app,!typed.isEmpty());
            app.disk.submit(()->{app.store.setSecret(DesktopStamp.KEY,typed);app.store.setSecret(DesktopStamp.KEY+".at",String.valueOf(System.currentTimeMillis()));return null;},done->{
                keyHelp.setForeground(DesktopUi.QUIET);
                if(to.isEmpty())return;
                app.status.setText(Say.t("Sending to my devices…"));
                app.network.submit(()->Post.sendKey(app.context,app.store,app.keys,DesktopStamp.KEY,to),said->app.status.setText(Say.t(said)),app::failed);
            },app::failed);
        });
        JPanel keyRow=new JPanel(new BorderLayout(8,0));keyRow.setOpaque(false);keyRow.add(apiKey);keyRow.add(keepKey,BorderLayout.EAST);
        DesktopUi.add(integritas,DesktopUi.body("Integritas API key"));DesktopUi.gap(integritas,4);DesktopUi.add(integritas,keyRow);DesktopUi.gap(integritas,6);
        DesktopUi.add(integritas,keyHelp);DesktopUi.gap(integritas,4);
        DesktopUi.add(integritas,DesktopUi.actions(DesktopUi.link("integritas.technology",()->{try{Desktop.browse(Integritas.SITE);}catch(Exception noBrowser){app.status.setText(Integritas.SITE);}})));
        DesktopUi.add(integritas,devices);
        app.disk.submit(()->{java.util.List<NoteStore.Contact> own=new java.util.ArrayList<>();for(NoteStore.Contact one:app.store.addresses())if(one.mine&&one.paired())own.add(one);
            return new Object[]{app.store.secret(DesktopStamp.KEY),own};},got->{
            apiKey.setText((String)got[0]);
            @SuppressWarnings("unchecked") java.util.List<NoteStore.Contact> own=(java.util.List<NoteStore.Contact>)got[1];
            if(own.isEmpty())return;
            DesktopUi.gap(devices,12);DesktopUi.add(devices,DesktopUi.body("Also on my devices"));
            for(NoteStore.Contact one:own) {
                JCheckBox on=DesktopUi.toggle(one.name,chosenDevices.contains(one.address));
                on.addActionListener(e->{
                    if(on.isSelected())chosenDevices.add(one.address);else chosenDevices.remove(one.address);
                    Post.keyGoesTo(app.context,DesktopStamp.KEY,chosenDevices);
                    // Switched on with a key already saved: it goes now, to that device.
                    if(on.isSelected()&&apiKey.getPassword().length>0){app.status.setText(Say.t("Sending to my devices…"));
                        app.network.submit(()->Post.sendKey(app.context,app.store,app.keys,DesktopStamp.KEY,java.util.Set.of(one.address)),said->app.status.setText(Say.t(said)),app::failed);}
                });
                DesktopUi.add(devices,DesktopUi.switchRow(one.name,on));
            }
            DesktopUi.add(devices,DesktopUi.note(Say.t("Those switched on receive the key, sealed for them, as soon as they are switched on and whenever it is saved.")));
            devices.revalidate();devices.repaint();
        },app::failed);
        // The lock lives in Security, which the bar shows at all times; here, where it stands and the way there.
        JPanel lock=DesktopUi.column();
        boolean isLocked=DesktopLock.locked(app.context.getFilesDir().toPath());
        JLabel lockState=new JLabel(Say.t(isLocked?"Encrypted with a password":"Not encrypted"),DesktopLock.padlock(isLocked,16),SwingConstants.LEFT);lockState.setIconTextGap(8);lockState.setFont(DesktopUi.BODY);
        lock.add(DesktopUi.row(lockState,app.button("Security…",()->DesktopLock.settings(app))));
        JPanel backup=DesktopUi.column();
        // What it holds is said under its heading (decision 231), and what it never holds here, plainly (the owner,
        // 2026-10-22: "It should be clear that any private element can't be backed up"; decision 235).
        DesktopUi.add(backup,DesktopUi.note(Say.t("A backup holds every folder, note and file in Mininotes on this PC, except private spaces, which are never backed up.")));
        DesktopUi.gap(backup,8);
        DesktopUi.add(backup,DesktopUi.actions(app.button("Export…",()->app.save(app::backup)),app.button("Add from a backup…",()->app.save(app::importBackup))));

        // The help request is no longer here, in plain sight: it is set inside a private space alone (decision 201).
        // The size of the whole app on this PC, at the app's level (the owner, 2026-10-18; decision 200; see DesktopScale).
        JPanel size=DesktopUi.column();
        DesktopUi.add(size,DesktopUi.note(Say.t("Everything in Mininotes on this PC: Home, Recent, the menus and the boxes. A note's own size is in its menu.")));
        DesktopUi.gap(size,12);
        // The ladder a note's menu has, with the app's nine steps, and the size chosen under it (decisions 209 and 219).
        JLabel named=new JLabel(DesktopScale.named(DesktopScale.chosen()));named.setFont(DesktopUi.BODY.deriveFont(13f));named.setForeground(DesktopUi.QUIET);
        JButton again=app.button("Restart now",()->{if(!DesktopScale.restart(app))app.status.setText(Say.t("Close Mininotes and open it again to see the new size."));});
        again.setVisible(DesktopScale.chosen()!=DesktopScale.started);
        DesktopUi.Text waits=DesktopUi.note(Say.t("The new size shows when Mininotes starts again."));waits.setVisible(again.isVisible());
        // A restart only when asked (the owner, 2026-10-19, after it restarted by itself: "if it is not possible to reflect the
        // change without a restart, leave it nevertheless manually"; decision 219): Restart now, then back here.
        JComponent steps=DesktopLook.ladder(DesktopScale.STEPS.length,DesktopScale::chosen,n->{
            if(n==DesktopScale.chosen())return;
            try{DesktopScale.choose(n);}catch(Exception none){app.failed(none);return;}
            named.setText(DesktopScale.named(n));
            boolean pending=n!=DesktopScale.started;again.setVisible(pending);waits.setVisible(pending);size.revalidate();size.repaint();});
        steps.setName("app text size");
        JPanel ladder=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));ladder.setOpaque(false);ladder.add(steps);
        DesktopUi.add(size,ladder);DesktopUi.gap(size,2);
        DesktopUi.add(size,DesktopUi.actions(named,again));DesktopUi.gap(size,6);DesktopUi.add(size,waits);
        // What everything does, under the pointer, or not (the owner, 2026-10-18; decision 205).
        DesktopUi.gap(size,12);
        JCheckBox describe=DesktopUi.toggle("Describe what each thing does",!"false".equals(app.context.getSharedPreferences("settings",0).getString(DesktopMenus.DESCRIBE,"true")));
        describe.addActionListener(e->{app.context.getSharedPreferences("settings",0).edit().putString(DesktopMenus.DESCRIBE,String.valueOf(describe.isSelected())).apply();DesktopMenus.describing(app.context);});
        JPanel describeRow=DesktopUi.switchRow("Describe what each thing does",describe);
        DesktopUi.add(size,describeRow);
        DesktopUi.add(size,DesktopUi.note(Say.t("A line saying what it does, when the pointer rests on a menu entry or a button.")));
        JPanel body=DesktopUi.column();
        // Each part by its heading, for opening at one of them (decision 213): what to show at the top, and what to type in.
        JPanel[] parts={you,codeCard,connection,people,integritas,size,lock,backup};
        java.util.Map<String,JComponent[]> sections=new java.util.HashMap<>();
        for(int i=0;i<parts.length;i++) {
            JPanel card=part(PARTS[i],ICONS[i],ABOUT[i],parts[i]);DesktopUi.add(body,card);DesktopUi.gap(body,DesktopUi.L);
            sections.put(PARTS[i],new JComponent[]{card,null});
        }
        sections.put(STAMPING,new JComponent[]{sections.get(STAMPING)[0],apiKey});
        sections.put("Your Parlons! address",new JComponent[]{parlonsRow,parlons});
        sections.put("Describe what each thing does",new JComponent[]{describeRow,describe});
        JPanel foot=new JPanel(new BorderLayout(DesktopUi.M,0));foot.setOpaque(false);foot.add(outcome);
        JScrollPane scroll=DesktopUi.scrolling(body);scroll.setName("profileScroll");
        // At the top, every part by its name, a link taking the page to it (the owner, 2026-10-21: "for now it is difficult to
        // find what we are looking for"; decision 231).
        JButton[] jumps=new JButton[PARTS.length];
        for(int i=0;i<PARTS.length;i++){JComponent card=sections.get(PARTS[i])[0];jumps[i]=DesktopUi.link(PARTS[i],()->showAt(scroll,body,card,null));jumps[i].setName("profileJump "+PARTS[i]);}
        JPanel jump=DesktopUi.actions(jumps);jump.setName("profileParts");jump.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(jump,0);body.add(Box.createVerticalStrut(DesktopUi.M),1);
        JDialog dialog=DesktopUi.sheet(over!=null?over:app.frame,"My profile",scroll,foot,false);box[0]=dialog;
        if(backTo!=null)DesktopUi.head(dialog,backTo,dialog::dispose,"My profile",null);
        AtomicBoolean busy=new AtomicBoolean();
        Runnable refresh=()->{
            app.disk.submit(()->new int[]{app.store.addresses().size(),app.store.owed(NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING).size()},values->counts.setText((values[0]==1?Say.t("1 paired device"):Say.t("{0} paired devices",values[0]))+"  ·  "+(values[1]==0?Say.t("nothing waiting to send"):values[1]==1?Say.t("1 delivery waiting"):Say.t("{0} deliveries waiting",values[1]))),app::failed);
            if(!app.offline)app.connectivity.submit(Node::reachability,line->{if(dialog.isDisplayable())direct.setText(line.isEmpty()?" ":Say.t(line));},e->{});
            app.connectivity.submit(()->Node.hereLines(app.context),lines->{if(!dialog.isDisplayable())return;door.setText(lines.isEmpty()?" ":Say.t(lines.get(0)));travel.setText(lines.size()<2?" ":Say.t(lines.get(1)));},e->{});
            if(app.offline||!busy.compareAndSet(false,true))return;
            app.connectivity.submit(()->{
                String called=Node.nameHere(app.context);var addresses=Node.addresses(app.context);String live=addresses.isEmpty()?"":addresses.get(0);
                app.store.myAddress=live;
                return new Connection(called,live,Node.permanent(app.context),addresses.size(),live.isEmpty()?"":app.keys.line(called,live,"",false,"",""),Node.onlyMine(app.context));
            },value->{busy.set(false);if(!dialog.isDisplayable())return;render(value,qr,address,permanent,state);current[0]=value;boolean ready=!value.address.isEmpty();copy.setEnabled(ready);link.setEnabled(ready);raw.setEnabled(ready);},e->{busy.set(false);state.setText("Not connected. Try Reconnect");});
        };
        app.disk.submit(()->new String[]{Node.nameHere(app.context),Node.deviceHere(app.context)},values->{
            name.setText(values[0]);name.setEnabled(true);device.setText(values[1]);device.setEnabled(true);outcome.setText(" ");
            saveAsTyped(name,saveName);saveAsTyped(device,saveDevice);
        },app::failed);
        javax.swing.Timer timer=new javax.swing.Timer(5000,e->refresh.run());
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){timer.stop();}});
        dialog.pack();dialog.setSize(600,Math.min(780,dialog.getHeight()));dialog.setLocationRelativeTo(app.frame);
        JComponent[] at=section==null?null:sections.get(section);
        if(at!=null&&at[1]!=null)dialog.getRootPane().putClientProperty("focus",at[1]);
        DesktopUi.shade(dialog);dialog.setVisible(true);refresh.run();timer.start();
        if(at!=null)SwingUtilities.invokeLater(()->showAt(scroll,body,at[0],at[1]));
    }

    /** The part that holds the Integritas account: what Your Integritas account… and stamping with no key yet open. */
    static final String STAMPING="Stamping";
    /**
     * My profile's parts in their order, each with its symbol and a line saying what it holds (the owner, 2026-10-21: "In the
     * profile page, the sections need to be better delimited and labelled"; decision 231). The phone's are the same words.
     */
    static final String[] PARTS={"You","Your code","Connection","People and devices",STAMPING,"Text size","Security","Backup"};
    static final String[] ICONS={"user","qr-code","wifi","users","stamp","type","lock","database-backup"};
    static final String[] ABOUT={
        "Your name, colour, picture and Parlons! address.",
        "What another device scans to pair with this PC.",
        "Whether this PC is connected, and its addresses.",
        "The people and devices you share with, and your groups.",
        "Your Integritas account, which stamps notes and files on Minima.",
        "The size of everything in Mininotes, and what the pointer describes.",
        "Whether your notebook on this PC is locked with a password.",
        "One file holding every folder, note and attachment on this PC."};

    /**
     * A part on a card that stands out from the page: its symbol and name in the title role, the quiet line saying what it
     * holds, a rule under them, then its own things.
     */
    static JPanel part(String heading,String icon,String about,JComponent inside) {
        JPanel all=DesktopUi.column();
        JLabel named=new JLabel(Say.t(heading),DesktopIcons.glyph(icon,22),SwingConstants.LEFT);named.setIconTextGap(10);
        // The title role (DesktopUi.title), as a window's own name is.
        named.setFont(DesktopUi.BODY.deriveFont(Font.BOLD,20f));named.setForeground(DesktopUi.INK);named.setName("profilePart "+heading);
        DesktopUi.add(all,named);DesktopUi.gap(all,4);
        DesktopUi.Text said=DesktopUi.spread(about);said.setName("profileAbout "+heading);DesktopUi.add(all,said);DesktopUi.gap(all,DesktopUi.S+4);
        JPanel rule=new JPanel(){@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,1);}};
        rule.setBackground(DesktopUi.LINE);rule.setPreferredSize(new Dimension(1,1));DesktopUi.add(all,rule);DesktopUi.gap(all,DesktopUi.M);
        DesktopUi.add(all,inside);
        Color edge=DesktopUi.mix(DesktopUi.LINE,DesktopUi.INK,0.25f);
        JPanel card=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DesktopUi.CARD);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g.setColor(edge);g.setStroke(new BasicStroke(1.2f));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.dispose();
            }
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };
        card.setOpaque(false);card.putClientProperty("ground",DesktopUi.CARD);card.putClientProperty("part",heading);card.setName("profileCard "+heading);
        card.setBorder(BorderFactory.createEmptyBorder(DesktopUi.M+2,DesktopUi.L,DesktopUi.L,DesktopUi.L));card.add(all);card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    /** The part shown at the top of the page, as far as the page goes, and its field given the keyboard. */
    static void showAt(JScrollPane scroll,JComponent body,JComponent part,JComponent field) {
        scroll.validate();
        Rectangle where=SwingUtilities.convertRectangle(part.getParent(),part.getBounds(),body);
        where.y=Math.max(0,where.y-8);where.height=Math.max(where.height,scroll.getViewport().getHeight());
        body.scrollRectToVisible(where);
        if(field!=null&&field.isEnabled())field.requestFocusInWindow();
    }    static void render(Connection value,JLabel qr,JTextArea address,JTextArea permanent,JLabel state) {
        if(!address.getText().equals(value.address))address.setText(value.address);
        String permanentText=value.permanent.isEmpty()?"Not published yet":value.permanent;
        if(!permanent.getText().equals(permanentText))permanent.setText(permanentText);
        // Only between the owner's devices there is no relay to count: the address is this PC's door at home.
        state.setText(value.onlyMine?(value.address.isEmpty()?"Only between your devices · not on a network":"Only between your devices · no relay")
            :value.relays>0?"Connected · "+value.relays+(value.relays==1?" relay":" relays"):"Not connected: no relay has answered");
        if(!value.pairing.equals(qr.getClientProperty("pairing"))) {
            if(value.pairing.isEmpty()){qr.setIcon(null);qr.setText("<html><div style='width:150px'>"+Say.t(value.onlyMine?"Your code appears here once this PC is on your home network.":"Your code appears here once this PC is connected.")+"</div></html>");}
            else try{qr.setText("");qr.setIcon(new ImageIcon(DesktopQr.draw(Pairing.link(value.pairing),216)));}catch(Exception e){qr.setText(Say.t("Could not draw QR code"));return;}
            qr.putClientProperty("pairing",value.pairing);
        }
    }
    // ---- the help request, set up and controlled in Profile (decision 115) -------------------------------------------

    /** The set-up page the Help request row opens: who receives it, the message, the notes and folders that send it. */
    static void helpRequest(Desktop app,Window over) {
        app.disk.submit(()->new Object[]{app.store.addresses(),app.store.helpTriggers()},got->{
            Object[] g=(Object[])got;
            @SuppressWarnings("unchecked") java.util.List<NoteStore.Contact> all=(java.util.List<NoteStore.Contact>)g[0];
            java.util.List<NoteStore.Contact> paired=new java.util.ArrayList<>();
            for(NoteStore.Contact one:all)if(one.paired())paired.add(one);
            JPanel body=DesktopUi.column();
            DesktopUi.add(body,DesktopUi.note(Help.WHAT));
            DesktopUi.add(body,DesktopUi.note(Help.PC_PLACE));
            DesktopUi.add(body,DesktopUi.note("It is set up and controlled here. To turn it off, remove every note and folder below."));
            DesktopUi.add(body,DesktopUi.heading(Help.TO));
            java.util.Set<String> seeded=new java.util.LinkedHashSet<>(helpRecipients(app));
            final java.util.Map<String,JCheckBox> picks=new java.util.LinkedHashMap<>();
            if(paired.isEmpty())DesktopUi.add(body,DesktopUi.note("Nobody is paired yet. Pair someone in People and devices first."));
            for(NoteStore.Contact one:paired){JCheckBox box=DesktopUi.toggle(one.name,seeded.contains(one.address));picks.put(one.address,box);DesktopUi.add(body,DesktopUi.switchRow(one.name,box));}
            DesktopUi.add(body,DesktopUi.heading("Message"));
            final JTextField message=new JTextField(helpMessage(app),28);DesktopUi.add(body,message);
            DesktopUi.add(body,DesktopUi.heading("Notes and folders that send it"));
            final JPanel list=DesktopUi.column();DesktopUi.add(body,list);
            final java.util.function.Supplier<java.util.List<String>> chosen=()->{java.util.List<String> to=new java.util.ArrayList<>();for(java.util.Map.Entry<String,JCheckBox> e:picks.entrySet())if(e.getValue().isSelected())to.add(e.getKey());return to;};
            final JDialog[] box={null};
            final Runnable[] redraw={null};
            redraw[0]=()->app.disk.submit(()->app.store.helpTriggers(),now->{
                list.removeAll();
                if(now.isEmpty())DesktopUi.add(list,DesktopUi.quiet("None yet. Choose one below."));
                for(NoteStore.HelpTrigger t:now) {
                    JButton remove=app.button("Remove",()->app.disk.submit(()->{app.store.helpWhenOpened(t.kind,t.id,false,null,null);return null;},d->redraw[0].run(),app::failed));
                    DesktopUi.add(list,DesktopUi.row(DesktopUi.body(t.name),DesktopUi.actions(remove)));
                }
                list.revalidate();list.repaint();
            },app::failed);
            DesktopUi.add(body,DesktopUi.actions(app.button("Choose…",()->helpChoose(app,box[0],chosen.get(),message.getText().trim(),redraw[0]))));
            JButton done=DesktopUi.primary("Done",()->{saveHelp(app,chosen.get(),message.getText().trim());box[0].dispose();});
            box[0]=DesktopUi.sheet(over!=null?over:app.frame,"Help request",DesktopUi.scrolling(body),DesktopUi.footer(done),true);
            box[0].getRootPane().setDefaultButton(done);
            redraw[0].run();
            DesktopUi.show(box[0],540,700);
        },app::failed);
    }

    /** Choose an ordinary note or folder over Home's tree to send the help request (decision 115). */
    private static void helpChoose(Desktop app,Window over,java.util.List<String> to,String words,Runnable redraw) {
        if(to.isEmpty()){DesktopUi.tell(over,"Help request",DesktopUi.note(Help.NEED_SOMEONE));return;}
        app.disk.submit(()->{
            java.util.Set<String> already=new java.util.HashSet<>();
            for(NoteStore.HelpTrigger t:app.store.helpTriggers())already.add(t.id);
            java.util.List<NoteStore.Branch> pick=new java.util.ArrayList<>();
            for(NoteStore.Branch b:app.store.wholeTree())if(!already.contains(b.id))pick.add(b);
            return pick;
        },got->{
            @SuppressWarnings("unchecked") java.util.List<NoteStore.Branch> pick=(java.util.List<NoteStore.Branch>)got;
            if(pick.isEmpty()){DesktopUi.tell(over,"Help request",DesktopUi.note("Every note and folder is already chosen."));return;}
            NoteStore.Branch b=DesktopUi.pick(over,"Choose a note or folder","A help request is sent the moment it opens.",pick,
                x->(x.kind==NoteStore.Branch.Kind.COLLECTION?"Folder: ":"")+x.name,"Choose");
            if(b==null)return;
            app.disk.submit(()->{app.store.helpWhenOpened(b.kind,b.id,true,to,words);return null;},d->{persistHelp(app,to,words);redraw.run();},app::failed);
        },app::failed);
    }

    static java.util.List<String> helpRecipients(Desktop app) {
        java.util.List<String> to=new java.util.ArrayList<>();
        for(String a:app.context.getSharedPreferences("settings",0).getString("helpTo","").split("\n"))if(!a.trim().isEmpty())to.add(a.trim());
        return to;
    }
    static String helpMessage(Desktop app){return app.context.getSharedPreferences("settings",0).getString("helpWords","");}
    private static void persistHelp(Desktop app,java.util.List<String> to,String words) {
        app.context.getSharedPreferences("settings",0).edit()
            .putString("helpTo",to==null?"":String.join("\n",to)).putString("helpWords",words==null?"":words).apply();
    }
    /** Remember who receives the request and the message, and set them on every ordinary note and folder that sends it. */
    private static void saveHelp(Desktop app,java.util.List<String> to,String words) {
        persistHelp(app,to,words);
        java.util.List<String> recipients=to==null?new java.util.ArrayList<>():to;String message=words==null?"":words;
        app.disk.submit(()->{for(NoteStore.HelpTrigger t:app.store.helpTriggers())app.store.helpWhenOpened(t.kind,t.id,true,recipients,message);return null;},d->{},app::failed);
    }

    /** A field that keeps what is typed in it as it is typed. */
    private static void saveAsTyped(JTextField field,Runnable save) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){save.run();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){save.run();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){save.run();}
        });
    }
    static JPanel column(){JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));panel.setBackground(Desktop.PAPER);panel.setBorder(BorderFactory.createEmptyBorder(20,24,20,24));return panel;}
    static void space(JPanel panel){panel.add(Box.createVerticalStrut(16));}
    static JTextArea readonly(int rows){JTextArea area=new JTextArea(rows,35);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(false);area.setBackground(new Color(246,245,239));area.setForeground(Desktop.QUIET);area.setFont(new Font("Consolas",Font.PLAIN,12));area.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopUi.LINE),BorderFactory.createEmptyBorder(8,10,8,10)));((javax.swing.text.DefaultCaret)area.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);return area;}
    static void finish(JDialog dialog,JPanel body,int width,int height){
        for(Component child:body.getComponents())if(child instanceof JComponent component)component.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane scroll=new JScrollPane(body);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getVerticalScrollBar().setUnitIncrement(20);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setContentPane(scroll);dialog.setSize(width,height);dialog.setLocationRelativeTo(dialog.getOwner());
    }
    static void clipboard(String text){Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),null);}
}
