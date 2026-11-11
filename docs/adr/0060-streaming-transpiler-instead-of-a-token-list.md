---
status: accepted
date: 2025-10-21
---
# 0060. Streaming transpiler instead of a token list

## Context and Problem Statement

The tokenizer stored tokens in a list, which the transpiler then processed. The writer stack and merging of HTML lines are simpler in a single pass.

## Considered Options

* Streaming transpiler instead of a token list (chosen)
* Token list with separate transpiler pass

## Decision Outcome

Chosen option: Tokenizer and transpiler work in a single pass. Instead of `tokens.add(...)`, the tokenizer calls `emit(type, value, line, …)`, which writes the transpiled Java code directly to a writer. The parsing context is available at that point, and in-place transpilation simplifies writer-stack injection and HTML line bundling.

Rationale: No token allocation, no second pass, simpler architecture. Debuggability and multi-pass optimizations are dispensable for this purpose.

### Options not chosen

* **Token list with separate transpiler pass** — More complexity; writer-stack injection would need lookahead across multiple tokens.
