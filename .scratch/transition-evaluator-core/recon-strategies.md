# Recon — the strategy inventory, and five things the ticket has wrong

Read-only brief for the [Transition Evaluator core](PRD.md) slab, map ticket
[09](../state-machine-implementation/issues/09-transition-evaluator-core.md). No production file was
touched to produce it. Everything below was measured at `5d17d3ca`, the branch tip, and every claim
carries a `file:line` a reader can check.

Ticket 09 says to take the Guard and Action strategies "from ticket 01's graph dump, not from
memory". **That instruction cannot be followed as written**, and the reason is the most important
thing in this brief. The dump is authoritative for Actions, States, Events and authorities. It is
silent on Guards, and the one Guard bean in the codebase has never run.

## 1. What the graph dump carries, and what it does not

`backend/src/test/resources/lifecycle/negotiation-graph-v1.json` — 8 States, 7 Events, 8 Transitions,
initial `SUBMITTED`. `resource-graph-v1.json` — 12 States, 11 Events, 13 Transitions, initial
`SUBMITTED`.

**All 21 Transitions across both graphs record `"guard": null`.** Not one carries a Guard.

Actions appear on exactly three Negotiation Transitions and on no Resource Transition:

| Transition | Action in the dump |
|---|---|
| `DRAFT --SUBMIT--> SUBMITTED` | `EnablePublicPostsAction` |
| `SUBMITTED --APPROVE--> IN_PROGRESS` | `EnablePrivatePostsAction` |
| `IN_PROGRESS --ABANDON--> ABANDONED` | `DisablePostsAction` |

Wired at `NegotiationStateMachineConfig.java:42`, `:49` and `:76`, from the `@Bean` methods at `:85`,
`:90` and `:95`. The other five Negotiation Transitions carry nothing — note in particular that
`PAUSED --ABANDON--> ABANDONED` does **not** disable posts, though `IN_PROGRESS --ABANDON-->` does.

So the dump gives the slab its Action inventory and its wiring positions, and gives it nothing at all
about Guards. The Guard inventory has to be read out of the two lifecycle services instead, which is
what the rest of this brief does.

## 2. Correction 1 — the one Guard bean in the codebase has never run

`negotiation/state_machine/resource/NegotiationIsApprovedGuard.java` is the only
`org.springframework.statemachine.guard.Guard` implementation in `src/main`. It is attached to
nothing. Both Resource configs end with a fragment naming no source, no event and no target:

```java
transitions.withExternal().guard(negotiationIsApproved());
```

`ResourceStateMachineConfig.java:119` and `SimplifiedResourceStateMachineConfig.java:72`. Spring
Statemachine discards it silently. `ResourceStateMachineConfig` contains 14 `withExternal()` calls
and the dump reports 13 Transitions; the fourteenth is this one, and it produces no edge.

This is already pinned in the characterization suite, twice, and the second one is explicit about
what the redesign must do:

- `characterization/dump/LifecycleGraphDumpDriftTest.java:103` —
  *"Finding: NegotiationIsApprovedGuard is attached to no Transition of either graph"*.
- `characterization/service/ResourceGraphV1BindingTest.java:170-212`, whose javadoc reads:
  *"What actually enforces 'the parent Negotiation must be IN_PROGRESS' is the imperative gate in
  `ResourceLifecycleServiceImpl` … **A Guard that has never fired must not be reimplemented in the
  new registry.**"*

`characterization/dump/LifecycleGraphDumperUnwrapTest.java` closes the obvious escape route: it
proves the dumper *would* have named the bean had any Transition carried it, so the absence is a real
absence and not an unwrap failure wearing a disguise.

**What this means for the slab.** The bean is not the thing to port. It reads the Negotiation through
`NegotiationService.findById(...)`, which is I/O, and it has never influenced a single decision the
application has made.

## 3. Correction 2 — `NEGOTIATION_APPROVED` still has live behaviour, somewhere else

ADR 0005 says `NEGOTIATION_APPROVED` "becomes one definition-level Guard entry per Resource-scope
definition, matching what it always meant". That is right about the *rule*, and the rule is live —
just not in the Guard.

It is imperative, in the service, at `ResourceLifecycleServiceImpl.java:142`:

```java
if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS.name())) {
  return Set.of();
}
```

Pinned by `ResourcePossibleEventsAuthorityTest.possibleEvents_areGatedOnTheParentBeingInProgress`,
which walks it across every State the Negotiation definition declares, from both sides.

The two spellings agree on the predicate — parent must be `IN_PROGRESS` — and differ in three ways
that matter:

1. The imperative gate runs on **both** `getPossibleEvents` and `sendEvent`; the dead bean would have
   run only when an Event fired.
