package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Every word on a screen in every language (the owner, 2026-10-17: "make sure everything is translated ... can you put a
 * rule for that, a structure making sure we don't miss any element?"; decision 186).
 *
 * <p>Two rules, checked at every build of the PC, for both apps:
 * <ol>
 * <li>The English list, words/en.json, is the list of every phrase the apps say. Every other language has every one of
 * them, translated: one missing is a failure, named here.</li>
 * <li>Every phrase the screens are given in the sources, as words or as a sentence built around a name ("Shared with " +
 * name, read as "Shared with {0}"), is on the English list. A new screen whose words are not listed fails until they are,
 * and so cannot reach anybody in English only.</li>
 * </ol>
 * The sources are read as text: a phrase is what is handed, in quotes, to one of the calls that put words on a screen
 * ({@link #CALLS}). A name, a number or a file's name in a sentence is a {0}, as Say.t finds it.
 */
public class WordsEverywhereTest {
    private static final Path ROOT=Path.of("..").toAbsolutePath().normalize();
    private static final Path WORDS=ROOT.resolve("android/app/src/main/resources/org/mininotes/android/words");

    /** The calls that put words on a screen, on either app. */
    static final String CALLS="Say\\.t|IOException|IllegalStateException|IllegalArgumentException|item|heading|label|line|row|tapRow|toast|alert|under|part|setTitle|setMessage|setText|setToolTipText|button|primary|danger|quiet|body|note|spread|setPositiveButton|setNegativeButton|setNeutralButton|dimRow|wakeRow|confirm|confirmOr|ask|busy|busyDone|busySay|plain|switchRow|toggle|section|adds|chip|card|tell|roleShown|person|says|noteSays|failed";
    private static final String LIT="\"(?:[^\"\\\\\\n]|\\\\.)*\"";
    private static final Pattern CALL=Pattern.compile("\\b(?:"+CALLS+")\\s*\\(\\s*((?:"+LIT+"|[^,(){};\"]+|\\([^()]*\\))(?:\\s*\\+\\s*(?:"+LIT+"|[^,(){};\"+]+|\\([^()]*\\)))*)");

    static Map<String,String> list(String code) throws Exception {
        try(var in=Files.newInputStream(WORDS.resolve(code+".json"))){return Say.read(in);}
    }

    @Test public void everyLanguageHasEveryPhrase() throws Exception {
        Set<String> english=list("en").keySet();
        StringBuilder missing=new StringBuilder();int count=0;
        for(String code:Say.LANGUAGES.keySet()) {
            if(code.equals("en"))continue;
            Map<String,String> theirs=list(code);
            for(String phrase:english)if(theirs.getOrDefault(phrase,"").isEmpty()){count++;if(count<=40)missing.append(code).append(": ").append(phrase).append('\n');}
        }
        assertEquals("phrases missing a translation (the first 40):\n"+missing,0,count);
    }

    @Test public void everyPhraseOnAScreenIsListed() throws Exception {
        Set<String> english=list("en").keySet();
        Map<String,String> missing=new TreeMap<>();
        List<Path> sources=new ArrayList<>();
        for(String dir:List.of("android/app/src/main/java/org/mininotes/android","windows/src/main/java/org/mininotes/android"))
            try(var all=Files.list(ROOT.resolve(dir))){all.filter(p->p.toString().endsWith(".java")).forEach(sources::add);}
        for(Path source:sources) {
            String text=Files.readString(source,StandardCharsets.UTF_8);
            Matcher m=CALL.matcher(text);
            // Overlapping, so a call inside another's brackets, button(Say.t("…")), is read too.
            for(int from=0;from<text.length()&&m.find(from);from=m.start()+1) {
                String said=template(m.group(1));
                if(said==null||!onScreen(said)||listed(english,said))continue;
                missing.putIfAbsent(said,source.getFileName().toString());
            }
        }
        StringBuilder all=new StringBuilder();for(var one:missing.entrySet())all.append(one.getKey()).append("   (").append(one.getValue()).append(")\n");
        assertTrue(missing.size()+" phrases on a screen are not on the English list:\n"+all,missing.isEmpty());
    }

    private static boolean listed(Set<String> english,String said) {
        if(english.contains(said))return true;
        // The dots of a box, and an arrow, are added to a listed phrase by Say.t.
        for(String end:List.of("…","  ›","  ▾"))if(said.endsWith(end)&&english.contains(said.substring(0,said.length()-end.length())))return true;
        return false;
    }

    /** What the sources hand a screen, as the phrase Say.t looks for: words in quotes as they are, anything else a {n}. */
    static String template(String expression) {
        Matcher part=Pattern.compile(LIT+"|[^+]+").matcher(expression);
        StringBuilder out=new StringBuilder();int n=0;boolean words=false;
        while(part.find()) {
            String p=part.group().trim();
            if(p.isEmpty())continue;
            if(p.startsWith("\"")){out.append(unescape(p.substring(1,p.length()-1)));words=true;}
            else out.append('{').append(n++).append('}');
        }
        return words?out.toString():null;
    }
    private static String unescape(String s) {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(c!='\\'||i+1>=s.length()){out.append(c);continue;}
            char e=s.charAt(++i);
            switch(e){case 'n'->out.append('\n');case 't'->out.append('\t');case 'u'->{out.append((char)Integer.parseInt(s.substring(i+1,i+5),16));i+=4;}default->out.append(e);}
        }
        return out.toString();
    }
    /** Whether a phrase is words for somebody to read: not a key, a tag, a format or a shortcut. */
    static boolean onScreen(String said) {
        String bare=said.replaceAll("\\{\\d\\}","").trim();
        if(bare.length()<2||!bare.matches("(?s).*\\p{L}{2}.*"))return false;
        if(bare.matches("[a-z][a-zA-Z0-9_.:-]*"))return false;
        if(bare.matches("[A-Z0-9_]+")||bare.startsWith("Mininotes/")||bare.matches("(Ctrl|Shift|Alt)\\+.*"))return false;
        if(bare.equals("Ag\nAg"))return false;
        if(said.startsWith("<"))return false;   // markup around words given elsewhere
        char first=said.charAt(0);
        // Words starting with a small letter are words too where they make a phrase ("from {0}"); a lone small word is a key.
        return Character.isUpperCase(first)||said.startsWith("{")||"“‹+·".indexOf(first)>=0||bare.contains(" ");
    }
}
