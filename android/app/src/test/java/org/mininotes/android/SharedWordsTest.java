package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;

/**
 * The sentences the shared classes build, said in the language in use (decision 256; the owner, 2026-10-25, a PC set to
 * French paired with a phone: "the error message, Pro could not reach PixelPro is also in english even if I'm set in french
 * on the new laptop"). Each class that builds words for a screen is asked here in French, from the real word lists: a
 * sentence glued together in English, which no list can say, fails. Made-up names only.
 */
public class SharedWordsTest {
    @Before public void french(){Say.use("fr");}
    @After public void english(){Say.use("en");}

    @Test public void couldNotGoIsSaidInFrench() {
        Unsent.Problem far=new Unsent.Problem(Unsent.Why.NOT_REACHED,"PixelPro","Courses","","");
        assertEquals("« Courses » n’a pas pu atteindre PixelPro pour l’instant.",Unsent.said(far,"this PC"));
        assertEquals("« Courses » n’a pas pu atteindre PixelPro",Unsent.title(List.of(far),0,1));
        assertEquals("Ouvrez Mininotes sur PixelPro, sur le même Wi-Fi que ce PC si possible. Il reste en attente et part tout seul.",Unsent.remedy(far,"this PC"));
        assertEquals("PixelPro n’a pas pu être joint pour l’instant.",Unsent.said(new Unsent.Problem(Unsent.Why.NOT_REACHED,"PixelPro","","",""),"this phone"));
        assertEquals("Un appareil n’a pas encore été associé à ce téléphone, donc rien ne peut être scellé pour lui.",
            Unsent.said(new Unsent.Problem(Unsent.Why.NOT_PAIRED,"","","",""),"this phone"));
        assertEquals("L’ajouter",Unsent.button(Unsent.Fix.PAIR));
        List<Unsent.Problem> five=List.of(far,new Unsent.Problem(Unsent.Why.NOT_REACHED,"Tablette","Courses","",""),
            new Unsent.Problem(Unsent.Why.NOT_REACHED,"Liseuse","Courses","",""),new Unsent.Problem(Unsent.Why.NOT_REACHED,"Montre","Courses","",""),
            new Unsent.Problem(Unsent.Why.NOT_REACHED,"Cadre","Courses","",""));
        assertTrue(Unsent.words(five,"this PC").endsWith("\n\n2 autres raisons, de la même façon."));
        // Read back as the amber box reads it, in French as in English.
        assertTrue(Looks.onlyUpdates(Unsent.needsUpdate("Vieux téléphone")+"."));
    }

    @Test public void theNoteLineIsSaidInFrenchAndStillReadBack() {
        assertEquals("Envoi à Ana…",NoteLine.sending(List.of("Ana")));
        assertTrue(NoteLine.isSending(NoteLine.sending(List.of("Ana","Ben","Cy"))));
        assertTrue(NoteLine.isSending("Sending to Ana…"));
        assertFalse(NoteLine.isSending(NoteLine.SAVING));
        assertEquals("Envoyé à Ana et Ben, en attente de leur confirmation à tous",NoteLine.sent(List.of("Ana","Ben")));
        Unsent.Problem far=new Unsent.Problem(Unsent.Why.NOT_REACHED,"PixelPro","Courses","","");
        assertEquals("Impossible de joindre PixelPro. Voir pourquoi",NoteLine.couldNot(List.of(far),0));
    }

    @Test public void whatWaitsIsSaidInFrench() {
        assertEquals("En attente de joindre Tablette avec vos modifications.\nEn attente que Téléphone confirme 3 fichiers.",
            Waits.said(List.of(new Waits.Device("Tablette",new Waits.Where(1,0,0,false,false),0),
                new Waits.Device("Téléphone",new Waits.Where(0,3,9,true,false),0)),1));
        Waits.Where old=new Waits.Where(0,0,0,false,false,false,1);
        String said=Waits.said(List.of(new Waits.Device("Vieux téléphone",old,0),new Waits.Device("",old,0)),1);
        assertEquals("Vieux téléphone doit mettre à jour Mininotes pour recevoir ceci.\nUn appareil associé doit mettre à jour Mininotes pour recevoir ceci.",said);
        assertTrue(Looks.onlyUpdates(said));
        assertFalse(Looks.onlyUpdates(said+"\nEn attente de joindre Tablette avec vos modifications."));
    }

