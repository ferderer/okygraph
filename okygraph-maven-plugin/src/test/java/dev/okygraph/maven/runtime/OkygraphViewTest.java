package dev.okygraph.maven.runtime;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OkygraphView base class.
 */
class OkygraphViewTest {

    // ========================================================================
    // Basic Rendering Tests
    // ========================================================================

    @Test
    void testRenderToString() throws IOException {
        var view = new TestView("Hello, World!");
        String result = view.renderToString();
        assertEquals("Hello, World!", result);
    }

    @Test
    void testRenderToWriter() throws IOException {
        var view = new TestView("Test content");
        var sw = new StringWriter();
        view.render(sw);
        assertEquals("Test content", sw.toString());
    }

    @Test
    void testRenderToOutputStream() throws IOException {
        var view = new TestView("Output stream test");
        var baos = new ByteArrayOutputStream();
        view.render(baos);
        String result = baos.toString(StandardCharsets.UTF_8);
        assertEquals("Output stream test", result);
    }

    @Test
    void testNullContent() throws IOException {
        var view = new TestView(null);
        String result = view.renderToString();
        assertEquals("", result);
    }


    // ========================================================================
    // Primitive Write Tests
    // ========================================================================

    @Test
    void testWriteInt() throws IOException {
        var view = new PrimitiveTestView();
        String result = view.renderToString();
        assertTrue(result.contains("42"));
    }

    @Test
    void testWriteLong() throws IOException {
        var view = new PrimitiveTestView();
        String result = view.renderToString();
        assertTrue(result.contains("9876543210"));
    }

    @Test
    void testWriteDouble() throws IOException {
        var view = new PrimitiveTestView();
        String result = view.renderToString();
        assertTrue(result.contains("3.14"));
    }

    @Test
    void testWriteBoolean() throws IOException {
        var view = new PrimitiveTestView();
        String result = view.renderToString();
        assertTrue(result.contains("true"));
    }

    @Test
    void testWriteChar() throws IOException {
        var view = new CharTestView('A');
        assertEquals("A", view.renderToString());
    }

    @Test
    void testWriteChar_needsEscaping() throws IOException {
        var view = new CharTestView('<');
        assertEquals("&lt;", view.renderToString());
    }


    // ========================================================================
    // HTML Escaping Tests
    // ========================================================================

    @Test
    void testEscapeHtml_basicEntities() {
        assertEquals("&lt;div&gt;", OkygraphView.escapeHtml("<div>"));
        assertEquals("&amp;", OkygraphView.escapeHtml("&"));
        assertEquals("&quot;", OkygraphView.escapeHtml("\""));
        assertEquals("&#39;", OkygraphView.escapeHtml("'"));
    }

    @Test
    void testEscapeHtml_combined() {
        String input = "<script>alert('XSS & \"injection\"')</script>";
        String expected = "&lt;script&gt;alert(&#39;XSS &amp; &quot;injection&quot;&#39;)&lt;/script&gt;";
        assertEquals(expected, OkygraphView.escapeHtml(input));
    }

    @Test
    void testEscapeHtml_noEscapeNeeded() {
        String input = "Hello World 123";
        assertSame(input, OkygraphView.escapeHtml(input)); // Should return same instance
    }

    @Test
    void testEscapeHtml_null() {
        assertNull(OkygraphView.escapeHtml(null));
    }

    @Test
    void testEscapeHtml_empty() {
        String empty = "";
        assertSame(empty, OkygraphView.escapeHtml(empty));
    }

    @Test
    void testEscapeMethod_withNull() throws IOException {
        var view = new EscapeTestView(null);
        assertEquals("", view.renderToString());
    }

    @Test
    void testEscapeMethod_withString() throws IOException {
        var view = new EscapeTestView("<script>alert('xss')</script>");
        String result = view.renderToString();
        assertTrue(result.contains("&lt;script&gt;"));
        assertFalse(result.contains("<script>"));
    }

    @Test
    void testEscapeMethod_withNumber() throws IOException {
        var view = new EscapeTestView(42);
        assertEquals("42", view.renderToString());
    }


    // ========================================================================
    // XSS Prevention Tests
    // ========================================================================

    @Test
    void testXssPrevention_scriptTag() throws IOException {
        String malicious = "<script>alert('XSS')</script>";
        var view = new EscapeTestView(malicious);
        String result = view.renderToString();

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;script&gt;"));
    }

    @Test
    void testXssPrevention_htmlInjection() throws IOException {
        String malicious = "\"><img src=x onerror=alert('XSS')>";
        var view = new EscapeTestView(malicious);
        String result = view.renderToString();

        assertFalse(result.contains("<img"));
        assertTrue(result.contains("&quot;&gt;&lt;img"));
    }

    @Test
    void testXssPrevention_eventHandler() throws IOException {
        String malicious = "' onload='alert(1)'";
        var view = new EscapeTestView(malicious);
        String result = view.renderToString();

        // The single quotes are escaped, preventing the attack
        assertFalse(result.contains("' onload='"));
        assertTrue(result.contains("&#39;"));
    }


