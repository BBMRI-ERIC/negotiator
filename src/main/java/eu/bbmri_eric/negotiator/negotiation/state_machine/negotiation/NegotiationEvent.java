package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;

@Getter
public enum NegotiationEvent {
  APPROVE("Approve", "Approve the negotiation", false),
  DECLINE("Decline", "Decline the negotiation", true),
  START("Start", "Start the negotiation", false),
  PAUSE("Pause", "Pause the negotiation", false),
  UNPAUSE("Unpause", "Unpause the negotiation", false),
  ABANDON("Abandon", "Abandon the negotiation", false),
  CONCLUDE("Conclude", "Conclude the negotiation", false);

  private final String label;
  private final String description;
  private final boolean messageRequired;

  NegotiationEvent(String label, String description, boolean messageRequired) {
    this.label = label;
    this.description = description;
    this.messageRequired = messageRequired;
  }

  public String getValue() {
    return this.name();
  }
}
