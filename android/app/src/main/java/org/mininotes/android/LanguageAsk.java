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

    /** The languages as the box lists them: the one in use first (and chosen), then the others in their usual order. */
    static List<String> order(String inUse,Collection<String> codes) {
        List<String> listed=new ArrayList<>();
        if(codes.contains(inUse))listed.add(inUse);
        for(String code:codes)if(!code.equals(inUse))listed.add(code);
        return listed;
    }

    /**
     * What is kept for the language picked: the one in use, picked again, keeps the setting as it was (so "the device's
     * language" goes on following the device); another is kept as itself.
     */
    static String kept(String picked,String setting,String inUse) {
        return picked.equals(inUse)&&setting!=null&&!setting.isEmpty()?setting:picked;
    }
}
