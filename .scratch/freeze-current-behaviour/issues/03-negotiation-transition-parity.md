# Negotiation transition and authority parity

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin every transition of the Negotiation graph and the authority rules that gate it, through the
Negotiation lifecycle service via the string adapter.

Two things are being frozen. First the **shape**: for each from-state, which event leads to which
to-state. Second the **authority**: which events the service actually offers, which differs by who is
asking. The service returns an empty set outright to anyone who is neither an admin nor the
Negotiation's creator, and then filters the remaining transitions by matching each transition's
security rule attributes against the caller's Spring roles. Two transitions are `ROLE_ADMIN`-secured.

Also pin refusal. Sending an event that is not currently available raises `ForbiddenRequestException`,
with a message derived from the event's own label. That behaviour is the Negotiation service's, and
it differs from the Resource service's — which is why the two are pinned separately and the asymmetry
is recorded rather than smoothed over.

A table-driven test over the transition set, parameterised by caller role, keeps this from becoming
twenty near-identical methods.

Remember that sending an event drives an asynchronous persist path, so assertions on the resulting
state must be made under Awaitility with a bounded timeout, following the existing lifecycle test's
lead. Never a bare sleep, and never an assertion immediately after the call.

## Acceptance criteria

- [ ] Every transition of the Negotiation graph is pinned by from-state, event and to-state.
- [ ] The set of available events is pinned for each reachable state, as an admin.
- [ ] The set of available events is pinned for the Negotiation's creator.
- [ ] The available-events set is pinned as empty for a user who is neither admin nor creator.
- [ ] The two admin-secured transitions are shown to be offered to an admin and withheld from a
      non-admin creator.
- [ ] Sending an unavailable event is pinned as raising `ForbiddenRequestException`.
- [ ] The exception's message content is pinned, since it is derived from the event label and is
      user-visible.
- [ ] Requesting available events for a non-existent Negotiation is pinned as raising
      `EntityNotFoundException`.
- [ ] Whether `DRAFT` is observably reachable is pinned to match the finding from the dump slice.
- [ ] All assertions on post-send state use Awaitility with a bounded timeout.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [String-keyed lifecycle test adapter and forbidden-import guard](02-string-adapter-and-import-guard.md)
