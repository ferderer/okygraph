package de.ferderer.okygraph.maven;

enum TokenType {
    JAVA_TOKEN,        // Pass through
    TEXT_START,        // Text blocks
    TEXT_END,
    COMMENT_START,     // Block comments
    COMMENT_END,
    TRY, CATCH,        // Writer stack injection points
    CATCH_END,
    BACKTICK,          // Mode toggle
    EXPRESSION_START,  // Template expressions
    EXPRESSION_END,
    HTML,              // Template content
    NEWLINE
}
