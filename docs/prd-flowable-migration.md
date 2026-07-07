# PRD: Migrate State Machine Implementation from Spring State Machine to Flowable (BPMN-backed)

## Problem Statement

The Negotiator relies on Spring State Machine (SSM) 4.0.0 to manage the lifecycle of Negotiations and Resources. SSM has reached end-of-life and is no longer maintained. The current implementation couples domain logic to SSM-specific concepts (PersistStateMachineHandler, SecurityRule, StateContext, message headers), making the code difficult to test in isolation, debug (reactive `.subscribe()` boundaries), and extend toward future needs.

A separate prototype (`docs/prd-stateless4j-migration.md`) evaluates stateless4j, a lightweight, plain-Java, no-framework-coupling library, as the replacement. This document evaluates a second, independent candidate: Flowable, a maintained BPMN process engine, used with hand-authored BPMN XML as the state machine's configuration format. A full BPMN engine is plausibly overkill for a finite-state-machine use case this size — the point of this prototype is to find out concretely whether it is, by building the same migration against it and comparing the result against stateless4j on equal footing.

## Solution

Build a prototype on branch `proto/state-machine-flowable` that fully migrates both state machines (negotiation and resource) from Spring State Machine to Flowable, using hand-authored BPMN process definitions deployed as classpath resources. The prototype reproduces current behavior exactly (existing integration tests as acceptance bar) while using Flowable idiomatically — real, persistent process instances per entity, not a stateless simulation of one — so that its actual strengths and costs (visual/inspectable configuration, engine footprint, testability, typed-safety trade-offs) show up concretely rather than theoretically.

The prototype is an exploration artifact — not intended for merge — to evaluate, side by side with the stateless4j prototype:
- Whether Flowable covers current lifecycle needs when used the way a BPMN engine is meant to be used
- What it actually costs (schema footprint, test runtime, lost type-safety) versus stateless4j
- Whether the "good integration seams, usable efficiently now" bar is met, independent of the future data-driven-workflow story (which is out of scope here and already documented elsewhere)
- How well the team can understand and maintain a Flowable-based implementation

## User Stories

