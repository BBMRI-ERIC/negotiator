# Flowable Prototype — Findings

Companion to `docs/prd-flowable-migration.md`. Written after building the prototype on
`proto/state-machine-flowable`, against the evaluation criteria the PRD's "Further Notes"
section asked to have answered with evidence, not assumption.

## Result

All 20 user stories' acceptance bar was met: the old Spring State Machine implementation was
fully replaced by Flowable, side by side with the original test suite until the last commit,
then deleted outright. Final state: **1066 backend tests, 0 failures, 0 errors** (15 pre-existing
skips, unrelated to this migration), Spring State Machine's three Maven dependencies gone.

## 1. Does idiomatic persistent-instance usage work cleanly, or does entity/runtime sync become friction?

**It becomes real, concrete friction — not theoretical.** `ResourceControllerTests.
getAllResources_approvedNegotiation_resourceContainsAllLinks` mutates `NegotiationResourceLink.
currentState` directly via the repository (bypassing `sendEvent`), then immediately asks for
possible events. The lazily-started Flowable process instance for that resource was already
parked at a *different* activity from an earlier (committed, non-transactional) test in the same
class — because `@IntegrationTest` doesn't reset the schema between methods, only after the whole
class. The persistent instance and the entity column silently disagreed, and the wrong link set
came back.

The fix (`findOrStartProcessInstance` in both `NegotiationLifecycleServiceImpl` and
`ResourceLifecycleServiceImpl`) now compares the process instance's actual current activity
against the entity's `currentState` column on every read, and discards + restarts the instance at
the now-current state on mismatch. This works, but it means the "one persistent process instance
per entity" model needs an explicit reconciliation step wherever the entity can change out from
under it — which, in this codebase, is not a hypothetical: `ResourceNotificationService` already
mutates `NegotiationResourceLink.currentState` directly today, as a pre-existing, unrelated
mechanism this prototype had to preserve for parity (see finding 4 below). A stateless
re-derive-from-column design (what SSM already did, and what stateless4j does) does not have this
failure mode at all, by construction.

## 2. What is the actual schema footprint?

Measured directly against the running Testcontainers Postgres instance mid-test-run:

| | table count |
|---|---|
| Application's own tables (Flyway-managed) | 35 |
| Flowable's tables (`ACT_*` / `FLW_*`, all sub-engines) | **70** |
| **Total** | **105** |

Flowable roughly **triples** the schema's table count. The 70 is not just the process engine:
`flowable-spring-boot-starter` (the artifact the PRD specifies) bundles CMMN, DMN, IDM, the App
engine, and the Event Registry, each with its own schema, none of which this migration uses for
anything. A narrower `flowable-spring-boot-starter-process`-only dependency would cut this
significantly, but wasn't what was evaluated (PRD-specified, so this prototype could report the
starter's actual, not estimated, footprint).

## 3. What is the actual per-test-suite runtime cost?

Same 28-test acceptance-bar set (13 negotiation-only + cross-machine tests, 10 resource-only +
info-requirement tests), each class using `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`
(a fresh Spring context, and therefore a fresh Flowable engine bootstrap + BPMN redeploy, *per
test method* — not once per class):

| | tests | wall time | avg/test |
|---|---|---|---|
| Old SSM (`NegotiationLifecycleServiceImplTest`, pre-migration baseline) | 29 | 134.0s | 4.6s |
| New Flowable (negotiation + resource test files combined) | 28 | 161.7s | 5.8s |

