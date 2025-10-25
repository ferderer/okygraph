package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class KeywordIdentifierPatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testKeywordMatches() {
        String[][] tests = {
            {"class", "0", "class"},
            {"public", "0", "public"},
            {"true", "0", "true"},
            {"if", "0", "if"},
            {"record", "0", "record"},
            {"x class", "2", "class"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.KEYWORD, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testKeywordNoMatch() {
        Matcher m = matcher(TokenPattern.KEYWORD, "className", 0);
        assertFalse(m.lookingAt(), "Should not match: className");
    }

    @Test
    void testIdentifierMatches() {
        String[][] tests = {
            {"variable", "0", "variable"},
            {"_private", "0", "_private"},
            {"$dollar", "0", "$dollar"},
            {"müller", "0", "müller"},
            {"Übung", "0", "Übung"},
            {"変数", "0", "変数"},
            {"тест", "0", "тест"},
            {"className123", "0", "className123"},
            {"x+y", "0", "x"},
            {"foo()", "0", "foo"},
            {"bar;", "0", "bar"},
            {"obj.method", "0", "obj"},
            {"arr[i]", "0", "arr"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.IDENTIFIER, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testIdentifierNoMatch() {
        String[][] tests = {
            {"123abc", "0"},  // starts with digit
            {"+var", "0"},    // starts with operator
            {"(test)", "0"},  // starts with separator
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.IDENTIFIER, test[0], Integer.parseInt(test[1]));
            assertFalse(m.lookingAt(), "Should not match: " + test[0]);
        }
    }

    @Test
    void testWhitespaceMatches() {
        String[][] tests = {
            {"   ", "0", "   "},
            {"\t\t", "0", "\t\t"},
            {" \t ", "0", " \t "},
            {"x   y", "1", "   "},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.WHITESPACE, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }
}
