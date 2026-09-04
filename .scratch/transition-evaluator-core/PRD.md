# Transition Evaluator core — PRD

Status: ready-for-agent

Slab for map ticket [09 Transition Evaluator core](../state-machine-implementation/issues/09-transition-evaluator-core.md),
prototyped by [ticket 13](../state-machine-implementation/issues/13-prototype-the-compiled-graph-package.md)
(resolved) and specified by ADRs [0001](../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md),
[0002](../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md),
[0003](../../backend/docs/adr/0003-definition-versioning-and-identity.md),
[0005](../../backend/docs/adr/0005-information-requirements-gate-transitions-as-a-built-in-stage.md)
and [0007](../../backend/docs/adr/0007-lifecycle-coupling-is-orchestration.md).

**Slab gate: pure unit tests, no I/O, no database — and the parity half of
[parity-gate.md](../state-machine-implementation/parity-gate.md) unchanged at 255 tests in 24
classes.** Nothing in production calls this slab's code when it lands.

---

## Problem Statement

Three people are badly served by how a Lifecycle is judged today, and one of the three does not know
it yet.

**A requester clicks a button that then refuses them.** The Events offered for a Negotiation are
built by one piece of code and enforced by another. Whether an Information Requirement has been
filled in is checked inside the service method that fires an Event; the listing that decides which
Events to offer knows nothing about that check. So a researcher is shown an action, takes it, and is
told it failed — a dead click, every time a form is outstanding. The same split means the authority
rule is hand-checked once to build the listing and then checked again by the framework when the Event
fires, and the two can drift.

**A biobank representative gets silence instead of a refusal.** When a Resource Event is blocked
today, the service returns the Resource's current State rather than an error. Nothing tells the caller
why nothing happened, and nothing distinguishes "you may not do that", "a form is outstanding" and
"the Negotiation is not in a State where that is possible".

**An administrator cannot configure a Resource Lifecycle at all.** Both Lifecycles are Java: two
`@Configuration` classes, four enums, and hand-written Guard and Action beans. Changing which
preconditions gate a move, or giving one Network a different Resource Lifecycle from another's, means
a code change and a deploy. The redesign's whole purpose is that this becomes data — and data needs
something that can read it and answer questions about it.

**And the ground is going away.** Spring Statemachine is end-of-life: archived to `spring-attic`,
frozen at 4.0.0, commercial-only in future. Every one of the above has to be rebuilt regardless.

What is missing underneath all of it is a single component that can be handed one Definition Version
and answer, once and consistently, what it permits.

## Solution

**The Transition Evaluator**: the stateless component that answers what a Definition Version permits
from a given State — whether a particular Event may fire and which State it leads to, or which Events
could fire at all. It holds no state, reads no data of its own, and changes nothing.

Because there is exactly one of it and exactly one evaluation path through it, the dead click stops
being a bug that can be reintroduced: the Possible Events listing *is* a dry run of the same function
that gates the real move, with blocked Events omitted. The two cannot disagree, because there is
nothing for them to disagree with.

Because it is handed an already-compiled graph rather than loading one, it can be tested exhaustively
without a database, and the cost of loading a definition is an explicit step somewhere a reader can
see it — rather than an N+1 across a Negotiation's Resources, discovered under load.

Because which Guards and Actions apply is data and only their logic is Java, an administrator will
eventually change a Resource Lifecycle by writing rows.

**This slab delivers the component and changes no behaviour.** Nothing in production calls it when it
lands; the two Lifecycle services still run on Spring Statemachine, and the parity suite must pass
unchanged. The user-visible fixes above arrive in the cutover slab, which wires this component in.
That is deliberate: this is the deep module of the whole effort, and its no-I/O boundary is the
argument that makes every later slab testable.

---

## User Stories

### Judging one move

1. As a requester, I want an Event I am offered to actually fire when I click it, so that I am not
   told an action failed after being invited to take it.
2. As a requester, I want a refusal to tell me which kind of thing is wrong, so that I know whether
   to fill in a form, ask an administrator, or wait for someone else.
3. As a biobank representative, I want a blocked Resource Event to refuse me rather than silently
   return my current State, so that I can tell "nothing happened" from "that is not allowed".
4. As a requester, I want the reason a move was refused to name the earliest thing wrong rather than
   the last thing checked, so that the message I see does not change depending on how an
   administrator happened to wire a Guard.
5. As a frontend developer, I want the refused outcome to carry a stable reason code and a details
   map, so that I can render a specific message without parsing prose.
6. As a frontend developer, I want the failure category to map cleanly onto one HTTP status, so that
   the controller layer needs no rules of its own about which refusal means what.
7. As a backend developer, I want the Evaluation Pipeline to short-circuit at the first failure, so
   that a Guard is never asked a question about a caller who was never allowed to ask it.
8. As a backend developer, I want a permitted outcome to be a judgement and not a commit, so that
   evaluating twenty candidate Events does not set twenty posts' visibility.
9. As a backend developer, I want a permitted outcome to carry the ordered Action chain rather than
   run it, so that the service that commits the move owns when effects happen.

### Listing what is possible

10. As a requester, I want the Events listed for my Negotiation to be exactly the ones I may act on
    now, so that the interface never offers me something that will be refused.
11. As a requester, I want a blocked Event left out of the listing rather than listed as unavailable,
    so that the list is short and every entry is actionable.
12. As a frontend developer, I want the Possible Events endpoint to keep its current shape, so that
    no new endpoint or DTO is needed to get a correct answer.
13. As a backend developer, I want the listing and the real gate to call one function, so that a
    future change to how a move is judged cannot update one and forget the other.
14. As a maintainer, I want a test that asserts the listing and the gate agree, so that the guarantee
    is executable rather than a comment.

### Who may fire

15. As an administrator, I want who may fire a Transition to be a separate field from whether the
    domain permits the move, so that "you are not allowed" and "this is not currently possible" fail
    differently.
