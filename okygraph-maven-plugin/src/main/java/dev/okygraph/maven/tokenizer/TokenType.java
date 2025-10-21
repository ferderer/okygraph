package dev.okygraph.maven.tokenizer;

/**
 * Enumeration of all token types recognized by the Okygraph tokenizer.
 */
public enum TokenType {

    // ===== JAVA LITERALS =====

    /** String literal: "hello" or "hello \"world\"" */
    STRING_LITERAL,

    /** Text block start: """ */
    TEXT_BLOCK_START,

    /** Text block content line */
    TEXT_BLOCK_LINE,

    /** Text block end: """ */
    TEXT_BLOCK_END,

    /** Character literal: 'a' or '\n' */
    CHAR_LITERAL,

    /** Numeric literal: 42, 3.14, 0xFF, 1_000_000L */
    NUMBER,


    // ===== COMMENTS =====

    /** Single-line comment: // comment */
    LINE_COMMENT,

    /** Block comment start: /* */
    BLOCK_COMMENT_START,

    /** Block comment content line */
    BLOCK_COMMENT_LINE,

    /** Block comment end: star-slash */
    BLOCK_COMMENT_END,


    // ===== JAVA KEYWORDS =====

    /** Java keyword: if, for, class, etc. */
    KEYWORD,

    /** Java identifier: variable name, method name, etc. */
    IDENTIFIER,


    // ===== TEMPLATE MARKERS =====

    /** Backtick for toggling between Java and template (HTML) modes: ` */
    BACKTICK,

    /** Template expression start in HTML: { */
    EXPRESSION_START,

    /** Template expression end in HTML: } */
    EXPRESSION_END,


    // ===== OPERATORS =====

    /** Operator: +, -, *, /, ==, !=, etc. */
    OPERATOR,


    // ===== SEPARATORS =====

    /** Separator: ( ) { } [ ] ; , . ... @ */
    SEPARATOR,


    // ===== HTML CONTENT =====

    /** Plain HTML/text content (when in template mode) */
    HTML_TEXT,


    // ===== WHITESPACE =====

    /** Whitespace: spaces, tabs (not newlines) */
    WHITESPACE,

    /** Newline character */
    NEWLINE,


    // ===== OTHER =====

    /** End of file marker */
    EOF,

    /** Unknown/unrecognized token */
    UNKNOWN
}
