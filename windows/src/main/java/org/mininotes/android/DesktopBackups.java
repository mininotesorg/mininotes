package org.mininotes.android;

import java.awt.*;
import java.nio.file.*;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * The Backup page on the PC (decision 329): the folders backups go to, each with what goes in, when, how many are kept and
 * what opens them; Back up now; made by themselves at their time, checked each minute, and when Mininotes closes where that is
 * chosen. Always sealed. The rules are {@link Backups}; the file is the one Export writes (DesktopBackup), so Add from a
 * backup opens it on any phone or PC.
 */
final class DesktopBackups {
    private DesktopBackups(){}

    /** Where the places are kept: the PC's settings, never the notebook (a backup restored elsewhere brings none). */
    static final String KEPT="backups";
    private static final ZoneId ZONE=ZoneId.systemDefault();

    static List<Backups.Place> places(Desktop app){return Backups.read(app.context.getSharedPreferences("settings",0).getString(KEPT,""));}
    static synchronized void keep(Desktop app,List<Backups.Place> places){app.context.getSharedPreferences("settings",0).edit().putString(KEPT,Backups.said(places)).apply();}
    /** One place's news written back, the others as they are now (the page may have changed them meanwhile). */
    static synchronized void update(Desktop app,Backups.Place one) {
        List<Backups.Place> all=places(app);
        for(int i=0;i<all.size();i++)if(all.get(i).id.equals(one.id)){Backups.Place now=all.get(i);now.last=one.last;now.tried=one.tried;now.said=one.said;now.fingerprint=one.fingerprint;}
        keep(app,all);
    }

    // ---- making one -------------------------------------------------------------------------------------------

    /**
     * One backup into its folder, on the disk's thread. Not asked for and nothing changed since the last one made here: none
     * is made, and that is said. Once it is whole, this PC's own backups there beyond the number kept are removed.
     */
    static Backups.Said make(Desktop app,Backups.Place p,boolean asked,Slots pages){return make(app.context,app.store,p,asked,pages);}
    static Backups.Said make(org.mininotes.desktop.platform.content.Context context,NoteStore store,Backups.Place p,boolean asked,Slots pages) {
        long now=System.currentTimeMillis();p.tried=now;
        if(p.toDevice())return waiting(context,store,p,asked,pages,now);
        try {
            Path folder=Path.of(p.where);
            if(p.where.isEmpty()||!Files.isDirectory(folder))return p.said=Backups.Said.NO_FOLDER;
            byte[][] sealed=key(context,store,p);
            if(sealed==null)return p.said=Backups.Said.NO_KEY;
            String fingerprint=Backups.fingerprint(store.backup(),ids(store),p.what);
            if(!asked&&!p.fingerprint.isEmpty()&&p.fingerprint.equals(fingerprint)&&(p.said==Backups.Said.MADE||p.said==Backups.Said.SAME)) {
                p.last=now;return p.said=Backups.Said.SAME;
            }
            long needs=(p.what==Backups.What.EVERYTHING?store.weight():0)+(8L<<20);
            try{if(Files.getFileStore(folder).getUsableSpace()<needs)return p.said=Backups.Said.NO_ROOM;}catch(Exception unknown){/* tried all the same */}
            String device=Node.nameHere(context);
            Path target=folder.resolve(Backups.fileName(device,now,ZONE));
            // Never over another: two in the same second wait for the next.
            if(Files.exists(target))return p.said=Backups.Said.SAME;
            DesktopBackup.writeSaved(store,target,sealed[0],sealed[1],pages,p.what==Backups.What.EVERYTHING);
            p.last=now;p.fingerprint=fingerprint;p.said=Backups.Said.MADE;
            List<String> names=new ArrayList<>();
            try(DirectoryStream<Path> in=Files.newDirectoryStream(folder)){for(Path one:in)names.add(one.getFileName().toString());}
            for(String old:Backups.beyond(names,device,p.keep))try{Files.deleteIfExists(folder.resolve(old));}catch(Exception kept){/* tried again next time */}
            // When, for the reminder that proposes one (decision 235).
            context.getSharedPreferences("settings",0).edit().putString(DesktopBackup.BACKED_UP,Long.toString(now)).apply();
            return p.said;
        } catch(Exception failed) {
            org.mininotes.desktop.platform.util.Log.w("Mininotes/Backup","a backup not made: "+failed.getClass().getSimpleName());
            return p.said=Backups.Said.FAILED;
        }
    }
    // ---- to another of my devices (decision 330) -------------------------------------------------------------------

