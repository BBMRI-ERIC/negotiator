package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Resource State names that behaviour outside the Lifecycle depends on <em>existing</em>.
 *
 * <p>All three are here for one reason: they are what Spawn writes. When a Negotiation is approved,
 * each requested Resource has its Lifecycle started, and is given a State by hand rather than by a
 * Transition - {@code REPRESENTATIVE_CONTACTED} if the Resource has a representative to notify,
 * {@code REPRESENTATIVE_UNREACHABLE} if it has none. {@code SUBMITTED} is the State a Resource
 * Lifecycle starts in and the default a request carries when it names no State.
 *
 * <p>The same growth rule applies as to {@link WellKnownNegotiationStates}: the Resource Lifecycle
 * has twelve States, three are here because behaviour names them, and the other nine are absent
 * because nothing does. Do not add a fourth to make the set look complete - a holder that lists
 * every State is the enum again with worse ergonomics.
 *
 * <p><b>Hazard, recorded rather than solved.</b> {@link WellKnownNegotiationStates} is safe because
 * ADR 0004 keeps a single Negotiation-scope Definition Family, so those names cannot go missing.
 * Resource scope is precisely the scope that diverges once custom Definition Families ship, and
 * these three names are a bet on a family's vocabulary rather than on the model: a custom family
 * may reasonably have no {@code REPRESENTATIVE_UNREACHABLE}. Nothing in stage 1 can make that bet
 * lose - there is one seeded Resource family and the seed is a faithful transcription of the enum -
 * so this is noted here for whoever ships the second one, not fixed here.
 */
public final class WellKnownResourceStates {

  /**
   * The initial State of the Resource Lifecycle, and the default a request carries when it names no
   * State.
   *
   * <p>The default is worth flagging for the same reason as the hazard above: a default that names
   * a Resource State is wrong for any family that lacks it.
   */
  public static final String SUBMITTED = "SUBMITTED";

  /**
   * Written by Spawn to a Resource whose representatives were notified of the new request. Also one
   * half of the "nobody ever responded" network statistic.
   */
  public static final String REPRESENTATIVE_CONTACTED = "REPRESENTATIVE_CONTACTED";

  /**
   * Written by Spawn to a Resource with no representative in the system, so there was nobody to
   * notify. The other half of "nobody ever responded".
   */
  public static final String REPRESENTATIVE_UNREACHABLE = "REPRESENTATIVE_UNREACHABLE";

  private WellKnownResourceStates() {
    throw new AssertionError("WellKnownResourceStates holds constants and is not instantiable.");
  }
}
