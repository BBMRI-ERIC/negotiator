# String-keyed lifecycle test adapter and forbidden-import guard

Status: resolved

(The five triage roles in `docs/agents/triage-labels.md` all describe work still waiting on someone.
`resolved` is `docs/agents/issue-tracker.md`'s own closing value and is used here so no later agent
picks this ticket up again.)

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

The harness every later slice writes against, plus the mechanism that keeps it honest.

A parity suite that must pass *unchanged* after the cutover cannot name a type the cutover deletes.
Today the lifecycle services take the four enums as arguments; after ADR 0002 those enums are gone,
and the decision about what replaces them has not been made yet.

So build a thin test-scope adapter: one method per lifecycle service operation, **strings in and
strings out**. Its single implementation today converts string to enum and delegates to the real
service. At cutover that implementation is rewritten once and every assertion written against it
stays byte-identical. The adapter is the only test-scope file allowed to name the four enums.

Alongside it, a test that scans the characterization source tree and fails on any import of a Spring
Statemachine type, or of any of the four lifecycle enums outside the adapter itself. Review
discipline is not enough to hold this property across ten slices.

Prove both work end to end with one smoke test that drives a single real Negotiation transition
through the adapter and asserts the resulting state as a string — so this slice lands as a working
vertical slice rather than an unused interface.

The whole suite lives under one test package so the parity gate can be a single selector later.

## Acceptance criteria

- [x] An adapter interface covers the lifecycle operations the suite needs: the available events for
      a Negotiation and for a Resource, and sending an event to each, with and without a message.
- [x] Every parameter and return value naming a State or an Event is a string, never an enum.
- [x] One implementation exists, it converts strings to the current enums, and it is the only
      test-scope file that references them.
- [x] A smoke test drives one real Negotiation transition through the adapter and asserts the
      resulting state as a string.
- [x] A guard test fails on any import of `org.springframework.statemachine..*` anywhere in the
      characterization tree.
- [x] The same guard test fails on any reference to the four lifecycle enums outside the adapter
      implementation.
- [x] The guard test is demonstrated to actually fail when a violation is introduced deliberately,
      then reverted.
- [x] The suite is organised under a single test package addressable as one selector.
- [x] No production code is modified.

## Blocked by

None - can start immediately.

## Notes for downstream tickets

### Package layout

Everything lives under `backend/src/test/java/eu/bbmri_eric/negotiator/characterization/`:

| Package | Contents |
|---|---|
| `characterization/adapter` | `LifecycleTestAdapter` (interface), `EnumBackedLifecycleTestAdapter` (impl, package private), `LifecycleTestAdapterConfig`, `LifecycleTestAdapterSmokeTest` |
| `characterization/guard` | `CharacterizationImportGuardTest` |
| `characterization/dump` | reserved for ticket 01's graph-dump generator - exempt from the Spring Statemachine rule |
| `characterization/delta` | reserved for ticket 10's intended-delta tests |

Add per-seam packages as needed (`characterization/service`, `.../rest`, `.../event`); the guard and
the selector both cover any depth.

**Selector for the whole suite:**

```
/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

### Wiring the adapter into a test

The adapter is published by a `@TestConfiguration`, so it never leaks into any other test's context.
Import it explicitly:

```java
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SomeCharacterizationTest {
  @Autowired LifecycleTestAdapter adapter;
}
```

### The adapter's API surface

```java
Set<String> possibleNegotiationEvents(String negotiationId);
String sendNegotiationEvent(String negotiationId, String event);
String sendNegotiationEvent(String negotiationId, String event, String message);
String currentNegotiationState(String negotiationId);

Set<String> possibleResourceEvents(String negotiationId, String resourceId);
String sendResourceEvent(String negotiationId, String resourceId, String event);
String currentResourceState(String negotiationId, String resourceId);

Map<String, Object> resourceLifecycleDiagram();
```

Behaviour worth knowing before writing assertions:

- **The refusal asymmetry is passed through, not smoothed over.** `sendNegotiationEvent` propagates
  `ForbiddenRequestException`; `sendResourceEvent` raises nothing and returns the unchanged current
  State name. Both stay observable exactly as the services behave today.
- **The Information Requirement gate is passed through too**: `sendResourceEvent` propagates the
  `StateMachineException` today's `ResourceLifecycleServiceImpl` throws. Ticket 05 must assert on the
  thrown message without importing that class - the guard rejects the import. Catch
  `RuntimeException` and assert on the message, or use `assertThatThrownBy(...).hasMessageContaining(...)`.
- **A misspelled Event name fails fast** with `IllegalArgumentException` listing the known names, so
  a typo can never masquerade as a refused Event.
- **`currentNegotiationState`** throws `EntityNotFoundException` for an unknown Negotiation;
  **`currentResourceState`** returns `null` when the Resource has no recorded State yet (seeded rows
  with a `null` `current_state` exist, e.g. on `negotiation-4`).
- **`sendEvent` is asynchronous** (`handleEventWithStateReactively(...).subscribe()`). Assert
  downstream effects with Awaitility and a bounded timeout, re-reading through
  `currentNegotiationState` / `currentResourceState`. Do not put such a read inside a
  `@Transactional` test method - Awaitility polls on another thread and will not see the test's
  transaction.

### What the guard allows and forbids

- Forbidden everywhere except `characterization/dump`: any occurrence of
  `org.springframework.statemachine`.
- Forbidden everywhere except `EnumBackedLifecycleTestAdapter.java`: the four enum names, matched on
  word boundaries.
- **Deliberately still allowed**, because those word boundaries exclude them:
  `NegotiationStateChangeEvent` and `ResourceStateChangeEvent`. Ticket 08 can record them with
  `@RecordApplicationEvents` and read their States as strings via `event.getToState().name()` - the
  enum *type* is never named. Verified while demonstrating the guard.
- Comments are stripped before scanning, so prose may mention a forbidden name.
- The guard exempts its own source file, which necessarily contains every name it forbids. Nothing
  else belongs in that file.
- A third test asserts the tree was actually found and that the exempted adapter file still exists,
  so the guard can never pass by scanning nothing or by exempting a file that was renamed away.

### Ticket 01 note

The dump package exemption covers `org.springframework.statemachine` **only**. The four enums stay
forbidden there too - the dump is specified to be produced by walking the
`StateMachine<String, String>` beans, which are string-typed already.
