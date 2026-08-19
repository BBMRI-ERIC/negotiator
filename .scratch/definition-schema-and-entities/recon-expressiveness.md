# Recon: can ADR 0002/0003's schema express both live graphs verbatim?

Read-only recon for ticket [08 Definition schema and entities](../state-machine-implementation/issues/08-definition-schema-and-entities.md).
Nothing here is a design proposal; nothing was modified.

## Sources checked

| Source | What it is |
|---|---|
| `backend/src/test/resources/lifecycle/negotiation-graph-v1.json` | mechanical dump, 117 lines |
| `backend/src/test/resources/lifecycle/resource-graph-v1.json` | mechanical dump, 230 lines |
| `backend/src/test/resources/lifecycle/graphs-v1.mmd` | rendered from the two JSON files only |
| `backend/src/test/java/eu/bbmri_eric/negotiator/characterization/dump/LifecycleGraphDumper.java` | the generator — read to establish what the dump does *not* capture |
| `backend/docs/adr/0002…`, `0003…`, `0005…`, `0007…`, `0009…` | the schema and seed contracts |
| `backend/src/test/resources/characterization/rest/{negotiation,resource}-{states,events}.json` | committed published metadata — the only committed source of `label`, `description`, `ordinal` |
| `.scratch/state-machine-implementation/before-picture-findings.md` | Parts 3 and 7 in full; findings 1, 2, 6, 7, 8, 9, 10, 11, 14, 16, 17 read |

## Verified counts (computed from the JSON, not transcribed)

| | Negotiation | Resource | Total |
|---|---|---|---|
| `states` | 8 | 12 | **20** |
| `events` (dump array — transition-derived) | 7 | 11 | **18** |
| `transitionCount` | 8 (`:24`) | 13 (`:32`) | **21** |
| transitions with `"guard": null` | 8 | 13 | **21 / 21** |
| transition Actions | 3 | 0 | **3** |
| transitions with `securityRule: null` | 6 | 0 | 6 |
| distinct `kind` values | `{EXTERNAL}` | `{EXTERNAL}` | 1 |
| `securityRule` entries with ≠1 attribute | 0 | 0 | **0** |
| duplicate `(source, event)` pairs | 0 | 0 | **0** |
| `initialState` | `SUBMITTED` (`:4`) | `SUBMITTED` (`:4`) | 1 each |

Authority values in the dump, exhaustively: `null` ×6, `ROLE_ADMIN` ×2 (`negotiation-graph-v1.json:91`, `:108`),
`isRepresentative` ×8, `isAdmin` ×3, `isCreator` ×2. Five distinct values, nothing else.
Every `securityRule` also carries `"comparisonType": "ANY"` and `"expression": null`.

Published metadata universes (wider than the dump, cross-checked):
`negotiation-events.json` = 8 (adds `START`); `resource-events.json` = 13 (adds `OVERRIDE`,
`RETURN_FOR_RESUBMISSION`); `negotiation-states.json` = 8 and `resource-states.json` = 12, matching the dump.

## Part A — Element-by-element mapping

### A1. Definition-level (nothing in the dump maps here)

| Dump element | Table.column | Note |
|---|---|---|
| `graph` (`"negotiation"` / `"resource"`) | `state_machine_definition.scope` ∈ `NEGOTIATION`/`RESOURCE` (ADR 0002:12) | 1:1 |
| `beanName` | — | drops at cutover, no meaning after SSM deletion |
| *(absent)* | `.family_key`, `.name`, `.version`, `.active`, `.is_global_default` (ADR 0003:12–16; ticket 08 §Schema) | seed-authored; the dump carries none of it. Not a gap — versioning is not a property of a Java builder chain. |

### A2. States — all 20

