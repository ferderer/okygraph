---
status: accepted
date: 2026-07-01
---
# 0112. Always transpile all .oky files completely

## Context and Problem Statement

Incremental transpilation based on timestamp comparison led to inconsistencies; a full run takes only about 9 ms.

## Considered Options

* Always transpile all .oky files completely (chosen)
* Incremental transpilation via last-modified comparison

## Decision Outcome

Chosen option: `OkygraphMojo.execute()` transpiles all discovered .oky files on every run without timestamp comparison. The log output reads `Transpiled N .oky files in M ms`; without files, `No .oky files found`.

Rationale: The transpiler is fast enough; incrementality without dependency tracking is a source of errors with no meaningful benefit.

### Options not chosen

* **Incremental transpilation via last-modified comparison** — Leads to inconsistencies, no dependency tracking, minimal time savings.
