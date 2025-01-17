package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.UserNotFoundException;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationWithResourcesDTO;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithRepsDTO;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Transactional
@Service
public class NetworkServiceImpl implements NetworkService {
  @Autowired NetworkRepository networkRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired PersonRepository personRepository;
  @Autowired ModelMapper modelMapper;

  @Override
  public NetworkDTO findNetworkById(Long id) {
    return modelMapper.map(
        networkRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        NetworkDTO.class);
  }

  @Override
  public Iterable<NetworkDTO> findAllNetworks(Pageable pageable) {
    return networkRepository
        .findAll(pageable)
        .map(network -> modelMapper.map(network, NetworkDTO.class));
  }

  @Override
  public Iterable<NetworkDTO> findAllForManager(Long managerId, Pageable pageable) {
    Person manager =
        personRepository
            .findById(managerId)
            .orElseThrow(() -> new EntityNotFoundException(managerId));
    return networkRepository
        .findAllByManagersContains(manager, pageable)
        .map(network -> modelMapper.map(network, NetworkDTO.class));
  }

  @Override
  public void deleteNetworkById(Long id) {
    Network network = getNetwork(id);
    networkRepository.delete(network);
  }

  @Override
  public void removeResourceFromNetwork(Long networkId, Long resourceId) {
    Network network = getNetwork(networkId);
    Resource resource = getResource(resourceId);
    network.removeResource(resource);
    networkRepository.save(network);
  }

  @Override
  public void removeManagerFromNetwork(Long networkId, Long managerId) {
    Network network = getNetwork(networkId);
    Person manager = getManager(managerId);
    network.removeManager(manager);
    networkRepository.save(network);
  }

  @Override
  public Set<OrganizationWithResourcesDTO> findAllOrganizations(Long id) {
    Person person =
        personRepository
            .findById(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
            .orElseThrow(() -> new EntityNotFoundException(id));
    Network network = getNetwork(id);
    if (!person.getNetworks().contains(network)
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      throw new ForbiddenRequestException("You do not have permission to access this network");
    }
    Set<Resource> resources = network.getResources();
    Set<OrganizationWithResourcesDTO> organizations = new HashSet<>();
    resources.forEach(
        resource -> {
          organizations.add(
              modelMapper.map(resource.getOrganization(), OrganizationWithResourcesDTO.class));
        });
    organizations.forEach(
        organization -> {
          resources.forEach(
              resource -> {
                if (organization
                    .getExternalId()
                    .equals(resource.getOrganization().getExternalId())) {
                  organization.addResource(modelMapper.map(resource, ResourceWithRepsDTO.class));
                }
              });
        });
    return organizations;
  }

  @Override
  public NetworkDTO updateNetwork(Long id, NetworkCreateDTO networkDTO)
      throws EntityNotStorableException {
    Network network = getNetwork(id);
    modelMapper.map(networkDTO, network);
    networkRepository.save(network);
    return modelMapper.map(network, NetworkDTO.class);
  }

  @Override
  public NetworkDTO createNetwork(NetworkCreateDTO networkCreateDTO)
      throws EntityNotStorableException {
    Network network = modelMapper.map(networkCreateDTO, Network.class);
    Network savedNetwork = networkRepository.saveAndFlush(network);
    return modelMapper.map(savedNetwork, NetworkDTO.class);
  }

  @Override
  public List<NetworkDTO> createNetworks(Iterable<NetworkCreateDTO> request) {
    ArrayList<Network> networks = new ArrayList();
    for (NetworkCreateDTO networkDTO : request) {
      Network network = modelMapper.map(networkDTO, Network.class);
      networks.add(network);
    }
    return networkRepository.saveAll(networks).stream()
        .map(network -> modelMapper.map(network, NetworkDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public void addManagersToNetwork(Long id, List<Long> managerIds) {
    Network network = getNetwork(id);
    managerIds.forEach(
        managerId -> {
          Person manager = getManager(managerId);
          network.addManager(manager);
        });
    networkRepository.save(network);
  }

  @Override
  public void addResourcesToNetwork(Long id, List<Long> resourceIds) {
    Network network = getNetwork(id);
    resourceIds.forEach(
        resourceId -> {
          Resource resource = getResource(resourceId);
          network.addResource(resource);
        });
    networkRepository.save(network);
  }

  private Resource getResource(Long resourceId) {
    return resourceRepository
        .findById(resourceId)
        .orElseThrow(() -> new EntityNotFoundException(resourceId));
  }

  private Person getManager(Long managerId) {
    return personRepository
        .findById(managerId)
        .orElseThrow(() -> new UserNotFoundException(managerId));
  }

  private Network getNetwork(Long networkId) {
    return networkRepository
        .findById(networkId)
        .orElseThrow(() -> new EntityNotFoundException(networkId));
  }
}
