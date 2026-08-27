# Slab status — decouple consumers from the Lifecycle enums

Working reference for the twelve slices of
[PRD — Decouple consumers from the Lifecycle enums](PRD.md), against map ticket
[07](../state-machine-implementation/issues/07-decouple-consumers-from-enums.md). Branch:
`feat/state-machine-implementation`.

Each slice adds a section for what it settled on behalf of the ones after it. Decisions recorded
here are settled; do not relitigate them in a later slice.

## Landed

| Slice | State | Evidence |
|---|---|---|
| [01 Well-known name holders](issues/01-well-known-name-holders.md) | **done** | 8 tests green; full suite 1435/0/0/16; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [02 The Enum-Backed Lifecycle Catalog](issues/02-enum-backed-lifecycle-catalog.md) | **done** | catalog test 13/0/0/0; whole suite not measured at this tip - reconstructed as 1444, see this slice's section; parity and deltas green over it rather than at it |
| [07 Information Requirements name their Event as a string](issues/07-information-requirements-name-their-event-as-a-string.md) | **done** | +3 tests; controller 32/0/0/0, service 4/0/0/0, model 2/0/0/0; full suite 1453/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - measured by slice 03 on a tree rebased onto this slice |

Parity and delta numbers are summed from `backend/target/surefire-reports`, filtered by mtime.
**That filtering is not optional here**, and this run showed why: `surefire-reports` is not cleared
between invocations, so after a full-suite run it holds a report for all 25 characterization
classes. The parity run rewrites 24 of them and correctly leaves
`delta.IntendedDeltasAdr0005WillInvertTest` untouched, because the tag excluded it - so a naive sum
over the directory reads 263 and looks like the parity count has moved. Take the mtime window of
the run you just did, then check that the classes outside it are the ones you expect to be stale.

## What slice 01 fixed for slices 04, 05, 08, 09 and 11

**Package: `eu.bbmri_eric.negotiator.lifecycle`.** The three holders are
`WellKnownNegotiationStates`, `WellKnownResourceStates` and `WellKnownResourceEvents`, all public
and all directly under `lifecycle`. Import from there.

The choice was open — the issue left it deliberately undecided — and four constraints closed it:

- **Not `negotiation/state_machine/`.** That package is deleted at cutover and these nine names
  outlive it. This was given.
- **Not `lifecycle/definition/`, and this is the load-bearing one.**
  `DefinitionInertnessGuardTest`'s package rule fails any production source outside that package
  naming `eu.bbmri_eric.negotiator.lifecycle.definition`. Holders placed one level deeper would
  therefore have taken the *whole slab* red the moment slice 04 imported one, and the only way out
  would have been deleting the inertness guard — a stage early, which the map forbids and PRD user
  story 4 depends on. `lifecycle` is the parent, so the guard's `Pattern.quote` on the fully
  qualified definition package does not match it. Verified: the full suite is green with the
  holders in place.
- **Not `common/`.** These are Lifecycle vocabulary, and `common` is where a name goes to stop
  meaning anything.
- **`lifecycle` reads naturally from all five consumer subsystems** — webhook, notification,
  network statistics, information requirements and resource governance — none of which owns the
  vocabulary, and all of which are peers of it.

The useful accident is that `lifecycle` is also where ADR 0002's model already lives. After the
cutover, `lifecycle.definition` holds what a State *is* and `lifecycle` holds the handful of names
behaviour reaches for by hand. Nothing about that arrangement has to change when Spring Statemachine
goes.

**Naming: `WellKnown<Scope><Kind>s`, plural, and deliberately unlike the enums.** A stale
`import ...NegotiationState;` cannot silently resolve against `WellKnownNegotiationStates`, and
`NegotiationState.DRAFT` → `WellKnownNegotiationStates.DRAFT` is a visible edit at every call site
rather than a change of meaning under a name that stayed the same.

