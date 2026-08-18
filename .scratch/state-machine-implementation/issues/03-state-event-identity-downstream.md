# State and Event identity for downstream consumers

Type: grilling
Status: resolved
Blocked by: 02

## Question

ADR 0002 deletes the four enums. Roughly **50 main-source files outside the state_machine package** — whatever ticket 02 does not relocate into Actions — still need to name a State or an Event. What do they use instead?

No ADR addresses this. It is the single largest unowned piece of stage 1.

### The consumers, by kind

**JPA queries and filters** — the hardest case, because the identifier reaches the database:
- `negotiation/NegotiationSpecification.java:80-95` — `hasState(List<NegotiationState>, boolean)` builds predicates against `root.get("currentState")`.
- Same file, `:33` and `:49` — hardcodes `NegotiationState.DRAFT` for a **visibility rule**, a semantic dependency on that specific state existing.
- `negotiation/dto/NegotiationFilterDTO.java:33` — `List<NegotiationState> status`, an API-level filter with enum validation today.
- `negotiation/NegotiationRepository.java`, `governance/resource/ResourceServiceImpl.java`.

**Aggregation** — `governance/network/stats/NetworkStatistics`, `SimpleNetworkStatistics`, `NetworkStatisticsServiceImpl` count negotiations by state.

**External contract** — `webhook/event/NegotiationStateUpdatedWebhookEvent`, `NegotiationResourceStateUpdatedWebhookEvent`, `NegotiationAddedWebhookEvent`. Relevant fact: enums already serialize as JSON strings, so the **wire format survives** a swap to `String`; only the OpenAPI enumerated values and `@Schema(example = "DRAFT")` metadata are affected.

**DTOs and mapping** — `negotiation/dto/NegotiationDTO`, `mappers/NegotiationModelMapper`, `mappers/NegotiationStatusConverter`, `mappers/NegotiationEventAssembler`, `governance/resource/dto/ResourceWithStatusDTO`, `ResourceViewDTO`, `negotiation/dto/UpdateResourcesDTO`, `negotiation/NegotiationTimelineImpl`.

**Entities** — `negotiation/Negotiation.java`, `negotiation/NegotiationResourceLink.java` hold the live state column. ADR 0009 fixes this one already: live state **stays an authoritative VARCHAR with no FK**, resolved through the natural key of the Definition Version Pin plus the state name. So the entity field is a string; the question is what the *type* is at the Java boundary.

Plus **26 of 138 test files**.

### Sharpen at least

1. **What is the type?** A bare `String`; a `StateName` / `EventName` value object wrapping a validated string; or keeping the enums as a *vocabulary* type decoupled from the definition rows, converted at the boundary. Note that a value object makes `switch` impossible — which is arguably *correct*, since you cannot exhaustively switch over configurable data.
2. **What happens to API-level validation?** `NegotiationFilterDTO` currently rejects an unknown state with a 400. Filtering on a string means an unknown value silently matches nothing. Is that acceptable, is it validated against the definition rows, or against a fixed vocabulary?
3. **The hardcoded `DRAFT` visibility rule.** The Negotiation Lifecycle keeps one definition so `DRAFT` is stable in practice — but the rule is a semantic dependency on a state name. Does it stay hardcoded, become a State flag, or something else? Note ADR 0002 already gives `State` `initial` and `terminal` flags, so "flags on State" is an established shape.
4. **Do statistics and filters need state *names* or state *ids*?** Ids are stable across label edits but meaningless across families; names are comparable across families but not unique globally.
5. **Is there a single answer, or does it differ per consumer kind?** An external webhook contract and an internal filter spec have genuinely different constraints.

Whatever is chosen is what the next slab decouples ~50 files onto, so the answer needs to be concrete enough to mechanically apply.

Use `/grilling` + `/domain-modeling` + `/codebase-design`. Any new term goes in `backend/CONTEXT.md`.

## Answer

**A State or an Event is named by a bare `String` everywhere outside the Lifecycle.** No value
object, no demoted enum. The names that survive in Java are only those some behaviour depends on
*existing* — nine of them, in three narrow holders — and everything else carries the name as data.

### The population, corrected

The ticket estimated ~50 files and treated them as one job. There are **46**, and they decompose
into three jobs with different risks:

