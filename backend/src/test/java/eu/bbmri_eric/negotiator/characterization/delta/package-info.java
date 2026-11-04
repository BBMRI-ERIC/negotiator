/**
 * Behaviour pinned so that a later change can replace it, unlike the rest of the characterization
 * tree, where a red test is a regression. A red test here means the intended change landed.
 *
 * <p>Every test here carries the JUnit tag {@code intended-delta}, so the parity run excludes them
 * with {@code -DexcludedGroups=intended-delta} and runs them alone with {@code
 * -Dgroups=intended-delta}. The package stays inside the characterization tree so the import guard
 * covers it too.
 *
 * @see eu.bbmri_eric.negotiator.characterization.delta.RequirementHintLinksIntendedChangeTest
 */
package eu.bbmri_eric.negotiator.characterization.delta;
