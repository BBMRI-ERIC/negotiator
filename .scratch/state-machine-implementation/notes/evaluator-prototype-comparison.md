# Prototypes A and B of the compiled-graph package: an architectural comparison

Comparison of the two independent prototypes of map ticket
[13](state-machine-implementation/issues/13-prototype-the-compiled-graph-package.md), which prototypes
[09 Transition Evaluator core](state-machine-implementation/issues/09-transition-evaluator-core.md).

- **A** — branch `proto/evaluator-a`, one commit `39215517` on base `6803f8e6`. Artifacts:
  `.scratch/compiled-graph-prototype/PLAN.md` (61 lines) and `judgement-calls.md` (72 lines).
- **B** — branch `proto/evaluator-b`, six commits `5a4dc902..91b901d8` on the same base. Artifacts:
  `.scratch/compiled-graph-prototype/PLAN.md` (265 lines) and `.scratch/evaluator-b/findings.md`
  (517 lines).

This report does not rank the prototypes and does not recommend one. It records what each one built,
where the two differ structurally, and what each difference costs later. The axis used for the
structural reading is the `codebase-design` vocabulary: module, interface, implementation, depth,
seam, adapter, leverage, locality.

**Method.** Both trees were read in full by one reader, in one pass each, so no part of the
comparison is delegated or averaged. Every measurement below (line counts, test counts, formatter
check, Spring context boot, the Jackson probe in §9.3) was produced by this session running the same
command against both branches, not taken from either prototype's own artifact. Where a number
differs from what an artifact claims, both are given. Paths are relative to
`backend/src/main/java/eu/bbmri_eric/negotiator/` for production code and
`backend/src/test/java/eu/bbmri_eric/negotiator/` for tests.

---

## 1. Measured size and verification

| | A | B |
|---|---|---|
| Commits on top of `6803f8e6` | 1 | 6 |
| Diff against base | +1,660 / −32, 42 files | +4,639 / −5, 42 files |
| New production files | 24 | 28 (27 new, `RequiredAuthority` moved) |
| New production lines | 833 | 1,959 |
| — `lifecycle/graph` | 188 lines, 3 files | 775 lines, 11 files |
| — `lifecycle/evaluation` | 520 lines, 17 files | 943 lines, 15 files |
| — additions to `lifecycle/definition` | 125 lines, 4 files | 241 lines, 2 files |
| New test files / lines | 8 files, 637 lines | 6 files, 1,868 lines |
| Comment lines in new production files | 26 | 747 |
| Spring beans declared (`@Component`) | 7 | 9, including the evaluator |

Verification runs, executed here against both worktrees with the repository's own JDK 21 and Maven
3.9.16 from `flake.nix`:

| Check | A | B |
|---|---|---|
| `mvn compile` | passes | passes |
| Focused tests | 21 tests in 9 classes, 0 failures, 17.1 s | 104 tests in 7 classes, 0 failures, 22.6 s |
| — of which new to the prototype | 15 in 8 classes | 98 in 6 classes |
| — of which the amended inertness guard | 6 | 6 |
| `com.spotify.fmt:fmt-maven-plugin:check` (Google Java Style, `pom.xml:80-84`) | clean | clean |
| `ApplicationTest` (whole Spring context, Postgres testcontainer) | passes, 25.9 s | passes, 27.3 s |

Two counts in the artifacts do not match the trees. B's `findings.md:29` says "21 production files,
100 tests in six classes"; the trees hold 28 production files and the surefire reports on that branch
record 98 tests in those six classes (23 + 19 + 8 + 21 + 23 + 4), which is also what re-running them
here produced. A's issue-13 entry records "21 focused prototype and structural-gate tests passed",
which matches exactly, and records that the full backend suite was started and aborted; B ran the
parity gate and recorded it unchanged at 255 tests in 24 classes.

Neither prototype's new tests carry `@DisplayName`, which the pre-existing lifecycle tests use on
every method (for example `definition/StateRepositoryTest.java`). That deviation is common to both.

---

## 2. The two designs in one paragraph each

