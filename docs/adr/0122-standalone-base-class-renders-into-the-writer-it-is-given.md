---
status: accepted
date: 2026-11-11
---
# 0122. STANDALONE base class: renders into the Writer it is given, no framework types

## Context and Problem Statement

`Framework.STANDALONE` existed in the plugin parameter, but only `SPRING` had a base class template, so a project without Spring (a library, a batch job, e-mail or report generation, tests) could not use the plugin, and the benchmark had to drive the Spring base class with stubs for the servlet request and response. The Spring base class also showed that a framework writer should not be wrapped (ADR-0001, ADR-0125).

## Considered Options

* `STANDALONE.template` with `render(Writer)`, the writer used as it is (chosen)
* A `PrintWriter`-based base class like the Spring one
* A base class that returns a `String` from `render()`
* Wait for the Quarkus and Micronaut templates and derive a common base

## Decision Outcome

Chosen option: A second template, `base-classes/STANDALONE.template`. `public void render(Writer writer)` uses the writer exactly as given: no wrapping, no buffering, no flushing. The output API is the same as in the Spring template (`html`, the `write` overloads, `raw`, `attr`, `Content`, and the writer stack behind `try/catch`), so a template compiles against either base class. Differences: a failing writer surfaces as `UncheckedIOException`, because the chaining methods cannot throw checked exceptions, and there are no formatters, because they need a locale source that only a framework provides. Applications bring their own, for example ICU4J.

Rationale: It needs no framework type, so it is the smallest base class (79 lines) and the one to benchmark against engines that take a `Writer`. Taking a `Writer` instead of a `PrintWriter` keeps the caller in control: a `StringWriter`, a servlet writer or a file writer all work without an adapter layer.

### Options not chosen

* **`PrintWriter`-based** — A `PrintWriter` swallows I/O errors and adds a lock; callers with a `Writer` would have to wrap it.
* **`String render()`** — Forces a copy of the whole page.
* **Wait for other frameworks** — Standalone is needed now and does not depend on them.
