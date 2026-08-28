# PRD — Decouple consumers from the Lifecycle enums

Status: resolved — all twelve slices landed, slab gate green, map ticket 07 closed 2026-08-28

Slab for map ticket
[07 Decouple consumers from the lifecycle enums](../state-machine-implementation/issues/07-decouple-consumers-from-enums.md)
of [State machine implementation](../state-machine-implementation/map.md). Branch:
`feat/state-machine-implementation`, already rebased on `master`.

**The recon for this slab lives in ticket 07 itself** — its *Carve-out 2, recorded* and *Blast
radius, measured* sections carry the file lists, the per-subsystem counts and the five findings that
contradict the tickets this PRD is written against. Read them rather than re-deriving; every number
below comes from there and was measured at `3de318c3`.

Two decisions this slab applies mechanically and must not reopen:
[ticket 02](../state-machine-implementation/issues/02-state-triggered-behaviour-location.md) (what
becomes an Action) and
[ticket 03](../state-machine-implementation/issues/03-state-event-identity-downstream.md) (what
names a State or an Event). Where this PRD departs from 03, it says so and files the trigger.

## Problem Statement

Four Java enums — `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent`,
`NegotiationResourceEvent` — are how this codebase names a State or an Event today. Each enum
declares the complete set, fixed at compile time, and 42 production files across five subsystems
depend on that closed set: they `switch` over it, they hold it in DTO fields and JPA Criteria
predicates, they read labels and descriptions off it, and they use its `ordinal()` as a sort key.

ADR [0002](../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md)
deletes all four. After it, a State exists only as a row of a Definition Version, and which States
exist depends on which Definition Family a Lifecycle resolved to — so no Java type can enumerate
them.

The danger is not the deletion, it is the *timing*. If those 42 files move in the same change that
replaces Spring Statemachine with the Transition Evaluator, the result is a single commit touching
most of the application, in which a mis-mapped DTO field and a wrong Transition look identical from
a failing test. There would be no way to attribute a regression, and nothing green to bisect back
to.

## Solution

Move the 42 consumers off the enums **first, while Spring Statemachine is still running and every
test is green**, one subsystem per commit. Standing decision 2: make the change easy, then make the
easy change.

A State or an Event becomes a bare `String` at every boundary outside the Lifecycle. Nine names that
some behaviour depends on *existing* — the five Well-known States, three Resource States that Spawn
writes, and the Override Event — keep Java constants in three narrow holders. Everything else
carries the name as data, off the column, the request or the Definition Version Pin.

Nothing observable changes. The enums already persist as `VARCHAR` and already serialise as JSON
strings, so the database, the wire format and the rendered PDF are byte-identical before and after.
This slab moves types, not logic, and the 255 characterization tests are the proof at every commit.

When it closes, replacing Spring Statemachine touches the two Lifecycle services and little else.

## User Stories

