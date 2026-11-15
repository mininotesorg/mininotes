// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Backups the owner sets once and that then happen by themselves (decision 329; the owner, 2026-11-05: "we should have a full
 * backup page where the user can set its backup parameters, what is backup, where, when and at what frequency, like daily,
 * weekly, monthly, on demand, when he closes the app. The backup should be encrypted"). Each place a backup goes, a folder
 * here, has its own what, when, how many kept and what opens it; this class holds those rules, when each is due, the names
 * its files are given and which older ones go. Always sealed: by the notebook's lock, a password chosen for the backups, or
 * 12 recovery words made for them. Holds no Android types; the apps write the files (DesktopBackups, PhoneBackups).
 */
final class Backups {
    private Backups(){}

    /** What goes in: everything, or everything but the files (smaller; a restore then brings back the notes only). */
    enum What{EVERYTHING,NO_FILES}
    /** When: only when asked, each day, each week, each month, or each time Mininotes is closed (the phone: left). */
    enum Often{ASKED,DAILY,WEEKLY,MONTHLY,ON_CLOSE}
    /**
     * What opens it: the notebook's own lock (its password or its words), a password chosen for it, 12 words made for it, a key
     * file saved where the owner chooses, or this device alone (step 3 of decision 329). Kept by name, in this order.
     */
    enum Opens{NOTEBOOK,PASSWORD,WORDS,KEY_FILE,DEVICE}

    /** How many of a place's backups are kept, at most and at first; the oldest go once a new one is written whole. */
    static final int KEEP=5,KEEP_MOST=50;
    /** A backup that failed is tried again this long after, not at every beat. */
    static final long AGAIN_AFTER=30L*60*1000;
    /** A backup when closing, at most this often: leaving the phone's app ten times in a minute is one backup. */
    static final long CLOSE_AT_MOST=10L*60*1000;
    /** The secret a place's key is kept under, in the notebook's secrets: locked with it, never in a backup (decision 199). */
    static String secretOf(String id){return "backup:"+id;}

    /** The ending every backup has; the start of the names this page gives, so only its own are ever counted or removed. */
    static final String ENDING=".mnbackup",START="mininotes-backup-";

    /** One place a backup goes, and how. */
    static final class Place {
        String id=UUID.randomUUID().toString();
        /** Where: a folder's path on the PC, the folder's address (a tree URI) on the phone. */
        String where="";
        /** Its name as shown: "D:\Backups", "Downloads", or the device's name. */
        String shown="";
        /**
         * Another of my devices it goes to instead of a folder (decision 330): that device's address, empty for a folder. The
         * backup is pushed to it, sealed; a backup server, or the PC, keeps it and cannot open it.
         */
        String device="";
        boolean toDevice(){return !device.isEmpty();}
        What what=What.EVERYTHING;
        Often often=Often.DAILY;
        /** The hour it is made at (0 to 23), the day of the week (1 Monday to 7) or of the month (1 to 28) where those apply. */
        int hour=20,weekday=1,monthday=1;
        int keep=KEEP;
        Opens opens=Opens.PASSWORD;
        /** The lock that opens it, as Vault keeps one: written at the head of each backup. Empty for the notebook's own. */
        String lock="";
        /** When it was set, last made whole, and last tried; what the last try came to (see {@link Said}). */
        long set=System.currentTimeMillis(),last,tried;
        Said said=Said.NONE;
        /** What the notebook was at the last backup made here: one made when closing is skipped while nothing changed. */
        String fingerprint="";
        /**
         * Whether backups go here by themselves (the owner, 2026-11-07: "we should have a button to turn on and off the backup").
         * Off: none made at its time or on closing, none waiting pushed again; Back up now still makes one. Kept as it was set.
         */
        boolean on=true;

