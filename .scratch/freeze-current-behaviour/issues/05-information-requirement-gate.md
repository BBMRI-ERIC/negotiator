# Information Requirement gate parity

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin the Information Requirement gate exactly as the Resource lifecycle service enforces it today,
because ADR 0005 turns this into a Built-in Stage and needs to reproduce the same block.

Today the check happens at the very top of sending a Resource event, before availability is even
consulted: if any Information Requirement exists for the event being sent, and no Information
Submission exists for that Resource and Negotiation pair, the send is refused.

Two details matter more than they look.

The refusal is a `StateMachineException` — a Spring Statemachine type that ceases to exist after the
cutover. Its message is user-facing and the frontend surfaces it. Pin the message text and record
that the type must change, so whatever replaces it is a deliberate choice.

The submission check is **not** scoped to the requirement. It asks only whether *any* submission
exists for that Resource in that Negotiation, so a submission against a different requirement
satisfies the gate for all of them. Pin that as it is — it is very likely a bug, and freezing it is
precisely how the cutover avoids changing it by accident.

Note also that this gate is evaluated ahead of the availability check, so a blocked-by-requirement
event and a not-currently-available event produce different outcomes: the former throws, the latter
silently returns. Pin both orderings.

## Acceptance criteria

- [ ] Sending a Resource event with a matching Information Requirement and no submission is pinned as
      refused.
- [ ] The refusal's exception type and full message text are pinned.
- [ ] The same send succeeds once a submission exists for that Resource and Negotiation.
- [ ] Sending an event with no Information Requirement attached is pinned as unaffected by the gate.
- [ ] The unscoped-submission behaviour is pinned: a submission against a *different* requirement
      satisfies the gate.
- [ ] The gate's precedence over the availability check is pinned, showing that a
      requirement-blocked event throws while a merely unavailable event returns silently.
- [ ] A note records that the exception type must change at cutover, since the current type is a
      Spring Statemachine class.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes, with the
      exception type referenced in a way that does not import a Spring Statemachine class into the
      characterization tree.
- [ ] No production code is modified.

## Blocked by

- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
