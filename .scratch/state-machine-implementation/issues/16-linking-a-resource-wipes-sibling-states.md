# Linking a Resource wipes its siblings' States

Type: task
Status: open
Triage: ready-for-agent

Split out of [05](05-resources-added-to-running-negotiation.md) on 2026-09-22, which measured the
defect while designing the late-link pin. Ticket 05 stays open for the design decisions; this ticket
carries only the fix, and it does not depend on any of them.

## The defect

`ResourceServiceImpl.addAnyNewResourcesToNegotiation:95-110` adds a Resource by rebuilding the whole
link set:

```java
Set<Resource> newNegotiationResources = new HashSet<>(negotiation.getResources());
newNegotiationResources.addAll(resourcesToUpdate);
negotiation.setResources(newNegotiationResources);
```

`Negotiation.setResources:243-246` clears the collection and re-adds every Resource through
`addResource:254-256`, which constructs each link with a null State. Its own javadoc says so, and
recommends the alternative: *"This operation will also clear their state. Consider using
addResource"*.

So adding one Resource nulls `current_state` on **every other link of that Negotiation**.
`setStatusForUpdatedResources` then re-states only the Resources named in the request body. The
siblings are left null.

## Measured, twice

Two temporary integration tests, non-transactional, reading committed rows back through
`JdbcTemplate`. An admin adds one not-yet-linked Resource; a pre-existing sibling was pre-set to
`RESOURCE_MADE_AVAILABLE` with `lifecycle_definition_id = 1`.

| | `IN_PROGRESS` (negotiation-1) | `SUBMITTED` (negotiation-5) |
| --- | --- | --- |
| `UPDATE … set current_state = null` emitted | yes | yes |
| Committed sibling State | `REPRESENTATIVE_CONTACTED` | `null` |
| Sibling `lifecycle_definition_id` | `1`, preserved | `1`, preserved |
| `NewResourcesAddedEvent` | fired | not fired |
| Notifications | 2 (representatives 103, 109) | none in the whole table |
| Lifecycle record | re-contact only, never the wipe | none |
| DELETEs on the link table | none | none |

On `IN_PROGRESS` the damage is a silent State regression plus spurious re-notification:
`UpdatedResourcesHandler` runs in the same transaction, sees a stateless link, and
`ResourceNotificationService.handleResourceStateManagement:45-65` resets it to
`REPRESENTATIVE_CONTACTED` and re-contacts its representatives. Representative 103 represents the
pre-existing Resource only, and was notified.

On any Negotiation that is not `IN_PROGRESS`, both the publish (`ResourceServiceImpl:165-167`) and
the handler are gated off, nothing repairs the row, and the `NULL` commits. No event, no
notification, no audit row. The Resource's Lifecycle State is gone with no trace.

There is no delete-and-reinsert. Hibernate's orphan detection works on identifiers, so the
replacement instance merges onto the existing row, and the emitted `UPDATE` leaves the pin column
out. That is why the pin survives while the State does not.

## The fix

Make the add additive. In `addAnyNewResourcesToNegotiation`, keep the authorization check exactly as
it is and replace the three rebuild lines with:

```java
resourcesToUpdate.forEach(negotiation::addResource);
```

`Negotiation.addResource` returns false and changes nothing for an already-linked Resource, because
the link set is a `Set` and `NegotiationResourceLink` equality is on the embedded id alone
(`NegotiationResourceLink.java:63-74`). So the already-linked case is unchanged and only genuinely
new links are created.

After this, `Negotiation.setResources` has two callers left, `NegotiationServiceImpl:178` and
`CustomNegotiationBuilder:19`, both at creation. Narrowing or deleting it is optional tidy-up, not
part of this ticket.

## Deadline

**No later than the first commit that writes a Definition Version Pin.** Today nothing writes
either pin, so the wipe produces `(current_state=null, lifecycle_definition_id=null)`, which is
uniformly "nothing has started". Once Spawn pins, the same path commits
`(current_state=null, lifecycle_definition_id=1)`. That combination has never existed and the new
model declares it impossible: `LifecycleContextAssembler.pinOf:111-121` would hand out a pin, and
every read path would treat the row as a running Resource Lifecycle standing in no State.

Landing earlier is fine and preferred. Landing later means the coupling slab ships a row shape its
own invariant forbids.

## Behaviour change, and the delta test

This is an observable change and must be recorded as an intended delta before the fix lands, while
today's behaviour is still measurable.

Add `characterization.delta.IntendedDeltasLinkingWillStopWipingSiblingsTest`, asserting the measured
behaviour above. It must:

- carry the JUnit tag `intended-delta` via the existing `IntendedDeltasAdr0005WillInvertTest.INTENDED_DELTA`
  constant, so the parity half of the gate keeps excluding it;
- live inside `eu.bbmri_eric.negotiator.characterization.**` so the mechanical forbidden-reference
  guard still scans it;
- name States as strings through the test-scope adapter, never through the enums.

It pins three things, all of which change when the fix lands: siblings keep their States rather than
being nulled; no Lifecycle Record is written for a wipe or a re-contact; and notifications for this
path drop to **zero**, because once nothing is wiped, the siblings are already stated and the new
Resource is already `SUBMITTED`, so `handleResourceStateManagement` skips all of them.
`NewResourcesAddedEvent` and its webhook mapping are unaffected.

`parity-gate.md` then needs three edits in the same commit: the intended-delta count 8 becomes 9,
the combined `263 tests in 25 classes` line changes, and the sentence asserting all eight deltas are
`IntendedDeltasAdr0005WillInvertTest` becomes false.

## Gate

- The parity half of `parity-gate.md`'s command green, unchanged.
- The new delta test green before the fix, red after, and updated in the fix's own commit to assert
  the new behaviour.
- `NegotiationControllerTests`' six `addResources_*` and two `removeResource_*` tests green without
  modification. *Expectation, not measurement:* none of them asserts a non-named sibling's State, so
  none should notice. If one does go red, that is a finding for this ticket, not a licence to edit
  the test.

## Not in scope

Three things surfaced alongside this and belong elsewhere. Do not absorb them.

- `UpdateResourcesDTO`'s `SUBMITTED` default writes a State name that a custom Definition Family
  need not have. Same shape as [10](10-network-kpis-name-resource-states.md), and it is stage 2's.
- A Resource added while the parent is `PAUSED` lands on `SUBMITTED` and nobody is ever contacted,
  because `handleResourceStateManagement` is gated on `IN_PROGRESS`. That is
  [12](12-adr-0009-seed-specified-against-wrong-picture.md)'s territory.
- No `ResourceStateChangeEvent` fires for the initial-State write. That is today's deliberate
  silence, already owned by [02](02-state-triggered-behaviour-location.md) and ticket 12.
