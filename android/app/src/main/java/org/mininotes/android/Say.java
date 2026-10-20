package org.mininotes.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The app's words in the language chosen (the owner, 2026-10-12: "both, let's do that": the app in several languages, and a
 * note translated on the device; decision 160). Every piece of wording is looked up by its English, as written in the
 * sources, in a flat JSON list per language (words/fr.json and so on, the shape Weblate keeps); what a list does not have is
 * said in English. Looked up where words are put on the screen - a label, a row, a menu line, a button, a box - so the
 * sources stay in English and a symbol is still found from the English words (Symbols).
 *
 * <p>Only a whole phrase is looked up: a note's own title that happens to read like one of the app's words is shown as it
 * is unless it is exactly that phrase. Words built around a name ("Remove Ana") are templates, {@code {0}} for the name.
 * Plain Java: both apps read it.
 */
final class Say {
    private Say(){}

    /** The languages there is a list for, by code, in their own words; English is the sources'. */
    static final Map<String,String> LANGUAGES;
    static {
        Map<String,String> all=new LinkedHashMap<>();
        // Chinese first (the owner, 2026-10-12: "Chinese is the priority"), then the other languages most people read.
        all.put("en","English");all.put("zh","中文 (简体)");all.put("es","Español");all.put("fr","Français");all.put("de","Deutsch");
        all.put("pt","Português");all.put("it","Italiano");all.put("ja","日本語");all.put("ru","Русский");all.put("hi","हिन्दी");all.put("fa","فارسی");
        LANGUAGES=Collections.unmodifiableMap(all);
    }
    /** What a setting keeps for "the device's own language". */
    static final String SYSTEM="system";

    private static volatile Map<String,String> words=Collections.emptyMap();
    /** The list's templates - phrases with {0}, {1}… where a name or a number goes - as patterns, longest first. */
    private static volatile java.util.List<Object[]> templates=Collections.emptyList();
    private static volatile String language="en";

    /** The language in use. */
    static String language(){return language;}
    /** Whether the language in use is written from right to left (Persian), so the screens are laid out mirrored. */
    static boolean rightToLeft(){return "fa".equals(language);}

    /**
     * The language to use for a setting: the device's own where it is {@link #SYSTEM} or empty and there is a list for it,
     * else English.
     */
    static String resolve(String setting,Locale device) {
        String asked=setting==null||setting.isEmpty()||SYSTEM.equals(setting)?(device==null?"en":device.getLanguage()):setting;
        return LANGUAGES.containsKey(asked)?asked:"en";
    }

    /** The list for a language read from where the app keeps them; English, or one that cannot be read, is no list at all. */
    static void use(String code) {
        if(code==null||"en".equals(code)||!LANGUAGES.containsKey(code)){words=Collections.emptyMap();language="en";return;}
        try(InputStream in=Say.class.getResourceAsStream("/org/mininotes/android/words/"+code+".json")) {
            words=in==null?Collections.emptyMap():read(in);language=in==null?"en":code;
        } catch(IOException|RuntimeException unreadable){words=Collections.emptyMap();language="en";}
        templates=templatesOf(words);
    }

    /**
     * Each phrase with {0}, {1}… in it, as a pattern its English is matched against: what stands where a {n} is - a name, a
     * number, a place - is kept as it is and put where the translation has the same {n}. Longest first, so the most said wins.
     */
    static java.util.List<Object[]> templatesOf(Map<String,String> list) {
        java.util.List<Object[]> out=new java.util.ArrayList<>();
        for(Map.Entry<String,String> one:list.entrySet()) {
            String english=one.getKey();
            if(!english.contains("{0}")||one.getValue()==null||one.getValue().isEmpty())continue;
            StringBuilder regex=new StringBuilder("^");java.util.List<Integer> order=new java.util.ArrayList<>();
            java.util.regex.Matcher slot=java.util.regex.Pattern.compile("\\{(\\d)\\}").matcher(english);int at=0;
            while(slot.find()){regex.append(java.util.regex.Pattern.quote(english.substring(at,slot.start())));regex.append("(.+?)");order.add(Integer.parseInt(slot.group(1)));at=slot.end();}
            regex.append(java.util.regex.Pattern.quote(english.substring(at))).append("$");
            out.add(new Object[]{java.util.regex.Pattern.compile(regex.toString(),java.util.regex.Pattern.DOTALL),order,one.getValue(),english.length()});
        }
        out.sort((a,b)->Integer.compare((Integer)b[3],(Integer)a[3]));
        return out;
    }
    /** For tests: a list given as it is. */
    static void use(String code,Map<String,String> list){words=list==null?Collections.emptyMap():new HashMap<>(list);language=code;templates=templatesOf(words);}

