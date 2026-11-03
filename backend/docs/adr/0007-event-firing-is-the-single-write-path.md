---
status: accepted
---

# Event Firing is the single write path

The Transition Evaluator judges a move against a supplied Compiled Graph and context, returning the permitted Action chain without executing it (see [One Transition Evaluator over a Compiled Graph](0006-one-transition-evaluator-over-a-compiled-graph.md)). Event Firing owns context assembly and Transition execution for both Definition Scopes, preventing their Lifecycles from drifting. Every Transition, whether requested by a person or an Orchestration Trigger, passes through it.

Callers supply an Event, their identity and an unambiguous Lifecycle reference covering the closed set of Definition Scopes. Event Firing loads the pinned Compiled Graph and relevant context: creator, representatives, parent State and, for Negotiations, sibling Resources with their own Compiled Graphs. It never runs Definition Resolution: publishing a Definition must not change in-flight work.

Firing an Event and listing Possible Events use the same context assembly and evaluation. Their answers must agree for the same context; a lighter listing query would violate this invariant. Listing writes nothing and runs no Actions.

Refusals return a failure category and reason code; the HTTP surface owns their status mapping. Event Firing throws when evaluation is impossible: an unknown Negotiation or Resource, an Unstarted Lifecycle without a pin, or a pinned Definition Version that cannot compile.

A permitted Transition writes the new State and Lifecycle Record together, then executes Actions in configured order. Arrival at the initial State creates no Lifecycle Record. The target Lifecycle comes from the evaluated context, ensuring the Transition changes only the Lifecycle it was permitted for.

Context reads, evaluation, writes and Actions share one database transaction, using the same entity throughout. Actions can read the new State and Lifecycle Record, but any Action failure rolls back the move, preventing a committed State with incomplete Actions. Spawn therefore participates in the Negotiation's approval transaction (see [Lifecycle coupling is orchestration](0011-lifecycle-coupling-is-orchestration.md)).

## Considered options

- **Each Lifecycle service assembles context and commits independently, sharing only the evaluator.** This allows the Definition Scopes to drift in both evaluation inputs and execution.
- **The Transition Evaluator owns reads and writes.** This mixes transition decisions with persistence, breaking the evaluator's database-independent boundary.
- **Callers supply the evaluation context.** This duplicates assembly and allows Transitions to be judged against context inconsistent with persisted facts.
- **Actions run in a separate transaction after the move commits.** Failures would leave incomplete effects and require retry or compensation rather than atomic execution.

## Consequences

An Action failure is a failed move, not a refusal, and leaves the Lifecycle unchanged. Effects that must be able to fail without undoing a move belong in post-commit listeners, not Actions (see [Notifications stay listeners](0010-notifications-stay-listeners.md)).
