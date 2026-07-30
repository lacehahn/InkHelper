# AFD Engineering Harness Interfaces

## Purpose

This document defines future command-line interfaces that automate existing AFD policy. It is an interface contract only: no script is implemented in this phase, and no command defines product behavior.

## Shared Contract

All future commands use PowerShell-compatible invocation from the repository root:

```powershell
python .\.afd\engineering-harness\<script>.py [options]
```

Every command accepts repository-relative paths only. Structured output is written only to the explicit `--output` path. Standard output emits one concise JSON summary; standard error emits diagnostics and actionable failures only. Commands do not emit private reasoning.

Unless a command explicitly supports an authorized state or checkpoint write, it is read-only with respect to source, specifications, Git state, and task/run records. Re-running a command with identical inputs SHALL produce the same gate decision and equivalent structured result.

## Global Exit Codes

| Code | Meaning |
|---:|---|
| 0 | Success; the requested gate passed or operation completed. |
| 10 | Validation failure; supplied evidence or checked behavior failed. |
| 11 | Invalid input or schema failure. |
| 12 | Policy violation. |
| 13 | Specification conflict. |
| 14 | Environment or tooling failure. |
| 15 | Internal harness error. |

No command assigns a different meaning to these codes. A nonzero result SHALL include a machine-readable failure category in its output artifact.

## State Models

`task.schema.json` enumerates task states: `planned`, `ready`, `in_progress`, `validating`, `awaiting_review`, `blocked`, `failed`, `completed`, and `cancelled`.

Normal task progression is `planned -> ready -> in_progress -> validating -> awaiting_review -> completed`. A task MAY move to `blocked`, `failed`, or `cancelled` from any nonterminal state. A blocked or failed task returns to `ready` only after the documented stop reason is resolved and the retry policy permits continuation. A completed or cancelled task is terminal.

`run-state.schema.json` enumerates run states: `planned`, `running`, `paused`, `blocked`, `failed`, `completed`, and `cancelled`. A resumable run retains its task queue, current task, checkpoint reference, histories, and last update time. `completed` requires `successful_completion`; blocked, failed, and cancelled runs require a non-`none` stop reason.

## Interfaces

### `afd_check.py`

**Responsibility:** aggregate deterministic AFD checks into one gate result. Authoritative policy: `.afd/HARNESS.md`, `.afd/VALIDATION.md`, and `.afd/TRACEABILITY.md`.

```powershell
python .\.afd\engineering-harness\afd_check.py --task <task.json> --run-state <run.json> --output <report.json> [--checks <name,...>]
```

Required inputs are a task record, run state, and requested deterministic check names. It generates one aggregate report containing constituent results and the overall exit category. It reads task and run state without changing them. It is idempotent for unchanged inputs. A failed constituent check returns `10`; malformed records return `11`; unresolved specification conflict returns `13`.

### `afd_spec_check.py`

**Responsibility:** validate feature-specification structure, stable identifiers, and deterministic cross-file invariants without judging subjective product quality. Authoritative policy: `AGENTS.md`, `spec/STYLE.md`, `spec/features/README.md`, and `.afd/TRACEABILITY.md`.

```powershell
python .\.afd\engineering-harness\afd_spec_check.py --spec-root spec --output <report.json> [--task <task.json>]
```

It requires the specification root and output path; task input is optional for scoped checks. It produces structural findings and identifier inventory. It does not modify repository files or state and is idempotent. Invalid structure returns `11`; deterministic policy violations return `12`; conflicting specifications return `13`.

### `afd_traceability.py`

**Responsibility:** validate references among feature scenarios, tests, task records, review records, and validation evidence. It shall not infer behavioral coverage from identifier presence alone. Authoritative policy: `.afd/TRACEABILITY.md`, `AGENTS.md`, and applicable feature specifications.

```powershell
python .\.afd\engineering-harness\afd_traceability.py --task <task.json> --review <review.json> --evidence <path,...> --output <report.json>
```

It requires a task record, review record, evidence references, and output path. It produces a traceability matrix with verified, missing, and unverified links. It is read-only and idempotent. Missing required links return `10`; malformed records return `11`; conflicting scenario ownership returns `13`.

### `afd_validation.py`

**Responsibility:** execute only approved repository validation commands and record structured results. Authoritative policy: `.afd/VALIDATION.md`, `AGENTS.md`, and applicable feature specifications.

```powershell
python .\.afd\engineering-harness\afd_validation.py --task <task.json> --run-state <run.json> --output <report.json> [--validation <reference,...>] [--write-run-state]
```

It requires a task, run state, and output path. It runs only validation references listed in the task and supported by the repository; it never invents commands. It produces per-command status, command text, concise observations, and artifact references. It may write run state only with `--write-run-state` and explicit task authorization; it never edits product or source files. Repeated execution may rerun commands but must not duplicate history entries for the same evidence fingerprint. Failed checks return `10`; unavailable tooling returns `14`.

### `afd_review_gate.py`

**Responsibility:** validate a review record and decide whether review policy permits continuation. It does not perform semantic review. Authoritative policy: `.afd/REVIEW.md` and `review.schema.json`.

```powershell
python .\.afd\engineering-harness\afd_review_gate.py --review <review.json> --output <report.json> [--task <task.json>] [--write-run-state]
```

It requires a review record and output path. It produces a gate decision: `continue` only for an approved record; otherwise `stop`. It is read-only unless authorized to update run state. It is idempotent for unchanged inputs. Invalid records return `11`; `changes_required` returns `10`; a blocked verdict returns `13`.

### `afd_checkpoint.py`

**Responsibility:** create, inspect, or restore safe Git checkpoints according to AFD authority. Authoritative policy: `.afd/HARNESS.md`, `.afd/DEBUGGING.md`, and `AGENTS.md`.

```powershell
python .\.afd\engineering-harness\afd_checkpoint.py <inspect|create|restore> --run-state <run.json> --output <report.json> [--checkpoint <commit>] [--write-run-state] [--permit-checkpoint]
```

It requires an operation, run state, and output path. `inspect` is read-only. `create` requires explicit checkpoint permission because it may create a commit; `restore` requires explicit permitted authority and SHALL never discard uncommitted changes. It emits the observed or created checkpoint reference. Repeated inspect is idempotent; create is idempotent only when the requested checkpoint already represents the same tree. Unauthorized mutation returns `12`; Git/tooling failure returns `14`.

### `afd_package.py`

**Responsibility:** verify final build artifacts and create a structured packaging report without claiming device validation absent evidence. Authoritative policy: `.afd/VALIDATION.md`, `AGENTS.md`, and applicable feature specifications.

```powershell
python .\.afd\engineering-harness\afd_package.py --task <task.json> --artifact <relative-path> --evidence <path,...> --output <report.json>
```

It requires a task, candidate artifact, evidence references, and output path. It verifies that the artifact exists and that required build evidence is present; it reports device validation only when evidence records prove it. It does not modify source or product specifications and is idempotent for unchanged inputs. Missing or invalid artifact returns `10`; unavailable tooling returns `14`.

## Task And Run Interaction

Task records declare scope, reading, validation, retry counters, artifacts, and terminal reasons. Run state owns queue position, checkpoint references, histories, and resumability. Commands SHALL not silently advance state: every state write requires an explicit write flag, a valid input record, and an output artifact that explains the proposed transition.

## Open Interface Constraints

The repository policy does not yet define artifact-directory conventions, task-record storage locations, checkpoint authorization workflow, or evidence fingerprint format. Future implementation SHALL keep these as explicit interface inputs until a policy update defines them.
