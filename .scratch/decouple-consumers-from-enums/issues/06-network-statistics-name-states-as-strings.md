# Network statistics name States as strings

Status: resolved

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

- [x] The three statistics files name no Lifecycle enum. `NetworkStatistics`,
      `SimpleNetworkStatistics` and `NetworkStatisticsServiceImpl` each dropped the
      `NegotiationState` import; a grep over the whole `governance/network/stats` package finds no
      occurrence of any of the four enum names.
- [x] The status-distribution map is keyed by name and its schema example is unchanged.
      `Map<String, Integer>` on the interface, the DTO field and the local in the service. Both
      `@Schema(example = …)` annotations are byte-identical — the diff touches neither line.
- [x] No query text is altered — slice 3's guard stays green without amendment.
      `NetworkStatsRepositoryImpl` is not in the diff at all, so none of the guard's eleven pinned
      entries in it — across seven distinct lines — can have moved. `RawStateNamesInSqlGuardTest`
      6/0/0/0, byte-identical to its state before this slice.
- [x] Every statistic returns the same number as before for the same data. The endpoint's whole
      body was captured before the change and pinned in `NetworkControllerTests`: all seven
      statistics, both `negotiationIds` lists and all three distribution entries. The pin was
      written and run **green against the enum-keyed code first**, so it records the old numbers
      rather than the new ones.
- [x] The schema metadata for the distribution keeps its worked example. Both examples survive,
      including the interface's `{"OPEN": 50, "CLOSED": 90, "PENDING": 10}` — which names three
      States that do not exist and never did. Left alone deliberately: correcting it would be a
      published-schema change this slice did not come to make, and the AC asks for it unchanged.
      Filed below.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0. Measured in the worktree at
      the tip of this slice: parity 255 in 24 classes, 0 failures, 0 errors, 1 skipped, no report
      written for the delta class; deltas 8/0/0/0; full suite 1473/0/0/16 in 159 classes.

## Notes

**A silently-zero KPI is the failure mode to think about, and it is not created here.** A Definition
Family that omits a named Resource State makes the metric report zero rather than fail. That is map
ticket 10's question, recorded so this slice does not try to answer it.

**No dependency on the holders.** These three files reference the enums as types only — no constant
appears in ticket 03's sweep for this subsystem — so this slice needs nothing from slice 1.

**The only observable difference is JSON key order, and it was never stable.** Before: the keys came
out in a `HashMap`'s order over enum keys, and an enum's `hashCode` is its identity hash — so the
order varied from one JVM run to the next. After: the same `HashMap`, keyed by strings, so the order
is `String.hashCode` and is now the *same* on every run. No subscriber could have depended on the
old order, and nothing about the set of keys or their values moved. Recorded because the observed
bodies differ textually, and a later reader comparing them should not read that as a regression.

**Two facts slice 03 handed this slice, both confirmed, and one of them sharpened.**
`getMedianResponseForNetwork` exists twice with identical SQL and the service calls the
`NegotiationRepository` copy, so `NetworkStatsRepositoryImpl:64` is unreachable from production —
confirmed. Slice 03's second fact was that *three* of that file's queries have no production caller;
the number of uncalled **methods** is **four**. The three it named are the uncalled queries that
hold pinned literals — `countIgnoredForNetwork`, `getMedianResponseForNetwork` and
`getNumberOfSuccessfulNegotiationsForNetwork`. `countAllForNetwork` is a fourth with no reference
outside its own interface, and it holds no literal, which is why slice 03's sweep did not count it.
Five called plus four uncalled is the file's nine. Neither fact changes this slice's work — no enum
reached any query — but both were checked rather than trusted, because "network statistics" reads as
a much bigger surface than the five methods the service actually calls.

**The stale schema example is filed, not fixed.** `NetworkStatistics.getStatusDistribution`'s
`@Schema(example = "{\"OPEN\": 50, \"CLOSED\": 90, \"PENDING\": 10}")` names three States that
are in no Definition Family and in none of the four enums; `SimpleNetworkStatistics` carries a
correct example on the same field. Which one the published schema shows depends on which class
springdoc resolves the property from. Out of scope here by AC, and it is not a decoupling defect —
it is a documentation bug that predates the slab and outlives it.

## Blocked by

- [03 Pin the raw State names in SQL](03-pin-the-raw-state-names-in-sql.md)
