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

- [x] Every hand-written null check on the graph's records is a Lombok `@NonNull`, and the
      behaviour a caller sees on a null argument is unchanged. **20 of 21**; the twenty-first is a
      fourth carve-out, below.
- [~] The two registries and the compiler use `@RequiredArgsConstructor`. **The compiler does. The
      two registries cannot** — a fifth carve-out, below.
- [x] The graph builder and the evaluator constructor are untouched, and each carries a short
      comment saying why Lombok is deliberately absent there. Read as *no `@Builder` and no
      `@RequiredArgsConstructor`*, since D11's own enumeration counts the builder's three null
      checks among the eighteen to convert.
- [x] No `@Nullable` annotation is introduced anywhere in `graph`, and `graph` still imports nothing
      but `java.` and `lombok.`.
- [x] The formatter check passes and no test assertion changes. **No test file is touched at all.**
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

| Where | Checks | Converted | Left hand-written |
|---|---|---|---|
| `EvaluationContext` + nested | 8 | 8 | — |
| `CompiledTransition` | 4 | 3 | 1 — carve-out 4 |
| `CompiledGraph.Builder` | 3 | 3 | — |
| `ActionContext` | 2 | 2 | — |
| `GuardVerdict` | 1 | 1 | — |
| `EvaluationOutcome` | 3 | 3 | — |
| **Total** | **21** | **20** | **1** |

**Six of the twenty are `@NonNull` on a record component; two are `@NonNull` on a factory-method
parameter**, and that distinction is not cosmetic. `EvaluationContext.parentNegotiationState` and
`Subject.resourceId` are legitimately null on the record — `forNegotiation` and `Subject.negotiation`
both pass null deliberately — while `forResource` and `Subject.resource` require them. Annotating
those components would have broken the other factory; the check belongs on the parameter, where it
already was. The remaining three are `@NonNull` on the builder's method parameters.

### Two carve-outs beyond the issue's three

**Carve-out 4 — `CompiledTransition.requiredAuthority` keeps its hand-written check.**
`CompiledGraphTest` asserts `hasMessageContaining("RequiredAuthority.NONE")` under the display name
*"an edge with no Required Authority is refused, because NONE is how anyone is spelled"*. That
message does not report the mistake, it names the fix, and Lombok's `"requiredAuthority is marked
non-null but is null"` loses it. Converting it would have meant deleting a test assertion, which
this issue forbids. This is the issue's own Notes rule — *leave it and say so* — applied to the
compiled Transition rather than to the outcome type where it was expected to bite.

**Carve-out 5 — the two registries cannot take `@RequiredArgsConstructor`.** Their constructors do
not assign their second field, they *derive* it: `(ObjectMapper, List<Guard<?>>)` folds into
`Map<String, Guard<?>>`. The generated constructor would be `(ObjectMapper, Map<String, Guard<?>>)`,
and three things follow:

1. `GuardRegistryTest` and `LifecycleStrategiesTest` construct both registries with a `List` and
   would stop compiling — a test change, forbidden here.
2. The constructor-time duplicate-key refusal disappears, and that is slice 01's own acceptance
   criterion and D15's boot-failure rule.
3. Worst and quietest: **Spring satisfies a `Map<String, T>` parameter by injecting bean names as
   keys**, so the container would boot happily with a catalogue keyed on `negotiationApprovedGuard`
   instead of on `PARENT_NEGOTIATION_APPROVED`, and every `bind` would miss.

This is D11 carve-out 1's argument verbatim — *a generated member would take from the caller the
work the hand-written one exists to do* — arriving at a class D11 did not expect it to. Both
registries carry the reason in javadoc; `ActionRegistry`'s points at `GuardRegistry`'s rather than
repeating eighty words.

`DefinitionCompiler` took the annotation cleanly, as **`@RequiredArgsConstructor(access =
AccessLevel.PACKAGE)`** — the default is `public` and D19 plus the inertness guard require that
constructor to stay package-private. `javap` confirms the generated constructor has no `public`
modifier. It is not a Spring bean and only its test constructs it.

### One wording correction to the issue

The AC says the graph builder is "untouched". Taken literally that contradicts the AC above it, since
D11's enumeration — and this issue's own Notes — count the builder's three null checks among the
eighteen to convert. Read as *no `@Builder`*, which is what D11's carve-out 2 actually argues about
and the only reading under which both bullets hold, the builder's three parameter checks became
`@NonNull` and the class kept its hand-written shape plus a javadoc paragraph saying why no
`@Builder`. Nothing about how it derives its sets or builds its indexes changed.

### Evidence

- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — exact, and no surefire
  report exists for `IntendedDeltasAdr0005WillInvertTest`, which is how `parity-gate.md` says to
  verify the split. (The `-output.txt` files in `surefire-reports` are stdout captures, not reports;
  counting them puts the class total at 34 and reads as a regression that is not there.)
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
  caught carve-out 5 had the annotation been applied blind.
- **Full suite: 166 classes, 0 failures, 0 errors, 16 skipped.**

**On the full-suite test count, which does not match slice 01's 1590.** Summing the per-class
surefire `.txt` summaries gives **1588**; summing the `tests=` attribute of the same run's `.xml`
reports gives **1592**. The two disagree by four *within one run*, so neither is comparable to slice
01's figure, which came from Maven's own aggregate line — and `test-backend.sh` swallows that line,
leaving only a JVM warning in `target/test-run.log`. What settles it instead of a number:
`git diff --name-only <base> -- '*/src/test/*'` is **empty**, so this slice cannot have changed a
test count, and failures and errors are zero by both measures. Worth knowing for any later slice
that tries to reproduce 1590 through this script.

### Every conversion was proven to fire, then the proof was deleted

A `@NonNull` that compiles is not a `@NonNull` that throws. A throwaway
`ScratchNonNullFiresTest` asserted `NullPointerException` on all **20** converted seams — every
component, every factory parameter, every builder parameter — and ran **8 tests, 0 failures**. It
was deleted with its surefire report before commit, because the slice may not add test assertions
either. `javap -c` independently confirms the generated check sits in the canonical constructor
ahead of the record's own compact-constructor body.

Not mutation-tested beyond that: the existing 100 tests are the regression net for behaviour, and
they were green before and after.
