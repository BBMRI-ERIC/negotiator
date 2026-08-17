# The before picture: what characterizing the Lifecycle subsystem found

The resolution of map ticket [01 — Freeze current behaviour](issues/01-freeze-current-behaviour.md).
The gate that protects all of it is [parity-gate.md](parity-gate.md).

**Everything here is reported, never fixed.** That is the whole point: a characterization suite that
needed a production edit to pass would not be characterizing anything. Verified over the slab as a
whole, not per commit:

```
git diff --stat plan/state-machine-redesign..HEAD -- backend/src/main frontend backend/pom.xml pom.xml
```

is **empty** across all 21 commits of the branch. Where a finding is obviously a bug, the fix is a
later decision with a migration story — and pinning it is precisely what stops the cutover changing it
by accident.

**How to read the evidence.** Line references are to `backend/src/main/java/eu/bbmri_eric/negotiator/`
unless another root is named, and were re-checked against the working tree when this report was
written. Where a claim was established by *firing* a real Transition rather than by reading code, the
naming test is given. Per-finding detail lives in the slab's ticket files under
`.scratch/freeze-current-behaviour/issues/`, cited as "(ticket NN)".

---

## Part 1 — The eight questions ticket 11 asked by name

### 1. `NegotiationIsApprovedGuard` is dead code and must not be reimplemented

**Answer: it is attached to no Transition. Not one Transition in either graph carries any Guard.**

Evidence. It is attached only by `negotiation/state_machine/resource/ResourceStateMachineConfig.java:117`:

```java
transitions.withExternal().guard(negotiationIsApproved());
```

— a transition fragment with no source, no event and no target. Spring Statemachine discards it
silently: the Resource graph's dump records `transitionCount` 13, exactly the 13 fully specified
`withExternal()` chains above that line, with no fourteenth orphan carrying null endpoints. All 13
Resource Transitions and all 8 Negotiation Transitions record `"guard" : null` in the committed
mechanical dump (`backend/src/test/resources/lifecycle/{resource,negotiation}-graph-v1.json`).

This is a real absence, not an unwrap failure in disguise. `LifecycleGraphDumperUnwrapTest` feeds
`Guards.from(new NegotiationIsApprovedGuard())` — the exact wrapper Spring Statemachine hands back —
through the same code path the dumper uses, and recovers the bean class. The generator would have
named the Guard had any Transition carried it. (Tickets 01, 04.)

**What actually performs the check the Guard's name suggests** is imperative, at
`negotiation/state_machine/resource/ResourceLifecycleServiceImpl.java:143-144`:

```java
if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
  return Set.of();
}
```

Ticket 04 took the "path two" the ticket offered and pinned that gate instead, walking all eight
declared Negotiation States (`ResourcePossibleEventsAuthorityTest.possibleEvents_areGatedOnTheParentBeingInProgress[1..8]`),
with the IN_PROGRESS row asserting a non-empty set so the gate cannot pass by being empty everywhere.

**Consequence for ADR 0002's Guard registry:** the Guard has never fired in production. Registering it
would not preserve behaviour, it would *introduce* a check that does not exist today. The behaviour to
reproduce is the imperative gate.

### 2. `DRAFT` is occupied but not enterable — and those are two different facts

**Answer: reachable as an entry State, no. Occupied, yes.**

*Not enterable.* `negotiation-graph-v1.json` records `"initialState" : "SUBMITTED"`. `DRAFT` is
declared, and `DRAFT --SUBMIT--> SUBMITTED` is a real Transition leading *out* of it, but **no
Transition anywhere targets `DRAFT`**. A Negotiation that leaves `DRAFT` can never return.

*Occupied.* The seed really does put a Negotiation there — `negotiation-6` — and
`select distinct current_state from negotiation` returns exactly
`{DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED}`. Whatever creates a Negotiation writes `DRAFT` without
going through the Lifecycle.

Pinned in both directions separately, by `NegotiationDraftReachabilityTest.draft_isObservablyOccupied`
and `seededCorpus_occupiesDraftAndNotTheLegacyState` for occupancy, and
`draft_isTargetedByNoTransition`, `draft_isDeclaredByTheDumpButTargetedByNoTransitionOfIt` and
`initialState_isTheDumpsInitialState` for enterability. (Tickets 01, 03.)

**Consequence for ADR 0009's seed:** it must reproduce both facts — `DRAFT` needs an occupant and needs
no inbound Transition. A design that read "occupied" as evidence of "enterable" would model the graph
wrongly. The pattern generalises: ticket 04's finding 10 shows a State can be occupied from outside the
Lifecycle without being enterable through it, so this is not a quirk of `DRAFT`.

### 3. The Information Requirement gate's submission check is not scoped to the requirement

**Answer: confirmed, and it is worse than "any submission satisfies every requirement for that
Resource" — it is also the first thing `sendEvent` does at all.**

Evidence, `ResourceLifecycleServiceImpl.java:110-115`:

```java
if (requirementRepository.existsByForEvent(negotiationResourceEvent)
    && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
        resourceId, negotiationId)) {
  throw new StateMachineException(
      "The requirement for this operation was not met. Please make sure you have submitted the required form and try again.");
}
```

The scoping asymmetry, precisely: **unscoped in the Requirement dimension, scoped in the Resource
`source_id` and Negotiation id dimensions.** Both halves pinned, by
`ResourceInformationRequirementGateTest.submissionAgainstADifferentRequirement_satisfiesTheGate` and
its boundary `submissionForADifferentResource_doesNotSatisfyTheGate`.

Two sharper facts on top of it:

- **`existsByForEvent(event)` is scoped by nothing at all.** One `information_requirement` row anywhere
  blocks that Event for every Resource of every Negotiation in the deployment. This is what makes
  `@DirtiesContext` load-bearing for a second reason beyond test ordering.
- **The correctly scoped query already exists and is deliberately not called.**
  `info_submission/InformationSubmissionRepository.java:9` declares the unscoped
  `existsByResource_SourceIdAndNegotiation_Id`, and `:11` the scoped
  `existsByResource_SourceIdAndNegotiation_IdAndRequirement_Id`, which
  `info_submission/InformationSubmissionServiceImpl.java:76` uses. The gate is **one method call away
  from being "fixed" by accident** during the cutover, which is exactly what pinning it prevents. ADR
  0005's Built-in Stage should treat the change as a decision with a migration story, not a typo.

**The gate outranks everything, not just availability.** Being the first statement of `sendEvent`, before
the Resource's State is even read, it outranks the parent-Negotiation IN_PROGRESS gate, the Required
Authority rules, and "this Event has no Transition anywhere". Pinned by
`unmetRequirement_outranksTheAvailabilityCheck` (3 rows) and `unmetRequirement_outranksTheParentNegotiationGate`.
Consequences ADR 0005 inherits:

