# The transition table

Status: resolved

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

The edges of the graph. A Transition names its Lifecycle Definition, its from-State, its to-State,
its Event, and its **Required Authority** — one of `NONE`, `IS_ADMIN`, `IS_CREATOR`,
`IS_REPRESENTATIVE`, `SYSTEM`.

Required Authority is a **field of its own, not a Guard**. ADR 0002 is deliberate about this: asking
*who is firing* and asking *whether the move is currently legal* fail differently and must not be
expressible as the same kind of row.

The table must support the lookup the evaluator will live on — "which Transitions leave State X for
Event Y" — as an indexed query rather than a scan.

## Acceptance criteria

- [x] The table is created by **this slice's own additive migration file** (one file per slice, never
      appended to an earlier slice's file), with foreign keys to the definition, to both States and to
      the Event.
- [x] `required_authority` persists as a string, not an ordinal, and rejects a value outside the five.
- [x] An index supports lookup by `(from_state, event)`.
- [x] A repository test round-trips a Transition and reads back all five references.
- [x] A test proves a Transition cannot reference a State or Event belonging to a **different**
      Lifecycle Definition, or — if the schema cannot express that constraint — the ticket records
      explicitly that it is unenforced and names what will enforce it.
- [x] `(lifecycle_definition, from_state, event)` is unique, enforced by the database, with a test
      that inserts a duplicate and asserts refusal. The evaluator's answer to "which Transition
      leaves State X for Event Y" must be a single row, and the frozen dump confirms **0 duplicate
      `(source, event)` pairs** across both graphs today — so this constrains nothing real and
      prevents an ambiguity the evaluator has no way to resolve.
- [x] Entity and repository are package-private.
- [x] Full suite green, parity count unchanged.

## Notes

**Build `required_authority` single-valued, exactly as ADR 0002 specifies.** Map ticket
[11](../../state-machine-implementation/issues/11-transition-authority-admin-or-creator.md) records a
genuine contradiction here — six of the eight Negotiation Transitions are behaviourally
`IS_ADMIN OR IS_CREATOR` and no single enum value reproduces that. Resolving it is **not** this
slice's job, and two of its three candidate resolutions leave this DDL untouched. Do not invent a
disjunction, a set-valued column or a join table to get ahead of it.

## Blocked by

- [02 The state and event tables](02-state-and-event-tables.md)

## Outcome

**Landed on `feat/state-machine-implementation`.** Five new files. No existing file was touched, though
this slice's migration does `ALTER` two of slice 02's tables — see the first decision below.

| File | What |
|---|---|
| `backend/src/main/resources/db/migration/V36.2__add_transition_table.sql` | The table, four FKs, the authority CHECK, the `(definition, from_state, event)` unique constraint, the `(from_state, event)` lookup index, and two UNIQUE constraints added to `state` and `event` |
| `.../lifecycle/definition/RequiredAuthority.java` | Package-private enum, the five values, single-valued |
| `.../lifecycle/definition/Transition.java` | Package-private entity |
| `.../lifecycle/definition/TransitionRepository.java` | Package-private repository, no finders |
| `backend/src/test/java/.../TransitionRepositoryTest.java` | 16 tests against a real Postgres 16 |

### Evidence

- `TransitionRepositoryTest` **16 tests, 0 failures**. Slices 01 and 02 unchanged in the same run:
  `LifecycleDefinitionRepositoryTest` **11**, `StateRepositoryTest` **11**, `EventRepositoryTest`
  **4**, all 0 failures — worth checking here, because this migration adds constraints to their
  tables.
- **Full suite: 150 classes, 1371 tests, 0 failures, 0 errors, 16 skipped.** Measured against a
  cleared `surefire-reports`, so the aggregate is this run alone. Slice 02's owed full-suite run is
  covered by this one.
- **Parity gate: 255 tests in 24 classes, 0 failures, 0 errors, 1 skipped** — the expected numbers,
  run separately with `-DexcludedGroups=intended-delta`. The full-suite run above carries the
  unfiltered form of the same package, **25 classes, 263 tests, 1 skipped**, which is the number the
  gate document gives for dropping both flags. Both halves therefore green and unchanged.
- `fmt-maven-plugin:check` clean across all 607 files.

### Each rule ran red before it ran green

The table was scaffolded first with no foreign keys, no unique constraint, no CHECK and no index.
Exactly seven tests failed — the three straddling refusals, the duplicate `(source, event)` refusal,
the sixth-authority refusal, the state-delete refusal, and the index assertion — six of them with
"Expected `DataIntegrityViolationException` to be thrown, but nothing was thrown". Every round-trip
and accepted-case test was green throughout, so none of them can be passing because a constraint is
missing.

`insert_withoutAnAuthority_isRefusedByTheDatabase` was added later, after review, and was run red on
its own by temporarily dropping `NOT NULL` from the column before being restored.

### Cross-definition references are *enforced*, not recorded as unenforced

The acceptance criterion allowed either. The schema can express the constraint, so it does: three
**composite** foreign keys, `(lifecycle_definition_id, from_state_id)` and the same for `to_state_id`
and `event_id`, each referencing `(lifecycle_definition_id, id)` on the vertex table. PostgreSQL only
accepts a composite FK against a unique constraint, so `state` and `event` each gained
`UNIQUE (lifecycle_definition_id, id)` — redundant as a uniqueness claim, since `id` alone is already
the primary key; its whole job is to be a referenceable pair.

