# PRD — Transition Evaluator core

Status: ready-for-agent

Slab for map ticket
[09 Transition Evaluator core](../state-machine-implementation/issues/09-transition-evaluator-core.md)
of [State machine implementation](../state-machine-implementation/map.md). Branch:
`feat/state-machine-implementation`, already rebased on `master`.

**The recon for this slab is [recon-strategies.md](recon-strategies.md).** It carries the strategy
inventory with `file:line` citations, the five places ticket 09 describes the code wrongly, and the
six divergences from today's behaviour with their owners. Read it rather than re-deriving; every
number and citation below comes from there and was measured at `5d17d3ca`.

Four decisions were settled with the developer before this PRD was written and are not reopened here:
the evaluator gets its own package and two vocabulary enums move up to reach it; the entity-to-graph
loader is deferred behind a port; the ported strategies get real bodies; and the slab opens with one
recon brief. Each is argued in Implementation Decisions below.

## Problem Statement

The definition schema exists and is inert. Six tables hold States, Events, Transitions and the Guard
and Action Wiring of a Definition Version, and `DefinitionInertnessGuardTest` proves no production
code reads any of it. Forty-two consumers have been moved off the four Lifecycle enums. What has not
been built is the thing all of that was clearing the way for: **nothing yet can answer what a
Definition Version permits.**

Until something can, every remaining piece of the redesign is blocked on the same missing component.
The cutover cannot replace `NegotiationStateMachineConfig` and `ResourceStateMachineConfig`, because
there is nothing to replace them with. Lifecycle coupling cannot re-attempt a Negotiation's System
Events, because nothing can be asked whether one would fire. The Information Requirement check cannot
move out of `ResourceLifecycleServiceImpl.sendEvent()` into a pipeline that does not exist. The
migration cannot seed a v1 Definition Family with confidence, because no code has ever read a
definition graph and so no seed has ever been exercised.

The danger in building it is not difficulty, it is **scope collapse**. The evaluator sits at the
centre of a subsystem that also commits moves, runs Actions, writes Lifecycle Records, notifies
Audiences, resolves definitions and loads graphs. Every one of those is I/O, and every one of them is
one keystroke away from an evaluator that happens to hold a repository. ADR 0001 is explicit that the
no-I/O boundary is "a deliberate constraint rather than a performance claim: an evaluator that
structurally cannot query the database makes loading the definition graph an explicit, testable
step", and warns what the alternative costs — "an N+1 across a negotiation's resources, discovered
only under load."

An evaluator built with a repository in reach would still pass its tests. It would fail two years
later, in production, in a way no test in this repository is shaped to catch.

## Solution

Build the Transition Evaluator, the Guard and Action strategy registries, and the compiled graph and
its cache — **and nothing that writes, and nothing that reads.**

The evaluator is handed an already-materialized definition graph, a current State name, an Event, and
an evaluation context carrying the caller's authorities and the domain facts a Guard may read. It
answers whether the Event may fire and where it leads, or which Events could fire at all. It commits
nothing, runs nothing, and loads nothing.

**Statelessness is built as a structure, not a convention.** The evaluator lives in its own package,
its constructor takes no repository and no `EntityManager`, and the slab closes with a gate that
fails the build if that ever stops being true — including the case an identifier scan cannot see, a
persistence type reached through a method call. The slab gate ticket 09 states as a sentence becomes
a test, as every slab on this map has done with its own.

**The graph loader is deferred, and that is the design rather than a shortcut.** The compiled graph
cache takes a `CompiledDefinitionSource` port; in this slab it has a test double and graphs come from
a fixture builder. Building the repository-backed compiler here would make this the first slab to
read the definition tables, which obliges it to delete `DefinitionInertnessGuardTest` a whole slab
early and forfeits the "pure unit tests, no I/O, no database" gate that makes the slab reviewable at
all. The port is the same pattern ticket 09 already mandates for the requirement lookup, applied
consistently.

**One evaluation path serves the real gate and the listing**, so the two can never disagree — ADR
0005's central point, and the fix for a live UX bug where an Event is listed, clicked, and refused.