1. As the developer of the cutover slab, I want the consumer churn already done and verified, so that swapping in the Transition Evaluator is a small change I can attribute a regression to.
2. As the developer of the cutover slab, I want every commit of that churn to have been green on its own, so that I can bisect a late failure to one subsystem rather than to a 100-file change.
3. As the developer of the cutover slab, I want one named, disposable place holding the last compile-time knowledge of the closed State set, so that I delete a class rather than hunt for assumptions.
4. As the developer of the cutover slab, I want to know that no production code reads the Definition Version tables yet, so that the inertness guard is still a meaningful gate when I arrive.
5. As the developer of the coupling slab, I want the Spawn loop's enum references already swapped for constants, so that I relocate a method whose body is already String-shaped.
6. As the developer of the coupling slab, I want that loop otherwise untouched, so that the relocation diff shows only the relocation.
7. As the developer of the migration slab, I want the fourteen State names embedded in raw SQL recorded as a standing fact rather than a one-off grep, so that I know exactly which literals the seed and the audit-table conversion must satisfy.
8. As the developer of the Information Requirements slab, I want `forEvent` already carrying an Event name as a string, so that re-homing a requirement onto a Definition Version's Event is a data question and not also a type change.
9. As the developer of ticket 04, I want every label and description read through one named provider, so that replacing it with a join to the `state` row is a single substitution.
10. As a reviewer, I want a mechanical test proving no production code outside the Lifecycle package names one of the four enums, so that "decoupled" is a checked fact rather than a claim in a commit message.
11. As a reviewer, I want that test to also cover the four seam signatures, so that a consumer cannot stay coupled by calling an enum-returning method without importing the type.
12. As a reviewer, I want the parity suite green and unchanged at 255 tests after every slice, so that behaviour neutrality is demonstrated ten times rather than asserted once.
13. As a reviewer, I want the intended-delta suite still at 8 passing tests, so that I know this slab changed nothing ADR 0005 is supposed to change later.
14. As a webhook subscriber, I want the JSON payload of a state-change delivery to be byte-identical, so that my integration does not break when the Negotiator upgrades.
15. As a webhook subscriber, I want the published OpenAPI schema to still document an example State name, so that the loss of the `enum` constraint does not leave the field undiscoverable.
16. As an API consumer, I want `?status=UNKNOWN` to keep returning 400, so that a typo is reported rather than silently answered with an empty page.
17. As an API consumer, I want an unknown Event in a lifecycle URL path to keep returning its current status code, so that client error handling is unchanged.
18. As an API consumer, I want every State and Event field to keep the same JSON name and the same string value, so that no response shape changes.
19. As a requester, I want the PDF summary of my Negotiation to render the same status text as before, so that a document I already downloaded still matches a new one.
20. As a requester, I want to keep being notified when my Negotiation is submitted, declined or abandoned, so that the change of internal types costs me no notification.
21. As a requester, I want a Negotiation in `DRAFT` to stay invisible to representatives and network viewers, so that unsubmitted work is not exposed.
22. As a Resource representative, I want the notification about a Resource's State change to keep naming both States by their human labels, so that the message stays readable.
23. As a Resource representative, I want to keep being contacted when a Negotiation is approved, so that Spawn's notification behaviour survives the type change.
24. As a network manager, I want every network statistic to report the same number as before, so that a KPI does not silently move because a query changed shape.
25. As an administrator, I want the Information Requirement admin screens to keep working, so that the change to `forEvent` costs no admin downtime.
26. As an administrator (eventually, in stage 3), I want behaviour that names a State to name it as a Well-known State in one place, so that I can see what a custom Definition Family must provide.
27. As an administrator (eventually, in stage 3), I want the number of such names to be nine and not thirty, so that authoring a Definition Family is not constrained by Java.
28. As an operator, I want this slab to add no Flyway migration, so that landing it carries no schema risk and no coupling to the cutover.
29. As an operator, I want the frontend to need no change, so that backend and frontend stay deployable independently through this work.
30. As a maintainer, I want the dead `NegotiationStatusConverter` deleted rather than migrated, so that the codebase does not carry a converter nothing registers.
31. As a maintainer, I want the 30 affected test files to change alongside the subject they test, so that no commit is a lump of unrelated test churn.
32. As a maintainer, I want the two independent subsystems to be landable at any point in the order, so that the slab does not serialise work that has no dependency.

## Implementation Decisions

### Applied from ticket 03, not re-decided

- **A State or an Event is a bare `String`.** No value object, no demoted enum, no per-consumer
  variation. Every boundary is already a string underneath: `@Enumerated(EnumType.STRING)` on the
  column, JSON strings on the wire.
- **Three holders, nine names.** `WellKnownNegotiationStates` (`DRAFT`, `SUBMITTED`, `IN_PROGRESS`,
  `DECLINED`, `ABANDONED`), new `WellKnownResourceStates` (`SUBMITTED`, `REPRESENTATIVE_CONTACTED`,
  `REPRESENTATIVE_UNREACHABLE`) and new `WellKnownResourceEvents` (`OVERRIDE`). A name earns a
  constant only when some behaviour depends on that name existing. No holder declares a complete
  set — that would be the enum again.
