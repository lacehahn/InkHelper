# Product Harness

## Purpose

This document defines the stable product boundary for the repository.

It describes:

- what product is being built
- why the product exists
- the primary runtime roles
- the high-level runtime flow
- the MVP boundary
- explicit non-goals

This document does not define detailed feature behavior.

Detailed behavior belongs in feature specifications under:

```text
spec/features/
```

Architectural structure belongs in:

```text
spec/ARCHITECTURE.md
```

Implementation practices belong in:

```text
spec/STYLE.md
```

---

## Product Summary

This repository contains a personal Android research project for relaying
notifications between two Android devices over a local network.

The project produces a single Android application that can operate in one of
two roles:

- Sender
- Receiver

The Sender runs on the user's primary Android phone.

The Receiver runs on a secondary Android phone, with an e-ink Android device as
the primary target.

The Sender observes eligible Android notifications and transfers notification
information to the Receiver over the local network.

The Receiver receives transferred notifications and presents them as a readable
inbox.

The initial product is a local-network MVP intended for personal use and
research.

---

## Motivation

Some Android devices are better suited for communication and application
compatibility, while other devices are better suited for focused reading.

The project explores whether notifications from a primary Android phone can be
made available on a secondary reading-oriented device without depending on a
cloud service.

The intended user experience is:

1. The primary phone continues to receive normal Android notifications.
2. The application running as Sender observes eligible notifications.
3. The Sender transfers them over a local network.
4. The application running as Receiver displays them in a readable inbox.
5. The user reads notifications on the secondary device.

The project prioritizes:

- simplicity
- local operation
- inspectable behavior
- incremental development
- practical two-device validation

---

## Product Form

The product is one Android application distributed as one APK.

The same application supports both runtime roles.

A device operates in one active role at a time:

- Sender
- Receiver

The application is not initially split into separate Sender and Receiver APKs.

The application is not initially intended to operate both roles concurrently on
the same device.

Detailed role-selection and role-transition behavior must be defined in a
feature specification.

---

## Runtime Roles

### Sender

The Sender runs on the primary Android phone.

Its high-level responsibilities are:

- obtain the Android access required to observe notifications
- capture eligible notification events
- apply configured filtering behavior
- prepare transferable notification data
- establish or join the local communication session
- transfer notification data to the Receiver
- expose enough operational state for the user to understand whether sending is
  active

The Sender is not responsible for presenting the primary notification inbox.

Detailed capture, filtering, session, and transfer behavior belongs in separate
feature specifications.

---

### Receiver

The Receiver runs on the secondary Android phone.

Its high-level responsibilities are:

- establish or join the local communication session
- receive notification data from the Sender
- retain the notification information required by the MVP
- present received notifications as a readable inbox
- expose enough operational state for the user to understand whether receiving
  is active

The Receiver is optimized for reading rather than reproducing the complete
interaction model of the original application notification.

Detailed inbox, storage, ordering, and presentation behavior belongs in
separate feature specifications.

---

## Primary Runtime Flow

The high-level runtime flow is:

```text
Capture
    ↓
Filter
    ↓
Transfer
    ↓
Receive
    ↓
Present
```

In role terms:

```text
Primary Android Phone
    ↓
Sender
    ↓
Local Network
    ↓
Receiver
    ↓
Secondary Android Phone
```

This flow defines the product boundary.

It does not prescribe implementation classes, transport protocols, persistence
technology, UI framework structure, or internal module organization.

---

## Local-Network Model

The MVP operates over a local network reachable by both Android devices.

The expected initial usage model is:

1. The primary phone provides or joins a local network.
2. The secondary phone joins the same network.
3. Sender and Receiver establish a local session.
4. Notification data is transferred without a cloud relay.

The primary research scenario uses the primary phone's hotspot, but the product
boundary is local-network communication rather than one specific hotspot API or
network topology.

Detailed discovery, addressing, pairing, connection, and transport behavior
belongs in feature specifications and architectural decisions.

---

## Target Devices

### Primary Device

The primary device is a conventional Android phone used for normal daily
applications and notifications.

It runs the application in Sender role.

### Secondary Device

The secondary device is an Android phone used primarily for reading.

An e-ink Android phone is the primary target.

It runs the application in Receiver role.

The MVP should remain usable on a conventional Android display, but
presentation decisions should account for e-ink constraints where practical.

Detailed visual and interaction rules belong in feature specifications and
style guidance.

---

## MVP Goals

The MVP should demonstrate that:

1. One APK can operate as either Sender or Receiver.
2. The Sender can observe eligible Android notifications.
3. The Sender can transfer notification information over a local network.
4. The Receiver can receive transferred notification information.
5. The Receiver can present received notifications in a readable inbox.
6. The complete flow can be validated using two physical Android devices.
7. The core flow can operate without a cloud service or internet relay.