**The growth rule is a test, not a convention.** `WellKnownNamesTest` asserts *set equality* on each
holder's constants, so adding a tenth name requires deleting a line of the test in the same diff.
This is the one thing the PRD, ticket 03 and the slice issue all independently warn about: a holder
that lists every State is the enum again with worse ergonomics. If a later slice finds a call site
whose name is not in the nine, the answer is almost always that the name is **data** — off the
column, the request or the Definition Version Pin — not that the holder is short one.

**The names are pinned against the enums while the enums still exist.** The same test resolves every
constant through `NegotiationState.valueOf` / `NegotiationResourceState.valueOf` /
`NegotiationResourceEvent.valueOf`. This is worth knowing about because it is the only cheap defence
against the failure mode of slices 04–11: a typo in a holder constant does not fail to compile, it
compiles into a comparison that silently stops matching, which is precisely the behaviour change the
slab promises not to make. At cutover the enums go and these three assertions go with them — as a
compile error, which is the loud case.

**Test placement.** The holders are public, so their test can live anywhere; it lives in
`eu.bbmri_eric.negotiator.lifecycle` beside them rather than under `unit/`. Enum references in test
scope are permitted by the slab gate, which is what makes the pinning above legal.

**Filter synthetic members before asserting on reflection.** The "no behaviour" rule first failed
against `getDeclaredMethods().length == 0` because JaCoCo instruments every loaded class with a
synthetic `$jacocoInit`; the compiled class on disk has only the constructor. Any later guard that
counts members must exclude `isSynthetic()`, or it passes under a plain compile and fails under the
suite.

**Nothing reads the holders yet, and that was checked rather than assumed.** After this slice the
only production occurrences of the three names are the declarations themselves plus two Javadoc
`{@link}` cross-references between holders. Slice 04 is the first real consumer.

**Whole-suite count moved 1415 → 1435, and only +7 of that is this slice.** The two test files this
slice touches are the new `WellKnownNamesTest` (+8) and `ConverterTest` (−1, the status-converter
method); `git status` confirms no other test file changed, and the deleted converter was production
code that no test but that one named. The 1415 recorded against the previous slab's slice 07 is
therefore stale relative to this branch rather than a figure this slice contradicts. Later slices
should take **1435/0/0/16** as the baseline and not try to reconcile against the older number.

**Terminology the glossary actually binds.** `CONTEXT-MAP.md` makes `backend/CONTEXT.md`'s `_Avoid_`
lines binding, and Spawn's entry is the one this slab will brush against constantly: *"Nothing is
created — the Resources are already linked — so Spawn names the initialization, not an
instantiation."* Review caught "one Resource Lifecycle is **created** per requested Resource" in a
holder's Javadoc. Slices 05 and 09 write the most prose about Spawn; say *started*, *initialized* or
*Spawn writes*, never *created*, *instantiated*, *fanned out* or *launched*.

**A finished slice is `Status: resolved`.** Not `done` — that is in neither `triage-labels.md` nor
`issue-tracker.md`, and `resolved` is what six of the previous slab's seven finished slices used.

## What slice 01 removed

`NegotiationStatusConverter` is **deleted**, not migrated. `WebConfig.addFormatters` registers
`NegotiationEventConverter`, `NegotiationRoleConverter` and `NegotiationResourceEventConverter` and
never this one — re-verified in the source, so the 400 it appears to produce actually comes from
Spring's default enum binding and survives the deletion untouched.

**Ticket 03 says its test class goes with it; that overstates it and the correction stands.**
`unit/converters/ConverterTest` also covers the unrelated `NegotiationRoleConverter`. The class
survives with `testConvert`; only `testStatusConverter` was removed. Post-deletion sweep for
`NegotiationStatusConverter` across `backend/src` and `frontend/` comes back empty.

Preserving today's `?status=UNKNOWN` 400 is **slice 02's** job, via the Enum-Backed Lifecycle
Catalog. This slice removed dead code adjacent to that question and did not touch the behaviour.

## What slice 02 settled, for slices 05, 10 and 11

