# ADR 0005 intended-delta tests

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

ADR 0005 deliberately changes two behaviours. Pinning them as parity would freeze the very bugs the
ADR exists to fix, so they get their own clearly named test class that asserts **today's** behaviour
and states plainly that it is expected to be flipped.

**Available events include events that cannot actually fire.** The Resource service's available-events
set is computed from the graph and the caller's authority alone; it does not consult the Information
Requirement gate. So an event whose requirement is unmet is offered, and then refused when clicked.
Pin that today's set includes such an event, given an unmet requirement. ADR 0005 omits blocked events,
so this assertion is the thing that must fail after the cutover.

**Requirement hint links.** The Resource-with-status assembler adds per-row links for requirements
and submissions, one rel per row, with a rel name carrying the row's numeric identifier. ADR 0005
changes the inclusion condition to structural reachability, changes the display name to the Event's
label, and collapses the per-row rels into array-valued ones. Pin the current rel naming and the
current inclusion condition.

Both tests must be named and documented so it is unmistakable that failing them later is success, not
regression — a later session finding these red must be able to tell instantly that flipping them is
the intent. Keep them out of the parity gate selector's default expectations, or mark them such that
the gate reports them separately.

The frontend depends on the current rel naming — it filters link keys by prefix — so record that
these deltas force the two known frontend breakages, which standing decision 5 assigns to whichever
slab breaks them.

## Acceptance criteria

- [ ] A test asserts that today's available-events set for a Resource includes an event whose
      Information Requirement is unmet.
- [ ] That test is documented as an intended delta that ADR 0005 will invert.
- [ ] A test pins the current per-row rel naming for requirement and submission links, including the
      numeric identifier in the rel name.
- [ ] A test pins the current inclusion condition for those links.
- [ ] A test pins the current display name for those links.
- [ ] All intended-delta tests live in a separately named class whose name makes their purpose
      unmistakable.
- [ ] Each carries a comment stating what the post-cutover behaviour should be and which ADR mandates
      the change.
- [ ] The intended-delta tests are excluded from, or separately reported by, the parity gate selector.
- [ ] A note records that these deltas force the two known frontend breakages.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
- [REST seam: metadata endpoints and the graph diagram endpoint](09-rest-seam-metadata-and-diagram.md)
