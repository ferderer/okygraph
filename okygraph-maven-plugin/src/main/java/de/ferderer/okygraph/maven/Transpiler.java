package de.ferderer.okygraph.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Transpiler for Okygraph template files (.oky). */
public class Transpiler {

    record TokenSpec(Pattern pattern, TokenType type) {}

    static TokenSpec spec(String regex, TokenType type) {
        return new TokenSpec(Pattern.compile("^(?:" + regex + ")"),  type);
    }

    public record Token(TokenType type, String value, int line, int column) {}



    private static final List<TokenSpec> JAVA_PATTERNS = List.of(
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

        // Separators
        spec("\\.\\.\\.", TokenType.JAVA_TOKEN),
        spec("[.;,()\\[\\]{}@]", TokenType.JAVA_TOKEN),

        // Whitespace (spaces and tabs only, not newlines)
        spec("[ \\t]+", TokenType.JAVA_TOKEN)
    );

    private static final List<TokenSpec> TRY_PATTERNS = new ArrayList<>(JAVA_PATTERNS);
    static {
        TRY_PATTERNS.add(0, spec("`\\}\\s*catch\\b", TokenType.CATCH));
    }

    enum ParsingContext {
        JAVA(JAVA_PATTERNS),
        TEMPLATE(List.of(
            spec("`try\\s*\\{`", TokenType.TRY),
            spec("`\\}\\s*catch\\b", TokenType.CATCH),
            spec("[^`{]+", TokenType.HTML),
            spec("\\{", TokenType.EXPRESSION_START),
            spec("`", TokenType.BACKTICK)
        )),
        TRY(TRY_PATTERNS),
        CATCH(List.of(
            spec("\\{", TokenType.CATCH_END),
            spec("final\\b[@.,|()\\s]+", TokenType.JAVA_TOKEN),
            spec("[a-zA-Z_$][a-zA-Z0-9_$]*", TokenType.JAVA_TOKEN)
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

    private final Writer writer = new StringWriter();
    private ParsingContext context = ParsingContext.JAVA;

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
    public static String process(String source) throws IOException {
        return new Transpiler().transpile(preprocessUnicode(source));
    }

    /**
     * Tokenizes the entire source code and returns all tokens.
     *
     * @return List of tokens
     * @throws TokenizerException if an unexpected character is encountered
     */
    public String transpile(String source) throws IOException {
        int currentLine = 0;
        for(String line : source.split("\n", -1)) {
            int position = 0;
            while (position < line.length()) {
                int newPosition = matchToken(line, position);
                if (newPosition == -1) {
                    throw new RuntimeException(String.format("Unexpected character '%c' at %d:%d",
                        line.charAt(position), currentLine + 1, position + 1));
                }
                position = newPosition;
            }
            currentLine++;
        }
        return writer.toString();
    }

    private int matchToken(String line, int position) throws IOException {
        for (TokenSpec tokenSpec : context.specs) {
            Matcher matcher = tokenSpec.pattern().matcher(line.substring(position));
            if (matcher.find() && matcher.start() == 0) {
                String value = matcher.group();
                TokenType type = tokenSpec.type();

                emit(type, value, line);

                // Update parsing context
                context = switch(context) {
                    case JAVA -> switch (type) {
                        case COMMENT_START -> ParsingContext.COMMENT;
                        case TEXT_START -> ParsingContext.TEXT;
                        case BACKTICK -> ParsingContext.TEMPLATE;
                        default -> context;
                    };
                    case TRY -> switch (type) {
                        case COMMENT_START -> ParsingContext.COMMENT;
                        case TEXT_START -> ParsingContext.TEXT;
                        case BACKTICK -> ParsingContext.TEMPLATE;
                        default -> context;
                    };
                    case CATCH -> type == TokenType.CATCH_END ? ParsingContext.JAVA : context;
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

    private final StringBuilder htmlBuffer = new StringBuilder();
    private boolean seenTemplateToken = false;

    private void emit(TokenType type, String value, String line) throws IOException {
        switch (type) {
            case HTML -> {
                if (value.equals(line)) { // start text block
                    htmlBuffer.append(value).append("\n");
                }
                else { // this is a HTML fragment - line with an expression
                    checkFragmentedLine();
                    writer.write("html(\"" + escape(value) + "\")");
                }
            }
            case EXPRESSION_START -> {
                checkFragmentedLine();
                writer.write("write(");
            }
            case EXPRESSION_END -> writer.write(")");
            case TRY -> writer.write("try { this.pushBuffer(); ");
            case CATCH -> writer.write(" this.commitBuffer(); } catch");
            case CATCH_END -> writer.write("{ this.discardBuffer(); ");
            case BACKTICK -> flushHtmlBuffer();
            case NEWLINE -> {
                if (seenTemplateToken) {
                    writer.write(";");
                    seenTemplateToken = false;
                }
                if (htmlBuffer.isEmpty()) {
                    writer.write("\n");
                }
            }
            default -> writer.write(value);
        }
    }

    private void checkFragmentedLine() throws IOException {
        if (seenTemplateToken) { // not the first fragment - do chaining
            writer.write(".");
        } else {
            flushHtmlBuffer();
        }
        seenTemplateToken = true;
    }
    
    private void flushHtmlBuffer() throws IOException {
        if (!htmlBuffer.isEmpty()) {
            writer.write("html(\"\"\"\n");
            writer.write(htmlBuffer.toString());
            writer.write("\"\"\");\n");
            htmlBuffer.setLength(0);
        }
    }

    private String escape(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
