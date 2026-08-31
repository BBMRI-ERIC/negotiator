# TERMINAL_AGGREGATION and SPAWN_RESOURCE_LIFECYCLES

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The last two strategies: the one that needs a definition source, and the one that is a name only.

**`TERMINAL_AGGREGATION`** — a Guard taking no params. It passes when every Resource of the
Negotiation is in a terminal State, asking **each Resource's own pinned Definition Version** whether
that Resource's current State carries the terminal flag. Terminal cannot be a hardcoded list, because
Resources in one Negotiation may run different definitions — which is the whole reason this is a
structural question and not a set membership test.

The evaluation context supplies the Negotiation's Resources as pinned-version-and-State-name pairs;
the terminal answer comes from the cache. That is the first real consumer of slice 07's port.

**This slice decides nothing about which States are terminal.** That is seed content, owned by the
migration slab and blocked by map ticket 12. Today's predicate counts exactly two of twelve declared
Resource States, and two States that read like ends of the road deliberately do not count — recon §7
has the list and the reasoning. Behaviour-preserving means the eventual v1 seed flags those same two;
widening to four is a behaviour change that is not this slab's to make. Build the mechanism, and say
plainly in the javadoc that the set is not yours.

**`SPAWN_RESOURCE_LIFECYCLES`** — an Action, type key and params type only. The body throws, and its
javadoc names the coupling slab. Today's spawn is not in the state machine at all but in a
notification service, and the map has already parked two obligations on the coupling slab that this
slice must not pre-empt: the relocated Action publishes a resource-lifecycles-spawned event, and must
**not** publish a resource state change event.

## Acceptance criteria

- [ ] `TERMINAL_AGGREGATION` declares no params type.
- [ ] It passes when every Resource's State is terminal in that Resource's **own** pinned version, and
      refuses otherwise with the domain-state-conflict category.
- [ ] A test covers two Resources pinned to **different** Definition Versions, where the same State
      name is terminal in one and not in the other, and asserts each is judged by its own version.
- [ ] A Negotiation with no Resources yields a defined, documented answer rather than an incidental
      one.
- [ ] The terminal answer is obtained through the cache; the strategy holds no repository and no
      `EntityManager`.
- [ ] It is exercised **only through the evaluator**, over a fixture graph that wires it. No test
      names the strategy class directly.
- [ ] The javadoc states that which States carry the terminal flag is seed content owned by the
      migration slab, and names ticket 12.
- [ ] Nothing in the slice hardcodes a Resource State name.
- [ ] `SPAWN_RESOURCE_LIFECYCLES` is registered with its type key and params type; its body throws and
      its javadoc names the coupling slab and both of that slab's publishing obligations.
- [ ] A test asserts the spawn type key resolves through the Action registry and that invoking it
      refuses.
- [ ] Divergence D3 from recon §10 is recorded in `STATUS.md` with the migration slab named as owner,
      including the two-versus-four count.
- [ ] No production code calls either strategy.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**The empty-Resource-set case is a real decision, not an edge case to shrug at.** "Every Resource is
terminal" is vacuously true of no Resources, which would conclude a Negotiation that has nothing in
it. Today's predicate has the same shape and the same latent answer. Decide it, write it down, and
note that map ticket 05 — a Resource linked to an already-running Negotiation — is the related hole
and is not this slab's.

**Registering a throwing Action is not a stub.** The type key existing is what lets the migration
slab seed a wiring row against it and the coupling slab fill in a body without also inventing a
registration. Say that in the javadoc so it does not read as unfinished work.

**Do not widen terminal to four States because it looks more correct.** A Negotiation all of whose
Resources ended in the two uncounted States is finished in every practical sense and stays in progress
for ever. That is pinned as behaviour, not endorsed, and changing it is a decision with an owner.

## Blocked by

- [07 The compiled graph cache](07-compiled-graph-cache.md)
- [08 NEGOTIATION_APPROVED and SET_POST_VISIBILITY](08-negotiation-approved-and-post-visibility.md)
