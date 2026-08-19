# Slab status — definition schema and entities

Working record for the slab. Delete this file when the slab closes.

## Landed on `feat/state-machine-implementation`

| Slice | State | Evidence |
|---|---|---|
| [01 lifecycle_definition table](issues/01-lifecycle-definition-table.md) | **done** | 11 tests green; full suite 1343/0/0/16; parity 255/24/1 skipped |
| [02 state and event tables](issues/02-state-and-event-tables.md) | **landed, gate not measured** | 15 tests green; full suite stopped early at 91 classes/1014 tests, 0 failures; parity not run |
| [03 transition table](issues/03-transition-table.md) | not started | |
| [04 guard and action wiring](issues/04-guard-and-action-wiring.md) | not started | |
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

## Invariants deliberately left unenforced, for stage 3

No trigger, no deferred constraint:

- the *at least one* half of "exactly one active version per family" — zero active rows is a valid
  intermediate state during a publish;
- `scope` is fixed for a whole Definition Family but is stored per row, with no family table to hold it;
- a Definition Version needs at least one initial State — slice 02 added the *at most one* half as
  `uq_state_initial_per_definition`; zero initial States is still legal;
- a Transition may reference a State or Event belonging to a *different* Definition Version: the FKs
  point at `state.id` and `event.id`, which carry no definition in them. Slice 03 owns whether the
  schema can express that at all.
