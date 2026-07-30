# Runtime Role Selection Specification

## Purpose

The application supports the Sender and Receiver roles within one APK. This feature defines selection, restoration, and switching of the one active runtime role. It does not define role-specific notification, session, or inbox behavior.

## Requirements

### Requirement: The user selects the active runtime role

The application SHALL require the user to select Sender or Receiver before role-specific runtime behavior becomes active. The application SHALL NOT activate both roles concurrently.

#### Scenario: Initial launch requires role selection <!-- S:F-0001-S01 -->

- **GIVEN** no role was previously selected
- **WHEN** the application starts
- **THEN** neither Sender nor Receiver runtime behavior is active
- **AND** the user can select one role

#### Scenario: Sender becomes active <!-- S:F-0001-S02 -->

- **GIVEN** no active role
- **WHEN** the user selects Sender
- **THEN** Sender becomes the active role
- **AND** Receiver runtime behavior remains inactive

#### Scenario: Receiver becomes active <!-- S:F-0001-S03 -->

- **GIVEN** no active role
- **WHEN** the user selects Receiver
- **THEN** Receiver becomes the active role
- **AND** Sender runtime behavior remains inactive

### Requirement: The selected role is restored

The application SHALL restore the previously selected role when it starts after a normal application or process restart. Restoration SHALL NOT activate the other role.

#### Scenario: Sender role is restored <!-- S:F-0001-S04 -->

- **GIVEN** Sender was previously selected
- **AND** the application is not running
- **WHEN** the application starts
- **THEN** Sender is the active role
- **AND** Receiver runtime behavior is inactive

#### Scenario: Receiver role is restored <!-- S:F-0001-S05 -->

- **GIVEN** Receiver was previously selected
- **AND** the application is not running
- **WHEN** the application starts
- **THEN** Receiver is the active role
- **AND** Sender runtime behavior is inactive

### Requirement: The user can switch roles

The application SHALL allow the user to replace the active role with the other role. The previously active role SHALL become inactive before the replacement role becomes active. Switching roles SHALL deactivate the prior role's active session and transfer work as defined by their owning features, and SHALL NOT delete existing Receiver inbox items.

#### Scenario: Sender switches to Receiver <!-- S:F-0001-S06 -->

- **GIVEN** Sender is active
- **WHEN** the user selects Receiver
- **THEN** Sender becomes inactive
- **AND** Receiver becomes the active role

#### Scenario: Receiver switches to Sender <!-- S:F-0001-S07 -->

- **GIVEN** Receiver is active
- **WHEN** the user selects Sender
- **THEN** Receiver becomes inactive
- **AND** Sender becomes the active role

#### Scenario: Selecting the active role again preserves the selection <!-- S:F-0001-S08 -->

- **GIVEN** Sender is active
- **WHEN** the user selects Sender again
- **THEN** Sender remains the active role
- **AND** Receiver remains inactive

#### Scenario: Selecting the active Receiver role again preserves the selection <!-- S:F-0001-S09 -->

- **GIVEN** Receiver is active
- **WHEN** the user selects Receiver again
- **THEN** Receiver remains the active role
- **AND** Sender remains inactive

## Validation

- Automated: role selection, restoration, switching, and repeated selection.
- Single-device manual: launch, choose each role, restart, and verify the displayed active role.

## Dependencies

None.
