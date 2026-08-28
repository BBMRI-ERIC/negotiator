# The parity gate

**Stage 1's gate, in two commands.** Standing decision 1 of [the map](map.md) says tests written
against the current Spring Statemachine behaviour must pass **unchanged** against the new subsystem.
This file is the operational form of that decision: the commands, the numbers they must produce, and
what a red test means in each half.

Written by the [Freeze current behaviour](issues/01-freeze-current-behaviour.md) slab, which built the
suite. It deliberately lives next to the map rather than inside
`.scratch/freeze-current-behaviour/`, because that slab's `STATUS.md` is meant to be deleted when the
slab closes and the gate outlives it. The findings the slab produced are in
[before-picture-findings.md](before-picture-findings.md).

## Half one — parity. This must be green.

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
```

**Expected: 255 tests in 24 classes, 0 failures, 0 errors, 1 skipped.** About 8.5 minutes.

The one skip is deliberate and permanent: `dump.LifecycleGraphDumpGeneratorTest` writes the committed
graph artifacts and only runs under `-Dlifecycle.dump.regenerate=true`. A run reporting 0 skipped has
either lost that class or accidentally armed it.

**A red test here is a behaviour change.** Either fix the production change, or argue that the
behaviour *should* change and move the test into the delta package below — never edit the assertion
in place.

## Half two — intended deltas. These are *expected* to go red at cutover.

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -Dgroups=intended-delta
```

**Expected today: 8 tests, 0 failures, 0 errors, 0 skipped.**

All eight are `characterization.delta.IntendedDeltasAdr0005WillInvertTest`. They assert **today's**
behaviour for the two things ADR 0005 deliberately changes, so **a red test in this class is the
cutover succeeding**. Four of the eight are the ones designed to invert (available events including
unfireable ones; the array-valued rels; the display name; the creator seeing the hint); the others
state things ADR 0005 keeps, and each method's javadoc says which it is. Do not delete the class
wholesale on the assumption that everything in it flips.

Dropping both flags runs everything — **263 tests in 25 classes, 0 failures, 0 errors, 1 skipped** —
which is the cross-package test-ordering check.

## Why the two halves exist, and how the split is implemented

Standing decision 1's carve-out: a pure-parity suite would pin the very bugs ADR 0005 exists to fix,
so those assertions must not be counted as parity. They carry the JUnit tag `intended-delta`
(the compile-time constant `IntendedDeltasAdr0005WillInvertTest.INTENDED_DELTA`, referenced by the
`@Tag` annotation itself, so the annotation and these flags cannot drift).

`maven-surefire-plugin` 3.5.5 reads `groups` / `excludedGroups` as plain user properties and composes
them with `-Dtest=`, so no POM change was needed. The tagged class stays *inside* the
`characterization` tree on purpose: the forbidden-reference guard resolves its scan root from
`src/test/java/eu/bbmri_eric/negotiator/characterization` and walks it whole, so a class moved outside
would silently escape the string-and-adapter rule — the property those tests need most, since they
are the ones a later session edits.

**Verify the split by which surefire reports get written, not by a pass count.** With the exclusion,
no report exists for the delta class; with `-Dgroups`, no report exists for anything else.

## Environment, twice bitten

- **The Nix dev shell is not active in an agent session.** `java`, `mvn` and `JAVA_HOME` are absent
  from `PATH`, so a bare `mvn` — or a direct call to the script — fails with command-not-found. Every
  Maven command runs from the repository root as `nix develop .#opencode --command <command>`. That
  gives JDK 21 and Maven 3.9.12. It prints `warning: Git tree ... is dirty` to stderr while there is
  uncommitted work, which is **not** an error, and the first invocation is slower than the rest.
  **Both halves of that stopped being true during slab 07.** `nix develop` began failing outright
  with `error: setting up a private mount namespace: Operation not permitted` — the sandbox refusing
  Nix its namespaces, from every directory, unrelated to this repository — and it turned out not to
  be needed, because `mvn` and `java` *were* on `PATH` (Maven 3.9.16, OpenJDK 21.0.12). Try the
  prefix first, since it is what this document specifies and it may work again; if it fails, drop it
  and run the command bare rather than treating it as a blocker.
