// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.eurobuddha.maxima.core.net.Frame;
import com.eurobuddha.maxima.core.net.PrivateStreams;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A big file sent straight to another device, never kept in the app and never through a relay (the owner, 2026-11-04: "Could
 * we have a way to send file directly to the other device without putting it in the app, so the person will get an
 * notification"; "ok big files only travel when the two devices can reach each other directly"; 2026-11-05: up to 2 GB, saved
 * in Downloads; decision 327).
 *
 * <p>What is sent through the usual way is only the offer: a {@link Message} of its own kind, sealed end to end for the one
 * device it is for, naming the file, its size, a token and a key made for this sending alone. A build from before reads that
 * kind as nothing, so it never becomes a note. The file itself is read from where it is, a piece at a time, when the other
 * device asks for it at this device's door with the token ({@link PrivateStreams}): each piece sealed with the key (AES-GCM),
 * numbered by its place in the nonce, so a piece missing, out of order or changed is found; the end says it is the end. The
 * receiver writes it straight into Downloads. Nothing is kept here but the token, in memory, for a day.
 *
 * <p>Holds no Android types: the offer, the stream and its checks are unit tested.
 */
final class Big {
    private Big(){}

    /** The largest file sent this way. */
    static final long MOST=2L*1024*1024*1024;
    /** What a piece holds before it is sealed. */
    static final int PIECE=256*1024;
    /** How long an offer is kept here, waiting for the other device to save it. */
    static final long KEPT_FOR=24L*60*60*1000;
    /** Why one is not sent: larger than even this way takes. */
    static final String TOO_BIG="larger than 2 GB";
    /** Why one cannot go now: the other device is not where this one can reach it directly. */
    static final String NOT_NEAR="{0} cannot be reached directly just now. Files over 16 MB go only when both devices are on the same network.";

    /** Whether a file this big goes this way rather than the usual one. */
    static boolean goes(long bytes){return bytes>Enclosure.MOST&&bytes<=MOST;}

    // ---- not reached now: the way to send it, and tried again by itself (the owner, 2026-11-07) ----------------------
    // "we should say this is a large file that can only be sent directly and then present the way to do it, all accross the app
    // we should present the solution instead of presenting the problem."

    /** How often one not reached is offered again by itself, and for how long, while Mininotes stays open here. */
    static final long AGAIN_EVERY=2*60_000L, AGAIN_FOR=24*60*60_000L;
    /** The box's title: what to do, not what went wrong. */
    static String sendTitle(String name,String device){return Say.t("Send “{0}” straight to {1}",name,device);}
    /** The steps that let it go, the device named in each. */
    static String steps(String device) {
        return Say.t("Big files go only from device to device. To send it:")+"\n\n"
            +"•  "+Say.t("Open Mininotes on {0}",device)+"\n"
            +"•  "+Say.t("Put both on the same Wi-Fi")+"\n"
            +"•  "+Say.t("Wake {0} if it is asleep",device)+"\n\n"
            +Say.t("It is tried again by itself every few minutes while Mininotes stays open here.");
    }
    /** Said in the bar or the strip while it waits. */
    static String waiting(String name,String device){return Say.t("◷ “{0}” waits for {1}",name,device);}
    /** A day of tries and still not reached: what to do then. */
    static String gaveUp(String name,String device){return Say.t("Send “{0}” again once {1} is on the same Wi-Fi with Mininotes open",name,device);}
    /** Whether what offerBig said is that the device was not reached directly: the one case the steps and the tries are for. */
    static boolean notReached(String said){return NOT_NEAR.equals(said);}
    /** A big file that did not arrive whole, said as the way to get it: the offer still stands, so Save again fetches it anew. */
    static String notWholeTitle(String name){return Say.t("Save “{0}” again",name);}
    static String notWhole(String device){return Say.t("It did not come whole. Keep Mininotes open on {0}, on the same Wi-Fi, then press Save again.",device);}
    /** Android 9 and older cannot be handed a big file: said with what does. */
    static String needsAndroid10(){return Say.t("Big files are saved on Android 10 and later. Have it sent to a computer instead.");}
    /** Whether a try that began at {@code since} may go again at {@code now}. */
    static boolean tryAgain(long since,long now){return now-since<AGAIN_FOR;}

