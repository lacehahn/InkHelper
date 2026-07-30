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

The Receiver SHALL present each valid notification made available to it as a readable inbox item. An inbox item SHALL present its source application and captured timestamp on one visible header line, and SHALL present every available non-empty title and text field. When a source application matches a simple locally known package name, presentation MAY use the known human-readable application name while preserving transfer behavior. Presentation SHALL use only the transferable notification information and SHALL NOT require access to the Sender's Android notification objects.

#### Scenario: Received notification appears in the inbox <!-- S:F-0002-S04 -->

- **GIVEN** Receiver is active
- **AND** a valid received notification is available
- **WHEN** the inbox is shown
- **THEN** the notification is observable as an inbox item

#### Scenario: Visible Receiver inbox updates after inbound acceptance <!-- S:F-0002-S10 -->

- **GIVEN** Receiver is active
- **AND** the Receiver presentation is visible
- **WHEN** a valid inbound notification is accepted by the Receiver
- **THEN** the Receiver presentation updates so the notification is observable as an inbox item
- **AND** the user does not need to switch roles or manually refresh before the accepted notification can appear

#### Scenario: Inbox item presents the minimum notification content <!-- S:F-0002-S06 -->

- **GIVEN** Receiver is active
- **AND** a valid notification has a source application, captured timestamp, title, and text
- **WHEN** the inbox is shown
- **THEN** the inbox item presents the source application and captured timestamp on one header line
- **AND** the inbox item presents the title and text

#### Scenario: Known package source is presented as a readable application name <!-- S:F-0002-S15 -->

- **GIVEN** Receiver is active
- **AND** a valid notification source application is `com.tencent.mm`
- **WHEN** the inbox item is shown
- **THEN** the inbox item presents the source application as `微信`
- **AND** the underlying transfer representation is unchanged

#### Scenario: Malformed notification is absent from the inbox <!-- S:F-0002-S05 -->

- **GIVEN** Receiver is active
- **AND** inbound notification information was rejected by the transfer feature
- **WHEN** the inbox is shown
- **THEN** the rejected information is not an inbox item
- **AND** previously available inbox items remain available

### Requirement: The Receiver supports local inbox deletion

The Receiver SHALL allow the user to remove a single visible inbox item from
the current Receiver inbox list using a compact right-aligned `X` icon control.
The Receiver SHALL allow the user to clear all visible inbox items from the
current Receiver inbox list. These actions SHALL affect only local presentation
state and SHALL NOT send deletion commands to the Sender, change local-session
state, revoke permissions, or imply deletion from any external application.

#### Scenario: User deletes one Receiver inbox item <!-- S:F-0002-S13 -->

- **GIVEN** Receiver has multiple visible inbox items
- **WHEN** the user deletes one inbox item
- **THEN** that item is removed from the current Receiver inbox list
- **AND** the remaining inbox items stay visible
- **AND** the local session state is unchanged
- **AND** the per-item delete control is presented as a right-aligned `X` icon

#### Scenario: User clears the Receiver inbox <!-- S:F-0002-S14 -->

- **GIVEN** Receiver has one or more visible inbox items
- **WHEN** the user clears the Receiver inbox
- **THEN** all current Receiver inbox items are removed
- **AND** the empty inbox state is presented
- **AND** the local session state is unchanged

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

### Requirement: The presentation is optimized for an e-ink reading device

The application SHALL use a high-contrast black-and-white presentation with a
white primary background, black readable text, visible boundaries for grouped
controls, and minimal motion. The presentation SHALL keep role, session,
pairing, and inbox controls visually distinct without relying on color hue alone.
Primary user-facing application copy SHALL be presented in Simplified Chinese.
Protocol tokens, package names, debug logs, and externally supplied notification
content MAY remain in their source language.

#### Scenario: E-ink-oriented visual presentation is shown <!-- S:F-0002-S09 -->

- **GIVEN** the application is displayed on an Android device
- **WHEN** the main application presentation is shown
- **THEN** the presentation uses a white primary background and black foreground content
- **AND** grouped controls and inbox items have visible black or grayscale boundaries
- **AND** role, session, pairing, and inbox information remain distinguishable without color hue

#### Scenario: Main presentation keeps configuration out of the primary workflow <!-- S:F-0002-S11 -->

- **GIVEN** the main application presentation is shown
- **WHEN** the user is operating Sender or Receiver
- **THEN** the primary presentation emphasizes role, connection, disconnection, Receiver pairing, and Receiver inbox
- **AND** lower-frequency configuration and diagnostic actions are available from a settings menu
- **AND** configuration and diagnostic actions do not dominate the primary presentation

#### Scenario: Primary application copy is shown in Simplified Chinese <!-- S:F-0002-S12 -->

- **GIVEN** the main application presentation is shown
- **WHEN** the user reads role, session, pairing, inbox, settings, and action labels
- **THEN** app-authored labels and status messages are presented in Simplified Chinese
- **AND** protocol tokens, package names, debug logs, and transferred notification content are not translated by presentation behavior

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: empty versus non-empty presentation, minimum content, local
  representative input, inbox-change notification, simplified-presentation and
  Simplified Chinese copy behavior where practical, rejection preservation, and
  e-ink presentation constants where practical.
- Single-device manual: Receiver role, representative inbox presentation, and black-and-white visual grouping.
- Target e-ink validation: readability, contrast, tap target clarity, and low-motion presentation on the target device.

## Dependencies

- `F-0001` Runtime Role Selection

## Open Questions

- Inbox ordering, persistence across application restart, read state, grouping, and deduplication are not defined. Each affects observable inbox behavior and requires a future feature specification before implementation.