- **The `DRAFT` visibility rule stays a name**, as a single-column comparison on the hot Negotiation
  list query. Not a State flag, and emphatically not "hide the initial State": the initial State is
  `SUBMITTED` and nothing targets `DRAFT`, so that generalisation would reveal drafts.
- **The four network statistics keep their raw SQL untouched.** Behaviour-identical, parity-safe,
  and the divergence gap is already ticketed as
  [10](../state-machine-implementation/issues/10-network-kpis-name-resource-states.md) for stage 2.
- **OpenAPI goes uniformly to `type: string`,** keeping `@Schema(example = …)`. The `enum`
  constraint is lost; the wire format is not.
- **`NegotiationStatusConverter` is a deletion, not a migration.** `WebConfig` registers
  `NegotiationEventConverter`, `NegotiationRoleConverter` and `NegotiationResourceEventConverter`
  and never this one — verified again during recon. Its only reference is `unit/converters/ConverterTest`,
  which **survives**: that class also covers the unrelated `NegotiationRoleConverter`, so one test
  method goes rather than the whole file. Ticket 03 records the class as going with it, which
  overstates it.

### The Enum-Backed Lifecycle Catalog — the one new component

Ticket 03's decision 3 says `?status=` must be validated against "a State row with this name in the
Negotiation-scope active version". **This slab cannot do that, and must not try.** The Definition
Version tables exist but are empty until the migration slab writes the seed, so the lookup would
refuse every value and take the parity suite red. It would also make this the first slab to read
those tables, which per the map obliges it to delete `DefinitionInertnessGuardTest` a whole stage
early.

Instead, one package-private component **inside `negotiation/state_machine/`** answers three
questions that need the closed set while the closed set still legitimately exists:

- **Does this name exist?** — for `?status=`, preserving today's 400 exactly.
- **What is this name's label and description?** — for the notification body that names both
  Resource States, and for the assemblers that put a label on a link.
- **What is this Resource State's ordinal?** — for the metadata DTO the frontend sorts by.

It is backed by the enums, which is why it lives where the enums live: the gate permits enum
references there, the fixed set is honest for exactly as long as Spring Statemachine runs, and the
cutover deletes the class together with the library. Named for disposability and by deliberate
symmetry with the test-scope `EnumBackedLifecycleTestAdapter` that ticket 01 built for the same
reason.

**This is a departure from ticket 03's decision 3, and it is filed as one.** The decision's
*intent* — the 400 survives, and the check is not a hand-maintained Java list — is preserved. Only
the source of truth is deferred. The cutover slab replaces the catalog's three methods with reads of
the `state` and `event` rows; that is the trigger, and it is recorded in the slab issue as well as
here.

### The seam, and why it is a slice of its own

`NegotiationStateChangeEvent` and `ResourceStateChangeEvent` expose their States and Event as enums;
`NegotiationLifecycleService` and `ResourceLifecycleService` return and accept them. All four types
are inside `negotiation/state_machine/`, so the slab gate permits them — but a consumer can call
`getToState().name()` and import nothing, which means a guard built from Java identifiers reports
green over code that is still fully type-coupled. This is slab 08's lesson in mirror image, and it
means converting these four types is a deliberate slice rather than a consequence of the others.

**The adapter pattern that makes the order work.** Slices 03–08 convert each consumer's own fields
and signatures to `String` and translate at the call boundary. Slice 09 then flips the four seam
types and removes those translations. Every intermediate commit is behaviour-identical by
construction, because the translation is the identity function on the name. The alternative — flip
the seam first — puts all 42 files in one commit, which is exactly what this slab exists to avoid.

The three assemblers that hold a Lifecycle service as a dependency belong to slice 09 for the same
reason, not to the mapper slice they otherwise resemble.

### Entities and JPA

`Negotiation.currentState` and `NegotiationResourceLink.currentState` become `String` and lose
`@Enumerated`. The columns are already `VARCHAR` and are not touched, so **this slab adds no Flyway
migration at all.**

