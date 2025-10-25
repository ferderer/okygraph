package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum TokenPattern {
    COMMENT_LINE("//[^\n]*", TokenType.JAVA_TOKEN),

    COMMENT_START("/\\*", TokenType.COMMENT_START),
    COMMENT_CONTENT("[^*]+|\\*(?!/)", TokenType.JAVA_TOKEN),
    COMMENT_END("\\*/", TokenType.COMMENT_END),

    STRING("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)'", TokenType.JAVA_TOKEN),
    NUMBER(
        "0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*\\.[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b|" +
        "0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b|" +
        "[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?\\b|" +
        "\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?\\b|" +
        "[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b|" +
        "[0-9]+(?:_[0-9]+)*[fFdD]\\b|" +
        "0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?\\b|" +
        "0[bB][01]+(?:_[01]+)*[lL]?\\b|" +
        "0[0-7]+(?:_[0-7]+)*[lL]?\\b|" +
        "[0-9]+(?:_[0-9]+)*[lL]?\\b", TokenType.JAVA_TOKEN),
    OPERATOR(
        ">>>=|<<=|>>=|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|" +
        ">>>|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|->|::|" +
        "\\.\\.\\.|" +
        "[+\\-*/%<>=&|^~!?:;.,()\\[\\]{}@]", TokenType.JAVA_TOKEN),
    KEYWORD(
        "(?:abstract|assert|boolean|break|byte|case|char|class|const|continue|" +
        "default|do|double|else|enum|extends|final|finally|float|for|goto|if|" +
        "implements|import|instanceof|int|interface|long|native|new|package|" +
        "private|protected|public|return|short|static|strictfp|super|switch|" +
        "synchronized|this|throw|throws|transient|void|volatile|while|" +
        "true|false|null|" +
        "record|sealed|non-sealed|permits|yield|var|when|" +
        "module|requires|exports|opens|uses|provides|to|with|open|transitive)\\b",
        TokenType.JAVA_TOKEN),
    EXPRESSION_KEYWORD("(?:true|false|null|new|instanceof)\\b", TokenType.JAVA_TOKEN),
    IDENTIFIER("[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*", TokenType.JAVA_TOKEN),
    WHITESPACE("[ \\t]+", TokenType.JAVA_TOKEN),
    BACKTICK("`", TokenType.BACKTICK),
    TEXT_BLOCK("\"\"\"", TokenType.TEXT),
    BRACE_OPEN("\\{", TokenType.EXPRESSION_START),
    BRACE_CLOSE("\\}", TokenType.EXPRESSION_END),
    TRY_START("`try\\s*\\{`", TokenType.TRY),
    CATCH_START("`\\}\\s*catch\\b", TokenType.CATCH),
    CATCH_OPS("final\\b|[@.,|()]", TokenType.JAVA_TOKEN),
    HTML("(?:[^`{\\\\]|\\\\.)+", TokenType.HTML),
    TEXT_CONTENT("[^\"]+|\"(?!\"\")", TokenType.JAVA_TOKEN);

    public final Pattern pattern;
    public final TokenType type;

    TokenPattern(String regex, TokenType type) {
        this.pattern = Pattern.compile(regex);
        this.type = type;
    }

    String match(String line, int position) {
        Matcher m = pattern.matcher(line).region(position, line.length());
        return m.lookingAt() ? m.group() : null;
    }
}
