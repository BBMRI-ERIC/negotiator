# Guards enter the pipeline

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The first widening of slice 02's path: the Guard stage, and the registry that makes which Guards
apply a matter of data.

A Guard strategy contract, self-describing — each strategy declares its own string type key and its
own params type. A registry folded from a constructor-injected list of strategies, following the
mechanism of the existing webhook event mapper exactly: a private static fold, an insert-if-absent
check, an exception thrown **from the constructor** naming the key and both colliding implementation
class names, and an immutable copy to freeze the registry after startup. Throwing from the
constructor is what makes a duplicate type key fail the boot as a bean creation failure.

The compiled graph gains Guard entries, each carrying its type key, its sort order and its scope —
definition-wide, spelled as the absence of a Transition reference, or one Transition alone. The graph
composes the effective chain for a Transition: **definition-level entries first, then Transition
entries, each scope ordered by its own sort order.** Composition belongs to the graph because the
sort orders sequence within a scope while the order *between* scopes is pipeline logic, not a column.

The pipeline runs the chain third, after Required Authority, short-circuiting at the first failure
and refusing with the domain-state-conflict category. Tested with simple in-test strategies that pass
or fail on command; the real ported strategies arrive in slices 08 and 09.

## Acceptance criteria

- [ ] A Guard strategy declares its own type key and its own params type; the registry reads both off
      the strategy rather than being told them.
- [ ] Two strategies declaring the same type key fail **construction**, and the message names the key
      and both implementation class names.
- [ ] The registry is immutable after construction.
- [ ] A definition-level Guard entry applies to every Transition of the Definition Version, including
      one added to the fixture graph afterwards.
- [ ] A Transition-scoped entry applies to that Transition alone.
- [ ] The effective chain places every definition-level entry before every Transition entry, each
      group in sort order.
- [ ] The same type key wired twice in one definition at different sort orders takes effect twice.
- [ ] The chain short-circuits: given two failing Guards, only the first is consulted.
- [ ] A Guard failure refuses with the domain-state-conflict category and carries the Guard's reason
      code.
- [ ] Required Authority is still evaluated before any Guard, demonstrated by a case that would fail
      both.
- [ ] Slice 02's agreement test between `mayFire` and Possible Events still holds with Guards wired.
- [ ] Registry construction is exercised in a test with no Spring context.
- [ ] No production code calls the evaluator or the registry.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**The precedent is the mechanism, not the key type.** The webhook event mapper's registry is keyed on
a class, and no string-keyed strategy registry exists anywhere in this backend. Recon §8 records
this; "exactly as the webhook mapper does" reads as though a keyed-by-string example were sitting
there to copy, and it is not.

**Strictness is a decision.** The notification listener folds the same shape into a multimap with no
duplicate-key failure, because multiple handlers per event are legal there. Guards are not. Say so
where the fold lives.

**Do not put the definition-level-before-Transition ordering in the pipeline.** It belongs to the
graph, so the pipeline does not re-derive it on every call and admin tooling can show the effective
chain as one answer.

## Blocked by

- [02 The thinnest evaluation, end to end](02-thinnest-evaluation-end-to-end.md)
