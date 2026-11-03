---
status: accepted
---

# Audience and Qualifying Submission are derived

The [Built-in Stage](0008-information-requirements-gate-transitions-as-a-built-in-stage.md) checks Information Requirements before an Event fires. This ADR defines who must answer and how satisfaction is derived.

Audience and Quantifier are configuration; satisfaction is a boolean derived from recorded submissions, with no stored per-occurrence state.

An Audience Resolver follows the Guard catalogue-and-configuration model, using its configuration and the Evaluation Pipeline's context to return the full Audience. That set supplies membership checks, the `ALL` threshold and [Audience Notification](0010-notifications-stay-listeners.md):

- `RESOURCE_REPRESENTATIVES`, for the Resource Definition Scope only.
- `NEGOTIATION_CREATORS`, for both Definition Scopes, including Resource requirements addressed to the requester.
- `IAM_GROUP`, for both Definition Scopes.

Each resolver declares its supported Definition Scopes; incompatible configuration is rejected when saved.

The Quantifier counts distinct submitters in the resolved Audience: `ANY` requires one, `N_OF_M` a configured N, and `ALL` the current Audience size. Only the `ALL` threshold changes with membership. Repeated submissions count once; submissions from outside the Audience are retained but do not qualify.

The Audience is resolved afresh at each check. Membership changes can change Possible Events but cannot undo a committed Transition, whose resulting State is stored independently.

Derivation requires durable domain evidence for every action that produces the fact. Submissions provide that evidence; future waivers or notification-dependent reminders and expiry must do likewise. Audience Notification remains an unrecorded post-commit effect because no decision depends on whether it happened.

This ADR defines the `IAM_GROUP` resolver's contract, not how LS-AAI membership is obtained or kept current.

## Considered options

- Give each Information Requirement its own Lifecycle. We rejected it because it requires per-occurrence state for a decision that is only yes or no.
- Freeze the Audience when it is notified. We rejected it because it requires a per-occurrence snapshot.

## Consequences

- With no Audience configured, the defaults are `RESOURCE_REPRESENTATIVES` and `ANY`.
- A submission counts only for the Information Requirement it answers, even within the same Negotiation and Resource.
