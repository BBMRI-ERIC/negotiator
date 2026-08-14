# Information Requirement gate parity

Status: ready-for-human

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin the Information Requirement gate exactly as the Resource lifecycle service enforces it today,
because ADR 0005 turns this into a Built-in Stage and needs to reproduce the same block.

Today the check happens at the very top of sending a Resource event, before availability is even
consulted: if any Information Requirement exists for the event being sent, and no Information
Submission exists for that Resource and Negotiation pair, the send is refused.

Two details matter more than they look.

The refusal is a `StateMachineException` — a Spring Statemachine type that ceases to exist after the
cutover. Its message is user-facing and the frontend surfaces it. Pin the message text and record
that the type must change, so whatever replaces it is a deliberate choice.

The submission check is **not** scoped to the requirement. It asks only whether *any* submission
exists for that Resource in that Negotiation, so a submission against a different requirement
satisfies the gate for all of them. Pin that as it is — it is very likely a bug, and freezing it is
precisely how the cutover avoids changing it by accident.

Note also that this gate is evaluated ahead of the availability check, so a blocked-by-requirement
event and a not-currently-available event produce different outcomes: the former throws, the latter
silently returns. Pin both orderings.

## Acceptance criteria

- [x] Sending a Resource event with a matching Information Requirement and no submission is pinned as
      refused.
- [x] The refusal's exception type and full message text are pinned.
- [x] The same send succeeds once a submission exists for that Resource and Negotiation.
- [x] Sending an event with no Information Requirement attached is pinned as unaffected by the gate.
- [x] The unscoped-submission behaviour is pinned: a submission against a *different* requirement
      satisfies the gate.
- [x] The gate's precedence over the availability check is pinned, showing that a
      requirement-blocked event throws while a merely unavailable event returns silently.
- [x] A note records that the exception type must change at cutover, since the current type is a
      Spring Statemachine class.
- [x] Every State and Event is named as a string; the forbidden-import guard passes, with the
      exception type referenced in a way that does not import a Spring Statemachine class into the
      characterization tree.
- [x] No production code is modified.

## Blocked by

- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)

## Resolution

`ResourceInformationRequirementGateTest` (11 tests) in
`backend/src/test/java/eu/bbmri_eric/negotiator/characterization/service/`, plus four SQL helpers
added to the shared `SeededResourceSubject`. Full parity gate:
**168 tests, 0 failures, 0 errors, 1 intentional skip** (was 157 + 1 skip).

Per criterion:

| Criterion | Test |
|---|---|
| refused with a Requirement and no Submission | `offeredEvent_withAnUnmetRequirement_isRefused` |
| type and message pinned | `theRefusal_isUncheckedAndIsNoneOfTheServicesOwnRefusalTypes`, `theRefusal_reachesTheCallerAsBadRequestCarryingTheMessage` |
| succeeds once a Submission exists | `unmetRequirement_isMetByASubmission` |
| unaffected without a Requirement | `eventWithNoRequirement_isNotGated` |
| unscoped Submission check | `submissionAgainstADifferentRequirement_satisfiesTheGate` (+ its boundary, `submissionForADifferentResource_doesNotSatisfyTheGate`) |
| precedence over the availability check | `unmetRequirement_outranksTheAvailabilityCheck` (3 rows) and `unmetRequirement_outranksTheParentNegotiationGate` |

## Findings

### 1. The exception type, recorded here because no test may name it

The before-picture, verbatim, so the cutover chooses its replacement deliberately:

- thrown at `ResourceLifecycleServiceImpl.java:113-115` as
  `org.springframework.statemachine.StateMachineException`;
- **it extends `org.springframework.dao.NonTransientDataAccessException`**, i.e. it is a Spring
  *data access* exception, not a domain one. Any `catch (DataAccessException)` on the path would
  swallow the gate's refusal, and the exception's ancestry has nothing to do with the reason it is
  raised. This is the strongest argument for changing the type rather than merely renaming it;
- mapped in `NegotiatorExceptionHandler.java:441-447` by
  `@ExceptionHandler(StateMachineException.class)` + `@ResponseStatus(HttpStatus.BAD_REQUEST)`, to a
  `ProblemDetail` with title `"Could not advance the state machine"` and detail = the exception
  message.

