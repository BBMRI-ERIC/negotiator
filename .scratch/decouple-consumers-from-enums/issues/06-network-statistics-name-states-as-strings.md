# Network statistics name States as strings

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

Three files in the network statistics subsystem, where the State appears as a map key in the
per-status distribution a network manager sees. The Java types become `String`.

**The SQL is not touched.** Ticket 03's decision 5 settled that deliberately: the four network
statistics each define a business metric by naming Resource States in query text, and leaving them
alone is behaviour-identical, parity-safe, zero-churn and correct while one seeded Resource family
exists. Making them structural instead would mean putting a success or outcome flag on the State
row, which is outcome-sensitive conclusion — already ruled out of this map's scope, and not
something to smuggle back in through a statistics query.

The gap those literals leave becomes real when stage 2 ships, because Resource families are exactly
what diverges. It is already ticketed as map ticket 10 and is not this slice's problem.

Slice 3 has already pinned the literals, so this slice cannot quietly alter one.

## Acceptance criteria

- [ ] The three statistics files name no Lifecycle enum.
- [ ] The status-distribution map is keyed by name and its schema example is unchanged.
- [ ] No query text is altered — slice 3's guard stays green without amendment.
- [ ] Every statistic returns the same number as before for the same data.
- [ ] The schema metadata for the distribution keeps its worked example.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**A silently-zero KPI is the failure mode to think about, and it is not created here.** A Definition
Family that omits a named Resource State makes the metric report zero rather than fail. That is map
ticket 10's question, recorded so this slice does not try to answer it.

**No dependency on the holders.** These three files reference the enums as types only — no constant
appears in ticket 03's sweep for this subsystem — so this slice needs nothing from slice 1.

## Blocked by

- [03 Pin the raw State names in SQL](03-pin-the-raw-state-names-in-sql.md)
