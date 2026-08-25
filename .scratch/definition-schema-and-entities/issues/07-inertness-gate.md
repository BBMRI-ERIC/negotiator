# The inertness gate

Status: done

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

The slab's gate, as a **test rather than a claim**: proof that no production code path reads any of
the new schema. If something reads these tables, the slab has failed — that is the whole point of
landing additive DDL on its own, "harmless if it lands alone".

A mechanical test scans the production source tree and fails on any reference to the new types from
outside the definition package. Reuse the technique already proven in this repo by
`CharacterizationImportGuardTest` — plain JUnit text-scanning with comment-blanking, a
working-dir-resolved scan root, named exemptions, and an **anti-vacuity test** so the guard cannot
silently pass by scanning nothing.

A one-off grep is not acceptable here. It would pass today and rot the moment the next slab starts.

Package-private entities and repositories already give part of this enforcement for free at compile
time; the test covers what package-privacy cannot — reflection, string references, and anything a
later slice makes public without thinking.

## Acceptance criteria

- [x] A guard test fails when a reference to any new definition type is added to production code
      outside the definition package, demonstrated by a temporary violation during development.
- [x] The guard has an anti-vacuity test proving it actually scanned files.
- [x] Any exemption is named explicitly in the test, with a reason.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is green
      at its unchanged count of 255, and the intended-delta tests are unchanged.
- [x] `git diff` against the slab's base shows the only production changes are the new package, the
      new migration, and the two pin columns on the two existing entities.
- [x] The migration applies cleanly from an empty database — which every Testcontainers test already
      proves, since test contexts run clean+migrate on every build.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)
- [02 The state and event tables](02-state-and-event-tables.md)
- [03 The transition table](03-transition-table.md)
- [04 Guard wiring and action wiring](04-guard-and-action-wiring.md)
- [05 The Definition Version Pin columns](05-lifecycle-definition-pin-columns.md)
- [06 The DefinitionResolver seam](06-definition-resolver-seam.md)
