# Freeze current behaviour

Type: task
Status: resolved

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

## Progress

**Claimed and charted 2026-08-13.** The slab's inner tracker is [`.scratch/freeze-current-behaviour/`](../../freeze-current-behaviour/PRD.md) — a PRD plus 11 commit-sized issues. Work resumes there; do not re-plan this slab. Branch: `feat/state-machine-implementation`, off `plan/state-machine-redesign` per standing decision 4.

Three decisions taken with the user while charting, not up for relitigation inside the slab:

1. **Seams:** the two lifecycle service interfaces (primary), the two lifecycle controllers, and the two state-change application events via `@RecordApplicationEvents`. All three already exist in the test tree; no new production seam.
2. **The string-and-adapter rule.** A suite that must pass *unchanged* cannot name the four deleted enums, so characterization tests name States and Events as **string literals** and call through one test-scope adapter — the only test file permitted to reference the enums. At cutover the adapter is rewritten once and every assertion is untouched. This deliberately decouples the parity gate from ticket [03](03-state-event-identity-downstream.md)'s identity decision, which is why the gate can be built before that decision exists. A forbidden-import test enforces it mechanically.
3. **Dump fidelity:** guard/action identity recovered by reflective unwrap of SSM 4.0's wrapped lambdas, throwing rather than emitting a half-faithful artifact. Output is canonical JSON plus a Mermaid view generated from it, which also gives `docs/negotiation_state_machine.png` a regeneration path.

Two structural findings to confirm during the slab, not fix:

- **`NegotiationIsApprovedGuard` is probably dead.** Attached only by `transitions.withExternal().guard(negotiationIsApproved())` at `ResourceStateMachineConfig.java:117` — a fragment with no source, event or target. The real IN_PROGRESS check is imperative in `ResourceLifecycleServiceImpl.getPossibleEventsForCurrentStateMachine`. If dead, this ticket's "every guard outcome, including `NegotiationIsApprovedGuard` in both directions" is satisfied by pinning the imperative gate instead — and the Guard must not be reimplemented in ADR 0002's registry.
- **`DRAFT` may be unreachable as an entry state** — the graph declares SUBMITTED initial while also defining `DRAFT → SUBMIT → SUBMITTED`. ADR 0009's seed must reproduce whichever is true.

Two corrections to this ticket's own text, made from the code:

- The "**seven** `NotificationStrategy` handlers" is wrong on both counts: there are **eight** strategies, of which **five** key on lifecycle identity (in-progress, submission, status-change, resource-state-change, and pending-reminder on `REPRESENTATIVE_CONTACTED`). Only those five are pinned.
- The baseline note "no seed SQL exists" — inherited from the map's verified facts — was **false**; see the corrected fact on the map. `@IntegrationTest(loadTestData = true)` loads a real seed that already contains lifecycle state and history rows, and the suite builds on it.

Also newly owned by this slab: `GET /v3/resource-lifecycle` publicly returns a graph diagram built by recursively walking SSM's transition set. No ADR owns it; it is pinned in the REST-seam issue.

## Answer

**Resolved 2026-08-17. Both deliverables exist and the gate is green.**

**The gate.** `255 tests in 24 classes` under `eu.bbmri_eric.negotiator.characterization.**`, 0 failures, 0 errors, 1 deliberate skip (the dump generator, which only writes under `-Dlifecycle.dump.regenerate=true`), plus **8** ADR 0005 intended-delta tests carrying `@Tag("intended-delta")` and excluded from the parity number — a red test there is the cutover succeeding. The two commands, the counts each must produce, the test-ordering rule and the coverage gaps are in **[parity-gate.md](../parity-gate.md)**, which lives beside the map rather than inside the slab directory because the slab's `STATUS.md` is deleted when it closes and the gate outlives it.

**No production code changed anywhere**, verified as an empty `git diff` over `backend/src/main`, `frontend/` and the POMs across all of the slab's commits — which is what makes the suite credible as a before-picture.

**The dump.** Both graphs are emitted by walking the live beans, never transcribed: `negotiation-graph-v1.json`, `resource-graph-v1.json` and `graphs-v1.mmd` under `backend/src/test/resources/lifecycle/`, committed, regenerated and byte-compared on every run. Guard and Action identity is recovered by reflective unwrap, throwing rather than degrading. The hand-written graph tables in the parity tests are bound edge-for-edge to those artifacts, so no expectation in the suite rests on a hand transcription.

**The string-and-adapter rule holds.** One test-scope adapter names the enums; `guard/CharacterizationImportGuardTest` enforces it mechanically. Two honest qualifications on that claim: the rule is a property of the *suite*, and **29 files** elsewhere under `src/test` still reference the four enums (the slab PRD said 26) — that churn belongs to the decouple-consumers slab. And the Spring Statemachine exemption is the throwaway `dump` **package**, five files, not a single one.

### Both structural findings confirmed, neither fixed

- **`NegotiationIsApprovedGuard` is dead.** All 21 Transitions across both graphs dump `"guard": null`; the `withExternal().guard(...)` fragment produces no Transition at all, and this was verified not to be an unwrap failure in disguise. What is pinned instead is the imperative IN_PROGRESS gate at the service seam, walked across all 8 Negotiation States. **It must not be reimplemented in ADR 0002's registry** — it has never fired.
- **`DRAFT` is occupied but not enterable**, which turned out to be two separate facts. `negotiation-6` is seeded in `DRAFT`, the initial State is `SUBMITTED`, and no Transition targets `DRAFT`. ADR 0009's seed must reproduce both halves.

### What this ticket now owes the rest of the effort

The findings are consolidated in **[before-picture-findings.md](../before-picture-findings.md)** — 18 findings with evidence, the coverage gaps, and a closing list of decisions the redesign now owes. **Read its part 3 (corrections owed upward) and part 7 (open decisions) before implementing ADR 0005, 0007 or 0009.**

The sharpest item is a correction this ticket's own text contributed to. **Spawn does not do what three settled documents say it does:** it writes `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE`, never the graph's initial State; it publishes no `ResourceStateChangeEvent` at all, so the conclusion listener and the webhook subsystem never hear of it; and it keys on *arriving at* `IN_PROGRESS`, so `PAUSED --UNPAUSE--> IN_PROGRESS` spawns exactly as `APPROVE` does. ADR 0007 specifies `SPAWN_RESOURCE_LIFECYCLES` as setting the initial State and wires it to the approval Transition alone — wrong twice over — ADR 0009 seeds against that specification, and `backend/CONTEXT.md`'s **Spawn** entry repeats it. **None of the three was edited**, per the binding constraint that an ADR contradiction gets its own decision ticket rather than a quiet edit. That ticket does not exist yet and is the first thing stage 1 needs.

Also unpinned and deliberately so: `ResourceServiceImpl.updateResourceStatus` writes an arbitrary State straight onto the link row and publishes a `ResourceStateChangeEvent` stamped `OVERRIDE` — no Transition, no authority rule of the graph, no IN_PROGRESS gate. The event seam's assumption that the event traces a Transition does not always hold.
