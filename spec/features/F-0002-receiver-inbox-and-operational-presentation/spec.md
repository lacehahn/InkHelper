# Receiver Inbox and Operational Presentation Specification

## Purpose

The Receiver presents valid notification information as a readable inbox and exposes the operational information needed to understand Receiver activity. This feature defines presentation behavior only; it does not define notification capture, transport, transfer validation, storage technology, or message ordering policy.

## Requirements

### Requirement: The Receiver presents its operational state

When Receiver is the active role, the application SHALL present the Receiver role as active and SHALL present the currently available operational state supplied by Receiver-owned capabilities. The presentation SHALL distinguish an empty inbox from an unavailable or failed operational condition when those conditions are available.

#### Scenario: Receiver role is presented as active <!-- S:F-0002-S01 -->

- **GIVEN** Receiver is the active role
- **WHEN** the Receiver presentation is shown
- **THEN** the active Receiver role is observable

#### Scenario: Unavailable operational state is distinct from an empty inbox <!-- S:F-0002-S02 -->

- **GIVEN** Receiver is active
- **AND** an owning capability reports an unavailable or failed operational condition
- **WHEN** the Receiver presentation is shown
- **THEN** that condition is observable
- **AND** it is not presented as evidence that the inbox contains no notifications

### Requirement: The Receiver presents an empty inbox

The Receiver SHALL present an empty inbox state when no valid received notifications are available for presentation.

#### Scenario: Empty inbox is presented <!-- S:F-0002-S03 -->

- **GIVEN** Receiver is active
- **AND** no valid received notifications are available
- **WHEN** the inbox is shown
- **THEN** an empty inbox state is observable

### Requirement: The Receiver presents valid notification information

The Receiver SHALL present each valid notification made available to it as a readable inbox item. An inbox item SHALL present its source application and captured timestamp, and SHALL present every available non-empty title and text field. Presentation SHALL use only the transferable notification information and SHALL NOT require access to the Sender's Android notification objects.

#### Scenario: Received notification appears in the inbox <!-- S:F-0002-S04 -->

- **GIVEN** Receiver is active
- **AND** a valid received notification is available
- **WHEN** the inbox is shown
- **THEN** the notification is observable as an inbox item

#### Scenario: Inbox item presents the minimum notification content <!-- S:F-0002-S06 -->

- **GIVEN** Receiver is active
- **AND** a valid notification has a source application, captured timestamp, title, and text
- **WHEN** the inbox is shown
- **THEN** the inbox item presents the source application and captured timestamp
- **AND** the inbox item presents the title and text

#### Scenario: Malformed notification is absent from the inbox <!-- S:F-0002-S05 -->

- **GIVEN** Receiver is active
- **AND** inbound notification information was rejected by the transfer feature
- **WHEN** the inbox is shown
- **THEN** the rejected information is not an inbox item
- **AND** previously available inbox items remain available

### Requirement: The Receiver supports representative local inbox input before transfer is available

Before a local session and live transfer are available, the Receiver SHALL be able to present representative notification information that conforms to the transfer representation. Representative local input SHALL NOT cause the Receiver to present a local session as connected.

#### Scenario: Representative local notification is presented without a session <!-- S:F-0002-S07 -->

- **GIVEN** Receiver is active
- **AND** no local session is connected
- **AND** representative notification information conforms to the transfer representation
- **WHEN** the information is made available to the Receiver
- **THEN** it is presented as an inbox item
- **AND** the Receiver does not present a local session as connected

#### Scenario: Inbox remains available after Receiver deactivation <!-- S:F-0002-S08 -->

- **GIVEN** Receiver has existing inbox items
- **WHEN** the user switches from Receiver to Sender
- **THEN** Receiver becomes inactive
- **AND** the existing inbox items are not deleted by the role switch

## Validation

- Automated: empty versus non-empty presentation, minimum content, local representative input, and rejection preservation.
- Single-device manual: Receiver role and representative inbox presentation.
- Target e-ink validation: readability, contrast, and low-motion presentation on the target device.

## Dependencies

- `F-0001` Runtime Role Selection

## Open Questions

- Inbox ordering, persistence across application restart, read state, deletion, grouping, and deduplication are not defined. Each affects observable inbox behavior and requires a future feature specification before implementation.
