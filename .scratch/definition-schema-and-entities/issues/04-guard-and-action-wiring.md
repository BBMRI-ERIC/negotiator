# Guard wiring and action wiring

Status: ready-for-agent

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

**Two separate wiring tables, not one polymorphic one.** Guards and Actions never interleave — Guards
run before a commit, Actions only after one — so a shared ordering would be meaningless, and only
Guards need a scope.

**Guard wiring** has a **nullable** transition reference: null means the Guard applies to every
Transition of the Lifecycle Definition, set means that Transition alone. Two partial unique indexes
keep the ordering column unique within each of those two scopes independently. The effective Guard
chain for a Transition — what admin tooling must eventually show — is then one query.

**Action wiring** is transition-scoped only and carries **no definition reference at all**, since the
Transition already implies it.

Both carry a string type key naming a Java strategy, and per-strategy `params` as jsonb. Runtime
domain state never travels in `params` — it reaches a strategy through the evaluation context at fire
time. Giving Actions params is what collapses today's three post-visibility Action classes into one
typed key with a scope and a flag.

The ordering column is `sort_order`, not `order`: no identifier is double-quoted in any of the fifty
existing migrations and `order` would have to be. The order *between* the two scopes is pipeline
logic, not a column.

## Acceptance criteria

- [ ] Both tables are created by **this slice's own additive migration file** (one file per slice,
      never appended to an earlier slice's file). Action wiring has no definition FK.
- [ ] Guard wiring's transition FK is nullable, and both shapes round-trip in a repository test.
- [ ] Two partial unique indexes on Guard wiring: a test inserts a duplicate `sort_order` within the
      definition-wide scope and asserts refusal, inserts a duplicate within one Transition's scope
      and asserts refusal, and asserts that the **same** `sort_order` in the two different scopes is
      accepted.
- [ ] `params` round-trips a non-trivial JSON payload unchanged, mapped as jsonb.
- [ ] A wiring row with null `params` is accepted.
- [ ] Entities and repositories are package-private.
- [ ] Full suite green, parity count unchanged.

## Notes

The frozen graph dump carries **21 of 21 Transitions with no Guard**, so no v1 data will exercise
Guard wiring — the imperative IN_PROGRESS gate it will eventually replace lives in a service today,
not in the graph. That makes these tests the *only* evidence the tables work, so do not thin them.
Full analysis in [recon-expressiveness.md](../recon-expressiveness.md) gaps 8 and 9.

## Blocked by

- [03 The transition table](03-transition-table.md)
