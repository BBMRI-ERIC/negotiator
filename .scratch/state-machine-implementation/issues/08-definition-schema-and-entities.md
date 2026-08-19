# Definition schema and entities

Type: task
Status: claimed

## Question

Build ADR 0002's and 0003's relational schema as an **additive-only** Flyway migration plus its JPA entities. Nothing reads it yet.

**Slab gate:** the additive DDL migration applies cleanly, the entities map and round-trip in repository tests, the full suite stays green, and **no production code path reads any of it**. ADR 0009 fixes this packaging: additive DDL is its own migration, "harmless if it lands alone". The atomic data cutover is a separate, much later file.

### Schema — from ADR 0002 and 0003

- `StateMachineDefinition` — a **Definition Version** row. `scope` (`NEGOTIATION` | `RESOURCE`), immutable `family_key`, editable `name` label, system-assigned `version` display integer unique on `(family_key, version)`, an active flag with exactly one active row per family, and `is_global_default` (see below).
- `State` — human `label`, plus `initial` and `terminal` flags. Exactly one initial State per Definition Version.
- `Event` — may carry **no Transition at all**; that is how the Override Event survives as a name under which an admin's direct state change appears in history.
- `Transition` — from-State, to-State, Event, and `required_authority` ∈ `NONE`, `IS_ADMIN`, `IS_CREATOR`, `IS_REPRESENTATIVE`, `SYSTEM`.
- **Two separate wiring tables, not one polymorphic one.** Guard wiring has a **nullable `transition_id`** — null means the Guard applies to every Transition of the Definition Version, set means that Transition alone — with **two partial unique indexes** keeping `order` unique within each scope. Action wiring is transition-scoped only and needs no `definition_id`. Both carry a string type key and per-strategy `params` as jsonb. The order *between* scopes is pipeline logic, not a column.
- **Definition Version Pin** — immutable `definition_version_id` on `Negotiation` and `NegotiationResourceLink`. Add **nullable** here; ADR 0009's cutover backfills and sets NOT NULL. Do not add NOT NULL in this slab.

Read ADRs [0002](../../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md) and [0003](../../../backend/docs/adr/0003-definition-versioning-and-identity.md) in full — the reasoning behind each shape matters, particularly why the row id is the sole machine identity and `version` is display-only.

### Explicitly in scope despite stage 2

- **`is_global_default`** on the family, with a partial unique index enforcing exactly one. Kept now even though Definition Resolution is stage 2, because keeping it costs one boolean and one index and means stage 2 needs no data migration to designate the default.
- **A `DefinitionResolver` interface with a trivial body** — returns the Global Default Family for `RESOURCE`, the sole definition for `NEGOTIATION`. This is the seam stage 2 replaces. ADR 0007 already localizes all resolution into `SPAWN_RESOURCE_LIFECYCLES`, so keeping the interface means stage 2 replaces a body, not a call graph. **Do not inline the repository lookup into the Spawn Action** — that is the one way this shortcut would cost real work later.

### Explicitly out of scope

- Association tables (Resource↔family, Network↔family), the precedence walk, and the `NetworkService` conflict check — all stage 2.
- Any read path. If something reads these tables, the gate has failed.
- No `description` column on `State` unless ticket 04 requires one — that would be an ADR 0002 amendment, and its cost is assessed there, not assumed here.

### Note

Read ticket 01's graph dump if it exists. The schema must be able to express **both** current graphs verbatim — every state, event, transition, guard, action, required authority and initial flag — and the dump is the only mechanical statement of what that is. If something in the dump cannot be represented, that is a genuine ADR 0002 contradiction and becomes its own decision ticket, not a quiet schema addition.

## Progress

**Claimed and charted 2026-08-19.** The slab's inner tracker is
[`.scratch/definition-schema-and-entities/`](../../definition-schema-and-entities/PRD.md) — a PRD plus
7 commit-sized issues. Work resumes there; do not re-plan this slab. Branch:
`feat/state-machine-implementation`.

Two read-only recon briefs were produced before the PRD and are the slab's reference — do not
re-derive what they contain:

- [recon-expressiveness.md](../../definition-schema-and-entities/recon-expressiveness.md) — element-by-element
  proof that ADR 0002/0003's schema holds both live graphs from ticket 01's dump. Verdict: **yes, with
  one genuine contradiction** (below). Counts verified: 20 States, 21 Transitions, 18 dumped Events,
  21/21 with no Guard, 3 Actions, both Legacy States present and fully isolated, `initialState:
  SUBMITTED` in both graphs, all five authority values mapping onto the enum with nothing left over.
