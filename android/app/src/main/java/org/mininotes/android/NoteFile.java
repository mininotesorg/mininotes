// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.List;
import java.util.Locale;

/**
 * A note kept as a Markdown file (the owner, 2026-10-13: notes stay inside by default, and a note or a folder can also be
 * kept as files anywhere, to see and organise them in Explorer; docs/FILES.md, decision 162). The file is UTF-8 Markdown:
 * a short header that carries the note's id, so a file moved or renamed is still known as the same note, then the note's
 * words as typed. Bold and italic are already Markdown; underline, which Markdown has not, is written {@code <u>…</u>};
 * a tab line, {@code === Ideas ===}, is written {@code <!-- tab: Ideas -->}, which other editors hide.
 *
 * <p>Read back, a file Mininotes wrote gives exactly the words it was written from. Where a note itself says
 * {@code <u>} or begins a line with {@code <!-- tab:}, a backslash is put before it, as Markdown escapes, so it is read
 * back as those letters and not as a mark; backslashes already there are doubled before such a place, and only there.
 *
 * <p>Holds no Android types and no Swing: the phone will write the same files (docs/FILES.md, step 5).
 */
final class NoteFile {
    /** The header's one line of ours, before the id. */
    static final String KEY="mininotes";
    static final String EXTENSION=".md";
    /** Where a note's files are kept beside it: Title.files. */
    static final String FILES=".files";
    private static final String FENCE="---";
    private static final String U_OPEN="<u>",U_CLOSE="</u>",TAB_OPEN="<!-- tab: ",TAB_CLOSE=" -->",TAB_TOKEN="<!-- tab:";
    /** A file name no longer than this, before its .md: Windows' paths stay short enough, and a title is a few words. */
    static final int NAME_MOST=80;

    private NoteFile(){}

    /**
     * A file read: the note's id where its header says one (null for a Markdown file never kept by Mininotes), the note's
     * words, and any other header lines it had, kept as they were for the next writing (Obsidian keeps its own there).
     */
    record Read(String id,String body,String head) {}

    /** The whole file for a note. {@code head}: header lines of others' to keep, each ending in a line end, or empty. */
    static String write(String id,String head,String body) {
        StringBuilder out=new StringBuilder(FENCE).append('\n').append(KEY).append(": ").append(id==null?"":id).append('\n');
        if(head!=null&&!head.isEmpty()){out.append(head);if(!head.endsWith("\n"))out.append('\n');}
        // Never a private code in a file either (decision 114): the guard at the door stands at this door too.
        return out.append(FENCE).append('\n').append(markdown(PrivateCode.scrub(body==null?"":body))).toString();
    }

    /** A file's text as a note: what its header says, and its words in Mininotes' own marks. */
    static Read read(String file) {
        String text=file==null?"":file;
        if(text.startsWith("\uFEFF"))text=text.substring(1);
        // Saved by an editor that writes Windows line ends throughout, the file is read with the line ends a note has. A file
        // Mininotes wrote has its header's line ends as Mininotes wrote them, so a note's own \r\n are kept as they were.
        int first=text.indexOf('\n');
        if(first>0&&text.charAt(first-1)=='\r')text=text.replace("\r\n","\n");
        String id=null,head="",rest=text;
        // A header is a fence whose first line names something, "key: value": a file that only opens on a rule is all words.
        if(text.startsWith(FENCE+"\n")&&text.substring(FENCE.length()+1).matches("(?s)([A-Za-z0-9_-]+:.*|"+FENCE+"(\n.*)?)")) {
            int end=-1,closing=-1,at=FENCE.length()+1;
            while(at<=text.length()) {
                int next=text.indexOf('\n',at);
                String line=next<0?text.substring(at):text.substring(at,next);
                if(line.equals(FENCE)){closing=at;end=next<0?text.length():next+1;break;}
                if(next<0)break;
                at=next+1;
            }
            if(end>=0) {
                StringBuilder others=new StringBuilder();
                for(String line:text.substring(FENCE.length()+1,closing).split("\n",-1)) {
                    if(line.isEmpty())continue;
                    String key=line.toLowerCase(Locale.ROOT);
                    if(id==null&&key.startsWith(KEY+":"))id=line.substring(KEY.length()+1).strip();
                    else others.append(line).append('\n');
                }
                if(id!=null&&id.isEmpty())id=null;
                head=others.toString();rest=text.substring(end);
            }
        }
        return new Read(id,PrivateCode.scrub(words(rest)),head);
    }

