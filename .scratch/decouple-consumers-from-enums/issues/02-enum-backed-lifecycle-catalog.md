# The Enum-Backed Lifecycle Catalog

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The second half of the expand step, and the one new component this slab introduces: a single
disposable place holding the last compile-time knowledge of the closed State and Event sets.

Ticket 03 answered three questions by pointing at an enum, and each answer has to survive this slab
while its source of truth is not yet available. The catalog answers all three:

- **Does this name exist?** For the Negotiation status filter, so its 400 on an unknown value
  survives, and for the Event named in a lifecycle URL path.
- **What is this name's label and description?** For the notification body that names both Resource
  States of a change, and for the assemblers that put a label on a link.
- **What is this Resource State's ordinal?** For the metadata contract the frontend sorts by.

It lives **inside `negotiation/state_machine/`** and is backed by the four enums. That placement is
the whole design: the slab gate permits enum references there, a fixed set is honest for exactly as
long as Spring Statemachine runs, and the cutover deletes the class together with the library rather
than having to find and unpick assumptions spread across five subsystems.

Nothing reads it when this slice lands.

## Acceptance criteria

- [ ] The catalog lives inside the Spring Statemachine package and is package-private if the callers
      that arrive in later slices allow it, or as narrow as they do allow.
- [ ] It answers name existence, label, description and Resource State ordinal, for both Definition
      Scopes where the question applies.
- [ ] Its answers are derived from the enums rather than restated, so a name cannot drift.
- [ ] Its javadoc states plainly that it is deleted at cutover, and names what replaces each method —
      reads of the `state` and `event` rows.
- [ ] Unit tests cover each answer, including the unknown-name case for the existence check.
- [ ] No production code references it yet.
- [ ] The Definition Version tables are **not** read. `DefinitionInertnessGuardTest` stays green and
      stays in the tree.
- [ ] Full backend suite green.
- [ ] Parity green at 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**This is a filed departure from ticket 03's decision 3, not a shortcut.** That decision requires the
status filter to validate against the State rows of the Negotiation-scope active Definition Version.
It is not buildable here: slab 08's tables exist but are empty until the migration slab seeds them,
so the lookup would refuse every value and take parity red — and it would make this the first slab
to read those tables, which per the map obliges it to delete `DefinitionInertnessGuardTest` a whole
stage early. The decision's *intent* survives intact: the 400 is preserved, and the check is not a
hand-maintained Java list. Only the source of truth is deferred, and the cutover slab's replacement
of these three methods is the recorded trigger.

**Name it for disposability.** The deliberate symmetry is with `EnumBackedLifecycleTestAdapter`,
which ticket 01 built in test scope for exactly the same reason and with exactly the same lifetime.

**Do not let it grow.** It answers metadata questions only. It must never acquire a method that
decides whether a Transition may fire — that is the Transition Evaluator's, and building any of it
here would put behaviour in a class designed to be deleted.

## Blocked by

None - can start immediately. Independent of slices 1, 3 and 7; may be authored in parallel with
them, but test runs must be serialized.
