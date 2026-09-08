# The compiled-graph cache

Status: resolved

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

- [x] A given Definition Version is compiled **once** across repeated requests for it, so assembling
      a context with one graph per Resource does not recompile the same version repeatedly.
- [x] Identity is the row id alone. No method anywhere takes a family key and a version number
      together, and a test asserts the cache's key type.
- [x] The cache holds **no lock while a graph is being produced**, asserted by a test rather than by
      reading the code — a producer that blocks until a second thread has entered the cache must not
      deadlock.
- [x] A producer that throws is **not** cached: a second call retries, and a definition fixed after
      a failed compile is served without a restart.
- [x] Invalidating one version evicts that version's entry and no other.
- [x] The cache is **package-private** in the definition package, and nothing in production calls it
      (D19). No repository gains a load-the-whole-version-by-id query in this slice — that query
      reads the definition tables, which is what deletes the inertness guard, and it is the cutover
      slab's.
- [x] The named exception from [04](04-one-named-exception-for-an-impossible-graph.md) propagates
      out of the cache unwrapped, so a caller catching graph corruption still catches it through the
      cache.
- [x] `@RequiredArgsConstructor` per D11, matching
      [02](02-lombok-where-it-fits.md) if that slice has landed.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

The "no lock across production" criterion is the one that is easy to write and easy to fake. A test
that merely calls the cache twice from two threads will pass against `computeIfAbsent` as well.
Make the producer itself observe re-entry — have it block until a second thread has demonstrably
reached the cache — so the test can only pass if the lock genuinely is not held.

## Blocked by

- [04 — One named exception for a graph that cannot exist](04-one-named-exception-for-an-impossible-graph.md)

## Outcome

Two new files, both in the definition package: `CompiledGraphCache` and its test. Nothing else in
the tree changed, which is what D19 asked for — no repository gained a load-the-whole-version-by-id
query, and the definition inertness guard therefore still holds without amendment.

The cache is `LongFunction<CompiledGraph>` in, `CompiledGraph` out, over a `ConcurrentHashMap<Long,
CompiledGraph>`: `graphFor(long)` reads, produces **outside** the map, then puts if absent and hands
back whichever graph won the put; `invalidate(long)` removes one entry. No load port, as D10
directed — and the producer that would justify one cannot exist in this slice anyway, since loading
a version's rows reads the definition tables.

**The no-lock criterion was run red against `computeIfAbsent` before being trusted.** That matters
more here than on the other rules, because the Notes are right that this is the easy criterion to
fake. Both callers ask for the **same** version — two different ids usually land in different bins
of a `ConcurrentHashMap` and would not serialize even under `computeIfAbsent`, so a two-id test
would have proved nothing — and the first production blocks until a second caller has demonstrably
reached the producer. Against `computeIfAbsent` the test fails in 5.2 seconds with its own message
rather than hanging the suite, which is the property that makes it safe to keep: the blocked
production's wait (5s) is deliberately shorter than the caller's wait on its future (30s), so a
broken cache reports the rule it broke instead of a bare timeout.

Every other failure path was likewise run red first: the identity and package-private rules against
a deliberately public class carrying `graphFor(String familyKey, int version)`; the
exception-propagation and failure-not-cached rules against a cache that cached failures and wrapped
them in an `IllegalStateException`.

### One rule beyond the criteria, and why it is here

**A graph whose own `definitionVersionId()` disagrees with the id asked for is refused rather than
filed under that id.** No criterion asked for this. It is here because the cache is the only place
the check can live: a producer asked for version 5 that reads version 7's rows compiles a graph that
is internally consistent, so the compiler's own version-mixing refusal — which compares rows against
`rows.definition()`, not against the id anyone asked for — sees nothing wrong with it. Only
`graphFor` knows which version was asked for, and it is the caching step rather than the compile
that would make the wrong answer durable: one version's graph served to a Lifecycle pinned to
another, silently, for as long as the entry lived. That is precisely what the Definition Version Pin
exists to prevent, so the headline criterion — identity is the row id alone — is what the check
defends.

**A tension it creates, recorded because this is the only record.** [04](04-one-named-exception-for-an-impossible-graph.md)
settled that a programming error in a *strategy* is not `InvalidGraphException`; that is why the
verdict type's own refusal kept `IllegalArgumentException`. A misreading producer is that same
shape, and naming it `InvalidGraphException` slightly widens what the cutover slab's "catch graph
corruption and nothing else" clause will catch. It is kept deliberately: the caller's response is
identical either way — a 500 and a log line, never a 403, 422 or 409 — and a second type for one
case would be the split 04 removed. Whoever writes that catch clause should know the set includes
this.

### Identity, and what asserts it

`theCache_isKeyedOnTheRowIdAlone` reflects over the class rather than over a call, because a green
call cannot show a reader that a row id is the *only* thing the cache accepts. Non-private methods
must take exactly `[long]`. Constructors are read under a weaker rule — one legitimately takes the
producer — asserting only that no `String` or `int`/`Integer` is among their parameters: a cache
handed a family key at construction is keyed on the composite identity just as surely as one taking
it per call, and `getDeclaredMethods` never sees a constructor. The constructor half came from the
review and was itself run red against `CompiledGraphCache(String, int, LongFunction)`.

### Gates

**The parity half is unchanged at 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped — exact.**
The one skip is `dump.LifecycleGraphDumpGeneratorTest`, as the gate specifies. It ran as the only
Maven process in the worktree, per slice 06's lesson, and passed first time.

The review's fixes all landed on the test file; `CompiledGraphCache.java` is byte-identical to what
the gate ran, verified by an empty `git diff` against the pre-review commit. The lifecycle package —
21 classes, including both structural gates — is green after them.

### Left to another slice, deliberately

`CompiledGraphCache` is **not** in `DefinitionInertnessGuardTest.DISTINCTIVE_TYPE_NAMES`. Neither is
the compiler or its input record: that name-list amendment is [03](03-structural-gates-and-topology-fixtures.md)'s
criterion and 03 is still open, so adding the cache here would have meant two slices editing one
list in parallel. **03's criterion has been amended to name the cache alongside the compiler and its
input record**, since it was written before this type existed and nothing else owned the omission.
While the type stays package-private the guard's package rule already catches the only spelling a
production caller could compile; the name list is what catches a string or reflection reference, and
a later slice making the type public.
