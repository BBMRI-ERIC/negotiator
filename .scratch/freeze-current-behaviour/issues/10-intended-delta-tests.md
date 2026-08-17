# ADR 0005 intended-delta tests

Status: resolved

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

ADR 0005 deliberately changes two behaviours. Pinning them as parity would freeze the very bugs the
ADR exists to fix, so they get their own clearly named test class that asserts **today's** behaviour
and states plainly that it is expected to be flipped.

**Available events include events that cannot actually fire.** The Resource service's available-events
set is computed from the graph and the caller's authority alone; it does not consult the Information
Requirement gate. So an event whose requirement is unmet is offered, and then refused when clicked.
Pin that today's set includes such an event, given an unmet requirement. ADR 0005 omits blocked events,
so this assertion is the thing that must fail after the cutover.

**Requirement hint links.** The Resource-with-status assembler adds per-row links for requirements
and submissions, one rel per row, with a rel name carrying the row's numeric identifier. ADR 0005
changes the inclusion condition to structural reachability, changes the display name to the Event's
label, and collapses the per-row rels into array-valued ones. Pin the current rel naming and the
current inclusion condition.

Both tests must be named and documented so it is unmistakable that failing them later is success, not
regression — a later session finding these red must be able to tell instantly that flipping them is
the intent. Keep them out of the parity gate selector's default expectations, or mark them such that
the gate reports them separately.

The frontend depends on the current rel naming — it filters link keys by prefix — so record that
these deltas force the two known frontend breakages, which standing decision 5 assigns to whichever
slab breaks them.

## Acceptance criteria

Every box was ticked against the surefire report of the runs recorded under Verification, not against
the source.

- [x] A test asserts that today's available-events set for a Resource includes an event whose
      Information Requirement is unmet.
      `deltaA_possibleEvents_offerAnEventTheRequirementGateThenRefuses` records a Requirement for
      `CONTACT`, asserts the Possible Events set for an administrator still `contains` it, and then
      fires it and asserts the gate's verbatim refusal message — the offer and the dead click in one
      method, because separately neither is the bug.
      `deltaA_theResourceListing_advertisesTheUnfireableEvent` shows the same delta reaching a
      client: the HAL listing carries a `CONTACT` link titled `Next Lifecycle event` while the
      Requirement is unmet.
- [x] That test is documented as an intended delta that ADR 0005 will invert.
      Both carry a javadoc naming ADR 0005 and stating that the assertion must fail once the
      Requirement check becomes a Built-in Stage of the one Evaluation Pipeline. The class javadoc
      opens with "A red test in this class is the cutover succeeding, not a regression."
- [x] A test pins the current per-row rel naming for requirement and submission links, including the
      numeric identifier in the rel name.
      `deltaB_requirementLinks_useOneRelPerRowCarryingTheRowId` creates two Requirements and asserts
      `requirement-<id1>` and `requirement-<id2>` are separate rels, each a single link *object*,
      each `href` ending in the same numeric id its rel carries, and that no array-valued
      `requirement` rel exists. `deltaB_submissionLinks_useOneRelPerRowCarryingTheRowId` does the
      same for `submission-<id>` and additionally pins that filing a Submission removes the
      Requirement hint. The ids come from the inserted rows, not from literals.
- [x] A test pins the current inclusion condition for those links.
      Two tests, one per half. `deltaB_requirementLink_isOmittedForACallerNotOfferedTheEvent` pins
      the half that flips: the condition is *caller dependent*, so the Negotiation's creator, who is
      offered nothing from this State, sees no hint for the very Requirement row the administrator
      is hinted at. `deltaB_requirementLink_isOmittedWhenNoTransitionLeavesTheCurrentState` pins the
      half ADR 0005 keeps, with an offered Event as a control in the same response so the assertion
      cannot pass by the assembler emitting nothing.
- [x] A test pins the current display name for those links.
      `deltaB_requirementLink_displayNameIsTheRawEventKey` pins `name` as `"CONTACT requirement"`,
      asserts it is *not* the Event's published label (read from the committed
      `resource-events.json`, not transcribed), and pins `title` as the Access Form's name read back
      from the Requirement row. `deltaB_submissionLink_isTitledFixedAndNamedAfterTheAccessForm`
      pins the Submission link's fixed `"Submitted Information"` title and its Access-Form `name`.
- [x] All intended-delta tests live in a separately named class whose name makes their purpose
      unmistakable. `IntendedDeltasAdr0005WillInvertTest`, alone in the new package
      `eu.bbmri_eric.negotiator.characterization.delta`, which carries a `package-info.java` saying
      the same thing at directory level.
