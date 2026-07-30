# Feature Specifications

## Purpose

This directory contains the behavioral specifications for individual product
capabilities.

Each feature specification defines one coherent capability of the product.

A feature specification describes observable behavior rather than
implementation.

---

# Specification Schema

Every feature specification SHALL follow the canonical repository structure.

```markdown
# <Feature Name> Specification

## Purpose

<Feature overview>

## Requirements

### Requirement: <Behavioral contract>

<Normative requirement>

#### Scenario: <Observable behavior> <!-- S:<SCENARIO-ID> -->

- **GIVEN** ...
- **WHEN** ...
- **THEN** ...
- **AND** ...
```

This schema is the canonical structure for all feature specifications in this
repository.

---

# Purpose

Every feature specification SHALL contain exactly one `Purpose` section.

The Purpose section describes the capability as a whole.

It SHOULD explain:

- why the capability exists
- what behavior it introduces
- important capability-wide constraints
- important capability-wide exclusions

The Purpose section SHALL NOT describe implementation details.

---

# Requirements

Every feature specification SHALL contain one or more requirements.

Each requirement SHALL use the following heading:

```markdown
### Requirement: <Behavioral contract>
```

Requirements define mandatory product behavior.

Requirements SHALL describe behavior rather than implementation.

Normative language SHALL use:

- `SHALL`
- `SHALL NOT`
- `MAY`

Each requirement SHOULD define one coherent behavioral contract.

---

# Scenarios

Every requirement SHALL contain one or more scenarios.

Each scenario SHALL use the following heading:

```markdown
#### Scenario: <Observable behavior> <!-- S:<SCENARIO-ID> -->
```

Scenario identifiers SHALL:

- be unique
- remain stable
- never be reused
- follow the repository identifier convention

Scenario identifiers are intended to be referenced by tests and implementation.

---

# Scenario Format

Scenario steps SHALL use only the following keywords:

- **GIVEN**
- **WHEN**
- **THEN**
- **AND**

Example:

```markdown
#### Scenario: Previously selected role is restored <!-- S:<FEATURE-ID>-S01 -->

- **GIVEN** Sender was previously selected
- **WHEN** the application starts
- **THEN** Sender becomes the active role
- **AND** Receiver remains inactive
```

A scenario MAY omit `GIVEN` when no meaningful precondition exists.

Every scenario SHALL contain exactly one primary `WHEN`.

Every scenario SHALL contain at least one `THEN`.

Additional observable outcomes SHALL use `AND`.

Scenario steps SHALL describe observable behavior.

Scenario steps SHALL NOT describe implementation details.

---

# Behavioral Coverage

Feature specifications SHOULD define the observable behavior required by the
capability.

Where applicable, behavior SHOULD include:

- successful behavior
- rejected behavior
- invalid input
- failure behavior
- recovery behavior
- state transitions
- repeated actions
- restart behavior
- preservation of unrelated state

Only behaviors relevant to the capability should be specified.

---

# Writing Rules

Feature specifications SHALL:

- describe behavior
- remain implementation independent
- use observable outcomes
- remain internally consistent
- remain independently understandable

Feature specifications SHALL NOT:

- prescribe frameworks
- prescribe libraries
- prescribe class names
- prescribe package structures
- prescribe implementation patterns

Implementation belongs to source code.

Behavior belongs to feature specifications.

---

# Example

```markdown
# Runtime Role Selection Specification

## Purpose

The application supports two mutually exclusive runtime roles: Sender and
Receiver.

The selected role SHALL remain the active runtime role until the user changes
it.

The application SHALL NOT activate both runtime roles simultaneously.

## Requirements

### Requirement: Only one runtime role is active

The application SHALL operate as either Sender or Receiver.

#### Scenario: Sender becomes the active role <!-- S:<FEATURE-ID>-S01 -->

- **WHEN** the user selects Sender
- **THEN** Sender becomes the active role
- **AND** Receiver is inactive

#### Scenario: Receiver becomes the active role <!-- S:<FEATURE-ID>-S02 -->

- **WHEN** the user selects Receiver
- **THEN** Receiver becomes the active role
- **AND** Sender is inactive
```
