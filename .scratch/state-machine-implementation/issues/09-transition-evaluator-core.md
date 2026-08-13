# Transition Evaluator core

Type: task
Status: open
Blocked by: 08

## Question

Build the Transition Evaluator and the Guard and Action strategy registries. **Slab gate: pure unit tests, no I/O, no database** — the evaluator is handed an already-materialized definition graph and answers what is permitted. Nothing calls it from production code yet.

`backend/CONTEXT.md` defines it: "the stateless component that answers what a Definition Version permits from a given State: whether a particular Event may fire and which State it leads to, or which Events could fire at all. It holds no state, reads no data of its own, and changes nothing."

### What to build

- **One scope-parameterized evaluator core** serving both Definition Scopes (ADR 0001). Not two.
- **Statelessness as a structural constraint, not a convention.** ADR 0001 is explicit that this is "a deliberate constraint rather than a performance claim: an evaluator that structurally cannot query the database makes loading the definition graph an explicit, testable step." The evaluator must have no repository, no `EntityManager`, no Spring data dependency at all — that should be visible in its constructor. An engine that owned its persistence "would invite an N+1 across a negotiation's resources, discovered only under load."
- **The compiled graph cache**, keyed on the Definition Version's **row id alone** (ADR 0003 — no composite `(family, version)` key anywhere), invalidated only when a new version is published. Safe because a version is immutable once active.
- **Guard and Action registries** — self-describing Spring strategy beans, each declaring its own string type key and its own params type, folded into a registry at startup exactly as the existing `WebhookEventMapper` does. **Duplicate keys must fail the boot.** Read `webhook/event/WebhookEventMapper` and follow that shape; ADR 0002 names it as the precedent deliberately.
- **jsonb `params` deserialized into the strategy's declared type at load time** — "one unchecked bridge, in one place". Runtime domain state (the negotiation, the acting person) reaches a strategy through the **evaluation context** at fire time, never through `params`.
- **The Evaluation Pipeline**, in ADR 0005's fixed order: Required Authority → the Information Requirement check → Guards (definition-level entries before Transition entries, each in configured order), **short-circuiting at the first failure**. Failure categories stay monotonic: authorization (403), unmet requirement (422), domain-state conflict (409).
- **The Information Requirement check as a Built-in Stage** — no wiring row exists for it anywhere, so no admin can omit it and no newly added Transition can miss it. It is deliberately **not** a registry Guard type. It still speaks the Guard contract, emitting a result with a reason code and the missing forms in its details. In this slab the satisfaction lookup is an injected port with a test double; the real implementation is a later slab, since it reads submissions and the evaluator does no I/O.
- **One evaluation path for both the real gate and the listing.** `sendEvent` and Possible Events call the same function; the listing is a dry run over the Events reachable from the current State, and a blocked Event is simply omitted. This is ADR 0005's central point — the two can never disagree because there is one path.

### Guard and Action strategies to port

Take them from ticket 01's graph dump, not from memory. At minimum:

- `NEGOTIATION_APPROVED` — becomes a **definition-level** Guard entry per Resource-scope definition (nullable `transition_id`), matching what it always meant. Today it is hacked in as a dangling transition with no source, event or target because Spring Statemachine has no concept for it.
- `SET_POST_VISIBILITY` — one Action type with a scope and a flag, collapsing today's three classes (`DisablePostsAction`, `EnablePrivatePostsAction`, `EnablePublicPostsAction`). This is ADR 0002's worked example of why Actions get params.
- `TERMINAL_AGGREGATION` — takes no params; passes when every Resource of the Negotiation is in a terminal State, asking **each Resource's own pinned Definition Version** whether its current State carries the terminal flag. "Terminal" cannot be a hardcoded list, because Resources in one Negotiation may run different definitions.
- `SPAWN_RESOURCE_LIFECYCLES` — register the type here; its body belongs to the coupling slab, since it writes.

### Out of scope

- Wiring the evaluator into either lifecycle service — that is the cutover slab.
- Anything that writes: committing a move, running Actions for real, history rows, notifications. ADR 0001 puts all of that in the services around the evaluator.
- The real Information Requirement satisfaction lookup, Audience resolution and Quantifier counting (ADR 0006) — a later slab. Port shape only.

### Note

Read ADRs [0001](../../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md), [0002](../../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md) and [0005](../../../backend/docs/adr/0005-information-requirements-gate-transitions-as-a-built-in-stage.md) in full first. `/codebase-design` is appropriate — this is the deep module of the whole effort, and its no-I/O boundary is the argument that makes everything else testable.
