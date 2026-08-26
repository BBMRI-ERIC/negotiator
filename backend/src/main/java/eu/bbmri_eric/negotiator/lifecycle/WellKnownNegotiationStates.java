package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Negotiation State names that behaviour outside the Lifecycle depends on <em>existing</em>.
 *
 * <p>A State is a bare {@code String} everywhere outside the Lifecycle: the column is a {@code
 * VARCHAR}, the wire format is a JSON string, and after ADR 0002 a State exists only as a row of a
 * Definition Version, so no Java type can enumerate one. Almost every consumer therefore carries
 * the name as data - off the column, off the request, off the Definition Version Pin - and needs no
 * constant at all.
 *
 * <p>These five are the exception. Each one is a name some behaviour reaches for by hand: the
 * visibility rule that hides {@code DRAFT} from representatives and network viewers, the {@code
 * IN_PROGRESS} trigger that spawns Resource Lifecycles, and the submitted / declined / abandoned
 * notifications a requester receives.
 *
 * <p><b>This is not the enum with a different name, and must never grow into one.</b> The
 * Negotiation Lifecycle has eight States; five are here because behaviour names them, and the other
 * three are absent because nothing does. A holder that listed all eight would restate in Java the
 * closed universe ADR 0002 exists to delete, and would then have to be kept in sync with a seed it
 * cannot see. The rule is: a name earns a constant only when some behaviour depends on that name
 * existing.
 *
 * <p>Nothing here answers what a State <em>means</em> - its label, its description, whether it is
 * terminal. That is a lookup against the Definition Version, and while Spring Statemachine still
 * runs it is the Enum-Backed Lifecycle Catalog's job.
 *
 * <p>ADR 0004 keeps a single Negotiation-scope Definition Family, so these five names are exactly
 * as stable as the enum constants they replace. {@link WellKnownResourceStates} cannot say the
 * same.
 */
public final class WellKnownNegotiationStates {

  /**
   * An unsubmitted Negotiation. Load-bearing for visibility: the Negotiation list query excludes
   * this State so that work in progress is not exposed to representatives and network viewers.
   *
   * <p>Note that nothing <em>transitions</em> to {@code DRAFT} - the Negotiation graph's initial
   * State is {@code SUBMITTED}, and something outside the Lifecycle writes {@code DRAFT} at
   * creation. So "hide the initial State" is not a valid generalisation of this rule; applied
   * literally it would hide submitted Negotiations and reveal drafts.
   */
  public static final String DRAFT = "DRAFT";

  /** A Negotiation submitted for review. Notifies the requester and the reviewers. */
  public static final String SUBMITTED = "SUBMITTED";

  /**
   * An approved Negotiation that is now running. Triggers the spawn of one Resource Lifecycle per
   * requested Resource, and the notification of their representatives.
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
