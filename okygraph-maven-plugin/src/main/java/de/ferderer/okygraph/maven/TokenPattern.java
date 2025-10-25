package de.ferderer.okygraph.maven;

import java.util.regex.Pattern;

enum TokenPattern {
    COMMENT_LINE("//[^\n]*", TokenType.JAVA_TOKEN),
    COMMENT_BLOCK_START("/\\*", TokenType.COMMENT_START);

    public final Pattern pattern;
    public final TokenType type;

    TokenPattern(String regex, TokenType type) {
        this.pattern = Pattern.compile(regex);
        this.type = type;
    }
}
