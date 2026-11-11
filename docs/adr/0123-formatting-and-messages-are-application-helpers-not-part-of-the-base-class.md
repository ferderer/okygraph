---
status: accepted
date: 2026-11-11
---
# 0123. Formatting and messages are application helpers, not part of the base class

## Context and Problem Statement

The Spring base class carried four formatters (`formatDate`, `formatTime`, `formatDateTime`, `format` for numbers). Nothing in the examples or the benchmark templates called them. They built a formatter on every call, and `format` created a `NumberFormat` per call, which was the slowest variant of those measured. They also depended on `LocaleContextHolder`, a locale source that only Spring provides, so the standalone base class could not have them. Applications that need better formatting (ICU4J) had to work around the defaults.

## Considered Options

* Remove the formatters from every base class; document an ICU4J recipe and show it in the examples (chosen)
* Keep them and make them fast (one formatter per locale)
* A plugin option to choose the formatter flavour
* Separate base templates per formatter flavour

## Decision Outcome

Chosen option: `SPRING.template` loses the four formatters and its `LocaleContextHolder` import; `STANDALONE.template` has none. Formatting and message lookup are application helpers in the app's layout class, like i18n (ADR-0091) and the CSRF field (ADR-0117). The README has a recipe, and `examples/ferderer.de` shows it: `t(key, args)` with ICU messages (plural, date, number) and `number(value, decimals)` with ICU number formatters cached per locale; ICU number formatters are thread-safe, ICU message formats are not and are kept per thread.

Rationale: What a base class should format is the application's choice (JDK, ICU4J, a hand-written formatter for fixed shapes), so any default is the wrong one for someone, and a faster default would still need a locale source. Removing them keeps the base class a contract with the framework only and makes the Spring and standalone base classes the same kind of thing.

### Options not chosen

* **Keep them, faster** — Still a choice made for every application, and still tied to a locale source.
* **Plugin option** — A new feature to maintain for code applications do better themselves.
* **Separate base templates** — Framework times formatter flavour; the templates multiply.
