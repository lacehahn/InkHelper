# Receiver Background System Notifications Specification

## Purpose

This feature allows Receiver to continue receiving through an established local
session while the main application UI is not visible, and to surface accepted
inbound notification information through Android system notifications.

It defines Receiver-side background presentation only. It does not define cloud
delivery, guaranteed process survival after Android kills the application
process, reply actions, notification grouping, or full mirroring of the
Sender's original Android notification.

## Requirements

### Requirement: Receiver session is independent of the visible UI

After Receiver establishes a local session, hiding or backgrounding the main
application UI SHALL NOT intentionally disconnect the Receiver session. The
application SHALL keep Receiver runtime support active through a background
component while Receiver remains the selected role. Receiver SHALL continue to
accept valid inbound notification information while the application process
remains alive and the user has not explicitly disconnected or changed role.

#### Scenario: Receiver keeps an established session while backgrounded <!-- S:F-0008-S01 -->

- **GIVEN** Receiver is active
- **AND** Receiver has an established local session
- **WHEN** the main application UI is no longer visible
- **THEN** the Receiver session remains available for inbound notification information
- **AND** the session is not disconnected merely because the UI is backgrounded

### Requirement: Accepted inbound notifications appear as Android system notifications

When Receiver accepts valid inbound notification information, the application
SHALL request presentation through Android's system notification surface. The
system notification SHALL include readable notification content derived from
the accepted transfer representation, including source application and any
available non-empty title or text. The notification SHALL use an application
notification channel suitable for new message alerts. App-authored notification
channel text SHALL be presented in Simplified Chinese.

#### Scenario: Accepted inbound notification posts a system notification <!-- S:F-0008-S02 -->

- **GIVEN** Receiver is active
- **AND** Receiver accepts a valid inbound notification
- **AND** Android notification permission is available
- **WHEN** the inbound notification is accepted
- **THEN** Android is asked to show a system notification for that message
- **AND** the notification content includes the source application and readable title or text

#### Scenario: Missing notification permission does not reject inbound notification <!-- S:F-0008-S03 -->

- **GIVEN** Receiver is active
- **AND** Receiver accepts a valid inbound notification
- **AND** Android notification permission is unavailable
- **WHEN** the inbound notification is accepted
- **THEN** the notification remains available in the Receiver inbox
- **AND** the inbound transfer is not rejected merely because the system notification cannot be posted

#### Scenario: Receiver notification channel copy is shown in Simplified Chinese <!-- S:F-0008-S05 -->

- **GIVEN** Receiver system notifications are available
- **WHEN** Android creates or displays the application notification channel
- **THEN** app-authored channel name and description are presented in Simplified Chinese

### Requirement: Tapping the system notification returns to the Receiver application

When the user taps a Receiver system notification, Android SHALL open the
application so the Receiver inbox can be viewed.

#### Scenario: System notification opens the Receiver inbox application <!-- S:F-0008-S04 -->

- **GIVEN** Android has shown a Receiver system notification
- **WHEN** the user taps that system notification
- **THEN** Android opens the application
- **AND** the accepted notification remains available through the Receiver inbox

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: system-notification content formatting, Simplified Chinese channel
  copy, notification presenter invocation after accepted inbound messages,
  missing-presenter preservation of inbox acceptance, and stable scenario
  traceability.
- Single-device manual: grant notification permission, run Receiver, accept a
  representative inbound notification, background the app, and verify Android
  status bar and notification shade display message content.
- Two-device manual: connect Sender and Receiver, background Receiver, send an
  eligible notification from Sender, and verify Receiver system notification
  and inbox visibility.

## Dependencies

- `F-0002` Receiver Inbox and Operational Presentation
- `F-0005` Local Session Lifecycle
- `F-0006` Notification Transfer and Inbound Validation

## Open Questions

- Persistent foreground service behavior, notification grouping, read state,
  dismissal synchronization, notification actions, and exact heads-up behavior
  are not defined by this MVP feature.
