package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/** Defines external webhook event type names. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum WebhookEventType {
  CUSTOM("custom"),
  NEGOTIATION_ADDED("negotiation.added"),
  NEGOTIATION_STATE_UPDATED("negotiation.state.updated"),
  NEGOTIATION_INFO_UPDATED("negotiation.info.updated"),
  NEGOTIATION_RESOURCE_ADDED("negotiation.resource.added"),
  NEGOTIATION_RESOURCE_STATE_UPDATED("negotiation.resource.state.updated"),
  NEGOTIATION_POST_ADDED("negotiation.post.added");

  private final String value;

  @JsonValue
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
