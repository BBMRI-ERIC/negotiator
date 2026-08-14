# Event seam: spawn, conclusion, and notification firing conditions

Status: ready-for-human

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin everything that happens *because* of a transition rather than *in* it, observed through recorded
application events rather than through delivered mail. This is the likeliest silent breakage of the
enum removal, and no ADR owns it.

**The state change events themselves.** Each transition publishes an event carrying the origin state,
destination state and the triggering event. Pin those payloads for both graphs — they are the
contract every handler and the webhook subsystem reads.

**Spawn.** A Negotiation reaching in-progress initialises its Resources and notifies their
representatives, via the handler that keys on the destination state. Pin that the Resources start in
the initial Resource state and that representatives are notified. ADR 0007 relocates this into a
spawn Action and needs an equivalence check.

**Conclusion.** Once every Resource in a Negotiation is delivered or unavailable, a listener concludes
the Negotiation as the system user, after commit and in a new transaction.

The exact predicate matters more than the description. It counts only two Resource states —
delivered, and unavailable — and it does **not** count the not-made-available state or the
unavailable-but-willing-to-collect state, despite both reading like terminal unavailability. Pin the
predicate against every terminal Resource state individually, so ADR 0007's terminal aggregation Guard
is configured against observed behaviour rather than against what the state names suggest.

**Handler firing conditions.** Only the handlers that actually key on lifecycle identity are in scope.
Of the eight notification strategies, five do: the in-progress handler, the submission handler, the
status-change handler, the resource-state-change handler, and the pending-reminder handler, which keys
on the representative-contacted state. The post handler and the updated-resources handler do not key
on state, and the new-negotiation handler keys on a non-lifecycle event. The parent ticket says seven
handlers; five is the accurate count and only those are pinned.

Note while you are here: the conclusion listener carries both an event-listener and a
transactional-event-listener annotation on the same method. Whether that causes double invocation is
an observable question — pin whatever actually happens and report it. Do not fix it.

## Acceptance criteria

- [x] The Negotiation state change event payload is pinned for a real transition: origin state,
      destination state, triggering event, Negotiation.
      `LifecycleStateChangeEventsTest.everyNegotiationTransition_publishesItsOwnStateChange`, all 8.
- [x] The Resource state change event payload is pinned equivalently, including the Resource.
      `LifecycleStateChangeEventsTest.everyResourceTransition_publishesItsOwnStateChange`, all 13,
      plus `overrideProducer_publishesAStateChangeThatTracesNoTransition` for the second producer.
- [x] Reaching in-progress is pinned as initialising the Negotiation's Resources — **but not to the
      initial Resource state**, which is finding 1. `NegotiationSpawnTest`
      `.reachingInProgress_initialisesEveryResourceThatHadNoState` and
      `.spawn_doesNotUseTheGraphsInitialState`.
- [x] Reaching in-progress is pinned as notifying the Resource representatives.
      `NegotiationSpawnTest.reachingInProgress_notifiesTheRepresentatives`, plus
      `.spawn_skipsAResourceThatAlreadyHasAState` for the negative half.
- [x] Driving every Resource to delivered is pinned as concluding the Negotiation.
      `NegotiationConclusionTest.everyResourceDelivered_concludesTheNegotiation`.
- [x] Driving every Resource to unavailable is pinned as concluding the Negotiation.
      `NegotiationConclusionTest.everyResourceUnavailable_concludesTheNegotiation`.
- [x] A mix of delivered and unavailable Resources is pinned as concluding the Negotiation.
      `NegotiationConclusionTest.aMixOfDeliveredAndUnavailable_concludesTheNegotiation`.
- [x] Each remaining terminal Resource state is pinned individually as **not** counting toward
      conclusion, including not-made-available and unavailable-but-willing-to-collect.
      `theConclusionPredicate_walkedOverEveryTransition` (every State a Transition leads to, one
      assertion per arm) and `theConclusionPredicate_walkedOverEveryDeclaredState` (all twelve
      declared States through the override producer, which is the only way to reach the two no
      Transition leads to). `theUnavailableSoundingStates_doNotCount` states the ticket's two by name.
- [x] A Negotiation with any non-terminal Resource is pinned as not concluding.
      `NegotiationConclusionTest.aNonTerminalResource_blocksConclusion`.
- [x] Conclusion is pinned as performed as the system user, not the caller.
      `NegotiationConclusionTest.conclusion_isPerformedAsTheSystemUser` — the Record is Person 0's.