**A.** Three packages. `lifecycle/graph` holds 3 files: `CompiledGraph` with `State`, `Event`,
`StrategyCall` and `Transition` as nested public records, an `InvalidGraphException`, and
`RequiredAuthority`. A `StrategyCall` is `(String typeKey, Object parameters)` — a type key paired
with already-deserialized parameters (`graph/CompiledGraph.java:141-145`). `lifecycle/evaluation`
holds the evaluator, the two strategy contracts, the two registries, the evaluation context and
result types, and the four ported strategies. The evaluator holds a `GuardRegistry` and resolves each
`StrategyCall`'s type key against it at fire time
(`evaluation/TransitionEvaluator.java:52-58`, `evaluation/GuardRegistry.java:30-33`).
`lifecycle/definition` gains a package-private compiler, a package-private
`MaterializedDefinition` input record, and a compiled-graph cache behind a `CompiledGraphSource`
seam.

**B.** The same three packages with different contents. `lifecycle/graph` holds 11 files: the graph
behind a builder, `CompiledTransition`, `GuardStep` and `ActionStep` as callable interfaces,
`GuardVerdict`, `FailureCategory`, `EvaluationContext` with its nested `Caller`, `Subject` and
`SiblingResource`, `ActionContext`, `RequiredAuthority`, and — the load-bearing part —
`GuardCatalogue` and `ActionCatalogue`, two one-method interfaces the registries implement and the
compiler calls. Type-key resolution and JSON deserialization both happen while compiling, so a
`GuardStep` is a closure with no type key to dispatch on (`graph/GuardStep.java:16-23`,
`evaluation/GuardRegistry.java:66-84`). The evaluator's only constructor argument is the Information
Requirement port. No cache and no load seam were built.

---

## 3. The difference that drives most of the others: where the seam sits

Both `PLAN.md` files draw the same package diagram. The trees do not match it in the same way.

In A, `definition` imports the two concrete registry classes from `evaluation`:

```
definition/DefinitionGraphCompiler.java:3-4
  import eu.bbmri_eric.negotiator.lifecycle.evaluation.ActionRegistry;
  import eu.bbmri_eric.negotiator.lifecycle.evaluation.GuardRegistry;
```

In B, no file in `definition` names `evaluation` at all; the compiler's two constructor arguments are
`GuardCatalogue` and `ActionCatalogue`, declared in `graph`
(`definition/DefinitionCompiler.java:37-43`). B's purity gate asserts the absence of that edge in
both directions (`lifecycle/evaluation/EvaluatorPurityGuardTest.java:93-133`).

In `codebase-design` terms, A has one adapter-free dependency on a concrete implementation, and B has
a seam with two adapters across it (the registry in production, a lambda or fake in tests). The
consequences are visible in the compiler tests. A's compiler test must build both real registries,
each with a real `ObjectMapper` and a list of test strategies, before it can compile anything
(`lifecycle/definition/DefinitionGraphCompilerTest.java:72-73`). B's compiler test hands the compiler
whatever it wants a row to bind to and asserts on the calls
(`lifecycle/definition/DefinitionCompilerTest.java:284-311`, which is how it can assert that a
definition-wide row is bound once however many Transitions carry it).

Three further consequences follow from the seam's placement rather than from anyone's intent:

- **What has to be public.** A's registries and its strategy classes are public because `definition`
  and its tests reach them (`evaluation/NegotiationApprovedGuard.java:7-9` is a public class with a
  public `TYPE_KEY`). B's strategies are package-private with package-private keys
  (`evaluation/NegotiationApprovedGuard.java:40-42`), because only same-package tests use them.
- **What the purity gate can promise.** B's gate can forbid the `definition`↔`evaluation` edge
  because there is none. A's cannot, because the edge is the design.
- **Where a third construction path would attach.** Both prototypes anticipate a future loader.
  In B it attaches in `definition` against the catalogues. In A it attaches in `definition` against
  `GuardRegistry` and `ActionRegistry` directly, so the loader inherits a dependency on the Spring
  bean that owns Jackson.

---

## 4. Where strategy dispatch happens

This is the choice both artifacts identify as the one that has to be decided rather than merged
(B's `findings.md:482-486`; A's `judgement-calls.md:8-15`).

**A: the graph is data, the registry dispatches at fire time.** `StrategyCall` carries the key and the
parameters as `Object`. Each evaluation resolves the key again and casts:

```
evaluation/GuardRegistry.java:30-33   evaluate(call, ctx) -> evaluate(require(call.typeKey()), ...)
evaluation/GuardRegistry.java:56-59   strategy.evaluate(context, strategy.parametersType().cast(parameters))
```

