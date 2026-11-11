# Okygraph

Okygraph is a minimal compile-time Java template engine delivered as a Maven plugin. It transpiles `.oky` files (Java classes whose methods contain HTML in backtick regions plus `{expr}` expressions) into plain Java before compilation. The output has no runtime dependencies, type checking is left to the Java compiler. Open source under the Apache License 2.0. Coordinates: `dev.okygraph:okygraph-maven-plugin:1.0.0` (not yet published).

## Repository layout

```
okygraph-maven-plugin/            Maven plugin, standard layout (published library)
  src/main/java/dev/okygraph/maven/   OkygraphMojo, Transpiler, TokenPattern, ParsingContext, TokenType
  src/main/resources/base-classes/    SPRING.template = OkyTemplate with ${package}
  src/test/java/dev/okygraph/maven/   pattern tests (one class per group) + TranspilerTest
okygraph-vscode/                  VS Code extension phase 1 (grammar + snippets, no build)
examples/ferderer.de/             Spring Boot site using the plugin (DefaultLayout with t(key), five page templates)
examples/sci-life/                Sci-Life templates (Layout, LoginView, LandingView)
docs/adr/                        architecture decision records (ADR-NNNN in this file refer to them)
```

## Build and test (verified 2026-11-11, JDK 25, Maven 3.9.16)

- Plugin: `cd okygraph-maven-plugin && mvn -B verify` → 90 tests, all green; builds `target/okygraph-maven-plugin-1.0.0.jar`.
- Release build: `mvn -B -Prelease -Dgpg.skip verify` builds plugin, sources and javadoc jars (verified). Publishing: `mvn -Prelease deploy` uploads to the Sonatype Central Portal via central-publishing-maven-plugin (needs a GPG key, a portal user token as server `central` in `~/.m2/settings.xml` and the verified namespace `dev.okygraph`; `autoPublish` is off, so the deployment is published in the portal). Not run yet.
- VS Code extension: no build; check the JSON with `python3 -m json.tool <file>` for the four files in `okygraph-vscode/`.
- `examples/ferderer.de`: uses the plugin (version 1.0.0 is not published yet, so first `cd okygraph-maven-plugin && mvn install`), then `mvn -B package` and `java -jar target/site-1.0.0.jar`; `/`, `/about`, `/projects`, `/blog` render through `DefaultLayout` (verified 2026-11-11).
- `examples/sci-life`: templates only, no build file. They transpile with the current plugin and compile against `SPRING.template` (Spring 7, Spring Security 7, SLF4J on the classpath).

## Current state

- The plugin has 361 LOC in 5 Java files (`OkygraphMojo` 87, `Transpiler` 188, `TokenPattern` 58, `ParsingContext` 15, `TokenType` 13; without blank and comment lines). `OkyTemplate` has 117 LOC.
- `Transpiler` is the tokenizer and the emitter in one class (ADR-0115). It works line by line with `TokenPattern` regexes and a flat `ParsingContext` state machine (ADR-0059, ADR-0072). It emits `html()`/`write()` chains and text blocks in one streaming pass (ADR-0060, ADR-0062).
- `Transpiler` handles indentation via `saveIndent`/`writeIndent`/`flush` (ADR-0102), try/catch writer-stack injection (ADR-0087) and Unicode escapes (ADR-0057). CRLF/CR line endings are normalized first (ADR-0111).
- The mojo transpiles all `.oky` files on every run, without timestamp check (ADR-0112: timestamps miss changes in base classes and layouts; a full run takes about 9 ms in Sci-Life). It logs `Transpiled N .oky files in X ms`.
- `generateBaseClass()` writes `OkyTemplate.java` from `base-classes/<FRAMEWORK>.template` into `baseClassPackage` (default under `src/main/java`), only if the file does not exist yet. Only Spring has a template; other frameworks fail with a clear error.
- The VS Code extension has phase 1 done: TextMate grammar with custom HTML patterns (ADR-0110), the template region injected into all Java scopes (ADR-0119), and snippets. An LSP is planned later.
- Measured render time: about 30 µs warm, 655 µs cold (Sci-Life LoginView).

## Conventions and constraints

- Only two syntax concepts: a backtick switches between Java and HTML, and `{expr}` outputs a Java expression. Add no further template features; use plain Java instead (layouts via methods with `Content` lambdas, ADR-0092).
- Control flow (if/else/for/try-catch) is plain Java between backticks. Do not introduce @-directives, `#{}` or an AST.
- Transpile in a single streaming pass: `matchToken()` calls `emit(type, value, line)` directly. No token list.
- All regexes live in the `TokenPattern` enum:
  - no `^` prefix; match with `region()` + `lookingAt()`
  - one `TokenType` per pattern; `updateContext()` decides transitions
  - no TRY context; the CATCH context uses `CATCH_OPS` `(?:final\b\s*|[@.,|()])+`
  - the catch declaration ends at `{` in the CATCH context; there is no `CATCH_END`
- Static HTML goes out via `html()`: a single line as `html("...\n")`, multiple lines as a text block. Fragmented lines are chained, e.g. `html(...).write(expr)`. Text blocks open and close at the shallowest line indent (ADR-0120); output indentation is best effort.
- `write(String)` HTML-escapes `&`, `<`, `>`; inside a quoted attribute the author writes `{attr(x)}`, which also escapes `"` and `'` and returns a `Raw` (ADR-0121). Primitives are not escaped. Unescaped output uses only `{raw(x)}` with `record Raw`. There is no `write(Object)`, so a missing overload fails compilation. `html()`/`write()` are public (protected methods are not callable on a chain's return value).
- In HTML, `` \` `` and `\{` print a backtick and a brace; every other backslash is literal (ADR-0118). Generated text blocks encode backslashes and `"""`.
- No JS/CSS/URL escaping helpers and no context detection yet (ADR-0121): the author chooses text, `attr` or `raw`; move JS/CSS into external files.
- For try/catch in templates, emit:
  - `this.pushBuffer()` after `try {`
  - `this.commitBuffer()` before `} catch`
  - `this.discardBuffer()` at the start of the catch block

  The writer-stack root is the response writer.
- `.oky` files sit next to `.java` files in `src/main/java`. Output goes to `target/generated-sources` with the package tree, and the mojo registers it as a compile source root (ADR-0113). IDEs may still need build-helper-maven-plugin.
- The generated base class holds framework integration only (Spring MVC, Servlet API, SLF4J). App helpers that need other libraries, e.g. `csrfInput()` for Spring Security, belong in the app's layout class (ADR-0117, like i18n in ADR-0091).
- The mojo requires `framework` (enum, upper case: `SPRING`, `QUARKUS`, `MICRONAUT`, `STANDALONE`) and `baseClassPackage`. No runtime JARs and no Spring Boot starter.
- Normalize CRLF/CR, then resolve Unicode escapes before tokenizing (ADR-0111). On syntax errors, report the line, the column and a `^` marker.
- Pattern tests use regions (start at 0 and mid-line), with one test class per pattern group. Avoid `@CsvSource` for char literals.
- Publish no unmeasured performance claims; mark estimates as estimates.

## Decisions

Architecture decisions are in [docs/adr](docs/adr/README.md). The ADR numbers in this file refer to them.
