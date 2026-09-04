# Prototype the compiled-graph package

Type: prototype
Status: resolved
Blocked by: 08

## Question

Prototype [09 Transition Evaluator core](09-transition-evaluator-core.md) to later inform the PRD.
Code first.

## Instructions

- Create a new graph package. It should own the not concretely defined, but in several documents
  mentioned "compiled-graph" the evaluator evaluates on.
- The graph is not assembled by JPA entities, it has its own types.
- `RequiredAuthority` moves to the graph package.
- The "compilation" (might need another word) should happen in the definition package.
- `DefinitionScope` stays in the definition package.

For decisions not covered here, stick close to issue 9 and the ADRs.

While implementing the prototype, judgement calls may need to be made. All of them should be
recorded in a separate artifact for further analysis.

Use the `/codebase-design` skill in planning.

Do everything in a local git worktree, and don't check out other worktrees or workspaces.

Plan first, then implement.

## Prior art — read before starting, do not re-derive

- ~~`recon-strategies.md`~~ — **gone with its branch, deliberately.** It was a read-only inventory
  of the Guard and Action strategies with `file:line` citations. Prototype B audited all ten of its
  sections against the code while porting the strategies, found one thing that does not transfer
  (§8's bean pattern), and carried everything else it needed into its own javadoc. What this slab
  needs from it now lives in prototype B's tree, in `before-picture-findings.md`, and in
  [the PRD](../../transition-evaluator-core/PRD.md). One claim in its §9 was the seed of the layout
  this ticket superseded — that the evaluator cannot express itself without `DefinitionScope` — and
  both prototypes disproved it.
- **[before-picture-findings.md](../before-picture-findings.md)** parts 3 and 7.
- ADRs [0001](../../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md),
  [0002](../../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md),
  [0003](../../../backend/docs/adr/0003-definition-versioning-and-identity.md),
  [0005](../../../backend/docs/adr/0005-information-requirements-gate-transitions-as-a-built-in-stage.md).

## This supersedes a package layout already chosen elsewhere

*(Written while it existed; kept because it is why this ticket says what it says.)* Branch
`slice-01-vocabulary-move` was three commits ahead of `feat/state-machine-implementation` and had
already begun slab 09 on a different layout — a 474-line PRD, ten commit-sized slices, and one
landed commit moving **both** `DefinitionScope` **and** `RequiredAuthority` up to
`eu.bbmri_eric.negotiator.lifecycle`, with the evaluator in a new `lifecycle.evaluation` and no graph
package. None of it was merged. The instructions above contradict it on two of the five points and
add a package it does not have; they are the later word.

Left open for whoever claims this: whether that branch's slice 01 is rebased, redone or abandoned.
**Answered below: abandoned, and the branch has since been deleted.**
Note also that it amended issue 09's own text with a `## Progress` section and five corrections —
those are about the code, not the packaging. **They are now carried in slab 09's PRD, D14**, and
quoted in this Answer.

`DefinitionInertnessGuardTest.java:87-88` lists both `DefinitionScope` and `RequiredAuthority` among
the 14 names no `src/main` file outside `definition` may mention. Moving `RequiredAuthority` out
needs that list amended; `DefinitionScope` staying means its entry stands.

## Out of scope

Everything issue 09 already excludes, plus **the PRD itself** — this issue produces the code and the
judgement-call artifact to write it from.

## Answer

**Resolved 2026-09-04.** Two sessions prototyped this ticket in parallel and neither resolution
stands alone. This Answer consolidates three divergent versions of this file and supersedes all of
them.

### What it supersedes

- On `proto/evaluator-a` this ticket is `Status: resolved` with an Answer that speaks of "the
  prototype", singular — written before that session knew B existed. It is accurate about A's own
  tree and **stands only as a record of A's work**, not as this ticket's resolution.
- On `proto/evaluator-b` it is `Status: claimed`, with a Progress section recording both prototypes
  and the sentence "Not resolved by either session alone." That reading is the correct one; this
  Answer closes it.
- Neither prototype branch is merged. The analysis this Answer rests on is
  [the comparison](../notes/evaluator-prototype-comparison.md) — both trees read in full, every number
  produced by running the same command against both branches — and
  [taking A's good ideas into B](../notes/evaluator-a-into-b.md), which treats B as the base and asks of
  each of A's ideas whether it grafts. Both live in [`notes/`](../notes/) at the effort level rather
  than under this ticket or the slab, because they outlive both — the same placement, and the same
  reason, as `parity-gate.md` and `before-picture-findings.md`.

### The measured figures, which are not the self-reported ones

B's Progress section on its own branch and its `findings.md:29` both claim "21 production files, 100
pure tests in six classes". Measured: **28 production files** (27 new plus `RequiredAuthority`
moved) and **98 tests** in those six classes — 23 + 19 + 8 + 21 + 23 + 4 in the surefire reports,
reproduced by re-running them (comparison §1). Anywhere the PRD quotes a size, those are the
numbers. A's own entry — "21 focused prototype and structural-gate tests passed", full suite started
and aborted — matches its tree exactly; B ran the parity gate and recorded it unchanged at 255 tests
in 24 classes. Other measured figures: A is one commit, +1,660/−32, 833 new production lines with 26
comment lines, 8 new test files / 637 lines; B is six commits, +4,639/−5, 1,959 new production lines
with 747 comment lines, 6 new test files / 1,868 lines. Both compile, both pass
`fmt-maven-plugin:check`, both pass `ApplicationTest` against a Postgres testcontainer. Neither
prototype's new tests carry `@DisplayName`, which every pre-existing lifecycle test method has.

### What the two prototypes settle by agreeing

Reached twice, independently, from the same ADRs (comparison §13, B's `findings.md` Part 7
"Converged"): three packages with `graph` at the bottom; `RequiredAuthority` moving to `graph` while
`DefinitionScope` stays package-private in `definition`; State and Event identity as bare strings;
topology validated in the graph rather than in the compiler; definition-level Guards folded ahead of
Transition-level ones at construction; separate Guard and Action registries with duplicate keys
failing the boot; a compiler over already-materialized rows, so `DefinitionInertnessGuardTest`
survives with an amendment instead of the deletion the map anticipated; both compiler and cache
package-private; the four strategies, with `NEGOTIATION_APPROVED` porting the live imperative gate
rather than the dead Spring bean and `SET_POST_VISIBILITY` as one type with `PUBLIC | PRIVATE |
BOTH`; `SPAWN_RESOURCE_LIFECYCLES` registered and failing loudly; and purity plus inertness as
structural gates. That list is the settled part of this ticket's answer, and it is longer than the
list of disagreements.

### Three rows of B's divergence table are wrong, and the PRD must not lift it

B's `findings.md` Part 7 states plainly (`:443-451`) that it was written from A's `PLAN.md` and
`judgement-calls.md` and that **A's Java was not read**. Read against A's tree, three rows do not
survive (comparison §12):

1. "`FailureCategory` has a fourth value, not three" is listed as B-only (`findings.md:504-505`). A
   has the same fourth value — `evaluation/RefusalCategory.java:4` is `NO_TRANSITION`. What is
   genuinely B-only is splitting the structural refusal into `UNKNOWN_EVENT` and
   `NO_TRANSITION_FOR_EVENT`.
2. "Cycles asserted as valid graphs" is listed as A-and-not-B (`:515-516`). A's `PLAN.md:55` says
   cycles stay valid, but no test in A builds one. **Neither prototype proves it.**
3. The table's silences are not evidence of absence, as the section itself warns — and one silence
   hid a divergence in B's favour. A's artifact says nothing about the pre-computed
   `Set<RequiredAuthority>` on its evaluation context, which is a larger difference than four of the
   six listed rows, and it is the side carrying the ADR 0007 hole: A's `case NONE -> true`
   (`evaluation/TransitionEvaluator.java:64-68`) is reachable by the system caller, so A's
   Orchestration Trigger could fire every unguarded open Event. B enforces both halves of "machine-
   fired if and only if the authority is `SYSTEM`" and tests the second over all four human
   authorities.

Nothing here suggests bad faith — B labelled the limitation itself. It is recorded in this Answer
because that table is the most liftable thing in either artifact, and two of its rows would carry a
false statement about A into the specification.

### `slice-01-vocabulary-move`: the code is abandoned, three documents are not

This ticket left that open. **The code is abandoned — not rebased, not redone.** Both prototypes
abandoned it independently, and both contradict its layout: its landed commit `f879867e` moves
`RequiredAuthority` *and* `DefinitionScope` up to `eu.bbmri_eric.negotiator.lifecycle`, while the
instructions above send `RequiredAuthority` to `graph` and keep `DefinitionScope` in `definition`,
which is what both trees built. The branch was 3 commits and 28 files ahead of
`feat/state-machine-implementation`; nothing was cherry-picked from it.

**The branch and its worktree have since been deleted**, so that nobody implementing slab 09 finds a
second PRD at the very path the live one occupies. Its commit tip was
`6b654468a676bdfdba255dc1af8e26199a468d8f`, recoverable from the reflog. Only
nothing was carried across: `recon-strategies.md` went with it, once prototype B's tree
and `before-picture-findings.md` were confirmed to hold what slab 09 needs.
The one layout choice of its that both prototypes reached anyway is the evaluator living in
`lifecycle.evaluation`.

Its documents are not disposable, and each has a different fate:

- **`recon-strategies.md` (333 lines)** — **deleted with the branch.** It was kept as prior art at
  first and then dropped, once prototype B's tree and `before-picture-findings.md` were confirmed to
  hold what slab 09 needs. Prototype B's audit is the evidence: it checked all ten sections against
  the code while porting the strategies and found only §8's bean pattern wanting. Its findings that
  matter are now stated from primary sources instead — today's two-hardcoded-name terminal predicate
  from B's own Guard javadoc, spawn's real location from ticket 02's decision, and `type_key`
  carrying no uniqueness constraint from `V36.3` itself, which the PRD's D4 records because it makes
  a compiled Guard chain a list and never a map. **One correction to it, from B:** §8 offers `DefaultWebhookMappingStrategy` as "the direct
  model for `SET_POST_VISIBILITY`: one class, three configured instances". It does not transfer and
  following it fails the boot — the webhook beans each declare a *different* key, while all three
  post-visibility configurations declare *one*, so three beans collide in the fold. It is one
  `@Component` and three Wiring rows, with the variation in `params` (B's `findings.md` J14, asserted
  at `lifecycle/evaluation/LifecycleStrategiesTest.java:226`).
- **`PRD.md` (474 lines) and its ten inner issues** — **re-chart, do not resume.** Its slice 01 dies
  with the layout, and its tracer-bullet sequence (02 thinnest evaluation, then 03 Guards, 04 params,
  05 the Built-in Stage, 06 the Action chain, with 07 the cache orthogonal and 10 the gate) is
  written for a slab starting from nothing, whereas either prototype already delivers most of 02–06
  and 08–09 as one tree. Three things in it outlive the slicing: the argument at `:145-155` for why
  the evaluator is not inside `definition`; the testing decisions at `:345-425` — both Guards tested
  through the evaluator and nowhere else, and no seam for the params bridge because the fixture
  builder feeds raw JSON through the real binder; and **a sixth correction, to issue 09 and ADR 0002
  jointly** (`:251-256`): `WebhookEventMapper`'s registry is keyed on
  `Class<? extends ApplicationEvent>`, and a grep finds no string-keyed strategy registry anywhere in
  this backend, so "exactly as the existing `WebhookEventMapper` does" names the mechanism and not a
  copyable example — the string type key is new.
- **The 56-line amendment to issue 09** — **its five corrections are now carried in the PRD's D14
  and quoted below; the amendment itself went with the branch.** Issue 09 was deliberately left
  untouched by this ticket, and was amended separately when slab 09 claimed it. The five
  corrections, which are about behaviour and not about packaging: (1) "take them from ticket 01's graph dump" cannot be followed for Guards —
  all 21 Transitions across both dumps record `"guard": null`, so the dump is authoritative for
  Actions, States, Events and authorities and silent on Guards; (2) `NEGOTIATION_APPROVED`'s live
  behaviour is not in `NegotiationIsApprovedGuard`, which is attached to nothing
  (`ResourceStateMachineConfig:119`) — the rule runs imperatively at
  `ResourceLifecycleServiceImpl:142`, so the slab ports the gate and not the class; (3)
  `SET_POST_VISIBILITY` needs a three-valued scope, because `DisablePostsAction` writes both flags and
  `PUBLIC | PRIVATE` alone turns three dump Actions into four rows; (4) today's Information
  Requirement check is not requirement-scoped — `ResourceLifecycleServiceImpl:109-113` passes if
  *any* submission exists for the resource-and-negotiation pair, so ADR 0005/0006 are a
  strengthening; (5) a blocked Resource Event is a silent no-op today, returning the current State
  (`:115-117`), so the pipeline's categories are a change against current behaviour. Both prototypes
  already act on (2) and (3). The same amendment records four decisions taken with the developer, of
  which the first — both enums up to `lifecycle` — is the one this ticket superseded.
- **`DefinitionInertnessGuardTest`'s name list** needs `RequiredAuthority` off and `DefinitionScope`
  kept, as stated above. Both prototypes did that in different ways — A also added four of its own
  new package-private type names to the list; B added 26 lines of javadoc recording why the name came
  off and that this is not a precedent for trimming the list (comparison §9.2). They protect
  different things; do both.

### What issue 09 still has to decide

Basing on B closes most of the comparison's eight open decisions. **Five are closed by B's tree as
built**: §13.1 bound closures rather than pure data in the graph; §13.2 the `definition`↔`evaluation`
seam sitting on `GuardCatalogue`/`ActionCatalogue` in `graph`, with the absent edge asserted in both
directions (`lifecycle/evaluation/EvaluatorPurityGuardTest.java:93-133`); §13.3 raw authority facts
reaching the evaluator with the ADR 0007 rule computed inside the one shared component; §13.6
placeholders that refuse, and with them the evaluator as a `@Component` that `ApplicationTest`
exercises; §13.7 the sealed `Permitted`/`Refused` pair over a six-component record with nullable
fields. **A sixth, §13.4, is settled in direction rather than by construction** — both prototypes'
plans say an unknown current State is graph corruption and must fail distinctly, only A's code does
it, and B does it in one of three lookups (`graph/CompiledGraph.java:120-127` throws, while `:135-146`
answer `Optional.empty()` and an empty list). The change is a-into-b §1, plus asking the State
question before `declaresEvent` at `evaluation/TransitionEvaluator.java:55` so a corrupt pin is not
reported as `UNKNOWN_EVENT`. It hands the cutover slab one short rule: a corrupt pin is a 500 and a
log line, never a 403, 422 or 409.

Four items are left, and none of them is decided by picking a base.

1. **The compiled-graph cache.** Issue 09's own "What to build" requires it, keyed on the Definition
   Version row id alone (ADR 0003). A built it — `definition/CompiledGraphCache.java`, 24 lines, one
   test, unbounded, targeted invalidation. B did not, and recorded the deferral
   (`findings.md:408-409`). **B's deferral therefore cannot simply be carried**: dropping it amends
   issue 09, which is a decision to take out loud. If it lands, two things change from A's version —
   drop `CompiledGraphSource`, a port with one adapter that A's own `judgement-calls.md:50-52`
   flags and B declined for the same reason; and do not keep `computeIfAbsent`, which runs the loader
   inside the map's per-bin lock, harmless against a test lambda and not against a real repository
   loader (comparison §9.4, a-into-b §7). B's F1 (`findings.md:328-353`) is the concrete argument for
   a cache that A's note says is missing: terminality must be asked of each Resource's *own* pinned
   version, so a caller resolves N graphs before it calls.
2. **One vocabulary — and a term the glossary does not have.** §13.8 is two naming sets to choose
   between: `RefusalCategory`/`GuardResult`/`EvaluationResult`/`GuardStrategy`/`ActionStrategy`/
   `PostVisibilityWriter`/`MaterializedDefinition` against `FailureCategory`/`GuardVerdict`/
   `EvaluationOutcome`/`Guard`/`Action`/`PostVisibility`/`DefinitionVersionRows`, and a-into-b §2
   adds one more name, a single exception type for a graph that cannot exist. Underneath the Java
   names sits a real gap: **`backend/CONTEXT.md` has no term for the compiled graph at all.** Its
   "The definition graph" section defines the States, Events and Transitions *of a Definition
   Version*; nothing names the evaluator-ready value compiled from those rows, and nothing names the
   compiling. The instructions above flagged it while creating the thing — "the 'compilation' (might
   need another word)" — and both prototypes shipped `CompiledGraph` with no glossary entry behind
   it. The map's binding constraints make this `/domain-modeling`'s job rather than a naming
   preference.
