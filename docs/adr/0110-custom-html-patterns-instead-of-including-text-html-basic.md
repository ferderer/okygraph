---
status: accepted
date: 2026-06-28
---
# 0110. Custom HTML patterns instead of including text.html.basic

## Context and Problem Statement

HTML in template regions was not highlighted in .oky files because `include: text.html.basic` inside a begin/end pattern does not take effect.

## Considered Options

* Custom HTML patterns instead of including text.html.basic (chosen)
* include text.html.basic in the template region

## Decision Outcome

Chosen option: Custom HTML patterns are defined in the repository of oky.tmLanguage.json: tags (opening/closing with captures), attributes, attribute values in double and single quotes, entities and comments. The attribute value patterns include `#expression` themselves, so that `src="{user.getAvatar()}"` scopes the expression as Java.

Rationale: VS Code only reliably loads external grammars at root level, not inside begin/end regions; the include silently fell back to default coloring.

### Options not chosen

* **include text.html.basic in the template region** — Ignored inside regions, no HTML highlighting.
