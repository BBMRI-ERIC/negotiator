# Lifecycle history rows for both graphs

Status: ready-for-human

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin the audit history a transition writes, for both graphs, because ADR 0008 converts these tables'
state column to a foreign key and the conversion has to preserve what is there.

Every Negotiation transition writes a Negotiation lifecycle record, and every Resource transition
writes a Resource lifecycle record carrying the Resource it concerns. Pin, for a real transition
driven through the adapter: that exactly one row appears, which state it records as the state changed
to, that it is associated with the right Negotiation — and for the Resource records, the right
Resource — and that the auditing columns are populated.

Two things to establish rather than assume, since both bear directly on the migration:

- Whether a record captures only the destination state or also the origin. If only the destination,
  say so explicitly in this issue, because reconstructing a transition from the audit trail then
  depends on row ordering, which the migration must preserve.
  **Answered: only the destination.** Not the origin, and not the event either. Row order is
  load-bearing and the only ordering key is the identity `id`. Finding 1.
- Whether a refused send writes a row. It should not, but the Resource service's silent refusal
  returns normally, so this is worth an explicit negative test.
  **Answered: it does not**, for either service, across all three shapes of refusal each.

Also pin the accumulation across a multi-step path — several transitions in sequence produce several
rows in order — since the seeded test data already contains history rows and the migration's backfill
joins on state names across the whole table.

## Acceptance criteria

- [x] A single Negotiation transition is pinned as writing exactly one Negotiation lifecycle record,
      with the expected destination state and Negotiation. (All eight Transitions, not one.)
- [x] A single Resource transition is pinned as writing exactly one Resource lifecycle record, with
      the expected destination state, Negotiation and Resource. (All thirteen Transitions.)
- [x] The auditing columns on both record types are pinned as populated.
- [x] Whether the origin state is recorded is established and stated in this issue. **It is not** -
      see finding 1, and the two tests that fire it rather than read it off the entity.
- [x] A refused Negotiation send is pinned as writing no record.
- [x] A refused Resource send is pinned as writing no record, despite returning normally.
- [x] A multi-step transition path is pinned as producing the expected records in order.
- [x] Records written by these tests are pinned as distinguishable from those already present in the
      seeded test data.
- [x] All assertions use Awaitility with a bounded timeout. (Precisely: every observable the
      asynchronous persist path writes is reached through `LifecyclePersistence` first, with a
      settling period wherever the claim is that no row was written. The plain assertions that
      follow read rows whose arrival was already awaited.)
- [x] Every State and Event is named as a string; the forbidden-import guard passes.
- [x] No production code is modified.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)

## What landed

| File | What it does |
|---|---|
| `characterization/service/LifecycleHistory.java` | new: the suite's single reader of the two Lifecycle Record tables, and the only place that knows the State comes out of a column called `changed_to` |
| `characterization/service/NegotiationHistoryRowsTest.java` | 16 tests: the eight-Transition walk, auditing columns, destination-only, multi-step order, declared State names, three refusals, seeded-versus-written |
| `characterization/service/ResourceHistoryRowsTest.java` | 21 tests: the same seven statements over the thirteen-Transition graph |

No existing file was modified. Parity gate after: **226 tests, 0 failures, 0 errors, 1 intentional
skip** (was 189).

## Findings

**1. A Record captures the destination State and nothing else - not the origin, not the Event.** Both
entities carry exactly one Lifecycle field, `changedTo`. This was settled by firing rather than by
reading the entity: `ABANDON` reaches `ABANDONED` from both `IN_PROGRESS` and `PAUSED`, and `CONTACT`
reaches `REPRESENTATIVE_CONTACTED` from both `SUBMITTED` and `REPRESENTATIVE_UNREACHABLE`; in each
pair the two rows are *equal* once the identity and the two timestamps are removed. So:

- reconstructing which Transition ran depends entirely on **row order**, and the only ordering key
  the tables have is the identity `id`. `creation_date` agreed with `id` order in every path pinned
  here, but it is not unique by construction and nothing enforces the agreement. ADR 0009's
  conversion must preserve the ids, or at least their relative order;
- the **first** row of a trail has no recoverable origin at all. `negotiation-2` driven from
  `SUBMITTED` leaves a first row naming `IN_PROGRESS` and nothing anywhere records that it came from
  `SUBMITTED`;
- this matches ADR 0008's own deferrals ("the from-State stays derivable as the previous record",
  "the firing Event is still not captured"). The ADR is accurate; it is now also tested.

**2. The trail records State *assignments*, not Transitions.** The Negotiation row is written by
`Negotiation.setCurrentState` (`Negotiation.java:132-135`) and the Resource row by
`Negotiation.setStateForResource` -> `buildResourceStateChangeRecord` (`:163-171`, `:225-233`) - that
is, by the entity, on any write, not by the Lifecycle machinery. Three producers therefore exist that
no Transition accounts for, all out of this ticket's seam and all recorded here rather than pinned:

- **spawn.** `ResourceNotificationService.handleResourceStateManagement` writes a Resource Record
  per Resource when a Negotiation reaches IN_PROGRESS - `REPRESENTATIVE_CONTACTED` where the Resource
  has representatives, `REPRESENTATIVE_UNREACHABLE` where it has none. Ticket 08's seam;
- **the Override path.** `ResourceServiceImpl.updateResourceStatus` writes one for an arbitrary
  State (ticket 04, finding 10);
