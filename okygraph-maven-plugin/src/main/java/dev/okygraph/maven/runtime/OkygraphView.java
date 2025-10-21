package dev.okygraph.maven.runtime;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Base class for all Okygraph template views.
 *
 * <p>Generated template classes extend this to gain access to rendering capabilities,
 * HTML escaping, and try/catch buffering support.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class UserProfileView extends OkygraphView {
 *     private final User user;
 *
 *     public UserProfileView(User user) {
 *         this.user = user;
 *     }
 *
 *     @Override
 *     protected void render() throws IOException {
 *         write("<h1>");
 *         write(escape(user.getName()));
 *         write("</h1>");
 *     }
 * }
 *
 * // Render to string
 * String html = new UserProfileView(user).renderToString();
 *
 * // Render to response
 * new UserProfileView(user).render(response.getOutputStream());
 * }</pre>
 *
 * <h2>XSS Protection</h2>
 * <p>All expressions are HTML-escaped by default using {@link #escape(Object)}.
 * Override this method to customize escaping behavior.</p>
 *
 * <h2>Raw HTML</h2>
 * <p>Use {@link #raw(String)} to bypass escaping for trusted HTML content:</p>
 * <pre>{@code
 * write(escape(raw(trustedHtmlContent)));
 * }</pre>
 *
 * <h2>Try/Catch Buffering</h2>
 * <p>The transpiler automatically manages output buffering in try/catch blocks
 * to prevent partial output on exceptions.</p>
 */
public abstract class OkygraphView {

    /** Current writer - used by generated template code */
    protected Writer w;

    /** Stack of writers for try/catch buffering */
    private final Deque<Writer> writerStack = new ArrayDeque<>();


    // ========================================================================
    // Abstract Method - Implemented by Generated Subclasses
    // ========================================================================

    /**
     * Renders the template content.
     *
     * <p>This method is implemented by generated template classes and should not
     * be called directly. Use {@link #render(Writer)}, {@link #render(OutputStream)},
     * or {@link #renderToString()} instead.</p>
     *
     * @throws IOException if an I/O error occurs during rendering
     */
    protected abstract void render() throws IOException;


    // ========================================================================
    // Public Rendering API
    // ========================================================================

    /**
     * Renders the template to the specified writer.
     *
     * @param writer the target writer
     * @throws IOException if an I/O error occurs during rendering
     */
    public void render(Writer writer) throws IOException {
        this.w = writer;
        try {
            render();
        } finally {
            this.w = null;
            this.writerStack.clear();
        }
    }

    /**
     * Renders the template to the specified output stream using UTF-8 encoding.
     *
     * @param out the target output stream
     * @throws IOException if an I/O error occurs during rendering
     */
    public void render(OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        render(writer);
        writer.flush();
    }

    /**
     * Renders the template to a string.
     *
     * @return the rendered HTML as a string
     * @throws IOException if an I/O error occurs during rendering
     */
    public String renderToString() throws IOException {
        StringWriter sw = new StringWriter();
        render(sw);
        return sw.toString();
    }


    // ========================================================================
    // Write Methods - Used by Generated Code
    // ========================================================================

    /**
     * Writes a string to the output with HTML escaping.
     *
     * <p>This is the main write method used by generated code for expressions.
     * The string is automatically HTML-escaped using {@link #escape(Object)}.</p>
     *
     * <p>Generated code: {@code write(user.getName())}</p>
     *
     * @param text the text to write (null values become empty string)
     * @throws IOException if an I/O error occurs
     */
    protected void write(String text) throws IOException {
        w.write(escape(text));
    }

    /**
     * Writes an integer to the output without escaping.
     *
     * <p>Numbers don't need escaping, so they're written directly.</p>
     *
     * @param value the integer value to write
     * @throws IOException if an I/O error occurs
     */
    protected void write(int value) throws IOException {
        w.write(String.valueOf(value));
    }

    /**
     * Writes a long to the output without escaping.
     *
     * @param value the long value to write
     * @throws IOException if an I/O error occurs
     */
    protected void write(long value) throws IOException {
        w.write(String.valueOf(value));
    }

    /**
     * Writes a double to the output without escaping.
     *
     * @param value the double value to write
     * @throws IOException if an I/O error occurs
     */
    protected void write(double value) throws IOException {
        w.write(String.valueOf(value));
    }

    /**
     * Writes a boolean to the output without escaping.
     *
     * @param value the boolean value to write
     * @throws IOException if an I/O error occurs
     */
    protected void write(boolean value) throws IOException {
        w.write(String.valueOf(value));
    }

    /**
     * Writes a character to the output with HTML escaping.
     *
     * @param c the character to write
     * @throws IOException if an I/O error occurs
     */
    protected void write(char c) throws IOException {
        w.write(escape(String.valueOf(c)));
    }

    /**
     * Writes a raw object returned by {@link #raw(String)}.
     *
     * <p>This overload bypasses HTML escaping. The {@link Raw} wrapper
     * indicates the content has already been validated as safe HTML.</p>
     *
     * <p>Generated code: {@code write(raw(trustedHtml))}</p>
     *
     * @param raw the raw HTML wrapper
     * @throws IOException if an I/O error occurs
     */
    protected void write(Raw raw) throws IOException {
        if (raw != null && raw.content != null) {
            w.write(raw.content);
        }
    }

    /**
     * Writes raw HTML literal text without escaping.
     *
     * <p>This method is used internally by generated code for HTML literals
     * in the template. It's package-private to prevent misuse.</p>
     *
     * @param text the literal HTML text
     * @throws IOException if an I/O error occurs
     */
    void writeRaw(String text) throws IOException {
        if (text != null) {
            w.write(text);
        }
    }


    // ========================================================================
    // Escaping Methods
    // ========================================================================

    /**
     * Escapes a value for safe output in HTML context.
     *
     * <p>The default implementation performs HTML entity escaping. Override this
     * method to customize escaping behavior, for example to handle custom types:</p>
     *
     * <pre>{@code
     * @Override
     * protected String escape(Object value) {
     *     if (value instanceof SafeHtml) {
     *         return value.toString(); // Already safe
     *     }
     *     return super.escape(value);
     * }
     * }</pre>
     *
     * @param value the value to escape (null becomes empty string)
     * @return the escaped string, safe for HTML output
     */
    protected String escape(Object value) {
        if (value == null) {
            return "";
        }
        return escapeHtml(String.valueOf(value));
    }

    /**
     * Performs HTML entity escaping on a string.
     *
     * <p>Escapes the following characters:
     * <ul>
     *   <li>{@code &} to {@code &amp;}</li>
     *   <li>{@code <} to {@code &lt;}</li>
     *   <li>{@code >} to {@code &gt;}</li>
     *   <li>{@code "} to {@code &quot;}</li>
     *   <li>{@code '} to {@code &#39;}</li>
     * </ul>
     *
     * @param text the text to escape
     * @return the escaped text
     */
    protected static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = null; // Lazy allocation
        int len = text.length();
        int lastWritten = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            String escaped = switch (c) {
                case '&'  -> "&amp;";
                case '<'  -> "&lt;";
                case '>'  -> "&gt;";
                case '"'  -> "&quot;";
                case '\'' -> "&#39;";
                default   -> null;
            };

            if (escaped != null) {
                if (sb == null) {
                    sb = new StringBuilder(len + 16);
                }
                sb.append(text, lastWritten, i);
                sb.append(escaped);
                lastWritten = i + 1;
            }
        }

        if (sb == null) {
            return text; // No escaping needed
        }

        sb.append(text, lastWritten, len);
        return sb.toString();
    }


    // ========================================================================
    // Helper Methods for Users
    // ========================================================================

    /**
     * Wrapper for raw HTML content that bypasses escaping.
     */
    protected static class Raw {
        final String content;

        Raw(String content) {
            this.content = content;
        }
    }

    /**
     * Marks HTML content as safe, bypassing HTML escaping.
     *
     * <p><strong>WARNING:</strong> This is dangerous if used with untrusted content.
     * Only use with HTML that you trust completely.</p>
     *
     * <p>Usage in templates:</p>
     * <pre>{@code
     * {raw(trustedHtmlContent)}
     * }</pre>
     *
     * @param html the HTML string to pass through
     * @return a Raw wrapper that bypasses escaping
     */
    protected Raw raw(String html) {
        return new Raw(html);
    }

    /**
     * Escapes a string for use in JavaScript string literals.
     *
     * <p>Escapes backslashes, quotes, and control characters. Use this when
     * embedding Java values in JavaScript code:</p>
     *
     * <pre>{@code
     * <script>
     *   var userName = "{js(user.name)}";
     * </script>
     * }</pre>
     *
     * @param text the text to escape
     * @return the JavaScript-safe string
     */
    protected String js(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = null;
        int len = text.length();
        int lastWritten = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            String escaped = switch (c) {
                case '\\' -> "\\\\";
                case '"'  -> "\\\"";
                case '\'' -> "\\'";
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                case '\b' -> "\\b";
                case '\f' -> "\\f";
                default   -> null;
            };

            if (escaped != null) {
                if (sb == null) {
                    sb = new StringBuilder(len + 16);
                }
                sb.append(text, lastWritten, i);
                sb.append(escaped);
                lastWritten = i + 1;
            }
        }

        if (sb == null) {
            return text;
        }

        sb.append(text, lastWritten, len);
        return sb.toString();
    }

    /**
     * URL-encodes a string for use in URL query parameters.
     *
     * <p>Uses UTF-8 encoding. Example usage:</p>
     * <pre>{@code
     * <a href="/search?q={url(searchTerm)}">Search</a>
     * }</pre>
     *
     * @param text the text to encode
     * @return the URL-encoded string
     */
    protected String url(String text) {
        if (text == null) {
            return "";
        }
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /**
     * Escapes a string for use in CSS.
     *
     * <p>Escapes characters that have special meaning in CSS:</p>
     * <pre>{@code
     * <style>
     *   .user-{css(user.id)} { color: red; }
     * </style>
     * }</pre>
     *
     * @param text the text to escape
     * @return the CSS-safe string
     */
    protected String css(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = null;
        int len = text.length();
        int lastWritten = 0;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            boolean needsEscape = false;

            // Escape special CSS characters and control characters
            if (c < 0x20 || c == '\\' || c == '"' || c == '\'' ||
                c == '<' || c == '>' || c == '&' || c == '(' || c == ')' ||
                c == '{' || c == '}' || c == '[' || c == ']' || c == ';' || c == ':') {
                needsEscape = true;
            }

            if (needsEscape) {
                if (sb == null) {
                    sb = new StringBuilder(len + 16);
                }
                sb.append(text, lastWritten, i);
                sb.append('\\').append(String.format("%x", (int) c)).append(' ');
                lastWritten = i + 1;
            }
        }

        if (sb == null) {
            return text;
        }

        sb.append(text, lastWritten, len);
        return sb.toString();
    }


    // ========================================================================
    // Try/Catch Buffering Support
    // ========================================================================

    /**
     * Starts buffering output for try/catch blocks.
     *
     * <p>Called by generated code at the start of a try block to prevent partial
     * output if an exception occurs. The buffered content is either committed
     * via {@link #popBufferCommit()} or discarded via {@link #popBufferDiscard()}.</p>
     *
     * <p>Do not call this method directly - it is managed by the transpiler.</p>
     */
    protected void pushBuffer() {
        writerStack.push(w);
        w = new StringWriter();
    }

    /**
     * Commits the buffered output to the parent writer.
     *
     * <p>Called by generated code at the end of a successful try block.
     * The buffered content is written to the previous writer and the buffer
     * is discarded.</p>
     *
     * <p>Do not call this method directly - it is managed by the transpiler.</p>
     *
     * @throws IOException if an I/O error occurs while committing
     */
    protected void popBufferCommit() throws IOException {
        String content = w.toString();
        w = writerStack.pop();
        w.write(content);
    }

    /**
     * Discards the buffered output without writing it.
     *
     * <p>Called by generated code in catch blocks to discard any partial output
     * from the failed try block.</p>
     *
     * <p>Do not call this method directly - it is managed by the transpiler.</p>
     */
    protected void popBufferDiscard() {
        w = writerStack.pop();
    }
}
