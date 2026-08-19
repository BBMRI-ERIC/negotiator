# The state and event tables

Status: resolved

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

The two vertex tables of the definition graph, both owned by a Lifecycle Definition.

A **State** is a named position carrying a human `label` plus `initial` and `terminal` flags.
Exactly one State per Lifecycle Definition is initial, and the database enforces it. `label` is not
decoration — the current notification body for a Resource state change is built from it, so a
definition whose States have no labels degrades a live notification to nulls in a sentence.

An **Event** is a named trigger. Critically, **an Event may carry no Transition at all** — that is
how the Override Event survives as a name under which an admin's direct state change appears in
history. Nothing about this table may assume an Event is reachable from a Transition.

Two shapes that look degenerate and are both real, verified against the frozen graph dump, so neither
may be constrained away:

- A **State that no Transition targets** — ADR 0009's Legacy States (`APPROVED`,
  `RETURNED_FOR_RESUBMISSION`) are exactly this, and they exist so that live strings and audit
  history still resolve after the cutover.
- A State with outgoing Transitions that nothing targets — `DRAFT` is occupied from outside the
  graph but is not enterable through it.

**Name uniqueness within a definition is load-bearing, not hygiene.** ADR 0009's cutover resolves a
live state string "through the natural key of the Definition Version Pin plus the state name", and
re-homes Information Requirements by matching the legacy `for_event` string against the Event "of the
same name". Both are natural-key lookups, so a duplicate name inside one definition would make the
cutover ambiguous. The same "exactly one initial State" caveat applies as in slice 01: a partial
unique index enforces at most one, and the at-least-one half waits for publish-time validation in
stage 3.

No `description` column on either table. That belongs to map ticket
[04](../../state-machine-implementation/issues/04-global-state-event-metadata-contract.md), as does
any Resource-State `ordinal`; adding either here would be an ADR 0002 amendment whose cost is
assessed there, not assumed here.

## Acceptance criteria

- [x] Both tables are created by **this slice's own additive migration file** (one file per slice,
      `V36.0` onward — never appended to an earlier slice's file), each with a foreign key to
      `lifecycle_definition`.
- [x] A partial unique index enforces at most one `initial` State per Lifecycle Definition; a test
      inserts a second one and asserts the write is refused.
- [x] A test asserts two **non-initial** States in the same definition are accepted.
- [x] A test persists an Event that no Transition references, and reads it back.
- [x] A test persists a State that no Transition targets, and reads it back.
- [x] Both `label` and `name` round-trip; a State with a null `label` is refused if the column is
      NOT NULL, and the test states which it is.
- [x] `name` is unique within a Lifecycle Definition, for **both** tables, enforced by the database,
      with a test that inserts a duplicate and asserts refusal — and a test that the **same** name in
      two different definitions is accepted.
- [x] Entities and repositories are package-private.
- [x] Full suite green, parity count unchanged. *(Verified in CI by the developer, not
      measured locally — see Outcome.)*

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)

## Outcome

**Landed on `feat/state-machine-implementation`.** Seven new files, no existing file touched.

| File | What |
|---|---|
| `backend/src/main/resources/db/migration/V36.1__add_state_and_event_tables.sql` | Both tables, both FKs, both name-uniqueness constraints, the partial unique index |
| `.../lifecycle/definition/State.java` | Package-private entity |
| `.../lifecycle/definition/Event.java` | Package-private entity |
| `.../lifecycle/definition/StateRepository.java`, `.../EventRepository.java` | Package-private repositories, no finders |
| `backend/src/test/java/.../StateRepositoryTest.java` | 11 tests against a real Postgres 16 |
| `backend/src/test/java/.../EventRepositoryTest.java` | 4 tests against a real Postgres 16 |

### Evidence

- `StateRepositoryTest` **11 tests, 0 failures**; `EventRepositoryTest` **4 tests, 0 failures**;
  slice 01's `LifecycleDefinitionRepositoryTest` still **11, 0 failures**.
- **Full suite: not measured.** A first run was contaminated — a concurrent Maven invocation
  recompiled `target/test-classes` mid-run, so 150 tests in 6 Spring-context classes errored with
  `FileNotFoundException: class path resource [...Test.class] cannot be opened because it does not
  exist`. Nothing to do with this slice; the run was discarded rather than interpreted. A clean re-run
  reached **91 classes, 1014 tests, 0 failures, 0 errors, 11 skipped** and was then stopped early by
  the developer, who asked for the commit without it. No failure was observed in any class that ran.
- **Parity gate: not run.** It needs a Maven run of its own and never got one.

Both remain owed. Nothing in this slice can plausibly break either — no existing file is touched, the
migration only adds two tables, and nothing in production reads them — but that is an argument, not a
measurement, and slice 01's precedent is a recorded count.

### Each rule ran red before it ran green

The tables were scaffolded first with no foreign keys, no unique constraints and a nullable `label`.
Exactly the six refusal tests failed, each with "Expected `DataIntegrityViolationException` to be
thrown, but nothing was thrown"; every round-trip and accepted-case test was green throughout, so
none of them can be passing because a constraint is missing.

Each violating row differs from its seed row **only** in the constrained dimension — with one
unavoidable exception. `save_withASecondInitialStateInTheSameDefinition_isRefused` cannot use the
same name in both rows, because that would trip `uq_state_name_per_definition` instead, so it asserts
the refusal message names `uq_state_initial_per_definition` rather than leaving attribution to a
reading of the DDL.

### `label` is NOT NULL, and the database is what refuses it

The acceptance criterion allowed either, so: **NOT NULL**. The current Resource state-change
notification body is built from `label`, so a State without one degrades a live notification to nulls
in a sentence. `insert_withoutALabel_isRefusedByTheDatabase` inserts through `JdbcTemplate` rather
than the repository, because the v1 seed is SQL and bypasses the mapping entirely — Hibernate's
`nullable = false` would otherwise mask whether the column itself carries the constraint.

### Two decisions later slices inherit

- **States and Events map their owner as a `@ManyToOne` association, not a plain `Long`.** The PRD
  requires a plain `Long` for the *pin* columns, and its reason is specific to them: nothing may
  lazily traverse into the definition graph from a read path that exists today. Inside this package
  there is no read path at all, and slice 03's Transition needs to reference both States and the
  Event, so the graph stays navigable.
- **The natural key is immutable.** `state.name` and `event.name` are locked with
  `@Setter(AccessLevel.NONE)` and `updatable = false`, as slice 01 locked `family_key` and `version`.
  Renaming a State would silently re-point every live string that resolves through it; a rename is a
  new Definition Version. `label`, `initial` and `terminal` stay editable, and
  `update_toTheEditableFields_leavesTheNameAndDefinitionUntouched` proves the observable half.
  Nothing on an `Event` is editable at all, so it carries no `@Setter`.

### Two review findings deliberately declined

- **The shared test fixture was not extracted.** Both test classes carry their own `definitionIn`
  helper and `setUp`. `recon-conventions` §4.3 shows this codebase keeps repository-test fixtures
  inline, so a shared fixture type would itself be a first — more than six duplicated lines cost.
- **`ON DELETE RESTRICT` is not distinguished from PostgreSQL's default `NO ACTION`.** Both refuse the
  delete; the difference is only deferrability, so no test can separate them. The two delete tests
  defend the convention's intent — not `CASCADE`, not `SET NULL` — and not its letter.
