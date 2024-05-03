package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import eu.bbmri_eric.negotiator.dto.OrganizationDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceDTO;

import java.util.List;
import java.util.Set;

public interface NetworkService {

  NetworkDTO findNetworkById(Long id);

  List<NetworkDTO> findAllNetworks();

  List<ResourceDTO> getResources(Long networkId);

  Set<OrganizationDTO> getOrganizations(Long networkId);
}
