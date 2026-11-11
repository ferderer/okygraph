package de.ferderer.site;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

public abstract class OkyTemplate implements View {

    private static final Logger log = LoggerFactory.getLogger(OkyTemplate.class);

    private final Deque<Object> writerStack = new ArrayDeque<>(); // Writer | StringWriter
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
        writerStack.clear();
        out = new PrintWriter(res.getWriter(), true);
        writerStack.push(out);
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

    public record Raw(String value) {}

    @FunctionalInterface
    public interface Content {
        void render();
    }

    // ── Writer stack (try/catch injection) ───────────────────────────────────

    protected void pushBuffer() {
        StringWriter sw = new StringWriter();
        writerStack.push(sw);
        out = new PrintWriter(sw, true);
    }

    protected void commitBuffer() throws IOException {
        StringWriter completed = (StringWriter) writerStack.pop();
        out = new PrintWriter((java.io.Writer) writerStack.peek(), true);
        out.write(completed.toString());
    }

    protected void discardBuffer() {
        writerStack.pop();
        out = new PrintWriter((java.io.Writer) writerStack.peek(), true);
    }

    // ── Formatters ───────────────────────────────────────────────────────────

    protected String formatDate(LocalDate date, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(date);
    }

    protected String formatDateTime(Instant instant, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDateTime(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(LocalDateTime.ofInstant(instant, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    protected String formatTime(Instant instant, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedTime(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(LocalTime.ofInstant(instant, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    protected String format(Double number, int minDecimals, int maxDecimals) {
        NumberFormat nf = NumberFormat.getInstance(LocaleContextHolder.getLocale());
        nf.setMinimumFractionDigits(minDecimals);
        nf.setMaximumFractionDigits(maxDecimals);
        return nf.format(number == null ? 0.0 : number);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