Following ticket 09's cross-ticket fix, no assertion embeds the class name — it is a guaranteed
delta dressed as parity. What is pinned instead is what a caller observes: the message verbatim,
that the throwable is unchecked and causeless, that it is none of `EntityNotFoundException`,
`ForbiddenRequestException` or `WrongRequestException`, and the HTTP 400 + title + detail the
frontend reads. **The pinned title is engine-flavoured prose** ("Could not advance the state
machine"); if the cutover wants to reword it that is fine, but it must be a decision that breaks
this test rather than a drift.

The message text itself, verbatim:

> The requirement for this operation was not met. Please make sure you have submitted the required
> form and try again.

### 2. The gate outranks more than the availability check

The ticket describes the gate as sitting ahead of the availability check. It sits ahead of
*everything* — it is the first statement of `sendEvent`, before the Resource's State is even read.
Pinned consequences beyond the ticket's description:

- it outranks the **parent-Negotiation IN_PROGRESS gate**: with the Negotiation outside
  `IN_PROGRESS`, where no Event is offered at all and every send is a silent no-op, an unmet
  Requirement still throws;
- it outranks **"this Event has no Transition anywhere"**: a Requirement recorded for `OVERRIDE` or
  `RETURN_FOR_RESUBMISSION` — the two published Events ticket 04 found carry no Transition — turns
  those sends from a silent no-op into a user-facing error. Recording a Requirement for an Event
  that can never fire is a way to make the Lifecycle produce an error message for an Event that
  never had any effect;
- it outranks the **Required Authority rules**: a caller with no authority for the Event gets the
  Requirement error rather than the silent refusal, which leaks that a Requirement exists to callers
  who could not have fired the Event.

### 3. The Requirement lookup is global, and that is a test-ordering hazard

`existsByForEvent(event)` is scoped by nothing but the Event — not the Resource, not the
Negotiation, not the Access Form. One row anywhere in `information_requirement` blocks that Event
for every Resource of every Negotiation in the deployment.

Operationally this makes `@DirtiesContext(AFTER_EACH_TEST_METHOD)` load-bearing for this class in a
second way beyond the ordering rule: a leaked Requirement row would block that Event for the whole
rest of the run, not just for the subject. The seed carries **no** `information_requirement` or
`information_submission` rows, so every fixture here is inserted by the test.

### 4. The requirement-scoped query already exists and is deliberately not called

`InformationSubmissionRepository` declares **both**
`existsByResource_SourceIdAndNegotiation_Id` (what the gate calls) and
`existsByResource_SourceIdAndNegotiation_IdAndRequirement_Id` (what it would need). The scoped one
is used by `InformationSubmissionServiceImpl.java:76`. So the unscoped check is one method call away
from being correct, which makes it much more likely to be "fixed" by accident during the cutover —
exactly what pinning it prevents. ADR 0005's Built-in Stage should treat the change as a decision
with a migration story, not as a typo.

The gate's scoping asymmetry, precisely: **unscoped** in the Requirement dimension, **scoped** in the
Resource `source_id` and Negotiation id dimensions. Both halves are pinned.

### 5. The gate covers `sendEvent` only

`ResourceServiceImpl.updateResourceStatus` (ticket 04's finding 10) writes a Resource State straight
onto the link row and publishes a `ResourceStateChangeEvent` stamped `OVERRIDE`, without going
through `sendEvent` — so it passes no Information Requirement gate at all. Whether the Built-in Stage
applies to that path is an open question for ADR 0005, and nothing pins it today. Deliberately left
out of scope here, as in ticket 04.

### 6. Mechanics worth reusing

- Requirement and Submission fixtures are written with SQL through the extended
  `SeededResourceSubject` (`requireInformationFor`, `submitInformationFor` ×2,
  `ANOTHER_RESOURCE_ROW_ID`). SQL rather than the JPA repositories because
  `InformationRequirement.forEvent` is one of the four deleted enums — an entity-level fixture could
  not be written without naming it, and the `for_event` column takes the Event name as a plain
  string.
- The HTTP-mapping test lives in the `service` package with the rest of the class rather than under
  `rest`, because `SeededResourceSubject` is package private in `service`. The class therefore
  carries `@AutoConfigureMockMvc` alongside the service-seam tests.
- `@WithUserDetails("TheBiobanker")` (Person 109) is the caller for the HTTP test: the endpoint
  `PUT /v3/negotiations/{id}/resources/{resourceId}/lifecycle/{event}` has its own
  representative-or-creator guard in `NegotiationController` ahead of the service, so an admin token
  alone would be rejected before the gate is reached.