    /** These words in the language in use; as they are where there is nothing for them. */
    static String t(String english) {
        if(english==null||english.isEmpty())return english;
        Map<String,String> now=words;
        if(now.isEmpty())return english;
        String said=now.get(english);
        if(said!=null&&!said.isEmpty())return said;
        // "Name…" and "Name" are the same words: the dots say a box comes, and are kept.
        if(english.endsWith("…")){String bare=now.get(english.substring(0,english.length()-1));if(bare!=null&&!bare.isEmpty())return bare+"…";}
        // And a button's arrow, › or ▾ after its words, is not part of them: the words said, the arrow kept (the owner,
        // 2026-10-17: "Not set ›", "The name they share ▾" stayed English).
        for(String arrow:new String[]{"  ›","  ▾"," ›"," ▾"})
            if(english.endsWith(arrow)&&english.length()>arrow.length()){String words=english.substring(0,english.length()-arrow.length());String said2=t(words);if(!said2.equals(words))return said2+arrow;}
        // Built around a name or a number: the template it fits, with what stands in its places kept.
        for(Object[] one:templates) {
            java.util.regex.Matcher fits=((java.util.regex.Pattern)one[0]).matcher(english);
            if(!fits.matches())continue;
            @SuppressWarnings("unchecked") java.util.List<Integer> order=(java.util.List<Integer>)one[1];
            String filled=(String)one[2];
            for(int g=0;g<order.size();g++)filled=filled.replace("{"+order.get(g)+"}",fits.group(g+1));
            return filled;
        }
        return english;
    }

    /** A template's words with what goes in them: {0}, {1}… in the order given. */
    static String t(String english,Object... fill) {
        String said=t(english);
        for(int at=0;at<fill.length;at++)said=said.replace("{"+at+"}",String.valueOf(fill[at]));
        return said;
    }

    /** A flat JSON object of strings, as Weblate writes one: {"English":"Translation",…}. Anything else is refused. */
    static Map<String,String> read(InputStream in) throws IOException {
        ByteArrayOutputStream all=new ByteArrayOutputStream();byte[] chunk=new byte[8192];
        for(int n;(n=in.read(chunk))>0;)all.write(chunk,0,n);
        String s=new String(all.toByteArray(),StandardCharsets.UTF_8);
        Map<String,String> out=new HashMap<>();
        int[] at={0};
        skip(s,at);expect(s,at,'{');skip(s,at);
        if(peek(s,at)=='}')return out;
        while(true) {
            skip(s,at);String key=string(s,at);skip(s,at);expect(s,at,':');skip(s,at);String value=string(s,at);
            out.put(key,value);skip(s,at);
            char c=s.charAt(at[0]++);
            if(c=='}')break;
            if(c!=',')throw new IOException("Not a list of words");
        }
        return out;
    }
    private static char peek(String s,int[] at){return at[0]<s.length()?s.charAt(at[0]):0;}
    private static void skip(String s,int[] at){while(at[0]<s.length()&&Character.isWhitespace(s.charAt(at[0])))at[0]++;}
    private static void expect(String s,int[] at,char c) throws IOException{if(peek(s,at)!=c)throw new IOException("Not a list of words");at[0]++;}
    private static String string(String s,int[] at) throws IOException {
        expect(s,at,'"');StringBuilder b=new StringBuilder();
        while(true) {
            if(at[0]>=s.length())throw new IOException("Not a list of words");
            char c=s.charAt(at[0]++);
            if(c=='"')return b.toString();
            if(c!='\\'){b.append(c);continue;}
            char e=s.charAt(at[0]++);
            switch(e) {
                case 'n':b.append('\n');break;case 't':b.append('\t');break;case 'r':b.append('\r');break;
                case 'b':b.append('\b');break;case 'f':b.append('\f');break;
                case 'u':b.append((char)Integer.parseInt(s.substring(at[0],at[0]+4),16));at[0]+=4;break;
                default:b.append(e);
            }
        }
    }
}
