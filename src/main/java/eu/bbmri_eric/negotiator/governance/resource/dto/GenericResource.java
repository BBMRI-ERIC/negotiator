package eu.bbmri_eric.negotiator.governance.resource.dto;


public interface GenericResource {

  String getId();

  String getName();

  String getDescription();

  GenericOrganization getOrganization();
}
