package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationTimelineEventDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class NegotiationTimelineImpl implements NegotiationTimeline {
  private final NegotiationService negotiationService;
  private final NegotiationRepository negotiationRepository;
  private final ModelMapper modelMapper;

  public NegotiationTimelineImpl(
      NegotiationService negotiationService,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper) {
    this.negotiationService = negotiationService;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public List<NegotiationTimelineEventDTO> getTimelineEvents(String negotiationId) {
    if (!negotiationService.isAuthorizedForNegotiation(negotiationId)) {
      throw new ForbiddenRequestException("You are not authorized to view this Negotiation.");
    }

    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    List<NegotiationTimelineEvent> resourceRecords =
        negotiation.getNegotiationResourceLifecycleRecords().stream()
            .filter(Objects::nonNull)
            .filter(
                res ->
                    !res.getChangedTo().equals(NegotiationResourceState.REPRESENTATIVE_CONTACTED)
                        && !res.getChangedTo()
                            .equals(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE))
            .map(NegotiationTimelineEvent.class::cast)
            .toList();
    List<NegotiationTimelineEvent> negotiationRecords =
        negotiation.getLifecycleHistory().stream()
            .filter(Objects::nonNull)
            .map(NegotiationTimelineEvent.class::cast)
            .toList();
    return Stream.concat(resourceRecords.stream(), negotiationRecords.stream())
        .map(event -> modelMapper.map(event, NegotiationTimelineEventDTO.class))
        .sorted(Comparator.comparing(NegotiationTimelineEventDTO::getTimestamp))
        .toList();
  }
}
