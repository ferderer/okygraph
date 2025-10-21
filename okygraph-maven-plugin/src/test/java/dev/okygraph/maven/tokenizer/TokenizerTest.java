package dev.okygraph.maven.tokenizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for the Tokenizer class.
 */
class TokenizerTest {

    @Test
    @DisplayName("Should tokenize empty source")
    void testEmptySource() {
        Tokenizer tokenizer = new Tokenizer("");
        List<Token> tokens = tokenizer.tokenize();

        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }

    @Test
    @DisplayName("Should tokenize simple Java code")
    void testSimpleJavaCode() {
        String source = "int x = 42;";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        assertTokenTypes(tokens,
            TokenType.KEYWORD,      // int
            TokenType.WHITESPACE,   // space
            TokenType.IDENTIFIER,   // x
            TokenType.WHITESPACE,   // space
            TokenType.OPERATOR,     // =
            TokenType.WHITESPACE,   // space
            TokenType.NUMBER,       // 42
            TokenType.SEPARATOR,    // ;
            TokenType.EOF
        );
    }

    @Test
    @DisplayName("Should tokenize Java keywords")
    void testKeywords() {
        String source = "public class for if while return";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        // Filter out whitespace
        List<Token> nonWhitespace = tokens.stream()
            .filter(t -> t.type() != TokenType.WHITESPACE)
            .toList();

        assertEquals(7, nonWhitespace.size()); // 6 keywords + EOF
        for (int i = 0; i < 6; i++) {
            assertEquals(TokenType.KEYWORD, nonWhitespace.get(i).type());
        }
    }

    @Test
    @DisplayName("Should tokenize identifiers")
    void testIdentifiers() {
        String source = "myVar _private $dollar userName123";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> identifiers = tokens.stream()
            .filter(t -> t.type() == TokenType.IDENTIFIER)
            .toList();

        assertEquals(4, identifiers.size());
        assertEquals("myVar", identifiers.get(0).value());
        assertEquals("_private", identifiers.get(1).value());
        assertEquals("$dollar", identifiers.get(2).value());
        assertEquals("userName123", identifiers.get(3).value());
    }

    @Test
    @DisplayName("Should tokenize numbers")
    void testNumbers() {
        String source = "42 3.14 0xFF 0b1010 1_000_000 2.5f 3.14d";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> numbers = tokens.stream()
            .filter(t -> t.type() == TokenType.NUMBER)
            .toList();

        assertEquals(7, numbers.size());
        assertEquals("42", numbers.get(0).value());
        assertEquals("3.14", numbers.get(1).value());
        assertEquals("0xFF", numbers.get(2).value());
        assertEquals("0b1010", numbers.get(3).value());
        assertEquals("1_000_000", numbers.get(4).value());
        assertEquals("2.5f", numbers.get(5).value());
        assertEquals("3.14d", numbers.get(6).value());
    }

    @Test
    @DisplayName("Should tokenize string literals")
    void testStringLiterals() {
        String source = "\"hello\" \"world\\n\" \"with \\\"quotes\\\"\"";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> strings = tokens.stream()
            .filter(t -> t.type() == TokenType.STRING_LITERAL)
            .toList();

        assertEquals(3, strings.size());
        assertEquals("\"hello\"", strings.get(0).value());
        assertEquals("\"world\\n\"", strings.get(1).value());
        assertEquals("\"with \\\"quotes\\\"\"", strings.get(2).value());
    }

    @Test
    @DisplayName("Should tokenize character literals")
    void testCharLiterals() {
        String source = "'a' 'b' '\\n' '\\\\'";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> chars = tokens.stream()
            .filter(t -> t.type() == TokenType.CHAR_LITERAL)
            .toList();

        assertEquals(4, chars.size());
        assertEquals("'a'", chars.get(0).value());
        assertEquals("'b'", chars.get(1).value());
        assertEquals("'\\n'", chars.get(2).value());
        assertEquals("'\\\\'", chars.get(3).value());
    }

    @Test
    @DisplayName("Should tokenize operators")
    void testOperators() {
        String source = "+ - * / == != <= >= && ||";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> operators = tokens.stream()
            .filter(t -> t.type() == TokenType.OPERATOR)
            .toList();

        assertEquals(10, operators.size());
    }

    @Test
    @DisplayName("Should tokenize separators")
    void testSeparators() {
        String source = "( ) { } [ ] ; , . ...";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> separators = tokens.stream()
            .filter(t -> t.type() == TokenType.SEPARATOR)
            .toList();

        // ( ) { } [ ] ; , . ... = 10 separators
        assertEquals(10, separators.size());
    }

    @Test
    @DisplayName("Should tokenize line comments")
    void testLineComments() {
        String source = "int x = 5; // This is a comment\nint y = 10;";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        List<Token> comments = tokens.stream()
            .filter(t -> t.type() == TokenType.LINE_COMMENT)
            .toList();

        assertEquals(1, comments.size());
        assertEquals("// This is a comment", comments.get(0).value());
    }

