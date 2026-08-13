# REST seam: metadata endpoints and the graph diagram endpoint

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin the published HTTP surface that treats States and Events as a fixed global universe, because
ticket 04 of the map changes that contract and ADR 0002 removes the enums the endpoints currently
enumerate.

**Four metadata endpoints**, not the three the parent map ticket names: Negotiation states,
Negotiation events, Resource states and Resource events. Each has a collection form and a
single-item form addressed by the State or Event name. Pin the full response body for each — the
complete set of members, each member's value, label and description, and the HAL link structure the
assemblers add. This is a payload-shape freeze, so assert on the actual JSON rather than on mapped
objects.

Pin the failure mode too: the single-item endpoints bind the path variable directly to an enum, so an
unrecognised name fails during binding rather than in the handler. Whatever status and body that
produces today is part of the contract the replacement has to consider.

**The graph diagram endpoint.** There is a public endpoint returning a map of the Resource graph,
built by recursively walking Spring Statemachine's transition set. Because the walk descends into each
target as it goes and can revisit targets along different paths, the response is path-shaped rather
than graph-shaped — nested, with repeated subtrees. Pin the response exactly as it is. A faithful
reimplementation from relational configuration is the cutover slab's problem, and it cannot be done
without this before-picture.

## Acceptance criteria

- [x] The Negotiation states collection endpoint's full response body is pinned, including every
      member and its value, label and description.
- [x] The Negotiation events collection endpoint's full response body is pinned.
- [x] The Resource states collection endpoint's full response body is pinned.
- [x] The Resource events collection endpoint's full response body is pinned.
- [x] Each of the four single-item endpoints is pinned for at least one known name.
- [x] The HAL link structure the assemblers add is pinned for both a collection and a single item.
- [x] The response to an unrecognised State or Event name on a single-item endpoint is pinned,
      including status code.
- [x] The graph diagram endpoint's response body is pinned in full, including its nesting and any
      repeated subtrees.
- [x] Assertions are made against response JSON, not against mapped Java objects.
- [x] Any State or Event name appearing in a test is a string literal; the forbidden-import guard
      passes.
- [x] No production code is modified.

## Blocked by

- [String-keyed lifecycle test adapter and forbidden-import guard](02-string-adapter-and-import-guard.md)

## Delivered

Test sources, `eu.bbmri_eric.negotiator.characterization.rest`:

- `LifecycleMetadataEndpointsTest` (19 tests) — the four collections against committed fixtures, the
  four single-item forms, HAL link structure, anonymous and authenticated access, the four
  unrecognised-name failure modes, and the case-handling asymmetry.
- `ResourceLifecycleDiagramEndpointTest` (7 tests) — the diagram body against a committed fixture,
  plus its nesting depth, node shape, repeated subtrees, omissions, and the acyclicity its
  termination depends on.
- `CanonicalJson` — sorts object keys recursively and sorts `_embedded.<rel>` by `value`, so the two
  incidental orderings in these payloads cannot make the suite flaky. Normalisation only; no field
  is dropped and no comparison is relaxed.

Committed fixtures, `backend/src/test/resources/characterization/rest/`:
`negotiation-states.json`, `negotiation-events.json`, `resource-states.json`,
`resource-events.json`, `resource-lifecycle-diagram.json`.

Selector: `eu.bbmri_eric.negotiator.characterization.**` — 26 tests, green, no production source
touched. The fixture comparisons were mutation-checked: perturbing a label and a target State in the
committed fixtures fails the run.

## Findings

### The failure mode is not one failure mode, it is two

The four single-item endpoints do not fail alike, because the two Event path variables have
registered `Converter`s and the two State path variables do not.

| Endpoint | Status | Content-Type | Body |
|---|---|---|---|
| `/v3/negotiation-lifecycle/states/NOT_A_STATE` | 400 | `application/json` | `{"title":"Bad request.","detail":"No enum constant eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState.NOT_A_STATE","status":400}` |
| `/v3/resource-lifecycle/states/NOT_A_STATE` | 400 | `application/json` | same shape, with `...state_machine.resource.NegotiationResourceState.NOT_A_STATE` |
| `/v3/negotiation-lifecycle/events/NOT_AN_EVENT` | 400 | *(none)* | *(empty)* |
| `/v3/resource-lifecycle/events/NOT_AN_EVENT` | 400 | *(none)* | *(empty)* |

