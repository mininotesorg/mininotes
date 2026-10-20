package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;

/** What both apps share for Translate… (docs/TRANSLATION.md): packs, the list, the engine's settings, a guess, lines kept. */
public class TranslationTest {
    @Test public void throughEnglishWhereNeitherSideIsEnglish() {
        assertEquals(List.of(new Translation.Pair("fr","en"),new Translation.Pair("en","zh")),Translation.steps("fr","zh"));
        assertEquals(List.of(new Translation.Pair("en","es")),Translation.steps("en","es"));
        assertTrue(Translation.steps("fr","fr").isEmpty());
        // Chinese is zh in the app, zh-Hans in Mozilla's packs.
        assertEquals("enzh-Hans",new Translation.Pair("en","zh").folder());
        assertEquals("zh-Hansen",new Translation.Pair("zh","en").folder());
        assertEquals("fren",new Translation.Pair("fr","en").folder());
        // Every language the app speaks can be translated.
        for(String code:Say.LANGUAGES.keySet())assertTrue(code,Translation.PACKS.containsKey(code));
    }

    @Test public void theListOfPacksTakesOnlyWholeLines() {
        String sha="a".repeat(64);
        List<Translation.File> all=Translation.manifest("# a comment\n\nfren\tmodel.fren.bin\t31561787\t"+sha+"\r\nfren\t../escape\t1\t"+sha+"\nenfr\tvocab.enfr.spm\tlots\t"+sha+"\nzh-Hansen\tlex.zhen.bin\t9\t"+sha+"\n");
        assertEquals(2,all.size());
        assertEquals(new Translation.File("fren","model.fren.bin",31561787,sha),all.get(0));
        assertEquals(1,Translation.filesOf(all,new Translation.Pair("zh","en")).size());
    }

    @Test public void theEngineIsToldEveryFileOfThePack() {
        String one=Translation.config("C:\\p\\fren",List.of("lex.50.50.fren.s2t.bin","model.fren.intgemm.alphas.bin","vocab.fren.spm"));
        assertTrue(one.contains("  - C:/p/fren/model.fren.intgemm.alphas.bin\n"));
        assertTrue(one.contains("vocabs:\n  - C:/p/fren/vocab.fren.spm\n  - C:/p/fren/vocab.fren.spm\n"));
        String two=Translation.config("p",List.of("lex.enzh.bin","model.enzh.bin","srcvocab.enzh.spm","trgvocab.enzh.spm"));
        assertTrue(two.contains("vocabs:\n  - p/srcvocab.enzh.spm\n  - p/trgvocab.enzh.spm\n"));
        assertNull("a pack without its model is not whole",Translation.config("p",List.of("lex.x","vocab.x")));
    }

    @Test public void aFirstGuessAtTheLanguage() {
        assertEquals("fr",Translation.guess("Nous allons au marché demain pour acheter du pain et des légumes."));
        assertEquals("es",Translation.guess("¿Vamos a la playa mañana con los niños?"));
        assertEquals("de",Translation.guess("Wir gehen morgen mit den Kindern auf den Markt."));
        // Turkish (the owner, 2026-10-13), a language to translate though the app is not written in it.
        assertEquals("tr",Translation.guess("Yarın çocuklarla birlikte pazara gideceğiz ve ekmek alacağız."));
        assertEquals("Türkçe",Translation.name("tr"));assertEquals("Français",Translation.name("fr"));
        assertEquals("entr",Translation.steps("en","tr").get(0).folder());
        assertEquals(List.of(new Translation.Pair("tr","en"),new Translation.Pair("en","fr")),Translation.steps("tr","fr"));
        assertEquals("it",Translation.guess("Domani andiamo al mercato con i bambini, non è lontano."));
        assertEquals("pt",Translation.guess("Amanhã vamos ao mercado com as crianças, não é longe."));
        assertEquals("en",Translation.guess("We are going to the market tomorrow with the kids."));
        assertEquals("zh",Translation.guess("明天早上我们去市场买蔬菜。"));
        assertEquals("ja",Translation.guess("明日の朝、市場に行きます。"));
        assertEquals("hi",Translation.guess("कल सुबह हम बाज़ार जाएंगे।"));
        assertEquals("fa",Translation.guess("فردا صبح به بازار می‌رویم."));
        assertEquals("ru",Translation.guess("Завтра утром мы пойдём на рынок."));
        assertEquals("en",Translation.guess("   "));
    }

    @Test public void listsMarksAndEmptyLinesComeThrough() {
        String note="Courses\n\n- Du pain\n- [ ] Des oeufs\n**Important**\n1. Appeler Marie  ";
        Translation.Prepared prepared=Translation.prepare(note);
        assertEquals("Courses\n\nDu pain\n\nDes oeufs\n\nImportant\n\nAppeler Marie",prepared.input());
        // The engine's paragraphs, with Windows' line ends, put back line for line.
        String back=Translation.restore(prepared,"Shopping\r\n\r\nBread\r\n\r\nEggs\r\n\r\nImportant\r\n\r\nCall Marie\r\n");
        assertEquals("Shopping\n\n- Bread\n- [ ] Eggs\n**Important**\n1. Call Marie  ",back);
        assertEquals("",Translation.prepare("\n\n").input());
    }
}
