# AFD Debugging Policy

## Responsibility

This document owns failure diagnosis, bounded retry, recovery of the engineering task, and escalation. It does not define validation evidence requirements or independent review criteria.

## Failure Intake

When a command, test, manual procedure, review, or runtime observation fails, the agent SHALL record the first actionable failure, affected scenario or boundary, actual result, expected result, environment, and relevant log location. The agent SHALL preserve the original evidence and SHALL not conceal it in a large undifferentiated log.

## Failure Taxonomy

Every terminal failure has exactly one primary category: `SPECIFICATION_AMBIGUITY`,
`ENVIRONMENT_UNAVAILABLE`, `TOOL_FAILURE`, `SCHEMA_INVALID`,
`ILLEGAL_TRANSITION`, `STALE_REVISION`, `VALIDATION_FAILURE`, `REVIEW_FAILURE`,
`CHECKPOINT_FAILURE`, `BUDGET_EXHAUSTED`, `NO_PROGRESS`, `PACKAGING_FAILURE`,
`UNAUTHORISED_MUTATION`, `INTERNAL_HARNESS_ERROR`, or
`HUMAN_DECISION_REQUIRED`. Secondary tags are diagnostic only. A record SHALL
include a stable code, blocking and retryable flags, evidence, affected task or
run, and next required authority.

## Diagnosis Workflow

1. Reproduce the failure using the narrowest practical command or procedure.
2. Determine whether the cause is specification ambiguity, implementation behavior, test expectation, environment, dependency, or external system state.
3. Inspect only the files and evidence relevant to the suspected cause.
4. Apply the smallest authorized correction.
5. Re-run the failed evidence and then the required validation surface from [VALIDATION.md](VALIDATION.md).
6. Record the diagnosis, correction, and result in the completion report.

An agent SHALL update a specification before changing implementation when diagnosis reveals a missing or incorrect behavioral requirement. If the correct behavior is uncertain, the agent SHALL stop rather than choosing one.

## Retry Policy

An agent MAY repeat a failed operation once without modification to confirm reproducibility. Further retries require new evidence: a code or configuration change, a changed external condition, or a narrower diagnostic hypothesis.

An agent SHALL stop retrying after two consecutive attempts produce the same actionable failure without new evidence. It SHALL not retry an operation indefinitely, bypass a failure by disabling checks, or mutate unrelated files to change the result.

## Recoverability

Before a risky correction, an agent SHALL inspect the exact target and current diff. Corrections SHALL be narrow and reversible through normal version-control history; destructive commands require explicit user authority. Existing user work SHALL remain intact throughout debugging.

## Escalation

An agent SHALL return control to [HARNESS.md](HARNESS.md) stop handling when the root cause is a specification conflict, missing product decision, unavailable external dependency, required credential or device, repeated unexplained failure, or unauthorized action. The escalation SHALL state what was tried, what changed between attempts, the current evidence, and the decision needed.

## Policy Boundary

Debugging explains and bounds recovery from failure. It does not redefine product behavior, weaken validation, or declare a task complete.