16. As an administrator, I want one Required Authority value to mean "any administrator" across both
    Lifecycles, so that the two spellings the old machines used cannot drift apart again.
17. As a requester, I want to be able to fire an Event on my own Negotiation where the definition says
    the creator may, so that my own work does not need an administrator.
18. As a biobank representative, I want authority over exactly the Resources I represent, so that I
    cannot act on a colleague's Resource in the same Negotiation.
19. As an administrator, I want a System Event to be unfireable by any human, over REST or anywhere
    else, so that machine-driven moves cannot be triggered by hand.
20. As an administrator, I want the Orchestration Trigger to be unable to fire an Event that merely
    has no authority requirement, so that marking an Event open to humans does not silently make it
    machine-fireable.
21. As a maintainer, I want both halves of that mutual exclusion held by the type system rather than
    by a branch, so that a later edit cannot introduce a fall-through between the machine case and the
    human rules.
22. As a maintainer, I want the authority rule to live inside the one shared evaluator rather than in
    each caller, so that the two Lifecycles cannot come to disagree about who may act.
23. As a maintainer, I want Required Authority to stay single-valued in this slab, so that the
    admin-or-creator question is answered on its own ticket rather than invented here.

### Guards and Actions as configuration

24. As an administrator, I want which Guards gate a move to be rows rather than Java, so that
    changing a precondition does not need a deploy.
25. As an administrator, I want a Guard I attach to a whole Definition Version to apply to every one
    of its Transitions, so that adding a Transition later cannot forget it.
26. As an administrator, I want definition-wide Guards to run before Transition-specific ones, each in
    the order I configured, so that the chain is predictable.
27. As an administrator, I want one post-visibility Action with configuration rather than three
    classes, so that a new visibility rule is a row and not a release.
28. As an administrator, I want a Guard's configuration validated when the definition is compiled, so
    that a bad row is caught once and loudly rather than in front of a user.
29. As an administrator, I want a misspelled field in a Wiring row's configuration to be refused, so
    that a typo cannot silently leave a Guard running with a default it was never configured with.
30. As an administrator, I want configuration supplied to a Guard that takes none to be refused, so
    that a row which looks configured is never silently ignored.
31. As a maintainer, I want two Guard beans claiming one type key to fail the boot, so that which one
    runs is never decided by classpath order.
32. As a maintainer, I want Guards and Actions keyed in separate namespaces, so that an Action wired
    where a Guard belongs is refused rather than discovered at fire time.
33. As a maintainer, I want a strategy to declare its own key and its own configuration type, so that
    adding behaviour is one class and no registry edit.
34. As a maintainer, I want the single unchecked narrowing from stored configuration to a typed value
    to live in one place, so that no other class needs a suppression.
35. As a maintainer, I want runtime domain state to reach a strategy only through the evaluation
    context, so that one Wiring row can never come to mean one Negotiation.

### Information Requirements

36. As a requester, I want an outstanding form to block the Event it guards wherever that Event is
    used, so that the rule does not depend on an administrator remembering to attach it.
37. As an administrator, I want no way to omit the Information Requirement check, so that a newly
    added Transition cannot miss it.
38. As a requester, I want an unmet-requirement refusal to tell me which forms are missing, so that I
    can act without guessing.
39. As a maintainer, I want the requirement check to speak the same contract as a Guard, so that
    callers see one uniform shape of failure.
40. As a maintainer, I want whether a form is satisfied to arrive through a port with a test double in
    this slab, so that the evaluator stays free of I/O while the real lookup is built later.

### Compiling a Definition Version

41. As a maintainer, I want the relational rows of one Definition Version turned into an
    evaluator-ready graph in one place, so that the two shapes are both known in exactly one file.
42. As a maintainer, I want compiling to be handed already-loaded rows rather than repositories, so
    that loading stays an explicit step and compilation is unit-testable.
43. As a maintainer, I want an unknown Guard or Action type key to fail the compile, so that a
    definition naming a strategy this deployment does not have never reaches a user.
44. As a maintainer, I want rows from two different Definition Versions to be refused when compiled
    together, so that a loader with a wrong `WHERE` clause cannot quietly produce a mixed graph.
45. As a maintainer, I want the definition package never to name the evaluation package, so that the
    compiler can be tested without a registry, Jackson or Spring.
46. As a maintainer, I want a compiled graph's every question to be a lookup rather than a scan, so
    that a Possible Events listing over a large graph stays cheap.
47. As a maintainer, I want a graph to be constructible by a test in one line per Transition, so that
    a fixture is readable and the invariants are still enforced.
48. As an administrator, I want a Definition Version with two Transitions leaving one State for one
    Event to be refused, so that which move fires is never decided by load order.
49. As an administrator, I want a Definition Version with no initial State to be refused, so that a
    Lifecycle can always be started.
50. As an administrator, I want a cycle, an unreachable Legacy State, and a version with no terminal
    State all to remain valid graphs, so that the definitions this subsystem exists to run are not
    rejected.
51. As an administrator, I want an Event that carries no Transition to remain declarable, so that the
    Override Event survives as a name under which a direct state change appears in history.

### Terminality and coupling groundwork

52. As a maintainer, I want terminality to be a question put to each Resource's own pinned Definition
    Version, so that two Resources of one Negotiation running different definitions are both judged
    correctly.
53. As a maintainer, I want the Guard behind Feedback to receive each Resource paired with its own
    compiled graph, so that no hardcoded list of terminal State names exists anywhere.
54. As a maintainer, I want an unfinished-Resource refusal to name which Resources are unfinished, so
    that an operator can see why a Negotiation has not concluded.
55. As a maintainer, I want `SPAWN_RESOURCE_LIFECYCLES` registered but refusing to run, so that a
    Wiring row can name it and a definition mentioning it compiles, while calling it early fails
    loudly.

### Loading and caching

