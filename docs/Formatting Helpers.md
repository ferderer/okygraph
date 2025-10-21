# Formatting Helpers Design

## Overview

Add common formatting methods to `OkygraphView` for dates, numbers, currencies, etc.

## Design Principles

1. **Optional** - Users don't have to use them
2. **Overridable** - Easy to customize
3. **Locale-aware** - Support internationalization
4. **Zero dependencies** - Use JDK classes only

## Proposed Methods

### Date/Time Formatting

```java
/**
 * Current locale for formatting (thread-local for safety).
 */
private static final ThreadLocal<Locale> currentLocale =
    ThreadLocal.withInitial(() -> Locale.getDefault());

/**
 * Sets the locale for this rendering context.
 */
protected void setLocale(Locale locale) {
    currentLocale.set(locale);
}

/**
 * Gets the current locale.
 */
protected Locale getLocale() {
    return currentLocale.get();
}

/**
 * Formats a date using ISO format (2025-10-20).
 */
protected String formatDate(LocalDate date) {
    if (date == null) return "";
    return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
}

/**
 * Formats a date using a custom pattern.
 */
protected String formatDate(LocalDate date, String pattern) {
    if (date == null) return "";
    return DateTimeFormatter.ofPattern(pattern, getLocale()).format(date);
}

/**
 * Formats a date/time using ISO format (2025-10-20T14:30:00).
 */
protected String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) return "";
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
}

/**
 * Formats a date/time using a custom pattern.
 */
protected String formatDateTime(LocalDateTime dateTime, String pattern) {
    if (dateTime == null) return "";
    return DateTimeFormatter.ofPattern(pattern, getLocale()).format(dateTime);
}

/**
 * Formats a date in locale-specific format.
 */
protected String formatDateLocale(LocalDate date) {
    if (date == null) return "";
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(getLocale())
        .format(date);
}

/**
 * Formats a date/time in locale-specific format.
 */
protected String formatDateTimeLocale(LocalDateTime dateTime) {
    if (dateTime == null) return "";
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(getLocale())
        .format(dateTime);
}
```

### Number Formatting

```java
/**
 * Formats a number with default locale formatting.
 */
protected String formatNumber(Number number) {
    if (number == null) return "";
    return NumberFormat.getInstance(getLocale()).format(number);
}

/**
 * Formats a number with specified decimal places.
 */
protected String formatNumber(Number number, int decimals) {
    if (number == null) return "";
    NumberFormat nf = NumberFormat.getInstance(getLocale());
    nf.setMinimumFractionDigits(decimals);
    nf.setMaximumFractionDigits(decimals);
    return nf.format(number);
}

/**
 * Formats a number as currency using default currency.
 */
protected String formatCurrency(Number amount) {
    if (amount == null) return "";
    return NumberFormat.getCurrencyInstance(getLocale()).format(amount);
}

/**
 * Formats a number as currency with explicit currency code.
 */
protected String formatCurrency(Number amount, String currencyCode) {
    if (amount == null) return "";
    NumberFormat nf = NumberFormat.getCurrencyInstance(getLocale());
    nf.setCurrency(Currency.getInstance(currencyCode));
    return nf.format(amount);
}

/**
 * Formats a number as percentage.
 */
protected String formatPercent(Number number) {
    if (number == null) return "";
    return NumberFormat.getPercentInstance(getLocale()).format(number);
}
```

### String Formatting

```java
/**
 * Truncates a string to max length with ellipsis.
 */
protected String truncate(String text, int maxLength) {
    if (text == null) return "";
    if (text.length() <= maxLength) return text;
    return text.substring(0, maxLength - 3) + "...";
}

/**
 * Capitalizes the first letter of each word.
 */
protected String capitalize(String text) {
    if (text == null || text.isEmpty()) return text;

    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;

    for (char c : text.toCharArray()) {
        if (Character.isWhitespace(c)) {
            capitalizeNext = true;
            result.append(c);
        } else if (capitalizeNext) {
            result.append(Character.toUpperCase(c));
            capitalizeNext = false;
        } else {
            result.append(Character.toLowerCase(c));
        }
    }

    return result.toString();
}

/**
 * Formats a file size in human-readable format.
 */
protected String formatFileSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    char pre = "KMGTPE".charAt(exp - 1);
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
}

/**
 * Pluralizes a word based on count.
 */
protected String pluralize(int count, String singular, String plural) {
    return count == 1 ? singular : plural;
}
```

### Collection Formatting

