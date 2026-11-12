# Okygraph

Okygraph is a minimal compile-time Java template engine delivered as a Maven plugin. It transpiles `.oky` files (Java classes whose methods contain HTML in backtick regions plus `{expr}` expressions) into plain Java before compilation. The output has no runtime dependencies, type checking is left to the Java compiler. Open source under the Apache License 2.0. Coordinates: `dev.okygraph:okygraph-maven-plugin:1.0.0` (not yet published).

## Repository layout

```
okygraph-maven-plugin/            Maven plugin, standard layout (published library)
  src/main/java/dev/okygraph/maven/   OkygraphMojo, Transpiler, TokenPattern, ParsingContext, TokenType
  src/main/resources/base-classes/    SPRING.template, STANDALONE.template = OkyTemplate with ${package}
  src/test/java/dev/okygraph/maven/   pattern tests (one class per group) + TranspilerTest
okygraph-vscode/                  VS Code extension phase 1 (grammar + snippets, no build)
examples/ferderer.de/             Spring Boot site using the plugin (DefaultLayout with ICU4J t(key, args) and number(), five page templates)
examples/sci-life/                Sci-Life templates (Layout, LoginView, LandingView)
docs/adr/                        architecture decision records (ADR-NNNN in this file refer to them)
```

## Build and test (verified 2026-11-11, JDK 25, Maven 3.9.16)

- Plugin: `cd okygraph-maven-plugin && mvn -B verify` → 95 tests, all green; builds `target/okygraph-maven-plugin-1.0.0.jar`.
- Release build: `mvn -B -Prelease -Dgpg.skip verify` builds plugin, sources and javadoc jars (verified). Publishing: `mvn -Prelease deploy` uploads to the Sonatype Central Portal via central-publishing-maven-plugin (needs a GPG key, a portal user token as server `central` in `~/.m2/settings.xml` and the verified namespace `dev.okygraph`; `autoPublish` is off, so the deployment is published in the portal). Not run yet.
- VS Code extension: no build; check the JSON with `python3 -m json.tool <file>` for the four files in `okygraph-vscode/`.
- `examples/ferderer.de`: uses the plugin (version 1.0.0 is not published yet, so first `cd okygraph-maven-plugin && mvn install`), then `mvn -B package` and `java -jar target/site-1.0.0.jar`; `/`, `/about`, `/projects`, `/blog` render through `DefaultLayout` (verified 2026-11-11).
- `examples/sci-life`: templates only, no build file. They transpile with the current plugin and compile against `SPRING.template` (Spring 7, Spring Security 7, SLF4J on the classpath).

## Current state

