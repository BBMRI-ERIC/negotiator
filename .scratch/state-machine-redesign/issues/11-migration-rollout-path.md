# Migration / rollout path

Type: grilling
Status: resolved

Blocked by: 01, 04, 06, 08

## Question

How does existing production data migrate into the new definition model without breaking in-flight negotiations? Design the migration/rollout path covering three data populations:

1. **Lifecycle state data** — existing hardcoded enum values (`NegotiationState`/`NegotiationEvent`, `NegotiationResourceState`/`NegotiationResourceEvent`) become rows in the ticket 01 `State`/`Event`/`Transition` schema. Decide how the current single hardcoded Negotiation lifecycle and the current hardcoded Resource lifecycle are seeded as the initial definition **family/version** (ticket 04/08), and how each in-flight `Negotiation`/`NegotiationResourceLink` gets its immutable `definition_version_id` pin (ticket 08) backfilled so running work resolves to the same behaviour it has today.

2. **Information requirements** — existing `InformationRequirement` rows (today tying an `AccessForm` to a `NegotiationResourceEvent`) re-home onto the ticket 05/07/10 `Event`-level requirement model.

3. **Audit history** — existing enum-valued `LifecycleRecord` rows: map each legacy `changedTo` enum value → the `State` row of whichever definition version historical negotiations are deemed to have run under, then backfill the new `state_id` FK (ticket 06). Handle the "which version did history run under?" question, given all pre-migration history necessarily ran under the single seeded v1 family.

## Framing for the resolving session

