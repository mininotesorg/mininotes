// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A note translated on the device (docs/TRANSLATION.md; the owner, 2026-10-12: "when we select a text section or a full
 * note, to be able to translate it", and 2026-10-13: Bergamot, packs from our GitHub, the result in a box). What both apps
 * share: which languages, which packs a translation needs (through English where neither side is English), the list of
 * packs we publish, the settings the engine reads, a first guess at the language of some words, and a note's lines taken
 * apart and put back so lists and empty lines come through. Holds no Android or Swing types.
 */
final class Translation {
    private Translation(){}

    /** English, and every language with a pack each way with English: the app's code, and the pack's. */
    static final Map<String,String> PACKS;
    static {
        Map<String,String> all=new LinkedHashMap<>();
        all.put("en","en");all.put("fr","fr");all.put("es","es");all.put("zh","zh-Hans");all.put("de","de");all.put("pt","pt");
        all.put("it","it");all.put("ja","ja");all.put("ru","ru");all.put("hi","hi");all.put("fa","fa");
        // Turkish (the owner, 2026-10-13: "we should have the Turkish language for translation"): a language to translate
        // into and from, not one the app is written in, so it has a name of its own here.
        all.put("tr","tr");
        PACKS=Collections.unmodifiableMap(all);
    }

    /** Languages to translate that the app is not written in, by their own name. */
    private static final Map<String,String> OTHER_NAMES=Map.of("tr","Türkçe");

    /** A language's name in its own words, for the Translate box: the app's languages' as Say has them, the others' here. */
    static String name(String code){return Say.LANGUAGES.getOrDefault(code,OTHER_NAMES.getOrDefault(code,code));}

    /** One pack: from a language to another, one of them English. Its folder and its name on our release are the codes joined. */
    record Pair(String from,String to) {
        String folder(){return PACKS.getOrDefault(from,from)+PACKS.getOrDefault(to,to);}
    }

    /** The packs a translation goes through, in order: none for the same language, one with English, else two through it. */
    static List<Pair> steps(String from,String to) {
        List<Pair> out=new ArrayList<>();
        if(from==null||to==null||from.equals(to))return out;
        if(from.equals("en")||to.equals("en"))out.add(new Pair(from,to));
        else{out.add(new Pair(from,"en"));out.add(new Pair("en",to));}
        return out;
    }

    /** The packs one language needs, both ways with English. */
    static List<Pair> both(String language){return language.equals("en")?List.of():List.of(new Pair("en",language),new Pair(language,"en"));}

    // ---- the list of packs we publish ---------------------------------------------------------------------------------

    /** A file of a pack as our release lists it: which pack, its name, its size and its SHA-256. */
    record File(String pack,String name,long size,String sha256){}

    /**
     * Our list of packs, one file a line: the pack's folder, the file's name, its size and its SHA-256, between tabs. Lines
     * that are not that are left out, so a comment or an empty line is harmless.
     */
    static List<File> manifest(String text) {
        List<File> out=new ArrayList<>();
        if(text==null)return out;
        for(String line:text.split("\r?\n")) {
            String[] part=line.trim().split("\t");
            if(part.length!=4||!part[3].matches("[0-9a-f]{64}")||!part[1].matches("[A-Za-z0-9._-]+")||!part[0].matches("[A-Za-z-]+"))continue;
            try{out.add(new File(part[0],part[1],Long.parseLong(part[2]),part[3]));}catch(NumberFormatException skipped){/* not a line of the list */}
        }
        return out;
    }

    /** The files of one pack in the list. */
    static List<File> filesOf(List<File> all,Pair pair) {
        List<File> out=new ArrayList<>();
        for(File one:all)if(one.pack().equals(pair.folder()))out.add(one);
        return out;
    }

    // ---- what the engine reads --------------------------------------------------------------------------------------

    /**
     * The engine's settings for a pack in a folder, from the names of the files in it: the model, the word list, and one
     * vocabulary for both sides or one for each. Null where a file is missing.
     */
    static String config(String folder,List<String> names) {
        String model=first(names,"model."),lex=first(names,"lex."),both=first(names,"vocab."),src=first(names,"srcvocab."),trg=first(names,"trgvocab.");
        if(src==null)src=both;if(trg==null)trg=both;
        if(model==null||lex==null||src==null||trg==null)return null;
        String at=folder.replace('\\','/')+"/";
        return "models:\n  - "+at+model+"\nvocabs:\n  - "+at+src+"\n  - "+at+trg+"\nshortlist:\n  - "+at+lex+"\n  - false\n"
            +"beam-size: 1\nnormalize: 1.0\nword-penalty: 0\nmax-length-break: 128\nmini-batch-words: 1024\nworkspace: 128\n"
            +"max-length-factor: 2.0\nskip-cost: true\ncpu-threads: 0\nquiet: true\nquiet-translation: true\n"
            +"gemm-precision: int8shiftAlphaAll\nalignment: soft\n";
    }
    private static String first(List<String> names,String start){for(String n:names)if(n.startsWith(start))return n;return null;}

    // ---- which language -----------------------------------------------------------------------------------------------

