# Firing an Event through the new subsystem: context assembly and commit

Type: task
Status: open
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