    // ========================================================================
    // Raw HTML Tests
    // ========================================================================

    @Test
    void testRaw_bypassesEscaping() throws IOException {
        var view = new RawTestView("<strong>Bold</strong>");
        String result = view.renderToString();
        assertEquals("<strong>Bold</strong>", result);
    }

    @Test
    void testRaw_dangerous() throws IOException {
        var view = new RawTestView("<script>alert('danger')</script>");
        String result = view.renderToString();
        // Raw bypasses escaping - this is intentional but dangerous
        assertEquals("<script>alert('danger')</script>", result);
    }


    // ========================================================================
    // JavaScript Escaping Tests
    // ========================================================================

    @Test
    void testJs_quotes() throws IOException {
        var view = new JsTestView("He said \"Hello\"");
        assertEquals("He said \\\"Hello\\\"", view.renderToString());
    }

    @Test
    void testJs_singleQuotes() throws IOException {
        var view = new JsTestView("It's working");
        assertEquals("It\\'s working", view.renderToString());
    }

    @Test
    void testJs_backslash() throws IOException {
        var view = new JsTestView("C:\\Users\\test");
        assertEquals("C:\\\\Users\\\\test", view.renderToString());
    }

    @Test
    void testJs_controlCharacters() throws IOException {
        var view = new JsTestView("Line1\nLine2\rLine3\tTabbed");
        assertEquals("Line1\\nLine2\\rLine3\\tTabbed", view.renderToString());
    }

    @Test
    void testJs_null() throws IOException {
        var view = new JsTestView(null);
        assertEquals("", view.renderToString());
    }

    @Test
    void testJs_noEscapeNeeded() throws IOException {
        String plain = "Hello World 123";
        var view = new JsTestView(plain);
        assertEquals(plain, view.renderToString());
    }


    // ========================================================================
    // URL Encoding Tests
    // ========================================================================

    @Test
    void testUrl_spaces() throws IOException {
        var view = new UrlTestView("hello world");
        assertEquals("hello+world", view.renderToString());
    }

    @Test
    void testUrl_specialCharacters() throws IOException {
        var view = new UrlTestView("user@example.com");
        assertEquals("user%40example.com", view.renderToString());
    }

    @Test
    void testUrl_unicode() throws IOException {
        var view = new UrlTestView("Héllo Wörld");
        String result = view.renderToString();
        assertTrue(result.contains("%C3%A9")); // é encoded
        assertTrue(result.contains("%C3%B6")); // ö encoded
    }

    @Test
    void testUrl_null() throws IOException {
        var view = new UrlTestView(null);
        assertEquals("", view.renderToString());
    }


    // ========================================================================
    // CSS Escaping Tests
    // ========================================================================

    @Test
    void testCss_specialCharacters() throws IOException {
        var view = new CssTestView("user:hover");
        String result = view.renderToString();
        assertTrue(result.contains("\\"));
    }

    @Test
    void testCss_brackets() throws IOException {
        var view = new CssTestView("data[value]");
        String result = view.renderToString();
        assertTrue(result.contains("\\"));
    }

    @Test
    void testCss_null() throws IOException {
        var view = new CssTestView(null);
        assertEquals("", view.renderToString());
    }

    @Test
    void testCss_safe() throws IOException {
        var view = new CssTestView("user-profile");
        assertEquals("user-profile", view.renderToString());
    }


    // ========================================================================
    // Try/Catch Buffering Tests
    // ========================================================================

    @Test
    void testBuffering_commitSuccess() throws IOException {
        var view = new BufferingTestView(false, "Success");
        String result = view.renderToString();
        assertEquals("Before\nSuccess\nAfter", result);
    }

    @Test
    void testBuffering_discardOnException() throws IOException {
        var view = new BufferingTestView(true, "Should not appear");
        String result = view.renderToString();
        assertEquals("Before\nCaught\nAfter", result);
        assertFalse(result.contains("Should not appear"));
    }

    @Test
    void testBuffering_nestedBuffers() throws IOException {
        var view = new NestedBufferingTestView(false, false);
        String result = view.renderToString();
        assertEquals("Start\nOuter start\nInner\nOuter end\nEnd", result);
    }

    @Test
    void testBuffering_nestedWithInnerException() throws IOException {
        var view = new NestedBufferingTestView(false, true);
        String result = view.renderToString();
        assertEquals("Start\nOuter start\nInner caught\nOuter end\nEnd", result);
    }

    @Test
    void testBuffering_nestedWithOuterException() throws IOException {
        var view = new NestedBufferingTestView(true, false);
        String result = view.renderToString();
        assertEquals("Start\nOuter caught\nEnd", result);
    }


    // ========================================================================
    // Custom Escape Override Tests
    // ========================================================================

