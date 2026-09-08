package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns a Negotiation's post visibility on or off, for public posts, private posts, or both.
 *
 * <p>{@code BOTH} lets the abandon Transition, which hides both kinds of post, use one Wiring row
 * instead of two.
 *
 * <p>All configurations share one bean and one type key and differ only in {@code params}. Several
 * beans with the same key would collide in {@link ActionRegistry} and fail the boot.
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class SetPostVisibilityAction implements Action<SetPostVisibilityAction.Params> {

  static final String TYPE_KEY = "SET_POST_VISIBILITY";

  /** Which flags to set, and to what. */
  record Params(Scope scope, boolean enabled) {}

  enum Scope {
    PUBLIC,
    PRIVATE,
    BOTH
  }

  private final PostVisibility postVisibility;

  @Override
  public String typeKey() {
    return TYPE_KEY;
  }

  @Override
  public Class<Params> paramsType() {
    return Params.class;
  }

  @Override
  public void run(ActionContext context, Params params) {
    String negotiationId = context.evaluation().subject().negotiationId();
    switch (params.scope()) {
      case PUBLIC -> postVisibility.setPublicPostsEnabled(negotiationId, params.enabled());
      case PRIVATE -> postVisibility.setPrivatePostsEnabled(negotiationId, params.enabled());
      case BOTH -> {
        postVisibility.setPublicPostsEnabled(negotiationId, params.enabled());
        postVisibility.setPrivatePostsEnabled(negotiationId, params.enabled());
      }
    }
  }
}
