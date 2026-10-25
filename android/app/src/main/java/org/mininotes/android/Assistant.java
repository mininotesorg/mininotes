// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Agent, first called Ask Mininotes (the owner, 2026-10-18: "build the agent as much as we can, to answer question but also do tasks like, add a
 * contact, share a note, tell me what is new, tell me what is on my todo list, add this to my todo list"; decision 191).
 *
 * <p>On the device and nothing else: no model, no server, nothing sent. What is asked is read two ways. A task, said in
 * English or French ("share Grocery with Carol", "ajoute du lait à ma liste"), is turned into one of the things the menus
 * already do, with what it is about and who; the app then shows it as a card to confirm, and does it the way its menu
 * does. Anything else is a question, answered from the help kept inside the app (help/en.txt and help/fr.txt, the same
 * files that are on GitHub), by the words it shares with each answer's ways of asking. Holds no Android types: both apps
 * understand the same way.
 */
final class Assistant {
    private Assistant(){}

    /** What can be asked to be done. HELP is a question; NOTHING is an empty line. */
    enum Act { HELP, WHAT_NEW, TODO_SHOW, TODO_ADD, SHARE, ADD_CONTACT, OPEN, FIND, NEW_NOTE, DOCK, FAVOURITE, NOTHING }

    /**
     * What was understood: the act, the words naming the thing it is about (or what to add, find, or call a new note),
     * and the words naming who, where the act has somebody - or, for a line to add, the note it goes in, empty for the
     * to-do list. Read only, so each part can be said back.
     */
    record Asked(Act act,String what,String who,boolean readOnly) {
        static Asked of(Act act){return new Asked(act,"","",false);}
    }

    // ---- reading what was asked --------------------------------------------------------------------------------------