- a Requirement recorded for `OVERRIDE` or `RETURN_FOR_RESUBMISSION` — the two Events that carry no
  Transition — turns those silent no-ops into user-facing errors;
- an unauthorised caller gets the Requirement error rather than the silent refusal, which **leaks that a
  Requirement exists** to callers who could never have fired the Event.

**The refusal type is a Spring *data access* exception.** `StateMachineException` extends
`org.springframework.dao.NonTransientDataAccessException`, so any `catch (DataAccessException)` on the
path would swallow the gate's refusal, and the ancestry has nothing to do with the reason it is raised.
That is the strongest argument for *changing* the type at cutover rather than renaming it. It surfaces
via `common/exceptions/NegotiatorExceptionHandler.java:441-447` as HTTP 400 with title
`"Could not advance the state machine"` and the message as `detail`. Per the rule from ticket 09, the
test **documents the class name rather than asserting it** and pins what a caller observes: the message
verbatim, that the throwable is unchecked and causeless, that it is none of the service's own refusal
types, and the HTTP shape. The title is engine-flavoured prose, so rewording it at cutover reddens that
test by design. (Ticket 05.)

**The gate covers `sendEvent` only.** `governance/resource/ResourceServiceImpl.updateResourceStatus`
writes a Resource State without going through it, so it passes no Requirement gate at all — see
finding 12.

### 4. The two services refuse differently: one throws, one silently returns

**Answer: confirmed — and "the Resource service never throws" is false, which is the part everyone will
carry into the redesign.**

*Negotiation side.* An Event that is not currently available raises `ForbiddenRequestException`
(`negotiation/state_machine/negotiation/NegotiationLifecycleServiceImpl.java:61`) with a message that
is the Event's label lowercased, identical with and without the optional message argument, reaching the
frontend verbatim. Pinned three ways — every Event `SUBMITTED` does not offer, an Event offered to
somebody but not this caller, and a caller offered nothing at all
(`NegotiationAuthorityParityTest.sendUnofferedEvent_asAdmin_isForbidden[1..6]`,
`sendAdminOnlyEvent_asCreator_isForbidden[1..2]`, `sendAnyEvent_asUnrelatedUser_isForbidden[1..4]`),
with the message text bound to the labels `/v3/negotiation-lifecycle/events` actually publishes.

*Resource side.* `ResourceLifecycleServiceImpl.java:116-118` refuses by returning the unchanged State
and raising nothing. Pinned four times in two shapes
(`ResourceTransitionParityTest.unofferedEvent_returnsUnchangedStateAndRaisesNothing_unlikeTheNegotiationService[1..2]`,
`eventOnNoTransition_isSilentlyRefused[1..2]`), each asserting nothing was thrown, that the returned
State is unchanged, and — after a three-second `pollDelay` — that it is *still* unchanged, which is what
makes "silent" mean "no-op" rather than "had not landed yet".

**The exception, which the PRD's story 10 does not mention.** The refusal is
`return getCurrentStateForResource(...)` at `:117`, and that lookup ends in
`.orElseThrow(() -> new EntityNotFoundException(negotiationId))` at `:134`. So a refused Event on a
Resource with **no recorded State** — blank link row or no link row — raises `EntityNotFoundException`
carrying the *Negotiation's* id, not the Resource's. The read path swallows the identical lookup failure
into `Set.of()` at `:67`, so the two calls disagree about whether the situation is an error at all.
(Ticket 04.)

### 5. The double-annotated conclusion listener runs exactly once

**Answer: once. The `@TransactionalEventListener` wins and the plain `@EventListener` never produces a
listener at all.**

Evidence. `negotiation/state_machine/negotiation/ResourceStateChangeListener.java:33-34` carries both:

