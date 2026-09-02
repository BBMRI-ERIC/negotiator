package eu.bbmri_eric.negotiator.lifecycle.evaluation;

/**
 * Setting a Negotiation's two post-visibility flags — the whole of what {@code SET_POST_VISIBILITY}
 * needs from the rest of the application, and deliberately no more.
 *
 * <p>Narrow because the alternative is a {@code NegotiationService}, which is how the three Action
 * classes this replaces reach the same two setters today: each one is a {@code @Component} holding
 * an {@code @Autowired @Lazy NegotiationService} and calling one or two methods on it. Injecting a
 * whole service into a strategy would put every negotiation operation within an Action's reach and
 * make the Action untestable without one.
 */
interface PostVisibility {

  void setPublicPostsEnabled(String negotiationId, boolean enabled);

  void setPrivatePostsEnabled(String negotiationId, boolean enabled);
}