`NegotiationSpecification.hasState` takes `List<String>`, and this is why the entity fields and the
Criteria predicates must move in the same slice: a predicate comparing an enum-mapped path to a
`String` is not reliably coercible, so splitting them would leave an intermediate commit whose
behaviour depends on Hibernate's willingness to guess.

`NegotiationRepository`'s derived queries follow the field type. Its JPQL at the unquoted
`!= DRAFT` fails loudly at Hibernate query validation once the field is a `String`, which is the
good case — the two literals in its native query do not, which is the bad one.

### The carve-outs

- **The three metadata DTOs are excluded** — `NegotiationStateMetadataDto`,
  `ResourceStateMetadataDto`, `ResourceEventMetadataDto`. They belong to ticket
  [04](../state-machine-implementation/issues/04-global-state-event-metadata-contract.md), which asks
  whether an endpoint enumerating a *universe* of States still makes sense. The slab gate names them.
- **Carve-out 2 is two files, and only the Spawn behaviour inside them.** Ticket 02 relocates exactly
  one thing into an Action: the per-Resource assign-and-accumulate loop in
  `ResourceNotificationService`, together with the `IN_PROGRESS` trigger in
  `NegotiationInProgressHandler`. This slab swaps their four enum references for holder constants
  **in place** and changes nothing else — no new event, no transaction change, no restructuring. The
  carve-out excludes a relocation, not a type swap, and it has to: the gate exempts only the three
  metadata DTOs, and ticket 03's holder table assigns these exact call sites to this slab.

### The fifth subsystem, and the fourth kind of work

Two things the tickets did not know, both recorded in ticket 07 during recon:

- **Information Requirements is a fifth consumer subsystem** that no commit-order line names. Four
  files, including the persisted `forEvent` field and `existsByForEvent`. The column is already
  `VARCHAR(255)`, so no migration and no data change — only the Java type. The map's standing
  decision 6 governs what happens to `forResourceEvent` *later*; this slab only makes it a name.
- **There is a fourth kind of work beyond ticket 03's three.** About ten sites use an enum as a
  lookup table rather than as an identity — label, description, value, ordinal. These are what the
  catalog exists for, and `ResourceStateChangeHandler` is the clearest case: ticket 02 concluded it
  "needs no change", but the type swap deletes the `getLabel()` it calls.

### Slice order

This is a wide refactor, so it sequences as **expand–contract** rather than as tracer bullets.
Slices 01–03 expand: the new forms land beside the enums and nothing breaks. Slices 04–09 migrate
call sites in batches sized per subsystem, each green on its own because the enums still exist.
Slices 10–11 contract the seam and the persistence layer. The final contract — deleting the enums
themselves — belongs to the cutover slab, so what closes *this* slab is slice 12's proof that no
consumer reference remains.

| # | Slice | Blocked by |
|---|---|---|
| 01 | [The three Well-known name holders](issues/01-well-known-name-holders.md) | — |
| 02 | [The Enum-Backed Lifecycle Catalog](issues/02-enum-backed-lifecycle-catalog.md) | — |
| 03 | [Pin the raw State names in SQL](issues/03-pin-the-raw-state-names-in-sql.md) | — |
| 04 | [Webhook payloads name States as strings](issues/04-webhook-payloads-name-states-as-strings.md) | 01 |
| 05 | [Notification handlers name States as strings](issues/05-notification-handlers-name-states-as-strings.md) | 01, 02 |
| 06 | [Network statistics name States as strings](issues/06-network-statistics-name-states-as-strings.md) | 03 |
| 07 | [Information Requirements name their Event as a string](issues/07-information-requirements-name-their-event-as-a-string.md) | — |
| 08 | [DTOs, mappers and the Negotiation timeline](issues/08-dtos-mappers-and-the-negotiation-timeline.md) | 01, 02 |
| 09 | [Resource governance names States as strings](issues/09-resource-governance-names-states-as-strings.md) | 01 |
| 10 | [The Lifecycle seam deals in strings](issues/10-the-lifecycle-seam-deals-in-strings.md) | 01, 02, 04, 05 |
| 11 | [Entities and JPA queries name States as strings](issues/11-entities-and-jpa-queries-name-states-as-strings.md) | 01, 03, 05, 08, 09 |
| 12 | [The decoupling gate](issues/12-the-decoupling-gate.md) | 01–11 |

