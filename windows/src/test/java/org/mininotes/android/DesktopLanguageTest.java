package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;

/**
 * A language chosen on the PC (decision 160): kept beside the notebook for the next start, and every language's name
 * drawn in a face that has its letters (the owner, 2026-10-12: Hindi showed as boxes in the Language menu).
 */
public class DesktopLanguageTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void theLanguageChosenIsReadFromBesideTheNotebook() throws Exception {
        Path folder=temp.newFolder().toPath();
        assertEquals(Say.SYSTEM,Desktop.languageChosen(folder));
        Files.writeString(folder.resolve(Desktop.LANGUAGE_FILE),"fr\n");
        assertEquals("fr",Desktop.languageChosen(folder));
    }

    @Test public void everyLanguagesNameIsDrawnWithItsOwnLetters() {
        Font usual=new Font("Segoe UI",Font.PLAIN,14);
        for(String name:Say.LANGUAGES.values()) {
            Font face=DesktopUi.fontFor(name,usual);
            assertEquals(name+" in "+face.getFamily(),-1,face.canDisplayUpTo(name));
        }
        assertEquals("Segoe UI",DesktopUi.family("fr"));
    }
}
