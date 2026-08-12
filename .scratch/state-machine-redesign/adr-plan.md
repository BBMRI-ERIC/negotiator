# ADR plan — state-machine redesign

The contract for turning the resolved map into ADRs. **Nine ADRs from eleven tickets**, split by architectural axis rather than by ticket, because tickets 07, 08, 09 and 11 amend 01 — a reader who lands on four ADRs that partially retract each other is worse off than one that states the settled position.

All nine are backend-internal, so they land in **`backend/docs/adr/`** (not root `docs/adr/`, which `docs/agents/domain.md` reserves for decisions spanning both contexts).

## Conventions

- **Status frontmatter: `accepted`**, plus one line noting implementation is follow-on work. Without it, a reader greps for the engine, finds Spring Statemachine, and concludes the ADR is stale.
- **Link source tickets by path** (`.scratch/state-machine-redesign/issues/NN-*.md`). `.scratch/` is git-tracked here, so the links are durable — keep the ADR self-contained on the *what* and *why*, and let the link carry the full reasoning.
- **Record the rejected alternatives.** "No OSS library fits" and "Network-priority rejected as silent ambiguity" are exactly what gets reopened in six months.
- **Numbers are fixed by this file.** Assign nothing outside it.
- **Write in numeric order** — it follows the dependency order, so each ADR can cite the ones before it.

## The nine

### 0001 — Replace Spring Statemachine with a hand-rolled data-driven engine
*Sources: 02, 03*