56. As a maintainer, I want a compiled graph cached per Definition Version and keyed on its row id
    alone, so that no composite lookup and no join exists between a pinned Lifecycle and its graph.
57. As a maintainer, I want the cache to compile a given version once, so that assembling a context
    with one graph per Resource does not recompile the same version repeatedly.
58. As a maintainer, I want the cache not to hold a lock while a graph is being produced, so that the
    real loader's queries do not serialize behind a map.
59. As a maintainer, I want a failed compile not to be cached, so that fixing a definition does not
    need a restart.
60. As an administrator, I want publishing a new Definition Version to invalidate only that version's
    cache entry, so that work pinned to older versions is untouched.

### Corrupt data and failure modes

61. As an operator, I want a Lifecycle whose current State the pinned Definition Version does not
    declare to fail distinctly, so that a broken Definition Version Pin is not reported as an ordinary
    unavailable move.
62. As an operator, I want that failure to be one named exception type, so that it can be caught and
    logged in one place rather than being indistinguishable from any other argument error.
63. As a requester, I want a Definition Version that does not declare an Event at all to refuse
    differently from one that declares it but offers no Transition from my current State, so that a
    typo and a deliberately transition-less Event are distinguishable.
64. As an operator, I want an empty Possible Events listing to mean "nothing is available", never
    "the data underneath is broken", so that the two do not look the same from outside.

### Structural guarantees

65. As a maintainer, I want the evaluator's inability to reach a database enforced by a test rather
    than by convention, so that the constraint survives the next person.
66. As a maintainer, I want the rule to forbid any repository that does not exist yet, so that a
    future one cannot be injected here either.
67. As a maintainer, I want the graph package's dependencies expressed as what is permitted rather
    than as a list of what is forbidden, so that the rule also stops what nobody thought of.
68. As a maintainer, I want the absence of an edge from the graph and evaluation packages back into
    the definition package asserted in both directions, so that the definition package's inertness
    cannot be undermined from the far side.
69. As a maintainer, I want the definition package to remain provably unread by production code after
    this slab, so that its schema stays inert until the slab that legitimately reads it says so in its
    diff.
70. As a maintainer, I want every structural gate to fail if it scans nothing, so that a gate can
    never pass by losing its way.
71. As a maintainer, I want every gate rule proven to fire on the thing it forbids, so that a regex
    that matches nothing cannot pass forever.

### Working on this code

72. As a maintainer, I want the new code to use Lombok where the rest of this backend does, so that
    half the codebase and this subsystem do not read as two different projects.
73. As a maintainer, I want every new test method to carry a `@DisplayName`, so that a failure reads
    as a sentence like every other Lifecycle test.
74. As a maintainer, I want the compiled graph and the act of compiling it to have entries in the
    glossary, so that the term everyone is already using is defined somewhere binding.
75. As a maintainer, I want the parity suite to pass unchanged at every commit of this slab, so that
    ~60 files of later consumer churn have a trustworthy baseline.

---

## Implementation Decisions

### D1 — Prototype B's tree is adopted, not rewritten

Ticket 13 produced two independent prototypes. **B's tree is this slab's starting point.** It is
1,959 new production lines and 1,868 test lines across 28 production files and six test classes, and
it already passes `mvn compile`, `fmt-maven-plugin:check` (Google Java Style), `ApplicationTest`
against a Postgres testcontainer, and the parity gate unchanged at 255 tests in 24 classes. Its six
commits are already slice-shaped.

Slice 01 therefore **adopts** that tree and brings it up to this repository's test convention
(`@DisplayName` on every method — neither prototype has it, every pre-existing Lifecycle test does).
Later slices are one graft or one decision each. The alternative — treating both branches as
throwaway and retyping from this document — was considered and rejected against work that already
passes three gates.