- [x] Each of the five lifecycle-keyed handlers is pinned as firing on its triggering condition and
      not firing otherwise. `LifecycleNotificationHandlerTest`: two whole-graph walks
      (`negotiationHandlers_fireOnTheirDestinationStateAndNowhereElse`,
      `inProgressHandler_firesOnItsDestinationStateAndNowhereElse`),
      `resourceStateChangeHandler_firesOnEveryStateChangeFromEitherProducer`, three reminder tests,
      and `theFiveLifecycleKeyedHandlers_areAllWalked` so a sixth cannot be added unnoticed.
- [x] Handler behaviour is observed through recorded application events, with no dependency on SMTP.
      Each arm asserts the triggering application event was recorded (`@RecordApplicationEvents`,
      read through `StateChangeEvents`) and then reads the handler's effect off the `notification`
      rows through `HandlerNotifications`. Nothing in the suite touches `JavaMailSender`, an SMTP
      port or the email listener. **Read honestly:** the *trigger* is a recorded application event;
      the *effect* is a database row, because a handler's only durable effect is the notification it
      writes and `NewNotificationEvent` is published on the async dispatcher's thread, where
      `@RecordApplicationEvents`' inheritable thread local cannot be relied on.
- [x] Whether the double-annotated conclusion listener runs once or twice is established and reported
      in this issue. **Once.** See finding 5.
- [x] All assertions use Awaitility with a bounded timeout — through `LifecyclePersistence`, with
      `awaitValueAfterSettling` wherever the claim is that nothing happened. No bare sleep, no
      assertion made immediately after a send.
- [x] Every State and Event is named as a string; the forbidden-import guard passes
      (`CharacterizationImportGuardTest`, 3 tests green in the full-gate run).
- [x] No production code is modified. `git diff` against `e2f30a68` touches `src/test` and this
      issue file only.

## Outcome

Full parity gate after this ticket: **255 tests in 24 classes, 0 failures, 0 errors, 1 intentional
skip** (ticket 01's opt-in generator), up from 226 in 20. 29 new tests in four new classes:
`LifecycleStateChangeEventsTest` (5), `NegotiationSpawnTest` (7), `NegotiationConclusionTest` (9),
`LifecycleNotificationHandlerTest` (8). Two new shared helpers, `service/StateChangeEvents` (the
suite's only reader of the two state change events) and `service/HandlerNotifications` (its only
reader of the `notification` table); `LifecycleTestAdapter`, `SeededResourceSubject`,
`SeededNegotiationSubject` and `rest/CanonicalJson` extended.

## The call on the Override producer: pinned, not scoped out

Ticket 04's finding 10 asked for a decision. **It is pinned**, and the adapter grew one method,
`overrideResourceStates`, so the suite can reach it without naming a State enum. Three reasons:

1. **The conclusion predicate cannot be walked completely without it.** The acceptance criterion is
   "pin the predicate against every terminal Resource state individually". Two of the twelve declared
   States have no incoming Transition at all — the initial State, and the Legacy State
   `RETURNED_FOR_RESUBMISSION` — so a Lifecycle-only walk leaves two holes in exactly the table ADR
   0007's aggregation Guard is configured from. The override walk closes them.
2. **A lifecycle-keyed handler does fire on an Override-stamped event.** `ResourceStateChangeHandler`
   notifies the Negotiation's creator identically for a Transition and for an override, and the
   conclusion listener concludes identically. That is behaviour the redesign must preserve or
   deliberately change, and nothing recorded it.
3. **It is live.** It is reachable from `PATCH /negotiations/{id}/resources`, and the existing
   `NegotiationLifecycleServiceImplTest.successfulNegotiation_2finishedResources_closedAutomatically`
   — the only pre-existing test of automatic conclusion — drives it through this path and not through
   the Lifecycle at all.

What is deliberately **not** pinned here: the override's own authorisation rule (admin, or
representative of every named Resource), its behaviour when the Negotiation is in `DRAFT`, and its
`NewResourcesAddedEvent` branch. Those belong to the governance service's own seam, not to the
Lifecycle's, and the assertions above never phrase themselves as "every `ResourceStateChangeEvent`
carries the Transition that caused it".

## Findings