    // ---- the offer -----------------------------------------------------------------------------------------

    /**
     * One file offered: the token that asks for it, its key, its name, its kind and its size; and whether it is pushed: brought
     * to the other device's door by this one rather than fetched from this one's (decision 330), for a device that always has
     * a door, a server, reached from a phone that has none of its own on mobile data.
     */
    static final class Offer {
        final String token; final byte[] key; final String name, kind; final long bytes; final boolean pushed;
        Offer(String token,byte[] key,String name,String kind,long bytes){this(token,key,name,kind,bytes,false);}
        Offer(String token,byte[] key,String name,String kind,long bytes,boolean pushed) {
            this.token=token;this.key=key.clone();this.name=name==null?"":name;this.kind=kind==null?"":kind;this.bytes=bytes;this.pushed=pushed;
        }
        /** A new offer of this file: a token and a key of its own. */
        static Offer of(String name,String kind,long bytes){return of(name,kind,bytes,false);}
        /** The same, to be pushed: see {@link #push}. */
        static Offer pushing(String name,String kind,long bytes){return of(name,kind,bytes,true);}
        private static Offer of(String name,String kind,long bytes,boolean pushed) {
            SecureRandom random=new SecureRandom();
            byte[] token=new byte[32],key=new byte[32];random.nextBytes(token);random.nextBytes(key);
            StringBuilder hex=new StringBuilder();for(byte b:token)hex.append(String.format("%02x",b&0xff));
            String called=Attachment.named(name);if(called.length()>200)called=called.substring(0,200);
            return new Offer(hex.toString(),key,called,kind,bytes,pushed);
        }
        /**
         * As the offer's words say it: a line each, the name last, which may hold anything but a line's end. A pushed one says
         * 2 where a fetched one says 1, so a build from before, which only reads 1, takes it for nothing and never waits on it.
         */
        String said() {
            return (pushed?"2":"1")+"\n"+token+"\n"+Base64.getEncoder().encodeToString(key)+"\n"+bytes+"\n"+kind.replace('\n',' ')+"\n"+name.replace('\n',' ');
        }
        /** An offer read back, or null where these words are not one. */
        static Offer read(String said) {
            if(said==null)return null;
            String[] lines=said.split("\n",6);
            if(lines.length!=6||!("1".equals(lines[0])||"2".equals(lines[0]))||!lines[1].matches("[a-f0-9]{64}"))return null;
            try {
                byte[] key=Base64.getDecoder().decode(lines[2]);long bytes=Long.parseLong(lines[3]);
                if(key.length!=32||bytes<=0||bytes>MOST||lines[5].trim().isEmpty())return null;
                return new Offer(lines[1],key,Attachment.named(lines[5]),lines[4],bytes,"2".equals(lines[0]));
            } catch(RuntimeException broken){return null;}
        }
    }

    // ---- sending: the file read where it is, when it is asked for --------------------------------------------

    /** Where the file is read from, from a place in it: a path on the PC, what another app handed over on the phone. */
    interface Source{InputStream from(long at) throws IOException;}

    /** What is offered from here, by its token, until it is fetched, refused or its day is over. */
    private static final Map<String,Serving> SERVING=new ConcurrentHashMap<>();

    /** What the sender hears: the file saved there, or refused. */
    interface Heard{void saved();void refused();}

    /** This file, offered: its token answered at this device's door from now, for a day. */
    static void serve(Offer offer,Source source,Heard heard) {
        sweep(System.currentTimeMillis());
        Serving one=new Serving(offer,source,heard,System.currentTimeMillis()+KEPT_FOR);
        SERVING.put(offer.token,one);PrivateStreams.register(offer.token,one);
    }
    /** No longer offered. */
    static void withdraw(String token){SERVING.remove(token);try{PrivateStreams.remove(token);}catch(RuntimeException none){/* gone */}}
    /** Whether it is still offered here. */
    static boolean serving(String token){return SERVING.containsKey(token);}
    private static void sweep(long now){for(Serving one:new ArrayList<>(SERVING.values()))if(now>one.until)withdraw(one.offer.token);}

