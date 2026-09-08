package eu.bbmri_eric.negotiator.lifecycle.evaluation;

/**
 * Sets a Negotiation's two post-visibility flags, which is all {@code SET_POST_VISIBILITY} needs.
 *
 * <p>It is narrow so the Action cannot reach every negotiation operation and can be tested without
 * a {@code NegotiationService}.
 */
interface PostVisibility {

  void setPublicPostsEnabled(String negotiationId, boolean enabled);

  void setPrivatePostsEnabled(String negotiationId, boolean enabled);
}
