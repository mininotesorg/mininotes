package org.mininotes.android;

import java.nio.file.*;
import java.util.Base64;
import org.mininotes.desktop.platform.content.Context;

/**
 * A device of the owner's, as far as the backup server sees it (decision 330): a notebook of its own that takes the server's
 * link, as Paste a link and One of my devices do, hears that the server only keeps backups, and pushes it a made-up backup.
 * Run by hand against a server started apart: senderProbe --args "<data dir> <link> <file to push>". Made-up files only.
 */
public final class SenderProbe {
    public static void main(String[] args) throws Exception {
        Path data=Path.of(args[0]);String link=args[1];Path file=Path.of(args[2]);
        // The server is on this machine for the probe: its door is at 127.0.0.1, which Maxima otherwise never takes.
        com.eurobuddha.maxima.core.MaximaNode.ALLOW_ALL_IP=true;
        Context context=new Context(data.toFile());
        context.getSharedPreferences("settings",0).edit().putString("me","Probe phone").apply();
        NoteStore store=new NoteStore(context);store.getWritableDatabase();
        Keys keys=new Keys(context);
        store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());store.myName=Node.nameHere(context);
        store.myAgreement=Point.shorten(keys.agreement().getPublic());
        if(!Node.listen(context,bytes->{try{Post.arrived(context,store,keys,bytes);}catch(Exception e){System.out.println("arrived: "+e);}}))throw new IllegalStateException("no node");
        java.util.List<String> a=Node.addresses(context);store.myAddress=a.isEmpty()?"":a.get(0);
        System.out.println("node up, relays "+Node.attached());
        Pairing.Said said=Pairing.read(Pairing.line(link.trim()));
        System.out.println("code from "+said.name+", mine: "+Joining.saysMine(said));
        Joining.paired(context,store,said,true);
        try{String key=Node.introduce(context,said.address);if(!key.isEmpty())store.knownAs(said.address,key);}catch(Exception e){System.out.println("introduce: "+e.getMessage());}
        Joining.accepting(store,said);
        Post.accept(context,keys,said,Node.nameHere(context),store.myAddress);
        System.out.println("hello sent");
        long until=System.currentTimeMillis()+180_000;
        while(System.currentTimeMillis()<until&&!(store.keepsBackups(said.address)&&Post.readsMessages(context,store.address(said.address))))Thread.sleep(1000);
        System.out.println("server keeps backups: "+store.keepsBackups(said.address)+", reads messages: "+Post.readsMessages(context,store.address(said.address)));
        NoteStore.Contact them=store.address(said.address);
        System.out.println("contact key known: "+(them!=null&&them.contact!=null&&!them.contact.isEmpty()));
        for(int i=0;i<60&&Post.bigDoors(context,store,said.address).isEmpty();i++)Thread.sleep(1000);
        System.out.println("doors: "+Post.bigDoors(context,store,said.address));
        long bytes=Files.size(file);
        Backups.Said went=Post.pushBig(context,store,keys,said.address,file.getFileName().toString(),Backups.kindKeeping(3),bytes,
            at->{java.io.InputStream in=Files.newInputStream(file);in.skipNBytes(at);return in;});
        System.out.println("pushed: "+went);
        // Temp is never for it: what Temp would go to here.
        System.out.println("sent things: "+store.keepsBackups(said.address));
        System.exit(went==Backups.Said.MADE?0:2);
    }
}
