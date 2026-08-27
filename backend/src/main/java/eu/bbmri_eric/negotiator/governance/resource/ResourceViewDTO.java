package eu.bbmri_eric.negotiator.governance.resource;

public interface ResourceViewDTO {
  Long getId();

  String getName();

  String getDescription();

  String getContactEmail();

  String getUri();

  String getSourceId();

  String getNegotiationId();

  /** Name of the Resource's current State within the Negotiation. */
  String getCurrentState();

  String getOrganizationName();

  String getOrganizationId();

  String getOrganizationExternalId();

  String getOrganizationContactEmail();

  String getOrganizationDescription();

  String getOrganizationUri();
}