| Job | Size | What slab 07 does |
|---|---|---|
| Named constant references | **35 references across 18 files** | rewrite against a holder constant, or drop the constant entirely |
| Type-only references (fields, signatures, imports) | ~28 files | mechanical `String` swap; the compiler finds every one |
| **State names as raw SQL literals** | **8 literals** in `NetworkStatsRepositoryImpl` | **the compiler finds none of these** — needs a deliberate grep |

The third row is the one this ticket most wants on the record. `NetworkStatsRepositoryImpl` already
names states as string literals inside native and JPQL queries — lines 28, 51, 97, 119, 163, 216 —
so **deleting the four enums produces no compile error there at all**. They are decoupled by
accident today, which means the usual safety net (it doesn't build) does not exist for them. Line
216 is worse than the rest: it filters `nrlr.changed_to` on the *audit* table, which ADR 0008
converts to an FK, so that literal breaks twice, in two different slabs.

`NegotiationRepository:74` (`n.currentState != DRAFT`, a bare JPQL enum reference) *does* break, but
into a string literal rather than into anything a Java constant can reach.

### Decision 1 — the type is a bare `String`

A value object (`StateName` / `EventName`) was the serious alternative and is rejected on a single
argument: **a value object's worth is validation, and after ADR 0002 there is nothing to validate
against.** A name is valid only relative to a Definition Version — `CHECKING_AVAILABILITY` is a real
State in one family and meaningless in another — and the value object does not hold the pin, so the
only check it could make is "non-blank, uppercase". That charges a JPA `AttributeConverter`, friction
in every Criteria predicate, and 46 files of ceremony for a check that never catches the real bug.

Every boundary is already a string: `@Enumerated(EnumType.STRING)` today, an authoritative VARCHAR
after ADR 0009, JSON strings on the wire, and ticket 02's holder is already string-valued.

Demoted enums as a fixed *vocabulary* were rejected for the reason ticket 02 already rejected a
demoted `NegotiationState`: it preserves the closed-universe assumption ADR 0002 exists to delete,
and invites all 46 files to keep depending on it so the decoupling never happens.

**Accepted cost:** no compile-time separation of a State name from an Event name. The two rarely
meet in one signature, and this is the price of decision 1.

**Sub-question 4 (names or ids) is settled by this plus ADR 0009:** names. The natural key is the
Definition Version Pin plus the state name; ids appear only in ADR 0008's audit FK conversion, which
is a different table and a different slab.

### Decision 2 — literals are narrow and behaviour-owned

**Ticket 02's `WellKnownNegotiationStates` survives.** It does not fold into a repo-wide holder,
because a holder declaring all 8 Negotiation States, 12 Resource States and both Event sets is the
enum again with worse ergonomics: it restates the closed universe in Java, it has to be kept in sync
with ADR 0009's seed, and it could not be total anyway — the 8 SQL literals are unreachable from a
Java constant.

The rule is: **a name earns a constant only when some behaviour depends on that name existing.**
Everything else — the filter's `List<String>`, statistics' `Map<String, Integer>` keys, DTO fields,
timeline text — carries the name as data from the column, the request or the pin, with no constant.

Contents, derived from all 35 references rather than assumed:

| Holder | Names | Call sites |
|---|---|---|
| `WellKnownNegotiationStates` | `DRAFT`, `SUBMITTED`, `IN_PROGRESS`, `DECLINED`, `ABANDONED` | `NegotiationSpecification:33,49`; `ResourceServiceImpl:104,162,166`; `NegotiationServiceImpl:373,393`; `NegotiationDTO:87-89`; `NegotiationModelMapper:96`; `Negotiation:237`; `NegotiationInProgressHandler:31`; `NegotiationSubmissionHandler:38`; `NewNegotiationHandler:43`; `ResourceNotificationService:47`; `NegotiationStateChangeWebhookMappingStrategy:23-24`; `NewNegotiationWebhookMappingStrategy:22`; `NegotiationStatusChangeHandler` |
| `WellKnownResourceStates` *(new)* | `SUBMITTED`, `REPRESENTATIVE_CONTACTED`, `REPRESENTATIVE_UNREACHABLE` | `NonRepresentedResourcesHandlerImpl:33,35`; `PendingNegotiationReminderHandler:68`; `ResourceServiceImpl:88,93`; `ResourceNotificationService:57,60`; `NegotiationTimelineImpl:46,48`; `UpdateResourcesDTO:21`; `Negotiation:226` |
| `WellKnownResourceEvents` *(new)* | `OVERRIDE` | `ResourceServiceImpl:193` |

Three things worth noting about that table:

1. **`WellKnownNegotiationStates`' five names are exactly ticket 02's five.** That is an independent
   confirmation, not an inheritance — this sweep enumerated every constant reference in the codebase
   and the Negotiation-scope set came out identical. Ticket 02's holder needs no revision.
2. **`WellKnownResourceStates` is new and the divergence hazard is real there.** Ticket 02 could
   dismiss divergence for Negotiation scope because ADR 0004 keeps a single definition. Resource
   scope is precisely the scope that diverges, and the two query-driven sites ticket 02 handed here
   name Resource States. This is noted, not solved — see decision 5.
3. **`OVERRIDE` needs no new vocabulary, and this was checked rather than assumed.** A parallel
   **Well-known Event** term was drafted and then rejected: the population is exactly one, and that
   one already has a glossary entry — the **Override Event**. More importantly it is not the same
   *kind* of dependency. `Well-known State` names a fragile bet on a family's vocabulary (a custom
   family may simply have no `DRAFT`), whereas ADR 0002 makes the Override Event structural to the
   model — "that is how the Override Event survives as a name under which an admin's direct state
   change appears in history" — so every Definition Version is expected to carry it. Naming
   `OVERRIDE` in Java references a modelled concept rather than gambling on a name.
   **No new term in `backend/CONTEXT.md`.** If a second, genuinely fragile Event dependency ever
   appears, the term earns its place then.

