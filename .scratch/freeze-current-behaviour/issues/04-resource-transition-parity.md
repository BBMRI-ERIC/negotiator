# Resource transition and authority parity, including the IN_PROGRESS gate

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

The same treatment as the Negotiation graph, for the larger Resource graph — and it behaves
differently in three ways that all need pinning.

**The graph.** Every transition by from-state, event and to-state, across the thirteen transitions
covering contact, availability checking, access conditions and delivery.

**Authority, evaluated differently.** Unlike the Negotiation machine, the Resource machine does not
enable Spring Statemachine's security at all; its service reimplements rule evaluation imperatively
against three rule names — admin, representative of the Resource, and creator of the Negotiation.
That implementation has sharp edges: a `NullPointerException` while resolving the caller falls back
to a creator id of zero, a `ClassCastException` returns false, and an entirely absent
`Authentication` is treated as satisfying the admin rule. Pin the observable outcomes of all of it.
Do not tidy any of it.

**Refusal is silent.** Where the Negotiation service throws, the Resource service returns the
unchanged current state and raises nothing. This asymmetry is real, load-bearing for callers, and
must be frozen explicitly so that making the two consistent later is a decision rather than an
accident.

**The IN_PROGRESS gate.** No Resource event is available unless the parent Negotiation is
IN_PROGRESS — enforced imperatively in the service, returning an empty set otherwise. Pin it from
both sides: available events for a Resource in an IN_PROGRESS Negotiation, and empty for every other
Negotiation state.

That gate is also where this slice depends on the dump. If the dump shows
`NegotiationIsApprovedGuard` is attached to no transition, then it never fires, and the parent
ticket's requirement to pin "every guard outcome, including `NegotiationIsApprovedGuard` in both
directions" is satisfied by pinning this imperative gate instead — with the dead Guard recorded as a
finding so nobody reimplements it in ADR 0002's registry. If the dump shows it *is* attached, pin the
guard in both directions as originally written.

## Acceptance criteria

- [ ] Every transition of the Resource graph is pinned by from-state, event and to-state.
- [ ] Available events are pinned per reachable state for an admin caller.
- [ ] Available events are pinned per reachable state for a representative of the Resource.
- [ ] Available events are pinned per reachable state for the Negotiation's creator.
- [ ] Available events are pinned for a caller with none of those relationships.
- [ ] Available events are pinned as empty for every parent Negotiation state other than IN_PROGRESS.
- [ ] Available events are pinned as non-empty for an IN_PROGRESS parent, given a suitable caller.
- [ ] Sending an unavailable event is pinned as returning the unchanged current state and raising
      nothing, with the asymmetry against the Negotiation service noted in the test's name or comment.
- [ ] The absent-`Authentication`-counts-as-admin behaviour is pinned.
- [ ] Available events for a Resource not linked to the Negotiation are pinned as an empty set.
- [ ] The `NegotiationIsApprovedGuard` question is resolved against the dump's finding, and this
      issue records which of the two paths was taken.
- [ ] All assertions on post-send state use Awaitility with a bounded timeout.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [Lifecycle graph dump generator and frozen v1 artifacts](01-graph-dump-generator.md)
- [String-keyed lifecycle test adapter and forbidden-import guard](02-string-adapter-and-import-guard.md)

## WIP — halted mid-implementation, and incomplete

An agent was implementing this ticket and was killed by a session limit while it was still adding
refusal cases. **The tests have never been executed and the ticket was not finished.** Nothing was
merged.

- Branch `worktree-agent-a3dab4653dd6c2484`, commit `7f0226c8`, based on `556958f7` (which already
  carries tickets 01 and 02).
- Adds `ResourceTransitionParityTest` and `ResourcePossibleEventsAuthorityTest` under
  `characterization/service`. No findings were written and no criteria were ticked.
- Its last action was described as "adding one more refusal edge: sending to a Resource with no
  recorded State" — so at minimum that case, and everything after it in the criteria list, is
  missing.

**To resume:** land ticket 03 first (it adds a shared `NegotiationGraphV1` helper to the same
package). Then check this branch out, run the suite, work through the acceptance criteria above from
scratch rather than trusting coverage to be complete, and record the findings — including the
`NegotiationIsApprovedGuard` path, which ticket 01 already settled as *dead code, take path two*.
