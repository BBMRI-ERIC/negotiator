# Adopt prototype B's tree, in this repository's test convention

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The whole subsystem, in one slice, by **adopting** the tree ticket 13's second prototype already
built rather than retyping it from the PRD. That tree is the starting point D1 fixes: three
packages, the Transition Evaluator, the two registries, the four ported strategies, the compiled
graph and its builder, the compiler that turns rows into a graph, both refusing placeholders, and
the two structural gates. It already passes `mvn compile`, the Google Java Style check,
`ApplicationTest` against a Postgres testcontainer, and the parity gate unchanged.

**Context pointer: `proto/evaluator-b`.** Six commits, the last of which is documentation. Their
subjects are the slice shape: `RequiredAuthority` becomes graph vocabulary; the compiled graph and
its vocabulary; the Evaluation Pipeline and the Guard catalogue; compilation in the definition
package; the Action chain and the four strategies. Graft them; do not rewrite them. The state
machine, the sealed caller type, the sealed outcome, the pipeline order and the fold shape are all
decisions the code encodes more precisely than prose does — that is why the PRD points here instead
of restating them.

Two things change on the way in, and nothing else:

- **`@DisplayName` on every new test method.** Neither prototype has one and every pre-existing
  Lifecycle test does, so the convention is restored here rather than left as a second dialect.
  86 test methods across the six classes carry none today.
- **The Well-known State constant in place of the string literal** in the parent-approval Guard.
  D14.2 requires that gate to name the Negotiation State it reads through the existing Well-known
  State holder, since that holder is what slab 07 built the names for.

Read the PRD's D2, D3, D5, D6, D13, D14, D15, D16 and D17 before starting: each one records a
decision the tree already embodies, and a reviewer's first instinct on several of them is wrong.
D14 in particular corrects four things map ticket 09's own text says about the code being ported.

## Acceptance criteria

- [x] The three packages exist with D2's dependency arrows: `definition` and `evaluation` both
      depend on `graph`; neither `graph` nor `evaluation` names `definition`; `definition` does not
      name `evaluation`. `graph` imports nothing outside the JDK.
- [x] `RequiredAuthority` lives in `graph` and is off the inertness guard's name list, with its
      pre-existing javadoc about the admin-or-creator question carried through the move intact
      (D12) — it predates the prototype and must not be lost to it.
- [x] `DefinitionScope` stays package-private in `definition`. The compiled graph carries no
      Definition Scope at all.
- [x] The compiled graph holds bound closures, not a type key plus a blob (D3). The evaluator never
      sees a type key and never sees JSON.
- [x] The evaluator's only constructor argument is the Information Requirement satisfaction port,
      and its constructor is hand-written so that a reader can see there is no repository on it.
- [x] The Evaluation Pipeline runs Required Authority, then the Information Requirement check, then
      definition-wide Guards before Transition Guards, short-circuiting at the first failure.
- [x] A permitted outcome reports its ordered Action chain and runs none of it.
- [x] `possibleEvents` and `evaluate` are one path, with a test asserting the listing offers exactly
      what the gate would permit.
- [x] The authority rule matches D5 in both halves: the caller is a sealed type, the system caller
      satisfies `SYSTEM` and nothing else, and no human satisfies `SYSTEM`. `IS_ADMIN` reads the
      granted `ROLE_ADMIN` authority, not the Person row's `admin` column.
- [x] The two registries fold their strategy beans in the constructor, refuse a duplicate type key
      from the constructor with both colliding class names in the message, and keep Guards and
      Actions in separate namespaces.
- [x] Both unbuilt seams have production beans that **throw** — Information Requirement
      satisfaction and the post-visibility write — and `SPAWN_RESOURCE_LIFECYCLES` is registered but
      refuses to run.
- [x] Every new test method carries a `@DisplayName` that reads as a sentence.
- [x] The parent-approval Guard names its Negotiation State through the Well-known State holder,
      not as a string literal, and does no lookup of its own.
- [x] `ApplicationTest` is green, which is what proves the real container folds every strategy bean
      into its registry without a key collision.
- [x] The formatter check passes, and the parity half of
      [parity-gate.md](../../state-machine-implementation/parity-gate.md) is unchanged at
      **255 tests in 24 classes**.
