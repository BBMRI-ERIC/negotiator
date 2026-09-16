---
status: accepted
---

# 0002 — Lifecycle definitions are relational configuration, not Java

_Source tickets: [Definition model](../../../.scratch/state-machine-redesign/issues/01-definition-model.md), [Event-requirement guard-chain wiring mechanism](../../../.scratch/state-machine-redesign/issues/07-event-requirement-guard-wiring.md) (wiring scope), [Lifecycle coupling: fan-out / fan-in](../../../.scratch/state-machine-redesign/issues/09-lifecycle-coupling.md) (`SYSTEM` authority, State flags)._
_Implementation is follow-on work — nothing here is built yet, and the definitions are still hardcoded Java._

Today the two Lifecycles are Java enums and builder calls. The redesign's whole point is that a Resource Lifecycle becomes configurable per Network or per Resource, which means the definition has to be data. The Negotiation Lifecycle keeps its single definition, but is externalized the same way so there is one model rather than two.

**One relational schema serves both Definition Scopes.** `StateMachineDefinition` (with a `scope` of `NEGOTIATION` or `RESOURCE`), `State`, `Event` and `Transition` are normalized tables with foreign keys — not a JSON or DSL blob. Rows give us indexed "which Transitions leave State X for Event Y" lookups and let admin CRUD lean on referential integrity instead of re-validating a document on every write. A `State` carries a human label plus `initial` and `terminal` flags, with exactly one initial State per Definition Version; the evaluator needs `initial` to start a Lifecycle and `terminal` to judge when one is finished. An Event may carry no Transition at all — that is how the Override Event survives as a name under which an admin's direct state change appears in history.

**Guard and Action logic stays in Java; only the Wiring is data.** A Guard or Action is a self-describing Spring strategy bean declaring its own string type key and its own params type, folded into a registry at startup exactly as the existing `WebhookEventMapper` does, with duplicate keys failing the boot. Wiring rows name the type key and carry per-strategy `params` as jsonb, deserialized into the strategy's declared type at load time — one unchecked bridge, in one place. Runtime domain state (the negotiation, the acting person) reaches a strategy through the evaluation context at fire time, never through `params`. New behaviour therefore needs a new Java class; new *configuration* needs only a row. Giving Actions params too collapses today's three post-visibility Action classes into one `SET_POST_VISIBILITY` type with a scope and a flag.

**Guards and Actions get two separate wiring tables, not one polymorphic one.** They never interleave — Guards run before a commit, Actions only after one — so a shared ordering would be meaningless, and only Guards need a scope. Guard wiring is one table with a nullable `transition_id`: null means the Guard applies to every Transition of the Definition Version, set means it applies to that Transition alone. Two partial unique indexes keep the `order` column unique within each scope; the backend is PostgreSQL-only, so partial indexes cost nothing in portability. The effective Guard chain for a Transition — what admin tooling must show — is then one query. Action wiring is transition-scoped only and needs no `definition_id`, since the Transition already implies it. The `order` columns sequence within a scope; the order *between* scopes is pipeline logic, not a column.

**Who may fire is a separate field from whether the domain permits it.** `Transition.required_authority` takes one of `NONE`, `IS_ADMIN`, `IS_CREATOR`, `IS_REPRESENTATIVE`, `SYSTEM`, rather than being folded into the Guard chain. Asking *who is firing* and asking *whether the move is currently legal* fail differently and should not be expressible as the same kind of row. `IS_ADMIN` collapses today's two spellings — `ROLE_ADMIN` on the Negotiation machine and `isAdmin` on the Resource one — which were verified to check the same granted authority. `SYSTEM` marks a System Event, which no human caller can ever satisfy.

Enum values that were already unreachable — `NegotiationState.APPROVED`, `NegotiationEvent.START`, and the `RETURN_FOR_RESUBMISSION` event and state — leave the active graph. The Override Event does not: it is live today, tagging admin state changes for history, and is kept.

This ADR does not claim that Information Requirement satisfaction is one of these registry Guards — it deliberately is not (0005) — nor does it settle versioning or identity (0003).

## Amendments

### 2026-09-16 — State and Event rows carry the metadata the REST API already publishes

_Source ticket: [The global state and event metadata contract](../../../.scratch/state-machine-implementation/issues/04-global-state-event-metadata-contract.md)._

This ADR enumerated a `State` as carrying "a human label plus `initial` and `terminal` flags", and said nothing at all about what an `Event` carries. Both were written without noticing that four live REST endpoints already publish, for every State **and every Event**, a `label` *and* a `description` — and for every Resource State an ordering integer that the frontend computes an organization's rolled-up status from. Deleting the enums deletes the only source of all three, so the schema needs them.

**Three columns are added, in `V36.5`.** `event.label` is NOT NULL; `state.description` and `event.description` are nullable; `state.progress_order` is an integer. ADR 0009's v1 seed populates all of them from the enum constructors it is already transcribing, with Progress Order taking the enum declaration order it reproduces exactly — 0–11 for Resource States, 0–7 for Negotiation States.

**Labels cannot be derived from names.** Doing so would render `Submitted` where the UI says "Under review", and cannot produce the comma in "Mark as Currently Unavailable, But Willing to Collect".

**Progress Order cannot be derived from the Transitions**, because a graph with branches has no total order. It is a *progress* ordering — what the deleted enum's own comment meant by "the most advanced state is at the bottom" — so recording it is the only available answer. It is the new glossary term **Progress Order**; the REST field keeps its older name, `ordinal`.

This changes what a definition row holds. It changes nothing about the two claims this ADR exists to make: that definitions are normalized relational configuration rather than a document, and that Guard and Action logic stays in Java with only the Wiring as data.
