# Slab status — definition schema and entities

Working record for the slab. Delete this file when the slab closes.

## Landed on `feat/state-machine-implementation`

| Slice | State | Evidence |
|---|---|---|
| [01 lifecycle_definition table](issues/01-lifecycle-definition-table.md) | **done** | 11 tests green; full suite 1343/0/0/16; parity 255/24/1 skipped |
| [02 state and event tables](issues/02-state-and-event-tables.md) | not started | |
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
  so slice 02 is the first to apply it. The reason is recorded at the foot of `V36.0`.
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

## Invariants deliberately left unenforced, for stage 3

Recorded at the foot of `V36.0` too. No trigger, no deferred constraint:

- the *at least one* half of "exactly one active version per family" — zero active rows is a valid
  intermediate state during a publish;
- `scope` is fixed for a whole Definition Family but is stored per row, with no family table to hold it;
- a Definition Version needs at least one initial State (slice 02 will add the *at most one* half).