**B: the graph holds bound closures, the registry dispatches once at compile time.**
`GuardRegistry.bindTyped` returns an anonymous `GuardStep` closing over the typed parameters
(`evaluation/GuardRegistry.java:66-84`); the evaluator's Guard loop is
`for (GuardStep guard : transition.guards()) { guard.check(context); }`
(`evaluation/TransitionEvaluator.java:102-107`) and names no key and no map.

Consequences:

| | A | B |
|---|---|---|
| Unknown type key discovered | at fire time as well as at compile time — `GuardRegistry.java:61-67` throws `IllegalArgumentException` on lookup, and `CompiledGraph.StrategyCall` is public and constructible with any string | only at compile time; the evaluator has no key to fail on |
| Work per Guard per evaluation | one map lookup plus one `Class.cast` | none |
| Work per Guard per Possible Events listing | that, times the candidate Transitions | none |
| Graph inspectability | a reader can see each Guard's key and parameters | a step exposes only `typeKey()`, for messages (`graph/GuardStep.java:18-19`); parameters are gone |
| Untyped element in the graph's public interface | yes — `StrategyCall.parameters()` is `Object` | no |
| Immutability of a compiled graph | depends on the strategy's parameter type being immutable, recorded as an open question in `judgement-calls.md:10-15` | same underlying dependency, but not exposed at the interface |

B's compiler binds each definition-wide row once and shares the resulting step across every
Transition of the version (`definition/DefinitionCompiler.java:107-119`). A's compiler binds the
definition-level rows once as well and concatenates the same references into each Transition inside
the graph constructor (`graph/CompiledGraph.java:35`, `120-125`). Both therefore parse a
definition-wide row's JSON once; the difference is only where the concatenation lives.

---

## 5. What a compiled graph is, and what it refuses

**A.** A constructor with five arguments — version id, States, Events, definition-level Guard calls,
Transitions (`graph/CompiledGraph.java:20-25`). States are records carrying `name`, `label`,
`initial`, `terminal`, with `label` required non-null (`:127-132`). It enforces: unique State names,
unique Event names, exactly one initial State, every Transition naming a declared State and Event,
and at most one Transition per `(source, event)`; failures raise `InvalidGraphException`
(`:87-118`). Every lookup validates the State first, so an unknown current State raises
`InvalidGraphException("Unknown State: …")` (`:79-85`), including through `transitionsFrom`, which is
the Possible Events path.

**B.** A builder (`graph/CompiledGraph.java:162-309`). States and Events are name sets, not types;
labels and descriptions are absent, attributed to the metadata ticket (`:158` of `findings.md`). It
enforces: exactly one initial State, every Transition naming declared States, and at most one
Transition per `(fromState, event)`, all as `IllegalStateException` from `build()`. Unique names are
free because the collections are sets. Unknown-State handling is asymmetric: `isTerminal` throws with
a message naming the version and its States (`:120-127`), while `transition` and `transitionsFrom`
answer `Optional.empty()` and an empty list for a State the version does not declare
(`:135-146`).

That asymmetry is worth naming because it is the one place B's code does not do what B's plan says.
`PLAN.md:94-96` states "Unknown current State is graph corruption and should fail distinctly. A known
Event with no Transition from the current State is an ordinary unavailable move." In the tree, a
corrupt pin reaches the evaluator and comes back as the ordinary refusal
`NO_TRANSITION_FOR_EVENT` (`evaluation/TransitionEvaluator.java:61-67`), or as an empty Possible
Events set. `declaresState` exists and is public but no production caller uses it — the only use of
either predicate is `declaresEvent` at `TransitionEvaluator.java:55`. A's version does fail distinctly
on that input, from the same lookups.

The Event universe is the other substantive graph difference. A carries declared Events and can
answer `event(name)`, but its evaluator does not consult it: an Event with no Transition from the
current State and an Event the version never declared both produce
`RefusalCategory.NO_TRANSITION` with reason code `"NO_TRANSITION"`
(`evaluation/TransitionEvaluator.java:22-28`). B splits them into `UNKNOWN_EVENT` and
`NO_TRANSITION_FOR_EVENT` (`:55-67`), which is what makes an Override Event distinguishable from a
typo at the evaluator's interface.

