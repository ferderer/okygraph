# Design Refinements - Framework Integration

## Key Insights from Original BaseView

After reviewing `docs/BaseView.java`, several important design patterns emerged:

### 1. ✅ Dependency Injection (Not Model Maps!)

**Original approach:**
```java
@Autowired
private MessageSource messageSource;

@Autowired
@Getter
private HttpServletRequest request;

@Autowired
@Getter
private HttpServletResponse response;
```

**Why this is better:**
- ✅ Views are Spring beans (`@Component` + `@RequestScope`)
- ✅ Services injected via `@Autowired`
- ✅ No `populateFromModel()` - that was wrong!
- ✅ Data comes via constructor or setters (type-safe)

### 2. ✅ Type-Safe Data, Not String Maps

**Wrong (old Thymeleaf way):**
```java
model.put("user", user);  // String key
return "user/profile";    // String template name
```

**Right (Okygraph way):**
```java
return new UserProfileView(user);  // Type-safe!
// OR
UserProfileView view = context.getBean(UserProfileView.class);
view.setUser(user);  // Type-safe setter!
return view;
```

### 3. ✅ i18n Integration

```java
protected String i18n(@NotNull String code, String ...args) {
    return messageSource.getMessage(code, args, code,
        LocaleContextHolder.getLocale());
}
```

**Usage in template:**
```html
<p>{i18n("user.greeting", user.name)}</p>
```

### 4. ✅ Request/Response Access

```java
@Getter
private HttpServletRequest request;

@Getter
private HttpServletResponse response;
```

**Can be used in templates:**
```html
<p>Your IP: {request.getRemoteAddr()}</p>
<p>User-Agent: {request.getHeader("User-Agent")}</p>
```

### 5. ✅ Performance Logging

```java
long start = System.nanoTime();
try {
    render();
} finally {
    log.info("Time: {} ms; Rendered class {}",
        df.format((System.nanoTime() - start) / 1_000_000.0),
        getClass().getSimpleName());
}
```

## Updated SpringOkygraphView Design

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@RequestScope
public abstract class SpringOkygraphView extends OkygraphView implements View {

    private static final Logger log = LoggerFactory.getLogger(SpringOkygraphView.class);
    private static final DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

    static {
        df.applyPattern("0.00");
    }

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
        long start = System.nanoTime();

        try {
            this.response.setContentType(getContentType());
            this.response.setCharacterEncoding("UTF-8");

            // Render to response output stream
            render(this.response.getOutputStream());

        } catch (Exception ex) {
            log.error("Rendering error in {}", getClass().getSimpleName(), ex);
            throw new RuntimeException("E_RENDERING_ERROR", ex);
        } finally {
            log.info("Time: {} ms; Rendered class {}",
                df.format((System.nanoTime() - start) / 1_000_000.0),
                getClass().getSimpleName());
        }
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

    /**
     * Current language code (e.g., "en", "de").
     */
    protected String language() {
        return LocaleContextHolder.getLocale().getLanguage().toLowerCase();
    }
}
```

## Maven Plugin Configuration Enhancements

### New Options

1. **`baseClassPackage`** - Where to generate `OkygraphView.java`
2. **`annotations`** - Add annotations to generated classes
3. **`includePattern`** - Regex to select files
4. **`excludePattern`** - Regex to exclude files

### Example
```xml
<templateSet>
    <id>pages</id>
    <directory>src/main/templates/pages</directory>
    <baseClass>com.example.views.PageView</baseClass>
    <package>com.example.views.pages</package>

    <!-- Only process Profile and Dashboard templates -->
    <includePattern>.*Profile\.oky|.*Dashboard\.oky</includePattern>
    <excludePattern>.*_draft\.oky</excludePattern>

    <!-- Add Spring annotations -->
    <annotations>
        <annotation>org.springframework.stereotype.Component</annotation>
        <annotation>org.springframework.web.context.annotation.RequestScope</annotation>
    </annotations>
</templateSet>
```

## Template Directives

### Field Declarations (Type-Safe!)

```html
{!-- @field User user --}
{!-- @field List<Product> products --}
{!-- @import com.example.model.User --}
{!-- @import com.example.model.Product --}
{!-- @import java.util.List --}
```

**Generates:**
```java
import com.example.model.User;
import com.example.model.Product;
import java.util.List;

@Getter @Setter
private User user;

@Getter @Setter
private List<Product> products;

public UserProfileView() {}

public UserProfileView(User user, List<Product> products) {
    this.user = user;
    this.products = products;
}
```

## Complete Example

### Template: `UserProfile.oky`
```html
{!-- @field User user --}
{!-- @import com.example.model.User --}

<div class="profile">
    <h1>{user.name}</h1>
    <p>{i18n("user.email")}: {user.email}</p>
    <p>{i18n("user.ip")}: {request.remoteAddr}</p>
</div>
```

### Generated: `UserProfileView.java`
```java
package com.example.views.pages;

import com.example.model.User;
import com.example.views.PageView;
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

    public UserProfileView() {}

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
        write(i18n("user.ip"));
        writeRaw(": ");
        write(request.getRemoteAddr());
        writeRaw("</p>");
        writeRaw("\n</div>");
    }
}
```

### Usage: `UserController.java`
```java
@Controller
public class UserController {

    @Autowired
    private ApplicationContext context;

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);

        // Spring creates bean with injected dependencies
        UserProfileView view = context.getBean(UserProfileView.class);
        view.setUser(user);  // Type-safe!
        return view;

        // Or direct instantiation (no @Autowired in view)
        // return new UserProfileView(user);
    }
}
```

## Key Advantages

### 1. Type Safety
```java
// Compile error if wrong type!
view.setUser("string");  // ERROR: User expected

// Compile error if wrong method!
view.setUzer(user);  // ERROR: setUser not setUzer

// IDE autocomplete
view.set|  // IDE shows: setUser(User), setProducts(List<Product>)
```

### 2. Refactoring Support
```java
// Rename User.name → User.fullName
// IDE updates everywhere:
// - Model class
// - View class
// - Template (if IDE supports .oky files)
```

### 3. No Magic Strings
```java
// Old way
model.put("usr", user);  // Typo!
// Template: ${user.name}  // null, silent failure

// New way
view.setUser(user);  // Type-safe, no typos possible
```

### 4. Dependency Injection
```java
@Component
@RequestScope
public class UserProfileView extends SpringOkygraphView {

    @Autowired
    private UserService userService;  // Can inject services!

    @Getter @Setter
    private Long userId;

    @Override
    protected void render() throws IOException {
        // Can fetch data inside view
        User user = userService.findById(userId);
        write(user.getName());
    }
}
```

## Summary

**Key changes from initial design:**
1. ✅ No `populateFromModel()` - views are beans with DI
2. ✅ Type-safe fields with `@field` annotations
3. ✅ Constructors + setters generated (Lombok support)
4. ✅ Spring annotations configurable per template set
5. ✅ Regex patterns for selective generation
6. ✅ `baseClassPackage` for OkygraphView.java location
7. ✅ `i18n()`, `request`, `response` access
8. ✅ Performance logging built-in

**Result:** Type-safe, fast, Spring-native template engine! 🚀
