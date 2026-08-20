# Guard wiring and action wiring

Status: resolved

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

**Two separate wiring tables, not one polymorphic one.** Guards and Actions never interleave — Guards
run before a commit, Actions only after one — so a shared ordering would be meaningless, and only
Guards need a scope.

**Guard wiring** has a **nullable** transition reference: null means the Guard applies to every
Transition of the Lifecycle Definition, set means that Transition alone. Two partial unique indexes
keep the ordering column unique within each of those two scopes independently. The effective Guard
chain for a Transition — what admin tooling must eventually show — is then one query.

**Action wiring** is transition-scoped only and carries **no definition reference at all**, since the
Transition already implies it.

Both carry a string type key naming a Java strategy, and per-strategy `params` as jsonb. Runtime
domain state never travels in `params` — it reaches a strategy through the evaluation context at fire
time. Giving Actions params is what collapses today's three post-visibility Action classes into one
typed key with a scope and a flag.

The ordering column is `sort_order`, not `order`: no identifier is double-quoted in any of the fifty
existing migrations and `order` would have to be. The order *between* the two scopes is pipeline
logic, not a column.

## Acceptance criteria

- [x] Both tables are created by **this slice's own additive migration file** (one file per slice,
      never appended to an earlier slice's file). Action wiring has no definition FK.
- [x] Guard wiring's transition FK is nullable, and both shapes round-trip in a repository test.
- [x] Two partial unique indexes on Guard wiring: a test inserts a duplicate `sort_order` within the
      definition-wide scope and asserts refusal, inserts a duplicate within one Transition's scope
      and asserts refusal, and asserts that the **same** `sort_order` in the two different scopes is
      accepted.
- [x] `params` round-trips a non-trivial JSON payload unchanged, mapped as jsonb.
- [x] A wiring row with null `params` is accepted.
- [x] Entities and repositories are package-private.
- [x] Full suite green, parity count unchanged.

## Notes

The frozen graph dump carries **21 of 21 Transitions with no Guard**, so no v1 data will exercise
Guard wiring — the imperative IN_PROGRESS gate it will eventually replace lives in a service today,
not in the graph. That makes these tests the *only* evidence the tables work, so do not thin them.
Full analysis in [recon-expressiveness.md](../recon-expressiveness.md) gaps 8 and 9.

## Blocked by

- [03 The transition table](03-transition-table.md)

## Outcome

**Landed on `feat/state-machine-implementation`** in two commits: `0ee83e92` added the seven files
below, and a follow-up revised the two test classes against review (see *Review findings acted on*).
No file outside this slice was touched, though the migration does `ALTER` slice 03's `transition`
table — the composite FK target, exactly as slice 03's handoff asked for.

| File | What |
|---|---|
| `backend/src/main/resources/db/migration/V36.3__add_guard_and_action_wiring.sql` | `guard_wiring` (nullable `transition_id`, two partial unique indexes on `sort_order`, composite FK against cross-definition straddling), `action_wiring` (no definition FK, `UNIQUE (transition_id, sort_order)`), plus `UNIQUE (lifecycle_definition_id, id)` on `transition` so the pair is referenceable |
| `.../lifecycle/definition/GuardWiring.java` | Package-private entity, `typeKey` immutable |
| `.../lifecycle/definition/GuardWiringRepository.java` | Package-private repository, no finders |
| `.../lifecycle/definition/ActionWiring.java` | Package-private entity, `typeKey` immutable |
| `.../lifecycle/definition/ActionWiringRepository.java` | Package-private repository, no finders |
| `backend/src/test/java/.../GuardWiringRepositoryTest.java` | 12 tests against a real Postgres 16 |
| `backend/src/test/java/.../ActionWiringRepositoryTest.java` | 6 tests against a real Postgres 16 |

### Evidence

- `GuardWiringRepositoryTest` **12 tests**, `ActionWiringRepositoryTest` **6 tests**, 0 failures.
- **Full suite: 152 classes, 1389 tests, 0 failures, 0 errors, 16 skipped.** Measured by summing
  the `surefire-reports` totals, with the directory cleared first so the aggregate is that run
  alone, and every report's mtime checked to be after the run began.
- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped**, run separately in its
  filtered form with `-DexcludedGroups=intended-delta`. Unchanged.
  The full-suite run above carries the unfiltered form of the same package, **25 classes, 263
  tests, 1 skipped**, which is the number the gate document gives for dropping both flags — the
  difference being the 8 tests of `IntendedDeltasAdr0005WillInvertTest`, the one class the group
  filter excludes. Both halves therefore green and unchanged.
- `fmt-maven-plugin:check` clean across all 613 files.
- Nothing outside `lifecycle.definition` references either type or table, and neither Flyway seed was
  touched, so the slab stays inert.

### Each rule ran red before it ran green

Every refusal test was proven by temporarily removing the constraint it covers and watching that test
— and only that test — fail:

| Constraint removed | Test that went red |
|---|---|
| Both partial unique indexes replaced by one non-partial `(definition, transition, sort_order)` | `save_withDuplicateSortOrderInDefinitionScope_isRefused` ("nothing was thrown"), and `save_withDuplicateSortOrderInTransitionScope_isRefused` on its index-name assertion |
| Composite FK replaced by a plain `FOREIGN KEY (transition_id)` | `save_withATransitionFromAnotherDefinition_isRefused` |
| `uq_action_wiring_transition_sort_order` dropped | `save_withDuplicateSortOrderOnTheSameTransition_isRefused` |
| `action_wiring.transition_id` NOT NULL dropped | `insert_withoutATransition_isRefusedByTheDatabase` |
| `guard_wiring.lifecycle_definition_id` NOT NULL dropped | `insert_withoutADefinition_isRefusedByTheDatabase` |
| `fk_guard_wiring_lifecycle_definition` dropped | `insert_withADefinitionThatDoesNotExist_isRefusedByTheDatabase` |

The naive-index mutation is the interesting one. It leaves **every** acceptance test green and only
the definition-scope duplicate goes red, because PostgreSQL treats nulls as distinct in a plain
unique index — so a single non-partial index looks correct until exactly that row is tried. It also
failed `...InTransitionScope...` on the index-name assertion rather than on the throw, which is why
those `contains("uq_...")` assertions are kept: they detect the wrong index doing the work.

The last two rows were run as two separate mutations rather than one, so each test is attributable to
a single constraint: dropping the NOT NULL leaves the `-1` insert still refused by the FK, and
dropping the FK leaves the NULL insert still refused by the NOT NULL. Each run produced exactly one
failure and no collateral.

### The nullable `transition_id` leaves a hole, and it needed two tests rather than a comment

Slice 03's handoff warned that PostgreSQL's default `MATCH SIMPLE` skips a composite FK entirely once
either column is null. For a definition-scoped Guard that is the wanted behaviour — but it means the
composite FK does nothing for those rows, so `fk_guard_wiring_lifecycle_definition` is the only thing
tying them to a real Definition Version. Unlike slice 03's definition FK, which was transitively
implied by its NOT NULL composite references and therefore untestable, this one **can** fail.
`save_definitionScoped_roundTrips` shows the null shape accepted and
`insert_withADefinitionThatDoesNotExist_isRefusedByTheDatabase` shows the surviving FK still refusing.

### Proving `params` is jsonb, not merely a string that round-trips

`SELECT params::text` returns identical bytes from a `text`, `json` or `jsonb` column, so an
assertion against a payload that is already canonical proves nothing about the mapping. Both payloads
now go in **non-canonical** — keys out of order, no spaces — and the assertion is the canonical form.
Only jsonb rewrites it: object keys normalise to **length-then-bytewise** order (not alphabetical —
`{"b":1,"aa":2,"a":3}` becomes `{"a": 3, "b": 1, "aa": 2}`) and separators are re-spaced, while array
element order is preserved. Both payloads are nested and carry an array, which is the other half of
the criterion's "non-trivial".

