# Webhook payloads name States as strings

Status: resolved

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The first migrate batch, and the cheapest: five files in the webhook subsystem. Three payload
records carrying a State or an Event field, and the two mapping strategies that build them.

The payload fields become `String`. The two mapping strategies compare against the holders from
slice 1 and translate at the boundary where they read from the change events, which still deal in
enums until slice 10.

**The wire format must not change, and this slice proves it rather than assuming it.** The claim
that "enums already serialise as JSON strings" is a claim about Jackson's behaviour, and this whole
slab's safety rests on it. A subscriber's integration breaks silently if it is wrong.

Both mapping strategies keep their firing conditions exactly as they are. Ticket 02 decided that
question and rejected the alternative — re-keying the added-Negotiation delivery on the Event name
rather than the State pair — on faithfulness grounds against a parity gate. That fork is recorded
and may return; it is not reopened here.

Update the schema metadata: each field goes to a plain string with its example kept for
discoverability. The enumerated-values constraint is lost, which ticket 03 accepted — no subscriber
breaks, because the values on the wire are identical.

## Acceptance criteria

- [x] The three payload types and both mapping strategies name no Lifecycle enum. A whole-word scan
      for the four enum names over `webhook/` comes back empty; the only Lifecycle import left in
      the five files is `WellKnownNegotiationStates`, in the two strategies.
- [x] A test asserts the serialised JSON of a state-change delivery — field names and string values —
      and would fail if either changed. Two, at two heights.
      `WebhookEventMapperTest.map_whenNegotiationStateChangeEvent_serialisesStateAndEventNamesAsJsonStrings`
      compares the whole serialised payload object, and
      `WebhookEventListenerIntegrationTest.publishNegotiationStateChangeEvent_dispatchesToActiveWebhooks`
      compares the whole `data` object of the body WireMock actually received. Comparing the object
      rather than one path at a time is what makes a renamed, dropped or added field fail as well as
      a changed value.
- [x] A test covers the added-Negotiation delivery the same way — at both of its producers, since
      the payload is reached by two different routes: the state-change strategy's `DRAFT` →
      `SUBMITTED` branch and a newly created Negotiation. Four tests, two per height.
- [x] Both mapping strategies' firing conditions are behaviourally unchanged, including the fallback
      branch that fires on every other transition. The conditions are the same two comparisons with
      the enum constants swapped for holder constants; the existing branch tests at both heights —
      `DRAFT` → `SUBMITTED`, the fallback on `SUBMITTED` → `IN_PROGRESS`, and the suppressed `DRAFT`
      creation — still pass unedited.
- [x] Schema metadata reads as a string type and keeps a worked example on every State and Event
      field. Six fields across the three records; each `@Schema` keeps its example and its
      description now says it carries a name. The description rewrite is more than the criterion
      asked for and is deliberate: the published text is what a subscriber reads instead of the
      `enum` constraint this slice gives up, which is PRD user story 15's concern. One neighbouring
      description was also fixed, for the glossary rather than for this slice —
      `backend/CONTEXT.md` binds `_Avoid_: state machine`, and the resource payload's own
      `@WebhookEventDoc` still shipped it.
- [x] The existing webhook mapper and listener integration tests are extended rather than replaced.
      The mapper test goes 10 → 14 tests, with the four pre-existing State-carrying assertions
      keeping their shape and only their expected values becoming strings; the listener test stays at
      9 tests, with four whole-body assertions added to tests that already existed.
- [x] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0. Parity 255 in 24 classes, 0
      failures, 1 skipped; deltas 8/0/0/0, written to the delta report alone; full suite 1457/0/0/16
      in 158 classes, every report rewritten by the run. The count reconciles exactly for once:
      slice 03 recorded 1453, this slice adds four tests and no test file other than the two it was
      expected to touch.

## What resolving it established

**The wire format is now proven rather than argued, and the proof is the order the tests were
written in.** The whole-object JSON assertions were added and run *against the unchanged enum-typed
records* — mapper test 14 green, listener test 9 green — and then run again after the type swap with
the same expected JSON. Two heights, and they are not interchangeable: the mapper test builds its own
`new ObjectMapper()` and so pins the record's serialisation under Jackson's *defaults*, while the
listener test goes through the application's configured mapper and a real HTTP body. **It is the
integration test that discharges the PRD's claim about Jackson's behaviour**; the unit test would not
notice a Spring-level `WRITE_ENUMS_USING_TO_STRING`. Corrected in review, along with a related
overstatement: with that feature unset a `label`-valued `toString` could never have changed the wire
format either, so the live risk was only ever a `@JsonValue` on one of the enums' accessors. See
`STATUS.md` for how far this generalises — after this slice, only the Resource payload still converts
an enum through Jackson at all.

**`NegotiationResourceStateUpdatedWebhookEvent` is built by neither mapping strategy.** It comes out
of `DefaultWebhookMappingStrategy.of(...)` via `objectMapper.convertValue(event, payloadType)`, so
its three fields changed type with no strategy edit at all — Jackson does the enum-to-name step
itself, and the record is the only file that moved. Two consequences worth carrying: this slice
covers three payload records but only two boundaries, and slice 10 will find nothing to delete here
when it flips `ResourceStateChangeEvent`, because there is no translation to delete.

**The translation is `nameOf`, private and duplicated in both strategies.** Null-preserving on
purpose: `NegotiationStateChangeEvent`'s fields come from `valueOf` at its only producer and cannot
be null, but `NewNegotiationEvent` carries `Negotiation.currentState`, which has no non-null
constraint, and today a null there yields a delivery with `"currentState": null` rather than an
exception inside an event listener. Two three-line copies rather than a shared helper, because both
are deleted rather than maintained — see the ownership finding below for which slice deletes which.

**Half of this slice's translation is not slice 10's to delete.** The PRD says slices 04, 05, 08 and
09 write translations at their call boundaries and slice 10 removes them, and that holds for
`NegotiationStateChangeWebhookMappingStrategy`. It does not hold for
`NewNegotiationWebhookMappingStrategy`: `NewNegotiationEvent` is not one of slice 10's four seam
types — it lives in `negotiation/`, not `negotiation/state_machine/` — and no slice in the table
names it. It is forced by slice 11 instead: once `Negotiation.currentState` is a `String`,
`new NewNegotiationEvent(this, id, negotiation.getCurrentState())` in `NegotiationServiceImpl` stops
compiling, so slice 11 converts the event and this strategy's `nameOf` goes with it. Recorded in
`STATUS.md` for slices 10 and 11, since the file is also a decoupling-gate violation nobody owns.

**Ticket 02's fork stayed shut.** The added-Negotiation delivery is still keyed on the `DRAFT` →
`SUBMITTED` State pair rather than on the `SUBMIT` Event name, and the fallback branch still fires on
every other transition. Both firing conditions read as holder-constant comparisons on names and
nothing else changed shape.

## Notes

**The fallback branch is why the state-change strategy stayed a mapping strategy** rather than
becoming an Action. Ticket 02 recorded that wiring it as an Action would need a row on all eight
Transitions to preserve today's delivery, and a forgotten row silently stops delivery to a paying
external subscriber. Leave the shape alone.

**Two test files churn with this slice** — the webhook mapper unit test and the webhook listener
integration test. They change here, not in a lump at the end.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
