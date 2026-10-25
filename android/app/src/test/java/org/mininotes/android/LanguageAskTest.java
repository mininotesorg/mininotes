package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/** The language box asked at a start until a language is confirmed once, installs in use included (decision 240). */
public class LanguageAskTest {
    @Test public void askedUntilConfirmedNewFirstAndInUseOnceOpen() {
        assertEquals(LanguageAsk.When.BEFORE_NOTEBOOK,LanguageAsk.when(false,false,false));
        assertEquals(LanguageAsk.When.ONCE_OPEN,LanguageAsk.when(false,true,false));
        assertEquals(LanguageAsk.When.NEVER,LanguageAsk.when(true,false,false));
        assertEquals(LanguageAsk.When.NEVER,LanguageAsk.when(true,true,false));
        // The demo build's made-up notebook is never asked.
        assertEquals(LanguageAsk.When.NEVER,LanguageAsk.when(false,true,true));
        assertEquals(LanguageAsk.When.NEVER,LanguageAsk.when(false,false,true));
    }

    @Test public void theLanguagesAreInTheAlphabeticalOrderOfTheirOwnNames() {
        // Decision 254: Deutsch, English, Français, then 日本語, whichever is in use.
        assertEquals(List.of("de","en","fr","ja"),LanguageAsk.order("fr",List.of("en","de","fr","ja")));
        assertEquals(List.of("de","en","fr","ja"),LanguageAsk.order("ja",List.of("ja","fr","en","de")));
        // One there is no list for is not invented.
        assertEquals(List.of("de","en"),LanguageAsk.order("xx",List.of("en","de")));
        // Every language there is a list for, each once, English before Français.
        List<String> all=LanguageAsk.order("fr",Say.LANGUAGES.keySet());
        assertEquals(Say.LANGUAGES.size(),all.size());
        assertTrue(all.indexOf("en")<all.indexOf("fr"));assertEquals("de",all.get(0));
    }

    @Test public void theBoxIsMadeInEnglishAndTheLanguageComesBack() {
        Say.use("fr");
        try {
            assertEquals("Language",LanguageAsk.inEnglish(()->Say.t("Language")));
            assertEquals("fr",Say.language());
        } finally{Say.use("en");}
    }

    @Test public void theOneInUseKeepsItsSettingAnotherIsItself() {
        // The device's own, followed, picked again: still follows the device.
        assertEquals(Say.SYSTEM,LanguageAsk.kept("en",Say.SYSTEM,"en"));
        assertEquals("fr",LanguageAsk.kept("fr","fr","fr"));
        assertEquals("de",LanguageAsk.kept("de",Say.SYSTEM,"en"));
        assertEquals("ja",LanguageAsk.kept("ja",null,"ja"));
    }
}
