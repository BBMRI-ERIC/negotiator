# Firing an Event through the new subsystem: context assembly and commit

Type: task
Status: resolved
Blocked by: 09, 14

## Question

Build the module that turns *ids and a caller* into a committed move: resolve the Definition Version
Pin, resolve the Compiled Graph, assemble the evaluation context, call the Transition Evaluator, and
— when the outcome is permitted — write the new State, append the Lifecycle Record and run the Action
chain. Nothing replaces either Lifecycle service here; the Spring Statemachine path stays live
alongside.

Raised by the same architecture review as [ticket 14](14-compiled-graph-resolution.md) (2026-09-09).
Its finding: the Transition Evaluator is deep and pure, and everything the evaluator *refuses to do*
has no owner — so at cutover it gets written twice, once per Definition Scope, which is the drift ADR
0001 says one evaluator exists to prevent, relocated into the assembly.

### Why there is a ticket here at all

- **Nothing in `lifecycle` writes.** There is no `@Transactional` anywhere in the module. No State is
  written, no Lifecycle Record appended, no Action run. `EvaluationOutcome.Permitted.actions()`
  *reports* the ordered chain, and `TransitionEvaluatorTest` has a test whose name is the whole point:
  `evaluate_whenPermitted_reportsTheOrderedActionChainAndRunsNoneOfIt`.
- **The context is wide where the interface is narrow.** An evaluation needs the Negotiation's
  creator id, the Resource's representative ids, the parent Negotiation's State, and — for a
  Negotiation-scope move — every sibling Resource paired with *its own* Compiled Graph. That
  assembly is several reads plus ticket 14's batched resolution, and no module owns it.
- **So the assembly is untested by construction.** All 26 of `TransitionEvaluatorTest`'s contexts are
  hand-built in the test. A context assembled wrongly from real rows is not reachable by any test in
  the suite, which is exactly where the remaining bugs are.

### The Information Requirement lookup is a prerequisite, not follow-on work

`UnbuiltInformationRequirementSatisfaction` **throws**, and `TransitionEvaluator.gate` calls the port
on every path — including `possibleEvents`. So even a read-only Possible Events listing raises
`UnsupportedOperationException` until the real lookup exists. This slab builds it.

**Build today's behaviour, not the ADR's.** The port's javadoc is explicit that "today's check is far
weaker than its name suggests": it asks whether *any* Requirement exists for the Event name anywhere
in the deployment, and then whether *any* submission exists for that resource-and-negotiation pair —
not for that Requirement, and not by anyone in particular. ADR 0006's Audience, Audience Resolver,
Qualifying Submission and Quantifier are each a strengthening of that, "a behaviour change with a
migration story rather than a typo to fix in passing." Reproduce the weak check, and let the
characterization suite hold it. Slab 09's user story 40 already filed the real lookup as later work.

### The module needs its own package, and therefore a name

`EvaluatorPurityGuardTest` forbids the `graph` and `evaluation` packages from naming anything matching
`\b\w*Repository\b` — a **suffix** rule, deliberately, so repositories that do not exist yet are
banned too — and from reaching into `definition` at all. Unlike the other structural gates, that one
is explicitly *not* scheduled for deletion; it is the standing guarantee that the evaluator stays
free of I/O.

This module reads rows and resolves graphs, so it cannot live in either package. **Decide its
package and its glossary term as the slab's first act**, and add the term to `backend/CONTEXT.md` via
`/domain-modeling` before the package exists. `CONTEXT.md` already binds "Lifecycle" to the
progression itself and rejects *workflow*, *process* and *state machine*; whatever names the thing
that *moves* a Lifecycle is a new term. Do not settle it by picking a directory name.

### What to build

- **One interface taking a subject, an Event name and a Caller**, answering with an Evaluation
  Outcome — and a second answering Possible Events for the same subject and Caller. One shared
  assembly behind both, so the listing stays a dry run of the gate rather than a second code path.
- **The Definition Version Pin read**, so a Lifecycle is judged against the version pinned to it and
  not the active one.
- **The commit**: the new State, the Lifecycle Record appended, then the Action chain run in order —
  in that sequence, since an Action runs "only after a Transition commits". Compare
  `PersistStateChangeListener.onPersist`, which is today's equivalent: `@Transactional`, writes the
  state, appends the record, optionally creates a Post, then publishes the state-change event.
- **A refusal that survives to the caller.** ADR 0005's categories map to 403 / 422 / 409, and
  `FailureCategory` deliberately does not carry the status — that mapping is the controller's. This
  slab must at least not lose the category and reason code on the way out.

### Slab gate

