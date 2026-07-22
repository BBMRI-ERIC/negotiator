# Information-requirement audience, aggregation & contact

Type: grilling

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

**Multiple outcomes** (e.g. Ethics `APPROVED` vs `REJECTED` routing the parent down different transitions) is the one thing that would graduate the IR from a derived boolean to a derived multi-terminal-state machine, with the `child terminal state → parent event` map from [ticket 09](09-lifecycle-coupling.md). Keep the audience/guard seam shaped so this is a later addition, not a migration. Same for reminders/expiry (needs a `contacted_at` record → derivable `CONTACTED`) — additive when needed, out of scope now.

`InformationSubmission.submittedBy` (added by ticket 05) is what lets a submission be matched against a resolved audience — depended on here.
