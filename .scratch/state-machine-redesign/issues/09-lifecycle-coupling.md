# Lifecycle coupling: fan-out / fan-in

Type: grilling

Blocked by: 01, 03, 04

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
