// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The README note, said from the README the repository shows (the owner, 2026-10-19: "Again, this needs a full description
 * in the README file; name it this way too, README, as people are used to see it everywhere including on GitHub. And these
 * should match: the note in the app and what is available on GitHub, so please make sure everything we have now in both
 * places is combined in one global version"; decision 221).
 *
 * <p>help/README.md is a copy of the root README.md, kept identical (ReadMeTextTest fails when they differ), and is turned
 * into the note's own writing here: a heading a bold line, a link its words (and its address in brackets when it leads out
 * to the web), a picture or a badge nothing, a table row "- first cell: second cell", a code block its plain lines, and the
 * lines GitHub joins into one paragraph joined. Bold and italic marks stay: they are the note's own.
 *
 * <p>A note holds 24,000 characters at most, and the whole README is more: what is between {@link #LEAVE_OUT} and {@link
 * #TAKE_IN}, HTML comments GitHub does not show, is left out of the note (the contents, What's new, installing, the menu
 * table the PC says under the pointer anyway, building, the repository's files), and a heading's number with it, so the
 * note's headings have no gaps. ReadMeTextTest fails when the note would say more than {@link #MOST}.
 *
 * <p>help/README.sha256 lists the SHA-256 of every text a build ever wrote as the note, one a line, appended whenever the
 * README changes (ReadMeTextTest fails until it is): a note still saying exactly one of them is nobody's writing, and is
 * said again by the next build (see NoteStore.readMeAgain). Holds no Android types.
 */
final class ReadMe {
    private ReadMe(){}

    static final String MARKDOWN="/org/mininotes/android/help/README.md", SHIPPED="/org/mininotes/android/help/README.sha256";

    private static String text;
    private static Set<String> shipped;

    /** The note's text, from the README kept inside the app, or null where it could not be read. */
    static synchronized String text() {
        if(text==null){String md=read(MARKDOWN);if(md!=null)text=note(md);}
        return text;
    }

    /** Whether a note's body is exactly a text some build wrote as the README. */
    static boolean shipped(String body) {
        if(body==null)return false;
        synchronized(ReadMe.class) {
            if(shipped==null) {
                Set<String> all=new HashSet<>();String list=read(SHIPPED);
                if(list!=null)for(String line:list.split("\n")){String t=line.trim();if(!t.isEmpty()&&!t.startsWith("#"))all.add(t.toLowerCase(java.util.Locale.ROOT));}
                shipped=Collections.unmodifiableSet(all);
            }
        }
        return shipped.contains(sha256(body));
    }

    /** SHA-256 of the text's UTF-8, in lower-case hex. */
    static String sha256(String text) {
        try {
            byte[] d=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte b:d)hex.append(String.format("%02x",b&0xff));
            return hex.toString();
        } catch(java.security.NoSuchAlgorithmException never){throw new IllegalStateException(never);}
    }

    private static String read(String resource) {
        try(InputStream in=ReadMe.class.getResourceAsStream(resource)) {
            if(in==null)return null;
            ByteArrayOutputStream all=new ByteArrayOutputStream();byte[] chunk=new byte[8192];
            for(int n;(n=in.read(chunk))>0;)all.write(chunk,0,n);
            return all.toString(StandardCharsets.UTF_8.name()).replace("\r\n","\n").replace('\r','\n');
        } catch(Exception unreadable){return null;}
    }

    // ---- markdown to the note's writing ------------------------------------------------------------------------------

    private static final Pattern HEADING=Pattern.compile("^ {0,3}#{1,6}\\s+(.*?)\\s*#*\\s*$");
    /** The lines that leave a part of the README out of the note, and take it in again (see the class). */
    static final String LEAVE_OUT="<!-- not in the app -->", TAKE_IN="<!-- in the app again -->";
    /**
     * The most the note may say: under the 24,000 characters a note holds (DesktopEdits, MainActivity.PAGE_MOST, a backup's
     * notes), and under the 21,600 where the PC starts to warn, with room for a few lines of the reader's own.
     */
    static final int MOST=21000;
    private static final Pattern NUMBER=Pattern.compile("^\\d+\\.\\s+");
    private static final Pattern BOLD=Pattern.compile("^\\s*\\*\\*([^*]+)\\*\\*\\s*$");
    private static final Pattern ITEM=Pattern.compile("^(\\s*)(?:[-*+]|\\d+[.)])\\s+.*");
    private static final Pattern RULE=Pattern.compile("^ {0,3}(?:(?:-\\s*){3,}|(?:\\*\\s*){3,}|(?:_\\s*){3,})$");
    private static final Pattern DIVIDER=Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final Pattern IMAGE=Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");
    private static final Pattern LINK=Pattern.compile("\\[([^\\]]*)\\]\\(([^)\\s]*)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern CODE=Pattern.compile("`([^`]*)`");
    private static final Pattern ESCAPE=Pattern.compile("\\\\([\\\\`*_{}\\[\\]()#+\\-.!|<>])");

    /** README markdown as the note writes it (see the class). */
    static String note(String markdown) {
        String[] lines=markdown.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        List<String> out=new ArrayList<>();
        // What the last line written was: a paragraph or an item a next plain line joins, or something it does not.
        boolean joins=false,fenced=false;
        boolean left=false;
        for(int i=0;i<lines.length;i++) {
            String line=lines[i];
            if(!fenced&&line.trim().equals(LEAVE_OUT)){left=true;continue;}
            if(!fenced&&line.trim().equals(TAKE_IN)){left=false;blank(out);joins=false;continue;}
            if(left)continue;
            if(line.trim().startsWith("```")){fenced=!fenced;joins=false;continue;}
            if(fenced){out.add(line);continue;}
            if(line.trim().startsWith("<!--")&&line.trim().endsWith("-->")){blank(out);joins=false;continue;}
            if(line.trim().isEmpty()||RULE.matcher(line).matches()){blank(out);joins=false;continue;}
            Matcher h=HEADING.matcher(line),bold=BOLD.matcher(line);
            // A line all bold is a heading too, under a numbered one ("**Private spaces**"), and written as one.
            if(h.matches()||!joins&&bold.matches()) {
                blank(out);
                // A heading's number is for GitHub's contents, which the note leaves out with some of the sections numbered.
                String words=NUMBER.matcher(inline(h.matches()?h.group(1):bold.group(1)).replace("**","").trim()).replaceFirst("");
                if(!words.isEmpty())out.add("**"+words+"**");
                out.add(null);// a heading's own blank lines are not kept: its first line follows it at once
                joins=false;continue;
            }
            if(line.trim().startsWith("|")) {
                // The heading row, the one over the dashes, says what the columns are: the rows say it themselves.
                if(DIVIDER.matcher(line).matches()||i+1<lines.length&&DIVIDER.matcher(lines[i+1]).matches()&&lines[i+1].contains("-")){joins=false;continue;}
                List<String> cells=cells(line);
                StringBuilder row=new StringBuilder("- ");
                for(int c=0;c<cells.size();c++){String cell=inline(cells.get(c)).trim();if(cell.isEmpty())continue;if(row.length()>2)row.append(c==1?": ":", ");row.append(cell);}
                if(row.length()>2)out.add(row.toString());
                joins=false;continue;
            }
            String said=inline(line);
            if(said.trim().isEmpty())continue;// a badge or a picture alone on its line
            if(joins&&!ITEM.matcher(line).matches()&&!out.isEmpty()&&out.get(out.size()-1)!=null) {
                out.set(out.size()-1,out.get(out.size()-1)+" "+said.trim());continue;
            }
            Matcher item=ITEM.matcher(line);
            if(item.matches())said=item.group(1).replace("\t","  ")+said.trim();
            else said=said.trim();
            out.add(said);joins=true;
        }
        StringBuilder text=new StringBuilder();boolean gap=false;
        for(String one:out) {
            if(one==null)continue;
            if(one.isEmpty()){gap=text.length()>0;continue;}
            if(text.length()>0)text.append(gap?"\n\n":"\n");
            text.append(one);gap=false;
        }
        return text.toString();
    }

    /** A blank line, where the last one written is not one already; after a heading, none (marked by a null). */
    private static void blank(List<String> out) {
        if(out.isEmpty())return;
        String last=out.get(out.size()-1);
        if(last==null||last.isEmpty())return;
        out.add("");
    }

    private static List<String> cells(String row) {
        String t=row.trim();
        if(t.startsWith("|"))t=t.substring(1);
        if(t.endsWith("|")&&!t.endsWith("\\|"))t=t.substring(0,t.length()-1);
        List<String> cells=new ArrayList<>();StringBuilder cell=new StringBuilder();
        for(int i=0;i<t.length();i++) {
            char c=t.charAt(i);
            if(c=='\\'&&i+1<t.length()&&t.charAt(i+1)=='|'){cell.append('|');i++;continue;}
            if(c=='|'){cells.add(cell.toString());cell.setLength(0);continue;}
            cell.append(c);
        }
        cells.add(cell.toString());
        return cells;
    }

    /** One line's inline markdown as the note writes it: pictures out, links their words, code its letters, marks kept. */
    static String inline(String line) {
        String s=IMAGE.matcher(line).replaceAll("");
        Matcher link=LINK.matcher(s);StringBuffer done=new StringBuffer();
        while(link.find()) {
            String words=link.group(1).trim(),to=link.group(2);
            boolean web=to.regionMatches(true,0,"http://",0,7)||to.regionMatches(true,0,"https://",0,8);
            String said=words.isEmpty()?"":web&&!words.equals(to)?words+" ("+to+")":words;
            link.appendReplacement(done,Matcher.quoteReplacement(said));
        }
        link.appendTail(done);
        s=CODE.matcher(done.toString()).replaceAll("$1");
        return ESCAPE.matcher(s).replaceAll("$1");
    }
}
