// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import com.eurobuddha.maxima.core.MaximaNode;
import com.eurobuddha.maxima.core.MaximaSender;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Notes actually going somewhere, and notes actually arriving.
 *
 * <p>Two layers, and they are not the same thing. Maxima carries the bytes and knows nothing of what is in
 * them. Inside that, every note is sealed for one recipient with {@link Envelope} — agreement, a key nobody
 * else can derive, and a signature that says who wrote it. A relay handles the envelope; it does not open
 * it, and could not.
 *
 * <p><b>A mark is cleared by the far phone saying it has the note, and by nothing else.</b> That rule is
 * the whole reason the marks are worth looking at. For a long time it was cleared when the network said it
 * had taken the message, which is a different thing: the network will take a message for a phone that is
 * asleep, switched off, or no longer taking that note. So a note that goes asks to be answered, the phone
 * that gets it answers once it is written down — see {@link Receipt} — and what is never answered is sent
 * again, a minute later and then less and less often, for as long as it takes.
 */
final class Post {
    /** The Maxima application string this app owns. Its own traffic, nobody else's. */
    static final String APPLICATION="mininotes.v1";

    /** How long an answer is given before the same note may be handed to the network again. */
    private static final long JUST_NOW=20_000L;

    /** What one send did, in words a person can be shown. */
    static final class Done {
        final int sent, failed;
        final String why;
        /** Why what did not go did not, by device and thing, to be said with what to do about it: see Unsent. */
        final List<Unsent.Problem> problems;
        Done(int sent,int failed,String why){this(sent,failed,why,new ArrayList<>());}
        Done(int sent,int failed,String why,List<Unsent.Problem> problems){this.sent=sent;this.failed=failed;this.why=why;this.problems=problems;}
    }

    /** What this device is called in a sentence about it. */
    static String here(){return Node.A_PC?"this PC":"this phone";}

    private Post(){}