```java
@EventListener(ResourceStateChangeEvent.class)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

Established with a throwaway Mockito spy on the class (invocation count 1, for both the Lifecycle and
the override producer), then the spy was removed because it named a class the redesign deletes. What is
left in the suite is the observable consequence: `NegotiationConclusionTest.conclusion_happensExactlyOnce`
pins one published `CONCLUDE` state change and one added Record per terminal Resource change. The
mechanism is that `EventListenerMethodProcessor` takes the first `EventListenerFactory` that supports a
method and stops, so the transactional factory wins.

**So the invocation count itself is documented, not test-enforced.** Deleting the `@EventListener`
annotation is a no-op today; deleting the `@TransactionalEventListener` one is not.

**And the surviving registration is not harmless.** Because only the transactional one exists, the
method runs strictly after a commit — which is the mechanism behind finding 15. (Ticket 08.)

### 6. Conclusion counts only two of the twelve declared Resource States

**Answer: confirmed. `RESOURCE_MADE_AVAILABLE` and `RESOURCE_UNAVAILABLE`, and nothing else.**

Does **not** count: `SUBMITTED`, `REPRESENTATIVE_CONTACTED`, `REPRESENTATIVE_UNREACHABLE`,
`CHECKING_AVAILABILITY`, `RESOURCE_AVAILABLE`, `RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT`,
`ACCESS_CONDITIONS_INDICATED`, `ACCESS_CONDITIONS_MET`, `RESOURCE_NOT_MADE_AVAILABLE`,
`RETURNED_FOR_RESUBMISSION`.

Walked twice so the table has no holes: `NegotiationConclusionTest.theConclusionPredicate_walkedOverEveryTransition`
(one assertion per State a Transition leads to) and `theConclusionPredicate_walkedOverEveryDeclaredState`
(all twelve declared States, through the override producer — the only way to reach the two States no
Transition leads to). `theUnavailableSoundingStates_doNotCount` names the two that matter outright.

**The two that matter** are `RESOURCE_NOT_MADE_AVAILABLE` — where a researcher's own refusal of the
access conditions lands — and `RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT`. A Negotiation all of whose
Resources end in either is finished in every practical sense and stays `IN_PROGRESS` for ever.

This settles PRD story 15 against observation rather than against the names' apparent meaning, which is
what the story asked for. ADR 0007's `TERMINAL_AGGREGATION` Guard is the place to decide whether to keep
the predicate or widen it — but widening it is a **behaviour change**, not a faithful reproduction.
(Ticket 08.)

### 7. The two graphs evaluate authority through entirely different mechanisms

**Answer: confirmed, and the difference is structural rather than incidental.**

*Negotiation machine.* `withSecurity()` is enabled, and the service matches each Transition's security
rule attributes against the caller's Spring roles — after a **blanket** check that the caller is either
an admin or the Negotiation's creator, which returns `Set.of()` outright to anyone else. Exactly two
Transitions are secured, both `ROLE_ADMIN`, both out of `SUBMITTED` (`APPROVE`, `DECLINE`); the other six
dump `securityRule: null`. So in all six other States an admin and the creator are offered identical
sets, and the practical shape of a creator's authority is: submit your own draft, then nothing until an
admin approves, then pause / unpause / abandon / conclude are all yours. Which two are admin-only is
bound to the dump by `NegotiationGraphV1BindingTest.adminOnlyEvents_areTheDumpsSecuredTransitions`,
which also fails if any Transition were secured by something other than `ROLE_ADMIN`.

*Resource machine.* Spring Statemachine security is **not enabled** at all; the service reimplements
rule evaluation imperatively in `ResourceLifecycleServiceImpl.isSecurityRuleMet` (`:156-179`) against
three rule names. Required Authority is exactly **one rule per Transition** — 3 `isAdmin`,
8 `isRepresentative`, 2 `isCreator`, none unsecured, none with two attributes — pinned by
`ResourceGraphV1BindingTest.everyTransition_carriesExactlyOneOfTheThreeKnownRules`, so the day that
stops being true is a failing test rather than a silent change.

Three facts a unified configuration model needs:

- **`isAdmin` means the `ROLE_ADMIN` authority on the token, not the `admin` column of the Person row.**
  Seeded caller 101 has `admin = true` and, without the authority, is offered nothing anywhere
  (`adminRule_isMetOnlyByTheRoleAdminAuthority[1..3]`). Build the Required Authority model against the
  token, which is what is actually consulted.
- **Representing a Resource is scoped to that Resource exactly** — not to its Negotiation, and not to
  the representative's other Resources. Pinned from both sides: `TheBiobanker` represents Resource 4 of
  `negotiation-1` and is offered nothing over the Negotiation itself (ticket 03), and caller 105
  represents every Resource of biobank 3 and is offered nothing on `biobank:1:collection:1` in any State
  (ticket 04).
- **The delivery chain requires three distinct *rules*, not three distinct people.** Nothing partitions
  callers: `isAdmin` is an authority on the token, `isRepresentative` is a link row, `isCreator` is a
  column of the Negotiation, and one Person can hold all three at once. The
  `deliveryChain_reachesResourceMadeAvailable` walk changes identity three times only because seeded
  callers 101, 109 and 108 happen to be disjoint — a property of the seed, not of the system.

**And on the Negotiation side the blanket check runs before the Negotiation is looked up.** A caller who
is neither admin nor creator gets `Set.of()` for a Negotiation that does not exist, while an admin gets
`EntityNotFoundException`. The new evaluator inherits a decision: does "may not see it" continue to be
indistinguishable from "is not there"? Pinned as-is either way. (Ticket 03, finding 5.)

### 8. The two abandon Transitions are not equivalent

**Answer: confirmed, and the asymmetry is in the post Actions only — every other observer treats the two
identically.**

The Negotiation graph attaches exactly three Actions across eight Transitions. Verbatim before-picture,
read from the committed dump — the one place in the suite that may name an Action bean:

| Transition | `actions` in the dump |
|---|---|
| `DRAFT --SUBMIT--> SUBMITTED` | `EnablePublicPostsAction` |
| `SUBMITTED --APPROVE--> IN_PROGRESS` | `EnablePrivatePostsAction` |
| `IN_PROGRESS --ABANDON--> ABANDONED` | `DisablePostsAction` |
| the other five | `[]` |

Fired for real, the two abandon routes end in the same State with the post flags in different places:
`(true, true)` **survives** an abandon from `PAUSED` and becomes `(false, false)` on an abandon from
`IN_PROGRESS`. Each Transition is fired twice, from `(false,false)` and from `(true,true)`, because the
seed leaves `negotiation-2` and `negotiation-6` public-enabled and a naive walk would pass without the
Action having run at all. (Ticket 06.)

**But nothing else distinguishes them.** The Lifecycle Record captures the destination State only, so the
two abandons leave rows that are *equal* once id and timestamps are stripped (ticket 07); and all three
Negotiation-side notification handlers plus spawn key on the State arrived in rather than on the Event,
so the two are indistinguishable there too (ticket 08). So "not equivalent" is true at exactly one seam,
which is worth knowing before ADR 0002 decides where to attach an Action.

**The message-borne post is not an Action and must not be registered as one.** It is written
unconditionally on the same path that writes the new State, from a header the service sets on every
send, so it appears on Transitions with and without Actions alike — pinned across `APPROVE` (has an
Action), `DECLINE` (none) and `ABANDON`-from-`PAUSED` (none). Registering it per-Transition would be
wrong in both directions. Two edges of it: the emptiness check is `isEmpty`, not `isBlank`, so a
single-space message creates a post (`eventWithABlankMessage_createsAPostAllTheSame`); and the author is
`personRepository.findById(...).orElse(null)`, so a principal with no Person row would yield an
authorless post rather than a refusal — read from source, not reachable through the Lifecycle seam,
documented rather than pinned.

---

## Part 2 — The rest of the load-bearing set

### 9. `secured(..., ComparisonType.ALL)` has silently been `ANY` all along

All 15 secured Transitions across both configuration classes declare
`SecurityRule.ComparisonType.ALL`; the dump records `"comparisonType" : "ANY"` on every one. Not a dump
defect: Spring Statemachine 4.0.0's `AbstractTransitionConfigurer.setSecurityRule(String, ComparisonType)`
**ignores its `ComparisonType` argument** — its bytecode calls only `setAttributes(...)` — and
`SecurityRule`'s constructor defaults the comparison type to `ANY`. The declared `ALL` is dropped on the
floor by the library.

Harmless today only because every rule carries exactly one attribute, over which `ANY` and `ALL` are
indistinguishable. Pinned as observed (`ResourceGraphV1BindingTest.everySecurityRule_comparesAny`).
**ADR 0002's Required Authority model should be built against the behaviour, not the builder chain**, and
nobody should later "restore" an `ALL` semantics that has never been in effect. (Tickets 01, 04.)

### 10. Both graphs declare more States than their Transitions use — two Legacy States

Both configurations register the whole enum via `EnumSet.allOf(...)`, so the Negotiation graph has **8**
States (not the 6 the PRD hand-counted) and the Resource graph **12**. The extras are Legacy States in
`backend/CONTEXT.md`'s exact sense — declared, but no Transition leads to one:

- **`APPROVED`** (Negotiation). Source and target of no Transition, and **nothing in the seed occupies
  it** — the occupied set is exactly `{DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED}`. Unlike `DRAFT` it has
  no live occupant at all, which is what makes it free to be modelled as Legacy rather than as an entry
  point from outside.
- **`RETURNED_FOR_RESUBMISSION`** (Resource).

Transition counts, by contrast, match the hand count exactly: 8 and 13.

**ADR 0009's seed must carry both**, or existing rows naming them stop resolving — and the universe that
has to hold is the **declared** one, not the reachable one. Ticket 07 pinned over the whole of both
Record tables that every recorded State name is one the Definition declares, Legacy States included,
which is the precondition the backfill rests on. (Tickets 01, 03, 04, 07.)

### 11. Both Event universes are wider than their dump — three Transition-less Events

The dumps' `events` arrays name only the Events that trigger a Transition. Three more are declared,
published by the metadata endpoints, and nameable by any caller, while carrying no Transition and
therefore never being offered from any State:

- **`START`** (Negotiation) — Event universe 8, dump lists 7. (Ticket 03.)
- **`RETURN_FOR_RESUBMISSION`** (Resource).
- **`OVERRIDE`** (Resource), whose own published description is *"Override current state, ignoring state
  machine guards"* — and which is refused at the Lifecycle seam as silently as any other unoffered Event
  (`eventOnNoTransition_isSilentlyRefused[1..2]`, sent by an admin who is being offered other Events at
  the same moment so the refusal cannot be confused with the IN_PROGRESS gate).

That last is a statement about `ResourceLifecycleService.sendEvent` **only**. An out-of-Lifecycle override
path does exist and stamps this same Event name — finding 12. All three names are **candidate Override
Events in the new model, not names to drop.** (Tickets 03, 04, 09.)

### 12. `ResourceStateChangeEvent` has a second producer, and it is the seam's biggest blind spot

`governance/resource/ResourceServiceImpl.updateResourceStatus` (`:178-194`) writes an arbitrary State
straight onto the link row through `negotiation.setStateForResource(...)` at `:183`/`:185` — consulting
**no** Transition, **no** Required Authority rule of the graph, **no** IN_PROGRESS gate and **no**
Information Requirement gate — and then publishes a `ResourceStateChangeEvent` stamped
`NegotiationResourceEvent.OVERRIDE` (`:186-193`). It is reachable from
`PATCH /negotiations/{id}/resources` (`negotiation/NegotiationController.java:297`), gated by
`verifyAuthForStatusUpdate`: admin, or representative of *every* Resource in the payload.

**So a `ResourceStateChangeEvent` is not in general the trace of a Transition.** The PRD's event seam
(stories 13 and 19) and ticket 08's own premise both read it as one. Ticket 04 found this and left it;
ticket 08 decided to pin the path, for three reasons: the conclusion predicate cannot be walked over all
twelve declared States without it, a lifecycle-keyed handler *does* fire on an Override-stamped event, and
the only pre-existing test of automatic conclusion
(`integration/service/NegotiationLifecycleServiceImplTest.successfulNegotiation_2finishedResources_closedAutomatically`)
drives this path rather than the Lifecycle. The adapter grew `overrideResourceStates` so the suite reaches
it without naming a State enum.

Two things pinning it found:

- **The override path also spawns.** Updating Resources on an `IN_PROGRESS` Negotiation publishes
  `NewResourcesAddedEvent`, which initialises any stateless Resource exactly as arriving at `IN_PROGRESS`
  does.
- **It is silent in exactly one case:** writing the *initial* State onto a link row that already has a
  State writes nothing and publishes nothing (`isUninitialized` requires the previous State to be `null`,
  and `isStateMachineInitialized` excludes the initial State). Every other State, including a rewrite of
  the State already there, writes and publishes. Making the override uniform would start publishing an
  event this path has never published.

**Still unpinned, deliberately** — the governance service's own seam, not the Lifecycle's: the override's
authorisation rule, its `DRAFT` branch, and its `NewResourcesAddedEvent` branch. This is a named coverage
gap, see Part 6. (Tickets 04, 05, 07, 08.)

### 13. Five dead defensive branches, and one shape behind all of them

None of the five is reachable, and they all sit downstream of the same helper.

| Branch | Location |
|---|---|
| `NegotiationIsApprovedGuard` | `ResourceStateMachineConfig.java:117` (finding 1) |
| `catch (ClassCastException) → ForbiddenRequestException` | `NegotiationLifecycleServiceImpl.java:89-90` |
| `catch (ClassCastException) → false` | `ResourceLifecycleServiceImpl.java:164-165` |
| `catch (NullPointerException) → creatorId = 0L` | `ResourceLifecycleServiceImpl.java:166-167` |
| absent `Authentication` satisfies `isAdmin` | `ResourceLifecycleServiceImpl.java:175` |

The shape: every one of the last four sits *after* a call to
`common/AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId()`, which wraps its cast in
`catch (Exception e) { throw new AuthenticationCredentialsNotFoundException("No authenticated user found"); }`
(`AuthenticatedUserContext.java:37-45`). Every way of arriving without a usable principal therefore
throws before any rule is evaluated, and the escaping exception is neither a `ClassCastException` nor a
`NullPointerException`, so nothing downstream ever sees what it was written to catch. **Defensive
branches written around a helper that already normalises its failures.**

What a caller actually observes is pinned instead —
`ResourcePossibleEventsAuthorityTest.possibleEvents_withoutAuthentication_raisesCredentialsNotFound`
and `possibleEvents_withForeignPrincipal_raisesCredentialsNotFound`. Ticket 04's criterion 9 was reworded
for this reason, which is recorded in its own file.

**None should be carried into the new evaluator.** (Tickets 01, 03, 04.)

### 14. The Lifecycle history trail records State *assignments*, not Transitions

Rows are written by the entity on any write — `negotiation/Negotiation.setCurrentState` (`:132`) and
`setStateForResource` → `buildResourceStateChangeRecord` (`:163-171`, `:225-233`) — not by the Lifecycle
machinery. So three producers exist that no Transition accounts for: **spawn**, the **Override path**, and
**automatic conclusion**, which runs through `runAsSystemUser` and therefore attributes its Negotiation
Record to **Person 0** rather than to the caller whose Resource Event triggered it
(`NegotiationConclusionTest.conclusion_isPerformedAsTheSystemUser`). Anyone reading the trail as "who did
this" needs to know that.

Four more facts ADR 0008's FK conversion inherits:

- **A Record captures the destination State and nothing else** — not the origin, not the Event. Settled by
  firing, not by reading the entity: `ABANDON` into `ABANDONED` from `IN_PROGRESS` versus from `PAUSED`,
  and `CONTACT` into `REPRESENTATIVE_CONTACTED` from `SUBMITTED` versus from `REPRESENTATIVE_UNREACHABLE`,
  leave rows that are *equal* once id and the two timestamps are stripped. Row order is therefore
  load-bearing, the only ordering key is the identity `id` (`creation_date` agreed on every path pinned,
  but nothing enforces it), and the **first row of a trail has no recoverable origin at all**. This matches
  ADR 0008's own deferrals — the ADR is accurate, and now tested.
- **Records are not deduplicated and must not become so.** Both collections are `HashSet`s and neither
  Record type overrides `equals`/`hashCode`. Giving a Record value-based equality after the conversion —
  `state_id` + `negotiation_id` looks like a natural key — would silently lose every revisit. Pinned by
  driving a path through `IN_PROGRESS` twice and asserting four rows.
- **`buildResourceStateChangeRecord` silently drops `SUBMITTED`** (`Negotiation.java:226`:
  `if (!state.equals(NegotiationResourceState.SUBMITTED))`). Unreachable through the Lifecycle seam — no
  Resource Transition targets `SUBMITTED` and spawn does not use it — so the only live route is an Override
  to `SUBMITTED`, which finding 12 shows writes nothing anyway. Documented, not pinned. A reimplementation
  that dropped the special case would start writing rows this trail has never contained.
- **A third place the State universe is written down.** Both tables carry a `CHECK` constraint enumerating
  State names — `negotiation_lifecycle_record_changed_to_check` (8 names after
  `V22.0__add_draft_state_to_check_constraint.sql`) and `negotiation_resource_changed_to_check` (12 names,
  `V2.0__Add_auditing_to_missing_tables.sql`). A converted `state_id` cannot satisfy a check against
  varchar State names, so ADR 0009's conversion has to drop them. Recorded as a before-picture rather than
  asserted, per ticket 09's rule. (Ticket 07.)

### 15. No handler is reachable by an event published outside a transaction — and the scheduled reminder is published outside one

`notification/internal/NotificationListener.onNewEvent` is the single dispatcher between every application
event and all eight notification strategies, and it is `@Async @TransactionalEventListener` with default
fallback (`NotificationListener.java:40-42`), so an event published with no transaction in progress is
dropped silently. Every lifecycle event in the system is published from inside a transaction, so this is
invisible there — but `NotificationScheduler.forPendingNegotiations`, the only production publisher of
`PendingNegotiationReminderEvent`, is a plain `@Scheduled` method with no transaction
(`NotificationScheduler.java:16-17`, cron default `0 0 6 * * *`).

Pinned both ways in `LifecycleNotificationHandlerTest.noHandlerIsReached_whenTheEventIsPublishedOutsideATransaction`:
the same event, published the same way, twice, differing only in the transaction.

**On this evidence the daily pending reminder has never reached a representative in production.** Not
fixed — reported. (Ticket 08.)

### 16. Four handler facts that are not firing conditions

- **`ResourceStateChangeHandler` has no firing condition.** It fires on every published Resource state
  change, from either producer, and notifies the Negotiation's creator. Its dependence on lifecycle
  identity is entirely in its *content*: the body is built from the origin and destination States' published
  labels, so the suite computes the expected body from the committed `resource-states.json` rather than
  transcribing labels twice. **ADR 0009's seed has to carry those labels** or this notification degrades to
  `null`s in a sentence.
- **The pending reminder handler ignores the Negotiation's own State.** It selects Negotiations by creation
  date alone and then looks only at Resource States, so an `ABANDONED` Negotiation whose Resource is still
  `REPRESENTATIVE_CONTACTED` goes on reminding its representatives
  (`pendingReminderHandler_remindsEvenAboutAnAbandonedNegotiation`).
- **The administrators the submission handler notifies are the `admin` column, not `ROLE_ADMIN`.** Person 0
  — the system user conclusion runs as — has `admin = false` and is therefore never notified of submissions,
  while being treated as an administrator by every role-based check because `runAsSystemUser` grants itself
  `ROLE_ADMIN`. Same shape of split as finding 7's `isAdmin`.
- **Two notification titles are shared with handlers that are not lifecycle-keyed:** `"New Request"` with
  `NewNegotiationHandler`, `"New Negotiation Request"` with `UpdatedResourcesHandler`. Within this suite a
  title identifies its handler because nothing creates a Negotiation and every Resource named is already
  linked. **A later suite that creates Negotiations must not inherit that assumption.** (Ticket 08.)

### 17. The published REST surface: four facts a relational Definition must reproduce

- **The unrecognised-name failure is two failure modes**, because the two Event path variables have
  registered `Converter`s and the two State path variables do not. State endpoints return 400 with
  `application/json` and a `detail` of `No enum constant …NegotiationState.NOT_A_STATE` — **the body leaks a
  fully-qualified Java class name of a class ADR 0002 deletes.** Event endpoints return 400 with **no
  content type and an empty body**, because the converters swallow the `IllegalArgumentException` and throw a
  reasonless `ResponseStatusException`. Same converters, second consequence: they call
  `valueOf(source.toUpperCase())`, so `/events/approve` returns 200 while `/states/submitted` returns 400.
  The `@Valid` on all four path variables is inert — conversion fails before validation runs. The verbatim
  bodies are recorded in ticket 09; the test pins status, content type, `title`, `status`, and that `detail`
  is *derived from* the rejected name. See Part 4 on why.
- **`ResourceStateMetadataDto` alone carries an `ordinal` field** — 0 (`SUBMITTED`) … 11
  (`RESOURCE_MADE_AVAILABLE`), set from the enum constant's `ordinal()`, a published integer rank whose
  source file documents the declaration order as significant. Relational configuration must reproduce it and
  keep it stable.
- **Collection order is not a contract.** All four collection endpoints build their payload with
  `Collectors.toSet()`, so `_embedded` arrives in `HashSet` hash order — not declaration order, not
  alphabetical, and not stable under a change of member set. The member set and each member's fields are the
  contract; the array order is not.
- **The diagram endpoint has no visited set.** `GET /v3/resource-lifecycle` renders 13 configured Transitions
  as **29 transition nodes**, emits the `REPRESENTATIVE_CONTACTED` subtree twice and
  `ACCESS_CONDITIONS_INDICATED` four times, and nests **14** objects deep, because `traverseState` descends
  unconditionally into every target. **It terminates only because the Resource Lifecycle happens to be
  acyclic** — wiring the dangling `RETURN_FOR_RESUBMISSION` into `RETURNED_FOR_RESUBMISSION` and back turns
  the endpoint into `StackOverflowError`, not a larger response. A test walks the response and fails if any
  State repeats on a root-to-leaf path, so the property is pinned rather than assumed. **Any reimplementation
  needs a visited set or a depth bound.** Also: the diagram cannot be used to enumerate either universe —
  the three Transition-less names appear nowhere in it, and States with no outgoing Transition appear only as
  `target` values. All five endpoints are `permitAll` for GET and are pinned as an anonymous client calls
  them. (Ticket 09.)

### 18. The requirement hint's inclusion condition is caller-dependent, and ADR 0005 makes it caller-independent

Today `governance/resource/ResourceWithStatusAssembler.addRequirementLink` includes a hint only if a link
whose rel equals the Requirement's Event is already in the list (`:135-136`), evaluated *after*
`addLifecycleLink` has run — and that link's source is `getPossibleEvents`, Required Authority filter
included. **So a Requirement is hinted at only to callers who could fire the Event:** on the seeded Resource
the administrator sees `requirement-<id>`, the Negotiation's creator — offered nothing from that State —
does not. Pinned in both directions on the same Requirement row.

Structural reachability is a property of the graph, so after the cutover the creator, who can never fire
`CONTACT`, will be told a form is required for it. **That visibility change is a consequence neither ADR 0005
nor ticket 10 mentions.** It may well be wanted — the Audience of a Requirement is a different question from
the Required Authority of the Event (ADR 0006) — but it should be a decision.

Three more from the same seam, all in the intended-delta half of the gate:

- **`getPossibleEvents` is the single cause of the dead-click bug on both surfaces.** The assembler calls the
  same method the Lifecycle endpoint does. Fixing the listing inside the Evaluation Pipeline fixes the HAL
  links at the same time, so **no assembler change is needed for that delta** — only for the three link
  fixes. Worth stating because "the assembler advertises it too" reads like a second site to repair.
- **The frontend does not render the text ADR 0005 changes.** The ADR changes the hint's HAL `name`, today
  `"CONTACT requirement"` (`ResourceWithStatusAssembler.java:141`). `frontend/src/components/ResourceItem.vue:67`
  renders `link.title`, which is the Access Form's name. So the display-name fix alone changes nothing a user
  sees on a requirement hint; it does change a Submission link, where line 60 renders `link.name`. Whoever
  lands ADR 0005 should decide whether the intended change belongs in `name` or `title`. Both texts are now
  pinned on both link kinds.
- **`"Next Lifecycle event"` is a load-bearing magic string.** `ResourceItem.vue:108` picks lifecycle links
  out of `_links` by `link.title === 'Next Lifecycle event'` and never looks at the rel. The cutover may
  rename lifecycle rels freely, but rewording that title silently removes every "Update status" control from
  the Negotiation page. Nothing pinned it before ticket 10.
- **Submission links have no inclusion condition at all** — every Submission of the Negotiation belonging to
  this Resource is linked, whatever the State and whoever is asking. ADR 0005 collapses the *rels* for both
  kinds but changes the inclusion condition for Requirements only; applying structural reachability to
  Submissions would hide forms people have already filled in. (Ticket 10.)

---

## Part 3 — Corrections this slab owes upward

Four documents the later slabs are written against are wrong, all four from the same root, plus one that
turned out right.

### C1. PRD story 13 and ticket 08's own description of spawn are both wrong

**What they say:** "a Negotiation reaching IN_PROGRESS initialises its Resources" to the **initial Resource
State**, and the spawn path is observable through the event seam.

**What actually happens.** Spawn is `notification/internal/ResourceNotificationService.handleResourceStateManagement`,
and it writes **`REPRESENTATIVE_CONTACTED`** for a Resource that has a representative and
**`REPRESENTATIVE_UNREACHABLE`** for one that does not. It never writes the graph's initial State
`SUBMITTED`. And it publishes **no `ResourceStateChangeEvent` at all** — three Resources can change State
in one spawn and nothing is announced, so the Resource-state-change handler does not fire, the conclusion
listener never runs, and the webhook subsystem never hears of it. Every *other* writer of a Resource State
publishes one.

Pinned as observed by `NegotiationSpawnTest.reachingInProgress_initialisesEveryResourceThatHadNoState`,
`spawn_doesNotUseTheGraphsInitialState` (which states the divergence outright),
`reachingInProgress_notifiesTheRepresentatives` and `spawn_skipsAResourceThatAlreadyHasAState`. (Ticket 08,
findings 1 and 2.)

**Also: spawn keys on the destination State, not on the Event.** `PAUSED --UNPAUSE--> IN_PROGRESS` spawns
exactly as `SUBMITTED --APPROVE--> IN_PROGRESS` does.

The PRD has been amended with a visible correction; see
`.scratch/freeze-current-behaviour/PRD.md` story 13.

### C2. ADR 0007's `SPAWN_RESOURCE_LIFECYCLES` is specified against that wrong picture — twice over

`backend/docs/adr/0007-lifecycle-coupling-is-orchestration.md:22` specifies the Action as: per requested
Resource, resolve the family, pin the version, and **"set the State to that definition's initial one"**,
with its wiring configured **"exactly once — one row on the sole Negotiation definition's approval
Transition"**. Both halves diverge from observed behaviour:

- **The initial State is the wrong State.** Starting a spawned Resource at `SUBMITTED` moves every
  representative one step backwards and re-offers them `CONTACT` / `MARK_AS_UNREACHABLE`, which are
  `isAdmin` Events — so the representative would be offered nothing at all where today they are contacted.
  The Action must reproduce the **two-way choice** on whether the Resource has a representative.
- **Wiring it to the approval Transition alone loses the resume case.** Spawn keys on arriving at
  `IN_PROGRESS`, so `UNPAUSE` spawns today. A single wiring row on `APPROVE` is a behaviour change for
  every resumed Negotiation.

`backend/docs/adr/0009-forward-only-convert-in-place-migration.md:16` inherits the second half: it seeds
"the single `SPAWN_RESOURCE_LIFECYCLES` Action on the approval Transition". And
`backend/CONTEXT.md`'s **Spawn** entry defines the term itself as "set its State to that definition's
initial one", so the vocabulary builds in the divergence.

**Not edited here.** The ADRs are settled per the map's binding constraints — "reopening one is out of
scope unless implementation surfaces a genuine contradiction, and then it is its own decision ticket,
never a quiet edit" — and this is that contradiction. `backend/CONTEXT.md` is a domain-vocabulary document
maintained through `/domain-modeling`, so its Spawn definition is a modelling decision, not a factual
patch. **Both are flagged for a decision, not amended.**

### C3. The map's "no seed, demo or test SQL exists anywhere" was false — already corrected

`backend/src/main/resources/db/test/migration/R__Initial_data.sql` and `db/dev/migration/R__Initial_data.sql`
are repeatable Flyway seeds wired in through `application-test.yaml`, and they already populate
`negotiation.current_state`, `negotiation_resource_link.current_state`, `negotiation_lifecycle_record` and
`negotiation_resource_lifecycle_record`. The whole suite is built on them via
`@IntegrationTest(loadTestData = true)`. Corrected on the map's verified facts on 2026-08-13 while charting
the slab; recorded here for completeness because it had propagated into a ticket premise.

### C4. Ticket 01's "seven `NotificationStrategy` handlers" is wrong on both counts

There are **eight** strategies, of which **five** key on lifecycle identity:
`NegotiationInProgressHandler` (IN_PROGRESS), `NegotiationSubmissionHandler` (SUBMITTED),
`NegotiationStatusChangeHandler`, `ResourceStateChangeHandler` and `PendingNegotiationReminderHandler`
(`REPRESENTATIVE_CONTACTED`). `NewPostHandler` and `UpdatedResourcesHandler` do not key on State, and
`NewNegotiationHandler` keys on a non-lifecycle event. Only the five are pinned, and
`LifecycleNotificationHandlerTest.theFiveLifecycleKeyedHandlers_areAllWalked` fails if a sixth appears.

### C5. Nothing in ticket 10 contradicted anything — worth saying

PRD story 23, the PRD's "Intended deltas, not parity" section and ADR 0005's own paragraph on the three
assembler fixes all match the code line for line. That is the one slice with nothing to correct upward, and
it is stated rather than left as silence, given the PRD was wrong twice.

---

## Part 4 — Documented rather than asserted: the deliberate register

A test that names something the redesign deletes is a guaranteed delta dressed as parity. Ticket 09
established the rule after the forbidden-reference guard rejected a verbatim assertion containing
`...NegotiationState.NOT_A_STATE` — an assertion that could only ever go red at cutover. **That is the
guard catching something neither agent could see alone, which is the argument for having built it.**

So the following are recorded as a before-picture and **no test enforces them**. A cutover that changes any
of them breaks nothing; this list is the only warning.

| Recorded, not asserted | Where |
|---|---|
| `StateMachineException`'s class name and its `NonTransientDataAccessException` ancestry | finding 3 |
| The verbatim unrecognised-State response bodies | finding 17, ticket 09 |
| The two `CHECK` constraints enumerating State names | finding 14 |
| The three Action bean class names | finding 8 — named in one place, `NegotiationGraphV1BindingTest`'s `EFFECT_OF_ACTION`, which reads the frozen dump; every behavioural assertion speaks only of a `PostEffect` enum, so ADR 0002 re-registering the beans reddens nothing |
| The conclusion listener's invocation count of 1 | finding 5 — a spy would have named a class the redesign deletes; what is enforced is the observable consequence |
| `buildResourceStateChangeRecord` dropping `SUBMITTED` | finding 14 — unreachable |
| The authorless post from a principal with no Person row | finding 8 — unreachable through the Lifecycle seam |
| Three shapes in `ResourceWithStatusAssembler`: `requirementsCache` a `static` field on a singleton `@Component` (`:36`) reassigned per `toModel` call so concurrent requests interleave, and both caches refreshed once per Resource so N Resources cost 2N service calls; all three link loops wrapping the whole `for` in `catch (Exception)` (`:94`, `:122`, `:153`) so one bad row silently drops every *remaining* link with a 200 still returned — `addLifecycleLink` included, so a failure inside `getPossibleEvents` renders a Resource with no controls and no error; and `addSubmissionLink` reaching the requirement cache with `.findFirst().get()` (`:102-106`), which throws if a Submission outlives its Requirement, into that same catch | ticket 10, finding 5 — reaching any of them needs corrupted data or concurrency, and a test that manufactured either would be pinning its fixture |
| The override path's own authorisation rule, `DRAFT` branch and `NewResourcesAddedEvent` branch | finding 12 |
| Two Resource identifiers live in one method: `addSubmissionLink` matches on database **row ids** while `addLifecycleLink` four lines away keys on the **source id**, as every Lifecycle surface does | ticket 10, finding 7 — the two are not interchangeable and the seed makes them look similar (row 4 ↔ `biobank:1:collection:1`) |

---

## Part 5 — Mechanical confirmations

Run from the repository root on `feat/state-machine-implementation`, and re-run after this report's own
`package-info.java` was added, with identical results — it names nothing either grep forbids.

### The four Lifecycle enums are named by exactly one file of the suite

```
grep -rnE '\b(NegotiationState|NegotiationResourceState|NegotiationEvent|NegotiationResourceEvent)\b' \
  backend/src/test/java/eu/bbmri_eric/negotiator/characterization --include='*.java'
