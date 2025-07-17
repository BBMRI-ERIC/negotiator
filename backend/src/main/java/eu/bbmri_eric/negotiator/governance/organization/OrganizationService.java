package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.organization.dto.OrganizationFilterDTO;

public interface OrganizationService {
  OrganizationDTO findOrganizationById(Long id);

  OrganizationDTO findOrganizationByExternalId(String externalId);

  Iterable<OrganizationDTO> findAllOrganizations(OrganizationFilterDTO filters);

  Iterable<OrganizationDTO> addOrganizations(Iterable<OrganizationCreateDTO> organizations);

  OrganizationDTO updateOrganizationById(Long id, OrganizationUpdateDTO organization);
}
