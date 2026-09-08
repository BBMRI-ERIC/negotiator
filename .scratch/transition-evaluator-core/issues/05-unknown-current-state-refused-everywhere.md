# An unknown current State refused in every lookup, and the evaluator's ordering

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

A Lifecycle whose current State its own pinned Definition Version does not declare is **graph
corruption**, not an ordinary unavailable move. Both prototypes' plans say so and only one's code
does it; the adopted tree refuses in one of the graph's three State-taking lookups and answers the
other two as though the State were simply a State with nothing leaving it. This slice makes the
graph refuse in **all** of them (D7).

The two that answer today are the ones a caller actually reaches: asking for the Transition on a
State-and-Event pair returns empty, and asking for the Transitions leaving a State returns an empty
list. Both are indistinguishable from a legitimately terminal State, which means a broken
Definition Version Pin surfaces to a requester as "nothing is available" — the one thing user story
64 says an empty Possible Events listing must never be able to mean.

**And the evaluator asks its questions in the wrong order.** It currently checks whether the Event
is declared before it touches the State at all, so a corrupt pin on a Lifecycle is reported as an
unknown Event — a message about the caller's input, for a fault in the data. Ask the State question
first. This is also what finally gives the graph's "does this version declare this State" predicate
a production caller, rather than leaving it a method only tests use.

What must **not** change is the distinction the tree already draws between a version that does not
declare an Event at all and one that declares it but offers no Transition from here. A typo and a
deliberately transition-less Event — the Override Event, which exists as a name under which a direct
state change appears in history — are different things and refuse differently (user story 63).

## Acceptance criteria

- [x] Every graph lookup that takes a State refuses an undeclared one with the named exception from
      [04](04-one-named-exception-for-an-impossible-graph.md): the terminality question, the
      Transition-for-a-State-and-Event lookup, and the Transitions-leaving-a-State lookup. All three
      through one guard, and the public surface was enumerated to confirm there is no fourth.
- [x] The refusal names the State and the Definition Version, so a log line identifies the broken
      pin without a debugger. It also offers the States the version does declare, which is
      `isTerminal`'s pre-existing message, moved rather than rewritten.
- [x] The evaluator asks whether the current State is declared **before** it asks whether the Event
      is declared, and a test proves a corrupt pin is never reported as an unknown Event. That test
      is the one refusal the graph's guard does **not** make green on its own — see Verification.
- [x] A Possible Events listing over a graph that does not declare the current State **throws**
      rather than returning an empty set.
- [x] An empty Possible Events listing still happens, and still means "nothing is available" — a
      terminal State returns empty and does not throw. Rests on the pre-existing
      `possibleEvents_whenTheStateIsTerminal_isEmpty`, unmodified and still green.
- [x] An Event the version does not declare and an Event declared with no Transition from the
      current State still refuse distinguishably, each with its own reason code. Also unmodified:
      the `OVERRIDE` and `TYPO` pair still assert `NO_TRANSITION_FOR_EVENT` and `UNKNOWN_EVENT`.
- [~] The graph's declares-this-State predicate has a production caller. **On the letter, not on the
      narrative** — the caller is the graph's own guard rather than the evaluator. Argued in Outcome.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**. Run, and exact — but the first attempt was red for a
      reason worth reading, recorded in Verification.

## Notes

**This hands the cutover slab one short rule**, and stating it here is the point of the slice: a
corrupt pin is a 500 and a log line, never a 403, 422 or 409. A listing that throws and a listing
that silently returns an empty set are different failure modes for the same broken row, and only the
first is honest.

Run each refusal red before trusting it green. Two of the three lookups return an empty answer
today, so a test that asserts a throw and gets an empty collection is exactly the vacuity this
effort has already been bitten by twice.

## Blocked by

- [04 — One named exception for a graph that cannot exist](04-one-named-exception-for-an-impossible-graph.md)

---

## Outcome

`CompiledGraph.requireDeclaredState(String)`, public, five functional lines. It holds the refusal
`isTerminal` already made — same message, byte for byte — and all three State-taking lookups now ask
it first. The production diff is those five lines and nothing else; the rest is javadoc.

The evaluator's `evaluate` asks it before `declaresEvent`, which is the one-line fix for the ordering
bug, and `possibleEvents` gets the refusal through `transitionsFrom`.

### Why the evaluator asks through the guard rather than asking the predicate

Both review axes landed on the same partial: `declaresState`'s only production caller is
`requireDeclaredState`, inside `CompiledGraph` itself. The sixth acceptance criterion is met on the
letter — it is no longer a method only tests use — but the issue's narrative wanted the evaluator to
be that caller.

**Deliberately not done, for one reason:** the alternative is
`if (!graph.declaresState(s)) { throw new InvalidGraphException(...) }` in the evaluator, which puts
the formatting of a graph-corruption message in the evaluation package and makes it the second site
that has to name the State, the Definition Version and the declared States. The two would drift, and
the message is the thing acceptance criterion two is about. One formatting site, in the package that
owns the graph vocabulary, was worth a weaker reading of criterion seven.

