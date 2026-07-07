# PRD: Migrate State Machine Implementation from Spring State Machine to stateless4j

## Problem Statement

The Negotiator relies on Spring State Machine (SSM) 4.0.0 to manage the lifecycle of Negotiations and Resources. SSM has reached end-of-life and is no longer maintained. The current implementation couples domain logic to SSM-specific concepts (PersistStateMachineHandler, SecurityRule, StateContext, message headers), making the code difficult to test in isolation, debug (reactive `.subscribe()` boundaries), and extend toward future needs (configurable/data-driven workflows, per-resource guards, auditability).

The project needs a maintained, well-understood state machine library that fits its current and future requirements. stateless4j 2.6.0 is the candidate — lightweight, plain Java, no framework coupling — with the intent of maintaining it long-term if it proves to be a good fit.

## Solution

Build a prototype on branch `proto/state-machine-stateless4j` that fully migrates both state machines (negotiation and resource) from Spring State Machine to stateless4j 2.6.0. The prototype reproduces current behavior exactly (existing integration tests as acceptance bar) while structuring the code with future-aware seams for configurable workflows, per-resource guards, and auditability.

The prototype is an exploration artifact — not intended for merge — to evaluate:
- Whether stateless4j covers current lifecycle needs
- Whether the architecture supports future data-driven workflow configuration
- Whether testability, debuggability, and maintainability improve
- How well the team can understand and maintain stateless4j itself

## User Stories

1. As a developer, I want the state machine implementation to use a library that is actively maintained or maintainable by our team, so that we are not blocked by upstream EOL.
2. As a developer, I want lifecycle transition code that can be unit-tested without a Spring context, so that test feedback is fast and failures are easy to diagnose.
3. As a developer, I want a synchronous, steppable transition path (no reactive `.subscribe()` boundaries), so that I can debug lifecycle transitions with a standard debugger.
4. As a developer, I want typed context objects (not message headers with string keys) passed to actions and guards, so that compile-time safety prevents wiring errors.
5. As a developer, I want the state machine configuration expressed as introspectable data (StateMachineDefinition), so that `getPossibleEvents` and `getStateMachineDiagram` can be computed without building a machine instance.
6. As a developer, I want security attributes on transitions expressed as descriptor metadata (not SSM SecurityRule objects), so that the existing role-based filtering in `getPossibleEvents` continues to work identically.
7. As a developer, I want the `@Lazy` circular dependency workaround (action beans → NegotiationService → lifecycle service → action beans) eliminated, so that the dependency graph is clean and startup failures are easier to diagnose.
8. As a developer, I want the state machine core (SPI + factory) to have zero domain imports, so that it can be reasoned about independently and potentially reused.
9. As a developer, I want the architecture to leave a clear seam where a future data-driven configuration layer would attach, so that configurable workflows don't require rearchitecting.
10. As a developer, I want the architecture to leave a clear seam where per-resource, role-flexible guards (information requirements) would attach, so that that follow-up is a small incremental step.
11. As a developer, I want lifecycle audit records (NegotiationLifecycleRecord, NegotiationResourceLifecycleRecord) to continue being written with the same shape and timing as today, so that the timeline UI is unaffected.
12. As a developer, I want ApplicationEvents (NegotiationStateChangeEvent, ResourceStateChangeEvent) to continue being published on state transitions, so that webhook and notification handlers are unaffected.
13. As a developer, I want the cross-machine auto-conclude logic (all resources terminal → negotiation concludes) to continue working, so that the existing behavior is preserved.
14. As a developer, I want transitions that fail business-rule preconditions to throw a domain-named exception (not a library exception), so that exception semantics are clear and HTTP mapping lives at the boundary.
15. As a developer, I want the `getStateMachineDiagram` REST endpoint to return the same response shape, so that any consumers are unaffected.
16. As a developer, I want debug logging at key points (factory build, event fire, transition outcome, persist) with structured context, so that production issues are diagnosable from logs alone.
17. As a developer evaluating stateless4j, I want to understand where stateless4j's model falls short of our needs, so that we can make an informed go/no-go decision on long-term adoption.

## Implementation Decisions

### Module structure

One Spring Modulith module named `lifecycle`, replacing today's `negotiation.state_machine.*` packages. Three internal packages:

- `lifecycle.statemachine` — generic SPI + factory. Zero domain imports, enforced by an ArchUnit test. Contains: `TransitionContext` (marker interface), `Guard<C>`, `TransitionAction<C>`, `TransitionListener<C>`, `TransitionOutcome<C>` (record), `TransitionDescriptor` (record), `StateMachineDefinition` (record), `StateMachineFactory<C>`, `BeanResolver` (functional interface).
- `lifecycle.negotiation` — negotiation state machine definition, integration beans (actions, persist listener), lifecycle service, controller, cross-machine auto-conclude listener.
- `lifecycle.resource` — resource state machine definition, integration beans (persist listener), lifecycle service, controller.

