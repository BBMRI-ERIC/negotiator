# Information-requirement audience, aggregation & contact

Type: grilling

Status: resolved

Blocked by: 01, 05, 07

## Question

[Information requirements as first-class model](05-info-requirements-model.md) settled the IR **enforcement** seam — a `Requirement` attaches to an `Event`, and satisfaction is a boolean evaluated as a built-in engine guard stage ([ticket 07](07-event-requirement-guard-wiring.md)). This ticket designs the IR model **around** that seam, which ticket 05 left thin: *who* is asked, *how many* must answer, and *how they get told*. Ticket 05 is **not** re-opened — its core stands; this builds on it.

### Settled dead-end (record, don't re-explore)

The 2026-07-22 brainstorm explored making the IR **its own state machine** (a third `INFO_REQUIREMENT` scope on the shared engine, with a `contact → submitted → satisfied` internal flow). **Rejected.** Reasons: (1) an IR has no natural home for a persisted state column the way `Negotiation`/`NegotiationResourceLink` do, so a real IR machine would force a **persisted per-IR-occurrence instance** — exactly what the "no persisted instance" call rules out; (2) once contact moves onto the parent transition (below), the IR reduces to **two derived states** (`PENDING`/`SATISFIED`), a degenerate machine identical to ticket 05's boolean. The third scope is only justified by **multiple outcomes** (below), which are parked — so the IR stays a derived boolean until then.

Full reasoning (the whole iteration, the persistence-asymmetry argument, and the revisit trigger): [Why an Information Requirement is not its own state machine](../notes/ir-smd-rejected-rationale.md).

### The four things to design (leans from the brainstorm, to be nailed here)

1. **Audience, as a first-class concept with a resolver registry.** Replace ticket 05's vague "assignee: representative / Person / lightweight Group ref" with an explicit **audience** resolved at evaluation time by one of a small set of **resolver strategies** — `RESOURCE_REPRESENTATIVES`, `NEGOTIATION_CREATORS`, `IAM_GROUP(ref)` — structurally mirroring [ticket 01](01-definition-model.md)'s guard/action strategy registry. Design the resolver interface. (This absorbs the former "virtual groups": creator-as-group, representatives-as-group.) The `IAM_GROUP` resolver's *interface* is designed here; the LS-AAI group **membership-sync mechanics** are deferred (map fog).

2. **Aggregation quantifier** over the audience: `ANY` / `ALL` / `N-of-M`. Ticket 05 only had `ANY` ("≥1 qualifying submission"). This is what makes the SMB "everyone submits their own" case expressible as config (`audience: IAM_GROUP(SMB), quantifier: ALL`) — dissolving the former "multi-person submission modes" fog item, with **no per-member fan-out instances** (multiplicity is a count over the audience, not spawned machines).

