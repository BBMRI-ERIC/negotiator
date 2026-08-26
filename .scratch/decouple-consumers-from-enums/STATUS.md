# Slab status — decouple consumers from the Lifecycle enums

Working reference for the twelve slices of
[PRD — Decouple consumers from the Lifecycle enums](PRD.md), against map ticket
[07](../state-machine-implementation/issues/07-decouple-consumers-from-enums.md). Branch:
`feat/state-machine-implementation`.

Each slice adds a section for what it settled on behalf of the ones after it. Decisions recorded
here are settled; do not relitigate them in a later slice.

## Landed

| Slice | State | Evidence |
|---|---|---|
| [01 Well-known name holders](issues/01-well-known-name-holders.md) | **done** | 7 tests green; full suite 1434/0/0/16; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |

Parity and delta numbers are summed from `backend/target/surefire-reports`, filtered by mtime.
**That filtering is not optional here**, and this run showed why: `surefire-reports` is not cleared
between invocations, so after a full-suite run it holds a report for all 25 characterization
classes. The parity run rewrites 24 of them and correctly leaves
`delta.IntendedDeltasAdr0005WillInvertTest` untouched, because the tag excluded it - so a naive sum
over the directory reads 263 and looks like the parity count has moved. Take the mtime window of
the run you just did, then check that the classes outside it are the ones you expect to be stale.

## What slice 01 fixed for slices 04, 05, 08, 09 and 11

**Package: `eu.bbmri_eric.negotiator.lifecycle`.** The three holders are
`WellKnownNegotiationStates`, `WellKnownResourceStates` and `WellKnownResourceEvents`, all public
and all directly under `lifecycle`. Import from there.

The choice was open — the issue left it deliberately undecided — and four constraints closed it:

- **Not `negotiation/state_machine/`.** That package is deleted at cutover and these nine names
  outlive it. This was given.
- **Not `lifecycle/definition/`, and this is the load-bearing one.**
  `DefinitionInertnessGuardTest`'s package rule fails any production source outside that package
  naming `eu.bbmri_eric.negotiator.lifecycle.definition`. Holders placed one level deeper would
  therefore have taken the *whole slab* red the moment slice 04 imported one, and the only way out
  would have been deleting the inertness guard — a stage early, which the map forbids and PRD user
  story 4 depends on. `lifecycle` is the parent, so the guard's `Pattern.quote` on the fully
  qualified definition package does not match it. Verified: the full suite is green with the
  holders in place.
- **Not `common/`.** These are Lifecycle vocabulary, and `common` is where a name goes to stop
  meaning anything.
- **`lifecycle` reads naturally from all five consumer subsystems** — webhook, notification,
  network statistics, information requirements and resource governance — none of which owns the
  vocabulary, and all of which are peers of it.

The useful accident is that `lifecycle` is also where ADR 0002's model already lives. After the
cutover, `lifecycle.definition` holds what a State *is* and `lifecycle` holds the handful of names
behaviour reaches for by hand. Nothing about that arrangement has to change when Spring Statemachine
goes.

**Naming: `WellKnown<Scope><Kind>s`, plural, and deliberately unlike the enums.** A stale
`import ...NegotiationState;` cannot silently resolve against `WellKnownNegotiationStates`, and
`NegotiationState.DRAFT` → `WellKnownNegotiationStates.DRAFT` is a visible edit at every call site
rather than a change of meaning under a name that stayed the same.

**The growth rule is a test, not a convention.** `WellKnownNamesTest` asserts *set equality* on each
holder's constants, so adding a tenth name requires deleting a line of the test in the same diff.
This is the one thing the PRD, ticket 03 and the slice issue all independently warn about: a holder
that lists every State is the enum again with worse ergonomics. If a later slice finds a call site
whose name is not in the nine, the answer is almost always that the name is **data** — off the
column, the request or the Definition Version Pin — not that the holder is short one.

