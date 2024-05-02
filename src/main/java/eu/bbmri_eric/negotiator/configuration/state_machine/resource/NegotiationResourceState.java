package eu.bbmri_eric.negotiator.configuration.state_machine.resource;

import lombok.Getter;

@Getter
public enum NegotiationResourceState {
  SUBMITTED("Submitted", "The resource has been submitted"),
  REPRESENTATIVE_CONTACTED(
      "Representative Contacted", "Contact has been made with the representative"),
  REPRESENTATIVE_UNREACHABLE("Representative Unreachable", "Unable to reach the representative"),
  RETURNED_FOR_RESUBMISSION(
      "Returned for Resubmission", "The resource has been returned for resubmission"),
  CHECKING_AVAILABILITY("Checking Availability", "Currently checking availability of the resource"),
  RESOURCE_AVAILABLE("Resource Available", "The resource is available"),
  RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT(
      "Resource Unavailable, Willing to Collect",
      "The resource is unavailable but willing to be collected"),
  RESOURCE_UNAVAILABLE("Resource Unavailable", "The resource is unavailable"),
  ACCESS_CONDITIONS_INDICATED(
      "Access Conditions Indicated", "Indicated access conditions for the resource"),
  ACCESS_CONDITIONS_MET(
      "Access Conditions Met", "Access conditions for the resource have been met"),
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
