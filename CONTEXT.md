# Domain vocabulary

The shared language of the Negotiator lifecycle state machine. Code, tests, and conversation should
use these terms as defined here.

## Lifecycle state machine

- **Negotiation lifecycle** — the state machine governing a Negotiation as it moves through
  `DRAFT → SUBMITTED → IN_PROGRESS → …` (paused, abandoned, concluded, declined). Its transition
  table and wiring live in `lifecycle/negotiation`.
- **Resource lifecycle** — the state machine governing a single Resource within a Negotiation as it
  moves from `SUBMITTED` through representative contact, availability checks, and access conditions.
  Its transition table and wiring live in `lifecycle/resource`.

## Transition machinery

- **Transition Executor** (`TransitionExecutor`) — the deep module over the underlying stateless4j
  library. Its public interface is two methods: `fire(currentState, event, context)`, which returns
  a **Transition Outcome**, and `permittedEvents(currentState, context)`, which lists the events
  whose guards pass for that context. Both answer through the same guard evaluation, so listing and
  firing cannot drift. stateless4j is an implementation detail confined behind this seam (the
  ArchUnit-enforced `lifecycle.statemachine` package); callers never see a library type. The
  execution order of a successful transition is **machine guard → guard → precondition → transition
  action → persist transition listener → outcome**.
- **Transition Table** (`StateMachineDefinition`) — the declarative list of `TransitionDescriptor`s
  (source state, event, target state, optional action/guard) plus the machine-level slots (optional
  **Machine Guard** and **Precondition**) that define which events are valid from which states and
  who may fire them. The executor checks event validity against this table before firing, so an
  undefined event fails at the same site it is detected.
- **Guard** (`Guard`) — a named permission adapter evaluated by the executor for both listing and
  firing: "may *this caller* fire *this transition*?" Returns `false` to deny. A denied transition
  is excluded from `permittedEvents` and raises **Transition Denied** on `fire`. Guards read
  identity (roles, user id) from the Transition Context and data from injected repositories; a
  missing or unresolvable identity always denies. Adapters live in the per-machine packages
  (`lifecycle/negotiation`, `lifecycle/resource`) or `lifecycle` when shared.
- **Machine Guard** — a Guard attached to the whole Transition Table rather than one transition; it
  gates every event of that machine, for listing and firing alike (negotiation: `isCreatorOrAdmin`;
  resource: `negotiationInProgress`).
- **Precondition** (`Precondition`) — the loud, firing-only counterpart of a guard: "not yet — do X
  first." `check(context, event)` throws `TransitionPreconditionException` with a user-actionable
  message. Preconditions are *not* evaluated in listing, so an event with an unmet precondition
  stays discoverable (resource machine: `infoRequirementMet`).
- **Persist Transition Listener** (`TransitionListener`) — the commit step of a transition: it
  persists the state change and publishes the state-change event, synchronously, inside the caller's
  transaction, before `fire` returns. A failure here rolls back the transaction; no transition can
  report success while its write failed.
- **Transition Outcome** (`TransitionOutcome`) — the value returned by `fire`: the source state,
  target state, event, and context of a completed transition. Callers read the target state from the
  outcome rather than re-reading the database.

## Error contract

The seam (`lifecycle.statemachine`) owns a three-type family of transition failures. Business
failures thrown by transition actions or the persist transition listener propagate untouched.

- **`InvalidTransitionException`** — *structural*: the event is undefined from the current state in
  the Transition Table (stale UI, wrong state). Maps to HTTP 403. Nothing to do but refresh.
- **`TransitionDeniedException`** — *identity*: a guard (machine or transition) denied the caller.
  Maps to HTTP 403. Messages are generic, built from state/event names.
- **`TransitionPreconditionException`** — *world-state, actionable*: a precondition refused the
  firing with a user-facing "do X first" message (e.g. submit the required form). Maps to HTTP 400.
  The event remains listed in `permittedEvents`.