```

14 hits in 3 files, of which **only one is code**:

- `adapter/EnumBackedLifecycleTestAdapter.java` — 12 hits: three imports (`:8`, `:10`, `:11`) and their nine
  uses in the three private converters. This is the sanctioned single file.
- `dump/LifecycleGraphDumpDriftTest.java:81` — inside a `//` comment.
- `guard/CharacterizationImportGuardTest.java:62` — inside a javadoc.

The guard blanks comments before scanning (`codeLines`), so both non-code hits are correctly not
violations. **Confirmed.**

Two precisions worth recording rather than smoothing over:

- **The suite names only three of the four enums.** `NegotiationState` appears nowhere in the suite as code
  at all — the adapter returns Negotiation State names as strings from the service's own value, so no
  string-to-enum conversion for that type was ever needed.
- **The criterion is about the suite, not the test tree.** Across all of `backend/src/test`, **29 files**
  reference the four enums. That is expected and out of scope: the PRD lists "the 26 test files that
  reference the four enums" as churn for the decouple-consumers slab, and the number is 29 today. The
  enforced property is the *characterization* tree.

### No Spring Statemachine type is named outside the throwaway dump package

```
grep -rn 'org\.springframework\.statemachine' \
  backend/src/test/java/eu/bbmri_eric/negotiator/characterization --include='*.java'
```

