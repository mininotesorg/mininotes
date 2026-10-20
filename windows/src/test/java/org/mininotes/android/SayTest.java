package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

/** The app's words in the language chosen (decision 160): a list read, a phrase looked up whole, English where there is none. */
public class SayTest {
    @After public void english(){Say.use("en");}

    @Test public void frenchIsReadFromTheAppsOwnListAndEnglishIsTheFallback() {
        Say.use("fr");
        assertEquals("fr",Say.language());
        assertEquals("Supprimer",Say.t("Delete"));
        assertEquals("Renommer…",Say.t("Rename…"));
        assertEquals("a phrase with no translation stays English","Some made-up words",Say.t("Some made-up words"));
        assertEquals("a note's own title is left as it is","Groceries for Sunday",Say.t("Groceries for Sunday"));
        Say.use("en");
        assertEquals("Delete",Say.t("Delete"));
    }

    @Test public void theDotsOfABoxAreKeptWhereOnlyTheBareWordsAreListed() {
        Say.use("fr",Map.of("Colour","Couleur"));
        assertEquals("Couleur…",Say.t("Colour…"));
    }

    /** A button's arrow after its words is kept, and the words said (decision 186). */
    @Test public void theArrowOfAButtonIsKept() {
        Say.use("fr",Map.of("Not set","Non défini","The name they share","Le nom qu’ils partagent"));
        assertEquals("Non défini  ›",Say.t("Not set  ›"));
        assertEquals("Le nom qu’ils partagent  ▾",Say.t("The name they share  ▾"));
        assertEquals("nothing known: as it was","Something  ›",Say.t("Something  ›"));
    }

    @Test public void templatesTakeWhatGoesInThem() {
        Say.use("fr",Map.of("Remove {0} from {1}","Retirer {0} de {1}"));
        assertEquals("Retirer Ana de Amis",Say.t("Remove {0} from {1}","Ana","Amis"));
    }

    /** A sentence built around a name, as the sources build it, said in French with the name kept (decision 160). */
    @Test public void aSentenceBuiltAroundANameFindsItsTemplate() {
        Say.use("fr");
        String who="Ana",what="Courses";
        assertEquals("Retirer Ana de Courses ?",Say.t("Take "+who+" off "+what+"?"));
        assertEquals("Supprimer « Liste » ?",Say.t("Delete “"+"Liste"+"”?"));
        assertEquals("Envoyé à Ben",Say.t("Sent to "+"Ben"));
        assertEquals("nothing fits: English","Something else entirely",Say.t("Something else entirely"));
    }

    @Test public void theSettingOrTheDevicesLanguageWhereThereIsAList() {
        assertEquals("fr",Say.resolve(Say.SYSTEM,Locale.FRANCE));
        assertEquals("en",Say.resolve(Say.SYSTEM,Locale.KOREA));
        assertEquals("zh",Say.resolve(Say.SYSTEM,Locale.CHINA));
        assertEquals("en",Say.resolve("en",Locale.FRANCE));
        assertEquals("fr",Say.resolve("fr",Locale.US));
    }

    @Test public void everyListIsAFlatListOfWordsWithNothingElse() throws Exception {
        for(String code:Say.LANGUAGES.keySet()) {
            try(var in=Say.class.getResourceAsStream("/org/mininotes/android/words/"+code+".json")) {
                assertNotNull("the list for "+code,in);
                Map<String,String> list=Say.read(in);
                assertTrue(code+" has words",list.size()>500);
                // A translation keeps the box's dots and the arrow where its English has them.
                for(Map.Entry<String,String> one:list.entrySet())if(!one.getValue().isEmpty()&&one.getKey().endsWith("…"))
                    assertTrue(code+": "+one.getKey()+" -> "+one.getValue(),one.getValue().endsWith("…"));
            }
        }
    }
}
