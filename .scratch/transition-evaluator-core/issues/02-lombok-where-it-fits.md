# Lombok where it fits, with the three carve-outs

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Bring the adopted tree into house style. Lombok is not an optional flourish in this backend — 220 of
437 production files use it, at `provided` scope, and slab 08's six definition entities already
carry a fixed annotation set. New code that hand-writes what Lombok generates reads as a second
project inside the same package tree, which is the whole of why this slice exists (D11).

Two mechanical substitutions:

- **`@NonNull` replaces the hand-written null checks** on the graph's records — the evaluation
  context and its nested types, the compiled Transition, the action context, the verdict, and the
  graph builder. There is precedent for Lombok `@NonNull` on a record in this backend already.
- **`@RequiredArgsConstructor`** on the two registries and the compiler.

And three carve-outs, each of which must survive with its reason recorded in the code, because each
one looks like an oversight to the next reader:

1. **The graph's builder stays hand-written.** Lombok's `@Builder` generates one setter per field
   and calls a constructor; this builder is a different thing. Declaring a State as initial or
   terminal also declares it, declaring a Transition also declares its Event, and `build()` runs
   three validations and constructs two indexes. A generated builder would take the derived sets and
   both indexes *from the caller*, which is precisely the work this builder exists to remove — a
   fixture is one line per Transition and everything else is derived (user story 47).
2. **The Transition Evaluator's constructor stays hand-written.** Map ticket 09 requires that having
   no repository, `EntityManager` or Spring Data dependency "should be visible in its constructor".
   A generated constructor gives a reader nothing to look at.
3. **`@Nullable` is not adopted in `graph`.** Lombok has no such annotation and this backend's is
   `org.springframework.lang.Nullable` — a real runtime Spring dependency, and a far weaker case for
   the import allowlist than Lombok's source-retained annotations. Optional components stay
   documented in javadoc.

## Acceptance criteria

- [x] Every hand-written null check on the graph's records is a Lombok `@NonNull`. **All 21.** The
      exception type is preserved everywhere; the messages are Lombok's, which is accepted rather
      than worked around — see *the message was not worth a carve-out* below.
- [x] ~~The two registries and~~ the compiler uses `@RequiredArgsConstructor`. **The two registries
      keep hand-written constructors, accepted on review:** theirs fold a list into a map, and the
      constructor shows that without needing a comment. Four constructors the criterion never named
      were annotated instead — see below.
- [x] ~~The graph builder and the evaluator constructor are untouched, and each carries a short
      comment saying why Lombok is deliberately absent there.~~ **Retired on review.** Every
      absence-of-Lombok comment is gone, here and in three other classes, and the evaluator's
      constructor is now generated. The builder is still not a `@Builder`.
- [x] No `@Nullable` annotation is introduced anywhere in `graph`, and `graph` still imports nothing
      but `java.` and `lombok.`.
- [x] The formatter check passes. **One test assertion changed**, on review instruction — see below.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

**One count in the PRD does not match the tree, and the slice should settle it.** D11 says 18
hand-written null checks and enumerates where: the evaluation context and its nested types (8), the
compiled Transition (4), the action context (2), the verdict (1) and the graph builder (3). The tree
has **21** — the sealed outcome type carries three more that D11's enumeration does not mention.
Convert those three as well if they are ordinary argument checks; if either refused half of the
outcome is load-bearing in a way `@NonNull` would change, leave it and say so.

**This slice and the import allowlist rule interact, and safely.** Adding `lombok.` imports to
`graph` is legal under the allowlist only because D18 writes it as `java.` **and** `lombok.` from the
start. Lombok's annotations are `SOURCE` retention at `provided` scope, so nothing Lombok reaches the
runtime classpath and the property the purity gate protects stays literally true of the compiled
output. Whichever of the two slices lands second must not have to weaken the other.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)

## Outcome

