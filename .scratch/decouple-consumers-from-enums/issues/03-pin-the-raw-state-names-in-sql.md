# Pin the raw State names in SQL

Status: ready-for-agent

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

- [ ] A guard test records the fourteen literals, each attributed to its file and its reason.
- [ ] The guard scans for **bare names, not names inside quotes**. Eleven of the fourteen live in
      Java text blocks, so a quoted-literal regex finds none of them — the sweep that produced this
      slice made exactly that mistake first.
- [ ] The guard fails when a literal is added, removed or moved, with a message naming the file and
      line and saying what the reader must decide.
- [ ] An anti-vacuity test fails if the scan root resolves wrongly or the walk returns nothing.
- [ ] The unquoted JPQL reference is recorded separately, with its different failure mode stated.
- [ ] No query text is changed anywhere in this slice.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**Why this lands early rather than with the network statistics slice.** The guard covers two files,
one of which belongs to slice 11. Pinning before any slice touches either means an accidental edit
fails immediately, at the slice that caused it, rather than at the close of the slab.

**Prior art for the scanning technique** is `CharacterizationImportGuardTest` and
`DefinitionInertnessGuardTest` — a working-directory-resolved scan root, comment blanking so prose
naming a literal is not itself a violation, a violation report carrying file and line, and named
exemptions. Slab 08's `STATUS.md` records why the two existing guards were copied rather than
extracted: different lifetimes, each meant to be deleted whole. The same applies here.

**This guard outlives the slab.** Two downstream slabs consume what it records: the migration slab's
seed must satisfy these names, and ADR 0008's conversion of the audit table's column to a foreign
key breaks the four literals that filter on it. One of them is named in both places, so it breaks
twice, in two different slabs.

## Blocked by

None - can start immediately. Independent of slices 1, 2 and 7.
