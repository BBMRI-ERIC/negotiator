# One named exception for a graph that cannot exist

Status: ready-for-agent

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

- [ ] One named exception type exists, extending `IllegalStateException`, with javadoc saying what
      it means and why it is not two types.
- [ ] Every graph-builder validation failure, the unknown-State refusal, the compiler's
      version-mixing refusal, and every registry refusal raise it.
- [ ] The duplicate-key collision still propagates out of the registry's **constructor**, so a
      duplicate type key remains a bean creation failure and therefore a failed boot. Wrapping it in
      the new type must not change that.
- [ ] `GuardCatalogue`'s and `ActionCatalogue`'s `@throws` javadoc names the new type, matching what
      the implementations now raise.
- [ ] Every test that asserted one of these failures asserts the class, not a message substring.
      Messages are still asserted where the *content* is the requirement — the colliding class
      names, the known keys in an unknown-key message.
- [ ] The verdict type's malformed-verdict `IllegalArgumentException` is unchanged and carries a
      comment recording that the exclusion is deliberate.
- [ ] `ApplicationTest` is green and the parity half of
      [parity-gate.md](../../state-machine-implementation/parity-gate.md) is unchanged at
      **255 tests in 24 classes**.

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
