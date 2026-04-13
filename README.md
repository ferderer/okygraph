# Okygraph

> ὠκύγραφος — Greek for *swift writer*

A compile-time Java template engine in 330 lines. No runtime dependencies. No custom syntax to learn. No ViewResolver configuration. Just Java with HTML inside.

## Two Concepts

````
`   switches between Java and HTML
{}  outputs a Java expression
````

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
        <img src="{user.getAvatar()}">
    `} catch (AppException e) {`
        <img src="/img/defaultAvatar.png">
    `}`
`}
```

No other Java template engine supports `try/catch` inside templates — because in Okygraph, it's just Java.

The Maven plugin transpiles this to pure Java at compile time. The result is a regular Java class — fully type-safe, debuggable, refactorable in any IDE, optimizable by the JIT.

## Setup

```xml
<plugin>
    <groupId>de.ferderer.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- required -->
        <framework>spring</framework> <!-- spring | quarkus | micronaut | standalone -->
        <baseClassPackage>com.example.views</baseClassPackage>

        <!-- optional, shown with defaults -->
        <sourceDirectory>${project.basedir}/src/main/java</sourceDirectory>
        <outputDirectory>${project.build.directory}/generated-sources</outputDirectory>
        <baseClassOutputDirectory>${project.basedir}/src/main/java</baseClassOutputDirectory>
    </configuration>
</plugin>
```

Run `mvn compile`. The plugin generates the view implementation. No Spring Boot starter required — the generated class implements `org.springframework.web.servlet.View` directly.

## Why

Every Java template engine adds a runtime layer: expression evaluation, caching, view resolution, framework integration. Okygraph removes all of it.

| | Okygraph | JTE | Thymeleaf |
|---|---|---|---|
| Hot path | ~0.1ms | ~0.8ms | ~7ms |
| Runtime deps | 0 | 1 | 8+ |
| GraalVM native | ✓ | ✓ | ✗ |
| try/catch in template | ✓ | ✗ | ✗ |

There is no cold start — templates are Java methods, called like any other code. Performance advantage grows with template complexity: the JIT can inline everything.


## Features

- **Full Java inside templates** — conditions, loops, try/catch, method calls, lambdas
- **Framework-agnostic** — switch `<framework>` to target Spring, Quarkus, Micronaut, or standalone
- **Zero runtime overhead** — transpiles to bytecode at compile time
- **Zero dependencies** — the generated code depends only on what you choose
- **IDE support** — refactoring, go-to-definition, and debugging work as expected

## Philosophy

The goal was to find the minimum surface area for a template engine and remove everything else. 330 lines. Nothing left to remove.

## License

MIT
