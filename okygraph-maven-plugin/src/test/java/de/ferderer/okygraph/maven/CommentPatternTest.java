package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommentPatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testCommentLineType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.COMMENT_LINE.type);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "// simple comment|0|// simple comment",
        "code // comment|5|// comment",
        "x//no space|1|//no space",
    })
    void testCommentLineMatches(String input, int start, String expected) {
        Matcher m = matcher(TokenPattern.COMMENT_LINE, input, start);
        assertTrue(m.lookingAt(), "Should match at position " + start + ": " + input);
        assertEquals(expected, m.group());
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "/ / not a comment|0",
        "x// comment|0",
    })
    void testCommentLineNoMatch(String input, int start) {
        Matcher m = matcher(TokenPattern.COMMENT_LINE, input, start);
        assertFalse(m.lookingAt(), "Should not match at position " + start + ": " + input);
    }

    @Test
    void testCommentLineAtEnd() {
        String line = "code";
        Matcher m = matcher(TokenPattern.COMMENT_LINE, line, line.length());
        assertFalse(m.lookingAt(), "Should not match at end of string");
    }

    @Test
    void testCommentBlockStartType() {
        assertEquals(TokenType.COMMENT_START, TokenPattern.COMMENT_BLOCK_START.type);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "/* comment|0|/*",
        "code /* comment|5|/*",
        "/** javadoc|0|/*",
    })
    void testCommentBlockStartMatches(String input, int start, String expected) {
        Matcher m = matcher(TokenPattern.COMMENT_BLOCK_START, input, start);
        assertTrue(m.lookingAt(), "Should match at position " + start + ": " + input);
        assertEquals(expected, m.group());
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "/ * not a comment|0",
        "x/* comment|0",
    })
    void testCommentBlockStartNoMatch(String input, int start) {
        Matcher m = matcher(TokenPattern.COMMENT_BLOCK_START, input, start);
        assertFalse(m.lookingAt(), "Should not match at position " + start + ": " + input);
    }

    @Test
    void testCommentBlockStartAtEnd() {
        String line = "code";
        Matcher m = matcher(TokenPattern.COMMENT_BLOCK_START, line, line.length());
        assertFalse(m.lookingAt(), "Should not match at end of string");
    }
}
