# Parity gate selector and findings report

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Turn ten slices of tests into the one thing every later slab actually needs: a single command that
answers "is behaviour still identical?", plus the written record of what pinning the behaviour taught
us.

**The gate.** One selector runs the whole characterization suite and nothing else, reports the
intended-delta tests separately from parity tests, and is documented where a later session will find
it — the slab gate of every subsequent map ticket is "this command is green". Verify it runs clean
from a cold start, since the suite depends on the seeded test data being loaded.

**The findings report.** Characterization surfaces things that are true but were not known. Collect
them in one place, with the evidence, as the resolution of the parent map ticket. Everything found so
far is a report, never a fix — that is what makes the suite credible as a before-picture:

- Whether `NegotiationIsApprovedGuard` is attached to any transition, and therefore whether it is dead
  code that must not be reimplemented in ADR 0002's registry.
- Whether `DRAFT` is reachable as an entry state, which ADR 0009's seed must reproduce either way.
- That the Information Requirement gate's submission check is not scoped to the requirement, so any
  submission satisfies every requirement for that Resource.
- That the Negotiation service throws on a refused event while the Resource service silently returns
  the unchanged state.
- Whether the double-annotated conclusion listener runs once or twice.
- That conclusion counts only two of the terminal Resource states.
- That the two graphs evaluate authority through entirely different mechanisms.
- That the abandon transitions from in-progress and from paused are not equivalent.

**Report the coverage honestly.** State what the suite does *not* pin, so a later session does not
over-trust the gate. Known gaps by design: the frontend, the intended deltas, and anything only
reachable through Spring Statemachine internals.

Finally, hand the slab back: this issue's completion is the parent map ticket's resolution, so write
the answer in a form that can be lifted into the map's Decisions-so-far as a single gist plus a link.

## Acceptance criteria

- [ ] A single selector runs the entire characterization suite and no unrelated tests.
- [ ] The selector is verified green from a cold start against the current Spring Statemachine code.
- [ ] Intended-delta tests are reported separately from parity tests.
- [ ] The command is documented where a later slab will find it.
- [ ] Each finding listed above is recorded with its evidence, or explicitly marked as not applicable.
- [ ] Every finding is reported, not fixed; no production code is modified anywhere in the slab.
- [ ] The suite's coverage gaps are stated explicitly.
- [ ] A total count of pinned behaviours is recorded, so a later slab can notice if the suite shrinks.
- [ ] Confirmation that the four lifecycle enums appear in exactly one test-scope file, the adapter.
- [ ] Confirmation that no Spring Statemachine type is imported anywhere in the suite except the
      throwaway dump generator.
- [ ] A resolution gist suitable for the map's Decisions-so-far is drafted in this issue.

## Blocked by

- [Lifecycle graph dump generator and frozen v1 artifacts](01-graph-dump-generator.md)
- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
- [Information Requirement gate parity](05-information-requirement-gate.md)
- [Post side effects of Negotiation transitions](06-post-side-effects.md)
- [Lifecycle history rows for both graphs](07-lifecycle-history-rows.md)
- [Event seam: spawn, conclusion, and notification firing conditions](08-event-seam-spawn-conclusion-notifications.md)
- [REST seam: metadata endpoints and the graph diagram endpoint](09-rest-seam-metadata-and-diagram.md)
- [ADR 0005 intended-delta tests](10-intended-delta-tests.md)
