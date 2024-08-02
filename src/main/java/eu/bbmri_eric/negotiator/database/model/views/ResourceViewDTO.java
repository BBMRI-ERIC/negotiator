package eu.bbmri_eric.negotiator.database.model.views;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;

public interface ResourceViewDTO {
  String getName();

  String getSourceId();

  NegotiationResourceState getCurrentState();

  String getOrganizationName();

  String getOrganizationExternalId();
}
