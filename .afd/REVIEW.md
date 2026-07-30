# AFD Review Policy

## Responsibility

This document owns independent engineering review. It does not define task scope, validation execution, traceability storage, or debugging retries.

## Review Trigger

An implementation change SHALL receive review before completion when it changes observable behavior, a shared boundary, a feature scenario, failure handling, or validation evidence. Documentation-only changes MAY receive a focused consistency review.

The reviewer SHALL be independent of the implementation pass when a separate agent or clean review context is available. If independence is unavailable, the agent SHALL state that limitation and perform a deliberate second-pass review after implementation and validation.

## Review Inputs

The reviewer SHALL inspect the active specifications, affected scenario identifiers, changed files, current diff, validation evidence, and relevant existing tests. The reviewer SHALL treat the specifications as the source of truth and SHALL NOT infer new product behavior from code.

## Review Procedure

1. Confirm that each changed behavior has an active specification and scenario.
2. Compare the implementation and tests with the specified success, rejection, failure, recovery, and state-preservation behavior.
3. Check scope, ownership, dependency direction, error handling, and unintended behavior against repository constraints.
4. Check that validation evidence actually supports the claimed scenarios.
5. Inspect the diff for unrelated edits, generated output, secrets, weakened checks, and identifier changes.
6. Report findings before summaries, ordered by severity.

## Finding Severity

- **Blocker**: prevents coherent, safe, or specified implementation; completion is prohibited.
- **Major**: creates a likely behavioral defect, invariant violation, missing required evidence, or material regression risk.
- **Minor**: reduces clarity, traceability, maintainability, or confidence without changing the primary behavior.
- **Observation**: non-blocking context, assumption, or future consideration.

Each finding SHALL include the affected file and location, relevant scenario or rule, observable impact, and required correction or decision. A review with no findings SHALL say so and identify remaining evidence gaps.

## Review Outcome

Blocker findings SHALL return the task to [HARNESS.md](HARNESS.md) stop handling. Major findings SHALL be corrected and re-reviewed before completion unless the user explicitly accepts the risk. Minor findings and observations SHALL be reported without being silently hidden.

## Policy Boundary

Review evaluates conformance; it does not author product behavior or substitute for validation execution.
