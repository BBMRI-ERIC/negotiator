---
status: accepted
---

# One Transition Evaluator over a Compiled Graph

Both Lifecycles use one stateless Transition Evaluator to decide whether an Event is allowed from the current State and which Transition applies.

The evaluator works from a Compiled Graph for the relevant Definition Version and the facts needed for the decision. It does not change domain state, run Actions or append Lifecycle Records.

If no Transition matches the Event and current State, evaluation refuses the Event. Otherwise, checks run in a fixed order, stopping at the first failure:

1. **Required Authority:** the caller must hold at least one of the Transition's required authorities.
2. **Information Requirements:** required information must have been submitted.
3. **Guards:** domain conditions must hold. Definition-scope Guards run before Transition-scope Guards, respecting their configured order.

Refusals distinguish missing Transitions, insufficient authority, unmet Information Requirements and domain-state conflicts. Guards provide the reason for a domain-state conflict.

An unknown current State indicates inconsistent lifecycle data rather than a refused Event.

Firing an Event and listing Possible Events use the same evaluation rules. The listing includes only Events permitted by the current facts, without running Actions or changing state.

This keeps discovery and enforcement aligned; changing facts may still change a later decision.

## Considered options

- An evaluator that also loads Definitions. Rejected to keep data retrieval separate from transition decisions.
- Separate rules for listing Possible Events. Rejected because discovery and enforcement could drift apart.