    /** Lower case, no accents, one space between words, no punctuation at the ends: what every rule is matched against. */
    static String plain(String said) {
        if(said==null)return "";
        String flat=Normalizer.normalize(said,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        flat=flat.replace('’','\'').replaceAll("[\\s]+"," ").trim();
        return flat.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$","");
    }

    private static Pattern p(String regex){return Pattern.compile(regex);}

    private static final String TODO_WORDS="(?:todo|to do|to-do|todo list|to do list|to-do list|task list|tasks|liste de taches|liste des taches|taches|liste a faire|a faire|ma liste|my list|list)";
    private static final Pattern WHAT_NEW=p("^(?:what'?s|what is|anything|is there anything|show me what'?s|tell me what'?s|tell me what is|quoi de|qu'est-ce qu'il y a de|qu'y a-t-il de|quelles sont les)? ?(?:new|neuf|nouveau|nouveautes|news)(?: here| for me| today| pour moi| aujourd'hui)?\\??$");
    private static final Pattern WHAT_NEW_ALSO=p("^(?:what changed|what has changed|qu'est-ce qui a change|quoi de neuf|du nouveau)( .*)?$");
    private static final Pattern TODO_ADD=p("^(?:please |s'il te plait )?(?:add|put|write|note|ajoute|ajouter|mets|mettre|ecris|note) (.+?) (?:to|on|in|into|a|au|dans|sur) (?:my |the |ma |la |mes |les )?"+TODO_WORDS+"$");
    private static final Pattern TODO_COLON=p("^(?:todo|to do|a faire|tache)\\s*[:\\-]\\s*(.+)$");
    private static final Pattern TODO_SHOW=p("^(?:(?:show|tell|read|give)(?: me)?|what'?s|what is|what do i have|montre(?:-moi)?|affiche|lis|dis-moi|qu'est-ce qu'il y a|qu'y a-t-il|quoi)? ?(?:on|in|sur|dans)? ?(?:my |the |ma |la |mes )?"+TODO_WORDS+"\\??$");
    private static final Pattern SHARE=p("^(?:please )?(?:share|send|give|partage|partager|envoie|envoyer|donne) (.+?) (?:with|to|avec|a) (.+?)(?: (?:as |en )?(read only|reader|can read|to read|en lecture|lecture seule|lecture|can write|writer|en ecriture|ecriture))?$");
    private static final Pattern ADD_CONTACT=p("^(?:add|connect|link|ajoute|ajouter|connecte|connecter|relie) (?:a |an |un |une |new |nouveau |nouvelle |someone|somebody|quelqu'un)?(?:new )?(?:contact|person|people|someone|somebody|friend|device|personne|ami|amie|appareil|quelqu'un)(?: .*)?$");
    private static final Pattern ADD_CONTACT_ALSO=p("^(?:how (?:do i|can i|to) )?(?:add|ajouter|ajoute) (?:someone|somebody|quelqu'un)\\??$");
    private static final Pattern DOCK=p("^(?:add|put|pin|ajoute|ajouter|mets|mettre|epingle) (.+?) (?:to|in|on|into|au|dans le|sur le|dans) (?:the |my |le |mon )?dock$");
    private static final Pattern FAVOURITE=p("^(?:add|put|make|ajoute|ajouter|mets|mettre) (.+?) (?:to|in|into|a|aux|dans|dans les|en) (?:my |the |mes |les )?(?:favourites|favorites|favoris|favorite|favourite)$");
    private static final Pattern TO_NOTE=p("^(?:please )?(?:add|put|write|ajoute|ajouter|mets|mettre|ecris) (.+?) (?:to|on|in|into|a|au|dans|sur) (?:my |the |ma |la |mon |le )?(?:note )?(.+)$");
    private static final Pattern NEW_NOTE=p("^(?:new note|create a note|create note|make a note|start a note|write a note|nouvelle note|cree une note|creer une note|faire une note|ecrire une note)(?: (?:called|named|titled|about|appelee|nommee|intitulee|sur|pour))? ?(.*)$");
    private static final Pattern OPEN=p("^(?:open|show me|go to|ouvre|ouvrir|va a|aller a|montre-moi|montre moi) (?:the |my |le |la |les |mon |ma |mes )?(.+)$");
    private static final Pattern FIND=p("^(?:find|search|search for|look for|where is|where'?s|cherche|chercher|trouve|trouver|recherche|ou est|ou se trouve) (?:the |my |le |la |les |mon |ma |mes )?(.+?)\\??$");

    /** What a line asks, by the rules above, in that order; a question where none fits. */
    static Asked understand(String said) {
        String s=plain(said);
        if(s.isEmpty())return Asked.of(Act.NOTHING);
        Matcher m;
        if(WHAT_NEW.matcher(s).matches()||WHAT_NEW_ALSO.matcher(s).matches())return Asked.of(Act.WHAT_NEW);
        if((m=TODO_ADD.matcher(s)).matches())return new Asked(Act.TODO_ADD,original(said,m.group(1)),"",false);
        if((m=TODO_COLON.matcher(s)).matches())return new Asked(Act.TODO_ADD,original(said,m.group(1)),"",false);
        if(TODO_SHOW.matcher(s).matches()&&s.matches(".*"+TODO_WORDS+".*"))return Asked.of(Act.TODO_SHOW);
        if((m=DOCK.matcher(s)).matches())return new Asked(Act.DOCK,original(said,m.group(1)),"",false);
        if((m=FAVOURITE.matcher(s)).matches())return new Asked(Act.FAVOURITE,original(said,m.group(1)),"",false);
        if(ADD_CONTACT.matcher(s).matches()||ADD_CONTACT_ALSO.matcher(s).matches())return Asked.of(Act.ADD_CONTACT);
        // A line for another note than the to-do list: the note named last, what to add before it (who holds its name).
        if((m=TO_NOTE.matcher(s)).matches()&&!looksLikeAQuestion(s))return new Asked(Act.TODO_ADD,original(said,m.group(1)),original(said,m.group(2)),false);
        if((m=SHARE.matcher(s)).matches()) {
            String level=m.group(3)==null?"":m.group(3);
            boolean read=level.contains("read")||level.contains("lecture");
            return new Asked(Act.SHARE,original(said,m.group(1)),original(said,m.group(2)),read);
        }
        if((m=NEW_NOTE.matcher(s)).matches())return new Asked(Act.NEW_NOTE,original(said,m.group(1)),"",false);
        if((m=FIND.matcher(s)).matches()&&!looksLikeAQuestion(s))return new Asked(Act.FIND,original(said,m.group(1)),"",false);
        if((m=OPEN.matcher(s)).matches()&&!looksLikeAQuestion(s))return new Asked(Act.OPEN,original(said,m.group(1)),"",false);
        return new Asked(Act.HELP,said.trim(),"",false);
    }

    /** "How do I open…", "where is the dock" are questions about the app, not a thing to open or look for. */
    private static boolean looksLikeAQuestion(String s) {
        return s.startsWith("how ")||s.startsWith("comment ")||s.contains(" the dock")||s.contains(" le dock")||s.endsWith(" menu")
            ||s.contains("setting")||s.contains("parametre")||s.contains("reglage");
    }

    /**
     * The words the user typed for a part, as they typed them - capitals and accents kept - found by where the plain form of
     * that part sits in the plain form of the whole. What goes into a note is theirs, not a lowered copy of it.
     */
    static String original(String said,String plainPart) {
        if(said==null||plainPart==null||plainPart.isEmpty())return plainPart==null?"":plainPart;
        String typed=said.trim();
        // Each typed letter's plain form, one by one, so a position in the plain line is a position in what was typed.
        StringBuilder flat=new StringBuilder();List<Integer> from=new ArrayList<>();
        for(int i=0;i<typed.length();i++) {
            String one=Normalizer.normalize(String.valueOf(typed.charAt(i)),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
            if(one.equals("’"))one="'";
            for(int k=0;k<one.length();k++){flat.append(one.charAt(k));from.add(i);}
        }
        String squeezed=flat.toString().replaceAll("\\s"," ");
        int at=squeezed.indexOf(plainPart);
        if(at<0)return plainPart;
        int start=from.get(at),end=from.get(Math.min(from.size()-1,at+plainPart.length()-1))+1;
        return typed.substring(start,end);
    }

    // ---- finding what was named ------------------------------------------------------------------------------------

    /**
     * The one that best answers to the words naming it: the same name, then a name starting with them, then one holding
     * them, the shorter first; null where none does. Names are compared plain, so "epicerie" finds "Épicerie".
     */
    static <T> T best(String asked,List<T> among,java.util.function.Function<T,String> name) {
        String want=plain(asked);
        if(want.isEmpty())return null;
        T found=null;int score=0,length=Integer.MAX_VALUE;
        for(T one:among) {
            String called=plain(name.apply(one));
            if(called.isEmpty())continue;
            int s=called.equals(want)?4:called.startsWith(want)?3:called.contains(want)?2:want.contains(called)&&called.length()>=3?1:0;
            if(s>score||s==score&&s>0&&called.length()<length){found=one;score=s;length=called.length();}
        }
        return score>0?found:null;
    }

    /** The plain titles a to-do note goes by, in the languages the app reads tasks in. */
    static final Set<String> TODO_TITLES=Set.of("todo","to do","to-do","todo list","to do list","to-do list","tasks","task list","my tasks",
        "a faire","liste a faire","choses a faire","taches","mes taches","liste de taches","liste des taches",
        // And "Todo" as the other languages say it, the title a note made here in them is given.
        "tareas","da fare","tarefas","дела","待办","やること","कार्य","کارها");

    /** Whether a title is a to-do list's. */
    static boolean isTodo(String title) {
        // Both sides made plain the same way, so a title in a script whose signs plain() leaves out is still found.
        String said=plain(title);
        for(String one:TODO_TITLES)if(plain(one).equals(said))return true;
        return false;
    }

    /** The lines of a to-do note, as a list says them: each non-empty line, a bullet or a box at its start taken away. */
    static List<String> lines(String body) {
        List<String> out=new ArrayList<>();
        if(body==null)return out;
        for(String line:body.split("\n")) {
            String one=line.replaceFirst("^\\s*(?:[-*•·]|\\[[ xX]?\\]|☐|☑|✓|✔|\\d+[.)])\\s*","").trim();
            if(!one.isEmpty())out.add(one);
        }
        return out;
    }

    /** The body with one more line at its end, as the note's own lines are written: a "- " where they start with one. */
    static String withLine(String body,String line) {
        String was=body==null?"":body;
        boolean dashed=false;for(String l:was.split("\n"))dashed|=l.startsWith("- ");
        String dash=dashed?"- ":"";
        String end=was.isEmpty()||was.endsWith("\n")?"":"\n";
        return was+end+dash+line.trim();
    }

    // ---- the help kept inside the app ------------------------------------------------------------------------------

    /**
     * One answer: what it is about, the ways of asking it, the answer, what it can do at once, if anything, and the places in
     * the app it is about, for its Take me there (decision 213).
     */
    record Help(String id,String title,List<String> asks,String answer,Act act,List<Place> places) {
        Help(String id,String title,List<String> asks,String answer,Act act){this(id,title,asks,answer,act,List.of());}
    }

    private static final Map<String,List<Help>> KEPT=new HashMap<>();

    /** The help for a language, English where there is none in it. Read once. */
    static synchronized List<Help> help(String language) {
        String code=language==null?"en":language;
        List<Help> kept=KEPT.get(code);
        if(kept!=null)return kept;
        List<Help> read=readHelp(code);
        if(read.isEmpty()&&!"en".equals(code))read=help("en");
        KEPT.put(code,read);
        return read;
    }

    /**
     * The help file read: entries one after another, a line "# id" (one word) starting each, any other "#" line a comment, "= title", "? a way of asking" as many as
     * it has, "! the answer" (several "!" lines are one answer, a paragraph each), "> ACT" for what it can do at once, and
     * "@ PLACE" for each place in the app it is about, the likeliest first (decision 213).
     */
    static List<Help> readHelp(String code) {
        try(InputStream in=Assistant.class.getResourceAsStream("/org/mininotes/android/help/"+code+".txt")) {
            if(in==null)return Collections.emptyList();
            return parse(new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)));
        } catch(IOException|RuntimeException unreadable){return Collections.emptyList();}
    }

