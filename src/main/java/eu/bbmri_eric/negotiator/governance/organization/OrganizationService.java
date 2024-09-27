package eu.bbmri_eric.negotiator.governance.organization;

import org.springframework.data.domain.Pageable;

public interface OrganizationService {
  OrganizationDTO findOrganizationById(Long id);

  OrganizationDTO findOrganizationByExternalId(String externalId);

  Iterable<OrganizationDTO> findAllOrganizations(Pageable pageable);

  Iterable<OrganizationDTO> addOrganizations(Iterable<OrganizationCreateDTO> organizations);

  OrganizationDTO updateOrganizationById(Long id, OrganizationCreateDTO organization);
}
