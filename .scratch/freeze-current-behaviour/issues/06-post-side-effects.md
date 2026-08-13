# Post side effects of Negotiation transitions

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin what Negotiation transitions do to posts, so the three Action beans can be re-registered in ADR
0002's Action registry with evidence behind them.

Three Actions hang off Negotiation transitions today: submitting enables public posts, approving
enables private posts, and abandoning from in-progress disables posts. Note the asymmetry worth
pinning — abandoning from *paused* has no Action attached, so the two abandon routes are not
equivalent.

Separately, the Negotiation service accepts an optional message alongside an event, and when one is
supplied it becomes a `PUBLIC` post attributed to the caller, with the creation timestamp set at
persist time. Pin that the post is created, its type, its body and its author, and pin that no post
is created when the message is absent or empty.

One more effect belongs here because it is written by the same persist listener: reaching SUBMITTED
resets the Negotiation's creation date.

All of these are observed after an asynchronous persist path, so Awaitility with a bounded timeout
throughout.

## Acceptance criteria

- [ ] Public posts are pinned as enabled after the submit transition.
- [ ] Private posts are pinned as enabled after the approve transition.
- [ ] Posts are pinned as disabled after abandoning from in-progress.
- [ ] Abandoning from paused is pinned as leaving the post flags untouched, recording the asymmetry.
- [ ] Sending an event with a message is pinned as creating exactly one post, of type `PUBLIC`, with
      the given body and the caller as author.
- [ ] Sending an event with no message, and with an empty message, are both pinned as creating no post.
- [ ] Reaching SUBMITTED is pinned as resetting the Negotiation's creation date.
- [ ] All assertions use Awaitility with a bounded timeout.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
