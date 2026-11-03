---
status: accepted
---

# Information Requirements gate Transitions as a Built-in Stage

Information Requirements belong to Events and apply wherever those Events fire, regardless of State. Separate checks previously allowed Possible Events to offer Events that Event Firing would refuse.

The requirement check is a mandatory Built-in Stage of the Evaluation Pipeline, between Required Authority and Guards, as defined in [One Transition Evaluator over a Compiled Graph](0006-one-transition-evaluator-over-a-compiled-graph.md). Requirements are configurable, but the Transition Evaluator enforces them for every Transition across all Definition Versions without separate Wiring. Event Firing and Possible Events share this pipeline, so unmet Requirements block firing and exclude Events from the listing.

The stage checks only the firing Event's Requirements, counting matching submissions from the Audience against each Requirement's Quantifier. [Audience and Qualifying Submission are derived](0009-audience-and-qualifying-submission-are-derived.md) defines those satisfaction rules; this ADR determines where they are enforced.

Failures use the same refusal structure as Guards, with Event and Lifecycle details. The pipeline assigns the unmet-requirement failure category.

## Considered options

Make the check a registry Guard with Transition- or Definition-scope Wiring, as described in [Lifecycle definitions are relational configuration](0002-lifecycle-definitions-are-relational-configuration.md). This was rejected because either scope depends on configuration being present and cannot guarantee enforcement across every Transition and Definition Version.

## Consequences

Resource listings must expose requirement hints for blocked Events reachable from the current State, even though Possible Events omits them. Submissions may exist without a Resource so Negotiation-scope Events can also carry Requirements.
