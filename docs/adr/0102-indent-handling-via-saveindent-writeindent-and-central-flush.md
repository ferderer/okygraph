---
status: accepted
date: 2026-06-17
---
# 0102. Indent handling via saveIndent/writeIndent and central flush()

## Context and Problem Statement

Several cases (EXPRESSION_START, CATCH, flushHtmlBuffer) triggered writing the line indent; the lazy-indent solution using a boolean was confusing and error-prone.

## Considered Options

* Indent handling via saveIndent/writeIndent and central flush() (chosen)
* Separate boolean indentWritten with explicit writeIndent() calls at each call site

## Decision Outcome

Chosen option: The field `indent` is set per line via `saveIndent(line)` and serves as a sentinel: non-empty means not yet written, empty means already written or discarded. `writeIndent()` writes the indent exactly once and clears it. `flush()` is the only trigger point: it first empties the htmlCache (single line as `html("...")`, multi-line as a text block) and then calls `writeIndent()`. TRY saves `tryIndent = indent` before `flush()`, CATCH sets `indent = ""` before `flush()`, BACKTICK and default call `flush()`; `flushHtmlBuffer` is removed.

Rationale: A single trigger point avoids coupled double calls; an empty indent as sentinel makes flush() idempotent and requires no additional boolean.

### Options not chosen

* **Separate boolean indentWritten with explicit writeIndent() calls at each call site** — More trigger points; calls to flushHtmlBuffer and writeIndent are effectively always coupled.