- **automatic conclusion.** `ResourceStateChangeListener` concludes through
  `authenticatedUserContext.runAsSystemUser(...)`, which swaps the SecurityContext for Person **0**
  for the duration of the `CONCLUDE`, so the Negotiation Record an automatic conclusion writes is
  attributed to the system user and not to the caller whose Resource Event triggered it. Anyone
  reading the trail as "who did this" needs to know that. Inferred rather than pinned - it is ticket
  08's seam - but the inference is safe: every Record written here carries the firing caller, so the
  persist path demonstrably runs on the caller's thread and therefore inside the swapped context.

The subject for the Negotiation half was chosen to keep all three out of the picture:
`negotiation-2` has no Resources.

**3. `buildResourceStateChangeRecord` silently drops `SUBMITTED`.** `Negotiation.java:226` -
`if (!state.equals(SUBMITTED))`. A Resource moved to `SUBMITTED` writes no Record at all. This is
**unreachable through the Lifecycle seam**: no Resource Transition targets `SUBMITTED`, and spawn
writes `REPRESENTATIVE_CONTACTED`/`REPRESENTATIVE_UNREACHABLE` rather than the initial State. The
only live route to it is an Override to `SUBMITTED` through `updateResourceStatus`. Deliberately not
pinned - it is not in this seam - but a reimplementation that dropped the special case would start
writing rows this audit trail has never contained.

**4. The auditing columns are populated, by two mechanisms that look identical from outside.** For a
Negotiation Record all four come from Spring Data's `AuditingEntityListener` on `AuditEntity`, with
`created_by`/`modified_by` resolved from the SecurityContext by `AuditorAwareImpl`. For a Resource
Record, `buildResourceStateChangeRecord` *additionally* sets `creationDate` and `modifiedDate` by
hand before persist. Observationally the two are indistinguishable - all four columns carry the
firing caller and the current time, which is what is pinned - so the hand-set dates are belt and
braces, not behaviour, and can go.

**5. Records are not deduplicated, and must not become so.** The Negotiation holds both collections
in a `HashSet` and neither Record type overrides `equals`/`hashCode`, so identity is object identity
and revisiting a State leaves a second row. Pinned by the multi-step path, which passes through
`IN_PROGRESS` twice and leaves four rows. Giving the Record value-based equality after the FK
conversion - `state_id` plus `negotiation_id` looks like a natural key - would silently lose every
revisit.

**6. Every recorded State name is one the Definition declares, Legacy States included.** Pinned over
the whole table in both classes, against `NegotiationGraphV1.allStateNames()` /
`ResourceGraphV1.allStateNames()` (which the binding tests equate to the committed dump). This is the
precondition ADR 0009's backfill rests on, since it resolves the table by name. Note the universe
that has to hold is the **declared** one, not the reachable one: dropping `APPROVED` or
`RETURNED_FOR_RESUBMISSION` from the seeded States would strand any row naming them.

**7. The verbatim before-picture, recorded here rather than asserted anywhere.** Per ticket 09's
rule, no assertion in this ticket names anything the redesign deletes. What the redesign deletes:

- both entities declare the State as `@Enumerated(EnumType.STRING) private <enum> changedTo;`
  (`NegotiationLifecycleRecord.java:31-32`, `NegotiationResourceLifecycleRecord.java:38-39`);
- both tables store it as `changed_to varchar(255)` under a `CHECK` constraint that **enumerates the
  State names**: `negotiation_lifecycle_record_changed_to_check` (7 names in
  `B1__Baseline_migration.sql`, re-created with `DRAFT` added by
  `V22.0__add_draft_state_to_check_constraint.sql`, so 8) and
  `negotiation_resource_changed_to_check` (12 names, `V2.0__Add_auditing_to_missing_tables.sql`).
  These constraints are a third place the State universe is written down, after the enums and the
  Definition config, and ADR 0009's conversion has to drop them - a converted `state_id` cannot
  satisfy a check against varchar State names.

**8. One file changes at cutover, deliberately.** `LifecycleHistory` is the suite's only reader of
these two tables; everything above it speaks of a recorded State as a string and of a Resource by its
`source_id`. The FK conversion is therefore one join in one file, and every assertion in the two test
classes stays byte-identical - the same shape of argument the adapter makes for the services. It also
resolves `resource_id` to `source_id` there, so no row id leaks into an assertion.

**9. Corpus facts the next ticket should not rediscover.** The seed carries one Negotiation Record
(`IN_PROGRESS`, `negotiation-1`, 2023-06-19) and three Resource Records (`REPRESENTATIVE_CONTACTED`
for `negotiation-1`/resource 4; `REPRESENTATIVE_CONTACTED` and `RESOURCE_AVAILABLE` for
`negotiation-3`/resource 5). Consequences:

- `negotiation-2` has **no** Negotiation Record, which is what makes "exactly one row" assertable on
  the Negotiation half without a baseline;
- the Resource half has no such luck, and worse, the seeded row for the subject Resource names
  `REPRESENTATIVE_CONTACTED` - the exact State the obvious first Transition (`CONTACT` from
  `SUBMITTED`) leads to. A test that waited for "the last row names `REPRESENTATIVE_CONTACTED`" would
  pass before the send. Every Resource expectation here is therefore "the trail as it stood, plus
  what the send added", and the waits are on row *count*;
- every seeded row is dated 2023-2024, so a timestamp taken at the start of a method separates seeded
  from written cleanly. That is what the seeded-versus-written tests use, alongside the id and the
  Negotiation.

**10. Writing a starting State with SQL writes no Record, which is what makes the walks possible.**
`SeededNegotiationSubject.putInState` and `SeededResourceSubject.putResourceInState` go at the column
directly and bypass `setCurrentState`, so setup leaves the trail untouched. A future helper that
placed a subject by *driving* a path would quietly add rows to it.
