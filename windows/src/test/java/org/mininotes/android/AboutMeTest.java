package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

/** Who a device belongs to (decision 163): the card there and back, refused when broken; and one round per person. Made up. */
public class AboutMeTest {
    @Rule public org.junit.rules.TemporaryFolder temp=new org.junit.rules.TemporaryFolder();

    @Test public void thereAndBackAndBrokenIsNothing() throws Exception {
        String id=Persons.newId(new java.security.SecureRandom());
        Keys laptop=new Keys(new org.mininotes.desktop.platform.content.Context(temp.newFolder("laptop")));
        String key=Groups.kept(Point.shorten(laptop.signing().getPublic()));
        byte[] card=AboutMe.wrap(new AboutMe.Card(key,id,1_700_000_000_000L));
        assertTrue(AboutMe.isCard(card));
        AboutMe.Card back=AboutMe.open(card);
        assertNotNull(back);assertEquals(key,back.key);assertEquals(id,back.person);assertEquals(1_700_000_000_000L,back.made);
        for(int cut=AboutMe.MAGIC.length;cut<card.length;cut++)assertNull("cut at "+cut,AboutMe.open(Arrays.copyOf(card,cut)));
        assertNull(AboutMe.open(Arrays.copyOf(card,card.length+1)));
        try{AboutMe.wrap(new AboutMe.Card(key,"not an id",1));fail("not an id");}catch(IllegalArgumentException refused){/* said */}
        assertFalse(AboutMe.isCard(new byte[]{'M','N','Y','1'}));
    }

    @Test public void oneRoundPerPersonTheNeediestMarkAndHowManyDevices() {
        String ana=Persons.newId(new java.security.SecureRandom());
        List<SyncStatus.Person> who=List.of(
            new SyncStatus.Person("Mx-ana-phone","Ana",SyncMark.GONE,true,"",null,false,"Has this version."),
            new SyncStatus.Person("Mx-ben","Ben",SyncMark.GONE,true,"",null,false,"Has this version."),
            new SyncStatus.Person("Mx-ana-laptop","Ana",SyncMark.SENT,true,"",null,false,"Sent. Waiting."),
            new SyncStatus.Person("Mx-my-phone","Pro",SyncMark.GONE,true,"",null,false,"Has this version."),
            new SyncStatus.Person("Mx-my-pc","PC",SyncMark.GONE,true,"",null,false,"Has this version."));
        Map<String,String> person=Map.of("Mx-ana-phone",ana,"Mx-ana-laptop",ana,"Mx-my-phone","mine","Mx-my-pc","mine");
        List<SyncStatus.Person> folded=SyncStatus.folded(who,a->person.getOrDefault(a,""));
        assertEquals(3,folded.size());
        assertEquals("Mx-ana-phone",folded.get(0).address());assertEquals(SyncMark.SENT,folded.get(0).mark());
        assertTrue(folded.get(0).standing(),folded.get(0).standing().endsWith("2 devices"));
        assertEquals("Ben",folded.get(1).name());
        assertEquals(Groups.MY_DEVICES,folded.get(2).name());
    }
}