*Recorded retroactively on 2026-08-27. This slice landed at `909d9055` with no Landed row, no
section and its issue still reading `ready-for-agent`; slices 03, 04, 08 and 09 went in on top of
it before the omission was noticed. The commit sits at its place in the history so the slab reads
in order, but the evidence below was gathered afterwards and points forward to later slices where
that is the only honest source for a number.*

**The catalog is `public` and a Spring `@Component`, not package-private.** It lands under the
second half of its own AC - "as narrow as the callers allow" - because the three questions are asked
from `notification/internal` (slice 05), `negotiation/mappers` (slice 10) and `negotiation/dto`
(slice 11), none of which is inside `negotiation.state_machine`. Package-private was never
available. `Scope` and `Element` are nested public enums for the same reason. What stays narrow is
the *surface*: three methods, no state, and nothing that decides whether a Transition may fire.

**All four enums are reached through one pair of coordinates rather than four methods.**
`nameExists(Scope, Element, String)`, `metadata(Scope, Element, String)` and
`resourceStateOrdinal(String)`. A caller asking a metadata question therefore names `NEGOTIATION` or
`RESOURCE` and `STATE` or `EVENT`, and never a Lifecycle enum - which is exactly what lets the
decoupling guard go green over a consumer that still needs the closed set. Slices 05, 10 and 11
should ask through the coordinates rather than adding a per-enum method.

**`Metadata` carries label and description together, as one record.** Both always come off the same
row, before and after the cutover, so a caller needing both makes one call. Slice 05's notification
body names two Resource States: two calls, not four.

**The unknown-name contract differs per method, deliberately.** `nameExists` returns `false`;
`metadata` and `resourceStateOrdinal` throw `IllegalArgumentException`. The split is the point.
Existence is a question a caller asks *in order to* produce a 400, while a metadata lookup for a
name that already passed validation is a bug and should say so. A later slice that turns those
throws into a 400 is deciding something new - decide it on purpose rather than by catch block.

**Only Resource State has an ordinal, and it is pinned to an absolute number.**
`resourceStateOrdinal("RESOURCE_MADE_AVAILABLE")` is asserted to be `11`, so inserting a Resource
State anywhere before it fails this test. That is faithful to today, because the frontend sorts by
that number. There is deliberately no Negotiation-side ordinal: adding one would *extend* the
closed-set ordering contract ticket 04 is questioning rather than merely preserve it.

**Every answer is derived, never restated.** The maps are built from `values()` with `getLabel` and
`getDescription` method references, so no name, label or description here can drift from the enum
while the enum exists. At cutover the four imports stop compiling - loudly, at the one moment the
seed becomes the source of truth. The javadoc names the replacement for each method: the presence of
the named `state` or `event` row, that row's label and description, and the Resource State row's
ordering value.

**Nothing reads it, and that is still true several slices later.** After this slice
`EnumBackedLifecycleCatalog` occurs in `main` only as its own declaration. Slice 08 needed the same
existence check inside a Jackson deserializer, which is instantiated reflectively and cannot hold a
Spring bean, so it wrote `NegotiationResourceStateNameDeserializer` instead - as slice 07 had
already done for `forResourceEvent`. Those are not a second source of truth and not a reason to drop
this class: **the catalog stays the route for every caller that can inject it**, and all three read
the same enums and die in the same cutover.

**The Definition Version tables are not read, which is what keeps the filed departure honest.**
`DefinitionInertnessGuardTest` is untouched and green, so PRD user story 4 still has its gate. The
trigger for undoing the departure is recorded in the slice issue and the PRD: the cutover slab
replaces these three methods with reads of the `state` and `event` rows.

**The whole-suite figure was never measured at this tip, and is reconstructed rather than
restated.** `EnumBackedLifecycleCatalogTest` is 13 tests and is green. The slice touched exactly two
files, one of them that new test class, so from slice 01's 1435 the arithmetic predicts 1448. Slice
03 measured 1453 after rebasing onto slice 07 and attributes six tests to itself and three to slice
07, which puts the branch at **1444** here - four below the prediction. The gap cannot be this
slice's: it added one test file and touched no other. It is the same four tests slice 03's section
reports from the other side, and it remains unattributed.