2. The imperative gate runs **before** any Transition or authority rule is consulted, and
   short-circuits to an empty set. ADR 0005's pipeline puts Guards **third**, after Required
   Authority and the requirement check.
3. The imperative gate yields an absence — an empty Possible Events set — where a Guard yields a
   refusal with a category.

**So the slab ports the gate, not the class.** That satisfies ADR 0005 and the characterization
finding simultaneously, and it is the only reading under which both are true. Points 2 and 3 are
recorded as divergences D1 below, and they belong to the cutover slab because nothing here is wired.

## 4. Correction 3 — `SET_POST_VISIBILITY` is not a three-into-three collapse

ADR 0002 names this as its worked example for why Actions get params: three classes become "one
`SET_POST_VISIBILITY` type with a scope and a flag". The three bodies, in full, are the whole
difference between them:

| Class | Body |
|---|---|
| `EnablePublicPostsAction.java` | `setPublicPostsEnabled(id, true)` |
| `EnablePrivatePostsAction.java` | `setPrivatePostsEnabled(id, true)` |
| `DisablePostsAction.java` | `setPublicPostsEnabled(id, false)` **and** `setPrivatePostsEnabled(id, false)` |

All three are `@Component public class … implements Action<String, String>`, all three depend on
exactly `NegotiationService` field-injected `@Autowired @Lazy`, and all three pull the id out of the
message headers by the literal `"negotiationId"`.

`DisablePostsAction` touches **both** flags. So a scope of `PUBLIC | PRIVATE` needs two wiring rows
to reproduce the `ABANDON` Transition, and a reader comparing the new seed against the dump would
find three Actions becoming four rows with no explanation. A scope of `PUBLIC | PRIVATE | BOTH` is
one row per dump Action, and the mapping stays legible.

Minor, but worth recording because it is the kind of thing a seed review catches late: there are
**two instances of each Action class in the Spring context** — the `@Component` one and the `@Bean`
one at `NegotiationStateMachineConfig.java:85-97`. The Transitions use the `@Bean` instances.

## 5. Correction 4 — today's Information Requirement check is not requirement-scoped

`ResourceLifecycleServiceImpl.java:109-113`:

```java
if (requirementRepository.existsByForEvent(event)
    && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
        resourceId, negotiationId)) {
  throw new StateMachineException("The requirement for this operation was not met. …");
}
```

Read the two halves together. The first asks whether *any* Information Requirement exists for this
Event name, anywhere, for anyone. The second asks whether *any* submission exists for this
resource-and-negotiation pair — **not for that requirement, and not by anyone in particular.** A
submission against a completely different form satisfies the check.

ADR 0005 wants a reason code and the missing forms in the details; ADR 0006 wants distinct qualifying
members of a resolved Audience counted against a Quantifier. Both are behaviour changes against this,
and both are already owned by the later IR-satisfaction slab. **The slab builds the port's shape and
nothing behind it.**

`before-picture-findings.md` finding 3 already asks whether the check becomes requirement-scoped, so
this is a confirmation rather than a discovery. It is restated here because the ticket asks for "port
shape only" without saying what today's shape actually is, and the answer is "much weaker than the
name suggests".

## 6. Correction 5 — a blocked Resource Event is a silent no-op today

`ResourceLifecycleServiceImpl.java:115-117`:

```java
if (!getPossibleEvents(negotiationId, resourceId).contains(event)) {
  return getCurrentStateForResource(negotiationId, resourceId);
}
```

No exception, no status, no message. Firing an Event that is not permitted returns the current State,
indistinguishable from firing one that was permitted and led back to the same place. The only
refusal that throws is the requirement one above, and it throws
`org.springframework.statemachine.StateMachineException` — a type the redesign deletes, whose
replacement is still unnamed (`before-picture-findings.md` Part 7).

The Evaluation Pipeline's monotonic categories — authorization, unmet requirement, domain-state
conflict, shaped as 403, 422 and 409 — are therefore a change against this, for every refusal except
the requirement one. Divergence D4.

## 7. The strategy inventory, as the slab should build it

Four type keys. Two are Guards, two are Actions, and the sources are three different places.

### `NEGOTIATION_APPROVED` — Guard, definition-level, no params

Ported from the imperative gate at `ResourceLifecycleServiceImpl.java:142`, per §3. Reads the parent
Negotiation's current State off the evaluation context. **Needs no port and does no I/O**, which is
the whole reason it is worth putting the parent State on the context rather than behind a lookup.

One definition-level entry — a null `transition_id` — per Resource-scope definition. Definition-level
wiring is what removes the copy-drift risk of re-attaching it to every Transition a later version
adds (ADR 0005). Hardcoding it into the evaluator was rejected in that ADR, because a Network may
legitimately want Resource work to begin before approval.

