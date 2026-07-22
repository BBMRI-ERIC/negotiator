# Negotiator Backend

The Spring Boot service behind the BBMRI-ERIC Negotiator: negotiations over access to biobank resources, their lifecycles, and the governance structures (networks, representatives) around them.

## Language

### Lifecycle state machines

**Definition Family**:
A lineage of state-machine definition versions sharing an immutable `family_key`. What Resources and Networks associate with — never a specific version.
_Avoid_: definition group, machine type

**Definition Version**:
One immutable row of a Definition Family — a complete states/events/transitions graph. Its row id is its sole machine identity; all FKs and the engine cache key use it. Exactly one version per family is *active* — the one new work resolves to; publishing flips the active flag, and never moves pinned in-flight work.
_Avoid_: definition revision, definition instance

**Version (sequence)**:
A per-family, system-assigned incrementing integer on each Definition Version, for human display and audit only ("v3 of Standard flow"). Assigned at row creation; gaps are harmless. Carries no identity role.

**Definition Version Pin**:
The immutable `definition_version_id` recorded on a Negotiation or NegotiationResourceLink at creation, fixing which Definition Version governs it for its entire lifetime regardless of later publishes.