    /** What is said first on the stream: from where in the file, or {@link #REFUSE}. */
    static final long REFUSE=-1;
    /** What the receiver says once it has the whole file, checked. */
    static final int HAVE=1;

    private static final class Serving implements PrivateStreams.Handler {
        final Offer offer;final Source source;final Heard heard;final long until;
        Serving(Offer offer,Source source,Heard heard,long until){this.offer=offer;this.source=source;this.heard=heard;this.until=until;}
        @Override public void serve(Socket socket,DataInputStream in,DataOutputStream out) throws Exception {
            if(System.currentTimeMillis()>until){withdraw(offer.token);return;}
            socket.setSoTimeout(120_000);
            long at=in.readLong();
            if(at==REFUSE){withdraw(offer.token);if(heard!=null)heard.refused();return;}
            if(at<0||at>offer.bytes||at%PIECE!=0)return;
            send(offer,source,at,out);
            try{if(in.readUnsignedByte()==HAVE){withdraw(offer.token);if(heard!=null)heard.saved();}}catch(EOFException gone){/* asked again, or not */}
        }
    }

    /** The file from {@code at}, a sealed piece at a time, then the end. */
    static void send(Offer offer,Source source,long at,DataOutputStream out) throws IOException {
        try(InputStream file=source.from(at)) {
            byte[] piece=new byte[PIECE];long index=at/PIECE,left=offer.bytes-at;
            while(left>0) {
                int want=(int)Math.min(PIECE,left),got=0;
                while(got<want){int n=file.read(piece,got,want-got);if(n<0)throw broken("the-file-is-shorter-than-it-was");got+=n;}
                byte[] sealed=seal(offer,index,piece,got);
                out.writeInt(sealed.length);out.write(sealed);
                index++;left-=got;
            }
            out.writeInt(0);out.flush();
        }
    }

    // ---- receiving ------------------------------------------------------------------------------------------

    /** An offer that came, waiting for Save or Refuse: from whom, and what. */
    static final class Waiting {
        final String from,name;final Offer offer;final long at;
        Waiting(String from,String name,Offer offer,long at){this.from=from;this.name=name;this.offer=offer;this.at=at;}
    }
    /** What came and is not answered yet, by token: in memory, as the sender keeps its side. */
    private static final Map<String,Waiting> WAITING=new ConcurrentHashMap<>();
    /** Told of each that came to be asked about: the phone's notification, the PC's box. */
    static volatile Consumer<Waiting> asking;
    /** Told of each that came from somebody whose files are taken without asking: saved at once. */
    static volatile Consumer<Waiting> saving;
    /**
     * Told of each pushed to this device by one of the owner's (decision 330), to {@link #take} it where it goes: a backup
     * server's folder, the PC's. Null where this device takes none, a phone: the sender then hears nobody and tries later.
     */
    static volatile Consumer<Waiting> pushed;

    static void waiting(Waiting one){WAITING.put(one.offer.token,one);}
    static Waiting waitingFor(String token){return token==null?null:WAITING.get(token);}
    static void answered(String token){if(token!=null)WAITING.remove(token);}

    /**
     * The file fetched from the first of these doors that answers, written to {@code to} as it comes, every piece checked;
     * {@code progress} told how much is here. Throws where no door answers or anything is wrong; then what was written is
     * not the file and is to be thrown away.
     */
    static void fetch(List<InetSocketAddress> doors,Offer offer,OutputStream to,LongConsumer progress) throws IOException {
        IOException last=broken("no-door-answered");
        for(InetSocketAddress door:doors) {
            Socket socket=new Socket();
            try {
                socket.connect(door,Direct.LEASH);socket.setSoTimeout(120_000);socket.setTcpNoDelay(true);
                DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                DataInputStream in=new DataInputStream(new java.io.BufferedInputStream(socket.getInputStream(),1<<16));
                Frame.write(out,asking(offer.token));
                out.writeLong(0);out.flush();
                receive(offer,in,to,progress);
                out.writeByte(HAVE);out.flush();
                return;
            } catch(java.net.ConnectException|java.net.SocketTimeoutException|java.net.NoRouteToHostException notThere) {
                last=notThere;
            } finally{try{socket.close();}catch(IOException ignored){/* closed */}}
        }
        throw last;
    }

