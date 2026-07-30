# Local Network Discovery and Pairing Specification

## Purpose

The application supports same-network discovery so a Receiver can find a
reachable Sender without manually typing the Sender address and session code.

This feature defines discovery and pairing selection only. It does not replace
the local-session lifecycle, transfer validation, notification transfer, or
manual address entry behavior.

Discovery is limited to the local network. It SHALL NOT require a cloud relay,
internet-wide directory, account, or hosted coordination service.

## Requirements

### Requirement: Sender advertises a current pairing candidate

When Sender is active and a local-session attempt has current session details,
the application SHALL make that Sender discoverable on the local network as a
pairing candidate. The candidate SHALL contain enough information for a Receiver
to begin the existing local-session establishment flow: Sender address, session
port, and current session code. Sender SHALL NOT advertise as discoverable when
Sender is inactive or no current local-session attempt exists.

#### Scenario: Active Sender is discoverable <!-- S:F-0007-S01 -->

- **GIVEN** Sender is active
- **AND** a current local-session attempt has Sender address, session port, and session code
- **WHEN** a Receiver scans the same local network
- **THEN** the Sender is observable as a pairing candidate
- **AND** the candidate contains Sender address, session port, and current session code

#### Scenario: Sender without session details is not discoverable <!-- S:F-0007-S02 -->

- **GIVEN** Sender is active
- **AND** no current local-session attempt exists
- **WHEN** a Receiver scans the same local network
- **THEN** that Sender is not returned as a pairing candidate

#### Scenario: Inactive Sender is not discoverable <!-- S:F-0007-S03 -->

- **GIVEN** Sender is inactive
- **WHEN** a Receiver scans the same local network
- **THEN** that device is not returned as a Sender pairing candidate

### Requirement: Receiver scans for local pairing candidates

When Receiver is active, the application SHALL allow the user to scan the local
network for discoverable Senders. Scanning SHALL expose discovered candidates
and SHALL preserve the active role and current local-session state when no
candidate is found.

Receiver scanning SHALL NOT depend only on a single global broadcast address.
When no explicit scan target is configured, Receiver SHALL attempt discovery
across practical local-network targets, including the global broadcast address,
available interface broadcast addresses, and bounded same-subnet unicast probe
targets. Duplicate responses SHALL be collapsed to one candidate.

#### Scenario: Receiver finds a discoverable Sender <!-- S:F-0007-S04 -->

- **GIVEN** Receiver is active
- **AND** a discoverable Sender is reachable on the same local network
- **WHEN** the user starts scanning
- **THEN** the Sender is observable as a discovered pairing candidate
- **AND** Receiver does not present the session as connected until pairing is selected and local-session establishment completes

#### Scenario: Scan without candidates preserves Receiver state <!-- S:F-0007-S05 -->

- **GIVEN** Receiver is active
- **AND** no discoverable Sender is reachable on the same local network
- **WHEN** the user starts scanning
- **THEN** no pairing candidate is returned
- **AND** Receiver remains active
- **AND** the local session is not presented as connected

#### Scenario: Inactive Receiver does not scan <!-- S:F-0007-S06 -->

- **GIVEN** Receiver is inactive
- **WHEN** the user attempts to scan for local pairing candidates
- **THEN** no scan result is accepted
- **AND** the active role remains unchanged

#### Scenario: Receiver scans multiple local-network targets <!-- S:F-0007-S10 -->

- **GIVEN** Receiver is active
- **AND** Sender discovery packets may not reach peers through the global broadcast address
- **WHEN** the user starts scanning
- **THEN** Receiver sends discovery requests to multiple practical local-network targets
- **AND** duplicate candidate responses are exposed as one discovered Sender
- **AND** scan diagnostics identify the number of targets used

### Requirement: Selecting a discovered Sender starts local-session establishment

When Receiver is active and the user selects a discovered Sender pairing
candidate, the Receiver SHALL use the candidate's address, session port, and
session code to begin the existing local-session establishment behavior defined
by `F-0005`. A rejected, expired, or unreachable candidate SHALL leave the active
role unchanged and SHALL NOT present the local session as connected.

#### Scenario: Receiver pairs with a discovered Sender <!-- S:F-0007-S07 -->

- **GIVEN** Receiver is active
- **AND** a discovered Sender candidate contains current matching session details
- **WHEN** the user selects that candidate
- **THEN** Receiver begins local-session establishment with that Sender
- **AND** the session becomes connected only if the Sender accepts the same session details

#### Scenario: Expired discovered candidate is rejected <!-- S:F-0007-S08 -->

- **GIVEN** Receiver is active
- **AND** a discovered Sender candidate is no longer current or reachable
- **WHEN** the user selects that candidate
- **THEN** Receiver does not present the session as connected
- **AND** Receiver remains active

### Requirement: Discovery and pairing diagnostics are observable

When Receiver scans for Sender candidates or selects a discovered candidate,
the application SHALL emit stable diagnostic log events that identify the scan
start, discovery request, candidate response, ignored response, timeout or
failure, candidate selection, and resulting connection attempt handoff. Logs
SHALL NOT expose the full session code.

#### Scenario: Discovery diagnostics identify the pairing step <!-- S:F-0007-S09 -->

- **GIVEN** Receiver is active
- **WHEN** the user scans for local pairing candidates or selects a discovered candidate
- **THEN** diagnostic logs identify whether scanning started, a request was sent, a candidate was received, the candidate was selected, and Receiver began local-session establishment
- **AND** diagnostic logs include network address and port context needed for debugging
- **AND** diagnostic logs do not include the full session code

## Scenarios

Scenario identifiers are defined under their owning requirements above.

## Validation

- Automated: discovery message parsing, Sender discoverability gating, Receiver
  scan result handling, empty scans, bounded multi-target scan calculation,
  pairing through discovered candidates, and compile-time validation of stable
  diagnostic log call sites.
- Two-device manual: place two Android devices on the same local network, start
  Sender local-session establishment, scan from Receiver, select the discovered
  Sender, and verify the local session becomes connected.

## Dependencies

- `F-0001` Runtime Role Selection
- `F-0005` Local Session Lifecycle

## Open Questions

- Multiple simultaneous discoverable Senders, candidate ordering, automatic
  refresh, and remembered paired devices are not defined by this feature.