The MVP is complete when the basic end-to-end flow works reliably enough for
personal research and repeated manual use.

Detailed completion criteria must be defined by the roadmap and feature
specifications.

---

## Product Principles

### Local First

The core runtime flow should operate within the local network.

The MVP must not require a cloud relay for notification delivery.

### One Product, Two Roles

Sender and Receiver belong to one product and one Android application.

Shared concepts should remain consistent between roles.

### Readability First

Receiver presentation should prioritize reading and scanning notifications.

It does not need to reproduce the original application's complete notification
UI.

### Smallest Complete Capability

The project should evolve through small, independently verifiable features.

New behavior must be specified before it becomes an implementation dependency.

### Research Before Generalization

The project should validate the two-device personal-use workflow before adding
general-purpose platform features.

---

## Product Boundaries

The MVP includes:

- Android as the only supported platform
- one Android application
- Sender role
- Receiver role
- Android notification observation on the Sender
- notification filtering
- local-network session establishment
- local notification transfer
- notification reception
- readable Receiver inbox
- personal two-device use
- manual validation on physical devices

The MVP may include lightweight configuration required to make the core flow
usable.

Any such behavior must be defined in a feature specification.

---

## Non-Goals

The following are outside the initial MVP unless explicitly added by a future
specification.

### Cloud and Internet Services

- cloud notification relay
- internet-wide device communication
- hosted accounts
- hosted synchronization
- remote server dependency

### Multi-User Product Features

- public user registration
- shared accounts
- organizations
- teams
- multiple independent users sharing one Receiver
- public device directories

### Production-Grade Security

- enterprise identity management
- certificate infrastructure
- zero-trust networking
- production key lifecycle management
- remote device administration
- formal security certification
- adversarial internet threat protection

The MVP may still use lightweight pairing or session checks where required for
basic personal use.

### Full Notification Mirroring

The project does not initially aim to reproduce every Android notification
capability.

Examples outside the initial boundary include:

- remote notification actions
- inline replies
- media controls
- custom notification layouts
- exact visual reproduction
- arbitrary notification interaction forwarding

### General Device Synchronization

The project is not initially:

- a file synchronization system
- a clipboard synchronization system
- a messaging platform
- a remote-control system
- a general-purpose device bridge
- a device backup system

### Broad Platform Support

The MVP does not initially target:

- iOS
- desktop operating systems
- web clients
- browser extensions
- non-Android embedded devices

### Production Distribution

The initial project does not require:

- Play Store release readiness
- commercial support
- large-scale telemetry
- monetization
- enterprise deployment
- broad hardware compatibility certification

---

## Security Position

Security is necessary only to the extent required for a functional personal
local-network MVP.

The project should avoid obvious accidental cross-device delivery where a small
and understandable pairing or session mechanism can prevent it.

Security must not dominate the initial architecture.

The MVP does not claim protection against a hostile local network, compromised
device, advanced attacker, or internet-scale threat model.

Any concrete pairing, authorization, identity, token, or session behavior must
be defined in feature specifications.

---

## Reliability Position

The MVP should provide enough reliability for repeated personal testing.

It should make important operational states understandable, including whether
the relevant role is active and whether a local session is available.

The initial product does not require distributed-system guarantees,
high-availability operation, or lossless delivery under every failure mode.

Detailed delivery guarantees, retry behavior, queueing, deduplication, and
recovery rules must be introduced only through explicit feature
specifications.

---

## Data Position

The product handles notification information originating from the primary
Android device.

The MVP should process and retain only the information required by specified
features.

This document does not define the exact notification data model.

Detailed rules concerning:

- captured fields
- redaction
- filtering
- persistence
- deletion
- retention
- deduplication
- ordering

must be defined in feature specifications and, where appropriate,
architectural documentation.

---

## User Model

The initial user model is one person who owns or controls both Android devices.

The user is responsible for:

- installing the application on both devices
- selecting the appropriate role on each device
- granting required Android permissions
- placing both devices on a reachable local network
- performing any specified local pairing or session setup

The MVP does not initially require a remote account identity.

---

## Success Definition

The product direction is successful when a user can repeatedly perform the
following end-to-end journey:

1. Start or prepare the Sender on the primary Android phone.
2. Start or prepare the Receiver on the secondary Android phone.
3. Establish local communication between the devices.
4. Receive an eligible Android notification on the primary phone.
5. Transfer the notification through the local runtime flow.
6. Read the transferred notification in the Receiver inbox.

This journey defines the central product outcome.

Each step must eventually be supported by one or more feature specifications
with stable scenario identifiers and validation evidence.

---

## Specification Boundary

This document is intentionally stable and high level.

Changes to this document should represent a change in:

- product identity
- core roles
- primary runtime flow
- MVP boundary
- major goals
- major non-goals

Ordinary feature additions or behavioral refinements should not require changes
to this document.

Those changes belong under:

```text
spec/features/
```