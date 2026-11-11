---
status: accepted
date: 2025-10-21
---
# 0059. Tokenizer as a context-based state machine without a stack

## Context and Problem Statement

The first tokenizer used a mode stack and ModeAction in the token specs. However, the context transitions are few and clearly defined.

## Considered Options

* Tokenizer as a context-based state machine without a stack (chosen)
* Mode stack with ModeAction in the specs
* Dedicated token types for IF/ELSE/FOR/WHILE

## Decision Outcome

Chosen option: The tokenizer uses a simple switch-based state machine model without a parsing-context stack. The transitions are in a nested switch over (context, tokentype). The token patterns are `TokenPattern` constants (ADR-0072) that each `ParsingContext` lists. Tokenizing is done line by line via regex. The token types are reduced to a few (JAVA_TOKEN, HTML, BACKTICK, EXPRESSION_START/END, TEXT/COMMENT markers, TRY, CATCH, NEWLINE). Control-flow keywords and generic brackets are not token types of their own.

Rationale: There is no deep nesting. The stack and ModeAction were superfluous complexity (about 50 lines saved). The transpiler only needs tokens that are semantically relevant.

### Options not chosen

* **Mode stack with ModeAction in the specs** — Transitions are flat and deterministic; a stack is overkill.
* **Dedicated token types for IF/ELSE/FOR/WHILE** — Irrelevant to the transpiler.