        JSONObject json() {
            try{return made();}catch(org.json.JSONException never){throw new IllegalStateException(never);}
        }
        private JSONObject made() throws org.json.JSONException {
            return new JSONObject().put("id",id).put("where",where).put("shown",shown).put("what",what.name()).put("often",often.name())
                .put("hour",hour).put("weekday",weekday).put("monthday",monthday).put("keep",keep).put("opens",opens.name()).put("lock",lock)
                .put("set",set).put("last",last).put("tried",tried).put("said",said.name()).put("fingerprint",fingerprint).put("device",device).put("on",on);
        }
        static Place read(JSONObject o) {
            Place p=new Place();
            p.id=o.optString("id",p.id);p.where=o.optString("where","");p.shown=o.optString("shown","");
            p.what=pick(What.class,o.optString("what"),What.EVERYTHING);p.often=pick(Often.class,o.optString("often"),Often.DAILY);
            p.hour=clamp(o.optInt("hour",20),0,23);p.weekday=clamp(o.optInt("weekday",1),1,7);p.monthday=clamp(o.optInt("monthday",1),1,28);
            p.keep=clamp(o.optInt("keep",KEEP),1,KEEP_MOST);p.opens=pick(Opens.class,o.optString("opens"),Opens.PASSWORD);p.lock=o.optString("lock","");
            p.set=o.optLong("set",0);p.last=o.optLong("last",0);p.tried=o.optLong("tried",0);p.said=pick(Said.class,o.optString("said"),Said.NONE);
            p.fingerprint=o.optString("fingerprint","");p.device=o.optString("device","");
            // Places kept before the switch existed were all on.
            p.on=o.optBoolean("on",true);
            return p;
        }
        Place copy(){return read(json());}
        /** One read back from {@link #json}'s words. */
        static Place of(String said){try{return read(new JSONObject(said));}catch(org.json.JSONException unreadable){return new Place();}}
    }

    /**
     * What the last try came to: none yet, made, nothing changed since the last one, or why it failed; for a device, that it
     * could not be reached directly, or did not take it.
     */
    enum Said{NONE,MADE,SAME,NO_FOLDER,NO_ROOM,NO_KEY,FAILED,NO_DEVICE,NOT_TAKEN}

    // ---- pushed to a device (decision 330) -----------------------------------------------------------------------

    /** The kind a pushed backup is offered as, with how many of this device's the receiver keeps: "…;keep=5". */
    static final String KIND="application/x-mininotes-backup";
    static String kindKeeping(int keep){return KIND+";keep="+clamp(keep,1,KEEP_MOST);}
    /** How many the sender asks to be kept, from the kind it offered; {@link #KEEP} where it says none. */
    static int keepIn(String kind) {
        if(kind==null)return KEEP;
        int at=kind.indexOf(";keep=");
        if(at<0)return KEEP;
        try{return clamp(Integer.parseInt(kind.substring(at+6).trim()),1,KEEP_MOST);}catch(NumberFormatException none){return KEEP;}
    }
    /**
     * Whether a pushed file's name is a backup's, as {@link #fileName} gives one: the only files a device takes pushed. Its
     * device part is what {@link #safe} makes, so it holds no path, no dot, nothing a folder would read as more than a name.
     */
    static boolean isBackupName(String name) {
        return name!=null&&name.matches(java.util.regex.Pattern.quote(START)+"[A-Za-z0-9-]{1,40}-\\d{8}-\\d{6}"+java.util.regex.Pattern.quote(ENDING));
    }
    /** The device part of a backup's name: "Pixel-7" of "mininotes-backup-Pixel-7-20261105-204000.mnbackup". */
    static String deviceIn(String name){return isBackupName(name)?name.substring(START.length(),name.length()-ENDING.length()-16):"";}

    /** The places as kept in the settings; none for nothing or anything unreadable. */
    static List<Place> read(String kept) {
        List<Place> out=new ArrayList<>();
        if(kept==null||kept.isEmpty())return out;
        try{JSONArray all=new JSONArray(kept);for(int i=0;i<all.length();i++)out.add(Place.read(all.getJSONObject(i)));}
        catch(Exception unreadable){out.clear();}
        return out;
    }
    static String said(List<Place> places){JSONArray all=new JSONArray();for(Place p:places)all.put(p.json());return all.toString();}