14 hits, **all 14 inside `characterization/dump/`**, spread over five files:
`LifecycleGraphDumper.java` (8: 7 imports plus the `SSM_PACKAGE_PREFIX` constant at `:51`),
`LifecycleGraphDumperUnwrapTest.java` (3), and one import each in `LifecycleGraphArtifacts.java`,
`LifecycleGraphDumpDriftTest.java` and `LifecycleGraphDumpGeneratorTest.java`. Zero hits anywhere else in
the tree, `delta/` included. **Confirmed.**

**Stated precisely:** the exemption is the *package*, not a single generator file — the guard's
`isInDumpPackage` check is package-scoped, and all five files are throwaway together, deleted with the
library at cutover. Outside the characterization tree, one pre-existing test still imports
`org.springframework.statemachine`: `integration/service/NegotiationLifecycleServiceImplTest`. It is not
part of the suite and is not a violation, but it is one more file the cutover has to touch.

### Total count of pinned behaviours, so a later slab notices the suite shrinking

| Measure | Parity | Intended delta | Total |
|---|---|---|---|
| Test **invocations** (surefire) | **255** in 24 classes, 1 skipped | **8** in 1 class | **263** in 25 |
| Test **methods declared** (`@Test` / `@ParameterizedTest`) | **165** | **8** | **173** |
| Java files in the suite | 39 | 2 | 41 |

