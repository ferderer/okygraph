---
status: accepted
date: 2025-10-23
---
# 0062. HTML output: text blocks for whole lines, method chaining for fragments

## Context and Problem Statement

HTML lines and lines with expressions should be readable and compact in the generated Java. Escaping should stay minimal.

## Considered Options

* HTML output: text blocks for whole lines, method chaining for fragments (chosen)
* `this.` prefix before html/write
* One escaped write("…") per line

## Decision Outcome

Chosen option: Whole HTML lines (token value equal to the line) are collected in a buffer and, when left, emitted via backtick as a Java text block `html("""…""");`. Lines with expressions are generated as a chain without `this`, for example `html("<div>").write(user.name).html("</div>");`. `checkFragmentedLine()` appends `.` and flushes the buffer at the first chain component. At NEWLINE, a `;` is appended if the chain was started. If the HTML buffer is not empty, the line break is not emitted twice. The line break after fragments is currently emitted as `.html("\n")`.

Rationale: Text blocks avoid escaping and preserve formatting; chaining keeps the calls compact. `html` was preferred over `writeRaw`.

### Options not chosen

* **`this.` prefix before html/write** — Not used.
* **One escaped write("…") per line** — Text block is more readable and needs no escaping.
