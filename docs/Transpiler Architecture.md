# Transpiler Architecture Design

## Goal: ~430 Lines of Beautiful, Minimal Code

**Philosophy:** "Code is perfect when nothing can be taken away"

**Target:**
- DirectiveParser: ~50 LOC
- FieldDetector: ~80 LOC
- TranspilerContext: ~50 LOC
- CodeGenerator: ~250 LOC (Pass 1: ~100, Pass 2: ~150)
- **Total: ~430 LOC** 🎯

---

## Architecture Overview

```
Template File (.oky)
        ↓
    Tokenizer (already done ✅)
        ↓
    Token Stream (List<Token>)
        ↓
    ┌─────────────────────────┐
    │   TRANSPILER (430 LOC)  │
    ├─────────────────────────┤
    │  Pass 1: Analyze        │
    │  - DirectiveParser      │
    │  - FieldDetector        │
    │  → TranspilerContext    │
    ├─────────────────────────┤
    │  Pass 2: Generate       │
    │  - CodeGenerator        │
    │  → Java Source Code     │
    └─────────────────────────┘
        ↓
    Generated .java File
        ↓
    javac (Java compiler)
        ↓
    Compiled .class (pure bytecode!)
```

---

## Component Design

### 1. TranspilerContext (~50 LOC)

**Purpose:** Hold all metadata extracted from template

**Data:**
```java
package dev.okygraph.maven.transpiler;

import java.util.*;

/**
 * Context for transpiling a template.
 * Holds all metadata extracted during Pass 1.
 */
public class TranspilerContext {

    // Metadata
    private String packageName;
    private String className;
    private String baseClass;

    // Fields detected from template
    private final Map<String, FieldInfo> fields = new LinkedHashMap<>();

    // Imports from @import directives
    private final Set<String> imports = new LinkedHashSet<>();

    // Annotations from config + @annotation directives
    private final List<String> annotations = new ArrayList<>();

    // Constructors
    public TranspilerContext(String packageName, String className, String baseClass) {
        this.packageName = packageName;
        this.className = className;
        this.baseClass = baseClass;
    }

    // Field management
    public void addField(String name, String type) {
        fields.putIfAbsent(name, new FieldInfo(name, type));
    }

    public void addField(FieldInfo field) {
        fields.putIfAbsent(field.name(), field);
    }

    public Collection<FieldInfo> getFields() {
        return fields.values();
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    // Import management
    public void addImport(String importStmt) {
        imports.add(importStmt);
    }

    public Set<String> getImports() {
        return imports;
    }

    // Annotation management
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    // Getters
    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getBaseClass() { return baseClass; }
}

/**
 * Field metadata.
 */
record FieldInfo(String name, String type) {}
```

**Size:** ~50 LOC ✅
**Complexity:** Minimal - just a data holder

---

### 2. DirectiveParser (~50 LOC)

**Purpose:** Parse template directives from COMMENT tokens

**Directives:**
- `{!-- @field Type name --}` - Declare field with type
- `{!-- @import package.Class --}` - Add import
- `{!-- @annotation @Annotation --}` - Add class annotation

**Code:**
```java
package dev.okygraph.maven.transpiler;

import java.util.regex.*;

/**
 * Parses template directives from comments.
 */
public class DirectiveParser {

    // Regex patterns for directives
    private static final Pattern FIELD_PATTERN =
        Pattern.compile("@field\\s+(\\S+)\\s+(\\w+)");

    private static final Pattern IMPORT_PATTERN =
        Pattern.compile("@import\\s+(\\S+)");

    private static final Pattern ANNOTATION_PATTERN =
        Pattern.compile("@annotation\\s+(.+)");

    /**
     * Parse @field directive.
     * Example: "@field User user" -> FieldInfo("user", "User")
     */
    public FieldInfo parseField(String comment) {
        Matcher m = FIELD_PATTERN.matcher(comment);
        if (m.find()) {
            String type = m.group(1);  // "User"
            String name = m.group(2);  // "user"
            return new FieldInfo(name, type);
        }
        return null;
    }

    /**
     * Parse @import directive.
     * Example: "@import com.example.User" -> "com.example.User"
     */
    public String parseImport(String comment) {
        Matcher m = IMPORT_PATTERN.matcher(comment);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Parse @annotation directive.
     * Example: "@annotation @PreAuthorize(...)" -> "@PreAuthorize(...)"
     */
    public String parseAnnotation(String comment) {
        Matcher m = ANNOTATION_PATTERN.matcher(comment);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Check if comment contains a directive.
     */
    public boolean isDirective(String comment) {
        return comment.contains("@field") ||
               comment.contains("@import") ||
               comment.contains("@annotation");
    }
}
```

