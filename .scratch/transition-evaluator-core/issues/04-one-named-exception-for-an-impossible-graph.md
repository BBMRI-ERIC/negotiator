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
- [x] The verdict type's malformed-verdict `IllegalArgumentException` is unchanged and carries a
      comment recording that the exclusion is deliberate.
- [~] `ApplicationTest` is green. The parity half of
      [parity-gate.md](../../state-machine-implementation/parity-gate.md) was not run — focused
      tests only, at the requester's instruction — and is unchanged by inspection rather than by
      execution. See Outcome.

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
- **`GuardVerdict` stated its exclusion rationale twice**, once in a new `@throws` and once in the
  inline comment. The comment is the record the issue asked for and sits where a sweeper's grep
  lands; the javadoc is now a bare `@throws`.
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

The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) was **not**
run — 8.5 minutes, and the requester asked for focused tests only. Verified structurally instead: no
file under `characterization/` references `lifecycle.graph`, `lifecycle.evaluation`,
`InvalidGraphException`, `CompiledGraph`, `GuardRegistry` or `ActionRegistry`, so that half cannot
see this diff. The claim is "unchanged at 255 in 24", and it is a no-change claim about a suite this
diff is invisible to — but it is unrun, and that is worth knowing.