### `TERMINAL_AGGREGATION` — Guard, no params

Today's predicate lives in `negotiation/state_machine/negotiation/ResourceStateChangeListener.java:46`:

```java
if (resources.stream().allMatch(resource -> isDelivered(resource) || isUnavailable(resource))) {
```

with `isUnavailable` at `:56-58` and `isDelivered` at `:60-63` comparing against
`RESOURCE_UNAVAILABLE` and `RESOURCE_MADE_AVAILABLE` respectively.

**Exactly two of twelve declared Resource States count, hardcoded.**
`before-picture-findings.md` finding 6 walked it twice — once over every State a Transition leads to,
once over all twelve through the override producer — and the two that matter are the ones that do
*not* count: `RESOURCE_NOT_MADE_AVAILABLE`, where a researcher's own refusal of the access conditions
lands, and `RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT`. A Negotiation all of whose Resources end in
either is finished in every practical sense and stays `IN_PROGRESS` for ever. That is pinned as
behaviour, not endorsed.

ADR 0007 replaces the hardcoded pair with a structural question: the Guard asks **each Resource's own
pinned Definition Version** whether its current State carries the `terminal` flag, because Resources
in one Negotiation may run different definitions and so terminal cannot be a list.

**The slab builds the mechanism and decides nothing about the set.** Which States carry the flag is
seed content, owned by the migration slab and blocked by map ticket
[12](../state-machine-implementation/issues/12-adr-0009-seed-specified-against-wrong-picture.md).
`state.terminal` has no per-definition constraint in `V36.1` — "any number of States may carry it" —
so the schema is indifferent. Behaviour-preserving means the v1 Resource seed flags exactly
`RESOURCE_MADE_AVAILABLE` and `RESOURCE_UNAVAILABLE`; widening to four is a behaviour change.
Divergence D3.

### `SET_POST_VISIBILITY` — Action, params `{scope, enabled}`

Per §4. Scope is `PUBLIC | PRIVATE | BOTH`. Three configured instances reproduce the three dump
Actions one-for-one.

### `SPAWN_RESOURCE_LIFECYCLES` — Action, registration only

**Not in the state machine at all today.** It is a notification handler:
`notification/internal/NegotiationInProgressHandler.java` fires on
`WellKnownNegotiationStates.IN_PROGRESS.equals(event.getToState())`, and the spawn itself is
`notification/internal/ResourceNotificationService.java:80-100`, which re-checks `IN_PROGRESS`, skips
any Resource that already has a State, and then assigns `REPRESENTATIVE_UNREACHABLE` or
`REPRESENTATIVE_CONTACTED` depending on whether the Resource has representatives.

The ticket is explicit that only the type key belongs here, "since it writes". Two obligations the
map already parked on the coupling slab and that this slab must not pre-empt: the relocated Action
must publish `ResourceLifecyclesSpawnedEvent` and must **not** publish `ResourceStateChangeEvent`.

## 8. The precedent's actual shape, and one correction to how the ticket describes it

ADR 0002 and ticket 09 both say the registries fold "exactly as the existing `WebhookEventMapper`
does". Read `webhook/event/WebhookEventMapper.java:57-78`. The mechanism to copy is:

- a **private static** fold, so the registry is testable through the constructor with no Spring;
- `putIfAbsent`, and an `IllegalStateException` thrown **from the constructor** — which is what makes
  a duplicate fail the boot, as a bean creation failure;
- a message naming **the key and both colliding implementation class names**;
- `Map.copyOf` to freeze the registry after startup.

And `mapWithStrategy` at `:52-55` is the "one unchecked bridge, in one place" pattern already in this
codebase — a private generic bridge method using `Class.cast` rather than a `@SuppressWarnings`.
That is the idiom the params deserialization should follow.

**The correction: that registry is keyed on `Class<? extends ApplicationEvent>`, not on a string.**
A grep for a string-keyed strategy registry across `src/main` and `src/test` returns nothing — none
exists anywhere in this backend. The *mechanism* is the precedent; the string type key is new. Worth
saying out loud, because "follow `WebhookEventMapper`" reads as though a keyed-by-string example were
sitting there to copy, and it is not.

Two related precedents worth reading before choosing shapes:

- `webhook/event/DefaultWebhookMappingStrategy.java` — one `final` parameterized strategy with a
  private constructor and a static `of(...)` returning the interface, configured N times as `@Bean`
  methods in `WebhookMappingStrategyConfiguration.java`. This is the direct model for
  `SET_POST_VISIBILITY`: one class, three configured instances.
