# Negotiation transition and authority parity

Status: resolved

(The five triage roles in `docs/agents/triage-labels.md` all describe work still waiting on someone.
`resolved` is `docs/agents/issue-tracker.md`'s own closing value and is used here so no later agent
picks this ticket up again.)

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin every transition of the Negotiation graph and the authority rules that gate it, through the
Negotiation lifecycle service via the string adapter.

Two things are being frozen. First the **shape**: for each from-state, which event leads to which
to-state. Second the **authority**: which events the service actually offers, which differs by who is
asking. The service returns an empty set outright to anyone who is neither an admin nor the
Negotiation's creator, and then filters the remaining transitions by matching each transition's
security rule attributes against the caller's Spring roles. Two transitions are `ROLE_ADMIN`-secured.

Also pin refusal. Sending an event that is not currently available raises `ForbiddenRequestException`,
with a message derived from the event's own label. That behaviour is the Negotiation service's, and
it differs from the Resource service's — which is why the two are pinned separately and the asymmetry
is recorded rather than smoothed over.

A table-driven test over the transition set, parameterised by caller role, keeps this from becoming
twenty near-identical methods.

Remember that sending an event drives an asynchronous persist path, so assertions on the resulting
state must be made under Awaitility with a bounded timeout, following the existing lifecycle test's
lead. Never a bare sleep, and never an assertion immediately after the call.

## Acceptance criteria

Every box below was ticked against the surefire report of the run recorded under Verification, not
against the source.

- [x] Every transition of the Negotiation graph is pinned by from-state, event and to-state.
      `NegotiationTransitionParityTest.transition_leadsToItsTarget[1..8]` fires all eight for real
      and reads the resulting State back. That eight is the whole graph is not this test's word:
      `NegotiationGraphV1BindingTest.transitions_areTheDumpsTransitions` equates the table to the
      committed dump edge for edge and to its own `transitionCount`.
- [x] The set of available events is pinned for each reachable state, as an admin. All seven:
      `possibleEvents_asAdmin_areEveryTransitionFromTheState[1..4]` covers the four States the seed
      occupies (DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED),
      `possibleEvents_inDrivenState_matchTheGraph[1..3]` the three it does not (PAUSED, DECLINED,
      CONCLUDED).
- [x] The set of available events is pinned for the Negotiation's creator. The same seven States:
      `possibleEvents_asCreator_omitTheAdminOnlyTransitions[1..4]` and
      `possibleEvents_inDrivenState_asCreator_matchTheGraph[1..3]`, plus SUBMITTED-after-SUBMIT in
      `draft_submitFiresForTheCreatorAndLeavesDraftBehind`.
- [x] The available-events set is pinned as empty for a user who is neither admin nor creator.
      `possibleEvents_asUnrelatedUser_areEmpty[1..4]` in all four seeded States,
      `draft_offersNothingToAnUnrelatedCaller`, and — the interesting case —
      `possibleEvents_asRepresentativeOfAResource_areEmpty`.
- [x] The two admin-secured transitions are shown to be offered to an admin and withheld from a
      non-admin creator. `adminSecuredTransitions_areOfferedToAnAdmin` and
      `adminSecuredTransitions_areWithheldFromTheCreator`; *which* two is bound to the dump's
      security rules by `adminOnlyEvents_areTheDumpsSecuredTransitions`, which also fails if any
      Transition were secured by something other than `ROLE_ADMIN`.
- [x] Sending an unavailable event is pinned as raising `ForbiddenRequestException`. Three ways:
      `sendUnofferedEvent_asAdmin_isForbidden[1..6]` (every Event SUBMITTED does not offer, admin),
      `sendAdminOnlyEvent_asCreator_isForbidden[1..2]` (offered to somebody, not to this caller),
      `sendAnyEvent_asUnrelatedUser_isForbidden[1..4]` (offered nothing at all).
- [x] The exception's message content is pinned. Every one of those cases asserts the message
      verbatim, and `refusalMessages_useThePublishedEventLabels` binds the label table to the labels
      `/v3/negotiation-lifecycle/events` actually publishes, so the pinned text is the user's text.
      `refusalMessage_withAMessageArgument_isTheSame` covers the two-argument overload.
- [x] Requesting available events for a non-existent Negotiation is pinned as raising
      `EntityNotFoundException`. `possibleEvents_ofUnknownNegotiation_asAdmin_raisesEntityNotFound`,
      with the message; `sendEvent_toUnknownNegotiation_raisesEntityNotFound` and
      `currentState_ofUnknownNegotiation_raisesEntityNotFound` for the neighbouring calls. Note the
      criterion has to name the admin — see finding 5.
