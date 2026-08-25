# State machine implementation

## Destination

The lifecycle subsystem of ADRs [0001–0009](../../backend/docs/adr/) built and merged on a feature branch, **not deployed**: Spring Statemachine deleted outright, Lifecycle Definitions living as relational configuration, the Flyway cutover written and replayed against production-shaped data in Testcontainers, the suite green, the frontend working — and **admin authoring** of Definition Families and their associations delivered, in a form yet to be decided (API-only, or API plus UI). Deployment and the production migration run are excluded; the migration itself is still written and tested.

The effort runs in three **stages** — milestones inside this map, not separate maps:

| Stage | Content | Charting state |
|---|---|---|
| **1 — Replacement** | Data-driven evaluator, one seeded Definition Family per Definition Scope, Definition Version Pin, Information Requirements as a Built-in Stage, coupling (Spawn / Feedback / conclusion), the migration, Spring Statemachine deleted, frontend repaired where the backend breaks it. **Behaviour identical to today.** | partly ticketed |
| **2 — Configurability** | Association schema (Resource↔family, Network↔family), Definition Resolution precedence, the multi-Network conflict check. | fog |
| **3 — Admin authoring** | An admin creates a Definition Family, publishes a Definition Version, points a Network or Resource at it. Shape unknown — API-only vs API + UI is itself undecided. | fog |

Stage 1 finishing is a **milestone, not the destination**. The map is done when an admin can configure a Resource Lifecycle and the branch is green.

## Notes

### This effort carries execution — it does not stop at planning

**Wayfinder's "Plan, don't do" default is explicitly overridden here.** Ticket resolutions deliver **merged code**, not decisions to hand off. Reaching a build ticket is *not* the signal to hand off — build it. This is the hatch the skill sanctions: "an effort can override this in its Notes — carrying execution into the map itself."

Two consequences, stated so no session relitigates them:

- **The `task` type is stretched deliberately.** Wayfinder documents `task` as unblocking *a decision*; here it also delivers destination work. Intended.
- **A build ticket spans sessions.** Wayfinder caps a ticket at one ~100K session and forbids resolving more than one per session. A build **slab** exceeds that, so the rules are honoured by nesting rather than bent:

| Level | Artifact | Granularity |
|---|---|---|
| Map ticket | `.scratch/state-machine-implementation/issues/NN-*.md` | One slab — a multi-session container |
| Inner issues | `.scratch/<slab-slug>/PRD.md` + `issues/NN-*.md` | One commit, consumed by `/implement` |

A claimed slab runs its own `/to-prd` → `/to-issues` → per-issue `/implement` loop in ordinary tracker files, and resolves when its gate is green. A slab left `claimed` across sessions is normal.

### Stage 1's gate is one command — see [parity-gate.md](parity-gate.md)

Standing decision 1 in operational form, written by the Freeze-current-behaviour slab. **[parity-gate.md](parity-gate.md)** carries the two commands (parity, and the ADR 0005 intended deltas reported separately), the counts each must produce, the `nix develop` prefix requirement, the real path of `test-backend.sh`, the test-ordering rule and the suite's honest coverage gaps. **Every later slab's gate is "the parity half of that command is green".**

The empirical findings that pinning today's behaviour produced — including several that contradict documents the later slabs are written against — are in **[before-picture-findings.md](before-picture-findings.md)**. Read its Part 3 (corrections) and Part 7 (decisions the redesign now owes) before implementing ADR 0005, 0007 or 0009.

Neither file is inside `.scratch/freeze-current-behaviour/`: that slab's `STATUS.md` is meant to be deleted when the slab closes, and both of these outlive it.

### Settled while charting — standing decisions for the whole effort

These were agreed with the user during charting. They are not ticket resolutions, and they are not up for relitigation.

