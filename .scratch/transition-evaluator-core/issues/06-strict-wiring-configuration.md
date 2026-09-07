# Strict Wiring configuration, and the no-configuration rule

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Two rules about reading a Wiring row's `params` into a strategy's declared type, both living in code
rather than in the container's ambient Jackson settings (D9).

**1. Configuration supplied to a strategy that takes none is refused**, naming the key and the blob.
Null, blank, the literal `null` and `{}` are all accepted — a strategy that takes no parameters
needs none, and an empty object is an ordinary way for a row to say so. Anything else is a row that
looks configured while its configuration goes nowhere, and silently ignoring it is how an
administrator comes to believe a Guard is doing something it is not.

**2. Unknown fields are refused.** The reader used for Wiring configuration enables
`FAIL_ON_UNKNOWN_PROPERTIES` explicitly, regardless of which `ObjectMapper` the container built.
This goes past both prototypes and is a deliberate decision: Spring Boot disables that feature by
default and this repository does not re-enable it, so **today a misspelled field in a Wiring row
binds the type's default and nobody hears about it** — the exact failure that binding configuration
at compile time exists to prevent. Wiring configuration is configuration, not user input.

Neither rule may depend on the injected mapper's leniency, and proving that is the third piece of
work. Prototype B's registry tests construct a strict `ObjectMapper` while production injects Boot's
lenient one, so its suite could not observe production behaviour in either direction — it would have
passed whether the production reader was strict or not. **One test builds a deliberately lenient
mapper and asserts both rules still hold.**

Because binding happens when the graph is compiled and not when an Event fires (D3), both refusals
are compile-time. A definition with a typo in a Wiring row never reaches a user; it fails once,
loudly, where an administrator publishing it can see it (user story 28).

## Acceptance criteria

- [ ] Configuration supplied for a strategy whose params type takes none is refused, and the message
      names both the type key and the offending blob.
- [ ] Null, blank, the literal `null` and `{}` are accepted for such a strategy.
- [ ] A field name that no property of the declared params type matches is refused.
- [ ] Both refusals raise the named exception from
      [04](04-one-named-exception-for-an-impossible-graph.md), at compile time, and both are
      structurally unreachable from the evaluator — which is why they are tested at the catalogue
      seam and not through a fired Event.
- [ ] The reader enables `FAIL_ON_UNKNOWN_PROPERTIES` itself rather than relying on an injected
      mapper's configuration, and the no-configuration rule lives above the mapper rather than
      inside it.
- [ ] A test constructs a deliberately **lenient** `ObjectMapper`, hands it to the registry, and
      asserts both rules still hold. Run it red against the pre-slice code to prove it can see the
      difference.
- [ ] Well-formed configuration still binds to the declared type, including the three-valued
      post-visibility scope.
- [ ] `ApplicationTest` is green — the production reader is the one under test, so the container
      folding every strategy bean with the real mapper is part of the evidence.
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

**One row-level fact the strict reading must not break** (D4). There is no uniqueness constraint on
`type_key` in either Wiring table, so one Definition Version may legitimately wire the same type key
twice at different sort orders — the same Guard, configured two ways. A compiled chain is therefore a
**list** and never a map, and two steps in one chain may report the same key. A strictness change
that started deduplicating by key would break a legal definition.

Note also that the ordering column is `sort_order`, not `order`, and that `params` is `JSONB` mapped
to a Java `String` through `@JdbcTypeCode` — so what a catalogue receives is already a string, and
there is no `JsonNode` in the seam to be strict or lenient about.

## Blocked by

- [04 — One named exception for a graph that cannot exist](04-one-named-exception-for-an-impossible-graph.md)
