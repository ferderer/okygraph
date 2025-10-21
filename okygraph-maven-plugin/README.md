# Okygraph Maven Plugin

Clean implementation of the Okygraph template transpiler Maven plugin.

## Project Structure

```
okygraph-maven-plugin/
├── src/main/java/dev/okygraph/maven/
│   ├── OkygraphMojo.java           # Maven plugin entry point
│   ├── tokenizer/                   # Lexical analysis
│   │   ├── Token.java               # Token record
│   │   ├── TokenType.java           # Token type enumeration
│   │   ├── Tokenizer.java           # Main tokenizer
│   │   └── UnicodePreprocessor.java # Unicode escape handler
│   ├── transpiler/                  # Code generation (TODO)
│   └── generator/                   # Base class generation (TODO)
└── src/test/java/dev/okygraph/maven/
    └── tokenizer/
        ├── TokenizerTest.java
        └── UnicodePreprocessorTest.java
```

## Architecture

### 1. Tokenizer (COMPLETE)
- **Purpose**: Lexical analysis of .jmt files
- **Input**: Raw source code (String)
- **Output**: List of Token objects
- **Features**:
  - Full Java token recognition (keywords, identifiers, literals, operators)
  - Template boundary markers: `{`` and ``}`
  - Template expression markers: `{` and `}` in HTML mode
  - Backtick support for Java/HTML mode switching
  - Block comments and text blocks
  - Mode stack for context tracking
  - Line/column tracking for error reporting

### 2. Transpiler (TODO)
- **Purpose**: Convert tokens to Java code
- **Input**: List of Token objects
- **Output**: Generated .java file
- **Features**:
  - Framework-agnostic core
  - HTML to write() call conversion
  - Expression escaping
  - Control flow handling (if/for/while with backticks)
  - Exception handling with buffered writers

### 3. Generator (TODO)
- **Purpose**: Generate framework-specific base classes
- **Input**: Configuration (framework type)
- **Output**: OkygraphTemplate.java or custom base class
- **Features**:
  - Zero-dependency option (inline generation)
  - Spring/Quarkus/Micronaut support
  - Custom base class support

## Configuration Options

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Generate OkygraphTemplate class for zero dependencies -->
        <generateBaseClass>true</generateBaseClass>

        <!-- Base class that all views extend -->
        <baseClassName>dev.okygraph.OkygraphTemplate</baseClassName>

        <!-- Framework: auto, spring, quarkus, micronaut, standalone -->
        <framework>auto</framework>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>transpile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Testing

Run tests with:
```bash
mvn test
```

Current test coverage:
- ✅ Tokenizer: Comprehensive tests for all token types
- ✅ UnicodePreprocessor: Unicode escape handling tests
- ⏳ Transpiler: TODO
- ⏳ Generator: TODO

## Development Status

- [x] Project structure
- [x] Token and TokenType
- [x] Tokenizer core implementation
- [x] UnicodePreprocessor
- [x] Tokenizer tests
- [ ] Transpiler implementation
- [ ] Generator implementation
- [ ] Integration tests
- [ ] Documentation

## Notes

- **No AST**: Direct token-to-code transpilation for simplicity
- **Mode Stack**: Tracks JAVA vs TEMPLATE vs COMMENT contexts
- **Framework Detection**: Will be implemented when needed (not critical for initial transpiler)
- **Writer Abstraction**: Two types needed:
  1. Direct writer (e.g., ServletWriter)
  2. Buffered writer (for exception handling)