- [x] Each carries a comment stating what the post-cutover behaviour should be and which ADR mandates
      the change. All eight methods have a javadoc naming ADR 0005 and the behaviour that replaces
      the assertion. The one method whose statement ADR 0005 *keeps*
      (`...isOmittedWhenNoTransitionLeavesTheCurrentState`) says so explicitly, so nobody deletes it
      on the assumption that everything in this class flips.
- [x] The intended-delta tests are excluded from, or separately reported by, the parity gate selector.
      Both, by the JUnit tag `intended-delta`, and both directions were run rather than reasoned
      about — see Verification. The tag string is the compile-time constant
      `IntendedDeltasAdr0005WillInvertTest.INTENDED_DELTA`, referenced by the `@Tag` annotation
      itself, so the annotation and the documented flags cannot drift.
- [x] A note records that these deltas force the two known frontend breakages. In the class javadoc,
      with file and line: `frontend/src/components/ResourceItem.vue:97` and `:103`. Finding 6.
- [x] Every State and Event is named as a string; the forbidden-import guard passes.
      `CharacterizationImportGuardTest` 3/3 green in the same runs, with the new package present —
      the guard walks the whole characterization tree from `Files.walk`, so the new sub-package
      needed no change to its scan. The new files name no Lifecycle enum and no Spring Statemachine
      type; every State and Event is a string literal and every service call goes through
      `LifecycleTestAdapter`.
- [x] No production code is modified. `git diff --stat` over `backend/src/main` is empty. The commit
      touches two new test files, one edited test helper, and markdown.

## Blocked by

- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
- [REST seam: metadata endpoints and the graph diagram endpoint](09-rest-seam-metadata-and-diagram.md)

## Outcome

**8 new tests in one new class**, `characterization/delta/IntendedDeltasAdr0005WillInvertTest`, plus
a `package-info.java` for the new package. `SeededResourceSubject` was widened — the class and the
six members the new package uses become public, and `submitInformationFor` now returns the new row's
id, which is what the per-row rel names are built from. No new helper was written: the Requirement
and Submission fixtures, the subject Resource and its callers are ticket 05's, unchanged.

The parity gate is **unchanged at 255 tests in 24 classes**, because the deltas are excluded from it.
Run without the exclusion the same selector is 263 in 25.

### Where the two deltas are pinned

| Delta | Production code | Pinned by |
|---|---|---|
| A — Possible Events include Events that cannot fire | `ResourceLifecycleServiceImpl.getPossibleEvents` (`:61-73`), which never consults the gate at `sendEvent` (`:110-115`) | `deltaA_possibleEvents_offerAnEventTheRequirementGateThenRefuses`, `deltaA_theResourceListing_advertisesTheUnfireableEvent` |
| B — rel naming | `ResourceWithStatusAssembler.addRequirementLink` (`:127-143`), `addSubmissionLink` (`:99-115`) | `deltaB_requirementLinks_...`, `deltaB_submissionLinks_useOneRelPerRowCarryingTheRowId` |
| B — inclusion condition | `addRequirementLink`'s `links.stream().anyMatch(rel == event)` (`:134-136`) | `deltaB_requirementLink_isOmittedForACallerNotOfferedTheEvent`, `...isOmittedWhenNoTransitionLeavesTheCurrentState` |
| B — display name | `withName(forResourceEvent + " requirement")` (`:141`) | `deltaB_requirementLink_displayNameIsTheRawEventKey`, `deltaB_submissionLink_isTitledFixedAndNamedAfterTheAccessForm` |

## Verification

All runs from the repository root. The Nix dev shell is not active by default in an agent session,
so `java`, `mvn` and `JAVA_HOME` are absent from `PATH` and every Maven command needs the prefix
below; `nix develop` prints a `warning: Git tree ... is dirty` line to stderr, which is not an error.

**The parity gate, with the deltas excluded — this is the command ticket 11 should carry forward:**

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
```

**255 tests in 24 classes, 0 failures, 0 errors, 1 intentional skip** — byte for byte the run ticket
08 left behind, and no surefire report is written for the delta class at all, which is the check that
the exclusion is real.

**The deltas alone, reported separately:**

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -Dgroups=intended-delta
```

**8 tests, 0 failures, 0 errors, 0 skipped**, and the only report written is the delta class's.

**The whole tree, deltas included, as the ordering check:**

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

