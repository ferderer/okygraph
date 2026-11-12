package dev.okygraph.maven;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Transpiler for Okygraph template files (.oky). */
public class Transpiler {
    public record Token(TokenType type, String value, int line, int column) {}

    private final StringBuilder writer = new StringBuilder();
    private ParsingContext context = ParsingContext.JAVA;

    /** CRLF and lone CR become LF; no token pattern matches '\r' (ADR-0111). */
    private static String normalizeLineEndings(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** Unicode escapes as in JLS 3.3: a backslash starts one only if an even number of backslashes precedes it, so "\\u0041" stays. */
    private static String preprocessUnicode(String s) {
        Matcher m = Pattern.compile("(?<!\\\\)((?:\\\\\\\\)*)\\\\u+([0-9a-fA-F]{4})").matcher(s);
        StringBuilder sb = new StringBuilder(s.length());
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start()).append(m.group(1));
            sb.appendCodePoint(Integer.parseInt(m.group(2), 16));
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
        return new Transpiler().transpile(preprocessUnicode(normalizeLineEndings(source)));
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
            int position = saveIndent(line);
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
            if (context == ParsingContext.EXPRESSION) {
                throw new RuntimeException(String.format("Expression not closed at the end of line %d (an expression ends on the line it starts):\n%s", currentLine + 1, line));
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
                // whitespace after the last token of a line is not output; a blank between two tokens, as in {a} {b}, is
                boolean trailingBlank = pattern.type == TokenType.HTML && value.isBlank() && position + value.length() == line.length();
                if (!trailingBlank) {
                    emit(pattern.type, value, line);
                }
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

    /** An open try block: the name of its buffer variable and the indent of its line. Names are numbered by nesting depth. */
    private record Try(String name, String indent) {}

    private final Deque<Try> tries = new ArrayDeque<>();
    private String indent = "";

    /** Captures the current line's leading whitespace and returns where its content starts. */
    private int saveIndent(String line) {
        indent = getIndent(line);
        return indent.length();
    }

    /** Writes the pending indent exactly once; a no-op once drained or explicitly discarded. */
    private StringBuilder writeIndent() {
        writer.append(indent);
        indent = "";
        return writer;
    }

    /** Drains anything pending before new content is written: cached HTML lines, then the line indent. */
    private void flush() throws IOException {
        if (!htmlCache.isEmpty()) {
            String cachedIndent = shallowestIndent();
            if (htmlCache.size() == 1) {
                writer.append(cachedIndent).append("html(\"").append(escape(htmlCache.getFirst().strip())).append("\\n\");\n");
            } else {
                writer.append(cachedIndent).append("html(\"\"\"\n");
                for (String l : htmlCache) {
                    writer.append(escapeBlock(l)).append("\n");
                }
                writer.append(cachedIndent).append("\"\"\");\n");
            }
            htmlCache.clear();
        }
        writeIndent();
    }

    private void emit(TokenType type, String value, String line) throws IOException {
        switch (type) {
            case HTML -> {
                if (value.equals(line.substring(indent.length()))) { // whole line -> defer into the text-block cache
                    htmlCache.add(unescape(line));
                }
                else { // this is a HTML fragment - line with an expression
                    checkFragmentedLine();
                    writer.append("html(\"").append(escape(unescape(value))).append("\")");
                }
            }
            case EXPRESSION_START -> {
                if (context == ParsingContext.CATCH) { // the end of the catch declaration
                    writer.append("{");
                }
                else {
                    checkFragmentedLine();
                    writer.append("write(");
                }
            }
            case EXPRESSION_END -> writer.append(")");
            case TRY -> {
                Try open = new Try("oky" + (tries.size() + 1), indent); // capture the indent before flush() drains it
                tries.push(open);
                flush();
                writer.append("try (var ").append(open.name()).append(" = this.buffer()) {");
            }
            case CATCH -> {
                Try open = tries.pollFirst();
                if (open == null) {
                    throw new IllegalStateException("catch without a try in the template");
                }
                indent = ""; // discard this line's own indent - align with the matching try instead
                flush();
                writer.append(open.indent()).append("    ").append(open.name()).append(".commit();\n").append(open.indent()).append("} catch");
            }
            case BACKTICK -> {
                if (seenTemplateToken) { // the template ends in the middle of a line: close the chain
                    writer.append(";");
                    seenTemplateToken = false;
                }
                flush();
            }
            case NEWLINE -> {
                if (seenTemplateToken) {
                    writer.append(".html(\"\\n\");");
                    seenTemplateToken = false;
                }
                if (htmlCache.isEmpty()) {
                    writer.append("\n");
                }
            }
            default -> {
                flush();
                writer.append(value);
            }
        }
    }

    private void checkFragmentedLine() throws IOException {
        if (seenTemplateToken) { // not the first fragment - do chaining
            writer.append(".");
        } else {
            flush();
        }
        seenTemplateToken = true;
    }

    /** Indent of the least indented cached line: a text block closed deeper than one of its lines makes javac warn. */
    private String shallowestIndent() {
        return htmlCache.stream().map(this::getIndent).min(Comparator.comparingInt(String::length)).get();
    }

    private String getIndent(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) ++i;
        return line.substring(0, i);
    }

    /** In HTML only \` and \{ are escapes; any other backslash is literal. Inside one token each of them follows a backslash. */
    private String unescape(String s) {
        return s.replace("\\`", "`").replace("\\{", "{");
    }

    /** Text block content: Java reads backslashes here, and """ would end the block. */
    private String escapeBlock(String s) {
        return s.replace("\\", "\\\\").replace("\"\"\"", "\"\"\\\"");
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
