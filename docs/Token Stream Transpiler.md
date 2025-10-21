# Token Stream Transpiler Architecture

## Philosophy: No AST Needed!

**"Code is perfect when nothing can be taken away"**

Traditional compilers: `Source → Tokens → AST → IR → Code`
Okygraph: `Template → Tokens → Code` ✅

**Why skip AST?**
- ❌ AST is unnecessary abstraction
- ❌ Extra memory allocation
- ❌ Extra traversal pass
- ❌ More complex code
- ✅ Token stream already has all information we need
- ✅ Simpler = faster = fewer bugs

## Token Stream Analysis

### The Tokenizer Already Tracks:
- **Mode transitions** - JAVA vs TEMPLATE vs JAVA_EXPRESSION
- **Nesting** - Braces, parentheses, brackets
- **Line numbers** - For error reporting
- **Token types** - HTML, EXPRESSION, JAVA_BLOCK, COMMENT, etc.

### What We Need to Generate:
```java
@Override
protected void render() throws IOException {
    writeRaw("<div>");        // From HTML token
    write(user.name);         // From EXPRESSION token
    if (user.isAdmin()) {     // From JAVA_BLOCK token
        writeRaw("<div>Admin</div>");
    }
    writeRaw("</div>");
}
```

**Token stream already has this!**

## Transpiler Architecture

### Single-Pass Architecture (Ideal)
```
Template file
    ↓
Tokenizer
    ↓
Token Stream → Field Detector (scan for fields)
               ↓
               Detected fields
    ↓
Token Stream → Code Generator (emit Java)
               ↓
               Generated .java file
```

### Two-Pass Architecture (If Needed)
```
Template file
    ↓
Tokenizer
    ↓
Pass 1: Field Detection
    - Scan EXPRESSION tokens for field references
    - Parse COMMENT tokens for @field directives
    - Collect imports, annotations
    - Build field map
    ↓
Pass 2: Code Generation
    - Emit package, imports, class header
    - Emit field declarations
    - Emit constructors
    - Process tokens:
        * HTML → writeRaw(...)
        * EXPRESSION → write(...)
        * JAVA_BLOCK → direct Java code
        * COMMENT with @field → skip (already processed)
```

## Token-to-Code Mapping

### 1. HTML Text → writeRaw()
**Token:**
```
Token(HTML, "<div class=\"header\">", line=5)
```

**Generated:**
```java
writeRaw("<div class=\"header\">");
```

### 2. Expression → write()
**Token:**
```
Token(EXPRESSION, "user.name", line=6)
```

**Generated:**
```java
write(user.name);
```

### 3. Raw Expression → writeRaw()
**Token:**
```
Token(EXPRESSION, "raw(trustedHtml)", line=7)
```

**Generated:**
```java
writeRaw(trustedHtml);  // Detect raw() call, unwrap
```

### 4. Java Block → Direct Java
**Tokens:**
```
Token(JAVA_KEYWORD, "if", line=8)
Token(LPAREN, "(", line=8)
Token(IDENTIFIER, "user", line=8)
Token(DOT, ".", line=8)
Token(IDENTIFIER, "isAdmin", line=8)
Token(LPAREN, "(", line=8)
Token(RPAREN, ")", line=8)
Token(RPAREN, ")", line=8)
Token(LBRACE, "{", line=8)
```

**Generated:**
```java
if (user.isAdmin()) {
```

**Simple rule:** Java tokens pass through as-is!

### 5. Comment with Directive → Process
**Token:**
```
Token(COMMENT, "{!-- @field User user --}", line=3)
```

**Action:**
- Parse directive type: `@field`
- Extract: type=`User`, name=`user`
- Add to field map
- **Don't emit in render() method**

### 6. Comment with Annotation → Process
**Token:**
```
Token(COMMENT, "{!-- @annotation @PreAuthorize(\"hasRole('ADMIN')\") --}", line=2)
```

**Action:**
- Parse directive type: `@annotation`
- Extract: `@PreAuthorize("hasRole('ADMIN')")`
- Add to class annotations
- **Don't emit in render() method**

## Field Detection Algorithm

### Simple Heuristic
```java
class FieldDetector {
    Set<String> detectFields(List<Token> tokens) {
        Set<String> fields = new HashSet<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.type() == EXPRESSION) {
                String expr = token.value();

                // Simple case: "user.name"
                if (expr.contains(".")) {
                    String fieldName = expr.substring(0, expr.indexOf('.'));
                    fields.add(fieldName);
                }

                // Method call: "user.getName()"
                // Still detects "user" as field
            }
        }

        return fields;
    }
}
```