Landed on `slice/02-lombok-where-it-fits`. **Ten production files changed, no test file touched** —
which is the strongest form of "no test assertion changes" this slice could deliver.

### The count question the slice was asked to settle

**21, not 18.** D11's enumeration is short by exactly the three the issue suspected, on the sealed
outcome type: `Permitted.transition`, `Refused.category` and `Refused.reasonCode`. All three are
ordinary argument checks with plain single-word messages and no factory funnelling them, so all
three converted. Neither refused half turned out to be load-bearing.

| Where | Checks | Converted |
|---|---|---|
| `EvaluationContext` + nested | 8 | 8 |
| `CompiledTransition` | 4 | 4 |
| `CompiledGraph.Builder` | 3 | 3 |
| `ActionContext` | 2 | 2 |
| `GuardVerdict` | 1 | 1 |
| `EvaluationOutcome` | 3 | 3 |
| **Total** | **21** | **21** |

**Sixteen are `@NonNull` on a record component, three on the builder's method parameters, and two on
a factory-method parameter**, and that last distinction is not cosmetic.
`EvaluationContext.parentNegotiationState` and
`Subject.resourceId` are legitimately null on the record — `forNegotiation` and `Subject.negotiation`
both pass null deliberately — while `forResource` and `Subject.resource` require them. Annotating
those components would have broken the other factory; the check belongs on the parameter, where it
already was.

### The message was not worth a carve-out

The first pass kept `CompiledTransition.requiredAuthority` hand-written to preserve the message
*"RequiredAuthority.NONE is how 'anyone' is spelled"*, which `CompiledGraphTest` asserted on.
**Reversed on review.** The component is now `@NonNull` like the other three, the test asserts on
`"requiredAuthority"` and its display name drops the clause about how NONE is spelled. Nothing was
lost: `RequiredAuthority.NONE`'s own javadoc already says "No authority is required of the caller",
which is the same fact in the place a reader looks for it. A per-field exception, a five-line
comment defending it, and a test pinned to prose is a large price for a sentence that lives on the
enum anyway.

This was the slice's only test-assertion change, and it makes the earlier claim of "no test file
touched at all" no longer true.

### The registries keep hand-written constructors, and say nothing about it

Their constructors do not assign their second field, they *derive* it: `(ObjectMapper,
List<Guard<?>>)` folds into `Map<String, Guard<?>>`. `@RequiredArgsConstructor` would generate
`(ObjectMapper, Map<String, Guard<?>>)`, which loses the fold and its collision rule, stops
`GuardRegistryTest` and `LifecycleStrategiesTest` compiling, and — quietest — boots anyway, because
**Spring satisfies a `Map<String, T>` parameter by injecting bean names as keys**, so the catalogue
would key on `negotiationApprovedGuard` instead of `PARENT_NEGOTIATION_APPROVED`.

**None of that is written in the code, deliberately.** The first pass explained it in nine lines of
javadoc on `GuardRegistry` and three more on `ActionRegistry`; both are gone. A constructor that
takes a `List` and assigns a `Map` shows a reader it is doing work, and that is enough — the reason
it cannot be generated is legible from the code, so a comment asserting it only ages. The reasoning
is recorded here instead, which is where a decision belongs.

### Six generated constructors, not the issue's three

The issue names the two registries and the compiler. **The rule is "use Lombok where it fits", not a
list**, so it was applied to every constructor in the subsystem that only assigns — including
`@AllArgsConstructor`, which the issue does not mention at all. Slab 08's six definition entities
already carry the full set and were left alone.

| Class | Annotation | Was |
|---|---|---|
| `DefinitionCompiler` | `@RequiredArgsConstructor(PACKAGE)` | 2-arg, package-private |
| `TransitionEvaluator` | `@RequiredArgsConstructor` | 1-arg, public |
| `DefinitionResolverImpl` | `@RequiredArgsConstructor(PACKAGE)` | 1-arg, package-private |
| `SetPostVisibilityAction` | `@RequiredArgsConstructor(PACKAGE)` | 1-arg, package-private |
| `CompiledGraph` | `@AllArgsConstructor(PRIVATE)` | **7-arg, private** |
| `CompiledGraph.Builder` | `@RequiredArgsConstructor(PRIVATE)` | 1-arg, private |

