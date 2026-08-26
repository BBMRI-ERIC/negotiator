# The three Well-known name holders

Status: done

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The first half of the expand step: the names that survive the enums' deletion, added beside them so
nothing breaks.

Three holders of `String` constants, carrying the nine names that some behaviour depends on
*existing* — five Well-known States on the Negotiation side, three Resource States that Spawn
writes, and the Override Event. Ticket
[03](../../state-machine-implementation/issues/03-state-event-identity-downstream.md) fixes the
contents exactly and derived them from a sweep of all 35 constant references; do not add a tenth
name, and do not let any holder grow toward declaring a complete set. A holder that lists every
State is the enum again with worse ergonomics.

Name them so a stale import cannot silently resolve against one — deliberately unlike the enum
names they replace.

Nothing reads the holders when this slice lands. That is the point: every later slice migrates its
own subsystem onto them.

The slice also removes one piece of dead code that ticket 03 identified and this slab's recon
confirmed. `NegotiationStatusConverter` is registered nowhere — `WebConfig` wires the two Event
converters and the role converter and never this one — so the 400 it appears to produce actually
comes from Spring's default enum binding. It is a deletion, not a migration. Note that the unit test
covering it also covers an unrelated role converter, so the test **class survives** and only its
status-converter method goes; ticket 03 records this as the whole class going, which overstates it.

## Acceptance criteria

- [x] Three holders exist, carrying exactly the nine names ticket 03 specifies and no others.
- [x] Each holder is a holder of constants only — no behaviour, and not instantiable.
- [x] No production code references any holder yet.
- [x] `NegotiationStatusConverter` is deleted, and the sweep for references to it comes back empty
      apart from the one test method removed with it.
- [x] The unrelated role-converter test in the same class still exists and still passes.
- [x] The four Lifecycle enums are untouched, and no consumer has been migrated in this slice.
- [x] Full backend suite green.
- [x] Parity green at its unchanged count — 255 tests in 24 classes, 0 failures, 1 skipped — and the
      intended-delta half at 8 tests, 0 failures. Read both out of the surefire reports, checking
      mtimes.

## Notes

**Where the holders live is a real choice and is not pre-decided here.** They must not sit inside
`negotiation/state_machine/`: that package is deleted at cutover and these names outlive it. Pick a
location that reads naturally from all five consumer subsystems and record the reasoning in
`STATUS.md`, because slices 4, 5, 8, 9 and 11 all import from wherever this lands.

**`WellKnownResourceStates` carries a hazard the Negotiation one does not.** ADR 0004 keeps a single
Negotiation-scope definition, so those five names are as stable as the enum was. Resource scope is
precisely the scope that diverges once stage 2 ships. Ticket 03 recorded this as noted-not-solved;
do not try to solve it here.

**Do not run two Maven invocations against `backend/` at once** — a concurrent recompile clears
`target/test-classes` under a running suite and presents as ~150 unrelated failures.

## Blocked by

None - can start immediately. Independent of slices 2, 3 and 7; may be authored in parallel with
them, but test runs must be serialized.
