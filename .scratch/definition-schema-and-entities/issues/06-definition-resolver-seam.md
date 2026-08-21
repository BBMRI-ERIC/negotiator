# The DefinitionResolver seam

Status: resolved

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

An interface answering "which Lifecycle Definition does this new work run under", with a deliberately
trivial body: the Global Default Family's active definition for `RESOURCE`, the sole active
definition for `NEGOTIATION`.

This exists to be a **seam**, not a feature. Stage 2 replaces the body with ADR 0004's precedence walk
— direct association, then Network, then Global Default — and ADR 0007 already localizes all
resolution into the Spawn Action, so keeping the interface means stage 2 replaces a body rather than
a call graph.

**Do not inline the repository lookup into the Spawn Action.** The map ticket names that as the one
way this shortcut would cost real work later.

Nothing calls the resolver in this slab. It exists, it is tested, and it is unreferenced by
production code — which is a deliberate state, not an oversight.

## Acceptance criteria

- [x] The interface and its implementation live in the new definition package.
- [x] Resolving for `RESOURCE` returns the active definition of the family flagged
      `is_global_default`.
- [x] Resolving for `NEGOTIATION` returns the sole active `NEGOTIATION`-scope definition.
- [x] Behaviour is specified and tested for the case where no such definition exists — the schema is
      empty until the seed lands, so this is the *normal* state throughout this slab, not an edge
      case.
- [x] No production code outside the definition package calls it.
- [x] Full suite green, parity count unchanged.

## Notes

**Visibility:** package-private like everything else in this slab. That is not in tension with it
being a seam — nothing calls it *yet*, and the coupling slab that wires it into
`SPAWN_RESOURCE_LIFECYCLES` widens it deliberately at that point, which is a visible decision rather
than a default. Slice 07's guard test would otherwise flag it.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)

## Outcome

**Landed on `feat/state-machine-implementation`.** Java only: the resolver seam needs no DDL, so this
slice adds no migration and `V36.5` stays free for whoever needs it next. Nothing outside
`eu.bbmri_eric.negotiator.lifecycle.definition` was touched, and nothing inside it is reachable from
outside — the interface, its implementation and its exception are all package private.

| File | What |
|---|---|
| `backend/src/main/java/.../lifecycle/definition/DefinitionResolver.java` | The seam. `resolveForNegotiation()` and `resolveForResource()`, each returning a `LifecycleDefinition` |
| `.../lifecycle/definition/DefinitionResolverImpl.java` | `@Service` holding the trivial body: two queries and a check that the answer is exactly one row |
| `.../lifecycle/definition/DefinitionResolutionException.java` | Unchecked and package private. Thrown when the configuration cannot name exactly one definition |
| `.../lifecycle/definition/LifecycleDefinitionRepository.java` | Two derived finders: `findByScopeAndActiveTrue` and `findByScopeAndActiveTrueAndGlobalDefaultTrue` |
| `backend/src/test/java/.../lifecycle/definition/DefinitionResolverTest.java` | 5 tests, mocked repository, no container |
| `backend/src/test/java/.../lifecycle/definition/LifecycleDefinitionRepositoryTest.java` | 6 finder tests added to the existing 11 |

### Evidence

- `DefinitionResolverTest` **5 tests, 0 failures, 0 errors, 0 skipped**.
- `LifecycleDefinitionRepositoryTest` **17 tests, 0 failures, 0 errors, 0 skipped** — slice 01's 11
  plus exactly these 6.
- **Full suite: 154 classes, 1409 tests, 0 failures, 0 errors, 16 skipped.** Summed from
  `surefire-reports` with the directory cleared before the run, so the aggregate is that run alone.
  1409 is slice 05's 1398 plus exactly these 11.
- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — unchanged, run in its
  filtered form with `-DexcludedGroups=intended-delta`. No report was written for
  `IntendedDeltasAdr0005WillInvertTest`, which is how the gate document says to verify the split.
  Count the reports by hand and note that 19 of the files in that directory are `*-output.txt`
  stdout captures rather than test reports: 43 files, 24 of them reports.
