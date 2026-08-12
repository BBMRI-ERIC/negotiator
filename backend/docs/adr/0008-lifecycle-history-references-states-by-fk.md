---
status: accepted
---

# 0008 — Lifecycle history references States by FK and renders labels live

_Source ticket: [Audit/history model redesign](../../../.scratch/state-machine-redesign/issues/06-audit-history-model.md)._
_Implementation is follow-on work — nothing here is built yet; the audit columns are still `@Enumerated` enums._

Both Lifecycle Record tables store the destination State as a JPA `@Enumerated` string and render timeline text from the enum's label — that is, from immutable Java. Once States are editable rows the enum is gone, so the audit row has to record the State some other way.

**The `changedTo` enum column becomes a `state_id` FK to a `State` row, and timeline text is rendered live by join** on that row's label. This preserves today's read-time behaviour exactly; it is only data-driven instead of enum-driven.

**Labels are deliberately not frozen.** Editing a State's label may ripple into historical timeline text, and that is accepted rather than tolerated: it is precisely what happens today when someone renames an enum's label in Java, so it is no new hazard. This is the hinge the whole decision turned on, and it was settled against the earlier lean. Because labels are not frozen, no "a label edit means a new version" rule is needed, and the alternative shape — denormalizing the state code and its label onto every audit row as an as-of-transition snapshot — was dropped. A snapshot would have been self-contained and immune to label drift, at the cost of duplicating on every row data that already exists once.

**No version column on the audit tables.** A `StateMachineDefinition` row *is* a version, and a `State` is owned by exactly one of them, so `state_id` already identifies the pinned version transitively; the version is recoverable by join and is never stored. Referential integrity holds because a referenced version is never discarded (0003), and an audit row's FK is such a reference.

The scope is held tight, and three things are deferred as cleanly additive rather than designed now: the firing Event is still not captured (destination State only, as today — an `event_id` FK is a later addition if history ever needs to say which Event landed here); the from-State stays derivable as the previous record; and the two tables stay separate rather than being unified under the definition model's scope, since the read side is already unified behind one interface and merging would cost a nullable `resource_id`, a discriminator and a migration for a cosmetic gain. An Override Event records its resulting State like any other row — the audit row never encoded the bypass, only where the Lifecycle ended up.

This ADR does not say how the *live* current-state column is represented; that stays a plain string with no FK, for reasons in 0009.