**263 tests in 25 classes, 0 failures, 0 errors, 1 skipped.** The new class declares
`@DirtiesContext(AFTER_EACH_TEST_METHOD)` and writes `information_requirement` rows, whose lookup is
global; this run is the evidence that it turns nobody else red.

### Mutation check

The four assertions that are supposed to invert were each rewritten to the post-cutover expectation
and the class re-run: `.contains` → `.doesNotContain` for delta A, `requirement` present as an
array-valued rel, the display name equal to the Event's published label, and the creator seeing the
hint. **All four went red** (`Tests run: 8, Failures: 4`), then were reverted. The tests state
something about the system, and they will flip when the system changes.

## Findings

### 1. The frontend does not render the text ADR 0005 changes

ADR 0005 changes the requirement hint's **display name** — the HAL `name` attribute, today
`"CONTACT requirement"`. The frontend renders the hint from **`title`**:
`ResourceItem.vue:67` is `{{ link.title }} Required`, and `title` is the Access Form's name
(`"BBMRI Template"` on the seed). So *the display-name fix alone changes nothing a user sees for a
requirement hint.* It does change what a Submission link shows, because line 60 renders `link.name`
there.

Whoever implements ADR 0005 should decide whether the intended user-visible change belongs in `name`
or in `title`; the ADR names only `name`, and on today's frontend that is the invisible one. Both
texts are pinned, on both link kinds, so the decision is made against a before-picture.

### 2. The inclusion condition is caller-dependent, and ADR 0005 makes it caller-independent

The ticket and the ADR both describe the change as inclusion becoming *structural reachability*.
What that replaces is sharper than "a lifecycle link was already built": the condition is
`links.stream().anyMatch(link -> link.getRel().equals(dto.getForResourceEvent()))` evaluated after
`addLifecycleLink` has run, and `addLifecycleLink`'s source is `getPossibleEvents` — the Required
Authority filter included. **So today a Requirement hint is shown only to callers who could fire the
Event.** Pinned in both directions on the same Requirement row: the administrator sees
`requirement-<id>`, the Negotiation's creator does not.

Structural reachability is a property of the graph, so after the cutover the creator, who can never
fire `CONTACT`, will be told a form is required for it. That is a visibility change, not only an
inclusion change, and it is the one consequence of ADR 0005 that nothing in the ADR or the ticket
mentions. It may well be wanted — the Audience of a Requirement is a different question from the
Required Authority of the Event (ADR 0006) — but it should be a decision.

### 3. `getPossibleEvents` is the single cause of delta A on both surfaces

The assembler calls the same `resourceLifecycleService.getPossibleEvents` the Lifecycle endpoint
does. There is no second place that decides which Events to advertise. Fixing the listing inside the
Evaluation Pipeline therefore fixes the HAL links at the same time, and **no assembler change is
needed for delta A** — only for delta B. Recorded because "the assembler also advertises it" reads
like a second site to fix and is not one.

### 4. `"Next Lifecycle event"` is a load-bearing magic string

`ResourceItem.vue:108` selects lifecycle links with `link.title === 'Next Lifecycle event'`. It never
looks at the rel. So the cutover may rename or restructure lifecycle rels freely, but rewording that
title silently removes every "Update status" control from the Negotiation page. Nothing pinned it
before; `deltaA_theResourceListing_advertisesTheUnfireableEvent` now does.

### 5. Three things in the assembler that are not deltas and should not be carried forward

Read from source, deliberately not pinned — reaching any of them needs corrupted data or concurrency,
and a test that manufactured either would be pinning the fixture rather than the system.

- **`requirementsCache` is a `static` field on a singleton `@Component`**
  (`ResourceWithStatusAssembler.java:36`), reassigned at the top of every `toModel` call, while
  `submittedInformationCache` next to it is an instance field reassigned the same way. Two concurrent
  requests interleave: request B's `addRequirementLink` can read the list request A just wrote. Both
  caches are also per-*row* state held on a shared bean, refreshed once per Resource rendered, so a
  collection of N Resources issues 2N service calls.
- **Both link loops swallow every exception around the whole loop.** `addSubmissionLinks` (`:89-97`)
  and `addRequirementLinks` (`:117-125`) each wrap their `for` in `catch (Exception e) { log.error }`,
  so one bad row drops every *remaining* link silently and the response is still 200.
  `addLifecycleLink` (`:145-156`) has the same shape, so a failure inside `getPossibleEvents` renders
  a Resource with no lifecycle controls and no error.