1. **Stage 1's gate is characterized parity.** Tests written against the *current* Spring Statemachine behaviour must pass **unchanged** against the new subsystem. With 4 of 138 test files touching lifecycle, this is the only real evidence that ~60 files of consumer churn broke nothing. **Carve-out:** ADR 0005 deliberately changes `getPossibleEvents` (blocked Events omitted) and the requirement hint links (structural reachability, array-valued rels). A pure-parity suite would pin the very bugs the ADRs fix, so those are excluded from parity and asserted as **intended deltas**.
2. **Decouple consumers first, then swap the engine.** Make the change easy, then make the easy change. While Spring Statemachine is still running and green, migrate the consumer subsystems off the four enums one at a time — each a small verified commit. Only then swap the two lifecycle services onto the Transition Evaluator and delete Spring Statemachine, which by then touches very little. This does **not** violate ADR 0001: decoupling consumers from an enum is not running two engines. ADR 0001's constraint is about *rollout*, not build sequence.
3. **A slab is one gate.** Size follows from having a single meaningful verification gate ("additive only, nothing reads it", "pure unit tests, no I/O", "characterization suite passes unchanged"), not from a file count. Commit-level granularity lives in the slab's inner issues.
4. **One long-lived branch off `plan/state-machine-redesign`, for the whole destination.** Not merged to master per stage. **Hazard this avoids:** ADR 0009's migration is a breaking stop-the-world cutover, so if stage 1 reached master the next production deploy would run it unintentionally. Merge master *into* the branch regularly instead — ~60 files across 7 subsystems will collide with concurrent work.
5. **Frontend fixes ride along, verified manually.** Each backend slab that breaks a screen repairs it in the same slab, so the app is never knowingly broken; verification is running the app and looking. No new frontend test infrastructure. **Accepted cost:** the frontend has no unit test runner at all and its Cypress specs contain zero references to requirements, submissions, lifecycle or state — so stage 3's authoring UI inherits no safety net.
6. **The forced IR admin change stays minimal parity.** `InformationRequirementCreateDTO.forResourceEvent` (typed as the deleted `NegotiationResourceEvent`) becomes an Event reference. Audience and Quantifier are persisted as ADR 0006 requires but **defaulted server-side** to `RESOURCE_REPRESENTATIVES` / `ANY` — exactly what ADR 0009 backfills — and are **not** exposed for authoring. Authoring them belongs to stage 3.
7. **Migration packaging is already fixed by ADR 0009** — additive DDL alone first, then one atomic data-cutover file. In-branch that means the schema DDL lands early with the entities and the cutover lands last. Not re-decided.

### Binding constraints

- **The nine ADRs are settled.** They are the spec. Reopening one is out of scope unless implementation surfaces a genuine contradiction — and then it is its own decision ticket, never a quiet edit.
- **`backend/CONTEXT.md` vocabulary is binding**, `_Avoid_` lines included: Transition Evaluator not engine, Lifecycle not workflow, Audience not assignee. New implementation-phase terms belong there, added via `/domain-modeling`.
- **`.scratch/state-machine-redesign/` is immutable.** All nine ADRs link into its `issues/` directory by relative path. Do not move, rewrite or tidy it.
- **The branch must descend from `plan/state-machine-redesign`.** That directory is tracked (15 files) and exists only on that branch — it is absent from `master`, so branching from master breaks every ADR's provenance link.
- **Do not touch or reference the proto worktrees** (`.claude/worktrees/proto-stateless4j`, `proto-flowable`, `proto+state-machine-stateless4j*`). ADR 0001 already rejected those approaches; they are dead ends, not prior art.
- **PostgreSQL only** — ADR 0002 relies on partial indexes.
- **Production ships once; development must not.** The big-bang rollout constrains rollout, not build sequence. Prefer many small verified commits; with snapshot-restore as the only rollback, per-commit verification *is* the safety net.

### Verified facts — rely on these, don't re-derive

