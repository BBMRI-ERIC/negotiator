# Parity gate selector and findings report

Status: resolved

(The five triage roles in `docs/agents/triage-labels.md` all describe work still waiting on someone.
`resolved` is `docs/agents/issue-tracker.md`'s own closing value and is used here so no later agent
picks this ticket up again.)

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Turn ten slices of tests into the one thing every later slab actually needs: a single command that
answers "is behaviour still identical?", plus the written record of what pinning the behaviour taught
us.

**The gate.** One selector runs the whole characterization suite and nothing else, reports the
intended-delta tests separately from parity tests, and is documented where a later session will find
it — the slab gate of every subsequent map ticket is "this command is green". Verify it runs clean
from a cold start, since the suite depends on the seeded test data being loaded.

**The findings report.** Characterization surfaces things that are true but were not known. Collect
them in one place, with the evidence, as the resolution of the parent map ticket. Everything found so
far is a report, never a fix — that is what makes the suite credible as a before-picture:

- Whether `NegotiationIsApprovedGuard` is attached to any transition, and therefore whether it is dead
  code that must not be reimplemented in ADR 0002's registry.
- Whether `DRAFT` is reachable as an entry state, which ADR 0009's seed must reproduce either way.
- That the Information Requirement gate's submission check is not scoped to the requirement, so any
  submission satisfies every requirement for that Resource.
- That the Negotiation service throws on a refused event while the Resource service silently returns
  the unchanged state.
- Whether the double-annotated conclusion listener runs once or twice.
- That conclusion counts only two of the terminal Resource states.
- That the two graphs evaluate authority through entirely different mechanisms.
- That the abandon transitions from in-progress and from paused are not equivalent.

**Report the coverage honestly.** State what the suite does *not* pin, so a later session does not
over-trust the gate. Known gaps by design: the frontend, the intended deltas, and anything only
reachable through Spring Statemachine internals.

Finally, hand the slab back: this issue's completion is the parent map ticket's resolution, so write
the answer in a form that can be lifted into the map's Decisions-so-far as a single gist plus a link.

## Acceptance criteria

Every box below was ticked against the surefire reports of the runs recorded under Verification, not
against the source or against an earlier ticket's stated numbers.

- [x] A single selector runs the entire characterization suite and no unrelated tests.
      `-Dtest='eu.bbmri_eric.negotiator.characterization.**'`. Verified negatively as well as
      positively: the reports directory was **deleted** before each run, so the set of reports written
      is the set of classes that ran, and it was exactly the 24 characterization classes and nothing
      else.
- [x] The selector is verified green from a cold start against the current Spring Statemachine code.
      **255 tests in 24 classes, 0 failures, 0 errors, 1 skipped.** Cold in the sense that matters
      here: `@IntegrationTest(loadTestData = true)` builds a fresh Testcontainers Postgres and Flyway
      cleans and migrates the seed on every context build. Run twice — once as found, once again on the
      tree as left behind. See Verification.
- [x] Intended-delta tests are reported separately from parity tests. Two commands, one tag. With
      `-DexcludedGroups=intended-delta` no report is written for the delta class; with
      `-Dgroups=intended-delta` **8 tests, 0 failures**, and after emptying the directory first the
      delta class's report was the **only** file written — a stronger check than ticket 10's, which
      could only observe that a stale report had not been overwritten.
- [x] The command is documented where a later slab will find it. Three places, deliberately:
      [`parity-gate.md`](../../state-machine-implementation/parity-gate.md) next to the map (the
      authoritative one), linked from `map.md` above the standing decisions; and a short form in the
      suite itself at
      `backend/src/test/java/eu/bbmri_eric/negotiator/characterization/package-info.java`, so a session
      that never opens `.scratch/` still finds it. Not `backend/CONTEXT.md` — see Resolution for why.
- [x] Each finding listed above is recorded with its evidence, or explicitly marked as not applicable.
      All eight are in part 1 of
      [`before-picture-findings.md`](../../state-machine-implementation/before-picture-findings.md),
      one section each, in the order this ticket lists them. **None had to be marked not-applicable.**
      Every cited `file:line` was re-checked against the working tree rather than copied from the
      ticket files.
- [x] Every finding is reported, not fixed; no production code is modified anywhere in the slab.
      `git diff` over `backend/src/main` and `frontend/` is empty for this ticket, and the formatter
      run reported 593 files processed with **1 reformatted** — the new test-scope `package-info.java`.
