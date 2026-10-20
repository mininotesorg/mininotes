package org.mininotes.android;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import org.json.*;
import org.mininotes.desktop.platform.AtomicFile;

/** Local writing, outside the shared schema and all note/sync queries. Uses the notebook's encryption. */
final class DesktopRecovery {
    static final String KEPT="Writing kept on this PC. Shorten it to save as a note";
    record Draft(String id,String token,String title,String body,NoteStore.Note base,Writers writers,String reason) {
        String text(){return title+"\n\n"+body;}
    }
    record Result(NoteStore.Note note,Draft draft) {}
    private static void table(NoteStore store) {
        store.getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS desktop_recovery (id TEXT NOT NULL, token TEXT PRIMARY KEY, writing TEXT NOT NULL)");
    }
    static Draft keep(NoteStore store,NoteStore.Note base,String title,String body,Writers writers,String reason) {
        table(store);
        Draft draft=new Draft(base.id,UUID.randomUUID().toString(),title,body,base.copy(),writers==null?null:writers.copy(),reason);
        store.getWritableDatabase().execSQL("INSERT OR REPLACE INTO desktop_recovery(id,token,writing) VALUES(?,?,?)",new Object[]{draft.id,draft.token,json(draft).toString()});
        return draft;
    }
    /** The transaction returns only once the complete writing, or the ordinary note, has committed. */
    static Result save(NoteStore store,NoteStore.Note base,String title,String body,Writers writers,String expected) {
        table(store);var db=store.getWritableDatabase();db.beginTransaction();
        try {
            Result result;
            // A validation refusal happens before any note/version write. Do not nest a failed transaction.
            String refusal=DesktopEdits.refusal(store,base,title,body);
            if(refusal!=null){Draft kept=keep(store,base,title,body,writers,refusal);if(expected!=null)discard(store,base.id,expected);result=new Result(null,kept);}
            else {
                NoteStore.Note saved=DesktopEdits.save(store,base,title,body,writers);
                if(expected!=null)discard(store,base.id,expected);
                result=new Result(saved,null);
            }
            db.setTransactionSuccessful();return result;
        } finally {db.endTransaction();}
    }
    static List<Draft> all(NoteStore store) {
        table(store);List<Draft> out=new ArrayList<>();
        try(var c=store.getReadableDatabase().rawQuery("SELECT writing FROM desktop_recovery ORDER BY rowid",null)) {
            while(c.moveToNext())out.add(read(new JSONObject(c.getString(0))));
        }return out;
    }
    static Draft get(NoteStore store,String id){Draft latest=null;for(Draft d:all(store))if(d.id.equals(id))latest=d;return latest;}
    static Draft token(NoteStore store,String token){for(Draft d:all(store))if(d.token.equals(token))return d;return null;}
    static void discard(NoteStore store,String id,String token){table(store);store.getWritableDatabase().delete("desktop_recovery","id=? AND token=?",new String[]{id,token});}
    static void export(Draft draft,Path target) throws Exception {AtomicFile.write(target,draft.text().getBytes(StandardCharsets.UTF_8));}
    static String backup(NoteStore store){JSONArray a=new JSONArray();for(Draft d:all(store))a.put(json(d));return new JSONObject().put("format","mininotes.desktop-writing.v1").put("drafts",a).toString();}
    /** Adds alongside current recovery writing, never replaces it. Different snapshots get distinct local identities. */
    static int restore(NoteStore store,String text) {
        JSONObject o=new JSONObject(text);if(!"mininotes.desktop-writing.v1".equals(o.getString("format")))throw new IllegalArgumentException("Unknown writing backup format");
        List<Draft> parsed=new ArrayList<>();JSONArray a=o.getJSONArray("drafts");for(int i=0;i<a.length();i++)parsed.add(read(a.getJSONObject(i)));
        table(store);var db=store.getWritableDatabase();db.beginTransaction();
        try {for(Draft d:parsed) {
            db.execSQL("INSERT OR IGNORE INTO desktop_recovery(id,token,writing) VALUES(?,?,?)",new Object[]{d.id,d.token,json(d).toString()});
        }db.setTransactionSuccessful();return parsed.size();}finally{db.endTransaction();}
    }
    private static JSONObject json(Draft d){return new JSONObject().put("id",d.id).put("token",d.token).put("title",d.title).put("body",d.body)
        .put("baseTitle",d.base.title).put("baseBody",d.base.body).put("revision",d.base.revision).put("book",d.base.book)
        .put("writers",d.writers==null?"":Objects.toString(d.writers.write(),"")).put("reason",d.reason);}
    private static Draft read(JSONObject o){NoteStore.Note base=new NoteStore.Note();base.id=o.getString("id");base.title=o.getString("baseTitle");base.body=o.getString("baseBody");base.revision=o.getLong("revision");base.book=o.getString("book");
        String body=o.getString("body");return new Draft(base.id,o.getString("token"),o.getString("title"),body,base,Writers.read(o.getString("writers"),body.length()),o.getString("reason"));}
}
