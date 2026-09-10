# Compiled Graph resolution: the definition package gets an interface

Type: task
Status: open
Blocked by: 09

## Question

Give the definition package a public interface that answers **one Definition Version id → one Compiled
Graph**, with loading, Definition Compilation and caching behind it. This is the step ADR 0001 asked
for and [slab 09](09-transition-evaluator-core.md) deliberately left as a hole: "an evaluator that
structurally cannot query the database makes loading the definition graph an explicit, testable
step."

Raised by an architecture review of the lifecycle module (2026-09-09), which found the seam sitting
in the middle of the implementation rather than at the package edge.

### Why there is a ticket here at all

The pieces exist and none of them can be reached or assembled:

- **The definition package has 19 types and not one public type.** `DefinitionCompiler` and
  `CompiledGraphCache` are package-private *and are not Spring beans* — no `@Component`, no
  `@Service` — so nothing in production constructs either.
- **`CompiledGraphCache` takes a `LongFunction<CompiledGraph>` producer that nothing supplies.** Its
  own javadoc says so: "nothing in production passes one yet, because that load reads the definition
  tables and is the cutover's to write." One test lambda is the only adapter that has ever existed.
- **`DefinitionVersionRows` has no populator.** Also deliberate, also documented: "No repository in
  this package has a 'load the whole graph by definition id' query."
- **The Definition Version Pin has nothing to hand itself to.** `Negotiation` and
  `NegotiationResourceLink` each carry the pin as a bare nullable `Long` — deliberately not an
  association, because an association would let a consumer traverse into the definition graph. So the
  caller holds a version id and the package that could turn it into a graph exposes no way to.
- **`DefinitionResolver` cannot be used from outside either**, even if it were made public: it
  answers in `LifecycleDefinition`, which is itself package-private.

So the seam is currently a function type in the middle of the implementation, with zero production
adapters, rather than an interface at the package edge. Nothing outside `lifecycle` can begin a
Lifecycle, and [ticket 15](15-firing-an-event-through-the-new-subsystem.md) cannot start.

### What to build

- **One public interface on the definition package** answering `graphFor(definitionVersionId)`.
  Loading a version's rows, compiling them and caching per version id all sit behind it. The
  existing compiler and cache become internals, not collaborators a caller wires together.
- **The loader.** Reads exactly one Definition Version's States, Events, Transitions, Guard Wiring
  and Action Wiring, and produces `DefinitionVersionRows`. One transaction, and a query count the
  slab pins in a test — the cache's javadoc already anticipates "six queries in a transaction", which
  is why it deliberately avoids `computeIfAbsent`.
- **A batched resolution for a Negotiation's Resources.** Assembling a Negotiation-scope evaluation
  needs one graph per Resource, because terminality has to be asked of each Resource's *own* pinned
  version. The cache exists precisely for this, and its javadoc names the hazard: this is "equally
  the N-way load that ADR [0001] warned would otherwise be discovered under load." Resolving a set of
  version ids must compile each distinct version once, and a test must hold that — three Resources
  across two versions is two loads, not three.
- **Definition Resolution answers in version ids**, so that new work can find the version to pin
  without naming a package-private entity. The precedence walk stays stage 2; only the answer's type
  changes. `DefinitionResolutionException` is package-private and unmapped today — making it usable
  from outside is in scope; deciding *what an unresolvable definition does to a Negotiation approval*
  is not, and stays where the map files it.
- **Delete `DefinitionInertnessGuardTest` in this slab's diff.** The map is explicit that this
  belongs here and "belongs in its diff rather than in a quiet edit to the lists inside it" — this is
  the slab that makes production code read the definition tables, so the gate has done its job and
  goes whole.

### Slab gate

- [ ] Production code outside `lifecycle.definition` resolves a Compiled Graph from a Definition
      Version id, and a test proves it against rows the test wrote.
- [ ] Resolving one version twice loads and compiles once; resolving a set of version ids compiles
      each distinct version once.
- [ ] A version whose rows cannot compile fails at resolution with `InvalidGraphException`, and the
      failure is not cached — a definition fixed afterwards is served without a restart.
- [ ] Definition Resolution is callable from outside the package for both Definition Scopes.
- [ ] `DefinitionInertnessGuardTest` is deleted, and `EvaluatorPurityGuardTest` stays green — the
      `graph` and `evaluation` packages must still name no repository and must still not reach into
      `definition`.
- [ ] The parity half of [parity-gate.md](../parity-gate.md) green at 255 tests in 24 classes.

### Verification is by test, not by demo

**The six definition tables are empty in every environment.** Neither `db/dev/migration` nor
`db/test/migration` inserts a single row into any of them, so `DefinitionResolverImpl` throws on its
first call today and will keep throwing until ADR 0009's seed lands in the migration slab. Every
acceptance criterion above is therefore verified against rows the tests write themselves — which is
the same standard slab 09 held, and slab 08 before it.

Worth knowing while writing those tests: the two v1 graphs already exist as committed artifacts
(`src/test/resources/lifecycle/negotiation-graph-v1.json`, `resource-graph-v1.json`) and as Java
transcriptions under `characterization/service/`. A fixture that builds a real v1 Compiled Graph from
those constants gives this slab and ticket 15 a full-sized graph without a database. It is a fixture,
not a second adapter — the seam is the interface, and its second adapter is the test double ticket 15
will hold the module behind.

### Out of scope

- **The v1 seed.** ADR 0009 fixes it as frozen SQL and the migration slab owns it; whether it is
  generated from ticket 01's graph dump or transcribed by eye is still undecided in the map.
- **Any caller of invalidation.** Publishing a Definition Version is stage 3, so nothing can call
  `invalidate` yet. Leaving it package-private until a publisher exists is the recommendation — a
  public method with no caller is the same hypothetical seam this ticket is removing.
- **The precedence walk** over a Resource's own associations and its Networks' — stage 2, ADR 0004.
  `resolveForResource()` gaining a Resource parameter is a signature change that belongs there.
- **Building the index on either pin column**, and setting them NOT NULL. The map assigns both to the
  cutover slab, in the migration that backfills.
- **Reading a graph to answer anything about a live Lifecycle.** That is ticket 15.

### Note

Read ADR [0001](../../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md) and
[0003](../../../backend/docs/adr/0003-definition-versioning-and-identity.md) first, and the javadoc on
`CompiledGraphCache` and `DefinitionVersionRows` — both argue at length for the shape they currently
have, and both name this ticket's work as the thing that was missing. `/codebase-design` is
appropriate: the whole ticket is a seam-placement decision.

One argument to engage with rather than skip. The cache's javadoc rejects a load port on the grounds
that "a port with one adapter is indirection; the interface arrives when a second adapter does." That
argument holds, and this ticket does not contradict it — it adds no port *inside* the module. It gives
the module itself an interface, which is a different thing from injecting the producer through one.