**More sophisticated (if needed):**
- Parse expression as Java expression
- Detect root identifier
- Handle complex expressions: `user.address.city`, `orders[0].total`

### Directive Parsing
```java
class DirectiveParser {
    Map<String, FieldInfo> parseFieldDirectives(List<Token> tokens) {
        Map<String, FieldInfo> fields = new HashMap<>();

        for (Token token : tokens) {
            if (token.type() == COMMENT) {
                String content = token.value();

                // Match: {!-- @field Type name --}
                if (content.contains("@field")) {
                    Matcher m = Pattern.compile("@field\\s+(\\S+)\\s+(\\w+)")
                        .matcher(content);
                    if (m.find()) {
                        String type = m.group(1);  // "User"
                        String name = m.group(2);  // "user"
                        fields.put(name, new FieldInfo(type, name));
                    }
                }
            }
        }

        return fields;
    }
}
```

## Code Generation Algorithm

### Pass 1: Collect Metadata
```java
class TranspilerPass1 {

    TranspilerContext analyzeTemplate(List<Token> tokens) {
        TranspilerContext ctx = new TranspilerContext();

        for (Token token : tokens) {
            if (token.type() == COMMENT) {
                String content = token.value();

                // @field directives
                if (content.contains("@field")) {
                    FieldInfo field = parseFieldDirective(content);
                    ctx.addField(field);
                }

                // @import directives
                if (content.contains("@import")) {
                    String importStmt = parseImportDirective(content);
                    ctx.addImport(importStmt);
                }

                // @annotation directives
                if (content.contains("@annotation")) {
                    String annotation = parseAnnotationDirective(content);
                    ctx.addAnnotation(annotation);
                }
            }

            if (token.type() == EXPRESSION) {
                // Detect fields from expressions
                String fieldName = extractFieldName(token.value());
                if (fieldName != null && !ctx.hasField(fieldName)) {
                    // Field used but not declared, mark as Object
                    ctx.addField(new FieldInfo("Object", fieldName));
                }
            }
        }

        return ctx;
    }
}
```

### Pass 2: Generate Code
```java
class TranspilerPass2 {

    String generateClass(List<Token> tokens, TranspilerContext ctx) {
        StringBuilder out = new StringBuilder();

        // Package
        out.append("package ").append(ctx.getPackageName()).append(";\n\n");

        // Imports
        for (String imp : ctx.getImports()) {
            out.append("import ").append(imp).append(";\n");
        }
        out.append("\n");

        // Class header
        for (String ann : ctx.getAnnotations()) {
            out.append(ann).append("\n");
        }
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
            out.append("    public ").append(ctx.getClassName()).append("(");
            out.append(ctx.getFields().stream()
                .map(f -> f.type() + " " + f.name())
                .collect(Collectors.joining(", ")));
            out.append(") {\n");
            for (FieldInfo field : ctx.getFields()) {
                out.append("        this.").append(field.name())
                   .append(" = ").append(field.name()).append(";\n");
            }
            out.append("    }\n\n");
        }

        // render() method
        out.append("    @Override\n");
        out.append("    protected void render() throws IOException {\n");

        // Process tokens
        for (Token token : tokens) {
            switch (token.type()) {
                case HTML:
                    out.append("        writeRaw(")
                       .append(javaStringLiteral(token.value()))
                       .append(");\n");
                    break;

                case EXPRESSION:
                    if (isRawExpression(token.value())) {
                        out.append("        writeRaw(")
                           .append(unwrapRaw(token.value()))
                           .append(");\n");
                    } else {
                        out.append("        write(")
                           .append(token.value())
                           .append(");\n");
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
                    // Java tokens pass through
                    out.append("        ").append(token.value());
                    if (token.type() == JAVA_KEYWORD ||
                        token.type() == RBRACE) {
                        out.append("\n");
                    }
                    break;

                case COMMENT:
                    // Skip directives, already processed
                    break;

                case NEWLINE:
                    // Preserve some newlines for readability
                    break;
            }
        }

        out.append("    }\n");
        out.append("}\n");

        return out.toString();
    }
}
```

## Example: Token Stream → Code

### Template
```html
{!-- @field User user --}
{!-- @import com.example.model.User --}

<div class="profile">
    <h1>{user.name}</h1>
    % if (user.isAdmin()) {
        <div class="admin">Admin</div>
    % }
</div>
```