The two counts differ because parameterized methods carry the whole-graph walks: one
`@ParameterizedTest` can fire all 13 Resource Transitions. Both numbers are worth keeping — a refactor that
merges methods moves the second and not the first, and a refactor that narrows a walk moves the first and not
the second.

**If either number falls, something was deleted rather than migrated.** The one legitimate historical
decrease is on record: a follow-up review deleted
`NegotiationDraftReachabilityTest.approved_isDeclaredButNeverEntered`, a strictly weaker duplicate of
`NegotiationGraphV1BindingTest.legacyState_isDeclaredButUnusedInTheDump` (which makes the same statement
against the mechanical dump), taking the total from 158 to 157 at the time. No assertion about the system was
weakened.

### Two properties the suite enforces about itself

- **`CharacterizationImportGuardTest`** (3 tests) is the mechanical form of the string-and-adapter rule. Its
  third test asserts the scan found a real, non-empty tree *and* that the exempted adapter file still exists,
  so the guard can never pass by scanning nothing or by exempting a file that was renamed away. It was
  demonstrated failing on a deliberately introduced violation, then reverted (ticket 02).
- **`LifecycleGraphDumpDriftTest`** (9 tests) regenerates both graph JSON files and the Mermaid diagram from
  the live beans on every run and asserts byte equality against the committed copies, so the artifact the
  whole slab reasons from cannot silently drift from the beans it claims to describe.

