# The Definition Version Pin columns

Status: resolved

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

- [x] Both columns are added nullable by **this slice's own additive migration file** (one file per
      slice, never appended to an earlier slice's file), each with a foreign key to
      `lifecycle_definition`.
- [x] Neither column is NOT NULL, and neither has a default.
- [x] Both entities expose the pin as an immutable field — settable when the Lifecycle starts, never
      updatable afterwards.
- [x] Neither entity gains a JPA association to any definition entity.
- [x] A repository test persists and reads back a Negotiation and a NegotiationResourceLink with the
      pin set, and another with it null.
- [x] Existing rows loaded from the test seed still load, with a null pin.
- [x] Full suite green, parity count unchanged — this slice touches two of the most-read entities in
      the codebase, so the parity number is the real check here.

## Notes

The naming diverges from ADR 0003 and ADR 0009, which both spell this column `definition_version_id`
literally. It follows the table instead — see the PRD's naming decisions. The domain term
**Definition Version Pin** is unaffected.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)

## Outcome

**Landed on `feat/state-machine-implementation`.** One migration, two entities and one test, and no
file outside this slice was touched. Unlike slices 02-04 this one alters tables the running system reads constantly, which is
what the parity number was there to check.

| File | What |
|---|---|
| `backend/src/main/resources/db/migration/V36.4__add_definition_version_pin_columns.sql` | `lifecycle_definition_id BIGINT` added to `negotiation` and to `negotiation_resource_link`, nullable, no default, each with `ON DELETE RESTRICT` to `lifecycle_definition` |
| `.../negotiation/Negotiation.java` | Nullable immutable `Long lifecycleDefinitionId`; no association |
| `.../negotiation/NegotiationResourceLink.java` | The same field, plus a four-argument public constructor taking the pin; the existing three-argument one delegates with null |
| `backend/src/test/java/.../lifecycle/definition/DefinitionVersionPinRepositoryTest.java` | 9 tests against a real Postgres 16, with the test seed loaded |

### Evidence

- `DefinitionVersionPinRepositoryTest` **9 tests, 0 failures, 0 errors, 0 skipped**.
- **Full suite: 153 classes, 1398 tests, 0 failures, 0 errors, 16 skipped.** Summed from
  `surefire-reports` with the directory cleared before the run, so the aggregate is that run alone.
  1398 is slice 04's 1389 plus exactly these 9.
- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — unchanged, run in its
  filtered form with `-DexcludedGroups=intended-delta`. No report was written for
  `IntendedDeltasAdr0005WillInvertTest`, which is how the gate document says to verify the split.
- `fmt-maven-plugin:format` reports 0 of 615 files reformatted on the final tree.
- Nothing outside `lifecycle.definition` references any type from it. The pin is a `Long`, not a
  `LifecycleDefinition`, so the two entities gain no import from the package at all and the slab
  stays inert.

### Each rule ran red before it ran green

| Mutation | Result |
|---|---|
| Both foreign keys dropped | Exactly 4 red: both `update_...PinnedToAnUnknownDefinition_isRefusedByTheDatabase` ("nothing was thrown") and both `delete_aDefinitionPinnedBy..._isRefused` |
| Both foreign keys changed to `ON DELETE CASCADE` | Exactly 2 red: the two `delete_...` tests. The four acceptance tests and both FK-refusal tests stay green, which is what earns the delete tests their place |
| `negotiation.lifecycle_definition_id` made `NOT NULL` | All 9 error: Flyway fails on `R__Initial_data.sql` with `null value in column "lifecycle_definition_id" of relation "negotiation" violates not-null constraint`, and the context never builds |
| `insertable = false` added to both entity fields | Exactly 4 red: both `save_...PinnedToADefinition_roundTrips` (`expected: <6> but was: <null>`), and the two `delete_...` tests as collateral — with no pin ever written there is nothing to restrict |