- [x] `git diff` shows no change to any pre-existing production class beyond the `RequiredAuthority`
      move and the two amended gate tests.

## Notes

**Do not lift prototype B's divergence table.** Its Part 7 was written from prototype A's two
planning documents rather than from A's Java, and three of its rows do not survive a reading of A's
tree. Ticket 13's Answer records all three with citations.

**Prototype A is not merged and must not be cherry-picked.** Seven of its ideas arrive as later
slices in this slab; the rest are recorded as declined, with reasons, in
[taking A's good ideas into B](../../state-machine-implementation/notes/evaluator-a-into-b.md).

**Nothing in production calls any of this when the slice lands** (D19). The compiler and the cache
stay package-private, no repository gains a load-the-whole-version query, and both Lifecycle
services still run on Spring Statemachine. A parity movement in this slab is therefore a defect in
the slab, never a delta to accept.

## Blocked by

None - can start immediately.

## Outcome

**Landed on `feat/state-machine-implementation`** in eight commits. Five are `proto/evaluator-b`'s,
cherry-picked unchanged; three are this slice's.

| Commit | What |
|---|---|
| `df270bdc` | `RequiredAuthority` becomes graph vocabulary, and the gate says why |
| `98124e3a` | The compiled graph, and the vocabulary it is expressed in |
| `6c0bcc32` | The Evaluation Pipeline and the Guard catalogue |
| `df3ec338` | Compilation happens in the definition package |
| `c4bba24d` | The Action chain, and the four ported strategies |
| `7d3ea721` | A `@DisplayName` sentence on every new test method — the one mandated change |
| `66022f6a` | The cross-catalogue test reaches the claim its name makes — *problem 1* |
| `5b47127b` | A permitted outcome reports its Actions and runs none of them — *problem 2* |

The graft is verbatim. `git diff proto/evaluator-b HEAD -- backend/` after `7d3ea721` was exactly the
100 lines of the `@DisplayName` change and nothing else. B's sixth commit is its findings document
and stayed on its branch, where ticket 13's Answer already cites it.

### Evidence

- The six adopted classes: **98 tests**, 0 failures. Now **100**, with problem 2's two.
- **Parity gate: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped.** Exact, and no surefire
  report exists for `IntendedDeltasAdr0005WillInvertTest`, which is how `parity-gate.md` says to
  verify the split rather than by a pass count. The unfiltered form of the same package carries
  **25 classes, 263 tests**, the number the gate gives for dropping both flags.
- `ApplicationTest` green against a Postgres testcontainer — the real container folds every strategy
  bean into its registry with no key collision, which is the only test that exercises D15's
  boot-failure rule from the container.
- `fmt-maven-plugin:check` clean across 664 files.
- **Full suite: 1590 tests, 0 failures, 0 errors, 16 skipped.**
- Both later commits are test-only and touched no file under `src/main`, so the parity and
  `ApplicationTest` results above still hold for the tip.

### Only one of the two mandated changes was real

**The `@DisplayName` change was.** 86 methods across the six classes: 23 in `CompiledGraphTest`, 20
in `TransitionEvaluatorTest`, 19 in `DefinitionCompilerTest`, 12 in `LifecycleStrategiesTest`, 8 in
`GuardRegistryTest`, 4 in `EvaluatorPurityGuardTest` — exactly the count this issue predicted. The
annotation goes last in the block, below any `@CsvSource` or `@EnumSource` and above the signature,
which is where the other 213 in this backend sit.

**The Well-known State change was not.** This issue says the parent-approval Guard holds a string
literal and D14.2 requires the holder instead. `NegotiationApprovedGuard` already reads
`WellKnownNegotiationStates.IN_PROGRESS`, in both the pass branch and the refusal details, at
`proto/evaluator-b`'s tip. The criterion is satisfied and no work was needed. The premise was wrong,
not the code — worth knowing because this issue presents it as half the slice's content.

One further deviation from "carried through intact", noted rather than reverted: `RequiredAuthority`'s
javadoc reads "not this **vocabulary's** call" where it read "not this **schema's** call". D12's
substance — the eighteen lines on why six of the eight Negotiation Transitions are behaviourally
administrator-or-creator, and why inventing a disjunction is not this type's decision — is intact.
The one word follows the type out of the schema package.

### Two problems found, and fixed past this issue's scope

This issue says two things change on the way in **and nothing else**. Both problems below are in the
adopted tree, so fixing either breaks that instruction. They were reported first and fixed on
instruction, each in its own commit, and neither touches production code.

**Problem 1 — a test that could not fail for its stated reason.** `DefinitionCompilerTest`'s
`compile_whenAnActionKeyIsWiredAsAGuard_isRefused` had a body identical to
`compile_whenTheCatalogueRefusesAKey_failsTheCompileRatherThanTheFiring`: both wired the key
`NO_SUCH`. So it proved the unknown-key rule twice and never wired an Action key anywhere, leaving
this PRD's seam B item *cross-catalogue misuse refused* with no executable form. It now wires
`SPAWN_RESOURCE_LIFECYCLES` — a real Action's key — as a definition-wide Guard, and the fake Guard
catalogue refuses it for the reason the real registry would: no Guard strategy declares it.

One fixture moved with it. `compile_handsEachWiringsRawParamsToTheCatalogueOnce` wired
`SET_POST_VISIBILITY`, an Action, as a Guard and expected it to bind — the same confusion the test
above now forbids, so the two could not both stand. It carries a Guard's key and Guard-shaped params
instead. Slab 08 settled this exact point on its own wiring tests: *"a Guard fixture used an Action's
type key ... the glossary keeps Guard and Action strictly distinct."*

**Problem 2 — an acceptance criterion of this issue with no test behind it.** *"A permitted outcome
reports its ordered Action chain and runs none of it"* was true of the production code and unproven
by any test: `Permitted.actions()` had no caller in the test tree, and `TransitionEvaluatorTest`'s
fixture graph put no Action on any Transition. Two tests now cover it, one per caller that must not
commit anything — `evaluate` and `possibleEvents`. The listing is the one with teeth, because it
evaluates every candidate Event, so an evaluator that ran Actions would multiply an effect by the
size of the fan it offers.

### Each new test ran red before it ran green

| Mutation | What went red |
|---|---|
| `ACTION_ONLY_KEY` dropped from the fake Guard catalogue's undeclared set | `compile_whenAnActionKeyIsWiredAsAGuard_isRefused`, alone — which is what proves it is no longer the other test's duplicate |
| The evaluator runs the Action chain before returning `Permitted` | `evaluate_whenPermitted_reportsTheOrderedActionChainAndRunsNoneOfIt` and `possibleEvents_runsNoAction`, and nothing else |
| The reported Action chain is reversed | `evaluate_whenPermitted_...` alone, so the chain's order is load-bearing rather than incidental |

The 86 display names were not mutation-tested. A display name asserts nothing; what it changes is
what a failure reads as.

### One trap for whoever runs the whole suite here

The first full-suite run failed with six errors in `StrategyRegistryTest`,
`DefinitionGraphCompilerTest` and `CompiledGraphCacheTest` — `NoSuchMethod`, `ClassCast`,
`NoClassDefFound`. No source file of any of those names exists on this branch. They are prototype
**A**'s classes, left as stale `.class` files in `backend/target/test-classes` by an earlier build of
`proto/evaluator-a`, and surefire runs whatever it finds there. `mvn clean test` gives 1590 tests
green. Worth knowing for any later slice of this slab, since A's tree is never merged and this will
recur on any machine that has built it.

### Left for later slices, deliberately

D20's slices 3–9 are untouched: no purity-gate allowlist rule and no `.formatted` fix, no new
inertness names, no topology fixtures, no named graph exception, no unknown-State ordering fix, no
strict Wiring configuration, no cache. D19 holds — the compiler and its input record are
package-private, no repository gained a load-the-whole-version query, nothing in production calls any
of this, and both Lifecycle services still run on Spring Statemachine.

Three findings from the standards review are recorded here rather than acted on, all in grafted code
and all candidates for slice 02's Lombok pass or a later simplification: `GuardRegistry` and
`ActionRegistry` are roughly eighty near-identical lines; `DefinitionVersionRows.withoutActions(...)`
has no caller in main or test; and `GuardRegistry`'s javadoc still calls itself "the subsystem's
**one** unchecked narrowing, in one place" now that there are two of them.
