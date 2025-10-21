# HTML Tag and Attribute Context Detection

## Overview

The tokenizer can recognize HTML tags and attributes to enable **context-aware escaping** and **smart attribute handling**.

## Benefits

1. **Context-aware escaping** - Different escaping for different contexts:
   - HTML content: `&lt;` `&gt;` `&amp;`
   - Attribute values: `&quot;` additionally
   - URL attributes: URL encoding
   - JavaScript attributes: JS escaping
   - Style attributes: CSS escaping

2. **Boolean attributes** - Smart handling:
   ```html
   <input disabled={user.isDisabled()} />
   <!-- If false, omit entirely -->
   <input />
   ```

3. **Attribute validation** - Catch errors at compile time:
   ```html
   <img src={unsafe_url} />  <!-- Warning: URL not validated -->
   ```

## Token Types to Add

```java
public enum TokenType {
    // ... existing types ...

    // HTML structure
    TAG_OPEN,           // <
    TAG_CLOSE,          // >
    TAG_SELF_CLOSE,     // />
    TAG_END_OPEN,       // </
    TAG_NAME,           // div, span, input, etc.

    // Attributes
    ATTR_NAME,          // class, id, href, etc.
    ATTR_EQ,            // =
    ATTR_VALUE_START,   // " or '
    ATTR_VALUE_END,     // " or '
    ATTR_VALUE_TEXT,    // text between quotes
}
```

## Tokenizer Modes

### Current Modes
- `JAVA` - Java code
- `TEMPLATE` - HTML text
- `JAVA_EXPRESSION` - Inside `{expr}`
- `JAVA_TEXT_BLOCK` - Inside `"""..."""`
- `JAVA_BLOCK_COMMENT` - Inside `/* ... */`

### New Modes
- `HTML_TAG` - Inside `<tag ...>`
- `HTML_ATTR_VALUE` - Inside attribute value `="..."`

## Parsing Strategy

### State Machine

```
TEMPLATE mode:
  '<' + letter → TAG_OPEN, push HTML_TAG mode
  '{' → EXPRESSION_START, push JAVA_EXPRESSION mode
  text → HTML_TEXT

HTML_TAG mode:
  identifier → TAG_NAME (first time) or ATTR_NAME
  '=' → ATTR_EQ
  '"' → ATTR_VALUE_START, push HTML_ATTR_VALUE mode
  '>' → TAG_CLOSE, pop to TEMPLATE
  '/>' → TAG_SELF_CLOSE, pop to TEMPLATE
  whitespace → skip
  '{' → EXPRESSION_START, push JAVA_EXPRESSION mode (for attr value)

HTML_ATTR_VALUE mode:
  '{' → EXPRESSION_START, push JAVA_EXPRESSION mode
  '"' → ATTR_VALUE_END, pop to HTML_TAG
  text → ATTR_VALUE_TEXT
```

### Example Token Stream

```html
<div class="container" id={divId}>
  Hello {user.name}!
</div>
```

**Tokens:**
```
TAG_OPEN        <
TAG_NAME        div
ATTR_NAME       class
ATTR_EQ         =
ATTR_VALUE_START    "
ATTR_VALUE_TEXT container
ATTR_VALUE_END  "
ATTR_NAME       id
ATTR_EQ         =
EXPRESSION_START    {
IDENTIFIER      divId
EXPRESSION_END  }
TAG_CLOSE       >
HTML_TEXT       \n  Hello
EXPRESSION_START    {
IDENTIFIER      user
SEPARATOR       .
IDENTIFIER      name
EXPRESSION_END  }
HTML_TEXT       !\n
TAG_END_OPEN    </
TAG_NAME        div
TAG_CLOSE       >
```

## Context-Aware Escaping

### Attribute Context Detection

```java
public enum AttributeContext {
    HTML,           // Regular attributes
    URL,            // href, src, action
    JAVASCRIPT,     // onclick, onload, etc.
    STYLE,          // style attribute
    BOOLEAN,        // disabled, checked, readonly
}

private static final Map<String, AttributeContext> ATTR_CONTEXTS = Map.of(
    "href", AttributeContext.URL,
    "src", AttributeContext.URL,
    "action", AttributeContext.URL,
    "onclick", AttributeContext.JAVASCRIPT,
    "onload", AttributeContext.JAVASCRIPT,
    "style", AttributeContext.STYLE,
    "disabled", AttributeContext.BOOLEAN,
    "checked", AttributeContext.BOOLEAN
);
```

### Transpiler Code Generation

```html
<!-- HTML context (default) -->
<div title={user.name}>
```
**Generates:**
```java
writeRaw("<div title=\"");
write(user.name);  // HTML-escaped
writeRaw("\">");
```

```html
<!-- URL context -->
<a href={profileUrl}>
```
**Generates:**
```java
writeRaw("<a href=\"");
writeRaw(url(profileUrl));  // URL-encoded
writeRaw("\">");
```

```html
<!-- JavaScript context -->
<button onclick={handleClick}>
```
**Generates:**
```java
writeRaw("<button onclick=\"");
writeRaw(js(handleClick));  // JS-escaped
writeRaw("\">");
```