Prototype A is not merged. Seven of its ideas are taken as individual slices; the rest are recorded
as declined with reasons in [taking A's good ideas into B](../state-machine-implementation/notes/evaluator-a-into-b.md).

### D2 — Three packages, and the dependency arrows only point one way

- **`lifecycle.graph`** — the vocabulary the other two agree on: the compiled graph, its Transitions,
  the callable Guard and Action steps, the evaluation and action contexts, the verdict and failure
  category types, `RequiredAuthority`, and the two catalogue interfaces. Depends on the JDK (plus
  Lombok — see D11) and nothing else.
- **`lifecycle.evaluation`** — the Transition Evaluator, the Guard and Action strategy contracts, the
  two registries, the four ported strategies, the outcome type, and the Information Requirement port.
- **`lifecycle.definition`** — the existing entities and repositories from slab 08, plus the compiler
  that turns rows into a graph and the compiled-graph cache.

`definition` and `evaluation` both depend on `graph`. **Neither `graph` nor `evaluation` may name
`definition`, and `definition` may not name `evaluation`.** `DefinitionScope` stays package-private in
`definition`; `RequiredAuthority` moves to `graph` and comes off the inertness guard's name list.

The compiled graph carries no Definition Scope. ADR 0001's scope-parameterization is *which graph the
evaluator is handed*, not a field on one — a field would only invite the branch that lets the two
Lifecycles drift apart again.

### D3 — The compiled graph holds bound closures, not data plus a type key

This is the one decision the two prototypes could not merge. **B's answer stands.** A Guard Wiring row
is a type key and a jsonb blob; a compiled Guard step is a thing you can call. Resolving the key
against the catalogue and reading the blob into the strategy's declared type both happen once, when
the graph is compiled.

Consequences, all intended:

- The evaluator never sees a type key and never sees JSON, so it cannot fail on either at fire time.
- The Guard stage is a loop over closures: for each step, ask it.
- A step exposes its type key for messages only. Nothing dispatches on it.
- The compiled graph is **not** a good source for admin tooling — it has forgotten sort orders,
  scopes and configuration on purpose. ADR 0002 already says the effective chain admin tooling needs
  is one query against the Wiring tables.

### D4 — The seam between `definition` and `evaluation` is two one-method interfaces in `graph`

`GuardCatalogue` and `ActionCatalogue` each take a type key and a raw configuration string and return
something callable. The registries in `evaluation` implement them; the compiler in `definition` calls
them. Neither package names the other, and neither grows a reason to.

```java
public interface GuardCatalogue {
  /** @throws InvalidGraphException if no strategy declares that key, or the params do not fit it */
  GuardStep bind(String typeKey, String paramsJson);
}
```

**One row-level fact the chain must preserve.** There is no uniqueness constraint on `type_key` in
either Wiring table, so one Definition Version may legitimately wire the same type key twice at
different `sort_order`s — the same Guard, configured two ways. A compiled chain is therefore a
**list** and never a map, and two steps in one chain may report the same key. (Note also that the
ordering column is `sort_order`, not `order`; and `params` is `JSONB` mapped to a Java `String`
through `@JdbcTypeCode`, so what a catalogue receives is already a string.)

Two adapters exist on each side of the seam — the real registry in production, a lambda or fake in the
compiler's tests — and something real varies across them, which is that `definition` must not know
`evaluation` exists. That is what makes it a seam rather than hypothetical indirection.

### D5 — Authority facts reach the evaluator raw, and the rule lives inside it

The evaluation context carries the caller and the subject as facts — a person id and their granted
authorities, the Negotiation's creator id, the Resource's representative ids — and the evaluator
computes creator-ness and representative-ness itself. It does **not** receive a pre-computed set of
satisfied authorities. ADR 0001's stated reason for having one evaluator is that the two Lifecycles
cannot disagree about how a move is judged; pre-computing moves that rule back out to each caller.

The caller is a sealed type, not a boolean flag, because ADR 0007's "machine-fired if and only if the
authority is `SYSTEM`" has two halves and both must hold:

```java
switch (context.caller()) {
  case Caller.TheSystem ignored -> required == SYSTEM;      // and nothing else
  case Caller.Person person -> switch (required) {
    case NONE              -> true;
    case IS_ADMIN          -> person.authorities().contains("ROLE_ADMIN");
    case IS_CREATOR        -> person.personId() == subject.negotiationCreatorId();
    case IS_REPRESENTATIVE -> subject.representativeIds().contains(person.personId());
    case SYSTEM            -> false;                        // no human, ever
  };
}
```
*(shape taken from prototype B, where it is the fix for a real hole in prototype A: reading `NONE` as
including the system caller lets the Orchestration Trigger fire every unguarded open Event.)*

`IS_ADMIN` reads the granted `ROLE_ADMIN` authority on the token — verified to be what `isAdmin` has
always meant on both machines — and **not** the `admin` column of the Person row, which a seeded
caller holds while being offered nothing anywhere.

Every new thing a Guard wants to know becomes a visible change to the evaluation context, justified
once and in the open. That is the mechanism the no-I/O rule exists to create, not an ascetic gesture.

### D6 — The outcome is a sealed pair, not one record with nullable halves

```java
public sealed interface EvaluationOutcome {
  boolean permitted();
  record Permitted(CompiledTransition transition) implements EvaluationOutcome { }
  record Refused(FailureCategory category, String reasonCode, Map<String, Object> details)
      implements EvaluationOutcome { }
}
```

A caller pattern-matches and cannot reach a field the case does not have. `FailureCategory` carries
four values, not three — the three ADR 0005 maps to 403, 422 and 409, plus the structural "there is no
such move", which is not a gate failure at all and must not be mapped to one of the three.

**The 403/422/409 mapping stays out of this package.** The category names the categories and nothing
about HTTP; the controller owns the mapping. A Guard's refusal is always a domain-state conflict,
assigned by the pipeline stage rather than by the Guard — the Guard says *what* is wrong through its
reason code and details, which is what keeps the categories monotonic however a Guard is written or
wired. Every refusal carries details: the wanted authority, the Event and version, the unfinished
Resource ids, and the refusing Guard's key.

### D7 — An unknown current State fails distinctly, in every lookup

Both prototypes' plans say an unknown current State is graph corruption and must fail distinctly.
Only A's code does it; B's does it in one of three lookups. **The graph refuses in all of them.**

The Transition Evaluator asks the State question *before* it asks whether the Event is declared, so a
corrupt Definition Version Pin is never reported as an unknown Event. This is also what finally gives
the graph's "does this version declare this State" predicate a production caller.

This hands the cutover slab one short rule to write: **a corrupt pin is a 500 and a log line, never a
403, 422 or 409.** A Possible Events listing that throws and one that silently returns an empty set
are different failure modes for the same broken row, and only the first is honest.

### D8 — One named exception for a graph that cannot exist

Every rejection of a graph or a compile raises one named exception type rather than a mix of
`IllegalStateException` and `IllegalArgumentException`. It extends `IllegalStateException`, because
these are broken-invariant statements about data the caller did not choose. It lets the cutover slab
catch graph corruption and nothing else, and lets a test assert a class rather than a message
substring.

Deliberately **not** folded in: the verdict type's own refusal of a malformed verdict, which is a
programming error in a strategy and genuinely an `IllegalArgumentException`.

This names what a *graph* raises, and only that. What a *refused move* becomes at the REST boundary
is still open and is the cutover slab's: today the Resource Lifecycle service throws Spring
Statemachine's own `StateMachineException`, a type this effort deletes, so that slab needs a
replacement — and D6's sealed outcome type is what it answers from, rather than an exception.

### D9 — Wiring configuration is read strictly, and the no-configuration rule lives above the mapper

Two rules, and both are in code rather than in Jackson's ambient settings:

1. **Configuration supplied to a strategy that takes none is refused**, naming the key and the blob.
   Null, blank, `null` and `{}` are accepted; anything else is not.
2. **Unknown fields are refused.** The reader used for Wiring configuration enables
   `FAIL_ON_UNKNOWN_PROPERTIES` explicitly, regardless of which `ObjectMapper` the container built.

The second goes past both prototypes and is a deliberate decision. Spring Boot disables that feature
by default and this repository does not re-enable it, so today a misspelled field in a Wiring row
binds the type's default and nobody hears about it — the exact failure that binding configuration at
compile time exists to prevent. Wiring configuration is configuration, not user input.

Neither rule may depend on the injected mapper's leniency. Prototype B's registry tests construct a
strict `ObjectMapper` while production injects Boot's lenient one, so its suite could not observe
production behaviour in either direction; **one test builds a deliberately lenient mapper and asserts
both rules still hold.**

### D10 — The compiled-graph cache is built, minus a port and minus `computeIfAbsent`

Issue 09's "What to build" requires it and ADR 0001 and 0003 fix its shape, so prototype B's deferral
is not carried. It is keyed on the Definition Version's row id alone, unbounded (immutable published
versions are retained for pinned work; a size policy needs production cardinality evidence), and
supports targeted invalidation on publish.

