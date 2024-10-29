package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import lombok.Getter;
@Getter
public enum NegotiationResourceState {
  // Note that the order of the individual values is important. The most advanced state (final state) is at the bottom.
  SUBMITTED("Submitted", "Initial state"),
  REPRESENTATIVE_UNREACHABLE(
      "Representative Unreachable",
      "This resource does not have any representatives in the system"),
  REPRESENTATIVE_CONTACTED(
      "Representative Contacted",
      "Representative of the given resource was notified about this new request"),
  RETURNED_FOR_RESUBMISSION(
      "Returned for Resubmission",
      "A representative has asked for further clarification of the request"),
  CHECKING_AVAILABILITY("Checking Availability", "Currently checking availability of the resource"),
  RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT(
      "Resource Unavailable, Willing to Collect",
      "The resource is unavailable at this time, but the organization is willing to collect it and make it available for access"),
  RESOURCE_UNAVAILABLE("Resource Unavailable", "The resource is unavailable for access"),
  RESOURCE_AVAILABLE("Resource Available", "The resource is available for access"),
  ACCESS_CONDITIONS_INDICATED(
      "Access Conditions Indicated",
      "A representative has indicated access conditions for the resource"),
  ACCESS_CONDITIONS_MET(
      "Access Conditions Met", "Access conditions for the resource have been met by the requester"),
  RESOURCE_NOT_MADE_AVAILABLE(
      "Resource Not Made Available", "The resource has not been made available"),
  RESOURCE_MADE_AVAILABLE("Resource Made Available", "The resource has been made available");

  private final String label;
  private final String description;

  NegotiationResourceState(String label, String description) {
    this.label = label;
    this.description = description;
  }

  public String getValue() {
    return this.name();
  }
}
