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
| [03 Pin the raw State names in SQL](issues/03-pin-the-raw-state-names-in-sql.md) | **done** | 6 tests green; full suite 1453/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [04 Webhook payloads name States as strings](issues/04-webhook-payloads-name-states-as-strings.md) | **done** | mapper test 10 → 14, listener test 9 unchanged in count; full suite 1457/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [05 Notification handlers name States as strings](issues/05-notification-handlers-name-states-as-strings.md) | **done** | +8 tests: new `ResourceStateChangeHandlerTest` 4, status-change handler 8 -> 12; full suite 1472/0/0/16 in 159 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [06 Network statistics name States as strings](issues/06-network-statistics-name-states-as-strings.md) | **done** | +1 test: `NetworkControllerTests` 26 -> 27; full suite 1473/0/0/16 in 159 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [07 Information Requirements name their Event as a string](issues/07-information-requirements-name-their-event-as-a-string.md) | **done** | +3 tests; controller 32/0/0/0, service 4/0/0/0, model 2/0/0/0; full suite 1453/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - measured by slice 03 on a tree rebased onto this slice |
| [08 DTOs, mappers and the Negotiation timeline](issues/08-dtos-mappers-and-the-negotiation-timeline.md) | **done** | mapper test 7 -> 10, timeline test 4 -> 5, controller test +1; full suite 1462/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - measured at base `a3e02e59`, before 09 landed; see the rebase note in 08's section |
| [09 Resource governance names States as strings](issues/09-resource-governance-names-states-as-strings.md) | **done** | focused resource/event tests green; full suite 1463/0/0/16 in 158 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 |
| [10 The Lifecycle seam deals in strings](issues/10-the-lifecycle-seam-deals-in-strings.md) | **done** | +1 test; full suite 1480/0/0/16 in 159 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - landed after 11 and rebased onto it, so every figure here is measured at the rebased tip |
| [11 Entities and JPA queries name States as strings](issues/11-entities-and-jpa-queries-name-states-as-strings.md) | **done** | +6 tests: PDF generation 8 -> 10, `NegotiationControllerTests` 115 -> 118, raw-SQL guard 6 -> 7; full suite 1479/0/0/16 in 159 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - parity and deltas measured twice, at the refactor tip and again at the review tip |
| [12 The decoupling gate](issues/12-the-decoupling-gate.md) | **done** | +8 tests, one new class `LifecycleEnumDecouplingGuardTest`, no production file touched; full suite 1492/0/0/16 in 160 classes; parity 255 in 24 classes, 0 failures, 1 skipped; deltas 8/0/0/0 - all three partitioned out of one `--all` run. **This row's whole-suite figure is an XML sum; every row above it is a `.txt` sum and is four low** - see slice 12's section |

Parity and delta numbers are summed from `backend/target/surefire-reports`, filtered by mtime.
**That filtering is not optional here**, and this run showed why: `surefire-reports` is not cleared
between invocations, so after a full-suite run it holds a report for all 25 characterization
classes. The parity run rewrites 24 of them and correctly leaves
`delta.IntendedDeltasAdr0005WillInvertTest` untouched, because the tag excluded it - so a naive sum
over the directory reads 263 and looks like the parity count has moved. Take the mtime window of
the run you just did, then check that the classes outside it are the ones you expect to be stale.

**Sum the `TEST-*.xml` reports, not the `.txt` ones.** Slice 12 found that surefire's plain-text
writer reports `Tests run: 0` for a class with an `@Nested` inner class while the XML records its
real count, and that this is the whole of the four-test gap three earlier slices reported as
unattributed. The mtime rule above is still necessary; it was never sufficient.

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

**The whole-suite count is 1453/0/0/16 in 158 classes, and it does not reconcile with 1435.**
Measured after rebasing onto slice 07, which is what actually lands. This slice adds one test file
and six tests — `git show --stat` on its commits shows nothing else — and slice 07 accounts for
three, so the branch stood at 1444 at slice 02's tip. Slice 01 recorded 1435 and slice 02 added a
13-test class and touched no other test file, which predicts 1448. The four-test gap therefore
predates this slice and is not attributable from here without re-running an earlier commit.
**Take 1453 as the baseline** and do not try to reconcile against 1435; this is the second time the
recorded figure has gone stale relative to the branch, for the same reason slice 01 gave — the
number is measured in a worktree, at a moment, and the branch moves under it.

**`nix develop` stopped working here after `bacf4391 dev: bump toolchain`**: it fails with
`permission denied` creating a directory under `/nix/var/nix/builds`, so the commands written into
`characterization/package-info.java` and below do not run as documented. `mvn` and Java 21 are on
`PATH` regardless, so dropping the `nix develop .#opencode --command` prefix and invoking
`test-backend.sh` directly works. Whether that is this sandbox or the bump itself is not
established.

**Two things the guard deliberately does not cover.** Flyway migrations under `src/main/resources`
name plenty of States in check constraints, but a landed migration is immutable by checksum: it
cannot move, and pinning it would say nothing a reader could act on. And the vocabulary the scan
looks for is taken from the four enums through `values()` rather than typed out, so it cannot drift
while they exist — at cutover that import stops compiling, loudly, at the moment the seed becomes
the source of truth and this guard's reading list has to be decided again.

## What slice 04 established for slices 05, 08, 09, 10 and 11

**The wire-format claim is discharged, once, for the whole slab — and at two heights that prove
different things.** Eight assertions in total: four in the mapper unit test comparing the whole
serialised payload object, and four in the listener integration test comparing the whole `data`
object of the body WireMock received. All were run green against the *enum-typed* records before the
type swap and again after it, with the same expected JSON both times.

**Read the two heights precisely, because only one of them covers Jackson configuration.** The
mapper unit test builds its own `new ObjectMapper()`, so it measures Jackson's *defaults*; the
integration test goes through the application's configured mapper and a real HTTP body, and it is
that one which discharges the PRD's claim about "Jackson's behaviour". What the pair establishes for
the slab: Jackson's default enum serialisation is `name()`, and nothing here configures
`WRITE_ENUMS_USING_TO_STRING`, so a State or an Event reached the wire as its name — which is
exactly the string these consumers now pass.

**Two corrections to how far that generalises**, made in review because a later slice would have
over-trusted the looser version. A `label`-valued `toString` on one of the four enums would *not*
have changed the wire format, since that feature is unset and `toString` is therefore never
consulted — the live risk was only ever a `@JsonValue`, and `NegotiationState` exposes `getLabel`,
`getDescription` and `getValue` for one to be added to. And after this slice the two Negotiation
payloads are no longer sensitive to any of it: their strategy calls `.name()` itself, so Jackson
never sees an enum on that path. Only `NegotiationResourceStateUpdatedWebhookEvent` still converts
an enum through Jackson, via `convertValue`, so it is the one payload whose tests still guard enum
serialisation config. Slices 08–10 should not read "the enum-serialisation question is settled" as
"our JSON is pinned" — pin the JSON where you change it.

**Compare the object, not the paths.** The listener test already asserted every field of the
state-change delivery with `matchingJsonPath`, and it would have stayed green if a *seventh* field
had appeared or if `fromState` had turned into an object with a `name` property. Whole-object
comparison is what makes the wire format pinned rather than sampled, and it is three lines
(`readTree` on both sides, `assertEquals`). Slices 08 and 09 assert JSON responses next; use this
shape rather than a path per field.

**Translating at the call boundary costs three lines and reads honestly.** `nameOf(Enum<?>)`,
private and static, in each strategy that reads a change event. Null-preserving deliberately: the
Negotiation state-change event cannot carry a null (its only producer builds it from `valueOf`), but
`NewNegotiationEvent` carries `Negotiation.currentState`, which has no non-null constraint, and today
a null there produces a delivery with `"currentState": null` rather than an exception inside an event
listener. Keep that property when writing the same translation in slices 05, 08 and 09 — the slab's
promise is that nothing observable changes, and an NPE where there was none is observable.