    @Test public void filesAndPicturesAreCountedInFrench() {
        assertEquals("Ajout de 2 fichiers…",Given.adding(2));
        assertEquals("3 fichiers ajoutés, 1 non",Given.added(3,null,1));
        assertEquals("Réduction de 3 images…",Shrink.shrinking(3));
        assertEquals("Réduire 1 image ?",Shrink.asking(1));
        assertEquals("Ana vous a envoyé 3 fichiers",Drop.arrived("Ana",3));
        assertEquals("Le fichier est arrivé chez Ana",Drop.answered("Ana",1,Drop.DELIVERED));
        assertEquals("2 dossiers · 1 note",Things.holds(2,1));
    }

    @Test public void whatOthersShareIsSaidInFrench() {
        assertEquals("Ana et Ben vous a partagé « Courses »",FirstShare.title(List.of("Ana","Ben"),1,"Courses"));
        assertEquals("« Courses » est maintenant sur l’accueil",FirstShare.accepted("Courses","Home"));
        assertEquals("Ana a refusé « Courses »",FirstShare.notice("Ana","Courses",""));
        assertEquals("À L’AIDE de la part de Ana",Help.title("Ana"));
        assertTrue(Help.body("",false,0,0,0,"").endsWith("Aucune position n’a été envoyée."));
        assertEquals("Lié par « Courses »",Linking.through("Courses"));
    }

    /**
     * What the Pro showed in English when a PC set to French offered it something: an offer travels in English, which every
     * build reads, and is said on the device that shows it, in its own language.
     */
    @Test public void anOfferIsSaidInTheLanguageOfTheDeviceShowingIt() {
        assertEquals("le dossier Courses",Sharing.shown(Sharing.travelling(Sharing.Scope.COLLECTION,"Courses")));
        assertEquals("la note Courses",Sharing.shown(Sharing.travelling(Sharing.Scope.PAGE,"Courses")));
        assertEquals("tout leur bloc-notes",Sharing.shown(Sharing.travelling(Sharing.Scope.LIBRARY,"Everything")));
        assertEquals("cet appareil comme l’un des vôtres, rien de partagé pour l’instant",Sharing.shown(Sharing.MINE_OFFER));
        assertEquals("tous les dossiers et notes",Sharing.offering(Sharing.Scope.LIBRARY,""));
        assertEquals("le dossier « Courses »",Sharing.offering(Sharing.Scope.COLLECTION,"Courses"));
        assertEquals("Mes appareils · 2 en attente",Sharing.says(Sharing.State.DEVICES,2,false));
        assertEquals("Partagé avec 3 adresses",Sharing.describe(Sharing.State.OTHERS,3));
        // It travels as it always did, in English: a build from before reads it.
        Say.use("en");
        assertEquals("the collection Courses",Sharing.travelling(Sharing.Scope.COLLECTION,"Courses"));
    }

    @Test public void devicesAndPlacesAreSaidInFrench() {
        assertEquals("2 messages sont gardés pour vos téléphones jusqu’à ce qu’ils les récupèrent.",Home.said(2,0));
        assertEquals("il y a 3 h",Routes.ago(3*3600_000L));
        assertEquals("Connexions directes : ouvertes sur le port 49152 · 2 appareils sur ce Wi-Fi",Routes.doorLine(true,49152,2));
        assertTrue(Recording.limit(16_000).startsWith("Jusqu'à "));
        assertEquals("Enregistrement privé… environ 5 minutes restantes. Gardez Mininotes ouvert.",PrivateSpace.saying(false,5));
        assertEquals("Il ne compte pas ce PC parmi vos appareils. Activez aussi Mon appareil là-bas pour que les noms voyagent.",
            Persons.oneSided(true,true,false,"PC"));
        assertEquals("Enregistré, et envoyé à Pro et Tablette. Vieux n’a pas pu être joint : enregistrez de nouveau plus tard",
            Parlons.said(false,"this PC",List.of("Pro","Tablette"),List.of("Vieux"),List.of()));
        assertEquals("Enregistré sur ce téléphone",Called.said("this phone",List.of(),List.of(),List.of()));
        assertTrue(new SyncStatus.State(0,0,SyncStatus.ONLY_HERE).brief("this phone").startsWith("Seulement sur ce téléphone · enregistré "));
        Say.use("de");
        assertEquals("Seite 3",new Sections.Section(null,0,0,0).called(2));
    }

    /** A count's place takes a number only: a note called "Meeting notes" is its own name, not "Meeting" notes in French. */
    @Test public void aNameIsNeverReadAsACount() {
        assertEquals("Meeting notes",Say.t("Meeting notes"));
        assertEquals("Old files",Say.t("Old files"));
        assertEquals("3 fichiers",Say.t("3 files"));
        assertTrue(Say.fits("Sending to {0}…","Envoi à Ana…"));
        assertFalse(Say.fits("Sending to {0}…","Enregistré"));
    }
}