Nothing observable changes. No production code calls the evaluator when this slab closes; the parity
suite is green because it has nothing new to see. What closes the slab is a gate proving the
evaluator is pure and unreached, and what it hands the cutover slab is a component whose behaviour is
already pinned by unit tests it does not have to write.

## User Stories

1. As the developer of the cutover slab, I want a Transition Evaluator whose behaviour is already pinned by unit tests, so that replacing Spring Statemachine is a wiring change rather than a wiring change plus a new engine.
2. As the developer of the cutover slab, I want one evaluation path behind both `sendEvent` and Possible Events, so that I delete today's double enforcement instead of reproducing it.
3. As the developer of the cutover slab, I want the evaluator's constructor to take no repository, so that I cannot accidentally satisfy a compile error by handing it one.
4. As the developer of the cutover slab, I want a named `CompiledDefinitionSource` port waiting for me, so that writing the entity-to-graph compiler is an interface to implement rather than a shape to invent.
5. As the developer of the cutover slab, I want `DefinitionInertnessGuardTest` still alive when I arrive, so that deleting it is a visible line in my diff and marks the moment the tables became read.
6. As the developer of the cutover slab, I want the failure categories already modelled and ordered, so that mapping them to 403, 422 and 409 is a translation at the controller and not a redesign.
7. As the developer of the cutover slab, I want the four divergences from today's behaviour recorded with their owners, so that a red characterization test tells me which one I just hit instead of sending me to read the graph dump again.
8. As the developer of the cutover slab, I want to know that a blocked Event is a silent no-op today, so that introducing a refusal is a decision I make deliberately rather than a regression I discover.
9. As the developer of the coupling slab, I want `SPAWN_RESOURCE_LIFECYCLES` already registered as a type key, so that I relocate a body into a slot that exists.
10. As the developer of the coupling slab, I want `TERMINAL_AGGREGATION` to already ask each Resource's own pinned Definition Version whether its State is terminal, so that Resources running different definitions are handled by construction rather than by a later fix.
11. As the developer of the coupling slab, I want `SYSTEM` authority to already fail for every human caller, so that a System Event's invisibility in Possible Events is a property I inherit rather than one I add.
12. As the developer of the Information Requirements slab, I want the built-in stage's port and result shape already fixed, so that I implement satisfaction behind a stable seam.
13. As the developer of the Information Requirements slab, I want the stage to be structurally unomittable, so that no Wiring row I forget can reintroduce the dead-click bug.
14. As the developer of the Information Requirements slab, I want today's check recorded as not requirement-scoped, so that I know I am fixing a real weakness rather than reproducing a rule.
15. As the developer of the migration slab, I want `TERMINAL_AGGREGATION` to decide nothing about which States are terminal, so that the seed owns that question and my behaviour-preserving choice stays mine.
16. As the developer of the migration slab, I want the compiled graph to have been exercised against fixture graphs shaped like both v1 families, so that the seed's shape has been read by code before it is written.
17. As the developer of the migration slab, I want this slab to add no Flyway migration, so that `V36.5` is still free and landing it carries no schema risk.
18. As an administrator, eventually, I want which Guards apply to be data rather than code, so that a Network can let Resource work begin before approval without a code change.
19. As an administrator, eventually, I want one `SET_POST_VISIBILITY` type with parameters instead of three Action classes, so that changing which Transition opens which posts is a row rather than a deployment.
20. As an administrator, eventually, I want a Guard I wire at Definition Version level to apply to every Transition including ones added later, so that a new Transition cannot silently escape a rule.
21. As an administrator, eventually, I want the requirement check to be impossible to unwire, so that I cannot misconfigure my way into an Event that gates nothing.
22. As an administrator, eventually, I want two Wiring rows naming the same type key at different sort orders to both take effect, so that the same Guard can be applied twice with different parameters.
23. As a requester, eventually, I want an Event I can see to be an Event I can actually fire, so that clicking it does not fail on an unfilled form.
24. As a requester, eventually, I want a refusal to tell me which forms are missing, so that I know what to do rather than only that something went wrong.
25. As a Resource representative, eventually, I want the parent Negotiation's approval to gate my Resource's Events through the same pipeline as everything else, so that the reason I am offered nothing is legible rather than an empty list.
26. As an operator, I want this slab to change no observable behaviour, so that landing it needs no coordination and no announcement.
27. As an operator, I want the Spring context to still build, so that a strategy registry folded at startup is proven by every integration test rather than only by unit tests.
28. As an operator, I want a duplicate strategy type key to fail the boot loudly and name both colliding classes, so that a misconfiguration is a startup failure rather than a silent last-one-wins.
29. As a reviewer, I want the slab's no-I/O claim to be a test rather than a sentence, so that "pure" is a checked fact I do not have to verify by reading.
30. As a reviewer, I want that test to also catch a persistence type reached through a method call, so that the gate cannot go green over an evaluator that imports nothing and still queries.
31. As a reviewer, I want the gate to have an anti-vacuity test, so that a scan root resolving wrongly fails rather than reporting success over nothing.
32. As a reviewer, I want a test proving `mayFire` and Possible Events cannot disagree, so that ADR 0005's central claim is demonstrated rather than asserted.
33. As a reviewer, I want the dead Guard bean read and deliberately not ported, with the reason recorded, so that a later reader does not restore it as an apparent omission.
34. As a reviewer, I want the parity suite green and unchanged at 255 tests after every slice, so that behaviour neutrality is demonstrated ten times rather than claimed once.
35. As a maintainer, I want the two vocabulary enums to move once, publicly, with the reason in the source, so that the next component needing them does not repeat the argument.
36. As a maintainer, I want the jsonb-to-typed-params bridge in exactly one place, so that there is one unchecked cast in the subsystem and it has a test.
37. As a maintainer, I want the compiled graph keyed on the Definition Version row id alone, so that no composite lookup can grow anywhere later.
38. As a maintainer, I want the evaluator to know nothing about HTTP, so that the pipeline stays reusable by the Orchestration Trigger, which has no request.