**Parity and deltas were not measured at this tip either, and are recorded as green over it rather
than at it.** Slices 03, 04, 08 and 09 each measured 255 tests in 24 classes, 0 failures, 1 skipped,
and deltas 8/0/0/0, on trees that contain this class. Backfilling those numbers here as if they had
been taken at this commit would be the one thing the Landed table exists to prevent.

## What slice 03 measured, for slices 06 and 11 and for two later slabs

The guard is `eu.bbmri_eric.negotiator.lifecycle.RawStateNamesInSqlGuardTest`, beside
`WellKnownNamesTest`. It scans `backend/src/main/java` and pins every State or Event name that
reaches the database as query text, in four coordinates: file, line, name, and whether the query
quotes the name or leaves it bare. Five tests, no production file touched.

**The population is fourteen names, and the slice issue's own arithmetic was off by one.** Thirteen
are SQL string constants; the fourteenth is the bare JPQL reference at `NegotiationRepository:74`.
The issue's "all fifteen" counts that fourteenth twice. Its "eleven of the fourteen live in Java
text blocks" is **thirteen** — `NegotiationRepository:35` is a text block too, which the sweep read
as a concatenation. The AC it was drawing is untouched by either correction: no name anywhere in
the fourteen is spelled as a Java double-quoted literal, so a `"DRAFT"` regex still finds exactly
zero of them while reporting green.

**Ticket 03's count is corrected a second time.** It recorded 8 literals in
`NetworkStatsRepositoryImpl` at lines 28, 51, 97, 119, 163 and 216. The file holds **eleven**, at
those six lines plus line 75, which it missed entirely. Two more sit in `NegotiationRepository:35`,
which it did not know about at all.

**The audit-column population is six, not four.** `negotiation_resource_lifecycle_record.changed_to`
— the column ADR 0008 converts to a foreign key — is filtered by
`NetworkStatsRepositoryImpl:75` (×2), `NetworkStatsRepositoryImpl:216` (×2) and
`NegotiationRepository:35` (×2). Ticket 03 named only line 216.

**`getMedianResponseForNetwork` exists twice, with identical SQL.** Once on
`NetworkStatsRepositoryImpl:64` and once on `NegotiationRepository:39`.
`NetworkStatisticsServiceImpl` calls the **`NegotiationRepository`** one, so the network-statistics
copy is unreachable from production. That is why the same comparison has to be found in two places
by both later slabs.

**Six of the eleven network-statistics literals are in queries no production path reaches.**
`countIgnoredForNetwork` (line 28), `getMedianResponseForNetwork` (line 75) and
`getNumberOfSuccessfulNegotiationsForNetwork` (line 97) are called only from
`integration/repository/NegotiationRepositoryTest`. Slice 06 should not read "network statistics"
as "everything in `NetworkStatsRepositoryImpl` is live" — the service reaches five of its nine
methods, and takes the count and the median off `NegotiationRepository` instead.

**What counts as query text.** A run of adjacent Java string literals joined only by `+` is read as
one string, and that string is a query when its content matches `SELECT … FROM`, `INSERT INTO`,
`UPDATE … SET` or `DELETE FROM`. The concatenation step is load-bearing: the chunk holding
`!= DRAFT` on `NegotiationRepository:74` carries no `SELECT` of its own. The rule is what separates
the fourteen from the twenty-three other places a State name legitimately appears inside a Java
string in production — OpenAPI `@Schema(example = …)`, `@Operation` prose, two exception messages,
and the three holders' own declarations. Slices 04 and 09 will move some of those; the guard is
meant to ignore them, and a test asserts it does.

**The pin is line-exact, and that is the point.** An inserted import above these queries fails the
guard. Slices 06 and 11 will trip it; the failure pairs each old line with its new one and says to
update the number and nothing else. Loosening the rule is the one repair that must not be made —
the compiler cannot see any of these names, so this list is the only place they are written down.

