# Framework Integration Strategy

## Overview

Users may want different base classes for different purposes:
1. **Pages** - Full HTML documents with Spring View integration
2. **Emails** - Plain HTML without framework integration
3. **Fragments** - Partial HTML for AJAX responses
4. **API responses** - JSON/XML templates (future)

## Base Class Hierarchy

```
OkygraphView (abstract)
    ├─ SpringOkygraphView (implements Spring's View interface)
    ├─ QuarkusOkygraphView (implements Quarkus TemplateInstance)
    ├─ MicronautOkygraphView (implements Micronaut Writable)
    └─ [User custom subclasses]
```

## Core Design: Multiple Base Classes

### Option A: User Choice (Recommended)
Generate templates with configurable base class:

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <configuration>
        <templateSets>
            <templateSet>
                <directory>src/main/templates/pages</directory>
                <baseClass>com.example.views.PageView</baseClass>
                <package>com.example.views.pages</package>
            </templateSet>
            <templateSet>
                <directory>src/main/templates/emails</directory>
                <baseClass>com.example.views.EmailView</baseClass>
                <package>com.example.views.emails</package>
            </templateSet>
        </templateSets>
    </configuration>
</plugin>
```

**User defines base classes:**
```java
// For pages - extends Spring adapter, adds common helpers
@Component
@RequestScope
public abstract class PageView extends SpringOkygraphView {

    // Common page metadata
    protected String pageTitle() {
        return "My App";
    }

    protected String pageDescription() {
        return i18n("app.description");
    }

    // Custom formatting
    protected String formatPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return i18n("price.free");
        }
        return formatCurrency(price);
    }
}

// For emails - pure, no framework dependencies
public abstract class EmailView extends OkygraphView {

    protected String formatDate(LocalDate date) {
        return DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
            .format(date);
    }

    // Email-specific helpers
    protected String buttonLink(String url, String text) throws IOException {
        return "<a href=\"" + url + "\" style=\"...\">" + text + "</a>";
    }
}
```

## Generated View Classes - Type-Safe Data

Templates compile to classes with **type-safe fields**, not loose model maps:

### Example Template: `UserProfile.oky`
```html
<div class="profile">
    <h1>{user.name}</h1>
    <p>{i18n("user.email")}: {user.email}</p>
    <p>{i18n("user.registered")}: {formatDate(user.createdAt)}</p>
</div>
```

### Generated: `UserProfileView.java`
```java
package com.example.views.pages;

import com.example.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import java.io.IOException;

@Component
@RequestScope
public class UserProfileView extends PageView {

    @Getter @Setter
    private User user;

    // Constructor injection (preferred)
    public UserProfileView() {
    }

    public UserProfileView(User user) {
        this.user = user;
    }

    @Override
    protected void render() throws IOException {
        writeRaw("<div class=\"profile\">");
        writeRaw("\n    <h1>");
        write(user.name);
        writeRaw("</h1>");
        writeRaw("\n    <p>");
        write(i18n("user.email"));
        writeRaw(": ");
        write(user.email);
        writeRaw("</p>");
        writeRaw("\n    <p>");
        write(i18n("user.registered"));
        writeRaw(": ");
        write(formatDate(user.createdAt));
        writeRaw("</p>");
        writeRaw("\n</div>");
    }
}
```

### Usage - Type-Safe!
```java
@Controller
public class UserController {

    @Autowired
    private ApplicationContext context;

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);

        // Option 1: Bean lookup + setter (for @Autowired dependencies)
        UserProfileView view = context.getBean(UserProfileView.class);
        view.setUser(user);  // Type-safe!
        return view;

        // Option 2: Direct instantiation (no @Autowired in view)
        return new UserProfileView(user);  // Type-safe!
    }
}
```

**Benefits over model maps:**
- ✅ **Compile-time safety** - `view.setUser(user)` vs `model.put("user", user)`
- ✅ **IDE autocomplete** - Field names, types, validation
- ✅ **Refactoring support** - Rename field = rename everywhere
- ✅ **No magic strings** - No `model.get("user")`
- ✅ **Constructor injection** - Immutable if desired

## Framework Integration

### 1. Spring MVC/Boot

**SpringOkygraphView** implements Spring's `View` interface and uses dependency injection:

```java
package dev.okygraph.spring;

import dev.okygraph.maven.runtime.OkygraphView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.View;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import java.util.Map;

@RequestScope
public abstract class SpringOkygraphView extends OkygraphView implements View {

    @Autowired
    @Getter
    private MessageSource messageSource;

    @Autowired
    @Getter
    private HttpServletRequest request;

