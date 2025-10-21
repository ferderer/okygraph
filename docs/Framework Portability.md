# Framework Portability - The One-Line Migration

## The Problem with Traditional Template Engines

**Thymeleaf, JSP, Freemarker:**
```
Templates → Tightly coupled to framework → Hard to port
```

If you want to migrate from Spring to Quarkus:
- ❌ Rewrite all templates
- ❌ Change all controllers
- ❌ Learn new template syntax
- ❌ Fix subtle differences
- ❌ Test everything again

**Result:** Teams stay locked to their framework forever. 🔒

## The Okygraph Solution

**Okygraph:**
```
Templates → Pure Java → Framework adapter (1 class!)
```

### Change ONE Line to Port Entire Application

**Before (Spring):**
```xml
<templateSet>
    <directory>src/main/templates/pages</directory>
    <baseClass>com.example.views.SpringPageView</baseClass>
    <package>com.example.views.pages</package>
</templateSet>
```

**After (Quarkus):**
```xml
<templateSet>
    <directory>src/main/templates/pages</directory>
    <baseClass>com.example.views.QuarkusPageView</baseClass> <!-- Changed! -->
    <package>com.example.views.pages</package>
</templateSet>
```

**That's it!** All templates now use Quarkus. No template changes needed. ✅

## Complete Migration Example

### Step 1: Original Spring Application

**Maven plugin config:**
```xml
<templateSet>
    <directory>src/main/templates</directory>
    <baseClass>com.example.views.PageView</baseClass>
    <annotations>
        <annotation>org.springframework.stereotype.Component</annotation>
        <annotation>org.springframework.web.context.annotation.RequestScope</annotation>
    </annotations>
</templateSet>
```

**Base class: `PageView.java`**
```java
package com.example.views;

import dev.okygraph.spring.SpringOkygraphView;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public abstract class PageView extends SpringOkygraphView {

    protected String pageTitle() {
        return "My App";
    }

    protected String formatPrice(BigDecimal price) {
        return formatCurrency(price);
    }
}
```

**Template: `ProductDetail.oky`**
```html
{!-- @field Product product --}

<div class="product">
    <h1>{product.name}</h1>
    <p>{formatPrice(product.price)}</p>
    <p>{i18n("product.description")}</p>
</div>
```

**Generated: `ProductDetailView.java`**
```java
@Component
@RequestScope
public class ProductDetailView extends PageView {
    @Getter @Setter
    private Product product;

    // render() method...
}
```

### Step 2: Migrate to Quarkus

**Change 1: Update Maven plugin (1 line!)**
```xml
<templateSet>
    <directory>src/main/templates</directory>
    <baseClass>com.example.views.PageView</baseClass>  <!-- Same! -->
    <annotations>
        <annotation>jakarta.enterprise.context.RequestScoped</annotation>  <!-- Quarkus -->
    </annotations>
</templateSet>
```

**Change 2: Update base class (1 class!)**
```java
package com.example.views;

import dev.okygraph.quarkus.QuarkusOkygraphView;  // Changed import!
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public abstract class PageView extends QuarkusOkygraphView {  // Changed parent!

    protected String pageTitle() {
        return "My App";
    }

    protected String formatPrice(BigDecimal price) {
        return formatCurrency(price);
    }
}
```

**Change 3: Templates?**
```html
{!-- NO CHANGES NEEDED! Same template works! --}

<div class="product">
    <h1>{product.name}</h1>
    <p>{formatPrice(product.price)}</p>
    <p>{i18n("product.description")}</p>
</div>
```

**Generated code automatically adapts:**
```java
@RequestScoped  // Quarkus annotation now!
public class ProductDetailView extends PageView {  // Still same!
    @Getter @Setter
    private Product product;

    // render() method - unchanged!
}
```

**Done!** Your entire application now runs on Quarkus. ✅

## Migration Matrix

| From → To | Config Changes | Base Class Changes | Template Changes | Generated Code Changes |
|-----------|----------------|---------------------|------------------|------------------------|
| Spring → Quarkus | 1 line (annotation) | 1 class (extend QuarkusOkygraphView) | **0** | Automatic |
| Spring → Micronaut | 1 line (annotation) | 1 class (extend MicronautOkygraphView) | **0** | Automatic |
| Quarkus → Spring | 1 line (annotation) | 1 class (extend SpringOkygraphView) | **0** | Automatic |
| Spring → Plain Java | Remove annotations | 1 class (extend OkygraphView) | **0** | Automatic |

**Key insight:** Templates are framework-agnostic Java code generators!

## Adding Custom Platform Support

Want to use Okygraph with a custom framework? **Write one adapter class!**

### Example: Vert.x Integration

**Step 1: Create adapter (one time)**
```java
package dev.okygraph.vertx;

import dev.okygraph.maven.runtime.OkygraphView;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class VertxOkygraphView extends OkygraphView {

    private RoutingContext context;

    public void render(RoutingContext context) throws IOException {
        this.context = context;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        render(baos);

        context.response()
            .putHeader("Content-Type", "text/html; charset=UTF-8")
            .end(Buffer.buffer(baos.toByteArray()));
    }

    protected RoutingContext getContext() {
        return context;
    }

    // Helper methods specific to Vert.x
    protected String getParam(String name) {
        return context.request().getParam(name);
    }
}
```

