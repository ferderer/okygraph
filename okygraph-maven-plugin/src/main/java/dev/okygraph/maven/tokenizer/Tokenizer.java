package dev.okygraph.maven.tokenizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizer for Okygraph template files (.jmt).
 *
 * Performs lexical analysis on Java source code with template method bodies.
 * Recognizes:
 * - All standard Java tokens (keywords, identifiers, literals, operators, etc.)
 * - Template boundaries: {` (template start) and `} (template end)
 * - Template expressions: { and } within template bodies
 * - Backticks for switching between HTML and Java modes
 *
 * The tokenizer maintains a mode stack to track context:
 * - JAVA: Normal Java code
 * - TEMPLATE: HTML/text content with expressions and control flow
 * - JAVA_TEXT_BLOCK: Inside Java text block
 * - JAVA_BLOCK_COMMENT: Inside Java block comment
 *
 * @author Vadim Ferderer
 */
public class Tokenizer {

    /**
     * Tokenization mode - tracks current parsing context.
     * Each mode holds its own token specifications.
     */
    private enum Mode {
        /** Normal Java code */
        JAVA,

        /** Inside template body (HTML/text with expressions) */
        TEMPLATE,

        /** Inside Java expression within template (between { and }) */
        JAVA_EXPRESSION,

        /** Inside Java text block (""" ... """) */
        JAVA_TEXT_BLOCK,

        /** Inside Java block comment */
        JAVA_BLOCK_COMMENT;

        // Lazy initialization to avoid circular dependency
        private List<TokenSpec> specs;

        public List<TokenSpec> getSpecs() {
            if (specs == null) {
                specs = switch (this) {
                    case JAVA -> buildJavaSpecs();
                    case TEMPLATE -> buildTemplateSpecs();
                    case JAVA_EXPRESSION -> buildJavaExpressionSpecs();
                    case JAVA_TEXT_BLOCK -> buildTextBlockSpecs();
                    case JAVA_BLOCK_COMMENT -> buildBlockCommentSpecs();
                };
            }
            return specs;
        }
    }

    /**
     * Mode transition action
     */
    private enum ModeAction {
        /** No mode change */
        NONE,
        /** Push a new mode onto the stack */
        PUSH,
        /** Pop the current mode from the stack */
        POP
    }

    /**
     * Token specification - pattern, token type, and mode transition
     */
    private record TokenSpec(Pattern pattern, TokenType type, ModeAction action, Mode newMode) {

        TokenSpec {
            if (action == ModeAction.PUSH && newMode == null) {
                throw new IllegalArgumentException("newMode must not be null when action is PUSH");
            }
        }

        static TokenSpec of(String regex, TokenType type) {
            return new TokenSpec(Pattern.compile("^(?:" + regex + ")"),  type, ModeAction.NONE, null);
        }

        static TokenSpec of(String regex, TokenType type, ModeAction action, Mode newMode) {
            return new TokenSpec(Pattern.compile("^(?:" + regex + ")"), type, action, newMode);
        }
    }

