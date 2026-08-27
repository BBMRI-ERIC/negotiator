# Resource governance names States as strings

Status: resolved

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

Two files in resource governance: the service that adds and removes Resources on a Negotiation and
applies an administrator's direct state change, and the handler that finds Resources with no
representative.

The service names the most constants of any consumer in the slab — Negotiation States gating what
may be edited, Resource States it writes, and the Override Event. All become holder constants.

The non-represented Resources handler is one of the two query-driven sites ticket 02 moved *into*
this population rather than out of it. It filters on a State column rather than reacting to a
Transition, so it is not Action-shaped: a missing name makes it silently return nothing, which is a
data hole of the same kind as the statistics queries, not the behaviour hole a missing trigger name
would be.

## Acceptance criteria

- [x] Both files name no Lifecycle enum.
- [x] Adding and removing a Resource is permitted and refused for exactly the same Negotiation States
      as before, with identical error messages and status codes.
- [x] The administrator's direct state change still records history under the Override Event's name.
- [x] The non-represented Resources handler selects the same Resources for the same data.
- [x] The existing resource controller and repository tests are extended rather than replaced.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**The Override Event is named in Java deliberately, and it is not a fragile bet.** Ticket 03 checked
whether it needed a parallel Well-known Event term and rejected one: ADR 0002 makes the Override
Event structural to the model — it is how an administrator's direct state change appears in history
under a name at all — so every Definition Version is expected to carry it. Naming it references a
modelled concept rather than gambling on a family's vocabulary.

**This service is the second producer of Resource state-change events**, the one that is not a
Transition. The parity gate does not cover its authorisation rule, its draft branch or its
newly-added-Resources branch — the override *path* is pinned, the governance rules around it are
not. Change none of them.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)

## Resolution

`ResourceServiceImpl` and `NonRepresentedResourcesHandlerImpl` now compare and write State and Event
names through the three holders. Temporary conversions live at the enum-owning entity, repository
and application-event seams, ready to disappear in slices 10 and 11.

The existing resource controller, resource repository and handler tests now pin the returned State
names, draft initialization, non-draft authorization response, and unchanged selection of only
unreachable Resources in ongoing Negotiations. The existing Lifecycle event characterization still
pins the direct State change under `OVERRIDE`.

Verification on 2026-08-27: focused resource and event tests green; full backend suite 1463 tests in
158 classes, 0 failures, 0 errors, 16 skipped; parity 255 tests in 24 classes, 0 failures, 0 errors,
1 skipped; intended deltas 8 tests, 0 failures, 0 errors, 0 skipped.