- [x] Whether `DRAFT` is observably reachable is pinned to match the finding from the dump slice.
      Both halves, separately: `draft_isObservablyOccupied` and
      `seededCorpus_occupiesDraftAndNotTheLegacyState` for occupancy,
      `draft_isTargetedByNoTransition` plus `draft_isDeclaredByTheDumpButTargetedByNoTransitionOfIt`
      and `initialState_isTheDumpsInitialState` for enterability. See finding 1.
- [x] All assertions on post-send state use Awaitility with a bounded timeout. Every read-back goes
      through `awaitState` (`Awaitility.await().atMost(15s).untilAsserted`), including the ones
      inside the `drive` helper that walks a Negotiation to a Transition's source State. No
      `Thread.sleep` anywhere in the characterization tree, and no assertion immediately after a
      send.
- [x] Every State and Event is named as a string; the forbidden-import guard passes.
      `CharacterizationImportGuardTest` 3/3 green in the same run. The four files added here name no
      Lifecycle enum and no Spring Statemachine type.
- [x] No production code is modified. `git diff --stat` over `backend/src/main` is empty; the commit
      touches five test files and this ticket.

## Blocked by

- [String-keyed lifecycle test adapter and forbidden-import guard](02-string-adapter-and-import-guard.md)

## What was built

Five files under `backend/src/test/java/eu/bbmri_eric/negotiator/characterization/service/`:

| File | Tests | Pins |
|---|---|---|
| `NegotiationGraphV1` | — | the graph as a string-only table: 8 Edges, the admin-secured Events, the initial State, the Legacy State, the Event universe and the refusal-message labels |
| `NegotiationGraphV1BindingTest` | 8 | that table against the committed dump and the committed Event metadata — no Spring context needed |
| `NegotiationTransitionParityTest` | 14 | all 8 Transitions fired for real; the offerings of the 3 States the seed cannot reach, to both an admin and the creator |
| `NegotiationAuthorityParityTest` | 33 | offerings per role in the 4 seeded States; every refusal and its message; the not-found cases |
| `NegotiationDraftReachabilityTest` | 8 | the DRAFT question, both halves, and the Legacy State |

## Verification