- [x] The suite's coverage gaps are stated explicitly. Part 6 of the findings report: five gaps by
      design (frontend, the deltas, Spring Statemachine internals, the `minimal-workflow` profile, the
      Override producer's own seam) and four not by design, including the whole
      documented-rather-than-asserted register in part 4.
- [x] A total count of pinned behaviours is recorded, so a later slab can notice if the suite shrinks.
      Part 5 records it **two ways** — 255 parity test invocations over 165 declared methods, 8 deltas
      over 8, 41 Java files — because a refactor that merges methods moves one number and a refactor
      that narrows a whole-graph walk moves the other. The one legitimate historical decrease
      (158 → 157) is on record with its reason.
- [x] Confirmation that the four lifecycle enums appear in exactly one test-scope file, the adapter.
      Confirmed by grep, command and output in part 5. 14 occurrences in 3 files, of which **only
      `adapter/EnumBackedLifecycleTestAdapter.java` is code**; the other two are a `//` comment and a
      javadoc, which the guard blanks before scanning. Two precisions recorded rather than smoothed
      over: the suite names only **three** of the four enums (`NegotiationState` appears nowhere as
      code), and the property holds for the *characterization tree*, not for `src/test` as a whole,
      where 29 files reference them — expected churn for the decouple-consumers slab.
- [x] Confirmation that no Spring Statemachine type is imported anywhere in the suite except the
      throwaway dump generator. Confirmed by grep: 14 occurrences, all 14 inside `characterization/dump/`,
      zero anywhere else including `delta/`. **Stated precisely:** the exemption is the *package*, not a
      single file — the guard's check is package-scoped and all five files there are throwaway together.
      One pre-existing test outside the suite still imports the library
      (`integration/service/NegotiationLifecycleServiceImplTest`); not a violation, but one more file
      the cutover has to touch.
- [x] A resolution gist suitable for the map's Decisions-so-far is drafted in this issue. Below.

## Blocked by

- [Lifecycle graph dump generator and frozen v1 artifacts](01-graph-dump-generator.md)
- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
- [Information Requirement gate parity](05-information-requirement-gate.md)
- [Post side effects of Negotiation transitions](06-post-side-effects.md)
- [Lifecycle history rows for both graphs](07-lifecycle-history-rows.md)
- [Event seam: spawn, conclusion, and notification firing conditions](08-event-seam-spawn-conclusion-notifications.md)
- [REST seam: metadata endpoints and the graph diagram endpoint](09-rest-seam-metadata-and-diagram.md)
- [ADR 0005 intended-delta tests](10-intended-delta-tests.md)

## Resolution

Two new documents next to the map, one new test-scope `package-info.java`, and corrections to the PRD.
No test was written, changed or deleted, and no production code was touched.

| File | Role |
|---|---|
| `.scratch/state-machine-implementation/parity-gate.md` | **new.** Both commands, both counts, the `nix develop` prefix, the script's real path, what `intended-delta` means and how to verify the split, the ordering rule, the coverage gaps, the dump-regeneration command, the formatter. |
| `.scratch/state-machine-implementation/before-picture-findings.md` | **new.** The consolidated findings report: the eight named findings, ten more load-bearing ones, the corrections owed upward, the documented-not-asserted register, the mechanical confirmations, the coverage gaps, and the twelve decisions the redesign now owes. |
| `backend/src/test/java/eu/bbmri_eric/negotiator/characterization/package-info.java` | **new**, test scope. The gate in the code tree, so a cutover session that never reads `.scratch/` cannot run the suite without knowing about the tag. |
| `.scratch/state-machine-implementation/map.md` | link to both, above the standing decisions. Decisions-so-far deliberately left alone — see the gist below. |
| `.scratch/freeze-current-behaviour/PRD.md` | the spawn correction, as a marked amendment plus a pointer on story 13. |
| `.scratch/freeze-current-behaviour/STATUS.md` | ticket table, and a banner saying the file is now safe to delete and where its two durable halves went. |

### Why the gate does not live in `backend/CONTEXT.md`

It was considered and rejected. `backend/CONTEXT.md` is a **ubiquitous-language document** — every
entry is a term, a definition and an `_Avoid_` line, maintained through `/domain-modeling`. A Maven
invocation with two surefire flags is not vocabulary, and putting it there would be the first entry of
its kind, inviting the file to become a general handbook. The map is the effort's spine and lives
across all three stages, so a file beside it outlives this slab by exactly the right amount; the
`package-info.java` covers the case where a later session works from the code and never opens
`.scratch/`.

## Verification

All runs from the repository root, `nix develop .#opencode --command` prefixed, reports directory
deleted first so the set of reports written is evidence rather than the absence of an overwrite.

**Parity, twice.** Once on the tree as found, once again after this ticket's only Java addition:

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
```

**255 tests in 24 classes, 0 failures, 0 errors, 1 skipped**, exit code 0, ~8.5 minutes. Per-class
counts summed from the 24 `.txt` reports rather than read off a summary line. **No test-ordering
failure appeared** — the ordering rule ticket 03 established and tickets 04–10 inherited holds across
the whole selector, which is the only place it can be checked.

**The deltas alone:**

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -Dgroups=intended-delta
```

**8 tests, 0 failures, 0 errors, 0 skipped**, and the delta class's report was the only file in an
emptied reports directory.

**Compilation and the guard, after adding `package-info.java`:** the whole test tree compiles and
`CharacterizationImportGuardTest` is 3/3 green. The formatter (`fmt-maven-plugin:2.25`) processed 593
files and reformatted exactly one, the new file.

### One environment trap worth recording

The first attempt at the parity run was aborted — a surefire fork was shut down mid-class at 12:35:37
and left a `-jvmRun1.dumpstream` behind, while the reports from the classes that had already finished
stayed on disk looking perfectly healthy. **A summary line and a directory listing both looked fine.**
The check that caught it was comparing each report's mtime against the run's start time. Deleting
`backend/target/surefire-reports/` before a run is cheaper and is now what `parity-gate.md` recommends.

## Findings

This ticket produced no new findings about the system — it consolidated everyone else's. The report is
[`before-picture-findings.md`](../../state-machine-implementation/before-picture-findings.md); its
structure, so this ticket is a usable index:

| Part | Contents |
|---|---|
| 1 | The eight findings this ticket named, one section each, with evidence and consequence |
| 2 | Ten more load-bearing findings: the silent `ANY` comparison type, the two Legacy States, the three Transition-less Events, the second `ResourceStateChangeEvent` producer, the five dead defensive branches, the history trail recording assignments rather than Transitions, the untransactional pending reminder, the handler facts, the REST surface, the caller-dependent requirement hint |
| 3 | Five corrections owed upward, four wrong and one right |
| 4 | The register of things documented rather than asserted, and why each one is |
| 5 | The mechanical confirmations, with commands and output, and the suite's size two ways |
| 6 | Nine coverage gaps, five by design |
| 7 | Twelve decisions the redesign now owes, each pointing at the finding that creates it |

Two things this ticket did establish, which are worth stating here because they are about the *suite*
rather than about the system:

**1. The criterion about the four enums is true of the suite and false of the test tree, and the
difference matters.** Only `EnumBackedLifecycleTestAdapter.java` names an enum as code inside
`characterization/`; across `src/test` as a whole, **29 files** do. The PRD's "26 test files" is now
29. That is scoped out by design, but a cutover session reading "the enums appear in exactly one
test-scope file" without the qualifier will badly underestimate the churn ahead of it.

**2. The Spring Statemachine exemption is a package, not a generator.** Five files under
`characterization/dump/` import the library — the dumper, its artifacts holder, the generator, the
drift test and the unwrap test. They are throwaway *together*; the criterion's singular "the throwaway
dump generator" undercounts what gets deleted at cutover, and the drift test in particular is the thing
that keeps the committed graph artifacts honest, so deleting it is what makes the artifacts frozen
rather than live.

## Draft resolution gist for the map's Decisions-so-far

Deliberately **not** written into `map.md` by this ticket: appending to Decisions-so-far asserts that
map ticket 01 is resolved, and closing a map ticket is the claiming session's call, not this issue's.
`map.md` has instead gained a pointer to both documents above the standing decisions, so the gate is
findable now either way. Lift the following when resolving
[map ticket 01](../../state-machine-implementation/issues/01-freeze-current-behaviour.md):

> - **[01 Freeze current behaviour](issues/01-freeze-current-behaviour.md)** — **resolved.** Stage 1's
>   parity gate exists and is green: **255 tests in 24 classes** under
>   `eu.bbmri_eric.negotiator.characterization.**`, plus **8** ADR 0005 intended deltas tagged out of
>   it, with **no production code changed anywhere**. States and Events are named only as strings
>   behind one test-scope adapter and a mechanical guard enforces it, so the suite must pass
>   *unchanged* after the enums are deleted. Both graphs are dumped by walking the live beans and the
>   committed artifacts are regenerated and byte-compared on every run. Commands, counts and coverage
>   gaps: **[parity-gate.md](parity-gate.md)**. **Pinning the behaviour left twelve decisions the ADRs
>   do not yet answer, and one place where three settled documents describe behaviour the code does not
>   have — read
>   [before-picture-findings.md](before-picture-findings.md) parts 3 and 7 before implementing 0005,
>   0007 or 0009.** Headlines: `NegotiationIsApprovedGuard` is attached to nothing and must not be
>   registered; spawn writes `REPRESENTATIVE_CONTACTED`/`REPRESENTATIVE_UNREACHABLE`, never the initial
>   State, publishes no Resource state change, and keys on arriving at `IN_PROGRESS` rather than on
>   `APPROVE` — so ADR 0007's Spawn Action, ADR 0009's seed and `backend/CONTEXT.md`'s **Spawn** entry
>   are all specified against a picture that is not the code; conclusion counts only 2 of 12 Resource
>   States; the Information Requirement check is unscoped and outranks every other gate; and
>   `ResourceStateChangeEvent` has a second, non-Transition producer.
