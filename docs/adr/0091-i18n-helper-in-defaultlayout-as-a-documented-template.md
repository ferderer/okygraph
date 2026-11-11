---
status: accepted
date: 2026-01-06
---
# 0091. i18n helper in DefaultLayout as a documented template

## Context and Problem Statement

A base view from a production app contained i18n with a message enum and ICU4J, which is too specific for the OkyTemplate base class.

## Considered Options

* i18n helper in DefaultLayout as a documented template (chosen)
* i18n() with a Message enum and ICU4J in OkyTemplate

## Decision Outcome

Chosen option: i18n does not belong in OkyTemplate; it is implemented in DefaultLayout (for example a helper t(key) based on ResourceBundle and LocaleContextHolder). This is documented and serves as a template that users can adopt themselves. Formatters (formatDate/Time/DateTime, format for numbers) remain in OkyTemplate.

Rationale: Keeps the base class lean and avoids an ICU4J dependency; users choose their own i18n solution.

### Options not chosen

* **i18n() with a Message enum and ICU4J in OkyTemplate** — Too specific, and it brings in a dependency.
