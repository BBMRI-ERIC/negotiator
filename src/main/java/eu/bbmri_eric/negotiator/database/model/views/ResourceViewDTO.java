package eu.bbmri_eric.negotiator.database.model.views;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;

public class ResourceViewDTO {
  String name;
  String sourceId;
  NegotiationResourceState currentState;
  String organizationName;
  String organizationExternalId;
}
