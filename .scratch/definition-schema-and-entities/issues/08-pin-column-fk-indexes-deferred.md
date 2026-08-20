# The two pin columns have no index on them

Status: needs-triage

## Parent

[PRD — Definition schema and entities](../PRD.md). Filed by slice
[05](05-lifecycle-definition-pin-columns.md), which took the decision recorded below.

## What was chosen

`V36.4` adds `lifecycle_definition_id` to `negotiation` and to `negotiation_resource_link`, each with
a foreign key to `lifecycle_definition` and **no index on the column**.

Both are the *referencing* side of a foreign key, so PostgreSQL creates nothing for them
automatically, and neither table has any other index leading with the column. The consequence is a
sequential scan over the referencing table whenever the `ON DELETE RESTRICT` check on
`lifecycle_definition` runs. Slice 04 took the same decision for `guard_wiring` and its reasoning
carries here too: the check runs only when a Definition Version is deleted, which never happens to
one that is active or referenced.

Two things make the case weaker here than it was for `guard_wiring`, which is why this is filed
rather than only noted:

- `negotiation` and `negotiation_resource_link` are **not** small configuration tables. They grow
  with usage, so a scan that is free today is not free indefinitely.
- Both are among the most-written tables in the schema, so an index is not free either. That is the
  other half of why it was left out: paying write cost for a column that is 100% NULL until the data
  cutover backfills it buys nothing.

## Trigger — when this must be revisited

Any one of these turns the deferral into a bug:

- **A query filters or joins either table by `lifecycle_definition_id`.** The likely first one is
  admin tooling answering "which work is pinned to this Definition Version?" before a version is
  retired, or a rollout report counting pinned rows per version.
- **The data cutover sets either column NOT NULL.** After the backfill the column is fully populated
  and selective, which is when an index starts earning its keep; the cutover is also the natural
  place to build it, since it is already rewriting both tables.
- **A Definition Version is ever deleted in an operational path** rather than superseded.

The fix is a new additive migration with a plain `CREATE INDEX` on each column — never an edit to
`V36.4`, which is applied.

## Related

- Slice 04's identical deferral on `guard_wiring`, recorded in the slab `STATUS.md`.