- **There is no `scripts/test-backend.sh` at the repository root.** Several early tickets recorded
  that path; it exits 127. The script ships with the `focused-backend-tests` skill, at the absolute
  path used above.
- **Read the numbers out of `backend/target/surefire-reports/`, not out of a summary line.** Check
  that every report's mtime is *after* the run started — an aborted run leaves stale reports behind
  that will otherwise be counted.
- **Sum the `TEST-*.xml` reports, not the `.txt` ones.** Surefire's plain-text writer reports `Tests
  run: 0` for a class that has an `@Nested` inner class, while the XML records its real count. Today
  exactly one class in the tree does that — `unit.mappers.RequestModelMapperTest`, 4 tests — so every
  `.txt`-based whole-suite figure recorded before slab 07 closed is four low. **The two gate numbers
  above are unaffected**: no characterization class uses `@Nested`, and the `.txt` and XML sums were
  compared class by class to confirm it. Found the slow way — three slices of slab 07 logged the same
  four-test gap as unattributed before anyone compared the two writers.
- `backend/target/` gets polluted by the JDT language server compiling without Lombok, which presents
  as ~200 bogus `cannot find symbol` errors in unrelated test files. One `clean` clears it.
- Testcontainers needs docker group membership; `postgres:16-alpine` and `testcontainers/ryuk:0.12.0`
  are pulled locally. `MailConnectException` noise in the output is expected — there is no SMTP server
  in the test environment — and is not a failure.

## The rule that makes the full-selector run necessary

**Any characterization class that fires an Event must declare
`@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`.** The Flyway strategy is clean-and-migrate on
every context build, so dirtying after each method restores the seed for whoever runs next. The corpus
is shared, and shared in ways that bite: `NegotiationAuthorityParityTest` reads `negotiation-1`
expecting `IN_PROGRESS`, and driving `negotiation-1`'s only Resource to a terminal State concludes
that Negotiation. The Information Requirement gate's lookup is global, so a leaked
`information_requirement` row blocks that Event for the remainder of the run.

A driving class that does not dirty turns another class red by test ordering alone, which no single
class's own run reveals. Read-only classes may use `BEFORE_CLASS` or `AFTER_CLASS`. **The
full-selector run is the only check** — never conclude the gate is green from a single class.

## What this gate does not cover

Stated so a later session does not over-trust it. Full reasoning in
[before-picture-findings.md](before-picture-findings.md).

- **The frontend.** Standing decision 5: repairs ride in the slab that breaks them, verified by hand.
  There is no frontend unit-test runner at all.
- **The two ADR 0005 deltas**, by construction — they are the other half.
- **Anything reachable only through Spring Statemachine internals.** The `characterization.dump`
  package is the sanctioned exception and is deleted at cutover with the library.
- **The `minimal-workflow` Spring profile** (`db9019d4` on `master`, not on this branch). Deliberate
  and needs no follow-up: it exists only because Lifecycles are not yet customizable, declares no new
  State, and is replaced during the rollout.
- **The second `ResourceStateChangeEvent` producer's own seam** — the override's authorisation rule,
  its `DRAFT` branch and its `NewResourcesAddedEvent` branch. The override *path* is pinned; the
  governance service's rules around it are not.
- Several behaviours are **documented rather than asserted**, because asserting them would name a type
  the redesign deletes or would pin a manufactured fixture. They are enumerated in the findings report
  and a cutover that changes one breaks no test.

## Regenerating the graph dump

Only while Spring Statemachine still lives. `LifecycleGraphDumpDriftTest` regenerates the two graph
JSON files and the Mermaid diagram from the live beans on every run and asserts byte equality against
the committed copies under `backend/src/test/resources/lifecycle/`, so the artifacts cannot drift. To
rewrite them deliberately:

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'LifecycleGraphDumpGeneratorTest' -Dlifecycle.dump.regenerate=true
```

## Formatter

Not bound to the `test` phase, so run it before committing any Java or it reformats someone else's
diff later:

```
nix develop .#opencode --command mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format
```
