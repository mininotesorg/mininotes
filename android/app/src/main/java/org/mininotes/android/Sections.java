// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.List;

/**
 * A note's tabs, kept as sections in its own words (the owner, 2026-10-10: "a note should have the possibility to have
 * multiple tabs"; tabs stored as sections, decision 142). A line that says {@code === Ideas ===} begins the tab called
 * Ideas; the words before the first such line, where there are any, are a first tab with no name of its own. Nothing
 * else changes: a note is still one text, so it is saved, merged, shared and backed up as it always was, and a build
 * from before reads the same words with the tab lines in them as headings.
 *
 * <p>Holds no Android types and no Swing: both apps read a note's tabs the same way, and every rule is unit tested.
 */
final class Sections {
    static final String OPEN="=== ", CLOSE=" ===";
    /** Longer names are cut: a tab is a word or two on a strip. */
    static final int NAME_MOST=40;
    /** What a first tab with no name of its own is called on the strip, and the name given to it when a tab is added. */
    static final String FIRST="Page 1";

    private Sections(){}

    /** One tab: its name, or null for words before any tab line; where its line begins; and where its words are. */
    static final class Section {
        final String name; final int line,start,end;
        Section(String name,int line,int start,int end){this.name=name;this.line=line;this.start=start;this.end=end;}
        /** What the strip calls it. */
        String called(int index){return name!=null?name:index==0?FIRST:"Page "+(index+1);}
    }

    /** Whether a line, without its line end, is a tab line. */
    static boolean isLine(String line) {
        if(line==null)return false;
        String l=line.endsWith("\r")?line.substring(0,line.length()-1):line;
        if(!l.startsWith(OPEN)||!l.endsWith(CLOSE)||l.length()<OPEN.length()+CLOSE.length()+1)return false;
        String name=l.substring(OPEN.length(),l.length()-CLOSE.length());
        return !name.isBlank()&&!name.contains("===")&&name.length()<=NAME_MOST&&name.equals(name.strip());
    }

    /** A name as a tab line can hold it: one line, no ===, not too long; nothing at all becomes the next page's name. */
    static String clean(String name,int index) {
        String n=name==null?"":name.replace('\r',' ').replace('\n',' ').replace("===","").strip();
        while(n.contains("  "))n=n.replace("  "," ");
        if(n.length()>NAME_MOST)n=n.substring(0,NAME_MOST).strip();
        return n.isEmpty()?"Page "+(index+1):n;
    }

    static String line(String name,int index){return OPEN+clean(name,index)+CLOSE;}

    /** The tabs, in order. A note with no tab line is one tab with no name, holding all of it. */
    static List<Section> of(String body) {
        String text=body==null?"":body;
        List<int[]> lines=new ArrayList<>();List<String> names=new ArrayList<>();
        int at=0;
        while(at<=text.length()) {
            int end=text.indexOf('\n',at);if(end<0)end=text.length();
            String l=text.substring(at,end);
            if(isLine(l)){lines.add(new int[]{at,end});String c=l.endsWith("\r")?l.substring(0,l.length()-1):l;names.add(c.substring(OPEN.length(),c.length()-CLOSE.length()));}
            if(end>=text.length())break;
            at=end+1;
        }
        List<Section> out=new ArrayList<>();
        if(lines.isEmpty()){out.add(new Section(null,-1,0,text.length()));return out;}
        int first=lines.get(0)[0];
        // Words before the first tab line are a tab of their own; the line end before that line is between them.
        if(first>0)out.add(new Section(null,-1,0,first-1));
        for(int i=0;i<lines.size();i++) {
            int start=Math.min(lines.get(i)[1]+1,text.length());
            int end=i+1<lines.size()?lines.get(i+1)[0]-1:text.length();
            if(end<start)end=start;
            out.add(new Section(names.get(i),lines.get(i)[0],start,end));
        }
        return out;
    }

    /** Whether the strip is shown: more than one tab, or one that has been given a name. */
    static boolean tabbed(String body){List<Section> all=of(body);return all.size()>1||all.get(0).name!=null;}

    /** The words of one tab. */
    static String words(String body,int index) {
        String text=body==null?"":body;Section s=of(text).get(index);
        return text.substring(s.start,s.end);
    }

    /** The note with one tab's words changed, every other letter as it was. */
    static String put(String body,int index,String words) {
        String text=body==null?"":body,now=words==null?"":words;
        List<Section> all=of(text);Section s=all.get(index);
        boolean last=index==all.size()-1;
        // Between two tab lines with nothing between them there is no line end to keep words off the next tab line.
        if(!last&&s.start==s.end&&!now.isEmpty()&&s.end<text.length()&&text.charAt(s.end)!='\n')now=now+"\n";
        // A tab line that is the last thing in the note has no line end after it: the words need one before them.
        if(s.name!=null&&s.start==text.length()&&!now.isEmpty()&&(text.isEmpty()||text.charAt(text.length()-1)!='\n'))now="\n"+now;
        return text.substring(0,s.start)+now+text.substring(s.end);
    }

    /**
     * A new tab, last. A note that had none becomes two: what it said, as Page 1, then the new one.
     *
     * @return the note with the tab added
     */
    static String add(String body,String name) {
        String text=body==null?"":body;
        List<Section> all=of(text);
        String made=line(name,tabbed(text)?all.size():1);
        if(!tabbed(text))return line(FIRST,0)+"\n"+text+(text.isEmpty()?"":"\n")+made;
        return text+(text.isEmpty()||text.endsWith("\n")?"":"\n")+made;
    }

    /** One tab called something else. The first tab with no name of its own is given one. */
    static String rename(String body,int index,String name) {
        String text=body==null?"":body;Section s=of(text).get(index);
        String made=line(name,index);
        if(s.name==null)return made+"\n"+text;
        int end=text.indexOf('\n',s.line);if(end<0)end=text.length();
        return text.substring(0,s.line)+made+text.substring(end);
    }

    /** One tab taken out, its line and its words. The last one left is never taken out: a note always has words. */
    static String remove(String body,int index) {
        String text=body==null?"":body;List<Section> all=of(text);
        if(all.size()<=1)return text;
        Section s=all.get(index);
        int from=s.name==null?0:s.line;
        int to=index+1<all.size()?all.get(index+1).name==null?all.get(index+1).start:all.get(index+1).line:text.length();
        // The last tab takes the line end before it with it, so the tab before it ends where its words do.
        if(index==all.size()-1&&from>0)from--;
        return text.substring(0,from)+text.substring(to);
    }
}
