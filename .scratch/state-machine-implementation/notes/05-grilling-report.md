# Ticket 05 grilling report

Session of 2026-09-21, against
[issues/05-resources-added-to-running-negotiation.md](../issues/05-resources-added-to-running-negotiation.md).

> How this session was run, and what that means for the weight of it.
> The user asked for a role swap. The questions were put as usual, but a sub-agent playing the
> technical owner answered them, not the user. No human has confirmed anything below. Every code
> fact is independently verified and is as reliable as any other fact in this tracker. Every
> decision is a proposal that still needs the user's yes. The ticket is therefore left `claimed`,
> not `resolved`.

## Why the ticket's framing did not survive

Ticket 05 was written from ADR 0007's deferral, and three of its premises are wrong. Establishing
that took two code searches and two throwaway integration tests, and it changed the design tree
before the first question could be asked.

| Ticket 05 claimed | Established |
| --- | --- |
| A late Resource has "no State, so it cannot appear in Possible Events" | False. It lands on `SUBMITTED` from `UpdateResourcesDTO`'s default, on a Negotiation in any State |
| An unpinned link "is not merely odd, it is unrepresentable" | False. Unpinned is the normal condition of every link between Negotiation creation and Spawn |
| "Make it structurally impossible" is "legitimate and much cheaper" | The opposite. It is the expensive answer, because it removes a live endpoint behaviour, a webhook mapping and a notification handler |
| The hole is a late-link corner case | Understated. The same broken row is reached with no late link at all, by a representative, on any `SUBMITTED` Negotiation |

## Facts established this session

All verified on `feat/state-machine-implementation`.

Post-approval linking is live, admin-facing and tested. The endpoint is
`PATCH /v3/negotiations/{id}/resources`, which reaches
`ResourceServiceImpl.updateResourcesInANegotiation:154`.
`addAnyNewResourcesToNegotiation:98-105` puts the State test in a disjunct that an admin
short-circuits, so an admin can add Resources in any Negotiation State. A non-admin creator can add
only in `DRAFT`. `NegotiationControllerTests.addResources_correctPayload_resourcesAdded:1501` proves
it against the `IN_PROGRESS` seed. There is no `@PreAuthorize` on the controller, and the HTTP layer
requires only `.authenticated()`.

Exactly three paths create a `NegotiationResourceLink`. Two of them are
`NegotiationServiceImpl:178` and `CustomNegotiationBuilder:19`, both at Negotiation creation and
both correctly unpinned. The third is `ResourceServiceImpl:108`. No admin path creates one outside
those.

Nothing in `src/main` writes either pin. Only the four-argument `NegotiationResourceLink`
constructor can, and no production caller exists. `EventFiringIntegrationTest:515` writes it by raw
SQL because no writer exists. `Negotiation.java:121` declares the sibling field, and nothing writes
that either.

The sibling wipe, measured. Two temporary integration tests, non-transactional, reading committed
rows back through `JdbcTemplate`. Both are now deleted. In each, an admin adds one not-yet-linked
Resource, and a pre-existing sibling was pre-set to `RESOURCE_MADE_AVAILABLE` with pin 1.

| | `IN_PROGRESS` (negotiation-1) | `SUBMITTED` (negotiation-5) |
| --- | --- | --- |
| `UPDATE … set current_state = null` emitted | yes | yes |
| Committed sibling State | `REPRESENTATIVE_CONTACTED` | `null` |
| Sibling `lifecycle_definition_id` | `1`, preserved | `1`, preserved |
| `NewResourcesAddedEvent` | fired | not fired |
| Notifications | 2 (representatives 103, 109) | none in the whole table |
| Lifecycle record | re-contact only, never the wipe | none |
| DELETEs on the link table | none | none |

