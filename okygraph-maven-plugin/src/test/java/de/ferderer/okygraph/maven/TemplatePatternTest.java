package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TemplatePatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testBacktickType() {
        assertEquals(TokenType.BACKTICK, TokenPattern.BACKTICK.type);
    }

    @Test
    void testBacktickMatches() {
        String[][] tests = {
            {"`", "0", "`"},
            {"x`y", "1", "`"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.BACKTICK, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testTextBlockDelimiterType() {
        assertEquals(TokenType.TEXT, TokenPattern.TEXT_BLOCK.type);
    }

    @Test
    void testTextBlockDelimiterMatches() {
        String[][] tests = {
            {"\"\"\"", "0", "\"\"\""},
            {"x\"\"\"y", "1", "\"\"\""},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.TEXT_BLOCK, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testBraceOpenType() {
        assertEquals(TokenType.EXPRESSION_START, TokenPattern.BRACE_OPEN.type);
    }

    @Test
    void testBraceOpenMatches() {
        String[][] tests = {
            {"{", "0", "{"},
            {"x{y", "1", "{"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.BRACE_OPEN, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testBraceCloseType() {
        assertEquals(TokenType.EXPRESSION_END, TokenPattern.BRACE_CLOSE.type);
    }

    @Test
    void testBraceCloseMatches() {
        String[][] tests = {
            {"}", "0", "}"},
            {"x}y", "1", "}"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.BRACE_CLOSE, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testTryStartType() {
        assertEquals(TokenType.TRY, TokenPattern.TRY_START.type);
    }

    @Test
    void testTryStartMatches() {
        String[][] tests = {
            {"`try{`", "0", "`try{`"},
            {"`try {`", "0", "`try {`"},
            {"`try  {`", "0", "`try  {`"},
            {"`try\t{`", "0", "`try\t{`"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.TRY_START, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testCatchStartType() {
        assertEquals(TokenType.CATCH, TokenPattern.CATCH_START.type);
    }

    @Test
    void testCatchStartMatches() {
        String[][] tests = {
            {"`}catch", "0", "`}catch"},
            {"`} catch", "0", "`} catch"},
            {"`}  catch", "0", "`}  catch"},
            {"`}\tcatch", "0", "`}\tcatch"},
            {"`} catch(", "0", "`} catch"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.CATCH_START, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testCatchBlockType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.CATCH_OPS.type);
    }

    @Test
    void testCatchBlockMatches() {
        String[][] tests = {
            {"final ", "0", "final "},
            {"final IOException", "0", "final "},
            {"final @Nullable ", "0", "final @"},
            {"final (", "0", "final ("},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.CATCH_OPS, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testHTMLContentType() {
        assertEquals(TokenType.HTML, TokenPattern.HTML.type);
    }

    @Test
    void testHTMLContentMatches() {
        String[][] tests = {
            {"Hello World", "0", "Hello World"},
            {"Hello \\` escaped", "0", "Hello \\` escaped"},
            {"Value: \\{x\\}", "0", "Value: \\{x\\}"},
            {"Text\\\\more", "0", "Text\\\\more"},
            {"<div>", "0", "<div>"},
            {"Plain text `", "0", "Plain text "},
            {"Before {expr}", "0", "Before "},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.HTML, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testHTMLContentNoMatch() {
        String[][] tests = {
            {"`start", "0"},
            {"{expr", "0"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.HTML, test[0], Integer.parseInt(test[1]));
            assertFalse(m.lookingAt(), "Should not match: " + test[0]);
        }
    }

    @Test
    void testCommentEndType() {
        assertEquals(TokenType.COMMENT_END, TokenPattern.COMMENT_END.type);
    }

    @Test
    void testCommentEndMatches() {
        String[][] tests = {
            {"*/", "0", "*/"},
            {"comment */", "8", "*/"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.COMMENT_END, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testCommentContentType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.COMMENT_CONTENT.type);
    }

    @Test
    void testCommentContentMatches() {
        String[][] tests = {
            {"comment text", "0", "comment text"},
            {"with * asterisk", "0", "with "},
            {"* alone", "0", "*"},
            {"no closing", "0", "no closing"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.COMMENT_CONTENT, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testCommentContentStopsBeforeEnd() {
        Matcher m = matcher(TokenPattern.COMMENT_CONTENT, "text*/end", 0);
        assertTrue(m.lookingAt());
        assertEquals("text", m.group());
    }

    @Test
    void testTextContentType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.TEXT_CONTENT.type);
    }

    @Test
    void testTextContentMatches() {
        String[][] tests = {
            {"text block content", "0", "text block content"},
            {"with \" quote", "0", "with "},
            {"\" alone", "0", "\""},
            {"no closing", "0", "no closing"},
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.TEXT_CONTENT, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testTextContentStopsBeforeDelimiter() {
        Matcher m = matcher(TokenPattern.TEXT_CONTENT, "text\"\"\"end", 0);
        assertTrue(m.lookingAt());
        assertEquals("text", m.group());
    }
}