    /** What one arriving message turned out to be. */
    static final class Landed {
        /** What to tell the reader, or null where there is nothing to say. */
        final String said;
        /** Somebody taking up an offer, which is a question for the reader rather than a note. */
        final Hello.Said accepted;
        /**
         * Which note changed, or empty where none did. The page that is open has to know when it is the one:
         * it is holding the text as it was, and the next word typed would write that back over what arrived.
         */
        final String note;
        /** Somebody said they have something. Nothing to tell the reader; the marks want drawing again. */
        final boolean answered;
        /** Who may do what can have changed, though no word of the note did. A box showing it is drawn again. */
        final boolean people;
        /**
         * Something about files sent or received on their own (see {@link Drop}): the list of them wants drawing
         * again. {@code asking} is a sending somebody who is not one of the owner's devices wants to make, to be
         * put to the person, by its id here; null where there is nothing to ask.
         */
        final boolean files; final String asking;
        Landed(String said,Hello.Said accepted){this(said,accepted,"");}
        Landed(String said,Hello.Said accepted,String note){this(said,accepted,note,false);}
        Landed(String said,Hello.Said accepted,String note,boolean answered){this(said,accepted,note,answered,false);}
        private Landed(String said,Hello.Said accepted,String note,boolean answered,boolean people) {
            this(said,accepted,note,answered,people,false,null);
        }
        private Landed(String said,Hello.Said accepted,String note,boolean answered,boolean people,boolean files,String asking) {
            this(said,accepted,note,answered,people,files,asking,false);
        }
        private Landed(String said,Hello.Said accepted,String note,boolean answered,boolean people,boolean files,String asking,boolean devices) {
            this(said,accepted,note,answered,people,files,asking,devices,false);
        }
        private Landed(String said,Hello.Said accepted,String note,boolean answered,boolean people,boolean files,String asking,boolean devices,boolean groups) {
            this(said,accepted,note,answered,people,files,asking,devices,groups,false);
        }
        private Landed(String said,Hello.Said accepted,String note,boolean answered,boolean people,boolean files,String asking,boolean devices,boolean groups,
                       boolean contacts) {
            this.said=said;this.accepted=accepted;this.note=note==null?"":note;this.answered=answered;
            this.people=people;this.files=files;this.asking=asking;this.devices=devices;this.groups=groups;this.contacts=contacts;
        }
        /**
         * What your own devices are called, or your name, changed on a card (see {@link Persons}). A People and devices
         * box drawn before it shows the old names until it is drawn again, which is what this asks for.
         */
        final boolean devices;
        static Landed devices(){return new Landed(null,null,"",true,true,false,null,true);}
        /**
         * Your groups changed on a card from another device of yours (see {@link Groups}): a People and devices box drawn
         * before it shows the groups as they were, and a share box who had what, until each is drawn again.
         */
        final boolean groups;
        static Landed groups(){return new Landed(null,null,"",true,true,false,null,false,true);}
        /**
         * Somebody's Parlons! address changed on a card from another device of yours (see {@link Parlons}): a People and
         * devices box drawn before it shows the old one until it is drawn again.
         */
        final boolean contacts;
        static Landed contacts(){return new Landed(null,null,"",true,true,false,null,false,false,true);}
        /**
         * Stamps on Minima came from another device of yours, or learned more (decision 252): every line, box and icon showing
         * them is drawn again, and those still waiting are looked for here too where this device has an Integritas key.
         * {@code stamped} holds their fingerprints.
         */
        boolean stamps; java.util.Set<String> stamped=java.util.Collections.emptySet();
        static Landed stamps(java.util.Set<String> hashes){Landed l=new Landed(null,null,"",true);l.stamps=true;l.stamped=java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(hashes));return l;}
        /** A note that arrived saying what it said already: nothing to tell, and the marks and the box to ask again. */
        static Landed people(String note){return new Landed(null,null,note,true,true);}
        /** Somebody left. That is worth saying, and whoever is looking at the list is looking at an old one. */
        static Landed left(String said,String note){return new Landed(said,null,note,false,true);}
        /** Files sent or received on their own moved on: said, where there is something to say, and asked about. */
        static Landed transfer(String said,String asking){return new Landed(said,null,"",false,false,true,asking);}
        /**
         * Something another person shared with me for the first time is here and waits for an answer (decision 109), or more of
         * it came: the pop-up is asked for, which says each thing once. {@code waits} is the thing on Home it waits under, so a
         * line in the phone's notifications is said once for it too; {@code said} is that line.
         */
        boolean waiting; String waits="";
        /**
         * Something shared with this device that came in without waiting for an answer, a file from another device of the
         * owner's: said on the phone, so it is not found only by chance (the owner, 2026-10-16: "once shared I should get a
         * notification on the Pro at the phone level that someone shared something with me").
         */
        boolean shared;
        static Landed shared(String said,String what){Landed l=new Landed(said,null,"",false,true);l.shared=true;l.waits=what==null?"":what;return l;}
        static Landed waiting(String said,String waits){Landed l=new Landed(said,null,"",false,true);l.waiting=true;l.waits=waits==null?"":waits;return l;}
        /**
         * A help request arrived (decision 113): a loud alert is wanted, "HELP from {@code helpFrom}", with the request's
         * words, place, time and map link, updated as the next updates of the same opening come. Null where nothing arrived.
         */
        Help.Request helpRequest; String helpFrom="";
        static Landed help(Help.Request request,String from){Landed l=new Landed(null,null);l.helpRequest=request;l.helpFrom=from==null?"":from;return l;}
        /**
         * A message arrived, or one sent from here was kept at its far end (decision 145): the Inbox and its count want
         * drawing again. {@code messageFrom} and {@code messageSaid} name a new one, for a line in the phone's notifications;
         * empty where only an answer came.
         */
        boolean message; String messageFrom="",messageSaid="",messageAddress="";
        static Landed message(String from,String said,String address){Landed l=new Landed(null,null);l.message=true;l.messageFrom=from==null?"":from;l.messageSaid=said==null?"":said;l.messageAddress=address==null?"":address;return l;}
        /**
         * Who it came from, by the name they go by here, and how many files came or are asked about: what the phone's one
         * notification of what arrived counts by (decision 236), so it can say "3 notes updated, 1 file" in the person's
         * language rather than repeat the last English line. Empty and 0 where nobody is known or no file came.
         */
        String from=""; int many;
        Landed by(String who,int count){from=who==null?"":who;many=count;return this;}
        /** A note shared with me for the first time, rather than one written in: counted as such. */
        boolean newly;
        Landed newly(){newly=true;return this;}
    }

    /**
     * Somebody's offer, taken up.
     *
     * <p>Sent back down the same road their code came along: sealed for them, signed by this phone, and
     * carrying what they need to hand the thing over - who this is, where to reach it, and the keys.
     * Blocking: the worker calls it.
     */
    static void accept(Context where,Keys keys,Pairing.Said them,String myName,String myAddress)
            throws Exception {
        say(where,keys,them.address,them.agreement,them.scope,them.target,them.level,myName,myAddress,null);
    }

    /**
     * Paired back: the answer to somebody who scanned this device's code, with nothing offered.
     *
     * <p>A plain code used to be read in one direction only. The phone that scanned it kept the other
     * device; the device that showed it never heard, and dropped everything the phone then sent as coming
     * from a stranger - the phone listed the PC on a shared note, and the note never reached it. So scanning
     * a plain code says hello too, and this is the hello coming back once the owner has said yes.
     */
    static void helloBack(Context where,Keys keys,Hello.Said them,String myName,String myAddress,String contact) throws Exception {
        say(where,keys,them.address,them.agreement,"","",Sharing.Level.READ,myName,myAddress,contact);
    }

    /**
     * The same thing said again.
     *
     * <p>An acceptance is one message to a phone that may be asleep, in a drawer, or being updated, and a
     * message to a node nobody is listening on is gone. So it is kept and repeated rather than sent once
     * and hoped over: otherwise accepting something works when the two phones happen to be awake together
     * and does nothing at all when they are not, with no sign either way.
     */
    static void sayAgain(Context where,NoteStore store,Keys keys,NoteStore.Accepting again,
                         String myName,String myAddress) throws Exception {
        NoteStore.Contact them=store.address(again.address);
        if(them==null||them.agreement.length==0)return;
        say(where,keys,again.address,them.agreement,again.scope,again.target,again.level,myName,myAddress,them.contact);
    }

    private static void say(Context where,Keys keys,String address,byte[] agreement,String scope,
                            String target,boolean writes,String myName,String myAddress,String contact) throws Exception {
        say(where,keys,address,agreement,scope,target,writes?Sharing.Level.WRITE:Sharing.Level.READ,myName,myAddress,contact);
    }

    private static void say(Context where,Keys keys,String address,byte[] agreement,String scope,
                            String target,Sharing.Level level,String myName,String myAddress,String contact) throws Exception {
        MaximaNode node=Node.node(where);
        if(node==null)throw new IllegalStateException("The node is not running.");
        if(myAddress==null||myAddress.trim().isEmpty())
            throw new IllegalStateException("This phone has no address to be reached at yet.");
        Hello.Said hello=new Hello.Said(myName,myAddress.trim(),
            Point.shorten(keys.agreement().getPublic()),Point.shorten(keys.signing().getPublic()),
            scope,target,level.writes()).claiming(level);
        // Accepting an offer of everything is connecting another device of one's own, and only then does the hello
        // say whose this device is and what it is called among them: nobody else is told either. See Persons.
        if(Sharing.Scope.LIBRARY.name().equals(scope))hello=hello.owner(me(where));
        byte[] plain=Hello.wrap(hello);
        byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),plain,
            keys.signing(),Keys.publicKey(agreement));
        // To the device, where the network knows it: an address is a snapshot, and a PC restarted since
        // its code was scanned is somewhere else. The address on the code only when nothing better is known.
        com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,contact);
        MaximaSender.Result said=reach!=null?Direct.send(node,reach,APPLICATION,sealed,null,Node.helpers()):node.sendRaw(routable(node,address),APPLICATION,sealed);
        if(said==null||!said.isOk())throw new IllegalStateException("They could not be reached just now.");
    }

    /**
     * Everything one thing owes, sent. Blocking: the worker calls it.
     *
     * <p>Each note goes once per address that is owed it. A note that reaches nobody is not an error — it
     * is a note shared with nobody, which is most of them.
     */
    static Done send(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id)
            throws Exception {
        return send(where,store,keys,kind,id,null);
    }

    /**
     * @param only one address to send to and nobody else, or null for everybody who is owed. Given when
     *             that address has just asked: it is plainly there, so it is sent what it is owed at once,
     *             whenever it was last tried.
     */
    static Done send(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,String only)
            throws Exception {
        return send(where,store,keys,kind,id,only,false);
    }

    /**
     * @param whole for one note: send it to everybody who has it whether or not they are thought to be
     *              owed it. What somebody pressing Sync on a note means is "make these the same", and two
     *              phones can each believe the other is up to date while holding different words.
     */
    static Done send(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,String only,
                     boolean whole) throws Exception {
        // A file shared on its own goes in its sleeve, and holds nothing else (decision 92).
        if(kind==NoteStore.Branch.Kind.FILE)return sleeves(where,store,keys,id,only,whole);
        // Temp is not a folder: what is in it is, each note, folder and file sent as itself (its card's Sync now).
        if(kind==NoteStore.Branch.Kind.TEMP) {
            int sent=0,failed=0;String why="";List<Unsent.Problem> problems=new ArrayList<>();
            for(NoteStore.Branch one:store.temporary(System.currentTimeMillis())) {
                if(one.kind!=NoteStore.Branch.Kind.PAGE&&one.kind!=NoteStore.Branch.Kind.COLLECTION&&one.kind!=NoteStore.Branch.Kind.FILE)continue;
                Done done=send(where,store,keys,one.kind,one.id,only,whole);
                sent+=done.sent;failed+=done.failed;if(why.isEmpty())why=done.why==null?"":done.why;problems.addAll(done.problems);
            }
            return new Done(sent,failed,why,problems);
        }
        Done notes=notes(where,store,keys,kind,id,only,whole);
        if(kind==NoteStore.Branch.Kind.PAGE)return notes;
        // And the collections in it that go on their own: those with no note in them to carry them, and those with a look
        // or files of their own to say (see Carton).
        // Sync, Send now, goes whole for these too (decision 294): passed as not whole, a collection or a file handed over a
        // moment before went on waiting its turn, up to a quarter of an hour, Send now or not.
        Done alone=cartons(where,store,keys,kind,id,only,whole);
        // And, for everything, the files shared on their own (see Sleeve).
        if(kind==NoteStore.Branch.Kind.LIBRARY)alone=both(alone,sleeves(where,store,keys,null,only,whole));
        return both(notes,alone);
    }

    /** Two sendings said as one. */
    private static Done both(Done one,Done other) {
        if(other.sent==0&&other.failed==0)return one;
        if(one.sent==0&&one.failed==0)return other;
        List<Unsent.Problem> all=new ArrayList<>(one.problems);all.addAll(other.problems);
        return new Done(one.sent+other.sent,one.failed+other.failed,one.why.isEmpty()?other.why:one.why,all);
    }

    /**
     * Whether a note waits for the device it would go to to know about trees: that device knows three levels and no
     * more, and the note does not sit where a 0.1 note can - two collections down (see {@link Things#fitsThreeLevels}).
     *
     * @param above the collections above the note, from Home down
     */
    static boolean waitsForTrees(boolean trees,List<String> above){return !trees&&!Things.fitsThreeLevels(above,true);}

    /**
     * The level a list is said to be the list of, to one device: as the rows here say it to a device that knows about
     * trees, and by how deep the thing sits to one that does not (see {@link Things#oldScope}) - a collection on Home
     * a collection, one inside that a book, a note a note. Null where nothing is shared, or where the thing sits deeper
     * than that device has a word for.
     *
     * @param heldAbove the collections above whatever the list is of, from Home down
     */
    static Sharing.Scope scopeSaid(boolean trees,Sharing.Scope held,List<String> heldAbove) {
        if(held==null||trees||held==Sharing.Scope.LIBRARY)return held;
        return Things.oldScope(heldAbove,held==Sharing.Scope.PAGE);
    }

    private static Done notes(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,String only,
                              boolean whole) throws Exception {
        List<Outbox.Wait> owed=store.owed(kind,id);
        if(whole&&kind==NoteStore.Branch.Kind.PAGE) {
            NoteStore.Note all=store.get(id);
            owed=new ArrayList<>();
            // Not from a reader: what it would send, everybody would refuse.
            if(all!=null&&!store.onlyReads(id))for(String address:store.everybodyIn(kind,id))
                owed.add(new Outbox.Wait(id,address,all.revision,false));
        }
        if(only!=null) {
            List<Outbox.Wait> theirs=new ArrayList<>();
            for(Outbox.Wait wait:owed)if(only.equals(wait.address))theirs.add(wait);
            owed=theirs;
        }
        // Never a note to a backup server (decision 330), whatever a list still says.
        if(!owed.isEmpty()){List<Outbox.Wait> kept=new ArrayList<>();for(Outbox.Wait wait:owed)if(!store.keepsBackups(wait.address))kept.add(wait);owed=kept;}
        if(owed.isEmpty())return new Done(0,0,"");
        // Named in whatever is said about what did not go: the thing that was asked to go, not each note in it.
        String thing=store.thingName(kind,id);
        List<Unsent.Problem> problems=new ArrayList<>();
        MaximaNode node=Node.node(where);
        if(node==null) {
            problems.add(new Unsent.Problem(Unsent.Why.NOT_CONNECTED,"",thing,"",""));
            return new Done(0,owed.size(),Unsent.said(problems.get(0),here()),problems);
        }

        // Who each address is, and the keys to seal for it. An address with no keys has never been paired,
        // and a note sealed for nobody is a note sent nowhere: those are counted as failed, not skipped.
        java.util.Map<String,NoteStore.Contact> known=new java.util.HashMap<>();
        for(NoteStore.Contact contact:store.addresses())known.put(contact.address,contact);
        // Linked through a list and not answered yet: nothing goes until they do, since a build from before could
        // not open it and would never say so. See Linking.
        java.util.Set<String> linking=store.linkingNow();

        KeyPair mine=keys.signing();
        // What went a moment ago and has not had time to be answered. Two things can ask for the same note
        // within a breath of each other - opening the pad sends what is owed, and so does the writing
        // having just stopped - and the second would otherwise send it again before the first could
        // possibly have been answered.
        java.util.Map<String,Outbox.Handed> lately=new java.util.HashMap<>();
        for(Outbox.Handed one:store.handed())lately.put(Outbox.mark(one.address,one.page),one);
        long clock=System.currentTimeMillis();
        int sent=0, failed=0; String why="";
        String me=myFingerprint(keys);
        for(Outbox.Wait wait:owed) {
            NoteStore.Contact them=known.get(wait.address);
            // A contact holding this device's own keys: nothing is sent to it, nor counted as owed to it. Sent, it came
            // straight back, was never answered, and went again every round (the Pro, 2026-11-04, about 9 MB an hour).
            if(them!=null&&isSelf(fingerprint(them),me)){android.util.Log.w("Mininotes/Post","not sent: a contact here holds this device's own keys");continue;}
            if(them==null||them.agreement.length==0) {
                failed++;
                // Who, by name: a device known here but never given its keys, or one only named in the list of people
                // for a thing - a list that came from somebody else - and never paired with this device at all.
                Unsent.Problem p=them==null
                    ?new Unsent.Problem(Unsent.Why.ONLY_LISTED,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address)
                    :new Unsent.Problem(Unsent.Why.NOT_PAIRED,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            if(linking.contains(them.address)) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.Why.LINKING,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address);
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            NoteStore.Note note=store.get(wait.page);
            if(note==null){failed++;continue;}
            // Where the note sits, from Home down; and whether the device it goes to can hold it there. A build from
            // before trees knows three levels: what does not fit them waits, saying so, until it says it knows more.
            List<String> above=store.above(note.id);
            boolean trees=knowsTrees(where,them);
            if(waitsForTrees(trees,above)) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.Why.NEEDS_UPDATE,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            Outbox.Handed before=lately.get(Outbox.mark(wait.address,wait.page));
            if(only==null&&!whole&&before!=null&&before.revision==note.revision&&clock>=before.at
                &&clock-before.at<JUST_NOW)continue;
            try {
                PublicKey theirs=Keys.publicKey(them.agreement);
                Parcel.Sent going=parcelFor(store,them,note,above,trees,true);
                // Only what changed, to a build that reads it and against a text both are known to hold exactly (decision
                // 320); the whole note to everybody else, the first time, on Sync, and where a change would not fit.
                Parcel.Sent change=changeFor(store,them.address,note,going,readsChanges(where,them),whole);
                byte[] text=null;
                if(change!=null)try{text=Parcel.wrap(change,Envelope.MAX_TEXT);}catch(java.io.IOException doesNotFit){change=null;}
                if(text==null)text=Parcel.wrap(going,Envelope.MAX_TEXT);
                byte[] sealed=Envelope.seal(sixteen(wait.page),
                    note.revision,System.currentTimeMillis(),text,mine,theirs);
                // Handed to the peer, not to an address they used to be at. Straight to their door first -
                // on this network, then at a public address they proved - and if one takes it, no relay
                // is given a copy (see Direct). Otherwise every address the transport knows for them, then
                // their directory is asked where they went - which is the whole reason a phone that moved
                // is still reachable. Only a peer this node has never been introduced to falls back to
                // dialling the address off their code.
                // And where they collect from, when neither their door nor anything else of theirs is in reach:
                // their home keeps it for them (see Home).
                com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,them.contact);
                int doors=Direct.WENT.get(),homes=Direct.LEFT.get(),shut=Direct.SHUT_HERE.get();
                MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
                // Which road it took and what the relay said, and nothing else: no address, no key, no
                // word of the note. A send that fails quietly while somebody is writing has to be findable
                // afterwards by whoever is holding the phone and a cable.
                android.util.Log.i("Mininotes/Post","sent "+(reach!=null?"to a contact":"to an address")
                    +", revision "+note.revision+(change!=null?", a change against "+change.change.base:", whole")+", "+text.length+" bytes"
                    +": "+(said==null?"no answer":said.statusName)
                    +Direct.road(doors,homes,shut,said!=null&&said.isOk()));
                // Not heard from lately, so perhaps not there: a copy goes with whoever can carry it, in
                // case they are back only after this device has gone.
                if(!there(them))leaveWithCarriers(where,store,keys,node,them,Courier.NOTE,sixteen(wait.page),note.revision,sealed);
                if(said!=null&&said.isOk()) {
                    // Handed over, and no more than that. The mark means somebody has it, and the only one
                    // who can say so is them: it stays until their answer comes back.
                    store.handedOver(them.address,wait.page,note.revision);
                    // Its stamps with it, so they go again only when they change (decision 251).
                    stampsWent(where,them.address,wait.page,going.stamps);
                    // Told where its files are, for the files that said so: whether they then say they have
                    // them decides whether the list goes to them again.
                    Parcel.Sent went=Parcel.open(text);
                    if(went!=null&&went.files!=null) {
                        List<String> named=new ArrayList<>();
                        for(Enclosure.Listed one:went.files)if(one.fetchable())named.add(one.id);
                        if(!named.isEmpty())store.told(named,them.address);
                    }
                    // What went is written down as it went. It is what the next thing they write will be
                    // weighed against, and a text that was only ever on the wire cannot be weighed against.
                    // Exactly the words that went, at the revision they went at: read again from the notebook, a note being
                    // typed in had moved on by then, and the text that went was missing from its history, so the next
                    // thing they wrote was weighed against an older text and words since deleted came back (the owner's
                    // Live note, 2026-10-14).
                    store.keepVersion(wait.page,note.revision,"",note.title==null?"":note.title,note.body==null?"":note.body);
                    sent++;
                } else {
                    failed++;
                    Unsent.Problem p=new Unsent.Problem(Unsent.Why.NOT_REACHED,them.name,thing,"","");
                    problems.add(p);
                    if(why.isEmpty())why=Unsent.said(p,here());
                }
            } catch(Exception e) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.of(e.getMessage()),them.name,thing,"",e.getMessage());
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
            }
        }
        return new Done(sent,failed,why,problems);
    }

    /**
     * A note as it goes to one device: its words, its path, who else has it, its files, its look. Made apart from the
     * sending so a test can hand what goes from one notebook to another without a node.
     *
     * @param above the collections above the note here, from Home down
     * @param trees whether that device knows about trees
     */
    static Parcel.Sent parcelFor(NoteStore store,NoteStore.Contact them,NoteStore.Note note,List<String> above,boolean trees,
                                 boolean answer) {
        // The shelf goes with the note. A note filed into whatever collection happened to be first on
        // the other phone has lost most of what it meant; and because each collection travels by its id
        // rather than by its name, the second note out of it lands beside the first instead of
        // building another that looks the same. The whole path, for a build that knows trees; and the
        // nearest two collections in the two old fields, for one that does not (decision 15).
        String[] two=Things.nearestTwo(above);
        String collection=two[0], book=two[1];
        List<String> path=new ArrayList<>(above);path.add(note.id);
        // And who else has it. A shared thing held only by whoever began it is a broadcast; a
        // thing every holder knows the holders of is something people can pass on between them.
        Sharing.Rule held=store.sharedAt(path);
        String what=held==null?note.id:held.target;
        int at=above.indexOf(what);
        Sharing.Scope level=scopeSaid(trees,held==null?null:held.scope,at<0?above:above.subList(0,at));
        // And the files the note keeps, each saying where its pieces are once it has gone up: see
        // Enclosure. Made to fit the envelope, which a longer note leaves less room in. And its icon and its
        // picture, after its path, to a build that knows about trees: a build from before could not show them.
        Parcel.Sent going=new Parcel.Sent(collection,store.nameOf(collection,true),
            book,store.nameOf(book,false),note.title,note.body,
            store.mayWrite(them.address,path),
            store.travelling(level==null?null:held.scope,what,them.address),level==null?"":level.name(),what,answer,
            store.agreedAt(note.id,them.address),true,store.enclosed(note.id,them.address),System.currentTimeMillis(),
            store.history(note.id),store.steps(above),
            trees?store.iconOf(NoteStore.Branch.Kind.PAGE,note.id):"",trees?store.imageOf(NoteStore.Branch.Kind.PAGE,note.id):null);
        // And the colour this owner chose for themselves, so everybody draws them in it (see Parcel.Sent#ink).
        long[] ink=store.myInk();going.ink=(int)ink[0];going.inkAt=ink[1];
        // And when it is to be gone, where it is temporary or was: said always to a build that knows trees.
        if(trees)going.until=store.untilOf(NoteStore.Branch.Kind.PAGE,note.id);
        // And its own colour and strength, with when each was decided, where either ever was (decision 108): the
        // last thing after the path, so a build from before reads all it ever read and stops there.
        long[] look=store.lookOf(NoteStore.Branch.Kind.PAGE,note.id);
        going.colour=(int)look[0];going.tone=(int)look[1];going.colourDecided=look[2];going.toneDecided=look[3];
        // And the stamps on Minima of the note and of the files it keeps, which travel with it (decision 251): after the look,
        // in the room the rest leaves.
        going.stamps=store.stampsTravelling(stampedWith(note.id,going.files));
        return going;
    }

    /**
     * The parcel saying only what changed since the revision that device last confirmed, or null where the whole note goes
     * (decision 320; see {@link Delta#going} for when). Made from the whole note's parcel, so nothing but the text differs.
     *
     * @param whole the whole note was asked for: Sync, or a device that could not rebuild a change
     */
    static Parcel.Sent changeFor(NoteStore store,String address,NoteStore.Note note,Parcel.Sent going,boolean reads,boolean whole) {
        if(!reads||whole)return null;
        long agreed=store.agreedAt(note.id,address);
        String base=agreed>0?store.keptAt(note.id,agreed,address):null;
        Delta change=Delta.going(true,false,agreed,base,PrivateCode.scrub(going.body==null?"":going.body));
        return change==null?null:Parcel.asChange(going,change);
    }

    /**
     * A change rebuilt into the whole note it stands for, from this device's own copy of the text it was made against; null
     * where that copy is not here or what it makes is not exactly what the sender has (decision 320). Then nothing is taken.
     */
    static Parcel.Sent rebuilt(NoteStore store,String id,String from,Parcel.Sent parcel) {
        if(parcel.change==null)return null;
        String base=store.keptAs(id,parcel.change.base,from,parcel.change.baseSha);
        String text=base==null?null:parcel.change.onto(base);
        return text==null?null:Parcel.withText(parcel,text);
    }

    /** A note's id and those of the files its list names: what its stamps are the stamps of (decision 251). */
    static List<String> stampedWith(String note,List<Enclosure.Listed> files) {
        List<String> ids=new ArrayList<>();ids.add(note);
        if(files!=null)for(Enclosure.Listed one:files)ids.add(one.id);
        return ids;
    }

    /**
     * Who may do what, told to everybody who has the thing. Blocking.
     *
     * <p>The list of who has a thing travels with its notes, and with nothing else. So changing what
     * somebody may do, and writing nothing, told nobody: the change sat on this phone until the next word
     * was typed, which for a note that is finished is never. What is owed goes first - somebody just added
     * is owed all of it - and then one note out of the thing goes whole to everybody, carrying the list.
     */
    static Done changed(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id)
            throws Exception {
        // A file shared on its own carries its own list, in its sleeve: to everybody who has it.
        if(kind==NoteStore.Branch.Kind.FILE)return sleeves(where,store,keys,id,null,true);
        Done done=send(where,store,keys,kind,id);
        boolean shelf=kind==NoteStore.Branch.Kind.COLLECTION||kind==NoteStore.Branch.Kind.BOOK;
        if(kind!=NoteStore.Branch.Kind.PAGE&&!shelf)return done;
        // A note carries one list: that of the nearest thing to it, itself included, that is shared by itself.
        // So the note to send is one that carries this list, and not any note that happens to be inside.
        String carrier=null;
        for(Outbox.Page page:store.pagesUnder(kind,id)) {
            Sharing.Rule on=store.sharedAt(page.path());
            if(on!=null&&on.target.equals(id)){carrier=page.id;break;}
        }
        // A collection with no note in it carries its own list, on its own (see Carton).
        Done carried=carrier!=null?send(where,store,keys,NoteStore.Branch.Kind.PAGE,carrier,null,true)
            :shelf?cartons(where,store,keys,kind,id,null,true):null;
        if(carried==null)return done;
        List<Unsent.Problem> both=new ArrayList<>(done.problems);both.addAll(carried.problems);
        return new Done(done.sent,done.failed+carried.failed,done.why.isEmpty()?carried.why:done.why,both);
    }

    /**
     * The collections owed to somebody on their own, sent: each one reached by a rule that holds no note, which would
     * otherwise carry it, or that has a look or files of its own to say (see {@link NoteStore#cartonsOwed} and
     * {@link Carton}). Sealed under the envelope id {@link Things} gives it and its own revision, and answered as a note
     * is. Only to a device that has said it knows about trees: a build from before would take these bytes for a note
     * written the oldest way. For one that has not, it waits, and is said to wait for that device to be updated
     * ({@link Unsent.Why#NEEDS_UPDATE}); it goes when that device says it knows more. Blocking.
     *
     * @param whole to everybody it reaches, whether or not they are thought to have it - somebody pressed Sync, or who
     *              may do what in it has changed, and a list only travels in one of these
     */
    static Done cartons(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,String only,boolean whole)
            throws Exception {
        List<Outbox.Wait> owed=store.cartonsOwed(kind,id,whole);
        if(only!=null){List<Outbox.Wait> theirs=new ArrayList<>();for(Outbox.Wait one:owed)if(only.equals(one.address))theirs.add(one);owed=theirs;}
        return cartons(where,store,keys,kind,id,owed,only,whole);
    }

    /**
     * One collection sent on its own to one device now, whatever it is thought to have: its files have gone up, or it
     * has not said it has them and is told again (see {@link NoteStore#toTell}). Blocking.
     */
    static Done cartonTo(Context where,NoteStore store,Keys keys,String collection,String address) throws Exception {
        List<Outbox.Wait> one=new ArrayList<>();
        one.add(new Outbox.Wait(collection,address,store.revisionOf(collection),false));
        return cartons(where,store,keys,NoteStore.Branch.Kind.COLLECTION,collection,one,address,true);
    }

    /** One collection sent on its own to everybody its files go to, now: see {@link NoteStore#goesWith}. Blocking. */
    static Done cartonToAll(Context where,NoteStore store,Keys keys,String collection) throws Exception {
        List<Outbox.Wait> all=new ArrayList<>();long revision=store.revisionOf(collection);
        for(String address:store.goesWith(collection))all.add(new Outbox.Wait(collection,address,revision,false));
        return cartons(where,store,keys,NoteStore.Branch.Kind.COLLECTION,collection,all,null,true);
    }

    private static Done cartons(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,List<Outbox.Wait> owed,
                                String only,boolean whole) throws Exception {
        if(owed.isEmpty())return new Done(0,0,"");
        java.util.Map<String,NoteStore.Contact> known=new java.util.HashMap<>();
        for(NoteStore.Contact contact:store.addresses())known.put(contact.address,contact);
        java.util.Set<String> linking=store.linkingNow();
        java.util.Map<String,Outbox.Handed> lately=new java.util.HashMap<>();
        for(Outbox.Handed one:store.handed())lately.put(Outbox.mark(one.address,one.page),one);
        long clock=System.currentTimeMillis();
        List<Unsent.Problem> problems=new ArrayList<>();
        List<Outbox.Wait> going=new ArrayList<>();
        int sent=0,failed=0;String why="";
        for(Outbox.Wait wait:owed) {
            NoteStore.Contact them=known.get(wait.address);
            String thing=store.thingName(NoteStore.Branch.Kind.COLLECTION,wait.page);
            // Nobody to seal for, or linked and not answered yet: as for a note, said and counted.
            if(them==null||them.agreement.length==0||linking.contains(them.address)) {
                failed++;
                Unsent.Problem p=them==null
                    ?new Unsent.Problem(Unsent.Why.ONLY_LISTED,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address)
                    :linking.contains(them.address)?new Unsent.Problem(Unsent.Why.LINKING,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address)
                    :new Unsent.Problem(Unsent.Why.NOT_PAIRED,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            // A build from before trees has nothing to show a collection on its own as - nor its icon, its picture or its
            // files: it is not sent one, and what waits for it waits for it to be updated, saying so.
            if(!knowsTrees(where,them)) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.Why.NEEDS_UPDATE,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            // Handed over a moment ago, or lately and not due again: its answer has had no time to come.
            Outbox.Handed before=lately.get(Outbox.mark(wait.address,wait.page));
            if(only==null&&!whole&&before!=null&&before.revision==wait.revision&&clock>=before.at
                &&clock-before.at<Math.max(JUST_NOW,Outbox.againAfter(before.tries,heardLately(them))))continue;
            going.add(wait);
        }
        // The node is asked for only where something is to go: a round with nothing for anybody starts nothing.
        MaximaNode node=going.isEmpty()?null:Node.node(where);
        if(!going.isEmpty()&&node==null) {
            problems.add(new Unsent.Problem(Unsent.Why.NOT_CONNECTED,"",store.thingName(kind,id),"",""));
            return new Done(0,failed+going.size(),why.isEmpty()?Unsent.said(problems.get(problems.size()-1),here()):why,problems);
        }
        for(Outbox.Wait wait:going) {
            NoteStore.Contact them=known.get(wait.address);
            String thing=store.thingName(NoteStore.Branch.Kind.COLLECTION,wait.page);
            try {
                // Made to fit the envelope, as a note's parcel is: a long list of files stops saying where each is first.
                byte[] text=Carton.wrap(store.carton(wait.page,them.address,true),Envelope.MAX_TEXT);
                byte[] sealed=Envelope.seal(Things.envelopeId(wait.page),wait.revision,System.currentTimeMillis(),text,
                    keys.signing(),Keys.publicKey(them.agreement));
                MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
                // What it was and how it went, and nothing else: no name, no address.
                android.util.Log.i("Mininotes/Post","sent a collection on its own, revision "+wait.revision+": "
                    +(said==null?"no answer":said.statusName));
                if(said!=null&&said.isOk()) {
                    store.handedOver(them.address,wait.page,wait.revision);sent++;
                    // Told where its files are, for the files that said so, as for a note's.
                    Carton.Sent went=Carton.open(text);
                    if(went!=null&&went.files!=null) {
                        List<String> named=new ArrayList<>();
                        for(Enclosure.Listed one:went.files)if(one.fetchable())named.add(one.id);
                        if(!named.isEmpty())store.told(named,them.address);
                    }
                }
                else {
                    failed++;
                    Unsent.Problem p=new Unsent.Problem(Unsent.Why.NOT_REACHED,them.name,thing,"","");
                    problems.add(p);
                    if(why.isEmpty())why=Unsent.said(p,here());
                }
            } catch(Exception e) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.of(e.getMessage()),them.name,thing,"",e.getMessage());
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
            }
        }
        return new Done(sent,failed,why,problems);
    }

    /**
     * The files owed to somebody on their own, sent: each shared by a rule on it (see {@link NoteStore#sleevesOwed} and
     * {@link Sleeve}; decision 92). Sealed under the file's own sixteen bytes and its own revision, and answered as a note
     * is. Only to a device that has said it knows files on their own ({@link Receipt#LOOSE}): a build from before would
     * take these bytes for a note written the oldest way and write them over somebody's words. For one that has not, it
     * waits, and is said to wait for that device to be updated ({@link Unsent.Why#NEEDS_UPDATE}), as a carton does; it goes
     * when that device says it knows more. Blocking.
     *
     * @param file  one file, or null for every file
     * @param only  one address to send to, or null for everybody owed
     * @param whole to everybody it reaches, whether or not they are thought to have it: Sync, or who has it changed
     */
    static Done sleeves(Context where,NoteStore store,Keys keys,String file,String only,boolean whole) throws Exception {
        List<Outbox.Wait> owed=store.sleevesOwed(file,whole);
        if(owed.isEmpty())return new Done(0,0,"");
        java.util.Map<String,NoteStore.Contact> known=new java.util.HashMap<>();
        for(NoteStore.Contact contact:store.addresses())known.put(contact.address,contact);
        java.util.Set<String> linking=store.linkingNow();
        java.util.Map<String,Outbox.Handed> lately=new java.util.HashMap<>();
        for(Outbox.Handed one:store.handed())lately.put(Outbox.mark(one.address,one.page),one);
        long clock=System.currentTimeMillis();
        List<Unsent.Problem> problems=new ArrayList<>();
        List<Outbox.Wait> going=new ArrayList<>();
        int sent=0,failed=0;String why="";
        for(Outbox.Wait wait:owed) {
            if(only!=null&&!only.equals(wait.address))continue;
            NoteStore.Contact them=known.get(wait.address);
            String thing=store.fileName(wait.page);
            if(them==null||them.agreement.length==0||linking.contains(them.address)) {
                failed++;
                Unsent.Problem p=them==null
                    ?new Unsent.Problem(Unsent.Why.ONLY_LISTED,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address)
                    :linking.contains(them.address)?new Unsent.Problem(Unsent.Why.LINKING,store.nameFor(wait.address),thing,store.listedIn(wait.address),"",wait.address)
                    :new Unsent.Problem(Unsent.Why.NOT_PAIRED,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            // Never sealed for a device that has not said it knows files on their own: the gate (see Receipt.LOOSE).
            if(!knowsLoose(where,them)) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.Why.NEEDS_UPDATE,store.nameFor(wait.address),thing,"","");
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
                continue;
            }
            // Nor before its bytes are up (decision 94): a sleeve that cannot say where its pieces are was refused as a file
            // that cannot be fetched, and only a round after the upload brought it. It stays owed, and the round of file work
            // that sends it up sends it then (see goUp). Not a failure: it is uploading, and its icon and its box say so. To a
            // device on this network it goes as soon as the pieces are at this device's door (decision 96).
            if(!store.sleeveReady(wait.page,wait.address))continue;
            Outbox.Handed before=lately.get(Outbox.mark(wait.address,wait.page));
            if(only==null&&!whole&&before!=null&&before.revision==wait.revision&&clock>=before.at
                &&clock-before.at<Math.max(JUST_NOW,Outbox.againAfter(before.tries,heardLately(them))))continue;
            going.add(wait);
        }
        MaximaNode node=going.isEmpty()?null:Node.node(where);
        if(!going.isEmpty()&&node==null) {
            problems.add(new Unsent.Problem(Unsent.Why.NOT_CONNECTED,"",file==null?"":store.fileName(file),"",""));
            return new Done(0,failed+going.size(),why.isEmpty()?Unsent.said(problems.get(problems.size()-1),here()):why,problems);
        }
        for(Outbox.Wait wait:going) {
            NoteStore.Contact them=known.get(wait.address);
            String thing=store.fileName(wait.page);
            try {
                Sleeve.Sent sleeve=store.sleeve(wait.page,them.address);
                byte[] text=Sleeve.wrap(sleeve,Envelope.MAX_TEXT);
                byte[] sealed=Envelope.seal(sixteen(wait.page),wait.revision,System.currentTimeMillis(),text,
                    keys.signing(),Keys.publicKey(them.agreement));
                MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
                // What it was and how it went, and nothing else: no name, no address.
                android.util.Log.i("Mininotes/Post","sent a file on its own, revision "+wait.revision+": "
                    +(said==null?"no answer":said.statusName));
                if(said!=null&&said.isOk()){store.handedOver(them.address,wait.page,wait.revision);stampsWent(where,them.address,wait.page,sleeve.stamps);sent++;}
                else {
                    failed++;
                    Unsent.Problem p=new Unsent.Problem(Unsent.Why.NOT_REACHED,them.name,thing,"","");
                    problems.add(p);
                    if(why.isEmpty())why=Unsent.said(p,here());
                }
            } catch(Exception e) {
                failed++;
                Unsent.Problem p=new Unsent.Problem(Unsent.of(e.getMessage()),them.name,thing,"",e.getMessage());
                problems.add(p);
                if(why.isEmpty())why=Unsent.said(p,here());
            }
        }
        return new Done(sent,failed,why,problems);
    }

    /** One at a time, off the thread the node brought the note on: an answer is a network call too. */
    private static final java.util.concurrent.ExecutorService ANSWERS=
        java.util.concurrent.Executors.newSingleThreadExecutor(work->{
            Thread one=new Thread(work,"mininotes-answers");one.setDaemon(true);return one;});

    /**
     * "I have it" has a lane of its own, ahead of everything else ANSWERS says (Graphene, 2026-10-17): behind forty "my
     * build can do this" said back at start, a note's answer waited minutes, so the sender sent the note again every half
     * minute, and each copy made more to answer. A note answered at once is a note that stops coming.
     */
    private static final java.util.concurrent.ExecutorService RECEIPTS=
        java.util.concurrent.Executors.newSingleThreadExecutor(work->{
            Thread one=new Thread(work,"mininotes-receipts");one.setDaemon(true);return one;});

    /** The answers queued and not yet on their way, so a note that comes ten times is answered once. */
    private static final java.util.Set<String> WAITING_ANSWERS=java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * "I have it", said to whoever sent it.
     *
     * <p>Nothing is done about one that does not get through. They will send the note again when no
     * answer comes, and be answered again; an answer is never itself answered, so nothing goes round.
     */
    /** Answers that went out, by device key, page, revision and what was said, and when: see {@link #answer}. */
    private static final java.util.Map<String,Long> ANSWERED=new java.util.concurrent.ConcurrentHashMap<>();
    /** How long the same answer is not sent again. */
    static final long ANSWER_AGAIN=10*60_000L;

    /** When this device last asked a device for one note whole, by its key and the note: see {@link #askWhole}. */
    private static final java.util.Map<String,Long> ASKED_WHOLE=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A change that could not be rebuilt here (decision 320): the note asked for whole, by the {@link Receipt#ASK} that names
     * it, which every build answers by sending that note whole whatever it thinks is owed. Not again within a minute for the
     * same note, so a change carried here twice asks once.
     */
    private static void askWhole(final Context where,final NoteStore store,final Keys keys,final NoteStore.Contact them,final byte[] page) {
        if(where==null||them.agreement.length==0)return;
        final String once=fingerprint(them)+" "+Courier.hex(page);
        Long lately=ASKED_WHOLE.get(once);
        long now=System.currentTimeMillis();
        if(lately!=null&&now>=lately&&now-lately<60_000L)return;
        ASKED_WHOLE.put(once,now);
        RECEIPTS.execute(()->{
            try {
                MaximaNode node=Node.node(where);
                if(node==null){ASKED_WHOLE.remove(once);return;}
                byte[] sealed=Envelope.seal(page,0,System.currentTimeMillis(),Receipt.wrap(Receipt.ASK),keys.signing(),Keys.publicKey(them.agreement));
                MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
                android.util.Log.i("Mininotes/Post","asked for a note whole: "+(said==null?"no answer":said.statusName));
                if(said==null||!said.isOk())ASKED_WHOLE.remove(once);
            } catch(Exception notNow) {
                // Asked again when the change comes again: their note waits on an answer that has not come.
                ASKED_WHOLE.remove(once);
                android.util.Log.w("Mininotes/Post","could not ask for a note whole: "+notNow.getClass().getSimpleName());
            }
        });
    }

    private static void answer(final Context where,final NoteStore store,final Keys keys,final NoteStore.Contact them,
                               final byte[] page,final long revision,final boolean took) {
        if(where==null||them.agreement.length==0)return;
        // The same answer already waiting to go is not queued twice: one says it.
        final String key=them.address+" "+Courier.hex(page)+" "+revision+" "+took;
        // And the same answer that went out lately is not sent again: the same revision brought back again and again (by a
        // carrier, or collected from a home) cost a proof of work each time, and kept a phone at full speed (the owner,
        // 2026-10-19: "the app on Graphene is not syncing well and is slow to respond"; decision 211). By the device's
        // key, which stays when its address moves.
        final String once=fingerprint(them)+" "+Courier.hex(page)+" "+revision+" "+took;
        Long lately=ANSWERED.get(once);
        if(lately!=null&&System.currentTimeMillis()-lately<ANSWER_AGAIN){android.util.Log.i("Mininotes/Post","answered revision "+revision+" lately: not again");return;}
        if(!WAITING_ANSWERS.add(key))return;
        RECEIPTS.execute(()->{
            WAITING_ANSWERS.remove(key);
            try {
                MaximaNode node=Node.node(where);
                if(node==null)return;
                byte[] sealed=Envelope.seal(page,revision,System.currentTimeMillis(),
                    Receipt.wrap(took?Receipt.TOOK:Receipt.HAVE),keys.signing(),Keys.publicKey(them.agreement));
                int doors=Direct.WENT.get(),homes=Direct.LEFT.get(),shut=Direct.SHUT_HERE.get();
                MaximaSender.Result said;
                try{said=handTo(where,store,keys,node,them,sealed);}
                catch(Exception noWay) {
                    if(Unsent.of(noWay.getMessage())!=Unsent.Why.NOT_IN_REACH)throw noWay;
                    // Only between the owner's devices, with their door not heard here and no home taking it: not a
                    // failure but a wait, as for a note. Kept for the next round that finds a way, or they send the
                    // note again every half minute for as long as they run. And a copy with whoever can carry it,
                    // whose door may well be in reach when theirs is not.
                    int kept=KEPT.keep(them.address,page,revision,took,System.currentTimeMillis());
                    android.util.Log.i("Mininotes/Post","answer kept until they are in reach ("+kept+" kept)");
                    leaveWithCarriers(where,store,keys,node,them,Courier.ANSWER,page,revision,sealed);
                    return;
                }
                if(said!=null&&said.isOk()){KEPT.went(them.address,page,revision);ANSWERED.put(once,System.currentTimeMillis());}
                // By the relays with nothing ever coming back is an answer handed to where they used to be.
                android.util.Log.i("Mininotes/Post","answered them: revision "+revision+": "
                    +(said==null?"no answer":said.statusName)+Direct.road(doors,homes,shut,said!=null&&said.isOk()));
                // A note that was carried here comes from a device that may be gone by now; its answer
                // goes back the same way, or it would send the note again for ever.
                if(!there(them))leaveWithCarriers(where,store,keys,node,them,Courier.ANSWER,page,revision,sealed);
            } catch(Exception notNow) {
                android.util.Log.w("Mininotes/Post","could not answer: "+Unsent.reason(notNow));
            }
        });
    }

    /** Answers with no way to their device yet, while notes go only between the owner's devices. See {@link Outbox.Answers}. */
    private static final Outbox.Answers KEPT=new Outbox.Answers();
    // The words about an amber mark say when an answer to a device is kept here: see Waits.
    static{SyncStatus.kept=KEPT::count;}

    /** The kept answers, tried again. Blocking: the round that sends notes again calls it. */
    private static void sayKept(Context where,NoteStore store,Keys keys) {
        if(KEPT.size()==0)return;
        MaximaNode node=Node.node(where);
        if(node==null)return;
        int[] said=KEPT.sayAll(one->{
            NoteStore.Contact them=store.address(one.address);
            if(them==null||them.agreement.length==0)throw new IllegalStateException("Not paired with them.");
            MaximaSender.Result went=handTo(where,store,keys,node,them,Envelope.seal(one.page,one.revision,System.currentTimeMillis(),
                Receipt.wrap(one.took?Receipt.TOOK:Receipt.HAVE),keys.signing(),Keys.publicKey(them.agreement)));
            if(went==null||!went.isOk())throw new IllegalStateException("They could not be reached just now.");
        },System.currentTimeMillis());
        if(said[0]>0)android.util.Log.i("Mininotes/Post","kept answers said: "+said[0]+" went, "+said[1]+" still kept");
    }

    /** Whether what arrived gives this phone the thing again, later than it left. */
    private static boolean givenAgain(NoteStore store,Parcel.Sent parcel,long leftAt) {
        if(parcel==null||store.mySigningKey.isEmpty())return false;
        for(Parcel.Member one:parcel.members)
            if(store.mySigningKey.equals(one.key)&&one.level>Sharing.Level.GONE.said()&&one.changed>leftAt)return true;
        return false;
    }

    /**
     * "I have left this" - or "you are off this" - to one device, off the thread the node brought their
     * note on. Which of the two is {@code what}, one of the numbers in {@link Receipt}.
     */
    private static void tellOff(final Context where,final NoteStore store,final Keys keys,final NoteStore.Contact them,
                                final byte[] page,final long when,final int what) {
        if(where==null||them.agreement.length==0||what==0)return;
        ANSWERS.execute(()->{
            try{saidOff(where,store,keys,them,page,when,what);}
            catch(Exception notNow) {
                android.util.Log.w("Mininotes/Post","could not say who is off what: "+notNow.getClass().getSimpleName());
            }
        });
    }

    /**
     * @param when when it was decided, carried where a note's revision is: see {@link NoteStore#left} and
     *             {@link NoteStore#takenOff}
     */
    private static boolean saidOff(Context where,NoteStore store,Keys keys,NoteStore.Contact them,byte[] page,long when,
                                   int what) throws Exception {
        MaximaNode node=Node.node(where);
        if(node==null)return false;
        byte[] sealed=Envelope.seal(page,when,System.currentTimeMillis(),
            Receipt.wrap(what),keys.signing(),Keys.publicKey(them.agreement));
        MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
        android.util.Log.i("Mininotes/Post",(Receipt.removedScope(what)!=null?"told them they are off this: "
            :"told them this phone has left: ")+(said==null?"no answer":said.statusName));
        return said!=null&&said.isOk();
    }

    /**
     * Unfollow: everybody who has the thing is told this phone has left it, and then it is let go. Blocking.
     *
     * <p>Told first, while this phone still knows who they are. Let go whether or not anybody could be
     * told - a phone with no signal can still leave - because whoever did not hear will send the thing
     * again one day, and is told then. See {@link NoteStore#letGo}.
     *
     * @return how many were told
     */
    static int leave(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id) {
        if(kind==NoteStore.Branch.Kind.FILE)return leaveFile(where,store,keys,id);
        boolean note=kind==NoteStore.Branch.Kind.PAGE;
        if(!note&&kind!=NoteStore.Branch.Kind.COLLECTION&&kind!=NoteStore.Branch.Kind.BOOK)return 0;
        // Said at the level a 0.1 device calls it, by how deep it sits: a collection on Home, one inside that a book, a
        // note a note. Every build hears it that way, and finds the thing from a note out of it on its own shelves.
        Sharing.Scope scope=store.oldScopeOf(note?Sharing.Scope.PAGE:Sharing.Scope.THING,id);
        java.util.Set<String> who=store.everybodyIn(kind,id);
        // Any note out of it names it: the phone that hears finds the shelf from its own shelves.
        byte[] about=null;final long now=System.currentTimeMillis();
        for(Outbox.Page page:store.pagesUnder(kind,id)) {
            try{about=sixteen(page.id);break;}
            catch(IllegalArgumentException older){/* a note from before sharing names nothing */}
        }
        int told=0;
        // A collection deeper than three levels has no word to be left in yet. None are made yet - every notebook moved
        // from 0.1 is three levels deep - and saying it is for a later step; it is let go here all the same.
        if(scope==null)android.util.Log.i("Mininotes/Post","left a collection deeper than three levels: nobody is told yet");
        else if(about!=null)for(NoteStore.Contact them:store.addresses()) {
            if(!who.contains(them.address)||them.agreement.length==0||!doesSpeak(where,them))continue;
            try{if(saidOff(where,store,keys,them,about,now,Receipt.left(scope)))told++;}
            catch(Exception notNow) {
                android.util.Log.w("Mininotes/Post","could not say this phone has left: "+notNow.getClass().getSimpleName());
            }
        }
        store.letGo(kind,id,who,now);
        return told;
    }

    /**
     * A file shared on its own, left (decision 92): everybody who has it told, by its own sixteen bytes, and then let go,
     * the copy here kept as this device's own. Only devices that know files on their own are told: nothing else has it.
     */
    private static int leaveFile(Context where,NoteStore store,Keys keys,String id) {
        java.util.Set<String> who=store.everybodyIn(NoteStore.Branch.Kind.FILE,id);
        final long now=System.currentTimeMillis();
        int told=0;
        for(NoteStore.Contact them:store.addresses()) {
            if(!who.contains(them.address)||them.agreement.length==0||!knowsLoose(where,them))continue;
            try{if(saidOff(where,store,keys,them,sixteen(id),now,Receipt.LEFT_FILE))told++;}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not say this device has left a file: "+notNow.getClass().getSimpleName());}
        }
        store.letGo(NoteStore.Branch.Kind.FILE,id,who,now);
        return told;
    }

    /** Whatever this phone has left lately, said again to whoever was told. Blocking. See {@link NoteStore#leavings}. */
    static void leftAgain(Context where,NoteStore store,Keys keys) {
        for(NoteStore.Leaving one:store.leavings()) {
            try {
                NoteStore.Contact them=null;
                for(NoteStore.Contact known:store.addresses())if(known.address.equals(one.address))them=known;
                if(them==null||them.agreement.length==0||!doesSpeak(where,them))continue;
                // A file shared on its own is said only to a device that knows them (see Receipt.LOOSE).
                if(one.scope==Sharing.Scope.FILE&&!knowsLoose(where,them))continue;
                // Deeper than three levels, which nothing can say yet: see leave.
                if(Receipt.left(one.scope)==0){android.util.Log.i("Mininotes/Post","left a collection deeper than three levels: not said again");continue;}
                saidOff(where,store,keys,them,sixteen(one.note),one.at,Receipt.left(one.scope));
            } catch(Exception notNow) {
                android.util.Log.w("Mininotes/Post","could not say again that this phone has left: "
                    +notNow.getClass().getSimpleName());
            }
        }
    }

    /**
     * Somebody taken off something, told so: by the same road as leaving, the other way. Blocking.
     *
     * @param when when it was decided here, which their phone then makes its copy its own as of
     * @return whether they could be told now; they are told again at every opening for a week either way
     */
    static boolean removed(Context where,NoteStore store,Keys keys,Sharing.Scope scope,String target,
                           String address,long when) {
        // Said at the level a 0.1 device calls it, by how deep it sits, as leaving is. A collection deeper than three
        // levels has no word yet - none are made yet - and is only written down here: see leave.
        Sharing.Scope said=store.oldScopeOf(scope,target);
        if(said==null&&NoteStore.onShelf(scope))android.util.Log.i("Mininotes/Post","took somebody off a collection deeper than three levels: not told yet");
        if(Receipt.removed(said)==0)return false;
        NoteStore.Contact them=null;
        for(NoteStore.Contact known:store.addresses())if(known.address.equals(address))them=known;
        if(them==null||them.agreement.length==0||!doesSpeak(where,them))return false;
        // A file shared on its own names itself, and is said only to a device that knows them (decision 92).
        if(scope==Sharing.Scope.FILE) {
            if(!knowsLoose(where,them))return false;
            try{return saidOff(where,store,keys,them,sixteen(target),when,Receipt.REMOVED_FILE);}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not say they are off a file: "+notNow.getClass().getSimpleName());return false;}
        }
        // Any note out of it names it: the phone that hears finds the shelf from its own shelves.
        byte[] about=null;
        for(Outbox.Page page:store.pagesUnder(NoteStore.kindFor(scope),target)) {
            try{about=sixteen(page.id);break;}
            catch(IllegalArgumentException older){/* a note from before sharing names nothing */}
        }
        if(about==null)return false;
        try{return saidOff(where,store,keys,them,about,when,Receipt.removed(said));}
        catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not say they are off this: "+notNow.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Remove this person (decision 163): off everything shared with them that this device may change, told now where they
     * can be reached, then out of my people. Blocking.
     *
     * @return how many things they were taken off
     */
    static int removePerson(Context where,NoteStore store,Keys keys,String address) {
        List<Sharing.Rule> off=store.offEverything(address);
        for(Sharing.Rule rule:off) {
            try{changed(where,store,keys,kindOf(rule.scope),rule.target);}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","a removal was not sent: "+notNow.getClass().getSimpleName());}
        }
        removedAgain(where,store,keys);
        store.removeAddress(address);
        return off.size();
    }
    private static NoteStore.Branch.Kind kindOf(Sharing.Scope scope) {
        switch(scope) {
            case COLLECTION: case BOOK: case THING: return NoteStore.Branch.Kind.COLLECTION;
            case PAGE: return NoteStore.Branch.Kind.PAGE;
            case FILE: return NoteStore.Branch.Kind.FILE;
            default: return NoteStore.Branch.Kind.LIBRARY;
        }
    }

    /** Whoever this phone has taken off something lately, told again. Blocking. See {@link NoteStore#removals}. */
    static void removedAgain(Context where,NoteStore store,Keys keys) {
        for(NoteStore.Leaving one:store.removals()) {
            try {
                NoteStore.Contact them=null;
                for(NoteStore.Contact known:store.addresses())if(known.address.equals(one.address))them=known;
                if(them==null||them.agreement.length==0||!doesSpeak(where,them))continue;
                if(one.scope==Sharing.Scope.FILE&&!knowsLoose(where,them))continue;
                // Deeper than three levels, which nothing can say yet: see removed.
                if(Receipt.removed(one.scope)==0)continue;
                saidOff(where,store,keys,them,sixteen(one.note),one.at,Receipt.removed(one.scope));
            } catch(Exception notNow) {
                android.util.Log.w("Mininotes/Post","could not say again that they are off this: "
                    +notNow.getClass().getSimpleName());
            }
        }
    }

    /**
     * Devices known to understand an answer, by the key they sign with.
     *
     * <p>A question is five bytes no note ever asked for, and a build from before answers existed would
     * read them as a note written the oldest way and write them over somebody's writing. An answer is
     * safe because it only goes to a phone that asked for one; a question has to be safe some other way,
     * so it goes only to a phone that has already shown it knows what these bytes are.
     */
    private static void speaks(Context where,NoteStore.Contact them) {
        if(where==null||them==null||them.signing.length==0)return;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("answers",new java.util.HashSet<String>()));
        if(all.add(android.util.Base64.encodeToString(them.signing,android.util.Base64.NO_WRAP)))
            kept.edit().putStringSet("answers",all).apply();
    }

    /** Whether anything signed by them has ever been answered here. */
    static boolean heardFrom(Context where,NoteStore.Contact them){return doesSpeak(where,them);}

    private static boolean doesSpeak(Context where,NoteStore.Contact them) {
        if(where==null||them==null||them.signing.length==0)return false;
        return where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("answers",new java.util.HashSet<String>())
            .contains(android.util.Base64.encodeToString(them.signing,android.util.Base64.NO_WRAP));
    }

    // ---- carrying for devices that are not on at the same time: see Courier ------------------------------------

    /** When each device was last heard from directly, by the fingerprint of its key. For this run only. */
    private static final java.util.Map<String,Long> HEARD=new java.util.concurrent.ConcurrentHashMap<>();

    static String fingerprint(NoteStore.Contact them) {
        if(them==null||them.signing.length==0)return "";
        // Kept by the key's bytes: asked for each device in every loop of every round, each time a key read and hashed (decision 261).
        String key=java.util.Base64.getEncoder().encodeToString(them.signing);
        String known=FINGERPRINTS.get(key);
        if(known!=null)return known;
        try {
            String made=Courier.hex(Envelope.fingerprint(Keys.publicKey(them.signing)));
            if(FINGERPRINTS.size()>256)FINGERPRINTS.clear();
            FINGERPRINTS.put(key,made);
            return made;
        } catch(Exception unreadable){return "";}
    }
    private static final java.util.Map<String,String> FINGERPRINTS=new java.util.concurrent.ConcurrentHashMap<>();

    /** Whether a device's fingerprint is this device's own. Never for one that cannot be read. */
    static boolean isSelf(String theirs,String mine){return mine!=null&&!mine.isEmpty()&&mine.equalsIgnoreCase(theirs);}

    /** This device's own fingerprint, as the others know it; empty where it cannot be read. */
    private static String myFingerprint(Keys keys) {
        try{return Courier.hex(Envelope.fingerprint(keys.signing().getPublic()));}catch(Exception unreadable){return "";}
    }

    /** Whether a device was heard from so lately that it is taken to be there. */
    private static boolean there(NoteStore.Contact them) {
        Long at=HEARD.get(fingerprint(them));
        if(at!=null&&System.currentTimeMillis()-at<Courier.THERE)return true;
        // Or collecting from this PC, which is as good as there: what is kept here for it is taken within minutes.
        Home.Host here=Node.host();
        return here!=null&&here.collecting(fingerprint(them),System.currentTimeMillis());
    }

    /** Whether anything was heard from a device lately, for People and devices (decision 96). */
    static boolean heardLately(NoteStore.Contact them){return there(them);}

    /** A device whose notes have said its build carries - and so may be left things, and brought them. */
    private static void carries(Context where,NoteStore.Contact them) {
        if(where==null||them==null||them.signing.length==0)return;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("carries",new java.util.HashSet<String>()));
        if(all.add(android.util.Base64.encodeToString(them.signing,android.util.Base64.NO_WRAP)))
            kept.edit().putStringSet("carries",all).apply();
    }

    private static boolean doesCarry(Context where,NoteStore.Contact them) {
        if(where==null||them==null||them.signing.length==0)return false;
        return where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("carries",new java.util.HashSet<String>())
            .contains(android.util.Base64.encodeToString(them.signing,android.util.Base64.NO_WRAP));
    }

    /** Sealed bytes handed to a device, where the network knows it. Whether the network took them. */
    private static boolean hand(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them,byte[] sealed) throws Exception {
        MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
        return said!=null&&said.isOk();
    }

    /**
     * Sealed bytes handed to a paired device: to the peer where the network knows it - its own doors first,
     * then its home, then the relays (see {@link Direct#send}) - and only where it has never been introduced,
     * to the address off its code.
     */
    private static MaximaSender.Result handTo(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them,
                                              byte[] sealed) throws Exception {
        com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,them.contact);
        return reach!=null
            ?Direct.send(node,reach,APPLICATION,sealed,homeFor(where,store,keys,node,them),Node.helpers())
            :node.sendRaw(routable(node,them.address),APPLICATION,sealed);
    }

    /**
     * The copies left with carriers lately, by the device they are for, the thing and its kind: each try at sending a note
     * to a device that is away used to leave another copy of the same revision with every carrier (decision 257).
     */
    static final Courier.Lately LEFT=new Courier.Lately(Courier.LEAVE_AGAIN);
    /** The carriers told lately that a thing they brought here was collected, by carrier, thing and kind (decision 257). */
    static final Courier.Lately COLLECTED_SAID=new Courier.Lately(Courier.COLLECTED_AGAIN);
    /** The receipts for carriers waiting their turn: one for each carrier, thing and kind, for the newest revision (decision 257). */
    static final Courier.Queued COLLECTED_QUEUED=new Courier.Queued();
    /** Whose things are waiting to be brought, so a burst of arrivals queues one bringing, not one each (decision 257). */
    private static final java.util.Set<String> BRINGING_SOON=java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** What is held for one device, brought on the answers' thread, queued once however often it is asked for. */
    private static void bringSoon(Context where,NoteStore store,Keys keys,String who) {
        if(who==null||!BRINGING_SOON.add(who))return;
        ANSWERS.execute(()->{BRINGING_SOON.remove(who);bring(where,store,keys,who);});
    }

    /**
     * A copy of something sealed for {@code them}, left with the devices that can carry it: every other
     * paired device whose build has said it carries, those heard from lately first. Nothing is waited for
     * and nothing counted - this is on top of the sending, which goes on as it did. The same revision is
     * left once in {@link Courier#LEAVE_AGAIN}, however often it is sent: the carriers keep it till it is
     * collected.
     */
    private static void leaveWithCarriers(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them,
                                          int sort,byte[] page,long revision,byte[] inner) {
        if(store==null||!Courier.fits(inner))return;
        String once=fingerprint(them)+" "+Courier.hex(page)+" "+sort;
        if(!LEFT.take(once,revision,System.currentTimeMillis()))return;
        int left=0;
        try {
            byte[] forWhom=Envelope.fingerprint(Keys.publicKey(them.signing));
            List<NoteStore.Contact> carriers=new ArrayList<>();
            for(NoteStore.Contact one:store.addresses())
                if(one.paired()&&sendsTo(store,one)&&!one.address.equals(them.address)&&!java.util.Arrays.equals(one.signing,them.signing)&&doesCarry(where,one))carriers.add(one);
            carriers.sort((a,b)->Boolean.compare(there(b),there(a)));
            for(NoteStore.Contact carrier:carriers) {
                if(left>=Courier.CARRIERS)break;
                try {
                    byte[] sealed=Envelope.seal(page,revision,System.currentTimeMillis(),Courier.leave(sort,forWhom,inner),
                        keys.signing(),Keys.publicKey(carrier.agreement));
                    if(hand(where,store,keys,node,carrier,sealed))left++;
                } catch(Exception notThisOne){/* the next carrier, or none */}
            }
            if(left>0)android.util.Log.i("Mininotes/Post","left a copy with "+left+" device(s) that can carry it");
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not leave a copy to be carried: "+notNow.getClass().getSimpleName());
        } finally {if(left==0)LEFT.giveBack(once,revision);}
    }

    /**
     * Something left here to be carried to somebody else, or brought here by whoever carried it.
     *
     * <p>Left: kept, if it is for a device paired here whose build knows what it will be brought - and
     * brought at once if they are there. Brought: opened as if it had come straight from whoever wrote it,
     * which is who signed it, and then the device that brought it is told it can let go.
     */
    private static Landed carried(Context where,NoteStore store,Keys keys,NoteStore.Contact from,
                                  Envelope.Opened opened,Courier.Said said) {
        if(said.kind==Courier.LEAVE) {
            final String forWhom=Courier.hex(said.forWhom);
            NoteStore.Contact them=null;
            for(NoteStore.Contact one:store.addresses())if(one.paired()&&forWhom.equals(fingerprint(one)))them=one;
            if(them==null||forWhom.equals(fingerprint(from))||!doesCarry(where,them)) {
                // Which of the three, and a note or an answer: a copy left here for a device this one is not paired
                // with is the usual one, and harmless; one "for" this very device would be a fault of the sender's.
                String why=forWhom.equals(fingerprint(from))?"it is for the device that left it"
                    :them!=null?"its build has not said it carries"
                    :forWhom.equals(myFingerprint(keys))?"it is for this device, which a copy never is":"not paired here with the device it is for";
                android.util.Log.i("Mininotes/Post","not carried: for a device this one cannot bring it to ("
                    +(said.sort==Courier.NOTE?"a note":"an answer")+", "+said.inner.length+" bytes; "+why+")");
                return new Landed(null,null);
            }
            // Already held at that revision or newer: nothing new to keep, to say or to bring (decision 257).
            int kept=store.carry(fingerprint(from),forWhom,Courier.hex(opened.page),said.sort,opened.revision,said.inner);
            if(kept==Courier.ALREADY)return new Landed(null,null);
            android.util.Log.i("Mininotes/Post",kept==Courier.CARRYING?"carrying something for another device":"not carried: there is no room");
            if(kept==Courier.CARRYING&&there(them))bringSoon(where,store,keys,forWhom);
            return new Landed(null,null);
        }
        Landed landed=arrived(where,store,keys,said.inner,true);
        // Opened, written down, or found not to be anything: either way there is nothing more to bring.
        final NoteStore.Contact carrier=from;final int collected=Receipt.collected(said.sort);
        final byte[] page=opened.page;final long revision=opened.revision;
        // Said once to each carrier for the thing, up to this revision, in a while: a carrier lets go of every copy it holds
        // for this device up to the revision it is told, so one receipt does for all. Each copy brought again used to cost
        // a receipt sealed and mined here (Graphene, 2026-10-25, decision 257); a receipt lost is said again once the while
        // is out, the next time the carrier brings it.
        final String once=fingerprint(carrier)+" "+Courier.hex(page)+" "+said.sort;
        if(!COLLECTED_SAID.take(once,revision,System.currentTimeMillis()))return landed;
        // One waiting at a time for each carrier and thing: a newer revision brought meanwhile is what it says.
        if(!COLLECTED_QUEUED.queue(once,revision))return landed;
        ANSWERS.execute(()->{
            boolean went=false;long newest=COLLECTED_QUEUED.run(once,revision);
            try {
                // Something just arrived, so the node is up; where it is not, nothing is started for this.
                MaximaNode node=Node.running()?Node.node(where):null;
                if(node==null)return;
                went=hand(where,store,keys,node,carrier,Envelope.seal(page,newest,System.currentTimeMillis(),Receipt.wrap(collected),
                    keys.signing(),Keys.publicKey(carrier.agreement)));
            } catch(Exception notNow){/* it is brought again, and collected again */}
            finally {if(!went)COLLECTED_SAID.giveBack(once,newest);}
        });
        return landed;
    }

    // ---- the PC as its owner's host: see Home --------------------------------------------------------------

    /**
     * Where a message for {@code them} can be left for them to collect, tried once their own doors have not
     * taken it. On the PC, here - if they are one of its owner's devices and collecting from it. On a phone, at
     * any paired device whose door is in reach and has a home behind it, which takes it only for a device of
     * its owner's that is collecting from it: so the phone need not know which home is whose, and nothing about
     * the owner's home travels to anybody.
     */
    private static Direct.Leave homeFor(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them) {
        if(store==null||keys==null||them==null||them.signing.length==0)return null;
        final String forWhom=fingerprint(them);
        if(forWhom.isEmpty())return null;
        return data->{
            long now=System.currentTimeMillis();
            KeyPair mine=keys.signing();
            Home.Host here=Node.host();
            if(here!=null) {
                boolean kept=here.keepHere(Courier.hex(Envelope.fingerprint(mine.getPublic())),forWhom,data,now)==Home.OK;
                if(kept)android.util.Log.i("Mininotes/Post","kept here for one of your devices to collect");
                return kept;
            }
            if(Node.A_PC)return false;
            byte[] whose=Envelope.fingerprint(Keys.publicKey(them.signing));
            // This device's own first: the owner's PC is the home most likely to be theirs. Whoever else is asked
            // learns what a relay would - who it is for, and how big - and can open none of it.
            List<NoteStore.Contact> homes=new ArrayList<>(store.addresses());
            homes.sort((one,other)->Boolean.compare(other.mine,one.mine));
            for(NoteStore.Contact home:homes) {
                if(!home.paired()||home.contact==null||home.contact.trim().isEmpty()||java.util.Arrays.equals(home.signing,them.signing))continue;
                com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,home.contact);
                for(String door:Direct.firstTries(node.lanAddressFor(home.contact),reach==null?null:new ArrayList<>(reach.addresses),home.contact)) {
                    String asking=door+"|"+forWhom;
                    if(Home.resting(door,now)||Home.resting(asking,now))continue;
                    try {
                        int said=Home.deposit(door,home.contact,Keys.publicKey(home.agreement),mine,whose,data);
                        if(said==Home.OK) {
                            android.util.Log.i("Mininotes/Post","left at a home, for them to collect");
                            return true;
                        }
                        // A no is about this device - not one of that home's owner's, or not collecting just now - and
                        // the next device may be; a door with no home behind it, or one not open just now, is about all.
                        Home.rest(said==Home.NO_HOME||said==Home.BUSY?door:asking,now,Home.restFor(said));
                    } catch(java.io.IOException|RuntimeException notThere) {
                        Home.rest(door,now,Home.UNREACHABLE);
                        continue;
                    }
                    break;
                }
            }
            return false;
        };
    }

    /** What the PC's home needs of its notebook: its key, who is paired, and which of them are "My device". */
    private static Home.Notebook notebook(NoteStore store,Keys keys) throws Exception {
        java.util.Set<String> paired=new java.util.HashSet<>(),mine=new java.util.HashSet<>();
        for(NoteStore.Contact one:store.addresses()) {
            String who=one.paired()?fingerprint(one):"";
            if(who.isEmpty())continue;
            paired.add(who);
            if(one.mine)mine.add(who);
        }
        return new Home.Notebook(keys.agreement().getPrivate(),paired,mine,store);
    }

    /**
     * What waits for this phone at its owner's PC, collected: from every device marked "My device" whose door
     * is in reach - on this network, or at the public address it proved - and has a home behind it. Each thing
     * is taken in as if it had just arrived. Blocking; called every round, and a door that is not there or has
     * no home behind it is let be for a while, so a round with nothing to collect costs one small question.
     *
     * @return how many things were collected
     */
    static int collect(Context where,NoteStore store,Keys keys) {
        if(Node.A_PC||!Node.running())return 0;
        MaximaNode node=Node.node(where);
        if(node==null)return 0;
        int got=0;
        long now=System.currentTimeMillis();
        for(NoteStore.Contact home:store.addresses()) {
            if(!home.mine||!home.paired()||home.contact==null||home.contact.trim().isEmpty())continue;
            com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,home.contact);
            for(String door:Direct.firstTries(node.lanAddressFor(home.contact),reach==null?null:new ArrayList<>(reach.addresses),home.contact)) {
                if(Home.resting(door,now))continue;
                try {
                    Home.Collected said=Home.collect(door,home.contact,Keys.publicKey(home.agreement),keys.signing(),Node::deliver);
                    got+=said.items;
                    if(said.status==Home.OK){Node.collected(now);break;}
                    Home.rest(door,now,Home.restFor(said.status));
                    break;
                } catch(Exception notThere) {
                    Home.rest(door,now,Home.UNREACHABLE);
                }
            }
        }
        if(got>0)android.util.Log.i("Mininotes/Post","collected "+got+" thing(s) from your PC");
        return got;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean BRINGING=new java.util.concurrent.atomic.AtomicBoolean();

    /**
     * What this device holds for others, brought to them. Blocking.
     *
     * @param only one device's fingerprint - which has just been heard from, so everything held for it goes
     *             now - or null for everything whose turn it is
     */
    static void bring(Context where,NoteStore store,Keys keys,String only) {
        if(!BRINGING.compareAndSet(false,true))return;
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            List<NoteStore.Carried> held=store.carried(only);
            if(held.isEmpty())return;
            java.util.Map<String,NoteStore.Contact> byKey=new java.util.HashMap<>();
            for(NoteStore.Contact one:store.addresses())if(one.paired())byKey.put(fingerprint(one),one);
            long now=System.currentTimeMillis();int brought=0;
            for(NoteStore.Carried one:held) {
                NoteStore.Contact them=byKey.get(one.recipient);
                if(them==null||!doesCarry(where,them))continue;
                // Just heard from: now, unless it went a moment ago. Otherwise, when its turn comes.
                if(only!=null?now-one.tried<JUST_NOW&&now>=one.tried:!Courier.due(one.tried,one.tries,now))continue;
                if(bringOne(store,one,()->{
                    byte[] page=new byte[16];
                    for(int at=0;at<16;at++)page[at]=(byte)Integer.parseInt(one.page.substring(at*2,at*2+2),16);
                    byte[] sealed=Envelope.seal(page,one.revision,now,Courier.bring(one.sort,one.bytes),
                        keys.signing(),Keys.publicKey(them.agreement));
                    hand(where,store,keys,node,them,sealed);
                }))brought++;
            }
            if(brought>0)android.util.Log.i("Mininotes/Post","brought "+brought+" thing(s) carried for another device");
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not bring what is carried: "+notNow.getClass().getSimpleName());
        } finally {BRINGING.set(false);}
    }

    /** One way of bringing a carried thing; what throws stops it. */
    interface Bringing {void go() throws Exception;}

    /**
     * One carried thing brought, its try counted whether it went or threw (decision 303). A try that threw was not counted, so
     * it was due again at the very next round: one thing held for the PC while it slept, sealed and mined for every road twice
     * a minute for six hours (Graphene, 2026-10-31, 2 h 42 min of CPU). Counted, it waits its turn like any other.
     *
     * @return whether it went without throwing
     */
    static boolean bringOne(NoteStore store,NoteStore.Carried one,Bringing going) {
        try {
            going.go();
            return true;
        } catch(Exception notThisOne) {
            android.util.Log.w("Mininotes/Post","could not bring a carried thing: "+notThisOne.getClass().getSimpleName()+", tried again in its turn");
            return false;
        } finally {store.broughtAgain(one);}
    }

    /**
     * Everybody who has anything in this thing, asked to send whatever they have for this phone. Blocking.
     *
     * @return how many were asked
     */
    static int ask(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id) {
        int asked=0;
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return 0;
            java.util.Set<String> who=store.everybodyIn(kind,id);
            for(NoteStore.Contact them:store.addresses()) {
                if(!who.contains(them.address)||them.agreement.length==0||!doesSpeak(where,them))continue;
                try {
                    // About this one note where it is a note that is being synced, so they send it whole.
                    byte[] about=new byte[16];
                    if(kind==NoteStore.Branch.Kind.PAGE)try{about=sixteen(id);}catch(IllegalArgumentException old){/* everything */}
                    byte[] sealed=Envelope.seal(about,0,System.currentTimeMillis(),
                        Receipt.wrap(Receipt.ASK),keys.signing(),Keys.publicKey(them.agreement));
                    MaximaSender.Result said=handTo(where,store,keys,node,them,sealed);
                    android.util.Log.i("Mininotes/Post","asked them for what they have: "
                        +(said==null?"no answer":said.statusName));
                    if(said!=null&&said.isOk())asked++;
                } catch(Exception notNow) {
                    android.util.Log.w("Mininotes/Post","could not ask: "+notNow.getClass().getSimpleName());
                }
            }
        } catch(Exception notNow){/* nobody asked; what they owe still comes when its turn does */}
        return asked;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean TRYING=
        new java.util.concurrent.atomic.AtomicBoolean();
    /** When a phone's round last sent what it owes and never handed over (decision 294). */
    private static final java.util.concurrent.atomic.AtomicLong OWED_TRIED=new java.util.concurrent.atomic.AtomicLong();

    /**
     * Whatever was handed over and never answered, sent again if it is time. Blocking, and never two at
     * once: called from the node's own upkeep, every half minute, for as long as the process lives.
     *
     * <p>{@link Outbox#due} decides what and when. Only a revision that already went once is sent again -
     * a note written in since is the business of whatever that note is set to, which may be "when I ask".
     */
    static void again(Context where,NoteStore store,Keys keys) {
        if(!TRYING.compareAndSet(false,true))return;
        try {
            // Files first, on their own thread, so a file waiting does not wait behind everything below (decision 304: a
            // screenshot waited a minute while this round sent again what others were owed). Again at the end, for what came meanwhile.
            travelSoon(where,store,keys);
            // Whose announcements on the local network are believed: the devices paired now. See Direct.
            Node.pairedWith(Direct.paired(store.addresses()));
            // And what a device heard anew there is owed and could not be given before, given then. See Node.metHere.
            Node.onMetHere(key->sayOpenAgain(where,store,keys,key));
            // The PC keeps what comes for its owner's phones; a phone collects what its PC keeps for it, first,
            // so that what it collects is in before anything is sent again. See Home.
            if(Node.A_PC)Node.hosting(notebook(store,keys));else collect(where,store,keys);
            // Answers that had no way to their device, said now if one has turned up.
            sayKept(where,store,keys);
            // That what waited here to be accepted was accepted, to whoever was told it waits. See Asking.
            sayAccepted(where,store,keys);
            // That this device takes files sent on their own, said to whoever has not been told this run.
            tellTakesFiles(where,store,keys);
            // That this build knows about persons, and, to the owner's own devices that do too, the card. See Persons.
            tellPersons(where,store,keys);
            sendCards(where,store,keys);
            // The relays chosen here, to the owner's devices that have not had them since (decision 316).
            sendRelays(where,store,keys);
            // That this build knows about trees, said as persons is. See Things.
            tellTrees(where,store,keys);
            // And that it knows files on their own, said the same way. See Sleeve.
            tellLoose(where,store,keys);
            // And that it reads a refusal, said the same way (decision 109). See Receipt.REFUSED.
            tellRefusals(where,store,keys);
            // And that it reads a help request, said the same way (decision 113). See Help and Receipt.HELP.
            tellHelp(where,store,keys);
            // That it takes a person's own Parlons! address, to everybody paired; and yours, to those that do (decision 147).
            tellOwnParlons(where,store,keys);
            sendMyParlons(where,store,keys,false);
            // And a person's own picture, the same way (decision 158).
            tellOwnFace(where,store,keys);
            sendMyFace(where,store,keys,false);
            // And who this device belongs to, the same way, so a contact sees one person for their devices (decision 163).
            tellAboutMe(where,store,keys);
            sendAboutMe(where,store,keys,false);
            // And that it reads "not here" on a list, the same way (decision 293).
            tellNotHere(where,store,keys);
            // And that it reads only what changed in a note, the same way (decision 320).
            tellChanges(where,store,keys);
            // Messages (decision 145) were withdrawn the next day (decision 147): no longer said to anybody, so no build starts
            // sending one; one that comes from a 0.3.036 build is still kept and answered, so it is never read as a note.
            // That it reads groups, to the owner's own devices; what groups give, given, where anything they depend on
            // changed since (a device paired, a thing arrived); and the card, where it changed. See Groups.
            tellGroups(where,store,keys);
            sent(where,store,keys,store.applyGroups(),"given by a group, or taken away");
            sendGroups(where,store,keys);
            // That it reads stamps on Minima, to the owner's own devices; and the card, where it changed (decision 252). See
            // StampCard.
            tellStamps(where,store,keys);
            sendStamps(where,store,keys);
            // That it links devices of mine introduced to it, to the owner's own devices; and what waits to be introduced
            // (decision 268). See Introducing.
            tellLinksMine(where,store,keys);
            introduce(where,store,keys);
            adoptVouched(where,store,keys);
            // And the stamps of what is shared with somebody else, with it, where they changed since it last went (decision 251).
            sendStampsShared(where,store,keys);
            // That it reads Parlons! addresses, to the owner's own devices, which are sent one only when the owner says so.
            // See Parlons.
            tellParlons(where,store,keys);
            // That it reads the name we pick for a person, to the owner's own devices. A name goes only when it is saved
            // for all of them (decision 124). See Called.
            tellCalled(where,store,keys);
            // Whoever a list linked this device with and has not answered, asked again when their turn comes. See Linking.
            linkAgain(where,store,keys);
            // What did not reach one of my devices when it was saved, sent again by itself (the owner, 2026-11-07). See Later.
            sendLater(where,store,keys);
            // A phone's notes owed and never handed over (their send failed: nobody reachable) went again only when the app was
            // opened or written in; the PC's queue tries them every round. Once a minute here too (decision 294).
            if(!Node.A_PC){long now=System.currentTimeMillis();long was=OWED_TRIED.get();
                if(now-was>=60_000&&OWED_TRIED.compareAndSet(was,now))send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING);}
            // A device not heard from lately is tried a quarter of an hour apart; it is sent everything the moment it is (decision 301).
            java.util.Set<String> quiet=new java.util.HashSet<>();
            for(NoteStore.Contact one:store.addresses())if(one.paired()&&!there(one))quiet.add(one.address);
            List<Outbox.Handed> due=Outbox.due(store.handed(),store.sent(),store.revisions(),
                System.currentTimeMillis(),quiet);
            java.util.Set<String> pages=new java.util.LinkedHashSet<>();
            for(Outbox.Handed one:due)pages.add(one.page);
            for(String page:pages) {
                Done done=send(where,store,keys,NoteStore.Branch.Kind.PAGE,page);
                android.util.Log.i("Mininotes/Post","not answered, so sent again: "+done.sent+" went, "
                    +done.failed+" did not");
            }
            // And the collections that go on their own: whoever is owed one, and whatever was not answered once its
            // turn to go again has come. Counted in the log only where any went.
            Done alone=cartons(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,null,false);
            if(alone.sent>0)android.util.Log.i("Mininotes/Post","collections on their own: "+alone.sent+" went, "+alone.failed+" did not");
            // And the files shared on their own, the same way: owed, or not answered once their turn has come (decision 92).
            Done files=sleeves(where,store,keys,null,null,false);
            if(files.sent>0)android.util.Log.i("Mininotes/Post","files on their own: "+files.sent+" went, "+files.failed+" did not");
            // And whatever is carried for others, brought again when its turn comes.
            if(!store.carried(null).isEmpty())bring(where,store,keys,null);
            // And files: going up, being fetched, and said again to whoever has not said they have them.
            travelSoon(where,store,keys);
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not try again: "+notNow.getClass().getSimpleName());
        } finally {TRYING.set(false);}
    }

    // ---- the files kept with a shared note: see Enclosure ------------------------------------------------------

    /**
     * Told which note's files have changed here - one fetched, one taken out by somebody else's list, somebody
     * saying they have one - so a page showing them can draw them again. Called off the interface thread.
     */
    static volatile java.util.function.Consumer<String> filesMoved;

    private static void moved(String note) {
        java.util.function.Consumer<String> told=filesMoved;
        if(told!=null&&note!=null)try{told.accept(note);}catch(RuntimeException notNow){/* drawn next time */}
    }

    /**
     * One file at a time, on a thread of its own: sending a file up can take the best part of a minute and
     * fetching one as long, and neither may hold up a note being sent or an answer being said.
     */
    private static final java.util.concurrent.ExecutorService FILES=
        java.util.concurrent.Executors.newSingleThreadExecutor(work->{
            Thread one=new Thread(work,"mininotes-files");one.setDaemon(true);return one;});
    private static final java.util.concurrent.atomic.AtomicBoolean TRAVELLING=new java.util.concurrent.atomic.AtomicBoolean();
    /** Asked for while a round was under way: another follows it, so nothing asked for waits a whole beat. */
    private static final java.util.concurrent.atomic.AtomicBoolean AGAIN=new java.util.concurrent.atomic.AtomicBoolean();

    /** A round of file work soon: now, or straight after the one under way. */
    static void travelSoon(final Context where,final NoteStore store,final Keys keys) {
        if(where==null||store==null||keys==null)return;
        if(!TRAVELLING.compareAndSet(false,true)){AGAIN.set(true);return;}
        try{FILES.execute(()->{
            try{do{AGAIN.set(false);travel(where,store,keys);}while(AGAIN.get());}
            finally{TRAVELLING.set(false);}
        });}
        catch(RuntimeException full){TRAVELLING.set(false);}
    }

    /**
     * A file added to a note or taken out of one here. Whoever has the note is sent its list now, rather than
     * the next time somebody writes in it; what is to go up then goes up, and the list goes again saying where.
     * Blocking: the worker calls it.
     */
    static void filesChanged(Context where,NoteStore store,Keys keys,String note) {
        try {
            // A collection's files go in its carton, to everybody it reaches (see NoteStore.goesWith).
            if(note!=null&&store.keptWith(note)==NoteStore.Branch.Kind.COLLECTION)cartonToAll(where,store,keys,note);
            else if(note!=null&&!store.everybodyIn(NoteStore.Branch.Kind.PAGE,note).isEmpty())
                send(where,store,keys,NoteStore.Branch.Kind.PAGE,note,null,true);
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not send the list of a note's files: "+notNow.getClass().getSimpleName());
        }
        travelSoon(where,store,keys);
    }

    /**
     * A file renamed or replaced here by somebody who may (decision 93): its sleeve, to whoever has it on its own, now, as
     * every sleeve goes (see {@link #sleeves}); and a round of file work, which sends a new version up, after which its
     * sleeve goes again saying where. Blocking.
     */
    static Done fileChanged(Context where,NoteStore store,Keys keys,String file) throws Exception {
        Done done=sleeves(where,store,keys,file,null,true);
        travelSoon(where,store,keys);
        return done;
    }

    /**
     * One round: pieces of what is no longer kept let go, what is to go up sent up, what is waiting fetched,

     * and the list said again to whoever has not said they have its files. Nothing here throws out: a round
     * that fails is tried again at the next.
     */
    private static void travel(Context where,NoteStore store,Keys keys) {
        try {
            for(java.util.Map.Entry<String,String> gone:store.publishedOrphans().entrySet()) {
                Node.forget(where,gone.getValue());
                store.forgetPublished(gone.getKey());
            }
            long now=System.currentTimeMillis();
            List<NoteStore.Going> up=store.toPublish(now);
            List<NoteStore.Incoming> down=store.toFetch(now);
            java.util.Map<String,java.util.Set<String>> tell=store.toTell(now);
            List<NoteStore.Transfer> sendings=store.transfersDue(now);
            if(up.isEmpty()&&down.isEmpty()&&tell.isEmpty()&&sendings.isEmpty())return;
            // Never the reason a node is started: files go when the node is up for everything else.
            if(!Node.running())return;
            com.eurobuddha.maxima.core.media.MediaService media=Node.media(where);
            if(media==null)return;
            // Every file offered at this device's door first, each one's list or sleeve going to the devices on this network,
            // and only then up to the relays one by one (decision 285): one at a time from start to end, two screenshots let go
            // together on Temp reached the laptop beside the phone 36 s apart, the second waiting for the first to go up.
            java.util.Map<String,String> doors=new java.util.HashMap<>();
            for(NoteStore.Going one:up)doors.put(one.file.id,offerNear(where,store,keys,one));
            List<NoteStore.Going> going=new ArrayList<>(up);
            for(int at=0;at<going.size();at++) {
                NoteStore.Going one=going.get(at);
                goUp(where,store,keys,media,one,doors.get(one.file.id));
                // One let go while this round was under way: offered at the door now, not after every upload before it.
                if(AGAIN.get())for(NoteStore.Going more:store.toPublish(System.currentTimeMillis()))
                    if(!doors.containsKey(more.file.id)){doors.put(more.file.id,offerNear(where,store,keys,more));going.add(more);}
            }
            for(NoteStore.Incoming one:down)fetch(where,store,keys,media,one);
            // Files sent on their own: offered, or fetched. See Drop.
            for(NoteStore.Transfer one:sendings)if(one.out)offer(where,store,keys,media,one);else fetchSending(where,store,keys,media,one);
            for(java.util.Map.Entry<String,java.util.Set<String>> note:tell.entrySet()) {
                // A note's list goes again in the note; a collection's in its carton.
                NoteStore.Branch.Kind kept=store.keptWith(note.getKey());
                for(String address:note.getValue()) {
                    Done done=kept==NoteStore.Branch.Kind.PAGE?send(where,store,keys,NoteStore.Branch.Kind.PAGE,note.getKey(),address,true)
                        :cartonTo(where,store,keys,note.getKey(),address);
                    // Counted as told whether or not it went, so a device that never says it has a file is told
                    // only a few times; a send that went has counted it already.
                    if(done.sent==0) {
                        List<String> ids=new ArrayList<>();
                        for(NoteStore.Held file:store.filesOf(kept,note.getKey()))ids.add(file.id);
                        store.told(ids,address);
                    }
                }
            }
        } catch(Throwable notNow) {
            android.util.Log.w("Mininotes/Post","files: a round failed: "+notNow.getClass().getSimpleName());
        }
    }

    /**
     * Near first (decision 96): a device on this network is not made to wait for two relays to take every piece and then
     * fetch them back from across the internet. The pieces are kept at this device's door first, as only between the
     * owner's devices they always are, and its list or sleeve goes to that device now; the upload in {@link #goUp} follows
     * for everybody else, and for that device too should the door not answer it.
     *
     * @return the manifest kept at the door, empty where there is none
     */
    private static String offerNear(Context where,NoteStore store,Keys keys,NoteStore.Going one) {
        String door=store.atDoorOnly(one.file.id)?one.manifest:"";
        try {
            List<String> near=nearFor(where,store,one.file);
            if(near.isEmpty()||!Routes.doorFirst(Node.helpers(),!one.manifest.isEmpty(),true,near.size()))return door;
            byte[] plain=store.bytesOf(one.file);
            if(Routes.doorFirst(Node.helpers(),!one.manifest.isEmpty(),Enclosure.travels(plain.length),near.size()))
                door=atTheDoor(where,store,keys,one.file,plain,near);
        } catch(Exception notAtTheDoor) {
            android.util.Log.w("Mininotes/Post","files: could not offer one at this device's door: "+notAtTheDoor.getClass().getSimpleName());
        }
        return door;
    }

    /**
     * Where a file is now, said to one device that could not get it (decision 289): the list of the note it is kept in, the
     * card of its collection where that goes to them, and its own sleeve where it is shared on its own, each whole, so the
     * place they hold is the place it is.
     */
    private static void sayWhereFileIs(Context where,NoteStore store,Keys keys,NoteStore.Contact them,NoteStore.Held file) {
        try {
            if(file.held==NoteStore.Branch.Kind.PAGE)send(where,store,keys,NoteStore.Branch.Kind.PAGE,file.note,them.address,true);
            else {
                if(!NoteStore.home(file.note)&&store.goesWith(file.note).contains(them.address))cartonTo(where,store,keys,file.note,them.address);
                sleeves(where,store,keys,file.id,them.address,true);
            }
            android.util.Log.i("Mininotes/Post","told them again where a file is");
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not tell them again where a file is: "+notNow.getClass().getSimpleName());
        }
    }

    /** One file sent up, and its note's list sent again saying where. {@code door}: what {@link #offerNear} kept at the door. */
    private static void goUp(Context where,NoteStore store,Keys keys,com.eurobuddha.maxima.core.media.MediaService media,
                             NoteStore.Going one,String door) {
        com.eurobuddha.maxima.core.media.MediaManifest made=null;
        if(door==null)door="";
        try {
            byte[] plain=store.bytesOf(one.file);
            made=media.publish(plain,one.file.kind);
            // Up only if a relay has it. A phone behind a router can hand its pieces to nobody directly, so a
            // list saying they are only here would send everybody to a place they cannot reach. A PC's own
            // door is in the list too once it is proved open, and it does not count: a router that restarts
            // closes it, and the pieces have to be somewhere that is still there.
            // Only between the owner's devices there is no relay to have it, and that is the choice: the pieces
            // are kept here, pinned, and the other devices take them from this device's door when they can
            // reach it. Offered, then, not gone: the card says it waits for them until they say they have it.
            boolean helpers=Node.helpers();
            if(helpers&&!Direct.relayed(made.sources,own(where)))throw new IllegalStateException("No relay took it.");
            store.published(one.file.id,made.encode());
            if(!one.manifest.isEmpty())Node.forget(where,one.manifest);
            // What was offered at the door alone goes: everybody is told where it went up just below, and a device on this
            // network still fetching the door's pieces is told the new place in the same breath, and tries again there.
            if(!door.isEmpty()&&!door.equals(one.manifest))Node.forget(where,door);
            android.util.Log.i("Mininotes/Post",helpers?"files: one went up, "+made.chunkIds.size()+" piece(s) on "+made.sources.size()+" relay(s)"
                :"files: one is offered from this device's door, "+made.chunkIds.size()+" piece(s)");
            moved(one.file.note);
            // The list that says where it is: a note's in its parcel, a collection's in its carton; and a file shared on its
            // own, in its sleeve, which going up has made owed again (decision 92).
            if(one.file.held==NoteStore.Branch.Kind.PAGE)send(where,store,keys,NoteStore.Branch.Kind.PAGE,one.file.note,null,true);
            else if(!NoteStore.home(one.file.note))cartonToAll(where,store,keys,one.file.note);
            if(one.file.held!=NoteStore.Branch.Kind.PAGE&&!store.looseAudience(one.file.id).isEmpty())sleeves(where,store,keys,one.file.id,null,false);
        } catch(Throwable notNow) {
            // What went up halfway is let go: it goes up whole, under a new key, next time.
            if(made!=null&&(!Node.helpers()||!Direct.relayed(made.sources,own(where))))Node.forget(where,made.encode());
            store.publishFailed(one.file.id);
            android.util.Log.w("Mininotes/Post","files: one could not go up: "+notNow.getClass().getSimpleName());
        }
    }

    /**
     * Who a file goes to that is on this network now (decision 96), by address: everybody its note, its collection or its own
     * sharing reaches - Temp's to the owner's devices among them - that is heard here.
     */
    private static List<String> nearFor(Context where,NoteStore store,NoteStore.Held file) {
        java.util.Set<String> goes=new java.util.LinkedHashSet<>();
        if(file.held==NoteStore.Branch.Kind.PAGE)goes.addAll(store.everybodyIn(NoteStore.Branch.Kind.PAGE,file.note));
        else {
            if(!NoteStore.home(file.note))goes.addAll(store.goesWith(file.note));
            for(Outbox.Wait wait:store.sleevesOwed(file.id,true))goes.add(wait.address);
        }
        List<String> near=new ArrayList<>();
        if(goes.isEmpty())return near;
        for(NoteStore.Contact them:store.addresses())if(goes.contains(them.address)&&them.paired()&&Routes.near(them))near.add(them.address);
        return near;
    }

    /**
     * A file kept at this device's door, pinned, with nothing sent up - as {@link #offer} keeps a sending for a device on
     * this network - and its list or sleeve sent now to the devices on this network it goes to, which fetch it from the door
     * (see {@link #fromTheirDoor}). Each send goes through the same gates as any: a sleeve only to a device that has said
     * {@link Receipt#LOOSE}, a carton only where it is owed.
     *
     * @return the manifest kept, which names no source
     */
    private static String atTheDoor(Context where,NoteStore store,Keys keys,NoteStore.Held file,byte[] plain,List<String> near) throws Exception {
        com.eurobuddha.maxima.core.store.BlobStore shelf=Node.blobs(where);
        if(shelf==null)throw new IllegalStateException("The node is not running.");
        com.eurobuddha.maxima.core.media.MediaManifest kept=new com.eurobuddha.maxima.core.media.MediaService(null,shelf).publish(plain,file.kind);
        String manifest=kept.encode();
        try{store.offeredAtDoor(file.id,manifest);}catch(RuntimeException notKept){Node.forget(where,manifest);throw notKept;}
        android.util.Log.i("Mininotes/Post","files: one is offered from this device's door first, "+kept.chunkIds.size()+" piece(s), to "
            +near.size()+" device(s) on this network; then up to the relays for the others");
        for(String address:near) {
            try {
                if(file.held==NoteStore.Branch.Kind.PAGE)send(where,store,keys,NoteStore.Branch.Kind.PAGE,file.note,address,true);
                else {
                    if(!NoteStore.home(file.note)&&store.goesWith(file.note).contains(address))cartonTo(where,store,keys,file.note,address);
                    sleeves(where,store,keys,file.id,address,false);
                }
            } catch(Exception notThisOne){/* it goes to them with everybody once it is up */}
        }
        return manifest;
    }

    /** One file fetched and kept, and everybody who has its note told this device has it. */
    private static void fetch(Context where,NoteStore store,Keys keys,com.eurobuddha.maxima.core.media.MediaService media,
                              NoteStore.Incoming one) {
        try {
            com.eurobuddha.maxima.core.media.MediaManifest said=com.eurobuddha.maxima.core.media.MediaManifest.decode(one.manifest);
            // What the list said about it and what the manifest says have to be the same file.
            if(said==null||said.size!=one.bytes||!Enclosure.travels(said.size))throw new IllegalArgumentException("Not a file that can come.");
            said=fromTheirDoor(where,store,said,one.origin,!Node.helpers(),store.holders(one.id));
            byte[] plain=media.fetch(said);
            if(plain.length!=one.bytes)throw new SecurityException("Not the file that was listed.");
            if(!store.fileArrived(one,plain))return;
            android.util.Log.i("Mininotes/Post","files: one arrived and is kept");
            store.happened(History.Kind.FILE_CAME,one.id,one.origin,System.currentTimeMillis());
            moved(one.note);
            // Everybody who has what it is kept with - a note, or a collection - is told this device has it.
            java.util.Set<String> who=store.everybodyIn(store.keptWith(one.note),one.note);
            for(NoteStore.Contact them:store.addresses()) {
                if(!who.contains(them.address)&&!them.address.equals(one.origin))continue;
                if(them.agreement.length==0||!doesSpeak(where,them))continue;
                try{saidAboutFile(where,store,keys,them,one.id,Receipt.FILE_HERE);}catch(Exception notThisOne){/* they go on showing it as on its way */}
            }
        } catch(Throwable notNow) {
            int tries=store.fetchFailed(one.id);
            // What went wrong and where, never the file's name or bytes: "IllegalStateException" alone could not be told apart
            // (the owner, 2026-10-11: a screenshot let go on Temp on the PC never reached the phone).
            StackTraceElement[] at=notNow.getStackTrace();
            android.util.Log.w("Mininotes/Post","files: one could not be fetched ("+tries+" tries): "+notNow.getClass().getSimpleName()
                +(notNow.getMessage()==null?"":": "+notNow.getMessage())+(at.length>0?" at "+at[0].getClassName()+"."+at[0].getMethodName():""));
            // Only between the owner's devices, what could not be had was not in reach: the device that has it
            // keeps it, and sending it up again would change nothing, so it is not asked to.
            // At once where all it was told is the sender's door (decision 289): the pieces there are let go as soon as they
            // are up, so the place to ask for is the one it went up to, which the sender says when asked.
            if(!(Enclosure.sayMissing(tries)||Routes.doorOnly(one.manifest,0))||!Node.helpers())return;
            // Asked for again from whoever listed it: the relays may have let it go.
            NoteStore.Contact them=store.address(one.origin);
            if(them!=null&&them.agreement.length>0&&doesSpeak(where,them))
                try{saidAboutFile(where,store,keys,them,one.id,Receipt.FILE_MISSING);}catch(Exception notNow2){/* asked again after four more */}
        }
    }

    /** This device's own doors, which hand out only its own pieces. */
    private static List<String> own(Context where) {
        MaximaNode node=Node.node(where);
        return node==null?new ArrayList<>():node.directAddresses();
    }

    /**
     * The pieces asked of the device that listed the file first, where its door can be reached - on this
     * network, or at a public address it proved - before any relay: see {@link Direct}. The door serves only
     * what that device holds, and each piece is checked against its hash as it comes, so asking it costs
     * nothing if it has not got them. Each door is asked once whether it has the first piece, so a door that
     * is not there costs one wait, not one a piece.
     */
    private static com.eurobuddha.maxima.core.media.MediaManifest fromTheirDoor(Context where,NoteStore store,
            com.eurobuddha.maxima.core.media.MediaManifest said,String origin,boolean onlyDoors) {
        return fromTheirDoor(where,store,said,origin,onlyDoors,List.of());
    }

    /**
     * @param others devices that have said they have the file too. Where the one that listed it cannot be reached,
     *               theirs are asked: a device keeps the pieces it fetched, and its door hands them out as it does its
     *               own. Only between the owner's devices this is the difference between a phone waiting until the
     *               other phone is on the same Wi-Fi and it having the file from the PC that already has it.
     */
    private static com.eurobuddha.maxima.core.media.MediaManifest fromTheirDoor(Context where,NoteStore store,
            com.eurobuddha.maxima.core.media.MediaManifest said,String origin,boolean onlyDoors,List<String> others) {
        List<String> doors=doorsWith(where,store,said,origin);
        for(String other:others==null?List.<String>of():others) {
            if(!doors.isEmpty())break;
            if(other!=null&&!other.equals(origin))doors=doorsWith(where,store,said,other);
        }
        // Only between the owner's devices, the doors that answered are the whole list: the relays a manifest
        // names are not asked, and where no door answered nothing is.
        if(onlyDoors&&doors.isEmpty())throw new IllegalStateException(Direct.WAITS);
        if(doors.isEmpty())return said;
        return new com.eurobuddha.maxima.core.media.MediaManifest(said.mime,said.size,said.keyHex,said.nonceHex,
            said.sha3Hex,said.chunkIds,onlyDoors?doors:Direct.sourcesFirst(doors,said.sources));
    }

    /** The doors of the device that listed a file that say they have its first piece; none where it is out of reach. */
    private static List<String> doorsWith(Context where,NoteStore store,com.eurobuddha.maxima.core.media.MediaManifest said,String origin) {
        List<String> doors=new ArrayList<>();
        try {
            MaximaNode node=Node.node(where);
            NoteStore.Contact them=origin==null||node==null?null:store.address(origin);
            com.eurobuddha.maxima.core.contacts.Contact reach=them==null?null:Node.known(where,them.contact);
            if(reach==null||said.chunkIds.isEmpty())return doors;
            for(String door:Direct.firstTries(node.lanAddressFor(reach.publicKey),new ArrayList<>(reach.addresses),reach.publicKey))
                if(com.eurobuddha.maxima.core.media.MediaWire.has(node,door,said.chunkIds.get(0)))doors.add(door);
        } catch(RuntimeException notNow){/* none, then: the relays, or with none of them, nothing */}
        return doors;
    }

    /**
     * Which files each device has been told this device has, by an answer to their list, this run: "key file". By the
     * device's key, not its address: the address changes as the device is reached by another door or relay, and each
     * change said all the files again, every 40 s or so (2026-10-19, decision 211).
     */
    private static final java.util.Set<String> SAID_HAVE=java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * "I have this file", for each of these, to the device whose list named them, off the arrival's thread. Once a
     * run for each file and device where it went: after that, the one said on fetching and this one have both had
     * their chance, and a list arriving with every note would otherwise cost a message a file each time. One that
     * did not go is said again at their next list.
     */
    private static void sayWeHave(final Context where,final NoteStore store,final Keys keys,final NoteStore.Contact them,
                                  final List<String> files) {
        if(where==null||them==null||files==null||files.isEmpty()||them.agreement.length==0)return;
        final String who=fingerprint(them);
        if(who.isEmpty())return;
        // Never the reason a node is started, as with the rest of file work.
        if(!doesSpeak(where,them)||!Node.running())return;
        // Taken before they go, not once they went: each costs a proof of work, and a list arriving every two seconds
        // found the last ones still on their way and said them all again, without end (the owner, 2026-10-24, the PC's
        // Home empty while a thousand "they have a file" came in from the Pro; decision 243).
        final List<String> due=reserve(SAID_HAVE,who,files);
        if(due.isEmpty())return;
        ANSWERS.execute(()->{
            int went=0;
            for(int at=0;at<due.size();at++) {
                boolean said=false;
                try{said=saidAboutFile(where,store,keys,them,due.get(at),Receipt.FILE_HERE);}
                catch(Exception notNow){release(SAID_HAVE,who,due.subList(at,due.size()));break;}   // out of reach: the rest would only wait out the same door; said again at their next list
                if(said)went++;else release(SAID_HAVE,who,due.subList(at,at+1));
            }
            // Every one of them is here: what the count says is how many of those it was told of. Read the other way
            // round, "0 of 12" looked like a device that had none of the files it held.
            android.util.Log.i("Mininotes/Post",filesHere(due.size(),went));
        });
    }

    /** The files of a list not yet said or being said to this device, each marked as said as it is taken (see sayWeHave). */
    static List<String> reserve(java.util.Set<String> said,String who,List<String> files) {
        List<String> due=new ArrayList<>();
        for(String id:files)if(id!=null&&said.add(who+" "+id))due.add(id);
        return due;
    }
    /** Files that did not go after all: said again at the device's next list. */
    static void release(java.util.Set<String> said,String who,List<String> files) {
        for(String id:files)said.remove(who+" "+id);
    }

    /** The log line for {@link #sayWeHave}: how many of the files a list named are here, and how many of those were said. */
    static String filesHere(int here,int told) {
        return here+" file(s) the device whose list came named are here: told it of "+(told>=here?"all":told+", the rest at its next list");
    }

    /** "I have this file", or "I cannot get it", to one device. The envelope names the file where it names a note. */
    private static boolean saidAboutFile(Context where,NoteStore store,Keys keys,NoteStore.Contact them,String file,int what) throws Exception {
        MaximaNode node=Node.node(where);
        if(node==null)return false;
        byte[] sealed=Envelope.seal(sixteen(file),0,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),Keys.publicKey(them.agreement));
        return hand(where,store,keys,node,them,sealed);
    }

    // ---- files sent straight to a device, belonging to no note: see Drop -------------------------------------

    /**
     * Told when something about files sent or received on their own moves on off the arrival path - files fetched,
     * an answer that came - so a screen can say it, or the phone can. Called off the interface thread.
     */
    static volatile java.util.function.Consumer<Landed> news;

    private static void tell(Landed landed) {
        java.util.function.Consumer<Landed> told=news;
        if(told!=null)try{told.accept(landed);}catch(RuntimeException notNow){/* the list says it when it is next drawn */}
    }

    /** A device whose build has said it takes files on their own, written down by the fingerprint of its key. */
    private static void takesFiles(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("files",new java.util.HashSet<String>()));
        if(all.add(fingerprint))kept.edit().putStringSet("files",all).apply();
    }

    /** Whether a device has said it takes files on their own: a sending is offered only to one that has. */
    static boolean takesFiles(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("files",new java.util.HashSet<String>()).contains(who);
    }

    /**
     * When each device was last told that this device takes files, by fingerprint, and whether it took it: {when, 1 or 0}.
     * What it took within the day is kept across restarts, as every other "this build knows" is (decision 225; see Said):
     * it was the one said again at every start, and to a device out of reach every ten minutes for as long as this one
     * ran, each try logged as not sent (decision 262).
     */
    private static final java.util.Map<String,long[]> TOLD_FILES=new java.util.concurrent.ConcurrentHashMap<>();
    /** A device that could not be told is tried again this much later, not every round. */
    private static final long TELL_AGAIN=Said.TELL_AGAIN;
    /**
     * How many times this run a device was told again something it was handed and never said back, by kind and fingerprint:
     * a relay takes it for a device that is not there, so a PC left off was told every ten minutes, for as long as this
     * device ran, each time a proof of work (decision 257). See {@link Said#unanswered}.
     */
    private static final java.util.Map<String,Integer> RETOLD=new java.util.concurrent.ConcurrentHashMap<>();
    /** How long after it was last handed something not said back is said again to a device. */
    private static long tellAgain(String kind,String who){Integer times=RETOLD.get(kind+" "+who);return Said.unanswered(times==null?0:times);}
    /** Told again what it was handed and never said back. */
    private static void retold(String kind,String who){RETOLD.merge(kind+" "+who,1,Integer::sum);}

    /** What a device was told and took within the day, by kind, kept across restarts (decision 225): see Said. Null for nothing. */
    private static String saidLately(Context where,String kind,String who) {
        if(where==null||who==null||who.isEmpty())return null;
        String kept=where.getSharedPreferences("post",Context.MODE_PRIVATE).getString("said."+kind+"."+who,null);
        return Said.recent(kept,System.currentTimeMillis())?kept:null;
    }
    /** That a device took it, kept for the next start. */
    private static void said(Context where,String kind,String who,String value) {
        if(where==null||who==null||who.isEmpty())return;
        try{where.getSharedPreferences("post",Context.MODE_PRIVATE).edit().putString("said."+kind+"."+who,Said.keep(value,System.currentTimeMillis())).apply();}
        catch(RuntimeException notKept){/* said again at the next start, as before */}
    }
    /**
     * The devices a receipt or a card could not be handed to lately, by fingerprint: {when, how many times in a row}. Left
     * alone for a while, longer each time (Said.rest), and tried again at once when anything comes from them (decision 226).
     */
    private static final java.util.Map<String,long[]> UNREACHED=new java.util.concurrent.ConcurrentHashMap<>();
    /** Whether a device that could not be handed the last receipt or card is still left alone. */
    private static boolean resting(String who){long[] missed=UNREACHED.get(who);return missed!=null&&Said.resting(missed[0],(int)missed[1],System.currentTimeMillis());}
    /** A receipt or a card handed, and whether it was taken kept for {@link #resting}. */
    private static boolean handSaying(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them,byte[] sealed) throws Exception {
        String who=fingerprint(them);boolean went=false;
        try{went=hand(where,store,keys,node,them,sealed);return went;}
        finally{reached(who,went);}
    }
    /** Whether a receipt or a card was taken by a device, kept for {@link #resting}. */
    private static void reached(String who,boolean went) {
        if(went)UNREACHED.remove(who);else{long[] missed=UNREACHED.get(who);UNREACHED.put(who,new long[]{System.currentTimeMillis(),missed==null?1:missed[1]+1});}
    }

    /** Forgotten, so it is said again: where they asked for it anew. */
    private static void unsaid(Context where,String kind,String who) {
        if(where==null||who==null||who.isEmpty())return;
        try{where.getSharedPreferences("post",Context.MODE_PRIVATE).edit().remove("said."+kind+"."+who).apply();}
        catch(RuntimeException notKept){/* then it is not said again until the day is out */}
    }
    /** What a device was told this run, or, the first time it is asked in a run, what it took within the day: {when, 1}. */
    private static long[] told(Context where,java.util.Map<String,long[]> run,String kind,String who) {
        long[] told=run.get(who);
        if(told!=null)return told;
        String kept=saidLately(where,kind,who);
        if(kept==null)return null;
        told=new long[]{Said.at(kept),1};
        run.put(who,told);
        return told;
    }

    /** When this device last said to them that it takes files, this run or, once asked, within the day; 0 for not yet. */
    private static long toldFilesAt(String who){long[] at=TOLD_FILES.get(who);return at==null?0:at[0];}

    /**
     * "This device takes files", said to every paired device that has shown it knows what an answer is and has not
     * been told this run. A device that has never answered anything is not told: it may be a build from before
     * answers, which would write these bytes over a note. Two devices paired since know it from their hellos.
     */
    private static void tellTakesFiles(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            if(!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||takesFiles(where,them)))continue;
            String who=fingerprint(them);
            if(who.isEmpty())continue;
            long[] told=told(where,TOLD_FILES,"files",who);
            if(!Drop.tellNow(told==null?0:told[0],told!=null&&told[1]==1,resting(who),now))continue;
            tellTakesFiles(where,store,keys,them);
        }
    }

    private static void tellTakesFiles(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_FILES.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.TAKES_FILES),
                keys.signing(),Keys.publicKey(them.agreement));
            boolean went=false;
            // Out of reach (Direct.WAITS, while notes go only between the owner's devices) or not taken, it is left alone as
            // long as a card or another receipt would be (Said.rest), not tried again every ten minutes.
            try{MaximaSender.Result said=handDrop(where,store,keys,node,them,sealed,"said this device takes files");went=said!=null&&said.isOk();}
            finally{reached(who,went);}
            if(went){told[1]=1;said(where,"files",who,"1");}
        } catch(Exception notNow){/* tried again once its rest is out */}
    }

    // ---- linked through what is shared: see Linking ------------------------------------------------------------

    /** One line for the log about linking: counts and states, never a name, an address or a key. */
    private static void link(String said){android.util.Log.i("Mininotes/Link",said);}

    /**
     * When each device a list linked this one with was last asked, and how often, by address. For this run only: the
     * asking is written down in the notebook ({@code accepting}) and said again at every opening as well.
     */
    private static final java.util.Map<String,long[]> ASKED=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Whether anything goes to a device at all: it was paired by a code or connected, or a list of something here still
     * names it. A device linked through a list and since taken off everything stays known, and is sent nothing.
     */
    private static boolean sendsTo(NoteStore store,NoteStore.Contact them) {
        // A backup server is sent nothing but its backups (decision 330): no note, no card, whatever a list says.
        return them!=null&&!store.keepsBackups(them.address)&&(!them.listed()||store.listing(them.address)!=null);
    }

    /**
     * Everybody a list that just arrived names and this device is not linked with, linked through it: written down
     * with the keys the list gave, and then - off the arrival's thread - met on the network and asked to answer. Only
     * from a device with a say in who has the thing. Nothing of the thing goes to them until they answer.
     */
    private static void linkThrough(final Context where,final NoteStore store,final Keys keys,String from,String note,Parcel.Sent parcel) {
        if(parcel==null||parcel.members.isEmpty())return;
        List<Parcel.Member> named=Linking.toLink(parcel.members,store.listFromASay(from,note,parcel),store.mySigningKey,store.linkedKeys());
        if(named.isEmpty())return;
        final List<NoteStore.Contact> made=new ArrayList<>();
        for(Parcel.Member one:named) {
            if(!store.linkThrough(one.address,one.name,one.agreement,Linking.signing(one),true))continue;
            NoteStore.Contact them=store.address(one.address);
            if(them!=null)made.add(them);
        }
        link("a list named "+named.size()+" device(s) not linked here; "+made.size()+" linked through it, asked to answer");
        if(!made.isEmpty())ANSWERS.execute(()->{for(NoteStore.Contact them:made)askToLink(where,store,keys,them);});
    }

    /**
     * One device linked through a list, asked to answer: introduced to the network where it is not known yet - which
     * while notes go only between the owner's devices dials only a door on this network - and said a hello to. Blocking.
     */
    private static boolean askToLink(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        long[] asked=ASKED.computeIfAbsent(them.address,any->new long[2]);
        asked[0]=System.currentTimeMillis();asked[1]++;
        // Never the reason a node is started: this goes when the node is up for everything else.
        if(!Node.running())return false;
        try {
            if(them.contact==null||them.contact.trim().isEmpty()) {
                String key=Node.introduce(where,them.address);
                if(!key.isEmpty())store.knownAs(them.address,key);
            }
        } catch(Exception notThere){/* the address the list gave is still tried below */}
        NoteStore.Contact now=store.address(them.address);
        if(now==null)return false;
        try {
            // A device of mine names "mine", so the other end takes it as one of its owner's where a card there says so (decision 268).
            say(where,keys,now.address,now.agreement,Linking.SCOPE,now.mine?Introducing.TARGET:Linking.TARGET,true,store.myName,store.myAddress,now.contact);
            link("asked a device "+(now.mine?"of the owner's":"a list linked this one with")+" to answer: handed over");
            return true;
        } catch(Exception notNow) {
            link("asked a device a list linked this one with to answer: not reached ("+notNow.getClass().getSimpleName()+"), asked again later");
            return false;
        }
    }

    /** Whether this run has put right what the first build that linked left behind: see {@link NoteStore#tidyLinks}. */
    private static final java.util.concurrent.atomic.AtomicBoolean TIDIED=new java.util.concurrent.atomic.AtomicBoolean();

    /** Whoever a list linked this device with and has not answered, asked again once their turn has come. Blocking. */
    private static void linkAgain(Context where,NoteStore store,Keys keys) {
        if(TIDIED.compareAndSet(false,true)) {
            store.tidyLinks();
            // Waiting for a device that has answered something already - a note, a receipt - ends: it plainly has this
            // device's key and is there. The first build waited for one hello only, and it could go astray.
            int healed=0;
            for(String address:store.linkingNow()) {
                NoteStore.Contact them=store.address(address);
                if(them!=null&&doesSpeak(where,them)){store.answered(address);healed++;}
            }
            if(healed>0)link("stopped waiting for "+healed+" device(s) linked through a list that had answered already");
        }
        java.util.Set<String> waiting=store.linkingNow();
        if(waiting.isEmpty())return;
        long now=System.currentTimeMillis();int asked=0;
        for(String address:waiting) {
            long[] was=ASKED.get(address);
            if(was!=null&&!Linking.due(was[0],(int)was[1],now))continue;
            NoteStore.Contact them=store.address(address);
            if(them==null||!them.paired())continue;
            store.triedAgain(address);
            if(askToLink(where,store,keys,them))asked++;
        }
        if(asked>0)link("asked "+asked+" device(s) again to answer");
    }

    /**
     * A hello from a device linking through something both have. From a device linked here already: it has answered,
     * so what it is owed goes now; and where it asks, it is answered. From one that is not: taken only where a list here
     * names the key it signs with - see {@link Linking#names} - and then written down, met, answered and sent what it is
     * owed. From anybody else, nothing: not a contact, not a question put to the owner, not a word back.
     */
    private static Landed linkHeard(final Context where,final NoteStore store,final Keys keys,final Hello.Said said,byte[] sender) {
        NoteStore.Contact known=null;
        for(NoteStore.Contact one:store.addresses())
            if(one.paired()&&Courier.hex(sender).equals(fingerprint(one)))known=one;
        final boolean asks=Linking.asks(said);
        // A device of mine, linking as one: taken as mine where a card here vouches for it, otherwise as any LINK hello (decision 268).
        if(Introducing.isMine(said)) {
            Landed mine=mineHeard(where,store,keys,said,known,asks);
            if(mine!=null)return mine;
        }
        if(known!=null) {
            final NoteStore.Contact them=known;
            boolean was=store.linkingNow().contains(them.address);
            store.answered(them.address);
            link("a device linked here said hello"+(asks?", asking to be answered":", answering")+(was?": linked, and what it is owed goes now":""));
            ANSWERS.execute(()->{
                // Something just arrived, so the node is up; where it is not, nothing is started for this.
                if(!Node.running())return;
                if(asks)answerLink(where,store,keys,them.address,said.address);
                if(was)owedTo(where,store,keys,them.address);
            });
            return was?Landed.people(""):new Landed(null,null);
        }
        Sharing.Rule named=store.namesDevice(said.signing);
        if(named==null) {
            link("a device not linked here asked to link, and no list here names it: not taken");
            return new Landed(null,null);
        }
        // Filed at the address the list gives it, which the list's rows point at; met at the one it says it is at now.
        final String address=named.address;
        String name=store.memberName(address);
        if(!store.linkThrough(address,name.isBlank()?said.name:name,said.agreement,said.signing,false))return new Landed(null,null);
        link("a device a list here names asked to link: linked"+(asks?", answered":""));
        ANSWERS.execute(()->{
            if(!Node.running())return;
            met(where,store,address,said.address);
            if(asks)answerLink(where,store,keys,address,said.address);
            owedTo(where,store,keys,address);
        });
        return Landed.people("");
    }

    /**
     * A device that has just said hello, met at the address its hello gives - where it is now - wherever the network
     * does not know it yet. The address a list gave is the one its sender last saw, and a device restarted since is
     * somewhere else: an answer handed to that address was taken by a relay for nobody, so the device that asked went
     * on asking, and both ends went on drawing the other as not linked.
     */
    private static void met(Context where,NoteStore store,String address,String now) {
        NoteStore.Contact them=store.address(address);
        if(them==null||now==null||now.trim().isEmpty())return;
        if(them.contact!=null&&!them.contact.trim().isEmpty()&&Node.known(where,them.contact)!=null)return;
        try {
            String key=Node.introduce(where,now);
            if(!key.isEmpty())store.knownAs(address,key);
        } catch(Exception notThere){/* the address kept here is tried below */}
    }

    /** A hello that asked to be answered, answered - and said in the log, since an answer that went nowhere is the fault. */
    private static void answerLink(Context where,NoteStore store,Keys keys,String address,String now) {
        met(where,store,address,now);
        NoteStore.Contact them=store.address(address);
        if(them==null)return;
        try {
            say(where,keys,them.address,them.agreement,Linking.SCOPE,them.mine?Introducing.TARGET:Linking.TARGET,false,store.myName,store.myAddress,them.contact);
            link("answered a device that asked to link: handed over");
        } catch(Exception notNow){link("could not answer a device that asked to link ("+notNow.getClass().getSimpleName()+"): it asks again");}
    }

    /** Whatever one device is owed of everything, sent now: it has just shown it is there. Blocking. */
    private static void owedTo(Context where,NoteStore store,Keys keys,String address) {
        try {
            Done done=send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,address);
            link("sent what a device just linked is owed: "+done.sent+" went, "+done.failed+" did not");
        } catch(Exception notNow){link("could not send what a device just linked is owed: "+notNow.getClass().getSimpleName());}
    }

    // ---- one person on every device they own: see Persons ------------------------------------------------------

    /** Who owns this device, as kept here; the id is made the first time it is asked for. */
    static synchronized Persons.Me me(Context where) {
        android.content.SharedPreferences kept=where.getSharedPreferences("settings",Context.MODE_PRIVATE);
        String id=kept.getString("person","");long made=kept.getLong("personMade",0);
        if(!Persons.isId(id)) {
            id=Persons.newId(new java.security.SecureRandom());made=System.currentTimeMillis();
            kept.edit().putString("person",id).putLong("personMade",made).apply();
        }
        String chosen=kept.getString("me","").trim();
        String device=kept.getString("device","").trim();
        return new Persons.Me(id,made,Persons.aliases(kept.getString("personAliases","")),
            chosen.isEmpty()?Node.nameHere(where):chosen,chosen.isEmpty()?0:kept.getLong("meChanged",0),
            device.isEmpty()?Node.deviceHere(where):device,device.isEmpty()?0:kept.getLong("deviceChanged",0));
    }

    /** Who owns this device, kept: a new id, its aliases, and your name where another device of yours chose it later. */
    private static synchronized void keepMe(Context where,NoteStore store,Persons.Me was,Persons.Me now) {
        android.content.SharedPreferences.Editor edit=where.getSharedPreferences("settings",Context.MODE_PRIVATE).edit()
            .putString("person",now.id).putLong("personMade",now.made).putString("personAliases",Persons.aliases(now.aliases));
        // A later choice is kept even where it is the same name: kept only when the words differed, the older time
        // stayed here and every card from then on said "your name chosen there later" again.
        boolean renamed=now.named>was.named&&!now.name.equals(was.name);
        if(now.named>was.named&&!now.name.isEmpty())edit.putString("me",now.name).putLong("meChanged",now.named);
        edit.apply();
        if(store!=null)store.ownDevicesAre(now.id);
        // One person now with another device: Photos under the id they share, not one each (decision 217).
        if(store!=null)try{store.photosFollow(was.id,now.id);}catch(RuntimeException notNow){/* the next time */}
        if(renamed) {
            if(store!=null)store.myName=now.name;
            // The network is told off whichever worker this is on: an arrival must not wait on the node.
            final String name=now.name;
            ANSWERS.execute(()->Node.called(where,name));
        }
        if(!now.id.equals(was.id))persons("now one person with another device of theirs");
    }

    /**
     * A device just bonded as one of the owner's own - its hello accepted an offer of everything - and so marked as
     * theirs here. Its hello says whose it is and what it is called, where its build knows about persons: whose id both
     * keep is decided now, its name among your devices written down, and cards can go to it from the next round, since
     * a build that knows about persons and accepted everything has marked this device as its owner's too.
     */
    static void bonded(Context where,NoteStore store,Hello.Said them) {
        NoteStore.Contact saved=store.address(them.address);
        if(saved==null)return;
        store.setMine(them.address,true);
        if(!them.persons){persons("bonded with a device whose build does not know about persons");return;}
        String who=fingerprint(saved);
        readsCards(where,who,true);
        if(Persons.later(them.deviceNamed,saved.named,true))store.ownDeviceNamed(them.signing,them.deviceName,them.deviceNamed);
        Persons.Me was=me(where);
        keepMe(where,store,was,Persons.bonded(was,them));
        persons("bonded with one of the owner's devices");
    }

    /**
     * A device of the owner's that scanned an Add my device code, added here (decision 255): paired, marked as theirs and
     * bonded as {@link #bonded} does, so it is listed among my devices and has the cards (persons, stamps, groups) as any
     * device of mine; and given everything at the level the code offered, or nothing where it offered nothing yet. What it
     * may do with everything is then chosen on its line, and a note or a folder is shared with it as with anybody. Blocking.
     *
     * @param everything what it may do with everything, or null for nothing yet
     */
    static void addedAsMine(Context where,NoteStore store,Hello.Said them,Sharing.Level everything) {
        store.pairedWith(them.address,them.name,false,them.agreement,them.signing);
        bonded(where,store,them);
        if(everything!=null&&everything!=Sharing.Level.GONE)store.give(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,them.address,everything,null);
        // Linked with my other devices too, but those unticked in the box that showed the code (decision 268).
        alsoLink(where,store,them.address,alsoLinkSkip(where));
    }

    /** A device whose build has said it knows about persons, and whether it said this device is its owner's too. */
    private static void readsCards(Context where,String fingerprint,boolean mineThere) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> reads=new java.util.HashSet<>(kept.getStringSet("persons",new java.util.HashSet<String>()));
        java.util.Set<String> mine=new java.util.HashSet<>(kept.getStringSet("mineThere",new java.util.HashSet<String>()));
        boolean changed=reads.add(fingerprint);
        changed|=mineThere?mine.add(fingerprint):mine.remove(fingerprint);
        if(changed)kept.edit().putStringSet("persons",reads).putStringSet("mineThere",mine).apply();
    }

    private static boolean readsCards(Context where,String fingerprint) {
        return where!=null&&!fingerprint.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("persons",new java.util.HashSet<String>()).contains(fingerprint);
    }

    private static boolean mineThere(Context where,String fingerprint) {
        return where!=null&&!fingerprint.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("mineThere",new java.util.HashSet<String>()).contains(fingerprint);
    }

    /** What People and devices says under a device that only one end counts as the owner's: see {@link Persons#oneSided}. */
    static String oneSided(Context where,NoteStore.Contact them) {
        if(them==null||!them.paired())return null;
        String who=fingerprint(them);
        String line=Persons.oneSided(them.mine,readsCards(where,who),mineThere(where,who),Node.A_PC?"PC":"phone");
        // Neither end has said, or both said no, and another device of yours lists it as yours: the switch to turn.
        return line==null&&!them.mine&&listedAsYours(where,who)?Persons.LISTED_AS_YOURS:line;
    }

    /** Devices a card from one of yours listed as yours, by fingerprint: kept, so People can say so after a restart. */
    private static boolean listedAsYours(Context where,String fingerprint) {
        return where!=null&&!fingerprint.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("listedAsYours",new java.util.HashSet<String>()).contains(fingerprint);
    }

    /** The devices paired here that a card lists, kept as yours or not by what it says of each. Whether that changed. */
    private static boolean listedAsYours(Context where,NoteStore store,Persons.Card card) {
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> listed=new java.util.HashSet<>(kept.getStringSet("listedAsYours",new java.util.HashSet<String>()));
        boolean changed=false;
        for(Persons.Device one:card.devices)for(NoteStore.Contact here:store.addresses()) {
            if(!here.paired()||!Persons.key(here.signing).equals(Persons.key(one.signing)))continue;
            String who=fingerprint(here);
            if(!who.isEmpty())changed|=one.state==Persons.ACTIVE?listed.add(who):listed.remove(who);
        }
        if(changed)kept.edit().putStringSet("listedAsYours",listed).apply();
        return changed;
    }

    /**
     * What each device was last told this run about persons, by fingerprint: which of the two, when, and whether it
     * went. For this run only, as with files: every start says it again. Said again when which of the two it would be
     * changes, because that is somebody turning the switch in People.
     */
    private static final java.util.Map<String,long[]> TOLD_PERSONS=new java.util.concurrent.ConcurrentHashMap<>();
    /** The last card each device was sent this run, by fingerprint, as a hash: sent again only when it changes. */
    private static final java.util.Map<String,String> CARDS=new java.util.concurrent.ConcurrentHashMap<>();
    /** The last card each device was found out of reach for, as a hash: so the log says so once a card. */
    private static final java.util.Map<String,String> CARD_WAITS=new java.util.concurrent.ConcurrentHashMap<>();
    /** What each device last said about persons this run, by fingerprint, and when: see {@link Persons#news}. */
    private static final java.util.Map<String,long[]> HEARD_PERSONS=new java.util.concurrent.ConcurrentHashMap<>();

    /** "This build knows about persons", said to every paired device that has shown it knows what an answer is. */
    private static void tellPersons(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsCards(where,who)))continue;
            long[] told=TOLD_PERSONS.get(who);int want=them.mine?Receipt.PERSONS_MINE:Receipt.PERSONS;
            // Which of the two it took within the day, at a restart (decision 225): said again where the switch changed since.
            String kept=told==null?saidLately(where,"persons",who):null;
            if(kept!=null&&String.valueOf(want).equals(Said.value(kept))){told=new long[]{want,Said.at(kept),1};TOLD_PERSONS.put(who,told);}
            if(told!=null&&told[0]==want&&(told[2]==1||now>=told[1]&&now-told[1]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellPersons(where,store,keys,them);
        }
    }

    private static void tellPersons(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        int what=them.mine?Receipt.PERSONS_MINE:Receipt.PERSONS;
        long[] told={what,System.currentTimeMillis(),0};
        TOLD_PERSONS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(what),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[2]=1;said(where,"persons",who,String.valueOf(what));}
            persons("said this build knows about persons"+(them.mine?", to one of the owner's devices":"")+(told[2]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    // ---- notes and collections, nested as deep as anybody likes: see Things -----------------------------------

    /** Devices whose build has said it knows about trees, by the fingerprint of their key: kept, across restarts. */
    private static boolean trees(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("trees",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("trees",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it knows about trees. One that has not is a 0.1 device: it is sent only what fits
     * three levels, and no collection on its own.
     */
    static boolean knowsTrees(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("trees",new java.util.HashSet<String>()).contains(who);
    }

    /**
     * A collection that arrived on its own, from a device paired here (see {@link Carton}). Taken in where they may write
     * in it or it is theirs, as a note is, and answered either way - "took" where this device now has what they sent,
     * "have" where it did not write it down - or they would send it every quarter of an hour for ever. Not where this
     * device has stopped taking it in or has left it: nothing is said about what somebody asked not to hear about.
     */
    private static Landed cartonArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened,
                                        Carton.Sent carton) {
        NoteStore.Refusal refused=store.refusal(from.address,carton);
        if(refused!=null) {
            android.util.Log.i("Mininotes/Post","a collection on its own, not taken in: this device "
                +(refused.gone?"has left it":refused.quiet?"refused it quietly":"has stopped taking it in"));
            // Refused quietly (decision 109): answered as had, so they stop sending it, and told nothing.
            if(refused.quiet&&carton.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,false);}
            return new Landed(null,null);
        }
        String here=store.cartonArrived(from.address,opened.revision,carton);
        // Something of theirs is here, so whatever was accepted has been answered.
        if(here!=null)store.answered(from.address);
        android.util.Log.i("Mininotes/Post","a collection arrived on its own: "+(here==null?"not taken in, they may not write in it":"taken in"));
        // The files it keeps at their end, taken in as a note's are, with the collection where the note would be: what is
        // not here yet is fetched in the background, what came from them and is no longer listed goes, and what of
        // theirs is here already is said to them. A list not said changes nothing.
        if(here!=null&&carton.files!=null) {
            if(store.listed(here,from.address,carton.filesAsOf,carton.files))travelSoon(where,store,keys);
            moved(here);
            sayWeHave(where,store,keys,from,store.haveHere(here,carton.files));
        }
        if(carton.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,here!=null);}
        // A folder somebody else shared with me for the first time, or more of one that still waits: a question (decision 109).
        Object[] waits=here==null?null:store.waitsUnder(NoteStore.Branch.Kind.COLLECTION,here);
        if(waits!=null)return Landed.waiting(waitingSaid(store,from,waits),(String)waits[1]).by(from.name,0);
        return Landed.people(here==null?"":here);
    }

    /** When each device was last told this run that this build knows about trees, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_TREES=new java.util.concurrent.ConcurrentHashMap<>();

    // ---- files shared on their own: see Sleeve (decision 92) ---------------------------------------------------

    /** Devices whose build has said it knows files on their own, by the fingerprint of their key: kept, across restarts. */
    private static boolean loose(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("loose",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("loose",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it knows files on their own (see {@link Receipt#LOOSE}). A sleeve is never sealed for
     * one that has not: a build from before would write it over a note.
     */
    static boolean knowsLoose(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("loose",new java.util.HashSet<String>()).contains(who);
    }

    /** When each device was last told this run that this build knows files on their own, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_LOOSE=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * "This build knows files on their own", said to every paired device that has shown it knows what an answer is, once a
     * run and again in a while where it was not taken, as {@link Receipt#TREE} is. A device that has never answered
     * anything is not told: it may be a build from before answers, which would write these bytes over a note.
     */
    private static void tellLoose(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||knowsLoose(where,them)))continue;
            long[] told=told(where,TOLD_LOOSE,"loose",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellLoose(where,store,keys,them);
        }
    }

    private static void tellLoose(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_LOOSE.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.LOOSE),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"loose",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build knows files on their own"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * A file that arrived on its own, from a device paired here (see {@link Sleeve}). Taken in from whoever it is from or an
     * admin of it, and answered either way ("took" where this device now has what they sent, "have" where it did not
     * write it down), or they would send it every quarter of an hour for ever. Not where this device has stopped taking it
     * in; where it has left it, they are told again, as for a note.
     */
    private static Landed sleeveArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened,
                                        Sleeve.Sent sleeve) {
        // The envelope names the file it is about: the two have to agree, or every answer would name another.
        if(!sleeve.id.equalsIgnoreCase(idFrom(opened.page)))return new Landed(null,null);
        NoteStore.Refusal refused=store.refusal(from.address,sleeve.id,null);
        if(refused!=null&&refused.gone&&givenAgainIn(store,sleeve.members,refused.at)) {
            android.util.Log.i("Mininotes/Post","given again a file this device had left, so it is taken in");
            store.followFileAgain(sleeve.id);
            refused=null;
        }
        if(refused!=null) {
            android.util.Log.i("Mininotes/Post","a file on its own, not taken in: this device "+(refused.gone?"has left it, and they are told again"
                :refused.quiet?"refused it quietly":"has stopped taking it in"));
            if(refused.gone&&knowsLoose(where,from))tellOff(where,store,keys,from,opened.page,refused.at,Receipt.LEFT_FILE);
            // Refused quietly (decision 109): answered as had, so they stop sending it, and told nothing.
            if(refused.quiet&&sleeve.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,false);}
            return new Landed(null,null);
        }
        int did=store.sleeveArrived(from.address,opened.revision,sleeve);
        if(did!=NoteStore.SLEEVE_NOT)store.answered(from.address);
        // Its stamps on Minima, where it was taken in: only the file's own (decision 251).
        NoteStore.StampsTaken stamped=(did==NoteStore.SLEEVE_FETCH||did==NoteStore.SLEEVE_TOOK)&&sleeve.stamps!=null
            ?stampsWith(where,store,keys,store.stampsCameWithFile(from.address,sleeve.id,sleeve.stamps)):null;
        android.util.Log.i("Mininotes/Post","a file arrived on its own: "+(did==NoteStore.SLEEVE_FETCH?"waited for"
            :did==NoteStore.SLEEVE_GONE?"gone for everybody, so let go here":did==NoteStore.SLEEVE_TOOK?"here already, its list taken":"not taken in"));
        // A file to fetch, or a new version of one here from somebody who may write in it (decision 93).
        if(did==NoteStore.SLEEVE_FETCH||did==NoteStore.SLEEVE_TOOK&&sleeve.replaced>0)travelSoon(where,store,keys);

        if(sleeve.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,did!=NoteStore.SLEEVE_NOT);}
        // And that it waits here to be accepted, so their tick does not say "everybody has it" yet (see Asking).
        if(sleeve.answer&&did!=NoteStore.SLEEVE_NOT&&did!=NoteStore.SLEEVE_GONE&&store.waits(NoteStore.Branch.Kind.FILE,sleeve.id))
            sayWaits(where,store,keys,from,opened.page,opened.revision,NoteStore.Branch.Kind.FILE,sleeve.id);
        if(did==NoteStore.SLEEVE_GONE)return Landed.left(Say.t("{0} deleted a file they shared with you.",from.name),"").by(from.name,0);
        // A file somebody else shared with me on its own, for the first time: it waits, coming and once here (decision 109).
        if(did==NoteStore.SLEEVE_FETCH&&store.waits(NoteStore.Branch.Kind.FILE,sleeve.id))
            return stamped(Landed.waiting(FirstShare.title(List.of(from.name),1,sleeve.name),sleeve.id).by(from.name,0),stamped);
        // New here and not waiting, from another device of mine: said all the same, once.
        if(did==NoteStore.SLEEVE_FETCH)return stamped(Landed.shared(Say.t("{0} shared “{1}” with you",from.name,sleeve.name),sleeve.id).by(from.name,0),stamped);
        return stamped(Landed.people(""),stamped);
    }

    // ---- heard from: what a device is owed goes when it is plainly there ----------------------------------------

    /** When each device was last heard from and sent what it is owed, by address: once a minute at most. */
    private static final java.util.Map<String,Long> HEARD_FROM=new java.util.concurrent.ConcurrentHashMap<>();
    /** On a thread of its own: an answer is never held up by a note going. */
    private static final java.util.concurrent.ExecutorService HEARD_SENDS=
        java.util.concurrent.Executors.newSingleThreadExecutor(work->{
            Thread one=new Thread(work,"mininotes-heard");one.setDaemon(true);return one;});

    /**
     * Anything signed by a device says it is there, so what it is owed goes now (the owner, 2026-10-14: "everything should be
     * sync as soon as possible ... the user likes it as quick as possible"). It used to wait for its next turn, up to a
     * quarter of an hour after a device came back. Once a minute a device at most, and only while this device's node is
     * already running: hearing is never what starts it.
     */
    private static void heardFrom(Context where,NoteStore store,Keys keys,NoteStore.Contact from) {
        if(where==null||from==null||from.agreement.length==0||!Node.running())return;
        // Only a device coming back: one heard from in the last two minutes is there already and is sent to as things are
        // written. Sent on every message, the same revision went two or three times in a few seconds while two devices were
        // busy with one note (the owner's Live note, 2026-10-14), which made crossing copies likelier.
        long now=System.currentTimeMillis();Long was=HEARD_FROM.put(from.address,now);
        // Heard again after a quiet stretch: a line in History (decision 305). Not at a start, when nobody has been heard yet.
        if(was!=null&&History.back(was,now))store.happened(History.Kind.BACK,"",from.address,now);
        if(was!=null&&now-was<120_000)return;
        HEARD_SENDS.execute(()->{
            try {
                Done done=send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,from.address);
                if(done.sent>0||done.failed>0)android.util.Log.i("Mininotes/Post","heard from them, so what they are owed went: "
                    +done.sent+" went, "+done.failed+" did not");
            } catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not send what they are owed: "+notNow.getClass().getSimpleName());}
        });
    }

    // ---- shared with me for the first time: waiting, accepted, said to the sender (see Asking) -------------------

    /**
     * Said beside the answer to something that waits here to be accepted (decision 109): "it waits for me to accept it", so
     * the sender does not draw "everybody has it" yet. Only to a device that speaks in answers, as a leaving is; the device is
     * kept, to be told it was accepted once it is ({@link #sayAccepted}).
     */
    private static void sayWaits(Context where,NoteStore store,Keys keys,NoteStore.Contact from,byte[] page,long revision,
                                 NoteStore.Branch.Kind kind,String id) {
        if(where==null||from.agreement.length==0||!doesSpeak(where,from))return;
        store.toldWaits(from.address,kind,id);
        ANSWERS.execute(()->{
            try{aboutAccepting(where,store,keys,from,page,revision,Receipt.WAITS);}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not say it waits to be accepted: "+notNow.getClass().getSimpleName());}
        });
    }

    /**
     * That what waited here was accepted, said to each device that was told it waits, and again on each round until it went.
     * Accepted by Accept here or by one of my own devices accepting it: whichever, it no longer waits. A thing refused or gone
     * is let go of, unsaid. Blocking.
     */
    static void sayAccepted(Context where,NoteStore store,Keys keys) {
        List<String[]> told=store.toldWaits();
        if(told.isEmpty())return;
        for(String[] one:told) {
            String address=one[0],kind=one[1],id=one[2];
            if(!store.stillHere(kind,id)){store.doneTelling(address,kind,id);continue;}
            if(!store.acceptedHere(kind,id))continue;
            NoteStore.Contact them=store.address(address);
            if(them==null||them.agreement.length==0){store.doneTelling(address,kind,id);continue;}
            // Only to a device heard answering, as "it waits" was: kept until it is.
            if(!doesSpeak(where,them))continue;
            try{if(aboutAccepting(where,store,keys,them,sixteen(id),System.currentTimeMillis(),Receipt.ACCEPTED))store.doneTelling(address,kind,id);}
            catch(IllegalArgumentException noId){store.doneTelling(address,kind,id);}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not say it was accepted: "+notNow.getClass().getSimpleName());}
        }
    }

    /** The same, off the screen's thread, right after Accept: see {@link #sayAccepted}. */
    static void acceptedNow(Context where,NoteStore store,Keys keys) {
        ANSWERS.execute(()->{
            try{sayAccepted(where,store,keys);}
            catch(Exception notNow){android.util.Log.w("Mininotes/Post","could not say it was accepted: "+notNow.getClass().getSimpleName());}
        });
    }

    private static boolean aboutAccepting(Context where,NoteStore store,Keys keys,NoteStore.Contact them,byte[] page,long when,int what) throws Exception {
        MaximaNode node=Node.node(where);
        if(node==null)return false;
        MaximaSender.Result said=handTo(where,store,keys,node,them,Envelope.seal(page,when,System.currentTimeMillis(),
            Receipt.wrap(what),keys.signing(),Keys.publicKey(them.agreement)));
        android.util.Log.i("Mininotes/Post",(what==Receipt.WAITS?"told them it waits to be accepted: ":"told them it was accepted: ")
            +(said==null?"no answer":said.statusName));
        return said!=null&&said.isOk();
    }

    // ---- shared with me for the first time: refused, and the sender told (decision 109) ---------------------------

    /** The line a waiting thing is said in, by the device it came from: "Ana shared “Saturday market” with you". */
    private static String waitingSaid(NoteStore store,NoteStore.Contact from,Object[] waits) {
        return FirstShare.title(List.of(from.name),1,store.thingName((NoteStore.Branch.Kind)waits[0],(String)waits[1]));
    }

    /**
     * Refused, something another person shared with me for the first time (the owner, 2026-10-06: "refuse could be silent or
     * inform the sender with the option to send a message back"; decision 109). Told: whoever it is from hears a refusal, with
     * the words, where their build reads one ({@link Receipt#REFUSED}); everybody else who has it, and a sender whose build
     * does not, hears what Unfollow says ({@link Receipt#LEFT_PAGE} and the others), and takes this device off it. Quietly:
     * nobody hears anything. Either way it is then deleted here and nothing more of it is taken in (see
     * {@link NoteStore#refuseShared}): told first, while this device still knows who has it and what to name it by. A folder
     * with no note in it yet has nothing to be named by, as for Unfollow, and is let go here without a word. Blocking.
     *
     * @return how many were told
     */
    static int refuse(Context where,NoteStore store,Keys keys,NoteStore.Branch.Kind kind,String id,boolean tell,String words) {
        java.util.Set<String> who=store.whoHasWaiting(kind,id);
        final long now=System.currentTimeMillis();
        int told=0;
        if(tell) {
            boolean file=kind==NoteStore.Branch.Kind.FILE||kind==NoteStore.Branch.Kind.WAITING;
            String note=store.namingNote(kind,id),origin=store.waitingFrom(kind,id);
            Sharing.Scope scope=file?Sharing.Scope.FILE:kind==NoteStore.Branch.Kind.PAGE?Sharing.Scope.PAGE:store.oldScopeOf(Sharing.Scope.THING,id);
            NoteStore.Contact sender=origin.isEmpty()?null:store.address(origin);
            byte[] about=null;
            try{about=note==null?null:sixteen(note);}catch(IllegalArgumentException older){/* a note from before sharing names nothing */}
            if(about==null||Receipt.left(scope)==0)android.util.Log.i("Mininotes/Post","refused something nothing can name: nobody is told");
            else for(NoteStore.Contact them:store.addresses()) {
                if(!who.contains(them.address)||them.agreement.length==0)continue;
                boolean theSender=them.address.equals(origin)||sender!=null&&sender.signing.length>0&&java.util.Arrays.equals(sender.signing,them.signing);
                try {
                    if(theSender&&knowsRefusals(where,them)){if(sealed(where,store,keys,them,about,now,Receipt.refusal(Receipt.left(scope),words)))told++;}
                    else if(file?knowsLoose(where,them):doesSpeak(where,them)){if(saidOff(where,store,keys,them,about,now,Receipt.left(scope)))told++;}
                } catch(Exception notNow) {
                    android.util.Log.w("Mininotes/Post","could not say this device refused it: "+notNow.getClass().getSimpleName());
                }
            }
        }
        store.refuseShared(kind,id,who,tell,now);
        return told;
    }

    /** Bytes of this build's own, sealed for one device and handed to it: see {@link #saidOff}. */
    private static boolean sealed(Context where,NoteStore store,Keys keys,NoteStore.Contact them,byte[] page,long when,byte[] inner) throws Exception {
        MaximaNode node=Node.node(where);
        if(node==null)return false;
        MaximaSender.Result said=handTo(where,store,keys,node,them,Envelope.seal(page,when,System.currentTimeMillis(),inner,keys.signing(),Keys.publicKey(them.agreement)));
        android.util.Log.i("Mininotes/Post","told them this device refused it: "+(said==null?"no answer":said.statusName));
        return said!=null&&said.isOk();
    }

    /**
     * Somebody refused what this device shared with them, and said so (decision 109): taken off it as a leaving takes them
     * off, and the refusal kept with their words for the Share box, which says Refused on their line. Said once, with the
     * words under it.
     */
    private static Landed refusedHere(NoteStore store,NoteStore.Contact from,String id,Envelope.Opened opened,Receipt.Refused said) {
        boolean off=store.left(from.address,id,said.scope,opened.revision);
        // Refused, so no longer waiting there to be accepted.
        store.unasked(from.address,id);
        String name=store.refusedBy(from.address,id,said.scope,said.words,opened.revision);
        android.util.Log.i("Mininotes/Post","they refused a "+said.scope.name().toLowerCase(java.util.Locale.ROOT)
            +(name==null?" - which is not here, so it changes nothing":off?", and are off it":", and were off it already"));
        if(name==null)return new Landed(null,null);
        return Landed.left(FirstShare.notice(from.name,name,said.words),id).by(from.name,0);
    }

    /** Devices whose build has said it reads a refusal, by the fingerprint of their key: kept, across restarts. */
    private static boolean refusals(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("refusals",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("refusals",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads a refusal (see {@link Receipt#REFUSALS}). A refusal is never sealed for one
     * that has not: a build from before would take it for a note. It hears what Unfollow says instead.
     */
    static boolean knowsRefusals(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("refusals",new java.util.HashSet<String>()).contains(who);
    }

    /** When each device was last told this run that this build reads a refusal, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_REFUSALS=new java.util.concurrent.ConcurrentHashMap<>();

    /** "This build reads a refusal", said to every paired device that has shown it knows what an answer is, as LOOSE is. */
    private static void tellRefusals(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||knowsRefusals(where,them)))continue;
            long[] told=told(where,TOLD_REFUSALS,"refusals",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellRefusals(where,store,keys,them);
        }
    }

    private static void tellRefusals(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_REFUSALS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.REFUSALS),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"refusals",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build reads a refusal"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /** Whether a list gives this device the thing again, later than it left: see {@link #givenAgain}. */
    private static boolean givenAgainIn(NoteStore store,List<Parcel.Member> members,long leftAt) {
        if(store.mySigningKey.isEmpty())return false;
        for(Parcel.Member one:members)
            if(store.mySigningKey.equals(one.key)&&one.level>Sharing.Level.GONE.said()&&one.changed>leftAt)return true;
        return false;
    }

    /**
     * "This build knows about trees", said to every paired device that has shown it knows what an answer is, once a run
     * and again in a while where it was not taken - as {@link Receipt#PERSONS} is. A device that has never answered
     * anything is not told: it may be a build from before answers, which would write these bytes over a note.
     */
    private static void tellTrees(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||knowsTrees(where,them)))continue;
            long[] told=told(where,TOLD_TREES,"trees",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellTrees(where,store,keys,them);
        }
    }

    private static void tellTrees(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_TREES.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.TREE),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"trees",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build knows about trees"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * "This notebook is locked", or "open again", said to every paired device that has said it knows about persons (see
     * {@link Receipt#LOCKED}). Sealed here, while the notebook is still open to say who they are, and handed over off
     * the caller's thread, which is closing the notebook: nothing after this reads it. Open again is said only after a
     * lock was: most openings are nobody's news.
     */
    static void sayLocked(Context where,NoteStore store,Keys keys,boolean locked) {
        try {
            android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            if(!locked&&kept.getLong("saidLocked",0L)==0L)return;
            MaximaNode node=Node.node(where);
            if(node==null||!Node.running())return;
            final List<Object[]> going=new ArrayList<>();
            long now=System.currentTimeMillis();
            for(NoteStore.Contact them:store.addresses()) {
                String who=fingerprint(them);
                if(who.isEmpty()||!them.paired()||them.agreement.length==0||!sendsTo(store,them)||!readsCards(where,who))continue;
                com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,them.contact);
                if(reach==null)continue;
                going.add(new Object[]{reach,Envelope.seal(new byte[16],0,now,Receipt.wrap(locked?Receipt.LOCKED:Receipt.OPENED),
                    keys.signing(),Keys.publicKey(them.agreement)),who});
            }
            kept.edit().putLong("saidLocked",locked?1L:0L).apply();
            // Locked again, nobody is owed "open again" any more.
            if(locked)OPEN_OWED.clear();
            // And the notebook just opened is the one to say it from, before any round has said so.
            else Node.onMetHere(key->sayOpenAgain(where,store,keys,key));
            if(going.isEmpty())return;
            final boolean helpers=Node.helpers();
            ANSWERS.execute(()->{
                int went=0;
                for(Object[] one:going) {
                    boolean ok=false;
                    try{ok=Direct.send(node,(com.eurobuddha.maxima.core.contacts.Contact)one[0],APPLICATION,(byte[])one[1],null,helpers).isOk();}
                    catch(Exception notNow){/* they go on waiting, amber, as before */}
                    if(ok)went++;
                    // Not reached: said again when its door is heard (see sayOpenAgain), not left to whatever it next hears.
                    if(!locked){if(ok)OPEN_OWED.remove((String)one[2]);else OPEN_OWED.add((String)one[2]);}
                }
                android.util.Log.i("Mininotes/Post","said this notebook is "+(locked?"locked":"open again")+" to "+went+" of "+going.size()+" device(s)");
            });
        } catch(Exception notNow) {
            android.util.Log.w("Mininotes/Post","could not say whether this notebook is locked: "+notNow.getClass().getSimpleName());
        }
    }

    /** The devices, by {@link #fingerprint}, that "open again" did not reach: said to each when its door is next heard. */
    private static final java.util.Set<String> OPEN_OWED=java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** "Open again", said to a device just heard on this network, by its identity key, where it did not reach it before. Blocking. */
    static void sayOpenAgain(Context where,NoteStore store,Keys keys,String key) {
        if(OPEN_OWED.isEmpty()||key==null||!Node.running())return;
        for(NoteStore.Contact them:store.addresses()) {
            String kept=them.contact==null?"":them.contact.trim(),who=fingerprint(them);
            if(kept.isEmpty()||!Direct.key(kept).equals(Direct.key(key))||!OPEN_OWED.contains(who))continue;
            try {
                MaximaNode node=Node.node(where);
                com.eurobuddha.maxima.core.contacts.Contact reach=Node.known(where,kept);
                if(node==null||reach==null)return;
                byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.OPENED),keys.signing(),Keys.publicKey(them.agreement));
                boolean ok=Direct.send(node,reach,APPLICATION,sealed,null,Node.helpers()).isOk();
                if(ok)OPEN_OWED.remove(who);
                android.util.Log.i("Mininotes/Post","said this notebook is open again to a device just heard on this network: "+(ok?"taken":"not taken"));
            } catch(Exception notNow){/* said when it is next heard anew */}
            return;
        }
    }

    /** The card, sent to every device of the owner's that reads cards and counts this one as theirs, where it changed. */
    private static void sendCards(Context where,NoteStore store,Keys keys) {
        List<NoteStore.Contact> own=cardsGoTo(where,store);
        if(own.isEmpty()||!Node.running())return;
        try {
            Persons.Me me=me(where);
            List<Persons.Device> others=new ArrayList<>();
            for(NoteStore.Contact one:store.addresses())if(one.mine&&one.paired())
                others.add(new Persons.Device(shortKey(one.signing),shortKey(one.agreement),one.address,one.name,Persons.ACTIVE,one.named));
            Persons.Device self=new Persons.Device(Point.shorten(keys.signing().getPublic()),Point.shorten(keys.agreement().getPublic()),
                store.myAddress==null?"":store.myAddress,me.device,Persons.ACTIVE,me.deviceNamed);
            Persons.Card card=Persons.card(me,self,others);
            byte[] plain=Persons.wrap(card);
            String hash=Courier.hex(java.security.MessageDigest.getInstance("SHA-256").digest(Persons.sameness(card)));
            MaximaNode node=Node.node(where);
            if(node==null)return;
            for(NoteStore.Contact them:own) {
                String who=fingerprint(them);
                // The same card handed within the day, at a restart (decision 225), is not handed again.
                if(!CARDS.containsKey(who)){String kept=saidLately(where,"cardPersons",who);if(kept!=null&&hash.equals(Said.value(kept)))CARDS.put(who,hash);}
                if(hash.equals(CARDS.get(who))||resting(who))continue;
                byte[] sealed=Envelope.seal(Persons.idBytes(me.id),card.newest(),System.currentTimeMillis(),plain,
                    keys.signing(),Keys.publicKey(them.agreement));
                // One device out of reach - only between your devices, with its door not heard here - is not a card
                // kept from the rest, and it is sent again at the next round, as anything not taken is.
                boolean went;
                try{went=handSaying(where,store,keys,node,them,sealed);}
                catch(Exception noWay) {
                    // Said once for each card, not at every round it waits.
                    if(!hash.equals(CARD_WAITS.put(who,hash)))persons("a card waits for one of the owner's devices: "+Unsent.reason(noWay));
                    continue;
                }
                // Why it went, so a log shows a card going round unchanged for what it is.
                String why=CARDS.containsKey(who)?"it changed":"none went since the start, or since their word about persons";
                if(went){CARDS.put(who,hash);said(where,"cardPersons",who,hash);}
                persons("a card of "+card.devices.size()+" device(s) "+(went?"handed to":"not taken for")+" one of the owner's devices ("+why+")");
            }
        } catch(Exception notNow) {
            persons("could not send a card: "+Unsent.reason(notNow));
        }
    }

    /** A key in the short form a card carries, whichever form it is kept in. */
    private static byte[] shortKey(byte[] kept) {
        try{return Point.shorten(Point.read(kept));}catch(Exception notAKey){return kept;}
    }

    /**
     * A card from another device of the owner's, taken in: whose id this device keeps, your name where it was chosen
     * later there, and what your devices are called. A card from a device not marked as theirs here - or one that does
     * not list this device as theirs - changes nothing; nor does one older than the last heard from the same device.
     */
    private static Landed cardArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        Persons.Card card=Persons.open(opened.text);
        String who=fingerprint(from);
        if(card==null||who.isEmpty()){persons("a card that could not be read");return new Landed(null,null);}
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        if(opened.revision<kept.getLong("card."+who,0)){persons("a card older than one already heard: nothing changes");return new Landed(null,null);}
        try {
            Persons.Me was=me(where);
            // Every device paired here, not only those marked yours: a device yours at both ends lists which are yours,
            // and one the switch was never turned for here is still called what you called it. Seen on 0.1.029: a PC
            // with one phone of two marked as the owner's went on showing the other by the name on its pairing code.
            Persons.Taken taken=Persons.take(was,card,from.mine,from.signing,keys.signing().getPublic().getEncoded(),store.devicesNamed());
            if(taken==null){persons("a card from a device that is not the owner's at both ends: nothing changes");return new Landed(null,null);}
            kept.edit().putLong("card."+who,opened.revision).apply();
            // Kept as it came, so the devices of mine it lists can be linked here, and vouched for (decision 268).
            boolean listsNew=keepCard(where,who,opened.text);
            // And what it lists as mine that is paired here as somebody else's, mine here too (decision 290).
            listsNew|=adoptVouched(where,store,keys)>0;
            // It is the owner's at both ends and reads cards, or it could not have sent one.
            readsCards(where,who,true);
            keepMe(where,store,was,taken.me);
            // Counted only where something here changed: a device is believed about itself at every card, and a log
            // saying "1 device name" at each of them read as a name that never stuck.
            int named=0;
            for(Persons.Device one:taken.names.values())if(store.ownDeviceNamed(one.signing,one.name,one.decided))named++;
            boolean renamed=taken.renamed&&!taken.me.name.equals(was.name);
            boolean listed=listedAsYours(where,store,card);
            persons("a card taken in"+(taken.adopted?": this device is now one person with it":"")
                +(renamed?", your name chosen there later":"")+(named==0?", nothing new":", "+named+" device name(s) changed"));
            return taken.adopted||renamed||named>0||listed||listsNew?Landed.devices():new Landed(null,null);
        } catch(Exception unreadable) {
            persons("a card that could not be weighed: "+unreadable.getClass().getSimpleName());
            return new Landed(null,null);
        }
    }

    // ---- my devices, each linked with the others: see Introducing (decision 268) --------------------------------------

    /**
     * A card from a device of mine, kept as it came, by its fingerprint. Whether it differs from the one kept before: My devices
     * is drawn again then.
     */
    private static boolean keepCard(Context where,String who,byte[] card) {
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        String now=java.util.Base64.getEncoder().encodeToString(card);
        if(now.equals(kept.getString("cardKept."+who,"")))return false;
        kept.edit().putString("cardKept."+who,now).apply();
        return true;
    }

    /** The card last kept from a device, by fingerprint; null where none was, or it does not read. */
    static Persons.Card cardKept(Context where,String who) {
        if(where==null||who==null||who.isEmpty())return null;
        String kept=where.getSharedPreferences("post",Context.MODE_PRIVATE).getString("cardKept."+who,"");
        if(kept.isEmpty())return null;
        try{return Persons.open(java.util.Base64.getDecoder().decode(kept));}catch(IllegalArgumentException broken){return null;}
    }

    /**
     * The cards kept from devices that are mine at both ends now: marked mine here and counting this one as theirs. A card from
     * a device no longer mine here says nothing any more, whatever it said.
     */
    static List<Persons.Card> cardsKept(Context where,NoteStore store) {
        List<Persons.Card> all=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses()) {
            if(!them.mine||!them.paired())continue;
            String who=fingerprint(them);
            if(!mineThere(where,who))continue;
            Persons.Card card=cardKept(where,who);
            if(card!=null)all.add(card);
        }
        return all;
    }

    /** Every device paired here, by key in one form, and whether it is marked mine here. */
    private static java.util.Map<String,Boolean> pairedHere(NoteStore store) {
        java.util.Map<String,Boolean> here=new java.util.HashMap<>();
        for(NoteStore.Contact one:store.addresses())if(one.paired())here.merge(Persons.key(one.signing),one.mine,(a,b)->a||b);
        return here;
    }

    /**
     * The devices of mine another device of mine lists and this one is not linked with as mine: under My devices with Link
     * where they are not paired here, and on their line in People with This is my device where they are (see
     * {@link Introducing#notLinked}).
     */
    static List<Introducing.Offered> notLinked(Context where,NoteStore store,Keys keys) {
        try {
            java.util.Map<String,Boolean> paired=pairedHere(store);
            // Not one forgotten here (decision 277), until it is paired again.
            java.util.Set<String> forgotten=store.forgotten();
            for(String key:forgotten)if(paired.containsKey(key))store.forgotten(key,false);
            List<Introducing.Offered> out=new ArrayList<>();
            for(Introducing.Offered one:Introducing.notLinked(cardsKept(where,store),keys.signing().getPublic().getEncoded(),paired))
                if(one.pairedHere||!forgotten.contains(one.key))out.add(one);
            return out;
        }
        catch(Exception unreadable){return new ArrayList<>();}
    }

    /**
     * The devices of mine another of mine lists that are paired here as somebody else's, taken as mine here too (decision 290).
     * Blocking.
     */
    static int adoptVouched(Context where,NoteStore store,Keys keys) {
        int took=0;
        for(Introducing.Offered one:Introducing.adopted(notLinked(where,store,keys)))
            if(takeAsMine(where,store,one.device)!=null)took++;
        if(took>0)persons(took+" device(s) another of mine counts as mine, paired here as somebody else's: now mine here too");
        return took;
    }

    /**
     * Who in People may be a device of mine, by address: listed as mine on a card from another device of mine, or saying it
     * counts this one as its owner's. Their line has This is my device; nothing is made mine until it is pressed.
     */
    static java.util.Set<String> maybeMine(Context where,NoteStore store,List<Introducing.Offered> offered) {
        java.util.Set<String> keys=new java.util.HashSet<>();
        if(offered!=null)for(Introducing.Offered one:offered)if(one.pairedHere)keys.add(one.key);
        java.util.Set<String> out=new java.util.HashSet<>();
        for(NoteStore.Contact one:store.addresses()) {
            if(one.mine||!one.paired())continue;
            String who=fingerprint(one);
            if(keys.contains(Persons.key(one.signing))||mineThere(where,who)||listedAsYours(where,who))out.add(one.address);
        }
        return out;
    }

    /**
     * A device of mine, taken as mine here: one paired already, marked so; one a card lists, paired from what the card says of
     * it and waited for until it answers, since it has never heard of this one. Its name among my devices is the card's.
     * Blocking.
     *
     * @return the device as it now stands here, or null where it could not be kept
     */
    private static NoteStore.Contact takeAsMine(Context where,NoteStore store,Persons.Device device) {
        String key=Persons.key(device.signing);
        if(key.isEmpty()||device.agreement.length==0||device.address.trim().isEmpty())return null;
        NoteStore.Contact had=null;
        for(NoteStore.Contact one:store.addresses())if(one.paired()&&key.equals(Persons.key(one.signing)))had=one;
        if(had==null) {
            store.pairedWith(device.address.trim(),device.name,true,device.agreement,device.signing);
            // Nothing goes to it until it answers: it does not know this device yet, and a build from before never will.
            store.accepting(device.address.trim(),device.name,Linking.SCOPE,Introducing.TARGET,true);
        }
        String address=had==null?device.address.trim():had.address;
        store.setMine(address,true);
        store.ownDevicesAre(me(where).id);
        if(!device.name.isEmpty()&&(had==null||Persons.later(device.decided,had.named,false)))store.ownDeviceNamed(device.signing,device.name,device.decided);
        return store.address(address);
    }

    /**
     * Link pressed here, on a device of mine another of mine lists (the owner's own word, on this device): taken as mine, and
     * asked to take this one as its owner's too, which it does where a card of its own lists this device. Blocking.
     *
     * @return the device as it now stands here, or null where it could not be kept
     */
    static NoteStore.Contact linkMine(Context where,NoteStore store,Keys keys,Persons.Device device) {
        NoteStore.Contact them=device==null?null:takeAsMine(where,store,device);
        if(them==null)return null;
        link("Link pressed on a device of the owner's another of theirs lists: taken as mine, asked to answer");
        askMine(where,store,keys,them);
        return them;
    }

    /**
     * This is my device, pressed on somebody paired here as a person, or My device turned on: marked mine, and told, so it
     * takes this one as its owner's too where a card there lists it, or where it counted this one as mine already. Blocking.
     */
    static NoteStore.Contact linkMine(Context where,NoteStore store,Keys keys,NoteStore.Contact who) {
        if(who==null)return null;
        store.setMine(who.address,true);
        store.ownDevicesAre(me(where).id);
        NoteStore.Contact them=store.address(who.address);
        if(them==null||!them.paired())return them;
        link("a device paired here counted as the owner's: told so");
        askMine(where,store,keys,them);
        return them;
    }

    /** The hello of a device of mine to another, asking to be answered, and that this build knows about persons: off this thread, where the node is up. */
    private static void askMine(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        ANSWERS.execute(()->{
            if(!Node.running())return;
            askToLink(where,store,keys,them);
            NoteStore.Contact now=store.address(them.address);
            if(now!=null)tellPersons(where,store,keys,now);
        });
    }

    /**
     * A hello naming {@link Introducing#TARGET}: from a device a card here vouches for (see {@link Introducing#vouched}), taken
     * as mine, paired where it was not, met, answered and sent what it is owed. Null where no card here vouches for it: it is
     * then weighed as any LINK hello is.
     */
    private static Landed mineHeard(Context where,NoteStore store,Keys keys,Hello.Said said,NoteStore.Contact known,boolean asks) {
        Persons.Device vouch=Introducing.vouched(cardsKept(where,store),said.signing);
        // The key to seal for is the card's too: a hello signed by a device of mine but naming another key is not taken as it.
        if(vouch==null||!Persons.key(vouch.agreement).equals(Persons.key(said.agreement))) {
            link("a device asked to link as the owner's, and no card here from another device of theirs lists it: weighed as any");
            return null;
        }
        boolean was=known!=null&&known.mine;
        // Kept where its hello says it is now: the card's address is where its sender last saw it.
        String now=said.address==null||said.address.trim().isEmpty()?vouch.address:said.address.trim();
        Persons.Device here=new Persons.Device(vouch.signing,vouch.agreement,now,vouch.name.isEmpty()?said.name:vouch.name,vouch.state,vouch.decided);
        NoteStore.Contact them=known!=null?linkedAsMine(where,store,known,here):takeAsMine(where,store,here);
        if(them==null)return null;
        // It said hello, so it is there and has this device's key: not waited for.
        store.answered(them.address);
        link("a device another of the owner's lists asked to link"+(was?": mine already":": taken as mine")+(asks?", answered":""));
        final String address=them.address;
        ANSWERS.execute(()->{
            if(!Node.running())return;
            met(where,store,address,said.address);
            if(asks)answerLink(where,store,keys,address,said.address);
            NoteStore.Contact up=store.address(address);
            if(up!=null)tellPersons(where,store,keys,up);
            owedTo(where,store,keys,address);
        });
        return Landed.people("");
    }

    /** A device paired here, vouched for as mine: marked so, and called what my devices call it. */
    private static NoteStore.Contact linkedAsMine(Context where,NoteStore store,NoteStore.Contact known,Persons.Device vouch) {
        if(!known.mine){store.setMine(known.address,true);store.ownDevicesAre(me(where).id);}
        if(!vouch.name.isEmpty()&&Persons.later(vouch.decided,known.named,false))store.ownDeviceNamed(vouch.signing,vouch.name,vouch.decided);
        return store.address(known.address);
    }

    /** When each introduction was last said this run, by the line kept. */
    private static final java.util.Map<String,Long> INTRODUCED=new java.util.concurrent.ConcurrentHashMap<>();
    /** When each device was last told this run that this build links devices of mine introduced to it, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_LINKS=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A device of mine just added here, linked with my other devices too, each but those unticked (decision 268): kept to be
     * said to both of each two, and said once both can hear it. Blocking.
     *
     * @param skip the addresses of my devices unticked on the way; null or empty for none
     */
    static void alsoLink(Context where,NoteStore store,String added,java.util.Collection<String> skip) {
        NoteStore.Contact them=added==null?null:store.address(added);
        if(where==null||them==null||!them.paired())return;
        String who=fingerprint(them);
        List<String> others=new ArrayList<>();
        for(NoteStore.Contact one:store.addresses()) {
            if(!one.mine||!one.paired()||one.address.equals(added)||skip!=null&&skip.contains(one.address))continue;
            String other=fingerprint(one);
            if(!other.isEmpty()&&!other.equals(who)&&!others.contains(other))others.add(other);
        }
        if(who.isEmpty()||others.isEmpty())return;
        synchronized(INTRODUCED) {
            android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            java.util.Set<String> now=Introducing.introduce(kept.getStringSet("introduce",new java.util.HashSet<String>()),who,others,System.currentTimeMillis());
            kept.edit().putStringSet("introduce",new java.util.HashSet<>(now)).apply();
        }
        link("a device of the owner's added here, to be linked with "+others.size()+" other(s) of theirs");
    }

    /** The devices of mine unticked in Add my device's box, by address: kept while its code is up, for when it is scanned. */
    static void alsoLinkSkip(Context where,java.util.Collection<String> skip) {
        if(where==null)return;
        where.getSharedPreferences("settings",Context.MODE_PRIVATE).edit()
            .putStringSet("alsoLinkSkip",new java.util.HashSet<>(skip==null?new java.util.HashSet<String>():skip)).apply();
    }

    private static java.util.Set<String> alsoLinkSkip(Context where) {
        return new java.util.HashSet<>(where.getSharedPreferences("settings",Context.MODE_PRIVATE).getStringSet("alsoLinkSkip",new java.util.HashSet<String>()));
    }

    /** A device whose build has said it links devices of mine introduced to it ({@link Receipt#LINKS_MINE}), kept. Whether it is the first time. */
    private static boolean linksMineHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("linksMine",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("linksMine",all).apply();
        return true;
    }

    private static boolean linksMine(Context where,String fingerprint) {
        return where!=null&&fingerprint!=null&&!fingerprint.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("linksMine",new java.util.HashSet<String>()).contains(fingerprint);
    }

    /** "This build links devices of mine introduced to it", said to each of the owner's own devices, as stamps are. */
    private static void tellLinksMine(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.mine||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||linksMine(where,who)))continue;
            long[] told=told(where,TOLD_LINKS,"linksMine",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellLinksMine(where,store,keys,them);
        }
    }

    private static void tellLinksMine(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_LINKS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.LINKS_MINE),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"linksMine",who,"1");}
            link("said this build links devices of the owner's introduced to it"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * What waits to be introduced, said: to each device told, about the other, once both have said they link devices of mine
     * introduced to them, the one told counts this one as mine, and its card does not list the other yet. Dropped once it
     * does, once either is forgotten or no longer mine here, and after two months. Blocking.
     */
    private static void introduce(Context where,NoteStore store,Keys keys) {
        java.util.Set<String> kept;
        synchronized(INTRODUCED){kept=new java.util.HashSet<>(where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("introduce",new java.util.HashSet<String>()));}
        if(kept.isEmpty())return;
        java.util.Map<String,NoteStore.Contact> byWho=new java.util.HashMap<>();
        for(NoteStore.Contact one:store.addresses())if(one.paired()){String who=fingerprint(one);if(!who.isEmpty())byWho.put(who,one);}
        long now=System.currentTimeMillis();
        java.util.Set<String> gone=new java.util.HashSet<>();
        for(String line:kept) {
            Introducing.Pending one=Introducing.pending(line);
            NoteStore.Contact told=one==null?null:byWho.get(one.told),about=one==null?null:byWho.get(one.about);
            boolean toldMine=told!=null&&told.mine,aboutMine=about!=null&&about.mine;
            Introducing.Step step=Introducing.weigh(one,now,toldMine,aboutMine,toldMine&&aboutMine&&Introducing.done(cardKept(where,one.told),about.signing),
                one!=null&&linksMine(where,one.told),one!=null&&linksMine(where,one.about),one!=null&&mineThere(where,one.told));
            if(step==Introducing.Step.DROP){gone.add(line);continue;}
            if(step==Introducing.Step.DONE){gone.add(line);link("an introduction between two devices of the owner's is done");continue;}
            if(step!=Introducing.Step.SAY||!Node.running()||resting(one.told))continue;
            Long last=INTRODUCED.get(line);
            if(!Introducing.due(last==null?0:last,now))continue;
            INTRODUCED.put(line,now);
            try {
                MaximaNode node=Node.node(where);
                if(node==null)return;
                byte[] sealed=Envelope.seal(Introducing.which(about.signing),0,now,Receipt.wrap(Receipt.LINK_MINE),keys.signing(),Keys.publicKey(told.agreement));
                boolean went=handSaying(where,store,keys,node,told,sealed);
                link("told a device of the owner's to link with another of theirs"+(went?"":": not taken"));
            } catch(Exception notNow){/* said again in a while */}
        }
        if(gone.isEmpty())return;
        synchronized(INTRODUCED) {
            android.content.SharedPreferences prefs=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            java.util.Set<String> left=new java.util.HashSet<>(prefs.getStringSet("introduce",new java.util.HashSet<String>()));
            left.removeAll(gone);
            prefs.edit().putStringSet("introduce",left).apply();
        }
    }

    /** What waits to be introduced here, as kept: for the tests, and the log. */
    static java.util.Set<String> introductions(Context where) {
        synchronized(INTRODUCED){return new java.util.HashSet<>(where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("introduce",new java.util.HashSet<String>()));}
    }

    /**
     * Told by a device of mine to link with another device of mine: taken as mine where that device's card, kept here, lists it,
     * and asked to answer. Only from a device mine at both ends; the owner's tick, on it, is the word (decision 268).
     */
    private static Landed introducedHere(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        String who=fingerprint(from);
        if(!from.mine||!mineThere(where,who)){link("told to link with a device by a device that is not the owner's at both ends: nothing changes");return new Landed(null,null);}
        Persons.Device about;
        try{about=Introducing.introduced(cardKept(where,who),opened.page,keys.signing().getPublic().getEncoded());}catch(Exception unreadable){about=null;}
        if(about==null){link("told to link with a device of the owner's its card does not list yet: it is said again");return new Landed(null,null);}
        NoteStore.Contact had=null;
        for(NoteStore.Contact one:store.addresses())if(one.paired()&&Persons.key(one.signing).equals(Persons.key(about.signing)))had=one;
        if(had!=null&&had.mine){link("told to link with a device of the owner's that is mine here already");return new Landed(null,null);}
        NoteStore.Contact them=takeAsMine(where,store,about);
        if(them==null)return new Landed(null,null);
        link("told by a device of the owner's to link with another of theirs: taken as mine, asked to answer");
        askMine(where,store,keys,them);
        return Landed.people("");
    }

    // ---- people in groups, the same on every device of the owner's: see Groups (decision 100) ------------------------

    /** When each device was last told this run that this build reads groups, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_GROUPS=new java.util.concurrent.ConcurrentHashMap<>();
    /** The last groups card each device was sent this run, by fingerprint, as a hash: sent again only when it changes. */
    private static final java.util.Map<String,String> GROUP_CARDS=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads groups, kept across restarts. Whether it is the first time. */
    private static boolean groupsHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("groups",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("groups",all).apply();
        return true;
    }

    /** Whether a device's build has said it reads groups ({@link Receipt#GROUPS}). A card is never sealed for one that has not. */
    static boolean readsGroups(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("groups",new java.util.HashSet<String>()).contains(who);
    }

    /**
     * Who a groups card goes to: every device paired here and marked yours, whose build has said it reads groups and that
     * counts this one as its owner's too (see {@link Groups#goesTo}). A build from before is never among them.
     */
    static List<NoteStore.Contact> groupsGoTo(Context where,NoteStore store) {
        List<NoteStore.Contact> own=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses())
            if(them.paired()&&sendsTo(store,them)&&Groups.goesTo(them.mine,readsGroups(where,them),mineThere(where,fingerprint(them))))own.add(them);
        return own;
    }

    /**
     * "This build reads groups", said to each of the owner's own devices that has shown it knows what an answer is, once a
     * run and again in a while where it was not taken, as {@link Receipt#LOOSE} is. Nobody else is told: nobody else is
     * ever sent a card.
     */
    private static void tellGroups(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.mine||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsGroups(where,them)))continue;
            long[] told=told(where,TOLD_GROUPS,"groups",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellGroups(where,store,keys,them);
        }
    }

    private static void tellGroups(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_GROUPS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.GROUPS),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"groups",who,"1");}
            groups("said this build reads groups"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /** The groups card, sent to every device of the owner's that reads one, where it changed since it last went. Blocking. */
    static void sendGroups(Context where,NoteStore store,Keys keys) {
        List<NoteStore.Contact> own=groupsGoTo(where,store);
        if(own.isEmpty()||!Node.running())return;
        try {
            Groups.Card card=store.groupsCard();
            // Nobody has made a group or given one anything: nothing to say, and a device with none has nothing to hear.
            if(card.groups.isEmpty()&&card.given.isEmpty())return;
            byte[] plain=Groups.wrap(card);
            String hash=Courier.hex(java.security.MessageDigest.getInstance("SHA-256").digest(plain));
            MaximaNode node=Node.node(where);
            if(node==null)return;
            for(NoteStore.Contact them:own) {
                String who=fingerprint(them);
                if(hash.equals(GROUP_CARDS.get(who))||resting(who))continue;
                byte[] sealed=Envelope.seal(new byte[16],card.newest(),System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
                boolean went;
                // One device out of reach is not a card kept from the rest: it goes again at the next round.
                try{went=handSaying(where,store,keys,node,them,sealed);}catch(Exception noWay){continue;}
                if(went)GROUP_CARDS.put(who,hash);
                groups("a groups card of "+card.groups.size()+" group(s) "+(went?"handed to":"not taken for")+" one of the owner's devices");
            }
        } catch(Exception notNow) {
            groups("could not send a groups card: "+Unsent.reason(notNow));
        }
    }

    /**
     * What groups gave or took away, sent: each thing whose people changed goes whole to everybody it reaches, carrying
     * its list, as a change of role does. Blocking. Counted together.
     */
    static Done sent(Context where,NoteStore store,Keys keys,List<Groups.Changed> changed,String why) {
        int sent=0,failed=0;String said="";List<Unsent.Problem> problems=new ArrayList<>();
        if(changed==null||changed.isEmpty())return new Done(0,0,"");
        for(Groups.Changed one:changed) {
            try {
                Done done=changed(where,store,keys,NoteStore.kindFor(one.scope),one.target);
                sent+=done.sent;failed+=done.failed;problems.addAll(done.problems);if(said.isEmpty())said=done.why;
            } catch(Exception notNow){failed++;if(said.isEmpty())said=Unsent.reason(notNow);}
        }
        // Taken off by a group is taken off: they are told, as Remove tells them.
        removedAgain(where,store,keys);
        groups(changed.size()+" thing(s) "+why+": "+sent+" went, "+failed+" did not");
        return new Done(sent,failed,said,problems);
    }

    /**
     * Your groups changed here (a group made, named or deleted, somebody put in or taken out, a thing given to a group or
     * taken from it): what that gives or takes, sent, and the card to your other devices. Blocking.
     */
    static Done groupsChanged(Context where,NoteStore store,Keys keys,List<Groups.Changed> changed) {
        Done done=sent(where,store,keys,changed,"given by a group, or taken away");
        sendGroups(where,store,keys);
        return done;
    }

    /**
     * A groups card from another device of the owner's, taken in row by row, the later decision standing; then what the
     * groups give here, given, and sent off this thread. A card from a device not marked as yours here changes nothing.
     */
    private static Landed groupsArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        if(!from.mine){groups("a groups card from a device that is not the owner's: nothing changes");return new Landed(null,null);}
        Groups.Card card=Groups.open(opened.text);
        if(card==null){groups("a groups card that could not be read");return new Landed(null,null);}
        // It sent one, so it reads them.
        groupsHeard(where,fingerprint(from));
        boolean took=store.mergeGroups(card);
        final List<Groups.Changed> moved=store.applyGroups();
        // Something just arrived, so the node is up; where it is not, nothing is started for this, and the next round sends it.
        if(!moved.isEmpty())ANSWERS.execute(()->{if(Node.running())sent(where,store,keys,moved,"given by a group from another device of yours, or taken away");});
        groups("a groups card taken in"+(took?"":", nothing new")+(moved.isEmpty()?"":", "+moved.size()+" thing(s) given or taken away"));
        return took||!moved.isEmpty()?Landed.groups():new Landed(null,null);
    }

    // ---- stamps on Minima, the same on every device of the owner's: see StampCard (decision 252) -------------------------

    /** When each device was last told this run that this build reads stamps, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_STAMPS=new java.util.concurrent.ConcurrentHashMap<>();
    /** The last stamps card each device was sent this run, by fingerprint, as a hash: sent again only when it changes. */
    private static final java.util.Map<String,String> STAMP_CARDS=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads stamps, kept across restarts. Whether it is the first time. */
    private static boolean stampsHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("stamps",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("stamps",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads stamps ({@link Receipt#STAMPS}), and with them a key card: neither is ever
     * sealed for one that has not.
     */
    static boolean readsStamps(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("stamps",new java.util.HashSet<String>()).contains(who);
    }

    /**
     * Who the card of persons goes to: every device paired here and marked yours, whose build reads cards and that counts this
     * one as its owner's too (see {@link Persons#goesTo}). Whatever it may do with everything, nothing at all included.
     */
    static List<NoteStore.Contact> cardsGoTo(Context where,NoteStore store) {
        List<NoteStore.Contact> own=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(them.paired()&&Persons.goesTo(them.mine,readsCards(where,who),mineThere(where,who)))own.add(them);
        }
        return own;
    }

    /**
     * Who a stamps card goes to: every device paired here and marked yours, whose build has said it reads stamps and that
     * counts this one as its owner's too (see {@link StampCard#goesTo}). A build from before, and anybody else's device, never.
     */
    static List<NoteStore.Contact> stampsGoTo(Context where,NoteStore store) {
        List<NoteStore.Contact> own=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses())
            if(them.paired()&&them.agreement.length>0&&sendsTo(store,them)&&StampCard.goesTo(them.mine,readsStamps(where,them),mineThere(where,fingerprint(them))))own.add(them);
        return own;
    }

    /** "This build reads stamps", said to each of the owner's own devices, as {@link #tellGroups} says its own. */
    private static void tellStamps(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.mine||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsStamps(where,them)))continue;
            long[] told=told(where,TOLD_STAMPS,"stamps",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellStamps(where,store,keys,them);
        }
    }

    private static void tellStamps(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_STAMPS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.STAMPS),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"stamps",who,"1");}
            stamps("said this build reads stamps"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /** The stamps card as it is here now: every stamp kept, the newest first, as many as one envelope takes. */
    static byte[] stampsCard(NoteStore store){return StampCard.wrap(store.stampsCard());}

    /**
     * The stamps card, sent to every device of the owner's that reads one, where it changed since it last went: a stamp made
     * or found on Minima here, or one that came from another device. Blocking.
     */
    static void sendStamps(Context where,NoteStore store,Keys keys) {
        List<NoteStore.Contact> own=stampsGoTo(where,store);
        if(own.isEmpty()||!Node.running())return;
        try {
            java.util.Map<String,StampCardFor> cards=stampCards(where,store,own);
            // Nothing stamped here: nothing to say, and a device with none has nothing to hear.
            if(cards.isEmpty())return;
            int went=0,missed=0;MaximaNode node=null;
            for(NoteStore.Contact them:own) {
                StampCardFor card=cards.get(them.address);
                if(card==null)continue;
                String who=fingerprint(them);
                if(card.hash().equals(STAMP_CARDS.get(who))||resting(who))continue;
                // The node is asked for only where a card is to go: a round where every card went starts nothing.
                if(node==null&&(node=Node.node(where))==null)return;
                String hash=card.hash();byte[] plain=card.plain();
                byte[] sealed=Envelope.seal(new byte[16],card.newest(),System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
                boolean handed;
                // One device out of reach is not a card kept from the rest: it goes again at the next round.
                try{handed=handSaying(where,store,keys,node,them,sealed);}catch(Exception noWay){missed++;continue;}
                if(handed){STAMP_CARDS.put(who,hash);went++;}else missed++;
            }
            if(went+missed>0)stamps("stamps card: "+store.stampsCard().size()+" row(s) here, sent to "+went+" device(s)"
                +(missed>0?", "+missed+" out of reach":""));
        } catch(Exception notNow) {
            stamps("could not send a stamps card: "+Unsent.reason(notNow));
        }
    }

    /**
     * The stamps card each of these devices of the owner's is to have, by address, as {@link StampCard#wrap} makes it: none for
     * a device nothing stamped reaches. A device of mine given everything has every stamp; one added with nothing yet (decision
     * 255) only those of what is shared with it, so the text of a stamped note never reaches a device it was not shared with.
     */
    static java.util.Map<String,StampCardFor> stampCards(Context where,NoteStore store,List<NoteStore.Contact> own) {
        java.util.Map<String,StampCardFor> out=new java.util.HashMap<>();
        if(own.isEmpty())return out;
        // Kept while nothing it reads changed (decision 261): the notebook, and which devices it is for.
        StringBuilder key=new StringBuilder(store.changes());
        for(NoteStore.Contact them:own)key.append('/').append(them.address);
        Kept<java.util.Map<String,StampCardFor>> kept=CARDS_KEPT;
        java.util.Map<String,StampCardFor> known=kept==null?null:kept.at(store,key.toString());
        if(known!=null)return known;
        List<StampCard.Row> rows=store.stampsCard();
        if(rows.isEmpty())return out;
        java.util.Set<String> everything=givenEverything(store);
        for(NoteStore.Contact them:own) {
            List<StampCard.Row> its=everything.contains(them.address)?rows:stampsReaching(store,rows,them.address);
            if(its.isEmpty())continue;
            byte[] plain=StampCard.wrap(its);
            out.put(them.address,new StampCardFor(plain,StampCard.newest(its),sha256(plain)));
        }
        out=java.util.Collections.unmodifiableMap(out);
        CARDS_KEPT=new Kept<>(store,key.toString(),out);
        return out;
    }

    /** One device's stamps card: its bytes, its newest stamp's time, which the envelope carries, and its hash. */
    record StampCardFor(byte[] plain,long newest,String hash) {}

    private static String sha256(byte[] bytes) {
        try{return Courier.hex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException never){throw new IllegalStateException(never);}
    }

    /** The devices given everything here: a library rule of theirs that is not gone. */
    static java.util.Set<String> givenEverything(NoteStore store) {
        java.util.Set<String> out=new java.util.HashSet<>();
        for(Sharing.Rule rule:store.shares())
            if(rule.scope==Sharing.Scope.LIBRARY&&Sharing.EVERYTHING.equals(rule.target)&&rule.level!=Sharing.Level.GONE)out.add(rule.address);
        return out;
    }
    /**
     * The stamps of what reaches one device (decision 255): a note shared with it, or a file shared with it, or attached to
     * a note shared with it. A file of the drive, never in the notebook, reaches nobody but a device given everything.
     */
    static List<StampCard.Row> stampsReaching(NoteStore store,List<StampCard.Row> rows,String address) {
        List<StampCard.Row> out=new ArrayList<>();
        // The rules read once, not once a stamp (decision 261).
        List<Sharing.Rule> rules=store.shares();
        for(StampCard.Row row:rows) {
            boolean note=Integritas.NOTE.equals(row.kind)||Integritas.TITLED.equals(row.kind);
            boolean reaches;
            if(note)reaches=store.reaches(rules,Sharing.Scope.PAGE,row.id).containsKey(address);
            else if(Integritas.FILE.equals(row.kind)) {
                reaches=store.reaches(rules,Sharing.Scope.FILE,row.id).containsKey(address);
                if(!reaches){NoteStore.Held held=store.file(row.id);
                    reaches=held!=null&&held.held==NoteStore.Branch.Kind.PAGE&&store.reaches(rules,Sharing.Scope.PAGE,held.note).containsKey(address);}
            } else reaches=false;
            if(reaches)out.add(row);
        }
        return out;
    }

    /**
     * A stamp was made here, or found on Minima, or learned more (decision 252): the card goes to the owner's other devices
     * now, off the caller's thread, and not only at the next round. Never the reason a node is started. Each app calls it
     * after it keeps a stamp and when its watch hears one landed.
     */
    static void stampsChanged(Context where,NoteStore store,Keys keys) {
        if(where==null||store==null||keys==null)return;
        ANSWERS.execute(()->{if(Node.running()){sendStamps(where,store,keys);sendStampsShared(where,store,keys);}});
    }

    /**
     * A stamps card from another device of the owner's, taken in (see NoteStore#stampsArrived): what is new kept, what is
     * known learning only what it lacks. From a device not marked as yours and paired here, nothing changes. Where anything
     * did, the card goes on to the owner's other devices, and the screens draw the stamps again.
     */
    private static Landed stampsArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        if(from==null||!from.mine||!from.paired()){stamps("a stamps card from a device that is not the owner's: nothing changes");return new Landed(null,null);}
        List<StampCard.Row> rows=StampCard.open(opened.text);
        if(rows==null){stamps("a stamps card that could not be read");return new Landed(null,null);}
        // It sent one, so it reads them.
        stampsHeard(where,fingerprint(from));
        NoteStore.StampsTaken took=store.stampsArrived(rows);
        stamps("stamps card from one of mine: "+rows.size()+" row(s), "+took.added()+" new, "+took.updated()+" updated");
        if(!took.changed())return new Landed(null,null);
        stampsChanged(where,store,keys);
        return Landed.stamps(took.hashes());
    }

    /**
     * Stamps that came with something shared with this device (decision 251), taken in: where anything changed, said to the
     * log, and the owner's other devices sent the card, as for a stamp made here. What was taken, or null.
     */
    private static NoteStore.StampsTaken stampsWith(Context where,NoteStore store,Keys keys,NoteStore.StampsTaken took) {
        stamps("stamps with what was shared: "+took.added()+" new, "+took.updated()+" updated");
        if(took.changed())stampsChanged(where,store,keys);
        return took;
    }

    /** What landed, saying where stamps came with it so every line, box and icon showing them is drawn again. */
    private static Landed stamped(Landed landed,NoteStore.StampsTaken took) {
        if(took==null||!took.changed())return landed;
        landed.stamps=true;
        java.util.Set<String> all=new java.util.LinkedHashSet<>(landed.stamped);all.addAll(took.hashes());
        landed.stamped=java.util.Collections.unmodifiableSet(all);
        return landed;
    }

    /** One line for the log about stamps: counts, never a name, a fingerprint or a key. */
    private static void stamps(String said){android.util.Log.i("Mininotes/Post",said);}

    // ---- stamps on Minima travelling with what is shared with somebody else (decision 251) --------------------------------

    /**
     * What the stamps of a thing said when it last went to a device, by address and thing: see {@link StampCard#said}. Kept
     * across restarts, so a thing goes again for its stamps only when they changed since.
     */
    static void stampsWent(Context where,String address,String thing,List<StampCard.Row> stamps) {
        if(where==null||address==null||thing==null)return;
        where.getSharedPreferences("stampsshared",Context.MODE_PRIVATE).edit().putString(address+"\n"+thing,StampCard.said(stamps)).apply();
        STAMPS_WENT.incrementAndGet();
    }

    /** How many times what went with its stamps was written down this run: part of what {@link #stampsOwed} is kept by. */
    private static final java.util.concurrent.atomic.AtomicLong STAMPS_WENT=new java.util.concurrent.atomic.AtomicLong();

    /**
     * Something an upkeep round works out from the whole notebook, kept with what it was worked out from (decision 261): the
     * notebook, its {@link NoteStore#changes}, and whatever else it read. On the Graphene on 0.4.030 the stamps owed and the
     * stamps cards, worked out again every half minute, walked every stamp and asked the notebook who has each, most of the
     * 600 s of a core the round took in under three hours; a round where nothing was written worked out the same again.
     */
    private record Kept<T>(Object store,String key,T value) {
        T at(Object store,String key){return this.store==store&&this.key.equals(key)?value:null;}
    }
    private static volatile Kept<List<StampsOwed>> OWED_KEPT;
    private static volatile Kept<java.util.Map<String,StampCardFor>> CARDS_KEPT;
    /** Nothing kept: the next round works everything out again. For a test timing the work itself. */
    static void forgetKept(){OWED_KEPT=null;CARDS_KEPT=null;}

    /** One thing owed to one device again for its stamps: a note, or a file on its own in its sleeve. */
    record StampsOwed(String thing,boolean sleeve,String address) {}

    /**
     * Each thing stamped here that is shared with somebody else, to each device it goes to whose last copy said other stamps
     * than it has now (decision 251): a stamp made, found on Minima or given its NFT after it was shared went nowhere until
     * the thing was next changed, since what is sent again is decided by its revision. It goes again whole at the same
     * revision, as Sync sends it, which the far end takes as what it has and answers. Not to the owner's own devices, which
     * have every stamp in their stamps card; not from a device that only reads a note, whose copy nobody takes.
     */
    static List<StampsOwed> stampsOwed(Context where,NoteStore store) {
        if(where==null)return new ArrayList<>();
        // Kept while nothing it reads changed: the notebook, and what went with its stamps (decision 261).
        String key=System.identityHashCode(where)+"/"+store.changes()+"/"+STAMPS_WENT.get();
        Kept<List<StampsOwed>> kept=OWED_KEPT;
        List<StampsOwed> known=kept==null?null:kept.at(store,key);
        if(known!=null)return new ArrayList<>(known);
        List<StampsOwed> owed=new ArrayList<>();
        android.content.SharedPreferences went=where.getSharedPreferences("stampsshared",Context.MODE_PRIVATE);
        // Only to a device paired here and not one of the owner's: one only named in a list is never reached by this.
        java.util.Set<String> reach=new java.util.HashSet<>();
        for(NoteStore.Contact one:store.addresses())if(!one.mine&&one.agreement.length>0)reach.add(one.address);
        // Nobody paired here but the owner's own devices: nothing is owed to anybody, whatever is stamped.
        if(reach.isEmpty()){OWED_KEPT=new Kept<>(store,key,List.of());return owed;}
        // Who each file on its own is owed to, asked once for all of them, not once a file.
        java.util.Map<String,java.util.Set<String>> sleevesTo=null;
        // And who has each note, the same way.
        java.util.Map<String,Boolean> carriers=store.stampCarriers();
        List<String> notes=new ArrayList<>();
        for(java.util.Map.Entry<String,Boolean> one:carriers.entrySet())if(!one.getValue())notes.add(one.getKey());
        java.util.Map<String,java.util.Set<String>> notesTo=store.everybodyInNotes(notes);
        for(java.util.Map.Entry<String,Boolean> one:carriers.entrySet()) {
            String thing=one.getKey();boolean sleeve=one.getValue();
            java.util.Set<String> to=new java.util.LinkedHashSet<>();
            if(sleeve) {
                if(sleevesTo==null){sleevesTo=new java.util.HashMap<>();
                    for(Outbox.Wait wait:store.sleevesOwed(null,true))if(!wait.mine)sleevesTo.computeIfAbsent(wait.page,any->new java.util.LinkedHashSet<>()).add(wait.address);}
                java.util.Set<String> its=sleevesTo.get(thing);
                if(its!=null)to.addAll(its);
            }
            // Whether this device only reads it is asked only of a note that goes to somebody else at all.
            else if(notesTo.containsKey(thing)){to.addAll(notesTo.get(thing));to.retainAll(reach);if(!to.isEmpty()&&store.onlyReads(thing))to.clear();}
            to.retainAll(reach);
            if(to.isEmpty())continue;
            String said=StampCard.said(sleeve?store.sleeve(thing,"").stamps
                :store.stampsTravelling(stampedWith(thing,store.enclosed(thing))));
            for(String address:to)if(!said.equals(went.getString(address+"\n"+thing,"")))owed.add(new StampsOwed(thing,sleeve,address));
        }
        OWED_KEPT=new Kept<>(store,key,List.copyOf(owed));
        return owed;
    }

    /**
     * The things owed again for their stamps, sent: each to the one device it is owed to, whole, at its revision. Blocking;
     * never the reason a node is started. What went is written down as it goes (see {@link #stampsWent}); what did not goes
     * at the next round.
     */
    static void sendStampsShared(Context where,NoteStore store,Keys keys) {
        if(!Node.running())return;
        int went=0,missed=0;
        for(StampsOwed one:stampsOwed(where,store)) {
            try {
                Done done=one.sleeve()?sleeves(where,store,keys,one.thing(),one.address(),true)
                    :send(where,store,keys,NoteStore.Branch.Kind.PAGE,one.thing(),one.address(),true);
                went+=done.sent;missed+=done.failed;
            } catch(Exception notNow){missed++;}
        }
        if(went+missed>0)stamps("stamps of what is shared: "+went+" sent again, "+missed+" not");
    }

    // ---- a person's Parlons! address, to the devices of the owner's they choose: see Parlons (decision 101) -----------

    /** When each device was last told this run that this build reads Parlons! addresses, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_PARLONS=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads Parlons! addresses, kept across restarts. Whether it is the first time. */
    private static boolean parlonsHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("parlons",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("parlons",all).apply();
        return true;
    }

    /** Whether a device's build has said it reads Parlons! addresses ({@link Receipt#PARLONS}). A card is never sealed for one that has not. */
    static boolean readsParlons(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("parlons",new java.util.HashSet<String>()).contains(who);
    }

    /** Whether a Parlons! card can go to this device of the owner's at all, chosen or not: see {@link Parlons#goesTo}. */
    static boolean takesParlons(Context where,NoteStore store,NoteStore.Contact them) {
        return them.paired()&&sendsTo(store,them)&&Parlons.goesTo(true,them.mine,readsParlons(where,them),mineThere(where,fingerprint(them)));
    }

    /**
     * Who a Parlons! card goes to: of the devices the owner chose ({@code chosen}, by address), those paired here and marked
     * theirs, whose build has said it reads them and that count this one as their owner's too. A build from before and
     * anybody else's device are never among them.
     */
    static List<NoteStore.Contact> parlonsGoTo(Context where,NoteStore store,java.util.Collection<String> chosen) {
        List<NoteStore.Contact> own=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses())
            if(chosen!=null&&chosen.contains(them.address)&&takesParlons(where,store,them))own.add(them);
        return own;
    }

    /** "This build reads Parlons! addresses", said to each of the owner's own devices, as {@link #tellGroups} says its own. */
    private static void tellParlons(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.mine||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsParlons(where,them)))continue;
            long[] told=told(where,TOLD_PARLONS,"parlons",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellParlons(where,store,keys,them);
        }
    }

    private static void tellParlons(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_PARLONS.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.PARLONS),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"parlons",who,"1");}
            contacts("said this build reads Parlons! addresses"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * A person's Parlons! address, as it is here now (set or removed), sent to the devices of the owner's they chose, by
     * address: each that takes it is handed it, once, now. Blocking, and started by the owner, so the node is started for
     * it. What the owner is told, by the names of their devices: where it went, which could not be reached, and which
     * needs an update first. {@code here} is this device, as the sentence says it.
     */
    static String sendParlons(Context where,NoteStore store,Keys keys,String address,java.util.Collection<String> chosen,String here) {
        Parlons.Card card=store.parlonsCard(address);
        boolean removed=card==null||card.address.isEmpty();
        List<String> sent=new ArrayList<>(),notReached=new ArrayList<>(),older=new ArrayList<>();
        if(card==null||chosen==null||chosen.isEmpty())return Parlons.said(removed,here,sent,notReached,older);
        List<NoteStore.Contact> to=parlonsGoTo(where,store,chosen);
        java.util.Set<String> going=new java.util.HashSet<>();for(NoteStore.Contact them:to)going.add(them.address);
        for(NoteStore.Contact them:store.addresses())if(chosen.contains(them.address)&&them.mine&&them.paired()&&!going.contains(them.address))older.add(them.name);
        if(to.isEmpty())return Parlons.said(removed,here,sent,notReached,older);
        byte[] plain=Parlons.wrap(card);
        // Not started: every device is owed it, and it goes by itself (see Later).
        MaximaNode node;try{node=Node.node(where);}catch(Exception notNow){node=null;}
        for(NoteStore.Contact them:to) {
            boolean went=false;
            // One device out of reach is not a card kept from the rest.
            if(node!=null)try{went=hand(where,store,keys,node,them,Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)));}
                catch(Exception noWay){went=false;}
            (went?sent:notReached).add(them.name);
            owed(where,Later.Kind.PARLONS,address,them.address,!went);
        }
        contacts("a Parlons! card"+(removed?" (removed)":"")+" handed to "+sent.size()+" of the owner's devices, "+notReached.size()+" out of reach, "+older.size()+" from before");
        return Parlons.said(removed,here,sent,notReached,older);
    }

    /**
     * A Parlons! card from another device of the owner's: kept on the person it is about where it was decided later than
     * what is here. From a device not marked as yours here, about somebody not known here, or older: nothing changes.
     */
    private static Landed parlonsArrived(Context where,NoteStore store,NoteStore.Contact from,Envelope.Opened opened) {
        Parlons.Card card=Parlons.open(opened.text);
        if(card==null){contacts("a Parlons! card that could not be read");return new Landed(null,null);}
        // Somebody's own address, from them (decision 147): kept on their card, the later decision standing. From one of the
        // owner's own devices, it is the owner's own address too.
        if(store.aboutSender(from,card)) {
            ownParlonsHeard(where,fingerprint(from));
            NoteStore.ParlonsTaken taken=store.takeParlons(card);
            boolean mine=from.mine&&store.takeMyParlons(card);
            contacts("their own Parlons! address "+(taken==NoteStore.ParlonsTaken.TAKEN?"taken in":"older than what is here: nothing changes")+(mine?", and kept as yours":""));
            return taken==NoteStore.ParlonsTaken.TAKEN||mine?Landed.contacts():new Landed(null,null);
        }
        if(!from.mine){contacts("a Parlons! card about somebody else, from a device that is not the owner's: nothing changes");return new Landed(null,null);}
        // It sent one, so it reads them.
        parlonsHeard(where,fingerprint(from));
        NoteStore.ParlonsTaken taken=store.takeParlons(card);
        contacts("a Parlons! card "+(taken==NoteStore.ParlonsTaken.TAKEN?"taken in":taken==NoteStore.ParlonsTaken.OLDER?"older than what is here: nothing changes"
            :"about somebody not known here: dropped"));
        return taken==NoteStore.ParlonsTaken.TAKEN?Landed.contacts():new Landed(null,null);
    }

    // ---- what did not reach one of my devices, sent again by itself: see Later (2026-11-07) -------------------------

    private static final java.util.concurrent.atomic.AtomicLong LATER_TRIED=new java.util.concurrent.atomic.AtomicLong();

    /**
     * A send that failed before any device was tried: each of those chosen owed it, so it goes by itself. What the owner is
     * then told, kept or removed alike.
     */
    static String owedAll(Context where,Later.Kind kind,String about,java.util.Collection<String> devices,String here) {
        if(devices!=null)for(String one:devices)owed(where,kind,about,one,true);
        return Say.t("It goes to your devices by itself once they are reached.");
    }
    /** That device is owed it now, or has it: kept in the post settings either way. */
    private static void owed(Context where,Later.Kind kind,String about,String device,boolean owed) {
        if(where==null||device==null||device.isEmpty())return;
        synchronized(LATER_TRIED) {
            android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            java.util.Set<String> was=kept.getStringSet("later",new java.util.HashSet<String>());
            java.util.Set<String> now=owed?Later.add(was,new Later.Owed(kind,about,device,System.currentTimeMillis())):Later.remove(was,kind,about,device);
            if(!now.equals(was))kept.edit().putStringSet("later",new java.util.HashSet<>(now)).apply();
        }
    }

    /** Each thing owed, sent to its device as it is now; at most every two minutes, and a month at most. Blocking. */
    private static void sendLater(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis(),was=LATER_TRIED.get();
        if(now-was<Later.EVERY||!LATER_TRIED.compareAndSet(was,now))return;
        java.util.Set<String> lines;
        synchronized(LATER_TRIED) {
            android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            lines=kept.getStringSet("later",new java.util.HashSet<String>());
            java.util.Set<String> still=Later.kept(lines,now);
            if(!still.equals(lines))kept.edit().putStringSet("later",new java.util.HashSet<>(still)).apply();
            lines=still;
        }
        for(Later.Owed one:Later.owed(lines,now)) {
            NoteStore.Contact them=null;
            for(NoteStore.Contact c:store.addresses())if(c.address.equals(one.device))them=c;
            // A device taken off, or no longer mine: nothing to send it.
            if(them==null||!them.mine||!them.paired()){owed(where,one.kind,one.about,one.device,false);continue;}
            if(resting(fingerprint(them)))continue;
            java.util.Set<String> to=java.util.Collections.singleton(one.device);
            try {
                switch(one.kind) {
                    case PARLONS: sendParlons(where,store,keys,one.about,to,here());break;
                    case CALLED: sendCalled(where,store,keys,one.about.isEmpty()?null:one.about,to,here());break;
                    default: sendKey(where,store,keys,one.about,to);
                }
            } catch(Exception notNow){/* tried again in a while */}
            contacts("something owed to one of the owner's devices sent again ("+one.kind.name().toLowerCase(java.util.Locale.ROOT)+")");
        }
    }

    // ---- the name we pick for a person, to the owner's devices: see Called (decision 124) ---------------------------

    /** When each device was last told this run that this build reads those names, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_CALLED=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads them, kept across restarts. Whether it is the first time. */
    private static boolean calledHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("called",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("called",all).apply();
        return true;
    }

    /** Whether a device's build has said it reads the name we pick ({@link Receipt#CALLED}). A card is never sealed for one that has not. */
    static boolean readsCalled(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("called",new java.util.HashSet<String>()).contains(who);
    }

    /** Whether one of these cards can go to this device of the owner's at all. */
    static boolean takesCalled(Context where,NoteStore store,NoteStore.Contact them) {
        return them.paired()&&sendsTo(store,them)&&Called.goesTo(true,them.mine,readsCalled(where,them),mineThere(where,fingerprint(them)));
    }

    static List<NoteStore.Contact> calledGoTo(Context where,NoteStore store,java.util.Collection<String> chosen) {
        List<NoteStore.Contact> own=new ArrayList<>();
        for(NoteStore.Contact them:store.addresses())
            if(chosen!=null&&chosen.contains(them.address)&&takesCalled(where,store,them))own.add(them);
        return own;
    }

    private static void tellCalled(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.mine||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsCalled(where,them)))continue;
            long[] told=told(where,TOLD_CALLED,"called",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellCalled(where,store,keys,them);
        }
    }

    private static void tellCalled(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_CALLED.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.CALLED),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"called",who,"1");}
            contacts("said this build reads the name we pick"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * The name we pick, as it is here now, sent to the devices of the owner's they chose. {@code about} is the person.
     * Null is this device itself, whose name lives in settings rather than on a contact. Blocking, and started by the
     * owner, so the node is started for it.
     */
    static String sendCalled(Context where,NoteStore store,Keys keys,String about,java.util.Collection<String> chosen,String here) {
        Called.Card card=about==null?cardForUs(where,store,keys):store.calledCard(about);
        List<String> sent=new ArrayList<>(),notReached=new ArrayList<>(),older=new ArrayList<>();
        if(card==null||chosen==null||chosen.isEmpty())return Called.said(here,sent,notReached,older);
        List<NoteStore.Contact> to=calledGoTo(where,store,chosen);
        java.util.Set<String> going=new java.util.HashSet<>();for(NoteStore.Contact them:to)going.add(them.address);
        for(NoteStore.Contact them:store.addresses())if(chosen.contains(them.address)&&them.mine&&them.paired()&&!going.contains(them.address))older.add(them.name);
        if(to.isEmpty())return Called.said(here,sent,notReached,older);
        byte[] plain=Called.wrap(card);
        MaximaNode node;try{node=Node.node(where);}catch(Exception notNow){node=null;}
        for(NoteStore.Contact them:to) {
            boolean went=false;
            if(node!=null)try{went=hand(where,store,keys,node,them,Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)));}
                catch(Exception noWay){went=false;}
            (went?sent:notReached).add(them.name);
            owed(where,Later.Kind.CALLED,about,them.address,!went);
        }
        contacts("a name we pick handed to "+sent.size()+" of the owner's devices, "+notReached.size()+" out of reach, "+older.size()+" from before");
        return Called.said(here,sent,notReached,older);
    }

    /** This device's own picked name, for a card about itself. Null until one has been decided. */
    private static Called.Card cardForUs(Context where,NoteStore store,Keys keys) {
        android.content.SharedPreferences kept=where.getSharedPreferences("settings",Context.MODE_PRIVATE);
        long at=kept.getLong(Called.PREF_AT,0);
        if(at<=0)return null;
        String key;
        try{key=store.keyOf(keys.signing().getPublic().getEncoded());}
        catch(Exception unreadable){return null;}
        if(key.isEmpty())return null;
        return new Called.Card(key,kept.getString(Called.PREF_PICKED,""),kept.getInt(Called.PREF_SHOW,0)==1,at);
    }

    /**
     * A card from another device of the owner's. Kept on the person it is about where it is later, and on this device's
     * own settings where the card is about this device and this device has not kept a name of its own.
     */
    private static Landed calledArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        if(!from.mine){contacts("a name from a device that is not the owner's: nothing changes");return new Landed(null,null);}
        Called.Card card=Called.open(opened.text);
        if(card==null){contacts("a name that could not be read");return new Landed(null,null);}
        calledHeard(where,fingerprint(from));
        boolean ours=false;
        String mine="";
        try{mine=store.keyOf(keys.signing().getPublic().getEncoded());}
        catch(Exception unreadable){mine="";}
        if(mine.equals(card.key)) {
            android.content.SharedPreferences kept=where.getSharedPreferences("settings",Context.MODE_PRIVATE);
            boolean onlyHere=kept.getInt(Called.PREF_HERE,0)==1&&kept.getLong(Called.PREF_AT,0)>0;
            if(!onlyHere&&Called.later(card.decided,kept.getLong(Called.PREF_AT,0))) {
                kept.edit().putString(Called.PREF_PICKED,card.picked).putInt(Called.PREF_SHOW,card.showPicked?1:0)
                    .putLong(Called.PREF_AT,card.decided).apply();
                ours=true;
            }
        }
        NoteStore.CalledTaken taken=store.takeCalled(card);
        contacts("a name we pick "+(taken==NoteStore.CalledTaken.TAKEN?"taken in":taken==NoteStore.CalledTaken.KEPT_HERE?"left, this device keeps its own"
            :taken==NoteStore.CalledTaken.OLDER?"older than what is here: nothing changes":"about somebody not known here: dropped")+(ours?"; this device's own name too":""));
        return taken==NoteStore.CalledTaken.TAKEN||ours?Landed.contacts():new Landed(null,null);
    }

    // ---- your own Parlons! address, to everybody paired (decision 147) ---------------------------------------------------

    /** When each device was last told this run that this build takes a person's own address, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_OWN=new java.util.concurrent.ConcurrentHashMap<>();
    /** Which decision of yours each device was handed this run, by fingerprint. */
    private static final java.util.Map<String,Long> SENT_OWN=new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean ownParlonsHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("parlonsOwn",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("parlonsOwn",all).apply();
        return true;
    }

    /** Whether a device's build has said it takes a person's own Parlons! address ({@link Receipt#PARLONS_OWN}). */
    static boolean readsOwnParlons(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("parlonsOwn",new java.util.HashSet<String>()).contains(who);
    }

    /** Said to every paired device, again every while until it says it back, a minute on where it could not be handed. */
    private static void tellOwnParlons(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=told(where,TOLD_OWN,"parlonsOwn",who);
            if(told!=null&&(told[1]==1&&readsOwnParlons(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("parlonsOwn",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("parlonsOwn",who);
            tellOwnParlons(where,store,keys,them);
        }
    }

    private static void tellOwnParlons(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_OWN.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.PARLONS_OWN),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"parlonsOwn",who,"1");}
            contacts("said this build takes your own Parlons! address"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again a minute on */}
    }

    /**
     * Your own Parlons! address, or that you removed it, handed to every paired device whose build takes it, once for each
     * time you decide it ({@code again}: to all of them now, as on saving it). How many it was handed to. Blocking.
     */
    static int sendMyParlons(Context where,NoteStore store,Keys keys,boolean again) {
        Parlons.Card card=store.myParlonsCard();
        if(card.decided<=0||card.key.isEmpty())return 0;
        MaximaNode node;
        try{node=Node.node(where);}catch(Exception notNow){return 0;}
        if(node==null)return 0;
        byte[] plain=Parlons.wrap(card);int handed=0;
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||them.agreement.length==0||!sendsTo(store,them)||!readsOwnParlons(where,them)||resting(who))continue;
            Long sent=SENT_OWN.get(who);
            // Handed within the day, at a restart (decision 225): not again unless it changed.
            if(sent==null){String kept=saidLately(where,"cardOwn",who);if(kept!=null)try{sent=Long.valueOf(Said.value(kept));}catch(NumberFormatException notOne){sent=null;}}
            if(!again&&sent!=null&&sent==card.decided)continue;
            try {
                if(handSaying(where,store,keys,node,them,Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)))) {
                    SENT_OWN.put(who,card.decided);said(where,"cardOwn",who,String.valueOf(card.decided));handed++;
                }
            } catch(Exception noWay){/* tried again at the next turn */}
        }
        if(handed>0)contacts("your own Parlons! address handed to "+handed+" device(s)");
        return handed;
    }

    // ---- your own picture, to everybody paired (decision 158) ---------------------------------------------------------------

    private static final java.util.Map<String,long[]> TOLD_FACE=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String,Long> SENT_FACE=new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean ownFaceHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("faceOwn",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("faceOwn",all).apply();
        return true;
    }

    /** Whether a device's build has said it takes a person's own picture ({@link Receipt#FACE_OWN}). */
    static boolean readsOwnFace(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("faceOwn",new java.util.HashSet<String>()).contains(who);
    }

    /** Said to every paired device, again every while until it says it back, a minute on where it could not be handed. */
    private static void tellOwnFace(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=told(where,TOLD_FACE,"faceOwn",who);
            if(told!=null&&(told[1]==1&&readsOwnFace(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("faceOwn",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("faceOwn",who);
            tellOwnFace(where,store,keys,them);
        }
    }

    private static void tellOwnFace(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_FACE.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.FACE_OWN),keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"faceOwn",who,"1");}
            contacts("said this build takes your own picture"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again a minute on */}
    }

    /** Your own picture, or that you took it off, handed to every paired device whose build takes it, once each time it is decided. */
    static int sendMyFace(Context where,NoteStore store,Keys keys,boolean again) {
        Face.Card card=store.myFaceCard();
        if(card.decided<=0||card.key.isEmpty())return 0;
        MaximaNode node;
        try{node=Node.node(where);}catch(Exception notNow){return 0;}
        if(node==null)return 0;
        byte[] plain;try{plain=Face.wrap(card);}catch(IllegalArgumentException notOne){return 0;}
        int handed=0;
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||them.agreement.length==0||!sendsTo(store,them)||!readsOwnFace(where,them)||resting(who))continue;
            Long sent=SENT_FACE.get(who);
            // Handed within the day, at a restart (decision 225): not again unless it changed.
            if(sent==null){String kept=saidLately(where,"cardFace",who);if(kept!=null)try{sent=Long.valueOf(Said.value(kept));}catch(NumberFormatException notOne){sent=null;}}
            if(!again&&sent!=null&&sent==card.decided)continue;
            try {
                if(handSaying(where,store,keys,node,them,Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)))) {
                    SENT_FACE.put(who,card.decided);said(where,"cardFace",who,String.valueOf(card.decided));handed++;
                }
            } catch(Exception noWay){/* tried again at the next turn */}
        }
        if(handed>0)contacts("your own picture handed to "+handed+" device(s)");
        return handed;
    }

    /** A person's own picture, from them: kept on their rows where later; from one of your own devices, yours too. */
    private static Landed faceArrived(Context where,NoteStore store,NoteStore.Contact from,Envelope.Opened opened) {
        Face.Card card=Face.open(opened.text);
        if(card==null){contacts("a picture card that could not be read");return new Landed(null,null);}
        if(!store.aboutSender(from,card)){contacts("a picture card about somebody else: nothing changes");return new Landed(null,null);}
        ownFaceHeard(where,fingerprint(from));
        boolean taken=store.takeFace(card);
        boolean mine=from.mine&&store.takeMyFace(card);
        contacts("their own picture "+(taken?"taken in":"older than what is here: nothing changes")+(mine?", and kept as yours":""));
        return taken||mine?Landed.contacts():new Landed(null,null);
    }

    // ---- "not here" on a list, to everybody paired (decision 293) --------------------------------------------------------------

    private static final java.util.Map<String,long[]> TOLD_NOT_HERE=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads "not here" ({@link Receipt#NOT_HERE}), kept. Whether it is the first time. */
    static boolean notHereHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("notHere",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("notHere",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads "not here" on a list: only then is such an entry on a list sealed for it (see
     * NoteStore#travelling). Nobody, where nothing is known: a build from before is the one that has said nothing.
     */
    static boolean readsNotHere(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("notHere",new java.util.HashSet<String>()).contains(who);
    }

    /** Said to every paired device, again every while until it says it back, a minute on where it could not be handed. */
    private static void tellNotHere(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=told(where,TOLD_NOT_HERE,"notHere",who);
            if(told!=null&&(told[1]==1&&readsNotHere(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("notHere",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("notHere",who);
            tellNotHere(where,store,keys,them);
        }
    }

    private static void tellNotHere(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_NOT_HERE.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.NOT_HERE),keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"notHere",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build reads \"not here\""+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again a minute on */}
    }

    // ---- only what changed in a note, to everybody paired (decision 320) --------------------------------------------------------

    private static final java.util.Map<String,long[]> TOLD_CHANGES=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads only what changed ({@link Receipt#CHANGES}), kept. Whether it is the first time. */
    static boolean changesHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("changes",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("changes",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads only what changed in a note: only then is a change sealed for it (see
     * {@link #changeFor}). Nobody, where nothing is known: a build from before is the one that has said nothing, and gets
     * every note whole.
     */
    static boolean readsChanges(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("changes",new java.util.HashSet<String>()).contains(who);
    }

    /** Said to every paired device, again every while until it says it back, a minute on where it could not be handed. */
    private static void tellChanges(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=told(where,TOLD_CHANGES,"changes",who);
            if(told!=null&&(told[1]==1&&readsChanges(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("changes",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("changes",who);
            tellChanges(where,store,keys,them);
        }
    }

    private static void tellChanges(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_CHANGES.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.CHANGES),keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"changes",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build reads only what changed"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again a minute on */}
    }

    // ---- who a device belongs to, to everybody paired (decision 163) ----------------------------------------------------------

    private static final java.util.Map<String,long[]> TOLD_ABOUT=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String,String> SENT_ABOUT=new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean aboutHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("aboutMe",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("aboutMe",all).apply();
        return true;
    }

    /** Whether a device's build has said it reads who a device belongs to ({@link Receipt#ABOUT_ME}). */
    static boolean readsAboutMe(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE).getStringSet("aboutMe",new java.util.HashSet<String>()).contains(who);
    }

    /** Said to every paired device, again every while until it says it back, a minute on where it could not be handed. */
    private static void tellAboutMe(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=told(where,TOLD_ABOUT,"aboutMe",who);
            if(told!=null&&(told[1]==1&&readsAboutMe(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("aboutMe",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("aboutMe",who);
            tellAboutMe(where,store,keys,them);
        }
    }

    private static void tellAboutMe(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_ABOUT.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.ABOUT_ME),keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"aboutMe",who,"1");}
            contacts("said this build reads who a device belongs to"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again a minute on */}
    }

    /**
     * Who this device belongs to, handed to every paired device whose build reads it, once each time it changes (the person
     * id, when two of the owner's devices bond and agree on one). How many it was handed to. Blocking.
     */
    static int sendAboutMe(Context where,NoteStore store,Keys keys,boolean again) {
        Persons.Me me=me(where);
        if(!Persons.isId(me.id))return 0;
        String key=store.myKeyCanonical();
        if(key.isEmpty())return 0;
        MaximaNode node;
        try{node=Node.node(where);}catch(Exception notNow){return 0;}
        if(node==null)return 0;
        byte[] plain;try{plain=AboutMe.wrap(new AboutMe.Card(key,me.id,me.made));}catch(IllegalArgumentException notOne){return 0;}
        int handed=0;
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||them.agreement.length==0||!sendsTo(store,them)||!readsAboutMe(where,them)||resting(who))continue;
            // Handed within the day, at a restart (decision 225): not again unless it changed.
            String kept=SENT_ABOUT.containsKey(who)?null:saidLately(where,"cardAbout",who);
            if(!again&&(me.id.equals(SENT_ABOUT.get(who))||kept!=null&&me.id.equals(Said.value(kept))))continue;
            try {
                if(handSaying(where,store,keys,node,them,Envelope.seal(new byte[16],me.made,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)))) {
                    SENT_ABOUT.put(who,me.id);said(where,"cardAbout",who,me.id);handed++;
                }
            } catch(Exception noWay){/* tried again at the next turn */}
        }
        if(handed>0)contacts("who this device belongs to handed to "+handed+" device(s)");
        return handed;
    }

    /**
     * One of the owner's keys, set here, handed to the owner's own devices chosen (decision 210): said by the names of those it
     * reached and those it did not. Blocking.
     */
    /**
     * The owner's devices a key goes to, remembered on this device, so the switches say what was chosen and a key set
     * anywhere here goes to them (the owner, 2026-10-19: "I filled Integritas on the laptop and specified that I wanted that
     * reflected on both phones, but on the Pro it is not there"; decision 223). Addresses, as the switches name them.
     */
    static java.util.Set<String> keyGoesTo(Context where,String name) {
        return new java.util.HashSet<>(where.getSharedPreferences("settings",Context.MODE_PRIVATE).getStringSet("keyTo."+name,new java.util.HashSet<String>()));
    }
    static void keyGoesTo(Context where,String name,java.util.Set<String> to) {
        where.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putStringSet("keyTo."+name,new java.util.HashSet<>(to)).apply();
    }
    /** The key as it is here, to the devices remembered for it: after it is set, wherever it was set. Blocking. */
    static String sendKeyToChosen(Context where,NoteStore store,Keys keys,String name){return sendKey(where,store,keys,name,keyGoesTo(where,name));}

    static String sendKey(Context where,NoteStore store,Keys keys,String name,java.util.Collection<String> chosen) {
        if(chosen==null||chosen.isEmpty()){contacts("a key not handed: no device chosen for it");return "";}
        String value=store.secret(name);long decided=decidedAt(store,name);
        byte[] plain=KeyCard.wrap(new KeyCard.Card(name,value,decided));
        List<String> sent=new ArrayList<>(),notReached=new ArrayList<>();
        MaximaNode node;try{node=Node.node(where);}catch(Exception notNow){node=null;}
        int later=0;
        for(NoteStore.Contact them:store.addresses()) {
            if(!chosen.contains(them.address)||!them.mine||!them.paired()||them.agreement.length==0)continue;
            // A build that has not said it reads a key card would keep these bytes as a note (decision 252): not sealed for
            // it, and said as not reached; it is sent the key once it says so (see Receipt#STAMPS).
            if(!takesKey(where,them)){notReached.add(them.name);later++;continue;}
            boolean went=false;
            if(node!=null)try{went=hand(where,store,keys,node,them,Envelope.seal(new byte[16],decided,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)));}
                catch(Exception noWay){went=false;}
            (went?sent:notReached).add(them.name);
            owed(where,Later.Kind.KEY,name,them.address,!went);
        }
        contacts("a key handed to "+sent.size()+" of the owner's devices, "+(notReached.size()-later)+" out of reach"
            +(later>0?", "+later+" waiting for a build that reads it":""));
        String said=sent.isEmpty()?"":Say.t("Sent to {0}",String.join(", ",sent));
        if(!notReached.isEmpty())said=(said.isEmpty()?"":said+". ")+Say.t("It goes to {0} by itself once Mininotes is open there.",String.join(", ",notReached));
        return said;
    }

    /**
     * Whether a key card may be sealed for this device: one of the owner's, paired, whose build has said it reads stamps
     * ({@link Receipt#STAMPS}), the first number said only by a build that also reads a key card (decision 252). Before, a
     * key card went to any device of the owner's chosen, and a build older than 0.4.006 kept it as a note.
     */
    static boolean takesKey(Context where,NoteStore.Contact them) {
        return them!=null&&them.mine&&them.paired()&&them.agreement.length>0&&readsStamps(where,them);
    }

    /**
     * The relays and the public switch chosen here, on a key card (see KeyCard), to each device of the owner's that reads one and
     * has not had this choice yet: sent again at the next round where it was not taken, so a device that was off has it once it is
     * on. Nothing where the switch is off here, or nothing was ever chosen (decision 316).
     */
    private static void sendRelays(Context where,NoteStore store,Keys keys) {
        long at=Node.relaysAt(where);
        if(at<=0L||!Node.relaysSame(where)||!Node.running())return;
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] plain=KeyCard.wrap(new KeyCard.Card(Relays.CARD,Relays.toCard(Node.publicRelays(where),Node.ownRelays(where)),at));
            android.content.SharedPreferences sent=where.getSharedPreferences("post",Context.MODE_PRIVATE);
            for(NoteStore.Contact them:store.addresses()) {
                if(!takesKey(where,them))continue;
                String who=fingerprint(them);
                if(who.isEmpty()||sent.getLong("relays."+who,0L)>=at||resting(who))continue;
                boolean went;
                try{went=hand(where,store,keys,node,them,Envelope.seal(new byte[16],at,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement)));}
                catch(Exception noWay){went=false;}
                if(went)sent.edit().putLong("relays."+who,at).apply();
                contacts("the relays chosen here "+(went?"handed to":"not taken for")+" one of the owner's devices");
            }
        } catch(Exception notNow){contacts("could not send the relays: "+Unsent.reason(notNow));}
    }

    /** When a key was set here, kept beside it, so an older one arriving changes nothing. */
    static long decidedAt(NoteStore store,String name){try{return Long.parseLong(store.secret(name+".at"));}catch(NumberFormatException none){return 0L;}}

    /** A key from one of the owner's own paired devices: kept where it was set later than the one here; from any other, nothing. */
    private static Landed keyArrived(Context where,NoteStore store,NoteStore.Contact from,Envelope.Opened opened) {
        KeyCard.Card card=KeyCard.open(opened.text);
        if(card==null){contacts("a key card that could not be read");return new Landed(null,null);}
        if(from==null||!from.mine||!from.paired()){contacts("a key card from a device that is not the owner's: nothing changes");return new Landed(null,null);}
        if(Relays.CARD.equals(card.name)) {
            if(!Node.relaysSame(where)){contacts("relays from one of the owner's devices: not taken, switched off here");return new Landed(null,null);}
            if(card.decided<=Node.relaysAt(where)){contacts("relays from one of the owner's devices, older than what is here: nothing changes");return new Landed(null,null);}
            List<String> own=Relays.cardRelays(card.value);
            Node.relaysFromMine(where,Relays.cardPublic(card.value),own,card.decided);
            contacts("relays from one of the owner's devices: taken, "+own.size()+" of theirs, the public ones "+(Relays.cardPublic(card.value)?"on":"off"));
            return Landed.contacts();
        }
        if(card.decided<=decidedAt(store,card.name)){contacts("a key card older than what is here: nothing changes");return new Landed(null,null);}
        store.setSecret(card.name,card.value);store.setSecret(card.name+".at",String.valueOf(card.decided));
        contacts("a key card from one of the owner's devices: kept");
        return Landed.contacts();
    }

    /** Who a device belongs to, from that device: kept, so its rounds fold into one person's. */
    private static Landed aboutArrived(Context where,NoteStore store,NoteStore.Contact from,Envelope.Opened opened) {
        AboutMe.Card card=AboutMe.open(opened.text);
        if(card==null){contacts("a card about whose a device is that could not be read");return new Landed(null,null);}
        if(!store.aboutSender(from,card)){contacts("a card about somebody else's device: nothing changes");return new Landed(null,null);}
        aboutHeard(where,fingerprint(from));
        boolean changed=store.belongsTo(from.address,card.person);
        contacts("whose a device is: "+(changed?"kept":"as it was"));
        return changed?Landed.contacts():new Landed(null,null);
    }

    // ---- brief messages, for the Inbox: see Message (decision 145) -----------------------------------------------------

    /** When each device was last told this run that this build reads a message, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_MESSAGES=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads a message, kept across restarts. Whether it is the first time. */
    private static boolean messagesHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("messages",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("messages",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads a message (see {@link Receipt#MESSAGES}). A message is never sealed for one
     * that has not: a build from before would write it over a note. The Inbox says "needs Mininotes updated" for it instead.
     */
    static boolean readsMessages(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("messages",new java.util.HashSet<String>()).contains(who);
    }

    /** "This build reads a message", said to every paired device that has shown it knows what an answer is, as help is. */
    private static void tellMessages(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them))continue;
            long[] told=TOLD_MESSAGES.get(who);
            // Said again every while until they say it back: one said to a build from before, updated since, or one that
            // went astray, left both ends saying the other needed updating (Graphene and Pro, 2026-10-10).
            // One that could not be handed at all (the node still starting, say) is tried again a minute on, not ten.
            if(told!=null&&(told[1]==1&&readsMessages(where,them)||now>=told[0]&&now-told[0]<(told[1]==1?tellAgain("messages",who):60_000L)))continue;
            if(resting(who))continue;
            if(told!=null&&told[1]==1)retold("messages",who);
            tellMessages(where,store,keys,them);
        }
    }

    private static void tellMessages(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_MESSAGES.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.MESSAGES),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed))told[1]=1;
            android.util.Log.i("Mininotes/Post","said this build reads a message"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * What sending a message did: kept here and handed on; kept here to go when they can be reached; kept here to go once
     * their Mininotes has said it reads messages, which it is asked now; or not at all, as they are not paired.
     */
    enum Sent {HANDED,KEPT_TO_GO,WAITS_FOR_THEM,NOT_PAIRED}

    /**
     * Whether their Mininotes reads messages, asked now rather than at its turn: "this build reads a message" said to them
     * at once, which they say back. A conversation with somebody not heard to read them asks this as it opens.
     */
    static void askReadsMessages(Context where,NoteStore store,Keys keys,String address) {
        NoteStore.Contact them=store.address(address);
        if(them==null||!them.paired()||them.agreement.length==0||readsMessages(where,them))return;
        tellMessages(where,store,keys,them);
    }

    /**
     * A message written here: kept first, so it is never lost, then handed on to them if they can be reached. One not handed
     * on now goes again with everything else not answered (see {@link #messagesAgain}). Blocking: the worker calls it.
     */
    static Sent sendMessage(Context where,NoteStore store,Keys keys,String address,int kind,String words,String parlons) {
        NoteStore.Contact them=store.address(address);
        if(them==null||!them.paired()||them.agreement.length==0)return Sent.NOT_PAIRED;
        byte[] id=Message.newId();long at=System.currentTimeMillis();
        Message.Card card=new Message.Card(id,kind,words,parlons,at);
        byte[] plain=Message.wrap(card);
        store.keepMessage(Message.hex(id),address,true,kind,card.words.trim(),card.kind==Message.PARLONS?card.parlons.trim():"",at);
        // Never sealed for a build that has not said it reads one (it would write it over a note): kept, they are asked, and
        // it goes the moment they say so (see messagesAgain).
        if(!readsMessages(where,them)){tellMessages(where,store,keys,them);return Sent.WAITS_FOR_THEM;}
        return handMessage(where,store,keys,them,id,plain)?Sent.HANDED:Sent.KEPT_TO_GO;
    }

    private static boolean handMessage(Context where,NoteStore store,Keys keys,NoteStore.Contact them,byte[] id,byte[] plain) {
        store.messageHanded(Message.hex(id),System.currentTimeMillis());
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return false;
            byte[] sealed=Envelope.seal(id,0,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
            boolean went=hand(where,store,keys,node,them,sealed);
            android.util.Log.i("Mininotes/Post","a message"+(went?" handed on":": not taken, kept to go again"));
            return went;
        } catch(Exception notNow){return false;}
    }

    /** Every message its far end has not said it has, handed on again once its wait is over: 1, 2, 4, 8, then 15 minutes. */
    static void messagesAgain(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Said one:store.undeliveredMessages()) {
            long wait=Math.min(15*60_000L,60_000L<<Math.min(4,Math.max(0,one.tries-1)));
            if(one.tries>0&&now-one.handedAt<wait)continue;
            NoteStore.Contact them=store.address(one.address);
            if(them==null||!them.paired()||them.agreement.length==0||!readsMessages(where,them))continue;
            byte[] id=Message.unhex(one.id);
            if(id==null)continue;
            byte[] plain=Message.wrap(new Message.Card(id,one.kind,one.words,one.parlons,one.at));
            handMessage(where,store,keys,them,id,plain);
        }
    }

    /**
     * A message that arrived: kept first, then answered that it is kept, which is what stops its sender handing it on
     * again. Kept once by its id, so one that comes twice is answered twice and shown once. Handed up for the Inbox, and a
     * line in the phone's notifications where it is new. Never taken for a note: these bytes are read before a note is.
     */
    private static Landed messageArrived(Context where,NoteStore store,Keys keys,NoteStore.Contact from,Envelope.Opened opened) {
        // It sent one, so it reads them.
        messagesHeard(where,fingerprint(from));
        Message.Card card=Message.open(opened.text);
        if(card==null||!java.util.Arrays.equals(card.id,opened.page))return new Landed(null,null);
        // A big file offered straight from their device: never kept as a message (decision 327).
        if(card.kind==Message.BIG)return bigArrived(where,store,from,card);
        boolean fresh=store.keepMessage(Message.hex(card.id),from.address,false,card.kind,card.words,card.parlons,card.at);
        final NoteStore.Contact them=from;final byte[] id=card.id;
        ANSWERS.execute(()->{
            try {
                MaximaNode node=Node.node(where);
                if(node==null)return;
                byte[] sealed=Envelope.seal(id,0,System.currentTimeMillis(),Receipt.wrap(Receipt.MESSAGE_KEPT),keys.signing(),Keys.publicKey(them.agreement));
                hand(where,store,keys,node,them,sealed);
            } catch(Exception notNow){/* they hand it on again, and it is answered then */}
        });
        android.util.Log.i("Mininotes/Post","a message arrived"+(fresh?"":", one already kept"));
        if(!fresh)return Landed.message("","","");
        return Landed.message(from.name,Message.said(card.kind,card.words),from.address);
    }

    // ---- a big file, straight between two devices that reach each other: see Big (decision 327) ----------------------

    /**
     * A big file offered to one device: only where it can be reached directly now, at a door on this network or one it proved,
     * never a relay. The offer goes the usual way, sealed for them; the file stays where it is and is read when they save it.
     * Null where it was offered, or what to say where it was not. Blocking: the worker calls it.
     */
    static String offerBig(Context where,NoteStore store,Keys keys,String address,String name,String kind,long bytes,Big.Source source,Big.Heard heard) {
        NoteStore.Contact them=store.address(address);
        if(them==null||!them.paired()||them.agreement.length==0)return Say.t("Nobody added yet. Add someone, then files and messages can go straight to them.");
        if(bytes>Big.MOST)return Given.refusal(name,Big.TOO_BIG);
        if(!readsMessages(where,them)){tellMessages(where,store,keys,them);return Say.t("{0}'s Mininotes does not take big files yet. Once it is updated, try again.",them.name);}
        List<java.net.InetSocketAddress> doors=bigDoors(where,store,address);
        // Not reached: Big.NOT_NEAR itself, untranslated, so the caller can show the way to send it and try again (Big.steps).
        if(!reachable(doors))return Big.NOT_NEAR;
        Big.Offer offer=Big.Offer.of(name,kind,bytes);
        Big.serve(offer,source,heard);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)throw new IllegalStateException("The node is not running.");
            byte[] id=Message.newId();
            byte[] plain=Message.wrap(new Message.Card(id,Message.BIG,offer.said(),"",System.currentTimeMillis()));
            byte[] sealed=Envelope.seal(id,0,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
            if(!hand(where,store,keys,node,them,sealed)){Big.withdraw(offer.token);return Big.NOT_NEAR;}
            android.util.Log.i("Mininotes/Big","offered a big file, "+(bytes>>20)+" MB");
            return null;
        } catch(Exception notNow){Big.withdraw(offer.token);return Big.NOT_NEAR;}
    }

    /**
     * A file pushed to one of my devices (decision 330): a backup, brought to its door by this device, which a phone on mobile
     * data can do where it cannot be fetched from. The offer goes the usual way, sealed for it; then the file goes in pieces,
     * from where that device says it has it to. MADE once it said it has it whole; NO_DEVICE where it is not reached directly
     * (or no longer one of mine); NOT_TAKEN where it did not take it. Blocking, for as long as the file takes.
     */
    static Backups.Said pushBig(Context where,NoteStore store,Keys keys,String address,String name,String kind,long bytes,Big.Source source) {
        NoteStore.Contact them=store.address(address);
        if(them==null||!them.paired()||!them.mine||them.agreement.length==0||bytes>Big.MOST)return Backups.Said.NOT_TAKEN;
        List<java.net.InetSocketAddress> doors=bigDoors(where,store,address);
        if(!reachable(doors))return Backups.Said.NO_DEVICE;
        if(!readsMessages(where,them)){tellMessages(where,store,keys,them);return Backups.Said.NOT_TAKEN;}
        Big.Offer offer=Big.Offer.pushing(name,kind,bytes);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return Backups.Said.NO_DEVICE;
            byte[] id=Message.newId();
            byte[] plain=Message.wrap(new Message.Card(id,Message.BIG,offer.said(),"",System.currentTimeMillis()));
            byte[] sealed=Envelope.seal(id,0,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
            if(!hand(where,store,keys,node,them,sealed))return Backups.Said.NO_DEVICE;
            // The offer may come a moment after the first knock: knocked again for two minutes.
            Big.push(doors,offer,source,120_000L);
            android.util.Log.i("Mininotes/Big","pushed a backup, "+(bytes>>20)+" MB");
            return Backups.Said.MADE;
        } catch(Exception notThere) {
            android.util.Log.i("Mininotes/Big","a backup not pushed: "+notThere.getMessage());
            return Backups.Said.NOT_TAKEN;
        }
    }

    /** When each device of mine was last told this device only keeps backups, by address: see {@link #tellKeepsBackups}. */
    private static final java.util.Map<String,Long> TOLD_KEEPS=new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * "This device only keeps backups", said by a backup server to each of the owner's devices (decision 330), every six hours,
     * and at once to one just added. Blocking.
     */
    static void tellKeepsBackups(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            for(NoteStore.Contact them:store.addresses()) {
                if(!them.mine||!them.paired()||them.agreement.length==0)continue;
                Long told=TOLD_KEEPS.get(them.address);
                if(told!=null&&now-told<6*60*60_000L)continue;
                byte[] sealed=Envelope.seal(new byte[16],0,now,Receipt.wrap(Receipt.KEEPS_BACKUPS),keys.signing(),Keys.publicKey(them.agreement));
                if(handSaying(where,store,keys,node,them,sealed))TOLD_KEEPS.put(them.address,now);
                // And that it reads a message, so it pushes: the offer of a backup is one.
                tellMessages(where,store,keys,them);
            }
        } catch(Exception notNow){/* the next round */}
    }

    /** The doors of a paired device that are its own, never a relay: on this network first, then any it proved. */
    static List<java.net.InetSocketAddress> bigDoors(Context where,NoteStore store,String address) {
        List<java.net.InetSocketAddress> out=new ArrayList<>();
        try {
            MaximaNode node=Node.node(where);
            NoteStore.Contact them=store.address(address);
            com.eurobuddha.maxima.core.contacts.Contact reach=them==null||node==null?null:Node.known(where,them.contact);
            if(reach==null)return out;
            for(String door:Direct.firstTries(node.lanAddressFor(reach.publicKey),new ArrayList<>(reach.addresses),reach.publicKey))
                try{out.add(Home.where(door));}catch(RuntimeException notADoor){/* the next */}
        } catch(RuntimeException notNow){/* none, then */}
        return out;
    }

    /** Whether any of these doors answers at all, now. */
    private static boolean reachable(List<java.net.InetSocketAddress> doors) {
        for(java.net.InetSocketAddress door:doors)try(java.net.Socket socket=new java.net.Socket()){socket.connect(door,Direct.LEASH);return true;}
            catch(java.io.IOException notThere){/* the next */}
        return false;
    }

    /**
     * A big file offered to this device: kept in memory and put to the person (a notification, a box), or, from somebody whose
     * files are taken without asking, saved at once. A stranger, or somebody blocked, is not heard.
     */
    private static Landed bigArrived(Context where,NoteStore store,NoteStore.Contact from,Message.Card card) {
        Big.Offer offer=Big.Offer.read(card.words);
        if(offer==null||!from.paired()||store.senderBlocked(from.address))return new Landed(null,null);
        // Pushed (decision 330): only from one of mine, and only where this device takes pushes (a backup server, the PC).
        if(offer.pushed) {
            java.util.function.Consumer<Big.Waiting> take=Big.pushed;
            if(!from.mine||take==null||Big.taking(offer.token))return new Landed(null,null);
            android.util.Log.i("Mininotes/Big","a backup is being pushed here, "+(offer.bytes>>20)+" MB");
            take.accept(new Big.Waiting(from.address,from.name,offer,System.currentTimeMillis()));
            return new Landed(null,null);
        }
        if(Big.waitingFor(offer.token)!=null)return new Landed(null,null);
        Big.Waiting one=new Big.Waiting(from.address,from.name,offer,System.currentTimeMillis());
        Big.waiting(one);
        android.util.Log.i("Mininotes/Big","a big file offered, "+(offer.bytes>>20)+" MB");
        java.util.function.Consumer<Big.Waiting> tell=store.acceptsWithoutAsking(from.address)?Big.saving:Big.asking;
        if(tell==null)tell=Big.asking;
        if(tell!=null)tell.accept(one);
        return new Landed(null,null);
    }

    /** A big file saved: fetched from the sender's own door into {@code to}. Throws where it could not be. Blocking. */
    static void saveBig(Context where,NoteStore store,Big.Waiting one,java.io.OutputStream to,java.util.function.LongConsumer progress) throws java.io.IOException {
        List<java.net.InetSocketAddress> doors=bigDoors(where,store,one.from);
        if(doors.isEmpty())throw new java.io.IOException(Say.t(Big.NOT_NEAR,one.name));
        Big.fetch(doors,one.offer,to,progress);
        Big.answered(one.offer.token);
        android.util.Log.i("Mininotes/Big","a big file saved, "+(one.offer.bytes>>20)+" MB");
    }

    /** A big file refused: the sender told where it can be, so it stops offering it. Blocking. */
    static void refuseBig(Context where,NoteStore store,Big.Waiting one) {
        Big.answered(one.offer.token);
        Big.refuse(bigDoors(where,store,one.from),one.offer);
    }

    // ---- a help request, sent the moment a note or folder set to send one is opened: see Help (decision 113) ----------

    /** When each device was last told this run that this build reads a help request, by fingerprint, and whether it went. */
    private static final java.util.Map<String,long[]> TOLD_HELP=new java.util.concurrent.ConcurrentHashMap<>();

    /** A device whose build has said it reads a help request, kept across restarts. Whether it is the first time. */
    private static boolean helpHeard(Context where,String fingerprint) {
        if(where==null||fingerprint==null||fingerprint.isEmpty())return false;
        android.content.SharedPreferences kept=where.getSharedPreferences("post",Context.MODE_PRIVATE);
        java.util.Set<String> all=new java.util.HashSet<>(kept.getStringSet("help",new java.util.HashSet<String>()));
        if(!all.add(fingerprint))return false;
        kept.edit().putStringSet("help",all).apply();
        return true;
    }

    /**
     * Whether a device's build has said it reads a help request (see {@link Receipt#HELP}). The owner, 2026-10-06, decision
     * 113: a help request is never sealed for one that has not, so a build from before is never sent a cry for help it would
     * write over a note. Only a device that said this is ever handed one.
     */
    static boolean readsHelp(Context where,NoteStore.Contact them) {
        if(where==null||them==null)return false;
        String who=fingerprint(them);
        return !who.isEmpty()&&where.getSharedPreferences("post",Context.MODE_PRIVATE)
            .getStringSet("help",new java.util.HashSet<String>()).contains(who);
    }

    /** "This build reads a help request", said to every paired device that has shown it knows what an answer is, as REFUSALS is. */
    private static void tellHelp(Context where,NoteStore store,Keys keys) {
        long now=System.currentTimeMillis();
        for(NoteStore.Contact them:store.addresses()) {
            String who=fingerprint(them);
            if(who.isEmpty()||!them.paired()||!sendsTo(store,them)||!(doesSpeak(where,them)||readsHelp(where,them)))continue;
            long[] told=told(where,TOLD_HELP,"help",who);
            if(told!=null&&(told[1]==1||now>=told[0]&&now-told[0]<TELL_AGAIN))continue;
            if(resting(who))continue;
            tellHelp(where,store,keys,them);
        }
    }

    private static void tellHelp(Context where,NoteStore store,Keys keys,NoteStore.Contact them) {
        String who=fingerprint(them);
        // Never the reason a node is started: this is said when the node is up for everything else.
        if(who.isEmpty()||!Node.running())return;
        long[] told={System.currentTimeMillis(),0};
        TOLD_HELP.put(who,told);
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return;
            byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),Receipt.wrap(Receipt.HELP),
                keys.signing(),Keys.publicKey(them.agreement));
            if(handSaying(where,store,keys,node,them,sealed)){told[1]=1;said(where,"help",who,"1");}
            android.util.Log.i("Mininotes/Post","said this build reads a help request"+(told[1]==1?"":": not taken"));
        } catch(Exception notNow){/* tried again in a while */}
    }

    /**
     * A help request handed, now, to the chosen contacts that can read one (the owner, 2026-10-06, decision 113). Sealed and
     * signed like everything else, so it looks like any other sealed message on the network; sealed only for a device that
     * has said it reads one, so an older build is never sent it and never writes it over a note. Nothing is logged: this is
     * the one thing the owner asked to be fully secret, so neither who it went to, nor the words, nor the place is written
     * anywhere. Returns how many it reached. The node is brought up if it is not, since the opening may be the first thing
     * this run. Blocking, so the caller is off the main thread.
     */
    static int sendHelp(Context where,NoteStore store,Keys keys,Help.Request request,java.util.Collection<String> to) {
        if(where==null||store==null||keys==null||request==null||to==null||to.isEmpty())return 0;
        int reached=0;
        try {
            MaximaNode node=Node.node(where);
            if(node==null)return 0;
            byte[] plain=Help.wrap(request);
            for(NoteStore.Contact them:store.addresses()) {
                if(!to.contains(them.address)||!them.paired()||them.agreement.length==0||!readsHelp(where,them))continue;
                try {
                    byte[] sealed=Envelope.seal(new byte[16],0,System.currentTimeMillis(),plain,keys.signing(),Keys.publicKey(them.agreement));
                    if(hand(where,store,keys,node,them,sealed))reached++;
                } catch(Exception noWay){/* one out of reach is not the rest kept from help; the next update tries again */}
            }
        } catch(Exception notNow){/* nothing said, nowhere; the next update tries again */}
        return reached;
    }

    /**
     * A help request that arrived (decision 113): opened, and handed up as a loud alert by whose it is, so the app says
     * "HELP from <name>", the words, the place, the time and a map link, and fills them in as the updates come. Nothing is
     * logged about it. Never taken for a note: these bytes are read before a note is, as a Parlons! address is.
     */
    private static Landed helpArrived(Context where,NoteStore store,NoteStore.Contact from,Envelope.Opened opened) {
        // It sent one, so it reads them.
        helpHeard(where,fingerprint(from));
        Help.Request request=Help.open(opened.text);
        if(request==null)return new Landed(null,null);
        String name=from==null||from.name==null?"":from.name;
        return Landed.help(request,name);
    }

    /** One line for the log about Parlons! addresses: counts and states, never a name, an address or a key. */
    private static void contacts(String said){android.util.Log.i("Mininotes/Parlons",said);}

    /** One line for the log about groups: counts and states, never a name, an id or a key. */
    private static void groups(String said){android.util.Log.i("Mininotes/Groups",said);}

    /** One line for the log about persons: what happened, never a name, an id, an address or a key. */
    private static void persons(String said){android.util.Log.i("Mininotes/Persons",said);}

    /** One line for the log about files sent on their own: counts and states, never a name, an address or a key. */
    private static void drop(String said){android.util.Log.i("Mininotes/Drop",said);}

    /**
     * Sealed bytes about files sent on their own, handed to a device as anything is, and which road they took said
     * in the log - read off Direct's counts, so near enough when two sends cross. A sending that never arrives looked,
     * from both ends, exactly like one still on its way.
     */
    private static MaximaSender.Result handDrop(Context where,NoteStore store,Keys keys,MaximaNode node,NoteStore.Contact them,
                                                byte[] sealed,String what) throws Exception {
        int doors=Direct.WENT.get(),homes=Direct.LEFT.get(),shut=Direct.SHUT_HERE.get();
        MaximaSender.Result said;
        try{said=handTo(where,store,keys,node,them,sealed);}
        // Out of reach while notes go only between the owner's devices is not a fault: said as what it is (decision 262).
        catch(Exception notSent){drop(what+": "+(Direct.WAITS.equals(notSent.getMessage())?"not in reach just now, it waits"
            :"not sent ("+notSent.getClass().getSimpleName()+")"));throw notSent;}
        boolean ok=said!=null&&said.isOk();
        drop(what+": "+(!ok?"not taken":Direct.road(doors,homes,shut,true).substring(2))+(said==null?"":" ("+said.statusName+")"));
        return said;
    }

    /**
     * An offer, taken in. A new sending from one of the owner's devices is fetched at once; from anybody else it
     * is asked about. An offer of one already settled is answered again - they did not hear the first answer - and
     * a later offer of one still coming says where its files are now.
     */
    private static Landed offered(Context where,NoteStore store,Keys keys,NoteStore.Contact from,String wire,long revision,
                                  List<Enclosure.Listed> files) {
        // Whoever offers takes files: nothing more needs saying to know it.
        takesFiles(where,fingerprint(from));
        long bytes=0;for(Enclosure.Listed one:files)bytes+=one.bytes;
        drop("offer heard: "+Drop.files(files.size())+", "+bytes+" bytes, offer "+revision+", from a device "
            +(from.paired()?"paired here":"not paired here")+(from.mine?", one of yours":""));
        NoteStore.Transfer was=store.transferOnTheWire(wire,from.address,false);
        if(was!=null) {
            final int again=Drop.answerAgain(was.state);
            if(again!=0) {
                drop("offer heard again, already settled here: answered again");
                final NoteStore.Contact them=from;final long offer=Math.max(revision,was.revision);
                ANSWERS.execute(()->{try{sayAboutSending(where,store,keys,them,wire,offer,again);}catch(Exception notNow){/* said when it is offered again */}});
                return new Landed(null,null);
            }
            NoteStore.Transfer now=store.offerArrived(from.address,from.name,wire,revision,files,was.state);
            drop(now==null?"offer heard again, older than the one here: nothing changes"
                :"offer heard again: "+(now.state==Drop.FETCHING?"its files are fetched from where it says now":"still waiting for your answer"));
            if(now!=null&&now.state==Drop.FETCHING)travelSoon(where,store,keys);
            return new Landed(null,null);
        }
        // Somebody whose things are accepted without asking (decision 269) is taken at once, as one of my own devices is.
        boolean unasked=!from.mine&&from.paired()&&store.acceptsWithoutAsking(from.address);
        int state=Drop.arriving(from.paired(),from.mine,unasked);
        if(state==0){drop("offer not taken: from a device not paired here");return new Landed(null,null);}
        NoteStore.Transfer now=store.offerArrived(from.address,from.name,wire,revision,files,state);
        if(now==null)return new Landed(null,null);
        drop(state==Drop.FETCHING?(unasked?"offer taken at once, accepted without asking: fetching":"offer taken at once, from one of your devices: fetching")
            :"offer put to you: waiting for Accept or Refuse");
        if(state==Drop.FETCHING){travelSoon(where,store,keys);return Landed.transfer(null,null);}
        return Landed.transfer(Drop.asking(from.name,files.size(),now.bytes())+".",now.id).by(from.name,files.size());
    }

    /** What the device a sending went to says about it: all here, refused, or its files cannot be had from where they are. */
    private static Landed answeredAboutTransfer(Context where,NoteStore store,Keys keys,NoteStore.Contact from,String wire,
                                                long revision,int about) {
        String heard=about==Receipt.DROP_HAVE?"have them":about==Receipt.DROP_REFUSED?"refused":"cannot get them";
        NoteStore.Transfer one=store.transferOnTheWire(wire,from.address,true);
        if(one==null){drop("answer heard ("+heard+") about a sending not kept here: nothing changes");return new Landed(null,null);}
        if(about==Receipt.DROP_MISSING) {
            // Only between the owner's devices there is nothing to send the pieces up to: they wait here, at the door.
            boolean again=Node.helpers()&&store.offerAgain(one.id,revision);
            drop("answer heard: they cannot get them"+(again?", so the pieces go up to relays and it is offered again"
                :Node.helpers()?", but the pieces went up a moment ago: left as it is":", and with no helpers the pieces wait at this door"));
            if(again)travelSoon(where,store,keys);
            return new Landed(null,null);
        }
        int next=Drop.after(one.state,about);
        drop("answer heard: "+heard+(next==one.state?", which changes nothing here":next==Drop.DELIVERED?": delivered":": refused"));
        if(next==one.state)return new Landed(null,null);
        store.settle(one.id,next);
        // Finished with, either way: the copies kept to send it, and its pieces, go.
        for(String pieces:store.sentAndDone(one.id))Node.forget(where,pieces);
        return Landed.transfer(Drop.answered(from.name,one.files.size(),next)+".",null).by(from.name,0);
    }

    /** An answer about a sending, to the device at the other end of it. The envelope names it where it names a note. */
    private static boolean sayAboutSending(Context where,NoteStore store,Keys keys,NoteStore.Contact them,String wire,long revision,
                                           int what) throws Exception {
        // Not the reason a node is started either: an answer not said now is said when they offer again.
        MaximaNode node=Node.running()?Node.node(where):null;
        if(node==null||them==null||!them.paired()){drop("an answer not said now: "+(node==null?"the node is not up":"not paired with them"));return false;}
        byte[] sealed=Envelope.seal(sixteen(wire),revision,System.currentTimeMillis(),Receipt.wrap(what),keys.signing(),Keys.publicKey(them.agreement));
        MaximaSender.Result said=handDrop(where,store,keys,node,them,sealed,"said "+(what==Receipt.DROP_HAVE?"every file is here"
            :what==Receipt.DROP_REFUSED?"refused":"the files cannot be had"));
        return said!=null&&said.isOk();
    }

    /**
     * One sending made here, made ready and offered: each file sealed and cut into pieces kept here, where this
     * device's door hands them out - and sent up to relays as well only where helpers may be used and the other
     * device can reach no door of this one's (see {@link Drop#upToRelays}), or it said it could not get them. Then
     * the list goes to them sealed, by their doors, their home, and with helpers the relays. Nothing counts as
     * delivered until they say every file is here.
     *
     * @return whether the offer was handed over
     */
    private static boolean offer(Context where,NoteStore store,Keys keys,com.eurobuddha.maxima.core.media.MediaService media,
                                 NoteStore.Transfer one) {
        try {
            NoteStore.Contact them=store.address(one.address);
            drop((one.tries==0&&one.revision<=1?"offering: ":"offering again (try "+(one.tries+1)+", offer "+one.revision+"): ")
                +Drop.files(one.files.size())+", "+one.bytes()+" bytes");
            if(them==null||!them.paired())throw new IllegalStateException("Not paired with them.");
            // Waiting for them to say they take files: an older build would write the offer over a note.
            if(!takesFiles(where,them)) {
                // Asked again, now and then, for a device that has shown it knows what an answer is: it says it back
                // (see Drop.sayBack), so one that said it once while this device could not hear is heard now.
                boolean asked=doesSpeak(where,them)&&Drop.sayBack(toldFilesAt(fingerprint(them)),System.currentTimeMillis());
                drop("waiting: they have not said they take files"+(asked?"; told them this device does, so they say it back":doesSpeak(where,them)?"":"; they have never answered anything, so nothing is said to them"));
                if(asked)tellTakesFiles(where,store,keys,them);
                store.offered(one.id);return false;
            }
            MaximaNode node=Node.node(where);
            if(node==null)throw new IllegalStateException("The node is not running.");
            boolean helpers=Node.helpers();
            boolean near=them.contact!=null&&!them.contact.trim().isEmpty()&&node.lanAddressFor(them.contact.trim())!=null;
            boolean proved=false;
            for(String door:node.directAddresses())if(!Direct.onThisNetwork(door))proved=true;
            boolean relays=Drop.upToRelays(helpers,near,proved);
            drop("they take files; "+(near?"on this network":"not seen on this network")+(proved?", this device has a public door":"")
                +(helpers?"":", only between your devices")+(relays?": pieces go up to relays":": pieces stay at this device's door"));
            List<Enclosure.Listed> listed=new ArrayList<>();
            for(NoteStore.Loose file:one.files) {
                if(!file.here)continue;
                String manifest=file.manifest;
                boolean up=helpers&&(relays||file.relay);
                if(manifest.isEmpty()||helpers&&file.relay) {
                    byte[] plain=store.bytesOf(file.held());
                    com.eurobuddha.maxima.core.media.MediaManifest made=up?media.publish(plain,file.kind)
                        :new com.eurobuddha.maxima.core.media.MediaService(null,Node.blobs(where)).publish(plain,file.kind);
                    if(up&&!Direct.relayed(made.sources,own(where))) {
                        Node.forget(where,made.encode());
                        throw new IllegalStateException("No relay took it.");
                    }
                    if(!manifest.isEmpty())Node.forget(where,manifest);
                    manifest=made.encode();
                    store.loosePublished(file.id,manifest);
                    if(up)store.wentToRelays(file.id);
                    drop("one file made ready: "+made.chunkIds.size()+" piece(s) "+(up?"kept here and on "+made.sources.size()+" relay(s)":"kept at this device's door"));
                }
                listed.add(new Enclosure.Listed(file.theirs,file.name,file.kind,file.bytes,manifest));
            }
            if(listed.isEmpty())throw new IllegalStateException("Nothing left to send.");
            byte[] sealed=Envelope.seal(sixteen(one.wire),one.revision,System.currentTimeMillis(),Drop.wrap(listed),
                keys.signing(),Keys.publicKey(them.agreement));
            MaximaSender.Result said=handDrop(where,store,keys,node,them,sealed,"offer of "+Drop.files(listed.size())+" ("+sealed.length+" bytes sealed) sent");
            store.offered(one.id);
            return said!=null&&said.isOk();
        } catch(Throwable notNow) {
            store.offered(one.id);
            drop("not offered this time: "+notNow.getClass().getSimpleName()+"; tried again on the clock");
            return false;
        } finally {tell(Landed.transfer(null,null));}
    }

    /**
     * A sending that is coming, fetched file by file: from the sender's doors first, and with helpers from wherever
     * the offer says its pieces are. Once every file is here the sender is told, which is what it counts as
     * delivered. What cannot be had is tried again on the clock, and the sender told so (see {@link Drop#sayMissing}).
     */
    private static void fetchSending(Context where,NoteStore store,Keys keys,com.eurobuddha.maxima.core.media.MediaService media,
                                     NoteStore.Transfer one) {
        NoteStore.Contact them=store.address(one.address);
        long now=System.currentTimeMillis();
        for(NoteStore.Loose file:one.files) {
            if(file.here||!Drop.due(file.tried,file.tries,now))continue;
            try {
                com.eurobuddha.maxima.core.media.MediaManifest said=com.eurobuddha.maxima.core.media.MediaManifest.decode(file.manifest);
                if(said==null||said.size!=file.bytes||!Enclosure.travels(said.size))throw new IllegalArgumentException("Not a file that can come.");
                int named=said.sources.size();
                said=fromTheirDoor(where,store,said,one.address,!Node.helpers());
                drop("fetching one file (try "+(file.tries+1)+"): "+said.chunkIds.size()+" piece(s), "
                    +(said.sources.size()-(Node.helpers()?named:0))+" of their doors answered, "+named+" relay(s) named in the offer");
                // Nowhere to ask: their door did not answer and the offer names no relay. Said to them now, so they send it up.
                if(said.sources.isEmpty())throw new IllegalStateException(Direct.WAITS);
                byte[] plain=media.fetch(said);
                if(plain.length!=file.bytes)throw new SecurityException("Not the file that was listed.");
                drop(store.looseArrived(file,plain)?"one file fetched and kept, "+plain.length+" bytes":"one file fetched and not kept: no longer wanted");
            } catch(Throwable notNow) {
                int tries=store.looseFailed(file.id);
                drop("one file could not be fetched ("+tries+" tries): "+notNow.getClass().getSimpleName());
                if(Drop.sayMissing(tries,Node.helpers()))
                    try{sayAboutSending(where,store,keys,them,one.wire,one.revision,Receipt.DROP_MISSING);}catch(Exception notNow2){/* said again after more tries */}
            }
        }
        NoteStore.Transfer now2=store.transfer(one.id);
        if(now2==null||now2.state!=Drop.FETCHING||!now2.allHere()){tell(Landed.transfer(null,null));return;}
        store.settle(now2.id,Drop.HERE);
        drop("every file is here: "+Drop.files(now2.files.size())+", "+now2.bytes()+" bytes");
        try{sayAboutSending(where,store,keys,them,one.wire,now2.revision,Receipt.DROP_HAVE);}catch(Exception notNow){/* said again when they offer again */}
        tell(Landed.transfer(Drop.arrived(now2.name,now2.files.size())+".",null).by(now2.name,now2.files.size()));
    }

    /**
     * A sending made here, offered at once rather than at the next round - somebody has just pressed Send and is
     * watching. On the file worker, behind whatever it is doing, so a file is never made ready twice at once.
     * Blocking: the worker calls it.
     *
     * @return what to say about it now
     */
    static String sendNow(final Context where,final NoteStore store,final Keys keys,final String id) throws Exception {
        java.util.concurrent.Future<String> done=FILES.submit(()->{
            NoteStore.Transfer one=store.transfer(id);
            if(one==null)return "";
            com.eurobuddha.maxima.core.media.MediaService media=Node.media(where);
            if(media==null)drop("made: "+Drop.files(one.files.size())+", not offered yet: the node is not up");
            boolean handed=media!=null&&offer(where,store,keys,media,one);
            NoteStore.Contact them=store.address(one.address);
            return Drop.afterSending(one.name,handed,takesFiles(where,them));
        });
        return done.get(3,java.util.concurrent.TimeUnit.MINUTES);
    }

    /** A sending somebody asked about, accepted: fetched from now on. */
    static void takeSending(Context where,NoteStore store,Keys keys,String id) {
        NoteStore.Transfer one=store.transfer(id);
        if(one==null||one.out||one.state!=Drop.ASKING)return;
        store.settle(id,Drop.FETCHING);
        drop("accepted: fetching "+Drop.files(one.files.size()));
        travelSoon(where,store,keys);
    }

    /**
     * A received sending refused, or taken off the list before it had all come: nothing more of it is fetched, and
     * the device that sent it is told - now if it can be reached, and otherwise when it offers again. Blocking.
     */
    static void refuseSending(Context where,NoteStore store,Keys keys,String id) {
        NoteStore.Transfer one=store.transfer(id);
        if(one==null||one.out)return;
        store.forgetTransfer(id);
        drop(one.state==Drop.HERE?"a received sending taken off the list":"refused, or stopped while coming: they are told");
        if(one.state==Drop.HERE)return;
        try{sayAboutSending(where,store,keys,store.address(one.address),one.wire,one.revision,Receipt.DROP_REFUSED);}
        catch(Exception notNow){/* said when it is offered again */}
    }

    /** A sending made here, taken off the list: not offered again, and its copies and pieces let go. Blocking. */
    static void stopSending(Context where,NoteStore store,String id) {
        for(String pieces:store.forgetTransfer(id))Node.forget(where,pieces);
    }

    /**
     * A note id as the sixteen bytes an envelope carries. Every note this app makes is named by a UUID,
     * which is those sixteen bytes written out — so the id goes over the wire as what it already is, and
     * comes back the same on the other side. Anything else was not made here and cannot travel.
     */
    private static byte[] sixteen(String id) {
        java.util.UUID said;
        try{said=java.util.UUID.fromString(id);}
        catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("That note is older than sharing and has no id that travels.");
        }
        java.nio.ByteBuffer out=java.nio.ByteBuffer.allocate(16);
        out.putLong(said.getMostSignificantBits());out.putLong(said.getLeastSignificantBits());
        return out.array();
    }

    /** And back again, so the note that arrives is the same note it left as. */
    private static String idFrom(byte[] sixteen) {
        java.nio.ByteBuffer in=java.nio.ByteBuffer.wrap(sixteen);
        return new java.util.UUID(in.getLong(),in.getLong()).toString();
    }

    /**
     * An address to send to now. A permanent address is not routable itself — it is looked up, which is
     * what makes it survive a host move — so it is resolved before anything is handed to the transport.
     */
    private static String routable(MaximaNode node,String address) throws Exception {
        // Only between the owner's devices, an address is dialled without knowing whose it is only when it is on
        // this network: anywhere else it may be a relay, and a permanent one is a directory to ask.
        if(!Node.helpers()&&!Direct.onThisNetwork(address))throw new IllegalStateException(Direct.WAITS);
        if(!address.startsWith("MAX#"))return address;
        String live=node.resolvePermanent(address);
        if(live==null||live.trim().isEmpty())throw new IllegalStateException(
            "That permanent address could not be looked up, so there is nowhere to send to yet.");
        return live;
    }

    /**
     * Something arrived. Opened, checked against a device we know, and handed to the notebook, which
     * decides what the note says next — see {@link Arriving}.
     *
     * @return what to tell the reader, or null where the message was not ours to read
     */
    static Landed arrived(Context where,NoteStore store,Keys keys,byte[] message){return arrived(where,store,keys,message,false);}

    /**
     * The key the network knows a paired device by, written down from something it sent, where none is written or the
     * one written is not known to the network here. The envelope's signature says which device it is; the transport's,
     * which key it came from. Seen on 0.1.034: a PC linked through a list with a phone that sends only between its
     * owner's devices never had it written - the list gave a relay's address, which that phone does not dial - so the
     * phone did not believe the PC's announcements, never had a way to its door, and kept every answer to it.
     *
     * @return whether it was written down now
     */
    static boolean knownBy(Context where,NoteStore store,byte[] sender,String key) {
        if(sender==null||key==null||key.trim().isEmpty())return false;
        String who=Courier.hex(sender);
        for(NoteStore.Contact one:store.addresses()) {
            if(!one.paired()||!who.equals(fingerprint(one)))continue;
            String kept=one.contact==null?"":one.contact.trim();
            // One written already stays unless the network has no such device: never the reason a node is started.
            if(!kept.isEmpty()&&(Direct.key(kept).equals(Direct.key(key))||!Node.running()||Node.known(where,kept)!=null))return false;
            store.knownAs(one.address,key.trim());
            // Believed from its next announcement, not the next round's.
            Node.pairedWith(Direct.paired(store.addresses()));
            android.util.Log.i("Mininotes/Node","learnt which key a paired device has on the network ("+Direct.tag(key)+"), from what it sent: its announcements are believed now");
            return true;
        }
        return false;
    }

    /**
     * @param brought whether it was carried here by another device rather than coming from its sender: the
     *                sender was not heard from, and may be long gone, and what it carries is only a note or an
     *                answer - never something else to carry
     */
    private static Landed arrived(Context where,NoteStore store,Keys keys,byte[] message,boolean brought) {
        try {
            Envelope.Opened opened=Envelope.open(message,keys.agreement().getPrivate());
            // Signed by this device itself: something it sent to a contact that holds its own keys came back. Never
            // taken in as if somebody else had written it (the Pro, 2026-11-04: its own 37 KB note, twice every 30 s).
            if(isSelf(Courier.hex(opened.sender),myFingerprint(keys))) {
                android.util.Log.w("Mininotes/Post","dropped an arriving message: signed by this device itself");
                return new Landed(null,null);
            }
            // Who this device is, known before anything in it is taken in: a list of who has a note names it too (decision 324).
            if(store.mySigningKey==null||store.mySigningKey.isEmpty())store.knowMyself(keys.signing().getPublic().getEncoded());
            if(!brought)knownBy(where,store,opened.sender,Node.sentBy(message));
            // Somebody taking up an offer, which arrives from a phone this one has never heard of - that
            // is the whole point of it, so it is read before the check that everything else must pass.
            // The keys inside are checked against the signature the envelope was opened with, so the
            // sender is at least who these bytes say. Whether they may have anything is not decided here.
            Hello.Said accepted=Hello.open(opened.text);
            if(accepted!=null) {
                if(!java.util.Arrays.equals(Envelope.fingerprint(Keys.publicKey(accepted.signing)),
                    opened.sender))return new Landed(null,null);
                // Its build takes files sent on their own, which is written down by its key whoever it turns out to be:
                // it matters only once the device is paired here.
                if(accepted.files){takesFiles(where,Courier.hex(opened.sender));drop("a hello says they take files");}
                // A device linking through something both have: settled here, never put to anybody. See Linking.
                if(Linking.isLink(accepted))return linkHeard(where,store,keys,accepted,opened.sender);
                // A plain hello from a device already paired here is its answer to ours: nothing to ask
                // anybody, only to stop saying hello to it.
                if(accepted.target.isEmpty())for(NoteStore.Contact known:store.addresses())
                    if(known.signing.length>0&&java.util.Arrays.equals(Envelope.fingerprint(Keys.publicKey(known.signing)),opened.sender)) {
                        store.answered(known.address);
                        return new Landed(null,null);
                    }
                return new Landed(null,accepted);
            }
            // Who sent it: the envelope names a signing key by its fingerprint, and only a device already
            // paired with counts. An unsigned stranger's note is not put on anybody's shelves.
            NoteStore.Contact from=null;
            for(NoteStore.Contact contact:store.addresses()) {
                if(contact.signing.length==0)continue;
                if(java.util.Arrays.equals(Envelope.fingerprint(Keys.publicKey(contact.signing)),
                    opened.sender)){from=contact;break;}
            }
            if(from==null) {
                android.util.Log.w("Mininotes/Post","dropped an arriving message: signed by no device paired here");
                return new Landed(null,null);
            }
            // Blocked on this device (decision 127): nothing more from them is taken in. What is already kept stays.
            if(store.senderBlocked(from.address)) {
                android.util.Log.w("Mininotes/Post","dropped an arriving message: the sender is blocked");
                return new Landed(null,null);
            }
            // Heard from, so there: whatever receipt or card waited for them is tried again at once (decision 226).
            UNREACHED.remove(fingerprint(from));
            // Anything signed by a device linked through a list is its answer, whatever it is: it has this device's key
            // to seal for and is plainly there. Waiting for the hello alone left both ends waiting where that one hello
            // went astray while notes were going both ways. See Linking.
            if(store.linkingNow().contains(from.address)) {
                store.answered(from.address);
                link("heard from a device linked through a list: it has answered, and what it is owed goes now");
                final String them=from.address;
                ANSWERS.execute(()->{if(Node.running())owedTo(where,store,keys,them);});
            }
            if(!brought) {
                // Heard from, so there: nothing sent to them for a while is left with anybody else, and
                // whatever was being carried for them goes now.
                final String who=fingerprint(from);
                long now=System.currentTimeMillis();
                Long before=HEARD.put(who,now);
                // Brought at once only to a device back from being away: one that is there is brought things when their turn
                // comes, or every message from it brought everything held for it again (decision 257).
                boolean back=before==null||now<before||now-before>=Courier.THERE;
                if(back&&!store.carried(who).isEmpty())bringSoon(where,store,keys,who);
                Courier.Said carried=Courier.open(opened.text);
                if(carried!=null)return carried(where,store,keys,from,opened,carried);
            } else if(Courier.open(opened.text)!=null) {
                // Something to carry, inside something carried: never. Read as a note, it would be bytes
                // written over somebody's words.
                return new Landed(null,null);
            }
            // Heard from them, so they are there: what they are owed goes now, not at its next turn (the owner, 2026-10-14).
            if(!brought)heardFrom(where,store,keys,from);
            String id=idFrom(opened.page);
            // An answer: they have this note, at this revision. Which note and which revision are in the
            // envelope's header, sealed and signed with the rest, so nobody in between can say it for them.
            int about=Receipt.open(opened.text);
            if(about!=0)speaks(where,from);
            // A refusal of something this device shared, longer than an answer and so read before a note could be (decision
            // 109). Never something carried: nothing leaves one with a carrier.
            Receipt.Refused refusal=brought?null:Receipt.refused(opened.text);
            if(refusal!=null){speaks(where,from);return refusedHere(store,from,id,opened,refusal);}
            if(about==Receipt.LOCKED||about==Receipt.OPENED) {
                boolean changed=store.lockedThere(from.address,about==Receipt.LOCKED,opened.moment);
                android.util.Log.i("Mininotes/Post",(about==Receipt.LOCKED?"heard their notebook is locked: what waits for them is taken in when it is opened"
                    :"heard their notebook is open again")+(changed?"":", which changes nothing here"));
                return new Landed(null,null,"",true);
            }
            // Anything else from them says their notebook is open: a locked one takes nothing in and says nothing. Not what
            // another device carried here, which may have been written before it locked.
            if(!brought&&store.lockedThere(from.address,false,opened.moment))
                android.util.Log.i("Mininotes/Post","heard from a device that said its notebook was locked: it is open again");
            if(about==Receipt.ASK) {
                // They are there, and asking. Whatever they are owed goes now rather than when its turn
                // next comes round, which after a long silence can be a quarter of an hour away.
                android.util.Log.i("Mininotes/Post","asked for whatever they are owed");
                final String them=from.address;
                // About one note, where the envelope names one: that note whole, whatever is thought to be
                // owed. About none: whatever they are owed of everything.
                boolean named=false;
                for(byte one:opened.page)if(one!=0)named=true;
                final String which=named?id:null;
                ANSWERS.execute(()->{
                    try {
                        Done done=which!=null
                            ?send(where,store,keys,NoteStore.Branch.Kind.PAGE,which,them,true)
                            :send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,them);
                        android.util.Log.i("Mininotes/Post","sent what they asked for: "+done.sent+" went, "
                            +done.failed+" did not");
                    } catch(Exception notNow) {
                        android.util.Log.w("Mininotes/Post","could not send what they asked for: "
                            +notNow.getClass().getSimpleName());
                    }
                });
                return new Landed(null,null);
            }
            Sharing.Scope leftAt=Receipt.leftScope(about);
            if(leftAt!=null) {
                // When they left rides where a note's revision does: sealed and signed with the rest.
                boolean any=store.left(from.address,id,leftAt,opened.revision);
                android.util.Log.i("Mininotes/Post","they have left a "+leftAt.name().toLowerCase(java.util.Locale.ROOT)
                    +(any?"":" - which they were not on here, so it changes nothing"));
                if(!any)return new Landed(null,null);
                // Said here, in this device's language (decision 256): a phrase for each kind of thing.
                return Landed.left(leftAt==Sharing.Scope.PAGE?Say.t("{0} unfollowed a note.",from.name):leftAt==Sharing.Scope.FILE?Say.t("{0} unfollowed a file.",from.name)
                    :Say.t("{0} unfollowed a folder.",from.name),id).by(from.name,0);
            }
            Sharing.Scope offAt=Receipt.removedScope(about);
            if(offAt!=null) {
                // Somebody says this phone is off a thing of theirs. Whether they may say so is the
                // notebook's to check; when they decided it rides where a revision does.
                boolean any=store.takenOff(from.address,id,offAt,opened.revision);
                android.util.Log.i("Mininotes/Post","they say this phone is off a "+offAt.name().toLowerCase(java.util.Locale.ROOT)
                    +(any?"":" - which changes nothing here"));
                if(!any)return new Landed(null,null);
                String kind=offAt==Sharing.Scope.PAGE?Say.t("{0} removed you from a note.",from.name):offAt==Sharing.Scope.FILE?Say.t("{0} removed you from a file.",from.name)
                    :Say.t("{0} removed you from a folder.",from.name);
                return Landed.left(kind+" "+Say.t(Node.A_PC?"Your copy stays on this PC.":"Your copy stays on this phone."),id).by(from.name,0);
            }
            if((about==Receipt.COLLECTED||about==Receipt.COLLECTED_ANSWER)&&!brought) {
                // They have what was being carried for them: that note, or that answer, up to that revision.
                int gone=store.collected(fingerprint(from),Courier.hex(opened.page),
                    about==Receipt.COLLECTED_ANSWER?Courier.ANSWER:Courier.NOTE,opened.revision);
                android.util.Log.i("Mininotes/Post","collected: "+gone+" thing(s) carried for them let go");
                return new Landed(null,null);
            }
            if(about==Receipt.TAKES_FILES) {
                // A build that takes files on their own, and says so. This one says it back - unless it did within the
                // last minute - so a device just updated hears about this one at once rather than when this one next
                // starts. Only once a run, it was lost for good when this one said it first, to a build from before.
                String who=fingerprint(from);
                takesFiles(where,who);
                boolean back=Drop.sayBack(toldFilesAt(who),System.currentTimeMillis());
                if(back){final NoteStore.Contact them=from;ANSWERS.execute(()->tellTakesFiles(where,store,keys,them));}
                // Whatever was waiting for them to say so goes now, not when its turn next comes round.
                boolean waiting=store.wakeSendings(from.address);
                drop("heard they take files"+(back?"; said it back":"")+(waiting?"; what waited for them is offered now":""));
                if(waiting)travelSoon(where,store,keys);
                return new Landed(null,null);
            }
            if(about==Receipt.PERSONS||about==Receipt.PERSONS_MINE) {
                // A build that knows about persons, saying whether it counts this device as its owner's. Said back
                // as "takes files" is, and whatever card they have had from here goes again: they may be just started.
                // Only news is answered: the same word again within minutes is two devices answering each other.
                String who=fingerprint(from);
                long now=System.currentTimeMillis();
                long[] heard=HEARD_PERSONS.put(who,new long[]{about,now});
                boolean anew=Persons.news(heard==null?0:(int)heard[0],heard==null?0:heard[1],about,now);
                readsCards(where,who,about==Receipt.PERSONS_MINE);
                if(anew){CARDS.remove(who);unsaid(where,"cardPersons",who);}
                long[] told=TOLD_PERSONS.get(who);
                boolean back=anew&&Drop.sayBack(told==null?0:told[1],now);
                if(back){final NoteStore.Contact them=from;ANSWERS.execute(()->tellPersons(where,store,keys,them));}
                persons("heard their build knows about persons"+(about==Receipt.PERSONS_MINE?", and counts this device as the owner's":"")
                    +(anew?"":", as they said a moment ago")+(back?"; said it back":""));
                if(anew&&from.mine&&about==Receipt.PERSONS_MINE)ANSWERS.execute(()->sendCards(where,store,keys));
                return new Landed(null,null);
            }
            if(about==Receipt.TREE) {
                // A build that knows about trees, and says so. Said back as persons is, and - the first time it is heard
                // - whatever waited for it goes now, off this thread: what does not fit three levels, and collections on
                // their own.
                String who=fingerprint(from);
                boolean first=trees(where,who);
                long[] told=TOLD_TREES.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellTrees(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build knows about trees"+(first?"":", as it said before")
                    +(back?"; said it back":""));
                if(first)ANSWERS.execute(()->{
                    // Something just arrived, so the node is up; where it is not, nothing is started for this.
                    if(!Node.running())return;
                    try {
                        Done done=send(where,store,keys,NoteStore.Branch.Kind.LIBRARY,Sharing.EVERYTHING,them.address);
                        android.util.Log.i("Mininotes/Post","sent what waited for their build to know about trees: "
                            +done.sent+" went, "+done.failed+" did not");
                    } catch(Exception notNow) {
                        android.util.Log.w("Mininotes/Post","could not send what waited for trees: "+notNow.getClass().getSimpleName());
                    }
                });
                // The first time, what waited for it to be updated waits no more: the marks and the words about them are
                // asked again at once, before anything has gone.
                return first?Landed.people(""):new Landed(null,null);
            }
            if(about==Receipt.LOOSE) {
                // A build that knows files on their own, and says so: heard as trees is, and, the first time, whatever
                // file waited for it goes now, off this thread. Until this, no sleeve was ever sealed for it.
                String who=fingerprint(from);
                boolean first=loose(where,who);
                long[] told=TOLD_LOOSE.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellLoose(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build knows files on their own"+(first?"":", as it said before")
                    +(back?"; said it back":""));
                if(first)ANSWERS.execute(()->{
                    if(!Node.running())return;
                    try {
                        Done done=sleeves(where,store,keys,null,them.address,false);
                        android.util.Log.i("Mininotes/Post","sent the files that waited for their build: "+done.sent+" went, "+done.failed+" did not");
                    } catch(Exception notNow) {
                        android.util.Log.w("Mininotes/Post","could not send the files that waited: "+notNow.getClass().getSimpleName());
                    }
                });
                return first?Landed.people(""):new Landed(null,null);
            }
            if(about==Receipt.REFUSALS) {
                // A build that reads a refusal, and says so: heard as files on their own are, and said back (decision 109).
                String who=fingerprint(from);
                boolean first=refusals(where,who);
                long[] told=TOLD_REFUSALS.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellRefusals(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build reads a refusal"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.NOT_HERE) {
                // A build that reads "not here" on a list, and says so (decision 293): said back, and the first time, the list of
                // every thing with "not here" on it sent again, since what went to it before left those entries off.
                String who=fingerprint(from);
                boolean first=notHereHeard(where,who);
                long[] told=TOLD_NOT_HERE.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellNotHere(where,store,keys,them));
                if(first)ANSWERS.execute(()->sent(where,store,keys,store.notHereOn(),"with \"not here\" on them, again for a build that reads it"));
                android.util.Log.i("Mininotes/Post","heard their build reads \"not here\""+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.CHANGES) {
                // A build that reads only what changed in a note, and says so (decision 320): said back. Nothing waits for it:
                // its next note goes as a change where one is worth it.
                String who=fingerprint(from);
                boolean first=changesHeard(where,who);
                long[] told=TOLD_CHANGES.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellChanges(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build reads only what changed"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.ABOUT_ME) {
                // A build that reads who a device belongs to, and says so: said back, and told the first time (decision 163).
                String who=fingerprint(from);
                boolean first=aboutHeard(where,who);
                long[] told=TOLD_ABOUT.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellAboutMe(where,store,keys,them));
                if(first)ANSWERS.execute(()->sendAboutMe(where,store,keys,false));
                contacts("heard their build reads who a device belongs to"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.FACE_OWN) {
                // A build that takes a person's own picture, and says so: said back, and given yours the first time (decision 158).
                String who=fingerprint(from);
                boolean first=ownFaceHeard(where,who);
                long[] told=TOLD_FACE.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellOwnFace(where,store,keys,them));
                if(first)ANSWERS.execute(()->sendMyFace(where,store,keys,false));
                contacts("heard their build takes your own picture"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.PARLONS_OWN) {
                // A build that takes a person's own Parlons! address, and says so: heard as a help request is, said back,
                // and given yours the first time (decision 147).
                String who=fingerprint(from);
                boolean first=ownParlonsHeard(where,who);
                long[] told=TOLD_OWN.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellOwnParlons(where,store,keys,them));
                if(first)ANSWERS.execute(()->sendMyParlons(where,store,keys,false));
                contacts("heard their build takes your own Parlons! address"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.KEEPS_BACKUPS) {
                // A backup server of the owner's (decision 330): from now on it is sent nothing but backups, and anything that
                // was given to it is taken off, so nothing waits for it. Heard only from one of mine: nobody else's word counts.
                if(!from.mine)return new Landed(null,null);
                boolean first=store.keepsBackups(from.address,true);
                if(first){List<Sharing.Rule> off=store.offEverything(from.address);
                    android.util.Log.i("Mininotes/Post","heard one of the owner's devices only keeps backups"+(off.isEmpty()?"":"; taken off "+off.size()));}
                return new Landed(null,null);
            }
            if(about==Receipt.MESSAGES) {
                // A build that reads a message, and says so: heard as a help request is, and said back (decision 145).
                String who=fingerprint(from);
                boolean first=messagesHeard(where,who);
                long[] told=TOLD_MESSAGES.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellMessages(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build reads a message"+(first?"":", as it said before")+(back?"; said it back":""));
                // Whatever is waiting for them goes now that they can take it, and a conversation with them says so.
                if(first)ANSWERS.execute(()->messagesAgain(where,store,keys));
                return first?Landed.message("","",from.address):new Landed(null,null);
            }
            if(about==Receipt.MESSAGE_KEPT) {
                // A message from here, kept at its far end: no longer handed on. Only this says it got there.
                if(brought)return new Landed(null,null);
                store.messageDelivered(Message.hex(opened.page));
                return Landed.message("","","");
            }
            if(about==Receipt.HELP) {
                // A build that reads a help request, and says so: heard as a refusal is, and said back (decision 113).
                String who=fingerprint(from);
                boolean first=helpHeard(where,who);
                long[] told=TOLD_HELP.get(who);
                boolean back=Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellHelp(where,store,keys,them));
                android.util.Log.i("Mininotes/Post","heard their build reads a help request"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.GROUPS) {
                // A build that reads groups, and says so: heard as files on their own are, and said back to one of the
                // owner's devices. Said back means it is just started, so the card goes again, as a card of persons does.
                String who=fingerprint(from);
                boolean first=groupsHeard(where,who);
                long[] told=TOLD_GROUPS.get(who);
                boolean back=from.mine&&Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellGroups(where,store,keys,them));
                if(first||back){GROUP_CARDS.remove(who);ANSWERS.execute(()->sendGroups(where,store,keys));}
                groups("heard their build reads groups"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.STAMPS) {
                // A build that reads stamps, and says so: heard as groups are, and said back to one of the owner's devices.
                // Said back means it is just started, so the card goes again; and the first time, the Integritas key too
                // where the owner chose this device for it, since no key card went to it before it said this (decision 252).
                String who=fingerprint(from);
                boolean first=stampsHeard(where,who);
                long[] told=TOLD_STAMPS.get(who);
                boolean back=from.mine&&Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellStamps(where,store,keys,them));
                if(first||back){STAMP_CARDS.remove(who);ANSWERS.execute(()->sendStamps(where,store,keys));}
                if(first&&from.mine&&keyGoesTo(where,Integritas.KEY).contains(from.address)&&!store.secret(Integritas.KEY).isEmpty())
                    ANSWERS.execute(()->{if(Node.running())sendKey(where,store,keys,Integritas.KEY,java.util.Collections.singleton(them.address));});
                stamps("heard their build reads stamps"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.LINKS_MINE) {
                // A build that links devices of mine introduced to it: heard as stamps are, and said back to one of the owner's
                // devices. The first time, or just started, the card of persons goes again, so it keeps one (decision 268).
                String who=fingerprint(from);
                boolean first=linksMineHeard(where,who);
                long[] told=TOLD_LINKS.get(who);
                boolean back=from.mine&&Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellLinksMine(where,store,keys,them));
                if((first||back)&&from.mine){CARDS.remove(who);unsaid(where,"cardPersons",who);ANSWERS.execute(()->{sendCards(where,store,keys);introduce(where,store,keys);});}
                link("heard their build links devices of the owner's introduced to it"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.LINK_MINE)return brought?new Landed(null,null):introducedHere(where,store,keys,from,opened);
            if(about==Receipt.PARLONS) {
                // A build that reads Parlons! addresses, and says so: heard as groups are, and said back to one of the
                // owner's devices. Nothing waits for it: an address goes only when the owner saves one.
                String who=fingerprint(from);
                boolean first=parlonsHeard(where,who);
                long[] told=TOLD_PARLONS.get(who);
                boolean back=from.mine&&Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellParlons(where,store,keys,them));
                contacts("heard their build reads Parlons! addresses"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.CALLED) {
                // A build that reads the name we pick, and says so: heard as a Parlons! address is (decision 124).
                String who=fingerprint(from);
                boolean first=calledHeard(where,who);
                long[] told=TOLD_CALLED.get(who);
                boolean back=from.mine&&Drop.sayBack(told==null?0:told[0],System.currentTimeMillis());
                final NoteStore.Contact them=from;
                if(back)ANSWERS.execute(()->tellCalled(where,store,keys,them));
                contacts("heard their build reads the name we pick"+(first?"":", as it said before")+(back?"; said it back":""));
                return new Landed(null,null);
            }
            if(about==Receipt.DROP_HAVE||about==Receipt.DROP_REFUSED||about==Receipt.DROP_MISSING)
                return answeredAboutTransfer(where,store,keys,from,id,opened.revision,about);
            if(about==Receipt.FILE_HERE||about==Receipt.FILE_MISSING) {
                // About a file, which the envelope names where it names a note: see Enclosure.
                if(about==Receipt.FILE_HERE) {
                    // Believed of one still being fetched here too: see NoteStore.fileReached.
                    String note=store.noteOfFile(id);
                    boolean kept=store.fileReached(id,from.address);
                    // In History only for a file added here: every holder tells every other that it has one (decision 305).
                    if(kept){NoteStore.Held mine=store.file(id);if(mine!=null&&mine.origin.isEmpty())store.happened(History.Kind.FILE_REACHED,id,from.address,System.currentTimeMillis(),Routes.roadTo(from.contact));}
                    android.util.Log.i("Mininotes/Post","they have a file"+(kept?"":" - which is not known here, so it changes nothing"));
                    if(note==null)return new Landed(null,null);
                    moved(note);
                    return Landed.people(note);
                }
                NoteStore.Held file=store.file(id);
                if(file==null)return new Landed(null,null);
                // Only between the owner's devices nothing goes up: the pieces are here, and they come from this door.
                boolean again=Node.helpers()&&store.upAgain(id);
                android.util.Log.i("Mininotes/Post","they cannot get a file"+(again?", so it goes up again":" - it went up a moment ago, so they are told again where it is"));
                if(again)travelSoon(where,store,keys);
                // Told again where it is now, whatever else (decision 289): a picture on the Pro's Temp was asked for, four times
                // each, by the laptop and the Graphene, from the place it was offered at the Pro's door before it went up; the
                // Pro, its upload a moment old, left it there, and they asked for pieces the door no longer kept.
                else {final NoteStore.Contact them=from;ANSWERS.execute(()->sayWhereFileIs(where,store,keys,them,file));}
                return new Landed(null,null);
            }
            if(about==Receipt.WAITS||about==Receipt.ACCEPTED) {
                // Something of this device's waits on theirs to be accepted, or was accepted: the tick says which. See Asking.
                boolean changed=store.asked(from.address,id,about==Receipt.ACCEPTED);
                android.util.Log.i("Mininotes/Post",(about==Receipt.WAITS?"heard it waits there to be accepted":"heard it was accepted there")
                    +(changed?"":", which changes nothing here"));
                return changed?Landed.people(store.get(id)!=null?id:""):new Landed(null,null);
            }
            if(about!=0&&about!=Receipt.HAVE&&about!=Receipt.TOOK)return new Landed(null,null);   // a later build
            if(about!=0) {
                boolean believed=store.acknowledged(from.address,id,opened.revision,about==Receipt.TOOK);
                // Their own answer, never a relay's: a line in History (decision 305), with the road the note went (322).
                if(believed)store.happened(History.Kind.NOTE_REACHED,id,from.address,System.currentTimeMillis(),Routes.roadTo(from.contact));
                // Not a note: a collection that went on its own, which the envelope names by the bytes Things gives it.
                if(!believed&&store.get(id)==null)
                    believed=store.collectionAcknowledged(from.address,opened.page,opened.revision,about==Receipt.TOOK);
                // Nor a collection: a file that went on its own, which the envelope names by its own bytes (see Sleeve).
                if(!believed&&store.get(id)==null)
                    believed=store.fileAcknowledged(from.address,id,opened.revision,about==Receipt.TOOK);
                android.util.Log.i("Mininotes/Post","answered: revision "+opened.revision
                    +(about==Receipt.TOOK?", and they now say the same":", and they put it with their own")
                    +(believed?"":" - which this phone never sent, so it changes nothing"));
                return new Landed(null,null,id,believed);
            }
            // Who a device belongs to, from that device (decision 163): see AboutMe. It starts with the same four bytes as a
            // card of persons, which was read first and took every one of these for a broken one; so it is known by reading it
            // whole, before (decision 252). Read before a note is too, never carried.
            if(AboutMe.isCard(opened.text)&&AboutMe.open(opened.text)!=null)return brought?new Landed(null,null):aboutArrived(where,store,from,opened);
            // A card from another device of the owner's: see Persons. Known by its first four bytes and read before
            // a note is, which it would otherwise be taken for; never something carried, which is only notes and answers.
            if(Persons.isCard(opened.text))return brought?new Landed(null,null):cardArrived(where,store,keys,from,opened);
            // A groups card from another device of the owner's: see Groups. Read before a note is too, and never carried.
            if(Groups.isCard(opened.text))return brought?new Landed(null,null):groupsArrived(where,store,keys,from,opened);
            // The stamps on Minima from another device of the owner's (decision 252): see StampCard. Read before a note is too,
            // never carried.
            if(StampCard.isCard(opened.text))return brought?new Landed(null,null):stampsArrived(where,store,keys,from,opened);
            // A person's Parlons! address from another device of the owner's: see Parlons. Read before a note is too, never carried.
            if(Parlons.isCard(opened.text))return brought?new Landed(null,null):parlonsArrived(where,store,from,opened);
            // A person's own picture, from them (decision 158): see Face. Read before a note is too, never carried.
            if(Face.isCard(opened.text))return brought?new Landed(null,null):faceArrived(where,store,from,opened);
            // A key for a service, from another device of the owner's (decision 210): see KeyCard. Read before a note is too, never carried.
            if(KeyCard.isCard(opened.text))return brought?new Landed(null,null):keyArrived(where,store,from,opened);
            // The name we pick for a person, from another device of the owner's: see Called. Read before a note is too.
            if(Called.isCard(opened.text))return brought?new Landed(null,null):calledArrived(where,store,keys,from,opened);
            // A help request, sent silently when a note or folder opens (decision 113): see Help. Read before a note is too,
            // which these bytes would otherwise be taken for; never one something carried.
            if(Help.isCard(opened.text))return brought?new Landed(null,null):helpArrived(where,store,from,opened);
            if(Message.isCard(opened.text))return brought?new Landed(null,null):messageArrived(where,store,keys,from,opened);
            // Files sent to this device on their own, belonging to no note: see Drop. Read before a note is, which
            // these bytes would otherwise be taken for.
            List<Enclosure.Listed> offer=Drop.open(opened.text);
            if(offer!=null)return offered(where,store,keys,from,id,opened.revision,offer);
            // A file on its own: see Sleeve. Read before a note is too, which it would otherwise be taken for; never one
            // something carried, as a carton is not.
            Sleeve.Sent sleeve=Sleeve.open(opened.text);
            if(sleeve!=null)return brought?new Landed(null,null):sleeveArrived(where,store,keys,from,opened,sleeve);
            // A collection on its own: see Carton. Read before a note is too, which it would otherwise be taken for.
            Carton.Sent carton=Carton.open(opened.text);
            if(carton!=null)return brought?new Landed(null,null):cartonArrived(where,store,keys,from,opened,carton);
            Parcel.Sent parcel=Parcel.open(opened.text);
            // Their build carries: from now on they may be left things, and brought them.
            if(parcel!=null&&parcel.carries)carries(where,from);
            // The colour they chose for themselves, from them and not from whoever brought it.
            if(parcel!=null&&!brought&&parcel.inkAt>0)store.inkSaid(from,parcel.ink,parcel.inkAt);
            // Something sent before notes carried their shelf: the text and nothing else. Still readable,
            // and it goes wherever anything whose shelf we were not told goes.
            if(parcel==null) {
                String text=new String(opened.text,StandardCharsets.UTF_8);
                int line=text.indexOf('\n');
                parcel=new Parcel.Sent("","","","",line<0?"":text.substring(0,line),
                    line<0?text:text.substring(line+1),false);
            }
            // Unsubscribed. They can still send it; this phone has stopped taking it in, and says nothing
            // about a thing somebody asked not to hear about again.
            NoteStore.Refusal refused=store.refusal(from.address,id,parcel);
            if(refused!=null&&refused.gone&&givenAgain(store,parcel,refused.at)) {
                // Left, and then given it again: the later decision stands, as it does for anybody.
                android.util.Log.i("Mininotes/Post","given again what this phone had left, so it is taken in");
                store.followAgain(from.address,id,refused);
                refused=null;
            }
            if(refused!=null) {
                // Nothing is said to the person, who asked not to hear. The log is told, because from
                // the outside this looks exactly like a note that never arrived.
                android.util.Log.i("Mininotes/Post",refused.gone
                    ?"not taken in: this phone has left it, and they are told again"
                    :refused.quiet?"not taken in: this device refused it quietly":"not taken in: this phone has unsubscribed from it");
                // They had not heard, or it went before they did. Said again, or they go on sending.
                if(refused.gone&&doesSpeak(where,from))
                    tellOff(where,store,keys,from,opened.page,refused.at,Receipt.left(refused.scope()));
                // Refused quietly when it first came (decision 109): answered as had, so they stop sending it, and told nothing.
                if(refused.quiet&&parcel.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,false);}
                return new Landed(null,null);
            }
            // Only what changed (decision 320): the whole text rebuilt from this device's own copy of what it was made against,
            // before anything here reads the parcel; from then on it is a whole note like any other. Where that copy is not
            // here exactly, nothing at all is taken and nothing answered: the note is asked for whole.
            if(parcel.changeSaid) {
                Parcel.Sent whole=rebuilt(store,id,from.address,parcel);
                // Older than what this device and they already both had: old news, answered and never asked for whole.
                // Asked for whole, each one of a closed phone's backlog brought the note again, and the next copy of
                // the backlog asked again: the Pro asked 129 times in two minutes (2026-11-04).
                if(whole==null&&store.get(id)!=null&&Arriving.stale(opened.revision,store.agreedAt(id,from.address))) {
                    android.util.Log.i("Mininotes/Post","a change, revision "+opened.revision+", older than what we agreed on: nothing taken, nothing asked");
                    if(parcel.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,false);}
                    return new Landed(null,null);
                }
                if(whole==null) {
                    android.util.Log.i("Mininotes/Post","a change, revision "+opened.revision
                        +(parcel.change==null?", which did not read":" against "+parcel.change.base+", whose text is not here exactly")
                        +", "+opened.text.length+" bytes: nothing taken, the whole note asked for");
                    askWhole(where,store,keys,from,opened.page);
                    return new Landed(null,null);
                }
                android.util.Log.i("Mininotes/Post","a change, revision "+opened.revision+" against "+parcel.change.base+", "
                    +opened.text.length+" bytes, rebuilt to "+whole.body.length()+" characters");
                parcel=whole;
            } else if(parcel.answer)android.util.Log.i("Mininotes/Post","a whole note, revision "+opened.revision+", "+opened.text.length+" bytes");
            // Who else has it, folded in before the note itself: the list is what says whether this phone
            // may hand it on, and it should not be one note behind the thing it describes.
            store.tookMembership(from.address,id,parcel);
            // And everybody on it this device is not linked with, linked through it - where the list came from a
            // device with a say in who has the thing. See Linking.
            linkThrough(where,store,keys,from.address,id,parcel);
            // Something of theirs is here, so whatever was accepted has been answered and need not be
            // said again.
            store.answered(from.address);
            // What they may do with it here decides what becomes of what they sent. A reader's words are
            // not written down, and neither are those of somebody taken off - who is told again, since
            // they plainly had not heard. Both are answered all the same, or they would send the same
            // thing every quarter of an hour for ever, which is the fault Pause still has.
            Sharing.Rule may=store.standingOf(from.address,id,parcel);
            if(may==null||!may.level.writes()) {
                android.util.Log.i("Mininotes/Post","not taken in: "+(may==null?"they were never given this"
                    :may.level==Sharing.Level.GONE?"they were taken off this, and are told again":"they may only read this"));
                if(parcel.answer){speaks(where,from);answer(where,store,keys,from,opened.page,opened.revision,false);}
                if(may!=null&&may.level==Sharing.Level.GONE&&store.saysWhoHas(may.scope,may.target)&&doesSpeak(where,from))
                    tellOff(where,store,keys,from,opened.page,may.changed,Receipt.removed(store.oldScopeOf(may.scope,may.target)));
                return Landed.people(id);
            }
            // What the note said before, so that one which arrives saying the same - sent only to carry
            // a change in who may do what - is not announced as "updated a note".
            NoteStore.Note before=store.get(id);
            boolean same=before!=null&&before.body.equals(parcel.body)
                &&(before.title==null?"":before.title).equals(parcel.title==null?"":parcel.title);
            Arriving.Decision said=store.landed(id,from.address,opened.revision,parcel,store.someBook());
            // The files it keeps at their end, now that the note is here to keep them with: what is not here
            // yet is fetched in the background, and what came from them and is no longer listed goes. A list
            // not said - an older build, or one that did not fit - changes nothing.
            if(parcel.files!=null) {
                if(store.listed(id,from.address,parcel.filesAsOf,parcel.files))travelSoon(where,store,keys);
                moved(id);
                // And what of theirs is here already, said to them - see NoteStore.haveHere.
                sayWeHave(where,store,keys,from,store.haveHere(id,parcel.files));
            }
            // The stamps on Minima of the note and of the files it keeps, that came with it (decision 251): only theirs.
            NoteStore.StampsTaken stamped=parcel.stamps==null?null:stampsWith(where,store,keys,store.stampsCameWith(id,parcel.files,parcel.stamps));
            // Written down, so now it can be said. Whatever was decided about it - taken, put together,
            // or set aside as older than what is here - they are told that what they sent has arrived:
            // that is the question they asked. Only a phone that asked is answered; see Parcel.answer.
            if(parcel.answer) {
                speaks(where,from);
                // Whether this phone's note now says exactly what they sent - which is what lets them
                // count this revision as one both phones have, and not only one that arrived.
                boolean took=said.what==Arriving.What.NEW||said.what==Arriving.What.NEWER;
                answer(where,store,keys,from,opened.page,opened.revision,took);
            }
            // Shared with me for the first time by somebody else, or more of something that still waits: kept, shown nowhere,
            // and said only as the question it is (decision 109), never as "shared a note" or "updated a note".
            // Said in a line only as it first comes; what follows of it is taken in quietly, still waiting.
            // Changed by somebody else, not one of my own devices: "new" on it, and on the folders it is in, until it is opened
            // (the owner, 2026-10-14). What is open as it changes is seen at once (changedUnderneath, both apps).
            if(!same&&!from.mine&&(said.what==Arriving.What.NEW||said.what==Arriving.What.NEWER||said.what==Arriving.What.MERGED))
                store.unseen(id);
            // From one of my own devices: ✓ instead, freshly updated (the owner, 2026-10-19; decision 220).
            else if(!same&&from.mine&&(said.what==Arriving.What.NEWER||said.what==Arriving.What.MERGED))
                store.updatedHere(id);
            Object[] waits=store.waitsUnder(NoteStore.Branch.Kind.PAGE,id);
            // And that it waits here to be accepted, so their tick does not say "everybody has it" yet (see Asking).
            if(waits!=null&&parcel.answer)sayWaits(where,store,keys,from,opened.page,opened.revision,NoteStore.Branch.Kind.PAGE,id);
            // Something somebody wrote, taken in here: a line in History (decision 305). One waiting to be accepted is not, yet.
            if(waits==null&&!same&&(said.what==Arriving.What.NEW||said.what==Arriving.What.NEWER||said.what==Arriving.What.MERGED))
                store.happened(History.Kind.NOTE_CAME,id,from.address,System.currentTimeMillis());
            if(waits!=null)return stamped(Landed.waiting(said.what==Arriving.What.NEW?waitingSaid(store,from,waits):null,(String)waits[1]).by(from.name,0),stamped);
            // Nothing in it changed. The marks and the box are asked again, because who may do what can have.
            if(same&&said.what!=Arriving.What.MERGED)return stamped(Landed.people(id),stamped);
            switch(said.what) {
                // In the person's language (decision 236): the line is said on the phone as it is.
                case NEW: return stamped(new Landed(Say.t("{0} shared a note with you.",from.name),null,id).by(from.name,0).newly(),stamped);
                case NEWER: return stamped(new Landed(Say.t("{0} updated a note.",from.name),null,id).by(from.name,0),stamped);
                case MERGED: return stamped(new Landed(said.keepTheirs
                    ? Say.t("{0} wrote in the same place you did. Both are in the note.",from.name)
                    : Say.t("{0} wrote in a note you had also written in. Both are in it.",from.name),null,id).by(from.name,0),stamped);
                default: return stamped(new Landed(null,null),stamped);
            }
        } catch(Exception e) {
            // A message that cannot be opened is not ours, or has been tampered with. Either way it is
            // dropped in silence: saying so would tell a stranger their guess was close. Silence towards
            // them, that is. This phone's own log is told what kind of thing went wrong, because a fault
            // of this app's own making lands here too and would otherwise look like nothing arriving.
            android.util.Log.w("Mininotes/Post","dropped an arriving message: "+e.getClass().getSimpleName());
            return new Landed(null,null);
        }
    }
}