The module root (`lifecycle`) holds shared types: `NegotiatorTransitionContext` (sealed interface), `TransitionPreconditionException`, `SpringBeanResolver`.

### What stays in the `negotiation` module (not moved to `lifecycle`)

Domain vocabulary types remain in `negotiation` to avoid circular module dependencies:
- Enums: `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent`, `NegotiationResourceEvent`
- ApplicationEvents: `NegotiationStateChangeEvent`, `ResourceStateChangeEvent`
- Audit entities: `NegotiationLifecycleRecord`, `NegotiationResourceLifecycleRecord`

Dependency direction: `lifecycle` → `negotiation` (one-way). The `negotiation` module does not depend on `lifecycle`.

### Instance model: factory + per-entity machines

No singleton state machine bean. A `StateMachineFactory<C>` builds a fresh stateless4j `StateMachine` instance per transition request, configured from the immutable `StateMachineDefinition`, initialized to the entity's current state from DB. The machine is disposable after `fire()`.

This eliminates SSM's PersistStateMachineHandler pattern (singleton + state reset) and aligns with stateless4j's design.

### Typed context replaces message headers

A sealed interface hierarchy replaces SSM's `Message<String>` with typed headers:

```java
// lifecycle.statemachine (core — marker)
public interface TransitionContext {}

// lifecycle (module root — sealed domain hierarchy)
sealed interface NegotiatorTransitionContext extends TransitionContext
    permits NegotiationTransitionContext, ResourceTransitionContext {
  String negotiationId();
  Set<String> roles();
}

// lifecycle.negotiation
record NegotiationTransitionContext(String negotiationId, Set<String> roles,
    String postBody, Long senderId) implements NegotiatorTransitionContext {}

// lifecycle.resource
record ResourceTransitionContext(String negotiationId, Set<String> roles,
    String resourceId) implements NegotiatorTransitionContext {}
```

The factory closes over the context when wiring guards/actions as lambdas. Actions/guards receive the typed context — no `message.getHeaders().get("negotiationId", String.class)` pattern.

### Security model: descriptor metadata, not guards

SSM's `.secured("ROLE_ADMIN")` / `.secured("isAdmin"/"isRepresentative"/"isCreator")` become `securityAttributes: Set<String>` on `TransitionDescriptor`. Enforcement stays in the lifecycle service's `getPossibleEvents()` (filters descriptors by source-state + user-roles match) and `sendEvent()` (throws `ForbiddenRequestException` if event not in possible set). No stateless4j guard is used for security.

The `IN_PROGRESS` check for resources stays as a discovery-time filter in the orchestrator, not a guard.

### Guards: SPI present, zero wired in prototype

`Guard<C>` interface exists in `lifecycle.statemachine`. The `TransitionDescriptor` has an optional `guardName` field. The factory resolves and wires guards when present. In this prototype, no guard beans are wired — the information-requirement check stays as an orchestrator pre-check (behavioral parity). Moving it to a guard bean is a documented follow-up.

Guard convention: return `false` for silent deny; throw `TransitionPreconditionException` for loud deny with user-facing message.

### Persistence and event publishing

The lifecycle service method is `@Transactional`. The `TransitionListener<C>` implementation (persist listener) is invoked synchronously by the factory's `onTransition` hook, within the caller's transaction. This replaces SSM's reactive `handleEventWithStateReactively(...).subscribe()` — an improvement: errors propagate, transaction boundary is honored, no race conditions.

The persist listener calls domain entity methods (`negotiation.setCurrentState(...)`, `negotiation.setStateForResource(...)`) which append `*LifecycleRecord` audit rows as today. The listener then publishes `NegotiationStateChangeEvent` / `ResourceStateChangeEvent` via `ApplicationEventPublisher`.

### Introspection: definition, not machine

`getPossibleEvents()` and `getStateMachineDiagram()` introspect the `StateMachineDefinition`'s `TransitionDescriptor` list directly. No machine instance is built for read operations. This is simpler and cheaper than today's `stateMachine.getTransitions()` traversal.

### Exception handling

`org.springframework.statemachine.StateMachineException` is replaced by `TransitionPreconditionException` (in `lifecycle` module root). Caught by `NegotiatorExceptionHandler` → HTTP 400. The single throw site (information-requirement pre-check) uses `TransitionPreconditionException` with the same message. Security violations use the existing `ForbiddenRequestException` (→ 403).

### Bean resolution and the `@Lazy` cycle

The factory resolves guard/action beans by name via a `BeanResolver` (functional interface implemented by `SpringBeanResolver` wrapping `ApplicationContext`). Resolution happens at request time (inside `factory.build()`), not at construction time. This eliminates the `@Lazy NegotiationService` workaround in action beans — the lifecycle service depends on the generic factory, which holds no domain refs; action beans depend on `NegotiationService`; no cycle.

### Implementation order

1. Negotiation state machine (fewer transitions, exercises action + security pattern)
2. Resource state machine (more transitions, adds diagram endpoint, custom security attributes)

### ArchUnit boundary enforcement

