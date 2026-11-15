// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * The backup server (decision 330; the owner, 2026-11-05: "could we have a cli script of mininote for server just to receive the
 * backup, mainly Linux servers?"): Mininotes with no screen, on the owner's own Linux server. It pairs as one of the owner's
 * devices from a code shown in the terminal, says to each of them that it only keeps backups (so it is sent nothing else), and
 * keeps the backups they push to it, each device's in a folder of its own, sealed as they made them: it cannot open one.
 *
 * <pre>
 *   mininotes-server run    [--data DIR] [--backups DIR] [--public HOST] [--name NAME]   the service itself
 *   mininotes-server pair   [--data DIR]    a code to scan or paste, valid 15 minutes, then who paired
 *   mininotes-server status [--data DIR]    the devices paired and the backups kept
 * </pre>
 *
 * The commands talk to the running service through files in its folder (pair/ask, pair/code, pair/paired, status.txt), so they
 * need no port of their own and run as the service's user.
 */
public final class Server {
    private Server(){}

    static final String VERSION=Server.class.getPackage()==null||Server.class.getPackage().getImplementationVersion()==null?"dev":Server.class.getPackage().getImplementationVersion();
    /** How long a code shown is taken without asking: as on the phone and the PC, showing it is the decision. */
    static final long CODE_FOR=15*60_000L;

    public static void main(String[] args) throws Exception {
        Map<String,String> o=options(args);
        String command=args.length==0||args[0].startsWith("--")?"help":args[0];
        Path data=Path.of(o.getOrDefault("data","/var/lib/mininotes-backup-server"));
        switch(command) {
            case "run": run(data,o);break;
            case "pair": System.exit(pair(data));break;
            case "status": System.exit(status(data));break;
            case "version": System.out.println("Mininotes backup server "+VERSION);break;
            default:
                System.out.println("Mininotes backup server "+VERSION+"\n\n"
                    +"  run    [--data DIR] [--backups DIR] [--public HOST] [--name NAME]   the service (systemd starts it)\n"
                    +"  pair   [--data DIR]   show a code to add this server as one of your devices\n"
                    +"  status [--data DIR]   the devices paired, and the backups kept\n\n"
                    +"Guide: https://github.com/mininotesorg/server");
        }
    }

    static Map<String,String> options(String[] args) {
        Map<String,String> out=new HashMap<>();
        for(int i=0;i<args.length;i++)if(args[i].startsWith("--")&&i+1<args.length){out.put(args[i].substring(2),args[i+1]);i++;}
        return out;
    }

    // ---- the service --------------------------------------------------------------------------------------------

