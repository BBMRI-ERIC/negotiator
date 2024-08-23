package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.OrganizationDTO;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {
  OrganizationDTO findOrganizationById(Long id);

  OrganizationDTO findOrganizationByExternalId(String externalId);

  Iterable<OrganizationDTO> findAllOrganizations(Pageable pageable);
}
