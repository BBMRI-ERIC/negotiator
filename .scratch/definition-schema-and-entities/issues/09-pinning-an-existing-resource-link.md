# Pinning a Negotiation Resource Link that already exists

Status: needs-triage
Routed: carried on the map under [Stage 1 after the evaluator](../../state-machine-implementation/map.md#not-yet-specified) — the **coupling** slab owns it, as the first thing that has to write a Resource's pin.

## Parent

[PRD — Definition schema and entities](../PRD.md). Filed by slice
[05](05-lifecycle-definition-pin-columns.md), which took the decision recorded below.

## What was chosen

Slice 05's ticket asks for the pin to be "an immutable field — settable when the Lifecycle starts,
never updatable afterwards", so both entities carry it as

```java
@Setter(AccessLevel.NONE)
@Column(updatable = false)
private Long lifecycleDefinitionId;
```

`updatable = false` keeps the column out of every UPDATE Hibernate generates, and
`@Setter(AccessLevel.NONE)` makes that visible at compile time instead of as a silent no-op. The pin
is therefore writable **only at insert** — through `Negotiation`'s builder, and through the new
four-argument `NegotiationResourceLink` constructor.

## The problem that leaves behind

For a Negotiation the two moments coincide: the row is inserted already in `SUBMITTED`, so its
Lifecycle starts at insert.

For a Resource they do not. `Negotiation.addResource` creates the link with a null state at
Negotiation creation time, and the Resource Lifecycle starts later, when the spawn Action runs on a
Negotiation Transition. By then the link row is already persisted, so **the spawn cannot write the
pin through the mapping at all**. `link.setLifecycleDefinitionId(...)` does not exist, and removing
`@Setter(AccessLevel.NONE)` would not help on its own: `updatable = false` would drop the value
silently, which is the worse failure of the two.

This is not hypothetical, and it is not an inference either — the domain model states it. `backend/CONTEXT.md`
defines the **Definition Version Pin** as "A Negotiation pins at creation; a Resource pins at Spawn,
since that is when its Lifecycle starts", and **Spawn** as "Nothing is created — the Resources are
already linked — so Spawn names the initialization, not an instantiation". An already-linked row
being initialized later is precisely the case `updatable = false` forbids.

## Why it was left this way rather than decided now

The slab is inert: nothing writes either pin, so no code is blocked today. `updatable = false` is the
strictest honest reading of slice 05's "never updatable afterwards", and it fails **loudly** — a
compile error or a dropped-value the moment someone tries — rather than looking fine and being wrong.
Choosing between the three options below needs the Spawn Action's shape in front of you, which this
slab does not have.

## Options, for whoever picks this up

1. **A `@Modifying` query** on a link repository that sets the column where it is null. Keeps the
   field immutable through the mapping and states in one place that the pin is written once. Needs a
   repository for `NegotiationResourceLink`, which does not exist today.
2. **Drop `updatable = false`** and enforce write-once in Java — a setter that refuses a second,
   different value. Cheapest, but moves the guarantee out of the database.
3. **Create the link pinned.** Only possible if resolution moves to the moment the Resource is
   attached rather than the moment its Lifecycle starts, which is a change to when Definition
   Resolution runs, not a mapping change.

## Trigger — when this must be revisited

The first slab that has to write a Resource's pin: the spawn Action, or the data cutover if it
backfills the link table through the mapping rather than in SQL. The cutover as specified is pure
SQL and is unaffected.

Whichever option wins, it needs no new migration — the column and its foreign key are already
correct. It is a mapping decision on `NegotiationResourceLink`.
