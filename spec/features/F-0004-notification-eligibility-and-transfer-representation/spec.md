# Notification Eligibility and Transfer Representation Specification

## Purpose

This feature evaluates observed Sender notification events for relay eligibility and prepares accepted events for cross-device transfer. It owns neither Android observation nor local-session transport.

## Requirements

### Requirement: The MVP uses a defined eligibility baseline

The Sender SHALL evaluate each observed notification event against the MVP eligibility baseline. The baseline SHALL accept an event only when its source application is available, its captured timestamp is available, and at least one of its title or text is non-empty. The MVP SHALL NOT apply application source filtering, package allowlisting, notification category filtering, or any other user-configured filter to eligibility. The MVP SHALL NOT require additional filtering-related Android permissions. An event rejected by the baseline SHALL NOT be offered for transfer.

#### Scenario: Eligible notification is accepted <!-- S:F-0004-S01 -->

- **GIVEN** Sender provides an observed notification event
- **AND** the event has a source application, captured timestamp, and non-empty title or text
- **WHEN** eligibility is evaluated
- **THEN** the event is accepted for transfer preparation

#### Scenario: Ineligible notification is rejected <!-- S:F-0004-S02 -->

- **GIVEN** Sender provides an observed notification event
- **AND** the event has no source application, no captured timestamp, or no readable title or text
- **WHEN** eligibility is evaluated
- **THEN** the event is rejected
- **AND** no transferable notification is produced

#### Scenario: Rejected notification preserves unrelated transfer work <!-- S:F-0004-S03 -->

- **GIVEN** a previously accepted notification is available for transfer
- **AND** Sender provides an event rejected by the MVP eligibility baseline
- **WHEN** eligibility is evaluated
- **THEN** the new event is rejected
- **AND** the previously accepted notification remains unchanged

#### Scenario: Configured source list does not exclude a source application <!-- S:F-0004-S06 -->

- **GIVEN** Sender provides an observed notification event with required readable information
- **AND** a user-configured source list does not contain the event's source application
- **WHEN** eligibility is evaluated
- **THEN** the event is accepted
- **AND** a transferable notification is produced

### Requirement: The MVP defers application allowlist filtering

The application MAY retain internal application allowlist data for future work, but the MVP UI SHALL NOT ask the user to configure notification source filters. Changing any retained allowlist data SHALL NOT affect eligibility evaluation of later observed events and SHALL NOT alter a notification already accepted for transfer.

#### Scenario: Existing source-list data is ignored for later eligibility <!-- S:F-0004-S08 -->

- **GIVEN** retained source-list data contains one source application
- **WHEN** a later observed event from a different source application is evaluated
- **THEN** the later event is not rejected by source-list data

#### Scenario: Source-list changes preserve previously accepted notifications <!-- S:F-0004-S09 -->

- **GIVEN** a notification has already been accepted for transfer
- **WHEN** retained source-list data changes
- **THEN** the previously accepted notification remains unchanged
- **AND** later observed events are still evaluated only by the MVP eligibility baseline

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

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: accepted and rejected eligibility evaluation, representation preparation, and preservation of unrelated accepted work.
- Single-device manual: observe representative accessible notifications and verify notifications with required transferable fields are prepared without source filtering.

## Dependencies

- `F-0003` Sender Notification Access and Observation

## Open Questions

- Stable notification identity is not defined. The MVP does not require identity-based duplicate suppression.
