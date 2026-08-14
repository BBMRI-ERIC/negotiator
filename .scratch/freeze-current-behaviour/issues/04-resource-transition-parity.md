# Resource transition and authority parity, including the IN_PROGRESS gate

Status: resolved

(The five triage roles in `docs/agents/triage-labels.md` all describe work still waiting on someone.
`resolved` is `docs/agents/issue-tracker.md`'s own closing value and is used here so no later agent
picks this ticket up again.)

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

The same treatment as the Negotiation graph, for the larger Resource graph — and it behaves
differently in three ways that all need pinning.

**The graph.** Every transition by from-state, event and to-state, across the thirteen transitions
covering contact, availability checking, access conditions and delivery.

**Authority, evaluated differently.** Unlike the Negotiation machine, the Resource machine does not
enable Spring Statemachine's security at all; its service reimplements rule evaluation imperatively
against three rule names — admin, representative of the Resource, and creator of the Negotiation.
That implementation has sharp edges: a `NullPointerException` while resolving the caller falls back
to a creator id of zero, a `ClassCastException` returns false, and an entirely absent
`Authentication` is treated as satisfying the admin rule. Pin the observable outcomes of all of it.
Do not tidy any of it. (All three of those edges turned out to be unreachable — see finding 2.)

**Refusal is silent.** Where the Negotiation service throws, the Resource service returns the
unchanged current state and raises nothing. This asymmetry is real, load-bearing for callers, and
must be frozen explicitly so that making the two consistent later is a decision rather than an
accident.

**The IN_PROGRESS gate.** No Resource event is available unless the parent Negotiation is
IN_PROGRESS — enforced imperatively in the service, returning an empty set otherwise. Pin it from
both sides: available events for a Resource in an IN_PROGRESS Negotiation, and empty for every other
Negotiation state.

That gate is also where this slice depends on the dump. If the dump shows
`NegotiationIsApprovedGuard` is attached to no transition, then it never fires, and the parent
ticket's requirement to pin "every guard outcome, including `NegotiationIsApprovedGuard` in both
directions" is satisfied by pinning this imperative gate instead — with the dead Guard recorded as a
finding so nobody reimplements it in ADR 0002's registry. If the dump shows it *is* attached, pin the
guard in both directions as originally written.

## Acceptance criteria

Every box below was ticked against the surefire report of the run recorded under Verification, not
against the source. Criterion 9 is reworded from the original — see finding 2 for why the behaviour
it named cannot be observed.

- [x] Every transition of the Resource graph is pinned by from-state, event and to-state.
      `ResourceTransitionParityTest.transition_movesResourceToTargetState[1..13]` fires all thirteen
      for real — it writes the source State onto the link row, asserts the Event is offered, sends
      it, and reads the target back under Awaitility. That thirteen is the whole graph is not this
      test's word: `ResourceGraphV1BindingTest.transitions_areTheDumpsTransitions` equates the table
      to the committed dump edge for edge — source, event, target *and* Required Authority — and to
      its own `transitionCount`. `deliveryChain_reachesResourceMadeAvailable` then walks six of them
      as one Lifecycle from the seeded initial State using nothing but Events, looking each step's
      target up in the same table.
- [x] Available events are pinned per reachable state for an admin caller.
      `ResourcePossibleEventsAuthorityTest.possibleEvents_perStatePerCaller[1]` — one invocation
      covering all twelve declared States, including the four terminal ones and the Legacy State,
      which offer nothing. The expected set per State is computed from `ResourceGraphV1`, which
      `securedEvents_perRule_areTheDumpsSecurityRules` and
      `everyTransition_carriesExactlyOneOfTheThreeKnownRules` bind to the dump's security rules.
      `adminRule_isMetOnlyByTheRoleAdminAuthority[1..3]` pins what "admin" means — see finding 8.
