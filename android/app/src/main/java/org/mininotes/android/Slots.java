// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Where private notes and folders are kept, and nothing says whether any are (the owner, 2026-10-06: "Once closed they
 * should not appear anywhere, and a hacker should not have any trace of their existence even by looking at the code of
 * the app"; decision 111).
 *
 * <p>The code is public, so the feature cannot be hidden: what is kept hidden is whether a given device's owner ever used
 * it, how much, and for what. Every install has this one file, {@link #NAME}, from its first start: {@link #COUNT} slots of
 * {@link #SLOT} bytes, 64 MiB, the same size for everybody and never more or less (the owner: "maybe like 50 photos").
 * Without a password it is random bytes from end to end: no mark, no header, no version, no count.
 *
 * <p>Each slot on disk is sixteen random bytes and then the rest of it in AES-CTR under this device's own key (from its
 * agreement key, which the Keystore or the Windows user guards): so every slot written is new bytes, whether what it holds
 * changed or not, and two copies of the file taken a day apart do not show which slots somebody's notes are in. Under that
 * is what the slot holds, the same on any device (a backup carries it so): a password's slot is a nonce, a mark that only
 * that password's key can make from the nonce (HMAC-SHA256, sixteen bytes), and AES-256-GCM of a short head (which thing,
 * which writing of it, which part) and up to {@link #DATA} bytes of it, Deflate-compressed before; any other slot is
 * random. A password's key is PBKDF2-HMAC-SHA256 ({@link #ROUNDS} rounds) of it and the salt that slot 0 carries. Opening
 * reads every slot's nonce and mark, and only a slot whose mark that key makes is decrypted: a password nothing answers to
 * opens nothing, and nothing is said or written.
 *
 * <p>What a password opens is a set of things by name (see {@link PrivateSpace}), each Deflated and cut into slots, and an
 * index of them. A change is written beside what is there (new slots, new writings), the index last, and the old slots are
 * filled with nothing under the same key once the new index is whole: never written over before, so a write cut short
 * leaves the last whole state.
 *
 * <p>Any number of independent spaces, each its own order of the data slots (the owner, 2026-10-06: "every time I type
 * privatespace//: with a new password it will create a new independent space"; decision 116, his choice over the safe
 * two-halves model). A password owns a deterministic pseudo-random order of all the data slots, seeded from its own key
 * ({@link Space#order}, an HMAC-of-the-key stream): it keeps its content in the slots it already owns, and when it needs
 * more it takes the next slots in its order it does not already own, writing there even if another password's space happens
 * to hold them (accepted overwrite, warned in Read me). It never writes over a space open at the same moment here. A space
 * is found by trying slots and matching its per-slot mark, as before: nothing records how many spaces exist, nothing is a
 * room list, nothing says a space is there at all until a password answers for it.
 *
 * <p>The rhythm, the same for everybody (the owner: "how do you make sure it stays always the same"): {@link #batch} writes
 * {@link #BATCH} slots, 1 MiB, at every start, every {@link #EVERY} while the app is open and when it is left, whether
 * anything is private or not. Each slot written is either the next part of a change that waits, or a slot written again as
 * it was under new random bytes. A change is never written outside a batch: one too big for a batch waits in memory,
 * sealed, and goes in the next ones. A batch is written first to a journal (slots 1 to 17), then in place, so a batch cut
 * short is put in place again at the next start, and a slot is never left half old and half new.
 *
 * <p>Holds no Android types and logs nothing, ever.
 */
final class Slots {
    /** The file's name: one like the app's other files, saying nothing of what it is. */
    static final String NAME="pages.protected";
    static final int SLOT=1<<16, COUNT=1024, BATCH=16, FIRST=2+BATCH;
    /** How often a batch is written while the app is open. */
    static final long EVERY=60_000L;
    /** PBKDF2 rounds: slow on purpose, every guess at a password costs this. Fixed: there is no header to say another. */
    static final int ROUNDS=200_000;
    static final int MASK=16, INNER=SLOT-MASK, NONCE=12, MARK=16, TAG=16, PLAIN=INNER-NONCE-MARK-TAG, HEAD=16+8+4+4+4, DATA=PLAIN-HEAD;
    private static final byte[] INDEX=new byte[16], FILLER=new byte[16];
    static {Arrays.fill(FILLER,(byte)-1);}
    private static final SecureRandom RANDOM=new SecureRandom();

    /** No room left for a change: said only inside what is open, and nothing of the change is written. */
    static final class Full extends Exception {Full(){super("Private storage is full");}}

    private final File file;private final byte[] mask;private final int rounds,wanted;
    private int count;
    /** What is open, or was and still has writing waiting (keys gone, only sealed slots and their places left). */
    private final List<Space> spaces=new ArrayList<>();
    /** Bytes written to the file, and batches, since this was made: for the tests that hold the rhythm the same for everybody. */
    long written;int batches;
    /** For the test of a batch the power cut: the journal written, nothing in place. */
    boolean cutShort;

    Slots(File file,byte[] mask){this(file,mask,COUNT,ROUNDS);}
    /** {@code count} is the size of a file made new; one already there keeps its own. */
    Slots(File file,byte[] mask,int count,int rounds) {
        if(count<FIRST+2*BATCH)throw new IllegalArgumentException("Too few slots");
        this.file=file;this.mask=mask.clone();this.wanted=count;this.rounds=rounds;
    }

    /** This device's own key for the slots' outer layer, from a secret this device keeps (its agreement key). */
    static byte[] maskFrom(byte[] secret) throws GeneralSecurityException {
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        sha.update("Mininotes pages 1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));sha.update(secret);
        return sha.digest();
    }

    File file(){return file;}
    synchronized int count(){return count;}

    // ---- the file ---------------------------------------------------------------------------------------------------

    /**
     * At every start: the file made if it is not there (and on the upgrade that brings it), a batch cut short put in place,
     * and the first batch of this start. The same for everybody.
     */
    synchronized void start() throws IOException, GeneralSecurityException {
        File landing=new File(file.getPath()+".new");
        if(landing.exists()&&!landing.delete())throw new IOException("Could not tidy");
        if(!file.isFile()||file.length()<(long)SLOT*(FIRST+2*BATCH))make(landing);
        count=(int)(file.length()/SLOT);
        replay();
        batch();
    }

    /** Random from end to end, written beside and put in place whole. */
    private void make(File landing) throws IOException, GeneralSecurityException {
        byte[] key=new byte[32],nonce=new byte[16];RANDOM.nextBytes(key);RANDOM.nextBytes(nonce);
        Cipher stream=Cipher.getInstance("AES/CTR/NoPadding");stream.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(nonce));
        byte[] zeros=new byte[SLOT];
        try(RandomAccessFile out=new RandomAccessFile(landing,"rw")) {
            for(int at=0;at<wanted;at++)out.write(stream.update(zeros));
            out.getFD().sync();
        }
        Arrays.fill(key,(byte)0);
        if(file.exists()&&!file.delete())throw new IOException("Could not replace");
        if(!landing.renameTo(file))throw new IOException("Could not put in place");
    }

    private synchronized boolean ready(){return count>0&&file.isFile();}

    private byte[] readInner(RandomAccessFile in,int at) throws IOException, GeneralSecurityException {
        byte[] image=new byte[SLOT];in.seek((long)at*SLOT);in.readFully(image);
        return unmask(image,INNER);
    }
    private byte[] unmask(byte[] image,int length) throws GeneralSecurityException {
        Cipher c=Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(mask,"AES"),new IvParameterSpec(image,0,MASK));
        return c.doFinal(image,MASK,length);
    }
    private byte[] masked(byte[] inner) throws GeneralSecurityException {
        byte[] image=new byte[SLOT];byte[] nonce=new byte[MASK];RANDOM.nextBytes(nonce);
        System.arraycopy(nonce,0,image,0,MASK);
        Cipher c=Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(mask,"AES"),new IvParameterSpec(nonce));
        c.doFinal(inner,0,INNER,image,MASK);
        return image;
    }

    /** A batch the last start did not finish: its journal checks, so it is put in place again (the same slots, the same bytes under). */
    private void replay() throws IOException, GeneralSecurityException {
        try(RandomAccessFile io=new RandomAccessFile(file,"rw")) {
            byte[] head=readInner(io,1);
            int[] at=new int[BATCH];byte[][] inner=new byte[BATCH][];
            DataInputStream said=new DataInputStream(new ByteArrayInputStream(head));
            for(int i=0;i<BATCH;i++){at[i]=said.readInt();if(at[i]<FIRST||at[i]>=count)return;}
            byte[] check=new byte[32];said.readFully(check);
            MessageDigest sha=MessageDigest.getInstance("SHA-256");sha.update(head,0,4*BATCH);
            for(int i=0;i<BATCH;i++){inner[i]=readInner(io,2+i);sha.update(inner[i]);}
            if(!MessageDigest.isEqual(check,sha.digest()))return;
            for(int i=0;i<BATCH;i++){io.seek((long)at[i]*SLOT);io.write(masked(inner[i]));}
            io.getFD().sync();
        }
    }

    // ---- the rhythm -------------------------------------------------------------------------------------------------

    /**
     * One batch: {@link #BATCH} slots, whatever is open or not. First the parts that wait, in order (a password's own free
     * slots where it has them, or slots nobody here owns); then slots anywhere, written again as they are. Written to the
     * journal, then in place.
     */
    synchronized void batch() throws IOException, GeneralSecurityException {
        if(!ready())return;
        LinkedHashMap<Integer,byte[]> chosen=new LinkedHashMap<>();
        List<Object[]> placed=new ArrayList<>();
        List<Integer> free=null;
        for(Space s:spaces) {
            for(Entry e:s.queue) {
                if(chosen.size()>=BATCH)break;
                int at=e.target;
                if(at<0) {
                    if(free==null)free=freePlaces(chosen.keySet());
                    if(free.isEmpty())break;
                    at=free.remove(RANDOM.nextInt(free.size()));
                } else if(chosen.containsKey(at))break;
                chosen.put(at,e.inner);placed.add(new Object[]{s,e,at});
            }
        }
        while(chosen.size()<BATCH){int at=FIRST+RANDOM.nextInt(count-FIRST);if(!chosen.containsKey(at))chosen.put(at,null);}
        try(RandomAccessFile io=new RandomAccessFile(file,"rw")) {
            int[] at=new int[BATCH];byte[][] inner=new byte[BATCH][];int i=0;
            for(Map.Entry<Integer,byte[]> one:chosen.entrySet()){at[i]=one.getKey();inner[i]=one.getValue()!=null?one.getValue():readInner(io,at[i]);i++;}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(INNER);DataOutputStream head=new DataOutputStream(bytes);
            for(int p:at)head.writeInt(p);
            MessageDigest sha=MessageDigest.getInstance("SHA-256");sha.update(bytes.toByteArray());
            for(byte[] one:inner)sha.update(one);
            head.write(sha.digest());
            byte[] rest=new byte[INNER-bytes.size()];RANDOM.nextBytes(rest);head.write(rest);
            io.seek(SLOT);io.write(masked(bytes.toByteArray()));
            for(i=0;i<BATCH;i++)io.write(masked(inner[i]));
            io.getFD().sync();
            if(cutShort)throw new IOException("Cut short");
            for(i=0;i<BATCH;i++){io.seek((long)at[i]*SLOT);io.write(masked(inner[i]));}
            io.getFD().sync();
        }
        written+=(long)(1+2*BATCH)*SLOT;batches++;
        for(Object[] p:placed){Space s=(Space)p[0];Entry e=(Entry)p[1];s.queue.remove(e);s.own.put((Integer)p[2],e.head);}
        for(Space s:new ArrayList<>(spaces))s.settle();
        spaces.removeIf(s->s.enc==null&&s.queue.isEmpty());
    }

    private Set<Integer> owned() {
        Set<Integer> all=new HashSet<>();
        for(Space s:spaces)all.addAll(s.own.keySet());
        return all;
    }
    /** Slots no space open here owns (a closed one's may be among them: that is the risk two unrelated passwords run). */
    private List<Integer> freePlaces(Collection<Integer> also) {
        Set<Integer> taken=owned();taken.addAll(also);
        List<Integer> free=new ArrayList<>();
        for(int at=FIRST;at<count;at++)if(!taken.contains(at))free.add(at);
        return free;
    }

    // ---- opening --------------------------------------------------------------------------------------------------

    /** What a password opens, or null: nothing answers to it, and nothing was changed or said. */
    Space open(char[] password) throws IOException, GeneralSecurityException {
        if(!ready()||password==null||password.length==0)return null;
        // The slow part outside the lock, so a batch due meanwhile is not held up by it.
        byte[][] keys=keys(password);
        try{synchronized(this){return open(keys[0],keys[1]);}}
        finally{Arrays.fill(keys[0],(byte)0);Arrays.fill(keys[1],(byte)0);}
    }

    /**
     * A new, independent space made from outside (privatespace//:, the password typed twice; the owner, 2026-10-06, decision
     * 116), holding {@code first} (an empty folder; see {@link PrivateSpace#empty}). It owns its own order of the data slots,
     * from its own key, and writes only there; it knows of no other space and remembers none. Where its order lands on a
     * closed space's slots it writes over them (accepted; Read me warns of it).
     */
    Space make(char[] password,Map<String,byte[]> first) throws IOException, GeneralSecurityException, Full {
        if(!ready()||password==null||password.length==0)return null;
        byte[][] keys=keys(password);
        synchronized(this){return make(keys,first);}
    }
    private Space make(byte[][] keys,Map<String,byte[]> first) throws IOException, GeneralSecurityException, Full {
        Space made=new Space(keys[0],keys[1]);
        Arrays.fill(keys[0],(byte)0);Arrays.fill(keys[1],(byte)0);
        made.newest=new Index();
        spaces.add(made);made.contents=new LinkedHashMap<>(first);
        try{made.put(first,List.of());}
        catch(Full full){spaces.remove(made);made.forget();throw full;}
        return made;
    }

    private byte[][] keys(char[] password) throws IOException, GeneralSecurityException {
        byte[] salt;
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){salt=Arrays.copyOf(readInner(in,0),32);}
        byte[] master=Vault.pbkdf2(password,salt,rounds);
        try{return new byte[][]{hmac(master,"enc"),hmac(master,"mark")};}
        finally{Arrays.fill(master,(byte)0);}
    }

    private Space open(byte[] enc,byte[] mark) throws IOException, GeneralSecurityException {
        for(Space s:spaces)if(s.mark!=null&&MessageDigest.isEqual(s.mark,mark))return s;
        Mac marker=mac(mark);
        Map<String,byte[]> parts=new HashMap<>();Map<String,Head> heads=new HashMap<>();
        Map<Integer,Head> found=new HashMap<>();
        // A slot another open space owns is that space's, whatever an older key might still read in it (a password changed
        // a moment ago and not yet all written: the old one opens nothing).
        // A space closed a moment ago whose writing still waits here: it is the same one, and what waits is its newest.
        Space ghost=null;
        for(Space s:spaces)if(s.enc==null&&!s.queue.isEmpty()&&marked(marker,s.queue.get(0).inner)){ghost=s;break;}
        // A slot another open space holds or has queued to write is that space's: skip it, so a space opened while another
        // has writing waiting never reads back and claims a slot that one is about to own (decision 116).
        Set<Integer> taken=new HashSet<>();
        for(Space s:spaces)if(s!=ghost){taken.addAll(s.own.keySet());for(Entry e:s.queue)if(e.target>=0)taken.add(e.target);}
        try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
            byte[] start=new byte[MASK+NONCE+MARK];
            for(int at=FIRST;at<count;at++) {
                if(taken.contains(at))continue;
                in.seek((long)at*SLOT);in.readFully(start);
                byte[] inner=unmask(start,NONCE+MARK);
                if(!marked(marker,inner))continue;
                byte[] whole=readInner(in,at);
                Object[] read=unseal(enc,whole);if(read==null)continue;
                Head h=(Head)read[0];found.put(at,h);keep(h,(byte[])read[1],parts,heads);
            }
        }
        if(ghost!=null)for(Entry e:ghost.queue){Object[] read=unseal(enc,e.inner);if(read!=null)keep((Head)read[0],(byte[])read[1],parts,heads);}
        if(found.isEmpty()&&ghost==null)return null;
        // The newest index that is whole, with every part of everything it names.
        Index newest=null,committed=null;long most=0;
        Map<Long,Index> indexes=new HashMap<>();
        for(Head h:heads.values()) {
            most=Math.max(most,h.gen);
            if(!Arrays.equals(h.id,INDEX)||indexes.containsKey(h.gen))continue;
            byte[] said=assemble(INDEX,h.gen,h.parts,parts);
            if(said!=null)try{indexes.put(h.gen,Index.read(inflate(said)));}catch(IOException|DataFormatException damaged){/* not whole */}
        }
        Set<String> onDisk=new HashSet<>();for(Head h:found.values())onDisk.add(key(h.id,h.gen,h.part));
        for(Index one:indexes.values()) {
            if(!whole(one,parts.keySet()))continue;
            if(newest==null||one.gen>newest.gen)newest=one;
            if(whole(one,onDisk)&&(committed==null||one.gen>committed.gen))committed=one;
        }
        boolean damaged=false;
        if(newest==null) {
            // Every index is gone or cut: what is left of the newest is shown, and said to be damaged.
            for(Index one:indexes.values())if(newest==null||one.gen>newest.gen)newest=one;
            if(newest==null){if(ghost!=null)return null;Space nothing=new Space(enc,mark);nothing.own.putAll(found);
                // Slots this password filled with nothing: its own, and nothing to show. Opens nothing.
                boolean any=false;for(Head h:found.values())if(!Arrays.equals(h.id,FILLER))any=true;
                if(!any)return null;
                nothing.newest=new Index();nothing.damaged=true;nothing.contents=new LinkedHashMap<>();nothing.gens=most;spaces.add(nothing);return nothing;}
            damaged=true;
        }
        Space space=ghost!=null?ghost:new Space(enc,mark);
        if(ghost!=null){space.enc=enc.clone();space.mark=mark.clone();}
        space.own.putAll(found);space.committed=committed;space.newest=newest;space.gens=Math.max(space.gens,most);space.damaged=damaged;
        space.contents=new LinkedHashMap<>();
        for(Map.Entry<String,Ref> one:newest.objects.entrySet()) {
            byte[] said=assemble(one.getValue().id,one.getValue().gen,one.getValue().parts,parts);
            if(said==null){space.damaged=true;continue;}
            try{space.contents.put(one.getKey(),inflate(said));}catch(DataFormatException|IOException cut){space.damaged=true;}
        }
        if(ghost==null)spaces.add(space);
        for(byte[] kept:parts.values())Arrays.fill(kept,(byte)0);
        return space;
    }

    private static void keep(Head h,byte[] data,Map<String,byte[]> parts,Map<String,Head> heads) {
        if(Arrays.equals(h.id,FILLER))return;
        String k=key(h.id,h.gen,h.part);parts.put(k,data);heads.put(k,h);
    }
    private static String key(byte[] id,long gen,int part){return hex(id)+":"+gen+":"+part;}
    private static boolean whole(Index index,Set<String> have) {
        for(int p=0;p<index.parts;p++)if(!have.contains(key(INDEX,index.gen,p)))return false;
        for(Ref r:index.objects.values())for(int p=0;p<r.parts;p++)if(!have.contains(key(r.id,r.gen,p)))return false;
        return true;
    }
    private static byte[] assemble(byte[] id,long gen,int count,Map<String,byte[]> parts) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int p=0;p<count;p++){byte[] one=parts.get(key(id,gen,p));if(one==null)return null;out.write(one,0,one.length);}
        return out.toByteArray();
    }

    /** Closes everything: keys and contents gone; what still has writing waiting keeps only its sealed slots and their places. */
    synchronized void close() {
        for(Space s:new ArrayList<>(spaces))s.closeQuietly();
        spaces.removeIf(s->s.enc==null&&s.queue.isEmpty());
    }

    /** Whether anything still waits to be written, open or closed. */
    synchronized boolean anyWaiting(){for(Space s:spaces)if(!s.queue.isEmpty())return true;return false;}

    /** Whether anything is open here, shown or kept from. */
    synchronized boolean anyOpen(){for(Space s:spaces)if(s.enc!=null)return true;return false;}

    // ---- backups ----------------------------------------------------------------------------------------------------

    /** The size of what a backup carries: every slot's inner part, the same on any device. */
    synchronized long portableSize(){return (long)count*INNER;}

    /** Written into a backup: the file without this device's outer layer, so it opens wherever the backup is put back. */
    synchronized void portable(OutputStream out) throws IOException, GeneralSecurityException {
        if(!ready())return;
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){for(int at=0;at<count;at++)out.write(readInner(in,at));}
    }

    /**
     * A backup's copy, read and laid down beside the file under this device's outer layer, to be put in its place only once
     * the rest of the backup is in (see {@link #adopt}); nothing of what is here changes yet.
     */
    File stage(InputStream in) throws IOException, GeneralSecurityException {
        File landing=new File(file.getPath()+".new");
        int slots=0;
        try(RandomAccessFile out=new RandomAccessFile(landing,"rw")) {
            out.setLength(0);
            byte[] inner=new byte[INNER];
            while(true) {
                int got=0;while(got<INNER){int n=in.read(inner,got,INNER-got);if(n<0)break;got+=n;}
                if(got==0)break;
                if(got<INNER)throw new IOException("That backup's private part is cut short");
                out.write(masked(inner));slots++;
            }
            out.getFD().sync();
        } catch(IOException|GeneralSecurityException|RuntimeException failed){landing.delete();throw failed;}
        if(slots<FIRST+2*BATCH){landing.delete();throw new IOException("That backup's private part is cut short");}
        return landing;
    }

    /** What a backup carried, put in place of what was here: everything open or waiting here is let go first. */
    synchronized void adopt(File staged) throws IOException {
        for(Space s:new ArrayList<>(spaces))s.forget();
        spaces.clear();
        if(file.exists()&&!file.delete()){staged.delete();throw new IOException("Could not replace");}
        if(!staged.renameTo(file))throw new IOException("Could not put in place");
        count=(int)(file.length()/SLOT);
    }

    // ---- sealing ----------------------------------------------------------------------------------------------------

    private static byte[] hmac(byte[] key,String what) throws GeneralSecurityException {
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
        return mac.doFinal(what.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
    private static Mac mac(byte[] mark) throws GeneralSecurityException {
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(mark,"HmacSHA256"));return mac;
    }
    private static boolean marked(Mac marker,byte[] inner) {
        marker.update(inner,0,NONCE);byte[] made=marker.doFinal();
        return MessageDigest.isEqual(Arrays.copyOf(made,MARK),Arrays.copyOfRange(inner,NONCE,NONCE+MARK));
    }

    /** A slot's inner part: nonce, mark, and the head and data under AES-256-GCM. */
    private static byte[] seal(byte[] enc,byte[] mark,Head h,byte[] data,int from,int length) throws GeneralSecurityException {
        byte[] plain=new byte[PLAIN];
        java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(plain);
        b.put(h.id).putLong(h.gen).putInt(h.part).putInt(h.parts).putInt(length);
        if(data!=null)b.put(data,from,length);
        else{byte[] noise=new byte[DATA];RANDOM.nextBytes(noise);b.put(noise);}
        byte[] inner=new byte[INNER],nonce=new byte[NONCE];RANDOM.nextBytes(nonce);
        System.arraycopy(nonce,0,inner,0,NONCE);
        Mac marker=mac(mark);marker.update(nonce);System.arraycopy(marker.doFinal(),0,inner,NONCE,MARK);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(enc,"AES"),new GCMParameterSpec(TAG*8,nonce));
        c.doFinal(plain,0,PLAIN,inner,NONCE+MARK);
        Arrays.fill(plain,(byte)0);
        return inner;
    }
    /** The head and the data of a slot this key sealed, or null. */
    private static Object[] unseal(byte[] enc,byte[] inner) {
        try {
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(enc,"AES"),new GCMParameterSpec(TAG*8,inner,0,NONCE));
            byte[] plain=c.doFinal(inner,NONCE+MARK,INNER-NONCE-MARK);
            java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(plain);
            Head h=new Head(new byte[16],0,0,0,0);b.get(h.id);h.gen=b.getLong();h.part=b.getInt();h.parts=b.getInt();h.length=b.getInt();
            if(h.length<0||h.length>DATA||h.parts<1||h.part<0||h.part>=h.parts)return null;
            byte[] data=Arrays.copyOfRange(plain,HEAD,HEAD+h.length);
            Arrays.fill(plain,(byte)0);
            return new Object[]{h,data};
        } catch(GeneralSecurityException|RuntimeException not){return null;}
    }

    static byte[] deflate(byte[] plain) {
        Deflater d=new Deflater(Deflater.BEST_COMPRESSION);d.setInput(plain);d.finish();
        ByteArrayOutputStream out=new ByteArrayOutputStream(Math.max(64,plain.length/2));byte[] buf=new byte[1<<14];
        while(!d.finished()){int n=d.deflate(buf);out.write(buf,0,n);}
        d.end();return out.toByteArray();
    }
    static byte[] inflate(byte[] packed) throws DataFormatException, IOException {
        Inflater i=new Inflater();i.setInput(packed);
        ByteArrayOutputStream out=new ByteArrayOutputStream(Math.max(64,packed.length*2));byte[] buf=new byte[1<<14];
        try {
            while(!i.finished()){int n=i.inflate(buf);if(n==0&&(i.needsInput()||i.needsDictionary()))throw new IOException("Cut short");out.write(buf,0,n);}
        } finally{i.end();}
        return out.toByteArray();
    }

    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(Character.forDigit((x>>4)&15,16)).append(Character.forDigit(x&15,16));return s.toString();}

    // ---- what a password holds ---------------------------------------------------------------------------------------

    static final class Head {
        final byte[] id;long gen;int part,parts,length;
        Head(byte[] id,long gen,int part,int parts,int length){this.id=id;this.gen=gen;this.part=part;this.parts=parts;this.length=length;}
    }
    static final class Ref {
        final byte[] id;final long gen;final int parts;
        Ref(byte[] id,long gen,int parts){this.id=id;this.gen=gen;this.parts=parts;}
    }
    /**
     * The list of what a password holds, in its own slots: which thing, which writing of it, in how many parts. No room
     * and no remembered keys any more (decision 116: a space owns an order from its key, not a stored room); a space
     * written by a build before 0.3.013 has those trailing after the objects, which {@link #read} leaves unread.
     */
    static final class Index {
        long gen;int parts;
        final LinkedHashMap<String,Ref> objects=new LinkedHashMap<>();
        boolean has(byte[] id,long gen){for(Ref r:objects.values())if(r.gen==gen&&Arrays.equals(r.id,id))return true;return false;}
        byte[] write() throws IOException {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(1);out.writeLong(gen);out.writeInt(objects.size());
            for(Map.Entry<String,Ref> one:objects.entrySet()){out.writeUTF(one.getKey());out.write(one.getValue().id);out.writeLong(one.getValue().gen);out.writeInt(one.getValue().parts);}
            return bytes.toByteArray();
        }
        static Index read(byte[] said) throws IOException {
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(said));
            if(in.readInt()!=1)throw new IOException("Unknown");
            Index made=new Index();made.gen=in.readLong();
            int n=in.readInt();if(n<0||n>1_000_000)throw new IOException("Damaged");
            for(int i=0;i<n;i++){String name=in.readUTF();byte[] id=new byte[16];in.readFully(id);made.objects.put(name,new Ref(id,in.readLong(),in.readInt()));}
            // A space from a build before 0.3.013 has its remembered keys and room after this: left unread, so it opens whole.
            return made;
        }
        Index copy(){Index c=new Index();c.gen=gen;c.parts=parts;c.objects.putAll(objects);return c;}
    }
    /** A slot sealed and waiting for a batch: where it goes ({@code -1}: a free slot, chosen then), and what it says. */
    static final class Entry {
        final int target;final byte[] inner;final Head head;
        Entry(int target,byte[] inner,Head head){this.target=target;this.inner=inner;this.head=head;}
        boolean filler(){return Arrays.equals(head.id,FILLER);}
    }

    /** What one password opens. Its keys and contents go when it closes; what it still has to write stays, sealed. */
    final class Space {
        byte[] enc,mark;
        /** Every slot known to be this space's, and what it holds (a filler holds nothing). */
        final Map<Integer,Head> own=new HashMap<>();
        /** The last index whole on disk, and the newest (on disk or waiting). */
        Index committed,newest;
        final List<Entry> queue=new ArrayList<>();
        long gens;
        /** Its own order of all the data slots, from its key: where it grows into (decision 116). Made once, dropped on rekey. */
        private int[] order;
        /** Some of it could not be read: written over, or cut. */
        boolean damaged;
        /** What it held when it was opened, by name; handed to whoever shows it, then let go here. */
        Map<String,byte[]> contents;

        Space(byte[] enc,byte[] mark){this.enc=enc.clone();this.mark=mark.clone();}

        /** What it held when opened, handed over once. */
        Map<String,byte[]> take(){synchronized(Slots.this){Map<String,byte[]> c=contents;contents=null;return c==null?new LinkedHashMap<>():c;}}
        boolean damaged(){synchronized(Slots.this){return damaged;}}

        /** Bytes of it still waiting for batches: what is said inside, so nobody leaves thinking it is written. */
        long waiting(){synchronized(Slots.this){long n=0;for(Entry e:queue)if(!e.filler())n+=SLOT;return n;}}
        /**
         * How many batches, one a minute, until what it says is all written: none where nothing of it waits; else every
         * slot queued up to its last, its own and those ahead of it, its fillers too, {@link #BATCH} to a batch. For the
         * words inside, in minutes rather than bytes (the owner, 2026-10-06: "Why it says 192 KB to go and then MB?";
         * decision 114).
         */
        int batchesLeft() {
            synchronized(Slots.this) {
                if(waiting()==0)return 0;
                int ahead=0;boolean listed=false;
                for(Space s:spaces){ahead+=s.queue.size();if(s==this){listed=true;break;}}
                if(!listed)ahead+=queue.size();
                return (ahead+BATCH-1)/BATCH;
            }
        }
        /** How many slots it uses now, for what is said inside: for the tests that a rewrite reuses its own and does not grow. */
        int used(){synchronized(Slots.this){int n=0;for(Head h:own.values())if(h!=null)n++;return Math.max(n,own.size());}}
        /** For the test of what a space holds, slot by slot: its index as written. */
        byte[] indexForTest() throws IOException {synchronized(Slots.this){return newest.write();}}

        /**
         * This space's own order of all the data slots, from its mark key (decision 116): a deterministic Fisher-Yates
         * shuffle of {@code [FIRST,count)} driven by an HMAC-of-the-key stream, so a space always grows into the same places
         * and another password's order is unrelated, which spreads the spaces and lets them meet only by chance. Made once
         * and kept; dropped when the key changes (see {@link #rekey}).
         */
        private int[] order() throws GeneralSecurityException {
            if(order!=null)return order;
            int n=Math.max(0,count-FIRST);int[] seq=new int[n];for(int i=0;i<n;i++)seq[i]=FIRST+i;
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(mark,"HmacSHA256"));
            byte[] block=null;int used=0,b=0;
            for(int i=n-1;i>0;i--) {
                long r=0;
                for(int k=0;k<8;k++) {
                    if(block==null||used>=block.length){mac.update((byte)(b>>>24));mac.update((byte)(b>>>16));mac.update((byte)(b>>>8));mac.update((byte)b);block=mac.doFinal();b++;used=0;}
                    r=(r<<8)|(block[used++]&0xFFL);
                }
                int j=(int)Long.remainderUnsigned(r,i+1);int t=seq[i];seq[i]=seq[j];seq[j]=t;
            }
            order=seq;return seq;
        }

        private boolean live(Head h) {
            if(h==null||Arrays.equals(h.id,FILLER))return false;
            return partOf(h,committed)||partOf(h,newest);
        }
        private boolean partOf(Head h,Index index) {
            if(index==null)return false;
            if(Arrays.equals(h.id,INDEX))return h.gen==index.gen;
            return index.has(h.id,h.gen);
        }

        /**
         * A change: these things written anew, those gone. Sealed now, written by the next batches. Refused whole, with
         * nothing changed, when it does not fit.
         */
        void put(Map<String,byte[]> changed,Collection<String> removed) throws Full, GeneralSecurityException, IOException {
            synchronized(Slots.this) {
                if(enc==null)throw new IllegalStateException("Closed");
                Index next=newest.copy();next.gen=++gens;
                for(String name:removed)next.objects.remove(name);
                List<Object[]> parts=new ArrayList<>();
                for(Map.Entry<String,byte[]> one:changed.entrySet()) {
                    Ref was=next.objects.get(one.getKey());
                    byte[] id=was!=null?was.id:new byte[16];if(was==null)RANDOM.nextBytes(id);
                    byte[] packed=deflate(one.getValue());int pieces=Math.max(1,(packed.length+DATA-1)/DATA);
                    long gen=++gens;next.objects.put(one.getKey(),new Ref(id,gen,pieces));
                    for(int p=0;p<pieces;p++)parts.add(new Object[]{new Head(id,gen,p,pieces,Math.min(DATA,packed.length-p*DATA)),packed,p*DATA});
                }
                byte[] said=deflate(next.write());int pieces=Math.max(1,(said.length+DATA-1)/DATA);next.parts=pieces;
                for(int p=0;p<pieces;p++)parts.add(new Object[]{new Head(INDEX,next.gen,p,pieces,Math.min(DATA,said.length-p*DATA)),said,p*DATA});
                // What waits and is not part of this state any more is let go, before any spare slot is counted.
                List<Entry> kept=new ArrayList<>();
                for(Entry e:queue) {
                    if(e.filler()){Head there=own.get(e.target);if(!(partOf(there==null?FILLED:there,committed)||partOf(there==null?FILLED:there,next)))kept.add(e);continue;}
                    if(!Arrays.equals(e.head.id,INDEX)&&next.has(e.head.id,e.head.gen))kept.add(e);
                }
                Index before=newest;newest=next;
                Set<Integer> aimed=new HashSet<>();for(Entry e:kept)if(e.target>=0&&!e.filler())aimed.add(e.target);
                // Another open space's slots are off limits: the ones it already holds and the ones it has queued to write
                // but a batch has not placed yet, which it will claim as its own the moment it does. Count only its own and
                // two freshly made spaces, neither written, pick from their own orders blind to each other and can land on
                // the same slot, which both then own (decision 116: "never ... open at the same moment", here for a space
                // not yet written too).
                Set<Integer> theirs=new HashSet<>();
                for(Space s:spaces)if(s!=this){theirs.addAll(s.own.keySet());for(Entry e:s.queue)if(e.target>=0)theirs.add(e.target);}
                // First its own slots that hold nothing named now: reused, so a rewrite of what it holds never grows it.
                Set<Integer> chosen=new HashSet<>();List<Integer> spare=new ArrayList<>();
                for(Map.Entry<Integer,Head> one:own.entrySet())
                    if(!live(one.getValue())&&!aimed.contains(one.getKey())&&!theirs.contains(one.getKey())){spare.add(one.getKey());chosen.add(one.getKey());}
                java.util.Collections.shuffle(spare,RANDOM);
                // Then, where it needs more, the next slots in its own order it does not already own, writing over a closed
                // space's if they fall there (decision 116: accepted overwrite), never over its own live slots nor a space
                // open here. Full only when its order is spent, that is when it has taken the whole file.
                if(parts.size()>spare.size()) {
                    for(int at:order()) {
                        if(spare.size()>=parts.size())break;
                        if(own.containsKey(at)||theirs.contains(at)||aimed.contains(at)||chosen.contains(at))continue;
                        spare.add(at);chosen.add(at);
                    }
                    if(parts.size()>spare.size()){newest=before;throw new Full();}
                }
                List<Entry> fresh=new ArrayList<>();
                for(Object[] p:parts) {
                    Head h=(Head)p[0];int at=spare.remove(spare.size()-1);
                    final int taken=at;kept.removeIf(e->e.filler()&&e.target==taken);
                    fresh.add(new Entry(at,seal(enc,mark,h,(byte[])p[1],(Integer)p[2],h.length),h));
                }
                queue.clear();queue.addAll(kept);queue.addAll(fresh);
            }
        }

        /** A new password for the same things: all of it sealed again, and the old slots filled with nothing once it is written. */
        void rekey(char[] password) throws Full, GeneralSecurityException, IOException {
            synchronized(Slots.this) {
                if(enc==null)throw new IllegalStateException("Closed");
                byte[][] keys=keys(password);
                Map<String,byte[]> all=readAll();
                byte[] oldMark=mark;
                Index saved=newest;List<Entry> was=new ArrayList<>(queue);byte[] oldEnc=enc;int[] oldOrder=order;
                enc=keys[0];mark=keys[1];order=null;queue.clear();
                // Every name written again under the new key, as new writings, placed by the new key's own order.
                newest=newest.copy();
                try{put(all,List.of());}
                catch(Full full){enc=oldEnc;mark=oldMark;order=oldOrder;newest=saved;queue.clear();queue.addAll(was);throw full;}
                Arrays.fill(oldEnc,(byte)0);Arrays.fill(oldMark,(byte)0);
            }
        }

        /** Everything it holds now, read back from its slots and from what waits: for sealing it again under a new key. */
        private Map<String,byte[]> readAll() throws IOException, GeneralSecurityException {
            Map<String,byte[]> parts=new HashMap<>();Map<String,Head> heads=new HashMap<>();
            try(RandomAccessFile in=new RandomAccessFile(file,"r")) {
                for(Map.Entry<Integer,Head> one:own.entrySet()) {
                    if(one.getValue()==null||!live(one.getValue()))continue;
                    Object[] read=unseal(enc,readInner(in,one.getKey()));
                    if(read!=null)keep((Head)read[0],(byte[])read[1],parts,heads);
                }
            }
            for(Entry e:queue){Object[] read=unseal(enc,e.inner);if(read!=null)keep((Head)read[0],(byte[])read[1],parts,heads);}
            Map<String,byte[]> all=new LinkedHashMap<>();
            for(Map.Entry<String,Ref> one:newest.objects.entrySet()) {
                byte[] said=assemble(one.getValue().id,one.getValue().gen,one.getValue().parts,parts);
                if(said==null)continue;
                try{all.put(one.getKey(),inflate(said));}catch(DataFormatException cut){/* lost already */}
            }
            for(byte[] kept:parts.values())Arrays.fill(kept,(byte)0);
            return all;
        }

        /**
         * Deleted for good (the owner, 2026-10-07: "there should be a way to delete the privatespace"; decision 117): every
         * slot this space owns is queued to become a fresh filler, sealed as the batch seals a freed slot (random under this
         * device's own layer, so a filler is indistinguishable from a slot never used), its index slots FIRST so the newest
         * index is gone before anything else and a cut-short erase can never be opened to anything whole again, the rest over
         * the following batches at the same fixed rhythm. It does NOT burst-write the whole space at once, which would betray
         * the timing: the fillers go out through the ordinary queue, {@link #BATCH} to a batch. After this the keys are
         * forgotten and it is dropped from the open set once the fillers have drained; nothing records that a space was
         * deleted, and it touches only its own slots, never another space's.
         */
        void erase() {
            synchronized(Slots.this) {
                if(enc==null)return; // closed already: no key to seal fillers with, and nothing of it is open to erase
                try {
                    // Its index slots before the rest, so the newest index is gone first and what is left opens nothing whole.
                    List<Entry> fillers=new ArrayList<>();List<Integer> rest=new ArrayList<>();
                    for(Map.Entry<Integer,Head> one:own.entrySet()) {
                        Head h=one.getValue();
                        if(h!=null&&Arrays.equals(h.id,INDEX))fillers.add(filler(one.getKey()));else rest.add(one.getKey());
                    }
                    for(int at:rest)fillers.add(filler(at));
                    // Nothing of the space is written any more: what waited is dropped, only fillers over its own slots remain.
                    queue.clear();queue.addAll(fillers);
                } catch(GeneralSecurityException never){/* the same ciphers sealed everything else */}
                // Nothing is live now, so settle adds no fillers of its own; the keys go, and it leaves the open set when drained.
                committed=null;newest=null;
                forget();
            }
        }
        /** A fresh filler sealed for one of this space's own slots, as {@link #fill} and {@link #closeQuietly} seal theirs. */
        private Entry filler(int at) throws GeneralSecurityException {
            Head nothing=new Head(FILLER,0,0,1,0);
            return new Entry(at,seal(enc,mark,nothing,null,0,0),nothing);
        }

        /** After a batch: an index now whole on disk is the committed one, and slots nothing names are filled with nothing. */
        private void settle() {
            if(newest!=null&&newest!=committed) {
                Set<String> have=new HashSet<>();for(Head h:own.values())if(h!=null)have.add(key(h.id,h.gen,h.part));
                if(whole(newest,have))committed=newest;
            }
            if(enc==null)return;
            fill();
        }
        /** A filler for every slot of its own that holds something nothing names now, unless one is on its way. */
        private void fill() {
            Set<Integer> aimed=new HashSet<>();for(Entry e:queue)if(e.target>=0)aimed.add(e.target);
            try {
                for(Map.Entry<Integer,Head> one:own.entrySet()) {
                    Head h=one.getValue();
                    if(h==null||Arrays.equals(h.id,FILLER)||live(h)||aimed.contains(one.getKey()))continue;
                    Head nothing=new Head(FILLER,0,0,1,0);
                    queue.add(new Entry(one.getKey(),seal(enc,mark,nothing,null,0,0),nothing));
                }
            } catch(GeneralSecurityException never){/* the same ciphers sealed everything else */}
        }

        /**
         * Closed: the keys and contents gone. What still waits is written by the next batches, and after it, fillers for
         * every slot of its own the newest state does not name, sealed now, while there is a key to seal them with.
         */
        private void closeQuietly() {
            if(enc!=null) {
                Set<Integer> aimed=new HashSet<>();for(Entry e:queue)if(e.target>=0)aimed.add(e.target);
                try {
                    for(Map.Entry<Integer,Head> one:own.entrySet()) {
                        Head h=one.getValue();
                        if(h==null||Arrays.equals(h.id,FILLER)||partOf(h,newest)||aimed.contains(one.getKey()))continue;
                        Head nothing=new Head(FILLER,0,0,1,0);
                        queue.add(new Entry(one.getKey(),seal(enc,mark,nothing,null,0,0),nothing));
                    }
                } catch(GeneralSecurityException never){/* as above */}
            }
            forget();
        }
        private void forget() {
            if(enc!=null)Arrays.fill(enc,(byte)0);if(mark!=null)Arrays.fill(mark,(byte)0);
            enc=null;mark=null;contents=null;order=null;
        }
    }
    /** Stands for a slot that holds nothing named, where a head is wanted. */
    private static final Head FILLED=new Head(FILLER,0,0,1,0);
}
