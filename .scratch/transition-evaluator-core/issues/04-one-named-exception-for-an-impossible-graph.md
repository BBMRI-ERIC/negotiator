# One named exception for a graph that cannot exist

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Every rejection of a graph or of a compile raises **one named exception type** instead of the mix of
`IllegalStateException` and `IllegalArgumentException` the adopted tree throws today. It extends
`IllegalStateException`, because each of these is a broken-invariant statement about data the caller
did not choose (D8).

The tree currently spreads thirteen such rejections across four classes: the graph builder's three
validations and its unknown-State refusal, the compiler's version-mixing refusal, and four apiece in
the two registries — unknown key, params that do not fit, and the duplicate-key collision thrown
from the constructor. They are one thing wearing two type names, and the type names carry no
information: nothing distinguishes the argument errors from the state errors except which class the
author happened to be in.

Two properties this buys, and both are the point:

- The cutover slab can catch graph corruption **and nothing else**. It has one short rule to write —
  a corrupt Definition Version Pin is a 500 and a log line, never a 403, 422 or 409 — and a catch
  clause naming `IllegalStateException` would swallow far more than that.
- A test asserts a **class** rather than a message substring, which is what stops the assertions
  from pinning prose.

The seam javadoc goes with it. `GuardCatalogue.bind` currently documents
`@throws IllegalArgumentException`; the PRD's D4 already writes that javadoc against the new name,
so the interface contract and the implementation must end this slice naming the same type.

**Deliberately not folded in:** the verdict type's own refusal of a malformed verdict. That is a
programming error in a strategy — a Guard returning a refusal with no reason code — and genuinely an
`IllegalArgumentException`. Leave it, and leave a comment saying it was left on purpose, or the next
sweep will collect it.

## Acceptance criteria

- [x] One named exception type exists, extending `IllegalStateException`, with javadoc saying what
      it means and why it is not two types.
- [x] Every graph-builder validation failure, the unknown-State refusal, the compiler's
      version-mixing refusal, and every registry refusal raise it.
- [x] The duplicate-key collision still propagates out of the registry's **constructor**, so a
      duplicate type key remains a bean creation failure and therefore a failed boot. Wrapping it in
      the new type must not change that.
- [x] `GuardCatalogue`'s and `ActionCatalogue`'s `@throws` javadoc names the new type, matching what
      the implementations now raise.
- [x] Every test that asserted one of these failures asserts the class, not a message substring.
      Messages are still asserted where the *content* is the requirement — the colliding class
      names, the known keys in an unknown-key message.
- [~] The verdict type's malformed-verdict `IllegalArgumentException` is unchanged. It carries **no
      comment** recording that the exclusion is deliberate: dropped on the requester's
      instruction, so this file is the only record. See Outcome.
- [x] `ApplicationTest` is green and the parity half of
      [parity-gate.md](../../state-machine-implementation/parity-gate.md) is unchanged at
      **255 tests in 24 classes**. Run, not inferred — it needed a clean target to run at all, and
      the two attempts before that are recorded in the Outcome.

## Notes

**This names what a *graph* raises, and only that.** What a *refused move* becomes at the REST
boundary is still open, and it is the cutover slab's question — today the Resource Lifecycle service
throws Spring Statemachine's own `StateMachineException`, a type this effort deletes, so that slab
needs a replacement. It answers from the sealed outcome type, not from an exception. Do not invent a
refusal exception here.

Three later slices assert refusals that must be written against this type once rather than retyped:
[05](05-unknown-current-state-refused-everywhere.md),
[06](06-strict-wiring-configuration.md) and
[07](07-compiled-graph-cache.md) are all blocked by this one for that reason.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)

---

## Outcome

`eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException`, public, extending
`IllegalStateException`. The name is the one D4's javadoc snippet already wrote, so the interface
contract and the implementation ended the slice naming the same type without anyone having to
choose.

