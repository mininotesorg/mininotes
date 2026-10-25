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

    /**
     * The PC's own Swing calls: words handed straight to a label, a button, a tip, a title or a box are shown as they are, so
     * English handed to one without Say.t stayed English in every language (found running the PC in French, 2026-10-24). A
     * third rule: every such call on the PC is given Say.t, or a helper that translates, never bare words.
     */
    static final Pattern SWING=Pattern.compile("(?:\\.(setText|setToolTipText|setTitle|drawString|displayMessage|showConfirmDialog|showMessageDialog|showOptionDialog|showInputDialog|createTitledBorder)\\s*\\("
        +"|\\bnew\\s+(JLabel|JButton|JMenuItem|JMenu|JCheckBox|JCheckBoxMenuItem|JRadioButton|JRadioButtonMenuItem|JToggleButton|MenuItem|TrayIcon|JFrame|JDialog|TitledBorder)\\s*\\("
        +"|(PLACEHOLDER_TEXT|\"JTextField\\.placeholderText\")\\s*,)");
    /** Components that translate their own words, and so must be given English: the connection's dot, which picks its colour from it. */
    static final Set<String> TRANSLATE_THEMSELVES=Set.of("Desktop.java connection","DesktopProfile.java state");
    /** Words that are no language's: the name, keys and their shortcuts. */
    static final Set<String> NOT_WORDS=Set.of("Mininotes","  ·  Mininotes"," (Esc)"," (Ctrl+K)","host:port");

    @Test public void pcScreenCallsAreGivenTranslatedWords() throws Exception {
        List<String> bare=new ArrayList<>();
        try(var all=Files.list(ROOT.resolve("windows/src/main/java/org/mininotes/android"))) {
            for(Path source:all.filter(p->p.toString().endsWith(".java")).sorted().toList()) {
                String text=Files.readString(source,StandardCharsets.UTF_8),file=source.getFileName().toString();
                Matcher m=SWING.matcher(text);
                while(m.find()) {
                    int open=m.end();
                    String receiver=receiver(text,m.start());
                    if(receiver!=null&&TRANSLATE_THEMSELVES.contains(file+" "+receiver))continue;
                    String argument=m.group(3)!=null?text.substring(open,Math.max(open,until(text,open,','))):text.substring(open,closing(text,open));
                    for(String words:bareWords(argument))
                        bare.add(file+":"+(text.substring(0,m.start()).split("\n",-1).length)+"  "+m.group().trim()+" "+words);
                }
            }
        }
        assertTrue(bare.size()+" places hand English to the screen without Say.t:\n"+String.join("\n",bare),bare.isEmpty());
    }
    @Test public void theGuardFindsBareWords() {
        assertEquals(List.of("\"Saved\""),bareWords("\"Saved\""));
        assertEquals(List.of("\"Saved\"","\"Not saved\""),bareWords("ok?\"Saved\":\"Not saved\""));
        assertEquals(List.of("\" is in the dock\""),bareWords("Say.t(\"Home\")+\" is in the dock\""));
        assertTrue(bareWords("Say.t(ok?\"Saved\":\"Not saved\")").isEmpty());
        assertTrue(bareWords("Say.t(\"Moved into {0}\",name)+\"  ·  \"").isEmpty());
        assertTrue(bareWords("starting(\"Share a note…\")").isEmpty());
        assertTrue(bareWords("\"<html><div style='width:150px'>\"+Say.t(\"Your code\")+\"</div></html>\"").isEmpty());
        assertTrue(bareWords("\"v\"+VERSION").isEmpty());
    }

    /**
     * A fourth rule (decision 256; the owner, 2026-10-25, a PC set to French: "the error message, Pro could not reach
     * PixelPro is also in english"): the shared classes that build sentences for both apps' screens build each one as a
     * phrase of the lists, Say.t("{0} could not reach {1}", …), never English glued to a name, "…"+who+" could not reach "
     * +…, which no list can say in another language. Words handed to a call (Say.t, an exception, a log) are that call's;
     * words glued to words are one constant. What is left is named here with why.
     */
    static final List<String> SENTENCES=List.of("Unsent","NoteLine","Waits","Looks","Given","Shrink","FirstShare","Drop","Home","Routes",
        "Recording","PrivateSpace","Things","Persons","Linking","Help","Parlons","Called","Sharing","SyncStatus","Sections");
    static final Set<String> GLUED_ON_PURPOSE=Set.of(
        "Unsent.java \"something else (\"",            // a log line's kind of failure, never on a screen
        "Drop.java \"more than \"",                       // a constant, "more than 20 files at once", listed as it is
        "Drop.java \" files at once\"",
        "Recording.java \"Recording \"",                  // a file's name, kept as data
        "Help.java \"geo:\"","Help.java \"https://www.openstreetmap.org/?mlat=\"","Help.java \"&mlon=\"","Help.java \"#map=17/\"",
        "Sharing.java \"the collection \"","Sharing.java \"the note \"","Sharing.java \"the file \"", // an offer as it travels, read by every build
        "Sharing.java \" folder\"",                       // Sharing.shortly: the name in a title the phone puts together itself
        "Sections.java \"Page \"");                       // a tab's name written into the note's own text
    @Test public void sharedSentencesAreNotGluedEnglish() throws Exception {
        List<String> glued=new ArrayList<>();
        for(String name:SENTENCES) {
            String file=name+".java",text=noComments(Files.readString(ROOT.resolve("android/app/src/main/java/org/mininotes/android").resolve(file),StandardCharsets.UTF_8));
            for(String[] one:glued(text))if(!GLUED_ON_PURPOSE.contains(file+" "+one[1]))glued.add(file+":"+one[0]+"  "+one[1]);
        }
        assertTrue(glued.size()+" sentences in shared classes are English glued to a name or a number, which stays English in every language:\n"
            +String.join("\n",glued),glued.isEmpty());
    }
    @Test public void theGuardFindsGluedSentences() {
        assertEquals(List.of("\" could not reach \""),glued("return what+\" could not reach \"+who;").stream().map(one->one[1]).toList());
        assertTrue(glued("return Say.t(\"{0} could not reach {1}\",what,who);").isEmpty());
        assertTrue(glued("static final String A=\"Pictures you add\"\n    +\" are made smaller.\";").isEmpty());
        assertTrue(glued("throw new IllegalArgumentException(\"Too long: \"+n);").isEmpty());
        assertTrue(glued("return where+\" · \"+owed;").isEmpty());
    }

    /** Each literal with words in it glued with + to something that is not another literal, outside any call: line and words. */
    static List<String[]> glued(String text) {
        List<String[]> found=new ArrayList<>();
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='('&&i>0&&(Character.isJavaIdentifierPart(text.charAt(i-1))||text.charAt(i-1)=='>')){i=closing(text,i+1);continue;}
            if(c=='\''){i=literalEnd(text,i)-1;continue;}
            if(c!='"')continue;
            int end=literalEnd(text,i);String said=text.substring(i,end);
            String before=text.substring(0,i).stripTrailing(),after=text.substring(end).stripLeading();
            boolean left=before.endsWith("+")&&!before.substring(0,before.length()-1).stripTrailing().endsWith("\"");
            boolean right=after.startsWith("+")&&!after.substring(1).stripLeading().startsWith("\"");
            if((left||right)&&words(said.substring(1,said.length()-1)))found.add(new String[]{String.valueOf(text.substring(0,i).split("\n",-1).length),said});
            i=end-1;
        }
        return found;
    }
    /** The source without its comments, line for line, so a line number still points at the line. */
    static String noComments(String text) {
        StringBuilder out=new StringBuilder();
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='"'||c=='\''){int end=literalEnd(text,i);out.append(text,i,end);i=end-1;continue;}
            if(text.startsWith("//",i)){int end=text.indexOf('\n',i);if(end<0)end=text.length();i=end-1;continue;}
            if(text.startsWith("/*",i)){int end=text.indexOf("*/",i);end=end<0?text.length():end+2;for(int k=i;k<end;k++)if(text.charAt(k)=='\n')out.append('\n');i=end-1;continue;}
            out.append(c);
        }
        return out.toString();
    }

    /** The words in quotes an argument hands on as they are: not inside Say.t or any other call, nor in a class body. */
    static List<String> bareWords(String argument) {
        List<String> found=new ArrayList<>();
        for(int i=0;i<argument.length();i++) {
            char c=argument.charAt(i);
            if(c=='"'){int end=literalEnd(argument,i);String said=argument.substring(i,end);if(words(said.substring(1,said.length()-1)))found.add(said);i=end-1;continue;}
            if(c=='\''){i=literalEnd(argument,i)-1;continue;}
            // A call's own brackets - Say.t( or any other - and a class body: what is inside is that call's to say.
            if(c=='('&&i>0&&(Character.isJavaIdentifierPart(argument.charAt(i-1))||argument.charAt(i-1)=='>')){i=closing(argument,i+1)-1;continue;}
            if(c=='{'){int depth=1,j=i+1;for(;j<argument.length()&&depth>0;j++){char k=argument.charAt(j);if(k=='"'||k=='\''){j=literalEnd(argument,j)-1;continue;}if(k=='{')depth++;else if(k=='}')depth--;}i=j-1;}
        }
        return found;
    }
    private static boolean words(String said) {
        if(NOT_WORDS.contains(said)||said.matches("\\s*\\(?(Ctrl|Shift|Alt)\\+.*"))return false;
        return said.replaceAll("<[^>]*>","").replaceAll("\\\\u[0-9a-fA-F]{4}","").matches("(?s).*\\p{L}{2}.*");
    }
    /** Where the literal starting at {@code at} ends, its closing quote included. */
    private static int literalEnd(String s,int at) {
        char quote=s.charAt(at);int i=at+1;
        while(i<s.length()&&s.charAt(i)!=quote){if(s.charAt(i)=='\\')i++;i++;}
        return Math.min(s.length(),i+1);
    }
    /** Where the bracket opened just before {@code from} closes. */
    private static int closing(String s,int from) {
        int depth=1,i=from;
        for(;i<s.length();i++){char c=s.charAt(i);if(c=='"'||c=='\''){i=literalEnd(s,i)-1;continue;}if(c=='(')depth++;else if(c==')'&&--depth==0)return i;}
        return i;
    }
    /** The next {@code stop} at this level, or the bracket that closes it. */
    private static int until(String s,int from,char stop) {
        int depth=0,i=from;
        for(;i<s.length();i++){char c=s.charAt(i);if(c=='"'||c=='\''){i=literalEnd(s,i)-1;continue;}if(c=='(')depth++;else if(c==')'){if(depth==0)return i;depth--;}else if(c==stop&&depth==0)return i;}
        return i;
    }
    /** The name before ".setText(" and the like, if there is a plain one. */
    private static String receiver(String text,int dot) {
        if(dot>=text.length()||text.charAt(dot)!='.')return null;
        int start=dot;while(start>0&&Character.isJavaIdentifierPart(text.charAt(start-1)))start--;
        return start<dot?text.substring(start,dot):null;
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
