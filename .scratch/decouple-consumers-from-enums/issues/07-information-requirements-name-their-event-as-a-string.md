# Information Requirements name their Event as a string

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

Four files in the Information Requirements subsystem: the entity's persisted Event field, the
repository method that queries it, and both DTOs that carry it.

**This subsystem appears in no commit-order line of ticket 07 and in no consumer list of ticket 03.**
It was found by the blast-radius sweep and recorded there. It is nonetheless squarely in scope: the
slab gate exempts only the three metadata DTOs, and these four files name a Lifecycle enum.

An Information Requirement attaches to an Event, so that it applies wherever that Event is used.
After this slice it names that Event as a string.

**No migration and no data change.** The column is already `VARCHAR(255)` and holds the name today,
so this is purely a Java type change. The additive migration slab 08 landed already anticipates
re-homing these rows onto a Definition Version's Event; that re-homing is the migration slab's work,
not this slice's.

## Acceptance criteria

- [ ] The entity, the repository and both DTOs name no Lifecycle enum.
- [ ] No Flyway migration is added and no stored value changes.
- [ ] The repository's existence-by-Event query returns the same rows for the same data.
- [ ] Creating, reading and updating an Information Requirement through the API is unchanged, request
      and response body alike.
- [ ] The schema metadata for the Event field reads as a string with a worked example.
- [ ] The existing Information Requirement controller, service and model tests are extended rather
      than replaced.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**What this slice does not do.** The map's standing decision 6 governs the forced change to the
Information Requirement admin API — the creation DTO's Event field becoming an Event reference, with
Audience and Quantifier persisted but defaulted server-side and not exposed for authoring. That is
the Information Requirements slab's work. This slice only makes the field a name, which is the
minimum the gate requires and changes nothing an administrator can observe.

**The admin screens must keep working.** The frontend drives this API and hand-codes the strings it
sends, so an identical wire format means no frontend change. Verify by running the app and looking,
per standing decision 5.

**`isViewableOnlyByAdmin`** is a live field on the entity and both DTOs that appears in no ADR. This
slice touches these files without touching it. Still unowned; the map records it.

## Blocked by

None - can start immediately. Independent of slices 1, 2 and 3.
