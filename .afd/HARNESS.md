# AFD Harness Policy

## Responsibility

This document owns the agent task lifecycle, required reading, scope control, modification authority, stop conditions, and completion reporting. It does not define review methods, validation evidence, traceability records, or debugging procedures; those belong to the other `.afd/` documents.

## Terms

- **Task**: a bounded user-requested unit of work.
- **Active specification**: the repository document that authoritatively defines the behavior affected by a task.
- **Scenario**: a stable, observable behavior identified by a feature specification.
- **Evidence**: an executed command, test, inspection, or manual procedure with an actual result.
- **Agent**: an AI contributor performing a task within granted authority.
- **Uncertainty**: a missing, conflicting, inaccessible, or unverified fact that materially affects the task.

## Required Reading

Before modifying a repository, an agent SHALL read `AGENTS.md`, inspect `git status --short`, read every applicable project specification, and read every applicable feature specification. The agent SHALL inspect the affected implementation and tests before changing behavior.

For a specification-system task, the agent SHALL read every affected project specification and every affected feature specification. The agent SHALL identify the active scenario identifiers before changing implementation behavior.

## Task Lifecycle

1. Classify the task as specification, implementation, review, debugging, validation, or documentation work.
2. Identify the active specifications, affected scenarios, constraints, and user-owned worktree changes.
3. State the smallest complete planned change, expected files, and validation plan before substantial implementation.
4. Make only the authorized changes.
5. Collect evidence through the workflow in [VALIDATION.md](VALIDATION.md).
6. Obtain an independent assessment through [REVIEW.md](REVIEW.md) when implementation behavior, shared boundaries, or validation results warrant it.
7. Inspect the final diff and report completion using the requirements below.

## Scope And Modification Authority

An agent SHALL modify only files required by the task and active specifications. A specification update is required before an implementation changes observable behavior, unless the user explicitly authorizes a correction to an existing specification.

An agent SHALL preserve unrelated changes, existing identifiers, generated outputs, project identity, and build configuration unless the task explicitly authorizes a change. This policy does not grant authority to broaden product scope, add future-roadmap work, introduce a new architecture, change dependencies, or create commits.

An agent SHALL NOT use destructive version-control operations, overwrite user-owned work, edit generated or build output, weaken tests or validation solely to obtain success, or claim an unexecuted result. Repository-specific prohibitions in `AGENTS.md` remain controlling.

## Stop Conditions

An agent SHALL stop before modification when any of the following is true:

- applicable specifications conflict or omit a decision required for coherent behavior;
- the requested change would violate a product, architecture, safety, or repository constraint;
- the target file or required evidence cannot be safely inspected;
- the only apparent solution requires unauthorized destructive action, scope expansion, or external authority;
- required validation remains failed after the bounded debugging policy is exhausted.

When stopped, the agent SHALL identify the blocking statements or evidence, explain the implementation impact, preserve the worktree, and request the smallest decision or authority needed to continue.

## Completion Reporting

A completion report SHALL state:

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

For documentation-only work, the report SHALL explicitly state that build validation was not required. Completion is not permitted while a known conflict or required failed validation remains unresolved.

## Policy Boundary

This policy is process-only. It does not interpret or extend product requirements, feature behavior, roadmap order, or architecture.
