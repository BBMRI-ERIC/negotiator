# The lifecycle_definition table

Status: resolved

## Parent

[PRD — Definition schema and entities](../PRD.md), for map ticket
[08](../../state-machine-implementation/issues/08-definition-schema-and-entities.md).

## What to build

The root table of the whole schema: one row is one complete, immutable Lifecycle Definition. This
slice creates the additive migration file that every later slice appends to, plus the entity and
repository that map this table, and proves the four uniqueness rules hold against a real PostgreSQL.

A row carries its Definition Scope (`NEGOTIATION` or `RESOURCE`), the immutable `family_key` that
makes it a member of a Definition Family, a freely editable `name` display label, the system-assigned
display-only `version` integer, the `active` flag, and `is_global_default`.

Four rules the database itself must enforce, because publishing a malformed definition has to be
impossible rather than merely discouraged:

- `version` is unique within a family.
- At most one row per family is `active`.
- At most one row in the whole table is `is_global_default`.
- `family_key` is immutable and `version` carries no identity — the row id is the sole machine
  identity (ADR 0003), so nothing anywhere looks a definition up by `(family_key, version)`.

`is_global_default` belongs to stage 2's Definition Resolution and is deliberately built now: it
costs one boolean and one index, and it means stage 2 needs no data migration to designate the
default.

**Read [recon-conventions.md](../recon-conventions.md) before writing anything** — it carries the
next free Flyway version, the DDL and entity idiom, and the template tests, all cited `file:line`.
Do not re-derive them.

## Acceptance criteria

- [x] A new additive-only Flyway migration exists at the next free version, creating this table. It
      contains no `DROP`, no `ALTER ... DROP`, and no data.
- [x] The entity and its repository are **package-private**, per the codebase's most recent idiom.
- [x] A repository test round-trips a row through a real PostgreSQL, asserting every column survives.
- [x] A test inserts a second row with the same `(family_key, version)` and asserts the write is
      refused.
- [x] A test inserts a second `active` row in the same family and asserts the write is refused, then
      asserts an `active` row in a *different* family is accepted.
- [x] The `is_global_default` index is scoped to active rows — `WHERE is_global_default AND active`.
      A test inserts a second row that is both `is_global_default` and `active` and asserts refusal;
      a further test asserts an **inactive** row carrying `is_global_default` **is** accepted, since
      the flag belongs to the family and travels across its versions.
- [x] Two rows in the same family that are both **inactive** are accepted — the index is partial, so
      it must not constrain them.
- [x] `scope` persists as a string, not an ordinal.
- [~] Foreign keys pointing at this table are `ON DELETE RESTRICT`. *(Vacuous in this slice — none
      exist yet. Recorded as a note at the foot of `V36.0` for slices 02-05.)* ADR 0003 says a version that is
      active or referenced is never mutated in place and **never discarded**, so a cascade would
      express a deletion the model does not have.
- [x] The full backend suite is green, including the parity half of
      [parity-gate.md](../../state-machine-implementation/parity-gate.md) at its unchanged count.
- [x] `git diff` shows no change to any existing production class other than what this slice adds.

## Notes

**"Exactly one" is only half-enforceable, and the gap is deliberate.** ADR 0003 says exactly one row
per family is active; a partial unique index gives **at most** one. The "at least one" half cannot be
expressed as a row-level constraint at all — a family with zero active rows is a valid intermediate
state during any publish. Enforcing it belongs to whatever validates a version at publish time, which
is stage 3, and does not exist yet. Build the at-most-one index, and do not attempt a trigger or a
deferred constraint to close the other half.

**`scope` is a family-level invariant that nothing here enforces.** `backend/CONTEXT.md:27` says
Definition Scope is "fixed for the whole family", but scope is stored per row and there is no family
table to hold it, so a family whose rows disagree is expressible. This is the same shape as the
"at least one" problem above and has the same answer: do **not** build a trigger for it. Record it as
a known unenforced invariant, and note that publish-time validation in stage 3 is where it belongs —
alongside "at least one active row" and "at least one initial State".

**The entities do not extend `AuditEntity`.** Definitions are immutable configuration and ADR 0003
places no audit requirement on them; inheriting audit columns would also drag
`MockUserDetailsService` into every repository test for no gain.

These are the **first** partial indexes and the first unique indexes in this codebase — two
`CREATE INDEX` statements exist in total, neither with a `WHERE` clause, and zero
`CREATE UNIQUE INDEX`. There is no prior art for the syntax here, which is exactly why every one of
the rules above is proven by a test that violates it rather than assumed from the DDL reading
correctly.

## Blocked by

None - can start immediately.

## Outcome

**Landed on `feat/state-machine-implementation`.** Five new files, no existing file touched:

| File | What |
|---|---|
| `backend/src/main/resources/db/migration/V36.0__add_lifecycle_definition_table.sql` | The table, the `(family_key, version)` unique constraint and the two partial unique indexes |
| `backend/src/main/java/eu/bbmri_eric/negotiator/lifecycle/definition/LifecycleDefinition.java` | Package-private entity |
| `.../lifecycle/definition/DefinitionScope.java` | Package-private enum, `NEGOTIATION` \| `RESOURCE` |
| `.../lifecycle/definition/LifecycleDefinitionRepository.java` | Package-private repository |
| `backend/src/test/java/eu/bbmri_eric/negotiator/lifecycle/definition/LifecycleDefinitionRepositoryTest.java` | 11 tests against a real Postgres 16 |

The test lives in the entity's own package rather than `integration/repository/`, because package-private
types are not visible from there. That is the `template/` and `webhook/` precedent.

### Evidence

- `LifecycleDefinitionRepositoryTest`: **11 tests, 0 failures**.
- Full backend suite: **147 classes, 1343 tests, 0 failures, 0 errors, 16 skipped**.
- Parity gate re-run from an emptied reports directory: **24 classes, 255 tests, 0 failures, 1 skipped** —
  the unchanged count from [parity-gate.md](../../state-machine-implementation/parity-gate.md).
- `git status` shows five new files under `backend/` and **zero modified files**.

### Each rule ran red before it ran green

The unique constraint and both partial indexes were written *after* the tests that violate them, and the
three refusal tests were observed failing with "Expected DataIntegrityViolationException to be thrown,
but nothing was thrown" while the DDL still lacked them. The accepted cases were green throughout, so
none of them can be passing because a constraint is missing.

Each refusal test differs from its seed row **only in the constrained dimension** — the duplicate-version
rows are both inactive, the two active rows carry different version numbers, the second global default
sits in another family — so no other index can be the source of the refusal.

### Two things the ticket asked for that a test cannot carry

- **`ON DELETE RESTRICT`**: no foreign key points at this table yet, so the rule survives as a note at
  the foot of `V36.0` for slices 02–05 to follow.
- **`family_key` immutability**: `@Setter(AccessLevel.NONE)` plus `updatable = false` makes an attempted
  change a *compile* error, so there is no runtime refusal to assert. What is asserted instead is the
  observable half — `update_toTheEditableFields_leavesTheIdentityColumnsUntouched` renames a family and
  flips `active`, then proves `family_key`, `version` and `scope` are exactly where they were. The
  repository javadoc records why there is deliberately no `findByFamilyKeyAndVersion`.

### Known unenforced invariants, recorded per the Notes

Written at the foot of `V36.0`, where a reader of the DDL asks why they are absent: the *at least one*
half of "exactly one active version per family", and `scope` being fixed for a whole family while stored
per row. Both belong to publish-time validation in stage 3. No trigger, no deferred constraint.
