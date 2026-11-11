---
status: accepted
date: 2026-06-28
---
# 0108. VS Code plugin phase 1: pure grammar extension, LSP later

## Context and Problem Statement

Editor support for .oky files is missing. An LSP would require the transpiler logic in Node/TS or as an external Java process.

## Considered Options

* VS Code plugin phase 1: pure grammar extension, LSP later (chosen)
* LSP with diagnostics immediately

## Decision Outcome

Chosen option: Phase 1 of the VS Code plugin consists of a single extension project without a build tool, with four files: package.json, language-configuration.json, syntaxes/oky.tmLanguage.json and snippets/oky.code-snippets. An LSP for real diagnostics is postponed to phase 2 as a separate project after the public release.

Rationale: Grammar-only extensions do not need a build step. Highlighting does not need a language server.

### Options not chosen

* **LSP with diagnostics immediately** — Considerable effort and premature.
