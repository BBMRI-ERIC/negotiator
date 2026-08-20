# Slab status — definition schema and entities

Working record for the slab. Delete this file when the slab closes.

## Landed on `feat/state-machine-implementation`

| Slice | State | Evidence |
|---|---|---|
| [01 lifecycle_definition table](issues/01-lifecycle-definition-table.md) | **done** | 11 tests green; full suite 1343/0/0/16; parity 255/24/1 skipped |
| [02 state and event tables](issues/02-state-and-event-tables.md) | **done** | 15 tests green; full suite and parity green in CI over `071ff565` (developer-run) |
| [03 transition table](issues/03-transition-table.md) | **done** | 16 tests green; full suite 1371/0/0/16; parity 255/24/1 skipped |
| [04 guard and action wiring](issues/04-guard-and-action-wiring.md) | **done** | 18 tests green; full suite 1389/0/0/16; parity 255/24/1 skipped |
| [05 pin columns](issues/05-lifecycle-definition-pin-columns.md) | not started | |
| [06 DefinitionResolver seam](issues/06-definition-resolver-seam.md) | not started | |
| [07 inertness gate](issues/07-inertness-gate.md) | not started | |

## What slice 01 fixed for every later slice

- **Migration numbering.** `V36.0` is taken. Slice 02 is `V36.1`, and so on to `V36.5` — one file per
  slice, never appending to an applied file, because that changes its checksum and fails Flyway
  validation on any developer database that already ran it.
- **Table name and FK spelling.** `lifecycle_definition`, so foreign keys read
  `lifecycle_definition_id`. Settled by the PRD; do not relitigate.
- **`ON DELETE RESTRICT`** on every foreign key pointing at `lifecycle_definition`. No FK exists yet,
  so slice 02 is the first to apply it. ADR 0003 says a version that is active or referenced is never
  mutated in place and never discarded, so a cascade would express a deletion the model does not have.
- **Package.** `eu.bbmri_eric.negotiator.lifecycle.definition`, entities and repositories
  package-private. `@EntityScan`/`@EnableJpaRepositories` glob `eu.bbmri_eric.negotiator.*` reaches
  two levels down, so no configuration change was needed and none will be.
- **Test placement.** Package-private types are invisible from `integration/repository/`, so
  repository tests live in the entity's own package under `src/test/java`. `template/` and `webhook/`
  are the precedent.
- **Partial unique index syntax, now proven against Postgres 16** rather than assumed:

  ```sql
  CREATE UNIQUE INDEX uq_lifecycle_definition_active_per_family
      ON lifecycle_definition (family_key)
      WHERE active;
  ```

  Naming is `uq_<table>_<what-it-makes-unique>`. Slices 02 and 04 need the same shape for "one initial
  State per version" and the two Guard `sort_order` scopes.
- **How to prove one.** `assertThrows(DataIntegrityViolationException.class, () ->
  repository.saveAndFlush(row))`, one violation per test method — a refused write poisons the
  transaction, so the "and this one *is* accepted" case must be its own method. Make the violating row
  differ from the seed row **only** in the constrained dimension, or the test proves nothing about
  which index refused it.
- **Fixture idiom.** A `versionBuilder(familyKey, version)` static helper returning a half-built
  builder, `@RepositoryTest` with `loadTestData` left false, `@Autowired EntityManager` for
  `clear()` between write and read.

## What slice 02 fixed for slices 03-05

- **The FK to `lifecycle_definition`** is `lifecycle_definition_id BIGINT NOT NULL` with
  `CONSTRAINT fk_<table>_lifecycle_definition ... ON DELETE RESTRICT`. Slice 02 applied it first;
  copy it verbatim. No separate index on the FK column is needed where a composite UNIQUE already
  leads with it.
- **Owner mapping is a `@ManyToOne(fetch = FetchType.LAZY)` association** with an explicit
  `@JoinColumn(name = ..., nullable = false, updatable = false)`, not a plain `Long`. The PRD's
  plain-`Long` rule is specific to the *pin* columns on `negotiation` and
  `negotiation_resource_link`. Slice 03's Transition references States and the Event the same way.
- **Natural keys are immutable**: `@Setter(AccessLevel.NONE)` + `updatable = false`, as on
  `family_key` and `version`. Locked so far: `state.name`, `event.name`, and both owner associations.
- **Name uniqueness within a definition** is `ALTER TABLE ... ADD CONSTRAINT
  uq_<table>_name_per_definition UNIQUE (lifecycle_definition_id, name)`.
- **When a violating row cannot differ from its seed row only in the constrained dimension**, assert
  the index name out of the refusal message —
  `assertTrue(refused.getMessage().contains("uq_state_initial_per_definition"))` — rather than
  leaving attribution to a reading of the DDL. Slice 04 needs this for the two Guard `sort_order`
  scopes.
