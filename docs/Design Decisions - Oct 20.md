# Design Decisions Summary - October 20, 2025

## Completed Today

### 1. ✅ Enhanced .gitignore
- Added Maven-specific ignores (target/, *.iml, etc.)
- Added IDE ignores (.idea/, .vscode/, .settings/)
- Added OS-specific files (.DS_Store, Thumbs.db)

### 2. ✅ Renamed Escaping Functions
**Changed:**
- `jsEscape()` → `js()`
- `urlEncode()` → `url()`
- **Added:** `css()` for CSS escaping

**Rationale:** Cleaner, more concise API

**Usage:**
```html
<script>var name = "{js(user.name)}";</script>
<a href="/search?q={url(query)}">Search</a>
<style>.user-{css(user.id)} { color: red; }</style>
```

**Tests:** 43 passing (added 4 CSS tests)

### 3. ✅ Framework Integration Strategy

**Designed multi-base-class approach:**

```
OkygraphView (abstract, core)
    ├─ SpringOkygraphView (implements Spring's View)
    ├─ QuarkusOkygraphView (implements TemplateInstance)
    ├─ MicronautOkygraphView (implements Writable)
    └─ [User custom: PageView, EmailView, etc.]
```

**Key Features:**
- **Multiple template sets** - Different base classes for pages vs. emails
- **Framework adapters** - Optional integration JARs
- **User control** - Create custom base class hierarchies

**Configuration:**
```xml
<templateSets>
    <templateSet>
        <directory>src/main/templates/pages</directory>
        <baseClass>com.example.views.PageView</baseClass>
    </templateSet>
    <templateSet>
        <directory>src/main/templates/emails</directory>
        <baseClass>com.example.views.EmailView</baseClass>
    </templateSet>
</templateSets>
```

**User defines:**
```java
// For web pages
public abstract class PageView extends SpringOkygraphView {
    protected String pageTitle() { return "My App"; }
}

// For emails
public abstract class EmailView extends OkygraphView {
    protected String formatDate(LocalDate date) { ... }
}
```

### 4. ✅ Formatting Helpers Design

**Added to OkygraphView:**

```java
// Date/time
formatDate(LocalDate date)
formatDate(LocalDate date, String pattern)
formatDateTime(LocalDateTime dateTime)
formatDateLocale(LocalDate date)

// Numbers
formatNumber(Number number)
formatNumber(Number number, int decimals)
formatCurrency(Number amount)
formatCurrency(Number amount, String currencyCode)
formatPercent(Number number)

// Strings
truncate(String text, int maxLength)
capitalize(String text)
formatFileSize(long bytes)
pluralize(int count, String singular, String plural)

// Collections
join(Collection<?> items, String delimiter)
joinAnd(Collection<?> items, String delimiter)
```

**Usage:**
```html
<p>Price: {formatCurrency(product.price)}</p>
<p>Published: {formatDate(article.date, "MMM d, yyyy")}</p>
<p>{user.count} {pluralize(user.count, "item", "items")}</p>
<p>Size: {formatFileSize(file.bytes)}</p>
```

**Locale support:**
```java
view.setLocale(Locale.GERMANY);
String price = view.formatCurrency(19.99); // "19,99 €"
```

### 5. ✅ Tag/Attribute Mode Design

**Context-aware escaping:**

```html
<!-- Detects context automatically -->
<a href={profileUrl}>        <!-- URL-encodes -->
<button onclick={handler}>   <!-- JS-escapes -->
<div style={cssStyle}>       <!-- CSS-escapes -->
<div title={userName}>       <!-- HTML-escapes -->
```

**Boolean attributes:**
```html
<input disabled={user.isLocked()} />
<!-- If false, omits attribute entirely -->
```

**New token types:**
```java
TAG_OPEN, TAG_CLOSE, TAG_NAME
ATTR_NAME, ATTR_EQ
ATTR_VALUE_START, ATTR_VALUE_END
```

**Phases:**
- **Phase 1 (v1.0):** Manual escaping with `{url()}`, `{js()}`
- **Phase 2 (v1.1):** Auto context detection
- **Phase 3 (v1.2):** Boolean attributes
- **Phase 4 (v2.0):** HTML validation

## Current Status

**Tests:** 75 tests passing
- 43 OkygraphView tests ✅
- 19 Tokenizer tests ✅
- 13 UnicodePreprocessor tests ✅

**Documentation:**
- ✅ Base Class Design
- ✅ API Design (overloaded write methods)
- ✅ Framework Integration
- ✅ Formatting Helpers
- ✅ Tag and Attribute Mode
- ✅ Try-catch handling
- ✅ Escape Modes
- ✅ Progress tracker

