# Sender Notification Access and Observation Specification

## Purpose

The Sender obtains the Android access required to observe notifications and exposes accessible notification events to eligibility evaluation. It defines no filtering rule, transfer representation, or delivery behavior.

## Requirements

### Requirement: The Sender exposes notification-access availability

When Sender is the active role, the application SHALL expose whether the Android notification access required for observation is available. The application SHALL provide a user-initiated path to request or enable that access when it is unavailable, and SHALL update the exposed state when access becomes available or is revoked.

#### Scenario: Available notification access is presented <!-- S:F-0003-S01 -->

- **GIVEN** Sender is active
- **AND** Android notification access is available
- **WHEN** Sender operational state is presented
- **THEN** notification access is observable as available

#### Scenario: Unavailable notification access is presented <!-- S:F-0003-S02 -->

- **GIVEN** Sender is active
- **AND** Android notification access is unavailable
- **WHEN** Sender operational state is presented
- **THEN** notification access is observable as unavailable
- **AND** the user can initiate the access-enablement path

#### Scenario: Access remains unavailable after denial or cancellation <!-- S:F-0003-S03 -->

- **GIVEN** Sender is active
- **AND** notification access is unavailable
- **WHEN** the user does not enable notification access through the available path
- **THEN** Sender remains the active role
- **AND** notification observation remains inactive

#### Scenario: Access becomes available after user action <!-- S:F-0003-S07 -->

- **GIVEN** Sender is active
- **AND** notification access is unavailable
- **WHEN** the user enables Android notification access through the available path
- **THEN** notification access is presented as available
- **AND** Sender can observe newly accessible notification events

#### Scenario: Access is revoked while Sender is active <!-- S:F-0003-S08 -->

- **GIVEN** Sender is active
- **AND** notification access is available
- **WHEN** Android notification access is revoked
- **THEN** notification access is presented as unavailable
- **AND** notification observation becomes inactive

### Requirement: The Sender observes accessible notification events

The Sender SHALL observe new notification events only while Sender is active and required Android notification access is available. The Sender SHALL pass an observed event to notification eligibility evaluation without exposing Android notification objects beyond the platform boundary.

#### Scenario: Accessible notification is observed <!-- S:F-0003-S04 -->

- **GIVEN** Sender is active
- **AND** notification access is available
- **WHEN** Android makes a new notification event accessible
- **THEN** the event is available for eligibility evaluation

#### Scenario: Notification is not observed while access is unavailable <!-- S:F-0003-S05 -->

- **GIVEN** Sender is active
- **AND** notification access is unavailable
- **WHEN** Android posts a notification
- **THEN** the event is not made available for eligibility evaluation by Sender

#### Scenario: Sender stops observation after a role switch <!-- S:F-0003-S06 -->

- **GIVEN** Sender is active
- **AND** notification access is available
- **WHEN** the user switches to Receiver
- **THEN** Sender notification observation becomes inactive
- **AND** Receiver does not require notification access

## Validation

- Automated: role and access-state gating of observation where platform-independent behavior can be isolated.
- Single-device manual: enable, deny or leave unavailable, revoke, and restore Android notification access on a Sender device.

## Dependencies

- `F-0001` Runtime Role Selection

## Open Questions

- The supported Android versions and notification-access edge cases are constrained by platform behavior but are not further defined by current specifications.
