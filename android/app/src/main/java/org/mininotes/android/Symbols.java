package org.mininotes.android;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One symbol for each thing the app does, the same on the phone and the PC (the owner, 2026-10-11: "minimize the use of text
 * in our menus and use symbols, since we will soon come with the app in multiple languages"; decision 157). A menu row
 * says what it does in a word or two and wears its symbol before them; the symbol is found from the words, so every menu
 * gets it without each row naming one, and a row whose words have none has none. Lucide glyph names, as icons.txt has
 * them. Plain Java: both apps read it.
 */
final class Symbols {
    private Symbols(){}

    /** Words a row begins with, lower case, and its glyph: the first that fits wins, so the longer go first. */
    private static final Map<String,String> BY_START=new LinkedHashMap<>();
    static {
        put("remove from recent","clock-fading");
        // The dock on its own (decision 190), and the Agent (decisions 191, 192).
        put("add to the dock","panel-bottom-open");
        put("remove from the dock","panel-bottom-close");
        put("agent","bot-message-square");
        // A note's files saved together into a folder (decision 196).
        put("save to a folder","folder-down");
        // On Minima through Integritas (decision 199).
        put("stamp on minima","stamp");
        put("verify on minima","file-badge");
        put("compare with a fingerprint","fingerprint-pattern");
        put("stamped on minima","receipt-text");
        // Uninstalling, from the app after offering a backup (decision 235).
        put("uninstall mininotes","package-x");
        // The + of Stamped on Minima (decision 230): something in the notebook, or a file from the drive or the phone.
        put("a note or file in mininotes","notebook-text");put("a file on this","hard-drive-upload");
        // A thing's own stamp, as its line says it, and an attachment taken into another note (decision 224).
        put("stamping","receipt-text");
        put("stamped","receipt-text");
        put("move to another note","notebook-pen");
        put("integritas account","shield-check");
        // A folder of the PC kept in step (decision 197).
        put("sync a folder","folder-sync");
        put("remove from favourites","bookmark-x");
        put("add to favourites","bookmark-plus");
        put("hide from home","eye-off");
        put("show on home","eye");
        put("also show on home","eye");
        put("new note","file-plus");
        put("new folder","folder-plus");
        put("new folder in","folder-tree");
        put("new group","user-round-plus");
        put("new tab","plus");
        put("add a tab","plus");
        put("from another device","monitor-smartphone");
        put("connect","qr-code");
        put("open the bin","trash");
        put("empty the bin","eraser");
        put("move to bin","trash");
        put("move to the bin","trash");
        put("move to","folder-input");
        put("attach","paperclip");
        put("add a file","paperclip");
        put("attachments","files");
        put("record","mic");
        put("take a photo","camera");
        // A folder shown as an album, and pictures added to it (decision 217).
        put("show as an album","images");
        put("add photos","image-plus");
        put("text size","type");
        put("note","file-plus");
        put("folder","folder-plus");
        put("move","folder-input");
        // A tab moved along its strip (decision 239, 2026-10-24).
        put("move left","arrow-left");
        put("move right","arrow-right");
        put("rename","pencil");
        put("delete","circle-x");
        put("forget","unlink");
        put("remove this person","user-minus");
        put("keep it here","check");
        put("remove","minus");
        put("put back","rotate-ccw");
        put("restore","rotate-ccw");
        put("archive","archive");
        put("temporary","alarm-clock");
        put("temp","timer");
        put("things stay","calendar-clock");
        put("send what is in temp","radio-tower");
        put("who it goes to","user-round-cog");
        put("sync","refresh-cw");
        // Share with people, and the phone's own share for another app with the mark everybody knows (the owner, 2026-10-18:
        // "only the icon should be the share we are used to, not the same as Send to a device"; decision 207).
        put("share","user-plus");
        put("sharing","user-plus");
        put("send with another app","share-2");
        put("send to another app","share-2");
        put("who has it","user-round-check");
        put("send","send");
        put("colour","palette");
        put("strength","sliders-horizontal");
        put("icon","sticker");
        put("picture","image-plus");
        put("choose a picture","image-plus");
        put("remove the picture","image-minus");
        put("writing lines","rows-3");
        put("lines","rows-3");
        put("reading","glasses");
        put("writing colour","brush");
        put("show who wrote","pen-line");
        put("versions","clock-arrow-down");
        put("history","clock-arrow-down");
        put("undo","undo-2");
        put("copy","copy");
        // README in Show on Home wears the icon the note itself wears (decision 214).
        put("readme",NoteStore.READ_ME_ICON);
        put("read me",NoteStore.READ_ME_ICON);
        put("choose several","list-checks");
        put("download","download");
        put("save a copy","save");
        put("save on my","hard-drive-download");
        put("open with","square-arrow-out-up-right");
        put("open","arrow-up-right");
        put("their page","user");
        put("my profile","user");
        put("profile","user");
        put("people","users");
        put("my device","smartphone");
        put("chat on parlons","message-circle");
        put("parlons","message-circle");
        put("help","circle-question-mark");
        put("ask for help","bell");
        put("search","search");
        put("all pages","layout-grid");
        put("back to the main page","house");
        put("settings","settings");
        put("lock","lock");
        put("unlock","lock-open");
        put("private","shield");
        put("pause","pause");
        put("resume","play");
        put("accept","check");
        put("refuse","thumbs-down");
        put("block","user-x");
        put("see sender","user-round-search");
        put("exit","log-out");
        put("about","info");
        put("translate","book-a");
        put("version","badge-check");
        put("pin","pin");
        put("unpin","pin-off");
        put("close","x");
        put("waiting","hourglass");
        put("language","languages");
        put("the phone's language","languages");
        put("this pc's language","languages");
        // The rest of every menu (the owner, 2026-10-12: "icons everywhere in the menus"; Tree, the backups, Feedback,
        // Sent files and the others a list of every menu's words showed had none).
        put("tree","list-tree");
        put("export backup","database-backup");
        put("add from backup","archive-restore");
        put("recovery writing","key-round");
        put("feedback","message-square-text");
        put("sent files","clock-arrow-up");
        put("send files","file-up");
        put("update to","circle-arrow-down");
        put("check for updates","circle-arrow-down");
        put("recent","clock");
        put("bin","trash");
        put("favourites","star");
        put("shared with me","inbox");
        put("dock","panel-bottom");
        put("change password","key");
        put("cut","scissors");
        put("paste their link","link-2");
        put("paste","clipboard-paste");
        put("select all","text-cursor-input");
        put("from this device","hard-drive-upload");
        put("from ","user-round");
        put("put in a note","notebook-pen");
        put("replace with","replace");
        put("scan","scan-line");
        put("show my code","qr-code");
        put("show the address","at-sign");
        put("copy my link","link");
        put("this note","file-text");
        put("read only","book-open");
        put("can write","square-pen");
        put("admin","shield-check");
        put("why","message-circle-question-mark");
        put("take a photo","camera");
        put("share mininotes","gift");
        // The other app, each from the other (decision 241).
        put("mininotes on your phone","smartphone");
        put("mininotes on your pc","monitor");
        put("people and devices","contact");
        // Notes kept as files too (docs/FILES.md, decision 162): one symbol for each doing.
        put("keep as a file","file-down");
        put("keep as a folder on disk","folder-down");
        put("open a markdown file","file-input");
        // A file only opened and worked in, never kept (docs/FILES.md): a file with a pen.
        put("edit a file","file-pen");
        put("open to look","eye");
        put("open to edit","square-pen");
        put("open with","app-window");
        put("keep a windows folder in sync","folder-sync");
        put("share a file through mininotes","file-symlink");
        put("stop syncing","link-2-off");
        put("synced with the file","link");
        put("sync parameters","sliders-horizontal");
        put("remove from mininotes","file-minus");
        put("which version to keep","files");
        put("show in explorer","folder-open");
        put("find it","file-search");
        put("keep it inside only","link-2-off");
        // The menu pinned open on the right of Recent (the owner, 2026-10-13).
        put("menu","panel-right");
    }
    private static void put(String start,String glyph){BY_START.put(start,glyph);}

    /** The glyph for a row that says these words; null where none fits, and the row is words alone. */
    static String of(String words) {
        if(words==null)return null;
        String said=words.stripLeading().toLowerCase(Locale.ROOT);
        String best=null;int longest=0;
        for(Map.Entry<String,String> one:BY_START.entrySet())
            if(said.startsWith(one.getKey())&&one.getKey().length()>longest){best=one.getValue();longest=one.getKey().length();}
        return best;
    }
}