Interface weight, for the same fixture. A four-State fixture graph in A is four `State` records with a
label each plus explicit `Event` records (`lifecycle/graph/CompiledGraphTest.java:15-31`). In B it is
`builder(id).initialState("X").terminalState("Y").transition("X","GO","Y",NONE)`, with each
Transition's Event auto-declared (`graph/CompiledGraph.java:209-214`). B's is the smaller interface to
learn per fixture; A's carries the label the entity has and B's discards it.

---

## 6. The evaluation context and the authority rule

This is the second large divergence, and neither artifact frames it as one.

**A.** `EvaluationContext(boolean system, Set<RequiredAuthority> authorities, String
parentNegotiationState, List<ResourceLifecycle> resources)` (`evaluation/EvaluationContext.java:8-12`).
The caller decides which authorities it holds — including `IS_CREATOR` and `IS_REPRESENTATIVE` — and
the evaluator checks set membership (`evaluation/TransitionEvaluator.java:62-69`).

**B.** `EvaluationContext(Caller caller, Subject subject, String parentNegotiationState,
List<SiblingResource> siblingResources)` (`graph/EvaluationContext.java:24-28`). `Caller` is a sealed
interface with `Person(personId, authorities)` and `TheSystem()` (`:68-92`). `Subject` carries
`negotiationCreatorId` and `representativeIds` as raw facts (`:115-126`), and the evaluator computes
creator-ness and representative-ness itself (`evaluation/TransitionEvaluator.java:137-152`).

Consequences:

- **Where the authority rule lives.** In A, "is this caller the creator" is answered before the
  evaluator is called, once per call site. In B it is answered inside the single evaluator. ADR 0001's
  stated reason for having one evaluator is that the two Lifecycles cannot disagree about how a move
  is judged; A leaves that rule outside the shared component for four of the five authority values,
  B does not.
- **ADR 0007's "if and only if".** B enforces both halves: a human cannot satisfy `SYSTEM`
  (`:149`), and the system caller satisfies `SYSTEM` and nothing else (`:140`). A enforces one half:
  `case SYSTEM -> context.system()` refuses humans, but `case NONE -> true`
  (`evaluation/TransitionEvaluator.java:64-68`) is reached by a system caller as well, so in A the
  Orchestration Trigger can fire every unguarded open Event. B tests the second half over all four
  human authorities (`lifecycle/evaluation/TransitionEvaluatorTest.java:166`).
- **Cost of a new Guard input.** Both put runtime facts on one record, so both make a new input a
  visible change in one place. A's record is 29 lines, B's is 170 including the javadoc that explains
  each field's presence.
- **The N-way load.** Both pair each Resource with its own compiled graph so terminality is a question
  put to that Resource's pinned version (A: `EvaluationContext.ResourceLifecycle`,
  `evaluation/EvaluationContext.java:27-28`; B: `SiblingResource`, `graph/EvaluationContext.java:158-168`).
  Both therefore make the caller resolve N graphs before calling. B records this as the concrete
  argument for the cache (`findings.md:328-353`); A built the cache but did not connect it to this
  argument.

---

## 7. The outcome type

A returns one record with six components, three of which are null in the permitted case and two in
the refused case: `EvaluationResult(permitted, targetState, actions, refusalCategory, reasonCode,
details)` with static `permit` and `refuse` factories
(`evaluation/EvaluationResult.java:8-28`). B returns a sealed interface with
`Permitted(CompiledTransition)` and `Refused(category, reasonCode, details)`
(`evaluation/EvaluationOutcome.java:18-67`).

The consequence is on the calling side, which is the cutover slab's: A's caller reads a boolean and
then fields that may be null; B's caller pattern-matches and cannot reach a field the case does not
have. Both keep the 403/422/409 mapping out of the package. Both categorise a Guard refusal as
`DOMAIN_STATE_CONFLICT` from the stage rather than from the Guard: A because `GuardResult` has no
category field (`evaluation/GuardResult.java:7`), B because `GuardVerdict` deliberately has none and
says so (`graph/GuardVerdict.java:9-13`). Both enums carry four values, not three — A's
`RefusalCategory.NO_TRANSITION` (`evaluation/RefusalCategory.java:4`) and B's
`FailureCategory.NO_TRANSITION` (`graph/FailureCategory.java:26`).

