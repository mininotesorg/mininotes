package org.mininotes.android;

/** Commit against the current row, preserving anything received while typing. */
final class DesktopEdits {
    static String limits(String title,String body) {
        if(title.length()>160)return "Title has "+title.length()+" characters; the limit is 160.";
        if(body.length()>24000)return "Note text has "+body.length()+" characters; the limit is 24,000.";
        return null;
    }
    static String warning(String title,String body) {
        String over=limits(title,body);if(over!=null)return over;
        if(title.length()>=144)return "Title: "+title.length()+" of 160 characters.";
        if(body.length()>=21600)return "Note text: "+body.length()+" of 24,000 characters.";
        return null;
    }
    static String refusal(NoteStore store,NoteStore.Note base,String title,String body) {
        NoteStore.Note current=store.get(base.id);
        if(current==null)return "This note no longer exists. Your writing can be kept on this PC.";
        if(Boolean.TRUE.equals(store.readOnlyHere(base.id)[0]))return "This note is now read only. Your writing can be kept on this PC.";
        String reason=limits(title,body);if(reason!=null)return reason;
        return limits(Merge.merge(base.title,title,current.title).text,Merge.merge(base.body,body,current.body).text);
    }
    static NoteStore.Note save(NoteStore store,NoteStore.Note base,String title,String body){return save(store,base,title,body,null);}

    /**
     * @param runs who wrote each letter of {@code body} (see Writers), or null where the page does not know: what is
     *             new is then nobody's. Put together with what arrived while typing, each letter keeps its writer.
     */
    static NoteStore.Note save(NoteStore store,NoteStore.Note base,String title,String body,Writers runs) {
        var db=store.getWritableDatabase();db.beginTransaction();
        try {
            NoteStore.Note current=store.get(base.id);
            if(current==null)throw new IllegalStateException("This note no longer exists. Copy your writing before leaving this page.");
            if(Boolean.TRUE.equals(store.readOnlyHere(base.id)[0]))
                throw new IllegalStateException("This note is now read only. Copy your unsaved writing before leaving this page.");
            String reason=limits(title,body);if(reason!=null)throw new IllegalStateException(reason);
            NoteStore.Note next=current.copy();
            next.body=Merge.merge(base.body,body,current.body).text;
            next.title=Merge.merge(base.title,title,current.title).text;
            reason=limits(next.title,next.body);if(reason!=null)throw new IllegalStateException(reason);
            // The page's letters keep the page's writers; what only the notebook had keeps its own.
            next.writers=runs==null?null:Writers.follow(next.body,Writers.UNKNOWN,body,runs,current.body,store.writersOf(current.id,current.body));
            if(!next.body.equals(current.body)||!next.title.equals(current.title)) {
                store.keepVersion(current.id,"");
                next.revision=current.revision+1;next.updated=System.currentTimeMillis();
                store.save(next);
            }
            db.setTransactionSuccessful();return next;
        } finally{db.endTransaction();}
    }
}
