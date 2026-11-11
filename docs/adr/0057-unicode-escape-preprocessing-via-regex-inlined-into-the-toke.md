---
status: accepted
date: 2025-10-21
---
# 0057. Unicode escape preprocessing via regex, inlined into the tokenizer

## Context and Problem Statement

The tokenizer broke without handling the JLS §3.3 Unicode escapes. A manual algorithm was longer and more error-prone.

## Considered Options

* Unicode escape preprocessing via regex, inlined into the tokenizer (chosen)
* Manual loop over charAt
* Separate UnicodePreprocessor class

## Decision Outcome

Chosen option: Unicode escapes are processed with the regex `\\u+([0-9a-fA-F]{4})` and `appendCodePoint`. The processing is integrated directly into the tokenizer. The separate UnicodePreprocessor class is removed, and the Mojo no longer calls it.

Rationale: The regex is clearer and shorter and supports multiple `u` characters as well as surrogate pairs. 15 LOC do not justify a separate class. Regex performance is sufficient.

### Options not chosen

* **Manual loop over charAt** — Longer and more prone to edge-case errors.
* **Separate UnicodePreprocessor class** — Unnecessary for 15 LOC in exactly one place.