- [x] Available events are pinned per reachable state for a representative of the Resource.
      `possibleEvents_perStatePerCaller[2]` (caller 109, all twelve States), and — the interesting
      case — `[5]`, a caller who represents Resources of a *different* organization and is offered
      nothing anywhere. See finding 9.
- [x] Available events are pinned per reachable state for the Negotiation's creator.
      `possibleEvents_perStatePerCaller[3]` (caller 108, all twelve States): the two access-condition
      answers out of `ACCESS_CONDITIONS_INDICATED` and nothing else.
- [x] Available events are pinned for a caller with none of those relationships.
      `possibleEvents_perStatePerCaller[4]` (caller 104): empty from every one of the twelve States.
      `[5]` is the same result reached by a caller who is a representative, just not of this
      Resource.
- [x] Available events are pinned as empty for every parent Negotiation state other than IN_PROGRESS.
      `possibleEvents_areGatedOnTheParentBeingInProgress[1..8]` — one invocation per State the
      Negotiation Definition declares, seven of them asserting empty, as an admin standing on the
      Resource's initial State. The universe of eight is not retyped here: it comes from
      `NegotiationGraphV1.allStateNames()`, which `NegotiationGraphV1BindingTest` equates to the
      Negotiation dump. `possibleEvents_onSeededAbandonedNegotiation_isEmpty` shows the same on
      unmutated seed rows.
- [x] Available events are pinned as non-empty for an IN_PROGRESS parent, given a suitable caller.
      The IN_PROGRESS row of that same parameterized test asserts exactly `CONTACT` and
      `MARK_AS_UNREACHABLE` *and* `isNotEmpty()`, so the gate cannot pass by being empty everywhere.
      Each of the thirteen transition rows independently asserts its Event is offered before firing.
- [x] Sending an unavailable event is pinned as returning the unchanged current state and raising
      nothing, with the asymmetry against the Negotiation service noted in the test's name or comment.
      Four cases in two shapes.
      `unofferedEvent_returnsUnchangedStateAndRaisesNothing_unlikeTheNegotiationService[1..2]` covers
      an Event this caller has no Required Authority for and an Event with no Transition from this
      State; `eventOnNoTransition_isSilentlyRefused[1..2]` covers the two Events that carry no
      Transition anywhere (`OVERRIDE`, `RETURN_FOR_RESUBMISSION`), sent by an admin who is being
      offered Events at the same moment so the refusal cannot be confused with the gate. All four
      assert that nothing is thrown, that the returned State is the unchanged one, and — after a
      three-second `pollDelay` — that the State is *still* unchanged, which is what makes "silent"
      mean "no-op" rather than "had not landed yet". The asymmetry is named in the method name and
      spelled out in its javadoc.
- [x] **Reworded:** the observable outcome for a caller the service cannot resolve is pinned, and the
      absent-`Authentication`-counts-as-admin branch is recorded as unreachable.
      `possibleEvents_withoutAuthentication_raisesCredentialsNotFound` and
      `possibleEvents_withForeignPrincipal_raisesCredentialsNotFound` both pin an escaping
      `AuthenticationCredentialsNotFoundException` carrying "No authenticated user found". The
      original criterion asked for the absent-`Authentication`-counts-as-admin behaviour; that branch
      cannot be reached, and neither can the other two sharp edges the ticket advertised. Finding 2.
- [x] Available events for a Resource not linked to the Negotiation are pinned as an empty set.
      `possibleEvents_forResourceNotLinkedToTheNegotiation_isEmpty`, which also pins that the current
      State reads back as absent. `possibleEvents_forResourceWithNoRecordedState_isEmpty` pins that a
      *linked* Resource with a blank State is indistinguishable from it, and
      `possibleEvents_forUnknownNegotiation_isEmpty` that an unknown Negotiation is too. Sending an
      Event in those situations is the one place the refusal is not silent — finding 3.
