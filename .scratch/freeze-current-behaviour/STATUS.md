# Slab status — freeze current behaviour

Snapshot taken when the session stopped. Update or delete this file once the slab completes.

## Landed on `feat/state-machine-implementation`

| Ticket | State | Evidence |
|---|---|---|
| 01 graph dump generator | **done** | dump + drift test green |
| 02 string adapter + import guard | **done** | guard demonstrated failing on a real violation, then reverted |
| 09 REST seam | **done** | 26 tests green after one cross-ticket fix (see below) |
| 03 Negotiation parity | **done** | 63 tests green; all 12 criteria re-verified against surefire output |
| 04 Resource parity | **done** | 52 tests green; all 14 criteria re-verified against surefire output |
| 05 Information Requirement gate | **done** | 11 tests green; all criteria re-verified against surefire output |
| 06 Post side effects | **done** | 21 tests green; all criteria re-verified against surefire output |
| 07 Lifecycle history rows | **done** | 37 tests green; all criteria re-verified against surefire output |
| 08 Event seam, spawn, conclusion, handlers | **done** | 29 tests green; all 16 criteria re-verified against surefire output |

Parity gate as it stands:

```
/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh -f backend 'eu.bbmri_eric.negotiator.characterization.**'
```

**255 tests in 24 classes, 0 failures, 0 errors, 1 intentional skip** (ticket 01's opt-in generator)
as of ticket 08; the full run takes about 8.5 minutes. It was 226 after 07, 189 after 06, 168 after
05, 157 before it, and 158
when 04 landed; a follow-up review of `26da8c45` and `fd7d0385` deleted one strictly weaker duplicate
test — `NegotiationDraftReachabilityTest.approved_isDeclaredButNeverEntered`, whose statement
`NegotiationGraphV1BindingTest.legacyState_isDeclaredButUnusedInTheDump` already makes against the
mechanical dump. No assertion about the system was weakened. There is **no `scripts/test-backend.sh`
at the repository root** — the early tickets recorded that path and it exits 127; the script ships
with the `focused-backend-tests` skill.

Ticket 04's WIP branch (`worktree-agent-a3dab4653dd6c2484`, commit `7f0226c8`) was cherry-picked, made
to compile, and then reworked — its hand-transcribed graph tables are now bound to the committed dump
by a new `ResourceGraphV1BindingTest`, following ticket 03's finding 3. The branch is spent.

## Conventions the later tickets inherit from the follow-up review

- **Expected offerings are computed from the pinned graph table, never typed out.** The table is
  bound edge for edge to the committed mechanical dump, which the drift test regenerates from the
  live beans, so a computed expectation is anchored to an artifact nobody transcribed — and it stays
  complete if the graph is larger than the rows anyone remembered to type. Reasoning in ticket 03's
  follow-up section.
- **Three shared helpers now exist in the characterization tree, and a fourth was extended.**
  `service/SeededResourceSubject` (the seeded Resource, its three callers, the link-row SQL and the
  hand-rolled authentication), `service/LifecyclePersistence` (the one bounded post-send wait and
  the one `PERSIST_TIMEOUT`), and `rest/CanonicalJson` (now also the suite's one reader of committed
  artifacts: `artifact`, `namesIn`, `publishedValues`). A ticket that needs any of these should use
  them rather than write a fourth copy. Ticket 05 extended `SeededResourceSubject` again, with
  `requireInformationFor`, `submitInformationFor` and `ANOTHER_RESOURCE_ROW_ID` — Information
  Requirement and Submission fixtures are inserted as SQL, not JPA, because
  `InformationRequirement.forEvent` is one of the four deleted enums and an entity-level fixture
  could not avoid naming it. Ticket 06 added `service/SeededNegotiationSubject` — the Negotiation-side
  counterpart, carrying the subjects, callers, State and post-flag SQL, and the post and
  creation-date readers — and generalised the waits into `LifecyclePersistence.awaitValue` /
  `awaitValueAfterSettling`, which take any observable rather than just a State. Ticket 07 added
  `service/LifecycleHistory`, the suite's only reader of the two Record tables: it is the single
  place that knows the State comes out of `changed_to`, and it resolves `resource_id` to `source_id`
  so no row id leaks into an assertion. Same shape of argument as the adapter — one cutover point.
  Ticket 08 added two more of the same shape: `service/StateChangeEvents`, the suite's only reader of
  the two state-change events, and `service/HandlerNotifications`, its only reader of the
  `notification` table. It also extended the adapter with `overrideResourceStates`, so the suite can
  reach the Override producer without naming a State enum, and `CanonicalJson` with
  `publishedLabels`.
- **Handlers are observed by their `notification` rows, not by `NewNotificationEvent`.** The trigger
  is a recorded application event as the PRD requires, but the *effect* is read from the table:
  `NewNotificationEvent` is published on the async dispatcher's thread, where
  `@RecordApplicationEvents`' inheritable thread-local is unreliable. No SMTP anywhere. (Ticket 08.)
- **Corpus facts for anyone driving spawn or conclusion.** `negotiation-5` is the only seeded
  Negotiation still holding stateless Resources (rows 5 and 7, representatives 109 and 105, creator
  108), so it is the only usable spawn subject. Resource row 10 (`biobank:3:collection:4`) is the
  only Resource with no representative, and is linked to nothing. `negotiation-1` has exactly one
  Resource, so any change that counts toward the terminal predicate concludes it — ticket 08's
  Resource-side walks attach a second Resource in a non-counting State purely as a brake.

## Not started

10 (intended deltas) and 11 (parity gate + findings) — all that remains.

10 needs 04 and 09, both long done, so it is unblocked. 11 needs everything, including 10.

**Before 10 and 11 run, read ticket 08's findings 1 and 2 below.** Both contradict documents the
later slabs are written against: PRD story 13 and ticket 08's own description of spawn are wrong
about which State a spawned Resource starts in, and about spawn announcing itself at all. 11's
findings report owes an explicit correction of the PRD, not a silent one.

## Findings so far

Recorded in full in the individual ticket files. The load-bearing ones:

- **`NegotiationIsApprovedGuard` is dead code, and ticket 04 took path two.** All 21 Transitions
  across both graphs dump `"guard": null`; the `withExternal().guard(...)` fragment produces no
  Transition at all. Verified not to be an unwrap failure in disguise. What is pinned instead is the
  imperative gate at `ResourceLifecycleServiceImpl.java:143-145` — no Resource Event is offered
  unless the parent Negotiation is IN_PROGRESS — walked across all 8 Negotiation States by ticket
  04. Reimplementing the Guard in ADR 0002's registry would introduce a check that has never fired.
- **`secured(..., ComparisonType.ALL)` is silently `ANY`.** SSM 4.0's
  `AbstractTransitionConfigurer.setSecurityRule` ignores its `ComparisonType` argument. Harmless
  today because every rule carries exactly one attribute; ADR 0002's Required Authority model should
  be built against the behaviour, not the builder chain.
- **Both graphs declare more States than the PRD hand-counted**, because the configs register the
  whole enum via `EnumSet.allOf`. `APPROVED` (Negotiation, 8 States not 6) and
  `RETURNED_FOR_RESUBMISSION` (Resource) are Legacy States: declared, no Transition leads to them.
  ADR 0009's seed must carry them or existing rows naming them stop resolving.
- **`DRAFT` is occupied but not enterable.** `negotiation-6` is seeded in `DRAFT`, yet the initial
  State is `SUBMITTED` and no Transition targets `DRAFT`. "Occupied" and "reachable" are different
  questions and the seed must reproduce both. Confirmed at the service seam by ticket 03, which also
  read the occupied set straight out of the table: exactly
  `{DRAFT, SUBMITTED, IN_PROGRESS, ABANDONED}`, so `APPROVED` has no live occupant at all.
- **Both Event universes are wider than their dump.** The Negotiation dump's `events` array lists
  only the seven Events that trigger a Transition; `START` is declared, published by the metadata
  endpoint and nameable by a caller, but carries no Transition (ticket 03). The Resource graph has
  two such Events, `RETURN_FOR_RESUBMISSION` and `OVERRIDE` — and `OVERRIDE`, whose published
  description is "Override current state, ignoring state machine guards", carries no Transition and
  is therefore refused at the Lifecycle seam as silently as any other unoffered Event. That is a
  statement about `ResourceLifecycleService.sendEvent` only: an out-of-Lifecycle override path does
  exist elsewhere, and stamps the same Event name — see the next finding. All three names are
  candidate Override Events in the new model, not names to drop. (Ticket 04.)
- **`ResourceStateChangeEvent` has a second producer, and nothing pins it.**
  `ResourceServiceImpl.updateResourceStatus`
  (`governance/resource/ResourceServiceImpl.java:178-194`) writes an arbitrary State straight onto
  the link row via `negotiation.setStateForResource(...)` — no Transition, no Required Authority
  rule of the graph, no IN_PROGRESS gate — and publishes a `ResourceStateChangeEvent` stamped
  `OVERRIDE`. Reachable from `PATCH /negotiations/{id}/resources`
  (`NegotiationController.java:297-304`), gated by admin-or-representative-of-every-Resource. The
  PRD's event seam (stories 13 and 19) and ticket 08 both assume that event traces a Transition; it
  does not always. Out of scope for ticket 04, deliberately left unpinned, and flagged as input in
  ticket 08's own file. (Ticket 04, finding 10.)
- **Five dead branches, and one shape behind all of them.** Alongside `NegotiationIsApprovedGuard`:
  `NegotiationLifecycleServiceImpl`'s `catch (ClassCastException)` refusal (ticket 03), and all
  three advertised sharp edges of `ResourceLifecycleServiceImpl.isSecurityRuleMet` — absent
  `Authentication` counting as admin, `catch (ClassCastException) → false`, and
  `catch (NullPointerException) → creatorId = 0L` (ticket 04). Every one sits after a call to
  `AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId()`, which wraps *every* failure
  in `AuthenticationCredentialsNotFoundException`, so nothing downstream ever sees the exception it
  was written to catch. Defensive branches written around a helper that already normalises its
  failures. None should be carried into the new evaluator. Ticket 04's criterion 9 was reworded to
  pin the escaping exception instead, which is what a caller actually observes.
- **The Resource service is silent only when it has something to be silent about.** Its refusal
  returns `getCurrentStateForResource(...)`, which `orElseThrow`s — so a refused Event on a Resource
  with no recorded State (blank link row or no link row) raises `EntityNotFoundException` carrying
  the *Negotiation's* id. The read path swallows the same lookup failure into `Set.of()`. "The
  Resource service never throws" is the summary everyone will carry into the redesign and it is
  false. (Ticket 04.)
- **Required Authority is exactly one rule per Resource Transition.** 3 `isAdmin`,
  8 `isRepresentative`, 2 `isCreator`, none unsecured, none with two attributes. The delivery chain
  from `SUBMITTED` to `RESOURCE_MADE_AVAILABLE` therefore requires three distinct *rules* — but not
  three distinct people: nothing partitions callers, since `isAdmin` is an authority on the token,
  `isRepresentative` a link row and `isCreator` a column of the Negotiation, and one Person can hold
  all three. The delivery walk changes identity only because seeded callers 101, 109 and 108 happen
  to be disjoint. `isAdmin` means the `ROLE_ADMIN` authority on the token, not the `admin` column of
  the Person row; and representing a Resource is scoped to that Resource, not to its Negotiation —
  the mirror of ticket 03's finding that representing a Resource confers nothing over the
  Negotiation. (Ticket 04.)
- **A hand-transcribed graph table must be bound to the dump.** Ticket 03 shipped its parity table
  as a transcription; two of its findings were assertions over that constant and could not fail.
  `NegotiationGraphV1BindingTest` now equates the table to the committed dump and to the committed
  Event metadata. Ticket 04 found its inherited WIP in exactly the same state and added
  `ResourceGraphV1BindingTest`, and takes the Negotiation State universe from
  `NegotiationGraphV1.allStateNames()` rather than transcribing it a second time. Any later ticket
  adding such a table owes the same binding.
- **The diagram endpoint has no visited set.** It terminates only because the Resource Lifecycle is
  acyclic; adding any cycle produces `StackOverflowError`, not a larger response. 13 Transitions
  render as 29 nodes, nesting 14 deep. A reimplementation needs a visited set or a depth bound.
- **The unrecognised-name failure is two failure modes.** State path variables bind straight to the
  enum and return a JSON problem detail; Event path variables have `Converter`s that swallow the
  exception and return an empty body. The State detail leaks the fully-qualified name of a class
  ADR 0002 deletes — see below.
- **`ResourceStateMetadataDto` alone carries an `ordinal` field** (0–11, the enum's declaration
  order, documented as significant). Relational configuration must reproduce it.
- **The Information Requirement gate outranks everything, not just availability.** It is the first
  statement of `ResourceLifecycleServiceImpl.sendEvent` (`:110-115`), before the Resource's State is
  even read, so it outranks the parent-Negotiation IN_PROGRESS gate, the Required Authority rules,
  and "this Event has no Transition anywhere". Consequences ADR 0005's Built-in Stage inherits: a
  Requirement recorded for `OVERRIDE` or `RETURN_FOR_RESUBMISSION` turns those silent no-ops into
  user-facing errors, and an unauthorised caller gets the Requirement error rather than the silent
  refusal — which leaks that a Requirement exists. (Ticket 05.)
- **The gate's refusal is a Spring *data access* exception.** `StateMachineException` extends
  `org.springframework.dao.NonTransientDataAccessException`, so any `catch (DataAccessException)` on
  the path would swallow it. That is the strongest argument for changing the type at cutover rather
  than renaming it. It surfaces via `NegotiatorExceptionHandler.java:441-447` as 400 with title
  `"Could not advance the state machine"` and the message as `detail`. Ticket 05 pins the observable
  half — message text, unchecked, causeless, distinct from the service's own refusal types, plus the
  HTTP shape — and **documents the type name rather than asserting it**, per ticket 09's rule. The
  title is engine-flavoured prose, so rewording it at cutover reddens that test by design.
- **The gate's requirement lookup is global.** `existsByForEvent(event)` is scoped by nothing else:
  one `information_requirement` row blocks that Event for every Resource of every Negotiation. This
  gives `@DirtiesContext` a second load-bearing role beyond the ordering rule — a leaked fixture row
  would block that Event for the remainder of the run. Note also that the correctly scoped query
  already exists (`existsByResource_SourceIdAndNegotiation_IdAndRequirement_Id`, used at
  `InformationSubmissionServiceImpl.java:76`) and is deliberately not called by the gate, so the
  unscoped check is one method call away from being "fixed" by accident at cutover. (Ticket 05.)
- **The two abandon routes really are not equivalent, and the ticket's account of all three post
  Actions is accurate.** Confirmed against the dump and then fired for real: `DRAFT→SUBMIT` enables
  public posts, `SUBMITTED→APPROVE` enables private, `IN_PROGRESS→ABANDON` disables both, and the
  other five Transitions carry no Action — `(true,true)` survives an abandon from `PAUSED` intact.
  Action bean class names live in exactly one place, `EFFECT_OF_ACTION` in
  `NegotiationGraphV1BindingTest`, which reads the frozen dump; every behavioural assertion speaks
  only of a `PostEffect` enum, so ADR 0002 re-registering the beans reddens nothing. (Ticket 06.)
- **The message-borne post is not an Action and must not be registered as one.** It is written
  unconditionally on the same path as the new State, so it appears on Transitions with and without
  Actions — pinned across `APPROVE`, `DECLINE` and `ABANDON`-from-`PAUSED`. Registering it
  per-Transition in ADR 0002 would be wrong in both directions. The emptiness check is `isEmpty`,
  not `isBlank`: a single-space message creates a post. The author is
  `personRepository.findById(...).orElse(null)`, so a principal with no Person row would yield an
  authorless post rather than a refusal — read from source, not reachable through the Lifecycle
  seam, documented rather than pinned. (Ticket 06.)
- **Effects run ahead of the State being written.** Confirmed across 16 fired arms: awaiting the
  target State is sufficient to observe an Action's effect, and only "nothing moved" claims need a
  settling period. Later tickets should not add a second wait per observable. (Ticket 06.)
- **Seeded post flags make a naive walk vacuous.** `negotiation-2` and `negotiation-6` are seeded
  public-enabled, so a test that fires `SUBMIT` and asserts "public enabled" can pass without the
  Action running. Ticket 06 writes a known flag setting first and fires each Transition twice, from
  `(false,false)` and from `(true,true)`. Corpus facts worth reusing: `negotiation-2` has zero
  seeded posts and zero Resources (all seeded posts are on `negotiation-1`), and `negotiation-6` is
  the only `DRAFT` row, with a creation date in the past.
- **The creation-date reset keys on the State arrived in, not the Event.** Not separable today,
  because exactly one Transition targets `SUBMITTED`; if ADR 0009's seed adds a second, this becomes
  two distinct behaviours. (Ticket 06.)
- **A Record captures the destination State only** — not the origin, not the Event. Settled by
  firing: `ABANDON` into `ABANDONED` from `IN_PROGRESS` versus from `PAUSED`, and `CONTACT` into
  `REPRESENTATIVE_CONTACTED` from `SUBMITTED` versus from `REPRESENTATIVE_UNREACHABLE`, leave rows
  that are *equal* once id and timestamps are stripped. Row order is therefore load-bearing and the
  only ordering key is the identity `id`; `creation_date` agreed with it on every path pinned, but
  nothing enforces that. The first row of a trail has no recoverable origin at all. This matches
  ADR 0008's own deferrals — the ADR is accurate, and now tested. (Ticket 07.)
- **The trail records State *assignments*, not Transitions.** Rows are written by
  `Negotiation.setCurrentState` / `setStateForResource`, i.e. by the entity on any write. Three
  producers no Transition accounts for: spawn (`ResourceNotificationService`, writing
  `REPRESENTATIVE_CONTACTED` / `REPRESENTATIVE_UNREACHABLE`), the Override path
  (`ResourceServiceImpl.updateResourceStatus`), and automatic conclusion — which runs through
  `runAsSystemUser`, so that Negotiation Record is attributed to **Person 0**, not to the caller
  whose Resource Event triggered it. Ticket 07 documented these rather than pinning them; they are
  ticket 08's seam. (Ticket 07.)
- **`buildResourceStateChangeRecord` silently drops `SUBMITTED`** (`Negotiation.java:226`).
  Unreachable through the Lifecycle seam — no Resource Transition targets `SUBMITTED` and spawn does
  not use it — so only an Override to `SUBMITTED` reaches it. A reimplementation that drops the
  special case would start writing rows this trail has never contained. Documented, not pinned.
- **Records are not deduplicated and must not become so.** Both collections are `HashSet`s and
  neither Record type overrides `equals`. Giving a Record value-based equality after ADR 0008's FK
  conversion — `state_id` + `negotiation_id` looks like a natural key — would silently lose every
  revisit. Pinned by driving a path through `IN_PROGRESS` twice. (Ticket 07.)
- **Every recorded State name is one the Definition declares, Legacy States included** — pinned over
  the whole table in both graphs. That is the precondition ADR 0009's backfill rests on.
- **A third place the State universe is written down.** Both Record tables carry a `CHECK` constraint
  enumerating State names (`negotiation_lifecycle_record_changed_to_check`, 8 names after `V22.0`;
  `negotiation_resource_changed_to_check`, 12 names). ADR 0009's conversion has to drop them.
  Recorded as a before-picture rather than asserted, per ticket 09's rule.
- **Corpus trap on the Resource history side.** The seeded Record for the subject Resource already
  names `REPRESENTATIVE_CONTACTED` — the exact target of the obvious first Transition — so a wait on
  "the last row names X" passes *before* the send. Ticket 07's Resource waits are all on row count.
  `negotiation-2` has no Negotiation Record, which is what makes the Negotiation half baseline-free.
- **Spawn does not use the Definition's initial State.** It writes `REPRESENTATIVE_CONTACTED` or
  `REPRESENTATIVE_UNREACHABLE`, never the graph's initial `SUBMITTED`. **PRD story 13 and ticket 08's
  own description are both wrong on this**, and ADR 0007's `SPAWN_RESOURCE_LIFECYCLES` Action must be
  written against the observed behaviour: starting a spawned Resource at `SUBMITTED` would re-offer
  representatives Events that are admin-only from there. Pinned as observed, with
  `spawn_doesNotUseTheGraphsInitialState` stating the divergence outright. (Ticket 08.)
- **Spawn publishes no `ResourceStateChangeEvent` at all.** Three Resources change State and nothing
  is announced — neither the Resource-state-change handler, nor the conclusion listener, nor the
  webhook subsystem hears it. If ADR 0007 routes spawn through ordinary Transition machinery it
  starts emitting events to consumers nobody has counted. (Ticket 08.)
- **The Negotiation-side handlers and spawn all key on the destination State, not the Event.**
  `PAUSED --UNPAUSE--> IN_PROGRESS` spawns exactly as `APPROVE` does, and the two `ABANDON`
  Transitions — *not* equivalent in ticket 06's post effects — are indistinguishable here. (Ticket 08.)
- **The conclusion predicate, empirically.** Counts toward concluding: `RESOURCE_MADE_AVAILABLE` and
  `RESOURCE_UNAVAILABLE`, and nothing else. The other ten declared Resource States do not, including
  `RESOURCE_NOT_MADE_AVAILABLE` — where a researcher's own refusal lands — so such a Negotiation
  stays `IN_PROGRESS` for ever. Walked twice: every Transition, and every declared State via the
  Override path. This settles PRD story 15 against observation rather than the names' apparent
  meaning. (Ticket 08.)
- **The double-annotated conclusion listener runs exactly once.** `EventListenerMethodProcessor`
  takes the first supporting `EventListenerFactory` and breaks, so the `@TransactionalEventListener`
  wins and the plain `@EventListener` never produces a listener at all. Deleting `@EventListener` is
  a no-op; deleting `@TransactionalEventListener` is not. The invocation count itself is
  **documented, not test-enforced** — a spy would have named `ResourceStateChangeListener`, a class
  the redesign deletes; what is enforced is the observable consequence, one published `CONCLUDE` and
  one Record. (Ticket 08.)
- **No handler is reachable by an event published outside a transaction — and the scheduled pending
  reminder is published outside one.** `NotificationListener.onNewEvent` is the single dispatcher for
  all eight strategies and is a `@TransactionalEventListener` with default fallback;
  `NotificationScheduler.forPendingNegotiations` is a plain `@Scheduled` method. On this evidence the
  daily reminder has never reached a representative in production. Pinned both ways in
  `noHandlerIsReached_whenTheEventIsPublishedOutsideATransaction`. (Ticket 08.)
- **`ResourceStateChangeHandler` has no firing condition** — it fires on every published Resource
  state change, from either producer. Its lifecycle dependence is entirely in its *content*: the body
  is built from the two States' labels, so the expected body is computed from the committed
  `resource-states.json`. `PendingNegotiationReminderHandler`, for its part, ignores the
  Negotiation's own State: an `ABANDONED` Negotiation whose Resource is still
  `REPRESENTATIVE_CONTACTED` keeps reminding its representatives. (Ticket 08.)
- **The administrators the submission handler notifies are the `admin` column, not `ROLE_ADMIN`.**
  Person 0 — the system user automatic conclusion runs as — has `admin = false`, so it is never
  notified of submissions while being treated as admin by every role check. Two notification titles
  are also shared with non-lifecycle-keyed handlers: `"New Request"` with `NewNegotiationHandler`,
  `"New Negotiation Request"` with `UpdatedResourcesHandler`. (Ticket 08.)

## The Override producer is now pinned, not scoped out

Ticket 04 found it and left it; ticket 08 decided to pin it, and the reasoning is worth keeping.

Two of the twelve declared Resource States have no incoming Transition — the initial State and Legacy
`RETURNED_FOR_RESUBMISSION` — so a Lifecycle-only walk leaves holes in exactly the table ADR 0007's
aggregation Guard is configured from. A lifecycle-keyed handler *does* fire on an Override-stamped
event: `ResourceStateChangeHandler` and the conclusion listener behave identically for both
producers. And the one pre-existing test of automatic conclusion drives this path rather than the
Lifecycle. The adapter grew `overrideResourceStates` so the suite reaches it without naming a State
enum.

Two consequences found by pinning it:

- **The Override path also spawns.** Updating Resources on an `IN_PROGRESS` Negotiation publishes
  `NewResourcesAddedEvent`, which initialises any stateless Resource exactly as arriving at
  `IN_PROGRESS` does.
- **It is silent in exactly one case**: writing the initial State onto a link row that already has
  one writes nothing and publishes nothing. Making the override uniform would start publishing an
  event it has never published. This is also why ticket 07's `SUBMITTED`-drop finding stayed
  untested — the only path to it writes nothing.

Deliberately still unpinned, as the governance service's seam rather than the Lifecycle's: the
override's own authorisation rule, its DRAFT branch, and its `NewResourcesAddedEvent` branch.

## One cross-ticket fix worth knowing about

Ticket 09 originally asserted the unrecognised-State response body verbatim. The forbidden-reference
guard from ticket 02 rejected it, correctly: the body contains
`...NegotiationState.NOT_A_STATE`, naming an enum the redesign deletes, so the assertion could only
ever go red at cutover — a guaranteed delta dressed as parity. The test now pins status, content
type, `title`, `status`, and that `detail` is *derived from* the rejected name; the verbatim text is
recorded in ticket 09's findings as the before-picture.

This is the guard catching something neither agent could see alone, which is the argument for having
built it.

## The ordering rule every later ticket inherits

Established by ticket 03, honoured by ticket 04, and binding on 05–08, which all drive Lifecycles:

**Any characterization class that fires an Event must declare
`@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`.** The Flyway strategy is clean-and-migrate on
every context build, so dirtying after each method restores the seed for whoever runs next. The
corpus is shared: `NegotiationAuthorityParityTest` reads `negotiation-1` expecting IN_PROGRESS, and
driving `negotiation-1`'s only Resource to a terminal State concludes that Negotiation. A driving
class that does not dirty turns another ticket red by test ordering alone, which no single class's
own run would reveal. Read-only classes may use `BEFORE_CLASS` or `AFTER_CLASS`. The full-selector
run is the check.

## Environment notes

- Testcontainers needs docker group membership; the images (`postgres:16-alpine`,
  `testcontainers/ryuk:0.12.0`) are already pulled locally.
- `backend/target/` gets polluted by the JDT language server compiling without Lombok, which
  presents as ~200 bogus `cannot find symbol` errors in unrelated test files (and matching spurious
  LSP diagnostics). A single `clean` clears it. Agent worktrees have their own `target/` and are
  unaffected.
- Agent worktrees are branched from `master`, not from the slab branch. Every agent brief must tell
  the agent to check and `git reset --hard feat/state-machine-implementation` if needed.

## Settled: the `minimal-workflow` profile is out of scope

`master` has `db9019d4`, a `minimal-workflow` Spring profile whose
`SimplifiedResourceStateMachineConfig` registers the `resourceStateMachine` bean with five
Transitions. It is not on this branch, and the dump and parity suite describe the default graph only.

**That is deliberate and needs no follow-up.** The profile exists only because workflows are not yet
customizable, and it declares no new State, so ADR 0009's migration is unaffected wherever it is
deployed. Customizable workflows land during the rollout and replace it. No ticket in this slab, and
no later slab, owes anything to this profile — later sessions should read the absence of coverage as
intended rather than as a gap.
