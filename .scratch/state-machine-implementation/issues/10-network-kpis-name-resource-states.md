# Network KPIs name Resource States a custom family need not have

Type: grilling
Status: not-on-the-frontier
Stage: 2
Blocked by: stage 2 itself — there is no stage-1 ticket that unblocks this

**Not takeable.** Stage 2 is fog on the map, so nothing here can be resolved until a Resource
Definition Family other than the Global Default one can actually exist. Recorded now, while the
evidence is fresh, rather than left to be rediscovered.

## Question

Four network statistics define a business metric by **naming specific Resource States**. Under a
custom Definition Family those names need not exist, and each KPI then reports a wrong number rather
than failing. What do these KPIs mean once a Network can run its own Resource Lifecycle?

Raised by ticket [03](03-state-event-identity-downstream.md) decision 5. Stage 1 deliberately leaves
all of them untouched — one seeded Resource family means every name is present and behaviour is
identical, so touching them would be churn against a parity gate.

### The five queries, precisely

**Four** of these name a Resource State and are this ticket's; the fifth names a Negotiation State
and is not. All are **raw string literals in native SQL and JPQL** — not Java constants, so nothing
about them is compiler-visible. They live in **two** files, not one: `stats` below is
`governance/network/stats/NetworkStatsRepositoryImpl.java`, and the median query the statistics
service actually calls is in `negotiation/NegotiationRepository.java`.

| KPI | Query and site | Definition | Intended semantic |
|---|---|---|---|
| "Ignored" negotiations | `getIgnoredForNetwork` — stats:51 | `nrl.current_state` is `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE` | nobody ever responded |
| Median response time | `getMedianResponseForNetwork` — **`NegotiationRepository:33`** | audit `nrlr.changed_to` is `CHECKING_AVAILABILITY` or `RESOURCE_UNAVAILABLE` | days until a representative first responded |
| "Successful" negotiations | `getSuccessfulForNetwork` — stats:119 | `rl.currentState = 'RESOURCE_MADE_AVAILABLE'` | the resource was delivered |
| Active representatives | `getNumberOfActiveRepresentatives` — stats:216 | audit `nrlr.changed_to` is **neither** of the two spawn states | a human actually moved it |
| Status distribution | `countStatusDistribution` — stats:163 | `n.currentState != 'DRAFT'` | *Negotiation* scope — **not this ticket**, covered by ticket 03 decision 4 |

"Ignored" and Active representatives name "the two States that Spawn writes", which ticket 01 pinned
empirically and ADR 0007's `SPAWN_RESOURCE_LIFECYCLES` owns. So their definition is really "the
Lifecycle never advanced past Spawn" — a structural idea currently expressed by naming two strings.
They are the *same* question asked in opposite directions: one wants the Lifecycle to have stayed
where Spawn put it, the other wants it to have moved. Anything structural bought for one is bought
for both.

### Sharpen at least

1. **Is silently-zero acceptable?** A Network whose family omits `RESOURCE_MADE_AVAILABLE` reports
   zero successful negotiations forever, indistinguishable from a genuinely unsuccessful Network.
   That is a wrong number rather than a missing one, which is the worse failure for a KPI.
2. **Refuse, degrade, or annotate?** Options include rejecting a family that omits the names at
   publish time (same moment as the Well-known State check ticket 02 deferred to stage 3), returning
   the KPI as null/absent rather than zero for such Networks, or accepting zero and documenting it.
3. **Does "ignored" generalise better than "successful"?** "Never advanced past Spawn" can be
   expressed structurally — the State a Spawn Action wrote, still unchanged — without naming
   anything. "Successful" cannot: it is an outcome judgement, and there is no outcome concept in the
   model.
4. **This is outcome-sensitive conclusion in disguise.** The map's **Out of scope** section rules out
   "routing all-delivered vs all-unavailable to distinct terminal outcomes", noting ADR 0007's
   mechanism *accommodates it as later configuration* — "excluded, not foreclosed". A success/outcome
   flag on `State` would answer this ticket and reopen that scope decision at the same time. Decide
   deliberately whether this ticket is the moment that returns.
5. **Do the KPIs need to be comparable across Networks?** If two Networks run different families,
   a per-family definition of "successful" makes the numbers incomparable — which may be correct, or
   may defeat the purpose of a cross-Network statistics page.

### Why it is a ticket rather than fog

The question is already precisely stateable, which is wayfinder's test — even though it is blocked
until stage 2 supplies the families that make it real. What is *not* known is the answer.

Use `/grilling` + `/domain-modeling`.

## Corrected 2026-09-10 — the ticket listed three of the four, and named the wrong file

Found while reading the queries directly, ahead of the grilling. Three claims in the body above were
wrong and are now fixed; this section records what was wrong and how to re-check it.

**1. The median response time KPI was missing.** It filters the audit column
`negotiation_resource_lifecycle_record.changed_to` for `CHECKING_AVAILABILITY` or
`RESOURCE_UNAVAILABLE` — two of the twelve Resource States — so it belongs in the table and in every
"Sharpen" point below. Four statistics name a Resource State, and this ticket listed three of them.
The old table's fourth row, Status distribution, names a *Negotiation* State and was already marked
out of scope.

**2. "All in `NetworkStatsRepositoryImpl.java`" was false.** Three of the fourteen pinned literals
are in `negotiation/NegotiationRepository.java` — lines 33 ×2 (median) and 71 (`DRAFT`). ADR 0008
converts `changed_to` to a foreign key and so breaks six literals in three queries, **one of which
exists twice, in two repositories**. A slab that greps only the stats package will miss half of it.

**3. Two of the four cited line numbers had no production caller.** The old table cited 28 and 97.
`countIgnoredForNetwork` (28), `getNumberOfSuccessfulNegotiationsForNetwork` (97) and the duplicate
`getMedianResponseForNetwork` (75) are reached only by `NegotiationRepositoryTest`;
`countAllForNetwork` (130) has no caller at all. The live sites are the ones now in the table.
**Consequence worth its own look:** the repository test at `NegotiationRepositoryTest:427` exercises
the *dead* copy of the median query, so the live copy at `NegotiationRepository:33` has no test.

**Two observations that sharpen the question rather than correct it.**

- **The harm is not uniformly "silently zero".** A query that *looks for* a name finds nothing and
  falls to zero. A query that *removes* a name removes nothing and **over-counts**. Active
  representatives, Status distribution and the network total are all of the second kind. Sharpen
  point 1 below is written as if only the first kind existed; a number that quietly grows is the
  harder failure to notice, because nobody investigates a good number.
- **Sharpen point 2's "return null rather than zero" already exists here, by accident.**
  `PERCENTILE_CONT` over an empty set returns SQL `NULL`; `NetworkStatisticsServiceImpl:30-37`
  catches the resulting `NullPointerException` and emits `null`. So one field of this API already
  answers "absent" rather than "zero", and every consumer already tolerates it. That turns the
  refuse/degrade/annotate choice into a question about consistency with behaviour that already ships.

**How to re-check all of this in one command:** run `RawStateNamesInSqlGuardTest`. It pins all
fourteen literals by file, line, name and SQL quoting, and its javadoc attributes each one to the
KPI it serves and to what breaks it. It fails the build when it drifts, which no ticket does.