`CompiledGraph`'s is the one that pays: seven fields and fourteen lines of assignment gone, and
because the annotation takes fields in declaration order there is no longer a seven-argument
positional call to keep in step with the field list. `javap` confirms **all six** generated
constructors match their predecessors in signature *and* access level, which is the whole of what
had to hold — `access` is explicit on five of them because Lombok defaults to `public` and four of
these were narrower.

**`TransitionEvaluator` was a carve-out and is not one now** — this issue's carve-out 2, D11's
carve-out 3. Both, and map ticket 09 behind them, ask that having no repository be "visible in its
constructor"; with the constructor generated there is none to look at, so its javadoc now says
**"Look at what it depends on"** and points at the single `InformationRequirementSatisfaction`
field. The property is a one-line field declaration either way, and an IDE shows the generated
signature. **The *What to build* section above still asks for the hand-written constructor and is
now stale** — it is left as the historical ask rather than quietly edited, but D11's carve-out 3 is
owed a strike by whoever owns the PRD.

The graph builder is still not a Lombok `@Builder` — it derives sets, declares Events as a
side effect of declaring Transitions, and runs three validations while constructing two indexes, so
there is nothing for `@Builder` to generate. Its javadoc says what the builder does and no longer
says what it is not.

### One wording correction to the issue

The AC says the graph builder is "untouched". Taken literally that contradicts the AC above it, since
D11's enumeration — and this issue's own Notes — count the builder's three null checks among the
eighteen to convert. Read as *no `@Builder`*, which is what D11's carve-out 2 actually argues about
and the only reading under which both bullets hold, the builder's three parameter checks became
`@NonNull`. Nothing about how it derives its sets or builds its indexes changed; its own constructor
is now generated, which is a different annotation and not what that carve-out protects.

### Evidence

- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — exact, and no surefire
  report exists for `IntendedDeltasAdr0005WillInvertTest`, which is how `parity-gate.md` says to
  verify the split. (The `-output.txt` files in `surefire-reports` are stdout captures, not reports;
  counting them puts the class total at 34 and reads as a regression that is not there.)
- The same package unfiltered: **25 classes, 263 tests, 1 skipped** — the figure `parity-gate.md`
  gives for dropping both flags, which is a second independent read on parity holding.
- The six adopted classes: **100 tests**, 0 failures — 23 `CompiledGraphTest`, 25
  `TransitionEvaluatorTest`, 19 `DefinitionCompilerTest`, 21 `LifecycleStrategiesTest`, 8
  `GuardRegistryTest`, 4 `EvaluatorPurityGuardTest`.
- `fmt-maven-plugin:check` clean across **664 files**. `EvaluationOutcome.Refused`'s header needed
  the formatter's own wrapping, applied via `:format`.
- `graph`'s imports are ten `java.util.*` and `lombok.NonNull`, nothing else. The only string
  matching "Nullable" anywhere under `lifecycle/` is a pre-existing `Optional.ofNullable` call.
- The purity gate needed no weakening: it carries no `java.`/`lombok.` allowlist rule yet — that is
  slice 03's — and `lombok.NonNull` trips none of its current persistence or package-edge rules. D18
  writes the allowlist as `java.` **and** `lombok.` from the start, so slice 03 lands on this tree
  unchanged.
- Both structural gates green: `EvaluatorPurityGuardTest` 4, `DefinitionInertnessGuardTest` 6.
- `ApplicationTest` green against a Postgres testcontainer — still the only test that folds every
  strategy bean into its registry from the real container, and therefore the one that would have
  caught a blind `@RequiredArgsConstructor` on either registry. It also now covers
  `TransitionEvaluator`, `DefinitionResolverImpl` and `SetPostVisibilityAction` being constructed by
  the container through Lombok-generated constructors rather than hand-written ones.