1. As a developer, I want the state machine implementation to use an actively maintained engine, so that we are not blocked by SSM's end-of-life.
2. As a developer, I want lifecycle transitions to be testable through the existing Spring Boot integration test seam, so that verifying behavior doesn't require building new test infrastructure just to get started.
3. As a developer, I want a synchronous, steppable transition path (no reactive `.subscribe()` boundary), so that I can debug lifecycle transitions with a standard debugger.
4. As a developer, I want to know precisely where Flowable's stringly-typed process-variable model diverges from a compile-time-safe typed context, so that the real cost of that gap can be weighed against stateless4j's typed context.
5. As a developer, I want the state machine configuration expressed as an inspectable BPMN process definition (not a Java DSL), so that `getPossibleEvents` and `getStateMachineDiagram` can be computed by walking the BPMN model directly.
6. As a developer, I want security attributes on transitions expressed as BPMN extension-element metadata, so that the existing role-based filtering in `getPossibleEvents` continues to work identically.
7. As a developer, I want to know whether the `@Lazy` circular-dependency workaround (action beans → NegotiationService → lifecycle service → action beans) is actually eliminated by Flowable's lazy, execution-time bean resolution, verified by building it rather than assumed.
8. As a developer, I want each Negotiation/Resource to have its own persistent Flowable process instance correlated by a business key, so that the engine is exercised the way it's actually designed to be used, not forced into a shape that only superficially resembles stateless4j.
9. As a developer, I want lifecycle audit records (`NegotiationLifecycleRecord`, `NegotiationResourceLifecycleRecord`) to continue being written with the same shape and timing as today, so that the timeline UI is unaffected.
10. As a developer, I want ApplicationEvents (`NegotiationStateChangeEvent`, `ResourceStateChangeEvent`) to continue being published on state transitions, so that webhook and notification handlers are unaffected.
11. As a developer, I want the cross-machine auto-conclude logic (all resources terminal → negotiation concludes) to continue working, so that existing behavior is preserved.
12. As a developer, I want transitions that fail business-rule preconditions to throw a domain-named exception (not a Flowable/`BpmnError`-shaped one), so that exception semantics are clear and HTTP mapping lives at the boundary.
13. As a developer, I want the `getStateMachineDiagram` REST endpoint to return the same response shape, so that any consumers are unaffected.
14. As a developer, I want debug logging at key points (process instance start, message correlation, transition outcome, persist), so that production issues are diagnosable from logs alone.
15. As a developer, I want the prototype to reproduce actual current behavior for `NegotiationIsApprovedGuard` — which is dead code in the current SSM configuration, not a working global guard — rather than accidentally introducing new, previously-nonexistent enforcement.
16. As a developer evaluating Flowable, I want a concrete measurement of the schema/runtime footprint a persistent-process-instance model adds (`ACT_RU_*`/`ACT_HI_*` tables) compared to today's columns-only state model, so that the footprint tradeoff is data, not a guess.
17. As a developer evaluating Flowable, I want a concrete measurement of engine-bootstrap and per-test overhead against the Spring Boot integration test seam, so that the testability comparison against stateless4j's pure-POJO unit tests is quantified.
18. As a developer, I want the module/package structure to mirror the stateless4j prototype's shape wherever the concepts align, so that the two proposals are easy to compare side by side.
19. As a developer, I want an explicit, documented list of where Flowable's model forces awkward or partial solutions (stringly-typed variables, no fast Spring-free unit-test seam, no native representation for a cross-cutting guard applied to every transition), so that the go/no-go decision weighs real gaps rather than assumed strengths.
20. As a developer, I want the architecture to leave the same seam BPMN naturally invites for future data-driven/configurable workflows, without building that capability now, so that this evaluation stays scoped to SSM replacement only.

## Implementation Decisions

### Module structure

A `lifecycle` Spring Modulith module, matching the stateless4j prototype's naming for direct comparability, with two internal packages:

- `lifecycle.negotiation` — negotiation process integration beans (delegates, listeners), lifecycle service, controller, cross-machine auto-conclude listener.
- `lifecycle.resource` — resource process integration beans, lifecycle service, controller.

Unlike the stateless4j prototype, there is no `lifecycle.statemachine` generic-SPI package: Flowable's own engine APIs (`RuntimeService`, `RepositoryService`, `TaskService`) are the generic core, and they are a third-party dependency rather than in-house code with a domain-import boundary to police.

BPMN process definitions live under `src/main/resources/processes/negotiation.bpmn20.xml` and `resource.bpmn20.xml`, hand-authored and deployed automatically at startup by `flowable-spring-boot-starter`.

Domain vocabulary (enums, ApplicationEvents, audit entities) stays in the `negotiation` module, same one-way `lifecycle` → `negotiation` dependency direction as the stateless4j prototype.

### Flowable dependency

