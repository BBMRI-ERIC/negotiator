# Definition schema and entities

Type: task
Status: open

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