**Size:** ~50 LOC ✅
**Complexity:** Simple regex matching

---

### 3. FieldDetector (~80 LOC)

**Purpose:** Detect field references from EXPRESSION tokens

**Strategy:**
1. Simple heuristic: `user.name` → field "user"
2. Split expression on `.`, take first part
3. Filter out method calls like `formatDate(...)`
4. Merge with explicit `@field` declarations

**Code:**
```java
package dev.okygraph.maven.transpiler;

import dev.okygraph.maven.tokenizer.Token;
import dev.okygraph.maven.tokenizer.TokenType;
import java.util.*;

/**
 * Detects field references from expressions.
 */
public class FieldDetector {

    /**
     * Detect all field references from token stream.
     * Returns set of field names (without types).
     */
    public Set<String> detectFieldNames(List<Token> tokens) {
        Set<String> fieldNames = new HashSet<>();

        for (Token token : tokens) {
            if (token.type() == TokenType.EXPRESSION) {
                String fieldName = extractFieldName(token.value());
                if (fieldName != null) {
                    fieldNames.add(fieldName);
                }
            }
        }

        return fieldNames;
    }

    /**
     * Extract field name from expression.
     *
     * Examples:
     * - "user.name" -> "user"
     * - "product.price" -> "product"
     * - "orders[0].total" -> "orders"
     * - "formatDate(...)" -> null (method call, not field)
     */
    private String extractFieldName(String expression) {
        expression = expression.trim();

        // Skip raw() wrapper
        if (expression.startsWith("raw(")) {
            expression = unwrapRaw(expression);
        }

        // Skip method calls (no field reference)
        if (isMethodCall(expression)) {
            return null;
        }

        // Simple case: "user.name" -> "user"
        if (expression.contains(".")) {
            return expression.substring(0, expression.indexOf('.')).trim();
        }

        // Array/list access: "orders[0]" -> "orders"
        if (expression.contains("[")) {
            return expression.substring(0, expression.indexOf('[')).trim();
        }

        // Single identifier: "user" -> "user"
        if (isIdentifier(expression)) {
            return expression;
        }

        return null;
    }

    /**
     * Check if expression is a method call (starts with lowercase, has parens).
     */
    private boolean isMethodCall(String expr) {
        // Simple heuristic: if starts with lowercase and has '(', it's a method
        // Example: "formatDate(...)", "i18n(...)"
        if (expr.contains("(")) {
            String beforeParen = expr.substring(0, expr.indexOf('(')).trim();
            if (!beforeParen.contains(".")) {
                return Character.isLowerCase(beforeParen.charAt(0));
            }
        }
        return false;
    }

    /**
     * Check if string is a valid Java identifier.
     */
    private boolean isIdentifier(String s) {
        if (s.isEmpty()) return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Unwrap raw() function.
     * Example: "raw(html)" -> "html"
     */
    private String unwrapRaw(String expr) {
        if (expr.startsWith("raw(") && expr.endsWith(")")) {
            return expr.substring(4, expr.length() - 1).trim();
        }
        return expr;
    }
}
```

**Size:** ~80 LOC ✅
**Complexity:** Simple string parsing

---

### 4. CodeGenerator (~250 LOC)

**Purpose:** Generate Java source code from tokens + context