    /**
     * Builds token specifications for JAVA mode.
     */
    private static List<TokenSpec> buildJavaSpecs() {
        return List.of(
            // Comments (must come before operators)
            TokenSpec.of("//[^\n]*", TokenType.LINE_COMMENT),
            TokenSpec.of("/\\*", TokenType.BLOCK_COMMENT_START, ModeAction.PUSH, Mode.JAVA_BLOCK_COMMENT),

            // String literals (must come before operators)
            TokenSpec.of("\"\"\"", TokenType.TEXT_BLOCK_START, ModeAction.PUSH, Mode.JAVA_TEXT_BLOCK),
            TokenSpec.of("\"(?:[^\"\\\\]|\\\\.)*\"", TokenType.STRING_LITERAL),
            TokenSpec.of("'(?:[^'\\\\]|\\\\.)'", TokenType.CHAR_LITERAL),

            // Template marker (must come before regular backticks)
            TokenSpec.of("`", TokenType.BACKTICK, ModeAction.PUSH, Mode.TEMPLATE),

            // Numbers (most specific first)
            // Hexadecimal floating-point
            TokenSpec.of("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*\\.[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.NUMBER),
            // Decimal floating-point
            TokenSpec.of("[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[fFdD]", TokenType.NUMBER),
            // Integer literals
            TokenSpec.of("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("0[bB][01]+(?:_[01]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("0[0-7]+(?:_[0-7]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[lL]?", TokenType.NUMBER),

            // Keywords (must come before identifiers)
            TokenSpec.of("(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|" +
                        "default|do|double|else|enum|extends|final|finally|float|for|goto|if|" +
                        "implements|import|instanceof|int|interface|long|native|new|package|" +
                        "private|protected|public|return|short|static|strictfp|super|switch|" +
                        "synchronized|this|throw|throws|transient|try|void|volatile|while|" +
                        "true|false|null|" +
                        "record|sealed|non-sealed|permits|yield|var|when|" +
                        "module|requires|exports|opens|uses|provides|to|with|open|transitive)\\b",
                        TokenType.KEYWORD),

            // Identifiers
            TokenSpec.of("[a-zA-Z_$][a-zA-Z0-9_$]*", TokenType.IDENTIFIER),

            // Operators (longest first to avoid partial matches)
            TokenSpec.of(">>>=|<<=|>>=|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=", TokenType.OPERATOR),
            TokenSpec.of(">>>|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|->|::", TokenType.OPERATOR),
            TokenSpec.of("[+\\-*/%<>=&|^~!?:]", TokenType.OPERATOR),

            // Separators
            TokenSpec.of("\\.\\.\\.", TokenType.SEPARATOR),
            TokenSpec.of("[.;,()\\[\\]{}@]", TokenType.SEPARATOR),

            // Whitespace (spaces and tabs only, not newlines)
            TokenSpec.of("[ \\t]+", TokenType.WHITESPACE)
        );
    }

    /**
     * Builds token specifications for TEMPLATE mode (HTML content with expressions).
     */
    private static List<TokenSpec> buildTemplateSpecs() {
        return List.of(
            // Backtick toggles back to JAVA mode
            TokenSpec.of("`", TokenType.BACKTICK, ModeAction.POP, null),

            // Expression start - enters JAVA_EXPRESSION mode
            TokenSpec.of("\\{", TokenType.EXPRESSION_START, ModeAction.PUSH, Mode.JAVA_EXPRESSION),

            // HTML text (everything else except backticks and braces)
            TokenSpec.of("[^`{]+", TokenType.HTML_TEXT)
        );
    }

    /**
     * Builds token specifications for JAVA_EXPRESSION mode (Java code inside {} in templates).
     */
    private static List<TokenSpec> buildJavaExpressionSpecs() {
        return List.of(
            // String literals (must come before operators to avoid tokenizing quotes)
            TokenSpec.of("\"(?:[^\"\\\\]|\\\\.)*\"", TokenType.STRING_LITERAL),
            TokenSpec.of("'(?:[^'\\\\]|\\\\.)'", TokenType.CHAR_LITERAL),

            // Expression end - pops back to TEMPLATE mode
            TokenSpec.of("\\}", TokenType.EXPRESSION_END, ModeAction.POP, null),

            // Nested braces (for lambdas, arrays, etc.) - stays in expression mode
            TokenSpec.of("\\{", TokenType.SEPARATOR),

            // Numbers
            TokenSpec.of("[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("\\.[0-9]+(?:_[0-9]+)*(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[fFdD]", TokenType.NUMBER),
            TokenSpec.of("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("0[bB][01]+(?:_[01]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("0[0-7]+(?:_[0-7]+)*[lL]?", TokenType.NUMBER),
            TokenSpec.of("[0-9]+(?:_[0-9]+)*[lL]?", TokenType.NUMBER),

            // Keywords
            TokenSpec.of("(?:true|false|null|new|instanceof)\\b", TokenType.KEYWORD),

            // Identifiers
            TokenSpec.of("[a-zA-Z_$][a-zA-Z0-9_$]*", TokenType.IDENTIFIER),

            // Operators (longest first)
            TokenSpec.of(">>>=|<<=|>>=|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=", TokenType.OPERATOR),
            TokenSpec.of(">>>|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|->|::", TokenType.OPERATOR),
            TokenSpec.of("[+\\-*/%<>=&|^~!?:]", TokenType.OPERATOR),

            // Separators
            TokenSpec.of("\\.\\.\\.", TokenType.SEPARATOR),
            TokenSpec.of("[.;,()\\[\\]@]", TokenType.SEPARATOR),

            // Whitespace
            TokenSpec.of("[ \\t]+", TokenType.WHITESPACE)
        );
    }

    /**
     * Builds token specifications for JAVA_BLOCK_COMMENT mode.
     */
    private static List<TokenSpec> buildBlockCommentSpecs() {
        return List.of(
            TokenSpec.of("\\*/", TokenType.BLOCK_COMMENT_END, ModeAction.POP, null),
            TokenSpec.of("[^*]+|\\*(?!/)", TokenType.BLOCK_COMMENT_LINE)
        );
    }

    /**
     * Builds token specifications for JAVA_TEXT_BLOCK mode.
     */
    private static List<TokenSpec> buildTextBlockSpecs() {
        return List.of(
            TokenSpec.of("\"\"\"", TokenType.TEXT_BLOCK_END, ModeAction.POP, null),
            TokenSpec.of("[^\"]+|\"(?!\"\")", TokenType.TEXT_BLOCK_LINE)
        );
    }

    // ===== Instance State =====

    private final String source;
    private final String[] lines;
    private int currentLine;
    private int currentColumn;
    private final Deque<Mode> modeStack;
    private final List<Token> tokens;

    /**
     * Creates a new tokenizer for the given source code.
     *
     * @param source The source code to tokenize
     */
    public Tokenizer(String source) {
        this.source = source != null ? source : "";
        this.lines = this.source.split("\n", -1); // -1 to preserve trailing empty lines
        this.currentLine = 0;
        this.currentColumn = 1;
        this.modeStack = new ArrayDeque<>();
        this.modeStack.push(Mode.JAVA);
        this.tokens = new ArrayList<>();
    }

    /**
     * Tokenizes the entire source code and returns all tokens.
     *
     * @return List of tokens
     * @throws TokenizerException if an unexpected character is encountered
     */
    public List<Token> tokenize() {
        tokens.clear();

        while (currentLine < lines.length) {
            String line = lines[currentLine];
            tokenizeLine(line);

            // Add newline token after each line (except last if it ends without newline)
            if (currentLine < lines.length - 1 || source.endsWith("\n")) {
                tokens.add(new Token(TokenType.NEWLINE, "\n", currentLine + 1, line.length() + 1));
            }

            currentLine++;
            currentColumn = 1;
        }

        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", currentLine, currentColumn));

        return tokens;
    }

    /**
     * Tokenizes a single line based on current mode.
     */
    private void tokenizeLine(String line) {
        int position = 0;

        while (position < line.length()) {
            Mode currentMode = modeStack.peek();
            MatchResult result = matchToken(line, position, currentMode);

            if (result == null) {
                throw new TokenizerException(
                    String.format("Unexpected character '%c' at %d:%d",
                        line.charAt(position), currentLine + 1, position + 1)
                );
            }

            position = result.newPosition;
            currentColumn = position + 1;
        }
    }

    /**
     * Result of a token match operation.
     */
    private record MatchResult(int newPosition) {}

    /**
     * Universal token matching method that works for all modes.
     */
    private MatchResult matchToken(String line, int position, Mode mode) {
        List<TokenSpec> specs = mode.getSpecs();

        for (TokenSpec spec : specs) {
            Matcher matcher = spec.pattern().matcher(line.substring(position));
            if (matcher.find() && matcher.start() == 0) {
                String value = matcher.group();
                TokenType type = spec.type();

                // Create token
                tokens.add(new Token(type, value, currentLine + 1, position + 1));

                // Handle mode transitions
                switch (spec.action()) {
                    case PUSH -> modeStack.push(spec.newMode());
                    case POP -> modeStack.pop();
                    case NONE -> {} // No action
                }

                return new MatchResult(position + value.length());
            }
        }

        return null;
    }    /**
     * Exception thrown when tokenization fails.
     */
    public static class TokenizerException extends RuntimeException {
        public TokenizerException(String message) {
            super(message);
        }

        public TokenizerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
