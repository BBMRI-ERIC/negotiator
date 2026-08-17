# Where state-triggered behaviour lives

Type: grilling
Status: resolved

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

## Answer

**They stay listeners. Exactly one thing moves into the Action registry, and it moves for spawn's reasons, not notification's.** Notification and webhooks are named **Well-known State** dependencies (term added to `backend/CONTEXT.md`) — behaviour that finds its occasion by naming a State directly rather than by being wired to a Transition.

### The population, corrected

The ticket treated nine sites as one population. Reading them, they are four kinds, and the distinction carries the whole answer:

| Kind | Sites | How it keys |
|---|---|---|
| Transition-triggered, on destination State | `NegotiationStatusChangeHandler`, `NegotiationSubmissionHandler`, `NegotiationInProgressHandler` | the State a committed move arrived in |
| Transition-triggered, on the `(from, to)` pair | `NegotiationStateChangeWebhookMappingStrategy` | effectively a Transition — the only such site |
| Transition-triggered, no condition | `ResourceStateChangeHandler` | fires on every Resource state change; lifecycle dependence is in its *content* (both States' labels) |
| Creation-triggered, State as a filter | `NewNegotiationHandler`, `NewNegotiationWebhookMappingStrategy` | `NewNegotiationEvent`, suppressed on `DRAFT` |
| **Not this ticket** | `PendingNegotiationReminderHandler`, `NonRepresentedResourcesHandlerImpl` | scheduled / request-driven **queries** over the state column |

**Decision 1 — the scope split.** This ticket decides the first four kinds. The last is not Action-shaped: an Action is by definition "run only after a Transition commits", and these are `findAllByCurrentState`-style filters. They fail differently too — a missing State name makes a trigger silently never fire (a behaviour hole) but makes a query silently return nothing (a data hole, the same shape as network statistics and the JPA filter specs). They join ticket [03](03-state-event-identity-downstream.md)'s population and are decoupled in slab [07](07-decouple-consumers-from-enums.md).

### Decision 2 — no notification becomes an Action

**The divergence hazard the ticket cites hits none of the transition-triggered sites.** Three key on **Negotiation** State names, and ADR 0004 speaks of "the *single* Negotiation-scope definition" — those names are exactly as stable as the enum was. The one Resource-scope site, `ResourceStateChangeHandler`, keys on no name at all. Family divergence bites only one hop away, inside `ResourceNotificationService`, which is where ADR 0007 already claims it.

Four supports, in descending weight:

1. **ADR 0006 already ruled this way for the one notification it examined.** `NOTIFY_IR_AUDIENCE` is "a built-in post-commit stage, **not** an admin-wired Action", because a configurable version's fatal case is "an admin who … forgets a row, and produces a requirement nobody is ever told about". A researcher never told their request was declined is the same failure with a different audience.
2. **An Action would not remove the switch.** `NegotiationStatusChangeHandler.createStatusChangeMessage` switches on the State to pick *message text*. Wired per-Transition, that text has to become an Action param — message templates in jsonb, a feature nobody has asked for. Without it, the Action body still switches on State.
3. **ADR 0009's seed has no source to be faithful to.** The frozen v1 seed's correctness claim is being "a faithful transcription of the current two configuration classes". Notification wiring is not in those classes.
4. It expands a stage whose gate is characterized parity (standing decision 1).

**Sub-question 2 is therefore answered in the negative-by-default: the `NotificationStrategy` mechanism survives whole.** No second mechanism, nothing retired.

### Decision 3 — `NegotiationInProgressHandler` splits, via a new event

`ResourceNotificationService.handleResourceStateManagement` fuses spawn and notification in one loop: per Resource with no state yet it assigns `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE`, accumulating the contacted Resources' representatives into one set, then sends **one** notification to that union (and none when the set is empty). ADR 0007 takes that method for `SPAWN_RESOURCE_LIFECYCLES`.

**The Action spawns and publishes one `ResourceLifecyclesSpawnedEvent` carrying the contacted representatives; a `NotificationStrategy` consumes it.** Rejected alternatives:

- **Absorb the notification into the Action.** ADR 0007 lists the Action's job as three things (resolve family, pin version, set initial State); notifying is a fourth, it is I/O, and it moves notification text out of the notification module — where `"New Negotiation Request"` is already shared with `UpdatedResourcesHandler`.
- **Publish per-Resource `ResourceStateChangeEvent`s and let existing handlers pick them up. This breaks parity.** The freeze slab pinned that **spawn publishes no `ResourceStateChangeEvent` at all** today — three Resources change State and nothing is announced. Turning that on wakes `ResourceStateChangeHandler`, which has no firing condition, *and* the webhook subsystem: N extra notifications and N extra webhook deliveries per approval.

The new event also lands the timing where it already is. Today spawn runs in a **separate transaction after** approval commits (`NotificationListener` is a `@TransactionalEventListener`; `NegotiationInProgressHandler` is `@Transactional`, opening a new one). ADR 0007 moves spawn *into* the approval transaction, which is deliberate; with a published event the notification still fires post-commit through the same dispatcher, so only spawn's atomicity changes.

**Accepted cost: ADR 0007's Action is not a closed box** — it contains a publish, and the "who did we contact" set leaves it as event payload.

**One consequence this inherits from ticket 01, not created here.** Ticket 01 pinned that spawn "keys on arriving at `IN_PROGRESS` rather than on `APPROVE`", and that `PAUSED --UNPAUSE--> IN_PROGRESS` spawns exactly as `APPROVE` does. ADR 0007 wires `SPAWN_RESOURCE_LIFECYCLES` to "the sole Negotiation definition's approval Transition" — one row — so after the cutover neither spawn *nor* this notification fires on unpause. On today's data that is invisible: by unpause every Resource already has a State, the loop skips them all, the contacted set is empty and no notification is sent either way. It becomes visible only for a Resource linked while the Negotiation was already running, which is exactly ticket [05](05-resources-added-to-running-negotiation.md)'s question. **Flagged for 05, not decided here** — routing notification through the Action's event means whatever 05 decides about spawn's trigger automatically governs the notification too, which is the reason this split is preferable to a second independent listener on `IN_PROGRESS`.

### Decision 4 — a missing Well-known State is a silent no-op

No warning log, no boot-time validation. `NegotiationStatusChangeHandler`'s `default -> {}` **already** notifies nobody for 4 of the 8 Negotiation States (`APPROVED`, `CONCLUDED`, `PAUSED`, `DRAFT`), so a missing name landing in the same branch is the shipped failure mode, not a new one — and a warning would make those four existing States noisy on every pause for no gain.

Boot-time validation is the wrong instrument regardless of appetite: a new Definition Version is activated at **runtime**, so a startup check passes and is then falsified by the next publish. The moment that can actually refuse is version activation, which is a stage-3 write path that does not exist yet. In stage 1 the risk is nil by construction — ADR 0009's seed is a faithful transcription, so all five names are present.

### Decision 5 — `WellKnownNegotiationStates`

The five names notification and webhooks depend on: **`DRAFT`, `SUBMITTED`, `IN_PROGRESS`, `DECLINED`, `ABANDONED`**. One holder, in one place, naming only what these consumers need — not a re-declaration of the graph.

Rejected: **keeping a demoted `NegotiationState` enum** (~60 main files and 26 test files reference that type, so preserving the name invites them all to keep depending on it and the decoupling never happens — and it prejudges ticket 03 in the direction its premise resists); and **inline string literals** (the freeze slab already showed the cost — `"Next Lifecycle event"` at `ResourceItem.vue:108` is load-bearing and nothing pinned it until ticket 10).

The name is deliberately *unlike* `NegotiationState` so a stale import cannot silently resolve against it. **The holder is provisional and disposable:** if ticket 03 lands a repo-wide constants holder, these five names fold into it and the type dies.

Note for the record: **parametrizing an Action cannot avoid naming a State.** ADR 0002 does give Actions typed jsonb params — its own example collapses three post-visibility Action classes into one `SET_POST_VISIBILITY` with a scope and a flag — but what removes the name from Java is the **Wiring row on a Transition**, not the params. Params say *how*, the row says *where*. A State name in params would be legal (it is configuration, not the runtime domain state 0002 forbids there) and would buy nothing but moving `"DECLINED"` from a `case` label into jsonb. So an effect either knows a Transition (Action, wired, name-free) or knows a State name (listener). Five names in Java is the accepted price of decision 2.

### Decision 6 — both webhook mapping strategies stay listeners, conditions unchanged

`NegotiationStateChangeWebhookMappingStrategy` is the only site keying on a Transition rather than a State, so it is the one place the Action registry would fit naturally — but it has a **fallback branch firing on every other transition**. As a wired Action that needs a row on all eight Transitions to preserve `NEGOTIATION_STATE_UPDATED`, and a missing row silently stops delivery to an external subscriber: ADR 0006's forgotten-row failure with a paying consumer on the other end.

**The `(DRAFT → SUBMITTED)` pair stays as the condition.** Re-keying it on the Event name `SUBMIT` was considered and rejected on faithfulness grounds: it is behaviour-identical on the current graph (exactly one `SUBMIT` transition, and `NegotiationStateChangeEvent` already carries `getEvent()`) and arguably the better semantics — `NEGOTIATION_ADDED` is about the *submitting*, not the destination — but it is a deliberate divergence in intent against a parity gate, and it would cost a second holder for Event names. The fork is real and may return: the freeze slab hit its mirror image, noting the creation-date reset "keys on the State arrived in, not the Event. Not separable today, because exactly one Transition targets `SUBMITTED`; if ADR 0009's seed adds a second, this becomes two distinct behaviours."

Webhook **payload** types are not this ticket's — ticket 03 claims them under "External contract", with the useful fact that enums already serialize as JSON strings, so the wire format survives.

### Decision 7 — `ResourceStateChangeHandler` needs no change here

No name dependency to remove. Its need for State **labels** (`event.getFromState().getLabel()`) is ticket [04](04-global-state-event-metadata-contract.md)'s metadata contract; after the cutover those come off the `State` row, as ADR 0008 renders timeline text "live by join".

### Consequences for the rest of the map — worth more than the decision

- **This ticket narrows ticket 03 the *opposite* way to how it predicted.** It expected handlers to become Actions and so leave the ~60-file problem. Almost nothing moves: notification and webhooks contribute **five Well-known State names**, and the two query-driven sites *enter* ticket 03's population rather than leaving it.
- **Against standing decision 2's split, notifications and webhooks can be decoupled early.** They need no Action registry, no schema and no evaluator — so they belong in the decoupling slab, not after it. Only the spawn split waits, and that is ADR 0007 coupling-slab work.
- **A parity hazard for the coupling slab: spawn must not begin publishing `ResourceStateChangeEvent`.** Added to the map's fog.
- **Publish-time validation of Well-known State names** is stage-3 work and is fog, not a ticket: *when* the check runs depends on whether version activation is an API endpoint or a UI flow, which is stage 3's first undecided thing.

No new tickets. Resolving this unblocks ticket [03](03-state-event-identity-downstream.md).
