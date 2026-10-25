package org.mininotes.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * When the language box comes up, and what it lists, on the phone and on the PC alike (the owner, 2026-10-24: "When
 * launching for the first time, or until the language hasn't been confirmed, a pop up window should ask the user to pick
 * his language"; decision 240). Until then only a new install was asked (decision 232); now every start asks until a
 * language has been confirmed once, in the box or in Settings, installs already in use included. Android-free, so both
 * apps and a plain test share it.
 */
final class LanguageAsk {
    private LanguageAsk(){}
    /** The setting (phone) and the file beside the notebook (PC) saying a language was confirmed once. */
    static final String CONFIRMED="languageConfirmed",CONFIRMED_FILE="language-confirmed";

    /** Never; first, before the notebook, so README is written in it (a new install); or once the app is open (one in use). */
    enum When{NEVER,BEFORE_NOTEBOOK,ONCE_OPEN}

    /** Confirmed once, or the demo build with its made-up notebook: never again. Otherwise a new install first, one in use once open. */
    static When when(boolean confirmed,boolean inUse,boolean demo) {
        if(confirmed||demo)return When.NEVER;
        return inUse?When.ONCE_OPEN:When.BEFORE_NOTEBOOK;
    }

    /**
     * The languages as the box lists them: in the alphabetical order of their own names, as they are shown (the owner,
     * 2026-10-25: "when we display the languages window first, put the message in english and order the language
     * alphabetically"; decision 254), the one in use ticked where it falls, not moved first.
     */
    static List<String> order(String inUse,Collection<String> codes) {
        List<String> listed=new ArrayList<>(codes);
        java.text.Collator alphabet=java.text.Collator.getInstance(java.util.Locale.ROOT);
        listed.sort((a,b)->alphabet.compare(Say.LANGUAGES.getOrDefault(a,a),Say.LANGUAGES.getOrDefault(b,b)));
        return listed;
    }

    /**
     * The box made with the app's words in English (decision 254): whoever opens it first reads English, the language most
     * people read, whatever the device is set to; each language keeps its own name. The language in use comes back after.
     */
    static <T> T inEnglish(java.util.function.Supplier<T> making) {
        String was=Say.language();
        Say.use("en");
        try{return making.get();}finally{Say.use(was);}
    }

    /**
     * What is kept for the language picked: the one in use, picked again, keeps the setting as it was (so "the device's
     * language" goes on following the device); another is kept as itself.
     */
    static String kept(String picked,String setting,String inUse) {
        return picked.equals(inUse)&&setting!=null&&!setting.isEmpty()?setting:picked;
    }
}
