---
status: accepted
---

# 0005 — Information Requirements gate Transitions as a built-in pipeline stage

_Source tickets: [Information requirements as first-class model](../../../.scratch/state-machine-redesign/issues/05-info-requirements-model.md), [Event-requirement guard-chain wiring mechanism](../../../.scratch/state-machine-redesign/issues/07-event-requirement-guard-wiring.md)._
_Implementation is follow-on work — nothing here is built yet, and requirements are still enforced ad hoc inside `ResourceLifecycleServiceImpl.sendEvent()`._

An Information Requirement attaches to an Event, so it applies wherever that Event fires, whatever State it fires from. Today enforcement happens in a service method that `getPossibleEvents()` knows nothing about, which is a live UX bug: an Event is listed as available, the user clicks it, and the submission fails because a form is unfilled.

**The requirement check is a Built-in Stage of the Evaluation Pipeline.** The Transition Evaluator always evaluates the firing Event's Requirements, derived from the Event↔Requirement association at evaluation time. There is no Wiring row for it anywhere, so no admin can omit it and no newly added Transition can miss it — the "applies wherever this Event fires" guarantee is structural rather than configured. This is deliberately **not** a registry Guard type: a wireable Guard would have to be attached to each Transition by hand, and one forgotten row silently reintroduces exactly the dead-click bug this design exists to remove. The stage still speaks the Guard contract, emitting a normal result with a reason code and the missing forms in its details, so callers see one uniform list of failures.

**Guards gain a Definition Version scope alongside the Transition scope** (the wiring shape is in 0002). `NEGOTIATION_APPROVED` becomes one definition-level Guard entry per Resource-scope definition, matching what it always meant — it gates any interaction with a Resource Lifecycle, and today is hacked in as a dangling transition with no source, event or target because Spring Statemachine has no concept for it. Definition-level wiring also removes the copy-drift risk of re-attaching it to every Transition a later version adds. Hardcoding it into the evaluator was rejected: which Guards apply should stay data, because a Network may legitimately want Resource work to begin before approval.

**The pipeline order is fixed: Required Authority, then the requirement check, then Guards** — definition-level entries before Transition entries, each in their configured order, short-circuiting at the first failure. The order is chosen by expected failure likelihood, so the cheapest and most common rejection comes first: authority fails most often for ordinary users, unfilled requirements next, and Guards are consistency checks that usually pass. It also keeps failure categories monotonic — authorization, then unmet requirement, then domain-state conflict, shaped as 403, 422 and 409 — so the category a caller sees never flip-flops depending on where a Guard happens to be wired.

**One evaluation path serves both the real gate and the listing.** `sendEvent` and the Possible Events listing call the same function; the listing is a dry run over the Events reachable from the current State, and a blocked Event is simply omitted. That removes today's double enforcement, where the authority rule is hand-checked once to build the listing and then checked again by the framework when the Event fires. The endpoint keeps its current shape — no new endpoint, no new DTO — because the correct listing is the one that already computes the right answer.

Three fixes ride along, in the Resource assembler that builds the requirement hint links. Their inclusion condition becomes *structural reachability* — a Transition exists from the current State for this Event — rather than "a lifecycle link was already built", which would now always be false for precisely the blocked Event whose hint matters. The link's display name becomes the Event's human label rather than the raw key. And the per-row rel names (`requirement-7`, `requirement-9`, …) collapse into HAL's real mechanism for multiples, array-valued `requirement` and `submission` rels.

An `InformationSubmission` gains a `submittedBy` reference, which it has never had, and its `resource` becomes optional so Negotiation-scope Events can carry Requirements too.

This ADR does not settle who a Requirement is asked of, or how many of them must answer (0006).