**A payload built by `objectMapper.convertValue` needs no translation at all.**
`NegotiationResourceStateUpdatedWebhookEvent` is produced by `DefaultWebhookMappingStrategy`, which
converts the application event straight into the record, so its three fields went from enum to
`String` with the record as the only file touched — Jackson does the name step. This is why the slice
covers three payload records but only two boundaries, **and it is what slice 10 needs to know**: when
`ResourceStateChangeEvent` flips to strings there is nothing to delete in the webhook subsystem,
because nothing was written there.

**`NewNegotiationEvent` is unowned, and it is slice 11 that is forced to convert it.** It lives in
`eu.bbmri_eric.negotiator.negotiation`, holds a `NegotiationState` field, and is therefore a
decoupling-gate violation — but it is not one of slice 10's four seam types and no slice in the PRD's
table names it. The forcing function is a compile error, which is the good case: once slice 11 makes
`Negotiation.currentState` a `String`, `NegotiationServiceImpl`'s
`new NewNegotiationEvent(this, id, negotiation.getCurrentState())` stops compiling. So slice 11
converts the event, and `NewNegotiationWebhookMappingStrategy`'s `nameOf` goes with it rather than
with slice 10. Its other consumer is `NewNegotiationHandler`, which slice 05 owns; slice 05 should
expect to translate at that boundary too, and to have its translation removed by 11 rather than 10.

**Deliveries the webhook subsystem keys on State names, for the record.** Two: the added-Negotiation
delivery on the `DRAFT` → `SUBMITTED` pair, and the suppression of that delivery for a Negotiation
created in `DRAFT`. Both now read as `WellKnownNegotiationStates` comparisons. Everything else in the
subsystem carries the name as data and needed no constant, which is the holder growth rule behaving
as slice 01 predicted: six payload fields, two comparisons, no new name.

**`nix develop` works from the main checkout but not from a worktree.** Slice 03 recorded that
`nix develop` broke after `bacf4391 dev: bump toolchain` with a permission error; the sharper form is
that `nix develop .#opencode` inside `.claude/worktrees/<slice>` fails with `opening lock file
'/nix/store/…-source.lock': Permission denied`, while the same command from
`/home/claude/repos/bbmri-eric-negotiator` succeeds. So run Maven from the main checkout and point it
at the worktree — `nix develop .#opencode --command <script> -f <worktree>/backend <selector>` — which
keeps the Nix shell, keeps each worktree's own `target/`, and needs no bare `mvn`.

## What slice 05 settled, for slices 10 and 11

**The batch is seven files, not the six every count records.** `ResourceStateChangeHandler` names no
Lifecycle enum and so appears in no grep, but it built its message from `getLabel()` on both of the
event's Resource States - the PRD's "fourth kind of work", using an enum as a lookup table rather
than as an identity. Ticket 02 concluded the file "needs no change here". A later slice taking a
file list from a whole-word identifier scan should add the label, description and ordinal readers by
hand; they are invisible to it by construction, which is the same trap the decoupling guard's second
rule exists for.

**The catalog has its first production caller, and it is shaped the way slice 02 asked.** Two
`metadata(Scope.RESOURCE, Element.STATE, name).label()` calls, one per State rather than one per
field, reached through the coordinates rather than a per-enum method. `ResourceStateChangeHandler`
gained a third constructor dependency for it. Its new test builds `new EnumBackedLifecycleCatalog()`
directly - the class has no state and needs no Spring - so the labels the message is pinned against
are the real ones and not a mock's idea of them.

**Carve-out 2 needed nothing outside `notification/internal`, because slice 09 had already built the
gap-crosser.** `Negotiation.setStateForResource(String, String)` exists and is null-preserving, so
the Spawn loop's two holder constants resolve straight onto it and the third swap is a comparison.
`ResourceNotificationService` differs by its imports and the three lines at `:47`, `:57`, `:60`;
`NegotiationInProgressHandler` by its import and the one line at `:31`. Exactly the four references
ticket 07 named, and nothing else - so the coupling slab relocates a loop whose body is already
String-shaped, and its diff shows only the relocation.

**Six translations, and the slice that deletes each.** Slice 10 flips the two change events; slice 11
flips the entity field and `NewNegotiationEvent`.

- `NewNegotiationHandler.nameOf(Enum<?>)` - **slice 11**, with `NewNegotiationEvent`, exactly as
  slice 04 predicted from the other end.
- `PendingNegotiationReminderHandler.nameOf(Enum<?>)` - **slice 11**, with
  `NegotiationResourceLink.currentState`. It reads the entity, not an event, which is why it is a
  second copy of the same three lines rather than a shared helper: the two die in different slices
  and a shared home would outlive one of them.
- `NegotiationSubmissionHandler.nameOf(Enum<?>)` - **slice 10**, with the seam.
- Bare `.name()` on `getToState()`, once in `NegotiationInProgressHandler` and twice in
  `NegotiationStatusChangeHandler` - **slice 10**.
- `ResourceNotificationService`'s `.name()` on `getCurrentState()` - **slice 11**.
- `ResourceStateChangeHandler`'s two `.name()` on the change event - **slice 10**. The catalog call
  behind them is not a translation and outlives both; it dies at the cutover.

**Null behaviour, in three groups, with one accepted delta.** Slice 04's rule is that an NPE where
there was none is observable, and it holds here except in one place that could not keep it.

- *Faithful by construction:* `NegotiationStatusChangeHandler`'s switch and
  `ResourceNotificationService`'s comparison both threw on a null State before the swap - a `switch`
  over a null enum and `.equals` on a null receiver - and both still throw.
- *Kept null-tolerant through `nameOf`:* `NewNegotiationHandler`, `PendingNegotiationReminderHandler`
  and `NegotiationSubmissionHandler`. All three compared with `!=` or `==` before, so null was
  quietly ignored, and it still is.
- *The delta:* `NegotiationInProgressHandler` turned a null-safe `==` into `.name()`, so a null
  destination State now throws where it used to be passed over. Carve-out 2 forbids that file a new
  member and a null guard inline would have been the structural change the AC rules out. The field
  cannot be null in production: `PersistStateChangeListener` is its only publisher and builds it with
  `NegotiationState.valueOf`, which throws rather than returning null. Slice 10 removes the `.name()`
  and the question with it - **and if it harmonises the group, harmonise onto null-tolerance, not
  away from it.**

**A holder constant works as a `switch` case label, which is worth knowing before assuming it does
not.** A qualified `public static final String` is a constant expression, so
`case WellKnownNegotiationStates.SUBMITTED ->` compiles and the destination-State switch survived the
swap as a switch rather than degrading into an if-chain. Its `default` is what keeps the four
Negotiation States that notify nobody notifying nobody, and a name absent from the holders lands in
that same silent branch.

**Test churn was three files, not the eight ticket 02 predicted, and the prediction counted the wrong
thing.** Eight test files under `notification/` name a Lifecycle enum, but the slab gate exempts test
scope and the seam still deals in enums, so seven of them compile and pass untouched. What churns is
a test whose *subject's signature* moved, and only `ResourceNotificationServiceTest` qualified - its
`verify` calls followed the Spawn loop onto the String overload, including three `any(...)` matchers
that had to become `anyString()` or they would have matched the wrong overload. **Slice 10 inherits
the other seven**, when the two change events flip.

