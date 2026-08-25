# PRD — Definition schema and entities

Status: resolved — all seven slices landed, slab gate green, map ticket 08 closed 2026-08-25

Slab for map ticket
[08 Definition schema and entities](../state-machine-implementation/issues/08-definition-schema-and-entities.md)
of [State machine implementation](../state-machine-implementation/map.md). Branch:
`feat/state-machine-implementation`.

Two recon briefs were produced before this PRD and **are the reference for every convention and every
fact below** — read them rather than re-deriving:

- [recon-conventions.md](recon-conventions.md) — Flyway, DDL, entity, repository and test idiom, cited `file:line`.
- [recon-expressiveness.md](recon-expressiveness.md) — element-by-element proof that this schema holds both live graphs.

## Problem Statement

ADR [0002](../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md) and ADR
[0003](../../backend/docs/adr/0003-definition-versioning-and-identity.md) describe a relational schema
for Lifecycle Definitions. None of it exists. Every later piece of the effort — the Transition
Evaluator, the seed, the cutover, admin authoring — needs those tables to exist and needs entities
that map them, and none of that work can start against a schema that is still prose.

At the same time this is the riskiest possible moment to touch a running system: the schema is large,
it lands on a branch that will live for weeks, and a mistake in it propagates into every later slab.

## Solution

Land the schema and its entities **additively and inertly**: one Flyway migration that only ever
adds, a package of JPA entities that map it, repository tests that prove the mapping round-trips, and
**no production code path that reads any of it**. If something reads these tables, this slab has
failed.

That inertness is the whole point of the packaging. ADR
[0009](../../backend/docs/adr/0009-forward-only-convert-in-place-migration.md) already fixes it:
additive DDL is its own migration, "harmless if it lands alone", and the atomic data cutover is a
separate, much later file. Nothing here is destructive, nothing here changes behaviour, and the
parity gate must be green before and after with the same 255 tests.

## User Stories

1. As the developer of the Transition Evaluator slab, I want `state`, `event` and `transition` tables with foreign keys, so that I can load a Definition Version's graph without inventing its storage first.
2. As the developer of the Transition Evaluator slab, I want the graph reachable from a single Definition Version row id, so that the compiled-graph cache key ADR 0001 assumes actually exists.
3. As the developer of the migration slab, I want every column the v1 seed must populate to already exist, so that the cutover file is pure INSERT and UPDATE and adds no DDL of its own.
4. As the developer of the migration slab, I want `lifecycle_definition_id` present and **nullable** on `negotiation` and `negotiation_resource_link`, so that my cutover can backfill it and only then set NOT NULL.
5. As an administrator (eventually, in stage 3), I want a Definition Family to be a lineage of immutable versions, so that publishing a change never moves work already in flight.
6. As an administrator, I want exactly one version of a family to be active, so that publishing is a one-step flip rather than a bulk re-point of every association.
7. As an administrator, I want a freely editable `name` on a version and a stable `family_key` underneath it, so that renaming a family breaks nothing.
8. As an administrator, I want a per-family display `version` integer, so that tooling and history can say "v3 of Standard flow" instead of an opaque id.
9. As an administrator, I want gaps in that display sequence to be harmless, so that abandoning a draft costs nothing.
10. As an administrator, I want a State to carry a human label, so that the UI and the notification bodies that already depend on labels keep working.
11. As an administrator, I want `initial` and `terminal` flags on a State, so that the evaluator knows where a Lifecycle starts and when it is finished.
12. As the system, I want exactly one initial State per Definition Version enforced by the database, so that a malformed definition cannot be published at all.
13. As an administrator, I want to define an Event that carries no Transition, so that the Override Event survives as a name under which a direct state change appears in history.
14. As an administrator, I want a State that no Transition targets, so that ADR 0009's Legacy States (`APPROVED`, `RETURNED_FOR_RESUBMISSION`) can be seeded without distorting history.
15. As an administrator, I want a Transition to name its Required Authority separately from its Guards, so that "who may fire" and "is the move currently legal" stay different questions that fail differently.
16. As an administrator, I want to wire a Guard to one Transition, so that a condition can be specific.
17. As an administrator, I want to wire a Guard to a whole Definition Version, so that a condition applying to every Transition is one row rather than twenty-one.
18. As an administrator, I want Guard ordering to be unique within each of those two scopes, so that the effective chain is deterministic.
19. As an administrator, I want to wire an Action to a Transition, so that committing a move can have consequences.
20. As an administrator, I want per-wiring `params` as jsonb, so that one Java strategy serves many configurations — ADR 0002's collapse of three post-visibility Action classes into one typed key.
21. As the developer of stage 2, I want `is_global_default` already on the family, so that Definition Resolution becomes total with no data migration to designate the default.
22. As the developer of stage 2, I want a `DefinitionResolver` interface already in place, so that I replace a body rather than a call graph.
23. As a reviewer, I want a mechanical test proving no production code reads any of this, so that "additive and inert" is a checked fact rather than a claim in a commit message.
24. As a reviewer, I want the parity suite green and unchanged, so that this slab is demonstrably behaviour-neutral.
25. As an operator, I want this migration to be safe to deploy on its own, so that landing it early carries no coupling to the cutover.

