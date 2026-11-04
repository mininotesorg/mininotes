// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * What Mininotes did, for the History page (the owner, 2026-10-31: "do we have a full log of all actions? This should be made
 * available to the user but of course not including anything related to the private space"; decision 305).
 *
 * <p>A line is what happened, to which thing, with which device or person, and when: a note or a file reached somebody (their
 * own answer kept, never a relay's), came from somebody, or a device was heard again after a quiet stretch. A line keeps only
 * ids, never a title or a word of a note: the title and the name are read when the page is drawn, so a thing no longer in the
 * notebook says nothing, and a renamed one shows its name now. The private space is never in the notebook and never travels,
 * so nothing of it can be a line; nor is a help request, sent or heard (decisions 201 and 204: no trace). The same thing with
 * the same device again within ten minutes is the same line, its time the latest, so a note written in for an hour is a few
 * lines, not hundreds. Kept thirty days, two thousand lines at most. Holds no Android types: both apps say the same words.
 */
final class History {
    private History(){}

    /** What a line says happened. The code is what the notebook keeps: never renamed once shipped. */
    enum Kind {
        NOTE_REACHED("note-reached"), NOTE_CAME("note-came"), FILE_REACHED("file-reached"), FILE_CAME("file-came"), BACK("back");
        final String code;
        Kind(String code){this.code=code;}
        static Kind of(String code){for(Kind one:values())if(one.code.equals(code))return one;return null;}
        boolean file(){return this==FILE_REACHED||this==FILE_CAME;}
    }

    /** The same thing with the same device within this is one line. */
    static final long TOGETHER=10*60_000L;
    /** How long a line is kept. */
    static final long KEPT=30L*24*60*60_000L;
    /** And how many at most, the oldest let go first. */
    static final int MOST=2000;
    /** A device heard again after this long without a word is said to be back: what decision 301 counts as quiet. */
    static final long QUIET=15*60_000L;

    /**
     * One line as kept: when (the latest time), what, which thing by id (empty for BACK), which device or person by address, and
     * for what reached somebody the road the last send to them went, as {@link Routes#road} writes it (empty where not known).
     */
    record Line(long id,long at,Kind kind,String thing,String address,int times,String road) {}

    /** Whether something happening now is the same line as one kept at {@code was}. */
    static boolean together(long was,long now){return now>=was&&now-was<TOGETHER;}

    /** Whether a device last heard at {@code was} (0: not since this start) is back, heard {@code now}. */
    static boolean back(long was,long now){return was>0&&now-was>=QUIET;}

    /**
     * The sentence for a line, in the language in use: {@code title} the note's or file's name now (blank: Untitled), {@code who}
     * the device's or person's name.
     */
    static String said(Kind kind,String title,String who) {
        String what=title==null||title.isBlank()?Say.t("Untitled"):title.trim();
        switch(kind) {
            case NOTE_REACHED: case FILE_REACHED: return Say.t("{0} reached {1}.",what,who);
            case NOTE_CAME: case FILE_CAME: return Say.t("{0} came from {1}.",what,who);
            default: return Say.t("{0} can be reached again.",who);
        }
    }

    /**
     * The same, with how it went where it reached somebody (the owner, 2026-11-03: "could we have a list of transfers and indicate
     * how they went through? ... could it also keep the IP of the public relay?"; decision 322): "Plan reached Pro, through your
     * relay 70.34.244.170." A relay is the owner's where it is among {@code own} (host and port, as Settings keeps them), public
     * otherwise.
     */
    static String said(Kind kind,String title,String who,String road,java.util.Collection<String> own) {
        String how=kind==Kind.NOTE_REACHED||kind==Kind.FILE_REACHED?how(road,own):"";
        if(how.isEmpty())return said(kind,title,who);
        String what=title==null||title.isBlank()?Say.t("Untitled"):title.trim();
        return Say.t("{0} reached {1}, {2}.",what,who,how);
    }

    /** How a road went, in a few words: direct, left at your PC, through your relay or a public relay and its address. */
    static String how(String road,java.util.Collection<String> own) {
        if(road==null||road.isBlank())return "";
        String[] part=road.trim().split(" ");
        if(part[0].equals("door"))return Say.t("direct");
        if(part[0].equals("home"))return Say.t("left at your PC");
        if(!part[0].equals("relay"))return "";
        java.util.Set<String> mine=new java.util.HashSet<>();
        if(own!=null)for(String one:own)if(one!=null)mine.add(one.trim().toLowerCase(Locale.ROOT));
        java.util.List<String> ours=new java.util.ArrayList<>(),theirs=new java.util.ArrayList<>();
        for(int at=1;at<part.length;at++)(mine.contains(part[at].toLowerCase(Locale.ROOT))?ours:theirs).add(ip(part[at]));
        String first=ours.isEmpty()?"":Say.t(ours.size()==1?"through your relay {0}":"through your relays {0}",String.join(", ",ours));
        String then=theirs.isEmpty()?"":Say.t(theirs.size()==1?"through a public relay {0}":"through public relays {0}",String.join(", ",theirs));
        if(first.isEmpty()&&then.isEmpty())return Say.t("through a relay");
        return first.isEmpty()?then:then.isEmpty()?first:Say.t("{0} and {1}",first,then);
    }

    /** A relay's host without its port, an IPv6 one without its brackets: the address the owner reads. */
    static String ip(String hostPort) {
        String host=hostPort.trim();
        int colon=host.lastIndexOf(':');
        if(colon>0&&host.indexOf(':')==colon||host.startsWith("[")&&colon>host.indexOf(']'))host=host.substring(0,colon);
        return host.startsWith("[")&&host.endsWith("]")?host.substring(1,host.length()-1):host;
    }

    /** The day a line is under: Today, Yesterday, or the date in the language in use. */
    static String day(long at,long now,ZoneId zone) {
        LocalDate when=Instant.ofEpochMilli(at).atZone(zone).toLocalDate(),today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        if(when.equals(today))return Say.t("Today");
        if(when.equals(today.minusDays(1)))return Say.t("Yesterday");
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(Say.language())).format(when);
    }

    /** The time on a line, as the language writes it. */
    static String time(long at,ZoneId zone) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(Say.language()))
            .format(Instant.ofEpochMilli(at).atZone(zone));
    }
}
