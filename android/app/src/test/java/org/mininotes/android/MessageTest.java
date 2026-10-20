package org.mininotes.android;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class MessageTest {
    private static byte[] id(int n){byte[] id=new byte[16];id[0]=(byte)n;id[15]=(byte)(n*3);return id;}

    @Test public void eachKindGoesThereAndBack() {
        for(Message.Card c:new Message.Card[]{
            new Message.Card(id(1),Message.TEXT,"Are you coming on Sunday?","",1000L),
            new Message.Card(id(2),Message.ASK_PARLONS,"","",2000L),
            new Message.Card(id(3),Message.PARLONS,"Here it is","MxG18HGGSYNTHETIC@127.0.0.1:9001",3000L)}) {
            byte[] wire=Message.wrap(c);
            assertTrue(Message.isCard(wire));
            Message.Card back=Message.open(wire);
            assertNotNull(back);
            assertArrayEquals(c.id,back.id);assertEquals(c.kind,back.kind);assertEquals(c.words,back.words);
            assertEquals(c.parlons,back.parlons);assertEquals(c.at,back.at);
        }
    }

    @Test public void longWordsAreCutAndOnlyAnAnswerCarriesAnAddress() {
        Message.Card back=Message.open(Message.wrap(new Message.Card(id(4),Message.TEXT,"x".repeat(400),"MxNOTSENT@1.2.3.4:9001",5L)));
        assertEquals(Message.MOST_WORDS,back.words.length());assertEquals("",back.parlons);
    }

    @Test public void wrongCardsAreRefused() {
        assertThrows(IllegalArgumentException.class,()->Message.wrap(new Message.Card(new byte[3],Message.TEXT,"hi","",1L)));
        assertThrows(IllegalArgumentException.class,()->Message.wrap(new Message.Card(id(5),Message.TEXT,"  ","",1L)));
        assertThrows(IllegalArgumentException.class,()->Message.wrap(new Message.Card(id(6),Message.PARLONS,"","",1L)));
        assertThrows(IllegalArgumentException.class,()->Message.wrap(new Message.Card(id(7),9,"hi","",1L)));
    }

    @Test public void otherBytesAreNotAMessage() {
        assertFalse(Message.isCard(Help.MAGIC));assertFalse(Message.isCard(Receipt.wrap(Receipt.MESSAGES)));
        assertNull(Message.open("MNM1 plain words of a note".getBytes(StandardCharsets.UTF_8)));
        byte[] wire=Message.wrap(new Message.Card(id(8),Message.TEXT,"hi","",1L));
        byte[] longer=java.util.Arrays.copyOf(wire,wire.length+1);
        assertNull(Message.open(longer));
        byte[] shorter=java.util.Arrays.copyOf(wire,wire.length-1);
        assertNull(Message.open(shorter));
        wire[4]=2;assertNull(Message.open(wire));
    }

    @Test public void idsGoToHexAndBack() {
        byte[] one=Message.newId();
        assertEquals(32,Message.hex(one).length());assertArrayEquals(one,Message.unhex(Message.hex(one)));
        assertNull(Message.unhex("zz"));assertNull(Message.unhex("g".repeat(32)));
    }

    @Test public void kindsWithoutWordsSayWhatTheyAre() {
        assertEquals(Message.ASKS,Message.said(Message.ASK_PARLONS,""));
        assertEquals("Thanks",Message.said(Message.PARLONS,"Thanks"));
        assertEquals(Message.SENDS,Message.said(Message.PARLONS," "));
    }
}
