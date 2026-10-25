package org.mininotes.android;

import java.time.*;
import java.time.format.DateTimeFormatter;

/** Read persisted evidence, never the time a relay accepted a parcel. */
final class SyncStatus {
    static final String ONLY_HERE="Only here",WAITING="Waiting for delivery",MATCHING="Waiting for matching versions",
        CONFIRMED="Sync confirmed",NOT_CONFIRMED="Sync not confirmed",UNAVAILABLE="Note unavailable";
    record State(long saved,long confirmed,String status) {
        /** One quiet line, the same marks as the tree: ✓ everybody has it, ↑ somebody is waiting. */
        String brief(String device){return brief(device,ZonedDateTime.now());}
        String brief(String device,ZonedDateTime now) {
            return switch(status) {
                // In the language in use (decision 256), the device as it is called and the time in their places.
                case ONLY_HERE -> Say.t(here(device)?"Only on this phone · saved {0}":pc(device)?"Only on this PC · saved {0}":"Only on {1} · saved {0}",shortly(saved,now),device);
                case CONFIRMED -> Say.t("✓ Synced {0}",shortly(confirmed,now));
                case WAITING, NOT_CONFIRMED -> Say.t("↑ Saved {0} · waiting to sync",shortly(saved,now));
                case MATCHING -> Say.t("↑ Saved {0} · waiting for the others",shortly(saved,now));
                default -> Say.t(status);
            };
        }
        /** Whether something written here has not reached everybody yet: worth saying before a tab closes. */
        boolean unsent(){return status.equals(WAITING)||status.equals(NOT_CONFIRMED)||status.equals(MATCHING);}
        /** Everything, exactly: for a tooltip or a tap. */
        String detail(String device) {
            boolean phone=here(device),pc=pc(device);
            return Say.t(phone?"Saved on this phone: {0}":pc?"Saved on this PC: {0}":"Saved on {1}: {0}",date(saved),device)
                +"\n"+(status.equals(CONFIRMED)?Say.t("Everyone it is shared with has this version: {0}",date(confirmed))
                    :status.equals(ONLY_HERE)?Say.t("Not shared."):Say.t(status+"."))
                +"\n\n"+Say.t(phone?"Times are local to this phone.":pc?"Times are local to this PC.":"Times are local to {0}.",device)
                +" "+Say.t("Devices that are offline may have newer edits. Files kept with the note, up to 16 MB each, go with it, and those kept with a folder go with the folder; larger ones stay on this device.");
        }
    }
    private static boolean here(String device){return "this phone".equals(device);}
    private static boolean pc(String device){return "this PC".equals(device);}
    private static String date(long millis) {
        return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z")
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }
    /** "14:05" today, "23 Sep, 14:05" this year, "23 Sep 2025" before. */
    static String shortly(long millis,ZonedDateTime now) {
        ZonedDateTime at=Instant.ofEpochMilli(millis).atZone(now.getZone());
        String pattern=at.toLocalDate().equals(now.toLocalDate())?"HH:mm":at.getYear()==now.getYear()?"d MMM, HH:mm":"d MMM yyyy";
        return DateTimeFormatter.ofPattern(pattern).format(at);
    }
    /**
     * One device a note reaches, as the line under its title shows it: a round with their initial, tinted by
     * where they stand.
     *
     * @param linked   whether this device is paired with them: a person only named in the list that came with the
     *                 note can be sent nothing from here, and their round says so
     * @param listedIn for somebody not linked, the name of the thing whose list names them; empty otherwise
     * @param listing  for somebody not linked, the rule that lists them, to take them off by; null otherwise
     */
    /**
     * The rounds a note's line draws, of everybody it reaches: all of them while they are few, else {@code most}, those that
     * need a look first (not linked, or not all there yet), and the others counted in one +N round that lists everybody (the
     * owner, 2026-10-15: "instead of showing the icons of every device on top of the note ... I can imagine having 10 or
     * more"). Never a +1: that would hide one round only to draw another in its place.
     */
    static java.util.List<Person> roundsShown(java.util.List<Person> who,int most) {
        if(who.size()<=most+1)return who;
        return lookFirst(who).subList(0,most);
    }
    /** Everybody, those that need a look first (not linked, or not all there yet), each part in the note's own order. */
    static java.util.List<Person> lookFirst(java.util.List<Person> who) {
        java.util.List<Person> first=new java.util.ArrayList<>(),rest=new java.util.ArrayList<>();
        for(Person one:who)(!one.linked()||one.mark()!=SyncMark.GONE?first:rest).add(one);
        first.addAll(rest);
        return first;
    }
    /** How many rounds a phone's line draws before the +N, and a PC's, which has the room for one more. */
    static final int PHONE_ROUNDS=3,PC_ROUNDS=4;