`UpdateResourcesDTO:21` deserves a flag for slab 07: it is a **default value**
(`state = NegotiationResourceState.SUBMITTED`), not a comparison. A default that names a Resource
State is wrong for any family that lacks it — the same shape of problem as decision 5, in an API DTO.

### Decision 3 — `?status=` is validated against the definition rows

Today's **400 is preserved**, but the check becomes "is there a State row with this name in the
Negotiation-scope active version?" — one indexed lookup, not a Java list.

Two findings made this a free and cheap choice rather than a forced one:

- **`NegotiationStatusConverter` is dead code.** `WebConfig.addFormatters` registers
  `NegotiationEventConverter`, `NegotiationRoleConverter` and `NegotiationResourceEventConverter` —
  **not** the status converter. Its only reference anywhere is `unit/converters/ConverterTest`,
  which instantiates it directly. Today's 400 comes from Spring's default enum binding, so the class
  deletes with the enum at zero cost and the ticket's framing of it as a consumer is obsolete.
- **The filter's 400 is pinned by nothing.** `NegotiationControllerTests` pins `sortBy=UNK` and
  `sortOrder=UNK` as 400s; **no test anywhere exercises `?status=UNKNOWN`**, in the parity suite or
  outside it.

Dropping validation (unknown name silently matches nothing, 200 with an empty page) was the honest
alternative and is rejected because ADR 0004 keeps **one** Negotiation definition, which makes this
universe well-defined and total — so the API *can* know, and turning a typo into an empty result
would be a behaviour regression *and* a third intended-delta carve-out against standing decision 1,
which the map says must be deliberate rather than a side effect. Validating against a fixed Java
vocabulary was rejected as decision 2 in miniature.

Note the check's honest limit: it catches typos, not category errors. A name that exists in some
other family but not the one a Negotiation runs still yields an empty result.

### Decision 4 — the `DRAFT` visibility rule stays a name

`NegotiationSpecification:33,49` keeps excluding `WellKnownNegotiationStates.DRAFT`, so the
predicate stays a single-column comparison on the hot negotiation-list query.

**The tempting generalisation is wrong, and the freeze slab already proved it.**
"Hide the initial State" would be elegant and would fail closed — but `negotiation-graph-v1.json`
records `"initialState": "SUBMITTED"`, and **no Transition targets `DRAFT`** at all; something
outside the Lifecycle writes it at creation. That finding is tagged "(Tickets 01, 03)" in
[before-picture-findings.md](../before-picture-findings.md) §2, routed here deliberately. Applied
literally it would hide `SUBMITTED` and **reveal drafts** — exactly backwards.

A dedicated State flag alongside `initial`/`terminal` was the real alternative. Rejected on two
counts: it amends ADR 0002, which enumerates State's fields and is a binding constraint of this map;
and it converts a column comparison into a join to `state` through the pin, inside the main
negotiation list query.