Reason codes differ in kind, and the PRD has to pick: A emits bare codes with empty details in the
authority and no-transition cases (`Map.of()` at `TransitionEvaluator.java:27` and `:44`); B attaches
details at every refusal — the wanted authority, the event and version, the unfinished Resource ids
(`TransitionEvaluator.java:59`, `:66`, `:95`; `TerminalAggregationGuard.java:58-66`) — and adds the
refusing Guard's key to the details map (`:116-121`).

---

## 8. What each prototype built that the other did not

| | A | B |
|---|---|---|
| Compiled-graph cache | built: `definition/CompiledGraphCache.java` (24 lines), `ConcurrentHashMap.computeIfAbsent`, keyed on the row id, unbounded, targeted invalidation, one test | not built; deferred with the reason recorded (`findings.md:408`) |
| Load seam | built: `definition/CompiledGraphSource.java`, a functional interface with a test fake; the artifact flags that one adapter makes it hypothetical (`judgement-calls.md:50-52`) | not built, on the same reasoning stated in advance (`findings.md:409`) |
| Row-provenance check | none | `DefinitionCompiler.requireRowsBelongToTheVersion`, 35 lines, refuses rows from another version (`definition/DefinitionCompiler.java:163-197`) |
| Cross-catalogue misuse | not covered | tested: an Action key wired as a Guard is refused (`DefinitionCompilerTest.java:405`) |
| Parameters supplied to a no-parameter strategy | rejected explicitly (`evaluation/GuardRegistry.java:36-40`, `ActionRegistry.java:35-39`) | not rejected — see §9.3 |
| Post-visibility write path | a real adapter bean onto `NegotiationService` (`evaluation/NegotiationPostVisibilityWriter.java:8-24`) | a bean that throws (`evaluation/UnbuiltPostVisibility.java:17-35`) |
| Information Requirement port bean | none; the port is a `@FunctionalInterface` supplied by tests (`evaluation/InformationRequirementStage.java:4-7`) | a bean that throws (`evaluation/UnbuiltInformationRequirementSatisfaction.java:21-31`) |
| Evaluator as a Spring bean | not annotated, so the container holds no evaluator | `@Component` (`evaluation/TransitionEvaluator.java:40`), so the whole wiring is exercised by `ApplicationTest` |

Two of these rows are the same decision taken opposite ways. Issue 09 excludes anything that writes.
A satisfies that by having no caller for its writer, while leaving a working write path in production
code; B satisfies it by having the write path refuse, and records deleting that class as how the
owning slab announces itself. A's arrangement means the Action chain works the day someone calls it;
B's means calling it early fails loudly. The same pattern repeats for the Information Requirement
port: A's absence of a bean means the evaluator cannot be constructed by the container at all, which
is why A's `ApplicationTest` proves the registries fold but not that the evaluator wires.

---

## 9. Reliability findings from reading the code

### 9.1 The structural gates are asymmetric in strength

A ships two gates, 74 test lines total:

- `lifecycle/graph/CompiledGraphPurityTest.java` (49 lines): every `import` line in the graph package
  must start with `import java.`. Strong for what it checks, and it asserts the source list is not
  empty (`:19`). Two limits: a fully-qualified reference without an import is invisible to it, and the
  root is resolved as `Path.of("src/main/java/…")` relative to the working directory (`:34`), so it
  depends on the run being rooted at `backend/`.
- `lifecycle/evaluation/TransitionEvaluatorPurityTest.java` (25 lines): reflection over
  `TransitionEvaluator.class.getConstructors()[0]`, asserting no parameter type is under
  `jakarta.persistence` or `org.springframework.data` or has a name containing `Repository`. It reads
  one arbitrarily-chosen constructor, checks parameters only, and does not look at fields, method
  bodies or the rest of the package.

B ships one gate, 271 test lines (`lifecycle/evaluation/EvaluatorPurityGuardTest.java`), built in the
register of the existing `DefinitionInertnessGuardTest`: a text scan over both packages with comments
blanked so prose is not a violation, `file:line` reporting, a repository rule written as the suffix
`\b\w*Repository\b` so a repository that does not exist yet is also forbidden (`:63`), the
`definition`-edge rule in both directions (`:93-133`), a minimum-source-count check (`:45`, `:143-147`),
positive and negative regex fixtures (`:149-160`), and a `moduleRoot()` that walks upward and throws
rather than passing by finding nothing (`:233-248`).

