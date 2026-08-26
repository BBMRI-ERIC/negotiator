# Entities and JPA queries name States as strings

Status: ready-for-agent

## Parent

[PRD — Decouple consumers from the Lifecycle enums](../PRD.md), for map ticket
[07](../../state-machine-implementation/issues/07-decouple-consumers-from-enums.md).

## What to build

The last and hardest migrate step: the two entities holding a Lifecycle's current State, the
repository that queries them, the specification that filters a Negotiation list, and the filter DTO
that carries a status from the request.

The two state fields become `String` and lose their enum mapping. **No Flyway migration** — both
columns are already `VARCHAR` and hold the name today.

The entity fields and the Criteria predicates must move **in the same slice**. A predicate comparing
an enum-mapped path against a `String` is not reliably coercible, so splitting them would leave a
commit whose behaviour depends on Hibernate's willingness to guess.

The filter's status list becomes a list of names, validated against slice 2's catalog so its 400 on
an unknown value survives exactly.

The Negotiation list's visibility rule keeps excluding drafts **by name**, as a single-column
comparison on a hot query. Ticket 03 settled this and killed the tempting generalisation: "hide the
initial State" would be elegant and fails closed, but the initial State is not the draft one and
nothing targets the draft State at all, so applied literally it would hide submitted Negotiations
and **reveal drafts** — exactly backwards.

## Acceptance criteria

- [ ] Both entities, the repository, the specification and the filter DTO name no Lifecycle enum.
- [ ] No Flyway migration is added and no stored value changes.
- [ ] A test written **before** the change pins today's 400 for an unknown status filter value, and
      passes unchanged after it. Ticket 03 established that nothing pins this today.
- [ ] A test written **before** the change pins the status text rendered in the Negotiation PDF
      summary, and passes unchanged after it.
- [ ] The Negotiation list returns identical results for every combination of role, network,
      organization, status filter and search that the suite exercises.
- [ ] Draft Negotiations remain invisible to representatives and to network viewers.
- [ ] Every repository query returns the same rows for the same data, and slice 3's guard stays green
      without amendment.
- [ ] The existing repository, controller and model tests are extended rather than replaced.
- [ ] Full backend suite green; parity 255/24/1 skipped; deltas 8/0/0/0.

## Notes

**The PDF pin is the one that catches a silent change.** Two places put the Negotiation's State into
a template variable that a template renders directly. Neither names an enum type, so both compile
clean either way and the guard cannot see them. The rendered text is expected to be identical,
because a Java enum renders as its name and so does the string that replaces it — that was checked
during recon, not assumed. Only a test written first distinguishes "still correct" from "silently
changed".

**The visibility rule fails open, and that is worth understanding before touching it.** A missing
Well-known State is a silent no-op for notification — nobody is told. Here a missing draft name
means the exclusion excludes nothing and unsubmitted Negotiations become visible. Stage 1 is safe by
construction: ADR 0004 keeps one Negotiation-scope definition and ADR 0009's seed is a faithful
transcription. Recorded, not ticketed, because nothing in this map's destination can make it real.

**One repository query fails loudly and one fails silently.** The unquoted JPQL reference breaks at
Hibernate query validation once the field is a string, which is the good case. The two names in the
same file's native query do not — slice 3 pinned them for exactly this reason.

## Blocked by

- [01 The three Well-known name holders](01-well-known-name-holders.md)
- [03 Pin the raw State names in SQL](03-pin-the-raw-state-names-in-sql.md)
- [05 Notification handlers name States as strings](05-notification-handlers-name-states-as-strings.md)
- [08 DTOs, mappers and the Negotiation timeline](08-dtos-mappers-and-the-negotiation-timeline.md)
- [09 Resource governance names States as strings](09-resource-governance-names-states-as-strings.md)
