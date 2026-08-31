# Params bind at load time

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The one unchecked bridge in the subsystem, in one place.

A Wiring row carries its strategy configuration as a raw jsonb string. This slice resolves that
string, plus the type key beside it, into the strategy's declared params type — **at load time, not
at evaluation time**. The compiled graph therefore carries already-bound typed params, which is what
makes it a projection rather than a row-shaped copy, and it is why binding belongs to this slab even
though loading the graph does not.

Follow the cast idiom the webhook event mapper already uses for its own bridge — a private generic
method narrowing through the declared type — rather than suppressing a warning.

Binding refuses rather than defers. An unknown type key, malformed JSON, or params that do not fit
the declared type are failures at binding, when a human is publishing a definition, not surprises
when a user clicks something. Null params stay legal: a strategy that takes none needs none.

The fixture builder from slice 02 now takes **raw JSON strings** and runs them through the real
binder, so a Guard receiving its typed params and behaving accordingly is observable at the
evaluator seam. That is deliberate and is why this slice adds no test seam of its own.

## Acceptance criteria

- [ ] A raw jsonb params string binds to the type the strategy declares, and the strategy receives a
      typed object rather than a string or a map.
- [ ] Null params are accepted for a strategy declaring no params type.
- [ ] An unknown type key is refused at binding, naming the key.
- [ ] Malformed JSON is refused at binding.
- [ ] JSON that parses but does not fit the declared type is refused at binding.
- [ ] Exactly one unchecked narrowing exists in the slice, in one place, and it is not a suppressed
      warning.
- [ ] Runtime domain state does not travel in params: no params type in the slab names a Negotiation,
      a Resource or a person.
- [ ] The fixture builder accepts raw JSON and binds through the production binder, not a test-only
      shortcut.
- [ ] A Guard reading its bound params and changing its answer is demonstrated through the evaluator.
- [ ] No production code calls the binder.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**Why the fixture builder must use the real binder.** If fixtures took pre-bound typed params, the
bridge would need a seam of its own and would be exercised only by tests about itself. Feeding raw
JSON means every later test that uses params exercises it, which is the higher seam and the smaller
total surface. Settled in the PRD's Testing Decisions.

**Binding failures belong to the registry seam**, where construction already refuses, not to the
evaluator's.

**Neither params column has a uniqueness constraint on its type key**, so binding must handle the
same key appearing twice with different params in one definition.

## Blocked by

- [03 Guards enter the pipeline](03-guards-enter-the-pipeline.md)
