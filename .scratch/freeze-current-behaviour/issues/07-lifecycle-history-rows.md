# Lifecycle history rows for both graphs

Status: ready-for-agent

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin the audit history a transition writes, for both graphs, because ADR 0008 converts these tables'
state column to a foreign key and the conversion has to preserve what is there.

Every Negotiation transition writes a Negotiation lifecycle record, and every Resource transition
writes a Resource lifecycle record carrying the Resource it concerns. Pin, for a real transition
driven through the adapter: that exactly one row appears, which state it records as the state changed
to, that it is associated with the right Negotiation — and for the Resource records, the right
Resource — and that the auditing columns are populated.

Two things to establish rather than assume, since both bear directly on the migration:

- Whether a record captures only the destination state or also the origin. If only the destination,
  say so explicitly in this issue, because reconstructing a transition from the audit trail then
  depends on row ordering, which the migration must preserve.
- Whether a refused send writes a row. It should not, but the Resource service's silent refusal
  returns normally, so this is worth an explicit negative test.

Also pin the accumulation across a multi-step path — several transitions in sequence produce several
rows in order — since the seeded test data already contains history rows and the migration's backfill
joins on state names across the whole table.

## Acceptance criteria

- [ ] A single Negotiation transition is pinned as writing exactly one Negotiation lifecycle record,
      with the expected destination state and Negotiation.
- [ ] A single Resource transition is pinned as writing exactly one Resource lifecycle record, with
      the expected destination state, Negotiation and Resource.
- [ ] The auditing columns on both record types are pinned as populated.
- [ ] Whether the origin state is recorded is established and stated in this issue.
- [ ] A refused Negotiation send is pinned as writing no record.
- [ ] A refused Resource send is pinned as writing no record, despite returning normally.
- [ ] A multi-step transition path is pinned as producing the expected records in order.
- [ ] Records written by these tests are pinned as distinguishable from those already present in the
      seeded test data.
- [ ] All assertions use Awaitility with a bounded timeout.
- [ ] Every State and Event is named as a string; the forbidden-import guard passes.
- [ ] No production code is modified.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)
- [Resource transition and authority parity, including the IN_PROGRESS gate](04-resource-transition-parity.md)