## Implementation Decisions

### The evaluator gets its own package, and two enums move up to reach it

The evaluator lives in a new **`eu.bbmri_eric.negotiator.lifecycle.evaluation`**. Every type in the
sibling `lifecycle.definition` stays package-private, so the evaluator can neither name an entity nor
reach a repository — the structural half of ADR 0001's constraint, obtained for free from a package
boundary that already exists.

That boundary costs one thing. `DefinitionInertnessGuardTest`'s type rule forbids fourteen distinctive
names outside the definition package, and two of them — **`DefinitionScope`** and
**`RequiredAuthority`** — are vocabulary the evaluator cannot express itself without. Both move to
`eu.bbmri_eric.negotiator.lifecycle`, `public`, beside the three `WellKnown*` holders, and come off
the guard's list with the reason stated in the source: they are vocabulary, not schema, and they carry
no persistence behaviour beyond `@Enumerated(STRING)` on a column that is not moving.

The precedent for that judgement is already inside the guard. `State`, `Event` and `Transition` are
exempt from the bare form of the same rule because their names are taken by
`org.springframework.statemachine` and by `EventListener`; their tables are caught by a narrower
SQL-clause pattern instead. This is the same distinction applied to two more names, and
`guard_forbidsOnlyNamesThatStillExist` is what makes it safe to do by editing the list rather than by
adding an exemption — a forbidden name whose file disappears fails the guard instead of passing
quietly.

**Two alternatives were considered and rejected.** Putting the evaluator *inside*
`lifecycle.definition` needs no amendment at all, since every inertness rule only scans outside the
package — but it puts the evaluator in the same package as six repositories, and "structurally cannot
query the database" degrades to a convention in the exact component whose whole value is that it is
not one. Declaring private copies of the two enums in the evaluation package trips the same type rule
anyway, since it matches the bare name regardless of which package declares it, and leaves two copies
of a closed vocabulary to keep in step.

