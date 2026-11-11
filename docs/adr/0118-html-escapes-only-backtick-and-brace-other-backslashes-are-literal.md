---
status: accepted
date: 2026-11-11
---
# 0118. HTML escapes: only \` and \{ are escapes, every other backslash is literal

## Context and Problem Statement

The HTML pattern lets the tokenizer swallow `\` plus any character, so `` \` `` and `\{` do not switch mode. The output side was never defined: the backslash was printed, whole lines inside a Java text block did not compile (`\d`, `` \` ``), `\t` became a tab, and `"""` ended the block. A lone backslash at the end of a line failed with "Unexpected character".

## Considered Options

* Backslash escape for backtick and brace (chosen)
* Doubled backtick (`` `` ``) as the escape
* No escape syntax; only the expression `{"`"}`

## Decision Outcome

Chosen option: In HTML, `` \` `` prints a backtick and `\{` prints a brace. Every other backslash, including `\\` and `\d`, is literal and printed unchanged. A literal backslash directly before a backtick or brace is written as an expression, `{"\\"}`. Generated Java encodes the text for its target: string literals as before, text blocks with doubled backslashes and `""\"` for `"""`.

Rationale: One rule covers both special characters and is local, so it needs no knowledge of the parser state. Backslashes in HTML (`pattern="\d+"`, CSS escapes) keep working. Doubling the backtick would leave `{` without an escape, and `` ``` `` would mean different things in Java and in HTML mode.

### Options not chosen

* **Doubled backtick** — Solves only the backtick; runs of backticks are ambiguous depending on the current mode.
* **Expression only** — Works, but is noisy and turns every affected line into a chained fragment.
