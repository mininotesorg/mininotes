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
                DesktopRecovery.save(store,note,"t".repeat(161),"Complete synthetic recovery 😀 "+"x".repeat(24001),null,null);
                context.getSharedPreferences("settings",0).edit().putString(Desktop.WHERE,Desktop.NOTE_SHOWN+note.id).putString("listenInTray","false").apply();
            } else {
                var drafts=DesktopRecovery.all(store);
                if(drafts.size()!=1||drafts.get(0).title().length()!=161||!drafts.get(0).body().equals("Complete synthetic recovery 😀 "+"x".repeat(24001)))throw new AssertionError("Packaged launch lost recovery writing");
                if(!store.get(drafts.get(0).id()).title.equals("Synthetic package check"))throw new AssertionError("Saved note changed");
            }
            System.out.println("Packaged "+Desktop.VERSION+" "+args[0]+": one complete recovery draft; source="+Desktop.class.getProtectionDomain().getCodeSource().getLocation());
        }
    }
}
