package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Negotiation State names that behaviour outside the Lifecycle depends on <em>existing</em>.
 *
 * <p>A State is a bare {@code String} outside the Lifecycle, because it exists only as a row of a
 * Definition Version and no Java type can enumerate it. Most consumers carry the name as data and
 * need no constant.
 *
 * <p>A name earns a constant here only when some behaviour depends on it existing: the visibility
 * rule that hides {@code DRAFT}, the {@code IN_PROGRESS} State that starts Spawn, and the
 * notifications a requester receives. Do not add the other Negotiation States to make the set look
 * complete. A full list would restate the Definition Version in Java and drift from it.
 *
 * <p>What a State means, such as its label or whether it is terminal, is a lookup against the
 * Definition Version, not a constant.
 *
 * <p>There is one Negotiation-scope Definition Family, so these names cannot go missing. {@link
 * WellKnownResourceStates} cannot say the same.
 */
public final class WellKnownNegotiationStates {

  /**
   * An unsubmitted Negotiation. The Negotiation list query excludes this State so representatives
   * and network viewers do not see unsubmitted work.
   *
   * <p>No Transition leads to {@code DRAFT}. The initial State is {@code SUBMITTED}, and code
   * outside the Lifecycle writes {@code DRAFT} at creation. So "hide the initial State" would hide
   * submitted Negotiations and show drafts.
   */
  public static final String DRAFT = "DRAFT";

  /** A Negotiation submitted for review. Notifies the requester and the reviewers. */
  public static final String SUBMITTED = "SUBMITTED";

  /**
   * An approved, running Negotiation. Entering it starts Spawn for the requested Resources and
   * notifies their representatives.
   */
  public static final String IN_PROGRESS = "IN_PROGRESS";

  /** A Negotiation refused at review. Notifies the requester. */
  public static final String DECLINED = "DECLINED";

  /** A Negotiation given up on. Notifies the requester. */
  public static final String ABANDONED = "ABANDONED";

  private WellKnownNegotiationStates() {
    throw new AssertionError("WellKnownNegotiationStates holds constants and is not instantiable.");
  }
}