`org.flowable:flowable-spring-boot-starter`, Flowable 7.x line (targets Spring Boot 3.x / Java 17+ baseline — compatible with this repo's Spring Boot 3.5.15 / Java 21; the 6.x line targets Spring Boot 2 and is not usable here). Exact patch version to be pinned at implementation time.

### Instance model: persistent process instance per entity

Unlike stateless4j's disposable-machine-per-transition model, Flowable process instances are long-running by design. This prototype uses that idiomatically rather than fighting it: one persistent process instance per `Negotiation` and per `NegotiationResource`, correlated by business key = entity ID.

- Started via `runtimeService.startProcessInstanceByKey(...)` at entity creation, in the same transaction as entity persistence.
- Terminal domain states (`CONCLUDED`, `ABANDONED`, etc.) map to BPMN end events; Flowable moves completed instances out of `ACT_RU_*` into history automatically, bounding runtime-table size to active negotiations/resources only.
- This is the central architectural fork versus stateless4j and is deliberate: it tests Flowable as a real engine, not as a reimplementation of stateless4j's model with extra steps.

### FSM shape in BPMN

Each domain state = one wait-point task (a Receive Task or User Task named after the state, e.g. `DRAFT`, `SUBMITTED`, `PAUSED`). Each such task is followed by an event-based gateway with one outgoing sequence flow per valid event from that state, gated by a correlated message event (message name = event name; correlation key = business key). Loops back to earlier states are ordinary sequence flows — BPMN's graph model handles cycles natively. This produces a 1:1, visually-inspectable mapping of the existing state graphs (`NegotiationStateMachineConfig`, `ResourceStateMachineConfig`) as hand-authored BPMN XML, not Java-generated BPMN — generating it programmatically would defeat the reason to choose BPMN at all.

### Actions

Modeled as visible **Service Task** nodes (not invisible execution listeners), placed between the message event and the next state-task, wired via `flowable:delegateExpression` to Spring-managed `JavaDelegate` beans (equivalents of `EnablePublicPostsAction`, `EnablePrivatePostsAction`, `DisablePostsAction`). Chosen so the diagram stays meaningful — hiding actions as invisible sequence-flow listener attributes would undercut the reason to evaluate BPMN's visual model in the first place.

### Guards

Two distinct guard shapes exist in current SSM behavior — verified against the actual code and git history, not assumed:

- **`NegotiationIsApprovedGuard` is confirmed dead code.** It is wired via `transitions.withExternal().guard(negotiationIsApproved())` with no `.source()`, `.event()`, or `.target()` — an incomplete transition definition. Decompiling `spring-statemachine-core-4.0.0` confirms `AbstractStateMachineFactory` only attaches a guard's `Transition` when both `source` and `target` resolve to non-null states in its internal state map; here both are `null`, so no `Transition` is ever created and the guard never runs. Git history shows this line has been carried unchanged since 2023. The real IN_PROGRESS enforcement already lives elsewhere, in `ResourceLifecycleServiceImpl`'s possible-events filtering. **This prototype reproduces that actual behavior — an application-layer check — and does not introduce a new BPMN-level guard to replace non-functioning code.**
- **The information-requirement precheck** (the one guard that is actually load-bearing today, currently a plain pre-check in `ResourceLifecycleServiceImpl.sendEvent` throwing `StateMachineException`) is modeled as a **Service Task + Exclusive Gateway** pair: the task runs the requirement check and sets a process variable; the gateway's "denied" branch throws a `BpmnError` (a single well-known error code) with no boundary catch, propagating out of the `runtimeService`/`taskService` call as an exception the lifecycle service catches and wraps into `TransitionPreconditionException`. A "silent deny" convention (denied branch loops back to the same state-task) is documented as the general pattern for any guard that should block a transition without an exception, matching stateless4j's `Guard<C>` convention ("return false for silent deny; throw for loud deny") — expressed here as two different graph shapes rather than two return behaviors.

### Security model

Same approach as the stateless4j prototype: enforcement stays in the lifecycle service's `getPossibleEvents()` (filters by source-state + role) and `sendEvent()` (throws `ForbiddenRequestException` if the event isn't in the possible set) — no Flowable guard is used for security. Security attributes are expressed as BPMN `<extensionElements>` metadata on sequence flows (a custom namespace, e.g. `<negotiator:security roles="isRepresentative"/>`), parsed from the cached `BpmnModel`, playing the same role `TransitionDescriptor.securityAttributes` plays for stateless4j.

### Typed context — accepted gap

