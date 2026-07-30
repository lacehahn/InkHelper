# AGENTS.md

# Project Overview

This repository contains a personal Android research project for relaying
notifications between two Android devices over a local network.

The project produces one Android application that can operate in one of two
roles:

- Sender
- Receiver

The Sender runs on the primary Android phone, observes eligible Android
notifications, and transfers them over the local network.

The Receiver runs on a secondary Android phone, receives transferred
notifications, and presents them as a readable inbox.

The initial product is intentionally limited to a local-network MVP.

The project does not initially target:

- cloud delivery
- internet relay
- multi-user accounts
- enterprise deployment
- production-grade security
- broad device-management capabilities

Detailed product behavior belongs in repository specifications, not in this
file.

---

# Agent-First Development

This repository follows Agent-First Development, abbreviated as AFD.

Specifications are the source of truth.

Code implements specifications.

Tests and validation provide evidence that the implementation satisfies the
specifications.

Implementation must not silently define new product behavior.

For every behavioral change:

1. Locate the relevant specification.
2. Create or update the specification when behavior changes.
3. Identify the scenarios affected by the change.
4. Implement the smallest complete change satisfying those scenarios.
5. Validate the implementation.
6. Inspect the final diff.
7. Report the result and any remaining uncertainty.

Do not implement unspecified behavior merely because it appears useful.

---

# Repository Map

The expected repository structure is:

```text
app/
    Android application module

gradle/
    Gradle Wrapper support files

spec/
    Project and feature specifications

spec/features/
    Detailed behavioral specifications

.afd/
    Agent-First Development engineering policy

AGENTS.md
    Repository entry point for agents

build.gradle.kts
    Root Gradle build configuration

settings.gradle.kts
    Gradle project configuration

gradlew
gradlew.bat
    Gradle Wrapper entry points
```

This map must be updated when stable top-level repository areas are added,
removed, or renamed.

Do not list temporary files, generated output, or incidental implementation
details here.

---

# Specification Map

Project specifications live under:

```text
spec/
```

The planned project specifications are:

```text
spec/HARNESS.md
spec/ROADMAP.md
spec/ARCHITECTURE.md
spec/STYLE.md
spec/features/
```

Their responsibilities are:

## HARNESS.md

Defines:

- the product being built
- its purpose
- its roles
- its runtime overview
- its scope
- its non-goals

It answers:

> What system are we building?

## ROADMAP.md

Defines:

- implementation stages
- milestone order
- completion conditions
- deferred work

It answers:

> In what order should the system be built?

## ARCHITECTURE.md

Defines:

- stable system boundaries
- component ownership
- dependency direction
- architectural invariants

It answers:

> What structural rules must remain true?

## STYLE.md

Defines:

- implementation discipline
- naming and organization practices
- testing expectations
- validation expectations

It answers:

> How should repository work be implemented and validated?

## Feature Specifications

Feature specifications live under:

```text
spec/features/
```

They define:

- observable behavior
- accepted conditions
- rejected conditions
- scenarios
- failures
- recovery behavior
- state changes
- acceptance criteria
- validation requirements

They answer:

> Exactly how should one capability behave?

Feature specifications are the authoritative source for detailed feature
behavior.

---

# Instruction Priority

Follow instructions in this order:

1. Direct user instructions
2. Relevant feature specifications
3. `spec/HARNESS.md`
4. `spec/ARCHITECTURE.md`
5. `spec/STYLE.md`
6. `spec/ROADMAP.md`
7. This `AGENTS.md`

Higher-priority instructions may narrow lower-priority instructions.

They must not silently invalidate repository safety or overwrite unrelated user
work.

If applicable specifications conflict:

1. Stop before implementing the conflicting behavior.
2. Identify the conflicting statements.
3. Explain the implementation impact.
4. Request clarification.

Do not guess.

Do not silently select the most convenient interpretation.

---

# Required Reading

Before making a change:

1. Read this `AGENTS.md`.
2. Read the relevant feature specification.
3. Read the applicable project specifications.
4. Inspect the existing implementation and tests.
5. Inspect the current Git worktree state.

Do not assume every task requires every document.

Read the documents relevant to the affected scope, but always read the
feature specification when one exists.

For specification-system changes, read all project specifications affected by
the change.

