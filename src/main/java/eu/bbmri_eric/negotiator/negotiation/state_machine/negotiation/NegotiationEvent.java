package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;

@Getter
public enum NegotiationEvent {
  APPROVE("Approve", "Approve the negotiation", true),
  DECLINE("Decline", "Decline the negotiation", false),
  START("Start", "Start the negotiation", false),
  PAUSE("Pause", "Pause the negotiation", false),
  UNPAUSE("Unpause", "Unpause the negotiation", false),
  ABANDON("Abandon", "Abandon the negotiation", false),
  CONCLUDE("Conclude", "Conclude the negotiation", false);

  private final String label;
  private final String description;
  private final Boolean detailsRequired;

  NegotiationEvent(String label, String description, Boolean detailsRequired) {
    this.label = label;
    this.description = description;
    this.detailsRequired = detailsRequired;
  }

  public String getValue() {
    return this.name();
  }
}
