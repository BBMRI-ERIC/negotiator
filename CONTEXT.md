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
  library. Its entire public interface is one method, `fire(currentState, event, context)`, which
  returns a **Transition Outcome**. stateless4j is an implementation detail confined behind this
  seam (the ArchUnit-enforced `lifecycle.statemachine` package); callers never see a library type.
  The execution order of a successful transition is **guard → transition action → persist transition
  listener → outcome**.
- **Transition Table** (`StateMachineDefinition`) — the declarative list of `TransitionDescriptor`s
  (source state, event, target state, optional action/guard/security attributes) that defines which
  events are valid from which states. The executor checks event validity against this table before
  firing, so an undefined event fails at the same site it is detected.
- **Persist Transition Listener** (`TransitionListener`) — the commit step of a transition: it
  persists the state change and publishes the state-change event, synchronously, inside the caller's
  transaction, before `fire` returns. A failure here rolls back the transaction; no transition can
  report success while its write failed.
- **Transition Outcome** (`TransitionOutcome`) — the value returned by `fire`: the source state,
  target state, event, and context of a completed transition. Callers read the target state from the
  outcome rather than re-reading the database.

## Error contract

- **`InvalidTransitionException`** — a transition-validity failure raised at the executor seam:
  either the event is undefined from the current state in the Transition Table, or a guard denied
  the transition (in which case the underlying library exception is preserved as the cause).
  Distinct from a business failure. Business failures thrown by transition actions or the persist
  transition listener propagate untouched.