- [ ] Possible Events for a real Resource, from ids and a Caller alone, against rows a test wrote.
- [ ] Firing a permitted Event writes the new State, appends exactly one Lifecycle Record, and runs
      the Action chain in order.
- [ ] Firing a refused Event writes nothing, appends no Lifecycle Record, runs no Action, and the
      caller receives the category and reason code.
- [ ] A test fires an Event end-to-end — State A, Event, State B, with a Guard chain and an Action
      chain actually running. **No such test exists anywhere today**, in either subsystem.
- [ ] Possible Events and the real gate agree for the same subject and Caller, asserted against real
      rows rather than hand-built contexts.
- [ ] The Information Requirement lookup reproduces today's check, and the characterization suite says
      so.
- [ ] `EvaluatorPurityGuardTest` green — the new module is in neither guarded package.
- [ ] The parity half of [parity-gate.md](../parity-gate.md) green at 255 tests in 24 classes, with
      both lifecycle paths present and nothing calling the new one from a controller.

### Scope boundary: Resource scope, and where Negotiation scope starts costing

**Recommendation: land Resource scope, and stop.** A Resource-scope context needs no siblings, its
Actions are none today, and it exercises the whole path end to end.

Negotiation scope is what the assembly was interesting for — it is where the sibling Resources and
their own graphs arrive, and therefore where `TERMINAL_AGGREGATION` and Feedback become testable. It
also forces three decisions the map files elsewhere, and taking them here means taking them without
their tickets:

- **How a Resource's pin is written at all.** The column is `updatable = false` and the link row
  already exists at Spawn, so the mapping cannot write it. Three options are filed; no migration is
  needed for any of them.
- **What an unresolvable definition does to a Negotiation approval.** A rolled-back 500 is today's
  default.
- **The pin-column indexes**, absent on purpose because the columns are 100% NULL and both tables are
  among the most-written.

If Negotiation scope is wanted in this slab, pull those three in deliberately and say so in the
inner tracker. Otherwise they stay with the cutover.

### Out of scope

- **Replacing either Lifecycle service, and deleting Spring Statemachine.** Standing decision 2:
  decouple consumers first, then swap the engine. Both paths coexist here.
- **`SPAWN_RESOURCE_LIFECYCLES`' body.** Registered and throwing; the coupling slab owns it, and its
  javadoc lists three things ADR 0007 and 0009 get wrong about spawn that the characterization suite
  pinned. Read it before touching it.
- **The Orchestration Trigger and any scheduler.** ADR 0007 leaves the plumbing undesigned;
  `Caller.TheSystem` and `RequiredAuthority.SYSTEM` exist, the puller does not.
- **The real `PostVisibility` adapter**, unless Negotiation scope is pulled in — the three
  post-visibility Actions sit on Negotiation Transitions. Note that the same architecture review
  argues the `SET_POST_VISIBILITY` strategy should move *out* of `lifecycle` to the subsystem owning
  the effect, deleting the outward port rather than implementing it. That is a separate question and
  a separate ticket if it is wanted.
- **A REST surface for the new module.** No controller, no DTO, no assembler; the existing endpoints
  keep their current shape and their current implementation.

### Note

Read ADRs [0001](../../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md),
[0005](../../../backend/docs/adr/0005-information-requirements-gate-transitions-as-a-built-in-stage.md)
and [0008](../../../backend/docs/adr/0008-lifecycle-history-references-states-by-fk.md) first, plus
Part 3 and Part 7 of [before-picture-findings.md](../before-picture-findings.md) — several documents
specify this area against a picture of the code that the characterization suite disproved.

ADR 0001 is the argument for this ticket rather than against it: "the Negotiation and Resource
lifecycle services stay separate — they own different JPA entities and different persistence wiring —
but they call one shared evaluation path." The evaluator is that shared path for *judging* a move.
This ticket reads "shared evaluation path" as including the assembly of what is judged and the commit
of what was permitted, because that is where the duplication actually sits once the evaluator refuses
to do either.

## Answer

**Resolved 2026-09-14. Resource scope landed; Negotiation scope refused by name.** Built directly
rather than through an inner tracker, following [ticket 14](14-compiled-graph-resolution.md)'s
precedent — the scope decision below removed the only fork the slab had.

### The two decisions this ticket said to take first

- **The glossary term is Event Firing, and the package is
  `eu.bbmri_eric.negotiator.lifecycle.firing`.** Taken with the developer before the package existed,
  and recorded in `backend/CONTEXT.md` under Evaluation, next to the Transition Evaluator it is
  defined against: *"Presenting an Event to a Lifecycle so that the Evaluation Pipeline judges it
  and, when it permits, the Lifecycle moves — the new State recorded, the Lifecycle Record appended
  and the Actions run. Where the Transition Evaluator only judges, firing is what reads the
  Definition Version Pin the judgement is made against, and the only thing that writes."* `_Avoid_:
  send event, trigger, dispatch, execute transition. "Fire" was already the glossary's verb — Possible
  Events is defined as "the Events a caller could fire right now" — so the term nominalizes existing
  vocabulary rather than introducing any.
