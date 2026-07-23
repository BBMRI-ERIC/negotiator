# Lifecycle State-Machine Redesign — Design Summary

**Status:** Draft for early comment · 8 of 10 design decisions locked, 2 open
**Scope:** Design/spec only — **no code is delivered by this effort.** Implementation happens in separate follow-on work.
**Date:** 2026-07-23
**Purpose of this doc:** (1) get early comments on the decisions so far, (2) report where the effort stands.

> This is a synthesis of a decision map worked ticket-by-ticket. Each section links back to the ticket that holds the full reasoning; the map lives at `.scratch/state-machine-redesign/map.md`.

---

## TL;DR — what this unlocks

We're replacing the two hardcoded Spring Statemachine (EOL) lifecycles with **one configurable, data-driven engine**. In feature terms, that means:

- **Lifecycles become editable data, not code.** States, events, transitions, guards, and info-requirements live in the database. Changing a flow is a configuration task for an admin, not code changes and deployment.
- **Per-Network (and per-Resource) Resource lifecycles.** A Resource's lifecycle can be assigned directly or inherited from its Network(s), with a global default fallback — so different Networks can run genuinely different flows. The Negotiation lifecycle stays a single global flow, but is externalized from Java too.
- **Versioning that never breaks in-flight work.** Lifecycles are versioned families; a negotiation is pinned to the version it started on and runs it to completion, even after a newer version is published.
- **Information requirements are first-class — for both lifecycles.** Configure "you must submit form X before event Y" as data, now on the Negotiation lifecycle too (today it's Resource-only, enforced ad-hoc). Requirements are checked automatically wherever an event fires — they can't be forgotten by misconfiguration.
- **Consistent, declarative access control.** Each transition declares who may fire it (`NONE` / admin / creator / representative), evaluated in one pipeline alongside guards and requirements.
- **Clean-up.** Spring Statemachine and ~25 classes deleted; dead states/events dropped; three hardcoded action classes collapse into one configurable action.

**Still being designed:** how the Negotiation and Resource lifecycles coordinate (auto-conclude when all resources finish), and *who* gets asked for information / *how many* must respond / *how they're notified*. See §4.

---

## 1. Why we're doing this

The lifecycle state machines today are **hardcoded Java**: two Spring Statemachine singleton beans (`NegotiationStateMachineConfig`, `ResourceStateMachineConfig`) in `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/`, with states/events baked in as enums. This has three problems:

- **Spring Statemachine is end-of-life**, and no other off-the-shelf FSM library is a good fit (see §3.1).
- **Lifecycles can't be configured.** We want the Resource lifecycle to be data — associable to a Resource directly or via its Network(s) — and the Negotiation lifecycle externalized from Java (still a single global definition).
- **Enum/config drift** has already crept in (dead states/events, ad-hoc info-requirement enforcement, a double-enforcement bug in the possible-events API).

### Destination

A complete design spec (decisions + ADRs) for a replacement lifecycle state-machine subsystem, covering:

- **(a) the engine** — replace Spring Statemachine, chosen from a clean slate;
- **(b) a persistence model** for Resource-lifecycle *definitions* as configurable data (states / events / transitions / guards / info-requirements), associable with a Resource directly or via its Network(s); the Negotiation lifecycle keeps a single externalized definition;
- **(c) information requirements** as a first-class part of the definition model, for **both** lifecycles (today they only gate Resource transitions);
- **(d) architectural clean-ups** that fall out along the way.

REST API / vocabulary compatibility is preferred but not a hard constraint.

---

## 2. Feature ↔ requirement matrix

Traceability from delivered **features** (rows) to the destination's four **requirements** (columns, §1):

- **(a) Engine** — replace Spring Statemachine, clean slate
- **(b) Configurable & associable definitions** — lifecycles as data, associable per Resource / Network
- **(c) Info-requirements first-class** — for both Negotiation *and* Resource lifecycles
- **(d) Architectural clean-up** — resolve the enum/config drift

Cells give a short note where the relationship carries nuance; otherwise **✓** = related, **—** = not related.

| Feature | Rationale (why) | (a) Engine | (b) Configurable & associable | (c) IR first-class | (d) Clean-up |
|---|---|---|---|---|---|
| **Data-driven definition model** (states/events/transitions as rows) | Edit a flow without a code change/deploy | Graph the engine evaluates | **Directly satisfies** | Requirements attach to its `Event` | Replaces baked-in enums |
| **Per-Network / per-Resource association + global default** | Different Networks can run different flows | — | **Directly satisfies** (the *associable* half) | — | — |
| **Versioned families + pin-at-start** | New versions never disrupt in-flight negotiations | Cache keyed by version-row id | Makes editing safe | — | — |
| **Info-requirements as a first-class model** (both lifecycles) | Config-driven mandatory forms, now on Negotiation too | Enforced as a built-in eval stage | Attaches to `Event` in the model | **Directly satisfies** | Replaces ad-hoc `sendEvent()` enforcement |
| **Declarative access control** (`required_authority` per transition) | Who may fire a transition becomes data | First stage of the eval pipeline | Field on `Transition` | — | Collapses duplicate `ROLE_ADMIN`/`isAdmin` |
| **Hand-rolled stateless engine core** | No library fits; SSM is EOL | **Directly satisfies** | Consumes the definition graph | Runs the requirements stage | Removes SSM dependency |
| **Guard/action strategy registry** (typed `params`) | New logic = one class; wiring stays data | Engine invokes the strategies | Wiring persisted as rows | Shares the `GuardResult` contract | 3 hardcoded action classes → 1 configurable |
| **Single evaluation pipeline** (possible-events = real gate) | "Possible events" never advertises an event that will fail | One engine `evaluate()` | — | IR check is a pipeline stage | Fixes dead-click + double-enforcement |
| **Audit/history via `state_id` FK** | History stays data-driven, minimal change | — (read-side, not the engine) | References `State` rows in the model | — | Removes enum coupling in audit tables |
| **SSM removal + dead-enum drop** | Delete an EOL dependency and accumulated drift | Enabled by the hand-rolled engine | — | — | **Directly satisfies** |

---

## 3. Decisions

Eight decisions are locked; two remain open (§4). At a glance:

| # | Area | Decision | Status |
|---|------|----------|--------|
| [01](issues/01-definition-model.md) | Definition model | Unified relational schema + named guard/action registry | ✅ Locked |
| [02](issues/02-engine-landscape-research.md) | Engine landscape | No off-the-shelf FSM/workflow engine fits | ✅ Locked |
| [03](issues/03-engine-choice.md) | Engine choice | Hand-rolled, shared stateless evaluator; delete SSM | ✅ Locked |
| [04](issues/04-resource-network-association.md) | Resource↔Network association | Versioned families, direct-wins resolution, pin-at-start | ✅ Locked |
| [05](issues/05-info-requirements-model.md) | Information requirements | First-class model, evaluated as a guard | ✅ Locked |
| [06](issues/06-audit-history-model.md) | Audit / history | `enum column → state_id FK`, minimal change | ✅ Locked |
| [07](issues/07-event-requirement-guard-wiring.md) | Requirement guard wiring | Built-in engine stage + machine-level guard scope | ✅ Locked |
| [08](issues/08-version-identity-reconciliation.md) | Version identity | Row id = machine identity; `version` = display sequence | ✅ Locked |
| [09](issues/09-lifecycle-coupling.md) | **Lifecycle coupling (fan-out/fan-in)** | How Negotiation↔Resource lifecycles couple | 🔲 **Open** |
| [10](issues/10-info-requirement-audience-contact.md) | **IR audience, aggregation & contact** | Who is asked, how many, how they're notified | 🔲 **Open** |

Two smaller items remain in the fog (not yet ticketed) — see §5.

### 3.1 The engine

*(Tickets [02](issues/02-engine-landscape-research.md), [03](issues/03-engine-choice.md))*

**We will hand-roll the engine.** A 2026 landscape survey found no viable off-the-shelf option:

- Spring Statemachine is EOL; nearly all FSM libraries are dormant (only `jd-easyflow`, `davidmoten/state-machine`, and `pnavais/state-machine` are actively maintained, and all are low-adoption / single-sponsor risk).
- Workflow engines are too heavy or licensing-encumbered (Flowable is the cleanest OSS option but carries full BPMN weight; Camunda 8 core requires a paid license).
- A hand-rolled data-driven engine is estimated at ~2,500–4,000 LOC (1.5–2.5× the current 1,493 LOC), and most of that cost — the persisted schema and admin layer — exists regardless of engine choice. A library would only cover the cheap part (the evaluation loop) and would have to be bent to fit our bespoke schema.

**Architecture:**

- **One shared engine core** (`StateMachineEngine`), scope-parameterized, replaces both config classes. The Negotiation and Resource lifecycle *services* stay separate (different JPA entities, different persistence wiring); only the transition-evaluation core is shared.
- **Stateless pure evaluator.** The engine holds no state and does no DB I/O. It is handed `(definition graph, current state, event, context)` and returns `{ allowed, next state, ordered actions }`. This makes graph-caching an explicit, testable step rather than an easy-to-miss optimization (structurally rules out an N+1 under load).
- **Definition graph cached in memory**, keyed by the definition **version row id** (see §3.3). Compiled once, invalidated only on a new-version publish (definitions are immutable once active).
- **Spring Statemachine is deleted entirely** — the dependency plus all ~25 classes (configs, guards, actions, persist-handlers, converters, listeners).

---

### 3.2 The definition model

*(Ticket [01](issues/01-definition-model.md), amended by [07](issues/07-event-requirement-guard-wiring.md) and [08](issues/08-version-identity-reconciliation.md))*

One **unified relational schema** for both lifecycles (normalized rows with FKs, not a JSON/DSL blob — so we get indexed queries and FK-enforced admin CRUD):

| Entity | Key fields |
|--------|-----------|
| `StateMachineDefinition` | id, name, `scope` (`NEGOTIATION` \| `RESOURCE`), `version`, active flag, `family_key`, `is_global_default` |
| `State` | id, definition_id, key, label, description, `is_initial` |
| `Event` | id, definition_id, key, label, description |
| `Transition` | id, definition_id, source_state_id, target_state_id, event_id, `required_authority` |

**Guards & actions — named strategy registry.** A fixed set of Java-implemented `GuardType` / `ActionType` strategies, referenced by string key from **wiring rows** with typed JSON `params`. New *logic* needs a new Java class + registry entry; *wiring* (which transition uses which guard/action, in what order) is pure data.

- Realized as a Spring strategy-bean registry (mirrors the existing `WebhookEventMapper` pattern: inject `List<Strategy>`, fold to a `Map`, fail at startup on duplicate keys). Each strategy declares its own key and its own `params` DTO type; `params` persists as jsonb via `@Type(JsonType.class)` (already the idiom for `InformationSubmission.payload`).
- **Two separate wiring tables** (guards and actions never interleave — guards run pre-commit, actions run only after a transition succeeds):
  - **Guard wiring** spans two scopes via a nullable `transition_id`: `null` = machine-level (applies to every transition of the definition), set = transition-specific. Per-scope ordering via partial unique indexes (PostgreSQL-only, so no portability caveat).
  - **Action wiring** is transition-scoped only.
- Example collapse: one `SET_POST_VISIBILITY` action with `{scope, enabled}` params replaces today's three hardcoded action classes.

**Security — `required_authority`** is a distinct field on `Transition` (not folded into the guard list), from a small fixed set: `NONE`, `IS_ADMIN`, `IS_CREATOR`, `IS_REPRESENTATIVE`. (`IS_ADMIN` collapses today's identical `ROLE_ADMIN` / `isAdmin` checks.)

**Info-requirement attachment point:** requirements attach to the **`Event`** (not the transition), preserving today's "applies wherever this event fires" semantics. The model itself is §3.4.

**Dead-enum cleanup (verified unreachable):** drop `NegotiationState.APPROVED`, `NegotiationEvent.START`, `NegotiationResourceEvent.RETURN_FOR_RESUBMISSION`, `NegotiationResourceState.RETURNED_FOR_RESUBMISSION`. **`OVERRIDE` is kept** — it's not dead; it tags an admin's direct state override for audit, modelled as a transition-less `Event` row that bypasses `evaluate()` entirely.

---

### 3.3 Versioning & Resource↔Network association

*(Ticket [04](issues/04-resource-network-association.md), reconciled by [08](issues/08-version-identity-reconciliation.md))*

**Definition families.** Rows form **families** — a lineage of versions sharing an immutable `family_key` (the stable identity everything FKs against). `name` is a freely-editable display label with no identity role. Exactly one row per family is `active`; publishing a new version is a one-step active-flag flip.

**Two identities (resolved a cross-ticket inconsistency):**
- **Row id** = the sole machine identity — every FK, the engine cache key, the version pin.
- **`version`** = a per-family, system-assigned incrementing integer for humans ("v3 of Standard flow"), unique on `(family_key, version)`, assigned at row creation. Display-only; gaps (from abandoned drafts) are harmless.

**Resolution order** for a Resource's definition:
1. **Direct** Resource↔family association — always wins, unconditionally.
2. Otherwise, **Network** association (optional per Network).
3. Otherwise, a single **global default** family (admin-only `is_global_default` flag; mirrors the Negotiation lifecycle's single global definition).

**Multi-Network conflict.** If a Resource belongs to multiple Networks pointing at *different* families, the write that would create the conflict (adding a Resource to a Network, or changing a Network's family) is **rejected at write-time** with an error naming the conflicting Networks — enforced as an application-level check in `NetworkService` (a single set-based query, cheap because these are rare governance actions). Resources with a direct override are exempt. A Network priority/precedence field was considered and rejected (it trades a loud, correctable rejection for silent ambiguity).

**Versioning is pin-at-start.** `NegotiationResourceLink` (and `Negotiation` for the Negotiation lifecycle) records the resolved version at creation via an immutable `definition_version_id` FK. In-flight work keeps running the exact version it started on, forever — a published new version never moves in-flight negotiations, and an old version is never mutated or discarded while anything references it.

---

### 3.4 Information requirements as a first-class model

*(Ticket [05](issues/05-info-requirements-model.md), wiring by [07](issues/07-event-requirement-guard-wiring.md); audience/contact still open in [10](issues/10-info-requirement-audience-contact.md))*

Today `InformationRequirement` ties an `AccessForm` to a `NegotiationResourceEvent`, enforced ad-hoc inside `ResourceLifecycleServiceImpl.sendEvent()`, Resource-lifecycle only. The redesign makes it first-class for **both** lifecycles:

- **Attachment:** 0..N unordered `Requirement`s per `Event`. Because `Event` is scope-agnostic, Negotiation-scope events can carry requirements with no extra modeling. (Implication: `InformationSubmission.resource` becomes optional.)
- **Enforcement is a built-in engine stage, not wiring.** Ticket 07 settled that requirement-satisfaction is **not** an admin-wireable guard row — the engine *always* checks the firing Event's requirements as a fixed pipeline stage. It cannot be omitted, so the "applies wherever this event fires" guarantee is structural and the dead-click bug can't be reintroduced by misconfiguration. It shares the guard contract and emits a normal `GuardResult`.
- **One evaluation pipeline** used both for the real gate (`sendEvent`) and the "possible next events" dry-run — killing today's double-enforcement bug (the security rule is checked once by hand to build `getPossibleEvents()` and again by SSM's interceptor on fire). Pipeline order, short-circuiting: **`required_authority` → requirements check → guard rows** (machine-level then transition-level), ordered by expected failure likelihood, with monotonic failure categories (403 → 422 → 409).
- **Machine-level guards** are now a first-class wiring scope (ticket 07): `NEGOTIATION_APPROVED` becomes one definition-level guard entry instead of today's dangling source-less `.withExternal().guard(...)` hack.
- **Guard contract returns structure:** `GuardResult { satisfied, reasonCode, details }` — the requirement guard populates `details` with the missing `AccessForm`(s) and assignee.
- **API: no new endpoint/DTO.** `getPossibleEvents` keeps today's shape; a blocked event is simply omitted. The existing `requirement-{id}` / `submission-{id}` hint links carry over with three fixes: (1) include the hint based on *structural reachability* not "a lifecycle link already exists"; (2) show the event's human label instead of the raw enum key; (3) HAL cleanup to array-valued `requirement` / `submission` rels.
- **`InformationSubmission` gains `submittedBy` (`Person`)** — needed to match a submission against an audience.

*(A known `ResourceWithStatusAssembler` N+1 was flagged as a follow-on implementation fix, not designed here.)*

**Still open (ticket 10):** *who* is asked (audience resolver registry), *how many* must answer (`ANY`/`ALL`/`N-of-M`), and *how they're told* (contact) — see §4.

---

### 3.5 Audit / history

*(Ticket [06](issues/06-audit-history-model.md))*

**Minimal change.** Each `LifecycleRecord`'s `@Enumerated changedTo` column becomes a **`state_id` FK to a `State` row**; the timeline label renders live by join (`state.getLabel()`).

- **Labels are not frozen.** A later label edit may ripple into historical timeline text — explicitly accepted, because it's identical to today's behavior when an enum label is renamed. No snapshot, no "label edit = new version" rule.
- **No version column** on the audit tables. Version is implied transitively (`audit → State → StateMachineDefinition`) and recovered by join; referential integrity holds because a referenced version is never discarded (§3.3).
- **Two tables stay separate** (Negotiation vs. Resource); the read side is already unified via `NegotiationTimelineEvent`.
- Deferred but additive: capturing the firing `Event`, and from-state (still derivable as the prior record).

---

## 4. Open questions (the current frontier)

Both are grilling sessions, each roughly one working session, and both are **fully unblocked** — either can be picked up now.

### 🔲 Ticket 09 — Lifecycle coupling (fan-out / fan-in) *[next by order]*

Every decision so far designs a *single* definition in isolation. This ticket designs the **coupling** between the Negotiation lifecycle and its Resource lifecycles — where today's behavior actually lives:

- **Fan-out (SPAWN):** on negotiation approval, each requested resource begins its own (possibly heterogeneous) resource lifecycle in parallel.
- **Fan-in (FEEDBACK):** when all resources reach a terminal state, the negotiation auto-concludes.

Working lean (not settled): coupling is one recursive primitive — SPAWN deflates to "initialize the child state columns on the parent transition" (nothing new is instantiated), and FEEDBACK is a **query-based** aggregation guard (reusing ticket 07's machinery) fired by a signal event, with `required_authority = NONE ⇒ auto-conclude`. **The core open decision: who pokes the parent, and do any transitions self-fire?** — this same "self-firing transition" question also covers IR reminders/expiry (ticket 10) and is meant to answer it for the whole subsystem at once.

### 🔲 Ticket 10 — IR audience, aggregation & contact

Ticket 05 settled the enforcement seam but left the model *around* it thin. Four things to nail:

1. **Audience as a first-class concept** with a resolver registry (`RESOURCE_REPRESENTATIVES`, `NEGOTIATION_CREATORS`, `IAM_GROUP(ref)`), mirroring the guard/action registry. (The `IAM_GROUP` *interface* is designed here; LS-AAI membership-sync mechanics are deferred to the fog.)
2. **Aggregation quantifier** over the audience — `ANY` / `ALL` / `N-of-M` (makes "everyone in the SMB submits their own" expressible as config, with no per-member fan-out).
3. **Contact as a parent-transition action** (`NOTIFY_IR_AUDIENCE`) — no `CONTACTED` state; lean is to fire structurally so it can't be forgotten.
4. **Derive, don't store** — IR state is a projection of `information_submission` rows, never a stored column.

Recorded dead-end (don't re-explore): making the IR its own state machine — rejected because it would force a persisted per-IR instance, which the "no persisted instance" rule forbids. Rationale: `notes/ir-smd-rejected-rationale.md`.

---

## 5. Not yet specified (fog)

In scope, but not yet sharp enough / not yet unblocked to ticket:

- **Migration / rollout path** — how existing hardcoded enum data, existing `InformationRequirement` rows, and existing enum-valued `LifecycleRecord` audit rows migrate into the new model without breaking in-flight negotiations. **This is already sharp enough to ticket now** (its last dependency, the audit model, is settled) and doesn't wait on the frontier.
- **IAM / LS-AAI group membership-sync mechanics** — how the `IAM_GROUP` audience resolver actually fetches/mirrors LS-AAI virtual groups (membership resolution, caching, refresh cadence). Becomes ticketable once ticket 10 fixes the resolver interface.

Nothing is currently ruled out of scope.

---

## 6. Glossary

| Term | Meaning |
|---|---|
| **ADR** | Architecture Decision Record — a short document capturing one design decision and its rationale |
| **API** | Application Programming Interface — here, the backend's REST endpoints |
| **BPMN** | Business Process Model and Notation — the modelling standard used by workflow engines like Flowable/Camunda |
| **CRUD** | Create, Read, Update, Delete — the basic data-management operations (here, admin editing of definitions) |
| **DTO** | Data Transfer Object — a plain object shaping data sent over the API |
| **EOL** | End of Life — no longer maintained or supported (Spring Statemachine's status) |
| **FK** | Foreign Key — a database column referencing a row in another table |
| **FSM** | Finite State Machine — the model of states + transitions this subsystem implements |
| **HAL** | Hypertext Application Language — the JSON hypermedia format used for the API's `_links` |
| **HATEOAS** | Hypermedia As The Engine Of Application State — the REST style that embeds navigable links in responses |
| **IAM** | Identity and Access Management — the external identity system (here, LS-AAI) |
| **IR** | Information Requirement — a mandatory form/submission gating an event |
| **JPA** | Java Persistence API — the ORM layer mapping Java entities to database tables |
| **jsonb** | PostgreSQL's binary JSON column type (used for strategy `params` and submission payloads) |
| **LOC** | Lines Of Code — a rough size estimate |
| **LS-AAI** | Life Science Authentication and Authorisation Infrastructure — the federated identity provider supplying virtual groups |
| **N+1** | The N+1 query problem — issuing one query per row instead of one batched query |
| **OSS** | Open-Source Software |
| **REST** | Representational State Transfer — the API architectural style |
| **SMB** | Sample/Biobank member group — example audience for an information requirement |
| **SQL** | Structured Query Language — the database query language |
| **SSM** | Spring Statemachine — the EOL library being replaced |
