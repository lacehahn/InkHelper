# Repository Style

## Purpose

This document defines the repository's specification, implementation, testing,
validation, and review style.

It exists to keep agent-written changes consistent, reviewable, and traceable.

This document does not define product behavior.

Product behavior belongs in feature specifications.

Architectural boundaries belong in `ARCHITECTURE.md`.

Product scope belongs in `HARNESS.md`.

Implementation order belongs in `ROADMAP.md`.

---

# Core Style Principles

Repository work should be:

- specification-first
- behavior-oriented
- minimal
- explicit
- traceable
- testable
- reviewable
- honest about uncertainty

Prefer direct language over abstract language.

Prefer observable behavior over implementation description.

Prefer the smallest complete change over broad generalization.

Do not introduce future behavior through implementation alone.

---

# Specification Style

Specifications describe required behavior.

They should state:

- what capability exists
- under what conditions it applies
- what is accepted
- what is rejected
- what observable outcome occurs
- how failure is represented
- how recovery works
- how the behavior is validated

Specifications should avoid unnecessary implementation details.

Do not define behavior using only vague words such as:

- should work
- handles correctly
- behaves normally
- supports notifications
- is reliable
- is user-friendly

Replace vague statements with observable conditions.

Instead of:

```text
The Receiver should handle invalid messages correctly.
```

Write:

```text
The Receiver rejects a message that cannot be decoded and keeps the current
inbox state unchanged.
```

---

# Requirement Style

Each feature specification should contain one or more requirements.

Use this structure:

```markdown
### Requirement: <short capability statement>

<Normative requirement text>
```

Requirement names should describe one coherent behavioral contract.

Good examples:

```text
Requirement: The selected role persists across application launches

Requirement: Ineligible notifications are not transferred

Requirement: Malformed inbound messages do not enter the inbox
```

Avoid requirement names that describe implementation tasks:

```text
Requirement: Add SharedPreferences

Requirement: Create ViewModel

Requirement: Implement socket server
```

Requirements should use normative language.

Use:

- `SHALL` for mandatory behavior
- `SHALL NOT` for prohibited behavior
- `MAY` for explicitly optional behavior

Example:

```text
The application SHALL restore the previously selected role after process
restart.

The application SHALL NOT activate both runtime roles concurrently.
```

Avoid mixing mandatory and optional behavior in the same sentence.

---

# Scenario Style

Each requirement should be supported by one or more scenarios.

Use this structure:

```markdown
#### Scenario: <observable behavior> <!-- S:F-0001-S01 -->

- **GIVEN** <initial state or precondition>
- **WHEN** <trigger or action>
- **THEN** <observable result>
- **AND** <additional result>
```

`GIVEN` is optional when no meaningful precondition exists.

`WHEN` identifies the event being evaluated.

`THEN` identifies the primary observable result.

`AND` adds related conditions without hiding separate behavior.

Example:

```markdown
#### Scenario: Previously selected Sender role is restored <!-- S:F-0001-S02 -->

- **GIVEN** the user previously selected Sender
- **AND** the application process is not running
- **WHEN** the user launches the application
- **THEN** Sender is the active role
- **AND** Receiver runtime behavior is not active
```

Scenarios should describe behavior from the perspective of an observer, user,
test, or system boundary.

Do not write scenarios as implementation steps.

Avoid:

```text
WHEN the ViewModel calls the repository
THEN the StateFlow emits
```

Prefer:

```text
WHEN the user selects Receiver
THEN Receiver becomes the active role
```

Implementation-level scenarios are acceptable only for explicit engineering
contracts, not product feature behavior.

---

# Scenario Scope

One scenario should represent one independently understandable behavior.

A scenario may contain multiple assertions when they describe one outcome.

Split scenarios when:

- different preconditions produce different outcomes
- success and failure behavior differ
- recovery behavior differs
- accepted and rejected input need separate evidence
- one assertion can fail independently of the others

Do not create one scenario that attempts to describe an entire feature.

Avoid excessively small scenarios for incidental details that cannot be
meaningfully validated alone.

---

# Feature Identifiers

Each feature uses a stable identifier:

```text
F-0001
F-0002
F-0003
```

Feature directories should use:

```text
F-0001-role-selection
F-0002-receiver-inbox
F-0003-notification-capture
```

Rules:

- feature numbers are assigned sequentially
- identifiers are never reused
- existing feature identifiers are never renumbered for presentation
- directory names may be clarified, but the numeric identifier remains stable
- one feature identifier represents one coherent user or system capability

Do not infer feature priority from gaps in numbering.

A retired feature keeps its identifier and should be marked accordingly.

---

# Scenario Identifiers

Scenario identifiers use this form:

