package eu.bbmri_eric.negotiator.negotiation;

import java.time.LocalDateTime;

public interface NegotiationTimelineEvent {
  String getTriggeredBy();

  String getText();

  LocalDateTime getTimestamp();
}
