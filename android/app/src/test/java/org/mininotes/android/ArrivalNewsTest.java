package org.mininotes.android;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

/**
 * The one notification of what arrived (the owner, 2026-10-22: one grouped notification with its count, a sound only for the
 * first arrival after a quiet spell, in the person's language, cleared when the app is opened; decision 236).
 */
public class ArrivalNewsTest {
    private static final long MINUTE=60_000L;

    @After public void english(){Say.use("en");}

    @Test public void oneThingIsSaidAsASentenceUnderWhoItIsFrom() {
        ArrivalNews news=new ArrivalNews();
        assertTrue(news.isEmpty());
        assertTrue("the first since the app was opened sounds",news.arrived(ArrivalNews.Kind.UPDATED,"Ana","note-1",1,"Ana updated a note.",0));
        assertEquals("Ana",news.title());
        assertEquals("Ana updated a note.",news.text());
        assertEquals(1,news.total());
        assertEquals("tapped, it opens that note","note-1",news.onlyNote());
        assertFalse(news.onlyFiles());
    }

    @Test public void aNoteWrittenInTwentyTimesIsOneNote() {
        ArrivalNews news=new ArrivalNews();
        for(int i=0;i<20;i++)news.arrived(ArrivalNews.Kind.UPDATED,"Ana","note-1",1,"",i*5_000L);
        assertEquals(1,news.total());
        assertEquals("Ana updated a note.",news.text());
        // Shared and then written in: still the note shared with me.
        ArrivalNews shared=new ArrivalNews();
        shared.arrived(ArrivalNews.Kind.SHARED,"Ben","note-2",1,"",0);
        shared.arrived(ArrivalNews.Kind.UPDATED,"Ben","note-2",1,"",1_000);
        assertEquals("Ben shared a note with you.",shared.text());
    }

    @Test public void severalAreCountedByKindAndOnlyTheFirstAfterAQuietSpellSounds() {
        ArrivalNews news=new ArrivalNews();
        assertTrue(news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n1",1,"",0));
        assertFalse(news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n2",1,"",MINUTE));
        assertFalse(news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n3",1,"",9*MINUTE));
        assertFalse(news.arrived(ArrivalNews.Kind.FILES,"Ana",null,1,"",15*MINUTE));
        assertEquals("Ana",news.title());
        assertEquals("3 notes updated, 1 file",news.text());
        assertEquals(4,news.total());
        assertEquals("",news.onlyNote());
        // Ten minutes with nothing: heard again.
        assertTrue(news.arrived(ArrivalNews.Kind.FILES,"Ana",null,3,"",25*MINUTE));
        assertEquals("3 notes updated, 4 files",news.text());
        assertEquals(7,news.total());
    }

    @Test public void fromSeveralPeopleTheTitleIsTheAppsAndTheLineSaysWho() {
        ArrivalNews news=new ArrivalNews();
        news.arrived(ArrivalNews.Kind.SHARED,"Ana","n1",1,"",0);
        news.arrived(ArrivalNews.Kind.UPDATED,"Ben","n2",1,"",1);
        news.arrived(ArrivalNews.Kind.WAITING,"Ana","folder-1",1,"Ana shared “Trip” with you",2);
        assertEquals("New in Mininotes",news.title());
        assertEquals("1 note shared with you, 1 note updated, 1 thing to accept, from Ana, Ben",news.text());
        assertEquals("Ana, Ben, Cy +2",ArrivalNews.names(List.of("Ana","Ben","Cy","Dee","Ed")));
    }

    @Test public void somethingSharedForTheFirstTimeIsSaidByItsOwnLineAlone() {
        ArrivalNews news=new ArrivalNews();
        news.arrived(ArrivalNews.Kind.WAITING,"Ana","folder-1",1,"Ana shared “Trip” with you",0);
        assertEquals("Ana shared “Trip” with you",news.text());
        assertEquals("a thing to accept is not a note to open","",news.onlyNote());
    }

    @Test public void filesAloneOpenReceivedFilesAndCountAsFiles() {
        ArrivalNews news=new ArrivalNews();
        news.arrived(ArrivalNews.Kind.FILES,"Ana",null,2,"",0);
        assertTrue(news.onlyFiles());
        assertEquals("2 files",news.text());
        assertEquals(2,news.total());
        news.arrived(ArrivalNews.Kind.FILES,"",null,1,"",1);
        assertEquals("Ana",news.title());
        assertEquals("3 files",news.text());
    }

    @Test public void openingTheAppClearsItAndTheNextArrivalSoundsAgain() {
        ArrivalNews news=new ArrivalNews();
        news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n1",1,"",0);
        assertFalse(news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n2",1,"",MINUTE));
        news.clear();
        assertTrue(news.isEmpty());
        assertEquals(0,news.total());
        assertEquals("",news.text());
        assertTrue(news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n3",1,"",2*MINUTE));
    }

    @Test public void saidInThePersonsLanguage() {
        Say.use("fr",Map.of("New in Mininotes","Nouveau dans Mininotes","{0} notes updated","{0} notes modifiées",
            "1 file","1 fichier","{0}, from {1}","{0}, de {1}","{0} updated a note.","{0} a modifié une note."));
        ArrivalNews news=new ArrivalNews();
        news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n1",1,"",0);
        assertEquals("Ana a modifié une note.",news.text());
        news.arrived(ArrivalNews.Kind.UPDATED,"Ben","n2",1,"",1);
        news.arrived(ArrivalNews.Kind.FILES,"Ben",null,1,"",2);
        assertEquals("Nouveau dans Mininotes",news.title());
        assertEquals("2 notes modifiées, 1 fichier, de Ana, Ben",news.text());
    }

    @Test public void neverAWordOfANote() {
        ArrivalNews news=new ArrivalNews();
        // Whatever line comes with a note, the line said is built here, from who and what kind of thing.
        news.arrived(ArrivalNews.Kind.UPDATED,"Ana","n1",1,"Buy the ring, she must not know",0);
        assertFalse(news.text().contains("ring"));
        news.arrived(ArrivalNews.Kind.SHARED,"Ana","n2",1,"Buy the ring, she must not know",1);
        assertFalse(news.text().contains("ring"));
    }
}
