# Compiled Graph resolution: the definition package gets an interface

Type: task
Status: resolved
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

- [x] Production code outside `lifecycle.definition` resolves a Compiled Graph from a Definition
      Version id, and a test proves it against rows the test wrote.
- [x] Resolving one version twice loads and compiles once; resolving a set of version ids compiles
      each distinct version once.
- [x] A version whose rows cannot compile fails at resolution with `InvalidGraphException`, and the
      failure is not cached — a definition fixed afterwards is served without a restart.
- [x] Definition Resolution is callable from outside the package for both Definition Scopes.
- [x] `DefinitionInertnessGuardTest` is deleted, and `EvaluatorPurityGuardTest` stays green — the
      `graph` and `evaluation` packages must still name no repository and must still not reach into
      `definition`.
- [x] The parity half of [parity-gate.md](../parity-gate.md) green at 255 tests in 24 classes.

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

## Answer

**The definition package has a public interface, and it is `LifecycleDefinitions`.** Four methods —
`resolveForNegotiation()`, `resolveForResource()`, `graphFor(long)`, `graphsFor(Collection<Long>)` —
answering in `long` and `CompiledGraph`, so nothing package-private appears in a signature that
leaves the package. It and `DefinitionResolutionException` are the package's only public types; the
six entities, their repositories, `DefinitionCompiler`, `CompiledGraphCache` and `DefinitionResolver`
all stay package private.

### What was built

- **`LifecycleDefinitionsImpl`** (`@Service`) **builds the compiler and the cache itself** rather
  than injecting them, and its constructor *is* the producer the cache's javadoc has been describing
  as the cutover's to write: `new CompiledGraphCache(id -> compiler.compile(loader.load(id)))`.
  Neither internal became a bean — a cache that is a bean needs something for its producer to be,
  which is the load port the cache argued against and this ticket agreed not to add.
- **`DefinitionVersionLoader`** (`@Component`, package private), six queries in one transaction,
  one per table. `findById` runs first so the version is the instance every row's
  `lifecycle_definition_id` resolves to; the other five are ordered for the same reason. Three carry
  fetch joins — Transition to its three vertices, both Wiring tables to their Transition — because
  compilation happens *after* the transaction commits, so a lazy association there is not a slow
  path but a failure on a detached row. `action_wiring` is the one query that cannot name
  `lifecycle_definition_id` and reaches the version through its Transition, which is ADR 0002's
  shape showing up as a join.
- **Five repository finders**, two derived and three `@Query`. The Guard Wiring one is a `left join
  fetch`: a definition-wide Guard is spelled as a null `transition_id`, and an inner join would drop
  exactly the rows that apply to every edge — silently, leaving a graph that compiles and gates less
  than it was configured to.