- Spring Statemachine fully in place: `spring-statemachine-core` / `-recipes-common` / `-starter` at `backend/pom.xml:204-216`; **27 classes** under `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/`.
- **The four deleted enums reach far beyond that package.** `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent`, `NegotiationResourceEvent` are referenced across **~60 main-source files in seven subsystems** — notifications (7 handlers), webhooks (5 classes), network statistics, resource governance, JPA filter specs, metadata DTOs, timeline — and **26 of 138 test files**. This dominates the cutover and **no ADR addresses it**.
- **Lifecycle test coverage is thin**: 138 test files, 4 touch lifecycle directly.
- **Seed data exists — two repeatable Flyway seeds.** `backend/src/main/resources/db/test/migration/R__Initial_data.sql` (193 lines) and `db/dev/migration/R__Initial_data.sql` (139 lines), alongside the 50 `V*.sql` in `db/migration/`. `application-test.yaml:3` sets `locations: classpath:db/migration/, classpath:db/test/migration`; `@IntegrationTest(loadTestData = true)` selects that, while `loadTestData = false` narrows Flyway to `db/migration/` only (`EnablePostgresTestContainerContextCustomizerFactory:88-90`). The test seed already populates `negotiation.current_state`, `negotiation_resource_link.current_state`, `negotiation_lifecycle_record` and `negotiation_resource_lifecycle_record`. Testcontainers 1.21.1 + Flyway 11.10.0 present; container setup at `backend/src/test/java/eu/bbmri_eric/negotiator/config/PostgresContainerManager.java`. **Corrected 2026-08-13** while charting the Freeze-current-behaviour slab; the original entry claimed no fixtures existed anywhere, which was false and had propagated into ticket 06's premise.
- **Frontend has no unit test runner** — Cypress e2e only, four Negotiation specs (create, message, PDF, negotiation), none referencing requirements, submissions, lifecycle or state.
- **The two forced frontend breakages, precisely.** (a) `frontend/src/components/ResourceItem.vue:96,102` filters `_links` keys with `startsWith('submission-')` and `startsWith('requirement-')` — exactly the per-row rels ADR 0005 collapses into array-valued ones. (b) `frontend/src/components/modals/InfoRequirementModal.vue` (with `frontend/src/store/admin.js`, `InformationRequirementsSection.vue`, `InformationRequirementCard.vue`) drives an IR admin API whose `InformationRequirementCreateDTO.forResourceEvent` is typed as the **deleted `NegotiationResourceEvent` enum**, so its event dropdown must fetch Events instead. The predecessor map was backend-only, so no ADR owns either.
- `frontend/CONTEXT.md` is deliberately absent — `CONTEXT-MAP.md:14` says directories are created lazily. Absence means nothing written, not something missing.

### Skills

- `/grilling` + `/domain-modeling` for every `grilling` ticket. One question at a time; never answer on the user's behalf.
- `/codebase-design` when a ticket is really about a seam — ADR 0001's stateless no-I/O evaluator boundary is a deep-module argument already.
- `/prototype` for stage 3's shape.
- `/to-prd` → `/to-issues` → `/implement` (and `/tdd`) **inside a claimed slab only**.
- `/focused-backend-tests` for backend test runs.

## Decisions so far

<!-- one line per closed ticket: enough to judge relevance, then open the link for the detail -->

- **[01 Freeze current behaviour](issues/01-freeze-current-behaviour.md)** — **resolved.** Stage 1's
  parity gate exists and is green: **255 tests in 24 classes** under
  `eu.bbmri_eric.negotiator.characterization.**`, plus **8** ADR 0005 intended deltas tagged out of
  it, with **no production code changed anywhere**. States and Events are named only as strings
  behind one test-scope adapter and a mechanical guard enforces it, so the suite must pass
  *unchanged* after the enums are deleted. Both graphs are dumped by walking the live beans and the
  committed artifacts are regenerated and byte-compared on every run. Commands, counts and coverage
  gaps: **[parity-gate.md](parity-gate.md)**. **Pinning the behaviour left twelve decisions the ADRs
  do not yet answer, and one place where three settled documents describe behaviour the code does not
  have — read
  [before-picture-findings.md](before-picture-findings.md) parts 3 and 7 before implementing 0005,
  0007 or 0009.** Headlines: `NegotiationIsApprovedGuard` is attached to nothing and must not be
  registered; spawn writes `REPRESENTATIVE_CONTACTED`/`REPRESENTATIVE_UNREACHABLE`, never the initial
  State, publishes no Resource state change, and keys on arriving at `IN_PROGRESS` rather than on
  `APPROVE` — so ADR 0007's Spawn Action, ADR 0009's seed and `backend/CONTEXT.md`'s **Spawn** entry
  are all specified against a picture that is not the code; conclusion counts only 2 of 12 Resource
  States; the Information Requirement check is unscoped and outranks every other gate; and
  `ResourceStateChangeEvent` has a second, non-Transition producer.
