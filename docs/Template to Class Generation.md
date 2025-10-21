# Template to Java Class Generation

## Overview

The transpiler analyzes templates to determine:
1. **Required fields** - Extract from `{expr}` usage
2. **Field types** - Infer from expressions
3. **Constructor/setters** - Generate for type-safe data injection
4. **Annotations** - Add based on configuration

## Field Detection

### Simple Field Access
```html
<h1>{user.name}</h1>
```

**Detected:**
- Field: `user`
- Type: Unknown (requires type inference or user annotation)
- Usage: `user.name` (method call or field access)

### Multiple Fields
```html
<div>
    <h1>{product.title}</h1>
    <p>{product.description}</p>
    <span>${product.price}</span>
    <p>By {author.name}</p>
</div>
```

**Detected:**
- Field: `product` (type unknown)
- Field: `author` (type unknown)

### Type Inference Strategies

#### Strategy 1: User Annotation (Recommended)
```html
{!-- @field User user --}
{!-- @field Author author --}
<h1>{user.name}</h1>
<p>By {author.name}</p>
```

**Generates:**
```java
@Getter @Setter
private User user;

@Getter @Setter
private Author author;
```

#### Strategy 2: Import Detection
```html
{!-- @import com.example.model.User --}
{!-- @import com.example.model.Author --}
<h1>{user.name}</h1>
```

**Generates:**
```java
import com.example.model.User;
import com.example.model.Author;

@Getter @Setter
private User user;

@Getter @Setter
private Author author;
```

#### Strategy 3: Package Convention
```
Configuration:
<fieldTypePackage>com.example.model</fieldTypePackage>

Template uses: {user.name}
Looks for: com.example.model.User
```

#### Strategy 4: Object Fallback
```java
// If type unknown, use Object
@Getter @Setter
private Object user;

@Getter @Setter
private Object author;
```

## Generated Class Structure

### Template with Annotations
```html
{!-- @field User user --}
{!-- @field List<Product> products --}
{!-- @import com.example.model.User --}
{!-- @import com.example.model.Product --}
{!-- @import java.util.List --}

<div class="profile">
    <h1>{user.name}</h1>
    <ul>
    ` for (Product p : products) { `
        <li>{p.title} - ${p.price}</li>
    ` } `
    </ul>
</div>
```

### Generated Class
```java
package com.example.views.pages;

import com.example.model.User;
import com.example.model.Product;
import com.example.views.PageView;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import java.io.IOException;
import java.util.List;

/**
 * Generated from: UserProfile.oky
 */
@Component
@RequestScope
public class UserProfileView extends PageView {

    // ============================================
    // Fields (detected from template)
    // ============================================

    @Getter @Setter
    private User user;

    @Getter @Setter
    private List<Product> products;

    // ============================================
    // Constructors
    // ============================================

    /**
     * Default constructor for Spring bean creation.
     */
    public UserProfileView() {
    }

    /**
     * Constructor with all required fields.
     */
    public UserProfileView(User user, List<Product> products) {
        this.user = user;
        this.products = products;
    }

    // ============================================
    // Render method
    // ============================================

    @Override
    protected void render() throws IOException {
        writeRaw("<div class=\"profile\">");
        writeRaw("\n    <h1>");
        write(user.name);
        writeRaw("</h1>");
        writeRaw("\n    <ul>");
        writeRaw("\n    ");

        for (Product p : products) {
            writeRaw("\n        <li>");
            write(p.title);
            writeRaw(" - $");
            write(p.price);
            writeRaw("</li>");
            writeRaw("\n    ");
        }

        writeRaw("\n    </ul>");
        writeRaw("\n</div>");
    }
}
```

## Annotation Support

### Configuration
```xml
<templateSet>
    <id>pages</id>
    <annotations>
        <annotation>org.springframework.stereotype.Component</annotation>
        <annotation>org.springframework.web.context.annotation.RequestScope</annotation>
    </annotations>
</templateSet>
```

### Generated
```java
@Component
@RequestScope
public class UserProfileView extends PageView {
    // ...
}
```

### Template-Specific Annotations
```html
{!-- @annotation org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')") --}
<div>Admin content</div>
```

**Generates:**
```java
@Component
@RequestScope
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardView extends PageView {
    // ...
}
```

## Field Type Detection Algorithm