The `NOT NULL` mutation is the one that matters most, and it is the acceptance criterion "adding
NOT NULL here would make this migration destructive to existing data" turned into an observation.
The seed stands in for existing data: it runs after the migration and inserts negotiations without
the column, so a `NOT NULL` column takes the whole application down at startup rather than failing a
test. Nothing subtler was needed to prove the column has to be nullable.

`ON DELETE RESTRICT` got a test even though slice 02 established that `RESTRICT` cannot be told from
PostgreSQL's default `NO ACTION` — both refuse. What the CASCADE mutation shows is that the test
rules out the one referential action that would be actively dangerous here: on `negotiation`, a
cascade would delete the work along with the configuration it was submitted under. That is a
different failure from the one the FK-refusal tests cover, so it is not one of slice 03's tests that
cannot fail.

### "No default" needed no assertion of its own

The seed runs *after* the migration and names neither new column, so seeded rows reading back null
is simultaneously the proof that neither column carries a `DEFAULT` —
`load_seededRowsThatPredateTheColumn_haveANullPin` covers both criteria at once. A mutation for it
was not run because there is no valid default to mutate *to*: any non-null literal is a
`lifecycle_definition` id that does not exist at migration time, so the foreign key refuses it and
the failure is attributable to the wrong constraint.

### The pin is writable only at insert, and for Resources that is wrong

`@Setter(AccessLevel.NONE)` plus `@Column(updatable = false)` is the strictest honest reading of
"settable when the Lifecycle starts, never updatable afterwards". For a Negotiation the two moments
coincide — the row is inserted already in `SUBMITTED`.

For a Resource they do not, and `backend/CONTEXT.md` says so: a Resource "pins at Spawn", and Spawn
"names the initialization, not an instantiation" of an already-linked row. So the spawn Action will
find a persisted, unpinned `negotiation_resource_link` and will not be able to write its pin through
the mapping. Nothing is blocked today, because nothing writes either pin in this slab; deciding
between a `@Modifying` query, a write-once setter, and moving resolution earlier needs the spawn's
shape in hand. Filed as
[09 Pinning a Negotiation Resource Link that already exists](09-pinning-an-existing-resource-link.md),
with the warning that removing `updatable = false` on its own converts a compile error into a
silently dropped value.

The four-argument constructor exists for the same reason: the link has no repository, `Negotiation`
owns the collection and its `resourcesLink` accessors are private, so a constructor argument is the
only way to create a pinned link at all — and the only way the test could build one.

### Neither column is indexed

Deliberate, and a weaker call than slice 04's identical one on `guard_wiring`: these two tables are
not small configuration tables. They are, however, among the most-written, and the column is 100%
NULL until the cutover backfills it, so an index today costs write throughput and buys nothing.
Filed with its trigger conditions as
[08 The two pin columns have no index on them](08-pin-column-fk-indexes-deferred.md); the first
trigger to fire will be the cutover setting either column NOT NULL, which is also the migration that
should build the index.

### Where the test lives, and why it loads the seed

`DefinitionVersionPinRepositoryTest` is in `eu.bbmri_eric.negotiator.lifecycle.definition`, not under
`integration/repository/`, because it needs `LifecycleDefinitionRepository` for a row to point at and
that repository is package private; `Negotiation` and `NegotiationResourceLink` are public and reach
it fine from there.

`loadTestData = true` pays three times: it supplies rows that predate the column, it makes "no
default" free, and `negotiations.findById("negotiation-1")` hands over a `DiscoveryService` and a
`Resource` without building an Organization, a DiscoveryService, a Person and a Resource by hand the
way `NegotiationRepositoryTest` does. `@Import(MockUserDetailsService.class)` is still needed because
`Negotiation` extends `AuditEntity`.

Both pins point at the one `NEGOTIATION`-scoped definition the fixtures build rather than at a
`NEGOTIATION` one and a `RESOURCE` one. Nothing constrains a pin against a Definition Version's
`scope` — matching the two is Definition Resolution's job, not this column's — so a second family
would add fixture surface to every test and document nothing the schema enforces. The class javadoc
says so, since a reader would otherwise assume it was an oversight.