Nothing else in `lifecycle.definition` is widened. The amended javadoc says so.

### The compiled graph is a projection, not the entities

`CompiledDefinition` and its parts are immutable records in the evaluation package, carrying the
version's **row id alone** as identity. No `familyKey` and no `version` appear anywhere in the graph
or in the cache — ADR 0003 makes the row id the sole machine identity, and there is no composite
lookup to be had.

The graph answers exactly what the pipeline asks: the Transition for a `(fromState, event)` pair, the
Transitions leaving a State, the initial State, whether a State name is terminal, and the Guard
entries applying to a Transition **already ordered definition-level-first, each scope by its own
`sortOrder`**. Ordering belongs to the graph rather than to the pipeline because ADR 0002 is explicit
that the `sort_order` columns sequence within a scope while "the order *between* scopes is pipeline
logic, not a column" — putting the composition in the compiled graph is what stops the pipeline from
re-deriving it on every call.

A State arrives as a **name**, never an id. ADR 0009 fixes the natural key as the Definition Version
Pin plus the state name, precisely because a string plus a pin cannot drift the way an FK to
`state.id` could. Legacy States must resolve as positions even though no Transition targets one, and
the graph must not assume every State is reachable.

Two facts from the schema shape the records and are easy to get wrong. The same `type_key` may appear
twice in one definition at different sort orders, since no uniqueness constraint forbids it — so the
Guard chain is a list, not a map. And `state.terminal` has no per-definition constraint, so any number
of States may carry it.

### The evaluator's answer includes the Action chain

A permitted outcome carries the target State **and the ordered, params-bound Action chain** the
committing service must run afterwards. The evaluator runs nothing: Actions run only after a commit
(ADR 0002) and this slab commits nothing.

Reporting the chain rather than staying silent about it is what puts Action ordering and Action params
binding under the primary test seam. The alternative leaves the cutover slab to look the chain up off
the graph itself, with neither ordering nor binding exercised anywhere in this slab.

### The pipeline, and where HTTP is not

ADR 0005's order, short-circuiting at the first failure: **Required Authority, then the requirement
check, then Guards** — definition-level entries before Transition entries, each in configured order.
The order is by expected failure likelihood, and it keeps failure categories monotonic:
authorization, then unmet requirement, then domain-state conflict.

A refusal carries a category and a reason code. **The evaluator does not know about HTTP.** The
403/422/409 mapping belongs to the controller, and recording it here rather than applying it is what
keeps the pipeline reusable by the Orchestration Trigger, which has no request to answer.

`SYSTEM` authority fails for every human caller, so a System Event never appears in Possible Events
and can never be fired over REST (ADR 0007). That falls out of authority being checked first, and is
covered rather than arranged.

`RequiredAuthority` stays single-valued. Slab 08 built it that way after deciding it with the
developer, and six of the eight Negotiation Transitions are behaviourally `IS_ADMIN OR IS_CREATOR`
with no value that reproduces it. **The evaluator must not invent a disjunction** — a
`Set<RequiredAuthority>`, or an `IS_ADMIN_OR_CREATOR` value, would resolve map ticket 11 by accident
and in the wrong place.

### The requirement check is a Built-in Stage, and the satisfaction lookup is a port

No Wiring row exists for it anywhere, so no admin can omit it and no newly added Transition can miss
it. It is deliberately **not** a registry Guard type: a wireable Guard would have to be attached to
each Transition by hand, and one forgotten row silently reintroduces the dead-click bug ADR 0005
exists to remove. It still speaks the Guard contract, emitting a result with a reason code and the
missing forms in its details, so callers see one uniform list of failures.

Satisfaction reaches it through an injected port with a test double. Audience resolution and
Quantifier counting are ADR 0006's and a later slab's; **port shape only** here. Recon §5 records
what today's check actually does — it passes if any submission exists for the resource-and-negotiation
pair, whatever form it was — so the eventual implementation is a real strengthening rather than a
reproduction.

### The graph loader is deferred behind a port

