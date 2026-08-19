# ADR 0007 and 0009 specify a spawn and a seed the code does not have

Type: grilling
Status: open

## Question

Ticket [01](01-freeze-current-behaviour.md)'s answer closes by naming a ticket that "does not exist
yet and is the first thing stage 1 needs". This is it, now with a second defect of the same kind
found by slab [08](08-definition-schema-and-entities.md)'s expressiveness check
([recon-expressiveness.md](../../definition-schema-and-entities/recon-expressiveness.md) gap 9).

Both are **ADR contradictions, not schema gaps** — the relational schema of ADR 0002 holds every one
of these shapes without change. What is wrong is what ADR 0007 and ADR 0009 say the seed contains.

### Defect 1 — spawn (from ticket 01)

ADR 0007 specifies `SPAWN_RESOURCE_LIFECYCLES` as setting the initial State, wired to the approval
Transition alone. The code does neither: it writes `REPRESENTATIVE_CONTACTED` or
`REPRESENTATIVE_UNREACHABLE`, never the graph's initial State; it publishes no
`ResourceStateChangeEvent`; and it keys on *arriving at* `IN_PROGRESS`, so
`PAUSED --UNPAUSE--> IN_PROGRESS` spawns exactly as `APPROVE` does. ADR 0009 seeds against that
specification and `backend/CONTEXT.md`'s **Spawn** entry repeats it. None of the three was edited,
per the binding constraint.

### Defect 2 — the Action list (new)

ADR 0009 enumerates the seed's Action wiring as "the single `SPAWN_RESOURCE_LIFECYCLES` Action on the
approval Transition". The graph dump carries **three** real transition Actions and no spawn at all:
`EnablePublicPostsAction`, `EnablePrivatePostsAction`, `DisablePostsAction`. A seed written to ADR
0009's list verbatim would silently drop all three — and per before-picture finding 8, post
visibility is the **only** thing distinguishing the two `ABANDON` routes from each other.

ADR 0002 already anticipates the fix in shape — it says the three post-visibility Action classes
collapse into one `SET_POST_VISIBILITY` type with a scope and a flag, which is exactly what
per-strategy `params` is for. So the decision is about ADR 0009's seed content, not about the wiring
table.

### The decision

For each defect: is the ADR amended to match the code, or is the code's behaviour deliberately
changed to match the ADR — and if the latter, does that change survive the parity gate, which
requires the characterization suite to pass **unchanged**?

`SPAWN_RESOURCE_LIFECYCLES` keying on arrival at `IN_PROGRESS` rather than on `APPROVE` is the sharp
end: ticket [02](02-state-triggered-behaviour-location.md) has already committed the spawn half of
`NegotiationInProgressHandler` to becoming an Action, so whatever is decided here is what that Action
is wired to.

### Note

Slab 08 is unaffected — it builds tables, and the tables express every shape above. This ticket must
resolve before the seed SQL is written (the migration slab) and before the coupling slab wires
`SPAWN_RESOURCE_LIFECYCLES`.
