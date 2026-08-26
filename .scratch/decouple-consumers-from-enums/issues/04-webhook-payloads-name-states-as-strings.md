# Webhook payloads name States as strings

Status: ready-for-agent

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

- [ ] The three payload types and both mapping strategies name no Lifecycle enum.
- [ ] A test asserts the serialised JSON of a state-change delivery — field names and string values —
      and would fail if either changed.
- [ ] A test covers the added-Negotiation delivery the same way.
- [ ] Both mapping strategies' firing conditions are behaviourally unchanged, including the fallback
      branch that fires on every other transition.
- [ ] Schema metadata reads as a string type and keeps a worked example on every State and Event
      field.
- [ ] The existing webhook mapper and listener integration tests are extended rather than replaced.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**The fallback branch is why the state-change strategy stayed a mapping strategy** rather than
becoming an Action. Ticket 02 recorded that wiring it as an Action would need a row on all eight
Transitions to preserve today's delivery, and a forgotten row silently stops delivery to a paying
external subscriber. Leave the shape alone.

**Two test files churn with this slice** — the webhook mapper unit test and the webhook listener
integration test. They change here, not in a lump at the end.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