1. **Spawn does not start a Resource in the Definition's initial State.** The graph's initial State
   is `SUBMITTED`; spawn writes `REPRESENTATIVE_CONTACTED` for a Resource that has a representative
   and `REPRESENTATIVE_UNREACHABLE` for one that does not. So "the State a Resource is seeded in" and
   "the State a Resource is spawned in" are two different States, and this ticket's own wording
   ("initialises its Resources to the initial Resource state") and PRD story 13's are both wrong on
   the point. ADR 0007's `SPAWN_RESOURCE_LIFECYCLES` Action must reproduce the two-way choice, not
   the initial State: starting a spawned Resource at `SUBMITTED` would move every representative one
   step backwards and re-offer them `CONTACT`/`MARK_AS_UNREACHABLE`, which are admin-only Events.
2. **Spawn publishes no Resource state change at all.** Three Resources can change State in one spawn
   and not one `ResourceStateChangeEvent` is published, so the Resource-state-change handler does not
   fire, the conclusion listener never runs, and the webhook subsystem never hears of it. Every other
   writer of a Resource State publishes one. If ADR 0007 routes spawn through the ordinary Transition
   machinery it will start emitting events to consumers nobody has counted — including a conclusion
   check that would then run against a Negotiation that has only just started.
3. **Spawn keys on the destination State, not on the Event.** `PAUSED --UNPAUSE--> IN_PROGRESS`
   spawns exactly as `SUBMITTED --APPROVE--> IN_PROGRESS` does. Attaching the Action to `APPROVE`
   would be a behaviour change for every resumed Negotiation. The same is true of the submission and
   status-change handlers: all three notification handlers on the Negotiation side key on the State
   arrived in and none on the Event, so the two `ABANDON` Transitions — which ticket 06 showed are
   *not* equivalent in their post effects — are indistinguishable here.
4. **The conclusion predicate, empirically, over all twelve declared States.** Counts:
   `RESOURCE_MADE_AVAILABLE`, `RESOURCE_UNAVAILABLE`. Does not count: `SUBMITTED`,
   `REPRESENTATIVE_CONTACTED`, `REPRESENTATIVE_UNREACHABLE`, `CHECKING_AVAILABILITY`,
   `RESOURCE_AVAILABLE`, `RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT`, `ACCESS_CONDITIONS_INDICATED`,
   `ACCESS_CONDITIONS_MET`, `RESOURCE_NOT_MADE_AVAILABLE`, `RETURNED_FOR_RESUBMISSION`. The two that
   matter are `RESOURCE_NOT_MADE_AVAILABLE` — where a researcher's own refusal of the access
   conditions lands — and `RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT`. A Negotiation all of whose
   Resources end in either is finished in every practical sense and stays `IN_PROGRESS` for ever.
   Pinned as behaviour, not endorsed; ADR 0007's aggregation Guard is the place to decide.
5. **The double-annotated conclusion listener runs once, not twice.** Established with a throwaway
   Mockito spy on `ResourceStateChangeListener` (invocation count 1, for both the Lifecycle and the
   override producer), and left behind in the suite as the observable consequence:
   `conclusion_happensExactlyOnce` pins one published `CONCLUDE` state change and one added Record
   per terminal Resource change. The mechanism is that `EventListenerMethodProcessor` takes the first
   `EventListenerFactory` that supports a method and stops, so the transactional factory wins and the
   plain `@EventListener` never produces a listener. **The consequence is not harmless, though:** the
   surviving registration is the transactional one, so the method only ever runs after a commit —
   which is what finding 6 is about. Deleting the `@EventListener` annotation would be a no-op today;
   deleting the `@TransactionalEventListener` one would change behaviour.
6. **No handler is reachable by an event published outside a transaction, and the scheduled reminder
   is published outside one.** `NotificationListener.onNewEvent` is the single dispatcher between
   every application event and all eight strategies, and it is `@TransactionalEventListener` with
   default fallback, so an event published with no transaction in progress is dropped silently.
   Every lifecycle event in the system is published from inside a transaction, so this is invisible
   there — but `NotificationScheduler.forPendingNegotiations`, the only production publisher of
   `PendingNegotiationReminderEvent`, is a plain `@Scheduled` method with no transaction. Pinned both
   ways in `noHandlerIsReached_whenTheEventIsPublishedOutsideATransaction`: the same event, published
   the same way, twice, differing only in the transaction. On the evidence here the daily pending
   reminder has never reached a representative in production. Not fixed — reported.
