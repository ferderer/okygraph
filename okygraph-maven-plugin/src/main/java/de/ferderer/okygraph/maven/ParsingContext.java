package de.ferderer.okygraph.maven;

import static de.ferderer.okygraph.maven.TokenPattern.*;
import java.util.List;

enum ParsingContext {
    JAVA(COMMENT_LINE, COMMENT_START, TEXT_BLOCK, STRING, BACKTICK, NUMBER, KEYWORD, IDENTIFIER, OPERATOR, WHITESPACE),
    TEMPLATE(TRY_START, CATCH_START, HTML, BRACE_OPEN, BACKTICK),
    CATCH(BRACE_OPEN, CATCH_OPS, IDENTIFIER, WHITESPACE),
    EXPRESSION(STRING, BRACE_CLOSE, NUMBER, EXPRESSION_KEYWORD, IDENTIFIER, OPERATOR, WHITESPACE),
    TEXT(TEXT_BLOCK, TEXT_CONTENT),
    COMMENT(COMMENT_END, COMMENT_CONTENT);

    public final List<TokenPattern> patterns;

    ParsingContext(TokenPattern... patterns) {
        this.patterns = List.of(patterns);
    }
}