**Two things the guard deliberately does not cover.** Flyway migrations under `src/main/resources`
name plenty of States in check constraints, but a landed migration is immutable by checksum: it
cannot move, and pinning it would say nothing a reader could act on. And the vocabulary the scan
looks for is taken from the four enums through `values()` rather than typed out, so it cannot drift
while they exist — at cutover that import stops compiling, loudly, at the moment the seed becomes
the source of truth and this guard's reading list has to be decided again.

## What slice 07 settled, for slices 08, 10 and 11

*Recorded retroactively on 2026-08-27. This slice landed at `31c6ed41` with no Landed row, no
section and its issue still reading `ready-for-agent`; slices 03, 04, 08 and 09 went in on top of it
before the omission was noticed. The commit sits at its place in the history so the slab reads in
order, but the evidence below was gathered afterwards - including one fact established by experiment
during the recording itself, which is flagged where it appears.*

**The batch was four files and turned out to be six.** The entity, the repository and both DTOs, as
the issue predicted - plus `InformationRequirementServiceImpl.mapToDTO` and a new
`NegotiationResourceEventNameDeserializer` in `negotiation/state_machine/resource/`. Both additions
are behaviour *preservation*, and the first of them is the sharpest thing this slice hands the
slices that come after.

**A ModelMapper implicit match can disappear when the only thing that changed is the property type.
Measured, not inferred.** `InformationRequirement.forEvent` and
`InformationRequirementDTO.forResourceEvent` have never shared a name and there is no `TypeMap` for
the pair, yet ModelMapper matched them implicitly while both were `NegotiationResourceEvent`. With
both a `String` it does not. Established by experiment during this recording: removing the explicit
`dto.setForResourceEvent(informationRequirement.getForEvent())` and running the class makes
`$.forResourceEvent` come back **null** and fails four tests -
`findRequirementById_existingId_ok`, `updateRequirement_correctBody_ok`,
`createInformationRequirement_correctBody_ok` and `createInformationRequirement_onlyForAdmin_ok`.
Without that one line this slice would have silently dropped a field from the admin API's response
body, in a slab whose entire claim is that nothing observable changes. The mechanism inside
ModelMapper is *not* established here - only the effect, which is what matters.
**Slices 10 and 11 must assume nothing about an implicit match surviving a type change.** Slice 08
met the adjacent trap from the other side - ModelMapper does not coerce a `Converter`'s result to the
destination type - and `NegotiationModelAssembler`, `NegotiationEventAssembler` and
`ResourceWithStatusAssembler` are all still ahead. Assert the wire field, never the mapping.

**`equals` moved from `==` to `Objects.equals`, and that is not cosmetic.** Reference comparison is
correct for enum constants and wrong for names: a `==` left in place would have compiled and then
compared interned literals against strings loaded from the database. This is precisely the failure
mode slice 01 warned about - a comparison that silently stops matching rather than failing to
compile - in its cheapest possible form. Any later slice converting an entity field must inspect
that entity's own `equals` and `hashCode` in the same diff.

**The 400 on an unknown Event is preserved by a Jackson deserializer, not by the catalog.**
`@JsonDeserialize(using = NegotiationResourceEventNameDeserializer.class)` on
`InformationRequirementCreateDTO.forResourceEvent` reads the name, calls
`NegotiationResourceEvent.valueOf` purely as the check, and returns the string. `@JsonDeserialize`
instantiates its deserializer reflectively, so it cannot hold the Spring-managed
`EnumBackedLifecycleCatalog` - this is one of the two places in the slab that genuinely cannot use
it. Pinned by a new controller test asserting 400 **and** `$.title` of `"Wrong request"`, so the
body is pinned rather than just the status code. Slice 08 landed the same shape for
`UpdateResourcesDTO.state`; the two deserializers are siblings and are deleted together at cutover.

