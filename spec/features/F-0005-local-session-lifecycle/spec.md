# Local Session Lifecycle Specification

## Purpose

This feature establishes and exposes the local communication session between one Sender and one Receiver. It defines session availability and recovery boundaries, not notification payload transfer.

## Requirements

### Requirement: The Sender provides local-session details and the Receiver enters them

When Sender is active, the application SHALL present the Sender's current local address and a session code for the current local-session attempt. When Receiver is active, the application SHALL allow the user to enter a Sender local address and session code. The session code SHALL be used only for the current session attempt and SHALL NOT establish a session by itself without the entered address.

#### Scenario: Sender presents session details <!-- S:F-0005-S07 -->

- **GIVEN** Sender is active
- **WHEN** the user starts local-session establishment
- **THEN** the Sender local address and current session code are observable

#### Scenario: Receiver enters matching session details <!-- S:F-0005-S08 -->

- **GIVEN** Receiver is active
- **AND** a reachable Sender presents a current local address and session code
- **WHEN** the user enters that address and code
- **THEN** Receiver begins local-session establishment with that Sender

#### Scenario: Sender presents the best available local-network address <!-- S:F-0005-S15 -->

- **GIVEN** Sender is active
- **AND** the Sender device has multiple non-loopback IPv4 interfaces
- **WHEN** the user starts local-session establishment
- **THEN** Sender presents the address most likely to be reachable on the local network
- **AND** emulator-only NAT loopback-style addresses are not preferred over available Wi-Fi-style local addresses

#### Scenario: Missing or incorrect session details are rejected <!-- S:F-0005-S09 -->

- **GIVEN** Receiver is active
- **WHEN** the user enters a missing or incorrect Sender local address or session code
- **THEN** Receiver does not present the session as connected
- **AND** the active role remains unchanged

### Requirement: A local session is limited to one Sender and one Receiver

The application SHALL establish local communication only between one active Sender and one active Receiver on a reachable local network. A role SHALL NOT report a connected local session until the other role is available for that same session.

#### Scenario: Sender and Receiver establish a local session <!-- S:F-0005-S01 -->

- **GIVEN** one device has Sender active
- **AND** Receiver entered the Sender's current local address and session code
- **WHEN** local-session establishment completes
- **THEN** both roles present the session as connected

#### Scenario: Session is not connected without a reachable peer <!-- S:F-0005-S02 -->

- **GIVEN** one role is active
- **AND** no reachable counterpart role is available
- **WHEN** local-session establishment is attempted
- **THEN** the role does not present the session as connected
- **AND** the active role remains unchanged

#### Scenario: A second counterpart does not replace an active session <!-- S:F-0005-S03 -->

- **GIVEN** Sender and Receiver already present the same local session as connected
- **WHEN** another potential counterpart attempts to establish a session
- **THEN** the existing connected session remains the active session
- **AND** the attempt does not create a second active counterpart session

#### Scenario: Repeating Receiver pairing while connected does not replace the active session <!-- S:F-0005-S12 -->

- **GIVEN** Sender and Receiver already present the same local session as connected
- **WHEN** the connected Receiver repeats local-session establishment with a manual address or discovered candidate
- **THEN** Receiver remains connected to the existing local session
- **AND** Sender does not replace the active session
- **AND** Receiver does not present the repeated attempt as unavailable

### Requirement: Local-session state is observable

The application SHALL expose whether the active role is attempting to establish a session, connected, disconnected, or unable to establish the requested session. A session failure SHALL remain attributable to local-session behavior and SHALL NOT change the selected role.

#### Scenario: Connection attempt is observable <!-- S:F-0005-S04 -->

- **GIVEN** an active role has no connected session
- **WHEN** local-session establishment begins
- **THEN** the attempt is observable as in progress

#### Scenario: Connection loss is observable <!-- S:F-0005-S05 -->

- **GIVEN** the local session is connected
- **WHEN** the local connection is interrupted
- **THEN** the session is observable as disconnected or unavailable
- **AND** the selected role remains active

#### Scenario: Idle connected session remains available for later transfer <!-- S:F-0005-S13 -->

- **GIVEN** Sender and Receiver already present the same local session as connected
- **WHEN** no notification transfer arrives for a short idle period
- **THEN** Receiver keeps the local session connected
- **AND** a later notification transfer can still be received through that session

### Requirement: The user can attempt recovery after an unavailable session

After a failed or interrupted local session, the application SHALL allow another session-establishment attempt. A later successful attempt SHALL replace the unavailable session state with connected state without activating another role.

#### Scenario: User retries after session failure <!-- S:F-0005-S06 -->

- **GIVEN** an active role presents the local session as unavailable
- **WHEN** the user initiates another establishment attempt and a reachable counterpart is available
- **THEN** the session becomes connected
- **AND** the active role remains unchanged

#### Scenario: Role deactivation disconnects its local session <!-- S:F-0005-S10 -->

- **GIVEN** an active role presents the local session as connected
- **WHEN** the user switches to the other role
- **THEN** the prior role's session becomes disconnected
- **AND** the replacement role has no connected session until it establishes one

#### Scenario: User disconnects the active local session <!-- S:F-0005-S14 -->

- **GIVEN** an active role has a local session attempt or connected local session
- **WHEN** the user disconnects from the primary presentation
- **THEN** the active role's local session becomes disconnected
- **AND** the selected role remains active

### Requirement: Local-session diagnostics are observable

The application SHALL emit stable diagnostic log events for local-session
attempts so failed manual or discovered pairing attempts can be debugged from
the next captured device logs. Diagnostic logs SHALL identify connection start,
socket connection, hello send, Sender accept, Sender rejection reason, Receiver
rejection response, successful connection, session stop, and unavailable state
transitions. Logs SHALL NOT expose the full session code.

#### Scenario: Connection diagnostics identify unavailable session causes <!-- S:F-0005-S11 -->

- **GIVEN** an active role attempts local-session establishment
- **WHEN** the session becomes connected, rejected, interrupted, or unavailable
- **THEN** diagnostic logs identify the last completed connection step
- **AND** diagnostic logs include address, port, state, and rejection context needed for debugging
- **AND** diagnostic logs do not include the full session code

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: session-state transitions, local address selection, one-counterpart
  constraints where platform-independent behavior can be isolated, and
  compile-time validation of stable diagnostic log call sites.
- Two-device manual: establish, interrupt, and re-establish a session on the intended local network without internet access.

## Dependencies

- `F-0001` Runtime Role Selection

## Open Questions

- Automatic reconnect and session timeout behavior are not defined. The MVP uses the displayed local address and current session code as its establishment path and session identity.