One defect in B's gate: at `:145-147` the `.formatted(...)` binds only to the second half of a
concatenated literal, so the failure message renders the scanned count where the minimum belongs and
leaves the first `%d` literal. It affects the message only, never the pass/fail decision.

### 9.2 The inertness guard was amended two different ways

Both had to remove `RequiredAuthority` from `DISTINCTIVE_TYPE_NAMES`, and both did.

- A also **added four names** — `MaterializedDefinition`, `DefinitionGraphCompiler`,
  `CompiledGraphSource`, `CompiledGraphCache` — so the guard now also proves that no production code
  outside `definition` names A's own new package-private types
  (`lifecycle/definition/DefinitionInertnessGuardTest.java`, diff at `:85-94`).
- B added 26 lines of javadoc instead, recording why the name came off, why `DefinitionScope` stays,
  which sibling test makes the removal impossible to fake, and that this is not a precedent for
  trimming the list one name at a time (diff at `:72-98`).

Both amendments are truthful, and the guard's own `guard_forbidsOnlyNamesThatStillExist` passes on
both branches. They protect different things: A's extends the gate's reach to its new types, B's
protects the gate against being weakened by the next person.

### 9.3 A no-parameter strategy given parameters: A refuses, B accepts silently

A's registries reject a non-null `params` for a strategy declaring `Void`
(`evaluation/GuardRegistry.java:36-40`, `ActionRegistry.java:35-39`). B's read the blob into
`NoParams` instead (`evaluation/GuardRegistry.java:86-98`), and what happens then depends on the
`ObjectMapper`:

```
strict (new ObjectMapper):                          threw UnrecognizedPropertyException
boot-like (FAIL_ON_UNKNOWN_PROPERTIES off):         accepted -> NoParams[]
```

That probe was run here against this project's own dependency set. In production both registries are
injected with the Spring `ObjectMapper` bean, and Spring Boot disables
`FAIL_ON_UNKNOWN_PROPERTIES` by default; this repository does not re-enable it
(`backend/src/main/resources/application.yaml:54-55` sets only `date-format`, and
`common/configuration/BaseConfig.java:42` customises only `JsonNullable`). So in B a Wiring row that
carries configuration for a strategy that takes none is accepted and the configuration is discarded,
while B's own registry tests construct `new ObjectMapper()`
(`lifecycle/evaluation/GuardRegistryTest.java:30`) and would therefore never see that behaviour.
Neither prototype has a test for this input. A's protection is in the code and untested; B has no
protection.

### 9.4 A's cache holds compilation inside `computeIfAbsent`

`CompiledGraphCache.get` is `graphs.computeIfAbsent(definitionVersionId, source::load)`
(`definition/CompiledGraphCache.java:17-19`). This gives one compilation per id and does not cache
failures, which is what the artifact claims. It also means the eventual repository-backed loader will
run its queries inside the map's per-bin lock, and that a loader which reached back into the same
cache would deadlock or throw. Neither is a defect today, because the only loader is a test lambda;
both are constraints the loader slab inherits.

### 9.5 Fire-time failure surface

A's evaluator can throw where B's cannot: `IllegalArgumentException` for a Guard key the registry does
not know (`evaluation/GuardRegistry.java:61-67`) and `InvalidGraphException` for a current State the
graph does not declare (`graph/CompiledGraph.java:79-85`), both from inside `evaluate` and
`possibleEvents`. B's evaluator has no lookup to fail and tolerates the unknown State as described in
§5. Whichever is chosen, the cutover slab needs a rule for these, because a Possible Events listing
that throws and one that silently returns an empty set are different failure modes for the same
corrupt pin.

---

## 10. Documentation and conformance to the surrounding code

The repository documents its style in two places only: Google Java Style enforced by
`fmt-maven-plugin` (`docs/contributing.md:58-65`, `backend/pom.xml:80-84`), and the binding glossary
in `backend/CONTEXT.md` with `CONTEXT-MAP.md:18` making its `_Avoid_` lines binding. Both prototypes
pass the formatter and both use the glossary's terms. Everything else is observed practice, and the
practice in this subsystem is heavy javadoc carrying rationale rather than restating signatures
(`definition/Transition.java:40-44`, `definition/LifecycleDefinitionRepository.java:12-16`).