```text
F-0001-S01
F-0001-S02
F-0002-S01
```

Rules:

- every maintained scenario has one stable identifier
- scenario identifiers are unique across the repository
- identifiers are not renumbered after tests or implementation reference them
- deleted scenarios are not silently reused
- materially changed behavior should normally receive a new scenario identifier
- editorial clarification does not require a new identifier

The HTML comment form is preferred:

```markdown
#### Scenario: Sender becomes active <!-- S:F-0001-S01 -->
```

This keeps the rendered heading readable while allowing deterministic parsing.

---

# Accepted and Rejected Conditions

Feature specifications should explicitly identify accepted and rejected
conditions when input or eligibility is involved.

Example:

```markdown
## Accepted Conditions

A captured notification is eligible when:

- it originates from an enabled application
- it contains the fields required by the active transfer specification
- it is not excluded by an active filter
```

```markdown
## Rejected Conditions

A captured notification is rejected when:

- its source application is disabled
- required content is unavailable
- an active exclusion rule matches it
```

Accepted and rejected conditions must not contradict scenario behavior.

Where practical, each important rejection rule should have a scenario.

---

# Failure Style

Failure behavior must be explicit.

A specification should identify:

- what fails
- what remains unchanged
- what state is exposed
- whether retry is permitted
- whether recovery is automatic or user-driven
- whether data is retained, discarded, or quarantined

Avoid:

```text
The app shows an error.
```

Prefer:

```text
When the local session cannot be established, the application remains in the
selected role, reports the session as disconnected, and allows the user to try
again.
```

Do not claim lossless behavior, guaranteed delivery, or automatic recovery
unless the specification defines and validates it.

---

# State Style

Significant state should be named consistently across specifications,
implementation, tests, and UI.

Examples:

```text
active role
notification access
session state
capture state
transfer state
receiver inbox
```

Avoid creating multiple terms for the same state.

If two terms have different meanings, define the distinction explicitly.

State transitions should be described when behavior depends on them.

Example:

```text
Disconnected
    ↓
Connecting
    ↓
Connected
```

State names in specifications are conceptual and do not require identical code
identifiers.

---

# Feature Specification Structure

A feature specification should normally contain:

```text
Status
Summary
Motivation
Scope
Out of Scope
Actors
Preconditions
Requirements
Failure and Recovery
State Changes
Acceptance Criteria
Validation
Dependencies
Notes
```

Not every section must contain content.

Remove empty optional sections rather than adding placeholder prose.

Requirements and scenarios are mandatory for behavioral features.

---

# Status Style

Feature status should use one of:

```text
Draft
Ready
In Progress
Implemented
Validated
Deferred
Retired
```

Meaning:

- `Draft`: behavior is still being designed
- `Ready`: behavior is sufficiently defined for implementation
- `In Progress`: implementation is active
- `Implemented`: implementation exists but full validation may remain
- `Validated`: required validation evidence exists
- `Deferred`: not part of the active roadmap
- `Retired`: behavior is intentionally no longer active

Do not mark a feature `Validated` when required manual or device validation has
not been performed.

---

# Implementation Style

Implementation should satisfy the active specification with the smallest
complete design.

Prefer:

- direct code
- clear ownership
- narrow changes
- explicit state
- deterministic behavior
- existing repository patterns

Avoid:

- speculative abstractions
- premature framework layers
- generic infrastructure for one use case
- unrelated refactoring
- hidden behavior in convenience utilities
- duplicate sources of truth

An abstraction is justified when it:

- represents an architectural boundary
- supports more than one current behavior
- isolates platform or infrastructure concerns
- makes required testing possible
- removes demonstrated duplication without obscuring ownership

Do not add abstractions solely because they are common Android patterns.

---

# Naming Style

Names should describe current responsibility.

Prefer:

```text
RoleSelection
NotificationCapture
TransferMessage
ReceiverInbox
SessionState
```

Avoid vague names such as:

```text
Manager
Helper
Util
Handler
Processor
Common
Base
```

These names are acceptable only when the narrower responsibility is already
clear from context.

Do not use future-oriented names for capabilities that do not yet exist.

---

# Test Style

Tests provide evidence for scenarios.

Tests should focus on observable behavior and stable boundaries.

When practical, each behavioral test should reference one or more scenario
identifiers.

Kotlin example:

```kotlin
// F-0001-S01
@Test
fun selectingSenderActivatesOnlySenderRole() {
    // ...
}
```

A test name should describe:

- the condition or action
- the expected outcome

Prefer:

```text
selectingReceiverDeactivatesSender
invalidTransferMessageDoesNotEnterInbox
savedRoleIsRestoredAfterRestart
```

Avoid:

```text
testRole
testNotification
worksCorrectly
```

