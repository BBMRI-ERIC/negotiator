# Lifecycle graph dump generator and frozen v1 artifacts

Status: ready-for-human

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

A test-scope generator that walks the two live Spring Statemachine beans — the Negotiation one and
the Resource one — and emits their complete structure as canonical JSON, plus a Mermaid state
diagram generated from that JSON.

This is the **only** component in the slab permitted to touch Spring Statemachine internals. It is a
throwaway, deleted at cutover, and it is a generator rather than a parity test.

Per graph it records: every state, the initial state, and every transition with its source, event,
target, kind, security rule attributes and comparison type, its actions, and its guard.

Guard and Action **identity** is the hard part. SSM 4.0 hands back wrapped lambdas rather than the
`Guard` and `Action` beans, so the generator recovers the underlying bean class by reflecting on the
lambda's captured field. If that unwrap fails it **throws** — a partially faithful dump must never be
written, because faithfulness is the artifact's entire value. There are three Actions and one Guard
in total.

The generated files are committed, and a test regenerates them and asserts equality against the
committed copies, so the artifact cannot silently drift from the beans while SSM still lives.

Finally, the dump has to adjudicate two structural questions found while charting, and the answers
are reported in this issue rather than acted on:

- **Does `NegotiationIsApprovedGuard` sit on any transition at all?** It is attached only by a
  `withExternal().guard(...)` fragment with no source, event or target, while the real
  "Negotiation must be IN_PROGRESS" check is imperative inside the Resource lifecycle service.
- **Is `DRAFT` reachable as an entry state?** The Negotiation graph declares SUBMITTED initial while
  also defining a `DRAFT → SUBMIT → SUBMITTED` transition.

## Acceptance criteria

- [x] A generator walks both state machine beans and writes one canonical JSON file per graph, with
      stable key ordering so regeneration is byte-reproducible.
- [x] Each transition records source, event, target, kind, security rule attributes, comparison type,
      actions and guard.
- [x] Guard and Action bean classes are recovered by name via reflective unwrap.
- [x] The generator throws, and writes nothing, if any guard or action cannot be unwrapped.
- [x] A Mermaid diagram is generated from the JSON, not from the beans, and is committed alongside it.
- [x] Both JSON files and the Mermaid file are committed as frozen v1 artifacts.
- [x] A test regenerates all three and fails on any difference from the committed copies.
- [x] The dump's state and transition counts are sanity-checked against the two configuration
      classes, and any discrepancy is investigated before the artifact is accepted.
- [x] Whether `NegotiationIsApprovedGuard` is attached to any transition is stated as a finding in
      this issue, with the evidence from the dump.
- [x] Whether `DRAFT` appears as a reachable entry state is stated as a finding in this issue.
- [x] No production code is modified.

## Blocked by

None - can start immediately.

## What was built

Test scope only, all under `eu.bbmri_eric.negotiator.characterization.dump`:

| File | Role |
|---|---|
| `LifecycleGraphDumper` | Walks one live bean, renders canonical JSON; renders Mermaid from the JSON. The only file in the slab that imports `org.springframework.statemachine..*`. |
| `LifecycleGraphArtifacts` | The three artifacts as filename to content. Produced whole or not at all; writes nothing itself. |
| `LifecycleGraphDumpGeneratorTest` | The generator. Gated on `-Dlifecycle.dump.regenerate=true`, writes into `src/test/resources/lifecycle/`. |
| `LifecycleGraphDumpDriftTest` | The gate. Regenerates, asserts byte equality against the committed copies, sanity-checks counts, pins the findings below. |
| `LifecycleGraphDumperUnwrapTest` | Pins the reflective unwrap for all four Guard/Action beans and the throw-rather-than-degrade contract. |

Artifacts: `backend/src/test/resources/lifecycle/{negotiation-graph-v1.json,resource-graph-v1.json,graphs-v1.mmd}`.

Run the gate with `/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'`.
Regenerate with `/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'LifecycleGraphDumpGeneratorTest' -Dlifecycle.dump.regenerate=true`.

