---
status: accepted
date: 2025-08-05
---
# 0039. No AST: transpilation directly on the token stream

## Context and Problem Statement

After defining the tokens, the question arose whether an AST is needed for transpilation.

## Considered Options

* No AST: transpilation directly on the token stream (chosen)
* AST for structural validation, optimization, inheritance/macros

## Decision Outcome

Chosen option: The transpiler works linearly on the token stream (HTML→write()/text block, `{expr}`→write(expr), control structures without backticks passed through as Java); no AST is built. Template inheritance, macro expansion, and compile-time evaluation are not implemented – instead, regular Java classes/methods are used (e.g. a layout class).

Rationale: Pure template substitution with already valid Java; the Java compiler handles type checking. Simpler, faster, less memory.

### Options not chosen

* **AST for structural validation, optimization, inheritance/macros** — Over-engineering; unneeded features.