```java
/**
 * Joins a collection with a delimiter.
 */
protected String join(Collection<?> items, String delimiter) {
    if (items == null || items.isEmpty()) return "";
    return items.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(delimiter));
}

/**
 * Joins a collection with delimiter and "and" for last item.
 */
protected String joinAnd(Collection<?> items, String delimiter) {
    if (items == null || items.isEmpty()) return "";
    List<String> list = items.stream()
        .map(String::valueOf)
        .toList();
    if (list.size() == 1) return list.get(0);
    if (list.size() == 2) return list.get(0) + " and " + list.get(1);

    return String.join(delimiter, list.subList(0, list.size() - 1)) +
           delimiter + "and " + list.get(list.size() - 1);
}
```

## Usage Examples

### In Templates

```html
<!-- Date formatting -->
<p>Published: {formatDate(article.publishDate)}</p>
<p>Updated: {formatDate(article.updateDate, "MMM d, yyyy")}</p>
<p>Event: {formatDateTimeLocale(event.startTime)}</p>

<!-- Number formatting -->
<p>Price: {formatCurrency(product.price)}</p>
<p>Tax: {formatCurrency(order.tax, "EUR")}</p>
<p>Rating: {formatNumber(product.rating, 1)} / 5.0</p>
<p>Discount: {formatPercent(0.15)}</p>

<!-- String formatting -->
<h1>{capitalize(post.title)}</h1>
<p>{truncate(post.excerpt, 150)}</p>
<p>File size: {formatFileSize(file.size)}</p>
<p>{user.itemCount} {pluralize(user.itemCount, "item", "items")}</p>

<!-- Collection formatting -->
<p>Tags: {join(post.tags, ", ")}</p>
<p>Authors: {joinAnd(book.authors, ", ")}</p>
```

### Custom Formatters in Subclasses

```java
public abstract class PageView extends OkygraphView {

    @Override
    protected String formatDate(LocalDate date) {
        // Custom date format for all pages
        if (date == null) return "";
        return DateTimeFormatter.ofPattern("MMMM d, yyyy", getLocale())
            .format(date);
    }

    protected String formatPrice(BigDecimal price) {
        // Custom price formatting
        if (price == null) return "FREE";
        if (price.compareTo(BigDecimal.ZERO) == 0) return "FREE";
        return formatCurrency(price);
    }

    protected String timeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        long seconds = ChronoUnit.SECONDS.between(dateTime, LocalDateTime.now());

        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";

        long days = seconds / 86400;
        if (days == 1) return "yesterday";
        if (days < 30) return days + " days ago";

        return formatDateLocale(dateTime.toLocalDate());
    }
}
```

## Configuration

Users can control locale per request in Spring:

```java
public abstract class SpringPageView extends SpringOkygraphView {

    @Override
    protected void populateFromModel(Map<String, ?> model) {
        // Set locale from Spring's LocaleResolver
        if (model.containsKey("locale")) {
            setLocale((Locale) model.get("locale"));
        }
    }
}

// In controller
@GetMapping("/user/{id}")
public View userProfile(@PathVariable Long id, Locale locale) {
    User user = userService.findById(id);
    UserProfileView view = new UserProfileView(user);
    view.setLocale(locale);
    return view;
}
```

## Testing

```java
@Test
void testFormatDate() throws IOException {
    var view = new FormatTestView();
    view.setLocale(Locale.US);
    String result = view.renderToString();
    // Verify formatted output
}

@Test
void testFormatCurrency_USD() {
    var view = new OkygraphView() {
        protected void render() throws IOException {
            writeRaw(formatCurrency(19.99));
        }
    };
    view.setLocale(Locale.US);
    String result = view.renderToString();
    assertTrue(result.contains("$19.99"));
}

@Test
void testFormatCurrency_EUR() {
    var view = new OkygraphView() {
        protected void render() throws IOException {
            writeRaw(formatCurrency(19.99, "EUR"));
        }
    };
    view.setLocale(Locale.GERMANY);
    String result = view.renderToString();
    assertTrue(result.contains("€"));
}
```

## Import Requirements

```java
import java.time.*;
import java.time.format.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;
```

## Alternative: External Dependency

For more advanced formatting, users could add:

```xml
<dependency>
    <groupId>org.ocpsoft.prettytime</groupId>
    <artifactId>prettytime</artifactId>
    <version>5.0.7.Final</version>
</dependency>
```

Then in their base class:
```java
public abstract class PageView extends OkygraphView {
    private static final PrettyTime prettyTime = new PrettyTime();

    protected String timeAgo(Date date) {
        return prettyTime.format(date);
    }
}
```

## Recommendation

**Include basic formatters in core `OkygraphView`:**
- ✅ Date/time formatting (ISO and custom patterns)
- ✅ Number formatting (decimals, currency, percent)
- ✅ String helpers (truncate, capitalize, join)

**Document how to add custom formatters:**
- ✅ Override methods in base class
- ✅ Add utility methods
- ✅ Use third-party libraries

**Keep it simple:**
- Use JDK classes only
- No external dependencies
- Easy to understand and customize