3. **Required Authority stays single-valued, with ticket [11](11-transition-authority-admin-or-creator.md)
   still open over it.** Both prototypes kept it single-valued and said so (A's `PLAN.md`: "Authority
   remains single-valued; this prototype does not solve admin-or-creator"; B's `PLAN.md:40` and
   `:261`, `findings.md:101`). That follows slab [08](08-definition-schema-and-entities.md)'s
   precedent, which built `required_authority` single-valued as ADR 0002 specifies with ticket 11
   left open rather than getting ahead of it. Slab 09 should do the same and not invent a
   disjunction. Note that the base commit's own `definition/RequiredAuthority.java` carries this in
   18 of its 29 lines — six of the eight Negotiation Transitions are behaviourally `IS_ADMIN OR
   IS_CREATOR`, and inventing a disjunction is not that type's call. B's move kept that text and
   added to it; A's 10-line replacement discarded it (comparison §10). Keep the text.
4. **One recommendation that goes past both prototypes, and needs a developer's decision.**
   a-into-b §3's second half: read Wiring `params` strictly — `FAIL_ON_UNKNOWN_PROPERTIES` on the
   reader — regardless of which `ObjectMapper` the container built, so that a misspelled field in a
   configuration row is refused when the graph compiles instead of binding to the type's default.
   Neither prototype does it, no ADR covers it, and its only evidence is comparison §9.3's probe
   against this project's dependency set. It changes what a wrong Wiring row does, so it is a
   decision and not a graft.

Everything else in a-into-b is additive and needs no decision: the no-params-given-params rule
refused **in code above the mapper** (A refuses it untested at `evaluation/GuardRegistry.java:36-40`;
B accepts and silently discards it under Boot's mapper, while its own registry tests construct a
strict one at `GuardRegistryTest.java:30`, so its suite cannot observe either behaviour); the
graph package's purity as a **whitelist** — A's rule passes on B unchanged, since every import across
B's eleven graph files is `java.util.*` (§4); the inertness list extended to `DefinitionCompiler` and
`DefinitionVersionRows`, which turns that compiler's own javadoc claim into a rule (§5); the three
topology fixtures both plans assert and neither proves — a cycle, an unreachable State, a version
with no terminal State (§6); and the message-formatting defect at `EvaluatorPurityGuardTest:145-147`,
where `.formatted(...)` binds to only the second half of a concatenated literal. Those can land
before the PRD exists. Two smaller conventions for the slab to fix while it is there: `@DisplayName`
on the new tests, and A's `"IN_PROGRESS"` string literal at
`evaluation/NegotiationApprovedGuard.java:23`, where `lifecycle/WellKnownNegotiationStates.java:52`
already holds that constant for exactly this purpose.