    static List<Help> parse(BufferedReader lines) throws IOException {
        List<Help> out=new ArrayList<>();
        String id=null,title="";List<String> asks=new ArrayList<>();StringBuilder answer=new StringBuilder();Act act=null;List<Place> places=new ArrayList<>();
        for(String line;(line=lines.readLine())!=null;) {
            String t=line.trim();
            if(t.startsWith("#")&&!t.matches("# [a-z0-9-]+"))continue;   // a comment
            if(t.startsWith("# ")) {
                if(id!=null)out.add(new Help(id,title,List.copyOf(asks),answer.toString().trim(),act,List.copyOf(places)));
                id=t.substring(2).trim();title="";asks.clear();answer.setLength(0);act=null;places.clear();
            } else if(t.startsWith("= "))title=t.substring(2).trim();
            else if(t.startsWith("? "))asks.add(t.substring(2).trim());
            else if(t.startsWith("! ")){if(answer.length()>0)answer.append("\n\n");answer.append(t.substring(2).trim());}
            else if(t.startsWith("> ")){try{act=Act.valueOf(t.substring(2).trim());}catch(IllegalArgumentException unknown){act=null;}}
            else if(t.startsWith("@ ")){try{places.add(Place.valueOf(t.substring(2).trim()));}catch(IllegalArgumentException unknown){/* a place this version does not have */}}
        }
        if(id!=null)out.add(new Help(id,title,List.copyOf(asks),answer.toString().trim(),act,List.copyOf(places)));
        return out;
    }