Slices 01, 02, 03 and 07 have no dependency on each other and may be authored in parallel — but
**their test runs must be serialized**, because two concurrent Maven invocations against `backend/`
present as ~150 bogus failures. The 30 affected test files change with the slice that changes their
subject, never in a lump.

Slice 11 depending on 05, 08 and 09 is not a compile dependency. Those three write translations
against the entity's state accessor, and slice 11 deletes them; landing 11 first would work but
would force each of them to write a translation and remove it inside the same slab.

## Testing Decisions

A good test here asserts **externally observable behaviour that a consumer could notice** — the JSON
a subscriber receives, the status code an API client gets, the text in a notification body, the rows
a filter returns. It never asserts a Java type, an annotation or a mapping internal. That rule has
unusual force in this slab: the whole claim is that *nothing observable changed*, so a test asserting
that a field is now a `String` would be asserting the one thing that must not matter.

Almost all of the safety net already exists and is not rewritten.

- **The parity gate is the primary seam, and it is an existing one.** The parity half of
  [parity-gate.md](../state-machine-implementation/parity-gate.md) must report **255 tests in 24
  classes, 0 failures, 1 skipped** after *every* slice, and the intended-delta half must stay at
  **8 tests, 0 failures**. Those tests name States and Events as strings only and reach the
  Lifecycle through `LifecycleTestAdapter`, so they are already written to survive this slab. Read
  the numbers out of `backend/target/surefire-reports/`, checking mtimes, not out of a summary line.
- **Per-slice, the highest existing seam for that subsystem.** Webhook deliveries through
  `WebhookEventMapperTest` and `WebhookEventListenerIntegrationTest`; notifications through the
  seven `notification/internal/*HandlerTest` classes; the REST surface through
  `NegotiationControllerTests` and `ResourceControllerTests`; persistence through
  `NegotiationRepositoryTest` and `ResourceRepositoryTest`. Prefer extending one of these to adding
  a class.
- **The wire format is asserted, not assumed.** The webhook slice adds an assertion on the serialised
  JSON of a state-change payload — field names and string values — because "enums already serialise
  as strings" is a claim about Jackson's behaviour and this slab's value rests on it.
- **Three behaviours are currently pinned by nothing and get a test before they are touched.**
  `?status=UNKNOWN` returning 400 (ticket 03 established no test anywhere exercises it), an unknown
  Event in a lifecycle URL path, and the PDF summary's rendered status text. The PDF one matters
  most: neither PDF file names an enum type and both compile clean either way, so only a test
  distinguishes "still correct" from "silently changed". `NegotiationControllerTests`'
  `sortBy=UNK` / `sortOrder=UNK` cases are the prior art for the two 400s.

Two new mechanical guards, both modelled on `CharacterizationImportGuardTest` and
`DefinitionInertnessGuardTest` — plain JUnit text scanning over a working-directory-resolved source
root, comment blanking, named exemptions, and an anti-vacuity test that fails if the scan finds
nothing:

- **The decoupling guard**, the slab gate in executable form. No production source outside
  `negotiation/state_machine/` and the three named metadata DTOs may contain
  `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent` or `NegotiationResourceEvent`
  as a whole word. Word boundaries are load-bearing: `NegotiationStateChangeEvent` and
  `ResourceStateChangeEvent` are application events consumers legitimately name, and
  `CharacterizationImportGuardTest` already documents that trap. **This guard carries a second rule
  that the identifier scan cannot express** — a reflective check that the four seam accessors
  (`getFromState`, `getToState`, `getEvent` on both change events) and the two Lifecycle service
  interfaces return and accept `String`. Without it the guard goes green over a codebase where every
  consumer reaches an enum through a method call, which is precisely the failure slab 08 hit in its
  table rule and warned the next slab about.
