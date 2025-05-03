package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationTimelineEventDTO;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NegotiationTimelineImpl implements NegotiationTimeline {
    private final NegotiationService negotiationService;
    private final NegotiationRepository negotiationRepository;
    private final ModelMapper modelMapper;

    public NegotiationTimelineImpl(NegotiationService negotiationService, NegotiationRepository negotiationRepository, ModelMapper modelMapper) {
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

        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new EntityNotFoundException(negotiationId));

        return negotiation.getLifecycleHistory().stream()
                .filter(Objects::nonNull)
                .map(NegotiationTimelineEvent.class::cast)
                .map(event -> modelMapper.map(event, NegotiationTimelineEventDTO.class))
                .toList();
    }

}