    record Person(String address,String name,SyncMark mark,boolean linked,String listedIn,Sharing.Rule listing,boolean linking,String says) {
        Person(String address,String name,SyncMark mark){this(address,name,mark,true,"",null,false,"");}
        Person(String address,String name,SyncMark mark,boolean linked,String listedIn,Sharing.Rule listing) {
            this(address,name,mark,linked,listedIn,listing,false,"");
        }
        Person(String address,String name,SyncMark mark,boolean linked,String listedIn,Sharing.Rule listing,boolean linking) {
            this(address,name,mark,linked,listedIn,listing,linking,"");
        }
        /** Where they stand, in the words a tap on their round shows: the writing and the files (see Waits.person). */
        String standing(){return says==null||says.isEmpty()?mark.person():says;}
        /** What a tap on their round says: who, then where they stand, in plain words. */
        String called(){return name.isEmpty()?Say.t("A paired device"):name;}
        /** What the round of somebody not linked says: being linked through the list and waiting for them, or not at all. */
        String notLinked(String here){return linking?Unsent.linking(called()):Unsent.notLinked(called(),listedIn,here);}
    }

    /**
     * What to call a device, the best thing known first: the name it was paired under here, then the name a list
     * of people gave it, then a short form of its key ("device 3F9A21") - never "?" or "A device", which leave
     * the owner asking which one.
     *
     * @param key     the key it signs with, as kept; empty where not known, and the address stands in
     */
    static String named(String paired,String listed,String key,String address) {
        if(paired!=null&&!paired.isBlank())return paired.trim();
        if(listed!=null&&!listed.isBlank())return listed.trim();
        String from=key!=null&&!key.isBlank()?key.trim():address==null?"":address.trim();
        if(from.isEmpty())return "";
        byte[] print=Sha3.of(from.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex=new StringBuilder("device ");
        for(int i=0;i<3;i++)hex.append(String.format(java.util.Locale.ROOT,"%02X",print[i]&0xff));
        return hex.toString();
    }

    /** Everybody a note reaches and where each stands with it, in the order they were given it. */
    static java.util.List<Person> who(NoteStore store,String id) {
        java.util.List<Person> out=new java.util.ArrayList<>();
        var each=store.whereEach(store.pagesUnder(NoteStore.Branch.Kind.PAGE,id)).get(id);
        java.util.Map<String,Waits.Where> where=each==null?java.util.Map.of():each;
        java.util.Map<String,SyncMark> owed=new java.util.LinkedHashMap<>();
        for(var one:where.entrySet())owed.put(one.getKey(),one.getValue().mark());
        // Everybody the note reaches, not only those this device sends to: on a note somebody else shared,
        // the one who shared it is sent nothing from here, and the line showed nobody at all. A device this one
        // does not send to is where this copy came from, so it has what is here.
        java.util.Set<String> seen=new java.util.LinkedHashSet<>(owed.keySet());
        var note=store.get(id);
        java.util.List<Sharing.Rule> rules=store.shares();
        // Along the whole path: a rule on any collection above it, however deep, reaches it.
        if(note!=null)seen.addAll(Sharing.audience(rules,store.pathOf(id)).keySet());
        // Linked through the list and not answered yet: nothing goes to them, so their round says so. See Linking.
        java.util.Set<String> linking=store.linkingNow();
        for(String address:seen) {
            NoteStore.Contact known=store.address(address);
            boolean waiting=known!=null&&known.paired()&&linking.contains(address);
            if(known!=null&&known.paired()&&!waiting) {
                String name=store.nameFor(address);
                out.add(new Person(address,name,owed.getOrDefault(address,SyncMark.GONE),true,"",null,false,
                    Waits.person(name.isEmpty()?Say.t("A paired device"):name,where.get(address),kept(address))));
                continue;
            }
            // Named in the list and never paired here: which list, so the round can say where to take them off.
            Sharing.Rule listing=null;
            for(Sharing.Rule rule:rules)if(rule.address.equals(address)&&Sharing.covers(rule,store.pathOf(id))){listing=rule;break;}
            String listedIn=listing==null?"":store.thingName(NoteStore.kindFor(listing.scope),listing.target);
            out.add(new Person(address,store.nameFor(address),owed.getOrDefault(address,SyncMark.GONE),false,listedIn,listing,waiting));
        }
        // The owner's own devices stay a round each: which of them has it is what their rounds are for.
        return folded(out,address->{NoteStore.Contact c=store.address(address);return c!=null&&c.mine?"":store.personOf(address);});
    }

    /**
     * One round per person, not per device (decision 163): the devices whose cards say they are one person's (see AboutMe),
     * folded into the first of them, which wears the mark that most needs a look among them and
     * says how many devices it stands for. Others, and anybody not linked, as they were.
     *
     * @param personOf whose a device is, by address: a person id, "mine" for one of the owner's own, empty for unknown
     */
    static java.util.List<Person> folded(java.util.List<Person> who,java.util.function.Function<String,String> personOf) {
        java.util.List<Person> out=new java.util.ArrayList<>();
        java.util.Map<String,Integer> at=new java.util.HashMap<>();java.util.Map<String,Integer> many=new java.util.HashMap<>();
        for(Person one:who) {
            String person=one.linked()?personOf.apply(one.address()):"";
            if(person==null||person.isEmpty()){out.add(one);continue;}
            Integer first=at.get(person);
            if(first==null){at.put(person,out.size());many.put(person,1);out.add(one);continue;}
            Person was=out.get(first);many.merge(person,1,Integer::sum);
            SyncMark mark=needier(was.mark(),one.mark());
            out.set(first,new Person(was.address(),was.name(),mark,true,"",null,false,mark==was.mark()?was.says():one.says()));
        }
        for(java.util.Map.Entry<String,Integer> one:at.entrySet()) {
            int n=many.get(one.getKey());if(n<2)continue;
            Person was=out.get(one.getValue());
            String name="mine".equals(one.getKey())?Say.t(Groups.MY_DEVICES):was.name();
            out.set(one.getValue(),new Person(was.address(),name,was.mark(),true,"",null,false,
                Say.t("{0} · {1} devices",was.standing(),String.valueOf(n))));
        }
        return out;
    }
    /** Of two marks, the one that needs a look more: stuck, then waiting to go, then sent, paused, only here, all there. */
    private static SyncMark needier(SyncMark a,SyncMark b){return rank(b)>rank(a)?b:a;}
    private static int rank(SyncMark m) {
        switch(m){case STUCK:return 5;case WAITING:return 4;case SENT:return 3;case PAUSED:return 2;case HERE:return 1;default:return 0;}
    }

    /**
     * How many answers to a device are kept here for want of a way to it (see Outbox.Answers): told by Post, which
     * keeps them, so that the words about a mark can say so. None where Post has kept nothing.
     */
    static volatile java.util.function.ToIntFunction<String> kept=address->0;
    private static int kept(String address){try{return Math.max(0,kept.applyAsInt(address));}catch(RuntimeException notNow){return 0;}}

    /**
     * What an amber mark is waiting for, in words, for the box a press on it opens: each device the thing reaches,
     * with its writing and its files added up over every note under it (see Waits).
     */
    static String waits(NoteStore store,NoteStore.Branch.Kind kind,String id) {
        var pages=store.pagesUnder(kind,id);
        java.util.Map<String,Waits.Where> sum=new java.util.LinkedHashMap<>();
        for(var note:store.whereEach(pages).values())
            for(var one:note.entrySet())sum.merge(one.getKey(),one.getValue(),Waits.Where::and);
        // And each collection in it that waits to go on its own - its look, its files - to a device that has to be
        // updated first: "Ana's phone needs to update Mininotes to receive this" (see Unsent.Why.NEEDS_UPDATE).
        for(var waiting:store.cartonsForUpdate(kind,id).values())
            for(String address:waiting)sum.merge(address,Waits.needsUpdate(),Waits.Where::and);
        // And a file shared on its own: owed to a device, or waiting for it to be updated to know files (decision 92). In Temp,
        // each file in it.
        java.util.List<String> files=new java.util.ArrayList<>();
        if(kind==NoteStore.Branch.Kind.FILE)files.add(id);
        if(kind==NoteStore.Branch.Kind.TEMP)
            for(var one:store.temporary(System.currentTimeMillis()))if(one.kind==NoteStore.Branch.Kind.FILE)files.add(one.id);
        if(!files.isEmpty()) {
            java.util.Set<String> old=store.beforeLoose();
            for(String file:files)for(var wait:store.sleevesOwed(file,false))
                sum.merge(wait.address,old.contains(wait.address)?Waits.needsUpdate():new Waits.Where(1,0,0,false,false),Waits.Where::and);
            // And, once it went, each device that has not said it has its bytes: "Waiting for … to confirm 1 file", as the mark
            // waits for (NoteStore.fileOwed), rather than the words about writing a file never has.
            for(String file:files)for(String address:store.toConfirm(file))sum.merge(address,new Waits.Where(0,1,1,false,false),Waits.Where::and);
        }
        java.util.List<Waits.Device> devices=new java.util.ArrayList<>();
        for(var one:sum.entrySet())devices.add(new Waits.Device(store.nameFor(one.getKey()),one.getValue(),kept(one.getKey())));
        return Waits.said(devices,kind==NoteStore.Branch.Kind.PAGE?1:pages.size());
    }

    /**
     * What waits to go, as the things themselves, each to be opened from the box a press on an amber mark opens (the owner,
     * 2026-10-14: "clicking the yellow arrow showing that something is not fully synced should give us the list of what they
     * are"): the notes under it owed to anybody, then the files shared on their own owed to anybody.
     */
    static java.util.List<NoteStore.Branch> waitingThings(NoteStore store,NoteStore.Branch.Kind kind,String id) {
        java.util.Map<String,NoteStore.Branch> out=new java.util.LinkedHashMap<>();
        for(var wait:store.owed(kind,id)) {
            if(out.containsKey(wait.page))continue;
            var note=store.get(wait.page);
            if(note!=null)out.put(note.id,new NoteStore.Branch(NoteStore.Branch.Kind.PAGE,note.id,note.book,note.heading(),"",0,0,false,note.colour));
        }
        // And each file of those notes still on its way to somebody, as the line "Waiting for … to confirm n files" counts them.
        for(var held:store.filesLacking(store.pagesUnder(kind,id)))
            if(!out.containsKey(held.id))out.put(held.id,new NoteStore.Branch(NoteStore.Branch.Kind.FILE,held.id,held.note,held.name,"",0,0,false));
        java.util.List<String> files=new java.util.ArrayList<>();
        if(kind==NoteStore.Branch.Kind.FILE)files.add(id);
        else if(kind==NoteStore.Branch.Kind.TEMP){for(var one:store.temporary(System.currentTimeMillis()))if(one.kind==NoteStore.Branch.Kind.FILE)files.add(one.id);}
        else if(kind==NoteStore.Branch.Kind.LIBRARY)files.add(null);
        for(String file:files)for(var wait:store.sleevesOwed(file,false)) {
            if(out.containsKey(wait.page))continue;
            var held=store.file(wait.page);
            if(held!=null)out.put(held.id,new NoteStore.Branch(NoteStore.Branch.Kind.FILE,held.id,held.note,held.name,"",0,0,false));
        }
        // And each that went and waits for a device to say it has it, as the line "Waiting for … to confirm 1 file" says.
        for(String file:files) {
            if(file==null||out.containsKey(file)||store.toConfirm(file).isEmpty())continue;
            var held=store.file(file);
            if(held!=null)out.put(held.id,new NoteStore.Branch(NoteStore.Branch.Kind.FILE,held.id,held.note,held.name,"",0,0,false));
        }
        return new java.util.ArrayList<>(out.values());
    }

    /** The mark on the line under a note's title: the worst of where its people stand (see SyncMark.of). */
    static SyncMark mark(NoteStore store,String id,java.util.List<Person> people,boolean unsaved) {
        java.util.List<SyncMark> all=new java.util.ArrayList<>();
        for(Person one:people)all.add(one.mark());
        return SyncMark.of(store.pausedHere(NoteStore.Branch.Kind.PAGE,id),store.sharedAtAll(NoteStore.Branch.Kind.PAGE,id),
            unsaved,SyncMark.worst(all));
    }

    /** The names of everybody a note reaches, in the order they were given it. */
    static java.util.List<String> names(NoteStore store,String id) {
        java.util.List<String> out=new java.util.ArrayList<>();
        var note=store.get(id);if(note==null)return out;
        for(String address:Sharing.audience(store.shares(),store.pathOf(id)).keySet()) {
            String name=store.nameFor(address);
            if(name.isEmpty())name="a paired device";
            if(!out.contains(name))out.add(name);
        }
        return out;
    }

    static State read(NoteStore store,String id) {
        var db=store.getReadableDatabase();db.beginTransaction();
        try {
            var note=store.get(id);
            if(note==null)return new State(0,0,UNAVAILABLE);
            var audience=Sharing.audience(store.shares(),store.pathOf(id));
            if(audience.isEmpty())return new State(note.updated,0,
                store.sharedAtAll(NoteStore.Branch.Kind.PAGE,id)?NOT_CONFIRMED:ONLY_HERE);
            if(!store.owed(NoteStore.Branch.Kind.PAGE,id).isEmpty())
                return new State(note.updated,0,WAITING);
            long latest=0;
            // Arrived somewhere and waiting there to be accepted: not synced with them yet, whatever they answered. See Asking.
            java.util.Set<String> asking=store.askedWaiting();
            for(String address:audience.keySet()) {
                if(asking.contains(Asking.key(address,id)))return new State(note.updated,0,MATCHING);
                try(var row=db.query("sent",new String[]{"revision","agreed","at"},"address=? AND page=?",
                    new String[]{address,id},null,null,null,"1")) {
                    if(!row.moveToFirst()||row.getLong(0)!=note.revision||row.getLong(1)!=note.revision)
                        return new State(note.updated,0,MATCHING);
                    latest=Math.max(latest,row.getLong(2));
                }
            }
            return new State(note.updated,latest,latest>0?CONFIRMED:NOT_CONFIRMED);
        } finally {db.setTransactionSuccessful();db.endTransaction();}
    }
}
