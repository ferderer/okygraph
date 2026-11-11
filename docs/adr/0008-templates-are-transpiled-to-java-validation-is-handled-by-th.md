---
status: accepted
date: 2025-06-21
---
# 0008. Templates are transpiled to Java; validation is handled by the build

## Context and Problem Statement

It was open whether the parser should validate Java expressions itself or only recognize structure.

## Considered Options

* Templates are transpiled to Java; validation is handled by the build (chosen)

## Decision Outcome

Chosen option: The plugin transpiles templates only into Java code, which is then compiled by Maven. Semantic validation of Java expressions does not belong to the template library. The transpiler only deals with structure: HTML, Java and expressions.

Rationale: The Java compiler checks types and expressions anyway; this keeps the parser simple.