- **Definition Resolution answers in row ids.** Only the answer's type changed. `DefinitionResolver`
  stays a package-private interface with `DefinitionResolverImpl` behind it, deliberately: it is a
  one-adapter seam and arguably redundant now that a real one sits above it, but
  [the resolver's shape](../definition-schema-and-entities/issues/10-definition-resolver-shape-is-a-guess.md)
  is an open stage-2 question and collapsing it here would answer it in passing.
- **`DefinitionResolutionException` is public with a package-private constructor** — catchable from
  outside, throwable only by resolution. It is the *expected* answer today, not a remote one, since
  the six tables are empty in every environment.
- **`DefinitionInertnessGuardTest` is deleted**, 499 lines, in this diff.

### The transaction is load-bearing, and only one kind of test can see it

`DefinitionCompiler` compares each row's owning version **by reference** and groups Wiring by
`Transition` **identity**. Six repository calls without a transaction around them are six persistence
contexts, and every one of those comparisons is then against a different instance of the same row —
so the compile refuses a perfectly good version as straddling two.

**`@DataJpaTest` cannot catch this**, because it wraps each test in a transaction and supplies the
guarantee for free. Run rather than reasoned about: with `@Transactional` removed from
`DefinitionVersionLoader.load`, `DefinitionVersionLoaderTest` stays **green in full** and five of
`CompiledGraphResolutionIntegrationTest`'s eight fail with *"These rows belong to a different
Definition Version"*. That is the whole argument for the integration test existing, and it is
written into both classes' javadoc so the next person does not delete it as redundant.

Related: `@Transactional` is on a **public method of a package-private class**. Spring's
proxy-based transaction management applies to public methods only, so a package-private one is
silently ignored — which would have produced exactly the failure above, at runtime, with no
annotation visibly missing.

### Where each criterion is proven, and why there

Three test classes, **21 tests**, because the three questions need different instruments:

- **`CompiledGraphResolutionIntegrationTest`** (8 tests) sits in `lifecycle`, **not**
  `lifecycle.definition`, and that placement is most of what it proves — every other test of this
  subsystem is inside the package it exercises, which is exactly why none of them noticed nothing
  outside could reach any of it. It writes its rows **as SQL**, because the entity builders are not
  reachable from out here; that is how the v1 seed arrives anyway (ADR 0009), and it means the
  assertions rest on the schema rather than on a mapping that agrees with itself. It commits, which
  is what makes it the transaction's only witness.
- **`LifecycleDefinitionsImplTest`** (7 tests) owns the caching criteria, with the loader mocked.
  A load is only observable by **counting**: two resolutions of one version return the same graph
  whether it was compiled once or twice, because the cache hands both callers whichever production
  won the put. "Three Resources across two versions is two loads, not three" is `verify(loader,
  times(1))` twice — it is not expressible as an assertion on the returned graphs.
- **`DefinitionVersionLoaderTest`** (6 tests) owns the queries, against real PostgreSQL. Two
  Definition Versions are written before every test and only one is ever asked for, because against
  a single-version fixture a loader with the wrong `WHERE` clause passes everything. **The six-query
  count is pinned here** via Hibernate's own statement counter, across the load *and* the compile —
  a lazy association would not show up until something dereferenced it, and compiling is what does.
  The persistence context is cleared first, or `findById` answers from the first-level cache and the
  count is short by one for an unrelated reason.

### Gates

| Criterion | Result |
| --- | --- |
| Production code outside the package resolves a graph, proven against rows a test wrote | green — `CompiledGraphResolutionIntegrationTest` |
| One version twice loads once; a set compiles each distinct version once | green — counted, not inferred |
| A version that cannot compile fails with `InvalidGraphException` and is not cached | green — refused twice, then served after one `UPDATE`, no restart |
| Definition Resolution callable from outside for both Scopes | green |
| `DefinitionInertnessGuardTest` deleted, `EvaluatorPurityGuardTest` green | green |
| Parity half at 255 tests in 24 classes | **255 / 24 classes / 0 failures / 0 errors / 1 skipped** |

### Two things done that the ticket did not list

- **`EvaluatorPurityGuardTest`'s failure message named the deleted guard.** Not a stale comment — a
  message a developer reads at the moment the rule fires, telling them to go look at a file that no
  longer exists and asserting an inertness claim that this slab ended. Rewritten to state the rule on
  its own terms. The provenance paragraphs in that class and in `RawStateNamesInSqlGuardTest` and
  `LifecycleEnumDecouplingGuardTest` now record that the sibling has gone. Worth noting that slab
  08's copy-don't-extract decision was **vindicated** here: its stated reason was that a shared
  helper "would then survive the guard it was extracted from", and the sibling went whole with
  nothing to move.
- **The javadoc on `CompiledGraphCache`, `DefinitionCompiler` and `DefinitionVersionRows` was
  updated**, because all three said in so many words that nothing populated them yet and that the
  package was inert. Those sentences named this ticket's work as the thing that was missing; leaving
  them would have made three long arguments read as current.

### Not done, and why

- **No v1 Compiled Graph fixture.** The ticket suggested one built from the committed artifacts. It
  is not built, and the argument against is in the artifacts themselves: the graph dump records an
  Action **by its Spring Statemachine bean class name**, and `NegotiationGraphV1`'s own javadoc says
  naming one in an assertion "would be a guaranteed delta dressed as parity" — so a fixture over the
  dump needs a bean-name-to-type-key mapping, which the characterization suite deliberately keeps
  local to itself. Ticket 15 should decide whether it wants that mapping; this slab did not need a
  full-sized graph to prove any of its criteria.
- **`invalidate` stays package private.** No publisher exists to call it, and a public method with
  no caller is the hypothetical seam this ticket removed.
- **No new glossary term.** Compiled Graph, Definition Compilation, Definition Resolution and
  Definition Version Pin already cover every noun here, and "loading" is already named in
  `CONTEXT.md` as the step Definition Compilation is *not*.
- **No production caller**, and this is the one place the gate had to be read rather than followed.
  Criterion 1 says "*production code* outside `lifecycle.definition` resolves a Compiled Graph from
  a Definition Version id, and a test proves it against rows the test wrote". Taken literally it
  wants a caller, and Out of scope forbids the only kind of caller there is — "reading a graph to
  answer anything about a live Lifecycle. That is ticket 15." The two cannot both be satisfied, so
  it is read as the **capability** claim the rest of the ticket argues for: the interface is public,
  the bean is real, the queries are real, and a test **outside the package** drives the whole path
  against rows it wrote. What a literal reading would have added is a caller with no purpose, which
  is the same hypothetical seam this ticket exists to remove. Say so if that reading is wrong — the
  fix is a line in ticket 15, not a change here.

### For the migration slab, when ADR 0009's seed lands

Two things in `CompiledGraphResolutionIntegrationTest` are correct **only while the six tables are
empty**, and both are written into its javadoc where the next reader will meet them:

- **Its cleanup is scoped to the versions it wrote.** It first said `DELETE FROM
  lifecycle_definition` and five siblings — correct today, and it would have stayed correct right
  up until the seed landed, at which point this class would have wiped the seed for every test that
  ran afterwards and the failure would have surfaced anywhere but here. Now it deletes by the ids it
  recorded.
- **`resolution_whenNothingIsSeeded_isRefused` is the one test the seed invalidates rather than
  joins.** Once an active version of each Scope exists, both its assertions become false —
  correctly. It is expected to be rewritten in the migration slab's diff, not repaired in passing.

### Review findings acted on

A two-axis review (standards, spec) ran over the diff. Neither axis found a hard violation or a
missed requirement. What it did find, and what was done:

- **The cleanup landmine above** — fixed, and it is the one finding that was a real defect rather
  than a judgement call.
- **`writeTransition`'s parameter order deliberately differed from the table's column order**
  (`from, event, to` against `from, to, event`) — three interchangeable `bigint`s, where a
  transposition inserts a different edge and still compiles. The column list is now written to match
  the parameters.
- **Byte-identical `@throws` javadoc** on `DefinitionResolver` and `LifecycleDefinitions` — the
  inner interface now points at the outer one instead of restating it.
- **The gate checkboxes above were still unticked** while the results table said green — ticked.
- **`DefinitionResolver` was pressed as Speculative Generality**, on the argument that the shape
  decision has already migrated up to `LifecycleDefinitions` (where `resolveForResource()` now
  appears verbatim), so stage 2's parameter addition changes that signature whether or not the inner
  interface survives — making it four edits instead of three. **Deliberately not acted on**, because
  the ticket says only the answer's type changes and the map files this seam's shape as an open
  stage-2 question. The argument is a good one and is recorded here so stage 2 does not have to
  rediscover it.
- **`LifecycleDefinitionsImpl` was labelled Middle Man** and dismissed by the reviewer on its own
  terms: Fowler's remedy is to let the caller call the delegate, and the delegates are package
  private, so there is no caller that could.

### For ticket 15

- `graphsFor` returns `Map<Long, CompiledGraph>` keyed by version id, which is what a per-Resource
  pin is looked up in. It **refuses a null id before resolving anything**, naming the unpinned
  Lifecycle — both pin columns are still nullable until the cutover backfills them, so an assembly
  collecting pins off a Negotiation's Resources will meet this.
- Nothing in `graphsFor`'s signature promises a query per version. It is a method rather than a
  documented loop precisely so a later implementation can read several versions' rows together.
- The test double the module goes behind is `LifecycleDefinitions` — four methods, no database.
