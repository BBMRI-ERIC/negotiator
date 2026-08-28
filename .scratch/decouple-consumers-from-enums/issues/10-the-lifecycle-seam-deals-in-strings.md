# The Lifecycle seam deals in strings

Status: resolved

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

- [x] Both application events and both Lifecycle service interfaces name no Lifecycle enum in any
      signature.
- [x] All three assemblers and the controller name no Lifecycle enum.
- [x] Every translation written at a call boundary by slices 4, 5, 8 and 9 is gone.
- [x] A test written **before** the change pins today's response for an unknown Event in a lifecycle
      URL path, and passes unchanged after it. Ticket 03 established that nothing pins this today.
- [x] Possible Events returns the same Events, in the same form, for the same Lifecycle and caller.
- [x] Sending an Event still returns the same resulting State and the same errors.
- [x] Every link built by the three assemblers has the same relation name, the same target and the
      same display name as before.
- [x] The two Event path converters are deleted, and the sweep for references to them comes back
      empty.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

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

## Resolution

`NegotiationStateChangeEvent`, `ResourceStateChangeEvent`, `NegotiationLifecycleService` and
`ResourceLifecycleService` name States and Events as bare `String`s. Every translation slices 4, 5, 8
and 9 wrote against those four types is deleted - the webhook strategy's `nameOf`, the submission
handler's `nameOf`, four bare `.name()` calls across three notification handlers, slice 9's
`ResourceStateChangeEvent.fromNames` and slice 7's `existsByForEvent(event.name())`.

The two Event path converters are deleted and the sweep for them comes back empty apart from two
javadoc lines that name them as the reason their behaviour now sits inline. `NegotiationController`
resolves the Event through slice 2's catalog as the first statement of both lifecycle handlers; the
two metadata controllers resolve theirs with the converter's own `valueOf(x.toUpperCase())` in a try
and `ResponseStatusException(BAD_REQUEST)` in the catch, which is what keeps their pinned empty-body
400 and their lower-case acceptance byte-identical.

**The slice was not split.** The four seam types are one compile unit, so no ordering of them leaves
an intermediate commit both green and honest. The pinning test is its own commit.

**Three discoveries, recorded in `STATUS.md`.** The three assemblers are four -
`ResourceEventAssembler` reaches an enum through a carved-out metadata DTO's accessor and so appears
in no identifier scan. The two converters could not be deleted without also converting the two
metadata controllers' `getEvent`, because both bound through them and the characterization suite pins
their case handling and their 400 body. And ticket 03's "nothing pins this today" is wrong: the status
code was pinned for both lifecycle paths, and the *body* was what was not - along with the fact that
an unknown Event is refused before the caller is, which is the property a check placed after the
authority test would have broken silently.

**One accepted deviation from the PRD's "any behaviour change whatsoever", stated here and not only
in `STATUS.md`.** A request to `PUT /negotiations/{id}/lifecycle/{event}` carrying *both* an unknown
Event *and* an unreadable body used to get the converter's empty 400, because path variables were
bound before the body was read; it now gets the body's `"Wrong request"` 400. Both are 400, no client
sending valid JSON can reach it, and it is unavoidable once the converter goes - which is what this
slice's own acceptance criterion asks for. Recorded rather than preserved.

**Two things a reviewer should know are deliberate.** The sweep for the converters returns two hits,
both Javadoc lines that name them as the reason their behaviour now sits inline; nothing functional
references them. And the two inline `eventNamed` helpers in the metadata controllers were considered
for extraction and left as two copies: they return the *enum constant* their `ModelMapper` call
needs, they live in two different packages, and a shared home would mean a new class inside the
package the cutover deletes.

**This slice landed after slice 11 and was rebased onto it.** The two met in four files and each
side's translation was what the other deletes, so the resolution deleted both - the third time this
slab has hit that collision. `STATUS.md` records each hunk, including the one `valueOf` that had to
be carried back out of the deleted `fromNames` factory by hand.

Verification at the rebased tip: full backend suite 1480 tests in 159 classes, 0 failures, 0 errors, 16 skipped; parity 255 tests in 24 classes, 0
failures, 0 errors, 1 skipped; intended deltas 8 tests, 0 failures, 0 errors, 0 skipped.
