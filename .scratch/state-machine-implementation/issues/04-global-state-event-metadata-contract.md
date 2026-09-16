# The global state and event metadata contract

Type: grilling
Status: resolved
Blocked by: 03

## Question

Three DTOs enumerate States and Events **globally**, as a fixed universe:

- `negotiation/dto/NegotiationStateMetadataDto` — `NegotiationState value`, `String label`, `String description`
- `governance/resource/dto/ResourceStateMetadataDto`
- `governance/resource/dto/ResourceEventMetadataDto`

Once Resource Lifecycles can run different Definition Families, **a global list is no longer well-defined.** There is no single set of Resource States; there is one set per Definition Version, and two Resources in one Negotiation may run different ones (ADR 0004). What do these endpoints return?

In stage 1 a total answer exists — one seeded family per scope, so "the states of the Global Default Family's active version" is correct and complete. The decision is what **contract** to commit to now, given stage 2 makes that answer wrong.

### Sharpen at least

1. **Who consumes these, and for what?** Find out before designing — most likely a frontend filter dropdown or a status legend. If it is a filter vocabulary, the honest answer may be "the union across all families", which is a different thing from "one definition's graph".
2. **Global, per-family, or per-negotiation?** A per-`Negotiation` endpoint is always well-defined (its resources' pinned versions are known). A per-family endpoint is well-defined but pushes family selection onto the caller. A global union is well-defined but describes no actual graph, so it can list two states that never coexist.
3. **Where does `description` come from?** ADR 0002 gives `State` a `label` and the `initial`/`terminal` flags — **no description column**. Either the schema needs one (an ADR 0002 amendment, which is a real cost) or the field is dropped, or it is derived. Check what populates it today: the DTO's javadoc references `eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata`, a class that **may no longer exist** — verify before designing.
4. **Does the answer change per Definition Scope?** The Negotiation Lifecycle keeps a single definition, so its metadata endpoint stays globally well-defined and may not need to change at all. Only the two Resource DTOs have the problem.
5. **Is a breaking API change acceptable here?** The predecessor map's destination said REST/vocabulary compatibility is "preferred but not a hard constraint", and stage 1's gate is characterized parity with two named carve-outs — a third carve-out needs to be a deliberate decision, not a side effect.

### Why it is separate from ticket 03

Ticket 03 decides the *type* that names a State. This ticket decides whether an endpoint that enumerates a *universe* of States still makes sense. Ticket 03's answer constrains this one but does not settle it — and these three DTOs are explicitly **carved out** of the consumer-decoupling slab for that reason, so that slab is not blocked waiting on this.

Use `/grilling` + `/domain-modeling`.

## Added by ticket 03 — a sixth thing to sharpen

**`ordinal` is a published ordering contract, and deleting the enum deletes it.**

`ResourceStateMetadataDto:22` does `this.ordinal = value.ordinal()` — it publishes the enum's
**declaration order**, which `NegotiationResourceState`'s own comment says is load-bearing ("the
order of the individual values is important. The most advanced state (final state) is at the
bottom"). Two frontend consumers depend on it:

- `frontend/src/views/NegotiationPage.vue:348-367` builds a `state → ordinal` map and rolls a
  multi-resource organization up to **whichever state has the highest ordinal** — i.e. it uses the
  ordering as "how far along is this organization".
- `frontend/src/components/OrganizationCard.vue:71` sorts a state list by it.

**ADR 0002's `State` carries `label` plus `initial` and `terminal` flags — there is no order
column.** So this joins `description` (sub-question 3) as a field the schema cannot currently
supply, with the difference that `description` is presentational while this one drives a
**computed status**: lose it and the organization roll-up silently picks an arbitrary state.

Note it is a *progress* ordering, not a graph ordering — it cannot be derived from the Transitions,
because a graph with branches has no total order. So "derive it" is likely not available; the real
options are a column (an ADR 0002 amendment, same cost as sub-question 3's), dropping it and
changing the roll-up rule, or moving the roll-up server-side.

Routed here by ticket [03](03-state-event-identity-downstream.md) rather than decided there: it is
metadata, not identity, and this ticket already owns `ResourceStateMetadataDto`.

## Answer

**The endpoints stay, and stay global — but "global" stops meaning "the enum" and starts meaning
"the active Definition Version of this Definition Scope's family".** Metadata itself is no longer
asked for globally at all: every caller that holds a Negotiation or a Resource asks *through that
work item*, and the Lifecycle reads its Definition Version Pin.

Two deliberate deltas fall out, and only two. Both State collections stay **byte-identical**.

### The population, corrected

The ticket's premises were wrong in four places, and each correction changed an answer.

| The ticket said | Actually |
|---|---|
| "Three DTOs" | **Four.** `negotiation/dto/NegotiationEventMetadataDTO` exists too — already `String`-valued and already fed by the Enum-Backed Lifecycle Catalog, because slab 07 reached it through `NegotiationController` while the other three stayed carved out. |
| "check what populates `description` — `NegotiationStateMetadata` may no longer exist" | It **does not exist**; the javadoc reference is dead. `label` and `description` come from the four enum constructors. |
| "no description column — either the schema needs one, or the field is dropped" | True for `state`, and **worse for `event`: that table is `id, lifecycle_definition_id, name` and has no `label` either** (`V36.1`). For Events *both* published fields are unsourced, not one. |
| "Only the two Resource DTOs have the problem" | **False.** `/negotiation-lifecycle/events` loses `START`, so a Negotiation-scope endpoint changes too. Sub-question 4 is answered in the negative: the answer does *not* stay clean per Definition Scope. |

**Who consumes these — sub-question 1, answered by reading the frontend rather than guessing.**
Five call sites, all in the Vue app, and they are not all legends:

| Endpoint | Consumer | Reads |
|---|---|---|
| `negotiation-lifecycle/states` | `UserPage:75` → `FilterSort:74-86` | `value`, `label` — the `?status=` filter checkboxes |
| `negotiation-lifecycle/states` | `NetworksPage:279` → `NetworkNegotiationsTab` | same filter, network view |
| `resource-lifecycle/states` | `NegotiationPage:463`, roll-up at `:349-367` | `value`, **`ordinal`** |
| `resource-lifecycle/states` | `OrganizationCard:71` → `OrganizationHeader:42` | `value`, `label`, **`ordinal`** |
| `resource-lifecycle/states` | `AddResourcesModal:43` | `value`, `label` |
| `resource-lifecycle/events` | `InformationRequirementsSection:88` → `InfoRequirementModal:53` | `value`, `label` |

Three facts from that sweep:

1. **`/negotiation-lifecycle/events` and all four single-item endpoints have no frontend consumer at
   all.** The single-item ones exist because the assemblers put a `self` link on every collection
   member — they are the other half of a HAL contract, not a feature.
2. **`description` is rendered nowhere.** No consumer reads it. That made dropping it the cheap
   option and is why decision 2 had to be argued on something other than current use.
3. It is a filter vocabulary *and* a computed status *and* an admin authoring vocabulary. The
   ticket's guess ("most likely a frontend filter dropdown or a status legend") was half right, and
   the half it missed — `ordinal` driving a computed organization status — is the one with teeth.

**The read path is six callers wider than the endpoints.** Resolving this also retires slab 07's
deliberate throwaway, `EnumBackedLifecycleCatalog`: `NegotiationController:279`,
`NegotiationLifecycleServiceImpl:62`, `NegotiationModelAssembler:61`,
`ResourceWithStatusAssembler:164`, `ResourceStateChangeHandler:58` (a label rendered into a
notification body) and `KnownNegotiationStateNamesValidator:36` (ticket 03's `?status=` 400).

**The gate this is judged against.** `LifecycleMetadataEndpointsTest` is 14 tests plus four JSON
fixtures pinning all four collections byte-for-byte — including one named *"Collections publish
exactly the enum universe"* asserting 8/8/12/13. Any change here is a parity break by construction,
so every decision below is stated in terms of what it does to that suite.

### Decision 1 — global, redefined as the scope's active Definition Version

Each endpoint answers from **one** Definition Version: the active version of the sole
Negotiation-scope family, or of the Global Default Family for Resource scope. `LifecycleDefinitions`
already returns exactly those two ids from `resolveForNegotiation()` and `resolveForResource()`, so
this needs no new concept.

Per-family endpoints (`/lifecycle-definitions/{familyKey}/states`) and a per-Negotiation endpoint
were both rejected for stage 1: each buys a stage-2 shape now at the cost of rewriting four working
screens, in a slab whose gate is parity and whose standing decision 5 makes frontend repair ride
along. A **global union across families** was rejected outright — it describes no actual graph, and
the organization roll-up would compute a Progress Order over a set no Resource can traverse.

**Explicitly provisional.** Once more than one Resource family is active this answer is no longer
total: it describes the Global Default Family, which is what a Resource with no association runs and
nothing more. Recorded as a stage-2 revisit beside ticket
[10](10-network-kpis-name-resource-states.md), which is the same class of gap.

### Decision 2 — the rows carry the metadata the API already publishes

`event` gains `label` NOT NULL. `state` and `event` both gain `description`, nullable. **ADR 0002 is
amended by this ticket**, not quietly edited — see its Amendments section.

The two gaps were not equally expensive, and that asymmetry decided it. ADR 0002 is *silent* on what
an Event carries, so `event.label` contradicts only slab 08's entity javadoc ("Nothing on an Event
is editable … it is all an Event is"). ADR 0002 *does* enumerate State's fields, so
`state.description` is a real amendment.

**Deriving labels from names is not available.** It would render `Submitted` where the UI says
**"Under review"**, and cannot produce the comma in "Mark as Currently Unavailable, But Willing to
Collect" or the wording of "Override current state". That is visible UI text changing, in a slab
gated on parity.

**Dropping `description` was the serious alternative and is rejected.** It is provably dead on the
wire today — no consumer renders it — which made it the lean answer. But it is authored prose, and
**stage 3 is admin authoring of these families**: an admin describing their own States is exactly
what that stage is for, so dropping it now means removing a field and re-adding it two stages later.
The amendment is legitimate rather than opportunistic: ADR 0002 enumerated State's fields without
noticing that the live REST API already publishes a description for every State, which is the
"implementation surfaces a genuine contradiction" hatch the map's binding constraints name.

### Decision 3 — the ordering is a column called `progress_order`; the wire keeps `ordinal`

New term **Progress Order** in `backend/CONTEXT.md`: how far along its Lifecycle a State sits.
`ordinal` is an enum word for a concept that outlives the enum.

Two findings sharpened what ticket 03 routed here. **`EnumBackedLifecycleCatalog.resourceStateOrdinal`
has no production caller** — its only reference anywhere is `EnumBackedLifecycleCatalogTest:49`.
Slab 07 built it and wrote in its javadoc that it is "replaced by reading the Resource State row's
ordering value", which was an **assumption that such a column would exist**, made in passing and
never decided. And **enum ordinals are contiguous 0–11**, with `RETURNED_FOR_RESUBMISSION` surviving
as a Legacy State at 3, so seeding 0–11 reproduces today's values exactly rather than equivalently.

Ticket 03 already established the ordering cannot be derived from the Transitions. "Move the roll-up
server-side" does not remove the need — it relocates who applies an ordering that must still exist.
No order-free roll-up rule preserves the meaning: `terminal` is the only other ordering-ish flag and
most Resource States are not terminal.

Three sub-decisions:

- **The JSON field stays `ordinal`.** Renaming to `progressOrder` is more honest and buys nothing —
  a fixture rewrite plus `NegotiationPage:350` and `OrganizationCard:71`, for no behaviour change.
- **Seeded for both Definition Scopes, published only where it is published today.** The column sits
  on `state`, so Negotiation States get values (declaration order, 0–7). `NegotiationStateMetadataDto`
  does **not** start publishing it.
- Columns land in `V36.5`, which slab 08 left free.

With decisions 2 and 3 together, **`negotiation-states.json` and `resource-states.json` pass
unchanged**.

### Decision 4 — the Event lists shrink, and that is delta one

ADR 0009 seeds Legacy States as transition-less rows but says dropped *Events* "are fully omitted".
So reading rows costs one member from each Event collection:

| | Today | After |
|---|---|---|
| `/v3/negotiation-lifecycle/events` | 8 | 7 — loses `START` |
| `/v3/resource-lifecycle/events` | 13 | 12 — loses `RETURN_FOR_RESUBMISSION` |
| `/v3/negotiation-lifecycle/events/START` | 200 | 400 |
| `/v3/resource-lifecycle/events/RETURN_FOR_RESUBMISSION` | 200 | 400 |

**Accepted as a deliberate delta**, answering sub-question 5 in the affirmative. The Information
Requirements consequence the user asked for arrives for free: `InfoRequirementModal` is fed by
`/resource-lifecycle/events`, so its dropdown offers 12 without a frontend change — and an
Information Requirement can no longer be authored against an Event that carries no Transition.

Seeding the dropped Events as transition-less rows — a "Legacy Event", symmetric with Legacy State
and with the Override Event — was the alternative. It would have left all four collections
byte-identical and taken the carve-out count to zero, at the cost of amending ADR 0009 as well as
0002 and keeping two dead Events in the admin vocabulary forever. Rejected: the point of omitting
them is that they should not be offerable, and that is worth one delta.

**Carved out of parity:** both Event fixtures; the 8/13 count assertion; and the Event half of
*"Members that sit on no transition are published anyway"* — its State half still holds, because
Legacy States are still published.

### Decision 4b — an Information Requirement naming a dropped Event aborts the cutover

**ADR 0009's reasoning for omitting Events is incomplete, and this is the finding worth the most to
a later slab.** It argues "an Event leaves no data residue, because history records the resulting
State and not the Event that caused it" — and its *very next paragraph* re-homes
`information_requirement.for_event`, a column that holds Event names. That column is
`VARCHAR(255)` with no constraint (`V11.0:5`), and the admin screen that writes it offers all 13
Resource Events. So a row naming `RETURN_FOR_RESUBMISSION` is possible, matches no Event row, gets a
null FK, and then loses `for_event` to the drop.

Checked, so that the rest of 0009's reasoning is not reopened: the audit tables carry `changed_to`,
a State, and no Event column (`B1:132-138`). **`information_requirement.for_event` is the single
place in the schema where an Event name is stored.**

**The migration slab adds a pre-flight assert** that aborts naming the offending rows, consistent
with ADR 0009's own ordering ("pre-flight asserts first, raising before anything destructive so a
surprise value aborts with the snapshot intact"). Deleting the rows in the cutover was rejected: a
requirement on a transition-less Event gates nothing, but it is *listed on an admin screen*, so
deletion silently removes rows a person created. Re-homing onto a surviving Event is fabrication.
Unlike the States case 0009 argued about, this data is admin-created and expected to be absent —
ticket 06's rehearsal establishes that cheaply.

### Decision 5 — metadata is read through the work item, which reads its Definition Version Pin

`LifecycleDefinitions` gains a fifth method rather than a second public type, keeping its javadoc's
claim ("this is the whole of the package's public surface") true. It reuses `DefinitionVersionLoader`,
which already reads both vertex tables in one transaction, so no query is duplicated.

**Two ways in, because there are two questions:**

| Question | Entry point | Version |
|---|---|---|
| Describe this Negotiation's / this Resource's States and Events | `metadataForNegotiation(id)`, `metadataForResource(negotiationId, resourceId)` | reads the **Definition Version Pin** |
| Describe what new work will run under | `metadataForNewNegotiations()`, `metadataForNewResources()` | resolves the **active** version |

The second row is decision 1, now with exactly one home — and it is the one place stage 2 revisits.
It serves the four global endpoints and `KnownNegotiationStateNamesValidator`, which has no work item
to name because it filters a list of all Negotiations.

**The shape costs nothing at any call site**, which is what settled it: five of the six catalog
callers already hold what they need to pass — `ResourceStateChangeHandler` has
`event.getNegotiationId()` and `getResourceId()`, `ResourceWithStatusAssembler` has
`entity.getNegotiationId()` and `getSourceId()`, the other three hold a Negotiation id. No caller
acquires anything new, and the pin read joins a per-work-item lookup those callers already do
(`getPossibleEvents(...)`), so it introduces no new class of N+1 — it lands inside the
`ResourceWithStatusAssembler` batching the map already lists as follow-on.

**Labels do not go on the `CompiledGraph`.** Tempting — the existing version-keyed cache would serve
them free. Rejected on two counts: the Transition Evaluator never needs a label, so it widens a deep
module's interface with a question it does not answer; and `backend/CONTEXT.md` already draws the
line ("Compiled Graph … Never a source for admin tooling").

### Decision 5b — the rule, which outlives this ticket

> **A call made in the context of a Negotiation or a Resource must read that Lifecycle's Definition
> Version Pin. It must never resolve the active Definition Version.**

Stated as a constraint, not a preference, and added to `backend/CONTEXT.md` under **Definition
Version Pin** — whose entry said what the pin *is* but not that reading it is mandatory for work in
flight. A caller that resolves the active version instead relabels work already running the moment a
second Definition Version is published, which is precisely what ADR 0003 exists to prevent.

### Decision 6 — enforced by naming only

No guard test. The two entry-point pairs are named so the mistake reads wrongly — a caller holding a
Negotiation reaching for `metadataForNewNegotiations()` should look wrong — and both resolving
methods state the rule in their javadoc.

**Recorded hazard, since this was the argued-against option.** The failure is silent and
stage-delayed: in stage 1 a caller that resolves instead of reading the pin behaves identically,
nothing goes red, and the defect surfaces only when a second Definition Version is published, in a
different stage and a different slab. A guard test over `resolveForNegotiation()` /
`resolveForResource()` callers was the recommendation and was declined; it remains available at any
time, and its allowlist is short — the two metadata entry points, the filter validator, and the two
sites that write a pin.

### Decision 7 — the four single-item endpoints stay, and match case-insensitively

They are kept: dropping them would leave the `self` link on every collection member pointing at
nothing, which is a worse contract than an unused endpoint.

**Delta two: the lookup becomes case-insensitive.** Today `/events/contact` answers 200 (a converter
upper-cases it) while `/states/submitted` answers 400 (Spring's enum binding is case-sensitive).
Once a State path variable is a bare `String` the asymmetry has to be reproduced deliberately or
removed deliberately; it is removed.

**Match case-insensitively against the names in the Definition Version — do not `.toUpperCase()`.**
Every name is upper-case today only because an enum constant made it so. A Definition Family an
admin authors in stage 3 may name a State `Ready`, and upper-casing turns `/states/Ready` into
`READY`, which matches nothing. Same cost, same result today, no assumption that stops being true.

**A latent defect this surfaced, filed not fixed:** `NegotiationController.lifecycleEventNamed` and
both controllers' `eventNamed` helpers already do `.toUpperCase()` — on the path that *fires* Events.
Same problem, wider blast radius, and it belongs to whichever slab owns those controllers at cutover.

**One parity assertion is relaxed rather than carved out.** `assertUnrecognisedStateIsRejected`
asserts `detail` starts with `"No enum constant "`, while its own javadoc says it deliberately pins
only "the status, the content type, the problem shape, and the fact that the detail is derived from
the rejected name". The `startsWith` goes further than that sentence and is enum-specific.
Correcting the assertion to match its javadoc keeps it parity; leaving it would manufacture a third
delta out of a test that already intended not to have one.

`caseHandlingOfSingleItemEndpoints_isPinned` **moves to the intended deltas** and asserts 200 on all
four paths. Leaving case handling untested was considered: normalising is code someone writes, not
behaviour that arrives, and untested code existing only to be lenient is what a later cleanup deletes
as unused. Four one-line assertions are cheap insurance.

### What this hands the cutover slab

- **`EnumBackedLifecycleCatalog` is a deletion**, along with its test, once the fifth
  `LifecycleDefinitions` method exists and its six callers move.
- **`V36.5` carries three columns** — `event.label` NOT NULL, `state.description`,
  `event.description`, `state.progress_order` — and ADR 0009's seed must populate all of them, with
  Progress Order 0–11 for Resource States and 0–7 for Negotiation States.
- **The intended-delta count for these endpoints is two**, and `parity-gate.md` needs both: the Event
  collections losing one member each, and case-insensitive single-item lookup.
- **The `ordinal` JSON field survives the enum**, so no frontend change is needed for decisions 1–3.
- **No frontend change is needed for decision 4 either** — the IR dropdown shrinks because its source
  shrank.

### Consequences for the rest of the map

- Ticket 03's routed `ordinal` question is **answered with a column**, and its guess that the schema
  "cannot currently supply" it is now an amendment rather than an obstacle.
- The migration slab gains **one pre-flight assert** with a stated trigger (4b).
- Stage 2 gains **one revisit** — decision 1's global answer stops being total when a second Resource
  family goes active.
- Stage 3 gains **three authorable fields** its authoring surface must collect, and the
  case-insensitivity decision means it may name States in any case it likes.
- Two new `backend/CONTEXT.md` entries: **Progress Order**, and the pin-reading rule on **Definition
  Version Pin**.