    /** Words too common to say what a question is about, in English and French. */
    private static final Set<String> SMALL=new HashSet<>(List.of(("a an the of to in on at for with and or is are be it this that my me i you your "
        +"do does did how what why when where which who can could should would will there here from by as into about "
        +"le la les un une des du de d l et ou est sont ce cette mon ma mes je j me moi tu vous votre vos il elle on "
        +"faire fait comment quoi pourquoi quand ou qui que qu est-ce peut peux puis-je dans sur pour avec par en au aux y a").split(" ")));

    /** The words of a line that say what it is about: plain, small words left out, a plural's s too. */
    static List<String> words(String line) {
        List<String> out=new ArrayList<>();
        for(String w:plain(line).split("[^a-z0-9]+")) {
            if(w.length()<2||SMALL.contains(w))continue;
            out.add(w.length()>4&&w.endsWith("s")?w.substring(0,w.length()-1):w);
        }
        return out;
    }

    /**
     * The answers that fit a question best, the best first: each scored by the words it shares with its ways of asking, which
     * count most, its title, and its answer; none that shares nothing.
     */
    static List<Help> lookUp(String question,List<Help> all,int most) {
        List<String> asked=words(question);
        if(asked.isEmpty())return List.of();
        List<Object[]> scored=new ArrayList<>();
        for(Help one:all) {
            double best=0;
            for(String ask:one.asks()) {
                List<String> w=words(ask);
                if(w.isEmpty())continue;
                int shared=0;for(String x:asked)if(w.contains(x))shared++;
                // How much of the question it covers, and how much of the way of asking the question covers.
                double s=shared==0?0:3.0*shared/asked.size()+1.0*shared/w.size();
                best=Math.max(best,s);
            }
            List<String> title=words(one.title()),answer=words(one.answer());
            for(String x:asked){if(title.contains(x))best+=0.8;if(answer.contains(x))best+=0.25;}
            if(best>0.5)scored.add(new Object[]{best,one});
        }
        scored.sort((x,y)->Double.compare((Double)y[0],(Double)x[0]));
        List<Help> out=new ArrayList<>();
        for(Object[] one:scored){if(out.size()>=most)break;out.add((Help)one[1]);}
        return out;
    }

