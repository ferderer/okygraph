---
status: accepted
date: 2025-12-16
---
# 0086. Framework integration via Maven plugin parameter instead of Spring Boot starter

## Context and Problem Statement

Template engines usually require a Spring Boot starter and ViewResolver configuration. Okygraph should work without runtime dependencies.

## Considered Options

* Framework integration via Maven plugin parameter instead of Spring Boot starter (chosen)
* Custom Spring Boot starter with ViewResolver configuration

## Decision Outcome

Chosen option: The Maven mojo has the mandatory parameter `okygraph.framework` (enum Framework: SPRING, QUARKUS, MICRONAUT, STANDALONE; only SPRING has a base class template so far). It generates the appropriate base class for the view classes; the Spring variant implements `org.springframework.web.servlet.View`. Controllers return the view instance directly, so Spring calls `View.render()` without a ViewResolver lookup. A Spring Boot starter is not needed.

Rationale: The framework integration is created at compile time; this keeps Okygraph at zero runtime dependencies and minimal configuration.

### Options not chosen

* **Custom Spring Boot starter with ViewResolver configuration** — Not necessary, since the Maven plugin already generates the integration.
