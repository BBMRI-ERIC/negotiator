# Event seam: spawn, conclusion, and notification firing conditions

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin everything that happens *because* of a transition rather than *in* it, observed through recorded
application events rather than through delivered mail. This is the likeliest silent breakage of the
enum removal, and no ADR owns it.

**The state change events themselves.** Each transition publishes an event carrying the origin state,
destination state and the triggering event. Pin those payloads for both graphs — they are the
contract every handler and the webhook subsystem reads.

**Spawn.** A Negotiation reaching in-progress initialises its Resources and notifies their
representatives, via the handler that keys on the destination state. Pin that the Resources start in
the initial Resource state and that representatives are notified. ADR 0007 relocates this into a
spawn Action and needs an equivalence check.

**Conclusion.** Once every Resource in a Negotiation is delivered or unavailable, a listener concludes
the Negotiation as the system user, after commit and in a new transaction.

The exact predicate matters more than the description. It counts only two Resource states —
delivered, and unavailable — and it does **not** count the not-made-available state or the
unavailable-but-willing-to-collect state, despite both reading like terminal unavailability. Pin the
predicate against every terminal Resource state individually, so ADR 0007's terminal aggregation Guard
is configured against observed behaviour rather than against what the state names suggest.

**Handler firing conditions.** Only the handlers that actually key on lifecycle identity are in scope.
Of the eight notification strategies, five do: the in-progress handler, the submission handler, the
status-change handler, the resource-state-change handler, and the pending-reminder handler, which keys
on the representative-contacted state. The post handler and the updated-resources handler do not key
on state, and the new-negotiation handler keys on a non-lifecycle event. The parent ticket says seven
handlers; five is the accurate count and only those are pinned.

Note while you are here: the conclusion listener carries both an event-listener and a
transactional-event-listener annotation on the same method. Whether that causes double invocation is
an observable question — pin whatever actually happens and report it. Do not fix it.

## Acceptance criteria

- [ ] The Negotiation state change event payload is pinned for a real transition: origin state,
      destination state, triggering event, Negotiation.
- [ ] The Resource state change event payload is pinned equivalently, including the Resource.
- [ ] Reaching in-progress is pinned as initialising the Negotiation's Resources to the initial
      Resource state.
- [ ] Reaching in-progress is pinned as notifying the Resource representatives.
- [ ] Driving every Resource to delivered is pinned as concluding the Negotiation.
- [ ] Driving every Resource to unavailable is pinned as concluding the Negotiation.
- [ ] A mix of delivered and unavailable Resources is pinned as concluding the Negotiation.
- [ ] Each remaining terminal Resource state is pinned individually as **not** counting toward
      conclusion, including not-made-available and unavailable-but-willing-to-collect.
- [ ] A Negotiation with any non-terminal Resource is pinned as not concluding.
- [ ] Conclusion is pinned as performed as the system user, not the caller.
- [ ] Each of the five lifecycle-keyed handlers is pinned as firing on its triggering condition and
      not firing otherwise.
- [ ] Handler behaviour is observed through recorded application events, with no dependency on SMTP.
- [ ] Whether the double-annotated conclusion listener runs once or twice is established and reported
      in this issue.
- [ ] All assertions use Awaitility with a bounded timeout.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