```html
<!-- Boolean context -->
<input disabled={user.isLocked()} />
```
**Generates:**
```java
writeRaw("<input");
if (user.isLocked()) {
    writeRaw(" disabled");
}
writeRaw(" />");
```

## Implementation Phases

### Phase 1: Basic Tag Recognition (v1.1)
- ✅ Recognize `<tag>` and `</tag>`
- ✅ Parse attributes: `name="value"`
- ✅ Parse expression attributes: `name={expr}`
- ✅ Self-closing tags: `<tag />`

### Phase 2: Context-Aware Escaping (v1.2)
- ✅ Detect attribute context (URL, JS, etc.)
- ✅ Auto-apply appropriate escaping
- ✅ Warning if wrong escaping used

### Phase 3: Boolean Attributes (v1.3)
- ✅ Conditional attribute rendering
- ✅ Omit attribute if false

### Phase 4: Validation (v2.0)
- ✅ Validate tag names
- ✅ Validate attribute names for tag
- ✅ Detect unclosed tags
- ✅ Validate nesting (no `<p>` inside `<p>`)

## Configuration

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <configuration>
        <!-- Enable context-aware escaping -->
        <contextAwareEscaping>true</contextAwareEscaping>

        <!-- Enable boolean attributes -->
        <booleanAttributes>true</booleanAttributes>

        <!-- Validate HTML -->
        <validateHtml>false</validateHtml>

        <!-- Custom attribute contexts -->
        <attributeContexts>
            <data-url>URL</data-url>
            <data-script>JAVASCRIPT</data-script>
        </attributeContexts>
    </configuration>
</plugin>
```

## Examples

### Before (Manual Escaping)

```html
<a href="{url(link)}" onclick="handle('{js(id)}')">
    {user.name}
</a>
```

### After (Auto Context Detection)

```html
<a href={link} onclick="handle('{id}')">
    {user.name}
</a>
```

**Transpiler automatically:**
- URL-encodes `link` in `href`
- JS-escapes `id` in `onclick`
- HTML-escapes `user.name` in content

### Boolean Attributes

```html
<input type="checkbox"
       name="agree"
       checked={user.agreedToTerms}
       disabled={!user.canEdit()} />
```

**Generates:**
```java
writeRaw("<input type=\"checkbox\" name=\"agree\"");
if (user.agreedToTerms) {
    writeRaw(" checked");
}
if (!user.canEdit()) {
    writeRaw(" disabled");
}
writeRaw(" />");
```

**Result if both true:**
```html
<input type="checkbox" name="agree" checked disabled />
```

**Result if both false:**
```html
<input type="checkbox" name="agree" />
```

## Edge Cases

### 1. Mixed Content in Attributes
```html
<div title="Name: {user.name}, Age: {user.age}">
```

**Generates:**
```java
writeRaw("<div title=\"Name: ");
write(user.name);
writeRaw(", Age: ");
write(user.age);
writeRaw("\">");
```

### 2. Expressions in Tag Names (NOT SUPPORTED)
```html
<{tagName}>  <!-- ERROR -->
```
**Reason:** Tag names must be known at compile time for validation.

### 3. Complex Boolean Expressions
```html
<input disabled={user.locked || user.suspended} />
```

**Generates:**
```java
if (user.locked || user.suspended) {
    writeRaw(" disabled");
}
```

## Testing Strategy

```java
@Test
void testTagRecognition() {
    String template = "<div class=\"test\">Content</div>";
    List<Token> tokens = tokenizer.tokenize(template);

    assertEquals(TAG_OPEN, tokens.get(0).type());
    assertEquals(TAG_NAME, tokens.get(1).type());
    assertEquals(ATTR_NAME, tokens.get(2).type());
    // ...
}

@Test
void testContextAwareEscaping_url() {
    String template = "<a href={link}>Click</a>";
    String java = transpiler.transpile(template);

    assertTrue(java.contains("url(link)"));
}

@Test
void testBooleanAttribute() {
    String template = "<input disabled={isDisabled} />";
    String java = transpiler.transpile(template);

    assertTrue(java.contains("if (isDisabled)"));
    assertTrue(java.contains("writeRaw(\" disabled\")"));
}
```

## Compatibility

**Backwards compatible:**
- Manual escaping still works: `{url(link)}`
- Can disable context-aware escaping
- Explicit escaping overrides auto-detection

**Example:**
```html
<!-- Auto-detect -->
<a href={link}>

<!-- Force specific escaping -->
<a href={url(link)}>

<!-- Both generate same code -->
```

## Recommendation for v1

**Start simple:**
1. ✅ Implement tag/attribute tokenization
2. ✅ Document the feature
3. ⏳ Implement context-aware escaping in v1.1
4. ⏳ Implement boolean attributes in v1.2

**For v1.0:**
- Keep manual escaping: `{url(link)}`, `{js(id)}`
- Add tokenization infrastructure
- Prepare for future enhancements

**Benefits:**
- Simpler initial implementation
- More testing time for context detection
- Users can start using it immediately
- No breaking changes later