- **Full suite: `BUILD SUCCESS`, 1592 tests, 0 failures, 0 errors, 16 skipped**, from
  `mvn clean test` read off Maven's own `Results:` line.

**The full-suite count is 1592, and slice 01's 1590 was two short of its own tip.** Slice 01 landed
two new tests last (`Permitted.actions()`'s two callers) and its recorded full-suite figure predates
them; 1590 + 2 = 1592, and this slice adds no test method. Two counting traps behind that, both
worth knowing:

- **`test-backend.sh` swallows Maven's `Results:` line**, leaving only a JVM warning in
  `target/test-run.log`, so the aggregate has to come from `mvn` directly.
- **Summing the per-class `.txt` reports undercounts** — 1588 against Maven's 1592 for the same
  tree. The `.xml` reports' `tests=` attributes sum to 1592 and agree with Maven, so the XML is the
  one to trust if the summary line is unavailable.
- **`target/surefire-reports` is never cleared**, so an aggregate over it can mix several runs and a
  previous day's build. That is how a green run was first read here as "9 errors": the arithmetic
  was over reports from three overlapping runs. `mvn clean` before a run that will be counted, and
  never two Maven runs against one `target/` at once.

### Every conversion was proven to fire, then the proof was deleted

A `@NonNull` that compiles is not a `@NonNull` that throws. A throwaway
`ScratchNonNullFiresTest` asserted `NullPointerException` on the converted seams — every component,
every factory parameter, every builder parameter — and ran **8 tests, 0 failures**. It was deleted
with its surefire report before commit. `javap -c` independently confirms the generated check sits
in the canonical constructor ahead of the record's own compact-constructor body, and `javap -p`
confirms every generated constructor's signature and access level.

Not mutation-tested beyond that: the existing 100 tests are the regression net for behaviour, and
they were green before and after.

### Review feedback, applied

The first pass over-documented and under-applied. Three things changed on review:

- **Every "deliberately not a Lombok X" comment is gone** — five of them, on both registries, the
  builder, the compiled Transition and the evaluator. A comment cannot tell a reader anything about
  a constructor that the constructor does not already show, and no other of this backend's 227
  Lombok files documents an annotation's absence. Where the carve-out is real the code shows it; the
  reasoning lives in this file.
- **`CompiledTransition.requiredAuthority` is `@NonNull` after all**, and its message went with the
  carve-out.
- **The Lombok rule was applied to constructors the issue never listed**, `@AllArgsConstructor`
  included. Three more classes and `CompiledGraph`'s own seven-argument constructor were sitting
  hand-written because the issue's text named only three classes.

Two smaller notes from the two-axis review:

- **`access = AccessLevel.PACKAGE` and `PRIVATE` are strictly redundant on the nested and
  package-private classes** — a generated `public` constructor is unreachable from outside a
  package-private class anyway, and the `webhook` precedent leaves the attribute off. Kept because
  it holds the *declared* visibility identical across each substitution, which is what makes these
  faithful refactors rather than ones that happen not to matter yet.
- **Primitive Obsession on State and Event names, pre-existing and deliberately not touched.**
  `CompiledTransition` and `Subject` now carry `@NonNull String fromState`, `event`, `toState`,
  `currentState` where the glossary has State and Event as concepts with entities in `definition`.
  `CompiledTransition`'s own javadoc already argues the case for names over ids, so this is a
  standing design decision, not a slip — but D20 has no slice for typing them and this one was not
  it. A candidate for the vocabulary slice (10) or a later simplification, alongside slice 01's
  three unactioned findings.
- The issue tracker documents resolution as an `## Answer` heading plus a pointer in a `map.md`.
  This effort's slices use `## Outcome` and have no `map.md`, following slice 01. Left consistent
  with its sibling rather than half-migrated.