### Two tables, and what stays out of them

Guards and Actions never interleave, so a shared ordering would be meaningless and only Guards need a
scope. `action_wiring` therefore carries no definition reference at all and cannot straddle
definitions by construction — there is no column with which to do it, so there is nothing to test.
`sort_order` is the column name because `order` is reserved and would have been the first
double-quoted identifier in the whole migration history — no identifier is quoted in any of the 54
files, and the only double quotes anywhere are inside comments. The order *between* the two chains is pipeline
logic, not a column.

Neither repository has a finder. The "effective Guard chain for a Transition is one query" claim in
the ticket describes what the schema makes possible, not something this slice builds; the two partial
indexes are what make both halves of that query indexed.

### Review findings acted on

- **Composite FK added** on `(lifecycle_definition_id, transition_id)` → `transition
  (lifecycle_definition_id, id)`, so a Guard cannot reference a Transition in another definition.
  Slice 03's pattern, and its handoff had already reserved the `UNIQUE (lifecycle_definition_id, id)`
  on `transition` for this slice to add.
- **`typeKey` made immutable** (`@Setter(AccessLevel.NONE)`, `updatable = false`): it names a strategy
  from a fixed catalogue, so changing it makes a different wiring rather than an edit — matching
  `State.name`.
- **Two Action fixtures used type keys the design abolishes.** `DISABLE_POSTS` is one of the three
  post-visibility Action classes that `params` exists to collapse into `SET_POST_VISIBILITY`, so a
  test using it demonstrated the opposite of the design point. Both rows now share the one typed key
  and differ only in `params` — which is also the realistic collision the collapse creates, and
  leaves `sort_order` as the only constrained column the refusal can be about.
- **A Guard fixture used an Action's type key.** `SET_POST_VISIBILITY` is a side effect, not a
  predicate; the glossary keeps Guard and Action strictly distinct. Now `REQUIREMENT_MET`, with
  Guard-shaped params.
- **The jsonb assertions could not fail on the wrong column type.** Rewritten as described above.
- **Guard wiring had no database-level NOT NULL or FK test** where its sibling did, and the slab's own
  rule is that such a column is proven through `JdbcTemplate` because the v1 seed is SQL. Two tests
  added.
- **`otherTransition` read as belonging to `otherDefinition`** but was deliberately in the same one,
  which is what makes the scope test meaningful. Renamed `secondTransition` and moved beside
  `transition`.
- **No `ON DELETE RESTRICT` test was added.** Slice 03's handoff records that `RESTRICT` cannot be
  told apart from PostgreSQL's default `NO ACTION` by any test — both refuse — so such a test defends
  the intent, not the letter. The seven refusal tests here all distinguish a real constraint from its
  absence; an eighth that cannot would weaken the set rather than strengthen it.

### The shared test fixture: decided, not deferred again

Slice 03 asked this slice to decide rather than drift. The duplication is now real: `definitionIn` in
**5** copies, `eventIn` in 4, `stateIn` in 3, `STANDARD_FAMILY` in 6, across all six repository tests
in the package.

**Decision: extract it, in its own commit, before slice 05** — a package-private
`DefinitionFixtures` beside the tests, holding the family constants and the three helpers that
return a *built* entity (`definitionIn`, `stateIn`, `eventIn`) — those three return entities, not
builders. Of the two that do return a builder, `versionBuilder` stayed inline because it is a
different fixture rather than `definitionIn` short of its `build()`, and `stateBuilder` came along
because `stateIn` is now defined in terms of it. Executed after this slice's three commits; the
landed shape is in `STATUS.md`.
Not folded into this slice, for two reasons: it rewrites slices 01-03's landed tests, which is a
change with its own blast radius and deserves to be reviewable on its own; and `recon-conventions`
§4.3's "this codebase keeps repository-test fixtures inline" was an argument about *one* test, not
about six sharing three helpers and two constants. Slice 05 adds no new copy — it touches `negotiation` and
`negotiation_resource_link` — so extracting first costs nothing and stops the seventh copy.