One test may validate more than one scenario only when the scenarios are
inseparable at that validation surface.

Do not add a scenario reference to a test that does not actually validate it.

---

# Test Surface Selection

Use the lowest-cost deterministic test surface that proves the behavior.

Preferred order:

1. local unit test
2. Android instrumented test
3. single-device manual validation
4. two-device manual validation
5. target-device validation

Higher-cost validation does not replace lower-cost deterministic tests when
the behavior can be tested locally.

Unit tests should not imitate Android integration when real platform behavior
is the subject of the scenario.

Manual validation should be used where automation is impractical, not as a
default substitute for tests.

---

# Validation Style

Every implemented feature should define applicable validation.

Validation may include:

- unit tests
- lint
- assembly
- instrumentation tests
- single-device steps
- two-device steps
- e-ink device checks

Validation instructions must state:

- setup
- action
- expected result
- required devices or environment
- limitations

Avoid validation statements such as:

```text
Test manually.
```

Prefer:

```text
On two Android devices connected to the same local network, configure one
device as Sender and the other as Receiver. Trigger an eligible notification on
the Sender. Verify that one corresponding item appears in the Receiver inbox.
```

Do not record a validation result unless it was actually performed.

---

# Validation Evidence

Completion reports should distinguish:

```text
Passed
Failed
Not Run
Blocked
Partially Verified
```

Use `Passed` only when the described command or procedure completed
successfully.

Use `Not Run` when validation was not attempted.

Use `Blocked` when an external condition prevented validation.

Use `Partially Verified` when only part of a scenario was validated.

Unverified behavior must remain visible in the completion report.

---

# Review Style

Review the final change against:

- the active feature specification
- affected scenario identifiers
- architectural invariants
- scope rules
- test evidence
- accidental unrelated changes

A review should ask:

1. Does the implementation match the specified behavior?
2. Is any new behavior unspecified?
3. Are failure cases handled as specified?
4. Are state owners clear?
5. Are scenario references accurate?
6. Is validation strong enough?
7. Did the change expand scope unnecessarily?

Review should focus on correctness before stylistic preference.

---

# Documentation Style

Use Markdown headings consistently.

Prefer short paragraphs.

Use lists for conditions, rules, and steps.

Use diagrams only when they clarify boundaries or flow.

Use repository-relative paths.

Use code blocks for:

- identifiers
- directory layouts
- commands
- state flows
- structured examples

Do not repeat the same normative rule across multiple documents unless one
document explicitly references the authoritative source.

Avoid decorative wording and promotional language.

---

# Language Style

Normative repository specifications should use clear technical English unless
the repository explicitly adopts another primary specification language.

User-facing copy may use the language required by the feature.

Do not mix languages within one normative requirement or scenario unless the
scenario is specifically validating localized content.

Terms that appear in product state, UI, code, and tests should be translated or
defined consistently.

---

# Change Style

When behavior changes:

1. update the relevant specification
2. update or add scenarios
3. update tests
4. update implementation
5. run validation
6. review the diff

When behavior does not change, avoid unnecessary specification edits.

Refactoring should preserve scenario behavior.

A refactor that changes observable behavior is a behavioral change and requires
specification review.

---

# Completion Report Style

Every completion report should use this structure:

```text
Purpose

Specifications

Scenarios

Files Changed

Behavioral Result

Architectural Impact

Validation

Unverified Items

Next Step
```

Example:

```markdown
## Purpose

Persist the selected runtime role across application launches.

## Specifications

- `spec/features/F-0001-role-selection/spec.md`

## Scenarios

- `F-0001-S02`

## Files Changed

- `...`

## Behavioral Result

The previously selected role is restored after process restart.

## Architectural Impact

No architectural boundary changed.

## Validation

- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — Passed

## Unverified Items

- Physical-device process-death behavior was not manually verified.

## Next Step

Define the role-transition failure scenarios.
```

Do not hide warnings or omitted validation inside general summary text.

---

# Prohibited Style

Do not:

- implement behavior without an applicable specification
- use one scenario identifier for unrelated outcomes
- renumber stable feature or scenario identifiers
- claim validation that was not performed
- add broad abstractions for hypothetical needs
- mix product requirements with implementation tasks
- use compilation as the only evidence for behavioral correctness
- silently resolve conflicting specifications
- overwrite unrelated user work
- describe uncertain behavior as guaranteed

---

# Style Evolution

This document should change when repeated repository work reveals a stable
engineering rule.

Do not add rules for one isolated preference.

A new rule should improve at least one of:

- behavioral clarity
- traceability
- correctness
- validation quality
- reviewability
- repository safety

Rules that can be deterministically enforced may later become part of the local
AFD harness.