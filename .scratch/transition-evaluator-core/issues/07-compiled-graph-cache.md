# The compiled-graph cache

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The one piece of this slab that neither prototype built. Map ticket 09 requires it and ADRs 0001 and
0003 fix its shape, so prototype B's deferral is not carried (D10).

A cache holding one compiled graph per Definition Version, **keyed on the version's row id alone**.
No composite `(family, version)` key exists anywhere in this subsystem, and no join exists between a
pinned Lifecycle and its graph — ADR 0003 makes the row id the sole machine identity, and a cache
keyed any other way would quietly reintroduce the composite lookup the ADR removed. It is unbounded:
published versions are immutable and are retained for work pinned to them, and a size policy needs
production cardinality evidence nobody has yet. It supports **targeted invalidation**, so publishing
a new version evicts that version's entry and leaves every other pinned version untouched.

**The concrete reason it exists is the Feedback Guard.** Terminality must be asked of each Resource's
*own* pinned Definition Version, so a caller assembling a Negotiation-scope evaluation context
resolves one graph per Resource before it calls the evaluator. That is exactly the explicit, testable
load step ADR 0001 wanted in place of an engine that owns its persistence — and exactly the N-way
load the same ADR warned would otherwise be discovered under load.

Two things it deliberately does **not** do:

- **No load port.** A port with one adapter is indirection; prototype A's own artifact flagged this
  and B declined to declare one for the same reason. The cache names its producer directly. The
  interface arrives when a second adapter does.
- **No `computeIfAbsent`.** That runs the producer inside the map's per-bin lock — harmless against
  a test lambda, wrong against the real loader, which will issue six queries in a transaction.
  Read, produce **outside** the map, then put-if-absent. Two threads may compile one version twice;
  compilation is pure and idempotent, so that is duplicated work rather than a wrong answer, which
  is the right trade against a lock held across I/O.

## Acceptance criteria

- [ ] A given Definition Version is compiled **once** across repeated requests for it, so assembling
      a context with one graph per Resource does not recompile the same version repeatedly.
- [ ] Identity is the row id alone. No method anywhere takes a family key and a version number
      together, and a test asserts the cache's key type.
- [ ] The cache holds **no lock while a graph is being produced**, asserted by a test rather than by
      reading the code — a producer that blocks until a second thread has entered the cache must not
      deadlock.
- [ ] A producer that throws is **not** cached: a second call retries, and a definition fixed after
      a failed compile is served without a restart.
- [ ] Invalidating one version evicts that version's entry and no other.
- [ ] The cache is **package-private** in the definition package, and nothing in production calls it
      (D19). No repository gains a load-the-whole-version-by-id query in this slice — that query
      reads the definition tables, which is what deletes the inertness guard, and it is the cutover
      slab's.
- [ ] The named exception from [04](04-one-named-exception-for-an-impossible-graph.md) propagates
      out of the cache unwrapped, so a caller catching graph corruption still catches it through the
      cache.
- [ ] `@RequiredArgsConstructor` per D11, matching
      [02](02-lombok-where-it-fits.md) if that slice has landed.
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

The "no lock across production" criterion is the one that is easy to write and easy to fake. A test
that merely calls the cache twice from two threads will pass against `computeIfAbsent` as well.
Make the producer itself observe re-entry — have it block until a second thread has demonstrably
reached the cache — so the test can only pass if the lock genuinely is not held.

## Blocked by

- [04 — One named exception for a graph that cannot exist](04-one-named-exception-for-an-impossible-graph.md)
