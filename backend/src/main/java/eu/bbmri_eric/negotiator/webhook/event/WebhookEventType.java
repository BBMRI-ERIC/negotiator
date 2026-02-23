package eu.bbmri_eric.negotiator.webhook.event;

/** Defines external webhook event type names. */
public final class WebhookEventType {
  public static final String NEGOTIATION_ADDED = "negotiation.added";
  public static final String NEGOTIATION_STATE_UPDATED = "negotiation.state.updated";
  public static final String NEGOTIATION_INFO_UPDATED = "negotiation.info.updated";
  public static final String NEGOTIATION_RESOURCE_ADDED = "negotiation.resource.added";
  public static final String NEGOTIATION_RESOURCE_STATE_UPDATED =
      "negotiation.resource.state.updated";
  public static final String NEGOTIATION_POST_ADDED = "negotiation.post.added";

  private WebhookEventType() {}
}