Adding one Resource resets every sibling's State without announcing it. On a Negotiation that is not
`IN_PROGRESS`, the `NULL` commits, with no event, no notification and no audit row. There is no
delete-and-reinsert. Hibernate's orphan detection works on identifiers, so the replacement instance
merges onto the existing row, and the `UPDATE` Hibernate emits leaves the pin column out. That is
why `@Column(updatable = false)` is what saves the pin.

The parity gate does not cover this path.
`EnumBackedLifecycleTestAdapter.overrideResourceStates:95-106` is the characterization suite's only
caller of `updateResourcesInANegotiation`, and it passes already-linked ids, so `containsAll` is
true and `setResources` is never reached. Nothing in the 255 tests adds a Resource in any State.

## Decisions, with rationale

### D1. The Resource-link pin stays nullable, and ADR 0009 is wrong

ADR 0007 pins at Spawn. ADR 0009 §18 and §28 then set the column NOT NULL. Both cannot hold. Links
exist from Negotiation creation and Spawn runs at approval, so every Negotiation created after the
cutover would violate the constraint. This is the ordinary path, with no late link involved.

ADR 0009 is the one at fault. It generalised a one-time observation about the backfilled rows into a
standing schema invariant it never argued for. The minimal correction is that a
`NegotiationResourceLink` is backfilled only where it already holds a State, and its column stays
nullable. A NULL there is the positive fact that Spawn has not run. NOT NULL survives on
`negotiation.lifecycle_definition_id` only.

Two alternatives were rejected. Pinning at creation is destroyed by any draft edit, and it resolves
against versions and Network membership that both move before approval. A sentinel version row
corrupts the foreign key and the Compiled Graph cache key, which ADR 0003 makes the sole machine
identity.

### D2. The invariant is one-directional

The first proposal was `state IS NULL ⇔ pin IS NULL`, and the answerer withdrew it under challenge.
A representative can already write a State on an unpinned link of a `SUBMITTED` Negotiation through
the ordinary Override path, so the biconditional is false today. Enforcing it would cost either a
refused write or an early pin, and both are observable.

The surviving invariant is `pin IS NOT NULL ⇒ current_state IS NOT NULL`. A pinned link with no
State is incoherent, because a Lifecycle is progression through the States of the version pinned to
it. `CHECK (lifecycle_definition_id IS NULL OR current_state IS NOT NULL)` enforces it, and lands
with the additive DDL. The other direction needs no constraint, because
`LifecycleContextAssembler.pinOf:111-121` already refuses to judge an unpinned row rather than
inferring a graph for it.

The pin's moment does not move. Writing a State before approval does not start a Lifecycle. No Event
can be fired at a Resource until the parent is `IN_PROGRESS` (`ResourceLifecycleServiceImpl:142`,
`sendEvent:115`), so a State written earlier is not a position in any graph. What changes is that
Spawn pins unconditionally over every requested Resource, whether or not the link already carries a
State.

### D3. Post-approval linking is supported, and pinning is separate from State initialization

Forbidding loses on standing decision 1. Every narrowing the answerer could construct is either
observable or is support under another name.

Every State write stays exactly as it is today. The pin becomes a separate write, made inside
`updateResourcesInANegotiation`, in the same transaction as the membership change. It goes through a
collaborator that the `SPAWN_RESOURCE_LIFECYCLES` Action also calls, rather than through the Action
itself. A listener on `NewResourcesAddedEvent` was rejected for three reasons. It would make the
invariant true only after the commit, which for an invariant means false for a window. It is gated
on `IN_PROGRESS`, so it misses `PAUSED`, `CONCLUDED` and `ABANDONED`. And webhooks and notifications
consume it, so an integrity write should not depend on it. The glossary rules out invoking the
Action itself, because an Action runs only after a Transition commits and there is no Transition
here. This answers the ticket's sharpening question 3, which had assumed reuse was free.

The predicate is sibling-pin, not a State name. The new model cannot ask whether a Negotiation is
past approval. There is no total order on States, Progress Order is display-only, and ticket 12 has
not yet decided whether Spawn keys on `APPROVE` or on arriving at `IN_PROGRESS`. Since Spawn pins
its batch atomically, a Negotiation's links are pinned all-or-none, so a new link joins whichever
side its siblings are on. That holds whichever way ticket 12 decides.

