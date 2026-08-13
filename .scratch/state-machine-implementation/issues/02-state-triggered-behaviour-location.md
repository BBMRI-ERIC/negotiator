# Where state-triggered behaviour lives

Type: grilling
Status: open

## Question

Seven `NotificationStrategy` handlers react to lifecycle transitions by **switching on enum constants**, entirely outside ADR 0002's Guard/Action registry. Do they become data-wired Actions, or stay listeners keyed on state identity?

No ADR addresses this. ADR 0002 puts configured effects in the Action registry and ADR 0007 relocates spawn into a `SPAWN_RESOURCE_LIFECYCLES` Action — but the notification handlers are Spring listeners on `NegotiationStateChangeEvent` / `ResourceStateChangeEvent`, a mechanism the ADRs never mention.

### What the code actually does

- `notification/internal/NegotiationStatusChangeHandler.java:40-41` — `case SUBMITTED -> …; case IN_PROGRESS, DECLINED, ABANDONED -> …`, and again at `:84-94` to pick message text.
- `notification/internal/NegotiationInProgressHandler.java` — `if (event.getToState() == NegotiationState.IN_PROGRESS) notifyResourceRepresentatives(...)`. This is the **spawn trigger today**, so ADR 0007 already claims part of this file.
- Also keyed on states: `NegotiationSubmissionHandler`, `NewNegotiationHandler`, `PendingNegotiationReminderHandler`, `ResourceNotificationService`, `NonRepresentedResourcesHandlerImpl`.
- Webhooks too: `webhook/event/NegotiationStateChangeWebhookMappingStrategy`, `NewNegotiationWebhookMappingStrategy`.

### Why it is a real decision, not mechanical

A Java `switch` over states is only sound while states are compile-time constants. Once a Network can run its own Definition Family, **a custom family may have no `IN_PROGRESS` state at all** — so "notify representatives when the negotiation goes in-progress" either silently never fires, or has to be expressed as configuration on that family's graph.

Sharpen at least:

1. **Actions, listeners, or split?** Making them Actions makes notification configurable per definition and consistent with ADR 0002/0007 — but expands stage 1 and means every seeded family must wire notification rows, which ADR 0009's frozen seed would have to carry. Keeping them listeners keyed on state *names* is far smaller but leaves behaviour that silently depends on a state name existing.
2. **If Actions: what happens to the `NotificationStrategy` mechanism?** Does it survive for non-lifecycle notifications, leaving two mechanisms, or does it go?
3. **If listeners: what do they key on**, and what happens when the name is absent from a custom family — silent no-op, boot-time validation, or a runtime warning?
4. **`NegotiationInProgressHandler` is contested.** ADR 0007 moves spawn into an Action, but this handler currently does spawn *and* notification in one place. Split it, or does the Action absorb both?
5. **Does the answer differ by Definition Scope?** The Negotiation Lifecycle keeps a single definition, so its state names are effectively stable; Resource families are the ones that can diverge.

### Why this ticket comes first

It **narrows the next ticket**. Any handler that becomes an Action stops needing state identity in Java at all, removing it from the ~60-file decoupling problem. It also decides the split in standing decision 2: consumers that become Actions cannot be decoupled early, because they need the Action registry — and therefore the schema and evaluator — to exist first.

Use `/grilling` + `/domain-modeling`; `/codebase-design` is likely useful, since this is a seam question. Note `backend/CONTEXT.md` defines **Action** as "a named, configurable effect run only after a Transition commits" — check whether notification actually fits that, or whether it is closer to the built-in `NOTIFY_IR_AUDIENCE` stage ADR 0006 makes deliberately structural rather than wired.