**Recorded hazard — this one fails open.** Ticket 02 established that a missing Well-known State is
a silent no-op, which is benign for notification (nobody is told). Here it is not: a missing `DRAFT`
means the exclusion excludes nothing and unsubmitted negotiations become visible to representatives
and network viewers. Stage 1 is safe by construction — ADR 0004 keeps one Negotiation definition and
ADR 0009's seed is a faithful transcription, so `DRAFT` is present — and the hazard only becomes
real if Negotiation scope ever gains a second family. It is recorded here rather than ticketed
because there is no stage in this map's destination where that can happen.

### Decision 5 — network KPI names stay in SQL; the gap becomes a stage-2 ticket

The four network statistics each define a business metric by naming Resource States:

| KPI | Definition | Semantic |
|---|---|---|
| "Ignored" | `current_state` is `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE` | nobody ever responded |
| "Successful" | `currentState = 'RESOURCE_MADE_AVAILABLE'` | terminal success outcome |
| Active representatives | audit `changed_to` is **not** those two spawn states | a human actually moved it |
| Status distribution | `n.currentState != 'DRAFT'` | Negotiation scope — decision 4 covers it |

Stage 1 **leaves the SQL untouched**: behaviour identical, parity-safe, zero churn, and correct
while one seeded Resource family exists.

Making them structural now — a success/outcome flag on the State row — was rejected because that
*is* **outcome-sensitive conclusion**, which the map's Out of scope section already rules out
("excluded, not foreclosed"). Reopening it here would be a scope change smuggled in through a
statistics query.

But unlike decision 4, this gap becomes real the moment stage 2 ships, because Resource families are
exactly what diverges — so it gets a ticket rather than a hazard note:
**[10 Network KPIs name Resource States a custom family need not have](10-network-kpis-name-resource-states.md)**.

### Decision 6 — uniform `type: string` in OpenAPI

Every State and Event field becomes a plain string in the published schema, keeping
`@Schema(example = ...)` for discoverability. **The wire format is identical** — the enums already
serialise as JSON strings — so no webhook subscriber breaks; only the `enum: [...]` constraint is
lost, and no test anywhere asserts the schema.

**Sub-question 5 is therefore answered in the negative: the external contract does not need a
different answer.** A per-scope split was considered — Negotiation-scope fields could honestly keep
`allowableValues` since ADR 0004 keeps one definition, while Resource-scope fields could not — and
rejected as two conventions to maintain plus a hand-written list that drifts from ADR 0009's seed.
Generating the list from the definition rows at startup was rejected because it makes the published
API contract deployment-specific.

Consumers wanting the live set of valid values are pointed at the metadata endpoints, whose contract
is ticket [04](04-global-state-event-metadata-contract.md)'s to settle.

### Routed to ticket 04, not decided here

**Enum declaration order is load-bearing and reaches the frontend.**
`NegotiationResourceState`'s comment ("the order of the individual values is important. The most
advanced state (final state) is at the bottom") is not decoration: `ResourceStateMetadataDto:22`
publishes `value.ordinal()`, `frontend/src/views/NegotiationPage.vue:364-367` rolls a multi-resource
organization up to whichever state has the **highest ordinal**, and
`frontend/src/components/OrganizationCard.vue:71` sorts a state list by it. ADR 0002's `State`
carries `label` + `initial` + `terminal` and **no order column**, so deleting the enum deletes an
ordering the UI depends on.

It is metadata rather than identity, and ticket 04 already owns `ResourceStateMetadataDto`, so it
goes there rather than being decided here. Flagged because ticket 04's text does not know about it:
it asks where `description` comes from and does not mention `ordinal` at all.

### Consequences for the rest of the map

- **Slab 07 owes a manual sweep for SQL literals.** The compiler cannot find the 8 in
  `NetworkStatsRepositoryImpl`, so "it builds" is not evidence that slab is complete. Added to the
  map's fog as an explicit obligation.
- **Slab 07's job is now sized and sliceable in three parts** — 18 files needing holders, ~28
  mechanical, 8 invisible — which is what tickets 03, 04 and 06 were blocking.
- **Ticket 04 gains the `ordinal` ordering contract** on top of the questions it already carries.
- **`NegotiationStatusConverter` and its `ConverterTest` are deletions, not migrations.**
- **Ticket 02's holder is confirmed, not revised** — its five names are exactly right.

One new ticket ([10](10-network-kpis-name-resource-states.md)). Resolving this unblocks ticket
[04](04-global-state-event-metadata-contract.md).