All thirteen rejections converted, and a grep of `lifecycle/` main now finds exactly one
`IllegalArgumentException` left — `GuardVerdict.fail`, the deliberate exclusion. Nothing was
over-converted: `DefinitionResolutionException` is resolution rather than compilation and was not
in scope.

### Two things the issue did not anticipate

**Five of the thirteen sites silently changed their HTTP status, in the direction D8 wants.**
`NegotiatorExceptionHandler` already maps `IllegalArgumentException` to 400 and
`IllegalStateException` to 500. The five sites that were `IllegalArgumentException` — the compiler's
version-mixing refusal, `isTerminal`'s unknown State, and each registry's unknown key, unreadable
params and missing params — would have answered 400 and now answer 500. That is exactly the "a
corrupt pin is a 500 and a log line, never a 403, 422 or 409" that D8 argues for, and it arrives for
free rather than waiting for the cutover slab to write the rule. Nothing observable changes today,
because nothing in production calls this slab. The commit is labelled `refactor:` even so, which is
honest about the diff and slightly generous about the effect; worth a release-note line at cutover.

The cutover slab still needs its own catch clause, for the log line and to stop a corrupt graph
being reported as an anonymous Internal Server Error. But the fallback under it is already right.

**"Assert the class, not a message substring" has a floor, and the first pass went through it.**
With thirteen rejections wearing one type name, the message is the *only* thing that tells two of
them apart — so stripping it stops being a fix for pinned prose and starts being a weakened test.
Three assertions went too far and came back:

- `GuardRegistry`'s missing-params and unreadable-params tests were left asserting only the type and
  the key. Swapping the two throw sites left both green — confirmed by actually swapping them, and
  confirmed red again once `carries none` and `could not read its params` were restored.
- `build_whenNoStateIsInitial_isRefused` was left asserting nothing but the class, so any other
  broken invariant in the builder would have satisfied it. `found 0` is the count under test.
- The `ActionRegistry` collision test asserted only the key, never the colliding class name its
  `GuardRegistry` twin has always asserted. Pre-existing, but the acceptance criterion names
  "the colliding class names" as content, so it was fixed while the line was open.

The rule that survives: **strip explanatory prose, keep every value the message carries** — State and
Event names, type keys, colliding class names, counts, the known-key set. That is what the criterion
means and it is a sharper line than "class, not substring".

### Two properties of the type that had no test at all

Both are load bearing and neither is visible from any test that asserts
`isInstanceOf(InvalidGraphException.class)`, which is every other test of a rejection. They now live
in `InvalidGraphExceptionTest`:

- **That it extends `IllegalStateException`.** This is the decision, not an implementation detail —
  it is what buys the 500. Re-parenting it to `RuntimeException` would have compiled and passed
  every other test in the subsystem.
- **That the wrapping constructor keeps its cause.** The registries wrap Jackson's
  `JsonProcessingException`; the message names the key, the type and the blob, but which field and
  which character are only in the cause.

### Review feedback, applied

- **The class javadoc's opening sentence was untrue of two of its own sites.** "A Definition Version
  describes a graph that cannot exist" does not describe a duplicate type key, which is thrown from
  a catalogue constructor at boot with no Definition Version in play. The opening now leads with the
  compiled graph rather than the Definition Version, and a paragraph names the two odd sites and
  says why they are not split off.
- **`GuardVerdict.fail` carries no record of the exclusion at all, on the requester's instruction.**
  It first stated the rationale twice — a new `@throws` and a five-line inline comment. The comment
  was dropped, the rationale briefly moved into the javadoc, and then that was dropped too. What
  the method has now is one line saying what it does and a bare `@throws`.

  **This is a deliberate deviation from the sixth acceptance criterion**, which asks that the
  method "carries a comment recording that the exclusion is deliberate". It does not. **This file
  is now the only record**, which is the same place [02](02-lombok-where-it-fits.md) put its five
  deleted comments — "the reasoning lives in this file" — so the two slices agree after all, and
  the conflict noted below is settled in 02's favour.

  The risk the criterion was guarding against is real and now unmitigated in the code: a later
  sweep that greps for `IllegalArgumentException` in `lifecycle/` finds exactly one hit, with
  nothing next to it saying it was already considered. Whoever runs that sweep should read this
  entry before converting it. The reason it must not convert: a Guard returning a refusal with no
  reason code is a programming error in a strategy, and the graph it was asked about may be
  perfectly well formed.