`CompiledDefinitionCache` is keyed on `Long` and takes a `CompiledDefinitionSource`. Invalidation
happens only on publish, which is safe because a version is immutable once active, and there is
nothing else to invalidate on.

The port's only implementation in this slab is a refusing one, whose javadoc names the
repository-backed compiler that replaces it at cutover. That is what keeps the Spring context
buildable while the registries genuinely fold at startup, and it follows the
`EnumBackedLifecycleCatalog` precedent from slab 07: a class named for its own deletion, placed where
the thing it stands in for will eventually live.

Building the real compiler here was rejected for two reasons rather than one. It makes this the first
slab to read the definition tables, which per the map obliges it to delete
`DefinitionInertnessGuardTest`; and it forfeits the "pure unit tests, no I/O, no database" gate, which
is the only reason this slab can be reviewed on its own.

### The registries follow the mechanism of `WebhookEventMapper`, not its key type

A constructor-injected `List<Strategy>`, a **private static** fold, `putIfAbsent`, an
`IllegalStateException` thrown **from the constructor** naming the key and both colliding class names,
and `Map.copyOf` to freeze. Throwing from the constructor is what makes a duplicate fail the boot, as
a bean creation failure.

Each strategy is self-describing: it declares its own string type key and its own params type.

**One correction to how ticket 09 and ADR 0002 describe the precedent.** `WebhookEventMapper`'s
registry is keyed on `Class<? extends ApplicationEvent>`, and no string-keyed strategy registry exists
anywhere in this backend. The mechanism is the precedent; the string type key is new. Worth stating,
because "exactly as `WebhookEventMapper` does" reads as though a keyed-by-string example were sitting
there to copy.

The strictness is a decision, not a default: `NotificationListener` folds the same shape into a
multimap with no duplicate-key failure, because multiple handlers per event are legal there. Guards
and Actions are not.

### jsonb params bind once, in one place

`typeKey` plus the raw `params` string off the Wiring row resolve to the strategy's declared params
type at load time — one unchecked bridge, following the `Class.cast` idiom of
`WebhookEventMapper.mapWithStrategy` rather than a `@SuppressWarnings`. Null params are legal for a
strategy that takes none. Unknown type keys, malformed JSON and params that do not fit the declared
type are refusals at binding, not at evaluation.

The compiled graph carries **already-bound** typed params. That is why binding belongs to this slab
even though loading does not: binding is registry logic, and it is what makes the graph a projection
rather than a row-shaped copy.

Runtime domain state never travels in `params`. The negotiation and the acting person reach a strategy
through the evaluation context at fire time (ADR 0002).

### The four strategies, and how faithful each is

Taken from recon §7, which reads them out of the two lifecycle services because the graph dump does
not carry Guards at all.

- **`NEGOTIATION_APPROVED`** — Guard, definition-level, no params. Ported from the **imperative gate**
  in `ResourceLifecycleServiceImpl`, not from `NegotiationIsApprovedGuard`. That bean is attached to
  nothing, has never run, and the characterization suite says outright that a Guard which has never
  fired must not be reimplemented. The rule it names is live; the class is not. Reads the parent
  Negotiation's State off the context, so it needs no port and does no I/O — which is the reason the
  parent State is on the context rather than behind a lookup.
- **`TERMINAL_AGGREGATION`** — Guard, no params. Asks each Resource's own pinned Definition Version
  whether its current State carries the terminal flag, because Resources in one Negotiation may run
  different definitions and terminal therefore cannot be a list. **The slab builds the mechanism and
  decides nothing about the set**; which States are terminal is seed content owned by the migration
  slab and blocked by map ticket 12.
- **`SET_POST_VISIBILITY`** — Action, params `{scope, enabled}` with a scope of `PUBLIC | PRIVATE |
  BOTH`. The third value is not decoration: `DisablePostsAction` sets both flags, so without it the
  `ABANDON` Transition needs two rows and three dump Actions become four. One class, three configured
  instances, following `DefaultWebhookMappingStrategy`'s static-factory shape.