    @Test
    @DisplayName("Should tokenize block comments")
    void testBlockComments() {
        String source = "/* This is a\n multi-line\n comment */";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        boolean hasCommentStart = tokens.stream()
            .anyMatch(t -> t.type() == TokenType.BLOCK_COMMENT_START);
        boolean hasCommentEnd = tokens.stream()
            .anyMatch(t -> t.type() == TokenType.BLOCK_COMMENT_END);

        assertTrue(hasCommentStart);
        assertTrue(hasCommentEnd);
    }

    @Test
    @DisplayName("Should tokenize text blocks")
    void testTextBlocks() {
        String source = "\"\"\"\nHello\nWorld\n\"\"\"";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        boolean hasTextBlockStart = tokens.stream()
            .anyMatch(t -> t.type() == TokenType.TEXT_BLOCK_START);
        boolean hasTextBlockEnd = tokens.stream()
            .anyMatch(t -> t.type() == TokenType.TEXT_BLOCK_END);

        assertTrue(hasTextBlockStart);
        assertTrue(hasTextBlockEnd);
    }

    @Test
    @DisplayName("Should tokenize template start marker")
    void testTemplateStart() {
        String source = "public void render() {`";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        Token backtick = tokens.stream()
            .filter(t -> t.type() == TokenType.BACKTICK)
            .findFirst()
            .orElse(null);

        assertNotNull(backtick);
        assertEquals("`", backtick.value());
    }

    @Test
    @DisplayName("Should tokenize template end marker")
    void testTemplateEnd() {
        String source = "`<html></html>`";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        long backtickCount = tokens.stream()
            .filter(t -> t.type() == TokenType.BACKTICK)
            .count();

        assertEquals(2, backtickCount); // Opening and closing backticks
    }

    @Test
    @DisplayName("Should tokenize HTML content in template")
    void testHtmlContent() {
        String source = "`<h1>Hello World</h1>`";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        assertTokenTypes(tokens,
            TokenType.BACKTICK,         // `
            TokenType.HTML_TEXT,        // <h1>Hello World</h1>
            TokenType.BACKTICK,         // `
            TokenType.EOF
        );
    }

    @Test
    @DisplayName("Should tokenize expression in template")
    void testTemplateExpression() {
        String source = "`<h1>{user.name}</h1>`";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        assertTokenTypes(tokens,
            TokenType.BACKTICK,           // `
            TokenType.HTML_TEXT,          // <h1>
            TokenType.EXPRESSION_START,   // {
            TokenType.IDENTIFIER,         // user
            TokenType.SEPARATOR,          // .
            TokenType.IDENTIFIER,         // name
            TokenType.EXPRESSION_END,     // }
            TokenType.HTML_TEXT,          // </h1>
            TokenType.BACKTICK,           // `
            TokenType.EOF
        );
    }

    @Test
    @DisplayName("Should tokenize backticks in template")
    void testBackticksInTemplate() {
        String source = "`<div>`if (x > 0) {`<span>positive</span>`}`</div>`";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        long backtickCount = tokens.stream()
            .filter(t -> t.type() == TokenType.BACKTICK)
            .count();

        assertEquals(6, backtickCount); // 6 backticks total (template open/close + if open/close + block close/open)
    }

    @Test
    @DisplayName("Should track line and column numbers")
    void testLineAndColumn() {
        String source = "int x = 5;\nint y = 10;";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        // First 'int' should be at line 1, column 1
        Token firstInt = tokens.stream()
            .filter(t -> t.type() == TokenType.KEYWORD && t.value().equals("int"))
            .findFirst()
            .orElse(null);

        assertNotNull(firstInt);
        assertEquals(1, firstInt.line());
        assertEquals(1, firstInt.column());

        // Second 'int' should be at line 2
        Token secondInt = tokens.stream()
            .filter(t -> t.type() == TokenType.KEYWORD && t.value().equals("int"))
            .skip(1)
            .findFirst()
            .orElse(null);

        assertNotNull(secondInt);
        assertEquals(2, secondInt.line());
    }

    @Test
    @DisplayName("Should handle complex template with multiple features")
    void testComplexTemplate() {
        String source = """
            public void render() {`
                <html>
                <head><title>{title}</title></head>
                <body>
                    `if (user != null) {`
                        <h1>Welcome {user.name}!</h1>
                    `}`
                </body>
                </html>
            `}
            """;

        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();

        // Should successfully tokenize without errors
        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);

        // Should have backticks
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.BACKTICK));

        // Should have expressions
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.EXPRESSION_START));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.EXPRESSION_END));
    }

    // Helper method to assert token types in sequence
    private void assertTokenTypes(List<Token> tokens, TokenType... expectedTypes) {
        assertEquals(expectedTypes.length, tokens.size(),
            "Expected " + expectedTypes.length + " tokens but got " + tokens.size());

        for (int i = 0; i < expectedTypes.length; i++) {
            assertEquals(expectedTypes[i], tokens.get(i).type(),
                "Token at index " + i + " should be " + expectedTypes[i] +
                " but was " + tokens.get(i).type());
        }
    }
}
