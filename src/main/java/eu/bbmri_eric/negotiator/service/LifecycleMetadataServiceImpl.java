package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.repository.NegotiationLifecycleEventRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationLifecycleStateRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationEventMetadataDto;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationStateMetadataDto;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class LifecycleMetadataServiceImpl implements LifecycleMetadataService {

  private final NegotiationLifecycleStateRepository lifecycleStateRepository;
  private final NegotiationLifecycleEventRepository lifecycleEventRepository;
  private final ModelMapper modelMapper;

  public LifecycleMetadataServiceImpl(
      NegotiationLifecycleStateRepository lifecycleStateRepository,
      NegotiationLifecycleEventRepository lifecycleEventRepository,
      ModelMapper modelMapper) {
    this.lifecycleStateRepository = lifecycleStateRepository;
    this.lifecycleEventRepository = lifecycleEventRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public NegotiationStateMetadataDto findStateById(Long stateId) {
    return modelMapper.map(
        lifecycleStateRepository
            .findById(stateId)
            .orElseThrow(() -> new EntityNotFoundException(stateId)),
        NegotiationStateMetadataDto.class);
  }

  @Override
  public Set<NegotiationStateMetadataDto> findAllStates() {
    return lifecycleStateRepository.findAll().stream()
        .map(
            negotiationStateMetadata ->
                modelMapper.map(negotiationStateMetadata, NegotiationStateMetadataDto.class))
        .collect(Collectors.toSet());
  }

  @Override
  public NegotiationEventMetadataDto findEventById(Long eventId) {
    return modelMapper.map(
        lifecycleEventRepository
            .findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException(eventId)),
        NegotiationEventMetadataDto.class);
  }

  @Override
  public Set<NegotiationEventMetadataDto> findAllEvents() {
    return lifecycleEventRepository.findAll().stream()
        .map(
            negotiationEventMetadata ->
                modelMapper.map(negotiationEventMetadata, NegotiationEventMetadataDto.class))
        .collect(Collectors.toSet());
  }
}
