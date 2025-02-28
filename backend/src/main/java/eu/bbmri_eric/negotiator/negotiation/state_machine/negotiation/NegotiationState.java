package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;

@Getter
public enum NegotiationState {
  DRAFT(
      "Draft",
      "The negotiation request has been drafted. The requester has to complete the request before submitting it for review"),
  SUBMITTED("Under review", "The negotiation has been submitted for review"),
  APPROVED("Approved", "The negotiation has been approved"),
  DECLINED("Declined", "The negotiation has been declined"),
  IN_PROGRESS("In Progress", "The negotiation is currently in progress"),
  PAUSED("Paused", "The negotiation is paused"),
  CONCLUDED("Concluded", "The negotiation has been concluded"),
  ABANDONED("Abandoned", "The negotiation has been abandoned");

  private final String label;
  private final String description;

  NegotiationState(String label, String description) {
    this.label = label;
    this.description = description;
  }

  public String getValue() {
    return this.name();
  }
}
