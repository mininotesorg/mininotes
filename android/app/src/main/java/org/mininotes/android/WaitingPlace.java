package org.mininotes.android;

/**
 * The place a note made from a place's + waits to be put in - Temp, the archive, the bin, Favourites - kept past the page it
 * was made on (the owner, 2026-10-24: "Again, adding a note to temp, without title saved it on the desktop not in temp";
 * decision 238).
 *
 * <p>A note made from a place's + is not in the notebook before its first written word, and is put in the place then
 * (decision 87). Where it was to go lived only in the screen's memory: the screen made again before that word -
 * the phone turned, dark mode coming on, the notebook locked while away, Android closing the app behind the camera or
 * another app - came back on a blank page with a new name and nothing waiting for it, and the words typed there
 * went on Home. Now the note's name and its place are kept with the screen and with where the app was left, said as one
 * line, and the blank page that comes back is the same note, still on its way to the place.
 */
final class WaitingPlace {
    private WaitingPlace(){}

    /** The note and its place as one line to keep; null where nothing waits. */
    static String say(String note,String place) {
        if(note==null||note.isEmpty()||place==null||place.isEmpty()||!takes(place))return null;
        return note+"\t"+place;
    }

    /** The place kept for this note, or null: kept for another note, or for none, or by a build that wrote it otherwise. */
    static String forNote(String said,String note) {
        if(said==null||note==null||note.isEmpty())return null;
        int tab=said.indexOf('\t');
        if(tab<=0||tab!=said.lastIndexOf('\t')||!said.substring(0,tab).equals(note))return null;
        String place=said.substring(tab+1);
        return takes(place)?place:null;
    }
    /** The places a + makes a note on Home for and then puts it in, as MainActivity.takesNew says them. */
    private static boolean takes(String place){return NoteStore.TEMP.equals(place)||NoteStore.ARCHIVE.equals(place)||NoteStore.BIN.equals(place)||NoteStore.FAVOURITES.equals(place);}
}
