# Taking A's good ideas into B

Companion to [the comparison](evaluator-prototype-comparison.md). That report ranked nothing. This one
does one asymmetric thing: it treats **B's tree as the base** and asks, of every idea A has that B
does not, whether it grafts — and if so, what the change is and what it costs.

"Grafts" means one of three things, and each item says which:

- **as-is** — the idea lands in B without touching any of the eight open decisions in the comparison's
  §13. Additive.
- **with modification** — the idea is right and A's implementation of it is not, or not in B.
- **does not graft** — the idea belongs to a decision B already took the other way, or it is worse in B
  than in A for a reason particular to B.

Paths are relative to `backend/src/main/java/eu/bbmri_eric/negotiator/` and
`backend/src/test/java/eu/bbmri_eric/negotiator/`. Every line reference was re-read on both branches
for this document; the two facts that needed running rather than reading (B's `graph` imports, the
uniqueness of B's two new type names) were checked with `grep` against B's worktree.

---

## Summary

| # | A's idea | Fits | Touches an open §13 decision? |
|---|---|---|---|
| 1 | Unknown current State refuses distinctly, in every lookup | as-is | settles §13.4 the way both plans already say |
| 2 | One named exception for a graph that cannot exist | as-is | adds one word to §13.8 |
| 3 | Params supplied to a no-params strategy are refused | with modification | no — this is §13's trailing "needs a rule and a test" |
| 4 | The graph package's purity as a **whitelist**, not a blacklist | as-is | no |
| 5 | The inertness guard's reach extended to the new package-private types | as-is | no |
| 6 | The topology constraints both plans assert and neither proves | as-is | no |
| 7 | A compiled-graph cache | with modification, later | answers §13.5 |
| — | eight further ideas | do not graft | see §9 |

Six of the seven are additive. None of them requires B to reopen the closure-versus-data decision,
which is the point of doing it in this direction.

---

## 1. Unknown current State refuses distinctly — **as-is**

**What A has.** `CompiledGraph.requireState` (`lifecycle/graph/CompiledGraph.java:79-85`) throws
`InvalidGraphException("Unknown State: …")`, and every lookup calls it first: `transition` (`:66`),
`transitionsFrom` (`:71`), `isTerminal` (`:76`).

**What B has.** One of the three. `isTerminal` throws (`graph/CompiledGraph.java:120-127`), while
`transition` and `transitionsFrom` answer `Optional.empty()` and `List.of()` for a State the version
does not declare (`:135-146`).

**Why it fits.** This is not A's opinion arriving in B — it is B's own stated intent arriving in B's
code. Three things on B's branch already ask for it:

- `PLAN.md:94-96`: "Unknown current State is graph corruption and should fail distinctly. A known
  Event with no Transition from the current State is an ordinary unavailable move."
- `isTerminal`'s javadoc argues the general case in B's own words: "Answering `false` for a State the
  graph has never heard of would be the more forgiving choice and the wrong one" (`:114-118`). The
  argument does not stop at terminality.
- `declaresState` exists, is public, and has no production caller (`:83-85`). This graft is what makes
  it load-bearing.

Today a corrupt Definition Version Pin reaches the evaluator and comes back as the ordinary refusal
`NO_TRANSITION_FOR_EVENT` (`evaluation/TransitionEvaluator.java:61-67`), or as an empty Possible
Events set — a Negotiation with no available moves and no explanation.

**The change.** Extract `requireDeclared(String state)` from `isTerminal`'s existing check and call it
first in `transition` and `transitionsFrom` as well.

Then one ordering detail that A does not have to face and B does. B's evaluator checks
`declaresEvent` *before* it looks up the edge (`evaluation/TransitionEvaluator.java:55-61`), so a
corrupt State combined with an unknown Event would still return `UNKNOWN_EVENT` — the wrong diagnosis,
reported confidently. Put the State question first:

```java
// evaluation/TransitionEvaluator.java, before the declaresEvent check
if (!graph.declaresState(fromState)) { /* corrupt pin */ }
```

Which is also the moment `declaresState` earns its keep.

**Cost.** None in B's suite: nothing in `graph/CompiledGraphTest.java` asserts the forgiving
behaviour — `transition("IN_PROGRESS", "APPROVE")` (`:63`), `transition("SUBMITTED", "START")` (`:68`)
and `transitionsFrom("APPROVED")` (`:110`) all name declared States, which is why they are the
*ordinary* cases they claim to be. Two new tests, mirroring
`isTerminal_whenTheVersionDoesNotDeclareTheState_throws` (`:128-132`).

The real cost is downstream and is the comparison's §9.5: the evaluator can now throw at fire time,
so the cutover slab needs a rule. That rule is short — a corrupt pin is a 500 and a log line, never a
403/422/409 — but it has to be written, because a Possible Events listing that throws and one that
silently returns an empty set are different failure modes for the same broken row.

---

## 2. One named exception for a graph that cannot exist — **as-is**

**What A has.** `graph/InvalidGraphException.java`, nine lines, thrown by every graph rejection.

**What B has.** Five throw sites and three exception types between them: `IllegalStateException` from
the builder for the initial-State, undeclared-State and duplicate-edge rules
(`graph/CompiledGraph.java:243`, `:270`, `:282`), `IllegalArgumentException` from `isTerminal`
(`:122`), `IllegalArgumentException` from
`DefinitionCompiler.requireRowsBelongToTheVersion` (`definition/DefinitionCompiler.java:193`).

**Why it fits.** Three reasons, and the first only appears once graft 1 lands:

- The cutover slab has to catch this and map it. Catching `IllegalStateException` around an evaluation
  catches anything else in reach that throws one; catching a named type catches graph corruption and
  nothing else.
- A test can assert the class instead of a message substring. B's tests currently pin messages
  (`hasMessageContaining("declares no State")`), which is the assertion that breaks when someone
  improves the wording.
- It names the category. "This graph cannot exist" is one idea told five ways right now.

**The change.** One nine-line class in `graph`; five throw sites; roughly five test expectations.

**One decision inside it, and my recommendation.** A chose `extends IllegalArgumentException`. Prefer
`extends IllegalStateException` in B: four of the five sites are broken-invariant statements about
data the caller did not choose, not bad arguments. Do **not** fold in `GuardVerdict.java:40` — that
one refuses a verdict built with a null reason code, which is a programming error in a strategy and
genuinely is an `IllegalArgumentException`.

**Feeds §13.8.** One more word for the vocabulary list to carry.

---

## 3. Params supplied to a no-params strategy are refused — **with modification**

**What A has.** Both registries refuse a non-null `params` blob for a strategy declaring `Void`
(`evaluation/GuardRegistry.java:36-40`, `evaluation/ActionRegistry.java:35-39`).

**What B has.** Nothing, and the comparison's §9.3 probe says what happens instead: B's `readParams`
sends any non-blank blob through the injected mapper into `NoParams`
(`evaluation/GuardRegistry.java:86-98`). Under a strict mapper that throws
`UnrecognizedPropertyException`; under Spring Boot's, which disables `FAIL_ON_UNKNOWN_PROPERTIES` and
which this repository does not re-enable, it is accepted and the configuration is silently discarded.
Production injects Boot's. B's own registry tests construct `new ObjectMapper()`
(`evaluation/GuardRegistryTest.java:30`, and three times in `LifecycleStrategiesTest.java`), so the
suite cannot see either behaviour.

**Why it fits, and better in B than in A.** B has a named type to test against. `paramsType() ==
NoParams.class` is already the branch `defaultParams` takes (`:106-113`) — the graft is the same
question asked on the other side of the null check.

**The modification, in two parts.**

*First*, don't route the rule through Jackson. A's instinct is right and A's placement is what makes
it right: the check belongs in the registry, in code, above the mapper. Otherwise the rule holds or
doesn't hold depending on which `ObjectMapper` bean the container happened to build — which is exactly
how B ended up with a rule its tests could not observe.

```java
// evaluation/GuardRegistry.readParams, before the mapper is touched
if (strategy.paramsType() == NoParams.class && carriesConfiguration(paramsJson)) {
  throw new IllegalArgumentException(...);  // names the key and the blob
}
```
Accept null, blank, `null` and `{}`; refuse anything else. Mirror it in `ActionRegistry`. Two tests,
one per registry — neither prototype has one.

*Second, and this goes past A.* The same hole exists one level down, for a strategy that **does** take
params: under Boot's mapper a Wiring row with a misspelled field binds to the type's default and no
one hears about it. That is precisely the failure that binding-at-compile-time exists to prevent, and
neither prototype covers it. Wiring params are configuration, not user input, so read them strictly
regardless of the ambient mapper:

```java
objectMapper.reader().with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
```

Then add one test that builds a mapper with the feature *disabled* and asserts both rules still hold —
which is the assertion that stops the suite drifting away from production again.

**Cost.** One line, one reader configuration, three tests. This is the comparison's trailing "needs a
rule and a test in either design", closed.

---

## 4. The graph package's purity as a whitelist — **as-is**

**What A has.** `lifecycle/graph/CompiledGraphPurityTest.java`, 49 lines: every `import` line in the
graph package must start with `import java.`, plus an assertion that the source list is not empty
(`:19`).

**What B has.** Four rules in `lifecycle/evaluation/EvaluatorPurityGuardTest.java`, all of them
blacklists: `PERSISTENCE_PACKAGES` (`:51-52`), an `EntityManager`-and-friends pattern (`:55-56`), the
`\b\w*Repository\b` suffix (`:63`), and the two direction rules (`:94-133`).

**Why it fits.** A blacklist forbids what someone thought of. A whitelist forbids what nobody thought
of. B's `graph` package could today grow an import of Jackson, of `org.springframework.stereotype`
(deliberately absent from B's list, and correctly so for `evaluation` — but `graph` should not be a
bean at all), or of anything under `common/`, and no rule on B's branch would notice. A's rule refuses
all three by construction.

And it passes on B unchanged. Every import across B's eleven graph files is `java.util.*` — verified
by grep, nothing else appears. So this can land now and be load-bearing immediately, which is the rare
case for a structural gate.

**The change.** A fifth test in `EvaluatorPurityGuardTest`, scanning only
`path.toString().contains(GRAPH_PACKAGE)` for lines matching `^\s*import\s` and asserting each
continues `import java.`. In B's harness it inherits three things A's version does not have: comment
blanking, `file:line` reporting, and the upward-walking `moduleRoot()` (`:233-248`) that fixes A's own
stated weakness — A resolves `Path.of("src/main/java/…")` against the working directory (`:34`), so
A's gate depends on the run being rooted at `backend/`.

A's other stated weakness — a fully-qualified reference with no import line is invisible to an
import scan — is already covered on B's side by
`theGraphPackage_doesNotKnowTheEvaluationPackageExists` (`:117-133`), which scans text rather than
imports. The two rules together cover both spellings; neither does alone.

Follow B's own anti-vacuity convention and add fixtures to
`theRules_scanTheCodeTheyClaimToAndMatchWhatTheyForbid` (`:141-161`). While in that method, fix the
defect the comparison records at §9.1: at `:145-147` the `.formatted(...)` binds only to the second
half of a concatenated literal, so the message prints the scanned count where the minimum belongs and
leaves the first `%d` literal. Message only, never the pass/fail decision.

---

## 5. The inertness guard's reach extended to the new types — **as-is**

**What A has.** Four names added to `DISTINCTIVE_TYPE_NAMES`: `MaterializedDefinition`,
`DefinitionGraphCompiler`, `CompiledGraphSource`, `CompiledGraphCache`
(`lifecycle/definition/DefinitionInertnessGuardTest.java`, diff at `:85-94`).

**What B has.** 26 lines of javadoc explaining why `RequiredAuthority` came off, why `DefinitionScope`
stayed, which sibling test makes the removal impossible to fake, and that this is not a precedent for
trimming the list (`:70-101`). Its list (`:102-116`) holds thirteen names and includes neither of the
two types B added.

**Why it fits.** The comparison already says these two amendments protect different things (§9.2):
A's extends the gate's reach, B's protects the gate from the next person. Nothing makes them
alternatives. Do both.

The specific gap is that `DefinitionCompiler`'s own javadoc makes a claim nothing tests: "It is
package-private on purpose. Nothing outside this package may compile a graph yet, which is what keeps
the package inert while there is no loader to call it"
(`definition/DefinitionCompiler.java:32-34`). Adding `DefinitionCompiler` and `DefinitionVersionRows`
to the list turns that sentence into the rule it describes.

**The change.** Two strings. Both names were checked for uniqueness — neither appears anywhere in
production outside `lifecycle/definition/`, which is the precondition B's javadoc sets at `:70-74`.
`guard_forbidsOnlyNamesThatStillExist` keeps them honest.

---

## 6. The topology constraints both plans assert and neither proves — **as-is**

**What A has.** A list, in prose: "disconnected States, transition-less Events, cycles and zero or
many terminal States remain valid" (`PLAN.md:52-56`), and — as §12.2 of the comparison records —
no test that builds any of them.

**What B has.** Four of them tested and three not. Covered: transition-less Events
(`graph/CompiledGraphTest.java:83-92`), a Legacy State (`:109-110`), one State both initial and
terminal (`:164-171`), and the duplicate-edge refusal (`:214`). Not covered: a cycle, a State no
Transition touches, and a version with no terminal State at all.

**Why it fits.** Cheapest item here, and it is the paragraph B's builder already wrote made
executable — `requireEveryTransitionToNameDeclaredStates`'s javadoc says reachability is deliberately
*not* checked and names `DRAFT` and the Legacy State as the reasons (`:249-258`). Three fixtures:

```java
builder(1).initialState("A").terminalState("Z")
    .transition("A", "GO",   "B", NONE)
    .transition("B", "BACK", "A", NONE)   // a cycle is a valid graph
    .state("ORPHAN")                      // and so is a State nothing reaches
    .build();                             // and so is one with no terminal State
```

**Cost.** One test class grows by about thirty lines. This is A's *plan* rather than A's code, and it
is the one item where A's document is ahead of both trees.

---

## 7. A compiled-graph cache — **with modification, and later**

**What A has.** `definition/CompiledGraphCache.java` (24 lines), `ConcurrentHashMap.computeIfAbsent`
keyed on the Definition Version row id, unbounded, targeted invalidation, one test; behind
`definition/CompiledGraphSource.java`, a functional interface with a test fake.

**What B has.** Neither, deferred with reasons (`findings.md` Part 5, rows 2 and 3): "with no loader
there is nothing to cache", and "a port with one adapter is the indirection `/codebase-design` warns
against".

**Why the argument fits even though the code should wait.** B's F1 is the first concrete case for a
cache rather than an assertion that one is wanted: terminality has to be asked of each Resource's
*own* pinned version, so `SiblingResource` carries a `CompiledGraph` per Resource
(`graph/EvaluationContext.java:158-168`) and a caller resolves N graphs before it calls. A built the
cache and did not connect it to that argument; B made the argument and did not build the cache. They
are the two halves of one item.

But A's 24 lines have exactly one caller today and it is a test, so the code can land with the loader.
What should not wait is writing down the two things that change when it does:

**Drop `CompiledGraphSource`.** A's own artifact flags it — "one adapter alone would make it
hypothetical indirection" (`judgement-calls.md:50-52`) — and B declined to declare it for the same
reason. A package-private cache with exactly one loader can name the loader directly; the interface
arrives when the second adapter does.

**Do not keep `computeIfAbsent`.** This is the comparison's §9.4. `graphs.computeIfAbsent(id,
source::load)` runs the loader inside the map's per-bin lock. Harmless while the loader is a test
lambda; the real one issues six queries in a transaction, and one that reached back into the same
cache would deadlock or throw — which F1's N-way sibling resolution makes a live possibility rather
than a hypothetical, since compiling one graph needs no siblings but a *caller* assembling a context
resolves several. Use `get`, load outside the map, `putIfAbsent`. Two threads may then compile the same
version once each; compilation is pure and idempotent, so that is duplicated work rather than a wrong
answer — the right trade against a lock held across I/O.

**Keep** A's key decision (the row id alone, which ADR 0001 fixes) and its not caching failures.

---

## 8. One thing A establishes that is a sentence in B, not a class

A's `evaluation/NegotiationPostVisibilityWriter.java:8-24` is a real adapter onto
`NegotiationService`. Under a ticket that excludes writes, B's throwing `UnbuiltPostVisibility` is the
better placeholder, and the comparison's §8 says why: A's arrangement leaves a working write path in
production code, B's makes an early call fail loudly and makes deleting the class the owning slab's
announcement.

What A establishes is only that the target is real: `NegotiationService.setPrivatePostsEnabled` and
`setPublicPostsEnabled`, both `(String negotiationId, boolean enabled)`, exist at
`negotiation/NegotiationService.java:100` and `:108`. Worth one line in `UnbuiltPostVisibility`'s
javadoc so the coupling slab knows the seam it is asked to fill already fits something.

---

## 9. What does not graft, and why

| A's idea | Why not |
|---|---|
| `StrategyCall` as pure data, dispatched at fire time | §13.1, the one decision that is not combinable. Nothing in this document requires reopening it. What B gives up is narrow: a step exposes `typeKey()` (`graph/GuardStep.java:18-19`) and a `toString` (`evaluation/GuardRegistry.java:80-82`), so a chain is still printable; only the params are gone, and B's own `findings.md:248-249` says the compiled graph is not what admin tooling should read either way. |
| `State` carrying a `label` | Nothing in either evaluator reads it, and A requires it non-null (`graph/CompiledGraph.java:127-132`), so every fixture must invent one. It is presentation data in the type graft 4 keeps JDK-only. A's real point here is different and already settled in B's favour: the *move* of `RequiredAuthority` discarded 18 lines of pre-existing javadoc in A and kept it in B (§10). |
| Pre-computed `Set<RequiredAuthority>` on the context | §13.3, and this is the side with the ADR 0007 hole: A's `case NONE -> true` (`evaluation/TransitionEvaluator.java:64-68`) is reachable by the system caller, so A's Orchestration Trigger can fire every unguarded open Event. B enforces both halves and tests the second over all four human authorities. |
| The nullable six-component `EvaluationResult` | §13.7. A caller of B's sealed pair cannot reach a field its case does not have. |
| `Void` instead of `NoParams` | A's stated reason is to avoid a public marker value *in the graph* (`judgement-calls.md:29-31`). B's `NoParams` is in `evaluation`, so the reason does not apply — and graft 3 is easier to write against a named type than against `Void`. |
| A Transition must name a *declared* Event | A's `validate` refuses one that does not (`graph/CompiledGraph.java:94-96`). In B a Transition auto-declares its Event (`:209-214`), which is what makes a fixture one line, and the composite FK already makes the violation impossible on the only path that reads rows. The check would be vacuous and the ergonomics are real. |
| Definition-level Guards folded in the graph constructor | A folds in `CompiledGraph` (`:35`, `:120-125`), B in the compiler (`definition/DefinitionCompiler.java:107-119`). The comparison finds them equivalent in cost. B's split is coherent as it stands: topology is an invariant and lives in the builder; the two Guard *scopes* are a definition concept and stay in the package that knows what a scope is. |
| The evaluator not being a Spring bean | A's is unannotated and its Information Requirement port has no bean, which is why A's `ApplicationTest` proves the registries fold but not that the evaluator wires (§8). B's `@Component` is the stronger of the two. |
| `EvaluationContext.human()` / `systemCaller()` | B already has the better-typed equivalents: `forNegotiation`/`forResource` (`graph/EvaluationContext.java:40-54`), `Caller.person`/`Caller.system` (`:85-91`), `Subject.negotiation`/`Subject.resource` (`:128-144`). A's factories leave `parentNegotiationState` null, which B's `forResource` requires. |

---

## 10. What this does to the comparison's open list

Grafts 4, 5 and 6 touch no decision at all — they are gates and tests, and they can land before the
PRD exists.

Graft 1 **settles §13.4** in the direction both prototypes' plans already state and only A's code
does, and hands the cutover slab one short rule to write. Graft 3 closes the first of the two
"smaller items to carry forward" at the end of §13, and goes one step past both prototypes by making
the rule independent of which `ObjectMapper` the container built. Graft 7 answers §13.5 as *with the
loader, minus the port, without `computeIfAbsent`* — which retires the second trailing item as well.
Graft 2 adds one name to §13.8's vocabulary list.

§13.1, §13.2, §13.3, §13.6 and §13.7 are untouched by everything here. That is the useful result: A's
transplantable value sits almost entirely outside the decisions the two prototypes actually disagree
about, so the PRD can take all seven grafts and still choose freely on the five that remain.
