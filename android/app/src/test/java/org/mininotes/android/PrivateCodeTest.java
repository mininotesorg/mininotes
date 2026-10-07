// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The guard at the door (the owner, 2026-10-06: "We have to make sure that if we type privatenotes or private//: or anything
 * related to the private elements in a shared element, this is not shared"; decision 114), and the catching as a phone's page
 * does it, every way a code can come in: typed a key at a time, handed over as a whole word again at every letter as a
 * phone's keyboard does, pasted in one go, cut by a save that cannot wait, and in a parcel. What is kept, every version and
 * what is sealed never hold a code or its password.
 */
public class PrivateCodeTest {
    private static final String PASSWORD="velvet kettle 4417";
    private static final String[] NEVER={"privatespace//","privatenote//","privatefolder//","private//:","private space//","private note//","private folder//",PASSWORD,"velvet"};

    private static void clean(String what,String text) {
        String low=text.toLowerCase(java.util.Locale.ROOT);
        for(String word:NEVER)assertFalse(what+" holds "+word+": "+text,low.contains(word.toLowerCase(java.util.Locale.ROOT)));
    }

    // ---- the guard -----------------------------------------------------------------------------------------------------

    @Test public void theGuardTakesEveryCodeAndTheRestOfItsLine() {
        assertEquals("Milk\n\nEggs",PrivateCode.scrub("Milk\nprivatespace//:"+PASSWORD+"\nEggs"));
        assertEquals("Milk \nEggs",PrivateCode.scrub("Milk privatespace//:"+PASSWORD+"\nEggs"));
        // The new word, and the two old ones still caught so a code from a build before the rename is taken out (decision 116).
        for(String code:new String[]{"privatespace//:","private space//:","privatenote//:","privatefolder//:","private//:","Private Space//:","PRIVATE FOLDER//:","PrivateNote//:","private //:"})
            assertEquals(code,"Plan ",PrivateCode.scrub("Plan "+code+PASSWORD));
        assertEquals("a\r\nb",PrivateCode.scrub("aprivate//:x\r\nb"));
        assertEquals("two on a line, both gone","x ",PrivateCode.scrub("x privatespace//:a private//:b"));
        // Words people write are left as they are, and the same object comes back.
        for(String kept:new String[]{"my private space","private space","private//","privatespace//","the word privatespace, then //: and your password","https://example.org/a//:b","",null}) {
            assertSame(kept,PrivateCode.scrub(kept));assertFalse(String.valueOf(kept),PrivateCode.holds(kept));
        }
        assertTrue(PrivateCode.holds("go privatespace//:"));
        assertTrue(PrivateCode.holds("go privatefolder//:"));
    }

    @Test public void theCaretStaysWhereItMeans() {
        String text="ab privatespace//:pw\ncd private//:x";
        List<int[]> runs=PrivateCode.runs(text);
        assertEquals(2,runs.size());
        assertEquals(1,PrivateCode.caretAfter(runs,1));
        assertEquals("inside a code: at its start",3,PrivateCode.caretAfter(runs,8));
        assertEquals("after the first: moved back by it",5,PrivateCode.caretAfter(runs,text.indexOf("cd")+1));
        assertEquals(PrivateCode.scrub(text).length(),PrivateCode.caretAfter(runs,text.length()));
    }

    // ---- a phone's page, as PrivateScreen.watch and MainActivity.save treat it ------------------------------------------

    /**
     * A page as the phone's is: a keyboard writes into it (a key committed, or a word it is still writing, handed over whole
     * at every letter), the watcher looks through each short change for a code it finished and takes it out with what came
     * after it, the keyboard's copy of the word dropped; while the password field comes up what is typed is caught for it,
     * and once it is up the keyboard is the field's. A save drops an unfinished code with its slashes, then lets the guard
     * look. Every save is kept, as the note and its versions would be.
     */
    static final class Page {
        final StringBuilder text=new StringBuilder();int caret,composeFrom=-1,composeTo=-1;
        StringBuilder catching;final StringBuilder field=new StringBuilder();boolean asking;
        final List<String> kept=new ArrayList<>();

