package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;

public interface ResourceViewDTO {
  Long getId();

  String getName();

  String getDescription();

  String getContactEmail();

  String getUri();

  String getSourceId();

  String getNegotiationId();

  NegotiationResourceState getCurrentState();

  String getOrganizationName();

  String getOrganizationId();

  String getOrganizationExternalId();

  String getOrganizationContactEmail();

  String getOrganizationDescription();

  String getOrganizationUri();
}
