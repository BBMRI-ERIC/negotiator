---
status: accepted
---

# Lifecycle Records reference States by FK

Lifecycle Records reference the State entered by foreign key rather than store an enum-backed name. The timeline uses the referenced State's label.

Labels remain live: edits change past timeline text without a new Definition Version, preserving current behavior.

Each State belongs to exactly one Definition Version, so the reference also identifies the record's version. Referenced Definition Versions cannot be deleted ([Definition versioning and identity](0004-definition-versioning-and-identity.md)).

A Lifecycle Record captures only the destination State, not the causing Event; the previous record supplies the source State. Override Events follow the same rule. Negotiation and Resource histories remain separate, avoiding a storage redesign solely for a combined timeline.

The current State remains a name resolved within the Definition Version Pin. Names are unique within each Definition Version, so pin and name resolve to exactly one State. The cutover migration leaves the live columns as they are. Filtering, sorting and statistics compare the name column without a join.

## Considered options

- Snapshot State names and labels in history. We rejected it because it duplicates State information and changes live-label behavior.
- Reference current State by foreign key. We rejected it because the cutover would have to convert the live columns too, and filtering, sorting and statistics would need a join to `state`.

## Consequences

History consumers must resolve State references rather than read stored names.