    /** A note's words as Markdown: underline as {@code <u>}, tab lines as comments, everything else as it is. */
    static String markdown(String words) {
        String text=words==null?"":words;int length=text.length();
        boolean[] opens=new boolean[length+1],closes=new boolean[length+1];
        List<Marks.Run> runs=Marks.of(text);
        for(Marks.Run run:runs)if(run.style()==Marks.Style.UNDERLINE){opens[run.open()]=true;closes[run.end()]=true;}
        StringBuilder out=new StringBuilder(length+32);
        int at=0;
        while(at<length) {
            boolean lineStart=at==0||text.charAt(at-1)=='\n';
            if(lineStart) {
                int end=text.indexOf('\n',at);if(end<0)end=length;
                String line=text.substring(at,end);
                if(Sections.isLine(line)) {
                    boolean cr=line.endsWith("\r");String bare=cr?line.substring(0,line.length()-1):line;
                    out.append(TAB_OPEN).append(bare,Sections.OPEN.length(),bare.length()-Sections.CLOSE.length()).append(TAB_CLOSE);
                    if(cr)out.append('\r');
                    at=end;continue;
                }
            }
            int slashes=0;while(at+slashes<length&&text.charAt(at+slashes)=='\\')slashes++;
            int after=at+slashes;
            if(after<length&&(opens[after]||closes[after])) {
                // A mark: the backslashes before it doubled, so none of them is read as escaping it.
                repeat(out,'\\',2*slashes).append(opens[after]?U_OPEN:U_CLOSE);
                at=after+Marks.Style.UNDERLINE.mark.length();continue;
            }
            String token=token(text,after,lineStart);
            if(token!=null) {
                // Letters that would read as a mark: escaped, with the backslashes before them doubled.
                repeat(out,'\\',2*slashes+1).append(token);
                at=after+token.length();continue;
            }
            if(slashes>0){repeat(out,'\\',slashes);at=after;continue;}
            out.append(text.charAt(at));at++;
        }
        return out.toString();
    }

    /** Markdown as a note's words: the reverse of {@link #markdown}, and kind to a file somebody else wrote. */
    static String words(String markdown) {
        String text=markdown==null?"":markdown;int length=text.length();
        StringBuilder out=new StringBuilder(length);
        int at=0;
        while(at<length) {
            boolean lineStart=at==0||text.charAt(at-1)=='\n';
            if(lineStart&&text.startsWith(TAB_OPEN,at)) {
                int end=text.indexOf('\n',at);if(end<0)end=length;
                String line=text.substring(at,end);boolean cr=line.endsWith("\r");String bare=cr?line.substring(0,line.length()-1):line;
                if(bare.endsWith(TAB_CLOSE)&&bare.length()>=TAB_OPEN.length()+TAB_CLOSE.length()) {
                    String made=Sections.OPEN+bare.substring(TAB_OPEN.length(),bare.length()-TAB_CLOSE.length())+Sections.CLOSE;
                    // Only a name a tab can have becomes a tab; any other comment stays as it was written.
                    if(Sections.isLine(made)){out.append(made);if(cr)out.append('\r');at=end;continue;}
                }
            }
            int slashes=0;while(at+slashes<length&&text.charAt(at+slashes)=='\\')slashes++;
            int after=at+slashes;
            String token=token(text,after,lineStart);
            if(token!=null) {
                if(slashes%2==1){repeat(out,'\\',(slashes-1)/2).append(token);}
                else if(token.equals(TAB_TOKEN)){repeat(out,'\\',slashes).append(token);}
                else{repeat(out,'\\',slashes/2).append(Marks.Style.UNDERLINE.mark);}
                at=after+token.length();continue;
            }
            if(slashes>0){repeat(out,'\\',slashes);at=after;continue;}
            out.append(text.charAt(at));at++;
        }
        return out.toString();
    }

    /** The letters at {@code at} that mean a mark in a file: {@code <u>}, {@code </u>}, or a tab comment's start at a line's start. */
    private static String token(String text,int at,boolean lineStart) {
        if(text.startsWith(U_OPEN,at))return U_OPEN;
        if(text.startsWith(U_CLOSE,at))return U_CLOSE;
        if(lineStart&&text.startsWith(TAB_TOKEN,at))return TAB_TOKEN;
        return null;
    }

    private static StringBuilder repeat(StringBuilder out,char c,int times){for(int i=0;i<times;i++)out.append(c);return out;}

    /**
     * A name Windows takes for a file: what it cannot hold taken out, not ending in a dot or a space, none of the names it
     * keeps for itself, not too long; nothing left is "Untitled".
     */
    static String fileName(String name) {
        StringBuilder out=new StringBuilder();
        String said=name==null?"":name.strip();
        for(int i=0;i<said.length();i++) {
            char c=said.charAt(i);
            if(c<32||"<>:\"/\\|?*".indexOf(c)>=0){if(out.length()>0&&out.charAt(out.length()-1)!=' ')out.append(' ');continue;}
            out.append(c);
        }
        String made=out.toString().strip();
        while(made.contains("  "))made=made.replace("  "," ");
        if(made.length()>NAME_MOST)made=made.substring(0,NAME_MOST);
        while(!made.isEmpty()&&(made.endsWith(".")||made.endsWith(" ")))made=made.substring(0,made.length()-1);
        if(made.isEmpty())return "Untitled";
        String upper=made.toUpperCase(Locale.ROOT);int dot=upper.indexOf('.');String stem=dot<0?upper:upper.substring(0,dot);
        if(stem.matches("CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9]"))made="_"+made;
        return made;
    }

    /** What a file is called without its .md, for a note made from it. */
    static String nameOf(String fileName) {
        String n=fileName==null?"":fileName,lower=n.toLowerCase(Locale.ROOT);
        return lower.endsWith(EXTENSION)?n.substring(0,n.length()-EXTENSION.length()):lower.endsWith(".markdown")?n.substring(0,n.length()-9):n;
    }

    /** Whether a file is a Markdown one, by its name. */
    static boolean markdownFile(String fileName) {
        String n=fileName==null?"":fileName.toLowerCase(Locale.ROOT);
        return n.endsWith(EXTENSION)||n.endsWith(".markdown");
    }
}