        Page(String start){text.append(start);caret=start.length();}
        /** A key, or a paste, committed at the caret. */
        void commit(String s){composeFrom=composeTo=-1;put(caret,caret,s);}
        /** The keyboard's word, written again whole: where it has none, it starts one at the caret. */
        void compose(String word) {
            if(asking){field.setLength(0);field.append(word);return;}
            int from=composeFrom<0?caret:composeFrom,to=composeFrom<0?caret:composeTo;
            composeFrom=from;composeTo=from+word.length();
            put(from,to,word);
        }
        private void put(int from,int to,String s) {
            if(asking){field.append(s);return;}
            if(catching!=null){PrivateCode.more(catching,s);return;}
            text.replace(from,to,s);caret=from+s.length();
            if(s.length()==0||s.length()>48)return;
            PrivateCode.Caught found=PrivateCode.finished(text,from,from+s.length());
            if(found==null)return;
            // removeComposingSpans and restartInput: the keyboard forgets its word.
            composeFrom=composeTo=-1;
            text.delete(found.start,found.stop);caret=found.start;
            catching=new StringBuilder(found.rest);if(found.entered)catching.append('\n');
        }
        /** The password field is up: what was caught is in it, and the keyboard is its own. */
        String up() {
            String already=catching.toString();catching=null;
            int enter=already.indexOf('\n');
            if(enter>=0)return already.substring(0,enter);
            asking=true;field.setLength(0);field.append(already);composeFrom=composeTo=-1;
            return null;
        }
        String enter(){asking=false;String said=field.toString();field.setLength(0);return said;}
        /** The writing down that happens by itself waits while the word at the caret could still become a code. */
        void autosave(){if(!PrivateCode.typing(text,caret))save();}
        /** A save that cannot wait. */
        void save() {
            int[] code=PrivateCode.unfinished(text,caret);
            if(code!=null){text.delete(code[0],code[1]);caret=code[0];composeFrom=composeTo=-1;}
            List<int[]> runs=PrivateCode.runs(text);
            if(!runs.isEmpty()){caret=PrivateCode.caretAfter(runs,caret);for(int i=runs.size()-1;i>=0;i--)text.delete(runs.get(i)[0],runs.get(i)[1]);composeFrom=composeTo=-1;}
            kept.add(text.toString());
        }
        void allClean(){for(String one:kept)clean("a save",one);clean("the page",text.toString());}
    }

    @Test public void typedAKeyAtATime() {
        Page page=new Page("Groceries\n");
        for(char c:"privatespace//:".toCharArray()){page.commit(String.valueOf(c));page.autosave();}
        assertNotNull("caught at its colon",page.catching);
        assertEquals("Groceries\n",page.text.toString());
        for(char c:"velvet ".toCharArray())page.commit(String.valueOf(c));   // before the field is up
        assertNull(page.up());
        for(char c:"kettle 4417".toCharArray())page.commit(String.valueOf(c));
        assertEquals(PASSWORD,page.enter());
        page.commit("milk");page.save();
        assertEquals("Groceries\nmilk",page.kept.get(page.kept.size()-1));
        page.allClean();
    }

    @Test public void typedAsWholeWordsTheWayAPhonesKeyboardHandsThemOver() {
        for(boolean wordAgain:new boolean[]{false,true}) {
            Page page=new Page("Plan ");
            String word="privatespace//:";
            for(int i=1;i<=word.length();i++){page.compose(word.substring(0,i));page.autosave();}
            assertNotNull(page.catching);assertEquals("Plan ",page.text.toString());
            // The password's first letters before the field is up: a keyboard restarted, a letter at a time, or one that
            // hands over its whole word again, code and all, at every letter.
            String first="velvet";
            for(int i=1;i<=first.length();i++){if(wordAgain)page.compose(word+first.substring(0,i));else page.commit(first.substring(i-1,i));}
            assertEquals("only the password is caught",first,page.catching.toString());
            page.save();
            assertNull(page.up());page.compose(PASSWORD);
            assertEquals(PASSWORD,page.enter());
            page.allClean();
        }
    }

