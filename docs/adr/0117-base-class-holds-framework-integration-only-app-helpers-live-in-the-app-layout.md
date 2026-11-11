---
status: accepted
date: 2026-11-08
---
# 0117. The base class holds framework integration only; app helpers such as CSRF live in the app's layout class

## Context and Problem Statement

The Spring `OkyTemplate` came from Sci-Life and contained `csrfInput()`, which imports `org.springframework.security.web.csrf.CsrfToken`. Since the plugin generates this class into every consumer project (ADR-0104), every project would have needed Spring Security on the classpath, including projects that do not use it.

## Considered Options

* Move app helpers out of the base class into the app's own layout class (chosen)
* Keep `csrfInput()` and read the `_csrf` request attribute via reflection
* A plugin option or a second template variant (e.g. `SPRING_SECURITY`)

## Decision Outcome

Chosen option: The generated base class contains only what the framework integration needs: rendering into the response with content type and encoding, the `html()`/`write()` overloads, `raw()`/`Content`, the writer stack for try/catch, and the formatters (ADR-0091). Its dependencies are Spring MVC, the Servlet API and SLF4J. Application helpers that need further libraries, such as a CSRF hidden field for Spring Security, go into the app's own layout class (`Layout`/`DefaultLayout`, or an intermediate class). The documentation shows the CSRF helper as a recipe.

Rationale: The base class is a minimal, framework-level contract. This is the same rule as ADR-0091, which keeps i18n out of `OkyTemplate`. The generated class lives in the user's source tree anyway and may be adapted there.

### Options not chosen

* **Reflection on the `_csrf` attribute** — No compile-time dependency, but it hides the dependency and is harder to read than six lines in the app.
* **Plugin option or second template** — A new feature and a second template to maintain for a single helper method.
