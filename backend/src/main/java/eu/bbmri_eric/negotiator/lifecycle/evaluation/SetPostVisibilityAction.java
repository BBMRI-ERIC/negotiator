package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Turns a Negotiation's post visibility on or off, for public posts, private posts, or both.
 *
 * <p>ADR 0002's worked example of why Actions get params, and it holds up: the three classes it
 * replaces differ only in their one-line bodies — {@code setPublicPostsEnabled(id, true)}, {@code
 * setPrivatePostsEnabled(id, true)}, and one that sets <em>both</em> to false.
 *
 * <p><b>{@code BOTH} exists because of that third one.</b> A scope of {@code PUBLIC | PRIVATE}
 * alone would need two Wiring rows to reproduce the abandon Transition, and a reviewer comparing
 * the v1 seed against the committed graph dump would find three Actions becoming four rows with no
 * explanation. Three values keep it one row per dump Action and the mapping legible.
 *
 * <p><b>One bean, three rows — not three beans.</b> The obvious precedent here is {@code
 * DefaultWebhookMappingStrategy}, which is one final class configured N times as {@code @Bean}
 * methods. It does not apply: those N beans each declare a <em>different</em> key, and all three
 * post-visibility configurations declare this one. Three beans would collide in {@link
 * ActionRegistry} and fail the boot. The variation belongs in {@code params}, which is the entire
 * point of ADR 0002 giving Actions params in the first place.
 */
@Component
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

  SetPostVisibilityAction(PostVisibility postVisibility) {
    this.postVisibility = postVisibility;
  }

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