- `fmt-maven-plugin:format` reports 0 of 619 files reformatted on the final tree.
- Nothing outside `lifecycle.definition` names `DefinitionResolver`, `DefinitionResolverImpl` or
  `DefinitionResolutionException`, by grep over `backend/src`. The slab stays inert by reference —
  though see the `@Service` note in `STATUS.md`, which is a weaker claim than inert by instantiation.

### The shape was three decisions the ticket did not make

The ticket fixed the two answers and said nothing about the shape of the question. Two methods rather
than one taking a `DefinitionScope`; `resolveForResource()` taking no Resource yet; and an exception
rather than an `Optional` when nothing resolves. All three are Java-only and inside a package no
production code outside may name, so by the slab's ambiguity rule they were decided and filed rather
than escalated:
[10 The DefinitionResolver's shape is a guess until something calls it](10-definition-resolver-shape-is-a-guess.md),
which records each one with what would make it wrong.

The one worth repeating here is the exception. Resolution is meant to be *total* — exactly one family
carries the Global Default flag, which is what lets Spawn initialize every Resource of a Negotiation
or none — so an `Optional` would have invited the first caller to write an "unresolved" branch that
must not exist. Both methods therefore return a definition, and the empty schema this slab leaves
behind is a refusal rather than an absence.

### The refusal is the whole of the observable behaviour today

The schema holds no Definition Versions until the v1 seed lands, so both methods can currently only
throw. That is why the two refusal tests are not edge cases and why the messages are worth their
length: they name the scope, and for the plural case the colliding `family_key`s, because an admin
reading a stack trace is the only consumer this code has for now.

### Why the tests are split across two classes

`DefinitionResolverTest` mocks the repository and is about the decisions the resolver makes once rows
are in front of it — which of the two queries it runs, and what it does with none, one, or two rows.
A mock cannot show *which* rows either query selects: it would happily return an inactive row from a
finder whose name promises active ones. So the six finder tests are in
`LifecycleDefinitionRepositoryTest` against a real Postgres 16, one per predicate the two queries
carry, and the two classes' javadocs each point at the other so the split does not read as an
accident.

`findByScopeAndActiveTrue_returnsAnActiveVersionOfEveryFamilyOfTheScope` is the one that ties them
together: it proves the plural input the resolver refuses is *reachable*, since the database enforces
one active version per family and says nothing about one active version per scope. It cannot pass
vacuously — if that state were impossible, the second `saveAndFlush` would be refused and the test
would error rather than pass.

### Each rule ran red before it ran green

| Mutation | Result |
|---|---|
| Both derived finders overridden with a `@Query` weakened by one predicate each — `WHERE d.scope = ?1` for the first, dropping `active`; `WHERE d.globalDefault = true AND (d.scope = ?1 OR 1 = 1)` for the second, which drops `active` and neutralizes `scope` while keeping the parameter bound | Exactly 3 red of the 16 tests the class then held: `findByScopeAndActiveTrue_returnsTheActiveVersionAndNotTheSupersededOne` (`expected: <1> but was: <2>`), `..._ignoresASupersededDefault` and `..._ignoresOneOfTheOtherScope`. The two tests whose predicate survived — `findByScopeAndActiveTrue_ignoresAnActiveVersionOfTheOtherScope` and `..._returnsTheFlaggedFamilysActiveVersion` — stayed green, which is what makes the attribution readable |
| Both resolver bodies replaced with `findByScopeAndActiveTrue(scope).getFirst()` | Exactly 4 red of 5: both refusals became `NoSuchElementException` instead of `DefinitionResolutionException`, the plural case threw nothing at all, and `resolveForResource_withAnActiveGlobalDefault_returnsIt` errored with `NoSuchElementException` — the mutated body asks the finder that test never stubbed, so it gets an empty list and `getFirst()` throws before strict stubbing can report the now-unused global-default stub. Either way that red is the proof that the resource path asks for the global default rather than for any active Resource-scope definition. Only `resolveForNegotiation_withOneActiveDefinition_returnsIt` stayed green |

The seventeenth test in the repository class was added after the first mutation ran and is unaffected
by it: it seeds two active Negotiation-scope versions and asserts a count of 2, which dropping the
`active` predicate leaves at 2.
