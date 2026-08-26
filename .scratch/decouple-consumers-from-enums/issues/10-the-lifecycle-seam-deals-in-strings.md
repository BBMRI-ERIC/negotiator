# The Lifecycle seam deals in strings

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The first contract step, and the slice that makes the decoupling real rather than textual.

Two application events announce a committed move and expose its States and its Event; two Lifecycle
service interfaces return and accept States, Events and Possible Events. All four types live inside
the Spring Statemachine package, so the slab gate permits them — **and that is exactly the problem**.
A consumer can reach an enum through one of these methods while importing nothing, so a guard built
from Java identifiers reports green over code that is still fully type-coupled. Slab 08 hit the same
shape in its table rule and warned the next slab; this slice is that warning taken seriously.

The four types deal in `String`. Every translation that slices 4, 5, 8 and 9 wrote at their call
boundaries is deleted here, which is the visible sign the seam actually moved.

Three assemblers hold one of these services as a dependency and can only decouple now: the two that
build links onto a Negotiation and a Resource, and the one that converts a name back into an Event
to build a link to the Lifecycle controller — a conversion that simply disappears once the
controller takes a name.

The controller itself takes its Event as a name in the URL path. **The two converters that produce
today's 400 for an unknown Event are live**, unlike the status converter slice 1 deleted, and no
ticket decided their fate — ticket 03 settled the status filter and never mentioned them. They are
replaced by a check against slice 2's catalog, so the status code and the response body are
unchanged.

## Acceptance criteria

- [ ] Both application events and both Lifecycle service interfaces name no Lifecycle enum in any
      signature.
- [ ] All three assemblers and the controller name no Lifecycle enum.
- [ ] Every translation written at a call boundary by slices 4, 5, 8 and 9 is gone.
- [ ] A test written **before** the change pins today's response for an unknown Event in a lifecycle
      URL path, and passes unchanged after it. Ticket 03 established that nothing pins this today.
- [ ] Possible Events returns the same Events, in the same form, for the same Lifecycle and caller.
- [ ] Sending an Event still returns the same resulting State and the same errors.
- [ ] Every link built by the three assemblers has the same relation name, the same target and the
      same display name as before.
- [ ] The two Event path converters are deleted, and the sweep for references to them comes back
      empty.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**This slice may want splitting once underway.** It is the only one in the slab whose size is not
well predicted by its file count, because the four seam types have many call sites. That is a
discovery to make against real code; split it if it turns out to warrant it, and record why in
`STATUS.md`.

**Do not change what Possible Events contains.** ADR 0005 deliberately changes it — blocked Events
omitted — and four of the eight intended-delta tests exist to invert when it does. Those eight must
still pass here. A red one in this slice means this slab did something ADR 0005 reserved for later.

**One assembler straddles.** The one that builds per-Resource links consumes Possible Events and
builds the per-row relations ADR 0005 collapses into array-valued ones. Keep it a pure type swap;
its shape is the Information Requirements slab's to change.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
- [02 The Enum-Backed Lifecycle Catalog](02-enum-backed-lifecycle-catalog.md)
- [04 Webhook payloads name States as strings](04-webhook-payloads-name-states-as-strings.md)
- [05 Notification handlers name States as strings](05-notification-handlers-name-states-as-strings.md)
