# Freeze current behaviour

Type: task
Status: open

## Question

Nothing in this effort is verifiable until today's behaviour is pinned. **Slab gate: a characterization suite that passes against the current Spring Statemachine code, with no production code changed.** Those same tests must later pass *unchanged* against the new subsystem — that is stage 1's gate.

Two deliverables.

### 1. A mechanical dump of both definition graphs

Walk the live `NegotiationStateMachineConfig` and `ResourceStateMachineConfig` beans and emit their full graphs: states, events, transitions, guards, actions, required authorities, and which state is initial. **Generate it by walking the beans, not by transcribing by eye** — ADR 0009's correctness claim is precisely that the v1 seed faithfully reproduces those two classes, so a mechanically derived artifact is the only honest oracle.

This artifact does triple duty and resolves a circular dependency in the ADR set: ADR 0009 places the v1 seed SQL in the migration slab, but seeded definitions are needed as a **test fixture** from the moment the Transition Evaluator exists. One artifact serves as (a) the oracle for the characterization tests, (b) the dev/test fixture, and (c) the reference for 0009's frozen seed SQL. Whether the seed SQL is *generated from* it is not decided here — that belongs to the migration slab.

Link the dump from this ticket as an asset.

### 2. Characterization tests

Pin today's behaviour at a level that **survives the implementation swap** — this is the hard part. Assert through the lifecycle service APIs and the REST controllers, never against Spring Statemachine internals; a test that mentions a `StateMachine` bean, a `StateContext` or a persist handler is worthless post-cutover. Coverage to reach:

- Every transition of both graphs: from-state, event, to-state.
- Every guard outcome, including `NegotiationIsApprovedGuard` in both directions.
- The Information Requirement gate as `ResourceLifecycleServiceImpl.sendEvent()` enforces it today.
- The spawn path (`NegotiationInProgressHandler` → resource state initialization) and the conclusion path.
- The `*LifecycleRecord` history rows written by a transition.
- The seven `NotificationStrategy` handlers' firing conditions, since they key on enum constants and are the likeliest silent breakage.

**Two explicit carve-outs — do not pin these as parity.** ADR 0005 deliberately changes them, so pinning current behaviour would freeze the bugs the ADRs exist to fix:

- `getPossibleEvents` — today it lists Events that then fail on click; ADR 0005 omits blocked Events.
- The requirement hint links in `ResourceWithStatusAssembler` — inclusion condition becomes structural reachability, display name becomes the Event's label, and per-row `requirement-7` rels collapse into array-valued rels.

Record both as **intended deltas** with a test asserting the *new* behaviour is absent today, so the cutover slab has something to flip.

### Notes

- Baseline: 138 test files, only 4 touch lifecycle (`integration/service/NegotiationLifecycleServiceImplTest`, `integration/api/v3/LifecycleInfoTests`, two `unit/model/*LifecycleRecordTest`).
- 26 of 138 test files reference the four enums and will churn in a later slab — that is expected and not this slab's problem.
- Frontend is out of this slab: standing decision 5 puts FE fixes in the slab that breaks them, verified manually.