## Implementation Decisions

### Naming — two calls taken here, both flagged upward

- **The table is `lifecycle_definition`, entity `LifecycleDefinition`** — not
  `state_machine_definition` (ADR 0002's literal identifier) and not `definition_version`. Three
  things were being conflated and only one is a table: the **lineage** is not a table at all, it is a
  shared `family_key` value; the **row** is a complete immutable graph; and **`version` is a
  display-only column on that row**, carrying no identity by ADR 0003. Naming the table out of that
  column would give `definition_version.version` and imply the row *is* the version. `state machine`
  is _Avoid_ vocabulary (`backend/CONTEXT.md:11`), while "Lifecycle definition" is ADR 0002's own
  title phrase. FKs then read `state.lifecycle_definition_id`. **Decided with the developer and
  reviewed a second time, 2026-08-19.**

  The runner-up was `definition_version` **with the column renamed** to `version_sequence` — which
  matches `backend/CONTEXT.md:19`'s binding term for this row exactly, and resolves the ambiguity from
  the other side, since CONTEXT.md already names the integer separately at `:23` as **Version
  (sequence)**. It was rejected for diverging from ADR 0003's literal column name. Recorded because it
  is a genuinely close call: the acknowledged cost of `lifecycle_definition` is that it matches no
  CONTEXT.md term exactly, and its name carries no signal that rows are immutable versions within a
  family. Do not relitigate this without new information.
- **The pin column follows the table: `lifecycle_definition_id`.** ADR 0003 and ADR 0009 both name it
  literally `definition_version_id`; keeping that spelling would leave a column inconsistent with the
  table it points at. The ADRs are binding on *shape*, and this changes no shape. The domain term
  **Definition Version Pin** is unaffected.
- **The domain vocabulary is untouched.** `backend/CONTEXT.md:19` continues to define **Definition
  Version** as the term for this row. This slab renames an identifier, not a term; changing the term
  would be a `/domain-modeling` decision and is not one this slab may take.
- **`state`, `event`, `transition`, `guard_wiring`, `action_wiring` are unprefixed.** No collision
  exists (verified against all 41 existing tables and against the main source tree), and the house
  style already carries bare generic names — `role`, `post`, `request`, `template`, `notification`.
- **`order` becomes `sort_order`.** No identifier is double-quoted in any of the 50 existing
  migrations, and `order` would have to be.

### Schema

Six tables plus two columns, additive only, no drops, no CHECK removals, no data.

**One migration file per slice, `V36.0` through `V36.5`** — the next free version onward. Not one
shared file: seven commits appending to a single file changes its checksum every time, which fails
Flyway validation on any database that already ran it. Test contexts clean+migrate on every build and
so would never notice, but a developer's local database would break at every slice. ADR 0009 is
satisfied either way — it requires the additive DDL to land **before and separately from** the data
cutover, not to be a single file. Each slice's file is independently applicable, which is also what
makes each slice independently green.

- `lifecycle_definition` — `scope` (`NEGOTIATION` | `RESOURCE`), immutable `family_key`, editable
  `name`, system-assigned `version` integer **unique on `(family_key, version)`**, `active` flag with
  a **partial unique index giving exactly one active row per family**, and `is_global_default` with a
  **partial unique index scoped to active rows** — `WHERE is_global_default AND active`. The flag is a
  *family*-level fact with no family table to live on, so it is carried by each version row: the flag
  travels with the family across versions, and publishing a new version does not have to move it off
  the old one in the same statement. An inactive row keeping a `true` there is honest — it *was* the
  default while it was active.
- `state` — `lifecycle_definition_id` FK, `name`, human `label`, `initial` and `terminal` flags.
  Exactly one initial State per version, enforced by a **partial unique index** on
  `(lifecycle_definition_id) WHERE initial`.
- `event` — `lifecycle_definition_id` FK, `name`. May carry no Transition at all.
- `transition` — `lifecycle_definition_id` FK, from-State, to-State, Event, and `required_authority`
  ∈ `NONE`, `IS_ADMIN`, `IS_CREATOR`, `IS_REPRESENTATIVE`, `SYSTEM`.
- `guard_wiring` — `lifecycle_definition_id` FK, **nullable `transition_id`** (null = every Transition
  of the version; set = that Transition alone), `type_key`, `params` jsonb, `sort_order`. **Two
  partial unique indexes** keep `sort_order` unique within each scope.
- `action_wiring` — transition-scoped only, **no `definition_id`** (the Transition implies it),
  `type_key`, `params` jsonb, `sort_order`.
- **Definition Version Pin** — `lifecycle_definition_id` added **nullable** to `negotiation` and to
  `negotiation_resource_link`. ADR 0009's cutover backfills and sets NOT NULL. **Not NOT NULL here.**

The `order`-between-scopes question is pipeline logic, not a column (ADR 0002).

### Entities

New package `eu.bbmri_eric.negotiator.lifecycle.definition`. `@EntityScan` already covers
`eu.bbmri_eric.negotiator.*`, so no configuration changes.

- Ids: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;` against
  `BIGSERIAL PRIMARY KEY`.
- No naming strategy is configured, so Boot's `CamelCaseToUnderscoresNamingStrategy` applies and
  `familyKey` → `family_key` needs no `@Column`. Annotate only where names deliberately differ.
- Enums (`scope`, `required_authority`): `@Enumerated(EnumType.STRING)` on VARCHAR, uniform across
  the codebase.
- jsonb `params`: `@JdbcTypeCode(SqlTypes.JSON)` on a `String` field — Hibernate 6 native, the most
  recent of the two idioms present, and needs no dependency. Every jsonb site in this codebase maps
  to `String`, never to a POJO; this slab does not break that.
- **Entities and repositories are package-private**, following the most recent idiom
  (`Template`/`TemplateRepository`). This is not cosmetic here: it makes the "nothing reads it" gate
  partly enforced by the compiler.
- `Negotiation` and `NegotiationResourceLink` gain a nullable, immutable `lifecycleDefinitionId`. Map
  it as a plain `Long` column rather than an association, so nothing can lazily traverse into the
  definition graph from a read path that exists today.

### The `DefinitionResolver` seam

An interface with a trivial body: the Global Default Family's active version for `RESOURCE`, the sole
active version for `NEGOTIATION`. This is the seam stage 2 replaces, and ADR 0007 already localizes
all resolution into `SPAWN_RESOURCE_LIFECYCLES`, so stage 2 replaces a body rather than a call graph.

**Do not inline the repository lookup into the Spawn Action** — the ticket names that as the one way
this shortcut costs real work later. Nothing calls the resolver in this slab; it exists and is
unit-tested.

### Known contradictions this slab does NOT resolve

The expressiveness check found two things the ADRs get wrong. Both are filed as their own decision
tickets on the map, per the binding constraint that a contradiction is never a quiet edit. **Neither
blocks this slab** — the tables hold every shape involved:

- [11 Transition authority cannot express "admin or creator"](../state-machine-implementation/issues/11-transition-authority-admin-or-creator.md).
  Six Negotiation Transitions are behaviourally `IS_ADMIN OR IS_CREATOR` and no single enum value
  reproduces that. **This slab builds `required_authority` as ADR 0002 specifies — single-valued.**
  Two of the three candidate resolutions leave that DDL untouched; only "make it set-valued" would
  change it, and the migration has never been applied outside a branch where every test run does
  clean+migrate, so amending it later is cheap. Flagged, not guessed at.
- [12 ADR 0007 and 0009 specify a spawn and a seed the code does not have](../state-machine-implementation/issues/12-adr-0009-seed-specified-against-wrong-picture.md).
  Concerns seed *content*, which is the migration slab.

Three further findings are already routed elsewhere and are **not** acted on here: `description` on
State and Event, and the Resource `ordinal` ordering contract, both belong to map ticket 04; `label`'s
source for the seed is the committed `characterization/rest/*-states.json`, not the graph dump.

## Testing Decisions

A good test here asserts **externally observable persistence behaviour** — a row round-trips, a
constraint refuses a second row, a jsonb payload survives — never a Hibernate internal or a mapping
annotation. These are not behaviour tests, because this slab deliberately adds no behaviour.

- **Seam: the repository interfaces**, against a real PostgreSQL via Testcontainers. Prior art:
  `integration/repository/AttachmentRepositoriesTest` (cleanest `@RepositoryTest`),
  `NegotiationRepositoryTest` (adds `@Import(MockUserDetailsService.class)`, needed if an entity
  extends `AuditEntity`), `OrganizationRepositoryTest` (`loadTestData = true`).
- **The migration itself needs no dedicated test.** Test contexts run clean+migrate on every build
  (`FlywayConfig.java:34-40`), so a broken `V36.0` fails every Testcontainers test in the suite. That
  is stronger coverage than a bespoke assertion, and it is free.
- **Every partial unique index gets a test that violates it** and expects the write to be refused —
  one active version per family, one global default, one initial State per version, `sort_order`
  unique within each Guard scope. These are the first partial indexes and the first unique indexes in
  this codebase (recon-conventions finding 1), so none of the syntax has prior art here and all of it
  must be proven against a real Postgres rather than assumed.
- **`DefinitionResolver`** gets a plain unit test of its trivial body.
- **The inertness gate is a test, not a grep.** Reuse the `CharacterizationImportGuardTest` technique
  — plain JUnit text-scanning with comment-blanking, a working-dir-resolved scan root, named
  exemptions and an anti-vacuity test — pointed at `backend/src/main`, failing on any reference to the
  new types from outside `lifecycle.definition`. A one-off grep would pass today and rot immediately.
- **The parity gate**: the parity half of [parity-gate.md](../state-machine-implementation/parity-gate.md)
  must be green — 255 tests, unchanged. Run with
  `nix develop .#opencode --command ~/.claude/skills/focused-backend-tests/scripts/test-backend.sh`.

## Out of Scope

- Association tables (Resource↔family, Network↔family), the Definition Resolution precedence walk,
  and the `NetworkService` conflict check — all stage 2.
- **Any read path.** If production code reads these tables, the gate has failed.
- The v1 seed SQL and the data cutover — a separate, much later migration file (ADR 0009).
- NOT NULL on either pin column.
- A `description` column on `State`, and any Resource-State `ordinal` column — map ticket 04 owns
  both, and adding either here would be an ADR 0002 amendment whose cost is assessed there.
- Deleting anything. No column, constraint or table of the current Spring Statemachine world is
  touched.

## Further Notes

- **PostgreSQL only** — this schema depends on partial indexes, per the map's binding constraints.
- The additive migration must stay safe to land alone. Nothing in this slab may depend on the cutover
  having run.
- `negotiation_resource_link.current_state` has **no** CHECK constraint (the 12-value CHECK belongs to
  the dropped `resource_state_per_negotiation` table); only `negotiation.current_state` has one.
  Neither is touched here, but the cutover slab inherits that asymmetry.