**Pass 1: Analyze (~100 LOC)**
```java
package dev.okygraph.maven.transpiler;

import dev.okygraph.maven.tokenizer.Token;
import dev.okygraph.maven.tokenizer.TokenType;
import java.util.*;

/**
 * Transpiles templates to Java code.
 */
public class CodeGenerator {

    private final DirectiveParser directiveParser = new DirectiveParser();
    private final FieldDetector fieldDetector = new FieldDetector();

    /**
     * Pass 1: Analyze token stream, build context.
     */
    public TranspilerContext analyze(List<Token> tokens,
                                     String packageName,
                                     String className,
                                     String baseClass) {

        TranspilerContext ctx = new TranspilerContext(packageName, className, baseClass);

        // Parse directives from comments
        for (Token token : tokens) {
            if (token.type() == TokenType.COMMENT) {
                String content = token.value();

                // @field directive
                FieldInfo field = directiveParser.parseField(content);
                if (field != null) {
                    ctx.addField(field);
                }

                // @import directive
                String importStmt = directiveParser.parseImport(content);
                if (importStmt != null) {
                    ctx.addImport(importStmt);
                }

                // @annotation directive
                String annotation = directiveParser.parseAnnotation(content);
                if (annotation != null) {
                    ctx.addAnnotation(annotation);
                }
            }
        }

        // Detect fields from expressions
        Set<String> detectedFieldNames = fieldDetector.detectFieldNames(tokens);

        // Add detected fields (if not already declared)
        for (String fieldName : detectedFieldNames) {
            if (!ctx.hasField(fieldName)) {
                // Type unknown, use Object
                ctx.addField(fieldName, "Object");
            }
        }

        // Add default imports
        ctx.addImport("java.io.IOException");
        ctx.addImport("lombok.Getter");
        ctx.addImport("lombok.Setter");

        return ctx;
    }

    // Pass 2 methods follow...
}
```

**Pass 2: Generate (~150 LOC)**
```java
/**
 * Pass 2: Generate Java source code.
 */
public String generate(List<Token> tokens, TranspilerContext ctx) {
    StringBuilder out = new StringBuilder();

    // Package
    out.append("package ").append(ctx.getPackageName()).append(";\n\n");

    // Imports
    for (String imp : ctx.getImports()) {
        out.append("import ").append(imp).append(";\n");
    }
    out.append("\n");

    // Class annotations
    for (String ann : ctx.getAnnotations()) {
        out.append(ann).append("\n");
    }

    // Class header
    out.append("public class ").append(ctx.getClassName());
    out.append(" extends ").append(ctx.getBaseClass()).append(" {\n\n");

    // Fields
    for (FieldInfo field : ctx.getFields()) {
        out.append("    @Getter @Setter\n");
        out.append("    private ").append(field.type()).append(" ");
        out.append(field.name()).append(";\n\n");
    }

    // Default constructor
    out.append("    public ").append(ctx.getClassName()).append("() {}\n\n");

    // All-args constructor
    if (!ctx.getFields().isEmpty()) {
        generateAllArgsConstructor(out, ctx);
    }

    // render() method
    out.append("    @Override\n");
    out.append("    protected void render() throws IOException {\n");

    // Process tokens -> render() body
    generateRenderMethod(out, tokens, ctx);

    out.append("    }\n");
    out.append("}\n");

    return out.toString();
}

/**
 * Generate all-args constructor.
 */
private void generateAllArgsConstructor(StringBuilder out, TranspilerContext ctx) {
    out.append("    public ").append(ctx.getClassName()).append("(");

    // Parameters
    List<FieldInfo> fields = new ArrayList<>(ctx.getFields());
    for (int i = 0; i < fields.size(); i++) {
        FieldInfo field = fields.get(i);
        out.append(field.type()).append(" ").append(field.name());
        if (i < fields.size() - 1) out.append(", ");
    }
    out.append(") {\n");

    // Assignments
    for (FieldInfo field : fields) {
        out.append("        this.").append(field.name())
           .append(" = ").append(field.name()).append(";\n");
    }

    out.append("    }\n\n");
}

/**
 * Generate render() method body from tokens.
 */
private void generateRenderMethod(StringBuilder out, List<Token> tokens, TranspilerContext ctx) {
    for (Token token : tokens) {
        switch (token.type()) {
            case HTML:
                // writeRaw("...")
                out.append("        writeRaw(")
                   .append(escapeJavaString(token.value()))
                   .append(");\n");
                break;

            case EXPRESSION:
                // write(...) or writeRaw(...)
                String expr = token.value().trim();
                if (expr.startsWith("raw(") && expr.endsWith(")")) {
                    // raw() -> writeRaw()
                    String inner = expr.substring(4, expr.length() - 1);
                    out.append("        writeRaw(").append(inner).append(");\n");
                } else {
                    // Normal expression -> write() with escaping
                    out.append("        write(").append(expr).append(");\n");
                }
                break;

            case JAVA_KEYWORD:
            case JAVA_OPERATOR:
            case IDENTIFIER:
            case LPAREN:
            case RPAREN:
            case LBRACE:
            case RBRACE:
            case SEMICOLON:
                // Pass through Java code
                out.append("        ").append(token.value());
                if (token.type() == TokenType.SEMICOLON ||
                    token.type() == TokenType.LBRACE ||
                    token.type() == TokenType.RBRACE) {
                    out.append("\n");
                }
                break;

            case COMMENT:
                // Skip (directives already processed)
                break;

            case NEWLINE:
                // Skip (we control formatting)
                break;
        }
    }
}

/**
 * Escape string for Java string literal.
 */
private String escapeJavaString(String s) {
    return "\"" + s.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t") + "\"";
}
```

