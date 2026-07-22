# Definition version/identity reconciliation

Type: grilling

Status: resolved

Blocked by: 01, 03, 04

## Question

Three resolved tickets describe the identity/versioning of `StateMachineDefinition` in ways that were never reconciled (flagged by the 2026-07-16 consistency review on all three):

- [Definition model](01-definition-model.md) gives `StateMachineDefinition` a bare `version` field with no stated semantics beyond existing.
- [Resource-Network association & conflict/versioning](04-resource-network-association.md) introduces `family_key` as the immutable lineage identity — one row per version, one `active` row per family, pinning via a `definition_version_id` FK that already picks out one specific row by itself.
- [Engine choice](03-engine-choice.md) caches the compiled definition graph keyed by `(definition_id, version)` — two axes, without saying whether `definition_id` means the family or the version row.

Decide, in one coherent statement that the three tickets can be amended to cite:

1. What happens to ticket 01's `version` field — per-family incrementing sequence number, dropped as redundant with row identity, or something else?
2. What is the engine cache actually keyed by — the version row's own id, `(family_key, version)`, or something else?

## Answer

**Two identities, one for machines and one for humans.** The row id is the sole machine identity of a definition version — every FK (including ticket 04's `definition_version_id` pin) and every internal lookup uses it. `version` is kept purely for humans: a per-family, system-assigned, monotonically incrementing integer (unique constraint on `(family_key, version)`) so admin UI and audit history can say "v3 of Standard flow" instead of an opaque row id. It has no identity role and is never edited.

**Sequence assigned at row creation**, not at publish: a new row gets `max(version)+1` within its family when inserted, whether or not it ever goes active. An abandoned draft leaves a gap in the sequence — harmless, because the number is display-only, not a contiguity contract. (Chosen over publish-time assignment, which would guarantee a gapless published history at the cost of a nullable field state and a publish-time uniqueness race.)

**Engine cache keyed by the version row id alone**, amending ticket 03's `(definition_id, version)` two-axis wording. Since rows are immutable once active, one id ↔ one compiled graph; the lookup path from a pinned negotiation/resource to the cached graph is a single id with no join and no re-derivation of `(family_key, version)`. Ticket 03's "`definition_id`" is thus retroactively read as "the version row's id", and the `version` half of its key is dropped as redundant.

**Amends**: [Definition model](01-definition-model.md) (`version` semantics), [Engine choice](03-engine-choice.md) (cache key), [Resource-Network association & conflict/versioning](04-resource-network-association.md) (reconciliation noted) — amendment notes appended to each.
