---
status: accepted
date: 2025-08-05
---
# 0038. Template syntax: Java control structures in backticks, expressions in {…}

## Context and Problem Statement

For template methods, it had to be determined how HTML content, Java expressions, and control flow in the template body are told apart.

## Considered Options

* Template syntax: Java control structures in backticks, expressions in {…} (chosen)
* `elseif` as its own keyword
* Custom template features (includes, special variables, template comments)

## Decision Outcome

Chosen option: A method body whose opening brace is followed by a backtick (`{` + backtick) is a template body; it ends with a backtick + `}`. Control structures are plain Java between backticks (e.g. `if (x) {`, `} else if (y) {`, `} else {`, `for (...) {`, `}`) and each sits on its own line. Expressions are always written as non-nested `{...}` (also inside attributes, e.g. `<div class="{className}">`) and are transpiled as arguments to a write(...) call; anything that is allowed as a Java argument is permitted. Escapes are `\{` and `` \` `` (ADR-0118). There are no template-specific comments, special variables, or imports/includes; `else if` is used instead of `elseif`;

Rationale: No new syntax vocabulary: anyone who knows Java knows the template control logic; the result is fast and has no runtime dependencies.

### Options not chosen

* **`elseif` as its own keyword** — `else if` is valid Java and therefore more consistent.
* **Custom template features (includes, special variables, template comments)** — Everything should be solvable with plain Java (method calls).
