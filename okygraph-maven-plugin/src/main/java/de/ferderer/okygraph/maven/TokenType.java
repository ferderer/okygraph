package de.ferderer.okygraph.maven;

enum TokenType {
    JAVA_TOKEN,        // Pass through
    TEXT,              // Text blocks
    COMMENT_START,     // Block comments
    COMMENT_END,
    TRY, CATCH,        // Writer stack injection points
    BACKTICK,          // Mode toggle
    EXPRESSION_START,  // Template expressions
    EXPRESSION_END,
    HTML,              // Template content
    NEWLINE
}