A late Resource pins to the versions active at that moment, resolved again per Resource through
ADR 0004. Inheriting the batch's pin is not well defined, because resolution runs per Resource, and
a late Resource may resolve to a different family.

For an already-concluded Negotiation, the link pins, the Negotiation stays concluded, and the write
is not refused. Nothing deadlocks, because conclusion already happened and no System Event fires
from a terminal State. The Resource is pinned, so an admin can still move it by Override. This is a
real product defect and a stage-3 candidate for a Guard, not a stage-1 change.

The pin write pulls no Orchestration Trigger. Pinning is not a Transition, and a membership change
can only make `TERMINAL_AGGREGATION` false. Removal is the one operation that could make it true,
and it is restricted to the creator, restricted to `DRAFT`, and cannot empty the set.

### D4. The sibling wipe is fixed here, not filed

The answerer first proposed filing it, then reversed. The reason is not that the parity gate fails
to cover the path. A path the gate does not cover is a gap in the evidence, and it raises the
standard of proof rather than lowering it.

The reason is that preserving the wipe does not preserve behaviour. Today a wiped sibling row is
`(state=null, pin=null)`, which uniformly means nothing has started. Once Spawn pins, the same path
commits `(state=null, pin=1)`, the row measured above. That combination has never existed, and the
new model declares it impossible. `pinOf` would hand out a pin, and every read path would then treat
the row as a running Lifecycle standing in no State.

The fix is local. `addAnyNewResourcesToNegotiation` calls `Negotiation.addResource` per
genuinely-new Resource, which that method's own javadoc already recommends, instead of clearing and
rebuilding the collection. It must land no later than the first commit that writes a pin. While
nothing writes pins the wipe produces `(null, null)`, which is today's behaviour, so landing it
earlier is safe and preferred. This is now ticket
[16](../issues/16-linking-a-resource-wipes-sibling-states.md), split out on 2026-09-22 because it
rests on the measurements alone and needs none of D1 to D5 confirmed first.

The answerer proposed a standing decision for the map, since it governs more than this ticket. A
pre-existing defect becomes this effort's to fix exactly when this effort's own change makes that
defect produce a state the new model declares impossible. Otherwise it stays filed.

### D5. The Negotiation pin writer is filed, not owned

D4's rule, applied to itself, files this one. Ticket 05 discovered the gap but did not cause it. The
missing writer would break the first `POST /negotiations` after the cutover whether or not ticket 05
had ever been written. It goes to the cutover slab as a map obligation.

Two consequences are worth recording. `buildNegotiation:177` uses `modelMapper.map` rather than the
builder, so passing the id through the builder is not available. And a `@Modifying` follow-up cannot
pin a Negotiation at all, because PostgreSQL checks NOT NULL at statement end and the INSERT fails
first. The two pins therefore need different writers, a `@Modifying` query for the link and a
write-once setter for the Negotiation. That is a direct cost of keeping NOT NULL in D1.

The ordering is exact. The writer cannot precede the cutover either, because the v1 seed lives
inside the cutover file, so before that file runs there is no Global Default Family for ADR 0004's
totality guarantee to resolve against. The writer is correct at one moment only, after the seed and
before NOT NULL, which is inside ADR 0009 §28's stated order.

## Net effect on stage 1

One thing changes observably. The sibling wipe stops, so siblings keep their States, no Lifecycle
Record is written for a wipe or a re-contact, and notifications for this path drop to zero. Today's
single notification exists only because the wipe made a sibling stateless and the handler
re-contacted it. Nothing changes for `NewResourcesAddedEvent` or its webhook mapping, which is what
consumers depend on.

Everything else is write-only columns and constraints nothing reads yet.

## Obligations this session creates

