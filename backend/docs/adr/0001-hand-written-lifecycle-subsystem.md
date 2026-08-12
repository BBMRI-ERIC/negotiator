---
status: accepted
---

# 0001 — Replace Spring Statemachine with a hand-written lifecycle subsystem

_Source tickets: [Engine landscape research](../../../.scratch/state-machine-redesign/issues/02-engine-landscape-research.md), [Engine choice](../../../.scratch/state-machine-redesign/issues/03-engine-choice.md)._
_Implementation is follow-on work — nothing here is built yet, and Spring Statemachine is still in place in the code._

Both Lifecycles run today on Spring Statemachine, configured in Java by `NegotiationStateMachineConfig` and `ResourceStateMachineConfig`. Spring Statemachine is end-of-life: the project is archived to `spring-attic`, Maven Central is frozen at `4.0.0` (Dec 2023), and future releases are commercial-only. Staying is not an option, and a 2026 survey of the landscape found nothing to move to.

**We write the whole lifecycle subsystem ourselves rather than adopting a library.** What gets hand-written is more than the Transition Evaluator: the persisted definition model, the strategy catalogue for Guards and Actions, the admin CRUD surface over all of it, and the orchestration around commits — with the evaluator at the centre as the one piece a library could plausibly have supplied. That is the crux of the choice. The definition model this redesign commits to is a bespoke relational schema plus a named strategy catalogue; no surveyed library reads such a schema, so adopting one means bending its execution model to ours — and it would cover only the cheap part, the evaluation loop, while everything around it dominates the cost regardless of what sits underneath. A hand-written, data-driven implementation is sized at roughly **2,500–4,000 LOC** against today's 1,493 across 25 files: 1.5–2.5×, for a subsystem we then own outright.

**One evaluator core serves both Definition Scopes.** A single scope-parameterized component replaces both configuration classes. The Negotiation and Resource lifecycle services stay separate — they own different JPA entities and different persistence wiring — but they call one shared evaluation path, so the two Lifecycles can never drift in how a move is judged.

**The evaluator is stateless and does no I/O of its own.** It is handed an already-materialized definition graph plus the current State and the Event, and answers what is permitted; committing the move, running Actions and writing history belong to the services around it. This is a deliberate constraint rather than a performance claim: an evaluator that structurally cannot query the database makes loading the definition graph an explicit, testable step. The compiled graph is cached in memory per Definition Version and invalidated only when a new version is published, which is safe because a version is immutable once active. An engine that owned its own persistence would have no such forcing function and would invite an N+1 across a negotiation's resources, discovered only under load.

**Spring Statemachine is deleted entirely** — the `spring-statemachine-*` dependency leaves the POM, and all ~25 classes (both configs, the Guard and Action classes, persist handlers, converters, listeners) are replaced. There is no period in which both engines run.

## Considered Options

- **Adopt an FSM library.** Every named candidate other than one is dormant or zombie-maintained (stateless4j, Squirrel Foundation, EasyFlow, Apache Commons SCXML). The single actively-maintained entrant, `jd-easyflow` (Apache-2.0, JD.com), has low adoption and single-corporate-sponsor bus-factor risk — a worse position than the one we are leaving.
- **Adopt a BPMN process engine.** Camunda 7 is archived; Camunda 8's core requires a paid licence for production use under the source-available Camunda License 1.0. Flowable is the cleanest fully-Apache-2.0, actively-maintained option, but it is a full BPMN/CMMN/DMN engine — disproportionate to a single-entity Lifecycle, and it brings its own transactional store to reconcile against our domain entities.
- **Keep Spring Statemachine.** Frozen upstream; no security or compatibility floor going forward.

This ADR does not fix the definition schema (0002), what identifies a Definition Version (0003), or the order in which a move is gated (0005).
