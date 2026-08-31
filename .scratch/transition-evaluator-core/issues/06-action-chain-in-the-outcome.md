# The Action chain in the outcome

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The other half of the widening: what a permitted outcome *says*, as opposed to what the pipeline
checks.

An Action strategy contract and its registry, the same self-describing shape and the same fold as the
Guard registry — duplicate type keys failing construction, naming the key and both classes. Two
registries, not one polymorphic one: Guards run before a commit and Actions only after one, so they
never interleave and a shared ordering would be meaningless.

The compiled graph gains Action entries. Action wiring is **always transition-scoped** — it carries no
definition reference at all, since the Transition already implies it — and each entry carries its type
key, its bound params and its sort order within that Transition's chain.

A permitted outcome now carries the target State **and the ordered, params-bound Action chain** the
committing service must run afterwards. **The evaluator runs nothing.** It reports what must run; the
running belongs to the services around it, and this slab commits nothing.

Reporting the chain rather than staying silent about it is what puts Action ordering and Action params
binding under the primary test seam. The alternative leaves the cutover slab to look the chain up off
the graph itself, with neither exercised anywhere here.

## Acceptance criteria

- [ ] An Action strategy declares its own type key and its own params type.
- [ ] Two Action strategies declaring the same type key fail construction, naming the key and both
      implementation class names.
- [ ] The Guard and Action registries are separate types with independent key spaces, so the same
      string may name a Guard and an Action without collision.
- [ ] Action entries are transition-scoped only; nothing in the slice allows a definition-scoped
      Action.
- [ ] A permitted outcome carries the Action chain for its Transition, in sort order, with params
      already bound.
- [ ] A refused outcome carries no Action chain.
- [ ] A Transition with no Actions yields an empty chain rather than an absence to branch on.
- [ ] The same Action type key wired twice on one Transition at different sort orders appears twice in
      the chain, in order.
- [ ] The evaluator invokes no Action; a test asserts a strategy wired into a fixture graph is
      reported and not executed.
- [ ] No production code calls the evaluator or the Action registry.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**Independent of slice 05**, which widens the other half of the same path — that one adds a pipeline
stage, this one extends the permitted outcome. Either order; no dependency either way. But **test runs
must be serialized**, because two concurrent Maven invocations against `backend/` present as roughly
150 bogus failures.

**The evaluator reporting Actions is not the evaluator running them.** Keep that distinction visible
in the naming, because it is the one a later reader is most likely to collapse.

**Two registries rather than one is ADR 0002's decision, with a reason** — Guards and Actions never
interleave, so a shared ordering would be meaningless and only Guards need a scope. Do not
generalize them into one.

**The empty chain matters.** Five of the eight Negotiation Transitions carry no Action today, so the
common case must not be an `Optional` a caller forgets to handle.

## Blocked by

- [04 Params bind at load time](04-params-bind-at-load-time.md)
