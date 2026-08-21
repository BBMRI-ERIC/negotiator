# The DefinitionResolver's shape is a guess until something calls it

Status: needs-triage

## Parent

[PRD — Definition schema and entities](../PRD.md). Filed by slice
[06](06-definition-resolver-seam.md), which took the three decisions recorded below.

The map already lists "whether the `DefinitionResolver` seam stage 1 leaves behind is the right
shape" as an open question of
[stage 2](../../state-machine-implementation/map.md). This ticket is the concrete version of it: what
slice 06 actually chose, and what would make each choice wrong.

## What was chosen

Slice 06's ticket specified the two answers the resolver must give and said nothing about the shape
of the question, so three calls were taken. All three are Java-only and entirely inside
`eu.bbmri_eric.negotiator.lifecycle.definition`, whose types no production code outside the package
may name — so all three are cheap to reverse, and none needs a migration.

**1. Two methods, not one method taking a scope.** `resolveForNegotiation()` and
`resolveForResource()`, rather than `resolve(DefinitionScope)`. The two questions do not have the
same inputs and are not expected to converge: a Negotiation has exactly one family to resolve, so
its answer depends on nothing, while a Resource's is resolved per resource over a precedence order
of its own associations and its Networks'. A single scope-parameterized method would carry a
parameter that one of its two branches must ignore.

**2. `resolveForResource()` takes no Resource.** The trivial body cannot use one — every Resource
resolves to the Global Default Family — and adding the parameter now would be building the stage 2
signature ahead of the stage 2 body, with nothing able to check that the parameter is the right one.
The consequence is accepted: stage 2 changes this method's signature rather than only its body,
which is a compile error at whatever calls it by then and not a silent behaviour change.

**3. Resolution is total, so failing to resolve throws.** Both methods return a
`LifecycleDefinition` and throw a package-private `DefinitionResolutionException` when the
configuration cannot produce exactly one — none found, or, for the Negotiation scope, more than one
family with an active version. An `Optional` was rejected because it invites the caller to write an
"unresolved" branch: Spawn either initializes every Resource of a Negotiation or none, and the
absence of a resolvable definition is a misconfigured or unseeded schema rather than an outcome.

The exception is package-private and is not registered with `NegotiatorExceptionHandler`, so nothing
maps it to an HTTP status. That is correct while nothing calls the resolver and is the first thing
to revisit when something does.

## Trigger — when this must be revisited

- **The slab that wires the resolver into `SPAWN_RESOURCE_LIFECYCLES`.** It is the first caller, and
  the first place the return type and the failure mode are experienced rather than argued about. It
  must decide what an unresolvable definition does to a Negotiation approval — the approval
  transaction rolling back with a 500 is the current default, and a 409 naming the missing
  configuration is probably better — and therefore whether the exception stays package-private.
- **Stage 2's precedence walk**, which adds the Resource parameter to `resolveForResource()` and may
  well want the *family* rather than the version as its intermediate result, since the walk resolves
  a family and the active version is a second step.
- **A second caller that is not Spawn.** Two callers with different needs is the point at which "two
  methods" versus "one method plus a scope" stops being a matter of taste.
- **A Negotiation-scope Definition Family becoming legitimately plural.** Decision 3 treats a second
  active Negotiation-scope definition as a misconfiguration, and the schema does not forbid one; if
  Negotiations ever resolve a family the way Resources do, that refusal becomes wrong rather than
  strict.

## Related

- [09 Pinning a Negotiation Resource Link that already exists](09-pinning-an-existing-resource-link.md),
  the other thing the Spawn slab has to settle before it can write a pin.
