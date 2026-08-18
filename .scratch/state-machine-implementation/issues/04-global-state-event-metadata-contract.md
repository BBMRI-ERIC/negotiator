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

## Added by ticket 03 — a sixth thing to sharpen

**`ordinal` is a published ordering contract, and deleting the enum deletes it.**

`ResourceStateMetadataDto:22` does `this.ordinal = value.ordinal()` — it publishes the enum's
**declaration order**, which `NegotiationResourceState`'s own comment says is load-bearing ("the
order of the individual values is important. The most advanced state (final state) is at the
bottom"). Two frontend consumers depend on it:

- `frontend/src/views/NegotiationPage.vue:348-367` builds a `state → ordinal` map and rolls a
  multi-resource organization up to **whichever state has the highest ordinal** — i.e. it uses the
  ordering as "how far along is this organization".
- `frontend/src/components/OrganizationCard.vue:71` sorts a state list by it.

**ADR 0002's `State` carries `label` plus `initial` and `terminal` flags — there is no order
column.** So this joins `description` (sub-question 3) as a field the schema cannot currently
supply, with the difference that `description` is presentational while this one drives a
**computed status**: lose it and the organization roll-up silently picks an arbitrary state.

Note it is a *progress* ordering, not a graph ordering — it cannot be derived from the Transitions,
because a graph with branches has no total order. So "derive it" is likely not available; the real
options are a column (an ADR 0002 amendment, same cost as sub-question 3's), dropping it and
changing the roll-up rule, or moving the roll-up server-side.

Routed here by ticket [03](03-state-event-identity-downstream.md) rather than decided there: it is
metadata, not identity, and this ticket already owns `ResourceStateMetadataDto`.
