# System Architecture

## Purpose

This document defines the stable architectural boundaries of the repository.

It describes:

- major runtime areas
- ownership of responsibilities
- allowed dependency directions
- cross-role boundaries
- architectural invariants

This document does not prescribe:

- concrete class names
- package names
- framework choices
- transport protocol details
- persistence technology
- dependency-injection technology
- UI architecture patterns

Detailed product behavior belongs in feature specifications.

Implementation practices belong in `STYLE.md`.

---

# Architectural Goals

The architecture should support:

- one Android application
- two mutually exclusive runtime roles
- clear separation between Sender and Receiver behavior
- local-network notification transfer
- independently testable product capabilities
- incremental implementation
- understandable failure boundaries
- practical validation on physical Android devices

The architecture should remain as small as possible while preserving these
boundaries.

---

# System Context

The product operates across two Android devices.

```text
Primary Android Device
    ↓
Sender Runtime
    ↓
Local Communication Boundary
    ↓
Receiver Runtime
    ↓
Secondary Android Device
```

The Sender observes eligible Android notifications.

The Receiver presents transferred notification information.

The local communication boundary connects the two roles without making either
role responsible for the other's internal implementation.

---

# Runtime Role Boundary

A device has one active runtime role at a time:

- Sender
- Receiver

Role-specific behavior must remain separated.

The Sender must not execute Receiver-only responsibilities.

The Receiver must not execute Sender-only responsibilities.

Role selection may be shared product behavior, but active runtime components
must respect the selected role.

Changing roles must not require maintaining both role runtimes concurrently
unless a future feature specification explicitly changes this invariant.

---

# Sender Boundary

The Sender owns capabilities related to:

- Android notification access
- notification observation
- eligibility evaluation
- preparation of transferable notification data
- initiation or maintenance of outbound local communication
- transmission status
- sender-specific operational state

The Sender must not own:

- Receiver inbox rendering
- Receiver notification history presentation
- Receiver-specific storage behavior
- Receiver reading interactions

The Sender may produce transferable data, but it must not depend on Receiver UI
or Receiver persistence details.

---

# Receiver Boundary

The Receiver owns capabilities related to:

- inbound local communication
- notification reception
- validation of received notification data
- Receiver-side retention required by active specifications
- inbox presentation
- reading-oriented interaction
- receiver-specific operational state

The Receiver must not own:

- Android notification observation on the primary device
- Sender filtering decisions
- Sender notification-access management
- Sender runtime lifecycle

The Receiver consumes transferable notification data without depending on the
Sender's Android notification objects or implementation details.

---

# Transfer Boundary

Communication between Sender and Receiver must pass through an explicit transfer
boundary.

The transfer boundary is responsible for representing data that can cross
between devices.

Role-internal objects must not cross this boundary directly.

Examples of role-internal objects include:

- Android framework notification objects
- UI state objects
- database entities tied to one device
- service lifecycle objects
- framework-specific callbacks

Transfer data should contain only the information required by active feature
specifications.

The transfer representation must not silently become the storage model or UI
model unless a specification explicitly justifies that choice.

---

# Notification Processing Flow

The architectural processing flow is:

```text
Notification Source
    ↓
Observation
    ↓
Eligibility Decision
    ↓
Transfer Representation
    ↓
Transport
    ↓
Reception
    ↓
Receiver Representation
    ↓
Presentation
```

Each boundary should have a clear responsibility.

Observation does not decide presentation.

Presentation does not capture Android notifications.

Transport does not define notification eligibility.

Persistence does not define network protocol behavior.

---

# UI Boundary

User-interface code owns:

- rendering visible state
- receiving user intent
- presenting loading, empty, success, and failure states
- invoking application behavior through explicit boundaries

User-interface code must not directly own:

- Android notification listener callbacks
- raw network socket lifecycle
- protocol encoding
- protocol decoding
- long-lived background service coordination
- persistence implementation details

UI code may observe state exposed by the responsible runtime capability.

The architecture does not require one specific UI state-management pattern.

---

# Android Platform Boundary

Android platform integration must remain isolated from product behavior where
practical.

Platform-facing areas may include:

- notification-listener integration
- permissions and settings access
- services
- lifecycle callbacks
- connectivity APIs
- foreground execution requirements

Product behavior should not depend on Android framework objects beyond the
boundary where those objects are required.

Platform callbacks should be translated into repository-owned data or events
before being used by feature logic where practical.

---

# Persistence Boundary

Persistence is optional until required by a feature specification.

When persistence is introduced:

- persistence concerns must remain separate from UI rendering
- persisted representations must not define transfer protocol behavior
- storage failures must be observable by the owning capability
- retention and deletion rules must come from feature specifications
- storage technology must remain replaceable unless a present requirement makes
  that impractical

The architecture does not require persistence for capabilities that can remain
in memory.

---

# Background Execution Boundary

Long-running or background behavior belongs to explicit runtime components.

