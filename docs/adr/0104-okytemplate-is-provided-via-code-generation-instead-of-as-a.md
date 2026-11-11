---
status: accepted
date: 2026-06-18
---
# 0104. OkyTemplate is provided via code generation instead of as a runtime dependency

## Context and Problem Statement

Okygraph advertises zero runtime dependencies to ease adoption in large enterprises; a JAR dependency would be the alternative. The Quarkus, Micronaut and standalone variants differ only slightly.

## Considered Options

* OkyTemplate is provided via code generation instead of as a runtime dependency (chosen)
* Runtime JAR okygraph-runtime as a compile dependency

## Decision Outcome

Chosen option: The base class OkyTemplate is copied/generated at build time by the Maven plugin into the user's configured package (`generateBaseClass()`), instead of being delivered as a compile dependency. Using it as a direct dependency remains at most a documented alternative. Since the differences between frameworks are small, generation is the simplest path. The file is written once into `baseClassOutputDirectory` (default `src/main/java`) and never overwritten, so it can be adapted; delete it to regenerate.

Rationale: No foreign JAR on the classpath, nothing for dependency scanners to flag, easy to audit; a shared JAR would anyway need multiple artifacts because the Spring variant is structurally different.

### Options not chosen

* **Runtime JAR okygraph-runtime as a compile dependency** — Contradicts the zero-dependency goal for enterprise adoption.
