# The Information Requirement Built-in Stage

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The second pipeline stage, and the one that is structural rather than configured.

The Transition Evaluator always evaluates the firing Event's Information Requirements, derived from
the Event-to-Requirement association at evaluation time. It sits **between Required Authority and the
Guard chain**, refusing with the unmet-requirement category — the middle of the three monotonic
categories, which is what keeps the category a caller sees from flip-flopping depending on where a
Guard happens to be wired.

**There is no Wiring row for it anywhere.** No admin can omit it and no newly added Transition can
miss it. It is deliberately not a registry Guard type: a wireable Guard would have to be attached to
each Transition by hand, and one forgotten row silently reintroduces the dead-click bug this design
exists to remove. It still speaks the Guard contract — a result with a reason code and the missing
forms in its details — so a caller sees one uniform list of failures.

Satisfaction reaches the stage through an **injected port with a test double**. Audience resolution
and Quantifier counting are a later slab's; this slice fixes the port's shape and nothing behind it.
Recon §5 records what today's check actually does, which is much weaker than its name suggests, so
the eventual implementation is a real strengthening rather than a reproduction.

The structural claim is the point of this slice, so it gets a test: adding a Transition or an Event to
a fixture graph cannot skip the stage, and no configuration the graph can express omits it.

## Acceptance criteria

- [ ] The stage runs for every Event with a Requirement, from every State that Event fires from, with
      no Wiring row involved.
- [ ] It runs after Required Authority and before the first Guard, demonstrated by a case that would
      fail both authority and the requirement.
- [ ] An unsatisfied requirement refuses with the unmet-requirement category.
- [ ] The refusal carries a reason code and names the missing forms in its details.
- [ ] The refusal is shaped like a Guard failure, so a caller reads one uniform list.
- [ ] Satisfaction is asked of an injected port; the evaluator gains no repository and no new
      dependency that reads data.
- [ ] The stage is **not** in the Guard registry, and a test asserts no registry lookup can reach it.
- [ ] A fixture graph extended with a new Transition still gates that Event's requirement, with no
      configuration change.
- [ ] Possible Events omits an Event blocked only by an unmet requirement.
- [ ] Slice 02's agreement test between `mayFire` and Possible Events still holds with requirements
      in play.
- [ ] The port's javadoc names what implements it later, and that Audience and Quantifier are out of
      scope here.
- [ ] No production code calls the evaluator.
- [ ] Full backend suite green; parity **255/24/1 skipped**; deltas **8/0/0/0**.

## Notes

**Independent of slice 06**, which widens the other half of the same path — this one adds a pipeline
stage, that one extends the permitted outcome. Either order; no dependency either way. But **test runs
must be serialized**, because two concurrent Maven invocations against `backend/` present as roughly
150 bogus failures.

**This is the slice that fixes a live UX bug, eventually.** Today enforcement happens in a service
method the Possible Events listing knows nothing about: an Event is listed, the user clicks it, and
the submission fails because a form is unfilled. Nothing here reaches a user, because nothing calls
the evaluator yet — but the shape that removes the bug is fixed here.

**Do not make it a registry Guard "for symmetry".** That trade is exactly what ADR 0005 rejects, and
the reason is in the ADR. If a later slice finds the built-in stage awkward, the awkwardness is the
price of the guarantee.

**Four of the eight intended-delta tests are the ones designed to invert at cutover**, and the
requirement behaviour is among them. They must stay green here: this slab changes nothing they
observe.

## Blocked by

- [03 Guards enter the pipeline](03-guards-enter-the-pipeline.md)