    // ---- doing it, the way the menus do: both apps call these off the screen's thread ------------------------------

    /** The note or folder the words name, among everything in the notebook; null where nothing answers to them. */
    static NoteStore.Branch thing(NoteStore store,String words) {
        List<NoteStore.Branch> all=new ArrayList<>();
        for(NoteStore.Branch one:store.wholeTree())if(one.kind==NoteStore.Branch.Kind.PAGE||one.kind==NoteStore.Branch.Kind.COLLECTION)all.add(one);
        String want=words==null?"":words.replaceAll("(?i)\s+(?:note|folder|dossier)$","").replaceAll("(?i)^(?:note|folder|dossier)\s+","");
        return best(want,all,b->b.name);
    }

    /** The paired person or device of mine the words name; null where nobody answers to them. */
    static NoteStore.Contact person(NoteStore store,String words) {
        List<NoteStore.Contact> all=new ArrayList<>();
        for(NoteStore.Contact one:store.addresses())if(one.paired())all.add(one);
        return best(words,all,c->store.nameFor(c.address));
    }

    /** Everybody who can be shared with, by name, for when the words named nobody. */
    static List<String> people(NoteStore store) {
        List<String> out=new ArrayList<>();
        for(NoteStore.Contact one:store.addresses())if(one.paired())out.add(store.nameFor(one.address));
        return out;
    }