**The names are pinned against the enums while the enums still exist.** The same test resolves every
constant through `NegotiationState.valueOf` / `NegotiationResourceState.valueOf` /
`NegotiationResourceEvent.valueOf`. This is worth knowing about because it is the only cheap defence
against the failure mode of slices 04–11: a typo in a holder constant does not fail to compile, it
compiles into a comparison that silently stops matching, which is precisely the behaviour change the
slab promises not to make. At cutover the enums go and these three assertions go with them — as a
compile error, which is the loud case.

**Test placement.** The holders are public, so their test can live anywhere; it lives in
`eu.bbmri_eric.negotiator.lifecycle` beside them rather than under `unit/`. Enum references in test
scope are permitted by the slab gate, which is what makes the pinning above legal.

**Filter synthetic members before asserting on reflection.** The "no behaviour" rule first failed
against `getDeclaredMethods().length == 0` because JaCoCo instruments every loaded class with a
synthetic `$jacocoInit`; the compiled class on disk has only the constructor. Any later guard that
counts members must exclude `isSynthetic()`, or it passes under a plain compile and fails under the
suite.

**Nothing reads the holders yet, and that was checked rather than assumed.** After this slice the
only production occurrences of the three names are the declarations themselves plus two Javadoc
`{@link}` cross-references between holders. Slice 04 is the first real consumer.

**Whole-suite count moved 1415 → 1434, and only +6 of that is this slice.** The two test files this
slice touches are the new `WellKnownNamesTest` (+7) and `ConverterTest` (−1, the status-converter
method); `git status` confirms no other test file changed, and the deleted converter was production
code that no test but that one named. The 1415 recorded against the previous slab's slice 07 is
therefore stale relative to this branch rather than a figure this slice contradicts. Later slices
should take **1434/0/0/16** as the baseline and not try to reconcile against the older number.

## What slice 01 removed

`NegotiationStatusConverter` is **deleted**, not migrated. `WebConfig.addFormatters` registers
`NegotiationEventConverter`, `NegotiationRoleConverter` and `NegotiationResourceEventConverter` and
never this one — re-verified in the source, so the 400 it appears to produce actually comes from
Spring's default enum binding and survives the deletion untouched.

**Ticket 03 says its test class goes with it; that overstates it and the correction stands.**
`unit/converters/ConverterTest` also covers the unrelated `NegotiationRoleConverter`. The class
survives with `testConvert`; only `testStatusConverter` was removed. Post-deletion sweep for
`NegotiationStatusConverter` across `backend/src` and `frontend/` comes back empty.

Preserving today's `?status=UNKNOWN` 400 is **slice 02's** job, via the Enum-Backed Lifecycle
Catalog. This slice removed dead code adjacent to that question and did not touch the behaviour.

## Standing hazards, carried not solved

**`WellKnownResourceStates` is a bet on a family's vocabulary; the Negotiation holder is not.** ADR
0004 keeps a single Negotiation-scope Definition Family, so those five names are as stable as the
enum constants were. Resource scope is exactly the scope that diverges once custom families ship, and
a custom family may reasonably have no `REPRESENTATIVE_UNREACHABLE`. Nothing in stage 1 can make the
bet lose — one seeded Resource family, and the seed is a faithful transcription — so this is recorded
in the holder's Javadoc for whoever ships the second one. Ticket 03 filed it as noted-not-solved;
this slab does not solve it.

**`WellKnownResourceStates.SUBMITTED` is a default as well as a comparison.** `UpdateResourcesDTO`
uses it as a *default value* rather than a test, which is the same divergence hazard in an API DTO.
Flagged in the holder's Javadoc; slice 09 will meet it.

## Operational

**Do not run two Maven invocations against `backend/` at once.** A concurrent recompile clears
`target/test-classes` under a running suite and every Spring-context class then errors with
`FileNotFoundException`. It presents as ~150 unrelated failures and is an artifact. A sub-agent that
verifies by running tests counts as a second invocation.

Slices 01, 02, 03 and 07 have no dependency on each other and are being authored in parallel in
separate worktrees. Separate worktrees have separate `target/` directories, so that much is safe —
but serialize anything that touches a shared resource.

Run the formatter before committing any Java; it is not bound to the `test` phase:

```
nix develop .#opencode --command mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format
```
