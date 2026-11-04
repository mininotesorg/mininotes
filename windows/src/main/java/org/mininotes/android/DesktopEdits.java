package org.mininotes.android;

/** Commit against the current row, preserving anything received while typing. */
final class DesktopEdits {
    /** Said in the language in use, as the phone says it (decision 282; see NoteLimit). */
    static String limits(String title,String body){return NoteLimit.over(title.length(),body);}
    /** The line near a limit: the whole note, and the tab open, which is {@code tab} letters of it (decision 319). */
    static String warning(String title,String body,int tab){return NoteLimit.near(title.length(),body.length(),tab,Sections.of(body).size());}
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
