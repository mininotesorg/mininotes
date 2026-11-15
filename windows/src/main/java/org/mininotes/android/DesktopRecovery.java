package org.mininotes.android;

import java.util.*;
import org.json.*;

/**
 * Writing a note could not take is made into ordinary notes (decision 328; the owner, 2026-11-05: "For me everything is
 * saved directly, there is no difference between draft and other notes"). Until 0.4.056 the PC kept it aside here as a
 * "recovery draft", in its own table, outside notes and sync; drafts still there from then are made notes at the start
 * (see {@link #adopt}). The table stays only for them, and for nothing new.
 */
final class DesktopRecovery {
    record Draft(String id,String token,String title,String body,NoteStore.Note base,Writers writers,String reason) {}
    /**
     * What a save came to: the note written in, or null where it could not be (gone, read only, or what arrived with it too
     * long); the notes made for what it could not take, in order; and why, or null where it took it all.
     */
    record Result(NoteStore.Note note,List<NoteStore.Note> made,String reason) {}
    private static void table(NoteStore store) {
        store.getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS desktop_recovery (id TEXT NOT NULL, token TEXT PRIMARY KEY, writing TEXT NOT NULL)");
    }
    /**
     * The page written down, all of it, one transaction. Too long for the note: what fits goes into it, at line ends, and the
     * rest into "Plan (continued)" notes beside it. The note gone or read only, or what arrived with it too long: the note
     * is left as it is and the writing becomes "Plan (copy)", on Home, the rest after it as before.
     */
    static Result save(NoteStore store,NoteStore.Note base,String title,String body,Writers writers) {
        var db=store.getWritableDatabase();db.beginTransaction();
        try {
            Result result;
            // A validation refusal happens before any note/version write. Do not nest a failed transaction.
            String refusal=DesktopEdits.refusal(store,base,title,body);
            if(refusal==null)result=new Result(DesktopEdits.save(store,base,title,body,writers),List.of(),null);
            else {
                NoteStore.Note current=store.get(base.id);
                boolean writable=current!=null&&!Boolean.TRUE.equals(store.readOnlyHere(base.id)[0]);
                List<NoteSplit.Piece> pieces=NoteSplit.pieces(title,body);
                Writers all=writers!=null&&writers.length()==body.length()?writers:null;
                NoteSplit.Piece first=pieces.get(0);
                if(writable&&DesktopEdits.limits(title,body)!=null&&DesktopEdits.refusal(store,base,first.title,first.body)==null) {
                    NoteStore.Note saved=DesktopEdits.save(store,base,first.title,first.body,first.writers(all));
                    result=new Result(saved,made(store,pieces.subList(1,pieces.size()),saved.book,saved.title,false,all),refusal);
                } else result=new Result(null,made(store,pieces,writable?current.book:Things.HOME,title,true,all),refusal);
            }
            db.setTransactionSuccessful();return result;
        } finally {db.endTransaction();}
    }
    /** Each piece a new note in {@code book}: the first "(copy)" where it is a copy, the others "(continued)", counted. */
    private static List<NoteStore.Note> made(NoteStore store,List<NoteSplit.Piece> pieces,String book,String title,boolean copy,Writers all) {
        List<NoteStore.Note> out=new ArrayList<>();
        for(int i=0;i<pieces.size();i++) {
            NoteSplit.Piece one=pieces.get(i);
            NoteStore.Note note=new NoteStore.Note();note.book=book==null?Things.HOME:book;
            String name=one.title.isEmpty()?title:one.title;
            note.title=copy&&i==0?NoteSplit.copyTitle(NoteSplit.cut(name,NoteLimit.TITLE),1,true):NoteSplit.copyTitle(NoteSplit.cut(name,NoteLimit.TITLE),copy?i:i+1,false);
            note.body=one.body;note.writers=one.writers(all);note.updated=System.currentTimeMillis();
            store.save(note);out.add(note);
        }
        return out;
    }
    /**
     * Drafts kept before 0.4.056 made notes, each on Home as "(copy)" with the rest after it: so nothing is kept aside any
     * more. Said once at the start; how many notes were made.
     */
    static List<NoteStore.Note> adopt(NoteStore store) {
        List<Draft> drafts=all(store);
        if(drafts.isEmpty())return List.of();
        var db=store.getWritableDatabase();db.beginTransaction();
        try {
            List<NoteStore.Note> out=new ArrayList<>();
            for(Draft d:drafts) {
                Writers all=d.writers!=null&&d.writers.length()==d.body.length()?d.writers:null;
                out.addAll(made(store,NoteSplit.pieces(d.title,d.body),Things.HOME,d.title,true,all));
                discard(store,d.id,d.token);
            }
            db.setTransactionSuccessful();return out;
        } finally {db.endTransaction();}
    }
    static List<Draft> all(NoteStore store) {
        table(store);List<Draft> out=new ArrayList<>();
        try(var c=store.getReadableDatabase().rawQuery("SELECT writing FROM desktop_recovery ORDER BY rowid",null)) {
            while(c.moveToNext())out.add(read(new JSONObject(c.getString(0))));
        }return out;
    }
    static Draft get(NoteStore store,String id){Draft latest=null;for(Draft d:all(store))if(d.id.equals(id))latest=d;return latest;}
    static void discard(NoteStore store,String id,String token){table(store);store.getWritableDatabase().delete("desktop_recovery","id=? AND token=?",new String[]{id,token});}
    /** A draft as 0.4.055 kept it: for the tests, which make one as an older PC had. */
    static Draft keep(NoteStore store,NoteStore.Note base,String title,String body,Writers writers,String reason) {
        table(store);
        Draft draft=new Draft(base.id,UUID.randomUUID().toString(),title,body,base.copy(),writers==null?null:writers.copy(),reason);
        store.getWritableDatabase().execSQL("INSERT OR REPLACE INTO desktop_recovery(id,token,writing) VALUES(?,?,?)",new Object[]{draft.id,draft.token,json(draft).toString()});
        return draft;
    }
    private static JSONObject json(Draft d){return new JSONObject().put("id",d.id).put("token",d.token).put("title",d.title).put("body",d.body)
        .put("baseTitle",d.base.title).put("baseBody",d.base.body).put("revision",d.base.revision).put("book",d.base.book)
        .put("writers",d.writers==null?"":Objects.toString(d.writers.write(),"")).put("reason",d.reason);}
    private static Draft read(JSONObject o){NoteStore.Note base=new NoteStore.Note();base.id=o.getString("id");base.title=o.getString("baseTitle");base.body=o.getString("baseBody");base.revision=o.getLong("revision");base.book=o.getString("book");
        String body=o.getString("body");return new Draft(base.id,o.getString("token"),o.getString("title"),body,base,Writers.read(o.getString("writers"),body.length()),o.getString("reason"));}
}