## Findings

### 1. `NegotiationIsApprovedGuard` is attached to no Transition. It is dead code.

**Answer: no. Not one Transition in either graph carries a Guard.**

Evidence, from the dump itself: every one of the 13 Transitions in `resource-graph-v1.json` and all 8
in `negotiation-graph-v1.json` records `"guard" : null`. The fragment at
`ResourceStateMachineConfig.java:117` — `transitions.withExternal().guard(negotiationIsApproved())`,
with no source, event or target — produces **no Transition at all**: the Resource graph's
`transitionCount` is 13, exactly the 13 fully-specified `withExternal()` chains above it. There is no
fourteenth, orphaned entry with null endpoints. Spring Statemachine discards the fragment silently.

The unwrap is not what is missing. `LifecycleGraphDumperUnwrapTest` feeds
`Guards.from(new NegotiationIsApprovedGuard())` — the exact wrapper Spring Statemachine would hand
back — to the same code path the dumper uses, and it recovers `NegotiationIsApprovedGuard`. So the
generator would have named the Guard had any Transition carried it. It reports none because there are
none.

**Consequence for ticket 04 and for ADR 0002's Guard registry:** the Guard has never fired in
production. Reimplementing it would not preserve behaviour, it would *introduce* a check that does
not exist today. The real "Negotiation must be IN_PROGRESS" gate is imperative, in
`ResourceLifecycleServiceImpl.getPossibleEventsForCurrentStateMachine`, and that imperative gate is
what must be pinned at the service seam and reproduced. `NegotiationIsApprovedGuard` itself must
**not** be carried into the new registry.

### 2. `DRAFT` is declared, but is not an entry State.

**Answer, as far as the dump can settle it: no.**

`negotiation-graph-v1.json` records `"initialState" : "SUBMITTED"`. `DRAFT` does appear in `states`,
and `DRAFT --SUBMIT--> SUBMITTED` (action `EnablePublicPostsAction`) is a real Transition in the
graph. But **no Transition anywhere targets `DRAFT`**. So within the graph, `DRAFT` is a source with
no way in: the machine starts at `SUBMITTED`, and nothing can move a Lifecycle back to `DRAFT`.

The only way a Negotiation could observably be in `DRAFT` is if something outside the graph writes
that value onto the row directly. That is the service-seam half of the question and belongs to
ticket 03. The graph half is settled: `DRAFT` is not reachable *through the Lifecycle*.

### 3. Unbidden: every `secured(..., ComparisonType.ALL)` is really `ANY`.

Both configuration classes declare `SecurityRule.ComparisonType.ALL` on all 15 secured Transitions.
The dump records `"comparisonType" : "ANY"` on every single one.

This is not a dump defect. `AbstractTransitionConfigurer.setSecurityRule(String, ComparisonType)` in
Spring Statemachine 4.0.0 **ignores its `ComparisonType` argument** — its bytecode calls only
`setAttributes(...)` — and `SecurityRule`'s constructor defaults `comparisonType` to `ANY`. The
declared `ALL` is dropped on the floor by the library.

It happens to be harmless today: every rule carries exactly one attribute, and over one attribute
`ANY` and `ALL` are indistinguishable. Recorded so that ADR 0002's Required Authority model is built
against what the system does rather than what the builder chain appears to say, and so nobody later
"restores" an `ALL` semantics that has never been in effect.

### 4. Unbidden: both graphs declare more States than their Transitions use.

Both configurations register the whole enum via `EnumSet.allOf(...)`, not just the States their
Transitions mention. So the Negotiation graph has **8** States, not the 6 the hand count expected, and
the Resource graph has 12.

The extras are Legacy States in `backend/CONTEXT.md`'s sense — declared, but no Transition leads to
one: `APPROVED` in the Negotiation graph, `RETURNED_FOR_RESUBMISSION` in the Resource graph.
ADR 0009's seed must carry them, or existing rows naming them stop resolving.

The Transition counts match the hand count exactly: 8 and 13.
