---
status: accepted
date: 2026-01-06
---
# 0092. Layout as a method with Content lambdas instead of overriding render()

## Context and Problem Statement

It needed to be clarified how a concrete view uses a layout and passes content (body, meta tags).

## Considered Options

* Layout as a method with Content lambdas instead of overriding render() (chosen)
* DefaultLayout implements render() and defines an abstract content() method

## Decision Outcome

Chosen option: DefaultLayout (abstract, extends OkyTemplate) defines only layout methods such as layout(String title, Content meta, Content body), not render(). The concrete view implements render() and calls layout(...) with lambdas that supply the content blocks. Multiple Content parameters are possible, for example for meta tags. Views can thereby switch layouts or omit one.

Rationale: More flexible than inheritance with an abstract content() method; multiple layout methods and parameters per call are possible.

### Options not chosen

* **DefaultLayout implements render() and defines an abstract content() method** — Less flexible; the view cannot freely choose the layout.
