# Audit/history model redesign

Type: grilling
Status: resolved

Blocked by: 01, 04

## Question

Today, `NegotiationLifecycleRecord`/`NegotiationResourceLifecycleRecord` append-only tables record transition history against a fixed set of hardcoded states/events. Given the definition model from ticket 01 (definitions become editable, versioned data rather than fixed Java enums), decide whether these audit tables need to also capture the pinned version in effect at the time of each transition, and design that shape if so.

## Review findings (unresolved)

<!-- Surfaced by a wayfinder-map consistency review on 2026-07-16. Unchecked = not yet reconciled; pick up in a fresh session. -->

- [x] **Missing `Blocked by: 04`.** This ticket's question — whether/how to stamp which definition/version was active on each audit row — can't be meaningfully answered without ticket 04's family/`definition_version_id` model (which didn't exist when this ticket was filed against ticket 01 alone). Add `04` to the Blocked-by line before claiming this off the frontier. — *Fixed.*
- [x] **Check for redundancy with ticket 04 before designing a new field.** Versioning is pin-at-start and immutable for a lifecycle's entire duration (per ticket 04), so every `LifecycleRecord` for a given negotiation/resource already shares the same version recorded on the parent row's `definition_version_id`. This ticket may conclude no new per-row version field is needed at all (just join to the parent) — worth checking before designing new audit-table columns. — *Resolved: no new version field. The audit row's `state_id` FK already implies the version transitively (`State.definition_id`), so version is recoverable by join, not stored. See Answer.*

## Framing for the resolving session

<!-- Sharpened in a grilling session (2026-07-21) that reconciled the ticket-04 findings; captured here so this ticket opens hot. Not yet decided. -->

Current shape (facts): both `NegotiationLifecycleRecord` and `NegotiationResourceLifecycleRecord` extend `AuditEntity` (who/when) and store **only the destination state** as a JPA `@Enumerated(STRING)` enum `changedTo`, then render timeline text via `changedTo.getLabel()` — i.e. the label lives in **immutable Java**. The event that caused the transition and the from-state are not recorded. In the new model the enum is gone and states/events are editable versioned data rows, so the audit table must record the transition some other way. That choice — not the version field — is the real decision here; the version-field question falls out of it.

The core decision is **how the audit row records state/event identity**:
- **(a) FK to the pinned version's `State`/`Event` row.** History text renders by joining and reading that row's label. Safe *only if* a referenced version's labels are frozen (label edit = new version, not in-place). Then the version is **implied by the FK'd row** → no `definition_version_id` column needed at all, and Finding C resolves a fortiori. DRY.
- **(b) Denormalized snapshot** of state/event code + human label as-of-transition. Self-contained, frozen forever, tolerates in-place label edits on a live version; version becomes optional metadata (join to parent if ever wanted). Duplicates data.

The hinge between (a) and (b) is one question to settle first: **are a version's labels frozen once it's referenced?** (Ticket 04 freezes referenced versions from mutation — does that extend to labels?) Provisional lean from the grilling: **(a) + freeze labels per version**, also capturing the firing `Event` (not just destination state) since the new model is event-driven; from-state stays derivable from the prior record. Either (a) or (b) resolves Finding C as "no new version column." Blocked-by 01 and 04 are both now resolved, so this ticket is on the frontier.

## Answer

**Core decision — how the audit row records state identity: option (a), FK to `State`.** Each `LifecycleRecord`'s `@Enumerated` `changedTo` enum column becomes a **`state_id` FK to a `State` row**. Timeline text renders the label **live, by join** — `state.getLabel()` replacing today's `changedTo.getLabel()` — which preserves exactly today's read-time render behavior, just data-driven instead of enum-driven.

**Labels are not frozen (the hinge, decided against the provisional lean).** A later label edit is allowed to ripple into historical timeline text, and that is explicitly acceptable — it's already what happens today when an enum label is renamed in Java. So neither of the framing's guards is needed: no "label edit = new version" freeze rule, and no denormalized snapshot (option b is dropped). The audit row does not preserve as-of-transition label text; it always shows the referenced `State` row's current label.

**No version column on the audit tables (resolves Finding C / the redundancy finding).** Versioning is a **definition-level** concept — a `StateMachineDefinition` row *is* a version; a `State` row is not independently versioned, it is owned by exactly one version via `State.definition_id`. So the audit row's `state_id` already identifies the pinned version **transitively** (`audit → State → StateMachineDefinition`); the version is recoverable by join and is never stored on the audit table. No `definition_version_id` column is added. Referential integrity holds because ticket 04 guarantees a referenced version is never discarded, and an audit-row FK is such a reference. This ticket does **not** re-open or iterate on versioning mechanics — it only consumes ticket 04's model.

**Scope held tight — deferred but not foreclosed:**
- **Firing `Event` not captured.** Destination-state only, as today. A later `event_id` FK is a clean additive change if richer history ("X *approved* …", disambiguating two events landing on one state) is ever wanted.
- **From-state not stored** — remains derivable as the chronologically previous record for that negotiation/resource, as today.
- **`OVERRIDE` needs no special audit handling** — an admin override records its resulting `State` via `state_id` like any other row, exactly as today (the audit row never encoded the bypass; only the resulting state).

**Two audit tables kept separate.** `NegotiationLifecycleRecord` and `NegotiationResourceLifecycleRecord` are not unified under the definition model's `scope`; each simply swaps its enum column for a `state_id` FK. The resource table keeps its `resource_id`; the read-side is already unified via the `NegotiationTimelineEvent` interface, so a table merge would be cost (nullable `resource_id`, discriminator, migration) for a cosmetic gain.

**Net change:** *`@Enumerated` enum column → `state_id` FK* on both audit tables. No new version column, no snapshot, no new semantics.

**Graduates fog:** this was the last blocker on the **Migration/rollout path** item in the map's *Not yet specified*. Migrating existing enum-valued `LifecycleRecord` rows onto `state_id` FKs (mapping legacy enum values → the `State` rows of whichever version historical negotiations are deemed to have run under) is a genuine migration question — it belongs to that now-unblocked fog item, not here.
- [x] **Question wording conflates two different "active" concepts.** "Which definition/version was active at the time of each transition" is ambiguous between the family's active-flag version (which can flip while a negotiation is in flight) and the specific pinned version the negotiation/resource is actually running (which ignores the family's active flag entirely, per ticket 04). Tighten to "the pinned version in effect" before resolving. — *Fixed.*
