package org.mininotes.android;

import java.nio.file.*;
import org.mininotes.desktop.platform.content.Context;

/** Runs against the packaged jars, before/after an isolated offline executable launch. */
public final class DesktopPackageProbe {
    public static void main(String[] args) throws Exception {
        Path folder=Path.of(args[1]).toAbsolutePath();
        if(!folder.getFileName().toString().startsWith("synthetic-package-"))throw new IllegalArgumentException("Synthetic notebook required");
        Context context=new Context(folder.toFile());
        try(NoteStore store=new NoteStore(context)) {
            if(args[0].equals("prepare")) {
                NoteStore.Note note=new NoteStore.Note();note.title="Synthetic package check";note.body="Only synthetic writing. The owner's notebook is separate.";note.book=Things.HOME;store.save(note);
                // A draft as 0.4.055 and older kept one: the packaged app makes it a note at its start (decision 328).
                DesktopRecovery.keep(store,note,"t".repeat(161),"Complete synthetic recovery 😀 "+"x".repeat(24001),null,"long");
                context.getSharedPreferences("settings",0).edit().putString(Desktop.WHERE,Desktop.NOTE_SHOWN+note.id).putString("listenInTray","false").apply();
            } else {
                if(!DesktopRecovery.all(store).isEmpty())throw new AssertionError("Packaged launch kept recovery writing aside");
                // The title's line, then the words in two, as one note holds at most 24,000 letters: three notes.
                java.util.Map<String,String> made=new java.util.HashMap<>();
                for(NoteStore.Note row:store.pages(Things.HOME))if(row.title.startsWith("t".repeat(20)))made.put(row.title.substring(row.title.lastIndexOf('(')),store.get(row.id).body);
                if(made.size()!=3||!(made.get("(copy)")+made.get("(continued)")+made.get("(continued 2)")).equals("t".repeat(161)+"\nComplete synthetic recovery 😀 "+"x".repeat(24001)))throw new AssertionError("Packaged launch lost recovery writing");
            }
            System.out.println("Packaged "+Desktop.VERSION+" "+args[0]+": recovery writing made notes; source="+Desktop.class.getProtectionDomain().getCodeSource().getLocation());
        }
    }
}
