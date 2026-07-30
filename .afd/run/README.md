# Autonomous Execution Contract

This contract owns deterministic execution planning and enforcement. It does
not change product behavior. A task advances only through the transitions in
`execution.schema.json`; every other transition is rejected.

## States And Gates

Tasks: `planned -> ready -> implementing -> validating -> awaiting_review ->
checkpointing -> completed`. Repair enters `repairing -> revalidating ->
awaiting_review`. `blocked`, `failed`, `deferred`, and `cancelled` are terminal.
Validation, independent approved review, and a verified checkpoint are required
before completion. Runs use `initialized`, `running`, `paused`, `blocked`,
`failed`, `stopped`, `packaging`, and `completed`.

## Budgets

Defaults: implementation 3, repair 3, review 3, validation-command retry 2,
checkpoint 2, packaging 2, identical root cause 2, consecutive no-progress 2,
task attempts 100, reviews 100, duration 8 hours. An attempt is consumed before
the operation; equality with a limit rejects the next attempt. Cumulative
counters survive resume. Only progress resets consecutive no-progress counters.

## Progress And Failures

Progress is a verified state advance, validation recovery, blocking-review
resolution, dependency satisfaction, or verified checkpoint advance. Edits,
timestamps, repeated failures, and reports are not progress. Regression is loss
of verified validation/evidence/artifact/checkpoint. A terminal result has one
primary category: specification, policy, implementation, validation, review,
environment, repository, git, checkpoint, packaging, schema, budget,
no-progress, or unknown failure. Repeated root-cause fingerprints block at two.

## Recovery

Pause preserves revision and counters. Resume requires the expected revision.
Restore is human-assisted unless explicit non-destructive authority exists;
the harness returns a blocked result and preserves evidence. Checkpoints never
move backwards and require clean status, validation, and approved review.
