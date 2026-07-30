# AFD Traceability Policy

## Responsibility

This document owns the linkage between requested work, specifications, scenario identifiers, implementation changes, tests, validation evidence, review findings, and completion reports. It does not define how validation runs or how findings are judged.

## Traceability Chain

Every behavioral change SHALL be traceable through this chain:

```text
User request -> active feature specification -> scenario identifier -> changed implementation -> test or manual evidence -> review result -> completion report
```

The chain MAY omit implementation or test artifacts only for documentation-only tasks, and the completion report SHALL state that limitation.

## Scenario Identifiers

Scenario identifiers from feature specifications are stable behavioral references. An agent SHALL preserve existing identifiers, SHALL NOT reuse a retired or deleted identifier, and SHALL not renumber identifiers for presentation. A new materially distinct behavior requires a new scenario identifier before implementation begins.

Tests SHOULD reference the scenarios they validate when practical. One test MAY support multiple scenarios only when the evidence is inseparable and the report names every supported scenario.

## Change Mapping

Before implementation, an agent SHALL list the active feature specifications and scenarios. During work, the agent SHALL maintain a concise mapping from each changed file to the scenario or policy reason that requires it. After work, the agent SHALL reconcile the mapping against the final diff.

When a changed file has no active scenario, the agent SHALL classify it as supporting infrastructure, validation evidence, documentation, or unrelated work. Unrelated work SHALL be removed from the task scope only with safe, authorized action; otherwise it SHALL be reported as user-owned.

## Review And Evidence Links

Review findings SHALL reference the affected scenario, rule, or explicitly state that a requirement is missing. Validation results SHALL identify the scenario, boundary, or check they support. A passing check SHALL not be linked to a scenario it does not actually exercise.

## Completion Trace

The completion report SHALL provide the active scenarios, changed files, and evidence summary needed for a later agent or reviewer to reconstruct why the change was made. It SHALL identify assumptions, blocked evidence, and unverified scenarios explicitly.

## Policy Boundary

Traceability records relationships between artifacts. It does not create requirements, define acceptance behavior, or decide whether a failed check may be retried.
