# The transition table

Status: ready-for-agent

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

The edges of the graph. A Transition names its Lifecycle Definition, its from-State, its to-State,
its Event, and its **Required Authority** — one of `NONE`, `IS_ADMIN`, `IS_CREATOR`,
`IS_REPRESENTATIVE`, `SYSTEM`.

Required Authority is a **field of its own, not a Guard**. ADR 0002 is deliberate about this: asking
*who is firing* and asking *whether the move is currently legal* fail differently and must not be
expressible as the same kind of row.

The table must support the lookup the evaluator will live on — "which Transitions leave State X for
Event Y" — as an indexed query rather than a scan.

## Acceptance criteria

- [ ] The table is created by **this slice's own additive migration file** (one file per slice, never
      appended to an earlier slice's file), with foreign keys to the definition, to both States and to
      the Event.
- [ ] `required_authority` persists as a string, not an ordinal, and rejects a value outside the five.
- [ ] An index supports lookup by `(from_state, event)`.
- [ ] A repository test round-trips a Transition and reads back all five references.
- [ ] A test proves a Transition cannot reference a State or Event belonging to a **different**
      Lifecycle Definition, or — if the schema cannot express that constraint — the ticket records
      explicitly that it is unenforced and names what will enforce it.
- [ ] `(lifecycle_definition, from_state, event)` is unique, enforced by the database, with a test
      that inserts a duplicate and asserts refusal. The evaluator's answer to "which Transition
      leaves State X for Event Y" must be a single row, and the frozen dump confirms **0 duplicate
      `(source, event)` pairs** across both graphs today — so this constrains nothing real and
      prevents an ambiguity the evaluator has no way to resolve.
- [ ] Entity and repository are package-private.
- [ ] Full suite green, parity count unchanged.

## Notes

**Build `required_authority` single-valued, exactly as ADR 0002 specifies.** Map ticket
[11](../../state-machine-implementation/issues/11-transition-authority-admin-or-creator.md) records a
genuine contradiction here — six of the eight Negotiation Transitions are behaviourally
`IS_ADMIN OR IS_CREATOR` and no single enum value reproduces that. Resolving it is **not** this
slice's job, and two of its three candidate resolutions leave this DDL untouched. Do not invent a
disjunction, a set-valued column or a join table to get ahead of it.

## Blocked by

- [02 The state and event tables](02-state-and-event-tables.md)
