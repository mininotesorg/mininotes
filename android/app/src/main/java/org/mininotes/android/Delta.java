// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Only what changed in a note, sent in place of the whole of it (decision 320; the owner, 2026-11-03).
 *
 * <p>A note went whole at every revision: a line typed into a note of 90,000 characters sent all 90,000 again, to every
 * device that has it, through relays and carriers. What changed is the stretches of lines {@link Merge} already reads a
 * side's writing as. Only against a text both devices are known to hold exactly: the revision the other device confirmed it
 * took ({@link NoteStore#agreedAt}), kept here word for word. The far end rebuilds the whole text from its own copy of that
 * text and then goes on exactly as with a whole note: weighed, merged, kept as a version, answered.
 *
 * <p>Nothing is guessed. The SHA-256 of the text it was made against travels, so the far end finds its copy of it by what
 * it says rather than by a number two devices may use for two texts; and that of the text rebuilt, which must match. Where
 * either does not, nothing is taken and the whole note is asked for (see {@link #onto}).
 *
 * <p>Holds no Android types, so what it makes and takes is unit tested without a device.
 */
final class Delta {

    /** What says a change follows, in a parcel's tail (see {@link Parcel#open}): "MND1". */
    static final int MARK=0x4d4e4431;
    /**
     * The body of a parcel that carries a change instead (see {@link Parcel#asChange}): no note typed or pasted holds a NUL,
     * so a reader that finds this and no change it can read refuses it rather than taking these few characters as the note.
     */
    static final String IN_TAIL="\u0000MND1";
    /** Below this many bytes a note goes whole: a change saves too little to be worth a second way of reading it. */
    static final int WORTH_FROM=2048;
    /** The most stretches one change may say: a stranger cannot make a reader allocate more. */
    static final int STRETCHES_MOST=100_000;
    /**
     * The most cells the line by line comparison may fill ({@link Merge#changes} is lines times lines): past it, the lines
     * between the first and last that changed are said as one stretch, which is still exact, only larger.
     */
    static final long CELLS_MOST=4_000_000L;

    /** The revision it was made against, as the sender counts it: where the far end looks first for its copy. */
    final long base;
    /** The SHA-256 of the text it was made against, and of the text it makes. */
    final byte[] baseSha, sha;
    /** The stretches of the base's lines replaced, in order and never overlapping. */
    final List<Merge.Change> stretches;

    private Delta(long base,byte[] baseSha,byte[] sha,List<Merge.Change> stretches) {
        this.base=base;this.baseSha=baseSha;this.sha=sha;this.stretches=stretches;
    }

    /**
     * What goes to one device instead of the whole note, or null where the whole note goes: the device has not said it reads
     * changes (a build from before is the one that has said nothing), the whole is asked for (Sync, or a device that could not
     * rebuild one), they have confirmed no revision yet (the first time), the text of that revision is not kept here exactly,
     * or the change would not be clearly smaller.
     *
     * @param agreed the revision the device last confirmed it took, 0 where none
     * @param base   this device's exact copy of that revision, or null where none is kept
     */
    static Delta going(boolean readsChanges,boolean whole,long agreed,String base,String now) {
        if(!readsChanges||whole||agreed<=0||base==null||now==null)return null;
        return worth(base,agreed,now);
    }

    /** A change from {@code base} to {@code now}, or null where it would not be clearly smaller than {@code now} whole. */
    static Delta worth(String base,long baseRevision,String now) {
        int whole=now.getBytes(StandardCharsets.UTF_8).length;
        if(whole<WORTH_FROM)return null;
        Delta change=between(base,baseRevision,now);
        // Clearly smaller: at most half. Near the whole, the whole is the simpler thing to send and to read.
        return change.size()*2<=whole?change:null;
    }

    /** The change from {@code base} to {@code now}, however big. */
    static Delta between(String base,long baseRevision,String now) {
        List<String> old=Merge.lines(base), lines=Merge.lines(now);
        // The lines both start and end with are left out of the comparison: a line typed into a long note is one stretch,
        // found without comparing every line with every other.
        int head=0;
        while(head<old.size()&&head<lines.size()&&old.get(head).equals(lines.get(head)))head++;
        int tail=0;
        while(tail<old.size()-head&&tail<lines.size()-head
            &&old.get(old.size()-1-tail).equals(lines.get(lines.size()-1-tail)))tail++;
        List<String> was=old.subList(head,old.size()-tail), is=lines.subList(head,lines.size()-tail);
        List<Merge.Change> stretches=new ArrayList<>();
        if(!was.isEmpty()||!is.isEmpty()) {
            if((long)(was.size()+1)*(is.size()+1)>CELLS_MOST)
                stretches.add(new Merge.Change(head,head+was.size(),new ArrayList<>(is)));
            else for(Merge.Change one:Merge.changes(new ArrayList<>(was),new ArrayList<>(is)))
                stretches.add(new Merge.Change(head+one.from,head+one.to,one.said));
        }
        return new Delta(baseRevision,sha(base),sha(now),stretches);
    }

    /**
     * The whole text, rebuilt from this device's copy of the base, or null where it cannot be, and then nothing may be taken
     * and the whole note is to be asked for: the copy is not the text it was made against, a stretch does not fit it, or what
     * it makes is not, to the last byte, what the sender has.
     */
    String onto(String base) {
        if(base==null||!Arrays.equals(sha(base),baseSha))return null;
        List<String> old=Merge.lines(base), out=new ArrayList<>();
        int at=0;
        for(Merge.Change one:stretches) {
            if(one.from<at||one.to<one.from||one.to>old.size())return null;
            out.addAll(old.subList(at,one.from));
            out.addAll(one.said);
            at=one.to;
        }
        out.addAll(old.subList(at,old.size()));
        String text=String.join("\n",out);
        return Arrays.equals(sha(text),sha)?text:null;
    }

    /** How many bytes it takes in a parcel, its mark included. */
    int size() {
        int bytes=4+8+32+32+4;
        for(Merge.Change one:stretches) {
            bytes+=12;
            for(String line:one.said)bytes+=4+line.getBytes(StandardCharsets.UTF_8).length;
        }
        return bytes;
    }

    /** Written after its {@link #MARK}: the base revision, the two SHA-256, then each stretch, its lines and their words. */
    void write(DataOutputStream out) throws IOException {
        out.writeLong(base);out.write(baseSha);out.write(sha);
        out.writeInt(stretches.size());
        for(Merge.Change one:stretches) {
            out.writeInt(one.from);out.writeInt(one.to);out.writeInt(one.said.size());
            for(String line:one.said)Parcel.put(out,line,Parcel.TEXT_MOST);
        }
    }

    /** One read, its mark read already; null where it does not read whole: half a change is no change. */
    static Delta read(DataInputStream in) {
        try {
            long base=in.readLong();
            byte[] baseSha=new byte[32], sha=new byte[32];
            in.readFully(baseSha);in.readFully(sha);
            int many=in.readInt();
            if(many<0||many>STRETCHES_MOST)return null;
            List<Merge.Change> stretches=new ArrayList<>(Math.min(many,1024));
            long words=0;
            for(int at=0;at<many;at++) {
                int from=in.readInt(), to=in.readInt(), lines=in.readInt();
                if(from<0||to<from||lines<0||lines>in.available()/4)return null;
                List<String> said=new ArrayList<>(Math.min(lines,1024));
                for(int line=0;line<lines;line++) {
                    String one=Parcel.get(in,Parcel.TEXT_MOST);
                    words+=one.length();
                    if(words>Parcel.TEXT_MOST)return null;
                    said.add(one);
                }
                stretches.add(new Merge.Change(from,to,said));
            }
            return new Delta(base,baseSha,sha,stretches);
        } catch(IOException | IllegalArgumentException damaged){return null;}
    }

    /** The SHA-256 of a text, as UTF-8. */
    static byte[] sha(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest((text==null?"":text).getBytes(StandardCharsets.UTF_8));
        } catch(Exception noDigest) {
            // Cannot happen: SHA-256 is required of every Java. Named rather than swallowed, as in Parcel.localId.
            throw new IllegalStateException("SHA-256 is missing",noDigest);
        }
    }
}
