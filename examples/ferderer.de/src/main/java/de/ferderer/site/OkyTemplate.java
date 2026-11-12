package de.ferderer.site;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

public abstract class OkyTemplate implements View {

    private static final Logger log = LoggerFactory.getLogger(OkyTemplate.class);

    /** What an open try block has written so far; it knows the writer to return to and the block around it. */
    private static final class Buffer {
        final StringWriter content = new StringWriter();
        final PrintWriter writer = new PrintWriter(content);
        final PrintWriter parent;
        final Buffer enclosing;

        Buffer(PrintWriter parent, Buffer enclosing) {
            this.parent = parent;
            this.enclosing = enclosing;
        }
    }

    private Buffer open; // the innermost open try block; most templates have none
    private PrintWriter out;
    protected HttpServletRequest  request;
    protected HttpServletResponse response;

    @Override
    public String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setContentType(MediaType.TEXT_HTML_VALUE);
        res.setCharacterEncoding("UTF-8");
        this.request  = req;
        this.response = res;
        open = null;
        out = res.getWriter();
        long start = System.nanoTime();
        render();
        log.debug("Rendered {} in {} µs", getClass().getSimpleName(), (System.nanoTime() - start) / 1_000);
    }

    protected abstract void render() throws Exception;

    // ── Output ───────────────────────────────────────────────────────────────

    /** Raw HTML — no escaping. Used by transpiler for literal template content. */
    public OkyTemplate html(String s) {
        out.write(s);
        return this;
    }

    /** Expression output — routes to the correct overload by type. */
    public OkyTemplate write(int v)     { out.print(v);  return this; }
    public OkyTemplate write(long v)    { out.print(v);  return this; }
    public OkyTemplate write(double v)  { out.print(v);  return this; }
    public OkyTemplate write(float v)   { out.print(v);  return this; }
    public OkyTemplate write(boolean v) { out.print(v);  return this; }
    public OkyTemplate write(char v)    { out.print(v);  return this; }

    /** String — HTML-escaped. */
    public OkyTemplate write(String s) {
        if (s != null) out.write(escape(s));
        return this;
    }

    /** Raw — skips escaping. Use raw("...") for trusted HTML fragments. */
    public OkyTemplate write(Raw raw) {
        if (raw != null) out.write(raw.value());
        return this;
    }

    /** Content slot — renders a lambda into the current output position. */
    public OkyTemplate write(Content content) {
        if (content != null) content.render();
        return this;
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    protected static Raw raw(String s) { return new Raw(s); }

    /** Value inside a quoted HTML attribute: escapes &, <, > and both quote characters. Use {attr(x)} there instead of {x}. */
    protected static Raw attr(String s) {
        return s == null ? null : new Raw(escape(s).replace("\"", "&quot;").replace("'", "&#39;"));
    }

    public record Raw(String value) {}

    @FunctionalInterface
    public interface Content {
        void render();
    }

    // ── Writer stack (try/catch injection) ───────────────────────────────────

    protected void pushBuffer() {
        open = new Buffer(out, open);
        out = open.writer;
    }

    protected void commitBuffer() throws IOException {
        Buffer buffer = open;
        open = buffer.enclosing;
        out = buffer.parent;
        out.write(buffer.content.toString());
    }

    protected void discardBuffer() {
        out = open.parent;
        open = open.enclosing;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
