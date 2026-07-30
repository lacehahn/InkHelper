# Notification Transfer and Inbound Validation Specification

## Purpose

This feature transfers eligible notification representations through a connected local session, validates inbound information, and makes accepted notifications available to the Receiver inbox. It does not define the transport protocol, persistence, ordering, or deduplication policy.

## Requirements

### Requirement: Eligible notifications transfer through a connected local session

The Sender SHALL attempt to transfer an eligible notification representation only while Sender is active and its local session is connected. A transfer is confirmed received only when the connected Receiver validates the representation, makes it available to the Receiver inbox, and returns confirmation through that same local session. If no session is connected, the notification SHALL be marked not sent and SHALL NOT be retained as pending work for later retry.

#### Scenario: Eligible notification is transferred <!-- S:F-0006-S01 -->

- **GIVEN** Sender is active
- **AND** an eligible transferable notification is available
- **AND** the local session is connected
- **WHEN** transfer is initiated
- **THEN** the notification is delivered to Receiver for inbound validation
- **AND** Sender presents the transfer as confirmed received only after Receiver accepts it and returns confirmation

#### Scenario: Transfer is not sent without a connected session <!-- S:F-0006-S02 -->

- **GIVEN** Sender is active
- **AND** an eligible transferable notification is available
- **AND** the local session is not connected
- **WHEN** transfer is initiated
- **THEN** the notification is not delivered
- **AND** Sender presents the notification as not sent
- **AND** the notification is not retained as pending transfer work

### Requirement: Receiver validates inbound notification information

The Receiver SHALL accept inbound notification information only when Receiver is active, the local session is connected, the information arrives through the recognized current session, and the information conforms to the MVP transfer representation. The Receiver SHALL reject information that fails any of those conditions and SHALL NOT add rejected information to the inbox.

#### Scenario: Valid inbound notification is accepted <!-- S:F-0006-S03 -->

- **GIVEN** Receiver is active
- **AND** the local session is connected
- **AND** inbound information arrives through the recognized current session
- **WHEN** Receiver receives inbound information that conforms to the active transfer representation
- **THEN** the notification is accepted
- **AND** it becomes available to the Receiver inbox

#### Scenario: Malformed inbound notification is rejected <!-- S:F-0006-S04 -->

- **GIVEN** Receiver has existing inbox items
- **WHEN** Receiver receives malformed or incomplete inbound notification information
- **THEN** the information is rejected
- **AND** existing inbox items remain unchanged

#### Scenario: Inbound information is rejected while Receiver is inactive <!-- S:F-0006-S07 -->

- **GIVEN** Receiver is inactive
- **WHEN** inbound notification information arrives
- **THEN** the information is rejected
- **AND** no Receiver inbox item is added

#### Scenario: Inbound information is rejected without a connected session <!-- S:F-0006-S08 -->

- **GIVEN** Receiver is active
- **AND** no local session is connected
- **WHEN** inbound notification information arrives
- **THEN** the information is rejected
- **AND** existing inbox items remain unchanged

#### Scenario: Inbound information from another session is rejected <!-- S:F-0006-S09 -->

- **GIVEN** Receiver is active
- **AND** a local session is connected
- **WHEN** inbound notification information arrives from an unrecognized session
- **THEN** the information is rejected
- **AND** existing inbox items remain unchanged

### Requirement: Interrupted transfer has an explicit non-success outcome

When the local session is interrupted before a notification is confirmed received, the Sender SHALL expose the transfer as unconfirmed or failed. The Receiver SHALL NOT add a partial or unvalidated transfer to the inbox. The feature SHALL NOT claim guaranteed delivery or automatic retransmission.

#### Scenario: Session interruption prevents confirmed delivery <!-- S:F-0006-S05 -->

- **GIVEN** Sender is transferring an eligible notification
- **AND** Receiver has not confirmed receipt
- **WHEN** the local session is interrupted
- **THEN** Sender does not report confirmed receipt
- **AND** Receiver does not add a partial notification to the inbox
- **AND** Sender does not retain the notification as pending transfer work

#### Scenario: Role switch interrupts an in-progress transfer <!-- S:F-0006-S10 -->

- **GIVEN** Sender is transferring an eligible notification
- **AND** Receiver has not confirmed receipt
- **WHEN** the user switches Sender to Receiver
- **THEN** the transfer is unconfirmed
- **AND** it is not retained as pending transfer work
- **AND** Receiver inbox items already present on that device are not deleted

#### Scenario: Later transfers can proceed after session recovery <!-- S:F-0006-S06 -->

- **GIVEN** a prior transfer was unconfirmed because the local session was interrupted
- **AND** the local session is connected again
- **WHEN** Sender initiates transfer of a later eligible notification
- **THEN** the later notification follows the normal transfer and validation behavior
- **AND** the prior unconfirmed transfer is not represented as confirmed

### Requirement: Repeated notification events remain distinct in the MVP

The MVP SHALL treat each accepted notification event as a distinct transfer event. The MVP SHALL NOT claim duplicate-event suppression.

#### Scenario: Repeated accepted events create distinct inbox items <!-- S:F-0006-S11 -->

- **GIVEN** Receiver is active and connected
- **AND** two accepted notification events are transferred and validated
- **WHEN** the Receiver inbox is shown
- **THEN** each accepted event is available as a distinct inbox item

## Validation

- Automated: transfer confirmation, no-session disposition, inbound rejection, repeated-event handling, and state preservation.
- Two-device manual: transfer a representative eligible notification, interrupt a transfer, restore the session, and transfer a later notification.

## Dependencies

- `F-0004` Notification Eligibility and Transfer Representation
- `F-0005` Local Session Lifecycle

## Open Questions

- Automatic retry, user-initiated resend, and future duplicate-event suppression are not defined. The MVP has no pending transfer queue and no duplicate suppression.
