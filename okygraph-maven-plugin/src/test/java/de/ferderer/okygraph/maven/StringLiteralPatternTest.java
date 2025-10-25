package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringLiteralPatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testStringLiteralType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.STRING.type);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "\"simple string\"|0|\"simple string\"",
        "\"\"|0|\"\"",
        "x\"test\"|1|\"test\"",
    })
    void testStringLiteralMatches(String input, int start, String expected) {
        Matcher m = matcher(TokenPattern.STRING, input, start);
        assertTrue(m.lookingAt(), "Should match at position " + start + ": " + input);
        assertEquals(expected, m.group());
    }

    @Test
    void testCharLiterals() {
        String[][] tests = {
            {"'a'", "0", "'a'"},
            {"code'c'", "4", "'c'"}
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.STRING, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: `" + test[0] + "`");
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testStringWithEscapedQuote() {
        String input = "\"escaped \\\"quote\\\"\"";
        Matcher m = matcher(TokenPattern.STRING, input, 0);
        assertTrue(m.lookingAt());
        assertEquals("\"escaped \\\"quote\\\"\"", m.group());
    }

    @Test
    void testStringWithEscapedBackslash() {
        String input = "\"escaped \\\\backslash\"";
        Matcher m = matcher(TokenPattern.STRING, input, 0);
        assertTrue(m.lookingAt());
        assertEquals("\"escaped \\\\backslash\"", m.group());
    }

    @Test
    void testCharWithEscapedQuote() {
        String input = "'\\\"'";
        Matcher m = matcher(TokenPattern.STRING, input, 0);
        assertTrue(m.lookingAt());
        assertEquals("'\\\"'", m.group());
    }

    @Test
    void testCharWithEscapedBackslash() {
        String input = "'\\\\'";
        Matcher m = matcher(TokenPattern.STRING, input, 0);
        assertTrue(m.lookingAt());
        assertEquals("'\\\\'", m.group());
    }

    @Test
    void testCharWithEscapedNewline() {
        String input = "'\\n'";
        Matcher m = matcher(TokenPattern.STRING, input, 0);
        assertTrue(m.lookingAt());
        assertEquals("'\\n'", m.group());
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "\"unclosed|0",
        "'ab'|0",
        "x\"test\"|0",
    })
    void testStringLiteralNoMatch(String input, int start) {
        Matcher m = matcher(TokenPattern.STRING, input, start);
        assertFalse(m.lookingAt(), "Should not match at position " + start + ": " + input);
    }

    @Test
    void testUnclosedChar() {
        Matcher m = matcher(TokenPattern.STRING, "'", 0);
        assertFalse(m.lookingAt());
    }

    @Test
    void testIncompleteEscape() {
        Matcher m = matcher(TokenPattern.STRING, "\"\\", 0);
        assertFalse(m.lookingAt());
    }

    @Test
    void testStringLiteralAtEnd() {
        String line = "code";
        Matcher m = matcher(TokenPattern.STRING, line, line.length());
        assertFalse(m.lookingAt(), "Should not match at end of string");
    }
}