Against that practice the two prototypes are far apart: 26 comment lines in A's 833 new production
lines, 747 in B's 1,959. The clearest single instance is the enum both were told to move. At the base
commit `definition/RequiredAuthority.java` is 29 lines, of which 18 are javadoc, including the note
that six of the eight Negotiation Transitions are behaviourally `IS_ADMIN OR IS_CREATOR` and that
inventing a disjunction is not this type's call. A's `graph/RequiredAuthority.java` is 10 lines with a
one-line javadoc and five bare constants: the move discarded documentation that existed before the
prototype. B's is 35 lines — the original text plus a paragraph on why the type is vocabulary rather
than schema.

One convention deviation is A's alone: `evaluation/NegotiationApprovedGuard.java:23` spells
`"IN_PROGRESS"` as a literal, while `lifecycle/WellKnownNegotiationStates.java:52` holds that exact
constant in the parent package for exactly this purpose, and B uses it
(`evaluation/NegotiationApprovedGuard.java:57`). No guard test forbids the literal in Java — the
existing `RawStateNamesInSqlGuardTest` pins names in SQL only — so this is a convention miss rather
than a failing build.

The artifacts themselves differ in kind, not only in length. A's 72-line `judgement-calls.md` states
each decision and its reason. B's 517-line `findings.md` numbers each one, names the rejected
alternative, and records what breaking the rule looked like: which two of the inertness guard's six
tests went red before the amendment and with what messages (`:109-117`), that an attempt to red the
purity gate with a `StateRepository` field would not compile because the type is unreachable
(`:127-131`), and that the recon document's bean pattern for `SET_POST_VISIBILITY` fails the boot
because three beans would claim one key (`:270-283`) — asserted in the tree at
`lifecycle/evaluation/LifecycleStrategiesTest.java:226`.

---

## 11. The `codebase-design` reading

**Depth of the graph module.** A's graph package presents a five-argument constructor, seven lookups
and four nested public records over 188 lines. B's presents a builder with six declaring methods,
seven accessors and one public record over 775 lines, plus the vocabulary the other two packages share.
Per unit of interface a caller must learn, B's graph hides more: name-set membership, auto-declaration
of an Event from its Transition, and the whole of what a Guard step is. A's hides less by design,
because its `StrategyCall` is data a caller can read — which is leverage if anything needs to read it,
and interface surface if nothing does. Nothing in either tree reads it, and B's `findings.md:248-249`
notes that the compiled graph is not a good source for admin tooling either way.

**The deletion test.** Delete A's graph package and its topology validation, indexing and Guard-chain
composition reappear split between the compiler and the evaluator; the `StrategyCall` record itself
would be reconstructed as-is. Delete B's graph package and the same three concerns reappear, plus the
`GuardStep`/`ActionStep` abstraction and the two catalogue interfaces — which is to say the seam
between the other two packages disappears and they become mutually dependent. Both packages earn
their keep; B's carries more that would have to be rebuilt.

**Seams and adapters.** The rule that one adapter means a hypothetical seam and two means a real one
cuts differently in three places:

- A's `CompiledGraphSource` has one adapter, a test fake. A's own artifact records the reservation
  (`judgement-calls.md:50-52`). B declined to declare the same port for the same stated reason
  (`findings.md:409`).
- B's `GuardCatalogue` and `ActionCatalogue` have two adapters each — the registry in production, a
  fake in the compiler tests — and something real varies across them, which is that `definition` must
  not know `evaluation` exists.
- B's `PostVisibility` and `InformationRequirementSatisfaction` each have one production adapter that
  refuses plus test doubles. They are seams for a stated reason: they mark work the next slab must
  claim, and deleting the refusing class is the announcement.

**The interface as the test surface.** B tests only through module interfaces and needs no access to
internals; its 98 tests are close to one behaviour each, which is why a failure names the behaviour.
A's 15 tests each assert several behaviours at once — `TransitionEvaluatorTest.java:22-34` asserts
five things about authority, target State and Possible Events in one method — so a regression there
reports one failure covering several rules. Both approaches pass; they differ in what a red test
tells the next maintainer.

