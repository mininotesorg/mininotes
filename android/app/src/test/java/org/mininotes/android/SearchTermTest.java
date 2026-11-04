package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/** When a search box looks (decision 287): one character where one character is a word, two otherwise. */
public class SearchTermTest {
    @Test public void oneChineseJapaneseOrKoreanCharacterIsEnough() {
        assertTrue(SearchTerm.enough("喷"));
        assertTrue(SearchTerm.enough(" 茶 "));
        assertTrue(SearchTerm.enough("ね"));
        assertTrue(SearchTerm.enough("カ"));
        assertTrue(SearchTerm.enough("한"));
    }

    @Test public void oneLetterElsewhereIsNot() {
        assertFalse(SearchTerm.enough("a"));
        assertFalse(SearchTerm.enough("é"));
        assertFalse(SearchTerm.enough("ب"));
        assertFalse(SearchTerm.enough("😀"));
        assertFalse(SearchTerm.enough(""));
        assertFalse(SearchTerm.enough("   "));
        assertFalse(SearchTerm.enough(null));
    }

    @Test public void twoOfAnythingAre() {
        assertTrue(SearchTerm.enough("ab"));
        assertTrue(SearchTerm.enough("喷嘴"));
        assertTrue(SearchTerm.enough("😀😀"));
    }
}
