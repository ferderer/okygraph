package dev.okygraph.maven.tokenizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the UnicodePreprocessor class.
 */
class UnicodePreprocessorTest {

    @Test
    @DisplayName("Should handle null source")
    void testNullSource() {
        String result = UnicodePreprocessor.process(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle empty source")
    void testEmptySource() {
        String result = UnicodePreprocessor.process("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should not modify source without Unicode escapes")
    void testNoUnicodeEscapes() {
        String source = "Hello World!";
        String result = UnicodePreprocessor.process(source);
        assertEquals(source, result);
    }

    @Test
    @DisplayName("Should process single Unicode escape")
    void testSingleUnicodeEscape() {
        String source = "\\u0048"; // H
        String result = UnicodePreprocessor.process(source);
        assertEquals("H", result);
    }

    @Test
    @DisplayName("Should process multiple Unicode escapes")
    void testMultipleUnicodeEscapes() {
        String source = "\\u0048\\u0065\\u006C\\u006C\\u006F"; // Hello
        String result = UnicodePreprocessor.process(source);
        assertEquals("Hello", result);
    }

    @Test
    @DisplayName("Should process Unicode escapes mixed with regular text")
    void testMixedContent() {
        String source = "Hello \\u0057\\u006F\\u0072\\u006C\\u0064!"; // Hello World!
        String result = UnicodePreprocessor.process(source);
        assertEquals("Hello World!", result);
    }

    @Test
    @DisplayName("Should handle uppercase hex digits")
    void testUppercaseHex() {
        String source = "\\u0041\\u0042\\u0043"; // ABC
        String result = UnicodePreprocessor.process(source);
        assertEquals("ABC", result);
    }

    @Test
    @DisplayName("Should handle lowercase hex digits")
    void testLowercaseHex() {
        String source = "\\u0061\\u0062\\u0063"; // abc
        String result = UnicodePreprocessor.process(source);
        assertEquals("abc", result);
    }

    @Test
    @DisplayName("Should handle mixed case hex digits")
    void testMixedCaseHex() {
        String source = "\\u00aB\\u00Cd";
        String result = UnicodePreprocessor.process(source);
        assertNotNull(result);
        assertEquals(2, result.length());
    }

    @Test
    @DisplayName("Should not process incomplete Unicode escape")
    void testIncompleteEscape() {
        String source = "\\u004"; // Only 3 hex digits
        String result = UnicodePreprocessor.process(source);
        assertEquals(source, result); // Should remain unchanged
    }

    @Test
    @DisplayName("Should not process invalid Unicode escape")
    void testInvalidEscape() {
        String source = "\\u00XY"; // Invalid hex digits
        String result = UnicodePreprocessor.process(source);
        assertEquals(source, result); // Should remain unchanged
    }

    @Test
    @DisplayName("Should process Unicode escape at end of string")
    void testEscapeAtEnd() {
        String source = "test\\u0021"; // test!
        String result = UnicodePreprocessor.process(source);
        assertEquals("test!", result);
    }

    @Test
    @DisplayName("Should process Unicode escape at beginning of string")
    void testEscapeAtBeginning() {
        String source = "\\u0074est"; // test
        String result = UnicodePreprocessor.process(source);
        assertEquals("test", result);
    }
}
