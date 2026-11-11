---
status: accepted
date: 2026-01-06
---
# 0090. MVP: HTML escaping only, move JS/CSS out

## Context and Problem Statement

Dedicated wrappers or context-sensitive escaping via HTML tokenization had been discussed for css/js/attr/url.

## Considered Options

* MVP: HTML escaping only, move JS/CSS out (chosen)
* Explicit wrappers attr/js/css/url for the template author
* Context-sensitive escaping via HTML tokenization

## Decision Outcome

Chosen option: The MVP has default HTML escaping plus raw() for exceptions. Dedicated escaping wrappers for JS/CSS/attribute/URL and context-sensitive escaping via HTML tokenization are not part of the MVP; JS and CSS are moved into external files.

Rationale: HTML escaping covers most cases; JS and CSS can always be moved out.

### Options not chosen

* **Explicit wrappers attr/js/css/url for the template author** — Not necessary for the MVP.
* **Context-sensitive escaping via HTML tokenization** — Postponed to later; not part of the MVP.
