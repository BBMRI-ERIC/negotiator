# DTOs, mappers and the Negotiation timeline

Status: resolved

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The bulk mechanical batch: the Negotiation and Resource DTOs that carry a State, the model mapper
that converts one, the update DTO whose State field has a default, and the Negotiation timeline that
renders Lifecycle Records as text.

Most of this is a type swap the compiler finds for you. Three sites need judgement:

- The Negotiation DTO decides whether a payload is still updatable by comparing its status against
  three names. Those become holder constants.
- The timeline names two Resource States when building its text. Holder constants again.
- The update DTO **defaults** its Resource State field to a name rather than comparing against one.
  Ticket 03 flagged this specifically for this slab: a default that names a Resource State is wrong
  for any Definition Family that lacks it. Keep the default's value identical — changing it is a
  behaviour change — but record the hazard where the next reader will find it.

**The three assemblers that hold a Lifecycle service are not in this slice**, despite resembling
these mappers. They can only decouple when the service interfaces do, so they belong to slice 10.

## Acceptance criteria

- [ ] Every DTO, mapper and the timeline in this batch names no Lifecycle enum - **five of six**.
      `NegotiationModelMapper` keeps one `NegotiationState.valueOf` as the boundary translation into
      the still-enum-typed entity field; ModelMapper refuses to coerce a converter's result, so it
      cannot go from here. Deferred to slice 11 and recorded in `STATUS.md`.
- [x] Every JSON response keeps the same field names and the same string values.
- [x] The payload-updatable rule admits and refuses exactly the same States as before.
- [x] The timeline renders identical text for identical history.
- [x] The update DTO's default value is unchanged, and the hazard ticket 03 raised about it is
      recorded in the code where a reader meets it.
- [x] Schema metadata on every State and Event field reads as a string with a worked example.
- [x] The existing mapper unit tests and timeline integration test are extended rather than replaced.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**The model mapper's status conversion already returns a name.** It maps a State to its name and an
empty string for absent, which means the boundary this slice is chasing is half-built already —
check what it does before rewriting it, because the conversion may simply become the identity and
disappear.

**Do not touch the three metadata DTOs.** They are carve-out 1 and belong to ticket 04, which asks
whether an endpoint enumerating a universe of States still makes sense. The slab gate names them.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
- [02 The Enum-Backed Lifecycle Catalog](02-enum-backed-lifecycle-catalog.md)