`State.name` ← each element of `$.states[*]`. `State.initial` ← `true` iff the name equals `$.initialState`
(exactly one per graph, so ADR 0002's "exactly one initial State per Definition Version" holds verbatim).

| State column | Held by | Source |
|---|---|---|
| name | ✅ `state.name` | `$.states[*]` |
| `initial` | ✅ `state.initial` | `$.initialState` — `SUBMITTED` in **both** graphs |
| `terminal` | ⚠️ `state.terminal` exists, **the dump carries no terminal information at all** | see gap 3 |
| `label` | ⚠️ `state.label` exists (ADR 0002:12), **the dump carries no label** | see gap 4 |
| `description` | ❌ no column, by design (ticket 08 §Out of scope) | see gap 5 |
| `ordinal` | ❌ no column (ADR 0002:12 enumerates label + initial + terminal only) | see gap 6 |

### A3. Events — 18 in the dump, 21 in the declared universe

`Event.name` ← each element of `$.events[*]`. The dumper derives that array from the transitions
(`LifecycleGraphDumper.java:80`, `graph.put("events", eventsOf(transitions))`), so it is by construction the
*triggering* universe, not the declared one. ADR 0002 gives `Event` no `label` and no `description` column;
the published metadata has both for all 21. See gaps 5 and 7.

### A4. Transitions — all 21

| Dump field | Table.column |
|---|---|
| `source` | ✅ `transition.from_state_id` → `state.id` |
| `target` | ✅ `transition.to_state_id` → `state.id` |
| `event` | ✅ `transition.event_id` → `event.id` |
| `securityRule` | ⚠️ `transition.required_authority` (ADR 0002:18) — see gaps 1 and 2 |
| `kind` = `EXTERNAL` (21/21) | ❌ no column — **not a loss**: single-valued, and an SSM concept ADR 0001 deletes |
| `securityRule.comparisonType` = `ANY` (15/15) | ❌ no column — **not a loss**: single-valued, and finding 9 shows `ALL` was never in effect (SSM 4.0.0 drops the argument) |
| `securityRule.expression` = `null` (15/15) | ❌ no column — **not a loss**: never set |
| `guard` = `null` (21/21) | ✅ expressible as zero rows in the Guard-wiring table |
| `actions` (3 non-empty) | ✅ `action_wiring.transition_id` + `.type` + `.params` (jsonb) + `.order` |

**Required-authority mapping, literal.** The five dump values map onto ADR 0002:18's enum with nothing left over:

| Dump value | Count | `required_authority` |
|---|---|---|
| `ROLE_ADMIN` | 2 | `IS_ADMIN` |
| `isAdmin` | 3 | `IS_ADMIN` (ADR 0002:18 states the two spellings were verified identical; finding 7 refines this to "the token authority, not the `admin` column") |
| `isRepresentative` | 8 | `IS_REPRESENTATIVE` |
| `isCreator` | 2 | `IS_CREATOR` |
| `null` | 6 | `NONE` — **literally, but not behaviourally**; see gap 1 |

**No value fails to map, and `SYSTEM` is used by nothing in v1.**

### A5. Actions — 3, all Negotiation-scope

| Transition | dump `actions` | JSON | Expressible as |
|---|---|---|---|
| `DRAFT --SUBMIT--> SUBMITTED` | `EnablePublicPostsAction` | `negotiation-graph-v1.json:34` | one `action_wiring` row, `type = SET_POST_VISIBILITY`, params `{scope, flag}` (ADR 0002:14 names this collapse explicitly) |
| `SUBMITTED --APPROVE--> IN_PROGRESS` | `EnablePrivatePostsAction` | `:98` | same |
| `IN_PROGRESS --ABANDON--> ABANDONED` | `DisablePostsAction` | `:45` | same |
| other 18 transitions | `[]` | | no rows |

Each carries exactly one Action, so `order` is trivially 1 and inter-Action ordering is unexercised.
Two things the dumper structurally cannot have missed and did not find: state entry/exit Actions
(`describeTransition` at `LifecycleGraphDumper.java:204-214` reads transition Actions only — but a grep of
`negotiation/state_machine/**` for `stateEntry|stateExit|entryAction|exitAction|.state(` returns **zero hits**,
so there are none to miss), and orphan transition fragments (rendered as an explicit
`%% unattached transition fragment` Mermaid comment at `:139-150`; `graphs-v1.mmd` contains none).

### A6. Guards — zero wiring rows in v1

All 21 transitions record `"guard": null`. The Guard-wiring table (nullable `transition_id`, two partial
unique indexes on `order`) is therefore **expressible but exercised by zero rows derived from the dump**.
ADR 0009:16 nonetheless seeds one definition-level `NEGOTIATION_APPROVED` Guard on the Resource family. See gap 8.

---

## Part B — Gaps and contradictions

### 1. The six unsecured Negotiation transitions are not `NONE`; they are `IS_ADMIN OR IS_CREATOR` — **(a) genuine ADR 0002 contradiction**

The dump's `securityRule: null` is a statement about the *state machine bean*, not about who may fire.
`NegotiationLifecycleServiceImpl.java:93-96` runs a blanket check **before** any rule is consulted:

```java
if (!roles.contains("ROLE_ADMIN")
    && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId)) {
  return Set.of();
}
```

so `SUBMIT`, `PAUSE`, `UNPAUSE`, `CONCLUDE` and both `ABANDON`s are today firable by **admin or the
Negotiation's creator, and by nobody else**. `required_authority` is a single enum value (ADR 0002:18) and has
no disjunction: `NONE` widens these six to every authenticated caller (any user could abandon any
Negotiation), `IS_CREATOR` narrows them by removing the admin, `IS_ADMIN` removes the creator. **No value of
the enum reproduces today's behaviour for these six transitions.** The two secured ones are unaffected —
`IS_ADMIN` is exactly right for `APPROVE`/`DECLINE`, since a non-admin creator passes the blanket check and
then fails the `ROLE_ADMIN` rule.

Half-recorded upward: before-picture finding 7 (lines 226–268) documents the blanket check and the resulting
"an admin and the creator are offered identical sets" in all six other States, and Part 7 owes a decision on
"whether 'may not see it' stays indistinguishable from 'is not there'". **It does not state the schema
consequence**, and no Part 7 row covers the missing enum value. Per ticket 08 §Note this is a decision ticket,
not a schema addition.

Two smaller variants of the same shape, recorded for completeness rather than as separate gaps:
`ResourceLifecycleServiceImpl.isSecurityRuleMet` (`:156-179`) evaluates its three rules in a fixed
`isCreator` → `isRepresentative` → `isAdmin` `if/else` chain — harmless because 0 transitions carry more than
one attribute — and its `isAdmin` branch returns `true` when `SecurityContextHolder`'s authentication is
`null`, which `IS_ADMIN` does not reproduce.

### 2. `SYSTEM` is unexercised by the dump, and `CONCLUDE`'s authority is a live choice — **(c) the seed must decide; the schema expresses either**

No transition in either graph carries a `SYSTEM`-shaped authority. Today's automatic conclusion fires
`CONCLUDE` through `runAsSystemUser` (finding 14), *and* the same Event is offered to admin/creator from
`IN_PROGRESS` (finding 7). ADR 0007:20 makes conclusion "a System Event plus a `TERMINAL_AGGREGATION` Guard".
Seeding `CONCLUDE` as `SYSTEM` removes a human capability that exists today; seeding it as anything else
contradicts ADR 0007. The column holds either value — this is a seed decision, and it is adjacent to but not
identical with Part 7's "whether conclusion widens beyond two States".

### 3. `terminal` is absent from the dump, and the two available derivations disagree — **(c), with (b) evidence already on record**

The dumper writes no terminal field anywhere (`LifecycleGraphDumper.java:66-82`). Derivation by
"has no outgoing transition" yields **4 + 4**:

- Negotiation: `ABANDONED`, `APPROVED`, `CONCLUDED`, `DECLINED`
- Resource: `RESOURCE_MADE_AVAILABLE`, `RESOURCE_NOT_MADE_AVAILABLE`, `RESOURCE_UNAVAILABLE`, `RETURNED_FOR_RESUBMISSION`

But before-picture finding 6 (lines 203–225) establishes by walking every declared State that today's
conclusion predicate counts **exactly two** Resource States — `RESOURCE_MADE_AVAILABLE` and
`RESOURCE_UNAVAILABLE` — and explicitly *not* `RESOURCE_NOT_MADE_AVAILABLE` or
`RETURNED_FOR_RESUBMISSION`. Since ADR 0007:20 has `TERMINAL_AGGREGATION` read the `terminal` flag,
flagging the graph-derived 4 **widens conclusion**, which finding 6 calls "a behaviour change, not a faithful
reproduction". Two documents disagree and both were checked:

- `resource-graph-v1.json` (structure): 4 states have no outgoing transition.
- before-picture-findings.md:205 (behaviour): "**Answer: confirmed. `RESOURCE_MADE_AVAILABLE` and
  `RESOURCE_UNAVAILABLE`, and nothing else.**"

Sub-question the seed inherits: whether the two Legacy States are `terminal`. ADR 0009:20 says only that
"nothing flags them" as legacy — it does not say what `terminal` is for them. Part 7 already owns "whether
conclusion widens beyond two States"; **this brief's addition is that the `terminal` column is where that
decision now physically lands**, and that the Negotiation-side terminal flag is constrained by nothing
observable at all.

### 4. `label` is absent from the dump but exists in committed published metadata — **(c), and it is load-bearing**

`State.label` is required by ADR 0002:12; the dump has no label field. All 20 labels *are* committed, in
`characterization/rest/{negotiation,resource}-states.json` (`"label": "Access Conditions Indicated"` etc.),
and before-picture finding 16 records that `ResourceStateChangeHandler` builds its notification body from
those labels, so **"ADR 0009's seed has to carry those labels or this notification degrades to `null`s in a
sentence."** So the information exists, in a different committed artifact than the one the ticket names as
the oracle. No contradiction; the seed's source for `label` is `resource-states.json` /
`negotiation-states.json`, not the graph dump.

### 5. `description` exists on all 20 States and all 21 Events and has no column — **(b) already routed, to ticket 04**

Every entry in all four metadata fixtures carries a `description`. ADR 0002:12 gives `State` label + flags
only, and ticket 08 §Out of scope says a `description` column "would be an ADR 0002 amendment, and its cost is
assessed there [ticket 04], not assumed here". Ticket 04 sub-question 3 owns it verbatim. **Not a new finding.**

### 6. `ordinal` (0–11 on Resource States) has no column and cannot be derived — **(b) already routed, to ticket 04**

`resource-states.json` publishes `ordinal` on all 12 Resource States; `negotiation-states.json` does not.
Before-picture finding 17 records it as a published contract and ticket 03's "Routed to ticket 04, not decided
here" section records that two frontend consumers depend on it
(`NegotiationPage.vue:348-367` rolls a multi-resource organization up to the **highest** ordinal;
`OrganizationCard.vue:71` sorts by it), and that it is a *progress* ordering that "cannot be derived from the
Transitions, because a graph with branches has no total order". **Not a new finding.**

### 7. Three declared Events carry no Transition; the dump omits all three — **(c)/(b)**

Before-picture finding 11 (lines 343–359) names them: `START` (Negotiation), and `RETURN_FOR_RESUBMISSION` and
`OVERRIDE` (Resource). Confirmed independently here: the dumper derives `$.events` from transitions
(`LifecycleGraphDumper.java:80`), and the metadata fixtures carry 8 and 13 against the dump's 7 and 11.

**The schema can hold them.** `Event` has no NOT NULL back-reference to `Transition`; an Event row that no
`transition.event_id` cites is structurally fine — which is exactly ADR 0002:12's "An Event may carry no
Transition at all". ADR 0009:20 keeps `OVERRIDE` and "fully omit[s]" dropped Events. So the seed decision is
about `START` and `RETURN_FOR_RESUBMISSION` only, and it has a visible cost: omitting them shrinks the
published metadata collections from 8 → 7 and 13 → 12, which is a metadata-endpoint delta. Ticket 04 owns
that contract; ADR 0009's "an Event leaves no data residue" is about *history*, not about the metadata
endpoints. **Note the asymmetry:** the Override Event is Resource-scope only — `negotiation-events.json`
contains no `OVERRIDE` at all — so ADR 0002:12's phrasing does not imply an Override Event on the Negotiation
definition.

### 8. Guard wiring is expressible, exercised by zero dump rows, and seeded with one anyway — **(b) recorded, restated factually**

21/21 transitions dump `"guard": null` — the single most-repeated fact in both files
(`resource-graph-v1.json:46…226`, `negotiation-graph-v1.json:32…113`). Before-picture finding 1
(lines 28–65) establishes why: `NegotiationIsApprovedGuard` is attached only by
`ResourceStateMachineConfig.java:117` as a fragment with no source, event or target, which SSM discards, and
`LifecycleGraphDumperUnwrapTest` proves the dumper *would* have named a Guard had one been carried.

ADR 0005:14 and ADR 0009:16 nonetheless seed a **definition-level** `NEGOTIATION_APPROVED` Guard on the
Resource family. That is the correct shape for the nullable-`transition_id` Guard-wiring table, and it is
**not** an invented check: `NegotiationIsApprovedGuard.evaluate` (`:22-27`) and the imperative gate at
`ResourceLifecycleServiceImpl.java:143-144` test the same predicate — is the parent Negotiation
`IN_PROGRESS`. Finding 1's warning ("registering it would *introduce* a check that does not exist today")
is about resurrecting the dead bean's wiring, and its own conclusion is that "the behaviour to reproduce is
the imperative gate" — which a definition-scoped Guard row does exactly. **The wiring table is necessary;
what the dump alone cannot supply is the one seed row.**

### 9. ADR 0009's seed enumeration omits the three post-visibility Actions — **(a) contradiction, against ADR 0009 rather than 0002**

ADR 0009:16 lists the seed's contents as "states, events, transitions, required authorities, initial and
terminal flags, the definition-level `NEGOTIATION_APPROVED` Guard wiring (0005) and **the single
`SPAWN_RESOURCE_LIFECYCLES` Action** on the approval Transition (0007)". The dump carries **three** transition
Actions (Part A5), none of which is spawn. The generic clause "a faithful transcription of the current two
configuration classes" arguably covers them, but the explicit enumeration says "the single … Action". The
schema expresses all three without difficulty; **the risk is a seed written to the enumerated list, which
would silently drop `EnablePublicPostsAction`, `EnablePrivatePostsAction` and `DisablePostsAction`** — and
before-picture finding 8 shows exactly what that loses: the two `ABANDON` routes differ *only* in their post
Actions, `(true,true)` surviving an abandon from `PAUSED` and becoming `(false,false)` from `IN_PROGRESS`.
Flagging as a decision ticket rather than a quiet correction, consistent with C2's precedent.

### 10. Spawn is not in the dump, and is wired against a picture Part 3 already corrected — **(b), already the top row of Part 7**

The dump carries no spawn Action because spawn is not a transition Action today — it is
`ResourceNotificationService.handleResourceStateManagement`, keyed on **arriving at `IN_PROGRESS`** (so
`UNPAUSE` spawns exactly as `APPROVE` does), writing `REPRESENTATIVE_CONTACTED` or
`REPRESENTATIVE_UNREACHABLE` rather than the graph's initial `SUBMITTED`. Recorded in
before-picture-findings §C1 and §C2 (lines 567–616) and already Part 7's first owed decision. The
Action-wiring table can hold one row or two (`APPROVE` and `UNPAUSE`); it is the *number and placement* of
rows that is undecided, not the schema. **Not a new discovery.**

### 11. Legacy States and transition-less rows — **no gap; confirmed expressible**

- `APPROVED` is present at `negotiation-graph-v1.json:7` and is **fully isolated**: source of no transition
  and target of none.
- `RETURNED_FOR_RESUBMISSION` is present at `resource-graph-v1.json:16` and is likewise fully isolated.
- Both are non-initial (`$.initialState` is `SUBMITTED` in both graphs), so ADR 0002:12's "exactly one initial
  State per Definition Version" is satisfied with them present.
- The schema holds them trivially: `transition` carries FKs *to* `state`, never the reverse, so a `state` row
  that no `transition` cites needs no special provision. ADR 0009:20's "transition-less, non-initial `State`
  rows" is expressible verbatim.
- **`DRAFT` is a third shape and is not a Legacy State**: `negotiation-graph-v1.json` has no transition
  targeting it but one leaving it (`DRAFT --SUBMIT--> SUBMITTED`), and finding 2 records that the seed corpus
  really occupies it (`negotiation-6`) — something outside the Lifecycle writes it at creation. The schema
  holds this without a flag; ticket 03's decision 4 already ruled that the `DRAFT` visibility rule stays a
  hardcoded name rather than becoming a State flag, precisely because "hide the initial State" would hide
  `SUBMITTED` and reveal drafts.

---

## Part C — Schema elements the v1 data will not exercise

Stated factually; none of these is an argument that the element is unnecessary.

| Schema element | Exercised by v1 dump data? |
|---|---|
| Guard-wiring table, transition-scoped (`transition_id` set) | **No** — 0 of 21 |
| Guard-wiring table, definition-scoped (`transition_id` null) | **No rows from the dump**; ADR 0009:16 seeds exactly 1 (`NEGOTIATION_APPROVED`) |
| Guard-wiring `order` and both partial unique indexes | No — at most one Guard row per scope |
| Action-wiring `order` | Trivially — each of the 3 Actions is alone on its transition |
| Action-wiring `params` jsonb | Yes, if ADR 0002:14's `SET_POST_VISIBILITY` collapse is taken (scope + flag) |
| `required_authority = SYSTEM` | **No** — 0 of 21 (see gap 2) |
| `required_authority = NONE` | Only if gap 1 is resolved by choosing it |
| `is_global_default` | Once, on the Resource family (ADR 0009:16) |
| `version`, `active`, `family_key` | One row per family; uniqueness on `(family_key, version)` untested by one row each |
| Definition Version Pin on `Negotiation` / `NegotiationResourceLink` | Backfilled by the cutover, not by this slab (ticket 08 §Schema: nullable here) |
