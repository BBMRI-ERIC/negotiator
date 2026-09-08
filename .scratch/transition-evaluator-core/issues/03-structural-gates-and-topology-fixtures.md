# Both structural gates amended, and the three topology fixtures

Status: resolved

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

- [x] The purity gate refuses any import in `graph` that does not start with `java.` or `lombok.`,
      and the rule's comment says why the allowlist has exactly those two entries.
- [x] The inertness guard's name list includes the compiler, its input record and the
      compiled-graph cache, so a reference to any of them from outside the definition package fails
      the gate. The cache is the addition [07](07-compiled-graph-cache.md) made after this criterion
      was written: it landed the type but deliberately left the name list to this slice, so that
      the two slices did not edit one list in parallel. It is the only new definition type not
      already covered here, and nothing else owns the omission.
- [x] The inertness guard carries javadoc recording why `RequiredAuthority` was removed from the
      list, why `DefinitionScope` remains, and that removing a name is not a precedent.
- [x] Every rule on both gates — the new ones and the pre-existing ones — is proven to fire on the
      thing it forbids **and** proven not to fire on something innocent (user story 71).
- [x] Both gates fail if the scan finds fewer than its minimum number of sources, so a gate can
      never pass by losing its way (user story 70).
- [x] The purity gate's minimum-count failure message prints the minimum where the minimum belongs.
      Today a `.formatted(...)` is bound to only the second half of a concatenated literal, so it
      prints the scanned count in the minimum's place. Message only — it never affected the
      pass/fail decision.
- [x] A graph with a cycle builds and evaluates.
- [x] A graph with a State no Transition reaches builds, and the State is still declared.
- [x] A graph with no terminal State builds.
- [x] All three fixtures assert **acceptance**, so this slice depends on no exception type and can
      land before or after
      [04](04-one-named-exception-for-an-impossible-graph.md).
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
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

## Outcome

Four test files changed, no production file touched — which is what the slab gate and this issue's
own "None of them touches production code" both asked for. Two commits: the three pieces, then the
review's fixes.

### 1. The purity gate's allowlist

`IMPORTS_PERMITTED_IN_GRAPH = List.of("java.", "lombok.")`, with the argument for each entry
written beside the list rather than assumed, because a two-entry allowlist invites a third: the JDK
because a compiled graph must stay constructible by a compile, by a test, or by whatever one day
reads a definition file; Lombok because its annotations are `SOURCE` retention at `provided` scope,
so the property the gate protects stays literally true of the compiled output. The javadoc also
records the near miss — `org.springframework.lang.Nullable`, which D11 turned down as a real runtime
dependency — so the next person widening the list has the shape of the argument in front of them.
A's rule passed on B's tree unchanged, as D18 predicted: `graph`'s twelve sources import only
`java.util.*` and four Lombok annotations.

**Two things beyond the letter of the criteria, both closing a vacuity this slice is specifically
about.** The gate's three pre-existing rules became named predicates, because the anti-vacuity test
had been asserting against *copies* of two rules' regexes rather than against the rules — the exact
shape `LifecycleEnumDecouplingGuardTest` was green under. And the graph package got its own
minimum-source floor: two of the four rules read `graph` alone, and the combined 28-source count
would have looked healthy while those two went vacuous over an empty walk.

### 2. The inertness guard, amended

`DefinitionCompiler`, `DefinitionVersionRows` and `CompiledGraphCache` joined
`DISTINCTIVE_TYPE_NAMES`, with javadoc recording why package-private types belong on a name list at
all: the package rule already catches the only spelling that would *compile*, and what the names add
is the three it cannot catch — a type name in a string, a reflection lookup, and a later slice that
makes one public and drops it into a signature. That is what turns `DefinitionCompiler`'s own
javadoc claim, "nothing outside this package may compile a graph yet", into a rule that reds when it
stops being true. `CompiledGraph` is recorded as deliberately *not* listed, since the near-collision
with `CompiledGraphCache` is exactly the kind of thing a later reader would otherwise ask about.

The cache is [07](07-compiled-graph-cache.md)'s deferral, taken here as its outcome asked.

**The javadoc criterion was already met before this slice started.** The paragraphs on why
`RequiredAuthority` came off, why `DefinitionScope` stays, and that removing a name is not a
precedent were all in prototype B's tree and landed with slice 01's adoption of it. Verified against
`git show 75a4eb8e:…/DefinitionInertnessGuardTest.java` rather than assumed. The criterion is met;
it is not met *by this diff*, and the issue was wrong to expect it as work — D18 says B added that
javadoc, and B's tree is what slice 01 adopted.

### 3. The three topology fixtures

`build_whenTheTransitionsFormACycle_isAccepted`,
`build_whenNoTransitionReachesAState_isAcceptedAndTheStateStaysDeclared` and
`build_whenNoStateIsTerminal_isAccepted` in `CompiledGraphTest`, all asserting acceptance, so this
slice depends on no exception type and could have landed either side of
[04](04-one-named-exception-for-an-impossible-graph.md).

The unreachable-State fixture asserts more than that the build succeeds: it asserts `isTerminal` and
`transitionsFrom` *answer* for the Legacy State rather than throwing, because being declared is the
whole of what makes a Legacy State useful — a historical row sitting in one has to resolve. The
no-terminal fixture records the asymmetry it depends on: an initial State is what starts a
Lifecycle, so `build()` requires exactly one; a terminal State is not needed to run one, so it
requires none.