### Hand-transcribed tables are bound to the mechanical dump

Ticket 03 shipped its parity table as a transcription, and two of its findings were assertions over that
constant and so could not fail — they stated something about the test package rather than about the system.
Both graph tables are now equated edge for edge to the committed dump and to the committed metadata
(`NegotiationGraphV1BindingTest`, `ResourceGraphV1BindingTest`), and expected offerings are **computed from
the bound table, never typed out**, so an expectation stays complete if the graph is larger than the rows
anyone remembered to type. **Any later ticket adding a table of this kind owes the same binding.**

---

## Part 6 — Coverage gaps, stated so the gate is not over-trusted

Known by design:

1. **The frontend.** Standing decision 5. No unit-test runner exists; the four Cypress specs reference
   nothing about requirements, submissions, lifecycle or state. The two forced breakages are cited with
   file and line in ticket 10's finding 6, and repaired by hand in whichever slab lands ADR 0005.
2. **The intended deltas**, by construction — the other half of the gate.
3. **Anything reachable only through Spring Statemachine internals.** `characterization/dump` is the
   sanctioned exception and is deleted at cutover, so whatever only it can see is unprotected from that
   moment on.
4. **The `minimal-workflow` Spring profile** (`db9019d4`, on `master`, not on this branch). Its
   `SimplifiedResourceStateMachineConfig` registers the `resourceStateMachine` bean with five Transitions;
   the dump and the suite describe the default graph only. **This is settled and needs no follow-up** — the
   profile exists only because Lifecycles are not yet customizable, it declares no new State so ADR 0009's
   migration is unaffected wherever it is deployed, and customizable Lifecycles replace it during the
   rollout. Read the absence of coverage as intended.
