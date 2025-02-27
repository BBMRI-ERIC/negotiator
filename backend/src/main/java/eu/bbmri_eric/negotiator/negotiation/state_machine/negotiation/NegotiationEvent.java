package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;

@Getter
public enum NegotiationEvent {
  SUBMIT("Submit", "Submit the negotiation"),
  APPROVE("Approve", "Approve the negotiation"),
  DECLINE("Decline", "Decline the negotiation"),
  START("Start", "Start the negotiation"),
  PAUSE("Pause", "Pause the negotiation"),
  UNPAUSE("Unpause", "Unpause the negotiation"),
  ABANDON("Abandon", "Abandon the negotiation"),
  CONCLUDE("Conclude", "Conclude the negotiation");

  private final String label;
  private final String description;

  NegotiationEvent(String label, String description) {
    this.label = label;
    this.description = description;
  }

  public String getValue() {
    return this.name();
  }
}