- **Resource scope, and stop** — this ticket's own recommendation, taken deliberately. The three
  decisions Negotiation scope would have forced stay with the slabs that own them, and
  `UnsupportedScopeException` names all three where a developer will actually meet them.

### The slab gate

| Criterion | |
|---|---|
| Possible Events for a real Resource, from ids and a Caller alone | ✅ |
| A permitted Event writes the State, appends exactly one Lifecycle Record, runs the Action chain in order | ✅ |
| A refused Event writes nothing, appends nothing, runs no Action, and the category and reason code reach the caller | ✅ |
| An Event fired end to end — State A, Event, State B, Guard chain and Action chain actually running | ✅ |
| Possible Events and the real gate agree, against real rows | ✅ |
| The Information Requirement lookup reproduces today's check | ✅ |
| `EvaluatorPurityGuardTest` green, the new module in neither guarded package | ✅ |
| Parity **255 tests in 24 classes, 0 failures, 1 skipped**, both lifecycle paths live, no controller calling the new one | ✅ |

**`EventFiringIntegrationTest` — 21 tests, and it is the test this ticket said does not exist
anywhere.** It sits in `lifecycle` rather than `lifecycle.firing`, for the reason slab 14's test sits
one package out from `lifecycle.definition`: from there the module's edge is the only thing nameable,
so that it compiles is an assertion about the surface. Definition rows and the pin are written as
SQL. **The commit assertions were checked against a mutant** — the `commit(...)` call removed — and
three tests go red, so they are not passing by construction.

### What the module is

Five production files. `EventFiring` (the edge: `fire` and `possibleEvents`), `LifecycleRef` (a
sealed subject, one case per Definition Scope), `LifecycleContextAssembler`, `EventFiringImpl`,
`TodaysInformationRequirementLookup`, plus `AssembledEvaluation` and the two exceptions. Everything
but the interface, the ref and the exceptions is package private.

- **One assembly behind both methods**, so the listing is a dry run of the gate rather than a second
  code path — including the same reads, which is the half ADR 0005's argument actually rests on.
- **The pin is read, never resolved.** `resolveForResource()` is not called and must not be:
  resolution answers what *new* work runs under, and a Lifecycle in flight is judged against what was
  pinned to it or the pin does nothing.
- **The commit writes through `Negotiation.setStateForResource`**, which already writes the link row
  and appends the Lifecycle Record together. "Exactly one record" falls out of reusing the entity's
  rule rather than being counted — including its one exception, that arriving at the initial State
  appends nothing.
- **`@Transactional` on public methods of a package-private class**, which is the trap slab 14
  recorded: Spring's proxy applies to public methods only and ignores a package-private one silently.
- **The subject committed to comes from the evaluation context, not from the caller's reference.**
  They name the same thing; taking it from the context makes "the Lifecycle written to is the one
  that was judged" structural rather than a convention.

### `UnbuiltInformationRequirementSatisfaction` is deleted

Its javadoc said "deleting this class is how the IR slab announces itself", and this is that. It
threw on every path including `possibleEvents`, so a read-only listing raised
`UnsupportedOperationException` until now. `TodaysInformationRequirementLookup` replaces it in the
`firing` package — it *must* be outside `evaluation`, because it holds two repositories and
`EvaluatorPurityGuardTest` bans the `Repository` suffix there.

**It reproduces today's weak check and does not improve it.** Any Requirement for the Event name
anywhere in the deployment, then any submission for that resource-and-negotiation pair — not for that
Requirement, and not by anyone in particular. The predicate is line-for-line the one in
`ResourceLifecycleServiceImpl.sendEvent`.

**What "the characterization suite says so" can and cannot mean**, since this ticket's gate asks for
it: that suite exercises the **old** path only, so nothing in it runs this class, and its staying
green proves the old path is untouched rather than that the new one matches. What actually holds the
equivalence is five new tests mirroring five of `ResourceInformationRequirementGateTest`'s eight
cases — the five that are statements about the predicate; the other three are about the old path's
*ordering* and its exception type, which ADR 0005 deliberately changes. Including the two cases that
look like bugs and are the point:
`submissionAgainstADifferentRequirement_satisfiesTheGate` and
`submissionForADifferentResource_doesNotSatisfyTheGate`. ADR 0006's Audience and Quantifier are the
change that makes the first false, with a migration story of its own.

