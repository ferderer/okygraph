---
status: accepted
date: 2025-09-17
---
# 0047. No write(Object); type checking by the Java compiler

## Context and Problem Statement

The question was whether the transpiler must analyze expression types to choose the matching write method.

## Considered Options

* No write(Object); type checking by the Java compiler (chosen)
* Type analysis in the transpiler with heuristics

## Decision Outcome

Chosen option: There is no `write(Object)`; `write` has overloads for primitives, `String`, `Raw` and `Content` only. The transpiler performs no type analysis and generates calls matching the context. If an overload is missing, the Java compilation fails.

Rationale: Type safety without additional transpiler complexity, and no hidden toString() calls.

### Options not chosen

* **Type analysis in the transpiler with heuristics** — Unnecessary; the Java compiler performs the check.
