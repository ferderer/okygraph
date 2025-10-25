package de.ferderer.okygraph.maven;

import static de.ferderer.okygraph.maven.TokenPattern.*;
import java.util.List;

enum ParsingContext {
    JAVA(COMMENT_LINE, COMMENT_START, TEXT_BLOCK, STRING, BACKTICK, NUMBER, KEYWORD, IDENTIFIER, OPERATOR, WHITESPACE),
    TRY(COMMENT_LINE, COMMENT_START, TEXT_BLOCK, STRING, BACKTICK, NUMBER, KEYWORD, IDENTIFIER, OPERATOR, WHITESPACE, CATCH_START),
    TEMPLATE(TRY_START, CATCH_START, HTML, BRACE_OPEN, BACKTICK),
    CATCH(COMMENT_END, CATCH_BLOCK),
    EXPRESSION(STRING, BRACE_CLOSE, NUMBER, EXPRESSION_KEYWORD, IDENTIFIER, OPERATOR, WHITESPACE),
    TEXT(TEXT_BLOCK, TEXT_CONTENT),
    COMMENT(COMMENT_END, COMMENT_CONTENT);

    public final List<TokenPattern> patterns;

    ParsingContext(TokenPattern... patterns) {
        this.patterns = List.of(patterns);
    }
}