    /** The to-do note: the first note whose title is a to-do list's; null where there is none. */
    static NoteStore.Note todo(NoteStore store) {
        for(NoteStore.Branch one:store.wholeTree())if(one.kind==NoteStore.Branch.Kind.PAGE&&isTodo(one.name))return store.get(one.id);
        return null;
    }

    /** Whether this device may only read a note: somebody else's, given to read. */
    static boolean readOnly(NoteStore.Note note){return note!=null&&note.theirs&&!note.writes;}

    /** A line written at the end of a note, by this owner, as a page writes: the note to send after. */
    static NoteStore.Note addLine(NoteStore store,String id,String line) {
        NoteStore.Note note=store.get(id);
        if(note==null)throw new IllegalStateException(Say.t("That note is not here any more."));
        if(readOnly(note))throw new IllegalStateException(Say.t("You can only read “{0}”.",note.heading()));
        note.body=withLine(note.body,line);note.revision++;note.updated=System.currentTimeMillis();note.by=Writers.ME;
        store.save(note);
        return note;
    }

    /** A note made on Home with this title and these words: the to-do list, or a new note asked for. */
    static NoteStore.Note make(NoteStore store,String title,String body) {
        NoteStore.Note note=new NoteStore.Note();note.book=Things.HOME;note.title=title==null?"":title.trim();note.body=body==null?"":body;note.by=Writers.ME;
        store.save(note);
        return note;
    }

    /**
     * Somebody given a note or a folder, to write in or to read, as the Share box gives it, and refused where this device may
     * not give it. The kind of thing to send after it.
     */
    static void share(NoteStore store,NoteStore.Branch thing,NoteStore.Contact who,boolean read) {
        Sharing.Scope scope=thing.kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:Sharing.Scope.THING;
        store.give(scope,thing.id,who.address,read?Sharing.Level.READ:Sharing.Level.WRITE,null);
    }

    // ---- what is said back: the same words on both apps ------------------------------------------------------------

    static String notFound(String words){return Say.t("I did not find a note or a folder called “{0}”.",words);}
    static String nobody(String words,List<String> people) {
        return people.isEmpty()?Say.t("You have nobody to share with yet. Add someone first.")
            :Say.t("I do not know anybody called “{0}”. Your people: {1}.",words,String.join(", ",people));
    }
    static String shareAsk(String thing,String who,boolean read) {
        return read?Say.t("Share “{0}” with {1}? They can read it.",thing,who):Say.t("Share “{0}” with {1}? They can write in it.",thing,who);
    }
    static String shared(String who){return Say.t("Shared with {0}. They are asked to accept it.",who);}
    static String addAsk(String line,String note){return Say.t("Add “{0}” to {1}?",line,note);}
    static String added(String note){return Say.t("Added to {0}.",note);}
    static String noTodo(){return Say.t("You have no todo list yet: a note called Todo. Shall I make it?");}

    /** What it is, said at the top of its box on both apps (decision 192). */
    static final String WHAT="I answer your questions about Mininotes, and I do things for you: share a note, add to your todo list, tell you what is new.";
    static final String PRIVATE_PC="Private: everything stays in Mininotes on this PC. Nothing you type here is sent anywhere.";
    static final String PRIVATE_PHONE="Private: everything stays in Mininotes on this phone. Nothing you type here is sent anywhere.";

    /** What is offered under the field before anything is asked, and again after each answer. */
    static final String[] SUGGESTED={"What is new?","What is on my todo list?","Add to my todo list…","Share a note…","Add someone","How do I use the dock?"};

    // ---- Take me there: the place an answer is about, opened (decision 213) -----------------------------------------

