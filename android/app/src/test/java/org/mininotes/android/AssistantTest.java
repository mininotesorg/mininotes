package org.mininotes.android;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

/** The Agent (decision 191): what a line is understood to ask, in English and French, and the help it finds. */
public class AssistantTest {
    private static Assistant.Asked u(String said){return Assistant.understand(said);}

    @Test public void tasksAreUnderstoodInEnglishAndFrench() {
        assertEquals(Assistant.Act.WHAT_NEW,u("What's new?").act());
        assertEquals(Assistant.Act.WHAT_NEW,u("quoi de neuf ?").act());
        assertEquals(Assistant.Act.WHAT_NEW,u("Qu'est-ce qui a changé").act());
        assertEquals(Assistant.Act.TODO_SHOW,u("What is on my todo list?").act());
        assertEquals(Assistant.Act.TODO_SHOW,u("my to do list").act());
        assertEquals(Assistant.Act.TODO_SHOW,u("Qu'y a-t-il sur ma liste de tâches ?").act());

        Assistant.Asked add=u("Add Buy bread to my todo list");
        assertEquals(Assistant.Act.TODO_ADD,add.act());assertEquals("Buy bread",add.what());assertEquals("",add.who());
        Assistant.Asked ajoute=u("Ajoute acheter du pain à ma liste");
        assertEquals(Assistant.Act.TODO_ADD,ajoute.act());assertEquals("acheter du pain",ajoute.what());
        assertEquals("Café crème",u("todo: Café crème").what());
        Assistant.Asked toNote=u("add milk to Grocery");
        assertEquals(Assistant.Act.TODO_ADD,toNote.act());assertEquals("milk",toNote.what());assertEquals("Grocery",toNote.who());

        Assistant.Asked share=u("Share Grocery with Carol");
        assertEquals(Assistant.Act.SHARE,share.act());assertEquals("Grocery",share.what());assertEquals("Carol",share.who());assertFalse(share.readOnly());
        Assistant.Asked partage=u("partage Courses avec Carole en lecture seule");
        assertEquals(Assistant.Act.SHARE,partage.act());assertEquals("Courses",partage.what());assertEquals("Carole",partage.who());assertTrue(partage.readOnly());
        assertEquals(Assistant.Act.SHARE,u("send the Work folder to Bob").act());

        assertEquals(Assistant.Act.ADD_CONTACT,u("Add a contact").act());
        assertEquals(Assistant.Act.ADD_CONTACT,u("ajoute un contact").act());
        assertEquals(Assistant.Act.ADD_CONTACT,u("add someone").act());
        assertEquals(Assistant.Act.DOCK,u("put Grocery in the dock").act());
        assertEquals(Assistant.Act.FAVOURITE,u("ajoute Courses aux favoris").act());
        Assistant.Asked made=u("New note called Holiday plans");
        assertEquals(Assistant.Act.NEW_NOTE,made.act());assertEquals("Holiday plans",made.what());
        assertEquals(Assistant.Act.OPEN,u("open Grocery").act());assertEquals("Grocery",u("open Grocery").what());
        assertEquals(Assistant.Act.FIND,u("find soup").act());
        assertEquals(Assistant.Act.NOTHING,u("   ").act());
    }

    @Test public void questionsAboutTheAppAreQuestions() {
        assertEquals(Assistant.Act.HELP,u("What is remove from the dock?").act());
        assertEquals(Assistant.Act.HELP,u("How do I open a file with another program?").act());
        assertEquals(Assistant.Act.HELP,u("why is my note not syncing").act());
    }

    @Test public void theHelpAnswersTheQuestionsItWasWrittenFor() {
        List<Assistant.Help> en=Assistant.help("en"),fr=Assistant.help("fr");
        assertTrue("the help is read",en.size()>=30);
        assertEquals("the French help has every answer the English has",en.size(),fr.size());
        for(int i=0;i<en.size();i++)assertEquals(en.get(i).id(),fr.get(i).id());
        for(Assistant.Help one:en){assertFalse(one.id()+" has an answer",one.answer().isEmpty());assertFalse(one.id()+" has ways of asking",one.asks().isEmpty());
            assertFalse(one.id()+": no long dash",one.answer().contains("—")||one.answer().contains(" - "));}
        assertEquals("dock",Assistant.lookUp("What is remove from the dock?",en,3).get(0).id());
        assertEquals("marks",Assistant.lookUp("what does the green tick mean",en,3).get(0).id());
        assertEquals("notsyncing",Assistant.lookUp("my grocery note is not syncing",en,3).get(0).id());
        assertEquals("filelink",Assistant.lookUp("how do I keep a spreadsheet in sync",en,3).get(0).id());
        assertEquals("dock",Assistant.lookUp("à quoi sert le dock",fr,3).get(0).id());
        assertEquals("lock",Assistant.lookUp("comment chiffrer mes notes avec un mot de passe",fr,3).get(0).id());
        assertTrue("nothing for nonsense",Assistant.lookUp("zzz qqq",en,3).isEmpty());
        // A language with no help of its own is answered in English.
        assertEquals(en.size(),Assistant.help("ja").size());
    }

    @Test public void namesAreFoundAsTheyAreCalled() {
        List<String> names=List.of("Grocery","Groceries for Sunday","Épicerie","Work");
        assertEquals("Grocery",Assistant.best("grocery",names,n->n));
        assertEquals("Épicerie",Assistant.best("epicerie",names,n->n));
        assertEquals("Work",Assistant.best("the work",names,n->n));
        assertNull(Assistant.best("holiday",names,n->n));
        assertTrue(Assistant.isTodo("To do"));assertTrue(Assistant.isTodo("कार्य"));assertTrue(Assistant.isTodo("やること"));assertTrue(Assistant.isTodo("Дела"));assertTrue(Assistant.isTodo("À faire"));assertFalse(Assistant.isTodo("Grocery"));
        assertEquals(List.of("bread","milk","eggs"),Assistant.lines("- bread\n\n[ ] milk\n1. eggs\n"));
        assertEquals("- bread\n- milk",Assistant.withLine("- bread","milk"));
        assertEquals("bread\nmilk",Assistant.withLine("bread\n","milk"));
    }
}