- **The raw-literal guard.** Ticket 03 says this slab "owes a manual sweep" for State names in SQL,
  and a one-off grep passes today and rots tomorrow. Instead the fourteen known literals are
  recorded as an expected set — file, line reason, and name — and the guard fails on any literal
  that appears, disappears or moves. It must scan for **bare names, not names inside quotes**: the
  eleven in the network-stats repository live in Java text blocks, and a quoted-literal regex finds
  none of them. This is the fact that hands the migration and ADR 0008 slabs their work.

The guards live in the test tree and scan production sources, so unlike
`CharacterizationImportGuardTest` they need no exemption for themselves.

**Do not run two Maven invocations against `backend/` at once** — slab 08 recorded that a concurrent
recompile clears `target/test-classes` under a running suite and presents as ~150 unrelated
failures. A sub-agent that verifies by running tests counts as a second invocation.

## Out of Scope

- **Deleting Spring Statemachine, or any part of it.** The library runs, green, throughout. The
  27 classes in `negotiation/state_machine/` keep their enum references.
- **Any behaviour change whatsoever.** If a consumer notices, this slab has failed.
- **Any Flyway migration.** The columns are already `VARCHAR`; nothing in this slab needs DDL.
- **Reading the Definition Version tables.** `DefinitionInertnessGuardTest` stays green and stays
  alive — the slab that first reads them deletes it as a visible line in its own diff, and that slab
  is not this one.
- **The three metadata DTOs, and the Resource State `ordinal` ordering contract** — ticket 04.
- **The semantics of the network KPI State names** — the SQL is untouched here; the divergence
  question is ticket 10, in stage 2.
- **Relocating the Spawn loop into `SPAWN_RESOURCE_LIFECYCLES`, and the new
  `ResourceLifecyclesSpawnedEvent`** — the coupling slab, per ADR 0007 and ticket 02's decision 3.
- **Validating `?status=` against the Definition Version's State rows** — deferred to the cutover
  slab with a named trigger, for the reason given above.
- **Everything ADR 0005 changes** — Possible Events omitting blocked Events, the array-valued
  requirement rels, the display name. The eight intended-delta tests must still pass, which is the
  check that this slab left them alone.
- **The audit tables' `changed_to` columns and their conversion to a `state_id` FK** — ADR 0008.
- **Frontend changes.** The wire format is identical and the frontend generates no client from the
  OpenAPI schema — it hand-codes the name strings it needs. Standing decision 5 still applies, so
  the claim is verified by running the app and looking, not assumed.

## Further Notes

- **The map's "~60 main-source files" is the total including the Lifecycle package.** The consumer
  population is **42**; 23 of the package's 27 classes reference an enum, for 65 in main sources
  overall. Test sources are **30**, not the 26 the map records. Corrected in ticket 07.
- **The invisible SQL literals are 14 across two files, not 8 across one.** Ticket 03 attributed all
  of them to the network-stats repository and missed one of its lines entirely as well as both
  literals in `NegotiationRepository`'s native query. The third literal there — the unquoted JPQL
  enum reference — fails loudly at Hibernate query validation rather than silently, which makes it
  the safe one.
- **The order of slices 03–08 is not load-bearing beyond the recorded blockers.** Any order that
  respects them produces the same end state; the table's order simply puts the cheapest and most
  isolated first, as ticket 07 suggests.
- **Slice 09 may want splitting once it is underway.** It is the largest and the only one whose size
  is not well predicted by a file count, because the four seam types have many call sites. That is a
  discovery to make against real code, not a guess to encode here.
- **`InformationRequirement.isViewableOnlyByAdmin`** remains a live field appearing in no ADR. This
  slab touches its file without touching it. Still unowned; the map records it.
- **PostgreSQL only**, per the map's binding constraints.
- Run the formatter before committing any Java, since it is not bound to the `test` phase:
  `nix develop .#opencode --command mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format`.