### Two findings a later slab needs

- **A committed State must be a name the legacy enum still knows.** `NegotiationResourceLifecycleRecord`
  stores `changed_to` as `NegotiationResourceState` and resolves it through `valueOf` — "deliberately
  the loud kind". So a Definition Version naming a State that enum does not carry compiles,
  evaluates, is permitted, **and then fails at the append**. Found by writing a graph with invented
  State names; the test now uses legacy-valid ones. This is a hard constraint on seeding and on the
  cutover, and it lifts only with ADR 0008's `state_id` FK conversion. Recorded in
  `EventFiringImpl.commit`'s javadoc where someone will meet it.
- **No state-change event is published, deliberately.** Today's `PersistStateChangeListener.onPersist`
  publishes one and notifications and webhook deliveries ride on it. Ticket 02 already hands the
  coupling slab two constraints about exactly that — Spawn must *not* publish
  `ResourceStateChangeEvent`, and notification is to ride on a new `ResourceLifecyclesSpawnedEvent`
  — so publishing here would pre-empt that decision with the one option already known to be wrong for
  one of its callers. Nothing calls this from production, so nothing is currently unannounced.

### One production file outside the module changed

`Negotiation.getLifecycleDefinitionIdForResource(String)`, the sibling of the existing
`getCurrentStateForResource`, keyed the same way and answering `Long` because the column is still
nullable. The pin lives on the link row and `resourcesLink` has a private getter, so there was no
way to read it from outside. **How production *writes* a pin is still unanswered** — the column is
`updatable = false` and the row already exists — and the test writing its own pin as SQL is not an
answer to it.

### Out of scope, and still out

Neither Lifecycle service replaced, Spring Statemachine not deleted, both paths live.
`SPAWN_RESOURCE_LIFECYCLES` untouched. No Orchestration Trigger. No real `PostVisibility` adapter —
the Action chain is proven with recording test strategies instead, because the two real Actions both
reach a throwing placeholder and implementing either would be building another slab's work inside
this one's test. No REST surface, no controller, no DTO.

### Review, and what it changed

Both axes of `/code-review` ran against the diff. Nine findings were acted on; the substantive ones:

- **A real defect, from the spec axis and the standards axis both.**
  `EvaluationContext.forResource` declares its parent-State parameter `@NonNull` and
  `negotiation.getCurrentState()` is nullable, so a Negotiation with no State produced a bare Lombok
  `NullPointerException` naming a parameter — from inside a factory the caller never called, and
  flatly contradicting `EventFiring`'s "every way this could fail to answer throws". Now routed
  through `UnstartedLifecycleException` like every sibling case.
- **A trap set for a later session.** The test wrote its Definition Version with `active = TRUE` and
  `is_global_default = TRUE`. Neither is needed — the pin is read, never resolved — and
  `uq_lifecycle_definition_global_default` is a unique index over the *whole table*, so the test
  would have started colliding with ADR 0009's v1 seed the moment that seed landed, failing anywhere
  but here. Both flags are now `FALSE`, with the reason written where they are set.
- **A decision taken in a branch that the slab had refused by name everywhere else.** The
  Information Requirement lookup passed a Negotiation-scope context unchecked, which is not
  "reproducing today" but deciding that Negotiations are ungated for ever. It now throws
  `UnsupportedScopeException`.
- **Two justifications that overreached, corrected rather than defended.** The deferral of the
  state-change event cited ticket 02, whose constraints are about *Spawn* and do not cover firing a
  Resource Event — the javadoc now says plainly that this is a gap the cutover owns, and what it
  would silence. And "the characterization suite says so" cannot mean what this ticket's gate implies:
  that suite runs the old path only, so its staying green proves the old path untouched, not that the
  new one matches. The five mirroring tests are what hold the equivalence, and the answer above now
  says which three of the eight they deliberately do not mirror.
- **Duplication removed rather than added to.** `Negotiation` had the same resource-link walk written
  out three times, throwing three different things for the same miss; this slab would have made it a
  fourth. Extracted to one `linkFor`. The six SQL row writers were about to exist twice, so they are
  now `DefinitionRows`, shared with `CompiledGraphResolutionIntegrationTest`.

One finding was **declined**: that `@DirtiesContext(AFTER_EACH_TEST_METHOD)` on 21 tests is 21
context rebuilds. It is, and it costs about 80 seconds — but `parity-gate.md` makes it the rule for
any class that fires Events, because the corpus is shared and a class that does not dirty turns
another class red by test ordering alone. The alternative is per-method cleanup of five tables, which
is more code and more risk for less certainty.
