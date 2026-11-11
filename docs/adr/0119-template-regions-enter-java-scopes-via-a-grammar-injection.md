---
status: accepted
date: 2026-11-11
---
# 0119. Template regions enter Java scopes via a grammar injection

## Context and Problem Statement

The grammar listed the template region (backtick to backtick) as a top-level pattern next to `source.java`. In a TextMate grammar, rules nested inside an included grammar, such as class and method bodies, do not see the including grammar's patterns. In real files (class, method, template) the backtick was therefore never recognized, and the whole file was highlighted as plain Java. This was not noticed because the grammar rules looked correct and only top-level backticks worked.

## Decision Outcome

Chosen option: The template region is no longer a top-level pattern. It is injected into every `source.oky` scope, except inside the template itself, inside `{...}` expressions, strings and comments, through the grammar's own `injections` entry (`L:source.oky - meta.embedded.html.oky - meta.embedded.java.oky - string - comment`). The extension stays four declarative files without a build step (ADR-0108).

Rationale: An injection reaches every Java rule without copying or patching the Java grammar. Excluding strings and comments keeps backticks in text blocks, char literals and comments plain Java.
