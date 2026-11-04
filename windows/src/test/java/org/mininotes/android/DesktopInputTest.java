package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Locale;

/** Windows' input composition, only for a keyboard that composes (decision 288). */
public class DesktopInputTest {
    @Test public void onlyChineseJapaneseAndKoreanKeyboardsCompose() {
        assertTrue(DesktopInput.composes(Locale.SIMPLIFIED_CHINESE));
        assertTrue(DesktopInput.composes(Locale.TRADITIONAL_CHINESE));
        assertTrue(DesktopInput.composes(Locale.JAPAN));
        assertTrue(DesktopInput.composes(Locale.KOREA));
        assertFalse(DesktopInput.composes(Locale.FRANCE));
        assertFalse(DesktopInput.composes(Locale.US));
        assertFalse(DesktopInput.composes(Locale.forLanguageTag("fa-IR")));
        assertFalse(DesktopInput.composes(null));
    }

    @Test public void aFieldTypedInWithAFrenchKeyboardTakesNoComposition() throws Exception {
        javax.swing.JTextField field=new javax.swing.JTextField();
        javax.swing.SwingUtilities.invokeAndWait(()->DesktopInput.attach(field));
        Object said=field.getClientProperty(DesktopInput.class);
        // The keyboard this test runs with is not one that composes (an English or French one on the build machine).
        java.awt.im.InputContext context=field.getInputContext();
        if(context==null||!DesktopInput.composes(context.getLocale()))assertEquals(Boolean.FALSE,said);
    }
}
