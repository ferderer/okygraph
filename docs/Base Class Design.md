# OkygraphView Base Class Design

## Overview

The base class provides the runtime support for generated template code. It must be:
- **Simple**: Minimal API surface
- **Flexible**: Support multiple frameworks
- **Type-safe**: Strong typing where possible
- **Performant**: Efficient string building

## What The Transpiler Generates

### Example Template
```java
package com.example.views;

public class UserProfile extends OkygraphView {
    private User user;

    public UserProfile(User user) {
        this.user = user;
    }

    @Override
    protected void render() throws IOException {
        writeRaw("<div class=\"profile\">");
        writeRaw("\n    <h1>");
        write(user.name);  // Auto-escaped
        writeRaw("</h1>");
        writeRaw("\n    <p>Email: ");
        write(user.email);  // Auto-escaped
        writeRaw("</p>");
        writeRaw("\n</div>");
    }
}
```

### Code Patterns Generated

| Template Construct | Generated Code |
|-------------------|----------------|
| HTML text | `writeRaw("<div>")` |
| Expression | `write(expr)` (auto-escapes) |
| Int/long/double/boolean | `write(42)` (no escape) |
| Try block start | `try { pushBuffer();` |
| Try block end | `popBufferCommit(); }` |
| Catch block start | `catch (...) { popBufferDiscard();` |
| Raw method call | `write(raw(html))` |
| Java code in `` ` `` | Direct Java (if, for, etc.) |

## Base Class Structure

```java
package dev.okygraph.runtime;

import java.io.*;

public abstract class OkygraphView {

    // ===== CORE ABSTRACT METHOD =====

    /**
     * Subclasses implement this to render the template.
     * Called by render(Writer) and render(OutputStream).
     */
    protected abstract void render() throws IOException;


    // ===== PUBLIC API =====

    /**
     * Render to a Writer.
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
     * Render to an OutputStream (convenience).
     */
    public void render(OutputStream out) throws IOException {
        render(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    /**
     * Render to a String.
     */
    public String renderToString() throws IOException {
        StringWriter sw = new StringWriter();
        render(sw);
        return sw.toString();
    }


    // ===== WRITER MANAGEMENT =====

    /** Current writer (transpiler writes directly to this) */
    protected Writer w;

    /** Stack for try/catch buffering */
    private final Deque<Writer> writerStack = new ArrayDeque<>();


    // ===== WRITE METHODS (used by transpiler) =====

    /**
     * Write string with HTML escaping.
     * Used for string expressions.
     */
    protected void write(String text) throws IOException {
        w.write(escape(text));
    }

    /**
     * Write primitives without escaping.
     * Overloaded for: int, long, double, boolean, char
     */
    protected void write(int value) throws IOException {
        w.write(String.valueOf(value));
    }
    // ... other primitive overloads

    /**
     * Write Raw object without escaping.
     * Used with raw() method.
     */
    protected void write(Raw raw) throws IOException {
        if (raw != null && raw.content != null) {
            w.write(raw.content);
        }
    }

    /**
     * Write HTML literals (package-private).
     * Used internally by generated code.
     */
    void writeRaw(String text) throws IOException {
        if (text != null) {
            w.write(text);
        }
    }


    // ===== ESCAPING =====

    /**
     * HTML escape by default.
     * Users can override for custom logic.
     */
    protected String escape(Object value) {
        if (value == null) {
            return "";
        }
        return escapeHtml(String.valueOf(value));
    }

    /**
     * HTML entity escaping.
     */
    protected static String escapeHtml(String text) {
        // Escape &, <, >, ", '
        // Optimized with lazy StringBuilder allocation
    }


    // ===== HELPER METHODS (for users) =====

    /**
     * Raw HTML wrapper class.
     */
    protected static class Raw {
        final String content;
        Raw(String content) { this.content = content; }
    }

    /**
     * Pass through without escaping (DANGEROUS!).
     * Usage: write(raw(trustedHtml))
     */
    protected Raw raw(String html) {
        return new Raw(html);
    }

    /**
     * JavaScript string escaping.
     */
    protected String jsEscape(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * URL encoding.
     */
    protected String urlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }


    // ===== TRY/CATCH BUFFERING =====

    /**
     * Start buffering output (called by transpiler after "try {").
     */
    protected void pushBuffer() {
        writerStack.push(w);
        w = new StringWriter();
    }

    /**
     * Commit buffer to output (called before "}" of try block).
     */
    protected void popBufferCommit() throws IOException {
        String content = w.toString();
        w = writerStack.pop();
        w.write(content);
    }

    /**
     * Discard buffer (called after "catch (...) {").
     */
    protected void popBufferDiscard() {
        w = writerStack.pop();
    }
}
```

## Framework-Specific Considerations

### 1. **Spring MVC / Spring Boot**
```java
@Controller
public class UserController {

    @GetMapping("/profile/{id}")
    public void profile(@PathVariable Long id, HttpServletResponse response)
            throws IOException {
        User user = userService.findById(id);
        new UserProfileView(user).render(response.getOutputStream());
    }
}
```

**No special base class needed** - standard servlet API works.

### 2. **Quarkus (JAX-RS)**
```java
@Path("/profile")
public class UserResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public String profile(@PathParam("id") Long id) throws IOException {
        User user = userService.findById(id);
        return new UserProfileView(user).renderToString();
    }
}
```

**No special base class needed** - return String or stream.

### 3. **Micronaut**
```java
@Controller("/profile")
public class UserController {

