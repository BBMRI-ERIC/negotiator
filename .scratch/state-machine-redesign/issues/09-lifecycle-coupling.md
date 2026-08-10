# Lifecycle coupling: fan-out / fan-in

Type: grilling

Blocked by: 01, 03, 04

Status: resolved

## Question

The map's destination covers a state-machine subsystem for **both** lifecycles, but every resolved ticket so far designs a *single* definition in isolation ([Definition model](01-definition-model.md), [Engine choice](03-engine-choice.md)). Nothing yet designs the **coupling between** the Negotiation lifecycle and its Resource lifecycles — which is where the current behaviour actually lives:

- **Fan-out**: on negotiation approval, the negotiation goes `IN_PROGRESS` and each requested resource begins its own resource lifecycle (possibly *heterogeneous* — different definitions per resource, per [Resource-Network association](04-resource-network-association.md)), in parallel.
- **Fan-in**: when all resources reach a terminal state the negotiation **auto-concludes** to `CONCLUDED` (an admin can also conclude manually).

This ticket must decide how that coupling is expressed **as configurable data on the data-driven engine**, rather than as the hardcoded Java it is today, and do so idiomatically with the design already committed.

### Framing reached in the 2026-07-22 whiteboard brainstorm (a lean, not a settled answer)

Coupling looks like **one recursive primitive** appearing at every scope boundary, with two halves:

- **SPAWN (fan-out)**: a parent transition/state-entry *activates* the children. Under the constraints below this deflates to "**initialize the child entities' state column** on the parent transition" — the resource set already exists as the negotiation's requested resources, so nothing is *instantiated*.
- **FEEDBACK (fan-in)**: the children's states drive a parent transition. The aggregation (`ALL` / `ANY` / `N-of-M` terminal) is expressible as a **guard** (reusing [ticket 07](07-event-requirement-guard-wiring.md)'s machinery); the trigger is a **signal event** on the parent; auto-fire-vs-wait-for-human reduces to the existing `required_authority` (`NONE` ⇒ auto-conclude). Terminal-state *identity* can select *which* parent event (a `child terminal state → parent event` map), which generalises "all done" into outcome-sensitive conclusion.

### Constraints already fixed (do not re-open here)

- **Query-based, no persisted instance object** (confirmed 2026-07-22): fan-in is a query over the negotiation's *existing* resources and their state columns — there are no persisted parent→child instance-link rows.
- **In-memory / transient instances**: consistent with [ticket 03](03-engine-choice.md)'s stateless evaluator; DB (state columns + `LifecycleRecord`) stays source of truth. Negotiation/resource persistence is explicitly *not* being changed.
- The engine is *handed* `(definition, currentState, event)` and is indifferent to how coupling is orchestrated — so this is a **lifecycle-service/orchestration** design, layered on the engine, not an engine change.

### The core open decision this ticket exists to resolve

