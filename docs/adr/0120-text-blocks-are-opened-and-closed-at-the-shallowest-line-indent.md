---
status: accepted
date: 2026-11-11
---
# 0120. Text blocks are opened and closed at the shallowest line indent

## Context and Problem Statement

`html("""` and `""");` were written at the indent of the first cached HTML line. When a later line was less indented (`</head>` after `<link>`), the closing delimiter sat deeper than that line, and javac warned "trailing white space will be removed" (`-Xlint:text-blocks`). Sci-Life templates produced 5 such warnings, and `examples/ferderer.de` 2. An alternative was the indent of the backtick line (`flushIndent`).

## Considered Options

* Opener and closer at the shallowest indent of the cached lines (chosen)
* `flushIndent`: indent of the backtick line
* Strip the indentation of every HTML line

## Decision Outcome

Chosen option: The opener and closer of a text block, and the single-line `html("...")`, are written at the shallowest indent among the cached lines. The content lines are unchanged.

Rationale: A closing delimiter that is never deeper than a content line cannot cause the lint warning, and Java strips the same amount as before, so the HTML output does not change. It needs no extra state.

`flushIndent` was not implemented: it is only set for backticks and `try`, so blocks flushed by an expression (`{body}`) would keep a stale value. It would also give blocks a relative indentation that single lines and fragments do not have. Stripping every line would destroy whitespace in `<pre>` and `<textarea>`.

Known limitation: HTML output indentation is best effort. A run of whole lines loses its common indentation, keeps the indentation between its own lines, and single lines and fragments are flush-left. Browsers ignore it.