    /**
     * When it is next due, after its last backup (or after it was set, for none yet): the hour on the next day, week or month
     * that comes after. -1 for one made only when asked or when closing. One missed while the device was off is due at once.
     */
    static long next(Place p,ZoneId zone) {
        long from=Math.max(p.last,p.set);
        if(p.often==Often.ASKED||p.often==Often.ON_CLOSE)return -1;
        ZonedDateTime after=Instant.ofEpochMilli(from).atZone(zone);
        LocalTime at=LocalTime.of(p.hour,0);
        LocalDate day=after.toLocalDate();
        ZonedDateTime candidate;
        switch(p.often) {
            case DAILY:
                candidate=day.atTime(at).atZone(zone);
                if(!candidate.isAfter(after))candidate=day.plusDays(1).atTime(at).atZone(zone);
                break;
            case WEEKLY: {
                DayOfWeek want=DayOfWeek.of(p.weekday);
                LocalDate d=day;while(d.getDayOfWeek()!=want)d=d.plusDays(1);
                candidate=d.atTime(at).atZone(zone);
                if(!candidate.isAfter(after))candidate=d.plusWeeks(1).atTime(at).atZone(zone);
                break;
            }
            default: {
                LocalDate d=day.withDayOfMonth(p.monthday);
                candidate=d.atTime(at).atZone(zone);
                if(!candidate.isAfter(after))candidate=d.plusMonths(1).atTime(at).atZone(zone);
            }
        }
        return candidate.toInstant().toEpochMilli();
    }

    /** Whether to make one now by the clock: its time has come, and it has not failed in the last half hour. */
    static boolean due(Place p,long now,ZoneId zone) {
        long next=next(p,zone);
        return p.on&&next>0&&now>=next&&now-p.tried>=AGAIN_AFTER;
    }
    /** Whether to make one now that Mininotes is being closed: set so, and none made in the last minutes. */
    static boolean dueOnClose(Place p,long now){return p.on&&p.often==Often.ON_CLOSE&&now-p.tried>=CLOSE_AT_MOST;}
    /** Whether one waiting for its device is pushed again now: on, and its last try half an hour ago or more (decision 330). */
    static boolean pushAgain(Place p,long now){return p.on&&p.toDevice()&&now-p.tried>=AGAIN_AFTER;}
    /** Whether anything about it needs looking at by the clock: on, and with a time, or a device a backup may wait for. */
    static boolean timed(Place p){return p.on&&(p.often!=Often.ASKED&&p.often!=Often.ON_CLOSE||p.toDevice());}

    /** A backup's name: "mininotes-backup-Pixel-7-20261105-204000.mnbackup", the device's name made safe for any folder. */
    static String fileName(String device,long at,ZoneId zone) {
        return START+safe(device)+"-"+DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss",Locale.ROOT).format(Instant.ofEpochMilli(at).atZone(zone))+ENDING;
    }
    /** The device's name as a file's: letters and digits, the rest a hyphen, never empty, at most 40. */
    static String safe(String device) {
        StringBuilder out=new StringBuilder();
        String name=device==null?"":device.trim();
        for(int i=0;i<name.length()&&out.length()<40;i++){char c=name.charAt(i);out.append(c<128&&Character.isLetterOrDigit(c)?c:'-');}
        String made=out.toString().replaceAll("-+","-").replaceAll("^-|-$","");
        return made.isEmpty()?"device":made;
    }

    /**
     * Which of the names in a folder to remove once a new backup there is whole: this device's own, by the names this page
     * gives, beyond the newest {@code keep}. Never anything else in the folder, never another device's.
     */
    static List<String> beyond(List<String> names,String device,int keep) {
        String own=START+safe(device)+"-";
        List<String> mine=new ArrayList<>();
        for(String n:names)if(n!=null&&n.startsWith(own)&&n.endsWith(ENDING)&&n.length()==own.length()+15+ENDING.length()&&n.substring(own.length(),own.length()+15).matches("\\d{8}-\\d{6}"))mine.add(n);
        // The stamp sorts as the time does.
        Collections.sort(mine);Collections.reverse(mine);
        return mine.size()<=keep?new ArrayList<>():new ArrayList<>(mine.subList(keep,mine.size()));
    }

