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

    @Test public void theLanguageInUseIsListedFirst() {
        assertEquals(List.of("fr","en","de","ja"),LanguageAsk.order("fr",List.of("en","de","fr","ja")));
        assertEquals(List.of("en","de"),LanguageAsk.order("en",List.of("en","de")));
        // One there is no list for is not invented.
        assertEquals(List.of("en","de"),LanguageAsk.order("xx",List.of("en","de")));
        // Every language there is a list for, each once.
        assertEquals(Say.LANGUAGES.size(),LanguageAsk.order("fr",Say.LANGUAGES.keySet()).size());
        assertEquals("fr",LanguageAsk.order("fr",Say.LANGUAGES.keySet()).get(0));
    }

    @Test public void theOneInUseKeepsItsSettingAnotherIsItself() {
        // The device's own, followed, picked again: still follows the device.
        assertEquals(Say.SYSTEM,LanguageAsk.kept("en",Say.SYSTEM,"en"));
        assertEquals("fr",LanguageAsk.kept("fr","fr","fr"));
        assertEquals("de",LanguageAsk.kept("de",Say.SYSTEM,"en"));
        assertEquals("ja",LanguageAsk.kept("ja",null,"ja"));
    }
}
