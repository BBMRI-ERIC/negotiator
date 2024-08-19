package eu.bbmri_eric.negotiator.discovery_service;

import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotStorableException;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultDiscoveryServiceService")
public class DiscoveryServiceServiceImpl implements DiscoveryServiceService {

  private final DiscoveryServiceRepository discoveryServiceRepository;

  private final ModelMapper modelMapper;

  @Autowired
  public DiscoveryServiceServiceImpl(
      DiscoveryServiceRepository discoveryServiceRepository, ModelMapper modelMapper) {
    this.discoveryServiceRepository = discoveryServiceRepository;
    this.modelMapper = modelMapper;
  }

  private DiscoveryService findEntityById(Long id) {
    return discoveryServiceRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));
  }

  @Transactional
  public DiscoveryServiceDTO create(DiscoveryServiceCreateDTO discoveryServiceCreateDTO)
      throws EntityNotStorableException {
    DiscoveryService discoveryServiceEntity =
        modelMapper.map(discoveryServiceCreateDTO, DiscoveryService.class);
    try {
      return modelMapper.map(
          discoveryServiceRepository.save(discoveryServiceEntity), DiscoveryServiceDTO.class);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DiscoveryServiceDTO update(Long id, DiscoveryServiceCreateDTO discoveryServiceRequest)
      throws EntityNotStorableException {
    DiscoveryService discoveryServiceEntity = findEntityById(id);
    modelMapper.map(discoveryServiceRequest, discoveryServiceEntity);
    try {
      return modelMapper.map(
          discoveryServiceRepository.save(discoveryServiceEntity), DiscoveryServiceDTO.class);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DiscoveryServiceDTO findById(Long id) {
    DiscoveryService discoveryService = findEntityById(id);
    return modelMapper.map(discoveryService, DiscoveryServiceDTO.class);
  }

  public List<DiscoveryServiceDTO> findAll() {
    return discoveryServiceRepository.findAll().stream()
        .map(discoveryService -> modelMapper.map(discoveryService, DiscoveryServiceDTO.class))
        .collect(Collectors.toList());
  }
}
