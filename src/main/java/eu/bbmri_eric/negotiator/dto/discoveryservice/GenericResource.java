package eu.bbmri_eric.negotiator.dto.discoveryservice;

public interface GenericResource {

  String getId();

  String getName();

  String getDescription();

  GenericOrganization getOrganization();
}
