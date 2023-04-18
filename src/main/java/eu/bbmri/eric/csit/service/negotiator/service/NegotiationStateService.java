package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NegotiationStateService {
    NegotiationState getNegotiationState(String negotiationId);

    List<NegotiationEvent> getPossibleEvents(String negotiationId);

    NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent);
}
