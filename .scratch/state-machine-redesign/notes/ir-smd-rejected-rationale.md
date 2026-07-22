# Why an Information Requirement is *not* its own state machine

**Status:** settled conclusion (2026-07-22 whiteboard brainstorm). Record so it isn't re-explored.
**Feeds:** [Information-requirement audience, aggregation & contact](../issues/10-info-requirement-audience-contact.md).
**Relation to prior work:** does **not** re-open [Information requirements as first-class model](../issues/05-info-requirements-model.md) — it confirms 05's boolean seam and explains why the richer idea collapses back onto it.

## The idea we were iterating on ("IR-SMD")

Model an **Information Requirement as its own state-machine definition** — a third scope (`INFO_REQUIREMENT`) running on the same engine and definition tables we're already building for the Negotiation and Resource lifecycles — with an internal flow like `AWAITING_CONTACT → CONTACTED → SATISFIED`, and possibly *multiple terminal states* (e.g. `APPROVED` / `REJECTED`) that could route the parent workflow differently.

## Why it was attractive (the pull)

- **Self-similarity.** [Definition model](../issues/01-definition-model.md) already made definitions scope-parameterized and [Engine choice](../issues/03-engine-choice.md) made the engine a generic scope-agnostic evaluator. An IR flow would just be "a third scope" — workflows all the way down, no bespoke code.
- **It answered the awkward "where does the contact step live?" question** — contact would be the first edge of the IR's own machine.
- **Uniform vocabulary** (same states/events/transitions DSL) and a **natural multi-outcome future**.
- It matched the intuition of an IR as a **self-contained component that hooks into workflows** rather than something baked into state-machine thinking.

## The reasoning that rationalized it out

The arguments, in the order they landed:

1. **The boolean seam is load-bearing and never moves.** From the outside, an IR is a black box that reports *satisfied / not* — this is [ticket 05](../issues/05-info-requirements-model.md)'s enforcement decision (satisfaction is a built-in guard stage, [ticket 07](../issues/07-event-requirement-guard-wiring.md)). So IR-SMD could only ever be an *internal* refactor of the black box; it could never change how the IR couples to the workflow. That capped its upside from the start.

2. **Two axes were being conflated; separating them removed most of the motivation.**
   - **Axis X — multiplicity of submitters** (the SMB "5 people each submit" case). Settled *independently* by the **audience + aggregation quantifier** model: a single IR with `audience = IAM_GROUP(SMB), quantifier = ALL`, satisfied by counting qualifying submissions. **No per-member instances, no fan-out.**
   - **Axis Y — does the IR have its own flow?** The state-machine question lives *only* here. With Axis X handled by counting, the only thing left for an IR machine to model was the contact step and (parked) multiple outcomes.

3. **The persistence asymmetry — the decisive argument.** Negotiation and Resource get to be state machines "for free" because they are domain entities with a natural home for a current-state column and history (`Negotiation.state` / `NegotiationResourceLink.state` + `LifecycleRecord`). **An IR has no such home.** An IR "instance" is `(Requirement × this negotiation/resource)` — a row that does not exist today. So making the IR a *real* state machine would **force materializing a persisted per-IR-occurrence instance** (state + audit history), for potentially very many of them. That directly contradicts the deliberate decision that there is **no persisted instance object** and instances are transient/in-memory (consistent with [ticket 03](../issues/03-engine-choice.md)'s stateless evaluator; DB stays source of truth). The IR is the one scope where "be a state machine" is *not* free.

4. **IR state is derivable, so it needn't be stored at all.** Satisfaction is a pure projection of `information_submission` rows (`satisfied = quantifier(audience, submissions)`) — drift-free, single source of truth, and exactly what the current code already computes on the fly. The general test: *persist a state only when it is the primary record of a decision that leaves no other trace (the lifecycle states); derive it when it is a projection of facts you already store (the IR).* The engine is handed `currentState` and is indifferent to its origin, so this is a per-scope persistence choice with zero engine impact.

5. **The only non-derivable state was `CONTACTED` — and "underivable" turned out not to be intrinsic.** A state is underivable only if its *entering action leaves no domain fact*. Contact left no trace only because we weren't recording it. Two ways out — record the notification (→ `CONTACTED` becomes derivable like `submitted`), or **move contact out of the IR entirely** — and the second is cleaner.

6. **Contact-as-a-parent-transition-action removed the IR machine's last job.** The "initial transition you'd need to fire the contact" already exists — it is the **parent machine's** transition into the state where the IR becomes relevant. Hang a `NOTIFY_IR_AUDIENCE` action there (reusing [ticket 01](../issues/01-definition-model.md)'s action registry). Exactly-once comes for free from the parent transition being discrete and recorded — **no `CONTACTED` state, no contact trace, no instance.** An IR-internal `init_event`/`init_action` would have been strictly *worse*: firing an init action exactly once requires knowing it already fired, which reintroduces the persisted instance and the trace we were trying to avoid.

7. **What's left collapses to the boolean.** With contact gone and state derived, the IR "machine" is two derived states (`PENDING`/`SATISFIED`) — a degenerate machine identical to [ticket 05](../issues/05-info-requirements-model.md)'s boolean guard. It buys nothing over "compute a boolean."

## What we kept instead

IR = **ticket 05's derived boolean** + **audience resolver registry** (`RESOURCE_REPRESENTATIVES` / `NEGOTIATION_CREATORS` / `IAM_GROUP`) + **aggregation quantifier** (`ANY`/`ALL`/`N-of-M`) + **contact as a parent-transition action** (structurally fired). No third scope, no persisted instance, no stored IR state. (Designed in [ticket 10](../issues/10-info-requirement-audience-contact.md).)

## When to revisit (the trigger that would flip this)

**Multiple outcomes** — an IR whose *terminal-state identity* routes the parent down different transitions (Ethics `APPROVED` vs `REJECTED`) — is the **only** thing that justifies promoting the IR to a genuine multi-terminal-state machine, using the `child terminal state → parent event` map from [ticket 09](../issues/09-lifecycle-coupling.md). Even then it would likely be a **derived** machine (no stored instance) if the outcome is captured in a submission's payload. Reminders/escalation-as-configurable-transitions could also pull this way (they need a `contacted_at` record → derivable `CONTACTED`). Both are additive later, not a migration — the audience/guard seam is shaped to allow it.

## Reusable takeaway

A concept earns a first-class state machine only when **(a)** its state is a *primary fact* that needs a persistent home, **and (b)** it has genuine multi-step internal transitions worth configuring as data. An IR has neither (its state is derived; its "flow" reduces to a boolean once contact moves to the parent). **Self-similarity with the surrounding design is a pull, not a justification** — reusing the engine "because we can" would have bought a persisted-instance cost and drift surface in exchange for zero expressive gain.
