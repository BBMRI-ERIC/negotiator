# The global state and event metadata contract

Type: grilling
Status: open
Blocked by: 03

## Question

Three DTOs enumerate States and Events **globally**, as a fixed universe:

- `negotiation/dto/NegotiationStateMetadataDto` — `NegotiationState value`, `String label`, `String description`
- `governance/resource/dto/ResourceStateMetadataDto`
- `governance/resource/dto/ResourceEventMetadataDto`

Once Resource Lifecycles can run different Definition Families, **a global list is no longer well-defined.** There is no single set of Resource States; there is one set per Definition Version, and two Resources in one Negotiation may run different ones (ADR 0004). What do these endpoints return?

In stage 1 a total answer exists — one seeded family per scope, so "the states of the Global Default Family's active version" is correct and complete. The decision is what **contract** to commit to now, given stage 2 makes that answer wrong.

### Sharpen at least

1. **Who consumes these, and for what?** Find out before designing — most likely a frontend filter dropdown or a status legend. If it is a filter vocabulary, the honest answer may be "the union across all families", which is a different thing from "one definition's graph".
2. **Global, per-family, or per-negotiation?** A per-`Negotiation` endpoint is always well-defined (its resources' pinned versions are known). A per-family endpoint is well-defined but pushes family selection onto the caller. A global union is well-defined but describes no actual graph, so it can list two states that never coexist.
3. **Where does `description` come from?** ADR 0002 gives `State` a `label` and the `initial`/`terminal` flags — **no description column**. Either the schema needs one (an ADR 0002 amendment, which is a real cost) or the field is dropped, or it is derived. Check what populates it today: the DTO's javadoc references `eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata`, a class that **may no longer exist** — verify before designing.
4. **Does the answer change per Definition Scope?** The Negotiation Lifecycle keeps a single definition, so its metadata endpoint stays globally well-defined and may not need to change at all. Only the two Resource DTOs have the problem.
5. **Is a breaking API change acceptable here?** The predecessor map's destination said REST/vocabulary compatibility is "preferred but not a hard constraint", and stage 1's gate is characterized parity with two named carve-outs — a third carve-out needs to be a deliberate decision, not a side effect.

### Why it is separate from ticket 03

Ticket 03 decides the *type* that names a State. This ticket decides whether an endpoint that enumerates a *universe* of States still makes sense. Ticket 03's answer constrains this one but does not settle it — and these three DTOs are explicitly **carved out** of the consumer-decoupling slab for that reason, so that slab is not blocked waiting on this.

Use `/grilling` + `/domain-modeling`.