7. **`ResourceStateChangeHandler` is not lifecycle-keyed in its firing condition.** It has no
   condition: it fires on every published Resource state change, from either producer, and notifies
   the Negotiation's creator. Its dependence on lifecycle identity is entirely in its *content* — the
   body is built from the origin and destination States' user-facing labels. The suite computes the
   expected body from the committed `characterization/rest/resource-states.json` rather than
   transcribing labels a second time (`CanonicalJson.publishedLabels`, new). ADR 0009's seed has to
   carry those labels or this notification degrades to `null`s in a sentence.
8. **The pending reminder handler ignores the Negotiation's own State.** It selects Negotiations by
   creation date alone and then looks only at Resource States, so an `ABANDONED` Negotiation whose
   Resource is still marked `REPRESENTATIVE_CONTACTED` goes on reminding its representatives to
   attend to it. Pinned in `pendingReminderHandler_remindsEvenAboutAnAbandonedNegotiation`.
9. **Two notification titles are shared with handlers that are not lifecycle-keyed.** `"New Request"`
   is written by `NegotiationSubmissionHandler` *and* by `NewNegotiationHandler` on creation;
   `"New Negotiation Request"` by `NegotiationInProgressHandler` *and* by `UpdatedResourcesHandler`,
   which calls the same `ResourceNotificationService` when Resources are added to a running
   Negotiation. Within this ticket's tests a title identifies its handler, because nothing here
   creates a Negotiation and every Resource named is already linked; `HandlerNotifications` says so
   in its class comment. A later suite that creates Negotiations must not inherit the assumption.
   Note the second collision means the **override path also spawns**: adding or updating Resources on
   an `IN_PROGRESS` Negotiation publishes `NewResourcesAddedEvent`, which initialises any Resource
   with no State exactly as arriving at `IN_PROGRESS` does.
10. **The override path is silent in exactly one case.** Writing the initial State onto a link row
    that already has a State writes nothing and publishes nothing — the governance service treats it
    as "not a change" (`isUninitialized` requires the previous State to be `null`, and
    `isStateMachineInitialized` excludes the initial State). Every other State, including a rewrite of
    the State already there, writes and publishes. A reimplementation that made the override uniform
    would start publishing an event this path has never published.
11. **The administrators the submission handler notifies are the `admin` column, not `ROLE_ADMIN`.**
    Person 0, the system user the conclusion runs as, has `admin = false` and is therefore *not*
    notified of submissions, while it *is* treated as an administrator by every role-based check
    because `runAsSystemUser` grants itself `ROLE_ADMIN`. The suite reads the expected recipients out
    of the `admin` column rather than naming Person 101, so the assertion states the rule rather than
    the seed. Same shape of split as ticket 04's finding about `isAdmin`.
12. **The corpus facts this ticket added.** `negotiation-5` is the only seeded Negotiation still
    holding Resources with no State (rows 5 and 7, represented by 109 and 105 respectively, created by
    108), which makes it the only usable spawn subject; resource row 10 (`biobank:3:collection:4`) is
    the seed's only Resource with no representative and is linked to no Negotiation. `negotiation-1`
    has exactly one Resource, so any terminal-counting change to it concludes the Negotiation — which
    is why the Resource-side walks in this ticket attach a second Resource in a non-counting State
    purely as a brake.

## Note from ticket 04 — the event seam has a second producer

Read before starting: this ticket's premise, that a `ResourceStateChangeEvent` is the trace of a
Resource Transition, is not true in general.

`ResourceServiceImpl.updateResourceStatus`
(`backend/src/main/java/eu/bbmri_eric/negotiator/governance/resource/ResourceServiceImpl.java:178-194`)
writes an arbitrary State straight onto the link row, bypassing the Lifecycle entirely — no
Transition, no Required Authority rule of the graph, no IN_PROGRESS gate — and then publishes a
`ResourceStateChangeEvent` stamped with the `OVERRIDE` Event. It is reachable from
`PATCH /negotiations/{id}/resources` (`NegotiationController.java:297-304`), gated by
`verifyAuthForStatusUpdate` (admin, or representative of every Resource in the payload).

Nothing in the slab pins that path today. Ticket 04 recorded it as finding 10 and deliberately left
it alone, because its own seam is the Lifecycle service. Two things follow for this ticket:

- when pinning the Resource state change event payload, say which producer the assertion is about,
  and do not phrase it as "every `ResourceStateChangeEvent` carries the Transition that caused it";
- when pinning the five lifecycle-keyed handlers, remember they can be reached by a state change
  that never went through a Transition. Whether that is worth pinning here, or belongs to a later
  slab, is this ticket's call — but it should be a call, not an omission.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
