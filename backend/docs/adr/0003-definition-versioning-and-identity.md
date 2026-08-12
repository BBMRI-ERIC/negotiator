---
status: accepted
---

# 0003 — Definition versioning: immutable families, pinned at start, row id is identity

_Source tickets: [Resource-Network association & conflict/versioning](../../../.scratch/state-machine-redesign/issues/04-resource-network-association.md) (versioning), [Definition version/identity reconciliation](../../../.scratch/state-machine-redesign/issues/08-version-identity-reconciliation.md)._
_Implementation is follow-on work — nothing here is built yet._

Once a definition is editable data (0002), editing one becomes dangerous: a Negotiation halfway through its Lifecycle could find the graph beneath it changed, or its current State no longer present. Versioning exists to make that impossible.

**Definitions form families of immutable versions.** A Definition Family is a lineage of `StateMachineDefinition` rows sharing an immutable `family_key`, set once and never edited. `name` stays a freely editable display label with no identity role. Exactly one row per family is active — the version new work resolves to — so publishing is a one-step flip of that flag rather than a bulk re-point of every association. A version that is active or referenced is never mutated in place and never discarded; editing means publishing a new row.

**The version row's id is its sole machine identity.** Every foreign key uses it: the pin below, and the evaluator's compiled-graph cache key (0001). There is no composite `(family, version)` lookup anywhere, and no join needed to get from a pinned Lifecycle to its cached graph.

**`version` survives only as a display sequence.** It is a per-family, system-assigned incrementing integer, unique on `(family_key, version)`, so admin tooling and history can say "v3 of Standard flow" instead of an opaque id. It is assigned at row creation rather than at publish: an abandoned draft leaves a gap, which is harmless because the number is display-only and promises no contiguity. Publish-time assignment would guarantee a gapless published history at the cost of a nullable field and a publish-time uniqueness race — not worth it for a label.

**Work pins its Definition Version when its Lifecycle starts.** `Negotiation` and `NegotiationResourceLink` each carry an immutable `definition_version_id`, written when that Lifecycle begins and never changed. Publishing a new active version therefore affects only work that starts afterwards; nothing in flight ever moves. This is what makes immutability worth having — the alternative, re-pointing live work at a new graph, would mean a Negotiation could be sitting in a State the new version does not define.

This ADR does not say how a Definition Family is found in the first place — that is Definition Resolution (0004).