    /** Refused: said at the first door that answers, so the sender stops offering it. Nothing where none does. */
    static void refuse(List<InetSocketAddress> doors,Offer offer) {
        for(InetSocketAddress door:doors)try(Socket socket=new Socket()) {
            socket.connect(door,Direct.LEASH);
            DataOutputStream out=new DataOutputStream(socket.getOutputStream());
            Frame.write(out,asking(offer.token));out.writeLong(REFUSE);out.flush();return;
        } catch(IOException notThere){/* the next */}
    }

    // ---- pushed: brought to this device's door by the sender (decision 330) -----------------------------------------

    /** Where a pushed file is written: how much of it is there from a try before, and the rest added from a place. */
    interface Sink {
        /** Bytes already there, kept from a try that broke off; 0 for none. */
        long have();
        /** Written from {@code at}: what was there past it is dropped. */
        OutputStream from(long at) throws IOException;
        /** The whole file came, every piece checked: made the file it is. */
        void done() throws IOException;
    }

    /** What is taken at this device's door, by token, until it came whole or its day is over. */
    private static final Map<String,Taking> TAKING=new ConcurrentHashMap<>();

    /** This pushed file, taken: its token answered at this device's door from now, for a day, a broken try taken up again. */
    static void take(Offer offer,Sink sink,LongConsumer progress) {
        long now=System.currentTimeMillis();
        for(Taking one:new ArrayList<>(TAKING.values()))if(now>one.until)stopTaking(one.offer.token);
        Taking one=new Taking(offer,sink,progress,now+KEPT_FOR);
        TAKING.put(offer.token,one);PrivateStreams.register(offer.token,one);
    }
    static boolean taking(String token){return TAKING.containsKey(token);}
    static void stopTaking(String token){TAKING.remove(token);try{PrivateStreams.remove(token);}catch(RuntimeException none){/* gone */}}

    private static final class Taking implements PrivateStreams.Handler {
        final Offer offer;final Sink sink;final LongConsumer progress;final long until;
        Taking(Offer offer,Sink sink,LongConsumer progress,long until){this.offer=offer;this.sink=sink;this.progress=progress;this.until=until;}
        @Override public void serve(Socket socket,DataInputStream in,DataOutputStream out) throws Exception {
            if(System.currentTimeMillis()>until){stopTaking(offer.token);return;}
            socket.setSoTimeout(120_000);
            // Said first, by the side that knows: where to go on from, a whole piece, so a try that broke off is not sent again.
            long at=Math.max(0,Math.min(sink.have(),offer.bytes));at-=at%PIECE;
            out.writeLong(at);out.flush();
            synchronized(this) {
                try(OutputStream to=sink.from(at)){receive(offer,in,to,progress,at);}
                sink.done();
            }
            stopTaking(offer.token);
            out.writeByte(HAVE);out.flush();
        }
    }

