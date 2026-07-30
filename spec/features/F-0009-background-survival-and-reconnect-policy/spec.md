# Background Survival and Reconnect Policy Specification

## Purpose

This feature improves the chance that the local-network MVP remains usable when
Android or vendor battery policy would otherwise restrict background execution.

It defines user-visible foreground execution behavior and the visible guidance
for Android battery settings. It does not guarantee survival after Android kills
the process, does not bypass vendor power-management policy, and does not add
cloud relay behavior.

## Requirements

### Requirement: Active local sessions use foreground execution

When the user starts Sender local-session availability or Receiver establishes a
local session, the application SHALL request Android foreground-service
execution with a persistent low-distraction system notification. The foreground
notification SHALL indicate that InkHelper is running in the background for
local notification relay. The foreground notification SHALL use Simplified
Chinese app-authored copy. The foreground notification SHALL request ongoing
and non-clearable notification behavior so normal notification-shade clearing
does not dismiss the local-session foreground indicator while the foreground
service is active.

#### Scenario: Sender foreground service starts with local-session availability <!-- S:F-0009-S01 -->

- **GIVEN** Sender is active
- **WHEN** the user starts Sender local-session availability
- **THEN** the application requests foreground execution
- **AND** Android is asked to show a persistent foreground notification for Sender background operation

#### Scenario: Receiver foreground service starts after connection <!-- S:F-0009-S02 -->

- **GIVEN** Receiver is active
- **WHEN** Receiver establishes a local session
- **THEN** the application requests foreground execution
- **AND** Android is asked to show a persistent foreground notification for Receiver background operation

#### Scenario: Foreground notification uses Simplified Chinese copy <!-- S:F-0009-S03 -->

- **GIVEN** foreground execution is requested
- **WHEN** Android displays the foreground notification or its notification channel
- **THEN** app-authored title, text, channel name, and channel description are presented in Simplified Chinese

#### Scenario: Foreground notification requests non-clearable ongoing behavior <!-- S:F-0009-S07 -->

- **GIVEN** foreground execution is active for a local session
- **WHEN** Android displays the foreground notification
- **THEN** the application requests ongoing foreground-service notification behavior
- **AND** the application requests that normal notification clearing does not dismiss the notification
- **AND** the notification still opens the application when tapped

### Requirement: Foreground execution stops after explicit user exit

When the user explicitly disconnects the active local session or changes role,
the application SHALL stop foreground execution for the prior local-session
operation. Stopping foreground execution SHALL NOT delete existing Receiver
inbox items or Sender outbox items.

#### Scenario: User disconnect stops foreground execution <!-- S:F-0009-S04 -->

- **GIVEN** foreground execution is active for a local session
- **WHEN** the user disconnects the active local session
- **THEN** foreground execution is stopped
- **AND** the selected role remains active

#### Scenario: Role change stops prior foreground execution <!-- S:F-0009-S05 -->

- **GIVEN** foreground execution is active for a local session
- **WHEN** the user changes role
- **THEN** foreground execution for the prior role is stopped
- **AND** the replacement role has no connected local session until it establishes one

### Requirement: Foreground execution does not overstate reliability

The application SHALL present foreground execution and battery settings as
best-effort background survival aids. The application SHALL NOT claim guaranteed
delivery, guaranteed reconnection, or guaranteed survival under all Android or
vendor policies.

#### Scenario: Battery guidance stays best-effort <!-- S:F-0009-S06 -->

- **GIVEN** the user reviews background operation settings
- **WHEN** the application presents battery and foreground-service guidance
- **THEN** the guidance explains that foreground execution and battery settings reduce interruption risk
- **AND** the guidance does not claim absolute background survival

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: foreground-service role policy, foreground notification copy
  resource presence, non-clearable notification flag policy, and stable scenario
  traceability.
- Single-device manual: start Sender session and Receiver connection, background
  the app, and verify a persistent foreground notification is visible.
- Two-device manual: establish a session, background both devices, send an
  eligible notification, and verify the Receiver receives it while foreground
  service notifications remain visible.

## Dependencies

- `F-0005` Local Session Lifecycle
- `F-0008` Receiver Background System Notifications

## Open Questions

- Exact foreground-service type, long-duration Android timeout behavior, and
  vendor-specific setting deep links may need refinement after physical-device
  validation on Xiaomi, HyperOS, and the target e-ink device.