```java
class FieldDetector {

    // Scan template for field references
    Map<String, FieldInfo> detectFields(Template template) {
        Map<String, FieldInfo> fields = new HashMap<>();

        for (Expression expr : template.getExpressions()) {
            if (expr.isFieldAccess()) {
                String fieldName = expr.getFieldName();
                String typeName = inferType(expr, template);

                fields.putIfAbsent(fieldName, new FieldInfo(
                    fieldName,
                    typeName,
                    expr.getLineNumber()
                ));
            }
        }

        return fields;
    }

    String inferType(Expression expr, Template template) {
        // 1. Check for @field annotation
        String annotatedType = template.getFieldAnnotation(expr.getFieldName());
        if (annotatedType != null) {
            return annotatedType;
        }

        // 2. Check imports
        String imported = template.getImportedType(expr.getFieldName());
        if (imported != null) {
            return imported;
        }

        // 3. Check fieldTypePackage configuration
        String packageName = config.getFieldTypePackage();
        if (packageName != null) {
            String className = capitalize(expr.getFieldName());
            return packageName + "." + className;
        }

        // 4. Fallback to Object
        return "Object";
    }
}
```

## Template Directive Syntax

### Field Declaration
```html
{!-- @field TypeName fieldName --}
{!-- @field User user --}
{!-- @field List<Product> products --}
{!-- @field Map<String, Object> metadata --}
```

### Imports
```html
{!-- @import com.example.model.User --}
{!-- @import java.util.List --}
{!-- @import java.util.Map --}
```

### Annotations
```html
{!-- @annotation org.springframework.stereotype.Component --}
{!-- @annotation org.springframework.web.context.annotation.RequestScope --}
```

### Class-Level JavaDoc
```html
{!-- @description User profile page showing account details --}
{!-- @author John Doe --}
{!-- @since 1.0 --}
```

**Generates:**
```java
/**
 * User profile page showing account details
 *
 * @author John Doe
 * @since 1.0
 */
@Component
@RequestScope
public class UserProfileView extends PageView {
    // ...
}
```

## Usage Patterns

### Pattern 1: Constructor Injection (Immutable)
```java
@Controller
public class UserController {

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);
        List<Product> products = productService.findByUser(user);

        // All data via constructor - immutable view
        return new UserProfileView(user, products);
    }
}
```

### Pattern 2: Setter Injection (Spring beans)
```java
@Controller
public class UserController {

    @Autowired
    private ApplicationContext context;

    @GetMapping("/user/{id}")
    public View userProfile(@PathVariable Long id) {
        User user = userService.findById(id);
        List<Product> products = productService.findByUser(user);

        // Spring creates bean, we set data
        UserProfileView view = context.getBean(UserProfileView.class);
        view.setUser(user);
        view.setProducts(products);
        return view;
    }
}
```

### Pattern 3: Builder (Optional)
```java
// Generate builder method
public class UserProfileView extends PageView {

    public static UserProfileView of(User user, List<Product> products) {
        return new UserProfileView(user, products);
    }
}

// Usage
@GetMapping("/user/{id}")
public View userProfile(@PathVariable Long id) {
    return UserProfileView.of(
        userService.findById(id),
        productService.findByUser(id)
    );
}
```

## Error Handling

### Missing Type Information
```
[WARNING] UserProfile.oky:5 - Cannot infer type for field 'user'
          Consider adding: {!-- @field User user --}
          Falling back to: Object
```

### Unused Fields
```
[WARNING] UserProfile.oky - Field 'products' declared but never used
```

### Type Mismatch
```
[ERROR] UserProfile.oky:10 - Field 'user' declared as 'User' but used as 'String'
        Expression: {user.toString().substring(0, 10)}
```

## Configuration Options

```xml
<plugin>
    <configuration>
        <!-- Field type inference -->
        <fieldTypePackage>com.example.model</fieldTypePackage>
        <fieldTypeStrategy>ANNOTATION</fieldTypeStrategy>
        <!-- ANNOTATION | IMPORT | CONVENTION | OBJECT -->

        <!-- Constructor generation -->
        <generateDefaultConstructor>true</generateDefaultConstructor>
        <generateAllArgsConstructor>true</generateAllArgsConstructor>
        <generateBuilder>false</generateBuilder>

        <!-- Lombok support -->
        <useLombok>true</useLombok>
        <!-- Generates @Getter @Setter -->

        <!-- Validation -->
        <validateFieldUsage>true</validateFieldUsage>
        <warnUnusedFields>true</warnUnusedFields>
    </configuration>
</plugin>
```

## Summary

**Type-safe templates:**
- ✅ Fields detected from expressions
- ✅ Types from annotations, imports, or conventions
- ✅ Constructors + setters generated
- ✅ Lombok support for cleaner code
- ✅ Spring annotations configurable
- ✅ IDE autocomplete and refactoring
- ✅ Compile-time type checking

**No more model maps!**
```java
// Old way (Thymeleaf, etc.)
model.put("user", user);  // String keys, no type safety
return "user/profile";

// New way (Okygraph)
return new UserProfileView(user);  // Type-safe!
```
