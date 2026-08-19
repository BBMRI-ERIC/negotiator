# The state and event tables

Status: ready-for-agent

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

The two vertex tables of the definition graph, both owned by a Lifecycle Definition.

A **State** is a named position carrying a human `label` plus `initial` and `terminal` flags.
Exactly one State per Lifecycle Definition is initial, and the database enforces it. `label` is not
decoration — the current notification body for a Resource state change is built from it, so a
definition whose States have no labels degrades a live notification to nulls in a sentence.

An **Event** is a named trigger. Critically, **an Event may carry no Transition at all** — that is
how the Override Event survives as a name under which an admin's direct state change appears in
history. Nothing about this table may assume an Event is reachable from a Transition.

Two shapes that look degenerate and are both real, verified against the frozen graph dump, so neither
may be constrained away:

- A **State that no Transition targets** — ADR 0009's Legacy States (`APPROVED`,
  `RETURNED_FOR_RESUBMISSION`) are exactly this, and they exist so that live strings and audit
  history still resolve after the cutover.
- A State with outgoing Transitions that nothing targets — `DRAFT` is occupied from outside the
  graph but is not enterable through it.

**Name uniqueness within a definition is load-bearing, not hygiene.** ADR 0009's cutover resolves a
live state string "through the natural key of the Definition Version Pin plus the state name", and
re-homes Information Requirements by matching the legacy `for_event` string against the Event "of the
same name". Both are natural-key lookups, so a duplicate name inside one definition would make the
cutover ambiguous. The same "exactly one initial State" caveat applies as in slice 01: a partial
unique index enforces at most one, and the at-least-one half waits for publish-time validation in
stage 3.

No `description` column on either table. That belongs to map ticket
[04](../../state-machine-implementation/issues/04-global-state-event-metadata-contract.md), as does
any Resource-State `ordinal`; adding either here would be an ADR 0002 amendment whose cost is
assessed there, not assumed here.

## Acceptance criteria

- [ ] Both tables are created by **this slice's own additive migration file** (one file per slice,
      `V36.0` onward — never appended to an earlier slice's file), each with a foreign key to
      `lifecycle_definition`.
- [ ] A partial unique index enforces at most one `initial` State per Lifecycle Definition; a test
      inserts a second one and asserts the write is refused.
- [ ] A test asserts two **non-initial** States in the same definition are accepted.
- [ ] A test persists an Event that no Transition references, and reads it back.
- [ ] A test persists a State that no Transition targets, and reads it back.
- [ ] Both `label` and `name` round-trip; a State with a null `label` is refused if the column is
      NOT NULL, and the test states which it is.
- [ ] `name` is unique within a Lifecycle Definition, for **both** tables, enforced by the database,
      with a test that inserts a duplicate and asserts refusal — and a test that the **same** name in
      two different definitions is accepted.
- [ ] Entities and repositories are package-private.
- [ ] Full suite green, parity count unchanged.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)
