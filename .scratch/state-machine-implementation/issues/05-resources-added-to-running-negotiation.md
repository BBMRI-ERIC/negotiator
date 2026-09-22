# Resources linked to an already-running Negotiation

Type: grilling
Status: resolved

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

## Answer

Resolved 2026-09-22. Full rationale, the measurements behind it, and the points where the interview
changed its mind: **[../notes/05-grilling-report.md](../notes/05-grilling-report.md)**.

> **Provenance.** The questions were put by `/grilling`, but a sub-agent playing the technical owner
> answered them rather than the user, at the user's request. Every code fact below was independently
> verified; the decisions were approved by the user on close.

### The ticket's premises were wrong, and that changed the answer

- **Post-approval linking is live, tested and admin-facing**, not hypothetical.
  `PATCH /v3/negotiations/{id}/resources` reaches `ResourceServiceImpl:154`, and
  `addAnyNewResourcesToNegotiation:98-105` puts the State test in a disjunct an admin
  short-circuits, so an admin can add a Resource in **any** Negotiation State. A non-admin creator
  can add only in `DRAFT`. `NegotiationControllerTests:1501` proves it against the `IN_PROGRESS`
  seed. So "make it structurally impossible" is the expensive answer, not the cheap one.
- **A late Resource is not stateless.** It lands on `SUBMITTED` from `UpdateResourcesDTO`'s default.
- **An unpinned link is not unrepresentable.** It is the normal condition of every link between
  Negotiation creation and Spawn. The real defect is the *stated-but-unpinned* row.

### The decisions

1. **The link pin stays nullable permanently. ADR 0009 is amended.** ADR 0007 pins at Spawn while
   ADR 0009 set the column NOT NULL, and both cannot hold: link rows exist from Negotiation
   creation, so every Negotiation created after the cutover would violate it, on the ordinary path.
   ADR 0009 is the one at fault, having generalised a one-time backfill observation into a standing
   invariant. NOT NULL survives on `negotiation.lifecycle_definition_id` alone. Amended in the ADR
   rather than left as a note, and the cutover slab carries both consequences (see the map).
2. **The invariant is one-directional:** `pin IS NOT NULL ⇒ current_state IS NOT NULL`, as
   `CHECK (lifecycle_definition_id IS NULL OR current_state IS NOT NULL)` with the additive DDL. A
   biconditional was proposed and **withdrawn**: a representative can already write a State on an
   unpinned link of a `SUBMITTED` Negotiation through the Override path. The other direction needs
   no constraint, because `LifecycleContextAssembler.pinOf:111-121` already refuses to judge an
   unpinned row. The pin's moment does **not** move — no Event can be fired at a Resource until the
   parent is `IN_PROGRESS` (`ResourceLifecycleServiceImpl:142`), so a State written earlier is not a
   graph position. What changes is that **Spawn pins unconditionally** over every requested
   Resource, whether or not the link already carries a State.
3. **Late linking is supported** (sharpening questions 1 and 2). Pinning is separated from State
   initialization: every State write stays as it is today, and the pin becomes an independent write.
4. **The pin write lives inside `updateResourcesInANegotiation`**, in the same transaction as the
   membership change, through a collaborator that `SPAWN_RESOURCE_LIFECYCLES` also calls — **not**
   the Action, which by the glossary runs only after a Transition commits, and there is no
   Transition here (sharpening question 3, which had assumed reuse was free). A listener on
   `NewResourcesAddedEvent` was rejected: it makes the invariant true only after the commit, it is
   gated on `IN_PROGRESS` so it misses `PAUSED`/`CONCLUDED`/`ABANDONED`, and webhooks and
   notifications consume it.
5. **The predicate is sibling-pin, not a State name.** The model cannot ask whether a Negotiation is
   past approval: there is no total order on States, Progress Order is display-only, and ticket 12
   has not decided whether Spawn keys on `APPROVE` or on arriving at `IN_PROGRESS`. Spawn pins its
   batch atomically, so links are pinned all-or-none and a new link joins whichever side its
   siblings are on. This holds whichever way ticket 12 goes.
6. **A late Resource pins to the versions active then**, resolved again per Resource through
   ADR 0004. Inheriting "the batch's pin" is not well defined, because resolution runs per Resource,
   and a late Resource may resolve to a different *family*.
7. **A concluded Negotiation** (sharpening question 4): the link pins, the Negotiation stays
   concluded, the write is not refused. Nothing deadlocks — conclusion already happened and no
   System Event fires from a terminal State — and the Resource is pinned, so an admin can still move
   it by Override. A real product defect, and a stage-3 candidate for a Guard.
8. **No Orchestration Trigger** on the pin write (sharpening question 5). Pinning is not a
   Transition, and a membership change can only make `TERMINAL_AGGREGATION` false; removal is the
   one operation that could make it true, and it is creator-only, `DRAFT`-only, and cannot empty the
   set.

### The fact-finding the ticket asked for

`NonRepresentedResourcesHandlerImpl` creates no links — it only re-states an existing link on an
`IN_PROGRESS` Negotiation, which is therefore already pinned, so it cannot produce either impossible
row. Exactly **three** paths create a `NegotiationResourceLink`: `NegotiationServiceImpl:178` and
`CustomNegotiationBuilder:19` at creation, both correctly unpinned, and `ResourceServiceImpl:108`,
the one designed here. No admin path creates one outside those.

### What this hands on

- **[16](16-linking-a-resource-wipes-sibling-states.md)** — the sibling-State wipe, measured twice
  and split out as a fixable task. Its deadline comes from this ticket: no later than the first
  commit that writes a pin.
- **Two obligations to the cutover slab**, recorded on the map: NOT NULL on the Negotiation column
  only, and the fact that nothing writes that column yet.
- **Ticket 10** — `UpdateResourcesDTO`'s `SUBMITTED` default writes a State name a custom family
  need not have.
- **Ticket 12** — a Resource added while the parent is `PAUSED` is never contacted; and a third
  defect of ticket 12's own kind, that `NegotiationStateMachineConfig:31` declares `SUBMITTED`
  initial while `DRAFT` is entered only by direct insert, giving the v1 Negotiation graph **two**
  entry points where the glossary allows one.
- **Standing decision 8** on the map, on when a pre-existing defect becomes this effort's to fix.
- **`backend/CONTEXT.md`** — new term **Unstarted Lifecycle**, and an amended **Definition Version
  Pin**.

## Comments

**2026-09-22 — the sibling wipe is split out to [16](16-linking-a-resource-wipes-sibling-states.md).**
The grilling session ([notes/05-grilling-report.md](../notes/05-grilling-report.md)) measured a
defect that is not this ticket's subject: adding one Resource nulls `current_state` on every other
link of the Negotiation, silently and without an audit row. It is filed as a fixable task rather
than carried here, because it rests on the two measurements alone and not on any of this ticket's
design decisions, so it can land before they are confirmed. Ticket 05 still owns the deadline it
imposes on 16 — the fix must land no later than the first commit that writes a pin — and 16 records
that.
