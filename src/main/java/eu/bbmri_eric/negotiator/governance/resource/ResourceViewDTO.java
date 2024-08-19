package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;

public interface ResourceViewDTO {
  Long getId();

  String getName();

  String getSourceId();

  String getNegotiationId();

  NegotiationResourceState getCurrentState();

  String getOrganizationName();

  String getOrganizationId();

  String getOrganizationExternalId();
}