    /** What the notebook is, in a few bytes: its text and its files' names, so a backup when closing knows nothing changed. */
    static String fingerprint(String text,List<String> files,What what) {
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            d.update(what.name().getBytes(StandardCharsets.UTF_8));d.update((byte)0);
            d.update(text.getBytes(StandardCharsets.UTF_8));
            if(what==What.EVERYTHING)for(String f:files){d.update((byte)0);d.update(f.getBytes(StandardCharsets.UTF_8));}
            StringBuilder hex=new StringBuilder();for(byte b:d.digest())hex.append(String.format(Locale.ROOT,"%02x",b));
            return hex.toString();
        } catch(java.security.NoSuchAlgorithmException none){return "";}
    }

    /** "Each day at 20:00", "Each Monday at 08:00", "On the 1st of each month at 20:00", "When asked", "When Mininotes closes". */
    static String when(Place p,boolean phone) {
        String hour=String.format(Locale.ROOT,"%02d:00",p.hour);
        switch(p.often) {
            case ASKED: return Say.t("Only when I ask");
            case ON_CLOSE: return phone?Say.t("Each time I leave Mininotes"):Say.t("Each time Mininotes closes");
            case DAILY: return Say.t("Each day at {0}",hour);
            case WEEKLY: return Say.t("Each week, {0}, at {1}",dayName(p.weekday),hour);
            default: return Say.t("Each month, on day {0}, at {1}",String.valueOf(p.monthday),hour);
        }
    }
    /** Monday to Sunday in the language in use. */
    static String dayName(int weekday) {
        String name=DayOfWeek.of(weekday).getDisplayName(java.time.format.TextStyle.FULL,Locale.forLanguageTag(Say.language()));
        return name.isEmpty()?name:name.substring(0,1).toUpperCase(Locale.forLanguageTag(Say.language()))+name.substring(1);
    }
    /** The choices of {@link Often}, said, in order. */
    static String[] oftenNames(boolean phone){return new String[]{Say.t("Only when I ask"),Say.t("Each day"),Say.t("Each week"),Say.t("Each month"),phone?Say.t("Each time I leave Mininotes"):Say.t("Each time Mininotes closes")};}
    static String[] whatNames(){return new String[]{Say.t("Everything"),Say.t("Everything but the files")};}
    static String[] opensNames(boolean locked) {
        List<String> out=new ArrayList<>();
        if(locked)out.add(Say.t("The notebook's lock"));
        out.add(Say.t("A password for the backups"));out.add(Say.t("12 recovery words"));
        out.add(Say.t("A key file"));out.add(Say.t("This device only"));
        return out.toArray(new String[0]);
    }

    // ---- step 3: a key file, and this device only (decision 329) -------------------------------------------------
    // Both are a long random secret that the lock is made with, as a password would be (Vault.make): written to a file
    // the owner keeps apart from the backups, or kept in this device's own settings, never in the notebook or a backup.

    /** The first line of a key file, and its ending. */
    static final String KEY_HEAD="MININOTES BACKUP KEY 1",KEY_ENDING=".mnkey";
    /** Where this device keeps the secrets of its "this device only" backups: its settings, never the notebook. */
    static final String HERE_KEYS="backupKeysHere";
    /** A new secret: 32 random bytes, as letters a file or a setting holds. */
    static String newSecret() {
        byte[] b=new byte[32];new java.security.SecureRandom().nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    /** What a key file holds. */
    static String keyFile(String secret){return KEY_HEAD+"\n"+secret+"\n";}
    /** The secret a key file holds, or null for any other file. */
    static String secretIn(byte[] file) {
        if(file==null||file.length>4096)return null;
        String[] lines=new String(file,StandardCharsets.UTF_8).replace("\r","").split("\n");
        if(lines.length<2||!lines[0].trim().equals(KEY_HEAD))return null;
        String secret=lines[1].trim();
        return secret.matches("[A-Za-z0-9_-]{20,100}")?secret:null;
    }
    /** "mininotes-backup-key-Pixel-7.mnkey": the name a key file is offered under. */
    static String keyFileName(String device){return "mininotes-backup-key-"+safe(device)+KEY_ENDING;}
    /** The secrets kept here with one more. */
    static java.util.Set<String> withHere(java.util.Set<String> kept,String secret) {
        java.util.Set<String> out=new java.util.LinkedHashSet<>(kept==null?java.util.Collections.emptySet():kept);out.add(secret);return out;
    }
    /** The key of a backup's lock, from one of the secrets this device keeps or a key file's; null where none opens it. Slow: off the screen's thread. */
    static byte[] openWith(byte[] lock,java.util.Collection<String> secrets) {
        if(lock==null||secrets==null)return null;
        for(String one:secrets)try{return Vault.open(lock,one.toCharArray());}catch(Exception notThis){/* the next */}
        return null;
    }
    /** What the choice at {@code index} of {@link #opensNames} is. */
    static Opens opensAt(int index,boolean locked){return locked?Opens.values()[index]:Opens.values()[index+1];}
    static int indexOf(Opens o,boolean locked){return locked?o.ordinal():Math.max(0,o.ordinal()-1);}

    /**
     * The last try, in a few words: "Last backup: 5 Nov 20:40", or, when it failed, what to do first and why after (the owner,
     * 2026-11-07: "present the solution instead of presenting the problem").
     */
    static String lastSaid(Place p,ZoneId zone) {
        String at=p.last==0?"":DateTimeFormatter.ofPattern("d MMM HH:mm",Locale.forLanguageTag(Say.language())).format(Instant.ofEpochMilli(p.last).atZone(zone));
        switch(p.said) {
            case NO_FOLDER: return Say.t("Plug in the drive it is on, or choose another folder: the last backup could not reach it.");
            case NO_DEVICE: return Say.t("Open Mininotes on {0}, on the same Wi-Fi: the backup waiting for it goes by itself.",p.shown);
            case NOT_TAKEN: return Say.t("Open Mininotes on {0}, then press Back up now: it did not take the last one.",p.shown);
            case NO_ROOM: return Say.t("Free some room there, or choose another folder: the last backup did not fit.");
            case NO_KEY: return Say.t("Unlock Mininotes, then press Back up now: a locked notebook cannot be backed up.");
            case FAILED: return Say.t("Press Back up now to try again: the last try did not finish, and nothing was replaced.");
            default: return p.last==0?Say.t("No backup made here yet."):Say.t("Last backup: {0}",at);
        }
    }
    /** Whether its last try asks for another folder: the card then offers Choose another folder… beside Back up now. */
    static boolean wantsFolder(Place p){return !p.toDevice()&&(p.said==Said.NO_FOLDER||p.said==Said.NO_ROOM);}
    /** "Next: 6 Nov 20:00", or nothing for one made when asked or when closing. */
    static String nextSaid(Place p,ZoneId zone) {
        long next=next(p,zone);
        if(next<0||!p.on)return "";
        long shown=Math.max(next,System.currentTimeMillis());
        return Say.t("Next: {0}",DateTimeFormatter.ofPattern("d MMM HH:mm",Locale.forLanguageTag(Say.language())).format(Instant.ofEpochMilli(shown).atZone(zone)));
    }
    private static int clamp(int v,int low,int high){return Math.max(low,Math.min(high,v));}
    private static <E extends Enum<E>> E pick(Class<E> kind,String name,E otherwise){try{return Enum.valueOf(kind,name);}catch(RuntimeException no){return otherwise;}}
    /** For the tests: a time in a zone, as a number. */
    static long at(LocalDateTime when,ZoneId zone){return when.atZone(zone).toInstant().toEpochMilli();}
}