Two changes from prototype A's version:

- **No load port.** A port with one adapter is indirection; A's own artifact flagged it and B declined
  to declare it for the same reason. The cache names its producer directly. The interface arrives when
  a second adapter does.
- **No `computeIfAbsent`.** That runs the producer inside the map's per-bin lock — harmless against a
  test lambda, wrong against the real loader, which will issue six queries in a transaction. Read,
  produce outside the map, then put-if-absent. Two threads may compile one version twice;
  compilation is pure and idempotent, so that is duplicated work rather than a wrong answer, which is
  the right trade against a lock held across I/O. Failures are not cached.

The concrete argument for the cache is the Feedback Guard: terminality must be asked of each
Resource's *own* pinned Definition Version, so a caller assembling a Negotiation-scope context
resolves one graph per Resource before it calls. That is exactly the explicit, testable load step ADR
0001 wanted, and exactly the N-way load it warned an engine owning its persistence would discover only
under load.

### D11 — Lombok is used where it fits, with three carve-outs

Lombok is house style: 220 of 437 production files, `provided` scope, and slab 08's six definition
entities already carry `@Getter @Setter @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PROTECTED)
@Builder(access = PROTECTED)`. New code matches that.

- **`@NonNull` replaces the hand-written null checks** on the graph's records — 18 of them across the
  evaluation context and its nested types, the compiled Transition, the action context, the verdict
  and the graph builder. There is precedent for Lombok `@NonNull` on a record in this backend already.
- **`@RequiredArgsConstructor`** on the two registries, the compiler and the cache.

Three carve-outs, each with a reason:

1. **The purity gate's allowlist becomes `java.` *and* `lombok.`** — see D18. Lombok's annotations are
   `SOURCE` retention at `provided` scope, so nothing Lombok reaches the runtime classpath and the
   property the gate protects stays literally true of the compiled output. The rule carries a comment
   saying why those two and nothing else, because a two-entry allowlist invites a third.
2. **The graph's builder stays hand-written.** Lombok's `@Builder` generates one setter per field and
   calls a constructor. This builder is not that: declaring a State as initial or terminal also
   declares it, declaring a Transition also declares its Event, and `build()` runs three validations
   and constructs two indexes. A Lombok builder would take the derived sets and both indexes from the
   caller, which is exactly the work this builder exists to remove — a fixture is
   `builder(id).initialState("DRAFT").terminalState("ABANDONED").transition("DRAFT","SUBMIT","SUBMITTED",IS_CREATOR)`
   and everything else is derived.
3. **The Transition Evaluator's constructor stays hand-written.** Issue 09 requires that having no
   repository, `EntityManager` or Spring Data dependency "should be visible in its constructor". With a
   generated constructor there is none to look at. One class, and the PRD records why.

**`@Nullable` is not adopted in `graph`.** Lombok has no such annotation, and this backend's is
`org.springframework.lang.Nullable` — a Spring import, which is a real runtime dependency and a much
weaker case for the allowlist than Lombok's. Optional components stay documented in javadoc, which is
what prototype B already does.

### D12 — Required Authority stays single-valued

Both prototypes kept it single-valued and said so. That follows slab 08's precedent, which built the
column single-valued as ADR 0002 specifies with ticket 11 left open rather than getting ahead of it.
**This slab invents no disjunction.**

The existing enum's javadoc already records why: six of the eight Negotiation Transitions are
behaviourally "administrator or creator", and inventing a disjunction is not that type's call. **Keep
that text through the move** — prototype A's replacement discarded 18 lines of javadoc that existed
before the prototype; B's kept it and added to it.

### D13 — One vocabulary, and one gap in the glossary

Prototype B's naming set is adopted whole: `FailureCategory`, `GuardVerdict`, `EvaluationOutcome`,
`Guard`, `Action`, `PostVisibility`, `DefinitionVersionRows` — over A's `RefusalCategory`,
`GuardResult`, `EvaluationResult`, `GuardStrategy`, `ActionStrategy`, `PostVisibilityWriter`,
`MaterializedDefinition`. Plus D8's exception name.

Underneath the Java names sits a real gap. **`backend/CONTEXT.md` has no term for the compiled graph
at all.** Its "The definition graph" section defines the States, Events and Transitions *of a
Definition Version*; nothing names the evaluator-ready value compiled from those rows, and nothing
names the act of compiling. Ticket 13's own instructions flagged it while creating the thing — "the
'compilation' (might need another word)".

The map's binding constraints make this `/domain-modeling`'s job, not a naming preference. **One slice
adds the terms to `backend/CONTEXT.md` with their `_Avoid_` lines**, and the Java names follow the
glossary rather than the reverse.

### D14 — The four strategies, and five corrections about the code they port

