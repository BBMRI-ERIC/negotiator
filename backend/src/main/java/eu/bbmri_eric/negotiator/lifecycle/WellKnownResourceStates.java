package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Resource State names that behaviour outside the Lifecycle depends on <em>existing</em>.
 *
 * <p>Spawn writes these by hand rather than through a Transition: {@code REPRESENTATIVE_CONTACTED}
 * if the Resource has a representative to notify, {@code REPRESENTATIVE_UNREACHABLE} if it has
 * none. {@code SUBMITTED} is the initial State and the default a request carries when it names no
 * State.
 *
 * <p>The growth rule of {@link WellKnownNegotiationStates} applies. Do not add other Resource
 * States to make the set look complete.
 *
 * <p>Unlike the Negotiation names, these can go missing. A custom Resource-scope Definition Family
 * may have no {@code REPRESENTATIVE_UNREACHABLE}, and nothing here guards against that. The case
 * arises only once a second Resource-scope family exists.
 */
public final class WellKnownResourceStates {

  /**
   * The initial State of the Resource Lifecycle, and the default a request carries when it names no
   * State. The default is wrong for any family that lacks this State.
   */
  public static final String SUBMITTED = "SUBMITTED";

  /**
   * Spawn writes this to a Resource whose representatives it notified. The "nobody ever responded"
   * network statistic counts it.
   */
  public static final String REPRESENTATIVE_CONTACTED = "REPRESENTATIVE_CONTACTED";

  /**
   * Spawn writes this to a Resource with no representative to notify. The "nobody ever responded"
   * network statistic counts it too.
   */
  public static final String REPRESENTATIVE_UNREACHABLE = "REPRESENTATIVE_UNREACHABLE";

  private WellKnownResourceStates() {
    throw new AssertionError("WellKnownResourceStates holds constants and is not instantiable.");
  }
}