- **[02 Where state-triggered behaviour lives](issues/02-state-triggered-behaviour-location.md)** —
  **resolved.** Notification and webhooks **stay listeners**, keyed on five **Well-known State**
  names (`DRAFT`, `SUBMITTED`, `IN_PROGRESS`, `DECLINED`, `ABANDONED`) in one provisional
  `WellKnownNegotiationStates` holder; a missing name is a **silent no-op**, matching the `default`
  branch that already notifies nobody for 4 of 8 Negotiation States. Nothing becomes an Action except
  the spawn half of `NegotiationInProgressHandler`, which ADR 0007 already claimed — and it now
  publishes a `ResourceLifecyclesSpawnedEvent` so notification splits off it *without* spawn emitting
  `ResourceStateChangeEvent`, which ticket 01 pinned as silent today. The `NotificationStrategy`
  mechanism survives whole. **Two consequences for the rest of stage 1:** notifications and webhooks
  need no Action registry, schema or evaluator, so they **decouple early, inside slab 07** rather than
  after it; and the two query-driven sites (`PendingNegotiationReminderHandler`,
  `NonRepresentedResourcesHandlerImpl`) move *into* ticket 03's population rather than out of it — so
  this ticket narrowed 03 the opposite way to how it predicted. New term **Well-known State** in
  `backend/CONTEXT.md`.

- **[03 State and Event identity for downstream consumers](issues/03-state-event-identity-downstream.md)** —
  **resolved.** A State or Event is named by a **bare `String`** everywhere outside the Lifecycle — no
  value object (there is nothing to validate against: a name is valid only relative to a Definition
  Version, which the type does not hold), no demoted enum. Only a name some behaviour depends on
  *existing* keeps a Java constant: **nine names in three narrow holders** —
  `WellKnownNegotiationStates` (ticket 02's five, **independently confirmed exact** by a full sweep),
  new `WellKnownResourceStates` (`SUBMITTED`, `REPRESENTATIVE_CONTACTED`,
  `REPRESENTATIVE_UNREACHABLE`) and new `WellKnownResourceEvents` (`OVERRIDE`). Everything else
  carries the name as data. `?status=` **keeps its 400**, now checked against the Negotiation-scope
  active version's State rows; the `DRAFT` visibility rule **stays a name**; OpenAPI goes uniformly to
  `type: string` (the wire format is identical, so no subscriber breaks); names not ids. **No new
  vocabulary** — a parallel *Well-known Event* term was drafted and rejected, because its whole
  population is `OVERRIDE`, which is already the **Override Event** and is structural to ADR 0002's
  model rather than a fragile bet on a family's names. **Three findings that change what slab 07 is:** the
  population is **46 files, not ~50, and it is three jobs** — 35 constant references in 18 files, ~28
  type-only mechanical swaps, and **8 raw SQL literals the compiler cannot see**, so "it builds" is
  *not* evidence that slab is done; **`NegotiationStatusConverter` is dead code** (never registered in
  `WebConfig`) and is a deletion, not a migration; and **`DRAFT` is not the initial State** —
  `initialState` is `SUBMITTED` and nothing targets `DRAFT`, so "hide the initial State" would have
  revealed drafts. One new ticket ([10](issues/10-network-kpis-name-resource-states.md)); the
  `ordinal` ordering contract is routed to ticket 04.

- **[08 Definition schema and entities](issues/08-definition-schema-and-entities.md)** — **resolved.**
  ADR 0002/0003's schema exists and is **inert**: five migrations `V36.0`–`V36.4` (one per slice,
  never appending to an applied file — that changes its checksum and fails Flyway validation on a
  developer database), six tables, seventeen package-private files in
  `eu.bbmri_eric.negotiator.lifecycle.definition`, **86 tests in nine classes** against a real
  Postgres, and **exactly two production files touched outside the package** — `Negotiation` and
  `NegotiationResourceLink`, for the nullable pin. Parity **255/24/1 skipped, unchanged at every
  slice**; deltas 8/0/0/0; full suite 1415/0/0/16. `V36.5` is free. **The inertness claim is a test**
  — `DefinitionInertnessGuardTest`, three rules over all 431 production sources, and the rule that
  matters is the **table** one: a native query names the table, never the entity, so a
  `@Query(nativeQuery = true, ... "select * from lifecycle_definition")` passed the package and type
  rules green. **A guard built only from Java identifiers does not prove a database claim.** The slab
  that starts reading these tables deletes that test as a visible line in its diff. **One schema fact
  that moved:** a Transition can no longer reference a State or Event of a different Definition
  Version — composite FKs against a `UNIQUE (lifecycle_definition_id, id)` on every vertex table
  express it, so it left the left-for-stage-3 list. Three "exactly one" rules remain enforceable only
  as *at most* one (one active version per family, one initial State per version, `scope` fixed per
  family), because a partial unique index cannot require at least one and zero active rows is a valid
  intermediate state mid-publish — that half is publish-time validation in stage 3.
  **`required_authority` is built single-valued as ADR 0002 specifies**, with ticket
  [11](issues/11-transition-authority-admin-or-creator.md) still open over it. **Three decisions were
  deferred with explicit triggers and are routed below**, and the slab's `STATUS.md` is **kept, not
  deleted** — its per-slice sections are the working reference for anyone extending this schema
  (proven partial-index syntax, how to attribute a refused write to the right index, why `SELECT
  col::text` does not prove jsonb, the composite-FK technique, and the trap that two concurrent Maven
  invocations against `backend/` look like 150 real failures).

