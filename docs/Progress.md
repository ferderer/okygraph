# Okygraph Progress Tracker

## ✅ Phase 1: Foundation (COMPLETE)

### Tokenizer Implementation
- **Status**: ✅ Complete - 19/19 tests passing
- **Files**:
  - `Token.java` - Immutable token record
  - `TokenType.java` - Token type enumeration
  - `Tokenizer.java` - Main lexical analyzer (~356 lines)
  - `UnicodePreprocessor.java` - JLS-compliant Unicode handling
- **Features**:
  - Mode-based tokenization (JAVA, TEMPLATE, JAVA_EXPRESSION, etc.)
  - Lazy mode initialization to avoid circular dependencies
  - Line-by-line processing with NEWLINE tokens
  - Backtick toggle between Java and HTML modes
  - Expression parsing with `{expr}` syntax
  - Java keyword and operator recognition
  - Try/catch keyword support (for transpiler buffer management)

### Unicode Preprocessing
- **Status**: ✅ Complete - 13/13 tests passing
- **Features**:
  - JLS-compliant `\uXXXX` escape handling
  - Comment-aware processing
  - Multiple Unicode escape sequences
  - Invalid escape detection

### Base Class (OkygraphView)
- **Status**: ✅ Complete - 33/33 tests passing
- **Files**:
  - `OkygraphView.java` - Base class for all generated templates
  - `OkygraphViewTest.java` - Comprehensive test suite
- **Features**:
  - Abstract `render()` method for subclasses
  - Public rendering API: `render(Writer)`, `render(OutputStream)`, `renderToString()`
  - HTML escaping by default with `escape(Object)`
  - Overridable escaping for custom types
  - Raw HTML support with `raw(String)` (dangerous but necessary)
  - JavaScript escaping with `jsEscape(String)`
  - URL encoding with `urlEncode(String)`
  - Try/catch buffering: `pushBuffer()`, `popBufferCommit()`, `popBufferDiscard()`
  - Zero dependencies - pure Java
  - Framework-agnostic (works with Spring, Quarkus, Micronaut, etc.)

### Total Test Coverage
**65 tests, 0 failures** 🎉
- 33 OkygraphView tests
- 19 Tokenizer tests
- 13 UnicodePreprocessor tests

## 🔄 Phase 2: Transpiler (IN PROGRESS)

### Next Steps
1. **AST Node Design**
   - Design node types for template AST
   - Template structure: HTML blocks, expressions, Java blocks
   - Nested structure support
   - Template directives: `{!-- @field Type name --}`

2. **Field Detection & Type Inference**
   - Scan template for field references
   - Extract types from `@field` annotations
   - Support `@import` directives
   - Generate fields with getters/setters (Lombok support)

3. **Template Parser**
   - Convert token stream to AST
   - Parse template directives
   - Handle nesting (expressions in HTML, Java code blocks)
   - Validation (balanced braces, proper mode transitions)

4. **Code Generator**
   - Generate Java class with detected fields
   - Generate constructors (default + all-args)
   - Add configured annotations (@Component, @RequestScope, etc.)
   - Emit `writeRaw()` for HTML literals
   - Emit `write()` for expressions (auto-escaped)
   - Handle try/catch buffer management
   - Proper indentation and formatting

5. **Integration**
   - Connect parser to Maven plugin
   - File discovery (find `.oky` template files)
   - Package structure preservation
   - Incremental compilation support
   - Multiple template sets with different base classes

6. **Testing**
   - Parser unit tests
   - Field detection tests
   - Code generator unit tests
   - Integration tests (template → Java → execution)
   - Real-world template examples

## 📋 Phase 3: Maven Plugin (PLANNED)