Spring Statemachine is EOL and no off-the-shelf FSM or workflow engine fits a schema-driven bespoke registry (the active niche libraries don't bend to it; Flowable carries full BPMN weight, Camunda 8 core needs a paid licence) — and a library would only cover the cheap part. One stateless evaluator core, shared by both lifecycle scopes, definition graph cached per version; Spring Statemachine deleted entirely (dependency plus ~25 classes). Estimated 2,500–4,000 LOC against today's 1,493.

**Must not claim:** the schema itself (0002), pipeline ordering (0005), or what the cache is keyed on (0003 owns identity).

### 0002 — Lifecycle definitions are relational configuration, not Java
*Sources: 01, 07 (wiring scope), 09 (SYSTEM, state flags)*

One unified `StateMachineDefinition`/`State`/`Event`/`Transition` relational schema — not a blob — serving both lifecycle scopes. Guards and actions resolve through a `WebhookEventMapper`-style strategy-bean registry: string type keys, per-strategy typed `params` (jsonb). Two wiring tables — guard wiring with a nullable `transition_id` carrying definition and transition scope (partial unique indexes per scope), action wiring transition-only. `required_authority` on transitions (`NONE`/`IS_ADMIN`/`IS_CREATOR`/`IS_REPRESENTATIVE`/`SYSTEM`). `State` carries `initial` and `terminal` flags. Cross-scope evaluation order is engine pipeline logic, not an `order` column.

**Must not claim:** that information-requirement satisfaction is a registry guard — it is explicitly not (0005). Not versioning or identity (0003).

### 0003 — Definition versioning: immutable families, pinned at start, row id is identity
*Sources: 04 (versioning half), 08*

Definitions form versioned families keyed by an immutable `family_key`, distinct from the editable `name` label, with exactly one active version per family. The version row's id is its sole machine identity — every FK, including the pin and the engine cache key. `version` survives only as a per-family, system-assigned display sequence (unique on `(family_key, version)`, assigned at creation, gaps harmless). `Negotiation` and `NegotiationResourceLink` pin `definition_version_id` at start, so publishing a new version never moves work already in flight.

**Must not claim:** how a definition is resolved in the first place (0004).

### 0004 — Definition resolution: direct association wins, Network second, global default last
*Sources: 04*

Resolution order: a direct Resource association always wins, then Network association, then the admin-only `is_global_default` family as fallback (default-ness is resolution-time only — such families version like any other). A multi-Network conflict — a shared Resource whose Networks name different families — is rejected at write time by a single set-based `NetworkService` check, exempting Resources with a direct override. Exhaustive rather than sampled, affordable because conflicts are rare. Network-priority tie-breaking was reconsidered and rejected as silent ambiguity.

**Must not claim:** pinning mechanics (0003).

### 0005 — Information requirements gate transitions as a built-in pipeline stage
*Sources: 05, 07*

The engine always evaluates the firing Event's Requirements as a structural stage of the guard pipeline — no wiring rows, so it is impossible to misconfigure away. Deliberately *not* a registry `GuardType`. Definition-level guards become a second first-class wiring scope (`NEGOTIATION_APPROVED` is one such entry). Order: `required_authority` → requirements → definition-level guards → transition guards, short-circuiting, sequenced by expected failure likelihood with monotonic failure categories (403 → 422 → 409). One shared `evaluate()` path serves both the real gate and the possible-events dry run, so `getPossibleEvents` keeps today's shape and simply omits blocked events — with the inclusion-condition fix (structural reachability, not "lifecycle link exists"), the blocking event's label displayed, and HAL rels flattened to array-valued `requirement`/`submission`.

**Must not claim:** audience or quantifier semantics (0006).

### 0006 — Information-requirement audience and satisfaction are derived, never stored
*Sources: 10, 05 (assignee → audience amendment)*

No per-IR-occurrence instance row. Audience is a resolver registry mirroring 0002's strategy beans (`audience_type` + `audience_params`) behind one primitive, `resolve(params, context) → Set<PersonRef>`, with three strategies and write-time-validated scope compatibility. The quantifier is *distinct qualifying members ≥ threshold*, counted live, qualifying iff `submittedBy ∈ audience`. Audience is re-resolved live with **no snapshot** — drift is harmless because the guard gates only the firing instant, and the committed state is a persisted fact never re-derived. Contact is a built-in post-commit `NOTIFY_IR_AUDIENCE` stage keyed on the target state's one-hop requirement-bearing events, not admin-wired. The governing invariant: a state is derivable iff its entering action leaves a domain fact — which is why `submit` is safe and `contact` is only safe as a side effect. Note IAM group membership sync as out of scope: the resolver interface is settled, the plumbing is a separate effort.

**Must not claim:** the gating pipeline's position or order (0005).

### 0007 — Lifecycle coupling is orchestration over the stateless engine
*Sources: 09*

Coupling is an orchestration layer on the existing schema — no coupling table, no persisted parent→child instance rows. One self-firing primitive: an orchestration trigger re-attempts a definition's automatic events from the current state, and the guard pipeline decides. "Automatic" is a distinct `SYSTEM` authority rather than a `NONE` overload, so it fails authority for every human caller and stays orchestrator-only. FEEDBACK is an implicit, zero-config post-commit re-attempt of the parent's `SYSTEM` events. CONCLUDE is a `SYSTEM` event plus a parameterized `TERMINAL_AGGREGATION` guard (`ALL`/`ANY`/`N_OF_M`, configured `ALL`) that asks each resource's own pinned definition whether its state is terminal. SPAWN is a wired-once `SPAWN_RESOURCE_LIFECYCLES` action on the sole Negotiation definition's approval transition, resolving → pinning → initializing each resource atomically, pin-at-approval against the final resource set. Double-fire safety: source-state check for correctness, parent row lock against duplicate work.

**Must not claim:** outcome-sensitive conclusion — record it as *accommodated as later config but excluded*, not foreclosed. Clock-tick scheduling is declared and deferred.

### 0008 — Lifecycle history references States by FK and renders labels live
*Sources: 06*

Each `LifecycleRecord`'s `@Enumerated` `changedTo` column becomes a `state_id` FK to a `State` row, with timeline text rendered live by join on `state.getLabel()`. Labels are deliberately **not** frozen: editing one ripples into historical text, accepted because it is identical to today's enum-rename behaviour — snapshotting was considered and dropped. No version column on the audit tables; version is implied transitively through `State.definition_id` and recovered by join. `OVERRIDE` records its resulting state like any other row. Firing-event capture, from-state, and audit-table unification are deliberately deferred as additive.

**Must not claim:** how the *live* `current_state` column is represented — it stays a VARCHAR with no FK (0009).

### 0009 — Forward-only convert-in-place migration, stop-the-world
*Sources: 11*

A single forward-only Flyway cutover: no dual-write, no retained legacy columns. Flyway 11 Community has no `undo`, so rollback is a pre-deploy snapshot restore. The live `current_state` stays an authoritative VARCHAR resolved through the `(pinned version, name)` natural key — **no `current_state_id` FK**, which is what confines 0008's FK conversion to the audit tables — gaining the pin and losing its obsolete CHECK. Two v1 families seeded from raw frozen SQL (the Resource family carrying `is_global_default`); every negotiation and link pins to v1, drafts included. Dead states `APPROVED` and `RETURNED_FOR_RESUBMISSION` survive as transition-less, audit-only Legacy States — "dropped" means out of the active graph, not physically absent — so history and live strings still resolve; dropped *events* are fully omitted. Information requirements re-home by name (`for_event` → `event_id`) with audience defaults. Stop-the-world short maintenance window, because the migration is breaking for old code reading `for_event`; in-flight work is safe since state lives in the DB and the changes are additive. Packaging: additive DDL first, then one atomic data-cutover file ordered assert → seed → backfill → NOT NULL → drop, all id references via name and family-key subqueries.

**Must not claim:** anything that re-opens versioning (0003) or audit mechanics (0008).

## Deliberately not ADRs

- **"No new endpoint — `getPossibleEvents` keeps its shape."** Folded into 0005. Choosing *not* to change an API is the unsurprising path, and an ADR needs a reader who would wonder why.
- **IAM / LS-AAI group membership-sync mechanics** and **outcome-sensitive conclusion.** Both ruled out of scope by the map. They get a sentence inside 0006 and 0007 respectively, so the boundary is recorded where someone would look for it, without an ADR for a decision not taken.

## After drafting

Update `map.md`'s closing note — it currently claims the ADRs already exist — to point at `backend/docs/adr/`. Then commit, so the review pass has a clean fixed point.
