// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Private notes' slots (decision 111): the same file for everybody, and nothing in it without a password. */
public class SlotsTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    private static final int FEW=80,QUICK=1000;
    private final byte[] mask=new byte[32];{new Random(7).nextBytes(mask);}

    private Slots slots(File file) throws Exception {Slots s=new Slots(file,mask,FEW,QUICK);s.start();return s;}
    private static Map<String,byte[]> one(String name,String said){Map<String,byte[]> m=new LinkedHashMap<>();m.put(name,said.getBytes(StandardCharsets.UTF_8));return m;}
    private static void drain(Slots s) throws Exception {for(int i=0;i<200&&s.anyWaiting();i++)s.batch();assertFalse("everything written",s.anyWaiting());}
    private static String hash(File f) throws Exception {return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath())));}
    private static boolean contains(byte[] all,String word) {
        byte[] w=word.getBytes(StandardCharsets.UTF_8);
        outer:for(int i=0;i+w.length<=all.length;i++){for(int j=0;j<w.length;j++)if(all[i+j]!=w[j])continue outer;return true;}
        return false;
    }

    @Test public void everyInstallHasTheSameSizeOfRandomFromItsFirstStart() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        assertEquals((long)FEW*Slots.SLOT,f.length());
        byte[] all=Files.readAllBytes(f.toPath());
        // Random bytes do not compress: no header, no runs, nothing a tool could tell from noise.
        assertTrue(Slots.deflate(all).length>all.length*0.99);
        assertFalse(new File(f.getPath()+".new").exists());
        s.batch();s.batch();
        assertEquals((long)FEW*Slots.SLOT,f.length());
    }

    @Test public void theFullSizeIsSixtyFourMebibytesAndABatchOneMebibyte() {
        assertEquals(64L<<20,(long)Slots.COUNT*Slots.SLOT);
        assertEquals(1L<<20,(long)Slots.BATCH*Slots.SLOT);
        assertEquals(Slots.SLOT,Slots.MASK+Slots.NONCE+Slots.MARK+Slots.HEAD+Slots.DATA+Slots.TAG);
    }

    @Test public void aPasswordNothingAnswersToOpensNothingAndChangesNothing() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        String before=hash(f);
        assertNull(s.open("nothing here".toCharArray()));
        assertEquals(before,hash(f));
        assertFalse(s.anyOpen());assertFalse(s.anyWaiting());
    }

    @Test public void madeWrittenClosedAndOpenedAgainWithItsWords() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space made=s.make("a long phrase".toCharArray(),one("note:1","Pick up the parcel on Tuesday"));
        String before=hash(f);
        // A change is never written outside a batch.
        assertEquals(before,hash(f));
        assertTrue(made.waiting()>0);
        drain(s);
        s.close();
        assertFalse(s.anyOpen());
        Slots again=slots(f);
        Slots.Space open=again.open("a long phrase".toCharArray());
        assertNotNull(open);
        assertEquals("Pick up the parcel on Tuesday",new String(open.take().get("note:1"),StandardCharsets.UTF_8));
        byte[] all=Files.readAllBytes(f.toPath());
        assertFalse(contains(all,"parcel"));assertFalse(contains(all,"a long phrase"));assertFalse(contains(all,"note:1"));
    }

    @Test public void everyBatchWritesTheSameAmountWhateverIsPrivate() throws Exception {
        File used=folder.newFile(),never=folder.newFile();used.delete();never.delete();
        Slots a=slots(used),b=slots(never);
        a.make("p".toCharArray(),one("note:1","x".repeat(300_000)));
        for(int i=0;i<10;i++){a.batch();b.batch();}
        assertEquals(b.written,a.written);assertEquals(b.batches,a.batches);
        assertEquals(never.length(),used.length());
        // Thirty-three slots a batch: the journal's head, its sixteen, and the sixteen in place.
        assertEquals(11L*33*Slots.SLOT,a.written);
    }

    @Test public void aChangeTooBigForOneBatchWaitsAndIsWholeOnceWritten() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Random r=new Random(3);byte[] big=new byte[Slots.DATA*20];r.nextBytes(big);
        Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:a",big);
        s.make("p".toCharArray(),m);
        s.batch();
        // Not whole yet: the old state (nothing) is what opens from the disk alone.
        assertTrue(s.anyWaiting());
        drain(s);s.close();
        Slots again=slots(f);
        assertArrayEquals(big,again.open("p".toCharArray()).take().get("file:a"));
    }

    @Test public void closedWithWritingWaitingItGoesOnAndOpensAgainFromWhatWaits() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Random r=new Random(4);byte[] big=new byte[Slots.DATA*25];r.nextBytes(big);
        Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:a",big);
        s.make("p".toCharArray(),m);
        s.batch();s.close();
        assertFalse(s.anyOpen());assertTrue(s.anyWaiting());
        // Opened again before it is all written: what waits is its newest.
        Slots.Space back=s.open("p".toCharArray());
        assertArrayEquals(big,back.take().get("file:a"));
        back.put(one("note:b","and more"),List.of());
        s.close();drain(s);
        Slots.Space last=slots(f).open("p".toCharArray());
        Map<String,byte[]> held=last.take();
        assertArrayEquals(big,held.get("file:a"));
        assertEquals("and more",new String(held.get("note:b"),StandardCharsets.UTF_8));
    }

    /** Three passwords make three independent spaces, each holding its own content and opening on its own (decision 116). */
    @Test public void threeSpacesWithThreePasswordsEachHoldTheirOwnAndOpenIndependently() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        s.make("alpha phrase".toCharArray(),one("note:a","apples"));
        s.make("bravo phrase".toCharArray(),one("note:b","bananas"));
        s.make("charlie phrase".toCharArray(),one("note:c","cherries"));
        drain(s);s.close();
        Slots again=slots(f);
        assertEquals("apples",new String(again.open("alpha phrase".toCharArray()).take().get("note:a"),StandardCharsets.UTF_8));
        assertEquals("bananas",new String(again.open("bravo phrase".toCharArray()).take().get("note:b"),StandardCharsets.UTF_8));
        assertEquals("cherries",new String(again.open("charlie phrase".toCharArray()).take().get("note:c"),StandardCharsets.UTF_8));
        // A wrong password opens nothing and changes nothing.
        String before=hash(f);
        assertNull(again.open("delta phrase".toCharArray()));
        assertEquals(before,hash(f));
        byte[] all=Files.readAllBytes(f.toPath());
        for(String w:new String[]{"alpha phrase","bravo phrase","charlie phrase","apples","bananas","cherries"})assertFalse(w,contains(all,w));
    }

    /** Rewriting one space many times reuses its own slots: it never grows without bound, and never corrupts itself. */
    @Test public void rewritingOneSpaceManyTimesReusesItsOwnSlotsAndNeverGrowsWithoutBound() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space mine=s.make("p".toCharArray(),one("note:1","first words"));drain(s);
        int settled=mine.used();
        for(int i=0;i<150;i++){mine.put(one("note:1","round "+i+", a handful of words, kept here"),List.of());drain(s);}
        assertTrue("it reuses its own slots, never growing without bound: "+mine.used()+" from "+settled,mine.used()<=settled+4);
        s.close();
        assertEquals("round 149, a handful of words, kept here",new String(slots(f).open("p".toCharArray()).take().get("note:1"),StandardCharsets.UTF_8));
    }

    /**
     * Three spaces with content write the same batches, the same bytes and the same file size as a fresh install never used:
     * nothing of how many spaces exist, or that any do, shows in the rhythm or the size (decision 116). The Desktop test
     * {@code DesktopPrivateTest} proves the same file list and no plaintext of three spaces and fifty photographs.
     */
    @Test public void threeSpacesWriteTheSameAmountAndSizeAsAFreshInstall() throws Exception {
        File used=folder.newFile(),never=folder.newFile();used.delete();never.delete();
        Slots a=slots(used),b=slots(never);
        Slots.Space[] three={a.make("one phrase".toCharArray(),new LinkedHashMap<>()),a.make("two phrase".toCharArray(),new LinkedHashMap<>()),a.make("three phrase".toCharArray(),new LinkedHashMap<>())};
        Random r=new Random(8);
        for(int i=0;i<9;i++){byte[] photo=new byte[Slots.DATA];r.nextBytes(photo);Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:"+i,photo);three[i%3].put(m,List.of());}
        for(int i=0;i<500&&a.anyWaiting();i++){a.batch();b.batch();}
        assertFalse("all the private content is written",a.anyWaiting());
        for(int i=0;i<3;i++){a.batch();b.batch();}
        assertEquals("the same batches, used or not",b.batches,a.batches);
        assertEquals("the same bytes written",b.written,a.written);
        assertEquals("the same file size",never.length(),used.length());
        assertEquals((long)FEW*Slots.SLOT,used.length());
        byte[] all=Files.readAllBytes(used.toPath());
        for(String w:new String[]{"one phrase","two phrase","three phrase","file:0","file:8"})assertFalse(w,contains(all,w));
    }

    /**
     * A space whose order lands on a closed space's slots writes over them: the owner's explicit choice, nothing records
     * spaces to prevent it (decision 116). The one doing the writing always stays whole; the one sat upon loses itself.
     */
    @Test public void growingOverAClosedSpaceIsAcceptedAndTheWriterStaysWhole() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=new Slots(f,mask,52,QUICK);s.start();
        Slots.Space first=s.make("first phrase".toCharArray(),one("note:1","believable"));
        Random r=new Random(9);
        for(int i=0;i<6;i++){byte[] b=new byte[Slots.DATA];r.nextBytes(b);Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:"+i,b);first.put(m,List.of());}
        drain(s);s.close();
        // A second space fills the whole file, so it is certain to have written over the first's slots.
        Slots again=new Slots(f,mask,52,QUICK);again.start();
        Slots.Space second=again.make("second phrase".toCharArray(),one("note:s","the real one"));
        int added=0;
        try{for(;added<200;added++){byte[] b=new byte[Slots.DATA*2];r.nextBytes(b);Map<String,byte[]> m=new LinkedHashMap<>();m.put("big:"+added,b);second.put(m,List.of());drain(again);}fail("never full");}
        catch(Slots.Full full){/* it has taken the whole file */}
        assertTrue("the second grew",added>3);
        drain(again);again.close();
        Slots last=new Slots(f,mask,52,QUICK);last.start();
        // The writer is whole, every piece of it.
        Map<String,byte[]> held=last.open("second phrase".toCharArray()).take();
        assertEquals("the real one",new String(held.get("note:s"),StandardCharsets.UTF_8));
        for(int i=0;i<added;i++)assertEquals(Slots.DATA*2,held.get("big:"+i).length);
        last.close();
        // The first, sat upon, has lost itself: most of its slots now answer to the second, so its password opens nothing
        // whole of it (null, damaged, or short of the six files it had). The owner's accepted overwrite.
        Slots check=new Slots(f,mask,52,QUICK);check.start();
        Slots.Space firstAgain=check.open("first phrase".toCharArray());
        assertTrue("the first was written over",firstAgain==null||firstAgain.damaged()||firstAgain.take().size()<6);
    }

    /** A space's index and everything its password opens name nothing of another space: not its keys, its ids, or its words. */
    @Test public void aSpacesIndexAndContentsNameNothingOfAnother() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space one=s.make("one phrase".toCharArray(),one("note:1","one's own words"));
        Slots.Space two=s.make("two phrase".toCharArray(),one("note:2","two's own words"));drain(s);
        byte[] twoEnc=two.enc.clone(),twoMark=two.mark.clone();
        List<byte[]> twoIds=new java.util.ArrayList<>();for(Slots.Ref ref:two.newest.objects.values())twoIds.add(ref.id.clone());
        s.close();
        Slots again=slots(f);
        Slots.Space opened=again.open("one phrase".toCharArray());
        byte[] index=opened.indexForTest();
        Map<String,byte[]> held=opened.take();
        for(byte[] secret:new byte[][]{twoEnc,twoMark})assertFalse(containsBytes(index,secret));
        for(byte[] id:twoIds)assertFalse(containsBytes(index,id));
        assertEquals(java.util.Set.of("note:1"),held.keySet());
        for(byte[] v:held.values())for(byte[] secret:new byte[][]{twoEnc,twoMark})assertFalse(containsBytes(v,secret));
    }
    private static boolean containsBytes(byte[] all,byte[] w) {
        outer:for(int i=0;i+w.length<=all.length;i++){for(int j=0;j<w.length;j++)if(all[i+j]!=w[j])continue outer;return true;}
        return false;
    }

    @Test public void aNewPasswordOpensEverythingAndTheOldOneNothingOnceWritten() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        // Two independent spaces (decision 116); rekeying one never touches the other, which is open and so kept from.
        Slots.Space other=s.make("other".toCharArray(),one("note:o","untouched"));drain(s);
        Slots.Space mine=s.make("old one".toCharArray(),one("note:1","kept"));drain(s);
        mine.put(one("note:2","also kept"),List.of());drain(s);
        mine.rekey("new one".toCharArray());
        drain(s);s.close();
        Slots again=slots(f);
        assertNull(again.open("old one".toCharArray()));
        Map<String,byte[]> held=again.open("new one".toCharArray()).take();
        assertEquals("kept",new String(held.get("note:1"),StandardCharsets.UTF_8));
        assertEquals("also kept",new String(held.get("note:2"),StandardCharsets.UTF_8));
        again.close();
        assertEquals("untouched",new String(slots(f).open("other".toCharArray()).take().get("note:o"),StandardCharsets.UTF_8));
    }

    @Test public void theOldPasswordOpensNothingEvenWhileTheChangeIsStillBeingWritten() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space mine=s.make("old one".toCharArray(),one("note:1","kept"));drain(s);
        mine.rekey("new one".toCharArray());
        // Not written yet: the slots are still the old key's, but they are the open space's, and nothing else reads them.
        assertTrue(s.anyWaiting());
        assertNull(s.open("old one".toCharArray()));
        assertSame(mine,s.open("new one".toCharArray()));
    }

    @Test public void fullSaysSoWritesNothingAndLosesNothing() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space mine=s.make("p".toCharArray(),one("note:1","before it was full"));drain(s);
        Random r=new Random(6);int added=0;
        try {
            for(;added<100;added++){byte[] more=new byte[Slots.DATA*4];r.nextBytes(more);Map<String,byte[]> n=new LinkedHashMap<>();n.put("file:"+added,more);mine.put(n,List.of());drain(s);}
            fail("never full");
        } catch(Slots.Full full){assertEquals("Private storage is full",full.getMessage());}
        long waiting=mine.waiting();assertEquals(0,waiting);
        s.close();
        Map<String,byte[]> held=slots(f).open("p".toCharArray()).take();
        assertEquals("before it was full",new String(held.get("note:1"),StandardCharsets.UTF_8));
        assertEquals(added,held.size()-1);
    }

    @Test public void aBatchCutShortAfterItsJournalIsPutInPlaceAtTheNextStart() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        s.make("p".toCharArray(),one("note:1","survives the cut"));
        s.cutShort=true;
        try{s.batch();fail();}catch(java.io.IOException cut){/* the power went */}
        Slots again=slots(f);
        assertEquals("survives the cut",new String(again.open("p".toCharArray()).take().get("note:1"),StandardCharsets.UTF_8));
    }

    @Test public void aBackupCarriesItAndItOpensOnAnotherDevice() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        s.make("p".toCharArray(),one("note:1","in the backup"));drain(s);s.close();
        ByteArrayOutputStream out=new ByteArrayOutputStream();s.portable(out);
        assertEquals(s.portableSize(),out.size());
        assertFalse(contains(out.toByteArray(),"in the backup"));
        File other=folder.newFile();other.delete();
        byte[] theirs=new byte[32];new Random(99).nextBytes(theirs);
        Slots there=new Slots(other,theirs,FEW,QUICK);there.start();
        there.adopt(there.stage(new ByteArrayInputStream(out.toByteArray())));
        there.start();
        assertEquals((long)FEW*Slots.SLOT,other.length());
        assertEquals("in the backup",new String(there.open("p".toCharArray()).take().get("note:1"),StandardCharsets.UTF_8));
    }

    @Test public void deletedThingsAreFilledWithNothingOnceTheNewStateIsWritten() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space mine=s.make("p".toCharArray(),one("note:1","first words"));drain(s);
        mine.put(one("note:1","second words"),List.of());drain(s);
        mine.put(new LinkedHashMap<>(),List.of("note:1"));drain(s);
        // The slots that held the older writings are this password's still, holding nothing: what opens has none of it.
        s.close();
        Map<String,byte[]> held=slots(f).open("p".toCharArray()).take();
        assertFalse(held.containsKey("note:1"));
        assertFalse(contains(Files.readAllBytes(f.toPath()),"words"));
    }

    /**
     * A space deleted for good (decision 117): its password opens nothing after, the file is a fresh install's size, no
     * plaintext of it is left, and another space is untouched. Erasing queues a filler for every slot it owns, drained by
     * the batches at the fixed rhythm, so from the file alone a made-and-erased space and a never-used one are the same.
     */
    @Test public void aSpaceErasedOpensNothingAfterAndLeavesNoTrace() throws Exception {
        File used=folder.newFile(),never=folder.newFile();used.delete();never.delete();
        Slots a=slots(used),b=slots(never);
        Slots.Space keep=a.make("keep phrase".toCharArray(),one("note:k","kept safe"));
        Slots.Space gone=a.make("gone phrase".toCharArray(),one("note:g","to be gone"));
        Random r=new Random(11);byte[] photo=new byte[Slots.DATA*3];r.nextBytes(photo);
        Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:g",photo);gone.put(m,List.of());
        for(int i=0;i<500&&a.anyWaiting();i++)a.batch();
        assertFalse("all the content is written before it is erased",a.anyWaiting());
        gone.erase();
        // Its index goes in the next batch, before its content: erased through the queue, never burst-written at once.
        assertTrue("the fillers wait to be written over the following batches",a.anyWaiting());
        // Drained in lockstep with a fresh install, so the rhythm and the size are the same either way.
        for(int i=0;i<500&&a.anyWaiting();i++){a.batch();b.batch();}
        assertFalse("every filler is written",a.anyWaiting());
        a.close();b.close();
        Slots again=slots(used);
        assertNull("its password opens nothing after it is erased",again.open("gone phrase".toCharArray()));
        // Another space is untouched: its slots were never among the erased one's own.
        assertEquals("kept safe",new String(again.open("keep phrase".toCharArray()).take().get("note:k"),StandardCharsets.UTF_8));
        assertEquals("the same file size as a fresh install",never.length(),used.length());
        byte[] all=Files.readAllBytes(used.toPath());
        for(String w:new String[]{"gone phrase","to be gone"})assertFalse(w,contains(all,w));
    }

    /** Erasing one space never writes over another open space's slots (decision 117): it fills only its own. */
    @Test public void erasingTouchesOnlyItsOwnSlotsNotAnotherSpaces() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        Slots.Space one=s.make("one phrase".toCharArray(),one("note:1","the first space"));
        Slots.Space two=s.make("two phrase".toCharArray(),one("note:2","the second space"));
        drain(s);
        java.util.Set<Integer> theirs=new java.util.HashSet<>(two.own.keySet());
        one.erase();
        // Not one of the erased space's queued fillers aims at a slot the other space owns.
        for(Slots.Entry e:one.queue)assertFalse("filler at a slot another space owns",theirs.contains(e.target));
        drain(s);s.close();
        assertNull(slots(f).open("one phrase".toCharArray()));
        assertEquals("the second space",new String(slots(f).open("two phrase".toCharArray()).take().get("note:2"),StandardCharsets.UTF_8));
    }

    // ---- the invariants, each looped so a rare order collision cannot hide (0.3.015; decisions 116, 117) --------------

    /** Every slot a space owns or has queued to write: what it holds now, or will hold at the next batch. */
    private static java.util.Set<Integer> held(Slots.Space sp) {
        java.util.Set<Integer> s=new java.util.HashSet<>(sp.own.keySet());
        for(Slots.Entry e:sp.queue)if(e.target>=0)s.add(e.target);
        return s;
    }
    private static void noSharedSlots(String where,Slots.Space... spaces) {
        for(int i=0;i<spaces.length;i++)for(int j=i+1;j<spaces.length;j++) {
            java.util.Set<Integer> both=held(spaces[i]);both.retainAll(held(spaces[j]));
            assertTrue(where+": two open spaces share slot(s) "+both,both.isEmpty());
        }
    }
    /** An index head's id is sixteen zero bytes; a content id is random and a filler's is all ones, so this tells them apart. */
    private static boolean indexId(byte[] id){for(byte x:id)if(x!=0)return false;return true;}

    /**
     * Invariant 1 (decisions 116, 117): spaces open at the same time, two freshly made ones included, never share an owned
     * slot. A space skips every slot another open space already holds or has queued, and claims one only when a batch writes
     * it. Many iterations, varied passwords (so varied salts and orders), so a chance order collision cannot slip through.
     */
    @Test public void spacesOpenTogetherNeverShareAnOwnedSlot() throws Exception {
        for(int t=0;t<200;t++) {
            File f=folder.newFile();f.delete();
            Slots s=slots(f);
            Slots.Space one=s.make(("one phrase "+t).toCharArray(),one("note:1","first"));
            Slots.Space two=s.make(("two phrase "+t).toCharArray(),one("note:2","second"));
            Slots.Space three=s.make(("three phrase "+t).toCharArray(),one("note:3","third"));
            // Freshly made, before any batch: no two share a slot, owned or merely queued.
            noSharedSlots("made, iteration "+t,one,two,three);
            Random r=new Random(100+t);
            for(int round=0;round<3;round++) {
                for(Slots.Space sp:new Slots.Space[]{one,two,three}) {
                    byte[] more=new byte[Slots.DATA/2];r.nextBytes(more);
                    Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:"+round,more);
                    try{sp.put(m,List.of());}catch(Slots.Full full){/* a small file can fill: still never a shared slot */}
                }
                noSharedSlots("grown, iteration "+t,one,two,three);
                for(int i=0;i<4&&s.anyWaiting();i++)s.batch();
                noSharedSlots("batched, iteration "+t,one,two,three);
            }
            drain(s);
            noSharedSlots("drained, iteration "+t,one,two,three);
            s.close();
        }
    }

    /**
     * Invariant 2 (decision 117): erase queues fillers only for slots the erasing space itself wrote, never a slot another
     * open space holds; its index slots first; through the ordinary batch path at the same byte rhythm as a fresh install;
     * the erased space then opens to nothing and the other is byte-intact. Many iterations, varied passwords.
     */
    @Test public void erasingFillsOnlyItsOwnIndexFirstAndLeavesTheOtherIntact() throws Exception {
        for(int t=0;t<150;t++) {
            File used=folder.newFile(),never=folder.newFile();used.delete();never.delete();
            Slots a=slots(used),b=slots(never);
            Slots.Space keep=a.make(("keep phrase "+t).toCharArray(),one("note:k","kept safe "+t));
            Slots.Space gone=a.make(("gone phrase "+t).toCharArray(),one("note:g","to be gone"));
            Random r=new Random(200+t);byte[] photo=new byte[Slots.DATA*2];r.nextBytes(photo);
            Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:g",photo);gone.put(m,List.of());
            for(int i=0;i<500&&a.anyWaiting();i++)a.batch();
            assertFalse("iteration "+t,a.anyWaiting());
            java.util.Set<Integer> keepOwns=new java.util.HashSet<>(keep.own.keySet());
            java.util.Set<Integer> goneOwns=new java.util.HashSet<>(gone.own.keySet());
            java.util.Set<Integer> indexSlots=new java.util.HashSet<>();
            for(Map.Entry<Integer,Slots.Head> en:gone.own.entrySet())if(en.getValue()!=null&&indexId(en.getValue().id))indexSlots.add(en.getKey());
            gone.erase();
            // Only its own slots, never the other's, one filler for each; and the index slots before any other.
            java.util.Set<Integer> aimed=new java.util.HashSet<>();boolean pastIndex=false;
            for(Slots.Entry e:gone.queue) {
                assertTrue("iteration "+t+": a filler at a slot gone never owned",goneOwns.contains(e.target));
                assertFalse("iteration "+t+": a filler at a slot the other space owns",keepOwns.contains(e.target));
                if(indexSlots.contains(e.target))assertFalse("iteration "+t+": an index slot queued after a non-index one",pastIndex);
                else pastIndex=true;
                aimed.add(e.target);
            }
            assertEquals("iteration "+t+": a filler for every slot gone owned",goneOwns,aimed);
            // The fillers drain in lockstep with a fresh install: the same bytes and batches, so the rhythm is unchanged.
            long aw=a.written,bw=b.written;int ab=a.batches,bb=b.batches;
            for(int i=0;i<500&&a.anyWaiting();i++){a.batch();b.batch();}
            assertFalse("iteration "+t,a.anyWaiting());
            assertEquals("iteration "+t+": the same bytes written draining the fillers",b.written-bw,a.written-aw);
            assertEquals("iteration "+t+": the same batches",b.batches-bb,a.batches-ab);
            a.close();b.close();
            assertEquals("iteration "+t+": a fresh install's size",never.length(),used.length());
            Slots again=slots(used);
            assertNull("iteration "+t+": the erased space opens nothing",again.open(("gone phrase "+t).toCharArray()));
            assertEquals("iteration "+t+": the other space is byte-intact","kept safe "+t,
                new String(again.open(("keep phrase "+t).toCharArray()).take().get("note:k"),StandardCharsets.UTF_8));
            again.close();
        }
    }

    /**
     * Invariant 3 (decisions 116, 117): opening a space, after any writes and erases of other spaces that were never open
     * together with it, never throws (no "bytes is null"). Big footprints in a small file, so an independent space's order
     * collides with it often and the accepted-overwrite path is hammered: it recovers whole where nothing wrote over it,
     * else opens to nothing or to a space marked damaged, and whatever it does hand back is its own and readable. Many
     * iterations, varied passwords.
     */
    @Test public void openingAfterIndependentWritesAndErasesNeverThrows() throws Exception {
        int recovered=0,clobbered=0;
        for(int t=0;t<80;t++) {
            File f=folder.newFile();f.delete();
            Slots s1=slots(f);
            Slots.Space a=s1.make(("alpha phrase "+t).toCharArray(),one("note:a","alpha's own words "+t));
            Random r=new Random(300+t);
            for(int i=0;i<4;i++){byte[] x=new byte[Slots.DATA/2];r.nextBytes(x);Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:a"+i,x);try{a.put(m,List.of());}catch(Slots.Full full){break;}}
            drain(s1);s1.close();
            // A second space, never open together with the first, writing and then erasing over its own order.
            Slots s2=slots(f);
            Slots.Space bb=s2.make(("beta phrase "+t).toCharArray(),one("note:b","beta"));
            try{for(int i=0;i<4;i++){byte[] x=new byte[Slots.DATA/2];r.nextBytes(x);Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:b"+i,x);bb.put(m,List.of());}}catch(Slots.Full full){/* it took the file */}
            for(int i=0;i<500&&s2.anyWaiting();i++)s2.batch();
            bb.erase();
            drain(s2);s2.close();
            Slots s3=slots(f);
            Slots.Space back;
            try{back=s3.open(("alpha phrase "+t).toCharArray());}
            catch(Throwable boom){throw new AssertionError("iteration "+t+": opening threw",boom);}
            if(back==null){clobbered++;s3.close();continue;}
            Map<String,byte[]> held;
            try{held=back.take();}
            catch(Throwable boom){throw new AssertionError("iteration "+t+": take threw",boom);}
            byte[] w=held.get("note:a");
            if(w!=null)assertEquals("iteration "+t+": half-garbage for its own words","alpha's own words "+t,new String(w,StandardCharsets.UTF_8));
            if(!back.damaged()){assertNotNull("iteration "+t+": whole but its words are missing",w);recovered++;}else clobbered++;
            s3.close();
        }
        assertTrue("some iterations recovered whole ("+recovered+" of 80)",recovered>0);
        System.out.println("invariant 3: recovered "+recovered+", clobbered "+clobbered+" of 80");
    }

    /**
     * Invariant 4 (decision 116): the only cross-space loss is the deliberate one, a space growing with no room left among
     * the slots it can see taken writing over a CLOSED space's slots. The writer always stays whole; the one sat upon loses
     * itself. Many iterations, varied passwords, a small file so the writer is certain to take the whole of it.
     */
    @Test public void onlyGrowingOverAClosedSpaceLosesItAndTheWriterStaysWhole() throws Exception {
        for(int t=0;t<25;t++) {
            File f=folder.newFile();f.delete();
            Slots s=new Slots(f,mask,52,QUICK);s.start();
            Slots.Space first=s.make(("first phrase "+t).toCharArray(),one("note:1","believable"));
            Random r=new Random(400+t);
            for(int i=0;i<6;i++){byte[] b=new byte[Slots.DATA];r.nextBytes(b);Map<String,byte[]> m=new LinkedHashMap<>();m.put("file:"+i,b);first.put(m,List.of());}
            drain(s);s.close();
            Slots again=new Slots(f,mask,52,QUICK);again.start();
            Slots.Space second=again.make(("second phrase "+t).toCharArray(),one("note:s","the real one"));
            int added=0;
            try{for(;added<200;added++){byte[] b=new byte[Slots.DATA*2];r.nextBytes(b);Map<String,byte[]> m=new LinkedHashMap<>();m.put("big:"+added,b);second.put(m,List.of());drain(again);}fail("never full");}
            catch(Slots.Full full){/* it has taken the whole file */}
            assertTrue("iteration "+t+": the second grew",added>3);
            drain(again);again.close();
            Slots last=new Slots(f,mask,52,QUICK);last.start();
            Map<String,byte[]> held=last.open(("second phrase "+t).toCharArray()).take();
            assertEquals("iteration "+t+": the writer is whole","the real one",new String(held.get("note:s"),StandardCharsets.UTF_8));
            for(int i=0;i<added;i++)assertEquals("iteration "+t,Slots.DATA*2,held.get("big:"+i).length);
            last.close();
            Slots check=new Slots(f,mask,52,QUICK);check.start();
            Slots.Space firstAgain=check.open(("first phrase "+t).toCharArray());
            assertTrue("iteration "+t+": the first was written over",firstAgain==null||firstAgain.damaged()||firstAgain.take().size()<6);
            check.close();
        }
    }

    @Test public void theKeyTakesAboutHalfASecondHere() throws Exception {
        long from=System.nanoTime();
        Vault.pbkdf2("a long phrase of several words".toCharArray(),new byte[32],Slots.ROUNDS);
        long ms=(System.nanoTime()-from)/1_000_000;
        System.out.println("PBKDF2 "+Slots.ROUNDS+" rounds: "+ms+" ms");
        assertTrue(ms<20_000);
    }
}