- `webhook/WebhookOpenApiDocumentationFactory.java:41-85` — a **second** fold over the same injected
  bean list, with a *different* collision rule: conflict only when the values disagree. Evidence that
  folding the same strategy list twice for two purposes is established practice here.
- `notification/internal/NotificationListener.java` — folds into a multimap with **no** duplicate-key
  failure, because multiple handlers per event are legal there. The counter-example that makes the
  Guard registry's strictness a decision rather than a default.

## 9. What slab 08 handed over, and the one thing standing in the way

Six tables, `V36.0`–`V36.4`; `V36.5` is the next free number and this slab needs none.

The column facts that shape the compiled graph:

- `guard_wiring.transition_id` is **nullable** — null means definition-level. `action_wiring.transition_id`
  is `NOT NULL`; Actions are never definition-scoped.
- Both `params` columns are `JSONB` and **nullable**; both map to a Java `String` with
  `@JdbcTypeCode(SqlTypes.JSON)`. There is no typed-params deserialization anywhere in the codebase
  yet.
- The ordering column is `sort_order`, not `order`. Two partial unique indexes keep it unique within
  each Guard scope independently.
- **No uniqueness on `type_key`.** A definition may wire the same type key twice at different sort
  orders, and the compiled graph must preserve that.
- `state.terminal` has no per-definition constraint; `state.initial` has a partial unique index.
- `idx_transition_source_event ON transition (from_state_id, event_id)` — the migration's own comment
  calls this "the index the evaluator will live on".
- `Transition.lifecycleDefinition` is stored rather than derived through `fromState`, and the entity
  javadoc says why: "because the evaluator loads a whole graph by its Definition Version id".
- The pins are deliberately plain `Long` ids — `Negotiation.java:121` and
  `NegotiationResourceLink.java:41` — specifically so the inertness guard stays green.
- **No repository has a "load the whole graph by definition id" query.** Nothing in slab 08 loads a
  graph, so the slab that first does is the one that writes it.

**The thing standing in the way.** Every one of the 17 types in
`eu.bbmri_eric.negotiator.lifecycle.definition` is package-private, and
`DefinitionInertnessGuardTest` fails the build the moment any file under `src/main/java` outside that
directory mentions the package, one of 14 distinctive type names, or one of the definition tables.
Two of those 14 names are **`DefinitionScope`** and **`RequiredAuthority`** — vocabulary the evaluator
cannot express itself without.

`State`, `Event` and `Transition` are already exempt from the bare form of the rule
(`NAMES_TOO_COMMON_TO_FORBID_BARE`), because `org.springframework.statemachine` takes the first two
names and `EventListener` takes the third; their tables are caught by a narrower SQL-clause pattern
instead. So the precedent for "this name is vocabulary rather than a reference" is already in the
guard, and the amendment the slab needs is the same judgement applied to two more names.

The guard's own javadoc names the exit — *"Reading the definition tables is a later slab's work,
behind `DefinitionResolver`. If that slab has started, this guard is what it deletes first,
deliberately."* — and **this slab is not that slab.** It reads no table, so the guard survives with
one amendment rather than a deletion. `guard_forbidsOnlyNamesThatStillExist` is what makes an
amendment safe to make by editing the list rather than by adding an exemption: a name that stops
existing fails the guard instead of passing quietly.

## 10. Divergences, with owners

None of these is this slab's to resolve, and none can break parity here, because nothing this slab
builds is called from production code.

| # | Divergence | Established by | Owner |
|---|---|---|---|
| D1 | The parent-`IN_PROGRESS` gate runs before authority today and yields an empty set; as a definition-level Guard it runs third and yields a category | §3 | cutover slab |
| D2 | The requirement check is not requirement-scoped, not Audience-aware and not Quantifier-counted | §5 | IR-satisfaction slab |
| D3 | `terminal` counts 2 States today; deriving it structurally invites 4 | §7 | migration slab, via ticket 12 |
| D4 | A blocked Event is a silent no-op returning the current State, not a refusal | §6 | cutover slab |
| D5 | Six of eight Negotiation Transitions are behaviourally `IS_ADMIN OR IS_CREATOR`, which `RequiredAuthority` cannot express | slab 08, ticket 11 | ticket 11 |
| D6 | What replaces `StateMachineException` is unnamed | §6 | cutover slab |

D5 needs one sentence of guidance rather than a decision: slab 08 built the column single-valued as
ADR 0002 specifies, after deciding it with the developer, so **the evaluator must not invent a
disjunction to paper over it.** A `Set<RequiredAuthority>` on the Transition, or an `IS_ADMIN_OR_CREATOR`
value, would resolve ticket 11 by accident and in the wrong place.