- **`SPAWN_RESOURCE_LIFECYCLES`** — Action, type key and params only; the body throws and its javadoc
  names the coupling slab. Today's spawn is not in the state machine at all but in
  `ResourceNotificationService`, and the map has already parked two obligations on the coupling slab
  that this slab must not pre-empt: the relocated Action publishes `ResourceLifecyclesSpawnedEvent`
  and must not publish `ResourceStateChangeEvent`.

### Slice order

| # | Slice | Blocked by |
|---|---|---|
| 01 | [The vocabulary move and the amended inertness gate](issues/01-vocabulary-move-and-inertness-gate.md) | — |
| 02 | [The thinnest evaluation, end to end](issues/02-thinnest-evaluation-end-to-end.md) | 01 |
| 03 | [Guards enter the pipeline](issues/03-guards-enter-the-pipeline.md) | 02 |
| 04 | [Params bind at load time](issues/04-params-bind-at-load-time.md) | 03 |
| 05 | [The Information Requirement Built-in Stage](issues/05-information-requirement-built-in-stage.md) | 03 |
| 06 | [The Action chain in the outcome](issues/06-action-chain-in-the-outcome.md) | 04 |
| 07 | [The compiled graph cache](issues/07-compiled-graph-cache.md) | 02 |
| 08 | [NEGOTIATION_APPROVED and SET_POST_VISIBILITY](issues/08-negotiation-approved-and-post-visibility.md) | 04, 06 |
| 09 | [TERMINAL_AGGREGATION and SPAWN_RESOURCE_LIFECYCLES](issues/09-terminal-aggregation-and-spawn.md) | 07, 08 |
| 10 | [The evaluator purity gate](issues/10-evaluator-purity-gate.md) | 01–09 |

**This sequences as tracer bullets, and slice 02 is the bullet.** It lands a minimal graph, the
evaluator, the outcome types and Required Authority as the only pipeline stage — enough to answer
`mayFire` and Possible Events over a one-Transition graph on the day it lands. Slices 03 to 06 each
**widen that one working path**: 03 adds the Guard stage and its registry, 04 makes params bind, 05
inserts the requirement stage, 06 extends the permitted outcome with the Action chain. Slices 08 and
09 then fill the widened shape with the ported strategies.

The practical payoff is that the primary test seam exists from slice 02, so the decision to test
Guards through the evaluator rather than directly is available to every later slice instead of
arriving at the end. An earlier draft of this table delivered the graph, the registries, the bridge
and the cache as four separate slices before anything used them; that is a horizontal slab of one
layer, verifiable only by reading it, and it was replaced.

**Slice 07 is the one orthogonal slice.** ADR 0001 hands the evaluator a graph, so the cache sits
outside it and needs nothing but slice 02's graph type. It is the natural parallel track.

Two pairs may be authored in parallel — 05 with 06, and 07 with anything after 02 — but **their test
runs must be serialized**. Two concurrent Maven invocations against `backend/` present as roughly 150
bogus failures, which slab 08 recorded the slow way.

Slice 02 is the one most likely to want splitting once it is underway, since it is the only slice
introducing more than one new concept at a time; that is a discovery to make against real code, not a
guess to encode here.

The gate is slice 10 and not slice 01 for the reason slab 07 recorded: a ratchet built first with
every rule exempted means every slice edits the exemption list, and a reviewer cannot easily tell a
legitimate removal from a sloppy one.

## Testing Decisions

A good test here asserts **behaviour a caller of the evaluator could observe** — that an Event is
refused, with which category and reason code, that a listing omits it, that a duplicate key refuses
construction, that a graph loads once. It never asserts a Java type, an annotation, a field or a
call count. The one deliberate exception is the slab gate, whose whole subject *is* structure, and
which says so.

The seams were settled with the developer. **Five, and each is forced by a boundary an ADR draws
rather than chosen:**

