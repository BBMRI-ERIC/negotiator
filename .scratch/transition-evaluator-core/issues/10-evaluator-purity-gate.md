# The evaluator purity gate

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

The slab gate in executable form. The parent ticket states it as a sentence — *pure unit tests, no
I/O, no database* — and this slice makes it a test, so that "pure" is a checked fact rather than a
claim in a commit message, and so the property cannot rot between here and the cutover.

**Four rules, because purity can be broken four ways.**

The *import rule* scans production sources in the evaluation package and fails on any reference to a
Spring Data type, a JPA type, a query annotation, or the Lifecycle Definition package.

The *signature rule* is the one a source scan cannot express, and the reason this slice exists rather
than being a grep in a pull request. Reflectively, the evaluator's constructor parameters are none of
those types either. A component can reach persistence through a method call on an injected
collaborator, importing nothing — which is precisely the failure the decoupling slab hit with its own
table rule and wrote down for the next slab to avoid.

The *caller rule* fails if any production source outside the evaluation package names the evaluator.
This is the "nothing calls it from production code yet" half of the gate, and the thing that lets this
slab claim it changed no behaviour.

The *anti-vacuity rules* keep the other three honest: a scan root that resolves wrongly must refuse
rather than find nothing, and each detector must be shown catching what it forbids. The decoupling
slab hit the vacuity trap three times in three disguises, the last time inside the very slice that had
just fixed it for something else, so a private fixture supplying the shapes each rule must catch is
part of the deliverable rather than a nicety.

Closing this slice closes the slab. Map ticket 09 is then resolved and the map's Decisions-so-far
gains its entry.

## Acceptance criteria

- [ ] The import rule scans all production sources in the evaluation package and fails on a Spring
      Data type, a JPA type, a query annotation, or the definition package.
- [ ] The signature rule covers the evaluator's constructor reflectively, and passes only because the
      parameters are genuinely clean.
- [ ] The caller rule fails if any production source outside the evaluation package names the
      evaluator, and is green with no exemption.
- [ ] A private fixture supplies the shapes each rule must catch — a forbidden type as an import, as a
      constructor parameter, and as a type argument. Nothing implements it and nothing scans it.
- [ ] The scan-root resolver takes its path components as arguments and refuses rather than finding
      nothing, watched by its own test.
- [ ] Every failure path is run red on purpose during development, one injected violation per way each
      rule can fail or go vacuous, and the table is recorded in `STATUS.md`.
- [ ] The guard's failure message names the file and line and says what the reader must do.
- [ ] Any exemption is named in the source with a reason; the target state is zero exemptions.
- [ ] The gate's javadoc states that it outlives the slab and what happens to it at cutover, when the
      evaluator acquires real callers.
- [ ] `DefinitionInertnessGuardTest` is still in the tree and still green at 6, with only slice 01's
      amendment visible in the diff.
- [ ] `LifecycleEnumDecouplingGuardTest` and `RawStateNamesInSqlGuardTest` are green and unamended.
- [ ] Full backend suite green; parity **255 tests in 24 classes, 0 failures, 1 skipped**; deltas
      **8 tests, 0 failures**.
- [ ] `STATUS.md` records per-slice evidence and all six divergences with their owners, and map ticket
      09 is resolved with an entry in the map's Decisions-so-far.

## Notes

**Why an end-gate rather than a ratchet.** The alternative was writing this guard in slice 01 with
every rule exempted and having each slice delete its own exemption. Rejected because every slice would
then edit the exemption list, and a reviewer cannot easily tell a legitimate removal from a sloppy
one. The inertness gate and the decoupling gate are both precedents followed here.

**Prior art is the inertness guard and the decoupling guard** — a working-directory-resolved scan
root, comment blanking, named exemptions, a violation report with file and line, and an anti-vacuity
test. **Copy rather than extract**: both of those recorded why, and it applies again. Each gate has a
different lifetime and each is meant to be deleted whole.

**The caller rule is what makes "no app run is owed" defensible.** This slab breaks no screen because
nothing reaches one, and standing decision 5 has nothing to bite on — but that is a claim, and this
rule is what turns it into a check. If the rule cannot be made green, the claim was wrong and the app
run is owed after all.

**The signature rule is not optional and not paranoid.** Without it the gate reports green over an
evaluator that holds a service which holds a repository, which is the shape a well-meaning later
change produces on the first attempt.

## Blocked by

- Slices [01](01-vocabulary-move-and-inertness-gate.md),
  [02](02-thinnest-evaluation-end-to-end.md),
  [03](03-guards-enter-the-pipeline.md),
  [04](04-params-bind-at-load-time.md),
  [05](05-information-requirement-built-in-stage.md),
  [06](06-action-chain-in-the-outcome.md),
  [07](07-compiled-graph-cache.md),
  [08](08-negotiation-approved-and-post-visibility.md),
  [09](09-terminal-aggregation-and-spawn.md) — all of them.
