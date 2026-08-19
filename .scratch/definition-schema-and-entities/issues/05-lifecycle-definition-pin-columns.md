# The Definition Version Pin columns

Status: ready-for-agent

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

Work pins its Lifecycle Definition when its Lifecycle starts, and never moves. This slice adds that
column to `negotiation` and to `negotiation_resource_link`, and maps it on both entities.

**Add it nullable.** ADR 0009's cutover backfills every row and only then sets NOT NULL, and that
cutover is a separate, much later migration. Adding NOT NULL here would make this migration
destructive to existing data, which is the one thing this slab may not be.

Map it as a plain id column rather than a JPA association. Both of these entities are read constantly
by code that exists today, and an association would let some existing read path lazily traverse into
the definition graph — which would fail this slab's gate.

## Acceptance criteria

- [ ] Both columns are added nullable by **this slice's own additive migration file** (one file per
      slice, never appended to an earlier slice's file), each with a foreign key to
      `lifecycle_definition`.
- [ ] Neither column is NOT NULL, and neither has a default.
- [ ] Both entities expose the pin as an immutable field — settable when the Lifecycle starts, never
      updatable afterwards.
- [ ] Neither entity gains a JPA association to any definition entity.
- [ ] A repository test persists and reads back a Negotiation and a NegotiationResourceLink with the
      pin set, and another with it null.
- [ ] Existing rows loaded from the test seed still load, with a null pin.
- [ ] Full suite green, parity count unchanged — this slice touches two of the most-read entities in
      the codebase, so the parity number is the real check here.

## Notes

The naming diverges from ADR 0003 and ADR 0009, which both spell this column `definition_version_id`
literally. It follows the table instead — see the PRD's naming decisions. The domain term
**Definition Version Pin** is unaffected.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)