    /** Common short words of the languages written in Latin letters, to tell them apart. */
    private static final Map<String,List<String>> USUAL=Map.of(
        "en",List.of("the","and","to","of","is","in","it","you","that","for","with","on","we","are","this","be"),
        "fr",List.of("le","la","les","et","de","des","du","un","une","est","que","pour","dans","nous","vous","pas","sur","avec"),
        "es",List.of("el","la","los","las","y","de","del","que","en","un","una","es","por","para","con","no","se","nos"),
        "de",List.of("der","die","das","und","ist","nicht","ein","eine","zu","mit","auf","wir","sie","ich","den","von","für"),
        "pt",List.of("o","a","os","as","e","de","do","da","que","em","um","uma","não","para","com","por","nos","é"),
        "it",List.of("il","lo","la","gli","le","e","di","del","che","in","un","una","è","per","con","non","sono","della"),
        "tr",List.of("ve","bir","bu","da","de","için","ile","çok","ne","ama","gibi","daha","olarak","var","yok","ben","sen","biz"));

    /**
     * A first guess at the language of some words, said in the box and changed there when wrong: by their script where it
     * says (Chinese, Japanese, Hindi, Persian, Russian), and among the languages in Latin letters by their common words.
     * English where nothing tells.
     */
    static String guess(String text) {
        if(text==null||text.isBlank())return "en";
        int han=0,kana=0,devanagari=0,arabic=0,cyrillic=0,latin=0;
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);i+=Character.charCount(c);
            Character.UnicodeScript script;
            try{script=Character.UnicodeScript.of(c);}catch(IllegalArgumentException none){continue;}
            switch(script) {
                case HAN->han++;case HIRAGANA,KATAKANA->kana++;case DEVANAGARI->devanagari++;case ARABIC->arabic++;
                case CYRILLIC->cyrillic++;case LATIN->latin++;default->{}
            }
        }
        if(kana>0&&kana*5>=han)return "ja";
        int most=Math.max(Math.max(han,devanagari),Math.max(arabic,cyrillic));
        if(most>latin) {
            if(most==han)return kana>0?"ja":"zh";
            if(most==devanagari)return "hi";
            if(most==arabic)return "fa";
            return "ru";
        }
        Map<String,Integer> score=new LinkedHashMap<>();
        Matcher words=Pattern.compile("\\p{L}+").matcher(text.toLowerCase(Locale.ROOT));
        while(words.find()) {
            String w=words.group();
            for(Map.Entry<String,List<String>> one:USUAL.entrySet())if(one.getValue().contains(w))score.merge(one.getKey(),1,Integer::sum);
        }
        // Accents only one language uses tip a close count.
        String low=text.toLowerCase(Locale.ROOT);
        if(low.matches("(?s).*[ñ¿¡].*"))score.merge("es",2,Integer::sum);
        if(low.matches("(?s).*[ãõ].*"))score.merge("pt",2,Integer::sum);
        if(low.matches("(?s).*[ßäöü].*"))score.merge("de",2,Integer::sum);
        if(low.matches("(?s).*[èêëçœ].*"))score.merge("fr",1,Integer::sum);
        if(low.matches("(?s).*[ğışİ].*"))score.merge("tr",2,Integer::sum);
        String best="en";int top=0;
        for(Map.Entry<String,Integer> one:score.entrySet())if(one.getValue()>top){best=one.getKey();top=one.getValue();}
        return best;
    }

    // ---- a note's lines, taken apart and put back ---------------------------------------------------------------------

    /** What starts a line of a list, or a checkbox, kept as it is and not translated. */
    private static final Pattern LEAD=Pattern.compile("^(\\s*(?:[-*•]\\s+|\\d+[.)]\\s+|\\[[ xX]\\]\\s+)*)");
    /** Marks around a whole line (bold, italic, underline), kept around its translation. */
    private static final String[] WHOLE={"**","__","*"};

    /** A note's words as the engine takes them, and what is needed to put its answer back line for line. */
    record Prepared(String input,List<String[]> lines){}

    /**
     * Each line with words in it becomes a paragraph of its own for the engine, without what leads a list line and without
     * marks around the whole line; empty lines are kept aside. So the answer comes back one paragraph a line.
     */
    static Prepared prepare(String text) {
        List<String[]> lines=new ArrayList<>();StringBuilder input=new StringBuilder();
        for(String line:(text==null?"":text).split("\n",-1)) {
            String plain=line.replace("\r","");
            if(plain.isBlank()){lines.add(new String[]{plain,null,""});continue;}
            Matcher lead=LEAD.matcher(plain);String before=lead.find()?lead.group(1):"";
            String words=plain.substring(before.length()),mark="";
            for(String m:WHOLE)if(words.length()>2*m.length()&&words.startsWith(m)&&words.endsWith(m)&&!words.startsWith(m+m.charAt(0))){mark=m;words=words.substring(m.length(),words.length()-m.length());break;}
            String trailing=words.substring(words.stripTrailing().length());
            lines.add(new String[]{before,mark,trailing});
            if(input.length()>0)input.append("\n\n");
            input.append(words.strip());
        }
        return new Prepared(input.toString(),lines);
    }

    /** The engine's answer put back into the note's lines: what led each line, its marks, and the empty lines, as they were. */
    static String restore(Prepared prepared,String answer) {
        String[] said=(answer==null?"":answer.replace("\r","")).strip().split("\n\\s*\n");
        StringBuilder out=new StringBuilder();int at=0;
        for(int i=0;i<prepared.lines().size();i++) {
            String[] line=prepared.lines().get(i);
            if(i>0)out.append('\n');
            if(line[1]==null){out.append(line[0]);continue;}
            String words=at<said.length?said[at++].strip().replace("\n"," "):"";
            out.append(line[0]).append(line[1]).append(words).append(line[1]).append(line[2]);
        }
        // More paragraphs than lines (a line the engine split): the rest after the last.
        while(at<said.length)out.append(' ').append(said[at++].strip().replace("\n"," "));
        return out.toString();
    }
}
