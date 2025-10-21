package de.ferderer.okygraph.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tokenizer for Okygraph template files (.oky). */
public class Tokenizer {

    record TokenSpec(Pattern pattern, TokenType type) {}

    static TokenSpec spec(String regex, TokenType type) {
        return new TokenSpec(Pattern.compile("^(?:" + regex + ")"),  type);
    }

    public record Token(TokenType type, String value, int line, int column) {}

    enum TokenType {
        JAVA_TOKEN,        // Pass through
        TEXT_START,        // Text blocks
        TEXT_END,
        COMMENT_START,     // Block comments
        COMMENT_END,
        TRY, CATCH,        // Writer stack injection points
        BACKTICK,          // Mode toggle
        EXPRESSION_START,  // Template expressions
        EXPRESSION_END,
        HTML,              // Template content
        BRACE_OPEN,        // For exception handling in templates
        BRACE_CLOSE,
        NEWLINE
    }

    enum ParsingContext {
        JAVA(List.of(
            // Comments (must come before operators)
            spec("//[^\n]*", TokenType.JAVA_TOKEN),
            spec("/\\*", TokenType.COMMENT_START),

            // String literals (must come before operators)
            spec("\"\"\"", TokenType.TEXT_START),
            spec("\"(?:[^\"\\\\]|\\\\.)*\"", TokenType.JAVA_TOKEN),
            spec("'(?:[^'\\\\]|\\\\.)'", TokenType.JAVA_TOKEN),

            // Template marker (must come before regular backticks)
            spec("`", TokenType.BACKTICK),

            // Numbers (most specific first)
            // Hexadecimal floating-point
            spec("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*\\.[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.JAVA_TOKEN),
            spec("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.JAVA_TOKEN),
            // Decimal floating-point
            spec("[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.JAVA_TOKEN),
            spec("\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[fFdD]", TokenType.JAVA_TOKEN),
            // Integer literals
            spec("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("0[bB][01]+(?:_[01]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("0[0-7]+(?:_[0-7]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[lL]?", TokenType.JAVA_TOKEN),

            spec("try\\b", TokenType.TRY),
            spec("catch\\b", TokenType.CATCH),
            // Keywords (must come before identifiers)
            spec("(?:abstract|assert|boolean|break|byte|case|char|class|const|continue|" +
                "default|do|double|else|enum|extends|final|finally|float|for|goto|if|" +
                "implements|import|instanceof|int|interface|long|native|new|package|" +
                "private|protected|public|return|short|static|strictfp|super|switch|" +
                "synchronized|this|throw|throws|transient|void|volatile|while|" +
                "true|false|null|" +
                "record|sealed|non-sealed|permits|yield|var|when|" +
                "module|requires|exports|opens|uses|provides|to|with|open|transitive)\\b",
                TokenType.JAVA_TOKEN),

            // Identifiers
            spec("[a-zA-Z_$][a-zA-Z0-9_$]*", TokenType.JAVA_TOKEN),

            // Operators (longest first to avoid partial matches)
            spec(">>>=|<<=|>>=|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=", TokenType.JAVA_TOKEN),
            spec(">>>|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|->|::", TokenType.JAVA_TOKEN),
            spec("[+\\-*/%<>=&|^~!?:]", TokenType.JAVA_TOKEN),

            // Braces - needed for exception handling in templates
            spec("\\{", TokenType.BRACE_OPEN),
            spec("\\}", TokenType.BRACE_CLOSE),

            // Separators
            spec("\\.\\.\\.", TokenType.JAVA_TOKEN),
            spec("[.;,()\\[\\]{}@]", TokenType.JAVA_TOKEN),

            // Whitespace (spaces and tabs only, not newlines)
            spec("[ \\t]+", TokenType.JAVA_TOKEN)
        )),
        TEMPLATE(List.of(
            // Backtick toggles back to JAVA mode
            spec("`", TokenType.BACKTICK),

            // Expression start - enters JAVA_EXPRESSION mode
            spec("\\{", TokenType.EXPRESSION_START),

            // HTML text (everything else except backticks and braces)
            spec("[^`{]+", TokenType.HTML)
        )),
        EXPRESSION(List.of(
            // String literals (must come before operators to avoid tokenizing quotes)
            spec("\"(?:[^\"\\\\]|\\\\.)*\"", TokenType.JAVA_TOKEN),
            spec("'(?:[^'\\\\]|\\\\.)'", TokenType.JAVA_TOKEN),

            // Expression end - pops back to TEMPLATE mode
            spec("\\}", TokenType.EXPRESSION_END),

            // Numbers
            spec("[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.JAVA_TOKEN),
            spec("\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[fFdD]", TokenType.JAVA_TOKEN),
            spec("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("0[bB][01]+(?:_[01]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("0[0-7]+(?:_[0-7]+)*[lL]?", TokenType.JAVA_TOKEN),
            spec("[0-9]+(?:_[0-9]+)*[lL]?", TokenType.JAVA_TOKEN),

            // Keywords
            spec("(?:true|false|null|new|instanceof)\\b", TokenType.JAVA_TOKEN),

            // Identifiers
            spec("[a-zA-Z_$][a-zA-Z0-9_$]*", TokenType.JAVA_TOKEN),

            // Operators (longest first)
            spec(">>>=|<<=|>>=|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=", TokenType.JAVA_TOKEN),
            spec(">>>|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|->|::", TokenType.JAVA_TOKEN),
            spec("[+\\-*/%<>=&|^~!?:]", TokenType.JAVA_TOKEN),

            // Separators
            spec("\\.\\.\\.", TokenType.JAVA_TOKEN),
            spec("[.;,()\\[\\]@]", TokenType.JAVA_TOKEN),

            // Whitespace
            spec("[ \\t]+", TokenType.JAVA_TOKEN)
        )),
        TEXT(List.of(
            spec("\"\"\"", TokenType.TEXT_END),
            spec("[^\"]+|\"(?!\"\")", TokenType.JAVA_TOKEN)
        )),
        COMMENT(List.of(
            spec("\\*/", TokenType.COMMENT_END),
            spec("[^*]+|\\*(?!/)", TokenType.JAVA_TOKEN)
        ));

        public final List<TokenSpec> specs;

        ParsingContext(List<TokenSpec> specs) {
            this.specs = specs;
        }
    }

    private final String[] lines;
    private int currentLine = 0;
    private ParsingContext context = ParsingContext.JAVA;
    private final List<Token> tokens = new ArrayList<>();

    /**
     * Creates a new tokenizer for the given source code.
     *
     * @param source The source code to tokenize
     */
    public Tokenizer(String source) {
        source = preprocessUnicode(source != null ? source : "");
        lines = source.split("\n", -1);
    }

    private static String preprocessUnicode(String s) {
        Matcher m = Pattern.compile("\\\\u+([0-9a-fA-F]{4})").matcher(s);
        StringBuilder sb = new StringBuilder(s.length());
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start());
            sb.appendCodePoint(Integer.parseInt(m.group(1), 16));
            last = m.end();
        }
        return sb.append(s, last, s.length()).toString();
    }

    /**
     * Tokenizes the given source code and returns all tokens.
     *
     * @param source The source code to tokenize
     * @return List of tokens
     * @throws TokenizerException if an unexpected character is encountered
     */
    public static List<Token> process(String source) {
        return new Tokenizer(source).tokenize();
    }

    /**
     * Tokenizes the entire source code and returns all tokens.
     *
     * @return List of tokens
     * @throws TokenizerException if an unexpected character is encountered
     */
    public List<Token> tokenize() {
        for(String line : lines) {
            int position = 0;
            while (position < line.length()) {
                int newPosition = matchToken(line, position);
                if (newPosition == -1) {
                    throw new RuntimeException(String.format("Unexpected character '%c' at %d:%d",
                        line.charAt(position), currentLine + 1, position + 1));
                }
                position = newPosition;
            }
            tokens.add(new Token(TokenType.NEWLINE, "\n", currentLine + 1, line.length() + 1));
            currentLine++;
        }
        return tokens;
    }

    private int matchToken(String line, int position) {
        for (TokenSpec tokenSpec : context.specs) {
            Matcher matcher = tokenSpec.pattern().matcher(line.substring(position));
            if (matcher.find() && matcher.start() == 0) {
                String value = matcher.group();
                TokenType type = tokenSpec.type();

                // Create token
                tokens.add(new Token(type, value, currentLine + 1, position + 1));

                // Update parsing context
                context = switch(context) {
                    case JAVA -> switch (type) {
                        case COMMENT_START -> ParsingContext.COMMENT;
                        case TEXT_START -> ParsingContext.TEXT;
                        case BACKTICK -> ParsingContext.TEMPLATE;
                        default -> context;
                    };
                    case TEMPLATE -> switch (type) {
                        case BACKTICK -> ParsingContext.JAVA;
                        case EXPRESSION_START -> ParsingContext.EXPRESSION;
                        default -> context;
                    };
                    case EXPRESSION -> type == TokenType.EXPRESSION_END ? ParsingContext.TEMPLATE : context;
                    case COMMENT -> type == TokenType.COMMENT_END ? ParsingContext.JAVA : context;
                    case TEXT -> type == TokenType.TEXT_END ? ParsingContext.JAVA : context;
                };
                return position + value.length();
            }
        }
        return -1;
    }
}