- Ticket [16](../issues/16-linking-a-resource-wipes-sibling-states.md) carries the wipe fix, the
  delta test and the `parity-gate.md` count updates. The two entries below are restated there in
  full.
- `characterization.delta.IntendedDeltasLinkingWillStopWipingSiblingsTest`. Record today's measured
  behaviour before the fix lands. Tag it `intended-delta` with the existing `INTENDED_DELTA`
  constant, keep it inside the characterization tree so the forbidden-reference guard still scans
  it, and name States as strings through the adapter. The two throwaway probes were the only record
  of this behaviour and they are deleted, so this test replaces them.
- `parity-gate.md` counts. Eight deltas becomes nine, the combined `263 tests in 25 classes` line
  changes, and the sentence claiming all eight are `IntendedDeltasAdr0005WillInvertTest` becomes
  wrong. Update all three in the same commit.
- ADR 0009 §18 and §28, amended per D1.
- `backend/CONTEXT.md`. Amend Definition Version Pin and add Unstarted Lifecycle. Text below.
- Map obligations. The Negotiation pin writer goes to the cutover slab, per D5. D4's standing rule
  goes in the Notes.
- One line each on tickets 10 and 12, rather than a new ticket. `UpdateResourcesDTO`'s `SUBMITTED`
  default writes a State name that a custom family need not have, which is ticket 10's question and
  belongs to stage 2. A Resource added while the parent is `PAUSED` lands on `SUBMITTED` and nobody
  is ever contacted, because `handleResourceStateManagement` is gated on `IN_PROGRESS`, which is
  ticket 12's.
- A third defect for ticket 12. `NegotiationStateMachineConfig:31` declares `SUBMITTED` the initial
  State. `DRAFT` appears only as a transition source (`:39`), never as a target, and is entered
  solely by direct insert from `NegotiationModelMapper.initialStateFor`. The v1 Negotiation graph
  therefore has two entry points where `backend/CONTEXT.md` allows one initial State. This matches
  ticket 12's existing defects, where an ADR and a glossary describe a graph the code does not have.

## Domain-modeling output, pending the user's yes

Amend Definition Version Pin by appending: "The pin is also what says whether a Lifecycle has
started at all. A Negotiation Resource Link with no pin has not been spawned, and a State value
sitting on such a link was written by an Override rather than reached through the graph."

Add under *Resolution and pinning*:

> **Unstarted Lifecycle**:
> A Negotiation Resource Link carrying no Definition Version Pin. Its Resource Lifecycle has not
> begun, so it has no graph, no Possible Events and no terminal judgement, and every attempt to
> judge it is refused rather than answered against the active version. It may still hold a State,
> because the Override path can write one before approval. That value is not a position in any
> graph, and nothing may read it as one. The converse cannot occur, because a pinned link always
> holds a State.
> _Avoid_: uninitialized resource, stateless link, orphan link

Listing "stateless link" under `_Avoid_` is deliberate. `before-picture-findings.md` uses that
phrase, and it is now wrong, because the rows that matter have a State and lack a pin.

## Where the interview moved

Recorded because the disagreement is the evidence that these answers were tested.

- The interviewer was wrong that `@Column(updatable = false)` does not protect the pin from
  `setResources`. The probe showed no DELETE and an `UPDATE` that omits the column, so the claim
  went back to the answerer.
- The interviewer was wrong that a late link on a `CONCLUDED` Negotiation sits stateless forever. It
  gets `SUBMITTED`, as the interviewer's own experiment had already shown.
- The interviewer's predicate, "a Negotiation already past approval", was replaced by sibling-pin,
  because no State-based test survives ticket 12 leaving Spawn's trigger open.
- The interviewer's argument for fixing the wipe, that the gate does not cover it and so the fix
  cannot turn it red, was rejected while its conclusion was kept on better reasoning.
- The answerer withdrew the biconditional co-invariant in D2, and withdrew the claim that parity
  obliged it to preserve the wipe in D4. Both withdrawals came under direct challenge.