    @Autowired
    @Getter
    private HttpServletResponse response;

    @Override
    public String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> model,
                      HttpServletRequest request,
                      HttpServletResponse response) throws Exception {
        // Spring injects these via @Autowired, but also passed here
        // Use the injected ones for consistency
        this.response.setContentType(getContentType());
        this.response.setCharacterEncoding("UTF-8");

        // Render to response
        render(this.response.getOutputStream());
    }

    /**
     * i18n message lookup using Spring's MessageSource.
     */
    protected String i18n(String code, Object... args) {
        return messageSource.getMessage(code, args, code,
            LocaleContextHolder.getLocale());
    }

    /**
     * Current locale from Spring's LocaleContextHolder.
     */
    @Override
    protected Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}
```

**Usage in Spring Controller - Type-safe, no model map!**
```java
@Controller
public class UserController {

    @Autowired
    private ApplicationContext context;

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);

        // Spring creates instance and injects dependencies
        UserProfileView view = context.getBean(UserProfileView.class);
        view.setUser(user);  // Type-safe setter!
        return view;
    }
}

// Generated view class
@Component
@RequestScope
public class UserProfileView extends SpringOkygraphView {
    @Getter @Setter
    private User user;

    @Override
    protected void render() throws IOException {
        writeRaw("<h1>");
        write(user.getName());
        writeRaw("</h1>");
        writeRaw("<p>");
        write(i18n("user.greeting", user.getName()));
        writeRaw("</p>");
    }
}
```

**Alternative: Constructor Injection (Simpler)**
```java
@Controller
public class UserController {

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);
        // Pass data via constructor, Spring injects services
        return new UserProfileView(user);
    }
}

// Generated view class
@Component
@RequestScope
public class UserProfileView extends SpringOkygraphView {
    private final User user;

    public UserProfileView(User user) {
        this.user = user;
    }

    @Override
    protected void render() throws IOException {
        writeRaw("<h1>");
        write(user.getName());
        writeRaw("</h1>");
    }
}
```

### 2. Quarkus (Qute alternative)

**QuarkusOkygraphView** implements Quarkus `TemplateInstance`:

```java
package dev.okygraph.quarkus;

import dev.okygraph.maven.runtime.OkygraphView;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import java.io.IOException;

public abstract class QuarkusOkygraphView extends OkygraphView
        implements TemplateInstance {

    @Override
    public String render() {
        try {
            return renderToString();
        } catch (IOException e) {
            throw new RuntimeException("Rendering failed", e);
        }
    }

    @Override
    public Uni<String> renderAsync() {
        return Uni.createFrom().item(this::render);
    }

    @Override
    public TemplateInstance data(String key, Object value) {
        // Optional: support Qute-style data binding
        return this;
    }
}
```

**Usage in Quarkus Resource:**
```java
@Path("/user")
public class UserResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance userProfile(@PathParam("id") Long id) {
        User user = userService.findById(id);
        return new UserProfileView(user);
    }
}
```

### 3. Micronaut

**MicronautOkygraphView** implements Micronaut `Writable`:

```java
package dev.okygraph.micronaut;

import dev.okygraph.maven.runtime.OkygraphView;
import io.micronaut.core.io.Writable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class MicronautOkygraphView extends OkygraphView
        implements Writable {

    @Override
    public void writeTo(OutputStream outputStream, Charset charset)
            throws IOException {
        render(outputStream);
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        render(writer);
    }
}
```

**Usage in Micronaut Controller:**
```java
@Controller("/user")
public class UserController {

    @Get("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Writable userProfile(@PathVariable Long id) {
        User user = userService.findById(id);
        return new UserProfileView(user);
    }
}
```

### 4. Standalone (no framework)

**Just use OkygraphView directly:**

```java
public class EmailSender {

