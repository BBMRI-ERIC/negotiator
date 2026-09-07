# Both structural gates amended, and the three topology fixtures

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Three additive pieces that share one property: each turns a claim this slab makes in prose into a
test that fails when the claim stops being true. None of them touches production code.

**1. The purity gate gains an import allowlist (D18).** Today's rules are blocklists — persistence
packages, `EntityManager` and its kin, any `*Repository` by suffix, and the forbidden edges between
the three packages. A blocklist stops only what someone thought of, and `graph` could today grow an
import of Jackson, of a Spring annotation, or of anything under `common/` with no rule noticing. Add
the rule prototype A wrote and B lacks: **every import in `graph` must start with `java.` or
`lombok.`**. It carries a comment saying why those two and nothing else, because a two-entry
allowlist invites a third.

**2. The definition inertness guard is amended twice, not once.** Prototype A added its own new
package-private type names to the guard's list; prototype B added javadoc recording why
`RequiredAuthority` came off, why `DefinitionScope` stays, and that this is not a precedent for
trimming the list one name at a time. They protect different things — reach, and resistance to
being weakened — so **do both**. Adding the compiler and its input record to the name list turns the
compiler's own javadoc claim, that nothing outside the package may compile a graph, into a tested
rule (D19: the guard survives with an amendment rather than the deletion the map anticipated).

**3. The three topology fixtures both plans assert and neither proves.** A **cycle**, an
**unreachable Legacy State**, and a version with **no terminal State** are all valid graphs and must
stay constructible (user story 50). These are the definitions this subsystem exists to run: a
Lifecycle that can return to an earlier State, a Legacy State kept only so historical rows still
resolve, and a definition nobody has yet given an end. A builder that rejects any of them would
reject production data.

## Acceptance criteria

- [ ] The purity gate refuses any import in `graph` that does not start with `java.` or `lombok.`,
      and the rule's comment says why the allowlist has exactly those two entries.
- [ ] The inertness guard's name list includes the compiler and its input record, so a reference to
      either from outside the definition package fails the gate.
- [ ] The inertness guard carries javadoc recording why `RequiredAuthority` was removed from the
      list, why `DefinitionScope` remains, and that removing a name is not a precedent.
- [ ] Every rule on both gates — the new ones and the pre-existing ones — is proven to fire on the
      thing it forbids **and** proven not to fire on something innocent (user story 71).
- [ ] Both gates fail if the scan finds fewer than its minimum number of sources, so a gate can
      never pass by losing its way (user story 70).
- [ ] The purity gate's minimum-count failure message prints the minimum where the minimum belongs.
      Today a `.formatted(...)` is bound to only the second half of a concatenated literal, so it
      prints the scanned count in the minimum's place. Message only — it never affected the
      pass/fail decision.
- [ ] A graph with a cycle builds and evaluates.
- [ ] A graph with a State no Transition reaches builds, and the State is still declared.
- [ ] A graph with no terminal State builds.
- [ ] All three fixtures assert **acceptance**, so this slice depends on no exception type and can
      land before or after
      [04](04-one-named-exception-for-an-impossible-graph.md).
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

**Anti-vacuity is not paranoia here; it is twice-learned.** `LifecycleEnumDecouplingGuardTest`
reported green over a codebase where every consumer reached the enum through a getter, importing
nothing — an identifier scan that matched no identifier. Slab 07 hit the same shape from the other
side: a detector exercised only against a tree containing no examples goes vacuous silently. Run
every rule red on purpose before trusting it green.

**`DefinitionInertnessGuardTest` is the register to write in**, and its own hardest-won lesson
applies: a guard built only from Java identifiers does not prove a database claim, because a native
query names the table and never the entity. `RawStateNamesInSqlGuardTest` is the precedent for
pinning names the compiler cannot see.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)