---

# Specification-First Workflow

Use the following workflow for behavioral work:

```text
Request
    ↓
Applicable specification
    ↓
Affected scenarios
    ↓
Implementation plan
    ↓
Smallest complete implementation
    ↓
Validation evidence
    ↓
Final diff review
    ↓
Completion report
```

Before implementation:

1. Determine whether the requested behavior is already specified.
2. Identify the relevant feature and scenario identifiers.
3. Update or create the specification if the requested behavior is missing or
   changed.
4. Inspect existing code and tests.
5. explain the planned change.

During implementation:

- implement only the active specification
- preserve unrelated behavior
- avoid speculative generalization
- keep changes reviewable
- prefer the smallest complete solution

After implementation:

1. Run applicable validation.
2. Inspect all changed files.
3. Check for accidental unrelated changes.
4. Compare the result with the active specification.
5. Produce a completion report.

---

# Scenario Traceability

Observable behavior should be described through stable scenario identifiers.

Use identifiers in this form:

```text
F-0001-S01
F-0001-S02
F-0002-S01
```

Where:

- `F-0001` identifies the feature
- `S01` identifies one scenario within that feature

Scenario identifiers must remain stable after implementation or tests reference
them.

Tests should reference the scenario identifiers they validate whenever
practical.

Example:

```kotlin
// F-0001-S01
@Test
fun selectingSenderActivatesSenderRole() {
    // ...
}
```

Do not reuse one scenario identifier for unrelated behavior.

Do not renumber existing scenarios merely to improve presentation.

---

# Scope Rules

Implement only what is required by the current task and its active
specifications.

Do not perform the following unless explicitly required:

- unrelated refactoring
- dependency upgrades
- package renaming
- build-version changes
- application identity changes
- speculative framework creation
- future roadmap implementation
- unrelated formatting across the repository

Do not add abstractions for hypothetical future requirements.

A useful idea is not automatically an approved requirement.

---

# Architecture Rules

Architecture is defined primarily by:

```text
spec/HARNESS.md
spec/ARCHITECTURE.md
```

Do not invent architecture merely because a pattern is common in other Android
projects.

Introduce an abstraction only when it solves a current specification or an
existing architectural constraint.

Potential abstractions include:

- repositories
- use cases
- service layers
- dependency injection
- navigation frameworks
- protocol abstractions
- plugin systems

These are not universally forbidden.

They require a concrete present need.

Prefer direct, understandable implementations until the specifications justify
additional structure.

---

# Repository Environment

The primary development environment is:

- Windows 11
- PowerShell
- JDK 21
- Android Studio
- Gradle Wrapper

Always use the repository Gradle Wrapper.

On Windows, use:

```powershell
.\gradlew.bat
```

Do not assume:

- Bash
- WSL
- a globally installed Gradle version
- Unix-only shell commands

Commands included in plans and completion reports should be valid for the
repository environment.

---

# Validation

Documentation-only changes normally do not require an Android build.

For implementation changes, run validation appropriate to the affected scope.

Typical validation commands include:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

Additional validation may be required by a feature specification.

Validation should demonstrate more than compilation when behavior is being
changed.

Use the strongest practical validation surface:

1. deterministic unit tests
2. Android instrumentation tests
3. single-device manual validation
4. two-device manual validation
5. target e-ink device validation

Do not claim that a command passed unless it was actually executed.

If validation could not be performed:

- state that clearly
- explain why
- identify what remains unverified

---

# Git Rules

Before implementation, inspect the worktree:

```powershell
git status --short
```

Before completion, inspect the diff:

```powershell
git diff --check
git diff
```

Never:

- discard unrelated user changes
- reset the worktree
- rewrite history
- force-push
- remove untracked files
- execute destructive Git commands

unless explicitly requested.

Do not create commits unless explicitly requested.

Treat pre-existing worktree changes as user-owned work.

---

# Completion Report

Every completed task must report:

1. Purpose
2. Specifications affected
3. Scenario identifiers affected
4. Files changed
5. Behavioral result
6. Architectural impact
7. Commands executed
8. Validation results
9. Remaining risks or unverified behavior
10. Recommended next specification step

For documentation-only work, explicitly state that build validation was not
required.

Do not report completion while known specification conflicts remain unresolved.