- The plugin has 380 LOC in 5 Java files (`OkygraphMojo` 87, `Transpiler` 207, `TokenPattern` 58, `ParsingContext` 15, `TokenType` 13; without blank and comment lines). `OkyTemplate` has 108 LOC (`SPRING`) and 98 LOC (`STANDALONE`); neither formats anything (ADR-0123).
- `Transpiler` is the tokenizer and the emitter in one class (ADR-0115). It works line by line with `TokenPattern` regexes and a flat `ParsingContext` state machine (ADR-0059, ADR-0072). It emits `html()`/`write()` chains and text blocks in one streaming pass (ADR-0060, ADR-0062).
- `Transpiler` handles indentation via `saveIndent`/`writeIndent`/`flush` (ADR-0102), try-block buffer injection (ADR-0125) and Unicode escapes (ADR-0057). CRLF/CR line endings are normalized first (ADR-0111).
- The mojo transpiles all `.oky` files on every run, without timestamp check (ADR-0112: timestamps miss changes in base classes and layouts; a full run takes about 9 ms in Sci-Life). It logs `Transpiled N .oky files in X ms`.
- `generateBaseClass()` writes `OkyTemplate.java` from `base-classes/<FRAMEWORK>.template` into `baseClassPackage` (default under `src/main/java`), only if the file does not exist yet. Only `SPRING` and `STANDALONE` have a template; other frameworks fail with a clear error. `STANDALONE` renders into the `java.io.Writer` it is given (`render(Writer)`), wraps and buffers nothing, reports I/O errors as `UncheckedIOException`, and has no formatters (ADR-0122).
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
- `write(String)` HTML-escapes `&`, `<`, `>` in one pass; inside a quoted attribute the author writes `{attr(x)}`, which also escapes `"` and `'` (ADR-0121); unescaped output is `{raw(x)}` (ADR-0089). `raw()` and `attr()` write immediately and return `this`, and `write(OkyTemplate)` accepts only `this`, so each must be the whole expression (ADR-0124). Primitives are not escaped. There is no `write(Object)`, so a missing overload fails compilation. `html()`/`write()` are public (protected methods are not callable on a chain's return value).
- In HTML, `` \` `` and `\{` print a backtick and a brace; every other backslash is literal (ADR-0118). Generated text blocks encode backslashes and `"""`.
- No JS/CSS/URL escaping helpers and no context detection yet (ADR-0121): the author chooses text, `attr` or `raw`; move JS/CSS into external files.
- For try/catch in templates, emit (ADR-0125):
  - `try (var oky1 = this.buffer()) {` for `try {`; the number is the nesting depth of open try blocks
  - `oky1.commit();` before `} catch`
  - nothing at the start of the catch block: the buffer is local to the block and `close()` restores the writer

  One catch clause per try (`catch (A | B e)`); a catch without a try is an error. The root writer is the response writer.
- `.oky` files sit next to `.java` files in `src/main/java`. Output goes to `target/generated-sources` with the package tree, and the mojo registers it as a compile source root (ADR-0113). IDEs may still need build-helper-maven-plugin.
- The generated base class holds framework integration only (Spring MVC, Servlet API, SLF4J). App helpers belong in the app's layout class (ADR-0117, like i18n in ADR-0091): `csrfInput()` for Spring Security, and number and message formatting, which the base class does not do (ADR-0123); `examples/ferderer.de` shows an ICU4J version with one formatter per locale.
- The mojo requires `framework` (enum, upper case: `SPRING`, `QUARKUS`, `MICRONAUT`, `STANDALONE`) and `baseClassPackage`. No runtime JARs and no Spring Boot starter.
- Normalize CRLF/CR, then resolve Unicode escapes before tokenizing (ADR-0111). On syntax errors, report the line, the column and a `^` marker.
- Pattern tests use regions (start at 0 and mid-line), with one test class per pattern group. Avoid `@CsvSource` for char literals.
- Publish no unmeasured performance claims; mark estimates as estimates.

## Decisions

Architecture decisions are in [docs/adr](docs/adr/README.md). The ADR numbers in this file refer to them.

## Review instructions

### Philosophy: code is perfect when nothing can be taken away

Review every change against one question: what can be removed without losing behavior?
Treat added code as a cost that must justify itself.

Flag as Important:
- Abstractions with a single implementation, factories/builders/strategies with one caller, and
  interfaces that exist only "for testing" or "for later".
- Configuration, feature flags, parameters, or extension points that nothing uses today.
- New dependencies where the JDK or Spring Boot already provides the capability.
- Dead code, unused fields/methods/branches, commented-out code, speculative generality.
- Layers that only delegate (pass-through services, DTO-to-DTO copies, wrapper classes).
- A change that adds code where deleting or simplifying existing code would solve the same problem.

Flag as Nit:
- Redundant comments that restate the code, needless intermediate variables, overly defensive checks
  for states the type system or callers already exclude.

When flagging, always name what to delete or collapse, and show the smaller version.
Do not praise. Do not suggest additions unless a correctness or security defect requires them.

### Performance

Flag as Important when a change plausibly affects runtime behavior at scale:
- N+1 queries, lazy-loading inside loops, missing pagination or unbounded result sets.
- Blocking calls on hot paths or inside transactions; transactions held open longer than needed.
- Repeated work in loops (regex compilation, object mapper creation, reflection, string concatenation).
- Unnecessary allocation, boxing, or stream pipelines in tight loops; collections with the wrong
  complexity for their access pattern.
- Missing indexes implied by new query predicates; over-fetching columns or entities.
- Synchronization or shared mutable state that serializes concurrent work.

Claims about performance need evidence: cite `file:line` and explain the cost in terms of
complexity or I/O count. Do not report speculative micro-optimizations; prefer measurable
wins (fewer queries, fewer round-trips, lower complexity class) over cosmetic ones.

### Severity calibration

- Important: correctness or security defects, performance problems as listed above, and
  clear violations of "nothing left to remove".
- Nit: style or minor redundancy. Cap nits at 3 per review.
- Skip: generated files, lockfiles, and formatting handled by the formatter.

### Output

Lead with the verdict in one line. List findings by severity. If the change is already minimal
and fast, say so in one sentence and stop.