```
/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

Green on `feat/state-machine-implementation`. **106 tests, 0 failures, 0 errors, 1 skipped** — the
skip is ticket 01's opt-in dump generator, which only writes when explicitly asked. By class:

| Class | Tests |
|---|---|
| `service.NegotiationAuthorityParityTest` | 33 |
| `rest.LifecycleMetadataEndpointsTest` | 19 |
| `service.NegotiationTransitionParityTest` | 14 |
| `dump.LifecycleGraphDumpDriftTest` | 9 |
| `service.NegotiationDraftReachabilityTest` | 8 |
| `service.NegotiationGraphV1BindingTest` | 8 |
| `rest.ResourceLifecycleDiagramEndpointTest` | 7 |
| `guard.CharacterizationImportGuardTest` | 3 |
| `dump.LifecycleGraphDumperUnwrapTest` | 3 |
| `adapter.LifecycleTestAdapterSmokeTest` | 1 |
| `dump.LifecycleGraphDumpGeneratorTest` | 1 (skipped) |

`MailConnectException` noise in the output is expected — there is no SMTP server in the test
environment — and is not a failure.

## Findings

**1. `DRAFT` is occupied but not enterable, and those are two different facts.** *Occupied*: the
seed really does put a Negotiation in `DRAFT` — `negotiation-6` — and `select distinct current_state
from negotiation` returns exactly `{DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED}`. Whatever creates a
Negotiation puts it there without going through the Lifecycle. *Not enterable*: the initial State is
`SUBMITTED`, `DRAFT --SUBMIT--> SUBMITTED` leads out of `DRAFT`, and no Transition anywhere targets
it, so a Negotiation that leaves `DRAFT` can never return. A design that read "occupied" as evidence
of "enterable" would model the graph wrongly; ADR 0009's seed has to reproduce both facts, which
means `DRAFT` needs an occupant and needs no inbound Transition.

**2. `APPROVED` is a Legacy State in the full sense.** It is declared by the Definition (the config
registers the whole enum), it is the source and target of no Transition, and nothing in the seed
occupies it. Unlike `DRAFT` it therefore has no live occupant at all, which is what makes it free to
be modelled as Legacy rather than as an entry point from outside.

**3. Nothing bound the pinned table to the dump until now, and two findings depended on it.** The
table in `NegotiationGraphV1` is a hand transcription of
`src/test/resources/lifecycle/negotiation-graph-v1.json`, which is exactly the step ticket 01 exists
to make unnecessary. As first written, `draft_isTargetedByNoTransition` and the `APPROVED` assertion
read only the in-test constant — they could not fail unless someone edited that constant, so they
stated something about the file rather than about the system. `NegotiationGraphV1BindingTest` closes
that: every constant is now checked against the committed dump (which
`LifecycleGraphDumpDriftTest` regenerates from the live beans on every run) or against the committed
Event metadata (which `LifecycleMetadataEndpointsTest` compares to the live endpoint). Any later
ticket that adds a table of this kind should bind it the same way.

**4. The Event universe is eight; the dump lists seven.** The dump's `events` array names only the
Events that trigger a Transition. `START` is a real Event all the same — declared by the Definition,
published by `/v3/negotiation-lifecycle/events`, nameable by any caller — and simply carries no
Transition, so it can never be offered from any State. `ALL_EVENT_NAMES` deliberately keeps it, so
that the refusal coverage covers every name a caller could actually send; the binding test checks
that field against the published metadata and the dump's seven against the transitions that use
them, rather than pretending the two lists are one list. `START` is thus a candidate Override Event
in the new model, not a name to drop.

**5. The blanket authority check runs before the Negotiation is looked up.** A caller who is neither
an admin nor the Negotiation's creator gets `Set.of()` for a Negotiation that does not exist, while
an admin gets `EntityNotFoundException`. So the not-found criterion has to name the admin, and the
new evaluator inherits a decision: does "may not see it" continue to be indistinguishable from "is
not there"? Pinned as-is either way.

**6. Representing a Resource of a Negotiation confers no authority over the Negotiation itself.**
`TheBiobanker` represents Resource 4 of `negotiation-1` and is offered nothing. Worth stating
because the Resource graph's Required Authority vocabulary includes the representative, and it would
be easy to carry that across when the two graphs are unified in configuration.

**7. With no authenticated caller the service never reaches its own refusal.** Resolving the
caller's internal id fails first and surfaces Spring Security's
`AuthenticationCredentialsNotFoundException`, not the `ForbiddenRequestException` that
`NegotiationLifecycleServiceImpl`'s `catch (ClassCastException)` branch was written to produce. That
branch is unreachable today. Like the dead `NegotiationIsApprovedGuard`, it should not be faithfully
reimplemented.

**8. The admin/creator split is entirely about `SUBMITTED`.** `APPROVE` and `DECLINE` out of
`SUBMITTED` are the only secured Transitions, so in all six other States an admin and the creator
are offered exactly the same set — including the three the seed cannot reach. The practical shape
of a creator's authority is therefore: submit your own draft, then nothing until an admin approves
it; after that, pause, unpause, abandon and conclude are all yours.

**9. The refusal message is the Event's label lowercased**, identically with and without the
optional message argument, and it reaches the frontend verbatim. Today every label happens to be its
Event name in title case; the table spells the labels out rather than deriving them, so that
coincidence is not what the suite is testing.

## Note for ticket 04 — a test-ordering hazard

Ticket 04 writes `ResourceTransitionParityTest` and `ResourcePossibleEventsAuthorityTest` into this
same package, and will drive `negotiation-1`'s Resources. `NegotiationAuthorityParityTest` reads
`negotiation-1` expecting `IN_PROGRESS`, and a Resource Lifecycle reaching its terminal States can
conclude the Negotiation, so an unclean neighbour could turn ticket 03 red by ordering alone.

Two things were done here, and one rule is left for ticket 04:

- The two read-only classes (`NegotiationAuthorityParityTest`, `NegotiationDraftReachabilityTest`)
  now declare `@DirtiesContext(classMode = BEFORE_CLASS)`. The Flyway strategy is `clean` +
  `migrate` on every context build, so forcing a fresh context before the class guarantees a freshly
  seeded database whatever ran before. It is close to free: every other class in the suite already
  dirties the context after itself, so there is usually no cached context to throw away.
- `NegotiationTransitionParityTest` keeps `AFTER_EACH_TEST_METHOD`, which is the neighbourly
  property — it drives Negotiations and always leaves the seed restored for whoever runs next.

**The rule for ticket 04: any class that drives a Lifecycle must dirty the context after each test
method.** A driving class that does not is the one thing that can break ticket 03 from outside.

## Environment note

`backend/target/` was polluted by a compile without Lombok annotation processing (the JDT language
server does this), which presents as ~200 bogus `cannot find symbol: method builder()` errors in
unrelated test files. Deleting `backend/target/{classes,test-classes,maven-status}` clears it. This
matches the note already in `STATUS.md`; it cost a full recompile here.
