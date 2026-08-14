# Slab status — freeze current behaviour

Snapshot taken when the session stopped. Update or delete this file once the slab completes.

## Landed on `feat/state-machine-implementation`

| Ticket | State | Evidence |
|---|---|---|
| 01 graph dump generator | **done** | dump + drift test green |
| 02 string adapter + import guard | **done** | guard demonstrated failing on a real violation, then reverted |
| 09 REST seam | **done** | 26 tests green after one cross-ticket fix (see below) |
| 03 Negotiation parity | **done** | 63 tests green; all 12 criteria re-verified against surefire output |
| 04 Resource parity | **done** | 52 tests green; all 14 criteria re-verified against surefire output |

Parity gate as it stands:

```
/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

**157 tests, 0 failures, 0 errors, 1 intentional skip** (ticket 01's opt-in generator). It was 158
when 04 landed; a follow-up review of `26da8c45` and `fd7d0385` deleted one strictly weaker duplicate
test — `NegotiationDraftReachabilityTest.approved_isDeclaredButNeverEntered`, whose statement
`NegotiationGraphV1BindingTest.legacyState_isDeclaredButUnusedInTheDump` already makes against the
mechanical dump. No assertion about the system was weakened. There is **no `scripts/test-backend.sh`
at the repository root** — the early tickets recorded that path and it exits 127; the script ships
with the `focused-backend-tests` skill.

Ticket 04's WIP branch (`worktree-agent-a3dab4653dd6c2484`, commit `7f0226c8`) was cherry-picked, made
to compile, and then reworked — its hand-transcribed graph tables are now bound to the committed dump
by a new `ResourceGraphV1BindingTest`, following ticket 03's finding 3. The branch is spent.

## Conventions the later tickets inherit from the follow-up review

- **Expected offerings are computed from the pinned graph table, never typed out.** The table is
  bound edge for edge to the committed mechanical dump, which the drift test regenerates from the
  live beans, so a computed expectation is anchored to an artifact nobody transcribed — and it stays
  complete if the graph is larger than the rows anyone remembered to type. Reasoning in ticket 03's
  follow-up section.
- **Three shared helpers now exist in the characterization tree, and a fourth was extended.**
  `service/SeededResourceSubject` (the seeded Resource, its three callers, the link-row SQL and the
  hand-rolled authentication), `service/LifecyclePersistence` (the one bounded post-send wait and
  the one `PERSIST_TIMEOUT`), and `rest/CanonicalJson` (now also the suite's one reader of committed
  artifacts: `artifact`, `namesIn`, `publishedValues`). A ticket that needs any of these should use
  them rather than write a fourth copy.

## Not started

05 (Information Requirement gate), 06 (post side effects), 07 (history rows),
08 (event seam: spawn, conclusion, notifications), 10 (intended deltas), 11 (parity gate + findings).

With 03 and 04 landed, **05, 06, 07 and 08 are all unblocked simultaneously** and can be fanned out in
one wave. 10 needs 04 and 09 (both done). 11 needs everything.

## Findings so far

Recorded in full in the individual ticket files. The load-bearing ones:

- **`NegotiationIsApprovedGuard` is dead code, and ticket 04 took path two.** All 21 Transitions
  across both graphs dump `"guard": null`; the `withExternal().guard(...)` fragment produces no
  Transition at all. Verified not to be an unwrap failure in disguise. What is pinned instead is the
  imperative gate at `ResourceLifecycleServiceImpl.java:143-145` — no Resource Event is offered
  unless the parent Negotiation is IN_PROGRESS — walked across all 8 Negotiation States by ticket
  04. Reimplementing the Guard in ADR 0002's registry would introduce a check that has never fired.
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
- **Both Event universes are wider than their dump.** The Negotiation dump's `events` array lists
  only the seven Events that trigger a Transition; `START` is declared, published by the metadata
  endpoint and nameable by a caller, but carries no Transition (ticket 03). The Resource graph has
  two such Events, `RETURN_FOR_RESUBMISSION` and `OVERRIDE` — and `OVERRIDE`, whose published
  description is "Override current state, ignoring state machine guards", carries no Transition and
  is therefore refused at the Lifecycle seam as silently as any other unoffered Event. That is a
  statement about `ResourceLifecycleService.sendEvent` only: an out-of-Lifecycle override path does
  exist elsewhere, and stamps the same Event name — see the next finding. All three names are
  candidate Override Events in the new model, not names to drop. (Ticket 04.)
- **`ResourceStateChangeEvent` has a second producer, and nothing pins it.**
  `ResourceServiceImpl.updateResourceStatus`
  (`governance/resource/ResourceServiceImpl.java:178-194`) writes an arbitrary State straight onto
  the link row via `negotiation.setStateForResource(...)` — no Transition, no Required Authority
  rule of the graph, no IN_PROGRESS gate — and publishes a `ResourceStateChangeEvent` stamped
  `OVERRIDE`. Reachable from `PATCH /negotiations/{id}/resources`
  (`NegotiationController.java:297-304`), gated by admin-or-representative-of-every-Resource. The
  PRD's event seam (stories 13 and 19) and ticket 08 both assume that event traces a Transition; it
  does not always. Out of scope for ticket 04, deliberately left unpinned, and flagged as input in
  ticket 08's own file. (Ticket 04, finding 10.)
- **Five dead branches, and one shape behind all of them.** Alongside `NegotiationIsApprovedGuard`:
  `NegotiationLifecycleServiceImpl`'s `catch (ClassCastException)` refusal (ticket 03), and all
  three advertised sharp edges of `ResourceLifecycleServiceImpl.isSecurityRuleMet` — absent
  `Authentication` counting as admin, `catch (ClassCastException) → false`, and
  `catch (NullPointerException) → creatorId = 0L` (ticket 04). Every one sits after a call to
  `AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId()`, which wraps *every* failure
  in `AuthenticationCredentialsNotFoundException`, so nothing downstream ever sees the exception it
  was written to catch. Defensive branches written around a helper that already normalises its
  failures. None should be carried into the new evaluator. Ticket 04's criterion 9 was reworded to
  pin the escaping exception instead, which is what a caller actually observes.
- **The Resource service is silent only when it has something to be silent about.** Its refusal
  returns `getCurrentStateForResource(...)`, which `orElseThrow`s — so a refused Event on a Resource
  with no recorded State (blank link row or no link row) raises `EntityNotFoundException` carrying
  the *Negotiation's* id. The read path swallows the same lookup failure into `Set.of()`. "The
  Resource service never throws" is the summary everyone will carry into the redesign and it is
  false. (Ticket 04.)
- **Required Authority is exactly one rule per Resource Transition.** 3 `isAdmin`,
  8 `isRepresentative`, 2 `isCreator`, none unsecured, none with two attributes. The delivery chain
  from `SUBMITTED` to `RESOURCE_MADE_AVAILABLE` therefore requires three distinct *rules* — but not
  three distinct people: nothing partitions callers, since `isAdmin` is an authority on the token,
  `isRepresentative` a link row and `isCreator` a column of the Negotiation, and one Person can hold
  all three. The delivery walk changes identity only because seeded callers 101, 109 and 108 happen
  to be disjoint. `isAdmin` means the `ROLE_ADMIN` authority on the token, not the `admin` column of
  the Person row; and representing a Resource is scoped to that Resource, not to its Negotiation —
  the mirror of ticket 03's finding that representing a Resource confers nothing over the
  Negotiation. (Ticket 04.)
- **A hand-transcribed graph table must be bound to the dump.** Ticket 03 shipped its parity table
  as a transcription; two of its findings were assertions over that constant and could not fail.
  `NegotiationGraphV1BindingTest` now equates the table to the committed dump and to the committed
  Event metadata. Ticket 04 found its inherited WIP in exactly the same state and added
  `ResourceGraphV1BindingTest`, and takes the Negotiation State universe from
  `NegotiationGraphV1.allStateNames()` rather than transcribing it a second time. Any later ticket
  adding such a table owes the same binding.
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

## The ordering rule every later ticket inherits

Established by ticket 03, honoured by ticket 04, and binding on 05–08, which all drive Lifecycles:

**Any characterization class that fires an Event must declare
`@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`.** The Flyway strategy is clean-and-migrate on
every context build, so dirtying after each method restores the seed for whoever runs next. The
corpus is shared: `NegotiationAuthorityParityTest` reads `negotiation-1` expecting IN_PROGRESS, and
driving `negotiation-1`'s only Resource to a terminal State concludes that Negotiation. A driving
class that does not dirty turns another ticket red by test ordering alone, which no single class's
own run would reveal. Read-only classes may use `BEFORE_CLASS` or `AFTER_CLASS`. The full-selector
run is the check.

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