    /**
     * This pushed file brought to the first of these doors that takes it, from where that door says to go on; tried again
     * every two seconds for {@code patience} ms, as the offer that names its token may reach the other device a moment after
     * this one knocks. Returns once that device said it has the whole file. Throws where none took it in time, or it refused.
     */
    static void push(List<InetSocketAddress> doors,Offer offer,Source source,long patience) throws IOException {
        long until=System.currentTimeMillis()+patience;IOException last=broken("no-door-answered");
        while(true) {
            for(InetSocketAddress door:doors) {
                Socket socket=new Socket();
                try {
                    socket.connect(door,Direct.LEASH);socket.setSoTimeout(120_000);socket.setTcpNoDelay(true);
                    DataOutputStream out=new DataOutputStream(new java.io.BufferedOutputStream(socket.getOutputStream(),1<<16));
                    DataInputStream in=new DataInputStream(socket.getInputStream());
                    Frame.write(out,asking(offer.token));out.flush();
                    long at=in.readLong();
                    if(at==REFUSE)throw broken("refused");
                    if(at<0||at>offer.bytes||at%PIECE!=0)throw broken("not-a-place-in-the-file");
                    send(offer,source,at,out);
                    if(in.readUnsignedByte()!=HAVE)throw broken("not-said-whole");
                    return;
                } catch(EOFException|java.net.SocketTimeoutException|java.net.SocketException notYet) {
                    // Not there, not taking it yet, or the line broke: knocked again, and it says where to go on from.
                    last=notYet;
                } finally{try{socket.close();}catch(IOException ignored){/* closed */}}
            }
            if(System.currentTimeMillis()>=until)throw last;
            try{Thread.sleep(2000);}catch(InterruptedException stop){Thread.currentThread().interrupt();throw last;}
        }
    }

    /** The pieces read and opened, in order, to the end, and the size checked. */
    static void receive(Offer offer,DataInputStream in,OutputStream to,LongConsumer progress) throws IOException {receive(offer,in,to,progress,0);}
    /** The same, from {@code at}, a whole piece in: the pieces numbered on from there. */
    static void receive(Offer offer,DataInputStream in,OutputStream to,LongConsumer progress,long at) throws IOException {
        long index=at/PIECE,here=at;
        while(true) {
            int length=in.readInt();
            if(length==0)break;
            if(length<0||length>PIECE+16)throw broken("not-a-piece");
            byte[] sealed=new byte[length];in.readFully(sealed);
            byte[] plain=open(offer,index,sealed);
            if(here+plain.length>offer.bytes)throw broken("more-than-was-offered");
            to.write(plain);here+=plain.length;index++;
            if(progress!=null)progress.accept(here);
        }
        if(here!=offer.bytes)throw broken("the-file-did-not-all-come");
        to.flush();
    }

    private static byte[] asking(String token) {
        byte[] asking=new byte[65];asking[0]=(byte)PrivateStreams.TYPE;
        System.arraycopy(token.getBytes(StandardCharsets.US_ASCII),0,asking,1,64);
        return asking;
    }

    /**
     * What went wrong on the way, for the log only: never shown, so a code rather than a sentence. The person is told in their own
     * words that the file could not be received.
     */
    static IOException broken(String code){return new IOException("big: "+code);}
    static IllegalArgumentException wrong(String code){return new IllegalArgumentException("big: "+code);}
    private static IOException broken(String code,Throwable cause){return new IOException("big: "+code,cause);}

    // ---- the seal -------------------------------------------------------------------------------------------

    private static Cipher cipher(Offer offer,long index,int mode) throws IOException {
        try {
            byte[] nonce=ByteBuffer.allocate(12).putInt(0).putLong(index).array();
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(mode,new SecretKeySpec(offer.key,"AES"),new GCMParameterSpec(128,nonce));
            c.updateAAD((offer.token+":"+offer.bytes).getBytes(StandardCharsets.US_ASCII));
            return c;
        } catch(java.security.GeneralSecurityException broken){throw broken("the-seal-could-not-be-made",broken);}
    }
    static byte[] seal(Offer offer,long index,byte[] plain,int length) throws IOException {
        try{return cipher(offer,index,Cipher.ENCRYPT_MODE).doFinal(plain,0,length);}
        catch(java.security.GeneralSecurityException broken){throw broken("the-seal-could-not-be-made",broken);}
    }
    static byte[] open(Offer offer,long index,byte[] sealed) throws IOException {
        try{return cipher(offer,index,Cipher.DECRYPT_MODE).doFinal(sealed);}
        catch(java.security.GeneralSecurityException broken){throw broken("a-piece-did-not-open",broken);}
    }
}