    /**
     * The places in the app an answer can take you to (the owner, 2026-10-19: "the agent should propose: do you want me to
     * bring you there. So for example in setting Integritas it will open directly the section in the profile page"; decision
     * 213). Each with the words its screen calls it by, and the plain words it is asked for by, in English and French. Each
     * app opens what it has of them; one an app does not have offers no button there.
     */
    enum Place {
        PROFILE("My profile","profile","my profile","profil","mon profil"),
        INTEGRITAS("Stamping","stamping","integritas","api key","integritas key","cle api","cle integritas","horodatage"),
        PARLONS("Your Parlons! address","parlons","parlons address","parlons! address","adresse parlons"),
        TEXT_SIZE("Text size","text size","size of the text","taille du texte"),
        DESCRIBE("Describe what each thing does","describe","descriptions","description","tooltips","decrire"),
        PEOPLE("People and devices","people","people and devices","contacts","groups","my devices","personnes","personnes et appareils","groupes","mes appareils"),
        SECURITY("Security","security","lock","password","encryption","securite","verrouillage","mot de passe","chiffrement"),
        BACKUP("Backup","backup","backups","sauvegarde","sauvegardes"),
        LANGUAGE("Language","language","langue"),
        // Temp's settings: how long things stay in it, and where they go. "Open Temp" opens Temp itself, a place on Home.
        TEMP("Temp"),
        SHOW_ON_HOME("Show on Home","show on home","afficher sur l'accueil"),
        STAMPED("Stamped on Minima…","stamped","stamped on minima","my stamps","stamps","verify","horodate","horodatages","verifier"),
        SENT_FILES("Sent files…","sent files","files sent","transfers","fichiers envoyes"),
        BIN("Bin","bin","the bin","trash","deleted","corbeille","poubelle"),
        ARCHIVE("Archive","archive","archives","archived"),
        SHARED_WITH_ME("Shared with me","shared with me","invitations","partage avec moi"),
        SETTINGS("Settings","settings","reglages","parametres");

        /** What its screen calls it, in English: said in the language in use by Say.t. */
        final String named;
        /** The plain words it is asked for by. */
        final List<String> called;
        Place(String named,String... called){this.named=named;this.called=List.of(called);}
        /** Its name as said here, without the … of a menu entry. */
        String said(){return Say.t(named).replace("…","");}
    }

    /**
     * The place an answer takes you to: of those it is about, the first the question names, else its likeliest; null for an
     * answer about no place. "Where is the bin" is the archive's answer, and goes to the bin.
     */
    static Place place(Help one,String question) {
        if(one==null||one.places().isEmpty())return null;
        String asked=" "+plain(question).replaceAll("[^\\p{L}\\p{N}' ]+"," ").replaceAll(" +"," ")+" ";
        for(Place p:one.places())for(String w:p.called)if(asked.contains(" "+w+" "))return p;
        return one.places().get(0);
    }

    /** The place the words name, as "open the bin" names it; null where they name none. */
    static Place placeNamed(String words) {
        String want=plain(words).replaceFirst("^(?:the|my|le|la|les|mon|ma|mes) ","");
        if(want.isEmpty())return null;
        for(Place p:Place.values()) {
            if(p.called.contains(want))return p;
            if(p.called.isEmpty())continue;
            String named=plain(p.named),here=plain(Say.t(p.named));
            if(want.equals(named)||want.equals(here))return p;
        }
        return null;
    }

    // ---- the conversation, kept a few hours (decision 213) ----------------------------------------------------------

    /**
     * How long a conversation is kept after its last message (the owner, 2026-10-19: "we have to make sure the agent keeps
     * the conversation for a couple of hours at least"; decision 213): three hours, through closing the window and starting
     * the app again; then it starts afresh.
     */
    static final long KEEP=3*60*60_000L;
    /** Where it is kept: among the notebook's secrets, on this device, locked with the notebook, never sent nor backed up. */
    static final String KEPT_AS="agent.conversation";
    /** The most exchanges kept: the oldest go first. */
    static final int MOST=150;