A later slice that wants the predicate called from outside its own class should argue with this
paragraph rather than assume it was an oversight.

### Two things worth knowing about the shape

**The evaluator asks the State question twice** — its own `requireDeclaredState` call, then again
inside `graph.transition`. Not redundant where it counts: delete the explicit call and
`evaluate(graph, "TYPO", corruptPin)` goes back to answering `UNKNOWN_EVENT`, because `declaresEvent`
would run first. It is asymmetric with `possibleEvents`, which needs no explicit call because nothing
precedes its lookup. A test holds the difference, so a later simplification that removes the line
turns red rather than quietly reverting the slice.

**`isTerminal` already had exactly one production caller, and it is the terminal-aggregation path.**
`EvaluationContext.SiblingResource.isFinished()` calls it, so that path now reaches the shared guard
too. Nothing about its behaviour changes — the message and the condition are the same — but the
refusal a Feedback Guard can raise and the refusal a Possible Events listing can raise are now
provably the same one.

### Review feedback, applied

The standards axis found no hard breach of a documented standard. Five judgement calls, all taken:

- **The same argument was restated in six javadocs.** It now lives once, on `requireDeclaredState`.
  The two method-specific halves stayed where they are actually specific: `transition`'s "empty never
  means the State was unknown" and `transitionsFrom`'s "which is why an undeclared State must not
  also answer empty".
- **The three graph refusal tests were byte-identical but for the lookup invoked.** They are now one
  `@ParameterizedTest` over the three lookups, which is also the shape of the first acceptance
  criterion. This replaced a pre-existing test name — `isTerminal_whenTheVersionDoesNotDeclareTheState_throws`
  — with three named invocations of `everyLookupTakingAState_whenTheVersionDoesNotDeclareIt_throws`;
  no assertion was lost and each invocation asserts one more value than that test did.
- **The ordering test asserted less than its two siblings.** No version id and no declared-State
  list. That is exactly the unevenness [04](04-one-named-exception-for-an-impossible-graph.md) fixed
  across a test pair, under the rule "strip explanatory prose, keep every value the message carries",
  so the three are level again. It keeps its `hasMessageNotContaining("TYPO")`, which is the half
  that makes it about ordering.
- **The corruption fixture was `contextFor` with one string changed**, and its `Caller` parameter
  never varied. `contextFor` now delegates to `contextIn(currentState, caller)`.
- **`"Definition Version 1"` was hardcoded** where the sibling suite used a constant. Both suites now
  name the version and the undeclared State.

**Not applied:** the fully-qualified `{@link eu.bbmri_eric...InvalidGraphException}` links in
`TransitionEvaluator`, which read oddly next to that file's simple-name links. An import used only by
javadoc **is stripped by `fmt-maven-plugin`** — checked by adding one and running the formatter, not
assumed — so the long form is forced there. Trimming the prose took it from four mentions to two,
both of them `@throws`.

The spec axis found no wrong implementation and no scope creep beyond `requireDeclaredState` itself
being public API this issue did not name.

### Verification

Six new tests: three parameterized invocations in `CompiledGraphTest` (25 tests in the class) and
three methods in `TransitionEvaluatorTest` (28). No test deleted; one pre-existing test consolidated
as described above.

**Each refusal was run red before it was trusted green, and then again after the rewrite.** This
matters more than usual here, because **two of the three evaluator tests pass off the graph's guard
alone** — the graph refusing is what makes them green, not anything in the evaluator. Neutering the
guard to `if (false)` was the check: all six fail, and nothing else in either class does. The one that
is genuinely about the evaluator is
`evaluate_whenTheCurrentStateIsUndeclaredAndTheEventUnknown_blamesTheStateAndNotTheEvent`, which stays
red with the guard intact and the evaluator's own line removed.

216 tests green across `lifecycle/**` in 19 classes, including the four structural gates, which are
the ones that would notice a new public method in these packages.

The parity half is **unchanged and exact: 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped.**
The skip is `dump.LifecycleGraphDumpGeneratorTest` and no report exists for
`IntendedDeltasAdr0005WillInvertTest`, which is how the gate says to verify the split.

**It took two attempts, and the first was not a parity movement.** Attempt 1 reported 199 errors and
**0 failures**, every error an `ApplicationContext failure threshold (1) exceeded` at context load,
with discovery already correct at 24 classes and 255 tests. The cause was a second Maven build
running concurrently in the same worktree — a review sub-agent recompiling `backend/target` to run
its own vacuity check while the gate was mid-flight. Same torn-target family as
[01](01-adopt-prototype-b-tree.md)'s stale-`.class` trap and 04's attempt 2, different trigger.

Two things to carry forward: **read a red gate for its failure shape before believing it** — 0 failed
assertions with a context-load cascade is an environment story, not a behaviour story — and **nothing
else may touch `backend/target` while the gate runs**, sub-agents included.