    @Test public void pastedInOneGo() {
        // Short, Enter and all: caught whole, the password straight to opening.
        Page page=new Page("Plan ");
        page.commit("privatespace//:"+PASSWORD+"\nnext line");
        assertEquals("Plan \nnext line",page.text.toString());
        assertEquals(PASSWORD,page.up());
        page.save();page.allClean();
        // Short, no Enter: the password waits in its field.
        page=new Page("");
        page.commit("private//:"+PASSWORD);
        assertEquals("",page.text.toString());assertNull(page.up());assertEquals(PASSWORD,page.enter());
        page.save();page.allClean();
        // Long, which the watcher leaves to the guard: taken out at the save, with the rest of its line.
        page=new Page("Plan\n");
        page.commit("Here is a long paragraph pasted from somewhere else, and in it privatespace//:"+PASSWORD+" and more\nthe end");
        assertNull(page.catching);
        page.save();
        assertEquals("Plan\nHere is a long paragraph pasted from somewhere else, and in it \nthe end",page.kept.get(0));
        page.allClean();
    }

    @Test public void aSaveThatCannotWaitInTheMiddleOfACode() {
        // A key at a time: the half typed is taken out; what is typed after it is no code any more.
        Page page=new Page("Plan ");
        for(char c:"privatespace//".toCharArray())page.commit(String.valueOf(c));
        page.autosave();assertTrue("the writing down waited",page.kept.isEmpty());
        page.save();
        assertEquals("Plan ",page.kept.get(0));
        page.allClean();
        // The keyboard's word: it writes the code again whole at the next letter, and that is caught.
        page=new Page("Plan ");
        String word="privatespace//:";
        for(int i=1;i<word.length();i++)page.compose(word.substring(0,i));
        page.save();
        assertEquals("Plan ",page.kept.get(0));
        page.compose(word);
        assertNotNull("caught when the keyboard wrote it back",page.catching);
        assertNull(page.up());page.compose(PASSWORD);assertEquals(PASSWORD,page.enter());
        page.save();page.allClean();
    }

    // ---- what is sealed, and what arrives -------------------------------------------------------------------------------

    @Test public void aParcelNeverCarriesACodeAndOneThatArrivesWithOneLeavesItAtTheDoor() throws Exception {
        String body="Meet at noon\nprivatespace//:"+PASSWORD+"\nBring water";
        Parcel.Sent going=new Parcel.Sent("","","","","Plan private//:"+PASSWORD,body,true,Collections.<Parcel.Member>emptyList(),"PAGE","x",false,-1L,false,
            List.of(),1L,List.of(),null,"",null);
        byte[] plain=Parcel.wrap(going);
        String bytes=new String(plain,java.nio.charset.StandardCharsets.ISO_8859_1);
        clean("the sealed parcel",bytes);
        Parcel.Sent back=Parcel.open(plain);
        assertEquals("Meet at noon\n\nBring water",back.body);assertEquals("Plan ",back.title);
        // From a build before the guard, which wrote the code: read without it.
        java.io.ByteArrayOutputStream raw=new java.io.ByteArrayOutputStream();java.io.DataOutputStream out=new java.io.DataOutputStream(raw);
        out.write(Parcel.MAGIC);out.writeBoolean(true);
        for(String one:new String[]{"","","","","privatefolder//:"+PASSWORD,body})Parcel.put(out,one,Parcel.TEXT_MOST);
        Parcel.Sent old=Parcel.open(raw.toByteArray());
        clean("an old parcel's title",old.title);clean("an old parcel's body",old.body);
        assertEquals("Meet at noon\n\nBring water",old.body);
    }
}