    /** Where a backup for a device waits until it is pushed: beside the notebook, one per place, the newest only. */
    static Path outgoing(org.mininotes.desktop.platform.content.Context context,Backups.Place p) {
        return context.getFilesDir().toPath().resolve("outgoing-backups").resolve(p.id.replaceAll("[^A-Za-z0-9-]",""));
    }
    /** The one waiting for this place, or null. */
    static Path waitingFile(org.mininotes.desktop.platform.content.Context context,Backups.Place p) {
        Path folder=outgoing(context,p);
        if(!Files.isDirectory(folder))return null;
        try(DirectoryStream<Path> in=Files.newDirectoryStream(folder,Backups.START+"*"+Backups.ENDING)){for(Path one:in)return one;}catch(Exception none){/* none */}
        return null;
    }

    /**
     * A backup for a device, sealed and put to wait beside the notebook, an older one waiting replaced: quick, on the disk's
     * thread. It is pushed after, off it ({@link #push}). Said NONE while it waits to go.
     */
    private static Backups.Said waiting(org.mininotes.desktop.platform.content.Context context,NoteStore store,Backups.Place p,boolean asked,Slots pages,long now) {
        try {
            byte[][] sealed=key(context,store,p);
            if(sealed==null)return p.said=Backups.Said.NO_KEY;
            String fingerprint=Backups.fingerprint(store.backup(),ids(store),p.what);
            if(!asked&&!p.fingerprint.isEmpty()&&p.fingerprint.equals(fingerprint)&&(p.said==Backups.Said.MADE||p.said==Backups.Said.SAME)&&waitingFile(context,p)==null) {
                p.last=now;return p.said=Backups.Said.SAME;
            }
            Path folder=outgoing(context,p);Files.createDirectories(folder);
            Path target=folder.resolve(Backups.fileName(Node.nameHere(context),now,ZONE));
            if(Files.exists(target))return p.said;
            DesktopBackup.writeSaved(store,target,sealed[0],sealed[1],pages,p.what==Backups.What.EVERYTHING);
            try(DirectoryStream<Path> in=Files.newDirectoryStream(folder)){for(Path one:in)if(!one.equals(target))Files.deleteIfExists(one);}
            p.fingerprint=fingerprint;
            return p.said=Backups.Said.NONE;
        } catch(Exception failed) {
            org.mininotes.desktop.platform.util.Log.w("Mininotes/Backup","a backup for a device not made: "+failed.getClass().getSimpleName());
            return p.said=Backups.Said.FAILED;
        }
    }

    /** The pushes going now, by place: one at a time each. */
    private static final Set<String> pushing=Collections.synchronizedSet(new HashSet<>());
    /** Pushes run here, never on the disk's thread: a backup takes minutes to go. */
    private static final java.util.concurrent.ExecutorService PUSHES=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mininotes-backup-push");t.setDaemon(true);return t;});

    /**
     * The backup waiting for this place, pushed to its device; once that device has it whole, it is no longer kept here. Not
     * reached, or not taken: it waits, and is tried again in half an hour, going on from where it stopped.
     */
    static void push(Desktop app,Backups.Place p,boolean asked,Runnable after) {
        Path file=waitingFile(app.context,p);
        if(file==null||app.offline||!pushing.add(p.id)){if(after!=null)SwingUtilities.invokeLater(after);return;}
        if(asked){app.status.setToolTipText(null);app.status.setText(Say.t("Sending the backup to {0}…",p.shown));}
        PUSHES.execute(()->{
            Backups.Said went;
            try {
                long bytes=Files.size(file);
                went=Post.pushBig(app.context,app.store,app.keys,p.device,file.getFileName().toString(),Backups.kindKeeping(p.keep),bytes,
                    at->{java.io.InputStream in=Files.newInputStream(file);in.skipNBytes(at);return in;});
                if(went==Backups.Said.MADE)Files.deleteIfExists(file);
            } catch(Exception failed){went=Backups.Said.NOT_TAKEN;}
            final Backups.Said said=went;
            SwingUtilities.invokeLater(()->{
                pushing.remove(p.id);
                Backups.Place now=null;for(Backups.Place one:places(app))if(one.id.equals(p.id))now=one;
                if(now!=null) {
                    now.tried=System.currentTimeMillis();now.said=said;
                    if(said==Backups.Said.MADE){now.last=now.tried;app.context.getSharedPreferences("settings",0).edit().putString(DesktopBackup.BACKED_UP,Long.toString(now.last)).apply();}
                    update(app,now);
                    if(asked||said==Backups.Said.MADE)app.status.setText(said==Backups.Said.MADE?Say.t("Backed up to {0}",p.shown):Backups.lastSaid(now,ZONE));
                }
                if(after!=null)after.run();
            });
        });
    }

