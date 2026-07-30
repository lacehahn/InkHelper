# Notification Eligibility and Transfer Representation Specification

## Purpose

This feature evaluates observed Sender notification events for relay eligibility and prepares accepted events for cross-device transfer. It owns neither Android observation nor local-session transport.

## Requirements

### Requirement: The MVP uses a defined eligibility baseline

The Sender SHALL evaluate each observed notification event against the MVP eligibility baseline. The baseline SHALL accept an event only when its source application is available, its captured timestamp is available, and at least one of its title or text is non-empty. An empty application allowlist SHALL accept events from every source application that satisfies those conditions. A non-empty application allowlist SHALL accept an event only when its source application appears in that allowlist. An event rejected by the baseline SHALL NOT be offered for transfer.

#### Scenario: Eligible notification is accepted <!-- S:F-0004-S01 -->

- **GIVEN** Sender provides an observed notification event
- **AND** the event has a source application, captured timestamp, and non-empty title or text
- **AND** the application allowlist is empty or contains the source application
- **WHEN** eligibility is evaluated
- **THEN** the event is accepted for transfer preparation

#### Scenario: Ineligible notification is rejected <!-- S:F-0004-S02 -->

- **GIVEN** Sender provides an observed notification event
- **AND** the event has no source application, no captured timestamp, no readable title or text, or a source application excluded by a non-empty allowlist
- **WHEN** eligibility is evaluated
- **THEN** the event is rejected
- **AND** no transferable notification is produced

#### Scenario: Rejected notification preserves unrelated transfer work <!-- S:F-0004-S03 -->

- **GIVEN** a previously accepted notification is available for transfer
- **AND** Sender provides an event rejected by the MVP eligibility baseline
- **WHEN** eligibility is evaluated
- **THEN** the new event is rejected
- **AND** the previously accepted notification remains unchanged

#### Scenario: Non-empty allowlist excludes a source application <!-- S:F-0004-S06 -->

- **GIVEN** Sender provides an observed notification event with required readable information
- **AND** the application allowlist does not contain the event's source application
- **WHEN** eligibility is evaluated
- **THEN** the event is rejected
- **AND** no transferable notification is produced

### Requirement: The user manages the application allowlist

The application SHALL start with an empty application allowlist. The user SHALL be able to add a source application to, or remove a source application from, the allowlist. Changing the allowlist SHALL affect eligibility evaluation of later observed events and SHALL NOT alter a notification already accepted for transfer.

#### Scenario: User adds a source application to the allowlist <!-- S:F-0004-S08 -->

- **GIVEN** the application allowlist does not contain a source application
- **WHEN** the user adds that source application to the allowlist
- **THEN** later observed events from that source application satisfy the allowlist condition

#### Scenario: User removes a source application from the allowlist <!-- S:F-0004-S09 -->

- **GIVEN** the application allowlist contains a source application
- **WHEN** the user removes that source application from the allowlist
- **THEN** later observed events from that source application fail the allowlist condition
- **AND** previously accepted notifications remain unchanged

### Requirement: Accepted notifications use the MVP transfer representation

The Sender SHALL prepare accepted notifications as a transferable representation containing source application, captured timestamp, title, and text. Source application and captured timestamp SHALL be present. Title and text MAY individually be empty, but SHALL NOT both be empty. The MVP SHALL transfer no other notification content, including images, actions, replies, attachments, custom layouts, or Android framework objects. The MVP applies no additional redaction to these fields.

#### Scenario: Accepted notification is prepared for transfer <!-- S:F-0004-S04 -->

- **GIVEN** an observed notification event is accepted
- **WHEN** transfer preparation occurs
- **THEN** a transferable notification representation is available
- **AND** it is independent of the original Android notification object

#### Scenario: Transfer representation contains the required fields <!-- S:F-0004-S07 -->

- **GIVEN** an observed notification event is accepted
- **WHEN** transfer preparation occurs
- **THEN** the transferable representation contains source application and captured timestamp
- **AND** it contains title, text, or both
- **AND** it contains no rich notification content

#### Scenario: Notification without required transferable information is rejected <!-- S:F-0004-S05 -->

- **GIVEN** an observed notification event is evaluated
- **AND** required information for the MVP transfer representation is unavailable
- **WHEN** transfer preparation occurs
- **THEN** no transferable notification is produced
- **AND** the event is not offered for transfer

## Validation

- Automated: accepted and rejected eligibility evaluation, representation preparation, and preservation of unrelated accepted work.
- Single-device manual: observe representative accessible notifications and verify only configured eligible notifications are prepared.

## Dependencies

- `F-0003` Sender Notification Access and Observation

## Open Questions

- Stable notification identity is not defined. The MVP does not require identity-based duplicate suppression.