**Two behaviours are pinned that were pinned by nothing before**, both written and run green against
the enum-typed code in their own commit. The Resource state-change message text, including one pair
whose labels differ from the State names (`RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT` reads "Resource
Unavailable, Willing to Collect"), so a label silently read as a name fails rather than passes. And
the States that notify nobody, named as the *complement* of the four branched States rather than
listed, so a ninth Negotiation State joins that test instead of escaping it.

**Whole-suite count is 1472/0/0/16 in 159 classes, and it corrects a prediction rather than
confirming one.** This slice added exactly 8 tests and one class, so the tip before it held 1464 in
158 - four below the 1468 slice 08's section computed by stacking 08 and 09. The gap is not this
slice's and it is not new: it is the same unattributed four that slice 02's section reports from the
other side. Take **1472/0/0/16 in 159 classes** as the baseline and do not try to reconcile against
1468.

## What slice 06 settled, for slice 11

The three files are `NetworkStatistics`, `SimpleNetworkStatistics` and
`NetworkStatisticsServiceImpl`, and the type change is one line in each. `NetworkStatsRepositoryImpl`
is **not** one of them and is not in the diff: it names no enum, only quoted State names inside
query text, and slice 03 pinned those by line.

**The translation is `stateNameOf(Object)`, and it reads both shapes deliberately.** Every other
slice so far translated at a *typed* accessor, so a `nameOf(Enum<?>)` helper was enough and slice 11
gets a compile error when the accessor's type flips. This call site is different:
`countStatusDistribution` returns `List<Object[]>`, so the projected State arrives as an `Object`
and **any cast here compiles clean forever**. Writing `((Enum<?>) result[0]).name()` would therefore
have gone green today and thrown `ClassCastException` at runtime the moment slice 11 makes
`Negotiation.currentState` a `String` — the precise silent-breakage failure mode the PRD's user
story 2 exists to prevent, with no compiler and no type to catch it. So the helper is

```java
private static String stateNameOf(Object projectedState) {
  return projectedState instanceof Enum<?> state ? state.name() : (String) projectedState;
}
```

**Slice 11 needs no edit in this subsystem, and should confirm that rather than assume it.** When the
entity field becomes a `String`, the projection starts yielding strings, the second branch takes
over, and the map is keyed identically. The check is
`NetworkControllerTests#getStatistics_validNetwork_distributionIsKeyedByStateNameAndOmitsDrafts`,
which fails loudly if the keys change shape. Slice 11 may then delete the `Enum<?>` branch — that is
a cleanup, not a fix, and the test proves either version.

**Only one thing about the response differs at all, and it is JSON key order.** Before, the keys came
out of a `HashMap` keyed by enums, and an enum's `hashCode` is its identity hash — so the order
varied between JVM runs. `{"SUBMITTED":1,"ABANDONED":1,"IN_PROGRESS":2}` before,
`{"IN_PROGRESS":2,"ABANDONED":1,"SUBMITTED":1}` after, same `HashMap`, now ordered by
`String.hashCode` and therefore stable run to run. The set of keys, their spelling and their values
are unchanged. **This is recorded so a later reader diffing two captured bodies does not read it as a
regression** — the old order was never a contract anything could have depended on, and the change is
toward determinism, not away from it.

**The endpoint had almost no pin, and now has one.** `getStatistics_validNetwork_ok` asserted three
numbers and one distribution entry out of a body carrying seven statistics, two id lists and three
entries. The other four statistics — median, successful, new requesters, active representatives —
were asserted by nothing anywhere. The method now pins the whole body, and a second method pins the
map's keying and the absent `DRAFT`. Both were written from a body **captured before the type
change** and run green against the enum-keyed code first, which is what makes them a record of the
old numbers rather than a description of the new ones. Net +1 test.

**The absent `DRAFT` key is a real assertion, not a vacuous one.** `negotiation-6` in the test seed
is a `DRAFT` created 2024-11-12 on resource 4, resource 4 is in network 1, and the window the test
uses contains that date — so the row is in scope and only the `!= 'DRAFT'` literal at
`NetworkStatsRepositoryImpl:163` keeps it out of the distribution. That literal is one of slice 03's
fourteen, so the SQL side and the response side are now pinned from both ends.

**Slice 03's two handed-over facts, both confirmed by reading the callers — and its "three
uncalled queries" is four uncalled methods.** The service takes its count and its median off
`NegotiationRepository`, not off `NetworkStatsRepositoryImpl`, so that file's
`getMedianResponseForNetwork` is unreachable from production and the service reaches **five** of the
file's nine methods. The other **four** — `countIgnoredForNetwork`, `getMedianResponseForNetwork`,
`getNumberOfSuccessfulNegotiationsForNetwork` and `countAllForNetwork` — have no production caller.
Slice 03's section says three, which is right for what it was counting: the uncalled queries holding
**pinned literals**. `countAllForNetwork` holds none, so its sweep had no reason to see it. Slice 11
should read slice 03's three as "three uncalled *literal-holding* queries" and not as the file's
uncalled surface. Neither fact affects this slice, since no enum reached any query; both were
checked because "the network statistics subsystem" reads as a much larger surface than it is.

**The whole-suite count is 1473/0/0/16 in 159 classes, and for once it reconciles exactly.** Slice
05 recorded 1472 in 159 and asked later slices to take it as the baseline; this slice adds one test
and no class, which predicts 1473 in 159. That is what the run reports. The four unattributed tests
slices 02, 03 and 05 each reported from a different side are still unattributed — they are inside the
1472, not introduced or resolved here.

**One documentation bug found and deliberately not fixed.**
`NetworkStatistics.getStatusDistribution` carries
`@Schema(example = "{\"OPEN\": 50, \"CLOSED\": 90, \"PENDING\": 10}")`, naming three States
that appear in no enum and no Definition Family; `SimpleNetworkStatistics` carries a correct example
on the same field. The slice's AC asks for the example unchanged and correcting it would be a
published-schema edit this slice did not come to make, so it is filed in the issue and left. It is
not a decoupling defect — it predates the slab and will outlive it.

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

## What slice 08 settled, for slices 09, 10, 11 and 12

**The batch is six files, and five of them name no enum.** `NegotiationDTO`, `UpdateResourcesDTO`,
`NegotiationTimelineImpl`, `ResourceWithStatusDTO` and `ResourceViewDTO` are clean.
`NegotiationModelMapper` is not, and cannot be from here - see the next paragraph. The three
assemblers stayed out, as the issue directed, and the three metadata DTOs were not touched.

**ModelMapper does not coerce a converter's result to the destination type, and this was measured
rather than assumed.** The create-side mapping writes `Negotiation.setCurrentState`, which is still
enum-typed until slice 11. Returning a `String` from that `Converter` compiles and then fails at
runtime with `IllegalArgumentException: argument type mismatch` out of
`MappingEngineImpl` - `NegotiationMapperTest` went red on exactly the two create-side cases. So the
mapper keeps `Converter<Boolean, NegotiationState>` and one `NegotiationState.valueOf(...)`, with
the *names* moved onto `WellKnownNegotiationStates` and the `valueOf` sitting alone as the boundary
translation. **Slice 11 deletes it**: once `currentState` is a name, `initialStateFor(...)` is
assigned directly and the import goes. Until then this file is the slab's one known gate violation,
and it is deliberate. Slice 12's guard runs after 11, so the gate still closes on time - but if 12
is ever run early, this is the file it will report.

**The entity-to-DTO status conversion did not disappear, and the issue's note over-predicted.** It
already returned a name, but it returns the **empty string** for an absent State, not null, and
`NegotiationDTO` is `@JsonInclude(NON_NULL)` - so letting it become the identity would drop `status`
from the response body entirely for a Negotiation with no State. It is now `nameOf(Enum<?>)`,
deliberately **not** null-preserving in slice 04's sense, because `""` is what the wire has always
carried. Slice 11 turns it into the identity *and must keep the empty-string branch*.

**Translations this slice wrote, for slices 10 and 11 to delete.** Three, and only one belongs to
slice 10:

- `NegotiationModelMapper.nameOf(Enum<?>)` plus the `NegotiationState.valueOf` beside it - **slice
  11**, with the entity field.
- `ResourceServiceImpl.stateNamed(String)` - **already gone**, deleted during the rebase below
  rather than by a later slice. Slice 09 closed the far end of the same gap while this slice was in
  flight.
- `ResourceStateChangeListener`'s two `.name()` calls on enum constants - **slice 10**, when
  `ResourceWithStatusDTO`'s producer and the change event both deal in names.

Note the comparison order there is `resource.getCurrentState().equals(CONSTANT.name())`, receiver
first, which looks backwards next to the null-safe `CONSTANT.equals(state)` in the timeline. It is
deliberate: the projection's `currentState` comes off a `left join` and can be null, and today that
throws. Constant-first would silently turn the throw into `false`. Keep the order until something
decides that null case on purpose.

**`UpdateResourcesDTO` got a deserializer rather than a catalog call, and the reason is Jackson.**
`@JsonDeserialize(using = ...)` instantiates the deserializer reflectively, so it cannot hold the
Spring-managed `EnumBackedLifecycleCatalog`. `NegotiationResourceStateNameDeserializer` is therefore
a second enum-backed class in `negotiation/state_machine/resource/` - the exact shape slice 07
landed for `InformationRequirementCreateDTO.forResourceEvent`, and deleted at cutover alongside it.
It is not a second source of truth: it reads `NegotiationResourceState.valueOf`, the same enum the
catalog reads. **The catalog stays the route for any caller that can inject it**; a Jackson
deserializer is the one place that cannot.

**Without it the 400 would have become a 500-shaped answer.** With the field a bare `String`,
`{"state": "NOT_A_STATE"}` binds fine and blows up later in `stateNamed`'s `valueOf`. That still
lands on 400 through `handleIllegalArgument`, but with a different body and - worse - only on the
non-`DRAFT` branch, so a draft would have started accepting nonsense. `NegotiationControllerTests`
now pins the 400; it was written and run green before the type swap.

**One accepted micro-delta, shared with slice 07.** Jackson's default enum binding accepts a JSON
*integer* as an ordinal (`FAIL_ON_NUMBERS_FOR_ENUMS` is unset in `application.yaml` and
`application-prod.yaml`), so `{"state": 3}` used to bind to the fourth Resource State. Through the
deserializer, `getValueAsString()` yields `"3"` and it is refused with a 400. Preserving it would
have meant writing `values()[i]` by hand - deliberately new ordinal-as-identity coupling, in a class
built to be deleted, to keep an accident nothing documents and no client uses. Slice 07 made the
same call for `forResourceEvent`. Recorded rather than preserved; if either ever needs undoing, undo
both.

**The `@Schema` on `NegotiationDTO.status` said `example = "PENDING"`, which is not a Negotiation
State at all.** It is now `IN_PROGRESS`. This is a published-schema change beyond the type swap and
is intentional: the slice's AC asks every State field to read as a string *with a worked example*,
and an example naming a State that does not exist is not one. `UpdateResourcesDTO.state` and
`ResourceWithStatusDTO.currentState` had no `@Schema` at all and now have one each. No
`defaultValue` was added anywhere - the PRD's decision is `type: string` keeping `example`, and
nothing more.

**Three behaviours are pinned that were pinned by nothing before**, all written and run green
against the enum-typed code first, in their own commit: the timeline's exclusion of the two States
Spawn writes, the payload-updatable rule over the *whole* Negotiation State set (a set-equality
assertion resolved through `NegotiationState.values()`, so a name that changes meaning fails here
rather than silently), and the unknown-State 400. The payload-updatable rule is wire-visible, not
just an internal link decision: `isPayloadUpdatable()` is a public getter on a serialised DTO, so
`payloadUpdatable` appears in every Negotiation response body.

**Whole-suite count moved 1457 -> 1462, and all five are this slice's own pins.** Mapper test 7 to
10, timeline test 4 to 5, controller test plus one. No other test file changed count; the four
touched by the type swap (`EnumBackedLifecycleTestAdapter`,
`NegotiationLifecycleServiceImplTest`, `NegotiationControllerTests`, and the mapper test) changed
assertions only. Take **1462/0/0/16 in 158 classes** as the baseline.

**The characterization adapter keeps its typo guard.** `overrideResourceStates` now builds
`new UpdateResourcesDTO(ids, resourceState(state).name())` rather than passing the raw string
through - `resourceState` is the adapter's loud-on-a-misspelled-name helper, and dropping it would
have let a typo in a characterization test look like a refused update.

**This slice was rebased onto slice 09, and the two met in one method.** Slice 09 landed while this
one was in flight, and both changed `ResourceServiceImpl.updateResourcesInANegotiation` for the same
reason from opposite ends: 09 made `setStatusForUpdatedResources` take a `String`, 08 made
`UpdateResourcesDTO.getState()` *return* one. Each side had written a translation to bridge a gap
the other was closing - `nameOf(...)` on 09's side, `stateNamed(...)` on 08's - and **neither
compiled against the other**. The resolution deleted both: a `String` field feeding a `String`
parameter needs no conversion, so the call is now
`setStatusForUpdatedResources(negotiation, resourcesToUpdate, updateResourcesDTO.getState())`.
`ResourceServiceImpl` names no Lifecycle enum, which is what 09's own AC asked for.

Two smaller merge repairs: 09's two new `ResourceRepositoryTest` assertions called `.name()` on the
projection's `currentState`, which this slice made a `String` - the `.name()` calls went and the
string literals they compare against are unchanged. Nothing else conflicted.

**The two `Landed` rows were measured in separate worktrees and do not compose.** 08 read
1462 at base `a3e02e59`; 09 read 1463 at the same base. Stacked, the tip should hold 1457 + 6 + 5 =
**1468**, but that is arithmetic, not a measurement - the whole suite was not re-run after the
rebase. The next slice should measure rather than trust either figure. This is the third time the
recorded count has gone stale for the reason slices 01 and 03 both gave.

**The frontend needs no change, checked by reading rather than assumed.** It reads
`resource.currentState` as a string key into `stateOrdinalMap` and through `transformStatus`
(`NegotiationPage.vue`, `ResourceItem.vue`), and sorts on the `currentState` field name
(`NegotiationList.vue`, `FilterSort.vue`). Neither enum carries `@JsonValue` or `@JsonFormat`, and
nothing sets `WRITE_ENUMS_USING_TO_STRING`, so the serialised value was the name before and is the
same name now.

## What slice 10 settled, for slice 12

**The batch is 25 production files and 11 test files, and the slice was not split.** The issue asked
whether it should be, and the answer is no for a reason worth recording: the four seam types are one
compile unit. There is no ordering of them that leaves an intermediate commit both green and honest -
flipping an event without its readers does not compile, and flipping a reader without its event
compiles into a comparison that is always false. What *was* separable is the pinning test, and it is
its own commit. The issue's warning that "size is not well predicted by file count" turned out to be
true of the *test* tree rather than the production one: 22 of the 25 production files are a one- or
two-line change.

**The three assemblers are four, and the fourth was invisible to every grep.** `ResourceEventAssembler`
builds a link to `ResourceLifecycleController.getEvent` and was already passing `entity.getValue()`
straight through - which compiled only because `ResourceEventMetadataDto.getValue()` is *itself*
enum-typed. It names no Lifecycle enum, so it appears in no identifier scan, and it moved in the
opposite direction from the other three: it *gained* a `.name()` rather than losing a conversion,
because the DTO behind it is one of ticket 04's three carve-outs and must stay enum-typed. **Slice 12
should expect this shape to exist and to be legal**: a file that reaches an enum through a
carved-out DTO's accessor, importing nothing. The guard's second rule covers the four seam accessors
and the two service interfaces; it does not cover the three metadata DTOs, and it should not - the
carve-out is exactly the statement that those are ticket 04's.

**The two Event path converters could not be deleted without touching the two metadata controllers,
and that was a hard requirement rather than a judgement call.** `NegotiationLifecycleController.getEvent`
and `ResourceLifecycleController.getEvent` bound their path variable through the same two registered
converters. Deleting the converters would have dropped both endpoints onto Spring's default enum
binding, which is case-*sensitive* and produces a different 400 body - and
`characterization/rest/LifecycleMetadataEndpointsTest` pins both properties explicitly
(`caseHandlingOfSingleItemEndpoints_isPinned`, `unrecognisedNegotiationEvent_isPinned`,
`unrecognisedResourceEvent_isPinned`). So each controller now resolves the name inline, with the
converter's exact body: `valueOf(event.toUpperCase())` in a try, `ResponseStatusException(BAD_REQUEST)`
in the catch. **Those two `valueOf` calls are not a translation this slab owes anyone.** They sit
inside `negotiation/state_machine/`, they exist because the endpoint publishes the metadata of a
closed Event *universe*, and that is the question ticket 04 reopens. They go when it is answered.

**The 400's shape is `ResponseStatusException(HttpStatus.BAD_REQUEST)` with no reason, and that means
an empty body and no content type.** `NegotiatorExceptionHandler` has no handler for it or for
`MethodArgumentTypeMismatchException`, so the status resolver's `sendError` is what a client actually
sees. Anything that produces this 400 in a later slab must throw that exact exception; a
`WrongRequestException` or an `IllegalArgumentException` would land on 400 with a `ProblemDetail`
body and break the pin.

**Ticket 03's "nothing pins this today" is wrong, and the correction is the useful half.** The
*status code* was already pinned for both lifecycle paths - `sendEvent_InvalidEvent_BadRequest` and
`sendEvent_InvalidResourceEvent_BadRequest` - and the lower-case acceptance was pinned too. What was
pinned by nothing is the response **body**, which is the part that a moved check silently changes. So
the test written before the change extends those two methods with an empty-body and null-content-type
assertion rather than adding a class.

**A third thing was pinned by nothing and is the one that would actually have broken.** The Resource
lifecycle endpoint refuses an unknown Event *before* it refuses the caller, because binding ran
before the handler body. A check written into the handler after the authority test would have turned
that 400 into a 403 for any caller who is neither representative nor creator - green under every
existing assertion. `sendEvent_invalidResourceEventAndNoAuthority_refusesTheEventNotTheCaller` pins
it, was run green against the enum-typed path variable, and is why `lifecycleEventNamed(...)` is the
first statement of both handlers rather than sitting next to the call it guards.

**Four new catalog callers, all metadata readers, none of them deciding anything.**
`NegotiationController` (label *and* description, for the possible-events DTO - one `metadata` call
per Event, as slice 02 asked), `NegotiationModelAssembler` (label, for a link's display name),
`ResourceWithStatusAssembler` (label, same) and `NegotiationLifecycleServiceImpl` (label, for the
"You are not allowed to %s the Negotiation" message). With slice 05's handler that is five, and every
one of them reaches through the `Scope`/`Element` coordinates.

**`ResourceStateChangeListener`'s two `.name()` calls stay, and slice 08's prediction about them was
wrong.** Slice 08 assigned them here, expecting them to disappear "when `ResourceWithStatusDTO`'s
producer and the change event both deal in names". They do not, and the reason is worth stating
because it recurs: the enum is on the **constant** side of the comparison, not on an accessor.
`resource.getCurrentState().equals(NegotiationResourceState.RESOURCE_UNAVAILABLE.name())` compares a
`String` field against a constant that is not one of the nine Well-known names, so removing the
`.name()` would make the comparison always false rather than removing a conversion. The file sits
inside `negotiation/state_machine/`, so the reference is gate-legal and dies with the enums. Adding
those two names to `WellKnownResourceStates` was the alternative and is refused: slice 01's growth
rule is a test, and "the two terminal Resource States" is a modelling decision, not a name this slab
discovered it needed.

**This slice landed *after* slice 11 and was rebased onto it, and the two met in four files - the
third time this slab has hit slice 08's collision.** Both slices changed the two Lifecycle service
impls, `ResourcePersistStateChangeListener` and `NegotiationLifecycleServiceImplTest` for the same
reason from opposite ends: 11 made the entity fields and the JPA projections deal in names and wrote
translations at the seam; 10 made the seam deal in names and wrote translations at the entity. **Each
side's translation was exactly what the other side deletes, so the resolution deleted both**, as it
did when 08 met 09.

- Both current-State readers lost their `.map(...)`: 11's `.map(NegotiationState::valueOf)` and 10's
  `.map(Enum::name)` are gone, because `findNegotiationStateById` and
  `findNegotiationResourceStateById` now return `Optional<String>` and the seam wants a name.
- `ResourcePersistStateChangeListener.nameOf(Enum<?>)` is gone with them - the link row's accessor
  returns a name, so there was nothing left to convert - and the publication went back from 11's
  `ResourceStateChangeEvent.fromNames(...)` to the plain constructor, because this slice deletes that
  factory.
- **The `valueOf` that `fromNames` performed had to be kept by hand**, as
  `.map(event -> NegotiationResourceEvent.valueOf(event).name())`. It is easy to lose here: 11 did
  not drop that check, it moved it *into* `fromNames`, and deleting the factory without carrying the
  check back out would have widened the seam's contract with nothing failing.
- `NegotiationLifecycleServiceImplTest`'s "no change" assertion had a `.name()` on each side in turn
  - 11 on the seam's result, 10 on the entity's expectation - and now needs neither.

**The `valueOf` calls left in the two persist listeners are a check, not a conversion, and should not
be read as leftovers.** `PersistStateChangeListener` publishes no event at all when a state machine
id does not resolve, and that log-and-skip branch is the only thing keeping a malformed Transition
out of every downstream handler. Writing `valueOf(x).name()` keeps it exactly; dropping the `valueOf`
would have widened the seam's contract silently.

**Slice 05's one accepted null delta is undone here, in the direction slice 05 asked for.**
`NegotiationInProgressHandler` reads `WellKnownNegotiationStates.IN_PROGRESS.equals(event.getToState())`
- constant first, so a null destination State is passed over rather than throwing, which is what the
pre-slab `==` did. Carve-out 2's "no new member in that file" is satisfied because no member was
needed. `NegotiationStatusChangeHandler`'s `switch` still throws on a null, as its `switch` over a
null enum did.

**Three new properties of the two service interfaces, for the cutover slab to decide on purpose
rather than by accident.** All three were previously unreachable, because the path converters refused
an unknown name before any of this code ran, and all three are unreachable *today* through HTTP,
because the controller validates and `EnumBackedLifecycleTestAdapter` validates.

- `NegotiationLifecycleService.sendEvent` with an unknown Event name: not in the possible set, so the
  label lookup for the refusal message throws `IllegalArgumentException` → 400 `"Bad request."`,
  where a *known* but impossible Event still gives 403.
- `ResourceLifecycleService.sendEvent` with an unknown Event name: silently returns the Resource's
  current State, which is what it already did for a known-but-impossible Event.
- `getPossibleEvents` no longer resolves Spring Statemachine's trigger ids through `valueOf`, so a
  trigger id outside the enums would be reported rather than throwing. The configuration is built
  from the enums, so it cannot happen while they exist.

**One result the rebase produced that neither slice could have measured alone.** With slices 10 and
11 both in, the only production files outside `negotiation/state_machine/` that still name a
Lifecycle enum are the three carved-out metadata DTOs - `NegotiationStateMetadataDto`,
`ResourceStateMetadataDto` and `ResourceEventMetadataDto`. That is the slab's stated end state
reached, and it means **slice 12's decoupling guard should close with exactly the three exemptions
the PRD names, and no others**. Slice 08's known deliberate violation in `NegotiationModelMapper` is
gone too, deleted by slice 11 as that section predicted.

**Both services now compare Event names case-sensitively and validate none of them internally**, so
their correctness rests on every caller passing a canonical name. `existsByForEvent(event)` and
`getPossibleEvents(...).contains(event)` are plain `String` compares where they used to be enum
identity. Every caller does pass one today - the controller upper-cases and checks the catalog, the
adapter resolves through the enums - but the *type* no longer enforces it. The cutover slab should
decide on purpose whether the seam validates or keeps trusting its callers; this slice kept trusting
them, because doing otherwise would have added a refusal where there was none.

**`EnumBackedLifecycleCatalog` gained a fourth method, `label(Scope, Element, String)`, and it is
derived rather than new knowledge.** Four call sites wanted only the label and each had written the
same private `metadata(...).label()` helper - three of them in this slice, the fourth slice 05's.
The method is one line over `metadata`, respects slice 02's coordinates, and keeps the walk to a
label in the one class the cutover deletes. **This changed slice 05's `ResourceStateChangeHandler`,
so that section's "two `metadata(Scope.RESOURCE, Element.STATE, name).label()` calls" now reads
`label(...)`; the two calls, the two States and the message are unchanged.** Slice 02's "do not let
it grow" is about behaviour - a method that decides whether a Transition may fire - and this is not
that.

**One accepted micro-delta, on argument-resolution order.** `PUT /negotiations/{id}/lifecycle/{event}`
resolved its path variables before its `@RequestBody`, so a request with *both* an unknown Event and
an unreadable body used to get the converter's empty 400. It now gets the body's
`"Wrong request"` 400, because the Event check moved into the handler and the body is deserialised
before the handler runs. Both are 400 and no client sending valid JSON can see the difference. It is
recorded rather than preserved: preserving it would mean keeping a converter, which is what the slice
came to delete.

**The characterization adapter converts nothing any more, and still names the enums on purpose.**
`EnumBackedLifecycleTestAdapter`'s three helpers are now `negotiationEventName`,
`resourceEventName` and `resourceStateName` - `valueOf(x).name()`, so the *only* thing the enums are
used for is failing loudly on a misspelled name in a characterization test. Slice 08 asked that the
typo guard survive; this is what surviving looks like once there is nothing left to convert. Not one
assertion in the suite changed.

**`StateChangeEvents` stopped being a conversion and no assertion downstream noticed**, which is the
clearest evidence the suite was written the way slice 01's argument said it should be. Its
`nameOf(Enum<?>)` is gone and the two records read the payload directly.

**Test churn is 11 files and slice 05's "the other seven" is five.** The five notification test files
are `NegotiationInProgressHandlerTest`, `NegotiationStatusChangeHandlerTest`,
`NegotiationSubmissionHandlerTest`, `NotificationListenerTest` and `ResourceStateChangeHandlerTest`;
three of them now name no Lifecycle enum at all, and the two that still do
(`@EnumSource` over `NegotiationState`, and a `List<NegotiationState>` iterated into the event) do it
for their own reasons and take `.name()` at the constructor. The other two of slice 05's seven -
`NegotiationCreationNotificationHandlerTest` and `PendingNegotiationReminderHandlerTest` - never
needed a change: they name enums for the entity, not for the change event, so they are **slice 11's**.
The rest of the churn is `WebhookEventMapperTest`, `WebhookEventListenerIntegrationTest`,
`NegotiationLifecycleServiceImplTest`, `NegotiationModelAssemblerTest` (a third `null` constructor
argument) and the two characterization helpers.

**`NegotiationLifecycleServiceImplTest` moved to string literals rather than to `.name()`, and that
is the slab's precedent rather than a new choice.** Slices 07 and 08 did the same for constructor
arguments. Here it is also what makes the test read as a consumer of the seam does; a typo in one of
those literals fails loudly, because every one of them is either an input the service refuses or an
expected value being asserted.

**`Objects.toString(obj, null)` in `getPossibleEventsForNegotiationResource` was an enum-to-string
conversion and is now `List.copyOf`.** Worth naming because it is the one place in this slice where
the old code's *intent* was invisible: written over `Object`, it survived the type change with no
compile error and no behaviour change, and a reader arriving later would have had no way to tell it
had ever done anything. Slice 06 hit the same shape from the dangerous side and had to keep a cast
that read both types.

**The whole-suite count is 1480/0/0/16 in 159 classes, it is the first figure in this slab
measured on a tree that holds every slice from 01 to 11, and it reconciles exactly.** Slice 11 recorded 1479 in 159 classes. This slice adds
exactly one test - the 400-before-403 ordering pin in `NegotiationControllerTests` - and no other
test file changes count, which predicts 1480 in 159. That is what the run reports. Parity is 255
tests in 24 classes, 0 failures, 0 errors, 1 skipped; intended deltas are 8 tests, 0 failures, 0
errors, 0 skipped. **All three came out of one `--all` run at the rebased tip**, read from
`backend/target/surefire-reports` filtered by mtime and partitioned by class - which is the stronger
form of the gate, because 255 + 8 = 263 in 25 classes is also the cross-package ordering check the
parity-gate document describes.
**Take 1480/0/0/16 in 159 classes as the baseline for slice 12.** The four tests slices 02, 03 and 05 each
reported unattributed from a different side are still inside it and still unattributed.

An earlier figure of 1474/0/0/16 was measured for this slice before the rebase, on a tree that did
not hold slice 11. It is superseded, not reconciled - the two trees are different, and stacking the
counts is the arithmetic that has gone stale four times in this slab already.
## What slice 11 settled, for slices 10 and 12

**The batch is nineteen production files, not the five the issue names.** The five are the two
entities, the repository, the specification and the filter DTO. The other fourteen are forced: six
translations earlier slices wrote against the entity and bound to this slice by name, the two audit
records, `NewNegotiationEvent` and its two consumers, `NegotiationModelMapper`, and four files
inside `negotiation/state_machine/` that had to start translating at their own boundary because the
entity stopped doing it for them. Every one of the fourteen was reached by a compile error, which is
the good case and is why the count could be wrong in the issue without costing anything.

**The `assertEquals(enum, String)` hazard is real, it is silent, and it cost three tests.** Slice 06
met this from the SQL-projection side and warned that a cast there compiles clean forever. The test
side has the same shape and is worse, because it is everywhere: `assertEquals(Object, Object)`
accepts an enum on one side and a name on the other, compiles without a murmur, and fails only when
the test runs. Three sites did exactly that - `NegotiationControllerTests` comparing the entity
against `NegotiationResourceState.valueOf(json)`, and two in `NegotiationLifecycleServiceImplTest`
comparing the entity against a constant. **Slice 10 will meet more of these than this slice did**,
because the seam's accessors are what the notification and webhook tests assert on. A green
`test-compile` proves nothing about assertion sites; run the class.

**The four enums now appear outside `negotiation/state_machine/` in exactly seven files, and every
one of them is slice 10's or ticket 04's.** The three metadata DTOs the gate exempts, plus
`NegotiationModelAssembler`, `NegotiationEventAssembler`, `ResourceWithStatusAssembler` and
`NegotiationController` - which issue 10 already claims. Slice 08's known deliberate gate violation
in `NegotiationModelMapper` is gone: the `Converter<Boolean, NegotiationState>` and its `valueOf`
became `Converter<Boolean, String>` returning `initialStateFor(...)` directly, exactly as 08
predicted. **So slice 12's gate closes on slice 10's work alone.**

**The empty-string branch survived, and it is not the identity function.** Slice 08 said the
entity-to-DTO conversion "must keep the empty-string branch" and it does, now named `statusTextFor`
rather than `nameOf` - with both sides names, substituting `""` for an absent State is the only
thing the method still does, and `@JsonInclude(NON_NULL)` on `NegotiationDTO` would drop `status`
from the body entirely if it returned null.

**The audit records keep their enums and gained a named factory each.** ADR 0008 owns
`changed_to`, not this slab, so `NegotiationLifecycleRecord` and
`NegotiationResourceLifecycleRecord` still store `NegotiationState` and `NegotiationResourceState`.
`forStateNamed` is where the translation went, deliberately inside the package the enums live in,
and deliberately the loud kind: a name no State carries fails at that call rather than reaching the
history table. Both are null-preserving, because a Negotiation with no State recorded a row with no
State before. **They are the successor to the `valueOf` that used to sit in
`PersistStateChangeListener`** - the loud failure moved, it was not spent.

**The 400 on `?status=` is preserved by a bean-validation constraint, and this is the third shape
the slab has used for the same job.** `@KnownNegotiationStateNames` on the filter's list, answered
by `EnumBackedLifecycleCatalog.nameExists` through a Spring-injected `ConstraintValidator`. Slices
07 and 08 both needed a Jackson deserializer instead, because `@JsonDeserialize` instantiates
reflectively and cannot hold a Spring bean; a `@ModelAttribute` has no such problem, so **this is
the first consumer to reach the catalog the way slice 02 intended**. The cutover replaces the
catalog's three methods and nothing here changes.

**One accepted delta in that 400, and it is in the body rather than the status.** The old refusal
came from Spring's own conversion failure, so its `detail` read `Failed to convert property value of
type 'java.lang.String' to required type 'java.util.List' ... for value [NOT_A_STATE]` and named
`NegotiationState` in full. That string could not survive a change whose whole point is that the
type is gone. What is unchanged and is pinned: the 400, the ProblemDetail `title` of `Wrong request
parameters`, and the field error still landing on `status`. A client branching on status code or
title sees nothing; a client parsing that sentence was reading an implementation detail.

**Slice 03's guard was amended, the AC said it would not be, and the AC was unsatisfiable.** The
issue asks that "slice 3's guard stays green without amendment" while also predicting, two
paragraphs earlier, that the unquoted JPQL reference "breaks at Hibernate query validation once the
field is a string, which is the good case". Both cannot hold: repairing the break means quoting the
name, and the guard pins spelling. It broke exactly as predicted, and **the guard's own failure
message prescribes the repair that was made** - "move this entry into PINNED_NAMES as a QUOTED
literal and say in its reason why the loud failure was given up". So the population is still
fourteen, still line-exact, and the rule was not loosened. Two line numbers moved (35 -> 33,
74 -> 71) and one spelling changed. **Read the AC as "do not loosen the rule", because that is the
only reading under which it can be met.**

**The guard's replacement test had a hole that no failure would have shown, and the review found
it.** `theUnquotedReference_isStillBare` read the source line directly, independent of the scanner.
Its replacement, `noQuerySpellsANameBare`, runs entirely through `spellingAt` - and this slice
removed the last bare name from the tree, so nothing was left to prove the detector can still
recognise one. A `spellingAt` stuck on `QUOTED` would have reported green over a query Hibernate
refuses to start with. It now has a two-branch test of its own. **The general lesson for slice 12,
which is writing two more mechanical guards: a guard that checks a property through its own scanner
needs the scanner checked separately, and "the tree no longer contains an example" is exactly when
that stops being true by accident.**

**Two methods that no production path reaches, found while tracing callers and left alone.**
`NegotiationService.findAllByCurrentStatus` has no caller anywhere, not even a test.
`PdfContextBuilder` is a `@Component` nothing injects - which is why its status text got a pin of
its own: a change there is invisible to every other test in the suite, including the PDF endpoint's.
Neither is this slice's to delete.

**Three spellings of a Well-known name now coexist inside `negotiation/state_machine/`, and slice 10
removes the third.** The holder constant, the enum constant, and `NegotiationState.SUBMITTED.name()`
in `PersistStateChangeListener` and `NegotiationState.IN_PROGRESS.name()` in
`ResourceLifecycleServiceImpl` - both introduced here, both comparing an enum's name against the
entity's name. They are legal where they sit and they are ugly; when the seam deals in strings they
become holder comparisons.

**Parity and deltas were measured twice, at two tips, and the second one is the evidence.** 255
tests in 24 classes, 0 failures, 0 errors, 1 skipped; deltas 8, 0 failures, 0 errors, 0 skipped.
Taken once at the refactor commit and again after the review fixes, because the slab's rule is 255
after every commit and a number from one commit earlier is not this commit's number. The tag split
was verified the way [parity-gate.md](../state-machine-implementation/parity-gate.md) insists -
by which surefire reports exist, not by a pass count: the parity run wrote none for
`delta.IntendedDeltasAdr0005WillInvertTest`, the delta run wrote only that one.

**The whole-suite count is 1479/0/0/16 in 159 classes, and for the second time in this slab it
reconciles exactly.** Slice 06 measured 1473 in 159 and asked later slices to take it as the
baseline; this slice adds 6 tests and no class, which predicts 1479 in 159, and that is what the run
reports. The 6 are the three status-filter cases in `NegotiationControllerTests`, the two PDF pins,
and the guard's new spelling-detector test.

*Recorded in two passes.* The figure was deliberately absent when this section was first written -
the operator had asked for the full suite not to be run, and the row went in carrying the arithmetic
above, explicitly labelled as arithmetic. The suite was then run on request and the prediction
became a measurement. **Nothing else in this section was revised afterwards**, which is what makes
the agreement worth anything: the number was written down before it was measured.

The four tests slices 02, 03 and 05 each reported from a different side are still unattributed. They
are inside the 1473 baseline, not introduced or resolved here.

## What slice 12 closed, for the cutover slab

**The gate is `eu.bbmri_eric.negotiator.lifecycle.LifecycleEnumDecouplingGuardTest`, seven tests,
and it touches no production file.** It sits beside `WellKnownNamesTest` and
`RawStateNamesInSqlGuardTest`, which is where this slab's standing facts live. Its javadoc says what
every other guard in this tree says: it is deleted at cutover, whole, together with the enums it
forbids naming - and it will say so loudly, because it imports all four of them and those imports
stop compiling the day they go.

**The end state, counted rather than claimed.** Four enums, named in **22 production files**: 19 of
the 29 inside `negotiation/state_machine/`, plus the three carved-out metadata DTOs. At `3de318c3`
the figure was 65 in main sources, 42 of them consumers. Slice 10 predicted this exact shape from
the other side and it holds - **the gate closes with the three exemptions the PRD names and no
others.**

**Both rules were run red on purpose, seven times, before being trusted green.** A guard that has
only ever passed is a guard whose scanner might match nothing, and this slab has now been bitten by
that twice - slab 08's table rule, and slice 11's discovery that slice 03's replacement test had
gone vacuous when the tree lost its last bare name. What was injected, and what each one proved:

| Injected | Reported |
|---|---|
| `import ...NegotiationState;` into `NegotiationInProgressHandler` | **one** violation with file and line - the file's two `NegotiationStateChangeEvent` lines were spared, which is the word-boundary trap holding |
| `default Set<NegotiationEvent> ...` on `NegotiationLifecycleService` | the signature rule, naming the method and the enum. **The identifier rule stayed green** - the method sits in the exempt package - which is the whole reason this slice exists |
| a non-`String` accessor present on only one of the two events | the missing-accessor path, naming the class and method |
| the same accessor on both | the return-type path, `expected: <java.lang.String> but was: <java.lang.Object>` |
| `MINIMUM_PRODUCTION_SOURCES` raised past the tree size | "Only 437 production sources found ... the guard must never pass by scanning nothing" |
| the package exemption pointed at a package that does not exist | both anti-vacuity assertions: the exemption is gone, **and** the rule now matches nothing anywhere |
| the scan root pointed at a package that does not exist | `IllegalStateException` out of `moduleRoot`, on every test that walks the tree |
| an exemption pointed at a file that names no enum | "the exemption buys nothing and hides everything" |

**Slice 11's lesson is applied, and it changed the design.** Its warning was that a guard checking a
property through its own scanner needs the scanner checked separately, and that "the tree no longer
contains an example" is exactly when that stops being true by accident. The identifier rule was
already safe - `theIdentifierRule_matchesWhatItForbidsAndNothingElse` feeds it synthetic lines, and
the state_machine package is a live positive control. **The signature rule was not.** Its detector
is only ever pointed at four types that mention no enum, so a walker that always returned an empty
set would have reported green forever. So the class carries a private `CoupledSeamFixture`
interface - three methods, one per way a signature can hide an enum from an identifier scan: as a
type argument, as a bare return type, as a parameter. It is a fixture, not a rule; nothing
implements it and nothing scans it.

**An exemption that stops being necessary fails the guard, deliberately.** Each of the three
metadata DTOs is checked twice: the file still exists, *and* it still names an enum. The second half
is the unusual one, and it is aimed at ticket 04: when that ticket decouples a metadata DTO, the
same diff has to delete the exemption, and a reviewer sees the removal rather than having to notice
its absence. The failure message says so. This is also the reason the exemptions are pinned by full
path rather than by simple name - a fourth DTO borrowing one of these names elsewhere would
otherwise be quietly exempt too.

**"All production sources" is read as `src/main/java`, and the alternative was checked rather than
assumed.** A whole-word scan of `src/main/resources` for the four names returns **zero** hits, so
including it would have bought nothing today; and the rule's exemption model is package-shaped,
which resources are not. `DefinitionInertnessGuardTest` made the same call for its own reason. The
gap this leaves is a fully-qualified enum name in a configuration string, which nothing in this
repository does.

**The shape slice 10 warned about is legal and stays uncovered, correctly.**
`ResourceEventAssembler` reaches an enum through `ResourceEventMetadataDto.getValue()`, importing
nothing - so it appears in no identifier scan, and the signature rule covers the four seam types
rather than the three metadata DTOs. That is not a hole: the carve-out *is* the statement that those
DTOs are ticket 04's. When ticket 04 answers, the assembler's `.name()` goes with the DTO's enum.

**Both blocked-on guards are intact.** `DefinitionInertnessGuardTest` is untouched and green at 6
tests - this slab read no Definition Version table, so PRD user story 4 still has its gate.
`RawStateNamesInSqlGuardTest` is green at 7 with no amendment; slice 11 spent its one amendment and
this slice needed none.

**Copy rather than extract, for the third time.** The scan root, the comment-blanking reader and the
violation report are the fourth copy of the same forty lines. Slab 08 recorded why and slice 03
repeated it: these guards have different lifetimes and each is meant to be deleted whole. This one
outlives the slab and dies at cutover; `RawStateNamesInSqlGuardTest` outlives the cutover and is
consumed by the migration slab and ADR 0008.

**Standing decision 5 is discharged for the whole slab here, not just for this slice.** This slice
changes no production file, so nothing it did could break a screen - but it is the slab's closing
slice and no earlier one ran the app, so the check is done once, at the tip, against everything
slices 01-11 changed. The app was started from `target/negotiator-spring-boot.jar` on the `dev`
profile against the compose Postgres, with the published frontend, Traefik and the OIDC mock beside
it, and driven with a real user token obtained headlessly through the authorization-code + PKCE
flow. **Startup is itself the strongest part of the check**: Hibernate validates every named query
at context build, which is where slice 11's entity and JPQL changes would have failed loudly.

| Screen | Exercised | Result |
|---|---|---|
| Negotiation list | `GET /v3/negotiations`, `?status=SUBMITTED`, `?sortBy=currentState` | 200, rows render, the filter and the sort key both work off the now-`String` column |
| Negotiation list | `?status=NOT_A_STATE` | **400**, `Wrong request parameters`, `{status=must name States of the Negotiation Lifecycle}` - slice 11's `@KnownNegotiationStateNames` answering through the catalog, live |
| Negotiation page | detail, `/resources`, `/lifecycle`, `/timeline` | 200; possible Events carry label and description from the catalog, and the timeline renders `"changed the status of the Negotiation to Draft"` - a label, not a name |
| Negotiation page | `/pdf` | 200, a real PDF - the silent-breakage candidate finding 5 named |
| Lifecycle metadata | all four `/*-lifecycle/{states,events}` | 200, `value` / `label` / `description` and Resource State `ordinal` unchanged, which is the wire shape the frontend hand-codes against |
| IR admin | `GET /v3/info-requirements`, `GET /v3/access-forms` | 200 |
| IR admin | `POST /v3/info-requirements` with `forResourceEvent: "CONTACT"` | 201-shaped 200, row created and then deleted again |
| IR admin | the same with `"NOT_AN_EVENT"` | **400** through slice 07's `NegotiationResourceEventNameDeserializer` |

**What this does not cover, stated so it is not over-trusted.** No human looked at a rendered page;
the frontend was confirmed to serve (200 on `/`) and its API surface was exercised directly, which
is one step short of what standing decision 5 literally asks for. The three screens' *endpoints* all
answer correctly with real data from a real Postgres. Two facts make the remaining gap small: the
wire format is byte-identical by construction, and slice 08 and slice 10 each read the frontend's
State handling and found it reads `currentState` as a string key with no generated client.

**The four unattributed tests are found, and they were never missing.** Slices 02, 03 and 05 each
reported the same four-test gap from a different side, and four sections of this file record it as
outstanding. It is a **surefire reporting artifact, not four tests**.
`eu.bbmri_eric.negotiator.unit.mappers.RequestModelMapperTest` is the only class in the tree with an
`@Nested` inner class, and surefire's *plain-text* writer reports `Tests run: 0` for it while its
XML records the real 4:

```
$ grep 'Tests run' surefire-reports/....RequestModelMapperTest.txt
Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
$ grep -o 'tests="[0-9]*"' surefire-reports/TEST-....RequestModelMapperTest.xml
tests="4"
```

The class predates the whole effort - `git log` shows it last touched by the repository merge - so
**every `.txt`-based whole-suite sum in this slab has been exactly four low, from slice 01 onward**.
That is why the gap never moved and never attached to a slice: nothing introduced it. The two
sources were compared class by class and this is the *only* class they disagree on, so parity's
255 and the deltas' 8 are unaffected - no characterization class uses `@Nested`.

**Read whole-suite counts out of the XML reports, not the `.txt` ones.** The mtime rule this file
already insists on is necessary and was not sufficient. The `.txt` summaries are what every slice
summed and they are consistent with each other, so each slice's reconciliation arithmetic still
holds - the slices were comparing like with like - but the absolute figures recorded in the Landed
table above are all four understated for this reason. They are left as measured rather than
retro-corrected, because a number nobody re-measured is not a measurement.

**The whole-suite count is 1492/0/0/16 in 160 classes, and it reconciles exactly.** Slice 10 asked
for 1480 in 159 to be taken as the baseline; that is a `.txt` sum, so the comparable figure is 1484.
This slice adds one class of eight tests and changes no other test file, which predicts 1492 in 160.
That is what the run reports. Parity is 255 tests in 24 classes, 0 failures, 0 errors, 1 skipped;
intended deltas are 8 tests, 0 failures, 0 errors, 0 skipped. **All three came out of one `--all`
run**, partitioned by class rather than re-run, which is the stronger form slice 10 used: 255 + 8 =
263 in 25 classes is also the cross-package ordering check.

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
Flagged in the holder's Javadoc; **slice 08 met it, not slice 09**, and recorded it a second time on
the field itself, which is where a reader meets it. The value is unchanged. What is still undecided
is what an absent `state` should mean once a Definition Family can lack `SUBMITTED` - leave the
Resource alone, or resolve the initial State off the Definition Version. That is the second Resource
family's question, not this slab's.

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

**`nix develop` stopped working during slice 12, and the wrapper turned out to be unnecessary.**
Every invocation - from the worktree and from the main checkout alike - fails with `error: setting
up a private mount namespace: Operation not permitted`, which is the sandbox refusing Nix its
namespaces rather than anything about this repository. It cost nothing: `mvn` and `java` are already
on `PATH` in an agent shell (Maven 3.9.16, OpenJDK 21.0.12), so every command in
[parity-gate.md](../state-machine-implementation/parity-gate.md) works with the
`nix develop .#opencode --command` prefix simply dropped. Try the prefix first - it is what the gate
document specifies and it may well work again - and fall back to a bare `mvn` rather than treating
the failure as a blocker.

**The JDT/Lombok `target/` pollution is real, and it fires on ordinary editing.** Slab 08 recorded
it; this slice hit it. The language server compiles without Lombok, so `target/classes` ends up
holding a `NegotiationStateChangeEvent` with no getters, and the next `testCompile` reports 45
`cannot find symbol` errors in `characterization/service/StateChangeEvents.java` - a file nobody
touched. `mvn -f backend clean` clears it. The tell is that the errors are all missing Lombok
members, and that the same tree compiled clean an hour earlier.
