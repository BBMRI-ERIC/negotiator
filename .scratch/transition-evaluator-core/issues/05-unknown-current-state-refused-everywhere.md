# An unknown current State refused in every lookup, and the evaluator's ordering

Status: ready-for-agent

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

- [ ] Every graph lookup that takes a State refuses an undeclared one with the named exception from
      [04](04-one-named-exception-for-an-impossible-graph.md): the terminality question, the
      Transition-for-a-State-and-Event lookup, and the Transitions-leaving-a-State lookup.
- [ ] The refusal names the State and the Definition Version, so a log line identifies the broken
      pin without a debugger.
- [ ] The evaluator asks whether the current State is declared **before** it asks whether the Event
      is declared, and a test proves a corrupt pin is never reported as an unknown Event.
- [ ] A Possible Events listing over a graph that does not declare the current State **throws**
      rather than returning an empty set.
- [ ] An empty Possible Events listing still happens, and still means "nothing is available" — a
      terminal State returns empty and does not throw.
- [ ] An Event the version does not declare and an Event declared with no Transition from the
      current State still refuse distinguishably, each with its own reason code.
- [ ] The graph's declares-this-State predicate has a production caller.
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

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
