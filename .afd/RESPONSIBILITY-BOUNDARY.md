# AFD Core Responsibility Boundary

## AFD Core

The repository defines schemas, state machines, revision binding, evidence
requirements, budgets as persisted data, failure taxonomy, checkpoint
integrity, traceability, atomic record mutation, and deterministic rejection of
invalid or incomplete records. It validates records; it does not infer intent.

## Runtime Responsibility

The runtime selects and starts agents, executes commands, performs repairs,
resumes work, and supplies platform identities when available. A runtime action
is accepted only when it emits the corresponding schema-valid, revision-bound
record. A session identifier is evidence supplied by a runtime, not proof that
the repository created an independent session.

## Reclassified Requirements

Agent startup is a runtime action; the Core requires an execution-attempt
record. Root-cause inference is a runtime action; the Core requires a taxonomy
classification and evidence. Automatic restore is not Core authority; the Core
verifies checkpoint data and returns `HUMAN_ASSISTED_RESTORE_REQUIRED` where no
non-destructive restore authority is recorded.

## Canonical State-Machine Migration Status

`.afd/schemas/state-machines.json` is the canonical target definition and
`.afd/engineering-harness/afd_state_machines.py` validates and queries it.
`afd_run.py` and `.afd/run/` templates intentionally retain the legacy model
until their later bounded migration; current runtime records need not conform
yet. No competing canonical transition table may be introduced.
