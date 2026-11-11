---
status: accepted
date: 2026-11-08
---
# 0115. Tokenizer and transpiler in one class; the catch declaration ends at EXPRESSION_START in the CATCH context

## Context and Problem Statement

An earlier plan (October 2025) was a separate `Tokenizer` class plus an emitter class `Transpiler`, and a new token type `CATCH_END`. The code kept both in one class and needs no extra token type.

## Decision Outcome

Chosen option (as implemented): `Transpiler` contains the line-based tokenizer loop (`transpile`, `matchToken`, `updateContext`) and the emitter (`emit`) in one class; tokens are emitted in a single pass without a token list (ADR-0060). There is no `CATCH_END`: in the `CATCH` context the `{` pattern (`BRACE_OPEN`, type `EXPRESSION_START`) closes the catch declaration; the emitter then writes `{` plus `this.discardBuffer();` and the context returns to `JAVA`.