The cycle's "and evaluates" half is `evaluate_whenTheGraphHasACycle_permitsBothDirections` in
`TransitionEvaluatorTest`, which is where a graph is run rather than only built.

### Anti-vacuity: what was actually run red

The Notes are the reason this section exists, and they were right to insist.

| Rule or fixture | Run red against |
| --- | --- |
| The allowlist | `import org.springframework.lang.Nullable;` in `graph` — one violation, one `file:line` |
| The allowlist, wildcard form | `import org.springframework.stereotype.*;` in `graph`, with `java.util.function.*` confirmed still green |
| No edge into `definition` | a real definition-package reference in a real scanned `evaluation` source |
| `graph` does not know `evaluation` | a real evaluation-package reference in a real scanned `graph` source |
| The three new names | string references to all three from `TransitionEvaluator` — three violations, three `file:line` |
| The stale-name net | a bogus `DefinitionCompilerThatWasRenamed` on the list |
| Purity's combined floor | raised to 999 against 28 sources |
| Purity's graph floor | raised to 999 against 12 sources |
| Inertness's floor | raised to 99999 against 467 sources |
| Cycle, both halves | a builder taught to refuse a mutually inverse pair of edges |
| Unreachable Legacy State | a builder taught to require every State reachable |
| No terminal State | a builder taught to require at least one terminal State |

Each rule fired on the thing it forbids **and** only on it: every probe reported exactly the rule
being probed, never a neighbour. Every probe was reverted by `git checkout`, and the final tree has
no production diff at all.

### The defect the criteria named, and one they did not

The `.formatted(...)` was bound to only the second half of a concatenated literal, so the
minimum-count message printed the scanned count where the minimum belongs. Parenthesized; it now
reads "found only 28 … expected at least 999" under probe. Message only, and it never affected the
pass/fail decision, exactly as the criterion says.

**The one the criteria did not name was in this slice's own new rule.** `IMPORT_STATEMENT` captured
`[\w.$]+`, which does not match an asterisk — so `import com.fasterxml.jackson.databind.*;` in the
graph package matched no import at all and **passed silently**. An allowlist with the one hole an
allowlist exists to not have, and not a hypothetical one: the wildcard form is forbidden by Google
Java Style, *unenforced* by `fmt-maven-plugin`, and this backend has four in `src/main/java` today.
Found by re-reading the rule after the review rather than by the review, which is worth recording —
a new rule deserves the same adversarial read as the code it guards.

### The review's other findings

The evaluator test's name and javadoc said the evaluator "runs" the cycle. `backend/CONTEXT.md` and
ADR 0001 both reserve committing a move and running its Actions to the services around it, and the
test only calls `evaluate` and `possibleEvents`; it permits, and now says so.

The purity guard borrows a rule predicate's name from a guard whose copy-rather-than-extract licence
reads "each is meant to be deleted whole" — a premise that cannot cover a guard whose own javadoc
says it is *not* meant to be deleted. It now states its own reason instead of inheriting one.

The Lombok entry's argument has one exception worth knowing at the place the allowlist would be
widened: `lombok.extern` generates code naming a real runtime logger. It has no business in a
vocabulary package regardless, but no rule would stop it.

One finding was **declined**: that the purity gate's single anti-vacuity method, now ~25 assertions
over four rules, should become `@Nested` per rule. That method's shape is deliberately the sibling
guard's, which this class's javadoc says it is built in the register of, and diverging from it for
tidiness would cost the correspondence the two guards are meant to keep.

### Gates

The lifecycle package is green at **243 tests in 21 classes, 0 failures, 0 errors, 0 skipped** —
`EvaluatorPurityGuardTest` 4 → 6, `CompiledGraphTest` 25 → 28, `TransitionEvaluatorTest` 28 → 29,
`DefinitionInertnessGuardTest` unchanged at 6 (its additions are assertions inside existing tests).
`fmt-maven-plugin:check` clean.

**The parity half is unchanged at 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped —
exact.** The one skip is `dump.LifecycleGraphDumpGeneratorTest`, as the gate specifies, and no
report exists for `delta.IntendedDeltasAdr0005WillInvertTest`, which is how the gate says to verify
the tag split rather than by a pass count. Read by summing the `TEST-*.xml` reports, not the `.txt`
ones, and `surefire-reports/` was deleted before the run so no stale report could be counted — both
traps the gate document names.

It took four attempts, none of them the diff's fault, and the reason is worth leaving here for the
next slice. Two other sessions held Maven in other worktrees for most of this slice; slice 06's
lesson is that the parity gate runs as the only Maven process, so the run was queued behind them by
a script that polls for a clear tree. That waiter was killed twice by system-wide memory pressure —
once while merely waiting, once mid-run at 6 of 24 classes — because two concurrent Maven and
Testcontainers runs on a 15 GB box do not fit. **The queue-and-wait approach was right and worth
keeping; what it needs is to survive being killed.** The successful run went green on the first
attempt once it had the machine to itself.

### For whoever deletes this guard

The inertness guard is still meant to be deleted **whole**, by the cutover slab, as a visible line
in its diff. This slice added three names to its list and none of that changes: a name comes off
only when its type has genuinely left the package, and the list is now longer precisely because
more of the package exists to keep inert.