    @Get("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public String profile(Long id) throws IOException {
        User user = userService.findById(id);
        return new UserProfileView(user).renderToString();
    }
}
```

**No special base class needed** - same as Quarkus.

### 4. **Vert.x (Reactive)**
```java
router.get("/profile/:id").handler(ctx -> {
    Long id = Long.parseLong(ctx.pathParam("id"));
    User user = userService.findById(id);

    // Need async-friendly version
    StringWriter sw = new StringWriter();
    new UserProfileView(user).render(sw);
    ctx.response()
        .putHeader("Content-Type", "text/html")
        .end(sw.toString());
});
```

**Could provide async helper**:
```java
public CompletableFuture<String> renderAsync() {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return renderToString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    });
}
```

## Base Class Variants

### Option 1: Single Base Class (Recommended)
```java
// One class, works everywhere
public abstract class OkygraphView { ... }
```

**Pros:**
- Simple - one class to understand
- Works with all frameworks
- Zero runtime dependencies

**Cons:**
- No framework-specific optimizations

### Option 2: Framework-Specific Subclasses
```java
// Base
public abstract class OkygraphView { ... }

// Spring-specific
public abstract class SpringOkygraphView extends OkygraphView {
    public ModelAndView toModelAndView(String viewName) { ... }
}

// Reactive
public abstract class ReactiveOkygraphView extends OkygraphView {
    public Mono<String> renderMono() { ... }
    public Flux<String> renderFlux() { ... }
}
```

**Pros:**
- Framework integration helpers
- Reactive support

**Cons:**
- More classes to maintain
- Adds dependency on frameworks

## Recommended Approach

### Start Simple: Single Base Class
1. **Generate the base class** alongside templates (or make it a tiny runtime JAR)
2. **Zero dependencies** - pure Java
3. **Framework-agnostic** - works everywhere
4. **Users add helpers** if they need framework integration

### Base Class Location

**Option A: Generate with each project** (Recommended for v1)
```
target/
  generated-sources/
    okygraph/
      OkygraphView.java       ← Generated once
      UserProfileView.java    ← Generated from template
      ProductListView.java    ← Generated from template
```

**Pros:**
- No runtime dependency
- Users can customize OkygraphView
- Copy-paste friendly

**Option B: Runtime JAR**
```xml
<dependency>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-runtime</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Pros:**
- Consistent implementation
- Easier to update
- Professional approach

**Cons:**
- Adds a dependency
- Less flexible for customization

## Implementation Plan

### Phase 1: Generate Base Class ✓
- Maven plugin generates `OkygraphView.java` on first run
- Configuration option: `generateBaseClass` (default: true)
- If false, assumes user provides their own

### Phase 2: Core Methods ✓
- `write()` methods
- `escape()` with HTML escaping
- `render()` abstract method
- Public render APIs

### Phase 3: Try/Catch Support ✓
- Buffer stack methods
- `pushBuffer()`, `popBufferCommit()`, `popBufferDiscard()`

### Phase 4: Helper Methods ✓
- `raw()`, `jsEscape()`, `urlEncode()`
- Users can add more in their own base class

### Phase 5: Optional Runtime JAR
- Package as separate artifact
- Users choose: generated class vs. JAR dependency

## Configuration in pom.xml

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Generate base class? -->
        <generateBaseClass>true</generateBaseClass>

        <!-- Custom base class name -->
        <baseClassName>OkygraphView</baseClassName>

        <!-- Package for base class -->
        <basePackage>dev.okygraph.runtime</basePackage>

        <!-- Framework hint (for future optimizations) -->
        <framework>spring</framework> <!-- spring|quarkus|micronaut|none -->
    </configuration>
</plugin>
```

## Testing Strategy

```java
// Unit test a view
@Test
void testUserProfileView() throws IOException {
    User user = new User("Alice", "alice@example.com");
    UserProfileView view = new UserProfileView(user);

    String html = view.renderToString();

    assertTrue(html.contains("<h1>Alice</h1>"));
    assertTrue(html.contains("alice@example.com"));
}

// Test XSS prevention
@Test
void testXssPrevention() throws IOException {
    User user = new User("<script>alert('xss')</script>", "test@example.com");
    UserProfileView view = new UserProfileView(user);

    String html = view.renderToString();

    assertFalse(html.contains("<script>"));
    assertTrue(html.contains("&lt;script&gt;"));
}

// Test try/catch buffering
@Test
void testTryCatchBuffering() throws IOException {
    RiskyView view = new RiskyView(true); // Will throw
    String html = view.renderToString();

    assertFalse(html.contains("This should not appear"));
    assertTrue(html.contains("Error message"));
}
```

## Summary

**The base class should be:**
1. ✅ **Simple**: ~150 lines of code
2. ✅ **Framework-agnostic**: Works with Spring, Quarkus, Micronaut, etc.
3. ✅ **Safe by default**: HTML escaping built-in
4. ✅ **Extensible**: Users can override `escape()` and add helpers
5. ✅ **Zero dependencies**: Pure Java, no external libraries

**Next Steps:**
1. Implement the transpiler to generate the code patterns shown above
2. Generate `OkygraphView.java` as part of the Maven plugin
3. Test with real templates
4. Optimize performance if needed