- **`TransitionEvaluator` — the primary seam, carrying most of the slab.** Pipeline order,
  short-circuiting, failure categories and reason codes, Required Authority including `SYSTEM`, the
  requirement stage, **both Guard strategies**, Possible Events, and the Action chain a permitted
  outcome reports. Driven over fixture graphs, with no test naming a Guard class.
- **The two registries' constructors.** A duplicate type key is a registry construction failure; no
  graph can express one, so it cannot be reached from the seam above. Also carries unknown type keys
  and malformed params.
- **`CompiledDefinitionCache`.** ADR 0001 hands the evaluator a graph, so the cache sits outside it by
  construction. Keyed on the row id alone, loads once per id, reloads after invalidation, and accepts
  no composite key.
- **Each Action's `execute`.** Actions run only after a commit and this slab commits nothing, so no
  evaluator call can reach an Action body. `SET_POST_VISIBILITY`'s three-into-one collapse is what
  this seam exists to demonstrate, against a port double.
- **The purity gate.** A source scan and a reflective check, not a behaviour.

**Both Guards are tested through the evaluator and nowhere else.** That is the higher seam and it
costs real fixture work — `TERMINAL_AGGREGATION` needs sibling-Resource facts on the context and a
cache double before the first assertion — but the scaffolding it forces into existence is the
evaluation context itself, which every later slab needs anyway. Direct Guard unit tests were rejected
because a stub-driven pipeline test proves nothing about whether the real Guards are reachable in ADR
0005's order, which is the property most worth having.

**The params bridge gets no seam of its own.** The fixture graph builder takes **raw JSON strings** and
runs them through the real binder, so "a Guard received its typed params and behaved accordingly" is
observable at the primary seam, and the one unchecked cast is exercised by every test that uses params
rather than by a test about itself. Binding *failures* — unknown key, malformed JSON, wrong shape —
belong to the registry seam, where construction already refuses.

**Prior art, all of it in this repository.** `WebhookEventMapperTest` is the model for the registry
seam: a pure unit test with no Spring context and no Mockito, instantiating the `@Configuration`
directly and asserting the duplicate-key throw on the constructor with
`assertThatThrownBy(...).hasMessageContaining(...)`. `EnumBackedLifecycleCatalogTest` is the model for
the evaluator's table-driven cases: no Spring, direct `new`, AssertJ, `@ParameterizedTest` with
`@MethodSource` providers placed after the tests. `DefinitionResolverTest` is the model where a
collaborator must be doubled: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
`DefinitionFixtures` is the model for the fixture builder, including its rule about where a fixture
lives and when a helper stays local to one test instead. `DefinitionInertnessGuardTest` and
`LifecycleEnumDecouplingGuardTest` are the model for slice 10 — a working-directory-resolved scan
root, comment blanking, named exemptions, a violation report naming file and line, and an
anti-vacuity test — **copied rather than extracted**, as both of those recorded, because each gate has
its own lifetime and is meant to be deleted whole.

The gate carries the rule an identifier scan cannot express. Slab 07's slice 12 found that a
signature check was needed because a consumer can reach a forbidden type through a method call and
import nothing, and that a detector which only ever sees compliant types would report green forever.
So slice 10 gets a reflective check on the evaluator's constructor **and** a private
`ImpureEvaluatorFixture` supplying the shapes the rules must catch, plus a scan-root resolver that
refuses rather than finding nothing — a trap slab 07 hit three times in three disguises, the last time
inside the very slice that had just fixed it.

Tests live in the production package under `src/test/java`, per slab 08's rule, and are
package-private with a class-level javadoc saying what the test would catch. Both assertion libraries
are in use in this codebase and mixing them in one file is normal.

**Existing seams that must stay green and unchanged.** The parity half of
[parity-gate.md](../state-machine-implementation/parity-gate.md) at **255 tests in 24 classes, 0
failures, 1 skipped**, and the intended-delta half at **8 tests, 0 failures**, after *every* slice.
This slab changes no observable behaviour, so a red parity test is a bug in the slab and never an
intended change. `DefinitionInertnessGuardTest` green at 6 with only slice 01's amendment visible;
`LifecycleEnumDecouplingGuardTest` and `RawStateNamesInSqlGuardTest` green and unamended.

