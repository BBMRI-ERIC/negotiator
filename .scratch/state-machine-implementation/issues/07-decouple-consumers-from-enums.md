# Decouple consumers from the lifecycle enums

Type: task
Status: claimed
Blocked by: 01, 02, 03

## Question

Migrate the consumer subsystems off the four lifecycle enums **while Spring Statemachine is still running**, one subsystem per commit. This is standing decision 2 — make the change easy, then make the easy change — and it is where the bulk of stage 1's file churn happens, deliberately done against a working system.

**Slab gate:** the full suite and ticket 01's characterization suite both green, and `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent` and `NegotiationResourceEvent` referenced **only** inside `negotiation/state_machine/` and the three metadata DTOs (see carve-out below). No behaviour change of any kind — this slab moves types, not logic.

### Apply ticket 03's decision mechanically

Ticket 03 decides what names a State or Event. This slab applies it. Do not re-open that decision; if it turns out to be unworkable for a specific consumer, that is a finding to record against ticket 03, not a local improvisation.

### Suggested commit order — cheapest and most isolated first

1. **Webhook payloads** — `webhook/event/NegotiationStateUpdatedWebhookEvent`, `NegotiationResourceStateUpdatedWebhookEvent`, `NegotiationAddedWebhookEvent`, `NegotiationStateChangeWebhookMappingStrategy`, `NewNegotiationWebhookMappingStrategy`. Enums already serialize as JSON strings, so the wire format is unchanged — assert that in a test rather than assuming it. Update the `@Schema(example = …)` metadata.
2. **Network statistics** — `governance/network/stats/NetworkStatistics`, `SimpleNetworkStatistics`, `NetworkStatisticsServiceImpl`.
3. **DTOs and mappers** — `negotiation/dto/NegotiationDTO`, `mappers/NegotiationModelMapper`, `NegotiationStatusConverter`, `NegotiationEventAssembler`, `NegotiationModelAssembler`, `governance/resource/dto/ResourceWithStatusDTO`, `ResourceViewDTO`, `negotiation/dto/UpdateResourcesDTO`, `negotiation/NegotiationTimelineImpl`.
4. **Resource governance** — `governance/resource/ResourceServiceImpl`, `NonRepresentedResourcesHandlerImpl`.
5. **JPA filters and specs — the hardest, do it last.** `negotiation/NegotiationSpecification` (`hasState` at `:80-95`, and the hardcoded `NegotiationState.DRAFT` visibility rule at `:33` and `:49`), `negotiation/dto/NegotiationFilterDTO:33`, `negotiation/NegotiationRepository`. The `DRAFT` rule's disposition is settled by ticket 03 — apply it.
6. **The 26 test files** — churn alongside whichever commit touches their subject, not in one lump at the end.

### Two carve-outs

- **The three metadata DTOs are excluded** — `NegotiationStateMetadataDto`, `ResourceStateMetadataDto`, `ResourceEventMetadataDto`. They belong to ticket 04, which asks a different question (whether an endpoint enumerating a *universe* of States still makes sense). They are carved out precisely so this slab is not blocked on it, which is why the gate above names them.
- **Anything ticket 02 relocates into an Action is excluded.** Those consumers cannot move early — they need the Action registry, and therefore the schema and evaluator, to exist first. They belong to the cutover slab. Ticket 02's resolution defines this boundary exactly; read it before starting and record the resulting file list here.

### Note

Merge master into the branch before starting and again before finishing. This slab touches ~50 files across six subsystems and is the likeliest place to collide with concurrent work.

## Carve-out 2, recorded — the file list ticket 02's resolution implies

Derived from [ticket 02](02-state-triggered-behaviour-location.md)'s decisions 2 and 3, verified
against the source tree on 2026-08-26.