- **A NOT NULL column is proven through `JdbcTemplate`**, not the repository: Hibernate's
  `nullable = false` refuses first and would mask whether the column carries the constraint, and the
  v1 seed is SQL.
- **`ON DELETE RESTRICT` cannot be told apart from PostgreSQL's default `NO ACTION`** by any test —
  both refuse, the difference is deferrability. A delete test defends the intent, not the letter.
- **Never run two Maven invocations against `backend/` at once.** A concurrent recompile clears
  `target/test-classes` under a running suite, and every Spring-context class then errors with
  `FileNotFoundException: class path resource [...Test.class] cannot be opened`. It looks like 150
  real failures and is an artifact. Sub-agents that verify by running tests count as a second
  invocation.

## What slice 03 fixed for slices 04-05

- **A row cannot straddle two Definition Versions, and the schema *can* say so.** The technique is a
  composite foreign key: the referencing table carries `lifecycle_definition_id`, and the FK is on the
  *pair*.

  ```sql
  -- once per vertex table, so the pair is referenceable at all: PostgreSQL only accepts a
  -- composite FK against a unique constraint
  ALTER TABLE state ADD CONSTRAINT uq_state_id_with_definition UNIQUE (lifecycle_definition_id, id);

  CONSTRAINT fk_transition_from_state FOREIGN KEY (lifecycle_definition_id, from_state_id)
      REFERENCES state (lifecycle_definition_id, id) ON DELETE RESTRICT
  ```

  **Slice 04 needs exactly this**, and it is the reason `guard_wiring` carries both
  `lifecycle_definition_id` and a nullable `transition_id`: without the composite FK, a
  definition-scoped Guard could be wired to a Transition in a different definition. `transition` has
  **no** `UNIQUE (lifecycle_definition_id, id)` yet — slice 03 did not add one, to avoid building
  ahead of slice 04. Slice 04's own migration should add it, exactly as slice 03 added it to `state`
  and `event`. Note the interaction with a *nullable* `transition_id`: PostgreSQL's default `MATCH
  SIMPLE` skips the check entirely when any column of the pair is null, which is the wanted behaviour
  for a definition-scoped Guard — but it means the null case is unconstrained, so it needs its own
  test rather than an assumption.
- **A composite FK is invisible to JPA and needs no mapping.** Each column stays a separate
  single-column `@JoinColumn`; Hibernate inserts all of them and PostgreSQL validates the pair. No
  `insertable = false` juggling, no `@IdClass`.
- **The consequence is that one slice's migration alters an earlier slice's tables.** Still additive,
  still one file per slice, but what constrains `state` now lives in both `V36.1` and `V36.2`.
- **A redundant-looking FK may be untestable.** `fk_transition_lifecycle_definition` is transitively
  implied by the composite FKs, so no write can violate it while satisfying them. It is kept for the
  reader and carries no test. Check for this before writing a test that cannot fail — slice 03 wrote
  one, watched it pass against a table with *no* constraints, and deleted it.
- **A unique constraint's index has a leading column, and that decides what it can serve.**
  `UNIQUE (lifecycle_definition_id, from_state_id, event_id)` cannot answer
  `WHERE from_state_id = ? AND event_id = ?`, so the lookup index is a second, separate
  `CREATE INDEX`.
- **An enum column gets a CHECK only when its ticket asks for one.** `transition.required_authority`
  has `transition_required_authority_check`; `lifecycle_definition.scope` has none. Follow the house
  form — `<table>_<column>_check` and the verbose `::character varying[]` cast idiom — and pair it with
  a test that all the *valid* values are accepted, or a too-narrow whitelist passes unnoticed.
- **An index has no behavioural signature.** Where a criterion asks for one, the only honest test is a
  `pg_indexes` assertion; `EXPLAIN` is flaky because the planner prefers a sequential scan on a table
  this small. Slice 03's `theLookupTheEvaluatorLivesOn_isIndexed` is the one schema-level assertion in
  the slab.
- **No ADR citations in code comments or migrations.** Comments carry definitions and short constraint
  rationales only — the convention set by `d7b81a40` and `d8cecdb5`. Slice 03's first draft
  reintroduced two and review caught them.
- **The shared repository-test fixture is now in its third copy** (`definitionIn` plus the two family
  constants). Slice 02 declined to extract it and slice 03 inherited that; slice 04 makes it a fourth,
  which is where the call should actually be made rather than drifted into.

## What slice 04 fixed for slices 05-07

- **A nullable column in a composite FK disables that FK for the null rows.** PostgreSQL's default
  `MATCH SIMPLE` stops checking as soon as any column of the pair is null, which is exactly what a
  definition-scoped Guard wants — but it means the surviving single-column FK is the only thing
  guarding those rows, and unlike slice 03's redundant one it **can** fail. Test both halves: the null
  shape accepted, and a bogus definition id refused.
- **Two non-overlapping partial unique indexes give two independent sequences**, and the test that
  proves it is the *duplicate-within-the-null-scope* one. A single non-partial
  `(definition, transition, sort_order)` index passes every other test in the class, because
  PostgreSQL treats nulls as distinct in a plain unique index. If a later slice needs "unique per
  scope where one scope is null", copy this shape:

  ```sql
  CREATE UNIQUE INDEX uq_guard_wiring_sort_order_definition
      ON guard_wiring (lifecycle_definition_id, sort_order) WHERE transition_id IS NULL;
  CREATE UNIQUE INDEX uq_guard_wiring_sort_order_transition
      ON guard_wiring (transition_id, sort_order) WHERE transition_id IS NOT NULL;
  ```
- **A partial index does not satisfy slice 02's "no separate index on the FK column is needed where a
  composite UNIQUE already leads with it".** That rule assumed the leading index is a *plain* one, and
  `guard_wiring` is the first table where it is not: the only index leading with
  `lifecycle_definition_id` is `uq_guard_wiring_sort_order_definition`, which is partial (`WHERE
  transition_id IS NULL`), so transition-scoped rows are not indexed on that column at all. The
  `ON DELETE RESTRICT` check behind `fk_guard_wiring_lifecycle_definition` therefore falls back to a
  sequential scan for them. **No index was added**, deliberately: the scan is over a small
  configuration table and runs only when a Definition Version is deleted, which ADR 0003 says does not
  happen to one that is active or referenced. A later slice must add a plain
  `INDEX (lifecycle_definition_id)` here the moment `guard_wiring` is queried by definition — a
  `findByLifecycleDefinitionId` on the resolver's load path is the trigger — or if the table stops
  being small. `action_wiring` has no such gap: `uq_action_wiring_transition_sort_order` is a plain
  `UNIQUE (transition_id, sort_order)` and fully covers its FK column.
- **`SELECT col::text` does not prove a column is jsonb.** It returns identical bytes from `text`,
  `json` and `jsonb`, so an assertion against an already-canonical payload proves only that Hibernate
  can hand a string back. Write the payload **non-canonical** and assert the canonical form: jsonb
  normalises object keys to **length-then-bytewise** order — not alphabetical, so `{"b":1,"aa":2,"a":3}`
  becomes `{"a": 3, "b": 1, "aa": 2}` — and re-spaces separators, while preserving array order.
- **A `uq_` name can belong to an index or to a constraint, and both surface in the refusal message.**
  `guard_wiring` needs `CREATE UNIQUE INDEX` (partial), `action_wiring` a plain `ALTER TABLE ... ADD
  CONSTRAINT UNIQUE`. The `contains("uq_...")` assertion works for either, and earns its place: under
  a wrong-index mutation the transition-scope test failed on the *name*, not on the throw.
- **Type keys in test fixtures are read as the catalogue.** No strategy catalogue exists yet, so the
  only place a reader learns what a `type_key` looks like is these tests. Guards are predicates
  (`REQUIREMENT_MET`, `NEGOTIATION_APPROVED`); Actions are effects (`SET_POST_VISIBILITY`,
  `SPAWN_RESOURCE_LIFECYCLES`). Do not spell an Action's key on a Guard, and do not use
  `ENABLE_PUBLIC_POSTS` / `ENABLE_PRIVATE_POSTS` / `DISABLE_POSTS` at all — those are the three
  classes `params` exists to collapse into one key.
- **A no-definition-column table cannot straddle, so it gets no straddle test.** `action_wiring` has
  no `lifecycle_definition_id`; there is nothing to make disagree. Check for this before writing a
  test that cannot fail, as slice 03 did for its redundant FK.
- **The shared repository-test fixture is now in five copies** (`definitionIn`; `eventIn` 4, `stateIn`
  3, `STANDARD_FAMILY` 6) across all six tests in the package. Slice 03 asked slice 04 to decide, and
  the decision is **extract it, in its own commit, before slice 05** — a package-private
  `DefinitionFixtures` holding the two family constants and the three helpers that return a *built*
  entity (`definitionIn`, `stateIn`, `eventIn`). The two helpers that do return a half-built builder,
  `stateBuilder` and `versionBuilder`, have one caller each and are not part of the duplication.
  Kept out of slice 04 because it rewrites slices 01-03's landed tests. Slice 05 adds no new copy, so
  extracting first costs nothing.

## Invariants deliberately left unenforced, for stage 3

No trigger, no deferred constraint:

- the *at least one* half of "exactly one active version per family" — zero active rows is a valid
  intermediate state during a publish;
- `scope` is fixed for a whole Definition Family but is stored per row, with no family table to hold it;
- a Definition Version needs at least one initial State — slice 02 added the *at most one* half as
  `uq_state_initial_per_definition`; zero initial States is still legal;
Resolved, and no longer on this list: **a Transition can no longer reference a State or Event
belonging to a different Definition Version.** Slice 03 found the schema can express it after all —
see the composite-FK entry below — so this moved from "left for stage 3" to enforced by the database.