**This is why slice 03's migration alters slice 02's tables.** It is still one file per slice and still
purely additive — nothing was appended to `V36.1` — but a reader looking for what constrains `state`
must now look in two files. The alternative was to leave a Transition able to straddle two graphs, so
that loading a graph by its Definition Version id could return edges pointing out of it.

Three tests cover it, one per reference: `save_withAFromStateFromAnotherDefinition_isRefused`,
`...AToState...`, `...AnEvent...`.

### The FK from `transition` to `lifecycle_definition` cannot be tested in isolation

It is transitively implied: `from_state_id` is NOT NULL and its composite FK guarantees the
`(definition, state)` pair exists in `state`, whose own FK guarantees the definition exists. So no
insert or delete can violate the direct FK while satisfying the composite ones. It is kept anyway —
the criterion asks for it, and it states the intent to a reader who does not reason through the
transitive argument — but it carries no test, deliberately.

A test that appeared to cover it, `delete_aDefinitionThatHasATransition_isRefused`, was **written and
then removed**: it passed during the red phase, against a table with no constraints at all, because
slice 02's FKs already refuse that delete. It proved nothing about this slice.

### `required_authority` gets a CHECK; slice 01's `scope` does not

An asymmetry worth naming rather than leaving to be discovered. The criterion here demands the column
"rejects a value outside the five", which needs a CHECK; slice 01 had no such requirement and
`recon-conventions` §2.6 records "no CHECK" as the most recent pattern for enum columns. The
constraint follows the house form exactly — `<table>_<column>_check` and the verbose
`::character varying[]` cast idiom shared by all thirteen existing CHECKs.

`insert_withAnAuthorityOutsideTheFive_isRefusedByTheDatabase` and
`insert_withoutAnAuthority_isRefusedByTheDatabase` both insert through `JdbcTemplate` rather than the
repository, because the v1 seed is SQL and bypasses the mapping entirely — Hibernate's Java enum and
`nullable = false` would otherwise mask whether the column carries the constraint.
`save_withEachOfTheFiveAuthorities_isAccepted` is the counterpart proving the whitelist is not
narrower than the enum.

### Two indexes, both earning their place

`uq_transition_definition_source_event` is unique on `(lifecycle_definition_id, from_state_id,
event_id)` as specified. Its index leads with the definition, so it **cannot** serve the evaluator's
`WHERE from_state_id = ? AND event_id = ?` lookup — hence the separate
`idx_transition_source_event`. Given the composite FKs, `from_state_id` functionally determines
`lifecycle_definition_id`, so a unique index on the pair alone would have done both jobs; that was
not done, because the criterion names the triple and the equivalence is an argument a reviewer would
have to verify rather than read.

### Column names come from the recon brief, not the ticket prose

`from_state_id`, `to_state_id`, `event_id` — `recon-expressiveness` §A4 names all three literally, and
they match the repo's `_id` suffix convention. The ticket's `(from_state, event)` was read as prose
describing the lookup, not as column spellings.

### `requiredAuthority` is the only mutable field

The four references are locked with `@Setter(AccessLevel.NONE)` and `updatable = false`: re-pointing
any of them does not edit this edge, it makes a different one. `requiredAuthority` is the one
attribute that is not part of the edge's shape, so it is the one thing left editable — the same
structure-immutable / attributes-editable split slice 02 applied to `State`.
`update_toTheRequiredAuthority_leavesTheReferencesUntouched` proves the observable half.

### One test asserts the schema rather than behaviour

`theLookupTheEvaluatorLivesOn_isIndexed` queries `pg_indexes`, which breaks the slab's rule that tests
assert externally observable behaviour. An index has no behavioural signature — it changes how fast
the lookup runs, not what it returns — and the alternative, an `EXPLAIN` asserting an index scan, is
worse: the planner rightly prefers a sequential scan on a table this small. The assertion matches on
`(from_state_id, event_id)` appearing in an `indexdef`, which the triple-column unique index cannot
satisfy, so it genuinely requires the dedicated index.

### Review findings acted on, and one declined

- **ADR citations were stripped from the code comments.** A first draft cited ADR 0002 in
  `RequiredAuthority`'s javadoc and in the migration. Commits `d7b81a40` and `d8cecdb5` had just
  established the opposite convention for this package — comments carry definitions and short
  constraint rationales, not ADR citations — and no sibling file in slices 01–02 contains one. The
  rationale was kept, the citation dropped.
- **A missing NOT NULL test was added.** `required_authority` being NOT NULL was asserted only in
  prose. `insert_withoutAnAuthority_isRefusedByTheDatabase` now covers it, and was proven red by
  temporarily dropping the constraint before being run green against it.
- **The shared test fixture was again not extracted.** `definitionIn` and the two family constants are
  now in their **third** copy across the package's repository tests. Slice 02's reason still holds —
  `recon-conventions` §4.3 shows this codebase keeps repository-test fixtures inline — but three copies
  is the point at which this stops being obviously right. Slice 04 adds a fourth; extraction should be
  decided there rather than drifting into it.