Flowable's native `DelegateExecution`/`DelegateTask` API exposes process variables as a stringly-typed `Map<String,Object>` — the same shape of problem SSM's `Message<String>` headers have today. This prototype accepts that as-is (`execution.getVariable("negotiationId", String.class)`), matching Flowable's own idiom rather than building a wrapper type or implementing a custom `VariableType` SPI. This is a deliberate, documented gap against stateless4j's compile-time-safe typed context (user stories #4, #19), not an oversight.

### Introspection: two-layer approach

`getStateMachineDiagram()` walks the cached `BpmnModel` directly (`RepositoryService.getBpmnModel(processDefinitionId)`, internally cached by Flowable per deployed definition) — no process instance needed, matching stateless4j's "introspect the definition, not a machine instance" property. `getPossibleEvents()` needs one additional step: because "current state" is no longer a column this codebase owns, but wherever the Flowable token currently sits, it first resolves the process instance's current active activity via `RuntimeService` (a business-key lookup), then filters that activity's outgoing flows from the cached `BpmnModel` by security metadata and role match.

### Persistence and event publishing

Flowable's schema lives in the same database as the domain tables (same DataSource, auto-managed by `flowable-spring-boot-starter`), and the engine's transaction manager is the same Spring-managed `PlatformTransactionManager` already used by JPA. A `sendEvent()` call — Flowable state advance, domain entity update, audit record write, event publish — is therefore one atomic transaction, the same "no reactive `.subscribe()` boundary" improvement over SSM that the stateless4j prototype makes. Flowable's own history-capture level is configured to the minimum the engine needs functionally; the existing `*LifecycleRecord` entities remain the sole, authoritative, exposed audit trail — Flowable's `ACT_HI_*` tables are not treated as a second audit system.

### Exception handling

`org.springframework.statemachine.StateMachineException` is replaced by `TransitionPreconditionException`, thrown at the lifecycle-service seam when the information-requirement guard's `BpmnError` propagates out of the engine call. Security violations remain `ForbiddenRequestException`, thrown entirely at the application layer — Flowable is never involved in a security-related failure.

### Bean resolution and the `@Lazy` cycle — expected, not yet verified

Flowable's Spring integration resolves `delegateExpression` references via SpEL against the `ApplicationContext` at execution time (when a service task actually runs), not at process-engine build time or application-startup time. This is expected to eliminate the `@Lazy NegotiationService` workaround the same way stateless4j's request-time `BeanResolver` does — but this is a **prediction to verify by building the prototype**, not a confirmed outcome to assume going in.

### Implementation order

1. Negotiation state machine (fewer transitions, exercises the action + security + introspection pattern)
2. Resource state machine (more transitions, exercises the two-layer introspection lookup and the loud-deny guard)

### ArchUnit boundary enforcement

Inverted from the stateless4j prototype's version (which asserts a generic SPI package has zero domain imports — no such package exists here). Instead: assert that Flowable engine types (`DelegateExecution`, `DelegateTask`, `RuntimeService`, etc.) used within `lifecycle.negotiation`/`lifecycle.resource` never leak past the module boundary into `negotiation` domain services.

### Footprint

`flowable-spring-boot-starter` brings its own schema (on the order of 50 `ACT_RU_*`/`ACT_HI_*` tables) into the same database. This prototype should report the actual table count and migration size as a concrete number, not an estimate — this is one of the two reasons a prior internal analysis eliminated a full BPMN engine option, and this prototype exists specifically to confirm or refute that concern with real evidence rather than analysis alone.

## Testing Decisions

### What makes a good test for this prototype

Same standard as the stateless4j prototype: tests verify external behavior through the lifecycle service interface — the same inputs produce the same observable outputs (state changes, events published, posts created, history records, exceptions). Tests do not verify Flowable internals (which BPMN elements exist, how the process is wired).

### Primary and only seam: lifecycle service interface (integration tests)

The existing `NegotiationLifecycleServiceImplTest` is the acceptance bar, exercised the same way as for the stateless4j prototype: `sendEvent()` (state transitions, action side-effects, history records, application events), `getPossibleEvents()` (role- and state-based filtering), error cases (`ForbiddenRequestException`, `TransitionPreconditionException` in place of `StateMachineException`, `EntityNotFoundException`), and cross-machine auto-conclude coordination. Expected minimal update: `assertThrows(StateMachineException.class, ...)` → `assertThrows(TransitionPreconditionException.class, ...)`.

**Unlike the stateless4j prototype, no new fast/Spring-free unit-test layer is built.** Flowable's standalone in-memory engine (`ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()`) could in principle provide a lighter-weight seam, analogous to stateless4j's factory-plus-stubbed-`BeanResolver` unit tests, but building it is out of scope for this prototype. The full Spring Boot integration test suite is the only seam exercised, and its runtime (engine bootstrap, BPMN parsing, schema creation cost per test class) should be measured and reported as a concrete testability finding against stateless4j's pure-POJO factory unit tests, rather than assumed to be comparable.

### ArchUnit test

One test enforcing the `lifecycle.negotiation`/`lifecycle.resource` boundary: no Flowable engine types leak into `negotiation` domain services.

### Prior art

- Integration test pattern: `NegotiationLifecycleServiceImplTest` — `@IntegrationTest(loadTestData = true)`, `@RecordApplicationEvents`, `@WithMockNegotiatorUser`, `@Transactional`, Awaitility for async event consumers.

## Out of Scope

- **Configurable/data-driven state machines authored at runtime** — BPMN naturally invites this, but it is explicitly out of scope here; it's already the subject of a separate, documented initiative. This prototype evaluates Flowable purely as an SSM replacement, not as the foundation for that initiative.
- **A fast/Spring-free unit-test layer on Flowable's standalone engine** — deliberately not built (see Testing Decisions); the absence, and its measured cost, is itself one of this prototype's findings.
- **Flowable Modeler/UI or any visual process editor** — purely the embedded engine plus hand-authored BPMN XML deployed as classpath resources.
- **A typed-context wrapper over Flowable's process variables** — the stringly-typed native API is used as-is; the gap versus stateless4j is documented, not engineered around.
- **Per-resource, role-flexible guards** — same as the stateless4j prototype: requires a guard refactor plus data-model changes to `InformationRequirementRepository`, not addressed here.
- **Fixing `ResourceServiceImpl` publishing `ResourceStateChangeEvent` directly**, and **resource `sendEvent`'s silent return on unauthorized events** — both preserved for parity, same as the stateless4j prototype.
- **Codebase-wide exception refactor** — only the SM-specific `StateMachineException` is replaced.
- **Production migration of in-flight negotiations** to Flowable-backed process instances — this is a prototype for evaluation, not a production delivery.
- **Merging to main.**

## Further Notes

- The prototype branch name is `proto/state-machine-flowable`.
- This document is a deliberate structural parallel to `docs/prd-stateless4j-migration.md` — same acceptance bar, same domain vocabulary, same implementation-order rationale — so that decisions can be compared directly: called out consistently as "same as stateless4j" or "diverges because ___" throughout.
- Evaluation criteria (go/no-go), specific to this prototype: Does idiomatic persistent-instance usage work cleanly against the existing domain model, or does keeping `Negotiation.currentState` in sync with Flowable's own runtime state turn into real friction? What is the actual schema footprint (table count, migration size) added to the database? What is the actual per-test-suite runtime cost versus stateless4j's unit tests? Does the stringly-typed process-variable gap turn out to matter in practice, or is it cosmetic? Does the visual/diagram value — the core reason to consider a BPMN engine at all — show up concretely, or does it stay theoretical because nobody but developers ever opens the BPMN XML?
- Two facts established during design (not assumptions): `NegotiationIsApprovedGuard` is dead code in the current SSM implementation, verified by decompiling `spring-statemachine-core-4.0.0` and checking git history — the prototype does not reintroduce it as new, previously-nonexistent enforcement. The `@Lazy` cycle elimination is expected but explicitly unverified until the prototype is built.
- Flowable 7.x is the starting point (Spring Boot 3.x / Java 17+ baseline, compatible with this repo's Spring Boot 3.5.15 / Java 21); exact patch version to be pinned at implementation time. If the prototype reveals gaps beyond what's documented here, those become additional inputs to the eventual adoption decision, same as for stateless4j.
