package org.mininotes.android;

import java.awt.event.*;
import java.util.Locale;
import javax.swing.text.JTextComponent;

/**
 * Windows' input composition, only where the keyboard needs it (decision 288). With "Show text suggestions when typing on the
 * physical keyboard" on, as on the owner's laptop, Windows composes every word: its letters are drawn in a window over the
 * page at the cursor and handed to the page only once the word is done, which then moves the rest of the line along (the
 * owner, 2026-10-29: "it first writes over and then moves the text"). A keyboard that writes letters as they are typed needs
 * none of it, so a field takes composition only while the keyboard in use is one that composes: Chinese, Japanese, Korean.
 * Looked at again whenever the field gets the focus and before each key, since the keyboard can be changed at any time.
 */
final class DesktopInput {
    private DesktopInput(){}

    /** Whether a keyboard of this language writes by composing what is typed into characters. */
    static boolean composes(Locale keyboard) {
        if(keyboard==null)return false;
        String language=keyboard.getLanguage();
        return "zh".equals(language)||"ja".equals(language)||"ko".equals(language);
    }

    /** The field takes composition only while the keyboard in use composes. */
    static void attach(JTextComponent field) {
        Runnable choose=()->{
            java.awt.im.InputContext context=field.getInputContext();
            boolean wanted=composes(context==null?null:context.getLocale());
            if(Boolean.valueOf(wanted).equals(field.getClientProperty(DesktopInput.class)))return;
            field.enableInputMethods(wanted);field.putClientProperty(DesktopInput.class,wanted);
        };
        field.addFocusListener(new FocusAdapter(){@Override public void focusGained(FocusEvent e){choose.run();}});
        field.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){choose.run();}});
        choose.run();
    }
}
