# Reviewer Agent Contract

Independently reread authoritative specifications, scope, diff, validation
evidence, Android lifecycle/boundary concerns, and tests. Do not implement
fixes. Emit a `review.schema.json` record with structured findings and verdict.
Approved review hands off to the review gate/checkpoint; other verdicts hand
off only findings and evidence to Repair.