- [x] The `NegotiationIsApprovedGuard` question is resolved against the dump's finding, and this
      issue records which of the two paths was taken. **Path two.** The Guard is attached to nothing;
      `ResourceGraphV1BindingTest.noTransition_carriesAGuard` pins both halves of that — thirteen
      Transitions, exactly the thirteen fully specified chains with no fourteenth orphan, and
      `"guard": null` on every one. What is pinned instead is the imperative gate, by
      `possibleEvents_areGatedOnTheParentBeingInProgress[1..8]`. Finding 1.
- [x] All assertions on post-send state use Awaitility with a bounded timeout. Every read-back after
      a send goes through `await().atMost(15s).untilAsserted` — in
      `transition_movesResourceToTargetState`, in the `fire` helper the delivery chain walks with,
      and in `awaitStillInTheInitialState`, which adds the three-second `pollDelay` the refusal cases
      need. No `Thread.sleep` anywhere in the characterization tree. The only assertion made
      immediately after a send is on `sendEvent`'s own return value, which is the behaviour criterion
      8 exists to pin.
- [x] Every State and Event is named as a string; the forbidden-import guard passes.
      `CharacterizationImportGuardTest` 3/3 green in the same run. The three files added here name no
      Lifecycle enum and no Spring Statemachine type.
- [x] No production code is modified. `git diff --stat` over `backend/src/main` is empty; the commit
      touches five test files and this ticket.

## Blocked by

- [Lifecycle graph dump generator and frozen v1 artifacts](01-graph-dump-generator.md)
- [String-keyed lifecycle test adapter and forbidden-import guard](02-string-adapter-and-import-guard.md)

## What was built

Three new files and two edits under
`backend/src/test/java/eu/bbmri_eric/negotiator/characterization/service/`:

| File | Tests | Pins |
|---|---|---|
| `ResourceGraphV1` | — | the graph as a string-only table: 13 Edges each carrying its Required Authority rule, the initial State, the Legacy State, the 12 States, the 13-name Event universe, the two Transition-less Events, the parent State the gate demands |
| `ResourceGraphV1BindingTest` | 10 | that table against the committed dump and the committed State and Event metadata — no Spring context needed — plus the dead Guard and the `ANY` comparison type |
| `ResourceTransitionParityTest` | 20 | all 13 Transitions fired for real, the delivery chain walked end to end, four silent refusals and the two that are not silent |
| `ResourcePossibleEventsAuthorityTest` | 22 | the offerings of all 12 States to five kinds of caller; the IN_PROGRESS gate across all 8 Negotiation States; the not-found and unresolvable-caller cases |
| `NegotiationGraphV1` (edited) | — | gains `allStateNames()`, derived from its own Transition table plus the Legacy State, so the gate test takes the Negotiation State universe from ticket 03's bound table instead of transcribing it again |
| `NegotiationGraphV1BindingTest` (edited) | 8 | one added assertion binding `allStateNames()` to the Negotiation dump's `states` array |

