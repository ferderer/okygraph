---
status: accepted
date: 2026-07-01
---
# 0111. CRLF line endings are normalized in the transpiler

## Context and Problem Statement

On Windows, CRLF files caused 'Unexpected character' errors at the end of every .oky line.

## Considered Options

* CRLF line endings are normalized in the transpiler (chosen)

## Decision Outcome

Chosen option: `Transpiler.process()` normalizes input before anything else with `source.replace("\r\n", "\n").replace("\r", "\n")` (helper `normalizeLineEndings`), followed by `preprocessUnicode`.

Rationale: \r is not matched by any JAVA-context pattern; the normalization costs practically nothing.