**The full suite is a real check, not a formality.** The registries fold at Spring startup, so all 61
`@IntegrationTest` classes exercise the duplicate-key path and the new beans' wiring. A context that
fails to build is the failure mode to expect from this slab, and it is the one thing unit tests cannot
see.

Read the numbers out of `backend/target/surefire-reports/`, **summing `TEST-*.xml` and not the `.txt`
files** — the plain-text writer reports `Tests run: 0` for the one class with an `@Nested` inner
class, and three slices of slab 07 logged the same four-test gap as unattributed before anyone
compared the two writers. Check every report's mtime is after the run started.

## Out of Scope

- **Wiring the evaluator into either lifecycle service.** The cutover slab. No production code calls
  anything this slab builds, and slice 10 is the proof.
- **Anything that writes:** committing a move, running Actions for real, Lifecycle Records,
  notifications, Audience Notification. ADR 0001 puts all of it in the services around the evaluator.
- **The entity-to-compiled-graph compiler**, and `DefinitionInertnessGuardTest`'s deletion. Deferred
  behind `CompiledDefinitionSource` with a named trigger.
- **The real Information Requirement satisfaction lookup**, Audience resolution and Quantifier
  counting. Port shape only.
- **Deciding which States carry `terminal`**, and the v1 seed generally. Migration slab, blocked by
  map ticket 12.
- **`SPAWN_RESOURCE_LIFECYCLES`'s body.** The coupling slab, since it writes.
- **Resolving map ticket 11.** The evaluator must not invent a disjunction to work around
  single-valued authority.
- **Mapping failure categories to HTTP statuses.** Recorded, not applied.
- **Definition Resolution and the Definition Version Pin's write path.** ADR 0004, and slab 08's
  deferred issues 09 and 10.
- **Deleting Spring Statemachine**, or any of its classes. The library runs, green, throughout.
- **Any Flyway migration.** `V36.5` stays free; this slab adds no DDL and no seed.
- **Any frontend change.** Nothing here is reachable from a screen, so standing decision 5 has
  nothing to bite on and slice 10's caller rule is what proves it.

## Further Notes

- **The ticket's instruction to take the strategies "from ticket 01's graph dump" cannot be followed
  as written.** All 21 Transitions across both graphs record `"guard": null`. The dump is
  authoritative for Actions, States, Events and authorities, and silent on Guards. Recon §1.
- **No app run is owed by this slab**, and that is a departure from every slab before it. Nothing here
  is reachable from a screen. What replaces it is stronger where it matters: the Spring context builds
  in 61 integration test classes, which is where a badly folded registry would fail.
- **`RETURNED_FOR_RESUBMISSION` is the dangling *State*, not the dangling Guard.** Ticket 09 describes
  `NEGOTIATION_APPROVED` as "hacked in as a dangling transition"; that is accurate, and separately
  there is a dangling State no Transition targets. Both survive as Legacy States per ADR 0009. Do not
  conflate them when reading the graph.
- **The compiled graph must tolerate an unreachable State.** Legacy States resolve as positions even
  though nothing targets them, and the Override Event carries no Transition at all.
- **`idx_transition_source_event` is the index the evaluator will live on**, per `V36.2`'s own
  comment. Nothing in this slab issues a query, but the compiled graph's primary lookup should be the
  same `(fromState, event)` shape, so the eventual compiler's query is the obvious one.
- **`InformationRequirement.isViewableOnlyByAdmin`** remains a live field appearing in no ADR. This
  slab does not touch it. Still unowned; the map records it.
- **PostgreSQL only**, per the map's binding constraints.
- `nix develop .#opencode --command` currently fails with `Operation not permitted` from every
  directory; `mvn` and `java` are already on `PATH`. Try the prefix first since parity-gate.md
  specifies it, and drop it rather than treating a failure as a blocker.
- Run the formatter before committing any Java, since it is not bound to the `test` phase:
  `mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format`.
