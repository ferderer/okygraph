package dev.okygraph.maven.transpiler;

import dev.okygraph.maven.tokenizer.Token;
import dev.okygraph.maven.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Transpiles Okygraph template tokens into Java source code.
 *
 * <p>The transpiler operates on a token stream and uses backticks (`) as mode switches:
 * <ul>
 *   <li><b>Outside backticks:</b> Pure Java code - passed through as-is</li>
 *   <li><b>Inside backticks:</b> Template mode - HTML → writeRaw(), {expr} → write()</li>
 * </ul>
 *
 * <p>Optimizations:
 * <ul>
 *   <li>Multiple consecutive HTML lines → Combined into Java text blocks</li>
 *   <li>Tokens on same line → Chained method calls</li>
 *   <li>Try/catch in templates → Wrapped with pushBuffer()/commit()/discard()</li>
 * </ul>
 *
 * @author Vadim Ferderer
 */
public class TemplateTranspiler {

    /**
     * Transpiles token stream into Java source code.
     *
     * @param tokens Token stream from tokenizer
     * @return Generated Java source code
     */
    public String transpile(List<Token> tokens) {
        StringBuilder out = new StringBuilder();
        boolean inTemplate = false;

        List<Token> htmlBuffer = new ArrayList<>();
        int currentLine = -1;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Skip EOF
            if (token.type() == TokenType.EOF) {
                break;
            }

            // Handle backtick - toggle template mode
            if (token.type() == TokenType.BACKTICK) {
                // Flush any buffered HTML before mode switch
                if (!htmlBuffer.isEmpty()) {
                    emitHtmlBuffer(out, htmlBuffer);
                    htmlBuffer.clear();
                }

                inTemplate = !inTemplate;
                continue;
            }

            if (inTemplate) {
                // ===== TEMPLATE MODE =====

                if (token.type() == TokenType.HTML_TEXT) {
                    // Buffer HTML for potential text block combining
                    htmlBuffer.add(token);

                } else if (token.type() == TokenType.EXPRESSION_START) {
                    // Flush HTML buffer before expression
                    if (!htmlBuffer.isEmpty()) {
                        emitHtmlBuffer(out, htmlBuffer);
                        htmlBuffer.clear();
                    }

                    // Extract expression content between { and }
                    StringBuilder expr = new StringBuilder();
                    i++; // Skip the {
                    int depth = 1;

                    while (i < tokens.size() && depth > 0) {
                        Token exprToken = tokens.get(i);

                        if (exprToken.type() == TokenType.EXPRESSION_END) {
                            depth--;
                            if (depth == 0) break;
                        } else if (exprToken.type() == TokenType.SEPARATOR &&
                                   exprToken.value().equals("{")) {
                            depth++;
                        }

                        // Append token value (whitespace-sensitive)
                        expr.append(exprToken.value());
                        i++;
                    }

                    // Emit write() call
                    out.append("        write(").append(expr.toString().trim()).append(");\n");

                } else if (token.type() == TokenType.NEWLINE) {
                    // Track newlines for HTML buffer grouping
                    if (!htmlBuffer.isEmpty()) {
                        htmlBuffer.add(token);
                    }

                } else if (token.type() == TokenType.WHITESPACE) {
                    // Include whitespace in HTML buffer if we're collecting HTML
                    if (!htmlBuffer.isEmpty()) {
                        htmlBuffer.add(token);
                    }
                }

            } else {
                // ===== JAVA MODE =====

                // Flush any buffered HTML (shouldn't happen, but safety)
                if (!htmlBuffer.isEmpty()) {
                    emitHtmlBuffer(out, htmlBuffer);
                    htmlBuffer.clear();
                }

                // Pass through all Java tokens as-is
                out.append(token.value());
            }
        }

        // Flush any remaining HTML buffer
        if (!htmlBuffer.isEmpty()) {
            emitHtmlBuffer(out, htmlBuffer);
        }

        return out.toString();
    }

    /**
     * Emit buffered HTML as writeRaw() call(s).
     * Combines consecutive HTML into text blocks where possible.
     */
    private void emitHtmlBuffer(StringBuilder out, List<Token> buffer) {
        if (buffer.isEmpty()) return;

        // Check if buffer contains multiple lines
        boolean hasMultipleLines = buffer.stream()
            .anyMatch(t -> t.type() == TokenType.NEWLINE);

        if (hasMultipleLines) {
            // Use text block for multi-line HTML
            out.append("        writeRaw(\"\"\"\n");

            for (Token token : buffer) {
                if (token.type() == TokenType.HTML_TEXT) {
                    out.append("            ").append(token.value()).append("\n");
                } else if (token.type() == TokenType.WHITESPACE) {
                    // Preserve whitespace within HTML
                    out.append(token.value());
                }
                // Skip NEWLINE tokens - already handled by line structure
            }

            out.append("            \"\"\");\n");

        } else {
            // Single line - use simple string literal
            StringBuilder html = new StringBuilder();
            for (Token token : buffer) {
                if (token.type() == TokenType.HTML_TEXT || token.type() == TokenType.WHITESPACE) {
                    html.append(token.value());
                }
            }

            out.append("        writeRaw(")
               .append(escapeJavaString(html.toString()))
               .append(");\n");
        }
    }

    /**
     * Escape string for Java string literal.
     */
    private String escapeJavaString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
