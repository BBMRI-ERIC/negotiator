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

## WIP — halted mid-implementation

An agent implemented this ticket but was killed by a session limit just before it ran the suite.
**The tests have never been executed.** Nothing was merged.

- Branch `worktree-agent-a1bde8bb418aa19fd`, commit `234c7325`, based on `556958f7` (which already
  carries tickets 01 and 02, so it cherry-picks cleanly onto the slab branch).
- Adds `NegotiationTransitionParityTest`, `NegotiationAuthorityParityTest`,
  `NegotiationDraftReachabilityTest` and a shared `NegotiationGraphV1` helper, all under
  `characterization/service`.
- That commit also carries its own drafted version of this ticket file, with criteria ticked and
  findings written. **Those ticks are unverified** — the agent ticked them before running anything.
  Treat the drafted version as a claim to check, not as a record.

**To resume:** check the branch out, run
`scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'`, fix whatever is
red, re-verify each acceptance criterion against actual output, then cherry-pick onto
`feat/state-machine-implementation`.

**Land this before ticket 04** — `NegotiationGraphV1` is a shared helper in the package ticket 04
also writes into, so merging 04 first invites a conflict.