- [recon-conventions.md](../../definition-schema-and-entities/recon-conventions.md) — Flyway, DDL,
  entity, repository and test idiom, cited `file:line`.

### Two ADR contradictions found, both filed rather than patched

- **[11 Transition authority cannot express "admin or creator"](11-transition-authority-admin-or-creator.md)** —
  six of eight Negotiation Transitions are behaviourally `IS_ADMIN OR IS_CREATOR` (the blanket check
  at `NegotiationLifecycleServiceImpl.java:93-96` runs before any rule) and no single value of ADR
  0002's enum reproduces it. **Decided with the developer: this slab builds the column single-valued
  as specified.** Two of the three candidate resolutions leave the DDL untouched, and the migration
  only ever runs against a branch database that is clean+migrated on every test build.
- **[12 ADR 0007 and 0009 specify a spawn and a seed the code does not have](12-adr-0009-seed-specified-against-wrong-picture.md)** —
  this is the ticket ticket 01's answer said "does not exist yet and is the first thing stage 1
  needs", now carrying a second defect: ADR 0009 enumerates one seeded Action, the dump carries three
  and no spawn. Concerns seed content, so it blocks the migration and coupling slabs, not this one.

### Three findings routed, not acted on here

`description` on State and Event, and the Resource-State `ordinal`, both belong to ticket
[04](04-global-state-event-metadata-contract.md). `label` and `terminal` are absent from the graph
dump: `label`'s source for the seed is the committed `characterization/rest/*-states.json` (it is
load-bearing — a notification body is built from it), and `terminal` is where Part 7's "does
conclusion widen beyond two Resource States?" decision physically lands, since deriving it
structurally gives 4 where today's behaviour counts 2.

### Naming decided with the developer, 2026-08-19

The table is **`lifecycle_definition`** / `LifecycleDefinition`, and the pin column follows it as
`lifecycle_definition_id`. Not ADR 0002's literal `state_machine_definition` (`state machine` is
_Avoid_ vocabulary, `backend/CONTEXT.md:11`) and not `definition_version` — `version` is a
display-only column carrying no identity by ADR 0003, so naming the table out of it would imply the
row *is* the version. Identifiers only: **Definition Version** remains the domain term.

The rename was **reviewed a second time and confirmed**, against a close runner-up
(`definition_version` with the integer renamed to `version_sequence`). The PRD records why it lost and
what `lifecycle_definition` costs. Not to be relitigated without new information.

### Also decided

- **`is_global_default` is unique among *active* rows** — `WHERE is_global_default AND active`. It is
  a family-level fact with no family table to live on, so each version row carries it: the flag
  travels with the family across versions and publishing need not move it off the old row in the same
  statement. An inactive row keeping `true` is honest — it *was* the default while it was active.
- **One migration file per slice, `V36.0`–`V36.5`, never one shared file.** Seven commits appending to
  one file changes its checksum each time, failing Flyway validation on any database that already ran
  it; test contexts clean+migrate every build and would never notice, but a local dev database breaks
  at every slice. ADR 0009 is satisfied either way — it requires additive DDL to land before and
  separately from the cutover, not to be one file.
- **Four constraints that were missing from the first draft of the inner tickets** and are now in
  them: State and Event `name` unique within a definition (ADR 0009's cutover resolves live strings by
  that natural key, so a duplicate makes it ambiguous); `(definition, from_state, event)` unique on
  Transition; FKs `ON DELETE RESTRICT`; and the recorded limitation that the ADRs' "exactly one" is
  only enforceable as "at most one" — a partial unique index cannot require at least one, and zero
  active rows is a valid intermediate state mid-publish, so that half waits for publish-time
  validation in stage 3.

The four partial unique indexes this slab needs are the **first** partial indexes and the first unique
indexes in the codebase — two `CREATE INDEX` exist in total, neither with a `WHERE`, and zero
`CREATE UNIQUE INDEX`. Every uniqueness rule is therefore proven by a test that violates it. And the
"nothing reads it" gate is a mechanical guard test reusing `CharacterizationImportGuardTest`'s
technique, not a grep — package-private entities and repositories cover the rest at compile time.
