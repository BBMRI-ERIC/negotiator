# NEGOTIATION_APPROVED and SET_POST_VISIBILITY

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The first two ported strategies — the two that need no definition source. Read
[recon-strategies.md](../recon-strategies.md) §3, §4 and §7 before starting; both of these are
described wrongly by the parent ticket.

**`NEGOTIATION_APPROVED`** — a Guard, definition-level, no params. It gates any interaction with a
Resource Lifecycle on the parent Negotiation being in progress. It reads the parent Negotiation's
current State **off the evaluation context**, so it holds no port and does no I/O — which is the
reason the parent State belongs on the context rather than behind a lookup.

It is ported from the **imperative gate in the Resource lifecycle service, not from the Guard bean**.
That bean is attached to nothing, has never influenced a single decision the application has made,
and the characterization suite says outright that a Guard which has never fired must not be
reimplemented in the new registry. The rule it names is live; the class is not. Record that decision
where the strategy lives, so a later reader does not restore the bean as an apparent omission.

**`SET_POST_VISIBILITY`** — an Action with params: a scope of public, private or both, and a boolean.
One strategy class, three configured instances, following the static-factory shape the webhook
subsystem already uses for its parameterized strategy. The three instances reproduce today's three
Action classes one-for-one. **The three-valued scope is not decoration**: today's disable-posts Action
sets both flags, so a two-valued scope turns three dump Actions into four wiring rows and the mapping
to the frozen graph stops being legible.

Its write goes through a narrow single-purpose port. Nothing invokes it from production code.

## Acceptance criteria

- [ ] `NEGOTIATION_APPROVED` declares no params type and is wired definition-level in every test that
      exercises it.
- [ ] It permits when the parent Negotiation is in progress and refuses otherwise, with the
      domain-state-conflict category, walked across every State the Negotiation graph declares.
- [ ] It reads the parent State from the evaluation context; it holds no port, no repository and no
      service.
- [ ] It is exercised **only through the evaluator**, over a fixture graph that wires it. No test
      names the strategy class directly.
- [ ] The strategy's javadoc records that the dead Guard bean was read and deliberately not ported,
      and why.
- [ ] `SET_POST_VISIBILITY` declares a params type with a three-valued scope and a boolean.
- [ ] Three configured instances reproduce enable-public, enable-private, and disable-both; a test
      asserts the third touches **both** flags.
- [ ] Its write goes through a narrow port, doubled in tests.
- [ ] Bound params reach the strategy as a typed object, through the binder from slice 04.
- [ ] A permitted outcome reports the Action, and the evaluator does not run it.
- [ ] The Action is exercised directly against its port double, since no evaluator call can reach an
      Action body in this slab.
- [ ] Divergences D1 and D4 from recon §10 are recorded in `STATUS.md` with the cutover slab named as
      owner.
- [ ] No production code calls either strategy.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**The three differences between the dead bean and the live gate**, all in recon §3, and all of them
divergences the cutover slab owns rather than problems to solve here: the imperative gate runs on both
the listing and the fire, it runs *before* authority and yields an empty set where a Guard yields a
category, and a blocked Event is a silent no-op today rather than a refusal.

**Definition-level wiring is what removes copy-drift**, so a Transition added by a later Definition
Version cannot escape the rule. Hardcoding the rule into the evaluator was rejected in ADR 0005,
because a Network may legitimately want Resource work to begin before approval.

**Both instances of each post-visibility Action exist in the Spring context today** — the component
one and the bean one — and the Transitions use the bean instances. Irrelevant to the port, recorded so
a reader comparing counts is not confused.

## Blocked by

- [04 Params bind at load time](04-params-bind-at-load-time.md)
- [06 The Action chain in the outcome](06-action-chain-in-the-outcome.md)
