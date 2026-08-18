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
custom Definition Family those names need not exist, and each KPI then silently reports zero rather
than failing. What do these KPIs mean once a Network can run its own Resource Lifecycle?

Raised by ticket [03](03-state-event-identity-downstream.md) decision 5. Stage 1 deliberately leaves
all four untouched — one seeded Resource family means every name is present and behaviour is
identical, so touching them would be churn against a parity gate.

### The four, precisely

All in `governance/network/stats/NetworkStatsRepositoryImpl.java`, as **raw string literals in
native SQL and JPQL** — not Java constants, so nothing about them is compiler-visible:

| KPI | Line | Definition | Intended semantic |
|---|---|---|---|
| "Ignored" negotiations | 28, 51 | `nrl.current_state` is `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE` | nobody ever responded |
| "Successful" negotiations | 97, 119 | `rl.currentState = 'RESOURCE_MADE_AVAILABLE'` | the resource was delivered |
| Active representatives | 216 | audit `nrlr.changed_to` is **neither** of the two spawn states | a human actually moved it |
| Status distribution | 163 | `n.currentState != 'DRAFT'` | *Negotiation* scope — **not this ticket**, covered by ticket 03 decision 4 |

The first and third are "the two States that Spawn writes", which ticket 01 pinned empirically and
ADR 0007's `SPAWN_RESOURCE_LIFECYCLES` owns. So their definition is really "the Lifecycle never
advanced past Spawn" — a structural idea currently expressed by naming two strings.

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
