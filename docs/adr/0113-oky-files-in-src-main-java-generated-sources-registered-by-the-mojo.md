---
status: accepted
date: 2026-11-08
---
# 0113. .oky files in src/main/java; generated sources in target/generated-sources, registered by the mojo

## Context and Problem Statement

Earlier plans put generated code into a subdirectory (`target/generated-sources/okygraph` or `.../templates`). The implemented mojo uses neither. This ADR records the approach the code actually takes.

## Decision Outcome

Chosen option (as implemented): `.oky` files live next to the `.java` files in `src/main/java` (`sourceDirectory`, default `${project.basedir}/src/main/java`). The mojo writes one `.java` file per `.oky` file to `outputDirectory` (default `${project.build.directory}/generated-sources`), keeping the relative package path, and registers that directory as a compile source root (`project.addCompileSourceRoot`). The goal `transpile` runs in `generate-sources`.

Rationale: Maven conventions without extra configuration; the mojo itself makes the generated sources visible to the compiler. IDEs may still need build-helper-maven-plugin.
