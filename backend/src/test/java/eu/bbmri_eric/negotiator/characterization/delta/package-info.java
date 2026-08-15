/**
 * Behaviour this slab pins <em>so that the cutover can change it</em> - the opposite of every other
 * package in the characterization tree.
 *
 * <p>Everything under {@code characterization.service}, {@code characterization.rest} and {@code
 * characterization.dump} is parity: it states what must survive the Lifecycle redesign unchanged,
 * and a red test there is a regression. This package states the two behaviours ADR 0005 exists to
 * fix. Pinning them as parity would have frozen the bugs, so they are pinned here instead,
 * asserting <b>today's</b> behaviour with each test naming the post-cutover behaviour that should
 * replace it. A red test here is the cutover working.
 *
 * <p>The separation is mechanical, not editorial. Every test in this package carries the JUnit tag
 * {@code intended-delta}, so the parity gate excludes them with {@code
 * -DexcludedGroups=intended-delta} and reports them separately with {@code
 * -Dgroups=intended-delta}. The package still sits inside the characterization tree deliberately:
 * the forbidden-reference guard walks the whole tree, so the string-and-adapter rule covers these
 * tests exactly as it covers the parity ones.
 *
 * @see eu.bbmri_eric.negotiator.characterization.delta.IntendedDeltasAdr0005WillInvertTest
 */
package eu.bbmri_eric.negotiator.characterization.delta;
