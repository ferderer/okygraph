package art.scilife.support.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.servlet.View;

public abstract class BaseView implements View {
    private static final Logger log = LoggerFactory.getLogger(BaseView.class);
    private static final DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

    static {
        df.applyPattern("0.00");
    }

    @Autowired
    private MessageSource messageSource;

    @Autowired
    @Getter
    private HttpServletRequest request;

    @Autowired
    @Getter
    private HttpServletResponse response;

    public BaseView w(String... objects) throws IOException {
        for(String o : objects)
            response.getWriter().write(o);
        return this;
    }

    public BaseView write(String s) throws IOException {
        response.getWriter().write(s);
        return this;
    }

    @Override
    public String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> m, HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.request = request;
        this.response = response;

        long start = System.nanoTime();
        try {
            this.response.setContentType(MediaType.TEXT_HTML_VALUE);
            this.response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            render();
        }
        catch (Exception ex) {
            throw new RuntimeException("E_RENDERING_ERROR", ex);
        }
        log.info("Time: {} ms; Rendered class {} ", df.format((System.nanoTime() - start) / 1_000_000.0),
            getClass().getSimpleName());
    }

    protected abstract void render() throws Exception;

    protected String i18n(@NotNull String code, String ...args) {
        Assert.hasText(code, "i18n code cannot be null!");
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
    }

    protected String language() {
        return LocaleContextHolder.getLocale().getLanguage().toLowerCase();
    }

    public String formatDate(LocalDate date, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDate(style).format(date);
    }

    public String formatTime(Instant i, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedTime(style)
            .format(LocalTime.ofInstant(i, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    public String formatDateTime(Instant i, FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDateTime(style)
            .format(LocalDateTime.ofInstant(i, LocaleContextHolder.getTimeZone().toZoneId()));
    }

    public String format(Double number, int min, int max) {
        NumberFormat nf = NumberFormat.getInstance(LocaleContextHolder.getLocale());
        nf.setMaximumFractionDigits(max);
        nf.setMinimumFractionDigits(min);
        return nf.format(number == null ? 0.0 : number);
    }
}