`NEGOTIATION_APPROVED`, `TERMINAL_AGGREGATION`, `SET_POST_VISIBILITY` and
`SPAWN_RESOURCE_LIFECYCLES`, as issue 09 lists them. Five corrections, recorded during ticket 13's
claim of ticket 09 that does not hold, and carried here because issue 09's own text is wrong on
each:

1. **"Take them from ticket 01's graph dump" cannot be followed for Guards.** All 21 Transitions
   across both dumps record a null guard. The dump is authoritative for Actions, States, Events and
   authorities, and silent on Guards. The Guard inventory comes out of the two Lifecycle services.
2. **`NEGOTIATION_APPROVED`'s live behaviour is not in the Guard bean.** That bean is attached to
   nothing and Spring Statemachine discards it; the characterization suite says outright that a Guard
   which has never fired must not be reimplemented. The rule runs imperatively in the Resource
   Lifecycle service. **The slab ports the gate, not the class** — the only reading under which ADR
   0005 and the characterization finding are both satisfied. It reads the parent Negotiation's State
   off the evaluation context and does no lookup, and it names that State through the existing
   Well-known State holder rather than as a string literal.
3. **`SET_POST_VISIBILITY` needs a three-valued scope** — `PUBLIC | PRIVATE | BOTH`. Today's disable
   Action writes both flags, so two values would turn three dump Actions into four Wiring rows. It is
   **one** `@Component` with three Wiring rows varying in configuration, *not* three bean instances:
   the webhook mapping strategy that looks like the model does not transfer, because those beans
   each declare a different key while all three post-visibility configurations declare one, and three
   beans claiming one key fail the boot by design.
4. **Today's Information Requirement check is not requirement-scoped** — it passes if *any*
   submission exists for the resource-and-negotiation pair, whatever form it was. ADR 0005 and 0006
   are a strengthening with a migration story, not a reproduction.
5. **A blocked Resource Event is a silent no-op today**, returning the current State rather than
   refusing. The pipeline's categories are a deliberate change against current behaviour, and belong
   in the intended-deltas half of the gate rather than the parity half when the cutover slab wires
   this in.

The Feedback Guard takes no configuration and passes vacuously over a Negotiation with no Resources —
the vacuous reading of "every", left vacuous deliberately, because refusing would need a rule about
what an empty Negotiation means and that is a product question nobody has asked. Which States carry
the terminal flag is seed content and belongs to the migration slab.

`SPAWN_RESOURCE_LIFECYCLES` is registration and configuration only. Its body writes, so it belongs to
the coupling slab; **calling it in this slab throws.** Worth knowing while naming it: unlike the other
three it is **not in the state machine at all today** — it is a notification handler, firing on arrival
at the Well-known `IN_PROGRESS` State, and the spawn itself lives in the resource notification
service, which re-checks that State, skips any Resource that already has one, and assigns
`REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE` by whether the Resource has
representatives. That is the code the coupling slab relocates, and it is why the map already forbids
that relocation from publishing a Resource state change while requiring it to publish a spawned
event — both constraints ticket 02 already recorded.

Today's terminal predicate, for the same reason, is not in a Guard either: it is a listener comparing
against two hardcoded Resource State names out of twelve. Two of the ten that do not count are States
in which a Resource is finished in every practical sense, so a Negotiation of only those stays in
progress for ever. That is pinned as behaviour, not endorsed; prototype B's own Guard javadoc names
both States and says which slab owns the flag.

### D15 — The string-keyed registry is new to this backend

ADR 0002 and issue 09 both say the registries are folded "exactly as the existing `WebhookEventMapper`
does". That mapper is keyed on a `Class`, and a grep finds no string-keyed strategy registry anywhere
in this backend. **The ADR names the mechanism, not a copyable example.** What transfers is the shape:
a private static pure fold called from the constructor, put-if-absent, an exception thrown *from the
constructor* so a duplicate key is a bean creation failure and therefore a failed boot, a message
naming the key and both colliding classes, and a frozen copy. What does not transfer is the key type.

Note also that the notification subsystem folds the same shape into a multimap with no collision rule
at all, because several handlers per event are legal there. Two Guards claiming one key are not, so
the strict rule is a choice and is recorded as one.

### D16 — The Information Requirement check is a Built-in Stage behind a port

It always runs, in ADR 0005's fixed position between Required Authority and the Guard chain, and no
Wiring row exists anywhere that could omit it. It is deliberately not a registry Guard type: a
wireable Guard would have to be attached to each Transition by hand, and one forgotten row silently
reintroduces the dead click this design exists to remove. It still emits the same verdict shape as a
Guard, so callers see one uniform list of failures.

In this slab the satisfaction lookup is **the evaluator's only constructor argument**, and it is an
interface: whether a form has been submitted is the single question about a move that cannot be handed
in on the context, so it goes behind a port instead of behind a query. That is also why the subject on
the context carries identities at all rather than only properties.

Its production bean **throws**. Audience, Quantifier and the real lookup are a later slab.

### D17 — Placeholders refuse rather than write

Issue 09 excludes everything that writes. Both unbuilt seams — Information Requirement satisfaction
and the post-visibility write — therefore have production beans that **refuse**, not permissive ones.
A permissive placeholder would let the cutover slab wire the evaluator and silently drop a gate or an
effect; throwing makes that a failed request on the first attempt. **Deleting each refusing class is
how the owning slab announces itself.**

Prototype A instead left a real, working write adapter in production code with no caller. That is
rejected — under a ticket that excludes writes, the placeholder that cannot write is the one that
honours it. What A does establish and is worth keeping is that the target methods exist with the
expected signatures, so the refusing class names a seam that really fits something.

The evaluator itself **is** a Spring bean, so `ApplicationTest` exercises the whole wiring — including
that every strategy bean folds into its registry without a key collision.

### D18 — Three structural gates, and one existing one amended

