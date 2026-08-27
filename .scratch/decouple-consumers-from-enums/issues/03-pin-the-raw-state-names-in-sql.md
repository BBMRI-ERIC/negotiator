# Pin the raw State names in SQL

Status: resolved

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

Ticket 03 hands this slab an explicit obligation: some State names reach the database as raw SQL
text, the compiler cannot see them, and therefore **"it builds" is not evidence that this slab is
done**. This slice discharges that obligation as a standing fact rather than a one-off grep, which
would pass today and rot by the next slice.

A mechanical guard that scans production sources for State and Event names appearing as query text,
holds the known set as an expected list — each with the file and the reason it is there — and fails
when a literal appears, disappears or moves.

The measured population is **fourteen literals across two files**, which is not what ticket 03
recorded. It attributed all of them to the network statistics repository, missed one of that file's
lines entirely, and missed both literals in the Negotiation repository's native query. A third name
there sits in JPQL unquoted, which fails loudly at Hibernate query validation once the column's Java
type changes — that is the safe one, and the guard should record why it is different rather than
treat all fifteen alike.

Nothing in this slice changes a query. The SQL is deliberately untouched by this whole slab.

## Acceptance criteria

- [x] A guard test records the fourteen literals, each attributed to its file and its reason.
      `RawStateNamesInSqlGuardTest.PINNED_NAMES`; a test asserts every entry carries a non-blank
      reason and sits on the line it claims.
- [x] The guard scans for **bare names, not names inside quotes**. It reads the *content* of Java
      strings and matches whole words inside it, so SQL quoting is recorded rather than required.
      Correction: **thirteen** of the fourteen live in Java text blocks, not eleven —
      `NegotiationRepository:35` is a text block too. The point stands more strongly than stated:
      a `"DRAFT"` regex finds zero of the fourteen.
- [x] The guard fails when a literal is added, removed or moved, with a message naming the file and
      line and saying what the reader must decide. Moves are paired nearest-line-first (`:28 ->
      :29`), so a single inserted import does not present as eleven deletions plus eleven additions,
      and a name occurring three times in one file cannot pair with the wrong occurrence. All three
      failure modes were exercised against perturbed sources and then reverted.
- [x] An anti-vacuity test fails if the scan root resolves wrongly or the walk returns nothing.
      Both halves are standing tests, not one-off checks:
      `resolvingTheScanRoot_failsRatherThanFindingNothing` asks the resolution step for a directory
      that does not exist and asserts it refuses by name, and `guard_scansTheTreeItClaimsToPin`
      holds the 435-file walk against a floor of 100 and asserts the scan comes back non-empty.
- [x] The unquoted JPQL reference is recorded separately, with its different failure mode stated.
      `UNQUOTED_JPQL_REFERENCE`, plus `theUnquotedReference_isStillBare` — which fails if anyone
      quotes it, trading Hibernate's startup rejection for a filter that matches nothing.
- [x] No query text is changed anywhere in this slice. `git status` shows one added test file.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0. Measured after rebasing
      onto slice 07: suite 1453/0/0/16 across 158 classes, every report rewritten by the run;
      parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0.

## Notes

**Why this lands early rather than with the network statistics slice.** The guard covers two files,
one of which belongs to slice 11. Pinning before any slice touches either means an accidental edit
fails immediately, at the slice that caused it, rather than at the close of the slab.

**Prior art for the scanning technique** is `CharacterizationImportGuardTest` and
`DefinitionInertnessGuardTest` — a working-directory-resolved scan root, comment blanking so prose
naming a literal is not itself a violation, a violation report carrying file and line, and named
exemptions. Slab 08's `STATUS.md` records why the two existing guards were copied rather than
extracted: different lifetimes, each meant to be deleted whole. The same applies here.

**Corrections made while resolving.** Three numbers in this issue did not survive measurement, and
the corrected ones are what `STATUS.md` now carries:

- The population is **fourteen names, not fifteen** — thirteen SQL string constants plus the one
  bare JPQL reference. "All fifteen" counts the bare one twice.
- **Thirteen** of the fourteen live in Java text blocks, not eleven. `NegotiationRepository:35` is a
  text block as well. This strengthens rather than weakens the acceptance criterion it supports.
- ADR 0008's audit-column conversion breaks **six** literals, not four:
  `NetworkStatsRepositoryImpl:75` and `:216` and `NegotiationRepository:35`, two apiece.

Two facts the sweep did not record and slice 06 will want: `getMedianResponseForNetwork` exists
twice with identical SQL, and it is the `NegotiationRepository` copy that the statistics service
calls; and three of `NetworkStatsRepositoryImpl`'s queries — holding six of its eleven literals —
have no production caller at all.

**This guard outlives the slab.** Two downstream slabs consume what it records: the migration slab's
seed must satisfy these names, and ADR 0008's conversion of the audit table's column to a foreign
key breaks the ~~four~~ **six** literals that filter on it (corrected above). Those six are named in
both places, so they break twice, in two different slabs.

## Blocked by

None - can start immediately. Independent of slices 1, 2 and 7.