**Who pokes the parent, and do any transitions self-fire?** Query-based fan-in needs *something* to re-evaluate "are all resources terminal now?" after each resource transition. Decide the trigger mechanism (e.g. the resource lifecycle service, on a child reaching terminal, attempts the parent's coupled signal event; the aggregation guard then passes/fails; auto-fire if `required_authority = NONE`). This same "self-firing transition" question also covers IR expiry/reminders (see [ticket 10](10-info-requirement-audience-contact.md)) and is expected to answer it for the whole subsystem at once. Also settle: how SPAWN targets heterogeneous per-resource definitions (via ticket 04 resolution), and whether SPAWN/FEEDBACK wiring is new definition-level data or engine-pipeline logic.

Prior-art vocabulary worth borrowing (not the engine — ruled out in [ticket 02](02-engine-landscape-research.md)): BPMN's **multi-instance activity** + **completion condition** describe exactly SPAWN-to-N + the fan-in quantifier.

## Answer

Coupling is a **lifecycle-service/orchestration layer over the stateless engine**, expressed on the existing ticket 01/07 schema plus **one** new authority value, **two** new `State` flags, and a couple of registry entries — **no new coupling table and no persisted parent→child instance rows** (upholding the ticket's fixed constraints). The whole coupling reduces to **one first-class self-firing primitive** with two halves and two trigger sources.

**1. The self-firing primitive (the core open decision — "who pokes the parent, do transitions self-fire?").** There is exactly one primitive: *an orchestration trigger attempts a definition's automatic events whose source = the current state, and the normal `evaluate()` guard pipeline decides whether any fires.* The engine stays the indifferent stateless evaluator of ticket 03; the trigger lives in the orchestration layer. It has **two interchangeable sources**:
- **(a) a child/sibling transition commit** — fully specified here (fan-in);
- **(b) a clock tick** — declared here as reusing the *identical* evaluation path; its scheduler plumbing is deferred (see Deferrals).

**2. "Automatic" is a distinct authority value, not an overload of `NONE`** (amends [ticket 07](07-event-requirement-guard-wiring.md)). `required_authority` gains a **`SYSTEM`** value (enum becomes `NONE`/`IS_ADMIN`/`IS_CREATOR`/`IS_REPRESENTATIVE`/`SYSTEM`). An event is machine-fired **iff** `required_authority = SYSTEM`. This rides ticket 07's pipeline for free: the authority check runs first, so a `SYSTEM` event **fails authority for any human caller** — it never appears in `getPossibleEvents` and can never be hand-triggered over REST; it is *only* ever attempted by the orchestrator's self-firing trigger. "Auto-fire vs. wait for a human" is thus genuine configurable data (flip `SYSTEM` ↔ `IS_ADMIN` and the same event becomes a manual admin action), but a *typed value*, not a fragile "no gate" overload. This deliberately refines the whiteboard lean's `NONE ⇒ auto` shorthand, which conflated "no authority needed" (open human event) with "no human at all" (machine event).

**3. FEEDBACK (fan-in) is implicit orchestration — zero config.** After any resource transition in negotiation N commits, a **post-commit hook** (the modern equivalent of today's `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`) re-attempts N's `SYSTEM` events whose source = N's current state. No coupling row links a child to a parent event — the negotiation↔resource relationship is a fixed architectural fact, not an admin-misconfigurable one (same built-in-stage philosophy that made ticket 07's requirement check unwireable). Post-commit, not inline in the child transaction, was chosen to preserve the stateless-engine boundary and avoid entangling parent/child atomicity; the brief "conclude-able but not yet concluded" window is identical to today's, so no regression. The alternative of a first-class `Coupling` entity was rejected as re-encoding a structurally-fixed relationship while adding a silent-breakage surface (a forgotten coupling row) for flexibility no one wants.

**4. The aggregation is an ordinary guard on the parent's `SYSTEM` event** (reuses ticket 07 machinery). `CONCLUDE` becomes: `required_authority = SYSTEM` + a **`TERMINAL_AGGREGATION`** GuardType. The guard is **parameterized** on ticket 07's typed `params` (jsonb) — `quantifier: ALL | ANY | N_OF_M` (+ `n`) — with today's production config being `ALL`; ANY / N-of-M cost nothing extra on the param mechanism and future-proof the guard so no follow-on has to widen an `ALL`-only boolean. "Terminal" is **not** a hardcoded state-id list (it can't be — resources run heterogeneous definitions): the guard asks *each resource's own pinned definition* whether its current state is flagged terminal.

**5. SPAWN (fan-out) is a named, wired-once action.** It rides ticket 01's transition-scoped **action** registry as a `SPAWN_RESOURCE_LIFECYCLES` strategy bean. Its *implementation* is regular business logic (written once), relocated out of today's `NegotiationInProgressHandler` + link-init scatter into one registered action. Its *wiring* — "this transition spawns" — is the only configurable part, and it is configured **exactly once**: one action-wiring row on the **sole Negotiation definition's** approval transition (the destination fixes one Negotiation-lifecycle definition). Resource definitions are the *targets* of spawn and carry **no** spawn config — adding a new resource family wires nothing for spawn. Making it a wired action (rather than hardcoding entry-to-`IN_PROGRESS`) directly honours ticket 07's note that a future network may legitimately want resource work to start pre-approval.

Per requested resource, the SPAWN action does three things atomically in the approval transaction: **resolve** the definition family (direct Resource override > Network resolution > global-default fallback, per [ticket 04](04-resource-network-association.md)); **pin** the resolved version by writing the immutable `definition_version_id` FK on that `NegotiationResourceLink` (ticket 08's identity FK), so a later version publish never disturbs in-flight work; **initialize** the resource's state column to that definition's initial state. **Pin-at-SPAWN, not at creation**: "start" for a resource is when its lifecycle begins (approval), so resolution runs against the *final* resource set at *approval-time* active versions. Ticket 04's global-default fallback guarantees resolution can't fail, so SPAWN can't half-initialize.

**6. `State` gains `initial` + `terminal` boolean flags** (amends [ticket 01](01-definition-model.md)), with a per-definition invariant "exactly one initial state." The engine needs `initial` to start any machine (consumed by SPAWN); the `TERMINAL_AGGREGATION` guard needs `terminal` to evaluate heterogeneous fan-in. Both scopes (Negotiation, Resource) get them.

**7. Double-fire safety.** Correctness rides the state machine's own **source-state check**: two resources finishing near-simultaneously each run the post-commit hook, but once the first commits N to `CONCLUDED`, the second attempt's source-state (`IN_PROGRESS`) no longer matches, so it is a natural no-op (a `SYSTEM` event from the wrong state is simply not a valid transition). On top of that, a **pessimistic row lock on the parent negotiation** during a `SYSTEM`-event attempt serializes concurrent attempts, so the loser re-reads `CONCLUDED` and cleanly no-ops without a duplicate audit row. State-guard for correctness; lock for no wasted/duplicate work.

**Amendment summary:** amends ticket 07 (`SYSTEM` authority value), ticket 01 (`initial` + `terminal` `State` flags; `SPAWN_RESOURCE_LIFECYCLES` action + `TERMINAL_AGGREGATION` GuardType as registry entries), and confirms ticket 04/08 timing (resolve+pin at SPAWN/approval against the final resource set).

### Deferrals & scope

- **Clock-source scheduler plumbing** (Spring `@Scheduled` vs. quartz vs. DB-polling; time-based guard specifics) is **not** designed here. Ticket 09 fixes only the *primitive* and its two sources for the whole subsystem; the clock source's concrete mechanics ride with [ticket 10](10-info-requirement-audience-contact.md)'s **parked** IR reminders/expiry ("additive when needed, out of scope now") — the primitive is ready for them, but the scheduler is built only when those features are. Stays fog.
- **Outcome-sensitive conclusion** (differentiated `CONCLUDE` per resource-outcome mix; the "child terminal state → parent event" map) is **out of scope** — a lifecycle/product change beyond this redesign, whose baseline is today's single outcome-blind `CONCLUDED`. The `SYSTEM`-event + `TERMINAL_AGGREGATION`-guard mechanism **accommodates it as later config** (additional mutually-exclusive `SYSTEM` events from `IN_PROGRESS` + new states), so it is excluded, not foreclosed.
- **Resources added after approval** (an admin linking a resource to an already-`IN_PROGRESS` negotiation) would bypass SPAWN and start unpinned/uninitialized. Flagged as an **orchestration follow-on** for the implementation effort, not designed here; today's model links resources up-front.