- **Evaluator purity.** A text scan over the graph and evaluation packages, with comments blanked so
  prose naming a forbidden term is not a violation, `file:line` reporting, and a root resolved by
  walking upward rather than against the working directory. Rules: nothing that could reach a database
  (persistence packages, `EntityManager` and its kin, and **any** `*Repository` by suffix, so one that
  does not exist yet is forbidden too); no edge from `graph` or `evaluation` into `definition`, in
  both directions; no edge from `graph` into `evaluation`; and — taken from prototype A — **every
  import in `graph` must start with `java.` or `lombok.`**. That last one is an allowlist rather than a
  blocklist on purpose: a blocklist stops only what someone thought of, and today `graph` could grow
  an import of Jackson, of a Spring annotation, or of anything under `common/` and no other rule would
  notice. A's rule passes on B's tree unchanged.
- **Definition inertness**, amended twice rather than once. Prototype A added its own new
  package-private type names to the guard's list; prototype B added javadoc recording why
  `RequiredAuthority` came off, why `DefinitionScope` stays, and that this is not a precedent for
  trimming the list one name at a time. They protect different things — reach, and resistance to being
  weakened — so **do both**. Adding the compiler and its input record to the list turns the compiler's
  own javadoc claim, that nothing outside the package may compile a graph, into a tested rule.
- **Anti-vacuity on every gate.** A minimum scanned-source count, and each rule proven to fire on the
  thing it forbids and not to fire on something innocent. Slab 07 learned this the hard way: a
  detector exercised only against a tree with no examples goes vacuous silently.

One defect to fix while in that file: prototype B's minimum-count message has a `.formatted(...)` bound
to only the second half of a concatenated literal, so it prints the scanned count where the minimum
belongs. Message only; it never affected the pass/fail decision.

### D19 — Nothing in production calls this slab

The compiler and the cache stay package-private. No repository in the definition package gains a
"load the whole version by id" query — the slab that writes one is the slab that starts reading these
tables, and it is not this one. The definition inertness guard therefore **survives with an amendment
rather than the deletion the map anticipated**; that deletion is still the cutover slab's, and still
owed as a visible line in its diff.

### D20 — Slice sequence

Adoption first, then the additive grafts, then the two decisions, then the vocabulary. Every slice
ends green on the slab gate and the parity gate.

1. Adopt prototype B's tree; `@DisplayName` throughout; the Well-known State constant in place of the
   string literal.
2. Lombok — `@NonNull` and `@RequiredArgsConstructor` per D11, with the two hand-written carve-outs
   left alone.
3. The purity gate's allowlist rule and the `.formatted` fix (D18).
4. The inertness guard's two new names (D18).
5. The three topology fixtures both plans assert and neither proves — a cycle, an unreachable State,
   a version with no terminal State (D2, user story 50).
6. One named exception type across every graph and compile rejection (D8).
7. Unknown current State refused in every lookup, plus the evaluator's ordering fix (D7).
8. Strict Wiring configuration and the no-configuration rule, with the lenient-mapper test (D9).
9. The compiled-graph cache (D10).
10. The glossary terms via `/domain-modeling` (D13).

Slices 3–5 are additive and depend on nothing else here; they can land in any order or in parallel.

---

## Testing Decisions

### What makes a good test here

Test what a module answers, never how it arrives there. Every one of the six seams below is a module
interface that something other than a test also calls — which is the property that keeps these tests
from pinning implementation. A test that reaches for a package-private field, or asserts on the shape
of an index, is testing the wrong thing.

One behaviour per test method, named in a `@DisplayName` sentence, so a failure names the behaviour
rather than the layer. Prototype B's 98 tests are already close to this; prototype A's 15 assert
several behaviours each — one of its evaluator tests asserts five things about authority, target State
and Possible Events at once — which is why a regression there reports one failure covering several
rules.

Every failure path is run **red on purpose** before being trusted green. That is this effort's
standing practice and it caught real vacuity twice already, in slabs 07 and 08.

### The six seams

Confirmed with the developer before this PRD was written. Note that the count is forced by D3 rather
than chosen: because binding happens at compile time, an unknown type key and unreadable configuration
are **structurally unreachable** from the evaluator, so they cannot be tested there.

| | Seam | What it carries |
|---|---|---|
| **A** | The Transition Evaluator's two questions | The Evaluation Pipeline's fixed order and short-circuit; both halves of the authority rule over all five values and both caller kinds; the four failure categories and their reason codes; that the listing and the gate agree; that a permitted outcome reports Actions and runs none. |
| **B** | Compiling a Definition Version's rows | The two Guard scopes folded in ADR 0005's order, each in configured order; that a definition-wide Wiring row is bound once and reaches every Transition including one added later; Action chains transition-scoped only; rows from another version refused; an unknown type key refused at compile; cross-catalogue misuse refused. Uses **fake catalogues**, which is what lets it run without a registry, Jackson or Spring. |
| **C** | The graph's builder | Exactly one initial State; Transitions naming declared States; at most one Transition per State-and-Event; the three shapes that stay valid (cycle, unreachable Legacy State, no terminal State); an Event declarable with no Transition; an unknown State refused by every lookup. Separate from B because the builder has three construction paths — the compiler, a test, and one day whatever reads a definition file — and B can only easily produce valid row sets. |
| **D** | The two catalogues | Duplicate type key fails the boot with both class names; unknown key refused, listing the known ones; configuration bound to the declared type; configuration supplied to a strategy that takes none refused; unknown field refused; and the same two rules asserted under a deliberately lenient mapper. |
| **E** | The four strategies | Each as a pure function of context and configuration. Parent-approval passes only on the Well-known State; Feedback passes when every Resource's own graph calls its State terminal, passes vacuously on none, and names the unfinished ones; post-visibility writes through its port for all three scope values; spawn throws. |
| **F** | The compiled-graph cache | One production per row id; no lock held across production; failures not cached; targeted invalidation; identity is the row id alone. |

