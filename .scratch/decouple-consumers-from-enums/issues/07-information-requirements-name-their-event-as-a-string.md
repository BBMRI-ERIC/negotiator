# Information Requirements name their Event as a string

Status: resolved

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

- [x] The entity, the repository and both DTOs name no Lifecycle enum.
- [x] No Flyway migration is added and no stored value changes.
- [x] The repository's existence-by-Event query returns the same rows for the same data.
- [x] Creating, reading and updating an Information Requirement through the API is unchanged, request
      and response body alike.
- [x] The schema metadata for the Event field reads as a string with a worked example.
- [x] The existing Information Requirement controller, service and model tests are extended rather
      than replaced.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

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

## Resolution

Recorded retroactively on 2026-08-27: the work landed at `31c6ed41` and this file was left saying
`ready-for-agent`. Nothing about the slice was redone.

The entity, the repository and both DTOs name no Lifecycle enum. `forEvent` is a `String` under
`@Column(length = 255)` over the same `VARCHAR(255)` column, no migration was added and no stored
value changed, so `existsByForEvent(String)` queries the same column for the same names and returns
the same rows.

**Two files beyond the predicted four were needed, both to keep behaviour identical rather than to
improve anything.** `InformationRequirementServiceImpl.mapToDTO` now sets `forResourceEvent`
explicitly, because ModelMapper's implicit match between `forEvent` and `forResourceEvent` survived
enum-to-enum and does not survive String-to-String - without that line the field serialises as
`null` and disappears from the admin API's response body. That was established by experiment during
this recording, not assumed; `STATUS.md` records the four tests that fail without it, and the
warning it carries for slices 10 and 11. And `NegotiationResourceEventNameDeserializer` preserves
the 400 on an unknown Event name, which a bare `String` field would otherwise have accepted;
`@JsonDeserialize` instantiates reflectively and so cannot use the Enum-Backed Lifecycle Catalog.
The entity's `equals` moved from `==` to `Objects.equals` in the same diff, which is required once
the field is a name rather than a constant.

Creating, reading and updating through the API is unchanged in request and response body - the five
pre-existing `$.forResourceEvent` wire assertions were untouched by this slice and still pass. The
schema is pinned as a string with a worked example against the *served* document, via
`GET /v3/api-docs`, not by reading the annotation back.

**One accepted micro-delta, shared with slice 08 and recorded rather than preserved.** A JSON
integer used to bind to a Resource Event by ordinal, because `FAIL_ON_NUMBERS_FOR_ENUMS` is unset;
through the deserializer it is refused with a 400. Slice 08 made the same call for
`UpdateResourcesDTO.state`. If either is ever undone, undo both.

Verification: +3 tests, all six touched test files extended rather than replaced. Focused runs
green - `InformationRequirementControllerTest` 32/0/0/0, `InformationRequirementServiceTest`
4/0/0/0, `unit/model/InformationRequirementTest` 2/0/0/0. The full suite, parity and delta figures
are slice 03's, measured explicitly after rebasing onto this slice: 1453/0/0/16 in 158 classes;
parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0.
