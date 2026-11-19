package group18.listener;

import group18.listener.TextDiff.Change;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TextDiff measures how much source text changed")
class TextDiffTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\": {2} removed, {3} inserted")
    @CsvSource({
            "abc, abXc, 0, 1",
            "abc, aXc, 1, 1",
            "abc, ac, 1, 0",
            "'', abc, 0, 3",
            "abc, '', 3, 0",
            "aaa, aaaa, 0, 1",
            "class A {}, class A { int x; }, 0, 8"
    })
    void countsRemovedAndInsertedCharacters(String before, String after, int removed, int inserted) {
        assertEquals(new Change(removed, inserted), TextDiff.between(before, after));
    }

    @Test
    void identicalTextIsNoChange() {
        Change change = TextDiff.between("same", "same");
        assertEquals(Change.NONE, change);
        assertTrue(change.isEmpty());
    }

    @Test
    void nullIsTreatedAsEmpty() {
        assertEquals(new Change(0, 2), TextDiff.between(null, "ab"));
        assertEquals(new Change(2, 0), TextDiff.between("ab", null));
        assertFalse(TextDiff.between(null, "ab").isEmpty());
    }
}
