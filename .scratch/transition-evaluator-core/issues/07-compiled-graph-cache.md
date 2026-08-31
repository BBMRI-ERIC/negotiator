# The compiled graph cache

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The explicit, testable loading step that the evaluator's no-I/O boundary exists to force into
existence.

A cache holding compiled definition graphs, **keyed on the Definition Version's row id alone**. No
composite family-key-and-version lookup anywhere, and no join needed to get from a pinned Lifecycle
to its graph. It is invalidated only when a new version is published, which is safe because a version
is immutable once active — there is nothing else to invalidate on.

Where a graph comes from on a miss is an **injected port**, and this slice does not implement it. The
entity-to-graph compiler reads six tables, which would make this the first slab to read the
definition schema, oblige it to delete the inertness gate a whole slab early, and forfeit the
no-I/O-no-database gate that makes this slab reviewable on its own. The port's only implementation
here refuses from every method, with a javadoc naming the repository-backed compiler that replaces it
at cutover — the same disposable-by-design shape slab 07 used for its enum-backed catalog. That is
what keeps the Spring context buildable while the registries genuinely fold at startup.

This is the one slice that is orthogonal rather than a widening. ADR 0001 hands the evaluator a
graph, so the cache sits outside it by construction and needs nothing but the graph type.

## Acceptance criteria

- [ ] The cache is keyed on a Definition Version row id; no type in the slice accepts or exposes a
      family-key-and-version pair.
- [ ] A second request for the same id does not consult the port again.
- [ ] Distinct ids yield distinct graphs and do not collide.
- [ ] Invalidating one id causes exactly that id to be reloaded on next request, and leaves others
      cached.
- [ ] The port is a named interface with one refusing implementation, whose javadoc names its
      replacement and the slab that writes it.
- [ ] The refusing implementation throws rather than returning an absence or an empty graph.
- [ ] The Spring context builds with the refusing implementation present.
- [ ] The cache holds no repository, no `EntityManager` and no Spring Data type.
- [ ] `DefinitionInertnessGuardTest` still green at 6 — this slab reads no definition table.
- [ ] No production code calls the cache.
- [ ] No Flyway migration is added.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**Independent of slices 03–06.** This is the one slice that is orthogonal rather than a widening, so
it may be authored in parallel with any of them once 02 has landed. But **test runs must be
serialized**, because two concurrent Maven invocations against `backend/` present as roughly 150 bogus
failures.

**Do not implement the compiler here, even partially.** The map's standing handoff is that the slab
which first reads these tables deletes the inertness gate as a visible line in its diff. This slab is
not that slab, and a "just the read query" shortcut would make it one by accident.

**The refusing implementation is not a stub to be embarrassed about.** It is the honest statement
that no definition source exists yet, placed where the real one will live. Nothing calls the
evaluator, so it never throws in practice.

**Slice 09's `TERMINAL_AGGREGATION` is the first consumer**, asking each Resource's own pinned version
whether its State is terminal. That is why this slice blocks it.

## Blocked by

- [02 The thinnest evaluation, end to end](02-thinnest-evaluation-end-to-end.md)