3. **Contact as a parent-transition action — no `CONTACTED` state, no trace, no init concept.** A `NOTIFY_IR_AUDIENCE` action ([ticket 01](01-definition-model.md) action registry) fires when the parent enters the state that makes the IR relevant. Exactly-once comes free from the parent transition being discrete/recorded; no separate contact record needed. **Lean: fire structurally** — on entry to any state from which a requirement-bearing event is reachable (reusing ticket 05's structural-reachability computation), *not* an explicit admin-wired action, so it can't be forgotten (same philosophy as ticket 07's built-in requirement check). Decide structural-vs-explicit and the exact firing point. (Fires on a parent transition, so brushes [ticket 09](09-lifecycle-coupling.md) — but the action registry already exists, so no blocking dependency.)

4. **Derive, don't store.** IR state is a *projection* of `information_submission` rows (drift-free; single source of truth), never a stored column, never a persisted instance. Confirm this holds and that the guard from ticket 05 reads the derived value. The general rule: a state is underivable only if its entering action leaves no domain fact — every IR state-entering action (submit, and *later* contact/waive) can drop a record and stay derivable.

### Deferred upgrade path (design so it's additive, don't build)

**Multiple outcomes** (e.g. Ethics `APPROVED` vs `REJECTED` routing the parent down different transitions) is the one thing that would graduate the IR from a derived boolean to a derived multi-terminal-state machine, with the `child terminal state → parent event` map from [ticket 09](09-lifecycle-coupling.md). Keep the audience/guard seam shaped so this is a later addition, not a migration. Same for reminders/expiry (needs a `contacted_at` record → derivable `CONTACTED`) — additive when needed, out of scope now. *Note: [ticket 09](09-lifecycle-coupling.md) has since defined the self-firing primitive (an orchestration trigger attempts a definition's `SYSTEM`-authority events from the current state; guards decide) and named a **clock tick** as its second trigger source specifically for IR reminders/expiry — so when that feature is built, its trigger reuses ticket 09's primitive; only the scheduler plumbing remains to design.*

`InformationSubmission.submittedBy` (added by ticket 05) is what lets a submission be matched against a resolved audience — depended on here.

## Answer

Designs the IR model *around* ticket 05's derived-boolean seam, across four areas. Nothing here re-opens ticket 05's core (a `Requirement` attaches to an `Event`; satisfaction is a built-in guard stage, ticket 07); it fills in *who* is asked, *how many* must answer, and *how they're told* — with **no persisted per-IR-occurrence instance** anywhere.

### 1. Audience — a first-class concept with a resolver registry

**One primitive.** An audience resolver has a single operation, `AudienceResolver.resolve(AudienceParams, EvaluationContext) → Set<PersonRef>`. Everything else derives from that one set: membership = `set.contains(submittedBy)`, denominator = `set.size()`, contact = iterate the set. Rejected a two-method `contains()`/`enumerate()` interface — the audiences in play (a resource's reps, a negotiation's creators, a scoped IAM group) are small, and all three consumers ultimately want the set, so lazy membership-testing earns nothing.

**Registry shape mirrors ticket 01 verbatim.** Config lives on the `Requirement` row as `audience_type` (string type-key) + `audience_params` (typed jsonb), resolved via a registered strategy bean — the same `{type-key + typed jsonb params + strategy bean}` pattern ticket 01 uses for guards/actions. The resolver receives the *existing* engine `EvaluationContext` (ticket 05's `evaluate(definition, currentState, event, context)`), which already carries the negotiation id, resource id (null for Negotiation-scope), and acting person — **no new plumbing**.

**Three strategies, with declared scope-compatibility:**
- `RESOURCE_REPRESENTATIVES` — empty params, reads `context.resource`; valid in **RESOURCE scope only**.
- `NEGOTIATION_CREATORS` — empty params, reads the negotiation (always in context, since a resource lives inside a negotiation); valid in **both scopes** (a resource-lifecycle requirement can legitimately ask the negotiation's requester for something).
- `IAM_GROUP` — params carry `{ "groupKey": "..." }`; needs neither entity; valid in **both scopes**.

Scope-compatibility is a **declared property of each resolver, validated at write-time** when the admin attaches the audience-bearing requirement to an event (matches ticket 04's write-time-rejection philosophy). Wiring `RESOURCE_REPRESENTATIVES` onto a Negotiation-scope event would resolve to an empty set — an unsatisfiable requirement / silent dead-click — so it's rejected at save, never discovered at evaluation. (This absorbs ticket 05's vague "assignee: representative / Person / Group ref" — creator-as-group and representatives-as-group are now resolver strategies, not a general `Group` entity. The `IAM_GROUP` resolver's *interface* is designed here; LS-AAI membership-sync mechanics stay deferred — map fog.)

### 2. Aggregation quantifier

**Unifying primitive:** `satisfied = (count of distinct audience members with ≥1 qualifying submission) ≥ threshold`, where the quantifier picks the threshold:
- `ANY` → 1
- `N_OF_M` → N (admin-configured integer, in the quantifier's params on the `Requirement`)
- `ALL` → live `|audience|` (the only quantifier with a *dynamic* threshold)

**Counting is by distinct member, not by submission** — one rep submitting twice counts as 1. A submission **qualifies iff `submittedBy` ∈ the resolved audience**; an out-of-audience submission of the same form type never counts toward satisfaction. (`InformationSubmission.submittedBy`, added by ticket 05, is what makes this checkable.)

**Live re-resolution, no snapshot.** The audience — and therefore `ALL`'s threshold and each submission's qualification — is recomputed on every evaluation (consistent with derive-don't-store). Membership drift is **harmless**: the requirements guard only gates the *instant an event fires*; once the guard passes and the transition commits, the lifecycle state advances and becomes a *persisted fact* that is never re-derived from IR satisfaction. So drift can only cause a transient flicker in the possible-events *listing* (event shows clickable, a rep is added, it un-shows) — never a rollback of a committed transition. Non-monotonicity is bounded to the pre-firing window. Rejected **freezing the audience at contact time**: it would require persisting a per-IR-occurrence audience snapshot — reintroducing exactly the stored instance the rationale doc eliminated, and making `CONTACTED` derivable again. `N_OF_M` with N above the current audience size simply blocks (correct); validated at write-time only where the audience size is statically knowable.

### 3. Contact — a built-in structural stage (not admin-wired)

**`NOTIFY_IR_AUDIENCE` is a built-in, post-commit engine stage, not an admin-wireable action.** *(Amends this ticket's own lean and the rationale doc's "action in ticket 01's action registry" framing.)* When any transition commits into state `S`, the engine checks whether `S` has a *direct outgoing transition* (one-hop structural reachability — the same computation ticket 05 uses for its `requirement-{id}` hint-link inclusion condition) for any requirement-bearing event; for each such requirement it resolves the audience and notifies. There is **no wiring row to forget** — it's structurally paired with ticket 07's built-in requirements *check*, so contact and enforcement can never drift apart, and the dead-click / silent-omission failure class (the reason this whole IR sub-effort exists) cannot be reintroduced by misconfiguration. Rides ticket 09's existing post-commit re-attempt point; no new plumbing, no blocking dependency.

*Configurable (admin-wired) contact was considered and rejected.* Its upside is timing control (notify N states ahead, suppress on rollback re-entry) and riding the "wiring is data" action registry; its fatal downside is re-opening silent omission — an admin who adds a new in-edge, or forgets the wiring, produces a requirement **nobody is ever told about**. The safest configurable variant (a new *state-entry* action scope robust to new in-edges + a write-time completeness lint that flags un-notified requirements) was sketched but not chosen — built-in gives the same can't-forget guarantee with no admin surface.

**Exactly-once: no dedup record + notify only the currently-unsatisfied.** Holding the line on no contact record (a contact trace would resurrect the killed `CONTACTED` state), the engine has no memory of prior contact, so re-entry re-notifies. This is made correct — not merely tolerable — by notifying only `audience \ {members who already submitted}` (derived per member, zero persistence): first entry notifies everyone; re-entry after partial submission nags only the stragglers; re-entry after full satisfaction notifies no one (self-suppressing). Notification channel/mechanism reuses the existing notification subsystem — an implementation concern, not designed here.

### 4. Derive, don't store — confirmed, with the forward rule locked

**Zero per-IR-occurrence instance rows.** Audience/quantifier are definition config on the `Requirement`; there is no snapshot (§2), no contact record (§3), no stored satisfaction column. Satisfaction is computed live and consumed by ticket 07's built-in requirements stage, which emits the `GuardResult` (with ticket 05's missing-form/assignee details). The IR's only "states" are the derived `PENDING`/`SATISFIED` projection over `information_submission`.

**Invariant recorded:** an IR state is legitimately derived **iff** every action that would enter it leaves a persisted domain fact. `submit` → an `information_submission` row → `SATISFIED` derivable. `contact` leaves *nothing by design* — safe **only because contact is a fire-and-forget side effect, not a state** (the IR stays `PENDING` regardless of whether contact fired, so nothing must derive "was contacted").

**Forward rule (gate for the parked upgrades):** any *new* IR-affecting action must drop a domain fact to stay derivable. A future **waive** writes a waiver record (then `SATISFIED = quantifier(submissions) OR waived`); a future **reminder/expiry** writes `contacted_at` → derivable `CONTACTED`, triggered by ticket 09's clock-tick primitive. Both additive, both preserve the invariant, neither built now. **Multiple outcomes** (the one thing that would promote the IR to a derived multi-terminal-state machine, using ticket 09's `child terminal state → parent event` map) stays shaped as a later addition — the audience/guard seam is untouched by it.

### Amendments to prior work produced here
- **Ticket 05:** the `Requirement`'s "assignee: representative / Person / Group ref" field → the audience resolver registry (`audience_type` + `audience_params`).
- **Rationale doc** ([Why an IR is not its own state machine](../notes/ir-smd-rejected-rationale.md)): its "contact as a parent-transition action (ticket 01 action registry)" → a **built-in structural stage** (same destination — contact on the parent transition — but not admin-wireable).
