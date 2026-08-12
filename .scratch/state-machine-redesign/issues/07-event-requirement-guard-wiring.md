# Event-requirement guard-chain wiring mechanism

Type: grilling
Status: resolved

Blocked by: 01, 05

## Question

Ticket 01 wires guards/actions onto `Transition` via explicit per-row join entries ("wiring... is pure data"). Ticket 05 attaches `Requirement` to `Event` and makes requirement-satisfaction "a registry `GuardType` entry... evaluated inside the same guard chain as any other guard."

Neither ticket says how an Event's Requirements get into the guard chain of every `Transition` that fires that event. Two candidate mechanisms, at minimum:

- **Implicit/automatic**: the engine's `evaluate()` pipeline always checks the firing Event's attached Requirements as a built-in step, with no join row needed at all — the requirement-guard isn't really "in" the per-transition join table, it's a pipeline stage keyed off the Event.
- **Explicit/manual**: an admin must add this GuardType's join row to each relevant Transition — in which case a missed transition silently reintroduces the dead-click bug ticket 05 exists to fix (an event displays as available/clickable, then fails on submission).

Decide which mechanism applies, and reconcile it with ticket 01's Transition-scoped guard-wiring model (does this mean requirement-satisfaction is *not* a Transition join-row guard after all, contradicting ticket 05's "registry `GuardType` entry... in the same guard chain" framing — or does something else resolve the tension?).

Surfaced identically on both [Definition model](01-definition-model.md) and [Information requirements as first-class model](05-info-requirements-model.md) as an unresolved review finding from the 2026-07-16 wayfinder-map consistency review.

## Answer

**Implicit — a built-in engine stage, not join rows.** The engine's `evaluate()` always checks the firing Event's attached Requirements as a fixed pipeline stage, derived from the `Event`↔`Requirement` association at evaluation time. No wiring data exists for it anywhere — like `required_authority`, it cannot be omitted, so ticket 01's "applies wherever this event fires" guarantee is structural and the dead-click bug cannot be reintroduced by configuration. The stage runs the same guard code contract and emits a normal `GuardResult` (reasonCode + missing-form details per ticket 05) into `TransitionEvaluation.guardResults`.

**Reconciling the two tickets' framing:** ticket 05's "registry `GuardType` entry … in the same guard chain" is amended to "built-in stage sharing the guard contract" — the requirements check does **not** appear in the admin-wireable GuardType registry and cannot be wired onto a Transition (one invocation path; no dual-use variant gating one event on another event's requirements). Ticket 01's "wiring is pure data on `Transition`" is amended by the next point.

**Definition-level guards become a first-class wiring scope** (amends ticket 01). Guard wiring now has two scopes: a definition-level guard entry applies the GuardType to *every* transition of that definition; transition-level entries stay for transition-specific conditions. `NEGOTIATION_APPROVED` becomes one definition-level entry per resource-scope definition — matching its existing machine-wide intent (its javadoc says it gates "any interaction" with the resource machine; today it's hacked in as a dangling `.withExternal().guard(...)` with no source/event/target because Spring Statemachine lacks the concept) and eliminating the copy-drift risk when later definition versions add transitions. Admin tooling must present the merged *effective guard chain* (definition-level + transition entries) per transition. Actions are untouched — transition-scoped only, as ticket 01 had them. The alternative of hardcoding `NEGOTIATION_APPROVED` into the engine was rejected: which guards apply should stay configurable data (a future network may legitimately want resource work to start pre-approval).

**Pipeline order: `required_authority` → built-in requirements check → guard rows (definition-level entries first, then transition entries, each in their explicit order), short-circuiting on first failure.** Ordered by expected failure likelihood (authority fails most often for ordinary users; unfilled requirements next; guards are consistency checks that usually pass), which minimizes evaluation work under short-circuit. Failure categories are also monotonic — authorization (403-shaped) → unmet requirement (422-shaped) → domain-state conflict (409-shaped) — so the category of the first-surfaced failure never flip-flops with wiring placement.

**Consequence, no new decision:** `OVERRIDE` (the transition-less admin state override from ticket 01) bypasses `evaluate()` entirely, so neither guards nor requirements gate it — unchanged from today's behavior.