**~25% slower per test**, driven by Flowable's own schema liquibase changelog (18+ create
scripts just for the process engine, more for the bundled sub-engines) re-running on every
context reload, on top of the application's own Flyway migration. This is a real, structural cost
of the persistent-instance-per-entity model in a test harness that reloads context per method —
not a one-time amortized cost. `stateless4j`'s pure-POJO unit tests pay none of this; the PRD
explicitly anticipated this comparison (see prototype's Testing Decisions section) and it holds up
directionally on measurement, though the specific number depends heavily on this repo's existing
`@DirtiesContext` granularity choice, not just Flowable itself.

## 4. Does the stringly-typed process-variable gap matter in practice, or is it cosmetic?

Cosmetic in this prototype, in practice. Every process variable read (`execution.getVariable(
"negotiationId", String.class)`) is a fixed, small, known set (`negotiationId`, `resourceId`,
`initialState`, `pendingEvent`) written by code in the same module a few lines away. No case arose
where a wrong variable name or type caused a runtime surprise that a typed context would have
caught at compile time — but the sample size here is one migration's worth of variables, not a
sustained multi-developer codebase. The gap is real (verified: nothing in Flowable's API stops a
typo from compiling), it just didn't bite during this build.

## 5. Does the visual/diagram value show up concretely, or stay theoretical?

**Partially theoretical, and one guard shape proved actively incompatible with it.** The 1:1
BPMN-to-state-graph mapping worked exactly as designed for the pure "wait state → gateway →
message-correlated events" shape (negotiation: 8 transitions; resource: 13 transitions — see
`negotiation.bpmn20.xml` / `resource.bpmn20.xml`). Actions render as genuinely visible service
tasks, matching the PRD's stated reason for not hiding them as listener attributes.

But the information-requirement guard — explicitly called out in the PRD as the one guard that's
actually load-bearing today — could **not** be modeled as the PRD's proposed Service Task +
Exclusive Gateway + `BpmnError` pattern, despite that pattern being spiked and confirmed
mechanically sound (a `BpmnError` from an uncaught error end event does propagate out of
`messageEventReceived(...)` exactly as described). The blocker wasn't Flowable — it was that the
actual guarded behavior (`sendEventForResource_notFulfilledRequirement_
throwsTransitionPreconditionException`) fires even when the requested event has *no modeled
transition at all* from the resource's current state. A guard reachable only via message
correlation from a specific source-state's gateway structurally cannot express a check that fires
regardless of reachability. It stayed a plain Java precondition, exactly where it was before.

So: nobody but the two developers who built this looked at the BPMN XML during the build (no
Modeler UI was in scope), and the one guard genuinely worth visualizing turned out not to fit the
visual model at all. The diagram value is real for the pure happy-path graph, not yet demonstrated
for anything with a cross-cutting precondition.

## Other confirmed/refuted predictions

- **`@Lazy` cycle elimination (user story #7): confirmed.** None of `EnablePublicPostsAction`,
  `EnablePrivatePostsAction`, or `DisablePostsAction` needed `@Lazy NegotiationService` — Flowable
  resolves `delegateExpression` beans at execution time, not at Spring context startup, so there
  was never a cycle to break in the first place (unlike SSM's `@EnableStateMachine`-scoped config
  beans, which were part of the eager singleton graph).
- **`NegotiationIsApprovedGuard` (dead code): not reintroduced,** as directed. No BPMN-level guard
  duplicates the `IN_PROGRESS`-gate check; it stays the same application-layer `if` it always was,
  now in `ResourceLifecycleServiceImpl.getPossibleEvents`.
- **Resource process instances don't start at entity-creation time,** unlike negotiations — a
  deliberate divergence forced by discovering that `NegotiationResourceLink.currentState` starts
  genuinely `null` in this codebase (`SUBMITTED` is dead/vestigial, never actually assigned) and
  only gets set later, asynchronously, by the untouched notification module. Resource processes
  start lazily, on first access, landing directly at whatever state the entity already holds.

## Go/no-go relevant summary

Flowable covers the current lifecycle needs functionally (every acceptance-bar test passes), and
two of its three headline strengths hold up as claimed (synchronous debugging with no reactive
`.subscribe()` boundary; the `@Lazy` cycle genuinely disappears). Against that: the schema and
runtime cost are concrete and non-trivial for a footprint this small, the typed-safety gap is real
even if it didn't bite here, and the one interesting guard-modeling question this prototype set
out to answer came back "doesn't fit the shape BPMN offers," not "fits, with these caveats." The
visual/diagram payoff — the core reason to reach for a BPMN engine at all — is the part still worth
treating as unproven rather than delivered.
