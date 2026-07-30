# AFD Dry Run

This directory contains fictional records used to test deterministic AFD
orchestration. It contains no product behavior, live run state, Android
validation evidence, or executable automation.

The task and run-state samples conform to the current schemas. Review samples
are individual records conforming to `review.schema.json`. Checkpoint,
validation, and report files are evidence records; no current schema governs
their internal shape.

The planned scenario sequence is recorded in `sample-task-plan.json`. The
final synthetic state is in `sample-run-state.json`; its `extensions` field
retains transition evidence without extending the authoritative schema.

`simulation-results.md` distinguishes verified behavior from blocked coverage.
It must not be read as evidence of Android behavior or autonomous execution.
