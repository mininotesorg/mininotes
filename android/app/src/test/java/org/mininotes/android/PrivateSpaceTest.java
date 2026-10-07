// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Private notes and folders (decision 111): what one password opens, and the codes that open it. */
public class PrivateSpaceTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    private final byte[] mask=new byte[32];{new Random(11).nextBytes(mask);}

    private Slots slots(File file) throws Exception {Slots s=new Slots(file,mask,120,1000);s.start();return s;}
    private static void drain(Slots s) throws Exception {for(int i=0;i<300&&s.anyWaiting();i++)s.batch();}

    @Test public void theCodesAreFoundTheMomentTheirColonIsTyped() {
        Object[] found=PrivateCode.ending("Shopping privatespace//:",24);
        assertEquals(PrivateCode.Kind.SPACE,found[0]);assertEquals(9,found[1]);
        assertEquals(PrivateCode.Kind.SPACE,PrivateCode.ending("Privatespace//:",15)[0]);
        // A phone's keyboard puts a space after "private", and a capital at the start of a line.
        assertEquals(PrivateCode.Kind.SPACE,PrivateCode.ending("Private space//:",16)[0]);
        assertEquals(PrivateCode.Kind.OPEN,PrivateCode.ending("x private//:",12)[0]);
        assertNull(PrivateCode.ending("privatespace//:x",16));
        assertNull(PrivateCode.ending("private:",8));
        assertNull(PrivateCode.ending("privatespace//",14));
    }

    @Test public void aCodeBeingTypedHoldsTheWritingDownButAWordPeopleWriteDoesNot() {
        assertTrue(PrivateCode.typing("privates",8));
        assertTrue(PrivateCode.typing("a privatespace/",15));
        assertTrue(PrivateCode.typing("Private space",13));
        assertTrue(PrivateCode.typing("private//",9));
        assertFalse(PrivateCode.typing("private",7));
        assertFalse(PrivateCode.typing("private ",8));
        assertFalse(PrivateCode.typing("my private spaces",17));
        assertFalse(PrivateCode.typing("privately",9));
        // A save that cannot wait takes out only what no word has: the slashes after "space", or two.
        assertArrayEquals(new int[]{2,16},PrivateCode.unfinished("a privatespace//",16));
        assertNull(PrivateCode.unfinished("privatespace",12));
        assertNull(PrivateCode.unfinished("/home/private/",14));
        assertArrayEquals(new int[]{0,9},PrivateCode.unfinished("private//",9));
    }

    @Test public void notesFoldersFilesVersionsAndTheBinComeBackWhole() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        // A brand-new space is empty (decision 116); the folder and note are made in it, as the owner would with the +.
        Slots.Space made=s.make("phrase".toCharArray(),PrivateSpace.empty());
        PrivateSpace p=PrivateSpace.of(s.open("phrase".toCharArray()));
        assertSame(made,p.space);
        assertTrue(p.in("").isEmpty());
        PrivateSpace.Thing box=p.newFolder("","Private folder",1);
        assertEquals(1,p.in("").size());assertTrue(p.in("").get(0).folder);assertEquals("Private folder",box.name());
        PrivateSpace.Thing first=p.newNote(box.id,1);
        p.text(first.id,"","Bank\nthe number is 12",2);p.keepVersion(first.id,3);
        p.text(first.id,"","Bank\nthe number is 34",4);
        PrivateSpace.Thing loose=p.newNote("",5);p.text(loose.id,"","Loose",5);
        p.colour(loose.id,3);
        byte[] picture=new byte[5000];new Random(1).nextBytes(picture);
        PrivateSpace.Kept kept=p.addFile(first.id,"scan.jpg","image/jpeg",picture,true,6);
        PrivateSpace.Thing gone=p.newNote("",7);p.text(gone.id,"","Throw away",7);p.bin(gone.id,8);
        assertFalse(p.move(box.id,box.id));
        p.write();drain(s);s.close();
        PrivateSpace back=PrivateSpace.of(slots(f).open("phrase".toCharArray()));
        assertEquals("Bank",back.thing(first.id).name());
        assertEquals("Bank\nthe number is 34",back.thing(first.id).body);
        assertEquals(1,back.versionsOf(first.id).size());
        assertEquals("Bank\nthe number is 12",back.versionsOf(first.id).get(0).body);
        assertEquals(3,back.thing(loose.id).colour);
        assertArrayEquals(picture,back.bytesOf(kept.id));
        assertTrue(back.filesOf(first.id).get(0).picture);
        assertEquals(1,back.binned().size());
        // The bin: put back, then gone for good with everything of it.
        back.putBack(gone.id);assertTrue(back.in("").stream().anyMatch(t->t.id.equals(gone.id)));
        back.bin(box.id,9);
        assertTrue(back.in("").stream().noneMatch(t->t.id.equals(box.id)));
        back.emptyBin();
        assertNull(back.thing(first.id));assertNull(back.bytesOf(kept.id));
    }

    @Test public void aHelpWhenOpenedSettingComesBackWholeAndLivesOnlyInTheVault() throws Exception {
        // decision 113: for a private thing the alarm, its recipients and its words live inside the vault, with no trace
        // outside it, and come back whole when it is opened again.
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        s.make("phrase".toCharArray(),PrivateSpace.empty());
        PrivateSpace p=PrivateSpace.of(s.open("phrase".toCharArray()));
        PrivateSpace.Thing note=p.newNote("",1);
        PrivateSpace.Thing other=p.newNote("",2);
        p.help(note.id,true,java.util.List.of("mx-one","mx-two"),"By the river");
        p.write();drain(s);s.close();
        Slots s2=slots(f);
        PrivateSpace back=PrivateSpace.of(s2.open("phrase".toCharArray()));
        PrivateSpace.Thing got=back.thing(note.id);
        assertTrue(got.helpOnOpen);
        assertEquals(java.util.List.of("mx-one","mx-two"),got.helpTo);
        assertEquals("By the river",got.helpMessage);
        // A thing never set to send one stays off, with nobody and no words.
        assertFalse(back.thing(other.id).helpOnOpen);
        assertTrue(back.thing(other.id).helpTo.isEmpty());
        // Turned off, it is off and holds nobody.
        back.help(note.id,false,null,null);back.write();drain(s2);s2.close();
        Slots s3=slots(f);
        PrivateSpace off=PrivateSpace.of(s3.open("phrase".toCharArray()));
        assertFalse(off.thing(note.id).helpOnOpen);
        assertTrue(off.thing(note.id).helpTo.isEmpty());
        s3.close();
    }

    @Test public void whatDoesNotFitIsNotKeptAndEverythingElseIsAsItWas() throws Exception {
        File f=folder.newFile();f.delete();
        Slots s=new Slots(f,mask,60,1000);s.start();
        s.make("p".toCharArray(),PrivateSpace.empty());
        PrivateSpace p=PrivateSpace.of(s.open("p".toCharArray()));
        PrivateSpace.Thing note=p.newNote("",1);
        p.text(note.id,"","Still here",2);p.write();drain(s);
        byte[] huge=new byte[Slots.DATA*60];new Random(2).nextBytes(huge);
        p.addFile(note.id,"big","",huge,false,3);
        try{p.write();fail("it fitted");}catch(Slots.Full full){assertEquals("Private storage is full",full.getMessage());}
        assertTrue(p.filesOf(note.id).isEmpty());
        assertEquals("Still here",p.thing(note.id).body);
        assertEquals(0,p.space.waiting());
    }

    /**
     * The line at the foot says minutes, never bytes (the owner, 2026-10-06: "Why it says 192 KB to go and then MB?";
     * decision 114): about as many minutes as batches still needed, one a minute, less than a minute for the last, then Saved.
     */
    @Test public void theSavingLineSaysMinutesAndThenSavedNeverBytes() throws Exception {
        assertEquals("Saved",PrivateSpace.saying(false,0));
        assertEquals("Saving privately… less than a minute left. Keep Mininotes open.",PrivateSpace.saying(false,1));
        assertEquals("Saving privately… about 4 minutes left. Keep Mininotes open.",PrivateSpace.saying(false,4));
        assertEquals("Private storage is full",PrivateSpace.saying(true,3));
        File f=folder.newFile();f.delete();
        Slots s=slots(f);
        s.make("p".toCharArray(),PrivateSpace.empty());
        PrivateSpace p=PrivateSpace.of(s.open("p".toCharArray()));
        PrivateSpace.Thing note=p.newNote("",1);p.write();drain(s);
        assertEquals("Saved",p.saying(false));
        byte[] photo=new byte[Slots.DATA*40];new Random(3).nextBytes(photo);
        p.addFile(note.id,"scan.jpg","image/jpeg",photo,true,2);p.write();
        int left=p.space.batchesLeft(),batches=0;
        assertTrue("forty slots and more take three batches or more: "+left,left>=3);
        java.util.List<String> said=new java.util.ArrayList<>();
        while(p.space.batchesLeft()>0){said.add(p.saying(false));s.batch();batches++;}
        assertEquals("one batch for each minute it said",left,batches);
        assertEquals("Saving privately… about "+left+" minutes left. Keep Mininotes open.",said.get(0));
        assertEquals("Saving privately… less than a minute left. Keep Mininotes open.",said.get(said.size()-1));
        assertEquals("Saved",p.saying(false));
        for(String one:said)assertFalse(one,one.matches(".*\\d+ ?(bytes|KB|MB).*"));
    }
}
