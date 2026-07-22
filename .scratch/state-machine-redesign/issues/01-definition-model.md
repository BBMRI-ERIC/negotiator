# Definition model

Type: grilling
Status: resolved

## Question

What must a persisted state-machine *definition* express in order to replace the current hardcoded Spring Statemachine config classes (`NegotiationStateMachineConfig`, `ResourceStateMachineConfig`)?

This ticket must settle, for both the Resource-level lifecycle (which becomes network-associable/configurable per the map's destination) and the Negotiation-level lifecycle (which stays a single global definition, just externalized from Java — folded into this same ticket rather than split out):

- States and events/transitions as data, not enums baked into Java.
- Guards (today: `NegotiationIsApprovedGuard`) — how are they expressed and evaluated against domain state when the definition itself is just data?
- Actions (today: `EnablePublicPostsAction`, `EnablePrivatePostsAction`, `DisablePostsAction`) — same question.
- Security/role requirements (today: `.secured("ROLE_ADMIN", ...)` on the Negotiation machine, `.secured("isAdmin"/"isRepresentative"/"isCreator", ...)` on the Resource machine).
- Attachment points for information requirements — ticket 05 designs the requirements model itself, but this ticket must define *where* on a transition/event a requirement attaches.
- A concrete persistence shape (relational tables vs. a JSON/DSL blob vs. something else) sufficient for the engine (ticket 03's decision) to load and execute.
- Whether/how the current dead enum values (`NegotiationState.APPROVED`, `NegotiationEvent.START`, unused `NegotiationResourceEvent` values) get resolved as part of the port — drop, wire up, or explicitly deprecate.

Reference: current implementation in `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/` (see the map's Notes for the full survey). This is the foundational ticket — tickets 03, 04, 05, and 06 all block on it (03 additionally blocks on ticket 02).

## Answer

**Schema & scope**: One unified `StateMachineDefinition` schema for both lifecycles.
- `StateMachineDefinition`: id, name, scope (`NEGOTIATION`|`RESOURCE`), version, active flag. Negotiation scope has exactly one active row (uniqueness constraint) and no Network/Resource association; Resource scope's association mechanics are ticket 04's job.
- `State`: id, definition_id (FK), key, label, description, is_initial.
- `Event`: id, definition_id (FK), key, label, description.
- `Transition`: id, definition_id (FK), source_state_id (FK), target_state_id (FK), event_id (FK), required_authority (enum, see below), plus ordered guard/action references (separate guard-wiring and action-wiring tables — see the wiring-table amendment below).

**Guards & actions**: Named registry pattern. A fixed set of Java-implemented `GuardType`/`ActionType` strategies, referenced by key from transition join rows with optional JSON params (e.g., which state to compare, which visibility flag to set). New logic requires a new Java class + registry entry; *wiring* (which transition uses which guard/action, in what order) is pure data.

**Security/authority**: A distinct `required_authority` field on `Transition` (not folded into the guard list), drawn from a small fixed registry: `NONE`, `IS_ADMIN` (collapsing today's `ROLE_ADMIN` and `isAdmin` — confirmed identical, both check the `"ROLE_ADMIN"` granted authority via `AuthenticatedUserContext`), `IS_CREATOR`, `IS_REPRESENTATIVE`.

**Info-requirement attachment point**: Requirements attach to the `Event`, not the `Transition` — preserves today's semantics (a requirement applies wherever that event fires, regardless of source/target state, matching `InformationRequirement.forEvent`'s current behavior). Ticket 05 designs the requirement model itself on top of this attachment point.

**Persistence shape**: Relational tables (not a JSON/DSL blob) — normalized rows with FKs, enabling indexed "what transitions exist from state X for event Y" queries and FK-enforced admin CRUD.

**Dead enum cleanup**: Drop `NegotiationState.APPROVED`, `NegotiationEvent.START`, `NegotiationResourceEvent.RETURN_FOR_RESUBMISSION`, `NegotiationResourceState.RETURNED_FOR_RESUBMISSION` — confirmed unreachable and unreferenced anywhere in code today (verified via repo-wide grep).

**OVERRIDE**: `NegotiationResourceEvent.OVERRIDE` is *not* dead — `ResourceServiceImpl.updateResourceStatus()` uses it to tag an admin's direct state override (which bypasses the transition graph) for audit purposes. Preserve it as a transition-less admin-bypass tag on `Event` if that's cheap under whichever engine ticket 03 picks. This is not a hard model requirement: if the chosen engine wants every event to have ≥1 transition, refactor the tagging out of the `Event` concept instead (e.g. a bare reason-code string on the audit record) rather than contorting the schema to allow orphan events.

**Explicitly deferred to ticket 04**: versioning mechanics (how an edited definition interacts with in-flight negotiations/resources already running the old one).

**Amendment ([Definition version/identity reconciliation](08-version-identity-reconciliation.md))**: the `version` field is a per-family, system-assigned monotonically incrementing sequence number (unique on `(family_key, version)`, assigned at row creation) with a display/audit role only — identity and all FKs stay on the row id.

**Amendment (guard/action wiring-table shape — resolves the ordering review finding)**: guards and actions get **two separate wiring tables**, not one polymorphic table. They never interleave at runtime (guards run pre-commit in `evaluate()`, actions run only after a transition succeeds), so a shared ordering would be meaningless, and ticket 07 gave guards a scope column actions don't have.

- **Guard wiring** — one table spanning both of ticket 07's scopes via a **nullable `transition_id`**: `id`, `definition_id` (always set), `transition_id` (nullable — null = machine-level/definition-wide guard, e.g. the promoted `NEGOTIATION_APPROVED`; set = transition-specific), `guard_type` (string key), `params` (jsonb), `order`. Per-scope order-uniqueness via two partial unique indexes — `(transition_id, "order") WHERE transition_id IS NOT NULL` and `(definition_id, "order") WHERE transition_id IS NULL`. The backend is PostgreSQL-only (Flyway + Testcontainers, no H2), so partial indexes carry no portability caveat. The "effective guard chain for transition X" ticket 07 asks admin tooling to show is one query: `WHERE definition_id = ? AND (transition_id IS NULL OR transition_id = ?)`.
- **Action wiring** — transition-scoped only (ticket 07 left actions untouched): `id`, `transition_id` (non-null FK), `action_type` (string key), `params` (jsonb), `order`, `UNIQUE (transition_id, "order")`. No `definition_id` (derivable through the transition).
- **Ordering is independent per table/scope.** The `order` columns sequence *within* a scope only. Cross-scope evaluation order (machine-level guards before transition guards; the pipeline `required_authority → built-in requirements check → guards`) is engine pipeline logic from ticket 07, **not** encoded in any `order` column.
- **Realization: strategy-bean registry, not enum-holds-class.** `guard_type`/`action_type` are plain string keys, not a Java enum carrying param classes. Each type is a self-describing Spring `@Component` strategy declaring its own key and its own params DTO type (`Class<P>`) — mirroring the existing `WebhookEventMapper` registry (inject `List<Strategy>`, fold to `Map<key, strategy>`, throw at startup on duplicate keys). The registry deserializes each row's `params` into the strategy's declared type at load time (the one unchecked bridge, in a single place). `params` persists as a `String`/jsonb column via `@Type(JsonType.class)` — already the idiom for `InformationSubmission.payload` — opaque to the schema, owned by the strategy. Runtime domain state (negotiation id, current user) comes from the evaluation context at fire time, never from `params`.
- **Actions keep `params`** (symmetric with guards). Example: one `SET_POST_VISIBILITY` action type with `{"scope":"PUBLIC","enabled":true}` / `{"scope":"PRIVATE","enabled":true}` / `{"scope":"ALL","enabled":false}` collapses today's three hardcoded post classes (`EnablePublicPostsAction`, `EnablePrivatePostsAction`, `DisablePostsAction`). The typed-params registry has to exist for guards regardless, so giving actions params adds no machinery — and divergent shapes for two structurally identical wiring tables would be its own small tax. (Params-*validation* timing — save-time vs. fire-time — left as implementation detail, out of scope for this finding.)

## Review findings (unresolved)

<!-- Surfaced by a wayfinder-map consistency review on 2026-07-16. Unchecked = not yet reconciled; pick up in a fresh session. -->

- [x] **Guard-chain wiring mechanism for Event-attached Requirements is unspecified.** This ticket wires guards/actions onto `Transition` via explicit per-row join entries ("wiring... is pure data"). Ticket 05 then attaches `Requirement` to `Event` and makes requirement-satisfaction "a registry `GuardType` entry... evaluated inside the same guard chain as any other guard." Neither ticket says how an Event's Requirements automatically populate the guard-chain join rows of every Transition that fires that event — if it requires manually adding the join row per transition, a missed one silently breaks the "applies wherever this event fires" guarantee this ticket gave as its reason for attaching to Event over Transition. Needs a mechanism decision (likely as a ticket 05 amendment, or a new short ticket). — *Resolved by [Event-requirement guard-chain wiring mechanism](07-event-requirement-guard-wiring.md): built-in engine stage, no wiring rows; guard wiring gains a machine-level scope (see Answer amendment above).*
- [x] **`version` field never reconciled with ticket 04's `family_key`.** This ticket gives `StateMachineDefinition` a bare `version` field with no stated semantics beyond existing. Ticket 04 later introduces `family_key` as the actual versioning/lineage identity, one row per version. It's never stated whether `version` becomes a per-family incrementing sequence number, is dropped, or is now redundant with row identity. — *Resolved by [Definition version/identity reconciliation](08-version-identity-reconciliation.md): per-family display sequence, no identity role (see Answer amendment above).*
- [x] **Ambiguous ordering on Transition's guard/action join rows.** "plus ordered guard/action references (join rows: type key from the registry + JSON params + order)" doesn't say whether guards and actions share one interleaved ordering or have independent per-type orderings — leaves the join-table shape (one polymorphic table vs. two) unresolved. — *Resolved (see the wiring-table amendment in the Answer): two separate tables, independent per-scope ordering. Guard wiring is one table with a nullable `transition_id` carrying ticket 07's machine/transition scopes (partial unique indexes per scope); action wiring is transition-only. Both realized via a `WebhookEventMapper`-style strategy-bean registry with string type-keys and per-strategy typed `params` (jsonb via `@Type(JsonType.class)`); actions keep `params`. Cross-scope order stays engine pipeline logic, not an `order` column.*