**Ticket 02 relocates exactly one thing into an Action: the spawn loop.** Decision 2 is explicit
that *no notification becomes an Action* and that "the `NotificationStrategy` mechanism survives
whole"; decision 6 keeps both webhook mapping strategies as listeners. Decision 3 relocates
`ResourceNotificationService.handleResourceStateManagement` into ADR 0007's
`SPAWN_RESOURCE_LIFECYCLES`, splitting the notification off it via a new
`ResourceLifecyclesSpawnedEvent`.

So carve-out 2 is **two files, and only the spawn behaviour inside them**:

| File | What moves to the coupling slab | What slab 07 still does here |
|---|---|---|
| `notification/internal/ResourceNotificationService.java` | `handleResourceStateManagement` (`:45-65`) — the whole per-Resource assign-and-accumulate loop becomes the Action body; `notifyRepresentatives` (`:73-87`) stays behind and is re-triggered by `ResourceLifecyclesSpawnedEvent` | swap the three enum references at `:47`, `:57`, `:60` to holder constants, **in place** |
| `notification/internal/NegotiationInProgressHandler.java` | the whole class — its only job is the `IN_PROGRESS` trigger for spawn, which ADR 0007 rewires to the approval Transition | swap the one enum reference at `:31` to `WellKnownNegotiationStates.IN_PROGRESS`, **in place** |

**Carve-out 2 excludes a relocation, not a type swap — and it has to.** The two readings collide and
the gate settles it: this ticket's gate exempts *only* the three metadata DTOs, so any file still
naming an enum after this slab fails it. Ticket 03's holder table independently assigns
`ResourceNotificationService:47,57,60` and `NegotiationInProgressHandler:31` to
`WellKnownNegotiationStates` / `WellKnownResourceStates`, i.e. to this slab. Carve-out 2's stated
rationale — "they need the Action registry, and therefore the schema and evaluator, to exist first"
— does not bite on a type swap: rewriting `NegotiationResourceState.REPRESENTATIVE_CONTACTED` as
`WellKnownResourceStates.REPRESENTATIVE_CONTACTED` needs nothing that does not already exist.

**What slab 07 must therefore not do in these two files:** no restructuring, no new event, no
transaction change, no moving the loop. The coupling slab moves a method whose body already names
Strings.

**Nothing else is carved out.** In particular these six files are ticket 02 sites that stay
listeners and are **in** slab 07: `NegotiationStatusChangeHandler`, `NegotiationSubmissionHandler`,
`NewNegotiationHandler`, `ResourceStateChangeHandler`, `NegotiationStateChangeWebhookMappingStrategy`,
`NewNegotiationWebhookMappingStrategy`. `PendingNegotiationReminderHandler` and
`NonRepresentedResourcesHandlerImpl` are in as well — ticket 02's decision 1 moved them *into* this
population rather than out of it.

## Blast radius, measured

Counted 2026-08-26 on `feat/state-machine-implementation` at `3de318c3`, by grep over
`backend/src/{main,test}/java`. **The map's "~60 main-source files" is the total including the
`state_machine` package itself; the consumer population is 42.**

| Population | Files |
|---|---|
| Main sources referencing any of the four enums | **65** |
| — inside `negotiation/state_machine/` (27 classes in the package) | 23 |
| — **outside it: the consumers this slab moves** | **42** |
| Test sources referencing any of the four enums | **30** (map said 26) |

Consumers by subsystem, and per enum:

| Subsystem | Files | | Enum | Files | Refs |
|---|---|---|---|---|---|
| `negotiation/**` | 17 | | `NegotiationState` | 25 | 76 |
| `governance/**` | 10 | | `NegotiationResourceState` | 13 | 46 |
| `notification/internal/**` | 6 | | `NegotiationResourceEvent` | 9 | 24 |
| `webhook/event/**` | 5 | | `NegotiationEvent` | 4 | 8 |
| `info_requirement/**` | 4 | | | | |

### Five things the measurement contradicts or adds

