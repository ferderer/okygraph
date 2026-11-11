---
status: accepted
date: 2025-10-25
---
# 0072. TokenPattern enum with match() method based on regions and lookingAt()

## Context and Problem Statement

The transpiler created a substring for each line and used scattered TokenSpec records. This was redundant and hard to test.

## Considered Options

* TokenPattern enum with match() method based on regions and lookingAt() (chosen)
* `matcher.find() && start()==0` with a `^` prefix in the pattern
* Create a substring per position

## Decision Outcome

Chosen option: All patterns live in the enum `TokenPattern` (its own file) with the public final fields `pattern` and `type`. The patterns have no `^` prefix. The method `String match(String line, int position)` uses `pattern.matcher(line).region(position, line.length())` and `lookingAt()`. It returns the matched string or `null`. `matchToken` in the transpiler iterates over `context.patterns`, calls `emit(...)` and `updateContext(type)`, and returns `position + value.length()`.

Rationale: The substring per token is no longer needed, the matcher is no longer visible in the transpiler, and the code gets shorter.

### Options not chosen

* **`matcher.find() && start()==0` with a `^` prefix in the pattern** — `^` does not cleanly refer to the region start; `lookingAt()` is simpler.
* **Create a substring per position** — Unnecessary copies.