**Step 2: Use it (change one line in config)**
```xml
<templateSet>
    <directory>src/main/templates</directory>
    <baseClass>com.example.views.PageView</baseClass>  <!-- Extends VertxOkygraphView -->
</templateSet>
```

**Step 3: All templates now work with Vert.x!** ✅

## Real-World Scenarios

### Scenario 1: Start with Plain Java, Add Spring Later

**Initial (prototyping):**
```xml
<templateSet>
    <baseClass>com.example.views.PageView</baseClass>  <!-- Extends OkygraphView -->
</templateSet>
```

```java
public abstract class PageView extends OkygraphView {
    // Plain Java, no dependencies
}
```

**Later (production):**
```xml
<templateSet>
    <baseClass>com.example.views.PageView</baseClass>  <!-- Still same name! -->
    <annotations>
        <annotation>org.springframework.stereotype.Component</annotation>
    </annotations>
</templateSet>
```

```java
@Component
@RequestScope
public abstract class PageView extends SpringOkygraphView {
    // Now has Spring DI, i18n, etc.

    @Autowired
    private UserService userService;  // New capability!
}
```

**Templates unchanged!** But now have access to Spring features.

### Scenario 2: Multi-Platform Application

**Same templates, multiple platforms:**

```xml
<templateSets>
    <!-- Web UI (Spring) -->
    <templateSet>
        <directory>src/main/templates</directory>
        <baseClass>com.example.web.PageView</baseClass>
        <package>com.example.web.views</package>
    </templateSet>

    <!-- Desktop UI (JavaFX) -->
    <templateSet>
        <directory>src/main/templates</directory>
        <baseClass>com.example.desktop.PanelView</baseClass>
        <package>com.example.desktop.views</package>
    </templateSet>

    <!-- CLI Reports (Plain Java) -->
    <templateSet>
        <directory>src/main/templates</directory>
        <baseClass>com.example.cli.ReportView</baseClass>
        <package>com.example.cli.views</package>
    </templateSet>
</templateSets>
```

**Same template generates 3 different view classes for 3 platforms!**

### Scenario 3: Gradual Migration

**Phase 1: Keep existing Thymeleaf, add Okygraph alongside**
```
/templates
    /thymeleaf  (old, being phased out)
    /okygraph   (new, growing)
```

**Phase 2: As you rewrite pages, move from thymeleaf/ → okygraph/**

**Phase 3: Switch frameworks (change base class)**

**Phase 4: Remove Thymeleaf dependency**

No "big bang" rewrite needed!

## Competitive Advantage

### Other Template Engines
```
Thymeleaf → Spring only
JSP → Servlet containers only
Freemarker → Requires Freemarker runtime everywhere
Velocity → Requires Velocity runtime everywhere
```

**Framework lock-in!** Can't easily switch.

### Okygraph
```
Okygraph → Pure Java → Any framework (or none!)
```

**Framework freedom!** Change anytime.

## Architecture Benefits

### 1. **Separation of Concerns**
```
Templates (business logic)
    ↓
Transpiler (code generation) ← Framework agnostic!
    ↓
Generated Views (pure Java)
    ↓
Base Class (framework adapter) ← User controlled!
    ↓
Framework (Spring/Quarkus/etc.)
```

### 2. **Testability**
```java
// No framework needed for testing!
ProductDetailView view = new ProductDetailView();
view.setProduct(product);

ByteArrayOutputStream out = new ByteArrayOutputStream();
view.render(out);

String html = out.toString();
assertThat(html).contains("<h1>Product Name</h1>");
```

### 3. **Portability**
- ✅ Web → Desktop (JavaFX HTML rendering)
- ✅ Server → Client (GraalVM native image)
- ✅ Microservices → Monolith
- ✅ Cloud → Edge computing
- ✅ Spring → Quarkus → Micronaut → Plain Java

### 4. **Future-Proof**
New framework in 2030? Write one adapter class!

## Marketing Message

> **"Write Once, Run Anywhere - For Real This Time"**
>
> Unlike traditional template engines that lock you into a specific framework,
> Okygraph templates compile to pure Java. Want to migrate from Spring to Quarkus?
> **Change one line of configuration.** Want to use the same templates for web,
> desktop, and CLI? **Just configure different base classes.**
>
> Your templates are **framework-agnostic Java code generators**. The framework
> adapter is **your code, your choice**. True portability, true freedom.

## Documentation TODOs

1. ✅ **This document** - Framework Portability overview
2. ⏳ **Migration Guides:**
   - Spring → Quarkus
   - Spring → Micronaut
   - Thymeleaf → Okygraph
   - JSP → Okygraph
3. ⏳ **Adapter Development Guide:**
   - How to write custom framework adapters
   - API contract for OkygraphView subclasses
   - Testing adapters
4. ⏳ **Examples Repository:**
   - Same templates, 3+ frameworks
   - Gradual migration demo
   - Multi-platform application

## Summary

**The Power of One Line:**
```xml
<baseClass>com.example.MyAdapterView</baseClass>
```

Change this one line, and your entire application ports to a new framework.

**This is only possible because:**
1. ✅ Transpiler is framework-agnostic
2. ✅ Templates compile to pure Java
3. ✅ Framework integration via inheritance (not tight coupling)
4. ✅ Base class is user-controlled (not baked into generated code)

**Result:** True framework portability! 🚀
