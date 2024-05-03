package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.repository.NetworkRepository;
import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import eu.bbmri_eric.negotiator.dto.OrganizationDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NetworkServiceImpl implements NetworkService {
  @Autowired NetworkRepository networkRepository;

  ModelMapper modelMapper = new ModelMapper();

  @Override
  public NetworkDTO findNetworkById(Long id) {
    return modelMapper.map(
        networkRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        NetworkDTO.class);
  }

  @Override
  public List<NetworkDTO> findAllNetworks() {
    List<Network> networks = networkRepository.findAll();
    return networks.stream()
            .map(network -> modelMapper.map(network, NetworkDTO.class))
            .collect(Collectors.toList());
  }

  @Override
  public List<ResourceDTO> getResources(Long networkId) {
      return networkRepository.findById(networkId)
              .orElseThrow(() -> new EntityNotFoundException(networkId))
              .getResources()
              .stream().map((element) -> modelMapper.map(element, ResourceDTO.class))
              .collect(Collectors.toList());
  }

  @Override
  public Set<OrganizationDTO> getOrganizations(Long networkId) {
    return networkRepository.findById(networkId)
            .orElseThrow(() -> new EntityNotFoundException(networkId))
            .getResources()
            .stream()
            .map(resource -> modelMapper.map(resource.getOrganization(), OrganizationDTO.class))
            .collect(Collectors.toSet());
  }

}
