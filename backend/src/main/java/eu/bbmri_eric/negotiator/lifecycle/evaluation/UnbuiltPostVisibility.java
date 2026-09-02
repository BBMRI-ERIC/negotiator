package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import org.springframework.stereotype.Component;

/**
 * The placeholder behind {@link PostVisibility} until the slab that runs Actions wires the real
 * one. It refuses.
 *
 * <p>Issue 09 excludes anything that writes, so there is nothing here to implement yet: the three
 * setters this stands in for live on {@code NegotiationService}, and reaching them is the cutover
 * slab's. Refusing rather than doing nothing means that wiring the Action chain before that happens
 * is a failed request rather than a Transition that silently stops changing post visibility.
 *
 * <p>Deleting this class is how that slab announces itself.
 */
@Component
class UnbuiltPostVisibility implements PostVisibility {

  @Override
  public void setPublicPostsEnabled(String negotiationId, boolean enabled) {
    throw refusal("public", negotiationId, enabled);
  }

  @Override
  public void setPrivatePostsEnabled(String negotiationId, boolean enabled) {
    throw refusal("private", negotiationId, enabled);
  }

  private static UnsupportedOperationException refusal(
      String which, String negotiationId, boolean enabled) {
    return new UnsupportedOperationException(
        ("Running Actions is not built yet. Asked to set %s posts to %s on Negotiation %s. "
                + "Replace this bean with the real one before running an Action chain.")
            .formatted(which, enabled, negotiationId));
  }
}
