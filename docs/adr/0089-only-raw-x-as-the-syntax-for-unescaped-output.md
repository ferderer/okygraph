---
status: accepted
date: 2026-01-06
---
# 0089. Only {raw(x)} as the syntax for unescaped output

## Context and Problem Statement

Earlier chats floated an additional syntax #{...} for raw output alongside raw().

## Considered Options

* Only {raw(x)} as the syntax for unescaped output (chosen)
* #{something} as the raw syntax

## Decision Outcome

Chosen option: Unescaped output is expressed exclusively as {raw(something)}, which is transpiled to write(raw(something)) using the wrapper record Raw(String value). The #{...} syntax will not be introduced.

Rationale: A single, self-explanatory syntax avoids ambiguity as in Thymeleaf; raw() is clearer, whereas #{} would have to be looked up.

### Options not chosen

* **#{something} as the raw syntax** — Multiple ways to do the same thing, as in Thymeleaf; not self-explanatory.
