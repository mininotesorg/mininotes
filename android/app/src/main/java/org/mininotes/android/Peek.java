// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The first words of a document, for its card to show as a small page instead of its kind alone (the owner, 2026-10-18:
 * "could we have a preview of the doc instead of just displaying in the image the file type?"; decision 208): Word,
 * PowerPoint, Excel and their open counterparts read from the XML inside them, and plain text as it is. Read here, from the
 * bytes already out of the notebook, and never handed to anything of the system's, so nothing of it is kept outside.
 * A PDF is drawn by each app (PdfRenderer on the phone, PDFBox on the PC). Holds no Android or Swing types.
 */
final class Peek {
    private Peek(){}

    /** The most of a file read for its first words: past it, its card shows its kind. */
    static final long MOST=20L*1024*1024;
    /** How much of what is unpacked is read at most, so a crafted file cannot unpack into all the memory there is. */
    private static final int UNPACKED_MOST=4*1024*1024;

    private static String ending(String name){
        String low=name==null?"":name.toLowerCase(Locale.ROOT);int dot=low.lastIndexOf('.');return dot<0?"":low.substring(dot+1);
    }

    /** Whether it is a PDF, which each app draws itself. */
    static boolean pdf(String name,String kind){return "pdf".equals(ending(name))||"application/pdf".equals(kind);}

    /** Whether its first words can be read here. */
    static boolean readable(String name,String kind) {
        switch(ending(name)) {
            case "docx": case "pptx": case "xlsx": case "odt": case "odp": case "ods":
            case "txt": case "md": case "csv": case "tsv": case "json": case "log": case "xml": case "html": case "htm": case "rtf":
                return true;
            default: return kind!=null&&kind.startsWith("text/");
        }
    }

    /** Whether its card can show more than its kind: a PDF, or a document whose words can be read. */
    static boolean previewed(String name,String kind,long bytes){return bytes>0&&bytes<=MOST&&(pdf(name,kind)||readable(name,kind));}

    /**
     * Its first lines, at most {@code most} of them, each trimmed; empty where it holds no words or cannot be read.
     */
    static List<String> lines(byte[] bytes,String name,String kind,int most) {
        List<String> out=new ArrayList<>();
        if(bytes==null||bytes.length==0)return out;
        try {
            switch(ending(name)) {
                case "docx": return paragraphs(part(bytes,"word/document.xml"),"w:p","w:t",most);
                case "pptx": {
                    // The slides in their order: slide1, slide2, … each its own part.
                    for(int n=1;n<=40&&out.size()<most;n++) {
                        String slide=part(bytes,"ppt/slides/slide"+n+".xml");if(slide==null)break;
                        out.addAll(paragraphs(slide,"a:p","a:t",most-out.size()));
                    }
                    return out;
                }
                case "xlsx": return paragraphs(part(bytes,"xl/sharedStrings.xml"),"si","t",most);
                case "odt": case "odp": case "ods": return paragraphs(part(bytes,"content.xml"),"text:p","",most);
                case "rtf": return plain(rtf(new String(bytes,0,Math.min(bytes.length,UNPACKED_MOST),StandardCharsets.ISO_8859_1)),most);
                case "html": case "htm": case "xml":
                    return plain(unescape(new String(bytes,0,Math.min(bytes.length,UNPACKED_MOST),StandardCharsets.UTF_8)
                        .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>"," ").replaceAll("(?i)<(br|/p|/div|/h[1-6]|/li|/tr)[^>]*>","\n").replaceAll("<[^>]*>"," ")),most);
                default: return plain(new String(bytes,0,Math.min(bytes.length,64*1024),StandardCharsets.UTF_8),most);
            }
        } catch(Exception unreadable) {
            return new ArrayList<>();
        }
    }

    /** One part of a zipped document, as text, or null where it has none. */
    private static String part(byte[] zipped,String name) throws java.io.IOException {
        try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(zipped))) {
            for(ZipEntry one;(one=in.getNextEntry())!=null;) {
                if(!name.equals(one.getName()))continue;
                ByteArrayOutputStream read=new ByteArrayOutputStream();byte[] chunk=new byte[16384];
                for(int got;(got=in.read(chunk))>0&&read.size()<UNPACKED_MOST;)read.write(chunk,0,got);
                return new String(read.toByteArray(),StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** Each paragraph's words, its runs put together: the words of {@code run} inside each {@code para}, or all its text. */
    private static List<String> paragraphs(String xml,String para,String run,int most) {
        List<String> out=new ArrayList<>();
        if(xml==null)return out;
        Matcher each=Pattern.compile("<"+Pattern.quote(para)+"(?:\\s[^>]*)?(?:/>|>(.*?)</"+Pattern.quote(para)+">)",Pattern.DOTALL).matcher(xml);
        Pattern words=run.isEmpty()?null:Pattern.compile("<"+Pattern.quote(run)+"(?:\\s[^>]*)?>(.*?)</"+Pattern.quote(run)+">",Pattern.DOTALL);
        while(each.find()&&out.size()<most) {
            String inside=each.group(1);if(inside==null)continue;
            StringBuilder said=new StringBuilder();
            if(words==null)said.append(inside.replaceAll("<text:(s|tab)[^>]*/>"," ").replaceAll("<[^>]*>",""));
            else for(Matcher w=words.matcher(inside);w.find();)said.append(w.group(1));
            String line=unescape(said.toString()).replaceAll("\\s+"," ").trim();
            if(!line.isEmpty())out.add(line);
        }
        return out;
    }

    private static List<String> plain(String text,int most) {
        List<String> out=new ArrayList<>();
        for(String line:text.split("\\r?\\n|\\r")) {
            if(out.size()>=most)break;
            String t=line.replace('\t',' ').replaceAll("\\s+"," ").trim();
            // Not the bytes of something that is not text.
            if(t.isEmpty()||t.indexOf('\u0000')>=0||t.indexOf('�')>=0)continue;
            out.add(t.length()>200?t.substring(0,200):t);
        }
        return out;
    }

    /** RTF's words without its control words and groups, roughly. */
    private static String rtf(String text) {
        return text.replaceAll("\\\\par[d]?\\b","\n").replaceAll("\\{\\\\\\*[^{}]*\\}","").replaceAll("\\\\'[0-9a-fA-F]{2}","")
            .replaceAll("\\\\[a-zA-Z]+-?\\d* ?","").replaceAll("[{}]","");
    }

    private static String unescape(String s) {
        return s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;","'").replace("&#39;","'").replace("&nbsp;"," ").replace("&amp;","&");
    }
}
