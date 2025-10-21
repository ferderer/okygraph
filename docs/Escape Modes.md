# Escape Modes in Okygraph Templates

## Design Philosophy: Keep It Simple

Okygraph has **2 core concepts**:
1. Backtick (`) toggles between Java and HTML
2. Curly braces `{expression}` embed Java expressions in HTML

**We preserve this simplicity.** No special escape syntax.

## Solution: Smart Default + Method Calls

### Default Behavior: HTML Escaping Everywhere

**All expressions are HTML-escaped by default:**

```java
`<div>{user.name}</div>`
// → write("<div>"); writeHtml(user.name); write("</div>");
```

HTML escaping (`<` → `&lt;`, `>` → `&gt;`, `&` → `&amp;`, `"` → `&quot;`, `'` → `&#39;`) is:
- ✅ **Safe in HTML text content**
- ✅ **Safe in HTML attributes** (entities work in attributes)
- ✅ **Safe in JavaScript strings** (inside `<script>` tags, `&quot;` → `"`)
- ✅ **Safe enough in most URL contexts** (prevents XSS)

### Special Cases: Use Method Calls

For rare cases where you need different behavior, **use regular Java methods**:

```java
`<div>{raw(trustedHtml)}</div>`              // No escaping (dangerous!)
`<script>var x = "{jsEscape(data)}";</script>` // Custom JS escaping
`<a href="/search?q={urlEncode(query)}">`    // URL encoding
```

These are just **normal Java method calls** - nothing special! You can:
- Provide utility methods in your base class
- Import static methods from helper classes
- Write your own escaping logic

## Examples

### User-Generated Content (Safe by Default)
```java
`<div class="comment">{userComment}</div>`
// → write("<div class=\"comment\">");
//    write(escape(userComment));
//    write("</div>");
```

### Attributes (HTML Escaping Works)
```java
`<input value="{userInput}" placeholder="{hint}">`
// → write("<input value=\"");
//    write(escape(userInput));
//    write("\" placeholder=\"");
//    write(escape(hint));
//    write("\">");
```

### JavaScript Context (HTML Escaping Is Adequate)
```java
`<script>
    var config = {
        name: "{user.name}",
        id: {user.id}
    };
</script>`
// → write("<script>\n    var config = {\n        name: \"");
//    write(escape(user.name));
//    write("\",\n        id: ");
//    write(escape(user.id));
//    write("\n    };\n</script>");
// For complex cases, call jsEscape() method: {jsEscape(data)}
```

### Trusted Content (Explicit Opt-Out)
```java
`<div>{raw(sanitizedHtmlFromLibrary)}</div>`
// → write("<div>");
//    write(escape(raw(sanitizedHtmlFromLibrary)));
//    write("</div>");
// raw() returns String unchanged, escape() processes it
// Only for pre-sanitized/trusted content!
```

## Implementation

### Transpiler Behavior
Every `{expression}` generates:
```java
write(escape(expression))
```

This allows users to override `escape()` if they need custom escaping logic.

### BaseView Method
```java
protected void write(String text) {
    w.write(text);  // Direct write, no escaping
}

protected String escape(Object value) {
    if (value == null) return "";
    String str = String.valueOf(value);
    return escapeHtml(str);
}

private static String escapeHtml(String text) {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
}
```

**Users can override `escape()`:**
```java
@Override
protected String escape(Object value) {
    // Custom logic
    if (value instanceof SafeHtml) {
        return value.toString();  // Already safe
    }
    return super.escape(value);  // Delegate to default
}
```

### Optional Utility Methods (User Provides)
```java
// In your base class or utility class
protected String raw(String html) {
    return html;  // Pass through unchanged
}

protected String jsEscape(String text) {
    return text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
}

protected String urlEncode(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8);
}
```

## Benefits

1. **✅ 2-concept simplicity preserved**: Just backticks and `{expressions}`
2. **✅ Safe by default**: Everything HTML-escaped automatically
3. **✅ No new syntax**: Use familiar Java method calls
4. **✅ Flexible**: Users can override `escape()` method
5. **✅ Easy to review**: `raw()` calls are easy to grep and audit
6. **✅ No magic**: Everything is just regular Java

## Future Enhancements

### Boolean Attributes
Many template engines support conditional attributes:
```java
`<input disabled={user.isDisabled()} />`
// If false/null → <input />
// If true → <input disabled />
```

This could be implemented with special handling for boolean values:
```java
protected String escape(Object value) {
    if (value == null || Boolean.FALSE.equals(value)) {
        return null;  // Signal to transpiler: omit attribute
    }
    if (Boolean.TRUE.equals(value)) {
        return "";  // Signal: include attribute without value
    }
    return escapeHtml(String.valueOf(value));
}
```

**Postponed for now** - adds complexity to keep transpiler simple initially.

## Security Considerations

- **Default is safe**: Plain `{expr}` prevents XSS
- **Explicit opt-out**: `raw()` calls are visible and auditable
- **User responsibility**: For special cases, users write/import their own helpers
- **No false sense of security**: Simple escaping for simple cases, delegate complexity when needed

