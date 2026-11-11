---
status: accepted
date: 2026-11-11
---
# 0121. Escaping: text by default, attr() for quoted attribute values

## Context and Problem Statement

`write(String)` escapes `&`, `<` and `>`. Inside a double-quoted attribute that is not enough: a `"` in the value ends the attribute, so `<img alt="{name}">` with a name like `6" pizza` produces broken markup and an injection hole. ADR-0090 left explicit escaping helpers out of the MVP and moved JS and CSS into files. The benchmark fixture, a food name with quotes in an image `alt`, showed that attributes cannot wait.

## Considered Options

* `attr()` helper that returns `Raw`, chosen by the template author (chosen)
* `write(String)` escapes quotes as well
* Detect the attribute context by HTML tokenization in the transpiler
* Distinct wrapper records `Attr`, `Js`, `Css` with overloaded `write()` methods

## Decision Outcome

Chosen option: The base class gets `protected static Raw attr(String s)`. It escapes `&`, `<`, `>`, `"` and `'`; `null` prints nothing. Text stays `{x}`; inside a quoted attribute the author writes `{attr(x)}`. `{raw(x)}` stays the only unescaped output (ADR-0089).

Not part of this decision: JS, CSS and URL helpers (scripts and styles stay in files, as in ADR-0090) and automatic context detection, which needs HTML tokenization and may come later.

Rationale: It is the smallest change. `attr()` returns the existing `Raw`, so there is no new overload and no change in the transpiler, and it is explicit like `raw()`. The text path keeps its three replacements. The cost is that a forgotten `attr()` in an attribute stays unsafe, which only context detection would remove.

### Options not chosen

* **Escape quotes in `write(String)`** — Safe by default, but it adds two passes to every text string and makes `attr()` pointless.
* **Context detection** — Needs HTML tokenization in the transpiler; later.
* **Wrapper records with overloads** — More code for the same behavior; the helper methods can be added later in the same style.
