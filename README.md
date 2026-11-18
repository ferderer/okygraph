# Okygraph

> ὠκύγραφος — Greek for *swift writer*

A compile-time Java template engine. 361 lines of plugin code. Zero lines of runtime you need to understand. No runtime dependencies. No custom syntax to learn. No ViewResolver configuration. Just Java with HTML inside.

## Two Concepts

```
`   switches between Java and HTML
{}  outputs a Java expression
```

That's the entire syntax — including conditions, loops, and try/catch:

```java
protected void render() {`
    <p>Greetings {user.name}!</p>
    `if (user.isActive()) {`
        <p>User is active</p>
    `} else {`
        <p>User is inactive</p>
    `}`
    <h3>Hobbies of {user.name}:</h3>
    <ul>
        `for (var hobby : user.getHobbies()) {`
            <li>{hobby}</li>
        `}`
    </ul>
    `try {`
        <img src="{attr(user.getAvatar())}">
    `} catch (AppException e) {`
        <img src="/img/defaultAvatar.png">
    `}`
`}
```

`try/catch` works in templates because it is just Java: output written inside a failed `try` block is discarded.

To print a literal backtick or `{` in HTML, escape it with a backslash: `` \` `` and `\{`. Other backslashes are literal.

An expression `{expr}` may continue on the following lines, for example a chain of calls.

Expressions inside `{expr}` must not contain nested Java braces. Array initializers, block lambdas, anonymous classes and switch expressions with braces are intentionally unsupported here; compute such values in the model and reference the result in the template. Braces inside Java string or character literals are allowed, for example `{"{}"}`.

### Escaping

`{expr}` escapes `&`, `<` and `>`, which is right for text. Inside a quoted attribute write `{attr(expr)}`: it also escapes `"` and `'`. `{raw(expr)}` prints unescaped HTML. Okygraph does not guess the context, so choosing `attr` is the template author's job. `raw` and `attr` write their output immediately, so each must be the whole expression: `{raw(x)}`, not `{raw(x) + y}`. There is no JS or CSS escaping yet: keep scripts and styles in their own files.

The Maven plugin transpiles this to pure Java at compile time. The result is a regular Java class — fully type-safe, debuggable, refactorable in any IDE, optimizable by the JIT.

## Setup

```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <framework>SPRING</framework> <!-- SPRING | QUARKUS | MICRONAUT | STANDALONE -->
        <baseClassPackage>com.example.views</baseClassPackage>
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

> Not on Maven Central yet. Install the plugin locally first: `cd okygraph-maven-plugin && mvn install`.

Run `mvn compile`. The plugin transpiles `.oky` files found under `src/main/java` to regular `.java` files in `target/generated-sources` and adds that directory to the compile sources. No Spring Boot starter required — with `SPRING` the base class `OkyTemplate` implements `org.springframework.web.servlet.View` directly.

On the first build the plugin also writes the base class `OkyTemplate.java` into `baseClassPackage` under `src/main/java` (`baseClassOutputDirectory`). It never overwrites an existing file, so you can adapt it; delete it to regenerate. `SPRING` and `STANDALONE` have a base class template so far. `STANDALONE` needs no framework at all: `new HomeView(model).render(writer)` renders into any `java.io.Writer` and uses it as it is.

### App helpers

The generated base class only integrates the framework (Spring MVC, Servlet API, SLF4J). Put helpers that need other libraries into your own layout class between `OkyTemplate` and your views. For example, a CSRF field for Spring Security forms. An output helper writes and returns `this`:

```java
import org.springframework.security.web.csrf.CsrfToken;

public abstract class Layout extends OkyTemplate {
    protected OkyTemplate csrfInput() {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) return this;
        html("<input type=\"hidden\" name=\"");
        attr(token.getParameterName());
        html("\" value=\"");
        attr(token.getToken());
        return html("\">");
    }
}
```

Use it in a form as `{csrfInput()}`. See [`examples/sci-life`](examples/sci-life/src/main/java/art/scilife/common/web/Layout.oky).

### Formatting and messages

The base class does not format numbers, dates or messages. That needs a locale source, and creating a JDK formatter per call is the slowest way to format a number. Keep the formatters in your layout class, one per locale. With [ICU4J](https://unicode-org.github.io/icu/) (`com.ibm.icu:icu4j`), whose number formatters are immutable and thread-safe and whose message formats (plural, select, date, number) are not, so each thread keeps its own:

```java
private static final Map<String, LocalizedNumberFormatter> NUMBER_FORMATS = new ConcurrentHashMap<>();
private static final ThreadLocal<Map<String, MessageFormat>> MESSAGE_FORMATS = ThreadLocal.withInitial(HashMap::new);

protected String t(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    String pattern = ResourceBundle.getBundle("messages", locale).getString(key);
    if (args.length == 0) return pattern;
    return MESSAGE_FORMATS.get().computeIfAbsent(locale + "|" + key, k -> new MessageFormat(pattern, locale)).format(args);
}

protected String number(double value, int decimals) {
    Locale locale = LocaleContextHolder.getLocale();
    return NUMBER_FORMATS.computeIfAbsent(locale + "|" + decimals,
        k -> NumberFormatter.withLocale(ULocale.forLocale(locale)).precision(Precision.fixedFraction(decimals))).format(value).toString();
}
```

With `blog.count={0, plural, =0 {No articles yet} one {# article} other {# articles}}` in `messages.properties`, a template writes `{t("blog.count", entries.size())}` and `{number(minutes, 1)}`. The complete version, with German messages, is in [`examples/ferderer.de`](examples/ferderer.de/src/main/java/de/ferderer/site/DefaultLayout.oky) and [`BlogView.oky`](examples/ferderer.de/src/main/java/de/ferderer/site/BlogView.oky).

## Why

Every Java template engine adds a runtime layer: expression evaluation, caching, view resolution, framework integration. Okygraph removes all of it.

| | Okygraph | JTE | Thymeleaf |
|---|---|---|---|
| Hot path | ~0.03 ms | ~0.8 ms | ~7 ms |
| Runtime deps | 0 | 1 | 8+ |

Okygraph's figure was measured on a view of a production app after JIT warm-up. The JTE and Thymeleaf figures and the dependency counts are rough estimates, not measurements; benchmarks will follow.

There is no cold start — templates are Java methods, called like any other code. Because templates are plain Java, the JIT can inline everything.

## Features

- **Full Java inside templates** — conditions, loops, try/catch, method calls, lambdas
- **Framework-neutral design** — `<framework>` selects the generated base class; only `SPRING` exists so far
- **No runtime template engine** — templates are transpiled to Java source at compile time
- **Zero dependencies** — the generated code depends only on what you choose
- **IDE support** — refactoring, go-to-definition, and debugging work as expected

## Philosophy

The goal was to find the minimum surface area for a template engine and remove everything else. ~360 lines. Nothing left to remove.

## Repository layout

```
okygraph-maven-plugin/   the Maven plugin (tokenizer/transpiler, mojo, Spring base-class template)
okygraph-vscode/         VS Code extension: TextMate grammar and snippets for .oky
examples/                ferderer.de (Spring Boot site using the plugin), sci-life (templates)
docs/adr/                architecture decision records
```

## Building

Targets Java 21 (`maven.compiler.release`); verified with JDK 25 and Maven 3.9.16.

```sh
cd okygraph-maven-plugin
mvn verify          # compile, 90 unit tests, plugin jar
```

The VS Code extension has no build step: it consists of four declarative JSON files.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

Maintained by [Vadim Ferderer](https://ferderer.de).
