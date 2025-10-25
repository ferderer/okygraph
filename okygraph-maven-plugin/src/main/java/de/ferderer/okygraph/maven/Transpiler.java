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
    public record Token(TokenType type, String value, int line, int column) {}

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
        String[] lines = source.split("\n", -1);
        for(int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String indent = getIndent(line);
            writer.write(indent);
            int position = indent.length();
            while (position < line.length()) {
                int newPosition = matchToken(line, position);
                if (newPosition == -1) {
                    throw new RuntimeException(String.format(
                        "Unexpected character '%c' at line %d, column %d:\n%s\n%s^",
                        line.charAt(position), 
                        currentLine + 1, 
                        position + 1,
                        line,
                        " ".repeat(position)
                    ));
                }
                position = newPosition;
            }
            // Nur Newline emittieren wenn nicht letzte Zeile
            if (i < lines.length - 1) {
                emit(TokenType.NEWLINE, "\n", line);
            }
            currentLine++;
        }
        return writer.toString();
    }

    private int matchToken(String line, int position) throws IOException {
        for (TokenPattern pattern : context.patterns) {
            String value = pattern.match(line, position);
            if (value != null) {
                emit(pattern.type, value, line);
                updateContext(pattern.type);
                return position + value.length();
            }
        }
        return -1;
    }

    private void updateContext(TokenType type) {
        context = switch(context) {
            case JAVA -> switch (type) {
                case COMMENT_START -> ParsingContext.COMMENT;
                case TEXT -> ParsingContext.TEXT;
                case BACKTICK -> ParsingContext.TEMPLATE;
                default -> context;
            };
            case CATCH -> switch(type) {
                case EXPRESSION_START -> ParsingContext.JAVA;
                default -> context;                
            };
            case TEMPLATE -> switch (type) {
                case BACKTICK -> ParsingContext.JAVA;
                case EXPRESSION_START -> ParsingContext.EXPRESSION;
                case CATCH -> ParsingContext.CATCH;
                default -> context;
            };
            case EXPRESSION -> type == TokenType.EXPRESSION_END ? ParsingContext.TEMPLATE : context;
            case COMMENT -> type == TokenType.COMMENT_END ? ParsingContext.JAVA : context;
            case TEXT -> type == TokenType.TEXT ? ParsingContext.JAVA : context;  // TEXT als Delimiter
        };
    }

    private final List<String> htmlCache = new ArrayList<>();
    private boolean seenTemplateToken = false;

    private void emit(TokenType type, String value, String line) throws IOException {
        switch (type) {
            case HTML -> {
                if (!value.isBlank()) {
                    if (value.equals(line)) { // start text block
                        htmlCache.add(line);
                    }
                    else { // this is a HTML fragment - line with an expression
                        checkFragmentedLine();
                        writer.write("html(\"" + escape(value) + "\")");
                    }
                }
            }
            case EXPRESSION_START -> {
                if (context == ParsingContext.CATCH) { // CATCH_END semantics
                    writer.write("{ this.discardBuffer(); ");
                }
                else {
                    checkFragmentedLine();
                    writer.write("write(");
                }
            }
            case EXPRESSION_END -> writer.write(")");
            case TRY -> writer.write("try { this.pushBuffer(); ");
            case CATCH -> writer.write(" this.commitBuffer(); } catch");
            case BACKTICK -> flushHtmlBuffer();
            case NEWLINE -> {
                if (seenTemplateToken) {
                    writer.write(".html(\"\\n\");");
                    seenTemplateToken = false;
                }
                if (htmlCache.isEmpty()) {
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
        if (!htmlCache.isEmpty()) {
            String indent = getIndent(htmlCache.getFirst());
            if (htmlCache.size() == 1) {
                writer.write(indent);
                writer.write("html(\"" + escape(htmlCache.getFirst().strip()) + "\");");
            } else {
                writer.write(indent);
                writer.write("html(\"\"\"\n");
                for (String line : htmlCache) {
                    writer.write(line);
                    writer.write("\n");
                }
                writer.write(indent);
                writer.write("\"\"\");\n");
            }
            htmlCache.clear();
        }
    }

    private String getIndent(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) ++i;
        return line.substring(0, i);
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