    @Test
    void testCustomEscape_override() throws IOException {
        var view = new CustomEscapeView("test");
        String result = view.renderToString();
        assertEquals("CUSTOM[test]", result);
    }


    // ========================================================================
    // Test Helper Views
    // ========================================================================

    /**
     * Simple view that writes a string.
     */
    static class TestView extends OkygraphView {
        private final String content;

        TestView(String content) {
            this.content = content;
        }

        @Override
        protected void render() throws IOException {
            writeRaw(content);
        }
    }

    /**
     * View that tests primitive types.
     */
    static class PrimitiveTestView extends OkygraphView {
        @Override
        protected void render() throws IOException {
            write(42);
            writeRaw(",");
            write(9876543210L);
            writeRaw(",");
            write(3.14);
            writeRaw(",");
            write(true);
        }
    }

    /**
     * View that tests character writing.
     */
    static class CharTestView extends OkygraphView {
        private final char c;

        CharTestView(char c) {
            this.c = c;
        }

        @Override
        protected void render() throws IOException {
            write(c);
        }
    }

    /**
     * View that escapes a value.
     */
    static class EscapeTestView extends OkygraphView {
        private final Object value;

        EscapeTestView(Object value) {
            this.value = value;
        }

        @Override
        protected void render() throws IOException {
            // With new API, write(String) automatically escapes
            if (value instanceof String) {
                write((String) value);
            } else if (value != null) {
                write(value.toString());
            }
        }
    }

    /**
     * View that uses raw() to bypass escaping.
     */
    static class RawTestView extends OkygraphView {
        private final String html;

        RawTestView(String html) {
            this.html = html;
        }

        @Override
        protected void render() throws IOException {
            // Transpiler generates: write(raw(html))
            // NOT: write(escape(raw(html)))
            write(raw(html));
        }
    }

    /**
     * View that tests JavaScript escaping.
     */
    static class JsTestView extends OkygraphView {
        private final String text;

        JsTestView(String text) {
            this.text = text;
        }

        @Override
        protected void render() throws IOException {
            // js() returns an already-escaped string, write raw
            writeRaw(js(text));
        }
    }

    /**
     * View that tests URL encoding.
     */
    static class UrlTestView extends OkygraphView {
        private final String text;

        UrlTestView(String text) {
            this.text = text;
        }

        @Override
        protected void render() throws IOException {
            // url() returns an already-encoded string, write raw
            writeRaw(url(text));
        }
    }

    /**
     * View that tests CSS escaping.
     */
    static class CssTestView extends OkygraphView {
        private final String text;

        CssTestView(String text) {
            this.text = text;
        }

        @Override
        protected void render() throws IOException {
            // css() returns an already-escaped string, write raw
            writeRaw(css(text));
        }
    }

    /**
     * View that tests try/catch buffering.
     */
    static class BufferingTestView extends OkygraphView {
        private final boolean shouldThrow;
        private final String content;

        BufferingTestView(boolean shouldThrow, String content) {
            this.shouldThrow = shouldThrow;
            this.content = content;
        }

        @Override
        protected void render() throws IOException {
            writeRaw("Before\n");
            try {
                pushBuffer();
                writeRaw(content + "\n");
                if (shouldThrow) {
                    throw new RuntimeException("Test exception");
                }
                popBufferCommit();
            } catch (RuntimeException e) {
                popBufferDiscard();
                writeRaw("Caught\n");
            }
            writeRaw("After");
        }
    }

    /**
     * View that tests nested buffering.
     */
    static class NestedBufferingTestView extends OkygraphView {
        private final boolean outerThrows;
        private final boolean innerThrows;

        NestedBufferingTestView(boolean outerThrows, boolean innerThrows) {
            this.outerThrows = outerThrows;
            this.innerThrows = innerThrows;
        }

        @Override
        protected void render() throws IOException {
            writeRaw("Start\n");
            try {
                pushBuffer();
                writeRaw("Outer start\n");

                // Nested try block
                try {
                    pushBuffer();
                    writeRaw("Inner\n");
                    if (innerThrows) {
                        throw new RuntimeException("Inner exception");
                    }
                    popBufferCommit();
                } catch (RuntimeException e) {
                    popBufferDiscard();
                    writeRaw("Inner caught\n");
                }

                writeRaw("Outer end\n");
                if (outerThrows) {
                    throw new RuntimeException("Outer exception");
                }
                popBufferCommit();
            } catch (RuntimeException e) {
                popBufferDiscard();
                writeRaw("Outer caught\n");
            }
            writeRaw("End");
        }
    }

    /**
     * View with custom escape logic.
     */
    static class CustomEscapeView extends OkygraphView {
        private final String value;

        CustomEscapeView(String value) {
            this.value = value;
        }

        @Override
        protected String escape(Object value) {
            // Custom escape - prepend CUSTOM[]
            return "CUSTOM[" + value + "]";
        }

        @Override
        protected void render() throws IOException {
            // write(String) calls escape() internally
            write(value);
        }
    }
}
