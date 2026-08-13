# Resources linked to an already-running Negotiation

Type: grilling
Status: open

## Question

ADR 0007 hands this to implementation explicitly, in its own words:

> **A Resource linked to an already-running Negotiation** would bypass Spawn and start unpinned; today's model links Resources up front, so this is flagged for the implementation effort rather than designed here.

Design it.

### Why it is a real hole

ADR 0007 makes Spawn a wired-once `SPAWN_RESOURCE_LIFECYCLES` Action on the Negotiation definition's approval Transition. Per requested Resource it resolves the Definition Family, pins the version on the `NegotiationResourceLink`, and sets the initial State — atomically, "against the final Resource set". Any Resource that arrives *after* approval never passes through that Action, so its link has:

- no Definition Version Pin — and ADR 0003 makes the pin **immutable and NOT NULL** after ADR 0009's migration, so an unpinned link is not merely odd, it is unrepresentable;
- no State — so it cannot appear in Possible Events, and ADR 0007's `TERMINAL_AGGREGATION` Guard asks "is every Resource of this Negotiation in a terminal State", which an unpinned Resource cannot answer.

That last point is the sharp end: **a single late-added Resource can permanently block conclusion**, because the Guard can never pass for it.

### Establish the facts first

Before designing, find out whether a Resource can actually be added post-approval today:

- `negotiation/dto/UpdateResourcesDTO` and its service path — what does it permit, and in which Negotiation states?
- `governance/resource/NonRepresentedResourcesHandlerImpl` — it references both state enums and may add or re-point links.
- Does any admin path create a `NegotiationResourceLink` outside negotiation creation?

If it is genuinely impossible today, the decision may be to **make it structurally impossible** rather than to support it — which is a legitimate and much cheaper answer, but it must be enforced, not assumed.

### Sharpen at least

1. **Is post-approval linking supported at all?** Forbid it (reject the write), or support it?
2. **If supported, when does the late Resource pin and initialize?** At link time, resolving against the versions active *then* — which means two Resources in one Negotiation pin to different versions of the same family, legal under ADR 0003 but worth stating. Or does it inherit the pin of the Spawn batch?
3. **Where does the logic live?** Reusing `SPAWN_RESOURCE_LIFECYCLES` for a single Resource keeps one code path (ADR 0007 already localizes all of it there). A separate path duplicates resolution logic.
4. **What if the Negotiation has already concluded?** Linking a Resource to a terminal Negotiation either reopens it, starts a Lifecycle that can never conclude, or is rejected.
5. **Does the answer interact with Feedback?** ADR 0007 re-attempts the parent's System Events after any Resource Transition commits. A newly initialized Resource is a state change — should it trigger a Feedback attempt (which would correctly *fail* `TERMINAL_AGGREGATION` and change nothing), or not fire at all?

### Note

The predecessor map lists "late-added resources" as **flagged, not ruled out of scope** — check ticket [09-lifecycle-coupling](../../state-machine-redesign/issues/09-lifecycle-coupling.md)'s Deferrals section for reasoning already done, so it is not re-derived here.

Use `/grilling` + `/domain-modeling`.
