---
status: accepted
date: 2026-11-12
---
# 0125. try blocks buffer into a try-with-resources object; the root writer stays the response writer

## Context and Problem Statement

ADR-0087 made the response writer the root of a writer stack and let the transpiler emit three calls around a try block: `pushBuffer()` after `try {`, `commitBuffer()` before `} catch` and `discardBuffer()` at the start of the catch block. The base class kept the bookkeeping for it (the previous writer, linked buffers), the transpiler emitted at three places, and an exception that the template's catch did not handle left the buffer on the stack: the writer was never restored.

## Considered Options

* A buffer object that is the resource of a try-with-resources (chosen)
* Push, commit and discard calls with bookkeeping in the base class (ADR-0087) (rejected)

## Decision Outcome

Chosen option: the transpiler turns `try {` into `try (var oky1 = this.buffer()) {`, writes `oky1.commit();` before `} catch`, and emits nothing at the start of the catch block. `buffer()` returns a `Buffer` that is a `StringWriter`: it remembers the writer in place, makes itself the output, and `close()` puts the previous writer back. `commit()` writes the content to the remembered writer. The buffer is local to the block, so on an exception it is simply dropped and nothing has to be discarded; `close()` runs on every exit, so an exception that reaches an outer catch, or the caller, leaves the right writer in place.

The number in the name is the nesting depth of open try blocks: nested blocks get `oky1`, `oky2`, sequential blocks reuse a name. A `catch` takes the innermost open try, so there is one catch clause per try (`catch (A | B e)` for several types), and a `catch` without a `try` in the template is an error. `finally` is not supported, as before. A variable named `okyN` in the template would collide.

The root writer is still the response writer and nothing wraps it (ADR-0087): code outside try blocks writes straight to it, and a template without try blocks costs nothing for this feature.

Rationale: less code on both sides (the base classes lose the bookkeeping, 117 → 108 lines in `SPRING` and 102 → 98 in `STANDALONE`; the transpiler emits at two places instead of three) and the writer is restored on every path. It is not faster: the old and the new design allocate the same objects, and the 89 try blocks of the food page cost about 0.4 µs and 750 B each in both.

### Options not chosen

* **Push, commit and discard calls** — More code in the base class and the transpiler, and the writer stays swapped when an exception is not caught by the template's own catch.
