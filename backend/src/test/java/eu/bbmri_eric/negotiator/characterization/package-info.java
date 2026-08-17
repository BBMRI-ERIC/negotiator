/**
 * The Lifecycle parity gate: today's observable behaviour, pinned so the redesign can prove it
 * preserved it.
 *
 * <p>Every assertion here names States and Events as <strong>string literals</strong> and reaches
 * the two Lifecycle services through {@code adapter.LifecycleTestAdapter}, the single test-scope
 * file permitted to name the four Lifecycle enums. At cutover the adapter's implementation is
 * rewritten once and every assertion stays byte-identical. {@code
 * guard.CharacterizationImportGuardTest} enforces that mechanically; {@code dump} is the one
 * package allowed to touch Spring Statemachine and is deleted along with the library.
 *
 * <h2>Running it</h2>
 *
 * <p>The suite is two halves, and they must be reported separately. Both commands run from the
 * repository root; {@code java} and {@code mvn} are absent from an agent session's {@code PATH}, so
 * the {@code nix develop} prefix is required, and the script is <em>not</em> at the repository root
 * — it ships with the {@code focused-backend-tests} skill.
 *
 * <p><strong>Parity — must be green. 255 tests in 24 classes, 0 failures, 1 skipped</strong> (the
 * skip is {@code dump.LifecycleGraphDumpGeneratorTest}, which only writes under {@code
 * -Dlifecycle.dump.regenerate=true}):
 *
 * <pre>
 * nix develop .#opencode --command \
 *   /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
 *   -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
 * </pre>
 *
 * <p><strong>Intended deltas — expected to invert at cutover. 8 tests</strong>, all in {@code
 * delta.IntendedDeltasAdr0005WillInvertTest}. A red test there is the cutover succeeding, not a
 * regression:
 *
 * <pre>
 * nix develop .#opencode --command \
 *   /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
 *   -f backend 'eu.bbmri_eric.negotiator.characterization.**' -Dgroups=intended-delta
 * </pre>
 *
 * <p>Dropping both flags runs everything — 263 tests in 25 classes — which is the cross-package
 * test-ordering check. Read the numbers out of {@code target/surefire-reports/}, and check each
 * report's mtime is after the run started: an aborted run leaves stale reports behind.
 *
 * <h2>The ordering rule</h2>
 *
 * <p>Any class here that fires an Event must declare {@code @DirtiesContext(classMode =
 * AFTER_EACH_TEST_METHOD)}. Flyway cleans and migrates on every context build, so dirtying after
 * each method restores the seed for whoever runs next. The corpus is shared and the Information
 * Requirement gate's lookup is global, so a driving class that does not dirty turns another class
 * red by ordering alone — which no single class's own run reveals.
 *
 * <h2>Provenance</h2>
 *
 * <p>Built by the "freeze current behaviour" slab of the state-machine-implementation effort, with
 * no production code changed. The gate's full documentation, the empirical findings and the honest
 * coverage gaps live in {@code .scratch/state-machine-implementation/parity-gate.md} and {@code
 * before-picture-findings.md} beside it.
 */
package eu.bbmri_eric.negotiator.characterization;