    /** What a line of the conversation is: what you asked, an answer's title, its words, a quieter line, a place to go to. */
    enum Kind { YOU, TITLE, SAY, QUIET, THERE }
    record Line(Kind kind,String words) {}

    /**
     * The conversation as it was said: each exchange the lines it showed, in order. The buttons of a task that was waiting
     * are not kept, only what was said and the places offered: a task is never done again by opening the window.
     */
    static final class Conversation {
        final List<List<Line>> turns=new ArrayList<>();
        /** When the last message was, in milliseconds; 0 for none. */
        long last;

        /** A new exchange, starting with what was asked, if it was asked rather than chosen from an answer. */
        List<Line> start(String asked,long now) {
            List<Line> turn=new ArrayList<>();
            if(asked!=null&&!asked.isBlank())turn.add(new Line(Kind.YOU,asked));
            turns.add(turn);last=now;
            while(turns.size()>MOST)turns.remove(0);
            return turn;
        }
        void add(List<Line> turn,Kind kind,String words,long now){if(turn!=null&&words!=null){turn.add(new Line(kind,words));last=now;}}
        void clear(){turns.clear();last=0;}
        boolean isEmpty(){for(List<Line> one:turns)if(!one.isEmpty())return false;return true;}
        /** Whether it is over: its last message longer ago than it is kept. */
        boolean over(long now){return last>0&&now-last>KEEP;}
        /** Those kept before, put first: what was said before this window opened. */
        void before(Conversation earlier) {
            if(earlier==null||earlier.isEmpty())return;
            turns.addAll(0,earlier.turns);last=Math.max(last,earlier.last);
            while(turns.size()>MOST)turns.remove(0);
        }

        /** Written as lines: "at" and when, then for each exchange a "-" and its lines, each its kind and its words. */
        String write() {
            StringBuilder out=new StringBuilder("mininotes agent 1\nat\t").append(last).append('\n');
            for(List<Line> turn:turns) {
                if(turn.isEmpty())continue;
                out.append("-\n");
                for(Line one:turn)out.append(one.kind().name()).append('\t').append(escape(one.words())).append('\n');
            }
            return out.toString();
        }

        /** What was kept, read back; empty where nothing was, where it cannot be read, or where it is over. */
        static Conversation read(String kept,long now) {
            Conversation c=new Conversation();
            if(kept==null||!kept.startsWith("mininotes agent 1\n"))return c;
            List<Line> turn=null;
            for(String line:kept.split("\n")) {
                if(line.equals("-")){turn=new ArrayList<>();c.turns.add(turn);continue;}
                int tab=line.indexOf('\t');if(tab<0)continue;
                String kind=line.substring(0,tab),words=unescape(line.substring(tab+1));
                if(kind.equals("at")){try{c.last=Long.parseLong(words);}catch(NumberFormatException none){c.last=0;}continue;}
                if(turn==null)continue;
                try{turn.add(new Line(Kind.valueOf(kind),words));}catch(IllegalArgumentException unknown){/* a kind of a later version */}
            }
            if(c.last<=0||c.over(now)||c.last>now+60_000L){c.clear();}
            return c;
        }
        private static String escape(String s){return s.replace("\\","\\\\").replace("\n","\\n").replace("\t","\\t").replace("\r","");}
        private static String unescape(String s) {
            StringBuilder out=new StringBuilder();
            for(int i=0;i<s.length();i++) {
                char ch=s.charAt(i);
                if(ch=='\\'&&i+1<s.length()){char next=s.charAt(++i);out.append(next=='n'?'\n':next=='t'?'\t':next);}
                else out.append(ch);
            }
            return out.toString();
        }
    }

    /** The words of the window's own buttons (decision 213). */
    static final String THERE="Take me there",AFRESH="New conversation",FULL="Full screen",SMALLER="Exit full screen";
    static final String KEPT_SAID="Forget this conversation. It is kept on this device for 3 hours after the last message.";
}