**A divergence from the superseded PRD, stated deliberately.** That document's testing decisions —
which ticket 13 said outlive its slicing — put both Guards behind the evaluator and gave the
configuration bridge no seam of its own, on the grounds that a fixture builder could feed raw JSON
through the real binder. This PRD does the opposite on both points, because D3 changed what is
reachable: binding failures cannot be provoked through the evaluator at all once binding happens at
compile time, and a strategy tested only through three layers reports a failure that names the wrong
one. What does carry across from that document is its argument for why the evaluator is not inside the
definition package.

### Gates, which are not seams

- **Evaluator purity** and **definition inertness** per D18 — text scans over production sources,
  each with its own anti-vacuity test.
- **The parity gate**, per `parity-gate.md`, run at every slice: the parity half unchanged at 255
  tests in 24 classes, and the intended deltas reported separately at 8. This slab changes no
  production behaviour, so a parity movement is a defect in the slab, not a delta to accept.
- **`ApplicationTest`**, which proves the whole Spring context folds every strategy bean into its
  registry without a collision — the only test that exercises D15's boot-failure rule from the real
  container.

### Prior art

- **`DefinitionInertnessGuardTest`** — the register for every structural gate here: source scanned as
  text, comments blanked, `file:line` reporting, a meta-test that fails if the scan found nothing. Its
  hardest-won lesson is that a guard built only from Java identifiers does not prove a database claim,
  because a native query names the table and never the entity.
- **`LifecycleEnumDecouplingGuardTest`** — the mirror-image lesson, and the reason for the
  anti-vacuity requirement: an identifier scan reported green over a codebase where every consumer
  reached an enum through a getter, importing nothing, so its signature rule carries its own fixture.
- **`RawStateNamesInSqlGuardTest`** — the precedent for pinning names the compiler cannot see.
- **The definition repository tests from slab 08** — `@DisplayName` on every method, which is the
  convention both prototypes missed and slice 01 restores.
- **`WebhookEventMapper`** — the fold shape, with D15's correction about the key type.
- **Prototype B's six test classes** — adopted in slice 01 and the starting point for seams A–E.

---

## Out of Scope

- **Wiring the evaluator into either Lifecycle service.** The cutover slab. Both service interfaces
  already compare Event names as plain strings and validate none of them, so whether that seam
  validates is a decision that slab takes on purpose.
- **Anything that writes.** Committing a move, running Actions for real, Lifecycle Records,
  notifications, webhooks. ADR 0001 puts all of it in the services around the evaluator.
- **The graph loader** — a "load the whole Definition Version by id" query. It reads the definition
  tables, which is what deletes the inertness guard. The cutover slab's, along with that deletion.
- **The real Information Requirement satisfaction lookup, Audience resolution and Quantifier
  counting.** Port shape only, per issue 09 and ADR 0006.
- **`SPAWN_RESOURCE_LIFECYCLES`'s body**, and with it Definition Resolution at Spawn and writing the
  Resource's Definition Version Pin. The coupling slab's, which also inherits slab 08's open question
  about how that pin is written at all given the column is not updatable.
- **The 403/422/409 mapping.** The controller's.
- **Which States carry the terminal flag.** Seed content; the migration slab's, via ticket 12.
- **Required Authority as a disjunction.** Ticket 11, still open.
- **Deleting Spring Statemachine.** Later, and by then it touches very little — which is standing
  decision 2's entire point.
- **Definition Scope on the compiled graph.** Not deferred — established as unnecessary. What
  parameterizes the evaluator is which graph it is handed.

---

## Further Notes

**Two documents this PRD rests on** live at the effort level rather than in this slab, because they
outlive it — the same reason `parity-gate.md` and `before-picture-findings.md` sit there:
[the prototype comparison](../state-machine-implementation/notes/evaluator-prototype-comparison.md),
which measured both trees by running every command against both branches, and
[taking A's good ideas into B](../state-machine-implementation/notes/evaluator-a-into-b.md), which
treats B as the base and records which of A's ideas graft and which are declined.

**Do not lift prototype B's divergence table.** Its Part 7 was written from prototype A's two
documents and not from A's Java, and B labelled that limitation itself — but three of its rows do not
survive a reading of A's tree. Ticket 13's Answer records all three with citations. The rows about the
fourth failure category and about cycles are both wrong, and one silence hid a divergence in B's
favour.

**Prototype A's branch is not merged and should not be cherry-picked.** The first, superseded
attempt at this slab — `slice-01-vocabulary-move`, whose landed commit moved both vocabulary enums up
to `lifecycle`, a layout ticket 13 superseded and both prototypes contradict — has been **deleted** in
full — its 474-line PRD, its ten inner issues and its read-only recon brief — so that nobody
implementing this slab finds a second PRD at this very path. Nothing was carried across. Prototype B
had audited that recon section by section while porting the strategies and found one thing in it that
does not transfer (D14.3); everything else in it that this slab needs is either in B's tree, in
`before-picture-findings.md`, or restated in this document.

**What this slab hands the cutover slab**, each already stated above but collected here: the corrupt-pin
rule from D7; the deletion of the definition inertness guard as a visible line in its diff (D19); the
deletion of both refusing placeholder classes as each owning slab claims its work (D17); the
intended-delta framing for the two behaviour changes in D14.4 and D14.5; and the N-way graph
resolution the cache exists to make cheap (D10).

**The measured size of what is being adopted**, for anyone estimating: 28 production files, 1,959
production lines of which 747 are comments, six test classes, 1,868 test lines, 98 tests. Prototype
B's own artifacts claim 21 files and 100 tests; those numbers are wrong and the ones here were
produced by running the commands against the tree.

**Branch discipline.** One long-lived branch descending from `plan/state-machine-redesign`, never
merged to master per stage — ADR 0009's migration is a breaking stop-the-world cutover, so a stage
reaching master would run it on the next production deploy. Merge master *into* the branch regularly
instead.
