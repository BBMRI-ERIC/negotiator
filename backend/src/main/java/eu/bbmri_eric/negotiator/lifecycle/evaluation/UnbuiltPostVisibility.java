package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import org.springframework.stereotype.Component;

/**
 * Placeholder for {@link PostVisibility} until Actions run in production. Replace it with an
 * implementation that calls {@code NegotiationService}.
 *
 * <p>It throws instead of doing nothing, so an Action that runs too early fails the request instead
 * of skipping the post-visibility change unnoticed.
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
