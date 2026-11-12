---
status: accepted
date: 2026-11-12
---
# 0124. raw() and attr() write directly; strings that need no escaping are written whole

## Context and Problem Statement

`raw()` and `attr()` returned a `Raw` wrapper that `write()` then printed (ADR-0089, ADR-0121). The wrapper was allocated on every use and could not be combined with other values, which is why helper code needed `.value()`. `attr()` also built the escaped string before anything was written. `write(String)` escaped with three `String.replace` passes, although almost every string needs no escaping. In the benchmark, escaping was a third of the time of the food page.

## Considered Options

* `raw()` and `attr()` write directly and return `this` (chosen)
* Keep the `Raw` wrapper
* Symbols for attribute and raw output, such as `{@x}` and `{!x}`
* The transpiler recognizes `{raw(x)}` and `{attr(x)}` and emits direct calls

## Decision Outcome

Chosen option: `raw(String)` and `attr(String)` are instance methods that write their argument at once and return `this`, like `html()`. `{raw(x)}` is still transpiled to `write(raw(x))`: Java evaluates the argument, which writes, and `write(OkyTemplate)` accepts `this` and does nothing. It rejects any other template, so one passed by mistake fails instead of printing nothing. There is no `Raw` type, and `null` writes nothing. `attr` escapes `&`, `<`, `>`, `"` and `'`, `write(String)` the first three (ADR-0121).

Because they write when evaluated, `raw` and `attr` must be the whole expression: `{raw(x)}`, not `{raw(x) + y}`. A helper that produces markup writes it and returns `this`; a CSRF field is `html(...)`, `attr(token)`, ... and `{csrfInput()}` works as before.

Escaping: a string that contains none of the characters to replace, which is nearly every string, is written whole; the rare one goes through `replace`. The test for "nothing to replace" is a loop for strings under 64 characters and the JDK's vectorized `indexOf` from 64 on, which is faster for long text (measured).

Rationale: No allocation, no new syntax and no change in the transpiler, and the one-pass check removes most of the escaping cost. The cost is that `raw` and `attr` are no longer values, and a rule to remember.

### Options not chosen

* **Keep `Raw`** — Allocates, cannot be combined, and `attr()` copies the string first.
* **Symbols** — A new syntax concept in a language that has two; the wrapper-free methods give the same result without one.
* **Recognize `raw(...)` in the transpiler** — Makes two method names reserved words of the expression syntax for no gain over the methods.