- This is the **last core in-scope design ticket** — resolving it makes the spec hand-off-ready. Keep it a *design* decision (migration strategy + Flyway/Liquibase shape, ordering, reversibility, in-flight safety), not an implementation.
- All dependencies are resolved; the seeded-v1-family assumption from tickets 04/08 is the backbone — every migrated record pins to that one initial version.
- Consult `/grilling` + `/domain-modeling` per the map's Notes.
- The IAM/LS-AAI group-sync mechanics are **out of scope** for this map (see map's Out of scope) — do not pull them in.

## Answer

A single, forward-only, **stop-the-world** Flyway cutover that seeds one v1 definition family per lifecycle, pins all in-flight work to it, and re-homes information requirements and audit history — additively where possible, faithful to today's behaviour everywhere.

### 1. Overall strategy — convert-in-place, snapshot-restore rollback

The new engine is data-driven and Spring Statemachine is deleted outright (ticket 03), so there is no dual-run-both-engines option: the migration that seeds definitions and backfills pins must land *with* the cutover deploy. Given that:

- **Convert-in-place**, not retain-and-contract. No legacy state columns are kept "for rollback": a frozen copy goes stale the instant new code processes a transition (it would need dual-write, which we reject for a subsystem we're deleting), and Flyway 11 **Community has no `undo`** anyway.
- **Rollback = restore the pre-deploy DB snapshot**, accepting the inherent loss of any transitions processed post-deploy (true of any snapshot rollback). Migrations are forward-only.
- **Live state stays a plain authoritative string.** `negotiation.current_state` / the resource-link state column keep owning live state as VARCHAR — **no `current_state_id` FK**. They resolve unambiguously to a `State` row via the natural key `(pinned definition_version_id, state name)`; the pin disambiguates the version, and a string+pin key can't drift the way an FK-to-`state.id` could (an FK could point at a `State` from a different version than the pin). The only change to these columns is **dropping the now-obsolete CHECK constraint** (states are data-driven now). *Amends nothing — clarifies that ticket 06's `state_id` FK conversion applies to the **audit** column only, not live state.*

### 2. Seeding mechanism — raw SQL, frozen

The two hardcoded lifecycles are seeded as **explicit INSERTs in a committed Flyway `V*.sql` file** (wiring FKs via name/family-key subqueries — see §7). Rationale: matches the repo's 50 existing SQL migrations, fully deterministic, and self-contained so it won't rot when entity code evolves (the classic Flyway-Java-migration trap). Any future "canonical default-definition file for admin import/export" is a separate downstream feature; the seed SQL may be *generated* from such a file but is committed as frozen SQL.

### 3. Dead states — kept as inactive audit-only `State` rows (amends ticket 01)

Facts: `NegotiationState.APPROVED` and `NegotiationResourceState.RETURNED_FOR_RESUBMISSION` are **unreachable in current code** but are **enshrined in live/audit CHECK constraints** (`APPROVED` in `negotiation_current_state_check`; `RETURNED_FOR_RESUBMISSION` in `resource_state_per_negotiation_current_state_check` and the resource-lifecycle-record check) — meaning older code once wrote them, so production audit history (and possibly live state) very likely still holds them.

- **"Dropped" (ticket 01) is refined to "removed from the active transition graph," not "physically absent as a `State` row."** The v1 seed keeps `APPROVED` and `RETURNED_FOR_RESUBMISSION` as **transition-less, non-initial, inactive `State` rows** — exactly the pattern ticket 01 already uses for the `OVERRIDE` audit-only `Event`. This keeps history faithful (no rewriting), lets ticket 06's `state_id` FK resolve, lets live-state strings resolve, and never blocks the deploy.
- **Dropped *events*** (`NegotiationEvent.START`, the `RETURN_FOR_RESUBMISSION` event) are **fully omitted** — events leave no data residue (audit stores the resulting state, not the firing event, per ticket 06's deferral).
- Alternatives rejected: remapping dead values onto survivors distorts immutable history; assert-and-fail-only just blocks the deploy since the data almost certainly exists.

### 4. Pinning in-flight work to v1

The seed creates two families at v1: the single **Negotiation-lifecycle** family, and the **Resource-lifecycle** family flagged **`is_global_default = true`** (ticket 04) — at migration time every resource resolves to it (no direct/Network associations exist yet). Backfill (ticket 08's immutable `definition_version_id`):

- Every `negotiation` → Negotiation v1; every `negotiation_resource_link` → Resource v1.
- **All rows, every state, drafts included.** Everything pre-migration ran under the one hardcoded lifecycle, so everything pins to v1; a draft that submits later keeps its v1 pin (consistent with pin-at-start).
- Mechanics: add column nullable → `UPDATE` all → `SET NOT NULL` (coverage is total).

### 5. Re-homing information requirements

Legacy `information_requirement(required_access_form_id, for_event VARCHAR)` → ticket 05/07/10 model. `for_event` is typed `NegotiationResourceEvent`, confirming today's IRs are strictly resource-lifecycle.

- **Event attachment:** add `event_id` FK, backfill by name-matching `for_event` → the v1 Resource-definition `Event` of the same name, then drop `for_event`; `required_access_form_id` carries over. Pre-flight assert every distinct `for_event` maps to a seeded `Event`.
- **Audience** (ticket 10): every legacy row → `RESOURCE_REPRESENTATIVES`, empty `audience_params` — the faithful reproduction of today's implicit "the resource's own representatives".
- **Quantifier** (ticket 10): every legacy row → `ANY` (threshold 1) — matches today's "any qualifying submission satisfies".
- **`submittedBy`** (ticket 05): **`NULL` for all legacy submissions.** `information_submission` never recorded an author (no `@CreatedBy`), so there is nothing to backfill and inferring one would be fabricated data. Only cost: a resource sitting *exactly* at a not-yet-fired requirement-bearing event with a pre-existing submission won't count toward ticket 10's `submittedBy ∈ audience` check, so the representative resubmits once. Every already-advanced resource is a committed fact never re-evaluated (ticket 10), so it's unaffected.

### 6. Audit history backfill

Each `*_lifecycle_record.changed_to` string → the `state_id` of the same-named `State` in the **matching v1 definition** (negotiation records → Negotiation v1 States, resource records → Resource v1 States; the inactive dead states from §3 included). "Which version did history run under?" is trivial — there is exactly one seeded version, v1 — so no per-record version reasoning, consistent with ticket 06 carrying **no version column** on the audit tables.

### 7. Deploy choreography — stop-the-world cutover

The migration is **breaking for old code** (it drops `information_requirement.for_event`, which old `ResourceLifecycleServiceImpl` reads, and drops CHECK constraints old code assumes), so it cannot ride a rolling zero-downtime deploy — old-code pods coexisting with the migrated schema would throw.

- **Declared a stop-the-world cutover:** drain/stop all old-version instances, *then* start the new version (Flyway runs on boot). A **short maintenance window** is proportionate for a core-subsystem replacement.
- **In-flight negotiations are safe** across it: their state lives in the DB (`current_state` strings + freshly backfilled pins), and the cutover touches it only additively.
- Engineering a rolling-safe migration would force expand/contract with a compatibility shim in old code — rejected as not worth it for a deleted subsystem.

### 8. Packaging, ordering & atomicity

- **Additive DDL first**, its own migration: create the definition/`State`/`Event`/`Transition`/wiring/requirement-addition tables and add the new columns *nullable* (pins, audit `state_id`, IR `event_id`/`audience`/quantifier, `submitted_by`). Harmless on its own.
- **The data cutover as one atomic `V*.sql` file** (Postgres DDL+DML is transactional → all-or-nothing; never half-migrated), ordered:
  1. **Pre-flight asserts** (`DO` block `RAISE EXCEPTION`) — no live/audit/IR value falls outside what the seed will provide; runs first, before anything destructive, so a surprise aborts with the snapshot intact.
  2. **Seed v1** — both families (Resource = global-default), all states (active + inactive dead), events, transitions, `required_authority`, `initial`/`terminal` flags, guard wiring (incl. `NEGOTIATION_APPROVED`), the `SPAWN_RESOURCE_LIFECYCLES` action on the approval transition — a faithful transcription of the current `NegotiationStateMachineConfig`/`ResourceStateMachineConfig`.
  3. **Backfill** pins → audit `state_id` → IR rehome.
  4. `SET NOT NULL` on the now-populated required columns.
  5. **Drop** `for_event`, the obsolete CHECK constraints, and the converted audit `changed_to` VARCHAR.
- **All id references via name/family-key subqueries**, never hardcoded seed ids (robust against Postgres-assigned sequence values). Flyway's version tracking means no re-run, so no idempotency guards.

### Amendments to prior tickets

- **Ticket 01** — "dropped" dead **states** (`APPROVED`, `RETURNED_FOR_RESUBMISSION`) are refined to "removed from the active transition graph," surviving in the v1 seed as inactive transition-less audit-only `State` rows (like `OVERRIDE`). Dropped **events** remain fully omitted.
- **Ticket 06** — clarified: the `state_id` FK conversion is the **audit** column only; live `current_state` stays a plain string.
- **Ticket 04** — the seeded Resource family is the one carrying `is_global_default = true`.

No new tickets surfaced; nothing ruled out of scope. This was the last core in-scope design ticket — the spec is now hand-off-ready.
