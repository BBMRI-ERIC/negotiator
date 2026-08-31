# The thinnest evaluation, end to end

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The tracer bullet. After this slice the Transition Evaluator exists and answers real questions; every
later slice widens this one path rather than adding a part nothing uses.

A compiled definition graph carrying only what is needed to answer a question: States with their
label and their initial and terminal flags, Transitions with their Required Authority, and the
lookups the pipeline asks for — the Transition for a State-and-Event pair, the Transitions leaving a
State, the initial State, and whether a State name is terminal. No Guard or Action entries; those
arrive with later slices, each widening the record. The graph carries the Definition Version's **row
id alone** as identity — no family key and no version sequence anywhere in it or in anything that
keys off it.

A `TransitionEvaluator`, one scope-parameterized component serving both Definition Scopes. **Its
constructor takes no repository, no `EntityManager` and no Spring Data type.** It is handed a graph,
a current State name, an Event name and an evaluation context carrying the caller's held authorities.

The Evaluation Pipeline with Required Authority as its only stage so far. An outcome that is either
permitted, carrying the target State, or refused, carrying a monotonic failure category and a reason
code. **The evaluator knows nothing about HTTP** — the status mapping is the cutover slab's.

And the property this whole design exists for: `mayFire` and Possible Events call the same function.
The listing is a dry run over the Events reachable from the current State, and a blocked Event is
omitted rather than listed as unavailable.

Plus the fixture builder in test scope that every later slice's tests use, able to express graphs
shaped like both v1 families from ticket 01's dump.

## Acceptance criteria

- [ ] A State arrives as a **name**, never an id, and a State that no Transition targets still
      resolves as a position.
- [ ] The graph's identity is the Definition Version row id alone; no type in the slice accepts or
      exposes a family-key-and-version pair.
- [ ] The evaluator's constructor parameters include no repository, no `EntityManager` and no Spring
      Data type.
- [ ] An Event with a Transition from the current State and satisfied authority is permitted, and the
      outcome names the target State.
- [ ] An Event with no Transition from the current State is refused, distinguishably from one refused
      on authority.
- [ ] Required Authority `NONE` permits any caller; `IS_ADMIN`, `IS_CREATOR` and `IS_REPRESENTATIVE`
      permit only a caller holding that authority, and refuse with the authorization category.
- [ ] `SYSTEM` refuses **every** human caller and never appears in Possible Events.
- [ ] Possible Events omits every blocked Event and lists no Event as unavailable.
- [ ] A test drives the same fixture graph through both entry points and asserts they cannot
      disagree — every Event `mayFire` permits appears in the listing, and every Event it refuses is
      absent.
- [ ] Authority is single-valued. Nothing in the slice accepts a set of authorities or an
      admin-or-creator value.
- [ ] The evaluator names no HTTP status, `ResponseEntity` or Spring Web type.
- [ ] The fixture builder can express a graph with an unreachable State, a terminal State and an
      Event carrying no Transition.
- [ ] No production code calls the evaluator.
- [ ] `DefinitionInertnessGuardTest` still green at 6.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**Why this is the tracer bullet and not the graph alone.** A slice that lands only the records would
be a horizontal slab of one layer, verifiable only by reading it. Threading a single evaluation
through the graph, the pipeline and the outcome means the primary test seam exists from here on, and
the seam decision — Guards tested through the evaluator, never directly — becomes available to every
later slice instead of arriving at the end.

**Failure categories are monotonic by design** — authorization, then unmet requirement, then
domain-state conflict. Only the first exists in this slice. Model the ordering now so later slices
slot in rather than reshuffle.

**Two schema facts to respect even though no wiring lands here.** The same strategy type key may
appear twice in one definition at different sort orders, so a Guard chain is a list and never a map.
And any number of States may carry the terminal flag.

**`possibleEvents` is a dry run, not a second implementation.** If the two entry points share
anything less than the whole pipeline, this slice has missed its point.

## Blocked by

- [01 The vocabulary move and the amended inertness gate](01-vocabulary-move-and-inertness-gate.md)