**Locality.** A concentrates dispatch in the two registries and authority resolution in its callers.
B concentrates dispatch at compile time and authority resolution in the evaluator. The maintainer's
question "where do I change how a move is judged" therefore has one answer in B and two in A.

---

## 12. Three claims in B's own comparison that its code cannot support

B's `findings.md` Part 7 states plainly that it was written from A's two documents and not from A's
Java (`:443-451`). Read against A's tree, three of its rows are wrong, and all three are the kind of
error that framing predicts:

1. "`FailureCategory` has a fourth value, not three" is listed as being in B and not in A
   (`:504-505`). A has the same fourth value, `RefusalCategory.NO_TRANSITION`
   (`evaluation/RefusalCategory.java:4`). What is genuinely B-only is splitting it into two reason
   codes.
2. "Cycles asserted as valid graphs" is listed as being in A and not in B (`:515-516`). A's
   `PLAN.md:55` says cycles stay valid, but A has no test that builds one; neither prototype proves
   it.
3. The divergence table's silences are not evidence of absence, as B says — and in at least one place
   the silence hid a real divergence in B's favour, since A's artifact says nothing about the
   pre-computed `Set<RequiredAuthority>` on its context (§6), which is arguably a larger difference
   than four of the six rows the table lists.

Nothing here suggests bad faith; B labelled the limitation itself. It is recorded because the PRD will
be tempted to lift that table, and two of its rows would carry a false statement about A into the
specification.

---

## 13. What the PRD has to decide, and what each choice costs

Both artifacts agree the convergent parts are settled: three packages with `graph` at the bottom,
`RequiredAuthority` moving while `DefinitionScope` stays, State and Event identity as bare strings,
topology validated in the graph rather than in the compiler, definition-level Guards folded ahead of
Transition-level ones at construction, separate Guard and Action registries with duplicate keys
failing the boot, a compiler over already-materialized rows so the inertness guard survives, both
compiler and cache package-private, the four strategies with `NEGOTIATION_APPROVED` porting the live
imperative gate and `SET_POST_VISIBILITY` as one type with `PUBLIC | PRIVATE | BOTH`, spawn registered
and failing loudly, and purity plus inertness as structural gates. Reaching that list twice
independently is the strongest evidence in either branch.

What remains open, in the order the decisions constrain each other:

1. **Bound closures or pure data in the graph** (§4). Decides where a bad definition is discovered,
   whether the evaluator can throw at fire time, what a compiled graph can be inspected for, and
   whether the graph's public interface contains an `Object`. Not combinable.
2. **Where the `definition`↔`evaluation` seam sits** (§3). Largely follows from 1, but not entirely:
   pure-data steps could still be bound through catalogue interfaces. Decides what the compiler's
   tests need, what has to be public, and what the purity gate can promise.
3. **Whether authority facts reach the evaluator raw or pre-computed** (§6), and with it whether the
   `NONE`-and-the-system-caller hole is closed. Independent of 1 and 2.
4. **Unknown current State: refuse distinctly or answer as no-transition** (§5, §9.5), and the same
   question for an undeclared Event. Both prototypes' plans say the former; only A's code does it, and
   only for the State.
5. **Cache and load seam now or with the loader** (§8). A's cache and B's §F1 argument are
   complementary rather than competing; the open question is whether `CompiledGraphSource` survives
   the arrival of a real adapter.
6. **Placeholders that write or placeholders that refuse** (§8), which is also the decision about
   whether the evaluator is a Spring bean before it has a caller.
7. **Outcome type: nullable record or sealed pair** (§7), and how much detail a refusal carries.
8. **One vocabulary.** `RefusalCategory`/`GuardResult`/`EvaluationResult`/`GuardStrategy`/
   `ActionStrategy`/`PostVisibilityWriter`/`MaterializedDefinition` against
   `FailureCategory`/`GuardVerdict`/`EvaluationOutcome`/`Guard`/`Action`/`PostVisibility`/
   `DefinitionVersionRows`. `backend/CONTEXT.md` is binding on the domain words and names neither set,
   so the PRD picks and the glossary should then carry the result.

Two smaller items to carry forward whichever way the above go: the no-parameter-with-parameters hole
(§9.3) needs a rule and a test in either design, and the loader slab inherits the
`computeIfAbsent` constraint (§9.4) if A's cache is kept.
