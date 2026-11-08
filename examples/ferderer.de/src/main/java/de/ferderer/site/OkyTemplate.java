package de.ferderer.site;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

public abstract class OkyTemplate implements View {

    private final Deque<Writer> writerStack = new ArrayDeque<>();
    private PrintWriter out;
    protected HttpServletRequest request;
    protected HttpServletResponse response;

    @Override
    public String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> m, HttpServletRequest req, HttpServletResponse res) throws Exception {
        this.request = req;
        this.response = res;
        writerStack.clear();
        writerStack.push(res.getWriter());
        out = new PrintWriter(writerStack.peek(), true);
        render();
    }

    protected abstract void render() throws Exception;

    // === Template Output ===

    // Raw HTML output
    protected OkyTemplate html(String s) throws IOException {
        out.write(s);
        return this;
    }

    // Primitives - no escaping needed
    protected OkyTemplate expr(int v) throws IOException { out.print(v); return this; }
    protected OkyTemplate expr(long v) throws IOException { out.print(v); return this; }
    protected OkyTemplate expr(double v) throws IOException { out.print(v); return this; }
    protected OkyTemplate expr(float v) throws IOException { out.print(v); return this; }
    protected OkyTemplate expr(boolean v) throws IOException { out.print(v); return this; }
    protected OkyTemplate expr(char v) throws IOException { out.print(v); return this; }

    // String - HTML escaped
    protected OkyTemplate expr(String s) throws IOException {
        if (s != null) out.write(escapeHtml(s));
        return this;
    }

    // Raw wrapper - no escaping
    protected OkyTemplate expr(Raw raw) throws IOException {
        if (raw != null) out.write(raw.value());
        return this;
    }

    // Content - for layout composition
    protected OkyTemplate expr(Content slot) throws Exception {
        slot.render();
        return this;
    }

    // === Helpers ===

    protected static Raw raw(String s) {
        return new Raw(s);
    }

    public record Raw(String value) {}

    @FunctionalInterface
    public interface Content {
        void render() throws Exception;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // === Exception Handling - Writer Stack ===

    protected void pushBuffer() {
        writerStack.push(new StringWriter());
        out = new PrintWriter(writerStack.peek(), true);
    }

    protected void commitBuffer() throws IOException {
        StringWriter completed = (StringWriter) writerStack.pop();
        out = new PrintWriter(writerStack.peek(), true);
        out.write(completed.toString());
    }

    protected void discardBuffer() {
        writerStack.pop();
        out = new PrintWriter(writerStack.peek(), true);
    }

    // === Formatters ===

    protected String formatDate(LocalDate date, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(date);
    }

    protected String formatTime(Instant instant, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedTime(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(LocalTime.ofInstant(instant, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    protected String formatDateTime(Instant instant, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDateTime(style)
            .withLocale(LocaleContextHolder.getLocale())
            .format(LocalDateTime.ofInstant(instant, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    protected String format(Double number, int minDecimals, int maxDecimals) {
        NumberFormat nf = NumberFormat.getInstance(LocaleContextHolder.getLocale());
        nf.setMinimumFractionDigits(minDecimals);
        nf.setMaximumFractionDigits(maxDecimals);
        return nf.format(number == null ? 0.0 : number);
    }
}
