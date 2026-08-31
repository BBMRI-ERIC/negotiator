# The vocabulary move and the amended inertness gate

Status: resolved

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

- [x] Both enums live in the Lifecycle package, are `public`, and are unchanged in values, order and
      persistence mapping.
- [x] Every remaining type in the Lifecycle Definition package is still package-private.
- [x] `DefinitionInertnessGuardTest`'s forbidden type list loses exactly two entries, with the reason
      stated in the source, and gains no exemption.
- [x] The guard's package rule, table rule and both meta-tests are untouched and green at 6 tests.
- [x] The javadoc records that no further widening of the definition package is sanctioned by this
      slab, and that the guard's deletion is still the cutover slab's.
- [x] No production code outside the definition package names any other definition type.
- [x] No Flyway migration is added.
- [x] Full backend suite green; parity **255 tests in 24 classes, 0 failures, 1 skipped**; deltas
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

## Comments

**Landed as `f879867e`.** The move went in as a `git mv` plus a visibility change and eleven added
imports — five production files in the definition package, six test files beside them. No entity
field moved, so `@Enumerated(STRING)` and both `@Column` declarations are untouched, and the values
and their order are byte-identical to the versions at `73e76d4b`.

**The red step is worth recording, because it is the argument for editing the list rather than
exempting.** With the enums moved and the guard unamended, `DefinitionInertnessGuardTest` failed 2
of 6: the type rule on the two new files, and `guard_forbidsOnlyNamesThatStillExist` reporting
exactly `[DefinitionScope, RequiredAuthority]` as names it forbids whose files have gone. The second
failure is the safety net the ticket promised. `DISTINCTIVE_TYPE_NAMES` then went 14 → 12;
`NAMES_TOO_COMMON_TO_FORBID_BARE` is byte-identical, and no new list, pattern or predicate branch
was added.

**Three javadoc claims were corrected after code review, before the commit was finalised.** The
first draft justified the move by saying a caller must name a Definition Scope before it can ask
anything — which `DefinitionResolver`, in the same package, deliberately contradicts by taking no
scope parameter at all. The second claimed the guard "does not police" the Lifecycle package; it
does scan it, and only stopped forbidding those two names in it. The third overstated the parallel
with `NAMES_TOO_COMMON_TO_FORBID_BARE`, which is a different remedy answering the same question. All
three now say what is true. A fourth change dropped the name `LifecycleDefinition` from
`DefinitionScope`'s javadoc, so that file's compliance no longer rests on the guard's
comment-blanker.

**Verification.** Full backend suite 1492 tests in 160 classes, 0 failures, 0 errors, 16 skipped,
measured before the javadoc corrections. The corrections change comments only, so everything they
could reach was re-run at the final tip: parity 255 tests in 24 classes, 0 failures, 0 errors, 1
skipped; intended deltas 8 tests, 0 failures, 0 errors, 0 skipped; and all four source-scanning
guards green — `DefinitionInertnessGuardTest` 6, `CharacterizationImportGuardTest` 3,
`RawStateNamesInSqlGuardTest` 7, `LifecycleEnumDecouplingGuardTest` 8. `fmt-maven-plugin:check`
green across 631 files.

**One environment note for whoever picks up slice 02.** The Nix store on this machine is empty and
`nix-daemon` is disabled, so `nix develop .#opencode` fails with `creating directory '/nix/store':
Permission denied` and provides no `mvn`. These runs used a JDK 21 and Maven 3.9.9 placed outside
the repository, offline against the existing `~/.m2`. The flake is not at fault and was not changed.
