# The DefinitionResolver seam

Status: ready-for-agent

## Parent

[PRD — Definition schema and entities](../PRD.md).

## What to build

An interface answering "which Lifecycle Definition does this new work run under", with a deliberately
trivial body: the Global Default Family's active definition for `RESOURCE`, the sole active
definition for `NEGOTIATION`.

This exists to be a **seam**, not a feature. Stage 2 replaces the body with ADR 0004's precedence walk
— direct association, then Network, then Global Default — and ADR 0007 already localizes all
resolution into the Spawn Action, so keeping the interface means stage 2 replaces a body rather than
a call graph.

**Do not inline the repository lookup into the Spawn Action.** The map ticket names that as the one
way this shortcut would cost real work later.

Nothing calls the resolver in this slab. It exists, it is tested, and it is unreferenced by
production code — which is a deliberate state, not an oversight.

## Acceptance criteria

- [ ] The interface and its implementation live in the new definition package.
- [ ] Resolving for `RESOURCE` returns the active definition of the family flagged
      `is_global_default`.
- [ ] Resolving for `NEGOTIATION` returns the sole active `NEGOTIATION`-scope definition.
- [ ] Behaviour is specified and tested for the case where no such definition exists — the schema is
      empty until the seed lands, so this is the *normal* state throughout this slab, not an edge
      case.
- [ ] No production code outside the definition package calls it.
- [ ] Full suite green, parity count unchanged.

## Notes

**Visibility:** package-private like everything else in this slab. That is not in tension with it
being a seam — nothing calls it *yet*, and the coupling slab that wires it into
`SPAWN_RESOURCE_LIFECYCLES` widens it deliberately at that point, which is a visible decision rather
than a default. Slice 07's guard test would otherwise flag it.

## Blocked by

- [01 The lifecycle_definition table](01-lifecycle-definition-table.md)
