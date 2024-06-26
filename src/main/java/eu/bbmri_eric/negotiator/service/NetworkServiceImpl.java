package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NetworkRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.network.NetworkCreateDTO;
import eu.bbmri_eric.negotiator.dto.network.NetworkDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.exceptions.UserNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
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
  public NetworkDTO updateNetwork(Long id, NetworkCreateDTO networkDTO)
      throws EntityNotStorableException {
    Network network = getNetwork(id);
    network.setName(networkDTO.getName());
    network.setUri(networkDTO.getUri());
    network.setExternalId(networkDTO.getExternalId());
    network.setContactEmail(networkDTO.getContactEmail());
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