    /** The key and the lock it is sealed with: the notebook's while it is open, or the one made for this place. */
    private static byte[][] key(org.mininotes.desktop.platform.content.Context context,NoteStore store,Backups.Place p) throws Exception {
        if(p.opens==Backups.Opens.NOTEBOOK) {
            byte[] key=context.databaseKey();Path lock=context.getFilesDir().toPath().resolve(DesktopLock.KEPT);
            return key==null||!Files.isRegularFile(lock)?null:new byte[][]{key,Files.readAllBytes(lock)};
        }
        String key=store.secret(Backups.secretOf(p.id));
        if(key.isEmpty()||p.lock.isEmpty())return null;
        return new byte[][]{Base64.getDecoder().decode(key),Base64.getDecoder().decode(p.lock)};
    }
    private static List<String> ids(NoteStore store){List<String> out=new ArrayList<>();for(NoteStore.Held one:store.everyFile())out.add(one.id);return out;}

    /** Said each minute: every place whose time has come, made one after another behind the writing. */
    static void tick(Desktop app) {
        if(app.closing)return;
        long now=System.currentTimeMillis();
        for(Backups.Place p:places(app)) {
            if(Backups.due(p,now,ZONE))run(app,p,false);
            // One waiting for its device, from closing or a try that did not reach it: pushed again each half hour.
            else if(Backups.pushAgain(p,now)&&waitingFile(app.context,p)!=null){p.tried=now;update(app,p);push(app,p,false,null);}
        }
    }
    /** Mininotes closing: those made then, on the disk's thread before the notebook closes. Blocking there. */
    static void onClose(Desktop app,Slots pages) {
        long now=System.currentTimeMillis();
        for(Backups.Place p:places(app))if(Backups.dueOnClose(p,now)){make(app,p,false,pages);update(app,p);}
    }
    private static final Set<String> going=Collections.synchronizedSet(new HashSet<>());
    /** One made, the bar saying it where it was asked for, and quietly where it was not; the page drawn again after. */
    static void run(Desktop app,Backups.Place p,boolean asked){run(app,p,asked,null);}
    static void run(Desktop app,Backups.Place p,boolean asked,Runnable after) {
        if(!going.add(p.id))return;
        if(asked){app.status.setToolTipText(null);app.status.setText(Say.t("Backing up to {0}…",p.shown));}
        Slots pages=app.privately.slots();
        app.disk.submit(()->make(app,p,asked,pages),said->{
            going.remove(p.id);update(app,p);
            // For a device: sealed and waiting; now it goes, off the disk's thread.
            if(p.toDevice()&&said==Backups.Said.NONE){push(app,p,asked,after);return;}
            if(said==Backups.Said.MADE)app.status.setText(Say.t("Backed up to {0}",p.shown));
            else if(said==Backups.Said.SAME){if(asked)app.status.setText(Say.t("Nothing changed since the last backup"));}
            else app.status.setText(Backups.lastSaid(p,ZONE));
            if(after!=null)after.run();
        },failed->{going.remove(p.id);app.failed(failed);if(after!=null)after.run();});
    }

    // ---- the page ---------------------------------------------------------------------------------------------

