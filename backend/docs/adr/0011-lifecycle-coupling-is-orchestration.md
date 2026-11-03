---
status: accepted
---

# Lifecycle coupling is orchestration

Lifecycle coupling belongs in an orchestration layer around the [Transition Evaluator](0006-one-transition-evaluator-over-a-compiled-graph.md), which remains unaware of it. Spawn starts Resource Lifecycles when a Negotiation starts Resource work; Feedback concludes the Negotiation when all its Resources finish.

The [Orchestration Trigger](0003-required-authority-is-a-set.md) re-attempts System Events from a Lifecycle's current State through the Evaluation Pipeline. Committed writes on related Lifecycles and clock ticks use the same path; scheduling is deferred to features that need it.

Feedback pulls the Orchestration Trigger after any Resource State write commits, including an Override Event, without configuring a target Event. It runs in a separate transaction, so a failed conclusion cannot undo the Resource's move.

`AUTO_CONCLUDE` moves a Negotiation from in progress to concluded only when every Resource is terminal, including when there are no Resources. Its Guard uses each Resource's pinned Definition Version to determine terminality, not shared State names. Human `CONCLUDE` remains separate and unguarded.

Concurrent attempts are serialized per Negotiation and evaluate its latest State. Once concluded, no automatic conclusion Transition remains, so repeated attempts change nothing.

Spawn is a Negotiation Action wired to every Transition that starts Resource work, including resumption; Resource definitions need no Spawn configuration.

Spawn resolves and pins missing Definition Versions and initializes only Resources without a State, recording whether a representative was reached. It runs atomically with the Negotiation's move ([Event Firing is the single write path](0007-event-firing-is-the-single-write-path.md)); resolution failure leaves no Resources partially started. It publishes one announcement for notification, not individual Resource state changes.

Spawn initializes a Lifecycle rather than moving it between States. Initialization still appends Lifecycle Records, excluded from the Negotiation's timeline because they do not represent participant activity.

## Considered options

- Configure the Negotiation-to-Resource relationship in a coupling table. Rejected because it would duplicate the existing relationship.

## Consequences

- Subscribers can distinguish automatic conclusions from human conclusions.
- Resource definitions share Well-known State conventions with Spawn for representative contact outcomes.
