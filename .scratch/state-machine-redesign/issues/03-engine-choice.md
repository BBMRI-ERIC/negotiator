# Engine choice

Type: grilling

Blocked by: 01, 02

Status: resolved

## Question

Given the definition-model requirements from ticket 01 and the landscape research from ticket 02: hand-rolled FSM, or adopt/maintain a small existing library? Lock the choice and the top-level reasoning (what tipped it), sized so a follow-on implementation effort can act on it directly.

## Answer

**Choice: hand-rolled.** `jd-easyflow` is the only actively-maintained library candidate, but it's low-adoption and single-corporate-sponsor risk, and the definition model from ticket 01 already requires a bespoke persisted schema (relational `StateMachineDefinition`/`State`/`Event`/`Transition`) plus a custom guard/action registry with no library equivalent — adopting a library would mean bending its execution model to fit our schema, for a library covering only the cheap part (the evaluation loop), not the data model or admin layer that dominates the LOC estimate regardless of engine choice.

**Architecture: one shared engine core.** A single generic `StateMachineEngine` (scope/definition-parameterized, not duplicated per Java type) replaces both `NegotiationStateMachineConfig` and `ResourceStateMachineConfig`. The Negotiation and Resource lifecycle services stay separate (they operate on different JPA entities and have different persistence/listener wiring), but the transition-evaluation core is shared.

**Execution model: stateless pure evaluator.** The engine holds no state and does no DB I/O of its own — it's handed an already-materialized definition graph plus current state + event, and returns allowed/next-state/ordered actions. The definition graph is cached in memory keyed by `(definition_id, version)`, compiled once and invalidated only on new-version publish (definitions are immutable once active, per ticket 04). The lifecycle services remain the transactional/persistence boundary, calling the engine from within their existing `@Transactional` methods — same shape as today's `sendEvent()`, with the engine slotted in where Spring Statemachine used to sit.

Checked against a concrete scenario (a negotiation with 300 resources spanning 3 network-wired definitions): today's engine is already a startup-built singleton, not reconstructed per resource — so "from-scratch construction" isn't a cost either option introduces. The real cost axis is definition-graph *loading*, which is orthogonal to statelessness: either design could reload the graph from the DB per call (N+1 across 300 resources) or work against a cached compiled graph. The difference is risk surface, not raw performance — a stateless evaluator structurally cannot do its own DB I/O, so caching the compiled graph becomes an explicit, testable step rather than an easy-to-miss optimization; an engine-owns-persistence design has no such forcing function and invites an N+1 discovered only under load. Engine-owns-persistence would only pay off if the engine needed to reconcile its own separate transactional store against the domain entities — exactly the workflow-engine shape (Flowable/Camunda) already ruled out as too heavy.

Flagged as follow-on implementation concerns, not blocking this decision: guard evaluation may itself hit the DB per resource (cost inherent to what a guard checks, not eliminated by graph caching); bulk-transaction granularity (300 individual `@Transactional` calls vs. one batch transaction) is a lifecycle-service-level decision.

**Spring Statemachine disposal: deleted entirely.** The `spring-statemachine-*` dependency is removed from the POM, and all ~25 existing classes (`NegotiationStateMachineConfig`, `ResourceStateMachineConfig`, guard/action classes, persist-handlers, converters, listeners) are replaced by the new engine + definition model + registry.

**`OVERRIDE` resolution (deferred by ticket 01 to this ticket):** kept as ticket 01's default — a transition-less `Event` row tagging an admin's direct state override for audit purposes. Hand-rolling removes the "every event needs ≥1 transition" pressure that an adopted library's execution model might have imposed, so there's no cost to keeping it as-is.

**Amendment ([Definition version/identity reconciliation](08-version-identity-reconciliation.md))**: the definition-graph cache is keyed by the definition version row's id alone — the same value ticket 04's `definition_version_id` pins — replacing this Answer's two-axis `(definition_id, version)` wording; `version` is a display-only per-family sequence per ticket 08.

**Explicitly out of scope here:** how information-requirement satisfaction plugs into the engine's evaluation (guard-registry entry vs. separate check), and the API contract for surfacing "possible next events + their requirement status" together — today's `getPossibleEvents()` ignores info-requirements entirely while `sendEvent()` gates on them separately, producing a UX bug (an event displays as available/clickable but fails on submission if its requirement isn't met). Ticket 05 (Information requirements as first-class model) owns both the engine-integration mechanics and that contract/UX fix.

## Review findings (unresolved)

<!-- Surfaced by a wayfinder-map consistency review on 2026-07-16. Unchecked = not yet reconciled; pick up in a fresh session. -->

- [x] **Misattribution: "definitions are immutable once active, per ticket 01."** Ticket 01 never says this — its only versioning statement is that mechanics are "explicitly deferred to ticket 04." The immutable-once-active rule is actually established by ticket 04 ("exactly one row per family is `active` at a time"). Fix the citation (ticket 04 also repeats this same misattribution). — *Fixed in both tickets.*
- [x] **`(definition_id, version)` cache key is incoherent with ticket 04's family model.** Ticket 04 makes each version its own row, with `family_key` as the stable lineage identity and each row already uniquely identified by its own id. It's unclear whether this cache key's "`definition_id`" means `family_key` or the specific version row's id — if the latter, pairing it with a separate "version" is redundant. Needs reconciling with ticket 04's actual schema (and with ticket 01's still-unexplained plain `version` field). — *Resolved by [Definition version/identity reconciliation](08-version-identity-reconciliation.md): cache keyed by the version row's id alone (see Answer amendment above).*