## Implementation Priority

### Immediate (v1.0)
1. **Transpiler** - Core implementation
   - Parse token stream to AST
   - Generate Java code
   - Handle expressions and backtick toggle
   - Try/catch buffering

2. **Maven Plugin Integration**
   - File discovery
   - Incremental compilation
   - Multiple template sets
   - Generate OkygraphView base class

3. **Testing**
   - Integration tests (template → Java → execution)
   - Real-world examples

### Near Future (v1.1)
1. **Tag/Attribute Tokenization**
   - Add HTML_TAG and HTML_ATTR_VALUE modes
   - Parse attributes
   - Token stream with tag structure

2. **Context-Aware Escaping**
   - Detect href/src → URL context
   - Detect onclick → JS context
   - Auto-apply correct escaping

### Later (v1.2+)
1. **Boolean Attributes**
   - Conditional rendering
   - `disabled={false}` → omit attribute

2. **Formatting Helpers Implementation**
   - Add all formatting methods to OkygraphView
   - Comprehensive tests
   - Locale support

3. **Framework Integration JARs**
   - okygraph-spring module
   - okygraph-quarkus module
   - okygraph-micronaut module

## Key Design Principles Confirmed

### 1. Simplicity
- **2 concepts:** Backtick toggle + expressions
- No special directives
- Just Java

### 2. Safety
- HTML-escaping by default
- Context-aware escaping (future)
- Try/catch buffering

### 3. Type Safety
- Overloaded `write()` methods
- Compile-time checks
- IDE support

### 4. Flexibility
- Multiple base classes
- Overridable methods
- Framework-agnostic

### 5. Performance
- Zero runtime overhead
- Direct method calls
- AOT-friendly

## API Summary

### Write Methods
```java
write(String text)              // HTML-escaped
write(int/long/double/boolean)  // No escape
write(char c)                   // HTML-escaped
write(Raw raw)                  // No escape
writeRaw(String literal)        // Package-private, for transpiler
```

### Escaping
```java
escape(Object value)            // Overridable HTML escape
raw(String html)                // Returns Raw wrapper
js(String text)                 // JavaScript escape
url(String text)                // URL encode
css(String text)                // CSS escape
```

### Formatting (Planned)
```java
formatDate/DateTime/...         // Date/time formatting
formatNumber/Currency/Percent   // Number formatting
truncate/capitalize/...         // String helpers
join/joinAnd                    // Collection helpers
```

### Buffering
```java
pushBuffer()                    // Start try block
popBufferCommit()               // End try block (success)
popBufferDiscard()              // Catch block (error)
```

## Next Steps

1. **Start transpiler implementation**
   - AST node design
   - Parser (tokens → AST)
   - Code generator (AST → Java)

2. **Create example templates**
   - Simple page
   - Form with validation
   - Email template
   - Fragment (AJAX response)

3. **Integration testing**
   - Template → Java → Compile → Execute
   - Spring Boot integration
   - Performance benchmarks

## Questions to Consider

1. **Formatting:** Implement in v1.0 or v1.1?
   - **Recommendation:** v1.1 - not blocking transpiler work

2. **Tag mode:** Start in v1.0 or v1.1?
   - **Recommendation:** v1.1 - add tokenization, implement detection later

3. **Framework JARs:** v1.0 or v1.1?
   - **Recommendation:** v1.1 - document pattern in v1.0, create JARs if requested

4. **Boolean attributes:** v1.1 or v1.2?
   - **Recommendation:** v1.2 - requires tag mode first

## Timeline Estimate

**v1.0 (Core Engine):** 2-3 weeks
- Transpiler
- Maven plugin
- Basic integration tests
- Documentation

**v1.1 (Enhancements):** 1-2 weeks
- Tag/attribute tokenization
- Context-aware escaping
- Formatting helpers
- More examples

**v1.2 (Advanced Features):** 1-2 weeks
- Boolean attributes
- Framework integration JARs
- Performance optimizations
- Migration guides

**Total to v1.2:** ~6 weeks

## Success Metrics

**v1.0 Goals:**
- ✅ 2-concept simplicity preserved
- ✅ Type-safe templates
- ✅ Zero runtime dependencies
- ✅ Works with Spring/Quarkus/Micronaut
- ✅ XSS-safe by default
- ✅ Faster than existing engines

**Community Goals:**
- Clear documentation
- Example projects
- Migration guides
- Active GitHub presence
- Responsive to feedback