5. **The `OVERRIDE` producer's own seam** — finding 12. The path is pinned; the governance service's
   authorisation rule, `DRAFT` branch and `NewResourcesAddedEvent` branch are not.

Not by design, and worth knowing:

6. **Everything in Part 4** is documented rather than asserted. Changing any of it breaks no test.
7. **Two effects have no separable test today** because the graph does not distinguish them: the
   creation-date reset keys on arriving in `SUBMITTED` rather than on the `SUBMIT` Event, and exactly one
   Transition targets `SUBMITTED` — if ADR 0009's seed adds a second, that becomes two behaviours; and the
   two `ABANDON` Transitions are distinguishable only at the post-Action seam (finding 8).
8. **Refusal timing is pinned with a three-second settling period**, not proven. "Silent" means "no-op after
   three seconds", which is the strongest claim an asynchronous persist path admits.
9. **The suite does not cover Negotiation *creation***, which is what writes `DRAFT` without going through
   the Lifecycle (finding 2), nor the webhook subsystem, which is a second consumer of both state-change
   events.

---

## Part 7 — Decisions the redesign now owes, gathered

Each is a place where reproducing observed behaviour and following a settled ADR are not the same act, or
where the observed behaviour is plainly not what anyone wanted. **None of them is decided here** — the
slab's job was to make them visible and to make sure the cutover cannot resolve one by accident.

| Decision | Where it bites | Finding |
|---|---|---|
| Spawn's initial State, and whether spawn wires to `APPROVE` or to arriving at `IN_PROGRESS` | ADR 0007's `SPAWN_RESOURCE_LIFECYCLES`, ADR 0009's seed, `backend/CONTEXT.md`'s Spawn entry | C1, C2 |
| Whether the requirement submission check becomes requirement-scoped, and the migration story if so | ADR 0005's Built-in Stage | 3 |
| Whether conclusion widens beyond two States | ADR 0007's `TERMINAL_AGGREGATION` | 6 |
| Whether the requirement hint's new caller-independence is wanted | ADR 0005 + ADR 0006's Audience | 18 |
| Whether ADR 0005's display-name change belongs in HAL `name` or `title` | the frontend renders `title` for hints, `name` for submissions | 18 |
| Whether "may not see it" stays indistinguishable from "is not there" | the Negotiation service's blanket authority check | 7 |
| Whether the two services' refusal asymmetry is unified, and what replaces the `EntityNotFoundException` edge | both Lifecycle services | 4 |
| What replaces `StateMachineException`, given it is a data-access exception today | ADR 0005, `NegotiatorExceptionHandler` | 3 |
| Whether the daily pending reminder is repaired | `NotificationScheduler` is not transactional | 15 |
| Whether the diagram endpoint is reimplemented at all, and with what cycle protection | `GET /v3/resource-lifecycle` | 17 |
| Whether `ordinal` stays a published contract | `ResourceStateMetadataDto` | 17 |
| Whether the override path gains the Requirement gate, the IN_PROGRESS gate, or a uniform publish | governance vs. Lifecycle seam | 12 |