### Token Stream (Simplified)
```
COMMENT: "{!-- @field User user --}"
COMMENT: "{!-- @import com.example.model.User --}"
HTML: "<div class=\"profile\">"
HTML: "<h1>"
EXPRESSION: "user.name"
HTML: "</h1>"
JAVA_KEYWORD: "if"
LPAREN: "("
IDENTIFIER: "user"
DOT: "."
IDENTIFIER: "isAdmin"
LPAREN: "("
RPAREN: ")"
RPAREN: ")"
LBRACE: "{"
HTML: "<div class=\"admin\">Admin</div>"
RBRACE: "}"
HTML: "</div>"
```

### Generated Code
```java
package com.example.views;

import com.example.model.User;
import lombok.Getter;
import lombok.Setter;
import java.io.IOException;

public class ProfileView extends OkygraphView {

    @Getter @Setter
    private User user;

    public ProfileView() {}

    public ProfileView(User user) {
        this.user = user;
    }

    @Override
    protected void render() throws IOException {
        writeRaw("<div class=\"profile\">");
        writeRaw("<h1>");
        write(user.name);
        writeRaw("</h1>");
        if (user.isAdmin()) {
            writeRaw("<div class=\"admin\">Admin</div>");
        }
        writeRaw("</div>");
    }
}
```

## Advantages of Token Stream Approach

### 1. **Simplicity**
- ✅ No AST node classes
- ✅ No AST traversal
- ✅ Fewer lines of code
- ✅ Easier to understand

### 2. **Performance**
- ✅ One or two passes max
- ✅ No tree allocation
- ✅ Better cache locality (sequential access)
- ✅ Lower memory usage

### 3. **Maintainability**
- ✅ Direct token → code mapping
- ✅ Easy to debug (print tokens, see output)
- ✅ Easy to extend (new token type → new case)

### 4. **Error Reporting**
- ✅ Tokens already have line numbers
- ✅ Can point to exact token in error message

### 5. **Philosophy Alignment**
- ✅ "Code is perfect when nothing can be taken away"
- ✅ No unnecessary abstraction layer
- ✅ Simplest thing that works

## Potential Challenges

### 1. **Java Block Formatting**
**Challenge:** Preserving indentation and newlines

**Solution:** Track indentation level, emit newlines appropriately
```java
int indent = 0;
for (Token token : tokens) {
    if (token.type() == LBRACE) {
        indent++;
        out.append(" {\n");
    } else if (token.type() == RBRACE) {
        indent--;
        out.append(indent(indent)).append("}\n");
    }
}
```

### 2. **Complex Expressions**
**Challenge:** Detecting field name in `orders[0].items.get(1).name`

**Solution:** Simple heuristic (split on `.`, take first part) works 90%
For complex cases, explicit `@field` directive

### 3. **Raw Expression Detection**
**Challenge:** Detecting `{raw(html)}` vs `{html}`

**Solution:** Simple string check
```java
boolean isRawExpression(String expr) {
    return expr.trim().startsWith("raw(") && expr.trim().endsWith(")");
}

String unwrapRaw(String expr) {
    return expr.trim().substring(4, expr.length() - 1);
}
```

## Implementation Plan

### Phase 1: Basic Transpiler (MVP)
1. ✅ Tokenizer (already done)
2. Field detector from tokens (simple heuristic)
3. Code generator (two-pass)
4. Test: Simple template → Java class

### Phase 2: Directive Support
5. Parse `@field` directives
6. Parse `@import` directives
7. Generate fields with types
8. Test: Template with directives → Type-safe class

### Phase 3: Java Block Support
9. Handle Java keywords, operators, braces
10. Preserve indentation
11. Test: Template with `if/for` → Correct Java code

### Phase 4: Polish
12. Error reporting with line numbers
13. Edge case handling
14. Performance optimization
15. Integration tests

## Summary

**Token stream is sufficient! No AST needed!**

**Philosophy:**
- "Code is perfect when nothing can be taken away"
- AST is unnecessary abstraction
- Token stream already has everything we need

**Architecture:**
```
Template → Tokens → Code
```

**Not:**
```
Template → Tokens → AST → Code
```

**Result:**
- ✅ Simpler code
- ✅ Faster compilation
- ✅ Lower memory
- ✅ Easier maintenance
- ✅ Same functionality

**This is the Okygraph way!** 🚀