The State endpoints use Spring's default enum conversion; the `IllegalArgumentException` from
`Enum.valueOf` is picked out of the cause chain by `NegotiatorExceptionHandler.handleIllegalArgument`
and its message is published verbatim — so **the response body leaks a fully-qualified Java class
name of a class ADR 0002 deletes**. A replacement that keeps this contract would have to keep
emitting a string naming a type that no longer exists. Worth flipping deliberately rather than by
accident.

The Event endpoints go through `NegotiationEventConverter` / `NegotiationResourceEventConverter`,
which swallow the `IllegalArgumentException` and throw a bare
`ResponseStatusException(HttpStatus.BAD_REQUEST)` with no reason, producing an empty body.

Same converters, second consequence: they call `valueOf(source.toUpperCase())`, so
`/v3/negotiation-lifecycle/events/approve` returns **200** while
`/v3/negotiation-lifecycle/states/submitted` returns **400**. Pinned.

The `@Valid` on all four path variables is inert — conversion fails before validation runs.

### There are four DTOs and they are not the same shape

`ResourceStateMetadataDto` publishes a fourth field the other three lack: `ordinal`, set from the
enum constant's `ordinal()` in a hand-written setter. It is a published integer rank over Resource
States (`SUBMITTED` 0 … `RESOURCE_MADE_AVAILABLE` 11) that a relational Lifecycle Definition must
reproduce and keep stable, since it exposes the enum's *declaration order*, which the source file
explicitly documents as significant.

### Collection order is not a contract today

All four collection endpoints build their payload with `Collectors.toSet()`, so `_embedded` arrives
in `HashSet` hash order — neither declaration order nor alphabetical, and not stable under a change
of member set. The suite therefore sorts by `value` before comparing. Nothing needs to preserve
today's array order; the member set and each member's fields do.

### The diagram is path-shaped, and it is bigger than the graph

`GET /v3/resource-lifecycle` returns the bare map — `EntityModel.of(Map)` adds no `_links` — with
content type `application/hal+json`. Top level is a single key, the initial State `SUBMITTED`.

Each transition renders as an object with `target`, `event`, and, when the target has outgoing
transitions, the target's whole transitions map nested under a key named after the target State. So
State keys and Event keys alternate, and the response is a tree of *paths*, not a graph.

- 13 configured transitions render as **29 transition nodes**.
- The `REPRESENTATIVE_CONTACTED` subtree is emitted **twice**, verbatim (reached directly from
  `SUBMITTED` and via `REPRESENTATIVE_UNREACHABLE`).
- The `ACCESS_CONDITIONS_INDICATED` subtree is emitted **four** times.
- The longest path nests **14 objects** deep.
- It is assembled from `HashMap`s at every level, so **key order is not guaranteed**; the suite
  canonicalises before comparing.

### The diagram's recursion terminates only because the Lifecycle is acyclic

`traverseState` keeps no visited set and unconditionally descends into each transition's target. The
walk terminates purely because the Resource Lifecycle graph happens to have no cycle. Adding one —
for instance wiring the currently-dangling `RETURN_FOR_RESUBMISSION` into
`RETURNED_FOR_RESUBMISSION` and back — turns this endpoint into unbounded recursion ending in
`StackOverflowError`, not merely a larger response. A test walks the response and fails if any State
repeats on a root-to-leaf path, so the property is pinned rather than assumed. Any reimplementation
from relational configuration should carry a visited set or a depth bound; the current shape cannot
be reproduced safely without one.

### The universe is the enum, not the graph — confirmed

`RETURNED_FOR_RESUBMISSION` (State) and `RETURN_FOR_RESUBMISSION` and `OVERRIDE` (Events) are
published in full by the metadata endpoints and appear **nowhere** in the diagram. Conversely,
States with no outgoing transition (`RESOURCE_UNAVAILABLE`, `RESOURCE_NOT_MADE_AVAILABLE`,
`RESOURCE_MADE_AVAILABLE`) never appear as a nesting key at all, only as `target` values, because
`traverseState` skips empty transition maps. The diagram cannot be used to enumerate either
universe. Both properties are pinned.

### Access

All five endpoints are `permitAll` for GET in `HTTPRegistryConfigurer`, including
`/v3/resource-lifecycle` itself, and are pinned as an anonymous client calls them. An authenticated
call returns identical bytes; that is pinned too.
