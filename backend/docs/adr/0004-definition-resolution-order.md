---
status: accepted
---

# 0004 — Definition resolution: direct association wins, Network second, global default last

_Source ticket: [Resource-Network association & conflict/versioning](../../../.scratch/state-machine-redesign/issues/04-resource-network-association.md)._
_Implementation is follow-on work — nothing here is built yet._

A Resource belongs to zero or more Networks, many-to-many. Once a Resource Lifecycle can run under different Definition Families, something has to say which family a given Resource uses — and a Resource in three Networks may be pointed at three different answers.

**Resolution is a fixed order of precedence.** A direct Resource↔family association wins outright and unconditionally. Otherwise the Resource's Networks are consulted; a Network's family association is optional, and a Network that has not set one makes no claim. If nothing more specific applies, the Global Default Family is used. Because exactly one family carries that flag, resolution is total — it can never fail to find a family, which is what lets Spawn (0007) initialize every Resource without a half-initialized outcome. Resolution always yields a family, never a version; the version is the family's active one at the moment work starts, and it is pinned there (0003).

**A multi-Network conflict is rejected at write time, not tie-broken.** When a Resource belongs to several Networks naming different families — compared by `family_key`, so two Networks on the same family never conflict regardless of which version is active — the write that would create the situation fails, naming the Networks involved. Two paths can create one: adding Resources to a Network, and changing a Network's family. The check is exhaustive over the affected Resources but expressed as a single set-based query per write, not per-Resource iteration: among the Resources in play, is there one with no direct override that also belongs to another Network with a different non-null family? Resources with a direct association are exempt, since their resolution cannot be affected. Worst-case cost is linear in a Network's membership, which is acceptable — these are rare governance actions, not request-hot-path — so no caching or incremental-diff machinery is warranted. Enforcement lives in `NetworkService`, consistent with how this codebase validates cross-entity rules everywhere else; no database triggers.

**The Global Default Family is admin-governed.** An `is_global_default` boolean on the family, with a partial unique index enforcing exactly one, settable by `ROLE_ADMIN` only — the same tier as owning the single Negotiation-scope definition. Network managers set their own Network's family and can never touch the default. Reassigning it is one admin flipping the flag. Default-ness is a resolution-time concern only: the default family versions like any other, and because the flag sits on the family rather than on a version row, it survives version flips without ever pointing at a stale row.

## Considered Options

**Network priority.** Giving Networks a precedence rank would eliminate the conflict check entirely: walk a Resource's Networks in rank order and take the first family. It was reconsidered and rejected. It trades a loud, admin-correctable rejection for the *silent* resolution of genuine lifecycle ambiguity — a Resource quietly running the wrong Lifecycle, with nothing to notice. It also moves the unsurfaced blast radius onto reorder operations, where changing one rank silently re-resolves every Resource beneath it, and it centralizes governance that reject-at-write leaves with the people who own each Network. That is too high a price to avoid a rare, cheap query.

This ADR does not restate pinning mechanics (0003).
