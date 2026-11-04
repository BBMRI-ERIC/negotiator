/**
 * Today's observable Lifecycle behaviour, pinned so a redesign can prove it kept it.
 *
 * <p>Every assertion names States and Events as string literals and reaches the two Lifecycle
 * services through {@code adapter.LifecycleTestAdapter}, the one test-scope file allowed to name the
 * four Lifecycle enums. Replacing the enums then means rewriting the adapter only, with every
 * assertion unchanged. {@code guard.CharacterizationImportGuardTest} enforces the rule; {@code dump}
 * is the one package allowed to touch Spring Statemachine.
 *
 * <h2>Running it</h2>
 *
 * <p>Report the two halves separately. Both commands run from the repository root. The {@code nix
 * develop} prefix puts {@code java} and {@code mvn} on the {@code PATH}, and the script ships with
 * the {@code focused-backend-tests} skill, not the repository.
 *
 * <p><strong>Parity, must be green. 255 tests in 24 classes, 0 failures, 1 skipped.</strong> The
 * skip is {@code dump.LifecycleGraphDumpGeneratorTest}, which only writes under {@code
 * -Dlifecycle.dump.regenerate=true}.
 *
 * <pre>
 * nix develop .#opencode --command \
 *   /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
 *   -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
 * </pre>
 *
 * <p><strong>Intended changes, expected to go red when they land. 8 tests</strong>, all in {@code
 * delta.RequirementHintLinksIntendedChangeTest}:
 *
 * <pre>
 * nix develop .#opencode --command \
 *   /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
 *   -f backend 'eu.bbmri_eric.negotiator.characterization.**' -Dgroups=intended-delta
 * </pre>
 *
 * <p>Dropping both flags runs all 263 tests in 25 classes, which checks test ordering across
 * packages. Read the numbers from {@code target/surefire-reports/} and check that each report is
 * newer than the run's start, because an aborted run leaves stale reports behind.
 *
 * <h2>The ordering rule</h2>
 *
 * <p>Any class here that fires an Event must declare {@code @DirtiesContext(classMode =
 * AFTER_EACH_TEST_METHOD)}. Flyway cleans and migrates on every context build, so dirtying after
 * each method restores the seed for the next test. The seed data is shared and the Information
 * Requirement gate's lookup is global, so a class that fires without dirtying can turn another class
 * red by ordering alone, which no single class's own run shows.
 */
package eu.bbmri_eric.negotiator.characterization;
