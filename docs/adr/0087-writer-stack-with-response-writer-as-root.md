---
status: accepted
date: 2026-01-06
---
# 0087. Writer stack with response writer as root

## Context and Problem Statement

OkyTemplate needs writer infrastructure for exception handling (discard the buffer on errors). The first draft used a StringWriter as the root.

## Considered Options

* Writer stack with response writer as root (chosen)
* Root StringWriter and write everything to the response at the end

## Decision Outcome

Chosen option: The bottom writer in the writer stack is the writer of the HttpServletResponse (res.getWriter()), not a StringWriter. pushBuffer() places a new StringWriter on top of the stack, commitBuffer() writes its contents to the writer beneath it, and discardBuffer() discards it. Output is written through a PrintWriter on whichever writer is currently on top.

Rationale: The root level must write directly to the response; an additional intermediate buffer is unnecessary there.

### Options not chosen

* **Root StringWriter and write everything to the response at the end** — Double buffering with no benefit at the root level.
