# The vocabulary move and the amended inertness gate

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The prefactor that makes every other slice possible: move the two vocabulary enums somewhere the
evaluator can name them, and amend the gate that currently forbids it.

`DefinitionScope` and `RequiredAuthority` move out of the Lifecycle Definition package and up into
its parent, `public`, beside the three Well-known name holders. The entities keep using them and
nothing about their values, their persistence mapping or their column types changes. Every other type
in the definition package stays package-private, which is what keeps entities and repositories
structurally unreachable from the evaluator.

`DefinitionInertnessGuardTest`'s type rule forbids both names outside the definition package, so the
move requires amending it. Drop the two names from the forbidden list — not by adding an exemption —
and state the reason in the source: they are *vocabulary*, not schema, and they carry no persistence
behaviour beyond an annotation on a column that is not moving. The precedent is already inside that
guard: three type names are exempt from the bare form of the same rule because their names are taken
by other libraries, and their tables are caught by a narrower pattern instead.

Nothing else in the definition package is widened, and the amended javadoc says so.

## Acceptance criteria

- [ ] Both enums live in the Lifecycle package, are `public`, and are unchanged in values, order and
      persistence mapping.
- [ ] Every remaining type in the Lifecycle Definition package is still package-private.
- [ ] `DefinitionInertnessGuardTest`'s forbidden type list loses exactly two entries, with the reason
      stated in the source, and gains no exemption.
- [ ] The guard's package rule, table rule and both meta-tests are untouched and green at 6 tests.
- [ ] The javadoc records that no further widening of the definition package is sanctioned by this
      slab, and that the guard's deletion is still the cutover slab's.
- [ ] No production code outside the definition package names any other definition type.
- [ ] No Flyway migration is added.
- [ ] Full backend suite green; parity **255 tests in 24 classes, 0 failures, 1 skipped**; deltas
      **8 tests, 0 failures**.

## Notes

**Why a move rather than making them public in place.** Leaving them where they are and widening
their visibility still trips the guard's *package* rule, because any import spells the package out.
Moving them is the only option that keeps a single rule green rather than trading one violation for
another.

**Why not private copies in the evaluation package.** The type rule matches the bare name wherever it
is declared, so a copy trips it anyway, and it leaves two versions of a closed vocabulary to keep in
step. Recorded in the PRD as a rejected alternative; do not revisit.

**`guard_forbidsOnlyNamesThatStillExist` is what makes editing the list safe.** A forbidden name
whose file disappears fails the guard rather than passing quietly, so shortening the list cannot
silently stop protecting something.

## Blocked by

None - can start immediately.
