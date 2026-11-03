---
status: accepted
---

# Definition versioning and identity

An edit to a definition could change the graph under a Negotiation halfway through its Lifecycle, or remove the State it stands in. Versioning makes that impossible.

**Definitions form families of immutable versions.** A Definition Family groups every version of one lifecycle definition under an identity that never changes; its name is only a display label. Each member is a Definition Version. A version that is active or in use is never edited or deleted: a change publishes a new version. A family has at most one active Definition Version, the one new work resolves to.

**Each Lifecycle pins its Definition Version, once.** A Negotiation pins when it is created. A Resource pins at Spawn, which pins every requested Resource of the Negotiation that has no pin yet. A pin never changes afterwards. Publishing a new active version therefore affects only work that pins later, and Event Firing always uses the pinned version, never the active one, so nothing in flight moves.

**A Resource linked after Spawn pins when it is linked.** An admin can add a Resource to a Negotiation in any State. If its sibling Resources are already pinned, the new one pins immediately, against the versions active at that moment. Two Resources of one Negotiation can therefore run different versions, or different families.