The WIP branch's two files were cherry-picked and then reworked. What changed beyond making them
compile: the hand-transcribed 13-row table, `ALL_STATES` and `ALL_NEGOTIATION_STATES` were replaced
by `ResourceGraphV1` and `NegotiationGraphV1` and bound to the committed artifacts (finding 6); the
half-written no-current-State case was split into two honest ones (it had been clearing the subject
Resource's State even in the row about an *unlinked* Resource, where that is irrelevant); a second
refusal shape was added; and the fifth caller — a representative of another Resource — was added.

## Verification

```
/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

Green on `feat/state-machine-implementation`. **158 tests, 0 failures, 0 errors, 1 skipped** — the
skip is ticket 01's opt-in dump generator, which only writes when explicitly asked. By class:

| Class | Tests |
|---|---|
| `service.NegotiationAuthorityParityTest` | 33 |
| `service.ResourcePossibleEventsAuthorityTest` | 22 |
| `service.ResourceTransitionParityTest` | 20 |
| `rest.LifecycleMetadataEndpointsTest` | 19 |
| `service.NegotiationTransitionParityTest` | 14 |
| `service.ResourceGraphV1BindingTest` | 10 |
| `dump.LifecycleGraphDumpDriftTest` | 9 |
| `service.NegotiationDraftReachabilityTest` | 8 |
| `service.NegotiationGraphV1BindingTest` | 8 |
| `rest.ResourceLifecycleDiagramEndpointTest` | 7 |
| `guard.CharacterizationImportGuardTest` | 3 |
| `dump.LifecycleGraphDumperUnwrapTest` | 3 |
| `adapter.LifecycleTestAdapterSmokeTest` | 1 |
| `dump.LifecycleGraphDumpGeneratorTest` | 1 (skipped) |

Ticket 03's four classes are green in the same run, which is the check on the ordering rule below.
`MailConnectException` noise in the output is expected — there is no SMTP server in the test
environment — and is not a failure.

## Findings

**1. `NegotiationIsApprovedGuard` is dead, and the check it looks like it performs lives somewhere
else entirely.** The fragment at `ResourceStateMachineConfig.java:117` —
`transitions.withExternal().guard(negotiationIsApproved())` — names no source, event or target, and
Spring Statemachine discards it silently. The dump records thirteen Transitions, exactly the thirteen
fully specified chains, and every one carries `"guard": null`; ticket 01's
`LifecycleGraphDumperUnwrapTest` shows the dumper would have named the Guard bean had any Transition
carried it, so this is a real absence rather than an unwrap failure in disguise. **Path two was
taken**: what is pinned instead is the imperative gate at
`ResourceLifecycleServiceImpl.java:143-145`. ADR 0002's registry must not reimplement a Guard that
has never fired.

**2. Three dead branches in `isSecurityRuleMet`, not one — and the same pattern in both services.**
The ticket advertised three sharp edges in `ResourceLifecycleServiceImpl.isSecurityRuleMet`: an
absent `Authentication` satisfying `isAdmin` (:175), `catch (ClassCastException) → false` (:164-165),
and `catch (NullPointerException) → creatorId = 0L` (:166-168). None of the three is reachable. All
of them sit after line :163, which resolves the caller through
`AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId()`, and that method
(`AuthenticatedUserContext.java:37-45`) wraps its cast in `catch (Exception e) { throw new
AuthenticationCredentialsNotFoundException(...) }`. Every way of arriving without a usable principal
therefore throws before any rule is evaluated, and the exception is neither a `ClassCastException`
nor a `NullPointerException`, so it escapes the service whole. What a caller observes is
`AuthenticationCredentialsNotFoundException("No authenticated user found")`, which is what the two
tests pin. Ticket 03 found the identical dead `catch (ClassCastException)` in
`NegotiationLifecycleServiceImpl`, so this is a pattern across both services rather than a quirk of
one: **defensive branches written around a helper that already converts every failure into one
exception type.** None of them should be carried into the new evaluator.

**3. The silent refusal is silent only when there is a current State to return.** `sendEvent`
refuses by `return getCurrentStateForResource(negotiationId, resourceId)`
(`ResourceLifecycleServiceImpl.java:117`), and that lookup ends in
`orElseThrow(() -> new EntityNotFoundException(negotiationId))` (:134). So a refused Event on a
Resource with no recorded State — whether the link row is blank or absent — raises
`EntityNotFoundException` rather than returning anything, and the message carries the *Negotiation's*
id, not the Resource's. The same two situations are perfectly silent on the read path
(`getPossibleEvents` swallows the same exception into `Set.of()` at :66-68), so the two calls
disagree about whether the situation is an error. Worth stating because "the Resource service never
throws" is the summary everyone will carry into the redesign, and it is not true.

**4. `secured(..., ComparisonType.ALL)` is silently `ANY`.** All thirteen chains ask for
`ComparisonType.ALL` and all thirteen read back `"comparisonType": "ANY"`, because Spring
Statemachine 4.0.0's `AbstractTransitionConfigurer.setSecurityRule` ignores the argument. Pinned as
observed by `everySecurityRule_comparesAny`, because the observed value is what a reimplementation
has to reproduce. Harmless today only because every rule carries exactly one attribute — which
`everyTransition_carriesExactlyOneOfTheThreeKnownRules` also pins, so the day that stops being true
is a failing test rather than a silent behaviour change.

**5. Required Authority is exactly one rule per Transition, and the three partitions are disjoint.**
Three Transitions are `isAdmin`, eight `isRepresentative`, two `isCreator`; no Transition carries
two rules and none is unsecured. That has a practical consequence the delivery chain makes visible:
**no single caller can drive a Resource from `SUBMITTED` to `RESOURCE_MADE_AVAILABLE`.** The walk in
`deliveryChain_reachesResourceMadeAvailable` changes identity three times — admin, representative,
creator, representative. It also means the whole authority model is a function of one column, which
is why the expected offerings in this suite are computed rather than typed out.

**6. The Resource table had nothing binding it either.** Ticket 03's finding 3 applied to the WIP
verbatim: its thirteen-row transition table, its `ALL_STATES` and its `ALL_NEGOTIATION_STATES` were
all hand transcriptions with nothing tying them to `resource-graph-v1.json`, so assertions phrased
over them stated something about the test package rather than about the system.
`ResourceGraphV1BindingTest` closes it, and the Negotiation State universe is now taken from ticket
03's already-bound table via `NegotiationGraphV1.allStateNames()` rather than transcribed a second
time. Any later ticket adding a table of this kind owes the same binding.

**7. `RETURNED_FOR_RESUBMISSION` is a Legacy State, and `RETURN_FOR_RESUBMISSION` and `OVERRIDE` are
Transition-less Events.** The Resource graph's version of ticket 03's finding 4, one name wider: the
Definition declares twelve States and publishes thirteen Events, while the graph uses eleven States
and eleven Events. `OVERRIDE` is the one worth flagging — its own published description is "Override
current state, ignoring state machine guards", and it is refused today as silently as any other
unoffered Event. **There is no override path in the system at all.** Both names are candidate
Override Events in the new model, not names to drop; ADR 0009's seed must carry the Legacy State or
existing rows naming it stop resolving.

**8. `isAdmin` means the `ROLE_ADMIN` authority on the token, not the `admin` column of the Person
row.** Seeded caller 101 has `admin = true`, and without the authority is offered nothing at all —
`adminRule_isMetOnlyByTheRoleAdminAuthority`. A representative authority string does not substitute
for it either. The new Required Authority model should be built against the token, which is what is
actually consulted.

**9. Representing a Resource is per Resource, not per Negotiation.** Caller 105 represents every
Resource of biobank 3 and is offered nothing on `biobank:1:collection:1`, in any State. This is
ticket 03's finding 6 from the other direction — there, representing a Resource of a Negotiation
conferred no authority over the Negotiation; here, representing *some* Resource confers none over
another one. The two together say the Audience of a Resource Transition is scoped to that Resource
exactly.

## Ordering rule, honoured

Ticket 03's rule — any class that drives a Lifecycle must dirty the context after each test method —
applies to `ResourceTransitionParityTest`, which is the only class here that fires an Event. It
declares `@DirtiesContext(AFTER_EACH_TEST_METHOD)`, so the Flyway clean-and-migrate restores the seed
after every method. That matters concretely: this class drives `negotiation-1`'s only Resource to
terminal States, which can conclude `negotiation-1`, and `NegotiationAuthorityParityTest` reads
`negotiation-1` expecting IN_PROGRESS. `ResourcePossibleEventsAuthorityTest` fires nothing — its only
writes are its own SQL, which its `@BeforeEach` restores — so it keeps `AFTER_CLASS`. The full-selector
run above, green including all four of ticket 03's classes, is the check.