- **`addSubmissionLink` reaches into the requirement cache with `.findFirst().get()`** (`:102-108`) —
  an `Optional.get()` that throws `NoSuchElementException` if a Submission outlives its Requirement,
  which the catch above then converts into "no submission links on this Resource".

### 6. The two frontend breakages, cited

Both are delta B's rel collapse, both in `frontend/src/components/ResourceItem.vue`:

| Line | Code | Breaks how |
|---|---|---|
| 97 | `Object.entries(props.resource._links).filter(([key]) => key.startsWith('submission-'))` | an array-valued `submission` rel has no trailing hyphen, so the filter matches nothing |
| 103 | the same over `'requirement-'` | same |

Repairing the prefix is not sufficient. Both filters end `.map(([, value]) => value)` and the results
are consumed as single link objects: `link.href` at lines 59 and 66, `link.title` at line 67,
`link.name` at line 60, and `getSubmissionLinks.value.forEach(... element.href ...)` in the
`onMounted` hook at lines 115-121. Each becomes an iteration over an array of links.

**Delta A breaks nothing in the frontend.** `getLifecycleLinks` (line 108) filters lifecycle links by
title and renders one control per link, so offering fewer Events renders fewer controls — which is
the fix working. Standing decision 5 assigns the delta B repairs to whichever slab lands ADR 0005.

### 7. Two Resource identifiers meet in one method

`addSubmissionLink` matches a Submission to a Resource with `info.getResourceId().equals(entity.getId())`
— database **row ids** — while `addLifecycleLink` four lines away keys on `entity.getSourceId()`,
because every Lifecycle surface does. The assembler is therefore one of the few places both
identifiers are live at once, which is worth knowing for any ADR 0002 rewrite of it: the two are not
interchangeable and the seed makes them look similar (row 4 ↔ `biobank:1:collection:1`).

### 8. Submission links have no inclusion condition at all

Every Submission of the Negotiation belonging to this Resource is linked, whatever State the Resource
is in and whoever is asking. ADR 0005 collapses the *rels* for both kinds but says nothing about
changing the submission inclusion condition — worth stating, because "inclusion becomes structural
reachability" reads as if it applied to both, and applying it to Submissions would hide forms people
have already filled in.

### 9. The tag mechanism, and why it was chosen over a separate selector

`@Tag("intended-delta")` on the class; `-DexcludedGroups=intended-delta` and
`-Dgroups=intended-delta` are plain `maven-surefire-plugin` user properties (3.5.5, no POM change
needed) and compose with `-Dtest=` rather than replacing it. Both were run, and the check is the set
of surefire reports written, not the pass count: with the exclusion no report for the delta class
exists; with `-Dgroups` no report for anything else does.

The alternative — moving the class outside `eu.bbmri_eric.negotiator.characterization.**` so the
existing selector misses it — was rejected: the forbidden-reference guard resolves its scan root from
`src/test/java/eu/bbmri_eric/negotiator/characterization`, so a class outside that tree would silently
escape the string-and-adapter rule, which is exactly the property these tests need most. Inside the
tree, in its own package, tagged, the guard covers them and the gate does not.

### 10. Corpus facts this ticket relied on

`negotiation-1` has exactly one Resource (row 4, `biobank:1:collection:1`), seeded `SUBMITTED` under
an `IN_PROGRESS` Negotiation. From `SUBMITTED` the graph offers `CONTACT` and `MARK_AS_UNREACHABLE`,
both `isAdmin`; the Negotiation's creator 108 is offered nothing there but still has read access to
the listing as its creator, and administrator 101 has read access as an admin. That pair is what
makes the caller-dependence of the inclusion condition observable without moving any Resource.
`GRANT_ACCESS_TO_RESOURCE` carries a Transition (out of `ACCESS_CONDITIONS_MET`) but none out of
`SUBMITTED`, which is what the unreachable-Event arm needs. `min(id)` of `access_form` is 1,
`'BBMRI Template'`, from `V3__add_dynamic_access_forms.sql` rather than the test seed — the test
reads the name back rather than transcribing it.

### 11. Nothing here contradicts the PRD or STATUS

PRD story 23 and the PRD's "Intended deltas, not parity" section describe both deltas accurately, and
ADR 0005's own paragraph on the three assembler fixes matches the code line for line. Ticket 10's
prose is accurate too. The findings above are additions, not corrections. This is the first ticket in
the slab with nothing to correct upward, which is worth saying explicitly given that the PRD has been
wrong twice.