**Total Size:** ~250 LOC ✅
**Complexity:** Sequential processing, simple string building

---

## Complete Flow Example

### Input Template: `UserProfile.oky`
```html
{!-- @field User user --}
{!-- @import com.example.model.User --}

<div class="profile">
    <h1>{user.name}</h1>
    <p>{user.email}</p>
    % if (user.isAdmin()) {
        <div>Admin</div>
    % }
</div>
```

### Transpiler Flow:

**1. Tokenizer (already done):**
```java
List<Token> tokens = tokenizer.tokenize(template);
// Returns: COMMENT, COMMENT, HTML, EXPRESSION, HTML, JAVA_KEYWORD, ...
```

**2. Pass 1 - Analyze:**
```java
CodeGenerator gen = new CodeGenerator();
TranspilerContext ctx = gen.analyze(
    tokens,
    "com.example.views",
    "UserProfileView",
    "PageView"
);

// ctx now contains:
// - fields: {user: User}
// - imports: {com.example.model.User, java.io.IOException, lombok.*}
// - annotations: []
```

**3. Pass 2 - Generate:**
```java
String javaCode = gen.generate(tokens, ctx);
// Returns full Java class as string
```

### Output: `UserProfileView.java`
```java
package com.example.views;

import com.example.model.User;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

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
        writeRaw("<h1>");
        write(user.name);
        writeRaw("</h1>");
        writeRaw("<p>");
        write(user.email);
        writeRaw("</p>");
        if (user.isAdmin()) {
            writeRaw("<div>Admin</div>");
        }
        writeRaw("</div>");
    }
}
```

---

## Total Code Budget

| Component | Target LOC | Complexity |
|-----------|------------|------------|
| TranspilerContext | 50 | Simple data class |
| DirectiveParser | 50 | Regex matching |
| FieldDetector | 80 | String parsing |
| CodeGenerator (Pass 1) | 100 | Loop + parse |
| CodeGenerator (Pass 2) | 150 | Loop + emit |
| **Total** | **430** | **Minimal** ✅ |

---

## Key Design Decisions

### 1. **No AST** ✅
- Process tokens directly
- Sequential, one-pass (per phase)
- No tree building, no traversal

### 2. **Two-Pass Design** ✅
- Pass 1: Collect metadata (fields, imports, annotations)
- Pass 2: Generate code
- Clean separation of concerns

### 3. **Simple Heuristics** ✅
- Field detection: Split on `.`, take first part
- Works for 90% of cases
- Explicit `@field` for complex cases

### 4. **Minimal API** ✅
- 3 directives: `@field`, `@import`, `@annotation`
- 4 classes: Context, Parser, Detector, Generator
- Clean, understandable

### 5. **Framework Agnostic** ✅
- No Spring/Quarkus/Micronaut knowledge
- Just: tokens → Java code
- Base class configured by user

---

## Philosophy Check ✅

**"Code is perfect when nothing can be taken away"**

**Can we remove:**
- ❌ TranspilerContext? No - need to hold metadata
- ❌ DirectiveParser? No - need to parse directives
- ❌ FieldDetector? No - need to detect fields
- ❌ CodeGenerator? No - need to emit code
- ❌ Two passes? No - need metadata before generation

**Everything is essential!** ✅

**Compared to JSP (27,269 LOC):** We're **63x smaller!** 🎯

---

## Next Steps

Ready to implement? Let's start with the simplest component and work up:

1. ✅ **TranspilerContext** (50 LOC) - Just a data class
2. ✅ **DirectiveParser** (50 LOC) - Simple regex
3. ✅ **FieldDetector** (80 LOC) - String parsing
4. ⏳ **CodeGenerator Pass 1** (100 LOC) - Analysis
5. ⏳ **CodeGenerator Pass 2** (150 LOC) - Generation
6. ⏳ **Tests** (200 LOC) - Verify everything works

Let's build this! 💪