## Not yet specified

- **Stage 1 after the evaluator: the cutover and everything downstream.** Swapping the two lifecycle services onto the Transition Evaluator and deleting Spring Statemachine; the audit `state_id` FK conversion (ADR 0008); Information Requirements as a Built-in Stage (0005) with Audience and Quantifier (0006); the coupling layer — Spawn, Feedback, conclusion (0007); the atomic data-cutover migration (0009). The *content* is fixed by the ADRs; the **slab boundaries and gates are not sliceable yet**, because tickets [03](issues/03-state-event-identity-downstream.md), [04](issues/04-global-state-event-metadata-contract.md) and [06](issues/06-migration-rehearsal-data.md) move work between them. Graduates as those resolve. Deliberately not pre-sliced. **Two constraints ticket 02 hands the coupling slab specifically:** `SPAWN_RESOURCE_LIFECYCLES` must **not** begin publishing `ResourceStateChangeEvent` — ticket 01 pinned spawn as announcing nothing, so emitting per-Resource events would add N notifications and N webhook deliveries per approval — and it must publish a `ResourceLifecyclesSpawnedEvent` carrying the contacted representatives, because notification now rides on that rather than on arriving at `IN_PROGRESS`. **And one obligation ticket 03 hands slab 07:** eight State names live as raw string literals in `NetworkStatsRepositoryImpl`'s native SQL and JPQL, so deleting the enums raises **no compile error** for them — the slab needs a deliberate grep, not a green build, and line 216 hits the audit table ADR 0008 converts to an FK, so it breaks again there. **And three obligations slab [08](issues/08-definition-schema-and-entities.md) hands these slabs, each filed with its trigger:** the **coupling** slab must decide how a Resource's pin is written at all — the column is `updatable = false` and the link row already exists at Spawn, so the mapping cannot write it ([the three options](../definition-schema-and-entities/issues/09-pinning-an-existing-resource-link.md); no migration needed for any of them) — and what an unresolvable definition does to a Negotiation approval, since `DefinitionResolver` throws a package-private, unmapped exception and a rolled-back 500 is today's default ([the resolver's three shape decisions](../definition-schema-and-entities/issues/10-definition-resolver-shape-is-a-guess.md)); the **cutover** slab must build the index on both pin columns in the same migration that sets them NOT NULL, which is deliberately absent today because the column is 100% NULL and both tables are among the most-written ([why, and the other triggers](../definition-schema-and-entities/issues/08-pin-column-fk-indexes-deferred.md)). **The first slab to read the definition tables also deletes `DefinitionInertnessGuardTest`**, and that deletion belongs in its diff rather than in a quiet edit to the lists inside it.
- **Stage 2 — configurability.** ADR 0004 fixes the design (direct association wins, Network second, Global Default Family last; conflict rejected at write time in `NetworkService`). Open: whether the `DefinitionResolver` seam stage 1 leaves behind is the right shape — now with the three concrete calls slab 08 took, and what would make each wrong, in [The DefinitionResolver's shape is a guess](../definition-schema-and-entities/issues/10-definition-resolver-shape-is-a-guess.md); the precedence walk adds a Resource parameter to `resolveForResource()`, so it changes a signature and not only a body — and whether the conflict check can be meaningfully built and tested before stage 3 supplies the write paths it guards. Sharpens once stage 1 lands. **Carries one sharp ticket already:** [10 Network KPIs name Resource States a custom family need not have](issues/10-network-kpis-name-resource-states.md), raised by ticket 03 — four network statistics define their metric by naming Resource States, and a family omitting them makes the KPI silently report zero.
- **Stage 3 — admin authoring.** Whether it is API-only or API plus a Vue authoring UI is undecided and is the first thing to settle there. Expects `/prototype` tickets before build tickets: graph authoring is an open design problem, not plumbing. Inherits no frontend test net (see standing decision 5). **Carries one deferred check from ticket 02:** whether publishing a Definition Version that omits a **Well-known State** name is refused, warned about, or ignored. Deliberately fog rather than a ticket — the question is *when* the check runs, and the only moment that can refuse is version activation, whose shape is this stage's first undecided thing. Stage 1 is safe by construction: ADR 0009's seed is a faithful transcription, so all five names are present.
- **Whether the frozen v1 seed SQL is generated from ticket 01's graph dump, or transcribed independently.** ADR 0009 requires "a faithful transcription of the current two configuration classes" as raw frozen SQL, and its correctness claim *is* that faithfulness — which argues for generating it by walking the live beans rather than by eye. Ticket 01 produces the dump; whether the seed derives from it is decided in the migration slab.
- **`ResourceWithStatusAssembler` N+1 bulk-fetch batching** — flagged as follow-on by predecessor ticket [05](../state-machine-redesign/issues/05-info-requirements-model.md), not designed there. Sharpens once ADR 0005's rel changes land in that assembler.
- **`InformationRequirement.isViewableOnlyByAdmin`** — a live field on the entity and both DTOs that appears in **no ADR**. Needs an owner when the IR model moves; too small to ticket alone, too real to drop.
- **Public documentation goes stale, and nothing owns it.** `docs/lifecycle.md` describes the subsystem being replaced; `docs/negotiation_state_machine.png` is a diagram of the graph being moved into data; `docs/administrator.md:41-59` documents the Information Requirements admin flow including its "Lifecycle Event" picker; `docs/webhooks.md` documents payloads whose state fields change type; `docs/notifications.md` and `docs/database_migration.md` are both in the blast radius. In scope — a finished implementation does not ship with docs describing the deleted design — but not sliceable until the slabs that change each behaviour are known. Graduates alongside them; the diagram in particular may need regenerating from ticket 01's graph dump.

## Out of scope

- **Deployment and the production migration run.** The destination stops at a green feature branch. The migration is written and replayed in Testcontainers; it is never run against production here.
- **Importable / exportable definition file.** ADR 0009 calls it "a separate downstream feature" — the seed "may one day be generated from one, but it is committed as frozen SQL". Ruled out 2026-08-13; returns as its own effort.
- **Clock-tick Orchestration Trigger, Information Requirement reminders, expiry.** ADRs 0006 and 0007 declare the primitive is ready for a clock tick but deliberately leave the scheduler plumbing undesigned, to be built "when the features that need it are". Includes the `notified_at` column that would make a `NOTIFIED` state derivable. Ruled out 2026-08-13.
- **LS-AAI / `IAM_GROUP` membership sync.** How the resolver actually fetches, mirrors, caches and refreshes LS-AAI virtual groups. The predecessor map ruled this out on 2026-08-10; only the interface `resolve(params, context) → Set<PersonRef>` is settled. Carried forward. The `IAM_GROUP` resolver still ships — it simply has no members to resolve.
- **Outcome-sensitive conclusion.** Routing all-delivered vs all-unavailable to distinct terminal outcomes. Ruled out by the predecessor map, which showed ADR 0007's System Event + `TERMINAL_AGGREGATION` Guard mechanism *accommodates it as later configuration* — excluded, not foreclosed. Carried forward.
