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

#### Scenario: User-initiated representative transfer follows connected-session behavior <!-- S:F-0006-S12 -->

- **GIVEN** Sender is active
- **AND** the local session is connected
- **WHEN** the user initiates a representative eligible notification transfer from the Sender UI
- **THEN** the transfer follows the same connected-session transfer and confirmation behavior as an observed eligible notification
- **AND** the connected session is not made unavailable merely because the transfer was initiated from the UI

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

### Requirement: Transfer diagnostics are observable

The application SHALL emit stable diagnostic log events for notification
transfer attempts so failed transfers can be debugged from captured Sender and
Receiver device logs. Diagnostic logs SHALL identify observed-event gating,
eligibility rejection, transfer start, socket write, Receiver inbound
validation, ACK, REJECT, unexpected responses, and transfer failure. Logs SHALL
NOT expose full notification text.

The Sender SHALL also present a local outbox containing recent notification
transfer attempts. Each outbox item SHALL present the source application and
captured timestamp on one visible header line, transferable title and text when
available, transfer status, and the latest diagnostic stage. When a source
application matches a simple locally known package name, presentation MAY use
the known human-readable application name while preserving transfer behavior.
The outbox SHALL update when an observed notification is accepted for transfer
preparation, rejected by eligibility, not sent, unconfirmed, rejected by
Receiver, or confirmed received. The outbox is diagnostic presentation only and
SHALL NOT create pending retry behavior. The Sender SHALL allow the user to
remove one visible outbox item using a compact right-aligned `X` icon control,
or clear all visible outbox items from the current local outbox list. Outbox
deletion SHALL NOT affect delivered Receiver inbox items, local-session state,
notification listener state, or transfer protocol behavior.

#### Scenario: Transfer diagnostics identify the failed transfer step <!-- S:F-0006-S13 -->

- **GIVEN** Sender initiates a notification transfer
- **WHEN** the transfer is confirmed, rejected, unconfirmed, or not sent
- **THEN** diagnostic logs identify the last completed transfer step
- **AND** diagnostic logs include transfer status and session state context needed for debugging
- **AND** diagnostic logs do not include full notification text

#### Scenario: Sender outbox presents recent transfer attempts <!-- S:F-0006-S14 -->

- **GIVEN** Sender is active
- **WHEN** an observed notification is accepted, rejected, not sent, unconfirmed, rejected by Receiver, or confirmed received
- **THEN** the Sender outbox presents a recent item for that notification attempt
- **AND** the item presents source application and captured timestamp on one header line
- **AND** the item presents available title and text, transfer status, and latest diagnostic stage
- **AND** the item does not imply automatic retry or guaranteed delivery

#### Scenario: Known package source is presented as a readable Sender outbox application name <!-- S:F-0006-S17 -->

- **GIVEN** Sender is active
- **AND** an outbox item source application is `com.tencent.mm`
- **WHEN** the Sender outbox item is shown
- **THEN** the outbox item presents the source application as `微信`
- **AND** the underlying transfer representation is unchanged

#### Scenario: User deletes one Sender outbox item <!-- S:F-0006-S15 -->

- **GIVEN** Sender has multiple visible outbox items
- **WHEN** the user deletes one outbox item
- **THEN** that item is removed from the current Sender outbox list
- **AND** the remaining outbox items stay visible
- **AND** local-session state and Receiver inbox contents are unchanged
- **AND** the per-item delete control is presented as a right-aligned `X` icon

#### Scenario: User clears the Sender outbox <!-- S:F-0006-S16 -->

- **GIVEN** Sender has one or more visible outbox items
- **WHEN** the user clears the Sender outbox
- **THEN** all current Sender outbox items are removed
- **AND** the empty outbox state is presented
- **AND** local-session state and transfer protocol behavior are unchanged

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: transfer confirmation, no-session disposition, inbound rejection, repeated-event handling, invalid-transfer state preservation, Sender outbox state updates and deletion, and compile-time validation of stable diagnostic log call sites.
- Two-device manual: transfer a representative eligible notification, interrupt a transfer, restore the session, and transfer a later notification.

## Dependencies

- `F-0004` Notification Eligibility and Transfer Representation
- `F-0005` Local Session Lifecycle

## Open Questions

- Automatic retry, user-initiated resend, persistence, and future duplicate-event suppression are not defined. The MVP has no pending transfer queue and no duplicate suppression.
