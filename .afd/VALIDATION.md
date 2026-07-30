# AFD Validation Policy

## Responsibility

This document owns validation planning, evidence execution, result classification, and reporting. It does not decide product acceptance criteria; those belong to feature specifications.

## Validation Plan

Before implementation, an agent SHALL select validation proportional to the active scenarios and affected boundaries. The plan SHALL identify deterministic checks, required repository commands, manual procedures, environment needs, and evidence gaps.

The agent SHALL use the strongest practical surface in this order: deterministic unit tests, Android instrumentation tests, single-device manual validation, two-device manual validation, then target-device validation. Higher-cost manual evidence does not replace a practical deterministic check.

## Execution Rules

All commands SHALL use the repository environment and checked-in tools defined by `AGENTS.md`. Android Gradle commands on Windows SHALL use the repository's required Windows wrapper form: `.\gradlew.bat`.

For implementation changes, the agent SHALL run every validation required by the active feature specifications and repository instructions. Documentation-only changes SHALL run structural checks and diff inspection unless a document changes executable build behavior.

Manual validation evidence SHALL state setup, action, expected result, actual result, required devices or environment, and limitations. An agent SHALL not represent a procedure as completed without performing it.

## Result Classification

- **Passed**: the command or procedure completed successfully.
- **Failed**: the command or procedure completed with an unmet expected result.
- **Not Run**: validation was not attempted.
- **Blocked**: an external condition prevented execution.
- **Partially Verified**: only a stated subset of the expected result was checked.

Each result SHALL name the evidence source and the scenarios or boundary it supports. Compilation alone SHALL NOT be presented as behavioral proof.

## Required Final Checks

Before completion, an agent SHALL run `git diff --check`, inspect `git diff`, and inspect the current worktree status. The report SHALL distinguish tracked diff evidence from untracked-file inspection when applicable.

## Failure Escalation

A failed required check SHALL be handed to [DEBUGGING.md](DEBUGGING.md). It SHALL not be hidden by reducing test scope, changing expectations without specification authority, or reporting partial success as completion.

## Policy Boundary

Validation proves implemented behavior. It does not define behavior, conduct independent review, or prescribe a debugging fix.