    private static final Map<String,Sharing.Level> OFFERS=new ConcurrentHashMap<>();
    private static final Map<String,Long> OFFERED_AT=new ConcurrentHashMap<>();
    private static final ExecutorService ARRIVING=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"server-arriving");t.setDaemon(true);return t;});

    static void run(Path data,Map<String,String> o) throws Exception {
        Files.createDirectories(data);
        Path backups=Path.of(o.getOrDefault("backups",data.resolve("backups").toString()));
        Files.createDirectories(backups);
        org.mininotes.desktop.platform.util.Log.to(data.resolve("logs"));
        System.setProperty("mininotes.welcome","false");
        Context context=new Context(data.resolve("device").toFile());
        if(o.containsKey("name"))context.getSharedPreferences("settings",0).edit().putString("me",o.get("name").trim()).apply();
        String host=o.getOrDefault("public","").trim();
        if(!host.isEmpty())Node.publicHost=host;
        // A server at home, on a private address (192.168…, 10…), is still said as its door: Maxima leaves internal addresses
        // out for phones, which are never reached at one; a server given one was given it on purpose.
        if(!host.isEmpty()&&privateAddress(host))com.eurobuddha.maxima.core.MaximaNode.ALLOW_ALL_IP=true;
        NoteStore store=new NoteStore(context);store.getWritableDatabase();
        Keys keys=new Keys(context);
        store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());store.myName=Node.nameHere(context);
        store.myAgreement=Point.shorten(keys.agreement().getPublic());
        say("Mininotes backup server "+VERSION+", "+store.myName+(host.isEmpty()?", no public address given (--public)":", at "+host));

        // Pushed backups, from the owner's devices only (Post.bigArrived), kept as they came.
        Big.pushed=one->{
            String not=Keeping.take(backups,one,null,kept->{say("kept a backup from "+one.name+", "+(one.offer.bytes>>20)+" MB");writeStatus(data,store,backups);});
            if(not!=null)say("a backup from "+one.name+" not taken: "+not);
        };
        if(!Node.listen(context,bytes->ARRIVING.execute(()->{
            try {
                Post.Landed landed=Post.arrived(context,store,keys,bytes);
                if(landed!=null&&landed.accepted!=null)accepted(context,store,keys,data,backups,landed.accepted);
            } catch(Exception failed){org.mininotes.desktop.platform.util.Log.w("Mininotes/Server","something that arrived could not be read: "+failed.getClass().getSimpleName());}
        })))throw new IllegalStateException("The node could not start.");
        List<String> addresses=Node.addresses(context);store.myAddress=addresses.isEmpty()?"":addresses.get(0);
        Node.everyBeat(()->{
            try{Post.again(context,store,keys);}catch(Exception notNow){/* the next round */}
            Post.tellKeepsBackups(context,store,keys);
        });
        Node.whenUp(()->{Node.tellEverybody(context);Post.tellKeepsBackups(context,store,keys);});
        Node.tellEverybody(context);
        writeStatus(data,store,backups);
        say("ready: "+Node.attached()+" relays, "+mine(store).size()+" device(s) paired");

        // The commands' files, looked at each second; the status rewritten each minute.
        long lastStatus=0;
        while(true) {
            try {
                Path ask=data.resolve("pair").resolve("ask");
                if(Files.exists(ask)){Files.deleteIfExists(ask);makeCode(context,store,keys,data);}
                if(System.currentTimeMillis()-lastStatus>60_000L){writeStatus(data,store,backups);lastStatus=System.currentTimeMillis();}
            } catch(Exception notNow){org.mininotes.desktop.platform.util.Log.w("Mininotes/Server","a command could not be answered: "+notNow.getClass().getSimpleName());}
            Thread.sleep(1000);
        }
    }

    /** A code to add this server as one of the owner's devices, given nothing: written for the pair command to show. */
    private static void makeCode(Context context,NoteStore store,Keys keys,Path data) throws Exception {
        if(store.myAddress==null||store.myAddress.isEmpty()){List<String> a=Node.addresses(context);store.myAddress=a.isEmpty()?"":a.get(0);}
        if(store.myAddress.isEmpty())throw new IOException("no relay yet");
        String line=keys.line(Node.nameHere(context),store.myAddress,Sharing.MINE_OFFER,Sharing.Level.READ,Sharing.Scope.LIBRARY.name(),Sharing.MINE_ONLY);
        String key=Sharing.Scope.LIBRARY.name()+":"+Sharing.MINE_ONLY;
        OFFERS.put(key,Sharing.Level.READ);OFFERED_AT.put(key,System.currentTimeMillis());
        Path pair=data.resolve("pair");Files.createDirectories(pair);
        Files.deleteIfExists(pair.resolve("paired"));
        org.mininotes.desktop.platform.AtomicFile.write(pair.resolve("code"),Pairing.link(line).getBytes(StandardCharsets.UTF_8));
        say("a code to pair was shown");
    }

    /**
     * A device that scanned or pasted this server's code (decision 255's way, as the PC's Desktop.accepted): within a quarter of
     * an hour of the code being shown, added as one of the owner's devices with nothing given; anything else is not heard.
     */
    private static void accepted(Context context,NoteStore store,Keys keys,Path data,Path backups,Hello.Said said) throws Exception {
        if(said.target.isEmpty())return;
        String bare=Pairing.scopeIn(said.scope);
        if(!Sharing.mineOnly(bare,said.target))return;
        String key=bare+":"+said.target;
        Long shown=OFFERED_AT.get(key);
        if(OFFERS.get(key)==null||shown==null||System.currentTimeMillis()-shown>CODE_FOR){say(said.name+" used a code that is too old: run pair again");return;}
        Post.addedAsMine(context,store,said,null);
        try{String contact=Node.introduce(context,said.address);if(!contact.isEmpty())store.knownAs(said.address,contact);}catch(Exception unreachable){/* the address they sent still works */}
        NoteStore.Contact saved=store.address(said.address);
        try{Post.helloBack(context,keys,said,Node.nameHere(context),store.myAddress,saved==null?null:saved.contact);}catch(Exception notNow){/* they say it again until they hear */}
        Node.tellEverybody(context);
        Post.tellKeepsBackups(context,store,keys);
        Files.createDirectories(data.resolve("pair"));
        org.mininotes.desktop.platform.AtomicFile.write(data.resolve("pair").resolve("paired"),said.name.getBytes(StandardCharsets.UTF_8));
        say(said.name+" is paired: its backups can come here");
        writeStatus(data,store,backups);
    }

    /** Whether a host is on a private network or this machine: 10/8, 172.16/12, 192.168/16, 127/8, ::1, or a name that is. */
    static boolean privateAddress(String host) {
        try {
            java.net.InetAddress a=java.net.InetAddress.getByName(host);
            return a.isLoopbackAddress()||a.isSiteLocalAddress()||a.isLinkLocalAddress();
        } catch(Exception unknown){return false;}
    }

    static List<NoteStore.Contact> mine(NoteStore store) {
        List<NoteStore.Contact> out=new ArrayList<>();
        for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())out.add(one);
        return out;
    }

    /** What the status command shows: the devices paired and the backups kept, names and sizes only. */
    static void writeStatus(Path data,NoteStore store,Path backups) {
        try {
            StringBuilder s=new StringBuilder();
            DateTimeFormatter at=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
            s.append("Mininotes backup server ").append(VERSION).append(", ").append(store.myName).append('\n');
            s.append("Relays: ").append(Node.attached()).append("    Folder: ").append(backups).append('\n');
            s.append("Free space: ").append(Files.getFileStore(backups).getUsableSpace()>>30).append(" GB\n\n");
            List<NoteStore.Contact> devices=mine(store);
            s.append(devices.isEmpty()?"No device paired yet: run pair.\n":"Devices paired:\n");
            for(NoteStore.Contact one:devices)s.append("  ").append(one.name).append('\n');
            List<Path> all=Keeping.all(backups);
            s.append('\n').append(all.isEmpty()?"No backup kept yet.\n":"Backups kept:\n");
            for(Path one:all)s.append("  ").append(backups.relativize(one)).append("   ").append(Files.size(one)>>20).append(" MB   ")
                .append(at.format(Instant.ofEpochMilli(Files.getLastModifiedTime(one).toMillis()))).append('\n');
            org.mininotes.desktop.platform.AtomicFile.write(data.resolve("status.txt"),s.toString().getBytes(StandardCharsets.UTF_8));
        } catch(Exception notNow){/* the next minute */}
    }

    private static void say(String what) {
        System.out.println(what);
        org.mininotes.desktop.platform.util.Log.i("Mininotes/Server",what);
    }

    // ---- the commands ---------------------------------------------------------------------------------------------

    /** Asks the running service for a code, shows it as a link and a picture to scan, then waits to say who paired. */
    static int pair(Path data) throws Exception {
        Path pair=data.resolve("pair");
        if(!Files.isDirectory(data)){System.err.println("No server here ("+data+"). Is it installed? See the guide.");return 1;}
        Files.createDirectories(pair);
        Path code=pair.resolve("code"),paired=pair.resolve("paired");
        long asked=System.currentTimeMillis();
        Files.writeString(pair.resolve("ask"),String.valueOf(asked));
        long until=asked+30_000;
        while(!(Files.exists(code)&&Files.getLastModifiedTime(code).toMillis()>=asked-1000)) {
            if(System.currentTimeMillis()>until){System.err.println("The server did not answer. Is it running? sudo systemctl status mininotes-backup-server");return 1;}
            Thread.sleep(300);
        }
        String link=Files.readString(code,StandardCharsets.UTF_8).trim();
        System.out.println();
        System.out.println(TerminalQr.draw(link));
        System.out.println("In Mininotes, on your phone or your PC: People and devices, then +.");
        System.out.println("Scan this code, or choose Paste a link and paste this link:");
        System.out.println();
        System.out.println(link);
        System.out.println();
        System.out.println("Then: One of my devices, and What it gets: Nothing yet.");
        System.out.println("This code works for 15 minutes. Waiting…");
        long wait=asked+CODE_FOR;
        while(System.currentTimeMillis()<wait) {
            if(Files.exists(paired)&&Files.getLastModifiedTime(paired).toMillis()>=asked) {
                System.out.println();
                System.out.println("Paired with "+Files.readString(paired,StandardCharsets.UTF_8).trim()+".");
                System.out.println("On that device, open Backups, then Add a place, and choose this server.");
                return 0;
            }
            Thread.sleep(1000);
        }
        System.out.println("Nobody paired in 15 minutes. Run pair again for a new code.");
        return 2;
    }

    static int status(Path data) throws IOException {
        Path file=data.resolve("status.txt");
        if(!Files.exists(file)){System.err.println("No status yet. Is the server running? sudo systemctl status mininotes-backup-server");return 1;}
        System.out.print(Files.readString(file,StandardCharsets.UTF_8));
        long age=System.currentTimeMillis()-Files.getLastModifiedTime(file).toMillis();
        if(age>5*60_000L)System.out.println("\n(Written "+(age/60_000L)+" minutes ago: the server may not be running.)");
        return 0;
    }
}
