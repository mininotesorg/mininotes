package org.mininotes.android;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * The name we pick for a person or a device (the owner, 2026-10-07; docs/HOME.md, decision 124): which name is shown,
 * the card that carries a name to another device of the owner's, and the words People and devices says. No device.
 */
public class CalledTest {
    private static Called.Card card(String picked,boolean show,long decided) throws Exception {
        return new Called.Card(Persons.key(Envelope.keys().getPublic().getEncoded()),picked,show,decided);
    }

    @Test public void theNameWePickIsShownOnlyWhenThatWasChosenAndThereIsOne() {
        assertEquals("Ana",Called.display("Ana","Annie",false));
        assertEquals("Annie",Called.display("Ana","Annie",true));
        assertEquals("a blank one we pick shows theirs","Ana",Called.display("Ana","  ",true));
        assertEquals("Ana",Called.display("Ana","",true));
        assertEquals("",Called.display(null,null,true));
        assertEquals("cut to what a name may be",Persons.NAME_MOST,Called.clip("N".repeat(Persons.NAME_MOST+20)).length());
        assertFalse("the same moment is no news",Called.later(5,5));
        assertFalse(Called.later(4,5));
        assertTrue(Called.later(6,5));
        assertTrue(Called.goesTo(true,true,true,true));
        assertFalse("not chosen",Called.goesTo(false,true,true,true));
        assertFalse("not ours here",Called.goesTo(true,false,true,true));
        assertFalse("their build has not said it reads them",Called.goesTo(true,true,false,true));
        assertFalse("they do not count this device as theirs",Called.goesTo(true,true,true,false));
    }

    @Test public void aCardSaysTheNameAndNothingAfterIt() throws Exception {
        Called.Card one=card("Annie",true,40);
        byte[] whole=Called.wrap(one);
        Called.Card back=Called.open(whole);
        assertNotNull(back);
        assertEquals(one.key,back.key);assertEquals("Annie",back.picked);assertTrue(back.showPicked);assertEquals(40,back.decided);
        assertTrue(Called.isCard(whole));
        assertFalse(Parlons.isCard(whole));
        assertFalse(Called.isCard(Parlons.wrap(new Parlons.Card(one.key,"",1))));
        assertNull("half a card is not a card",Called.open(Arrays.copyOf(whole,whole.length-3)));
        byte[] extra=Arrays.copyOf(whole,whole.length+1);extra[extra.length-1]=1;
        assertNull("nor one with more after it",Called.open(extra));
        assertNull(Called.open(null));assertNull(Called.open(new byte[0]));
        byte[] cleared=Called.wrap(card("  ",true,41));
        Called.Card empty=Called.open(cleared);
        assertNotNull(empty);assertEquals("",empty.picked);
        assertEquals("an empty name is not the one shown","Ana",Called.display("Ana",empty.picked,empty.showPicked));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(Called.MAGIC);out.writeByte(2);out.writeShort(1);out.write('A');out.writeShort(0);out.writeByte(1);out.writeLong(9);
        }
        assertNull("a later format is not this one",Called.open(bytes.toByteArray()));
        assertEquals(31,Receipt.CALLED);
        assertEquals(Receipt.CALLED,Receipt.open(Receipt.wrap(Receipt.CALLED)));
    }

    @Test public void theWordsSayWhereItWasSaved() {
        assertEquals("Saved on this phone",Called.said("this phone",List.of(),List.of(),List.of()));
        assertEquals("Saved, and sent to Test phone",Called.said("this PC",List.of("Test phone"),List.of(),List.of()));
        assertEquals("Saved on this PC. Pro could not be reached now: save again later",
            Called.said("this PC",List.of(),List.of("Pro"),List.of()));
        assertEquals("Saved, and sent to Test phone. Old tablet needs an update of Mininotes first",
            Called.said("this PC",List.of("Test phone"),List.of(),List.of("Old tablet")));
        for(String words:new String[]{Called.SHOW_THEIRS,Called.SHOW_OURS,Called.ALL_MINE,Called.ONLY_HERE,
                Called.said("this phone",List.of("A","B"),List.of("C"),List.of("D"))})
            assertFalse(words,words.contains("\u2014")||words.contains(" - "));
        assertEquals("The name they share",Called.SHOW_THEIRS);
        assertEquals("The name we pick",Called.SHOW_OURS);
        assertEquals("All my devices",Called.ALL_MINE);
        assertEquals("Only this device",Called.ONLY_HERE);
    }
}