**One accepted micro-delta, shared with slice 08.** `FAIL_ON_NUMBERS_FOR_ENUMS` is unset in both
`application.yaml` and `application-prod.yaml`, so Jackson's default enum binding accepted a JSON
integer as an ordinal and `{"forResourceEvent": 3}` used to bind to the fourth Resource Event.
Through the deserializer `getValueAsString()` yields `"3"` and it is refused with a 400. Preserving
it would have meant writing `values()[i]` by hand - deliberately new ordinal-as-identity coupling,
inside a class built to be deleted, to keep an accident nothing documents and no client uses.
Recorded rather than preserved. Slice 08 made the identical call for `UpdateResourcesDTO.state`;
**if either is ever undone, undo both.**

**The schema reads as a string with a worked example, and it is pinned against the served document.**
`@Schema(description = ..., example = "CONTACT")` on the field in both DTOs, asserted through
`GET /v3/api-docs` - `forResourceEvent.type` is `"string"` and `.example` is `"CONTACT"` - rather
than by reading the annotation back. That is the shape to copy anywhere this slab claims an OpenAPI
outcome: an annotation assertion proves nothing about what a client actually fetches.

**The seam translation this slice wrote, for slice 10 to delete.** `ResourceLifecycleServiceImpl`
now calls `existsByForEvent(negotiationResourceEvent.name())`. It sits inside
`negotiation/state_machine/`, so it is not a gate violation, and it disappears when slice 10 makes
`sendEvent` take an Event name.

**No migration, no data change, and no re-homing.** The column was already `VARCHAR(255)` and now
carries `@Column(length = 255)` over the same values. The map's standing decision 6 still owns
turning the creation DTO's field into an Event reference; this slice only made it a name.
`isViewableOnlyByAdmin` was touched by nothing and stays unowned.

**Tests are +3, and the full-suite figure is slice 03's measurement rather than one taken here.** The
two new controller tests - the unknown-Event 400 and the served-schema shape - plus one new entity
test; every other change across the six touched test files is a constructor argument moving from
`NegotiationResourceEvent.CONTACT` to `"CONTACT"`. Focused runs are green:
`InformationRequirementControllerTest` 32/0/0/0, `InformationRequirementServiceTest` 4/0/0/0 and
`unit/model/InformationRequirementTest` 2/0/0/0. Slice 03 then measured the full suite at
**1453/0/0/16 in 158 classes**, parity at 255 in 24 classes with 0 failures and 1 skipped, and
deltas at 8/0/0/0, explicitly *after rebasing onto this slice* - which makes that run this slice's
full-suite evidence, and the tightest available, since the only content on that tree beyond this
slice is slice 03 itself.

## Standing hazards, carried not solved

**`WellKnownResourceStates` is a bet on a family's vocabulary; the Negotiation holder is not.** ADR
0004 keeps a single Negotiation-scope Definition Family, so those five names are as stable as the
enum constants were. Resource scope is exactly the scope that diverges once custom families ship, and
a custom family may reasonably have no `REPRESENTATIVE_UNREACHABLE`. Nothing in stage 1 can make the
bet lose — one seeded Resource family, and the seed is a faithful transcription — so this is recorded
in the holder's Javadoc for whoever ships the second one. Ticket 03 filed it as noted-not-solved;
this slab does not solve it.

**`WellKnownResourceStates.SUBMITTED` is a default as well as a comparison.** `UpdateResourcesDTO`
uses it as a *default value* rather than a test, which is the same divergence hazard in an API DTO.
Flagged in the holder's Javadoc; slice 09 will meet it.

## Operational

**Do not run two Maven invocations against `backend/` at once.** A concurrent recompile clears
`target/test-classes` under a running suite and every Spring-context class then errors with
`FileNotFoundException`. It presents as ~150 unrelated failures and is an artifact. A sub-agent that
verifies by running tests counts as a second invocation.

Slices 01, 02, 03 and 07 have no dependency on each other and are being authored in parallel in
separate worktrees. Separate worktrees have separate `target/` directories, so that much is safe —
but serialize anything that touches a shared resource.

Run the formatter before committing any Java; it is not bound to the `test` phase:

```
nix develop .#opencode --command mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format
```
