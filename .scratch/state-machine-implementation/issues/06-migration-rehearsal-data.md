# Migration rehearsal data

Type: grilling
Status: open

## Question

The destination requires ADR 0009's cutover to be "replayed against production-shaped data in Testcontainers". Where does the rehearsal dataset come from?

This is the user's decision because it involves data access and possibly anonymization, not just engineering.

> **Premise corrected 2026-08-13** (while charting the Freeze-current-behaviour slab). This ticket originally opened "There is no such data, and nothing in the repo resembles it — 50 `V*.sql` migrations and zero fixtures: no `data.sql`, no `import.sql`, no seed or demo SQL, no test SQL of any kind." **That was false.** Two repeatable Flyway seeds exist: `backend/src/main/resources/db/test/migration/R__Initial_data.sql` (193 lines) and `db/dev/migration/R__Initial_data.sql` (139 lines), selected by `@IntegrationTest(loadTestData = true)` via `application-test.yaml:3`. They already populate `negotiation.current_state`, `negotiation_resource_link.current_state`, `negotiation_lifecycle_record` and `negotiation_resource_lifecycle_record`.
>
> This **narrows the ticket rather than closing it**. The starting point is now "extend an existing seed", not "build a fixture from nothing" — so sharpening item 2 below is cheaper than it looked. But the load-bearing questions are untouched: the existing seed is small, hand-written **developer** data, not production-*shaped* data, and it contains none of the pathologies the rehearsal exists to catch — no Legacy State values, no orphan `for_event`, no realistic cardinalities or state distribution. Item 3 (a read-only distinct-value query against production) remains the highest-value item and is entirely unaffected.

### Why a rehearsal is load-bearing here

ADR 0009 is unusually unforgiving. It is forward-only with no Flyway `undo`; **rollback is restoring the pre-deploy snapshot**, losing anything processed after the deploy. It is stop-the-world, so there is no canary. And its riskiest steps are all data-shape-dependent:

- **Pre-flight asserts** that "raise before anything destructive so a surprise value aborts with the snapshot intact" — these are exactly the checks that a synthetic dataset containing only expected values will never exercise.
- **`APPROVED` and `RETURNED_FOR_RESUBMISSION` surviving as Legacy States.** ADR 0009's reasoning is that these are unreachable in current code but "enshrined in live and audit CHECK constraints, which means older code once wrote them and production history very likely still holds them." That is an **inference about production data, never verified.** If it is wrong the seed carries dead rows; if it is right and something else is also lurking, the deploy aborts.
- **Information Requirements re-homing by name** — `for_event` string matched against the v1 Resource definition's Event of the same name. Any legacy `for_event` value with no matching Event fails the backfill.
- **Audit backfill by same-name join** — any historical `changed_to` value not present as a v1 State (including Legacy States) fails, or worse, silently drops.

Every one of those is a question about what values production actually holds.

### Sharpen at least

1. **Is a production dump obtainable at all?** If yes: anonymized how, held where, and can it live in CI or only on a developer machine? If it cannot leave production, the rehearsal cannot be automated and the gate has to be something else.
2. **If not a dump, then what?** A generator producing production-*shaped* data (right cardinalities, right distribution of states including the legacy ones, orphan `for_event` values, negotiations in every state, drafts) is honest work but is only as good as the assumptions in it — and the assumptions are the thing under test.
3. **Can the assumptions be checked cheaply without a dump?** A read-only query against production — "distinct `current_state`", "distinct `changed_to`", "distinct `for_event`", row counts — would settle the Legacy States inference and the orphan-`for_event` risk directly, and needs no data extraction at all. **This may be the highest-value item in the ticket.** If it is available, get the numbers.
4. **What is the rehearsal's pass condition?** Migration completes, plus asserts on row counts and total pin coverage? Or a full post-migration behaviour comparison?
5. **Does the rehearsal live in CI or is it a one-off?** ADR 0009's migration is written once and never changes, so a one-off rehearsal at the end may be proportionate — but then it gates the whole map on a step that can only run late.
6. **Is a smaller fixture acceptable as the automated gate**, with a full-scale rehearsal as a separate manual step before deployment (which is itself out of scope)?

### Note

This ticket is unblocked and worth resolving early even though the migration slab is late: if the answer is "a dump takes three weeks of approvals", that needs to be in motion now, not discovered at the end. If the answer involves querying production, the resulting numbers are facts that later tickets depend on — record them in the resolution.

Use `/grilling`.
