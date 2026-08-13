# Slab status — freeze current behaviour

Snapshot taken when the session stopped. Update or delete this file once the slab completes.

## Landed on `feat/state-machine-implementation`

| Ticket | State | Evidence |
|---|---|---|
| 01 graph dump generator | **done** | dump + drift test green |
| 02 string adapter + import guard | **done** | guard demonstrated failing on a real violation, then reverted |
| 09 REST seam | **done** | 26 tests green after one cross-ticket fix (see below) |
| 03 Negotiation parity | **done** | 63 tests green; all 12 criteria re-verified against surefire output |

Parity gate as it stands:

```
scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

**106 tests, 0 failures, 0 errors, 1 intentional skip** (ticket 01's opt-in generator) after 03
landed.

## Halted mid-flight — work preserved, NOT merged

One agent was stopped by a session limit before it could verify its work. Its tests are written but
**have never been executed**, so nothing was merged.

| Ticket | Branch | Commit | What is missing |
|---|---|---|---|
| 04 Resource parity | `worktree-agent-a3dab4653dd6c2484` | `7f0226c8` | still adding refusal cases; ticket file never updated; suite never run |

That branch is based on `556958f7`, which already contains tickets 01 and 02. It adds
`ResourceTransitionParityTest` and `ResourcePossibleEventsAuthorityTest` into
`eu.bbmri_eric.negotiator.characterization.service` — the same package ticket 03 has now landed
`NegotiationGraphV1` into. Class names are disjoint, so the conflict risk 03 was landed first to
avoid is gone, but 04 must now rebase onto a tip that already contains that package.

**Two things ticket 04's agent must read** in `issues/03-negotiation-transition-parity.md`: the
test-ordering hazard note (any class that drives a Lifecycle must dirty the context after each test
method, or it breaks ticket 03 by ordering alone) and finding 3 (a hand-transcribed graph table must
be bound to the committed dump, or its assertions state nothing about the system).

**To resume:** verify the branch with the selector above, finish 04's remaining acceptance criteria,
fill in its ticket file's findings, then cherry-pick onto `feat/state-machine-implementation`.

## Not started

05 (Information Requirement gate), 06 (post side effects), 07 (history rows),
08 (event seam: spawn, conclusion, notifications), 10 (intended deltas), 11 (parity gate + findings).

Once 03 and 04 land, **05, 06, 07 and 08 are all unblocked simultaneously** and can be fanned out in
one wave. 10 needs 04 and 09 (09 is done). 11 needs everything.

## Findings so far

Recorded in full in the individual ticket files. The load-bearing ones:

- **`NegotiationIsApprovedGuard` is dead code.** All 21 Transitions across both graphs dump
  `"guard": null`; the `withExternal().guard(...)` fragment produces no Transition at all. Verified
  not to be an unwrap failure in disguise. Reimplementing it in ADR 0002's registry would introduce
  a check that has never fired.
- **`secured(..., ComparisonType.ALL)` is silently `ANY`.** SSM 4.0's
  `AbstractTransitionConfigurer.setSecurityRule` ignores its `ComparisonType` argument. Harmless
  today because every rule carries exactly one attribute; ADR 0002's Required Authority model should
  be built against the behaviour, not the builder chain.
- **Both graphs declare more States than the PRD hand-counted**, because the configs register the
  whole enum via `EnumSet.allOf`. `APPROVED` (Negotiation, 8 States not 6) and
  `RETURNED_FOR_RESUBMISSION` (Resource) are Legacy States: declared, no Transition leads to them.
  ADR 0009's seed must carry them or existing rows naming them stop resolving.
- **`DRAFT` is occupied but not enterable.** `negotiation-6` is seeded in `DRAFT`, yet the initial
  State is `SUBMITTED` and no Transition targets `DRAFT`. "Occupied" and "reachable" are different
  questions and the seed must reproduce both. Confirmed at the service seam by ticket 03, which also
  read the occupied set straight out of the table: exactly
  `{DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED}`, so `APPROVED` has no live occupant at all.
- **The Event universe is one wider than the dump.** The dump's `events` array lists only the seven
  Events that trigger a Transition; `START` is declared, published by the metadata endpoint and
  nameable by a caller, but carries no Transition. A candidate Override Event in the new model, not
  a name to drop. (Ticket 03.)
- **Two dead branches, not one.** Alongside `NegotiationIsApprovedGuard`,
  `NegotiationLifecycleServiceImpl`'s `catch (ClassCastException)` refusal is unreachable: with no
  authenticated caller, resolving the internal id throws
  `AuthenticationCredentialsNotFoundException` first. (Ticket 03.)
- **A hand-transcribed graph table must be bound to the dump.** Ticket 03 shipped its parity table
  as a transcription; two of its findings were assertions over that constant and could not fail.
  `NegotiationGraphV1BindingTest` now equates the table to the committed dump and to the committed
  Event metadata. Any later ticket adding such a table owes the same binding.
- **The diagram endpoint has no visited set.** It terminates only because the Resource Lifecycle is
  acyclic; adding any cycle produces `StackOverflowError`, not a larger response. 13 Transitions
  render as 29 nodes, nesting 14 deep. A reimplementation needs a visited set or a depth bound.
- **The unrecognised-name failure is two failure modes.** State path variables bind straight to the
  enum and return a JSON problem detail; Event path variables have `Converter`s that swallow the
  exception and return an empty body. The State detail leaks the fully-qualified name of a class
  ADR 0002 deletes — see below.
- **`ResourceStateMetadataDto` alone carries an `ordinal` field** (0–11, the enum's declaration
  order, documented as significant). Relational configuration must reproduce it.

## One cross-ticket fix worth knowing about

Ticket 09 originally asserted the unrecognised-State response body verbatim. The forbidden-reference
guard from ticket 02 rejected it, correctly: the body contains
`...NegotiationState.NOT_A_STATE`, naming an enum the redesign deletes, so the assertion could only
ever go red at cutover — a guaranteed delta dressed as parity. The test now pins status, content
type, `title`, `status`, and that `detail` is *derived from* the rejected name; the verbatim text is
recorded in ticket 09's findings as the before-picture.

This is the guard catching something neither agent could see alone, which is the argument for having
built it.

## Environment notes

- Testcontainers needs docker group membership; the images (`postgres:16-alpine`,
  `testcontainers/ryuk:0.12.0`) are already pulled locally.
- `backend/target/` gets polluted by the JDT language server compiling without Lombok, which
  presents as ~200 bogus `cannot find symbol` errors in unrelated test files (and matching spurious
  LSP diagnostics). A single `clean` clears it. Agent worktrees have their own `target/` and are
  unaffected.
- Agent worktrees are branched from `master`, not from the slab branch. Every agent brief must tell
  the agent to check and `git reset --hard feat/state-machine-implementation` if needed.

## Unresolved question carried up

`master` has `db9019d4 feat: add minimal-workflow Spring profile with simplified resource state
machine`, which is not on this branch. If that profile defines a second Resource graph, the dump and
the parity suite describe only the default one, and the cutover would be working from an incomplete
picture. Check before treating the dump as complete.
