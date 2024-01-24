package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.OrganizationDTO;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {
  OrganizationDTO findOrganizationById(Long id);

  OrganizationDTO findOrganizationByExternalId(String externalId);

  Iterable<OrganizationDTO> findAllOrganizations(Pageable pageable);
}