    /** The page: each folder as a card, what it does and how it went; Add a folder…; and the way to put one back. */
    static void open(Desktop app) {
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        Runnable[] draw={null};
        draw[0]=()->{
            body.removeAll();
            DesktopUi.add(body,DesktopUi.note(Say.t("Backups are always encrypted. Each place has its own times, and keeps its newest copies: a folder, or another of your devices, such as your own server. Private spaces are never backed up."),420,DesktopUi.QUIET,DesktopUi.BODY));
            DesktopUi.gap(body,DesktopUi.M);
            List<Backups.Place> all=places(app);
            if(all.isEmpty())DesktopUi.add(body,DesktopUi.body("No place for backups yet"));
            for(Backups.Place p:all) {
                JPanel lines=DesktopUi.column();
                // On or off, first, where it is seen (the owner, 2026-11-07: "a button to turn on and off the backup").
                JCheckBox on=DesktopUi.toggle("Back up here",p.on);
                on.addActionListener(e->{
                    List<Backups.Place> every=places(app);
                    for(Backups.Place one:every)if(one.id.equals(p.id))one.on=on.isSelected();
                    keep(app,every);draw[0].run();
                });
                lines.add(DesktopUi.switchRow("Back up here",on));
                DesktopUi.add(lines,DesktopUi.note(Backups.when(p,false),400,p.on?DesktopUi.INK:DesktopUi.QUIET,DesktopUi.BODY));
                String what=Backups.whatNames()[p.what.ordinal()]+"  ·  "+Backups.opensNames(true)[p.opens.ordinal()]+"  ·  "+Say.t("Keeps {0}",String.valueOf(p.keep));
                DesktopUi.add(lines,DesktopUi.note(what,400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
                DesktopUi.gap(lines,4);
                DesktopUi.Text last=DesktopUi.note(Backups.lastSaid(p,ZONE),400,failed(p)?DesktopUi.WARN:DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f));
                DesktopUi.add(lines,last);
                String next=Backups.nextSaid(p,ZONE);if(!next.isEmpty())DesktopUi.add(lines,DesktopUi.note(next,400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
                DesktopUi.gap(lines,DesktopUi.S);
                // A folder that is gone or full: the way out first (the owner, 2026-11-07, "present the solution").
                if(Backups.wantsFolder(p)){DesktopUi.add(lines,DesktopUi.actions(DesktopUi.primary("Choose another folder…",()->edit(app,box[0],p,draw[0]))));DesktopUi.gap(lines,DesktopUi.S);}
                DesktopUi.add(lines,DesktopUi.actions(
                    DesktopUi.button("Back up now",()->run(app,p,true,draw[0])),
                    DesktopUi.button("Change…",()->edit(app,box[0],p,draw[0])),
                    DesktopUi.button("Remove…",()->{
                        if(!DesktopUi.confirm(box[0],"Remove this place for backups?",Say.t("Backups stop going to {0}. The backups already there stay.",p.shown),"Remove",true))return;
                        List<Backups.Place> left=places(app);left.removeIf(one->one.id.equals(p.id));keep(app,left);
                        app.disk.submit(()->{app.store.setSecret(Backups.secretOf(p.id),"");return null;},done->draw[0].run(),app::failed);
                    })));
                DesktopUi.add(body,DesktopUi.card(p.shown,lines));
                DesktopUi.gap(body,DesktopUi.S);
            }
            DesktopUi.gap(body,DesktopUi.S);
            DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Add from a backup…",()->{box[0].dispose();app.save(app::importBackup);})));
            // What my other devices send here (decision 330): where it is kept.
            DesktopUi.gap(body,DesktopUi.M);
            JPanel from=DesktopUi.column();
            DesktopUi.add(from,DesktopUi.note(Say.t("Backups your other devices send to this PC are kept in {0}.",received(app).toString()),400,DesktopUi.INK,DesktopUi.BODY.deriveFont(13f)));
            DesktopUi.gap(from,DesktopUi.S);
            DesktopUi.add(from,DesktopUi.actions(
                DesktopUi.button("Change…",()->{
                    Path got=DesktopPick.folder(box[0],"Keep my devices' backups in this folder","Choose");
                    if(got!=null){app.context.getSharedPreferences("settings",0).edit().putString(RECEIVED,got.toAbsolutePath().toString()).apply();draw[0].run();}
                }),
                DesktopUi.button("Show in Explorer",()->{try{Files.createDirectories(received(app));java.awt.Desktop.getDesktop().open(received(app).toFile());}catch(Exception e){app.failed(e);}})));
            DesktopUi.add(body,DesktopUi.card(Say.t("From your other devices"),from));
            body.revalidate();body.repaint();
        };
        draw[0].run();
        JButton add=DesktopUi.primary("Add a place…",()->edit(app,box[0],null,draw[0]));
        JButton done=DesktopUi.button("Done",()->box[0].dispose());
        box[0]=DesktopUi.sheet(app.frame,"Backup",DesktopUi.scrolling(body),DesktopUi.footer(done,add),true);
        DesktopUi.show(box[0],520,640);
    }
    private static boolean failed(Backups.Place p){return p.said!=Backups.Said.NONE&&p.said!=Backups.Said.MADE&&p.said!=Backups.Said.SAME;}

    private static final String[] HOURS=new String[24];
    static{for(int h=0;h<24;h++)HOURS[h]=String.format(Locale.ROOT,"%02d:00",h);}
    private static final int[] KEEPS={1,3,5,10,20,50};

    /** A folder added or changed: where, what, when, how many kept, what opens it. Save makes the lock where one is new. */
    static void edit(Desktop app,Window owner,Backups.Place was,Runnable after) {
        Backups.Place p=was==null?new Backups.Place():was.copy();
        boolean locked=app.context.databaseKey()!=null;
        if(was==null&&locked)p.opens=Backups.Opens.NOTEBOOK;
        if(!locked&&p.opens==Backups.Opens.NOTEBOOK)p.opens=Backups.Opens.PASSWORD;
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        // Where: a folder, or another of my devices (decision 330), a backup server among them.
        List<NoteStore.Contact> devices=new ArrayList<>();
        for(NoteStore.Contact one:app.store.addresses())if(one.mine&&one.paired())devices.add(one);
        String[] whereNames=new String[devices.size()+1];whereNames[0]=Say.t("A folder");
        int whereAt=0;
        for(int i=0;i<devices.size();i++){whereNames[i+1]=devices.get(i).name;if(devices.get(i).address.equals(p.device))whereAt=i+1;}
        JComboBox<String> whereTo=DesktopUi.combo(whereNames);whereTo.setSelectedIndex(whereAt);
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("Where"),whereTo));
        DesktopUi.gap(body,DesktopUi.S);
        String[] folderWas={p.toDevice()?"":p.where};
        DesktopUi.Text folder=DesktopUi.quiet(folderWas[0].isEmpty()?Say.t("No folder chosen"):folderWas[0]);
        JPanel folderRow=DesktopUi.row(folder,DesktopUi.button("Choose…",()->{
            Path got=DesktopPick.folder(box[0],"Back up into this folder","Choose");
            if(got!=null){folderWas[0]=got.toAbsolutePath().toString();folder.setText(folderWas[0]);}
        }));
        DesktopUi.add(body,folderRow);
        DesktopUi.Text deviceSays=DesktopUi.note(Say.t("Sent sealed to that device when it can be reached directly: it keeps the backup and cannot open it."),400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f));
        JPanel deviceRow=DesktopUi.column();DesktopUi.add(deviceRow,deviceSays);
        DesktopUi.add(body,deviceRow);
        DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.actions(DesktopUi.button("Your own server…",()->ownServer(app,box[0]))));
        Runnable whereShown=()->{
            boolean toFolder=whereTo.getSelectedIndex()==0;
            folderRow.setVisible(toFolder);deviceRow.setVisible(!toFolder);
            body.revalidate();if(box[0]!=null)box[0].pack();
        };
        whereTo.addActionListener(e->whereShown.run());whereShown.run();
        DesktopUi.gap(body,DesktopUi.M);
        JComboBox<String> what=DesktopUi.combo(Backups.whatNames());what.setSelectedIndex(p.what.ordinal());
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("What"),what));
        DesktopUi.gap(body,DesktopUi.S);
        JComboBox<String> often=DesktopUi.combo(Backups.oftenNames(false));often.setSelectedIndex(p.often.ordinal());
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("When"),often));
        JComboBox<String> hour=new JComboBox<>(HOURS);hour.setSelectedIndex(p.hour);
        String[] days=new String[7];for(int d=1;d<=7;d++)days[d-1]=Backups.dayName(d);
        JComboBox<String> weekday=new JComboBox<>(days);weekday.setSelectedIndex(p.weekday-1);
        String[] dates=new String[28];for(int d=1;d<=28;d++)dates[d-1]=String.valueOf(d);
        JComboBox<String> monthday=new JComboBox<>(dates);monthday.setSelectedIndex(p.monthday-1);
        JPanel dayRow=DesktopUi.row(DesktopUi.body("Day"),weekday),dateRow=DesktopUi.row(DesktopUi.body("Day of the month"),monthday),hourRow=DesktopUi.row(DesktopUi.body("At"),hour);
        DesktopUi.add(body,dayRow);DesktopUi.add(body,dateRow);DesktopUi.add(body,hourRow);
        Runnable shown=()->{
            Backups.Often o=Backups.Often.values()[often.getSelectedIndex()];
            dayRow.setVisible(o==Backups.Often.WEEKLY);dateRow.setVisible(o==Backups.Often.MONTHLY);
            hourRow.setVisible(o==Backups.Often.DAILY||o==Backups.Often.WEEKLY||o==Backups.Often.MONTHLY);
            body.revalidate();if(box[0]!=null)box[0].pack();
        };
        often.addActionListener(e->shown.run());shown.run();
        DesktopUi.gap(body,DesktopUi.S);
        String[] keepNames=new String[KEEPS.length];int keepAt=2;
        for(int i=0;i<KEEPS.length;i++){keepNames[i]=KEEPS[i]==1?Say.t("The newest only"):Say.t("The newest {0}",String.valueOf(KEEPS[i]));if(KEEPS[i]==p.keep)keepAt=i;}
        JComboBox<String> keep=new JComboBox<>(keepNames);keep.setSelectedIndex(keepAt);
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("Keep"),keep));
        DesktopUi.gap(body,DesktopUi.S);
        JComboBox<String> opens=DesktopUi.combo(Backups.opensNames(locked));opens.setSelectedIndex(Backups.indexOf(p.opens,locked));
        DesktopUi.add(body,DesktopUi.row(DesktopUi.body("Opened with"),opens));
        DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.note(Say.t("Whichever opens it is needed to put a backup back. If it is lost, so are the backups."),400,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(12.5f)));
        DesktopUi.Text wrong=DesktopUi.quiet(" ");wrong.setForeground(DesktopUi.WARN);DesktopUi.gap(body,6);DesktopUi.add(body,wrong);
        JButton save=DesktopUi.primary("Save",()->{
            if(whereTo.getSelectedIndex()==0) {
                if(folderWas[0].isEmpty()){wrong.setText(Say.t("Choose a folder first."));return;}
                p.where=folderWas[0];p.shown=p.where;p.device="";
            } else {
                NoteStore.Contact to=devices.get(whereTo.getSelectedIndex()-1);
                p.device=to.address;p.where="";p.shown=to.name;
            }
            p.what=Backups.What.values()[what.getSelectedIndex()];p.often=Backups.Often.values()[often.getSelectedIndex()];
            p.hour=hour.getSelectedIndex();p.weekday=weekday.getSelectedIndex()+1;p.monthday=monthday.getSelectedIndex()+1;p.keep=KEEPS[keep.getSelectedIndex()];
            Backups.Opens chosen=Backups.opensAt(opens.getSelectedIndex(),locked);
            // A new lock where the way to open it changed, or none was made yet: asked for, and its key kept in the notebook.
            boolean fresh=was==null||chosen!=was.opens||(chosen!=Backups.Opens.NOTEBOOK&&p.lock.isEmpty());
            Vault.Made made=null;
            try {
                if(fresh&&chosen==Backups.Opens.PASSWORD) {
                    char[] password=password(box[0]);if(password==null)return;
                    try{made=Vault.make(password);}finally{Arrays.fill(password,' ');}
                } else if(fresh&&chosen==Backups.Opens.WORDS) {
                    made=Vault.makeWithoutPassword();
                    if(!DesktopLock.showWords(box[0],made.words,true,true)||!DesktopLock.checkWords(box[0],made.words,"Use these words"))return;
                } else if(fresh&&chosen==Backups.Opens.KEY_FILE) {
                    // A key file, saved where the owner chooses, away from the backups (step 3 of decision 329).
                    String secret=Backups.newSecret();
                    if(!DesktopUi.confirm(box[0],"Save the key file",Say.t("Keep it apart from the backups, on a USB key for instance: whoever has both can open them, and without it nobody can."),"Choose where",false))return;
                    Path keyAt=DesktopPick.save(box[0],"Save the key file",Backups.keyFileName(Node.nameHere(app.context)));
                    if(keyAt==null)return;
                    Files.writeString(keyAt,Backups.keyFile(secret),java.nio.charset.StandardCharsets.UTF_8);
                    made=Vault.make(secret.toCharArray());
                } else if(fresh&&chosen==Backups.Opens.DEVICE) {
                    // This device only: the secret in its own settings, never in the notebook nor a backup (step 3 of decision 329).
                    if(!DesktopUi.confirm(box[0],"This device only?",Say.t("Only this PC can open these backups. If it is lost, or Mininotes is removed from it, so are they."),"Use this device only",false))return;
                    String secret=Backups.newSecret();
                    var settings=app.context.getSharedPreferences("settings",0);
                    settings.edit().putStringSet(Backups.HERE_KEYS,Backups.withHere(settings.getStringSet(Backups.HERE_KEYS,new HashSet<>()),secret)).apply();
                    made=Vault.make(secret.toCharArray());
                }
            } catch(Exception e){app.failed(e);return;}
            p.opens=chosen;
            if(made!=null)p.lock=Base64.getEncoder().encodeToString(made.kept);
            if(chosen==Backups.Opens.NOTEBOOK)p.lock="";
            // Changed, the next one is due from now, not from when it was first set.
            if(was!=null&&(was.often!=p.often||was.hour!=p.hour||was.weekday!=p.weekday||was.monthday!=p.monthday))p.set=System.currentTimeMillis();
            byte[] key=made==null?null:made.key;
            app.disk.submit(()->{
                if(key!=null)app.store.setSecret(Backups.secretOf(p.id),Base64.getEncoder().encodeToString(key));
                if(p.opens==Backups.Opens.NOTEBOOK)app.store.setSecret(Backups.secretOf(p.id),"");
                return null;
            },done->{
                synchronized(DesktopBackups.class){List<Backups.Place> all=places(app);boolean found=false;
                    for(int i=0;i<all.size();i++)if(all.get(i).id.equals(p.id)){all.set(i,p);found=true;}
                    if(!found)all.add(p);keep(app,all);}
                box[0].dispose();if(after!=null)after.run();
                // A first one made now, so the folder is known to work.
                if(was==null)run(app,p,true,after);
            },app::failed);
        });
        JButton cancel=DesktopUi.button("Cancel",()->box[0].dispose());
        box[0]=DesktopUi.sheet(owner,was==null?"Add a place for backups":"Change this backup",body,DesktopUi.footer(cancel,save),true);
        box[0].getRootPane().setDefaultButton(save);
        DesktopUi.show(box[0],500,640);
    }

    // ---- kept here for my other devices (decision 330) ------------------------------------------------------------

    /** Where the backups my other devices push to this PC are kept: in the settings, Documents\Mininotes backups at first. */
    static final String RECEIVED="backupsReceived";
    static Path received(Desktop app) {
        String kept=app.context.getSharedPreferences("settings",0).getString(RECEIVED,"");
        return kept.isEmpty()?Path.of(System.getProperty("user.home"),"Documents","Mininotes backups"):Path.of(kept);
    }
    /** Pushed backups taken in here: each device's in its own folder, its oldest beyond what it asked for removed. */
    static void listen(Desktop app) {
        Big.pushed=one->{
            String not=Keeping.take(received(app),one,null,kept->SwingUtilities.invokeLater(()->app.status.setText(Say.t("A backup from {0} is kept here",one.name))));
            if(not!=null)org.mininotes.desktop.platform.util.Log.i("Mininotes/Backup","a backup pushed here not taken: "+not);
        };
    }

    /** The guide to a backup server of one's own, and the line that installs it (decision 330). */
    static final String SERVER_GUIDE="https://github.com/mininotesorg/server";
    static final String SERVER_INSTALL="curl -fsSL https://raw.githubusercontent.com/mininotesorg/server/main/install.sh | sudo bash";

    /**
     * Your own server…: what a backup server is, the line that installs it on a Linux server, the one that shows its code, and
     * the guide (the owner, 2026-11-05: "from the app we should made available the linux script to be installed").
     */
    static void ownServer(Desktop app,Window owner) {
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(Say.t("A small Mininotes for a Linux server that keeps your backups. It is one of your devices, always on, and it cannot open what it keeps: every backup is sealed here first."),420,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.heading("1. Install it"));DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.note(Say.t("On the server, as root, run:"),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));DesktopUi.gap(body,4);
        DesktopUi.add(body,copyable(SERVER_INSTALL,app));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.heading("2. Pair it"));DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.note(Say.t("Then run the line below. It shows a code: in People and devices, choose +, scan it or paste its link, then One of my devices and Nothing yet."),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));DesktopUi.gap(body,4);
        DesktopUi.add(body,copyable("sudo mininotes-server pair",app));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.heading("3. Back up to it"));DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.note(Say.t("Here, choose Add a place…, then the server under Where."),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
        JButton guide=DesktopUi.button("Open the guide",()->{try{Desktop.browse(SERVER_GUIDE);}catch(Exception e){app.failed(e);}});
        JButton done=DesktopUi.primary("Done",()->box[0].dispose());
        box[0]=DesktopUi.sheet(owner,"Your own server",body,DesktopUi.footer(guide,done),true);
        DesktopUi.show(box[0],540,560);
    }
    /**
     * Your own relay… (Settings, Relays; the owner's OK, 2026-11-07): what it is, the line that installs it (the same as the
     * backup server's, which installs both), where its address goes, and the guide.
     */
    static void ownRelay(Desktop app,Window owner) {
        JDialog[] box={null};
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(Say.t("A relay on a server of yours carries your notes when your devices cannot reach each other directly. It passes on only what is sealed, and cannot read it."),420,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.heading("1. Install it"));DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.note(Say.t("On the server, as root, run:"),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));DesktopUi.gap(body,4);
        DesktopUi.add(body,copyable(SERVER_INSTALL,app));
        DesktopUi.gap(body,DesktopUi.M);
        DesktopUi.add(body,DesktopUi.heading("2. Add it here"));DesktopUi.gap(body,4);
        DesktopUi.add(body,DesktopUi.note(Say.t("At the end it shows the relay's address, like 203.0.113.7:9501. Type it in the relay field, then Add."),420,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
        JButton guide=DesktopUi.button("Open the guide",()->{try{Desktop.browse(SERVER_GUIDE);}catch(Exception e){app.failed(e);}});
        JButton done=DesktopUi.primary("Done",()->box[0].dispose());
        box[0]=DesktopUi.sheet(owner,"Your own relay",body,DesktopUi.footer(guide,done),true);
        DesktopUi.show(box[0],540,420);
    }
    /** A line to copy, in a field that can be selected, with Copy beside it. */
    static JPanel copyable(String line,Desktop app) {
        JTextField field=new JTextField(line);field.setEditable(false);field.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));field.setCaretPosition(0);
        return DesktopUi.row(field,DesktopUi.button("Copy",()->{
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(line),null);
            app.status.setText(Say.t("Copied"));
        }));
    }

    /** A password for the backups, typed twice; null where the box was closed. */
    private static char[] password(Window owner) {
        JDialog[] box={null};char[][] said={null};
        JPasswordField first=new JPasswordField(24),again=new JPasswordField(24);
        DesktopUi.Text wrong=DesktopUi.quiet(" ");wrong.setForeground(DesktopUi.WARN);
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note("Choose a password for these backups. It opens them on any phone or PC, and nothing else can: if it is lost, so are the backups.",380,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,12);
        DesktopUi.add(body,DesktopUi.quiet("Backup password"));DesktopUi.gap(body,4);DesktopUi.add(body,first);DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.quiet("The same again"));DesktopUi.gap(body,4);DesktopUi.add(body,again);DesktopUi.gap(body,6);DesktopUi.add(body,wrong);
        JButton go=DesktopUi.primary("Use this password",()->{
            String problem=DesktopLock.problem(first.getPassword(),again.getPassword());
            if(problem!=null){wrong.setText(Say.t(problem));return;}
            said[0]=first.getPassword();box[0].dispose();
        });
        box[0]=DesktopUi.sheet(owner,"Backup password",body,DesktopUi.footer(go),true);
        box[0].getRootPane().setDefaultButton(go);box[0].getRootPane().putClientProperty("focus",first);DesktopUi.show(box[0],460,460);
        return said[0];
    }
}
