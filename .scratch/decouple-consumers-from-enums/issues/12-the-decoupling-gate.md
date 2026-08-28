# The decoupling gate

Status: resolved

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The slab gate in executable form. Map ticket 07 states it as a sentence; this slice makes it a test,
so that "decoupled" is a checked fact rather than a claim in a commit message and so that the
property cannot rot between here and the cutover.

**Two rules, because coupling can be spelled two ways.**

The *identifier rule* scans production sources and fails on any whole-word reference to one of the
four Lifecycle enums from outside the Spring Statemachine package and the three named metadata DTOs.
Word boundaries are load-bearing: two application event types have names that contain an enum's name
as a prefix, consumers legitimately name them, and the existing characterization guard already
documents that exact trap.

The *signature rule* is the one an identifier scan cannot express, and the reason this slice exists
rather than being a grep in a pull request. It checks that the accessors on both application events
and the methods of both Lifecycle service interfaces deal in `String`. Without it the guard goes
green over a codebase where every consumer reaches an enum through a method call, importing nothing
— which is precisely the failure slab 08 hit with its table rule and wrote down for the next slab to
avoid.

Closing this slice closes the slab. Map ticket 07 is then resolved and the map's Decisions-so-far
gains its entry.

## Acceptance criteria

- [x] The identifier rule scans all production sources and exempts only the Spring Statemachine
      package and the three metadata DTOs, each exemption named and justified in the source.
- [x] The signature rule covers both application events' accessors and both Lifecycle service
      interfaces.
- [x] An anti-vacuity test fails if the scan root resolves wrongly, if the walk returns nothing, or
      if a named exemption no longer exists.
- [x] The guard's failure message names the file and line and says what the reader must do.
- [x] Both rules are green with no exemption beyond the three metadata DTOs and the package.
- [x] `DefinitionInertnessGuardTest` is still in the tree and still green — this slab read no
      Definition Version table.
- [x] Slice 3's raw-literal guard is still green with no amendment.
- [x] Full backend suite green; parity **255 tests in 24 classes, 0 failures, 1 skipped**; deltas
      **8 tests, 0 failures**.
- [x] The app runs and the Negotiation list, the Negotiation page and the Information Requirement
      admin screens work, verified by hand per standing decision 5 — with one honest shortfall
      recorded in `STATUS.md`: the endpoints behind all three screens were driven with a real user
      token against a real Postgres, and the frontend was confirmed to serve, but no human looked at
      a rendered page.
- [x] `STATUS.md` records the per-slice evidence, and map ticket 07 is resolved with an entry in the
      map's Decisions-so-far.

## Notes

**Why an end-gate rather than a ratchet.** The alternative was writing this guard in slice 1 with
every subsystem exempted, and having each migrate batch delete its own exemption — a continuous
proof of progress rather than a single check at the close. It was rejected because every slice would
then edit the guard's exemption list, and a reviewer cannot easily tell a legitimate removal from a
sloppy one. Slab 08's inertness gate is the precedent followed here.

**Prior art** is `CharacterizationImportGuardTest` and `DefinitionInertnessGuardTest`: a
working-directory-resolved scan root, comment blanking, named exemptions, a violation report with
file and line, and an anti-vacuity test. Copy rather than extract — slab 08 recorded why, and it
applies here too: these guards have different lifetimes and each is meant to be deleted whole.

**This guard outlives the slab and is deleted at cutover**, together with the enums it forbids
naming. Say so in its javadoc.

**The frontend needs no change and this is checkable, not hopeful.** The wire format is identical and
the frontend generates no client from the published schema — it hand-codes the name strings it uses.
Standing decision 5 still requires running the app and looking.

## Resolution

**`eu.bbmri_eric.negotiator.lifecycle.LifecycleEnumDecouplingGuardTest`, seven tests, no production
file touched.** It sits beside `WellKnownNamesTest` and `RawStateNamesInSqlGuardTest`. Both rules are
green with exactly the two exemptions the PRD names — the Lifecycle package and the three metadata
DTOs — and nothing else. `DefinitionInertnessGuardTest` is untouched and green at 6; slice 3's
raw-literal guard is green at 7 with no amendment.

**Every failure path was run red on purpose, and that is the part worth reading.** Seven injected
violations, one per way each rule can fail or go vacuous; the table is in `STATUS.md`. The two that
justify the slice: an enum-returning method added *inside* the exempt package reported through the
signature rule while the identifier rule stayed green — the exact failure mode this slice exists to
close — and misspelling one exemption made the identifier rule report the DTO behind it, which is
the evidence that the exemption rather than an inert scanner is what makes the rule green.

**One thing was added beyond the acceptance criteria, and it changed the design.** Slice 11 recorded
that a guard checking a property through its own scanner needs the scanner checked separately, and
that "the tree no longer contains an example" is exactly when that stops holding by accident. The
identifier rule was already covered both ways. The signature rule was not: its detector only ever
sees four types that mention no enum, so a walker that always returned an empty set would report
green forever. A private `CoupledSeamFixture` interface now supplies the three shapes it must
catch — enum as a type argument, as a bare return type, as a parameter. Nothing implements it and
nothing scans it.

**Two readings taken deliberately, both recorded in `STATUS.md`.** "All production sources" means
`src/main/java`, after checking that a whole-word scan of `src/main/resources` returns zero hits and
noting that the exemption model is package-shaped. And an exemption whose file stops naming an enum
**fails** the guard rather than passing quietly, so that ticket 04's decoupling of a metadata DTO
must delete the exemption in the same diff.

**The app run discharges standing decision 5 for the whole slab, not for this slice.** Nothing here
could break a screen — no production file was touched — but no earlier slice ran the app, so the
check is done once at the tip against everything slices 01–11 changed. Startup is the strongest part
of it: Hibernate validates every named query at context build, which is where slice 11's entity and
JPQL changes would have failed. The full endpoint table is in `STATUS.md`; the two that matter most
are `?status=NOT_A_STATE` still answering **400** through slice 11's validator, and
`POST /v3/info-requirements` with an unknown Event name still answering **400** through slice 07's
deserializer. The PDF renders.

**Verification:** full backend suite 1487 tests in 160 classes, 0 failures, 0 errors, 16 skipped;
parity 255 tests in 24 classes, 0 failures, 0 errors, 1 skipped; intended deltas 8 tests, 0 failures,
0 errors, 0 skipped — all three partitioned out of one `--all` run. Map ticket 07 is resolved and the
map's Decisions-so-far carries its entry.

## Blocked by

- Slices [01](01-well-known-name-holders.md), [02](02-enum-backed-lifecycle-catalog.md),
  [03](03-pin-the-raw-state-names-in-sql.md),
  [04](04-webhook-payloads-name-states-as-strings.md),
  [05](05-notification-handlers-name-states-as-strings.md),
  [06](06-network-statistics-name-states-as-strings.md),
  [07](07-information-requirements-name-their-event-as-a-string.md),
  [08](08-dtos-mappers-and-the-negotiation-timeline.md),
  [09](09-resource-governance-names-states-as-strings.md),
  [10](10-the-lifecycle-seam-deals-in-strings.md),
  [11](11-entities-and-jpa-queries-name-states-as-strings.md) — all of them.