- **The count came back to the twin test too.** `build_whenNoStateIsInitial` asserted `found 0`
  while `build_whenTwoStatesAreInitial` asserted only the two State names, so the pair applied the
  slice's own rule unevenly. It now asserts `found 2` as well.
- **And the declared-State list came back to `isTerminal`'s refusal.** That message is
  `declares no State named '%s'. Its States are %s`, and the test asserted only the unknown name.
  The second half is the same "known keys" content the criterion protects and that the registry's
  unknown-key twin asserts as `PARAMS_FREE` — the offer of what the graph *does* declare is what
  makes the refusal actionable. It now asserts a declared State as well.
- **This slice's comment rule and [02](02-lombok-where-it-fits.md)'s pointed in opposite
  directions, and 02 won.** Slice 02 deleted five "deliberately not a Lombok X" comments on the
  grounds that "a comment cannot tell a reader anything about a constructor that the constructor
  does not already show", and put the reasoning in its own issue file instead. This slice's sixth
  acceptance criterion *required* such a comment on `GuardVerdict.fail`.

  The case for keeping it was that the two rules address different things — 02 forbids documenting
  what the code already shows, and no code can show that a sweep looked at a throw site and chose
  to leave it. The requester overruled that, twice, and the comment is gone. So the standing
  convention for this effort is now uniform: **a decision not to do something is recorded in the
  slice's issue file, never in a comment beside the code.** A later slice that wants the opposite
  should argue with this line rather than re-add a comment.
- **Suppressed:** the four repeated throw shapes across the two registries read as Duplicated Code,
  but ADR 0002 and `ActionRegistry`'s own javadoc endorse the two-registry duplication as
  deliberate. Repo overrides the baseline.
- **Not done:** the class is neither `final` nor carries a `serialVersionUID`. `DefinitionResolutionException`
  is neither, and no exception in this backend declares one; left consistent rather than singular.

### Verification

`InvalidGraphExceptionTest` is new (2 tests). No test was deleted, and no assertion was removed
without a stronger one taking its place.

Focused runs only, at the requester's instruction. 131 tests green across `lifecycle/**` — the two
registries, the compiler, the compiled graph, the evaluator, the strategies, the new exception — plus
the four architecture guard tests, which are the ones that would notice a new class in these
packages. `ApplicationTest` green in 52s against a real context boot, which is the live exercise of
the acceptance criterion that a duplicate type key still fails the boot: both registries are
constructed from the real strategy beans on that path.

The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) **was** run,
and is unchanged at **24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — exact. The one skip
is `dump.LifecycleGraphDumpGeneratorTest`, and no surefire report exists for
`IntendedDeltasAdr0005WillInvertTest`, which is how the gate says to verify the split rather than by
a pass count.

It took three attempts, and the first two are worth recording because neither was a parity movement:

| Attempt | What happened |
|---|---|
| 1 | Died in JUnit **discovery** — `NoClassDefFoundError` on `ResourceGraphV1$Edge`. Zero tests ran. |
| 2 | Discovered correctly (24 classes, 255 tests, 1 skipped) but **133 errors**, every one a `PersonRepository` bean-resolution failure at context load. 0 failures. |
| 3 | After `mvn clean`: green and exact. |

Both were torn `backend/target` state from incremental builds across differently-filtered runs — the
same family as the stale-`.class` trap [01](01-adopt-prototype-b-tree.md) documented, and the same
remedy. Neither produced a single failed *assertion*. Worth knowing for later slices: on this machine
the parity gate wants a clean target, and a red gate here should be read for its failure *shape*
before it is believed.