    public void sendWelcomeEmail(User user) throws IOException {
        WelcomeEmailView view = new WelcomeEmailView(user);
        String html = view.renderToString();

        emailService.send(user.email, "Welcome!", html);
    }
}
```

## Maven Plugin Configuration

### Full Configuration Example

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Package for generated OkygraphView base class -->
        <baseClassPackage>com.example.views</baseClassPackage>

        <!-- Generate OkygraphView.java? -->
        <generateBaseClass>true</generateBaseClass>

        <!-- Default settings for all template sets -->
        <defaultBaseClass>com.example.views.PageView</defaultBaseClass>
        <defaultPackage>com.example.views</defaultPackage>

        <!-- Multiple template sets with different configurations -->
        <templateSets>
            <!-- Pages with Spring integration -->
            <templateSet>
                <id>pages</id>
                <directory>src/main/templates/pages</directory>
                <baseClass>com.example.views.PageView</baseClass>
                <package>com.example.views.pages</package>
                <extension>.oky</extension>

                <!-- Regex pattern for selective generation -->
                <includePattern>.*Profile\.oky|.*Dashboard\.oky</includePattern>
                <excludePattern>.*_draft\.oky</excludePattern>

                <!-- Add Spring annotations to generated classes -->
                <annotations>
                    <annotation>org.springframework.stereotype.Component</annotation>
                    <annotation>org.springframework.web.context.annotation.RequestScope</annotation>
                </annotations>
            </templateSet>

            <!-- Emails (standalone, no framework) -->
            <templateSet>
                <id>emails</id>
                <directory>src/main/templates/emails</directory>
                <baseClass>com.example.views.EmailView</baseClass>
                <package>com.example.views.emails</package>
                <!-- No annotations - standalone -->
            </templateSet>

            <!-- Fragments (AJAX responses) -->
            <templateSet>
                <id>fragments</id>
                <directory>src/main/templates/fragments</directory>
                <baseClass>com.example.views.FragmentView</baseClass>
                <package>com.example.views.fragments</package>
                <extension>.fragment.oky</extension>

                <annotations>
                    <annotation>org.springframework.stereotype.Component</annotation>
                    <annotation>org.springframework.web.context.annotation.RequestScope</annotation>
                </annotations>
            </templateSet>
        </templateSets>

        <!-- Framework hint for optimizations -->
        <framework>spring</framework> <!-- spring|quarkus|micronaut|none -->
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Configuration Options

**Template Set Options:**
- `directory` - Source directory for templates
- `baseClass` - Fully qualified name of base class to extend
- `package` - Package for generated classes
- `extension` - File extension (default: `.oky`)
- `includePattern` - Regex to match files to process
- `excludePattern` - Regex to exclude files
- `annotations` - List of annotations to add to generated classes

**Base Class Generation:**
- `baseClassPackage` - Where to generate `OkygraphView.java`
- `generateBaseClass` - Whether to generate it (default: true)

**Generated File Structure:**
```
target/generated-sources/okygraph/
  com/example/views/
    OkygraphView.java              ← Generated base class
    PageView.java                  ← User's base class (not generated)
    EmailView.java                 ← User's base class (not generated)
    pages/
      UserProfileView.java         ← Generated from UserProfile.oky
      DashboardView.java           ← Generated from Dashboard.oky
    emails/
      WelcomeEmailView.java        ← Generated from WelcomeEmail.oky
    fragments/
      UserCardView.java            ← Generated from UserCard.fragment.oky
```

## Optional: Runtime JARs

For users who want pre-built framework integrations:

```xml
<!-- Spring integration -->
<dependency>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-spring</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Quarkus integration -->
<dependency>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-quarkus</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Micronaut integration -->
<dependency>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-micronaut</artifactId>
    <version>1.0.0</version>
</dependency>
```

**But these are OPTIONAL** - users can always just use `OkygraphView` and write their own integration.

## Recommendation for v1

**Start simple:**
1. ✅ Core `OkygraphView` - works everywhere
2. ✅ Multi-template-set configuration
3. ✅ Configurable base class per set
4. ⏳ Framework integration JARs - later if requested

**Users get:**
- Full control over base classes
- Can create `PageView` vs `EmailView` hierarchies
- Can integrate with any framework
- No forced dependencies

## Testing Strategy

```java
// Test Spring integration
@Test
void testSpringViewIntegration() throws Exception {
    var view = new UserProfileView(user);
    var model = Map.of("user", user);
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var output = new ByteArrayOutputStream();
    when(response.getOutputStream()).thenReturn(
        new ServletOutputStream() {
            public void write(int b) { output.write(b); }
        }
    );

    view.render(model, request, response);

    String html = output.toString(StandardCharsets.UTF_8);
    assertTrue(html.contains(user.getName()));
}
```

## Migration Path

**From other template engines:**

```java
// Before (Thymeleaf)
@GetMapping("/user/{id}")
public String userProfile(@PathVariable Long id, Model model) {
    model.addAttribute("user", userService.findById(id));
    return "user/profile"; // template name
}

// After (Okygraph)
@GetMapping("/user/{id}")
public View userProfile(@PathVariable Long id) {
    User user = userService.findById(id);
    return new UserProfileView(user); // type-safe!
}
```

**Benefits:**
- ✅ Type safety - no more string-based template names
- ✅ Compile-time checks - catch errors before runtime
- ✅ IDE support - autocomplete, refactoring
- ✅ Performance - no template parsing at runtime
- ✅ Testability - unit test views directly