### Configuration
```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <configuration>
        <templateDirectory>src/main/templates</templateDirectory>
        <outputDirectory>target/generated-sources/okygraph</outputDirectory>
        <templateExtension>.oky</templateExtension>
        <generateBaseClass>true</generateBaseClass>
        <baseClassName>OkygraphView</baseClassName>
        <basePackage>dev.okygraph.runtime</basePackage>
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

### Features
- Automatic template discovery
- Incremental compilation (only changed templates)
- Package structure mirroring
- IDE integration (generated sources marked as source folder)
- Error reporting with line numbers

## 📊 Phase 4: Documentation & Examples (PLANNED)

### Documentation Needed
- Getting started guide
- Template syntax reference
- XSS prevention best practices
- Framework integration guides (Spring, Quarkus, Micronaut)
- Migration guides (from JSP, Thymeleaf, etc.)
- Performance tuning

### Example Projects
- Simple Spring Boot app
- Quarkus REST API with templates
- Micronaut service
- Real-world e-commerce site

## 🎯 Design Principles

### 1. Simplicity
- **2 concepts only**: Backtick toggle and expressions
- No special directives or control structures
- Use Java for logic

### 2. Safety
- **XSS protection by default**: All expressions HTML-escaped
- **Try/catch buffering**: No partial output on errors
- **Type-safe**: Generated Java code with compile-time checks

### 3. Performance
- **Zero runtime overhead**: Direct method calls, no reflection
- **Efficient string building**: Writer-based, no intermediate strings
- **AOT-friendly**: No dynamic class loading

### 4. Framework Agnostic
- **Pure Java**: No framework dependencies
- **Flexible output**: Writer, OutputStream, or String
- **Integration helpers**: Easy to add framework-specific utilities

## 📝 Key Decisions Made

### 1. Backtick Toggle
✅ **Decision**: Single backtick (`) toggles between Java and HTML modes
- Simple and intuitive
- No special end markers needed
- Follows the "2 concepts" principle

### 2. Escaping Strategy
✅ **Decision**: `write(escape(expr))` pattern
- HTML escape by default (safe)
- Overridable `escape()` method for custom types
- `raw()` method for trusted HTML (dangerous but necessary)
- Method calls for special contexts: `jsEscape()`, `urlEncode()`

### 3. Try/Catch Handling
✅ **Decision**: Transpiler injects buffer management
- Keywords already recognized by tokenizer
- `pushBuffer()` at try block start
- `popBufferCommit()` at try block end
- `popBufferDiscard()` in catch block
- Prevents partial output on exceptions

### 4. Base Class Location
✅ **Decision**: Generate with project (Option A for v1)
- No runtime dependency
- Users can customize if needed
- Simple copy-paste friendly approach
- Can offer runtime JAR later as option

### 5. Framework Integration
✅ **Decision**: Single base class, no framework-specific subclasses (yet)
- Keep it simple for v1
- Works with all frameworks out of the box
- Users can add helpers in their own subclasses
- Can add framework modules later if needed

## 🚀 What's Working

```java
// This works today!
public class UserProfileView extends OkygraphView {
    private User user;

    public UserProfileView(User user) {
        this.user = user;
    }

    @Override
    protected void render() throws IOException {
        write("<div class=\"profile\">");
        write("\n    <h1>");
        write(escape(user.getName()));
        write("</h1>");
        write("\n</div>");
    }
}

// Usage
String html = new UserProfileView(user).renderToString();

// XSS Protection
User malicious = new User("<script>alert('xss')</script>");
String safe = new UserProfileView(malicious).renderToString();
// Result: <h1>&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;</h1>

// Try/Catch Buffering
try {
    view.pushBuffer();
    view.write("Risky content");
    if (error) throw new RuntimeException();
    view.popBufferCommit(); // Commits on success
} catch (Exception e) {
    view.popBufferDiscard(); // Discards on error
}
```

## 📅 Timeline

- ✅ **Week 1**: Tokenizer implementation
- ✅ **Week 2**: Base class implementation
- 🔄 **Week 3**: Transpiler (parser + code generator) - **CURRENT**
- ⏳ **Week 4**: Maven plugin integration
- ⏳ **Week 5**: Testing & examples
- ⏳ **Week 6**: Documentation & release

## 🎓 Lessons Learned

1. **Lazy initialization solves circular dependencies** elegantly
2. **Unicode escapes in comments** need special handling (Java processes `\u` before parsing)
3. **Declarative mode transitions** (TokenSpec) cleaner than imperative code
4. **2-concept simplicity** is the killer feature - resist feature creep
5. **Method calls** (like `raw()`, `jsEscape()`) better than special syntax
6. **write(escape())** separation more flexible than `writeHtml()`
7. **Zero dependencies** = maximum compatibility

## 🔗 Related Documents

- [Base Class Design.md](Base%20Class%20Design.md) - Detailed base class design
- [Try-catch handling in templates.md](Try-catch%20handling%20in%20templates.md) - Exception handling spec
- [Escape Modes.md](Escape%20Modes.md) - Escaping strategy and rationale
- [Okygraph Java Template Engine.md](Okygraph%20Java%20Template%20Engine.md) - Original design doc