1. **`info_requirement` is a fifth consumer subsystem, named in no commit-order line.** Four files —
   `InformationRequirement` (the persisted `forEvent` field), `InformationRequirementRepository`
   (`existsByForEvent`), `InformationRequirementDTO`, `InformationRequirementCreateDTO`. The map's
   standing decision 6 knows about `InformationRequirementCreateDTO.forResourceEvent` but files it
   under the IR slab; the gate pulls it here. **No migration is needed:** `for_event` is already
   `VARCHAR(255)` (`V11.0__add_info_requirement.sql:5`).
2. **The invisible SQL literals are 14 across two files, not "8 in `NetworkStatsRepositoryImpl`".**
   Ticket 03 missed `NetworkStatsRepositoryImpl:75` (`CHECKING_AVAILABILITY`, `RESOURCE_UNAVAILABLE`)
   and the whole of `NegotiationRepository`. Actual: `NetworkStatsRepositoryImpl` `:28`(2) `:51`(2)
   `:75`(2) `:97` `:119` `:163` `:216`(2) = **11**; `NegotiationRepository` `:35`(2) = **2** silent,
   plus `:74`'s unquoted JPQL `!= DRAFT` = **1** that fails loudly at Hibernate query validation
   rather than silently. The sweep must be a **bare-name** grep: these live in Java text blocks, so
   a grep for names inside `"…"` finds none of them.
3. **Three lifecycle types inside `state_machine/` are the real seam, and the gate does not reach
   them.** `NegotiationStateChangeEvent` / `ResourceStateChangeEvent` expose
   `getFromState()`/`getToState()`/`getEvent()` as enums, and `NegotiationLifecycleService` /
   `ResourceLifecycleService` return and accept enums. A consumer can call
   `event.getToState().name()` with **no import and no textual reference**, so a
   `DefinitionInertnessGuardTest`-style identifier grep would go green over a codebase still
   type-coupled to all four enums. This is slab 08's lesson in mirror image, and it means the seam
   conversion is a slice, not a side effect.
4. **`ResourceStateChangeHandler` breaks and is on no list.** `:47-48` calls
   `event.getFromState().getLabel()`. Ticket 02's decision 7 says it "needs no change here" and
   routes labels to ticket 04 — but the type swap removes `getLabel()`, so this slab must source the
   label from somewhere. It appears in neither ticket 07's commit order nor ticket 03's holder table.
5. **The PDF export was the obvious silent-breakage candidate and is clear.**
   `NegotiationPdfServiceImpl:172` and `PdfContextBuilder:55` put the raw enum into a Thymeleaf
   variable rendered by `th:text="${negotiationStatus}"`
   (`PDF_NEGOTIATION_SUMMARY.html:283,330`). `NegotiationState` does not override `toString()`, so
   Thymeleaf already renders `name()` and a `String` renders identically. Checked, not assumed —
   and it should be pinned by a test in whichever slice touches it, because neither file names an
   enum type and both compile clean either way.

Also confirmed as measured facts: `NegotiationStatusConverter` is registered nowhere in `WebConfig`
(only `NegotiationEventConverter`, `NegotiationRoleConverter`, `NegotiationResourceEventConverter`
are), so ticket 03's "dead code, delete it" holds; and the three metadata DTOs of carve-out 1 each
reference exactly one enum, as a single `value` field.

## Slab

Planned as [PRD — Decouple consumers from the Lifecycle enums](../../decouple-consumers-from-enums/PRD.md),
2026-08-26. Ten slices; slice order and blockers are in the PRD.

**One departure from ticket 03 is filed there rather than taken quietly.** Decision 3 requires
`?status=` to be validated against the State rows of the Negotiation-scope active Definition
Version. That is not buildable in this slab — the tables slab 08 created are empty until the
migration slab seeds them, so the check would refuse every value and take parity red, and it would
make this the first slab to read them and therefore oblige it to delete `DefinitionInertnessGuardTest`
a stage early. A disposable **Enum-Backed Lifecycle Catalog** inside `negotiation/state_machine/`
answers the existence check, the labels and the ordinal instead, and is deleted with the library.
The cutover slab replaces its three methods with reads of the `state` and `event` rows; that is the
trigger.