Background components may coordinate:

- notification observation
- connection maintenance
- outbound transfer
- inbound reception

They must expose understandable operational state to the application.

Background execution must not silently become the owner of UI navigation or
presentation behavior.

Lifecycle requirements must be introduced through feature specifications and
Android platform constraints.

---

# State Ownership

Each significant state must have one clear owner.

Examples include:

- selected role
- notification-access state
- local-session state
- sender activity state
- receiver activity state
- received notification collection
- filter configuration

Multiple components may observe state.

Multiple components must not independently mutate the same state without an
explicit coordination boundary.

Derived UI state should not become a second source of truth for product state.

---

# Dependency Direction

Dependencies should point toward stable product behavior rather than toward
framework details.

Conceptually:

```text
UI
    ↓
Application Capability
    ↓
Product Rules
    ↓
Platform / Transport / Persistence Adapters
```

This diagram expresses responsibility, not a required package structure.

Product rules must not depend on:

- Android UI widgets
- concrete storage engines
- concrete transport implementations
- device-specific presentation details

Adapters may depend on platform or infrastructure APIs.

Higher-level behavior should depend on explicit boundaries rather than concrete
adapter internals where such separation is currently useful.

---

# Feature Ownership

Every implemented product capability should have one primary feature
specification.

A feature may affect multiple architectural areas, but its behavior must not be
duplicated across unrelated specifications.

Cross-cutting infrastructure may support multiple features.

Infrastructure must not introduce new user-visible behavior without a feature
specification.

When a capability spans Sender, transfer, and Receiver, the specification should
describe the end-to-end behavior while the implementation preserves the
architectural boundaries.

---

# Shared Code

Shared code is appropriate only for concepts genuinely shared by both roles.

Examples may include:

- role identifiers
- transfer data definitions
- common validation rules
- time representation
- stable notification identifiers
- shared presentation-neutral text formatting

Code must not be moved into a shared area merely because two implementations
currently look similar.

Shared code must not erase meaningful Sender and Receiver ownership boundaries.

---

# Failure Boundaries

Failures should remain attributable to the capability that owns them.

Examples:

- notification access failure belongs to Sender platform integration
- eligibility rejection belongs to filtering behavior
- connection failure belongs to the local-session capability
- decoding failure belongs to the transfer boundary
- storage failure belongs to Receiver persistence
- rendering failure belongs to UI presentation

One failure should not silently corrupt unrelated state.

Failure handling and recovery behavior must be defined in feature
specifications.

---

# Security Boundary

Security mechanisms must remain proportional to the personal local-network MVP.

Security responsibilities may include:

- local session identification
- basic peer verification
- rejection of malformed inbound data
- prevention of obvious accidental delivery to the wrong peer

Security behavior must be explicit.

It must not be scattered across UI, transport, and storage code without clear
ownership.

Production-grade threat models are outside the current architecture unless
introduced by a future specification.

---

# Validation Boundary

Each architectural area should be validated at the strongest practical level.

Examples include:

- product rules through deterministic unit tests
- serialization and validation through local tests
- Android platform integration through instrumentation or device validation
- local communication through two-process or two-device validation
- end-to-end delivery through two physical Android devices

Compilation alone is not sufficient evidence for changed behavior.

Specific validation requirements belong in feature specifications.

---

# Architectural Invariants

The following invariants must remain true unless this document is explicitly
changed.

## Invariant A-01 — One Active Role

A device operates in one active role at a time.

## Invariant A-02 — Role Separation

Sender-only and Receiver-only responsibilities remain separated.

## Invariant A-03 — Explicit Transfer Boundary

Cross-device communication uses an explicit transferable representation.

## Invariant A-04 — No Framework Objects Across Devices

Android framework objects do not cross the transfer boundary.

## Invariant A-05 — UI Does Not Own Runtime Infrastructure

UI code does not directly own notification observation, raw transport, or
long-lived background execution.

## Invariant A-06 — Single State Ownership

Significant mutable product state has one clear owner.

## Invariant A-07 — Behavior Comes From Specifications

Infrastructure and implementation do not silently introduce new observable
product behavior.

## Invariant A-08 — Local-First Core Flow

The core notification-delivery flow does not require a cloud relay.

## Invariant A-09 — Failures Stay Attributable

Failures remain observable and attributable to the capability that owns them.

## Invariant A-10 — No Speculative Architecture

New architectural layers require a current product or engineering need.

---

# Architectural Change Rules

This document should change only when:

- a major system boundary changes
- role ownership changes
- dependency direction changes
- a new persistent architectural invariant is introduced
- the product adopts a substantially different runtime model

Ordinary class extraction, package movement, UI refactoring, or implementation
replacement should not require changes to this document unless they affect a
defined boundary.

When implementation conflicts with this document:

1. stop the implementation
2. determine whether the implementation or architecture specification is wrong
3. update the appropriate specification first
4. continue only after the conflict is resolved