One test asserting `lifecycle.statemachine` package depends only on itself, stateless4j, JDK, Spring core, and Lombok — no `eu.bbmri_eric.negotiator.*` domain imports. ArchUnit is available on the test classpath transitively via `spring-modulith-starter-test`.

### stateless4j dependency

Published Maven artifact `com.github.stateless4j:stateless4j:2.6.0`. No fork for the prototype. Java 21 compatible (stateless4j is Java 8+, zero transitive dependencies that conflict with Spring Boot 3.5).

## Testing Decisions

### What makes a good test for this prototype

Tests verify external behavior through the lifecycle service interface — the same inputs produce the same observable outputs (state changes, events published, posts created, history records, exceptions). Tests do NOT verify stateless4j internals (which transitions are configured, how the factory wires them). The acceptance criterion is: existing tests pass against the new implementation.

### Primary seam: lifecycle service interface (integration tests)

The existing `NegotiationLifecycleServiceImplTest` is the acceptance bar. It exercises:
- `sendEvent()` — state transitions, action side-effects (posts created, post settings enabled/disabled), history records appended, application events published
- `getPossibleEvents()` — role-based filtering, state-based filtering
- Error cases — unauthorized events throw `ForbiddenRequestException`, unmet info-requirements throw (now `TransitionPreconditionException` instead of `StateMachineException`), entity-not-found throws `EntityNotFoundException`
- Cross-machine coordination — resource state changes trigger negotiation auto-conclude

The test must be updated minimally: `assertThrows(StateMachineException.class, ...)` → `assertThrows(TransitionPreconditionException.class, ...)`. No other assertion changes expected.

### New seam: factory unit tests (no Spring context)

The `StateMachineFactory` + `StateMachineDefinition` enable a new unit-test layer that doesn't exist today:
- Build a definition with known transitions
- Provide a stubbed `BeanResolver` returning mock guard/action/listener beans
- Construct a context, fire an event, assert on outcome (new state, listener invoked, action called)
- Verify that undefined transitions don't fire, guards gate transitions, actions execute with correct context

This validates the testability improvement that motivates the migration.

### ArchUnit test

One test enforcing the `statemachine` package boundary (no domain imports). Pattern: `classes().that().resideInAPackage("..statemachine..").should().onlyDependOnClassesThat()...`.

### Prior art

- Integration test pattern: `NegotiationLifecycleServiceImplTest` — `@IntegrationTest(loadTestData = true)`, `@RecordApplicationEvents`, `@WithMockNegotiatorUser`, `@Transactional`, Awaitility for async event consumers.
- Unit test pattern: `NegotiationLifecycleRecordTest`, `NegotiationResourceLifecycleRecordTest` — plain JUnit, no Spring context.

## Out of Scope

- **Configurable/data-driven state machines** — the architecture leaves the seam (StateMachineDefinition is pure data, could be loaded from DB), but no DB-backed configuration loader is built.
- **Persisted state machine configurations** — no schema, no admin UI, no CRUD for workflow definitions.
- **Shared lifecycle event vocabulary (LCD across resource workflows)** — noted as a future need, not addressed.
- **Information requirements as guard beans** — the Guard SPI exists but is unused; the info-requirement check remains an orchestrator pre-check. Moving it to a guard is the immediate follow-up.
- **Per-resource, role-flexible guards** — requires the guard refactor above plus data model changes to `InformationRequirementRepository`.
- **Cleaner audit reimplementation** — existing audit records (LifecycleRecord entities written by domain setters) are preserved as-is. A future separate audit listener consuming `TransitionOutcome` is the obvious upgrade path.
- **Fixing `ResourceServiceImpl` publishing `ResourceStateChangeEvent` directly** — layer violation preserved for parity.
- **Fixing resource `sendEvent` silently returning on unauthorized event** — behavior change deferred; prototype preserves today's inconsistency between negotiation (throws) and resource (silent return).
- **Codebase-wide exception refactor** — only the SM-specific `StateMachineException` is replaced. The broader pattern of HTTP-named exceptions in services is not addressed.
- **Merging to main** — this is a prototype branch for evaluation, not a production delivery.

## Further Notes

- The prototype branch name is `proto/state-machine-stateless4j`.
- The evaluation criteria for go/no-go on long-term adoption: Does stateless4j cover current behavior? Is the code clearer and more testable? Are the future seams (configurable workflows, guards, audit) realistic? Are there gaps where stateless4j's model doesn't fit? The prototype should surface these concretely.
- stateless4j 2.6.0 is the starting point. If the prototype reveals gaps (e.g., missing hooks, inadequate introspection API), those become inputs to the fork/maintain decision.
- The `ResourceStateChangeListener` (cross-machine auto-conclude) is an existing example of inter-machine coordination. It moves to `lifecycle.negotiation` and continues using Spring `ApplicationEvent` for decoupling. Future "shared lifecycle events for alignment" (common event vocabulary across resource workflow configurations) is a separate, larger concern.
