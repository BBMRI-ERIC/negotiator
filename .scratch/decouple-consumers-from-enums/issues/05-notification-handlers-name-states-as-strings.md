# Notification handlers name States as strings

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

Six files in the notification subsystem, migrated onto the holders and the catalog. This is ticket
02's population, and ticket 02 established that all of it decouples early: none of it needs the
Action registry, the Definition schema or the Transition Evaluator to exist.

The handlers that switch on a destination State compare against holder constants instead, keeping
their `default` behaviour exactly — four of the eight Negotiation States already notify nobody, and
a name that is absent must land in that same silent branch.

**One handler breaks in a way no ticket predicted.** The Resource state-change handler builds its
message from the human labels of both States, reading them off the enum. Ticket 02's decision 7 says
this file "needs no change here" and routes labels to ticket 04 — but the type swap deletes the
method it calls. It takes its labels from slice 2's catalog. The message text must be byte-identical.

**Two files are carve-out 2 and are handled with care.** The service holding the Spawn loop, and the
handler that triggers it, both move into an Action later — ADR 0007 takes the loop for
`SPAWN_RESOURCE_LIFECYCLES`, and ticket 02's decision 3 splits the notification off it via a new
event. **This slice swaps their four enum references for holder constants in place and changes
nothing else.** No new event, no transaction change, no restructuring, no moving the loop. The
coupling slab must find a method whose body is already string-shaped, and a relocation diff that
shows only the relocation.

The carve-out excludes a relocation, not a type swap, and it has to: the slab gate exempts only the
three metadata DTOs, and ticket 03's own holder table assigns these exact call sites here.

## Acceptance criteria

- [ ] All six notification files name no Lifecycle enum.
- [ ] The status-change handler notifies exactly the same people for exactly the same States, and
      still notifies nobody for the four States that have no branch today.
- [ ] The Resource state-change handler's message text is unchanged, with both labels sourced from
      the catalog.
- [ ] The two carve-out-2 files differ only by the constant swap — no structural change of any kind.
- [ ] Spawn still assigns the same two Resource States, still accumulates the same representatives,
      and still sends exactly one notification to their union, and none when that set is empty.
- [ ] Spawn still publishes no Resource state-change event, as ticket 01 pinned.
- [ ] The seven existing handler tests are extended rather than replaced.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**The parity hazard here is the largest in the slab.** Ticket 02 recorded that making Spawn publish
per-Resource state-change events would wake a handler with no firing condition *and* the webhook
subsystem — N extra notifications and N extra webhook deliveries per approval. Nothing in this slice
goes near that, and nothing in it should.

**Eight test files churn with this slice**, the largest test batch in the slab.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
- [02 The Enum-Backed Lifecycle Catalog](02-enum-backed-lifecycle-catalog.md)
